/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {
    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public TestAll() {
        super(
              new String[] {
            "VersionInfoTest",
            // "ICUListResourceBundleTest",
            // these tests fail since there is no ICULocaleData.jar to support it
            // ICUResoureBundleTest supercedes the ICUListResourceBundleTest
            "ICUResourceBundleTest",
            "CompactArrayTest",
            "StringTokenizerTest",
            "CurrencyTest",
            "UtilityTest",
            "TrieTest",
            "LocaleDataTest",
            "ULocaleTest",
            "LocaleAliasTest",
            "DebugUtilitiesTest"
        },
              "Test miscellaneous public utilities");
    }

    public static final String CLASS_TARGET_NAME = "Util";
}


