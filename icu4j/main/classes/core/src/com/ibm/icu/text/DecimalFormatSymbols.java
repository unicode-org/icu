/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.ChoiceFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.impl.CurrencyData.CurrencyDisplayInfo;
import com.ibm.icu.impl.CurrencyData.CurrencyFormatInfo;
import com.ibm.icu.impl.CurrencyData.CurrencySpacingInfo;
import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * {@icuenhanced java.text.DecimalFormatSymbols}.{@icu _usage_}
 *
 * This class represents the set of symbols (such as the decimal separator, the grouping
 * separator, and so on) needed by <code>DecimalFormat</code> to format
 * numbers. <code>DecimalFormat</code> creates for itself an instance of
 * <code>DecimalFormatSymbols</code> from its locale data.  If you need to change any of
 * these symbols, you can get the <code>DecimalFormatSymbols</code> object from your
 * <code>DecimalFormat</code> and modify it.
 *
 * @see          java.util.Locale
 * @see          DecimalFormat
 * @author       Mark Davis
 * @author       Alan Liu
 * @stable ICU 2.0
 */
public class DecimalFormatSymbols implements Cloneable, Serializable {
    /**
     * Creates a DecimalFormatSymbols object for the default locale.
     * @stable ICU 2.0
     */
    public DecimalFormatSymbols() {
        initialize(ULocale.getDefault());
    }

    /**
     * Creates a DecimalFormatSymbols object for the given locale.
     * @param locale the locale
     * @stable ICU 2.0
     */
    public DecimalFormatSymbols(Locale locale) {
        initialize(ULocale.forLocale(locale));
    }

    /**
     * {@icu} Creates a DecimalFormatSymbols object for the given locale.
     * @param locale the locale
     * @stable ICU 3.2
     */
    public DecimalFormatSymbols(ULocale locale) {
        initialize(locale);
    }

    /**
     * Returns a DecimalFormatSymbols instance for the default locale.
     *
     * <p><strong>Note:</strong> Unlike
     * <code>java.text.DecimalFormatSymbols#getInstance</code>, this method simply returns
     * <code>new com.ibm.icu.text.DecimalFormatSymbols()</code>.  ICU currently does not
     * support <code>DecimalFormatSymbolsProvider</code>, which was introduced in Java 6.
     *
     * @return A DecimalFormatSymbols instance.
     * @stable ICU 3.8
     */
    public static DecimalFormatSymbols getInstance() {
        return new DecimalFormatSymbols();
    }

    /**
     * Returns a DecimalFormatSymbols instance for the given locale.
     *
     * <p><strong>Note:</strong> Unlike
     * <code>java.text.DecimalFormatSymbols#getInstance</code>, this method simply returns
     * <code>new com.ibm.icu.text.DecimalFormatSymbols(locale)</code>.  ICU currently does
     * not support <code>DecimalFormatSymbolsProvider</code>, which was introduced in Java
     * 6.
     *
     * @param locale the locale.
     * @return A DecimalFormatSymbols instance.
     * @stable ICU 3.8
     */
    public static DecimalFormatSymbols getInstance(Locale locale) {
        return new DecimalFormatSymbols(locale);
    }

    /**
     * Returns a DecimalFormatSymbols instance for the given locale.
     *
     * <p><strong>Note:</strong> Unlike
     * <code>java.text.DecimalFormatSymbols#getInstance</code>, this method simply returns
     * <code>new com.ibm.icu.text.DecimalFormatSymbols(locale)</code>.  ICU currently does
     * not support <code>DecimalFormatSymbolsProvider</code>, which was introduced in Java
     * 6.
     *
     * @param locale the locale.
     * @return A DecimalFormatSymbols instance.
     * @stable ICU 3.8
     */
    public static DecimalFormatSymbols getInstance(ULocale locale) {
        return new DecimalFormatSymbols(locale);
    }

    /**
     * Returns an array of all locales for which the <code>getInstance</code> methods of
     * this class can return localized instances.
     *
     * <p><strong>Note:</strong> Unlike
     * <code>java.text.DecimalFormatSymbols#getAvailableLocales</code>, this method simply
     * returns the array of <code>Locale</code>s available for this class.  ICU currently
     * does not support <code>DecimalFormatSymbolsProvider</code>, which was introduced in
     * Java 6.
     *
     * @return An array of <code>Locale</code>s for which localized
     * <code>DecimalFormatSymbols</code> instances are available.
     * @stable ICU 3.8
     */
    public static Locale[] getAvailableLocales() {
        return ICUResourceBundle.getAvailableLocales();
    }

    /**
     * {@icu} Returns an array of all locales for which the <code>getInstance</code>
     * methods of this class can return localized instances.
     *
     * <p><strong>Note:</strong> Unlike
     * <code>java.text.DecimalFormatSymbols#getAvailableLocales</code>, this method simply
     * returns the array of <code>ULocale</code>s available in this class.  ICU currently
     * does not support <code>DecimalFormatSymbolsProvider</code>, which was introduced in
     * Java 6.
     *
     * @return An array of <code>ULocale</code>s for which localized
     * <code>DecimalFormatSymbols</code> instances are available.
     * @stable ICU 3.8 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public static ULocale[] getAvailableULocales() {
        return ICUResourceBundle.getAvailableULocales();
    }


    /**
     * Returns the character used for zero. Different for Arabic, etc.
     * @return the character
     * @stable ICU 2.0
     */
    public char getZeroDigit() {
        if ( digits != null ) {
            return digits[0];
        } else {
            return zeroDigit;
        }
    }
    /**
     * Returns the array of characters used as digits, in order from 0 through 9
     * @return The array
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public char[] getDigits() {
        if ( digits != null ) {
            return digits;
        } else {
            char [] digitArray = new char[10];
            for ( int i = 0 ; i < 10 ; i++ ) {
                digitArray[i] = (char) (zeroDigit + i);
            }
            return digitArray;
        }
    }

    /**
     * Sets the character used for zero.
     * @param zeroDigit the zero character.
     * @stable ICU 2.0
     */
    public void setZeroDigit(char zeroDigit) {
        if ( digits != null ) {
            this.digits[0] = zeroDigit;
            if (Character.digit(zeroDigit,10) == 0) {
                for ( int i = 1 ; i < 10 ; i++ ) {
                    this.digits[i] = (char)(zeroDigit+i);
                }
            }
        } else {
            this.zeroDigit = zeroDigit;
        }
    }

    /**
     * Returns the character used to represent a significant digit in a pattern.
     * @return the significant digit pattern character
     * @stable ICU 3.0
     */
    public char getSignificantDigit() {
        return sigDigit;
    }

    /**
     * Sets the character used to represent a significant digit in a pattern.
     * @param sigDigit the significant digit pattern character
     * @stable ICU 3.0
     */
    public void setSignificantDigit(char sigDigit) {
        this.sigDigit = sigDigit;
    }

    /**
     * Returns the character used for thousands separator. Different for French, etc.
     * @return the thousands character
     * @stable ICU 2.0
     */
    public char getGroupingSeparator() {
        return groupingSeparator;
    }

    /**
     * Sets the character used for thousands separator. Different for French, etc.
     * @param groupingSeparator the thousands character
     * @stable ICU 2.0
     */
    public void setGroupingSeparator(char groupingSeparator) {
        this.groupingSeparator = groupingSeparator;
    }

    /**
     * Returns the character used for decimal sign. Different for French, etc.
     * @return the decimal character
     * @stable ICU 2.0
     */
    public char getDecimalSeparator() {
        return decimalSeparator;
    }

    /**
     * Sets the character used for decimal sign. Different for French, etc.
     * @param decimalSeparator the decimal character
     * @stable ICU 2.0
     */
    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    /**
     * Returns the character used for mille percent sign. Different for Arabic, etc.
     * @return the mille percent character
     * @stable ICU 2.0
     */
    public char getPerMill() {
        return perMill;
    }

    /**
     * Sets the character used for mille percent sign. Different for Arabic, etc.
     * @param perMill the mille percent character
     * @stable ICU 2.0
     */
    public void setPerMill(char perMill) {
        this.perMill = perMill;
    }

    /**
     * Returns the character used for percent sign. Different for Arabic, etc.
     * @return the percent character
     * @stable ICU 2.0
     */
    public char getPercent() {
        return percent;
    }

    /**
     * Sets the character used for percent sign. Different for Arabic, etc.
     * @param percent the percent character
     * @stable ICU 2.0
     */
    public void setPercent(char percent) {
        this.percent = percent;
    }

    /**
     * Returns the character used for a digit in a pattern.
     * @return the digit pattern character
     * @stable ICU 2.0
     */
    public char getDigit() {
        return digit;
    }

    /**
     * Sets the character used for a digit in a pattern.
     * @param digit the digit pattern character
     * @stable ICU 2.0
     */
    public void setDigit(char digit) {
        this.digit = digit;
    }

    /**
     * Returns the character used to separate positive and negative subpatterns
     * in a pattern.
     * @return the pattern separator character
     * @stable ICU 2.0
     */
    public char getPatternSeparator() {
        return patternSeparator;
    }

    /**
     * Sets the character used to separate positive and negative subpatterns
     * in a pattern.
     * @param patternSeparator the pattern separator character
     * @stable ICU 2.0
     */
    public void setPatternSeparator(char patternSeparator) {
        this.patternSeparator = patternSeparator;
    }

    /**
     * Returns the String used to represent infinity. Almost always left
     * unchanged.
     * @return the Infinity string
     * @stable ICU 2.0
     */
     //Bug 4194173 [Richard/GCL]

    public String getInfinity() {
        return infinity;
    }

    /**
     * Sets the String used to represent infinity. Almost always left
     * unchanged.
     * @param infinity the Infinity String
     * @stable ICU 2.0
     */
    public void setInfinity(String infinity) {
        this.infinity = infinity;
    }

    /**
     * Returns the String used to represent NaN. Almost always left
     * unchanged.
     * @return the NaN String
     * @stable ICU 2.0
     */
     //Bug 4194173 [Richard/GCL]
    public String getNaN() {
        return NaN;
    }

    /**
     * Sets the String used to represent NaN. Almost always left
     * unchanged.
     * @param NaN the NaN String
     * @stable ICU 2.0
     */
    public void setNaN(String NaN) {
        this.NaN = NaN;
    }

    /**
     * Returns the character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     * @return the minus sign character
     * @stable ICU 2.0
     */
    public char getMinusSign() {
        return minusSign;
    }

    /**
     * Sets the character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     * @param minusSign the minus sign character
     * @stable ICU 2.0
     */
    public void setMinusSign(char minusSign) {
        this.minusSign = minusSign;
    }

    /**
     * Returns the string denoting the local currency.
     * @return the local currency String.
     * @stable ICU 2.0
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Sets the string denoting the local currency.
     * @param currency the local currency String.
     * @stable ICU 2.0
     */
    public void setCurrencySymbol(String currency) {
        currencySymbol = currency;
    }

    /**
     * Returns the international string denoting the local currency.
     * @return the international string denoting the local currency
     * @stable ICU 2.0
     */
    public String getInternationalCurrencySymbol() {
        return intlCurrencySymbol;
    }

    /**
     * Sets the international string denoting the local currency.
     * @param currency the international string denoting the local currency.
     * @stable ICU 2.0
     */
    public void setInternationalCurrencySymbol(String currency) {
        intlCurrencySymbol = currency;
    }

    /**
     * Returns the currency symbol, for JDK 1.4 compatibility only.
     * ICU clients should use the Currency API directly.
     * @return the currency used, or null
     * @stable ICU 3.4
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Sets the currency.
     *
     * <p><strong>Note:</strong> ICU does not use the DecimalFormatSymbols for the currency
     * any more.  This API is present for API compatibility only.
     *
     * <p>This also sets the currency symbol attribute to the currency's symbol
     * in the DecimalFormatSymbols' locale, and the international currency
     * symbol attribute to the currency's ISO 4217 currency code.
     *
     * @param currency the new currency to be used
     * @throws NullPointerException if <code>currency</code> is null
     * @see #setCurrencySymbol
     * @see #setInternationalCurrencySymbol
     *
     * @stable ICU 3.4
     */
    public void setCurrency(Currency currency) {
        if (currency == null) {
            throw new NullPointerException();
        }
        this.currency = currency;
        intlCurrencySymbol = currency.getCurrencyCode();
        currencySymbol = currency.getSymbol(requestedLocale);
    }

    /**
     * Returns the monetary decimal separator.
     * @return the monetary decimal separator character
     * @stable ICU 2.0
     */
    public char getMonetaryDecimalSeparator() {
        return monetarySeparator;
    }

    /**
     * {@icu} Returns the monetary grouping separator.
     * @return the monetary grouping separator character
     * @stable ICU 3.6
     */
    public char getMonetaryGroupingSeparator() {
        return monetaryGroupingSeparator;
    }

    /**
     * Internal API for NumberFormat
     * @return String currency pattern string
     */
    String getCurrencyPattern() {
        return currencyPattern;
    }

    /**
     * Sets the monetary decimal separator.
     * @param sep the monetary decimal separator character
     * @stable ICU 2.0
     */
    public void setMonetaryDecimalSeparator(char sep) {
        monetarySeparator = sep;
    }

    /**
     * Sets the monetary decimal separator.
     * @param sep the monetary decimal separator character
     * @stable ICU 3.6
     */
    public void setMonetaryGroupingSeparator(char sep) {
        monetaryGroupingSeparator = sep;
    }

    /**
     * {@icu} Returns the string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     * @return the localized exponent symbol, used in localized patterns
     * and formatted strings
     * @see #setExponentSeparator
     * @stable ICU 2.0
     */
    public String getExponentSeparator() {
        return exponentSeparator;
    }

    /**
     * {@icu} Sets the string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     * @param exp the localized exponent symbol, used in localized patterns
     * and formatted strings
     * @see #getExponentSeparator
     * @stable ICU 2.0
     */
    public void setExponentSeparator(String exp) {
        exponentSeparator = exp;
    }

    /**
     * {@icu} Returns the localized plus sign.
     * @return the plus sign, used in localized patterns and formatted
     * strings
     * @see #setPlusSign
     * @see #setMinusSign
     * @see #getMinusSign
     * @stable ICU 2.0
     */
    public char getPlusSign() {
        return plusSign;
    }

    /**
     * {@icu} Sets the localized plus sign.
     * @param plus the plus sign, used in localized patterns and formatted
     * strings
     * @see #getPlusSign
     * @see #setMinusSign
     * @see #getMinusSign
     * @stable ICU 2.0
     */
    public void setPlusSign(char plus) {
        plusSign = plus;
    }

    /**
     * {@icu} Returns the character used to pad numbers out to a specified width.  This is
     * not the pad character itself; rather, it is the special pattern character
     * <em>preceding</em> the pad character.  In the pattern "*_#,##0", '*' is the pad
     * escape, and '_' is the pad character.
     * @return the character
     * @see #setPadEscape
     * @see DecimalFormat#getFormatWidth
     * @see DecimalFormat#getPadPosition
     * @see DecimalFormat#getPadCharacter
     * @stable ICU 2.0
     */
    public char getPadEscape() {
        return padEscape;
    }

    /**
     * {@icu} Sets the character used to pad numbers out to a specified width.  This is not
     * the pad character itself; rather, it is the special pattern character
     * <em>preceding</em> the pad character.  In the pattern "*_#,##0", '*' is the pad
     * escape, and '_' is the pad character.
     * @see #getPadEscape
     * @see DecimalFormat#setFormatWidth
     * @see DecimalFormat#setPadPosition
     * @see DecimalFormat#setPadCharacter
     * @stable ICU 2.0
     */
    public void setPadEscape(char c) {
        padEscape = c;
    }

    /**
     * {@icu} Indicates the currency match pattern used in {@link #getPatternForCurrencySpacing}.
     * @draft ICU 4.2
     */
    public static final int CURRENCY_SPC_CURRENCY_MATCH = 0;

    /**
     * {@icu} Indicates the surrounding match pattern used in {@link
     * #getPatternForCurrencySpacing}.
     * @draft ICU 4.2
     */
    public static final int CURRENCY_SPC_SURROUNDING_MATCH = 1;

    /**
     * {@icu} Indicates the insertion value used in {@link #getPatternForCurrencySpacing}.
     * @draft ICU 4.4
     */
    public static final int CURRENCY_SPC_INSERT = 2;

    private String[] currencySpcBeforeSym;
    private String[] currencySpcAfterSym;

    /**
     * {@icu} Returns the desired currency spacing value. Original values come from ICU's
     * CLDR data based on the locale provided during construction, and can be null.  These
     * values govern what and when text is inserted between a currency code/name/symbol
     * and the currency amount when formatting money.
     *
     * <p>For more information, see <a href="http://www.unicode.org/reports/tr35/#Currencies"
     * >UTS#35 section 5.10.2</a>.
     *
     * <p><strong>Note:</strong> ICU4J does not currently use this information.
     *
     * @param itemType one of CURRENCY_SPC_CURRENCY_MATCH, CURRENCY_SPC_SURROUNDING_MATCH
     * or CURRENCY_SPC_INSERT
     * @param beforeCurrency true to get the <code>beforeCurrency</code> values, false
     * to get the <code>afterCurrency</code> values.
     * @return the value, or null.
     * @see #setPatternForCurrencySpacing(int, boolean, String)
     * @draft ICU 4.2
     */
    public String getPatternForCurrencySpacing(int itemType, boolean beforeCurrency)  {
        if (itemType < CURRENCY_SPC_CURRENCY_MATCH ||
            itemType > CURRENCY_SPC_INSERT ) {
            throw new IllegalArgumentException("unknown currency spacing: " + itemType);
        }
        if (beforeCurrency) {
            return currencySpcBeforeSym[itemType];
        }
        return currencySpcAfterSym[itemType];
    }

    /**
     * {@icu} Sets the indicated currency spacing pattern or value. See {@link
     * #getPatternForCurrencySpacing} for more information.
     *
     * <p>Values for currency match and surrounding match must be {@link
     * com.ibm.icu.text.UnicodeSet} patterns. Values for insert can be any string.
     *
     * <p><strong>Note:</strong> ICU4J does not currently use this information.
     *
     * @param itemType one of CURRENCY_SPC_CURRENCY_MATCH, CURRENCY_SPC_SURROUNDING_MATCH
     * or CURRENCY_SPC_INSERT
     * @param beforeCurrency true if the pattern is for before the currency symbol.
     * false if the pattern is for after it.
     * @param  pattern string to override current setting; can be null.
     * @see #getPatternForCurrencySpacing(int, boolean)
     * @draft ICU 4.2
     */
    public void setPatternForCurrencySpacing(int itemType, boolean beforeCurrency, String pattern) {
        if (itemType < CURRENCY_SPC_CURRENCY_MATCH ||
            itemType > CURRENCY_SPC_INSERT ) {
            throw new IllegalArgumentException("unknown currency spacing: " + itemType);
        }
        if (beforeCurrency) {
            currencySpcBeforeSym[itemType] = pattern;
        } else {
            currencySpcAfterSym[itemType] = pattern;
        }
    }

    /**
     * Returns the locale for which this object was constructed.
     * @return the locale for which this object was constructed
     * @stable ICU 2.0
     */
    public Locale getLocale() {
        return requestedLocale;
    }

    /**
     * Returns the locale for which this object was constructed.
     * @return the locale for which this object was constructed
     * @stable ICU 3.2
     */
    public ULocale getULocale() {
        return ulocale;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 2.0
     */
    public Object clone() {
        try {
            return (DecimalFormatSymbols) super.clone();
            // other fields are bit-copied
        } catch (CloneNotSupportedException e) {
            ///CLOVER:OFF
            throw new IllegalStateException();
            ///CLOVER:ON
        }
    }

    /**
     * {@inheritDoc}
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        DecimalFormatSymbols other = (DecimalFormatSymbols) obj;
        for (int i = 0; i <= CURRENCY_SPC_INSERT; i++) {
            if (!currencySpcBeforeSym[i].equals(other.currencySpcBeforeSym[i])) {
                return false;
            }
            if (!currencySpcAfterSym[i].equals(other.currencySpcAfterSym[i])) {
                return false;
            }
        }
        
        if ( other.digits == null ) {
            for (int i = 0 ; i < 10 ; i++) {
                if (digits[i] != other.zeroDigit + i) {
                    return false;
                }
            }
        } else if (!Arrays.equals(digits,other.digits)) {
                    return false;
        }

        return (
        groupingSeparator == other.groupingSeparator &&
        decimalSeparator == other.decimalSeparator &&
        percent == other.percent &&
        perMill == other.perMill &&
        digit == other.digit &&
        minusSign == other.minusSign &&
        patternSeparator == other.patternSeparator &&
        infinity.equals(other.infinity) &&
        NaN.equals(other.NaN) &&
        currencySymbol.equals(other.currencySymbol) &&
        intlCurrencySymbol.equals(other.intlCurrencySymbol) &&
        padEscape == other.padEscape &&
        plusSign == other.plusSign &&
        exponentSeparator.equals(other.exponentSeparator) &&
        monetarySeparator == other.monetarySeparator &&
        monetaryGroupingSeparator == other.monetaryGroupingSeparator);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 2.0
     */
    public int hashCode() {
            int result = digits[0];
            result = result * 37 + groupingSeparator;
            result = result * 37 + decimalSeparator;
            return result;
    }

    /**
     * Initializes the symbols from the LocaleElements resource bundle.
     * Note: The organization of LocaleElements badly needs to be
     * cleaned up.
     */
    private void initialize( ULocale locale ) {
        this.requestedLocale = locale.toLocale();
        this.ulocale = locale;

        String nsName;
        // Attempt to set the zero digit based on the numbering system for the locale requested
        NumberingSystem ns = NumberingSystem.getInstance(locale);
        digits = new char[10];
        if ( ns != null && ns.getRadix() == 10 && !ns.isAlgorithmic() &&
             NumberingSystem.isValidDigitString(ns.getDescription())) {
            String digitString = ns.getDescription();
            digits[0] = digitString.charAt(0);
            digits[1] = digitString.charAt(1);
            digits[2] = digitString.charAt(2);
            digits[3] = digitString.charAt(3);
            digits[4] = digitString.charAt(4);
            digits[5] = digitString.charAt(5);
            digits[6] = digitString.charAt(6);
            digits[7] = digitString.charAt(7);
            digits[8] = digitString.charAt(8);
            digits[9] = digitString.charAt(9);
            nsName = ns.getName();
        } else {
            digits[0] = DecimalFormat.PATTERN_ZERO_DIGIT;
            digits[1] = DecimalFormat.PATTERN_ONE_DIGIT;
            digits[2] = DecimalFormat.PATTERN_TWO_DIGIT;
            digits[3] = DecimalFormat.PATTERN_THREE_DIGIT;
            digits[4] = DecimalFormat.PATTERN_FOUR_DIGIT;
            digits[5] = DecimalFormat.PATTERN_FIVE_DIGIT;
            digits[6] = DecimalFormat.PATTERN_SIX_DIGIT;
            digits[7] = DecimalFormat.PATTERN_SEVEN_DIGIT;
            digits[8] = DecimalFormat.PATTERN_EIGHT_DIGIT;
            digits[9] = DecimalFormat.PATTERN_NINE_DIGIT;
            nsName = "latn"; // Default numbering system
        }

        /* try the cache first */
        String[][] data = cachedLocaleData.get(locale);
        String[] numberElements;
        if (data == null) {  /* cache miss */
            data = new String[1][];
            ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.
                getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
            boolean isLatn = nsName.equals("latn");
            String baseKey = "NumberElements/" + nsName + "/symbols/";
            String latnKey = "NumberElements/latn/symbols/";
            String[] symbolKeys = { "decimal", "group", "list", "percentSign", "minusSign", "plusSign", "exponential", "perMille", "infinity", "nan", "currencyDecimal", "currencyGroup" };
            String[] fallbackElements = { ".", ",", ";", "%", "-", "+", "E", "\u2030", "\u221e", "NaN", null, null };
            String[] symbolsArray = new String[symbolKeys.length];
            for ( int i = 0 ; i < symbolKeys.length; i++ ) {
                try {
                    symbolsArray[i] = rb.getStringWithFallback(baseKey+symbolKeys[i]);
                } catch (MissingResourceException ex) {
                    if (!isLatn) { // Fall back to latn numbering system for symbols if desired symbol isn't found.
                        try {
                            symbolsArray[i] = rb.getStringWithFallback(latnKey+symbolKeys[i]);
                        } catch (MissingResourceException ex1) {
                            symbolsArray[i] = fallbackElements[i];
                        }
                    } else {
                        symbolsArray[i] = fallbackElements[i];
                    }
                }
            }

            data[0] = symbolsArray;
            /* update cache */
            cachedLocaleData.put(locale, data);
        }
        numberElements = data[0];

        ICUResourceBundle r = (ICUResourceBundle)UResourceBundle.
            getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);

        // TODO: Determine actual and valid locale correctly.
        ULocale uloc = r.getULocale();
        setLocale(uloc, uloc);


        decimalSeparator = numberElements[0].charAt(0);
        groupingSeparator = numberElements[1].charAt(0);
        patternSeparator = numberElements[2].charAt(0);
        percent = numberElements[3].charAt(0);
        minusSign = numberElements[4].charAt(0);
        plusSign  =numberElements[5].charAt(0);
        exponentSeparator = numberElements[6];
        perMill = numberElements[7].charAt(0);
        infinity = numberElements[8];
        NaN = numberElements[9];

        if ( numberElements[10] != null) {
            monetarySeparator = numberElements[10].charAt(0);
        } else {
            monetarySeparator = decimalSeparator;
        }
        
        if ( numberElements[11] != null) {
            monetaryGroupingSeparator = numberElements[11].charAt(0);
        } else {
            monetaryGroupingSeparator = groupingSeparator;
        }
        
        digit = DecimalFormat.PATTERN_DIGIT;  // Localized pattern character no longer in CLDR
        padEscape = DecimalFormat.PATTERN_PAD_ESCAPE;
        sigDigit  = DecimalFormat.PATTERN_SIGNIFICANT_DIGIT;


        CurrencyDisplayInfo info = CurrencyData.provider.getInstance(locale, true);

        // Obtain currency data from the currency API.  This is strictly
        // for backward compatibility; we don't use DecimalFormatSymbols
        // for currency data anymore.
        String currname = null;
        currency = Currency.getInstance(locale);
        if (currency != null) {
            intlCurrencySymbol = currency.getCurrencyCode();
            boolean[] isChoiceFormat = new boolean[1];
            currname = currency.getName(locale, Currency.SYMBOL_NAME, isChoiceFormat);
            // If this is a ChoiceFormat currency, then format an
            // arbitrary value; pick something != 1; more common.
            currencySymbol = isChoiceFormat[0]
                ? new ChoiceFormat(currname).format(2.0)
                : currname;
        } else {
            intlCurrencySymbol = "XXX";
            currencySymbol = "\u00A4"; // 'OX' currency symbol
        }


        // Get currency pattern/separator overrides if they exist.
        Currency curr = Currency.getInstance(locale);
        if (curr != null){
            CurrencyFormatInfo fmtInfo = info.getFormatInfo(curr.getCurrencyCode());
            if (fmtInfo != null) {
                currencyPattern = fmtInfo.currencyPattern;
                monetarySeparator = fmtInfo.monetarySeparator;
                monetaryGroupingSeparator = fmtInfo.monetaryGroupingSeparator;
            }
        }

        // Get currency spacing data.
        currencySpcBeforeSym = new String[3];
        currencySpcAfterSym = new String[3];
        initSpacingInfo(info.getSpacingInfo());
    }

    private void initSpacingInfo(CurrencySpacingInfo spcInfo) {
        currencySpcBeforeSym[CURRENCY_SPC_CURRENCY_MATCH] = spcInfo.beforeCurrencyMatch;
        currencySpcBeforeSym[CURRENCY_SPC_SURROUNDING_MATCH] = spcInfo.beforeContextMatch;
        currencySpcBeforeSym[CURRENCY_SPC_INSERT] = spcInfo.beforeInsert;
        currencySpcAfterSym[CURRENCY_SPC_CURRENCY_MATCH] = spcInfo.afterCurrencyMatch;
        currencySpcAfterSym[CURRENCY_SPC_SURROUNDING_MATCH] = spcInfo.afterContextMatch;
        currencySpcAfterSym[CURRENCY_SPC_INSERT] = spcInfo.afterInsert;
    }

    /**
     * Reads the default serializable fields, then if <code>serialVersionOnStream</code>
     * is less than 1, initialize <code>monetarySeparator</code> to be
     * the same as <code>decimalSeparator</code> and <code>exponential</code>
     * to be 'E'.
     * Finally, sets serialVersionOnStream back to the maximum allowed value so that
     * default serialization will work properly if this object is streamed out again.
     */
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {

        // TODO: it looks to me {dlf} that the serialization code was never updated
        // to handle the actual/valid ulocale fields.

        stream.defaultReadObject();
        ///CLOVER:OFF
        // we don't have data for these old serialized forms any more
        if (serialVersionOnStream < 1) {
            // Didn't have monetarySeparator or exponential field;
            // use defaults.
            monetarySeparator = decimalSeparator;
            exponential = 'E';
        }
        if (serialVersionOnStream < 2) {
            padEscape = DecimalFormat.PATTERN_PAD_ESCAPE;
            plusSign = DecimalFormat.PATTERN_PLUS_SIGN;
            exponentSeparator = String.valueOf(exponential);
            // Although we read the exponential field on stream to create the
            // exponentSeparator, we don't do the reverse, since scientific
            // notation isn't supported by the old classes, even though the
            // symbol is there.
        }
        ///CLOVER:ON
        if (serialVersionOnStream < 3) {
            // Resurrected objects from old streams will have no
            // locale.  There is no 100% fix for this.  A
            // 90% fix is to construct a mapping of data back to
            // locale, perhaps a hash of all our members.  This is
            // expensive and doesn't seem worth it.
            requestedLocale = Locale.getDefault();
        }
        if (serialVersionOnStream < 4) {
            // use same default behavior as for versions with no Locale
            ulocale = ULocale.forLocale(requestedLocale);
        }
        if (serialVersionOnStream < 5) {
            // use the same one for groupingSeparator
            monetaryGroupingSeparator = groupingSeparator;
        }
        if (serialVersionOnStream < 6) {
            // Set null to CurrencySpacing related fields.
            if (currencySpcBeforeSym == null) {
                currencySpcBeforeSym = new String[CURRENCY_SPC_INSERT+1];
            }
            if (currencySpcAfterSym == null) {
                currencySpcAfterSym = new String[CURRENCY_SPC_INSERT+1];
            }
            initSpacingInfo(CurrencyData.CurrencySpacingInfo.DEFAULT);
        }
        serialVersionOnStream = currentSerialVersion;

    // recreate
    currency = Currency.getInstance(intlCurrencySymbol);
    }

    /**
     * Character used for zero.  This remains only for backward compatibility
     * purposes.  The digits array below is now used to actively store the digits.
     *
     * @serial
     * @see #getZeroDigit
     */
    private  char    zeroDigit;
    
    /**
     * Array of characters used for the digits 0-9 in order.
     *
     */   
    private  char    digits[];

    /**
     * Character used for thousands separator.
     *
     * @serial
     * @see #getGroupingSeparator
     */
    private  char    groupingSeparator;

    /**
     * Character used for decimal sign.
     *
     * @serial
     * @see #getDecimalSeparator
     */
    private  char    decimalSeparator;

    /**
     * Character used for mille percent sign.
     *
     * @serial
     * @see #getPerMill
     */
    private  char    perMill;

    /**
     * Character used for percent sign.
     * @serial
     * @see #getPercent
     */
    private  char    percent;

    /**
     * Character used for a digit in a pattern.
     *
     * @serial
     * @see #getDigit
     */
    private  char    digit;

    /**
     * Character used for a significant digit in a pattern.
     *
     * @serial
     * @see #getSignificantDigit
     */
    private  char    sigDigit;

    /**
     * Character used to separate positive and negative subpatterns
     * in a pattern.
     *
     * @serial
     * @see #getPatternSeparator
     */
    private  char    patternSeparator;

    /**
     * Character used to represent infinity.
     * @serial
     * @see #getInfinity
     */
    private  String  infinity;

    /**
     * Character used to represent NaN.
     * @serial
     * @see #getNaN
     */
    private  String  NaN;

    /**
     * Character used to represent minus sign.
     * @serial
     * @see #getMinusSign
     */
    private  char    minusSign;

    /**
     * String denoting the local currency, e.g. "$".
     * @serial
     * @see #getCurrencySymbol
     */
    private  String  currencySymbol;

    /**
     * International string denoting the local currency, e.g. "USD".
     * @serial
     * @see #getInternationalCurrencySymbol
     */
    private  String  intlCurrencySymbol;

    /**
     * The decimal separator used when formatting currency values.
     * @serial
     * @see #getMonetaryDecimalSeparator
     */
    private  char    monetarySeparator; // Field new in JDK 1.1.6

    /**
     * The decimal separator used when formatting currency values.
     * @serial
     * @see #getMonetaryGroupingSeparator
     */
    private  char    monetaryGroupingSeparator; // Field new in JDK 1.1.6

    /**
     * The character used to distinguish the exponent in a number formatted
     * in exponential notation, e.g. 'E' for a number such as "1.23E45".
     * <p>
     * Note that this field has been superseded by <code>exponentSeparator</code>.
     * It is retained for backward compatibility.
     *
     * @serial
     */
    private  char    exponential;       // Field new in JDK 1.1.6

    /**
     * The string used to separate the mantissa from the exponent.
     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
     * <p>
     * Note that this supersedes the <code>exponential</code> field.
     *
     * @serial
     * @since AlphaWorks
     */
    private String exponentSeparator;

    /**
     * The character used to indicate a padding character in a format,
     * e.g., '*' in a pattern such as "$*_#,##0.00".
     * @serial
     * @since AlphaWorks
     */
    private char padEscape;

    /**
     * The character used to indicate a plus sign.
     * @serial
     * @since AlphaWorks
     */
    private char plusSign;

    /**
     * The locale for which this object was constructed.  Set to the
     * default locale for objects resurrected from old streams.
     * @since ICU 2.2
     */
    private Locale requestedLocale;

    /**
     * The requested ULocale.  We keep the old locale for serialization compatibility.
     * @since IDU 3.2
     */
    private ULocale ulocale;

    // Proclaim JDK 1.1 FCS compatibility
    private static final long serialVersionUID = 5772796243397350300L;

    // The internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.5
    // - 1 for version from JDK 1.1.6, which includes two new fields:
    //     monetarySeparator and exponential.
    // - 2 for version from AlphaWorks, which includes 3 new fields:
    //     padEscape, exponentSeparator, and plusSign.
    // - 3 for ICU 2.2, which includes the locale field
    // - 4 for ICU 3.2, which includes the ULocale field
    // - 5 for ICU 3.6, which includes the monetaryGroupingSeparator field
    // - 6 for ICU 4.2, which includes the currencySpc* fields
    private static final int currentSerialVersion = 6;

    /**
     * Describes the version of <code>DecimalFormatSymbols</code> present on the stream.
     * Possible values are:
     * <ul>
     * <li><b>0</b> (or uninitialized): versions prior to JDK 1.1.6.
     *
     * <li><b>1</b>: Versions written by JDK 1.1.6 or later, which includes
     *      two new fields: <code>monetarySeparator</code> and <code>exponential</code>.
     * <li><b>2</b>: Version for AlphaWorks.  Adds padEscape, exponentSeparator,
     *      and plusSign.
     * <li><b>3</b>: Version for ICU 2.2, which adds locale.
     * <li><b>4</b>: Version for ICU 3.2, which adds ulocale.
     * <li><b>5</b>: Version for ICU 3.6, which adds monetaryGroupingSeparator.
     * <li><b>6</b>: Version for ICU 4.2, which adds currencySpcBeforeSym and 
     * currencySpcAfterSym.
     * </ul>
     * When streaming out a <code>DecimalFormatSymbols</code>, the most recent format
     * (corresponding to the highest allowable <code>serialVersionOnStream</code>)
     * is always written.
     *
     * @serial
     */
    private int serialVersionOnStream = currentSerialVersion;

    /**
     * cache to hold the NumberElements of a Locale.
     */
    private static final ICUCache<ULocale, String[][]> cachedLocaleData =
        new SimpleCache<ULocale, String[][]>();

    /**
     *
     */
    private String  currencyPattern = null;

    // -------- BEGIN ULocale boilerplate --------

    /**
     * {@icu} Returns the locale that was used to create this object, or null.
     * This may may differ from the locale requested at the time of
     * this object's creation.  For example, if an object is created
     * for locale <tt>en_US_CALIFORNIA</tt>, the actual data may be
     * drawn from <tt>en</tt> (the <i>actual</i> locale), and
     * <tt>en_US</tt> may be the most specific locale that exists (the
     * <i>valid</i> locale).
     *
     * <p>Note: The <i>actual</i> locale is returned correctly, but the <i>valid</i>
     * locale is not, in most cases.
     * @param type type of information requested, either {@link
     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
     * @return the information specified by <i>type</i>, or null if
     * this object was not constructed from locale data.
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @draft ICU 2.8 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public final ULocale getLocale(ULocale.Type type) {
        return type == ULocale.ACTUAL_LOCALE ?
            this.actualLocale : this.validLocale;
    }

    /**
     * {@icu} Sets information about the locales that were used to create this
     * object.  If the object was not constructed from locale data,
     * both arguments should be set to null.  Otherwise, neither
     * should be null.  The actual locale must be at the same level or
     * less specific than the valid locale.  This method is intended
     * for use by factories or other entities that create objects of
     * this class.
     * @param valid the most specific locale containing any resource
     * data, or null
     * @param actual the locale containing data used to construct this
     * object, or null
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     */
    final void setLocale(ULocale valid, ULocale actual) {
        // Change the following to an assertion later
        if ((valid == null) != (actual == null)) {
            ///CLOVER:OFF
            throw new IllegalArgumentException();
            ///CLOVER:ON
        }
        // Another check we could do is that the actual locale is at
        // the same level or less specific than the valid locale.
        this.validLocale = valid;
        this.actualLocale = actual;
    }

    /**
     * The most specific locale containing any resource data, or null.
     * @see com.ibm.icu.util.ULocale
     */
    private ULocale validLocale;

    /**
     * The locale containing data used to construct this object, or
     * null.
     * @see com.ibm.icu.util.ULocale
     */
    private ULocale actualLocale;

    // not serialized, reconstructed from intlCurrencyCode
    private transient Currency currency;

    // -------- END ULocale boilerplate --------
}
