/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.SimplePatternFormatter;

public class SimplePatternFormatterTest extends TestFmwk {

    /**
     * Constructor
     */
     public SimplePatternFormatterTest()
     {
     }
       
     // public methods -----------------------------------------------
     
     public static void main(String arg[]) 
     {
         SimplePatternFormatterTest test = new SimplePatternFormatterTest();
         try {
             test.run(arg);
         } catch (Exception e) {
             test.errln("Error testing SimplePatternFormatterTest");
         }
     }
     
     public void TestWithNoPlaceholders() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile("This doesn''t have templates '{0}");
         assertEquals(
                 "getPlaceholderCount",
                 0,
                 fmt.getPlaceholderCount());
         assertEquals(
                 "format",
                 "This doesn't have templates {0}",
                 fmt.format("unused"));
         assertEquals(
                 "toString",
                 "This doesn't have templates {0}",
                 fmt.toString());
         int[] offsets = new int[1];
         assertEquals(
                 "toString2",
                 "This doesn't have templates {0}",
                 fmt.formatAndAppend(new StringBuilder(), offsets).toString());
         assertEquals(
                 "offsets[0]",
                 -1,
                 offsets[0]);
         fmt = SimplePatternFormatter.compile("Some {} messed {12d up stuff.");
         assertEquals(
                 "getPlaceholderCount",
                 0,
                 fmt.getPlaceholderCount());
         assertEquals(
                 "format",
                 "Some {} messed {12d up stuff.",
                 fmt.format("to"));
     }
     
     public void TestOnePlaceholder() {
        assertEquals("TestOnePlaceholder",
                "1 meter",
                SimplePatternFormatter.compile("{0} meter").format("1"));
     }
     
     public void TestGetPatternWithNoPlaceholders() {
         assertEquals(
                 "",
                 "Templates  and  are here.",
                 SimplePatternFormatter.compile(
                         "Templates {1}{2} and {3} are here.").getPatternWithNoPlaceholders());
     }
     
     public void TestTooFewPlaceholderValues() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile(
                 "Templates {2}{1} and {4} are out of order.");
         try {
             fmt.format("freddy", "tommy", "frog", "leg");
             fail("Expected IllegalArgumentException");
         } catch (IllegalArgumentException e) {
             // Expected
         }
         try {
             fmt.formatAndAppend(
                     new StringBuilder(), null, "freddy", "tommy", "frog", "leg");
             fail("Expected IllegalArgumentException");
         } catch (IllegalArgumentException e) {
             // Expected
         }
         try {
             fmt.formatAndReplace(
                     new StringBuilder(), null, "freddy", "tommy", "frog", "leg");
             fail("Expected IllegalArgumentException");
         } catch (IllegalArgumentException e) {
             // Expected
         }
     }
     
     public void TestWithPlaceholders() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile(
                 "Templates {2}{1} and {4} are out of order.");
         assertEquals(
                 "getPlaceholderCount",
                 5,
                 fmt.getPlaceholderCount()); 
         assertEquals(
                 "toString",
                 "Templates {2}{1} and {4} are out of order.",
                 fmt.toString());
        int[] offsets = new int[6]; 
        assertEquals(
                 "format",
                 "123456: Templates frogtommy and {0} are out of order.",
                 fmt.formatAndAppend(
                         new StringBuilder("123456: "),
                         offsets,
                         "freddy", "tommy", "frog", "leg", "{0}").toString());
         
         int[] expectedOffsets = {-1, 22, 18, -1, 32, -1};
         verifyOffsets(expectedOffsets, offsets);
     }
     
     public void TestFormatUseAppendToAsPlaceholder() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile(
                 "Placeholders {0} and {1}");
         StringBuilder appendTo = new StringBuilder("previous:");
         try {
             fmt.formatAndAppend(appendTo, null, appendTo, "frog");
             fail("IllegalArgumentException expected.");
         } catch (IllegalArgumentException e) {
             // expected.
         }
     }
     
     public void TestFormatReplaceNoOptimization() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile("{2}, {0}, {1} and {3}");
         int[] offsets = new int[4];
         StringBuilder result = new StringBuilder("original");
        assertEquals(
                 "format",
                 "frog, original, freddy and by",
                 fmt.formatAndReplace(
                         result,
                         offsets,
                         result, "freddy", "frog", "by").toString());
         
         int[] expectedOffsets = {6, 16, 0, 27};
         verifyOffsets(expectedOffsets, offsets);
     }
     
     
     public void TestFormatReplaceNoOptimizationLeadingText() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile("boo {2}, {0}, {1} and {3}");
         int[] offsets = new int[4];
         StringBuilder result = new StringBuilder("original");
        assertEquals(
                 "format",
                 "boo original, freddy, frog and by",
                 fmt.formatAndReplace(
                         result,
                         offsets,
                         "freddy", "frog", result, "by").toString());
         
         int[] expectedOffsets = {14, 22, 4, 31};
         verifyOffsets(expectedOffsets, offsets);
     }
     
     public void TestFormatReplaceOptimization() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile("{2}, {0}, {1} and {3}");
         int[] offsets = new int[4];
         StringBuilder result = new StringBuilder("original");
        assertEquals(
                 "format",
                 "original, freddy, frog and by",
                 fmt.formatAndReplace(
                         result,
                         offsets,
                         "freddy", "frog", result, "by").toString());
         
         int[] expectedOffsets = {10, 18, 0, 27};
         verifyOffsets(expectedOffsets, offsets);  
     }
     
     public void TestFormatReplaceOptimizationNoOffsets() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile("{2}, {0}, {1} and {3}");
         StringBuilder result = new StringBuilder("original");
        assertEquals(
                 "format",
                 "original, freddy, frog and by",
                 fmt.formatAndReplace(
                         result,
                         null,
                         "freddy", "frog", result, "by").toString());
         
     }
     
     public void TestFormatReplaceNoOptimizationNoOffsets() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile(
                 "Placeholders {0} and {1}");
         StringBuilder result = new StringBuilder("previous:");
         assertEquals(
                 "",
                 "Placeholders previous: and frog",
                 fmt.formatAndReplace(result, null, result, "frog").toString());
     }
     
     public void TestFormatReplaceNoOptimizationLeadingPlaceholderUsedTwice() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile(
                 "{2}, {0}, {1} and {3} {2}");
         StringBuilder result = new StringBuilder("original");
         int[] offsets = new int[4];
         assertEquals(
                 "",
                 "original, freddy, frog and by original",
                 fmt.formatAndReplace(
                         result,
                         offsets,
                         "freddy", "frog", result, "by").toString());
         int[] expectedOffsets = {10, 18, 30, 27};
         verifyOffsets(expectedOffsets, offsets);
     }
     
     void verifyOffsets(int[] expected, int[] actual) {
         for (int i = 0; i < expected.length; ++i) {
             if (expected[i] != actual[i]) {
                 errln("Expected "+expected[i]+", got " + actual[i]);
             }
         } 
     }
     
}
