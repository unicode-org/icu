// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;

import com.ibm.icu.impl.number.Parse.GroupingMode;
import com.ibm.icu.impl.number.Parse.ParseMode;
import com.ibm.icu.impl.number.formatters.BigDecimalMultiplier;
import com.ibm.icu.impl.number.formatters.CompactDecimalFormat;
import com.ibm.icu.impl.number.formatters.CurrencyFormat;
import com.ibm.icu.impl.number.formatters.CurrencyFormat.CurrencyStyle;
import com.ibm.icu.impl.number.formatters.MagnitudeMultiplier;
import com.ibm.icu.impl.number.formatters.MeasureFormat;
import com.ibm.icu.impl.number.formatters.PaddingFormat;
import com.ibm.icu.impl.number.formatters.PaddingFormat.PadPosition;
import com.ibm.icu.impl.number.formatters.PositiveDecimalFormat;
import com.ibm.icu.impl.number.formatters.PositiveNegativeAffixFormat;
import com.ibm.icu.impl.number.formatters.ScientificFormat;
import com.ibm.icu.impl.number.rounders.IncrementRounder;
import com.ibm.icu.impl.number.rounders.MagnitudeRounder;
import com.ibm.icu.impl.number.rounders.SignificantDigitsRounder;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.DecimalFormat.SignificantDigitsMode;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.MeasureUnit;

public class Properties
    implements Cloneable,
        Serializable,
        PositiveDecimalFormat.IProperties,
        PositiveNegativeAffixFormat.IProperties,
        MagnitudeMultiplier.IProperties,
        ScientificFormat.IProperties,
        MeasureFormat.IProperties,
        CompactDecimalFormat.IProperties,
        PaddingFormat.IProperties,
        BigDecimalMultiplier.IProperties,
        CurrencyFormat.IProperties,
        Parse.IProperties,
        IncrementRounder.IProperties,
        MagnitudeRounder.IProperties,
        SignificantDigitsRounder.IProperties {

  private static final Properties DEFAULT = new Properties();

  /** Auto-generated. */
  private static final long serialVersionUID = 4095518955889349243L;

  // The setters in this class should NOT have any side-effects or perform any validation.  It is
  // up to the consumer of the property bag to deal with property validation.

  // The fields are all marked "transient" because custom serialization is being used.

  /*--------------------------------------------------------------------------------------------+/
  /| IMPORTANT!                                                                                 |/
  /| WHEN ADDING A NEW PROPERTY, add it here, in #_clear(), in #_copyFrom(), in #equals(),      |/
  /| and in #_hashCode().                                                                       |/
  /|                                                                                            |/
  /| The unit test PropertiesTest will catch if you forget to add it to #clear(), #copyFrom(),  |/
  /| or #equals(), but it will NOT catch if you forget to add it to #hashCode().                |/
  /+--------------------------------------------------------------------------------------------*/

  private transient CompactStyle compactStyle;
  private transient Currency currency;
  private transient CurrencyPluralInfo currencyPluralInfo;
  private transient CurrencyStyle currencyStyle;
  private transient CurrencyUsage currencyUsage;
  private transient boolean decimalPatternMatchRequired;
  private transient boolean decimalSeparatorAlwaysShown;
  private transient boolean exponentSignAlwaysShown;
  private transient int formatWidth;
  private transient int groupingSize;
  private transient int magnitudeMultiplier;
  private transient MathContext mathContext;
  private transient int maximumFractionDigits;
  private transient int maximumIntegerDigits;
  private transient int maximumSignificantDigits;
  private transient FormatWidth measureFormatWidth;
  private transient MeasureUnit measureUnit;
  private transient int minimumExponentDigits;
  private transient int minimumFractionDigits;
  private transient int minimumGroupingDigits;
  private transient int minimumIntegerDigits;
  private transient int minimumSignificantDigits;
  private transient BigDecimal multiplier;
  private transient String negativePrefix;
  private transient String negativePrefixPattern;
  private transient String negativeSuffix;
  private transient String negativeSuffixPattern;
  private transient PadPosition padPosition;
  private transient String padString;
  private transient boolean parseCaseSensitive;
  private transient GroupingMode parseGroupingMode;
  private transient boolean parseIntegerOnly;
  private transient ParseMode parseMode;
  private transient boolean parseNoExponent;
  private transient boolean parseToBigDecimal;
  private transient String positivePrefix;
  private transient String positivePrefixPattern;
  private transient String positiveSuffix;
  private transient String positiveSuffixPattern;
  private transient BigDecimal roundingIncrement;
  private transient RoundingMode roundingMode;
  private transient int secondaryGroupingSize;
  private transient boolean signAlwaysShown;
  private transient SignificantDigitsMode significantDigitsMode;

  /*--------------------------------------------------------------------------------------------+/
  /| IMPORTANT!                                                                                 |/
  /| WHEN ADDING A NEW PROPERTY, add it here, in #_clear(), in #_copyFrom(), in #equals(),      |/
  /| and in #_hashCode().                                                                       |/
  /|                                                                                            |/
  /| The unit test PropertiesTest will catch if you forget to add it to #clear(), #copyFrom(),  |/
  /| or #equals(), but it will NOT catch if you forget to add it to #hashCode().                |/
  /+--------------------------------------------------------------------------------------------*/

  public Properties() {
    clear();
  }

  private Properties _clear() {
    compactStyle = DEFAULT_COMPACT_STYLE;
    currency = DEFAULT_CURRENCY;
    currencyPluralInfo = DEFAULT_CURRENCY_PLURAL_INFO;
    currencyStyle = DEFAULT_CURRENCY_STYLE;
    currencyUsage = DEFAULT_CURRENCY_USAGE;
    decimalPatternMatchRequired = DEFAULT_DECIMAL_PATTERN_MATCH_REQUIRED;
    decimalSeparatorAlwaysShown = DEFAULT_DECIMAL_SEPARATOR_ALWAYS_SHOWN;
    exponentSignAlwaysShown = DEFAULT_EXPONENT_SIGN_ALWAYS_SHOWN;
    formatWidth = DEFAULT_FORMAT_WIDTH;
    groupingSize = DEFAULT_GROUPING_SIZE;
    magnitudeMultiplier = DEFAULT_MAGNITUDE_MULTIPLIER;
    mathContext = DEFAULT_MATH_CONTEXT;
    maximumFractionDigits = DEFAULT_MAXIMUM_FRACTION_DIGITS;
    maximumIntegerDigits = DEFAULT_MAXIMUM_INTEGER_DIGITS;
    maximumSignificantDigits = DEFAULT_MAXIMUM_SIGNIFICANT_DIGITS;
    measureFormatWidth = DEFAULT_MEASURE_FORMAT_WIDTH;
    measureUnit = DEFAULT_MEASURE_UNIT;
    minimumExponentDigits = DEFAULT_MINIMUM_EXPONENT_DIGITS;
    minimumFractionDigits = DEFAULT_MINIMUM_FRACTION_DIGITS;
    minimumGroupingDigits = DEFAULT_MINIMUM_GROUPING_DIGITS;
    minimumIntegerDigits = DEFAULT_MINIMUM_INTEGER_DIGITS;
    minimumSignificantDigits = DEFAULT_MINIMUM_SIGNIFICANT_DIGITS;
    multiplier = DEFAULT_MULTIPLIER;
    negativePrefix = DEFAULT_NEGATIVE_PREFIX;
    negativePrefixPattern = DEFAULT_NEGATIVE_PREFIX_PATTERN;
    negativeSuffix = DEFAULT_NEGATIVE_SUFFIX;
    negativeSuffixPattern = DEFAULT_NEGATIVE_SUFFIX_PATTERN;
    padPosition = DEFAULT_PAD_POSITION;
    padString = DEFAULT_PAD_STRING;
    parseCaseSensitive = DEFAULT_PARSE_CASE_SENSITIVE;
    parseGroupingMode = DEFAULT_PARSE_GROUPING_MODE;
    parseIntegerOnly = DEFAULT_PARSE_INTEGER_ONLY;
    parseMode = DEFAULT_PARSE_MODE;
    parseNoExponent = DEFAULT_PARSE_NO_EXPONENT;
    parseToBigDecimal = DEFAULT_PARSE_TO_BIG_DECIMAL;
    positivePrefix = DEFAULT_POSITIVE_PREFIX;
    positivePrefixPattern = DEFAULT_POSITIVE_PREFIX_PATTERN;
    positiveSuffix = DEFAULT_POSITIVE_SUFFIX;
    positiveSuffixPattern = DEFAULT_POSITIVE_SUFFIX_PATTERN;
    roundingIncrement = DEFAULT_ROUNDING_INCREMENT;
    roundingMode = DEFAULT_ROUNDING_MODE;
    secondaryGroupingSize = DEFAULT_SECONDARY_GROUPING_SIZE;
    signAlwaysShown = DEFAULT_SIGN_ALWAYS_SHOWN;
    significantDigitsMode = DEFAULT_SIGNIFICANT_DIGITS_MODE;
    return this;
  }

  private Properties _copyFrom(Properties other) {
    compactStyle = other.compactStyle;
    currency = other.currency;
    currencyPluralInfo = other.currencyPluralInfo;
    currencyStyle = other.currencyStyle;
    currencyUsage = other.currencyUsage;
    decimalPatternMatchRequired = other.decimalPatternMatchRequired;
    decimalSeparatorAlwaysShown = other.decimalSeparatorAlwaysShown;
    exponentSignAlwaysShown = other.exponentSignAlwaysShown;
    formatWidth = other.formatWidth;
    groupingSize = other.groupingSize;
    magnitudeMultiplier = other.magnitudeMultiplier;
    mathContext = other.mathContext;
    maximumFractionDigits = other.maximumFractionDigits;
    maximumIntegerDigits = other.maximumIntegerDigits;
    maximumSignificantDigits = other.maximumSignificantDigits;
    measureFormatWidth = other.measureFormatWidth;
    measureUnit = other.measureUnit;
    minimumExponentDigits = other.minimumExponentDigits;
    minimumFractionDigits = other.minimumFractionDigits;
    minimumGroupingDigits = other.minimumGroupingDigits;
    minimumIntegerDigits = other.minimumIntegerDigits;
    minimumSignificantDigits = other.minimumSignificantDigits;
    multiplier = other.multiplier;
    negativePrefix = other.negativePrefix;
    negativePrefixPattern = other.negativePrefixPattern;
    negativeSuffix = other.negativeSuffix;
    negativeSuffixPattern = other.negativeSuffixPattern;
    padPosition = other.padPosition;
    padString = other.padString;
    parseCaseSensitive = other.parseCaseSensitive;
    parseGroupingMode = other.parseGroupingMode;
    parseIntegerOnly = other.parseIntegerOnly;
    parseMode = other.parseMode;
    parseNoExponent = other.parseNoExponent;
    parseToBigDecimal = other.parseToBigDecimal;
    positivePrefix = other.positivePrefix;
    positivePrefixPattern = other.positivePrefixPattern;
    positiveSuffix = other.positiveSuffix;
    positiveSuffixPattern = other.positiveSuffixPattern;
    roundingIncrement = other.roundingIncrement;
    roundingMode = other.roundingMode;
    secondaryGroupingSize = other.secondaryGroupingSize;
    signAlwaysShown = other.signAlwaysShown;
    significantDigitsMode = other.significantDigitsMode;
    return this;
  }

  private boolean _equals(Properties other) {
    boolean eq = true;
    eq = eq && _equalsHelper(compactStyle, other.compactStyle);
    eq = eq && _equalsHelper(currency, other.currency);
    eq = eq && _equalsHelper(currencyPluralInfo, other.currencyPluralInfo);
    eq = eq && _equalsHelper(currencyStyle, other.currencyStyle);
    eq = eq && _equalsHelper(currencyUsage, other.currencyUsage);
    eq = eq && _equalsHelper(decimalPatternMatchRequired, other.decimalPatternMatchRequired);
    eq = eq && _equalsHelper(decimalSeparatorAlwaysShown, other.decimalSeparatorAlwaysShown);
    eq = eq && _equalsHelper(exponentSignAlwaysShown, other.exponentSignAlwaysShown);
    eq = eq && _equalsHelper(formatWidth, other.formatWidth);
    eq = eq && _equalsHelper(groupingSize, other.groupingSize);
    eq = eq && _equalsHelper(magnitudeMultiplier, other.magnitudeMultiplier);
    eq = eq && _equalsHelper(mathContext, other.mathContext);
    eq = eq && _equalsHelper(maximumFractionDigits, other.maximumFractionDigits);
    eq = eq && _equalsHelper(maximumIntegerDigits, other.maximumIntegerDigits);
    eq = eq && _equalsHelper(maximumSignificantDigits, other.maximumSignificantDigits);
    eq = eq && _equalsHelper(measureFormatWidth, other.measureFormatWidth);
    eq = eq && _equalsHelper(measureUnit, other.measureUnit);
    eq = eq && _equalsHelper(minimumExponentDigits, other.minimumExponentDigits);
    eq = eq && _equalsHelper(minimumFractionDigits, other.minimumFractionDigits);
    eq = eq && _equalsHelper(minimumGroupingDigits, other.minimumGroupingDigits);
    eq = eq && _equalsHelper(minimumIntegerDigits, other.minimumIntegerDigits);
    eq = eq && _equalsHelper(minimumSignificantDigits, other.minimumSignificantDigits);
    eq = eq && _equalsHelper(multiplier, other.multiplier);
    eq = eq && _equalsHelper(negativePrefix, other.negativePrefix);
    eq = eq && _equalsHelper(negativePrefixPattern, other.negativePrefixPattern);
    eq = eq && _equalsHelper(negativeSuffix, other.negativeSuffix);
    eq = eq && _equalsHelper(negativeSuffixPattern, other.negativeSuffixPattern);
    eq = eq && _equalsHelper(padPosition, other.padPosition);
    eq = eq && _equalsHelper(padString, other.padString);
    eq = eq && _equalsHelper(parseCaseSensitive, other.parseCaseSensitive);
    eq = eq && _equalsHelper(parseGroupingMode, other.parseGroupingMode);
    eq = eq && _equalsHelper(parseIntegerOnly, other.parseIntegerOnly);
    eq = eq && _equalsHelper(parseMode, other.parseMode);
    eq = eq && _equalsHelper(parseNoExponent, other.parseNoExponent);
    eq = eq && _equalsHelper(parseToBigDecimal, other.parseToBigDecimal);
    eq = eq && _equalsHelper(positivePrefix, other.positivePrefix);
    eq = eq && _equalsHelper(positivePrefixPattern, other.positivePrefixPattern);
    eq = eq && _equalsHelper(positiveSuffix, other.positiveSuffix);
    eq = eq && _equalsHelper(positiveSuffixPattern, other.positiveSuffixPattern);
    eq = eq && _equalsHelper(roundingIncrement, other.roundingIncrement);
    eq = eq && _equalsHelper(roundingMode, other.roundingMode);
    eq = eq && _equalsHelper(secondaryGroupingSize, other.secondaryGroupingSize);
    eq = eq && _equalsHelper(signAlwaysShown, other.signAlwaysShown);
    eq = eq && _equalsHelper(significantDigitsMode, other.significantDigitsMode);
    return eq;
  }

  private boolean _equalsHelper(boolean mine, boolean theirs) {
    return mine == theirs;
  }

  private boolean _equalsHelper(int mine, int theirs) {
    return mine == theirs;
  }

  private boolean _equalsHelper(Object mine, Object theirs) {
    if (mine == theirs) return true;
    if (mine == null) return false;
    return mine.equals(theirs);
  }

  private int _hashCode() {
    int hashCode = 0;
    hashCode ^= _hashCodeHelper(compactStyle);
    hashCode ^= _hashCodeHelper(currency);
    hashCode ^= _hashCodeHelper(currencyPluralInfo);
    hashCode ^= _hashCodeHelper(currencyStyle);
    hashCode ^= _hashCodeHelper(currencyUsage);
    hashCode ^= _hashCodeHelper(decimalPatternMatchRequired);
    hashCode ^= _hashCodeHelper(decimalSeparatorAlwaysShown);
    hashCode ^= _hashCodeHelper(exponentSignAlwaysShown);
    hashCode ^= _hashCodeHelper(formatWidth);
    hashCode ^= _hashCodeHelper(groupingSize);
    hashCode ^= _hashCodeHelper(magnitudeMultiplier);
    hashCode ^= _hashCodeHelper(mathContext);
    hashCode ^= _hashCodeHelper(maximumFractionDigits);
    hashCode ^= _hashCodeHelper(maximumIntegerDigits);
    hashCode ^= _hashCodeHelper(maximumSignificantDigits);
    hashCode ^= _hashCodeHelper(measureFormatWidth);
    hashCode ^= _hashCodeHelper(measureUnit);
    hashCode ^= _hashCodeHelper(minimumExponentDigits);
    hashCode ^= _hashCodeHelper(minimumFractionDigits);
    hashCode ^= _hashCodeHelper(minimumGroupingDigits);
    hashCode ^= _hashCodeHelper(minimumIntegerDigits);
    hashCode ^= _hashCodeHelper(minimumSignificantDigits);
    hashCode ^= _hashCodeHelper(multiplier);
    hashCode ^= _hashCodeHelper(negativePrefix);
    hashCode ^= _hashCodeHelper(negativePrefixPattern);
    hashCode ^= _hashCodeHelper(negativeSuffix);
    hashCode ^= _hashCodeHelper(negativeSuffixPattern);
    hashCode ^= _hashCodeHelper(padPosition);
    hashCode ^= _hashCodeHelper(padString);
    hashCode ^= _hashCodeHelper(parseCaseSensitive);
    hashCode ^= _hashCodeHelper(parseGroupingMode);
    hashCode ^= _hashCodeHelper(parseIntegerOnly);
    hashCode ^= _hashCodeHelper(parseMode);
    hashCode ^= _hashCodeHelper(parseNoExponent);
    hashCode ^= _hashCodeHelper(parseToBigDecimal);
    hashCode ^= _hashCodeHelper(positivePrefix);
    hashCode ^= _hashCodeHelper(positivePrefixPattern);
    hashCode ^= _hashCodeHelper(positiveSuffix);
    hashCode ^= _hashCodeHelper(positiveSuffixPattern);
    hashCode ^= _hashCodeHelper(roundingIncrement);
    hashCode ^= _hashCodeHelper(roundingMode);
    hashCode ^= _hashCodeHelper(secondaryGroupingSize);
    hashCode ^= _hashCodeHelper(signAlwaysShown);
    hashCode ^= _hashCodeHelper(significantDigitsMode);
    return hashCode;
  }

  private int _hashCodeHelper(boolean value) {
    return value ? 1 : 0;
  }

  private int _hashCodeHelper(int value) {
    return value * 13;
  }

  private int _hashCodeHelper(Object value) {
    if (value == null) return 0;
    return value.hashCode();
  }

  public Properties clear() {
    return _clear();
  }

  /** Creates and returns a shallow copy of the property bag. */
  @Override
  public Properties clone() {
    // super.clone() returns a shallow copy.
    try {
      return (Properties) super.clone();
    } catch (CloneNotSupportedException e) {
      // Should never happen since super is Object
      throw new UnsupportedOperationException(e);
    }
  }

  /**
   * Shallow-copies the properties from the given property bag into this property bag.
   *
   * @param other The property bag from which to copy and which will not be modified.
   * @return The current property bag (the one modified by this operation), for chaining.
   */
  public Properties copyFrom(Properties other) {
    return _copyFrom(other);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) return false;
    if (this == other) return true;
    if (!(other instanceof Properties)) return false;
    return _equals((Properties) other);
  }

  @Override
  public CompactStyle getCompactStyle() {
    return compactStyle;
  }

  @Override
  public Currency getCurrency() {
    return currency;
  }

  /// BEGIN GETTERS/SETTERS ///

  @Override
  @Deprecated
  public CurrencyPluralInfo getCurrencyPluralInfo() {
    return currencyPluralInfo;
  }

  @Override
  public CurrencyStyle getCurrencyStyle() {
    return currencyStyle;
  }

  @Override
  public CurrencyUsage getCurrencyUsage() {
    return currencyUsage;
  }

  @Override
  public boolean getDecimalPatternMatchRequired() {
    return decimalPatternMatchRequired;
  }

  @Override
  public boolean getDecimalSeparatorAlwaysShown() {
    return decimalSeparatorAlwaysShown;
  }

  @Override
  public boolean getExponentSignAlwaysShown() {
    return exponentSignAlwaysShown;
  }

  @Override
  public int getFormatWidth() {
    return formatWidth;
  }

  @Override
  public int getGroupingSize() {
    return groupingSize;
  }

  @Override
  public int getMagnitudeMultiplier() {
    return magnitudeMultiplier;
  }

  @Override
  public MathContext getMathContext() {
    return mathContext;
  }

  @Override
  public int getMaximumFractionDigits() {
    return maximumFractionDigits;
  }

  @Override
  public int getMaximumIntegerDigits() {
    return maximumIntegerDigits;
  }

  @Override
  public int getMaximumSignificantDigits() {
    return maximumSignificantDigits;
  }

  @Override
  public FormatWidth getMeasureFormatWidth() {
    return measureFormatWidth;
  }

  @Override
  public MeasureUnit getMeasureUnit() {
    return measureUnit;
  }

  @Override
  public int getMinimumExponentDigits() {
    return minimumExponentDigits;
  }

  @Override
  public int getMinimumFractionDigits() {
    return minimumFractionDigits;
  }

  @Override
  public int getMinimumGroupingDigits() {
    return minimumGroupingDigits;
  }

  @Override
  public int getMinimumIntegerDigits() {
    return minimumIntegerDigits;
  }

  @Override
  public int getMinimumSignificantDigits() {
    return minimumSignificantDigits;
  }

  @Override
  public BigDecimal getMultiplier() {
    return multiplier;
  }

  @Override
  public String getNegativePrefix() {
    return negativePrefix;
  }

  @Override
  public String getNegativePrefixPattern() {
    return negativePrefixPattern;
  }

  @Override
  public String getNegativeSuffix() {
    return negativeSuffix;
  }

  @Override
  public String getNegativeSuffixPattern() {
    return negativeSuffixPattern;
  }

  @Override
  public PadPosition getPadPosition() {
    return padPosition;
  }

  @Override
  public String getPadString() {
    return padString;
  }

  @Override
  public boolean getParseCaseSensitive() {
    return parseCaseSensitive;
  }

  @Override
  public GroupingMode getParseGroupingMode() {
    return parseGroupingMode;
  }

  @Override
  public boolean getParseIntegerOnly() {
    return parseIntegerOnly;
  }

  @Override
  public ParseMode getParseMode() {
    return parseMode;
  }

  @Override
  public boolean getParseNoExponent() {
    return parseNoExponent;
  }

  @Override
  public boolean getParseToBigDecimal() {
    return parseToBigDecimal;
  }

  @Override
  public String getPositivePrefix() {
    return positivePrefix;
  }

  @Override
  public String getPositivePrefixPattern() {
    return positivePrefixPattern;
  }

  @Override
  public String getPositiveSuffix() {
    return positiveSuffix;
  }

  @Override
  public String getPositiveSuffixPattern() {
    return positiveSuffixPattern;
  }

  @Override
  public BigDecimal getRoundingIncrement() {
    return roundingIncrement;
  }

  @Override
  public RoundingMode getRoundingMode() {
    return roundingMode;
  }

  @Override
  public int getSecondaryGroupingSize() {
    return secondaryGroupingSize;
  }

  @Override
  public boolean getSignAlwaysShown() {
    return signAlwaysShown;
  }

  @Override
  public SignificantDigitsMode getSignificantDigitsMode() {
    return significantDigitsMode;
  }

  @Override
  public int hashCode() {
    return _hashCode();
  }

  /** Custom serialization: re-create object from serialized properties. */
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();

    // Initialize to empty
    clear();

    // Extra int for possible future use
    ois.readInt();

    // 1) How many fields were serialized?
    int count = ois.readInt();

    // 2) Read each field by its name and value
    for (int i = 0; i < count; i++) {
      String name = (String) ois.readObject();
      Object value = ois.readObject();

      // Get the field reference
      Field field = null;
      try {
        field = Properties.class.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        // The field name does not exist! Possibly corrupted serialization. Ignore this entry.
        continue;
      } catch (SecurityException e) {
        // Should not happen
        throw new AssertionError(e);
      }

      // NOTE: If the type of a field were changed in the future, this would be the place to check:
      // If the variable `value` is the old type, perform any conversions necessary.

      // Save value into the field
      try {
        field.set(this, value);
      } catch (IllegalArgumentException e) {
        // Should not happen
        throw new AssertionError(e);
      } catch (IllegalAccessException e) {
        // Should not happen
        throw new AssertionError(e);
      }
    }
  }

  @Override
  public Properties setCompactStyle(CompactStyle compactStyle) {
    this.compactStyle = compactStyle;
    return this;
  }

  @Override
  public Properties setCurrency(Currency currency) {
    this.currency = currency;
    return this;
  }

  @Override
  @Deprecated
  public Properties setCurrencyPluralInfo(CurrencyPluralInfo currencyPluralInfo) {
    // TODO: In order to maintain immutability, we have to perform a clone here.
    // It would be better to just retire CurrencyPluralInfo entirely.
    if (currencyPluralInfo != null) {
      currencyPluralInfo = (CurrencyPluralInfo) currencyPluralInfo.clone();
    }
    this.currencyPluralInfo = currencyPluralInfo;
    return this;
  }

  @Override
  public Properties setCurrencyStyle(CurrencyStyle currencyStyle) {
    this.currencyStyle = currencyStyle;
    return this;
  }

  @Override
  public Properties setCurrencyUsage(CurrencyUsage currencyUsage) {
    this.currencyUsage = currencyUsage;
    return this;
  }

  @Override
  public Properties setDecimalPatternMatchRequired(boolean decimalPatternMatchRequired) {
    this.decimalPatternMatchRequired = decimalPatternMatchRequired;
    return this;
  }

  @Override
  public Properties setDecimalSeparatorAlwaysShown(boolean alwaysShowDecimal) {
    this.decimalSeparatorAlwaysShown = alwaysShowDecimal;
    return this;
  }

  @Override
  public Properties setExponentSignAlwaysShown(boolean exponentSignAlwaysShown) {
    this.exponentSignAlwaysShown = exponentSignAlwaysShown;
    return this;
  }

  @Override
  public Properties setFormatWidth(int paddingWidth) {
    this.formatWidth = paddingWidth;
    return this;
  }

  @Override
  public Properties setGroupingSize(int groupingSize) {
    this.groupingSize = groupingSize;
    return this;
  }

  @Override
  public Properties setMagnitudeMultiplier(int magnitudeMultiplier) {
    this.magnitudeMultiplier = magnitudeMultiplier;
    return this;
  }

  @Override
  public Properties setMathContext(MathContext mathContext) {
    this.mathContext = mathContext;
    return this;
  }

  @Override
  public Properties setMaximumFractionDigits(int maximumFractionDigits) {
    this.maximumFractionDigits = maximumFractionDigits;
    return this;
  }

  @Override
  public Properties setMaximumIntegerDigits(int maximumIntegerDigits) {
    this.maximumIntegerDigits = maximumIntegerDigits;
    return this;
  }

  @Override
  public Properties setMaximumSignificantDigits(int maximumSignificantDigits) {
    this.maximumSignificantDigits = maximumSignificantDigits;
    return this;
  }

  @Override
  public Properties setMeasureFormatWidth(FormatWidth measureFormatWidth) {
    this.measureFormatWidth = measureFormatWidth;
    return this;
  }

  @Override
  public Properties setMeasureUnit(MeasureUnit measureUnit) {
    this.measureUnit = measureUnit;
    return this;
  }

  @Override
  public Properties setMinimumExponentDigits(int exponentDigits) {
    this.minimumExponentDigits = exponentDigits;
    return this;
  }

  @Override
  public Properties setMinimumFractionDigits(int minimumFractionDigits) {
    this.minimumFractionDigits = minimumFractionDigits;
    return this;
  }

  @Override
  public Properties setMinimumGroupingDigits(int minimumGroupingDigits) {
    this.minimumGroupingDigits = minimumGroupingDigits;
    return this;
  }

  @Override
  public Properties setMinimumIntegerDigits(int minimumIntegerDigits) {
    this.minimumIntegerDigits = minimumIntegerDigits;
    return this;
  }

  @Override
  public Properties setMinimumSignificantDigits(int minimumSignificantDigits) {
    this.minimumSignificantDigits = minimumSignificantDigits;
    return this;
  }

  @Override
  public Properties setMultiplier(BigDecimal multiplier) {
    this.multiplier = multiplier;
    return this;
  }

  @Override
  public Properties setNegativePrefix(String negativePrefix) {
    this.negativePrefix = negativePrefix;
    return this;
  }

  @Override
  public Properties setNegativePrefixPattern(String negativePrefixPattern) {
    this.negativePrefixPattern = negativePrefixPattern;
    return this;
  }

  @Override
  public Properties setNegativeSuffix(String negativeSuffix) {
    this.negativeSuffix = negativeSuffix;
    return this;
  }

  @Override
  public Properties setNegativeSuffixPattern(String negativeSuffixPattern) {
    this.negativeSuffixPattern = negativeSuffixPattern;
    return this;
  }

  @Override
  public Properties setPadPosition(PadPosition paddingLocation) {
    this.padPosition = paddingLocation;
    return this;
  }

  @Override
  public Properties setPadString(String paddingString) {
    this.padString = paddingString;
    return this;
  }

  @Override
  public Properties setParseCaseSensitive(boolean parseCaseSensitive) {
    this.parseCaseSensitive = parseCaseSensitive;
    return this;
  }

  @Override
  public Properties setParseGroupingMode(GroupingMode parseGroupingMode) {
    this.parseGroupingMode = parseGroupingMode;
    return this;
  }

  @Override
  public Properties setParseIntegerOnly(boolean parseIntegerOnly) {
    this.parseIntegerOnly = parseIntegerOnly;
    return this;
  }

  @Override
  public Properties setParseMode(ParseMode parseMode) {
    this.parseMode = parseMode;
    return this;
  }

  @Override
  public Properties setParseNoExponent(boolean parseNoExponent) {
    this.parseNoExponent = parseNoExponent;
    return this;
  }

  @Override
  public Properties setParseToBigDecimal(boolean parseToBigDecimal) {
    this.parseToBigDecimal = parseToBigDecimal;
    return this;
  }

  @Override
  public Properties setPositivePrefix(String positivePrefix) {
    this.positivePrefix = positivePrefix;
    return this;
  }

  @Override
  public Properties setPositivePrefixPattern(String positivePrefixPattern) {
    this.positivePrefixPattern = positivePrefixPattern;
    return this;
  }

  @Override
  public Properties setPositiveSuffix(String positiveSuffix) {
    this.positiveSuffix = positiveSuffix;
    return this;
  }

  @Override
  public Properties setPositiveSuffixPattern(String positiveSuffixPattern) {
    this.positiveSuffixPattern = positiveSuffixPattern;
    return this;
  }

  @Override
  public Properties setRoundingIncrement(BigDecimal roundingIncrement) {
    this.roundingIncrement = roundingIncrement;
    return this;
  }

  @Override
  public Properties setRoundingMode(RoundingMode roundingMode) {
    this.roundingMode = roundingMode;
    return this;
  }

  @Override
  public Properties setSecondaryGroupingSize(int secondaryGroupingSize) {
    this.secondaryGroupingSize = secondaryGroupingSize;
    return this;
  }

  @Override
  public Properties setSignAlwaysShown(boolean signAlwaysShown) {
    this.signAlwaysShown = signAlwaysShown;
    return this;
  }

  @Override
  public Properties setSignificantDigitsMode(SignificantDigitsMode significantDigitsMode) {
    this.significantDigitsMode = significantDigitsMode;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("<Properties");
    toStringBare(result);
    result.append(">");
    return result.toString();
  }

  /**
   * Appends a string containing properties that differ from the default, but without being
   * surrounded by &lt;Properties&gt;.
   */
  public void toStringBare(StringBuilder result) {
    Field[] fields = Properties.class.getDeclaredFields();
    for (Field field : fields) {
      Object myValue, defaultValue;
      try {
        myValue = field.get(this);
        defaultValue = field.get(DEFAULT);
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
        continue;
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        continue;
      }
      if (myValue == null && defaultValue == null) {
        continue;
      } else if (myValue == null || defaultValue == null) {
        result.append(" " + field.getName() + ":" + myValue);
      } else if (!myValue.equals(defaultValue)) {
        result.append(" " + field.getName() + ":" + myValue);
      }
    }
  }

  /**
   * Custom serialization: save fields along with their name, so that fields can be easily added in
   * the future in any order. Only save fields that differ from their default value.
   */
  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();

    // Extra int for possible future use
    oos.writeInt(0);

    ArrayList<Field> fieldsToSerialize = new ArrayList<Field>();
    ArrayList<Object> valuesToSerialize = new ArrayList<Object>();
    Field[] fields = Properties.class.getDeclaredFields();
    for (Field field : fields) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      try {
        Object myValue = field.get(this);
        if (myValue == null) {
          // All *Object* values default to null; no need to serialize.
          continue;
        }
        Object defaultValue = field.get(DEFAULT);
        if (!myValue.equals(defaultValue)) {
          fieldsToSerialize.add(field);
          valuesToSerialize.add(myValue);
        }
      } catch (IllegalArgumentException e) {
        // Should not happen
        throw new AssertionError(e);
      } catch (IllegalAccessException e) {
        // Should not happen
        throw new AssertionError(e);
      }
    }

    // 1) How many fields are to be serialized?
    int count = fieldsToSerialize.size();
    oos.writeInt(count);

    // 2) Write each field with its name and value
    for (int i = 0; i < count; i++) {
      Field field = fieldsToSerialize.get(i);
      Object value = valuesToSerialize.get(i);
      oos.writeObject(field.getName());
      oos.writeObject(value);
    }
  }
}
