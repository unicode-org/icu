
/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/Attic/HexToUnicodeTransliteratorTest.java,v $ 
 * $Date: 2002/08/28 16:45:18 $ 
 * $Revision: 1.6 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.translit;
import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import com.ibm.icu.dev.test.*;
import com.ibm.icu.impl.Utility;
import java.text.*;
import java.util.*;


/**
 * @test
 * @summary General test of HexToUnicodeTransliterator
 */
public class HexToUnicodeTransliteratorTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new HexToUnicodeTransliteratorTest().run(args);
    }

    /**
    * Used by TestConstruction() and TestTransliterate.
    */
    UnicodeFilter HexFilter=new UnicodeFilter() {
        public boolean contains(int c) {
            if(c == 0x0061 || c == 0x0063 )
                return false;
            else
                return true;
        }
        public String toPattern(boolean escapeUnprintable) {
            return "";
        }
        public boolean matchesIndexValue(int v) {
            return false;
        }
        public void addMatchSetTo(UnicodeSet toUnionTo) {}
    };

    public void TestConstruction(){
        logln("Testing the construction HexToUnicodeTransliterator()");
        HexToUnicodeTransliterator trans1=new HexToUnicodeTransliterator();
       

        logln("Testing the cosntruction HexToUnicodeTransliterator(pattern)");
        String pattern="\\\\U+0000abc";
        try{
            trans1=new HexToUnicodeTransliterator(pattern);
        }catch(IllegalArgumentException ex2) {
            errln("FAIL: HexToUnicodeTransliterator construction failed with pattern=" + pattern + " Exception= " + ex2.toString());
        }

        logln("Testing the cosntruction HexToUnicodeTransliterator(pattern) with illegal pattern");
        String pattern2="\\X+";
        try{
            trans1=new HexToUnicodeTransliterator(pattern2);
        }catch(IllegalArgumentException ex3) {
            logln("OK: construction with Illegal pattern handled correctly");
        }  
        
        logln("Testing the construction HexToUnicodeTransliterator(pattern, adoptedFilter=null)");
        try{
            trans1=new HexToUnicodeTransliterator(pattern, null);
        }catch(IllegalArgumentException ex4) {
            errln("FAIL: HexToUnicodeTransliterator(pattern, adoptedFilter=null) construction failed. Exception= " + ex4.toString());
        }

        logln("Testing the construction HexToUnicodeTransliterator(pattern, adoptedFilter)");
        try{
            trans1=new HexToUnicodeTransliterator(pattern, HexFilter);
        }catch(IllegalArgumentException ex4) {
            errln("FAIL: HexToUnicodeTransliterator(pattern, adoptedFilter) construction failed. Exception= " + ex4.toString());
        }

    }
    public void TestPattern(){
        logln("Testing the applyPattern() and toPattern() API of HexToUnicodeTransliterator");
        /*default transliterator has pattern \\u0000*/
        HexToUnicodeTransliterator transdefault=new HexToUnicodeTransliterator();
                
        String defaultpattern=transdefault.toPattern();
        String pattern1="\\\\U+0000";
        
        HexToUnicodeTransliterator trans1=null;
        try{
            trans1=new HexToUnicodeTransliterator(pattern1, null);
        }catch(IllegalArgumentException ex1) {
            errln("FAIL: HexToUnicodeTransliterator construction failed with pattern =" + pattern1 +
                   "Exception:" + ex1.toString());
        }

        /*test toPattern() */
        if(transdefault.toPattern().equals(trans1.toPattern()) == true ||
            transdefault.toPattern().equals("\\\\u0000;\\\\U0000;u+0000;U+0000") != true ||
            trans1.toPattern().equals(pattern1) != true ){
            errln("Error: toPattern() failed");
        }

        /*apply patterns for transdefault*/
        String str="abKf";
        expectPattern(transdefault,  pattern1, "\\U+0061\\U+0062\\U+004B\\U+0066", str);
        expectPattern(transdefault,  "\\U##00,", "U61,U62,U4B,U66,", str);
        expectPattern(transdefault, defaultpattern, "\\u0061\\u0062\\u004B\\u0066", str);
        expectPattern(trans1, "\\uni0000", "uni0061uni0062uni004Buni0066", str);
        expectPattern(trans1, "\\\\S-0000-E", "\\S-0061-E\\S-0062-E\\S-004B-E\\S-0066-E", str);
        
        expectPattern(trans1, "\\\\S-0000-E", "\\S-0061-E\\S-0062-E\\S-004B-E\\S-0066-E",  str);
        expectPattern(trans1, "\\u##0000", "\\u##0061\\u##0062", "FAIL");
        expectPattern(trans1, "\\*0000",  "*0061*0062*004B*0066",  str); 
        expectPattern(trans1, "\\u####", "\\u##0061\\u##0062", "FAIL");
  

    }

    public void TestSimpleTransliterate(){ 
        String pattern1="\\\\U+0000";
        HexToUnicodeTransliterator trans1=null;
        try{
            trans1=new HexToUnicodeTransliterator(pattern1, null);
        }catch(IllegalArgumentException iae){
            errln("HexToUnicodeTransliterator(pattern1, null) construction failed with pattern =" 
                  + pattern1 + "Exception:" + iae.toString());
        }
        String source="He\\U+006C\\U+006C\\U+006F";
        String expected="Hello";

        expectTranslit(trans1, ":Replaceable ", source,  1, source.length(), 2, expected); 
        expect(trans1, "", "\\U+0048\\U+0065\\U+006C\\U+006C\\U+006F", expected);

        HexToUnicodeTransliterator transdefault=new HexToUnicodeTransliterator();
        
        HexToUnicodeTransliterator trans2=null;
        try{
            trans2=new HexToUnicodeTransliterator(transdefault.toPattern(), HexFilter);
        }catch(IllegalArgumentException ex2){
            errln("HexToUnicodeTransliterator(pattern1, HexFilter) construction failed with pattern =" 
                  + pattern1 + "Exception:" + ex2.toString());
        }
        expect(trans2, "with Filter(0x0061, 0x0063) ", "\u0061\u0062\u0063",   "\u0061b\u0063");
    }
   
    public void TestTransliterate(){
        String Data[]={
            //pattern, source, index.contextStart, index.contextLimit, index.start, expectedResult,
       //     "U+##00",    "abU+63", "1", "7", "2",  "abc", 
            "\\\\u0000", "a\\u0062c", "1", "7", "1",  "abc", 
            "Uni0000",   "abUni0063", "1", "9", "2",  "abc", 
            "U[0000]",   "heU[006C]U[006C]o", "0", "16", "2", "hello", 
       //     "prefix-0000-suffix", "aprefix-0062-suffixprefix-0063-suffix", "1", "39", "1", "abc", 
            "*##00*",    "hell*6F**74**68**65*re",  "1", "20", "4", "hellothere", 

        };
        HexToUnicodeTransliterator trans1=null;
        for(int i=0; i<Data.length; i+=6){
            try{
                trans1=new HexToUnicodeTransliterator(Data[i+0], null);
            }catch(IllegalArgumentException ex1){
                errln("HexToUnicodeTransliterator(pattern1, null) construction failed with pattern =" 
                     + Data[i+0] + "Exception:" + ex1.toString());
            }
            expectTranslit(trans1, "", Data[i+1], Integer.parseInt(Data[i+2]), 
                       Integer.parseInt(Data[i+3]), Integer.parseInt(Data[i+4]), Data[i+5] );
            expect(trans1, "", Data[i+1], Data[i+5]);
        }
    }
        
    //======================================================================
    // Support methods
    //======================================================================

    private void expectTranslit(HexToUnicodeTransliterator t,
                                   String message, String source, 
                                   int start, int limit, int cursor,  
                                   String expectedResult){
    

        ReplaceableString rsource = new ReplaceableString(source);
        Transliterator.Position index = new Transliterator.Position(start, limit, cursor, limit);
        t.transliterate(rsource, index);
        t.finishTransliteration(rsource, index);
        String result=rsource.toString();
        expectAux(t.getID() + ":keyboard " + message, source + "->" + result, result.equals(expectedResult), expectedResult);
    }
    private void expectPattern(HexToUnicodeTransliterator t,
                                 String pattern, String source, String expectedResult){

        try{
            t.applyPattern(pattern);
        }catch(IllegalArgumentException iae){
            if(expectedResult.equals("FAIL")){
                logln("OK: calling applyPattern() with illegal pattern failed as expected." + iae.toString());
            } else{
                errln("FAIL: applyPattern() failed with pattern =" + pattern + "--->" + iae.toString());
            }
        }
        if(!expectedResult.equals("FAIL")){
            if(!t.toPattern().equals(pattern)) {
                errln("FAIL: applyPattern or toPatten failed.  Expected: " + pattern + "Got: " + t.toPattern());
            }else{
                logln("OK: applyPattern passed. Testing transliteration");
                expect(t, " with pattern "+pattern, source, expectedResult);
            }
        }
    }
    private void expect(HexToUnicodeTransliterator t,  String message, String source, String expectedResult) {
    
        String result=t.transliterate(source);
        expectAux(t.getID() + ":String " + message, source + "->" + result, result.equals(expectedResult), expectedResult);

        ReplaceableString rsource = new ReplaceableString(source);
        t.transliterate(rsource);
        result = rsource.toString();
        expectAux(t.getID() + ":Replaceable" + message, source + "->" + result, result.equals(expectedResult), expectedResult);

        // Test keyboard (incremental) transliteration -- this result
        // must be the same after we finalize (see below).
        rsource.replace(0, rsource.length(), "");
        Transliterator.Position index = new Transliterator.Position();
        StringBuffer log = new StringBuffer();

        for (int i=0; i<source.length(); ++i) {
            if (i != 0) {
                log.append(" + ");
            }
            log.append(source.charAt(i)).append(" -> ");
            t.transliterate(rsource, index,
                            String.valueOf(source.charAt(i)));
            // Append the string buffer with a vertical bar '|' where
            // the committed index is.
            String s = rsource.toString();
            log.append(s.substring(0, index.start)).
                append('|').
                append(s.substring(index.start));
        }
        
        // As a final step in keyboard transliteration, we must call
        // transliterate to finish off any pending partial matches that
        // were waiting for more input.
        t.finishTransliteration(rsource, index);
        result = rsource.toString();
        log.append(" => ").append(rsource.toString());
        expectAux(t.getID() + ":Keyboard", log.toString() + "\n" + source + " -> " + result, result.equals(expectedResult), expectedResult);

    }
    private void expectAux(String tag, String summary, boolean pass,  String expectedResult) {
        if (pass) {
            logln("(" + tag + ") " + Utility.escape(summary));
        } else {
            errln("FAIL: (" + tag+ ") "
                + Utility.escape(summary)
                + ", expected " + Utility.escape(expectedResult));
        }
    }  
}



