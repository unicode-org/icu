// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParsePosition;

import org.junit.Test;

import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.DecimalFormatProperties.ParseMode;
import com.ibm.icu.impl.number.Padder.PadPosition;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringUtils;
import com.ibm.icu.impl.number.parse.NumberParserImpl;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormat.PropertySetter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;

public class NumberFormatDataDrivenTest {

    private static ULocale EN = new ULocale("en");

    private static Number toNumber(String s) {
        if (s.equals("NaN")) {
            return Double.NaN;
        } else if (s.equals("-Inf")) {
            return Double.NEGATIVE_INFINITY;
        } else if (s.equals("Inf")) {
            return Double.POSITIVE_INFINITY;
        }
        return new BigDecimal(s);
    }

    /**
     * Standard function for comparing expected and actual parse results. Handles NaN, Infinity, and
     * failure cases.
     */
    private static String compareParseResult(String expected, Number actual, ParsePosition ppos) {
        if (actual == null && ppos.getIndex() != 0) {
            throw new AssertionError("Error: value is null but parse position is not zero");
        }
        if (ppos.getIndex() == 0) {
            return "Parse failed; got " + actual + ", but expected " + expected;
        }
        if (expected.equals("NaN")) {
            if (!Double.isNaN(actual.doubleValue())) {
                return "Expected NaN, but got: " + actual;
            }
            return null;
        } else if (expected.equals("Inf")) {
            if (!Double.isInfinite(actual.doubleValue())
                    || Double.compare(actual.doubleValue(), 0.0) < 0) {
                return "Expected Inf, but got: " + actual;
            }
            return null;
        } else if (expected.equals("-Inf")) {
            if (!Double.isInfinite(actual.doubleValue())
                    || Double.compare(actual.doubleValue(), 0.0) > 0) {
                return "Expected -Inf, but got: " + actual;
            }
            return null;
        } else if (expected.equals("fail")) {
            return null;
        } else if (actual.toString().equals("Infinity")) {
            return "Expected " + expected + ", but got Infinity";
        } else {
            BigDecimal expectedDecimal = new BigDecimal(expected);
            BigDecimal actualDecimal;
            try {
                actualDecimal = new BigDecimal(actual.toString());
            } catch (NumberFormatException e) {
                throw new AssertionError("Could not convert to BigDecimal: " + actual.toString() + " - " + e.getMessage());
            }
            if (expectedDecimal.compareTo(actualDecimal) != 0) {
                return "Expected: " + expected + ", got: " + actual;
            } else {
                return null;
            }
        }
    }

    /**
     * Standard function for comparing expected and actual parse-currency results. Handles failure cases.
     * Does not currently handle NaN or Infinity because there are no parse-currency cases with NaN or
     * Infinity.
     */
    private static String compareParseCurrencyResult(
            String expected,
            String expectedCurrency,
            CurrencyAmount actual,
            ParsePosition ppos) {
        if (ppos.getIndex() == 0 || actual.getCurrency().getCurrencyCode().equals("XXX")) {
            return "Parse failed; got " + actual + ", but expected " + expected;
        }
        if (expected.equals("fail")) {
            return null;
        }
        BigDecimal expectedNumber = new BigDecimal(expected);
        if (expectedNumber.compareTo(new BigDecimal(actual.getNumber().toString())) != 0) {
            return "Wrong number: Expected: " + expectedNumber + ", got: " + actual;
        }
        if (!expectedCurrency.equals(actual.getCurrency().toString())) {
            return "Wrong currency: Expected: " + expectedCurrency + ", got: " + actual;
        }
        return null;
    }

    /**
     * Main ICU4J DecimalFormat data-driven test.
     */
    private DataDrivenNumberFormatTestUtility.CodeUnderTest ICU4J = new DataDrivenNumberFormatTestUtility.CodeUnderTest() {
        @Override
        public Character Id() {
            return 'J';
        }

        @Override
        public String format(DataDrivenNumberFormatTestData tuple) {
            DecimalFormat fmt = createDecimalFormat(tuple);
            String actual = fmt.format(toNumber(tuple.format));
            String expected = tuple.output;
            if (!expected.equals(actual)) {
                return "Expected " + expected + ", got " + actual;
            }
            return null;
        }

        @Override
        public String toPattern(DataDrivenNumberFormatTestData tuple) {
            DecimalFormat fmt = createDecimalFormat(tuple);
            StringBuilder result = new StringBuilder();
            if (tuple.toPattern != null) {
                String expected = tuple.toPattern;
                String actual = fmt.toPattern();
                if (!expected.equals(actual)) {
                    result.append("Expected toPattern=" + expected + ", got " + actual);
                }
            }
            if (tuple.toLocalizedPattern != null) {
                String expected = tuple.toLocalizedPattern;
                String actual = fmt.toLocalizedPattern();
                if (!expected.equals(actual)) {
                    result.append("Expected toLocalizedPattern=" + expected + ", got " + actual);
                }
            }
            return result.length() == 0 ? null : result.toString();
        }

        @Override
        public String parse(DataDrivenNumberFormatTestData tuple) {
            DecimalFormat fmt = createDecimalFormat(tuple);
            ParsePosition ppos = new ParsePosition(0);
            Number actual = fmt.parse(tuple.parse, ppos);
            return compareParseResult(tuple.output, actual, ppos);
        }

        @Override
        public String parseCurrency(DataDrivenNumberFormatTestData tuple) {
            DecimalFormat fmt = createDecimalFormat(tuple);
            ParsePosition ppos = new ParsePosition(0);
            CurrencyAmount actual = fmt.parseCurrency(tuple.parse, ppos);
            return compareParseCurrencyResult(tuple.output, tuple.outputCurrency, actual, ppos);
        }

        /**
         * @param tuple
         * @return
         */
        private DecimalFormat createDecimalFormat(DataDrivenNumberFormatTestData tuple) {
            DecimalFormat fmt = new DecimalFormat(tuple.pattern == null ? "0" : tuple.pattern,
                    new DecimalFormatSymbols(tuple.locale == null ? EN : tuple.locale));
            adjustDecimalFormat(tuple, fmt);
            return fmt;
        }

        /**
         * @param tuple
         * @param fmt
         */
        private void adjustDecimalFormat(DataDrivenNumberFormatTestData tuple, DecimalFormat fmt) {
            if (tuple.minIntegerDigits != null) {
                fmt.setMinimumIntegerDigits(tuple.minIntegerDigits);
            }
            if (tuple.maxIntegerDigits != null) {
                fmt.setMaximumIntegerDigits(tuple.maxIntegerDigits);
            }
            if (tuple.minFractionDigits != null) {
                fmt.setMinimumFractionDigits(tuple.minFractionDigits);
            }
            if (tuple.maxFractionDigits != null) {
                fmt.setMaximumFractionDigits(tuple.maxFractionDigits);
            }
            if (tuple.currency != null) {
                fmt.setCurrency(tuple.currency);
            }
            if (tuple.minGroupingDigits != null) {
                fmt.setMinimumGroupingDigits(tuple.minGroupingDigits);
            }
            if (tuple.useSigDigits != null) {
                fmt.setSignificantDigitsUsed(tuple.useSigDigits != 0);
            }
            if (tuple.minSigDigits != null) {
                fmt.setMinimumSignificantDigits(tuple.minSigDigits);
            }
            if (tuple.maxSigDigits != null) {
                fmt.setMaximumSignificantDigits(tuple.maxSigDigits);
            }
            if (tuple.useGrouping != null) {
                fmt.setGroupingUsed(tuple.useGrouping != 0);
            }
            if (tuple.multiplier != null) {
                fmt.setMultiplier(tuple.multiplier);
            }
            if (tuple.roundingIncrement != null) {
                fmt.setRoundingIncrement(tuple.roundingIncrement.doubleValue());
            }
            if (tuple.formatWidth != null) {
                fmt.setFormatWidth(tuple.formatWidth);
            }
            if (tuple.padCharacter != null && tuple.padCharacter.length() > 0) {
                fmt.setPadCharacter(tuple.padCharacter.charAt(0));
            }
            if (tuple.useScientific != null) {
                fmt.setScientificNotation(tuple.useScientific != 0);
            }
            if (tuple.grouping != null) {
                fmt.setGroupingSize(tuple.grouping);
            }
            if (tuple.grouping2 != null) {
                fmt.setSecondaryGroupingSize(tuple.grouping2);
            }
            if (tuple.roundingMode != null) {
                fmt.setRoundingMode(tuple.roundingMode);
            }
            if (tuple.currencyUsage != null) {
                fmt.setCurrencyUsage(tuple.currencyUsage);
            }
            if (tuple.minimumExponentDigits != null) {
                fmt.setMinimumExponentDigits(tuple.minimumExponentDigits.byteValue());
            }
            if (tuple.exponentSignAlwaysShown != null) {
                fmt.setExponentSignAlwaysShown(tuple.exponentSignAlwaysShown != 0);
            }
            if (tuple.decimalSeparatorAlwaysShown != null) {
                fmt.setDecimalSeparatorAlwaysShown(tuple.decimalSeparatorAlwaysShown != 0);
            }
            if (tuple.padPosition != null) {
                fmt.setPadPosition(tuple.padPosition);
            }
            if (tuple.positivePrefix != null) {
                fmt.setPositivePrefix(tuple.positivePrefix);
            }
            if (tuple.positiveSuffix != null) {
                fmt.setPositiveSuffix(tuple.positiveSuffix);
            }
            if (tuple.negativePrefix != null) {
                fmt.setNegativePrefix(tuple.negativePrefix);
            }
            if (tuple.negativeSuffix != null) {
                fmt.setNegativeSuffix(tuple.negativeSuffix);
            }
            if (tuple.signAlwaysShown != null) {
                fmt.setSignAlwaysShown(tuple.signAlwaysShown != 0);
            }
            if (tuple.localizedPattern != null) {
                fmt.applyLocalizedPattern(tuple.localizedPattern);
            }
            int lenient = tuple.lenient == null ? 1 : tuple.lenient.intValue();
            fmt.setParseStrict(lenient == 0);
            if (tuple.parseIntegerOnly != null) {
                fmt.setParseIntegerOnly(tuple.parseIntegerOnly != 0);
            }
            if (tuple.parseCaseSensitive != null) {
                fmt.setParseCaseSensitive(tuple.parseCaseSensitive != 0);
            }
            if (tuple.decimalPatternMatchRequired != null) {
                fmt.setDecimalPatternMatchRequired(tuple.decimalPatternMatchRequired != 0);
            }
            if (tuple.parseNoExponent != null) {
                fmt.setParseNoExponent(tuple.parseNoExponent != 0);
            }
        }
    };

    /**
     * Test of available JDK APIs.
     */
    private DataDrivenNumberFormatTestUtility.CodeUnderTest JDK = new DataDrivenNumberFormatTestUtility.CodeUnderTest() {
        @Override
        public Character Id() {
            return 'K';
        }

        @Override
        public String format(DataDrivenNumberFormatTestData tuple) {
            java.text.DecimalFormat fmt = createDecimalFormat(tuple);
            String actual = fmt.format(toNumber(tuple.format));
            String expected = tuple.output;
            if (!expected.equals(actual)) {
                return "Expected " + expected + ", got " + actual;
            }
            return null;
        }

        @Override
        public String toPattern(DataDrivenNumberFormatTestData tuple) {
            java.text.DecimalFormat fmt = createDecimalFormat(tuple);
            StringBuilder result = new StringBuilder();
            if (tuple.toPattern != null) {
                String expected = tuple.toPattern;
                String actual = fmt.toPattern();
                if (!expected.equals(actual)) {
                    result.append("Expected toPattern=" + expected + ", got " + actual);
                }
            }
            if (tuple.toLocalizedPattern != null) {
                String expected = tuple.toLocalizedPattern;
                String actual = fmt.toLocalizedPattern();
                if (!expected.equals(actual)) {
                    result.append("Expected toLocalizedPattern=" + expected + ", got " + actual);
                }
            }
            return result.length() == 0 ? null : result.toString();
        }

        @Override
        public String parse(DataDrivenNumberFormatTestData tuple) {
            java.text.DecimalFormat fmt = createDecimalFormat(tuple);
            ParsePosition ppos = new ParsePosition(0);
            Number actual = fmt.parse(tuple.parse, ppos);
            return compareParseResult(tuple.output, actual, ppos);
        }

        /**
         * @param tuple
         * @return
         */
        private java.text.DecimalFormat createDecimalFormat(DataDrivenNumberFormatTestData tuple) {
            java.text.DecimalFormat fmt = new java.text.DecimalFormat(
                    tuple.pattern == null ? "0" : tuple.pattern,
                    new java.text.DecimalFormatSymbols(
                            (tuple.locale == null ? EN : tuple.locale).toLocale()));
            adjustDecimalFormat(tuple, fmt);
            return fmt;
        }

        /**
         * @param tuple
         * @param fmt
         */
        private void adjustDecimalFormat(
                DataDrivenNumberFormatTestData tuple,
                java.text.DecimalFormat fmt) {
            if (tuple.minIntegerDigits != null) {
                fmt.setMinimumIntegerDigits(tuple.minIntegerDigits);
            }
            if (tuple.maxIntegerDigits != null) {
                fmt.setMaximumIntegerDigits(tuple.maxIntegerDigits);
            }
            if (tuple.minFractionDigits != null) {
                fmt.setMinimumFractionDigits(tuple.minFractionDigits);
            }
            if (tuple.maxFractionDigits != null) {
                fmt.setMaximumFractionDigits(tuple.maxFractionDigits);
            }
            if (tuple.currency != null) {
                fmt.setCurrency(java.util.Currency.getInstance(tuple.currency.toString()));
            }
            if (tuple.minGroupingDigits != null) {
                // Oops we don't support this.
            }
            if (tuple.useSigDigits != null) {
                // Oops we don't support this
            }
            if (tuple.minSigDigits != null) {
                // Oops we don't support this
            }
            if (tuple.maxSigDigits != null) {
                // Oops we don't support this
            }
            if (tuple.useGrouping != null) {
                fmt.setGroupingUsed(tuple.useGrouping != 0);
            }
            if (tuple.multiplier != null) {
                fmt.setMultiplier(tuple.multiplier);
            }
            if (tuple.roundingIncrement != null) {
                // Not supported
            }
            if (tuple.formatWidth != null) {
                // Not supported
            }
            if (tuple.padCharacter != null && tuple.padCharacter.length() > 0) {
                // Not supported
            }
            if (tuple.useScientific != null) {
                // Not supported
            }
            if (tuple.grouping != null) {
                fmt.setGroupingSize(tuple.grouping);
            }
            if (tuple.grouping2 != null) {
                // Not supported
            }
            if (tuple.roundingMode != null) {
                // Not supported
            }
            if (tuple.currencyUsage != null) {
                // Not supported
            }
            if (tuple.minimumExponentDigits != null) {
                // Not supported
            }
            if (tuple.exponentSignAlwaysShown != null) {
                // Not supported
            }
            if (tuple.decimalSeparatorAlwaysShown != null) {
                fmt.setDecimalSeparatorAlwaysShown(tuple.decimalSeparatorAlwaysShown != 0);
            }
            if (tuple.padPosition != null) {
                // Not supported
            }
            if (tuple.positivePrefix != null) {
                fmt.setPositivePrefix(tuple.positivePrefix);
            }
            if (tuple.positiveSuffix != null) {
                fmt.setPositiveSuffix(tuple.positiveSuffix);
            }
            if (tuple.negativePrefix != null) {
                fmt.setNegativePrefix(tuple.negativePrefix);
            }
            if (tuple.negativeSuffix != null) {
                fmt.setNegativeSuffix(tuple.negativeSuffix);
            }
            if (tuple.signAlwaysShown != null) {
                // Not supported.
            }
            if (tuple.localizedPattern != null) {
                fmt.applyLocalizedPattern(tuple.localizedPattern);
            }

            // lenient parsing not supported by JDK
            if (tuple.parseIntegerOnly != null) {
                fmt.setParseIntegerOnly(tuple.parseIntegerOnly != 0);
            }
            if (tuple.parseCaseSensitive != null) {
                // Not supported.
            }
            if (tuple.decimalPatternMatchRequired != null) {
                // Oops, not supported
            }
            if (tuple.parseNoExponent != null) {
                // Oops, not supported for now
            }
        }
    };

    static void propertiesFromTuple(
            DataDrivenNumberFormatTestData tuple,
            DecimalFormatProperties properties) {
        if (tuple.minIntegerDigits != null) {
            properties.setMinimumIntegerDigits(tuple.minIntegerDigits);
        }
        if (tuple.maxIntegerDigits != null) {
            properties.setMaximumIntegerDigits(tuple.maxIntegerDigits);
        }
        if (tuple.minFractionDigits != null) {
            properties.setMinimumFractionDigits(tuple.minFractionDigits);
        }
        if (tuple.maxFractionDigits != null) {
            properties.setMaximumFractionDigits(tuple.maxFractionDigits);
        }
        if (tuple.currency != null) {
            properties.setCurrency(tuple.currency);
        }
        if (tuple.minGroupingDigits != null) {
            properties.setMinimumGroupingDigits(tuple.minGroupingDigits);
        }
        if (tuple.useSigDigits != null) {
            // TODO
        }
        if (tuple.minSigDigits != null) {
            properties.setMinimumSignificantDigits(tuple.minSigDigits);
        }
        if (tuple.maxSigDigits != null) {
            properties.setMaximumSignificantDigits(tuple.maxSigDigits);
        }
        if (tuple.useGrouping != null) {
            properties.setGroupingUsed(tuple.useGrouping > 0);
        }
        if (tuple.multiplier != null) {
            properties.setMultiplier(new BigDecimal(tuple.multiplier));
        }
        if (tuple.roundingIncrement != null) {
            properties.setRoundingIncrement(new BigDecimal(tuple.roundingIncrement.toString()));
        }
        if (tuple.formatWidth != null) {
            properties.setFormatWidth(tuple.formatWidth);
        }
        if (tuple.padCharacter != null && tuple.padCharacter.length() > 0) {
            properties.setPadString(tuple.padCharacter.toString());
        }
        if (tuple.useScientific != null) {
            properties.setMinimumExponentDigits(tuple.useScientific != 0 ? 1 : -1);
        }
        if (tuple.grouping != null) {
            properties.setGroupingSize(tuple.grouping);
        }
        if (tuple.grouping2 != null) {
            properties.setSecondaryGroupingSize(tuple.grouping2);
        }
        if (tuple.roundingMode != null) {
            properties.setRoundingMode(RoundingMode.valueOf(tuple.roundingMode));
        }
        if (tuple.currencyUsage != null) {
            properties.setCurrencyUsage(tuple.currencyUsage);
        }
        if (tuple.minimumExponentDigits != null) {
            properties.setMinimumExponentDigits(tuple.minimumExponentDigits.byteValue());
        }
        if (tuple.exponentSignAlwaysShown != null) {
            properties.setExponentSignAlwaysShown(tuple.exponentSignAlwaysShown != 0);
        }
        if (tuple.decimalSeparatorAlwaysShown != null) {
            properties.setDecimalSeparatorAlwaysShown(tuple.decimalSeparatorAlwaysShown != 0);
        }
        if (tuple.padPosition != null) {
            properties.setPadPosition(PadPosition.fromOld(tuple.padPosition));
        }
        if (tuple.positivePrefix != null) {
            properties.setPositivePrefix(tuple.positivePrefix);
        }
        if (tuple.positiveSuffix != null) {
            properties.setPositiveSuffix(tuple.positiveSuffix);
        }
        if (tuple.negativePrefix != null) {
            properties.setNegativePrefix(tuple.negativePrefix);
        }
        if (tuple.negativeSuffix != null) {
            properties.setNegativeSuffix(tuple.negativeSuffix);
        }
        if (tuple.signAlwaysShown != null) {
            properties.setSignAlwaysShown(tuple.signAlwaysShown != 0);
        }
        if (tuple.localizedPattern != null) {
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(tuple.locale);
            String converted = PatternStringUtils
                    .convertLocalized(tuple.localizedPattern, symbols, false);
            PatternStringParser.parseToExistingProperties(converted, properties);
        }
        if (tuple.lenient != null) {
            properties.setParseMode(tuple.lenient == 0 ? ParseMode.STRICT : ParseMode.LENIENT);
        }
        if (tuple.parseIntegerOnly != null) {
            properties.setParseIntegerOnly(tuple.parseIntegerOnly != 0);
        }
        if (tuple.parseCaseSensitive != null) {
            properties.setParseCaseSensitive(tuple.parseCaseSensitive != 0);
        }
        if (tuple.decimalPatternMatchRequired != null) {
            properties.setDecimalPatternMatchRequired(tuple.decimalPatternMatchRequired != 0);
        }
        if (tuple.parseNoExponent != null) {
            properties.setParseNoExponent(tuple.parseNoExponent != 0);
        }
    }

    /**
     * Same as ICU4J, but bypasses the DecimalFormat wrapper and goes directly to the
     * DecimalFormatProperties.
     */
    private DataDrivenNumberFormatTestUtility.CodeUnderTest ICU4J_Properties = new DataDrivenNumberFormatTestUtility.CodeUnderTest() {

        @Override
        public Character Id() {
            return 'P';
        }

        /**
         * Runs a single formatting test. On success, returns null. On failure, returns the error. This
         * implementation just returns null. Subclasses should override.
         *
         * @param tuple
         *            contains the parameters of the format test.
         */
        @Override
        public String format(DataDrivenNumberFormatTestData tuple) {
            String pattern = (tuple.pattern == null) ? "0" : tuple.pattern;
            ULocale locale = (tuple.locale == null) ? ULocale.ENGLISH : tuple.locale;
            DecimalFormatProperties properties = PatternStringParser.parseToProperties(pattern,
                    tuple.currency != null ? PatternStringParser.IGNORE_ROUNDING_ALWAYS
                            : PatternStringParser.IGNORE_ROUNDING_NEVER);
            propertiesFromTuple(tuple, properties);
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
            LocalizedNumberFormatter fmt = NumberFormatter.fromDecimalFormat(properties, symbols, null)
                    .locale(locale);
            Number number = toNumber(tuple.format);
            String expected = tuple.output;
            String actual = fmt.format(number).toString();
            if (!expected.equals(actual)) {
                return "Expected \"" + expected + "\", got \"" + actual + "\"";
            }
            return null;
        }

        /**
         * Runs a single toPattern test. On success, returns null. On failure, returns the error. This
         * implementation just returns null. Subclasses should override.
         *
         * @param tuple
         *            contains the parameters of the format test.
         */
        @Override
        public String toPattern(DataDrivenNumberFormatTestData tuple) {
            String pattern = (tuple.pattern == null) ? "0" : tuple.pattern;
            final DecimalFormatProperties properties;
            DecimalFormat df;
            try {
                properties = PatternStringParser.parseToProperties(pattern,
                        tuple.currency != null ? PatternStringParser.IGNORE_ROUNDING_ALWAYS
                                : PatternStringParser.IGNORE_ROUNDING_NEVER);
                propertiesFromTuple(tuple, properties);
                // TODO: Use PatternString.propertiesToString() directly. (How to deal with
                // CurrencyUsage?)
                df = new DecimalFormat();
                df.setProperties(new PropertySetter() {
                    @Override
                    public void set(DecimalFormatProperties props) {
                        props.copyFrom(properties);
                    }
                });
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return e.getLocalizedMessage();
            }

            if (tuple.toPattern != null) {
                String expected = tuple.toPattern;
                String actual = df.toPattern();
                if (!expected.equals(actual)) {
                    return "Expected toPattern='" + expected + "'; got '" + actual + "'";
                }
            }
            if (tuple.toLocalizedPattern != null) {
                String expected = tuple.toLocalizedPattern;
                String actual = PatternStringUtils.propertiesToPatternString(properties);
                if (!expected.equals(actual)) {
                    return "Expected toLocalizedPattern='" + expected + "'; got '" + actual + "'";
                }
            }
            return null;
        }

        @Override
        public String parse(DataDrivenNumberFormatTestData tuple) {
            String pattern = (tuple.pattern == null) ? "0" : tuple.pattern;
            DecimalFormatProperties properties;
            ParsePosition ppos = new ParsePosition(0);
            Number actual;
            try {
                properties = PatternStringParser.parseToProperties(pattern,
                        tuple.currency != null ? PatternStringParser.IGNORE_ROUNDING_ALWAYS
                                : PatternStringParser.IGNORE_ROUNDING_NEVER);
                propertiesFromTuple(tuple, properties);
                actual = NumberParserImpl.parseStatic(tuple.parse,
                        ppos,
                        properties,
                        DecimalFormatSymbols.getInstance(tuple.locale));
            } catch (IllegalArgumentException e) {
                return "parse exception: " + e.getMessage();
            }
            return compareParseResult(tuple.output, actual, ppos);
        }

        @Override
        public String parseCurrency(DataDrivenNumberFormatTestData tuple) {
            String pattern = (tuple.pattern == null) ? "0" : tuple.pattern;
            DecimalFormatProperties properties;
            ParsePosition ppos = new ParsePosition(0);
            CurrencyAmount actual;
            try {
                properties = PatternStringParser.parseToProperties(pattern,
                        tuple.currency != null ? PatternStringParser.IGNORE_ROUNDING_ALWAYS
                                : PatternStringParser.IGNORE_ROUNDING_NEVER);
                propertiesFromTuple(tuple, properties);
                actual = NumberParserImpl.parseStaticCurrency(tuple.parse,
                        ppos,
                        properties,
                        DecimalFormatSymbols.getInstance(tuple.locale));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return "parse exception: " + e.getMessage();
            }
            return compareParseCurrencyResult(tuple.output, tuple.outputCurrency, actual, ppos);
        }
    };

    @Test
    public void TestNoUnknownIDs() {
        DataDrivenNumberFormatTestUtility.checkNoUnknownIDs("numberformattestspecification.txt", "CHJKP");
    }

    @Test
    public void TestDataDrivenICU4J() {
        DataDrivenNumberFormatTestUtility
                .runFormatSuiteIncludingKnownFailures("numberformattestspecification.txt", ICU4J);
    }

    @Test
    public void TestDataDrivenJDK() {
        // #13373: Since not all JDK implementations are the same, test only whitelisted JDKs
        // with known behavior. The JDK version should be occasionally updated.
        org.junit.Assume.assumeTrue(TestUtil.getJavaRuntimeName() == TestUtil.JavaRuntimeName.OpenJDK
                && TestUtil.getJavaVersion() == 8);

        DataDrivenNumberFormatTestUtility
                .runFormatSuiteIncludingKnownFailures("numberformattestspecification.txt", JDK);
    }

    @Test
    public void TestDataDrivenICU4JProperties() {
        DataDrivenNumberFormatTestUtility
                .runFormatSuiteIncludingKnownFailures("numberformattestspecification.txt", ICU4J_Properties);
    }
}
