/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/rbbi/TestAll.java,v $
 * $Date: 2003/01/28 18:55:34 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;
import com.ibm.icu.dev.test.TestFmwk;
import java.util.TimeZone;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestFmwk {
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        new TestAll().run(args);
    }
    public void TestRuleBasedBreakIterator() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.dev.test.rbbi.SimpleBITest(),
            new com.ibm.icu.dev.test.rbbi.BreakIteratorTest(),
            new com.ibm.icu.dev.test.rbbi.RBBITest(),
            new com.ibm.icu.dev.test.rbbi.RBBIAPITest()
                });
    }

}