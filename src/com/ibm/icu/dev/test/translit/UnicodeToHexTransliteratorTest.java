/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/Attic/UnicodeToHexTransliteratorTest.java,v $ 
 * $Date: 2002/08/28 16:45:19 $ 
 * $Revision: 1.7 $
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
 * @summary General test of UnicodeToHexTransliterator
 */
public class UnicodeToHexTransliteratorTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new UnicodeToHexTransliteratorTest().run(args);
    }

    /**
    * Used by TestConstruction() and TestTransliterate.
    */
    UnicodeFilter UniFilter=new UnicodeFilter() {
        public boolean contains(int c) {
            if(c==0x0063 || c==0x0061 || c==0x0043 || c==0x0041)
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
        logln("Testing the construction UnicodeToHexTransliterator()");
        UnicodeToHexTransliterator trans1=new UnicodeToHexTransliterator();
        

        logln("Testing the cosntruction UnicodeToHexTransliterator(pattern)");
        String pattern="\\\\U+0000abc";
        try{
            trans1=new UnicodeToHexTransliterator(pattern);
        }catch(IllegalArgumentException ex2) {
            errln("FAIL: UnicodeToHexTransliterator construction failed with pattern=" + pattern + " Exception= " + ex2.toString());
        }

        logln("Testing the cosntruction UnicodeToHexTransliterator(pattern) with illegal pattern");
        String pattern2="\\X+";
        try{
            trans1=new UnicodeToHexTransliterator(pattern2);
        }catch(IllegalArgumentException ex3) {
            logln("OK: construction with Illegal pattern handled correctly");
        }  
        
        logln("Testing the construction UnicodeToHexTransliterator(pattern, isUppercase=false, adoptedFilter=null)");
        try{
            trans1=new UnicodeToHexTransliterator(pattern, false, null);
        }catch(IllegalArgumentException ex4) {
            errln("FAIL: UnicodeToHexTransliterator(pattern, isUppercase=false, adoptedFilter=null) construction failed. Exception= " + ex4.toString());
        }

        logln("Testing the construction UnicodeToHexTransliterator(pattern, isUppercase=true, adoptedFilter)");
        try{
            trans1=new UnicodeToHexTransliterator(pattern, true, UniFilter);
        }catch(IllegalArgumentException ex4) {
            errln("FAIL: UnicodeToHexTransliterator(pattern, isUppercase=true, adoptedFilter) construction failed. Exception= " + ex4.toString());
        }

    }

    public void TestUpperCase(){
        logln("Testing the isUppercase() and setUppercase() API of UnicodeToHexTransliterator");
        String str="abk";
        /*default transliterator has upper case TRUE*/
        UnicodeToHexTransliterator transdefault=new UnicodeToHexTransliterator();
        expect(transdefault, "where uppercase=default", str, "\\u0061\\u0062\\u006B");

        String pattern="\\\\u0000";
        /*transliterator with Uppercase FALSE*/
        UnicodeToHexTransliterator trans1=null;
        try{
            trans1=new UnicodeToHexTransliterator(pattern, false, null);
        }catch(IllegalArgumentException ex1) {
            errln("FAIL: UnicodeToHexTransliterator(pattern, false,null) construction failed with patter" 
                  + pattern+ " Exception: " + ex1.toString());
        }
        expect(trans1, "where uppercase=FALSE", str, "\\u0061\\u0062\\u006b");  /*doesn't display uppercase*/

        if(transdefault.isUppercase() != true  || 
           trans1.isUppercase() != false ){
            errln("isUpperCase() failed");
        }
        
        /*changing the outputhexdigits to lower case for the default transliterator*/
        transdefault.setUppercase(trans1.isUppercase());
        if(transdefault.isUppercase() != trans1.isUppercase() || 
           transdefault.isUppercase() != false){
            errln("setUppercase() failed");
        }
        /*doesn't ouput uppercase hex, since transdefault's uppercase is set to FALSE using setUppercase*/
        expect(transdefault, "where uppercase=FALSE", str, "\\u0061\\u0062\\u006b");     

        /*trying round trip*/
        transdefault.setUppercase(true);
        if(transdefault.isUppercase() != true || 
           transdefault.isUppercase() == trans1.isUppercase() ){
            errln("setUppercase() failed");
        }
        /*displays upper case since it is set to TRUE*/
        expect(transdefault, "where uppercase=TRUE", str, "\\u0061\\u0062\\u006B");

    } 
    public void TestPattern(){
        logln("Testing the applyPattern() and toPattern() API of UnicodeToHexTransliterator");
        /*default transliterator has pattern \\u0000*/
        UnicodeToHexTransliterator transdefault=new UnicodeToHexTransliterator();
                
        String defaultpattern=transdefault.toPattern();
        String pattern1="\\\\U+0000";
        
        UnicodeToHexTransliterator trans1=null;
        try{
            trans1=new UnicodeToHexTransliterator(pattern1, true, null);
        }catch(IllegalArgumentException ex1) {
            errln("FAIL: UnicodeToHexTransliterator construction failed with pattern =" + pattern1 +
                   "Exception:" + ex1.toString());
        }

        /*test toPattern() */
        if(transdefault.toPattern().equals(trans1.toPattern()) == true ||
            transdefault.toPattern().equals("\\\\u0000") != true ||
            trans1.toPattern().equals(pattern1) != true ){
            errln("Error: toPattern() failed");
        }

        /*apply patterns for transdefault*/
        String str="abKf";
        expectPattern(transdefault,  pattern1, str, "\\U+0061\\U+0062\\U+004B\\U+0066");
        expectPattern(transdefault,  "\\U##00,", str, "U61,U62,U4B,U66,");
        expectPattern(transdefault, defaultpattern, str, "\\u0061\\u0062\\u004B\\u0066");
        expectPattern(trans1, "\\uni0000", str, "uni0061uni0062uni004Buni0066");
        expectPattern(trans1, "\\\\S-0000-E", str, "\\S-0061-E\\S-0062-E\\S-004B-E\\S-0066-E");
        expectPattern(trans1, "\\u##0000", str, "FAIL");
        expectPattern(trans1, "\\*0000", str, "*0061*0062*004B*0066"); 
        expectPattern(trans1, "\\u####", str, "FAIL");
   
    }

    public void TestSimpleTransliterate(){ 
        String pattern1="\\\\U+0000";
        UnicodeToHexTransliterator trans1=null;
        try{
            trans1=new UnicodeToHexTransliterator(pattern1, true, null);
        }catch(IllegalArgumentException iae){
            errln("UnicodeToHexTransliterator(pattern1, true, null) construction failed with pattern =" 
                  + pattern1 + "Exception:" + iae.toString());
        }
        String source="Hello";
        String expected="He\\U+006C\\U+006C\\U+006F";

        expectTranslit(trans1, ":Replaceable ", source,  1, 5, 2, expected);   
    }
   
    public void TestTransliterate(){
        String Data[]={
        //pattern, source, index.contextStart, index.contextLimit, index.start, expectedResult, expectedResult using filter(a, b)
        "U+##00",    "abc", "1", "3", "2", "abU+63", "abc",
        "\\\\u0000", "abc", "1", "2", "1", "a\\u0062c", "a\\u0062c",
        "Uni0000",   "abc", "1", "3", "2", "abUni0063", "abc",
        "U[0000]",   "hello", "0", "4", "2", "heU[006C]U[006C]o", "heU[006C]U[006C]o",
        "prefix-0000-suffix", "abc", "1", "3", "1", "aprefix-0062-suffixprefix-0063-suffix", "aprefix-0062-suffixc",
        "*##00*",     "hellothere", "1", "8", "4", "hell*6F**74**68**65*re", "hell*6F**74**68**65*re",

        };
        UnicodeToHexTransliterator trans1=null;
        UnicodeToHexTransliterator trans2=null;
        for(int i=0;i<Data.length;i+=7){
            try{
                trans1=new UnicodeToHexTransliterator(Data[i+0],true, null);
            }catch(IllegalArgumentException iae){
                errln("UnicodeToHexTransliterator(pattern1, true, null) construction failed with pattern =" 
                  + Data[i+0] + "Exception:" + iae.toString());
            }
            expectTranslit(trans1, "", Data[i+1], Integer.parseInt(Data[i+2]), 
                       Integer.parseInt(Data[i+3]), Integer.parseInt(Data[i+4]), 
                       Data[i+5] );
        
            try{
                trans2=new UnicodeToHexTransliterator(Data[i+0], true, UniFilter);
            }catch(IllegalArgumentException iae){
                errln("UnicodeToHexTransliterator(pattern1, true, UnicodeFilter) construction failed with pattern =" 
                  +  Data[i+0] + " with filter(a,c).  Exception:" + iae.toString());
            }
            expectTranslit(trans2, " with filter(a,A,c,C)", Data[i+1], 
                           Integer.parseInt(Data[i+2]), Integer.parseInt(Data[i+3]), 
                           Integer.parseInt(Data[i+4]), Data[i+6] );
        }

    } 
        
    //======================================================================
    // Support methods
    //======================================================================

    private void expectTranslit(UnicodeToHexTransliterator t,
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
    private void expectPattern(UnicodeToHexTransliterator t,
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
    private void expect(UnicodeToHexTransliterator t,  String message, String source, String expectedResult) {
    
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



