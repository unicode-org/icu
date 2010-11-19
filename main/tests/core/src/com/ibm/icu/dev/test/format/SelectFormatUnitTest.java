/*
 *******************************************************************************
 * Copyright (c) 2004-2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 * Copyright (C) 2010 , Yahoo! Inc.                                            
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.SelectFormat;

/**
 * @author kirtig 
 * This class does the unit testing for the SelectFormat
 */
public class SelectFormatUnitTest extends TestFmwk {
  
    static final String SIMPLE_PATTERN = "feminine {feminineVerbValue} other{otherVerbValue}";

    public static void main(String[] args) throws Exception {
        new SelectFormatUnitTest().run(args);
    }
  
    /**
     * Unit tests for pattern syntax
     */
    public void TestPatternSyntax() {
        String checkSyntaxData[] = {
            "odd{foo} odd{bar} other{foobar}",
            "odd{foo} other{bar} other{foobar}",
            "odd{foo}",
            "1odd{foo} other{bar}",
            "odd{foo},other{bar}",
            "od d{foo} other{bar}",
            "odd{foo}{foobar}other{foo}",
            "odd{foo1}other{foo2}}",  
            "odd{foo1}other{{foo2}",  
            "odd{fo{o1}other{foo2}}"
        };

        String expectedErrorMsgs[] = {
            "Duplicate keyword error.",
            "Duplicate keyword error.",
            "Pattern syntax error. Value for case \"other\" was not defined. ",
            "Pattern syntax error.",
            "Pattern syntax error.",
            "Pattern syntax error.",
            "Pattern syntax error.",
            "Pattern syntax error.",
            "Pattern syntax error.",
            "Pattern syntax error. Value for case \"other\" was not defined. ",
        };

        //Test SelectFormat pattern syntax
        SelectFormat selFmt = new SelectFormat(SIMPLE_PATTERN);
        for (int i=0; i<checkSyntaxData.length; ++i) {
            try {
                selFmt.applyPattern(checkSyntaxData[i]);
                errln("\nERROR: Unexpected result - SelectFormat Unit Test failed "
                      + "to detect syntax error with pattern: "+checkSyntaxData[i]);
            } catch (IllegalArgumentException e){
                assertEquals("Error:TestPatternSyntax failed with unexpected"
                             + " error message for pattern: " + checkSyntaxData[i] ,
                             expectedErrorMsgs[i], e.getMessage() );
                continue;
            }
        }
    }

    /**
     * Unit tests for invalid keywords 
     */
    public void TestInvalidKeyword() {
        //Test formatting with invalid keyword
        String keywords[] = {
            "9Keyword-_",       //Starts with a digit
            "-Keyword-_",       //Starts with a hyphen
            "_Keyword-_",       //Starts with an underscore
            "\\u00E9Keyword-_", //Starts with non-ASCII character
            "Key*word-_",        //Contains a sepial character not allowed
            "*Keyword-_"       //Starts with a sepial character not allowed
        };

        String expected = "Invalid formatting argument.";
        SelectFormat selFmt = new SelectFormat(SIMPLE_PATTERN);
        for (int i = 0; i< 6; i++ ){
            try {
                selFmt.format( keywords[i]);
                fail("Error:TestInvalidKeyword failed to detect invalid keyword "
                     + "for keyword: " + keywords[i]  );
            } catch (IllegalArgumentException e){
                assertEquals("Error:TestInvalidKeyword failed with unexpected "
                            +"error message for keyword: " + keywords[i] 
                            , expected , e.getMessage() );
                continue;
            }
        }

    }

    /**
     * API tests for  applyPattern and format
     */
    public void TestApplyFormat() {
        //Test applying and formatting with various pattern
        String patternTestData[] = {
            "fem {femValue} other{even}",
            "other{odd or even}",
            "odd{The number {0, number, integer} is odd.}other{The number {0, number, integer} is even.}",
            "odd{The number {1} is odd}other{The number {1} is even}"
        };

        String formatArgs[] = {
            "fem",
            "other",
            "odd"
        };

        String expFormatResult[][] = {
            {
                "femValue",
                "even",
                "even",
            },
            {
                "odd or even",
            "odd or even",
            "odd or even",
            },
            {
                "The number {0, number, integer} is even.",
                "The number {0, number, integer} is even.",
                "The number {0, number, integer} is odd.",
            },
            {
                "The number {1} is even",
                "The number {1} is even",
                "The number {1} is odd",
            }
        };

        log("SelectFormat Unit test: Testing  applyPattern() and format() ...");
        SelectFormat selFmt = new SelectFormat(SIMPLE_PATTERN); 

        for (int i=0; i<patternTestData.length; ++i) {
            try {
                selFmt.applyPattern(patternTestData[i]);
            } catch (IllegalArgumentException e){
                errln("ERROR: SelectFormat Unit Test failed to apply pattern- "
                     + patternTestData[i] );
                continue;
            }

            //Format with the keyword array
            for (int j=0; j<3; j++) {
                assertEquals("ERROR: SelectFormat Unit test failed in format() with unexpected result", selFmt.format(formatArgs[j]) ,expFormatResult[i][j] );
            }
        }
    }

}

