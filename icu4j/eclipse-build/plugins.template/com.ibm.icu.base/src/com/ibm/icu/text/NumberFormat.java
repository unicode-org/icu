// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.InvalidObjectException;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Set;

import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;

/**
 * {@icuenhanced java.text.NumberFormat}.{@icu _usage_}
 *
 * <code>NumberFormat</code> is the abstract base class for all number
 * formats. This class provides the interface for formatting and parsing
 * numbers. <code>NumberFormat</code> also provides methods for determining
 * which locales have number formats, and what their names are.
 *
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
 * Starting from ICU 4.2, you can use getInstance() by passing in a 'style'
 * as parameter to get the correct instance.
 * For example,
 * use getInstance(...NUMBERSTYLE) to get the normal number format,
 * getInstance(...PERCENTSTYLE) to get a format for displaying percentage,
 * getInstance(...SCIENTIFICSTYLE) to get a format for displaying scientific number,
 * getInstance(...INTEGERSTYLE) to get an integer number format,
 * getInstance(...CURRENCYSTYLE) to get the currency number format,
 * in which the currency is represented by its symbol, for example, "$3.00".
 * getInstance(...ISOCURRENCYSTYLE)  to get the currency number format,
 * in which the currency is represented by its ISO code, for example "USD3.00".
 * getInstance(...PLURALCURRENCYSTYLE) to get the currency number format,
 * in which the currency is represented by its full name in plural format,
 * for example, "3.00 US dollars" or "1.00 US dollar".
 *
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
     * {@icu} Constant to specify normal number style of format.
     * @stable ICU 4.2
     */
    public static final int NUMBERSTYLE = 0;
    /**
     * {@icu} Constant to specify currency style of format which uses currency symbol
     * to represent currency, for example: "$3.00".
     * @stable ICU 4.2
     */
    public static final int CURRENCYSTYLE = 1;
    /**
     * {@icu} Constant to specify a style of format to display percent.
     * @stable ICU 4.2
     */
    public static final int PERCENTSTYLE = 2;
    /**
     * {@icu} Constant to specify a style of format to display scientific number.
     * @stable ICU 4.2
     */
    public static final int SCIENTIFICSTYLE = 3;
    /**
     * {@icu} Constant to specify a integer number style format.
     * @stable ICU 4.2
     */
    public static final int INTEGERSTYLE = 4;
    /**
     * {@icu} Constant to specify currency style of format which uses currency
     * ISO code to represent currency, for example: "USD3.00".
     * @stable ICU 4.2
     */
    public static final int ISOCURRENCYSTYLE = 5;
    /**
     * {@icu} Constant to specify currency style of format which uses currency
     * long name with plural format to represent currency, for example,
     * "3.00 US Dollars".
     * @stable ICU 4.2
     */
    public static final int PLURALCURRENCYSTYLE = 6;

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
     * Formats a number and appends the resulting text to the given string buffer.
     * {@icunote} recognizes <code>BigInteger</code>
     * and <code>BigDecimal</code> objects.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(Object number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        FieldPosition jdkPos = toJDKFieldPosition(pos);
        StringBuffer buf = numberFormat.format(number, toAppendTo, jdkPos);
        if (jdkPos != null) {
            pos.setBeginIndex(jdkPos.getBeginIndex());
            pos.setEndIndex(jdkPos.getEndIndex());
        }
        return buf;
    }

    /**
     * Parses text from a string to produce a number.
     * @param source the String to parse
     * @param parsePosition the position at which to start the parse
     * @return the parsed number, or null
     * @see java.text.NumberFormat#parseObject(String, ParsePosition)
     * @stable ICU 2.0
     */
    public final Object parseObject(String source,
                                    ParsePosition parsePosition) {
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
     * {@icu} Convenience method to format a BigInteger.
     * @stable ICU 2.0
     */
    public final String format(BigInteger number) {
        return numberFormat.format(number);
    }

    /**
     * Convenience method to format a BigDecimal.
     * @stable ICU 2.0
     */
    public final String format(java.math.BigDecimal number) {
        return numberFormat.format(number);
    }

    /**
     * {@icu} Convenience method to format an ICU BigDecimal.
     * @stable ICU 2.0
     */
    public final String format(com.ibm.icu.math.BigDecimal number) {
        return numberFormat.format(number.toBigDecimal());
    }

//    /**
//     * {@icu} Convenience method to format a CurrencyAmount.
//     * @stable ICU 3.0
//     */
//    public final String format(CurrencyAmount currAmt) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(double number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos) {
        FieldPosition jdkPos = toJDKFieldPosition(pos);
        StringBuffer buf = numberFormat.format(number, toAppendTo, jdkPos);
        pos.setBeginIndex(jdkPos.getBeginIndex());
        pos.setEndIndex(jdkPos.getEndIndex());
        return buf;
    }

    /**
     * Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(long number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos) {
        FieldPosition jdkPos = toJDKFieldPosition(pos);
        StringBuffer buf = numberFormat.format(number, toAppendTo, jdkPos);
        pos.setBeginIndex(jdkPos.getBeginIndex());
        pos.setEndIndex(jdkPos.getEndIndex());
        return buf;
    }
    /**
     * {@icu} Formats a BigInteger. Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(BigInteger number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos) {
        FieldPosition jdkPos = toJDKFieldPosition(pos);
        StringBuffer buf = numberFormat.format(number, toAppendTo, jdkPos);
        pos.setBeginIndex(jdkPos.getBeginIndex());
        pos.setEndIndex(jdkPos.getEndIndex());
        return buf;
    }
    /**
     * {@icu} Formats a BigDecimal. Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(java.math.BigDecimal number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos) {
        FieldPosition jdkPos = toJDKFieldPosition(pos);
        StringBuffer buf = numberFormat.format(number, toAppendTo, jdkPos);
        pos.setBeginIndex(jdkPos.getBeginIndex());
        pos.setEndIndex(jdkPos.getEndIndex());
        return buf;
    }
    /**
     * {@icu} Formats an ICU BigDecimal. Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public StringBuffer format(com.ibm.icu.math.BigDecimal number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos) {
        FieldPosition jdkPos = toJDKFieldPosition(pos);
        StringBuffer buf = numberFormat.format(number.toBigDecimal(), toAppendTo, jdkPos);
        pos.setBeginIndex(jdkPos.getBeginIndex());
        pos.setEndIndex(jdkPos.getEndIndex());
        return buf;
    }

//    /**
//     * {@icu} Formats a CurrencyAmount. Specialization of format.
//     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
//     * @stable ICU 3.0
//     */
//    public StringBuffer format(CurrencyAmount currAmt,
//                               StringBuffer toAppendTo,
//                               FieldPosition pos) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

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
     * @throws ParseException if the beginning of the specified string
     * cannot be parsed.
     * @see #format
     * @stable ICU 2.0
     */
    public Number parse(String text) throws ParseException {
        return numberFormat.parse(text);
    }

//    /**
//     * Parses text from the given string as a CurrencyAmount.  Unlike
//     * the parse() method, this method will attempt to parse a generic
//     * currency name, searching for a match of this object's locale's
//     * currency display names, or for a 3-letter ISO currency code.
//     * This method will fail if this format is not a currency format,
//     * that is, if it does not contain the currency pattern symbol
//     * (U+00A4) in its prefix or suffix.
//     *
//     * @param text the text to parse
//     * @param pos input-output position; on input, the position within
//     * text to match; must have 0 <= pos.getIndex() < text.length();
//     * on output, the position after the last matched character. If
//     * the parse fails, the position in unchanged upon output.
//     * @return a CurrencyAmount, or null upon failure
//     * @draft ICU 49
//     * @provisional This API might change or be removed in a future release.
//     */
//    public CurrencyAmount parseCurrency(CharSequence text, ParsePosition pos) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

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

//    /**
//     * {@icu} Sets whether strict parsing is in effect.  When this is true, the
//     * following conditions cause a parse failure (examples use the pattern "#,##0.#"):<ul>
//     * <li>Leading zeros<br>
//     * '00', '0123' fail the parse, but '0' and '0.001' pass</li>
//     * <li>Leading or doubled grouping separators<br>
//     * ',123' and '1,,234" fail</li>
//     * <li>Groups of incorrect length when grouping is used<br>
//     * '1,23' and '1234,567' fail, but '1234' passes</li>
//     * <li>Grouping separators used in numbers followed by exponents<br>
//     * '1,234E5' fails, but '1234E5' and '1,234E' pass ('E' is not an exponent when
//     * not followed by a number)</li>
//     * </ul>
//     * When strict parsing is off, leading zeros and all grouping separators are ignored.
//     * This is the default behavior.
//     * @param value True to enable strict parsing.  Default is false.
//     * @see #isParseStrict
//     * @stable ICU 3.6
//     */
//    public void setParseStrict(boolean value) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns whether strict parsing is in effect.
//     * @return true if strict parsing is in effect
//     * @see #setParseStrict
//     * @stable ICU 3.6
//     */
//    public boolean isParseStrict() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    //============== Locale Stuff =====================

    /**
     * Returns the default number format for the current default locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getIntegerInstance,
     * getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale-dependent.
     * @stable ICU 2.0
     */
    //Bug 4408066 [Richard/GCL]
    public final static NumberFormat getInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), NUMBERSTYLE);
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
     * {@icu} Returns the default number format for the specified locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale-dependent.
     * @stable ICU 3.2
     */
    public static NumberFormat getInstance(ULocale inLocale) {
        return getInstance(inLocale, NUMBERSTYLE);
    }

    /**
     * {@icu} Returns a specific style number format for default locale.
     * @param style  number format style
     * @stable ICU 4.2
     */
    public final static NumberFormat getInstance(int style) {
        return getInstance(ULocale.getDefault(Category.FORMAT), style);
    }

    /**
     * {@icu} Returns a specific style number format for a specific locale.
     * @param inLocale  the specific locale.
     * @param style     number format style
     * @stable ICU 4.2
     */
    public static NumberFormat getInstance(Locale inLocale, int style) {
        return getInstance(ULocale.forLocale(inLocale), style);
    }


    /**
     * Returns a general-purpose number format for the current default locale.
     * @stable ICU 2.0
     */
    public final static NumberFormat getNumberInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), NUMBERSTYLE);
    }

    /**
     * Returns a general-purpose number format for the specified locale.
     * @stable ICU 2.0
     */
    public static NumberFormat getNumberInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), NUMBERSTYLE);
    }

    /**
     * {@icu} Returns a general-purpose number format for the specified locale.
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
    //Bug 4408066 [Richard/GCL]
    public final static NumberFormat getIntegerInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), INTEGERSTYLE);
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
    //Bug 4408066 [Richard/GCL]
    public static NumberFormat getIntegerInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), INTEGERSTYLE);
    }

    /**
     * {@icu} Returns an integer number format for the specified locale. The
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
        return getInstance(ULocale.getDefault(Category.FORMAT), CURRENCYSTYLE);
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
     * {@icu} Returns a currency format for the specified locale.
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
        return getInstance(ULocale.getDefault(Category.FORMAT), PERCENTSTYLE);
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
     * {@icu} Returns a percentage format for the specified locale.
     * @return a number format for percents
     * @stable ICU 3.2
     */
    public static NumberFormat getPercentInstance(ULocale inLocale) {
        return getInstance(inLocale, PERCENTSTYLE);
    }

    /**
     * {@icu} Returns a scientific format for the current default locale.
     * @return a scientific number format
     * @stable ICU 2.0
     */
    public final static NumberFormat getScientificInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), SCIENTIFICSTYLE);
    }

    /**
     * {@icu} Returns a scientific format for the specified locale.
     * @return a scientific number format
     * @stable ICU 2.0
     */
    public static NumberFormat getScientificInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), SCIENTIFICSTYLE);
    }

    /**
     * {@icu} Returns a scientific format for the specified locale.
     * @return a scientific number format
     * @stable ICU 3.2
     */
    public static NumberFormat getScientificInstance(ULocale inLocale) {
        return getInstance(inLocale, SCIENTIFICSTYLE);
    }

    /**
     * A NumberFormatFactory is used to register new number formats.  The factory
     * should be able to create any of the predefined formats for each locale it
     * supports.  When registered, the locales it supports extend or override the
     * locales already supported by ICU.
     *
     * <p><b>Note:</b> as of ICU4J 3.2, the default API for NumberFormatFactory uses
     * ULocale instead of Locale.  Instead of overriding createFormat(Locale, int),
     * new implementations should override createFactory(ULocale, int).  Note that
     * one of these two methods <b>MUST</b> be overridden or else an infinite
     * loop will occur.
     *
     * @stable ICU 2.6
     */
    public static abstract class NumberFormatFactory {
        /**
         * Value passed to format requesting a default number format.
         * @stable ICU 2.6
         */
        public static final int FORMAT_NUMBER = NUMBERSTYLE;

        /**
         * Value passed to format requesting a currency format.
         * @stable ICU 2.6
         */
        public static final int FORMAT_CURRENCY = CURRENCYSTYLE;

        /**
         * Value passed to format requesting a percent format.
         * @stable ICU 2.6
         */
        public static final int FORMAT_PERCENT = PERCENTSTYLE;

        /**
         * Value passed to format requesting a scientific format.
         * @stable ICU 2.6
         */
        public static final int FORMAT_SCIENTIFIC = SCIENTIFICSTYLE;

        /**
         * Value passed to format requesting an integer format.
         * @stable ICU 2.6
         */
        public static final int FORMAT_INTEGER = INTEGERSTYLE;

        /**
         * Returns true if this factory is visible.  Default is true.
         * If not visible, the locales supported by this factory will not
         * be listed by getAvailableLocales.  This value must not change.
         * @return true if the factory is visible.
         * @stable ICU 2.6
         */
        public boolean visible() {
            return true;
        }

        /**
         * Returns an immutable collection of the locale names directly
         * supported by this factory.
         * @return the supported locale names.
         * @stable ICU 2.6
         */
         public abstract Set<String> getSupportedLocaleNames();

        /**
         * Returns a number format of the appropriate type.  If the locale
         * is not supported, return null.  If the locale is supported, but
         * the type is not provided by this service, return null.  Otherwise
         * return an appropriate instance of NumberFormat.
         * <b>Note:</b> as of ICU4J 3.2, implementations should override
         * this method instead of createFormat(Locale, int).
         * @param loc the locale for which to create the format
         * @param formatType the type of format
         * @return the NumberFormat, or null.
         * @stable ICU 3.2
         */
        public NumberFormat createFormat(ULocale loc, int formatType) {
            return createFormat(loc.toLocale(), formatType);
        }

        /**
         * Returns a number format of the appropriate type.  If the locale
         * is not supported, return null.  If the locale is supported, but
         * the type is not provided by this service, return null.  Otherwise
         * return an appropriate instance of NumberFormat.
         * <b>Note:</b> as of ICU4J 3.2, createFormat(ULocale, int) should be
         * overridden instead of this method.  This method is no longer
         * abstract and delegates to that method.
         * @param loc the locale for which to create the format
         * @param formatType the type of format
         * @return the NumberFormat, or null.
         * @stable ICU 2.6
         */
        public NumberFormat createFormat(Locale loc, int formatType) {
            return createFormat(ULocale.forLocale(loc), formatType);
        }

        /**
         * @stable ICU 2.6
         */
        protected NumberFormatFactory() {
        }
    }

    /**
     * Returns the list of Locales for which NumberFormats are available.
     * @return the available locales
     * @stable ICU 2.0
     */
    public static Locale[] getAvailableLocales() {
        return java.text.NumberFormat.getAvailableLocales();
    }

    /**
     * {@icu} Returns the list of Locales for which NumberFormats are available.
     * @return the available locales
     * @draft ICU 3.2 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public static ULocale[] getAvailableULocales() {
        if (availableULocales == null) {
            synchronized (NumberFormat.class) {
                if (availableULocales == null) {
                    Locale[] locales = java.text.NumberFormat.getAvailableLocales();
                    ULocale[] ulocales = new ULocale[locales.length];
                    for (int i = 0; i < locales.length; ++i) {
                        ulocales[i] = ULocale.forLocale(locales[i]);
                    }
                    availableULocales = ulocales;
                }
            }
        }
        return (ULocale[])availableULocales.clone();
    }
    private static volatile ULocale[] availableULocales;

//    /**
//     * {@icu} Registers a new NumberFormatFactory.  The factory is adopted by
//     * the service and must not be modified.  The returned object is a
//     * key that can be used to unregister this factory.
//     * @param factory the factory to register
//     * @return a key with which to unregister the factory
//     * @stable ICU 2.6
//     */
//    public static Object registerFactory(NumberFormatFactory factory) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Unregisters the factory or instance associated with this key (obtained from
//     * registerInstance or registerFactory).
//     * @param registryKey a key obtained from registerFactory
//     * @return true if the object was successfully unregistered
//     * @stable ICU 2.6
//     */
//    public static boolean unregister(Object registryKey) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Overrides hashCode.
     * @stable ICU 2.0
     */
    public int hashCode() {
        return numberFormat.hashCode();
    }

    /**
     * Overrides equals.
     * Two NumberFormats are equal if they are of the same class
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
     * Overrides clone.
     * @stable ICU 2.0
     */
    public Object clone() {
        return new NumberFormat((java.text.NumberFormat)numberFormat.clone());
    }

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
     * Sets the <tt>Currency</tt> object used to display currency
     * amounts.  This takes effect immediately, if this format is a
     * currency format.  If this format is not a currency format, then
     * the currency object is used if and when this object becomes a
     * currency format.
     * @param theCurrency new currency object to use.  May be null for
     * some subclasses.
     * @stable ICU 2.6
     */
    public void setCurrency(Currency theCurrency) {
        numberFormat.setCurrency(theCurrency.currency);
    }

    /**
     * Returns the <tt>Currency</tt> object used to display currency
     * amounts.  This may be null.
     * @stable ICU 2.6
     */
    public Currency getCurrency() {
        return new Currency(numberFormat.getCurrency());
    }

    /**
     * Returns the rounding mode used in this NumberFormat.  The default implementation of
     * tis method in NumberFormat always throws <code>UnsupportedOperationException</code>.
     * @return A rounding mode, between <code>BigDecimal.ROUND_UP</code>
     * and <code>BigDecimal.ROUND_UNNECESSARY</code>.
     * @see #setRoundingMode(int)
     * @stable ICU 4.0
     */
    public int getRoundingMode() {
        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
    }

    /**
     * Set the rounding mode used in this NumberFormat.  The default implementation of
     * tis method in NumberFormat always throws <code>UnsupportedOperationException</code>.
     * @param roundingMode A rounding mode, between
     * <code>BigDecimal.ROUND_UP</code> and
     * <code>BigDecimal.ROUND_UNNECESSARY</code>.
     * @see #getRoundingMode()
     * @stable ICU 4.0
     */
    public void setRoundingMode(int roundingMode) {
        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
    }


    /**
     * Returns a specific style number format for a specific locale.
     * @param desiredLocale  the specific locale.
     * @param choice         number format style
     * @throws IllegalArgumentException  if choice is not one of
     *                                   NUMBERSTYLE, CURRENCYSTYLE,
     *                                   PERCENTSTYLE, SCIENTIFICSTYLE,
     *                                   INTEGERSTYLE,
     *                                   ISOCURRENCYSTYLE, PLURALCURRENCYSTYLE,
     * @stable ICU 4.2
     */
    public static NumberFormat getInstance(ULocale desiredLocale, int choice) {
        Locale locale = desiredLocale.toLocale();
        java.text.NumberFormat nf = null;
        switch (choice) {
        case NUMBERSTYLE:
            nf = java.text.NumberFormat.getInstance(locale);
            break;
        case INTEGERSTYLE:
            nf = java.text.NumberFormat.getIntegerInstance(locale);
            break;
        case CURRENCYSTYLE:
            nf = java.text.NumberFormat.getCurrencyInstance(locale);
            break;
        case PERCENTSTYLE:
            nf = java.text.NumberFormat.getPercentInstance(locale);
            break;
        case SCIENTIFICSTYLE:
            nf = new java.text.DecimalFormat("#E0",
                    new java.text.DecimalFormatSymbols(locale));
            nf.setMaximumFractionDigits(10);
            break;
        }
        return new NumberFormat(nf);
    }

    /**
     * Empty constructor.  Public for compatibility with JDK which lets the
     * compiler generate a default public constructor even though this is
     * an abstract class.
     * @stable ICU 2.6
     */
    public NumberFormat() {
        this(java.text.NumberFormat.getInstance(ULocale.getDefault(Category.FORMAT).toLocale()));
    }

    /**
     * The instances of this inner class are used as attribute keys and values
     * in AttributedCharacterIterator that
     * NumberFormat.formatToCharacterIterator() method returns.
     * <p>
     * There is no public constructor to this class, the only instances are the
     * constants defined here.
     * <p>
     * @stable ICU 3.6
     */
    public static class Field extends Format.Field {
        // generated by serialver from JDK 1.4.1_01
        static final long serialVersionUID = -4516273749929385842L;

        /**
         * @stable ICU 3.6
         */
        public static final Field SIGN = new Field("sign");

        /**
         * @stable ICU 3.6
         */
        public static final Field INTEGER = new Field("integer");

        /**
         * @stable ICU 3.6
         */
        public static final Field FRACTION = new Field("fraction");

        /**
         * @stable ICU 3.6
         */
        public static final Field EXPONENT = new Field("exponent");

        /**
         * @stable ICU 3.6
         */
        public static final Field EXPONENT_SIGN = new Field("exponent sign");

        /**
         * @stable ICU 3.6
         */
        public static final Field EXPONENT_SYMBOL = new Field("exponent symbol");

        /**
         * @stable ICU 3.6
         */
        public static final Field DECIMAL_SEPARATOR = new Field("decimal separator");
        /**
         * @stable ICU 3.6
         */
        public static final Field GROUPING_SEPARATOR = new Field("grouping separator");

        /**
         * @stable ICU 3.6
         */
        public static final Field PERCENT = new Field("percent");

        /**
         * @stable ICU 3.6
         */
        public static final Field PERMILLE = new Field("per mille");

        /**
         * @stable ICU 3.6
         */
        public static final Field CURRENCY = new Field("currency");

        /**
         * Constructs a new instance of NumberFormat.Field with the given field
         * name.
         * @stable ICU 3.6
         */
        protected Field(String fieldName) {
            super(fieldName);
        }

        /**
         * serizalization method resolve instances to the constant
         * NumberFormat.Field values
         * @stable ICU 3.6
         */
        protected Object readResolve() throws InvalidObjectException {
            if (this.getName().equals(INTEGER.getName()))
                return INTEGER;
            if (this.getName().equals(FRACTION.getName()))
                return FRACTION;
            if (this.getName().equals(EXPONENT.getName()))
                return EXPONENT;
            if (this.getName().equals(EXPONENT_SIGN.getName()))
                return EXPONENT_SIGN;
            if (this.getName().equals(EXPONENT_SYMBOL.getName()))
                return EXPONENT_SYMBOL;
            if (this.getName().equals(CURRENCY.getName()))
                return CURRENCY;
            if (this.getName().equals(DECIMAL_SEPARATOR.getName()))
                return DECIMAL_SEPARATOR;
            if (this.getName().equals(GROUPING_SEPARATOR.getName()))
                return GROUPING_SEPARATOR;
            if (this.getName().equals(PERCENT.getName()))
                return PERCENT;
            if (this.getName().equals(PERMILLE.getName()))
                return PERMILLE;
            if (this.getName().equals(SIGN.getName()))
                return SIGN;

            throw new InvalidObjectException("An invalid object.");
        }
    }

    private static FieldPosition toJDKFieldPosition(FieldPosition icuPos) {
        if (icuPos == null) {
            return null;
        }

        int fieldID = icuPos.getField();
        Format.Field fieldAttribute = icuPos.getFieldAttribute();

        FieldPosition jdkPos = null;

        if (fieldID >= 0) {
            if (fieldID == FRACTION_FIELD) {
                fieldID = java.text.NumberFormat.FRACTION_FIELD;
            } else if (fieldID == INTEGER_FIELD) {
                fieldID = java.text.NumberFormat.INTEGER_FIELD;
            }
        }

        if (fieldAttribute != null) {
            // map field
            if (fieldAttribute.equals(Field.CURRENCY)) {
                fieldAttribute = java.text.NumberFormat.Field.CURRENCY;
            } else if (fieldAttribute.equals(Field.DECIMAL_SEPARATOR)) {
                fieldAttribute = java.text.NumberFormat.Field.DECIMAL_SEPARATOR;
            } else if (fieldAttribute.equals(Field.EXPONENT)) {
                fieldAttribute = java.text.NumberFormat.Field.EXPONENT;
            } else if (fieldAttribute.equals(Field.EXPONENT_SIGN)) {
                fieldAttribute = java.text.NumberFormat.Field.EXPONENT_SIGN;
            } else if (fieldAttribute.equals(Field.EXPONENT_SYMBOL)) {
                fieldAttribute = java.text.NumberFormat.Field.EXPONENT_SYMBOL;
            } else if (fieldAttribute.equals(Field.FRACTION)) {
                fieldAttribute = java.text.NumberFormat.Field.FRACTION;
            } else if (fieldAttribute.equals(Field.GROUPING_SEPARATOR)) {
                fieldAttribute = java.text.NumberFormat.Field.GROUPING_SEPARATOR;
            } else if (fieldAttribute.equals(Field.INTEGER)) {
                fieldAttribute = java.text.NumberFormat.Field.INTEGER;
            } else if (fieldAttribute.equals(Field.PERCENT)) {
                fieldAttribute = java.text.NumberFormat.Field.PERCENT;
            } else if (fieldAttribute.equals(Field.PERMILLE)) {
                fieldAttribute = java.text.NumberFormat.Field.PERMILLE;
            } else if (fieldAttribute.equals(Field.SIGN)) {
                fieldAttribute = java.text.NumberFormat.Field.SIGN;
            }

            jdkPos = new FieldPosition(fieldAttribute, fieldID);
        } else {
            jdkPos = new FieldPosition(fieldID);
        }

        jdkPos.setBeginIndex(icuPos.getBeginIndex());
        jdkPos.setEndIndex(icuPos.getEndIndex());

        return jdkPos;
    }
}
