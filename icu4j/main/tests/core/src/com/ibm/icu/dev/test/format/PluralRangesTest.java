// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.range.StandardPluralRanges;
import com.ibm.icu.number.FormattedNumberRange;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.NumberRangeFormatter;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
@RunWith(JUnit4.class)
public class PluralRangesTest extends TestFmwk {
    @Test
    public void TestLocaleData() {
        String[][] tests = {
                {"de", "other", "one", "one"},
                {"xxx", "other", "other", "other" },
                {"de", "one", "other", "other"},
                {"de", "other", "one", "one"},
                {"de", "other", "other", "other"},
                {"ro", "one", "few", "few"},
                {"ro", "one", "other", "other"},
                {"ro", "few", "one", "few"},
        };
        for (String[] test : tests) {
            final ULocale locale = new ULocale(test[0]);
            final StandardPlural start = StandardPlural.fromString(test[1]);
            final StandardPlural end = StandardPlural.fromString(test[2]);
            final StandardPlural expected = StandardPlural.fromString(test[3]);
            final StandardPluralRanges pluralRanges = StandardPluralRanges.forLocale(locale);

            StandardPlural actual = pluralRanges.resolve(start, end);
            assertEquals("Deriving range category", expected, actual);
        }
    }

    @Test
    public void TestRangePattern() {
        String[][] tests = {
                {"de", "SHORT", "{0}–{1}"},
                {"ja", "NARROW", "{0}～{1}"},
        };
        for (String[] test : tests) {
            ULocale ulocale = new ULocale(test[0]);
            FormatWidth width = FormatWidth.valueOf(test[1]);
            String expected = test[2];
            String formatter = MeasureFormat.getRangeFormat(ulocale, width);
            String actual = SimpleFormatterImpl.formatCompiledPattern(formatter, "{0}", "{1}");
            assertEquals("range pattern " + Arrays.asList(test), expected, actual);
        }
    }

    @Test
    public void TestFormatting() {
        Object[][] tests = {
                {0.0, 1.0, ULocale.FRANCE, UnitWidth.FULL_NAME, MeasureUnit.FAHRENHEIT, "0–1\u00A0degré Fahrenheit"},
                {1.0, 2.0, ULocale.FRANCE, UnitWidth.FULL_NAME, MeasureUnit.FAHRENHEIT, "1–2\u00A0degrés Fahrenheit"},
                {3.1, 4.25, ULocale.FRANCE, UnitWidth.SHORT, MeasureUnit.FAHRENHEIT, "3,1–4,25\u202F°F"},
                {3.1, 4.25, ULocale.ENGLISH, UnitWidth.SHORT, MeasureUnit.FAHRENHEIT, "3.1–4.25°F"},
                {3.1, 4.25, ULocale.CHINESE, UnitWidth.FULL_NAME, MeasureUnit.INCH, "3.1-4.25英寸"},
                {0.0, 1.0, ULocale.ENGLISH, UnitWidth.FULL_NAME, MeasureUnit.INCH, "0–1 inches"},

                {0.0, 1.0, ULocale.ENGLISH, UnitWidth.NARROW, Currency.getInstance("EUR"), "€0.00 – €1.00"},
                {0.0, 1.0, ULocale.FRENCH, UnitWidth.NARROW, Currency.getInstance("EUR"), "0,00–1,00 €"},
                {0.0, 100.0, ULocale.FRENCH, UnitWidth.NARROW, Currency.getInstance("JPY"), "0–100\u00a0¥"},

                {0.0, 1.0, ULocale.ENGLISH, UnitWidth.SHORT, Currency.getInstance("EUR"), "€0.00 – €1.00"},
                {0.0, 1.0, ULocale.FRENCH, UnitWidth.SHORT, Currency.getInstance("EUR"), "0,00–1,00\u00a0€"},
                {0.0, 100.0, ULocale.FRENCH, UnitWidth.SHORT, Currency.getInstance("JPY"), "0–100\u00a0JPY"},

                {0.0, 1.0, ULocale.ENGLISH, UnitWidth.FULL_NAME, Currency.getInstance("EUR"), "0.00–1.00 euros"},
                {0.0, 1.0, ULocale.FRENCH, UnitWidth.FULL_NAME, Currency.getInstance("EUR"), "0,00–1,00 euro"},
                {0.0, 2.0, ULocale.FRENCH, UnitWidth.FULL_NAME, Currency.getInstance("EUR"), "0,00–2,00 euros"},
                {0.0, 100.0, ULocale.FRENCH, UnitWidth.FULL_NAME, Currency.getInstance("JPY"), "0–100 yens japonais"},
        };
        int i = 0;
        for (Object[] test : tests) {
            ++i;
            double low = (Double) test[0];
            double high = (Double) test[1];
            final ULocale locale = (ULocale) test[2];
            final UnitWidth unitWidth = (UnitWidth) test[3];
            final MeasureUnit unit = (MeasureUnit) test[4];
            final String expected = (String) test[5];

            FormattedNumberRange actual = NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with().unit(unit).unitWidth(unitWidth))
                .locale(locale)
                .formatRange(low, high);
            assertEquals(i + " Formatting unit", expected, actual.toString());
        }
    }
}
