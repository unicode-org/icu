/**
*******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.test.lang;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.dev.test.TestFmwk;

import java.util.Locale;
import java.util.MissingResourceException;

public class TestUScript extends TestFmwk{

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
        final Locale[] testNames={
        /* test locale */
        new Locale("en",""), new Locale("en","US"),
        new Locale("sr",""), new Locale("ta","") ,
        new Locale("te","IN"),
        new Locale("hi",""),
        new Locale("he",""), new Locale("ar",""),
        new Locale("abcde",""),
        new Locale("abcde","cdef")
        };
        final int[] expected ={
                /* locales should return */
                UScript.LATIN, UScript.LATIN,
                UScript.CYRILLIC, UScript.TAMIL,
                UScript.TELUGU,UScript.DEVANAGARI,
                UScript.HEBREW, UScript.ARABIC,
                UScript.INVALID_CODE,UScript.INVALID_CODE
        };
        int i =0;
        int numErrors =0;
        try{
            for( ; i<testNames.length; i++){
                int[] code = UScript.getCode(testNames[i]);
                if(code == null && expected[i]==UScript.INVALID_CODE){
                    // getCode returns null if the code could not be found
                    continue;
                }
                if((code[0] != expected[i])){
                    logln("Error getting script code Got: " +code[0] + " Expected: " +expected[i] +" for name "+testNames[i]);
                    numErrors++;
                }
            }
            if(numErrors >0 ){
                errln("Number of Errors in UScript.getCode() : " + numErrors);
            }
        }catch(MissingResourceException e){
            warnln("Could not find locale data: " + e.getMessage());
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
        try{
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
                }
            }
            if(numErrors >0 ){
                errln("Number of Errors in UScript.getCode() : " + numErrors);
            }
        }catch(MissingResourceException e){
            warnln("Could not find locale data: " + e.getMessage());
        }
    }
    public void TestGetCode(){

        final String[] testNames={
            /* test locale */
            "en", "en_US", "sr", "ta","  ___    ---ta" , "te_IN",
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
            "ucas", "arabic","Yi","Zyyy"
        };
        final int[] expected ={
            /* locales should return */
            UScript.LATIN, UScript.LATIN,
            UScript.CYRILLIC, UScript.TAMIL, UScript.TAMIL,
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
            UScript.UCAS, UScript.ARABIC, UScript.YI, UScript.COMMON
        };
        int i =0;
        int numErrors =0;
        try{
            for( ; i<testNames.length; i++){
                int[] code = UScript.getCode(testNames[i]);
                if(code == null){
                    if(expected[i]==UScript.INVALID_CODE){
                        // getCode returns null if the code could not be found
                        continue;
                    }
                    // currently commented out until jitterbug#2678 is fixed
                    // logln("Error getting script code Got: null" + " Expected: " +expected[i] +" for name "+testNames[i]);
                    // numErrors++;
                    continue;
                }
                if((code[0] != expected[i])){
                    logln("Error getting script code Got: " +code[0] + " Expected: " +expected[i] +" for name "+testNames[i]);
                    numErrors++;
                }
            }
            if(numErrors >0 ){
                errln("Number of Errors in UScript.getCode() : " + numErrors);
            }
        }catch(MissingResourceException e){
            warnln("Could not find locale data: " + e.getMessage());
        }

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
        int codepoints[] = {
                0x0000FF9D,
                0x0000FFBE,
                0x0000FFC7,
                0x0000FFCF,
                0x0000FFD7,
                0x0000FFDC,
                0x00010300,
                0x00010330,
                0x0001034A,
                0x00010400,
                0x00010428,
                0x0001D167,
                0x0001D17B,
                0x0001D185,
                0x0001D1AA,
                0x00020000,
                0x00000D02,
                0x00000D00,
                0x00000000,
                0x0001D169,
                0x0001D182,
                0x0001D18B,
                0x0001D1AD,
        };

        int expected[] = {
                UScript.KATAKANA ,
                UScript.HANGUL ,
                UScript.HANGUL ,
                UScript.HANGUL ,
                UScript.HANGUL ,
                UScript.HANGUL ,
                UScript.OLD_ITALIC,
                UScript.GOTHIC ,
                UScript.GOTHIC ,
                UScript.DESERET ,
                UScript.DESERET ,
                UScript.INHERITED,
                UScript.INHERITED,
                UScript.INHERITED,
                UScript.INHERITED,
                UScript.HAN ,
                UScript.MALAYALAM,
                UScript.COMMON,
                UScript.COMMON,
                UScript.INHERITED ,
                UScript.INHERITED ,
                UScript.INHERITED ,
                UScript.INHERITED ,
        };
        int i =0;
        int code = UScript.INVALID_CODE;
        boolean passed = true;

        while(i< codepoints.length){
            code = UScript.getScript(codepoints[i]);

            if(code != expected[i]){
                logln("UScript.getScript for codepoint 0x"+ hex(codepoints[i])+" failed");
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
 }
