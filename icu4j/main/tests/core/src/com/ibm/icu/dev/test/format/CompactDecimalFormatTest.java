// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2016, Google, International Business Machines Corporation and
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormat.PropertySetter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class CompactDecimalFormatTest extends TestFmwk {
    Object[][] EnglishTestData = {
            // default is 2 digits of accuracy
            {0.0d, "0"},
            {0.01d, "0.01"},
            {0.1d, "0.1"},
            {1d, "1"},
            {12, "12"},
            {123, "120"},
            {1234, "1.2K"},
            {1000, "1K"},
            {1049, "1K"},
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
            {12345678901234567890f, "12,000,000T"},
    };

    Object[][] SerbianTestDataShort = {
            {1, "1"},
            {12, "12"},
            {123, "120"},
            {12345, "12\u00a0хиљ."},
            {20789, "21\u00a0хиљ."},
            {123456, "120\u00a0хиљ."},
            {1234567, "1,2\u00a0мил."},
            {12345678, "12\u00a0мил."},
            {123456789, "120\u00a0мил."},
            {1234567890, "1,2\u00a0млрд."},
            {12345678901f, "12\u00a0млрд."},
            {123456789012f, "120\u00a0млрд."},
            {1234567890123f, "1,2\u00a0бил."},
            {12345678901234f, "12\u00a0бил."},
            {123456789012345f, "120\u00a0бил."},
            {1234567890123456f, "1200\u00a0бил."},
    };

    Object[][] SerbianTestDataLong = {
            {1, "1"},
            {12, "12"},
            {123, "120"},
            {1234, "1,2 хиљаде"},
            {12345, "12 хиљада"},
            {21789, "22 хиљаде"},
            {123456, "120 хиљада"},
            {999999, "1 милион"},
            {1234567, "1,2 милиона"},
            {12345678, "12 милиона"},
            {123456789, "120 милиона"},
            {1234567890, "1,2 милијарде"},
            {12345678901f, "12 милијарди"},
            {20890123456f, "21 милијарда"},
            {21890123456f, "22 милијарде"},
            {123456789012f, "120 милијарди"},
            {1234567890123f, "1,2 билиона"},
            {12345678901234f, "12 билиона"},
            {123456789012345f, "120 билиона"},
            {1234567890123456f, "1200 билиона"},
    };

    Object[][] SerbianTestDataLongNegative = {
            {-1, "-1"},
            {-12, "-12"},
            {-123, "-120"},
            {-1234, "-1,2 хиљаде"},
            {-12345, "-12 хиљада"},
            {-21789, "-22 хиљаде"},
            {-123456, "-120 хиљада"},
            {-999999, "-1 милион"},
            {-1234567, "-1,2 милиона"},
            {-12345678, "-12 милиона"},
            {-123456789, "-120 милиона"},
            {-1234567890, "-1,2 милијарде"},
            {-12345678901f, "-12 милијарди"},
            {-20890123456f, "-21 милијарда"},
            {-21890123456f, "-22 милијарде"},
            {-123456789012f, "-120 милијарди"},
            {-1234567890123f, "-1,2 билиона"},
            {-12345678901234f, "-12 билиона"},
            {-123456789012345f, "-120 билиона"},
            {-1234567890123456f, "-1200 билиона"},
    };

    Object[][] JapaneseTestData = {
            {1f, "1"},
            {12f, "12"},
            {123f, "120"},
            {1234f, "1200"},
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

    Object[][] ChineseTestData = {
            {1f, "1"},
            {12f, "12"},
            {123f, "120"},
            {1234f, "1200"},
            {12345f, "1.2万"},
            {123456f, "12万"},
            {1234567f, "120万"},
            {12345678f, "1200万"},
            {123456789f, "1.2亿"},
            {1234567890f, "12亿"},
            {12345678901f, "120亿"},
            {123456789012f, "1200亿"},
            {1234567890123f, "1.2万亿"},
            {12345678901234f, "12万亿"},
            {123456789012345f, "120万亿"},
    };

    Object[][] ChineseCurrencyTestData = {
            {new CurrencyAmount(1f, Currency.getInstance("CNY")), "¥1"},
            {new CurrencyAmount(12f, Currency.getInstance("CNY")), "¥12"},
            {new CurrencyAmount(123f, Currency.getInstance("CNY")), "¥120"},
            {new CurrencyAmount(1234f, Currency.getInstance("CNY")), "¥1200"},
            {new CurrencyAmount(12345f, Currency.getInstance("CNY")), "¥1.2万"},
            {new CurrencyAmount(123456f, Currency.getInstance("CNY")), "¥12万"},
            {new CurrencyAmount(1234567f, Currency.getInstance("CNY")), "¥120万"},
            {new CurrencyAmount(12345678f, Currency.getInstance("CNY")), "¥1200万"},
            {new CurrencyAmount(123456789f, Currency.getInstance("CNY")), "¥1.2亿"},
            {new CurrencyAmount(1234567890f, Currency.getInstance("CNY")), "¥12亿"},
            {new CurrencyAmount(12345678901f, Currency.getInstance("CNY")), "¥120亿"},
            {new CurrencyAmount(123456789012f, Currency.getInstance("CNY")), "¥1200亿"},
            {new CurrencyAmount(1234567890123f, Currency.getInstance("CNY")), "¥1.2万亿"},
            {new CurrencyAmount(12345678901234f, Currency.getInstance("CNY")), "¥12万亿"},
            {new CurrencyAmount(123456789012345f, Currency.getInstance("CNY")), "¥120万亿"},
    };
    Object[][] GermanCurrencyTestData = {
            {new CurrencyAmount(1f, Currency.getInstance("EUR")), "1 €"},
            {new CurrencyAmount(12f, Currency.getInstance("EUR")), "12 €"},
            {new CurrencyAmount(123f, Currency.getInstance("EUR")), "120 €"},
            {new CurrencyAmount(1234f, Currency.getInstance("EUR")), "1200 €"},
            {new CurrencyAmount(12345f, Currency.getInstance("EUR")), "12.000 €"},
            {new CurrencyAmount(123456f, Currency.getInstance("EUR")), "120.000 €"},
            {new CurrencyAmount(1234567f, Currency.getInstance("EUR")), "1,2 Mio. €"},
            {new CurrencyAmount(12345678f, Currency.getInstance("EUR")), "12 Mio. €"},
            {new CurrencyAmount(123456789f, Currency.getInstance("EUR")), "120 Mio. €"},
            {new CurrencyAmount(1234567890f, Currency.getInstance("EUR")), "1,2 Mrd. €"},
            {new CurrencyAmount(12345678901f, Currency.getInstance("EUR")), "12 Mrd. €"},
            {new CurrencyAmount(123456789012f, Currency.getInstance("EUR")), "120 Mrd. €"},
            {new CurrencyAmount(1234567890123f, Currency.getInstance("EUR")), "1,2 Bio. €"},
            {new CurrencyAmount(12345678901234f, Currency.getInstance("EUR")), "12 Bio. €"},
            {new CurrencyAmount(123456789012345f, Currency.getInstance("EUR")), "120 Bio. €"},
    };
    Object[][] EnglishCurrencyTestData = {
            {new CurrencyAmount(1f, Currency.getInstance("USD")), "$1"},
            {new CurrencyAmount(12f, Currency.getInstance("USD")), "$12"},
            {new CurrencyAmount(123f, Currency.getInstance("USD")), "$120"},
            {new CurrencyAmount(1234f, Currency.getInstance("USD")), "$1.2K"},
            {new CurrencyAmount(12345f, Currency.getInstance("USD")), "$12K"},
            {new CurrencyAmount(123456f, Currency.getInstance("USD")), "$120K"},
            {new CurrencyAmount(1234567f, Currency.getInstance("USD")), "$1.2M"},
            {new CurrencyAmount(12345678f, Currency.getInstance("USD")), "$12M"},
            {new CurrencyAmount(123456789f, Currency.getInstance("USD")), "$120M"},
            {new CurrencyAmount(1234567890f, Currency.getInstance("USD")), "$1.2B"},
            {new CurrencyAmount(12345678901f, Currency.getInstance("USD")), "$12B"},
            {new CurrencyAmount(123456789012f, Currency.getInstance("USD")), "$120B"},
            {new CurrencyAmount(1234567890123f, Currency.getInstance("USD")), "$1.2T"},
            {new CurrencyAmount(12345678901234f, Currency.getInstance("USD")), "$12T"},
            {new CurrencyAmount(123456789012345f, Currency.getInstance("USD")), "$120T"},
    };

    Object[][] SwahiliTestData = {
            {1f, "1"},
            {12f, "12"},
            {123f, "120"},
            {1234f, "elfu\u00a01.2"},
            {12345f, "elfu\u00a012"},
            {123456f, "elfu\u00A0120"},
            {1234567f, "1.2M"},
            {12345678f, "12M"},
            {123456789f, "120M"},
            {1234567890f, "1.2B"},
            {12345678901f, "12B"},
            {123456789012f, "120B"},
            {1234567890123f, "1.2T"},
            {12345678901234f, "12T"},
            {12345678901234567890f, "12,000,000T"},
    };

    Object[][] CsTestDataShort = {
            {1, "1"},
            {12, "12"},
            {123, "120"},
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
            {1572, "1,6 tis\u00edca"},
            {5184, "5,2 tis\u00edca"},
    };

    Object[][] SwahiliTestDataNegative = {
            {-1f, "-1"},
            {-12f, "-12"},
            {-123f, "-120"},
            {-1234f, "elfu\u00a0-1.2"},
            {-12345f, "elfu\u00a0-12"},
            {-123456f, "elfu\u00a0-120"},
            {-1234567f, "-1.2M"},
            {-12345678f, "-12M"},
            {-123456789f, "-120M"},
            {-1234567890f, "-1.2B"},
            {-12345678901f, "-12B"},
            {-123456789012f, "-120B"},
            {-1234567890123f, "-1.2T"},
            {-12345678901234f, "-12T"},
            {-12345678901234567890f, "-12,000,000T"},
    };

    Object[][] TestACoreCompactFormatList = {
            {1000, "1K"},
            {1100, "1,1K"},
            {1200, "1,2Ks"},
            {2000, "2Ks"},
    };

    Object[][] TestACoreCompactFormatListCurrency = {
            {1000, "1K$"},
            {1100, "1,1K$"},
            {1200, "1,2Ks$s"},
            {2000, "2Ks$s"},
    };

    // TODO(sffc): Re-write these tests for the new CompactDecimalFormat pipeline

//    @Test
//    public void TestACoreCompactFormat() {
//        Map<String,String[][]> affixes = new HashMap();
//        affixes.put("one", new String[][] {
//                {"","",}, {"","",}, {"","",},
//                {"","K"}, {"","K"}, {"","K"},
//                {"","M"}, {"","M"}, {"","M"},
//                {"","B"}, {"","B"}, {"","B"},
//                {"","T"}, {"","T"}, {"","T"},
//        });
//        affixes.put("other", new String[][] {
//                {"","",}, {"","",}, {"","",},
//                {"","Ks"}, {"","Ks"}, {"","Ks"},
//                {"","Ms"}, {"","Ms"}, {"","Ms"},
//                {"","Bs"}, {"","Bs"}, {"","Bs"},
//                {"","Ts"}, {"","Ts"}, {"","Ts"},
//        });
//
//        Map<String,String[]> currencyAffixes = new HashMap();
//        currencyAffixes.put("one", new String[] {"", "$"});
//        currencyAffixes.put("other", new String[] {"", "$s"});
//
//        long[] divisors = new long[] {
//                0,0,0,
//                1000, 1000, 1000,
//                1000000, 1000000, 1000000,
//                1000000000L, 1000000000L, 1000000000L,
//                1000000000000L, 1000000000000L, 1000000000000L};
//        long[] divisors_err = new long[] {
//                0,0,0,
//                13, 13, 13,
//                1000000, 1000000, 1000000,
//                1000000000L, 1000000000L, 1000000000L,
//                1000000000000L, 1000000000000L, 1000000000000L};
//        checkCore(affixes, null, divisors, TestACoreCompactFormatList);
//        checkCore(affixes, currencyAffixes, divisors, TestACoreCompactFormatListCurrency);
//        try {
//            checkCore(affixes, null, divisors_err, TestACoreCompactFormatList);
//        } catch(AssertionError e) {
//            // Exception expected, thus return.
//            return;
//        }
//        fail("Error expected but passed");
//    }

//    private void checkCore(Map<String, String[][]> affixes, Map<String, String[]> currencyAffixes, long[] divisors, Object[][] testItems) {
//        Collection<String> debugCreationErrors = new LinkedHashSet();
//        CompactDecimalFormat cdf = new CompactDecimalFormat(
//                "#,###.00",
//                DecimalFormatSymbols.getInstance(new ULocale("fr")),
//                CompactStyle.SHORT, PluralRules.createRules("one: j is 1 or f is 1"),
//                divisors, affixes, currencyAffixes,
//                debugCreationErrors
//                );
//        if (debugCreationErrors.size() != 0) {
//            for (String s : debugCreationErrors) {
//                errln("Creation error: " + s);
//            }
//        } else {
//            checkCdf("special cdf ", cdf, testItems);
//        }
//    }

    @Test
    public void TestDefaultSignificantDigits() {
        // We are expecting two significant digits for compact formats with one or two zeros,
        // and rounded to the unit for compact formats with three or more zeros.
        CompactDecimalFormat cdf =
                CompactDecimalFormat.getInstance(ULocale.ENGLISH, CompactStyle.SHORT);
        assertEquals("Default significant digits", "123K", cdf.format(123456));
        assertEquals("Default significant digits", "12K", cdf.format(12345));
        assertEquals("Default significant digits", "1.2K", cdf.format(1234));
        assertEquals("Default significant digits", "123", cdf.format(123));
    }

    @Test
    public void TestCharacterIterator() {
        CompactDecimalFormat cdf =
                getCDFInstance(ULocale.forLanguageTag("sw"), CompactStyle.SHORT);
        AttributedCharacterIterator iter = cdf.formatToCharacterIterator(1234567);
        assertEquals("CharacterIterator", "1.2M", iterToString(iter));
        iter = cdf.formatToCharacterIterator(1234567);
        iter.setIndex(0);
        assertEquals("Attributes", NumberFormat.Field.INTEGER, iter.getAttribute(NumberFormat.Field.INTEGER));
        assertEquals("Attributes", 0, iter.getRunStart());
        assertEquals("Attributes", 1, iter.getRunLimit());
    }

    @Test
    public void TestEnglishShort() {
        checkLocale(ULocale.ENGLISH, CompactStyle.SHORT, EnglishTestData);
    }

    @Test
    public void TestArabicLongStyle() {
        NumberFormat cdf =
                CompactDecimalFormat.getInstance(new Locale("ar-EG"), CompactStyle.LONG);
        assertEquals("Arabic Long", "\u061C-\u0665\u066B\u0663 \u0623\u0644\u0641", cdf.format(-5300));
    }

    @Test
    public void TestCsShort() {
        checkLocale(ULocale.forLanguageTag("cs"), CompactStyle.SHORT, CsTestDataShort);
    }

    @Test
    public void TestSkLong() {
        checkLocale(ULocale.forLanguageTag("sk"), CompactStyle.LONG, SkTestDataLong);
    }

    @Test
    public void TestSerbianShort() {
        checkLocale(ULocale.forLanguageTag("sr"), CompactStyle.SHORT, SerbianTestDataShort);
    }

    @Test
    public void TestSerbianLong() {
        checkLocale(ULocale.forLanguageTag("sr"), CompactStyle.LONG, SerbianTestDataLong);
    }

    @Test
    public void TestSerbianLongNegative() {
        checkLocale(ULocale.forLanguageTag("sr"), CompactStyle.LONG, SerbianTestDataLongNegative);
    }

    @Test
    public void TestJapaneseShort() {
        checkLocale(ULocale.JAPANESE, CompactStyle.SHORT, JapaneseTestData);
    }

     @Test
    public void TestChineseShort() {
        checkLocale(ULocale.CHINESE, CompactStyle.SHORT, ChineseTestData);
    }

   @Test
    public void TestSwahiliShort() {
        checkLocale(ULocale.forLanguageTag("sw"), CompactStyle.SHORT, SwahiliTestData);
    }

    @Test
    public void TestSwahiliShortNegative() {
        checkLocale(ULocale.forLanguageTag("sw"), CompactStyle.SHORT, SwahiliTestDataNegative);
    }

    @Test
    public void TestEnglishCurrency() {
        checkLocale(ULocale.ENGLISH, CompactStyle.SHORT, EnglishCurrencyTestData);
    }

    @Test
    public void TestGermanCurrency() {
        checkLocale(ULocale.GERMAN, CompactStyle.SHORT, GermanCurrencyTestData);
    }

    @Test
    public void TestChineseCurrency() {
        checkLocale(ULocale.CHINESE, CompactStyle.SHORT, ChineseCurrencyTestData);
    }

    @Test
    public void TestFieldPosition() {
        CompactDecimalFormat cdf = getCDFInstance(
                ULocale.forLanguageTag("sw"), CompactStyle.SHORT);
        FieldPosition fp = new FieldPosition(0);
        StringBuffer sb = new StringBuffer();
        cdf.format(1234567f, sb, fp);
        assertEquals("fp string", "1.2M", sb.toString());
        assertEquals("fp start", 0, fp.getBeginIndex());
        assertEquals("fp end", 1, fp.getEndIndex());
    }

    @Test
    public void TestEquals() {
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(
                ULocale.forLanguageTag("sw"), CompactStyle.SHORT);
        CompactDecimalFormat equalsCdf = CompactDecimalFormat.getInstance(
                ULocale.forLanguageTag("sw"), CompactStyle.SHORT);
        CompactDecimalFormat notEqualsCdf = CompactDecimalFormat.getInstance(
                ULocale.forLanguageTag("sw"), CompactStyle.LONG);
        assertEquals("equals", cdf, equalsCdf);
        assertNotEquals("not equals", cdf, notEqualsCdf);

    }

    @Test
    public void TestBig() {
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(
                ULocale.ENGLISH, CompactStyle.LONG);
        BigInteger source_int = new BigInteger("31415926535897932384626433");
        cdf.setMaximumFractionDigits(0);
        assertEquals("BigInteger format wrong: ", "31,415,926,535,898 trillion",
                     cdf.format(source_int));
        BigDecimal source_dec = new BigDecimal(source_int);
        assertEquals("BigDecimal format wrong: ", "31,415,926,535,898 trillion",
                     cdf.format(source_dec));
    }

    @Test
    public void TestParsing() {
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(
                ULocale.ENGLISH, CompactStyle.LONG);
        try{
            cdf.parse("Parse for failure", new ParsePosition(0));
        } catch(UnsupportedOperationException e) {
            // Exception expected, thus return.
            return;
        }
        fail("Parsing currently unsupported, expected test to fail but passed");
    }

    public void checkLocale(ULocale locale, CompactStyle style, Object[][] testData) {
        CompactDecimalFormat cdf = getCDFInstance(locale, style);
        checkCdf(locale + " (" + locale.getDisplayName(locale) + ") for ", cdf, testData);
    }

    private void checkCdf(String title, CompactDecimalFormat cdf, Object[][] testData) {
        for (Object[] row : testData) {
            Object source = row[0];
            Object expected = row[1];
            assertEquals(title + source, expected,
                    cdf.format(source));
        }
    }

    private static String iterToString(CharacterIterator iter) {
        StringBuilder builder = new StringBuilder();
        for (char c = iter.current(); c != CharacterIterator.DONE; c = iter.next()) {
            builder.append(c);
        }
        return builder.toString();
    }

    private static CompactDecimalFormat getCDFInstance(ULocale locale, CompactStyle style) {
        CompactDecimalFormat result = CompactDecimalFormat.getInstance(locale, style);
        // Our tests are written for two significant digits. We set explicitly here
        // because default significant digits may change.
        result.setMaximumSignificantDigits(2);
        return result;
    }

    @Test
    public void TestNordic() {
        String result = CompactDecimalFormat.getInstance( new ULocale("no_NO"),
                CompactDecimalFormat.CompactStyle.SHORT ).format(12000);
        assertNotEquals("CDF(12,000) for no_NO shouldn't be 12 (12K or similar)", "12", result);
    }

    @Test
    public void TestWriteAndReadObject() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream objoutstream = new ObjectOutputStream(baos);
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(
                ULocale.ENGLISH, CompactStyle.LONG);

        try {
            objoutstream.writeObject(cdf);
        } catch (NotSerializableException e) {
            if (logKnownIssue("10494", "PluralRules is not serializable")) {
                logln("NotSerializableException thrown when serializing CopactDecimalFormat");
            } else {
                errln("NotSerializableException thrown when serializing CopactDecimalFormat");
            }
        } finally {
            objoutstream.close();
        }

        // This test case relies on serialized byte stream which might be premature
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream objinstream = new ObjectInputStream(bais);

        try {
            cdf = (CompactDecimalFormat) objinstream.readObject();
        } catch (NotSerializableException e) {
            if (logKnownIssue("10494", "PluralRules is not de-serializable")) {
                logln("NotSerializableException thrown when deserializing CopactDecimalFormat");
            } else {
                errln("NotSerializableException thrown when deserializing CopactDecimalFormat");
            }
        } catch (ClassNotFoundException e) {
            errln("ClassNotFoundException: " + e.getMessage());
        } finally {
            objinstream.close();
        }
    }

    @Test
    public void TestLongShortFallback() {
        // smn, dz have long but not short
        // es_US, es_GT, es_419, ee have short but not long
        // TODO: This test is out-of-date. The locales have more data as of ICU 60.
        ULocale[] locs = new ULocale[] {
            new ULocale("smn"),
            new ULocale("es_US"),
            new ULocale("es_GT"),
            new ULocale("es_419"),
            new ULocale("ee"),
        };
        double number = 12345.0;
        // These expected values are the same in both ICU 58 and 59.
        String[][] expectedShortLong = new String[][] {
            { "12K", "12 tuhháát" },
            { "12 K", "12 mil" },
            { "12 k", "12 mil" },
            { "12 k", "12 mil" },
            { "12K", "akpe 12" },
        };

        for (int i=0; i<locs.length; i++) {
            ULocale loc = locs[i];
            String expectedShort = expectedShortLong[i][0];
            String expectedLong = expectedShortLong[i][1];
            CompactDecimalFormat cdfShort = CompactDecimalFormat.getInstance(loc, CompactStyle.SHORT);
            CompactDecimalFormat cdfLong = CompactDecimalFormat.getInstance(loc, CompactStyle.LONG);
            String actualShort = cdfShort.format(number);
            String actualLong = cdfLong.format(number);
            assertEquals("Short, locale " + loc, expectedShort, actualShort);
            assertEquals("Long, locale " + loc, expectedLong, actualLong);
        }
    }

    @Test
    public void TestLocales() {
        // Run a CDF over all locales to make sure there are no unexpected exceptions.
        ULocale[] locs = ULocale.getAvailableLocales();
        for (ULocale loc : locs) {
            CompactDecimalFormat cdfShort = CompactDecimalFormat.getInstance(loc, CompactStyle.SHORT);
            CompactDecimalFormat cdfLong = CompactDecimalFormat.getInstance(loc, CompactStyle.LONG);
            for (double d = 12345.0; d > 0.01; d /= 10) {
                String s1 = cdfShort.format(d);
                String s2 = cdfLong.format(d);
                assertNotNull("Short " + loc, s1);
                assertNotNull("Long " + loc, s2);
                assertNotEquals("Short " + loc, 0, s1.length());
                assertNotEquals("Long " + loc, 0, s2.length());
            }
        }
    }

    @Test
    public void TestDigitDisplay() {
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(ULocale.US, CompactStyle.SHORT);
        cdf.setMinimumSignificantDigits(2);
        cdf.setMaximumSignificantDigits(3);
        String actual = cdf.format(70123.45678);
        assertEquals("Should not display any extra fraction digits", "70.1K", actual);
    }

    @Test
    public void TestLocaleGroupingForLargeNumbers() {
        ULocale[] locs = {new ULocale("en"), new ULocale("it"), new ULocale("en_US_POSIX"), new ULocale("en-IN")};
        String[] expecteds = {"5,800,000T", "5.800.000 Bln", "5800000T", "58,00,000T"};
        for (int i=0; i<locs.length; i++) {
            ULocale loc = locs[i];
            String exp = expecteds[i];
            CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(loc, CompactStyle.SHORT);
            String act = cdf.format(5.8e18);
            assertEquals("Grouping sizes for very large numbers: " + loc, exp, act);
        }
    }

    @Test
    public void TestCustomData() {
        final Map<String,Map<String,String>> customData = new HashMap<String,Map<String,String>>();
        Map<String,String> inner = new HashMap<String,String>();
        inner.put("one", "0 qwerty");
        inner.put("other", "0 dvorak");
        customData.put("1000", inner);
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(ULocale.ENGLISH, CompactStyle.SHORT);
        cdf.setProperties(new PropertySetter() {
            @Override
            public void set(DecimalFormatProperties props) {
                props.setCompactCustomData(customData);
            }
        });
        assertEquals("Below custom range", "123", cdf.format(123));
        assertEquals("Plural form one", "1 qwerty", cdf.format(1000));
        assertEquals("Plural form other", "1.2 dvorak", cdf.format(1234));
        assertEquals("Above custom range", "12 dvorak", cdf.format(12345));
        assertEquals("Negative number", "-1 qwerty", cdf.format(-1000));
    }

    @Test
    public void TestBug12422() {
        CompactDecimalFormat cdf;
        String result;

        // Bug #12422
        cdf = CompactDecimalFormat.getInstance(new ULocale("ar", "SA"), CompactDecimalFormat.CompactStyle.LONG);
        result = cdf.format(43000);
        assertEquals("CDF should correctly format 43000 in 'ar'", "٤٣ ألف", result);

        // Bug #12449
        cdf = CompactDecimalFormat.getInstance(new ULocale("ar-EG"), CompactDecimalFormat.CompactStyle.SHORT);
        cdf.setMaximumSignificantDigits(3);
        result = cdf.format(1234);
        assertEquals("CDF should correctly format 1234 with 3 significant digits in 'ar-EG'", "١٫٢٣ ألف", result);

        // Check currency formatting as well
        cdf = CompactDecimalFormat.getInstance(new ULocale("ar-EG"), CompactDecimalFormat.CompactStyle.SHORT);
        result = cdf.format(new CurrencyAmount(43000f, Currency.getInstance("USD")));
        assertEquals("CDF should correctly format 43000 with currency in 'ar-EG'", "٤٣ ألف US$", result);
        result = cdf.format(new CurrencyAmount(-43000f, Currency.getInstance("USD")));
        assertEquals("CDF should correctly format -43000 with currency in 'ar-EG'", "؜-٤٣ ألف US$", result);

        // Extra locale with different positive/negative formats
        cdf = CompactDecimalFormat.getInstance(new ULocale("fi"), CompactDecimalFormat.CompactStyle.SHORT);
        result = cdf.format(new CurrencyAmount(43000f, Currency.getInstance("USD")));
        assertEquals("CDF should correctly format 43000 with currency in 'fi'", "43 t. $", result);
        result = cdf.format(new CurrencyAmount(-43000f, Currency.getInstance("USD")));
        assertEquals("CDF should correctly format -43000 with currency in 'fi'", "−43 t. $", result);
    }


    @Test
    public void TestBug12689() {
        CompactDecimalFormat cdf;
        String result;

        cdf = CompactDecimalFormat.getInstance(ULocale.forLanguageTag("en"), CompactStyle.SHORT);
        result = cdf.format(new CurrencyAmount(123, Currency.getInstance("USD")));
        assertEquals("CDF should correctly format 123 with currency in 'en'", "$123", result);

        cdf = CompactDecimalFormat.getInstance(ULocale.forLanguageTag("it"), CompactStyle.SHORT);
        result = cdf.format(new CurrencyAmount(123, Currency.getInstance("EUR")));
        assertEquals("CDF should correctly format 123 with currency in 'it'", "123\u00A0€", result);
    }

    @Test
    public void TestBug12688() {
        CompactDecimalFormat cdf;
        String result;

        cdf = CompactDecimalFormat.getInstance(ULocale.forLanguageTag("it"), CompactStyle.SHORT);
        result = cdf.format(new CurrencyAmount(123000, Currency.getInstance("EUR")));
        assertEquals("CDF should correctly format 123000 with currency in 'it'", "123.000\u00A0€", result);
    }

    @Test
    public void TestBug11319() {
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(new ULocale("yue-HK"), CompactStyle.SHORT);
        String result = cdf.format(958000000L);
        assertEquals("CDF should correctly format 958 million in yue-HK", "9.6億", result);
    }

    @Test
    public void TestBug12975() {
        ULocale locale = new ULocale("it");
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(locale, CompactStyle.SHORT);
        String resultCdf = cdf.format(12000);
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
        String resultDefault = df.format(12000);
        assertEquals("CompactDecimalFormat should use default pattern when compact pattern is unavailable",
                     resultDefault, resultCdf);
    }

    @Test
    public void TestBug11534() {
        ULocale locale = new ULocale("pt_PT");
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(locale, CompactStyle.SHORT);
        String result = cdf.format(1000);
        assertEquals("pt_PT should fall back to pt", "1 mil", result);
    }

    @Test
    public void TestBug12181() {
        ULocale loc = ULocale.ENGLISH;
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(loc, CompactStyle.SHORT);
        String s = cdf.format(-1500);
        assertEquals("Should work with negative numbers", "-1.5K", s);
    }

    @Test
    public void TestBug13156() {
        ULocale loc = ULocale.ENGLISH;
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(loc, CompactStyle.SHORT);
        cdf.setMaximumFractionDigits(1);
        String result = cdf.format(0.01);
        assertEquals("Should not throw exception on small number", "0", result);
    }
}
