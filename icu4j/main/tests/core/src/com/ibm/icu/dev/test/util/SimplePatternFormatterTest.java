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
                 "evaluate",
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
                 fmt.format(new StringBuilder(), offsets).toString());
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
                 "evaluate",
                 "Some {} messed {12d up stuff.",
                 fmt.format("to"));
     }
     
     public void TestOnePlaceholder() {
        assertEquals("TestOnePlaceholder",
                "1 meter",
                SimplePatternFormatter.compile("{0} meter").format("1"));
     }
     
     public void TestWithPlaceholders() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile(
                 "Templates {2}{1} and {4} are out of order.");
         assertEquals(
                 "getPlaceholderCount",
                 5,
                 fmt.getPlaceholderCount());
         try {
             fmt.format("freddy", "tommy", "frog", "leg");
             fail("Expected UnsupportedOperationException");
         } catch (IllegalArgumentException e) {
             // Expected
         }
         assertEquals(
                 "toString",
                 "Templates {2}{1} and {4} are out of order.",
                 fmt.toString());
        int[] offsets = new int[6]; 
        assertEquals(
                 "evaluate",
                 "123456: Templates frogtommy and {0} are out of order.",
                 fmt.format(
                         new StringBuilder("123456: "),
                         offsets,
                         "freddy", "tommy", "frog", "leg", "{0}").toString());
         
         int[] expectedOffsets = {-1, 22, 18, -1, 32, -1};
         for (int i = 0; i < offsets.length; i++) {
             if (offsets[i] != expectedOffsets[i]) {
                 fail("getOffset() returned wrong value for " + i);
             }
         }
     }
}
