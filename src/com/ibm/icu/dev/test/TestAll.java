/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/TestAll.java,v $
 * $Date: 2003/10/02 20:50:57 $
 * $Revision: 1.53 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test;

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
                  "com.ibm.icu.dev.test.format.TestAll",
                  "com.ibm.icu.dev.test.compression.TestAll",
                  "com.ibm.icu.dev.test.rbbi.TestAll",
                  "com.ibm.icu.dev.test.translit.TestAll",
                  "com.ibm.icu.dev.test.search.SearchTest", // not a group
                  "com.ibm.icu.dev.test.collator.TestAll",
                  "com.ibm.icu.dev.test.shaping.ArabicShapingRegTest",
                  "com.ibm.icu.dev.test.calendar.TestAll",
                  "com.ibm.icu.dev.test.timezone.TestAll",
                  "com.ibm.icu.dev.test.lang.TestAll",
                  "com.ibm.icu.dev.test.normalizer.TestAll",
                  "com.ibm.icu.dev.test.util.TestAll",
                  "com.ibm.icu.dev.test.iterator.TestUCharacterIterator", // not a group
                  "com.ibm.icu.dev.test.bigdec.DiagBigDecimal", // not a group
                  "com.ibm.icu.dev.test.impl.TestAll",
                  "com.ibm.icu.dev.test.stringprep.TestAll"
              },
              "All tests in ICU");
    }

    public static final String CLASS_TARGET_NAME  = "ICU";
}
