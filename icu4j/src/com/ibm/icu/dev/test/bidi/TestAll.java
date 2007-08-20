//##header J2SE15
/*
*******************************************************************************
*   Copyright (C) 2001-2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.test.bidi;

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
                  "com.ibm.icu.dev.test.bidi.TestCharFromDirProp",
                  "com.ibm.icu.dev.test.bidi.TestBidi",
                  "com.ibm.icu.dev.test.bidi.TestInverse",
                  "com.ibm.icu.dev.test.bidi.TestReorder",
                  "com.ibm.icu.dev.test.bidi.TestFailureRecovery",
                  "com.ibm.icu.dev.test.bidi.TestMultipleParagraphs",
                  "com.ibm.icu.dev.test.bidi.TestReorderingMode",
                  "com.ibm.icu.dev.test.bidi.TestReorderRunsOnly",
                  "com.ibm.icu.dev.test.bidi.TestStreaming",
                  "com.ibm.icu.dev.test.bidi.TestClassOverride",
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
                  "com.ibm.icu.dev.test.bidi.TestCompatibility",
//#endif
              },
              "Bidi tests");
    }

    public static final String CLASS_TARGET_NAME  = "Bidi";
}
