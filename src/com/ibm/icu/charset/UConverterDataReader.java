/**
*******************************************************************************
* Copyright (C) 2006-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 
 
package com.ibm.icu.charset;

import com.ibm.icu.impl.ICUBinary;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;

/**
 * ucnvmbcs.h
 *
 * ICU conversion (.cnv) data file structure, following the usual UDataInfo
 * header.
 *
 * Format version: 6.2
 *
 * struct UConverterStaticData -- struct containing the converter name, IBM CCSID,
 *                                min/max bytes per character, etc.
 *                                see ucnv_bld.h
 *
 * --------------------
 *
 * The static data is followed by conversionType-specific data structures.
 * At the moment, there are only variations of MBCS converters. They all have
 * the same toUnicode structures, while the fromUnicode structures for SBCS
 * differ from those for other MBCS-style converters.
 *
 * _MBCSHeader.version 4.2 adds an optional conversion extension data structure.
 * If it is present, then an ICU version reading header versions 4.0 or 4.1
 * will be able to use the base table and ignore the extension.
 *
 * The unicodeMask in the static data is part of the base table data structure.
 * Especially, the UCNV_HAS_SUPPLEMENTARY flag determines the length of the
 * fromUnicode stage 1 array.
 * The static data unicodeMask refers only to the base table's properties if
 * a base table is included.
 * In an extension-only file, the static data unicodeMask is 0.
 * The extension data indexes have a separate field with the unicodeMask flags.
 *
 * MBCS-style data structure following the static data.
 * Offsets are counted in bytes from the beginning of the MBCS header structure.
 * Details about usage in comments in ucnvmbcs.c.
 *
 * struct _MBCSHeader (see the definition in this header file below)
 * contains 32-bit fields as follows:
 * 8 values:
 *  0   uint8_t[4]  MBCS version in UVersionInfo format (currently 4.2.0.0)
 *  1   uint32_t    countStates
 *  2   uint32_t    countToUFallbacks
 *  3   uint32_t    offsetToUCodeUnits
 *  4   uint32_t    offsetFromUTable
 *  5   uint32_t    offsetFromUBytes
 *  6   uint32_t    flags, bits:
 *                      31.. 8 offsetExtension -- _MBCSHeader.version 4.2 (ICU 2.8) and higher
 *                                                0 for older versions and if
 *                                                there is not extension structure
 *                       7.. 0 outputType
 *  7   uint32_t    fromUBytesLength -- _MBCSHeader.version 4.1 (ICU 2.4) and higher
 *                  counts bytes in fromUBytes[]
 *
 * if(outputType==MBCS_OUTPUT_EXT_ONLY) {
 *     -- base table name for extension-only table
 *     char baseTableName[variable]; -- with NUL plus padding for 4-alignment
 *
 *     -- all _MBCSHeader fields except for version and flags are 0
 * } else {
 *     -- normal base table with optional extension
 *
 *     int32_t stateTable[countStates][256];
 *    
 *     struct _MBCSToUFallback { (fallbacks are sorted by offset)
 *         uint32_t offset;
 *         UChar32 codePoint;
 *     } toUFallbacks[countToUFallbacks];
 *    
 *     uint16_t unicodeCodeUnits[(offsetFromUTable-offsetToUCodeUnits)/2];
 *                  (padded to an even number of units)
 *    
 *     -- stage 1 tables
 *     if(staticData.unicodeMask&UCNV_HAS_SUPPLEMENTARY) {
 *         -- stage 1 table for all of Unicode
 *         uint16_t fromUTable[0x440]; (32-bit-aligned)
 *     } else {
 *         -- BMP-only tables have a smaller stage 1 table
 *         uint16_t fromUTable[0x40]; (32-bit-aligned)
 *     }
 *    
 *     -- stage 2 tables
 *        length determined by top of stage 1 and bottom of stage 3 tables
 *     if(outputType==MBCS_OUTPUT_1) {
 *         -- SBCS: pure indexes
 *         uint16_t stage 2 indexes[?];
 *     } else {
 *         -- DBCS, MBCS, EBCDIC_STATEFUL, ...: roundtrip flags and indexes
 *         uint32_t stage 2 flags and indexes[?];
 *     }
 *    
 *     -- stage 3 tables with byte results
 *     if(outputType==MBCS_OUTPUT_1) {
 *         -- SBCS: each 16-bit result contains flags and the result byte, see ucnvmbcs.c
 *         uint16_t fromUBytes[fromUBytesLength/2];
 *     } else {
 *         -- DBCS, MBCS, EBCDIC_STATEFUL, ... 2/3/4 bytes result, see ucnvmbcs.c
 *         uint8_t fromUBytes[fromUBytesLength]; or
 *         uint16_t fromUBytes[fromUBytesLength/2]; or
 *         uint32_t fromUBytes[fromUBytesLength/4];
 *     }
 * }
 *
 * -- extension table, details see ucnv_ext.h
 * int32_t indexes[>=32]; ...
 */
/*
 * ucnv_ext.h
 *
 * See icuhtml/design/conversion/conversion_extensions.html
 *
 * Conversion extensions serve two purposes:
 * 1. They support m:n mappings.
 * 2. They support extension-only conversion files that are used together
 *    with the regular conversion data in base files.
 *
 * A base file may contain an extension table (explicitly requested or
 * implicitly generated for m:n mappings), but its extension table is not
 * used when an extension-only file is used.
 *
 * It is an error if a base file contains any regular (not extension) mapping
 * from the same sequence as a mapping in the extension file
 * because the base mapping would hide the extension mapping.
 *
 *
 * Data for conversion extensions:
 *
 * One set of data structures per conversion direction (to/from Unicode).
 * The data structures are sorted by input units to allow for binary search.
 * Input sequences of more than one unit are handled like contraction tables
 * in collation:
 * The lookup value of a unit points to another table that is to be searched
 * for the next unit, recursively.
 *
 * For conversion from Unicode, the initial code point is looked up in
 * a 3-stage trie for speed,
 * with an additional table of unique results to save space.
 *
 * Long output strings are stored in separate arrays, with length and index
 * in the lookup tables.
 * Output results also include a flag distinguishing roundtrip from
 * (reverse) fallback mappings.
 *
 * Input Unicode strings must not begin or end with unpaired surrogates
 * to avoid problems with matches on parts of surrogate pairs.
 *
 * Mappings from multiple characters (code points or codepage state
 * table sequences) must be searched preferring the longest match.
 * For this to work and be efficient, the variable-width table must contain
 * all mappings that contain prefixes of the multiple characters.
 * If an extension table is built on top of a base table in another file
 * and a base table entry is a prefix of a multi-character mapping, then
 * this is an error.
 *
 *
 * Implementation note:
 *
 * Currently, the parser and several checks in the code limit the number
 * of UChars or bytes in a mapping to
 * UCNV_EXT_MAX_UCHARS and UCNV_EXT_MAX_BYTES, respectively,
 * which are output value limits in the data structure.
 *
 * For input, this is not strictly necessary - it is a hard limit only for the
 * buffers in UConverter that are used to store partial matches.
 *
 * Input sequences could otherwise be arbitrarily long if partial matches
 * need not be stored (i.e., if a sequence does not span several buffers with too
 * many units before the last buffer), although then results would differ
 * depending on whether partial matches exceed the limits or not,
 * which depends on the pattern of buffer sizes.
 *
 *
 * Data structure:
 *
 * int32_t indexes[>=32];
 *
 *   Array of indexes and lengths etc. The length of the array is at least 32.
 *   The actual length is stored in indexes[0] to be forward compatible.
 *
 *   Each index to another array is the number of bytes from indexes[].
 *   Each length of an array is the number of array base units in that array.
 *
 *   Some of the structures may not be present, in which case their indexes
 *   and lengths are 0.
 *
 *   Usage of indexes[i]:
 *   [0]  length of indexes[]
 *
 *   // to Unicode table
 *   [1]  index of toUTable[] (array of uint32_t)
 *   [2]  length of toUTable[]
 *   [3]  index of toUUChars[] (array of UChar)
 *   [4]  length of toUUChars[]
 *
 *   // from Unicode table, not for the initial code point
 *   [5]  index of fromUTableUChars[] (array of UChar)
 *   [6]  index of fromUTableValues[] (array of uint32_t)
 *   [7]  length of fromUTableUChars[] and fromUTableValues[]
 *   [8]  index of fromUBytes[] (array of char)
 *   [9]  length of fromUBytes[]
 *
 *   // from Unicode trie for initial-code point lookup
 *   [10] index of fromUStage12[] (combined array of uint16_t for stages 1 & 2)
 *   [11] length of stage 1 portion of fromUStage12[]
 *   [12] length of fromUStage12[]
 *   [13] index of fromUStage3[] (array of uint16_t indexes into fromUStage3b[])
 *   [14] length of fromUStage3[]
 *   [15] index of fromUStage3b[] (array of uint32_t like fromUTableValues[])
 *   [16] length of fromUStage3b[]
 *
 *   [17] Bit field containing numbers of bytes:
 *        31..24 reserved, 0
 *        23..16 maximum input bytes
 *        15.. 8 maximum output bytes
 *         7.. 0 maximum bytes per UChar
 *
 *   [18] Bit field containing numbers of UChars:
 *        31..24 reserved, 0
 *        23..16 maximum input UChars
 *        15.. 8 maximum output UChars
 *         7.. 0 maximum UChars per byte
 *
 *   [19] Bit field containing flags:
 *               (extension table unicodeMask)
 *         1     UCNV_HAS_SURROGATES flag for the extension table
 *         0     UCNV_HAS_SUPPLEMENTARY flag for the extension table
 *
 *   [20]..[30] reserved, 0
 *   [31] number of bytes for the entire extension structure
 *   [>31] reserved; there are indexes[0] indexes
 *
 *
 * uint32_t toUTable[];
 *
 *   Array of byte/value pairs for lookups for toUnicode conversion.
 *   The array is partitioned into sections like collation contraction tables.
 *   Each section contains one word with the number of following words and
 *   a default value for when the lookup in this section yields no match.
 *
 *   A section is sorted in ascending order of input bytes,
 *   allowing for fast linear or binary searches.
 *   The builder may store entries for a contiguous range of byte values
 *   (compare difference between the first and last one with count),
 *   which then allows for direct array access.
 *   The builder should always do this for the initial table section.
 *
 *   Entries may have 0 values, see below.
 *   No two entries in a section have the same byte values.
 *
 *   Each uint32_t contains an input byte value in bits 31..24 and the
 *   corresponding lookup value in bits 23..0.
 *   Interpret the value as follows:
 *     if(value==0) {
 *       no match, see below
 *     } else if(value<0x1f0000) {
 *       partial match - use value as index to the next toUTable section
 *       and match the next unit; (value indexes toUTable[value])
 *     } else {
 *       if(bit 23 set) {
 *         roundtrip;
 *       } else {
 *         fallback;
 *       }
 *       unset value bit 23;
 *       if(value<=0x2fffff) {
 *         (value-0x1f0000) is a code point; (BMP: value<=0x1fffff)
 *       } else {
 *         bits 17..0 (value&0x3ffff) is an index to
 *           the result UChars in toUUChars[]; (0 indexes toUUChars[0])
 *         length of the result=((value>>18)-12); (length=0..19)
 *       }
 *     }
 *
 *   The first word in a section contains the number of following words in the
 *   input byte position (bits 31..24, number=1..0xff).
 *   The value of the initial word is used when the current byte is not found
 *   in this section.
 *   If the value is not 0, then it represents a result as above.
 *   If the value is 0, then the search has to return a shorter match with an
 *   earlier default value as the result, or result in "unmappable" even for the
 *   initial bytes.
 *   If the value is 0 for the initial toUTable entry, then the initial byte
 *   does not start any mapping input.
 *
 *
 * UChar toUUChars[];
 *
 *   Contains toUnicode mapping results, stored as sequences of UChars.
 *   Indexes and lengths stored in the toUTable[].
 *
 *
 * UChar fromUTableUChars[];
 * uint32_t fromUTableValues[];
 *
 *   The fromUTable is split into two arrays, but works otherwise much like
 *   the toUTable. The array is partitioned into sections like collation
 *   contraction tables and toUTable.
 *   A row in the table consists of same-index entries in fromUTableUChars[]
 *   and fromUTableValues[].
 *
 *   Interpret a value as follows:
 *     if(value==0) {
 *       no match, see below
 *     } else if(value<=0xffffff) { (bits 31..24 are 0)
 *       partial match - use value as index to the next fromUTable section
 *       and match the next unit; (value indexes fromUTable[value])
 *     } else {
 *       if(value==0x80000001) {
 *         return no mapping, but request for <subchar1>;
 *       }
 *       if(bit 31 set) {
 *         roundtrip;
 *       } else {
 *         fallback;
 *       }
 *       // bits 30..29 reserved, 0
 *       length=(value>>24)&0x1f; (bits 28..24)
 *       if(length==1..3) {
 *         bits 23..0 contain 1..3 bytes, padded with 00s on the left;
 *       } else {
 *         bits 23..0 (value&0xffffff) is an index to
 *           the result bytes in fromUBytes[]; (0 indexes fromUBytes[0])
 *       }
 *     }
 *       
 *   The first pair in a section contains the number of following pairs in the
 *   UChar position (16 bits, number=1..0xffff).
 *   The value of the initial pair is used when the current UChar is not found
 *   in this section.
 *   If the value is not 0, then it represents a result as above.
 *   If the value is 0, then the search has to return a shorter match with an
 *   earlier default value as the result, or result in "unmappable" even for the
 *   initial UChars.
 *
 *   If the from Unicode trie is present, then the from Unicode search tables
 *   are not used for initial code points.
 *   In this case, the first entries (index 0) in the tables are not used
 *   (reserved, set to 0) because a value of 0 is used in trie results
 *   to indicate no mapping.
 *
 *
 * uint16_t fromUStage12[];
 *
 *   Stages 1 & 2 of a trie that maps an initial code point.
 *   Indexes in stage 1 are all offset by the length of stage 1 so that the
 *   same array pointer can be used for both stages.
 *   If (c>>10)>=(length of stage 1) then c does not start any mapping.
 *   Same bit distribution as for regular conversion tries.
 *
 *
 * uint16_t fromUStage3[];
 * uint32_t fromUStage3b[];
 *
 *   Stage 3 of the trie. The first array simply contains indexes to the second,
 *   which contains words in the same format as fromUTableValues[].
 *   Use a stage 3 granularity of 4, which allows for 256k stage 3 entries,
 *   and 16-bit entries in stage 3 allow for 64k stage 3b entries.
 *   The stage 3 granularity means that the stage 2 entry needs to be left-shifted.
 *
 *   Two arrays are used because it is expected that more than half of the stage 3
 *   entries will be zero. The 16-bit index stage 3 array saves space even
 *   considering storing a total of 6 bytes per non-zero entry in both arrays
 *   together.
 *   Using a stage 3 granularity of >1 diminishes the compactability in that stage
 *   but provides a larger effective addressing space in stage 2.
 *   All but the final result stage use 16-bit entries to save space.
 *
 *   fromUStage3b[] contains a zero for "no mapping" at its index 0,
 *   and may contain UCNV_EXT_FROM_U_SUBCHAR1 at index 1 for "<subchar1> SUB mapping"
 *   (i.e., "no mapping" with preference for <subchar1> rather than <subchar>),
 *   and all other items are unique non-zero results.
 *
 *   The default value of a fromUTableValues[] section that is referenced
 *   _directly_ from a fromUStage3b[] item may also be UCNV_EXT_FROM_U_SUBCHAR1,
 *   but this value must not occur anywhere else in fromUTableValues[]
 *   because "no mapping" is always a property of a single code point,
 *   never of multiple.
 *
 *
 * char fromUBytes[];
 *
 *   Contains fromUnicode mapping results, stored as sequences of chars.
 *   Indexes and lengths stored in the fromUTableValues[].
 */

final class UConverterDataReader implements ICUBinary.Authenticate {
    //private final static boolean debug = ICUDebug.enabled("UConverterDataReader");

    /*
     * 	 UConverterDataReader(UConverterDataReader r)
        {
            dataInputStream = new DataInputStream(r.dataInputStream);
            unicodeVersion = r.unicodeVersion;
        }
        */
   /* the number bytes read from the stream */ 
   int bytesRead = 0;
   /* the number of bytes read for static data */
   int staticDataBytesRead = 0;
   /**
    * <p>Protected constructor.</p>
    * @param inputStream ICU uprop.dat file input stream
    * @exception IOException throw if data file fails authentication 
    * @draft 2.1
    */
    protected UConverterDataReader(InputStream inputStream) 
                                        throws IOException{
        //if(debug) System.out.println("Bytes in inputStream " + inputStream.available());
        
        /*unicodeVersion = */ICUBinary.readHeader(inputStream, DATA_FORMAT_ID, this);
        
        //if(debug) System.out.println("Bytes left in inputStream " +inputStream.available());
        
        dataInputStream = new DataInputStream(inputStream);
        
        //if(debug) System.out.println("Bytes left in dataInputStream " +dataInputStream.available());
    }
    
    // protected methods -------------------------------------------------
    
    protected void readStaticData(UConverterStaticData sd) throws IOException
    {
        int bRead = 0;
        sd.structSize = dataInputStream.readInt();
        bRead +=4;
        byte[] name = new byte[UConverterConstants.MAX_CONVERTER_NAME_LENGTH];
        dataInputStream.readFully(name);
        bRead +=name.length;
        sd.name = new String(name, 0, name.length);
        sd.codepage = dataInputStream.readInt();
        bRead +=4;
        sd.platform = dataInputStream.readByte();
        bRead++;
        sd.conversionType = dataInputStream.readByte();
        bRead++;
        sd.minBytesPerChar = dataInputStream.readByte();
        bRead++;
        sd.maxBytesPerChar = dataInputStream.readByte();
        bRead++;
        dataInputStream.readFully(sd.subChar);
        bRead += sd.subChar.length;
        sd.subCharLen = dataInputStream.readByte();
        bRead++;
        sd.hasToUnicodeFallback = dataInputStream.readByte();
        bRead++;
        sd.hasFromUnicodeFallback = dataInputStream.readByte();
        bRead++;
        sd.unicodeMask = (short)dataInputStream.readUnsignedByte();
        bRead++;
        sd.subChar1 = dataInputStream.readByte();
        bRead++;
        dataInputStream.readFully(sd.reserved);
        bRead += sd.reserved.length;
        staticDataBytesRead = bRead;
        bytesRead += bRead;
    }

    protected void readMBCSHeader(CharsetMBCS.MBCSHeader h) throws IOException
    {
        dataInputStream.readFully(h.version);
        bytesRead += h.version.length;
        h.countStates = dataInputStream.readInt();
        bytesRead+=4;
        h.countToUFallbacks = dataInputStream.readInt();
        bytesRead+=4;
        h.offsetToUCodeUnits = dataInputStream.readInt();
        bytesRead+=4;
        h.offsetFromUTable = dataInputStream.readInt();
        bytesRead+=4;
        h.offsetFromUBytes = dataInputStream.readInt();
        bytesRead+=4;
        h.flags = dataInputStream.readInt();
        bytesRead+=4;
        h.fromUBytesLength = dataInputStream.readInt();
        bytesRead+=4;
    }
    
    protected void readMBCSTable(int[][] stateTableArray, CharsetMBCS.MBCSToUFallback[] toUFallbacksArray, char[] unicodeCodeUnitsArray, char[] fromUnicodeTableArray, byte[] fromUnicodeBytesArray) throws IOException
    {
        int i, j;
        for(i = 0; i < stateTableArray.length; ++i){
            for(j = 0; j < stateTableArray[i].length; ++j){
                stateTableArray[i][j] = dataInputStream.readInt();
                bytesRead+=4;
            }
        }
        for(i = 0; i < toUFallbacksArray.length; ++i) {
            toUFallbacksArray[i].offset = dataInputStream.readInt();
            bytesRead+=4;
            toUFallbacksArray[i].codePoint = dataInputStream.readInt();
            bytesRead+=4;
        }
        for(i = 0; i < unicodeCodeUnitsArray.length; ++i){
            unicodeCodeUnitsArray[i] = dataInputStream.readChar();
            bytesRead+=2;
        }
        for(i = 0; i < fromUnicodeTableArray.length; ++i){
            fromUnicodeTableArray[i] = dataInputStream.readChar();
            bytesRead+=2;
        }
        for(i = 0; i < fromUnicodeBytesArray.length; ++i){
            fromUnicodeBytesArray[i] = dataInputStream.readByte();
            bytesRead++;
        }
    }

    protected String readBaseTableName() throws IOException
    {
        char c;
        StringBuffer name = new StringBuffer();
        while((c = (char)dataInputStream.readByte()) !=  0){
            name.append(c);
            bytesRead++;
        }
        bytesRead++/*for null terminator*/;
        return name.toString();
    }

    //protected int[] readExtIndexes(int skip) throws IOException
    protected ByteBuffer readExtIndexes(int skip) throws IOException
    {
        int skipped = dataInputStream.skipBytes(skip);
        if(skipped != skip){
            throw new IOException("could not skip "+ skip +" bytes");
        }
        int n = dataInputStream.readInt();
        bytesRead+=4;
        int[] indexes = new int[n];
        indexes[0] = n;
        for(int i = 1; i < n; ++i) {
            indexes[i] = dataInputStream.readInt();
            bytesRead+=4;
        }
        //return indexes;

        ByteBuffer b = ByteBuffer.allocate(indexes[31]);
        for(int i = 0; i < n; ++i) {
            b.putInt(indexes[i]);
        }
        int len = dataInputStream.read(b.array(), b.position(), b.remaining());
        if(len==-1){
            throw new IOException("Read failed");
        }
        bytesRead += len;
        return b;
    }

    /*protected byte[] readExtTables(int n) throws IOException
    {
        byte[] tables = new byte[n];
        int len =dataInputStream.read(tables);
        if(len==-1){
            throw new IOException("Read failed");
        }
        bytesRead += len;
        return tables;
    }*/

    byte[] getDataFormatVersion(){
        return DATA_FORMAT_VERSION;
    }
    /**
     * Inherited method
     */
    public boolean isDataVersionAcceptable(byte version[]){
        return version[0] == DATA_FORMAT_VERSION[0];
    }
    
/*    byte[] getUnicodeVersion(){
        return unicodeVersion;    
    }*/
    // private data members -------------------------------------------------
      
    /**
    * ICU data file input stream
    */
    DataInputStream dataInputStream;
    
//    private byte[] unicodeVersion;
                                       
    /**
    * File format version that this class understands.
    * No guarantees are made if a older version is used
    * see store.c of gennorm for more information and values
    */
    // DATA_FORMAT_ID_ values taken from icu4c isCnvAcceptable (ucnv_bld.c)
    private static final byte DATA_FORMAT_ID[] = {(byte)0x63, (byte)0x6e, (byte)0x76, (byte)0x74}; // dataFormat="cnvt"
    private static final byte DATA_FORMAT_VERSION[] = {(byte)0x6};

}

