/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/collator/TestAll.java,v $
 * $Date: 2003/01/28 18:55:33 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.collator;

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
    public void TestCollator() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.dev.test.collator.CollationTest(),
            new com.ibm.icu.dev.test.collator.CollationAPITest(),
            new com.ibm.icu.dev.test.collator.CollationCurrencyTest(),
            new com.ibm.icu.dev.test.collator.CollationDanishTest(),
            new com.ibm.icu.dev.test.collator.CollationDummyTest(),
            new com.ibm.icu.dev.test.collator.CollationEnglishTest(),
            new com.ibm.icu.dev.test.collator.CollationFinnishTest(),
            new com.ibm.icu.dev.test.collator.CollationFrenchTest(),
            new com.ibm.icu.dev.test.collator.CollationGermanTest(),
            new com.ibm.icu.dev.test.collator.CollationIteratorTest(),
            new com.ibm.icu.dev.test.collator.CollationKanaTest(),
            new com.ibm.icu.dev.test.collator.CollationMonkeyTest(),
            new com.ibm.icu.dev.test.collator.CollationRegressionTest(),
            new com.ibm.icu.dev.test.collator.CollationSpanishTest(),
            new com.ibm.icu.dev.test.collator.CollationThaiTest(),
            new com.ibm.icu.dev.test.collator.CollationTurkishTest(),
            new com.ibm.icu.dev.test.collator.G7CollationTest(),
            new com.ibm.icu.dev.test.collator.LotusCollationKoreanTest(),
            new com.ibm.icu.dev.test.collator.CollationMiscTest()
                });
    }

    public void TestSearch() throws Exception {
            run(
                new com.ibm.icu.dev.test.search.SearchTest());
    }
}