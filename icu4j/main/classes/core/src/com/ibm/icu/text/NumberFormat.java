// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Collections;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;
import com.ibm.icu.util.UResourceBundle;

/**
 * {@icuenhanced java.text.NumberFormat}.{@icu _usage_}
 *
 * <p>
 * <strong>IMPORTANT:</strong> New users are strongly encouraged to see if
 * {@link NumberFormatter} fits their use case.  Although not deprecated, this
 * class, NumberFormat, is only provided for java.text.NumberFormat compatibility.
 * <hr>
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
 * for (int i = 0; i &lt; a.length; ++i) {
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
 * format for displaying percentages. Some factory methods are found within
 * subclasses of NumberFormat. With this format, a fraction like
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
 * if true,  "3456.78" -&gt; 3456 (and leaves the parse position just after '6')
 * if false, "3456.78" -&gt; 3456.78 (and leaves the parse position just after '8')
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
 * <h3>Synchronization</h3>
 * <p>
 * Number formats are generally not synchronized. It is recommended to create
 * separate format instances for each thread. If multiple threads access a format
 * concurrently, it must be synchronized externally.
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
public abstract class NumberFormat extends UFormat {

    /**
     * {@icu} Constant to specify normal number style of format.
     * @stable ICU 4.2
     */
    public static final int NUMBERSTYLE = 0;
    /**
     * {@icu} Constant to specify general currency style of format. Defaults to
     * STANDARDCURRENCYSTYLE, using currency symbol, for example "$3.00", with
     * non-accounting style for negative values (e.g. minus sign).
     * The specific style may be specified using the -cf- locale key.
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
     * {@icu} Constant to specify currency style of format which uses currency symbol
     * to represent currency for accounting, for example: "($3.00), instead of
     * "-$3.00" ({@link #CURRENCYSTYLE}).
     * Overrides any style specified using -cf- key in locale.
     * @stable ICU 53
     */
    public static final int ACCOUNTINGCURRENCYSTYLE = 7;
    /**
     * {@icu} Constant to specify currency cash style of format which uses currency
     * ISO code to represent currency, for example: "NT$3" instead of "NT$3.23".
     * @stable ICU 54
     */
    public static final int CASHCURRENCYSTYLE = 8;
    /**
     * {@icu} Constant to specify currency style of format which uses currency symbol
     * to represent currency, for example "$3.00", using non-accounting style for
     * negative values (e.g. minus sign).
     * Overrides any style specified using -cf- key in locale.
     * @stable ICU 56
     */
    public static final int STANDARDCURRENCYSTYLE = 9;

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
    @Override
    public StringBuffer format(Object number,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        // NOTE: Number type expansion happens both here
        // and in DecimalQuantity_DualStorageBCD.java
        if (number instanceof Long) {
            return format(((Long)number).longValue(), toAppendTo, pos);
        } else if (number instanceof BigInteger) {
            return format((BigInteger) number, toAppendTo, pos);
        } else if (number instanceof java.math.BigDecimal) {
            return format((java.math.BigDecimal) number, toAppendTo, pos);
        } else if (number instanceof com.ibm.icu.math.BigDecimal) {
            return format((com.ibm.icu.math.BigDecimal) number, toAppendTo, pos);
        } else if (number instanceof CurrencyAmount) {
            return format((CurrencyAmount)number, toAppendTo, pos);
        } else if (number instanceof Number) {
            return format(((Number)number).doubleValue(), toAppendTo, pos);
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
    }

    /**
     * Parses text from a string to produce a number.
     * @param source the String to parse
     * @param parsePosition the position at which to start the parse
     * @return the parsed number, or null
     * @see java.text.NumberFormat#parseObject(String, ParsePosition)
     * @stable ICU 2.0
     */
    @Override
    public final Object parseObject(String source,
                                    ParsePosition parsePosition) {
        return parse(source, parsePosition);
    }

    /**
     * Specialization of format.
     * @see java.text.Format#format(Object)
     * @stable ICU 2.0
     */
    public final String format(double number) {
        return format(number,new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * Specialization of format.
     * @see java.text.Format#format(Object)
     * @stable ICU 2.0
     */
    public final String format(long number) {
        StringBuffer buf = new StringBuffer(19);
        FieldPosition pos = new FieldPosition(0);
        format(number, buf, pos);
        return buf.toString();
    }

    /**
     * {@icu} Convenience method to format a BigInteger.
     * @stable ICU 2.0
     */
    public final String format(BigInteger number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * Convenience method to format a BigDecimal.
     * @stable ICU 2.0
     */
    public final String format(java.math.BigDecimal number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * {@icu} Convenience method to format an ICU BigDecimal.
     * @stable ICU 2.0
     */
    public final String format(com.ibm.icu.math.BigDecimal number) {
        return format(number, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * {@icu} Convenience method to format a CurrencyAmount.
     * @stable ICU 3.0
     */
    public final String format(CurrencyAmount currAmt) {
        return format(currAmt, new StringBuffer(),
                      new FieldPosition(0)).toString();
    }

    /**
     * Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(double number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);

    /**
     * Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(long number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);
    /**
     * {@icu} Formats a BigInteger. Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(BigInteger number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);
    /**
     * {@icu} Formats a BigDecimal. Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(java.math.BigDecimal number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);
    /**
     * {@icu} Formats an ICU BigDecimal. Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(com.ibm.icu.math.BigDecimal number,
                                        StringBuffer toAppendTo,
                                        FieldPosition pos);
    /**
     * {@icu} Formats a CurrencyAmount. Specialization of format.
     * @see java.text.Format#format(Object, StringBuffer, FieldPosition)
     * @stable ICU 3.0
     */
    public StringBuffer format(CurrencyAmount currAmt,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {
        // Default implementation -- subclasses may override
        synchronized(this) {
            Currency save = getCurrency(), curr = currAmt.getCurrency();
            boolean same = curr.equals(save);
            if (!same) setCurrency(curr);
            format(currAmt.getNumber(), toAppendTo, pos);
            if (!same) setCurrency(save);
        }
        return toAppendTo;
    }

    /**
     * Returns a Long if possible (e.g., within the range [Long.MIN_VALUE,
     * Long.MAX_VALUE] and with no decimals); otherwise, returns another type,
     * such as a BigDecimal, BigInteger, or Double. The return type is not
     * guaranteed other than for the Long case.
     *
     * <p>If IntegerOnly is set, will stop at a decimal
     * point (or equivalent; e.g., for rational numbers "1 2/3", will stop
     * after the 1).
     *
     * <p>Does not throw an exception; if no object can be parsed, index is
     * unchanged!
     *
     * <p>For more detail on parsing, see the "Parsing" header in the class
     * documentation of {@link DecimalFormat}.
     *
     * @see #isParseIntegerOnly
     * @see DecimalFormat#setParseBigDecimal
     * @see java.text.Format#parseObject(String, ParsePosition)
     * @stable ICU 2.0
     */
    public abstract Number parse(String text, ParsePosition parsePosition);

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
    //Bug 4375399 [Richard/GCL]
    public Number parse(String text) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        Number result = parse(text, parsePosition);
        if (parsePosition.getIndex() == 0) {
            throw new ParseException("Unparseable number: \"" + text + '"',
                                     parsePosition.getErrorIndex());
        }
        return result;
    }

    /**
     * Parses text from the given string as a CurrencyAmount.  Unlike
     * the parse() method, this method will attempt to parse a generic
     * currency name, searching for a match of this object's locale's
     * currency display names, or for a 3-letter ISO currency code.
     * This method will fail if this format is not a currency format,
     * that is, if it does not contain the currency pattern symbol
     * (U+00A4) in its prefix or suffix.
     *
     * @param text the text to parse
     * @param pos input-output position; on input, the position within
     * text to match; must have 0 &lt;= pos.getIndex() &lt; text.length();
     * on output, the position after the last matched character. If
     * the parse fails, the position in unchanged upon output.
     * @return a CurrencyAmount, or null upon failure
     * @stable ICU 49
     */
    public CurrencyAmount parseCurrency(CharSequence text, ParsePosition pos) {
        ///CLOVER:OFF
        // Default implementation only -- subclasses should override
        Number n = parse(text.toString(), pos);
        return n == null ? null : new CurrencyAmount(n, getEffectiveCurrency());
        ///CLOVER:ON
    }

    /**
     * Returns true if this format will parse numbers as integers only.
     * For example in the English locale, with ParseIntegerOnly true, the
     * string "1234." would be parsed as the integer value 1234 and parsing
     * would stop at the "." character.  The decimal separator accepted
     * by the parse operation is locale-dependent and determined by the
     * subclass.
     *
     * @return true if this will parse integers only
     * @stable ICU 2.0
     */
    public boolean isParseIntegerOnly() {
        return parseIntegerOnly;
    }

    /**
     * Sets whether to ignore the fraction part of a number when parsing
     * (defaults to false). If a string contains a decimal point, parsing will stop before the decimal
     * point. Note that determining whether a character is a decimal point depends on the locale.
     *
     * <p>For example, in <em>en-US</em>, parsing the string "123.45" will return the number 123 and
     * parse position 3.
     *
     * @param value true if this should parse integers only
     * @see #isParseIntegerOnly
     * @stable ICU 2.0
     */
    public void setParseIntegerOnly(boolean value) {
        parseIntegerOnly = value;
    }

    /**
     * {@icu} Sets whether strict parsing is in effect.  When this is true, the string
     * is required to be a stronger match to the pattern than when lenient parsing is in
     * effect.  More specifically, the following conditions cause a parse failure relative
     * to lenient mode (examples use the pattern "#,##0.#"):<ul>
     * <li>The presence and position of special symbols, including currency, must match the
     * pattern.<br>
     * '+123' fails (there is no plus sign in the pattern)</li>
     * <li>Leading or doubled grouping separators<br>
     * ',123' and '1,,234" fail</li>
     * <li>Groups of incorrect length when grouping is used<br>
     * '1,23' and '1234,567' fail, but '1234' passes</li>
     * <li>Grouping separators used in numbers followed by exponents<br>
     * '1,234E5' fails, but '1234E5' and '1,234E' pass ('E' is not an exponent when
     * not followed by a number)</li>
     * </ul>
     * When strict parsing is off,  all grouping separators are ignored.
     * This is the default behavior.
     * @param value True to enable strict parsing.  Default is false.
     * @see #isParseStrict
     * @stable ICU 3.6
     */
    public void setParseStrict(boolean value) {
        parseStrict = value;
    }

    /**
     * {@icu} Returns whether strict parsing is in effect.
     * @return true if strict parsing is in effect
     * @see #setParseStrict
     * @stable ICU 3.6
     */
    public boolean isParseStrict() {
        return parseStrict;
    }

    /**
     * {@icu} Set a particular DisplayContext value in the formatter,
     * such as CAPITALIZATION_FOR_STANDALONE.
     *
     * @param context The DisplayContext value to set.
     * @stable ICU 53
     */
    public void setContext(DisplayContext context) {
        if (context.type() == DisplayContext.Type.CAPITALIZATION) {
            capitalizationSetting = context;
        }
    }

    /**
     * {@icu} Get the formatter's DisplayContext value for the specified DisplayContext.Type,
     * such as CAPITALIZATION.
     *
     * @param type the DisplayContext.Type whose value to return
     * @return the current DisplayContext setting for the specified type
     * @stable ICU 53
     */
    public DisplayContext getContext(DisplayContext.Type type) {
        return (type == DisplayContext.Type.CAPITALIZATION && capitalizationSetting != null)?
                capitalizationSetting: DisplayContext.CAPITALIZATION_NONE;
    }

    //============== Locale Stuff =====================

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns the default number format for the current default <code>FORMAT</code> locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getIntegerInstance,
     * getCurrencyInstance or getPercentInstance.
     * Exactly which one is locale-dependent.
     * @see Category#FORMAT
     * @stable ICU 2.0
     */
    //Bug 4408066 [Richard/GCL]
    public final static NumberFormat getInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), NUMBERSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
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
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
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
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * {@icu} Returns a specific style number format for default <code>FORMAT</code> locale.
     * @param style  number format style
     * @see Category#FORMAT
     * @stable ICU 4.2
     */
    public final static NumberFormat getInstance(int style) {
        return getInstance(ULocale.getDefault(Category.FORMAT), style);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * {@icu} Returns a specific style number format for a specific locale.
     * @param inLocale  the specific locale.
     * @param style     number format style
     * @stable ICU 4.2
     */
    public static NumberFormat getInstance(Locale inLocale, int style) {
        return getInstance(ULocale.forLocale(inLocale), style);
    }


    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns a general-purpose number format for the current default <code>FORMAT</code> locale.
     * @see Category#FORMAT
     * @stable ICU 2.0
     */
    public final static NumberFormat getNumberInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), NUMBERSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns a general-purpose number format for the specified locale.
     * @stable ICU 2.0
     */
    public static NumberFormat getNumberInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), NUMBERSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * {@icu} Returns a general-purpose number format for the specified locale.
     * @stable ICU 3.2
     */
    public static NumberFormat getNumberInstance(ULocale inLocale) {
        return getInstance(inLocale, NUMBERSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns an integer number format for the current default <code>FORMAT</code> locale. The
     * returned number format is configured to round floating point numbers
     * to the nearest integer using IEEE half-even rounding (see {@link
     * com.ibm.icu.math.BigDecimal#ROUND_HALF_EVEN ROUND_HALF_EVEN}) for formatting,
     * and to parse only the integer part of an input string (see {@link
     * #isParseIntegerOnly isParseIntegerOnly}).
     *
     * @return a number format for integer values
     * @see Category#FORMAT
     * @stable ICU 2.0
     */
    //Bug 4408066 [Richard/GCL]
    public final static NumberFormat getIntegerInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), INTEGERSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
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
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
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
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns a currency format for the current default <code>FORMAT</code> locale.
     * @return a number format for currency
     * @see Category#FORMAT
     * @stable ICU 2.0
     */
    public final static NumberFormat getCurrencyInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), CURRENCYSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns a currency format for the specified locale.
     * @return a number format for currency
     * @stable ICU 2.0
     */
    public static NumberFormat getCurrencyInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), CURRENCYSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * {@icu} Returns a currency format for the specified locale.
     * @return a number format for currency
     * @stable ICU 3.2
     */
    public static NumberFormat getCurrencyInstance(ULocale inLocale) {
        return getInstance(inLocale, CURRENCYSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns a percentage format for the current default <code>FORMAT</code> locale.
     * @return a number format for percents
     * @see Category#FORMAT
     * @stable ICU 2.0
     */
    public final static NumberFormat getPercentInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), PERCENTSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns a percentage format for the specified locale.
     * @return a number format for percents
     * @stable ICU 2.0
     */
    public static NumberFormat getPercentInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), PERCENTSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * {@icu} Returns a percentage format for the specified locale.
     * @return a number format for percents
     * @stable ICU 3.2
     */
    public static NumberFormat getPercentInstance(ULocale inLocale) {
        return getInstance(inLocale, PERCENTSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * {@icu} Returns a scientific format for the current default <code>FORMAT</code> locale.
     * @return a scientific number format
     * @see Category#FORMAT
     * @stable ICU 2.0
     */
    public final static NumberFormat getScientificInstance() {
        return getInstance(ULocale.getDefault(Category.FORMAT), SCIENTIFICSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * {@icu} Returns a scientific format for the specified locale.
     * @return a scientific number format
     * @stable ICU 2.0
     */
    public static NumberFormat getScientificInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale), SCIENTIFICSTYLE);
    }

    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * {@icu} Returns a scientific format for the specified locale.
     * @return a scientific number format
     * @stable ICU 3.2
     */
    public static NumberFormat getScientificInstance(ULocale inLocale) {
        return getInstance(inLocale, SCIENTIFICSTYLE);
    }

    // ===== Factory stuff =====
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
     * A NumberFormatFactory that supports a single locale.  It can be visible or invisible.
     * @stable ICU 2.6
     */
    public static abstract class SimpleNumberFormatFactory extends NumberFormatFactory {
        final Set<String> localeNames;
        final boolean visible;

        /**
         * Constructs a SimpleNumberFormatFactory with the given locale.
         * @stable ICU 2.6
         */
        public SimpleNumberFormatFactory(Locale locale) {
            this(locale, true);
        }

        /**
         * Constructs a SimpleNumberFormatFactory with the given locale and the
         * visibility.
         * @stable ICU 2.6
         */
        public SimpleNumberFormatFactory(Locale locale, boolean visible) {
            localeNames = Collections.singleton(ULocale.forLocale(locale).getBaseName());
            this.visible = visible;
        }

        /**
         * Constructs a SimpleNumberFormatFactory with the given locale.
         * @stable ICU 3.2
         */
        public SimpleNumberFormatFactory(ULocale locale) {
            this(locale, true);
        }

        /**
         * Constructs a SimpleNumberFormatFactory with the given locale and the
         * visibility.
         * @stable ICU 3.2
         */
        public SimpleNumberFormatFactory(ULocale locale, boolean visible) {
            localeNames = Collections.singleton(locale.getBaseName());
            this.visible = visible;
        }

        /**
         * {@inheritDoc}
         * @stable ICU 2.6
         */
        @Override
        public final boolean visible() {
            return visible;
        }

        /**
         * {@inheritDoc}
         * @stable ICU 2.6
         */
        @Override
        public final Set<String> getSupportedLocaleNames() {
            return localeNames;
        }
    }

    // shim so we can build without service code
    static abstract class NumberFormatShim {
        abstract Locale[] getAvailableLocales();
        abstract ULocale[] getAvailableULocales();
        abstract Object registerFactory(NumberFormatFactory f);
        abstract boolean unregister(Object k);
        abstract NumberFormat createInstance(ULocale l, int k);
    }

    private static NumberFormatShim shim;
    private static NumberFormatShim getShim() {
        // Note: this instantiation is safe on loose-memory-model configurations
        // despite lack of synchronization, since the shim instance has no state--
        // it's all in the class init.  The worst problem is we might instantiate
        // two shim instances, but they'll share the same state so that's ok.
        if (shim == null) {
            try {
                Class<?> cls = Class.forName("com.ibm.icu.text.NumberFormatServiceShim");
                shim = (NumberFormatShim)cls.newInstance();
            }
            ///CLOVER:OFF
            catch (MissingResourceException e){
                throw e;
            }
            catch (Exception e) {
               // e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
            ///CLOVER:ON
        }
        return shim;
    }

    /**
     * Returns the list of Locales for which NumberFormats are available.
     * @return the available locales
     * @stable ICU 2.0
     */
    public static Locale[] getAvailableLocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableLocales();
        }
        return getShim().getAvailableLocales();
    }

    /**
     * {@icu} Returns the list of Locales for which NumberFormats are available.
     * @return the available locales
     * @draft ICU 3.2 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public static ULocale[] getAvailableULocales() {
        if (shim == null) {
            return ICUResourceBundle.getAvailableULocales();
        }
        return getShim().getAvailableULocales();
    }

    /**
     * {@icu} Registers a new NumberFormatFactory.  The factory is adopted by
     * the service and must not be modified.  The returned object is a
     * key that can be used to unregister this factory.
     *
     * <p>Because ICU may choose to cache NumberFormat objects internally, this must
     * be called at application startup, prior to any calls to
     * NumberFormat.getInstance to avoid undefined behavior.
     *
     * @param factory the factory to register
     * @return a key with which to unregister the factory
     * @stable ICU 2.6
     */
    public static Object registerFactory(NumberFormatFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        return getShim().registerFactory(factory);
    }

    /**
     * {@icu} Unregisters the factory or instance associated with this key (obtained from
     * registerInstance or registerFactory).
     * @param registryKey a key obtained from registerFactory
     * @return true if the object was successfully unregistered
     * @stable ICU 2.6
     */
    public static boolean unregister(Object registryKey) {
        if (registryKey == null) {
            throw new IllegalArgumentException("registryKey must not be null");
        }

        if (shim == null) {
            return false;
        }

        return shim.unregister(registryKey);
    }

    // ===== End of factory stuff =====

    /**
     * {@inheritDoc}
     *
     * @stable ICU 2.0
     */
    @Override
    public int hashCode() {
        return maximumIntegerDigits * 37 + maxFractionDigits;
        // just enough fields for a reasonable distribution
    }

    /**
     * Overrides equals.
     * Two NumberFormats are equal they are of the same class
     * and the user-specified values for settings
     * (groupingUsed, parseIntegerOnly, maximumIntegerDigits, etc.)
     * are equal.
     * @param obj the object to compare against
     * @return true if the object is equal to this.
     * @stable ICU 2.0
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        NumberFormat other = (NumberFormat) obj;
        return maximumIntegerDigits == other.maximumIntegerDigits
            && minimumIntegerDigits == other.minimumIntegerDigits
            && maximumFractionDigits == other.maximumFractionDigits
            && minimumFractionDigits == other.minimumFractionDigits
            && groupingUsed == other.groupingUsed
            && parseIntegerOnly == other.parseIntegerOnly
            && parseStrict == other.parseStrict
            && capitalizationSetting == other.capitalizationSetting;
    }

    /**
     * Overrides clone.
     * @stable ICU 2.0
     */
    @Override
    public Object clone() {
        NumberFormat other = (NumberFormat) super.clone();
        return other;
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
        return groupingUsed;
    }

    /**
     * Sets whether or not grouping will be used in this format.  Grouping
     * affects both parsing and formatting.
     * @see #isGroupingUsed
     * @param newValue true to use grouping.
     * @stable ICU 2.0
     */
    public void setGroupingUsed(boolean newValue) {
        groupingUsed = newValue;
    }

    /**
     * Returns the maximum number of digits allowed in the integer portion of a
     * number.  The default value is 40, which subclasses can override.
     *
     * When formatting, if the number of digits exceeds this value, the highest-
     * significance digits are truncated until the limit is reached, in accordance
     * with UTS#35.
     *
     * This setting has no effect on parsing.
     *
     * @return the maximum number of integer digits
     * @see #setMaximumIntegerDigits
     * @stable ICU 2.0
     */
    public int getMaximumIntegerDigits() {
        return maximumIntegerDigits;
    }

    /**
     * Sets the maximum number of digits allowed in the integer portion of a
     * number. This must be &gt;= minimumIntegerDigits.  If the
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
        maximumIntegerDigits = Math.max(0,newValue);
        if (minimumIntegerDigits > maximumIntegerDigits)
            minimumIntegerDigits = maximumIntegerDigits;
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
        return minimumIntegerDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the integer portion of a
     * number.  This must be &lt;= maximumIntegerDigits.  If the
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
        minimumIntegerDigits = Math.max(0,newValue);
        if (minimumIntegerDigits > maximumIntegerDigits)
            maximumIntegerDigits = minimumIntegerDigits;
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
        return maximumFractionDigits;
    }

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a
     * number. This must be &gt;= minimumFractionDigits.  If the
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
        maximumFractionDigits = Math.max(0,newValue);
        if (maximumFractionDigits < minimumFractionDigits)
            minimumFractionDigits = maximumFractionDigits;
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
        return minimumFractionDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number.  This must be &lt;= maximumFractionDigits.  If the
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
        minimumFractionDigits = Math.max(0,newValue);
        if (maximumFractionDigits < minimumFractionDigits)
            maximumFractionDigits = minimumFractionDigits;
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
        currency = theCurrency;
    }

    /**
     * Returns the <tt>Currency</tt> object used to display currency
     * amounts.  This may be null.
     * @stable ICU 2.6
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Returns the currency in effect for this formatter.  Subclasses
     * should override this method as needed.  Unlike getCurrency(),
     * this method should never return null.
     * @return a non-null Currency
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected Currency getEffectiveCurrency() {
        Currency c = getCurrency();
        if (c == null) {
            ULocale uloc = getLocale(ULocale.VALID_LOCALE);
            if (uloc == null) {
                uloc = ULocale.getDefault(Category.FORMAT);
            }
            c = Currency.getInstance(uloc);
        }
        return c;
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
        throw new UnsupportedOperationException(
            "getRoundingMode must be implemented by the subclass implementation.");
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
        throw new UnsupportedOperationException(
            "setRoundingMode must be implemented by the subclass implementation.");
    }


    /**
     * <strong>NOTE:</strong> New users are strongly encouraged to use
     * {@link NumberFormatter} instead of NumberFormat.
     * <hr>
     * Returns a specific style number format for a specific locale.
     * @param desiredLocale  the specific locale.
     * @param choice         number format style
     * @throws IllegalArgumentException  if choice is not one of
     *                                   NUMBERSTYLE, CURRENCYSTYLE,
     *                                   PERCENTSTYLE, SCIENTIFICSTYLE,
     *                                   INTEGERSTYLE, ISOCURRENCYSTYLE,
     *                                   PLURALCURRENCYSTYLE, ACCOUNTINGCURRENCYSTYLE.
     *                                   CASHCURRENCYSTYLE, STANDARDCURRENCYSTYLE.
     * @stable ICU 4.2
     */
    public static NumberFormat getInstance(ULocale desiredLocale, int choice) {
        if (choice < NUMBERSTYLE || choice > STANDARDCURRENCYSTYLE) {
            throw new IllegalArgumentException(
                "choice should be from NUMBERSTYLE to STANDARDCURRENCYSTYLE");
        }
//          if (shim == null) {
//              return createInstance(desiredLocale, choice);
//          } else {
//              // TODO: shims must call setLocale() on object they create
//              return getShim().createInstance(desiredLocale, choice);
//          }
        return getShim().createInstance(desiredLocale, choice);
    }

    // =======================privates===============================
    // Hook for service
    static NumberFormat createInstance(ULocale desiredLocale, int choice) {
        // If the choice is PLURALCURRENCYSTYLE, the pattern is not a single
        // pattern, it is a pattern set, so we do not need to get them here.
        // If the choice is ISOCURRENCYSTYLE, the pattern is the currrency
        // pattern in the locale but by replacing the single currency sign
        // with double currency sign.
        String pattern = getPattern(desiredLocale, choice);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(desiredLocale);

        // Here we assume that the locale passed in is in the canonical
        // form, e.g: pt_PT_@currency=PTE
        // For currency plural format, the pattern is get from
        // the locale (from CurrencyUnitPatterns) without override.
        if (choice == CURRENCYSTYLE || choice == ISOCURRENCYSTYLE || choice == ACCOUNTINGCURRENCYSTYLE
                || choice == CASHCURRENCYSTYLE || choice == STANDARDCURRENCYSTYLE) {
            String temp = symbols.getCurrencyPattern();
            if(temp!=null){
                pattern = temp;
            }
        }

        // replace single currency sign in the pattern with double currency sign
        // if the choice is ISOCURRENCYSTYLE.
        if (choice == ISOCURRENCYSTYLE) {
            pattern = pattern.replace("\u00A4", doubleCurrencyStr);
        }

        // Get the numbering system
        NumberingSystem ns = NumberingSystem.getInstance(desiredLocale);
        if ( ns == null ) {
            return null;
        }

        NumberFormat format;

        if ( ns != null && ns.isAlgorithmic()) {
            String nsDesc;
            String nsRuleSetGroup;
            String nsRuleSetName;
            ULocale nsLoc;
            int desiredRulesType = RuleBasedNumberFormat.NUMBERING_SYSTEM;

            nsDesc = ns.getDescription();
            int firstSlash = nsDesc.indexOf("/");
            int lastSlash = nsDesc.lastIndexOf("/");

            if ( lastSlash > firstSlash ) {
               String nsLocID = nsDesc.substring(0,firstSlash);
               nsRuleSetGroup = nsDesc.substring(firstSlash+1,lastSlash);
               nsRuleSetName = nsDesc.substring(lastSlash+1);

               nsLoc = new ULocale(nsLocID);
               if ( nsRuleSetGroup.equals("SpelloutRules")) {
                   desiredRulesType = RuleBasedNumberFormat.SPELLOUT;
               }
            } else {
                nsLoc = desiredLocale;
                nsRuleSetName = nsDesc;
            }

            RuleBasedNumberFormat r = new RuleBasedNumberFormat(nsLoc,desiredRulesType);
            r.setDefaultRuleSet(nsRuleSetName);
            format = r;
        } else {
            DecimalFormat f = new DecimalFormat(pattern, symbols, choice);
            // System.out.println("loc: " + desiredLocale + " choice: " + choice + " pat: " + pattern + " sym: " + symbols + " result: " + format);

            /*Bug 4408066
             Add codes for the new method getIntegerInstance() [Richard/GCL]
            */
            // TODO: revisit this -- this is almost certainly not the way we want
            // to do this.  aliu 1/6/2004
            if (choice == INTEGERSTYLE) {
                f.setMaximumFractionDigits(0);
                f.setDecimalSeparatorAlwaysShown(false);
                f.setParseIntegerOnly(true);
            }
            if (choice == CASHCURRENCYSTYLE) {
                f.setCurrencyUsage(CurrencyUsage.CASH);
            }
            if (choice == PLURALCURRENCYSTYLE) {
                f.setCurrencyPluralInfo(CurrencyPluralInfo.getInstance(desiredLocale));
            }
            format = f;
       }
        // TODO: the actual locale of the *pattern* may differ from that
        // for the *symbols*.  For now, we use the data for the symbols.
        // Revisit this.
        ULocale valid = symbols.getLocale(ULocale.VALID_LOCALE);
        ULocale actual = symbols.getLocale(ULocale.ACTUAL_LOCALE);
        format.setLocale(valid, actual);

        return format;
    }

    /**
     * Returns the pattern for the provided locale and choice.
     * @param forLocale the locale of the data.
     * @param choice the pattern format.
     * @return the pattern
     * @deprecated ICU 3.4 subclassers should override getPattern(ULocale, int) instead of this method.
     */
    @Deprecated
    protected static String getPattern(Locale forLocale, int choice) {
        return getPattern(ULocale.forLocale(forLocale), choice);
    }

    /**
     * Returns the pattern for the provided locale and choice.
     * @param forLocale the locale of the data.
     * @param choice the pattern format.
     * @return the pattern
     * @stable ICU 3.2
     */
    protected static String getPattern(ULocale forLocale, int choice) {
        return getPatternForStyle(forLocale, choice);
    }

    /**
     * Returns the pattern for the provided locale and choice.
     * @param forLocale the locale of the data.
     * @param choice the pattern format.
     * @return the pattern
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static String getPatternForStyle(ULocale forLocale, int choice) {
        NumberingSystem ns = NumberingSystem.getInstance(forLocale);
        String nsName = ns.getName();
        return getPatternForStyleAndNumberingSystem(forLocale, nsName, choice);
    }

    /**
     * Returns the pattern for the provided locale, numbering system, and choice.
     * @param forLocale the locale of the data.
     * @param nsName The name of the numbering system, like "latn".
     * @param choice the pattern format.
     * @return the pattern
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static String getPatternForStyleAndNumberingSystem(ULocale forLocale, String nsName, int choice) {
        /* for ISOCURRENCYSTYLE and PLURALCURRENCYSTYLE,
         * the pattern is the same as the pattern of CURRENCYSTYLE
         * but by replacing the single currency sign with
         * double currency sign or triple currency sign.
         */
        String patternKey = null;
        switch (choice) {
        case NUMBERSTYLE:
        case INTEGERSTYLE:
        case PLURALCURRENCYSTYLE:
            patternKey = "decimalFormat";
            break;
        case CURRENCYSTYLE:
            String cfKeyValue = forLocale.getKeywordValue("cf");
            patternKey = (cfKeyValue != null && cfKeyValue.equals("account")) ?
                    "accountingFormat" : "currencyFormat";
            break;
        case CASHCURRENCYSTYLE:
        case ISOCURRENCYSTYLE:
        case STANDARDCURRENCYSTYLE:
            patternKey = "currencyFormat";
            break;
        case PERCENTSTYLE:
            patternKey = "percentFormat";
            break;
        case SCIENTIFICSTYLE:
            patternKey = "scientificFormat";
            break;
        case ACCOUNTINGCURRENCYSTYLE:
            patternKey = "accountingFormat";
            break;
        default:
            assert false;
            patternKey = "decimalFormat";
            break;
        }

        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.
        getBundleInstance(ICUData.ICU_BASE_NAME, forLocale);

        String result = rb.findStringWithFallback(
                    "NumberElements/" + nsName + "/patterns/" + patternKey);
        if (result == null) {
            result = rb.getStringWithFallback("NumberElements/latn/patterns/" + patternKey);
        }

        return result;
    }

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
     */
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        ///CLOVER:OFF
        // we don't have serialization data for this format
        if (serialVersionOnStream < 1) {
            // Didn't have additional int fields, reassign to use them.
            maximumIntegerDigits = maxIntegerDigits;
            minimumIntegerDigits = minIntegerDigits;
            maximumFractionDigits = maxFractionDigits;
            minimumFractionDigits = minFractionDigits;
        }
        if (serialVersionOnStream < 2) {
            // Didn't have capitalizationSetting, set it to default
            capitalizationSetting = DisplayContext.CAPITALIZATION_NONE;
        }
        ///CLOVER:ON
        /*Bug 4185761
          Validate the min and max fields [Richard/GCL]
        */
        if (minimumIntegerDigits > maximumIntegerDigits ||
            minimumFractionDigits > maximumFractionDigits ||
            minimumIntegerDigits < 0 || minimumFractionDigits < 0) {
            throw new InvalidObjectException("Digit count range invalid");
        }
        serialVersionOnStream = currentSerialVersion;
    }

    /**
     * Write out the default serializable data, after first setting
     * the <code>byte</code> fields such as <code>maxIntegerDigits</code> to be
     * equal to the <code>int</code> fields such as <code>maximumIntegerDigits</code>
     * (or to <code>Byte.MAX_VALUE</code>, whichever is smaller), for compatibility
     * with the JDK 1.1 version of the stream format.
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

// Unused -- Alan 2003-05
//    /**
//     * Cache to hold the NumberPatterns of a Locale.
//     */
//    private static final Hashtable cachedLocaleData = new Hashtable(3);

      private static final char[] doubleCurrencySign = {0xA4, 0xA4};
      private static final String doubleCurrencyStr = new String(doubleCurrencySign);

    /*Bug 4408066
      Add Field for the new method getIntegerInstance() [Richard/GCL]
    */


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
     * @see #getMaximumIntegerDigits
     */
    private int    maximumIntegerDigits = 40;

    /**
     * The minimum number of digits allowed in the integer portion of a
     * number.  <code>minimumIntegerDigits</code> must be less than or equal to
     * <code>maximumIntegerDigits</code>.
     *
     * @serial
     * @see #getMinimumIntegerDigits
     */
    private int    minimumIntegerDigits = 1;

    /**
     * The maximum number of digits allowed in the fractional portion of a
     * number.  <code>maximumFractionDigits</code> must be greater than or equal to
     * <code>minimumFractionDigits</code>.
     *
     * @serial
     * @see #getMaximumFractionDigits
     */
    private int    maximumFractionDigits = 3;    // invariant, >= minFractionDigits

    /**
     * The minimum number of digits allowed in the fractional portion of a
     * number.  <code>minimumFractionDigits</code> must be less than or equal to
     * <code>maximumFractionDigits</code>.
     *
     * @serial
     * @see #getMinimumFractionDigits
     */
    private int    minimumFractionDigits = 0;

    /**
     * Currency object used to format currencies.  Subclasses may
     * ignore this if they are not currency formats.  This will be
     * null unless a subclass sets it to a non-null value.
     * @since ICU 2.6
     */
    private Currency currency;

    static final int currentSerialVersion = 2;

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
     *
     * <li><b>2</b>: adds capitalizationSetting.
     * </ul>
     * When streaming out a <code>NumberFormat</code>, the most recent format
     * (corresponding to the highest allowable <code>serialVersionOnStream</code>)
     * is always written.
     *
     * @serial
     */
    private int serialVersionOnStream = currentSerialVersion;

    // Removed "implements Cloneable" clause.  Needs to update serialization
    // ID for backward compatibility.
    private static final long serialVersionUID = -2308460125733713944L;

    /**
     * Empty constructor.  Public for API compatibility with historic versions of
     * {@link java.text.NumberFormat} which had public constructor even though this is
     * an abstract class.
     * @stable ICU 2.6
     */
    public NumberFormat() {
    }

    // new in ICU4J 3.6
    private boolean parseStrict;

    /*
     * Capitalization context setting, new in ICU 53
     * @serial
     */
    private DisplayContext capitalizationSetting = DisplayContext.CAPITALIZATION_NONE;

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
         * @stable ICU 64
         */
        public static final Field MEASURE_UNIT = new Field("measure unit");

        /**
         * @stable ICU 64
         */
        public static final Field COMPACT = new Field("compact");

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
        @Override
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
            if (this.getName().equals(MEASURE_UNIT.getName()))
                return MEASURE_UNIT;
            if (this.getName().equals(COMPACT.getName()))
                return COMPACT;

            throw new InvalidObjectException("An invalid object.");
        }
    }
}
