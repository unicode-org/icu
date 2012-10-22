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
public class TestAllCollate extends TestGroup {

    public static void main(String[] args) {
        new TestAllCollate().run(args);
    }

    public TestAllCollate() {
        super(
              new String[] {
                  "com.ibm.icu.dev.test.collator.TestAll",
                  "com.ibm.icu.dev.test.format.GlobalizationPreferencesTest",
                  "com.ibm.icu.dev.test.format.RbnfLenientScannerTest",
                  "com.ibm.icu.dev.test.search.SearchTest",
                  "com.ibm.icu.dev.test.util.ICUResourceBundleCollationTest",
                  "com.ibm.icu.dev.test.util.LocaleAliasCollationTest",
                  "com.ibm.icu.dev.test.util.ULocaleCollationTest",
              },
              "All tests in ICU collation");
    }

    public static final String CLASS_TARGET_NAME  = "Collate";
}
