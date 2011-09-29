/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {

    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(
              new String[] {
                  // Disabled for now; see comment in SimpleBITest for details
                  // "SimpleBITest",
                  "BreakIteratorTest",
                  "RBBITest",
                  "RBBIAPITest",
                  "BreakIteratorRegTest",
                  "RBBITestExtended",
                  "RBBITestMonkey"
              },
              " BreakIterator and RuleBasedBreakIterator Tests");
    }

    public static final String CLASS_TARGET_NAME = "RBBI";
}
