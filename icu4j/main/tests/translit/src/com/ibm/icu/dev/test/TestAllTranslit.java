/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAllTranslit extends TestGroup {

    public static void main(String[] args) {
        new TestAllTranslit().run(args);
    }

    public TestAllTranslit() {
        super(
              new String[] {
                  "com.ibm.icu.dev.test.translit.TestAll",
                  // funky tests of test code
                  // "com.ibm.icu.dev.test.util.TestBNF",
                  // "com.ibm.icu.dev.test.util.TestBagFormatter",
              },
              "All tests in ICU translit");
    }

    public static final String CLASS_TARGET_NAME  = "Translit";
}
