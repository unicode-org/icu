//##header
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
                "UnicodeSetTest",
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
                "RegexUtilitiesTest",
                "UnicodeMapTest",
//#endif
        });
    }

    public static final String CLASS_TARGET_NAME = "Translit";
}
