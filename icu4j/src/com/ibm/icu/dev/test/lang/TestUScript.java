/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.test.text;

import com.ibm.text.UScript;
import com.ibm.test.TestFmwk;
import java.util.Locale;

public class TestUScript extends TestFmwk{
  
    /**
    * Constructor
    */
    public TestUScript()
    {
    }
    
    public void TestLocaleGetCode(){
        final Locale[] testNames={
        /* test locale */
        new Locale("en",""), new Locale("en","US"),
        new Locale("sr",""), new Locale("ta","") , 
        new Locale("te","IN"),
        new Locale("hi",""), 
        new Locale("he",""), new Locale("ar",""),
        };
        final int[] expected ={
                /* locales should return */
                UScript.USCRIPT_LATIN, UScript.USCRIPT_LATIN, 
                UScript.USCRIPT_CYRILLIC, UScript.USCRIPT_TAMIL, 
                UScript.USCRIPT_TELUGU,UScript.USCRIPT_DEVANAGARI, 
                UScript.USCRIPT_HEBREW, UScript.USCRIPT_ARABIC,
        };
        int i =0;
        int numErrors =0;

        for( ; i<testNames.length; i++){
            int code = UScript.getCode(testNames[i]);
            if( code != expected[i]){
                logln("Error getting script code Got: " +code + " Expected: " +expected[i] +" for name "+testNames[i]);
                numErrors++;
            }
        }
        if(numErrors >0 ){
            errln("Number of Errors in UScript.getCode() : " + numErrors);
        }            
    }
    
    public void TestGetCode(){

        final String[] testNames={
            /* test locale */                
            "en", "en_US", "sr", "ta" , "te_IN",                
            "hi", "he", "ar",
            /* test abbr */
            "Hani", "Hang","Hebr","Hira",
            "Knda","Kana","Khmr","Lao",
            "Latn",/*"Latf","Latg",*/ 
            "Mlym", "Mong",
                
            /* test names */
            "CYRILLIC","DESERET","DEVANAGARI","ETHIOPIC","GEORGIAN", 
            "GOTHIC",  "GREEK",  "GUJARATI", 
            /* test lower case names */
            "malayalam", "mongolian", "myanmar", "ogham", "old-italic",
            "oriya",     "runic",     "sinhala", "syriac","tamil",     
            "telugu",    "thaana",    "thai",    "tibetan", 
            /* test the bounds*/
            "ucas", "arabic",
        };
        final int[] expected ={
            /* locales should return */
            UScript.USCRIPT_LATIN, UScript.USCRIPT_LATIN, 
            UScript.USCRIPT_CYRILLIC, UScript.USCRIPT_TAMIL, 
            UScript.USCRIPT_TELUGU,UScript.USCRIPT_DEVANAGARI, 
            UScript.USCRIPT_HEBREW, UScript.USCRIPT_ARABIC,
            /* abbr should return */
            UScript.USCRIPT_HAN, UScript.USCRIPT_HANGUL, UScript.USCRIPT_HEBREW, UScript.USCRIPT_HIRAGANA,
            UScript.USCRIPT_KANNADA, UScript.USCRIPT_KATAKANA, UScript.USCRIPT_KHMER, UScript.USCRIPT_LAO,
            UScript.USCRIPT_LATIN,/* UScript.USCRIPT_LATIN, UScript.USCRIPT_LATIN,*/ 
            UScript.USCRIPT_MALAYALAM, UScript.USCRIPT_MONGOLIAN,
            /* names should return */
            UScript.USCRIPT_CYRILLIC, UScript.USCRIPT_DESERET, UScript.USCRIPT_DEVANAGARI, UScript.USCRIPT_ETHIOPIC, UScript.USCRIPT_GEORGIAN,
            UScript.USCRIPT_GOTHIC, UScript.USCRIPT_GREEK, UScript.USCRIPT_GUJARATI,
            /* lower case names should return */    
            UScript.USCRIPT_MALAYALAM, UScript.USCRIPT_MONGOLIAN, UScript.USCRIPT_MYANMAR, UScript.USCRIPT_OGHAM, UScript.USCRIPT_OLD_ITALIC,
            UScript.USCRIPT_ORIYA, UScript.USCRIPT_RUNIC, UScript.USCRIPT_SINHALA, UScript.USCRIPT_SYRIAC, UScript.USCRIPT_TAMIL,
            UScript.USCRIPT_TELUGU, UScript.USCRIPT_THAANA, UScript.USCRIPT_THAI, UScript.USCRIPT_TIBETAN,
            /* bounds */
            UScript.USCRIPT_UCAS, UScript.USCRIPT_ARABIC,
        };
        int i =0;
        int numErrors =0;

        for( ; i<testNames.length; i++){
            int code = UScript.getCode(testNames[i]);
            if( code != expected[i]){
                logln("Error getting script code Got: " +code + " Expected: " +expected[i] +" for name "+testNames[i]);
                numErrors++;
            }
        }
        if(numErrors >0 ){
            errln("Number of Errors in UScript.getCode() : " + numErrors);
        }
    }
    public void TestGetName(){
        
        final int[] testCodes={
            /* names should return */
            UScript.USCRIPT_CYRILLIC, UScript.USCRIPT_DESERET, UScript.USCRIPT_DEVANAGARI, UScript.USCRIPT_ETHIOPIC, UScript.USCRIPT_GEORGIAN,
            UScript.USCRIPT_GOTHIC, UScript.USCRIPT_GREEK, UScript.USCRIPT_GUJARATI,
        };

        final String[] expectedNames={
              
            /* test names */
            "CYRILLIC","DESERET","DEVANAGARI","ETHIOPIC","GEORGIAN", 
            "GOTHIC",  "GREEK",  "GUJARATI", 
        };
        int i =0;

        while(i< testCodes.length){
            String scriptName  = UScript.getName(testCodes[i]);
            int numErrors=0;
            if(!expectedNames[i].equals(scriptName)){
                logln("Error getting abbreviations Got: " +scriptName +" Expected: "+expectedNames[i]);
                numErrors++;
            }
            if(numErrors > 0){
                if(numErrors >0 ){
                    errln("Errors UScript.getShorName() : " + numErrors);
                }
            }
            i++;
        }

    }
    public void TestGetShortName(){
        final int[] testCodes={
            /* abbr should return */
            UScript.USCRIPT_HAN, UScript.USCRIPT_HANGUL, UScript.USCRIPT_HEBREW, UScript.USCRIPT_HIRAGANA,
            UScript.USCRIPT_KANNADA, UScript.USCRIPT_KATAKANA, UScript.USCRIPT_KHMER, UScript.USCRIPT_LAO,
            UScript.USCRIPT_LATIN, 
            UScript.USCRIPT_MALAYALAM, UScript.USCRIPT_MONGOLIAN,
        };

        final String[] expectedAbbr={
              /* test abbr */
            "Hani", "Hang","Hebr","Hira",
            "Knda","Kana","Khmr","Lao",
            "Latn",
            "Mlym", "Mong",
        };
        int i=0;
        while(i<testCodes.length){
            String  shortName = UScript.getShortName(testCodes[i]);
            int numErrors=0;
            if(!expectedAbbr[i].equals(shortName)){
                logln("Error getting abbreviations Got: " +shortName+ " Expected: " +expectedAbbr[i]);
                numErrors++;
            }
            if(numErrors > 0){
                if(numErrors >0 ){
                    errln("Errors UChar.getScriptAbbr() : "+numErrors);
                }
            }
            i++;
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
                UScript.USCRIPT_KATAKANA ,
                UScript.USCRIPT_HANGUL ,
                UScript.USCRIPT_HANGUL ,
                UScript.USCRIPT_HANGUL ,
                UScript.USCRIPT_HANGUL ,
                UScript.USCRIPT_HANGUL ,
                UScript.USCRIPT_OLD_ITALIC, 
                UScript.USCRIPT_GOTHIC ,
                UScript.USCRIPT_GOTHIC ,
                UScript.USCRIPT_DESERET ,
                UScript.USCRIPT_DESERET ,
                UScript.USCRIPT_INHERITED,
                UScript.USCRIPT_INHERITED,
                UScript.USCRIPT_INHERITED,
                UScript.USCRIPT_INHERITED,
                UScript.USCRIPT_HAN ,
                UScript.USCRIPT_MALAYALAM,
                UScript.USCRIPT_INVALID_CODE,
                UScript.USCRIPT_COMMON,
                UScript.USCRIPT_INHERITED ,
                UScript.USCRIPT_INHERITED ,
                UScript.USCRIPT_INHERITED ,
                UScript.USCRIPT_INHERITED ,
        };
        int i =0;
        int code = UScript.USCRIPT_INVALID_CODE;
        boolean passed = true;

        while(i< codepoints.length){
            code = UScript.getScript(codepoints[i]);

            if(code != expected[i]){
                logln("UScript.USCRIPT_getScript for codepoint 0x"+ hex(codepoints[i])+" failed\n");
                passed = false;
            }

            i++;
        }
        if(!passed){
           errln("UScript.USCRIPT_getScript failed.");
        }      
    }
 }