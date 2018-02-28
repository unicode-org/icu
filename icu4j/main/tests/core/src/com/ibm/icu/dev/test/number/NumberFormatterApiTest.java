// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.Padder;
import com.ibm.icu.impl.number.Padder.PadPosition;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.number.CompactNotation;
import com.ibm.icu.number.FractionRounder;
import com.ibm.icu.number.IntegerWidth;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.Rounder;
import com.ibm.icu.number.ScientificNotation;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.ULocale;

public class NumberFormatterApiTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency CZK = Currency.getInstance("CZK");
    private static final Currency CAD = Currency.getInstance("CAD");
    private static final Currency ESP = Currency.getInstance("ESP");
    private static final Currency PTE = Currency.getInstance("PTE");

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

        assertFormatDescendingBig(
                "Big Simple",
                "",
                NumberFormatter.with().notation(Notation.simple()),
                ULocale.ENGLISH,
                "87,650,000",
                "8,765,000",
                "876,500",
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
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
        assertFormatDescendingBig(
                "Compact Short",
                "C",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                "88M",
                "8.8M",
                "876K",
                "88K",
                "8.8K",
                "876",
                "88",
                "8.8",
                "0");

        assertFormatDescendingBig(
                "Compact Long",
                "CC",
                NumberFormatter.with().notation(Notation.compactLong()),
                ULocale.ENGLISH,
                "88 million",
                "8.8 million",
                "876 thousand",
                "88 thousand",
                "8.8 thousand",
                "876",
                "88",
                "8.8",
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
                NumberFormatter.with().notation(Notation.compactShort()).unit(USD).unitWidth(UnitWidth.ISO_CODE),
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
                NumberFormatter.with().notation(Notation.compactShort()).unit(USD).unitWidth(UnitWidth.FULL_NAME),
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
                NumberFormatter.with().notation(Notation.compactLong()).unit(USD).unitWidth(UnitWidth.ISO_CODE),
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
                NumberFormatter.with().notation(Notation.compactLong()).unit(USD).unitWidth(UnitWidth.FULL_NAME),
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

        Map<String, Map<String, String>> compactCustomData = new HashMap<String, Map<String, String>>();
        Map<String, String> entry = new HashMap<String, String>();
        entry.put("one", "Kun");
        entry.put("other", "0KK");
        compactCustomData.put("1000", entry);
        assertFormatSingle(
                "Compact Somali No Figure",
                "",
                NumberFormatter.with().notation(CompactNotation.forCustomData(compactCustomData)),
                ULocale.ENGLISH,
                1000,
                "Kun");
    }

    @Test
    public void unitMeasure() {
        assertFormatDescending(
                "Meters Short",
                "measure-unit/length-meter",
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
                "measure-unit/length-meter unit-width-full-name",
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
                "compact-long measure-unit/length-meter unit-width-full-name",
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

        // es_US has "{0}°" for unitsNarrow/temperature/FAHRENHEIT.
        // NOTE: This example is in the documentation.
        assertFormatSingle(
                "MeasureUnit Difference between Narrow and Short (Narrow Version)",
                "",
                NumberFormatter.with().unit(MeasureUnit.FAHRENHEIT).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("es-US"),
                5.43,
                "5.43°");

        assertFormatSingle(
                "MeasureUnit Difference between Narrow and Short (Short Version)",
                "",
                NumberFormatter.with().unit(MeasureUnit.FAHRENHEIT).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("es-US"),
                5.43,
                "5.43 °F");

        assertFormatSingle(
                "MeasureUnit form without {0} in CLDR pattern",
                "",
                NumberFormatter.with().unit(MeasureUnit.KELVIN).unitWidth(UnitWidth.FULL_NAME),
                ULocale.forLanguageTag("es-MX"),
                1,
                "kelvin");

        assertFormatSingle(
                "MeasureUnit form without {0} in CLDR pattern and wide base form",
                "",
                NumberFormatter.with()
                    .rounding(Rounder.fixedFraction(20))
                    .unit(MeasureUnit.KELVIN)
                    .unitWidth(UnitWidth.FULL_NAME),
                ULocale.forLanguageTag("es-MX"),
                1,
                "kelvin");
    }

    @Test
    public void unitCompoundMeasure() {
        assertFormatDescending(
                "Meters Per Second Short (unit that simplifies)",
                "",
                NumberFormatter.with().unit(MeasureUnit.METER).perUnit(MeasureUnit.SECOND),
                ULocale.ENGLISH,
                "87,650 m/s",
                "8,765 m/s",
                "876.5 m/s",
                "87.65 m/s",
                "8.765 m/s",
                "0.8765 m/s",
                "0.08765 m/s",
                "0.008765 m/s",
                "0 m/s");

        assertFormatDescending(
                "Pounds Per Square Mile Short (secondary unit has per-format)",
                "",
                NumberFormatter.with().unit(MeasureUnit.POUND).perUnit(MeasureUnit.SQUARE_MILE),
                ULocale.ENGLISH,
                "87,650 lb/mi²",
                "8,765 lb/mi²",
                "876.5 lb/mi²",
                "87.65 lb/mi²",
                "8.765 lb/mi²",
                "0.8765 lb/mi²",
                "0.08765 lb/mi²",
                "0.008765 lb/mi²",
                "0 lb/mi²");

        assertFormatDescending(
                "Joules Per Furlong Short (unit with no simplifications or special patterns)",
                "",
                NumberFormatter.with().unit(MeasureUnit.JOULE).perUnit(MeasureUnit.FURLONG),
                ULocale.ENGLISH,
                "87,650 J/fur",
                "8,765 J/fur",
                "876.5 J/fur",
                "87.65 J/fur",
                "8.765 J/fur",
                "0.8765 J/fur",
                "0.08765 J/fur",
                "0.008765 J/fur",
                "0 J/fur");
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

        assertFormatDescending(
                "Currency Hidden",
                "$GBP unit-width=HIDDEN",
                NumberFormatter.with().unit(GBP).unitWidth(UnitWidth.HIDDEN),
                ULocale.ENGLISH,
                "87,650.00",
                "8,765.00",
                "876.50",
                "87.65",
                "8.76",
                "0.88",
                "0.09",
                "0.01",
                "0.00");

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

        // The full currency symbol is not shown in NARROW format.
        // NOTE: This example is in the documentation.
        assertFormatSingle(
                "Currency Difference between Narrow and Short (Narrow Version)",
                "",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("en-CA"),
                5.43,
                "$5.43");

        assertFormatSingle(
                "Currency Difference between Narrow and Short (Short Version)",
                "",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("en-CA"),
                5.43,
                "US$5.43");

        assertFormatSingle(
                "Currency-dependent format (Control)",
                "",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("ca"),
                444444.55,
                "444.444,55 USD");

        assertFormatSingle(
                "Currency-dependent format (Test)",
                "",
                NumberFormatter.with().unit(ESP).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("ca"),
                444444.55,
                "₧ 444.445");

        assertFormatSingle(
                "Currency-dependent symbols (Control)",
                "",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("pt-PT"),
                444444.55,
                "444 444,55 US$");

        // NOTE: This is a bit of a hack on CLDR's part. They set the currency symbol to U+200B (zero-
        // width space), and they set the decimal separator to the $ symbol.
        assertFormatSingle(
                "Currency-dependent symbols (Test)",
                "",
                NumberFormatter.with().unit(PTE).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("pt-PT"),
                444444.55,
                "444,444$55 \u200B");

        assertFormatSingle(
                "Currency-dependent symbols (Test)",
                "",
                NumberFormatter.with().unit(PTE).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("pt-PT"),
                444444.55,
                "444,444$55 PTE");

        assertFormatSingle(
                "Currency-dependent symbols (Test)",
                "",
                NumberFormatter.with().unit(PTE).unitWidth(UnitWidth.ISO_CODE),
                ULocale.forLanguageTag("pt-PT"),
                444444.55,
                "444,444$55 PTE");
    }

    @Test
    public void unitPercent() {
        assertFormatDescending(
                "Percent",
                "%",
                NumberFormatter.with().unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                "87,650%",
                "8,765%",
                "876.5%",
                "87.65%",
                "8.765%",
                "0.8765%",
                "0.08765%",
                "0.008765%",
                "0%");

        assertFormatDescending(
                "Permille",
                "%%",
                NumberFormatter.with().unit(NoUnit.PERMILLE),
                ULocale.ENGLISH,
                "87,650‰",
                "8,765‰",
                "876.5‰",
                "87.65‰",
                "8.765‰",
                "0.8765‰",
                "0.08765‰",
                "0.008765‰",
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
                -98.7654321,
                "-98.765432%");
    }

    @Test
    public void roundingFraction() {
        assertFormatDescending(
                "Integer",
                "round-integer",
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
                ".000",
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
                ".0+",
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
                ".#",
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
                ".0##",
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
                "@@@",
                NumberFormatter.with().rounding(Rounder.fixedDigits(3)),
                ULocale.ENGLISH,
                -98,
                "-98.0");

        assertFormatSingle(
                "Fixed Significant Rounding",
                "@@@",
                NumberFormatter.with().rounding(Rounder.fixedDigits(3)),
                ULocale.ENGLISH,
                -98.7654321,
                "-98.8");

        assertFormatSingle(
                "Fixed Significant Zero",
                "@@@",
                NumberFormatter.with().rounding(Rounder.fixedDigits(3)),
                ULocale.ENGLISH,
                0,
                "0.00");

        assertFormatSingle(
                "Min Significant",
                "@@+",
                NumberFormatter.with().rounding(Rounder.minDigits(2)),
                ULocale.ENGLISH,
                -9,
                "-9.0");

        assertFormatSingle(
                "Max Significant",
                "@###",
                NumberFormatter.with().rounding(Rounder.maxDigits(4)),
                ULocale.ENGLISH,
                98.7654321,
                "98.77");

        assertFormatSingle(
                "Min/Max Significant",
                "@@@#",
                NumberFormatter.with().rounding(Rounder.minMaxDigits(3, 4)),
                ULocale.ENGLISH,
                9.99999,
                "10.0");
    }

    @Test
    public void roundingFractionFigures() {
        assertFormatDescending(
                "Basic Significant", // for comparison
                "@#",
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
                ".0#/@@@+",
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
                ".0##/@#",
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
                ".00/@#",
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

        assertFormatSingle(
                "FracSig with trailing zeros A",
                ".00/@@@+",
                NumberFormatter.with().rounding(Rounder.fixedFraction(2).withMinDigits(3)),
                ULocale.ENGLISH,
                0.1,
                "0.10");

        assertFormatSingle(
                "FracSig with trailing zeros B",
                ".00/@@@+",
                NumberFormatter.with().rounding(Rounder.fixedFraction(2).withMinDigits(3)),
                ULocale.ENGLISH,
                0.0999999,
                "0.10");
    }

    @Test
    public void roundingOther() {
        assertFormatDescending(
                "Rounding None",
                "round-unlimited",
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
                "round-increment/0.5",
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
                "Increment with Min Fraction",
                "round-increment/0.50",
                NumberFormatter.with().rounding(Rounder.increment(new BigDecimal("0.50"))),
                ULocale.ENGLISH,
                "87,650.00",
                "8,765.00",
                "876.50",
                "87.50",
                "9.00",
                "1.00",
                "0.00",
                "0.00",
                "0.00");

        assertFormatDescending(
                "Currency Standard",
                "round-currency-standard",
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
                "round-currency-cash",
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
                "Currency Cash with Nickel Rounding",
                "round-currency-cash",
                NumberFormatter.with().rounding(Rounder.currency(CurrencyUsage.CASH)).unit(CAD),
                ULocale.ENGLISH,
                "CA$87,650.00",
                "CA$8,765.00",
                "CA$876.50",
                "CA$87.65",
                "CA$8.75",
                "CA$0.90",
                "CA$0.10",
                "CA$0.00",
                "CA$0.00");

        assertFormatDescending(
                "Currency not in top-level fluent chain",
                "round-currency-cash/CZK",
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

        // NOTE: Other tests cover the behavior of the other rounding modes.
        assertFormatDescending(
                "round-integer/CEILING",
                "",
                NumberFormatter.with().rounding(Rounder.integer().withMode(RoundingMode.CEILING)),
                ULocale.ENGLISH,
                "87,650",
                "8,765",
                "877",
                "88",
                "9",
                "1",
                "1",
                "1",
                "0");
    }

    @Test
    public void grouping() {
        assertFormatDescendingBig(
                "Western Grouping",
                "grouping=defaults",
                NumberFormatter.with().grouping(GroupingStrategy.AUTO),
                ULocale.ENGLISH,
                "87,650,000",
                "8,765,000",
                "876,500",
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                "0");

        assertFormatDescendingBig(
                "Indic Grouping",
                "grouping=defaults",
                NumberFormatter.with().grouping(GroupingStrategy.AUTO),
                new ULocale("en-IN"),
                "8,76,50,000",
                "87,65,000",
                "8,76,500",
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                "0");

        assertFormatDescendingBig(
                "Western Grouping, Min 2",
                "grouping=min2",
                NumberFormatter.with().grouping(GroupingStrategy.MIN2),
                ULocale.ENGLISH,
                "87,650,000",
                "8,765,000",
                "876,500",
                "87,650",
                "8765",
                "876.5",
                "87.65",
                "8.765",
                "0");

        assertFormatDescendingBig(
                "Indic Grouping, Min 2",
                "grouping=min2",
                NumberFormatter.with().grouping(GroupingStrategy.MIN2),
                new ULocale("en-IN"),
                "8,76,50,000",
                "87,65,000",
                "8,76,500",
                "87,650",
                "8765",
                "876.5",
                "87.65",
                "8.765",
                "0");

        assertFormatDescendingBig(
                "No Grouping",
                "grouping=none",
                NumberFormatter.with().grouping(GroupingStrategy.OFF),
                new ULocale("en-IN"),
                "87650000",
                "8765000",
                "876500",
                "87650",
                "8765",
                "876.5",
                "87.65",
                "8.765",
                "0");

        assertFormatDescendingBig(
                "Indic locale with THOUSANDS grouping",
                "",
                NumberFormatter.with().grouping(GroupingStrategy.THOUSANDS),
                new ULocale("en-IN"),
                "87,650,000",
                "8,765,000",
                "876,500",
                "87,650",
                "8,765",
                "876.5",
                "87.65",
                "8.765",
                "0");

        // NOTE: Hungarian is interesting because it has minimumGroupingDigits=4 in locale data
        // If this test breaks due to data changes, find another locale that has minimumGroupingDigits.
        assertFormatDescendingBig(
                "Hungarian Grouping",
                "",
                NumberFormatter.with().grouping(GroupingStrategy.AUTO),
                new ULocale("hu"),
                "87 650 000",
                "8 765 000",
                "876500",
                "87650",
                "8765",
                "876,5",
                "87,65",
                "8,765",
                "0");

        assertFormatDescendingBig(
                "Hungarian Grouping, Min 2",
                "",
                NumberFormatter.with().grouping(GroupingStrategy.MIN2),
                new ULocale("hu"),
                "87 650 000",
                "8 765 000",
                "876500",
                "87650",
                "8765",
                "876,5",
                "87,65",
                "8,765",
                "0");

        assertFormatDescendingBig(
                "Hungarian Grouping, Always",
                "",
                NumberFormatter.with().grouping(GroupingStrategy.ON_ALIGNED),
                new ULocale("hu"),
                "87 650 000",
                "8 765 000",
                "876 500",
                "87 650",
                "8 765",
                "876,5",
                "87,65",
                "8,765",
                "0");

        // NOTE: Bulgarian is interesting because it has no grouping in the default currency format.
        // If this test breaks due to data changes, find another locale that has no default grouping.
        assertFormatDescendingBig(
                "Bulgarian Currency Grouping",
                "",
                NumberFormatter.with().grouping(GroupingStrategy.AUTO).unit(USD),
                new ULocale("bg"),
                "87650000,00 щ.д.",
                "8765000,00 щ.д.",
                "876500,00 щ.д.",
                "87650,00 щ.д.",
                "8765,00 щ.д.",
                "876,50 щ.д.",
                "87,65 щ.д.",
                "8,76 щ.д.",
                "0,00 щ.д.");

        assertFormatDescendingBig(
                "Bulgarian Currency Grouping, Always",
                "",
                NumberFormatter.with().grouping(GroupingStrategy.ON_ALIGNED).unit(USD),
                new ULocale("bg"),
                "87 650 000,00 щ.д.",
                "8 765 000,00 щ.д.",
                "876 500,00 щ.д.",
                "87 650,00 щ.д.",
                "8 765,00 щ.д.",
                "876,50 щ.д.",
                "87,65 щ.д.",
                "8,76 щ.д.",
                "0,00 щ.д.");

        MacroProps macros = new MacroProps();
        macros.grouping = Grouper.getInstance((short) 4, (short) 1, (short) 3);
        assertFormatDescendingBig(
                "Custom Grouping via Internal API",
                "",
                NumberFormatter.with().macros(macros),
                ULocale.ENGLISH,
                "8,7,6,5,0000",
                "8,7,6,5000",
                "876500",
                "87650",
                "8765",
                "876.5",
                "87.65",
                "8.765",
                "0");
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
                88.88,
                "88.88**%");

        assertFormatSingle(
                "Pad After Suffix",
                "%",
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.AFTER_SUFFIX))
                        .unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                88.88,
                "88.88%**");

        assertFormatSingle(
                "Currency Spacing with Zero Digit Padding Broken",
                "$GBP unit-width=ISO_CODE",
                NumberFormatter.with().padding(Padder.codePoints('0', 12, PadPosition.AFTER_PREFIX)).unit(GBP)
                        .unitWidth(UnitWidth.ISO_CODE),
                ULocale.ENGLISH,
                514.23,
                "GBP 000514.23"); // TODO: This is broken; it renders too wide (13 instead of 12).
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
                "US$ 87,650.00",
                "US$ 8,765.00",
                "US$ 876.50",
                "US$ 87.65",
                "US$ 8.76",
                "US$ 0.88",
                "US$ 0.09",
                "US$ 0.01",
                "US$ 0.00");

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

        // NOTE: Locale ar puts ¤ after the number in NS arab but before the number in NS latn.

        assertFormatSingle(
                "Currency symbol should precede number in ar with NS latn",
                "",
                NumberFormatter.with().symbols(NumberingSystem.LATIN).unit(USD),
                new ULocale("ar"),
                12345.67,
                "US$ 12,345.67");

        assertFormatSingle(
                "Currency symbol should precede number in ar@numbers=latn",
                "",
                NumberFormatter.with().unit(USD),
                new ULocale("ar@numbers=latn"),
                12345.67,
                "US$ 12,345.67");

        assertFormatSingle(
                "Currency symbol should follow number in ar-EG with NS arab",
                "",
                NumberFormatter.with().unit(USD),
                new ULocale("ar-EG"),
                12345.67,
                "١٢٬٣٤٥٫٦٧ US$");

        assertFormatSingle(
                "Currency symbol should follow number in ar@numbers=arab",
                "",
                NumberFormatter.with().unit(USD),
                new ULocale("ar@numbers=arab"),
                12345.67,
                "١٢٬٣٤٥٫٦٧ US$");

        assertFormatSingle(
                "NumberingSystem in API should win over @numbers keyword",
                "",
                NumberFormatter.with().symbols(NumberingSystem.LATIN).unit(USD),
                new ULocale("ar@numbers=arab"),
                12345.67,
                "US$ 12,345.67");

        assertEquals("NumberingSystem in API should win over @numbers keyword in reverse order",
                "US$ 12,345.67",
                NumberFormatter.withLocale(new ULocale("ar@numbers=arab"))
                    .symbols(NumberingSystem.LATIN)
                    .unit(USD)
                    .format(12345.67)
                    .toString());

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
                "Sign Auto Zero",
                "",
                NumberFormatter.with().sign(SignDisplay.AUTO),
                ULocale.ENGLISH,
                0,
                "0");

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
                "Sign Always Zero",
                "",
                NumberFormatter.with().sign(SignDisplay.ALWAYS),
                ULocale.ENGLISH,
                0,
                "+0");

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
                "Sign Never Zero",
                "",
                NumberFormatter.with().sign(SignDisplay.NEVER),
                ULocale.ENGLISH,
                0,
                "0");

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
                "Sign Accounting Zero",
                "",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD),
                ULocale.ENGLISH,
                0,
                "$0.00");

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

        assertFormatSingle(
                "Sign Accounting-Always Zero",
                "",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_ALWAYS).unit(USD),
                ULocale.ENGLISH,
                0,
                "+$0.00");

        assertFormatSingle(
                "Sign Except-Zero Positive",
                "",
                NumberFormatter.with().sign(SignDisplay.EXCEPT_ZERO),
                ULocale.ENGLISH,
                444444,
                "+444,444");

        assertFormatSingle(
                "Sign Always Negative",
                "",
                NumberFormatter.with().sign(SignDisplay.EXCEPT_ZERO),
                ULocale.ENGLISH,
                -444444,
                "-444,444");

        assertFormatSingle(
                "Sign Except-Zero Zero",
                "",
                NumberFormatter.with().sign(SignDisplay.EXCEPT_ZERO),
                ULocale.ENGLISH,
                0,
                "0");

        assertFormatSingle(
                "Sign Accounting-Except-Zero Positive",
                "$USD sign=ACCOUNTING_ALWAYS",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_EXCEPT_ZERO).unit(USD),
                ULocale.ENGLISH,
                444444,
                "+$444,444.00");

        assertFormatSingle(
                "Sign Accounting-Except-Zero Negative",
                "$USD sign=ACCOUNTING_ALWAYS",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_EXCEPT_ZERO).unit(USD),
                ULocale.ENGLISH,
                -444444,
                "($444,444.00)");

        assertFormatSingle(
                "Sign Accounting-Except-Zero Zero",
                "",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_EXCEPT_ZERO).unit(USD),
                ULocale.ENGLISH,
                0,
                "$0.00");

        assertFormatSingle(
                "Sign Accounting Negative Hidden",
                "$USD unit-width=HIDDEN sign=ACCOUNTING",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD).unitWidth(UnitWidth.HIDDEN),
                ULocale.ENGLISH,
                -444444,
                "(444,444.00)");
    }

    @Test
    public void decimal() {
        assertFormatDescending(
                "Decimal Default",
                "decimal=AUTO",
                NumberFormatter.with().decimal(DecimalSeparatorDisplay.AUTO),
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
                NumberFormatter.with().decimal(DecimalSeparatorDisplay.ALWAYS),
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
        assertEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.withLocale(ULocale.ENGLISH));
        assertEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.withLocale(Locale.ENGLISH));
        assertNotEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.with().locale(Locale.FRENCH));
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

    @Test
    public void validRanges() throws NoSuchMethodException, IllegalAccessException {
        Method[] methodsWithOneArgument = new Method[] { Rounder.class.getDeclaredMethod("fixedFraction", Integer.TYPE),
                Rounder.class.getDeclaredMethod("minFraction", Integer.TYPE),
                Rounder.class.getDeclaredMethod("maxFraction", Integer.TYPE),
                Rounder.class.getDeclaredMethod("fixedDigits", Integer.TYPE),
                Rounder.class.getDeclaredMethod("minDigits", Integer.TYPE),
                Rounder.class.getDeclaredMethod("maxDigits", Integer.TYPE),
                FractionRounder.class.getDeclaredMethod("withMinDigits", Integer.TYPE),
                FractionRounder.class.getDeclaredMethod("withMaxDigits", Integer.TYPE),
                ScientificNotation.class.getDeclaredMethod("withMinExponentDigits", Integer.TYPE),
                IntegerWidth.class.getDeclaredMethod("zeroFillTo", Integer.TYPE),
                IntegerWidth.class.getDeclaredMethod("truncateAt", Integer.TYPE), };
        Method[] methodsWithTwoArguments = new Method[] {
                Rounder.class.getDeclaredMethod("minMaxFraction", Integer.TYPE, Integer.TYPE),
                Rounder.class.getDeclaredMethod("minMaxDigits", Integer.TYPE, Integer.TYPE), };

        final int EXPECTED_MAX_INT_FRAC_SIG = 999;
        final String expectedSubstring0 = "between 0 and 999 (inclusive)";
        final String expectedSubstring1 = "between 1 and 999 (inclusive)";
        final String expectedSubstringN1 = "between -1 and 999 (inclusive)";

        // We require that the upper bounds all be 999 inclusive.
        // The lower bound may be either -1, 0, or 1.
        Set<String> methodsWithLowerBound1 = new HashSet();
        methodsWithLowerBound1.add("fixedDigits");
        methodsWithLowerBound1.add("minDigits");
        methodsWithLowerBound1.add("maxDigits");
        methodsWithLowerBound1.add("minMaxDigits");
        methodsWithLowerBound1.add("withMinDigits");
        methodsWithLowerBound1.add("withMaxDigits");
        methodsWithLowerBound1.add("withMinExponentDigits");
        // Methods with lower bound 0:
        // fixedFraction
        // minFraction
        // maxFraction
        // minMaxFraction
        // zeroFillTo
        Set<String> methodsWithLowerBoundN1 = new HashSet();
        methodsWithLowerBoundN1.add("truncateAt");

        // Some of the methods require an object to be called upon.
        Map<String, Object> targets = new HashMap<String, Object>();
        targets.put("withMinDigits", Rounder.integer());
        targets.put("withMaxDigits", Rounder.integer());
        targets.put("withMinExponentDigits", Notation.scientific());
        targets.put("truncateAt", IntegerWidth.zeroFillTo(0));

        for (int argument = -2; argument <= EXPECTED_MAX_INT_FRAC_SIG + 2; argument++) {
            for (Method method : methodsWithOneArgument) {
                String message = "i = " + argument + "; method = " + method.getName();
                int lowerBound = methodsWithLowerBound1.contains(method.getName()) ? 1
                        : methodsWithLowerBoundN1.contains(method.getName()) ? -1 : 0;
                String expectedSubstring = lowerBound == 0 ? expectedSubstring0
                        : lowerBound == 1 ? expectedSubstring1 : expectedSubstringN1;
                Object target = targets.get(method.getName());
                try {
                    method.invoke(target, argument);
                    assertTrue(message, argument >= lowerBound && argument <= EXPECTED_MAX_INT_FRAC_SIG);
                } catch (InvocationTargetException e) {
                    assertTrue(message, argument < lowerBound || argument > EXPECTED_MAX_INT_FRAC_SIG);
                    // Ensure the exception message contains the expected substring
                    String actualMessage = e.getCause().getMessage();
                    assertNotEquals(message + ": " + actualMessage, -1, actualMessage.indexOf(expectedSubstring));
                }
            }
            for (Method method : methodsWithTwoArguments) {
                String message = "i = " + argument + "; method = " + method.getName();
                int lowerBound = methodsWithLowerBound1.contains(method.getName()) ? 1
                        : methodsWithLowerBoundN1.contains(method.getName()) ? -1 : 0;
                String expectedSubstring = lowerBound == 0 ? expectedSubstring0 : expectedSubstring1;
                Object target = targets.get(method.getName());
                // Check range on the first argument
                try {
                    // Pass EXPECTED_MAX_INT_FRAC_SIG as the second argument so arg1 <= arg2 in expected cases
                    method.invoke(target, argument, EXPECTED_MAX_INT_FRAC_SIG);
                    assertTrue(message, argument >= lowerBound && argument <= EXPECTED_MAX_INT_FRAC_SIG);
                } catch (InvocationTargetException e) {
                    assertTrue(message, argument < lowerBound || argument > EXPECTED_MAX_INT_FRAC_SIG);
                    // Ensure the exception message contains the expected substring
                    String actualMessage = e.getCause().getMessage();
                    assertNotEquals(message + ": " + actualMessage, -1, actualMessage.indexOf(expectedSubstring));
                }
                // Check range on the second argument
                try {
                    // Pass lowerBound as the first argument so arg1 <= arg2 in expected cases
                    method.invoke(target, lowerBound, argument);
                    assertTrue(message, argument >= lowerBound && argument <= EXPECTED_MAX_INT_FRAC_SIG);
                } catch (InvocationTargetException e) {
                    assertTrue(message, argument < lowerBound || argument > EXPECTED_MAX_INT_FRAC_SIG);
                    // Ensure the exception message contains the expected substring
                    String actualMessage = e.getCause().getMessage();
                    assertNotEquals(message + ": " + actualMessage, -1, actualMessage.indexOf(expectedSubstring));
                }
                // Check that first argument must be less than or equal to second argument
                try {
                    method.invoke(target, argument, argument - 1);
                    org.junit.Assert.fail();
                } catch (InvocationTargetException e) {
                    // Pass
                }
            }
        }

        // Check first argument less than or equal to second argument on IntegerWidth
        try {
            IntegerWidth.zeroFillTo(4).truncateAt(2);
            org.junit.Assert.fail();
        } catch (IllegalArgumentException e) {
            // Pass
        }
    }

    private static void assertFormatDescending(
            String message,
            String skeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            String... expected) {
        final double[] inputs = new double[] { 87650, 8765, 876.5, 87.65, 8.765, 0.8765, 0.08765, 0.008765, 0 };
        assertFormatDescending(message, skeleton, f, locale, inputs, expected);
    }

    private static void assertFormatDescendingBig(
            String message,
            String skeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            String... expected) {
        final double[] inputs = new double[] { 87650000, 8765000, 876500, 87650, 8765, 876.5, 87.65, 8.765, 0 };
        assertFormatDescending(message, skeleton, f, locale, inputs, expected);
    }

    private static void assertFormatDescending(
            String message,
            String skeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            double[] inputs,
            String... expected) {
        assert expected.length == 9;
        assertEquals(message + ": Skeleton:", skeleton, f.toSkeleton());
        LocalizedNumberFormatter l1 = f.threshold(0L).locale(locale); // no self-regulation
        LocalizedNumberFormatter l2 = f.threshold(1L).locale(locale); // all self-regulation
        LocalizedNumberFormatter l3 = NumberFormatter.fromSkeleton(skeleton).locale(locale);
        for (int i = 0; i < 9; i++) {
            double d = inputs[i];
            String actual1 = l1.format(d).toString();
            assertEquals(message + ": Unsafe Path: " + d, expected[i], actual1);
            String actual2 = l2.format(d).toString();
            assertEquals(message + ": Safe Path: " + d, expected[i], actual2);
            String actual3 = l3.format(d).toString();
            assertEquals(message + ": Skeleton Path: " + d, expected[i], actual3);
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
        LocalizedNumberFormatter l3 = NumberFormatter.fromSkeleton(skeleton).locale(locale);
        String actual1 = l1.format(input).toString();
        assertEquals(message + ": Unsafe Path: " + input, expected, actual1);
        String actual2 = l2.format(input).toString();
        assertEquals(message + ": Safe Path: " + input, expected, actual2);
        String actual3 = l3.format(input).toString();
        assertEquals(message + ": Skeleton Path: " + input, expected, actual3);
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
        LocalizedNumberFormatter l3 = NumberFormatter.fromSkeleton(skeleton).locale(locale);
        String actual1 = l1.format(input).toString();
        assertEquals(message + ": Unsafe Path: " + input, expected, actual1);
        String actual2 = l2.format(input).toString();
        assertEquals(message + ": Safe Path: " + input, expected, actual2);
        String actual3 = l3.format(input).toString();
        assertEquals(message + ": Skeleton Path: " + input, expected, actual3);
    }
}
