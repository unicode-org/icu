/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/TestAll.java,v $
 * $Date: 2004/02/25 01:24:58 $
 * $Revision: 1.4 $
 *
 *****************************************************************************************
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
            "TransliteratorTest",
            "UnicodeSetTest",
            "CompoundTransliteratorTest",
            "JamoTest",
            "ErrorTest",
            "RoundTripTest",
            "ReplaceableTest",
        });
    }

    public static final String CLASS_TARGET_NAME = "Translit";
}
