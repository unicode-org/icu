/*
 *******************************************************************************
 * Copyright (c) 2004-2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 * Copyright (C) 2010 , Yahoo! Inc.                                            
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.text.FieldPosition;
import java.text.ParsePosition;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.SelectFormat;

/**
 * @author kirtig 
 * This class tests the API functionality of the SelectFormat
 */
public class SelectFormatAPITest extends TestFmwk {
  
    static final String SIMPLE_PATTERN1 = "feminine {feminineVerbValue1} other{otherVerbValue1}";
    static final String SIMPLE_PATTERN2 = "feminine {feminineVerbValue2} other{otherVerbValue2}";

    public static void main(String[] args) throws Exception {
        new SelectFormatAPITest().run(args);
    }
  
    /**
     * API tests for constructors
     */
    public void TestConstructors() {
        log("Inside TestConstructors");
        try {
            SelectFormat selFmt = new SelectFormat(SIMPLE_PATTERN1);
            assertNotNull("Error: TestConstructors - SelectFormat object constructed "
                          + "with argument constructor is null" , selFmt );  
        } catch (Exception e){
            errln("Exception encountered in TestConstructors while creating a constructor with argument");
        }
    }

    /**
     * API tests for equals() method
     */
    public void TestEquals() {
        log("Inside TestEquals");
        SelectFormat selFmt1 = null;

        //Check equality for pattern constructed SelectFormats
        try {
            selFmt1 = new SelectFormat(SIMPLE_PATTERN1);
            SelectFormat selFmt2 = new SelectFormat(SIMPLE_PATTERN1);
            assertTrue("Equals test failed while checking equality for " 
                       + "pattern constructed SelectFormats ." 
                       , selFmt1.equals(selFmt2) ); 
        } catch (Exception e){
            errln("Exception encountered in TestEquals 1 " + e.getMessage());
        }

        //Check equality for 2 objects  
        try {
            Object selFmt3 = new SelectFormat(SIMPLE_PATTERN1);
            Object selFmt4 = new SelectFormat(SIMPLE_PATTERN1);
            Object selFmt5 = new SelectFormat(SIMPLE_PATTERN2);
            assertTrue("Equals test failed while checking equality for object 1." 
                    , selFmt3.equals(selFmt4) );
            assertTrue("Equals test failed while checking equality for object 2." 
                        , selFmt1.equals(selFmt3) );
            assertFalse("Equals test failed while checking equality for object 3." 
                    , selFmt3.equals(selFmt5) );
        } catch (Exception e){
            errln("Exception encountered in TestEquals 2" + e.getMessage());
        }
    }

    /**
     * API tests for applyPattern() method
     */
    public void TestApplyPatternToPattern() {
        log("Inside TestApplyPatternToPattern");
        SelectFormat selFmt = null;
        String pattern = "masculine{masculineVerbValue} other{otherVerbValue}";

        //Check for applyPattern/toPattern
        try {
            selFmt = new SelectFormat(SIMPLE_PATTERN1);
            selFmt.applyPattern(pattern);
            assertEquals("Failed in applyPattern,toPattern with unexpected output"
                         , pattern,  selFmt.toPattern() );
        } catch (Exception e){
            errln("Exception encountered in TestApplyPatternToPattern");
        }
        //Check for invalid pattern
        try {
            String brokenPattern = "broken }{ pattern";
            selFmt.applyPattern(brokenPattern);
            errln("Failed in applyPattern.  applyPattern should throw IllegalArgumentException for " + brokenPattern);
        } catch (IllegalArgumentException e) {
            // This is OK
        }
    }

    /**
     * API tests for toString() method
     */
    public void TestToString(){
        log("Inside TestToString");
        SelectFormat selFmt = null;

        //Check toString for pattern constructed SelectFormat
        try {
            selFmt = new SelectFormat(SIMPLE_PATTERN1);
            String expected = "pattern='feminine {feminineVerbValue1} other{otherVerbValue1}'";
            assertEquals("Failed in TestToString with unexpected output 2"
                         , expected, selFmt.toString() );
        } catch (Exception e){
            errln("Exception encountered in TestToString 2");
        }
    }

    /**
     * API tests for hashCode() method
     */
    public void TestHashCode(){
        log("Inside TestHashCode");

        //Check hashCode for pattern constructed SelectFormat
        try {
            SelectFormat selFmt = new SelectFormat(SIMPLE_PATTERN1);
            SelectFormat selFmt1 = new SelectFormat(SIMPLE_PATTERN1);
            SelectFormat selFmt2 = new SelectFormat(SIMPLE_PATTERN2);
            assertEquals("Failed in TestHashCode 1 with unexpected output"
                         , selFmt.hashCode(), selFmt1.hashCode() );
            assertNotEquals("Failed in TestHashCode 2 with unexpected output"
                         , selFmt.hashCode(), selFmt2.hashCode() );
        } catch (Exception e){
            errln("Exception encountered in TestHashCode 3 with message : " 
                  + e.getMessage());
        }

    }

    /**
     * API tests for toPattern() method
     */
    public void TestToPattern(){
        log("Inside TestToPattern");
        SelectFormat selFmt = null;

        //Check toPattern for pattern constructed SelectFormat
        try {
            selFmt = new SelectFormat(SIMPLE_PATTERN1);
            assertEquals("Failed in TestToPattern 2 with unexpected output"
                         , SIMPLE_PATTERN1, selFmt.toPattern() );
        } catch (Exception e){
            errln("Exception encountered in TestToPattern 2 with message : " 
                  + e.getMessage());
        }
    }

    /**
     * API tests for format() method
     */
    public void TestFormat(){
        log("Inside TestFormat");

        //Check format for pattern constructed object
        try {
            SelectFormat selFmt1 = new SelectFormat(SIMPLE_PATTERN1);
            String expected = "feminineVerbValue1";
            assertEquals("Failed in TestFormat with unexpected output 1"
                         , expected 
                         , selFmt1.format("feminine") );
        } catch (Exception e){
            errln("Exception encountered in TestFormat 3: " + e.getMessage() );
        }

        //Check format with appendTo for pattern constructed object
        try {
            SelectFormat selFmt1 = new SelectFormat(SIMPLE_PATTERN1);
            StringBuffer expected = new StringBuffer("AppendHere-otherVerbValue1");
            StringBuffer strBuf = new StringBuffer("AppendHere-");
            assertEquals("Failed in TestFormat with unexpected output 2"
                         , expected.toString()
                         , (selFmt1.format("other",strBuf, new FieldPosition(0))).toString()  );
        } catch (Exception e){
            errln("Exception encountered in TestFormat 2: " + e.getMessage() );
        }
    }

    /**
     * API tests for parseObject() method
     */
    public void TestParseObject(){
        log("Inside TestToPattern");
        System.out.println("Inside TestToPattern");

        //Check parseObject
        try {
            SelectFormat selFmt = new SelectFormat(SIMPLE_PATTERN1);
            selFmt.parseObject("feminine", new ParsePosition(0) );
            fail("Failed in TestParseObject - UnsupportedOperationException not received");
        } catch (UnsupportedOperationException e){
            //Expect this Exception
        } catch (Exception e){
            errln("Exception encountered in TestParseObject with message : "
                   + e.getMessage());
      }
    }

}

