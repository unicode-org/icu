// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.FieldPosition;
import java.text.Format;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.format.FormattedValueTest;
import com.ibm.icu.dev.test.serializable.SerializableTestUtility;
import com.ibm.icu.impl.IllegalIcuArgumentException;
import com.ibm.icu.impl.number.Grouper;
import com.ibm.icu.impl.number.LocalizedNumberFormatterAsFormat;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.impl.number.Padder;
import com.ibm.icu.impl.number.Padder.PadPosition;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.number.CompactNotation;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.FractionPrecision;
import com.ibm.icu.number.IntegerWidth;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.DecimalSeparatorDisplay;
import com.ibm.icu.number.NumberFormatter.GroupingStrategy;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.Scale;
import com.ibm.icu.number.ScientificNotation;
import com.ibm.icu.number.SkeletonSyntaxException;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.ULocale;

public class NumberFormatterApiTest extends TestFmwk {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency CZK = Currency.getInstance("CZK");
    private static final Currency CAD = Currency.getInstance("CAD");
    private static final Currency ESP = Currency.getInstance("ESP");
    private static final Currency PTE = Currency.getInstance("PTE");
    private static final Currency RON = Currency.getInstance("RON");
    private static final Currency TWD = Currency.getInstance("TWD");
    private static final Currency TRY = Currency.getInstance("TRY");
    private static final Currency CNY = Currency.getInstance("CNY");

    @Test
    public void notationSimple() {
        assertFormatDescending(
                "Basic",
                "",
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
                "notation-simple",
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
                "scientific",
                "E0",
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
                "engineering",
                "EE0",
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
                "scientific/sign-always",
                "E+!0",
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
                "scientific/*ee",
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
                "scientific",
                "E0",
                NumberFormatter.with().notation(Notation.scientific()),
                ULocale.ENGLISH,
                -1000000,
                "-1E6");

        assertFormatSingle(
                "Scientific Infinity",
                "scientific",
                "E0",
                NumberFormatter.with().notation(Notation.scientific()),
                ULocale.ENGLISH,
                Double.NEGATIVE_INFINITY,
                "-∞");

        assertFormatSingle(
                "Scientific NaN",
                "scientific",
                "E0",
                NumberFormatter.with().notation(Notation.scientific()),
                ULocale.ENGLISH,
                Double.NaN,
                "NaN");
    }

    @Test
    public void notationCompact() {
        assertFormatDescendingBig(
                "Compact Short",
                "compact-short",
                "K",
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
                "compact-long",
                "KK",
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
                "compact-short currency/USD",
                "K currency/USD",
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
                "compact-short currency/USD unit-width-iso-code",
                "K currency/USD unit-width-iso-code",
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
                "compact-short currency/USD unit-width-full-name",
                "K currency/USD unit-width-full-name",
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
                "compact-long currency/USD",
                "KK currency/USD",
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
                "compact-long currency/USD unit-width-iso-code",
                "KK currency/USD unit-width-iso-code",
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
                "compact-long currency/USD unit-width-full-name",
                "KK currency/USD unit-width-full-name",
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
                "compact-long",
                "KK",
                NumberFormatter.with().notation(Notation.compactLong()),
                ULocale.forLanguageTag("es"),
                1000000,
                "1 millón");

        assertFormatSingle(
                "Compact Plural One with rounding",
                "compact-long precision-integer",
                "KK precision-integer",
                NumberFormatter.with().notation(Notation.compactLong()).precision(Precision.integer()),
                ULocale.forLanguageTag("es"),
                1222222,
                "1 millón");

        assertFormatSingle(
                "Compact Plural Other",
                "compact-long",
                "KK",
                NumberFormatter.with().notation(Notation.compactLong()),
                ULocale.forLanguageTag("es"),
                2000000,
                "2 millones");

        assertFormatSingle(
                "Compact with Negative Sign",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                -9876543.21,
                "-9.9M");

        assertFormatSingle(
                "Compact Rounding",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                990000,
                "990K");

        assertFormatSingle(
                "Compact Rounding",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                999000,
                "999K");

        assertFormatSingle(
                "Compact Rounding",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                999900,
                "1M");

        assertFormatSingle(
                "Compact Rounding",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                9900000,
                "9.9M");

        assertFormatSingle(
                "Compact Rounding",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                9990000,
                "10M");

        assertFormatSingle(
                "Compact in zh-Hant-HK",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                new ULocale("zh-Hant-HK"),
                1e7,
                "10M");

        assertFormatSingle(
                "Compact in zh-Hant",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                new ULocale("zh-Hant"),
                1e7,
                "1000\u842C");

        if (!logKnownIssue("21258", "StandardPlural cannot handle keywords 1, 0")) {
            assertFormatSingle(
                    "Compact with plural form =1 (ICU-21258)",
                    "compact-long",
                    "K",
                    NumberFormatter.with().notation(Notation.compactLong()),
                    ULocale.FRANCE,
                    1e3,
                    "mille");
        }

        assertFormatSingle(
                "Compact Infinity",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                Double.NEGATIVE_INFINITY,
                "-∞");

        assertFormatSingle(
                "Compact NaN",
                "compact-short",
                "K",
                NumberFormatter.with().notation(Notation.compactShort()),
                ULocale.ENGLISH,
                Double.NaN,
                "NaN");

        Map<String, Map<String, String>> compactCustomData = new HashMap<>();
        Map<String, String> entry = new HashMap<>();
        entry.put("one", "Kun");
        entry.put("other", "0KK");
        compactCustomData.put("1000", entry);
        assertFormatSingle(
                "Compact Somali No Figure",
                null, // feature not supported in skeleton
                null,
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
                "unit/meter",
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
                "unit/meter unit-width-full-name",
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
                "KK unit/meter unit-width-full-name",
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

        // // TODO(ICU-20941): Support formatting for not-built-in units
        // assertFormatDescending(
        //         "Hectometers",
        //         "measure-unit/length-hectometer",
        //         "unit/hectometer",
        //         NumberFormatter.with().unit(MeasureUnit.forIdentifier("hectometer")),
        //         ULocale.ENGLISH,
        //         "87,650 hm",
        //         "8,765 ham",
        //         "876.5 hm",
        //         "87.65 hm",
        //         "8.765 hm",
        //         "0.8765 hm",
        //         "0.08765 hm",
        //         "0.008765 hm",
        //         "0 hm");

        assertFormatSingleMeasure(
                "Meters with Measure Input",
                "unit-width-full-name",
                "unit-width-full-name",
                NumberFormatter.with().unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                new Measure(5.43, MeasureUnit.METER),
                "5.43 meters");

        assertFormatSingleMeasure(
                "Measure format method takes precedence over fluent chain",
                "measure-unit/length-meter",
                "unit/meter",
                NumberFormatter.with().unit(MeasureUnit.METER),
                ULocale.ENGLISH,
                new Measure(5.43, USD),
                "$5.43");

        assertFormatSingle(
                "Meters with Negative Sign",
                "measure-unit/length-meter",
                "unit/meter",
                NumberFormatter.with().unit(MeasureUnit.METER),
                ULocale.ENGLISH,
                -9876543.21,
                "-9,876,543.21 m");

        // The locale string "सान" appears only in brx.txt:
        assertFormatSingle(
                "Interesting Data Fallback 1",
                "measure-unit/duration-day unit-width-full-name",
                "unit/day unit-width-full-name",
                NumberFormatter.with().unit(MeasureUnit.DAY).unitWidth(UnitWidth.FULL_NAME),
                ULocale.forLanguageTag("brx"),
                5.43,
                "5.43 सान");

        // Requires following the alias from unitsNarrow to unitsShort:
        assertFormatSingle(
                "Interesting Data Fallback 2",
                "measure-unit/duration-day unit-width-narrow",
                "unit/day unit-width-narrow",
                NumberFormatter.with().unit(MeasureUnit.DAY).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("brx"),
                5.43,
                "5.43 d");

        // en_001.txt has a unitsNarrow/area/square-meter table, but table does not contain the OTHER unit,
        // requiring fallback to the root.
        assertFormatSingle(
                "Interesting Data Fallback 3",
                "measure-unit/area-square-meter unit-width-narrow",
                "unit/square-meter unit-width-narrow",
                NumberFormatter.with().unit(MeasureUnit.SQUARE_METER).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("en-GB"),
                5.43,
                "5.43m²");

        // Try accessing a narrow unit directly from root.
        assertFormatSingle(
                "Interesting Data Fallback 4",
                "measure-unit/area-square-meter unit-width-narrow",
                "unit/square-meter unit-width-narrow",
                NumberFormatter.with().unit(MeasureUnit.SQUARE_METER).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("root"),
                5.43,
                "5.43 m²");

        // es_US has "{0}°" for unitsNarrow/temperature/FAHRENHEIT.
        // NOTE: This example is in the documentation.
        assertFormatSingle(
                "MeasureUnit Difference between Narrow and Short (Narrow Version)",
                "measure-unit/temperature-fahrenheit unit-width-narrow",
                "unit/fahrenheit unit-width-narrow",
                NumberFormatter.with().unit(MeasureUnit.FAHRENHEIT).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("es-US"),
                5.43,
                "5.43°");

        assertFormatSingle(
                "MeasureUnit Difference between Narrow and Short (Short Version)",
                "measure-unit/temperature-fahrenheit unit-width-short",
                "unit/fahrenheit unit-width-short",
                NumberFormatter.with().unit(MeasureUnit.FAHRENHEIT).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("es-US"),
                5.43,
                "5.43 °F");

        assertFormatSingle(
                "MeasureUnit form without {0} in CLDR pattern",
                "measure-unit/temperature-kelvin unit-width-full-name",
                "unit/kelvin unit-width-full-name",
                NumberFormatter.with().unit(MeasureUnit.KELVIN).unitWidth(UnitWidth.FULL_NAME),
                ULocale.forLanguageTag("es-MX"),
                1,
                "kelvin");

        assertFormatSingle(
                "MeasureUnit form without {0} in CLDR pattern and wide base form",
                "measure-unit/temperature-kelvin .00000000000000000000 unit-width-full-name",
                "unit/kelvin .00000000000000000000 unit-width-full-name",
                NumberFormatter.with()
                    .precision(Precision.fixedFraction(20))
                    .unit(MeasureUnit.KELVIN)
                    .unitWidth(UnitWidth.FULL_NAME),
                ULocale.forLanguageTag("es-MX"),
                1,
                "kelvin");

        assertFormatSingle(
                "Person unit not in short form",
                "measure-unit/duration-year-person unit-width-full-name",
                "unit/year-person unit-width-full-name",
                NumberFormatter.with()
                        .unit(MeasureUnit.YEAR_PERSON)
                        .unitWidth(UnitWidth.FULL_NAME),
                ULocale.forLanguageTag("es-MX"),
                5,
                "5 a\u00F1os");

        assertFormatSingle(
                "Mixed unit",
                "unit/yard-and-foot-and-inch",
                "unit/yard-and-foot-and-inch",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("yard-and-foot-and-inch")),
                new ULocale("en-US"),
                3.65,
                "3 yd, 1 ft, 11.4 in");

        assertFormatSingle(
                "Mixed unit, Scientific",
                "unit/yard-and-foot-and-inch E0",
                "unit/yard-and-foot-and-inch E0",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("yard-and-foot-and-inch"))
                        .notation(Notation.scientific()),
                new ULocale("en-US"),
                3.65,
                "3 yd, 1 ft, 1.14E1 in");

        assertFormatSingle(
                "Mixed Unit (Narrow Version)",
                "unit/metric-ton-and-kilogram-and-gram unit-width-narrow",
                "unit/metric-ton-and-kilogram-and-gram unit-width-narrow",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("metric-ton-and-kilogram-and-gram"))
                        .unitWidth(UnitWidth.NARROW),
                new ULocale("en-US"),
                4.28571,
                "4t 285kg 710g");

        assertFormatSingle(
                "Mixed Unit (Short Version)",
                "unit/metric-ton-and-kilogram-and-gram unit-width-short",
                "unit/metric-ton-and-kilogram-and-gram unit-width-short",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("metric-ton-and-kilogram-and-gram"))
                        .unitWidth(UnitWidth.SHORT),
                new ULocale("en-US"),
                4.28571,
                "4 t, 285 kg, 710 g");

        assertFormatSingle(
                "Mixed Unit (Full Name Version)",
                "unit/metric-ton-and-kilogram-and-gram unit-width-full-name",
                "unit/metric-ton-and-kilogram-and-gram unit-width-full-name",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("metric-ton-and-kilogram-and-gram"))
                        .unitWidth(UnitWidth.FULL_NAME),
                new ULocale("en-US"),
                4.28571,
                "4 metric tons, 285 kilograms, 710 grams");

        assertFormatSingle(
                "Testing \"1 foot 12 inches\"",
                "unit/foot-and-inch @### unit-width-full-name",
                "unit/foot-and-inch @### unit-width-full-name",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("foot-and-inch"))
                        .precision(Precision.maxSignificantDigits(4))
                        .unitWidth(UnitWidth.FULL_NAME),
                new ULocale("en-US"),
                1.9999,
                "2 feet, 0 inches");

        assertFormatSingle(
                "Negative numbers: temperature",
                "measure-unit/temperature-celsius",
                "unit/celsius",
                NumberFormatter.with().unit(MeasureUnit.forIdentifier("celsius")),
                new ULocale("nl-NL"),
                -6.5,
                "-6,5\u00B0C");

        assertFormatSingle(
                "Negative numbers: time",
                "unit/hour-and-minute-and-second",
                "unit/hour-and-minute-and-second",
                NumberFormatter.with().unit(MeasureUnit.forIdentifier("hour-and-minute-and-second")),
                new ULocale("de-DE"),
                -1.24,
                "-1 Std., 14 Min. und 24 Sek.");

        assertFormatSingle(
                "Zero out the unit field",
                "",
                "",
                NumberFormatter.with().unit(MeasureUnit.KELVIN).unit(NoUnit.BASE),
                new ULocale("en"),
                100,
                "100");
    }

    @Test
    public void unitCompoundMeasure() {
        assertFormatDescending(
                "Meters Per Second Short (unit that simplifies) and perUnit method",
                "measure-unit/length-meter per-measure-unit/duration-second",
                "unit/meter-per-second",
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
                "Meters Per Second Short, built-in m/s",
                "measure-unit/speed-meter-per-second",
                "unit/meter-per-second",
                NumberFormatter.with().unit(MeasureUnit.METER_PER_SECOND),
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
                "measure-unit/mass-pound per-measure-unit/area-square-mile",
                "unit/pound-per-square-mile",
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
                "measure-unit/energy-joule per-measure-unit/length-furlong",
                "unit/joule-per-furlong",
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

        // // TODO(ICU-20941): Support constructions such as this one.
        // assertFormatDescending(
        //         "Joules Per Furlong Short with unit identifier via API",
        //         "measure-unit/energy-joule per-measure-unit/length-furlong",
        //         "unit/joule-per-furlong",
        //         NumberFormatter.with().unit(MeasureUnit.forIdentifier("joule-per-furlong")),
        //         ULocale.ENGLISH,
        //         "87,650 J/fur",
        //         "8,765 J/fur",
        //         "876.5 J/fur",
        //         "87.65 J/fur",
        //         "8.765 J/fur",
        //         "0.8765 J/fur",
        //         "0.08765 J/fur",
        //         "0.008765 J/fur",
        //         "0 J/fur");

        assertFormatDescending(
                "Pounds per Square Inch: composed",
                "measure-unit/force-pound-force per-measure-unit/area-square-inch",
                "unit/pound-force-per-square-inch",
                NumberFormatter.with().unit(MeasureUnit.POUND_FORCE).perUnit(MeasureUnit.SQUARE_INCH),
                ULocale.ENGLISH,
                "87,650 psi",
                "8,765 psi",
                "876.5 psi",
                "87.65 psi",
                "8.765 psi",
                "0.8765 psi",
                "0.08765 psi",
                "0.008765 psi",
                "0 psi");

        assertFormatDescending(
                "Pounds per Square Inch: built-in",
                "measure-unit/force-pound-force per-measure-unit/area-square-inch",
                "unit/pound-force-per-square-inch",
                NumberFormatter.with().unit(MeasureUnit.POUND_PER_SQUARE_INCH),
                ULocale.ENGLISH,
                "87,650 psi",
                "8,765 psi",
                "876.5 psi",
                "87.65 psi",
                "8.765 psi",
                "0.8765 psi",
                "0.08765 psi",
                "0.008765 psi",
                "0 psi");

        assertFormatSingle(
                "m/s/s simplifies to m/s^2",
                "measure-unit/speed-meter-per-second per-measure-unit/duration-second",
                "unit/meter-per-square-second",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER_PER_SECOND)
                        .perUnit(MeasureUnit.SECOND),
                new ULocale("en-GB"),
                2.4,
                "2.4 m/s\u00B2");

        assertFormatSingle(
                "Negative numbers: acceleration",
                "measure-unit/acceleration-meter-per-square-second",
                "unit/meter-per-second-second",
                NumberFormatter.with().unit(MeasureUnit.forIdentifier("meter-per-pow2-second")),
                new ULocale("af-ZA"),
                -9.81,
                "-9,81 m/s\u00B2");

        // Testing the rejection of invalid specifications

        // If .unit() is not given a built-in type, .perUnit() is not allowed
        // (because .unit is now flexible enough to handle compound units,
        // .perUnit() is supported for backward compatibility).
        LocalizedNumberFormatter nf = NumberFormatter.with()
                .unit(MeasureUnit.forIdentifier("furlong-pascal"))
                .perUnit(MeasureUnit.METER)
                .locale(new ULocale("en-GB"));

        try {
            nf.format(2.4d);
            fail("Expected failure, got: " + nf.format(2.4d) + ".");
        } catch (UnsupportedOperationException e) {
            // Pass
        }

        // .perUnit() may only be passed a built-in type, or something that
        // combines to a built-in type together with .unit().
        nf = NumberFormatter.with()
                .unit(MeasureUnit.FURLONG)
                .perUnit(MeasureUnit.forIdentifier("square-second"))
                .locale(new ULocale("en-GB"));
        try {
            nf.format(2.4d);
            fail("Expected failure, got: " + nf.format(2.4d) + ".");
        } catch (UnsupportedOperationException e) {
            // pass
        }
        // As above, "square-second" is not a built-in type, however this time,
        // meter-per-square-second is a built-in type.
        assertFormatSingle(
                "meter per square-second works as a composed unit",
                "measure-unit/speed-meter-per-second per-measure-unit/duration-second",
                "unit/meter-per-square-second",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .perUnit(MeasureUnit.forIdentifier("square-second")),
                new ULocale("en-GB"),
                2.4,
                "2.4 m/s\u00B2");
    }

    // TODO: merge these tests into NumberSkeletonTest.java instead of here:
    @Test
    public void unitSkeletons() {
        Object[][] cases = {
            {"old-form built-in compound unit",     //
             "measure-unit/speed-meter-per-second", //
             "unit/meter-per-second"},

            {"old-form compound construction, converts to built-in",       //
             "measure-unit/length-meter per-measure-unit/duration-second", //
             "unit/meter-per-second"},

            {"old-form compound construction which does not simplify to a built-in", //
             "measure-unit/energy-joule per-measure-unit/length-meter",              //
             "unit/joule-per-meter"},

            {"old-form compound-compound ugliness resolves neatly",                  //
             "measure-unit/speed-meter-per-second per-measure-unit/duration-second", //
             "unit/meter-per-square-second"},

            {"short-form built-in units stick with the built-in", //
             "unit/meter-per-second",                             //
             "unit/meter-per-second"},

            {"short-form compound units stay as is", //
             "unit/square-meter-per-square-meter",   //
             "unit/square-meter-per-square-meter"},

            {"short-form compound units stay as is", //
             "unit/joule-per-furlong",               //
             "unit/joule-per-furlong"},

            {"short-form that doesn't consist of built-in units", //
             "unit/hectometer-per-second",                        //
             "unit/hectometer-per-second"},

            {"short-form that doesn't consist of built-in units", //
             "unit/meter-per-hectosecond",                        //
             "unit/meter-per-hectosecond"},

            {"percent compound skeletons handled correctly", //
             "unit/percent-per-meter",                       //
             "unit/percent-per-meter"},

            {"permille compound skeletons handled correctly",                //
             "measure-unit/concentr-permille per-measure-unit/length-meter", //
             "unit/permille-per-meter"},

            {"percent simple unit is not actually considered a unit", //
             "unit/percent",                                          //
             "percent"},

            {"permille simple unit is not actually considered a unit", //
             "measure-unit/concentr-permille",                         //
             "permille"},

            // // TODO: binary prefixes not supported yet!
            // {"Round-trip example from icu-units#35", //
            //  "unit/kibijoule-per-furlong",           //
            //  "unit/kibijoule-per-furlong"},
        };
        for (Object[] cas : cases) {
            String msg = (String)cas[0];
            String inputSkeleton = (String)cas[1];
            String normalizedSkeleton = (String)cas[2];
            UnlocalizedNumberFormatter nf = NumberFormatter.forSkeleton(inputSkeleton);
            assertEquals(msg, normalizedSkeleton, nf.toSkeleton());
        }

        Object[][] failCases = {
            {"Parsing measure-unit/* results in failure if not built-in unit",
             "measure-unit/hectometer", //
             true,                      //
             false},

            {"Parsing per-measure-unit/* results in failure if not built-in unit",
             "measure-unit/meter per-measure-unit/hectosecond", //
             true,                                              //
             false},

            {"\"currency/EUR measure-unit/length-meter\" fails, conflicting skeleton.",
             "currency/EUR measure-unit/length-meter", //
             true,                                     //
             false},

            {"\"measure-unit/length-meter currency/EUR\" fails, conflicting skeleton.",
             "measure-unit/length-meter currency/EUR", //
             true,                                     //
             false},

            {"\"currency/EUR per-measure-unit/meter\" fails, conflicting skeleton.",
             "currency/EUR per-measure-unit/length-meter", //
             true,                                         //
             false},
        };
        for (Object[] cas : failCases) {
            String msg = (String)cas[0];
            String inputSkeleton = (String)cas[1];
            boolean forSkeletonExpectFailure = (boolean)cas[2];
            boolean toSkeletonExpectFailure = (boolean)cas[3];
            UnlocalizedNumberFormatter nf = null;
            try {
                nf = NumberFormatter.forSkeleton(inputSkeleton);
                if (forSkeletonExpectFailure) {
                    fail("forSkeleton() should have failed: " + msg);
                }
            } catch (Exception e) {
                if (!forSkeletonExpectFailure) {
                    fail("forSkeleton() should not have failed: " + msg);
                }
                continue;
            }
            try {
                nf.toSkeleton();
                if (toSkeletonExpectFailure) {
                    fail("toSkeleton() should have failed: " + msg);
                }
            } catch (Exception e) {
                if (!toSkeletonExpectFailure) {
                    fail("toSkeleton() should not have failed: " + msg);
                }
            }
        }

        assertEquals(                                //
            ".unit(METER_PER_SECOND) normalization", //
            "unit/meter-per-second",                 //
            NumberFormatter.with().unit(MeasureUnit.METER_PER_SECOND).toSkeleton());
        assertEquals(                                     //
            ".unit(METER).perUnit(SECOND) normalization", //
            "unit/meter-per-second",
            NumberFormatter.with().unit(MeasureUnit.METER).perUnit(MeasureUnit.SECOND).toSkeleton());
        assertEquals(                                                         //
            ".unit(MeasureUnit.forIdentifier(\"hectometer\")) normalization", //
            "unit/hectometer",
            NumberFormatter.with().unit(MeasureUnit.forIdentifier("hectometer")).toSkeleton());
        assertEquals(                                                         //
            ".unit(MeasureUnit.forIdentifier(\"hectometer\")) normalization", //
            "unit/meter-per-hectosecond",
            NumberFormatter.with()
                .unit(MeasureUnit.METER)
                .perUnit(MeasureUnit.forIdentifier("hectosecond"))
                .toSkeleton());

        assertEquals(                                                //
            ".unit(CURRENCY) produces a currency/CURRENCY skeleton", //
            "currency/GBP",                                          //
            NumberFormatter.with().unit(GBP).toSkeleton());

        // .unit(CURRENCY).perUnit(ANYTHING) is not supported.
        try {
            NumberFormatter.with().unit(GBP).perUnit(MeasureUnit.METER).toSkeleton();
            fail("should give an error, unit(currency) with perUnit() is invalid.");
        } catch (UnsupportedOperationException e) {
            // Pass
        }
    }

    @Test
    public void unitUsage() {
        UnlocalizedNumberFormatter unloc_formatter;
        LocalizedNumberFormatter formatter;
        FormattedNumber formattedNum;
        String uTestCase;

        try {
            NumberFormatter.with().usage("road").locale(ULocale.ENGLISH).format(1);
            fail("should give an error, usage() without unit() is invalid");
        } catch (IllegalIcuArgumentException e) {
            // Pass
        }

        unloc_formatter = NumberFormatter.with().usage("road").unit(MeasureUnit.METER);

        uTestCase = "unitUsage() en-ZA road";
        formatter = unloc_formatter.locale(new ULocale("en-ZA"));
        formattedNum = formatter.format(321d);


        assertTrue(
                uTestCase + ", got outputUnit: \"" + formattedNum.getOutputUnit().getIdentifier() + "\"",
                MeasureUnit.METER.equals(formattedNum.getOutputUnit()));
        assertEquals(uTestCase, "300 m", formattedNum.toString());
        {
            final Object[][] expectedFieldPositions = {
                    {NumberFormat.Field.INTEGER, 0, 3},
                    {NumberFormat.Field.MEASURE_UNIT, 4, 5}
            };

            assertNumberFieldPositions(
                    uTestCase + " field positions",
                    formattedNum,
                    expectedFieldPositions);
        }

        assertFormatDescendingBig(
                uTestCase,
                "measure-unit/length-meter usage/road",
                "unit/meter usage/road",
                unloc_formatter,
                new ULocale("en-ZA"),
                "87\u00A0650 km",
                "8\u00A0765 km",
                "876 km", // 6.5 rounds down, 7.5 rounds up.
                "88 km",
                "8,8 km",
                "900 m",
                "90 m",
                "10 m",
                "0 m");

        uTestCase = "unitUsage() en-GB road";
        formatter = unloc_formatter.locale(new ULocale("en-GB"));
        formattedNum = formatter.format(321d);
        assertTrue(
                uTestCase + ", got outputUnit: \"" + formattedNum.getOutputUnit().getIdentifier() + "\"",
                MeasureUnit.YARD.equals(formattedNum.getOutputUnit()));
        assertEquals(uTestCase, "350 yd", formattedNum.toString());
        {
            final Object[][] expectedFieldPositions = {
                    {NumberFormat.Field.INTEGER, 0, 3},
                    {NumberFormat.Field.MEASURE_UNIT, 4, 6}};
            assertNumberFieldPositions(
                    (uTestCase + " field positions"),
                    formattedNum,
                    expectedFieldPositions);
        }

        assertFormatDescendingBig(
                uTestCase,
                "measure-unit/length-meter usage/road",
                "unit/meter usage/road",
                unloc_formatter,
                new ULocale("en-GB"),
                "54,463 mi",
                "5,446 mi",
                "545 mi",
                "54 mi",
                "5.4 mi",
                "0.54 mi",
                "96 yd",
                "9.6 yd",
                "0 yd");

        uTestCase = "unitUsage() en-US road";
        formatter = unloc_formatter.locale(new ULocale("en-US"));
        formattedNum = formatter.format(321d);
        assertTrue(
                uTestCase + ", got outputUnit: \"" + formattedNum.getOutputUnit().getIdentifier() + "\"",
                MeasureUnit.FOOT == formattedNum.getOutputUnit());
        assertEquals(uTestCase, "1,050 ft", formattedNum.toString());
        {
            final Object[][] expectedFieldPositions = {
                    {NumberFormat.Field.GROUPING_SEPARATOR, 1, 2},
                    {NumberFormat.Field.INTEGER, 0, 5},
                    {NumberFormat.Field.MEASURE_UNIT, 6, 8}};
            assertNumberFieldPositions(
                    uTestCase + " field positions",
                    formattedNum,
                    expectedFieldPositions);
        }
        assertFormatDescendingBig(
                uTestCase,
                "measure-unit/length-meter usage/road",
                "unit/meter usage/road",
                unloc_formatter,
                new ULocale("en-US"),
                "54,463 mi",
                "5,446 mi",
                "545 mi",
                "54 mi",
                "5.4 mi",
                "0.54 mi",
                "300 ft",
                "30 ft",
                "0 ft");

        unloc_formatter = NumberFormatter.with().usage("person").unit(MeasureUnit.KILOGRAM);
        uTestCase = "unitUsage() en-GB person";
        formatter = unloc_formatter.locale(new ULocale("en-GB"));
        formattedNum = formatter.format(80d);
        assertTrue(
                uTestCase + ", got outputUnit: \"" + formattedNum.getOutputUnit().getIdentifier() + "\"",
                MeasureUnit.forIdentifier("stone-and-pound").equals(formattedNum.getOutputUnit()));
        assertEquals(uTestCase, "12 st, 8.4 lb", formattedNum.toString());
        {
            final Object[][] expectedFieldPositions = {
                    // // Desired output: TODO(icu-units#67)
                    // {NumberFormat.Field.INTEGER, 0, 2},
                    // {NumberFormat.Field.MEASURE_UNIT, 3, 5},
                    // {NumberFormat.ULISTFMT_LITERAL_FIELD, 5, 6},
                    // {NumberFormat.Field.INTEGER, 7, 8},
                    // {NumberFormat.DECIMAL_SEPARATOR_FIELD, 8, 9},
                    // {NumberFormat.FRACTION_FIELD, 9, 10},
                    // {NumberFormat.Field.MEASURE_UNIT, 11, 13}};

                    // Current output: rather no fields than wrong fields
                    {NumberFormat.Field.INTEGER, 7, 8},
                    {NumberFormat.Field.DECIMAL_SEPARATOR, 8, 9},
                    {NumberFormat.Field.FRACTION, 9, 10},
            };

            assertNumberFieldPositions(
                    uTestCase + " field positions",
                    formattedNum,
                    expectedFieldPositions);
        }
        assertFormatDescending(
                uTestCase,
                "measure-unit/mass-kilogram usage/person",
                "unit/kilogram usage/person",
                unloc_formatter,
                new ULocale("en-GB"),
                "13,802 st, 7.2 lb",
                "1,380 st, 3.5 lb",
                "138 st, 0.35 lb",
                "13 st, 11 lb",
                "1 st, 5.3 lb",
                "1 lb, 15 oz",
                "0 lb, 3.1 oz",
                "0 lb, 0.31 oz",
                "0 lb, 0 oz");

        assertFormatDescending(
                uTestCase,
                "usage/person unit-width-narrow measure-unit/mass-kilogram",
                "usage/person unit-width-narrow unit/kilogram",
                unloc_formatter.unitWidth(UnitWidth.NARROW),
                new ULocale("en-GB"),
                "13,802st 7.2lb",
                "1,380st 3.5lb",
                "138st 0.35lb",
                "13st 11lb",
                "1st 5.3lb",
                "1lb 15oz",
                "0lb 3.1oz",
                "0lb 0.31oz",
                "0lb 0oz");

        assertFormatDescending(
                uTestCase,
                "usage/person unit-width-short measure-unit/mass-kilogram",
                "usage/person unit-width-short unit/kilogram",
                unloc_formatter.unitWidth(UnitWidth.SHORT),
                new ULocale("en-GB"),
                "13,802 st, 7.2 lb",
                "1,380 st, 3.5 lb",
                "138 st, 0.35 lb",
                "13 st, 11 lb",
                "1 st, 5.3 lb",
                "1 lb, 15 oz",
                "0 lb, 3.1 oz",
                "0 lb, 0.31 oz",
                "0 lb, 0 oz");

        assertFormatDescending(
                uTestCase,
                "usage/person unit-width-full-name measure-unit/mass-kilogram",
                "usage/person unit-width-full-name unit/kilogram",
                unloc_formatter.unitWidth(UnitWidth.FULL_NAME),
                new ULocale("en-GB"),
                "13,802 stone, 7.2 pounds",
                "1,380 stone, 3.5 pounds",
                "138 stone, 0.35 pounds",
                "13 stone, 11 pounds",
                "1 stone, 5.3 pounds",
                "1 pound, 15 ounces",
                "0 pounds, 3.1 ounces",
                "0 pounds, 0.31 ounces",
                "0 pounds, 0 ounces");

       assertFormatDescendingBig(
               "Scientific notation with Usage: possible when using a reasonable Precision",
               "scientific @### usage/default measure-unit/area-square-meter unit-width-full-name",
               "scientific @### usage/default unit/square-meter unit-width-full-name",
               NumberFormatter.with()
                       .unit(MeasureUnit.SQUARE_METER)
                       .usage("default")
                       .notation(Notation.scientific())
                       .precision(Precision.minMaxSignificantDigits(1, 4))
                       .unitWidth(UnitWidth.FULL_NAME),
               new ULocale("en-ZA"),
               "8,765E1 square kilometres",
               "8,765E0 square kilometres",
               "8,765E1 hectares",
               "8,765E0 hectares",
               "8,765E3 square metres",
               "8,765E2 square metres",
               "8,765E1 square metres",
               "8,765E0 square metres",
               "0E0 square centimetres");

        assertFormatSingle(
                "Negative numbers: minute-and-second",
                "measure-unit/duration-second usage/media",
                "unit/second usage/media",
                NumberFormatter.with().unit(MeasureUnit.SECOND).usage("media"),
                new ULocale("nl-NL"),
                -77.7,
                "-1 min, 18 sec");

        assertFormatSingle(
                "Rounding Mode propagates: rounding down",
                "usage/road measure-unit/length-centimeter rounding-mode-floor",
                "usage/road unit/centimeter rounding-mode-floor",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("centimeter"))
                        .usage("road")
                        .roundingMode(RoundingMode.FLOOR),
                new ULocale("en-ZA"),
                34500,
                "300 m");

        assertFormatSingle(
                "Rounding Mode propagates: rounding up",
                "usage/road measure-unit/length-centimeter rounding-mode-ceiling",
                "usage/road unit/centimeter rounding-mode-ceiling",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("centimeter"))
                        .usage("road")
                        .roundingMode(RoundingMode.CEILING),
                new ULocale("en-ZA"),
                30500,
                "350 m");

        // TODO(icu-units#38): improve unit testing coverage. E.g. add
        // vehicle-fuel triggering inversion conversion code. Test with 0 too,
        // to see divide-by-zero behaviour.
    }

    @Test
    public void unitUsageErrorCodes() {
        UnlocalizedNumberFormatter unloc_formatter;

        try {
            NumberFormatter.forSkeleton("unit/foobar");
            fail("should give an error, because foobar is an invalid unit");
        } catch (SkeletonSyntaxException e) {
            // Pass
        }

        unloc_formatter = NumberFormatter.forSkeleton("usage/foobar");
        // This does not give an error, because usage is not looked up yet.
        //status.errIfFailureAndReset("Expected behaviour: no immediate error for invalid usage");

        try {
            // Lacking a unit results in a failure. The skeleton is "incomplete", but we
            // support adding the unit via the fluent API, so it is not an error until
            // we build the formatting pipeline itself.
            unloc_formatter.locale(new ULocale("en-GB")).format(1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Pass
        }

        // Adding the unit as part of the fluent chain leads to success.
        unloc_formatter.unit(MeasureUnit.METER).locale(new ULocale("en-GB")).format(1); /* No Exception should be thrown */

    }


    // Tests for the "skeletons" field in unitPreferenceData, as well as precision
    // and notation overrides.
    @Test
    public void unitUsageSkeletons() {
        assertFormatSingle(
                "Default >300m road preference skeletons round to 50m",
                "usage/road measure-unit/length-meter",
                "usage/road unit/meter",
                NumberFormatter.with().unit(MeasureUnit.METER).usage("road"),
                new ULocale("en-ZA"),
                321,
                "300 m");

        assertFormatSingle(
                "Precision can be overridden: override takes precedence",
                "usage/road measure-unit/length-meter @#",
                "usage/road unit/meter @#",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .usage("road")
                        .precision(Precision.maxSignificantDigits(2)),
                new ULocale("en-ZA"),
                321,
                "320 m");

        assertFormatSingle(
                "Compact notation with Usage: bizarre, but possible (short)",
                "compact-short usage/road measure-unit/length-meter",
                "compact-short usage/road unit/meter",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .usage("road")
                        .notation(Notation.compactShort()),
                new ULocale("en-ZA"),
                987654321L,
                "988K km");

        assertFormatSingle(
                "Compact notation with Usage: bizarre, but possible (short, precision override)",
                "compact-short usage/road measure-unit/length-meter @#",
                "compact-short usage/road unit/meter @#",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .usage("road")
                        .notation(Notation.compactShort())
                        .precision(Precision.maxSignificantDigits(2)),
                new ULocale("en-ZA"),
                987654321L,
                "990K km");

        assertFormatSingle(
                "Compact notation with Usage: unusual but possible (long)",
                "compact-long usage/road measure-unit/length-meter @#",
                "compact-long usage/road unit/meter @#",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .usage("road")
                        .notation(Notation.compactLong())
                        .precision(Precision.maxSignificantDigits(2)),
                new ULocale("en-ZA"),
                987654321,
                "990 thousand km");

        assertFormatSingle(
                "Compact notation with Usage: unusual but possible (long, precision override)",
                "compact-long usage/road measure-unit/length-meter @#",
                "compact-long usage/road unit/meter @#",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .usage("road")
                        .notation(Notation.compactLong())
                        .precision(Precision.maxSignificantDigits(2)),
                new ULocale("en-ZA"),
                987654321,
                "990 thousand km");

        assertFormatSingle(
                "Scientific notation, not recommended, requires precision override for road",
                "scientific usage/road measure-unit/length-meter",
                "scientific usage/road unit/meter",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .usage("road")
                        .notation(Notation.scientific()),
                new ULocale("en-ZA"),
                321.45,
                // Rounding to the nearest "50" is not exponent-adjusted in scientific notation:
                "0E2 m");

        assertFormatSingle(
                "Scientific notation with Usage: possible when using a reasonable Precision",
                "scientific usage/road measure-unit/length-meter @###",
                "scientific usage/road unit/meter @###",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .usage("road")
                        .notation(Notation.scientific())
                        .precision(Precision.maxSignificantDigits(4)),
                new ULocale("en-ZA"),
                321.45, // 0.45 rounds down, 0.55 rounds up.
                "3,214E2 m");

        assertFormatSingle(
                "Scientific notation with Usage: possible when using a reasonable Precision",
                "scientific usage/default measure-unit/length-astronomical-unit unit-width-full-name",
                "scientific usage/default unit/astronomical-unit unit-width-full-name",
                NumberFormatter.with()
                        .unit(MeasureUnit.forIdentifier("astronomical-unit"))
                        .usage("default")
                        .notation(Notation.scientific())
                        .unitWidth(UnitWidth.FULL_NAME),
                new ULocale("en-ZA"),
                1e20,
                "1,5E28 kilometres");
    }


    @Test
    public void unitCurrency() {
        assertFormatDescending(
                "Currency",
                "currency/GBP",
                "currency/GBP",
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
                "currency/GBP unit-width-iso-code",
                "currency/GBP unit-width-iso-code",
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
                "currency/GBP unit-width-full-name",
                "currency/GBP unit-width-full-name",
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
                "currency/GBP unit-width-hidden",
                "currency/GBP unit-width-hidden",
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
                "",
                NumberFormatter.with(),
                ULocale.ENGLISH,
                new CurrencyAmount(5.43, GBP),
                "£5.43");

        assertFormatSingle(
                "Currency Long Name from Pattern Syntax",
                null,
                null,
                NumberFormatter.fromDecimalFormat(
                        PatternStringParser.parseToProperties("0 ¤¤¤"),
                        DecimalFormatSymbols.getInstance(ULocale.ENGLISH),
                        null).unit(GBP),
                ULocale.ENGLISH,
                1234567.89,
                "1234568 British pounds");

        assertFormatSingle(
                "Currency with Negative Sign",
                "currency/GBP",
                "currency/GBP",
                NumberFormatter.with().unit(GBP),
                ULocale.ENGLISH,
                -9876543.21,
                "-£9,876,543.21");

        // The full currency symbol is not shown in NARROW format.
        // NOTE: This example is in the documentation.
        assertFormatSingle(
                "Currency Difference between Narrow and Short (Narrow Version)",
                "currency/USD unit-width-narrow",
                "currency/USD unit-width-narrow",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("en-CA"),
                5.43,
                "$5.43");

        assertFormatSingle(
                "Currency Difference between Narrow and Short (Short Version)",
                "currency/USD unit-width-short",
                "currency/USD unit-width-short",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("en-CA"),
                5.43,
                "US$5.43");

        assertFormatSingle(
                "Currency Difference between Formal and Short (Formal Version)",
                "currency/TWD unit-width-formal",
                "currency/TWD unit-width-formal",
                NumberFormatter.with().unit(TWD).unitWidth(UnitWidth.FORMAL),
                ULocale.forLanguageTag("zh-TW"),
                5.43,
                "NT$5.43");

        assertFormatSingle(
                "Currency Difference between Formal and Short (Short Version)",
                "currency/TWD unit-width-short",
                "currency/TWD unit-width-short",
                NumberFormatter.with().unit(TWD).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("zh-TW"),
                5.43,
                "$5.43");

        assertFormatSingle(
                "Currency Difference between Variant and Short (Formal Version)",
                "currency/TRY unit-width-variant",
                "currency/TRY unit-width-variant",
                NumberFormatter.with().unit(TRY).unitWidth(UnitWidth.VARIANT),
                ULocale.forLanguageTag("tr-TR"),
                5.43,
                "TL\u00A05,43");

        assertFormatSingle(
                "Currency Difference between Variant and Short (Short Version)",
                "currency/TRY unit-width-short",
                "currency/TRY unit-width-short",
                NumberFormatter.with().unit(TRY).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("tr-TR"),
                5.43,
                "₺5,43");

        assertFormatSingle(
                "Currency-dependent format (Control)",
                "currency/USD unit-width-short",
                "currency/USD unit-width-short",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("ca"),
                444444.55,
                "444.444,55 USD");

        assertFormatSingle(
                "Currency-dependent format (Test)",
                "currency/ESP unit-width-short",
                "currency/ESP unit-width-short",
                NumberFormatter.with().unit(ESP).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("ca"),
                444444.55,
                "₧ 444.445");

        assertFormatSingle(
                "Currency-dependent symbols (Control)",
                "currency/USD unit-width-short",
                "currency/USD unit-width-short",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("pt-PT"),
                444444.55,
                "444 444,55 US$");

        // NOTE: This is a bit of a hack on CLDR's part. They set the currency symbol to U+200B (zero-
        // width space), and they set the decimal separator to the $ symbol.
        assertFormatSingle(
                "Currency-dependent symbols (Test Short)",
                "currency/PTE unit-width-short",
                "currency/PTE unit-width-short",
                NumberFormatter.with().unit(PTE).unitWidth(UnitWidth.SHORT),
                ULocale.forLanguageTag("pt-PT"),
                444444.55,
                "444,444$55 \u200B");

        assertFormatSingle(
                "Currency-dependent symbols (Test Narrow)",
                "currency/PTE unit-width-narrow",
                "currency/PTE unit-width-narrow",
                NumberFormatter.with().unit(PTE).unitWidth(UnitWidth.NARROW),
                ULocale.forLanguageTag("pt-PT"),
                444444.55,
                "444,444$55 \u200B");

        assertFormatSingle(
                "Currency-dependent symbols (Test ISO Code)",
                "currency/PTE unit-width-iso-code",
                "currency/PTE unit-width-iso-code",
                NumberFormatter.with().unit(PTE).unitWidth(UnitWidth.ISO_CODE),
                ULocale.forLanguageTag("pt-PT"),
                444444.55,
                "444,444$55 PTE");

        assertFormatSingle(
                "Plural form depending on visible digits (ICU-20499)",
                "currency/RON unit-width-full-name",
                "currency/RON unit-width-full-name",
                NumberFormatter.with().unit(RON).unitWidth(UnitWidth.FULL_NAME),
                ULocale.forLanguageTag("ro-RO"),
                24,
                "24,00 lei românești");

        assertFormatSingle(
                "Currency spacing in suffix (ICU-20954)",
                "currency/CNY",
                "currency/CNY",
                NumberFormatter.with().unit(CNY),
                ULocale.forLanguageTag("lu"),
                123.12,
                "123,12 CN¥");
    }

    @Test
    public void unitPercent() {
        assertFormatDescending(
                "Percent",
                "percent",
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
                "permille",
                "permille",
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
                "base-unit",
                "",
                NumberFormatter.with().unit(NoUnit.BASE),
                ULocale.ENGLISH,
                51423,
                "51,423");

        assertFormatSingle(
                "Percent with Negative Sign",
                "percent",
                "%",
                NumberFormatter.with().unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                -98.7654321,
                "-98.765432%");

        // ICU-20923
        assertFormatDescendingBig(
                "Compact Percent",
                "compact-short percent",
                "K %",
                NumberFormatter.with()
                        .notation(Notation.compactShort())
                        .unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                "88M%",
                "8.8M%",
                "876K%",
                "88K%",
                "8.8K%",
                "876%",
                "88%",
                "8.8%",
                "0%");

        // ICU-20923
        assertFormatDescendingBig(
                "Compact Percent with Scale",
                "compact-short percent scale/100",
                "K %x100",
                NumberFormatter.with()
                        .notation(Notation.compactShort())
                        .unit(NoUnit.PERCENT)
                        .scale(Scale.powerOfTen(2)),
                ULocale.ENGLISH,
                "8.8B%",
                "876M%",
                "88M%",
                "8.8M%",
                "876K%",
                "88K%",
                "8.8K%",
                "876%",
                "0%");

        // ICU-20923
        assertFormatDescendingBig(
                "Compact Percent Long Name",
                "compact-short percent unit-width-full-name",
                "K % unit-width-full-name",
                NumberFormatter.with()
                        .notation(Notation.compactShort())
                        .unit(NoUnit.PERCENT)
                        .unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                "88M percent",
                "8.8M percent",
                "876K percent",
                "88K percent",
                "8.8K percent",
                "876 percent",
                "88 percent",
                "8.8 percent",
                "0 percent");

        assertFormatSingle(
                "Per Percent",
                "measure-unit/length-meter per-measure-unit/concentr-percent unit-width-full-name",
                "measure-unit/length-meter per-measure-unit/concentr-percent unit-width-full-name",
                NumberFormatter.with()
                        .unit(MeasureUnit.METER)
                        .perUnit(MeasureUnit.PERCENT)
                        .unitWidth(UnitWidth.FULL_NAME),
                ULocale.ENGLISH,
                50,
                "50 meters per percent");

    }

    @Test
    public void roundingFraction() {
        assertFormatDescending(
                "Integer",
                "precision-integer",
                ".",
                NumberFormatter.with().precision(Precision.integer()),
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
                ".000",
                NumberFormatter.with().precision(Precision.fixedFraction(3)),
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
                ".0*",
                ".0+",
                NumberFormatter.with().precision(Precision.minFraction(1)),
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
                ".#",
                NumberFormatter.with().precision(Precision.maxFraction(1)),
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
                ".0##",
                NumberFormatter.with().precision(Precision.minMaxFraction(1, 3)),
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
                "@@@",
                NumberFormatter.with().precision(Precision.fixedSignificantDigits(3)),
                ULocale.ENGLISH,
                -98,
                "-98.0");

        assertFormatSingle(
                "Fixed Significant Rounding",
                "@@@",
                "@@@",
                NumberFormatter.with().precision(Precision.fixedSignificantDigits(3)),
                ULocale.ENGLISH,
                -98.7654321,
                "-98.8");

        assertFormatSingle(
                "Fixed Significant Zero",
                "@@@",
                "@@@",
                NumberFormatter.with().precision(Precision.fixedSignificantDigits(3)),
                ULocale.ENGLISH,
                0,
                "0.00");

        assertFormatSingle(
                "Min Significant",
                "@@*",
                "@@+",
                NumberFormatter.with().precision(Precision.minSignificantDigits(2)),
                ULocale.ENGLISH,
                -9,
                "-9.0");

        assertFormatSingle(
                "Max Significant",
                "@###",
                "@###",
                NumberFormatter.with().precision(Precision.maxSignificantDigits(4)),
                ULocale.ENGLISH,
                98.7654321,
                "98.77");

        assertFormatSingle(
                "Min/Max Significant",
                "@@@#",
                "@@@#",
                NumberFormatter.with().precision(Precision.minMaxSignificantDigits(3, 4)),
                ULocale.ENGLISH,
                9.99999,
                "10.0");

        assertFormatSingle(
                "Fixed Significant on zero with zero integer width",
                "@ integer-width/*",
                "@ integer-width/+",
                NumberFormatter.with().precision(Precision.fixedSignificantDigits(1)).integerWidth(IntegerWidth.zeroFillTo(0)),
                ULocale.ENGLISH,
                0,
                "0");

        assertFormatSingle(
                "Fixed Significant on zero with lots of integer width",
                "@ integer-width/+000",
                "@ 000",
                NumberFormatter.with().precision(Precision.fixedSignificantDigits(1)).integerWidth(IntegerWidth.zeroFillTo(3)),
                ULocale.ENGLISH,
                0,
                "000");
    }

    @Test
    public void roundingFractionFigures() {
        assertFormatDescending(
                "Basic Significant", // for comparison
                "@#",
                "@#",
                NumberFormatter.with().precision(Precision.maxSignificantDigits(2)),
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
                ".0#/@@@*",
                ".0#/@@@+",
                NumberFormatter.with().precision(Precision.minMaxFraction(1, 2).withMinDigits(3)),
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
                ".0##/@#",
                NumberFormatter.with().precision(Precision.minMaxFraction(1, 3).withMaxDigits(2)),
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
                ".00/@#",
                NumberFormatter.with().precision(Precision.fixedFraction(2).withMaxDigits(2)),
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

        assertFormatDescending(
                "FracSig minFrac maxSig",
                ".0+/@#",
                ".0+/@#",
                NumberFormatter.with().precision(Precision.minFraction(1).withMaxDigits(2)),
                ULocale.ENGLISH,
                "88,000.0",
                "8,800.0",
                "880.0",
                "88.0",
                "8.8",
                "0.88",
                "0.088",
                "0.0088",
                "0.0");

        assertFormatSingle(
                "FracSig with trailing zeros A",
                ".00/@@@*",
                ".00/@@@+",
                NumberFormatter.with().precision(Precision.fixedFraction(2).withMinDigits(3)),
                ULocale.ENGLISH,
                0.1,
                "0.10");

        assertFormatSingle(
                "FracSig with trailing zeros B",
                ".00/@@@*",
                ".00/@@@+",
                NumberFormatter.with().precision(Precision.fixedFraction(2).withMinDigits(3)),
                ULocale.ENGLISH,
                0.0999999,
                "0.10");
    }

    @Test
    public void roundingOther() {
        assertFormatDescending(
                "Rounding None",
                "precision-unlimited",
                ".+",
                NumberFormatter.with().precision(Precision.unlimited()),
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
                "precision-increment/0.5",
                "precision-increment/0.5",
                NumberFormatter.with().precision(Precision.increment(BigDecimal.valueOf(0.5))),
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
                "precision-increment/0.50",
                "precision-increment/0.50",
                NumberFormatter.with().precision(Precision.increment(new BigDecimal("0.50"))),
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
                "Strange Increment",
                "precision-increment/3.140",
                "precision-increment/3.140",
                NumberFormatter.with().precision(Precision.increment(new BigDecimal("3.140"))),
                ULocale.ENGLISH,
                "87,649.960",
                "8,763.740",
                "876.060",
                "87.920",
                "9.420",
                "0.000",
                "0.000",
                "0.000",
                "0.000");

        assertFormatDescending(
                "Increment Resolving to Power of 10",
                "precision-increment/0.010",
                "precision-increment/0.010",
                NumberFormatter.with().precision(Precision.increment(new BigDecimal("0.010"))),
                ULocale.ENGLISH,
                "87,650.000",
                "8,765.000",
                "876.500",
                "87.650",
                "8.760",
                "0.880",
                "0.090",
                "0.010",
                "0.000");

        assertFormatDescending(
                "Currency Standard",
                "currency/CZK precision-currency-standard",
                "currency/CZK precision-currency-standard",
                NumberFormatter.with().precision(Precision.currency(CurrencyUsage.STANDARD)).unit(CZK),
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
                "currency/CZK precision-currency-cash",
                "currency/CZK precision-currency-cash",
                NumberFormatter.with().precision(Precision.currency(CurrencyUsage.CASH)).unit(CZK),
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
                "currency/CAD precision-currency-cash",
                "currency/CAD precision-currency-cash",
                NumberFormatter.with().precision(Precision.currency(CurrencyUsage.CASH)).unit(CAD),
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
                "precision-integer", // calling .withCurrency() applies currency rounding rules immediately
                ".",
                NumberFormatter.with().precision(Precision.currency(CurrencyUsage.CASH).withCurrency(CZK)),
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
                "Rounding Mode CEILING",
                "precision-integer rounding-mode-ceiling",
                ". rounding-mode-ceiling",
                NumberFormatter.with().precision(Precision.integer()).roundingMode(RoundingMode.CEILING),
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

        assertFormatSingle(
                "ICU-20974 Double.MIN_NORMAL",
                "scientific",
                "E0",
                NumberFormatter.with().notation(Notation.scientific()),
                ULocale.ENGLISH,
                Double.MIN_NORMAL,
                "2.225074E-308");

        assertFormatSingle(
                "ICU-20974 Double.MIN_VALUE",
                "scientific",
                "E0",
                NumberFormatter.with().notation(Notation.scientific()),
                ULocale.ENGLISH,
                Double.MIN_VALUE,
                "4.9E-324");
    }

    @Test
    public void grouping() {
        assertFormatDescendingBig(
                "Western Grouping",
                "group-auto",
                "",
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
                "group-auto",
                "",
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
                "group-min2",
                ",?",
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
                "group-min2",
                ",?",
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
                "group-off",
                ",_",
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
                "group-thousands",
                "group-thousands",
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

        // NOTE: Polish is interesting because it has minimumGroupingDigits=2 in locale data
        // (Most locales have either 1 or 2)
        // If this test breaks due to data changes, find another locale that has minimumGroupingDigits.
        assertFormatDescendingBig(
                "Polish Grouping",
                "group-auto",
                "",
                NumberFormatter.with().grouping(GroupingStrategy.AUTO),
                new ULocale("pl"),
                "87 650 000",
                "8 765 000",
                "876 500",
                "87 650",
                "8765",
                "876,5",
                "87,65",
                "8,765",
                "0");

        assertFormatDescendingBig(
                "Polish Grouping, Min 2",
                "group-min2",
                ",?",
                NumberFormatter.with().grouping(GroupingStrategy.MIN2),
                new ULocale("pl"),
                "87 650 000",
                "8 765 000",
                "876 500",
                "87 650",
                "8765",
                "876,5",
                "87,65",
                "8,765",
                "0");

        assertFormatDescendingBig(
                "Polish Grouping, Always",
                "group-on-aligned",
                ",!",
                NumberFormatter.with().grouping(GroupingStrategy.ON_ALIGNED),
                new ULocale("pl"),
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
                "currency/USD group-auto",
                "currency/USD",
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
                "currency/USD group-on-aligned",
                "currency/USD ,!",
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
                null,
                null,
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
                null,
                null,
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
                null,
                null,
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
                null,
                null,
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
                null,
                null,
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
                null,
                null,
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
                null,
                null,
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.BEFORE_PREFIX)),
                ULocale.ENGLISH,
                -88.88,
                "**-88.88");

        assertFormatSingle(
                "Pad After Prefix",
                null,
                null,
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.AFTER_PREFIX)),
                ULocale.ENGLISH,
                -88.88,
                "-**88.88");

        assertFormatSingle(
                "Pad Before Suffix",
                null,
                null,
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.BEFORE_SUFFIX))
                        .unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                88.88,
                "88.88**%");

        assertFormatSingle(
                "Pad After Suffix",
                null,
                null,
                NumberFormatter.with().padding(Padder.codePoints('*', 8, PadPosition.AFTER_SUFFIX))
                        .unit(NoUnit.PERCENT),
                ULocale.ENGLISH,
                88.88,
                "88.88%**");

        assertFormatSingle(
                "Currency Spacing with Zero Digit Padding Broken",
                null,
                null,
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
                "integer-width/+0",
                "0",
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
                "integer-width/*",
                "integer-width/+",
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
                "0"); // see ICU-20844

        assertFormatDescending(
                "Integer Width Zero Fill 3",
                "integer-width/+000",
                "000",
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
                "integer-width/##0",
                "integer-width/##0",
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
                "integer-width/00",
                "integer-width/00",
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

        assertFormatDescending(
                "Integer Width Compact",
                "compact-short integer-width/000",
                "K integer-width/000",
                NumberFormatter.with()
                    .notation(Notation.compactShort())
                    .integerWidth(IntegerWidth.zeroFillTo(3).truncateAt(3)),
                ULocale.ENGLISH,
                "088K",
                "008.8K",
                "876",
                "088",
                "008.8",
                "000.88",
                "000.088",
                "000.0088",
                "000");

        assertFormatDescending(
                "Integer Width Scientific",
                "scientific integer-width/000",
                "E0 integer-width/000",
                NumberFormatter.with()
                    .notation(Notation.scientific())
                    .integerWidth(IntegerWidth.zeroFillTo(3).truncateAt(3)),
                ULocale.ENGLISH,
                "008.765E4",
                "008.765E3",
                "008.765E2",
                "008.765E1",
                "008.765E0",
                "008.765E-1",
                "008.765E-2",
                "008.765E-3",
                "000E0");

        assertFormatDescending(
                "Integer Width Engineering",
                "engineering integer-width/000",
                "EE0 integer-width/000",
                NumberFormatter.with()
                    .notation(Notation.engineering())
                    .integerWidth(IntegerWidth.zeroFillTo(3).truncateAt(3)),
                ULocale.ENGLISH,
                "087.65E3",
                "008.765E3",
                "876.5E0",
                "087.65E0",
                "008.765E0",
                "876.5E-3",
                "087.65E-3",
                "008.765E-3",
                "000E0");

        assertFormatSingle(
                "Integer Width Remove All A",
                "integer-width/00",
                "integer-width/00",
                NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(2).truncateAt(2)),
                ULocale.ENGLISH,
                2500,
                "00");

        assertFormatSingle(
                "Integer Width Remove All B",
                "integer-width/00",
                "integer-width/00",
                NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(2).truncateAt(2)),
                ULocale.ENGLISH,
                25000,
                "00");

        assertFormatSingle(
                "Integer Width Remove All B, Bytes Mode",
                "integer-width/00",
                "integer-width/00",
                NumberFormatter.with().integerWidth(IntegerWidth.zeroFillTo(2).truncateAt(2)),
                ULocale.ENGLISH,
                // Note: this double produces all 17 significant digits
                10000000000000002000.0,
                "00");
    }

    @Test
    public void symbols() {
        assertFormatDescending(
                "French Symbols with Japanese Data 1",
                null,
                null,
                NumberFormatter.with().symbols(DecimalFormatSymbols.getInstance(ULocale.FRENCH)),
                ULocale.JAPAN,
                "87\u202F650",
                "8\u202F765",
                "876,5",
                "87,65",
                "8,765",
                "0,8765",
                "0,08765",
                "0,008765",
                "0");

        assertFormatSingle(
                "French Symbols with Japanese Data 2",
                null,
                null,
                NumberFormatter.with().notation(Notation.compactShort())
                        .symbols(DecimalFormatSymbols.getInstance(ULocale.FRENCH)),
                ULocale.JAPAN,
                12345,
                "1,2\u4E07");

        assertFormatDescending(
                "Latin Numbering System with Arabic Data",
                "currency/USD latin",
                "currency/USD latin",
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
                "numbering-system/mathsanb",
                "numbering-system/mathsanb",
                NumberFormatter.with().symbols(NumberingSystem.getInstanceByName("mathsanb")),
                ULocale.FRENCH,
                "𝟴𝟳\u202f𝟲𝟱𝟬",
                "𝟴\u202f𝟳𝟲𝟱",
                "𝟴𝟳𝟲,𝟱",
                "𝟴𝟳,𝟲𝟱",
                "𝟴,𝟳𝟲𝟱",
                "𝟬,𝟴𝟳𝟲𝟱",
                "𝟬,𝟬𝟴𝟳𝟲𝟱",
                "𝟬,𝟬𝟬𝟴𝟳𝟲𝟱",
                "𝟬");

        assertFormatSingle(
                "Swiss Symbols (used in documentation)",
                null,
                null,
                NumberFormatter.with().symbols(DecimalFormatSymbols.getInstance(new ULocale("de-CH"))),
                ULocale.ENGLISH,
                12345.67,
                "12’345.67");

        assertFormatSingle(
                "Myanmar Symbols (used in documentation)",
                null,
                null,
                NumberFormatter.with().symbols(DecimalFormatSymbols.getInstance(new ULocale("my_MY"))),
                ULocale.ENGLISH,
                12345.67,
                "\u1041\u1042,\u1043\u1044\u1045.\u1046\u1047");

        // NOTE: Locale ar puts ¤ after the number in NS arab but before the number in NS latn.

        assertFormatSingle(
                "Currency symbol should precede number in ar with NS latn",
                "currency/USD latin",
                "currency/USD latin",
                NumberFormatter.with().symbols(NumberingSystem.LATIN).unit(USD),
                new ULocale("ar"),
                12345.67,
                "US$ 12,345.67");

        assertFormatSingle(
                "Currency symbol should precede number in ar@numbers=latn",
                "currency/USD",
                "currency/USD",
                NumberFormatter.with().unit(USD),
                new ULocale("ar@numbers=latn"),
                12345.67,
                "US$ 12,345.67");

        assertFormatSingle(
                "Currency symbol should follow number in ar-EG with NS arab",
                "currency/USD",
                "currency/USD",
                NumberFormatter.with().unit(USD),
                new ULocale("ar-EG"),
                12345.67,
                "١٢٬٣٤٥٫٦٧ US$");

        assertFormatSingle(
                "Currency symbol should follow number in ar@numbers=arab",
                "currency/USD",
                "currency/USD",
                NumberFormatter.with().unit(USD),
                new ULocale("ar@numbers=arab"),
                12345.67,
                "١٢٬٣٤٥٫٦٧ US$");

        assertFormatSingle(
                "NumberingSystem in API should win over @numbers keyword",
                "currency/USD latin",
                "currency/USD latin",
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
                null,
                null,
                f,
                ULocale.ENGLISH,
                12345.67,
                "12’345.67");

        assertFormatSingle(
                "The last symbols setter wins",
                "latin",
                "latin",
                NumberFormatter.with().symbols(symbols).symbols(NumberingSystem.LATIN),
                ULocale.ENGLISH,
                12345.67,
                "12,345.67");

        assertFormatSingle(
                "The last symbols setter wins",
                null,
                null,
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
                "sign-auto",
                "",
                NumberFormatter.with().sign(SignDisplay.AUTO),
                ULocale.ENGLISH,
                444444,
                "444,444");

        assertFormatSingle(
                "Sign Auto Negative",
                "sign-auto",
                "",
                NumberFormatter.with().sign(SignDisplay.AUTO),
                ULocale.ENGLISH,
                -444444,
                "-444,444");

        assertFormatSingle(
                "Sign Auto Zero",
                "sign-auto",
                "",
                NumberFormatter.with().sign(SignDisplay.AUTO),
                ULocale.ENGLISH,
                0,
                "0");

        assertFormatSingle(
                "Sign Always Positive",
                "sign-always",
                "+!",
                NumberFormatter.with().sign(SignDisplay.ALWAYS),
                ULocale.ENGLISH,
                444444,
                "+444,444");

        assertFormatSingle(
                "Sign Always Negative",
                "sign-always",
                "+!",
                NumberFormatter.with().sign(SignDisplay.ALWAYS),
                ULocale.ENGLISH,
                -444444,
                "-444,444");

        assertFormatSingle(
                "Sign Always Zero",
                "sign-always",
                "+!",
                NumberFormatter.with().sign(SignDisplay.ALWAYS),
                ULocale.ENGLISH,
                0,
                "+0");

        assertFormatSingle(
                "Sign Never Positive",
                "sign-never",
                "+_",
                NumberFormatter.with().sign(SignDisplay.NEVER),
                ULocale.ENGLISH,
                444444,
                "444,444");

        assertFormatSingle(
                "Sign Never Negative",
                "sign-never",
                "+_",
                NumberFormatter.with().sign(SignDisplay.NEVER),
                ULocale.ENGLISH,
                -444444,
                "444,444");

        assertFormatSingle(
                "Sign Never Zero",
                "sign-never",
                "+_",
                NumberFormatter.with().sign(SignDisplay.NEVER),
                ULocale.ENGLISH,
                0,
                "0");

        assertFormatSingle(
                "Sign Accounting Positive",
                "currency/USD sign-accounting",
                "currency/USD ()",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD),
                ULocale.ENGLISH,
                444444,
                "$444,444.00");

        assertFormatSingle(
                "Sign Accounting Negative",
                "currency/USD sign-accounting",
                "currency/USD ()",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD),
                ULocale.ENGLISH,
                -444444,
                "($444,444.00)");

        assertFormatSingle(
                "Sign Accounting Zero",
                "currency/USD sign-accounting",
                "currency/USD ()",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD),
                ULocale.ENGLISH,
                0,
                "$0.00");

        assertFormatSingle(
                "Sign Accounting-Always Positive",
                "currency/USD sign-accounting-always",
                "currency/USD ()!",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_ALWAYS).unit(USD),
                ULocale.ENGLISH,
                444444,
                "+$444,444.00");

        assertFormatSingle(
                "Sign Accounting-Always Negative",
                "currency/USD sign-accounting-always",
                "currency/USD ()!",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_ALWAYS).unit(USD),
                ULocale.ENGLISH,
                -444444,
                "($444,444.00)");

        assertFormatSingle(
                "Sign Accounting-Always Zero",
                "currency/USD sign-accounting-always",
                "currency/USD ()!",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_ALWAYS).unit(USD),
                ULocale.ENGLISH,
                0,
                "+$0.00");

        assertFormatSingle(
                "Sign Except-Zero Positive",
                "sign-except-zero",
                "+?",
                NumberFormatter.with().sign(SignDisplay.EXCEPT_ZERO),
                ULocale.ENGLISH,
                444444,
                "+444,444");

        assertFormatSingle(
                "Sign Except-Zero Negative",
                "sign-except-zero",
                "+?",
                NumberFormatter.with().sign(SignDisplay.EXCEPT_ZERO),
                ULocale.ENGLISH,
                -444444,
                "-444,444");

        assertFormatSingle(
                "Sign Except-Zero Zero",
                "sign-except-zero",
                "+?",
                NumberFormatter.with().sign(SignDisplay.EXCEPT_ZERO),
                ULocale.ENGLISH,
                0,
                "0");

        assertFormatSingle(
                "Sign Accounting-Except-Zero Positive",
                "currency/USD sign-accounting-except-zero",
                "currency/USD ()?",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_EXCEPT_ZERO).unit(USD),
                ULocale.ENGLISH,
                444444,
                "+$444,444.00");

        assertFormatSingle(
                "Sign Accounting-Except-Zero Negative",
                "currency/USD sign-accounting-except-zero",
                "currency/USD ()?",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_EXCEPT_ZERO).unit(USD),
                ULocale.ENGLISH,
                -444444,
                "($444,444.00)");

        assertFormatSingle(
                "Sign Accounting-Except-Zero Zero",
                "currency/USD sign-accounting-except-zero",
                "currency/USD ()?",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING_EXCEPT_ZERO).unit(USD),
                ULocale.ENGLISH,
                0,
                "$0.00");

        assertFormatSingle(
                "Sign Accounting Negative Hidden",
                "currency/USD unit-width-hidden sign-accounting",
                "currency/USD unit-width-hidden ()",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD).unitWidth(UnitWidth.HIDDEN),
                ULocale.ENGLISH,
                -444444,
                "(444,444.00)");

        assertFormatSingle(
                "Sign Accounting Negative Narrow",
                "currency/USD unit-width-narrow sign-accounting",
                "currency/USD unit-width-narrow ()",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD).unitWidth(UnitWidth.NARROW),
                ULocale.CANADA,
                -444444,
                "($444,444.00)");

        assertFormatSingle(
                "Sign Accounting Negative Short",
                "currency/USD sign-accounting",
                "currency/USD ()",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD).unitWidth(UnitWidth.SHORT),
                ULocale.CANADA,
                -444444,
                "(US$444,444.00)");

        assertFormatSingle(
                "Sign Accounting Negative Iso Code",
                "currency/USD unit-width-iso-code sign-accounting",
                "currency/USD unit-width-iso-code ()",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD).unitWidth(UnitWidth.ISO_CODE),
                ULocale.CANADA,
                -444444,
                "(USD 444,444.00)");

        // Note: CLDR does not provide an accounting pattern for long name currency.
        // We fall back to normal currency format. This may change in the future.
        assertFormatSingle(
                "Sign Accounting Negative Full Name",
                "currency/USD unit-width-full-name sign-accounting",
                "currency/USD unit-width-full-name ()",
                NumberFormatter.with().sign(SignDisplay.ACCOUNTING).unit(USD).unitWidth(UnitWidth.FULL_NAME),
                ULocale.CANADA,
                -444444,
                "-444,444.00 US dollars");
    }

    @Test
    public void signNearZero() {
        // https://unicode-org.atlassian.net/browse/ICU-20709
        Object[][] cases = {
            { SignDisplay.AUTO,  1.1, "1" },
            { SignDisplay.AUTO,  0.9, "1" },
            { SignDisplay.AUTO,  0.1, "0" },
            { SignDisplay.AUTO, -0.1, "-0" }, // interesting case
            { SignDisplay.AUTO, -0.9, "-1" },
            { SignDisplay.AUTO, -1.1, "-1" },
            { SignDisplay.ALWAYS,  1.1, "+1" },
            { SignDisplay.ALWAYS,  0.9, "+1" },
            { SignDisplay.ALWAYS,  0.1, "+0" },
            { SignDisplay.ALWAYS, -0.1, "-0" },
            { SignDisplay.ALWAYS, -0.9, "-1" },
            { SignDisplay.ALWAYS, -1.1, "-1" },
            { SignDisplay.EXCEPT_ZERO,  1.1, "+1" },
            { SignDisplay.EXCEPT_ZERO,  0.9, "+1" },
            { SignDisplay.EXCEPT_ZERO,  0.1, "0" }, // interesting case
            { SignDisplay.EXCEPT_ZERO, -0.1, "0" }, // interesting case
            { SignDisplay.EXCEPT_ZERO, -0.9, "-1" },
            { SignDisplay.EXCEPT_ZERO, -1.1, "-1" },
        };
        for (Object[] cas : cases) {
            SignDisplay sign = (SignDisplay) cas[0];
            double input = (Double) cas[1];
            String expected = (String) cas[2];
            String actual = NumberFormatter.with()
                .sign(sign)
                .precision(Precision.integer())
                .locale(Locale.US)
                .format(input)
                .toString();
            assertEquals(
                input + " @ SignDisplay " + sign,
                expected, actual);
        }
    }

    @Test
    public void signCoverage() {
        // https://unicode-org.atlassian.net/browse/ICU-20708
        Object[][][] cases = new Object[][][] {
            { {SignDisplay.AUTO}, { "-∞", "-1", "-0", "0", "1", "∞", "NaN", "-NaN" } },
            { {SignDisplay.ALWAYS}, { "-∞", "-1", "-0", "+0", "+1", "+∞", "+NaN", "-NaN" } },
            { {SignDisplay.NEVER}, { "∞", "1", "0", "0", "1", "∞", "NaN", "NaN" } },
            { {SignDisplay.EXCEPT_ZERO}, { "-∞", "-1", "0", "0", "+1", "+∞", "NaN", "NaN" } },
        };
        double negNaN = Math.copySign(Double.NaN, -0.0);
        double inputs[] = new double[] {
            Double.NEGATIVE_INFINITY, -1, -0.0, 0, 1, Double.POSITIVE_INFINITY, Double.NaN, negNaN
        };
        for (Object[][] cas : cases) {
            SignDisplay sign = (SignDisplay) cas[0][0];
            for (int i = 0; i < inputs.length; i++) {
                double input = inputs[i];
                String expected = (String) cas[1][i];
                String actual = NumberFormatter.with()
                    .sign(sign)
                    .locale(Locale.US)
                    .format(input)
                    .toString();
                assertEquals(
                    input + " " + sign,
                    expected, actual);
            }
        }
    }

    @Test
    public void decimal() {
        assertFormatDescending(
                "Decimal Default",
                "decimal-auto",
                "",
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
                "decimal-always",
                "decimal-always",
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
    public void scale() {
        assertFormatDescending(
                "Multiplier None",
                "scale/1",
                "",
                NumberFormatter.with().scale(Scale.none()),
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
                "Multiplier Power of Ten",
                "scale/1000000",
                "scale/1000000",
                NumberFormatter.with().scale(Scale.powerOfTen(6)),
                ULocale.ENGLISH,
                "87,650,000,000",
                "8,765,000,000",
                "876,500,000",
                "87,650,000",
                "8,765,000",
                "876,500",
                "87,650",
                "8,765",
                "0");

        assertFormatDescending(
                "Multiplier Arbitrary Double",
                "scale/5.2",
                "scale/5.2",
                NumberFormatter.with().scale(Scale.byDouble(5.2)),
                ULocale.ENGLISH,
                "455,780",
                "45,578",
                "4,557.8",
                "455.78",
                "45.578",
                "4.5578",
                "0.45578",
                "0.045578",
                "0");

        assertFormatDescending(
                "Multiplier Arbitrary BigDecimal",
                "scale/5.2",
                "scale/5.2",
                NumberFormatter.with().scale(Scale.byBigDecimal(new BigDecimal("5.2"))),
                ULocale.ENGLISH,
                "455,780",
                "45,578",
                "4,557.8",
                "455.78",
                "45.578",
                "4.5578",
                "0.45578",
                "0.045578",
                "0");

        assertFormatDescending(
                "Multiplier Arbitrary Double And Power Of Ten",
                "scale/5200",
                "scale/5200",
                NumberFormatter.with().scale(Scale.byDoubleAndPowerOfTen(5.2, 3)),
                ULocale.ENGLISH,
                "455,780,000",
                "45,578,000",
                "4,557,800",
                "455,780",
                "45,578",
                "4,557.8",
                "455.78",
                "45.578",
                "0");

        assertFormatDescending(
                "Multiplier Zero",
                "scale/0",
                "scale/0",
                NumberFormatter.with().scale(Scale.byDouble(0)),
                ULocale.ENGLISH,
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0");

        assertFormatSingle(
                "Multiplier Skeleton Scientific Notation and Percent",
                "percent scale/1E2",
                "%x100",
                NumberFormatter.with().unit(NoUnit.PERCENT).scale(Scale.powerOfTen(2)),
                ULocale.ENGLISH,
                0.5,
                "50%");

        assertFormatSingle(
                "Negative Multiplier",
                "scale/-5.2",
                "scale/-5.2",
                NumberFormatter.with().scale(Scale.byDouble(-5.2)),
                ULocale.ENGLISH,
                2,
                "-10.4");

        assertFormatSingle(
                "Negative One Multiplier",
                "scale/-1",
                "scale/-1",
                NumberFormatter.with().scale(Scale.byDouble(-1)),
                ULocale.ENGLISH,
                444444,
                "-444,444");

        assertFormatSingle(
                "Two-Type Multiplier with Overlap",
                "scale/10000",
                "scale/10000",
                NumberFormatter.with().scale(Scale.byDoubleAndPowerOfTen(100, 2)),
                ULocale.ENGLISH,
                2,
                "20,000");
    }

    @Test
    public void locale() {
        // Coverage for the locale setters.
        Assert.assertEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.with().locale(Locale.ENGLISH));
        Assert.assertEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.withLocale(ULocale.ENGLISH));
        Assert.assertEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.withLocale(Locale.ENGLISH));
        Assert.assertNotEquals(NumberFormatter.with().locale(ULocale.ENGLISH), NumberFormatter.with().locale(Locale.FRENCH));
    }

    @Test
    public void formatTypes() {
        LocalizedNumberFormatter formatter = NumberFormatter.withLocale(ULocale.ENGLISH);

        // Double
        Assert.assertEquals("514.23", formatter.format(514.23).toString());

        // Int64
        Assert.assertEquals("51,423", formatter.format(51423L).toString());

        // BigDecimal
        Assert.assertEquals("987,654,321,234,567,890",
                formatter.format(new BigDecimal("98765432123456789E1")).toString());

        // Also test proper DecimalQuantity bytes storage when all digits are in the fraction.
        // The number needs to have exactly 40 digits, which is the size of the default buffer.
        // (issue discovered by the address sanitizer in C++)
        Assert.assertEquals("0.009876543210987654321098765432109876543211",
                formatter.precision(Precision.unlimited())
                        .format(new BigDecimal("0.009876543210987654321098765432109876543211"))
                        .toString());
    }

    @Test
    public void fieldPositionLogic() {
        String message = "Field position logic test";

        FormattedNumber fmtd = assertFormatSingle(
                message,
                "",
                "",
                NumberFormatter.with(),
                ULocale.ENGLISH,
                -9876543210.12,
                "-9,876,543,210.12");

        Object[][] expectedFieldPositions = new Object[][]{
                {NumberFormat.Field.SIGN, 0, 1},
                {NumberFormat.Field.GROUPING_SEPARATOR, 2, 3},
                {NumberFormat.Field.GROUPING_SEPARATOR, 6, 7},
                {NumberFormat.Field.GROUPING_SEPARATOR, 10, 11},
                {NumberFormat.Field.INTEGER, 1, 14},
                {NumberFormat.Field.DECIMAL_SEPARATOR, 14, 15},
                {NumberFormat.Field.FRACTION, 15, 17}};

        assertNumberFieldPositions(message, fmtd, expectedFieldPositions);

        // Test the iteration functionality of nextFieldPosition
        ConstrainedFieldPosition actual = new ConstrainedFieldPosition();
        actual.constrainField(NumberFormat.Field.GROUPING_SEPARATOR);
        int i = 1;
        while (fmtd.nextPosition(actual)) {
            Object[] cas = expectedFieldPositions[i++];
            NumberFormat.Field expectedField = (NumberFormat.Field) cas[0];
            int expectedBeginIndex = (Integer) cas[1];
            int expectedEndIndex = (Integer) cas[2];

            assertEquals(
                    "Next for grouping, field, case #" + i,
                    expectedField,
                    actual.getField());
            assertEquals(
                    "Next for grouping, begin index, case #" + i,
                    expectedBeginIndex,
                    actual.getStart());
            assertEquals(
                    "Next for grouping, end index, case #" + i,
                    expectedEndIndex,
                    actual.getLimit());
        }
        assertEquals("Should have seen all grouping separators", 4, i);

        // Make sure strings without fraction do not contain fraction field
        actual.reset();
        actual.constrainField(NumberFormat.Field.FRACTION);
        fmtd = NumberFormatter.withLocale(ULocale.ENGLISH).format(5);
        assertFalse("No fraction part in an integer", fmtd.nextPosition(actual));
    }

    @Test
    public void fieldPositionCoverage() {
        {
            String message = "Measure unit field position basic";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "measure-unit/temperature-fahrenheit",
                    "unit/fahrenheit",
                    NumberFormatter.with().unit(MeasureUnit.FAHRENHEIT),
                    ULocale.ENGLISH,
                    68,
                    "68°F");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 2},
                    {NumberFormat.Field.MEASURE_UNIT, 2, 4}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Measure unit field position with compound unit";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "measure-unit/temperature-fahrenheit per-measure-unit/duration-day",
                    "unit/fahrenheit-per-day",
                    NumberFormatter.with().unit(MeasureUnit.FAHRENHEIT).perUnit(MeasureUnit.DAY),
                    ULocale.ENGLISH,
                    68,
                    "68°F/d");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 2},
                    {NumberFormat.Field.MEASURE_UNIT, 2, 6}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Measure unit field position with spaces";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "measure-unit/length-meter unit-width-full-name",
                    "unit/meter unit-width-full-name",
                    NumberFormatter.with().unit(MeasureUnit.METER).unitWidth(UnitWidth.FULL_NAME),
                    ULocale.ENGLISH,
                    68,
                    "68 meters");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 2},
                    // note: field starts after the space
                    {NumberFormat.Field.MEASURE_UNIT, 3, 9}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Measure unit field position with prefix and suffix, composed m/s";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "measure-unit/length-meter per-measure-unit/duration-second unit-width-full-name",
                    "measure-unit/length-meter per-measure-unit/duration-second unit-width-full-name",
                    NumberFormatter.with().unit(MeasureUnit.METER).perUnit(MeasureUnit.SECOND).unitWidth(UnitWidth.FULL_NAME),
                    new ULocale("ky"), // locale with the interesting data
                    68,
                    "секундасына 68 метр");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.MEASURE_UNIT, 0, 11},
                    {NumberFormat.Field.INTEGER, 12, 14},
                    {NumberFormat.Field.MEASURE_UNIT, 15, 19}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Measure unit field position with prefix and suffix, built-in m/s";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "measure-unit/speed-meter-per-second unit-width-full-name",
                    "unit/meter-per-second unit-width-full-name",
                    NumberFormatter.with().unit(MeasureUnit.METER_PER_SECOND).unitWidth(UnitWidth.FULL_NAME),
                    new ULocale("ky"), // locale with the interesting data
                    68,
                    "секундасына 68 метр");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.MEASURE_UNIT, 0, 11},
                    {NumberFormat.Field.INTEGER, 12, 14},
                    {NumberFormat.Field.MEASURE_UNIT, 15, 19}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Measure unit field position with inner spaces";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "measure-unit/temperature-fahrenheit unit-width-full-name",
                    "unit/fahrenheit unit-width-full-name",
                    NumberFormatter.with().unit(MeasureUnit.FAHRENHEIT).unitWidth(UnitWidth.FULL_NAME),
                    new ULocale("vi"), // locale with the interesting data
                    68,
                    "68 độ F");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 2},
                    // Should trim leading/trailing spaces, but not inner spaces:
                    {NumberFormat.Field.MEASURE_UNIT, 3, 7}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            // Data: other{"‎{0} K"} == "\u200E{0} K"
            // If that data changes, try to find another example of a non-empty unit prefix/suffix
            // that is also all ignorables (whitespace and bidi control marks).
            String message = "Measure unit field position with fully ignorable prefix";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "measure-unit/temperature-kelvin",
                    "unit/kelvin",
                    NumberFormatter.with().unit(MeasureUnit.KELVIN),
                    new ULocale("fa"), // locale with the interesting data
                    68,
                    "‎۶۸ K");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 1, 3},
                    {NumberFormat.Field.MEASURE_UNIT, 4, 5}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Compact field basic";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "compact-short",
                    "K",
                    NumberFormatter.with().notation(Notation.compactShort()),
                    ULocale.US,
                    65000,
                    "65K");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 2},
                    {NumberFormat.Field.COMPACT, 2, 3}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Compact field with spaces";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "compact-long",
                    "KK",
                    NumberFormatter.with().notation(Notation.compactLong()),
                    ULocale.US,
                    65000,
                    "65 thousand");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 2},
                    {NumberFormat.Field.COMPACT, 3, 11}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Compact field with inner space";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "compact-long",
                    "KK",
                    NumberFormatter.with().notation(Notation.compactLong()),
                    new ULocale("fil"),  // locale with interesting data
                    6000,
                    "6 na libo");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 1},
                    {NumberFormat.Field.COMPACT, 2, 9}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Compact field with bidi mark";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "compact-long",
                    "KK",
                    NumberFormatter.with().notation(Notation.compactLong()),
                    new ULocale("he"),  // locale with interesting data
                    6000,
                    "\u200F6 אלף");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 1, 2},
                    {NumberFormat.Field.COMPACT, 3, 6}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Compact with currency fields";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "compact-short currency/USD",
                    "K currency/USD",
                    NumberFormatter.with().notation(Notation.compactShort()).unit(USD),
                    new ULocale("sr_Latn"),  // locale with interesting data
                    65000,
                    "65 hilj. US$");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 2},
                    {NumberFormat.Field.COMPACT, 3, 8},
                    {NumberFormat.Field.CURRENCY, 9, 12}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Currency long name fields";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "currency/USD unit-width-full-name",
                    "currency/USD unit-width-full-name",
                    NumberFormatter.with().unit(USD)
                        .unitWidth(UnitWidth.FULL_NAME),
                    ULocale.ENGLISH,
                    12345,
                    "12,345.00 US dollars");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.GROUPING_SEPARATOR, 2, 3},
                    {NumberFormat.Field.INTEGER, 0, 6},
                    {NumberFormat.Field.DECIMAL_SEPARATOR, 6, 7},
                    {NumberFormat.Field.FRACTION, 7, 9},
                    {NumberFormat.Field.CURRENCY, 10, 20}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }

        {
            String message = "Compact with measure unit fields";
            FormattedNumber result = assertFormatSingle(
                    message,
                    "compact-long measure-unit/length-meter unit-width-full-name",
                    "KK unit/meter unit-width-full-name",
                    NumberFormatter.with().notation(Notation.compactLong())
                        .unit(MeasureUnit.METER)
                        .unitWidth(UnitWidth.FULL_NAME),
                    ULocale.US,
                    65000,
                    "65 thousand meters");
            Object[][] expectedFieldPositions = new Object[][] {
                    // field, begin index, end index
                    {NumberFormat.Field.INTEGER, 0, 2},
                    {NumberFormat.Field.COMPACT, 3, 11},
                    {NumberFormat.Field.MEASURE_UNIT, 12, 18}};
            assertNumberFieldPositions(
                    message,
                    result,
                    expectedFieldPositions);
        }
    }

    /** Handler for serialization compatibility test suite. */
    public static class FormatHandler implements SerializableTestUtility.Handler {
        @Override
        public Object[] getTestObjects() {
            return new Object[] {
                    NumberFormatter.withLocale(ULocale.FRENCH).toFormat(),
                    NumberFormatter.forSkeleton("percent").locale(ULocale.JAPANESE).toFormat(),
                    NumberFormatter.forSkeleton("scientific .000").locale(ULocale.ENGLISH).toFormat() };
        }

        @Override
        public boolean hasSameBehavior(Object a, Object b) {
            LocalizedNumberFormatterAsFormat f1 = (LocalizedNumberFormatterAsFormat) a;
            LocalizedNumberFormatterAsFormat f2 = (LocalizedNumberFormatterAsFormat) b;
            String s1 = f1.format(514.23);
            String s2 = f1.format(514.23);
            String k1 = f1.getNumberFormatter().toSkeleton();
            String k2 = f2.getNumberFormatter().toSkeleton();
            return s1.equals(s2) && k1.equals(k2);
        }
    }

    @Test
    public void toFormat() {
        LocalizedNumberFormatter lnf = NumberFormatter.withLocale(ULocale.FRENCH)
                .precision(Precision.fixedFraction(3));
        Format format = lnf.toFormat();
        FieldPosition fpos = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        StringBuffer sb = new StringBuffer();
        format.format(514.23, sb, fpos);
        assertEquals("Should correctly format number", "514,230", sb.toString());
        assertEquals("Should find decimal separator", 3, fpos.getBeginIndex());
        assertEquals("Should find end of decimal separator", 4, fpos.getEndIndex());
        assertEquals("LocalizedNumberFormatter should round-trip",
                lnf,
                ((LocalizedNumberFormatterAsFormat) format).getNumberFormatter());
        assertEquals("Should produce same character iterator",
                lnf.format(514.23).toCharacterIterator().getAttributes(),
                format.formatToCharacterIterator(514.23).getAttributes());
    }

    @Test
    public void plurals() {
        // TODO: Expand this test.

        assertFormatSingle(
                "Plural 1",
                "currency/USD precision-integer unit-width-full-name",
                "currency/USD . unit-width-full-name",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.FULL_NAME).precision(Precision.fixedFraction(0)),
                ULocale.ENGLISH,
                1,
                "1 US dollar");

        assertFormatSingle(
                "Plural 1.00",
                "currency/USD .00 unit-width-full-name",
                "currency/USD .00 unit-width-full-name",
                NumberFormatter.with().unit(USD).unitWidth(UnitWidth.FULL_NAME).precision(Precision.fixedFraction(2)),
                ULocale.ENGLISH,
                1,
                "1.00 US dollars");
    }

    @Test
    public void validRanges() throws NoSuchMethodException, IllegalAccessException {
        Method[] methodsWithOneArgument = new Method[] { Precision.class.getDeclaredMethod("fixedFraction", Integer.TYPE),
                Precision.class.getDeclaredMethod("minFraction", Integer.TYPE),
                Precision.class.getDeclaredMethod("maxFraction", Integer.TYPE),
                Precision.class.getDeclaredMethod("fixedSignificantDigits", Integer.TYPE),
                Precision.class.getDeclaredMethod("minSignificantDigits", Integer.TYPE),
                Precision.class.getDeclaredMethod("maxSignificantDigits", Integer.TYPE),
                FractionPrecision.class.getDeclaredMethod("withMinDigits", Integer.TYPE),
                FractionPrecision.class.getDeclaredMethod("withMaxDigits", Integer.TYPE),
                ScientificNotation.class.getDeclaredMethod("withMinExponentDigits", Integer.TYPE),
                IntegerWidth.class.getDeclaredMethod("zeroFillTo", Integer.TYPE),
                IntegerWidth.class.getDeclaredMethod("truncateAt", Integer.TYPE), };
        Method[] methodsWithTwoArguments = new Method[] {
                Precision.class.getDeclaredMethod("minMaxFraction", Integer.TYPE, Integer.TYPE),
                Precision.class.getDeclaredMethod("minMaxSignificantDigits", Integer.TYPE, Integer.TYPE), };

        final int EXPECTED_MAX_INT_FRAC_SIG = 999;
        final String expectedSubstring0 = "between 0 and 999 (inclusive)";
        final String expectedSubstring1 = "between 1 and 999 (inclusive)";
        final String expectedSubstringN1 = "between -1 and 999 (inclusive)";

        // We require that the upper bounds all be 999 inclusive.
        // The lower bound may be either -1, 0, or 1.
        Set<String> methodsWithLowerBound1 = new HashSet();
        methodsWithLowerBound1.add("fixedSignificantDigits");
        methodsWithLowerBound1.add("minSignificantDigits");
        methodsWithLowerBound1.add("maxSignificantDigits");
        methodsWithLowerBound1.add("minMaxSignificantDigits");
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
        Map<String, Object> targets = new HashMap<>();
        targets.put("withMinDigits", Precision.integer());
        targets.put("withMaxDigits", Precision.integer());
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
                    assertNotEquals(message + ": " + actualMessage + "; " + expectedSubstring
                            , -1, actualMessage.indexOf(expectedSubstring));
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

    static void assertFormatDescending(
            String message,
            String skeleton,
            String conciseSkeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            String... expected) {
        final double[] inputs = new double[] { 87650, 8765, 876.5, 87.65, 8.765, 0.8765, 0.08765, 0.008765, 0 };
        assertFormatDescending(message, skeleton, conciseSkeleton, f, locale, inputs, expected);
    }

    static void assertFormatDescendingBig(
            String message,
            String skeleton,
            String conciseSkeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            String... expected) {
        final double[] inputs = new double[] { 87650000, 8765000, 876500, 87650, 8765, 876.5, 87.65, 8.765, 0 };
        assertFormatDescending(message, skeleton, conciseSkeleton, f, locale, inputs, expected);
    }

    static void assertFormatDescending(
            String message,
            String skeleton,
            String conciseSkeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            double[] inputs,
            String... expected) {
        assert expected.length == 9;
        LocalizedNumberFormatter l1 = f.threshold(0L).locale(locale); // no self-regulation
        LocalizedNumberFormatter l2 = f.threshold(1L).locale(locale); // all self-regulation
        for (int i = 0; i < 9; i++) {
            double d = inputs[i];
            String actual1 = l1.format(d).toString();
            assertEquals(message + ": Unsafe Path: " + d, expected[i], actual1);
            String actual2 = l2.format(d).toString();
            assertEquals(message + ": Safe Path: " + d, expected[i], actual2);
        }
        if (skeleton != null) { // if null, skeleton is declared as undefined.
            // Only compare normalized skeletons: the tests need not provide the normalized forms.
            // Use the normalized form to construct the testing formatter to guarantee no loss of info.
            String normalized = NumberFormatter.forSkeleton(skeleton).toSkeleton();
            assertEquals(message + ": Skeleton:", normalized, f.toSkeleton());
            LocalizedNumberFormatter l3 = NumberFormatter.forSkeleton(normalized).locale(locale);
            for (int i = 0; i < 9; i++) {
                double d = inputs[i];
                String actual3 = l3.format(d).toString();
                assertEquals(message + ": Skeleton Path: " + d, expected[i], actual3);
            }
            // Concise skeletons should have same output, and usually round-trip to the normalized skeleton.
            // If the concise skeleton starts with '~', disable the round-trip check.
            boolean shouldRoundTrip = true;
            if (conciseSkeleton.length() > 0 && conciseSkeleton.charAt(0) == '~') {
                conciseSkeleton = conciseSkeleton.substring(1);
                shouldRoundTrip = false;
            }
            LocalizedNumberFormatter l4 = NumberFormatter.forSkeleton(conciseSkeleton).locale(locale);
            if (shouldRoundTrip) {
                assertEquals(message + ": Concise Skeleton:", normalized, l4.toSkeleton());
            }
            for (int i = 0; i < 9; i++) {
                double d = inputs[i];
                String actual4 = l4.format(d).toString();
                assertEquals(message + ": Concise Skeleton Path: '" + normalized + "': " + d, expected[i], actual4);
            }
        } else {
            assertUndefinedSkeleton(f);
        }
    }

    static FormattedNumber assertFormatSingle(
            String message,
            String skeleton,
            String conciseSkeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            Number input,
            String expected) {
        LocalizedNumberFormatter l1 = f.threshold(0L).locale(locale); // no self-regulation
        LocalizedNumberFormatter l2 = f.threshold(1L).locale(locale); // all self-regulation
        FormattedNumber result1 = l1.format(input);
        String actual1 = result1.toString();
        assertEquals(message + ": Unsafe Path: " + input, expected, actual1);
        String actual2 = l2.format(input).toString();
        assertEquals(message + ": Safe Path: " + input, expected, actual2);
        if (skeleton != null) { // if null, skeleton is declared as undefined.
            // Only compare normalized skeletons: the tests need not provide the normalized forms.
            // Use the normalized form to construct the testing formatter to ensure no loss of info.
            String normalized = NumberFormatter.forSkeleton(skeleton).toSkeleton();
            assertEquals(message + ": Skeleton:", normalized, f.toSkeleton());
            LocalizedNumberFormatter l3 = NumberFormatter.forSkeleton(normalized).locale(locale);
            String actual3 = l3.format(input).toString();
            assertEquals(message + ": Skeleton Path: " + input, expected, actual3);
            // Concise skeletons should have same output, and usually round-trip to the normalized skeleton.
            // If the concise skeleton starts with '~', disable the round-trip check.
            boolean shouldRoundTrip = true;
            if (conciseSkeleton.length() > 0 && conciseSkeleton.charAt(0) == '~') {
                conciseSkeleton = conciseSkeleton.substring(1);
                shouldRoundTrip = false;
            }
            LocalizedNumberFormatter l4 = NumberFormatter.forSkeleton(conciseSkeleton).locale(locale);
            if (shouldRoundTrip) {
                assertEquals(message + ": Concise Skeleton:", normalized, l4.toSkeleton());
            }
            String actual4 = l4.format(input).toString();
            assertEquals(message + ": Concise Skeleton Path: '" + normalized + "': " + input, expected, actual4);
        } else {
            assertUndefinedSkeleton(f);
        }
        return result1;
    }

    static void assertFormatSingleMeasure(
            String message,
            String skeleton,
            String conciseSkeleton,
            UnlocalizedNumberFormatter f,
            ULocale locale,
            Measure input,
            String expected) {
        LocalizedNumberFormatter l1 = f.threshold(0L).locale(locale); // no self-regulation
        LocalizedNumberFormatter l2 = f.threshold(1L).locale(locale); // all self-regulation
        String actual1 = l1.format(input).toString();
        assertEquals(message + ": Unsafe Path: " + input, expected, actual1);
        String actual2 = l2.format(input).toString();
        assertEquals(message + ": Safe Path: " + input, expected, actual2);
        if (skeleton != null) { // if null, skeleton is declared as undefined.
            // Only compare normalized skeletons: the tests need not provide the normalized forms.
            // Use the normalized form to construct the testing formatter to ensure no loss of info.
            String normalized = NumberFormatter.forSkeleton(skeleton).toSkeleton();
            assertEquals(message + ": Skeleton:", normalized, f.toSkeleton());
            LocalizedNumberFormatter l3 = NumberFormatter.forSkeleton(normalized).locale(locale);
            String actual3 = l3.format(input).toString();
            assertEquals(message + ": Skeleton Path: " + input, expected, actual3);
            // Concise skeletons should have same output, and usually round-trip to the normalized skeleton.
            // If the concise skeleton starts with '~', disable the round-trip check.
            boolean shouldRoundTrip = true;
            if (conciseSkeleton.length() > 0 && conciseSkeleton.charAt(0) == '~') {
                conciseSkeleton = conciseSkeleton.substring(1);
                shouldRoundTrip = false;
            }

            LocalizedNumberFormatter l4 = NumberFormatter.forSkeleton(conciseSkeleton).locale(locale);
            if (shouldRoundTrip) {
                assertEquals(message + ": Concise Skeleton:", normalized, l4.toSkeleton());
            }
            String actual4 = l4.format(input).toString();
            assertEquals(message + ": Concise Skeleton Path: '" + normalized + "': " + input, expected, actual4);
        } else {
            assertUndefinedSkeleton(f);
        }
    }

    static void assertUndefinedSkeleton(UnlocalizedNumberFormatter f) {
        try {
            String skeleton = f.toSkeleton();
            fail("Expected toSkeleton to fail, but it passed, producing: " + skeleton);
        } catch (UnsupportedOperationException expected) {}
    }

    private void assertNumberFieldPositions(String message, FormattedNumber result, Object[][] expectedFieldPositions) {
        FormattedValueTest.checkFormattedValue(message, result, result.toString(), expectedFieldPositions);
    }
}
