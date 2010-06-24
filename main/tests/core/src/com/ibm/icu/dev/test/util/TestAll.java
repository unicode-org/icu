/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
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
            "ICUResourceBundleTest",
            "CompactArrayTest",
            "StringTokenizerTest",
            "CurrencyTest",
            "UtilityTest",
            "TrieTest",
            "Trie2Test",
            "LocaleDataTest",
            "ULocaleTest",
            "LocaleAliasTest",
            "DebugUtilitiesTest",
            "LocaleBuilderTest",
            "LocaleMatcherTest",
            "LocalePriorityListTest",
        },
              "Test miscellaneous public utilities");
    }

    public static final String CLASS_TARGET_NAME = "Util";
}


