/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/NormalizerImpl.java,v $
 * $Date: 2002/03/28 01:50:59 $
 * $Revision: 1.4 $
 *******************************************************************************
 */
 
package com.ibm.icu.impl;
import java.io.*;
//import com.ibm.icu.text.NewNormalizer;
import com.ibm.icu.text.UTF16;	
/**
 * @version 	1.0
 * @author  Ram Viswanadha
 */
public final class NormalizerImpl {
	/* Static block for the class to initialize its own self */
	static NormalizerImpl IMPL=null;
	
	static
    {
        try
        {
            IMPL = new NormalizerImpl();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }
	
	static final int UNSIGNED_BYTE_MASK =0xFF;
	/*
	 * This new implementation of the normalization code loads its data from
	 * unorm.dat, which is generated with the gennorm tool.
	 * The format of that file is described at the end of this file.
	 */
	private static final String DATA_FILE_NAME_ = "data/unorm.dat";
	
	/* norm32 value constants */
	
    /* quick check flags 0..3 set mean "no" for their forms */
    static final int QC_NFC=0x11;          /* no|maybe */
    static final int QC_NFKC=0x22;         /* no|maybe */
    static final int QC_NFD=4;             /* no */
    static final int QC_NFKD=8;            /* no */
	
    static final int QC_ANY_NO=0xf;

    /* quick check flags 4..5 mean "maybe" for their forms; test flags>=QC_MAYBE */
    static final int QC_MAYBE=0x10;
    static final int QC_ANY_MAYBE=0x30;

    static final int QC_MASK=0x3f;

    static final int COMBINES_FWD=0x40;
    static final int COMBINES_BACK=0x80;
    static final int COMBINES_ANY=0xc0;

    static final int CC_SHIFT=8;           		   /* UnicodeData.txt combining class in bits 15..8 */
    static final int CC_MASK=0xff00;

    static final int EXTRA_SHIFT=16;               /* 16 bits for the index to UChars and other extra data */
    static final int EXTRA_INDEX_TOP=0xfc00;       /* start of surrogate specials after shift */

    static final int EXTRA_SURROGATE_MASK=0x3ff;
    static final int EXTRA_SURROGATE_TOP=0x3f0;    /* hangul etc. */

    static final int EXTRA_HANGUL=EXTRA_SURROGATE_TOP;
    static final int EXTRA_JAMO_L=EXTRA_SURROGATE_TOP+1;                 /* ### not used */
    static final int EXTRA_JAMO_V=EXTRA_SURROGATE_TOP+2;
    static final int EXTRA_JAMO_T=EXTRA_SURROGATE_TOP+3;
	
	/* norm32 value constants using >16 bits */
	static final int  UNSIGNED_INT_MASK = 0x7fffffff;
	static final int  MIN_SPECIAL     =  0xfc000000 & UNSIGNED_INT_MASK;
	static final int  SURROGATES_TOP  =  0xfff00000 & UNSIGNED_INT_MASK;
	static final int  MIN_HANGUL      =  0xfff00000 & UNSIGNED_INT_MASK;
	static final int  MIN_JAMO_V      =  0xfff20000 & UNSIGNED_INT_MASK;
	static final int  JAMO_V_TOP      =  0xfff30000 & UNSIGNED_INT_MASK;
	
	
	/* indexes[] value names */
	
	static final int INDEX_TRIE_SIZE 		  = 0;     /* number of bytes in normalization trie */
	static final int INDEX_CHAR_COUNT 		  = 1;     /* number of chars in extra data */
	
	static final int INDEX_COMBINE_DATA_COUNT = 2;     /* number of uint16_t words for combining data */
	static final int INDEX_COMBINE_FWD_COUNT  = 3;     /* number of code points that combine forward */
	static final int INDEX_COMBINE_BOTH_COUNT = 4;     /* number of code points that combine forward and backward */
	static final int INDEX_COMBINE_BACK_COUNT = 5;     /* number of code points that combine backward */
	
	static final int INDEX_MIN_NFC_NO_MAYBE   = 6;     /* first code point with quick check NFC NO/MAYBE */
	static final int INDEX_MIN_NFKC_NO_MAYBE  = 7;     /* first code point with quick check NFKC NO/MAYBE */
	static final int INDEX_MIN_NFD_NO_MAYBE   = 8;     /* first code point with quick check NFD NO/MAYBE */
	static final int INDEX_MIN_NFKD_NO_MAYBE  = 9;     /* first code point with quick check NFKD NO/MAYBE */
	
	static final int INDEX_FCD_TRIE_SIZE      = 10;    /* number of bytes in FCD trie */
    static final int INDEX_AUX_TRIE_SIZE      = 11;    /* number of bytes in the auxiliary trie */
    static final int INDEX_CANON_SET_COUNT    = 12;    /* number of uint16_t in the array of serialized USet */

	static final int INDEX_TOP                = 32;    /* changing this requires a new formatVersion */
	
	
	/* AUX constants */
	/* value constants for auxTrie */	
	static final int AUX_UNSAFE_SHIFT	= 11;
	static final int AUX_COMP_EX_SHIFT	= 10;
	
	static final int AUX_MAX_FNC        =   ((int)1<<AUX_COMP_EX_SHIFT);
	static final int AUX_UNSAFE_MASK    =   (1<<AUX_UNSAFE_SHIFT) & UNSIGNED_INT_MASK;
	static final int AUX_FNC_MASK       =   (AUX_MAX_FNC-1) & UNSIGNED_INT_MASK;
	static final int AUX_COMP_EX_MASK   =   (1<<AUX_COMP_EX_SHIFT) & UNSIGNED_INT_MASK;
	
	/* canonStartSets[0..31] contains indexes for what is in the array */
    static final int SET_INDEX_CANON_SETS_LENGTH		= 0; /* number of uint16_t in canonical starter sets */
    static final int SET_INDEX_CANON_BMP_TABLE_LENGTH	= 1; /* number of uint16_t in the BMP search table (contains pairs) */
    static final int SET_INDEX_CANON_SUPP_TABLE_LENGTH  = 2; /* number of uint16_t in the supplementary search table (contains triplets) */
    static final int SET_INDEX_TOP						= 32;/* changing this requires a new formatVersion */
	
	static final int CANON_SET_INDICIES_INDEX  			= 0;
	static final int CANON_SET_START_SETS_INDEX			= 1;
	static final int CANON_SET_BMP_TABLE_INDEX			= 2;
	static final int CANON_SET_SUPP_TABLE_INDEX			= 3;
	
	static final int CANON_SET_MAX_CANON_SETS     		= 0x0004; /* 14 bit indexes to canonical USerializedSets */
	/* single-code point BMP sets are encoded directly in the search table except if result=0x4000..0x7fff */
	static final int CANON_SET_BMP_MASK        			= 0xc000;
	static final int CANON_SET_BMP_IS_INDEX    			= 0x4000;
	
	/*******************************/
	
	/* Wrappers for Trie implementations */ 
	static final class NormTrieImpl implements Trie.DataManipulate{
		static IntTrie normTrie= null;
	   /**
	    * Called by com.ibm.icu.util.Trie to extract from a lead surrogate's 
	    * data the index array offset of the indexes for that lead surrogate.
	    * @param property data value for a surrogate from the trie, including the
	    *        folding offset
	    * @return data offset or 0 if there is no data for the lead surrogate
	    */
	    public int getFoldingOffset(int value){
	    	
	    	return 0x10000>>5+((value>>(EXTRA_SHIFT-5))&(0x3ff<<5));

	    }
		
	}
	static final class FCDTrieImpl implements Trie.DataManipulate{
		static CharTrie fcdTrie=null;
	   /**
	    * Called by com.ibm.icu.util.Trie to extract from a lead surrogate's 
	    * data the index array offset of the indexes for that lead surrogate.
	    * @param property data value for a surrogate from the trie, including the
	    *        folding offset
	    * @return data offset or 0 if there is no data for the lead surrogate
	    */

	    public int getFoldingOffset(int value){
			return 0;
	    }
	}
	
	static final class AuxTrieImpl implements Trie.DataManipulate{
		static CharTrie auxTrie = null;
	   /**
	    * Called by com.ibm.icu.util.Trie to extract from a lead surrogate's 
	    * data the index array offset of the indexes for that lead surrogate.
	    * @param property data value for a surrogate from the trie, including the
	    *        folding offset
	    * @return data offset or 0 if there is no data for the lead surrogate
	    */
	    public int getFoldingOffset(int value){
	        return (value&AUX_FNC_MASK)<<5;
	    }
	}
		 
	/****************************************************/
	
	
	static FCDTrieImpl fcdTrieImpl;
	static NormTrieImpl normTrieImpl;
	static AuxTrieImpl auxTrieImpl;
	static int[] indexes;
	static char[] combiningTable;
	static char[] extraData;
	static Object[] canonStartSets;
	
	static boolean isDataLoaded;
	static boolean isFormatVersion_2_1;
	/**
    * Default buffer size of datafile
    */
    private static final int DATA_BUFFER_SIZE_ = 25000;
	
	/* FCD check: everything below this code point is known to have a 0 lead combining class */
	public static final int MIN_WITH_LEAD_CC=0x300;


    /**
     * Bit 7 of the length byte for a decomposition string in extra data is
     * a flag indicating whether the decomposition string is
     * preceded by a 16-bit word with the leading and trailing cc
     * of the decomposition (like for A-umlaut);
     * if not, then both cc's are zero (like for compatibility ideographs).
     */
	static final int DECOMP_FLAG_LENGTH_HAS_CC=0x80;
    /**
	 * Bits 6..0 of the length byte contain the actual length.
	 */
	static final int DECOMP_LENGTH_MASK=0x7f;   
	
	/* -------------------------------------------------------------------------- */
	
	/* Korean Hangul and Jamo constants */
	
	private static final int JAMO_L_BASE=0x1100;     /* "lead" jamo */
	private static final int JAMO_V_BASE=0x1161;     /* "vowel" jamo */
	private static final int JAMO_T_BASE=0x11a7;     /* "trail" jamo */
	
	private static final int HANGUL_BASE=0xac00;
	
	private static final int JAMO_L_COUNT=19;
	private static final int JAMO_V_COUNT=21;
	private static final int JAMO_T_COUNT=28;
	private static final int HANGUL_COUNT=JAMO_L_COUNT*JAMO_V_COUNT*JAMO_T_COUNT;
	
	private static boolean isHangulWithoutJamoT(char c) {
	    c-=HANGUL_BASE;
	    return c<HANGUL_COUNT && c%JAMO_T_COUNT==0;
	}
	
	/* norm32 helpers */
	
	/* is this a norm32 with a regular index? */
	private static boolean isNorm32Regular(int norm32) {
	    return norm32<MIN_SPECIAL;
	}
	
	/* is this a norm32 with a special index for a lead surrogate? */
	private static boolean isNorm32LeadSurrogate(int norm32) {
	    return MIN_SPECIAL<=norm32 && norm32<SURROGATES_TOP;
	}
	
	/* is this a norm32 with a special index for a Hangul syllable or a Jamo? */
	private static boolean isNorm32HangulOrJamo(int norm32) {
	    return norm32>=MIN_HANGUL;
	}
	
	/*
	 * Given isNorm32HangulOrJamo(),
	 * is this a Hangul syllable or a Jamo?
	 */
	private static  boolean isHangulJamoNorm32HangulOrJamoL(int norm32) {
	    return norm32<MIN_JAMO_V;
	}
	
	/*
	 * Given norm32 for Jamo V or T,
	 * is this a Jamo V?
	 */
	private static boolean isJamoVTNorm32JamoV(int norm32) {
	    return norm32<JAMO_V_TOP;
	}
	
	static int getExtraDataIndex(int norm32) {
	    return (norm32>>EXTRA_SHIFT);
	}
	

    // protected constructor ---------------------------------------------
  
    /**
    * Constructor
    * @exception thrown when data reading fails or data corrupted
    */
    private NormalizerImpl() throws IOException{
    	//data should be loaded only once
    	if(!isDataLoaded){
    		indexes = null;
    		combiningTable=null;
    		extraData=null;
    		fcdTrieImpl = new FCDTrieImpl();
			normTrieImpl = new NormTrieImpl();
			auxTrieImpl = new AuxTrieImpl();
	        // jar access
	        InputStream i = getClass().getResourceAsStream(DATA_FILE_NAME_);
	        BufferedInputStream b = new BufferedInputStream(i, 
	                                                        DATA_BUFFER_SIZE_);
	        NormalizerDataReader reader = new NormalizerDataReader(b);
	        reader.read(this);
	        b.close();
	        i.close();
    	}
    }
  
    public static boolean checkFCD(char[] src) {

	    char fcd16,c;
	    int prevCC=0, cc;
		int i =0, length = src.length;
	
	    for(;;) {
            for(;;) {
                if(i==length) {
                    return true;
                } else if((c=src[i++])<MIN_WITH_LEAD_CC) {
                    prevCC=(int)-c;
                } else if((fcd16=fcdTrieImpl.fcdTrie.getBMPValue(c))==0) {
                    prevCC=0;
                } else {
                    break;
                }
            }

	        /* check one above-minimum, relevant code unit */
	        if(UTF16.isLeadSurrogate(c)) {
	            /* c is a lead surrogate, get the real fcd16 */
	            if(i!=length && UTF16.isTrailSurrogate(src[i])) {
	                ++i;
	                fcd16=fcdTrieImpl.fcdTrie.getSurrogateValue(fcd16, src[i]);
	            } else {
	                fcd16=0;
	            }
	        }
	
	        /*
	         * prevCC has values from the following ranges:
	         * 0..0xff - the previous trail combining class
	         * <0      - the negative value of the previous code unit;
	         *           that code unit was <_NORM_MIN_WITH_LEAD_CC and its _getFCD16()
	         *           was deferred so that average text is checked faster
	         */
	
	        /* check the combining order */
	        cc=(int)(fcd16>>8);
	        if(cc!=0) {
	            if(prevCC<0) {
	                /* the previous character was <_NORM_MIN_WITH_LEAD_CC, we need to get its trail cc */
	                prevCC=(int)(fcdTrieImpl.fcdTrie.getBMPValue((char)-prevCC)&0xff);
	            }
	
	            if(cc<prevCC) {
	                return false;
	            }
	        }
	        prevCC=(int)(fcd16&0xff);
	    }
	}
	/*
	public static NewNormalizer.QuickCheckResult quickCheck(char[] src,NewNormalizer.Mode mode) {
	    
	    int norm32, ccOrQCMask, qcMask;
	    char c, c2, minNoMaybe;
	    char cc, prevCC;
	    NewNormalizer.QuickCheckResult result;
		
		

	    if(!isDataLoaded) {
	        return NewNormalizer.MAYBE;
	    }
	
	    // check for a valid mode and set the quick check minimum and mask  

	   	if(mode.equals(NewNormalizer.NFC)){
	        minNoMaybe=(char)indexes[INDEX_MIN_NFC_NO_MAYBE];
	        qcMask=QC_NFC;
		}else if(mode.equals(NewNormalizer.NFKC)){
	        minNoMaybe=(char)indexes[INDEX_MIN_NFKC_NO_MAYBE];
	        qcMask=QC_NFKC;
	   	}else if(mode.equals(NewNormalizer.NFD)){
	        minNoMaybe=(char)indexes[INDEX_MIN_NFD_NO_MAYBE];
	        qcMask=QC_NFD;
	   	}else if(mode.equals(NewNormalizer.NFKD)){
	        minNoMaybe=(char)indexes[INDEX_MIN_NFKD_NO_MAYBE];
	        qcMask=QC_NFKD;
	   	}else if(mode.equals(NewNormalizer.FCD)){
	        return (checkFCD(src)) ? NewNormalizer.YES : NewNormalizer.NO;
	   	}else{
	        return NewNormalizer.MAYBE;
	    }
	
	    // initialize 
	    ccOrQCMask=CC_MASK|qcMask;
	    result=NewNormalizer.YES;
	    prevCC=0;
		int i=0;
	
	    for(;;) {
            for(;;) {
                if(i==src.length) {
                    return result;
                } else if((c=src[i++])>=minNoMaybe && ((norm32=normTrieImpl.normTrie.getBMPValue(c))&ccOrQCMask)!=0) {
                  	break;
                }
                prevCC=0;
            }
            
	
	        //* check one above-minimum, relevant code unit 
	        if(isNorm32LeadSurrogate(norm32)) {
	            //* c is a lead surrogate, get the real norm32 
	            if(i!=src.length && UTF16.isTrailSurrogate(c2=src[i])) {
	                ++i;
	                norm32=normTrieImpl.normTrie.getRawOffset(norm32, c2);
	            } else {
	                norm32=0;
	            }
	        }
	
	        //* check the combining order 
	        cc=(char)((norm32>>CC_SHIFT)&0xFF);
	        if(cc!=0 && cc<prevCC) {
	            return NewNormalizer.NO;
	        }
	        prevCC=cc;
	
	        //* check for "no" or "maybe" quick check flags 
	        norm32&=qcMask;
	        if((norm32& QC_ANY_NO)>=1) {
	            return NewNormalizer.NO;
	        } else if(norm32!=0) {
	            result=NewNormalizer.MAYBE;
	        }
	    }
	} */
	
	public static int getCombiningClass(int c) {
	    int norm32;
        if(c<=0xffff) {
            norm32=normTrieImpl.normTrie.getBMPValue((char)c);
        } else {
            norm32=normTrieImpl.normTrie.getBMPValue(UTF16.getLeadSurrogate(c));
            if((norm32&CC_MASK)!=0) {
                norm32=normTrieImpl.normTrie.getRawOffset(norm32, UTF16.getTrailSurrogate(c));
            }
        }
        return (char)((norm32>>CC_SHIFT)&0xFF);
	}
	
	public static boolean isFullCompositionExclusion(int c) {
	    if(isFormatVersion_2_1) {
	        int aux =auxTrieImpl.auxTrie.getCodePointValue(c);
	        return (boolean)((aux & AUX_COMP_EX_MASK)!=0);
	    } else {
	        return false;
	    }
	}
	
	public static boolean isCanonSafeStart(int c) {
	    if(isFormatVersion_2_1) {
	        int aux = auxTrieImpl.auxTrie.getCodePointValue(c);
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
	        int i, start, limit;
	        
	        if(c<=0xffff) {
	            table=(char[]) canonStartSets[CANON_SET_BMP_TABLE_INDEX];
	            start=0;
	            limit=table.length;
	
	            /* each entry is a pair { c, result } */
	            while(start<limit) {
	                i=(char)((start+limit)/2); 
	                if(c<table[i]) {
	                    limit=i;
	                } else {
	                    start=i;
	                }
	            }
	
	            /* found? */
	            if(c==table[start]) {
	                i=table[start+1];
	                if((i&CANON_SET_BMP_MASK)==CANON_SET_BMP_IS_INDEX) {
	                    /* result 01xxxxxx xxxxxx contains index x to a USerializedSet */
	                    i&=(CANON_SET_MAX_CANON_SETS-1);
	                    return fillSet.getSet(table,i);
	                } else {
	                    /* other result values are BMP code points for single-code point sets */
	                    fillSet.setSerializedToOne(i);
	                    return true;
	                }
	            }
	        } else {
	            char high, low, h;
	
	            table=(char[]) canonStartSets[CANON_SET_SUPP_TABLE_INDEX];
	            start=0;
	            limit=table.length;
	
	            high=(char)(c>>16);
	            low=(char)c;
	
	            /* each entry is a triplet { high(c), low(c), result } */
	            while(start<limit-3) {
	                i=(char)(((start+limit)/6)*3); /* (start+limit)/2 and address triplets */
	                h=(char)(table[i]&0x1f); /* high word */
	                if(high<h || (high==h && low<table[i+1])) {
	                    limit=i;
	                } else {
	                    start=i;
	                }
	            }
	
	            /* found? */
	            h=table[start];
	            if(high==(h&0x1f) && low==table[start+1]) {
	                i=table[start+2];
	                if((h&0x8000)==0) {
	                    /* the result is an index to a USerializedSet */
	                    return fillSet.getSet(table,i);
	                } else {
	                    /*
	                     * single-code point set {x} in
	                     * triplet { 100xxxxx 000hhhhh  llllllll llllllll  xxxxxxxx xxxxxxxx }
	                     */
	                    i|=((int)h&0x1f00)<<8; /* add high bits from high(c) */
	                    fillSet.setSerializedToOne((int)i);
	                    return true;
	                }
	            }
	        }
	    }
	
	    return false; /* not found */
	}
	
	/**
	 * Internal API, used by collation code.
	 * Get access to the internal FCD trie table to be able to perform
	 * incremental, per-code unit, FCD checks in collation.
	 * One pointer is sufficient because the trie index values are offset
	 * by the index size, so that the same pointer is used to access the trie data.
	 * @internal
	 */
	public CharTrie getFCDTrie(){
		return fcdTrieImpl.fcdTrie;
	}

}
