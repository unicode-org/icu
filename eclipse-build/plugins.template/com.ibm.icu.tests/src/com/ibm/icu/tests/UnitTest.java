/*
 ******************************************************************************
 * Copyright (C) 2005-2012, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package com.ibm.icu.tests;

import junit.framework.TestCase;

import com.ibm.icu.dev.test.TestAll;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestFmwk.TestParams;


public class UnitTest extends TestCase {

	public void runUtility(String testname) throws Exception {
        TestParams params = TestParams.create("-n", null);
        TestFmwk test = new TestAll();
        test.resolveTarget(params, testname).run();
        if (params.errorCount > 0) {
            fail(params.errorSummary.toString());
        }
    }


    // Collate

    public void testCollator() throws Exception {
        runUtility("Collate/Collator");
    }

    public void testGlobalizationPreferencesTest() throws Exception {
        runUtility("Collate/GlobalizationPreferencesTest");
    }

    public void testICUResourceBundleCollationTest() throws Exception {
        runUtility("Collate/ICUResourceBundleCollationTest");
    }

    public void testLocaleAliasCollationTest() throws Exception {
        runUtility("Collate/LocaleAliasCollationTest");
    }

    public void testRbnfLenientScannerTest() throws Exception {
        runUtility("Collate/RbnfLenientScannerTest");
    }

    public void testSearchTest() throws Exception {
        runUtility("Collate/SearchTest");
    }

    public void testULocaleCollationTest() throws Exception {
        runUtility("Collate/ULocaleCollationTest");
    }


    // Core

    public void testArabicShapingRegTest() throws Exception {
        runUtility("Core/ArabicShapingRegTest");
    }

    public void testBidi() throws Exception {
        runUtility("Core/Bidi");
    }

    public void testCalendar() throws Exception {
        runUtility("Core/Calendar");
    }

    public void testCompression() throws Exception {
        runUtility("Core/Compression");
    }

    public void testDiagBigDecimal() throws Exception {
        runUtility("Core/DiagBigDecimal");
    }

    public void testDuration() throws Exception {
        runUtility("Core/Duration");
    }

    public void testFormat() throws Exception {
        runUtility("Core/Format");
    }

    public void testImpl() throws Exception {
        runUtility("Core/Impl");
    }

    public void testNormalizer() throws Exception {
        runUtility("Core/Normalizer");
    }

    public void testProperty() throws Exception {
        runUtility("Core/Property");
    }

    public void testRBBI() throws Exception {
        runUtility("Core/RBBI");
    }

// Note: ICU serializable test cases do not support test data loading
// through Eclipse's class loader.
//    public void testSerializable() throws Exception {
//        runUtility("Core/Serializable");
//    }

    public void testSpoofChecker() throws Exception {
        runUtility("Core/SpoofChecker");
    }

    public void testStringPrep() throws Exception {
        runUtility("Core/StringPrep");
    }

    public void testTestCharsetDetector() throws Exception {
        runUtility("Core/TestCharsetDetector");
    }

    public void testTestUCharacterIterator() throws Exception {
        runUtility("Core/TestUCharacterIterator");
    }

    public void testTimeScale() throws Exception {
        runUtility("Core/TimeScale");
    }

    public void testTimeZone() throws Exception {
        runUtility("Core/TimeZone");
    }

    public void testUtil() throws Exception {
        runUtility("Core/Util");
    }


    // Translit

    public void testTranslit() throws Exception {
        runUtility("Translit/Translit");
    }
}
