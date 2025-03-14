// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.FieldPosition;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.dev.test.serializable.FormatHandler;
import com.ibm.icu.dev.test.serializable.SerializableTestUtility;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.MeasureUnit.Complexity;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
import com.ibm.icu.util.ULocale;

/**
 * This file contains regular unit tests.
 */
@RunWith(JUnit4.class)
public class MeasureUnitTest extends CoreTestFmwk {

    @Test
    public void TestExamplesInDocs() {
        MeasureFormat fmtFr = MeasureFormat.getInstance(
                ULocale.FRENCH, FormatWidth.SHORT);
        Measure measure = new Measure(23, MeasureUnit.CELSIUS);
        assertEquals("23\u202F°C", "23\u202F°C", fmtFr.format(measure));
        Measure measureF = new Measure(70, MeasureUnit.FAHRENHEIT);
        assertEquals("70\u202F°F", "70\u202F°F", fmtFr.format(measureF));
        MeasureFormat fmtFrFull = MeasureFormat.getInstance(
                ULocale.FRENCH, FormatWidth.WIDE);
        assertEquals(
                "70 pied et 5,3 pouces",
                "70 pieds et 5,3 pouces",
                fmtFrFull.formatMeasures(
                        new Measure(70, MeasureUnit.FOOT),
                        new Measure(5.3, MeasureUnit.INCH)));
        assertEquals(
                "1\u00A0pied et 1\u00A0pouce",
                "1\u00A0pied et 1\u00A0pouce",
                fmtFrFull.formatMeasures(
                        new Measure(1, MeasureUnit.FOOT),
                        new Measure(1, MeasureUnit.INCH)));
        MeasureFormat fmtFrNarrow = MeasureFormat.getInstance(
                ULocale.FRENCH, FormatWidth.NARROW);
        assertEquals(
                "1′ 1″",
                "1′ 1″",
                fmtFrNarrow.formatMeasures(
                        new Measure(1, MeasureUnit.FOOT),
                        new Measure(1, MeasureUnit.INCH)));
        MeasureFormat fmtEn = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals(
                "1 inch, 2 feet",
                "1 inch, 2 feet",
                fmtEn.formatMeasures(
                        new Measure(1, MeasureUnit.INCH),
                        new Measure(2, MeasureUnit.FOOT)));
    }

    @Test
    public void TestFormatPeriodEn() {
        TimeUnitAmount[] _19m = {new TimeUnitAmount(19.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _1h_23_5s = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(23.5, TimeUnit.SECOND)};
        TimeUnitAmount[] _1h_23_5m = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(23.5, TimeUnit.MINUTE)};
        TimeUnitAmount[] _1h_0m_23s = {
                new TimeUnitAmount(1.0, TimeUnit.HOUR),
                new TimeUnitAmount(0.0, TimeUnit.MINUTE),
                new TimeUnitAmount(23.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _2y_5M_3w_4d = {
                new TimeUnitAmount(2.0, TimeUnit.YEAR),
                new TimeUnitAmount(5.0, TimeUnit.MONTH),
                new TimeUnitAmount(3.0, TimeUnit.WEEK),
                new TimeUnitAmount(4.0, TimeUnit.DAY)};
        TimeUnitAmount[] _1m_59_9996s = {
                new TimeUnitAmount(1.0, TimeUnit.MINUTE),
                new TimeUnitAmount(59.9996, TimeUnit.SECOND)};
        TimeUnitAmount[] _5h_17m = {
                new TimeUnitAmount(5.0, TimeUnit.HOUR),
                new TimeUnitAmount(17.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _neg5h_17m = {
                new TimeUnitAmount(-5.0, TimeUnit.HOUR),
                new TimeUnitAmount(17.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _19m_28s = {
                new TimeUnitAmount(19.0, TimeUnit.MINUTE),
                new TimeUnitAmount(28.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _0h_0m_9s = {
                new TimeUnitAmount(0.0, TimeUnit.HOUR),
                new TimeUnitAmount(0.0, TimeUnit.MINUTE),
                new TimeUnitAmount(9.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _0h_0m_17s = {
                new TimeUnitAmount(0.0, TimeUnit.HOUR),
                new TimeUnitAmount(0.0, TimeUnit.MINUTE),
                new TimeUnitAmount(17.0, TimeUnit.SECOND)};
        TimeUnitAmount[] _6h_56_92m = {
                new TimeUnitAmount(6.0, TimeUnit.HOUR),
                new TimeUnitAmount(56.92, TimeUnit.MINUTE)};
        TimeUnitAmount[] _3h_4s_5m = {
                new TimeUnitAmount(3.0, TimeUnit.HOUR),
                new TimeUnitAmount(4.0, TimeUnit.SECOND),
                new TimeUnitAmount(5.0, TimeUnit.MINUTE)};
        TimeUnitAmount[] _6_7h_56_92m = {
                new TimeUnitAmount(6.7, TimeUnit.HOUR),
                new TimeUnitAmount(56.92, TimeUnit.MINUTE)};
        TimeUnitAmount[] _3h_5h = {
                new TimeUnitAmount(3.0, TimeUnit.HOUR),
                new TimeUnitAmount(5.0, TimeUnit.HOUR)};

        Object[][] fullData = {
                {_1m_59_9996s, "1 minute, 59.9996 seconds"},
                {_19m, "19 minutes"},
                {_1h_23_5s, "1 hour, 23.5 seconds"},
                {_1h_23_5m, "1 hour, 23.5 minutes"},
                {_1h_0m_23s, "1 hour, 0 minutes, 23 seconds"},
                {_2y_5M_3w_4d, "2 years, 5 months, 3 weeks, 4 days"}};
        Object[][] abbrevData = {
                {_1m_59_9996s, "1 min, 59.9996 sec"},
                {_19m, "19 min"},
                {_1h_23_5s, "1 hr, 23.5 sec"},
                {_1h_23_5m, "1 hr, 23.5 min"},
                {_1h_0m_23s, "1 hr, 0 min, 23 sec"},
                {_2y_5M_3w_4d, "2 yrs, 5 mths, 3 wks, 4 days"}};
        Object[][] narrowData = {
                {_1m_59_9996s, "1m 59.9996s"},
                {_19m, "19m"},
                {_1h_23_5s, "1h 23.5s"},
                {_1h_23_5m, "1h 23.5m"},
                {_1h_0m_23s, "1h 0m 23s"},
                {_2y_5M_3w_4d, "2y 5m 3w 4d"}};


        Object[][] numericData = {
                {_1m_59_9996s, "1:59.9996"},
                {_19m, "19m"},
                {_1h_23_5s, "1:00:23.5"},
                {_1h_0m_23s, "1:00:23"},
                {_1h_23_5m, "1:23.5"},
                {_5h_17m, "5:17"},
                {_neg5h_17m, "-5h 17m"},
                {_19m_28s, "19:28"},
                {_2y_5M_3w_4d, "2y 5m 3w 4d"},
                {_0h_0m_9s, "0:00:09"},
                {_6h_56_92m, "6:56.92"},
                {_6_7h_56_92m, "6:56.92"},
                {_3h_4s_5m, "3h 4s 5m"},
                {_3h_5h, "3h 5h"}};
        Object[][] fullDataDe = {
                {_1m_59_9996s, "1 Minute, 59,9996 Sekunden"},
                {_19m, "19 Minuten"},
                {_1h_23_5s, "1 Stunde, 23,5 Sekunden"},
                {_1h_23_5m, "1 Stunde, 23,5 Minuten"},
                {_1h_0m_23s, "1 Stunde, 0 Minuten und 23 Sekunden"},
                {_2y_5M_3w_4d, "2 Jahre, 5 Monate, 3 Wochen und 4 Tage"}};
        Object[][] numericDataDe = {
                {_1m_59_9996s, "1:59,9996"},
                {_19m, "19 Min."},
                {_1h_23_5s, "1:00:23,5"},
                {_1h_0m_23s, "1:00:23"},
                {_1h_23_5m, "1:23,5"},
                {_5h_17m, "5:17"},
                {_19m_28s, "19:28"},
                {_2y_5M_3w_4d, "2 J, 5 M, 3 W und 4 T"},
                {_0h_0m_17s, "0:00:17"},
                {_6h_56_92m, "6:56,92"},
                {_3h_5h, "3 Std., 5 Std."}};
        Object[][] numericDataBn = {
                {_1m_59_9996s, "১:৫৯.৯৯৯৬"},
                {_19m, "১৯ মিঃ"},
                {_1h_23_5s, "১:০০:২৩.৫"},
                {_1h_0m_23s, "১:০০:২৩"},
                {_1h_23_5m, "১:২৩.৫"},
                {_5h_17m, "৫:১৭"},
                {_19m_28s, "১৯:২৮"},
                {_2y_5M_3w_4d, "২ বছর, ৫ মাস, ৩ সপ্তাহ, ৪ দিন"},
                {_0h_0m_17s, "০:০০:১৭"},
                {_6h_56_92m, "৬:৫৬.৯২"},
                {_3h_5h, "৩ ঘঃ, ৫ ঘঃ"}};
        Object[][] numericDataBnLatn = {
                {_1m_59_9996s, "1:59.9996"},
                {_19m, "19 মিঃ"},
                {_1h_23_5s, "1:00:23.5"},
                {_1h_0m_23s, "1:00:23"},
                {_1h_23_5m, "1:23.5"},
                {_5h_17m, "5:17"},
                {_19m_28s, "19:28"},
                {_2y_5M_3w_4d, "2 বছর, 5 মাস, 3 সপ্তাহ, 4 দিন"},
                {_0h_0m_17s, "0:00:17"},
                {_6h_56_92m, "6:56.92"},
                {_3h_5h, "3 ঘঃ, 5 ঘঃ"}};

        NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);
        nf.setMaximumFractionDigits(4);
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE, nf);
        verifyFormatPeriod("en FULL", mf, fullData);
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT, nf);
        verifyFormatPeriod("en SHORT", mf, abbrevData);
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NARROW, nf);
        verifyFormatPeriod("en NARROW", mf, narrowData);
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("en NUMERIC", mf, numericData);

        nf = NumberFormat.getNumberInstance(ULocale.GERMAN);
        nf.setMaximumFractionDigits(4);
        mf = MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.WIDE, nf);
        verifyFormatPeriod("de FULL", mf, fullDataDe);
        mf = MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("de NUMERIC", mf, numericDataDe);

        // Same tests, with Java Locale
        nf = NumberFormat.getNumberInstance(Locale.GERMAN);
        nf.setMaximumFractionDigits(4);
        mf = MeasureFormat.getInstance(Locale.GERMAN, FormatWidth.WIDE, nf);
        verifyFormatPeriod("de FULL(Java Locale)", mf, fullDataDe);
        mf = MeasureFormat.getInstance(Locale.GERMAN, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("de NUMERIC(Java Locale)", mf, numericDataDe);

        ULocale bengali = ULocale.forLanguageTag("bn");
        nf = NumberFormat.getNumberInstance(bengali);
        nf.setMaximumFractionDigits(4);
        mf = MeasureFormat.getInstance(bengali, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("bn NUMERIC(Java Locale)", mf, numericDataBn);

        bengali = ULocale.forLanguageTag("bn-u-nu-latn");
        nf = NumberFormat.getNumberInstance(bengali);
        nf.setMaximumFractionDigits(4);
        mf = MeasureFormat.getInstance(bengali, FormatWidth.NUMERIC, nf);
        verifyFormatPeriod("bn NUMERIC(Java Locale)", mf, numericDataBnLatn);
    }

    private void verifyFormatPeriod(String desc, MeasureFormat mf, Object[][] testData) {
        StringBuilder builder = new StringBuilder();
        boolean failure = false;
        for (Object[] testCase : testData) {
            String actual = mf.format(testCase[0]);
            if (!testCase[1].equals(actual)) {
                builder.append(String.format("%s: Expected: '%s', got: '%s'\n", desc, testCase[1], actual));
                failure = true;
            }
        }
        if (failure) {
            errln(builder.toString());
        }
    }

    @Test
    public void Test10219FractionalPlurals() {
        double[] values = {1.588, 1.011};
        String[][] expected = {
                {"1 minute", "1.5 minutes", "1.58 minutes"},
                {"1 minute", "1.0 minutes", "1.01 minutes"}
        };
        for (int j = 0; j < values.length; j++) {
            for (int i = 0; i < expected[j].length; i++) {
                NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);
                nf.setRoundingMode(BigDecimal.ROUND_DOWN);
                nf.setMinimumFractionDigits(i);
                nf.setMaximumFractionDigits(i);
                MeasureFormat mf = MeasureFormat.getInstance(
                        ULocale.ENGLISH, FormatWidth.WIDE, nf);
                assertEquals("Test10219", expected[j][i], mf.format(new Measure(values[j], MeasureUnit.MINUTE)));
            }
        }
    }

    @Test
    public void TestGreek() {
        String[] locales = {"el_GR", "el"};
        final MeasureUnit[] units = new MeasureUnit[]{
                MeasureUnit.SECOND,
                MeasureUnit.MINUTE,
                MeasureUnit.HOUR,
                MeasureUnit.DAY,
                MeasureUnit.WEEK,
                MeasureUnit.MONTH,
                MeasureUnit.YEAR};
        FormatWidth[] styles = new FormatWidth[] {FormatWidth.WIDE, FormatWidth.SHORT};
        int[] numbers = new int[] {1, 7};
        String[] expected = {
                // "el_GR" 1 wide
                "1 δευτερόλεπτο",
                "1 λεπτό",
                "1 ώρα",
                "1 ημέρα",
                "1 εβδομάδα",
                "1 μήνας",
                "1 έτος",
                // "el_GR" 1 short
                "1 δευτ.",
                "1 λ.",
                "1 ώ.",
                "1 ημέρα",
                "1 εβδ.",
                "1 μήν.",
                "1 έτ.",	        // year (one)
                // "el_GR" 7 wide
                "7 δευτερόλεπτα",
                "7 λεπτά",
                "7 ώρες",
                "7 ημέρες",
                "7 εβδομάδες",
                "7 μήνες",
                "7 έτη",
                // "el_GR" 7 short
                "7 δευτ.",
                "7 λ.",
                "7 ώ.",		       // hour (other)
                "7 ημέρες",
                "7 εβδ.",
                "7 μήν.",
                "7 έτ.",            // year (other)
                // "el" 1 wide
                "1 δευτερόλεπτο",
                "1 λεπτό",
                "1 ώρα",
                "1 ημέρα",
                "1 εβδομάδα",
                "1 μήνας",
                "1 έτος",
                // "el" 1 short
                "1 δευτ.",
                "1 λ.",
                "1 ώ.",
                "1 ημέρα",
                "1 εβδ.",
                "1 μήν.",
                "1 έτ.",	        // year (one)
                // "el" 7 wide
                "7 δευτερόλεπτα",
                "7 λεπτά",
                "7 ώρες",
                "7 ημέρες",
                "7 εβδομάδες",
                "7 μήνες",
                "7 έτη",
                // "el" 7 short
                "7 δευτ.",
                "7 λ.",
                "7 ώ.",		        // hour (other)
                "7 ημέρες",
                "7 εβδ.",
                "7 μήν.",
                "7 έτ."};           // year (other
        int counter = 0;
        String formatted;
        for ( int locIndex = 0; locIndex < locales.length; ++locIndex ) {
            for( int numIndex = 0; numIndex < numbers.length; ++numIndex ) {
                for ( int styleIndex = 0; styleIndex < styles.length; ++styleIndex ) {
                    for ( int unitIndex = 0; unitIndex < units.length; ++unitIndex ) {
                        Measure m = new Measure(numbers[numIndex], units[unitIndex]);
                        MeasureFormat fmt = MeasureFormat.getInstance(new ULocale(locales[locIndex]), styles[styleIndex]);
                        formatted = fmt.format(m);
                        assertEquals(
                                "locale: " + locales[locIndex]
                                        + ", style: " + styles[styleIndex]
                                                + ", units: " + units[unitIndex]
                                                        + ", value: " + numbers[numIndex],
                                                expected[counter], formatted);
                        ++counter;
                    }
                }
            }
        }
    }

    @Test
    public void testAUnit() {
        String lastType = null;
        for (MeasureUnit expected : MeasureUnit.getAvailable()) {
            String type = expected.getType();
            String code = expected.getSubtype();
            if (!type.equals(lastType)) {
                logln(type);
                lastType = type;
            }
            MeasureUnit actual = MeasureUnit.internalGetInstance(type, code);
            assertSame("Identity check", expected, actual);
        }

        // The return value should contain only unique elements
        assertUnique(MeasureUnit.getAvailable());
    }

    static void assertUnique(Collection<?> coll) {
        int expectedSize = new HashSet<>(coll).size();
        int actualSize = coll.size();
        assertEquals("Collection should contain only unique elements", expectedSize, actualSize);
    }

    @Test
    public void testFormatSingleArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 meters", mf.format(new Measure(5, MeasureUnit.METER)));
    }

    @Test
    public void testFormatMeasuresZeroArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "", mf.formatMeasures());
    }

    @Test
    public void testFormatMeasuresOneArg() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 meters", mf.formatMeasures(new Measure(5, MeasureUnit.METER)));
    }



    @Test
    public void testMultiples() {
        ULocale russia = new ULocale("ru");
        Object[][] data = new Object[][] {
                {ULocale.ENGLISH, FormatWidth.WIDE, "2 miles, 1 foot, 2.3 inches"},
                {ULocale.ENGLISH, FormatWidth.SHORT, "2 mi, 1 ft, 2.3 in"},
                {ULocale.ENGLISH, FormatWidth.NARROW, "2mi 1\u2032 2.3\u2033"},
                {russia, FormatWidth.WIDE,   "2 \u043C\u0438\u043B\u0438 1 \u0444\u0443\u0442 2,3 \u0434\u044E\u0439\u043C\u0430"},
                {russia, FormatWidth.SHORT,  "2 \u043C\u0438 1 \u0444\u0442 2,3 \u0434\u044E\u0439\u043C."},
                {russia, FormatWidth.NARROW, "2 \u043C\u0438 1 \u0444\u0442 2,3 \u0434\u044E\u0439\u043C."},
   };
        for (Object[] row : data) {
            MeasureFormat mf = MeasureFormat.getInstance(
                    (ULocale) row[0], (FormatWidth) row[1]);
            assertEquals(
                    "testMultiples",
                    row[2],
                    mf.formatMeasures(
                            new Measure(2, MeasureUnit.MILE),
                            new Measure(1, MeasureUnit.FOOT),
                            new Measure(2.3, MeasureUnit.INCH)));
        }
    }

    @Test
    public void testManyLocaleDurations() {
        Measure hours   = new Measure(5, MeasureUnit.HOUR);
        Measure minutes = new Measure(37, MeasureUnit.MINUTE);
        ULocale ulocDanish       = new ULocale("da");
        ULocale ulocSpanish      = new ULocale("es");
        ULocale ulocFinnish      = new ULocale("fi");
        ULocale ulocIcelandic    = new ULocale("is");
        ULocale ulocNorwegianBok = new ULocale("nb");
        ULocale ulocNorwegianNyn = new ULocale("nn");
        ULocale ulocDutch        = new ULocale("nl");
        ULocale ulocSwedish      = new ULocale("sv");
        Object[][] data = new Object[][] {
            { ulocDanish,       FormatWidth.NARROW,  "5 t og 37 m" },
            { ulocDanish,       FormatWidth.NUMERIC, "5.37" },
            { ULocale.GERMAN,   FormatWidth.NARROW,  "5 Std., 37 Min." },
            { ULocale.GERMAN,   FormatWidth.NUMERIC, "5:37" },
            { ULocale.ENGLISH,  FormatWidth.NARROW,  "5h 37m" },
            { ULocale.ENGLISH,  FormatWidth.NUMERIC, "5:37" },
            { ulocSpanish,      FormatWidth.NARROW,  "5h 37min" },
            { ulocSpanish,      FormatWidth.NUMERIC, "5:37" },
            { ulocFinnish,      FormatWidth.NARROW,  "5t 37min" },
            { ulocFinnish,      FormatWidth.NUMERIC, "5.37" },
            { ULocale.FRENCH,   FormatWidth.NARROW,  "5h 37min" },
            { ULocale.FRENCH,   FormatWidth.NUMERIC, "5:37" },
            { ulocIcelandic,    FormatWidth.NARROW,  "5 klst. og 37 m\u00EDn." },
            { ulocIcelandic,    FormatWidth.NUMERIC, "5:37" },
            { ULocale.JAPANESE, FormatWidth.NARROW,  "5h37m" },
            { ULocale.JAPANESE, FormatWidth.NUMERIC, "5:37" },
            { ulocNorwegianBok, FormatWidth.NARROW,  "5t, 37m" },
            { ulocNorwegianBok, FormatWidth.NUMERIC, "5:37" },
            { ulocDutch,        FormatWidth.NARROW,  "5 u, 37 m" },
            { ulocDutch,        FormatWidth.NUMERIC, "5:37" },
            { ulocNorwegianNyn, FormatWidth.NARROW,  "5t 37m" },
            { ulocNorwegianNyn, FormatWidth.NUMERIC, "5:37" },
            { ulocSwedish,      FormatWidth.NARROW,  "5h 37m" },
            { ulocSwedish,      FormatWidth.NUMERIC, "5:37" },
            { ULocale.CHINESE,  FormatWidth.NARROW,  "5\u5C0F\u65F637\u5206\u949F" },
            { ULocale.CHINESE,  FormatWidth.NUMERIC, "5:37" },
        };
        for (Object[] row : data) {
            MeasureFormat mf = null;
            try{
                mf = MeasureFormat.getInstance( (ULocale)row[0], (FormatWidth)row[1] );
            } catch(Exception e) {
                errln("Exception creating MeasureFormat for locale " + row[0] + ", width " +
                        row[1] + ": " + e);
                continue;
            }
            String result = mf.formatMeasures(hours, minutes);
            if (!result.equals(row[2])) {
                errln("MeasureFormat.formatMeasures for locale " + row[0] + ", width " +
                        row[1] + ", expected \"" + (String)row[2] + "\", got \"" + result + "\"" );
            }
        }
    }

    @Test
    public void testSimplePer() {
        Object DONT_CARE = null;
        Object[][] data = new Object[][] {
                // per unit pattern
                {FormatWidth.WIDE, 1.0, MeasureUnit.SECOND, "1 pound per second", DONT_CARE, 0, 0},
                {FormatWidth.WIDE, 2.0, MeasureUnit.SECOND, "2 pounds per second", DONT_CARE, 0, 0},
                // compound pattern
                {FormatWidth.WIDE, 1.0, MeasureUnit.MINUTE, "1 pound per minute", DONT_CARE, 0, 0},
                {FormatWidth.WIDE, 2.0, MeasureUnit.MINUTE, "2 pounds per minute", DONT_CARE, 0, 0},
                // per unit
                {FormatWidth.SHORT, 1.0, MeasureUnit.SECOND, "1 lb/s", DONT_CARE, 0, 0},
                {FormatWidth.SHORT, 2.0, MeasureUnit.SECOND, "2 lb/s", DONT_CARE, 0, 0},
                // compound
                {FormatWidth.SHORT, 1.0, MeasureUnit.MINUTE, "1 lb/min", DONT_CARE, 0, 0},
                {FormatWidth.SHORT, 2.0, MeasureUnit.MINUTE, "2 lb/min", DONT_CARE, 0, 0},
                // per unit
                {FormatWidth.NARROW, 1.0, MeasureUnit.SECOND, "1#/s", DONT_CARE, 0, 0},
                {FormatWidth.NARROW, 2.0, MeasureUnit.SECOND, "2#/s", DONT_CARE, 0, 0},
                // compound
                {FormatWidth.NARROW, 1.0, MeasureUnit.MINUTE, "1#/min", DONT_CARE, 0, 0},
                {FormatWidth.NARROW, 2.0, MeasureUnit.MINUTE, "2#/min", DONT_CARE, 0, 0},
                // field positions
                {FormatWidth.SHORT, 23.3, MeasureUnit.SECOND, "23.3 lb/s", NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3},
                {FormatWidth.SHORT, 23.3, MeasureUnit.SECOND, "23.3 lb/s", NumberFormat.Field.INTEGER, 0, 2},
                {FormatWidth.SHORT, 23.3, MeasureUnit.MINUTE, "23.3 lb/min", NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3},
                {FormatWidth.SHORT, 23.3, MeasureUnit.MINUTE, "23.3 lb/min", NumberFormat.Field.INTEGER, 0, 2},

        };

        for (Object[] row : data) {
            FormatWidth formatWidth = (FormatWidth) row[0];
            Number amount = (Number) row[1];
            MeasureUnit perUnit = (MeasureUnit) row[2];
            String expected = row[3].toString();
            NumberFormat.Field field = (NumberFormat.Field) row[4];
            int startOffset = ((Integer) row[5]).intValue();
            int endOffset = ((Integer) row[6]).intValue();
            MeasureFormat mf = MeasureFormat.getInstance(
                    ULocale.ENGLISH, formatWidth);
            FieldPosition pos = field != null ? new FieldPosition(field) : new FieldPosition(0);
            String prefix = "Prefix: ";
            assertEquals(
                    "",
                    prefix + expected,
                    mf.formatMeasurePerUnit(
                            new Measure(amount, MeasureUnit.POUND),
                            perUnit,
                            new StringBuilder(prefix),
                            pos).toString());
            if (field != DONT_CARE) {
                assertEquals("startOffset", startOffset, pos.getBeginIndex() - prefix.length());
                assertEquals("endOffset", endOffset, pos.getEndIndex() - prefix.length());
            }
        }
    }

    @Test
    public void testNumeratorPlurals() {
        ULocale polish = new ULocale("pl");
        Object[][] data = new Object[][] {
                {1, "1 stopa na sekundę"},
                {2, "2 stopy na sekundę"},
                {5, "5 stóp na sekundę"},
                {1.5, "1,5 stopy na sekundę"}};

        for (Object[] row : data) {
            MeasureFormat mf = MeasureFormat.getInstance(polish, FormatWidth.WIDE);
            assertEquals(
                    "",
                    row[1],
                    mf.formatMeasurePerUnit(
                            new Measure((Number) row[0], MeasureUnit.FOOT),
                            MeasureUnit.SECOND,
                            new StringBuilder(),
                            new FieldPosition(0)).toString());
        }
    }

    @Test
    public void testGram() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT);
        assertEquals(
                "testGram",
                "1 g",
                mf.format(new Measure(1, MeasureUnit.GRAM)));
        assertEquals(
                "testGram",
                "1 G",
                mf.format(new Measure(1, MeasureUnit.G_FORCE)));
    }

    @Test
    public void testCurrencies() {
        Measure USD_1 = new Measure(1.0, Currency.getInstance("USD"));
        Measure USD_2 = new Measure(2.0, Currency.getInstance("USD"));
        Measure USD_NEG_1 = new Measure(-1.0, Currency.getInstance("USD"));
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("Wide currency", "-1.00 US dollars", mf.format(USD_NEG_1));
        assertEquals("Wide currency", "1.00 US dollars", mf.format(USD_1));
        assertEquals("Wide currency", "2.00 US dollars", mf.format(USD_2));
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT);
        assertEquals("short currency", "-USD 1.00", mf.format(USD_NEG_1));
        assertEquals("short currency", "USD 1.00", mf.format(USD_1));
        assertEquals("short currency", "USD 2.00", mf.format(USD_2));
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NARROW);
        assertEquals("narrow currency", "-$1.00", mf.format(USD_NEG_1));
        assertEquals("narrow currency", "$1.00", mf.format(USD_1));
        assertEquals("narrow currency", "$2.00", mf.format(USD_2));
        mf = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.NUMERIC);
        assertEquals("numeric currency", "-$1.00", mf.format(USD_NEG_1));
        assertEquals("numeric currency", "$1.00", mf.format(USD_1));
        assertEquals("numeric currency", "$2.00", mf.format(USD_2));

        mf = MeasureFormat.getInstance(ULocale.JAPAN, FormatWidth.WIDE);
        // Locale jp does NOT put a space between the number and the currency long name:
        // https://unicode.org/cldr/trac/browser/tags/release-32-0-1/common/main/ja.xml?rev=13805#L7046
        assertEquals("Wide currency", "-1.00\u7C73\u30C9\u30EB", mf.format(USD_NEG_1));
        assertEquals("Wide currency", "1.00\u7C73\u30C9\u30EB", mf.format(USD_1));
        assertEquals("Wide currency", "2.00\u7C73\u30C9\u30EB", mf.format(USD_2));

        Measure CAD_1 = new Measure(1.0, Currency.getInstance("CAD"));
        mf = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.SHORT);
        assertEquals("short currency", "CAD 1.00", mf.format(CAD_1));
    }

    @Test
    public void testDisplayNames() {
        Object[][] data = new Object[][] {
            // Unit, locale, width, expected result
            { MeasureUnit.YEAR, "en", FormatWidth.WIDE, "years" },
            { MeasureUnit.YEAR, "ja", FormatWidth.WIDE, "年" },
            { MeasureUnit.YEAR, "es", FormatWidth.WIDE, "años" },
            { MeasureUnit.YEAR, "pt", FormatWidth.WIDE, "anos" },
            { MeasureUnit.YEAR, "pt-PT", FormatWidth.WIDE, "anos" },
            { MeasureUnit.AMPERE, "en", FormatWidth.WIDE, "amperes" },
            { MeasureUnit.AMPERE, "ja", FormatWidth.WIDE, "アンペア" },
            { MeasureUnit.AMPERE, "es", FormatWidth.WIDE, "amperios" },
            { MeasureUnit.AMPERE, "pt", FormatWidth.WIDE, "amperes" },
            { MeasureUnit.AMPERE, "pt-PT", FormatWidth.WIDE, "amperes" },
            { MeasureUnit.METER_PER_SECOND_SQUARED, "pt", FormatWidth.WIDE, "metros por segundo ao quadrado" },
            { MeasureUnit.METER_PER_SECOND_SQUARED, "pt-PT", FormatWidth.WIDE, "metros por segundo quadrado" },
            { MeasureUnit.SQUARE_KILOMETER, "pt", FormatWidth.NARROW, "km²" },
            { MeasureUnit.SQUARE_KILOMETER, "pt", FormatWidth.SHORT, "km²" },
            { MeasureUnit.SQUARE_KILOMETER, "pt", FormatWidth.WIDE, "quilômetros quadrados" },
            { MeasureUnit.SECOND, "pt-PT", FormatWidth.NARROW, "s" },
            { MeasureUnit.SECOND, "pt-PT", FormatWidth.SHORT, "s" },
            { MeasureUnit.SECOND, "pt-PT", FormatWidth.WIDE, "segundos" },
            { MeasureUnit.SECOND, "pt", FormatWidth.NARROW, "s" },
            { MeasureUnit.SECOND, "pt", FormatWidth.SHORT, "s" },
            { MeasureUnit.SECOND, "pt", FormatWidth.WIDE, "segundos" },
        };

        for (Object[] test : data) {
            MeasureUnit unit = (MeasureUnit) test[0];
            ULocale locale = ULocale.forLanguageTag((String) test[1]);
            FormatWidth formatWidth = (FormatWidth) test[2];
            String expected = (String) test[3];

            MeasureFormat mf = MeasureFormat.getInstance(locale, formatWidth);
            String actual = mf.getUnitDisplayName(unit);
            assertEquals(String.format("Unit Display Name for %s, %s, %s", unit, locale, formatWidth),
                    expected, actual);
        }
    }

    @Test
    public void testFieldPosition() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.SHORT);
        FieldPosition pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        fmt.format(new Measure(43.5, MeasureUnit.FOOT), new StringBuffer("123456: "), pos);
        assertEquals("beginIndex", 10, pos.getBeginIndex());
        assertEquals("endIndex", 11, pos.getEndIndex());

        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        fmt.format(new Measure(43, MeasureUnit.FOOT), new StringBuffer(), pos);
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 0, pos.getEndIndex());
    }

    @Test
    public void testFieldPositionMultiple() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.SHORT);
        FieldPosition pos = new FieldPosition(NumberFormat.Field.INTEGER);
        String result = fmt.formatMeasures(
                new StringBuilder(),
                pos,
                new Measure(354, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER)).toString();
        assertEquals("result", "354 m, 23 cm", result);

        // According to javadocs for {@link Format#format} FieldPosition is set to
        // beginning and end of first such field encountered instead of the last
        // such field encountered.
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 3, pos.getEndIndex());

        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        result = fmt.formatMeasures(
                new StringBuilder("123456: "),
                pos,
                new Measure(354, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5.4, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "123456: 354 m, 23 cm, 5.4 mm", result);
        assertEquals("beginIndex", 23, pos.getBeginIndex());
        assertEquals("endIndex", 24, pos.getEndIndex());

        result = fmt.formatMeasures(
                new StringBuilder(),
                pos,
                new Measure(3, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5.4, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "3 m, 23 cm, 5.4 mm", result);
        assertEquals("beginIndex", 13, pos.getBeginIndex());
        assertEquals("endIndex", 14, pos.getEndIndex());

        pos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        result = fmt.formatMeasures(
                new StringBuilder("123456: "),
                pos,
                new Measure(3, MeasureUnit.METER),
                new Measure(23, MeasureUnit.CENTIMETER),
                new Measure(5, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "123456: 3 m, 23 cm, 5 mm", result);
        assertEquals("beginIndex", 0, pos.getBeginIndex());
        assertEquals("endIndex", 0, pos.getEndIndex());

        pos = new FieldPosition(NumberFormat.Field.INTEGER);
        result = fmt.formatMeasures(
                new StringBuilder("123456: "),
                pos,
                new Measure(57, MeasureUnit.MILLIMETER)).toString();
        assertEquals("result", "123456: 57 mm", result);
        assertEquals("beginIndex", 8, pos.getBeginIndex());
        assertEquals("endIndex", 10, pos.getEndIndex());

    }

    @Test
    public void testOldFormatWithList() {
        List<Measure> measures = new ArrayList<>(2);
        measures.add(new Measure(5, MeasureUnit.ACRE));
        measures.add(new Measure(3000, MeasureUnit.SQUARE_FOOT));
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 acres, 3,000 square feet", fmt.format(measures));
        assertEquals("", "5 acres", fmt.format(measures.subList(0, 1)));
        List<String> badList = new ArrayList<>();
        badList.add("be");
        badList.add("you");
        try {
            fmt.format(badList);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
           // Expected
        }
    }

    @Test
    public void testOldFormatWithArray() {
        Measure[] measures = new Measure[] {
                new Measure(5, MeasureUnit.ACRE),
                new Measure(3000, MeasureUnit.SQUARE_FOOT),
        };
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        assertEquals("", "5 acres, 3,000 square feet", fmt.format(measures));
    }

    @Test
    public void testOldFormatBadArg() {
        MeasureFormat fmt = MeasureFormat.getInstance(
                ULocale.ENGLISH, FormatWidth.WIDE);
        try {
            fmt.format("be");
            fail("Expected IllegalArgumentExceptino.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testUnitPerUnitResolution() {
        // Ticket 11274
        MeasureFormat fmt = MeasureFormat.getInstance(Locale.ENGLISH, FormatWidth.SHORT);

        // This fails unless we resolve to MeasureUnit.POUND_PER_SQUARE_INCH
        assertEquals("", "50 psi",
                fmt.formatMeasurePerUnit(
                        new Measure(50, MeasureUnit.POUND_FORCE),
                        MeasureUnit.SQUARE_INCH,
                        new StringBuilder(),
                        new FieldPosition(0)).toString());
    }

    @Test
    public void testEqHashCode() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.SHORT);
        MeasureFormat mfeq = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.SHORT);
        MeasureFormat mfne = MeasureFormat.getInstance(ULocale.CANADA, FormatWidth.WIDE);
        MeasureFormat mfne2 = MeasureFormat.getInstance(ULocale.ENGLISH, FormatWidth.SHORT);
        verifyEqualsHashCode(mf, mfeq, mfne);
        verifyEqualsHashCode(mf, mfeq, mfne2);
    }

    @Test
    public void testEqHashCodeOfMeasure() {
        Measure _3feetDouble = new Measure(3.0, MeasureUnit.FOOT);
        Measure _3feetInt = new Measure(3, MeasureUnit.FOOT);
        Measure _4feetInt = new Measure(4, MeasureUnit.FOOT);
        verifyEqualsHashCode(_3feetDouble, _3feetInt, _4feetInt);
    }

    @Test
    public void testGetLocale() {
        MeasureFormat mf = MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.SHORT);
        assertEquals("", ULocale.GERMAN, mf.getLocale(ULocale.VALID_LOCALE));
    }

    @Test
    public void TestSerial() {
        checkStreamingEquality(MeasureUnit.CELSIUS);
        checkStreamingEquality(MeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.NARROW));
        checkStreamingEquality(Currency.getInstance("EUR"));
        checkStreamingEquality(MeasureFormat.getInstance(ULocale.GERMAN, FormatWidth.SHORT));
        checkStreamingEquality(MeasureFormat.getCurrencyFormat(ULocale.ITALIAN));
    }

    @Test
    public void TestSerialFormatWidthEnum() {
        // FormatWidth enum values must map to the same ordinal values for all time in order for
        // serialization to work.
        assertEquals("FormatWidth.WIDE", 0, FormatWidth.WIDE.ordinal());
        assertEquals("FormatWidth.SHORT", 1, FormatWidth.SHORT.ordinal());
        assertEquals("FormatWidth.NARROW", 2, FormatWidth.NARROW.ordinal());
        assertEquals("FormatWidth.NUMERIC", 3, FormatWidth.NUMERIC.ordinal());
    }

    @Test
    public void testCurrencyFormatStandInForMeasureFormat() {
        MeasureFormat mf = MeasureFormat.getCurrencyFormat(ULocale.ENGLISH);
        assertEquals(
                "70 feet, 5.3 inches",
                "70 feet, 5.3 inches",
                mf.formatMeasures(
                        new Measure(70, MeasureUnit.FOOT),
                        new Measure(5.3, MeasureUnit.INCH)));
        assertEquals("getLocale", ULocale.ENGLISH, mf.getLocale());
        assertEquals("getNumberFormat", ULocale.ENGLISH, mf.getNumberFormat().getLocale(ULocale.VALID_LOCALE));
        assertEquals("getWidth", MeasureFormat.FormatWidth.WIDE, mf.getWidth());
    }

    @Test
    public void testCurrencyFormatLocale() {
        MeasureFormat mfu = MeasureFormat.getCurrencyFormat(ULocale.FRANCE);
        MeasureFormat mfj = MeasureFormat.getCurrencyFormat(Locale.FRANCE);

        assertEquals("getCurrencyFormat ULocale/Locale", mfu, mfj);
    }

    @Test
    public void testCurrencyFormatParseIsoCode() throws ParseException {
        MeasureFormat mf = MeasureFormat.getCurrencyFormat(ULocale.ENGLISH);
        CurrencyAmount result = (CurrencyAmount) mf.parseObject("GTQ 34.56");
        assertEquals("Parse should succeed", result.getNumber().doubleValue(), 34.56, 0.0);
        assertEquals("Should parse ISO code GTQ even though the currency is USD",
                "GTQ", result.getCurrency().getCurrencyCode());
    }

    @Test
    public void testDoubleZero() {
        ULocale en = new ULocale("en");
        NumberFormat nf = NumberFormat.getInstance(en);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        MeasureFormat mf = MeasureFormat.getInstance(en, FormatWidth.WIDE, nf);
        assertEquals(
                "Positive Rounding",
                "4 hours, 23 minutes, 16.00 seconds",
                mf.formatMeasures(
                        new Measure(4.7, MeasureUnit.HOUR),
                        new Measure(23, MeasureUnit.MINUTE),
                        new Measure(16, MeasureUnit.SECOND)));
        assertEquals(
                "Negative Rounding",
                "-4 hours, 23 minutes, 16.00 seconds",
                mf.formatMeasures(
                        new Measure(-4.7, MeasureUnit.HOUR),
                        new Measure(23, MeasureUnit.MINUTE),
                        new Measure(16, MeasureUnit.SECOND)));

    }

    @Test
    public void testIndividualPluralFallback() {
        // See ticket #11986 "incomplete fallback in MeasureFormat".
        // In CLDR 28, fr_CA temperature-generic/short has only the "one" form,
        // and falls back to fr for the "other" form.
        MeasureFormat mf = MeasureFormat.getInstance(new ULocale("fr_CA"), FormatWidth.SHORT);
        Measure twoDeg = new Measure(2, MeasureUnit.GENERIC_TEMPERATURE);
        assertEquals("2 deg temp in fr_CA", "2°", mf.format(twoDeg));
    }

    @Test
    public void testPopulateCache() {
        // Quick check that the lazily added additions to the MeasureUnit cache are present.
        assertTrue("MeasureUnit: unexpectedly few currencies defined", MeasureUnit.getAvailable("currency").size() > 50);
    }

    @Test
    public void testParseObject() {
        MeasureFormat mf = MeasureFormat.getInstance(Locale.GERMAN, FormatWidth.NARROW);
        try {
            mf.parseObject("3m", null);
            fail("MeasureFormat.parseObject(String, ParsePosition) " +
                    "should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testCLDRUnitAvailability() {
        Set<MeasureUnit> knownUnits = new HashSet<>();
        Class<?> cMeasureUnit, cTimeUnit;
        try {
            cMeasureUnit = Class.forName("com.ibm.icu.util.MeasureUnit");
            cTimeUnit = Class.forName("com.ibm.icu.util.TimeUnit");
        } catch (ClassNotFoundException e) {
            fail("Count not load MeasureUnit or TimeUnit class: " + e.getMessage());
            return;
        }
        for (Field field : cMeasureUnit.getFields()) {
            if (field.getGenericType() == cMeasureUnit || field.getGenericType() == cTimeUnit) {
                try {
                    MeasureUnit unit = (MeasureUnit) field.get(cMeasureUnit);
                    knownUnits.add(unit);
                } catch (IllegalArgumentException e) {
                    fail(e.getMessage());
                    return;
                } catch (IllegalAccessException e) {
                    fail(e.getMessage());
                    return;
                }
            }
        }
        for (String type : MeasureUnit.getAvailableTypes()) {
            if (type.equals("currency")
                    || type.equals("compound")
                    || type.equals("coordinate")
                    || type.equals("none")) {
                continue;
            }
            for (MeasureUnit unit : MeasureUnit.getAvailable(type)) {
                if (!knownUnits.contains(unit)) {
                    fail("Unit present in CLDR but not available via constant in MeasureUnit: " + unit);
                }
            }
        }
    }

    @Test
    public void testBug11966() {
        Locale locale = new Locale("en", "AU");
        MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE);
        // Should not throw an exception.
    }

    @Test
    public void test20332_PersonUnits() {
        Object[][] cases = new Object[][] {
            {ULocale.US, MeasureUnit.YEAR_PERSON, MeasureFormat.FormatWidth.NARROW, "25y"},
            {ULocale.US, MeasureUnit.YEAR_PERSON, MeasureFormat.FormatWidth.SHORT, "25 yrs"},
            {ULocale.US, MeasureUnit.YEAR_PERSON, MeasureFormat.FormatWidth.WIDE, "25 years"},
            {ULocale.US, MeasureUnit.MONTH_PERSON, MeasureFormat.FormatWidth.NARROW, "25m"},
            {ULocale.US, MeasureUnit.MONTH_PERSON, MeasureFormat.FormatWidth.SHORT, "25 mths"},
            {ULocale.US, MeasureUnit.MONTH_PERSON, MeasureFormat.FormatWidth.WIDE, "25 months"},
            {ULocale.US, MeasureUnit.WEEK_PERSON, MeasureFormat.FormatWidth.NARROW, "25w"},
            {ULocale.US, MeasureUnit.WEEK_PERSON, MeasureFormat.FormatWidth.SHORT, "25 wks"},
            {ULocale.US, MeasureUnit.WEEK_PERSON, MeasureFormat.FormatWidth.WIDE, "25 weeks"},
            {ULocale.US, MeasureUnit.DAY_PERSON, MeasureFormat.FormatWidth.NARROW, "25d"},
            {ULocale.US, MeasureUnit.DAY_PERSON, MeasureFormat.FormatWidth.SHORT, "25 days"},
            {ULocale.US, MeasureUnit.DAY_PERSON, MeasureFormat.FormatWidth.WIDE, "25 days"}
        };
        for (Object[] cas : cases) {
            ULocale locale = (ULocale) cas[0];
            MeasureUnit unit = (MeasureUnit) cas[1];
            MeasureFormat.FormatWidth width = (MeasureFormat.FormatWidth) cas[2];
            String expected = (String) cas[3];

            MeasureFormat fmt = MeasureFormat.getInstance(locale, width);
            String result = fmt.formatMeasures(new Measure(25, unit));
            assertEquals("" + locale + " " + unit + " " + width, expected, result);
        }
    }

    static TreeMap<String, List<MeasureUnit>> getAllUnits() {
        TreeMap<String, List<MeasureUnit>> allUnits = new TreeMap<>();
        for (String type : MeasureUnit.getAvailableTypes()) {
            ArrayList<MeasureUnit> units = new ArrayList<>(MeasureUnit.getAvailable(type));
            Collections.sort(
                    units,
                    new Comparator<MeasureUnit>() {

                        @Override
                        public int compare(MeasureUnit o1, MeasureUnit o2) {
                            return o1.getSubtype().compareTo(o2.getSubtype());
                        }

                    });
            allUnits.put(type, units);
        }
        return allUnits;
    }

    public <T extends Serializable> void checkStreamingEquality(T item) {
        try {
          ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
          ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOut);
          objectOutputStream.writeObject(item);
          objectOutputStream.close();
          byte[] contents = byteOut.toByteArray();
          logln("bytes: " + contents.length + "; " + item.getClass() + ": " + showBytes(contents));
          ByteArrayInputStream byteIn = new ByteArrayInputStream(contents);
          ObjectInputStream objectInputStream = new ObjectInputStream(byteIn);
          Object obj = objectInputStream.readObject();
          assertEquals("Streamed Object equals ", item, obj);
        } catch (IOException e) {
          e.printStackTrace();
          assertNull("Test Serialization " + item.getClass(), e);
        } catch (ClassNotFoundException e) {
          assertNull("Test Serialization " + item.getClass(), e);
        }
      }

    /**
     * @param contents
     * @return
     */
    private String showBytes(byte[] contents) {
      StringBuilder b = new StringBuilder("[");
      for (int i = 0; i < contents.length; ++i) {
        int item = contents[i] & 0xFF;
        if (item >= 0x20 && item <= 0x7F) {
          b.append((char) item);
        } else {
          b.append('(').append(Utility.hex(item, 2)).append(')');
        }
      }
      return b.append(']').toString();
    }

    private void verifyEqualsHashCode(Object o, Object eq, Object ne) {
        assertEquals("verifyEqualsHashCodeSame", o, o);
        assertEquals("verifyEqualsHashCodeEq", o, eq);
        assertNotEquals("verifyEqualsHashCodeNe", o, ne);
        assertNotEquals("verifyEqualsHashCodeEqTrans", eq, ne);
        assertEquals("verifyEqualsHashCodeHashEq", o.hashCode(), eq.hashCode());

        // May be a flaky test, but generally should be true.
        // May need to comment this out later.
        assertNotEquals("verifyEqualsHashCodeHashNe", o.hashCode(), ne.hashCode());
    }

    public static class MeasureUnitHandler implements SerializableTestUtility.Handler
    {
        @Override
        public Object[] getTestObjects()
        {
            MeasureUnit items[] = {
                    MeasureUnit.CELSIUS,
                    Currency.getInstance("EUR")
            };
            return items;
        }
        @Override
        public boolean hasSameBehavior(Object a, Object b)
        {
            MeasureUnit a1 = (MeasureUnit) a;
            MeasureUnit b1 = (MeasureUnit) b;
            return a1.getType().equals(b1.getType())
                    && a1.getSubtype().equals(b1.getSubtype());
        }
    }

    public static class MeasureFormatHandler  implements SerializableTestUtility.Handler
    {
        FormatHandler.NumberFormatHandler nfh = new FormatHandler.NumberFormatHandler();

        @Override
        public Object[] getTestObjects()
        {
            MeasureFormat items[] = {
                    MeasureFormat.getInstance(ULocale.FRANCE, FormatWidth.SHORT),
                    MeasureFormat.getInstance(
                            ULocale.FRANCE,
                            FormatWidth.WIDE,
                            NumberFormat.getIntegerInstance(ULocale.CANADA_FRENCH)),
            };
            return items;
        }
        @Override
        public boolean hasSameBehavior(Object a, Object b)
        {
            MeasureFormat a1 = (MeasureFormat) a;
            MeasureFormat b1 = (MeasureFormat) b;
            boolean getLocaleEqual = a1.getLocale().equals(b1.getLocale());
            boolean getWidthEqual = a1.getWidth().equals(b1.getWidth());
            boolean numFmtHasSameBehavior = nfh.hasSameBehavior(a1.getNumberFormat(), b1.getNumberFormat());
            if (getLocaleEqual && getWidthEqual && numFmtHasSameBehavior) {
                return true;
            }
            System.out.println("MeasureFormatHandler.hasSameBehavior fails:");
            if (!getLocaleEqual) {
                System.out.println("- getLocale equality fails: old a1: " + a1.getLocale().getName() + "; test b1: "
                        + b1.getLocale().getName());
            }
            if (!getWidthEqual) {
                System.out.println("- getWidth equality fails: old a1: " + a1.getWidth().name() + "; test b1: "
                        + b1.getWidth().name());
            }
            if (!numFmtHasSameBehavior) {
                System.out.println("- getNumberFormat hasSameBehavior fails");
            }
            return false;
        }
    }

    @Test
    public void TestNumericTimeNonLatin() {
        ULocale ulocale = ULocale.forLanguageTag("bn");
        MeasureFormat fmt = MeasureFormat.getInstance(ulocale, FormatWidth.NUMERIC);
        String actual = fmt.formatMeasures(new Measure(12, MeasureUnit.MINUTE), new Measure(39.12345, MeasureUnit.SECOND));
        assertEquals("Incorrect digits", "১২:৩৯.১২৩", actual);
    }

    @Test
    public void TestNumericTime() {
        MeasureFormat fmt = MeasureFormat.getInstance(ULocale.forLanguageTag("en"), FormatWidth.NUMERIC);

        Measure hours = new Measure(112, MeasureUnit.HOUR);
        Measure minutes = new Measure(113, MeasureUnit.MINUTE);
        Measure seconds = new Measure(114, MeasureUnit.SECOND);
        Measure fhours = new Measure(112.8765, MeasureUnit.HOUR);
        Measure fminutes = new Measure(113.8765, MeasureUnit.MINUTE);
        Measure fseconds = new Measure(114.8765, MeasureUnit.SECOND);

        Assert.assertEquals("112h", fmt.formatMeasures(hours));
        Assert.assertEquals("113m", fmt.formatMeasures(minutes));
        Assert.assertEquals("114s", fmt.formatMeasures(seconds));

        Assert.assertEquals("112.876h", fmt.formatMeasures(fhours));
        Assert.assertEquals("113.876m", fmt.formatMeasures(fminutes));
        Assert.assertEquals("114.876s", fmt.formatMeasures(fseconds));

        Assert.assertEquals("112:113", fmt.formatMeasures(hours, minutes));
        Assert.assertEquals("112:00:114", fmt.formatMeasures(hours, seconds));
        Assert.assertEquals("113:114", fmt.formatMeasures(minutes, seconds));

        Assert.assertEquals("112:113.876", fmt.formatMeasures(hours, fminutes));
        Assert.assertEquals("112:00:114.876", fmt.formatMeasures(hours, fseconds));
        Assert.assertEquals("113:114.876", fmt.formatMeasures(minutes, fseconds));

        Assert.assertEquals("112:113", fmt.formatMeasures(fhours, minutes));
        Assert.assertEquals("112:00:114", fmt.formatMeasures(fhours, seconds));
        Assert.assertEquals("113:114", fmt.formatMeasures(fminutes, seconds));

        Assert.assertEquals("112:113.876", fmt.formatMeasures(fhours, fminutes));
        Assert.assertEquals("112:00:114.876", fmt.formatMeasures(fhours, fseconds));
        Assert.assertEquals("113:114.876", fmt.formatMeasures(fminutes, fseconds));

        Assert.assertEquals("112:113:114", fmt.formatMeasures(hours, minutes, seconds));
        Assert.assertEquals("112:113:114.876", fmt.formatMeasures(fhours, fminutes, fseconds));
    }

    @Test
    public void TestNumericTimeSomeSpecialFormats() {
        Measure fhours = new Measure(2.8765432, MeasureUnit.HOUR);
        Measure fminutes = new Measure(3.8765432, MeasureUnit.MINUTE);

        // Latvian is one of the very few locales 0-padding the hour
        MeasureFormat fmt = MeasureFormat.getInstance(ULocale.forLanguageTag("lt"), FormatWidth.NUMERIC);
        Assert.assertEquals("02:03,877", fmt.formatMeasures(fhours, fminutes));

        // Danish is one of the very few locales using '.' as separator
        fmt = MeasureFormat.getInstance(ULocale.forLanguageTag("da"), FormatWidth.NUMERIC);
        Assert.assertEquals("2.03,877", fmt.formatMeasures(fhours, fminutes));
    }

    @Test
    public void TestIdentifiers() {
        class TestCase {
            final String id;
            final String normalized;

            TestCase(String id, String normalized) {
                this.id = id;
                this.normalized = normalized;
            }
        }

        TestCase cases[] = {
            // Correctly normalized identifiers should not change
            new TestCase("square-meter-per-square-meter", "square-meter-per-square-meter"),
            new TestCase("kilogram-meter-per-square-meter-square-second",
                         "kilogram-meter-per-square-meter-square-second"),
            new TestCase("square-mile-and-square-foot", "square-mile-and-square-foot"),
            new TestCase("square-foot-and-square-mile", "square-foot-and-square-mile"),
            new TestCase("per-cubic-centimeter", "per-cubic-centimeter"),
            new TestCase("per-kilometer", "per-kilometer"),

            // Normalization of power and per
            new TestCase("pow2-foot-and-pow2-mile", "square-foot-and-square-mile"),
            new TestCase("gram-square-gram-per-dekagram", "cubic-gram-per-dekagram"),
            new TestCase("kilogram-per-meter-per-second", "kilogram-per-meter-second"),
            new TestCase("kilometer-per-second-per-megaparsec", "kilometer-per-megaparsec-second"),

            // Correct order of units, as per unitQuantities in CLDR's units.xml
            new TestCase("newton-meter", "newton-meter"),
            new TestCase("meter-newton", "newton-meter"),
            new TestCase("pound-force-foot", "pound-force-foot"),
            new TestCase("foot-pound-force", "pound-force-foot"),
            new TestCase("kilowatt-hour", "kilowatt-hour"),
            new TestCase("hour-kilowatt", "kilowatt-hour"),

            // Testing prefixes are parsed and produced correctly (ensures no
            // collisions in the enum values)
            new TestCase("yoctofoot", "yoctofoot"),
            new TestCase("zeptofoot", "zeptofoot"),
            new TestCase("attofoot", "attofoot"),
            new TestCase("femtofoot", "femtofoot"),
            new TestCase("picofoot", "picofoot"),
            new TestCase("nanofoot", "nanofoot"),
            new TestCase("microfoot", "microfoot"),
            new TestCase("millifoot", "millifoot"),
            new TestCase("centifoot", "centifoot"),
            new TestCase("decifoot", "decifoot"),
            new TestCase("foot", "foot"),
            new TestCase("dekafoot", "dekafoot"),
            new TestCase("hectofoot", "hectofoot"),
            new TestCase("kilofoot", "kilofoot"),
            new TestCase("megafoot", "megafoot"),
            new TestCase("gigafoot", "gigafoot"),
            new TestCase("terafoot", "terafoot"),
            new TestCase("petafoot", "petafoot"),
            new TestCase("exafoot", "exafoot"),
            new TestCase("zettafoot", "zettafoot"),
            new TestCase("yottafoot", "yottafoot"),
            new TestCase("kibibyte", "kibibyte"),
            new TestCase("mebibyte", "mebibyte"),
            new TestCase("gibibyte", "gibibyte"),
            new TestCase("tebibyte", "tebibyte"),
            new TestCase("pebibyte", "pebibyte"),
            new TestCase("exbibyte", "exbibyte"),
            new TestCase("zebibyte", "zebibyte"),
            new TestCase("yobibyte", "yobibyte"),

            // Testing aliases
            new TestCase("foodcalorie", "foodcalorie"),
            new TestCase("dot-per-centimeter", "dot-per-centimeter"),
            new TestCase("dot-per-inch", "dot-per-inch"),
            new TestCase("dot", "dot"),

            // Testing sort order of prefixes.
            new TestCase("megafoot-mebifoot-kibifoot-kilofoot", "mebifoot-megafoot-kibifoot-kilofoot"),
            new TestCase("per-megafoot-mebifoot-kibifoot-kilofoot", "per-mebifoot-megafoot-kibifoot-kilofoot"),
            new TestCase("megafoot-mebifoot-kibifoot-kilofoot-per-megafoot-mebifoot-kibifoot-kilofoot", "mebifoot-megafoot-kibifoot-kilofoot-per-mebifoot-megafoot-kibifoot-kilofoot"),
            new TestCase("microfoot-millifoot-megafoot-mebifoot-kibifoot-kilofoot", "mebifoot-megafoot-kibifoot-kilofoot-millifoot-microfoot"),
            new TestCase("per-microfoot-millifoot-megafoot-mebifoot-kibifoot-kilofoot", "per-mebifoot-megafoot-kibifoot-kilofoot-millifoot-microfoot"),
        };

        for (TestCase testCase : cases) {
            MeasureUnit unit = MeasureUnit.forIdentifier(testCase.id);

            final String actual = unit.getIdentifier();
            assertEquals(testCase.id, testCase.normalized, actual);
        }

        assertEquals("for empty identifiers, the MeasureUnit will be null",
                null, MeasureUnit.forIdentifier(""));
    }

    @Test
    public void TestAcceptableConstantDenominator() {
        class ConstantDenominatorTestCase {
            String identifier;
            long expectedConstantDenominator;

            ConstantDenominatorTestCase(String identifier, long expectedConstantDenominator) {
                this.identifier = identifier;
                this.expectedConstantDenominator = expectedConstantDenominator;
            }
        }

        List<ConstantDenominatorTestCase> testCases = Arrays.asList(
                new ConstantDenominatorTestCase("meter-per-1000", 1000),
                new ConstantDenominatorTestCase("liter-per-1000-kiloliter", 1000),
                new ConstantDenominatorTestCase("meter-per-100-kilometer", 100),
                new ConstantDenominatorTestCase("liter-per-kilometer", 0),
                new ConstantDenominatorTestCase("second-per-1000-minute", 1000),
                new ConstantDenominatorTestCase("gram-per-1000-kilogram", 1000),
                new ConstantDenominatorTestCase("meter-per-100", 100), // Failing ICU-23045
                new ConstantDenominatorTestCase("portion-per-1", 1),
                new ConstantDenominatorTestCase("portion-per-2", 2),
                new ConstantDenominatorTestCase("portion-per-3", 3),
                new ConstantDenominatorTestCase("portion-per-4", 4),
                new ConstantDenominatorTestCase("portion-per-5", 5),
                new ConstantDenominatorTestCase("portion-per-6", 6),
                new ConstantDenominatorTestCase("portion-per-7", 7),
                new ConstantDenominatorTestCase("portion-per-8", 8),
                new ConstantDenominatorTestCase("portion-per-9", 9),

                // Test for constant denominators that are powers of 10
                new ConstantDenominatorTestCase("portion-per-10", 10),
                new ConstantDenominatorTestCase("portion-per-100", 100),
                new ConstantDenominatorTestCase("portion-per-1000", 1000),
                new ConstantDenominatorTestCase("portion-per-10000", 10000),
                new ConstantDenominatorTestCase("portion-per-100000", 100000),
                new ConstantDenominatorTestCase("portion-per-1000000", 1000000),
                new ConstantDenominatorTestCase("portion-per-10000000", 10000000),
                new ConstantDenominatorTestCase("portion-per-100000000", 100000000),
                new ConstantDenominatorTestCase("portion-per-1000000000", 1000000000), // Failing ICU-23045
                new ConstantDenominatorTestCase("portion-per-10000000000", 10000000000L),
                new ConstantDenominatorTestCase("portion-per-100000000000", 100000000000L),
                new ConstantDenominatorTestCase("portion-per-1000000000000", 1000000000000L),
                new ConstantDenominatorTestCase("portion-per-10000000000000", 10000000000000L),
                new ConstantDenominatorTestCase("portion-per-100000000000000", 100000000000000L),
                new ConstantDenominatorTestCase("portion-per-1000000000000000", 1000000000000000L),
                new ConstantDenominatorTestCase("portion-per-10000000000000000", 10000000000000000L),
                new ConstantDenominatorTestCase("portion-per-100000000000000000", 100000000000000000L),
                new ConstantDenominatorTestCase("portion-per-1000000000000000000", 1000000000000000000L),
                new ConstantDenominatorTestCase("portion-per-1e3-kilometer", 1000),
        
                // Test for constant denominators that are represented as scientific notation numbers.
                new ConstantDenominatorTestCase("portion-per-1e1", 10),
                new ConstantDenominatorTestCase("portion-per-1E1", 10),
                new ConstantDenominatorTestCase("portion-per-1e2", 100),
                new ConstantDenominatorTestCase("portion-per-1E2", 100),
                new ConstantDenominatorTestCase("portion-per-1e3", 1000),
                new ConstantDenominatorTestCase("portion-per-1E3", 1000),
                new ConstantDenominatorTestCase("portion-per-1e4", 10000),
                new ConstantDenominatorTestCase("portion-per-1E4", 10000),
                new ConstantDenominatorTestCase("portion-per-1e5", 100000),
                new ConstantDenominatorTestCase("portion-per-1E5", 100000),
                new ConstantDenominatorTestCase("portion-per-1e6", 1000000),
                new ConstantDenominatorTestCase("portion-per-1E6", 1000000),
                new ConstantDenominatorTestCase("portion-per-1e9", 1000000000), // Failing ICU-23045
                new ConstantDenominatorTestCase("portion-per-1E9", 1000000000), // Failing ICU-23045
                new ConstantDenominatorTestCase("portion-per-1e10", 10000000000L),
                new ConstantDenominatorTestCase("portion-per-1E10", 10000000000L),
                new ConstantDenominatorTestCase("portion-per-1e18", 1000000000000000000L),
                new ConstantDenominatorTestCase("portion-per-1E18", 1000000000000000000L),
        
                // Test for constant denominators that are randomly selected.
                new ConstantDenominatorTestCase("liter-per-12345-kilometer", 12345),
                new ConstantDenominatorTestCase("per-1000-kilometer", 1000),
                new ConstantDenominatorTestCase("liter-per-1000-kiloliter", 1000),

                // Test for constant denominators that give 0.
                new ConstantDenominatorTestCase("meter", 0),
                new ConstantDenominatorTestCase("meter-per-second", 0),
                new ConstantDenominatorTestCase("meter-per-square-second", 0));

        for (ConstantDenominatorTestCase testCase : testCases) {
            switch (testCase.identifier) {
                case "portion-per-1000000000":
                case "portion-per-1e9":
                case "portion-per-1E9":
                case "meter-per-100-kilometer":
                    logKnownIssue("ICU-23045", "Incorrect constant denominator for certain unit identifiers");
                    continue;
            }

            MeasureUnit unit = MeasureUnit.forIdentifier(testCase.identifier);
            assertEquals("Constant denominator for " + testCase.identifier, testCase.expectedConstantDenominator,
                    unit.getConstantDenominator());
            assertTrue("Complexity for " + testCase.identifier,
                    unit.getComplexity() == Complexity.COMPOUND || unit.getComplexity() == Complexity.SINGLE);
        }
    }

    @Test
    public void TestInvalidIdentifiers() {
        final String inputs[] = {
                "kilo",
                "kilokilo",
                "onekilo",
                "meterkilo",
                "meter-kilo",
                "k",
                "meter-",
                "meter+",
                "-meter",
                "+meter",
                "-kilometer",
                "+kilometer",
                "-pow2-meter",
                "+pow2-meter",
                "p2-meter",
                "p4-meter",
                "+",
                "-",
                "-mile",
                "-and-mile",
                "-per-mile",
                "one",
                "one-one",
                "one-per-mile",
                "one-per-cubic-centimeter",
                "square--per-meter",
                "metersecond", // Must have compound part in between single units

                // Negative powers not supported in mixed units yet. TODO(CLDR-13701).
                "per-hour-and-hertz",
                "hertz-and-per-hour",

                // Compound units not supported in mixed units yet. TODO(CLDR-13701).
                "kilonewton-meter-and-newton-meter",

                // Invalid units due to invalid constant denominator
                "meter-per--20-second",
                "meter-per-1000-1e9-second",
                "meter-per-1e20-second",
                "per-1000",
                "meter-per-1000-1000",
                "meter-per-1000-second-1000-kilometer",
                "1000-meter",
                "meter-1000",
                "meter-per-1000-1000",
                "meter-per-1000-second-1000-kilometer",
                "per-1000-and-per-1000",
                "liter-per-kilometer-100",
                "meter-per-100-100-kilometer", // Failing ICU-23045
            };

        for (String input : inputs) {
            if (input.equals("meter-per-100-100-kilometer")) {
                logKnownIssue("ICU-23045", "Incorrect constant denominator for certain unit identifiers " +
                        "leads to incorrect unit identifiers.");
                continue;
            }

            try {
                MeasureUnit.forIdentifier(input);
                Assert.fail("An IllegalArgumentException must be thrown");
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
    }

    @Test
    public void TestGetIdentifierForConstantDenominator() {
        String testCases[][] = {
                { "meter-per-1000", "meter-per-1000" },
                { "meter-per-1000-kilometer", "meter-per-1000-kilometer" },
                { "meter-per-1000000", "meter-per-1e6" },
                { "meter-per-1000000-kilometer", "meter-per-1e6-kilometer" },
                { "meter-per-1000000000", "meter-per-1e9" },
                { "meter-per-1000000000-kilometer", "meter-per-1e9-kilometer" },
                { "meter-per-1000000000000", "meter-per-1e12" },
                { "meter-per-1000000000000-kilometer", "meter-per-1e12-kilometer" },
                { "meter-per-1000000000000000", "meter-per-1e15" },
                { "meter-per-1e15-kilometer", "meter-per-1e15-kilometer" },
                { "meter-per-1000000000000000000", "meter-per-1e18" },
                { "meter-per-1e18-kilometer", "meter-per-1e18-kilometer" },
                { "meter-per-1000000000000001", "meter-per-1000000000000001" },
                { "meter-per-1000000000000001-kilometer", "meter-per-1000000000000001-kilometer" },
        };

        for (String[] testCase : testCases) {
            MeasureUnit unit = MeasureUnit.forIdentifier(testCase[0]);
            String actual = unit.getIdentifier();
            assertEquals(testCase[0], testCase[1], actual);
        }
    }

    @Test
    public void TestIdentifierDetails() {
        MeasureUnit joule = MeasureUnit.forIdentifier("joule");
        assertEquals("Initial joule", "joule", joule.getIdentifier());

        // "Invalid prefix" test not needed: in Java we cannot pass a
        // non-existent enum instance. (In C++ an int can be typecast.)

        MeasureUnit unit = joule.withPrefix(MeasureUnit.MeasurePrefix.HECTO);
        assertEquals("Joule with hecto prefix", "hectojoule", unit.getIdentifier());

        unit = unit.withPrefix(MeasureUnit.MeasurePrefix.EXBI);
        assertEquals("Joule with exbi prefix", "exbijoule", unit.getIdentifier());
    }

    @Test
    public void TestPrefixes() {
        class TestCase {
            final MeasureUnit.MeasurePrefix prefix;
            final int expectedBase;
            final int expectedPower;

            TestCase(MeasureUnit.MeasurePrefix prefix, int expectedBase, int expectedPower) {
                this.prefix = prefix;
                this.expectedBase = expectedBase;
                this.expectedPower = expectedPower;
            }
        }

        TestCase cases[] = {
            new TestCase(MeasureUnit.MeasurePrefix.QUECTO, 10, -30),
            new TestCase(MeasureUnit.MeasurePrefix.RONTO, 10, -27),
            new TestCase(MeasureUnit.MeasurePrefix.YOCTO, 10, -24),
            new TestCase(MeasureUnit.MeasurePrefix.ZEPTO, 10, -21),
            new TestCase(MeasureUnit.MeasurePrefix.ATTO, 10, -18),
            new TestCase(MeasureUnit.MeasurePrefix.FEMTO, 10, -15),
            new TestCase(MeasureUnit.MeasurePrefix.PICO, 10, -12),
            new TestCase(MeasureUnit.MeasurePrefix.NANO, 10, -9),
            new TestCase(MeasureUnit.MeasurePrefix.MICRO, 10, -6),
            new TestCase(MeasureUnit.MeasurePrefix.MILLI, 10, -3),
            new TestCase(MeasureUnit.MeasurePrefix.CENTI, 10, -2),
            new TestCase(MeasureUnit.MeasurePrefix.DECI, 10, -1),
            new TestCase(MeasureUnit.MeasurePrefix.ONE, 10, 0),
            new TestCase(MeasureUnit.MeasurePrefix.DEKA, 10, 1),
            new TestCase(MeasureUnit.MeasurePrefix.HECTO, 10, 2),
            new TestCase(MeasureUnit.MeasurePrefix.KILO, 10, 3),
            new TestCase(MeasureUnit.MeasurePrefix.MEGA, 10, 6),
            new TestCase(MeasureUnit.MeasurePrefix.GIGA, 10, 9),
            new TestCase(MeasureUnit.MeasurePrefix.TERA, 10, 12),
            new TestCase(MeasureUnit.MeasurePrefix.PETA, 10, 15),
            new TestCase(MeasureUnit.MeasurePrefix.EXA, 10, 18),
            new TestCase(MeasureUnit.MeasurePrefix.ZETTA, 10, 21),
            new TestCase(MeasureUnit.MeasurePrefix.YOTTA, 10, 24),
            new TestCase(MeasureUnit.MeasurePrefix.RONNA, 10, 27),
            new TestCase(MeasureUnit.MeasurePrefix.QUETTA, 10, 30),
            new TestCase(MeasureUnit.MeasurePrefix.KIBI, 1024, 1),
            new TestCase(MeasureUnit.MeasurePrefix.MEBI, 1024, 2),
            new TestCase(MeasureUnit.MeasurePrefix.GIBI, 1024, 3),
            new TestCase(MeasureUnit.MeasurePrefix.TEBI, 1024, 4),
            new TestCase(MeasureUnit.MeasurePrefix.PEBI, 1024, 5),
            new TestCase(MeasureUnit.MeasurePrefix.EXBI, 1024, 6),
            new TestCase(MeasureUnit.MeasurePrefix.ZEBI, 1024, 7),
            new TestCase(MeasureUnit.MeasurePrefix.YOBI, 1024, 8),
        };

        for (TestCase testCase : cases) {
            MeasureUnit m = MeasureUnit.AMPERE.withPrefix(testCase.prefix);
            assertEquals("getPrefixPower()", testCase.expectedPower, m.getPrefix().getPower());
            assertEquals("getPrefixBase()", testCase.expectedBase, m.getPrefix().getBase());
        }
    }

    @Test
    public void TestParseBuiltIns() {
        for (MeasureUnit unit : MeasureUnit.getAvailable()) {
            System.out.println("unit ident: " + unit.getIdentifier() + ", type: " + unit.getType());
            if (unit.getType() == "currency") {
                continue;
            }

            if (unit.getIdentifier().equals("portion-per-1e9")) {
            	logKnownIssue("ICU-22781", "Handle concentr/perbillion in ICU");
            	continue;
            }

            // Prove that all built-in units are parseable, except "generic" temperature
            // (and for now, beaufort units)
            if (unit == MeasureUnit.GENERIC_TEMPERATURE) {
                try {
                    MeasureUnit.forIdentifier(unit.getIdentifier());
                    Assert.fail("GENERIC_TEMPERATURE should not be parseable (BEAUFORT also currently non-parseable)");
                } catch (IllegalArgumentException e) {
                    continue;
                }
            } else {
                MeasureUnit parsed = MeasureUnit.forIdentifier(unit.getIdentifier());
                assertTrue("parsed MeasureUnit '" + parsed + "'' should equal built-in '" + unit + "'",
                           unit.equals(parsed));
            }
        }
    }

    @Test
    public void TestParseToBuiltIn() {
        class TestCase {
            final String identifier;
            MeasureUnit expectedBuiltin;

            TestCase(String identifier, MeasureUnit expectedBuiltin) {
                this.identifier = identifier;
                this.expectedBuiltin = expectedBuiltin;
            }
        }

        TestCase cases[] = {
            new TestCase("meter-per-second-per-second", MeasureUnit.METER_PER_SECOND_SQUARED),
            new TestCase("meter-per-second-second", MeasureUnit.METER_PER_SECOND_SQUARED),
            new TestCase("centimeter-centimeter", MeasureUnit.SQUARE_CENTIMETER),
            new TestCase("square-foot", MeasureUnit.SQUARE_FOOT),
            new TestCase("pow2-inch", MeasureUnit.SQUARE_INCH),
            new TestCase("milligram-per-deciliter", MeasureUnit.MILLIGRAM_PER_DECILITER),
            new TestCase("pound-force-per-pow2-inch", MeasureUnit.POUND_PER_SQUARE_INCH),
            new TestCase("yard-pow2-yard", MeasureUnit.CUBIC_YARD),
            new TestCase("square-yard-yard", MeasureUnit.CUBIC_YARD),
        };

        for (TestCase testCase : cases) {
            MeasureUnit m = MeasureUnit.forIdentifier(testCase.identifier);
            assertTrue(testCase.identifier + " parsed to builtin", m.equals(testCase.expectedBuiltin));
        }
    }

    @Test
    public void TestCompoundUnitOperations() {
        MeasureUnit.forIdentifier("kilometer-per-second-joule");

        MeasureUnit kilometer = MeasureUnit.KILOMETER;
        MeasureUnit cubicMeter = MeasureUnit.CUBIC_METER;
        MeasureUnit meter = kilometer.withPrefix(MeasureUnit.MeasurePrefix.ONE);
        MeasureUnit centimeter1 = kilometer.withPrefix(MeasureUnit.MeasurePrefix.CENTI);
        MeasureUnit centimeter2 = meter.withPrefix(MeasureUnit.MeasurePrefix.CENTI);
        MeasureUnit cubicDecimeter = cubicMeter.withPrefix(MeasureUnit.MeasurePrefix.DECI);

        verifySingleUnit(kilometer, MeasureUnit.MeasurePrefix.KILO, 1, "kilometer");
        verifySingleUnit(meter, MeasureUnit.MeasurePrefix.ONE, 1, "meter");
        verifySingleUnit(centimeter1, MeasureUnit.MeasurePrefix.CENTI, 1, "centimeter");
        verifySingleUnit(centimeter2, MeasureUnit.MeasurePrefix.CENTI, 1, "centimeter");
        verifySingleUnit(cubicDecimeter, MeasureUnit.MeasurePrefix.DECI, 3, "cubic-decimeter");

        assertTrue("centimeter equality", centimeter1.equals( centimeter2));
        assertTrue("kilometer inequality", !centimeter1.equals( kilometer));

        MeasureUnit squareMeter = meter.withDimensionality(2);
        MeasureUnit overCubicCentimeter = centimeter1.withDimensionality(-3);
        MeasureUnit quarticKilometer = kilometer.withDimensionality(4);
        MeasureUnit overQuarticKilometer1 = kilometer.withDimensionality(-4);

        verifySingleUnit(squareMeter, MeasureUnit.MeasurePrefix.ONE, 2, "square-meter");
        verifySingleUnit(overCubicCentimeter, MeasureUnit.MeasurePrefix.CENTI, -3, "per-cubic-centimeter");
        verifySingleUnit(quarticKilometer, MeasureUnit.MeasurePrefix.KILO, 4, "pow4-kilometer");
        verifySingleUnit(overQuarticKilometer1, MeasureUnit.MeasurePrefix.KILO, -4, "per-pow4-kilometer");

        assertTrue("power inequality", quarticKilometer != overQuarticKilometer1);

        MeasureUnit overQuarticKilometer2 = quarticKilometer.reciprocal();
        MeasureUnit overQuarticKilometer3 = kilometer.product(kilometer)
                .product(kilometer)
                .product(kilometer)
                .reciprocal();
        MeasureUnit overQuarticKilometer4 = meter.withDimensionality(4)
                .reciprocal()
                .withPrefix(MeasureUnit.MeasurePrefix.KILO);

        verifySingleUnit(overQuarticKilometer2, MeasureUnit.MeasurePrefix.KILO, -4, "per-pow4-kilometer");
        verifySingleUnit(overQuarticKilometer3, MeasureUnit.MeasurePrefix.KILO, -4, "per-pow4-kilometer");
        verifySingleUnit(overQuarticKilometer4, MeasureUnit.MeasurePrefix.KILO, -4, "per-pow4-kilometer");

        assertTrue("reciprocal equality", overQuarticKilometer1.equals(overQuarticKilometer2));
        assertTrue("reciprocal equality", overQuarticKilometer1.equals(overQuarticKilometer3));
        assertTrue("reciprocal equality", overQuarticKilometer1.equals(overQuarticKilometer4));

        MeasureUnit kiloSquareSecond = MeasureUnit.SECOND
                .withDimensionality(2).withPrefix(MeasureUnit.MeasurePrefix.KILO);
        MeasureUnit meterSecond = meter.product(kiloSquareSecond);
        MeasureUnit cubicMeterSecond1 = meter.withDimensionality(3).product(kiloSquareSecond);
        MeasureUnit centimeterSecond1 = meter.withPrefix(MeasureUnit.MeasurePrefix.CENTI).product(kiloSquareSecond);
        MeasureUnit secondCubicMeter = kiloSquareSecond.product(meter.withDimensionality(3));
        MeasureUnit secondCentimeter = kiloSquareSecond.product(meter.withPrefix(MeasureUnit.MeasurePrefix.CENTI));
        MeasureUnit secondCentimeterPerKilometer = secondCentimeter.product(kilometer.reciprocal());

        verifySingleUnit(kiloSquareSecond, MeasureUnit.MeasurePrefix.KILO, 2, "square-kilosecond");
        String meterSecondSub[] = {
                "meter", "square-kilosecond"
        };
        verifyCompoundUnit(meterSecond, "meter-square-kilosecond",
                meterSecondSub, meterSecondSub.length);
        String cubicMeterSecond1Sub[] = {
                "cubic-meter", "square-kilosecond"
        };
        verifyCompoundUnit(cubicMeterSecond1, "cubic-meter-square-kilosecond",
                cubicMeterSecond1Sub, cubicMeterSecond1Sub.length);
        String centimeterSecond1Sub[] = {
                "centimeter", "square-kilosecond"
        };
        verifyCompoundUnit(centimeterSecond1, "centimeter-square-kilosecond",
                centimeterSecond1Sub, centimeterSecond1Sub.length);
        String secondCubicMeterSub[] = {
                "cubic-meter", "square-kilosecond"
        };
        verifyCompoundUnit(secondCubicMeter, "cubic-meter-square-kilosecond",
                secondCubicMeterSub, secondCubicMeterSub.length);
        String secondCentimeterSub[] = {
                "centimeter", "square-kilosecond"
        };
        verifyCompoundUnit(secondCentimeter, "centimeter-square-kilosecond",
                secondCentimeterSub, secondCentimeterSub.length);
        String secondCentimeterPerKilometerSub[] = {
                "centimeter", "square-kilosecond", "per-kilometer"
        };
        verifyCompoundUnit(secondCentimeterPerKilometer, "centimeter-square-kilosecond-per-kilometer",
                secondCentimeterPerKilometerSub, secondCentimeterPerKilometerSub.length);

        assertTrue("reordering equality", cubicMeterSecond1.equals(secondCubicMeter));
        assertTrue("additional simple units inequality", !secondCubicMeter.equals(secondCentimeter));

        // Don't allow get/set power or SI or binary prefix on compound units
        try {
            meterSecond.getDimensionality();
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
            // Expecting an exception to be thrown
        }

        try {
            meterSecond.withDimensionality(3);
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
            // Expecting an exception to be thrown
        }

        try {
            meterSecond.getPrefix();
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
            // Expecting an exception to be thrown
        }

        try {
            meterSecond.withPrefix(MeasureUnit.MeasurePrefix.CENTI);
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
            // Expecting an exception to be thrown
        }

        MeasureUnit footInch = MeasureUnit.forIdentifier("foot-and-inch");
        MeasureUnit inchFoot = MeasureUnit.forIdentifier("inch-and-foot");

        String footInchSub[] = {
                "foot", "inch"
        };
        verifyMixedUnit(footInch, "foot-and-inch",
                footInchSub, footInchSub.length);
        String inchFootSub[] = {
                "inch", "foot"
        };
        verifyMixedUnit(inchFoot, "inch-and-foot",
                inchFootSub, inchFootSub.length);

        assertTrue("order matters inequality", !footInch.equals(inchFoot));


        MeasureUnit dimensionless  = NoUnit.BASE;
        MeasureUnit dimensionless2 = MeasureUnit.forIdentifier("");
        assertEquals("dimensionless equality", dimensionless, dimensionless2);

        // We support starting from an "identity" MeasureUnit and then combining it
        // with others via product:
        MeasureUnit kilometer2 = kilometer.product(dimensionless);

        verifySingleUnit(kilometer2, MeasureUnit.MeasurePrefix.KILO, 1, "kilometer");
        assertTrue("kilometer equality", kilometer.equals(kilometer2));

        // Test out-of-range powers
        MeasureUnit power15 = MeasureUnit.forIdentifier("pow15-kilometer");
        verifySingleUnit(power15, MeasureUnit.MeasurePrefix.KILO, 15, "pow15-kilometer");

        try {
            MeasureUnit.forIdentifier("pow16-kilometer");
            fail("An IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // Expecting an exception to be thrown
        }

        try {
            power15.product(kilometer);
            fail("An IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // Expecting an exception to be thrown
        }

        MeasureUnit powerN15 = MeasureUnit.forIdentifier("per-pow15-kilometer");
        verifySingleUnit(powerN15, MeasureUnit.MeasurePrefix.KILO, -15, "per-pow15-kilometer");

        try {
            MeasureUnit.forIdentifier("per-pow16-kilometer");
            fail("An IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // Expecting an exception to be thrown
        }

        try {
            powerN15.product(overQuarticKilometer1);
            fail("An IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
            // Expecting an exception to be thrown
        }
    }

    @Test
    public void TestDimensionlessBehaviour() {
        MeasureUnit dimensionless = MeasureUnit.forIdentifier("");
        MeasureUnit dimensionless2 = NoUnit.BASE;
        MeasureUnit dimensionless3 = null;
        MeasureUnit dimensionless4 = MeasureUnit.forIdentifier(null);

        assertEquals("dimensionless must be equals", dimensionless, dimensionless2);
        assertEquals("dimensionless must be equals", dimensionless2, dimensionless3);
        assertEquals("dimensionless must be equals", dimensionless3, dimensionless4);

        // product(dimensionless)
        MeasureUnit mile = MeasureUnit.MILE;
        mile = mile.product(dimensionless);
        verifySingleUnit(mile, MeasureUnit.MeasurePrefix.ONE, 1, "mile");
    }

    private void verifySingleUnit(MeasureUnit singleMeasureUnit, MeasureUnit.MeasurePrefix prefix, int power, String identifier) {
        assertEquals(identifier + ": SI or binary prefix", prefix, singleMeasureUnit.getPrefix());

        assertEquals(identifier + ": Power", power, singleMeasureUnit.getDimensionality());

        assertEquals(identifier + ": Identifier", identifier, singleMeasureUnit.getIdentifier());

        assertTrue(identifier + ": Constructor", singleMeasureUnit.equals(MeasureUnit.forIdentifier(identifier)));

        assertEquals(identifier + ": Complexity", MeasureUnit.Complexity.SINGLE, singleMeasureUnit.getComplexity());
    }


    // Kilogram is a "base unit", although it's also "gram" with a kilo- prefix.
    // This tests that it is handled in the preferred manner.
    @Test
    public void TestKilogramIdentifier() {
        // SI unit of mass
        MeasureUnit kilogram = MeasureUnit.forIdentifier("kilogram");
        // Metric mass unit
        MeasureUnit gram = MeasureUnit.forIdentifier("gram");
        // Microgram: still a built-in type
        MeasureUnit microgram = MeasureUnit.forIdentifier("microgram");
        // Nanogram: not a built-in type at this time
        MeasureUnit nanogram = MeasureUnit.forIdentifier("nanogram");

        assertEquals("parsed kilogram equals built-in kilogram", MeasureUnit.KILOGRAM.getType(),
                kilogram.getType());
        assertEquals("parsed kilogram equals built-in kilogram", MeasureUnit.KILOGRAM.getSubtype(),
                kilogram.getSubtype());
        assertEquals("parsed gram equals built-in gram", MeasureUnit.GRAM.getType(), gram.getType());
        assertEquals("parsed gram equals built-in gram", MeasureUnit.GRAM.getSubtype(),
                gram.getSubtype());
        assertEquals("parsed microgram equals built-in microgram", MeasureUnit.MICROGRAM.getType(),
                microgram.getType());
        assertEquals("parsed microgram equals built-in microgram", MeasureUnit.MICROGRAM.getSubtype(),
                microgram.getSubtype());
        assertEquals("nanogram", null, nanogram.getType());
        assertEquals("nanogram", "nanogram", nanogram.getIdentifier());

        assertEquals("prefix of kilogram", MeasureUnit.MeasurePrefix.KILO, kilogram.getPrefix());
        assertEquals("prefix of gram", MeasureUnit.MeasurePrefix.ONE, gram.getPrefix());
        assertEquals("prefix of microgram", MeasureUnit.MeasurePrefix.MICRO, microgram.getPrefix());
        assertEquals("prefix of nanogram", MeasureUnit.MeasurePrefix.NANO, nanogram.getPrefix());

        MeasureUnit tmp = kilogram.withPrefix(MeasureUnit.MeasurePrefix.MILLI);
        assertEquals("Kilogram + milli should be milligram, got: " + tmp.getIdentifier(),
                MeasureUnit.MILLIGRAM.getIdentifier(), tmp.getIdentifier());
    }

    @Test
    public void TestInternalMeasureUnitImpl() {
        MeasureUnitImpl mu1 = MeasureUnitImpl.forIdentifier("meter");
        assertEquals("mu1 initial identifier", null, mu1.getIdentifier());
        assertEquals("mu1 initial complexity", MeasureUnit.Complexity.SINGLE, mu1.getComplexity());
        assertEquals("mu1 initial units length", 1, mu1.getSingleUnits().size());
        assertEquals("mu1 initial units[0]", "meter", mu1.getSingleUnits().get(0).getSimpleUnitID());

        // Producing identifier via build(): the MeasureUnitImpl instance gets modified
        // while it also gets assigned to tmp's internal measureUnitImpl.
        MeasureUnit tmp = mu1.build();
        assertEquals("mu1 post-build identifier", "meter", mu1.getIdentifier());
        assertEquals("mu1 post-build complexity", MeasureUnit.Complexity.SINGLE, mu1.getComplexity());
        assertEquals("mu1 post-build units length", 1, mu1.getSingleUnits().size());
        assertEquals("mu1 post-build units[0]", "meter", mu1.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("MeasureUnit tmp identifier", "meter", tmp.getIdentifier());

        mu1 = MeasureUnitImpl.forIdentifier("hour-and-minute-and-second");
        assertEquals("mu1 = HMS: identifier", null, mu1.getIdentifier());
        assertEquals("mu1 = HMS: complexity", MeasureUnit.Complexity.MIXED, mu1.getComplexity());
        assertEquals("mu1 = HMS: units length", 3, mu1.getSingleUnits().size());
        assertEquals("mu1 = HMS: units[0]", "hour", mu1.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("mu1 = HMS: units[1]", "minute", mu1.getSingleUnits().get(1).getSimpleUnitID());
        assertEquals("mu1 = HMS: units[2]", "second", mu1.getSingleUnits().get(2).getSimpleUnitID());

        MeasureUnitImpl m2 = MeasureUnitImpl.forIdentifier("meter");
        m2.appendSingleUnit(MeasureUnit.METER.getCopyOfMeasureUnitImpl().getSingleUnitImpl());
        assertEquals("append meter twice: complexity", MeasureUnit.Complexity.SINGLE, m2.getComplexity());
        assertEquals("append meter twice: units length", 1, m2.getSingleUnits().size());
        assertEquals("append meter twice: units[0]", "meter", m2.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("append meter twice: identifier", "square-meter", m2.build().getIdentifier());

        MeasureUnitImpl mcm = MeasureUnitImpl.forIdentifier("meter");
        mcm.appendSingleUnit(MeasureUnit.CENTIMETER.getCopyOfMeasureUnitImpl().getSingleUnitImpl());
        assertEquals("append meter & centimeter: complexity", MeasureUnit.Complexity.COMPOUND, mcm.getComplexity());
        assertEquals("append meter & centimeter: units length", 2, mcm.getSingleUnits().size());
        assertEquals("append meter & centimeter: units[0]", "meter", mcm.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("append meter & centimeter: units[1]", "meter", mcm.getSingleUnits().get(1).getSimpleUnitID());
        assertEquals("append meter & centimeter: identifier", "meter-centimeter", mcm.build().getIdentifier());

        MeasureUnitImpl m2m = MeasureUnitImpl.forIdentifier("meter-square-meter");
        assertEquals("meter-square-meter: complexity", MeasureUnit.Complexity.SINGLE, m2m.getComplexity());
        assertEquals("meter-square-meter: units length", 1, m2m.getSingleUnits().size());
        assertEquals("meter-square-meter: units[0]", "meter", m2m.getSingleUnits().get(0).getSimpleUnitID());
        assertEquals("meter-square-meter: identifier", "cubic-meter", m2m.build().getIdentifier());

    }

    private void verifyCompoundUnit(
            MeasureUnit unit,
            String identifier,
            String subIdentifiers[],
            int subIdentifierCount) {
        assertEquals(identifier + ": Identifier",
                identifier,
                unit.getIdentifier());

        assertTrue(identifier + ": Constructor",
                unit.equals(MeasureUnit.forIdentifier(identifier)));

        assertEquals(identifier + ": Complexity",
                MeasureUnit.Complexity.COMPOUND,
                unit.getComplexity());

        List<MeasureUnit> subUnits = unit.splitToSingleUnits();
        assertEquals(identifier + ": Length", subIdentifierCount, subUnits.size());
        for (int i = 0; ; i++) {
            if (i >= subIdentifierCount || i >= subUnits.size()) break;
            assertEquals(identifier + ": Sub-unit #" + i,
                    subIdentifiers[i],
                    subUnits.get(i).getIdentifier());
            assertEquals(identifier + ": Sub-unit Complexity",
                    MeasureUnit.Complexity.SINGLE,
                    subUnits.get(i).getComplexity());
        }
    }

    private void verifyMixedUnit(
            MeasureUnit unit,
            String identifier,
            String subIdentifiers[],
            int subIdentifierCount) {
        assertEquals(identifier + ": Identifier",
                identifier,
                unit.getIdentifier());
        assertTrue(identifier + ": Constructor",
                unit.equals(MeasureUnit.forIdentifier(identifier)));

        assertEquals(identifier + ": Complexity",
                MeasureUnit.Complexity.MIXED,
                unit.getComplexity());

        List<MeasureUnit> subUnits = unit.splitToSingleUnits();
        assertEquals(identifier + ": Length", subIdentifierCount, subUnits.size());
        for (int i = 0; ; i++) {
            if (i >= subIdentifierCount || i >= subUnits.size()) break;
            assertEquals(identifier + ": Sub-unit #" + i,
                    subIdentifiers[i],
                    subUnits.get(i).getIdentifier());
        }
    }
}
