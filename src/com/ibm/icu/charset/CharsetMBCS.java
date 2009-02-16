/**
 *******************************************************************************
 * Copyright (C) 2006-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 *******************************************************************************
 */
package com.ibm.icu.charset;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import com.ibm.icu.charset.UConverterSharedData.UConverterType;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.InvalidFormatException;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.charset.UConverterConstants;

class CharsetMBCS extends CharsetICU {

    private byte[] fromUSubstitution = null;
    UConverterSharedData sharedData = null;
    private static final int MAX_VERSION_LENGTH = 4;
    
    // these variables are used in getUnicodeSet() and may be changed in future
    // typedef enum UConverterSetFilter {
      static final int UCNV_SET_FILTER_NONE = 1;
      static final int UCNV_SET_FILTER_DBCS_ONLY = 2;
      static final int UCNV_SET_FILTER_2022_CN = 3;
      static final int UCNV_SET_FILTER_SJIS= 4 ;
      static final int UCNV_SET_FILTER_GR94DBCS = 5;
      static final int UCNV_SET_FILTER_HZ = 6;
      static final int UCNV_SET_FILTER_COUNT = 7;
   //  } UConverterSetFilter;

    /**
     * Fallbacks to Unicode are stored outside the normal state table and code point structures in a vector of items of
     * this type. They are sorted by offset.
     */
    final class MBCSToUFallback {
        int offset;
        int codePoint;
    }

    /**
     * This is the MBCS part of the UConverterTable union (a runtime data structure). It keeps all the per-converter
     * data and points into the loaded mapping tables.
     */
    static final class UConverterMBCSTable {
        /* toUnicode */
        short countStates;
        byte dbcsOnlyState;
        boolean stateTableOwned;
        int countToUFallbacks;

        int stateTable[/* countStates */][/* 256 */];
        int swapLFNLStateTable[/* countStates */][/* 256 */]; /* for swaplfnl */
        char unicodeCodeUnits[/* countUnicodeResults */];
        MBCSToUFallback toUFallbacks[/* countToUFallbacks */];

        /* fromUnicode */
        char fromUnicodeTable[];
        byte fromUnicodeBytes[];
        byte swapLFNLFromUnicodeBytes[]; /* for swaplfnl */
        int fromUBytesLength;
        short outputType, unicodeMask;

        /* converter name for swaplfnl */
        String swapLFNLName;

        /* extension data */
        UConverterSharedData baseSharedData;
        // int extIndexes[];
        ByteBuffer extIndexes; // create int[] view etc. as needed
        
        CharBuffer mbcsIndex;                     /* for fast conversion from most of BMP to MBCS (utf8Friendly data) */
        char sbcsIndex[/* SBCS_FAST_LIMIT>>6 */]; /* for fast conversion from low BMP to SBCS (utf8Friendly data) */
        boolean utf8Friendly;                     /* for utf8Friendly data */
        char maxFastUChar;                        /* for utf8Friendly data */

        /* roundtrips */
        long asciiRoundtrips;

        UConverterMBCSTable() {
            utf8Friendly = false;
            mbcsIndex = null;
            sbcsIndex = new char[SBCS_FAST_LIMIT>>6];
        }

        /*
         * UConverterMBCSTable(UConverterMBCSTable t) { countStates = t.countStates; dbcsOnlyState = t.dbcsOnlyState;
         * stateTableOwned = t.stateTableOwned; countToUFallbacks = t.countToUFallbacks; stateTable = t.stateTable;
         * swapLFNLStateTable = t.swapLFNLStateTable; unicodeCodeUnits = t.unicodeCodeUnits; toUFallbacks =
         * t.toUFallbacks; fromUnicodeTable = t.fromUnicodeTable; fromUnicodeBytes = t.fromUnicodeBytes;
         * swapLFNLFromUnicodeBytes = t.swapLFNLFromUnicodeBytes; fromUBytesLength = t.fromUBytesLength; outputType =
         * t.outputType; unicodeMask = t.unicodeMask; swapLFNLName = t.swapLFNLName; baseSharedData = t.baseSharedData;
         * extIndexes = t.extIndexes; }
         */
    }

    /* Constants used in MBCS data header */
    // enum {
        static final int MBCS_OPT_LENGTH_MASK=0x3f;
        static final int MBCS_OPT_NO_FROM_U=0x40;
        /*
         * If any of the following options bits are set,
         * then the file must be rejected.
         */
        static final int MBCS_OPT_INCOMPATIBLE_MASK=0xffc0;
        /*
         * Remove bits from this mask as more options are recognized
         * by all implementations that use this constant.
         */
        static final int MBCS_OPT_UNKNOWN_INCOMPATIBLE_MASK=0xff80;
    // };
    /* Constants for fast and UTF-8-friendly conversion. */
    // enum {
        static final int SBCS_FAST_MAX=0x0fff;               /* maximum code point with UTF-8-friendly SBCS runtime code, see makeconv SBCS_UTF8_MAX */
        static final int SBCS_FAST_LIMIT=SBCS_FAST_MAX+1;    /* =0x1000 */
        static final int MBCS_FAST_MAX=0xd7ff;               /* maximum code point with UTF-8-friendly MBCS runtime code, see makeconv MBCS_UTF8_MAX */
        static final int MBCS_FAST_LIMIT=MBCS_FAST_MAX+1;    /* =0xd800 */
    // };
    /**
     * MBCS data header. See data format description above.
     */
    final class MBCSHeader {
        byte version[/* U_MAX_VERSION_LENGTH */];
        int countStates, countToUFallbacks, offsetToUCodeUnits, offsetFromUTable, offsetFromUBytes;
        int flags;
        int fromUBytesLength;
        
        /* new and required in version 5 */
        int options;

        /* new and optional in version 5; used if options&MBCS_OPT_NO_FROM_U */
        int fullStage2Length;  /* number of 32-bit units */

        MBCSHeader() {
            version = new byte[MAX_VERSION_LENGTH];
        }
    }

    public CharsetMBCS(String icuCanonicalName, String javaCanonicalName, String[] aliases, String classPath,
            ClassLoader loader) throws InvalidFormatException {
        super(icuCanonicalName, javaCanonicalName, aliases);
        
        /* See if the icuCanonicalName contains certain option information. */
        if (icuCanonicalName.indexOf(UConverterConstants.OPTION_SWAP_LFNL_STRING) > -1) {
            options = UConverterConstants.OPTION_SWAP_LFNL;
            icuCanonicalName = icuCanonicalName.substring(0, icuCanonicalName.indexOf(UConverterConstants.OPTION_SWAP_LFNL_STRING));
            super.icuCanonicalName = icuCanonicalName;
        }
        
        // now try to load the data
        sharedData = loadConverter(1, icuCanonicalName, classPath, loader);

        maxBytesPerChar = sharedData.staticData.maxBytesPerChar;
        minBytesPerChar = sharedData.staticData.minBytesPerChar;
        maxCharsPerByte = 1;
        fromUSubstitution = sharedData.staticData.subChar;
        subChar = sharedData.staticData.subChar;
        subCharLen = sharedData.staticData.subCharLen;
        subChar1 = sharedData.staticData.subChar1;
        fromUSubstitution = new byte[sharedData.staticData.subCharLen];
        System.arraycopy(sharedData.staticData.subChar, 0, fromUSubstitution, 0, sharedData.staticData.subCharLen);
        
        initializeConverter(options);
    }

    public CharsetMBCS(String icuCanonicalName, String javaCanonicalName, String[] aliases)
            throws InvalidFormatException {
        this(icuCanonicalName, javaCanonicalName, aliases, ICUResourceBundle.ICU_BUNDLE, null);
    }

    private UConverterSharedData loadConverter(int nestedLoads, String myName, String classPath, ClassLoader loader)
            throws InvalidFormatException {
        boolean noFromU = false;
        // Read converter data from file
        UConverterStaticData staticData = new UConverterStaticData();
        UConverterDataReader reader = null;
        try {
            String resourceName = classPath + "/" + myName + "." + UConverterSharedData.DATA_TYPE;
            InputStream i;

            if (loader != null) {
                i = ICUData.getRequiredStream(loader, resourceName);
            } else {
                i = ICUData.getRequiredStream(resourceName);
            }
            BufferedInputStream b = new BufferedInputStream(i, UConverterConstants.CNV_DATA_BUFFER_SIZE);
            reader = new UConverterDataReader(b);
            reader.readStaticData(staticData);
        } catch (IOException e) {
            throw new InvalidFormatException();
        } catch (Exception e) {
            throw new InvalidFormatException();
        }

        UConverterSharedData data = null;
        int type = staticData.conversionType;

        if (type != UConverterSharedData.UConverterType.MBCS
                || staticData.structSize != UConverterStaticData.SIZE_OF_UCONVERTER_STATIC_DATA) {
            throw new InvalidFormatException();
        }

        data = new UConverterSharedData(1, null, false, 0);
        data.dataReader = reader;
        data.staticData = staticData;
        data.sharedDataCached = false;

        // Load data
        UConverterMBCSTable mbcsTable = data.mbcs;
        MBCSHeader header = new MBCSHeader();
        try {
            reader.readMBCSHeader(header);
        } catch (IOException e) {
            throw new InvalidFormatException();
        }

        int offset;
        // int[] extIndexesArray = null;
        String baseNameString = null;
        int[][] stateTableArray = null;
        MBCSToUFallback[] toUFallbacksArray = null;
        char[] unicodeCodeUnitsArray = null;
        char[] fromUnicodeTableArray = null;
        byte[] fromUnicodeBytesArray = null;

        if (header.version[0] == 5 && header.version[1] >= 3 && (header.options & MBCS_OPT_UNKNOWN_INCOMPATIBLE_MASK) == 0) {
            noFromU = ((header.options & MBCS_OPT_NO_FROM_U) != 0);
        } else if (header.version[0] != 4) {
            throw new InvalidFormatException();
        }

        mbcsTable.outputType = (byte) header.flags;

        /* extension data, header version 4.2 and higher */
        offset = header.flags >>> 8;
        // if(offset!=0 && mbcsTable.outputType == MBCS_OUTPUT_EXT_ONLY) {
        if (mbcsTable.outputType == MBCS_OUTPUT_EXT_ONLY) {
            try {
                baseNameString = reader.readBaseTableName();
                if (offset != 0) {
                    // agljport:commment subtract 32 for sizeof(_MBCSHeader) and length of baseNameString and 1 null
                    // terminator byte all already read;
                    mbcsTable.extIndexes = reader.readExtIndexes(offset
                            - (reader.bytesRead - reader.staticDataBytesRead));
                }
            } catch (IOException e) {
                throw new InvalidFormatException();
            }
        }

        // agljport:add this would be unnecessary if extIndexes were memory mapped
        /*
         * if(mbcsTable.extIndexes != null) {
         * 
         * try { //int nbytes = mbcsTable.extIndexes[UConverterExt.UCNV_EXT_TO_U_LENGTH]*4 +
         * mbcsTable.extIndexes[UConverterExt.UCNV_EXT_TO_U_UCHARS_LENGTH]*2 +
         * mbcsTable.extIndexes[UConverterExt.UCNV_EXT_FROM_U_LENGTH]*6 +
         * mbcsTable.extIndexes[UConverterExt.UCNV_EXT_FROM_U_BYTES_LENGTH] +
         * mbcsTable.extIndexes[UConverterExt.UCNV_EXT_FROM_U_STAGE_12_LENGTH]*2 +
         * mbcsTable.extIndexes[UConverterExt.UCNV_EXT_FROM_U_STAGE_3_LENGTH]*2 +
         * mbcsTable.extIndexes[UConverterExt.UCNV_EXT_FROM_U_STAGE_3B_LENGTH]*4; //int nbytes =
         * mbcsTable.extIndexes[UConverterExt.UCNV_EXT_SIZE] //byte[] extTables = dataReader.readExtTables(nbytes);
         * //mbcsTable.extTables = ByteBuffer.wrap(extTables); } catch(IOException e) { System.err.println("Caught
         * IOException: " + e.getMessage()); pErrorCode[0] = UErrorCode.U_INVALID_FORMAT_ERROR; return; } }
         */
        if (mbcsTable.outputType == MBCS_OUTPUT_EXT_ONLY) {
            UConverterSharedData baseSharedData = null;
            ByteBuffer extIndexes;
            String baseName;

            /* extension-only file, load the base table and set values appropriately */
            extIndexes = mbcsTable.extIndexes;
            if (extIndexes == null) {
                /* extension-only file without extension */
                throw new InvalidFormatException();
            }

            if (nestedLoads != 1) {
                /* an extension table must not be loaded as a base table */
                throw new InvalidFormatException();
            }

            /* load the base table */
            baseName = baseNameString;
            if (baseName.equals(staticData.name)) {
                /* forbid loading this same extension-only file */
                throw new InvalidFormatException();
            }

            // agljport:fix args.size=sizeof(UConverterLoadArgs);
            baseSharedData = loadConverter(2, baseName, classPath, loader);

            if (baseSharedData.staticData.conversionType != UConverterType.MBCS
                    || baseSharedData.mbcs.baseSharedData != null) {
                // agljport:fix ucnv_unload(baseSharedData);
                throw new InvalidFormatException();
            }

            /* copy the base table data */
            // agljport:comment deep copy in C changes mbcs through local reference mbcsTable; in java we probably don't
            // need the deep copy so can just make sure mbcs and its local reference both refer to the same new object
            mbcsTable = data.mbcs = baseSharedData.mbcs;

            /* overwrite values with relevant ones for the extension converter */
            mbcsTable.baseSharedData = baseSharedData;
            mbcsTable.extIndexes = extIndexes;

            /*
             * It would be possible to share the swapLFNL data with a base converter, but the generated name would have
             * to be different, and the memory would have to be free'd only once. It is easier to just create the data
             * for the extension converter separately when it is requested.
             */
            mbcsTable.swapLFNLStateTable = null;
            mbcsTable.swapLFNLFromUnicodeBytes = null;
            mbcsTable.swapLFNLName = null;

            /*
             * Set a special, runtime-only outputType if the extension converter is a DBCS version of a base converter
             * that also maps single bytes.
             */
            if (staticData.conversionType == UConverterType.DBCS
                    || (staticData.conversionType == UConverterType.MBCS && staticData.minBytesPerChar >= 2)) {

                if (baseSharedData.mbcs.outputType == MBCS_OUTPUT_2_SISO) {
                    /* the base converter is SI/SO-stateful */
                    int entry;

                    /* get the dbcs state from the state table entry for SO=0x0e */
                    entry = mbcsTable.stateTable[0][0xe];
                    if (MBCS_ENTRY_IS_FINAL(entry) && MBCS_ENTRY_FINAL_ACTION(entry) == MBCS_STATE_CHANGE_ONLY
                            && MBCS_ENTRY_FINAL_STATE(entry) != 0) {
                        mbcsTable.dbcsOnlyState = (byte) MBCS_ENTRY_FINAL_STATE(entry);

                        mbcsTable.outputType = MBCS_OUTPUT_DBCS_ONLY;
                    }
                } else if (baseSharedData.staticData.conversionType == UConverterType.MBCS
                        && baseSharedData.staticData.minBytesPerChar == 1
                        && baseSharedData.staticData.maxBytesPerChar == 2 && mbcsTable.countStates <= 127) {

                    /* non-stateful base converter, need to modify the state table */
                    int newStateTable[][/* 256 */];
                    int state[]; // this works because java 2-D array is array of references and we can have state =
                    // newStateTable[i];
                    int i, count;

                    /* allocate a new state table and copy the base state table contents */
                    count = mbcsTable.countStates;
                    newStateTable = new int[(count + 1) * 1024][256];

                    for (i = 0; i < mbcsTable.stateTable.length; ++i)
                        System.arraycopy(mbcsTable.stateTable[i], 0, newStateTable[i], 0,
                                mbcsTable.stateTable[i].length);

                    /* change all final single-byte entries to go to a new all-illegal state */
                    state = newStateTable[0];
                    for (i = 0; i < 256; ++i) {
                        if (MBCS_ENTRY_IS_FINAL(state[i])) {
                            state[i] = MBCS_ENTRY_TRANSITION(count, 0);
                        }
                    }

                    /* build the new all-illegal state */
                    state = newStateTable[count];
                    for (i = 0; i < 256; ++i) {
                        state[i] = MBCS_ENTRY_FINAL(0, MBCS_STATE_ILLEGAL, 0);
                    }
                    mbcsTable.stateTable = newStateTable;
                    mbcsTable.countStates = (byte) (count + 1);
                    mbcsTable.stateTableOwned = true;

                    mbcsTable.outputType = MBCS_OUTPUT_DBCS_ONLY;
                }
            }

            /*
             * unlike below for files with base tables, do not get the unicodeMask from the sharedData; instead, use the
             * base table's unicodeMask, which we copied in the memcpy above; this is necessary because the static data
             * unicodeMask, especially the UCNV_HAS_SUPPLEMENTARY flag, is part of the base table data
             */
        } else {
            /* conversion file with a base table; an additional extension table is optional */
            /* make sure that the output type is known */
            switch (mbcsTable.outputType) {
            case MBCS_OUTPUT_1:
            case MBCS_OUTPUT_2:
            case MBCS_OUTPUT_3:
            case MBCS_OUTPUT_4:
            case MBCS_OUTPUT_3_EUC:
            case MBCS_OUTPUT_4_EUC:
            case MBCS_OUTPUT_2_SISO:
                /* OK */
                break;
            default:
                throw new InvalidFormatException();
            }

            stateTableArray = new int[header.countStates][256];
            toUFallbacksArray = new MBCSToUFallback[header.countToUFallbacks];
            for (int i = 0; i < toUFallbacksArray.length; ++i)
                toUFallbacksArray[i] = new MBCSToUFallback();
            unicodeCodeUnitsArray = new char[(header.offsetFromUTable - header.offsetToUCodeUnits) / 2];
            fromUnicodeTableArray = new char[(header.offsetFromUBytes - header.offsetFromUTable) / 2];
            fromUnicodeBytesArray = new byte[header.fromUBytesLength];
            try {
                reader.readMBCSTable(stateTableArray, toUFallbacksArray, unicodeCodeUnitsArray, fromUnicodeTableArray,
                        fromUnicodeBytesArray);
            } catch (IOException e) {
                throw new InvalidFormatException();
            }

            mbcsTable.countStates = (byte) header.countStates;
            mbcsTable.countToUFallbacks = header.countToUFallbacks;
            mbcsTable.stateTable = stateTableArray;
            mbcsTable.toUFallbacks = toUFallbacksArray;
            mbcsTable.unicodeCodeUnits = unicodeCodeUnitsArray;

            mbcsTable.fromUnicodeTable = fromUnicodeTableArray;
            mbcsTable.fromUnicodeBytes = fromUnicodeBytesArray;
            mbcsTable.fromUBytesLength = header.fromUBytesLength;

            /*
             * converter versions 6.1 and up contain a unicodeMask that is used here to select the most efficient
             * function implementations
             */
            // agljport:fix info.size=sizeof(UDataInfo);
            // agljport:fix udata_getInfo((UDataMemory *)sharedData->dataMemory, &info);
            // agljport:fix if(info.formatVersion[0]>6 || (info.formatVersion[0]==6 && info.formatVersion[1]>=1)) {
            /* mask off possible future extensions to be safe */
            mbcsTable.unicodeMask = (short) (staticData.unicodeMask & 3);
            // agljport:fix } else {
            /* for older versions, assume worst case: contains anything possible (prevent over-optimizations) */
            // agljport:fix mbcsTable->unicodeMask=UCNV_HAS_SUPPLEMENTARY|UCNV_HAS_SURROGATES;
            // agljport:fix }
            if (offset != 0) {
                try {
                    // agljport:commment subtract 32 for sizeof(_MBCSHeader) and length of baseNameString and 1 null
                    // terminator byte all already read;
                    // int namelen = baseNameString != null? baseNameString.length() + 1: 0;
                    mbcsTable.extIndexes = reader.readExtIndexes(offset
                            - (reader.bytesRead - reader.staticDataBytesRead));
                } catch (IOException e) {
                    throw new InvalidFormatException();
                }
            }
            
            if (header.version[1] >= 3 && (mbcsTable.unicodeMask & UConverterConstants.HAS_SURROGATES) == 0 &&
                    (mbcsTable.countStates == 1 ? ((char)header.version[2] >= (SBCS_FAST_MAX>>8)) : ((char)header.version[2] >= (MBCS_FAST_MAX>>8)))) {
                mbcsTable.utf8Friendly = true;
                
                if (mbcsTable.countStates == 1) {
                    /*
                     * SBCS: Stage 3 is allocated in 64-entry blocks for U+0000..SBCS_FAST_MAX or higher.
                     * Build a table with indexes to each block, to be used instaed of
                     * the regular stage 1/2 table.
                     */
                    for (int i = 0; i < (SBCS_FAST_LIMIT>>6); ++i) {
                        mbcsTable.sbcsIndex[i] = mbcsTable.fromUnicodeTable[mbcsTable.fromUnicodeTable[i>>4]+((i<<2)&0x3c)];
                    }
                    /* set SBCS_FAST_MAX to reflect the reach of sbcsIndex[] even if header.version[2]>(SBCS_FAST_MAX>>8) */
                    mbcsTable.maxFastUChar = SBCS_FAST_MAX;
                } else {
                    /*
                     * MBCS: Stage 3 is allocated in 64-entry blocks for U+0000..MBCS_FAST_MAX or higher.
                     * The .cnv file is prebuilt with an additional stage table with indexes to each block.
                     */
                    if (noFromU) {
                        mbcsTable.mbcsIndex = ByteBuffer.wrap(mbcsTable.fromUnicodeBytes).asCharBuffer();
                    }
                    mbcsTable.maxFastUChar = (char)((header.version[2]<<8) | 0xff);
                }
            }
            /* calculate a bit set of 4 ASCII characters per bit that round-trip to ASCII bytes */
            {
                long asciiRoundtrips = 0xffffffff;
                for (int i = 0; i < 0x80; ++i) {
                    if (mbcsTable.stateTable[0][i] != MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, i)) {
                        asciiRoundtrips&=~((long)1<<(i>>2))&UConverterConstants.UNSIGNED_INT_MASK;
                    }
                }
                mbcsTable.asciiRoundtrips = asciiRoundtrips&UConverterConstants.UNSIGNED_INT_MASK;
            }
            
            if (noFromU) {
                int stage1Length = (mbcsTable.unicodeMask&UConverterConstants.HAS_SUPPLEMENTARY) != 0 ? 0x440 : 0x40;
                int stage2Length = (header.offsetFromUBytes - header.offsetFromUTable)/4 - stage1Length/2;
                reconstituteData(mbcsTable, stage1Length, stage2Length, header.fullStage2Length);
            }
            if (mbcsTable.outputType == MBCS_OUTPUT_DBCS_ONLY || mbcsTable.outputType == MBCS_OUTPUT_2_SISO) {
                /*
                 * MBCS_OUTPUT_DBCS_ONLY: No SBCS mappings, therefore ASCII does not roundtrip.
                 * MBCS_OUTPUT_2_SISO: Bypass the ASCII fastpath to handle prevLength correctly.
                 */
                mbcsTable.asciiRoundtrips = 0;
            }
        }
        return data;
    }
    
    private static boolean writeStage3Roundtrip(UConverterMBCSTable mbcsTable, long value, int codePoints[]) {
        char[] table;
        byte[] bytes;
        int stage2;
        int p;
        int c;
        int i, st3;
        long temp;

        table = mbcsTable.fromUnicodeTable;
        bytes = mbcsTable.fromUnicodeBytes;

        /* for EUC outputTypes, modify the value like genmbcs.c's transformEUC() */
        switch(mbcsTable.outputType) {
        case MBCS_OUTPUT_3_EUC:
            if(value<=0xffff) {
                /* short sequences are stored directly */
                /* code set 0 or 1 */
            } else if(value<=0x8effff) {
                /* code set 2 */
                value&=0x7fff;
            } else /* first byte is 0x8f */ {
                /* code set 3 */
                value&=0xff7f;
            }
            break;
        case MBCS_OUTPUT_4_EUC:
            if(value<=0xffffff) {
                /* short sequences are stored directly */
                /* code set 0 or 1 */
            } else if(value<=0x8effffff) {
                /* code set 2 */
                value&=0x7fffff;
            } else /* first byte is 0x8f */ {
                /* code set 3 */
                value&=0xff7fff;
            }
            break;
        default:
            break;
        }

        for(i=0; i<=0x1f; ++value, ++i) {
            c=codePoints[i];
            if(c<0) {
                continue;
            }

            /* locate the stage 2 & 3 data */
            stage2 = table[c>>10] + ((c>>4)&0x3f);
            st3 = table[stage2*2]<<16|table[stage2*2 + 1];
            st3 = (int)(char)(st3 * 16 + (c&0xf));

            /* write the codepage bytes into stage 3 */
            switch(mbcsTable.outputType) {
            case MBCS_OUTPUT_3:
            case MBCS_OUTPUT_4_EUC:
                p = st3*3;
                bytes[p] = (byte)(value>>16);
                bytes[p+1] = (byte)(value>>8);
                bytes[p+2] = (byte)value;
                break;
            case MBCS_OUTPUT_4:
                bytes[st3*4] = (byte)(value >> 24);
                bytes[st3*4 + 1] = (byte)(value >> 16);
                bytes[st3*4 + 2] = (byte)(value >> 8);
                bytes[st3*4 + 3] = (byte)value;
                break;
            default:
                /* 2 bytes per character */
                bytes[st3*2] = (byte)(value >> 8);
                bytes[st3*2 + 1] = (byte)value;
                break;
            }

            /* set the roundtrip flag */
            temp = (1L<<(16+(c&0xf)));
            table[stage2*2] |= (char)(temp>>16);
            table[stage2*2 + 1] |= (char)temp;
        }
        return true;
     }
    
    private static void reconstituteData(UConverterMBCSTable mbcsTable, int stage1Length, int stage2Length, int fullStage2Length) {
        int datalength = stage1Length*2+fullStage2Length*4+mbcsTable.fromUBytesLength;
        int offset = 0;
        byte[] stage = new byte[datalength];
        
        for (int i = 0; i < stage1Length; ++i) {
            stage[i*2]   = (byte)(mbcsTable.fromUnicodeTable[i]>>8);
            stage[i*2+1] = (byte)(mbcsTable.fromUnicodeTable[i]);
        }
        
        offset = ((fullStage2Length - stage2Length) * 4) + (stage1Length * 2);
        for (int i = 0; i < stage2Length; ++i) {
            stage[offset + i*4]   = (byte)(mbcsTable.fromUnicodeTable[stage1Length + i*2]>>8);
            stage[offset + i*4+1] = (byte)(mbcsTable.fromUnicodeTable[stage1Length + i*2]);
            stage[offset + i*4+2] = (byte)(mbcsTable.fromUnicodeTable[stage1Length + i*2+1]>>8);
            stage[offset + i*4+3] = (byte)(mbcsTable.fromUnicodeTable[stage1Length + i*2+1]);
        }
        
        /* indexes into stage 2 count from the bottom of the fromUnicodeTable */
        
        /* reconsitute the initial part of stage 2 from the mbcsIndex */
        {
            int stageUTF8Length=((int)(mbcsTable.maxFastUChar+1))>>6;
            int stageUTF8Index=0;
            int st1, st2, st3, i;
            
            for (st1 = 0; stageUTF8Index < stageUTF8Length; ++st1) {
                st2 = ((char)stage[2*st1]<<8) | stage[2*st1+1];
                if (st2 != stage1Length/2) {
                    /* each stage 2 block has 64 entries corresponding to 16 entries in the mbcsIndex */
                    for (i = 0; i < 16; ++i) {
                        st3 = mbcsTable.mbcsIndex.get(stageUTF8Index++);
                        if (st3 != 0) {
                            /* a stage 2 entry's index is per stage 3 16-block, not per stage 3 entry */
                            st3>>=4;
                            /*
                             * 4 stage 2 entries point to 4 consecutive stage 3 16-blocks which are
                             * allocated together as a single 64-block for access from the mbcsIndex
                             */
                            stage[4*st2] = (byte)(st3>>24); stage[4*st2+1] = (byte)(st3>>16); stage[4*st2+2] = (byte)(st3>>8); stage[4*st2+3] = (byte)(st3); st2++; st3++;
                            stage[4*st2] = (byte)(st3>>24); stage[4*st2+1] = (byte)(st3>>16); stage[4*st2+2] = (byte)(st3>>8); stage[4*st2+3] = (byte)(st3); st2++; st3++;
                            stage[4*st2] = (byte)(st3>>24); stage[4*st2+1] = (byte)(st3>>16); stage[4*st2+2] = (byte)(st3>>8); stage[4*st2+3] = (byte)(st3); st2++; st3++;
                            stage[4*st2] = (byte)(st3>>24); stage[4*st2+1] = (byte)(st3>>16); stage[4*st2+2] = (byte)(st3>>8); stage[4*st2+3] = (byte)(st3);
                        } else {
                            /* no stage 3 block, skip */
                            st2+=4;
                        }
                    }
                } else {
                    /* no stage 2 block, skip */
                    stageUTF8Index+=16;
                }
            }
        }
        
        char[] stage1 = new char[stage.length/2];
        for (int i = 0; i < stage1.length; ++i) {
            stage1[i] = (char)(((stage[i*2])<<8)|(stage[i*2+1] & UConverterConstants.UNSIGNED_BYTE_MASK));
        }
        byte[] stage2 = new byte[stage.length - ((stage1Length * 2) + (fullStage2Length * 4))];
        System.arraycopy(stage, ((stage1Length * 2) + (fullStage2Length * 4)), stage2, 0, stage2.length);
        
        mbcsTable.fromUnicodeTable = stage1;
        mbcsTable.fromUnicodeBytes = stage2;
        
        /* reconstitute fromUnicodeBytes with roundtrips from toUnicode data */
        MBCSEnumToUnicode(mbcsTable);
    }
    
    /*
     * Internal function enumerating the toUnicode data of an MBCS converter.
     * Currently only used for reconstituting data for a MBCS_OPT_NO_FROM_U
     * table, but could also be used for a future getUnicodeSet() option
     * that includes reverse fallbacks (after updating this function's implementation).
     * Currently only handles roundtrip mappings.
     * Does not currently handle extensions.
     */
    private static void MBCSEnumToUnicode(UConverterMBCSTable mbcsTable) {
        /*
         * Properties for each state, to speed up the enumeration.
         * Ignorable actions are unassigned/illegal/state-change-only:
         * They do not lead to mappings.
         * 
         * Bits 7..6
         * 1 direct/initial state (stateful converters have mulitple)
         * 0 non-initial state with transitions or with nonignorable result actions
         * -1 final state with only ignorable actions
         * 
         * Bits 5..3
         * The lowest byte value with non-ignorable actions is
         * value<<5 (rounded down).
         * 
         * Bits 2..0:
         * The highest byte value with non-ignorable actions is
         * (value<<5)&0x1f (rounded up).
         */
        byte stateProps[] = new byte[MBCS_MAX_STATE_COUNT];
        int state;
        
        /* recurse from state 0 and set all stateProps */
        getStateProp(mbcsTable.stateTable, stateProps, 0);
        
        for (state = 0; state < mbcsTable.countStates; ++state) {
            if (stateProps[state] >= 0x40) {
                /* start from each direct state */
                enumToU(mbcsTable, stateProps, state, 0, 0);
            }
        }
        
        
    }
    
    private static boolean enumToU(UConverterMBCSTable mbcsTable, byte stateProps[], int state, int offset, int value) {
        int[] codePoints = new int[32];
        int[] row;
        char[] unicodeCodeUnits;
        int anyCodePoints;
        int b, limit;
        
        row = mbcsTable.stateTable[state];
        unicodeCodeUnits = mbcsTable.unicodeCodeUnits;
        
        value<<=8;
        anyCodePoints = -1; /* becomes non-negative if there is a mapping */
        
        b = (stateProps[state]&0x38)<<2;
        if (b == 0 && stateProps[state] >= 0x40) {
            /* skip byte sequences with leading zeros because they are note stored in the fromUnicode table */
            codePoints[0] = UConverterConstants.U_SENTINEL;
            b = 1;
        }
        limit = ((stateProps[state]&7)+1)<<5;
        while (b < limit) {
            int entry = row[b];
            if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                int nextState = MBCS_ENTRY_TRANSITION_STATE(entry);
                if (stateProps[nextState] >= 0) {
                    /* recurse to a state with non-ignorable actions */
                    if (!enumToU(mbcsTable, stateProps, nextState, offset+MBCS_ENTRY_TRANSITION_OFFSET(entry), value|b)) {
                        return false;
                    }
                }
                codePoints[b&0x1f] = UConverterConstants.U_SENTINEL;
            } else {
                int c;
                int action;
                
                /*
                 * An if-else-if chain provides more reliable performance for
                 * the most common cases compared to a switch.
                 */
                action = MBCS_ENTRY_FINAL_ACTION(entry);
                if (action == MBCS_STATE_VALID_DIRECT_16) {
                    /* output BMP code point */
                    c = (char)MBCS_ENTRY_FINAL_VALUE_16(entry);
                } else if (action == MBCS_STATE_VALID_16) {
                    int finalOffset = offset+MBCS_ENTRY_FINAL_VALUE_16(entry);
                    c = unicodeCodeUnits[finalOffset];
                    if (c < 0xfffe) {
                        /* output BMP code point */
                    } else {
                        c = UConverterConstants.U_SENTINEL;
                    }
                } else if (action == MBCS_STATE_VALID_16_PAIR) {
                    int finalOffset = offset+MBCS_ENTRY_FINAL_VALUE_16(entry);
                    c = unicodeCodeUnits[finalOffset++];
                    if (c < 0xd800) {
                        /* output BMP code point below 0xd800 */
                    } else if (c <= 0xdbff) {
                        /* output roundtrip or fallback supplementary code point */
                        c = ((c&0x3ff)<<10)+unicodeCodeUnits[finalOffset]+(0x10000-0xdc00);
                    } else if (c == 0xe000) {
                        /* output roundtrip BMP code point above 0xd800 or fallback BMP code point */
                        c = unicodeCodeUnits[finalOffset];
                    } else {
                        c = UConverterConstants.U_SENTINEL;
                    }
                } else if (action == MBCS_STATE_VALID_DIRECT_20) {
                    /* output supplementary code point */
                    c = (int)(MBCS_ENTRY_FINAL_VALUE(entry)+0x10000);
                } else {
                    c = UConverterConstants.U_SENTINEL;
                }
                
                codePoints[b&0x1f] = c;
                anyCodePoints&=c;
            }
            if (((++b)&0x1f) == 0) {
                if(anyCodePoints>=0) {
                    if(!writeStage3Roundtrip(mbcsTable, value|(b-0x20)&UConverterConstants.UNSIGNED_INT_MASK, codePoints)) {
                        return false;
                    }
                    anyCodePoints=-1;
                }
            }
        }
        
        return true;
    }
    
    /*
     * Only called if stateProps[state]==-1.
     * A recursive call may do stateProps[state]|=0x40 if this state is the target of an
     * MBCS_STATE_CHANGE_ONLY.
     */
    private static byte getStateProp(int stateTable[][], byte stateProps[], int state) {
        int[] row;
        int min, max, entry, nextState;
        
        row = stateTable[state];
        stateProps[state] = 0;
        
        /* find first non-ignorable state */
        for (min = 0;;++min) {
            entry = row[min];
            nextState = MBCS_ENTRY_STATE(entry);
            if (stateProps[nextState] == -1) {
                getStateProp(stateTable, stateProps, nextState);
            }
            if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                if (stateProps[nextState] >- 0) {
                    break;
                }
            } else if (MBCS_ENTRY_FINAL_ACTION(entry) < MBCS_STATE_UNASSIGNED) {
                break;
            }
            if (min == 0xff) {
                stateProps[state] = -0x40;  /* (byte)0xc0 */
                return stateProps[state];
            }
        }
        stateProps[state]|=(byte)((min>>5)<<3);
        
        /* find last non-ignorable state */
        for (max = 0xff; min < max; --max) {
            entry = row[max];
            nextState = MBCS_ENTRY_STATE(entry);
            if (stateProps[nextState] == -1) {
                getStateProp(stateTable, stateProps, nextState);
            }
            if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                if (stateProps[nextState] >- 0) {
                    break;
                }
            } else if (MBCS_ENTRY_FINAL_ACTION(entry) < MBCS_STATE_UNASSIGNED) {
                break;
            }
        }
        stateProps[state]|=(byte)(max>>5);
        
        /* recurse further and collect direct-state information */
        while (min <= max) {
            entry = row[min];
            nextState = MBCS_ENTRY_STATE(entry);
            if (stateProps[nextState] == -1) {
                getStateProp(stateTable, stateProps, nextState);
            }
            if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                stateProps[nextState]|=0x40;
                if (MBCS_ENTRY_FINAL_ACTION(entry) <= MBCS_STATE_FALLBACK_DIRECT_20) {
                    stateProps[state]|=0x40;
                }
            }
            ++min;
        }
        return stateProps[state];
    }

    protected void initializeConverter(int myOptions) {
        UConverterMBCSTable mbcsTable;
        ByteBuffer extIndexes;
        short outputType;
        byte maxBytesPerUChar;

        mbcsTable = sharedData.mbcs;
        outputType = mbcsTable.outputType;

        if (outputType == MBCS_OUTPUT_DBCS_ONLY) {
            /* the swaplfnl option does not apply, remove it */
            this.options = myOptions &= ~UConverterConstants.OPTION_SWAP_LFNL;
        }

        if ((myOptions & UConverterConstants.OPTION_SWAP_LFNL) != 0) {
            /* do this because double-checked locking is broken */
            boolean isCached;

            // agljport:todo umtx_lock(NULL);
            isCached = mbcsTable.swapLFNLStateTable != null;
            // agljport:todo umtx_unlock(NULL);

            if (!isCached) {
                try {
                    if (!EBCDICSwapLFNL()) {
                        /* this option does not apply, remove it */
                        this.options = myOptions &= ~UConverterConstants.OPTION_SWAP_LFNL;
                    }
                } catch (Exception e) {
                    /* something went wrong. */
                    return;
                }
            }
        }

        if (icuCanonicalName.toLowerCase().indexOf("gb18030") >= 0) {
            /* set a flag for GB 18030 mode, which changes the callback behavior */
            this.options |= MBCS_OPTION_GB18030;
        }

        /* fix maxBytesPerUChar depending on outputType and options etc. */
        if (outputType == MBCS_OUTPUT_2_SISO) {
            maxBytesPerChar = 3; /* SO+DBCS */
        }

        extIndexes = mbcsTable.extIndexes;
        if (extIndexes != null) {
            maxBytesPerUChar = (byte) GET_MAX_BYTES_PER_UCHAR(extIndexes);
            if (outputType == MBCS_OUTPUT_2_SISO) {
                ++maxBytesPerUChar; /* SO + multiple DBCS */
            }

            if (maxBytesPerUChar > maxBytesPerChar) {
                maxBytesPerChar = maxBytesPerUChar;
            }
        }
    }
     /* EBCDIC swap LF<->NL--------------------------------------------------------------------------------*/
     /*
      * This code modifies a standard EBCDIC<->Unicode mappling table for
      * OS/390 (z/OS) Unix System Services (Open Edition).
      * The difference is in the mapping of Line Feed and New Line control codes:
      * Standard EBDIC maps
      * 
      * <U000A> \x25 |0
      * <U0085> \x15 |0
      * 
      * but OS/390 USS EBCDIC swaps the control codes for LF and NL,
      * mapping
      * 
      * <U000A> \x15 |0
      * <U0085> \x25 |0
      * 
      * This code modifies a loaded standard EBCDIC<->Unicode mapping table
      * by copying it into allocated memory and swapping the LF and NL values.
      * It allows to support the same EBCDIC charset in both version without
      * duplicating the entire installed table.
      */
    /* standard EBCDIC codes */
    private static final short EBCDIC_LF = 0x0025;
    private static final short EBCDIC_NL = 0x0015;
    
    /* standard EBCDIC codes with roundtrip flag as stored in Unicode-to-single-byte tables */
    private static final short EBCDIC_RT_LF = 0x0f25;
    private static final short EBCDIC_RT_NL = 0x0f15;
    
    /* Unicode code points */
    private static final short U_LF = 0x000A;
    private static final short U_NL = 0x0085;
    
    private boolean EBCDICSwapLFNL() throws Exception {
        UConverterMBCSTable mbcsTable;
        
        char[] table;
        byte[] results;
        byte[] bytes;
        
        int[][] newStateTable;
        byte[] newResults;
        String newName;
        
        int stage2Entry;
//        int size;
        int sizeofFromUBytes;
        
        mbcsTable = sharedData.mbcs;
        
        table = mbcsTable.fromUnicodeTable;
        bytes = mbcsTable.fromUnicodeBytes;
        results = bytes;
        
        /*
         * Check that this is an EBCDIC table with SBCS portion -
         * SBCS or EBCDIC with standard EBCDIC LF and NL mappings.
         * 
         * If not, ignore the option Options are always ignored if they do not apply.
         */
        if (!((mbcsTable.outputType == MBCS_OUTPUT_1 || mbcsTable.outputType == MBCS_OUTPUT_2_SISO) &&
              mbcsTable.stateTable[0][EBCDIC_LF] == MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, U_LF) &&
              mbcsTable.stateTable[0][EBCDIC_NL] == MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, U_NL))) {
            return false;
        }
        
        if (mbcsTable.outputType == MBCS_OUTPUT_1) {
            if (!(EBCDIC_RT_LF == MBCS_SINGLE_RESULT_FROM_U(table, results, U_LF) &&
                  EBCDIC_RT_NL == MBCS_SINGLE_RESULT_FROM_U(table, results, U_NL))) {
                return false;
            }
        } else /* MBCS_OUTPUT_2_SISO */ {
            stage2Entry = MBCS_STAGE_2_FROM_U(table, U_LF);
            if (!(MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, U_LF) &&
                  EBCDIC_LF == MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, U_LF))) {
                return false;
            }
            
            stage2Entry = MBCS_STAGE_2_FROM_U(table, U_NL);
            if (!(MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, U_NL) &&
                  EBCDIC_NL == MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, U_NL))) {
                return false;
            }
        }
        
        if (mbcsTable.fromUBytesLength > 0) {
            /*
             * We _know_ the number of bytes in the fromUnicodeBytes array
             * starting with header.version 4.1.
             */
            sizeofFromUBytes = mbcsTable.fromUBytesLength;
        } else {
            /*
             * Otherwise:
             * There used to be code to enumerate the fromUnicode
             * trie and find the highest entry, but it was removed in ICU 3.2
             * because it was not tested and caused a low code coverage number.
             */
            throw new Exception("U_INVALID_FORMAT_ERROR");
        }
        
        /*
         * The table has an appropriate format.
         * Allocate and build
         * - a modified to-Unicode state table
         * - a modified from-Unicode output array
         * - a converter name string with the swap option appended
         */
//        size = mbcsTable.countStates * 1024 + sizeofFromUBytes + UConverterConstants.MAX_CONVERTER_NAME_LENGTH + 20;
        
        /* copy and modify the to-Unicode state table */
        newStateTable = new int[mbcsTable.stateTable.length][mbcsTable.stateTable[0].length];
        for (int i = 0; i < newStateTable.length; i++) {
            System.arraycopy(mbcsTable.stateTable[i], 0, newStateTable[i], 0, newStateTable[i].length);
        }
        
        newStateTable[0][EBCDIC_LF] = MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, U_NL);
        newStateTable[0][EBCDIC_NL] = MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, U_LF);
        
        /* copy and modify the from-Unicode result table */
        newResults = new byte[sizeofFromUBytes];
        System.arraycopy(bytes, 0, newResults, 0, sizeofFromUBytes);
        /* conveniently, the table access macros work on the left side of expressions */
        if (mbcsTable.outputType == MBCS_OUTPUT_1) {
            MBCS_SINGLE_RESULT_FROM_U_SET(table, newResults, U_LF, EBCDIC_RT_NL);
            MBCS_SINGLE_RESULT_FROM_U_SET(table, newResults, U_NL, EBCDIC_RT_LF);
        } else /* MBCS_OUTPUT_2_SISO */ {
            stage2Entry = MBCS_STAGE_2_FROM_U(table, U_LF);
            MBCS_VALUE_2_FROM_STAGE_2_SET(newResults, stage2Entry, U_LF, EBCDIC_NL);
            
            stage2Entry = MBCS_STAGE_2_FROM_U(table, U_NL);
            MBCS_VALUE_2_FROM_STAGE_2_SET(newResults, stage2Entry, U_NL, EBCDIC_LF);
        }
        
        /* set the canonical converter name */
        newName = new String(icuCanonicalName);
        newName.concat(UConverterConstants.OPTION_SWAP_LFNL_STRING);
        
        if (mbcsTable.swapLFNLStateTable == null) {
            mbcsTable.swapLFNLStateTable = newStateTable;
            mbcsTable.swapLFNLFromUnicodeBytes = newResults;
            mbcsTable.swapLFNLName = newName;
        }
        return true;
    }

    /**
     * MBCS output types for conversions from Unicode. These per-converter types determine the storage method in stage 3
     * of the lookup table, mostly how many bytes are stored per entry.
     */
    static final int MBCS_OUTPUT_1 = 0; /* 0 */
    static final int MBCS_OUTPUT_2 = MBCS_OUTPUT_1 + 1; /* 1 */
    static final int MBCS_OUTPUT_3 = MBCS_OUTPUT_2 + 1; /* 2 */
    static final int MBCS_OUTPUT_4 = MBCS_OUTPUT_3 + 1; /* 3 */
    static final int MBCS_OUTPUT_3_EUC = 8; /* 8 */
    static final int MBCS_OUTPUT_4_EUC = MBCS_OUTPUT_3_EUC + 1; /* 9 */
    static final int MBCS_OUTPUT_2_SISO = 12; /* c */
    static final int MBCS_OUTPUT_2_HZ = MBCS_OUTPUT_2_SISO + 1; /* d */
    static final int MBCS_OUTPUT_EXT_ONLY = MBCS_OUTPUT_2_HZ + 1; /* e */
    // static final int MBCS_OUTPUT_COUNT = MBCS_OUTPUT_EXT_ONLY + 1;
    static final int MBCS_OUTPUT_DBCS_ONLY = 0xdb; /* runtime-only type for DBCS-only handling of SISO tables */

    /* GB 18030 data ------------------------------------------------------------ */

    /* helper macros for linear values for GB 18030 four-byte sequences */
    private static long LINEAR_18030(long a, long b, long c, long d) {
        return ((((a & 0xff) * 10 + (b & 0xff)) * 126L + (c & 0xff)) * 10L + (d & 0xff));
    }

    private static long LINEAR_18030_BASE = LINEAR_18030(0x81, 0x30, 0x81, 0x30);

    private static long LINEAR(long x) {
        return LINEAR_18030(x >>> 24, (x >>> 16) & 0xff, (x >>> 8) & 0xff, x & 0xff);
    }

    /*
     * Some ranges of GB 18030 where both the Unicode code points and the GB four-byte sequences are contiguous and are
     * handled algorithmically by the special callback functions below. The values are start & end of Unicode & GB
     * codes.
     * 
     * Note that single surrogates are not mapped by GB 18030 as of the re-released mapping tables from 2000-nov-30.
     */
    private static final long gb18030Ranges[][] = new long[/* 13 */][/* 4 */] {
            { 0x10000L, 0x10FFFFL, LINEAR(0x90308130L), LINEAR(0xE3329A35L) },
            { 0x9FA6L, 0xD7FFL, LINEAR(0x82358F33L), LINEAR(0x8336C738L) },
            { 0x0452L, 0x200FL, LINEAR(0x8130D330L), LINEAR(0x8136A531L) },
            { 0xE865L, 0xF92BL, LINEAR(0x8336D030L), LINEAR(0x84308534L) },
            { 0x2643L, 0x2E80L, LINEAR(0x8137A839L), LINEAR(0x8138FD38L) },
            { 0xFA2AL, 0xFE2FL, LINEAR(0x84309C38L), LINEAR(0x84318537L) },
            { 0x3CE1L, 0x4055L, LINEAR(0x8231D438L), LINEAR(0x8232AF32L) },
            { 0x361BL, 0x3917L, LINEAR(0x8230A633L), LINEAR(0x8230F237L) },
            { 0x49B8L, 0x4C76L, LINEAR(0x8234A131L), LINEAR(0x8234E733L) },
            { 0x4160L, 0x4336L, LINEAR(0x8232C937L), LINEAR(0x8232F837L) },
            { 0x478EL, 0x4946L, LINEAR(0x8233E838L), LINEAR(0x82349638L) },
            { 0x44D7L, 0x464BL, LINEAR(0x8233A339L), LINEAR(0x8233C931L) },
            { 0xFFE6L, 0xFFFFL, LINEAR(0x8431A234L), LINEAR(0x8431A439L) } };

    /* bit flag for UConverter.options indicating GB 18030 special handling */
    private static final int MBCS_OPTION_GB18030 = 0x8000;

    // enum {
        static final int MBCS_MAX_STATE_COUNT = 128;
    // };
    /**
     * MBCS action codes for conversions to Unicode. These values are in bits 23..20 of the state table entries.
     */
    static final int MBCS_STATE_VALID_DIRECT_16 = 0;
    static final int MBCS_STATE_VALID_DIRECT_20 = MBCS_STATE_VALID_DIRECT_16 + 1;
    static final int MBCS_STATE_FALLBACK_DIRECT_16 = MBCS_STATE_VALID_DIRECT_20 + 1;
    static final int MBCS_STATE_FALLBACK_DIRECT_20 = MBCS_STATE_FALLBACK_DIRECT_16 + 1;
    static final int MBCS_STATE_VALID_16 = MBCS_STATE_FALLBACK_DIRECT_20 + 1;
    static final int MBCS_STATE_VALID_16_PAIR = MBCS_STATE_VALID_16 + 1;
    static final int MBCS_STATE_UNASSIGNED = MBCS_STATE_VALID_16_PAIR + 1;
    static final int MBCS_STATE_ILLEGAL = MBCS_STATE_UNASSIGNED + 1;
    static final int MBCS_STATE_CHANGE_ONLY = MBCS_STATE_ILLEGAL + 1;
    
    static int MBCS_ENTRY_SET_STATE(int entry, int state) { 
        return (int)(((entry)&0x80ffffff)|((int)(state)<<24L));
    }

    static int MBCS_ENTRY_STATE(int entry) {
        return (((entry)>>24)&0x7f);
    }

    /* Methods for state table entries */
    static int MBCS_ENTRY_TRANSITION(int state, int offset) {
        return (state << 24L) | offset;
    }

    static int MBCS_ENTRY_FINAL(int state, int action, int value) {
        return (int) (0x80000000 | ((int) (state) << 24L) | ((action) << 20L) | (value));
    }

    static boolean MBCS_ENTRY_IS_TRANSITION(int entry) {
        return (entry) >= 0;
    }

    static boolean MBCS_ENTRY_IS_FINAL(int entry) {
        return (entry) < 0;
    }

    static int MBCS_ENTRY_TRANSITION_STATE(int entry) {
        return ((entry) >>> 24);
    }

    static int MBCS_ENTRY_TRANSITION_OFFSET(int entry) {
        return ((entry) & 0xffffff);
    }

    static int MBCS_ENTRY_FINAL_STATE(int entry) {
        return ((entry) >>> 24) & 0x7f;
    }

    static boolean MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(int entry) {
        return ((entry) < 0x80100000);
    }

    static int MBCS_ENTRY_FINAL_ACTION(int entry) {
        return ((entry) >>> 20) & 0xf;
    }

    static int MBCS_ENTRY_FINAL_VALUE(int entry) {
        return ((entry) & 0xfffff);
    }

    static char MBCS_ENTRY_FINAL_VALUE_16(int entry) {
        return (char) (entry);
    }
    
    static boolean MBCS_IS_ASCII_ROUNDTRIP(int b, long asciiRoundtrips) {
        return (((asciiRoundtrips) & (1<<((b)>>2)))!=0);
    }
    
    /**
     * This macro version of _MBCSSingleSimpleGetNextUChar() gets a code point from a byte. It works for single-byte,
     * single-state codepages that only map to and from BMP code points, and it always returns fallback values.
     */
    static char MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(UConverterMBCSTable mbcs, final int b) {
        return MBCS_ENTRY_FINAL_VALUE_16(mbcs.stateTable[0][b & UConverterConstants.UNSIGNED_BYTE_MASK]);
    }

    /* single-byte fromUnicode: get the 16-bit result word */
    static char MBCS_SINGLE_RESULT_FROM_U(char[] table, byte[] results, int c) {
        int i1 = table[c >>> 10] + ((c >>> 4) & 0x3f);
        int i = 2 * (table[i1] + (c & 0xf)); // used as index into byte[] array treated as char[] array
        return (char) (((results[i] & UConverterConstants.UNSIGNED_BYTE_MASK) << 8) | (results[i + 1] & UConverterConstants.UNSIGNED_BYTE_MASK));
    }
    
    /* single-byte fromUnicode: set the 16-bit result word with newValue*/
    static void MBCS_SINGLE_RESULT_FROM_U_SET(char[] table, byte[] results, int c, int newValue) {
        int i1 = table[c >>> 10] + ((c >>> 4) & 0x3f);
        int i = 2 * (table[i1] + (c & 0xf)); // used as index into byte[] array treated as char[] array
        results[i] = (byte)((newValue >> 8) & UConverterConstants.UNSIGNED_BYTE_MASK);
        results[i + 1] =  (byte)(newValue & UConverterConstants.UNSIGNED_BYTE_MASK);
    }

    /* multi-byte fromUnicode: get the 32-bit stage 2 entry */
    static int MBCS_STAGE_2_FROM_U(char[] table, int c) {
        int i = 2 * (table[(c) >>> 10] + ((c >>> 4) & 0x3f)); // 2x because used as index into char[] array treated as
        // int[] array
        return ((table[i] & UConverterConstants.UNSIGNED_SHORT_MASK) << 16)
                | (table[i + 1] & UConverterConstants.UNSIGNED_SHORT_MASK);
    }

    private static boolean MBCS_FROM_U_IS_ROUNDTRIP(int stage2Entry, int c) {
        return (((stage2Entry) & (1 << (16 + ((c) & 0xf)))) != 0);
    }

    static char MBCS_VALUE_2_FROM_STAGE_2(byte[] bytes, int stage2Entry, int c) {
        int i = 2 * (16 * ((char) stage2Entry & UConverterConstants.UNSIGNED_SHORT_MASK) + (c & 0xf));
        return (char) (((bytes[i] & UConverterConstants.UNSIGNED_BYTE_MASK) << 8) | (bytes[i + 1] & UConverterConstants.UNSIGNED_BYTE_MASK));
    }
    
    static void MBCS_VALUE_2_FROM_STAGE_2_SET(byte[] bytes, int stage2Entry, int c, int newValue) {
        int i = 2 * (16 * ((char) stage2Entry & UConverterConstants.UNSIGNED_SHORT_MASK) + (c & 0xf));
        bytes[i] = (byte)((newValue >> 8) & UConverterConstants.UNSIGNED_BYTE_MASK);
        bytes[i + 1] = (byte)(newValue & UConverterConstants.UNSIGNED_BYTE_MASK);
    }

    private static int MBCS_VALUE_4_FROM_STAGE_2(byte[] bytes, int stage2Entry, int c) {
        int i = 4 * (16 * ((char) stage2Entry & UConverterConstants.UNSIGNED_SHORT_MASK) + (c & 0xf));
        return ((bytes[i] & UConverterConstants.UNSIGNED_BYTE_MASK) << 24)
                | ((bytes[i + 1] & UConverterConstants.UNSIGNED_BYTE_MASK) << 16)
                | ((bytes[i + 2] & UConverterConstants.UNSIGNED_BYTE_MASK) << 8)
                | (bytes[i + 3] & UConverterConstants.UNSIGNED_BYTE_MASK);
    }

    static int MBCS_POINTER_3_FROM_STAGE_2(byte[] bytes, int stage2Entry, int c) {
        return ((16 * ((char) (stage2Entry) & UConverterConstants.UNSIGNED_SHORT_MASK) + ((c) & 0xf)) * 3);
    }

    // ------------UConverterExt-------------------------------------------------------

    static final int EXT_INDEXES_LENGTH = 0; /* 0 */

    static final int EXT_TO_U_INDEX = EXT_INDEXES_LENGTH + 1; /* 1 */
    static final int EXT_TO_U_LENGTH = EXT_TO_U_INDEX + 1;
    static final int EXT_TO_U_UCHARS_INDEX = EXT_TO_U_LENGTH + 1;
    static final int EXT_TO_U_UCHARS_LENGTH = EXT_TO_U_UCHARS_INDEX + 1;

    static final int EXT_FROM_U_UCHARS_INDEX = EXT_TO_U_UCHARS_LENGTH + 1; /* 5 */
    static final int EXT_FROM_U_VALUES_INDEX = EXT_FROM_U_UCHARS_INDEX + 1;
    static final int EXT_FROM_U_LENGTH = EXT_FROM_U_VALUES_INDEX + 1;
    static final int EXT_FROM_U_BYTES_INDEX = EXT_FROM_U_LENGTH + 1;
    static final int EXT_FROM_U_BYTES_LENGTH = EXT_FROM_U_BYTES_INDEX + 1;

    static final int EXT_FROM_U_STAGE_12_INDEX = EXT_FROM_U_BYTES_LENGTH + 1; /* 10 */
    static final int EXT_FROM_U_STAGE_1_LENGTH = EXT_FROM_U_STAGE_12_INDEX + 1;
    static final int EXT_FROM_U_STAGE_12_LENGTH = EXT_FROM_U_STAGE_1_LENGTH + 1;
    static final int EXT_FROM_U_STAGE_3_INDEX = EXT_FROM_U_STAGE_12_LENGTH + 1;
    static final int EXT_FROM_U_STAGE_3_LENGTH = EXT_FROM_U_STAGE_3_INDEX + 1;
    static final int EXT_FROM_U_STAGE_3B_INDEX = EXT_FROM_U_STAGE_3_LENGTH + 1;
    static final int EXT_FROM_U_STAGE_3B_LENGTH = EXT_FROM_U_STAGE_3B_INDEX + 1;

    private static final int EXT_COUNT_BYTES = EXT_FROM_U_STAGE_3B_LENGTH + 1; /* 17 */
    // private static final int EXT_COUNT_UCHARS = EXT_COUNT_BYTES + 1;
    // private static final int EXT_FLAGS = EXT_COUNT_UCHARS + 1;
    //
    // private static final int EXT_RESERVED_INDEX = EXT_FLAGS + 1; /* 20, moves with additional indexes */
    //
    // private static final int EXT_SIZE=31;
    // private static final int EXT_INDEXES_MIN_LENGTH=32;

    static final int EXT_FROM_U_MAX_DIRECT_LENGTH = 3;

    /* toUnicode helpers -------------------------------------------------------- */

    private static final int TO_U_BYTE_SHIFT = 24;
    private static final int TO_U_VALUE_MASK = 0xffffff;
    private static final int TO_U_MIN_CODE_POINT = 0x1f0000;
    private static final int TO_U_MAX_CODE_POINT = 0x2fffff;
    private static final int TO_U_ROUNDTRIP_FLAG = (1 << 23);
    private static final int TO_U_INDEX_MASK = 0x3ffff;
    private static final int TO_U_LENGTH_SHIFT = 18;
    private static final int TO_U_LENGTH_OFFSET = 12;

    /* maximum number of indexed UChars */
    static final int MAX_UCHARS = 19;

    static int TO_U_GET_BYTE(int word) {
        return word >>> TO_U_BYTE_SHIFT;
    }

    static int TO_U_GET_VALUE(int word) {
        return word & TO_U_VALUE_MASK;
    }

    static boolean TO_U_IS_ROUNDTRIP(int value) {
        return (value & TO_U_ROUNDTRIP_FLAG) != 0;
    }

    static boolean TO_U_IS_PARTIAL(int value) {
        return (value & UConverterConstants.UNSIGNED_INT_MASK) < TO_U_MIN_CODE_POINT;
    }

    static int TO_U_GET_PARTIAL_INDEX(int value) {
        return value;
    }

    static int TO_U_MASK_ROUNDTRIP(int value) {
        return value & ~TO_U_ROUNDTRIP_FLAG;
    }

    private static int TO_U_MAKE_WORD(byte b, int value) {
        return ((b & UConverterConstants.UNSIGNED_BYTE_MASK) << TO_U_BYTE_SHIFT) | value;
    }

    /* use after masking off the roundtrip flag */
    static boolean TO_U_IS_CODE_POINT(int value) {
        return (value & UConverterConstants.UNSIGNED_INT_MASK) <= TO_U_MAX_CODE_POINT;
    }

    static int TO_U_GET_CODE_POINT(int value) {
        return (int) ((value & UConverterConstants.UNSIGNED_INT_MASK) - TO_U_MIN_CODE_POINT);
    }

    private static int TO_U_GET_INDEX(int value) {
        return value & TO_U_INDEX_MASK;
    }

    private static int TO_U_GET_LENGTH(int value) {
        return (value >>> TO_U_LENGTH_SHIFT) - TO_U_LENGTH_OFFSET;
    }

    /* fromUnicode helpers ------------------------------------------------------ */

    /* most trie constants are shared with ucnvmbcs.h */
    private static final int STAGE_2_LEFT_SHIFT = 2;

    // private static final int STAGE_3_GRANULARITY = 4;

    /* trie access, returns the stage 3 value=index to stage 3b; s1Index=c>>10 */
    static int FROM_U(CharBuffer stage12, CharBuffer stage3, int s1Index, int c) {
        return stage3.get(((int) stage12.get((stage12.get(s1Index) + ((c >>> 4) & 0x3f))) << STAGE_2_LEFT_SHIFT)
                + (c & 0xf));
    }

    private static final int FROM_U_LENGTH_SHIFT = 24;
    private static final int FROM_U_ROUNDTRIP_FLAG = 1 << 31;
    static final int FROM_U_RESERVED_MASK = 0x60000000;
    private static final int FROM_U_DATA_MASK = 0xffffff;

    /* special value for "no mapping" to <subchar1> (impossible roundtrip to 0 bytes, value 01) */
    static final int FROM_U_SUBCHAR1 = 0x80000001;

    /* at most 3 bytes in the lower part of the value */
    private static final int FROM_U_MAX_DIRECT_LENGTH = 3;

    /* maximum number of indexed bytes */
    static final int MAX_BYTES = 0x1f;

    static boolean FROM_U_IS_PARTIAL(int value) {
        return (value >>> FROM_U_LENGTH_SHIFT) == 0;
    }

    static int FROM_U_GET_PARTIAL_INDEX(int value) {
        return value;
    }

    static boolean FROM_U_IS_ROUNDTRIP(int value) {
        return (value & FROM_U_ROUNDTRIP_FLAG) != 0;
    }

    private static int FROM_U_MASK_ROUNDTRIP(int value) {
        return value & ~FROM_U_ROUNDTRIP_FLAG;
    }

    /* use after masking off the roundtrip flag */
    static int FROM_U_GET_LENGTH(int value) {
        return (value >>> FROM_U_LENGTH_SHIFT) & MAX_BYTES;
    }

    /* get bytes or bytes index */
    static int FROM_U_GET_DATA(int value) {
        return value & FROM_U_DATA_MASK;
    }

    /* get the pointer to an extension array from indexes[index] */
    static Buffer ARRAY(ByteBuffer indexes, int index, Class itemType) {
        int oldpos = indexes.position();
        Buffer b;

        indexes.position(indexes.getInt(index << 2));
        if (itemType == int.class)
            b = indexes.asIntBuffer();
        else if (itemType == char.class)
            b = indexes.asCharBuffer();
        else if (itemType == short.class)
            b = indexes.asShortBuffer();
        else
            // default or (itemType == byte.class)
            b = indexes.slice();
        indexes.position(oldpos);
        return b;
    }

    private static int GET_MAX_BYTES_PER_UCHAR(ByteBuffer indexes) {
        indexes.position(0);
        return indexes.getInt(EXT_COUNT_BYTES) & 0xff;
    }

    /*
     * @return index of the UChar, if found; else <0
     */
    static int findFromU(CharBuffer fromUSection, int length, char u) {
        int i, start, limit;

        /* binary search */
        start = 0;
        limit = length;
        for (;;) {
            i = limit - start;
            if (i <= 1) {
                break; /* done */
            }
            /* start<limit-1 */

            if (i <= 4) {
                /* linear search for the last part */
                if (u <= fromUSection.get(fromUSection.position() + start)) {
                    break;
                }
                if (++start < limit && u <= fromUSection.get(fromUSection.position() + start)) {
                    break;
                }
                if (++start < limit && u <= fromUSection.get(fromUSection.position() + start)) {
                    break;
                }
                /* always break at start==limit-1 */
                ++start;
                break;
            }

            i = (start + limit) / 2;
            if (u < fromUSection.get(fromUSection.position() + i)) {
                limit = i;
            } else {
                start = i;
            }
        }

        /* did we really find it? */
        if (start < limit && u == fromUSection.get(fromUSection.position() + start)) {
            return start;
        } else {
            return -1; /* not found */
        }
    }

    /*
     * @return lookup value for the byte, if found; else 0
     */
    static int findToU(IntBuffer toUSection, int length, short byt) {
        long word0, word;
        int i, start, limit;

        /* check the input byte against the lowest and highest section bytes */
        // agljport:comment instead of receiving a start position parameter for toUSection we'll rely on its position
        // property
        start = TO_U_GET_BYTE(toUSection.get(toUSection.position()));
        limit = TO_U_GET_BYTE(toUSection.get(toUSection.position() + length - 1));
        if (byt < start || limit < byt) {
            return 0; /* the byte is out of range */
        }

        if (length == ((limit - start) + 1)) {
            /* direct access on a linear array */
            return TO_U_GET_VALUE(toUSection.get(toUSection.position() + byt - start)); /* could be 0 */
        }

        /* word0 is suitable for <=toUSection[] comparison, word for <toUSection[] */
        word0 = TO_U_MAKE_WORD((byte) byt, 0) & UConverterConstants.UNSIGNED_INT_MASK;

        /*
         * Shift byte once instead of each section word and add 0xffffff. We will compare the shifted/added byte
         * (bbffffff) against section words which have byte values in the same bit position. If and only if byte bb <
         * section byte ss then bbffffff<ssvvvvvv for all v=0..f so we need not mask off the lower 24 bits of each
         * section word.
         */
        word = word0 | TO_U_VALUE_MASK;

        /* binary search */
        start = 0;
        limit = length;
        for (;;) {
            i = limit - start;
            if (i <= 1) {
                break; /* done */
            }
            /* start<limit-1 */

            if (i <= 4) {
                /* linear search for the last part */
                if (word0 <= (toUSection.get(toUSection.position() + start) & UConverterConstants.UNSIGNED_INT_MASK)) {
                    break;
                }
                if (++start < limit
                        && word0 <= (toUSection.get(toUSection.position() + start) & UConverterConstants.UNSIGNED_INT_MASK)) {
                    break;
                }
                if (++start < limit
                        && word0 <= (toUSection.get(toUSection.position() + start) & UConverterConstants.UNSIGNED_INT_MASK)) {
                    break;
                }
                /* always break at start==limit-1 */
                ++start;
                break;
            }

            i = (start + limit) / 2;
            if (word < (toUSection.get(toUSection.position() + i) & UConverterConstants.UNSIGNED_INT_MASK)) {
                limit = i;
            } else {
                start = i;
            }
        }

        /* did we really find it? */
        if (start < limit) {
            word = (toUSection.get(toUSection.position() + start) & UConverterConstants.UNSIGNED_INT_MASK);
            if (byt == TO_U_GET_BYTE((int)word)) {
                return TO_U_GET_VALUE((int) word); /* never 0 */
            }
        } 
        return 0; /* not found */
    }

    /*
     * TRUE if not an SI/SO stateful converter, or if the match length fits with the current converter state
     */
    static boolean TO_U_VERIFY_SISO_MATCH(byte sisoState, int match) {
        return sisoState < 0 || (sisoState == 0) == (match == 1);
    }

    /*
     * get the SI/SO toU state (state 0 is for SBCS, 1 for DBCS), or 1 for DBCS-only, or -1 if the converter is not
     * SI/SO stateful
     * 
     * Note: For SI/SO stateful converters getting here, cnv->mode==0 is equivalent to firstLength==1.
     */
    private static int SISO_STATE(UConverterSharedData sharedData, int mode) {
        return sharedData.mbcs.outputType == MBCS_OUTPUT_2_SISO ? (byte) mode
                : sharedData.mbcs.outputType == MBCS_OUTPUT_DBCS_ONLY ? 1 : -1;
    }

    class CharsetDecoderMBCS extends CharsetDecoderICU {

        CharsetDecoderMBCS(CharsetICU cs) {
            super(cs);
        }

        protected CoderResult decodeLoop(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult[] cr = { CoderResult.UNDERFLOW };

            int sourceArrayIndex;
            int stateTable[][/* 256 */];
            char[] unicodeCodeUnits;

            int offset;
            byte state;
            int byteIndex;
            byte[] bytes;

            int sourceIndex, nextSourceIndex;

            int entry = 0;
            char c;
            byte action;

            if (preToULength > 0) {
                /*
                 * pass sourceIndex=-1 because we continue from an earlier buffer in the future, this may change with
                 * continuous offsets
                 */
                cr[0] = continueMatchToU(source, target, offsets, -1, flush);

                if (cr[0].isError() || preToULength < 0) {
                    return cr[0];
                }
            }

            if (sharedData.mbcs.countStates == 1) {
                if ((sharedData.mbcs.unicodeMask & UConverterConstants.HAS_SUPPLEMENTARY) == 0) {
                    cr[0] = cnvMBCSSingleToBMPWithOffsets(source, target, offsets, flush);
                } else {
                    cr[0] = cnvMBCSSingleToUnicodeWithOffsets(source, target, offsets, flush);
                }
                return cr[0];
            }

            /* set up the local pointers */
            sourceArrayIndex = source.position();

            if ((options & UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                stateTable = sharedData.mbcs.swapLFNLStateTable;
            } else {
                stateTable = sharedData.mbcs.stateTable;
            }
            unicodeCodeUnits = sharedData.mbcs.unicodeCodeUnits;

            /* get the converter state from UConverter */
            offset = (int) toUnicodeStatus;
            byteIndex = toULength;
            bytes = toUBytesArray;

            /*
             * if we are in the SBCS state for a DBCS-only converter, then load the DBCS state from the MBCS data
             * (dbcsOnlyState==0 if it is not a DBCS-only converter)
             */
            state = (byte)mode;
            if (state == 0) {
                state = sharedData.mbcs.dbcsOnlyState;
            }

            /* sourceIndex=-1 if the current character began in the previous buffer */
            sourceIndex = byteIndex == 0 ? 0 : -1;
            nextSourceIndex = 0;

            /* conversion loop */
            while (sourceArrayIndex < source.limit()) {
                /*
                 * This following test is to see if available input would overflow the output. It does not catch output
                 * of more than one code unit that overflows as a result of a surrogate pair or callback output from the
                 * last source byte. Therefore, those situations also test for overflows and will then break the loop,
                 * too.
                 */
                if (!target.hasRemaining()) {
                    /* target is full */
                    cr[0] = CoderResult.OVERFLOW;
                    break;
                }

                if (byteIndex == 0) {
                    /* optimized loop for 1/2-byte input and BMP output */
                    // agljport:todo see ucnvmbcs.c for deleted block
                    do {
                        entry = stateTable[state][source.get(sourceArrayIndex)&UConverterConstants.UNSIGNED_BYTE_MASK];
                        if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                            state = (byte) MBCS_ENTRY_TRANSITION_STATE(entry);
                            offset = MBCS_ENTRY_TRANSITION_OFFSET(entry);
                            ++sourceArrayIndex;
                            if (sourceArrayIndex < source.limit()
                                    && MBCS_ENTRY_IS_FINAL(entry = stateTable[state][source.get(sourceArrayIndex)&UConverterConstants.UNSIGNED_BYTE_MASK])
                                    && MBCS_ENTRY_FINAL_ACTION(entry) == MBCS_STATE_VALID_16
                                    && (c = unicodeCodeUnits[offset + MBCS_ENTRY_FINAL_VALUE_16(entry)]) < 0xfffe) {
                                ++sourceArrayIndex;
                                target.put(c);
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                    sourceIndex = (nextSourceIndex += 2);
                                }
                                state = (byte) MBCS_ENTRY_FINAL_STATE(entry); /* typically 0 */
                                offset = 0;
                            } else {
                                /* set the state and leave the optimized loop */
                                ++nextSourceIndex;
                                bytes[0] = source.get(sourceArrayIndex - 1);
                                byteIndex = 1;
                                break;
                            }
                        } else {
                            if (MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry)) {
                                /* output BMP code point */
                                ++sourceArrayIndex;
                                target.put((char) MBCS_ENTRY_FINAL_VALUE_16(entry));
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                    sourceIndex = ++nextSourceIndex;
                                }
                                state = (byte) MBCS_ENTRY_FINAL_STATE(entry); /* typically 0 */
                            } else {
                                /* leave the optimized loop */
                                break;
                            }
                        }
                    } while (sourceArrayIndex < source.limit() && target.hasRemaining());
                    /*
                     * these tests and break statements could be put inside the loop if C had "break outerLoop" like
                     * Java
                     */
                    if (sourceArrayIndex >= source.limit()) {
                        break;
                    }
                    if (!target.hasRemaining()) {
                        /* target is full */
                        cr[0] = CoderResult.OVERFLOW;
                        break;
                    }

                    ++nextSourceIndex;
                    bytes[byteIndex++] = source.get(sourceArrayIndex++);
                } else /* byteIndex>0 */{
                    ++nextSourceIndex;
                    entry = stateTable[state][(bytes[byteIndex++] = source.get(sourceArrayIndex++))
                            & UConverterConstants.UNSIGNED_BYTE_MASK];
                }

                if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                    state = (byte) MBCS_ENTRY_TRANSITION_STATE(entry);
                    offset += MBCS_ENTRY_TRANSITION_OFFSET(entry);
                    continue;
                }

                /* save the previous state for proper extension mapping with SI/SO-stateful converters */
                mode = state;

                /* set the next state early so that we can reuse the entry variable */
                state = (byte) MBCS_ENTRY_FINAL_STATE(entry); /* typically 0 */

                /*
                 * An if-else-if chain provides more reliable performance for the most common cases compared to a
                 * switch.
                 */
                action = (byte) (MBCS_ENTRY_FINAL_ACTION(entry));
                if (action == MBCS_STATE_VALID_16) {
                    offset += MBCS_ENTRY_FINAL_VALUE_16(entry);
                    c = unicodeCodeUnits[offset];
                    if (c < 0xfffe) {
                        /* output BMP code point */
                        target.put(c);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                    } else if (c == 0xfffe) {
                        if (isFallbackUsed() && (entry = (int) getFallback(sharedData.mbcs, offset)) != 0xfffe) {
                            /* output fallback BMP code point */
                            target.put((char) entry);
                            if (offsets != null) {
                                offsets.put(sourceIndex);
                            }
                            byteIndex = 0;
                        }
                    } else {
                        /* callback(illegal) */
                        cr[0] = CoderResult.malformedForLength(byteIndex);
                    }
                } else if (action == MBCS_STATE_VALID_DIRECT_16) {
                    /* output BMP code point */
                    target.put((char) MBCS_ENTRY_FINAL_VALUE_16(entry));
                    if (offsets != null) {
                        offsets.put(sourceIndex);
                    }
                    byteIndex = 0;
                } else if (action == MBCS_STATE_VALID_16_PAIR) {
                    offset += MBCS_ENTRY_FINAL_VALUE_16(entry);
                    c = unicodeCodeUnits[offset++];
                    if (c < 0xd800) {
                        /* output BMP code point below 0xd800 */
                        target.put(c);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                    } else if (isFallbackUsed() ? c <= 0xdfff : c <= 0xdbff) {
                        /* output roundtrip or fallback surrogate pair */
                        target.put((char) (c & 0xdbff));
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                        if (target.hasRemaining()) {
                            target.put(unicodeCodeUnits[offset]);
                            if (offsets != null) {
                                offsets.put(sourceIndex);
                            }
                        } else {
                            /* target overflow */
                            charErrorBufferArray[0] = unicodeCodeUnits[offset];
                            charErrorBufferLength = 1;
                            cr[0] = CoderResult.OVERFLOW;

                            offset = 0;
                            break;
                        }
                    } else if (isFallbackUsed() ? (c & 0xfffe) == 0xe000 : c == 0xe000) {
                        /* output roundtrip BMP code point above 0xd800 or fallback BMP code point */
                        target.put(unicodeCodeUnits[offset]);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                    } else if (c == 0xffff) {
                        /* callback(illegal) */
                        cr[0] = CoderResult.malformedForLength(byteIndex);
                    }
                } else if (action == MBCS_STATE_VALID_DIRECT_20
                        || (action == MBCS_STATE_FALLBACK_DIRECT_20 && isFallbackUsed())) {
                    entry = MBCS_ENTRY_FINAL_VALUE(entry);
                    /* output surrogate pair */
                    target.put((char) (0xd800 | (char) (entry >> 10)));
                    if (offsets != null) {
                        offsets.put(sourceIndex);
                    }
                    byteIndex = 0;
                    c = (char) (0xdc00 | (char) (entry & 0x3ff));
                    if (target.hasRemaining()) {
                        target.put(c);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                    } else {
                        /* target overflow */
                        charErrorBufferArray[0] = c;
                        charErrorBufferLength = 1;
                        cr[0] = CoderResult.OVERFLOW;

                        offset = 0;
                        break;
                    }
                } else if (action == MBCS_STATE_CHANGE_ONLY) {
                    /*
                     * This serves as a state change without any output. It is useful for reading simple stateful
                     * encodings, for example using just Shift-In/Shift-Out codes. The 21 unused bits may later be used
                     * for more sophisticated state transitions.
                     */
                    if (sharedData.mbcs.dbcsOnlyState == 0) {
                        byteIndex = 0;
                    } else {
                        /* SI/SO are illegal for DBCS-only conversion */
                        state = (byte) (mode); /* restore the previous state */

                        /* callback(illegal) */
                        cr[0] = CoderResult.malformedForLength(byteIndex);
                    }
                } else if (action == MBCS_STATE_FALLBACK_DIRECT_16) {
                    if (isFallbackUsed()) {
                        /* output BMP code point */
                        target.put((char) MBCS_ENTRY_FINAL_VALUE_16(entry));
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                    }
                } else if (action == MBCS_STATE_UNASSIGNED) {
                    /* just fall through */
                } else if (action == MBCS_STATE_ILLEGAL) {
                    /* callback(illegal) */
                    cr[0] = CoderResult.malformedForLength(byteIndex);
                } else {
                    /* reserved, must never occur */
                    byteIndex = 0;
                }

                /* end of action codes: prepare for a new character */
                offset = 0;

                if (byteIndex == 0) {
                    sourceIndex = nextSourceIndex;
                } else if (cr[0].isError()) {
                    /* callback(illegal) */
                    break;
                } else /* unassigned sequences indicated with byteIndex>0 */{
                    /* try an extension mapping */
                    int sourceBeginIndex = sourceArrayIndex;
                    source.position(sourceArrayIndex);
                    byteIndex = toU(byteIndex, source, target, offsets, sourceIndex, flush, cr);
                    sourceArrayIndex = source.position();
                    sourceIndex = nextSourceIndex + (int) (sourceArrayIndex - sourceBeginIndex);

                    if (cr[0].isError() || cr[0].isOverflow()) {
                        /* not mappable or buffer overflow */
                        break;
                    }
                }
            }

            /* set the converter state back into UConverter */
            toUnicodeStatus = offset;
            mode = state;
            toULength = byteIndex;

            /* write back the updated pointers */
            source.position(sourceArrayIndex);

            return cr[0];
        }

        /*
         * continue partial match with new input never called for simple, single-character conversion
         */
        private CoderResult continueMatchToU(ByteBuffer source, CharBuffer target, IntBuffer offsets, int srcIndex,
                boolean flush) {
            CoderResult cr = CoderResult.UNDERFLOW;

            int[] value = new int[1];
            int match, length;

            match = matchToU((byte) SISO_STATE(sharedData, mode), preToUArray, preToUBegin, preToULength, source,
                    value, isToUUseFallback(), flush);

            if (match > 0) {
                if (match >= preToULength) {
                    /* advance src pointer for the consumed input */
                    source.position(source.position() + match - preToULength);
                    preToULength = 0;
                } else {
                    /* the match did not use all of preToU[] - keep the rest for replay */
                    length = preToULength - match;
                    System.arraycopy(preToUArray, preToUBegin + match, preToUArray, preToUBegin, length);
                    preToULength = (byte) -length;
                }

                /* write result */
                cr = writeToU(value[0], target, offsets, srcIndex);
            } else if (match < 0) {
                /* save state for partial match */
                int j, sArrayIndex;

                /* just _append_ the newly consumed input to preToU[] */
                sArrayIndex = source.position();
                match = -match;
                for (j = preToULength; j < match; ++j) {
                    preToUArray[j] = source.get(sArrayIndex++);
                }
                source.position(sArrayIndex); /* same as *src=srcLimit; because we reached the end of input */
                preToULength = (byte) match;
            } else /* match==0 */{
                /*
                 * no match
                 * 
                 * We need to split the previous input into two parts:
                 * 
                 * 1. The first codepage character is unmappable - that's how we got into trying the extension data in
                 * the first place. We need to move it from the preToU buffer to the error buffer, set an error code,
                 * and prepare the rest of the previous input for 2.
                 * 
                 * 2. The rest of the previous input must be converted once we come back from the callback for the first
                 * character. At that time, we have to try again from scratch to convert these input characters. The
                 * replay will be handled by the ucnv.c conversion code.
                 */

                /* move the first codepage character to the error field */
                System.arraycopy(preToUArray, preToUBegin, toUBytesArray, toUBytesBegin, preToUFirstLength);
                toULength = preToUFirstLength;

                /* move the rest up inside the buffer */
                length = preToULength - preToUFirstLength;
                if (length > 0) {
                    System.arraycopy(preToUArray, preToUBegin + preToUFirstLength, preToUArray, preToUBegin, length);
                }

                /* mark preToU for replay */
                preToULength = (byte) -length;

                /* set the error code for unassigned */
                cr = CoderResult.unmappableForLength(preToUFirstLength);
            }
            return cr;
        }

        /*
         * this works like natchFromU() except - the first character is in pre - no trie is used - the returned
         * matchLength is not offset by 2
         */
        private int matchToU(byte sisoState, byte[] preArray, int preArrayBegin, int preLength, ByteBuffer source,
                int[] pMatchValue, boolean isUseFallback, boolean flush) {
            ByteBuffer cx = sharedData.mbcs.extIndexes;
            IntBuffer toUTable, toUSection;

            int value, matchValue, srcLength = 0;
            int i, j, index, length, matchLength;
            short b;

            if (cx == null || cx.asIntBuffer().get(EXT_TO_U_LENGTH) <= 0) {
                return 0; /* no extension data, no match */
            }

            /* initialize */
            toUTable = (IntBuffer) ARRAY(cx, EXT_TO_U_INDEX, int.class);
            index = 0;

            matchValue = 0;
            i = j = matchLength = 0;
            if (source != null) { 
                srcLength = source.remaining();
            }

            if (sisoState == 0) {
                /* SBCS state of an SI/SO stateful converter, look at only exactly 1 byte */
                if (preLength > 1) {
                    return 0; /* no match of a DBCS sequence in SBCS mode */
                } else if (preLength == 1) {
                    srcLength = 0;
                } else /* preLength==0 */{
                    if (srcLength > 1) {
                        srcLength = 1;
                    }
                }
                flush = true;
            }

            /* we must not remember fallback matches when not using fallbacks */

            /* match input units until there is a full match or the input is consumed */
            for (;;) {
                /* go to the next section */
                int oldpos = toUTable.position();
                toUSection = ((IntBuffer) toUTable.position(index)).slice();
                toUTable.position(oldpos);

                /* read first pair of the section */
                value = toUSection.get();
                length = TO_U_GET_BYTE(value);
                value = TO_U_GET_VALUE(value);
                if (value != 0 && (TO_U_IS_ROUNDTRIP(value) || isToUUseFallback(isUseFallback))
                        && TO_U_VERIFY_SISO_MATCH(sisoState, i + j)) {
                    /* remember longest match so far */
                    matchValue = value;
                    matchLength = i + j;
                }

                /* match pre[] then src[] */
                if (i < preLength) {
                    b = (short) (preArray[preArrayBegin + i++] & UConverterConstants.UNSIGNED_BYTE_MASK);
                } else if (j < srcLength) {
                    b = (short) (source.get(source.position() + j++) & UConverterConstants.UNSIGNED_BYTE_MASK);
                } else {
                    /* all input consumed, partial match */
                    if (flush || (length = (i + j)) > MAX_BYTES) {
                        /*
                         * end of the entire input stream, stop with the longest match so far or: partial match must not
                         * be longer than UCNV_EXT_MAX_BYTES because it must fit into state buffers
                         */
                        break;
                    } else {
                        /* continue with more input next time */
                        return -length;
                    }
                }

                /* search for the current UChar */
                value = findToU(toUSection, length, b);
                if (value == 0) {
                    /* no match here, stop with the longest match so far */
                    break;
                } else {
                    if (TO_U_IS_PARTIAL(value)) {
                        /* partial match, continue */
                        index = TO_U_GET_PARTIAL_INDEX(value);
                    } else {
                        if ((TO_U_IS_ROUNDTRIP(value) || isToUUseFallback(isUseFallback)) && TO_U_VERIFY_SISO_MATCH(sisoState, i + j)) {
                            /* full match, stop with result */
                            matchValue = value;
                            matchLength = i + j;
                        } else {
                            /* full match on fallback not taken, stop with the longest match so far */
                        }
                        break;
                    }
                }
            }

            if (matchLength == 0) {
                /* no match at all */
                return 0;
            }

            /* return result */
            pMatchValue[0] = TO_U_MASK_ROUNDTRIP(matchValue);
            return matchLength;
        }

        private CoderResult writeToU(int value, CharBuffer target, IntBuffer offsets, int srcIndex) {
            ByteBuffer cx = sharedData.mbcs.extIndexes;
            /* output the result */
            if (TO_U_IS_CODE_POINT(value)) {
                /* output a single code point */
                return toUWriteCodePoint(TO_U_GET_CODE_POINT(value), target, offsets, srcIndex);
            } else {
                /* output a string - with correct data we have resultLength>0 */

                char[] a = new char[TO_U_GET_LENGTH(value)];
                CharBuffer cb = ((CharBuffer) ARRAY(cx, EXT_TO_U_UCHARS_INDEX, char.class));
                cb.position(TO_U_GET_INDEX(value));
                cb.get(a, 0, a.length);
                return toUWriteUChars(this, a, 0, a.length, target, offsets, srcIndex);
            }
        }

        private CoderResult toUWriteCodePoint(int c, CharBuffer target, IntBuffer offsets, int sourceIndex) {
            CoderResult cr = CoderResult.UNDERFLOW;
            int tBeginIndex = target.position();

            if (target.hasRemaining()) {
                if (c <= 0xffff) {
                    target.put((char) c);
                    c = UConverterConstants.U_SENTINEL;
                } else /* c is a supplementary code point */{
                    target.put(UTF16.getLeadSurrogate(c));
                    c = UTF16.getTrailSurrogate(c);
                    if (target.hasRemaining()) {
                        target.put((char) c);
                        c = UConverterConstants.U_SENTINEL;
                    }
                }

                /* write offsets */
                if (offsets != null) {
                    offsets.put(sourceIndex);
                    if ((tBeginIndex + 1) < target.position()) {
                        offsets.put(sourceIndex);
                    }
                }
            }

            /* write overflow from c */
            if (c >= 0) {
                charErrorBufferLength = UTF16.append(charErrorBufferArray, 0, c);
                cr = CoderResult.OVERFLOW;
            }

            return cr;
        }

        /*
         * Input sequence: cnv->toUBytes[0..length[ @return if(U_FAILURE) return the length (toULength, byteIndex) for
         * the input else return 0 after output has been written to the target
         */
        private int toU(int length, ByteBuffer source, CharBuffer target, IntBuffer offsets, int sourceIndex,
                boolean flush, CoderResult[] cr) {
            // ByteBuffer cx;

            if (sharedData.mbcs.extIndexes != null
                    && initialMatchToU(length, source, target, offsets, sourceIndex, flush, cr)) {
                return 0; /* an extension mapping handled the input */
            }

            /* GB 18030 */
            if (length == 4 && (options & MBCS_OPTION_GB18030) != 0) {
                long[] range;
                long linear;
                int i;

                linear = LINEAR_18030(toUBytesArray[0], toUBytesArray[1], toUBytesArray[2], toUBytesArray[3]);
                for (i = 0; i < gb18030Ranges.length; ++i) {
                    range = gb18030Ranges[i];
                    if (range[2] <= linear && linear <= range[3]) {
                        /* found the sequence, output the Unicode code point for it */
                        cr[0] = CoderResult.UNDERFLOW;

                        /* add the linear difference between the input and start sequences to the start code point */
                        linear = range[0] + (linear - range[2]);

                        /* output this code point */
                        cr[0] = toUWriteCodePoint((int) linear, target, offsets, sourceIndex);

                        return 0;
                    }
                }
            }

            /* no mapping */
            cr[0] = CoderResult.unmappableForLength(length);
            return length;
        }

        /*
         * target<targetLimit; set error code for overflow
         */
        private boolean initialMatchToU(int firstLength, ByteBuffer source, CharBuffer target, IntBuffer offsets,
                int srcIndex, boolean flush, CoderResult[] cr) {
            int[] value = new int[1];
            int match = 0;

            /* try to match */
            match = matchToU((byte) SISO_STATE(sharedData, mode), toUBytesArray, toUBytesBegin, firstLength, source,
                    value, isToUUseFallback(), flush);
            if (match > 0) {
                /* advance src pointer for the consumed input */
                source.position(source.position() + match - firstLength);

                /* write result to target */
                cr[0] = writeToU(value[0], target, offsets, srcIndex);
                return true;
            } else if (match < 0) {
                /* save state for partial match */
                byte[] sArray;
                int sArrayIndex;
                int j;

                /* copy the first code point */
                sArray = toUBytesArray;
                sArrayIndex = toUBytesBegin;
                preToUFirstLength = (byte) firstLength;
                for (j = 0; j < firstLength; ++j) {
                    preToUArray[j] = sArray[sArrayIndex++];
                }

                /* now copy the newly consumed input */
                sArrayIndex = source.position();
                match = -match;
                for (; j < match; ++j) {
                    preToUArray[j] = source.get(sArrayIndex++);
                }
                source.position(sArrayIndex);
                preToULength = (byte) match;
                return true;
            } else /* match==0 no match */{
                return false;
            }
        }

        private int simpleMatchToU(ByteBuffer source, boolean useFallback) {
            int[] value = new int[1];
            int match;

            if (source.remaining() <= 0) {
                return 0xffff;
            }

            /* try to match */
            match = matchToU((byte) -1, source.array(), source.position(), source.limit(), null, value, useFallback, true);

            if (match == source.limit()) {
                /* write result for simple, single-character conversion */
                if (TO_U_IS_CODE_POINT(value[0])) {
                    return TO_U_GET_CODE_POINT(value[0]);
                }
            }

            /*
             * return no match because - match>0 && value points to string: simple conversion cannot handle multiple
             * code points - match>0 && match!=length: not all input consumed, forbidden for this function - match==0:
             * no match found in the first place - match<0: partial match, not supported for simple conversion (and
             * flush==TRUE)
             */
            return 0xfffe;
        }

        CoderResult cnvMBCSToUnicodeWithOffsets(ByteBuffer source, CharBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult[] cr = { CoderResult.UNDERFLOW };
            
            int[][] stateTable;
            char[] unicodeCodeUnits;
            
            int sourceIndex, nextSourceIndex;
            
            int offset;
            short state;
            int byteIndex;
            byte[] bytes;
            
            int entry;
            char c;
            short action;
            
            if (this.preToULength > 0) {
                /*
                 * pass sourceIndex-1 because we continue from an earlier buffer
                 * in the future, this may change with continuous offsets
                 */
                cr[0] = continueMatchToU(source, target, offsets, -1, flush);
                if (cr[0].isError() || this.preToULength < 0) {
                    return cr[0];
                }
            }
            
            if (sharedData.mbcs.countStates == 1) {
                if ((sharedData.mbcs.unicodeMask&UConverterConstants.HAS_SUPPLEMENTARY) == 0) {
                    cr[0] = cnvMBCSSingleToBMPWithOffsets(source, target, offsets, flush);
                } else {
                    cr[0] = cnvMBCSSingleToUnicodeWithOffsets(source, target, offsets, flush);
                }
                return cr[0];
            }
            
            if ((options&UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                stateTable = sharedData.mbcs.swapLFNLStateTable;
            } else {
                stateTable = sharedData.mbcs.stateTable;
            }
            unicodeCodeUnits = sharedData.mbcs.unicodeCodeUnits;
            
            /* get the converter state from UConverter */
            offset = this.toUnicodeStatus;
            byteIndex = this.toULength;
            bytes = this.toUBytesArray;
            
            /*
             * if we are in the SBCS state for a DBCS-only converter,
             * then load the DBCS state from the MBCS data
             * (dbcsOnlyState==0 if it is not a DBCS-only converter)
             */
            state = (short)(UConverterConstants.UNSIGNED_BYTE_MASK&this.mode);
            if (state == 0) {
                state = sharedData.mbcs.dbcsOnlyState;
            }
            
            /* sourceIndex=-1 if the current character begain in the previous buffer */
            sourceIndex = byteIndex == 0 ? 0 : -1;
            nextSourceIndex = 0;
            
            /* conversion loop */
            while (source.hasRemaining()) {
                /*
                 * This following test is to see if available input would overflow the output.
                 * It does not catch output of more than one code unit that
                 * overflows as a result of a surrogate pair or callback output
                 * from the last source byte.
                 * Therefore, those situations also test for overflows and will
                 * then break the loop, too.
                 */
                if (!target.hasRemaining()) {
                    /* target is full */
                    cr[0] = CoderResult.OVERFLOW;
                    break;
                }
                
                if (byteIndex == 0) {
                    /* optimized loop for 1/2-byte input and BMP output */
                    do {
                        entry = stateTable[state][(short)source.get(source.position()) & UConverterConstants.UNSIGNED_BYTE_MASK];
                        if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                            state = (short)(UConverterConstants.UNSIGNED_BYTE_MASK&MBCS_ENTRY_TRANSITION_STATE(entry));
                            offset = MBCS_ENTRY_TRANSITION_OFFSET(entry);
                            
                            source.get();
                            if (source.hasRemaining() &&
                                    MBCS_ENTRY_IS_FINAL(entry=stateTable[state][(short)source.get(source.position()) & UConverterConstants.UNSIGNED_BYTE_MASK]) &&
                                    MBCS_ENTRY_FINAL_ACTION(entry) == MBCS_STATE_VALID_16 &&
                                    (c = unicodeCodeUnits[offset+MBCS_ENTRY_FINAL_VALUE_16(entry)]) < 0xfffe) {
                                source.get();
                                target.put(c);
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                    sourceIndex = (nextSourceIndex + 2);
                                }
                                state = (short)(UConverterConstants.UNSIGNED_BYTE_MASK&MBCS_ENTRY_FINAL_STATE(entry)); /* typically 0 */
                                offset = 0;
                            } else {
                                /* set the state and leave the optimized loop */
                                ++nextSourceIndex;
                                bytes[0] = source.get(source.position()-1);
                                byteIndex = 1;
                                break;
                            }
                        } else {
                            if (MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry)) {
                                /* output BMP code point */
                                source.get();
                                target.put((char)MBCS_ENTRY_FINAL_VALUE_16(entry));
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                    sourceIndex = ++nextSourceIndex;
                                }
                                state = (short)(UConverterConstants.UNSIGNED_BYTE_MASK&MBCS_ENTRY_FINAL_STATE(entry)); /* typically 0 */
                            } else {
                                /* leave the optimized loop */
                                break;
                            }
                        }
                    } while (source.hasRemaining() && target.hasRemaining());
                    
                    /* these tests and break statements could be put inside the loop
                     * if C had "break outerLoop" like Java
                     */
                    if (!source.hasRemaining()) {
                        break;
                    }
                    if (!target.hasRemaining()) {
                        /* target is full */
                        cr[0] = CoderResult.OVERFLOW;
                        break;
                    }
                    
                    ++nextSourceIndex;
                    bytes[byteIndex++] = source.get();
                } else { /* byteIndex>0 */
                    ++nextSourceIndex;
                    entry = stateTable[state][(short)(bytes[byteIndex++]=source.get()) & UConverterConstants.UNSIGNED_BYTE_MASK];
                }
                
                if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                    state = (short)(UConverterConstants.UNSIGNED_BYTE_MASK&MBCS_ENTRY_TRANSITION_STATE(entry));
                    offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                    continue;
                }
                
                /* save the previous state for proper extension mapping with SI/SO-stateful converters */
                mode = state;
                
                /* set the next state early so that we can reuse the entry variable */
                state = (short)(UConverterConstants.UNSIGNED_BYTE_MASK&MBCS_ENTRY_FINAL_STATE(entry)); /* typically 0 */
                
                /*
                 * An if-else-if chain provides more reliable performance for
                 * the most common cases compared to a switch.
                 */
                action = (short)(UConverterConstants.UNSIGNED_BYTE_MASK&MBCS_ENTRY_FINAL_ACTION(entry));
                if (action == MBCS_STATE_VALID_16) {
                    offset += MBCS_ENTRY_FINAL_VALUE_16(entry);
                    c = unicodeCodeUnits[offset];
                    if (c < 0xfffe) {
                        /* output BMP code point */
                        target.put(c);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                    } else if (c == 0xfffe) {
                        if (CharsetDecoderICU.isToUUseFallback() && (entry = (int)getFallback(sharedData.mbcs, offset)) != 0xfffe) {
                            /* output fallback BMP code point */
                            target.put((char)entry);
                            if (offsets != null) {
                                offsets.put(sourceIndex);
                            }
                            byteIndex = 0;
                        }
                    } else {
                        /* callback(illegal) */
                        cr[0] = CoderResult.malformedForLength(1);
                    }
                } else if (action == MBCS_STATE_VALID_DIRECT_16) {
                    /* output BMP code point */
                    target.put((char)MBCS_ENTRY_FINAL_VALUE_16(entry));
                    if (offsets != null) {
                        offsets.put(sourceIndex);
                    }
                    byteIndex = 0;
                } else if (action == MBCS_STATE_VALID_16_PAIR) {
                    offset += MBCS_ENTRY_FINAL_VALUE_16(entry);
                    c = unicodeCodeUnits[offset++];
                    if (c < 0xd800) {
                        /* output BMP code point below 0xd800 */
                        target.put(c);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                    } else if (CharsetDecoderICU.isToUUseFallback() ? c<=0xdfff : c<=0xdbff) {
                        /* output roundtrip or fallback surrogate pair */
                        target.put((char)(c&0xdbff));
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                        if (target.hasRemaining()) {
                            target.put(unicodeCodeUnits[offset]);
                            if (offsets != null) {
                                offsets.put(sourceIndex);
                            }
                        } else {
                            /* target overflow */
                            charErrorBufferArray[0] = unicodeCodeUnits[offset];
                            charErrorBufferLength = 1;
                            cr[0] = CoderResult.OVERFLOW;
                            
                            offset = 0;
                            break;
                        }
                    } else if (CharsetDecoderICU.isToUUseFallback() ? (c&0xfffe)==0xe000 : c==0xe000) {
                        /* output roundtrip BMP code point above 0xd800 or fallback BMP code point */
                        target.put(unicodeCodeUnits[offset]);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                    } else if (c == 0xffff) {
                        /* callback(illegal) */
                        cr[0] = CoderResult.malformedForLength(1);
                    }
                } else if (action == MBCS_STATE_VALID_DIRECT_20 ||
                        action == MBCS_STATE_FALLBACK_DIRECT_20 && CharsetDecoderICU.isToUUseFallback()) {
                    entry = MBCS_ENTRY_FINAL_VALUE(entry);
                    /* output surrogate pair */
                    target.put((char)(0xd800 | (char)(entry&0x3ff)));
                    if (offsets != null) {
                        offsets.put(sourceIndex);
                    }
                    byteIndex = 0;
                    c = (char)(0xdc00 | (char)(entry>>10));
                    if (target.hasRemaining()) {
                        target.put(c);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                    } else {
                        /* target overflow */
                        charErrorBufferArray[0] = c;
                        charErrorBufferLength = 1;
                        cr[0] = CoderResult.OVERFLOW;
                        
                        offset = 0;
                        break;
                    }
                    
                } else if (action == MBCS_STATE_CHANGE_ONLY) {
                    /*
                     * This serves as a state change without any output.
                     * It is useful for reading simple stateful encodings,
                     * for example using just Shift-In/Shift-Out codes.
                     * The 21 unused bits may later be used for more sophisticated
                     * state transistions.
                     */
                    if (sharedData.mbcs.dbcsOnlyState == 0) {
                        byteIndex = 0;
                    } else {
                        /* SI/SO are illegal for DBCS-only conversion */
                        state = (short)(UConverterConstants.UNSIGNED_BYTE_MASK&mode); /* restore the previous state */
                        
                        /* callback(illegal) */
                        cr[0] = CoderResult.malformedForLength(1);
                    }
                } else if (action == MBCS_STATE_FALLBACK_DIRECT_16) {
                    if (CharsetDecoderICU.isToUUseFallback()) {
                        /* output BMP code point */
                        target.put((char)MBCS_ENTRY_FINAL_VALUE_16(entry));
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                        byteIndex = 0;
                    }
                } else if (action == MBCS_STATE_UNASSIGNED) {
                    /* just fall through */
                } else if (action == MBCS_STATE_ILLEGAL) {
                    /* callback(illegal) */
                    cr[0] = CoderResult.malformedForLength(1);
                } else {
                    /* reserved, must never occur */
                    byteIndex = 0;
                }
                
                /* end of action codes: prepare for new character */
                offset = 0;
                
                if (byteIndex == 0) {
                    sourceIndex = nextSourceIndex;
                } else if (cr[0].isError()) {
                    /* callback(illegal) */
                    break;
                } else { /* unassigned sequences indicated with byteIndex>0 */
                    /* try an extension mapping */
                    byteIndex = toU(byteIndex, source, target, offsets, sourceIndex, flush, cr);
                    sourceIndex = nextSourceIndex + source.position();
                    
                    if (cr[0].isError()) {
                        /* not mappable or buffer overflow */
                        break;
                    }
                }
            }
            
            /* set the converter state back into UConverter */
            toUnicodeStatus = offset;
            mode = state;
            toULength = byteIndex;
            
            return cr[0];
        }
        /*
         * This version of cnvMBCSSingleToUnicodeWithOffsets() is optimized for single-byte, single-state codepages that
         * only map to and from the BMP. In addition to single-byte optimizations, the offset calculations become much
         * easier.
         */
        private CoderResult cnvMBCSSingleToBMPWithOffsets(ByteBuffer source, CharBuffer target, IntBuffer offsets,
                boolean flush) {
            CoderResult[] cr = { CoderResult.UNDERFLOW };

            int sourceArrayIndex, lastSource;
            int targetCapacity, length;
            int[][] stateTable;

            int sourceIndex;

            int entry;
            byte action;

            /* set up the local pointers */
            sourceArrayIndex = source.position();
            targetCapacity = target.remaining();

            if ((options & UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                stateTable = sharedData.mbcs.swapLFNLStateTable;
            } else {
                stateTable = sharedData.mbcs.stateTable;
            }

            /* sourceIndex=-1 if the current character began in the previous buffer */
            sourceIndex = 0;
            lastSource = sourceArrayIndex;

            /*
             * since the conversion here is 1:1 UChar:uint8_t, we need only one counter for the minimum of the
             * sourceLength and targetCapacity
             */
            length = source.remaining();
            if (length < targetCapacity) {
                targetCapacity = length;
            }

            /* conversion loop */
            while (targetCapacity > 0) {
                entry = stateTable[0][source.get(sourceArrayIndex++) & UConverterConstants.UNSIGNED_BYTE_MASK];
                /* MBCS_ENTRY_IS_FINAL(entry) */

                /* test the most common case first */
                if (MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry)) {
                    /* output BMP code point */
                    target.put((char) MBCS_ENTRY_FINAL_VALUE_16(entry));
                    --targetCapacity;
                    continue;
                }

                /*
                 * An if-else-if chain provides more reliable performance for the most common cases compared to a
                 * switch.
                 */
                action = (byte) (MBCS_ENTRY_FINAL_ACTION(entry));
                if (action == MBCS_STATE_FALLBACK_DIRECT_16) {
                    if (isFallbackUsed()) {
                        /* output BMP code point */
                        target.put((char) MBCS_ENTRY_FINAL_VALUE_16(entry));
                        --targetCapacity;
                        continue;
                    }
                } else if (action == MBCS_STATE_UNASSIGNED) {
                    /* just fall through */
                } else if (action == MBCS_STATE_ILLEGAL) {
                    /* callback(illegal) */
                    cr[0] = CoderResult.malformedForLength(sourceArrayIndex - lastSource);
                } else {
                    /* reserved, must never occur */
                    continue;
                }

                /* set offsets since the start or the last extension */
                if (offsets != null) {
                    int count = sourceArrayIndex - lastSource;

                    /* predecrement: do not set the offset for the callback-causing character */
                    while (--count > 0) {
                        offsets.put(sourceIndex++);
                    }
                    /* offset and sourceIndex are now set for the current character */
                }

                if (cr[0].isError()) {
                    /* callback(illegal) */
                    break;
                } else /* unassigned sequences indicated with byteIndex>0 */{
                    /* try an extension mapping */
                    lastSource = sourceArrayIndex;
                    toUBytesArray[0] = source.get(sourceArrayIndex - 1);
                    source.position(sourceArrayIndex);
                    toULength = toU((byte) 1, source, target, offsets, sourceIndex, flush, cr);
                    sourceArrayIndex = source.position();
                    sourceIndex += 1 + (int) (sourceArrayIndex - lastSource);

                    if (cr[0].isError()) {
                        /* not mappable or buffer overflow */
                        break;
                    }

                    /* recalculate the targetCapacity after an extension mapping */
                    targetCapacity = target.remaining();
                    length = source.remaining();
                    if (length < targetCapacity) {
                        targetCapacity = length;
                    }
                }
            }

            if (!cr[0].isError() && sourceArrayIndex < source.limit() && !target.hasRemaining()) {
                /* target is full */
                cr[0] = CoderResult.OVERFLOW;
            }

            /* set offsets since the start or the last callback */
            if (offsets != null) {
                int count = sourceArrayIndex - lastSource;
                while (count > 0) {
                    offsets.put(sourceIndex++);
                    --count;
                }
            }

            /* write back the updated pointers */
            source.position(sourceArrayIndex);

            return cr[0];
        }

        /* This version of cnvMBCSToUnicodeWithOffsets() is optimized for single-byte, single-state codepages. */
        private CoderResult cnvMBCSSingleToUnicodeWithOffsets(ByteBuffer source, CharBuffer target, IntBuffer offsets,
                boolean flush) {
            CoderResult[] cr = { CoderResult.UNDERFLOW };

            int sourceArrayIndex;
            int[][] stateTable;

            int sourceIndex;

            int entry;
            char c;
            byte action;

            /* set up the local pointers */
            sourceArrayIndex = source.position();

            if ((options & UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                stateTable = sharedData.mbcs.swapLFNLStateTable;
            } else {
                stateTable = sharedData.mbcs.stateTable;
            }

            /* sourceIndex=-1 if the current character began in the previous buffer */
            sourceIndex = 0;

            /* conversion loop */
            while (sourceArrayIndex < source.limit()) {
                /*
                 * This following test is to see if available input would overflow the output. It does not catch output
                 * of more than one code unit that overflows as a result of a surrogate pair or callback output from the
                 * last source byte. Therefore, those situations also test for overflows and will then break the loop,
                 * too.
                 */
                if (!target.hasRemaining()) {
                    /* target is full */
                    cr[0] = CoderResult.OVERFLOW;
                    break;
                }

                entry = stateTable[0][source.get(sourceArrayIndex++) & UConverterConstants.UNSIGNED_BYTE_MASK];
                /* MBCS_ENTRY_IS_FINAL(entry) */

                /* test the most common case first */
                if (MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry)) {
                    /* output BMP code point */
                    target.put((char) MBCS_ENTRY_FINAL_VALUE_16(entry));
                    if (offsets != null) {
                        offsets.put(sourceIndex);
                    }

                    /* normal end of action codes: prepare for a new character */
                    ++sourceIndex;
                    continue;
                }

                /*
                 * An if-else-if chain provides more reliable performance for the most common cases compared to a
                 * switch.
                 */
                action = (byte) (MBCS_ENTRY_FINAL_ACTION(entry));
                if (action == MBCS_STATE_VALID_DIRECT_20
                        || (action == MBCS_STATE_FALLBACK_DIRECT_20 && isFallbackUsed())) {

                    entry = MBCS_ENTRY_FINAL_VALUE(entry);
                    /* output surrogate pair */
                    target.put((char) (0xd800 | (char) (entry >>> 10)));
                    if (offsets != null) {
                        offsets.put(sourceIndex);
                    }
                    c = (char) (0xdc00 | (char) (entry & 0x3ff));
                    if (target.hasRemaining()) {
                        target.put(c);
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }
                    } else {
                        /* target overflow */
                        charErrorBufferArray[0] = c;
                        charErrorBufferLength = 1;
                        cr[0] = CoderResult.OVERFLOW;
                        break;
                    }

                    ++sourceIndex;
                    continue;
                } else if (action == MBCS_STATE_FALLBACK_DIRECT_16) {
                    if (isFallbackUsed()) {
                        /* output BMP code point */
                        target.put((char) MBCS_ENTRY_FINAL_VALUE_16(entry));
                        if (offsets != null) {
                            offsets.put(sourceIndex);
                        }

                        ++sourceIndex;
                        continue;
                    }
                } else if (action == MBCS_STATE_UNASSIGNED) {
                    /* just fall through */
                } else if (action == MBCS_STATE_ILLEGAL) {
                    /* callback(illegal) */
                    cr[0] = CoderResult.malformedForLength(1);
                } else {
                    /* reserved, must never occur */
                    ++sourceIndex;
                    continue;
                }

                if (cr[0].isError()) {
                    /* callback(illegal) */
                    break;
                } else /* unassigned sequences indicated with byteIndex>0 */{
                    /* try an extension mapping */
                    int sourceBeginIndex = sourceArrayIndex;
                    toUBytesArray[0] = source.get(sourceArrayIndex - 1);
                    source.position(sourceArrayIndex);
                    toULength = toU((byte) 1, source, target, offsets, sourceIndex, flush, cr);
                    sourceArrayIndex = source.position();
                    sourceIndex += 1 + (int) (sourceArrayIndex - sourceBeginIndex);

                    if (cr[0].isError()) {
                        /* not mappable or buffer overflow */
                        break;
                    }
                }
            }

            /* write back the updated pointers */
            source.position(sourceArrayIndex);

            return cr[0];
        }

        private int getFallback(UConverterMBCSTable mbcsTable, int offset) {
            MBCSToUFallback[] toUFallbacks;
            int i, start, limit;

            limit = mbcsTable.countToUFallbacks;
            if (limit > 0) {
                /* do a binary search for the fallback mapping */
                toUFallbacks = mbcsTable.toUFallbacks;
                start = 0;
                while (start < limit - 1) {
                    i = (start + limit) / 2;
                    if (offset < toUFallbacks[i].offset) {
                        limit = i;
                    } else {
                        start = i;
                    }
                }

                /* did we really find it? */
                if (offset == toUFallbacks[start].offset) {
                    return toUFallbacks[start].codePoint;
                }
            }

            return 0xfffe;
        }

        /**
         * This is a simple version of _MBCSGetNextUChar() that is used by other converter implementations. It only
         * returns an "assigned" result if it consumes the entire input. It does not use state from the converter, nor
         * error codes. It does not handle the EBCDIC swaplfnl option (set in UConverter). It handles conversion
         * extensions but not GB 18030.
         * 
         * @return U+fffe unassigned U+ffff illegal otherwise the Unicode code point
         */
        int simpleGetNextUChar(ByteBuffer source, boolean useFallback) {

            // #if 0
            // /*
            // * Code disabled 2002dec09 (ICU 2.4) because it is not currently used in ICU. markus
            // * TODO In future releases, verify that this function is never called for SBCS
            // * conversions, i.e., that sharedData->mbcs.countStates==1 is still true.
            // * Removal improves code coverage.
            // */
            // /* use optimized function if possible */
            // if(sharedData->mbcs.countStates==1) {
            // if(length==1) {
            // return ucnv_MBCSSingleSimpleGetNextUChar(sharedData, (uint8_t)*source, useFallback);
            // } else {
            // return 0xffff; /* illegal: more than a single byte for an SBCS converter */
            // }
            // }
            // #endif

            /* set up the local pointers */
            int[][] stateTable = sharedData.mbcs.stateTable;
            char[] unicodeCodeUnits = sharedData.mbcs.unicodeCodeUnits;

            /* converter state */
            int offset = 0;
            int state = sharedData.mbcs.dbcsOnlyState;

            int action;
            int entry;
            int c;
            int i = source.position();
            int length = source.limit() - i;

            /* conversion loop */
            while (true) {
                // entry=stateTable[state][(uint8_t)source[i++]];
                entry = stateTable[state][source.get() & UConverterConstants.UNSIGNED_BYTE_MASK];
                i = source.position();

                if (MBCS_ENTRY_IS_TRANSITION(entry)) {
                    state = MBCS_ENTRY_TRANSITION_STATE(entry);
                    offset += MBCS_ENTRY_TRANSITION_OFFSET(entry);

                    if (i == source.limit()) {
                        return 0xffff; /* truncated character */
                    }
                } else {
                    /*
                     * An if-else-if chain provides more reliable performance for the most common cases compared to a
                     * switch.
                     */
                    action = MBCS_ENTRY_FINAL_ACTION(entry);
                    if (action == MBCS_STATE_VALID_16) {
                        offset += MBCS_ENTRY_FINAL_VALUE_16(entry);
                        c = unicodeCodeUnits[offset];
                        if (c != 0xfffe) {
                            /* done */
                        } else if (isToUUseFallback()) {
                            c = getFallback(sharedData.mbcs, offset);
                        }
                        /* else done with 0xfffe */
                    } else if (action == MBCS_STATE_VALID_DIRECT_16) {
                        // /* output BMP code point */
                        c = MBCS_ENTRY_FINAL_VALUE_16(entry);
                    } else if (action == MBCS_STATE_VALID_16_PAIR) {
                        offset += MBCS_ENTRY_FINAL_VALUE_16(entry);
                        c = unicodeCodeUnits[offset++];
                        if (c < 0xd800) {
                            /* output BMP code point below 0xd800 */
                        } else if (isToUUseFallback() ? c <= 0xdfff : c <= 0xdbff) {
                            /* output roundtrip or fallback supplementary code point */
                            c = (((c & 0x3ff) << 10) + unicodeCodeUnits[offset] + (0x10000 - 0xdc00));
                        } else if (isToUUseFallback() ? (c & 0xfffe) == 0xe000 : c == 0xe000) {
                            /* output roundtrip BMP code point above 0xd800 or fallback BMP code point */
                            c = unicodeCodeUnits[offset];
                        } else if (c == 0xffff) {
                            return 0xffff;
                        } else {
                            c = 0xfffe;
                        }
                    } else if (action == MBCS_STATE_VALID_DIRECT_20) {
                        /* output supplementary code point */
                        c = 0x10000 + MBCS_ENTRY_FINAL_VALUE(entry);
                    } else if (action == MBCS_STATE_FALLBACK_DIRECT_16) {
                        if (!isToUUseFallback(useFallback)) {
                            c = 0xfffe;
                        } else {
                            /* output BMP code point */
                            c = MBCS_ENTRY_FINAL_VALUE_16(entry);
                        }
                    } else if (action == MBCS_STATE_FALLBACK_DIRECT_20) {
                        if (!isToUUseFallback(useFallback)) {
                            c = 0xfffe;
                        } else {
                            /* output supplementary code point */
                            c = 0x10000 + MBCS_ENTRY_FINAL_VALUE(entry);
                        }
                    } else if (action == MBCS_STATE_UNASSIGNED) {
                        c = 0xfffe;
                    } else {
                        /*
                         * forbid MBCS_STATE_CHANGE_ONLY for this function, and MBCS_STATE_ILLEGAL and reserved action
                         * codes
                         */
                        return 0xffff;
                    }
                    break;
                }
            }

            if (i != source.limit()) {
                /* illegal for this function: not all input consumed */
                return 0xffff;
            }

            if (c == 0xfffe) {
                /* try an extension mapping */
                if (sharedData.mbcs.extIndexes != null) {
                    /* Increase the limit for proper handling. Used in LMBCS. */
                    if (source.limit() >= source.position() + length) {
                        source.limit(source.position() + length);
                    }
                    return simpleMatchToU(source, useFallback);
                }
            }

            return c;
        }

    }

    class CharsetEncoderMBCS extends CharsetEncoderICU {
        private boolean allowReplacementChanges = false;

        CharsetEncoderMBCS(CharsetICU cs) {
            super(cs, fromUSubstitution);
            allowReplacementChanges = true; // allow changes in implReplaceWith
            implReset();
        }

        protected void implReset() {
            super.implReset();
            preFromUFirstCP = UConverterConstants.U_SENTINEL;
        }

        protected CoderResult encodeLoop(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {

            CoderResult[] cr = { CoderResult.UNDERFLOW };
            // if (!source.hasRemaining() && fromUChar32 == 0)
            // return cr[0];

            int sourceArrayIndex;
            char[] table;
            byte[] pArray, bytes;
            int pArrayIndex, outputType, c;
            int prevSourceIndex, sourceIndex, nextSourceIndex;
            int stage2Entry = 0, value = 0, length = 0, prevLength;
            short uniMask;
            // long asciiRoundtrips;
            
            boolean gotoUnassigned = false;

            try {

                if (!flush && preFromUFirstCP >= 0) {
                    /*
                     * pass sourceIndex=-1 because we continue from an earlier buffer in the future, this may change
                     * with continuous offsets
                     */
                    cr[0] = continueMatchFromU(source, target, offsets, flush, -1);

                    if (cr[0].isError() || preFromULength < 0) {
                        return cr[0];
                    }
                }

                /* use optimized function if possible */
                outputType = sharedData.mbcs.outputType;
                uniMask = sharedData.mbcs.unicodeMask;
                if (outputType == MBCS_OUTPUT_1 && (uniMask & UConverterConstants.HAS_SURROGATES) == 0) {
                    if ((uniMask & UConverterConstants.HAS_SUPPLEMENTARY) == 0) {
                        cr[0] = cnvMBCSSingleFromBMPWithOffsets(source, target, offsets, flush);
                    } else {
                        cr[0] = cnvMBCSSingleFromUnicodeWithOffsets(source, target, offsets, flush);
                    }
                    return cr[0];
                } else if (outputType == MBCS_OUTPUT_2) {
                    cr[0] = cnvMBCSDoubleFromUnicodeWithOffsets(source, target, offsets, flush);
                    return cr[0];
                }

                table = sharedData.mbcs.fromUnicodeTable;
                sourceArrayIndex = source.position();

                if ((options & UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                    bytes = sharedData.mbcs.swapLFNLFromUnicodeBytes;
                } else {
                    bytes = sharedData.mbcs.fromUnicodeBytes;
                }

                // asciiRoundtrips = sharedData.mbcs.asciiRoundtrips;

                /* get the converter state from UConverter */
                c = fromUChar32;

                if (outputType == MBCS_OUTPUT_2_SISO) {
                    prevLength = (int) fromUnicodeStatus;
                    if (prevLength == 0) {
                        /* set the real value */
                        prevLength = 1;
                    }
                } else {
                    /* prevent fromUnicodeStatus from being set to something non-0 */
                    prevLength = 0;
                }

                /* sourceIndex=-1 if the current character began in the previous buffer */
                prevSourceIndex = -1;
                sourceIndex = c == 0 ? 0 : -1;
                nextSourceIndex = 0;

                /* conversion loop */
                /*
                 * This is another piece of ugly code: A goto into the loop if the converter state contains a first
                 * surrogate from the previous function call. It saves me to check in each loop iteration a check of
                 * if(c==0) and duplicating the trail-surrogate-handling code in the else branch of that check. I could
                 * not find any other way to get around this other than using a function call for the conversion and
                 * callback, which would be even more inefficient.
                 * 
                 * Markus Scherer 2000-jul-19
                 */
                boolean doloop = true;
                boolean doread = true;
                if (c != 0 && target.hasRemaining()) {
                    if (UTF16.isLeadSurrogate((char) c) && (uniMask & UConverterConstants.HAS_SURROGATES) == 0) {
                        // c is a lead surrogate, read another input
                        SideEffects x = new SideEffects(c, sourceArrayIndex, sourceIndex, nextSourceIndex,
                                prevSourceIndex, prevLength);
                        doloop = getTrail(source, target, uniMask, x, flush, cr);
                        doread = x.doread;
                        c = x.c;
                        sourceArrayIndex = x.sourceArrayIndex;
                        sourceIndex = x.sourceIndex;
                        nextSourceIndex = x.nextSourceIndex;
                        prevSourceIndex = x.prevSourceIndex;
                        prevLength = x.prevLength;
                    } else {
                        // c is not a lead surrogate, do not read another input
                        doread = false;
                    }
                }

                if (doloop) {
                    while (!doread || sourceArrayIndex < source.limit()) {
                        /*
                         * This following test is to see if available input would overflow the output. It does not catch
                         * output of more than one byte that overflows as a result of a multi-byte character or callback
                         * output from the last source character. Therefore, those situations also test for overflows
                         * and will then break the loop, too.
                         */
                        if (target.hasRemaining()) {
                            /*
                             * Get a correct Unicode code point: a single UChar for a BMP code point or a matched
                             * surrogate pair for a "supplementary code point".
                             */

                            if (doread) {
                                // doread might be false only on the first looping

                                c = source.get(sourceArrayIndex++);
                                ++nextSourceIndex;

                                /*
                                 * This also tests if the codepage maps single surrogates. If it does, then surrogates
                                 * are not paired but mapped separately. Note that in this case unmatched surrogates are
                                 * not detected.
                                 */
                                if (UTF16.isSurrogate((char) c)
                                        && (uniMask & UConverterConstants.HAS_SURROGATES) == 0) {
                                    if (UTF16.isLeadSurrogate((char) c)) {
                                        // getTrail:
                                        SideEffects x = new SideEffects(c, sourceArrayIndex, sourceIndex,
                                                nextSourceIndex, prevSourceIndex, prevLength);
                                        doloop = getTrail(source, target, uniMask, x, flush, cr);
                                        c = x.c;
                                        sourceArrayIndex = x.sourceArrayIndex;
                                        sourceIndex = x.sourceIndex;
                                        nextSourceIndex = x.nextSourceIndex;
                                        prevSourceIndex = x.prevSourceIndex;

                                        if (x.doread) {
                                            if (doloop)
                                                continue;
                                            else
                                                break;
                                        }
                                    } else {
                                        /* this is an unmatched trail code unit (2nd surrogate) */
                                        /* callback(illegal) */
                                        cr[0] = CoderResult.malformedForLength(1);
                                        break;
                                    }
                                }
                            } else {
                                doread = true;
                            }
                            /* convert the Unicode code point in c into codepage bytes */

                            /*
                             * The basic lookup is a triple-stage compact array (trie) lookup. For details see the
                             * beginning of this file.
                             * 
                             * Single-byte codepages are handled with a different data structure by _MBCSSingle...
                             * functions.
                             * 
                             * The result consists of a 32-bit value from stage 2 and a pointer to as many bytes as are
                             * stored per character. The pointer points to the character's bytes in stage 3. Bits 15..0
                             * of the stage 2 entry contain the stage 3 index for that pointer, while bits 31..16 are
                             * flags for which of the 16 characters in the block are roundtrip-assigned.
                             * 
                             * For 2-byte and 4-byte codepages, the bytes are stored as uint16_t respectively as
                             * uint32_t, in the platform encoding. For 3-byte codepages, the bytes are always stored in
                             * big-endian order.
                             * 
                             * For EUC encodings that use only either 0x8e or 0x8f as the first byte of their longest
                             * byte sequences, the first two bytes in this third stage indicate with their 7th bits
                             * whether these bytes are to be written directly or actually need to be preceeded by one of
                             * the two Single-Shift codes. With this, the third stage stores one byte fewer per
                             * character than the actual maximum length of EUC byte sequences.
                             * 
                             * Other than that, leading zero bytes are removed and the other bytes output. A single zero
                             * byte may be output if the "assigned" bit in stage 2 was on. The data structure does not
                             * support zero byte output as a fallback, and also does not allow output of leading zeros.
                             */
                            stage2Entry = MBCS_STAGE_2_FROM_U(table, c);

                            /* get the bytes and the length for the output */
                            switch (outputType) {
                            /* This is handled above with the method cnvMBCSDoubleFromUnicodeWithOffsets() */
                            /* case MBCS_OUTPUT_2:
                                value = MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
                                if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xff) {
                                    length = 1;
                                } else {
                                    length = 2;
                                }
                                break; */
                            case MBCS_OUTPUT_2_SISO:
                                /* 1/2-byte stateful with Shift-In/Shift-Out */
                                /*
                                 * Save the old state in the converter object right here, then change the local
                                 * prevLength state variable if necessary. Then, if this character turns out to be
                                 * unassigned or a fallback that is not taken, the callback code must not save the new
                                 * state in the converter because the new state is for a character that is not output.
                                 * However, the callback must still restore the state from the converter in case the
                                 * callback function changed it for its output.
                                 */
                                fromUnicodeStatus = prevLength; /* save the old state */
                                value = MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
                                if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xff) {
                                    if (value == 0 && MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c) == false) {
                                        /* no mapping, leave value==0 */
                                        length = 0;
                                    } else if (prevLength <= 1) {
                                        length = 1;
                                    } else {
                                        /* change from double-byte mode to single-byte */
                                        value |= UConverterConstants.SI << 8;
                                        length = 2;
                                        prevLength = 1;
                                    }
                                } else {
                                    if (prevLength == 2) {
                                        length = 2;
                                    } else {
                                        /* change from single-byte mode to double-byte */
                                        value |= UConverterConstants.SO << 16;
                                        length = 3;
                                        prevLength = 2;
                                    }
                                }
                                break;
                            case MBCS_OUTPUT_DBCS_ONLY:
                                /* table with single-byte results, but only DBCS mappings used */
                                value = MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
                                if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xff) {
                                    /* no mapping or SBCS result, not taken for DBCS-only */
                                    value = stage2Entry = 0; /* stage2Entry=0 to reset roundtrip flags */
                                    length = 0;
                                } else {
                                    length = 2;
                                }
                                break;
                            case MBCS_OUTPUT_3:
                                pArray = bytes;
                                pArrayIndex = MBCS_POINTER_3_FROM_STAGE_2(bytes, stage2Entry, c);
                                value = ((pArray[pArrayIndex] & UConverterConstants.UNSIGNED_BYTE_MASK) << 16)
                                        | ((pArray[pArrayIndex + 1] & UConverterConstants.UNSIGNED_BYTE_MASK) << 8)
                                        | (pArray[pArrayIndex + 2] & UConverterConstants.UNSIGNED_BYTE_MASK);
                                if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xff) {
                                    length = 1;
                                } else if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xffff) {
                                    length = 2;
                                } else {
                                    length = 3;
                                }
                                break;
                            case MBCS_OUTPUT_4:
                                value = MBCS_VALUE_4_FROM_STAGE_2(bytes, stage2Entry, c);
                                if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xff) {
                                    length = 1;
                                } else if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xffff) {
                                    length = 2;
                                } else if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xffffff) {
                                    length = 3;
                                } else {
                                    length = 4;
                                }
                                break;
                            case MBCS_OUTPUT_3_EUC:
                                value = MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
                                /* EUC 16-bit fixed-length representation */
                                if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xff) {
                                    length = 1;
                                } else if ((value & 0x8000) == 0) {
                                    value |= 0x8e8000;
                                    length = 3;
                                } else if ((value & 0x80) == 0) {
                                    value |= 0x8f0080;
                                    length = 3;
                                } else {
                                    length = 2;
                                }
                                break;
                            case MBCS_OUTPUT_4_EUC:
                                pArray = bytes;
                                pArrayIndex = MBCS_POINTER_3_FROM_STAGE_2(bytes, stage2Entry, c);
                                value = ((pArray[pArrayIndex] & UConverterConstants.UNSIGNED_BYTE_MASK) << 16)
                                        | ((pArray[pArrayIndex + 1] & UConverterConstants.UNSIGNED_BYTE_MASK) << 8)
                                        | (pArray[pArrayIndex + 2] & UConverterConstants.UNSIGNED_BYTE_MASK);
                                /* EUC 16-bit fixed-length representation applied to the first two bytes */
                                if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xff) {
                                    length = 1;
                                } else if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xffff) {
                                    length = 2;
                                } else if ((value & 0x800000) == 0) {
                                    value |= 0x8e800000;
                                    length = 4;
                                } else if ((value & 0x8000) == 0) {
                                    value |= 0x8f008000;
                                    length = 4;
                                } else {
                                    length = 3;
                                }
                                break;
                            default:
                                /* must not occur */
                                /*
                                 * To avoid compiler warnings that value & length may be used without having been
                                 * initialized, we set them here. In reality, this is unreachable code. Not having a
                                 * default branch also causes warnings with some compilers.
                                 */
                                value = stage2Entry = 0; /* stage2Entry=0 to reset roundtrip flags */
                                length = 0;
                                break;
                            }
                            
                            /* is this code point assigned, or do we use fallbacks? */
                            if (gotoUnassigned || (!(MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c) || (isFromUUseFallback(c) && value != 0)))) {
                                gotoUnassigned = false;
                                /*
                                 * We allow a 0 byte output if the "assigned" bit is set for this entry. There is no way
                                 * with this data structure for fallback output to be a zero byte.
                                 */

                                // unassigned:
                                SideEffects x = new SideEffects(c, sourceArrayIndex, sourceIndex, nextSourceIndex,
                                        prevSourceIndex, prevLength);
                                doloop = unassigned(source, target, offsets, x, flush, cr);
                                c = x.c;
                                sourceArrayIndex = x.sourceArrayIndex;
                                sourceIndex = x.sourceIndex;
                                nextSourceIndex = x.nextSourceIndex;
                                prevSourceIndex = x.prevSourceIndex;
                                prevLength = x.prevLength;
                                if (doloop)
                                    continue;
                                else
                                    break;
                            }

                            /* write the output character bytes from value and length */
                            /* from the first if in the loop we know that targetCapacity>0 */
                            if (length <= target.remaining()) {
                                switch (length) {
                                /* each branch falls through to the next one */
                                case 4:
                                    target.put((byte) (value >>> 24));
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                    }
                                case 3:
                                    target.put((byte) (value >>> 16));
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                    }
                                case 2:
                                    target.put((byte) (value >>> 8));
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                    }
                                case 1:
                                    target.put((byte) value);
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                    }
                                default:
                                    /* will never occur */
                                    break;
                                }
                            } else {
                                int errorBufferArrayIndex;

                                /*
                                 * We actually do this backwards here: In order to save an intermediate variable, we
                                 * output first to the overflow buffer what does not fit into the regular target.
                                 */
                                /* we know that 1<=targetCapacity<length<=4 */
                                length -= target.remaining();

                                errorBufferArrayIndex = 0;
                                switch (length) {
                                /* each branch falls through to the next one */
                                case 3:
                                    errorBuffer[errorBufferArrayIndex++] = (byte) (value >>> 16);
                                case 2:
                                    errorBuffer[errorBufferArrayIndex++] = (byte) (value >>> 8);
                                case 1:
                                    errorBuffer[errorBufferArrayIndex] = (byte) value;
                                default:
                                    /* will never occur */
                                    break;
                                }
                                errorBufferLength = (byte) length;

                                /* now output what fits into the regular target */
                                value >>>= 8 * length; /* length was reduced by targetCapacity */
                                switch (target.remaining()) {
                                /* each branch falls through to the next one */
                                case 3:
                                    target.put((byte) (value >>> 16));
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                    }
                                case 2:
                                    target.put((byte) (value >>> 8));
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                    }
                                case 1:
                                    target.put((byte) value);
                                    if (offsets != null) {
                                        offsets.put(sourceIndex);
                                    }
                                default:
                                    /* will never occur */
                                    break;
                                }

                                /* target overflow */
                                cr[0] = CoderResult.OVERFLOW;
                                c = 0;
                                break;
                            }

                            /* normal end of conversion: prepare for a new character */
                            c = 0;
                            if (offsets != null) {
                                prevSourceIndex = sourceIndex;
                                sourceIndex = nextSourceIndex;
                            }
                            continue;
                        } else {
                            /* target is full */
                            cr[0] = CoderResult.OVERFLOW;
                            break;
                        }
                    }
                }

                /*
                 * the end of the input stream and detection of truncated input are handled by the framework, but for
                 * EBCDIC_STATEFUL conversion we need to emit an SI at the very end
                 * 
                 * conditions: successful EBCDIC_STATEFUL in DBCS mode end of input and no truncated input
                 */
                if (outputType == MBCS_OUTPUT_2_SISO && prevLength == 2 && flush && sourceArrayIndex >= source.limit()
                        && c == 0) {

                    /* EBCDIC_STATEFUL ending with DBCS: emit an SI to return the output stream to SBCS */
                    if (target.hasRemaining()) {
                        target.put((byte) UConverterConstants.SI);
                        if (offsets != null) {
                            /* set the last source character's index (sourceIndex points at sourceLimit now) */
                            offsets.put(prevSourceIndex);
                        }
                    } else {
                        /* target is full */
                        errorBuffer[0] = (byte) UConverterConstants.SI;
                        errorBufferLength = 1;
                        cr[0] = CoderResult.OVERFLOW;
                    }
                    prevLength = 1; /* we switched into SBCS */
                }

                /* set the converter state back into UConverter */
                fromUChar32 = c;
                fromUnicodeStatus = prevLength;

                source.position(sourceArrayIndex);
            } catch (BufferOverflowException ex) {
                cr[0] = CoderResult.OVERFLOW;
            }

            return cr[0];
        }

        /*
         * This is another simple conversion function for internal use by other conversion implementations. It does not
         * use the converter state nor call callbacks. It does not handle the EBCDIC swaplfnl option (set in
         * UConverter). It handles conversion extensions but not GB 18030.
         * 
         * It converts one single Unicode code point into codepage bytes, encoded as one 32-bit value. The function
         * returns the number of bytes in *pValue: 1..4 the number of bytes in *pValue 0 unassigned (*pValue undefined)
         * -1 illegal (currently not used, *pValue undefined)
         * 
         * *pValue will contain the resulting bytes with the last byte in bits 7..0, the second to last byte in bits
         * 15..8, etc. Currently, the function assumes but does not check that 0<=c<=0x10ffff.
         */
        int fromUChar32(int c, int[] pValue, boolean isUseFallback) {
            // #if 0
            // /* #if 0 because this is not currently used in ICU - reduce code, increase code coverage */
            // const uint8_t *p;
            // #endif

            char[] table;
            int stage2Entry;
            int value;
            int length;
            int p;

            /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
            if (c <= 0xffff || ((sharedData.mbcs.unicodeMask & UConverterConstants.HAS_SUPPLEMENTARY) != 0)) {
                table = sharedData.mbcs.fromUnicodeTable;

                /* convert the Unicode code point in c into codepage bytes (same as in _MBCSFromUnicodeWithOffsets) */
                if (sharedData.mbcs.outputType == MBCS_OUTPUT_1) {
                    value = MBCS_SINGLE_RESULT_FROM_U(table, sharedData.mbcs.fromUnicodeBytes, c);
                    /* is this code point assigned, or do we use fallbacks? */
                    if (isUseFallback ? value >= 0x800 : value >= 0xc00) {
                        pValue[0] = value & 0xff;
                        return 1;
                    }
                } else /* outputType!=MBCS_OUTPUT_1 */{
                    stage2Entry = MBCS_STAGE_2_FROM_U(table, c);

                    /* get the bytes and the length for the output */
                    switch (sharedData.mbcs.outputType) {
                    case MBCS_OUTPUT_2:
                        value = MBCS_VALUE_2_FROM_STAGE_2(sharedData.mbcs.fromUnicodeBytes, stage2Entry, c);
                        if (value <= 0xff) {
                            length = 1;
                        } else {
                            length = 2;
                        }
                        break;
                    // #if 0
                    // /* #if 0 because this is not currently used in ICU - reduce code, increase code coverage */
                    // case MBCS_OUTPUT_DBCS_ONLY:
                    // /* table with single-byte results, but only DBCS mappings used */
                    // value=MBCS_VALUE_2_FROM_STAGE_2(sharedData->mbcs.fromUnicodeBytes, stage2Entry, c);
                    // if(value<=0xff) {
                    // /* no mapping or SBCS result, not taken for DBCS-only */
                    // value=stage2Entry=0; /* stage2Entry=0 to reset roundtrip flags */
                    // length=0;
                    // } else {
                    // length=2;
                    // }
                    // break;
                    case MBCS_OUTPUT_3:
                        byte[] bytes = sharedData.mbcs.fromUnicodeBytes;
                        p = CharsetMBCS.MBCS_POINTER_3_FROM_STAGE_2(bytes, stage2Entry, c);
                        value = ((bytes[p] & UConverterConstants.UNSIGNED_BYTE_MASK)<<16) |
                            ((bytes[p+1] & UConverterConstants.UNSIGNED_BYTE_MASK)<<8) |
                            (bytes[p+2] & UConverterConstants.UNSIGNED_BYTE_MASK);
                        if (value <= 0xff) {
                            length = 1;
                        } else if (value <= 0xffff) {
                            length = 2;
                        } else {
                            length = 3;
                        }
                        break;
                    // case MBCS_OUTPUT_4:
                    // value=MBCS_VALUE_4_FROM_STAGE_2(sharedData->mbcs.fromUnicodeBytes, stage2Entry, c);
                    // if(value<=0xff) {
                    // length=1;
                    // } else if(value<=0xffff) {
                    // length=2;
                    // } else if(value<=0xffffff) {
                    // length=3;
                    // } else {
                    // length=4;
                    // }
                    // break;
                    // case MBCS_OUTPUT_3_EUC:
                    // value=MBCS_VALUE_2_FROM_STAGE_2(sharedData->mbcs.fromUnicodeBytes, stage2Entry, c);
                    // /* EUC 16-bit fixed-length representation */
                    // if(value<=0xff) {
                    // length=1;
                    // } else if((value&0x8000)==0) {
                    // value|=0x8e8000;
                    // length=3;
                    // } else if((value&0x80)==0) {
                    // value|=0x8f0080;
                    // length=3;
                    // } else {
                    // length=2;
                    // }
                    // break;
                    // case MBCS_OUTPUT_4_EUC:
                    // p=MBCS_POINTER_3_FROM_STAGE_2(sharedData->mbcs.fromUnicodeBytes, stage2Entry, c);
                    // value=((uint32_t)*p<<16)|((uint32_t)p[1]<<8)|p[2];
                    // /* EUC 16-bit fixed-length representation applied to the first two bytes */
                    // if(value<=0xff) {
                    // length=1;
                    // } else if(value<=0xffff) {
                    // length=2;
                    // } else if((value&0x800000)==0) {
                    // value|=0x8e800000;
                    // length=4;
                    // } else if((value&0x8000)==0) {
                    // value|=0x8f008000;
                    // length=4;
                    // } else {
                    // length=3;
                    // }
                    // break;
                    // #endif
                    default:
                        /* must not occur */
                        return -1;
                    }

                    /* is this code point assigned, or do we use fallbacks? */
                    if (MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c)
                            || (CharsetEncoderICU.isFromUUseFallback(isUseFallback, c) && value != 0)) {
                        /*
                         * We allow a 0 byte output if the "assigned" bit is set for this entry. There is no way with
                         * this data structure for fallback output to be a zero byte.
                         */
                        /* assigned */
                        pValue[0] = value;
                        return length;
                    }
                }
            }

            if (sharedData.mbcs.extIndexes != null) {
                length = simpleMatchFromU(c, pValue, isUseFallback);
                return length >= 0 ? length : -length; /* return abs(length); */
            }

            /* unassigned */
            return 0;
        }

        /*
         * continue partial match with new input, requires cnv->preFromUFirstCP>=0 never called for simple,
         * single-character conversion
         */
        private CoderResult continueMatchFromU(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush,
                int srcIndex) {
            CoderResult cr = CoderResult.UNDERFLOW;
            int[] value = new int[1];
            int match;

            match = matchFromU(preFromUFirstCP, preFromUArray, preFromUBegin, preFromULength, source, value, useFallback, flush);
            if (match >= 2) {
                match -= 2; /* remove 2 for the initial code point */

                if (match >= preFromULength) {
                    /* advance src pointer for the consumed input */
                    source.position(source.position() + match - preFromULength);
                    preFromULength = 0;
                } else {
                    /* the match did not use all of preFromU[] - keep the rest for replay */
                    int length = preFromULength - match;
                    System.arraycopy(preFromUArray, preFromUBegin + match, preFromUArray, preFromUBegin, length);
                    preFromULength = (byte) -length;
                }

                /* finish the partial match */
                preFromUFirstCP = UConverterConstants.U_SENTINEL;

                /* write result */
                writeFromU(value[0], target, offsets, srcIndex);
            } else if (match < 0) {
                /* save state for partial match */
                int sArrayIndex;
                int j;

                /* just _append_ the newly consumed input to preFromU[] */
                sArrayIndex = source.position();
                match = -match - 2; /* remove 2 for the initial code point */
                for (j = preFromULength; j < match; ++j) {
                    preFromUArray[j] = source.get(sArrayIndex++);
                }
                source.position(sArrayIndex); /* same as *src=srcLimit; because we reached the end of input */
                preFromULength = (byte) match;
            } else { /* match==0 or 1 */
                /*
                 * no match
                 * 
                 * We need to split the previous input into two parts:
                 * 
                 * 1. The first code point is unmappable - that's how we got into trying the extension data in the first
                 * place. We need to move it from the preFromU buffer to the error buffer, set an error code, and
                 * prepare the rest of the previous input for 2.
                 * 
                 * 2. The rest of the previous input must be converted once we come back from the callback for the first
                 * code point. At that time, we have to try again from scratch to convert these input characters. The
                 * replay will be handled by the ucnv.c conversion code.
                 */

                if (match == 1) {
                    /* matched, no mapping but request for <subchar1> */
                    useSubChar1 = true;
                }

                /* move the first code point to the error field */
                fromUChar32 = preFromUFirstCP;
                preFromUFirstCP = UConverterConstants.U_SENTINEL;

                /* mark preFromU for replay */
                preFromULength = (byte) -preFromULength;

                /* set the error code for unassigned */
                // TODO: figure out what the unmappable length really should be
                cr = CoderResult.unmappableForLength(1);
            }
            return cr;
        }

        /**
         * @param cx
         *            pointer to extension data; if NULL, returns 0
         * @param firstCP
         *            the first code point before all the other UChars
         * @param pre
         *            UChars that must match; !initialMatch: partial match with them
         * @param preLength
         *            length of pre, >=0
         * @param src
         *            UChars that can be used to complete a match
         * @param srcLength
         *            length of src, >=0
         * @param pMatchValue
         *            [out] output result value for the match from the data structure
         * @param useFallback
         *            "use fallback" flag, usually from cnv->useFallback
         * @param flush
         *            TRUE if the end of the input stream is reached
         * @return >1: matched, return value=total match length (number of input units matched) 1: matched, no mapping
         *         but request for <subchar1> (only for the first code point) 0: no match <0: partial match, return
         *         value=negative total match length (partial matches are never returned for flush==TRUE) (partial
         *         matches are never returned as being longer than UCNV_EXT_MAX_UCHARS) the matchLength is 2 if only
         *         firstCP matched, and >2 if firstCP and further code units matched
         */
        // static int32_t ucnv_extMatchFromU(const int32_t *cx, UChar32 firstCP, const UChar *pre, int32_t preLength,
        // const UChar *src, int32_t srcLength, uint32_t *pMatchValue, UBool useFallback, UBool flush)
        private int matchFromU(int firstCP, char[] preArray, int preArrayBegin, int preLength, CharBuffer source,
                int[] pMatchValue, boolean isUseFallback, boolean flush) {
            ByteBuffer cx = sharedData.mbcs.extIndexes;

            CharBuffer stage12, stage3;
            IntBuffer stage3b;

            CharBuffer fromUTableUChars, fromUSectionUChars;
            IntBuffer fromUTableValues, fromUSectionValues;

            int value, matchValue;
            int i, j, index, length, matchLength;
            char c;

            if (cx == null) {
                return 0; /* no extension data, no match */
            }

            /* trie lookup of firstCP */
            index = firstCP >>> 10; /* stage 1 index */
            if (index >= cx.asIntBuffer().get(EXT_FROM_U_STAGE_1_LENGTH)) {
                return 0; /* the first code point is outside the trie */
            }

            stage12 = (CharBuffer) ARRAY(cx, EXT_FROM_U_STAGE_12_INDEX, char.class);
            stage3 = (CharBuffer) ARRAY(cx, EXT_FROM_U_STAGE_3_INDEX, char.class);
            index = FROM_U(stage12, stage3, index, firstCP);

            stage3b = (IntBuffer) ARRAY(cx, EXT_FROM_U_STAGE_3B_INDEX, int.class);
            value = stage3b.get(stage3b.position() + index);
            if (value == 0) {
                return 0;
            }

            if (TO_U_IS_PARTIAL(value)) {
                /* partial match, enter the loop below */
                index = FROM_U_GET_PARTIAL_INDEX(value);

                /* initialize */
                fromUTableUChars = (CharBuffer) ARRAY(cx, EXT_FROM_U_UCHARS_INDEX, char.class);
                fromUTableValues = (IntBuffer) ARRAY(cx, EXT_FROM_U_VALUES_INDEX, int.class);

                matchValue = 0;
                i = j = matchLength = 0;

                /* we must not remember fallback matches when not using fallbacks */

                /* match input units until there is a full match or the input is consumed */
                for (;;) {
                    /* go to the next section */
                    int oldpos = fromUTableUChars.position();
                    fromUSectionUChars = ((CharBuffer) fromUTableUChars.position(index)).slice();
                    fromUTableUChars.position(oldpos);
                    oldpos = fromUTableValues.position();
                    fromUSectionValues = ((IntBuffer) fromUTableValues.position(index)).slice();
                    fromUTableValues.position(oldpos);

                    /* read first pair of the section */
                    length = fromUSectionUChars.get();
                    value = fromUSectionValues.get();
                    if (value != 0 && (FROM_U_IS_ROUNDTRIP(value) || isFromUUseFallback(isUseFallback, firstCP))) {
                        /* remember longest match so far */
                        matchValue = value;
                        matchLength = 2 + i + j;
                    }

                    /* match pre[] then src[] */
                    if (i < preLength) {
                        c = preArray[preArrayBegin + i++];
                    } else if (source != null && j < source.remaining()) {
                        c = source.get(source.position() + j++);
                    } else {
                        /* all input consumed, partial match */
                        if (flush || (length = (i + j)) > MAX_UCHARS) {
                            /*
                             * end of the entire input stream, stop with the longest match so far or: partial match must
                             * not be longer than UCNV_EXT_MAX_UCHARS because it must fit into state buffers
                             */
                            break;
                        } else {
                            /* continue with more input next time */
                            return -(2 + length);
                        }
                    }

                    /* search for the current UChar */
                    index = findFromU(fromUSectionUChars, length, c);
                    if (index < 0) {
                        /* no match here, stop with the longest match so far */
                        break;
                    } else {
                        value = fromUSectionValues.get(fromUSectionValues.position() + index);
                        if (FROM_U_IS_PARTIAL(value)) {
                            /* partial match, continue */
                            index = FROM_U_GET_PARTIAL_INDEX(value);
                        } else {
                            if (FROM_U_IS_ROUNDTRIP(value) || isFromUUseFallback(isUseFallback, firstCP)) {
                                /* full match, stop with result */
                                matchValue = value;
                                matchLength = 2 + i + j;
                            } else {
                                /* full match on fallback not taken, stop with the longest match so far */
                            }
                            break;
                        }
                    }
                }

                if (matchLength == 0) {
                    /* no match at all */
                    return 0;
                }
            } else /* result from firstCP trie lookup */{
                if (FROM_U_IS_ROUNDTRIP(value) || isFromUUseFallback(isUseFallback, firstCP)) {
                    /* full match, stop with result */
                    matchValue = value;
                    matchLength = 2;
                } else {
                    /* fallback not taken */
                    return 0;
                }
            }

            if ((matchValue & FROM_U_RESERVED_MASK) != 0) {
                /* do not interpret values with reserved bits used, for forward compatibility */
                return 0;
            }

            /* return result */
            if (matchValue == FROM_U_SUBCHAR1) {
                return 1; /* assert matchLength==2 */
            }

            pMatchValue[0] = FROM_U_MASK_ROUNDTRIP(matchValue);
            return matchLength;
        }

        private int simpleMatchFromU(int cp, int[] pValue, boolean isUseFallback) {
            int[] value = new int[1];
            int match; // signed

            /* try to match */
            match = matchFromU(cp, null, 0, 0, null, value, isUseFallback, true);
            if (match >= 2) {
                /* write result for simple, single-character conversion */
                int length;
                boolean isRoundtrip;

                isRoundtrip = FROM_U_IS_ROUNDTRIP(value[0]);
                length = FROM_U_GET_LENGTH(value[0]);
                value[0] = FROM_U_GET_DATA(value[0]);

                if (length <= EXT_FROM_U_MAX_DIRECT_LENGTH) {
                    pValue[0] = value[0];
                    return isRoundtrip ? length : -length;
                    // #if 0 /* not currently used */
                    // } else if(length==4) {
                    // /* de-serialize a 4-byte result */
                    // const uint8_t *result=UCNV_EXT_ARRAY(cx, UCNV_EXT_FROM_U_BYTES_INDEX, uint8_t)+value;
                    // *pValue=
                    // ((uint32_t)result[0]<<24)|
                    // ((uint32_t)result[1]<<16)|
                    // ((uint32_t)result[2]<<8)|
                    // result[3];
                    // return isRoundtrip ? 4 : -4;
                    // #endif
                }
            }

            /*
             * return no match because - match>1 && resultLength>4: result too long for simple conversion - match==1: no
             * match found, <subchar1> preferred - match==0: no match found in the first place - match<0: partial
             * match, not supported for simple conversion (and flush==TRUE)
             */
            return 0;
        }

        private CoderResult writeFromU(int value, ByteBuffer target, IntBuffer offsets, int srcIndex) {
            ByteBuffer cx = sharedData.mbcs.extIndexes;

            byte bufferArray[] = new byte[1 + MAX_BYTES];
            int bufferArrayIndex = 0;
            byte[] resultArray;
            int resultArrayIndex;
            int length, prevLength;

            length = FROM_U_GET_LENGTH(value);
            value = FROM_U_GET_DATA(value);

            /* output the result */
            if (length <= FROM_U_MAX_DIRECT_LENGTH) {
                /*
                 * Generate a byte array and then write it below. This is not the fastest possible way, but it should be
                 * ok for extension mappings, and it is much simpler. Offset and overflow handling are only done once
                 * this way.
                 */
                int p = bufferArrayIndex + 1; /* reserve buffer[0] for shiftByte below */
                switch (length) {
                case 3:
                    bufferArray[p++] = (byte) (value >>> 16);
                case 2:
                    bufferArray[p++] = (byte) (value >>> 8);
                case 1:
                    bufferArray[p++] = (byte) value;
                default:
                    break; /* will never occur */
                }
                resultArray = bufferArray;
                resultArrayIndex = bufferArrayIndex + 1;
            } else {
                byte[] slice = new byte[length];

                ByteBuffer bb = ((ByteBuffer) ARRAY(cx, EXT_FROM_U_BYTES_INDEX, byte.class));
                bb.position(value);
                bb.get(slice, 0, slice.length);

                resultArray = slice;
                resultArrayIndex = 0;
            }

            /* with correct data we have length>0 */

            if ((prevLength = (int) fromUnicodeStatus) != 0) {
                /* handle SI/SO stateful output */
                byte shiftByte;

                if (prevLength > 1 && length == 1) {
                    /* change from double-byte mode to single-byte */
                    shiftByte = (byte) UConverterConstants.SI;
                    fromUnicodeStatus = 1;
                } else if (prevLength == 1 && length > 1) {
                    /* change from single-byte mode to double-byte */
                    shiftByte = (byte) UConverterConstants.SO;
                    fromUnicodeStatus = 2;
                } else {
                    shiftByte = 0;
                }

                if (shiftByte != 0) {
                    /* prepend the shift byte to the result bytes */
                    bufferArray[0] = shiftByte;
                    if (resultArray != bufferArray || resultArrayIndex != bufferArrayIndex + 1) {
                        System.arraycopy(resultArray, resultArrayIndex, bufferArray, bufferArrayIndex + 1, length);
                    }
                    resultArray = bufferArray;
                    resultArrayIndex = bufferArrayIndex;
                    ++length;
                }
            }

            return fromUWriteBytes(this, resultArray, resultArrayIndex, length, target, offsets, srcIndex);
        }

        /*
         * @return if(U_FAILURE) return the code point for cnv->fromUChar32 else return 0 after output has been written
         * to the target
         */
        private int fromU(int cp_, CharBuffer source, ByteBuffer target, IntBuffer offsets, int sourceIndex,
                int length, boolean flush, CoderResult[] cr) {
            // ByteBuffer cx;
            long cp = cp_ & UConverterConstants.UNSIGNED_INT_MASK;

            useSubChar1 = false;

            if (sharedData.mbcs.extIndexes != null
                    && initialMatchFromU((int) cp, source, target, offsets, sourceIndex, flush, cr)) {
                return 0; /* an extension mapping handled the input */
            }

            /* GB 18030 */
            if ((options & MBCS_OPTION_GB18030) != 0) {
                long[] range;
                int i;

                for (i = 0; i < gb18030Ranges.length; ++i) {
                    range = gb18030Ranges[i];
                    if (range[0] <= cp && cp <= range[1]) {
                        /* found the Unicode code point, output the four-byte sequence for it */
                        long linear;
                        byte bytes[] = new byte[4];

                        /* get the linear value of the first GB 18030 code in this range */
                        linear = range[2] - LINEAR_18030_BASE;

                        /* add the offset from the beginning of the range */
                        linear += (cp - range[0]);

                        bytes[3] = (byte) (0x30 + linear % 10);
                        linear /= 10;
                        bytes[2] = (byte) (0x81 + linear % 126);
                        linear /= 126;
                        bytes[1] = (byte) (0x30 + linear % 10);
                        linear /= 10;
                        bytes[0] = (byte) (0x81 + linear);

                        /* output this sequence */
                        cr[0] = fromUWriteBytes(this, bytes, 0, 4, target, offsets, sourceIndex);
                        return 0;
                    }
                }
            }

            /* no mapping */
            cr[0] = CoderResult.unmappableForLength(length);
            return (int) cp;
        }

        /*
         * target<targetLimit; set error code for overflow
         */
        private boolean initialMatchFromU(int cp, CharBuffer source, ByteBuffer target, IntBuffer offsets,
                int srcIndex, boolean flush, CoderResult[] cr) {
            int[] value = new int[1];
            int match;

            /* try to match */
            match = matchFromU(cp, null, 0, 0, source, value, useFallback, flush);

            /* reject a match if the result is a single byte for DBCS-only */
            if (match >= 2
                    && !(FROM_U_GET_LENGTH(value[0]) == 1 && sharedData.mbcs.outputType == MBCS_OUTPUT_DBCS_ONLY)) {
                /* advance src pointer for the consumed input */
                source.position(source.position() + match - 2); /* remove 2 for the initial code point */

                /* write result to target */
                cr[0] = writeFromU(value[0], target, offsets, srcIndex);
                return true;
            } else if (match < 0) {
                /* save state for partial match */
                int sArrayIndex;
                int j;

                /* copy the first code point */
                preFromUFirstCP = cp;

                /* now copy the newly consumed input */
                sArrayIndex = source.position();
                match = -match - 2; /* remove 2 for the initial code point */
                for (j = 0; j < match; ++j) {
                    preFromUArray[j] = source.get(sArrayIndex++);
                }
                source.position(sArrayIndex); /* same as *src=srcLimit; because we reached the end of input */
                preFromULength = (byte) match;
                return true;
            } else if (match == 1) {
                /* matched, no mapping but request for <subchar1> */
                useSubChar1 = true;
                return false;
            } else /* match==0 no match */{
                return false;
            }
        }
        
        CoderResult cnvMBCSFromUnicodeWithOffsets(CharBuffer source, ByteBuffer target, IntBuffer offsets, boolean flush) {
            CoderResult[] cr = { CoderResult.UNDERFLOW };
            
            char[] table;
            int p;
            ByteBuffer bytes;
            short outputType;
            
            SideEffects x = new SideEffects(0, 0, 0, 0, 0, 0);
            
            int targetCapacity = target.limit() - target.position();
            
            int stage2Entry = 0;
            //int asciiRoundtrips;
            long value;
            int length = 0;
            int uniMask;
            
            boolean doLoop = true;
            boolean gotoGetTrail = false;
            
            if (preFromUFirstCP >= 0) {
                /*
                 * pass sourceIndex=-1 because we continue from an earlier buffer
                 * in the future, this may change with continuous offsets.
                 */
                cr[0] = continueMatchFromU(source, target, offsets, flush, -1);
                if (cr[0].isError() || preFromULength < 0) {
                    return cr[0];
                }
            }
            
            /* use optimized function if possible */
            outputType = sharedData.mbcs.outputType;
            uniMask = sharedData.mbcs.unicodeMask;
            if (outputType == MBCS_OUTPUT_1 && ((uniMask&UConverterConstants.HAS_SURROGATES) == 0)) {
                if ((uniMask&UConverterConstants.HAS_SURROGATES) == 0) {
                    cr[0] = cnvMBCSSingleFromBMPWithOffsets(source, target, offsets, flush);
                } else {
                    cr[0] = cnvMBCSSingleFromUnicodeWithOffsets(source, target, offsets, flush);
                }
                return cr[0];
            }/* else if (outputType == MBCS_OUTPUT_2 && mbcs.sharedData.mbcs.utf8Friendly) {
                cr[0] = cnvMBCSDoubleFromUnicodeWithOffsets(source, target, offsets, flush);
                return cr[0];
            }*/
            
            table = sharedData.mbcs.fromUnicodeTable;
            /* if (mbcs.sharedData.mbcs.utf8Friendly) {
                mbcsIndex = mbcs.sharedData.mbcs.mbcsIndex;
            } else {
                mbcsIndex = null;
            } */
            
            if ((options&UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                bytes = ByteBuffer.wrap(sharedData.mbcs.swapLFNLFromUnicodeBytes);
            } else {
                bytes = ByteBuffer.wrap(sharedData.mbcs.fromUnicodeBytes);
            }
            //asciiRoundtrips = mbcs.sharedData.mbcs.asciiRoundtrips;
            
            /* get the converter state from UConverter */
            x.c = fromUChar32;
            if (outputType == MBCS_OUTPUT_2_SISO) {
                x.prevLength = fromUnicodeStatus;
                if (x.prevLength == 0) {
                    /* set the real value */
                    x.prevLength = 1;
                }
            } else {
                /* prevent fromUnicodeStatus from being set to something non-0 */
                x.prevLength = 0;
            }
            
            /* sourceIndex = -1 if the current character began in the previous buffer */
            x.prevSourceIndex = -1;
            x.sourceIndex = x.c==0 ? 0 : -1;
            x.nextSourceIndex = 0;
            
            /* conversion loop */
            if (x.c != 0 && targetCapacity > 0) {
                gotoGetTrail = true; // set gotoGetTrail flag and go to gotoGetTrail label
            }
            
            while (gotoGetTrail || source.hasRemaining()) {
                /*
                 * This following test is to see if available input would overflow the output.
                 * It does not catch output of more than one byte that
                 * overflows as a result of a multi-byte character or callback output
                 * from the last source character.
                 * Therefore, those situations also test for overflows and will
                 * then break the loop, too.
                 */
                if (gotoGetTrail || targetCapacity > 0) {
                    /*
                     * Get a correct Unicode code point:
                     * a single UChar for a BMP code point or 
                     * a matched surrogate pair for a "supplementary code point."
                     */
                    if (!gotoGetTrail) {
                        x.c = source.get();
                        ++x.nextSourceIndex;
                        /* This is commented out because of the fact that IS_ASCII_ROUNDTRIP is not
                         * being used in ICU4J.
                         */
                        /*if (x.c <= 0x7f && IS_ASCII_ROUNDTRIP(c, asciiRoundtrips)) {
                            target.put((byte)x.c);
                            if (offsets != null) {
                                offsets.put(x.sourceIndex);
                                x.prevSourceIndex = x.sourceIndex;
                                x.sourceIndex = x.nextSourceIndex;
                            }
                            targetCapacity--;
                            x.c = 0;
                            continue;
                        }*/
                    }
                  /* Code to use utf8friendly code was removed since it is not needed in Java. */
                    /* This also tests if the codepage maps single surrogates.
                     * If it does, then surrogates are not paired but mapped separately.
                     * Note that in this case unmatched surrogates are not detected.
                     */
                    if (gotoGetTrail || (UTF16.isSurrogate((char)x.c) && (uniMask&UConverterConstants.HAS_SURROGATES) == 0)) {
                        if (gotoGetTrail || (UTF16.isLeadSurrogate((char)x.c))) {
// getTrail label
                            gotoGetTrail = false; // reset gotoGetTrail flag
                            
                            x.sourceArrayIndex = source.position();
                            
                            doLoop = getTrail(source, target, uniMask, x, flush, cr);
                            if (x.doread && doLoop) {
                                continue;
                            } else if (!x.doread && !doLoop) {
                                break;
                            } else if (!doLoop) {
                                break;
                            }
                        } else {
                            /* this is an unmatched trail code unit (2nd surrogate) */
                            /* callback(illegal) */
                            cr[0] = CoderResult.malformedForLength(1);
                            break;
                        }
                    }
                    
                    /* convert the Unicode point in c into codepage bytes */
                    /*
                     * The basic lookup is a triple-stage compact array (trie) lookup.
                     * 
                     * Single-byte codepages are handled with a different data structure
                     * by _MBCSSingle... functions.
                     * 
                     * The result consists of a 32-bit value from stage 2 and
                     * a pointer to as many bytes as are stored per character.
                     * The pointer points to the character's bytes in stage 3.
                     * Bits 15..0 of the stage 2 entry contain the stage 3 index
                     * for that pointer, while bits 31..16 are flags for which of
                     * the 16 characters in the block are roundtrip-assigned.
                     * 
                     * For 2-byte and 4 byte codepages, the bytes are stored as uint16_t
                     * respectively as uint32_t, in the platform encoding.
                     * For 3-byte codepages, the bytes are always stored in big-endian order.
                     * 
                     * For EUC encodings that use only either 0x8e or 0x8f as the first
                     * byte of their longest byte sequences, the first two bytes in 
                     * this third stage indicate with their 7th bits whether these bytes
                     * are to be writeen directly or actually need to be preceeded by
                     * one of the two Single-Shift codes. With this, the third stage
                     * stores one byte fewer per character than the actual maximum length of
                     * EUC byte sequences.
                     * 
                     * Other than that, leading zero bytes are removed and the other
                     * bytes output. A single zero byte may be ouput if the "assigned"
                     * bit in stage 2 was on.
                     * The data structure does not support zero byte output as a fallback,
                     * and also does not allow output of leading zeros.
                     */
                    stage2Entry = MBCS_STAGE_2_FROM_U(table, x.c);
                    
                    /* get the bytes and the length for the output */
                    switch (outputType) {
                    case MBCS_OUTPUT_2:
                        value = MBCS_VALUE_2_FROM_STAGE_2(bytes.array(), stage2Entry, x.c);
                        if (value <= 0xff) {
                            length = 1;
                        } else {
                            length = 2;
                        }
                        break;
                    case MBCS_OUTPUT_2_SISO:
                        /* 1/2-byte stateful with Shift-In/Shift-Out */
                        /*
                         * Save the old state in the converter object
                         * right here, then change the local pervLength state variable if necessary.
                         * Then, if this character turns out to be unassigned or a fallback that
                         * is not taken, the callback code must not save the new state in the converter
                         * because the new state is for a character that is not output.
                         * However, the callback must still restore the state from the converter
                         * in case the callback function changed it for its output.
                         */
                        fromUnicodeStatus = x.prevLength; /* save the old state */
                        value = MBCS_VALUE_2_FROM_STAGE_2(bytes.array(), stage2Entry, x.c);
                        if (value <= 0xff) {
                            if (value == 0 && MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, x.c)) {
                                /* no mapping, leave value == 0 */
                                length = 0;
                            } else if (x.prevLength <= 1) {
                                length = 1;
                            } else {
                                /* change from double-byte mode to single-byte */
                                value |= UConverterConstants.UNSIGNED_INT_MASK & (UConverterConstants.SI<<8);
                                length = 2;
                                x.prevLength = 1;
                            }
                        } else {
                            if (x.prevLength == 2) {
                                length = 2;
                            } else {
                                /* change from single-byte mode to double-byte */
                                value |= UConverterConstants.UNSIGNED_INT_MASK & (UConverterConstants.SO<<16);
                                length = 3;
                                x.prevLength = 2;
                            }
                        }
                        break;
                    case MBCS_OUTPUT_DBCS_ONLY:
                        /* table with single-byte results, but only DBCS mappings used */
                        value = MBCS_VALUE_2_FROM_STAGE_2(bytes.array(), stage2Entry, x.c);
                        if (value <= 0xff) {
                            /* no mapping or SBCS result, not taken for DBCS-only */
                            value = stage2Entry = 0; /* stage2Entry=0 to reset roundtrip flags */
                            length = 0;
                        } else {
                            length = 2;
                        }
                        break;
                    case MBCS_OUTPUT_3:
                        p = MBCS_POINTER_3_FROM_STAGE_2(bytes.array(), stage2Entry, x.c);
                        value = UConverterConstants.UNSIGNED_INT_MASK&((int)bytes.get(p)<<16 | (int)bytes.get(p+1)<<8 | bytes.get(p+2));
                        if (value <= 0xff) {
                            length = 1;
                        } else if (value <= 0xffff) {
                            length = 2;
                        } else {
                            length = 3;
                        }
                        break;
                    case MBCS_OUTPUT_4:
                        value = MBCS_VALUE_4_FROM_STAGE_2(bytes.array(), stage2Entry, x.c);
                        if (value <= 0xff) {
                            length = 1;
                        } else if (value <= 0xffff) {
                            length = 2;
                        } else if (value <= 0xffffff) {
                            length = 3;
                        } else {
                            length = 4;
                        }
                        break;
                    case MBCS_OUTPUT_3_EUC:
                        value = MBCS_VALUE_2_FROM_STAGE_2(bytes.array(), stage2Entry, x.c);
                        /* EUC 16-bit fixed-length representation */
                        if (value <= 0xff) {
                            length = 1;
                        } else if ((value&0x8000) == 0) {
                            value |= 0x8e8000;
                            length = 3;
                        } else if ((value&0x80) == 0) {
                            value |= 0x8f0080;
                            length = 3;
                        } else {
                            length = 2;
                        }
                        break;
                    case MBCS_OUTPUT_4_EUC:
                        p = MBCS_POINTER_3_FROM_STAGE_2(bytes.array(), stage2Entry, x.c);
                        value = UConverterConstants.UNSIGNED_INT_MASK&((int)bytes.get(p)<<16 | (int)bytes.get(p+1)<<8 | bytes.get(p+2));
                        /* EUC 16-bit fixed-length representation applied to the first two bytes */
                        if (value <= 0xff) {
                            length = 1;
                        } else if (value <= 0xffff) {
                            length = 2;
                        } else if ((value&0x800000) == 0) {
                            value |= 0x08e800000;
                            length = 4;
                        } else if ((value&0x8000) == 0) {
                            value |= 0x08f008000;
                            length = 4;
                        } else {
                            length = 3;
                        }
                        break;
                    default :
                        /* must not occur */
                        value = stage2Entry = 0;
                        length = 0;
                        break;
                    }
                    /* is this code point assigned, or do we use fallbacks? */
                    if (!(MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, x.c)) || 
                            (CharsetEncoderICU.isFromUUseFallback(useFallback, x.c) && value != 0)) {
                        /*
                         * We allow a 0 byte output if the "assigned" bit is set for this entry.
                         * There is no way with this data structure for fallback output
                         * to be a zero byte.
                         */
// unassigned label 
                        int currentSourcePos = source.position();
                        doLoop = unassigned(source, target, offsets, x, flush, cr);
                        if (doLoop) {
                            continue;
                        } else {
                            if (source.position() < currentSourcePos) {
                                source.position(currentSourcePos);
                            }
                            break;
                        }
                    }
                    
                    /* write the output character bytes from value and length */
                    /* from the first if in the loop we know that targetCapacity>0 */
                    if (length <= targetCapacity) {
                        switch (length) {
                        /* each branch falls through to the next one */
                        case 4:
                            target.put((byte)(value>>24));
                            if (offsets != null) {
                                offsets.put(x.sourceIndex);
                            }
                        case 3:
                            target.put((byte)(value>>16));
                            if (offsets != null) {
                                offsets.put(x.sourceIndex);
                            }
                        case 2:
                            target.put((byte)(value>>8));
                            if (offsets != null) {
                                offsets.put(x.sourceIndex);
                            }
                        case 1:
                            target.put((byte)value);
                            if (offsets != null) {
                                offsets.put(x.sourceIndex);
                            }
                        default :
                            /* will never occur */
                            break;
                        }
                        
                        targetCapacity -= length;
                    } else {
                        /*
                         * We actually do this backwards here:
                         * In order to save an intermediate variable, we output
                         * first to the overflow buffer what does not fit into the
                         * regular target.
                         */
                        /* we know that 1<=targetCapacity<length<=4 */
                        length -= targetCapacity;
                        int i = 0; // index for errorBuffer
                        switch (length) {
                            /* each branch falls through to the next one */
                        case 3:
                            errorBuffer[i++] = (byte)(value>>16);
                        case 2:
                            errorBuffer[i++] = (byte)(value>>8);
                        case 1:
                            errorBuffer[i++] = (byte)value;
                        default :
                            /* will never occur */
                            break;
                        }
                        errorBufferLength = length;
                        
                        /* now output what fits into the regular target */
                        value>>=8*length; /* length was reduced by targetCapacity */
                        switch (targetCapacity) {
                            /* each branch falls through to the next one */
                        case 3:
                            target.put((byte)(value>>16));
                            if (offsets != null) {
                                offsets.put(x.sourceIndex);
                            }
                        case 2:
                            target.put((byte)(value>>8));
                            if (offsets != null) {
                                offsets.put(x.sourceIndex);
                            }
                        case 1:
                            target.put((byte)value);
                            if (offsets != null) {
                                offsets.put(x.sourceIndex);
                            }
                        default :
                            /* will never occur */
                            break;
                        }
                        
                        /* target overflow */
                        targetCapacity = 0;
                        cr[0] = CoderResult.OVERFLOW;
                        x.c = 0;
                        break;
                    }
                    
                    /* normal end of conversion: prepare for a new character */
                    x.c = 0;
                    if (offsets != null) {
                        x.prevSourceIndex = x.sourceIndex;
                        x.sourceIndex = x.nextSourceIndex;
                    }
                    continue;
                } else {
                    /* target is full */
                    cr[0] = CoderResult.OVERFLOW;
                    break;
                }
            }
            
            /*
             * the end of the input stream and detection of truncated input
             * are handled by the framework, but for EBCDIC_STATEFUL conversion
             * we need to emit an SI at the very end
             * 
             * conditions:
             *  successful
             *  EBCDIC_STATEFUL in DBCS mode
             *  end of input and no truncated input
             */
            if (!cr[0].isError() && outputType == MBCS_OUTPUT_2_SISO && x.prevLength == 2 && flush && !source.hasRemaining() && x.c == 0) {
                /* EBCDIC_STATEFUL ending with DBCS: emit an SI to return the output stream to SBCS */
                if (targetCapacity > 0) {
                    target.put((byte)UConverterConstants.SI);
                    if (offsets != null) {
                        /* set the last source character's index (sourceIndex points at sourceLimit now) */
                        offsets.put(x.prevSourceIndex);
                    }
                } else {
                    /* target is full */
                    errorBuffer[0] = UConverterConstants.SI;
                    errorBufferLength = 1;
                    cr[0] = CoderResult.OVERFLOW;
                }
                x.prevLength = 1; /* we switched into SBCS */
            }
            /* set the converter state back into UConverter */
            fromUChar32 = x.c;
            fromUnicodeStatus = x.prevLength;
            
            return cr[0];
        }

        /*
         * This version of ucnv_MBCSFromUnicode() is optimized for single-byte codepages that map only to and from the
         * BMP. In addition to single-byte/state optimizations, the offset calculations become much easier.
         */
        private CoderResult cnvMBCSSingleFromBMPWithOffsets(CharBuffer source, ByteBuffer target, IntBuffer offsets,
                boolean flush) {

            CoderResult[] cr = { CoderResult.UNDERFLOW };

            int sourceArrayIndex, lastSource;
            int targetCapacity, length;
            char[] table;
            byte[] results;

            int c, sourceIndex;
            char value, minValue;

            /* set up the local pointers */
            sourceArrayIndex = source.position();
            targetCapacity = target.remaining();
            table = sharedData.mbcs.fromUnicodeTable;

            if ((options & UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                results = sharedData.mbcs.swapLFNLFromUnicodeBytes; // agljport:comment should swapLFNLFromUnicodeBytes
                // be a ByteBuffer so results can be a 16-bit view
                // of it?
            } else {
                results = sharedData.mbcs.fromUnicodeBytes; // agljport:comment should swapLFNLFromUnicodeBytes be a
                // ByteBuffer so results can be a 16-bit view of it?
            }

            if (useFallback) {
                /* use all roundtrip and fallback results */
                minValue = 0x800;
            } else {
                /* use only roundtrips and fallbacks from private-use characters */
                minValue = 0xc00;
            }

            /* get the converter state from UConverter */
            c = fromUChar32;

            /* sourceIndex=-1 if the current character began in the previous buffer */
            sourceIndex = c == 0 ? 0 : -1;
            lastSource = sourceArrayIndex;

            /*
             * since the conversion here is 1:1 UChar:uint8_t, we need only one counter for the minimum of the
             * sourceLength and targetCapacity
             */
            length = source.limit() - sourceArrayIndex;
            if (length < targetCapacity) {
                targetCapacity = length;
            }

            boolean doloop = true;
            if (c != 0 && targetCapacity > 0) {
                SideEffectsSingleBMP x = new SideEffectsSingleBMP(c, sourceArrayIndex);
                doloop = getTrailSingleBMP(source, x, cr);
                c = x.c;
                sourceArrayIndex = x.sourceArrayIndex;
            }

            if (doloop) {
                while (targetCapacity > 0) {
                    /*
                     * Get a correct Unicode code point: a single UChar for a BMP code point or a matched surrogate pair
                     * for a "supplementary code point".
                     */
                    c = source.get(sourceArrayIndex++);
                    /*
                     * Do not immediately check for single surrogates: Assume that they are unassigned and check for
                     * them in that case. This speeds up the conversion of assigned characters.
                     */
                    /* convert the Unicode code point in c into codepage bytes */
                    value = MBCS_SINGLE_RESULT_FROM_U(table, results, c);

                    /* is this code point assigned, or do we use fallbacks? */
                    if (value >= minValue) {
                        /* assigned, write the output character bytes from value and length */
                        /* length==1 */
                        /* this is easy because we know that there is enough space */
                        target.put((byte) value);
                        --targetCapacity;

                        /* normal end of conversion: prepare for a new character */
                        c = 0;
                        continue;
                    } else if (!UTF16.isSurrogate((char) c)) {
                        /* normal, unassigned BMP character */
                    } else if (UTF16.isLeadSurrogate((char) c)) {
                        // getTrail:
                        SideEffectsSingleBMP x = new SideEffectsSingleBMP(c, sourceArrayIndex);
                        doloop = getTrailSingleBMP(source, x, cr);
                        c = x.c;
                        sourceArrayIndex = x.sourceArrayIndex;
                        if (!doloop)
                            break;
                    } else {
                        /* this is an unmatched trail code unit (2nd surrogate) */
                        /* callback(illegal) */
                        cr[0] = CoderResult.malformedForLength(1);
                        break;
                    }

                    /* c does not have a mapping */

                    /* get the number of code units for c to correctly advance sourceIndex */
                    length = UTF16.getCharCount(c);

                    /* set offsets since the start or the last extension */
                    if (offsets != null) {
                        int count = sourceArrayIndex - lastSource;

                        /* do not set the offset for this character */
                        count -= length;

                        while (count > 0) {
                            offsets.put(sourceIndex++);
                            --count;
                        }
                        /* offsets and sourceIndex are now set for the current character */
                    }

                    /* try an extension mapping */
                    lastSource = sourceArrayIndex;
                    source.position(sourceArrayIndex);
                    c = fromU(c, source, target, offsets, sourceIndex, length, flush, cr);
                    sourceArrayIndex = source.position();
                    sourceIndex += length + (sourceArrayIndex - lastSource);
                    lastSource = sourceArrayIndex;

                    if (cr[0].isError()) {
                        /* not mappable or buffer overflow */
                        break;
                    } else {
                        /* a mapping was written to the target, continue */

                        /* recalculate the targetCapacity after an extension mapping */
                        targetCapacity = target.remaining();
                        length = source.limit() - sourceArrayIndex;
                        if (length < targetCapacity) {
                            targetCapacity = length;
                        }
                    }
                }
            }

            if (sourceArrayIndex < source.limit() && !target.hasRemaining()) {
                /* target is full */
                cr[0] = CoderResult.OVERFLOW;
            }

            /* set offsets since the start or the last callback */
            if (offsets != null) {
                int count = sourceArrayIndex - lastSource;
                while (count > 0) {
                    offsets.put(sourceIndex++);
                    --count;
                }
            }

            /* set the converter state back into UConverter */
            fromUChar32 = c;

            /* write back the updated pointers */
            source.position(sourceArrayIndex);

            return cr[0];
        }

        /* This version of ucnv_MBCSFromUnicodeWithOffsets() is optimized for single-byte codepages. */
        private CoderResult cnvMBCSSingleFromUnicodeWithOffsets(CharBuffer source, ByteBuffer target,
                IntBuffer offsets, boolean flush) {

            CoderResult[] cr = { CoderResult.UNDERFLOW };

            int sourceArrayIndex;

            char[] table;
            byte[] results; // agljport:comment results is used to to get 16-bit values out of byte[] array

            int c;
            int sourceIndex, nextSourceIndex;

            char value, minValue;

            /* set up the local pointers */
            short uniMask;
            sourceArrayIndex = source.position();

            table = sharedData.mbcs.fromUnicodeTable;

            if ((options & UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                results = sharedData.mbcs.swapLFNLFromUnicodeBytes; // agljport:comment should swapLFNLFromUnicodeBytes
                // be a ByteBuffer so results can be a 16-bit view
                // of it?
            } else {
                results = sharedData.mbcs.fromUnicodeBytes; // agljport:comment should swapLFNLFromUnicodeBytes be a
                // ByteBuffer so results can be a 16-bit view of it?
            }

            if (useFallback) {
                /* use all roundtrip and fallback results */
                minValue = 0x800;
            } else {
                /* use only roundtrips and fallbacks from private-use characters */
                minValue = 0xc00;
            }
            // agljport:comment hasSupplementary only used in getTrail block which now simply repeats the mask operation
            uniMask = sharedData.mbcs.unicodeMask;

            /* get the converter state from UConverter */
            c = fromUChar32;

            /* sourceIndex=-1 if the current character began in the previous buffer */
            sourceIndex = c == 0 ? 0 : -1;
            nextSourceIndex = 0;

            boolean doloop = true;
            boolean doread = true;
            if (c != 0 && target.hasRemaining()) {
                if (UTF16.isLeadSurrogate((char) c)) {
                    SideEffectsDouble x = new SideEffectsDouble(c, sourceArrayIndex, sourceIndex, nextSourceIndex);
                    doloop = getTrailDouble(source, target, uniMask, x, flush, cr);
                    doread = x.doread;
                    c = x.c;
                    sourceArrayIndex = x.sourceArrayIndex;
                    sourceIndex = x.sourceIndex;
                    nextSourceIndex = x.nextSourceIndex;
                } else {
                    doread = false;
                }
            }

            if (doloop) {
                while (!doread || sourceArrayIndex < source.limit()) {
                    /*
                     * This following test is to see if available input would overflow the output. It does not catch
                     * output of more than one byte that overflows as a result of a multi-byte character or callback
                     * output from the last source character. Therefore, those situations also test for overflows and
                     * will then break the loop, too.
                     */
                    if (target.hasRemaining()) {
                        /*
                         * Get a correct Unicode code point: a single UChar for a BMP code point or a matched surrogate
                         * pair for a "supplementary code point".
                         */

                        if (doread) {
                            c = source.get(sourceArrayIndex++);
                            ++nextSourceIndex;
                            if (UTF16.isSurrogate((char) c)) {
                                if (UTF16.isLeadSurrogate((char) c)) {
                                    // getTrail:
                                    SideEffectsDouble x = new SideEffectsDouble(c, sourceArrayIndex, sourceIndex,
                                            nextSourceIndex);
                                    doloop = getTrailDouble(source, target, uniMask, x, flush, cr);
                                    c = x.c;
                                    sourceArrayIndex = x.sourceArrayIndex;
                                    sourceIndex = x.sourceIndex;
                                    nextSourceIndex = x.nextSourceIndex;
                                    if (x.doread) {
                                        if (doloop)
                                            continue;
                                        else
                                            break;
                                    }
                                } else {
                                    /* this is an unmatched trail code unit (2nd surrogate) */
                                    /* callback(illegal) */
                                    cr[0] = CoderResult.malformedForLength(1);
                                    break;
                                }
                            }
                        } else {
                            doread = true;
                        }

                        /* convert the Unicode code point in c into codepage bytes */
                        value = MBCS_SINGLE_RESULT_FROM_U(table, results, c);

                        /* is this code point assigned, or do we use fallbacks? */
                        if (value >= minValue) {
                            /* assigned, write the output character bytes from value and length */
                            /* length==1 */
                            /* this is easy because we know that there is enough space */
                            target.put((byte) value);
                            if (offsets != null) {
                                offsets.put(sourceIndex);
                            }

                            /* normal end of conversion: prepare for a new character */
                            c = 0;
                            sourceIndex = nextSourceIndex;
                        } else { /* unassigned */
                            /* try an extension mapping */
                            SideEffectsDouble x = new SideEffectsDouble(c, sourceArrayIndex, sourceIndex,
                                    nextSourceIndex);
                            doloop = unassignedDouble(source, target, x, flush, cr);
                            c = x.c;
                            sourceArrayIndex = x.sourceArrayIndex;
                            sourceIndex = x.sourceIndex;
                            nextSourceIndex = x.nextSourceIndex;
                            if (!doloop)
                                break;
                        }
                    } else {
                        /* target is full */
                        cr[0] = CoderResult.OVERFLOW;
                        break;
                    }
                }
            }

            /* set the converter state back into UConverter */
            fromUChar32 = c;

            /* write back the updated pointers */
            source.position(sourceArrayIndex);

            return cr[0];
        }

        /* This version of ucnv_MBCSFromUnicodeWithOffsets() is optimized for double-byte codepages. */
        private CoderResult cnvMBCSDoubleFromUnicodeWithOffsets(CharBuffer source, ByteBuffer target,
                IntBuffer offsets, boolean flush) {
            CoderResult[] cr = { CoderResult.UNDERFLOW };

            int sourceArrayIndex;

            char[] table;
            byte[] bytes;

            int c, sourceIndex, nextSourceIndex;

            int stage2Entry;
            int value;
            int length;
            short uniMask;

            /* use optimized function if possible */
            uniMask = sharedData.mbcs.unicodeMask;

            /* set up the local pointers */
            sourceArrayIndex = source.position();

            table = sharedData.mbcs.fromUnicodeTable;

            if ((options & UConverterConstants.OPTION_SWAP_LFNL) != 0) {
                bytes = sharedData.mbcs.swapLFNLFromUnicodeBytes;
            } else {
                bytes = sharedData.mbcs.fromUnicodeBytes;
            }

            /* get the converter state from UConverter */
            c = fromUChar32;

            /* sourceIndex=-1 if the current character began in the previous buffer */
            sourceIndex = c == 0 ? 0 : -1;
            nextSourceIndex = 0;

            /* conversion loop */
            boolean doloop = true;
            boolean doread = true;
            if (c != 0 && target.hasRemaining()) {
                if (UTF16.isLeadSurrogate((char) c)) {
                    SideEffectsDouble x = new SideEffectsDouble(c, sourceArrayIndex, sourceIndex, nextSourceIndex);
                    doloop = getTrailDouble(source, target, uniMask, x, flush, cr);
                    doread = x.doread;
                    c = x.c;
                    sourceArrayIndex = x.sourceArrayIndex;
                    sourceIndex = x.sourceIndex;
                    nextSourceIndex = x.nextSourceIndex;
                } else {
                    doread = false;
                }
            }

            if (doloop) {
                while (!doread || sourceArrayIndex < source.limit()) {
                    /*
                     * This following test is to see if available input would overflow the output. It does not catch
                     * output of more than one byte that overflows as a result of a multi-byte character or callback
                     * output from the last source character. Therefore, those situations also test for overflows and
                     * will then break the loop, too.
                     */
                    if (target.hasRemaining()) {
                        if (doread) {
                            /*
                             * Get a correct Unicode code point: a single UChar for a BMP code point or a matched
                             * surrogate pair for a "supplementary code point".
                             */
                            c = source.get(sourceArrayIndex++);
                            ++nextSourceIndex;
                            /*
                             * This also tests if the codepage maps single surrogates. If it does, then surrogates are
                             * not paired but mapped separately. Note that in this case unmatched surrogates are not
                             * detected.
                             */
                            if (UTF16.isSurrogate((char) c) && (uniMask & UConverterConstants.HAS_SURROGATES) == 0) {
                                if (UTF16.isLeadSurrogate((char) c)) {
                                    // getTrail:
                                    SideEffectsDouble x = new SideEffectsDouble(c, sourceArrayIndex, sourceIndex,
                                            nextSourceIndex);
                                    doloop = getTrailDouble(source, target, uniMask, x, flush, cr);
                                    c = x.c;
                                    sourceArrayIndex = x.sourceArrayIndex;
                                    sourceIndex = x.sourceIndex;
                                    nextSourceIndex = x.nextSourceIndex;

                                    if (x.doread) {
                                        if (doloop)
                                            continue;
                                        else
                                            break;
                                    }
                                } else {
                                    /* this is an unmatched trail code unit (2nd surrogate) */
                                    /* callback(illegal) */
                                    cr[0] = CoderResult.malformedForLength(1);
                                    break;
                                }
                            }
                        } else {
                            doread = true;
                        }

                        /* convert the Unicode code point in c into codepage bytes */
                        stage2Entry = MBCS_STAGE_2_FROM_U(table, c);

                        /* get the bytes and the length for the output */
                        /* MBCS_OUTPUT_2 */
                        value = MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
                        if ((value & UConverterConstants.UNSIGNED_INT_MASK) <= 0xff) {
                            length = 1;
                        } else {
                            length = 2;
                        }

                        /* is this code point assigned, or do we use fallbacks? */
                        if (!(MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c) || (isFromUUseFallback(c) && value != 0))) {
                            /*
                             * We allow a 0 byte output if the "assigned" bit is set for this entry. There is no way
                             * with this data structure for fallback output to be a zero byte.
                             */

                            // unassigned:
                            SideEffectsDouble x = new SideEffectsDouble(c, sourceArrayIndex, sourceIndex,
                                    nextSourceIndex);

                            doloop = unassignedDouble(source, target, x, flush, cr);
                            c = x.c;
                            sourceArrayIndex = x.sourceArrayIndex;
                            sourceIndex = x.sourceIndex;
                            nextSourceIndex = x.nextSourceIndex;
                            if (doloop)
                                continue;
                            else
                                break;
                        }

                        /* write the output character bytes from value and length */
                        /* from the first if in the loop we know that targetCapacity>0 */
                        if (length == 1) {
                            /* this is easy because we know that there is enough space */
                            target.put((byte) value);
                            if (offsets != null) {
                                offsets.put(sourceIndex);
                            }
                        } else /* length==2 */{
                            target.put((byte) (value >>> 8));
                            if (2 <= target.remaining()) {
                                target.put((byte) value);
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                    offsets.put(sourceIndex);
                                }
                            } else {
                                if (offsets != null) {
                                    offsets.put(sourceIndex);
                                }
                                errorBuffer[0] = (byte) value;
                                errorBufferLength = 1;

                                /* target overflow */
                                cr[0] = CoderResult.OVERFLOW;
                                c = 0;
                                break;
                            }
                        }

                        /* normal end of conversion: prepare for a new character */
                        c = 0;
                        sourceIndex = nextSourceIndex;
                        continue;
                    } else {
                        /* target is full */
                        cr[0] = CoderResult.OVERFLOW;
                        break;
                    }
                }
            }

            /* set the converter state back into UConverter */
            fromUChar32 = c;

            /* write back the updated pointers */
            source.position(sourceArrayIndex);

            return cr[0];
        }

        private final class SideEffectsSingleBMP {
            int c, sourceArrayIndex;

            SideEffectsSingleBMP(int c_, int sourceArrayIndex_) {
                c = c_;
                sourceArrayIndex = sourceArrayIndex_;
            }
        }

        // function made out of block labeled getTrail in ucnv_MBCSSingleFromUnicodeWithOffsets
        // assumes input c is lead surrogate
        private final boolean getTrailSingleBMP(CharBuffer source, SideEffectsSingleBMP x, CoderResult[] cr) {
            if (x.sourceArrayIndex < source.limit()) {
                /* test the following code unit */
                char trail = source.get(x.sourceArrayIndex);
                if (UTF16.isTrailSurrogate(trail)) {
                    ++x.sourceArrayIndex;
                    x.c = UCharacter.getCodePoint((char) x.c, trail);
                    /* this codepage does not map supplementary code points */
                    /* callback(unassigned) */
                    cr[0] = CoderResult.unmappableForLength(2);
                    return false;
                } else {
                    /* this is an unmatched lead code unit (1st surrogate) */
                    /* callback(illegal) */
                    cr[0] = CoderResult.malformedForLength(1);
                    return false;
                }
            } else {
                /* no more input */
                return false;
            }
            // return true;
        }

        private final class SideEffects {
            int c, sourceArrayIndex, sourceIndex, nextSourceIndex, prevSourceIndex, prevLength;
            boolean doread = true;

            SideEffects(int c_, int sourceArrayIndex_, int sourceIndex_, int nextSourceIndex_, int prevSourceIndex_,
                    int prevLength_) {
                c = c_;
                sourceArrayIndex = sourceArrayIndex_;
                sourceIndex = sourceIndex_;
                nextSourceIndex = nextSourceIndex_;
                prevSourceIndex = prevSourceIndex_;
                prevLength = prevLength_;
            }
        }

        // function made out of block labeled getTrail in ucnv_MBCSFromUnicodeWithOffsets
        // assumes input c is lead surrogate
        private final boolean getTrail(CharBuffer source, ByteBuffer target, int uniMask, SideEffects x,
                boolean flush, CoderResult[] cr) {
            if (x.sourceArrayIndex < source.limit()) {
                /* test the following code unit */
                char trail = source.get(x.sourceArrayIndex);
                if (UTF16.isTrailSurrogate(trail)) {
                    ++x.sourceArrayIndex;
                    ++x.nextSourceIndex;
                    /* convert this supplementary code point */
                    x.c = UCharacter.getCodePoint((char) x.c, trail);
                    if ((uniMask & UConverterConstants.HAS_SUPPLEMENTARY) == 0) {
                        /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
                        fromUnicodeStatus = x.prevLength; /* save the old state */
                        /* callback(unassigned) */
                        x.doread = true;
                        return unassigned(source, target, null, x, flush, cr);
                    } else {
                        x.doread = false;
                        return true;
                    }
                } else {
                    /* this is an unmatched lead code unit (1st surrogate) */
                    /* callback(illegal) */
                    cr[0] = CoderResult.malformedForLength(1);
                    return false;
                }
            } else {
                /* no more input */
                return false;
            }
        }

        // function made out of block labeled unassigned in ucnv_MBCSFromUnicodeWithOffsets
        private final boolean unassigned(CharBuffer source, ByteBuffer target, IntBuffer offsets, SideEffects x,
                boolean flush, CoderResult[] cr) {
            /* try an extension mapping */
            int sourceBegin = x.sourceArrayIndex;
            source.position(x.sourceArrayIndex);
            x.c = fromU(x.c, source, target, null, x.sourceIndex, x.nextSourceIndex, flush, cr);
            x.sourceArrayIndex = source.position();
            x.nextSourceIndex += x.sourceArrayIndex - sourceBegin;
            x.prevLength = (int) fromUnicodeStatus;

            if (cr[0].isError()) {
                /* not mappable or buffer overflow */
                return false;
            } else {
                /* a mapping was written to the target, continue */

                /* recalculate the targetCapacity after an extension mapping */
                // x.targetCapacity=pArgs.targetLimit-x.targetArrayIndex;
                /* normal end of conversion: prepare for a new character */
                if (offsets != null) {
                    x.prevSourceIndex = x.sourceIndex;
                    x.sourceIndex = x.nextSourceIndex;
                }
                return true;
            }
        }

        private final class SideEffectsDouble {
            int c, sourceArrayIndex, sourceIndex, nextSourceIndex;
            boolean doread = true;

            SideEffectsDouble(int c_, int sourceArrayIndex_, int sourceIndex_, int nextSourceIndex_) {
                c = c_;
                sourceArrayIndex = sourceArrayIndex_;
                sourceIndex = sourceIndex_;
                nextSourceIndex = nextSourceIndex_;
            }
        }

        // function made out of block labeled getTrail in ucnv_MBCSDoubleFromUnicodeWithOffsets
        // assumes input c is lead surrogate
        private final boolean getTrailDouble(CharBuffer source, ByteBuffer target, int uniMask,
                SideEffectsDouble x, boolean flush, CoderResult[] cr) {
            if (x.sourceArrayIndex < source.limit()) {
                /* test the following code unit */
                char trail = source.get(x.sourceArrayIndex);
                if (UTF16.isTrailSurrogate(trail)) {
                    ++x.sourceArrayIndex;
                    ++x.nextSourceIndex;
                    /* convert this supplementary code point */
                    x.c = UCharacter.getCodePoint((char) x.c, trail);
                    if ((uniMask & UConverterConstants.HAS_SUPPLEMENTARY) == 0) {
                        /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
                        /* callback(unassigned) */
                        x.doread = true;
                        return unassignedDouble(source, target, x, flush, cr);
                    } else {
                        x.doread = false;
                        return true;
                    }
                } else {
                    /* this is an unmatched lead code unit (1st surrogate) */
                    /* callback(illegal) */
                    cr[0] = CoderResult.malformedForLength(1);
                    return false;
                }
            } else {
                /* no more input */
                return false;
            }
        }

        // function made out of block labeled unassigned in ucnv_MBCSDoubleFromUnicodeWithOffsets
        private final boolean unassignedDouble(CharBuffer source, ByteBuffer target, SideEffectsDouble x,
                boolean flush, CoderResult[] cr) {
            /* try an extension mapping */
            int sourceBegin = x.sourceArrayIndex;
            source.position(x.sourceArrayIndex);
            x.c = fromU(x.c, source, target, null, x.sourceIndex, x.nextSourceIndex, flush, cr);
            x.sourceArrayIndex = source.position();
            x.nextSourceIndex += x.sourceArrayIndex - sourceBegin;

            if (cr[0].isError()) {
                /* not mappable or buffer overflow */
                return false;
            } else {
                /* a mapping was written to the target, continue */

                /* recalculate the targetCapacity after an extension mapping */
                // x.targetCapacity=pArgs.targetLimit-x.targetArrayIndex;
                /* normal end of conversion: prepare for a new character */
                x.sourceIndex = x.nextSourceIndex;
                return true;
            }
        }

        /**
         * Overrides super class method
         * 
         * @param encoder
         * @param source
         * @param target
         * @param offsets
         * @return
         */
        protected CoderResult cbFromUWriteSub(CharsetEncoderICU encoder, CharBuffer source, ByteBuffer target,
                IntBuffer offsets) {
            CharsetMBCS cs = (CharsetMBCS) encoder.charset();
            byte[] subchar;
            int length;

            if (cs.subChar1 != 0
                    && (cs.sharedData.mbcs.extIndexes != null ? encoder.useSubChar1
                            : (encoder.invalidUCharBuffer[0] <= 0xff))) {
                /*
                 * select subChar1 if it is set (not 0) and the unmappable Unicode code point is up to U+00ff (IBM MBCS
                 * behavior)
                 */
                subchar = new byte[] { cs.subChar1 };
                length = 1;
            } else {
                /* select subChar in all other cases */
                subchar = cs.subChar;
                length = cs.subCharLen;
            }

            /* reset the selector for the next code point */
            encoder.useSubChar1 = false;

            if (cs.sharedData.mbcs.outputType == MBCS_OUTPUT_2_SISO) {
                byte[] buffer = new byte[4];
                int i = 0;

                /* fromUnicodeStatus contains prevLength */
                switch (length) {
                case 1:
                    if (encoder.fromUnicodeStatus == 2) {
                        /* DBCS mode and SBCS sub char: change to SBCS */
                        encoder.fromUnicodeStatus = 1;
                        buffer[i++] = UConverterConstants.SI;
                    }
                    buffer[i++] = subchar[0];
                    break;
                case 2:
                    if (encoder.fromUnicodeStatus <= 1) {
                        /* SBCS mode and DBCS sub char: change to DBCS */
                        encoder.fromUnicodeStatus = 2;
                        buffer[i++] = UConverterConstants.SO;
                    }
                    buffer[i++] = subchar[0];
                    buffer[i++] = subchar[1];
                    break;
                default:
                    throw new IllegalArgumentException();
                }

                subchar = buffer;
                length = i;
            }
            return CharsetEncoderICU.fromUWriteBytes(encoder, subchar, 0, length, target, offsets, source.position());
        }

        /**
         * Gets called whenever CharsetEncoder.replaceWith gets called. allowReplacementChanges only allows subChar and
         * subChar1 to be modified outside construction (since replaceWith is called once during construction).
         * 
         * @param replacement
         *            The replacement for subchar.
         */
        protected void implReplaceWith(byte[] replacement) {
            if (allowReplacementChanges) {
                CharsetMBCS cs = (CharsetMBCS) this.charset();

                System.arraycopy(replacement, 0, cs.subChar, 0, replacement.length);
                cs.subCharLen = (byte) replacement.length;
                cs.subChar1 = 0;
            }
        }
    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderMBCS(this);
    }

    public CharsetEncoder newEncoder() {
        return new CharsetEncoderMBCS(this);
    }
    
    void MBCSGetFilteredUnicodeSetForUnicode(UConverterSharedData data, UnicodeSet setFillIn, int which, int filter){
        UConverterMBCSTable mbcsTable;
        char[] table;
        char st1,maxStage1, st2;
        int st3;
        int c ;
        
        mbcsTable = data.mbcs;
        table = mbcsTable.fromUnicodeTable; 
        if((mbcsTable.unicodeMask & UConverterConstants.HAS_SUPPLEMENTARY)!=0){
            maxStage1 = 0x440;
        }
        else{
            maxStage1 = 0x40;
        }
        c=0; /* keep track of current code point while enumerating */
        
        if(mbcsTable.outputType==MBCS_OUTPUT_1){
            char stage2, stage3;
            char minValue;
            CharBuffer results;
            results = ByteBuffer.wrap(mbcsTable.fromUnicodeBytes).asCharBuffer();
                                   
            if(which==ROUNDTRIP_SET) {
                /* use only roundtrips */
                minValue=0xf00;
            } else {
                /* use all roundtrip and fallback results */
                minValue=0x800;
            }
            for(st1=0;st1<maxStage1;++st1){
                st2 = table[st1];
                if(st2>maxStage1){
                    stage2 = st2;
                    for(st2=0; st2<64; ++st2){
                        st3 = table[stage2 + st2];
                        if(st3!=0){
                            /*read the stage 3 block */
                            stage3 = (char)st3;
                            do {
                                if(results.get(stage3++)>=minValue){
                                     setFillIn.add(c);
                                }
                               
                            }while((++c&0xf) !=0);
                          } else {
                            c+= 16; /*empty stage 2 block */
                        }
                    }
                } else {
                    c+=1024; /* empty stage 2 block */
                }
            }
        } else {
            int stage2,stage3;
            byte[] bytes;
            int st3Multiplier;
            int value;
            boolean useFallBack;
            bytes = mbcsTable.fromUnicodeBytes;
            useFallBack = (which == ROUNDTRIP_AND_FALLBACK_SET);
            switch(mbcsTable.outputType) {
            case MBCS_OUTPUT_3:
            case MBCS_OUTPUT_4_EUC:
                st3Multiplier = 3;
                break;
            case MBCS_OUTPUT_4:
                st3Multiplier =4;
                break;
            default:
                st3Multiplier =2;
                break;
            }
            //ByteBuffer buffer = (ByteBuffer)charTobyte(table);
            
            for(st1=0;st1<maxStage1;++st1){
                st2 = table[st1]; 
                if(st2>(maxStage1>>1)){
                    stage2 =  st2 ;
                    for(st2=0;st2<128;++st2){
                        /*read the stage 3 block */
                        st3 = table[stage2*2 + st2]<<16;
                        st3+=table[stage2*2 + ++st2];
                        if(st3!=0){
                        //if((st3=table[stage2+st2])!=0){
                            stage3 = st3Multiplier*16*(int)(st3&UConverterConstants.UNSIGNED_SHORT_MASK);
                            
                            /* get the roundtrip flags for the stage 3 block */
                            st3>>=16;
                            st3 &= UConverterConstants.UNSIGNED_SHORT_MASK;
                            switch(filter) {
                            case UCNV_SET_FILTER_NONE:
                                do {
                                    
                                   if((st3&1)!=0){
                                        setFillIn.add(c);
                                        stage3+=st3Multiplier;
                                   }else if (useFallBack) {
                                        
                                        char b =0;
                                        switch(st3Multiplier) {
                                        case 4 :
                                           
                                            b|= ByteBuffer.wrap(bytes).getChar(stage3++);
                                           
                                        case 3 :
                                            
                                            b|= ByteBuffer.wrap(bytes).getChar(stage3++);
                                           
                                        case 2 :
                                           
                                            b|= ByteBuffer.wrap(bytes).getChar(stage3) | ByteBuffer.wrap(bytes).getChar(stage3+1);
                                            stage3+=2;
                                        default:
                                            break;
                                        }
                                        if(b!=0) {
                                            setFillIn.add(c);
                                        }
                                    }
                                    st3>>=1;
                                }while((++c&0xf)!=0);
                                break;
                            case UCNV_SET_FILTER_DBCS_ONLY:
                                /* Ignore single bytes results (<0x100). */
                                do {
                                    if(((st3&1) != 0 || useFallBack) && 
                                            (UConverterConstants.UNSIGNED_SHORT_MASK & (ByteBuffer.wrap(bytes).getChar(stage3))) >= 0x100){
                                        setFillIn.add(c);
                                    }
                                    st3>>=1;
                                    stage3+=2;
                                }while((++c&0xf) != 0);
                               break;
                            case UCNV_SET_FILTER_2022_CN :
                                /* only add code points that map to CNS 11643 planes 1&2 for non-EXT ISO-2202-CN. */
                                do {
                                    if(((st3&1) != 0 || useFallBack) && 
                                            ((value= (UConverterConstants.UNSIGNED_BYTE_MASK & (ByteBuffer.wrap(bytes).get(stage3))))==0x81 || value==0x82) ){
                                        setFillIn.add(c);
                                    }
                                    st3>>=1;
                                    stage3+=3;
                                }while((++c&0xf)!=0);
                                break;
                            case UCNV_SET_FILTER_SJIS:
                                /* only add code points that map tp Shift-JIS codes corrosponding to JIS X 0280. */
                                do{
                                    
                                    if(((st3&1) != 0 || useFallBack) && (value=(UConverterConstants.UNSIGNED_SHORT_MASK & (ByteBuffer.wrap(bytes).getChar(stage3))))>=0x8140 && value<=0xeffc){
                                        setFillIn.add(c);
                                    }
                                    st3>>=1;
                                    stage3+=2;
                                }while((++c&0xf)!=0);
                                break;
                            case UCNV_SET_FILTER_GR94DBCS:
                                /* only add code points that maps to ISO 2022 GR 94 DBCS codes*/
                                do {
                                    if(((st3&1) != 0 || useFallBack) && 
                                            (UConverterConstants.UNSIGNED_SHORT_MASK & ((value=(UConverterConstants.UNSIGNED_SHORT_MASK & (ByteBuffer.wrap(bytes).getChar(stage3))))- 0xa1a1))<=(0xfefe - 0xa1a1) && 
                                            (UConverterConstants.UNSIGNED_BYTE_MASK & (value - 0xa1)) <= (0xfe - 0xa1)){
                                        setFillIn.add(c);
                                    }
                                    st3>>=1;
                                    stage3+=2;
                                }while((++c&0xf)!=0);
                                break;
                            case UCNV_SET_FILTER_HZ:
                                /*Only add code points that are suitable for HZ DBCS*/
                                do {
                                    if( ((st3&1) != 0 || useFallBack) && 
                                            (UConverterConstants.UNSIGNED_SHORT_MASK & ((value=(UConverterConstants.UNSIGNED_SHORT_MASK & (ByteBuffer.wrap(bytes).getChar(stage3))))-0xa1a1))<=(0xfdfe - 0xa1a1) &&
                                            (UConverterConstants.UNSIGNED_BYTE_MASK & (value - 0xa1)) <= (0xfe - 0xa1)){
                                        setFillIn.add(c);
                                    }
                                    st3>>=1;
                                    stage3+=2;
                                }while((++c&0xf) != 0);
                                break;
                            default:
                                return;
                            }
                        } else {
                            c+=16; /* empty stage 3 block */
                        }
                    }
                } else {
                    c+=1024; /*empty stage2 block */
                }
            }
        }
        extGetUnicodeSet(setFillIn, which, filter, data);
    }
   
    static void extGetUnicodeSetString(ByteBuffer cx,UnicodeSet setFillIn, boolean useFallback, 
        int minLength, int c, char s[],int length,int sectionIndex){
        CharBuffer fromUSectionUChar;
        IntBuffer fromUSectionValues;
        fromUSectionUChar = (CharBuffer)ARRAY(cx, EXT_FROM_U_UCHARS_INDEX,char.class );
        fromUSectionValues = (IntBuffer)ARRAY(cx, EXT_FROM_U_VALUES_INDEX,int.class );
        int fromUSectionUCharIndex = fromUSectionUChar.position()+sectionIndex;
        int fromUSectionValuesIndex = fromUSectionValues.position()+sectionIndex;
        int value, i, count;
        
        /* read first pair of the section */
       count = fromUSectionUChar.get(fromUSectionUCharIndex++);
       value = fromUSectionValues.get(fromUSectionValuesIndex++);
       if(value!=0 && (FROM_U_IS_ROUNDTRIP(value) || useFallback) && FROM_U_GET_LENGTH(value)>=minLength) {
           if(c>=0){
               setFillIn.add(c);
           } else {
               String normalizedString=""; // String for composite characters 
               for(int j=0; j<length;j++){
                   normalizedString+=s[j];
               }
               for(int j=0;j<length;j++){
                   setFillIn.add(normalizedString);
               }
             
             }
       }
       
       for(i=0; i<count; ++i){
           s[length] = fromUSectionUChar.get(fromUSectionUCharIndex + i);
           value = fromUSectionValues.get(fromUSectionValuesIndex + i);
           
           if(value==0) {
               /* no mapping, do nothing */
           } else if (FROM_U_IS_PARTIAL(value)) {
               extGetUnicodeSetString( cx, setFillIn, useFallback, minLength, UConverterConstants.U_SENTINEL, s, length+1,
                       FROM_U_GET_PARTIAL_INDEX(value));
           } else if ((useFallback ? (value&FROM_U_RESERVED_MASK)==0:((value&(FROM_U_ROUNDTRIP_FLAG|FROM_U_RESERVED_MASK))==FROM_U_ROUNDTRIP_FLAG)) 
                   && FROM_U_GET_LENGTH(value)>=minLength) {
               String normalizedString=""; // String for composite characters 
               for(int j=0; j<(length+1);j++){
                   normalizedString+=s[j];
               }
             setFillIn.add(normalizedString);
           }
       }
        
    }
    
    
    static void extGetUnicodeSet(UnicodeSet setFillIn, int which, int filter, UConverterSharedData Data){
        int st1, stage1Length, st2, st3, minLength;
        int ps2, ps3;
        
        CharBuffer stage12, stage3;
        int value, length;
        IntBuffer stage3b;
        boolean useFallback;
        char s[] = new char[MAX_UCHARS];
        int c;
        ByteBuffer cx = Data.mbcs.extIndexes;
        if(cx == null){
            return;
        }
        stage12 = (CharBuffer)ARRAY(cx, EXT_FROM_U_STAGE_12_INDEX,char.class );
        stage3 = (CharBuffer)ARRAY(cx, EXT_FROM_U_STAGE_3_INDEX,char.class );
        stage3b = (IntBuffer)ARRAY(cx, EXT_FROM_U_STAGE_3B_INDEX,int.class );
        
        stage1Length = cx.asIntBuffer().get(EXT_FROM_U_STAGE_1_LENGTH);
        useFallback =(boolean)(which==ROUNDTRIP_AND_FALLBACK_SET);
        
        c = 0;
        if(filter == UCNV_SET_FILTER_2022_CN) {
            minLength = 3;
        } else if (Data.mbcs.outputType == MBCS_OUTPUT_DBCS_ONLY || filter != UCNV_SET_FILTER_NONE) {
            /* DBCS-only, ignore single-byte results */
            minLength = 2;
        } else {
            minLength = 1;
        }
        
        for(st1=0; st1< stage1Length; ++st1){
            st2 = stage12.get(st1);
            if(st2>stage1Length) {
                ps2 = st2;
                for(st2=0;st2<64;++st2){
                    st3=((int) stage12.get(ps2+st2))<<STAGE_2_LEFT_SHIFT;
                    if(st3!= 0){
                        ps3 = st3;
                        do {
                            value = stage3b.get((int)(UConverterConstants.UNSIGNED_SHORT_MASK&stage3.get(ps3++)));
                            if(value==0){
                                /* no mapping do nothing */
                            }else if (FROM_U_IS_PARTIAL(value)){
                                length = 0;
                                length=UTF16.append(s, length, c);
                                extGetUnicodeSetString(cx,setFillIn,useFallback,minLength,c,s,length,(int)FROM_U_GET_PARTIAL_INDEX(value));
                            } else if ((useFallback ?  (value&FROM_U_RESERVED_MASK)==0 :((value&(FROM_U_ROUNDTRIP_FLAG|FROM_U_RESERVED_MASK))== FROM_U_ROUNDTRIP_FLAG)) && 
                                    FROM_U_GET_LENGTH(value)>=minLength){
                                
                                switch(filter) {
                                case UCNV_SET_FILTER_2022_CN:
                                    if(!(FROM_U_GET_LENGTH(value)==3 && FROM_U_GET_DATA(value)<=0x82ffff)){
                                        continue;
                                    }
                                    break;
                                case UCNV_SET_FILTER_SJIS:
                                    if(!(FROM_U_GET_LENGTH(value)==2 && (value=FROM_U_GET_DATA(value))>=0x8140 && value<=0xeffc)){
                                        continue;
                                    }
                                    break;
                                case UCNV_SET_FILTER_GR94DBCS:
                                    if(!(FROM_U_GET_LENGTH(value)==2 && (UConverterConstants.UNSIGNED_SHORT_MASK & ((value=FROM_U_GET_DATA(value)) - 0xa1a1))<=(0xfefe - 0xa1a1) 
                                            && (UConverterConstants.UNSIGNED_BYTE_MASK & (value - 0xa1))<= (0xfe - 0xa1))){
                                        
                                        continue;
                                    }
                                    break;
                                case UCNV_SET_FILTER_HZ:
                                    if(!(FROM_U_GET_LENGTH(value)==2 && (UConverterConstants.UNSIGNED_SHORT_MASK & ((value=FROM_U_GET_DATA(value)) - 0xa1a1))<=(0xfdfe - 0xa1a1) 
                                            && (UConverterConstants.UNSIGNED_BYTE_MASK & (value - 0xa1))<= (0xfe - 0xa1))){
                                        continue;
                                    }
                                    break;
                                default:
                                    /*
                                     * UCNV_SET_FILTER_NONE,
                                     * or UCNV_SET_FILTER_DBCS_ONLY which is handled via minLength
                                     */
                                    break;
                                }
                                setFillIn.add(c);
                              
                            }
                        }while((++c&0xf) != 0);
                      
                    } else {
                        c+=16;   /* emplty stage3 block */
                    }
                }
            } else {
                c+=1024;  /* empty stage 2 block*/
            }
        }
    }
    
    void MBCSGetUnicodeSetForUnicode(UConverterSharedData data, UnicodeSet setFillIn, int which){
        MBCSGetFilteredUnicodeSetForUnicode(data, setFillIn, which, 
                this.sharedData.mbcs.outputType==MBCS_OUTPUT_DBCS_ONLY ? UCNV_SET_FILTER_DBCS_ONLY : UCNV_SET_FILTER_NONE );
    }
    
    void getUnicodeSetImpl( UnicodeSet setFillIn, int which){
        if((options & MBCS_OPTION_GB18030)!=0){
            setFillIn.add(0, 0xd7ff);
            setFillIn.add(0xe000, 0x10ffff);
        }
        else {
            this.MBCSGetUnicodeSetForUnicode(sharedData, setFillIn, which);
        }
    }

}
