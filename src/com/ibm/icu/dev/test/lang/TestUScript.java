/**
*******************************************************************************
* Copyright (C) 1996-2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.test.lang;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.dev.test.TestFmwk;

import java.util.Locale;

public class TestUScript extends TestFmwk {

    /**
    * Constructor
    */
    public TestUScript()
    {
    }

    public static void main(String[] args) throws Exception {
        new TestUScript().run(args);
    }
    public void TestLocaleGetCode(){
        final ULocale[] testNames={
        /* test locale */
        new ULocale("en"), new ULocale("en_US"),
        new ULocale("sr"), new ULocale("ta") ,
        new ULocale("te_IN"),
        new ULocale("hi"),
        new ULocale("he"), new ULocale("ar"),
        new ULocale("abcde"),
        new ULocale("abcde_cdef"),
        new ULocale("iw")
        };
        final int[] expected ={
                /* locales should return */
                UScript.LATIN, UScript.LATIN,
                UScript.CYRILLIC, UScript.TAMIL,
                UScript.TELUGU,UScript.DEVANAGARI,
                UScript.HEBREW, UScript.ARABIC,
                UScript.INVALID_CODE,UScript.INVALID_CODE,
                UScript.HEBREW
        };
        int i =0;
        int numErrors =0;

        for( ; i<testNames.length; i++){
            int[] code = UScript.getCode(testNames[i]);

            if(code==null){
                if(expected[i]!=UScript.INVALID_CODE){
                    logln("Error getting script code Got: null" + " Expected: " +expected[i] +" for name "+testNames[i]);
                    numErrors++;
                }
                // getCode returns null if the code could not be found
                continue;
            }
            if((code[0] != expected[i])){
                logln("Error getting script code Got: " +code[0] + " Expected: " +expected[i] +" for name "+testNames[i]);
                numErrors++;
            }
        }
        reportDataErrors(numErrors);
        
        // 
        ULocale defaultLoc = ULocale.getDefault(); 
        ULocale esparanto = new ULocale("eo_DE");
        ULocale.setDefault(esparanto);
        int[] code = UScript.getCode(esparanto); 
        if(code != null){
            if( code[0] != UScript.LATIN){
                errln("Did not get the expected script code for Esparanto");
            }
        }else{
            warnln("Could not load the locale data.");
        }
        ULocale.setDefault(defaultLoc);
    }

    private void reportDataErrors(int numErrors) {
        if (numErrors >0) {
            // assume missing locale data, so not an error, just a warning
            if (isModularBuild() || noData()) {
                // if nodata is set don't even warn
                warnln("Could not find locale data");
            } else {
                errln("encountered " + numErrors + " errors.");
            }
        }
    }

    public void TestMultipleCode(){
        final String[] testNames = { "ja" ,"ko_KR","zh","zh_TW"};
        final int[][] expected = {
                                {UScript.KATAKANA,UScript.HIRAGANA,UScript.HAN},
                                {UScript.HANGUL, UScript.HAN},
                                {UScript.HAN},
                                {UScript.HAN,UScript.BOPOMOFO}
                              };

        int numErrors = 0;
        for(int i=0; i<testNames.length;i++){
            int[] code = UScript.getCode(testNames[i]);
            int[] expt = (int[]) expected[i];
            if(code!=null){
                for(int j =0; j< code.length;j++){
                    if(code[j]!=expt[j]){
                        numErrors++;
                        logln("Error getting script code Got: " +code[j] + " Expected: " +expt[j] +" for name "+testNames[i]);
                    }
                }
            }else{
                numErrors++;
                logln("Error getting script code for name "+testNames[i]);
            }
        }
        reportDataErrors(numErrors);
        
        //cover UScript.getCode(Locale)
        Locale[] testLocales = new Locale[] {
            Locale.JAPANESE,
            Locale.KOREA,
            Locale.CHINESE,
            Locale.TAIWAN };
        logln("Testing UScript.getCode(Locale) ...");
        numErrors = 0;
        for(int i=0; i<testNames.length;i++){
            logln("  Testing locale: " + testLocales[i].getDisplayName());
            int[] code = UScript.getCode(testLocales[i]);
            int[] expt = (int[]) expected[i];
            if(code!=null){
                for(int j =0; j< code.length;j++){
                    if(code[j]!=expt[j]){
                        numErrors++;
                        logln("  Error getting script code Got: " +code[j] + " Expected: " +expt[j] +" for name "+testNames[i]);
                    }
                }
            }else{
                numErrors++;
                logln("  Error getting script code for name "+testNames[i]);
            }
        }
        reportDataErrors(numErrors);                 
    }

    public void TestGetCode(){

        final String[] testNames={
            /* test locale */
            "en", "en_US", "sr", "ta", "gu", "te_IN", 
            "hi", "he", "ar",
            /* test abbr */
            "Hani", "Hang","Hebr","Hira",
            "Knda","Kana","Khmr","Lao",
            "Latn",/*"Latf","Latg",*/
            "Mlym", "Mong",

            /* test names */
            "CYRILLIC","DESERET","DEVANAGARI","ETHIOPIC","GEORGIAN",
            "GOTHIC",  "GREEK",  "GUJARATI", "COMMON", "INHERITED",
            /* test lower case names */
            "malayalam", "mongolian", "myanmar", "ogham", "old-italic",
            "oriya",     "runic",     "sinhala", "syriac","tamil",
            "telugu",    "thaana",    "thai",    "tibetan",
            /* test the bounds*/
            "Cans", "arabic","Yi","Zyyy"
        };
        final int[] expected ={
            /* locales should return */
            UScript.LATIN, UScript.LATIN,
            UScript.CYRILLIC, UScript.TAMIL, UScript.GUJARATI,
            UScript.TELUGU,UScript.DEVANAGARI,
            UScript.HEBREW, UScript.ARABIC,
            /* abbr should return */
            UScript.HAN, UScript.HANGUL, UScript.HEBREW, UScript.HIRAGANA,
            UScript.KANNADA, UScript.KATAKANA, UScript.KHMER, UScript.LAO,
            UScript.LATIN,/* UScript.LATIN, UScript.LATIN,*/
            UScript.MALAYALAM, UScript.MONGOLIAN,
            /* names should return */
            UScript.CYRILLIC, UScript.DESERET, UScript.DEVANAGARI, UScript.ETHIOPIC, UScript.GEORGIAN,
            UScript.GOTHIC, UScript.GREEK, UScript.GUJARATI, UScript.COMMON, UScript.INHERITED,
            /* lower case names should return */
            UScript.MALAYALAM, UScript.MONGOLIAN, UScript.MYANMAR, UScript.OGHAM, UScript.OLD_ITALIC,
            UScript.ORIYA, UScript.RUNIC, UScript.SINHALA, UScript.SYRIAC, UScript.TAMIL,
            UScript.TELUGU, UScript.THAANA, UScript.THAI, UScript.TIBETAN,
            /* bounds */
            UScript.CANADIAN_ABORIGINAL, UScript.ARABIC, UScript.YI, UScript.COMMON
        };
        int i =0;
        int numErrors =0;

        for( ; i<testNames.length; i++){
            int[] code = UScript.getCode(testNames[i]);
            if(code == null){
                if(expected[i]==UScript.INVALID_CODE){
                    // getCode returns null if the code could not be found
                    continue;
                }
                // currently commented out until jitterbug#2678 is fixed
                logln("Error getting script code Got: null" + " Expected: " +expected[i] +" for name "+testNames[i]);
                numErrors++;
                continue;
            }
            if((code[0] != expected[i])){
                logln("Error getting script code Got: " +code[0] + " Expected: " +expected[i] +" for name "+testNames[i]);
                numErrors++;
            }
        }
        reportDataErrors(numErrors);
    }

    public void TestGetName(){

        final int[] testCodes={
            /* names should return */
            UScript.CYRILLIC, UScript.DESERET, UScript.DEVANAGARI, UScript.ETHIOPIC, UScript.GEORGIAN,
            UScript.GOTHIC, UScript.GREEK, UScript.GUJARATI,
        };

        final String[] expectedNames={

            /* test names */
            "Cyrillic","Deseret","Devanagari","Ethiopic","Georgian",
            "Gothic",  "Greek",  "Gujarati",
        };
        int i =0;
        int numErrors=0;
        while(i< testCodes.length){
            String scriptName  = UScript.getName(testCodes[i]);
            if(!expectedNames[i].equals(scriptName)){
                logln("Error getting abbreviations Got: " +scriptName +" Expected: "+expectedNames[i]);
                numErrors++;
            }
            i++;
        }
        if(numErrors >0 ){
            warnln("encountered " + numErrors + " errors in UScript.getName()");
        }

    }
    public void TestGetShortName(){
        final int[] testCodes={
            /* abbr should return */
            UScript.HAN, UScript.HANGUL, UScript.HEBREW, UScript.HIRAGANA,
            UScript.KANNADA, UScript.KATAKANA, UScript.KHMER, UScript.LAO,
            UScript.LATIN,
            UScript.MALAYALAM, UScript.MONGOLIAN,
        };

        final String[] expectedAbbr={
              /* test abbr */
            "Hani", "Hang","Hebr","Hira",
            "Knda","Kana","Khmr","Laoo",
            "Latn",
            "Mlym", "Mong",
        };
        int i=0;
        int numErrors=0;
        while(i<testCodes.length){
            String  shortName = UScript.getShortName(testCodes[i]);
            if(!expectedAbbr[i].equals(shortName)){
                logln("Error getting abbreviations Got: " +shortName+ " Expected: " +expectedAbbr[i]);
                numErrors++;
            }
            i++;
        }
        if(numErrors >0 ){
            warnln("encountered " + numErrors + " errors in UScript.getShortName()");
        }
    }
    public void TestGetScript(){
        int codepoints[][] = new int[][] {
                {0x0000FF9D, UScript.KATAKANA },
                {0x0000FFBE, UScript.HANGUL },
                {0x0000FFC7, UScript.HANGUL },
                {0x0000FFCF, UScript.HANGUL },
                {0x0000FFD7, UScript.HANGUL}, 
                {0x0000FFDC, UScript.HANGUL},
                {0x00010300, UScript.OLD_ITALIC},
                {0x00010330, UScript.GOTHIC},
                {0x0001034A, UScript.GOTHIC},
                {0x00010400, UScript.DESERET},
                {0x00010428, UScript.DESERET},
                {0x0001D167, UScript.INHERITED},
                {0x0001D17B, UScript.INHERITED},
                {0x0001D185, UScript.INHERITED},
                {0x0001D1AA, UScript.INHERITED},
                {0x00020000, UScript.HAN},
                {0x00000D02, UScript.MALAYALAM},
                {0x00000D00, UScript.UNKNOWN},
                {0x00000000, UScript.COMMON},
                {0x0001D169, UScript.INHERITED },
                {0x0001D182, UScript.INHERITED },
                {0x0001D18B, UScript.INHERITED },
                {0x0001D1AD, UScript.INHERITED },
        };

        int i =0;
        int code = UScript.INVALID_CODE;
        boolean passed = true;

        while(i< codepoints.length){
            code = UScript.getScript(codepoints[i][0]);

            if(code != codepoints[i][1]){
                logln("UScript.getScript for codepoint 0x"+ hex(codepoints[i][0])+" failed");
                passed = false;
            }

            i++;
        }
        if(!passed){
           errln("UScript.getScript failed.");
        }
    }
    public void TestScriptNames(){
        for(int i=0; i<UScript.CODE_LIMIT;i++){
            String name = UScript.getName(i);
            if(name.equals("") ){
                errln("FAILED: getName for code : "+i);
            }
            String shortName= UScript.getShortName(i);
            if(shortName.equals("")){
                errln("FAILED: getName for code : "+i);
            }
        }
    }
    public void TestAllCodepoints(){
        int code;
        //String oldId="";
        //String oldAbbrId="";
        for( int i =0; i <= 0x10ffff; i++){
          code =UScript.INVALID_CODE;
          code = UScript.getScript(i);
          if(code==UScript.INVALID_CODE){
                errln("UScript.getScript for codepoint 0x"+ hex(i)+" failed");
          }
          String id =UScript.getName(code);
          if(id.indexOf("INVALID")>=0){
                 errln("UScript.getScript for codepoint 0x"+ hex(i)+" failed");
          }
          String abbr = UScript.getShortName(code);
          if(abbr.indexOf("INV")>=0){
                 errln("UScript.getScript for codepoint 0x"+ hex(i)+" failed");
          }
        }
    }
    public void TestNewCode(){
        /*
         * These script codes were originally added to ICU pre-3.6, so that ICU would
         * have all ISO 15924 script codes. ICU was then based on Unicode 4.1.
         * These script codes were added with only short names because we don't
         * want to invent long names ourselves.
         * Unicode 5 and later encode some of these scripts and give them long names.
         * Whenever this happens, the long script names here need to be updated.
         */
        String[] expectedLong = new String[]{
            "Balinese", "Batk", "Blis", "Brah", "Cham", "Cirt", "Cyrs", "Egyd", "Egyh", "Egyp", 
            "Geok", "Hans", "Hant", "Hmng", "Hung", "Inds", "Java", "Kali", "Latf", "Latg", 
            "Lepc", "Lina", "Mand", "Maya", "Mero", "Nko", "Orkh", "Perm", "Phags_Pa", "Phoenician", 
            "Plrd", "Roro", "Sara", "Syre", "Syrj", "Syrn", "Teng", "Vaii", "Visp", "Cuneiform", 
            "Zxxx", "Unknown",
            "Cari", "Jpan", "Lana", "Lyci", "Lydi", "Olck", "Rjng", "Saur", "Sgnw", "Sund",
            "Moon", "Mtei",
        };
        String[] expectedShort = new String[]{
            "Bali", "Batk", "Blis", "Brah", "Cham", "Cirt", "Cyrs", "Egyd", "Egyh", "Egyp", 
            "Geok", "Hans", "Hant", "Hmng", "Hung", "Inds", "Java", "Kali", "Latf", "Latg", 
            "Lepc", "Lina", "Mand", "Maya", "Mero", "Nkoo", "Orkh", "Perm", "Phag", "Phnx", 
            "Plrd", "Roro", "Sara", "Syre", "Syrj", "Syrn", "Teng", "Vaii", "Visp", "Xsux", 
            "Zxxx", "Zzzz",
            "Cari", "Jpan", "Lana", "Lyci", "Lydi", "Olck", "Rjng", "Saur", "Sgnw", "Sund",
            "Moon", "Mtei",
        };
        int j = 0;
        int i = 0;
        for(i=UScript.BALINESE; i<UScript.CODE_LIMIT; i++, j++){
            String name = UScript.getName(i);
            if(name==null || !name.equals(expectedLong[j])){
                errln("UScript.getName failed for code"+ i + name +"!=" +expectedLong[j]);
            }
            name = UScript.getShortName(i);
            if(name==null || !name.equals(expectedShort[j])){
                errln("UScript.getShortName failed for code"+ i + name +"!=" +expectedShort[j]);
            }
        }
        for(i=0; i<expectedLong.length; i++){
            int[] ret = UScript.getCode(expectedShort[i]);
            if(ret.length>1){
                errln("UScript.getCode did not return expected number of codes for script"+ expectedShort[i]+". EXPECTED: 1 GOT: "+ ret.length);
            }
            if(ret[0]!= (UScript.BALINESE+i)){
                errln("UScript.getCode did not return expected code for script"+ expectedShort[i]+". EXPECTED: "+ (UScript.BALINESE+i)+" GOT: %i\n"+ ret[0] );
            }
        }
    }
 }
