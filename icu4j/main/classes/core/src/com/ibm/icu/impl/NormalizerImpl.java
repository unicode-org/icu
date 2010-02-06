 /*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
 
package com.ibm.icu.impl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.MissingResourceException;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;    
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.VersionInfo;
import com.ibm.icu.lang.UCharacter;

/**
 * @version     1.0
 * @author  Ram Viswanadha
 */
public final class NormalizerImpl {
    // Static block for the class to initialize its own self 
    static final NormalizerImpl IMPL;
    
    static
    {
        try
        {
            IMPL = new NormalizerImpl();
        }
        catch (Exception e)
        {
            throw new MissingResourceException(e.getMessage(), "", "");
        }
    }
    
    static final int UNSIGNED_BYTE_MASK =0xFF;
    static final long UNSIGNED_INT_MASK = 0xffffffffL;
    /*
     * This new implementation of the normalization code loads its data from
     * unorm.icu, which is generated with the gennorm tool.
     * The format of that file is described at the end of this file.
     */
    private static final String DATA_FILE_NAME = ICUResourceBundle.ICU_BUNDLE+"/unorm.icu";

    /* indexes[] value names */
    /* number of bytes in normalization trie */
    static final int INDEX_TRIE_SIZE           = 0;
     /* number of chars in extra data */     
    static final int INDEX_CHAR_COUNT           = 1;    
    /* number of uint16_t words for combining data */
    static final int INDEX_COMBINE_DATA_COUNT = 2;
    /* number of code points that combine forward */     
    static final int INDEX_COMBINE_FWD_COUNT  = 3;
    /* number of code points that combine forward and backward */     
    static final int INDEX_COMBINE_BOTH_COUNT = 4;
    /* number of code points that combine backward */     
    static final int INDEX_COMBINE_BACK_COUNT = 5;     
     /* first code point with quick check NFC NO/MAYBE */
    public static final int INDEX_MIN_NFC_NO_MAYBE   = 6;
    /* first code point with quick check NFKC NO/MAYBE */    
    public static final int INDEX_MIN_NFKC_NO_MAYBE  = 7;
     /* first code point with quick check NFD NO/MAYBE */     
    public static final int INDEX_MIN_NFD_NO_MAYBE   = 8;
    /* first code point with quick check NFKD NO/MAYBE */    
    public static final int INDEX_MIN_NFKD_NO_MAYBE  = 9;     
    /* number of bytes in FCD trie */
    static final int INDEX_FCD_TRIE_SIZE      = 10;
    /* number of bytes in the auxiliary trie */    
    static final int INDEX_AUX_TRIE_SIZE      = 11;
    /* number of uint16_t in the array of serialized USet */    
    static final int INDEX_CANON_SET_COUNT    = 12;    
    /* changing this requires a new formatVersion */
    static final int INDEX_TOP                = 32;    
    
    
    /* AUX constants */
    /* value constants for auxTrie */    
    private static final int AUX_UNSAFE_SHIFT           = 11;
    private static final int AUX_COMP_EX_SHIFT           = 10;
    
    private static final int AUX_MAX_FNC          =   1<<AUX_COMP_EX_SHIFT;
    private static final int AUX_UNSAFE_MASK      =   (int)((1<<AUX_UNSAFE_SHIFT) & UNSIGNED_INT_MASK);
    private static final int AUX_FNC_MASK         =   (int)((AUX_MAX_FNC-1) & UNSIGNED_INT_MASK);
    
    /* canonStartSets[0..31] contains indexes for what is in the array */
    /* number of uint16_t in canonical starter sets */
    static final int SET_INDEX_CANON_SETS_LENGTH        = 0;
    /* number of uint16_t in the BMP search table (contains pairs) */ 
    static final int SET_INDEX_CANON_BMP_TABLE_LENGTH    = 1;
    /* number of uint16_t in the supplementary search table(contains triplets)*/ 
    static final int SET_INDEX_CANON_SUPP_TABLE_LENGTH  = 2;
    /* changing this requires a new formatVersion */ 
    static final int SET_INDEX_TOP                        = 32;
    
    static final int CANON_SET_INDICIES_INDEX              = 0;
    static final int CANON_SET_START_SETS_INDEX            = 1;
    static final int CANON_SET_BMP_TABLE_INDEX            = 2;
    static final int CANON_SET_SUPP_TABLE_INDEX            = 3;
    /* 14 bit indexes to canonical USerializedSets */
    static final int CANON_SET_MAX_CANON_SETS             = 0x4000; 
    /* single-code point BMP sets are encoded directly in the search table 
     * except if result=0x4000..0x7fff 
     */
    static final int CANON_SET_BMP_MASK                    = 0xc000;
    static final int CANON_SET_BMP_IS_INDEX                = 0x4000;
    
    /**
     * Internal option for cmpEquivFold() for decomposing.
     * If not set, just do strcasecmp().
     * @internal
     */
     public static final int COMPARE_EQUIV = 0x80000;
    
    /*******************************/

    /* Wrappers for Trie implementations */ 
    static final class FCDTrieImpl implements Trie.DataManipulate{
        static CharTrie fcdTrie=null;
       /**
        * Called by com.ibm.icu.util.Trie to extract from a lead surrogate's 
        * data the index array offset of the indexes for that lead surrogate.
        * @param value data value for a surrogate from the trie, including
        *         the folding offset
        * @return data offset or 0 if there is no data for the lead surrogate
        */
        /* fcdTrie: the folding offset is the lead FCD value itself */
        public int getFoldingOffset(int value){
            return value;
        }
    }
    
    static final class AuxTrieImpl implements Trie.DataManipulate{
        static CharTrie auxTrie = null;
       /**
        * Called by com.ibm.icu.util.Trie to extract from a lead surrogate's 
        * data the index array offset of the indexes for that lead surrogate.
        * @param value data value for a surrogate from the trie, including 
        *        the folding offset
        * @return data offset or 0 if there is no data for the lead surrogate
        */
        /* auxTrie: the folding offset is in bits 9..0 of the 16-bit trie result */
        public int getFoldingOffset(int value) {
            return (value & AUX_FNC_MASK) << SURROGATE_BLOCK_BITS;
        }
    }
         
    /****************************************************/
    
    
    private static FCDTrieImpl fcdTrieImpl;
    private static AuxTrieImpl auxTrieImpl;
    private static int[] indexes;
    private static char[] combiningTable;
    private static char[] extraData;
    private static Object[] canonStartSets;
    
    private static boolean isDataLoaded;
    private static boolean isFormatVersion_2_1;
    private static byte[] unicodeVersion;
    
    /**
     * Default buffer size of datafile
     */
    private static final int DATA_BUFFER_SIZE = 25000;
    
    /**
     * FCD check: everything below this code point is known to have a 0 
     * lead combining class 
     */
    public static final int MIN_WITH_LEAD_CC=0x300;

    /** Number of bits of a trail surrogate that are used in index table 
     * lookups. 
     */
    private static final int SURROGATE_BLOCK_BITS=10-Trie.INDEX_STAGE_1_SHIFT_;


   // protected constructor ---------------------------------------------
    
    /**
    * Constructor
    * @exception thrown when data reading fails or data corrupted
    */
    private NormalizerImpl() throws IOException {
        //data should be loaded only once
        if(!isDataLoaded){
            
            // jar access
            InputStream i = ICUData.getRequiredStream(DATA_FILE_NAME);
            BufferedInputStream b = new BufferedInputStream(i,DATA_BUFFER_SIZE);
            NormalizerDataReader reader = new NormalizerDataReader(b);
            
            // read the indexes            
            indexes = reader.readIndexes(NormalizerImpl.INDEX_TOP);
            
            byte[] normBytes = new byte[indexes[NormalizerImpl.INDEX_TRIE_SIZE]];
            
            int combiningTableTop = indexes[NormalizerImpl.INDEX_COMBINE_DATA_COUNT];
            combiningTable = new char[combiningTableTop];
            
            int extraDataTop = indexes[NormalizerImpl.INDEX_CHAR_COUNT];
            extraData = new char[extraDataTop];

            byte[] fcdBytes = new byte[indexes[NormalizerImpl.INDEX_FCD_TRIE_SIZE]];
            byte[] auxBytes = new byte[indexes[NormalizerImpl.INDEX_AUX_TRIE_SIZE]];
            canonStartSets=new Object[NormalizerImpl.CANON_SET_MAX_CANON_SETS];
            
            fcdTrieImpl = new FCDTrieImpl();
            auxTrieImpl = new AuxTrieImpl();
                        
            // load the rest of the data data and initialize the data members
            reader.read(normBytes, fcdBytes,auxBytes, extraData, combiningTable, 
                        canonStartSets);
                                       
            FCDTrieImpl.fcdTrie   = new CharTrie( new ByteArrayInputStream(fcdBytes),fcdTrieImpl  );
            AuxTrieImpl.auxTrie   = new CharTrie( new ByteArrayInputStream(auxBytes),auxTrieImpl  );
            
            // we reached here without any exceptions so the data is fully 
            // loaded set the variable to true
            isDataLoaded = true;
            
            // get the data format version                           
            byte[] formatVersion = reader.getDataFormatVersion();
            
            isFormatVersion_2_1 =( formatVersion[0]>2 
                                    ||
                                   (formatVersion[0]==2 && formatVersion[1]>=1)
                                 );
            unicodeVersion = reader.getUnicodeVersion();
            b.close();
        }
    }
        
    /* ---------------------------------------------------------------------- */
    
    /* Korean Hangul and Jamo constants */
    
    public static final int JAMO_L_BASE=0x1100;     /* "lead" jamo */
    public static final int JAMO_V_BASE=0x1161;     /* "vowel" jamo */
    public static final int JAMO_T_BASE=0x11a7;     /* "trail" jamo */
    
    public static final int HANGUL_BASE=0xac00;
    
    public static final int JAMO_L_COUNT=19;
    public static final int JAMO_V_COUNT=21;
    public static final int JAMO_T_COUNT=28;
    public  static final int HANGUL_COUNT=JAMO_L_COUNT*JAMO_V_COUNT*JAMO_T_COUNT;
    
    /* data access primitives ----------------------------------------------- */
    
//    private static long getNorm32(int c,int mask){
//        long/*unsigned*/ norm32= getNorm32(UTF16.getLeadSurrogate(c));
//        if(((norm32&mask)>0) && isNorm32LeadSurrogate(norm32)) {
//            /* c is a lead surrogate, get the real norm32 */
//            norm32=getNorm32FromSurrogatePair(norm32,UTF16.getTrailSurrogate(c));
//        }
//        return norm32; 
//    }
    
    public static VersionInfo getUnicodeVersion(){
        return VersionInfo.getInstance(unicodeVersion[0], unicodeVersion[1],
                                       unicodeVersion[2], unicodeVersion[3]);
    }
    public static char    getFCD16(char c) {
        return  FCDTrieImpl.fcdTrie.getLeadValue(c);
    }
    
    public static char getFCD16FromSurrogatePair(char fcd16, char c2) {
        /* the surrogate index in fcd16 is an absolute offset over the 
         * start of stage 1 
         * */
        return FCDTrieImpl.fcdTrie.getTrailValue(fcd16, c2);
    }
    public static int getFCD16(int c) {
        return  FCDTrieImpl.fcdTrie.getCodePointValue(c);
    }

    public static boolean isCanonSafeStart(int c) {
        if(isFormatVersion_2_1) {
            int aux = AuxTrieImpl.auxTrie.getCodePointValue(c);
            return (aux & AUX_UNSAFE_MASK) == 0;
        } else {
            return false;
        }
    }
    
    public static boolean getCanonStartSet(int c, USerializedSet fillSet) {

        if(fillSet!=null && canonStartSets!=null) {
             /*
             * binary search for c
             *
             * There are two search tables,
             * one for BMP code points and one for supplementary ones.
             * See unormimp.h for details.
             */
            char[] table;
            int i=0, start, limit;
            
            int[] idxs = (int[]) canonStartSets[CANON_SET_INDICIES_INDEX];
            char[] startSets = (char[]) canonStartSets[CANON_SET_START_SETS_INDEX];
            
            if(c<=0xffff) {
                table=(char[]) canonStartSets[CANON_SET_BMP_TABLE_INDEX];
                start=0;
                limit=table.length;
    
                /* each entry is a pair { c, result } */
                while(start<limit-2) {
                    i=(char)(((start+limit)/4)*2); 
                    if(c<table[i]) {
                        limit=i;
                    } else {
                        start=i;
                    }
                }
                //System.out.println(i);
                /* found? */
                if(c==table[start]) {
                    i=table[start+1];
                    if((i & CANON_SET_BMP_MASK)==CANON_SET_BMP_IS_INDEX) {
                        /* result 01xxxxxx xxxxxx contains index x to a 
                         * USerializedSet */
                        i&=(CANON_SET_MAX_CANON_SETS-1);
                        return fillSet.getSet(startSets,(i-idxs.length));
                    } else {
                        /* other result values are BMP code points for 
                         * single-code point sets */
                        fillSet.setToOne(i);
                        return true;
                    }
                }
            } else {
                char high, low, h,j=0;
    
                table=(char[]) canonStartSets[CANON_SET_SUPP_TABLE_INDEX];
                start=0;
                limit=table.length;
    
                high=(char)(c>>16);
                low=(char)c;
    
                /* each entry is a triplet { high(c), low(c), result } */
                while(start<limit-3) {
                    /* (start+limit)/2 and address triplets */
                    i=(char)(((start+limit)/6)*3);
                    j=(char)(table[i]&0x1f); /* high word */
                    int tableVal = table[i+1];
                    int lowInt = low;
                    if(high<j || ((tableVal>lowInt) && (high==j))) {
                        limit=i;
                    } else {
                        start=i;
                    }
                    
                    //System.err.println("\t((high==j) && (table[i+1]>low)) == " + ((high==j) && (tableVal>lowInt)) );
                    
                    // KLUDGE: IBM JIT in 1.4.0 is sooo broken
                    // The below lines make TestExhaustive pass
                    if(ICUDebug.enabled()){
                        System.err.println("\t\t j = " + Utility.hex(j,4) +
                                           "\t i = " + Utility.hex(i,4) +
                                           "\t high = "+ Utility.hex(high)  +
                                           "\t low = "  + Utility.hex(lowInt,4)   +
                                           "\t table[i+1]: "+ Utility.hex(tableVal,4) 
                                           );
                    }
                   
                }

                /* found? */
                h=table[start];

                //System.err.println("c: \\U"+ Integer.toHexString(c)+" i : "+Integer.toHexString(i) +" h : " + Integer.toHexString(h));
                int tableVal1 = table[start+1];
                int lowInt = low;

                if(high==(h&0x1f) && lowInt==tableVal1) {
                    int tableVal2 = table[start+2];
                    i=tableVal2;
                    if((h&0x8000)==0) {
                        /* the result is an index to a USerializedSet */
                        return fillSet.getSet(startSets,(i-idxs.length));
                    } else {
                        /*
                         * single-code point set {x} in
                         * triplet { 100xxxxx 000hhhhh  llllllll llllllll  xxxxxxxx xxxxxxxx }
                         */
                        //i|=((int)h & 0x1f00)<<8; /* add high bits from high(c) */
                        int temp = ((int)h & 0x1f00)<<8;
                        i|=temp; /* add high bits from high(c) */
                        fillSet.setToOne(i);
                        return true;
                    }
                }
            }
        }
    
        return false; /* not found */
    }
    
    public static int getFC_NFKC_Closure(int c, char[] dest) {
        
        int destCapacity;
         
        if(dest==null ) {
            destCapacity=0;
        }else{
            destCapacity = dest.length;
        }
        
        int aux =AuxTrieImpl.auxTrie.getCodePointValue(c);

        aux&= AUX_FNC_MASK;
        if(aux!=0) {
            int s;
            int index=aux; 
            int length;
            
            s =extraData[index];
            if(s<0xff00) {
                /* s points to the single-unit string */
                length=1;
            } else {
                length=s&0xff;
                ++index;
            }
            if(0<length && length<=destCapacity) {
                System.arraycopy(extraData,index,dest,0,length);
            }
            return length;
        } else {
            return 0;
        }
    }

    public static UnicodeSet addPropertyStarts(UnicodeSet set) {
        int c;
       
        /* add the start code point of each same-value range of each trie */
        //utrie_enum(&fcdTrie, NULL, _enumPropertyStartsRange, set);
        TrieIterator fcdIter  = new TrieIterator(FCDTrieImpl.fcdTrie);
        RangeValueIterator.Element fcdResult = new RangeValueIterator.Element();

        while(fcdIter.next(fcdResult)){
            set.add(fcdResult.start);
        }
        
        if(isFormatVersion_2_1){
            //utrie_enum(&auxTrie, NULL, _enumPropertyStartsRange, set);
            TrieIterator auxIter  = new TrieIterator(AuxTrieImpl.auxTrie);
            RangeValueIterator.Element auxResult = new RangeValueIterator.Element();
            while(auxIter.next(auxResult)){
                set.add(auxResult.start);
            }
        }
        /* add Hangul LV syllables and LV+1 because of skippables */
        for(c=HANGUL_BASE; c<HANGUL_BASE+HANGUL_COUNT; c+=JAMO_T_COUNT) {
            set.add(c);
            set.add(c+1);
        }
        set.add(HANGUL_BASE+HANGUL_COUNT); /* add Hangul+1 to continue with other properties */
        return set; // for chaining
    }

   /* compare canonically equivalent ---------------------------------------- */

    /*
     * Compare two strings for canonical equivalence.
     * Further options include case-insensitive comparison and
     * code point order (as opposed to code unit order).
     *
     * In this function, canonical equivalence is optional as well.
     * If canonical equivalence is tested, then both strings must fulfill
     * the FCD check.
     *
     * Semantically, this is equivalent to
     *   strcmp[CodePointOrder](foldCase(NFD(s1)), foldCase(NFD(s2)))
     * where code point order, NFD and foldCase are all optional.
     *
     * String comparisons almost always yield results before processing both 
     * strings completely.
     * They are generally more efficient working incrementally instead of
     * performing the sub-processing (strlen, normalization, case-folding)
     * on the entire strings first.
     *
     * It is also unnecessary to not normalize identical characters.
     *
     * This function works in principle as follows:
     *
     * loop {
     *   get one code unit c1 from s1 (-1 if end of source)
     *   get one code unit c2 from s2 (-1 if end of source)
     *
     *   if(either string finished) {
     *     return result;
     *   }
     *   if(c1==c2) {
     *     continue;
     *   }
     *
     *   // c1!=c2
     *   try to decompose/case-fold c1/c2, and continue if one does;
     *
     *   // still c1!=c2 and neither decomposes/case-folds, return result
     *   return c1-c2;
     * }
     *
     * When a character decomposes, then the pointer for that source changes to
     * the decomposition, pushing the previous pointer onto a stack.
     * When the end of the decomposition is reached, then the code unit reader
     * pops the previous source from the stack.
     * (Same for case-folding.)
     *
     * This is complicated further by operating on variable-width UTF-16.
     * The top part of the loop works on code units, while lookups for decomposition
     * and case-folding need code points.
     * Code points are assembled after the equality/end-of-source part.
     * The source pointer is only advanced beyond all code units when the code point
     * actually decomposes/case-folds.
     *
     * If we were on a trail surrogate unit when assembling a code point,
     * and the code point decomposes/case-folds, then the decomposition/folding
     * result must be compared with the part of the other string that corresponds to
     * this string's lead surrogate.
     * Since we only assemble a code point when hitting a trail unit when the
     * preceding lead units were identical, we back up the other string by one unit
     * in such a case.
     *
     * The optional code point order comparison at the end works with
     * the same fix-up as the other code point order comparison functions.
     * See ustring.c and the comment near the end of this function.
     *
     * Assumption: A decomposition or case-folding result string never contains
     * a single surrogate. This is a safe assumption in the Unicode Standard.
     * Therefore, we do not need to check for surrogate pairs across
     * decomposition/case-folding boundaries.
     * Further assumptions (see verifications tstnorm.cpp):
     * The API function checks for FCD first, while the core function
     * first case-folds and then decomposes. This requires that case-folding does not
     * un-FCD any strings.
     *
     * The API function may also NFD the input and turn off decomposition.
     * This requires that case-folding does not un-NFD strings either.
     *
     * TODO If any of the above two assumptions is violated,
     * then this entire code must be re-thought.
     * If this happens, then a simple solution is to case-fold both strings up front
     * and to turn off UNORM_INPUT_IS_FCD.
     * We already do this when not both strings are in FCD because makeFCD
     * would be a partial NFD before the case folding, which does not work.
     * Note that all of this is only a problem when case-folding _and_
     * canonical equivalence come together.
     * 
     * This function could be moved to a different source file, at increased cost
     * for calling the decomposition access function.
     */
    
    // stack element for previous-level source/decomposition pointers
    private static class CmpEquivLevel {
        char[] source;
        int start;
        int s;
        int limit;
    }
    
    /**
     * Get the canonical decomposition for one code point.
     * @param c code point
     * @param buffer out-only buffer for algorithmic decompositions of Hangul
     * @param length out-only, takes the length of the decomposition, if any
     * @return index into the extraData array, or 0 if none
     * @internal
     */
     private static int decompose(int c, char[] buffer) {
        // TODO: this is only temporary; change norm-compare to use new code
        String decomp=Norm2AllModes.getNFCInstanceNoIOException().impl.getDecomposition(c);
        if(decomp!=null) {
            decomp.getChars(0, decomp.length(), buffer, 0);
            return decomp.length();
        } else {
            return 0;
        }
    }

    private static int foldCase(int c, char[] dest, int destStart, int destLimit,
                                 int options){
        String src = UTF16.valueOf(c);
        String foldedStr = UCharacter.foldCase(src,options);
        char[] foldedC = foldedStr.toCharArray();
        for(int i=0;i<foldedC.length;i++){
            if(destStart<destLimit){
                dest[destStart]=foldedC[i];
            }
            // always increment destStart so that we can return 
            // the required length
            destStart++;
        }
        return (c==UTF16.charAt(foldedStr,0)) ? -destStart : destStart;
    }
    
    /*
     private static int foldCase(char[] src,int srcStart,int srcLimit,
                                char[] dest, int destStart, int destLimit,
                                int options){
        String source =new String(src,srcStart,(srcLimit-srcStart));
        String foldedStr = UCharacter.foldCase(source,options);
        char[] foldedC = foldedStr.toCharArray();
        for(int i=0;i<foldedC.length;i++){
            if(destStart<destLimit){
                dest[destStart]=foldedC[i];
            }
            // always increment destStart so that we can return 
            // the required length
            destStart++;
            
        }
        return destStart;
    }
    */
    public static int cmpEquivFold(String s1, String s2,int options){
        if ((options & Normalizer.COMPARE_IGNORE_CASE) !=0) {
            return cmpSimpleEquivFold(s1, s2, options);
        }
        else {
            return cmpEquivFold(s1.toCharArray(),0,s1.length(),
                                s2.toCharArray(),0,s2.length(),
                                options);
        }
    }


    private static int cmpSimpleEquivFold(String s1, String s2, int options) {
        int cmp = 0;
        int i=0, j=0;
        String foldS1=null;
        String foldS2=null;
        int offset1=1;
        int offset2=1;
        while ((i+offset1<=s1.length() && j+offset2<=s2.length())) {
            if ((cmp!=0) || (s1.charAt(i) != s2.charAt(j))) {
                if(i>0 && j>0 && 
                   (UTF16.isLeadSurrogate(s1.charAt(i-1)) ||
                    UTF16.isLeadSurrogate(s2.charAt(j-1)))) {
                    // Current codepoint may be the low surrogate pair.
                    return cmpEquivFold(s1.toCharArray(),i-1,s1.length(),
                            s2.toCharArray(),j-1,s2.length(),
                            options);
                }
                else if (UTF16.isLeadSurrogate(s1.charAt(i))||
                         UTF16.isLeadSurrogate(s2.charAt(j))) {
                    return cmpEquivFold(s1.toCharArray(),i,s1.length(),
                            s2.toCharArray(),j,s2.length(),
                            options);
                }
                else {
                    if ( offset1 > 0 ) {
                        foldS1 = UCharacter.foldCase(s1.substring(i, i+offset1),options);
                    }
                    if ( offset2 > 0 ) {
                        foldS2 = UCharacter.foldCase(s2.substring(j, j+offset2),options);
                    }
                    cmp = foldS1.compareTo(foldS2);
                    if (cmp==0) {
                        i = moveToNext(i, offset1);
                        j = moveToNext(j, offset2);
                        offset1 = offset2 = 1;
                        continue;
                    }
                }
                if (foldS1.length()==foldS2.length()) {
                    return cmp;
                }
                if (foldS1.length()<foldS2.length()) {
                    offset1++;
                    offset2=0;
                }
                else {
                    offset1=0;
                    offset2++;
                }
                continue;
            }
            i++;
            j++;
        }
        if (cmp!=0) {
            return cmp;
        }
        if (i+offset1-1==s1.length()) {
            if (j+offset2-1==s2.length()) {
                return 0;
            }
            else {
                return -1;
            }
        }
        else {
            return 1;
        }
    }
    
    private static int moveToNext(int pos, int offset) {
        if (offset>0) {
            return pos+offset;
        }
        else {
            return pos+1;
        }
    }
    
    
    // internal function
    public static int cmpEquivFold(char[] s1, int s1Start,int s1Limit,
                                   char[] s2, int s2Start,int s2Limit,
                                   int options) {
        // current-level start/limit - s1/s2 as current
        int start1, start2, limit1, limit2;
        char[] cSource1, cSource2;
        
        cSource1 = s1;
        cSource2 = s2;
        // decomposition variables
        int length;
    
        // stacks of previous-level start/current/limit
        CmpEquivLevel[] stack1 = new CmpEquivLevel[]{ 
                                                    new CmpEquivLevel(),
                                                    new CmpEquivLevel()
                                                  };
        CmpEquivLevel[] stack2 = new CmpEquivLevel[]{ 
                                                    new CmpEquivLevel(),
                                                    new CmpEquivLevel()
                                                  };
    
        // decomposition buffers for Hangul
        char[] decomp1 = new char[8];
        char[] decomp2 = new char[8];
    
        // case folding buffers, only use current-level start/limit
        char[] fold1 = new char[32];
        char[] fold2 = new char[32];
    
        // track which is the current level per string
        int level1, level2;
    
        // current code units, and code points for lookups
        int c1, c2;
        int cp1, cp2;
    
        // no argument error checking because this itself is not an API
    
        // assume that at least one of the options COMPARE_EQUIV and 
        // COMPARE_IGNORE_CASE is set
        // otherwise this function must behave exactly as uprv_strCompare()
        // not checking for that here makes testing this function easier

    
        // initialize
        start1=s1Start;
        limit1=s1Limit;
        
        start2=s2Start;
        limit2=s2Limit;
        
        level1=level2=0;
        c1=c2=-1;
        cp1=cp2=-1;
        // comparison loop
        for(;;) {
            // here a code unit value of -1 means "get another code unit"
            // below it will mean "this source is finished"
    
            if(c1<0) {
                // get next code unit from string 1, post-increment
                for(;;) {
                    if(s1Start>=limit1) {
                        if(level1==0) {
                            c1=-1;
                            break;
                        }
                    } else {
                        c1=cSource1[s1Start];
                        ++s1Start;
                        break;
                    }
    
                    // reached end of level buffer, pop one level
                    do {
                        --level1;
                        start1=stack1[level1].start;
                    } while(start1==-1); //###### check this
                    s1Start=stack1[level1].s;
                    limit1=stack1[level1].limit;
                    cSource1=stack1[level1].source;
                }
            }
    
            if(c2<0) {
                // get next code unit from string 2, post-increment
                for(;;) {                    
                    if(s2Start>=limit2) {
                        if(level2==0) {
                            c2=-1;
                            break;
                        }
                    } else {
                        c2=cSource2[s2Start];
                        ++s2Start;
                        break;
                    }
    
                    // reached end of level buffer, pop one level
                    do {
                        --level2;
                        start2=stack2[level2].start;
                    } while(start2==-1);
                    s2Start=stack2[level2].s;
                    limit2=stack2[level2].limit;
                    cSource2=stack2[level2].source;
                }
            }
    
            // compare c1 and c2
            // either variable c1, c2 is -1 only if the corresponding string 
            // is finished
            if(c1==c2) {
                if(c1<0) {
                    return 0;   // c1==c2==-1 indicating end of strings
                }
                c1=c2=-1;       // make us fetch new code units
                continue;
            } else if(c1<0) {
                return -1;      // string 1 ends before string 2
            } else if(c2<0) {
                return 1;       // string 2 ends before string 1
            }
            // c1!=c2 && c1>=0 && c2>=0
    
            // get complete code points for c1, c2 for lookups if either is a 
            // surrogate
            cp1=c1;
            if(UTF16.isSurrogate((char)c1)) { 
                char c;
    
                if(UTF16.isLeadSurrogate((char)c1)) {
                    if(    s1Start!=limit1 && 
                           UTF16.isTrailSurrogate(c=cSource1[s1Start])
                      ) {
                        // advance ++s1; only below if cp1 decomposes/case-folds
                        cp1=UCharacterProperty.getRawSupplementary((char)c1, c);
                    }
                } else /* isTrail(c1) */ {
                    if(    start1<=(s1Start-2) && 
                            UTF16.isLeadSurrogate(c=cSource1[(s1Start-2)])
                      ) {
                        cp1=UCharacterProperty.getRawSupplementary(c, (char)c1);
                    }
                }
            }
            cp2=c2;
            if(UTF16.isSurrogate((char)c2)) {
                char c;
    
                if(UTF16.isLeadSurrogate((char)c2)) {
                    if(    s2Start!=limit2 && 
                           UTF16.isTrailSurrogate(c=cSource2[s2Start])
                      ) {
                        // advance ++s2; only below if cp2 decomposes/case-folds
                        cp2=UCharacterProperty.getRawSupplementary((char)c2, c);
                    }
                } else /* isTrail(c2) */ {
                    if(    start2<=(s2Start-2) &&  
                           UTF16.isLeadSurrogate(c=cSource2[s2Start-2])
                      ) {
                        cp2=UCharacterProperty.getRawSupplementary(c, (char)c2);
                    }
                }
            }
    
            // go down one level for each string
            // continue with the main loop as soon as there is a real change
            if( level1<2 && ((options & Normalizer.COMPARE_IGNORE_CASE)!=0)&&
                (length=foldCase(cp1, fold1, 0,32,options))>=0
            ) {
                // cp1 case-folds to fold1[length]
                if(UTF16.isSurrogate((char)c1)) {
                    if(UTF16.isLeadSurrogate((char)c1)) {
                        // advance beyond source surrogate pair if it 
                        // case-folds
                        ++s1Start;
                    } else /* isTrail(c1) */ {
                        // we got a supplementary code point when hitting its 
                        // trail surrogate, therefore the lead surrogate must 
                        // have been the same as in the other string;
                        // compare this decomposition with the lead surrogate
                        // in the other string
                        --s2Start;
                        c2=cSource2[(s2Start-1)];
                    }
                }
    
                // push current level pointers
                stack1[0].start=start1;
                stack1[0].s=s1Start;
                stack1[0].limit=limit1;
                stack1[0].source=cSource1;
                ++level1;
    
                cSource1 = fold1;
                start1=s1Start=0;
                limit1=length;
    
                // get ready to read from decomposition, continue with loop
                c1=-1;
                continue;
            }
    
            if( level2<2 && ((options& Normalizer.COMPARE_IGNORE_CASE)!=0) &&
                (length=foldCase(cp2, fold2,0,32, options))>=0
            ) {
                // cp2 case-folds to fold2[length]
                if(UTF16.isSurrogate((char)c2)) {
                    if(UTF16.isLeadSurrogate((char)c2)) {
                        // advance beyond source surrogate pair if it 
                        // case-folds
                        ++s2Start;
                    } else /* isTrail(c2) */ {
                        // we got a supplementary code point when hitting its 
                        // trail surrogate, therefore the lead surrogate must 
                        // have been the same as in the other string;
                        // compare this decomposition with the lead surrogate 
                        // in the other string
                        --s1Start;
                        c1=cSource1[(s1Start-1)];
                    }
                }
    
                // push current level pointers
                stack2[0].start=start2;
                stack2[0].s=s2Start;
                stack2[0].limit=limit2;
                stack2[0].source=cSource2;
                ++level2;
                
                cSource2 = fold2;
                start2=s2Start=0;
                limit2=length;
    
                // get ready to read from decomposition, continue with loop
                c2=-1;
                continue;
            }
            
            if( level1<2 && ((options&COMPARE_EQUIV)!=0) &&
                0!=(length=decompose(cp1,decomp1))
            ) {
                // cp1 decomposes into p[length]
                if(UTF16.isSurrogate((char)c1)) {
                    if(UTF16.isLeadSurrogate((char)c1)) {
                        // advance beyond source surrogate pair if it 
                        //decomposes
                        ++s1Start;
                    } else /* isTrail(c1) */ {
                        // we got a supplementary code point when hitting 
                        // its trail surrogate, therefore the lead surrogate 
                        // must have been the same as in the other string;
                        // compare this decomposition with the lead surrogate 
                        // in the other string
                        --s2Start;
                        c2=cSource2[(s2Start-1)];
                    }
                }
    
                // push current level pointers
                stack1[level1].start=start1;
                stack1[level1].s=s1Start;
                stack1[level1].limit=limit1;
                stack1[level1].source=cSource1;
                ++level1;
    
                // set next level pointers to decomposition
                cSource1 = decomp1;
                start1=s1Start=0;
                limit1=length;
                
                // set empty intermediate level if skipped
                if(level1<2) {
                    stack1[level1++].start=-1;
                }
                // get ready to read from decomposition, continue with loop
                c1=-1;
                continue;
            }
    
            if( level2<2 && ((options&COMPARE_EQUIV)!=0) &&
                0!=(length=decompose(cp2, decomp2))
            ) {
                // cp2 decomposes into p[length]
                if(UTF16.isSurrogate((char)c2)) {
                    if(UTF16.isLeadSurrogate((char)c2)) {
                        // advance beyond source surrogate pair if it 
                        // decomposes
                        ++s2Start;
                    } else /* isTrail(c2) */ {
                        // we got a supplementary code point when hitting its 
                        // trail surrogate, therefore the lead surrogate must 
                        // have been the same as in the other string;
                        // compare this decomposition with the lead surrogate 
                        // in the other string
                        --s1Start;
                        c1=cSource1[(s1Start-1)];
                    }
                }
    
                // push current level pointers
                stack2[level2].start=start2;
                stack2[level2].s=s2Start;
                stack2[level2].limit=limit2;
                stack2[level2].source=cSource2;
                ++level2;
    
                // set next level pointers to decomposition
                cSource2=decomp2;
                start2=s2Start=0;
                limit2=length;
                
                // set empty intermediate level if skipped
                if(level2<2) {
                    stack2[level2++].start=-1;
                }
                
                // get ready to read from decomposition, continue with loop
                c2=-1;
                continue;
            }
    
    
            // no decomposition/case folding, max level for both sides:
            // return difference result
    
            // code point order comparison must not just return cp1-cp2
            // because when single surrogates are present then the surrogate 
            // pairs that formed cp1 and cp2 may be from different string 
            // indexes
    
            // example: { d800 d800 dc01 } vs. { d800 dc00 }, compare at 
            // second code units
            // c1=d800 cp1=10001 c2=dc00 cp2=10000
            // cp1-cp2>0 but c1-c2<0 and in fact in UTF-32 
            // it is { d800 10001 } < { 10000 }
            // therefore fix-up 
    
            if(     c1>=0xd800 && c2>=0xd800 && 
                    ((options&Normalizer.COMPARE_CODE_POINT_ORDER)!=0)
              ) {
                /* subtract 0x2800 from BMP code points to make them smaller 
                 * than supplementary ones */
                if(
                    (    c1<=0xdbff && s1Start!=limit1 
                         && 
                         UTF16.isTrailSurrogate(cSource1[s1Start])
                    ) 
                     ||
                    (    UTF16.isTrailSurrogate((char)c1) && start1!=(s1Start-1) 
                         && 
                         UTF16.isLeadSurrogate(cSource1[(s1Start-2)])
                    )
                ) {
                    /* part of a surrogate pair, leave >=d800 */
                } else {
                    /* BMP code point - may be surrogate code point - 
                     * make <d800 */
                    c1-=0x2800;
                }
    
                if(
                    (    c2<=0xdbff && s2Start!=limit2 
                         && 
                         UTF16.isTrailSurrogate(cSource2[s2Start])
                    ) 
                     ||
                    (    UTF16.isTrailSurrogate((char)c2) && start2!=(s2Start-1) 
                         && 
                         UTF16.isLeadSurrogate(cSource2[(s2Start-2)])
                    )
                ) {
                    /* part of a surrogate pair, leave >=d800 */
                } else {
                    /* BMP code point - may be surrogate code point - 
                     * make <d800 */
                    c2-=0x2800;
                }
            }
    
            return c1-c2;
        }
    }
}
