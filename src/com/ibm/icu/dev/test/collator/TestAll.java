/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/collator/TestAll.java,v $
 * $Date: 2003/06/03 18:49:29 $
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.collator;

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
                  "CollationTest",
                  "CollationAPITest",
                  "CollationCurrencyTest",
                  "CollationDanishTest",
                  "CollationDummyTest",
                  "CollationEnglishTest",
                  "CollationFinnishTest",
                  "CollationFrenchTest",
                  "CollationGermanTest",
                  "CollationIteratorTest",
                  "CollationKanaTest",
                  "CollationMonkeyTest",
                  "CollationRegressionTest",
                  "CollationSpanishTest",
                  "CollationThaiTest",
                  "CollationTurkishTest",
                  "G7CollationTest",
                  "LotusCollationKoreanTest",
                  "CollationMiscTest",
                  "CollationServiceTest",
                  "com.ibm.icu.dev.test.search.SearchTest"
              },
              "All Collation Tests and Search Test"
              );
    }

    public static final String CLASS_TARGET_NAME = "Collator";
}
