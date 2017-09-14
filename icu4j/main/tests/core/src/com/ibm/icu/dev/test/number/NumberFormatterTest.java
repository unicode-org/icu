// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.ULocale;

import newapi.FormattedNumber;
import newapi.Grouper;
import newapi.IntegerWidth;
import newapi.LocalizedNumberFormatter;
import newapi.Notation;
import newapi.NumberFormatter;
import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnitWidth;
import newapi.Rounder;
import newapi.UnlocalizedNumberFormatter;
import newapi.impl.Padder;
import newapi.impl.Padder.PadPosition;

public class NumberFormatterTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency CZK = Currency.getInstance("CZK");

    @Test
    public void notationSimple() {
        assertFormatDescending(
                "Basic",
                "",
                NumberFormatter.with(),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                "0.8765",
                "0.08765",
                "0.008765",
                "0");

        assertFormatSingle(
                "Basic with Negative Sign",
                "",
                NumberFormatter.with(),
                ULocale.ENGLISH,
                -9876543.21,
                "-9,876,543.21");
    }

    @Test
    public void notationScientific() {
        assertFormatDescending(
                "Scientific",
                "E",
                NumberFormatter.with().notation(Notation.scientific()),
                ULocale.ENGLISH,
                "8.765E4",
                "8.765E3",
                "8.765E2",
                "8.765E1",
                "8.765E0",
                "8.765E-1",
                "8.765E-2",
                "8.765E-3",
                "0E0");

        assertFormatDescending(
                "Engineering",
                "E3",
                NumberFormatter.with().notation(Notation.engineering()),
                ULocale.ENGLISH,
                "87.65E3",
                "8.765E3",
                "876.5E0",
                "87.65E0",
                "8.765E0",
                "876.5E-3",
                "87.65E-3",
                "8.765E-3",
                "0E0");

        assertFormatDescending(
                "Scientific sign always shown",
                "E+",
                NumberFormatter.with().notation(Notation.scientific().withExponentSignDisplay(SignDisplay.ALWAYS)),
                ULocale.ENGLISH,
                "8.765E+4",
                "8.765E+3",
                "8.765E+2",
                "8.765E+1",
                "8.765E+0",
                "8.765E-1",
                "8.765E-2",
                "8.765E-3",
                "0E+0");

        assertFormatDescending(
                "Scientific min exponent digits",
                "E00",
                NumberFormatter.with().notation(Notation.scientific().withMinExponentDigits(2)),
                ULocale.ENGLISH,
                "8.765E04",
                "8.765E03",
                "8.765E02",
                "8.765E01",
                "8.765E00",
                "8.765E-01",
                "8.765E-02",
                "8.765E-03",
                "0E00");

        assertFormatSingle(
                "Scientific Negative",
                "E",
                NumberFormatter.with().notation(Notation.scientific()),
                ULocale.ENGLISH,
                -1000000,
                "-1E6");
    }

    @Test
    public void notationCompact() {
        assertFormatDescending(
                "Compact Short",
                "C",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                "88K",
                "8.8K",
                "876",
                "88",
                "8.8",
                "0.88",
                "0.088",
                "0.0088",
                "0");

        assertFormatDescending(
                "Compact Long",
                "CC",
                NumberFormatter.with().notation(Notation.compactLong()),
                ULocale.ENGLISH,
                "88 thousand",
                "8.8 thousand",
                "876",
                "88",
                "8.8",
                "0.88",
                "0.088",
                "0.0088",
                "0");

        assertFormatDescending(
                "Compact Short Currency",
                "C $USD",
                NumberFormatter.with().notation(Notation.compactShort()).unit(USD),
                ULocale.ENGLISH,
                "$88K",
                "$8.8K",
                "$876",
                "$88",
                "$8.8",
                "$0.88",
                "$0.088",
                "$0.0088",
                "$0");

        assertFormatDescending(
                "Compact Short with ISO Currency",
                "C $USD unit-width=ISO_CODE",
                NumberFormatter.with()
                    .notation(Notation.compactShort())
                    .unit(USD)
                    .unitWidth(UnitWidth.ISO_CODE),
                ULocale.ENGLISH,
                "USD 88K",
                "USD 8.8K",
                "USD 876",
                "USD 88",
                "USD 8.8",
                "USD 0.88",
                "USD 0.088",
                "USD 0.0088",
                "USD 0");

        assertFormatDescending(
                "Compact Short with Long Name Currency",
                "C $USD unit-width=FULL_NAME",
                NumberFormatter.with()
                    .notation(Notation.compactShort())
                    .unit(USD)
                    .unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                "88K US dollars",
                "8.8K US dollars",
                "876 US dollars",
                "88 US dollars",
                "8.8 US dollars",
                "0.88 US dollars",
                "0.088 US dollars",
                "0.0088 US dollars",
                "0 US dollars");

        // Note: Most locales don't have compact long currency, so this currently falls back to short.
        // This test case should be fixed when proper compact long currency patterns are added.
        assertFormatDescending(
                "Compact Long Currency",
                "CC $USD",
                NumberFormatter.with().notation(Notation.compactLong()).unit(USD),
                ULocale.ENGLISH,
                "$88K", // should be something like "$88 thousand"
                "$8.8K",
                "$876",
                "$88",
                "$8.8",
                "$0.88",
                "$0.088",
                "$0.0088",
                "$0");

        // Note: Most locales don't have compact long currency, so this currently falls back to short.
        // This test case should be fixed when proper compact long currency patterns are added.
        assertFormatDescending(
                "Compact Long with ISO Currency",
                "CC $USD unit-width=ISO_CODE",
                NumberFormatter.with()
                    .notation(Notation.compactLong())
                    .unit(USD)
                    .unitWidth(UnitWidth.ISO_CODE),
                ULocale.ENGLISH,
                "USD 88K", // should be something like "USD 88 thousand"
                "USD 8.8K",
                "USD 876",
                "USD 88",
                "USD 8.8",
                "USD 0.88",
                "USD 0.088",
                "USD 0.0088",
                "USD 0");

        // TODO: This behavior could be improved and should be revisited.
        assertFormatDescending(
                "Compact Long with Long Name Currency",
                "CC $USD unit-width=FULL_NAME",
                NumberFormatter.with()
                    .notation(Notation.compactLong())
                    .unit(USD)
                    .unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                "88 thousand US dollars",
                "8.8 thousand US dollars",
                "876 US dollars",
                "88 US dollars",
                "8.8 US dollars",
                "0.88 US dollars",
                "0.088 US dollars",
                "0.0088 US dollars",
                "0 US dollars");

        assertFormatSingle(
                "Compact Plural One",
                "CC",
                NumberFormatter.with().notation(Notation.compactLong()),
                ULocale.forLanguageTag("es"),
                1000000,
                "1 millón");

        assertFormatSingle(
                "Compact Plural Other",
                "CC",
                NumberFormatter.with().notation(Notation.compactLong()),
                ULocale.forLanguageTag("es"),
                2000000,
                "2 millones");

        assertFormatSingle(
                "Compact with Negative Sign",
                "C",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                -9876543.21,
                "-9.9M");

        assertFormatSingle(
                "Compact Rounding",
                "C",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                990000,
                "990K");

        assertFormatSingle(
                "Compact Rounding",
                "C",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                999000,
                "999K");

        assertFormatSingle(
                "Compact Rounding",
                "C",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                999900,
                "1M");

        assertFormatSingle(
                "Compact Rounding",
                "C",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                9900000,
                "9.9M");

        assertFormatSingle(
                "Compact Rounding",
                "C",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                9990000,
                "10M");
    }

    @Test
    public void unitMeasure() {
        assertFormatDescending(
                "Meters Short",
                "U:length:meter",
                NumberFormatter.with().unit(MeasureUnit.METER),
                ULocale.ENGLISH,
                "87,650 m",
                "8,765 m",
                "876.5 m",
                "87.65 m",
                "8.765 m",
                "0.8765 m",
                "0.08765 m",
                "0.008765 m",
                "0 m");

        assertFormatDescending(
                "Meters Long",
                "U:length:meter unit-width=FULL_NAME",
                NumberFormatter.with().unit(MeasureUnit.METER).unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                "87,650 meters",
                "8,765 meters",
                "876.5 meters",
                "87.65 meters",
                "8.765 meters",
                "0.8765 meters",
                "0.08765 meters",
                "0.008765 meters",
                "0 meters");

        assertFormatDescending(
                "Compact Meters Long",
                "CC U:length:meter unit-width=FULL_NAME",
                NumberFormatter.with().notation(Notation.compactLong()).unit(MeasureUnit.METER)
                        .unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                "88 thousand meters",
                "8.8 thousand meters",
                "876 meters",
                "88 meters",
                "8.8 meters",
                "0.88 meters",
                "0.088 meters",
                "0.0088 meters",
                "0 meters");

        assertFormatSingleMeasure(
                "Meters with Measure Input",
                "unit-width=FULL_NAME",
                NumberFormatter.with().unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                new Measure(5.43, MeasureUnit.METER),
                "5.43 meters");

        assertFormatSingleMeasure(
                "Measure format method takes precedence over fluent chain",
                "U:length:meter",
                NumberFormatter.with().unit(MeasureUnit.METER),
                ULocale.ENGLISH,
                new Measure(5.43, USD),
                "$5.43");

        assertFormatSingle(
                "Meters with Negative Sign",
                "U:length:meter",
                NumberFormatter.with().unit(MeasureUnit.METER),
                ULocale.ENGLISH,
                -9876543.21,
                "-9,876,543.21 m");

        // The locale string "सान" appears only in brx.txt:
        assertFormatSingle(
                "Interesting Data Fallback 1",
                "U:duration:day unit-width=FULL_NAME",
                NumberFormatter.with().unit(MeasureUnit.DAY).unitWidth(UnitWidth.FULL_NAME),
                ULocale.forLanguageTag("brx"),
                5.43,
                "5.43 सान");

        // Requires following the alias from unitsNarrow to unitsShort:
        assertFormatSingle(
                "Interesting Data Fallback 2",
                "U:duration:day unit-width=NARROW",
                NumberFormatter.with().unit(MeasureUnit.DAY).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("brx"),
                5.43,
                "5.43 d");

        // en_001.txt has a unitsNarrow/area/square-meter table, but table does not contain the OTHER unit,
        // requiring fallback to the root.
        assertFormatSingle(
                "Interesting Data Fallback 3",
                "U:area:square-meter unit-width=NARROW",
                NumberFormatter.with().unit(MeasureUnit.SQUARE_METER).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("en-GB"),
                5.43,
                "5.43 m²");
    }

    @Test
    public void unitCurrency() {
        assertFormatDescending(
                "Currency",
                "$GBP",
                NumberFormatter.with().unit(GBP),
                ULocale.ENGLISH,
                "£87,650.00",
                "£8,765.00",
                "£876.50",
                "£87.65",
                "£8.76",
                "£0.88",
                "£0.09",
                "£0.01",
                "£0.00");

        assertFormatDescending(
                "Currency ISO",
                "$GBP unit-width=ISO_CODE",
                NumberFormatter.with().unit(GBP).unitWidth(UnitWidth.ISO_CODE),
                ULocale.ENGLISH,
                "GBP 87,650.00",
                "GBP 8,765.00",
                "GBP 876.50",
                "GBP 87.65",
                "GBP 8.76",
                "GBP 0.88",
                "GBP 0.09",
                "GBP 0.01",
                "GBP 0.00");

        assertFormatDescending(
                "Currency Long Name",
                "$GBP unit-width=FULL_NAME",
                NumberFormatter.with().unit(GBP).unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                "87,650.00 British pounds",
                "8,765.00 British pounds",
                "876.50 British pounds",
                "87.65 British pounds",
                "8.76 British pounds",
                "0.88 British pounds",
                "0.09 British pounds",
                "0.01 British pounds",
                "0.00 British pounds");

        assertFormatSingleMeasure(
                "Currency with CurrencyAmount Input",
                "",
                NumberFormatter.with(),
                ULocale.ENGLISH,
                new CurrencyAmount(5.43, GBP),
                "£5.43");

        assertFormatSingle(
                "Currency Long Name from Pattern Syntax",
                "$GBP F0 grouping=none integer-width=1- symbols=loc:en sign=AUTO decimal=AUTO",
                NumberFormatter.fromDecimalFormat(
                        PatternStringParser.parseToProperties("0 ¤¤¤"),
                        DecimalFormatSymbols.getInstance(ULocale.ENGLISH),
                        null).unit(GBP),
                ULocale.ENGLISH,
                1234567.89,
                "1234568 British pounds");

        assertFormatSingle(
                "Currency with Negative Sign",
                "$GBP",
                NumberFormatter.with().unit(GBP),
                ULocale.ENGLISH,
                -9876543.21,
                "-£9,876,543.21");
    }

    @Test
    public void unitPercent() {
        assertFormatDescending(
                "Percent",
                "%",
                NumberFormatter.with().unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                "8,765,000%",
                "876,500%",
                "87,650%",
                "8,765%",
                "876.5%",
                "87.65%",
                "8.765%",
                "0.8765%",
                "0%");

        assertFormatDescending(
                "Permille",
                "%%",
                NumberFormatter.with().unit(NoUnit.PERMILLE),
                ULocale.ENGLISH,
                "87,650,000‰",
                "8,765,000‰",
                "876,500‰",
                "87,650‰",
                "8,765‰",
                "876.5‰",
                "87.65‰",
                "8.765‰",
                "0‰");

        assertFormatSingle(
                "NoUnit Base",
                "B",
                NumberFormatter.with().unit(NoUnit.BASE),
                ULocale.ENGLISH,
                51423,
                "51,423");

        assertFormatSingle(
                "Percent with Negative Sign",
                "%",
                NumberFormatter.with().unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                -0.987654321,
                "-98.7654321%");
    }

    @Test
    public void roundingFraction() {
        assertFormatDescending(
                "Integer",
                "F0",
                NumberFormatter.with().rounding(Rounder.integer()),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876",
                "88",
                "9",
                "1",
                "0",
                "0",
                "0");

        assertFormatDescending(
                "Fixed Fraction",
                "F3",
                NumberFormatter.with().rounding(Rounder.fixedFraction(3)),
                ULocale.ENGLISH,
                "87,650.000",
                "8,765.000",
                "876.500",
                "87.650",
                "8.765",
                "0.876",
                "0.088",
                "0.009",
                "0.000");

        assertFormatDescending(
                "Min Fraction",
                "F1-",
                NumberFormatter.with().rounding(Rounder.minFraction(1)),
                ULocale.ENGLISH,
                "87,650.0",
                "8,765.0",
                "876.5",
                "87.65",
                "8.765",
                "0.8765",
                "0.08765",
                "0.008765",
                "0.0");

        assertFormatDescending(
                "Max Fraction",
                "F-1",
                NumberFormatter.with().rounding(Rounder.maxFraction(1)),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876.5",
                "87.6",
                "8.8",
                "0.9",
                "0.1",
                "0",
                "0");

        assertFormatDescending(
                "Min/Max Fraction",
                "F1-3",
                NumberFormatter.with().rounding(Rounder.minMaxFraction(1, 3)),
                ULocale.ENGLISH,
                "87,650.0",
                "8,765.0",
                "876.5",
                "87.65",
                "8.765",
                "0.876",
                "0.088",
                "0.009",
                "0.0");
    }

    @Test
    public void roundingFigures() {
        assertFormatSingle(
                "Fixed Significant",
                "S3",
                NumberFormatter.with().rounding(Rounder.fixedDigits(3)),
                ULocale.ENGLISH,
                -98,
                "-98.0");

        assertFormatSingle(
                "Fixed Significant Rounding",
                "S3",
                NumberFormatter.with().rounding(Rounder.fixedDigits(3)),
                ULocale.ENGLISH,
                -98.7654321,
                "-98.8");

        assertFormatSingle(
                "Fixed Significant Zero",
                "S3",
                NumberFormatter.with().rounding(Rounder.fixedDigits(3)),
                ULocale.ENGLISH,
                0,
                "0.00");

        assertFormatSingle(
                "Min Significant",
                "S2-",
                NumberFormatter.with().rounding(Rounder.minDigits(2)),
                ULocale.ENGLISH,
                -9,
                "-9.0");

        assertFormatSingle(
                "Max Significant",
                "S-4",
                NumberFormatter.with().rounding(Rounder.maxDigits(4)),
                ULocale.ENGLISH,
                98.7654321,
                "98.77");

        assertFormatSingle(
                "Min/Max Significant",
                "S3-4",
                NumberFormatter.with().rounding(Rounder.minMaxDigits(3, 4)),
                ULocale.ENGLISH,
                9.99999,
                "10.0");
    }

    @Test
    public void roundingFractionFigures() {
        assertFormatDescending(
                "Basic Significant", // for comparison
                "S-2",
                NumberFormatter.with().rounding(Rounder.maxDigits(2)),
                ULocale.ENGLISH,
                "88,000",
                "8,800",
                "880",
                "88",
                "8.8",
                "0.88",
                "0.088",
                "0.0088",
                "0");

        assertFormatDescending(
                "FracSig minMaxFrac minSig",
                "F1-2>3",
                NumberFormatter.with().rounding(Rounder.minMaxFraction(1, 2).withMinDigits(3)),
                ULocale.ENGLISH,
                "87,650.0",
                "8,765.0",
                "876.5",
                "87.65",
                "8.76",
                "0.876", // minSig beats maxFrac
                "0.0876", // minSig beats maxFrac
                "0.00876", // minSig beats maxFrac
                "0.0");

        assertFormatDescending(
                "FracSig minMaxFrac maxSig A",
                "F1-3<2",
                NumberFormatter.with().rounding(Rounder.minMaxFraction(1, 3).withMaxDigits(2)),
                ULocale.ENGLISH,
                "88,000.0", // maxSig beats maxFrac
                "8,800.0", // maxSig beats maxFrac
                "880.0", // maxSig beats maxFrac
                "88.0", // maxSig beats maxFrac
                "8.8", // maxSig beats maxFrac
                "0.88", // maxSig beats maxFrac
                "0.088",
                "0.009",
                "0.0");

        assertFormatDescending(
                "FracSig minMaxFrac maxSig B",
                "F2<2",
                NumberFormatter.with().rounding(Rounder.fixedFraction(2).withMaxDigits(2)),
                ULocale.ENGLISH,
                "88,000.00", // maxSig beats maxFrac
                "8,800.00", // maxSig beats maxFrac
                "880.00", // maxSig beats maxFrac
                "88.00", // maxSig beats maxFrac
                "8.80", // maxSig beats maxFrac
                "0.88",
                "0.09",
                "0.01",
                "0.00");
    }

    @Test
    public void roundingOther() {
        assertFormatDescending(
                "Rounding None",
                "Y",
                NumberFormatter.with().rounding(Rounder.unlimited()),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                "0.8765",
                "0.08765",
                "0.008765",
                "0");

        assertFormatDescending(
                "Increment",
                "M0.5",
                NumberFormatter.with().rounding(Rounder.increment(BigDecimal.valueOf(0.5))),
                ULocale.ENGLISH,
                "87,650.0",
                "8,765.0",
                "876.5",
                "87.5",
                "9.0",
                "1.0",
                "0.0",
                "0.0",
                "0.0");

        assertFormatDescending(
                "Currency Standard",
                "$CZK GSTANDARD",
                NumberFormatter.with().rounding(Rounder.currency(CurrencyUsage.STANDARD)).unit(CZK),
                ULocale.ENGLISH,
                "CZK 87,650.00",
                "CZK 8,765.00",
                "CZK 876.50",
                "CZK 87.65",
                "CZK 8.76",
                "CZK 0.88",
                "CZK 0.09",
                "CZK 0.01",
                "CZK 0.00");

        assertFormatDescending(
                "Currency Cash",
                "$CZK GCASH",
                NumberFormatter.with().rounding(Rounder.currency(CurrencyUsage.CASH)).unit(CZK),
                ULocale.ENGLISH,
                "CZK 87,650",
                "CZK 8,765",
                "CZK 876",
                "CZK 88",
                "CZK 9",
                "CZK 1",
                "CZK 0",
                "CZK 0",
                "CZK 0");

        assertFormatDescending(
                "Currency not in top-level fluent chain",
                "F0",
                NumberFormatter.with().rounding(Rounder.currency(CurrencyUsage.CASH).withCurrency(CZK)),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876",
                "88",
                "9",
                "1",
                "0",
                "0",
                "0");
    }

    @Test
    public void grouping() {
        // NoUnit.PERMILLE multiplies all the number by 10^3 (good for testing grouping).
        // Note that en-US is already performed in the unitPercent() function.
        assertFormatDescending(
                "Indic Grouping",
                "%% grouping=defaults",
                NumberFormatter.with().unit(NoUnit.PERMILLE).grouping(Grouper.defaults()),
                new ULocale("en-IN"),
                "8,76,50,000‰",
                "87,65,000‰",
                "8,76,500‰",
                "87,650‰",
                "8,765‰",
                "876.5‰",
                "87.65‰",
                "8.765‰",
                "0‰");

        assertFormatDescending(
                "Western Grouping, Min 2",
                "%% grouping=min2",
                NumberFormatter.with().unit(NoUnit.PERMILLE).grouping(Grouper.min2()),
                ULocale.ENGLISH,
                "87,650,000‰",
                "8,765,000‰",
                "876,500‰",
                "87,650‰",
                "8765‰",
                "876.5‰",
                "87.65‰",
                "8.765‰",
                "0‰");

        assertFormatDescending(
                "Indic Grouping, Min 2",
                "%% grouping=min2",
                NumberFormatter.with().unit(NoUnit.PERMILLE).grouping(Grouper.min2()),
                new ULocale("en-IN"),
                "8,76,50,000‰",
                "87,65,000‰",
                "8,76,500‰",
                "87,650‰",
                "8765‰",
                "876.5‰",
                "87.65‰",
                "8.765‰",
                "0‰");

        assertFormatDescending(
                "No Grouping",
                "%% grouping=none",
                NumberFormatter.with().unit(NoUnit.PERMILLE).grouping(Grouper.none()),
                new ULocale("en-IN"),
                "87650000‰",
                "8765000‰",
                "876500‰",
                "87650‰",
                "8765‰",
                "876.5‰",
                "87.65‰",
                "8.765‰",
                "0‰");
    }

    @Test
    public void padding() {
        assertFormatDescending(
                "Padding",
                "",
                NumberFormatter.with().padding(Padder.none()),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                "0.8765",
                "0.08765",
                "0.008765",
                "0");

        assertFormatDescending(
                "Padding",
                "",
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.AFTER_PREFIX)),
                ULocale.ENGLISH,
                "**87,650",
                "***8,765",
                "***876.5",
                "***87.65",
                "***8.765",
                "**0.8765",
                "*0.08765",
                "0.008765",
                "*******0");

        assertFormatDescending(
                "Padding with code points",
                "",
                NumberFormatter.with().padding(Padder.codePoints(0x101E4, 8, PadPosition.AFTER_PREFIX)),
                ULocale.ENGLISH,
                "𐇤𐇤87,650",
                "𐇤𐇤𐇤8,765",
                "𐇤𐇤𐇤876.5",
                "𐇤𐇤𐇤87.65",
                "𐇤𐇤𐇤8.765",
                "𐇤𐇤0.8765",
                "𐇤0.08765",
                "0.008765",
                "𐇤𐇤𐇤𐇤𐇤𐇤𐇤0");

        assertFormatDescending(
                "Padding with wide digits",
                "symbols=ns:mathsanb",
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.AFTER_PREFIX))
                        .symbols(NumberingSystem.getInstanceByName("mathsanb")),
                ULocale.ENGLISH,
                "**𝟴𝟳,𝟲𝟱𝟬",
                "***𝟴,𝟳𝟲𝟱",
                "***𝟴𝟳𝟲.𝟱",
                "***𝟴𝟳.𝟲𝟱",
                "***𝟴.𝟳𝟲𝟱",
                "**𝟬.𝟴𝟳𝟲𝟱",
                "*𝟬.𝟬𝟴𝟳𝟲𝟱",
                "𝟬.𝟬𝟬𝟴𝟳𝟲𝟱",
                "*******𝟬");

        assertFormatDescending(
                "Padding with currency spacing",
                "$GBP unit-width=ISO_CODE",
                NumberFormatter.with().padding(Padder.codePoints('*', 10, PadPosition.AFTER_PREFIX)).unit(GBP)
                        .unitWidth(UnitWidth.ISO_CODE),
                ULocale.ENGLISH,
                "GBP 87,650.00",
                "GBP 8,765.00",
                "GBP*876.50",
                "GBP**87.65",
                "GBP***8.76",
                "GBP***0.88",
                "GBP***0.09",
                "GBP***0.01",
                "GBP***0.00");

        assertFormatSingle(
                "Pad Before Prefix",
                "",
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.BEFORE_PREFIX)),
                ULocale.ENGLISH,
                -88.88,
                "**-88.88");

        assertFormatSingle(
                "Pad After Prefix",
                "",
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.AFTER_PREFIX)),
                ULocale.ENGLISH,
                -88.88,
                "-**88.88");

        assertFormatSingle(
                "Pad Before Suffix",
                "%",
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.BEFORE_SUFFIX))
                        .unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                0.8888,
                "88.88**%");

        assertFormatSingle(
                "Pad After Suffix",
                "%",
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.AFTER_SUFFIX))
                        .unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                0.8888,
                "88.88%**");
    }

    @Test
    public void integerWidth() {
        assertFormatDescending(
                "Integer Width Default",
                "integer-width=1-",
                NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(1)),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                "0.8765",
                "0.08765",
                "0.008765",
                "0");

        assertFormatDescending(
                "Integer Width Zero Fill 0",
                "integer-width=0-",
                NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(0)),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                ".8765",
                ".08765",
                ".008765",
                ""); // TODO: Avoid the empty string here?

        assertFormatDescending(
                "Integer Width Zero Fill 3",
                "integer-width=3-",
                NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(3)),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876.5",
                "087.65",
                "008.765",
                "000.8765",
                "000.08765",
                "000.008765",
                "000");

        assertFormatDescending(
                "Integer Width Max 3",
                "integer-width=1-3",
                NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(1).truncateAt(3)),
                ULocale.ENGLISH,
                "650",
                "765",
                "876.5",
                "87.65",
                "8.765",
                "0.8765",
                "0.08765",
                "0.008765",
                "0");

        assertFormatDescending(
                "Integer Width Fixed 2",
                "integer-width=2",
                NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(2).truncateAt(2)),
                ULocale.ENGLISH,
                "50",
                "65",
                "76.5",
                "87.65",
                "08.765",
                "00.8765",
                "00.08765",
                "00.008765",
                "00");
    }

    @Test
    public void symbols() {
        assertFormatDescending(
                "French Symbols with Japanese Data 1",
                "symbols=loc:fr",
                NumberFormatter.with().symbols(DecimalFormatSymbols.getInstance(ULocale.FRENCH)),
                ULocale.JAPAN,
                "87 650",
                "8 765",
                "876,5",
                "87,65",
                "8,765",
                "0,8765",
                "0,08765",
                "0,008765",
                "0");

        assertFormatSingle(
                "French Symbols with Japanese Data 2",
                "C symbols=loc:fr",
                NumberFormatter.with().notation(Notation.compactShort())
                        .symbols(DecimalFormatSymbols.getInstance(ULocale.FRENCH)),
                ULocale.JAPAN,
                12345,
                "1,2\u4E07");

        assertFormatDescending(
                "Latin Numbering System with Arabic Data",
                "$USD symbols=ns:latn",
                NumberFormatter.with().symbols(NumberingSystem.LATIN).unit(USD),
                new ULocale("ar"),
                "87,650.00 US$",
                "8,765.00 US$",
                "876.50 US$",
                "87.65 US$",
                "8.76 US$",
                "0.88 US$",
                "0.09 US$",
                "0.01 US$",
                "0.00 US$");

        assertFormatDescending(
                "Math Numbering System with French Data",
                "symbols=ns:mathsanb",
                NumberFormatter.with().symbols(NumberingSystem.getInstanceByName("mathsanb")),
                ULocale.FRENCH,
                "𝟴𝟳 𝟲𝟱𝟬",
                "𝟴 𝟳𝟲𝟱",
                "𝟴𝟳𝟲,𝟱",
                "𝟴𝟳,𝟲𝟱",
                "𝟴,𝟳𝟲𝟱",
                "𝟬,𝟴𝟳𝟲𝟱",
                "𝟬,𝟬𝟴𝟳𝟲𝟱",
                "𝟬,𝟬𝟬𝟴𝟳𝟲𝟱",
                "𝟬");

        assertFormatSingle(
                "Swiss Symbols (used in documentation)",
                "symbols=loc:de_CH",
                NumberFormatter.with().symbols(DecimalFormatSymbols.getInstance(new ULocale("de-CH"))),
                ULocale.ENGLISH,
                12345.67,
                "12’345.67");

        assertFormatSingle(
                "Myanmar Symbols (used in documentation)",
                "symbols=loc:my_MY",
                NumberFormatter.with().symbols(DecimalFormatSymbols.getInstance(new ULocale("my_MY"))),
                ULocale.ENGLISH,
                12345.67,
                "\u1041\u1042,\u1043\u1044\u1045.\u1046\u1047");

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new ULocale("de-CH"));
        UnlocalizedNumberFormatter f = NumberFormatter.with().symbols(symbols);
        symbols.setGroupingSeparatorString("!");
        assertFormatSingle(
                "Symbols object should be copied",
                "symbols=loc:de_CH",
                f,
                ULocale.ENGLISH,
                12345.67,
                "12’345.67");

        assertFormatSingle(
                "The last symbols setter wins",
                "symbols=ns:latn",
                NumberFormatter.with().symbols(symbols).symbols(NumberingSystem.LATIN),
                ULocale.ENGLISH,
                12345.67,
                "12,345.67");

        assertFormatSingle(
                "The last symbols setter wins",
                "symbols=loc:de_CH",
                NumberFormatter.with().symbols(NumberingSystem.LATIN).symbols(symbols),
                ULocale.ENGLISH,
                12345.67,
                "12!345.67");
    }

    @Test
    @Ignore("This feature is not currently available.")
    public void symbolsOverride() {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        dfs.setCurrencySymbol("@");
        dfs.setInternationalCurrencySymbol("foo");
        assertFormatSingle(
                "Custom Short Currency Symbol",
                "$XXX",
                NumberFormatter.with().unit(Currency.getInstance("XXX")).symbols(dfs),
                ULocale.ENGLISH,
                12.3,
                "@ 12.30");
    }

    @Test
    public void sign() {
        assertFormatSingle(
                "Sign Auto Positive",
                "sign=AUTO",
                NumberFormatter.with().sign(SignDisplay.AUTO),
                ULocale.ENGLISH,
                444444,
                "444,444");

        assertFormatSingle(
                "Sign Auto Negative",
                "sign=AUTO",
                NumberFormatter.with().sign(SignDisplay.AUTO),
                ULocale.ENGLISH,
                -444444,
                "-444,444");

        assertFormatSingle(
                "Sign Always Positive",
                "sign=ALWAYS",
                NumberFormatter.with().sign(SignDisplay.ALWAYS),
                ULocale.ENGLISH,
                444444,
                "+444,444");

        assertFormatSingle(
                "Sign Always Negative",
                "sign=ALWAYS",
                NumberFormatter.with().sign(SignDisplay.ALWAYS),
                ULocale.ENGLISH,
                -444444,
                "-444,444");

        assertFormatSingle(
                "Sign Never Positive",
                "sign=NEVER",
                NumberFormatter.with().sign(SignDisplay.NEVER),
                ULocale.ENGLISH,
                444444,
                "444,444");

        assertFormatSingle(
                "Sign Never Negative",
                "sign=NEVER",
                NumberFormatter.with().sign(SignDisplay.NEVER),
                ULocale.ENGLISH,
                -444444,
                "444,444");

        assertFormatSingle(
                "Sign Accounting Positive",
                "$USD sign=ACCOUNTING",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD),
                ULocale.ENGLISH,
                444444,
                "$444,444.00");

        assertFormatSingle(
                "Sign Accounting Negative",
                "$USD sign=ACCOUNTING",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD),
                ULocale.ENGLISH,
                -444444,
                "($444,444.00)");

        assertFormatSingle(
                "Sign Accounting-Always Positive",
                "$USD sign=ACCOUNTING_ALWAYS",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_ALWAYS).unit(USD),
                ULocale.ENGLISH,
                444444,
                "+$444,444.00");

        assertFormatSingle(
                "Sign Accounting-Always Negative",
                "$USD sign=ACCOUNTING_ALWAYS",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_ALWAYS).unit(USD),
                ULocale.ENGLISH,
                -444444,
                "($444,444.00)");
    }

    @Test
    public void decimal() {
        assertFormatDescending(
                "Decimal Default",
                "decimal=AUTO",
                NumberFormatter.with().decimal(DecimalMarkDisplay.AUTO),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                "0.8765",
                "0.08765",
                "0.008765",
                "0");

        assertFormatDescending(
                "Decimal Always Shown",
                "decimal=ALWAYS",
                NumberFormatter.with().decimal(DecimalMarkDisplay.ALWAYS),
                ULocale.ENGLISH,
                "87,650.",
                "8,765.",
                "876.5",
                "87.65",
                "8.765",
                "0.8765",
                "0.08765",
                "0.008765",
                "0.");
    }

    @Test
    public void locale() {
        // Coverage for the locale setters.
        assertEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.with().locale(Locale.ENGLISH));
        assertNotEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.with().locale(Locale.FRENCH));
    }

    @Test
    public void getPrefixSuffix() {
        Object[][] cases = {
                { NumberFormatter.withLocale(ULocale.ENGLISH).unit(GBP).unitWidth(UnitWidth.ISO_CODE), "GBP", "",
                        "-GBP", "" },
                { NumberFormatter.withLocale(ULocale.ENGLISH).unit(GBP).unitWidth(UnitWidth.FULL_NAME), "",
                        " British pounds", "-", " British pounds" } };

        for (Object[] cas : cases) {
            LocalizedNumberFormatter f = (LocalizedNumberFormatter) cas[0];
            String posPrefix = (String) cas[1];
            String posSuffix = (String) cas[2];
            String negPrefix = (String) cas[3];
            String negSuffix = (String) cas[4];
            FormattedNumber positive = f.format(1);
            FormattedNumber negative = f.format(-1);
            assertEquals(posPrefix, positive.getPrefix());
            assertEquals(posSuffix, positive.getSuffix());
            assertEquals(negPrefix, negative.getPrefix());
            assertEquals(negSuffix, negative.getSuffix());
        }
    }

    @Test
    public void plurals() {
        // TODO: Expand this test.

        assertFormatSingle(
                "Plural 1",
                "$USD F0 unit-width=FULL_NAME",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.FULL_NAME).rounding(Rounder.fixedFraction(0)),
                ULocale.ENGLISH,
                1,
                "1 US dollar");

        assertFormatSingle(
                "Plural 1.00",
                "$USD F2 unit-width=FULL_NAME",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.FULL_NAME).rounding(Rounder.fixedFraction(2)),
                ULocale.ENGLISH,
                1,
                "1.00 US dollars");
    }

    private static void assertFormatDescending(
            String message,
            String skeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            String... expected) {
        assert expected.length == 9;
        assertEquals(message + ": Skeleton:", skeleton, f.toSkeleton());
        final double[] inputs = new double[] { 87650, 8765, 876.5, 87.65, 8.765, 0.8765, 0.08765, 0.008765, 0 };
        LocalizedNumberFormatter l1 = f.threshold(0L).locale(locale); // no self-regulation
        LocalizedNumberFormatter l2 = f.threshold(1L).locale(locale); // all self-regulation
        for (int i = 0; i < 9; i++) {
            double d = inputs[i];
            String actual1 = l1.format(d).toString();
            assertEquals(message + ": L1: " + d, expected[i], actual1);
            String actual2 = l2.format(d).toString();
            assertEquals(message + ": L2: " + d, expected[i], actual2);
        }
    }

    private static void assertFormatSingle(
            String message,
            String skeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            Number input,
            String expected) {
        assertEquals(message + ": Skeleton:", skeleton, f.toSkeleton());
        LocalizedNumberFormatter l1 = f.threshold(0L).locale(locale); // no self-regulation
        LocalizedNumberFormatter l2 = f.threshold(1L).locale(locale); // all self-regulation
        String actual1 = l1.format(input).toString();
        assertEquals(message + ": Unsafe Path: " + input, expected, actual1);
        String actual2 = l2.format(input).toString();
        assertEquals(message + ": Safe Path: " + input, expected, actual2);
    }

    private static void assertFormatSingleMeasure(
            String message,
            String skeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            Measure input,
            String expected) {
        assertEquals(message + ": Skeleton:", skeleton, f.toSkeleton());
        LocalizedNumberFormatter l1 = f.threshold(0L).locale(locale); // no self-regulation
        LocalizedNumberFormatter l2 = f.threshold(1L).locale(locale); // all self-regulation
        String actual1 = l1.format(input).toString();
        assertEquals(message + ": Unsafe Path: " + input, expected, actual1);
        String actual2 = l2.format(input).toString();
        assertEquals(message + ": Safe Path: " + input, expected, actual2);
    }
}
