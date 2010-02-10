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

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.RangeValueIterator;
import com.ibm.icu.util.VersionInfo;

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
            
            auxTrieImpl = new AuxTrieImpl();
                        
            // load the rest of the data data and initialize the data members
            reader.read(normBytes, fcdBytes,auxBytes, extraData, combiningTable, 
                        canonStartSets);
                                       
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
    
    public static VersionInfo getUnicodeVersion(){
        return VersionInfo.getInstance(unicodeVersion[0], unicodeVersion[1],
                                       unicodeVersion[2], unicodeVersion[3]);
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
}
