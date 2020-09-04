// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;


import java.io.Serializable;
import java.util.Locale;

import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;

/**
 * This class represents the set of symbols (such as the decimal separator, the
 * grouping separator, and so on) needed by <code>DecimalFormat</code> to format
 * numbers. <code>DecimalFormat</code> creates for itself an instance of
 * <code>DecimalFormatSymbols</code> from its locale data.  If you need to
 * change any of these symbols, you can get the
 * <code>DecimalFormatSymbols</code> object from your <code>DecimalFormat</code>
 * and modify it.
 *
 * <p><strong>This is an enhanced version of <code>DecimalFormatSymbols</code> that
 * is based on the standard version in the JDK.  New or changed functionality
 * is labeled
 * <strong><font face=helvetica color=red>NEW</font></strong>.</strong>
 *
 * @see          java.util.Locale
 * @see          DecimalFormat
 * @author       Mark Davis
 * @author       Alan Liu
 * @stable ICU 2.0
 */
final public class DecimalFormatSymbols implements Cloneable, Serializable {

    private static final long serialVersionUID =1L;

    /**
     * @internal
     */
    public final java.text.DecimalFormatSymbols dfs;

    /**
     * @internal
     */
    public DecimalFormatSymbols(java.text.DecimalFormatSymbols delegate) {
        this.dfs = delegate;
    }
        
    /**
     * Create a DecimalFormatSymbols object for the default locale.
     * @stable ICU 2.0
     */
    public DecimalFormatSymbols() {
        this(new java.text.DecimalFormatSymbols(ULocale.getDefault(Category.FORMAT).toLocale()));
    }
        
    /**
     * Create a DecimalFormatSymbols object for the given locale.
     * @param locale the locale
     * @stable ICU 2.0
     */
    public DecimalFormatSymbols(Locale locale) {
        this(new java.text.DecimalFormatSymbols(locale));
    }
        
    /**
     * Create a DecimalFormatSymbols object for the given locale.
     * @param locale the locale
     * @stable ICU 3.2
     */
    public DecimalFormatSymbols(ULocale locale) {
        this(new java.text.DecimalFormatSymbols(locale.toLocale()));
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

//    /**
//     * Returns an array of all locales for which the <code>getInstance</code> methods of
//     * this class can return localized instances.
//     *
//     * <p><strong>Note:</strong> Unlike
//     * <code>java.text.DecimalFormatSymbols#getAvailableLocales</code>, this method simply
//     * returns the array of <code>Locale</code>s available for this class.  ICU currently
//     * does not support <code>DecimalFormatSymbolsProvider</code>, which was introduced in
//     * Java 6.
//     *
//     * @return An array of <code>Locale</code>s for which localized
//     * <code>DecimalFormatSymbols</code> instances are available.
//     * @stable ICU 3.8
//     */
//    public static Locale[] getAvailableLocales() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns an array of all locales for which the <code>getInstance</code>
//     * methods of this class can return localized instances.
//     *
//     * <p><strong>Note:</strong> Unlike
//     * <code>java.text.DecimalFormatSymbols#getAvailableLocales</code>, this method simply
//     * returns the array of <code>ULocale</code>s available in this class.  ICU currently
//     * does not support <code>DecimalFormatSymbolsProvider</code>, which was introduced in
//     * Java 6.
//     *
//     * @return An array of <code>ULocale</code>s for which localized
//     * <code>DecimalFormatSymbols</code> instances are available.
//     * @stable ICU 3.8 (retain)
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static ULocale[] getAvailableULocales() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Return the character used for zero. Different for Arabic, etc.
     * @return the character
     * @stable ICU 2.0
     */
    public char getZeroDigit() {
        return dfs.getZeroDigit();
    }
        
    /**
     * Set the character used for zero.
     * @param zeroDigit the zero character.
     * @stable ICU 2.0
     */
    public void setZeroDigit(char zeroDigit) {
        dfs.setZeroDigit(zeroDigit);
    }

//    /**
//     * Returns the character used to represent a significant digit in a pattern.
//     * @return the significant digit pattern character
//     * @stable ICU 3.0
//     */
//    public char getSignificantDigit() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Sets the character used to represent a significant digit in a pattern.
//     * @param sigDigit the significant digit pattern character
//     * @stable ICU 3.0
//     */
//    public void setSignificantDigit(char sigDigit) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Return the character used for thousands separator. Different for French, etc.
     * @return the thousands character
     * @stable ICU 2.0
     */
    public char getGroupingSeparator() {
        return dfs.getGroupingSeparator();
    }
        
    /**
     * Set the character used for thousands separator. Different for French, etc.
     * @param groupingSeparator the thousands character
     * @stable ICU 2.0
     */
    public void setGroupingSeparator(char groupingSeparator) {
        dfs.setGroupingSeparator(groupingSeparator);
    }
        
    /**
     * Return the character used for decimal sign. Different for French, etc.
     * @return the decimal character
     * @stable ICU 2.0
     */
    public char getDecimalSeparator() {
        return dfs.getDecimalSeparator();
    }
        
    /**
     * Set the character used for decimal sign. Different for French, etc.
     * @param decimalSeparator the decimal character
     * @stable ICU 2.0
     */
    public void setDecimalSeparator(char decimalSeparator) {
        dfs.setDecimalSeparator(decimalSeparator);
    }
        
    /**
     * Return the character used for mille percent sign. Different for Arabic, etc.
     * @return the mille percent character
     * @stable ICU 2.0
     */
    public char getPerMill() {
        return dfs.getPerMill();
    }
        
    /**
     * Set the character used for mille percent sign. Different for Arabic, etc.
     * @param perMill the mille percent character
     * @stable ICU 2.0
     */
    public void setPerMill(char perMill) {
        dfs.setPerMill(perMill);
    }
        
    /**
     * Return the character used for percent sign. Different for Arabic, etc.
     * @return the percent character
     * @stable ICU 2.0
     */
    public char getPercent() {
        return dfs.getPercent();
    }
        
    /**
     * Set the character used for percent sign. Different for Arabic, etc.
     * @param percent the percent character
     * @stable ICU 2.0
     */
    public void setPercent(char percent) {
        dfs.setPercent(percent);
    }
        
    /**
     * Return the character used for a digit in a pattern.
     * @return the digit pattern character
     * @stable ICU 2.0
     */
    public char getDigit() {
        return dfs.getDigit();
    }
        
    /**
     * Returns the array of characters used as digits, in order from 0 through 9
     * @return The array
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public char[] getDigits() {
        char [] digitArray = new char[10];
        for ( int i = 0 ; i < 10 ; i++ ) {
            digitArray[i] = (char) (getZeroDigit() + i);
        }
        return digitArray;
    }
    
    /**
     * Set the character used for a digit in a pattern.
     * @param digit the digit pattern character
     * @stable ICU 2.0
     */
    public void setDigit(char digit) {
        dfs.setDigit(digit);
    }
        
    /**
     * Return the character used to separate positive and negative subpatterns
     * in a pattern.
     * @return the pattern separator character
     * @stable ICU 2.0
     */
    public char getPatternSeparator() {
        return dfs.getPatternSeparator();
    }
        
    /**
     * Set the character used to separate positive and negative subpatterns
     * in a pattern.
     * @param patternSeparator the pattern separator character
     * @stable ICU 2.0
     */
    public void setPatternSeparator(char patternSeparator) {
        dfs.setPatternSeparator(patternSeparator);
    }
        
    /**
     * Return the String used to represent infinity. Almost always left
     * unchanged.
     * @return the Infinity string
     * @stable ICU 2.0
     */
    public String getInfinity() {
        return dfs.getInfinity();
    }
        
    /**
     * Set the String used to represent infinity. Almost always left
     * unchanged.
     * @param infinity the Infinity String
     * @stable ICU 2.0
     */
    public void setInfinity(String infinity) {
        dfs.setInfinity(infinity);
    }
        
    /**
     * Return the String used to represent NaN. Almost always left
     * unchanged.
     * @return the NaN String
     * @stable ICU 2.0
     */
    public String getNaN() {
        return dfs.getNaN();
    }
        
    /**
     * Set the String used to represent NaN. Almost always left
     * unchanged.
     * @param NaN the NaN String
     * @stable ICU 2.0
     */
    public void setNaN(String NaN) {
        dfs.setNaN(NaN);
    }
        
    /**
     * Return the character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     * @return the minus sign character
     * @stable ICU 2.0
     */
    public char getMinusSign() {
        return dfs.getMinusSign();
    }
        
    /**
     * Set the character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     * @param minusSign the minus sign character
     * @stable ICU 2.0
     */
    public void setMinusSign(char minusSign) {
        dfs.setMinusSign(minusSign);
    }
        
    /**
     * Return the string denoting the local currency.
     * @return the local currency String.
     * @stable ICU 2.0
     */
    public String getCurrencySymbol() {
        return dfs.getCurrencySymbol();
    }
        
    /**
     * Set the string denoting the local currency.
     * @param currency the local currency String.
     * @stable ICU 2.0
     */
    public void setCurrencySymbol(String currency) {
        dfs.setCurrencySymbol(currency);
    }

    /**
     * Returns the currency symbol, for JDK 1.4 compatibility only.
     * ICU clients should use the Currency API directly.
     * @return the currency used, or null
     * @stable ICU 3.4
     */
    public Currency getCurrency() {
        return new Currency(dfs.getCurrency());
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
        dfs.setCurrency(java.util.Currency.getInstance(currency.getCurrencyCode()));
    }

    /**
     * Return the international string denoting the local currency.
     * @return the international string denoting the local currency
     * @stable ICU 2.0
     */
    public String getInternationalCurrencySymbol() {
        return dfs.getInternationalCurrencySymbol();
    }
        
    /**
     * Set the international string denoting the local currency.
     * @param currency the international string denoting the local currency.
     * @stable ICU 2.0
     */
    public void setInternationalCurrencySymbol(String currency) {
        dfs.setInternationalCurrencySymbol(currency);
    }
        
    /**
     * Return the monetary decimal separator.
     * @return the monetary decimal separator character
     * @stable ICU 2.0
     */
    public char getMonetaryDecimalSeparator() {
        return dfs.getMonetaryDecimalSeparator();
    }
        
    /**
     * Set the monetary decimal separator.
     * @param sep the monetary decimal separator character
     * @stable ICU 2.0
     */
    public void setMonetaryDecimalSeparator(char sep) {
        dfs.setMonetaryDecimalSeparator(sep);
    }

//    /**
//     * {@icu} Returns the monetary grouping separator.
//     * @return the monetary grouping separator character
//     * @stable ICU 3.6
//     */
//    public char getMonetaryGroupingSeparator() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Sets the monetary decimal separator.
//     * @param sep the monetary decimal separator character
//     * @stable ICU 3.6
//     */
//    public void setMonetaryGroupingSeparator(char sep) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the string used to separate the mantissa from the exponent.
//     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
//     * @return the localized exponent symbol, used in localized patterns
//     * and formatted strings
//     * @see #setExponentSeparator
//     * @stable ICU 2.0
//     */
//    public String getExponentSeparator() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the string used to separate the mantissa from the exponent.
//     * Examples: "x10^" for 1.23x10^4, "E" for 1.23E4.
//     * @param exp the localized exponent symbol, used in localized patterns
//     * and formatted strings
//     * @see #getExponentSeparator
//     * @stable ICU 2.0
//     */
//    public void setExponentSeparator(String exp) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the localized plus sign.
//     * @return the plus sign, used in localized patterns and formatted
//     * strings
//     * @see #setPlusSign
//     * @see #setMinusSign
//     * @see #getMinusSign
//     * @stable ICU 2.0
//     */
//    public char getPlusSign() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the localized plus sign.
//     * @param plus the plus sign, used in localized patterns and formatted
//     * strings
//     * @see #getPlusSign
//     * @see #setMinusSign
//     * @see #getMinusSign
//     * @stable ICU 2.0
//     */
//    public void setPlusSign(char plus) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the character used to pad numbers out to a specified width.  This is
//     * not the pad character itself; rather, it is the special pattern character
//     * <em>preceding</em> the pad character.  In the pattern "*_#,##0", '*' is the pad
//     * escape, and '_' is the pad character.
//     * @return the character
//     * @see #setPadEscape
//     * @see DecimalFormat#getFormatWidth
//     * @see DecimalFormat#getPadPosition
//     * @see DecimalFormat#getPadCharacter
//     * @stable ICU 2.0
//     */
//    public char getPadEscape() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the character used to pad numbers out to a specified width.  This is not
//     * the pad character itself; rather, it is the special pattern character
//     * <em>preceding</em> the pad character.  In the pattern "*_#,##0", '*' is the pad
//     * escape, and '_' is the pad character.
//     * @see #getPadEscape
//     * @see DecimalFormat#setFormatWidth
//     * @see DecimalFormat#setPadPosition
//     * @see DecimalFormat#setPadCharacter
//     * @stable ICU 2.0
//     */
//    public void setPadEscape(char c) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Indicates the currency match pattern used in {@link #getPatternForCurrencySpacing}.
//     * @stable ICU 4.2
//     */
//    public static final int CURRENCY_SPC_CURRENCY_MATCH = 0;
//
//    /**
//     * {@icu} Indicates the surrounding match pattern used in {@link
//     * #getPatternForCurrencySpacing}.
//     * @stable ICU 4.2
//     */
//    public static final int CURRENCY_SPC_SURROUNDING_MATCH = 1;
//
//    /**
//     * {@icu} Indicates the insertion value used in {@link #getPatternForCurrencySpacing}.
//     * @stable ICU 4.4
//     */
//    public static final int CURRENCY_SPC_INSERT = 2;

//    /**
//     * {@icu} Returns the desired currency spacing value. Original values come from ICU's
//     * CLDR data based on the locale provided during construction, and can be null.  These
//     * values govern what and when text is inserted between a currency code/name/symbol
//     * and the currency amount when formatting money.
//     *
//     * <p>For more information, see <a href="http://www.unicode.org/reports/tr35/#Currencies"
//     * >UTS#35 section 5.10.2</a>.
//     *
//     * <p><strong>Note:</strong> ICU4J does not currently use this information.
//     *
//     * @param itemType one of CURRENCY_SPC_CURRENCY_MATCH, CURRENCY_SPC_SURROUNDING_MATCH
//     * or CURRENCY_SPC_INSERT
//     * @param beforeCurrency true to get the <code>beforeCurrency</code> values, false
//     * to get the <code>afterCurrency</code> values.
//     * @return the value, or null.
//     * @see #setPatternForCurrencySpacing(int, boolean, String)
//     * @stable ICU 4.2
//     */
//    public String getPatternForCurrencySpacing(int itemType, boolean beforeCurrency)  {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the indicated currency spacing pattern or value. See {@link
//     * #getPatternForCurrencySpacing} for more information.
//     *
//     * <p>Values for currency match and surrounding match must be {@link
//     * com.ibm.icu.text.UnicodeSet} patterns. Values for insert can be any string.
//     *
//     * <p><strong>Note:</strong> ICU4J does not currently use this information.
//     *
//     * @param itemType one of CURRENCY_SPC_CURRENCY_MATCH, CURRENCY_SPC_SURROUNDING_MATCH
//     * or CURRENCY_SPC_INSERT
//     * @param beforeCurrency true if the pattern is for before the currency symbol.
//     * false if the pattern is for after it.
//     * @param  pattern string to override current setting; can be null.
//     * @see #getPatternForCurrencySpacing(int, boolean)
//     * @stable ICU 4.2
//     */
//    public void setPatternForCurrencySpacing(int itemType, boolean beforeCurrency, String pattern) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }


//    /**
//     * Returns the locale for which this object was constructed.
//     * @return the locale for which this object was constructed
//     * @stable ICU 2.0
//     */
//    public Locale getLocale() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the locale for which this object was constructed.
//     * @return the locale for which this object was constructed
//     * @stable ICU 3.2
//     */
//    public ULocale getULocale() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the locale that was used to create this object, or null.
//     * This may may differ from the locale requested at the time of
//     * this object's creation.  For example, if an object is created
//     * for locale <tt>en_US_CALIFORNIA</tt>, the actual data may be
//     * drawn from <tt>en</tt> (the <i>actual</i> locale), and
//     * <tt>en_US</tt> may be the most specific locale that exists (the
//     * <i>valid</i> locale).
//     *
//     * <p>Note: The <i>actual</i> locale is returned correctly, but the <i>valid</i>
//     * locale is not, in most cases.
//     * @param type type of information requested, either {@link
//     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
//     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
//     * @return the information specified by <i>type</i>, or null if
//     * this object was not constructed from locale data.
//     * @see com.ibm.icu.util.ULocale
//     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
//     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
//     * @draft ICU 2.8 (retain)
//     * @provisional This API might change or be removed in a future release.
//     */
//    public final ULocale getLocale(ULocale.Type type) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Standard override.
     * @stable ICU 2.0
     */
    public Object clone() {
        return new DecimalFormatSymbols((java.text.DecimalFormatSymbols)dfs.clone());
    }
        
    /**
     * Override equals.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        try {
            return dfs.equals(((DecimalFormatSymbols)obj).dfs);
        }
        catch (Exception e) {
            return false;
        }
    }
        
    /**
     * Override hashCode
     * @stable ICU 2.0
     */
    public int hashCode() {
        return dfs.hashCode();
    }
}
