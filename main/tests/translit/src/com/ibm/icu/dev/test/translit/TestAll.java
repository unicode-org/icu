/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestGroup {
    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
                "AnyScriptTest",
                "CompoundTransliteratorTest",
                "ErrorTest",
                "JamoTest",
                "ReplaceableTest",
                "RoundTripTest",
                "TransliteratorTest",
                "RegexUtilitiesTest",
                "UnicodeMapTest",
        });
    }

    public static final String CLASS_TARGET_NAME = "Translit";
}
