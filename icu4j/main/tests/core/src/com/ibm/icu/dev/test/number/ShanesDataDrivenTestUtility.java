// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.ParsePosition;

import com.ibm.icu.dev.test.format.DataDrivenNumberFormatTestData;
import com.ibm.icu.dev.test.format.DataDrivenNumberFormatTestUtility;
import com.ibm.icu.dev.test.format.DataDrivenNumberFormatTestUtility.CodeUnderTest;
import com.ibm.icu.impl.number.Endpoint;
import com.ibm.icu.impl.number.Format;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.FormatQuantity1;
import com.ibm.icu.impl.number.FormatQuantity2;
import com.ibm.icu.impl.number.FormatQuantity3;
import com.ibm.icu.impl.number.FormatQuantity4;
import com.ibm.icu.impl.number.Parse;
import com.ibm.icu.impl.number.Parse.ParseMode;
import com.ibm.icu.impl.number.PatternString;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.impl.number.formatters.PaddingFormat.PadPosition;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormat.PropertySetter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;

public class ShanesDataDrivenTestUtility extends CodeUnderTest {
  static final String dataPath =
      "../../../icu4j-core-tests/src/com/ibm/icu/dev/data/numberformattestspecification.txt";

  public static void run() {
    CodeUnderTest tester = new ShanesDataDrivenTestUtility();
    DataDrivenNumberFormatTestUtility.runFormatSuiteIncludingKnownFailures(dataPath, tester);
  }

  @Override
  public Character Id() {
    return 'S';
  }

  /**
   * Runs a single formatting test. On success, returns null. On failure, returns the error. This
   * implementation just returns null. Subclasses should override.
   *
   * @param tuple contains the parameters of the format test.
   */
  @Override
  public String format(DataDrivenNumberFormatTestData tuple) {
    String pattern = (tuple.pattern == null) ? "0" : tuple.pattern;
    ULocale locale = (tuple.locale == null) ? ULocale.ENGLISH : tuple.locale;
    Properties properties = PatternString.parseToProperties(pattern, tuple.currency != null);
    propertiesFromTuple(tuple, properties);
    Format fmt = Endpoint.fromBTA(properties, locale);
    FormatQuantity q1, q2, q3;
    if (tuple.format.equals("NaN")) {
      q1 = q2 = new FormatQuantity1(Double.NaN);
      q3 = new FormatQuantity2(Double.NaN);
    } else if (tuple.format.equals("-Inf")) {
      q1 = q2 = new FormatQuantity1(Double.NEGATIVE_INFINITY);
      q3 = new FormatQuantity1(Double.NEGATIVE_INFINITY);
    } else if (tuple.format.equals("Inf")) {
      q1 = q2 = new FormatQuantity1(Double.POSITIVE_INFINITY);
      q3 = new FormatQuantity1(Double.POSITIVE_INFINITY);
    } else {
      BigDecimal d = new BigDecimal(tuple.format);
      if (d.precision() <= 16) {
        q1 = new FormatQuantity1(d);
        q2 = new FormatQuantity1(Double.parseDouble(tuple.format));
        q3 = new FormatQuantity4(d);
      } else {
        q1 = new FormatQuantity1(d);
        q2 = new FormatQuantity3(d);
        q3 = new FormatQuantity4(d); // duplicate values so no null
      }
    }
    String expected = tuple.output;
    String actual1 = fmt.format(q1);
    if (!expected.equals(actual1)) {
      return "Expected \"" + expected + "\", got \"" + actual1 + "\" on FormatQuantity1 BigDecimal";
    }
    String actual2 = fmt.format(q2);
    if (!expected.equals(actual2)) {
      return "Expected \"" + expected + "\", got \"" + actual2 + "\" on FormatQuantity1 double";
    }
    String actual3 = fmt.format(q3);
    if (!expected.equals(actual3)) {
      return "Expected \"" + expected + "\", got \"" + actual3 + "\" on FormatQuantity4 BigDecimal";
    }
    return null;
  }

  /**
   * Runs a single toPattern test. On success, returns null. On failure, returns the error. This
   * implementation just returns null. Subclasses should override.
   *
   * @param tuple contains the parameters of the format test.
   */
  @Override
  public String toPattern(DataDrivenNumberFormatTestData tuple) {
    String pattern = (tuple.pattern == null) ? "0" : tuple.pattern;
    final Properties properties;
    DecimalFormat df;
    try {
      properties = PatternString.parseToProperties(pattern, tuple.currency != null);
      propertiesFromTuple(tuple, properties);
      // TODO: Use PatternString.propertiesToString() directly. (How to deal with CurrencyUsage?)
      df = new DecimalFormat();
      df.setProperties(
          new PropertySetter() {
            @Override
            public void set(Properties props) {
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
      String actual = PatternString.propertiesToString(properties);
      if (!expected.equals(actual)) {
        return "Expected toLocalizedPattern='" + expected + "'; got '" + actual + "'";
      }
    }
    return null;
  }

  /**
   * Runs a single parse test. On success, returns null. On failure, returns the error. This
   * implementation just returns null. Subclasses should override.
   *
   * @param tuple contains the parameters of the format test.
   */
  @Override
  public String parse(DataDrivenNumberFormatTestData tuple) {
    String pattern = (tuple.pattern == null) ? "0" : tuple.pattern;
    Properties properties;
    ParsePosition ppos = new ParsePosition(0);
    Number actual;
    try {
      properties = PatternString.parseToProperties(pattern, tuple.currency != null);
      propertiesFromTuple(tuple, properties);
      actual =
          Parse.parse(
              tuple.parse, ppos, properties, DecimalFormatSymbols.getInstance(tuple.locale));
    } catch (IllegalArgumentException e) {
      return "parse exception: " + e.getMessage();
    }
    if (actual == null && ppos.getIndex() != 0) {
      throw new AssertionError("Error: value is null but parse position is not zero");
    }
    if (ppos.getIndex() == 0) {
      return "Parse failed; got " + actual + ", but expected " + tuple.output;
    }
    if (tuple.output.equals("NaN")) {
      if (!Double.isNaN(actual.doubleValue())) {
        return "Expected NaN, but got: " + actual;
      }
      return null;
    } else if (tuple.output.equals("Inf")) {
      if (!Double.isInfinite(actual.doubleValue())
          || Double.compare(actual.doubleValue(), 0.0) < 0) {
        return "Expected Inf, but got: " + actual;
      }
      return null;
    } else if (tuple.output.equals("-Inf")) {
      if (!Double.isInfinite(actual.doubleValue())
          || Double.compare(actual.doubleValue(), 0.0) > 0) {
        return "Expected -Inf, but got: " + actual;
      }
      return null;
    } else if (tuple.output.equals("fail")) {
      return null;
    } else if (new BigDecimal(tuple.output).compareTo(new BigDecimal(actual.toString())) != 0) {
      return "Expected: " + tuple.output + ", got: " + actual;
    } else {
      return null;
    }
  }

  /**
   * Runs a single parse currency test. On success, returns null. On failure, returns the error.
   * This implementation just returns null. Subclasses should override.
   *
   * @param tuple contains the parameters of the format test.
   */
  @Override
  public String parseCurrency(DataDrivenNumberFormatTestData tuple) {
    String pattern = (tuple.pattern == null) ? "0" : tuple.pattern;
    Properties properties;
    ParsePosition ppos = new ParsePosition(0);
    CurrencyAmount actual;
    try {
      properties = PatternString.parseToProperties(pattern, tuple.currency != null);
      propertiesFromTuple(tuple, properties);
      actual =
          Parse.parseCurrency(
              tuple.parse, ppos, properties, DecimalFormatSymbols.getInstance(tuple.locale));
    } catch (ParseException e) {
      e.printStackTrace();
      return "parse exception: " + e.getMessage();
    }
    if (ppos.getIndex() == 0 || actual.getCurrency().getCurrencyCode().equals("XXX")) {
      return "Parse failed; got " + actual + ", but expected " + tuple.output;
    }
    BigDecimal expectedNumber = new BigDecimal(tuple.output);
    if (expectedNumber.compareTo(new BigDecimal(actual.getNumber().toString())) != 0) {
      return "Wrong number: Expected: " + expectedNumber + ", got: " + actual;
    }
    String expectedCurrency = tuple.outputCurrency;
    if (!expectedCurrency.equals(actual.getCurrency().toString())) {
      return "Wrong currency: Expected: " + expectedCurrency + ", got: " + actual;
    }
    return null;
  }

  /**
   * Runs a single select test. On success, returns null. On failure, returns the error. This
   * implementation just returns null. Subclasses should override.
   *
   * @param tuple contains the parameters of the format test.
   */
  @Override
  public String select(DataDrivenNumberFormatTestData tuple) {
    return null;
  }

  private static void propertiesFromTuple(
      DataDrivenNumberFormatTestData tuple, Properties properties) {
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
    if (tuple.useGrouping != null && tuple.useGrouping == 0) {
      properties.setGroupingSize(Integer.MAX_VALUE);
      properties.setSecondaryGroupingSize(Integer.MAX_VALUE);
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
      properties.setMinimumExponentDigits(
          tuple.useScientific != 0 ? 1 : Properties.DEFAULT_MINIMUM_EXPONENT_DIGITS);
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
    if (tuple.localizedPattern != null) {
      // TODO
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
}
