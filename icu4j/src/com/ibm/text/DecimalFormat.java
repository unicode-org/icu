/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/DecimalFormat.java,v $ 
 * $Date: 2000/06/01 23:52:17 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import java.text.Format;
import java.text.ParsePosition;
import java.text.FieldPosition;
import java.math.BigInteger;
import java.util.ResourceBundle;
import java.util.Locale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Hashtable;

/**
 * <code>DecimalFormat</code> is a concrete subclass of
 * <code>NumberFormat</code> that formats decimal numbers. It has a variety of
 * features designed to make it possible to parse and format numbers in any
 * locale, including support for Western, Arabic, or Indic digits.  It also
 * supports different flavors of numbers, including integers (123), fixed-point
 * numbers (123.4), scientific notation (1.23E4), percentages (12%), and
 * currency amounts ($123).  All of these flavors can be easily localized.
 *
 * <p><strong>This is an enhanced version of <code>DecimalFormat</code> that
 * is based on the standard version in the JDK.  New or changed functionality
 * is labeled
 * <strong><font face=helvetica color=red>NEW</font></strong> or
 * <strong><font face=helvetica color=red>CHANGED</font></strong>.</strong>
 *
 * <p>To obtain a <code>NumberFormat</code> for a specific locale (including the
 * default locale) call one of <code>NumberFormat</code>'s factory methods such
 * as <code>getInstance()</code>. Do not call the <code>DecimalFormat</code>
 * constructors directly, unless you know what you are doing, since the
 * <code>NumberFormat</code> factory methods may return subclasses other than
 * <code>DecimalFormat</code>. If you need to customize the format object, do
 * something like this:
 *
 * <blockquote><pre>
 * NumberFormat f = NumberFormat.getInstance(loc);
 * if (f instanceof DecimalFormat) {
 *     ((DecimalFormat) f).setDecimalSeparatorAlwaysShown(true);
 * }
 * </pre></blockquote>
 *
 * <p>A <code>DecimalFormat</code> comprises a <em>pattern</em> and a set of
 * <em>symbols</em>.  The pattern may be set directly using
 * <code>applyPattern()</code>, or indirectly using the API methods.  The
 * symbols are stored in a <code>DecimalFormatSymbols</code> object.  When using
 * the <code>NumberFormat</code> factory methods, the pattern and symbols are
 * read from localized <code>ResourceBundle</code>s in the package
 * <code>java.text.resource</code>.
 *
 * <p><strong>Example</strong>
 *
 * <blockquote><pre>
 * <strong>// Print out a number using the localized number, currency,
 * // and percent format for each locale</strong>
 * Locale[] locales = NumberFormat.getAvailableLocales();
 * double myNumber = -1234.56;
 * NumberFormat form;
 * for (int j=0; j<3; ++j) {
 *     System.out.println("FORMAT");
 *     for (int i = 0; i < locales.length; ++i) {
 *         if (locales[i].getCountry().length() == 0) {
 *            // Skip language-only locales
 *            continue;
 *         }
 *         System.out.print(locales[i].getDisplayName());
 *         switch (j) {
 *         case 0:
 *             form = NumberFormat.getInstance(locales[i]); break;
 *         case 1:
 *             form = NumberFormat.getCurrencyInstance(locales[i]); break;
 *         default:
 *             form = NumberFormat.getPercentInstance(locales[i]); break;
 *         }
 *         try {
 *             // Assume form is a DecimalFormat
 *             System.out.print(": " + ((DecimalFormat) form).toPattern()
 *                              + " -> " + form.format(myNumber));
 *         } catch (IllegalArgumentException e) {}
 *         try {
 *             System.out.println(" -> " + form.parse(form.format(myNumber)));
 *         } catch (ParseException e) {}
 *     }
 * }
 * </pre></blockquote>
 *
 * <p><strong>Notes</strong>
 *
 * <p>A <code>DecimalFormat</code> pattern contains a postive and negative
 * subpattern, for example, "#,##0.00;(#,##0.00)".  Each subpattern has a
 * prefix, numeric part, and suffix.  If there is no explicit negative
 * subpattern, the localized minus sign, typically '-', is prefixed to the
 * positive form. That is, "0.00" alone is equivalent to "0.00;-0.00".  If there
 * is an explicit negative subpattern, it serves only to specify the negative
 * prefix and suffix; the number of digits, minimal digits, and other
 * characteristics are all the same as the positive pattern. That means that
 * "#,##0.0#;(#)" has precisely the same result as "#,##0.0#;(#,##0.0#)".
 *
 * <p>The prefixes, suffixes, and various symbols used for infinity, digits,
 * thousands separators, decimal separators, etc. may be set to arbitrary
 * values, and they will appear properly during formatting.  However, care must
 * be taken that the symbols and strings do not conflict, or parsing will be
 * unreliable.  For example, either the positive and negative prefixes or the
 * suffixes must be distinct for <code>DecimalFormat.parse()</code> to be able
 * to distinguish positive from negative values.  Another example is that the
 * decimal separator and thousands separator should be distinct characters, or
 * parsing will be impossible.
 *
 * <p>The grouping separator is commonly used for thousands, but in some
 * countries it separates ten-thousands. The grouping size is a constant number
 * of digits between the grouping characters, such as 3 for 100,000,000 or 4 for
 * 1,0000,0000.
 * If you supply a pattern with multiple grouping characters, the interval
 * between the last one and the end of the integer determines the primary
 * grouping size, and the interval between the last two determines
 * the secondary grouping size (see below); all others are ignored.
 * So "#,##,###,####" == "###,###,####" == "##,#,###,####".
 *
 * <P>Some locales have two different grouping intervals:  One used for the
 * least significant integer digits (the primary grouping size), and
 * one used for all others (the secondary grouping size).  For example,
 * if the primary grouping interval is 3, and the secondary is 2, then
 * this corresponds to the pattern "#,##,##0", and the number 123456789
 * is formatted as "12,34,56,789".
 *
 * <p><code>DecimalFormat</code> parses all Unicode characters that represent
 * decimal digits, as defined by <code>Character.digit()</code>.  In addition,
 * <code>DecimalFormat</code> also recognizes as digits the ten consecutive
 * characters starting with the localized zero digit defined in the
 * <code>DecimalFormatSymbols</code> object.  During formatting, the
 * <code>DecimalFormatSymbols</code>-based digits are output.
 *
 * <p>Illegal patterns, such as "#.#.#" or "#.###,###", will cause
 * <code>DecimalFormat</code> to throw an <code>IllegalArgumentException</code>
 * with a message that describes the problem.
 *
 * <p>If <code>DecimalFormat.parse(String, ParsePosition)</code> fails to parse
 * a string, it returns <code>null</code> and leaves the parse position
 * unchanged.  The convenience method <code>DecimalFormat.parse(String)</code>
 * indicates parse failure by throwing a <code>ParseException</code>.
 *
 * <p><strong>Special Cases</strong>
 *
 * <p><code>NaN</code> is formatted as a single character, typically
 * <code>&#92;uFFFD</code>.  This character is determined by the
 * <code>DecimalFormatSymbols</code> object.  This is the only value for which
 * the prefixes and suffixes are not used.
 *
 * <p>Infinity is formatted as a single character, typically
 * <code>&#92;u221E</code>, with the positive or negative prefixes and suffixes
 * applied.  The infinity character is determined by the
 * <code>DecimalFormatSymbols</code> object.
 *
 * <p>
 * <strong><font face=helvetica color=red>NEW</font></strong>
 * <strong>Scientific Notation</strong>
 *
 * <p>Numbers in scientific notation are expressed as the product of a mantissa
 * and a power of ten, for example, 1234 can be expressed as 1.234 x 10^3.  The
 * mantissa is often in the range 1.0 <= x < 10.0, but it need not be.
 * <code>DecimalFormat</code> can be instructed to format and parse scientific
 * notation through the API or via a pattern.  In a pattern, the exponent
 * character immediately followed by one or more digit characters indicates
 * scientific notation.  Example: "0.###E0" formats the number 1234 as
 * "1.234E3".
 *
 * <ul>
 * <li>The number of digit characters after the exponent character gives the
 * minimum exponent digit count.  There is no maximum.  Negative exponents are
 * formatted using the localized minus sign, <em>not</em> the prefix and suffix
 * from the pattern.  This allows patterns such as "0.###E0 m/s".  To prefix
 * positive exponents with a localized plus sign, specify '+' between the
 * exponent and the digits: "0.###E+0" will produce formats "1E+1", "1E+0",
 * "1E-1", etc.  (In localized patterns, use the localized plus sign rather than
 * '+'.)
 *
 * <li>The minimum number of integer digits is achieved by adjusting the
 * exponent.  Example: 0.00123 formatted with "00.###E0" yields "12.3E-4".  This
 * only happens if there is no maximum number of integer digits.  If there is a
 * maximum, then the minimum number of integer digits is fixed at one.
 *
 * <li>The maximum number of integer digits, if present, specifies the exponent
 * grouping.  The most common use of this is to generate <em>engineering
 * notation</em>, in which the exponent is a multiple of three, e.g.,
 * "##0.###E0".  The number 12345 is formatted using "##0.####E0" as "12.345E3".
 *
 * <li>The number of significant digits is the sum of the <em>minimum
 * integer</em> and <em>maximum fraction</em> digits, and is unaffected by the
 * maximum integer digits.  If this sum is zero, then all significant digits are
 * shown.  The number of significant digits limits the total number of integer
 * and fraction digits that will be shown in the mantissa; it does not affect
 * parsing.  For example, 12345 formatted with "##0.##E0" is "12.3E3".
 *
 * <li>Exponential patterns may not contain grouping separators.
 * </ul>
 *
 * <p>
 * <strong><font face=helvetica color=red>NEW</font></strong>
 * <strong>Padding</strong>
 *
 * <p><code>DecimalFormat</code> supports padding the result of
 * <code>format()</code> to a specific width.  Padding may be specified either
 * through the API or through the pattern syntax.  In a pattern the pad escape
 * character, followed by a single pad character, causes padding to be parsed
 * and formatted.  The pad escape character is '*' in unlocalized patterns, and
 * can be localized using <code>DecimalFormatSymbols.setPadEscape()</code>.  For
 * example, <code>"$*x#,##0.00"</code> formats 123 to <code>"$xx123.00"</code>,
 * and 1234 to <code>"$1,234.00"</code>.
 *
 * <ul>
 * <li>When padding is in effect, the width of the positive subpattern,
 * including prefix and suffix, determines the format width.  For example, in
 * the pattern <code>"* #0 o''clock"</code>, the format width is 10.
 *
 * <li>Some parameters which usually do not matter have meaning when padding is
 * used, because the pattern width is significant with padding.  In the pattern
 * "^ ##,##,#,##0.##", the format width is 14.  The initial characters "##,##,"
 * do not affect the grouping size or maximum integer digits, but they do affect
 * the format width.
 *
 * <li>Padding may be inserted at one of four locations: before the prefix,
 * after the prefix, before the suffix, or after the suffix.  If padding is
 * specified in any other location, <code>DecimalFormat.applyPattern()</code>
 * throws an <code>IllegalArgumentException</code>.  If there is no prefix,
 * before the prefix and after the prefix are equivalent, likewise for the
 * suffix.
 *
 * <li>The pad character may not be a quote.
 * </ul>
 *
 * <p>
 * <strong><font face=helvetica color=red>NEW</font></strong>
 * <strong>Rounding</strong>
 *
 * <p><code>DecimalFormat</code> supports rounding to a specific increment.  For
 * example, 1230 rounded to the nearest 50 is 1250.  1.234 rounded to the
 * nearest 0.65 is 1.3.  The rounding increment may be specified through the API
 * or in a pattern.  To specify a rounding increment in a pattern, include the
 * increment in the pattern itself.  "#,#50" specifies a rounding increment of
 * 50.  "#,##0.05" specifies a rounding increment of 0.05.
 *
 * <ul>
 * <li>Rounding only affects the string produced by formatting.  It does
 * not affect parsing or change any numerical values.
 *
 * <li>A <em>rounding mode</em> determines how values are rounded; see the
 * <code>java.math.BigDecimal</code> documentation for a description of the
 * modes.  Rounding increments specified in patterns use the default mode,
 * <code>ROUND_HALF_EVEN</code>.
 *
 * <li>Some locales use rounding in their currency formats to reflect the
 * smallest currency denomination.
 *
 * <li>In a pattern, digits '1' through '9' specify rounding, but otherwise
 * behave identically to digit '0'.
 * </ul>
 *
 * <p><strong>Pattern Syntax</strong>
 * <pre>
 * pattern    := subpattern{';' subpattern}
 * subpattern := {prefix}number{suffix}
 * number     := integer{'.' fraction}{exponent}
 * prefix     := '&#92;u0000'..'&#92;uFFFD' - specialCharacters
 * suffix     := '&#92;u0000'..'&#92;uFFFD' - specialCharacters
 * integer    := '#'* '0'* '0'
 * fraction   := '0'* '#'*
 * exponent   := 'E' {'+'} '0'* '0'
 * padSpec    := '*' padChar
 * padChar    := '&#92;u0000'..'&#92;uFFFD' - quote
 * &#32;
 * Notation:
 *   X*       0 or more instances of X
 *   { X }    0 or 1 instances of X
 *   X..Y     any character from X up to Y, inclusive
 *   S - T    characters in S, except those in T
 * </pre>
 * The first subpattern is for positive numbers. The second (optional)
 * subpattern is for negative numbers.
 * 
 * <p>Not indicated in the BNF syntax above:
 * <ul><li>The grouping separator ',' can occur inside the integer portion between the
 * most significant digit and the least significant digit.
 *
 * <li><font color=red face=helvetica><strong>NEW</strong></font>
 *     Two grouping intervals are recognized: That between the
 *     decimal point and the first grouping symbol, and that
 *     between the first and second grouping symbols. These
 *     intervals are identical in most locales, but in some
 *     locales they differ. For example, the pattern
 *     &quot;#,##,###&quot; formats the number 123456789 as
 *     &quot;12,34,56,789&quot;.</li>
 * 
 * <li>
 * <strong><font face=helvetica color=red>NEW</font></strong>
 * The pad specifier <code>padSpec</code> may appear before the prefix,
 * after the prefix, before the suffix, after the suffix, or not at all.
 *
 * <li>
 * <strong><font face=helvetica color=red>NEW</font></strong>
 * In place of '0', the digits '1' through '9' may be used to
 * indicate a rounding increment.
 * </ul>
 *
 * <p><strong>Special Pattern Characters</strong>
 *
 * <p>Here are the special characters used in the pattern, with notes on their
 * usage.  Special characters must be quoted, unless noted otherwise, if they
 * are to appear in the prefix or suffix.  This does not apply to those listed
 * with location "prefix or suffix."  Such characters should only be quoted in
 * order to remove their special meaning.
 *
 * <p><table border=1>
 * <tr><th>Symbol<th>Location<th>Meaning</tr>
 * <tr><td>0-9<td>Number<td>Digit.
 *                  <strong><font face=helvetica color=red>NEW</font></strong>
 *                  '1' through '9' indicate rounding</tr>
 * <tr><td>#<td>Number<td>Digit, zero shows as absent</tr>
 * <tr><td>.<td>Number<td>Decimal separator or monetary decimal separator</tr>
 * <tr><td>,<td>Number<td>Grouping separator</tr>
 * <tr><td>E<td>Number
 *          <td>Separates mantissa and exponent in scientific notation.
 *              <em>Need not be quoted in prefix or suffix.</em></tr>
 * <tr><td><strong><font face=helvetica color=red>NEW</font></strong>
 *         +<td>Exponent
 *          <td>Prefix positive exponents with localized plus sign.
 *              <em>Need not be quoted in prefix or suffix.</em></tr>
 * <tr><td>;<td>Subpattern boundary
 *          <td>Separates positive and negative subpatterns</tr>
 * <tr><td>%<td>Prefix or suffix<td>Multiply by 100 and show as percentage</tr>
 * <tr><td>&#92;u2030<td>Prefix or suffix
 *                   <td>Multiply by 1000 and show as per mille</tr>
 * <tr><td>&#92;u00A4<td>Prefix or suffix
 *                   <td>Currency sign, replaced by currency symbol.  If
 *                   doubled, replaced by international currency symbol.
 *                   If present in a pattern, the monetary decimal separator
 *                   is used instead of the decimal separator.</tr>
 * <tr><td>'<td>Prefix or suffix
 *          <td>Used to quote special characters in a prefix or suffix,
 *              for example, <code>"'#'#"</code> formats 123 to
 *              <code>"#123"</code>.  To create a single quote
 *              itself, use two in a row: <code>"# o''clock"</code>.</tr>
 * <tr><td><strong><font face=helvetica color=red>NEW</font></strong>
 *         *<td>Prefix or suffix boundary
 *          <td>Pad escape, precedes pad character</tr>
 * </table>
 * </pre>
 *
 *
 * @see          java.text.Format
 * @see          NumberFormat
 * @version      1.48 09/21/98
 * @author       Mark Davis
 * @author       Alan Liu
 * */
public class DecimalFormat extends NumberFormat {

    /**
     * Create a DecimalFormat using the default pattern and symbols
     * for the default locale. This is a convenient way to obtain a
     * DecimalFormat when internationalization is not the main concern.
     * <p>
     * To obtain standard formats for a given locale, use the factory methods
     * on NumberFormat such as getNumberInstance. These factories will
     * return the most appropriate sub-class of NumberFormat for a given
     * locale.
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     */
    public DecimalFormat() {
        // [NEW]
        Locale def = Locale.getDefault();
        String pattern = getPattern(def, 0);
        // Always applyPattern after the symbols are set
        this.symbols = new DecimalFormatSymbols(def);
        applyPattern(pattern, false);
    }


    /**
     * Create a DecimalFormat from the given pattern and the symbols
     * for the default locale. This is a convenient way to obtain a
     * DecimalFormat when internationalization is not the main concern.
     * <p>
     * To obtain standard formats for a given locale, use the factory methods
     * on NumberFormat such as getNumberInstance. These factories will
     * return the most appropriate sub-class of NumberFormat for a given
     * locale.
     * @param pattern A non-localized pattern string.
     * @exception IllegalArgumentException if the given pattern is invalid.
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     */
    public DecimalFormat(String pattern) {
    // Always applyPattern after the symbols are set
        this.symbols = new DecimalFormatSymbols( Locale.getDefault() );
        applyPattern( pattern, false );
    }


    /**
     * Create a DecimalFormat from the given pattern and symbols.
     * Use this constructor when you need to completely customize the
     * behavior of the format.
     * <p>
     * To obtain standard formats for a given
     * locale, use the factory methods on NumberFormat such as
     * getInstance or getCurrencyInstance. If you need only minor adjustments
     * to a standard format, you can modify the format returned by
     * a NumberFormat factory method.
     * @param pattern a non-localized pattern string
     * @param symbols the set of symbols to be used
     * @exception IllegalArgumentException if the given pattern is invalid
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     * @see DecimalFormatSymbols
     */
    public DecimalFormat (String pattern, DecimalFormatSymbols symbols) {
        // Always applyPattern after the symbols are set
        this.symbols = (DecimalFormatSymbols)symbols.clone();
        applyPattern( pattern, false );
    }


    // Overrides
    public StringBuffer format(double number, StringBuffer result,
                               FieldPosition fieldPosition)
    {
        //FP:fieldPosition.setBeginIndex(0);
        //FP:fieldPosition.setEndIndex(0);

        if (Double.isNaN(number))
        {
            //FP:if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
            //FP:fieldPosition.setBeginIndex(result.length());

            result.append(symbols.getNaN());

            //FP:if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
            //FP:fieldPosition.setEndIndex(result.length());

            addPadding(result, false, false /*ignored*/);
            return result;
        }

        /* Detecting whether a double is negative is easy with the exception of
         * the value -0.0.  This is a double which has a zero mantissa (and
         * exponent), but a negative sign bit.  It is semantically distinct from
         * a zero with a positive sign bit, and this distinction is important
         * to certain kinds of computations.  However, it's a little tricky to
         * detect, since (-0.0 == 0.0) and !(-0.0 < 0.0).  How then, you may
         * ask, does it behave distinctly from +0.0?  Well, 1/(-0.0) ==
         * -Infinity.  Proper detection of -0.0 is needed to deal with the
         * issues raised by bugs 4106658, 4106667, and 4147706.  Liu 7/6/98.
         */
        boolean isNegative = (number < 0.0) || (number == 0.0 && 1/number < 0.0);
        if (isNegative) number = -number;

        // Do this BEFORE checking to see if value is infinite!
        if (multiplier != 1) number *= multiplier;

        // Apply rounding after multiplier
        if (roundingDouble > 0.0) {
            number = roundingDouble
                * round(number / roundingDouble, roundingMode, isNegative);
        }

        if (Double.isInfinite(number))
        {
            result.append(isNegative ? negativePrefix : positivePrefix);

            //FP:if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
            //FP:fieldPosition.setBeginIndex(result.length());

            result.append(symbols.getInfinity());

            //FP:if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
            //FP:eydfieldPosition.setEndIndex(result.length());

            result.append(isNegative ? negativeSuffix : positiveSuffix);

            addPadding(result, true, isNegative);
            return result;
        }

        // At this point we are guaranteed a nonnegative finite
        // number.
        synchronized(digitList) {
            digitList.set(number, useExponentialNotation ?
                      getMinimumIntegerDigits() + getMaximumFractionDigits() :
                      getMaximumFractionDigits(),
                      !useExponentialNotation);

            return subformat(result, fieldPosition, isNegative, false);
        }
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Round a double value to the nearest integer according to the
     * given mode.
     * @param a the absolute value of the number to be rounded
     * @param mode a BigDecimal rounding mode
     * @param isNegative true if the number to be rounded is negative
     * @return the absolute value of the rounded result
     */
    private static double round(double a, int mode, boolean isNegative) {
        switch (mode) {
        case java.math.BigDecimal.ROUND_CEILING:
            return isNegative ? Math.floor(a) : Math.ceil(a);
        case java.math.BigDecimal.ROUND_FLOOR:
            return isNegative ? Math.ceil(a) : Math.floor(a);
        case java.math.BigDecimal.ROUND_DOWN:
            return Math.floor(a);
        case java.math.BigDecimal.ROUND_UP:
            return Math.ceil(a);
        case java.math.BigDecimal.ROUND_HALF_EVEN:
            // We should be able to just return Math.rint(a), but this
            // doesn't work in some VMs.
            {
                double f = Math.floor(a);
                if ((a - f) != 0.5) {
                    return Math.rint(a);
                }
                f /= 2.0;
                return f == Math.floor(f) ? Math.floor(a) : (Math.floor(a) + 1.0);
            }
        case java.math.BigDecimal.ROUND_HALF_DOWN:
            return ((a - Math.floor(a)) <= 0.5) ? Math.floor(a) : Math.ceil(a);
        case java.math.BigDecimal.ROUND_HALF_UP:
            return ((a - Math.floor(a)) < 0.5) ? Math.floor(a) : Math.ceil(a);
        case java.math.BigDecimal.ROUND_UNNECESSARY:
            if (a != Math.floor(a)) {
                throw new ArithmeticException("Rounding necessary");
            }
            return a;
        default:
            throw new IllegalArgumentException("Invalid rounding mode: " + mode);
        }
    }

    public StringBuffer format(long number, StringBuffer result,
                               FieldPosition fieldPosition)
    {
        //FP:fieldPosition.setBeginIndex(0);
        //FP:fieldPosition.setEndIndex(0);

        // If we are to do rounding, we need to move into the BigDecimal
        // domain in order to do divide/multiply correctly.
        // [NEW]
        if (roundingIncrement != null) {
            return format(java.math.BigDecimal.valueOf(number), result, fieldPosition);
        }

        boolean isNegative = (number < 0);
        if (isNegative) number = -number;

        // In general, long values always represent real finite numbers, so
        // we don't have to check for +/- Infinity or NaN.  However, there
        // is one case we have to be careful of:  The multiplier can push
        // a number near MIN_VALUE or MAX_VALUE outside the legal range.  We
        // check for this before multiplying, and if it happens we use BigInteger
        // instead.
        // [NEW]
        if (multiplier != 1) {
            boolean tooBig = false;
            if (number < 0) { // This can only happen if number == Long.MIN_VALUE
                long cutoff = Long.MIN_VALUE / multiplier;
                tooBig = (number < cutoff);
            } else {
                long cutoff = Long.MAX_VALUE / multiplier;
                tooBig = (number > cutoff);
            }
            if (tooBig) {
                return format(BigInteger.valueOf(isNegative ? -number : number),
                              result, fieldPosition);
            }
        }

        number *= multiplier;
        synchronized(digitList) {
            digitList.set(number, useExponentialNotation ?
                          getMinimumIntegerDigits() + getMaximumFractionDigits() : 0);

            return subformat(result, fieldPosition, isNegative, true);
        }
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigInteger number.
     */
    public StringBuffer format(BigInteger number, StringBuffer result,
                               FieldPosition fieldPosition) {
        // If we are to do rounding, we need to move into the BigDecimal
        // domain in order to do divide/multiply correctly.
        if (roundingIncrement != null) {
            return format(new java.math.BigDecimal(number), result, fieldPosition);
        }

        if (multiplier != 1) {
            number = number.multiply(BigInteger.valueOf(multiplier));
        }

        // At this point we are guaranteed a nonnegative finite
        // number.
        synchronized(digitList) {
            digitList.set(number, useExponentialNotation ?
                          getMinimumIntegerDigits() + getMaximumFractionDigits() : 0);

            return subformat(result, fieldPosition, number.signum() < 0, false);
        }
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigDecimal number.
     */
    public StringBuffer format(java.math.BigDecimal number, StringBuffer result,
                               FieldPosition fieldPosition) {
        if (multiplier != 1) {
            number = number.multiply(java.math.BigDecimal.valueOf(multiplier));
        }

        if (roundingIncrement != null) {
            number = number.divide(roundingIncrement, 0, roundingMode)
                    .multiply(roundingIncrement);
        }

        // At this point we are guaranteed a nonnegative finite
        // number.
        synchronized(digitList) {
            digitList.set(number, useExponentialNotation ?
                      getMinimumIntegerDigits() + getMaximumFractionDigits() :
                      getMaximumFractionDigits(),
                      !useExponentialNotation);
            return subformat(result, fieldPosition, number.signum() < 0, false);
        }        
    }

    //This has been removed pending addition of com.ibm.math package to ICU4J
    /*
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Format a BigDecimal number.
     */
    public StringBuffer format(com.ibm.math.BigDecimal number, StringBuffer result,
                               FieldPosition fieldPosition) {
        /* This method is just a copy of the corresponding java.math.BigDecimal
         * method for now.  It isn't very efficient since it must create a
         * conversion object to do math on the rounding increment.  In the
         * future we may try to clean this up, or even better, limit our support
         * to just one flavor of BigDecimal.
         */
        if (multiplier != 1) {
            number = number.multiply(com.ibm.math.BigDecimal.valueOf(multiplier));
        }

        if (roundingIncrement != null) {
            com.ibm.math.BigDecimal ri = new com.ibm.math.BigDecimal(roundingIncrement);
            number = number.divide(ri, 0, roundingMode)
                    .multiply(ri);
        }

        // At this point we are guaranteed a nonnegative finite
        // number.
        synchronized(digitList) {
            digitList.set(number, useExponentialNotation ?
                      getMinimumIntegerDigits() + getMaximumFractionDigits() :
                      getMaximumFractionDigits(),
                      !useExponentialNotation);
            return subformat(result, fieldPosition, number.signum() < 0, false);
        }        
    }

    /**
     * Return true if a grouping separator belongs at the given
     * position, based on whether grouping is in use and the values of
     * the primary and secondary grouping interval.
     * @param pos the number of integer digits to the right of
     * the current position.  Zero indicates the position after the
     * rightmost integer digit.
     * @return true if a grouping character belongs at the current
     * position.
     */
    private boolean isGroupingPosition(int pos) {
        boolean result = false;
        if (isGroupingUsed() && (pos > 0) && (groupingSize > 0)) {
            if ((groupingSize2 > 0) && (pos > groupingSize)) {
                result = ((pos - groupingSize) % groupingSize2) == 0;
            } else {
                result = pos % groupingSize == 0;
            }
        }
        return result;
    }

    /**
     * Complete the formatting of a finite number.  On entry, the digitList must
     * be filled in with the correct digits.
     */
    private StringBuffer subformat(StringBuffer result, FieldPosition fieldPosition,
                   boolean isNegative, boolean isInteger)
    {
        // NOTE: This isn't required anymore because DigitList takes care of this.
        //
        //  // The negative of the exponent represents the number of leading
        //  // zeros between the decimal and the first non-zero digit, for
        //  // a value < 0.1 (e.g., for 0.00123, -fExponent == 2).  If this
        //  // is more than the maximum fraction digits, then we have an underflow
        //  // for the printed representation.  We recognize this here and set
        //  // the DigitList representation to zero in this situation.
        //
        //  if (-digitList.decimalAt >= getMaximumFractionDigits())
        //  {
        //      digitList.count = 0;
        //  }

        int i;
        char zero = symbols.getZeroDigit();
        int zeroDelta = zero - '0'; // '0' is the DigitList representation of zero
        char grouping = symbols.getGroupingSeparator();
        char decimal = isCurrencyFormat ?
            symbols.getMonetaryDecimalSeparator() :
            symbols.getDecimalSeparator();
        int maxIntDig = getMaximumIntegerDigits();
        int minIntDig = getMinimumIntegerDigits();

        /* Per bug 4147706, DecimalFormat must respect the sign of numbers which
         * format as zero.  This allows sensible computations and preserves
         * relations such as signum(1/x) = signum(x), where x is +Infinity or
         * -Infinity.  Prior to this fix, we always formatted zero values as if
         * they were positive.  Liu 7/6/98.
         */
        if (digitList.isZero())
        {
            digitList.decimalAt = 0; // Normalize
        }

        result.append(isNegative ? negativePrefix : positivePrefix);

        if (useExponentialNotation)
        {
            // Record field information for caller.
            //FP:if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
            //FP:{
                //FP:fieldPosition.setBeginIndex(result.length());
                //FP:fieldPosition.setEndIndex(-1);
            //FP:}
            //FP:else if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD)
            //FP:{
                //FP:fieldPosition.setBeginIndex(-1);
            //FP:}

            // Minimum integer digits are handled in exponential format by
            // adjusting the exponent.  For example, 0.01234 with 3 minimum
            // integer digits is "123.4E-4".

            // Maximum integer digits are interpreted as indicating the
            // repeating range.  This is useful for engineering notation, in
            // which the exponent is restricted to a multiple of 3.  For
            // example, 0.01234 with 3 maximum integer digits is "12.34e-3".
            // If maximum integer digits are defined and are larger than
            // minimum integer digits, then minimum integer digits are
            // ignored.

            int exponent = digitList.decimalAt;
            if (maxIntDig > 1 && maxIntDig != minIntDig) {
                // A exponent increment is defined; adjust to it.
                exponent = (exponent > 0) ? (exponent - 1) / maxIntDig
                                          : (exponent / maxIntDig) - 1;
                exponent *= maxIntDig;
            } else {
                // No exponent increment is defined; use minimum integer digits.
                // If none is specified, as in "#E0", generate 1 integer digit.
                exponent -= (minIntDig > 0 || getMinimumFractionDigits() > 0)
                    ? minIntDig : 1;
            }

            // We now output a minimum number of digits, and more if there
            // are more digits, up to the maximum number of digits.  We
            // place the decimal point after the "integer" digits, which
            // are the first (decimalAt - exponent) digits.
            int minimumDigits = minIntDig
                                + getMinimumFractionDigits();
            // The number of integer digits is handled specially if the number
            // is zero, since then there may be no digits.
            int integerDigits = digitList.isZero() ? minIntDig :
                digitList.decimalAt - exponent;
            int totalDigits = digitList.count;
            if (minimumDigits > totalDigits) totalDigits = minimumDigits;
            if (integerDigits > totalDigits) totalDigits = integerDigits;

            for (i=0; i<totalDigits; ++i)
            {
                if (i == integerDigits)
                {
                    // Record field information for caller.
                    //FP:if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
                    //FP:fieldPosition.setEndIndex(result.length());

                    result.append(decimal);

                    // Record field information for caller.
                    //FP:if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD)
                    //FP:fieldPosition.setBeginIndex(result.length());
                }
                result.append((i < digitList.count) ?
                          (char)(digitList.digits[i] + zeroDelta) :
                          zero);
            }

            // Record field information
            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
            {
                //FP:if (fieldPosition.getEndIndex() < 0)
                //FP:fieldPosition.setEndIndex(result.length());
            }
            else if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD)
            {
                //FP:if (fieldPosition.getBeginIndex() < 0)
                //FP:fieldPosition.setBeginIndex(result.length());
                //FP:fieldPosition.setEndIndex(result.length());
            }

            // The exponent is output using the pattern-specified minimum
            // exponent digits.  There is no maximum limit to the exponent
            // digits, since truncating the exponent would result in an
            // unacceptable inaccuracy.
            result.append(symbols.getExponentSeparator());

            // For zero values, we force the exponent to zero.  We
            // must do this here, and not earlier, because the value
            // is used to determine integer digit count above.
            if (digitList.isZero()) exponent = 0;

            boolean negativeExponent = exponent < 0;
            if (negativeExponent) {
                exponent = -exponent;
                result.append(symbols.getMinusSign());
            } else if (exponentSignAlwaysShown) {
                result.append(symbols.getPlusSign());
            }
            digitList.set(exponent);
            for (i=digitList.decimalAt; i<minExponentDigits; ++i) result.append(zero);
            for (i=0; i<digitList.decimalAt; ++i)
            {
                result.append((i < digitList.count) ?
                          (char)(digitList.digits[i] + zeroDelta) : zero);
            }
        }
        else
        {
            // Record field information for caller.
            //FP:if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
            //FP:fieldPosition.setBeginIndex(result.length());

            // Output the integer portion.  Here 'count' is the total
            // number of integer digits we will display, including both
            // leading zeros required to satisfy getMinimumIntegerDigits,
            // and actual digits present in the number.
            int count = minIntDig;
            int digitIndex = 0; // Index into digitList.fDigits[]
            if (digitList.decimalAt > 0 && count < digitList.decimalAt)
                count = digitList.decimalAt;

            // Handle the case where getMaximumIntegerDigits() is smaller
            // than the real number of integer digits.  If this is so, we
            // output the least significant max integer digits.  For example,
            // the value 1997 printed with 2 max integer digits is just "97".

            if (count > maxIntDig)
            {
                count = maxIntDig;
                digitIndex = digitList.decimalAt - count;
            }

            int sizeBeforeIntegerPart = result.length();
            for (i=count-1; i>=0; --i)
            {
                if (i < digitList.decimalAt && digitIndex < digitList.count)
                {
                    // Output a real digit
                    result.append((char)(digitList.digits[digitIndex++] + zeroDelta));
                }
                else
                {
                    // Output a leading zero
                    result.append(zero);
                }

                // Output grouping separator if necessary.
                if (isGroupingPosition(i)) {
                    result.append(grouping);
                }
            }

            // Record field information for caller.
            //FP:if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD)
            //FP:fieldPosition.setEndIndex(result.length());

            // Determine whether or not there are any printable fractional
            // digits.  If we've used up the digits we know there aren't.
            boolean fractionPresent = (getMinimumFractionDigits() > 0) ||
            (!isInteger && digitIndex < digitList.count);

            // If there is no fraction present, and we haven't printed any
            // integer digits, then print a zero.  Otherwise we won't print
            // _any_ digits, and we won't be able to parse this string.
            if (!fractionPresent && result.length() == sizeBeforeIntegerPart)
                result.append(zero);

            // Output the decimal separator if we always do so.
            if (decimalSeparatorAlwaysShown || fractionPresent)
                result.append(decimal);

            // Record field information for caller.
            //FP:if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD)
            //FP:fieldPosition.setBeginIndex(result.length());

            for (i=0; i < getMaximumFractionDigits(); ++i)
            {
                // Here is where we escape from the loop.  We escape if we've output
                // the maximum fraction digits (specified in the for expression above).
                // We also stop when we've output the minimum digits and either:
                // we have an integer, so there is no fractional stuff to display,
                // or we're out of significant digits.
                if (i >= getMinimumFractionDigits() &&
                    (isInteger || digitIndex >= digitList.count))
                    break;

                // Output leading fractional zeros.  These are zeros that come after
                // the decimal but before any significant digits.  These are only
                // output if abs(number being formatted) < 1.0.
                if (-1-i > (digitList.decimalAt-1))
                {
                    result.append(zero);
                    continue;
                }

                // Output a digit, if we have any precision left, or a
                // zero if we don't.  We don't want to output noise digits.
                if (!isInteger && digitIndex < digitList.count)
                {
                    result.append((char)(digitList.digits[digitIndex++] + zeroDelta));
                }
                else
                {
                    result.append(zero);
                }
            }

            // Record field information for caller.
            //FP:if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD)
            //FP:fieldPosition.setEndIndex(result.length());
        }

        result.append(isNegative ? negativeSuffix : positiveSuffix);

        // [NEW]
        addPadding(result, true, isNegative);
        return result;
    }

    // [NEW]
    private final void addPadding(StringBuffer result, boolean hasAffixes,
                                  boolean isNegative) {
        if (formatWidth > 0) {
            int len = formatWidth - result.length();
            if (len > 0) {
                char[] padding = new char[len];
                for (int i=0; i<len; ++i) {
                    padding[i] = pad;
                }
                switch (padPosition) {
                case PAD_AFTER_PREFIX:
                    if (hasAffixes) {
                        result.insert(isNegative ? negativePrefix.length()
                                      : positivePrefix.length(),
                                      padding);
                        break;
                    } // else fall through to next case
                case PAD_BEFORE_PREFIX:
                    result.insert(0, padding);
                    break;
                case PAD_BEFORE_SUFFIX:
                    if (hasAffixes) {
                        result.insert(result.length() -
                                      (isNegative ? negativeSuffix.length()
                                       : positiveSuffix.length()),
                                      padding);
                        break;
                    } // else fall through to next case
                case PAD_AFTER_SUFFIX:
                    result.append(padding);
                    break;
                }
            }
        }
    }

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong>
     * Parse the given string, returning a <code>Number</code> object to
     * represent the parsed value.  <code>Double</code> objects are returned to
     * represent non-integral values which cannot be stored in a
     * <code>BigDecimal</code>.  These are <code>NaN</code>, infinity,
     * -infinity, and -0.0.  All other values are returned as <code>Long</code>,
     * <code>BigInteger</code>, or <code>BigDecimal</code> values, in that order
     * of preference.  If the parse fails, null is returned.
     * @param text the string to be parsed
     * @param parsePosition defines the position where parsing is to begin,
     * and upon return, the position where parsing left off.  If the position
     * has not changed upon return, then parsing failed.
     * @return a <code>Number</code> object with the parsed value or
     * <code>null</code> if the parse failed
     */
    public Number parse(String text, ParsePosition parsePosition)
    {
        // Skip padding characters, if any
        int backup;
        int i = backup = parsePosition.getIndex();
        if (formatWidth > 0) {
            while (i < text.length() && text.charAt(i) == pad) {
                ++i;
            }
            parsePosition.setIndex(i);
        }

        // Handle NaN as a special case
        if (text.regionMatches(parsePosition.getIndex(), symbols.getNaN(),
                               0, symbols.getNaN().length())) {
            parsePosition.setIndex(parsePosition.getIndex()
                                   + symbols.getNaN().length());
            return new Double(Double.NaN);
        }

        boolean[] status = new boolean[STATUS_LENGTH];
        if (!subparse(text, parsePosition, digitList, false, status)) {
            parsePosition.setIndex(backup);
            return null;
        } else if (formatWidth < 0) {
            i = parsePosition.getIndex();
            while (i < text.length() && text.charAt(i) == pad) {
                ++i;
            }
            parsePosition.setIndex(i);
        }

        // Handle infinity
        if (status[STATUS_INFINITE]) {
            return new Double(status[STATUS_POSITIVE]
                              ? Double.POSITIVE_INFINITY
                              : Double.NEGATIVE_INFINITY);
        }

        // Handle -0.0
        if (!status[STATUS_POSITIVE] && digitList.isZero()) {
            return new Double(-0.0);
        }

        // Do as much of the multiplier conversion as possible without
        // losing accuracy.
        int mult = multiplier; // Don't modify this.multiplier
        while (mult % 10 == 0) {
            --digitList.decimalAt;
            mult /= 10;
        }

        // Handle integral values
        if (mult == 1 && digitList.isIntegral()) {
            BigInteger n = digitList.getBigInteger(status[STATUS_POSITIVE]);
            return (n.bitLength() < 64)
                ? (Number) new Long(n.longValue()) 
                : (Number) n;
        }

        // Handle non-integral values
        java.math.BigDecimal n = digitList.getBigDecimal(status[STATUS_POSITIVE]);
        if (mult != 1) {
            n = n.divide(java.math.BigDecimal.valueOf(mult),
                         java.math.BigDecimal.ROUND_HALF_EVEN);
        }
        return n;
    }

    private static final int STATUS_INFINITE = 0;
    private static final int STATUS_POSITIVE = 1;
    private static final int STATUS_LENGTH   = 2;

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong>
     * Parse the given text into a number.  The text is parsed beginning at
     * parsePosition, until an unparseable character is seen.
     * @param text The string to parse.
     * @param parsePosition The position at which to being parsing.  Upon
     * return, the first unparseable character.
     * @param digits The DigitList to set to the parsed value.
     * @param isExponent If true, parse an exponent.  This means no
     * infinite values and integer only.
     * @param status Upon return contains boolean status flags indicating
     * whether the value was infinite and whether it was positive.
     */
    private final boolean subparse(String text, ParsePosition parsePosition,
                   DigitList digits, boolean isExponent,
                   boolean status[])
    {
        int position = parsePosition.getIndex();
        int oldStart = parsePosition.getIndex();
        int backup;

        // check for positivePrefix; take longest
        boolean gotPositive = text.regionMatches(position,positivePrefix,0,
                                                 positivePrefix.length());
        boolean gotNegative = text.regionMatches(position,negativePrefix,0,
                                                 negativePrefix.length());
        if (gotPositive && gotNegative) {
            if (positivePrefix.length() > negativePrefix.length())
                gotNegative = false;
            else if (positivePrefix.length() < negativePrefix.length())
                gotPositive = false;
        }
        if (gotPositive) {
            position += positivePrefix.length();
        } else if (gotNegative) {
            position += negativePrefix.length();
        } else {
            //PP:parsePosition.errorIndex = position;
            return false;
        }
        // process digits or Inf, find decimal position
        status[STATUS_INFINITE] = false;
        if (!isExponent && text.regionMatches(position,symbols.getInfinity(),0,
                          symbols.getInfinity().length()))
        {
            position += symbols.getInfinity().length();
            status[STATUS_INFINITE] = true;
        } else {
            // We now have a string of digits, possibly with grouping symbols,
            // and decimal points.  We want to process these into a DigitList.
            // We don't want to put a bunch of leading zeros into the DigitList
            // though, so we keep track of the location of the decimal point,
            // put only significant digits into the DigitList, and adjust the
            // exponent as needed.

            digits.decimalAt = digits.count = 0;
            char zero = symbols.getZeroDigit();
            char decimal = isCurrencyFormat ?
            symbols.getMonetaryDecimalSeparator() : symbols.getDecimalSeparator();
            char grouping = symbols.getGroupingSeparator();
            String exponentSep = symbols.getExponentSeparator();
            boolean sawDecimal = false;
            boolean sawExponent = false;
            boolean sawDigit = false;
            int exponent = 0; // Set to the exponent value, if any
            int digit = 0;

            // We have to track digitCount ourselves, because digits.count will
            // pin when the maximum allowable digits is reached.
            int digitCount = 0;

            backup = -1;
            for (; position < text.length(); ++position)
            {
                char ch = text.charAt(position);

                /* We recognize all digit ranges, not only the Latin digit range
                 * '0'..'9'.  We do so by using the Character.digit() method,
                 * which converts a valid Unicode digit to the range 0..9.
                 *
                 * The character 'ch' may be a digit.  If so, place its value
                 * from 0 to 9 in 'digit'.  First try using the locale digit,
                 * which may or MAY NOT be a standard Unicode digit range.  If
                 * this fails, try using the standard Unicode digit ranges by
                 * calling Character.digit().  If this also fails, digit will
                 * have a value outside the range 0..9.
                 */
                digit = ch - zero;
                if (digit < 0 || digit > 9) digit = Character.digit(ch, 10);

                if (digit == 0)
                {
                    // Cancel out backup setting (see grouping handler below)
                    backup = -1; // Do this BEFORE continue statement below!!!
                    sawDigit = true;

                    // Handle leading zeros
                    if (digits.count == 0)
                    {
                        // Ignore leading zeros in integer part of number.
                        if (!sawDecimal) continue;

                        // If we have seen the decimal, but no significant digits yet,
                        // then we account for leading zeros by decrementing the
                        // digits.decimalAt into negative values.
                        --digits.decimalAt;
                    }
                    else
                    {
                        ++digitCount;
                        digits.append((char)(digit + '0'));
                    }
                }
                else if (digit > 0 && digit <= 9) // [sic] digit==0 handled above
                {
                    sawDigit = true;
                    ++digitCount;
                    digits.append((char)(digit + '0'));

                    // Cancel out backup setting (see grouping handler below)
                    backup = -1;
                }
                else if (!isExponent && ch == decimal)
                {
                    // If we're only parsing integers, or if we ALREADY saw the
                    // decimal, then don't parse this one.
                    if (isParseIntegerOnly() || sawDecimal) break;
                    digits.decimalAt = digitCount; // Not digits.count!
                    sawDecimal = true;
                }
                else if (!isExponent && ch == grouping && isGroupingUsed())
                {
                    if (sawDecimal) {
                        break;
                    }
                    // Ignore grouping characters, if we are using them, but require
                    // that they be followed by a digit.  Otherwise we backup and
                    // reprocess them.
                    backup = position;
                }
                else if (!isExponent && !sawExponent &&
                         text.regionMatches(position, exponentSep,
                                            0, exponentSep.length()))
                {
                    // Parse sign, if present
                    boolean negExp = false;
                    int pos = position + exponentSep.length();
                    if (pos < text.length()) {
                        ch = text.charAt(pos);
                        if (ch == symbols.getPlusSign()) {
                            ++pos;
                        } else if (ch == symbols.getMinusSign()) {
                            ++pos;
                            negExp = true;
                        }
                    }

                    DigitList exponentDigits = new DigitList();
                    exponentDigits.count = 0;
                    while (pos < text.length()) {
                        digit = text.charAt(pos) - zero;
                        if (digit < 0 || digit > 9) {
                            digit = Character.digit(ch, 10);
                        }
                        if (digit >= 0 && digit <= 9) {
                            exponentDigits.append((char)(digit + '0'));
                            ++pos;
                        } else {
                            break;
                        }
                    }
                    
                    if (exponentDigits.count > 0) {
                        exponentDigits.decimalAt = exponentDigits.count;
                        exponent = (int) exponentDigits.getLong();
                        if (negExp) {
                            exponent = -exponent;
                        }
                        position = pos; // Advance past the exponent
                        sawExponent = true;
                    }

                    break; // Whether we fail or succeed, we exit this loop
                }
                else break;
            }

            if (backup != -1) position = backup;

            // If there was no decimal point we have an integer
            if (!sawDecimal) digits.decimalAt = digitCount; // Not digits.count!

            // Adjust for exponent, if any
            digits.decimalAt += exponent;

            // If none of the text string was recognized.  For example, parse
            // "x" with pattern "#0.00" (return index and error index both 0)
            // parse "$" with pattern "$#0.00". (return index 0 and error index
            // 1).
            if (!sawDigit && digitCount == 0) {
                parsePosition.setIndex(oldStart);
                //PP:parsePosition.errorIndex = oldStart;
                return false;
            }
        }

        // check for positiveSuffix
        if (gotPositive)
            gotPositive = text.regionMatches(position,positiveSuffix,0,
                                             positiveSuffix.length());
        if (gotNegative)
            gotNegative = text.regionMatches(position,negativeSuffix,0,
                                             negativeSuffix.length());

        // if both match, take longest
        if (gotPositive && gotNegative) {
            if (positiveSuffix.length() > negativeSuffix.length())
                gotNegative = false;
            else if (positiveSuffix.length() < negativeSuffix.length())
                gotPositive = false;
        }

        // fail if neither or both
        if (gotPositive == gotNegative) {
            //PP:parsePosition.errorIndex = position;
            return false;
        }

        parsePosition.setIndex(position +
            (gotPositive ? positiveSuffix.length() : negativeSuffix.length())); // mark success!

        status[STATUS_POSITIVE] = gotPositive;
        if (parsePosition.getIndex() == oldStart) {
            //PP:parsePosition.errorIndex = position;
            return false;
        }
        return true;
    }

    /**
     * Returns the decimal format symbols, which is generally not changed
     * by the programmer or user.
     * @return desired DecimalFormatSymbols
     * @see DecimalFormatSymbols
     */
    public DecimalFormatSymbols getDecimalFormatSymbols() {
        try {
            // don't allow multiple references
            return (DecimalFormatSymbols) symbols.clone();
        } catch (Exception foo) {
            return null; // should never happen
        }
    }


    /**
     * Sets the decimal format symbols, which is generally not changed
     * by the programmer or user.
     * @param newSymbols desired DecimalFormatSymbols
     * @see DecimalFormatSymbols
     */
    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        try {
            // don't allow multiple references
            symbols = (DecimalFormatSymbols) newSymbols.clone();
        } catch (Exception foo) {
            // should never happen
        }
    }

    /**
     * Get the positive prefix.
     * <P>Examples: +123, $123, sFr123
     */
    public String getPositivePrefix () {
        return positivePrefix;
    }

    /**
     * Set the positive prefix.
     * <P>Examples: +123, $123, sFr123
     */
    public void setPositivePrefix (String newValue) {
        positivePrefix = newValue;
    }

    /**
     * Get the negative prefix.
     * <P>Examples: -123, ($123) (with negative suffix), sFr-123
     */
    public String getNegativePrefix () {
        return negativePrefix;
    }

    /**
     * Set the negative prefix.
     * <P>Examples: -123, ($123) (with negative suffix), sFr-123
     */
    public void setNegativePrefix (String newValue) {
        negativePrefix = newValue;
    }

    /**
     * Get the positive suffix.
     * <P>Example: 123%
     */
    public String getPositiveSuffix () {
        return positiveSuffix;
    }

    /**
     * Set the positive suffix.
     * <P>Example: 123%
     */
    public void setPositiveSuffix (String newValue) {
        positiveSuffix = newValue;
    }

    /**
     * Get the negative suffix.
     * <P>Examples: -123%, ($123) (with positive suffixes)
     */
    public String getNegativeSuffix () {
        return negativeSuffix;
    }

    /**
     * Set the positive suffix.
     * <P>Examples: 123%
     */
    public void setNegativeSuffix (String newValue) {
        negativeSuffix = newValue;
    }

    /**
     * Get the multiplier for use in percent, permill, etc.
     * For a percentage, set the suffixes to have "%" and the multiplier to be 100.
     * (For Arabic, use arabic percent symbol).
     * For a permill, set the suffixes to have "\u2031" and the multiplier to be 1000.
     * <P>Examples: with 100, 1.23 -> "123", and "123" -> 1.23
     */
    public int getMultiplier () {
        return multiplier;
    }

    /**
     * Set the multiplier for use in percent, permill, etc.
     * For a percentage, set the suffixes to have "%" and the multiplier to be 100.
     * (For Arabic, use arabic percent symbol).
     * For a permill, set the suffixes to have "\u2031" and the multiplier to be 1000.
     * <P>Examples: with 100, 1.23 -> "123", and "123" -> 1.23
     */
    public void setMultiplier (int newValue) {
        if (newValue <= 0) {
            throw new IllegalArgumentException("Bad multiplier: " + newValue);
        }
        multiplier = newValue;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Get the rounding increment.
     * @return A positive rounding increment, or <code>null</code> if rounding
     * is not in effect.
     * @see #setRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     */
    public java.math.BigDecimal getRoundingIncrement() {
        return roundingIncrement;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the rounding increment.  This method also controls whether
     * rounding is enabled.
     * @param newValue A positive rounding increment, or <code>null</code> or
     * <code>BigDecimal(0.0)</code> to disable rounding.
     * @exception IllegalArgumentException if <code>newValue</code> is < 0.0
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     */
    public void setRoundingIncrement(java.math.BigDecimal newValue) {
        int i = newValue == null
                ? 0 : newValue.compareTo(java.math.BigDecimal.valueOf(0));
        if (i < 0) {
            throw new IllegalArgumentException("Illegal rounding increment");
        }
        if (i == 0) {
            roundingIncrement = null;
            roundingDouble = 0.0;
        } else {
            roundingIncrement = newValue;
            roundingDouble = newValue.doubleValue();
        }
    }
    
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the rounding increment.  This method also controls whether
     * rounding is enabled.
     * @param newValue A positive rounding increment, or 0.0 to disable
     * rounding.
     * @exception IllegalArgumentException if <code>newValue</code> is < 0.0
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     */
    public void setRoundingIncrement(double newValue) {
        if (newValue < 0.0) {
            throw new IllegalArgumentException("Illegal rounding increment");
        }
        roundingDouble = newValue;
        roundingIncrement = (newValue > 0.0)
            ? new java.math.BigDecimal(String.valueOf(newValue)) : null;
    }
    
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Get the rounding mode.
     * @return A rounding mode, between <code>BigDecimal.ROUND_UP</code>
     * and <code>BigDecimal.ROUND_UNNECESSARY</code>.
     * @see #setRoundingIncrement
     * @see #getRoundingIncrement
     * @see #setRoundingMode
     * @see java.math.BigDecimal
     */
    public int getRoundingMode() {
        return roundingMode;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the rounding mode.  This has no effect unless the rounding
     * increment is greater than zero.
     * @param roundingMode A rounding mode, between
     * <code>BigDecimal.ROUND_UP</code> and
     * <code>BigDecimal.ROUND_UNNECESSARY</code>.
     * @exception IllegalArgumentException if <code>roundingMode</code>
     * is unrecognized.
     * @see #setRoundingIncrement
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @see java.math.BigDecimal
     */
    public void setRoundingMode(int roundingMode) {
        if (roundingMode < java.math.BigDecimal.ROUND_UP
            || roundingMode > java.math.BigDecimal.ROUND_UNNECESSARY) {
            throw new IllegalArgumentException("Invalid rounding mode: "
                                               + roundingMode);
        }
        this.roundingMode = roundingMode;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Get the width to which the output of <code>format()</code> is padded.
     * @return the format width, or zero if no padding is in effect
     * @see #setFormatWidth
     * @see #getPadCharacter
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     */
    public int getFormatWidth() {
        return formatWidth;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the width to which the output of <code>format()</code> is padded.
     * This method also controls whether padding is enabled.
     * @param width the width to which to pad the result of
     * <code>format()</code>, or zero to disable padding
     * @exception IllegalArgumentException if <code>width</code> is < 0
     * @see #getFormatWidth
     * @see #getPadCharacter
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     */
    public void setFormatWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("Illegal format width");
        }
        formatWidth = width;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Get the character used to pad to the format width.  The default is ' '.
     * @return the pad character
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     */
    public char getPadCharacter() {
        return pad;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the character used to pad to the format width.  This has no effect
     * unless padding is enabled.
     * @param padChar the pad character
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #getPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     */
    public void setPadCharacter(char padChar) {
        pad = padChar;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Get the position at which padding will take place.  This is the location
     * at which padding will be inserted if the result of <code>format()</code>
     * is shorter than the format width.
     * @return the pad position, one of <code>PAD_BEFORE_PREFIX</code>,
     * <code>PAD_AFTER_PREFIX</code>, <code>PAD_BEFORE_SUFFIX</code>, or
     * <code>PAD_AFTER_SUFFIX</code>.
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadCharacter
     * @see #setPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @see #PAD_AFTER_SUFFIX
     */
    public int getPadPosition() {
        return padPosition;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the position at which padding will take place.  This is the location
     * at which padding will be inserted if the result of <code>format()</code>
     * is shorter than the format width.  This has no effect unless padding is
     * enabled.
     * @param padPos the pad position, one of <code>PAD_BEFORE_PREFIX</code>,
     * <code>PAD_AFTER_PREFIX</code>, <code>PAD_BEFORE_SUFFIX</code>, or
     * <code>PAD_AFTER_SUFFIX</code>.
     * @exception IllegalArgumentException if the pad position in
     * unrecognized
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadCharacter
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @see #PAD_AFTER_SUFFIX
     */
    public void setPadPosition(int padPos) {
        if (padPos < PAD_BEFORE_PREFIX || padPos > PAD_AFTER_SUFFIX) {
            throw new IllegalArgumentException("Illegal pad position");
        }
        padPosition = padPos;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Return whether or not scientific notation is used.
     * @return true if this object formats and parses scientific notation
     * @see #setScientificNotation
     * @see #getMinimumExponentDigits
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     */
    public boolean isScientificNotation() {
        return useExponentialNotation;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set whether or not scientific notation is used.
     * @param useScientific true if this object formats and parses scientific
     * notation
     * @see #isScientificNotation
     * @see #getMinimumExponentDigits
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     */
    public void setScientificNotation(boolean useScientific) {
        useExponentialNotation = useScientific;
        if (useExponentialNotation && minExponentDigits < 1) {
            minExponentDigits = 1;
        }
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Return the minimum exponent digits that will be shown.
     * @return the minimum exponent digits that will be shown
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     */
    public byte getMinimumExponentDigits() {
        return minExponentDigits;
    }
    
    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the minimum exponent digits that will be shown.  This has no
     * effect unless scientific notation is in use.
     * @param minExpDig a value >= 1 indicating the fewest exponent digits
     * that will be shown
     * @exception IllegalArgumentException if <code>minExpDig</code> < 1
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #getMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     */
    public void setMinimumExponentDigits(byte minExpDig) {
        if (minExpDig < 1) {
            throw new IllegalArgumentException("Exponent digits must be >= 1");
        }
        minExponentDigits = minExpDig;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Return whether the exponent sign is always shown.
     * @return true if the exponent is always prefixed with either the
     * localized minus sign or the localized plus sign, false if only negative
     * exponents are prefixed with the localized minus sign.
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #getMinimumExponentDigits
     * @see #setExponentSignAlwaysShown
     */
    public boolean isExponentSignAlwaysShown() {
        return exponentSignAlwaysShown;
    }

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set whether the exponent sign is always shown.  This has no effect
     * unless scientific notation is in use.
     * @param expSignAlways true if the exponent is always prefixed with either
     * the localized minus sign or the localized plus sign, false if only
     * negative exponents are prefixed with the localized minus sign.
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #getMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     */
    public void setExponentSignAlwaysShown(boolean expSignAlways) {
        exponentSignAlwaysShown = expSignAlways;
    }

    /**
     * Return the grouping size. Grouping size is the number of digits between
     * grouping separators in the integer portion of a number.  For example,
     * in the number "123,456.78", the grouping size is 3.
     * @see #setGroupingSize
     * @see NumberFormat#isGroupingUsed
     * @see DecimalFormatSymbols#getGroupingSeparator
     */
    public int getGroupingSize () {
        return groupingSize;
    }

    /**
     * Set the grouping size. Grouping size is the number of digits between
     * grouping separators in the integer portion of a number.  For example,
     * in the number "123,456.78", the grouping size is 3.
     * @see #getGroupingSize
     * @see NumberFormat#setGroupingUsed
     * @see DecimalFormatSymbols#setGroupingSeparator
     */
    public void setGroupingSize (int newValue) {
        groupingSize = (byte)newValue;
    }

    /**
     * Return the secondary grouping size. In some locales one
     * grouping interval is used for the least significant integer
     * digits (the primary grouping size), and another is used for all
     * others (the secondary grouping size).  A formatter supporting a
     * secondary grouping size will return a positive integer unequal
     * to the primary grouping size returned by
     * <code>getGroupingSize()</code>.  For example, if the primary
     * grouping size is 4, and the secondary grouping size is 2, then
     * the number 123456789 formats as "1,23,45,6789", and the pattern
     * appears as "#,##,###0".
     * [NEW]
     * @return the secondary grouping size, or a value less than
     * one if there is none
     * @see #setSecondaryGroupingSize
     * @see NumberFormat#isGroupingUsed
     * @see DecimalFormatSymbols#getGroupingSeparator
     */
    public int getSecondaryGroupingSize () {
        return groupingSize2;
    }

    /**
     * Set the secondary grouping size. If set to a value less than 1,
     * then secondary grouping is turned off, and the primary grouping
     * size is used for all intervals, not just the least significant.
     * [NEW]
     * @see #getSecondaryGroupingSize
     * @see NumberFormat#setGroupingUsed
     * @see DecimalFormatSymbols#setGroupingSeparator
     */
    public void setSecondaryGroupingSize (int newValue) {
        groupingSize2 = (byte)newValue;
    }

    /**
     * Allows you to get the behavior of the decimal separator with integers.
     * (The decimal separator will always appear with decimals.)
     * <P>Example: Decimal ON: 12345 -> 12345.; OFF: 12345 -> 12345
     */
    public boolean isDecimalSeparatorAlwaysShown() {
        return decimalSeparatorAlwaysShown;
    }

    /**
     * Allows you to set the behavior of the decimal separator with integers.
     * (The decimal separator will always appear with decimals.)
     * <P>Example: Decimal ON: 12345 -> 12345.; OFF: 12345 -> 12345
     */
    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        decimalSeparatorAlwaysShown = newValue;
    }

    /**
     * Standard override; no change in semantics.
     */
    public Object clone() {
        try {
            DecimalFormat other = (DecimalFormat) super.clone();
            other.symbols = (DecimalFormatSymbols) symbols.clone();
            return other;
        } catch (Exception e) {
            throw new InternalError();
        }
    }

    /**
     * Overrides equals
     */
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (!super.equals(obj)) return false; // super does class check
        DecimalFormat other = (DecimalFormat) obj;
        return (positivePrefix.equals(other.positivePrefix)
            && positiveSuffix.equals(other.positiveSuffix)
            && negativePrefix.equals(other.negativePrefix)
            && negativeSuffix.equals(other.negativeSuffix)
            && multiplier == other.multiplier
            && groupingSize == other.groupingSize
            && groupingSize2 == other.groupingSize2
            && decimalSeparatorAlwaysShown == other.decimalSeparatorAlwaysShown
            && useExponentialNotation == other.useExponentialNotation
            && (!useExponentialNotation ||
                minExponentDigits == other.minExponentDigits)
            && symbols.equals(other.symbols));
    }

    /**
     * Overrides hashCode
     */
    public int hashCode() {
        return super.hashCode() * 37 + positivePrefix.hashCode();
        // just enough fields for a reasonable distribution
    }

    /**
     * Synthesizes a pattern string that represents the current state
     * of this Format object.
     * @see #applyPattern
     */
    public String toPattern() {
        return toPattern( false );
    }

    /**
     * Synthesizes a localized pattern string that represents the current
     * state of this Format object.
     * @see #applyPattern
     */
    public String toLocalizedPattern() {
        return toPattern( true );
    }

    /**
     * Append an affix to the given StringBuffer, using quotes if
     * there are special characters.  Single quotes themselves must be
     * escaped in either case.
     */
    private void appendAffix(StringBuffer buffer, String affix, boolean localized) {
        boolean needQuote;
        if (localized) {
            needQuote = affix.indexOf(symbols.getZeroDigit()) >= 0
                || affix.indexOf(symbols.getGroupingSeparator()) >= 0
                || affix.indexOf(symbols.getDecimalSeparator()) >= 0
                || affix.indexOf(symbols.getPercent()) >= 0
                || affix.indexOf(symbols.getPerMill()) >= 0
                || affix.indexOf(symbols.getDigit()) >= 0
                || affix.indexOf(symbols.getPatternSeparator()) >= 0
                || affix.indexOf(symbols.getExponentSeparator()) >= 0;
        }
        else {
            needQuote = affix.indexOf(PATTERN_ZERO_DIGIT) >= 0
                || affix.indexOf(PATTERN_GROUPING_SEPARATOR) >= 0
                || affix.indexOf(PATTERN_DECIMAL_SEPARATOR) >= 0
                || affix.indexOf(PATTERN_PERCENT) >= 0
                || affix.indexOf(PATTERN_PER_MILLE) >= 0
                || affix.indexOf(PATTERN_DIGIT) >= 0
                || affix.indexOf(PATTERN_SEPARATOR) >= 0
                || affix.indexOf(PATTERN_EXPONENT) >= 0;
        }
        if (needQuote) buffer.append('\'');
        if (affix.indexOf('\'') < 0) buffer.append(affix);
        else {
            for (int j=0; j<affix.length(); ++j) {
                char c = affix.charAt(j);
                buffer.append(c);
                if (c == '\'') buffer.append(c);
            }
        }
        if (needQuote) buffer.append('\'');
    }

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong>
     * Does the real work of generating a pattern.
     */
    private String toPattern(boolean localized) {
        StringBuffer result = new StringBuffer();
        char zero = localized ? symbols.getZeroDigit() : PATTERN_ZERO_DIGIT;
        char digit = localized ? symbols.getDigit() : PATTERN_DIGIT;
        char group = localized ? symbols.getGroupingSeparator()
                               : PATTERN_GROUPING_SEPARATOR;
        int i;
        int roundingDecimalPos = 0; // Pos of decimal in roundingDigits
        String roundingDigits = null;
        int padPos = (formatWidth > 0) ? padPosition : -1;
        String padSpec = (formatWidth > 0)
            ? new StringBuffer(2).
                append(localized ? symbols.getPadEscape() : PATTERN_PAD_ESCAPE).
                append(pad).toString()
            : null;
        if (roundingIncrement != null) {
            i = roundingIncrement.scale();
            roundingDigits = roundingIncrement.movePointRight(i).toString();
            roundingDecimalPos = roundingDigits.length() - i;
        }
        for (int part=0; part<2; ++part) {
            int partStart = result.length();
            if (padPos == PAD_BEFORE_PREFIX) {
                result.append(padSpec);
            }
            appendAffix(result,
                        (part==0 ? positivePrefix : negativePrefix),
                        localized);
            if (padPos == PAD_AFTER_PREFIX) {
                result.append(padSpec);
            }
            int sub0Start = result.length();
            int g = isGroupingUsed() ? Math.max(0, groupingSize) : 0;
            if (g > 0 && groupingSize2 > 0 && groupingSize2 != groupingSize) {
                g += groupingSize2;
            }
            int maxIntDig = useExponentialNotation ? getMaximumIntegerDigits() :
                (Math.max(Math.max(g, getMinimumIntegerDigits()),
                          roundingDecimalPos) + 1);
            for (i = maxIntDig; i > 0; --i) {
                if (!useExponentialNotation && i<maxIntDig &&
                    isGroupingPosition(i)) {
                    result.append(group);
                }
                if (roundingDigits != null) {
                    int pos = roundingDecimalPos - i;
                    if (pos >= 0 && pos < roundingDigits.length()) {
                        result.append((char) (roundingDigits.charAt(pos) - '0' + zero));
                        continue;
                    }
                }
                result.append(i<=getMinimumIntegerDigits() ? zero : digit);
            }
            if (getMaximumFractionDigits() > 0 || decimalSeparatorAlwaysShown) {
                result.append(localized ? symbols.getDecimalSeparator() :
                              PATTERN_DECIMAL_SEPARATOR);
            }
            int pos = roundingDecimalPos;
            for (i = 0; i < getMaximumFractionDigits(); ++i) {
                if (roundingDigits != null &&
                    pos < roundingDigits.length()) {
                    result.append(pos < 0 ? zero :
                                  (char) (roundingDigits.charAt(pos) - '0' + zero));
                    ++pos;
                    continue;
                }
                result.append(i<getMinimumFractionDigits() ? zero : digit);
            }
            if (useExponentialNotation) {
                result.append(localized ? symbols.getExponentSeparator() :
                              PATTERN_EXPONENT);
                if (exponentSignAlwaysShown) {
                    result.append(localized ? symbols.getPlusSign() :
                                  PATTERN_PLUS_SIGN);
                }
                for (i=0; i<minExponentDigits; ++i) {
                    result.append(zero);
                }
            }
            if (padSpec != null && !useExponentialNotation) {
                int add = formatWidth - result.length() + sub0Start
                    - ((part == 0)
                       ? positivePrefix.length() + positiveSuffix.length()
                       : negativePrefix.length() + negativeSuffix.length());
                while (add > 0) {
                    result.insert(sub0Start, digit);
                    ++maxIntDig;
                    --add;
                    // Only add a grouping separator if we have at least
                    // 2 additional characters to be added, so we don't
                    // end up with ",###".
                    if (add>1 && isGroupingPosition(maxIntDig)) {
                        result.insert(sub0Start, group);
                        --add;                        
                    }
                }
            }
            if (padPos == PAD_BEFORE_SUFFIX) {
                result.append(padSpec);
            }
            if (part == 0) {
                appendAffix(result, positiveSuffix, localized);
                if (padPos == PAD_AFTER_SUFFIX) {
                    result.append(padSpec);
                }
                if (negativeSuffix.equals(positiveSuffix) &&
                    negativePrefix.equals(symbols.getMinusSign() + positivePrefix)) {
                    part = 2;
                } else {
                    result.append(localized ? symbols.getPatternSeparator() :
                                  PATTERN_SEPARATOR);
                }
            } else {
                appendAffix(result, negativeSuffix, localized);
                if (padPos == PAD_AFTER_SUFFIX) {
                    result.append(padSpec);
                }
            }
        }
        return result.toString();
    }

    /**
     * Apply the given pattern to this Format object.  A pattern is a
     * short-hand specification for the various formatting properties.
     * These properties can also be changed individually through the
     * various setter methods.
     * <p>
     * There is no limit to integer digits are set
     * by this routine, since that is the typical end-user desire;
     * use setMaximumInteger if you want to set a real value.
     * For negative numbers, use a second pattern, separated by a semicolon
     * <P>Example "#,#00.0#" -> 1,234.56
     * <P>This means a minimum of 2 integer digits, 1 fraction digit, and
     * a maximum of 2 fraction digits.
     * <p>Example: "#,#00.0#;(#,#00.0#)" for negatives in parantheses.
     * <p>In negative patterns, the minimum and maximum counts are ignored;
     * these are presumed to be set in the positive pattern.
     */
    public void applyPattern( String pattern ) {
        applyPattern( pattern, false );
    }

    /**
     * Apply the given pattern to this Format object.  The pattern
     * is assumed to be in a localized notation. A pattern is a
     * short-hand specification for the various formatting properties.
     * These properties can also be changed individually through the
     * various setter methods.
     * <p>
     * There is no limit to integer digits are set
     * by this routine, since that is the typical end-user desire;
     * use setMaximumInteger if you want to set a real value.
     * For negative numbers, use a second pattern, separated by a semicolon
     * <P>Example "#,#00.0#" -> 1,234.56
     * <P>This means a minimum of 2 integer digits, 1 fraction digit, and
     * a maximum of 2 fraction digits.
     * <p>Example: "#,#00.0#;(#,#00.0#)" for negatives in parantheses.
     * <p>In negative patterns, the minimum and maximum counts are ignored;
     * these are presumed to be set in the positive pattern.
     */
    public void applyLocalizedPattern( String pattern ) {
        applyPattern( pattern, true );
    }

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong>
     * Does the real work of applying a pattern.
     */
    private void applyPattern(String pattern, boolean localized) {
        char zeroDigit         = PATTERN_ZERO_DIGIT;
        char groupingSeparator = PATTERN_GROUPING_SEPARATOR;
        char decimalSeparator  = PATTERN_DECIMAL_SEPARATOR;
        char percent           = PATTERN_PERCENT;
        char perMill           = PATTERN_PER_MILLE;
        char digit             = PATTERN_DIGIT;
        char separator         = PATTERN_SEPARATOR;
        String exponent        = PATTERN_EXPONENT;
        char plus              = PATTERN_PLUS_SIGN;
        char padEscape         = PATTERN_PAD_ESCAPE;
        if (localized) {
            zeroDigit         = symbols.getZeroDigit();
            groupingSeparator = symbols.getGroupingSeparator();
            decimalSeparator  = symbols.getDecimalSeparator();
            percent           = symbols.getPercent();
            perMill           = symbols.getPerMill();
            digit             = symbols.getDigit();
            separator         = symbols.getPatternSeparator();
            exponent          = symbols.getExponentSeparator();
            plus              = symbols.getPlusSign();
            padEscape         = symbols.getPadEscape();
        }
        char nineDigit = (char) (zeroDigit + 9);

        boolean gotNegative = false;

        int pos = 0;
        // Part 0 is the positive pattern.  Part 1, if present, is the negative
        // pattern.
        for (int part=0; part<2 && pos<pattern.length(); ++part) {
            // The subpart ranges from 0 to 4: 0=pattern proper, 1=prefix,
            // 2=suffix, 3=prefix in quote, 4=suffix in quote.  Subpart 0 is
            // between the prefix and suffix, and consists of pattern
            // characters.  In the prefix and suffix, percent, permille, and
            // currency symbols are recognized and translated.
            int subpart = 1, sub0Start = 0, sub0Limit = 0, sub2Limit = 0;

            // It's important that we don't change any fields of this object
            // prematurely.  We set the following variables for the multiplier,
            // grouping, etc., and then only change the actual object fields if
            // everything parses correctly.  This also lets us register
            // the data from part 0 and ignore the part 1, except for the
            // prefix and suffix.
            StringBuffer prefix = new StringBuffer();
            StringBuffer suffix = new StringBuffer();
            int decimalPos = -1;
            int multiplier = 1;
            int digitLeftCount = 0, zeroDigitCount = 0, digitRightCount = 0;
            byte groupingCount = -1;
            byte groupingCount2 = -1;
            int padPos = -1;
            char padChar = 0;
            int incrementPos = -1;
            long incrementVal = 0;
            byte expDigits = -1;
            boolean expSignAlways = false;
            boolean isCurrency = false;

            // The affix is either the prefix or the suffix.
            StringBuffer affix = prefix;

            int start = pos;

        PARTLOOP:
            for (; pos < pattern.length(); ++pos) {
                char ch = pattern.charAt(pos);
                switch (subpart) {
                case 0: // Pattern proper subpart (between prefix & suffix)
                    // Process the digits, decimal, and grouping characters.  We
                    // record five pieces of information.  We expect the digits
                    // to occur in the pattern ####00.00####, and we record the
                    // number of left digits, zero (central) digits, and right
                    // digits.  The position of the last grouping character is
                    // recorded (should be somewhere within the first two blocks
                    // of characters), as is the position of the decimal point,
                    // if any (should be in the zero digits).  If there is no
                    // decimal point, then there should be no right digits.
                    if (ch == digit) {
                        if (zeroDigitCount > 0) {
                            ++digitRightCount;
                        } else {
                            ++digitLeftCount;
                        }
                        if (groupingCount >= 0 && decimalPos < 0) {
                            ++groupingCount;
                        }
                    } else if (ch >= zeroDigit && ch <= nineDigit) {
                        if (digitRightCount > 0) {
                            throw new IllegalArgumentException(
                                   "Unexpected '0' in pattern \"" +
                                   pattern + '"');
                        }
                        ++zeroDigitCount;
                        if (groupingCount >= 0 && decimalPos < 0) {
                            ++groupingCount;
                        }
                        if (ch != zeroDigit) {
                            int p = digitLeftCount + zeroDigitCount
                                + digitRightCount;
                            if (incrementPos >= 0) {
                                while (incrementPos < p) {
                                    incrementVal *= 10;
                                    ++incrementPos;
                                }
                            } else {
                                incrementPos = p;
                            }
                            incrementVal += ch - zeroDigit;
                        }
                    } else if (ch == groupingSeparator) {
                        if (decimalPos >= 0) {
                            throw new IllegalArgumentException(
                                    "Grouping separator after decimal in pattern \"" +
                                    pattern + '"');
                        }
                        groupingCount2 = groupingCount;
                        groupingCount = 0;
                    } else if (ch == decimalSeparator) {
                        if (decimalPos >= 0) {
                            throw new IllegalArgumentException(
                                    "Multiple decimal separators in pattern \"" +
                                    pattern + '"');
                        }
                        // Intentionally incorporate the digitRightCount,
                        // even though it is illegal for this to be > 0
                        // at this point.  We check pattern syntax below.
                        decimalPos = digitLeftCount + zeroDigitCount + digitRightCount;
                    } else {
                        if (pattern.regionMatches(pos, exponent, 0, exponent.length())) {
                            if (expDigits >= 0) {
                                throw new IllegalArgumentException(
                                        "Multiple exponential " +
                                        "symbols in pattern \"" +
                                        pattern + '"');
                            }
                            if (groupingCount >= 0) {
                                throw new IllegalArgumentException(
                                        "Grouping separator in exponential " +
                                        "pattern \"" +
                                        pattern + '"');
                            }
                            // Check for positive prefix
                            if ((pos+1) < pattern.length()
                                && pattern.charAt(pos+1) == plus) {
                                expSignAlways = true;
                                ++pos;
                            }
                            // Use lookahead to parse out the exponential part of the
                            // pattern, then jump into suffix subpart.
                            expDigits = 0;
                            while (++pos < pattern.length() &&
                                   pattern.charAt(pos) == zeroDigit) {
                                ++expDigits;
                            }
                            
                            if ((digitLeftCount + zeroDigitCount) < 1 ||
                                expDigits < 1) {
                                throw new IllegalArgumentException(
                                        "Malformed exponential " +
                                        "pattern \"" + pattern + '"');
                            }
                        }
                        // Transition to suffix subpart
                        subpart = 2; // suffix subpart
                        affix = suffix;
                        sub0Limit = pos--;
                        continue;
                    }
                    break;
                case 1: // Prefix subpart
                case 2: // Suffix subpart
                    // Process the prefix / suffix characters
                    // Process unquoted characters seen in prefix or suffix
                    // subpart.
                    if (ch == digit ||
                        ch == groupingSeparator ||
                        ch == decimalSeparator ||
                        (ch >= zeroDigit && ch <= nineDigit)) {
                        // Any of these characters implicitly begins the
                        // next subpart if we are in the prefix
                        if (subpart == 1) { // prefix subpart
                            subpart = 0; // pattern proper subpart
                            sub0Start = pos--; // Reprocess this character
                            continue;
                        }
                        // Fall through to append(ch)
                    } else if (ch == CURRENCY_SIGN) {
                        // Use lookahead to determine if the currency sign is
                        // doubled or not.
                        boolean doubled = (pos + 1) < pattern.length() &&
                            pattern.charAt(pos + 1) == CURRENCY_SIGN;
                        affix.append(doubled ?
                                     symbols.getInternationalCurrencySymbol() :
                                     symbols.getCurrencySymbol());
                        if (doubled) ++pos; // Skip over the doubled character
                        isCurrency = true;
                        continue;
                    } else if (ch == QUOTE) {
                        // A quote outside quotes indicates either the opening
                        // quote or two quotes, which is a quote literal.  That is,
                        // we have the first quote in 'do' or o''clock.
                        if ((pos+1) < pattern.length() &&
                            pattern.charAt(pos+1) == QUOTE) {
                            ++pos;
                            // Fall through to append(ch)
                        } else {
                            subpart += 2; // open quote
                            continue;
                        }
                    } else if (ch == separator) {
                        // Don't allow separators in the prefix, and don't allow
                        // separators in the second pattern (part == 1).
                        if (subpart == 1 || part == 1) {
                            throw new IllegalArgumentException(
                                    "Unquoted special character '" +
                                    ch + "' in pattern \"" +
                                    pattern + '"');
                        }
                        sub2Limit = pos++;
                        break PARTLOOP; // Go to next part
                    } else if (ch == percent || ch == perMill) {
                        // Next handle characters which are appended directly.
                        if (multiplier != 1) {
                            throw new IllegalArgumentException(
                                    "Too many percent/permille characters "
                                    + "in pattern \"" + pattern + '"');
                        }
                        if (ch == percent) {
                            multiplier = 100;
                            ch = symbols.getPercent();
                        } else {
                            multiplier = 1000;
                            ch = symbols.getPerMill();
                        }
                        // Fall through to append(ch)
                    } else if (ch == padEscape) {
                        if (padPos >= 0) {
                            throw new IllegalArgumentException(
                                    "Multiple pad specifiers");
                        }
                        if ((pos+1) == pattern.length()) {
                            throw new IllegalArgumentException(
                                    "Invalid pad specifier");
                        }
                        padPos = pos++; // Advance past pad char
                        padChar = pattern.charAt(pos);
                        continue;
                    }
                    // Unquoted, non-special characters fall through to here, as
                    // well as other code which needs to append something to the
                    // affix.
                    affix.append(ch);
                    break;
                case 3: // Prefix subpart, in quote
                case 4: // Suffix subpart, in quote
                    // A quote within quotes indicates either the closing
                    // quote or two quotes, which is a quote literal.  That is,
                    // we have the second quote in 'do' or 'don''t'.
                    if (ch == QUOTE) {
                        if ((pos+1) < pattern.length() &&
                            pattern.charAt(pos+1) == QUOTE) {
                            ++pos; 
                            // Fall through to append(ch)
                        } else {
                            subpart -= 2; // close quote
                            continue;
                        }
                    }
                    affix.append(ch);
                    break;                    
                }
            }

            if (sub0Limit == 0) {
                sub0Limit = pattern.length();
            }

            if (sub2Limit == 0) {
                sub2Limit = pattern.length();
            }

            /* Handle patterns with no '0' pattern character.  These patterns
             * are legal, but must be recodified to make sense.  "##.###" ->
             * "#0.###".  ".###" -> ".0##".
             *
             * We allow patterns of the form "####" to produce a zeroDigitCount
             * of zero (got that?); although this seems like it might make it
             * possible for format() to produce empty strings, format() checks
             * for this condition and outputs a zero digit in this situation.
             * Having a zeroDigitCount of zero yields a minimum integer digits
             * of zero, which allows proper round-trip patterns.  We don't want
             * "#" to become "#0" when toPattern() is called (even though that's
             * what it really is, semantically).
             */
            if (zeroDigitCount == 0 && digitLeftCount > 0 && decimalPos >= 0) {
                // Handle "###.###" and "###." and ".###"
                int n = decimalPos;
                if (n == 0) ++n; // Handle ".###"
                digitRightCount = digitLeftCount - n;
                digitLeftCount = n - 1;
                zeroDigitCount = 1;
            }

            // Do syntax checking on the digits, decimal points, and quotes.
            if ((decimalPos < 0 && digitRightCount > 0) ||
                (decimalPos >= 0 &&
                 (decimalPos < digitLeftCount ||
                  decimalPos > (digitLeftCount + zeroDigitCount))) ||
                groupingCount == 0 || groupingCount2 == 0 ||
                subpart > 2) { // subpart > 2 == unmatched quote
                throw new IllegalArgumentException("Malformed pattern \"" +
                                                   pattern + '"');
            }

            // Make sure pad is at legal position before or after affix.
            if (padPos >= 0) {
                if (padPos == start) {
                    padPos = PAD_BEFORE_PREFIX;
                } else if (padPos+2 == sub0Start) {
                    padPos = PAD_AFTER_PREFIX;
                } else if (padPos == sub0Limit) {
                    padPos = PAD_BEFORE_SUFFIX;
                } else if (padPos+2 == sub2Limit) {
                    padPos = PAD_AFTER_SUFFIX;
                } else {
                    throw new IllegalArgumentException("Illegal pad position");
                }
            }

            if (part == 0) {
                // Set negative affixes temporarily to match the positive
                // affixes.  Fix this up later after processing both parts.
                this.positivePrefix = this.negativePrefix = prefix.toString();
                this.positiveSuffix = this.negativeSuffix = suffix.toString();
                useExponentialNotation = (expDigits >= 0);
                if (useExponentialNotation) {
                    minExponentDigits = expDigits;
                    exponentSignAlwaysShown = expSignAlways;
                }
                isCurrencyFormat = isCurrency;
                int digitTotalCount = digitLeftCount + zeroDigitCount + digitRightCount;
                // The effectiveDecimalPos is the position the decimal is at or
                // would be at if there is no decimal.  Note that if
                // decimalPos<0, then digitTotalCount == digitLeftCount +
                // zeroDigitCount.
                int effectiveDecimalPos = decimalPos >= 0 ? decimalPos : digitTotalCount;
                setMinimumIntegerDigits(effectiveDecimalPos - digitLeftCount);
                setMaximumIntegerDigits(useExponentialNotation
                        ? digitLeftCount + getMinimumIntegerDigits()
                        : Integer.MAX_VALUE);
                setMaximumFractionDigits(decimalPos >= 0
                        ? (digitTotalCount - decimalPos) : 0);
                setMinimumFractionDigits(decimalPos >= 0
                        ? (digitLeftCount + zeroDigitCount - decimalPos) : 0);
                setGroupingUsed(groupingCount > 0);
                this.groupingSize = (groupingCount > 0) ? groupingCount : 0;
                this.groupingSize2 = (groupingCount2 > 0 && groupingCount2 != groupingCount)
                    ? groupingCount2 : 0;
                this.multiplier = multiplier;
                setDecimalSeparatorAlwaysShown(decimalPos == 0
                        || decimalPos == digitTotalCount);
                if (padPos >= 0) {
                    padPosition = padPos;
                    formatWidth = prefix.length() + suffix.length() +
                        sub0Limit - sub0Start;
                    pad = padChar;
                } else {
                    formatWidth = 0;
                }
                if (incrementVal != 0) {
                    // BigDecimal scale cannot be negative (even though
                    // this makes perfect sense), so we need to handle this.
                    int scale = incrementPos - effectiveDecimalPos;
                    roundingIncrement =
                        java.math.BigDecimal.valueOf(incrementVal, scale > 0 ? scale : 0);
                    if (scale < 0) {
                        roundingIncrement =
                            roundingIncrement.movePointRight(-scale);
                    }
                    roundingDouble = roundingIncrement.doubleValue();
                    roundingMode = java.math.BigDecimal.ROUND_HALF_EVEN;
                } else {
                    setRoundingIncrement(null);
                }
            } else {
                this.negativePrefix = prefix.toString();
                this.negativeSuffix = suffix.toString();
                gotNegative = true;
            }
        }

        // If there was no negative pattern, or if the negative pattern is
        // identical to the positive pattern, then prepend the minus sign to the
        // positive pattern to form the negative pattern.
        if (!gotNegative ||
            (negativePrefix.equals(positivePrefix)
             && negativeSuffix.equals(positiveSuffix))) {
            negativeSuffix = positiveSuffix;
            negativePrefix = symbols.getMinusSign() + positivePrefix;
        }
    }

    /**
     * First, read the default serializable fields from the stream.  Then
     * if <code>serialVersionOnStream</code> is less than 1, indicating that
     * the stream was written by JDK 1.1, initialize <code>useExponentialNotation</code>
     * to false, since it was not present in JDK 1.1.
     * Finally, set serialVersionOnStream back to the maximum allowed value so that
     * default serialization will work properly if this object is streamed out again.
     */
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        if (serialVersionOnStream < 2) {
            exponentSignAlwaysShown = false;
            roundingDouble = 0.0;
            roundingIncrement = null;
            roundingMode = java.math.BigDecimal.ROUND_HALF_EVEN;
            formatWidth = 0;
            pad = ' ';
            padPosition = PAD_BEFORE_PREFIX;
            if (serialVersionOnStream < 1) {
                // Didn't have exponential fields
                useExponentialNotation = false;
            }
        }
        serialVersionOnStream = currentSerialVersion;
        digitList = new DigitList();
    }

    //----------------------------------------------------------------------
    // INSTANCE VARIABLES
    //----------------------------------------------------------------------

    private transient DigitList digitList = new DigitList();

    /**
     * The symbol used as a prefix when formatting positive numbers, e.g. "+".
     *
     * @serial
     * @see #getPositivePrefix
     */
    private String  positivePrefix = "";

    /**
     * The symbol used as a suffix when formatting positive numbers.
     * This is often an empty string.
     *
     * @serial
     * @see #getPositiveSuffix
     */
    private String  positiveSuffix = "";

    /**
     * The symbol used as a prefix when formatting negative numbers, e.g. "-".
     *
     * @serial
     * @see #getNegativePrefix
     */
    private String  negativePrefix = "-";

    /**
     * The symbol used as a suffix when formatting negative numbers.
     * This is often an empty string.
     *
     * @serial
     * @see #getNegativeSuffix
     */
    private String  negativeSuffix = "";

    /**
     * The multiplier for use in percent, permill, etc.
     *
     * @serial
     * @see #getMultiplier
     */
    private int     multiplier = 1;
    
    /**
     * The number of digits between grouping separators in the integer
     * portion of a number.  Must be greater than 0 if
     * <code>NumberFormat.groupingUsed</code> is true.
     *
     * @serial
     * @see #getGroupingSize
     * @see NumberFormat#isGroupingUsed
     */
    private byte    groupingSize = 3;  // invariant, > 0 if useThousands

    /**
     * The secondary grouping size.  This is only used for Hindi
     * numerals, which use a primary grouping of 3 and a secondary
     * grouping of 2, e.g., "12,34,567".  If this value is less than
     * 1, then secondary grouping is equal to the primary grouping.
     * [NEW]
     */
    private byte    groupingSize2 = 0;
    
    /**
     * If true, forces the decimal separator to always appear in a formatted
     * number, even if the fractional part of the number is zero.
     *
     * @serial
     * @see #isDecimalSeparatorAlwaysShown
     */
    private boolean decimalSeparatorAlwaysShown = false;
    
    /**
     * True if this object represents a currency format.  This determines
     * whether the monetary decimal separator is used instead of the normal one.
     */
    private transient boolean isCurrencyFormat = false;
    
    /**
     * The <code>DecimalFormatSymbols</code> object used by this format.
     * It contains the symbols used to format numbers, e.g. the grouping separator,
     * decimal separator, and so on.
     *
     * @serial
     * @see #setDecimalFormatSymbols
     * @see DecimalFormatSymbols
     */
    private DecimalFormatSymbols symbols = null; // LIU new DecimalFormatSymbols();

    /**
     * True to force the use of exponential (i.e. scientific) notation when formatting
     * numbers.
     * <p>
     * Note that the JDK 1.2 public API provides no way to set this field,
     * even though it is supported by the implementation and the stream format.
     * The intent is that this will be added to the API in the future.
     *
     * @serial
     * @since JDK 1.2
     */
    private boolean useExponentialNotation;  // Newly persistent in JDK 1.2

    /**
     * The minimum number of digits used to display the exponent when a number is
     * formatted in exponential notation.  This field is ignored if
     * <code>useExponentialNotation</code> is not true.
     * <p>
     * Note that the JDK 1.2 public API provides no way to set this field,
     * even though it is supported by the implementation and the stream format.
     * The intent is that this will be added to the API in the future.
     *
     * @serial
     * @since JDK 1.2
     */
    private byte    minExponentDigits;       // Newly persistent in JDK 1.2

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * If true, the exponent is always prefixed with either the plus
     * sign or the minus sign.  Otherwise, only negative exponents are
     * prefixed with the minus sign.  This has no effect unless
     * <code>useExponentialNotation</code> is true.
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private boolean exponentSignAlwaysShown = false;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * The value to which numbers are rounded during formatting.  For example,
     * if the rounding increment is 0.05, then 13.371 would be formatted as
     * 13.350, assuming 3 fraction digits.  Has the value <code>null</code> if
     * rounding is not in effect, or a positive value if rounding is in effect.
     * Default value <code>null</code>.
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private java.math.BigDecimal roundingIncrement = null;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * The rounding increment as a double.  If this value is <= 0, then no
     * rounding is done.  This value is
     * <code>roundingIncrement.doubleValue()</code>.  Default value 0.0.
     */
    private transient double roundingDouble = 0.0;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * The rounding mode.  This value controls any rounding operations which
     * occur when applying a rounding increment or when reducing the number of
     * fraction digits to satisfy a maximum fraction digits limit.  The value
     * may assume any of the <code>BigDecimal</code> rounding mode values.
     * Default value <code>BigDecimal.ROUND_HALF_EVEN</code>.
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private int roundingMode = java.math.BigDecimal.ROUND_HALF_EVEN;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * The padded format width, or zero if there is no padding.  Must
     * be >= 0.  Default value zero.
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private int formatWidth = 0;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * The character used to pad the result of format to
     * <code>formatWidth</code>, if padding is in effect.  Default value ' '.
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private char pad = ' ';

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * The position in the string at which the <code>pad</code> character
     * will be inserted, if padding is in effect.  Must have a value from
     * <code>PAD_BEFORE_PREFIX</code> to <code>PAD_AFTER_SUFFIX</code>.
     * Default value <code>PAD_BEFORE_PREFIX</code>.
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private int padPosition = PAD_BEFORE_PREFIX;

    //----------------------------------------------------------------------

    static final int currentSerialVersion = 2;

    /**
     * The internal serial version which says which version was written
     * Possible values are:
     * <ul>
     * <li><b>0</b> (default): versions before JDK 1.2
     * <li><b>1</b>: version from JDK 1.2 and later, which includes the two new fields
     *      <code>useExponentialNotation</code> and <code>minExponentDigits</code>.
     * <li><b>2</b>: version on AlphaWorks, which adds roundingMode, formatWidth,
     *      pad, padPosition, exponentSignAlwaysShown, roundingIncrement.
     * </ul>
     * @since JDK 1.2
     * @serial */
    private int serialVersionOnStream = currentSerialVersion;

    //----------------------------------------------------------------------
    // CONSTANTS
    //----------------------------------------------------------------------

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Constant for <code>getPadPosition()</code> and
     * <code>setPadPosition()</code> specifying pad characters inserted before
     * the prefix.
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @see #PAD_AFTER_SUFFIX
     */
    public static final int PAD_BEFORE_PREFIX = 0;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Constant for <code>getPadPosition()</code> and
     * <code>setPadPosition()</code> specifying pad characters inserted after
     * the prefix.
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @see #PAD_AFTER_SUFFIX
     */
    public static final int PAD_AFTER_PREFIX  = 1;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Constant for <code>getPadPosition()</code> and
     * <code>setPadPosition()</code> specifying pad characters inserted before
     * the suffix.
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_AFTER_SUFFIX
     */
    public static final int PAD_BEFORE_SUFFIX = 2;

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Constant for <code>getPadPosition()</code> and
     * <code>setPadPosition()</code> specifying pad characters inserted after
     * the suffix.
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     */
    public static final int PAD_AFTER_SUFFIX  = 3;

    // Constants for characters used in programmatic (unlocalized) patterns.
    private static final char       PATTERN_ZERO_DIGIT         = '0';
    private static final char       PATTERN_GROUPING_SEPARATOR = ',';
    private static final char       PATTERN_DECIMAL_SEPARATOR  = '.';
    private static final char       PATTERN_PER_MILLE          = '\u2030';
    private static final char       PATTERN_PERCENT            = '%';
    private static final char       PATTERN_DIGIT              = '#';
    private static final char       PATTERN_SEPARATOR          = ';';
            static final String     PATTERN_EXPONENT           = "E"; // [NEW]
            static final char       PATTERN_PAD_ESCAPE         = '*'; // [NEW]
            static final char       PATTERN_PLUS_SIGN          = '+'; // [NEW]
    // Pad escape is package private to allow access by DecimalFormatSymbols.
    // Also plus sign.  Also exponent.

    /**
     * The CURRENCY_SIGN is the standard Unicode symbol for currency.  It
     * is used in patterns and substitued with either the currency symbol,
     * or if it is doubled, with the international currency symbol.  If the
     * CURRENCY_SIGN is seen in a pattern, then the decimal separator is
     * replaced with the monetary decimal separator.
     *
     * The CURRENCY_SIGN is not localized.
     */
    private static final char       CURRENCY_SIGN = '\u00A4';

    private static final char       QUOTE = '\'';

    // Proclaim JDK 1.1 serial compatibility.
    static final long serialVersionUID = 864413376551465018L;

    /**
     * Cache to hold the NumberPattern of a Locale.
     * [NEW] No longer needed -- share the NumberFormat cache
     */
    // private static Hashtable cachedLocaleData = new Hashtable(3);
}

//eof
