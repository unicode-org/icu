/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/normalizer/TestAll.java,v $
 * $Date: 2003/01/28 18:55:34 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.normalizer;
import com.ibm.icu.dev.test.TestFmwk;
import java.util.TimeZone;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestFmwk {
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        new TestAll().run(args);
    }
    public void TestNormalizer() throws Exception {
        run( new TestFmwk[] {
            new com.ibm.icu.dev.test.normalizer.BasicTest(),
            new com.ibm.icu.dev.test.normalizer.ConformanceTest(),
            //new com.ibm.icu.dev.test.normalizer.TestDeprecatedNormalizerAPI(),
            new com.ibm.icu.dev.test.normalizer.TestCanonicalIterator(),
            new com.ibm.icu.dev.test.normalizer.NormalizationMonkeyTest(),
        });
    }

}