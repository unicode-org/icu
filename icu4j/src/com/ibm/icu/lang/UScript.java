/**
*******************************************************************************
* Copyright (C) 2001, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.lang;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.impl.UCharacterProperty;

import java.util.ResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * A class to reflect UTR #24: Script Names
 * (based on ISO 15924:2000, "Code for the representation of names of
 * scripts").  UTR #24 describes the basis for a new Unicode data file,
 * Scripts.txt.
 */
public final class UScript {
    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a92001 IBM Corp.  All rights reserved.";
        
    public static final int INVALID_CODE = -1;
    public static final int COMMON       =  0;  /* Zyyy */
    public static final int INHERITED    =  1;  /* Qaai */
    public static final int ARABIC       =  2;  /* Arab */
    public static final int ARMENIAN     =  3;  /* Armn */
    public static final int BENGALI      =  4;  /* Beng */
    public static final int BOPOMOFO     =  5;  /* Bopo */
    public static final int CHEROKEE     =  6;  /* Cher */
    public static final int COPTIC       =  7;  /* Qaac */
    public static final int CYRILLIC     =  8;  /* Cyrl (Cyrs) */
    public static final int DESERET      =  9;  /* Dsrt */
    public static final int DEVANAGARI   = 10;  /* Deva */
    public static final int ETHIOPIC     = 11;  /* Ethi */
    public static final int GEORGIAN     = 12;  /* Geor (Geon; Geoa) */
    public static final int GOTHIC       = 13;  /* Goth */
    public static final int GREEK        = 14;  /* Grek */
    public static final int GUJARATI     = 15;  /* Gujr */
    public static final int GURMUKHI     = 16;  /* Guru */
    public static final int HAN          = 17;  /* Hani */
    public static final int HANGUL       = 18;  /* Hang */
    public static final int HEBREW       = 19;  /* Hebr */
    public static final int HIRAGANA     = 20;  /* Hira */
    public static final int KANNADA      = 21;  /* Knda */
    public static final int KATAKANA     = 22;  /* Kana */
    public static final int KHMER        = 23;  /* Khmr */
    public static final int LAO          = 24;  /* Laoo */
    public static final int LATIN        = 25;  /* Latn (Latf; Latg) */
    public static final int MALAYALAM    = 26;  /* Mlym */
    public static final int MONGOLIAN    = 27;  /* Mong */
    public static final int MYANMAR      = 28;  /* Mymr */
    public static final int OGHAM        = 29;  /* Ogam */
    public static final int OLD_ITALIC   = 30;  /* Ital */
    public static final int ORIYA        = 31;  /* Orya */
    public static final int RUNIC        = 32;  /* Runr */
    public static final int SINHALA      = 33;  /* Sinh */
    public static final int SYRIAC       = 34;  /* Syrc (Syrj; Syrn; Syre) */
    public static final int TAMIL        = 35;  /* Taml */
    public static final int TELUGU       = 36;  /* Telu */
    public static final int THAANA       = 37;  /* Thaa */
    public static final int THAI         = 38;  /* Thai */
    public static final int TIBETAN      = 39;  /* Tibt */
    public static final int UCAS         = 40;  /* Cans */
    public static final int YI           = 41;  /* Yiii */
    public static final int TAGALOG      = 42;  /* Tglg */
    public static final int HANUNOO      = 43;  /* Hano */
    public static final int BUHID        = 44;  /* Buhd */
    public static final int TAGBANWA     = 45;  /* Tagb */
    public static final int CODE_LIMIT   = 46; 
    
    private static final class NameCodePair{
        String name;
        int code;
        private NameCodePair(String str, int cd){
            name = str;
            code=cd;
        }
    }
    
	private static final NameCodePair[] scriptNames={
	   new NameCodePair( "ARABIC",              ARABIC      ),
	   new NameCodePair( "ARMENIAN",            ARMENIAN    ),
	   new NameCodePair( "BENGALI",             BENGALI     ),
	   new NameCodePair( "BOPOMOFO",            BOPOMOFO    ),
	   new NameCodePair( "BUHID",               BUHID       ),
	   new NameCodePair( "CANADIAN_ABORIGINAL", UCAS        ),
	   new NameCodePair( "CHEROKEE",            CHEROKEE    ),
	   new NameCodePair( "COMMON",              COMMON      ),
	   new NameCodePair( "CYRILLIC",            CYRILLIC    ),
	   new NameCodePair( "DESERET",             DESERET     ),
	   new NameCodePair( "DEVANAGARI",          DEVANAGARI  ),
	   new NameCodePair( "ETHIOPIC",            ETHIOPIC    ),
	   new NameCodePair( "GEORGIAN",            GEORGIAN    ),
	   new NameCodePair( "GOTHIC",              GOTHIC      ),
	   new NameCodePair( "GREEK",               GREEK       ),
	   new NameCodePair( "GUJARATI",            GUJARATI    ),
	   new NameCodePair( "GURMUKHI",            GURMUKHI    ),
	   new NameCodePair( "HAN",                 HAN         ),
	   new NameCodePair( "HANGUL",              HANGUL      ),
	   new NameCodePair( "HANUNOO",             HANUNOO     ),
	   new NameCodePair( "HEBREW",              HEBREW      ),
	   new NameCodePair( "HIRAGANA",            HIRAGANA    ),
	   new NameCodePair( "INHERITED",           INHERITED   ),
	   new NameCodePair( "KANNADA",             KANNADA     ),
	   new NameCodePair( "KATAKANA",            KATAKANA    ),
	   new NameCodePair( "KHMER",               KHMER       ),
	   new NameCodePair( "LATIN",               LATIN       ),
	   new NameCodePair( "MALAYALAM",           MALAYALAM   ),
	   new NameCodePair( "MONGOLIAN",           MONGOLIAN   ),
	   new NameCodePair( "MYANMAR",             MYANMAR     ),
	   new NameCodePair( "OGHAM",               OGHAM       ),
	   new NameCodePair( "OLD_ITALIC",          OLD_ITALIC  ),
	   new NameCodePair( "ORIYA",               ORIYA       ),
	   new NameCodePair( "RUNIC",               RUNIC       ),
	   new NameCodePair( "SINHALA",             SINHALA     ),
	   new NameCodePair( "SYRIAC",              SYRIAC      ),
	   new NameCodePair( "TAGALOG",             TAGALOG     ),
	   new NameCodePair( "TAGBANWA",            TAGBANWA    ),
	   new NameCodePair( "TAMIL",               TAMIL       ),
	   new NameCodePair( "TELUGU",              TELUGU      ),
	   new NameCodePair( "THAANA",              THAANA      ),
	   new NameCodePair( "THAI",                THAI        ),
	   new NameCodePair( "TIBETAN",             TIBETAN     ),
	   new NameCodePair( "UCAS",                UCAS        ),
	   new NameCodePair( "YI",                  YI          )
    };
	
	/* script abbreviations with codes, sorted by abbreviations */
	private static final NameCodePair[] scriptAbbr= {
	    new NameCodePair( "Arab",       ARABIC      ),
	    new NameCodePair( "Armn",       ARMENIAN    ),
	    new NameCodePair( "Beng",       BENGALI     ),
	    new NameCodePair( "Bopo",       BOPOMOFO    ),
	    new NameCodePair( "Buhd",       BUHID       ),
	    new NameCodePair( "Cans",       UCAS        ),
	    new NameCodePair( "Cher",       CHEROKEE    ),
	    new NameCodePair( "Cyrl",       CYRILLIC    ),
	 /* new NameCodePair( "Cyrs",       CYRILLIC    ), */
	    new NameCodePair( "Deva",       DEVANAGARI  ),
	    new NameCodePair( "Dsrt",       DESERET     ),
	    new NameCodePair( "Ethi",       ETHIOPIC    ),
	 /* new NameCodePair( Geoa",       GEORGIAN    ), */
	 /* new NameCodePair( Geon",       GEORGIAN    ), */
	    new NameCodePair( "Geor",       GEORGIAN    ),
	    new NameCodePair( "Goth",       GOTHIC      ),
	    new NameCodePair( "Grek",       GREEK       ),
	    new NameCodePair( "Gujr",       GUJARATI    ),
	    new NameCodePair( "Guru",       GURMUKHI    ),
	    new NameCodePair( "Hang",       HANGUL      ),
	    new NameCodePair( "Hani",       HAN         ),
	    new NameCodePair( "Hano",       HANUNOO     ),
	    new NameCodePair( "Hebr",       HEBREW      ),
	    new NameCodePair( "Hira",       HIRAGANA    ),
	    new NameCodePair( "Ital",       OLD_ITALIC  ),
	    new NameCodePair( "Kana",       KATAKANA    ),
	    new NameCodePair( "Khmr",       KHMER       ),
	    new NameCodePair( "Knda",       KANNADA     ),
	    new NameCodePair( "Lao",        LAO         ),
	 /* new NameCodePair( "Laoo",       LAO         ), */
	 /* new NameCodePair( "Latf",       LATIN       ), */
	 /* new NameCodePair( "Latg",       LATIN       ), */
	    new NameCodePair( "Latn",       LATIN       ),
	    new NameCodePair( "Mlym",       MALAYALAM   ),
	    new NameCodePair( "Mong",       MONGOLIAN   ),
	    new NameCodePair( "Mymr",       MYANMAR     ),
	    new NameCodePair( "Ogam",       OGHAM       ),
	    new NameCodePair( "Orya",       ORIYA       ),
	    new NameCodePair( "Qaac",       COPTIC      ),
	    new NameCodePair( "Qaai",       INHERITED   ),
	    new NameCodePair( "Runr",       RUNIC       ),
	    new NameCodePair( "Sinh",       SINHALA     ),
	    new NameCodePair( "Syrc",       SYRIAC      ),
	 /* new NameCodePair( "Syre",       SYRIAC      ), */
	 /* new NameCodePair( "Syrj",       SYRIAC      ), */
	 /* new NameCodePair( "Syrn",       SYRIAC      ), */
	    new NameCodePair( "Tagb",       TAGBANWA    ),
	    new NameCodePair( "Taml",       TAMIL       ),
	    new NameCodePair( "Telu",       TELUGU      ),
	    new NameCodePair( "Tglg",       TAGALOG     ),
	    new NameCodePair( "Thaa",       THAANA      ),
	    new NameCodePair( "Thai",       THAI        ),
	    new NameCodePair( "Tibt",       TIBETAN     ),
	    new NameCodePair( "Yiii",       YI          ),
	    new NameCodePair( "Zyyy",       COMMON      )
    };



    /* binary search the string array */
    private static int findStringIndex(NameCodePair[] sortedArr, String target){
        int size = sortedArr.length;
        int left, middle, right,rc;
        left =0;
        right= size-1;
        
        target = target.toUpperCase();
        while(left <= right){
            middle = (left+right)/2;
            rc=sortedArr[middle].name.toUpperCase().compareTo(target);
            if(rc<0){
                left = middle+1;
            }else if(rc >0){
                right = middle -1;
            }else{
                return middle;
            }
        }
        return -1;
    }

    /* linearly search the array and return the index */
    private static int findCodeIndex(NameCodePair[] unsorted, int target){
        int size = unsorted.length;
        int i=0;
        while(i<size){
            if(target == unsorted[i].code){
                return i;
            }
            i++;
        }
        return -1;
    }
    
    private static final int SCRIPT_MASK   = 0x0000007f;
    private static final UCharacterProperty prop= UCharacterProperty.getInstance();
    
    /**
     * Helper function to find the code from locale.
     * @param Locale the locale.
     * @exception MissingResourceException if LocaleScript cannot be opened
     */
    private static int[] findCodeFromLocale(Locale locale) {

        ResourceBundle rb = ICULocaleData.getLocaleElements(locale);

        // if rb is not a strict fallback of the requested locale, return null
        if (!LocaleUtility.isFallbackOf(rb.getLocale(), locale)) {
            return null;
        }

        String[] scripts = rb.getStringArray("LocaleScript");
        int[] result = new int[scripts.length];
        int w = 0;
        for (int i = 0; i < scripts.length; ++i) {
            int strIndex = findStringIndex(scriptAbbr, scripts[i]);
            if (strIndex != -1) {
                result[w++] = scriptAbbr[strIndex].code;
            }
        }

        if (w < result.length) {
            throw new InternalError("bad locale data, listed " + scripts.length + " scripts but found only " + w);
        }

        return result;
    }
         
    /**
     * Gets a script codes associated with the given locale or ISO 15924 abbreviation or name. 
     * Returns MALAYAM given "Malayam" OR "Mlym".
     * Returns LATIN given "en" OR "en_US" 
     * @param locale Locale
     * @return The script codes array. null if the the code cannot be found. 
     * @exception MissingResourceException
     * @draft
     */
    public static final int[] getCode(Locale locale)
        throws MissingResourceException {
        return findCodeFromLocale(locale);
        }
    
    /**
     * Gets a script codes associated with the given locale or ISO 15924 abbreviation or name. 
     * Returns MALAYAM given "Malayam" OR "Mlym".
     * Returns LATIN given "en" OR "en_US" 
     * @param nameOrAbbrOrLocale name of the script or ISO 15924 code or locale
     * @return The script codes array. null if the the code cannot be found.
     * @draft
     */
    public static final int[] getCode(String nameOrAbbrOrLocale){
            
        int[] code = new int[1];
        code[0] = INVALID_CODE;
        int strIndex=0;
        
        /* try the Names array first */
        strIndex = findStringIndex(scriptNames, nameOrAbbrOrLocale);
        
        if(strIndex>=0 && strIndex < scriptNames.length){ 
            code[0] =  scriptNames[strIndex].code;
        }
        /* we did not find in names array so try abbr array*/
        if(code[0] == INVALID_CODE){
            strIndex = findStringIndex(scriptAbbr, nameOrAbbrOrLocale);
            if(strIndex>=0 && strIndex < scriptAbbr.length){ 
                code[0] =  scriptAbbr[strIndex].code;
            }
        }
        /* we still haven't found it try locale */        
        if(code[0]==INVALID_CODE){            
            code = findCodeFromLocale(LocaleUtility.getLocaleFromName(nameOrAbbrOrLocale));
        }
        return code;
    }

    /** 
     * Gets the script code associated with the given codepoint.
     * Returns UScript.MALAYAM given 0x0D02 
     * @param codepoint UChar32 codepoint
     * @param err the error status code.
     * @return The script code 
     * @exception IllegalArgumentException
     * @draft
     */
    public static final int getScript(int codepoint){
        if (codepoint >= UCharacter.MIN_VALUE & codepoint <= UCharacter.MAX_VALUE) {
            return (int)(prop.getAdditional(codepoint,0) & SCRIPT_MASK);
        }else{
            throw new IllegalArgumentException(Integer.toString(codepoint));
        } 
    }
    
    /**
     * Gets a script name associated with the given script code. 
     * Returns  "Malayam" given MALAYAM
     * @param scriptCode int script code
     * @return script name as a string in full as given in TR#24
     * @exception IllegalArgumentException
     * @draft
     */
    public static final String getName(int scriptCode){
        int index = -1;
        if(scriptCode > CODE_LIMIT){
            throw new IllegalArgumentException(Integer.toString(scriptCode));
        }
        index = findCodeIndex(scriptNames,scriptCode);
        if(index >=0){
            return scriptNames[index].name;
        }else{
            throw new IllegalArgumentException(Integer.toString(scriptCode));
        }
    }
    
    /**
     * Gets a script name associated with the given script code. 
     * Returns  "Mlym" given MALAYAM
     * @param scriptCode int script code 
     * @return script abbreviated name as a string  as given in TR#24
     * @exception IllegalArgumentException
     * @draft
     */
    public static final String getShortName(int scriptCode){
        int index = -1;
        if(scriptCode > CODE_LIMIT){
            throw new IllegalArgumentException(Integer.toString(scriptCode));
        }
        index = findCodeIndex(scriptAbbr,scriptCode);
        if(index >=0){
            return scriptAbbr[index].name;
        }else{
            throw new IllegalArgumentException(Integer.toString(scriptCode));
        }
    }
}

