/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.normalizer;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run normalizer tests as a batch.
 */
public class TestAll extends TestGroup {

    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
            "BasicTest",
            "ConformanceTest",
            "TestDeprecatedNormalizerAPI",
            "TestCanonicalIterator",
            "NormalizationMonkeyTest",
            "NormalizerRegressionTests",
            "UTS46Test"
        });
    }

    public static final String CLASS_TARGET_NAME = "Normalizer";
}
