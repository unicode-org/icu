/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
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
                  //"CollationDanishTest", //Danish is already tested through data driven tests
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
                  "CollationChineseTest",
                  "CollationServiceTest",
                  "CollationThreadTest",
                  //"RandomCollator", //Disabled until the problem in the test case is resolved #5747
                  "UCAConformanceTest",
                  // don't test Search API twice!
                  //"com.ibm.icu.dev.test.search.SearchTest"
              },
              "All Collation Tests"
              );
    }

    public static final String CLASS_TARGET_NAME = "Collator";
}
