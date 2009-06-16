/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.cldr;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all collation and search tests as a batch.
 */
public class TestAll extends TestGroup {
    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(
              new String[] {
                  "TestCLDRVsICU",
              },
              "All Cldr Vs ICU Tests"
              );
    }

    public static final String CLASS_TARGET_NAME = "Cldr";
}
