/*
 *******************************************************************************
 * Copyright (C) 1996-2012, Google, International Business Machines Corporation and
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberFormat.CompactStyle;
import com.ibm.icu.util.ULocale;

public class CompactDecimalFormatTest extends TestFmwk {
    
    public static void main(String[] args) {
        new CompactDecimalFormatTest().run(args);
    }

    Object[][] EnglishTestData = {
            // default is 2 digits of accuracy
            {0.0d, "0.0"},
            {0.1d, "0.1"},
            {1d, "1"},
            {1234, "1.2K"},
            {12345, "12K"},
            {123456, "120K"},
            {1234567, "1.2M"},
            {12345678, "12M"},
            {123456789, "120M"},
            {1234567890, "1.2B"},
            {12345678901f, "12B"},
            {123456789012f, "120B"},
            {1234567890123f, "1.2T"},
            {12345678901234f, "12T"},
            {123456789012345f, "120T"},
            {12345678901234567890f, "12000000T"},
    };
    
    Object[][] JapaneseTestData = {
            {1234, "1200"},
            {12345, "1.2万"},
            {123456, "12万"},
            {1234567, "120万"},
            {12345678, "1200万"},
            {123456789, "1.2億"},
            {1234567890, "12億"},
            {12345678901f, "120億"},
            {123456789012f, "1200億"},
            {1234567890123f, "1.2兆"},
            {12345678901234f, "12兆"},
            {123456789012345f, "120兆"},
    };

    public void TestEnglish() {
        checkLocale(ULocale.ENGLISH, EnglishTestData);
    }

    public void TestJapanese() {
        checkLocale(ULocale.JAPANESE, JapaneseTestData);
    }

    public void TestJapaneseGermany() {
        // check fallback.
        checkLocale(ULocale.forLanguageTag("ja-DE"), JapaneseTestData);
    }

    public void checkLocale(ULocale locale, Object[][] testData) {
        CompactDecimalFormat cdf = NumberFormat.getCompactDecimalInstance(locale, CompactStyle.SHORT);
        for (Object[] row : testData) {
            assertEquals(locale + " (" + locale.getDisplayName(locale) + ")", row[1], cdf.format((Number) row[0]));
        }
    }
}
