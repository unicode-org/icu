/**
*******************************************************************************
* Copyright (C) 2001, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.lang;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.LocaleUtility;

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
    public static final int CODE_LIMIT   = 42; 
    
    private static final String scriptNames[]={
        "ARABIC",               /* ARABIC     */
        "ARMENIAN",             /* ARMENIAN   */
        "BENGALI",              /* BENGALI    */
        "BOPOMOFO",             /* BOPOMOFO   */
        "CANADIAN-ABORIGINAL",  /* UCAS       */
        "CHEROKEE",             /* CHEROKEE   */
        "COMMON",               /* COMMON     */
        "CYRILLIC",             /* CYRILLIC   */
        "DESERET",              /* DESERET    */
        "DEVANAGARI",           /* DEVANAGARI */
        "ETHIOPIC",             /* ETHIOPIC   */
        "GEORGIAN",             /* GEORGIAN   */
        "GOTHIC",               /* GOTHIC     */
        "GREEK",                /* GREEK      */
        "GUJARATI",             /* GUJARATI   */
        "GURMUKHI",             /* GURMUKHI   */
        "HAN",                  /* HAN        */
        "HANGUL",               /* HANGUL     */
        "HEBREW",               /* HEBREW     */
        "HIRAGANA",             /* HIRAGANA   */
        "INHERITED",            /* INHERITED  */
        "KANNADA",              /* KANNADA    */
        "KATAKANA",             /* KATAKANA   */
        "KHMER",                /* KHMER      */
        "LATIN",                /* LATIN      */
        "MALAYALAM",            /* MALAYALAM  */
        "MONGOLIAN",            /* MONGOLIAN  */
        "MYANMAR",              /* MYANMAR    */
        "OGHAM",                /* OGHAM      */
        "OLD-ITALIC",           /* OLD_ITALIC */
        "ORIYA",                /* ORIYA      */
        "RUNIC",                /* RUNIC      */
        "SINHALA",              /* SINHALA    */
        "SYRIAC",               /* SYRIAC     */
        "TAMIL",                /* TAMIL      */
        "TELUGU",               /* TELUGU     */
        "THAANA",               /* THANA      */
        "THAI",                 /* THAI       */
        "TIBETAN",              /* TIBETAN    */
        "UCAS",                 /* UCAS       */
        "YI",                   /* YI         */
        
    };

    private static final String scriptAbbr[]= {
        "Arab",       /* ARABIC     */
        "Armn",       /* ARMENIAN   */
        "Beng",       /* BENGALI    */
        "Bopo",       /* BOPOMOFO   */
        "Cans",       /* UCAS       */
        "Cher",       /* CHEROKEE   */
        "Cyrl",       /* CYRILLIC   */
     /* "Cyrs",  */   /* CYRILLIC   */
        "Deva",       /* DEVANAGARI */
        "Dsrt",       /* DESERET    */
        "Ethi",       /* ETHIOPIC   */
     /* "Geoa",  */   /* GEORGIAN   */
     /* "Geon",  */   /* GEORGIAN   */
        "Geor",       /* GEORGIAN   */
        "Goth",       /* GOTHIC     */
        "Grek",       /* GREEK      */
        "Gujr",       /* GUJARATI   */
        "Guru",       /* GURMUKHI   */
        "Hang",       /* HANGUL     */
        "Hani",       /* HAN        */
        "Hebr",       /* HEBREW     */
        "Hira",       /* HIRAGANA   */
        "Ital",       /* OLD_ITALIC */
        "Kana",       /* KATAKANA   */
        "Khmr",       /* KHMER      */
        "Knda",       /* KANNADA    */
        "Lao",        /* LAO        */
     /* "Laoo",  */   /* LAO        */
     /* "Latf",  */   /* LATIN      */
     /* "Latg",  */   /* LATIN      */
        "Latn",       /* LATIN      */
        "Mlym",       /* MALAYALAM  */
        "Mong",       /* MONGOLIAN  */
        "Mymr",       /* MYANMAR    */
        "Ogam",       /* OGHAM      */
        "Orya",       /* ORIYA      */
        "Qaac",       /* COPTIC     */
        "Qaai",       /* INHERITED  */
        "Runr",       /* RUNIC      */
        "Sinh",       /* SINHALA    */
        "Syrc",       /* SYRIAC     */
     /* "Syre",  */   /* SYRIAC     */
     /* "Syrj",  */   /* SYRIAC     */
     /* "Syrn",  */   /* SYRIAC     */
        "Taml",       /* TAMIL      */
        "Telu",       /* TELUGU     */
        "Thaa",       /* THANA      */
        "Thai",       /* THAI       */
        "Tibt",       /* TIBETAN    */
        "Yiii",       /* YI         */
        "Zyyy",       /* COMMON     */   
    };


    private static final int scriptNameCodes[]= {
        ARABIC     ,
        ARMENIAN   ,
        BENGALI    ,
        BOPOMOFO   ,
        UCAS       ,
        CHEROKEE   ,
        COMMON     ,
        CYRILLIC   ,
        DESERET    ,
        DEVANAGARI ,
        ETHIOPIC   ,
        GEORGIAN   ,
        GOTHIC     ,
        GREEK      ,
        GUJARATI   ,
        GURMUKHI   ,
        HAN        ,
        HANGUL     ,
        HEBREW     ,
        HIRAGANA   ,
        INHERITED  ,
        KANNADA    ,
        KATAKANA   ,
        KHMER      ,
        LATIN      ,
        MALAYALAM  ,
        MONGOLIAN  ,
        MYANMAR    ,
        OGHAM      ,
        OLD_ITALIC ,
        ORIYA      ,
        RUNIC      ,
        SINHALA    ,
        SYRIAC     ,
        TAMIL      ,
        TELUGU     ,
        THAANA     ,
        THAI       ,
        TIBETAN    ,
        UCAS       ,
        YI
    };


    private static final int scriptAbbrCodes[] = {
        ARABIC     ,
        ARMENIAN   ,
        BENGALI    ,
        BOPOMOFO   ,
        UCAS       ,
        CHEROKEE   ,
        CYRILLIC   ,
     /* CYRILLIC   , */
        DEVANAGARI ,
        DESERET    ,
        ETHIOPIC   ,
     /* GEORGIAN   , */
     /* GEORGIAN   , */
        GEORGIAN   ,
        GOTHIC     ,
        GREEK      ,
        GUJARATI   ,
        GURMUKHI   ,
        HANGUL     ,
        HAN        ,
        HEBREW     ,
        HIRAGANA   ,
        OLD_ITALIC ,
        KATAKANA   ,
        KHMER      ,
        KANNADA    ,
        LAO        ,
     /* LAO        , */
     /* LATIN      , */
     /* LATIN      , */
        LATIN      ,
        MALAYALAM  ,
        MONGOLIAN  ,
        MYANMAR    ,
        OGHAM      ,
        ORIYA      ,
        COPTIC     ,
        INHERITED  ,
        RUNIC      ,
        SINHALA    ,
     /* SYRIAC     , */
     /* SYRIAC     , */
     /* SYRIAC     , */
        SYRIAC     ,
        TAMIL      ,
        TELUGU     ,
        THAANA     ,
        THAI       ,
        TIBETAN    ,
        YI         ,
        COMMON     , 
    };

    /**********************************************************
     *
     * WARNING: The below map is machine generated
     * by genscrpt after parsing Scripts.txt,
     * plese donot edit unless you know what you are doing
     *
     * TODO: Remove after merging script names to uprops.dat
     *
     **********************************************************
     */
    private static final class UScriptCodeMap {
        int  firstCodepoint;
        int  lastCodepoint;
        int  scriptCode;
        public UScriptCodeMap(int firstCp, int lastCp, int code){
            firstCodepoint = firstCp;
            lastCodepoint  = lastCp;
            scriptCode     = code ;
        }
    };
    
    private static final UScriptCodeMap[] scriptCodeIndex = {
        new UScriptCodeMap(0x00000000, 0x00000000, COMMON) ,
        new UScriptCodeMap(0x00000041, 0x0000005A, LATIN ) ,
        new UScriptCodeMap(0x00000061, 0x0000007A, LATIN ) ,
        new UScriptCodeMap(0x000000AA, 0x000000AA, LATIN ) ,
        new UScriptCodeMap(0x000000B5, 0x000000B5, GREEK ) ,
        new UScriptCodeMap(0x000000BA, 0x000000BA, LATIN ) ,
        new UScriptCodeMap(0x000000C0, 0x000000D6, LATIN ) ,
        new UScriptCodeMap(0x000000D8, 0x000000F6, LATIN ) ,
        new UScriptCodeMap(0x000000F8, 0x000001BA, LATIN ) ,
        new UScriptCodeMap(0x000001BB, 0x000001BB, LATIN ) ,
        new UScriptCodeMap(0x000001BC, 0x000001BF, LATIN ) ,
        new UScriptCodeMap(0x000001C0, 0x000001C3, LATIN ) ,
        new UScriptCodeMap(0x000001C4, 0x0000021F, LATIN ) ,
        new UScriptCodeMap(0x00000222, 0x00000233, LATIN ) ,
        new UScriptCodeMap(0x00000250, 0x000002AD, LATIN ) ,
        new UScriptCodeMap(0x000002B0, 0x000002B8, LATIN ) ,
        new UScriptCodeMap(0x000002E0, 0x000002E4, LATIN ) ,
        new UScriptCodeMap(0x00000300, 0x0000034E, INHERITED ) ,
        new UScriptCodeMap(0x00000360, 0x00000362, INHERITED ) ,
        new UScriptCodeMap(0x0000037A, 0x0000037A, GREEK ) ,
        new UScriptCodeMap(0x00000386, 0x00000386, GREEK ) ,
        new UScriptCodeMap(0x00000388, 0x0000038A, GREEK ) ,
        new UScriptCodeMap(0x0000038C, 0x0000038C, GREEK ) ,
        new UScriptCodeMap(0x0000038E, 0x000003A1, GREEK ) ,
        new UScriptCodeMap(0x000003A3, 0x000003CE, GREEK ) ,
        new UScriptCodeMap(0x000003D0, 0x000003D7, GREEK ) ,
        new UScriptCodeMap(0x000003DA, 0x000003F5, GREEK ) ,
        new UScriptCodeMap(0x00000400, 0x00000481, CYRILLIC ) ,
        new UScriptCodeMap(0x00000483, 0x00000486, CYRILLIC ) ,
        new UScriptCodeMap(0x00000488, 0x00000489, INHERITED ) ,
        new UScriptCodeMap(0x0000048C, 0x000004C4, CYRILLIC ) ,
        new UScriptCodeMap(0x000004C7, 0x000004C8, CYRILLIC ) ,
        new UScriptCodeMap(0x000004CB, 0x000004CC, CYRILLIC ) ,
        new UScriptCodeMap(0x000004D0, 0x000004F5, CYRILLIC ) ,
        new UScriptCodeMap(0x000004F8, 0x000004F9, CYRILLIC ) ,
        new UScriptCodeMap(0x00000531, 0x00000556, ARMENIAN ) ,
        new UScriptCodeMap(0x00000559, 0x00000559, ARMENIAN ) ,
        new UScriptCodeMap(0x00000561, 0x00000587, ARMENIAN ) ,
        new UScriptCodeMap(0x00000591, 0x000005A1, INHERITED ) ,
        new UScriptCodeMap(0x000005A3, 0x000005B9, INHERITED ) ,
        new UScriptCodeMap(0x000005BB, 0x000005BD, INHERITED ) ,
        new UScriptCodeMap(0x000005BF, 0x000005BF, INHERITED ) ,
        new UScriptCodeMap(0x000005C1, 0x000005C2, INHERITED ) ,
        new UScriptCodeMap(0x000005C4, 0x000005C4, INHERITED ) ,
        new UScriptCodeMap(0x000005D0, 0x000005EA, HEBREW ) ,
        new UScriptCodeMap(0x000005F0, 0x000005F2, HEBREW ) ,
        new UScriptCodeMap(0x00000621, 0x0000063A, ARABIC ) ,
        new UScriptCodeMap(0x00000641, 0x0000064A, ARABIC ) ,
        new UScriptCodeMap(0x0000064B, 0x00000655, INHERITED ) ,
        new UScriptCodeMap(0x00000670, 0x00000670, INHERITED ) ,
        new UScriptCodeMap(0x00000671, 0x000006D3, ARABIC ) ,
        new UScriptCodeMap(0x000006D5, 0x000006D5, ARABIC ) ,
        new UScriptCodeMap(0x000006D6, 0x000006DC, INHERITED ) ,
        new UScriptCodeMap(0x000006DD, 0x000006DE, INHERITED ) ,
        new UScriptCodeMap(0x000006DF, 0x000006E4, INHERITED ) ,
        new UScriptCodeMap(0x000006E5, 0x000006E6, ARABIC ) ,
        new UScriptCodeMap(0x000006E7, 0x000006E8, INHERITED ) ,
        new UScriptCodeMap(0x000006EA, 0x000006ED, INHERITED ) ,
        new UScriptCodeMap(0x000006FA, 0x000006FC, ARABIC ) ,
        new UScriptCodeMap(0x00000710, 0x00000710, SYRIAC ) ,
        new UScriptCodeMap(0x00000711, 0x00000711, SYRIAC ) ,
        new UScriptCodeMap(0x00000712, 0x0000072C, SYRIAC ) ,
        new UScriptCodeMap(0x00000730, 0x0000074A, SYRIAC ) ,
        new UScriptCodeMap(0x00000780, 0x000007A5, THAANA ) ,
        new UScriptCodeMap(0x000007A6, 0x000007B0, THAANA ) ,
        new UScriptCodeMap(0x00000901, 0x00000902, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000903, 0x00000903, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000905, 0x00000939, DEVANAGARI ) ,
        new UScriptCodeMap(0x0000093C, 0x0000093C, DEVANAGARI ) ,
        new UScriptCodeMap(0x0000093D, 0x0000093D, DEVANAGARI ) ,
        new UScriptCodeMap(0x0000093E, 0x00000940, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000941, 0x00000948, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000949, 0x0000094C, DEVANAGARI ) ,
        new UScriptCodeMap(0x0000094D, 0x0000094D, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000950, 0x00000950, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000951, 0x00000954, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000958, 0x00000961, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000962, 0x00000963, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000966, 0x0000096F, DEVANAGARI ) ,
        new UScriptCodeMap(0x00000981, 0x00000981, BENGALI ) ,
        new UScriptCodeMap(0x00000985, 0x0000098C, BENGALI ) ,
        new UScriptCodeMap(0x0000098F, 0x00000990, BENGALI ) ,
        new UScriptCodeMap(0x00000993, 0x000009A8, BENGALI ) ,
        new UScriptCodeMap(0x000009AA, 0x000009B0, BENGALI ) ,
        new UScriptCodeMap(0x000009B2, 0x000009B2, BENGALI ) ,
        new UScriptCodeMap(0x000009B6, 0x000009B9, BENGALI ) ,
        new UScriptCodeMap(0x000009BC, 0x000009BC, BENGALI ) ,
        new UScriptCodeMap(0x000009BE, 0x000009C0, BENGALI ) ,
        new UScriptCodeMap(0x000009C1, 0x000009C4, BENGALI ) ,
        new UScriptCodeMap(0x000009C7, 0x000009C8, BENGALI ) ,
        new UScriptCodeMap(0x000009CB, 0x000009CC, BENGALI ) ,
        new UScriptCodeMap(0x000009CD, 0x000009CD, BENGALI ) ,
        new UScriptCodeMap(0x000009D7, 0x000009D7, BENGALI ) ,
        new UScriptCodeMap(0x000009DC, 0x000009DD, BENGALI ) ,
        new UScriptCodeMap(0x000009DF, 0x000009E1, BENGALI ) ,
        new UScriptCodeMap(0x000009E2, 0x000009E3, BENGALI ) ,
        new UScriptCodeMap(0x000009E6, 0x000009EF, BENGALI ) ,
        new UScriptCodeMap(0x000009F0, 0x000009F1, BENGALI ) ,
        new UScriptCodeMap(0x00000A02, 0x00000A02, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A05, 0x00000A0A, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A0F, 0x00000A10, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A13, 0x00000A28, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A2A, 0x00000A30, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A32, 0x00000A33, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A35, 0x00000A36, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A38, 0x00000A39, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A3C, 0x00000A3C, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A3E, 0x00000A40, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A41, 0x00000A42, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A47, 0x00000A48, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A4B, 0x00000A4D, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A59, 0x00000A5C, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A5E, 0x00000A5E, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A66, 0x00000A6F, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A70, 0x00000A71, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A72, 0x00000A74, GURMUKHI ) ,
        new UScriptCodeMap(0x00000A81, 0x00000A82, GUJARATI ) ,
        new UScriptCodeMap(0x00000A83, 0x00000A83, GUJARATI ) ,
        new UScriptCodeMap(0x00000A85, 0x00000A8B, GUJARATI ) ,
        new UScriptCodeMap(0x00000A8D, 0x00000A8D, GUJARATI ) ,
        new UScriptCodeMap(0x00000A8F, 0x00000A91, GUJARATI ) ,
        new UScriptCodeMap(0x00000A93, 0x00000AA8, GUJARATI ) ,
        new UScriptCodeMap(0x00000AAA, 0x00000AB0, GUJARATI ) ,
        new UScriptCodeMap(0x00000AB2, 0x00000AB3, GUJARATI ) ,
        new UScriptCodeMap(0x00000AB5, 0x00000AB9, GUJARATI ) ,
        new UScriptCodeMap(0x00000ABC, 0x00000ABC, GUJARATI ) ,
        new UScriptCodeMap(0x00000ABD, 0x00000ABD, GUJARATI ) ,
        new UScriptCodeMap(0x00000ABE, 0x00000AC0, GUJARATI ) ,
        new UScriptCodeMap(0x00000AC1, 0x00000AC5, GUJARATI ) ,
        new UScriptCodeMap(0x00000AC7, 0x00000AC8, GUJARATI ) ,
        new UScriptCodeMap(0x00000AC9, 0x00000AC9, GUJARATI ) ,
        new UScriptCodeMap(0x00000ACB, 0x00000ACC, GUJARATI ) ,
        new UScriptCodeMap(0x00000ACD, 0x00000ACD, GUJARATI ) ,
        new UScriptCodeMap(0x00000AD0, 0x00000AD0, GUJARATI ) ,
        new UScriptCodeMap(0x00000AE0, 0x00000AE0, GUJARATI ) ,
        new UScriptCodeMap(0x00000AE6, 0x00000AEF, GUJARATI ) ,
        new UScriptCodeMap(0x00000B01, 0x00000B01, ORIYA ) ,
        new UScriptCodeMap(0x00000B02, 0x00000B03, ORIYA ) ,
        new UScriptCodeMap(0x00000B05, 0x00000B0C, ORIYA ) ,
        new UScriptCodeMap(0x00000B0F, 0x00000B10, ORIYA ) ,
        new UScriptCodeMap(0x00000B13, 0x00000B28, ORIYA ) ,
        new UScriptCodeMap(0x00000B2A, 0x00000B30, ORIYA ) ,
        new UScriptCodeMap(0x00000B32, 0x00000B33, ORIYA ) ,
        new UScriptCodeMap(0x00000B36, 0x00000B39, ORIYA ) ,
        new UScriptCodeMap(0x00000B3C, 0x00000B3C, ORIYA ) ,
        new UScriptCodeMap(0x00000B3D, 0x00000B3D, ORIYA ) ,
        new UScriptCodeMap(0x00000B3E, 0x00000B3E, ORIYA ) ,
        new UScriptCodeMap(0x00000B3F, 0x00000B3F, ORIYA ) ,
        new UScriptCodeMap(0x00000B40, 0x00000B40, ORIYA ) ,
        new UScriptCodeMap(0x00000B41, 0x00000B43, ORIYA ) ,
        new UScriptCodeMap(0x00000B47, 0x00000B48, ORIYA ) ,
        new UScriptCodeMap(0x00000B4B, 0x00000B4C, ORIYA ) ,
        new UScriptCodeMap(0x00000B4D, 0x00000B4D, ORIYA ) ,
        new UScriptCodeMap(0x00000B56, 0x00000B56, ORIYA ) ,
        new UScriptCodeMap(0x00000B57, 0x00000B57, ORIYA ) ,
        new UScriptCodeMap(0x00000B5C, 0x00000B5D, ORIYA ) ,
        new UScriptCodeMap(0x00000B5F, 0x00000B61, ORIYA ) ,
        new UScriptCodeMap(0x00000B66, 0x00000B6F, ORIYA ) ,
        new UScriptCodeMap(0x00000B82, 0x00000B82, TAMIL ) ,
        new UScriptCodeMap(0x00000B83, 0x00000B83, TAMIL ) ,
        new UScriptCodeMap(0x00000B85, 0x00000B8A, TAMIL ) ,
        new UScriptCodeMap(0x00000B8E, 0x00000B90, TAMIL ) ,
        new UScriptCodeMap(0x00000B92, 0x00000B95, TAMIL ) ,
        new UScriptCodeMap(0x00000B99, 0x00000B9A, TAMIL ) ,
        new UScriptCodeMap(0x00000B9C, 0x00000B9C, TAMIL ) ,
        new UScriptCodeMap(0x00000B9E, 0x00000B9F, TAMIL ) ,
        new UScriptCodeMap(0x00000BA3, 0x00000BA4, TAMIL ) ,
        new UScriptCodeMap(0x00000BA8, 0x00000BAA, TAMIL ) ,
        new UScriptCodeMap(0x00000BAE, 0x00000BB5, TAMIL ) ,
        new UScriptCodeMap(0x00000BB7, 0x00000BB9, TAMIL ) ,
        new UScriptCodeMap(0x00000BBE, 0x00000BBF, TAMIL ) ,
        new UScriptCodeMap(0x00000BC0, 0x00000BC0, TAMIL ) ,
        new UScriptCodeMap(0x00000BC1, 0x00000BC2, TAMIL ) ,
        new UScriptCodeMap(0x00000BC6, 0x00000BC8, TAMIL ) ,
        new UScriptCodeMap(0x00000BCA, 0x00000BCC, TAMIL ) ,
        new UScriptCodeMap(0x00000BCD, 0x00000BCD, TAMIL ) ,
        new UScriptCodeMap(0x00000BD7, 0x00000BD7, TAMIL ) ,
        new UScriptCodeMap(0x00000BE7, 0x00000BEF, TAMIL ) ,
        new UScriptCodeMap(0x00000BF0, 0x00000BF2, TAMIL ) ,
        new UScriptCodeMap(0x00000C01, 0x00000C03, TELUGU ) ,
        new UScriptCodeMap(0x00000C05, 0x00000C0C, TELUGU ) ,
        new UScriptCodeMap(0x00000C0E, 0x00000C10, TELUGU ) ,
        new UScriptCodeMap(0x00000C12, 0x00000C28, TELUGU ) ,
        new UScriptCodeMap(0x00000C2A, 0x00000C33, TELUGU ) ,
        new UScriptCodeMap(0x00000C35, 0x00000C39, TELUGU ) ,
        new UScriptCodeMap(0x00000C3E, 0x00000C40, TELUGU ) ,
        new UScriptCodeMap(0x00000C41, 0x00000C44, TELUGU ) ,
        new UScriptCodeMap(0x00000C46, 0x00000C48, TELUGU ) ,
        new UScriptCodeMap(0x00000C4A, 0x00000C4D, TELUGU ) ,
        new UScriptCodeMap(0x00000C55, 0x00000C56, TELUGU ) ,
        new UScriptCodeMap(0x00000C60, 0x00000C61, TELUGU ) ,
        new UScriptCodeMap(0x00000C66, 0x00000C6F, TELUGU ) ,
        new UScriptCodeMap(0x00000C82, 0x00000C83, KANNADA ) ,
        new UScriptCodeMap(0x00000C85, 0x00000C8C, KANNADA ) ,
        new UScriptCodeMap(0x00000C8E, 0x00000C90, KANNADA ) ,
        new UScriptCodeMap(0x00000C92, 0x00000CA8, KANNADA ) ,
        new UScriptCodeMap(0x00000CAA, 0x00000CB3, KANNADA ) ,
        new UScriptCodeMap(0x00000CB5, 0x00000CB9, KANNADA ) ,
        new UScriptCodeMap(0x00000CBE, 0x00000CBE, KANNADA ) ,
        new UScriptCodeMap(0x00000CBF, 0x00000CBF, KANNADA ) ,
        new UScriptCodeMap(0x00000CC0, 0x00000CC4, KANNADA ) ,
        new UScriptCodeMap(0x00000CC6, 0x00000CC6, KANNADA ) ,
        new UScriptCodeMap(0x00000CC7, 0x00000CC8, KANNADA ) ,
        new UScriptCodeMap(0x00000CCA, 0x00000CCB, KANNADA ) ,
        new UScriptCodeMap(0x00000CCC, 0x00000CCD, KANNADA ) ,
        new UScriptCodeMap(0x00000CD5, 0x00000CD6, KANNADA ) ,
        new UScriptCodeMap(0x00000CDE, 0x00000CDE, KANNADA ) ,
        new UScriptCodeMap(0x00000CE0, 0x00000CE1, KANNADA ) ,
        new UScriptCodeMap(0x00000CE6, 0x00000CEF, KANNADA ) ,
        new UScriptCodeMap(0x00000D02, 0x00000D03, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D05, 0x00000D0C, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D0E, 0x00000D10, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D12, 0x00000D28, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D2A, 0x00000D39, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D3E, 0x00000D40, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D41, 0x00000D43, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D46, 0x00000D48, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D4A, 0x00000D4C, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D4D, 0x00000D4D, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D57, 0x00000D57, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D60, 0x00000D61, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D66, 0x00000D6F, MALAYALAM ) ,
        new UScriptCodeMap(0x00000D82, 0x00000D83, SINHALA ) ,
        new UScriptCodeMap(0x00000D85, 0x00000D96, SINHALA ) ,
        new UScriptCodeMap(0x00000D9A, 0x00000DB1, SINHALA ) ,
        new UScriptCodeMap(0x00000DB3, 0x00000DBB, SINHALA ) ,
        new UScriptCodeMap(0x00000DBD, 0x00000DBD, SINHALA ) ,
        new UScriptCodeMap(0x00000DC0, 0x00000DC6, SINHALA ) ,
        new UScriptCodeMap(0x00000DCA, 0x00000DCA, SINHALA ) ,
        new UScriptCodeMap(0x00000DCF, 0x00000DD1, SINHALA ) ,
        new UScriptCodeMap(0x00000DD2, 0x00000DD4, SINHALA ) ,
        new UScriptCodeMap(0x00000DD6, 0x00000DD6, SINHALA ) ,
        new UScriptCodeMap(0x00000DD8, 0x00000DDF, SINHALA ) ,
        new UScriptCodeMap(0x00000DF2, 0x00000DF3, SINHALA ) ,
        new UScriptCodeMap(0x00000E01, 0x00000E30, THAI ) ,
        new UScriptCodeMap(0x00000E31, 0x00000E31, THAI ) ,
        new UScriptCodeMap(0x00000E32, 0x00000E33, THAI ) ,
        new UScriptCodeMap(0x00000E34, 0x00000E3A, THAI ) ,
        new UScriptCodeMap(0x00000E40, 0x00000E45, THAI ) ,
        new UScriptCodeMap(0x00000E46, 0x00000E46, THAI ) ,
        new UScriptCodeMap(0x00000E47, 0x00000E4E, THAI ) ,
        new UScriptCodeMap(0x00000E50, 0x00000E59, THAI ) ,
        new UScriptCodeMap(0x00000E81, 0x00000E82, LAO ) ,
        new UScriptCodeMap(0x00000E84, 0x00000E84, LAO ) ,
        new UScriptCodeMap(0x00000E87, 0x00000E88, LAO ) ,
        new UScriptCodeMap(0x00000E8A, 0x00000E8A, LAO ) ,
        new UScriptCodeMap(0x00000E8D, 0x00000E8D, LAO ) ,
        new UScriptCodeMap(0x00000E94, 0x00000E97, LAO ) ,
        new UScriptCodeMap(0x00000E99, 0x00000E9F, LAO ) ,
        new UScriptCodeMap(0x00000EA1, 0x00000EA3, LAO ) ,
        new UScriptCodeMap(0x00000EA5, 0x00000EA5, LAO ) ,
        new UScriptCodeMap(0x00000EA7, 0x00000EA7, LAO ) ,
        new UScriptCodeMap(0x00000EAA, 0x00000EAB, LAO ) ,
        new UScriptCodeMap(0x00000EAD, 0x00000EB0, LAO ) ,
        new UScriptCodeMap(0x00000EB1, 0x00000EB1, LAO ) ,
        new UScriptCodeMap(0x00000EB2, 0x00000EB3, LAO ) ,
        new UScriptCodeMap(0x00000EB4, 0x00000EB9, LAO ) ,
        new UScriptCodeMap(0x00000EBB, 0x00000EBC, LAO ) ,
        new UScriptCodeMap(0x00000EBD, 0x00000EBD, LAO ) ,
        new UScriptCodeMap(0x00000EC0, 0x00000EC4, LAO ) ,
        new UScriptCodeMap(0x00000EC6, 0x00000EC6, LAO ) ,
        new UScriptCodeMap(0x00000EC8, 0x00000ECD, LAO ) ,
        new UScriptCodeMap(0x00000ED0, 0x00000ED9, LAO ) ,
        new UScriptCodeMap(0x00000EDC, 0x00000EDD, LAO ) ,
        new UScriptCodeMap(0x00000F00, 0x00000F00, TIBETAN ) ,
        new UScriptCodeMap(0x00000F18, 0x00000F19, TIBETAN ) ,
        new UScriptCodeMap(0x00000F20, 0x00000F29, TIBETAN ) ,
        new UScriptCodeMap(0x00000F2A, 0x00000F33, TIBETAN ) ,
        new UScriptCodeMap(0x00000F35, 0x00000F35, TIBETAN ) ,
        new UScriptCodeMap(0x00000F37, 0x00000F37, TIBETAN ) ,
        new UScriptCodeMap(0x00000F39, 0x00000F39, TIBETAN ) ,
        new UScriptCodeMap(0x00000F40, 0x00000F47, TIBETAN ) ,
        new UScriptCodeMap(0x00000F49, 0x00000F6A, TIBETAN ) ,
        new UScriptCodeMap(0x00000F71, 0x00000F7E, TIBETAN ) ,
        new UScriptCodeMap(0x00000F7F, 0x00000F7F, TIBETAN ) ,
        new UScriptCodeMap(0x00000F80, 0x00000F84, TIBETAN ) ,
        new UScriptCodeMap(0x00000F86, 0x00000F87, TIBETAN ) ,
        new UScriptCodeMap(0x00000F88, 0x00000F8B, TIBETAN ) ,
        new UScriptCodeMap(0x00000F90, 0x00000F97, TIBETAN ) ,
        new UScriptCodeMap(0x00000F99, 0x00000FBC, TIBETAN ) ,
        new UScriptCodeMap(0x00000FC6, 0x00000FC6, TIBETAN ) ,
        new UScriptCodeMap(0x00001000, 0x00001021, MYANMAR ) ,
        new UScriptCodeMap(0x00001023, 0x00001027, MYANMAR ) ,
        new UScriptCodeMap(0x00001029, 0x0000102A, MYANMAR ) ,
        new UScriptCodeMap(0x0000102C, 0x0000102C, MYANMAR ) ,
        new UScriptCodeMap(0x0000102D, 0x00001030, MYANMAR ) ,
        new UScriptCodeMap(0x00001031, 0x00001031, MYANMAR ) ,
        new UScriptCodeMap(0x00001032, 0x00001032, MYANMAR ) ,
        new UScriptCodeMap(0x00001036, 0x00001037, MYANMAR ) ,
        new UScriptCodeMap(0x00001038, 0x00001038, MYANMAR ) ,
        new UScriptCodeMap(0x00001039, 0x00001039, MYANMAR ) ,
        new UScriptCodeMap(0x00001040, 0x00001049, MYANMAR ) ,
        new UScriptCodeMap(0x00001050, 0x00001055, MYANMAR ) ,
        new UScriptCodeMap(0x00001056, 0x00001057, MYANMAR ) ,
        new UScriptCodeMap(0x00001058, 0x00001059, MYANMAR ) ,
        new UScriptCodeMap(0x000010A0, 0x000010C5, GEORGIAN ) ,
        new UScriptCodeMap(0x000010D0, 0x000010F6, GEORGIAN ) ,
        new UScriptCodeMap(0x00001100, 0x00001159, HANGUL ) ,
        new UScriptCodeMap(0x0000115F, 0x000011A2, HANGUL ) ,
        new UScriptCodeMap(0x000011A8, 0x000011F9, HANGUL ) ,
        new UScriptCodeMap(0x00001200, 0x00001206, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001208, 0x00001246, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001248, 0x00001248, ETHIOPIC ) ,
        new UScriptCodeMap(0x0000124A, 0x0000124D, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001250, 0x00001256, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001258, 0x00001258, ETHIOPIC ) ,
        new UScriptCodeMap(0x0000125A, 0x0000125D, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001260, 0x00001286, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001288, 0x00001288, ETHIOPIC ) ,
        new UScriptCodeMap(0x0000128A, 0x0000128D, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001290, 0x000012AE, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012B0, 0x000012B0, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012B2, 0x000012B5, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012B8, 0x000012BE, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012C0, 0x000012C0, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012C2, 0x000012C5, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012C8, 0x000012CE, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012D0, 0x000012D6, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012D8, 0x000012EE, ETHIOPIC ) ,
        new UScriptCodeMap(0x000012F0, 0x0000130E, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001310, 0x00001310, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001312, 0x00001315, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001318, 0x0000131E, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001320, 0x00001346, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001348, 0x0000135A, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001369, 0x00001371, ETHIOPIC ) ,
        new UScriptCodeMap(0x00001372, 0x0000137C, ETHIOPIC ) ,
        new UScriptCodeMap(0x000013A0, 0x000013F4, CHEROKEE ) ,
        new UScriptCodeMap(0x00001401, 0x0000166C, UCAS ) ,
        new UScriptCodeMap(0x0000166F, 0x00001676, UCAS ) ,
        new UScriptCodeMap(0x00001681, 0x0000169A, OGHAM ) ,
        new UScriptCodeMap(0x000016A0, 0x000016EA, RUNIC ) ,
        new UScriptCodeMap(0x000016EE, 0x000016F0, RUNIC ) ,
        new UScriptCodeMap(0x00001780, 0x000017B3, KHMER ) ,
        new UScriptCodeMap(0x000017B4, 0x000017B6, KHMER ) ,
        new UScriptCodeMap(0x000017B7, 0x000017BD, KHMER ) ,
        new UScriptCodeMap(0x000017BE, 0x000017C5, KHMER ) ,
        new UScriptCodeMap(0x000017C6, 0x000017C6, KHMER ) ,
        new UScriptCodeMap(0x000017C7, 0x000017C8, KHMER ) ,
        new UScriptCodeMap(0x000017C9, 0x000017D3, KHMER ) ,
        new UScriptCodeMap(0x000017E0, 0x000017E9, KHMER ) ,
        new UScriptCodeMap(0x00001810, 0x00001819, MONGOLIAN ) ,
        new UScriptCodeMap(0x00001820, 0x00001842, MONGOLIAN ) ,
        new UScriptCodeMap(0x00001843, 0x00001843, MONGOLIAN ) ,
        new UScriptCodeMap(0x00001844, 0x00001877, MONGOLIAN ) ,
        new UScriptCodeMap(0x00001880, 0x000018A8, MONGOLIAN ) ,
        new UScriptCodeMap(0x000018A9, 0x000018A9, MONGOLIAN ) ,
        new UScriptCodeMap(0x00001E00, 0x00001E9B, LATIN ) ,
        new UScriptCodeMap(0x00001EA0, 0x00001EF9, LATIN ) ,
        new UScriptCodeMap(0x00001F00, 0x00001F15, GREEK ) ,
        new UScriptCodeMap(0x00001F18, 0x00001F1D, GREEK ) ,
        new UScriptCodeMap(0x00001F20, 0x00001F45, GREEK ) ,
        new UScriptCodeMap(0x00001F48, 0x00001F4D, GREEK ) ,
        new UScriptCodeMap(0x00001F50, 0x00001F57, GREEK ) ,
        new UScriptCodeMap(0x00001F59, 0x00001F59, GREEK ) ,
        new UScriptCodeMap(0x00001F5B, 0x00001F5B, GREEK ) ,
        new UScriptCodeMap(0x00001F5D, 0x00001F5D, GREEK ) ,
        new UScriptCodeMap(0x00001F5F, 0x00001F7D, GREEK ) ,
        new UScriptCodeMap(0x00001F80, 0x00001FB4, GREEK ) ,
        new UScriptCodeMap(0x00001FB6, 0x00001FBC, GREEK ) ,
        new UScriptCodeMap(0x00001FBE, 0x00001FBE, GREEK ) ,
        new UScriptCodeMap(0x00001FC2, 0x00001FC4, GREEK ) ,
        new UScriptCodeMap(0x00001FC6, 0x00001FCC, GREEK ) ,
        new UScriptCodeMap(0x00001FD0, 0x00001FD3, GREEK ) ,
        new UScriptCodeMap(0x00001FD6, 0x00001FDB, GREEK ) ,
        new UScriptCodeMap(0x00001FE0, 0x00001FEC, GREEK ) ,
        new UScriptCodeMap(0x00001FF2, 0x00001FF4, GREEK ) ,
        new UScriptCodeMap(0x00001FF6, 0x00001FFC, GREEK ) ,
        new UScriptCodeMap(0x0000207F, 0x0000207F, LATIN ) ,
        new UScriptCodeMap(0x000020D0, 0x000020DC, INHERITED ) ,
        new UScriptCodeMap(0x000020DD, 0x000020E0, INHERITED ) ,
        new UScriptCodeMap(0x000020E1, 0x000020E1, INHERITED ) ,
        new UScriptCodeMap(0x000020E2, 0x000020E3, INHERITED ) ,
        new UScriptCodeMap(0x00002126, 0x00002126, GREEK ) ,
        new UScriptCodeMap(0x0000212A, 0x0000212B, LATIN ) ,
        new UScriptCodeMap(0x00002E80, 0x00002E99, HAN ) ,
        new UScriptCodeMap(0x00002E9B, 0x00002EF3, HAN ) ,
        new UScriptCodeMap(0x00002F00, 0x00002FD5, HAN ) ,
        new UScriptCodeMap(0x00003005, 0x00003005, HAN ) ,
        new UScriptCodeMap(0x00003007, 0x00003007, HAN ) ,
        new UScriptCodeMap(0x00003021, 0x00003029, HAN ) ,
        new UScriptCodeMap(0x0000302A, 0x0000302F, INHERITED ) ,
        new UScriptCodeMap(0x00003038, 0x0000303A, HAN ) ,
        new UScriptCodeMap(0x00003041, 0x00003094, HIRAGANA ) ,
        new UScriptCodeMap(0x00003099, 0x0000309A, INHERITED ) ,
        new UScriptCodeMap(0x0000309D, 0x0000309E, HIRAGANA ) ,
        new UScriptCodeMap(0x000030A1, 0x000030FA, KATAKANA ) ,
        new UScriptCodeMap(0x000030FD, 0x000030FE, KATAKANA ) ,
        new UScriptCodeMap(0x00003105, 0x0000312C, BOPOMOFO ) ,
        new UScriptCodeMap(0x00003131, 0x0000318E, HANGUL ) ,
        new UScriptCodeMap(0x000031A0, 0x000031B7, BOPOMOFO ) ,
        new UScriptCodeMap(0x00003400, 0x00004DB5, HAN ) ,
        new UScriptCodeMap(0x00004E00, 0x00009FA5, HAN ) ,
        new UScriptCodeMap(0x0000A000, 0x0000A48C, YI ) ,
        new UScriptCodeMap(0x0000A490, 0x0000A4A1, YI ) ,
        new UScriptCodeMap(0x0000A4A4, 0x0000A4B3, YI ) ,
        new UScriptCodeMap(0x0000A4B5, 0x0000A4C0, YI ) ,
        new UScriptCodeMap(0x0000A4C2, 0x0000A4C4, YI ) ,
        new UScriptCodeMap(0x0000A4C6, 0x0000A4C6, YI ) ,
        new UScriptCodeMap(0x0000AC00, 0x0000D7A3, HANGUL ) ,
        new UScriptCodeMap(0x0000F900, 0x0000FA2D, HAN ) ,
        new UScriptCodeMap(0x0000FB00, 0x0000FB06, LATIN ) ,
        new UScriptCodeMap(0x0000FB13, 0x0000FB17, ARMENIAN ) ,
        new UScriptCodeMap(0x0000FB1D, 0x0000FB1D, HEBREW ) ,
        new UScriptCodeMap(0x0000FB1E, 0x0000FB1E, INHERITED ) ,
        new UScriptCodeMap(0x0000FB1F, 0x0000FB28, HEBREW ) ,
        new UScriptCodeMap(0x0000FB2A, 0x0000FB36, HEBREW ) ,
        new UScriptCodeMap(0x0000FB38, 0x0000FB3C, HEBREW ) ,
        new UScriptCodeMap(0x0000FB3E, 0x0000FB3E, HEBREW ) ,
        new UScriptCodeMap(0x0000FB40, 0x0000FB41, HEBREW ) ,
        new UScriptCodeMap(0x0000FB43, 0x0000FB44, HEBREW ) ,
        new UScriptCodeMap(0x0000FB46, 0x0000FB4F, HEBREW ) ,
        new UScriptCodeMap(0x0000FB50, 0x0000FBB1, ARABIC ) ,
        new UScriptCodeMap(0x0000FBD3, 0x0000FD3D, ARABIC ) ,
        new UScriptCodeMap(0x0000FD50, 0x0000FD8F, ARABIC ) ,
        new UScriptCodeMap(0x0000FD92, 0x0000FDC7, ARABIC ) ,
        new UScriptCodeMap(0x0000FDF0, 0x0000FDFB, ARABIC ) ,
        new UScriptCodeMap(0x0000FE20, 0x0000FE23, INHERITED ) ,
        new UScriptCodeMap(0x0000FE70, 0x0000FE72, ARABIC ) ,
        new UScriptCodeMap(0x0000FE74, 0x0000FE74, ARABIC ) ,
        new UScriptCodeMap(0x0000FE76, 0x0000FEFC, ARABIC ) ,
        new UScriptCodeMap(0x0000FF21, 0x0000FF3A, LATIN ) ,
        new UScriptCodeMap(0x0000FF41, 0x0000FF5A, LATIN ) ,
        new UScriptCodeMap(0x0000FF66, 0x0000FF6F, KATAKANA ) ,
        new UScriptCodeMap(0x0000FF71, 0x0000FF9D, KATAKANA ) ,
        new UScriptCodeMap(0x0000FFA0, 0x0000FFBE, HANGUL ) ,
        new UScriptCodeMap(0x0000FFC2, 0x0000FFC7, HANGUL ) ,
        new UScriptCodeMap(0x0000FFCA, 0x0000FFCF, HANGUL ) ,
        new UScriptCodeMap(0x0000FFD2, 0x0000FFD7, HANGUL ) ,
        new UScriptCodeMap(0x0000FFDA, 0x0000FFDC, HANGUL ) ,
        new UScriptCodeMap(0x00010300, 0x0001031E, OLD_ITALIC ) ,
        new UScriptCodeMap(0x00010330, 0x00010349, GOTHIC ) ,
        new UScriptCodeMap(0x0001034A, 0x0001034A, GOTHIC ) ,
        new UScriptCodeMap(0x00010400, 0x00010425, DESERET ) ,
        new UScriptCodeMap(0x00010428, 0x0001044D, DESERET ) ,
        new UScriptCodeMap(0x0001D167, 0x0001D169, INHERITED ) ,
        new UScriptCodeMap(0x0001D17B, 0x0001D182, INHERITED ) ,
        new UScriptCodeMap(0x0001D185, 0x0001D18B, INHERITED ) ,
        new UScriptCodeMap(0x0001D1AA, 0x0001D1AD, INHERITED ) ,
        new UScriptCodeMap(0x00020000, 0x0002A6D6, HAN ) ,
    };

    /* binary search the string array */
    private static int findStringIndex(String[] sortedArr, String target){
        int size = sortedArr.length;
        int left, middle, right,rc;
        left =0;
        right= size-1;
        
        target = target.toUpperCase();
        while(left <= right){
            middle = (left+right)/2;
            rc=sortedArr[middle].toUpperCase().compareTo(target);
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
    private static int findCodeIndex(int[] unsorted, int target){
        int size = unsorted.length;
        int i=0;
        while(i<size){
            if(target == unsorted[i]){
                return i;
            }
            i++;
        }
        return -1;
    }

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
                result[w++] = scriptAbbrCodes[strIndex];
            }
        }

        if (w < result.length) {
            throw new InternalError("bad locale data, listed " + scripts.length + " scripts but found only " + w);
            /*
            int[] newResult = new int[w];
            System.arraycopy(result, 0, newResult, 0, w);
            result = newResult;
            */
        }

        return result;
    }

    /* binary search the string array */
    private static int findScriptCode(int codepoint){
        int left, middle, right,rc;
        left =0;
        
        right= scriptCodeIndex.length-1;

        while(left <= right){
            middle = (left+right)/2;
            /* check if the codepoint is the valid range */
            if(scriptCodeIndex[middle].firstCodepoint<= codepoint  && 
               scriptCodeIndex[middle].lastCodepoint>= codepoint){
                rc = 0;
            }else if(codepoint> scriptCodeIndex[middle].lastCodepoint){
                rc =-1;
            }else {
                rc = 1;
            }

            if(rc<0){
                left = middle+1;
            }else if(rc >0){
                right = middle -1;
            }else{
                return  scriptCodeIndex[middle].scriptCode;
            }
        }
        return COMMON;
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
            code[0] =  scriptNameCodes[strIndex];
        }
        /* we did not find in names array so try abbr array*/
        if(code[0] == INVALID_CODE){
            strIndex = findStringIndex(scriptAbbr, nameOrAbbrOrLocale);
            if(strIndex>=0 && strIndex < scriptAbbr.length){ 
                code[0] =  scriptAbbrCodes[strIndex];
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
            return findScriptCode(codepoint);
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
        index = findCodeIndex(scriptNameCodes,scriptCode);
        if(index >=0){
            return scriptNames[index];
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
        index = findCodeIndex(scriptAbbrCodes,scriptCode);
        if(index >=0){
            return scriptAbbr[index];
        }else{
            throw new IllegalArgumentException(Integer.toString(scriptCode));
        }
    }
}

