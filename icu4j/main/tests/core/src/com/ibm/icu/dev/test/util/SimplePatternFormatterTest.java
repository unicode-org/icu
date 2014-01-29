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
         SimplePatternFormatter.Formatted formatted = fmt.formatValues(new Object[] {});
         assertEquals(
                 "toString2",
                 "This doesn't have templates {0}",
                 formatted.toString());
         assertEquals(
                 "getOffset(0)",
                 -1,
                 formatted.getOffset(0));
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
                SimplePatternFormatter.compile("{0} meter").format(1));
     }
     
     public void TestWithPlaceholders() {
         SimplePatternFormatter fmt = SimplePatternFormatter.compile(
                 "Templates {2}{1} and {4} are out of order.");
         assertEquals(
                 "getPlaceholderCount",
                 5,
                 fmt.getPlaceholderCount());
         try {
             fmt.format("freddy", "tommy", "frog");
             fail("Expected UnsupportedOperationException");
         } catch (UnsupportedOperationException e) {
             // Expected
         }
         try {
             fmt.formatValues(new String[] {"freddy", "tommy", "frog", "leg"});
             fail("Expected IllegalArgumentException");
         } catch (IllegalArgumentException e) {
             // Expected
         }
         String[] args = new String[] {"freddy", "tommy", "frog", "leg", "{0}"};
        assertEquals(
                 "evaluate",
                 "Templates frogtommy and {0} are out of order.",
                 fmt.formatValues(args).toString());
         assertEquals(
                 "toString",
                 "Templates {2}{1} and {4} are out of order.",
                 fmt.toString());
         SimplePatternFormatter.Formatted formatted =
                 fmt.formatValues(args);
         int[] offsets = {-1, 14, 10, -1, 24, -1};
         for (int i = 0; i < offsets.length; i++) {
             if (offsets[i] != formatted.getOffset(i)) {
                 fail("getOffset() returned wrong value for " + i);
             }
         }
         assertEquals(
                 "toString2",
                 "Templates frogtommy and {0} are out of order.",
                 formatted.toString());
     }
    
}
