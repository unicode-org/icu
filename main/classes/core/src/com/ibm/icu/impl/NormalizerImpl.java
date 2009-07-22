 /*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
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
import com.ibm.icu.text.UnicodeSetIterator;
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
    
    // norm32 value constants 
    
    // quick check flags 0..3 set mean "no" for their forms 
    public static final int QC_NFC=0x11;          /* no|maybe */
    public static final int QC_NFKC=0x22;         /* no|maybe */
    public static final int QC_NFD=4;             /* no */
    public static final int QC_NFKD=8;            /* no */
    
    public static final int QC_ANY_NO=0xf;

    /* quick check flags 4..5 mean "maybe" for their forms; 
     * test flags>=QC_MAYBE 
     */
    public static final int QC_MAYBE=0x10;
    public static final int QC_ANY_MAYBE=0x30;

    public static final int QC_MASK=0x3f;

    private static final int COMBINES_FWD=0x40;
    private static final int COMBINES_BACK=0x80;
    public  static final int COMBINES_ANY=0xc0;
    // UnicodeData.txt combining class in bits 15.
    private static final int CC_SHIFT=8;                     
    public  static final int CC_MASK=0xff00;
    // 16 bits for the index to UChars and other extra data
    private static final int EXTRA_SHIFT=16;
    // start of surrogate specials after shift                
    //private static final int EXTRA_INDEX_TOP=0xfc00;       

    //private static final int EXTRA_SURROGATE_MASK=0x3ff;
    //private static final int EXTRA_SURROGATE_TOP=0x3f0;    /* hangul etc. */

    //private static final int EXTRA_HANGUL=EXTRA_SURROGATE_TOP;
    //private static final int EXTRA_JAMO_L=EXTRA_SURROGATE_TOP+1;/* ### not used */
    //private static final int EXTRA_JAMO_V=EXTRA_SURROGATE_TOP+2;
    //private static final int EXTRA_JAMO_T=EXTRA_SURROGATE_TOP+3;
    
    /* norm32 value constants using >16 bits */
    private static final long  MIN_SPECIAL    =  (long)(0xfc000000 & UNSIGNED_INT_MASK);
    private static final long  SURROGATES_TOP =  (long)(0xfff00000 & UNSIGNED_INT_MASK);
    private static final long  MIN_HANGUL     =  (long)(0xfff00000 & UNSIGNED_INT_MASK);
    //private static final long  MIN_JAMO_V     =  (long)(0xfff20000 & UNSIGNED_INT_MASK);
    private static final long  JAMO_V_TOP     =  (long)(0xfff30000 & UNSIGNED_INT_MASK);
    
    
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
    private static final int AUX_NFC_SKIPPABLE_F_SHIFT = 12;
    
    private static final int AUX_MAX_FNC          =   ((int)1<<AUX_COMP_EX_SHIFT);
    private static final int AUX_UNSAFE_MASK      =   (int)((1<<AUX_UNSAFE_SHIFT) & UNSIGNED_INT_MASK);
    private static final int AUX_FNC_MASK         =   (int)((AUX_MAX_FNC-1) & UNSIGNED_INT_MASK);
    private static final int AUX_COMP_EX_MASK     =   (int)((1<<AUX_COMP_EX_SHIFT) & UNSIGNED_INT_MASK);
    private static final long AUX_NFC_SKIP_F_MASK =   ((UNSIGNED_INT_MASK&1)<<AUX_NFC_SKIPPABLE_F_SHIFT);
    
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
    
    private static final int MAX_BUFFER_SIZE                    = 20;
    
    /**
     * Internal option for cmpEquivFold() for decomposing.
     * If not set, just do strcasecmp().
     * @internal
     */
     public static final int COMPARE_EQUIV = 0x80000;
    
    /*******************************/

    /* Wrappers for Trie implementations */ 
    static final class NormTrieImpl implements Trie.DataManipulate{
        static IntTrie normTrie= null;
       /**
        * Called by com.ibm.icu.util.Trie to extract from a lead surrogate's 
        * data the index array offset of the indexes for that lead surrogate.
        * @param property data value for a surrogate from the trie, including 
        *         the folding offset
        * @return data offset or 0 if there is no data for the lead surrogate
        */
        /* normTrie: 32-bit trie result may contain a special extraData index with the folding offset */
        public int getFoldingOffset(int value){
            return  BMP_INDEX_LENGTH+
                    ((value>>(EXTRA_SHIFT-SURROGATE_BLOCK_BITS))&
                    (0x3ff<<SURROGATE_BLOCK_BITS)); 
        }
        
    }
    static final class FCDTrieImpl implements Trie.DataManipulate{
        static CharTrie fcdTrie=null;
       /**
        * Called by com.ibm.icu.util.Trie to extract from a lead surrogate's 
        * data the index array offset of the indexes for that lead surrogate.
        * @param property data value for a surrogate from the trie, including
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
        * @param property data value for a surrogate from the trie, including 
        *        the folding offset
        * @return data offset or 0 if there is no data for the lead surrogate
        */
        /* auxTrie: the folding offset is in bits 9..0 of the 16-bit trie result */
        public int getFoldingOffset(int value){
            return (int)(value &AUX_FNC_MASK)<<SURROGATE_BLOCK_BITS;
        }
    }
         
    /****************************************************/
    
    
    private static FCDTrieImpl fcdTrieImpl;
    private static NormTrieImpl normTrieImpl;
    private static AuxTrieImpl auxTrieImpl;
    private static int[] indexes;
    private static char[] combiningTable;
    private static char[] extraData;
    private static Object[] canonStartSets;
    
    private static boolean isDataLoaded;
    private static boolean isFormatVersion_2_1;
    private static boolean isFormatVersion_2_2;
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


    /**
     * Bit 7 of the length byte for a decomposition string in extra data is
     * a flag indicating whether the decomposition string is
     * preceded by a 16-bit word with the leading and trailing cc
     * of the decomposition (like for A-umlaut);
     * if not, then both cc's are zero (like for compatibility ideographs).
     */
    private static final int DECOMP_FLAG_LENGTH_HAS_CC=0x80;
    /**
     * Bits 6..0 of the length byte contain the actual length.
     */
    private static final int DECOMP_LENGTH_MASK=0x7f;   
    
    /** Length of the BMP portion of the index (stage 1) array. */
    private static final int BMP_INDEX_LENGTH=0x10000>>Trie.INDEX_STAGE_1_SHIFT_;
    /** Number of bits of a trail surrogate that are used in index table 
     * lookups. 
     */
    private static final int SURROGATE_BLOCK_BITS=10-Trie.INDEX_STAGE_1_SHIFT_;


   // public utility
   public static int getFromIndexesArr(int index){
        return indexes[index];
   }
   
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
            normTrieImpl = new NormTrieImpl();
            auxTrieImpl = new AuxTrieImpl();
                        
            // load the rest of the data data and initialize the data members
            reader.read(normBytes, fcdBytes,auxBytes, extraData, combiningTable, 
                        canonStartSets);
                                       
            NormTrieImpl.normTrie = new IntTrie( new ByteArrayInputStream(normBytes),normTrieImpl );
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
            isFormatVersion_2_2 =( formatVersion[0]>2 
                                    ||
                                   (formatVersion[0]==2 && formatVersion[1]>=2)
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
    
    private static boolean isHangulWithoutJamoT(char c) {
        c-=HANGUL_BASE;
        return c<HANGUL_COUNT && c%JAMO_T_COUNT==0;
    }
    
    /* norm32 helpers */
    
    /* is this a norm32 with a regular index? */
    private static boolean isNorm32Regular(long norm32) {
        return norm32<MIN_SPECIAL;
    }
    
    /* is this a norm32 with a special index for a lead surrogate? */
    private static boolean isNorm32LeadSurrogate(long norm32) {
        return MIN_SPECIAL<=norm32 && norm32<SURROGATES_TOP;
    }
    
    /* is this a norm32 with a special index for a Hangul syllable or a Jamo? */
    private static boolean isNorm32HangulOrJamo(long norm32) {
        return norm32>=MIN_HANGUL;
    }
    
    /*
     * Given isNorm32HangulOrJamo(),
     * is this a Hangul syllable or a Jamo?
     */
//    private static  boolean isHangulJamoNorm32HangulOrJamoL(long norm32) {
//        return norm32<MIN_JAMO_V;
//    }
    
    /*
     * Given norm32 for Jamo V or T,
     * is this a Jamo V?
     */
    private static boolean isJamoVTNorm32JamoV(long norm32) {
        return norm32<JAMO_V_TOP;
    }
    
    /* data access primitives ----------------------------------------------- */
    
    public static long/*unsigned*/ getNorm32(char c) {
        return ((UNSIGNED_INT_MASK) & (NormTrieImpl.normTrie.getLeadValue(c)));
    }
    
    public static long/*unsigned*/ getNorm32FromSurrogatePair(long norm32, 
                                                               char c2) {
        /*
         * the surrogate index in norm32 stores only the number of the surrogate
         * index block see gennorm/store.c/getFoldedNormValue()
         */
        return ((UNSIGNED_INT_MASK) & 
                    NormTrieImpl.normTrie.getTrailValue((int)norm32, c2));
    }
    private static long getNorm32(int c){
        return (UNSIGNED_INT_MASK&(NormTrieImpl.normTrie.getCodePointValue(c)));
    }
    
//    private static long getNorm32(int c,int mask){
//        long/*unsigned*/ norm32= getNorm32(UTF16.getLeadSurrogate(c));
//        if(((norm32&mask)>0) && isNorm32LeadSurrogate(norm32)) {
//            /* c is a lead surrogate, get the real norm32 */
//            norm32=getNorm32FromSurrogatePair(norm32,UTF16.getTrailSurrogate(c));
//        }
//        return norm32; 
//    }
    
    /*
     * get a norm32 from text with complete code points
     * (like from decompositions)
     */
    private static long/*unsigned*/ getNorm32(char[] p,int start,
                                              int/*unsigned*/ mask) {
        long/*unsigned*/ norm32= getNorm32(p[start]);
        if(((norm32&mask)>0) && isNorm32LeadSurrogate(norm32)) {
            /* *p is a lead surrogate, get the real norm32 */
            norm32=getNorm32FromSurrogatePair(norm32, p[start+1]);
        }
        return norm32;
    }
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
        
    private static int getExtraDataIndex(long norm32) {
        return (int)(norm32>>EXTRA_SHIFT);
    }
    
    private static final class DecomposeArgs{
        int /*unsigned byte*/ cc;
        int /*unsigned byte*/ trailCC;
        int length;
    }
    /**
     * 
     * get the canonical or compatibility decomposition for one character 
     * 
     * @return index into the extraData array
     */
    private static int/*index*/ decompose(long/*unsigned*/ norm32, 
                                          int/*unsigned*/ qcMask, 
                                          DecomposeArgs args) {
        int p= getExtraDataIndex(norm32);
        args.length=extraData[p++];
    
        if((norm32&qcMask&QC_NFKD)!=0 && args.length>=0x100) {
            /* use compatibility decomposition, skip canonical data */
            p+=((args.length>>7)&1)+(args.length&DECOMP_LENGTH_MASK);
            args.length>>=8;
        }
    
        if((args.length&DECOMP_FLAG_LENGTH_HAS_CC)>0) {
            /* get the lead and trail cc's */
            char bothCCs=extraData[p++];
            args.cc=(UNSIGNED_BYTE_MASK) & (bothCCs>>8);
            args.trailCC=(UNSIGNED_BYTE_MASK) & bothCCs;
        } else {
            /* lead and trail cc's are both 0 */
            args.cc=args.trailCC=0;
        }
    
        args.length&=DECOMP_LENGTH_MASK;
        return p;
    }
    
       
    /**
     * get the canonical decomposition for one character 
     * @return index into the extraData array
     */
    private static int decompose(long/*unsigned*/ norm32, 
                                 DecomposeArgs args) {
                             
        int p= getExtraDataIndex(norm32);
        args.length=extraData[p++];
    
        if((args.length&DECOMP_FLAG_LENGTH_HAS_CC)>0) {
            /* get the lead and trail cc's */
            char bothCCs=extraData[p++];
            args.cc=(UNSIGNED_BYTE_MASK) & (bothCCs>>8);
            args.trailCC=(UNSIGNED_BYTE_MASK) & bothCCs;
        } else {
            /* lead and trail cc's are both 0 */
            args.cc=args.trailCC=0;
        }
    
        args.length&=DECOMP_LENGTH_MASK;
        return p;
    }
    
    
    private static final class NextCCArgs{
        char[] source;
        int next;
        int limit;
        char c;
        char c2;
    }
    
    /*
     * get the combining class of (c, c2)= args.source[args.next++]
     * before: args.next<args.limit  after: args.next<=args.limit
     * if only one code unit is used, then c2==0
     */
    private static int /*unsigned byte*/ getNextCC(NextCCArgs args) {
        long /*unsigned*/ norm32;
    
        args.c=args.source[args.next++];
        
        norm32= getNorm32(args.c);
        if((norm32 & CC_MASK)==0) {
            args.c2=0;
            return 0;
        } else {
            if(!isNorm32LeadSurrogate(norm32)) {
                args.c2=0;
            } else {
                /* c is a lead surrogate, get the real norm32 */
                if(args.next!=args.limit && 
                        UTF16.isTrailSurrogate(args.c2=args.source[args.next])){
                    ++args.next;
                    norm32=getNorm32FromSurrogatePair(norm32, args.c2);
                } else {
                    args.c2=0;
                    return 0;
                }
            }
    
            return (int)((UNSIGNED_BYTE_MASK) & (norm32>>CC_SHIFT));
        }
    }

    private static final class PrevArgs{
        char[] src;
        int start;
        int current;
        char c;
        char c2;
    }
    
    /*
     * read backwards and get norm32
     * return 0 if the character is <minC
     * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first 
     * surrogate but read second!)
     */
    private static long /*unsigned*/ getPrevNorm32(PrevArgs args,
                                                      int/*unsigned*/ minC, 
                                                      int/*unsigned*/ mask) {
        long/*unsigned*/ norm32;
    
        args.c=args.src[--args.current];
        args.c2=0;
    
        /* check for a surrogate before getting norm32 to see if we need to 
         * predecrement further 
         */
        if(args.c<minC) {
            return 0;
        } else if(!UTF16.isSurrogate(args.c)) {
            return getNorm32(args.c);
        } else if(UTF16.isLeadSurrogate(args.c)) {
            /* unpaired first surrogate */
            return 0;
        } else if(args.current!=args.start && 
                    UTF16.isLeadSurrogate(args.c2=args.src[args.current-1])) {
            --args.current;
            norm32=getNorm32(args.c2);
    
            if((norm32&mask)==0) {
                /* all surrogate pairs with this lead surrogate have 
                 * only irrelevant data 
                 */
                return 0;
            } else {
                /* norm32 must be a surrogate special */
                return getNorm32FromSurrogatePair(norm32, args.c);
            }
        } else {
            /* unpaired second surrogate */
            args.c2=0;
            return 0;
        }
    }
    
    /*
     * get the combining class of (c, c2)=*--p
     * before: start<p  after: start<=p
     */
    private static int /*unsigned byte*/ getPrevCC(PrevArgs args) {

        return (int)((UNSIGNED_BYTE_MASK)&(getPrevNorm32(args, MIN_WITH_LEAD_CC,
                                                         CC_MASK)>>CC_SHIFT));
    }

    /*
     * is this a safe boundary character for NF*D?
     * (lead cc==0)
     */
    public static boolean isNFDSafe(long/*unsigned*/ norm32, 
                                     int/*unsigned*/ccOrQCMask, 
                                     int/*unsigned*/ decompQCMask) {
        if((norm32&ccOrQCMask)==0) {
            return true; /* cc==0 and no decomposition: this is NF*D safe */
        }
    
        /* inspect its decomposition - maybe a Hangul but not a surrogate here*/
        if(isNorm32Regular(norm32) && (norm32&decompQCMask)!=0) {
            DecomposeArgs args=new DecomposeArgs();
            /* decomposes, get everything from the variable-length extra data */
            decompose(norm32, decompQCMask, args);
            return args.cc==0;
        } else {
            /* no decomposition (or Hangul), test the cc directly */
            return (norm32&CC_MASK)==0;
        }
    }
    
    /*
     * is this (or does its decomposition begin with) a "true starter"?
     * (cc==0 and NF*C_YES)
     */
    public static boolean isTrueStarter(long/*unsigned*/ norm32, 
                                          int/*unsigned*/ ccOrQCMask, 
                                          int/*unsigned*/ decompQCMask) {
        if((norm32&ccOrQCMask)==0) {
            return true; /* this is a true starter (could be Hangul or Jamo L)*/
        }
    
        /* inspect its decomposition - not a Hangul or a surrogate here */
        if((norm32&decompQCMask)!=0) {
            int p; /* index into extra data array */
            DecomposeArgs args=new DecomposeArgs();
            /* decomposes, get everything from the variable-length extra data */
            p=decompose(norm32, decompQCMask, args);
          
            if(args.cc==0) {
                int/*unsigned*/ qcMask=ccOrQCMask&QC_MASK;
    
                /* does it begin with NFC_YES? */
                if((getNorm32(extraData,p, qcMask)&qcMask)==0) {
                    /* yes, the decomposition begins with a true starter */
                    return true;
                }
            }
        }
        return false;
    }

    /* reorder UTF-16 in-place ---------------------------------------------- */
    
    /**
     * simpler, single-character version of mergeOrdered() -
     * bubble-insert one single code point into the preceding string
     * which is already canonically ordered
     * (c, c2) may or may not yet have been inserted at src[current]..src[p]
     *
     * it must be p=current+lengthof(c, c2) i.e. p=current+(c2==0 ? 1 : 2)
     *
     * before: src[start]..src[current] is already ordered, and
     *         src[current]..src[p]     may or may not hold (c, c2) but
     *                          must be exactly the same length as (c, c2)
     * after: src[start]..src[p] is ordered
     *
     * @return the trailing combining class
     */
    private static int/*unsigned byte*/ insertOrdered(char[] source, 
                                                      int start, 
                                                      int current, int p,
                                                         char c, char c2, 
                                                         int/*unsigned byte*/ cc) {
        int back, preBack;
        int r;
        int prevCC, trailCC=cc;
    
        if(start<current && cc!=0) {
            // search for the insertion point where cc>=prevCC 
            preBack=back=current;
            PrevArgs prevArgs = new PrevArgs();
            prevArgs.current  = current;
            prevArgs.start    = start;
            prevArgs.src      = source;
            // get the prevCC 
            prevCC=getPrevCC(prevArgs);
            preBack = prevArgs.current;
            
            if(cc<prevCC) {
                // this will be the last code point, so keep its cc 
                trailCC=prevCC;
                back=preBack;
                while(start<preBack) {
                    prevCC=getPrevCC(prevArgs);
                    preBack=prevArgs.current;
                    if(cc>=prevCC) {
                        break;
                    }
                    back=preBack;
                }
    
                
                // this is where we are right now with all these indicies:
                // [start]..[pPreBack] 0..? code points that we can ignore
                // [pPreBack]..[pBack] 0..1 code points with prevCC<=cc
                // [pBack]..[current] 0..n code points with >cc, move up to insert (c, c2)
                // [current]..[p]         1 code point (c, c2) with cc
                 
                // move the code units in between up 
                r=p;
                do {
                    source[--r]=source[--current];
                } while(back!=current);
            }
        }
    
        // insert (c, c2) 
        source[current]=c;
        if(c2!=0) {
            source[(current+1)]=c2;
        }
    
        // we know the cc of the last code point 
        return trailCC;
    }
    
    /**
     * merge two UTF-16 string parts together
     * to canonically order (order by combining classes) their concatenation
     *
     * the two strings may already be adjacent, so that the merging is done 
     * in-place if the two strings are not adjacent, then the buffer holding the
     * first one must be large enough
     * the second string may or may not be ordered in itself
     *
     * before: [start]..[current] is already ordered, and
     *         [next]..[limit]    may be ordered in itself, but
     *                          is not in relation to [start..current[
     * after: [start..current+(limit-next)[ is ordered
     *
     * the algorithm is a simple bubble-sort that takes the characters from 
     * src[next++] and inserts them in correct combining class order into the 
     * preceding part of the string
     *
     * since this function is called much less often than the single-code point
     * insertOrdered(), it just uses that for easier maintenance
     *
     * @return the trailing combining class
     */
    private static int /*unsigned byte*/ mergeOrdered(char[] source,
                                                      int start, 
                                                      int current,
                                                      char[] data,
                                                        int next, 
                                                        int limit, 
                                                        boolean isOrdered) {
            int r;
            int /*unsigned byte*/ cc, trailCC=0;
            boolean adjacent;
        
            adjacent= current==next;
            NextCCArgs ncArgs = new NextCCArgs();
            ncArgs.source = data;
            ncArgs.next   = next;
            ncArgs.limit  = limit;
            
            if(start!=current || !isOrdered) {
                    
                while(ncArgs.next<ncArgs.limit) {
                    cc=getNextCC(ncArgs);
                    if(cc==0) {
                        // does not bubble back 
                        trailCC=0;
                        if(adjacent) {
                            current=ncArgs.next;
                        } else {
                            data[current++]=ncArgs.c;
                            if(ncArgs.c2!=0) {
                                data[current++]=ncArgs.c2;
                            }
                        }
                        if(isOrdered) {
                            break;
                        } else {
                            start=current;
                        }
                    } else {
                        r=current+(ncArgs.c2==0 ? 1 : 2);
                        trailCC=insertOrdered(source,start, current, r, 
                                              ncArgs.c, ncArgs.c2, cc);
                        current=r;
                    }
                }
            }
        
            if(ncArgs.next==ncArgs.limit) {
                // we know the cc of the last code point 
                return trailCC;
            } else {
                if(!adjacent) {
                    // copy the second string part 
                    do {
                        source[current++]=data[ncArgs.next++];
                    } while(ncArgs.next!=ncArgs.limit);
                    ncArgs.limit=current;
                }
                PrevArgs prevArgs = new PrevArgs();
                prevArgs.src   = data;
                prevArgs.start = start;
                prevArgs.current =  ncArgs.limit;
                return getPrevCC(prevArgs);
            }

    }
    private static int /*unsigned byte*/ mergeOrdered(char[] source,
                                                      int start, 
                                                      int current,
                                                      char[] data,
                                                        final int next, 
                                                        final int limit) {
        return mergeOrdered(source,start,current,data,next,limit,true);
    } 

    
      
    public static boolean checkFCD(char[] src,int srcStart, int srcLimit,
                                   UnicodeSet nx) {

        char fcd16,c,c2;
        int prevCC=0, cc;
        int i =srcStart, length = srcLimit;
    
        for(;;) {
            for(;;) {
                if(i==length) {
                    return true;
                } else if((c=src[i++])<MIN_WITH_LEAD_CC) {
                    prevCC=(int)-c;
                } else if((fcd16=getFCD16(c))==0) {
                    prevCC=0;
                } else {
                    break;
                }
            }

            // check one above-minimum, relevant code unit 
            if(UTF16.isLeadSurrogate(c)) {
                // c is a lead surrogate, get the real fcd16 
                if(i!=length && UTF16.isTrailSurrogate(c2=src[i])) {
                    ++i;
                    fcd16=getFCD16FromSurrogatePair(fcd16, c2);
                } else {
                    c2=0;
                    fcd16=0;
                }
            }else{
                c2=0;
            }
            
            if(nx_contains(nx, c, c2)) {
                prevCC=0; /* excluded: fcd16==0 */
                continue;
            }

            // prevCC has values from the following ranges:
            // 0..0xff -the previous trail combining class
            // <0      -the negative value of the previous code unit;
            //          that code unit was <MIN_WITH_LEAD_CC and its getFCD16()
            //          was deferred so that average text is checked faster
            //
    
            // check the combining order 
            cc=(int)(fcd16>>8);
            if(cc!=0) {
                if(prevCC<0) {
                    // the previous character was <_NORM_MIN_WITH_LEAD_CC, 
                    // we need to get its trail cc 
                    //
                    if(!nx_contains(nx, (int)-prevCC)) {
                        prevCC=(int)(FCDTrieImpl.fcdTrie.getBMPValue(
                                             (char)-prevCC)&0xff
                                             ); 
                    } else {
                        prevCC=0; /* excluded: fcd16==0 */
                    }
                                      
                }
    
                if(cc<prevCC) {
                    return false;
                }
            }
            prevCC=(int)(fcd16&0xff);
        }
    }
    
    public static Normalizer.QuickCheckResult quickCheck(char[] src,
                                                            int srcStart, 
                                                            int srcLimit,
                                                            int minNoMaybe,
                                                            int qcMask,
                                                            int options,
                                                            boolean allowMaybe,
                                                            UnicodeSet nx){

        int ccOrQCMask;
        long norm32;
        char c, c2;
        char cc, prevCC;
        long qcNorm32;
        Normalizer.QuickCheckResult result;
        ComposePartArgs args = new ComposePartArgs();
        char[] buffer ;
        int start = srcStart;
        
        if(!isDataLoaded) {
            return Normalizer.MAYBE;
        }
        // initialize 
        ccOrQCMask=CC_MASK|qcMask;
        result=Normalizer.YES;
        prevCC=0;
                
        for(;;) {
            for(;;) {
                if(srcStart==srcLimit) {
                    return result;
                } else if((c=src[srcStart++])>=minNoMaybe && 
                                  (( norm32=getNorm32(c)) & ccOrQCMask)!=0) {
                    break;
                }
                prevCC=0;
            }
            
    
            // check one above-minimum, relevant code unit 
            if(isNorm32LeadSurrogate(norm32)) {
                // c is a lead surrogate, get the real norm32 
                if(srcStart!=srcLimit&& UTF16.isTrailSurrogate(c2=src[srcStart])) {
                    ++srcStart;
                    norm32=getNorm32FromSurrogatePair(norm32,c2);
                } else {
                    norm32=0;
                    c2=0;
                }
            }else{
                c2=0;
            }
            if(nx_contains(nx, c, c2)) {
                /* excluded: norm32==0 */
                norm32=0;
            }
    
            // check the combining order 
            cc=(char)((norm32>>CC_SHIFT)&0xFF);
            if(cc!=0 && cc<prevCC) {
                return Normalizer.NO;
            }
            prevCC=cc;
    
            // check for "no" or "maybe" quick check flags 
            qcNorm32 = norm32 & qcMask;
            if((qcNorm32& QC_ANY_NO)>=1) {
                result= Normalizer.NO;
                break;
            } else if(qcNorm32!=0) {
                // "maybe" can only occur for NFC and NFKC 
                if(allowMaybe){
                    result=Normalizer.MAYBE;
                }else{
                    // normalize a section around here to see if it is really 
                    // normalized or not 
                    int prevStarter;
                    int/*unsigned*/ decompQCMask;
    
                    decompQCMask=(qcMask<<2)&0xf; // decomposition quick check mask 
    
                    // find the previous starter 

                    // set prevStarter to the beginning of the current character 
                    prevStarter=srcStart-1; 
                    if(UTF16.isTrailSurrogate(src[prevStarter])) {
                        // safe because unpaired surrogates do not result 
                        // in "maybe"
                        --prevStarter; 
                    }

                    prevStarter=findPreviousStarter(src, start, prevStarter,
                                                    ccOrQCMask, decompQCMask,
                                                    (char)minNoMaybe);
    
                    // find the next true starter in [src..limit[ - modifies 
                    // src to point to the next starter 
                    srcStart=findNextStarter(src,srcStart, srcLimit, qcMask, 
                                             decompQCMask,(char) minNoMaybe);
                    
                    //set the args for compose part
                    args.prevCC = prevCC;
                       
                    // decompose and recompose [prevStarter..src[ 
                    buffer = composePart(args,prevStarter,src,srcStart,srcLimit,options,nx);
    
                    // compare the normalized version with the original 
                    if(0!=strCompare(buffer,0,args.length,src,prevStarter,srcStart, false)) {
                        result=Normalizer.NO; // normalization differs 
                        break;
                    }
    
                    // continue after the next starter 
                }
            }
        }
        return result;
    } 
 
       
    //------------------------------------------------------ 
    // make NFD & NFKD 
    //------------------------------------------------------
    public static int getDecomposition(int c /*UTF-32*/ , 
                                        boolean compat,
                                           char[] dest,
                                           int destStart, 
                                           int destCapacity) {
            
        if( (UNSIGNED_INT_MASK & c)<=0x10ffff) {
            long /*unsigned*/ norm32;
            int qcMask;
            int minNoMaybe;
            int length;
    
            // initialize 
            if(!compat) {
                minNoMaybe=(int)indexes[INDEX_MIN_NFD_NO_MAYBE];
                qcMask=QC_NFD;
            } else {
                minNoMaybe=(int)indexes[INDEX_MIN_NFKD_NO_MAYBE];
                qcMask=QC_NFKD;
            }
    
            if(c<minNoMaybe) {
                // trivial case 
                if(destCapacity>0) {
                    dest[0]=(char)c;
                }
                return -1;
            }
    
            /* data lookup */
            norm32=getNorm32(c);
            if((norm32&qcMask)==0) {
                /* simple case: no decomposition */
                if(c<=0xffff) {
                    if(destCapacity>0) {
                        dest[0]=(char)c;
                    }
                    return -1;
                } else {
                    if(destCapacity>=2) {
                        dest[0]=UTF16.getLeadSurrogate(c);
                        dest[1]=UTF16.getTrailSurrogate(c);
                    }
                    return -2;
                }
            } else if(isNorm32HangulOrJamo(norm32)) {
                /* Hangul syllable: decompose algorithmically */
                char c2;
    
                c-=HANGUL_BASE;
    
                c2=(char)(c%JAMO_T_COUNT);
                c/=JAMO_T_COUNT;
                if(c2>0) {
                    if(destCapacity>=3) {
                        dest[2]=(char)(JAMO_T_BASE+c2);
                    }
                    length=3;
                } else {
                    length=2;
                }
    
                if(destCapacity>=2) {
                    dest[1]=(char)(JAMO_V_BASE+c%JAMO_V_COUNT);
                    dest[0]=(char)(JAMO_L_BASE+c/JAMO_V_COUNT);
                }
                return length;
            } else {
                /* c decomposes, get everything from the variable-length extra 
                 * data 
                 */
                int p, limit;
                DecomposeArgs args = new DecomposeArgs();
                /* the index into extra data array*/                 
                p=decompose(norm32, qcMask, args);
                if(args.length<=destCapacity) {
                    limit=p+args.length;
                    do {
                        dest[destStart++]=extraData[p++];
                    } while(p<limit);
                }
                return args.length;
            }
        } else {
            return 0;
        }
    }

    
    public static int decompose(char[] src,int srcStart,int srcLimit,
                                char[] dest,int destStart,int destLimit,
                                 boolean compat,int[] outTrailCC,
                                 UnicodeSet nx) {
                                
        char[] buffer = new char[3];
        int prevSrc;
        long norm32;
        int ccOrQCMask, qcMask;
        int reorderStartIndex, length;
        char c, c2, minNoMaybe;
        int/*unsigned byte*/ cc, prevCC, trailCC;
        char[] p;
        int pStart;
        int destIndex = destStart;
        int srcIndex = srcStart;
        if(!compat) {
            minNoMaybe=(char)indexes[INDEX_MIN_NFD_NO_MAYBE];
            qcMask=QC_NFD;
        } else {
            minNoMaybe=(char)indexes[INDEX_MIN_NFKD_NO_MAYBE];
            qcMask=QC_NFKD;
        }
    
        /* initialize */
        ccOrQCMask=CC_MASK|qcMask;
        reorderStartIndex=0;
        prevCC=0;
        norm32=0;
        c=0;
        pStart=0;
        
        cc=trailCC=-1;//initialize to bogus value
        
        for(;;) {
            /* count code units below the minimum or with irrelevant data for 
             * the quick check 
             */
            prevSrc=srcIndex;

            while(srcIndex!=srcLimit &&((c=src[srcIndex])<minNoMaybe || 
                                        ((norm32=getNorm32(c))&ccOrQCMask)==0)){
                prevCC=0;
                ++srcIndex;
            }

            /* copy these code units all at once */
            if(srcIndex!=prevSrc) {
                length=(int)(srcIndex-prevSrc);
                if((destIndex+length)<=destLimit) {
                    System.arraycopy(src,prevSrc,dest,destIndex,length);
                }
              
                destIndex+=length;
                reorderStartIndex=destIndex;
            }
    
            /* end of source reached? */
            if(srcIndex==srcLimit) {
                break;
            }
    
            /* c already contains *src and norm32 is set for it, increment src*/
            ++srcIndex;
    
            /* check one above-minimum, relevant code unit */
            /*
             * generally, set p and length to the decomposition string
             * in simple cases, p==NULL and (c, c2) will hold the length code 
             * units to append in all cases, set cc to the lead and trailCC to 
             * the trail combining class
             *
             * the following merge-sort of the current character into the 
             * preceding, canonically ordered result text will use the 
             * optimized insertOrdered()
             * if there is only one single code point to process;
             * this is indicated with p==NULL, and (c, c2) is the character to 
             * insert
             * ((c, 0) for a BMP character and (lead surrogate, trail surrogate)
             * for a supplementary character)
             * otherwise, p[length] is merged in with _mergeOrdered()
             */
            if(isNorm32HangulOrJamo(norm32)) {
                if(nx_contains(nx, c)) {
                    c2=0;
                    p=null;
                    length=1;
                } else {
                    // Hangul syllable: decompose algorithmically 
                    p=buffer;
                    pStart=0;
                    cc=trailCC=0;
    
                    c-=HANGUL_BASE;
    
                    c2=(char)(c%JAMO_T_COUNT);
                    c/=JAMO_T_COUNT;
                    if(c2>0) {
                        buffer[2]=(char)(JAMO_T_BASE+c2);
                        length=3;
                    } else {
                        length=2;
                    }
    
                    buffer[1]=(char)(JAMO_V_BASE+c%JAMO_V_COUNT);
                    buffer[0]=(char)(JAMO_L_BASE+c/JAMO_V_COUNT);
                }
            } else {
                if(isNorm32Regular(norm32)) {
                    c2=0;
                    length=1;
                } else {
                    // c is a lead surrogate, get the real norm32 
                    if(srcIndex!=srcLimit && 
                                    UTF16.isTrailSurrogate(c2=src[srcIndex])) {
                        ++srcIndex;
                        length=2;
                        norm32=getNorm32FromSurrogatePair(norm32, c2);
                    } else {
                        c2=0;
                        length=1;
                        norm32=0;
                    }
                }
    
                /* get the decomposition and the lead and trail cc's */
                if(nx_contains(nx, c, c2)) {
                    /* excluded: norm32==0 */
                    cc=trailCC=0;
                    p=null;
                } else if((norm32&qcMask)==0) {
                    /* c does not decompose */
                    cc=trailCC=(int)((UNSIGNED_BYTE_MASK) & (norm32>>CC_SHIFT));
                    p=null;
                    pStart=-1;
                } else {
                    DecomposeArgs arg = new DecomposeArgs();
                    /* c decomposes, get everything from the variable-length 
                     * extra data 
                     */
                    pStart=decompose(norm32, qcMask, arg);
                    p=extraData;
                    length=arg.length;
                    cc=arg.cc;
                    trailCC=arg.trailCC;
                    if(length==1) {
                        /* fastpath a single code unit from decomposition */
                        c=p[pStart];
                        c2=0;
                        p=null;
                        pStart=-1;
                    }
                }
            }
    
            /* append the decomposition to the destination buffer, assume 
             * length>0 
             */
            if((destIndex+length)<=destLimit) {
                int reorderSplit=destIndex;
                if(p==null) {
                    /* fastpath: single code point */
                    if(cc!=0 && cc<prevCC) {
                        /* (c, c2) is out of order with respect to the preceding
                         *  text 
                         */
                        destIndex+=length;
                        trailCC=insertOrdered(dest,reorderStartIndex, 
                                            reorderSplit, destIndex, c, c2, cc);
                    } else {
                        /* just append (c, c2) */
                        dest[destIndex++]=c;
                        if(c2!=0) {
                            dest[destIndex++]=c2;
                        }
                    }
                } else {
                    /* general: multiple code points (ordered by themselves) 
                     * from decomposition 
                     */
                    if(cc!=0 && cc<prevCC) {
                        /* the decomposition is out of order with respect to the
                         *  preceding text 
                         */
                        destIndex+=length;
                        trailCC=mergeOrdered(dest,reorderStartIndex, 
                                          reorderSplit,p, pStart,pStart+length);
                    } else {
                        /* just append the decomposition */
                        do {
                            dest[destIndex++]=p[pStart++];
                        } while(--length>0);
                    }
                }
            } else {
                /* buffer overflow */
                /* keep incrementing the destIndex for preflighting */
                destIndex+=length;
            }
    
            prevCC=trailCC;
            if(prevCC==0) {
                reorderStartIndex=destIndex;
            }
        }
    
        outTrailCC[0]=prevCC;

        return destIndex - destStart;
    }
    
    /* make NFC & NFKC ------------------------------------------------------ */
    private static final class NextCombiningArgs{
        char[] source;
        int start;
        //int limit;
        char c;
        char c2;
        int/*unsigned*/ combiningIndex;
        char /*unsigned byte*/ cc;
    }
    
    /* get the composition properties of the next character */
    private static int /*unsigned*/    getNextCombining(NextCombiningArgs args,
                                                    int limit,
                                                    UnicodeSet nx) {
        long/*unsigned*/ norm32; 
        int combineFlags;
        /* get properties */
        args.c=args.source[args.start++];
        norm32=getNorm32(args.c);
        
        /* preset output values for most characters */
        args.c2=0;
        args.combiningIndex=0;
        args.cc=0;
        
        if((norm32&(CC_MASK|COMBINES_ANY))==0) {
            return 0;
        } else {
            if(isNorm32Regular(norm32)) {
                /* set cc etc. below */
            } else if(isNorm32HangulOrJamo(norm32)) {
                /* a compatibility decomposition contained Jamos */
                args.combiningIndex=(int)((UNSIGNED_INT_MASK)&(0xfff0|
                                                        (norm32>>EXTRA_SHIFT)));
                return (int)(norm32&COMBINES_ANY);
            } else {
                /* c is a lead surrogate, get the real norm32 */
                if(args.start!=limit && UTF16.isTrailSurrogate(args.c2=
                                                     args.source[args.start])) {
                    ++args.start;
                    norm32=getNorm32FromSurrogatePair(norm32, args.c2);
                } else {
                    args.c2=0;
                    return 0;
                }
            }
            
            if(nx_contains(nx, args.c, args.c2)) {
                return 0; /* excluded: norm32==0 */
            }
    
            args.cc= (char)((norm32>>CC_SHIFT)&0xff);
        
            combineFlags=(int)(norm32&COMBINES_ANY);
            if(combineFlags!=0) {
                int index = getExtraDataIndex(norm32);
                args.combiningIndex=index>0 ? extraData[(index-1)] :0;
            }
    
            return combineFlags;
        }
    }
    
    /*
     * given a composition-result starter (c, c2) - which means its cc==0,
     * it combines forward, it has extra data, its norm32!=0,
     * it is not a Hangul or Jamo,
     * get just its combineFwdIndex
     *
     * norm32(c) is special if and only if c2!=0
     */
    private static int/*unsigned*/ getCombiningIndexFromStarter(char c,char c2){
        long/*unsigned*/ norm32;
    
        norm32=getNorm32(c);
        if(c2!=0) {
            norm32=getNorm32FromSurrogatePair(norm32, c2);
        }
        return extraData[(getExtraDataIndex(norm32)-1)];
    }
    
    /*
     * Find the recomposition result for
     * a forward-combining character
     * (specified with a pointer to its part of the combiningTable[])
     * and a backward-combining character
     * (specified with its combineBackIndex).
     *
     * If these two characters combine, then set (value, value2)
     * with the code unit(s) of the composition character.
     *
     * Return value:
     * 0    do not combine
     * 1    combine
     * >1   combine, and the composition is a forward-combining starter
     *
     * See unormimp.h for a description of the composition table format.
     */
    private static int/*unsigned*/ combine(char[]table,int tableStart, 
                                   int/*unsinged*/ combineBackIndex,
                                    int[] outValues) {
        int/*unsigned*/ key;
        int value,value2;
        
        if(outValues.length<2){
            throw new IllegalArgumentException();
        }
        
        /* search in the starter's composition table */
        for(;;) {
            key=table[tableStart++];
            if(key>=combineBackIndex) {
                break;
            }
            tableStart+= ((table[tableStart]&0x8000) != 0)? 2 : 1;
        }
    
        /* mask off bit 15, the last-entry-in-the-list flag */
        if((key&0x7fff)==combineBackIndex) {
            /* found! combine! */
            value=table[tableStart];
    
            /* is the composition a starter that combines forward? */
            key=(int)((UNSIGNED_INT_MASK)&((value&0x2000)+1));
    
            /* get the composition result code point from the variable-length 
             * result value 
             */
            if((value&0x8000) != 0) {
                if((value&0x4000) != 0) {
                    /* surrogate pair composition result */
                    value=(int)((UNSIGNED_INT_MASK)&((value&0x3ff)|0xd800));
                    value2=table[tableStart+1];
                } else {
                    /* BMP composition result U+2000..U+ffff */
                    value=table[tableStart+1];
                    value2=0;
                }
            } else {
                /* BMP composition result U+0000..U+1fff */
                value&=0x1fff;
                value2=0;
            }
            outValues[0]=value;
            outValues[1]=value2;    
            return key;
        } else {
            /* not found */
            return 0;
        }
    }
    
    
    private static final class RecomposeArgs{
        char[] source;
        int start;
        int limit;
    }
    /*
     * recompose the characters in [p..limit[
     * (which is in NFD - decomposed and canonically ordered),
     * adjust limit, and return the trailing cc
     *
     * since for NFKC we may get Jamos in decompositions, we need to
     * recompose those too
     *
     * note that recomposition never lengthens the text:
     * any character consists of either one or two code units;
     * a composition may contain at most one more code unit than the original 
     * starter, while the combining mark that is removed has at least one code 
     * unit
     */
    private static char/*unsigned byte*/ recompose(RecomposeArgs args, int options, UnicodeSet nx) {
        int  remove, q, r;
        int /*unsigned*/ combineFlags;
        int /*unsigned*/ combineFwdIndex, combineBackIndex;
        int /*unsigned*/ result, value=0, value2=0;
        int /*unsigned byte*/  prevCC;
        boolean starterIsSupplementary;
        int starter;
        int[] outValues = new int[2];
        starter=-1;                   /* no starter */
        combineFwdIndex=0;            /* will not be used until starter!=NULL */
        starterIsSupplementary=false; /* will not be used until starter!=NULL */
        prevCC=0;
        
        NextCombiningArgs ncArg = new NextCombiningArgs();
        ncArg.source  = args.source;
        
        ncArg.cc      =0;
        ncArg.c2      =0;    

        for(;;) {
            ncArg.start = args.start;
            combineFlags=getNextCombining(ncArg,args.limit,nx);
            combineBackIndex=ncArg.combiningIndex;
            args.start = ncArg.start;
                        
            if(((combineFlags&COMBINES_BACK)!=0) && starter!=-1) {
                if((combineBackIndex&0x8000)!=0) {
                    /* c is a Jamo V/T, see if we can compose it with the 
                     * previous character 
                     */
                    /* for the PRI #29 fix, check that there is no intervening combining mark */
                    if((options&BEFORE_PRI_29)!=0 || prevCC==0) {
                        remove=-1; /* NULL while no Hangul composition */
                        combineFlags=0;
                        ncArg.c2=args.source[starter];
                        if(combineBackIndex==0xfff2) {
                            /* Jamo V, compose with previous Jamo L and following 
                             * Jamo T 
                             */
                            ncArg.c2=(char)(ncArg.c2-JAMO_L_BASE);
                            if(ncArg.c2<JAMO_L_COUNT) {
                                remove=args.start-1;
                                ncArg.c=(char)(HANGUL_BASE+(ncArg.c2*JAMO_V_COUNT+
                                               (ncArg.c-JAMO_V_BASE))*JAMO_T_COUNT);
                                if(args.start!=args.limit && 
                                            (ncArg.c2=(char)(args.source[args.start]
                                             -JAMO_T_BASE))<JAMO_T_COUNT) {
                                    ++args.start;
                                    ncArg.c+=ncArg.c2;
                                 } else {
                                     /* the result is an LV syllable, which is a starter (unlike LVT) */
                                     combineFlags=COMBINES_FWD;
                                }
                                if(!nx_contains(nx, ncArg.c)) {
                                    args.source[starter]=ncArg.c;
                                   } else {
                                    /* excluded */
                                    if(!isHangulWithoutJamoT(ncArg.c)) {
                                        --args.start; /* undo the ++args.start from reading the Jamo T */
                                    }
                                    /* c is modified but not used any more -- c=*(p-1); -- re-read the Jamo V/T */
                                    remove=args.start;
                                }
                            }

                        /*
                         * Normally, the following can not occur:
                         * Since the input is in NFD, there are no Hangul LV syllables that
                         * a Jamo T could combine with.
                         * All Jamo Ts are combined above when handling Jamo Vs.
                         *
                         * However, before the PRI #29 fix, this can occur due to
                         * an intervening combining mark between the Hangul LV and the Jamo T.
                         */
                        } else {
                            /* Jamo T, compose with previous Hangul that does not have a Jamo T */
                            if(isHangulWithoutJamoT(ncArg.c2)) {
                                ncArg.c2+=ncArg.c-JAMO_T_BASE;
                                if(!nx_contains(nx, ncArg.c2)) {
                                    remove=args.start-1;
                                    args.source[starter]=ncArg.c2;
                                }
                            }
                        }
        
                        if(remove!=-1) {
                            /* remove the Jamo(s) */
                            q=remove;
                            r=args.start;
                            while(r<args.limit) {
                                args.source[q++]=args.source[r++];
                            }
                            args.start=remove;
                            args.limit=q;
                        }
        
                        ncArg.c2=0; /* c2 held *starter temporarily */

                        if(combineFlags!=0) {
                            /*
                             * not starter=NULL because the composition is a Hangul LV syllable
                             * and might combine once more (but only before the PRI #29 fix)
                             */

                            /* done? */
                            if(args.start==args.limit) {
                                return (char)prevCC;
                            }

                            /* the composition is a Hangul LV syllable which is a starter that combines forward */
                            combineFwdIndex=0xfff0;

                            /* we combined; continue with looking for compositions */
                            continue;
                        }
                    }

                    /*
                     * now: cc==0 and the combining index does not include 
                     * "forward" -> the rest of the loop body will reset starter
                     * to NULL; technically, a composed Hangul syllable is a 
                     * starter, but it does not combine forward now that we have
                     * consumed all eligible Jamos; for Jamo V/T, combineFlags 
                     * does not contain _NORM_COMBINES_FWD
                     */
    
                } else if(
                    /* the starter is not a Hangul LV or Jamo V/T and */
                    !((combineFwdIndex&0x8000)!=0) &&
                    /* the combining mark is not blocked and */
                    ((options&BEFORE_PRI_29)!=0 ?
                        (prevCC!=ncArg.cc || prevCC==0) :
                        (prevCC<ncArg.cc || prevCC==0)) &&
                    /* the starter and the combining mark (c, c2) do combine */
                    0!=(result=combine(combiningTable,combineFwdIndex, 
                                       combineBackIndex, outValues)) &&
                    /* the composition result is not excluded */
                    !nx_contains(nx, (char)value, (char)value2)
                ) {
                    value=outValues[0];
                    value2=outValues[1];
                    /* replace the starter with the composition, remove the 
                     * combining mark 
                     */
                    remove= ncArg.c2==0 ? args.start-1 : args.start-2; /* index to the combining mark */
    
                    /* replace the starter with the composition */
                    args.source[starter]=(char)value;
                    if(starterIsSupplementary) {
                        if(value2!=0) {
                            /* both are supplementary */
                            args.source[starter+1]=(char)value2;
                        } else {
                            /* the composition is shorter than the starter, 
                             * move the intermediate characters forward one */
                            starterIsSupplementary=false;
                            q=starter+1;
                            r=q+1;
                            while(r<remove) {
                                args.source[q++]=args.source[r++];
                            }
                            --remove;
                        }
                    } else if(value2!=0) {
                        /* the composition is longer than the starter, 
                         * move the intermediate characters back one */
                        starterIsSupplementary=true;
                        /* temporarily increment for the loop boundary */
                        ++starter; 
                        q=remove;
                        r=++remove;
                        while(starter<q) {
                            args.source[--r]=args.source[--q];
                        }
                        args.source[starter]=(char)value2;
                        --starter; /* undo the temporary increment */
                    /* } else { both are on the BMP, nothing more to do */
                    }
    
                    /* remove the combining mark by moving the following text 
                     * over it */
                    if(remove<args.start) {
                        q=remove;
                        r=args.start;
                        while(r<args.limit) {
                            args.source[q++]=args.source[r++];
                        }
                        args.start=remove;
                        args.limit=q;
                    }
    
                    /* keep prevCC because we removed the combining mark */
    
                    /* done? */
                    if(args.start==args.limit) {
                        return (char)prevCC;
                    }
    
                    /* is the composition a starter that combines forward? */
                    if(result>1) {
                       combineFwdIndex=getCombiningIndexFromStarter((char)value,
                                                                  (char)value2);
                    } else {
                       starter=-1;
                    }
    
                    /* we combined; continue with looking for compositions */
                    continue;
                }
            }
    
            /* no combination this time */
            prevCC=ncArg.cc;
            if(args.start==args.limit) {
                return (char)prevCC;
            }
    
            /* if (c, c2) did not combine, then check if it is a starter */
            if(ncArg.cc==0) {
                /* found a new starter; combineFlags==0 if (c, c2) is excluded */
                if((combineFlags&COMBINES_FWD)!=0) {
                    /* it may combine with something, prepare for it */
                    if(ncArg.c2==0) {
                        starterIsSupplementary=false;
                        starter=args.start-1;
                    } else {
                        starterIsSupplementary=false;
                        starter=args.start-2;
                    }
                    combineFwdIndex=combineBackIndex;
                } else {
                    /* it will not combine with anything */
                    starter=-1;
                }
            } else if((options&OPTIONS_COMPOSE_CONTIGUOUS)!=0) {
                /* FCC: no discontiguous compositions; any intervening character blocks */
                starter=-1;
            }
        }
    }
   
    // find the last true starter between src[start]....src[current] going 
    // backwards and return its index
    private static int findPreviousStarter(char[]src, int srcStart, int current, 
                                          int/*unsigned*/ ccOrQCMask, 
                                          int/*unsigned*/ decompQCMask,
                                          char minNoMaybe) { 
       long norm32; 
       PrevArgs args = new PrevArgs();
       args.src = src;
       args.start = srcStart;
       args.current = current;
       
       while(args.start<args.current) { 
           norm32= getPrevNorm32(args, minNoMaybe, ccOrQCMask|decompQCMask); 
           if(isTrueStarter(norm32, ccOrQCMask, decompQCMask)) { 
               break; 
           } 
       } 
       return args.current; 
    }
    
    /* find the first true starter in [src..limit[ and return the 
     * pointer to it 
     */
    private static int/*index*/    findNextStarter(char[] src,int start,int limit,
                                                 int/*unsigned*/ qcMask, 
                                                 int/*unsigned*/ decompQCMask, 
                                                 char minNoMaybe) {
        int p;
        long/*unsigned*/ norm32; 
        int ccOrQCMask;
        char c, c2;
    
        ccOrQCMask=CC_MASK|qcMask;
        
        DecomposeArgs decompArgs = new DecomposeArgs();

        for(;;) {
            if(start >= limit) {
                break; /* end of string */
            }
            c=src[start];
            if(c<minNoMaybe) {
                break; /* catches NUL terminater, too */
            }
    
            norm32=getNorm32(c);
            if((norm32&ccOrQCMask)==0) {
                break; /* true starter */
            }
    
            if(isNorm32LeadSurrogate(norm32)) {
                /* c is a lead surrogate, get the real norm32 */
                if((start+1)==limit || 
                                   !UTF16.isTrailSurrogate(c2=(src[start+1]))){
                    /* unmatched first surrogate: counts as a true starter */                  
                    break; 
                }
                norm32=getNorm32FromSurrogatePair(norm32, c2);
    
                if((norm32&ccOrQCMask)==0) {
                    break; /* true starter */
                }
            } else {
                c2=0;
            }
    
            /* (c, c2) is not a true starter but its decomposition may be */
            if((norm32&decompQCMask)!=0) {
                /* (c, c2) decomposes, get everything from the variable-length
                 *  extra data */
                p=decompose(norm32, decompQCMask, decompArgs);
    
                /* get the first character's norm32 to check if it is a true 
                 * starter */
                if(decompArgs.cc==0 && (getNorm32(extraData,p, qcMask)&qcMask)==0) {
                    break; /* true starter */
                }
            }
    
            start+= c2==0 ? 1 : 2; /* not a true starter, continue */
        }
    
        return start;
    }
    
    
    private static final class ComposePartArgs{
        int prevCC;
        int length;   /* length of decomposed part */
    }
        
     /* decompose and recompose [prevStarter..src[ */
    private static char[] composePart(ComposePartArgs args, 
                                      int prevStarter, 
                                         char[] src, int start, int limit,
                                       int options,
                                       UnicodeSet nx) {
        int recomposeLimit;
        boolean compat =((options&OPTIONS_COMPAT)!=0);
        
        /* decompose [prevStarter..src[ */
        int[] outTrailCC = new int[1];
        char[] buffer = new char[(limit-prevStarter)*MAX_BUFFER_SIZE];

        for(;;){
            args.length=decompose(src,prevStarter,(start),
                                      buffer,0,buffer.length, 
                                      compat,outTrailCC,nx);
            if(args.length<=buffer.length){
                break;
            }else{
                buffer = new char[args.length];
            }
        } 
    
        /* recompose the decomposition */
        recomposeLimit=args.length;
          
        if(args.length>=2) {
            RecomposeArgs rcArgs = new RecomposeArgs();
            rcArgs.source    = buffer;
            rcArgs.start    = 0;
            rcArgs.limit    = recomposeLimit; 
            args.prevCC=recompose(rcArgs, options, nx);
            recomposeLimit = rcArgs.limit;
        }
        
        /* return with a pointer to the recomposition and its length */
        args.length=recomposeLimit;
        return buffer;
    }
    
    private static boolean composeHangul(char prev, char c,
                                         long/*unsigned*/ norm32, 
                                         char[] src,int[] srcIndex, int limit,
                                            boolean compat, 
                                         char[] dest,int destIndex,
                                         UnicodeSet nx) {
        int start=srcIndex[0];
        if(isJamoVTNorm32JamoV(norm32)) {
            /* c is a Jamo V, compose with previous Jamo L and 
             * following Jamo T */
            prev=(char)(prev-JAMO_L_BASE);
            if(prev<JAMO_L_COUNT) {
                c=(char)(HANGUL_BASE+(prev*JAMO_V_COUNT+
                                                 (c-JAMO_V_BASE))*JAMO_T_COUNT);
    
                /* check if the next character is a Jamo T (normal or 
                 * compatibility) */
                if(start!=limit) {
                    char next, t;
    
                    next=src[start];
                    if((t=(char)(next-JAMO_T_BASE))<JAMO_T_COUNT) {
                        /* normal Jamo T */
                        ++start;
                        c+=t;
                    } else if(compat) {
                        /* if NFKC, then check for compatibility Jamo T 
                         * (BMP only) */
                        norm32=getNorm32(next);
                        if(isNorm32Regular(norm32) && ((norm32&QC_NFKD)!=0)) {
                            int p /*index into extra data array*/;
                            DecomposeArgs dcArgs = new DecomposeArgs();
                            p=decompose(norm32, QC_NFKD, dcArgs);
                            if(dcArgs.length==1 && 
                                   (t=(char)(extraData[p]-JAMO_T_BASE))
                                                   <JAMO_T_COUNT) {
                                /* compatibility Jamo T */
                                ++start;
                                c+=t;
                            }
                        }
                    }
                }
                if(nx_contains(nx, c)) {
                    if(!isHangulWithoutJamoT(c)) {
                        --start; /* undo ++start from reading the Jamo T */
                    }
                    return false;
                }
                dest[destIndex]=c;
                srcIndex[0]=start;
                return true;
            }
        } else if(isHangulWithoutJamoT(prev)) {
            /* c is a Jamo T, compose with previous Hangul LV that does not 
             * contain a Jamo T */
            c=(char)(prev+(c-JAMO_T_BASE));
            if(nx_contains(nx, c)) {
                return false;
            }
            dest[destIndex]=c;
            srcIndex[0]=start;
            return true;
        }
        return false;
    }
    /*
    public static int compose(char[] src, char[] dest,boolean compat, UnicodeSet nx){
        return compose(src,0,src.length,dest,0,dest.length,compat, nx);
    }
    */
    
    public static int compose(char[] src, int srcStart, int srcLimit,
                              char[] dest,int destStart,int destLimit,
                              int options,UnicodeSet nx) {
        
        int prevSrc, prevStarter;
        long/*unsigned*/ norm32; 
        int ccOrQCMask, qcMask;
        int  reorderStartIndex, length;
        char c, c2, minNoMaybe;
        int/*unsigned byte*/ cc, prevCC;
        int[] ioIndex = new int[1];
        int destIndex = destStart;
        int srcIndex = srcStart;
        
        if((options&OPTIONS_COMPAT)!=0) {
            minNoMaybe=(char)indexes[INDEX_MIN_NFKC_NO_MAYBE];
            qcMask=QC_NFKC;
        } else {
            minNoMaybe=(char)indexes[INDEX_MIN_NFC_NO_MAYBE];
            qcMask=QC_NFC;
        }
    
        /*
         * prevStarter points to the last character before the current one
         * that is a "true" starter with cc==0 and quick check "yes".
         *
         * prevStarter will be used instead of looking for a true starter
         * while incrementally decomposing [prevStarter..prevSrc[
         * in _composePart(). Having a good prevStarter allows to just decompose
         * the entire [prevStarter..prevSrc[.
         *
         * When _composePart() backs out from prevSrc back to prevStarter,
         * then it also backs out destIndex by the same amount.
         * Therefore, at all times, the (prevSrc-prevStarter) source units
         * must correspond 1:1 to destination units counted with destIndex,
         * except for reordering.
         * This is true for the qc "yes" characters copied in the fast loop,
         * and for pure reordering.
         * prevStarter must be set forward to src when this is not true:
         * In _composePart() and after composing a Hangul syllable.
         *
         * This mechanism relies on the assumption that the decomposition of a 
         * true starter also begins with a true starter. gennorm/store.c checks 
         * for this.
         */
        prevStarter=srcIndex;
    
        ccOrQCMask=CC_MASK|qcMask;
        /*destIndex=*/reorderStartIndex=0;/* ####TODO#### check this **/
        prevCC=0;
    
        /* avoid compiler warnings */
        norm32=0;
        c=0;
    
        for(;;) {
            /* count code units below the minimum or with irrelevant data for 
             * the quick check */
            prevSrc=srcIndex;

            while(srcIndex!=srcLimit && ((c=src[srcIndex])<minNoMaybe || 
                     ((norm32=getNorm32(c))&ccOrQCMask)==0)) {
                prevCC=0;
                ++srcIndex;
            }

    
            /* copy these code units all at once */
            if(srcIndex!=prevSrc) {
                length=(int)(srcIndex-prevSrc);
                if((destIndex+length)<=destLimit) {
                    System.arraycopy(src,prevSrc,dest,destIndex,length);
                }
                destIndex+=length;
                reorderStartIndex=destIndex;
    
                /* set prevStarter to the last character in the quick check 
                 * loop */
                prevStarter=srcIndex-1;
                if(UTF16.isTrailSurrogate(src[prevStarter]) && 
                    prevSrc<prevStarter && 
                    UTF16.isLeadSurrogate(src[(prevStarter-1)])) {
                    --prevStarter;
                }
    
                prevSrc=srcIndex;
            }
    
            /* end of source reached? */
            if(srcIndex==srcLimit) {
                break;
            }
    
            /* c already contains *src and norm32 is set for it, increment src*/
            ++srcIndex;
    
            /*
             * source buffer pointers:
             *
             *  all done      quick check   current char  not yet
             *                "yes" but     (c, c2)       processed
             *                may combine
             *                forward
             * [-------------[-------------[-------------[-------------[
             * |             |             |             |             |
             * start         prevStarter   prevSrc       src           limit
             *
             *
             * destination buffer pointers and indexes:
             *
             *  all done      might take    not filled yet
             *                characters for
             *                reordering
             * [-------------[-------------[-------------[
             * |             |             |             |
             * dest      reorderStartIndex destIndex     destCapacity
             */
    
            /* check one above-minimum, relevant code unit */
            /*
             * norm32 is for c=*(src-1), and the quick check flag is "no" or 
             * "maybe", and/or cc!=0
             * check for Jamo V/T, then for surrogates and regular characters
             * c is not a Hangul syllable or Jamo L because
             * they are not marked with no/maybe for NFC & NFKC(and their cc==0)
             */
            if(isNorm32HangulOrJamo(norm32)) {
                /*
                 * c is a Jamo V/T:
                 * try to compose with the previous character, Jamo V also with 
                 * a following Jamo T, and set values here right now in case we 
                 * just continue with the main loop
                 */
                prevCC=cc=0;
                reorderStartIndex=destIndex;
                ioIndex[0]=srcIndex;
                if( 
                    destIndex>0 &&
                    composeHangul(src[(prevSrc-1)], c, norm32,src, ioIndex,
                                  srcLimit, (options&OPTIONS_COMPAT)!=0, dest,
                                  destIndex<=destLimit ? destIndex-1: 0,
                                  nx)
                ) {
                    srcIndex=ioIndex[0];
                    prevStarter=srcIndex;
                    continue;
                }
                
                srcIndex = ioIndex[0];
    
                /* the Jamo V/T did not compose into a Hangul syllable, just 
                 * append to dest */
                c2=0;
                length=1;
                prevStarter=prevSrc;
            } else {
                if(isNorm32Regular(norm32)) {
                    c2=0;
                    length=1;
                } else {
                    /* c is a lead surrogate, get the real norm32 */
                    if(srcIndex!=srcLimit &&
                                     UTF16.isTrailSurrogate(c2=src[srcIndex])) {
                        ++srcIndex;
                        length=2;
                        norm32=getNorm32FromSurrogatePair(norm32, c2);
                    } else {
                        /* c is an unpaired lead surrogate, nothing to do */
                        c2=0;
                        length=1;
                        norm32=0;
                    }
                }
                ComposePartArgs args =new ComposePartArgs();
                
                /* we are looking at the character (c, c2) at [prevSrc..src[ */
                if(nx_contains(nx, c, c2)) {
                    /* excluded: norm32==0 */
                    cc=0;
                } else if((norm32&qcMask)==0) {
                    cc=(int)((UNSIGNED_BYTE_MASK)&(norm32>>CC_SHIFT));
                } else {
                    char[] p;
    
                    /*
                     * find appropriate boundaries around this character,
                     * decompose the source text from between the boundaries,
                     * and recompose it
                     *
                     * this puts the intermediate text into the side buffer because
                     * it might be longer than the recomposition end result,
                     * or the destination buffer may be too short or missing
                     *
                     * note that destIndex may be adjusted backwards to account
                     * for source text that passed the quick check but needed to
                     * take part in the recomposition
                     */
                    int decompQCMask=(qcMask<<2)&0xf; /* decomposition quick check mask */
                    /*
                     * find the last true starter in [prevStarter..src[
                     * it is either the decomposition of the current character (at prevSrc),
                     * or prevStarter
                     */
                    if(isTrueStarter(norm32, CC_MASK|qcMask, decompQCMask)) {
                        prevStarter=prevSrc;
                    } else {
                        /* adjust destIndex: back out what had been copied with qc "yes" */
                        destIndex-=prevSrc-prevStarter;
                    }
                
                    /* find the next true starter in [src..limit[ */
                    srcIndex=findNextStarter(src, srcIndex,srcLimit, qcMask, 
                                               decompQCMask, minNoMaybe);
                    //args.prevStarter = prevStarter;
                    args.prevCC    = prevCC;                    
                    //args.destIndex = destIndex;
                    args.length = length;
                    p=composePart(args,prevStarter,src,srcIndex,srcLimit,options,nx);
                        
                    if(p==null) {
                        /* an error occurred (out of memory) */
                        break;
                    }
                    
                    prevCC      = args.prevCC;
                    length      = args.length;
                    
                    /* append the recomposed buffer contents to the destination 
                     * buffer */
                    if((destIndex+args.length)<=destLimit) {
                        int i=0;
                        while(i<args.length) {
                            dest[destIndex++]=p[i++];
                            --length;
                        }
                    } else {
                        /* buffer overflow */
                        /* keep incrementing the destIndex for preflighting */
                        destIndex+=length;
                    }
    
                    prevStarter=srcIndex;
                    continue;
                }
            }
    
            /* append the single code point (c, c2) to the destination buffer */
            if((destIndex+length)<=destLimit) {
                if(cc!=0 && cc<prevCC) {
                    /* (c, c2) is out of order with respect to the preceding 
                     * text */
                    int reorderSplit= destIndex;
                    destIndex+=length;
                    prevCC=insertOrdered(dest,reorderStartIndex, reorderSplit, 
                                         destIndex, c, c2, cc);
                } else {
                    /* just append (c, c2) */
                    dest[destIndex++]=c;
                    if(c2!=0) {
                        dest[destIndex++]=c2;
                    }
                    prevCC=cc;
                }
            } else {
                /* buffer overflow */
                /* keep incrementing the destIndex for preflighting */
                destIndex+=length;
                prevCC=cc;
            }
        }

        return destIndex - destStart;
    }
    /* make FCD --------------------------------------------------------------*/
    
    private static int/*index*/ findSafeFCD(char[] src, int start, int limit, 
                                            char fcd16) {
        char c, c2;
    
        /*
         * find the first position in [src..limit[ after some cc==0 according 
         * to FCD data
         *
         * at the beginning of the loop, we have fcd16 from before src
         *
         * stop at positions:
         * - after trail cc==0
         * - at the end of the source
         * - before lead cc==0
         */
        for(;;) {
            /* stop if trail cc==0 for the previous character */
            if((fcd16&0xff)==0) {
                break;
            }
    
            /* get c=*src - stop at end of string */
            if(start==limit) {
                break;
            }
            c=src[start];
    
            /* stop if lead cc==0 for this character */
            if(c<MIN_WITH_LEAD_CC || (fcd16=getFCD16(c))==0) {
                break; /* catches terminating NUL, too */
            }
    
            if(!UTF16.isLeadSurrogate(c)) {
                if(fcd16<=0xff) {
                    break;
                }
                ++start;
            } else if(start+1!=limit && 
                                    (UTF16.isTrailSurrogate(c2=src[start+1]))) {
                /* c is a lead surrogate, get the real fcd16 */
                fcd16=getFCD16FromSurrogatePair(fcd16, c2);
                if(fcd16<=0xff) {
                    break;
                }
                start+=2;
            } else {
                /* c is an unpaired first surrogate, lead cc==0 */
                break;
            }
        }
    
        return start;
    }
    
    private static int/*unsigned byte*/ decomposeFCD(char[] src, 
                                                     int start,int decompLimit,
                                                     char[] dest, 
                                                     int[] destIndexArr,
                                                     UnicodeSet nx) {
        char[] p=null;
        int pStart=-1;
        
        long /*unsigned int*/ norm32;
        int reorderStartIndex;
        char c, c2;
        int/*unsigned byte*/ prevCC;
        DecomposeArgs args = new DecomposeArgs();
        int destIndex = destIndexArr[0];
        /*
         * canonically decompose [src..decompLimit[
         *
         * all characters in this range have some non-zero cc,
         * directly or in decomposition,
         * so that we do not need to check in the following for quick-check 
         * limits etc.
         *
         * there _are_ _no_ Hangul syllables or Jamos in here because they are 
         * FCD-safe (cc==0)!
         *
         * we also do not need to check for c==0 because we have an established 
         * decompLimit
         */
        reorderStartIndex=destIndex;
        prevCC=0;

        while(start<decompLimit) {
            c=src[start++];
            norm32=getNorm32(c);
            if(isNorm32Regular(norm32)) {
                c2=0;
                args.length=1;
            } else {
                /*
                 * reminder: this function is called with [src..decompLimit[
                 * not containing any Hangul/Jamo characters,
                 * therefore the only specials are lead surrogates
                 */
                /* c is a lead surrogate, get the real norm32 */
                if(start!=decompLimit && UTF16.isTrailSurrogate(c2=src[start])){
                    ++start;
                    args.length=2;
                    norm32=getNorm32FromSurrogatePair(norm32, c2);
                } else {
                    c2=0;
                    args.length=1;
                    norm32=0;
                }
            }
    
            /* get the decomposition and the lead and trail cc's */
            if(nx_contains(nx, c, c2)) {
                /* excluded: norm32==0 */
                args.cc=args.trailCC=0;
                p=null;
            } else if((norm32&QC_NFD)==0) {
                /* c does not decompose */
                args.cc=args.trailCC=(int)((UNSIGNED_BYTE_MASK)&
                                                            (norm32>>CC_SHIFT));
                p=null;
            } else {
                /* c decomposes, get everything from the variable-length extra 
                 * data */
                pStart=decompose(norm32, args);
                p=extraData;
                if(args.length==1) {
                    /* fastpath a single code unit from decomposition */
                    c=p[pStart];
                    c2=0;
                    p=null;
                }
            }
    
            /* append the decomposition to the destination buffer, assume 
             * length>0 */
            if((destIndex+args.length)<=dest.length) {
                int reorderSplit=destIndex;
                if(p==null) {
                    /* fastpath: single code point */
                    if(args.cc!=0 && args.cc<prevCC) {
                        /* (c, c2) is out of order with respect to the preceding
                         *  text */
                        destIndex+=args.length;
                        args.trailCC=insertOrdered(dest,reorderStartIndex, 
                                                   reorderSplit, destIndex, 
                                                   c, c2, args.cc);
                    } else {
                        /* just append (c, c2) */
                        dest[destIndex++]=c;
                        if(c2!=0) {
                            dest[destIndex++]=c2;
                        }
                    }
                } else {
                    /* general: multiple code points (ordered by themselves) 
                     * from decomposition */
                    if(args.cc!=0 && args.cc<prevCC) {
                        /* the decomposition is out of order with respect to 
                         * the preceding text */
                        destIndex+=args.length;
                        args.trailCC=mergeOrdered(dest,reorderStartIndex, 
                                                  reorderSplit, p, pStart,
                                                  pStart+args.length);
                    } else {
                        /* just append the decomposition */
                        do {
                            dest[destIndex++]=p[pStart++];
                        } while(--args.length>0);
                    }
                }
            } else {
                /* buffer overflow */
                /* keep incrementing the destIndex for preflighting */
                destIndex+=args.length;
            }
    
            prevCC=args.trailCC;
            if(prevCC==0) {
                reorderStartIndex=destIndex;
            }
        }
        destIndexArr[0]=destIndex;
        return prevCC;
    }
    
    public static int makeFCD(char[] src,  int srcStart,  int srcLimit,
                              char[] dest, int destStart, int destLimit,
                              UnicodeSet nx) {
                           
        int prevSrc, decompStart;
        int destIndex, length;
        char c, c2;
        int /* unsigned int*/ fcd16;
        int prevCC, cc;
    
        /* initialize */
        decompStart=srcStart;
        destIndex=destStart;
        prevCC=0;
        c=0;
        fcd16=0;
        int[] destIndexArr = new int[1];
        destIndexArr[0]=destIndex;
        
        for(;;) {
            /* skip a run of code units below the minimum or with irrelevant 
             * data for the FCD check */
            prevSrc=srcStart;

            for(;;) {
                if(srcStart==srcLimit) {
                    break;
                } else if((c=src[srcStart])<MIN_WITH_LEAD_CC) {
                    prevCC=(int)-c;
                } else if((fcd16=getFCD16(c))==0) {
                    prevCC=0;
                } else {
                    break;
                }
                ++srcStart;
            }

    
            /*
             * prevCC has values from the following ranges:
             * 0..0xff - the previous trail combining class
             * <0      - the negative value of the previous code unit;
             *           that code unit was <_NORM_MIN_WITH_LEAD_CC and its 
             *           getFCD16()
             *           was deferred so that average text is checked faster
             */
    
            /* copy these code units all at once */
            if(srcStart!=prevSrc) {
                length=(int)(srcStart-prevSrc);
                if((destIndex+length)<=destLimit) {
                    System.arraycopy(src,prevSrc,dest,destIndex,length);
                }
                destIndex+=length;
                prevSrc=srcStart;
    
                /* prevCC<0 is only possible from the above loop, i.e., only if
                 *  prevSrc<src */
                if(prevCC<0) {
                    /* the previous character was <_NORM_MIN_WITH_LEAD_CC, we 
                     * need to get its trail cc */
                    if(!nx_contains(nx, (int)-prevCC)) {
                        prevCC=(int)(getFCD16((int)-prevCC)&0xff);
                    } else {
                        prevCC=0; /* excluded: fcd16==0 */
                    }
                    /*
                     * set a pointer to this below-U+0300 character;
                     * if prevCC==0 then it will moved to after this character 
                     * below
                     */
                    decompStart=prevSrc-1;
                }
            }
            /*
             * now:
             * prevSrc==src - used later to adjust destIndex before 
             *          decomposition
             * prevCC>=0
             */
    
            /* end of source reached? */
            if(srcStart==srcLimit) {
                break;
            }
    
            /* set a pointer to after the last source position where prevCC==0*/
            if(prevCC==0) {
                decompStart=prevSrc;
            }
    
            /* c already contains *src and fcd16 is set for it, increment src */
            ++srcStart;
    
            /* check one above-minimum, relevant code unit */
            if(UTF16.isLeadSurrogate(c)) {
                /* c is a lead surrogate, get the real fcd16 */
                if(srcStart!=srcLimit && 
                                     UTF16.isTrailSurrogate(c2=src[srcStart])) {
                    ++srcStart;
                    fcd16=getFCD16FromSurrogatePair((char)fcd16, c2);
                } else {
                    c2=0;
                    fcd16=0;
                }
            } else {
                c2=0;
            }
    
            /* we are looking at the character (c, c2) at [prevSrc..src[ */
            if(nx_contains(nx, c, c2)) {
                fcd16=0; /* excluded: fcd16==0 */
            }
            /* check the combining order, get the lead cc */
            cc=(int)(fcd16>>8);
            if(cc==0 || cc>=prevCC) {
                /* the order is ok */
                if(cc==0) {
                    decompStart=prevSrc;
                }
                prevCC=(int)(fcd16&0xff);
    
                /* just append (c, c2) */
                length= c2==0 ? 1 : 2;
                if((destIndex+length)<=destLimit) {
                    dest[destIndex++]=c;
                    if(c2!=0) {
                        dest[destIndex++]=c2;
                    }
                } else {
                    destIndex+=length;
                }
            } else {
                /*
                 * back out the part of the source that we copied already but
                 * is now going to be decomposed;
                 * prevSrc is set to after what was copied
                 */
                destIndex-=(int)(prevSrc-decompStart);
    
                /*
                 * find the part of the source that needs to be decomposed;
                 * to be safe and simple, decompose to before the next character
                 * with lead cc==0
                 */
                srcStart=findSafeFCD(src,srcStart, srcLimit, (char)fcd16);
    
                /*
                 * the source text does not fulfill the conditions for FCD;
                 * decompose and reorder a limited piece of the text
                 */
                destIndexArr[0] = destIndex;
                prevCC=decomposeFCD(src,decompStart, srcStart,dest, 
                                    destIndexArr,nx);
                decompStart=srcStart;
                destIndex=destIndexArr[0];
            }
        }
        
        return destIndex - destStart;
    
    }

    public static int getCombiningClass(int c) {
        long norm32;
        norm32=getNorm32(c);
        return (char)((norm32>>CC_SHIFT)&0xFF);
    }
    
    public static boolean isFullCompositionExclusion(int c) {
        if(isFormatVersion_2_1) {
            int aux =AuxTrieImpl.auxTrie.getCodePointValue(c);
            return (boolean)((aux & AUX_COMP_EX_MASK)!=0);
        } else {
            return false;
        }
    }
    
    public static boolean isCanonSafeStart(int c) {
        if(isFormatVersion_2_1) {
            int aux = AuxTrieImpl.auxTrie.getCodePointValue(c);
            return (boolean)((aux & AUX_UNSAFE_MASK)==0);
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
                        fillSet.setToOne((int)i);
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


    /* Is c an NF<mode>-skippable code point? See unormimp.h. */
    public static boolean isNFSkippable(int c, Normalizer.Mode mode, long mask) {
        long /*unsigned int*/ norm32;
        mask = mask & UNSIGNED_INT_MASK;
        char aux;
   
        /* check conditions (a)..(e), see unormimp.h */
        norm32 = getNorm32(c);

        if((norm32&mask)!=0) {
            return false; /* fails (a)..(e), not skippable */
        }
    
        if(mode == Normalizer.NFD || mode == Normalizer.NFKD || mode == Normalizer.NONE){
            return true; /* NF*D, passed (a)..(c), is skippable */
        }
        /* check conditions (a)..(e), see unormimp.h */

        /* NF*C/FCC, passed (a)..(e) */
        if((norm32& QC_NFD)==0) {
            return true; /* no canonical decomposition, is skippable */
        }
    
        /* check Hangul syllables algorithmically */
        if(isNorm32HangulOrJamo(norm32)) {
            /* Jamo passed (a)..(e) above, must be Hangul */
            return !isHangulWithoutJamoT((char)c); /* LVT are skippable, LV are not */
        }
    
        /* if(mode<=UNORM_NFKC) { -- enable when implementing FCC */
        /* NF*C, test (f) flag */
        if(!isFormatVersion_2_2) {
            return false; /* no (f) data, say not skippable to be safe */
        }
    

        aux = AuxTrieImpl.auxTrie.getCodePointValue(c);
        return (aux&AUX_NFC_SKIP_F_MASK)==0; /* TRUE=skippable if the (f) flag is not set */
    
        /* } else { FCC, test fcd<=1 instead of the above } */
    }
    
    /*
        private static final boolean
    _enumPropertyStartsRange(const void *context, UChar32 start, UChar32 limit, uint32_t value) {
        // add the start code point to the USet 
        uset_add((USet *)context, start);
        return TRUE;
    }
    */

    public static UnicodeSet addPropertyStarts(UnicodeSet set) {
        int c;
       
        /* add the start code point of each same-value range of each trie */
        //utrie_enum(&normTrie, NULL, _enumPropertyStartsRange, set);
        TrieIterator normIter = new TrieIterator(NormTrieImpl.normTrie);
        RangeValueIterator.Element normResult = new RangeValueIterator.Element();
        
        while(normIter.next(normResult)){
            set.add(normResult.start);
        }
        
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

    /**
     * Internal API, used in UCharacter.getIntPropertyValue().
     * @internal
     * @param c code point
     * @param modeValue numeric value compatible with Mode
     * @return numeric value compatible with QuickCheck
     */
    public static final int quickCheck(int c, int modeValue) {
        final int qcMask[/*UNORM_MODE_COUNT*/]={
            0, 0, QC_NFD, QC_NFKD, QC_NFC, QC_NFKC
        };

        int norm32=(int)getNorm32(c)&qcMask[modeValue];

        if(norm32==0) {
            return 1; // YES
        } else if((norm32&QC_ANY_NO)!=0) {
            return 0; // NO
        } else /* _NORM_QC_ANY_MAYBE */ {
            return 2; // MAYBE;
        }
    }

    /**
     * Internal API, used by collation code.
     * Get access to the internal FCD trie table to be able to perform
     * incremental, per-code unit, FCD checks in collation.
     * One pointer is sufficient because the trie index values are offset
     * by the index size, so that the same pointer is used to access the trie 
     * data.
     * @internal
     */
    ///CLOVER:OFF
    public CharTrie getFCDTrie(){
        return FCDTrieImpl.fcdTrie;
    }
    ///CLOVER:ON


    
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
        
        long norm32;
        int length=0;
        norm32 = (long) ((UNSIGNED_INT_MASK) & NormTrieImpl.normTrie.getCodePointValue(c));
        if((norm32 & QC_NFD)!=0) {
            if(isNorm32HangulOrJamo(norm32)) {
                /* Hangul syllable: decompose algorithmically */
                char c2;
    
                c-=HANGUL_BASE;
    
                c2=(char)(c%JAMO_T_COUNT);
                c/=JAMO_T_COUNT;
                if(c2>0) {
                    buffer[2]=(char)(JAMO_T_BASE+c2);
                    length=3;
                } else {
                    length=2;
                }
                buffer[1]=(char)(JAMO_V_BASE+c%JAMO_V_COUNT);
                buffer[0]=(char)(JAMO_L_BASE+c/JAMO_V_COUNT);
                return length;
            } else {
                /* normal decomposition */
                DecomposeArgs  args = new DecomposeArgs();
                int index = decompose(norm32, args);
                System.arraycopy(extraData,index,buffer,0,args.length);
                return args.length ;
            }
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
                   (UTF16.isLeadSurrogate((char)s1.charAt(i-1)) ||
                    UTF16.isLeadSurrogate((char)s2.charAt(j-1)))) {
                    // Current codepoint may be the low surrogate pair.
                    return cmpEquivFold(s1.toCharArray(),i-1,s1.length(),
                            s2.toCharArray(),j-1,s2.length(),
                            options);
                }
                else if (UTF16.isLeadSurrogate((char)s1.charAt(i))||
                         UTF16.isLeadSurrogate((char)s2.charAt(j))) {
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
    private static int strCompare(char[] s1, int s1Start, int s1Limit,
                                  char[] s2, int s2Start, int s2Limit,
                                  boolean codePointOrder) {
                        
        int start1, start2, limit1, limit2;
 
        char c1, c2;
    
        /* setup for fix-up */
        start1=s1Start;
        start2=s2Start;
        
        int length1, length2;
        
        length1 = s1Limit - s1Start;
        length2 = s2Limit - s2Start;
            
        int lengthResult;

        if(length1<length2) {
            lengthResult=-1;
            limit1=start1+length1;
        } else if(length1==length2) {
            lengthResult=0;
            limit1=start1+length1;
        } else /* length1>length2 */ {
            lengthResult=1;
            limit1=start1+length2;
        }

        if(s1==s2) {
            return lengthResult;
        }

        for(;;) {
            /* check pseudo-limit */
            if(s1Start==limit1) {
                return lengthResult;
            }

            c1=s1[s1Start];
            c2=s2[s2Start];
            if(c1!=c2) {
                break;
            }
            ++s1Start;
            ++s2Start;
        }

        /* setup for fix-up */
        limit1=start1+length1;
        limit2=start2+length2;

    
        /* if both values are in or above the surrogate range, fix them up */
        if(c1>=0xd800 && c2>=0xd800 && codePointOrder) {
            /* subtract 0x2800 from BMP code points to make them smaller than
             *  supplementary ones */
            if(
                ( c1<=0xdbff && (s1Start+1)!=limit1 && 
                  UTF16.isTrailSurrogate(s1[(s1Start+1)])
                ) ||
                ( UTF16.isTrailSurrogate(c1) && start1!=s1Start && 
                  UTF16.isLeadSurrogate(s1[(s1Start-1)])
                )
            ) {
                /* part of a surrogate pair, leave >=d800 */
            } else {
                /* BMP code point - may be surrogate code point - make <d800 */
                c1-=0x2800;
            }
    
            if(
                ( c2<=0xdbff && (s2Start+1)!=limit2 && 
                  UTF16.isTrailSurrogate(s2[(s2Start+1)])
                ) ||
                ( UTF16.isTrailSurrogate(c2) && start2!=s2Start && 
                  UTF16.isLeadSurrogate(s2[(s2Start-1)])
                )
            ) {
                /* part of a surrogate pair, leave >=d800 */
            } else {
                /* BMP code point - may be surrogate code point - make <d800 */
                c2-=0x2800;
            }
        }
    
        /* now c1 and c2 are in UTF-32-compatible order */
        return (int)c1-(int)c2;
    }


    /*
     * Status of tailored normalization
     *
     * This was done initially for investigation on Unicode public review issue 7
     * (http://www.unicode.org/review/). See Jitterbug 2481.
     * While the UTC at meeting #94 (2003mar) did not take up the issue, this is
     * a permanent feature in ICU 2.6 in support of IDNA which requires true
     * Unicode 3.2 normalization.
     * (NormalizationCorrections are rolled into IDNA mapping tables.)
     *
     * Tailored normalization as implemented here allows to "normalize less"
     * than full Unicode normalization would.
     * Based internally on a UnicodeSet of code points that are
     * "excluded from normalization", the normalization functions leave those
     * code points alone ("inert"). This means that tailored normalization
     * still transforms text into a canonically equivalent form.
     * It does not add decompositions to code points that do not have any or
     * change decomposition results.
     *
     * Any function that searches for a safe boundary has not been touched,
     * which means that these functions will be over-pessimistic when
     * exclusions are applied.
     * This should not matter because subsequent checks and normalizations
     * do apply the exclusions; only a little more of the text may be processed
     * than necessary under exclusions.
     *
     * Normalization exclusions have the following effect on excluded code points c:
     * - c is not decomposed
     * - c is not a composition target
     * - c does not combine forward or backward for composition
     *   except that this is not implemented for Jamo
     * - c is treated as having a combining class of 0
     */
     
    /* 
     * Constants for the bit fields in the options bit set parameter. 
     * These need not be public. 
     * A user only needs to know the currently assigned values. 
     * The number and positions of reserved bits per field can remain private. 
     */ 
    private static final int OPTIONS_NX_MASK=0x1f;
    private static final int OPTIONS_UNICODE_MASK=0xe0; 
    public  static final int OPTIONS_SETS_MASK=0xff;
    //private static final int OPTIONS_UNICODE_SHIFT=5;
    private static final UnicodeSet[] nxCache = new UnicodeSet[OPTIONS_SETS_MASK+1];
     
    /* Constants for options flags for normalization.*/

    /** 
     * Options bit 0, do not decompose Hangul syllables. 
     */
    private static final int NX_HANGUL = 1;
    /** 
     * Options bit 1, do not decompose CJK compatibility characters.
     */
    private static final int NX_CJK_COMPAT=2;
    /**
     * Options bit 8, use buggy recomposition described in
     * Unicode Public Review Issue #29
     * at http://www.unicode.org/review/resolved-pri.html#pri29
     *
     * Used in IDNA implementation according to strict interpretation
     * of IDNA definition based on Unicode 3.2 which predates PRI #29.
     *
     * See ICU4C unormimp.h
     */
    public static final int BEFORE_PRI_29=0x100;

    /*
     * The following options are used only in some composition functions.
     * They use bits 12 and up to preserve lower bits for the available options
     * space in unorm_compare() -
     * see documentation for UNORM_COMPARE_NORM_OPTIONS_SHIFT.
     */

    /** Options bit 12, for compatibility vs. canonical decomposition. */
    public static final int OPTIONS_COMPAT=0x1000;
    /** Options bit 13, no discontiguous composition (FCC vs. NFC). */
    public static final int OPTIONS_COMPOSE_CONTIGUOUS=0x2000;

    /* normalization exclusion sets --------------------------------------------- */
    
    /*
     * Normalization exclusion UnicodeSets are used for tailored normalization;
     * see the comment near the beginning of this file.
     *
     * By specifying one or several sets of code points,
     * those code points become inert for normalization.
     */
    private static final synchronized UnicodeSet internalGetNXHangul() {
        /* internal function, does not check for incoming U_FAILURE */
    
        if(nxCache[NX_HANGUL]==null) {
             nxCache[NX_HANGUL]=new UnicodeSet(0xac00, 0xd7a3);
        }
        return nxCache[NX_HANGUL];
    }
    
    private static final synchronized UnicodeSet internalGetNXCJKCompat() {
        /* internal function, does not check for incoming U_FAILURE */
    
        if(nxCache[NX_CJK_COMPAT]==null) {

            /* build a set from [CJK Ideographs]&[has canonical decomposition] */
            UnicodeSet set, hasDecomp;
    
            set=new UnicodeSet("[:Ideographic:]");
    
            /* start with an empty set for [has canonical decomposition] */
            hasDecomp=new UnicodeSet();
    
            /* iterate over all ideographs and remember which canonically decompose */
            UnicodeSetIterator it = new UnicodeSetIterator(set);
            int start, end;
            long norm32;
    
            while(it.nextRange() && (it.codepoint != UnicodeSetIterator.IS_STRING)) {
                start=it.codepoint;
                end=it.codepointEnd;
                while(start<=end) {
                    norm32 = getNorm32(start);
                    if((norm32 & QC_NFD)>0) {
                        hasDecomp.add(start);
                    }
                    ++start;
                }
            }
    
            /* hasDecomp now contains all ideographs that decompose canonically */
             nxCache[NX_CJK_COMPAT]=hasDecomp;
         
        }
    
        return nxCache[NX_CJK_COMPAT];
    }
    
    private static final synchronized UnicodeSet internalGetNXUnicode(int options) {
        options &= OPTIONS_UNICODE_MASK;
        if(options==0) {
            return null;
        }
    
        if(nxCache[options]==null) {
            /* build a set with all code points that were not designated by the specified Unicode version */
            UnicodeSet set = new UnicodeSet();

            switch(options) {
            case Normalizer.UNICODE_3_2:
                set.applyPattern("[:^Age=3.2:]");
                break;
            default:
                return null;
            }
            
            nxCache[options]=set;
        }
    
        return nxCache[options];
    }
    
    /* Get a decomposition exclusion set. The data must be loaded. */
    private static final synchronized UnicodeSet internalGetNX(int options) {
        options&=OPTIONS_SETS_MASK;
    
        if(nxCache[options]==null) {
            /* return basic sets */            
            if(options==NX_HANGUL) {
                return internalGetNXHangul();
            }
            if(options==NX_CJK_COMPAT) {
                return internalGetNXCJKCompat();
            }
            if((options & OPTIONS_UNICODE_MASK)!=0 && (options & OPTIONS_NX_MASK)==0) {
                return internalGetNXUnicode(options);
            }
    
            /* build a set from multiple subsets */
            UnicodeSet set;
            UnicodeSet other;
    
            set=new UnicodeSet();

    
            if((options & NX_HANGUL)!=0 && null!=(other=internalGetNXHangul())) {
                set.addAll(other);
            }
            if((options&NX_CJK_COMPAT)!=0 && null!=(other=internalGetNXCJKCompat())) {
                set.addAll(other);
            }
            if((options&OPTIONS_UNICODE_MASK)!=0 && null!=(other=internalGetNXUnicode(options))) {
                set.addAll(other);
            }

               nxCache[options]=set;
        }
        return nxCache[options];
    }
    
    public static final UnicodeSet getNX(int options) {
        if((options&=OPTIONS_SETS_MASK)==0) {
            /* incoming failure, or no decomposition exclusions requested */
            return null;
        } else {
            return internalGetNX(options);
        }
    }
    
    private static final boolean nx_contains(UnicodeSet nx, int c) {
        return nx!=null && nx.contains(c);
    }
    
    private static final boolean nx_contains(UnicodeSet nx, char c, char c2) {
        return nx!=null && nx.contains(c2==0 ? c : UCharacterProperty.getRawSupplementary(c, c2));
    }


}
