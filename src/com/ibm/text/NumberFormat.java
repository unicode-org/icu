/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/NumberFormat.java,v $ 
 * $Date: 2000/06/01 01:21:34 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import java.util.Locale;
import java.util.ResourceBundle;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.resources.*;
import java.util.Hashtable;
import java.math.BigInteger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
 * normal number format. Use <code>getCurrencyInstance</code> to get the
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
 * to a <code>DecimalNumberFormat</code>. This will work for the vast majority
 * of locales; just remember to put it in a <code>try</code> block in case you
 * encounter an unusual one.
 *
 * <p>
 * NumberFormat and DecimalFormat are designed such that some controls
 * work for formatting and others work for parsing.  The following is
 * the detailed description for each these control methods,
 * <p>
 * setParseIntegerOnly : only affects parsing, e.g.
 * if true,  "3456.78" -> 3456 (and leaves the parse position just after index 6)
 * if false, "3456.78" -> 3456.78 (and leaves the parse position just after index 8)
 * This is independent of formatting.  If you want to not show a decimal point
 * where there might be no digits after the decimal point, use
 * setDecimalSeparatorAlwaysShown.
 * <p>
 * setDecimalSeparatorAlwaysShown : only affects formatting, and only where
 * there might be no digits after the decimal point, such as with a pattern
 * like "#,##0.##", e.g.,
 * if true,  3456.00 -> "3,456."
 * if false, 3456.00 -> "3456"
 * This is independent of parsing.  If you want parsing to stop at the decimal
 * point, use setParseIntegerOnly.
 *
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
 * see          DecimalFormat
 * see          java.text.ChoiceFormat
 * @version      $Revision: 1.8 $
 * @author       Mark Davis
 * @author       Helena Shih
 * @author       Alan Liu
 */
public abstract class NumberFormat extends Format{

    /**
     * Field constant used to construct a FieldPosition object. Signifies that
     * the position of the integer part of a formatted number should be returned.
     * @see java.text.FieldPosition
     */
    public static final int INTEGER_FIELD = 0;

    /**
     * Field constant used to construct a FieldPosition object. Signifies that
     * the position of the fraction part of a formatted number should be returned.
     * @see java.text.FieldPosition
     */
    public static final int FRACTION_FIELD = 1;

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong>
     * Format an object.  Change: recognizes <code>BigInteger</code>
     * and <code>BigDecimal</code> objects now.
     */
    public final StringBuffer format(Object number,
                                     StringBuffer toAppendTo,
                                     FieldPosition pos)
    {
        if (number instanceof Long) {
            return format(((Long)number).longValue(), toAppendTo, pos);
        } else if (number instanceof BigInteger) {
            return format((BigInteger) number, toAppendTo, pos);
        } else if (number instanceof java.math.BigDecimal) {
            return format((java.math.BigDecimal) number, toAppendTo, pos);
        } else if (number instanceof com.ibm.math.BigDecimal) {
            return format((com.ibm.math.BigDecimal) number, toAppendTo, pos);
        } else if (number instanceof Number) {
            return format(((Number)number).doubleValue(), toAppendTo, pos);
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
    }

    public final Object parseObject(String source,
                                    ParsePosition parsePosition)
    {
        return parse(source, parsePosition);
    }

   /**
     * Specialization of format.
     * @see java.text.Format#format
     */
    public final String format (double number) {
        return format(number,new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

   /**
     * Specialization of format.
     * @see java.text.Format#format
     */
    public final String format (long number) {
        return format(number,new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Convenience method to format a BigInteger.
     */
    public final String format(BigInteger number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Convenience method to format a BigDecimal.
     */
    public final String format(java.math.BigDecimal number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /* this has been removed pending addition of com.ibm.math packge to ICU4J
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Convenience method to format a BigDecimal.
     */
    public final String format(com.ibm.math.BigDecimal number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

   /**
     * Specialization of format.
     * @see java.text.Format#format
     */
    public abstract StringBuffer format(double number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

   /**
     * Specialization of format.
     * @see java.text.Format#format
     */
    public abstract StringBuffer format(long number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigInteger.
     */
    public abstract StringBuffer format(BigInteger number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos); 

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigDecimal.
     */
    public abstract StringBuffer format(java.math.BigDecimal number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigDecimal.
     */
    public abstract StringBuffer format(com.ibm.math.BigDecimal number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);
   
   /**
     * Returns a Long if possible (e.g., within the range [Long.MIN_VALUE,
     * Long.MAX_VALUE] and with no decimals), otherwise a Double.
     * If IntegerOnly is set, will stop at a decimal
     * point (or equivalent; e.g., for rational numbers "1 2/3", will stop
     * after the 1).
     * Does not throw an exception; if no object can be parsed, index is
     * unchanged!
     * @see #isParseIntegerOnly
     * @see java.text.Format#parseObject
     */
    public abstract Number parse(String text, ParsePosition parsePosition);

    /**
     * Convenience method.
     *
     * @exception ParseException if the specified string is invalid.
     * @see #format
     */
    public Number parse(String text) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Number result = parse(text, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("Unparseable number: \"" + text + "\"",
                                     //PP:parsePosition.errorIndex);
                                     0);
        }
        return result;
    }

    /**
     * Returns true if this format will parse numbers as integers only.
     * For example in the English locale, with ParseIntegerOnly true, the
     * string "1234." would be parsed as the integer value 1234 and parsing
     * would stop at the "." character.  Of course, the exact format accepted
     * by the parse operation is locale dependant and determined by sub-classes
     * of NumberFormat.
     */
    public boolean isParseIntegerOnly() {
        return parseIntegerOnly;
    }

    /**
     * Sets whether or not numbers should be parsed as integers only.
     * @see #isParseIntegerOnly
     */
    public void setParseIntegerOnly(boolean value) {
        parseIntegerOnly = value;
    }

    //============== Locale Stuff =====================

    /**
     * Returns the default number format for the current default locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale dependant.
     */
    public final static NumberFormat getInstance() {
        return getInstance(Locale.getDefault(), NUMBERSTYLE);
    }

    /**
     * Returns the default number format for the specified locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale dependant.
     */
    public static NumberFormat getInstance(Locale inLocale) {
        return getInstance(inLocale, NUMBERSTYLE);
    }

    /**
     * Returns a general-purpose number format for the current default locale.
     */
    public final static NumberFormat getNumberInstance() {
        return getInstance(Locale.getDefault(), NUMBERSTYLE);
    }

    /**
     * Returns a general-purpose number format for the specified locale.
     */
    public static NumberFormat getNumberInstance(Locale inLocale) {
        return getInstance(inLocale, NUMBERSTYLE);
    }

    /**
     * Returns a currency format for the current default locale.
     */
    public final static NumberFormat getCurrencyInstance() {
        return getInstance(Locale.getDefault(), CURRENCYSTYLE);
    }

    /**
     * Returns a currency format for the specified locale.
     */
    public static NumberFormat getCurrencyInstance(Locale inLocale) {
        return getInstance(inLocale, CURRENCYSTYLE);
    }

    /**
     * Returns a percentage format for the current default locale.
     */
    public final static NumberFormat getPercentInstance() {
        return getInstance(Locale.getDefault(), PERCENTSTYLE);
    }

    /**
     * Returns a percentage format for the specified locale.
     */
    public static NumberFormat getPercentInstance(Locale inLocale) {
        return getInstance(inLocale, PERCENTSTYLE);
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Returns a scientific format for the current default locale.
     */
    public final static NumberFormat getScientificInstance() {
        return getInstance(Locale.getDefault(), SCIENTIFICSTYLE);
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Returns a scientific format for the specified locale.
     */
    public static NumberFormat getScientificInstance(Locale inLocale) {
        return getInstance(inLocale, SCIENTIFICSTYLE);
    }


    /**
     * Get the set of Locales for which NumberFormats are installed
     * @return available locales
     */
    public static Locale[] getAvailableLocales() {
        return LocaleData.getAvailableLocales("NumberPatterns");
    }

    /**
     * Overrides hashCode
     */
    public int hashCode() {
        return maximumIntegerDigits * 37 + maxFractionDigits;
        // just enough fields for a reasonable distribution
    }

    /**
     * Overrides equals
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        NumberFormat other = (NumberFormat) obj;
        return (maximumIntegerDigits == other.maximumIntegerDigits
            && minimumIntegerDigits == other.minimumIntegerDigits
            && maximumFractionDigits == other.maximumFractionDigits
            && minimumFractionDigits == other.minimumFractionDigits
            && groupingUsed == other.groupingUsed
            && parseIntegerOnly == other.parseIntegerOnly);
    }

    /**
     * Overrides Cloneable
     */
    public Object clone()
    {
        NumberFormat other = (NumberFormat) super.clone();
        return other;
    }

    /**
     * Returns true if grouping is used in this format. For example, in the
     * English locale, with grouping on, the number 1234567 might be formatted
     * as "1,234,567". The grouping separator as well as the size of each group
     * is locale dependant and is determined by sub-classes of NumberFormat.
     * @see #setGroupingUsed
     */
    public boolean isGroupingUsed() {
        return groupingUsed;
    }

    /**
     * Set whether or not grouping will be used in this format.
     * @see #isGroupingUsed
     */
    public void setGroupingUsed(boolean newValue) {
        groupingUsed = newValue;
    }

    /**
     * Returns the maximum number of digits allowed in the integer portion of a
     * number.
     * @see #setMaximumIntegerDigits
     */
    public int getMaximumIntegerDigits() {
        return maximumIntegerDigits;
    }

    /**
     * Sets the maximum number of digits allowed in the integer portion of a
     * number. maximumIntegerDigits must be >= minimumIntegerDigits.  If the
     * new value for maximumIntegerDigits is less than the current value
     * of minimumIntegerDigits, then minimumIntegerDigits will also be set to
     * the new value.
     * @param newValue the maximum number of integer digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMaximumIntegerDigits
     */
    public void setMaximumIntegerDigits(int newValue) {
        maximumIntegerDigits = Math.max(0,newValue);
        if (minimumIntegerDigits > maximumIntegerDigits)
            minimumIntegerDigits = maximumIntegerDigits;
    }

    /**
     * Returns the minimum number of digits allowed in the integer portion of a
     * number.
     * @see #setMinimumIntegerDigits
     */
    public int getMinimumIntegerDigits() {
        return minimumIntegerDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the integer portion of a
     * number. minimumIntegerDigits must be <= maximumIntegerDigits.  If the
     * new value for minimumIntegerDigits exceeds the current value
     * of maximumIntegerDigits, then maximumIntegerDigits will also be set to
     * the new value
     * @param newValue the minimum number of integer digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMinimumIntegerDigits
     */
    public void setMinimumIntegerDigits(int newValue) {
        minimumIntegerDigits = Math.max(0,newValue);
        if (minimumIntegerDigits > maximumIntegerDigits)
            maximumIntegerDigits = minimumIntegerDigits;
    }

    /**
     * Returns the maximum number of digits allowed in the fraction portion of a
     * number.
     * @see #setMaximumFractionDigits
     */
    public int getMaximumFractionDigits() {
        return maximumFractionDigits;
    }

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a
     * number. maximumFractionDigits must be >= minimumFractionDigits.  If the
     * new value for maximumFractionDigits is less than the current value
     * of minimumFractionDigits, then minimumFractionDigits will also be set to
     * the new value.
     * @param newValue the maximum number of fraction digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMaximumFractionDigits
     */
    public void setMaximumFractionDigits(int newValue) {
        maximumFractionDigits = Math.max(0,newValue);
        if (maximumFractionDigits < minimumFractionDigits)
            minimumFractionDigits = maximumFractionDigits;
    }

    /**
     * Returns the minimum number of digits allowed in the fraction portion of a
     * number.
     * @see #setMinimumFractionDigits
     */
    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number. minimumFractionDigits must be <= maximumFractionDigits.  If the
     * new value for minimumFractionDigits exceeds the current value
     * of maximumFractionDigits, then maximumIntegerDigits will also be set to
     * the new value
     * @param newValue the minimum number of fraction digits to be shown; if
     * less than zero, then zero is used. The concrete subclass may enforce an
     * upper limit to this value appropriate to the numeric type being formatted.
     * @see #getMinimumFractionDigits
     */
    public void setMinimumFractionDigits(int newValue) {
        minimumFractionDigits = Math.max(0,newValue);
        if (maximumFractionDigits < minimumFractionDigits)
            maximumFractionDigits = minimumFractionDigits;
    }

    // =======================privates===============================

    // [NEW]
    private static NumberFormat getInstance(Locale desiredLocale,
                                            int choice) {
        return new DecimalFormat(getPattern(desiredLocale, choice),
                                 new DecimalFormatSymbols(desiredLocale));
    }

    // [NEW]
    protected static String getPattern(Locale forLocale, int choice) {

        /* The following code takes care of a few cases where the
         * resource data in the underlying JDK lags the new features
         * we have added to ICU4J: scientific notation, rounding, and
         * secondary grouping.
         *
         * We detect these cases here and return various hard-coded
         * resource data.  This is the simplest solution for now, but
         * it is not a good long-term mechanism.
         * 
         * We should replace this code with a data-driven mechanism
         * that reads the bundle com.ibm.text.resources.LocaleElements
         * and parses an exception table that overrides the standard
         * data at java.text.resource.LocaleElements*.java.
         * Alternatively, we should create our own copy of the
         * resource data, and use that exclusively.
         */

        // TEMPORARY, until we get scientific patterns into the main
        // resources:  Retrieve scientific patterns from our resources.
        if (choice == SCIENTIFICSTYLE) {
            // Temporarily hard code; retrieve from resource later
            return "0.######E0";
            // return NumberFormat.getBaseStringArray("NumberPatterns")[SCIENTIFICSTYLE];
        }
        // TEMPORARY: Use rounding for Swiss currency
        if (choice == CURRENCYSTYLE &&
            forLocale.getCountry().equals("CH")) {
            return "'Fr. '#,##0.05;'Fr.-'#,##0.05";
        }
        // TEMPORARY: Special case IN number format
        if (choice == NUMBERSTYLE &&
            forLocale.getCountry().equals("IN")) {
            return "#,##,##0.###";
        }
        // Try the cache first
        String[] numberPatterns = (String[]) cachedLocaleData.get(forLocale);
        if (numberPatterns == null) {
            ResourceBundle resource = ResourceBundle.getBundle
                (RESOURCE_BASE, forLocale);
            numberPatterns = resource.getStringArray("NumberPatterns");
            // Update the cache
            cachedLocaleData.put(forLocale, numberPatterns); 
        }
        return numberPatterns[choice];
    }

    public static final String RESOURCE_BASE = "java.text.resources.LocaleElements";
    
//!    static ResourceBundle baseBundle = null;
//!    
//!    public synchronized static final String[] getBaseStringArray(String name) {
//!        if (baseBundle == null) {
//!            baseBundle = ResourceBundle.getBundle(RESOURCE_BASE);
//!        }
//!        return baseBundle.getStringArray(name);
//!    }

    /**
     * First, read in the default serializable data.
     *
     * Then, if <code>serialVersionOnStream</code> is less than 1, indicating that
     * the stream was written by JDK 1.1,
     * set the <code>int</code> fields such as <code>maximumIntegerDigits</code>
     * to be equal to the <code>byte</code> fields such as <code>maxIntegerDigits</code>,
     * since the <code>int</code> fields were not present in JDK 1.1.
     * Finally, set serialVersionOnStream back to the maximum allowed value so that
     * default serialization will work properly if this object is streamed out again.
     *
     * @since JDK 1.2
     */
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        if (serialVersionOnStream < 1) {
            // Didn't have additional int fields, reassign to use them.
            maximumIntegerDigits = maxIntegerDigits;
            minimumIntegerDigits = minIntegerDigits;
            maximumFractionDigits = maxFractionDigits;
            minimumFractionDigits = minFractionDigits;
        }
        serialVersionOnStream = currentSerialVersion;
    }

    /**
     * Write out the default serializable data, after first setting
     * the <code>byte</code> fields such as <code>maxIntegerDigits</code> to be
     * equal to the <code>int</code> fields such as <code>maximumIntegerDigits</code>
     * (or to <code>Byte.MAX_VALUE</code>, whichever is smaller), for compatibility
     * with the JDK 1.1 version of the stream format.
     *
     * @since JDK 1.2
     */
    private void writeObject(ObjectOutputStream stream)
         throws IOException
    {
        maxIntegerDigits = (maximumIntegerDigits > Byte.MAX_VALUE) ? Byte.MAX_VALUE :
            (byte)maximumIntegerDigits;
        minIntegerDigits = (minimumIntegerDigits > Byte.MAX_VALUE) ? Byte.MAX_VALUE :
            (byte)minimumIntegerDigits;
        maxFractionDigits = (maximumFractionDigits > Byte.MAX_VALUE) ? Byte.MAX_VALUE :
            (byte)maximumFractionDigits;
        minFractionDigits = (minimumFractionDigits > Byte.MAX_VALUE) ? Byte.MAX_VALUE :
            (byte)minimumFractionDigits;
        stream.defaultWriteObject();
    }

    /**
     * Cache to hold the NumberPatterns of a Locale.
     */
    private static final Hashtable cachedLocaleData = new Hashtable(3);

    // Constants used by factory methods to specify a style of format.
    private static final int NUMBERSTYLE = 0;
    private static final int CURRENCYSTYLE = 1;
    private static final int PERCENTSTYLE = 2;
    private static final int SCIENTIFICSTYLE = 3;

    /**
     * True if the the grouping (i.e. thousands) separator is used when
     * formatting and parsing numbers.
     *
     * @serial
     * @see #isGroupingUsed
     */
    private boolean groupingUsed = true;

    /**
     * The maximum number of digits allowed in the integer portion of a
     * number.  <code>maxIntegerDigits</code> must be greater than or equal to
     * <code>minIntegerDigits</code>.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In JDK 1.2 and higher, the new
     * <code>int</code> field <code>maximumIntegerDigits</code> is used instead.
     * When writing to a stream, <code>maxIntegerDigits</code> is set to
     * <code>maximumIntegerDigits</code> or <code>Byte.MAX_VALUE</code>,
     * whichever is smaller.  When reading from a stream, this field is used
     * only if <code>serialVersionOnStream</code> is less than 1. 
     *
     * @serial
     * @see #getMaximumIntegerDigits
     */
    private byte    maxIntegerDigits = 40;

    /**
     * The minimum number of digits allowed in the integer portion of a
     * number.  <code>minimumIntegerDigits</code> must be less than or equal to
     * <code>maximumIntegerDigits</code>.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In JDK 1.2 and higher, the new
     * <code>int</code> field <code>minimumIntegerDigits</code> is used instead.
     * When writing to a stream, <code>minIntegerDigits</code> is set to
     * <code>minimumIntegerDigits</code> or <code>Byte.MAX_VALUE</code>,
     * whichever is smaller.  When reading from a stream, this field is used
     * only if <code>serialVersionOnStream</code> is less than 1. 
     *
     * @serial
     * @see #getMinimumIntegerDigits
     */
    private byte    minIntegerDigits = 1;

    /**
     * The maximum number of digits allowed in the fractional portion of a
     * number.  <code>maximumFractionDigits</code> must be greater than or equal to
     * <code>minimumFractionDigits</code>.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In JDK 1.2 and higher, the new
     * <code>int</code> field <code>maximumFractionDigits</code> is used instead.
     * When writing to a stream, <code>maxFractionDigits</code> is set to
     * <code>maximumFractionDigits</code> or <code>Byte.MAX_VALUE</code>,
     * whichever is smaller.  When reading from a stream, this field is used
     * only if <code>serialVersionOnStream</code> is less than 1. 
     *
     * @serial
     * @see #getMaximumFractionDigits
     */
    private byte    maxFractionDigits = 3;    // invariant, >= minFractionDigits

    /**
     * The minimum number of digits allowed in the fractional portion of a
     * number.  <code>minimumFractionDigits</code> must be less than or equal to
     * <code>maximumFractionDigits</code>.
     * <p>
     * <strong>Note:</strong> This field exists only for serialization
     * compatibility with JDK 1.1.  In JDK 1.2 and higher, the new
     * <code>int</code> field <code>minimumFractionDigits</code> is used instead.
     * When writing to a stream, <code>minFractionDigits</code> is set to
     * <code>minimumFractionDigits</code> or <code>Byte.MAX_VALUE</code>,
     * whichever is smaller.  When reading from a stream, this field is used
     * only if <code>serialVersionOnStream</code> is less than 1. 
     *
     * @serial
     * @see #getMinimumFractionDigits
     */
    private byte    minFractionDigits = 0;

    /**
     * True if this format will parse numbers as integers only.
     *
     * @serial
     * @see #isParseIntegerOnly
     */
    private boolean parseIntegerOnly = false;

    // new fields for 1.2.  byte is too small for integer digits.

    /**
     * The maximum number of digits allowed in the integer portion of a
     * number.  <code>maximumIntegerDigits</code> must be greater than or equal to
     * <code>minimumIntegerDigits</code>.
     *
     * @serial
     * @since JDK 1.2
     * @see #getMaximumIntegerDigits
     */
    private int    maximumIntegerDigits = 40;

    /**
     * The minimum number of digits allowed in the integer portion of a
     * number.  <code>minimumIntegerDigits</code> must be less than or equal to
     * <code>maximumIntegerDigits</code>.
     *
     * @serial
     * @since JDK 1.2
     * @see #getMinimumIntegerDigits
     */
    private int    minimumIntegerDigits = 1;

    /**
     * The maximum number of digits allowed in the fractional portion of a
     * number.  <code>maximumFractionDigits</code> must be greater than or equal to
     * <code>minimumFractionDigits</code>.
     *
     * @serial
     * @since JDK 1.2
     * @see #getMaximumFractionDigits
     */
    private int    maximumFractionDigits = 3;    // invariant, >= minFractionDigits

    /**
     * The minimum number of digits allowed in the fractional portion of a
     * number.  <code>minimumFractionDigits</code> must be less than or equal to
     * <code>maximumFractionDigits</code>.
     *
     * @serial
     * @since JDK 1.2
     * @see #getMinimumFractionDigits
     */
    private int    minimumFractionDigits = 0;

    static final int currentSerialVersion = 1;

    /**
     * Describes the version of <code>NumberFormat</code> present on the stream.
     * Possible values are:
     * <ul>
     * <li><b>0</b> (or uninitialized): the JDK 1.1 version of the stream format.
     *     In this version, the <code>int</code> fields such as
     *     <code>maximumIntegerDigits</code> were not present, and the <code>byte</code>
     *     fields such as <code>maxIntegerDigits</code> are used instead.
     *
     * <li><b>1</b>: the JDK 1.2 version of the stream format.  The values of the
     *     <code>byte</code> fields such as <code>maxIntegerDigits</code> are ignored,
     *     and the <code>int</code> fields such as <code>maximumIntegerDigits</code>
     *     are used instead.
     * </ul>
     * When streaming out a <code>NumberFormat</code>, the most recent format
     * (corresponding to the highest allowable <code>serialVersionOnStream</code>)
     * is always written.
     *
     * @serial
     * @since JDK 1.2
     */
    private int serialVersionOnStream = currentSerialVersion;

    // Removed "implements Cloneable" clause.  Needs to update serialization
    // ID for backward compatibility.
    static final long serialVersionUID = -2308460125733713944L;
}
