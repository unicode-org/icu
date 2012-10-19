/*
 *******************************************************************************
 * Copyright (C) 1996-2012, Google, International Business Machines Corporation and
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.FieldPosition;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.NumberFormat;
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

    Object[][] SerbianTestDataShort = {
            {1234, "1200"},
            {12345, "12K"},
            {20789, "21\u00a0хиљ"},
            {123456, "120\u00a0хиљ"},
            {1234567, "1,2\u00a0мил"},
            {12345678, "12\u00a0мил"},
            {123456789, "120\u00a0мил"},
            {1234567890, "1,2\u00a0млрд"},
            {12345678901f, "12\u00a0млрд"},
            {123456789012f, "120\u00a0млрд"},
            {1234567890123f, "1,2\u00a0бил"},
            {12345678901234f, "12\u00a0бил"},
            {123456789012345f, "120\u00a0бил"},
            {1234567890123456f, "1200\u00a0бил"},
    };

    Object[][] SerbianTestDataLong = {
            {1234, "1,2 хиљада"},
            {12345, "12 хиљада"},
            {21789, "22 хиљаде"},
            {123456, "120 хиљада"},
            {999999, "1 милион"},
            {1234567, "1,2 милиона"},
            {12345678, "12 милиона"},
            {123456789, "120 милиона"},
            {1234567890, "1,2 милијарди"},
            {12345678901f, "12 милијарди"},
            {20890123456f, "21 милијарда"},
            {21890123456f, "22 милијарде"},
            {123456789012f, "120 милијарди"},
            {1234567890123f, "1,2 трилиона"},
            {12345678901234f, "12 трилиона"},
            {123456789012345f, "120 трилиона"},
            {1234567890123456f, "1200 трилиона"},
    };

    Object[][] SerbianTestDataLongNegative = {
            {-1234, "-1,2 хиљада"},
            {-12345, "-12 хиљада"},
            {-21789, "-22 хиљаде"},
            {-123456, "-120 хиљада"},
            {-999999, "-1 милион"},
            {-1234567, "-1,2 милиона"},
            {-12345678, "-12 милиона"},
            {-123456789, "-120 милиона"},
            {-1234567890, "-1,2 милијарди"},
            {-12345678901f, "-12 милијарди"},
            {-20890123456f, "-21 милијарда"},
            {-21890123456f, "-22 милијарде"},
            {-123456789012f, "-120 милијарди"},
            {-1234567890123f, "-1,2 трилиона"},
            {-12345678901234f, "-12 трилиона"},
            {-123456789012345f, "-120 трилиона"},
            {-1234567890123456f, "-1200 трилиона"},
    };

   Object[][] JapaneseTestData = {
            {1234f, "1.2千"},
            {12345f, "1.2万"},
            {123456f, "12万"},
            {1234567f, "120万"},
            {12345678f, "1200万"},
            {123456789f, "1.2億"},
            {1234567890f, "12億"},
            {12345678901f, "120億"},
            {123456789012f, "1200億"},
            {1234567890123f, "1.2兆"},
            {12345678901234f, "12兆"},
            {123456789012345f, "120兆"},
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
    
    Object[][] CsTestDataShort = {
            {1000, "1\u00a0tis."},
            {1500, "1,5\u00a0tis."},
            {5000, "5\u00a0tis."},
            {23000, "23\u00a0tis."},
            {127123, "130\u00a0tis."},
            {1271234, "1,3\u00a0mil."},
            {12712345, "13\u00a0mil."},
            {127123456, "130\u00a0mil."},
            {1271234567f, "1,3\u00a0mld."},
            {12712345678f, "13\u00a0mld."},
            {127123456789f, "130\u00a0mld."},
            {1271234567890f, "1,3\u00a0bil."},
            {12712345678901f, "13\u00a0bil."},
            {127123456789012f, "130\u00a0bil."},
    };
  
    Object[][] SkTestDataLong = {
            {1000, "1 tis\u00edc"},
            {1572, "1,6 tis\u00edc"},
            {5184, "5,2 tis\u00edc"},
    };

    Object[][] SwahiliTestDataNegative = {
            {-1234f, "elfu\u00a0-1.2"},
            {-12345f, "elfu\u00a0-12"},
            {-123456f, "laki-1.2"},
            {-1234567f, "M-1.2"},
            {-12345678f, "M-12"},
            {-123456789f, "M-120"},
            {-1234567890f, "B-1.2"},
            {-12345678901f, "B-12"},
            {-123456789012f, "B-120"},
            {-1234567890123f, "T-1.2"},
            {-12345678901234f, "T-12"},
            {-12345678901234567890f, "T-12000000"},
    };

    // TODO: Write a test for negative numbers in arabic.

    public void TestCharacterIterator() {
        CompactDecimalFormat cdf =
            CompactDecimalFormat.getInstance(ULocale.forLanguageTag("sw"), CompactStyle.SHORT);
        AttributedCharacterIterator iter = cdf.formatToCharacterIterator(1234567);
        assertEquals("CharacterIterator", "M1.2", iterToString(iter));
        iter = cdf.formatToCharacterIterator(1234567);
        iter.setIndex(1);
        assertEquals("Attributes", NumberFormat.Field.INTEGER, iter.getAttribute(NumberFormat.Field.INTEGER));
        assertEquals("Attributes", 1, iter.getRunStart());
        assertEquals("Attributes", 2, iter.getRunLimit());
    }

    public void TestEnglishShort() {
        checkLocale(ULocale.ENGLISH, CompactStyle.SHORT, EnglishTestData);
    }

    public void TestArabicLongStyle() {
        NumberFormat cdf =
                CompactDecimalFormat.getInstance(
                        ULocale.forLanguageTag("ar"), CompactStyle.LONG);
        assertEquals("Arabic Long", "\u0665\u066B\u0663- \u0623\u0644\u0641", cdf.format(-5300));
    }
    
    public void TestCsShort() {
        checkLocale(ULocale.forLanguageTag("cs"), CompactStyle.SHORT, CsTestDataShort);
    }
    
    public void TestSkLong() {
        checkLocale(ULocale.forLanguageTag("sk"), CompactStyle.LONG, SkTestDataLong);
    }

    public void TestSerbianShort() {
        checkLocale(ULocale.forLanguageTag("sr"), CompactStyle.SHORT, SerbianTestDataShort);
    }

    public void TestSerbianLong() {
        checkLocale(ULocale.forLanguageTag("sr"), CompactStyle.LONG, SerbianTestDataLong);
    }

    public void TestSerbianLongNegative() {
        checkLocale(ULocale.forLanguageTag("sr"), CompactStyle.LONG, SerbianTestDataLongNegative);
    }

    public void TestJapaneseShort() {
         checkLocale(ULocale.JAPANESE, CompactStyle.SHORT, JapaneseTestData);
    }

    public void TestSwahiliShort() {
        checkLocale(ULocale.forLanguageTag("sw"), CompactStyle.SHORT, SwahiliTestData);
    }

    public void TestSwahiliShortNegative() {
        checkLocale(ULocale.forLanguageTag("sw"), CompactStyle.SHORT, SwahiliTestDataNegative);
    }

    public void TestFieldPosition() {
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(
                ULocale.forLanguageTag("sw"), CompactStyle.SHORT);
        FieldPosition fp = new FieldPosition(0);
        StringBuffer sb = new StringBuffer();
        cdf.format(1234567f, sb, fp);
        assertEquals("fp string", "M1.2", sb.toString());
        assertEquals("fp start", 1, fp.getBeginIndex());
        assertEquals("fp end", 2, fp.getEndIndex());
    }

    public void checkLocale(ULocale locale, CompactStyle style, Object[][] testData) {
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(locale, style);
        for (Object[] row : testData) {
            assertEquals(locale + " (" + locale.getDisplayName(locale) + ")", row[1], cdf.format(row[0]));
        }
    }

    private static String iterToString(CharacterIterator iter) {
        StringBuilder builder = new StringBuilder();
        for (char c = iter.current(); c != CharacterIterator.DONE; c = iter.next()) {
            builder.append(c);
        }
        return builder.toString();
    }
}
