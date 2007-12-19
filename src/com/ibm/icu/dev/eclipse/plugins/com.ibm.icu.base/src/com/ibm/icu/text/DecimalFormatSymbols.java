/*
 *******************************************************************************
 * Copyright (C) 1996-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;


import java.io.Serializable;
import java.util.Locale;

import com.ibm.icu.util.ULocale;

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
        this(new java.text.DecimalFormatSymbols());
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
     * @draft ICU 3.2
     */
    public DecimalFormatSymbols(ULocale locale) {
        this(new java.text.DecimalFormatSymbols(locale.toLocale()));
    }
        
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
