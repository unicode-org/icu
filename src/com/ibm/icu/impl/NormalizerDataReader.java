/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
 
package com.ibm.icu.impl;
import java.io.*;
import com.ibm.icu.impl.ICUDebug;

/**
 * @version     1.0
 * @author        Ram Viswanadha
 */

    /*
     * Description of the format of unorm.icu version 2.1.
     *
     * Main change from version 1 to version 2:
     * Use of new, common Trie instead of normalization-specific tries.
     * Change to version 2.1: add third/auxiliary trie with associated data.
     *
     * For more details of how to use the data structures see the code
     * in unorm.cpp (runtime normalization code) and
     * in gennorm.c and gennorm/store.c (build-time data generation).
     *
     * For the serialized format of Trie see Trie.c/TrieHeader.
     *
     * - Overall partition
     *
     * unorm.icu customarily begins with a UDataInfo structure, see udata.h and .c.
     * After that there are the following structures:
     *
     * char indexes[INDEX_TOP];                   -- INDEX_TOP=32, see enum in this file
     *
     * Trie normTrie;                           -- size in bytes=indexes[INDEX_TRIE_SIZE]
     * 
     * char extraData[extraDataTop];            -- extraDataTop=indexes[INDEX_UCHAR_COUNT]
     *                                                 extraData[0] contains the number of units for
     *                                                 FC_NFKC_Closure (formatVersion>=2.1)
     *
     * char combiningTable[combiningTableTop];  -- combiningTableTop=indexes[INDEX_COMBINE_DATA_COUNT]
     *                                                 combiningTableTop may include one 16-bit padding unit
     *                                                 to make sure that fcdTrie is 32-bit-aligned
     *
     * Trie fcdTrie;                            -- size in bytes=indexes[INDEX_FCD_TRIE_SIZE]
     *
     * Trie auxTrie;                            -- size in bytes=indexes[INDEX_AUX_TRIE_SIZE]
     *
     * char canonStartSets[canonStartSetsTop]   -- canonStartSetsTop=indexes[INDEX_CANON_SET_COUNT]
     *                                                 serialized USets, see uset.c
     *
     *
     * The indexes array contains lengths and sizes of the following arrays and structures
     * as well as the following values:
     *  indexes[INDEX_COMBINE_FWD_COUNT]=combineFwdTop
     *      -- one more than the highest combining index computed for forward-only-combining characters
     *  indexes[INDEX_COMBINE_BOTH_COUNT]=combineBothTop-combineFwdTop
     *      -- number of combining indexes computed for both-ways-combining characters
     *  indexes[INDEX_COMBINE_BACK_COUNT]=combineBackTop-combineBothTop
     *      -- number of combining indexes computed for backward-only-combining characters
     *
     *  indexes[INDEX_MIN_NF*_NO_MAYBE] (where *={ C, D, KC, KD })
     *      -- first code point with a quick check NF* value of NO/MAYBE
     *
     *
     * - Tries
     *
     * The main structures are two Trie tables ("compact arrays"),
     * each with one index array and one data array.
     * See Trie.h and Trie.c.
     *
     *
     * - Tries in unorm.icu
     *
     * The first trie (normTrie above)
     * provides data for the NF* quick checks and normalization.
     * The second trie (fcdTrie above) provides data just for FCD checks.
     *
     *
     * - norm32 data words from the first trie
     *
     * The norm32Table contains one 32-bit word "norm32" per code point.
     * It contains the following bit fields:
     * 31..16   extra data index, EXTRA_SHIFT is used to shift this field down
     *          if this index is <EXTRA_INDEX_TOP then it is an index into
     *              extraData[] where variable-length normalization data for this
     *              code point is found
     *          if this index is <EXTRA_INDEX_TOP+EXTRA_SURROGATE_TOP
     *              then this is a norm32 for a leading surrogate, and the index
     *              value is used together with the following trailing surrogate
     *              code unit in the second trie access
     *          if this index is >=EXTRA_INDEX_TOP+EXTRA_SURROGATE_TOP
     *              then this is a norm32 for a "special" character,
     *              i.e., the character is a Hangul syllable or a Jamo
     *              see EXTRA_HANGUL etc.
     *          generally, instead of extracting this index from the norm32 and
     *              comparing it with the above constants,
     *              the normalization code compares the entire norm32 value
     *              with MIN_SPECIAL, SURROGATES_TOP, MIN_HANGUL etc.
     *
     * 15..8    combining class (cc) according to UnicodeData.txt
     *
     *  7..6    COMBINES_ANY flags, used in composition to see if a character
     *              combines with any following or preceding character(s)
     *              at all
     *     7    COMBINES_BACK
     *     6    COMBINES_FWD
     *
     *  5..0    quick check flags, set for "no" or "maybe", with separate flags for
     *              each normalization form
     *              the higher bits are "maybe" flags; for NF*D there are no such flags
     *              the lower bits are "no" flags for all forms, in the same order
     *              as the "maybe" flags,
     *              which is (MSB to LSB): NFKD NFD NFKC NFC
     *  5..4    QC_ANY_MAYBE
     *  3..0    QC_ANY_NO
     *              see further related constants
     *
     *
     * - Extra data per code point
     *
     * "Extra data" is referenced by the index in norm32.
     * It is variable-length data. It is only present, and only those parts
     * of it are, as needed for a given character.
     * The norm32 extra data index is added to the beginning of extraData[]
     * to get to a vector of 16-bit words with data at the following offsets:
     *
     * [-1]     Combining index for composition.
     *              Stored only if norm32&COMBINES_ANY .
     * [0]      Lengths of the canonical and compatibility decomposition strings.
     *              Stored only if there are decompositions, i.e.,
     *              if norm32&(QC_NFD|QC_NFKD)
     *          High byte: length of NFKD, or 0 if none
     *          Low byte: length of NFD, or 0 if none
     *          Each length byte also has another flag:
     *              Bit 7 of a length byte is set if there are non-zero
     *              combining classes (cc's) associated with the respective
     *              decomposition. If this flag is set, then the decomposition
     *              is preceded by a 16-bit word that contains the
     *              leading and trailing cc's.
     *              Bits 6..0 of a length byte are the length of the
     *              decomposition string, not counting the cc word.
     * [1..n]   NFD
     * [n+1..]  NFKD
     *
     * Each of the two decompositions consists of up to two parts:
     * - The 16-bit words with the leading and trailing cc's.
     *   This is only stored if bit 7 of the corresponding length byte
     *   is set. In this case, at least one of the cc's is not zero.
     *   High byte: leading cc==cc of the first code point in the decomposition string
     *   Low byte: trailing cc==cc of the last code point in the decomposition string
     * - The decomposition string in UTF-16, with length code units.
     *
     *
     * - Combining indexes and combiningTable[]
     *
     * Combining indexes are stored at the [-1] offset of the extra data
     * if the character combines forward or backward with any other characters.
     * They are used for (re)composition in NF*C.
     * Values of combining indexes are arranged according to whether a character
     * combines forward, backward, or both ways:
     *    forward-only < both ways < backward-only
     *
     * The index values for forward-only and both-ways combining characters
     * are indexes into the combiningTable[].
     * The index values for backward-only combining characters are simply
     * incremented from the preceding index values to be unique.
     *
     * In the combiningTable[], a variable-length list
     * of variable-length (back-index, code point) pair entries is stored
     * for each forward-combining character.
     *
     * These back-indexes are the combining indexes of both-ways or backward-only
     * combining characters that the forward-combining character combines with.
     *
     * Each list is sorted in ascending order of back-indexes.
     * Each list is terminated with the last back-index having bit 15 set.
     *
     * Each pair (back-index, code point) takes up either 2 or 3
     * 16-bit words.
     * The first word of a list entry is the back-index, with its bit 15 set if
     * this is the last pair in the list.
     *
     * The second word contains flags in bits 15..13 that determine
     * if there is a third word and how the combined character is encoded:
     * 15   set if there is a third word in this list entry
     * 14   set if the result is a supplementary character
     * 13   set if the result itself combines forward
     *
     * According to these bits 15..14 of the second word,
     * the result character is encoded as follows:
     * 00 or 01 The result is <=0x1fff and stored in bits 12..0 of
     *          the second word.
     * 10       The result is 0x2000..0xffff and stored in the third word.
     *          Bits 12..0 of the second word are not used.
     * 11       The result is a supplementary character.
     *          Bits 9..0 of the leading surrogate are in bits 9..0 of
     *          the second word.
     *          Add 0xd800 to these bits to get the complete surrogate.
     *          Bits 12..10 of the second word are not used.
     *          The trailing surrogate is stored in the third word.
     *
     *
     * - FCD trie
     *
     * The FCD trie is very simple.
     * It is a folded trie with 16-bit data words.
     * In each word, the high byte contains the leading cc of the character,
     * and the low byte contains the trailing cc of the character.
     * These cc's are the cc's of the first and last code points in the
     * canonical decomposition of the character.
     *
     * Since all 16 bits are used for cc's, lead surrogates must be tested
     * by checking the code unit instead of the trie data.
     * This is done only if the 16-bit data word is not zero.
     * If the code unit is a leading surrogate and the data word is not zero,
     * then instead of cc's it contains the offset for the second trie lookup.
     *
     *
     * - Auxiliary trie and data
     *
     *
     * The auxiliary 16-bit trie contains data for additional properties.
     * Bits
     * 15..13   reserved
     *     12   not NFC_Skippable (f) (formatVersion>=2.2)
     *     11   flag: not a safe starter for canonical closure
     *     10   composition exclusion
     *  9.. 0   index into extraData[] to FC_NFKC_Closure string
     *          (not for lead surrogate),
     *          or lead surrogate offset (for lead surrogate, if 9..0 not zero)
     * 
     * Conditions for "NF* Skippable" from Mark Davis' com.ibm.text.UCD.NFSkippable:
     * (used in NormalizerTransliterator)
     *
     * A skippable character is
     * a) unassigned, or ALL of the following:
     * b) of combining class 0.
     * c) not decomposed by this normalization form.
     * AND if NFC or NFKC,
     * d) can never compose with a previous character.
     * e) can never compose with a following character.
     * f) can never change if another character is added.
     *    Example: a-breve might satisfy all but f, but if you
     *    add an ogonek it changes to a-ogonek + breve
     *
     * a)..e) must be tested from norm32.
     * Since f) is more complicated, the (not-)NFC_Skippable flag (f) is built
     * into the auxiliary trie.
     * The same bit is used for NFC and NFKC; (c) differs for them.
     * As usual, we build the "not skippable" flags so that unassigned
     * code points get a 0 bit.
     * This bit is only valid after (a)..(e) test FALSE; test NFD_NO before (f) as well.
     * Test Hangul LV syllables entirely in code.
     *   
     * 
     * - FC_NFKC_Closure strings in extraData[]
     *
     * Strings are either stored as a single code unit or as the length
     * followed by that many units.
     * 
     * - structure inside canonStartSets[]
     *
     * This array maps from code points c to sets of code points (USerializedSet).
     * The result sets are the code points whose canonical decompositions start
     * with c.
     *
     * canonStartSets[] contains the following sub-arrays:
     *
     * indexes[_NORM_SET_INDEX_TOP]
     *   - contains lengths of sub-arrays etc.
     *
     * startSets[indexes[_NORM_SET_INDEX_CANON_SETS_LENGTH]-_NORM_SET_INDEX_TOP]
     *   - contains serialized sets (USerializedSet) of canonical starters for
     *     enumerating canonically equivalent strings
     *     indexes[_NORM_SET_INDEX_CANON_SETS_LENGTH] includes _NORM_SET_INDEX_TOP
     *     for details about the structure see uset.c
     *
     * bmpTable[indexes[_NORM_SET_INDEX_CANON_BMP_TABLE_LENGTH]]
     *   - a sorted search table for BMP code points whose results are
     *     either indexes to USerializedSets or single code points for
     *     single-code point sets;
     *     each entry is a pair of { code point, result } with result=(binary) yy xxxxxx xxxxxxxx
     *     if yy==01 then there is a USerializedSet at canonStartSets+x
     *     else build a USerializedSet with result as the single code point
     *
     * suppTable[indexes[_NORM_SET_INDEX_CANON_SUPP_TABLE_LENGTH]]
     *   - a sorted search table for supplementary code points whose results are
     *     either indexes to USerializedSets or single code points for
     *     single-code point sets;
     *     each entry is a triplet of { high16(cp), low16(cp), result }
     *     each code point's high-word may contain extra data in bits 15..5:
     *     if the high word has bit 15 set, then build a set with a single code point
     *     which is (((high16(cp)&0x1f00)<<8)|result;
     *     else there is a USerializedSet at canonStartSets+result
     */
final class NormalizerDataReader implements ICUBinary.Authenticate {
    private final static boolean debug = ICUDebug.enabled("NormalizerDataReader");
    
   /**
    * <p>Protected constructor.</p>
    * @param inputStream ICU uprop.dat file input stream
    * @exception IOException throw if data file fails authentication 
    * @draft 2.1
    */
    protected NormalizerDataReader(InputStream inputStream) 
                                        throws IOException{
        if(debug) System.out.println("Bytes in inputStream " + inputStream.available());
        
        unicodeVersion = ICUBinary.readHeader(inputStream, DATA_FORMAT_ID, this);
        
        if(debug) System.out.println("Bytes left in inputStream " +inputStream.available());
        
        dataInputStream = new DataInputStream(inputStream);
        
        if(debug) System.out.println("Bytes left in dataInputStream " +dataInputStream.available());
    }
    
    // protected methods -------------------------------------------------
    
    protected int[] readIndexes(int length)throws IOException{
        int[] indexes = new int[length];
        //Read the indexes
        for (int i = 0; i <length ; i++) {
             indexes[i] = dataInputStream.readInt();
        }
        return indexes;
    } 
    /**
    * <p>Reads unorm.icu, parse it into blocks of data to be stored in
    * NormalizerImpl.</P
    * @param normBytes
    * @param fcdBytes
    * @param auxBytes
    * @param extraData
    * @param combiningTable
    * @param canonStartSets
    * @exception thrown when data reading fails
    * @draft 2.1
    */
    protected void read(byte[] normBytes, byte[] fcdBytes, byte[] auxBytes,
                        char[] extraData, char[] combiningTable, 
                        Object[] canonStartSets) 
                        throws IOException{

         //Read the bytes that make up the normTrie     
         dataInputStream.read(normBytes);
        
         //normTrieStream= new ByteArrayInputStream(normBytes);

         //Read the extra data
         for(int i=0;i<extraData.length;i++){
             extraData[i]=dataInputStream.readChar();
         }
         
         //Read the combining class table
         for(int i=0; i<combiningTable.length; i++){
             combiningTable[i]=dataInputStream.readChar();
         }
         
         //Read the fcdTrie
         dataInputStream.read(fcdBytes);
         
         
         //Read the AuxTrie         
        dataInputStream.read(auxBytes);
        
        //Read the canonical start sets
        int[] canonStartSetsIndexes = new int[NormalizerImpl.SET_INDEX_TOP];
        
        for(int i=0; i<canonStartSetsIndexes.length; i++){
             canonStartSetsIndexes[i]=dataInputStream.readChar();
         }
        
        char[] startSets = new char[canonStartSetsIndexes[NormalizerImpl.SET_INDEX_CANON_SETS_LENGTH]-NormalizerImpl.SET_INDEX_TOP];
        
        for(int i=0; i<startSets.length; i++){
             startSets[i]=dataInputStream.readChar();
         }
         char[] bmpTable  = new char[canonStartSetsIndexes[NormalizerImpl.SET_INDEX_CANON_BMP_TABLE_LENGTH]];
        for(int i=0; i<bmpTable.length; i++){
             bmpTable[i]=dataInputStream.readChar();
         }        
        char[] suppTable = new char[canonStartSetsIndexes[NormalizerImpl.SET_INDEX_CANON_SUPP_TABLE_LENGTH]];
        for(int i=0; i<suppTable.length; i++){
             suppTable[i]=dataInputStream.readChar();
         }
         canonStartSets[NormalizerImpl.CANON_SET_INDICIES_INDEX  ] = canonStartSetsIndexes;
         canonStartSets[NormalizerImpl.CANON_SET_START_SETS_INDEX] = startSets;
         canonStartSets[NormalizerImpl.CANON_SET_BMP_TABLE_INDEX    ] = bmpTable;
         canonStartSets[NormalizerImpl.CANON_SET_SUPP_TABLE_INDEX] = suppTable;         
    }
    
    public byte[] getDataFormatVersion(){
        return DATA_FORMAT_VERSION;
    }
    
    public boolean isDataVersionAcceptable(byte version[])
    {
        return version[0] == DATA_FORMAT_VERSION[0] 
               && version[2] == DATA_FORMAT_VERSION[2] 
               && version[3] == DATA_FORMAT_VERSION[3];
    }
    
    public byte[] getUnicodeVersion(){
        return unicodeVersion;    
    }
    // private data members -------------------------------------------------
      

    /**
    * ICU data file input stream
    */
    private DataInputStream dataInputStream;
    
    private byte[] unicodeVersion;
                                       
    /**
    * File format version that this class understands.
    * No guarantees are made if a older version is used
    * see store.c of gennorm for more information and values
    */
    private static final byte DATA_FORMAT_ID[] = {(byte)0x4E, (byte)0x6F, 
                                                    (byte)0x72, (byte)0x6D};
    private static final byte DATA_FORMAT_VERSION[] = {(byte)0x2, (byte)0x2, 
                                                        (byte)0x5, (byte)0x2};
    
}
