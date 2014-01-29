/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Template;

/**
 * @author rocketman
 *
 */
public class TemplateTest extends TestFmwk {

    /**
     * Constructor
     */
     public TemplateTest()
     {
     }
       
     // public methods -----------------------------------------------
     
     public static void main(String arg[]) 
     {
         TemplateTest test = new TemplateTest();
         try {
             test.run(arg);
         } catch (Exception e) {
             test.errln("Error testing templatetest");
         }
     }
     
     public void TestWithNoPlaceholders() {
         Template t = Template.compile("This doesn''t have templates '{0}");
         assertEquals(
                 "getPlaceholderCount",
                 0,
                 t.getPlaceholderCount());
         assertEquals(
                 "evaluate",
                 "This doesn't have templates {0}",
                 t.evaluate());
         assertEquals(
                 "toString",
                 "This doesn't have templates {0}",
                 t.toString());
         Template.Evaluation eval = t.evaluateFull();
         assertEquals(
                 "toString2",
                 "This doesn't have templates {0}",
                 eval.toString());
         assertEquals(
                 "getOffset(0)",
                 -1,
                 eval.getOffset(0));
         t = Template.compile("Some {} messed {12d up stuff.");
         assertEquals(
                 "getPlaceholderCount",
                 0,
                 t.getPlaceholderCount());
         assertEquals(
                 "evaluate",
                 "Some {} messed {12d up stuff.",
                 t.evaluate("to"));
     }
     
     public void TestOnePlaceholder() {
        assertEquals("TestOnePlaceholder",
                "1 meter",
                Template.compile("{0} meter").evaluate(1));
     }
     
     public void TestWithPlaceholders() {
         Template t = Template.compile(
                 "Templates {2}{1} and {4} are out of order.");
         assertEquals(
                 "getPlaceholderCount",
                 5,
                 t.getPlaceholderCount());
         try {
             t.evaluate("freddy", "tommy", "frog", "leg");
             fail("Expected IllegalArgumentException");
         } catch (IllegalArgumentException e) {
             // Expected
         }
         assertEquals(
                 "evaluate",
                 "Templates frogtommy and {0} are out of order.",
                 t.evaluate("freddy", "tommy", "frog", "leg", "{0}"));
         assertEquals(
                 "toString",
                 "Templates {2}{1} and {4} are out of order.",
                 t.toString());
         Template.Evaluation eval =
                 t.evaluateFull("freddy", "tommy", "frog", "leg", "{0}");
         int[] offsets = {-1, 14, 10, -1, 24, -1};
         for (int i = 0; i < offsets.length; i++) {
             if (offsets[i] != eval.getOffset(i)) {
                 fail("getOffset() returned wrong value for " + i);
             }
         }
         assertEquals(
                 "toString2",
                 "Templates frogtommy and {0} are out of order.",
                 eval.toString());
     }
    
}
