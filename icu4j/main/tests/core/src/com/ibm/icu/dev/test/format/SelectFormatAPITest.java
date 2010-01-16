/*
 *******************************************************************************
 * Copyright (c) 2004-2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 * Copyright (C) 2010 , Yahoo! Inc.                                            
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.SelectFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kirtig 
 * This class tests the API functionality of the SelectFormat
 */
public class SelectFormatAPITest extends TestFmwk {
  
    static final String SIMPLE_PATTERN = "feminine {feminineVerbValue} other{otherVerbValue}";

    public static void main(String[] args) throws Exception {
        new SelectFormatAPITest().run(args);
    }
  
    /**
     * API tests for constructors
     */
    public void TestConstructors() {
        log("Inside TestConstructors");
        //Default constructor
        try {
            SelectFormat selFmt = new SelectFormat();
            assertNotNull("Error: TestConstructors - SelectFormat object constructed "
                          + "with default constructor is null" , selFmt );  
        } catch (Exception e){
            errln("Exception encountered in TestConstructors while creating a default constructor");
        }

        //Constructor with argument - a pattern
        try {
            SelectFormat selFmt = new SelectFormat(SIMPLE_PATTERN);
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
        SelectFormat selFmt3 = null;

        //Check equality for Default constructed SelectFormats
        try {
            selFmt1 = new SelectFormat();
            SelectFormat selFmt2 = new SelectFormat();
            assertTrue("Equals test failed while checking equality for " 
                       + "default constructed SelectFormats ." 
                       , selFmt1.equals(selFmt2) ); 
        } catch (Exception e){
            errln("Exception encountered in TestEquals 1 " + e.getMessage());
        }

        //Check equality for pattern constructed SelectFormats
        try {
            selFmt3 = new SelectFormat(SIMPLE_PATTERN);
            SelectFormat selFmt4 = new SelectFormat(SIMPLE_PATTERN);
            assertTrue("Equals test failed while checking equality for " 
                       + "pattern constructed SelectFormats ." 
                       , selFmt3.equals(selFmt4) ); 
        } catch (Exception e){
            errln("Exception encountered in TestEquals 2 " + e.getMessage());
        }

/* To Do
        //Check equality for 2 objects  
        try {
            Object selFmt5 = new SelectFormat();
            Object selFmt6 = new SelectFormat(SIMPLE_PATTERN);
            assertTrue("Equals test failed while checking equality for object 1." 
                    , selFmt1.equals(selFmt5) );
            assertTrue("Equals test failed while checking equality for object 2." 
                        , selFmt3.equals(selFmt6) );
        } catch (Exception e){
            errln("Exception encountered in TestEquals 3" + e.getMessage());
        }
*/
    }

    /**
     * API tests for applyPattern() method
     */
    public void TestApplyPatternToPattern() {
        log("Inside TestApplyPatternToPattern");
        SelectFormat selFmt = null;
        String pattern = "masculine{masculineVerbValue} other{otherVerbValue}";

        //Check toPattern for Default constructed SelectFormat
        try {
            selFmt = new SelectFormat();
            selFmt.applyPattern(pattern);
            assertEquals("Failed in applyPattern,toPattern 1 with unexpected output"
                         , pattern,  selFmt.toPattern() );
        } catch (Exception e){
            errln("Exception encountered in TestApplyPatternToPattern 1");
        }

        //Check toPattern for pattern constructed SelectFormat
        try {
            selFmt = new SelectFormat(SIMPLE_PATTERN);
            selFmt.applyPattern(pattern);
            assertEquals("Failed in applyPattern,toPattern 2 with unexpected output"
                         , pattern,  selFmt.toPattern() );
        } catch (Exception e){
            errln("Exception encountered in TestApplyPatternToPattern 2");
        }
    }

    /**
     * API tests for toString() method
     */
    public void TestToString(){
        log("Inside TestToString");
        SelectFormat selFmt = null;

        //Check toString for Default constructed SelectFormat
        try {
            selFmt = new SelectFormat();
            String expected = "pattern='null'";
            assertEquals("Failed in TestToString with unexpected output 1"
                         , expected, selFmt.toString() );
        } catch (Exception e){
            errln("Exception encountered in TestToString 1");
        }

        //Check toString for pattern constructed SelectFormat
        try {
            selFmt = new SelectFormat(SIMPLE_PATTERN);
            String expected = "pattern='feminine {feminineVerbValue} other{otherVerbValue}'";
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
        SelectFormat selFmt = null;
        SelectFormat selFmt1 = null;
        SelectFormat selFmt2 = null;

        //Check hashCode for Default constructed SelectFormat
        try {
            selFmt = new SelectFormat();
            selFmt1 = new SelectFormat();
            selFmt2 = new SelectFormat(SIMPLE_PATTERN);
            assertEquals("Failed in TestHashCode 1 with unexpected output"
                         , selFmt.hashCode(), selFmt1.hashCode() );
            assertNotEquals("Failed in TestHashCode 2 with unexpected output"
                         , selFmt.hashCode(), selFmt2.hashCode() );
        } catch (Exception e){
            errln("Exception encountered in TestHashCode 3 with message : " 
                  + e.getMessage());
        }

        //Check hashCode for pattern constructed SelectFormat
        try {
            selFmt = new SelectFormat(SIMPLE_PATTERN);
            selFmt1 = new SelectFormat(SIMPLE_PATTERN);
            selFmt2 = new SelectFormat();
            assertEquals("Failed in TestHashCode 4 with unexpected output"
                         , selFmt.hashCode(), selFmt1.hashCode() );
            assertNotEquals("Failed in TestHashCode 5 with unexpected output"
                         , selFmt.hashCode(), selFmt2.hashCode() );
        } catch (Exception e){
            errln("Exception encountered in TestHashCode 6 with message : " 
                  + e.getMessage());
        }

    }

    /**
     * API tests for toPattern() method
     */
    public void TestToPattern(){
        log("Inside TestToPattern");
        SelectFormat selFmt = null;

        //Check toPattern for Default constructed SelectFormat
        try {
            selFmt = new SelectFormat();
            assertEquals("Failed in TestToPattern 1 with unexpected output"
                         , null, selFmt.toPattern() );
        } catch (Exception e){
            errln("Exception encountered in TestToPattern 1 with message : " 
                  + e.getMessage());
        }

        //Check toPattern for pattern constructed SelectFormat
        try {
            selFmt = new SelectFormat(SIMPLE_PATTERN);
            assertEquals("Failed in TestToPattern 2 with unexpected output"
                         , SIMPLE_PATTERN, selFmt.toPattern() );
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
            SelectFormat selFmt1 = new SelectFormat(SIMPLE_PATTERN);
            String expected = "feminineVerbValue";
            assertEquals("Failed in TestFormat with unexpected output 1"
                         , expected 
                         , selFmt1.format("feminine") );
        } catch (Exception e){
            errln("Exception encountered in TestFormat 3: " + e.getMessage() );
        }

        //Check format with appendTo for pattern constructed object
        try {
            SelectFormat selFmt1 = new SelectFormat(SIMPLE_PATTERN);
            StringBuffer expected = new StringBuffer("AppendHere-otherVerbValue");
            StringBuffer strBuf = new StringBuffer("AppendHere-");
            assertEquals("Failed in TestFormat with unexpected output 2"
                         , expected.toString()
                         , (selFmt1.format("other",strBuf, new FieldPosition(0))).toString()  );
        } catch (Exception e){
            errln("Exception encountered in TestFormat 2: " + e.getMessage() );
        }

        //Check format for Default constructed object
        SelectFormat selFmt1 = new SelectFormat();
        try {
            selFmt1.format("feminine");
            fail("Failed in TestFormat 3 as did not receive the expected IllegalStateException.");
        } catch (IllegalStateException e){
            String expected = "Invalid format error.";
            assertEquals("Failed in TestFormat 3 with unexpected excpetion message"
                        , expected , e.getMessage() );
        } catch (Exception e){
            fail("Failed in TestFormat 3 with exception as " + e.getMessage());
        }

        //Check format with appendTo for Default constructed object
        try {
            StringBuffer strBuf = new StringBuffer("AppendHere-");
            selFmt1.format("other",strBuf, new FieldPosition(0));
            fail("Failed in TestFormat 4 as did not receive the expected IllegalStateException.");
        } catch (IllegalStateException e){
            String expected = "Invalid format error.";
            assertEquals("Failed in TestFormat 4 with unexpected excpetion message"
                        , expected , e.getMessage() );
        } catch (Exception e){
            fail("Failed in TestFormat 4 with exception as " + e.getMessage());
        }
    }

    /**
     * API tests for parseObject() method
     */
    public void TestParseObject(){
        log("Inside TestToPattern");
        System.out.println("Inside TestToPattern");

        //Check parseObject for Default constructed SelectFormat
        try {
          SelectFormat selFmt = new SelectFormat();
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

