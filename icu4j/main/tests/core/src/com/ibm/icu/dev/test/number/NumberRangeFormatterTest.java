// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

import com.ibm.icu.number.LocalizedNumberRangeFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.NumberRangeFormatter;
import com.ibm.icu.number.NumberRangeFormatter.RangeCollapse;
import com.ibm.icu.number.NumberRangeFormatter.RangeIdentityFallback;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberRangeFormatter;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

/**
 * @author sffc
 *
 */
public class NumberRangeFormatterTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency PTE = Currency.getInstance("PTE");

    @Test
    public void testSanity() {
        LocalizedNumberRangeFormatter lnrf1 = NumberRangeFormatter.withLocale(ULocale.US);
        LocalizedNumberRangeFormatter lnrf2 = NumberRangeFormatter.with().locale(ULocale.US);
        LocalizedNumberRangeFormatter lnrf3 = NumberRangeFormatter.withLocale(Locale.US);
        LocalizedNumberRangeFormatter lnrf4 = NumberRangeFormatter.with().locale(Locale.US);
        assertEquals("Formatters should be equal 1", lnrf1, lnrf2);
        assertEquals("Formatters should be equal 2", lnrf2, lnrf3);
        assertEquals("Formatters should be equal 3", lnrf3, lnrf4);
        assertEquals("Formatters should have same behavior 1", lnrf1.formatRange(4, 6), lnrf2.formatRange(4, 6));
        assertEquals("Formatters should have same behavior 2", lnrf2.formatRange(4, 6), lnrf3.formatRange(4, 6));
        assertEquals("Formatters should have same behavior 3", lnrf3.formatRange(4, 6), lnrf4.formatRange(4, 6));
    }

    @Test
    public void testBasic() {
        assertFormatRange(
            "Basic",
            NumberRangeFormatter.with(),
            new ULocale("en-us"),
            "1–5",
            "~5",
            "~5",
            "0–3",
            "~0",
            "3–3,000",
            "3,000–5,000",
            "4,999–5,001",
            "~5,000",
            "5,000–5,000,000");

        assertFormatRange(
            "Basic with units",
            NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with().unit(MeasureUnit.METER)),
            new ULocale("en-us"),
            "1–5 m",
            "~5 m",
            "~5 m",
            "0–3 m",
            "~0 m",
            "3–3,000 m",
            "3,000–5,000 m",
            "4,999–5,001 m",
            "~5,000 m",
            "5,000–5,000,000 m");

        assertFormatRange(
            "Basic with different units",
            NumberRangeFormatter.with()
                .numberFormatterFirst(NumberFormatter.with().unit(MeasureUnit.METER))
                .numberFormatterSecond(NumberFormatter.with().unit(MeasureUnit.KILOMETER)),
            new ULocale("en-us"),
            "1 m – 5 km",
            "5 m – 5 km",
            "5 m – 5 km",
            "0 m – 3 km",
            "0 m – 0 km",
            "3 m – 3,000 km",
            "3,000 m – 5,000 km",
            "4,999 m – 5,001 km",
            "5,000 m – 5,000 km",
            "5,000 m – 5,000,000 km");

        assertFormatRange(
            "Basic long unit",
            NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with().unit(MeasureUnit.METER).unitWidth(UnitWidth.FULL_NAME)),
            new ULocale("en-us"),
            "1 meter – 5 meters",  // TODO: This doesn't collapse because the plurals are different.  Fix?
            "~5 meters",
            "~5 meters",
            "0–3 meters",  // Note: It collapses when the plurals are the same
            "~0 meters",
            "3–3,000 meters",
            "3,000–5,000 meters",
            "4,999–5,001 meters",
            "~5,000 meters",
            "5,000–5,000,000 meters");

        assertFormatRange(
            "Non-English locale and unit",
            NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with().unit(MeasureUnit.FAHRENHEIT).unitWidth(UnitWidth.FULL_NAME)),
            new ULocale("fr-FR"),
            "1 degré Fahrenheit – 5 degrés Fahrenheit",
            "~5 degrés Fahrenheit",
            "~5 degrés Fahrenheit",
            "0 degré Fahrenheit – 3 degrés Fahrenheit",
            "~0 degré Fahrenheit",
            "3–3 000 degrés Fahrenheit",
            "3 000–5 000 degrés Fahrenheit",
            "4 999–5 001 degrés Fahrenheit",
            "~5 000 degrés Fahrenheit",
            "5 000–5 000 000 degrés Fahrenheit");

        assertFormatRange(
            "Portuguese currency",
            NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with().unit(PTE)),
            new ULocale("pt-PT"),
            "1$00 - 5$00 \u200B",
            "~5$00 \u200B",
            "~5$00 \u200B",
            "0$00 - 3$00 \u200B",
            "~0$00 \u200B",
            "3$00 - 3000$00 \u200B",
            "3000$00 - 5000$00 \u200B",
            "4999$00 - 5001$00 \u200B",
            "~5000$00 \u200B",
            "5000$00 - 5,000,000$00 \u200B");
    }

    @Test
    public void testCollapse() {
        assertFormatRange(
            "Default collapse on currency (default rounding)",
            NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with().unit(USD)),
            new ULocale("en-us"),
            "$1.00 – $5.00",
            "~$5.00",
            "~$5.00",
            "$0.00 – $3.00",
            "~$0.00",
            "$3.00 – $3,000.00",
            "$3,000.00 – $5,000.00",
            "$4,999.00 – $5,001.00",
            "~$5,000.00",
            "$5,000.00 – $5,000,000.00");

        assertFormatRange(
            "Default collapse on currency",
            NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with().unit(USD).precision(Precision.integer())),
            new ULocale("en-us"),
            "$1 – $5",
            "~$5",
            "~$5",
            "$0 – $3",
            "~$0",
            "$3 – $3,000",
            "$3,000 – $5,000",
            "$4,999 – $5,001",
            "~$5,000",
            "$5,000 – $5,000,000");

        assertFormatRange(
            "No collapse on currency",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.NONE)
                .numberFormatterBoth(NumberFormatter.with().unit(USD).precision(Precision.integer())),
            new ULocale("en-us"),
            "$1 – $5",
            "~$5",
            "~$5",
            "$0 – $3",
            "~$0",
            "$3 – $3,000",
            "$3,000 – $5,000",
            "$4,999 – $5,001",
            "~$5,000",
            "$5,000 – $5,000,000");

        assertFormatRange(
            "Unit collapse on currency",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.UNIT)
                .numberFormatterBoth(NumberFormatter.with().unit(USD).precision(Precision.integer())),
            new ULocale("en-us"),
            "$1–5",
            "~$5",
            "~$5",
            "$0–3",
            "~$0",
            "$3–3,000",
            "$3,000–5,000",
            "$4,999–5,001",
            "~$5,000",
            "$5,000–5,000,000");

        assertFormatRange(
            "All collapse on currency",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.ALL)
                .numberFormatterBoth(NumberFormatter.with().unit(USD).precision(Precision.integer())),
            new ULocale("en-us"),
            "$1–5",
            "~$5",
            "~$5",
            "$0–3",
            "~$0",
            "$3–3,000",
            "$3,000–5,000",
            "$4,999–5,001",
            "~$5,000",
            "$5,000–5,000,000");

        assertFormatRange(
            "Default collapse on currency ISO code",
            NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with()
                    .unit(GBP)
                    .unitWidth(UnitWidth.ISO_CODE)
                    .precision(Precision.integer())),
            new ULocale("en-us"),
            "GBP 1–5",
            "~GBP 5",  // TODO: Fix this at some point
            "~GBP 5",
            "GBP 0–3",
            "~GBP 0",
            "GBP 3–3,000",
            "GBP 3,000–5,000",
            "GBP 4,999–5,001",
            "~GBP 5,000",
            "GBP 5,000–5,000,000");

        assertFormatRange(
            "No collapse on currency ISO code",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.NONE)
                .numberFormatterBoth(NumberFormatter.with()
                    .unit(GBP)
                    .unitWidth(UnitWidth.ISO_CODE)
                    .precision(Precision.integer())),
            new ULocale("en-us"),
            "GBP 1 – GBP 5",
            "~GBP 5",  // TODO: Fix this at some point
            "~GBP 5",
            "GBP 0 – GBP 3",
            "~GBP 0",
            "GBP 3 – GBP 3,000",
            "GBP 3,000 – GBP 5,000",
            "GBP 4,999 – GBP 5,001",
            "~GBP 5,000",
            "GBP 5,000 – GBP 5,000,000");

        assertFormatRange(
            "Unit collapse on currency ISO code",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.UNIT)
                .numberFormatterBoth(NumberFormatter.with()
                    .unit(GBP)
                    .unitWidth(UnitWidth.ISO_CODE)
                    .precision(Precision.integer())),
            new ULocale("en-us"),
            "GBP 1–5",
            "~GBP 5",  // TODO: Fix this at some point
            "~GBP 5",
            "GBP 0–3",
            "~GBP 0",
            "GBP 3–3,000",
            "GBP 3,000–5,000",
            "GBP 4,999–5,001",
            "~GBP 5,000",
            "GBP 5,000–5,000,000");

        assertFormatRange(
            "All collapse on currency ISO code",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.ALL)
                .numberFormatterBoth(NumberFormatter.with()
                    .unit(GBP)
                    .unitWidth(UnitWidth.ISO_CODE)
                    .precision(Precision.integer())),
            new ULocale("en-us"),
            "GBP 1–5",
            "~GBP 5",  // TODO: Fix this at some point
            "~GBP 5",
            "GBP 0–3",
            "~GBP 0",
            "GBP 3–3,000",
            "GBP 3,000–5,000",
            "GBP 4,999–5,001",
            "~GBP 5,000",
            "GBP 5,000–5,000,000");

        // Default collapse on measurement unit is in testBasic()

        assertFormatRange(
            "No collapse on measurement unit",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.NONE)
                .numberFormatterBoth(NumberFormatter.with().unit(MeasureUnit.METER)),
            new ULocale("en-us"),
            "1 m – 5 m",
            "~5 m",
            "~5 m",
            "0 m – 3 m",
            "~0 m",
            "3 m – 3,000 m",
            "3,000 m – 5,000 m",
            "4,999 m – 5,001 m",
            "~5,000 m",
            "5,000 m – 5,000,000 m");

        assertFormatRange(
            "Unit collapse on measurement unit",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.UNIT)
                .numberFormatterBoth(NumberFormatter.with().unit(MeasureUnit.METER)),
            new ULocale("en-us"),
            "1–5 m",
            "~5 m",
            "~5 m",
            "0–3 m",
            "~0 m",
            "3–3,000 m",
            "3,000–5,000 m",
            "4,999–5,001 m",
            "~5,000 m",
            "5,000–5,000,000 m");

        assertFormatRange(
            "All collapse on measurement unit",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.ALL)
                .numberFormatterBoth(NumberFormatter.with().unit(MeasureUnit.METER)),
            new ULocale("en-us"),
            "1–5 m",
            "~5 m",
            "~5 m",
            "0–3 m",
            "~0 m",
            "3–3,000 m",
            "3,000–5,000 m",
            "4,999–5,001 m",
            "~5,000 m",
            "5,000–5,000,000 m");

        assertFormatRange(
            "Default collapse on measurement unit with compact-short notation",
            NumberRangeFormatter.with()
                .numberFormatterBoth(NumberFormatter.with().notation(Notation.compactShort()).unit(MeasureUnit.METER)),
            new ULocale("en-us"),
            "1–5 m",
            "~5 m",
            "~5 m",
            "0–3 m",
            "~0 m",
            "3–3K m",
            "3K – 5K m",
            "~5K m",
            "~5K m",
            "5K – 5M m");

        assertFormatRange(
            "No collapse on measurement unit with compact-short notation",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.NONE)
                .numberFormatterBoth(NumberFormatter.with().notation(Notation.compactShort()).unit(MeasureUnit.METER)),
            new ULocale("en-us"),
            "1 m – 5 m",
            "~5 m",
            "~5 m",
            "0 m – 3 m",
            "~0 m",
            "3 m – 3K m",
            "3K m – 5K m",
            "~5K m",
            "~5K m",
            "5K m – 5M m");

        assertFormatRange(
            "Unit collapse on measurement unit with compact-short notation",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.UNIT)
                .numberFormatterBoth(NumberFormatter.with().notation(Notation.compactShort()).unit(MeasureUnit.METER)),
            new ULocale("en-us"),
            "1–5 m",
            "~5 m",
            "~5 m",
            "0–3 m",
            "~0 m",
            "3–3K m",
            "3K – 5K m",
            "~5K m",
            "~5K m",
            "5K – 5M m");

        assertFormatRange(
            "All collapse on measurement unit with compact-short notation",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.ALL)
                .numberFormatterBoth(NumberFormatter.with().notation(Notation.compactShort()).unit(MeasureUnit.METER)),
            new ULocale("en-us"),
            "1–5 m",
            "~5 m",
            "~5 m",
            "0–3 m",
            "~0 m",
            "3–3K m",
            "3–5K m",  // this one is the key use case for ALL
            "~5K m",
            "~5K m",
            "5K – 5M m");

        assertFormatRange(
            "No collapse on scientific notation",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.NONE)
                .numberFormatterBoth(NumberFormatter.with().notation(Notation.scientific())),
            new ULocale("en-us"),
            "1E0 – 5E0",
            "~5E0",
            "~5E0",
            "0E0 – 3E0",
            "~0E0",
            "3E0 – 3E3",
            "3E3 – 5E3",
            "4.999E3 – 5.001E3",
            "~5E3",
            "5E3 – 5E6");

        assertFormatRange(
            "All collapse on scientific notation",
            NumberRangeFormatter.with()
                .collapse(RangeCollapse.ALL)
                .numberFormatterBoth(NumberFormatter.with().notation(Notation.scientific())),
            new ULocale("en-us"),
            "1–5E0",
            "~5E0",
            "~5E0",
            "0–3E0",
            "~0E0",
            "3E0 – 3E3",
            "3–5E3",
            "4.999–5.001E3",
            "~5E3",
            "5E3 – 5E6");

        // TODO: Test compact currency?
        // The code is not smart enough to differentiate the notation from the unit.
    }

    @Test
    public void testIdentity() {
        assertFormatRange(
            "Identity fallback Range",
            NumberRangeFormatter.with().identityFallback(RangeIdentityFallback.RANGE),
            new ULocale("en-us"),
            "1–5",
            "5–5",
            "5–5",
            "0–3",
            "0–0",
            "3–3,000",
            "3,000–5,000",
            "4,999–5,001",
            "5,000–5,000",
            "5,000–5,000,000");

        assertFormatRange(
            "Identity fallback Approximately or Single Value",
            NumberRangeFormatter.with().identityFallback(RangeIdentityFallback.APPROXIMATELY_OR_SINGLE_VALUE),
            new ULocale("en-us"),
            "1–5",
            "~5",
            "5",
            "0–3",
            "0",
            "3–3,000",
            "3,000–5,000",
            "4,999–5,001",
            "5,000",
            "5,000–5,000,000");

        assertFormatRange(
            "Identity fallback  Single Value",
            NumberRangeFormatter.with().identityFallback(RangeIdentityFallback.SINGLE_VALUE),
            new ULocale("en-us"),
            "1–5",
            "5",
            "5",
            "0–3",
            "0",
            "3–3,000",
            "3,000–5,000",
            "4,999–5,001",
            "5,000",
            "5,000–5,000,000");

        assertFormatRange(
            "Identity fallback Approximately or Single Value with compact notation",
            NumberRangeFormatter.with()
                .identityFallback(RangeIdentityFallback.APPROXIMATELY_OR_SINGLE_VALUE)
                .numberFormatterBoth(NumberFormatter.with().notation(Notation.compactShort())),
            new ULocale("en-us"),
            "1–5",
            "~5",
            "5",
            "0–3",
            "0",
            "3–3K",
            "3K – 5K",
            "~5K",
            "5K",
            "5K – 5M");
    }

    @Test
    public void testDifferentFormatters() {
        assertFormatRange(
            "Different rounding rules",
            NumberRangeFormatter.with()
                .numberFormatterFirst(NumberFormatter.with().precision(Precision.integer()))
                .numberFormatterSecond(NumberFormatter.with().precision(Precision.fixedDigits(2))),
            new ULocale("en-us"),
            "1–5.0",
            "5–5.0",
            "5–5.0",
            "0–3.0",
            "0–0.0",
            "3–3,000",
            "3,000–5,000",
            "4,999–5,000",
            "5,000–5,000",  // TODO: Should this one be ~5,000?
            "5,000–5,000,000");
    }

    static void assertFormatRange(
            String message,
            UnlocalizedNumberRangeFormatter f,
            ULocale locale,
            String expected_10_50,
            String expected_49_51,
            String expected_50_50,
            String expected_00_30,
            String expected_00_00,
            String expected_30_3K,
            String expected_30K_50K,
            String expected_49K_51K,
            String expected_50K_50K,
            String expected_50K_50M) {
        LocalizedNumberRangeFormatter l = f.locale(locale);
        assertFormattedRangeEquals(message, l, 1, 5, expected_10_50);
        assertFormattedRangeEquals(message, l, 4.9999999, 5.0000001, expected_49_51);
        assertFormattedRangeEquals(message, l, 5, 5, expected_50_50);
        assertFormattedRangeEquals(message, l, 0, 3, expected_00_30);
        assertFormattedRangeEquals(message, l, 0, 0, expected_00_00);
        assertFormattedRangeEquals(message, l, 3, 3000, expected_30_3K);
        assertFormattedRangeEquals(message, l, 3000, 5000, expected_30K_50K);
        assertFormattedRangeEquals(message, l, 4999, 5001, expected_49K_51K);
        assertFormattedRangeEquals(message, l, 5000, 5000, expected_50K_50K);
        assertFormattedRangeEquals(message, l, 5e3, 5e6, expected_50K_50M);
    }

    private static void assertFormattedRangeEquals(String message, LocalizedNumberRangeFormatter l, Number first,
            Number second, String expected) {
        String actual = l.formatRange(first, second).toString();
        assertEquals(message + ": " + first + ", " + second, expected, actual);
    }

}
