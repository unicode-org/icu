/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */


package com.ibm.icu.text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
 * <code>NumberFormat</code> is the abstract base class for all number
 * formats. This class provides the interface for formatting and parsing
 * numbers. <code>NumberFormat</code> also provides methods for determining
 * which locales have number formats, and what their names are.
 *
 * <p><strong>This is an enhanced version of <code>NumberFormat</code> that
 * is based on the standard version in the JDK.  New or changed functionality
 * is labeled
 * <strong><font face=helvetica color=red>NEW</font></strong> or
 * <strong><font face=helvetica color=red>CHANGED</font></strong>.</strong>
 *
 * <p>
 * <code>NumberFormat</code> helps you to format and parse numbers for any locale.
 * Your code can be completely independent of the locale conventions for
 * decimal points, thousands-separators, or even the particular decimal
 * digits used, or whether the number format is even decimal.
 *
 * <p>
 * To format a number for the current Locale, use one of the factory
 * class methods:
 * <blockquote>
 * <pre>
 *  myString = NumberFormat.getInstance().format(myNumber);
 * </pre>
 * </blockquote>
 * If you are formatting multiple numbers, it is
 * more efficient to get the format and use it multiple times so that
 * the system doesn't have to fetch the information about the local
 * language and country conventions multiple times.
 * <blockquote>
 * <pre>
 * NumberFormat nf = NumberFormat.getInstance();
 * for (int i = 0; i < a.length; ++i) {
 *     output.println(nf.format(myNumber[i]) + "; ");
 * }
 * </pre>
 * </blockquote>
 * To format a number for a different Locale, specify it in the
 * call to <code>getInstance</code>.
 * <blockquote>
 * <pre>
 * NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
 * </pre>
 * </blockquote>
 * You can also use a <code>NumberFormat</code> to parse numbers:
 * <blockquote>
 * <pre>
 * myNumber = nf.parse(myString);
 * </pre>
 * </blockquote>
 * Use <code>getInstance</code> or <code>getNumberInstance</code> to get the
 * normal number format. Use <code>getIntegerInstance</code> to get an
 * integer number format. Use <code>getCurrencyInstance</code> to get the
 * currency number format. And use <code>getPercentInstance</code> to get a
 * format for displaying percentages. With this format, a fraction like
 * 0.53 is displayed as 53%.
 *
 * <p>
 * You can also control the display of numbers with such methods as
 * <code>setMinimumFractionDigits</code>.
 * If you want even more control over the format or parsing,
 * or want to give your users more control,
 * you can try casting the <code>NumberFormat</code> you get from the factory methods
 * to a <code>DecimalFormat</code>. This will work for the vast majority
 * of locales; just remember to put it in a <code>try</code> block in case you
 * encounter an unusual one.
 *
 * <p>
 * NumberFormat is designed such that some controls
 * work for formatting and others work for parsing.  The following is
 * the detailed description for each these control methods,
 * <p>
 * setParseIntegerOnly : only affects parsing, e.g.
 * if true,  "3456.78" -> 3456 (and leaves the parse position just after '6')
 * if false, "3456.78" -> 3456.78 (and leaves the parse position just after '8')
 * This is independent of formatting.  If you want to not show a decimal point
 * where there might be no digits after the decimal point, use
 * setDecimalSeparatorAlwaysShown on DecimalFormat.
 * <p>
 * You can also use forms of the <code>parse</code> and <code>format</code>
 * methods with <code>ParsePosition</code> and <code>FieldPosition</code> to
 * allow you to:
 * <ul>
 * <li> progressively parse through pieces of a string
 * <li> align the decimal point and other areas
 * </ul>
 * For example, you can align numbers in two ways:
 * <ol>
 * <li> If you are using a monospaced font with spacing for alignment,
 *      you can pass the <code>FieldPosition</code> in your format call, with
 *      <code>field</code> = <code>INTEGER_FIELD</code>. On output,
 *      <code>getEndIndex</code> will be set to the offset between the
 *      last character of the integer and the decimal. Add
 *      (desiredSpaceCount - getEndIndex) spaces at the front of the string.
 *
 * <li> If you are using proportional fonts,
 *      instead of padding with spaces, measure the width
 *      of the string in pixels from the start to <code>getEndIndex</code>.
 *      Then move the pen by
 *      (desiredPixelWidth - widthToAlignmentPoint) before drawing the text.
 *      It also works where there is no decimal, but possibly additional
 *      characters at the end, e.g., with parentheses in negative
 *      numbers: "(12)" for -12.
 * </ol>
 *
 * <h4>Synchronization</h4>
 * <p>
 * Number formats are generally not synchronized. It is recommended to create 
 * separate format instances for each thread. If multiple threads access a format
 * concurrently, it must be synchronized externally. 
 * <p>
 *
 * <h4>DecimalFormat</h4>
 * <p>DecimalFormat is the concrete implementation of NumberFormat, and the
 * NumberFormat API is essentially an abstraction from DecimalFormat's API.
 * Refer to DecimalFormat for more information about this API.</p>
 *
 * see          DecimalFormat
 * see          java.text.ChoiceFormat
 * @author       Mark Davis
 * @author       Helena Shih
 * @author       Alan Liu
 * @stable ICU 2.0
 */
public class NumberFormat extends Format {
    private static final long serialVersionUID = 1;

    /**
     * @internal
     */
    public final java.text.NumberFormat numberFormat;
        
    /**
     * @internal
     * @param delegate the NumberFormat to which to delegate
     */
    public NumberFormat(java.text.NumberFormat delegate) {
        this.numberFormat = delegate;
    }
    
    /**
     * Default constructor to mirror Java's default public
     * constructor.  Java's is not callable as a public API, since
     * their NumberFormat is abstract, so this is only useful to
     * subclasses.  In general, subclasses will not work unless
     * they manipulate the delegate.
     */
    public NumberFormat() {
        this.numberFormat = java.text.NumberFormat.getInstance();
    }

    /**
     * Field constant used to construct a FieldPosition object. Signifies that
     * the position of the integer part of a formatted number should be returned.
     * @see java.text.FieldPosition
     * @stable ICU 2.0
     */
    public static final int INTEGER_FIELD = 0;
        
    /**
     * Field constant used to construct a FieldPosition object. Signifies that
     * the position of the fraction part of a formatted number should be returned.
     * @see java.text.FieldPosition
     * @stable ICU 2.0
     */
    public static final int FRACTION_FIELD = 1;
        
    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong>
     * Format an object.  Change: recognizes <code>BigInteger</code>
     * and <code>BigDecimal</code> objects.
     * @stable ICU 2.0
     */
    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        return numberFormat.format(number, toAppendTo, pos);
    }
        
    /**
     * @stable ICU 2.0
     */
    public final Object parseObject(String source, ParsePosition parsePosition) {
        return numberFormat.parse(source, parsePosition);
    }
        
    /**
     * Specialization of format.
     * @see java.text.Format#format(Object)
     * @stable ICU 2.0
     */
    public final String format(double number) {
        return numberFormat.format(number);
    }
        
    /**
     * Specialization of format.
     * @see java.text.Format#format(Object)
     * @stable ICU 2.0
     */
    public final String format(long number) {
        return numberFormat.format(number);
    }
        
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Convenience method to format a BigInteger.
     * @stable ICU 2.0
     */
    public final String format(BigInteger number) {
        return numberFormat.format(number);
    }
        
    /**
     * Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        return numberFormat.format(number, toAppendTo, pos);
    }
        
    /**
     * Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return numberFormat.format(number, toAppendTo, pos);
    }
        
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigInteger.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
        return numberFormat.format(number, toAppendTo, pos);
    }
    /**
     * Returns a Long if possible (e.g., within the range [Long.MIN_VALUE,
     * Long.MAX_VALUE] and with no decimals), otherwise a Double.
     * If IntegerOnly is set, will stop at a decimal
     * point (or equivalent; e.g., for rational numbers "1 2/3", will stop
     * after the 1).
     * Does not throw an exception; if no object can be parsed, index is
     * unchanged!
     * @see #isParseIntegerOnly
     * @see java.text.Format#parseObject(String, ParsePosition)
     * @stable ICU 2.0
     */
    public Number parse(String text, ParsePosition parsePosition) {
        return numberFormat.parse(text, parsePosition);
    }
        
    /**
     * Parses text from the beginning of the given string to produce a number.
     * The method might not use the entire text of the given string.
     *
     * @param text A String whose beginning should be parsed.
     * @return A Number parsed from the string.
     * @exception ParseException if the beginning of the specified string 
     * cannot be parsed.
     * @see #format
     * @stable ICU 2.0
     */
    public Number parse(String text) throws ParseException {
        return numberFormat.parse(text);
    }
    /**
     * Returns true if this format will parse numbers as integers only.
     * For example in the English locale, with ParseIntegerOnly true, the
     * string "1234." would be parsed as the integer value 1234 and parsing
     * would stop at the "." character.  The decimal separator accepted
     * by the parse operation is locale-dependent and determined by the
     * subclass.
     * @return true if this will parse integers only
     * @stable ICU 2.0
     */
    public boolean isParseIntegerOnly() {
        return numberFormat.isParseIntegerOnly();
    }
        
    /**
     * Sets whether or not numbers should be parsed as integers only.
     * @param value true if this should parse integers only
     * @see #isParseIntegerOnly
     * @stable ICU 2.0
     */
    public void setParseIntegerOnly(boolean value) {
        numberFormat.setParseIntegerOnly(value);
    }
        
    //============== Locale Stuff =====================
        
    /**
     * Returns the default number format for the current default locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getIntegerInstance,
     * getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale-dependent.
     * @stable ICU 2.0
     */
    public final static NumberFormat getInstance() {
        return getInstance(ULocale.getDefault(), NUMBERSTYLE);
    }
        
    /**
     * Returns the default number format for the specified locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale-dependent.
     * @stable ICU 2.0
     */
    public static NumberFormat getInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), NUMBERSTYLE);
    }
        
    /**
     * Returns the default number format for the specified locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale-dependent.
     * @stable ICU 3.2
     */
    public static NumberFormat getInstance(ULocale inLocale) {
        return getInstance(inLocale, NUMBERSTYLE);
    }
        
    /**
     * Returns a general-purpose number format for the current default locale.
     * @stable ICU 2.0
     */
    public final static NumberFormat getNumberInstance() {
        return getInstance(ULocale.getDefault(), NUMBERSTYLE);
    }
        
    /**
     * Returns a general-purpose number format for the specified locale.
     * @stable ICU 2.0
     */
    public static NumberFormat getNumberInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), NUMBERSTYLE);
    }
        
    /**
     * Returns a general-purpose number format for the specified locale.
     * @stable ICU 3.2
     */
    public static NumberFormat getNumberInstance(ULocale inLocale) {
        return getInstance(inLocale, NUMBERSTYLE);
    }
        
    /**
     * Returns an integer number format for the current default locale. The
     * returned number format is configured to round floating point numbers
     * to the nearest integer using IEEE half-even rounding (see {@link 
     * com.ibm.icu.math.BigDecimal#ROUND_HALF_EVEN ROUND_HALF_EVEN}) for formatting,
     * and to parse only the integer part of an input string (see {@link
     * #isParseIntegerOnly isParseIntegerOnly}).
     *
     * @return a number format for integer values
     * @stable ICU 2.0
     */
    public final static NumberFormat getIntegerInstance() {
        return getInstance(ULocale.getDefault(), INTEGERSTYLE);
    }
        
    /**
     * Returns an integer number format for the specified locale. The
     * returned number format is configured to round floating point numbers
     * to the nearest integer using IEEE half-even rounding (see {@link 
     * com.ibm.icu.math.BigDecimal#ROUND_HALF_EVEN ROUND_HALF_EVEN}) for formatting,
     * and to parse only the integer part of an input string (see {@link
     * #isParseIntegerOnly isParseIntegerOnly}).
     *
     * @param inLocale the locale for which a number format is needed
     * @return a number format for integer values
     * @stable ICU 2.0
     */
    public static NumberFormat getIntegerInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), INTEGERSTYLE);
    }
        
    /**
     * Returns an integer number format for the specified locale. The
     * returned number format is configured to round floating point numbers
     * to the nearest integer using IEEE half-even rounding (see {@link 
     * com.ibm.icu.math.BigDecimal#ROUND_HALF_EVEN ROUND_HALF_EVEN}) for formatting,
     * and to parse only the integer part of an input string (see {@link
     * #isParseIntegerOnly isParseIntegerOnly}).
     *
     * @param inLocale the locale for which a number format is needed
     * @return a number format for integer values
     * @stable ICU 3.2
     */
    public static NumberFormat getIntegerInstance(ULocale inLocale) {
        return getInstance(inLocale, INTEGERSTYLE);
    }
        
    /**
     * Returns a currency format for the current default locale.
     * @return a number format for currency
     * @stable ICU 2.0
     */
    public final static NumberFormat getCurrencyInstance() {
        return getInstance(ULocale.getDefault(), CURRENCYSTYLE);
    }
        
    /**
     * Returns a currency format for the specified locale.
     * @return a number format for currency
     * @stable ICU 2.0
     */
    public static NumberFormat getCurrencyInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), CURRENCYSTYLE);
    }
        
    /**
     * Returns a currency format for the specified locale.
     * @return a number format for currency
     * @stable ICU 3.2
     */
    public static NumberFormat getCurrencyInstance(ULocale inLocale) {
        return getInstance(inLocale, CURRENCYSTYLE);
    }
        
    /**
     * Returns a percentage format for the current default locale.
     * @return a number format for percents
     * @stable ICU 2.0
     */
    public final static NumberFormat getPercentInstance() {
        return getInstance(ULocale.getDefault(), PERCENTSTYLE);
    }
        
    /**
     * Returns a percentage format for the specified locale.
     * @return a number format for percents
     * @stable ICU 2.0
     */
    public static NumberFormat getPercentInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), PERCENTSTYLE);
    }
        
    /**
     * Returns a percentage format for the specified locale.
     * @return a number format for percents
     * @stable ICU 3.2
     */
    public static NumberFormat getPercentInstance(ULocale inLocale) {
        return getInstance(inLocale, PERCENTSTYLE);
    }
        
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Returns a scientific format for the current default locale.
     * @return a scientific number format
     * @stable ICU 2.0
     */
    public final static NumberFormat getScientificInstance() {
        return getInstance(ULocale.getDefault(), SCIENTIFICSTYLE);
    }
        
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Returns a scientific format for the specified locale.
     * @return a scientific number format
     * @stable ICU 2.0
     */
    public static NumberFormat getScientificInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), SCIENTIFICSTYLE);
    }
        
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Returns a scientific format for the specified locale.
     * @return a scientific number format
     * @stable ICU 3.2
     */
    public static NumberFormat getScientificInstance(ULocale inLocale) {
        return getInstance(inLocale, SCIENTIFICSTYLE);
    }
        
    /**
     * Get the list of Locales for which NumberFormats are available.
     * @return the available locales
     * @stable ICU 2.0
     */
    public static Locale[] getAvailableLocales() {
        return java.text.NumberFormat.getAvailableLocales();
    }
        
    /**
     * Get the list of Locales for which NumberFormats are available.
     * @return the available locales
     * @draft ICU 3.2 (retain)
     */
    public static ULocale[] getAvailableULocales() {
        if (availableULocales == null) {
            Locale[] locales = java.text.NumberFormat.getAvailableLocales();
            ULocale[] ulocales = new ULocale[locales.length];
            for (int i = 0; i < locales.length; ++i) {
                ulocales[i] = ULocale.forLocale(locales[i]);
            }
            availableULocales = ulocales;
        }
        return (ULocale[])availableULocales.clone();
    }
    private static ULocale[] availableULocales;
        
        
    /**
     * Returns true if grouping is used in this format. For example, in the
     * en_US locale, with grouping on, the number 1234567 will be formatted
     * as "1,234,567". The grouping separator as well as the size of each group
     * is locale-dependent and is determined by subclasses of NumberFormat.
     * Grouping affects both parsing and formatting.
     * @return true if grouping is used
     * @see #setGroupingUsed
     * @stable ICU 2.0
     */
    public boolean isGroupingUsed() {
        return numberFormat.isGroupingUsed();
    }
        
    /**
     * Sets whether or not grouping will be used in this format.  Grouping
     * affects both parsing and formatting.
     * @see #isGroupingUsed
     * @param newValue true to use grouping.
     * @stable ICU 2.0
     */
    public void setGroupingUsed(boolean newValue) {
        numberFormat.setGroupingUsed(newValue);
    }
        
    /**
     * Returns the maximum number of digits allowed in the integer portion of a
     * number.  The default value is 40, which subclasses can override.
     * When formatting, the exact behavior when this value is exceeded is
     * subclass-specific.  When parsing, this has no effect.
     * @return the maximum number of integer digits
     * @see #setMaximumIntegerDigits
     * @stable ICU 2.0
     */
    public int getMaximumIntegerDigits() {
        return numberFormat.getMaximumIntegerDigits();
    }
        
    /**
     * Sets the maximum number of digits allowed in the integer portion of a
     * number. This must be >= minimumIntegerDigits.  If the
     * new value for maximumIntegerDigits is less than the current value
     * of minimumIntegerDigits, then minimumIntegerDigits will also be set to
     * the new value.
     * @param newValue the maximum number of integer digits to be shown; if
     * less than zero, then zero is used.  Subclasses might enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMaximumIntegerDigits
     * @stable ICU 2.0
     */
    public void setMaximumIntegerDigits(int newValue) {
        numberFormat.setMaximumIntegerDigits(newValue);
    }
        
    /**
     * Returns the minimum number of digits allowed in the integer portion of a
     * number.  The default value is 1, which subclasses can override.
     * When formatting, if this value is not reached, numbers are padded on the
     * left with the locale-specific '0' character to ensure at least this
     * number of integer digits.  When parsing, this has no effect.
     * @return the minimum number of integer digits
     * @see #setMinimumIntegerDigits
     * @stable ICU 2.0
     */
    public int getMinimumIntegerDigits() {
        return numberFormat.getMinimumIntegerDigits();
    }
        
    /**
     * Sets the minimum number of digits allowed in the integer portion of a
     * number.  This must be <= maximumIntegerDigits.  If the
     * new value for minimumIntegerDigits is more than the current value
     * of maximumIntegerDigits, then maximumIntegerDigits will also be set to
     * the new value.
     * @param newValue the minimum number of integer digits to be shown; if
     * less than zero, then zero is used. Subclasses might enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMinimumIntegerDigits
     * @stable ICU 2.0
     */
    public void setMinimumIntegerDigits(int newValue) {
        numberFormat.setMinimumIntegerDigits(newValue);
    }
        
    /**
     * Returns the maximum number of digits allowed in the fraction
     * portion of a number.  The default value is 3, which subclasses
     * can override.  When formatting, the exact behavior when this
     * value is exceeded is subclass-specific.  When parsing, this has 
     * no effect.
     * @return the maximum number of fraction digits
     * @see #setMaximumFractionDigits
     * @stable ICU 2.0 
     */
    public int getMaximumFractionDigits() {
        return numberFormat.getMaximumFractionDigits();
    }
        
    /**
     * Sets the maximum number of digits allowed in the fraction portion of a
     * number. This must be >= minimumFractionDigits.  If the
     * new value for maximumFractionDigits is less than the current value
     * of minimumFractionDigits, then minimumFractionDigits will also be set to
     * the new value.
     * @param newValue the maximum number of fraction digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMaximumFractionDigits
     * @stable ICU 2.0
     */
    public void setMaximumFractionDigits(int newValue) {
        numberFormat.setMaximumFractionDigits(newValue);
    }
        
    /**
     * Returns the minimum number of digits allowed in the fraction portion of a
     * number.  The default value is 0, which subclasses can override.
     * When formatting, if this value is not reached, numbers are padded on
     * the right with the locale-specific '0' character to ensure at least
     * this number of fraction digits.  When parsing, this has no effect.
     * @return the minimum number of fraction digits
     * @see #setMinimumFractionDigits
     * @stable ICU 2.0
     */
    public int getMinimumFractionDigits() {
        return numberFormat.getMinimumFractionDigits();
    }
        
    /**
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number.  This must be <= maximumFractionDigits.  If the
     * new value for minimumFractionDigits exceeds the current value
     * of maximumFractionDigits, then maximumFractionDigits will also be set to
     * the new value.
     * @param newValue the minimum number of fraction digits to be shown; if
     * less than zero, then zero is used.  Subclasses might enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMinimumFractionDigits
     * @stable ICU 2.0
     */
    public void setMinimumFractionDigits(int newValue) {
        numberFormat.setMinimumFractionDigits(newValue);
    }

    /**
     * Return a string suitable for debugging.
     * @return a string suitable for debugging
     * @stable ICU 3.4.2
     */
    public String toString() {
        return numberFormat.toString();
    }
        
    /**
     * Overrides Cloneable.
     * @stable ICU 2.0
     */
    public Object clone() {
        return new NumberFormat((java.text.NumberFormat)numberFormat.clone());
    }
        
    /**
     * Overrides equals.  Two NumberFormats are equal if they are of the same class
     * and the settings (groupingUsed, parseIntegerOnly, maximumIntegerDigits, etc.
     * are equal.
     * @param obj the object to compare against
     * @return true if the object is equal to this.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        try {
            return numberFormat.equals(((NumberFormat)obj).numberFormat);
        }
        catch (Exception e) {
            return false;
        }
    }
        
    /**
     * Overrides hashCode
     * @stable ICU 2.0
     */
    public int hashCode() {
        return numberFormat.hashCode();
    }

    // =======================privates===============================
        
    private static NumberFormat getInstance(ULocale desiredLocale, int choice) {
        Locale locale = desiredLocale.toLocale();
        java.text.NumberFormat nf = null;
        switch (choice) {
        case NUMBERSTYLE: nf = java.text.NumberFormat.getInstance(locale); break;
        case CURRENCYSTYLE: nf = java.text.NumberFormat.getCurrencyInstance(locale); break;
        case PERCENTSTYLE: nf = java.text.NumberFormat.getPercentInstance(locale); break;
        case SCIENTIFICSTYLE: nf = new java.text.DecimalFormat("#E0", new DecimalFormatSymbols(locale)); 
            nf.setMaximumFractionDigits(10);
            break;
        case INTEGERSTYLE: 
            if (unchecked) {
                unchecked = false;
                try {
                    Class[] args = { java.util.Locale.class };
                    integer14API = java.text.NumberFormat.class.getMethod("getIntegerInstance", args);
                }
                catch (Exception e) {
                }
            }
            if (integer14API != null) {
                try {
                    Object[] args = { locale };
                    nf = (java.text.NumberFormat)integer14API.invoke(null, args);
                }
                catch (IllegalAccessException e) {
                    integer14API = null;
                }
                catch (InvocationTargetException e) {
                    integer14API = null;
                }
                catch (Exception e) {
                    // shouldn't happen, but locale might be null, for example
                    // and we don't want to throw away our method because someone
                    // called us with a bad parameter
                }
            }
            if (nf == null) {
                nf = java.text.NumberFormat.getNumberInstance(locale); 
                nf.setMaximumFractionDigits(0);
                nf.setParseIntegerOnly(true);
            }
            break;
        }
        return new NumberFormat(nf);
    }
    
    private static boolean unchecked = true;
    private static Method integer14API;
    
    private static final int NUMBERSTYLE = 0;
    private static final int CURRENCYSTYLE = 1;
    private static final int PERCENTSTYLE = 2;
    private static final int SCIENTIFICSTYLE = 3;
    private static final int INTEGERSTYLE = 4;
}
