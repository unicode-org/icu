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
    
    Object[][] SerbianTestData = {
            {1234f, "1200"},
            {12345f, "12\u00a0\u0445\u0438\u0459"},
            {123456f, "120\u00a0\u0445\u0438\u0459"},
            {1234567f, "1,2\u00a0\u043c\u0438\u043b"},
            {12345678f, "12\u00a0\u043c\u0438\u043b"},
            {123456789f, "120\u00a0\u043c\u0438\u043b"},
            {1234567890f, "1,2\u00a0\u043c\u043b\u0440\u0434"},
            {12345678901f, "12\u00a0\u043c\u043b\u0440\u0434"},
            {123456789012f, "120\u00a0\u043c\u043b\u0440\u0434"},
            {1234567890123f, "1,2\u00a0\u0431\u0438\u043b"},
            {12345678901234f, "12\u00a0\u0431\u0438\u043b"},
            {123456789012345f, "120\u00a0\u0431\u0438\u043b"},
    };

    Object[][] JapaneseTestData = {
            {1234f, "1.2\u5343"},
            {12345f, "1.2\u4E07"},
            {123456f, "12\u4E07"},
            {1234567f, "120\u4E07"},
            {12345678f, "1200\u4E07"},
            {123456789f, "1.2\u5104"},
            {1234567890f, "12\u5104"},
            {12345678901f, "120\u5104"},
            {123456789012f, "1200\u5104"},
            {1234567890123f, "1.2\u5146"},
            {12345678901234f, "12\u5146"},
            {123456789012345f, "120\u5146"},
    };

    Object[][] SwahiliTestData = {
            {1234f, "elfu\u00a01.2"},
            {12345f, "elfu\u00a012"},
            {123456f, "laki1.2"},
            {1234567f, "M1.2"},
            {12345678f, "M12"},
            {123456789f, "M120"},
            {1234567890f, "B1.2"},
            {12345678901f, "B12"},
            {123456789012f, "B120"},
            {1234567890123f, "T1.2"},
            {12345678901234f, "T12"},
            {12345678901234567890f, "T12000000"},
    };

    public void TestEnglish() {
        checkLocale(ULocale.ENGLISH, EnglishTestData);
    }
    
    public void TestSerbian() {
        checkLocale(ULocale.forLanguageTag("sr"), SerbianTestData);
    }

    public void TestJapanese() {
         checkLocale(ULocale.JAPANESE, JapaneseTestData);
    }

    public void TestJapaneseGermany() {
        // check fallback.
        checkLocale(ULocale.forLanguageTag("ja-DE"), JapaneseTestData);
    }

    public void TestSwahili() {
        checkLocale(ULocale.forLanguageTag("sw"), SwahiliTestData);
    }

    public void checkLocale(ULocale locale, Object[][] testData) {
        CompactDecimalFormat cdf = NumberFormat.getCompactDecimalInstance(locale, CompactStyle.SHORT);
        for (Object[] row : testData) {
            assertEquals(locale + " (" + locale.getDisplayName(locale) + ")", row[1], cdf.format(row[0]));
        }
    }
}
