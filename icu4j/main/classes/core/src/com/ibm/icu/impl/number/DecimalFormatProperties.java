// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
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
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Map;

import com.ibm.icu.impl.number.Padder.PadPosition;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;

public class DecimalFormatProperties implements Cloneable, Serializable {

    private static final DecimalFormatProperties DEFAULT = new DecimalFormatProperties();

    /** Auto-generated. */
    private static final long serialVersionUID = 4095518955889349243L;

    /** Controls the set of rules for parsing a string from the old DecimalFormat API. */
    public static enum ParseMode {
        /**
         * Lenient mode should be used if you want to accept malformed user input. It will use heuristics
         * to attempt to parse through typographical errors in the string.
         */
        LENIENT,

        /**
         * Strict mode should be used if you want to require that the input is well-formed. More
         * specifically, it differs from lenient mode in the following ways:
         *
         * <ul>
         * <li>Grouping widths must match the grouping settings. For example, "12,3,45" will fail if the
         * grouping width is 3, as in the pattern "#,##0".
         * <li>The string must contain a complete prefix and suffix. For example, if the pattern is
         * "{#};(#)", then "{123}" or "(123)" would match, but "{123", "123}", and "123" would all fail.
         * (The latter strings would be accepted in lenient mode.)
         * <li>Whitespace may not appear at arbitrary places in the string. In lenient mode, whitespace
         * is allowed to occur arbitrarily before and after prefixes and exponent separators.
         * <li>Leading grouping separators are not allowed, as in ",123".
         * <li>Minus and plus signs can only appear if specified in the pattern. In lenient mode, a plus
         * or minus sign can always precede a number.
         * <li>The set of characters that can be interpreted as a decimal or grouping separator is
         * smaller.
         * <li><strong>If currency parsing is enabled,</strong> currencies must only appear where
         * specified in either the current pattern string or in a valid pattern string for the current
         * locale. For example, if the pattern is "¤0.00", then "$1.23" would match, but "1.23$" would
         * fail to match.
         * </ul>
         */
        STRICT,

        /**
         * Internal parse mode for increased compatibility with java.text.DecimalFormat.
         * Used by Android libcore. To enable this feature, java.text.DecimalFormat holds an instance of
         * ICU4J's DecimalFormat and enable it by calling setParseStrictMode(ParseMode.JAVA_COMPATIBILITY).
         */
        JAVA_COMPATIBILITY,
    }

    // The setters in this class should NOT have any side-effects or perform any validation. It is
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

    private transient Map<String, Map<String, String>> compactCustomData; // ICU4J-only
    private transient CompactStyle compactStyle;
    private transient Currency currency;
    private transient CurrencyPluralInfo currencyPluralInfo;
    private transient CurrencyUsage currencyUsage;
    private transient boolean decimalPatternMatchRequired;
    private transient boolean decimalSeparatorAlwaysShown;
    private transient boolean exponentSignAlwaysShown;
    private transient int formatWidth;
    private transient int groupingSize;
    private transient boolean groupingUsed;
    private transient int magnitudeMultiplier;
    private transient MathContext mathContext; // ICU4J-only
    private transient int maximumFractionDigits;
    private transient int maximumIntegerDigits;
    private transient int maximumSignificantDigits;
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
    private transient boolean parseIntegerOnly;
    private transient ParseMode parseMode;
    private transient boolean parseNoExponent;
    private transient boolean parseToBigDecimal;
    private transient PluralRules pluralRules;
    private transient String positivePrefix;
    private transient String positivePrefixPattern;
    private transient String positiveSuffix;
    private transient String positiveSuffixPattern;
    private transient BigDecimal roundingIncrement;
    private transient RoundingMode roundingMode;
    private transient int secondaryGroupingSize;
    private transient boolean signAlwaysShown;

    /*--------------------------------------------------------------------------------------------+/
    /| IMPORTANT!                                                                                 |/
    /| WHEN ADDING A NEW PROPERTY, add it here, in #_clear(), in #_copyFrom(), in #equals(),      |/
    /| and in #_hashCode().                                                                       |/
    /|                                                                                            |/
    /| The unit test PropertiesTest will catch if you forget to add it to #clear(), #copyFrom(),  |/
    /| or #equals(), but it will NOT catch if you forget to add it to #hashCode().                |/
    /+--------------------------------------------------------------------------------------------*/

    public DecimalFormatProperties() {
        clear();
    }

    /**
     * Sets all properties to their defaults (unset).
     *
     * <p>
     * All integers default to -1 EXCEPT FOR MAGNITUDE MULTIPLIER which has a default of 0 (since
     * negative numbers are important).
     *
     * <p>
     * All booleans default to false.
     *
     * <p>
     * All non-primitive types default to null.
     *
     * @return The property bag, for chaining.
     */
    private DecimalFormatProperties _clear() {
        compactCustomData = null;
        compactStyle = null;
        currency = null;
        currencyPluralInfo = null;
        currencyUsage = null;
        decimalPatternMatchRequired = false;
        decimalSeparatorAlwaysShown = false;
        exponentSignAlwaysShown = false;
        formatWidth = -1;
        groupingSize = -1;
        groupingUsed = true;
        magnitudeMultiplier = 0;
        mathContext = null;
        maximumFractionDigits = -1;
        maximumIntegerDigits = -1;
        maximumSignificantDigits = -1;
        minimumExponentDigits = -1;
        minimumFractionDigits = -1;
        minimumGroupingDigits = -1;
        minimumIntegerDigits = -1;
        minimumSignificantDigits = -1;
        multiplier = null;
        negativePrefix = null;
        negativePrefixPattern = null;
        negativeSuffix = null;
        negativeSuffixPattern = null;
        padPosition = null;
        padString = null;
        parseCaseSensitive = false;
        parseIntegerOnly = false;
        parseMode = null;
        parseNoExponent = false;
        parseToBigDecimal = false;
        pluralRules = null;
        positivePrefix = null;
        positivePrefixPattern = null;
        positiveSuffix = null;
        positiveSuffixPattern = null;
        roundingIncrement = null;
        roundingMode = null;
        secondaryGroupingSize = -1;
        signAlwaysShown = false;
        return this;
    }

    private DecimalFormatProperties _copyFrom(DecimalFormatProperties other) {
        compactCustomData = other.compactCustomData;
        compactStyle = other.compactStyle;
        currency = other.currency;
        currencyPluralInfo = other.currencyPluralInfo;
        currencyUsage = other.currencyUsage;
        decimalPatternMatchRequired = other.decimalPatternMatchRequired;
        decimalSeparatorAlwaysShown = other.decimalSeparatorAlwaysShown;
        exponentSignAlwaysShown = other.exponentSignAlwaysShown;
        formatWidth = other.formatWidth;
        groupingSize = other.groupingSize;
        groupingUsed = other.groupingUsed;
        magnitudeMultiplier = other.magnitudeMultiplier;
        mathContext = other.mathContext;
        maximumFractionDigits = other.maximumFractionDigits;
        maximumIntegerDigits = other.maximumIntegerDigits;
        maximumSignificantDigits = other.maximumSignificantDigits;
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
        parseIntegerOnly = other.parseIntegerOnly;
        parseMode = other.parseMode;
        parseNoExponent = other.parseNoExponent;
        parseToBigDecimal = other.parseToBigDecimal;
        pluralRules = other.pluralRules;
        positivePrefix = other.positivePrefix;
        positivePrefixPattern = other.positivePrefixPattern;
        positiveSuffix = other.positiveSuffix;
        positiveSuffixPattern = other.positiveSuffixPattern;
        roundingIncrement = other.roundingIncrement;
        roundingMode = other.roundingMode;
        secondaryGroupingSize = other.secondaryGroupingSize;
        signAlwaysShown = other.signAlwaysShown;
        return this;
    }

    private boolean _equals(DecimalFormatProperties other) {
        boolean eq = true;
        eq = eq && _equalsHelper(compactCustomData, other.compactCustomData);
        eq = eq && _equalsHelper(compactStyle, other.compactStyle);
        eq = eq && _equalsHelper(currency, other.currency);
        eq = eq && _equalsHelper(currencyPluralInfo, other.currencyPluralInfo);
        eq = eq && _equalsHelper(currencyUsage, other.currencyUsage);
        eq = eq && _equalsHelper(decimalPatternMatchRequired, other.decimalPatternMatchRequired);
        eq = eq && _equalsHelper(decimalSeparatorAlwaysShown, other.decimalSeparatorAlwaysShown);
        eq = eq && _equalsHelper(exponentSignAlwaysShown, other.exponentSignAlwaysShown);
        eq = eq && _equalsHelper(formatWidth, other.formatWidth);
        eq = eq && _equalsHelper(groupingSize, other.groupingSize);
        eq = eq && _equalsHelper(groupingUsed, other.groupingUsed);
        eq = eq && _equalsHelper(magnitudeMultiplier, other.magnitudeMultiplier);
        eq = eq && _equalsHelper(mathContext, other.mathContext);
        eq = eq && _equalsHelper(maximumFractionDigits, other.maximumFractionDigits);
        eq = eq && _equalsHelper(maximumIntegerDigits, other.maximumIntegerDigits);
        eq = eq && _equalsHelper(maximumSignificantDigits, other.maximumSignificantDigits);
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
        eq = eq && _equalsHelper(parseIntegerOnly, other.parseIntegerOnly);
        eq = eq && _equalsHelper(parseMode, other.parseMode);
        eq = eq && _equalsHelper(parseNoExponent, other.parseNoExponent);
        eq = eq && _equalsHelper(parseToBigDecimal, other.parseToBigDecimal);
        eq = eq && _equalsHelper(pluralRules, other.pluralRules);
        eq = eq && _equalsHelper(positivePrefix, other.positivePrefix);
        eq = eq && _equalsHelper(positivePrefixPattern, other.positivePrefixPattern);
        eq = eq && _equalsHelper(positiveSuffix, other.positiveSuffix);
        eq = eq && _equalsHelper(positiveSuffixPattern, other.positiveSuffixPattern);
        eq = eq && _equalsHelper(roundingIncrement, other.roundingIncrement);
        eq = eq && _equalsHelper(roundingMode, other.roundingMode);
        eq = eq && _equalsHelper(secondaryGroupingSize, other.secondaryGroupingSize);
        eq = eq && _equalsHelper(signAlwaysShown, other.signAlwaysShown);
        return eq;
    }

    private boolean _equalsHelper(boolean mine, boolean theirs) {
        return mine == theirs;
    }

    private boolean _equalsHelper(int mine, int theirs) {
        return mine == theirs;
    }

    private boolean _equalsHelper(Object mine, Object theirs) {
        if (mine == theirs)
            return true;
        if (mine == null)
            return false;
        return mine.equals(theirs);
    }

    private int _hashCode() {
        int hashCode = 0;
        hashCode ^= _hashCodeHelper(compactCustomData);
        hashCode ^= _hashCodeHelper(compactStyle);
        hashCode ^= _hashCodeHelper(currency);
        hashCode ^= _hashCodeHelper(currencyPluralInfo);
        hashCode ^= _hashCodeHelper(currencyUsage);
        hashCode ^= _hashCodeHelper(decimalPatternMatchRequired);
        hashCode ^= _hashCodeHelper(decimalSeparatorAlwaysShown);
        hashCode ^= _hashCodeHelper(exponentSignAlwaysShown);
        hashCode ^= _hashCodeHelper(formatWidth);
        hashCode ^= _hashCodeHelper(groupingSize);
        hashCode ^= _hashCodeHelper(groupingUsed);
        hashCode ^= _hashCodeHelper(magnitudeMultiplier);
        hashCode ^= _hashCodeHelper(mathContext);
        hashCode ^= _hashCodeHelper(maximumFractionDigits);
        hashCode ^= _hashCodeHelper(maximumIntegerDigits);
        hashCode ^= _hashCodeHelper(maximumSignificantDigits);
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
        hashCode ^= _hashCodeHelper(parseIntegerOnly);
        hashCode ^= _hashCodeHelper(parseMode);
        hashCode ^= _hashCodeHelper(parseNoExponent);
        hashCode ^= _hashCodeHelper(parseToBigDecimal);
        hashCode ^= _hashCodeHelper(pluralRules);
        hashCode ^= _hashCodeHelper(positivePrefix);
        hashCode ^= _hashCodeHelper(positivePrefixPattern);
        hashCode ^= _hashCodeHelper(positiveSuffix);
        hashCode ^= _hashCodeHelper(positiveSuffixPattern);
        hashCode ^= _hashCodeHelper(roundingIncrement);
        hashCode ^= _hashCodeHelper(roundingMode);
        hashCode ^= _hashCodeHelper(secondaryGroupingSize);
        hashCode ^= _hashCodeHelper(signAlwaysShown);
        return hashCode;
    }

    private int _hashCodeHelper(boolean value) {
        return value ? 1 : 0;
    }

    private int _hashCodeHelper(int value) {
        return value * 13;
    }

    private int _hashCodeHelper(Object value) {
        if (value == null)
            return 0;
        return value.hashCode();
    }

    public DecimalFormatProperties clear() {
        return _clear();
    }

    /** Creates and returns a shallow copy of the property bag. */
    @Override
    public DecimalFormatProperties clone() {
        // super.clone() returns a shallow copy.
        try {
            return (DecimalFormatProperties) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen since super is Object
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Shallow-copies the properties from the given property bag into this property bag.
     *
     * @param other
     *            The property bag from which to copy and which will not be modified.
     * @return The current property bag (the one modified by this operation), for chaining.
     */
    public DecimalFormatProperties copyFrom(DecimalFormatProperties other) {
        return _copyFrom(other);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (this == other)
            return true;
        if (!(other instanceof DecimalFormatProperties))
            return false;
        return _equals((DecimalFormatProperties) other);
    }

    /// BEGIN GETTERS/SETTERS ///

    public Map<String, Map<String, String>> getCompactCustomData() {
        return compactCustomData;
    }

    public CompactStyle getCompactStyle() {
        return compactStyle;
    }

    public Currency getCurrency() {
        return currency;
    }

    public CurrencyPluralInfo getCurrencyPluralInfo() {
        return currencyPluralInfo;
    }

    public CurrencyUsage getCurrencyUsage() {
        return currencyUsage;
    }

    public boolean getDecimalPatternMatchRequired() {
        return decimalPatternMatchRequired;
    }

    public boolean getDecimalSeparatorAlwaysShown() {
        return decimalSeparatorAlwaysShown;
    }

    public boolean getExponentSignAlwaysShown() {
        return exponentSignAlwaysShown;
    }

    public int getFormatWidth() {
        return formatWidth;
    }

    public int getGroupingSize() {
        return groupingSize;
    }

    public boolean getGroupingUsed() {
        return groupingUsed;
    }

    public int getMagnitudeMultiplier() {
        return magnitudeMultiplier;
    }

    public MathContext getMathContext() {
        return mathContext;
    }

    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    public int getMaximumIntegerDigits() {
        return maximumIntegerDigits;
    }

    public int getMaximumSignificantDigits() {
        return maximumSignificantDigits;
    }

    public int getMinimumExponentDigits() {
        return minimumExponentDigits;
    }

    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    public int getMinimumGroupingDigits() {
        return minimumGroupingDigits;
    }

    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    public int getMinimumSignificantDigits() {
        return minimumSignificantDigits;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public String getNegativePrefix() {
        return negativePrefix;
    }

    public String getNegativePrefixPattern() {
        return negativePrefixPattern;
    }

    public String getNegativeSuffix() {
        return negativeSuffix;
    }

    public String getNegativeSuffixPattern() {
        return negativeSuffixPattern;
    }

    public PadPosition getPadPosition() {
        return padPosition;
    }

    public String getPadString() {
        return padString;
    }

    public boolean getParseCaseSensitive() {
        return parseCaseSensitive;
    }

    public boolean getParseIntegerOnly() {
        return parseIntegerOnly;
    }

    public ParseMode getParseMode() {
        return parseMode;
    }

    public boolean getParseNoExponent() {
        return parseNoExponent;
    }

    public boolean getParseToBigDecimal() {
        return parseToBigDecimal;
    }

    public PluralRules getPluralRules() {
        return pluralRules;
    }

    public String getPositivePrefix() {
        return positivePrefix;
    }

    public String getPositivePrefixPattern() {
        return positivePrefixPattern;
    }

    public String getPositiveSuffix() {
        return positiveSuffix;
    }

    public String getPositiveSuffixPattern() {
        return positiveSuffixPattern;
    }

    public BigDecimal getRoundingIncrement() {
        return roundingIncrement;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public int getSecondaryGroupingSize() {
        return secondaryGroupingSize;
    }

    public boolean getSignAlwaysShown() {
        return signAlwaysShown;
    }

    @Override
    public int hashCode() {
        return _hashCode();
    }

    /** Custom serialization: re-create object from serialized properties. */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        readObjectImpl(ois);
    }

    /* package-private */ void readObjectImpl(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
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
                field = DecimalFormatProperties.class.getDeclaredField(name);
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

    /**
     * Specifies custom data to be used instead of CLDR data when constructing a CompactDecimalFormat.
     * The argument should be a map with the following structure:
     *
     * <pre>
     * {
     *   "1000": {
     *     "one": "0 thousand",
     *     "other": "0 thousand"
     *   },
     *   "10000": {
     *     "one": "00 thousand",
     *     "other": "00 thousand"
     *   },
     *   // ...
     * }
     * </pre>
     *
     * This API endpoint is used by the CLDR Survey Tool.
     *
     * @param compactCustomData
     *            A map with the above structure.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setCompactCustomData(
            Map<String, Map<String, String>> compactCustomData) {
        // TODO: compactCustomData is not immutable.
        this.compactCustomData = compactCustomData;
        return this;
    }

    /**
     * Use compact decimal formatting with the specified {@link CompactStyle}. CompactStyle.SHORT
     * produces output like "10K" in locale <em>en-US</em>, whereas CompactStyle.LONG produces output
     * like "10 thousand" in that locale.
     *
     * @param compactStyle
     *            The style of prefixes/suffixes to append.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setCompactStyle(CompactStyle compactStyle) {
        this.compactStyle = compactStyle;
        return this;
    }

    /**
     * Use the specified currency to substitute currency placeholders ('¤') in the pattern string.
     *
     * @param currency
     *            The currency.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setCurrency(Currency currency) {
        this.currency = currency;
        return this;
    }

    /**
     * Use the specified {@link CurrencyPluralInfo} instance when formatting currency long names.
     *
     * @param currencyPluralInfo
     *            The currency plural info object.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setCurrencyPluralInfo(CurrencyPluralInfo currencyPluralInfo) {
        // TODO: In order to maintain immutability, we have to perform a clone here.
        // It would be better to just retire CurrencyPluralInfo entirely.
        if (currencyPluralInfo != null) {
            currencyPluralInfo = (CurrencyPluralInfo) currencyPluralInfo.clone();
        }
        this.currencyPluralInfo = currencyPluralInfo;
        return this;
    }

    /**
     * Use the specified {@link CurrencyUsage} instance, which provides default rounding rules for the
     * currency in two styles, CurrencyUsage.CASH and CurrencyUsage.STANDARD.
     *
     * <p>
     * The CurrencyUsage specified here will not be used unless there is a currency placeholder in the
     * pattern.
     *
     * @param currencyUsage
     *            The currency usage. Defaults to CurrencyUsage.STANDARD.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setCurrencyUsage(CurrencyUsage currencyUsage) {
        this.currencyUsage = currencyUsage;
        return this;
    }

    /**
     * PARSING: Whether to require that the presence of decimal point matches the pattern. If a decimal
     * point is not present, but the pattern contained a decimal point, parse will not succeed: null will
     * be returned from <code>parse()</code>, and an error index will be set in the
     * {@link ParsePosition}.
     *
     * @param decimalPatternMatchRequired
     *            true to set an error if decimal is not present
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setDecimalPatternMatchRequired(boolean decimalPatternMatchRequired) {
        this.decimalPatternMatchRequired = decimalPatternMatchRequired;
        return this;
    }

    /**
     * Sets whether to always show the decimal point, even if the number doesn't require one. For
     * example, if always show decimal is true, the number 123 would be formatted as "123." in locale
     * <em>en-US</em>.
     *
     * @param alwaysShowDecimal
     *            Whether to show the decimal point when it is optional.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setDecimalSeparatorAlwaysShown(boolean alwaysShowDecimal) {
        this.decimalSeparatorAlwaysShown = alwaysShowDecimal;
        return this;
    }

    /**
     * Sets whether to show the plus sign in the exponent part of numbers with a zero or positive
     * exponent. For example, the number "1200" with the pattern "0.0E0" would be formatted as "1.2E+3"
     * instead of "1.2E3" in <em>en-US</em>.
     *
     * @param exponentSignAlwaysShown
     *            Whether to show the plus sign in positive exponents.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setExponentSignAlwaysShown(boolean exponentSignAlwaysShown) {
        this.exponentSignAlwaysShown = exponentSignAlwaysShown;
        return this;
    }

    /**
     * Sets the minimum width of the string output by the formatting pipeline. For example, if padding is
     * enabled and paddingWidth is set to 6, formatting the number "3.14159" with the pattern "0.00" will
     * result in "··3.14" if '·' is your padding string.
     *
     * <p>
     * If the number is longer than your padding width, the number will display as if no padding width
     * had been specified, which may result in strings longer than the padding width.
     *
     * <p>
     * Width is counted in UTF-16 code units.
     *
     * @param paddingWidth
     *            The output width.
     * @return The property bag, for chaining.
     * @see #setPadPosition
     * @see #setPadString
     */
    public DecimalFormatProperties setFormatWidth(int paddingWidth) {
        this.formatWidth = paddingWidth;
        return this;
    }

    /**
     * Sets the number of digits between grouping separators. For example, the <em>en-US</em> locale uses
     * a grouping size of 3, so the number 1234567 would be formatted as "1,234,567". For locales whose
     * grouping sizes vary with magnitude, see {@link #setSecondaryGroupingSize(int)}.
     *
     * @param groupingSize
     *            The primary grouping size.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setGroupingSize(int groupingSize) {
        this.groupingSize = groupingSize;
        return this;
    }

    /**
     * Sets whether to enable grouping when formatting.
     *
     * @param groupingUsed
     *            true to enable the display of grouping separators; false to disable.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setGroupingUsed(boolean groupingUsed) {
        this.groupingUsed = groupingUsed;
        return this;
    }

    /**
     * Multiply all numbers by this power of ten before formatting. Negative multipliers reduce the
     * magnitude and make numbers smaller (closer to zero).
     *
     * @param magnitudeMultiplier
     *            The number of powers of ten to scale.
     * @return The property bag, for chaining.
     * @see #setMultiplier
     */
    public DecimalFormatProperties setMagnitudeMultiplier(int magnitudeMultiplier) {
        this.magnitudeMultiplier = magnitudeMultiplier;
        return this;
    }

    /**
     * Sets the {@link MathContext} to be used during math and rounding operations. A MathContext
     * encapsulates a RoundingMode and the number of significant digits in the output.
     *
     * @param mathContext
     *            The math context to use when rounding is required.
     * @return The property bag, for chaining.
     * @see MathContext
     * @see #setRoundingMode
     */
    public DecimalFormatProperties setMathContext(MathContext mathContext) {
        this.mathContext = mathContext;
        return this;
    }

    /**
     * Sets the maximum number of digits to display after the decimal point. If the number has fewer than
     * this number of digits, the number will be rounded off using the rounding mode specified by
     * {@link #setRoundingMode(RoundingMode)}. The pattern "#00.0#", for example, corresponds to 2
     * maximum fraction digits, and the number 456.789 would be formatted as "456.79" in locale
     * <em>en-US</em> with the default rounding mode. Note that the number 456.999 would be formatted as
     * "457.0" given the same configurations.
     *
     * @param maximumFractionDigits
     *            The maximum number of fraction digits to output.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setMaximumFractionDigits(int maximumFractionDigits) {
        this.maximumFractionDigits = maximumFractionDigits;
        return this;
    }

    /**
     * Sets the maximum number of digits to display before the decimal point. If the number has more than
     * this number of digits, the extra digits will be truncated. For example, if maximum integer digits
     * is 2, and you attempt to format the number 1970, you will get "70" in locale <em>en-US</em>. It is
     * not possible to specify the maximum integer digits using a pattern string, except in the special
     * case of a scientific format pattern.
     *
     * @param maximumIntegerDigits
     *            The maximum number of integer digits to output.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setMaximumIntegerDigits(int maximumIntegerDigits) {
        this.maximumIntegerDigits = maximumIntegerDigits;
        return this;
    }

    /**
     * Sets the maximum number of significant digits to display. The number of significant digits is
     * equal to the number of digits counted from the leftmost nonzero digit through the rightmost
     * nonzero digit; for example, the number "2010" has 3 significant digits. If the number has more
     * significant digits than specified here, the extra significant digits will be rounded off using the
     * rounding mode specified by {@link #setRoundingMode(RoundingMode)}. For example, if maximum
     * significant digits is 3, the number 1234.56 will be formatted as "1230" in locale <em>en-US</em>
     * with the default rounding mode.
     *
     * <p>
     * If both maximum significant digits and maximum integer/fraction digits are set at the same time,
     * the behavior is undefined.
     *
     * <p>
     * The number of significant digits can be specified in a pattern string using the '@' character. For
     * example, the pattern "@@#" corresponds to a minimum of 2 and a maximum of 3 significant digits.
     *
     * @param maximumSignificantDigits
     *            The maximum number of significant digits to display.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setMaximumSignificantDigits(int maximumSignificantDigits) {
        this.maximumSignificantDigits = maximumSignificantDigits;
        return this;
    }

    /**
     * Sets the minimum number of digits to display in the exponent. For example, the number "1200" with
     * the pattern "0.0E00", which has 2 exponent digits, would be formatted as "1.2E03" in
     * <em>en-US</em>.
     *
     * @param minimumExponentDigits
     *            The minimum number of digits to display in the exponent field.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setMinimumExponentDigits(int minimumExponentDigits) {
        this.minimumExponentDigits = minimumExponentDigits;
        return this;
    }

    /**
     * Sets the minimum number of digits to display after the decimal point. If the number has fewer than
     * this number of digits, the number will be padded with zeros. The pattern "#00.0#", for example,
     * corresponds to 1 minimum fraction digit, and the number 456 would be formatted as "456.0" in
     * locale <em>en-US</em>.
     *
     * @param minimumFractionDigits
     *            The minimum number of fraction digits to output.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setMinimumFractionDigits(int minimumFractionDigits) {
        this.minimumFractionDigits = minimumFractionDigits;
        return this;
    }

    /**
     * Sets the minimum number of digits required to be beyond the first grouping separator in order to
     * enable grouping. For example, if the minimum grouping digits is 2, then 1234 would be formatted as
     * "1234" but 12345 would be formatted as "12,345" in <em>en-US</em>. Note that 1234567 would still
     * be formatted as "1,234,567", not "1234,567".
     *
     * @param minimumGroupingDigits
     *            How many digits must appear before a grouping separator before enabling grouping.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setMinimumGroupingDigits(int minimumGroupingDigits) {
        this.minimumGroupingDigits = minimumGroupingDigits;
        return this;
    }

    /**
     * Sets the minimum number of digits to display before the decimal point. If the number has fewer
     * than this number of digits, the number will be padded with zeros. The pattern "#00.0#", for
     * example, corresponds to 2 minimum integer digits, and the number 5.3 would be formatted as "05.3"
     * in locale <em>en-US</em>.
     *
     * @param minimumIntegerDigits
     *            The minimum number of integer digits to output.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setMinimumIntegerDigits(int minimumIntegerDigits) {
        this.minimumIntegerDigits = minimumIntegerDigits;
        return this;
    }

    /**
     * Sets the minimum number of significant digits to display. If, after rounding to the number of
     * significant digits specified by {@link #setMaximumSignificantDigits}, the number of remaining
     * significant digits is less than the minimum, the number will be padded with zeros. For example, if
     * minimum significant digits is 3, the number 5.8 will be formatted as "5.80" in locale
     * <em>en-US</em>. Note that minimum significant digits is relevant only when numbers have digits
     * after the decimal point.
     *
     * <p>
     * If both minimum significant digits and minimum integer/fraction digits are set at the same time,
     * both values will be respected, and the one that results in the greater number of padding zeros
     * will be used. For example, formatting the number 73 with 3 minimum significant digits and 2
     * minimum fraction digits will produce "73.00".
     *
     * <p>
     * The number of significant digits can be specified in a pattern string using the '@' character. For
     * example, the pattern "@@#" corresponds to a minimum of 2 and a maximum of 3 significant digits.
     *
     * @param minimumSignificantDigits
     *            The minimum number of significant digits to display.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setMinimumSignificantDigits(int minimumSignificantDigits) {
        this.minimumSignificantDigits = minimumSignificantDigits;
        return this;
    }

    /**
     * Multiply all numbers by this amount before formatting.
     *
     * @param multiplier
     *            The amount to multiply by.
     * @return The property bag, for chaining.
     * @see #setMagnitudeMultiplier
     */
    public DecimalFormatProperties setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
        return this;
    }

    /**
     * Sets the prefix to prepend to negative numbers. The prefix will be interpreted literally. For
     * example, if you set a negative prefix of <code>n</code>, then the number -123 will be formatted as
     * "n123" in the locale <em>en-US</em>. Note that if the negative prefix is left unset, the locale's
     * minus sign is used.
     *
     * <p>
     * For more information on prefixes and suffixes, see {@link MutablePatternModifier}.
     *
     * @param negativePrefix
     *            The CharSequence to prepend to negative numbers.
     * @return The property bag, for chaining.
     * @see #setNegativePrefixPattern
     */
    public DecimalFormatProperties setNegativePrefix(String negativePrefix) {
        this.negativePrefix = negativePrefix;
        return this;
    }

    /**
     * Sets the prefix to prepend to negative numbers. Locale-specific symbols will be substituted into
     * the string according to Unicode Technical Standard #35 (LDML).
     *
     * <p>
     * For more information on prefixes and suffixes, see {@link MutablePatternModifier}.
     *
     * @param negativePrefixPattern
     *            The CharSequence to prepend to negative numbers after locale symbol substitutions take
     *            place.
     * @return The property bag, for chaining.
     * @see #setNegativePrefix
     */
    public DecimalFormatProperties setNegativePrefixPattern(String negativePrefixPattern) {
        this.negativePrefixPattern = negativePrefixPattern;
        return this;
    }

    /**
     * Sets the suffix to append to negative numbers. The suffix will be interpreted literally. For
     * example, if you set a suffix prefix of <code>n</code>, then the number -123 will be formatted as
     * "-123n" in the locale <em>en-US</em>. Note that the minus sign is prepended by default unless
     * otherwise specified in either the pattern string or in one of the {@link #setNegativePrefix}
     * methods.
     *
     * <p>
     * For more information on prefixes and suffixes, see {@link MutablePatternModifier}.
     *
     * @param negativeSuffix
     *            The CharSequence to append to negative numbers.
     * @return The property bag, for chaining.
     * @see #setNegativeSuffixPattern
     */
    public DecimalFormatProperties setNegativeSuffix(String negativeSuffix) {
        this.negativeSuffix = negativeSuffix;
        return this;
    }

    /**
     * Sets the suffix to append to negative numbers. Locale-specific symbols will be substituted into
     * the string according to Unicode Technical Standard #35 (LDML).
     *
     * <p>
     * For more information on prefixes and suffixes, see {@link MutablePatternModifier}.
     *
     * @param negativeSuffixPattern
     *            The CharSequence to append to negative numbers after locale symbol substitutions take
     *            place.
     * @return The property bag, for chaining.
     * @see #setNegativeSuffix
     */
    public DecimalFormatProperties setNegativeSuffixPattern(String negativeSuffixPattern) {
        this.negativeSuffixPattern = negativeSuffixPattern;
        return this;
    }

    /**
     * Sets the location where the padding string is to be inserted to maintain the padding width: one of
     * BEFORE_PREFIX, AFTER_PREFIX, BEFORE_SUFFIX, or AFTER_SUFFIX.
     *
     * <p>
     * Must be used in conjunction with {@link #setFormatWidth}.
     *
     * @param paddingLocation
     *            The output width.
     * @return The property bag, for chaining.
     * @see #setFormatWidth
     */
    public DecimalFormatProperties setPadPosition(PadPosition paddingLocation) {
        this.padPosition = paddingLocation;
        return this;
    }

    /**
     * Sets the string used for padding. The string should contain a single character or grapheme
     * cluster.
     *
     * <p>
     * Must be used in conjunction with {@link #setFormatWidth}.
     *
     * @param paddingString
     *            The padding string. Defaults to an ASCII space (U+0020).
     * @return The property bag, for chaining.
     * @see #setFormatWidth
     */
    public DecimalFormatProperties setPadString(String paddingString) {
        this.padString = paddingString;
        return this;
    }

    /**
     * Whether to require cases to match when parsing strings; default is true. Case sensitivity applies
     * to prefixes, suffixes, the exponent separator, the symbol "NaN", and the infinity symbol. Grouping
     * separators, decimal separators, and padding are always case-sensitive. Currencies are always
     * case-insensitive.
     *
     * <p>
     * This setting is ignored in fast mode. In fast mode, strings are always compared in a
     * case-sensitive way.
     *
     * @param parseCaseSensitive
     *            true to be case-sensitive when parsing; false to allow any case.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setParseCaseSensitive(boolean parseCaseSensitive) {
        this.parseCaseSensitive = parseCaseSensitive;
        return this;
    }

    /**
     * Whether to ignore the fractional part of numbers. For example, parses "123.4" to "123" instead of
     * "123.4".
     *
     * @param parseIntegerOnly
     *            true to parse integers only; false to parse integers with their fraction parts
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setParseIntegerOnly(boolean parseIntegerOnly) {
        this.parseIntegerOnly = parseIntegerOnly;
        return this;
    }

    /**
     * Controls certain rules for how strict this parser is when reading strings. See
     * {@link ParseMode#LENIENT} and {@link ParseMode#STRICT}.
     *
     * @param parseMode
     *            Either {@link ParseMode#LENIENT} or {@link ParseMode#STRICT}.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setParseMode(ParseMode parseMode) {
        this.parseMode = parseMode;
        return this;
    }

    /**
     * Whether to ignore the exponential part of numbers. For example, parses "123E4" to "123" instead of
     * "1230000".
     *
     * @param parseNoExponent
     *            true to ignore exponents; false to parse them.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setParseNoExponent(boolean parseNoExponent) {
        this.parseNoExponent = parseNoExponent;
        return this;
    }

    /**
     * Whether to always return a BigDecimal from parse methods. By default, a Long or a BigInteger are
     * returned when possible.
     *
     * @param parseToBigDecimal
     *            true to always return a BigDecimal; false to return a Long or a BigInteger when
     *            possible.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setParseToBigDecimal(boolean parseToBigDecimal) {
        this.parseToBigDecimal = parseToBigDecimal;
        return this;
    }

    /**
     * Sets the PluralRules object to use instead of the default for the locale.
     *
     * @param pluralRules
     *            The object to reference.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setPluralRules(PluralRules pluralRules) {
        this.pluralRules = pluralRules;
        return this;
    }

    /**
     * Sets the prefix to prepend to positive numbers. The prefix will be interpreted literally. For
     * example, if you set a positive prefix of <code>p</code>, then the number 123 will be formatted as
     * "p123" in the locale <em>en-US</em>.
     *
     * <p>
     * For more information on prefixes and suffixes, see {@link MutablePatternModifier}.
     *
     * @param positivePrefix
     *            The CharSequence to prepend to positive numbers.
     * @return The property bag, for chaining.
     * @see #setPositivePrefixPattern
     */
    public DecimalFormatProperties setPositivePrefix(String positivePrefix) {
        this.positivePrefix = positivePrefix;
        return this;
    }

    /**
     * Sets the prefix to prepend to positive numbers. Locale-specific symbols will be substituted into
     * the string according to Unicode Technical Standard #35 (LDML).
     *
     * <p>
     * For more information on prefixes and suffixes, see {@link MutablePatternModifier}.
     *
     * @param positivePrefixPattern
     *            The CharSequence to prepend to positive numbers after locale symbol substitutions take
     *            place.
     * @return The property bag, for chaining.
     * @see #setPositivePrefix
     */
    public DecimalFormatProperties setPositivePrefixPattern(String positivePrefixPattern) {
        this.positivePrefixPattern = positivePrefixPattern;
        return this;
    }

    /**
     * Sets the suffix to append to positive numbers. The suffix will be interpreted literally. For
     * example, if you set a positive suffix of <code>p</code>, then the number 123 will be formatted as
     * "123p" in the locale <em>en-US</em>.
     *
     * <p>
     * For more information on prefixes and suffixes, see {@link MutablePatternModifier}.
     *
     * @param positiveSuffix
     *            The CharSequence to append to positive numbers.
     * @return The property bag, for chaining.
     * @see #setPositiveSuffixPattern
     */
    public DecimalFormatProperties setPositiveSuffix(String positiveSuffix) {
        this.positiveSuffix = positiveSuffix;
        return this;
    }

    /**
     * Sets the suffix to append to positive numbers. Locale-specific symbols will be substituted into
     * the string according to Unicode Technical Standard #35 (LDML).
     *
     * <p>
     * For more information on prefixes and suffixes, see {@link MutablePatternModifier}.
     *
     * @param positiveSuffixPattern
     *            The CharSequence to append to positive numbers after locale symbol substitutions take
     *            place.
     * @return The property bag, for chaining.
     * @see #setPositiveSuffix
     */
    public DecimalFormatProperties setPositiveSuffixPattern(String positiveSuffixPattern) {
        this.positiveSuffixPattern = positiveSuffixPattern;
        return this;
    }

    /**
     * Sets the increment to which to round numbers. For example, with a rounding interval of 0.05, the
     * number 11.17 would be formatted as "11.15" in locale <em>en-US</em> with the default rounding
     * mode.
     *
     * <p>
     * You can use either a rounding increment or significant digits, but not both at the same time.
     *
     * <p>
     * The rounding increment can be specified in a pattern string. For example, the pattern "#,##0.05"
     * corresponds to a rounding interval of 0.05 with 1 minimum integer digit and a grouping size of 3.
     *
     * @param roundingIncrement
     *            The interval to which to round.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setRoundingIncrement(BigDecimal roundingIncrement) {
        this.roundingIncrement = roundingIncrement;
        return this;
    }

    /**
     * Sets the rounding mode, which determines under which conditions extra decimal places are rounded
     * either up or down. See {@link RoundingMode} for details on the choices of rounding mode. The
     * default if not set explicitly is {@link RoundingMode#HALF_EVEN}.
     *
     * <p>
     * This setting is ignored if {@link #setMathContext} is used.
     *
     * @param roundingMode
     *            The rounding mode to use when rounding is required.
     * @return The property bag, for chaining.
     * @see RoundingMode
     * @see #setMathContext
     */
    public DecimalFormatProperties setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
        return this;
    }

    /**
     * Sets the number of digits between grouping separators higher than the least-significant grouping
     * separator. For example, the locale <em>hi</em> uses a primary grouping size of 3 and a secondary
     * grouping size of 2, so the number 1234567 would be formatted as "12,34,567".
     *
     * <p>
     * The two levels of grouping separators can be specified in the pattern string. For example, the
     * <em>hi</em> locale's default decimal format pattern is "#,##,##0.###".
     *
     * @param secondaryGroupingSize
     *            The secondary grouping size.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setSecondaryGroupingSize(int secondaryGroupingSize) {
        this.secondaryGroupingSize = secondaryGroupingSize;
        return this;
    }

    /**
     * Sets whether to always display of a plus sign on positive numbers.
     *
     * <p>
     * If the location of the negative sign is specified by the decimal format pattern (or by the
     * negative prefix/suffix pattern methods), a plus sign is substituted into that location, in
     * accordance with Unicode Technical Standard #35 (LDML) section 3.2.1. Otherwise, the plus sign is
     * prepended to the number. For example, if the decimal format pattern <code>#;#-</code> is used,
     * then formatting 123 would result in "123+" in the locale <em>en-US</em>.
     *
     * <p>
     * This method should be used <em>instead of</em> setting the positive prefix/suffix. The behavior is
     * undefined if alwaysShowPlusSign is set but the positive prefix/suffix already contains a plus
     * sign.
     *
     * @param signAlwaysShown
     *            Whether positive numbers should display a plus sign.
     * @return The property bag, for chaining.
     */
    public DecimalFormatProperties setSignAlwaysShown(boolean signAlwaysShown) {
        this.signAlwaysShown = signAlwaysShown;
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
     * Appends a string containing properties that differ from the default, but without being surrounded
     * by &lt;Properties&gt;.
     */
    public void toStringBare(StringBuilder result) {
        Field[] fields = DecimalFormatProperties.class.getDeclaredFields();
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
     * Custom serialization: save fields along with their name, so that fields can be easily added in the
     * future in any order. Only save fields that differ from their default value.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        writeObjectImpl(oos);
    }

    /* package-private */ void writeObjectImpl(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();

        // Extra int for possible future use
        oos.writeInt(0);

        ArrayList<Field> fieldsToSerialize = new ArrayList<>();
        ArrayList<Object> valuesToSerialize = new ArrayList<>();
        Field[] fields = DecimalFormatProperties.class.getDeclaredFields();
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
