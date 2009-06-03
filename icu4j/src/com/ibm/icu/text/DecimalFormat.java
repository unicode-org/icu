/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.math.MathContext;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;

//This is an enhanced version of DecimalFormat that is based on the standard version in the JDK. 
/**
 * <code>DecimalFormat</code> is a concrete subclass of
 * {@link NumberFormat} that formats decimal numbers. It has a variety of
 * features designed to make it possible to parse and format numbers in any
 * locale, including support for Western, Arabic, or Indic digits.  It also
 * supports different flavors of numbers, including integers ("123"),
 * fixed-point numbers ("123.4"), scientific notation ("1.23E4"), percentages
 * ("12%"), and currency amounts ("$123.00", "USD123.00", "123.00 US dollars").
 * All of these flavors can be easily localized.
 *
 * 
 * <p>To obtain a {@link NumberFormat} for a specific locale (including the
 * default locale) call one of <code>NumberFormat</code>'s factory methods such
 * as {@link NumberFormat#getInstance}. Do not call the <code>DecimalFormat</code>
 * constructors directly, unless you know what you are doing, since the
 * {@link NumberFormat} factory methods may return subclasses other than
 * <code>DecimalFormat</code>. If you need to customize the format object, do
 * something like this:
 *
 * <blockquote><pre>
 * NumberFormat f = NumberFormat.getInstance(loc);
 * if (f instanceof DecimalFormat) {
 *     ((DecimalFormat) f).setDecimalSeparatorAlwaysShown(true);
 * }</pre></blockquote>
 *
 * <p><strong>Example Usage</strong>
 *
 * <blockquote><pre>
 * <strong>// Print out a number using the localized number, currency,
 * // and percent format for each locale</strong>
 * Locale[] locales = NumberFormat.getAvailableLocales();
 * double myNumber = -1234.56;
 * NumberFormat format;
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
 *             format = NumberFormat.getInstance(locales[i]); break;
 *         case 1:
 *             format = NumberFormat.getCurrencyInstance(locales[i]); break;
 *         default:
 *             format = NumberFormat.getPercentInstance(locales[i]); break;
 *         }
 *         try {
 *             // Assume format is a DecimalFormat
 *             System.out.print(": " + ((DecimalFormat) format).toPattern()
 *                              + " -> " + form.format(myNumber));
 *         } catch (Exception e) {}
 *         try {
 *             System.out.println(" -> " + format.parse(form.format(myNumber)));
 *         } catch (ParseException e) {}
 *     }
 * }</pre></blockquote>
 *
 * <P>
 * Another example use getInstance(style)
 * <P>
 * <pre>
 * <strong>// Print out a number using the localized number, currency,
 * // percent, scientific, integer, iso currency, and plural currency
 * // format for each locale</strong>
 * ULocale locale = new ULocale("en_US");
 * double myNumber = 1234.56;
 * for (int j=NumberFormat.NUMBERSTYLE; j<=NumberFormat.PLURALCURRENCYSTYLE; ++j) {
 *     NumberFormat format = NumberFormat.getInstance(locale, j);
 *     try {
 *         // Assume format is a DecimalFormat
 *         System.out.print(": " + ((DecimalFormat) format).toPattern()
 *                          + " -> " + form.format(myNumber));
 *     } catch (Exception e) {}
 *     try {
 *         System.out.println(" -> " + format.parse(form.format(myNumber)));
 *     } catch (ParseException e) {}
 * }</pre></blockquote>
 *
 * <h4>Patterns</h4>
 *
 * <p>A <code>DecimalFormat</code> consists of a <em>pattern</em> and a set of
 * <em>symbols</em>.  The pattern may be set directly using
 * {@link #applyPattern}, or indirectly using other API methods which
 * manipulate aspects of the pattern, such as the minimum number of integer
 * digits.  The symbols are stored in a {@link DecimalFormatSymbols}
 * object.  When using the {@link NumberFormat} factory methods, the
 * pattern and symbols are read from ICU's locale data.
 * 
 * <h4>Special Pattern Characters</h4>
 *
 * <p>Many characters in a pattern are taken literally; they are matched during
 * parsing and output unchanged during formatting.  Special characters, on the
 * other hand, stand for other characters, strings, or classes of characters.
 * For example, the '#' character is replaced by a localized digit.  Often the
 * replacement character is the same as the pattern character; in the U.S. locale,
 * the ',' grouping character is replaced by ','.  However, the replacement is
 * still happening, and if the symbols are modified, the grouping character
 * changes.  Some special characters affect the behavior of the formatter by
 * their presence; for example, if the percent character is seen, then the
 * value is multiplied by 100 before being displayed.
 *
 * <p>To insert a special character in a pattern as a literal, that is, without
 * any special meaning, the character must be quoted.  There are some exceptions to
 * this which are noted below.
 *
 * <p>The characters listed here are used in non-localized patterns.  Localized
 * patterns use the corresponding characters taken from this formatter's
 * {@link DecimalFormatSymbols} object instead, and these characters lose
 * their special status.  Two exceptions are the currency sign and quote, which
 * are not localized.
 *
 * <blockquote>
 * <table border=0 cellspacing=3 cellpadding=0 summary="Chart showing symbol,
 *  location, localized, and meaning.">
 *   <tr bgcolor="#ccccff">
 *     <th align=left>Symbol
 *     <th align=left>Location
 *     <th align=left>Localized?
 *     <th align=left>Meaning
 *   <tr valign=top>
 *     <td><code>0</code>
 *     <td>Number
 *     <td>Yes
 *     <td>Digit
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>1-9</code>
 *     <td>Number
 *     <td>Yes
 *     <td>'1' through '9' indicate rounding. 
 *         
 *   <tr valign=top>
 *     <td><code>@</code>
 *     <td>Number
 *     <td>No
 *     <td>Significant digit
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>#</code>
 *     <td>Number
 *     <td>Yes
 *     <td>Digit, zero shows as absent
 *   <tr valign=top>
 *     <td><code>.</code>
 *     <td>Number
 *     <td>Yes
 *     <td>Decimal separator or monetary decimal separator
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>-</code>
 *     <td>Number
 *     <td>Yes
 *     <td>Minus sign
 *   <tr valign=top>
 *     <td><code>,</code>
 *     <td>Number
 *     <td>Yes
 *     <td>Grouping separator
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>E</code>
 *     <td>Number
 *     <td>Yes
 *     <td>Separates mantissa and exponent in scientific notation.
 *         <em>Need not be quoted in prefix or suffix.</em>
 *   <tr valign=top>
 *     <td><code>+</code>
 *     <td>Exponent
 *     <td>Yes
 *     <td>Prefix positive exponents with localized plus sign.
 *         <em>Need not be quoted in prefix or suffix.</em>
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>;</code>
 *     <td>Subpattern boundary
 *     <td>Yes
 *     <td>Separates positive and negative subpatterns
 *   <tr valign=top>
 *     <td><code>%</code>
 *     <td>Prefix or suffix
 *     <td>Yes
 *     <td>Multiply by 100 and show as percentage
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>&#92;u2030</code>
 *     <td>Prefix or suffix
 *     <td>Yes
 *     <td>Multiply by 1000 and show as per mille
 *   <tr valign=top>
 *     <td><code>&#164;</code> (<code>&#92;u00A4</code>)
 *     <td>Prefix or suffix
 *     <td>No
 *     <td>Currency sign, replaced by currency symbol.  If
 *         doubled, replaced by international currency symbol.
 *         If tripled, replaced by currency plural names, for example,
 *         "US dollar" or "US dollars" for America.
 *         If present in a pattern, the monetary decimal separator
 *         is used instead of the decimal separator.
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>'</code>
 *     <td>Prefix or suffix
 *     <td>No
 *     <td>Used to quote special characters in a prefix or suffix,
 *         for example, <code>"'#'#"</code> formats 123 to
 *         <code>"#123"</code>.  To create a single quote
 *         itself, use two in a row: <code>"# o''clock"</code>.
 *   <tr valign=top>
 *     <td><code>*</code>
 *     <td>Prefix or suffix boundary
 *     <td>Yes
 *     <td>Pad escape, precedes pad character
 * </table>
 * </blockquote>
 *
 * <p>A <code>DecimalFormat</code> pattern contains a postive and negative
 * subpattern, for example, "#,##0.00;(#,##0.00)".  Each subpattern has a
 * prefix, a numeric part, and a suffix.  If there is no explicit negative
 * subpattern, the negative subpattern is the localized minus sign prefixed to the
 * positive subpattern. That is, "0.00" alone is equivalent to "0.00;-0.00".  If there
 * is an explicit negative subpattern, it serves only to specify the negative
 * prefix and suffix; the number of digits, minimal digits, and other
 * characteristics are ignored in the negative subpattern. That means that
 * "#,##0.0#;(#)" has precisely the same result as "#,##0.0#;(#,##0.0#)".
 *
 * <p>The prefixes, suffixes, and various symbols used for infinity, digits,
 * thousands separators, decimal separators, etc. may be set to arbitrary
 * values, and they will appear properly during formatting.  However, care must
 * be taken that the symbols and strings do not conflict, or parsing will be
 * unreliable.  For example, either the positive and negative prefixes or the
 * suffixes must be distinct for {@link #parse} to be able
 * to distinguish positive from negative values.  Another example is that the
 * decimal separator and thousands separator should be distinct characters, or
 * parsing will be impossible.
 *
 * <p>The <em>grouping separator</em> is a character that separates clusters of
 * integer digits to make large numbers more legible.  It commonly used for
 * thousands, but in some locales it separates ten-thousands.  The <em>grouping
 * size</em> is the number of digits between the grouping separators, such as 3
 * for "100,000,000" or 4 for "1 0000 0000". There are actually two different
 * grouping sizes: One used for the least significant integer digits, the
 * <em>primary grouping size</em>, and one used for all others, the
 * <em>secondary grouping size</em>.  In most locales these are the same, but
 * sometimes they are different. For example, if the primary grouping interval
 * is 3, and the secondary is 2, then this corresponds to the pattern
 * "#,##,##0", and the number 123456789 is formatted as "12,34,56,789".  If a
 * pattern contains multiple grouping separators, the interval between the last
 * one and the end of the integer defines the primary grouping size, and the
 * interval between the last two defines the secondary grouping size. All others
 * are ignored, so "#,##,###,####" == "###,###,####" == "##,#,###,####".
 *
 * <p>Illegal patterns, such as "#.#.#" or "#.###,###", will cause
 * <code>DecimalFormat</code> to throw an {@link IllegalArgumentException}
 * with a message that describes the problem.
 *
 * <h4>Pattern BNF</h4>
 *
 * <pre>
 * pattern    := subpattern (';' subpattern)?
 * subpattern := prefix? number exponent? suffix?
 * number     := (integer ('.' fraction)?) | sigDigits
 * prefix     := '&#92;u0000'..'&#92;uFFFD' - specialCharacters
 * suffix     := '&#92;u0000'..'&#92;uFFFD' - specialCharacters
 * integer    := '#'* '0'* '0'
 * fraction   := '0'* '#'*
 * sigDigits  := '#'* '@' '@'* '#'*
 * exponent   := 'E' '+'? '0'* '0'
 * padSpec    := '*' padChar
 * padChar    := '&#92;u0000'..'&#92;uFFFD' - quote
 * &#32;
 * Notation:
 *   X*       0 or more instances of X
 *   X?       0 or 1 instances of X
 *   X|Y      either X or Y
 *   C..D     any character from C up to D, inclusive
 *   S-T      characters in S, except those in T
 * </pre>
 * The first subpattern is for positive numbers. The second (optional)
 * subpattern is for negative numbers.
 * 
 * <p>Not indicated in the BNF syntax above:
 *
 * <ul><li>The grouping separator ',' can occur inside the integer and
 * sigDigits elements, between any two pattern characters of that
 * element, as long as the integer or sigDigits element is not
 * followed by the exponent element.
 *
 * <li>Two grouping intervals are recognized: That between the
 *     decimal point and the first grouping symbol, and that
 *     between the first and second grouping symbols. These
 *     intervals are identical in most locales, but in some
 *     locales they differ. For example, the pattern
 *     &quot;#,##,###&quot; formats the number 123456789 as
 *     &quot;12,34,56,789&quot;.</li>
 * 
 * <li>
 * The pad specifier <code>padSpec</code> may appear before the prefix,
 * after the prefix, before the suffix, after the suffix, or not at all.
 *
 * <li>
 * In place of '0', the digits '1' through '9' may be used to
 * indicate a rounding increment.
 * </ul>
 *
 * <h4>Parsing</h4>
 *
 * <p><code>DecimalFormat</code> parses all Unicode characters that represent
 * decimal digits, as defined by {@link UCharacter#digit}.  In addition,
 * <code>DecimalFormat</code> also recognizes as digits the ten consecutive
 * characters starting with the localized zero digit defined in the
 * {@link DecimalFormatSymbols} object.  During formatting, the
 * {@link DecimalFormatSymbols}-based digits are output.
 *
 * <p>During parsing, grouping separators are ignored.
 *
 * <p>For currency parsing, the formatter is able to parse every currency
 * style formats no matter which style the formatter is constructed with.
 * For example, a formatter instance gotten from 
 * NumberFormat.getInstance(ULocale, NumberFormat.CURRENCYSTYLE) can parse
 * formats such as "USD1.00" and "3.00 US dollars".
 *
 * <p>If {@link #parse(String, ParsePosition)} fails to parse
 * a string, it returns <code>null</code> and leaves the parse position
 * unchanged.  The convenience method {@link #parse(String)}
 * indicates parse failure by throwing a {@link java.text.ParseException}.
 *
 * <h4>Formatting</h4>
 *
 * <p>Formatting is guided by several parameters, all of which can be
 * specified either using a pattern or using the API.  The following
 * description applies to formats that do not use <a href="#sci">scientific
 * notation</a> or <a href="#sigdig">significant digits</a>.
 *
 * <ul><li>If the number of actual integer digits exceeds the
 * <em>maximum integer digits</em>, then only the least significant
 * digits are shown.  For example, 1997 is formatted as "97" if the
 * maximum integer digits is set to 2.
 *
 * <li>If the number of actual integer digits is less than the
 * <em>minimum integer digits</em>, then leading zeros are added.  For
 * example, 1997 is formatted as "01997" if the minimum integer digits
 * is set to 5.
 *
 * <li>If the number of actual fraction digits exceeds the <em>maximum
 * fraction digits</em>, then half-even rounding it performed to the
 * maximum fraction digits.  For example, 0.125 is formatted as "0.12"
 * if the maximum fraction digits is 2.  This behavior can be changed
 * by specifying a rounding increment and a rounding mode.
 *
 * <li>If the number of actual fraction digits is less than the
 * <em>minimum fraction digits</em>, then trailing zeros are added.
 * For example, 0.125 is formatted as "0.1250" if the mimimum fraction
 * digits is set to 4.
 *
 * <li>Trailing fractional zeros are not displayed if they occur
 * <em>j</em> positions after the decimal, where <em>j</em> is less
 * than the maximum fraction digits. For example, 0.10004 is
 * formatted as "0.1" if the maximum fraction digits is four or less.
 * </ul>
 * 
 * <p><strong>Special Values</strong>
 *
 * <p><code>NaN</code> is represented as a single character, typically
 * <code>&#92;uFFFD</code>.  This character is determined by the
 * {@link DecimalFormatSymbols} object.  This is the only value for which
 * the prefixes and suffixes are not used.
 *
 * <p>Infinity is represented as a single character, typically
 * <code>&#92;u221E</code>, with the positive or negative prefixes and suffixes
 * applied.  The infinity character is determined by the
 * {@link DecimalFormatSymbols} object.
 *
 * <a name="sci"><h4>Scientific Notation</h4></a>
 *
 * <p>Numbers in scientific notation are expressed as the product of a mantissa
 * and a power of ten, for example, 1234 can be expressed as 1.234 x 10<sup>3</sup>. The
 * mantissa is typically in the half-open interval [1.0, 10.0) or sometimes [0.0, 1.0),
 * but it need not be.  <code>DecimalFormat</code> supports arbitrary mantissas.
 * <code>DecimalFormat</code> can be instructed to use scientific
 * notation through the API or through the pattern.  In a pattern, the exponent
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
 * <li>When using scientific notation, the formatter controls the
 * digit counts using significant digits logic.  The maximum number of
 * significant digits limits the total number of integer and fraction
 * digits that will be shown in the mantissa; it does not affect
 * parsing.  For example, 12345 formatted with "##0.##E0" is "12.3E3".
 * See the section on significant digits for more details.
 *
 * <li>The number of significant digits shown is determined as
 * follows: If areSignificantDigitsUsed() returns false, then the
 * minimum number of significant digits shown is one, and the maximum
 * number of significant digits shown is the sum of the <em>minimum
 * integer</em> and <em>maximum fraction</em> digits, and is
 * unaffected by the maximum integer digits.  If this sum is zero,
 * then all significant digits are shown.  If
 * areSignificantDigitsUsed() returns true, then the significant digit
 * counts are specified by getMinimumSignificantDigits() and
 * getMaximumSignificantDigits().  In this case, the number of
 * integer digits is fixed at one, and there is no exponent grouping.
 *
 * <li>Exponential patterns may not contain grouping separators.
 * </ul>
 *
 * <a name="sigdig"><h4>
 * Significant Digits</h4></a>
 *
 * <code>DecimalFormat</code> has two ways of controlling how many
 * digits are shows: (a) significant digits counts, or (b) integer and
 * fraction digit counts.  Integer and fraction digit counts are
 * described above.  When a formatter is using significant digits
 * counts, the number of integer and fraction digits is not specified
 * directly, and the formatter settings for these counts are ignored.
 * Instead, the formatter uses however many integer and fraction
 * digits are required to display the specified number of significant
 * digits.  Examples:
 *
 * <blockquote>
 * <table border=0 cellspacing=3 cellpadding=0>
 *   <tr bgcolor="#ccccff">
 *     <th align=left>Pattern
 *     <th align=left>Minimum significant digits
 *     <th align=left>Maximum significant digits
 *     <th align=left>Number
 *     <th align=left>Output of format()
 *   <tr valign=top>
 *     <td><code>@@@</code>
 *     <td>3
 *     <td>3
 *     <td>12345
 *     <td><code>12300</code>
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>@@@</code>
 *     <td>3
 *     <td>3
 *     <td>0.12345
 *     <td><code>0.123</code>
 *   <tr valign=top>
 *     <td><code>@@##</code>
 *     <td>2
 *     <td>4
 *     <td>3.14159
 *     <td><code>3.142</code>
 *   <tr valign=top bgcolor="#eeeeff">
 *     <td><code>@@##</code>
 *     <td>2
 *     <td>4
 *     <td>1.23004
 *     <td><code>1.23</code>
 * </table>
 * </blockquote>
 *
 * <ul>
 * <li>Significant digit counts may be expressed using patterns that
 * specify a minimum and maximum number of significant digits.  These
 * are indicated by the <code>'@'</code> and <code>'#'</code>
 * characters.  The minimum number of significant digits is the number
 * of <code>'@'</code> characters.  The maximum number of significant
 * digits is the number of <code>'@'</code> characters plus the number
 * of <code>'#'</code> characters following on the right.  For
 * example, the pattern <code>"@@@"</code> indicates exactly 3
 * significant digits.  The pattern <code>"@##"</code> indicates from
 * 1 to 3 significant digits.  Trailing zero digits to the right of
 * the decimal separator are suppressed after the minimum number of
 * significant digits have been shown.  For example, the pattern
 * <code>"@##"</code> formats the number 0.1203 as
 * <code>"0.12"</code>.
 *
 * <li>If a pattern uses significant digits, it may not contain a
 * decimal separator, nor the <code>'0'</code> pattern character.
 * Patterns such as <code>"@00"</code> or <code>"@.###"</code> are
 * disallowed.
 *
 * <li>Any number of <code>'#'</code> characters may be prepended to
 * the left of the leftmost <code>'@'</code> character.  These have no
 * effect on the minimum and maximum significant digits counts, but
 * may be used to position grouping separators.  For example,
 * <code>"#,#@#"</code> indicates a minimum of one significant digits,
 * a maximum of two significant digits, and a grouping size of three.
 *
 * <li>In order to enable significant digits formatting, use a pattern
 * containing the <code>'@'</code> pattern character.  Alternatively,
 * call {@link #setSignificantDigitsUsed setSignificantDigitsUsed(true)}.
 *
 * <li>In order to disable significant digits formatting, use a
 * pattern that does not contain the <code>'@'</code> pattern
 * character. Alternatively, call {@link #setSignificantDigitsUsed
 * setSignificantDigitsUsed(false)}.
 *
 * <li>The number of significant digits has no effect on parsing.
 *
 * <li>Significant digits may be used together with exponential notation. Such
 * patterns are equivalent to a normal exponential pattern with a minimum and
 * maximum integer digit count of one, a minimum fraction digit count of
 * <code>getMinimumSignificantDigits() - 1</code>, and a maximum fraction digit
 * count of <code>getMaximumSignificantDigits() - 1</code>. For example, the
 * pattern <code>"@@###E0"</code> is equivalent to <code>"0.0###E0"</code>.
 *
 * <li>If signficant digits are in use, then the integer and fraction
 * digit counts, as set via the API, are ignored.  If significant
 * digits are not in use, then the signficant digit counts, as set via
 * the API, are ignored.
 *
 * </ul>
 * 
 * <h4>
 * Padding</h4>
 *
 * <p><code>DecimalFormat</code> supports padding the result of
 * {@link #format} to a specific width.  Padding may be specified either
 * through the API or through the pattern syntax.  In a pattern the pad escape
 * character, followed by a single pad character, causes padding to be parsed
 * and formatted.  The pad escape character is '*' in unlocalized patterns, and
 * can be localized using {@link DecimalFormatSymbols#setPadEscape}.  For
 * example, <code>"$*x#,##0.00"</code> formats 123 to <code>"$xx123.00"</code>,
 * and 1234 to <code>"$1,234.00"</code>.
 *
 * <ul>
 * <li>When padding is in effect, the width of the positive subpattern,
 * including prefix and suffix, determines the format width.  For example, in
 * the pattern <code>"* #0 o''clock"</code>, the format width is 10.
 *
 * <li>The width is counted in 16-bit code units (Java <code>char</code>s).
 *
 * <li>Some parameters which usually do not matter have meaning when padding is
 * used, because the pattern width is significant with padding.  In the pattern
 * "* ##,##,#,##0.##", the format width is 14.  The initial characters "##,##,"
 * do not affect the grouping size or maximum integer digits, but they do affect
 * the format width.
 *
 * <li>Padding may be inserted at one of four locations: before the prefix,
 * after the prefix, before the suffix, or after the suffix.  If padding is
 * specified in any other location, {@link #applyPattern} throws an {@link
 * IllegalArgumentException}.  If there is no prefix, before the
 * prefix and after the prefix are equivalent, likewise for the suffix.
 *
 * <li>When specified in a pattern, the 16-bit <code>char</code> immediately
 * following the pad escape is the pad character. This may be any character,
 * including a special pattern character. That is, the pad escape
 * <em>escapes</em> the following character. If there is no character after
 * the pad escape, then the pattern is illegal.
 *
 * </ul>
 *
 * <p>
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
 * {@link com.ibm.icu.math.BigDecimal} documentation for a description of the
 * modes.  Rounding increments specified in patterns use the default mode,
 * {@link com.ibm.icu.math.BigDecimal#ROUND_HALF_EVEN}.
 *
 * <li>Some locales use rounding in their currency formats to reflect the
 * smallest currency denomination.
 *
 * <li>In a pattern, digits '1' through '9' specify rounding, but otherwise
 * behave identically to digit '0'.
 * </ul>
 *
 * <h4>Synchronization</h4>
 *
 * <p><code>DecimalFormat</code> objects are not synchronized.  Multiple
 * threads should not access one formatter concurrently.
 *
 * @see          java.text.Format
 * @see          NumberFormat
 * @author       Mark Davis
 * @author       Alan Liu
 * @stable ICU 2.0
 */
public class DecimalFormat extends NumberFormat {

    /**
     * Create a DecimalFormat using the default pattern and symbols for the default locale. This is a convenient way to
     * obtain a DecimalFormat when internationalization is not the main concern.
     * <p>
     * To obtain standard formats for a given locale, use the factory methods on NumberFormat such as getNumberInstance.
     * These factories will return the most appropriate sub-class of NumberFormat for a given locale.
     * 
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     * @stable ICU 2.0
     */
    public DecimalFormat() {
        ULocale def = ULocale.getDefault();
        String pattern = getPattern(def, 0);
        // Always applyPattern after the symbols are set
        this.symbols = new DecimalFormatSymbols(def);
        setCurrency(Currency.getInstance(def));
        applyPatternWithoutExpandAffix(pattern, false);
        if (currencySignCount == CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT) {
            currencyPluralInfo = new CurrencyPluralInfo(def);
            // the exact pattern is not known until the plural count is known.
            // so, no need to expand affix now.
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    /**
     * Create a DecimalFormat from the given pattern and the symbols for the default locale. This is a convenient way to
     * obtain a DecimalFormat when internationalization is not the main concern.
     * <p>
     * To obtain standard formats for a given locale, use the factory methods on NumberFormat such as getNumberInstance.
     * These factories will return the most appropriate sub-class of NumberFormat for a given locale.
     * 
     * @param pattern
     *            A non-localized pattern string.
     * @exception IllegalArgumentException
     *                if the given pattern is invalid.
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     * @stable ICU 2.0
     */
    public DecimalFormat(String pattern) {
        // Always applyPattern after the symbols are set
        ULocale def = ULocale.getDefault();
        this.symbols = new DecimalFormatSymbols(def);
        setCurrency(Currency.getInstance(def));
        applyPatternWithoutExpandAffix(pattern, false);
        if (currencySignCount == CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT) {
            currencyPluralInfo = new CurrencyPluralInfo(def);
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    /**
     * Create a DecimalFormat from the given pattern and symbols. Use this constructor when you need to completely
     * customize the behavior of the format.
     * <p>
     * To obtain standard formats for a given locale, use the factory methods on NumberFormat such as getInstance or
     * getCurrencyInstance. If you need only minor adjustments to a standard format, you can modify the format returned
     * by a NumberFormat factory method.
     * 
     * @param pattern
     *            a non-localized pattern string
     * @param symbols
     *            the set of symbols to be used
     * @exception IllegalArgumentException
     *                if the given pattern is invalid
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     * @see DecimalFormatSymbols
     * @stable ICU 2.0
     */
    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        createFromPatternAndSymbols(pattern, symbols);
    }

    private void createFromPatternAndSymbols(String pattern, DecimalFormatSymbols inputSymbols) {
        // Always applyPattern after the symbols are set
        symbols = (DecimalFormatSymbols) inputSymbols.clone();
        setCurrencyForSymbols();
        applyPatternWithoutExpandAffix(pattern, false);
        if (currencySignCount == CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT) {
            currencyPluralInfo = new CurrencyPluralInfo(symbols.getLocale());
        } else {
            expandAffixAdjustWidth(null);
        }
    }

    /**
     * Create a DecimalFormat from the given pattern, symbols, information used for currency plural format, and format
     * style. Use this constructor when you need to completely customize the behavior of the format.
     * <p>
     * To obtain standard formats for a given locale, use the factory methods on NumberFormat such as getInstance or
     * getCurrencyInstance.
     * <p>
     * If you need only minor adjustments to a standard format, you can modify the format returned by a NumberFormat
     * factory method using the setters.
     * <p>
     * If you want to completely customize a decimal format, using your own DecimalFormatSymbols (such as group
     * separators) and your own information for currency plural formatting (such as plural rule and currency plural
     * patterns), you can use this constructor.
     * <p>
     * 
     * @param pattern
     *            a non-localized pattern string
     * @param symbols
     *            the set of symbols to be used
     * @param infoInput
     *            the information used for currency plural format, including currency plural patterns and plural rules.
     * @param style
     *            the decimal formatting style, it is one of the following values: NumberFormat.NUMBERSTYLE;
     *            NumberFormat.CURRENCYSTYLE; NumberFormat.PERCENTSTYLE; NumberFormat.SCIENTIFICSTYLE;
     *            NumberFormat.INTEGERSTYLE; NumberFormat.ISOCURRENCYSTYLE; NumberFormat.PLURALCURRENCYSTYLE;
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public DecimalFormat(String pattern, DecimalFormatSymbols symbols, CurrencyPluralInfo infoInput, int style) {
        CurrencyPluralInfo info = infoInput;
        if (style == NumberFormat.PLURALCURRENCYSTYLE) {
            info = (CurrencyPluralInfo) infoInput.clone();
        }
        create(pattern, symbols, info, style);
    }

    private void create(String pattern, DecimalFormatSymbols inputSymbols, CurrencyPluralInfo info, int inputStyle) {
        if (inputStyle != NumberFormat.PLURALCURRENCYSTYLE) {
            createFromPatternAndSymbols(pattern, inputSymbols);
        } else {
            // Always applyPattern after the symbols are set
            symbols = (DecimalFormatSymbols) inputSymbols.clone();
            currencyPluralInfo = info;
            // the pattern used in format is not fixed until formatting,
            // in which, the number is known and
            // will be used to pick the right pattern based on plural count.
            // Here, set the pattern as the pattern of plural count == "other".
            // For most locale, the patterns are probably the same for all
            // plural count. If not, the right pattern need to be re-applied
            // during format.
            String currencyPluralPatternForOther = currencyPluralInfo.getCurrencyPluralPattern("other");
            applyPatternWithoutExpandAffix(currencyPluralPatternForOther, false);
            setCurrencyForSymbols();
        }
        style = inputStyle;
    }

    /*
     * Create a DecimalFormat for currency plural format from the given pattern, symbols, and style.
     */
    DecimalFormat(String pattern, DecimalFormatSymbols inputSymbols, int style) {
        CurrencyPluralInfo info = null;
        if (style == NumberFormat.PLURALCURRENCYSTYLE) {
            info = new CurrencyPluralInfo(inputSymbols.getLocale());
        }
        create(pattern, inputSymbols, info, style);
    }

    /**
     * @stable ICU 2.0
     */
    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    // [Spark/CDL] The actual method to format number. If boolean value
    // parseAttr == true, then attribute information will be recorded.
    private StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);

        if (Double.isNaN(number)) {
            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                fieldPosition.setBeginIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setBeginIndex(result.length());
            }

            result.append(symbols.getNaN());
            // [Spark/CDL] Add attribute for NaN here.
            // result.append(symbols.getNaN());
            if (parseAttr) {
                addAttribute(Field.INTEGER, result.length() - symbols.getNaN().length(), result.length());
            }
            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                fieldPosition.setEndIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setEndIndex(result.length());
            }

            addPadding(result, fieldPosition, 0, 0);
            return result;
        }

        // Do this BEFORE checking to see if value is infinite or negative!
        if (multiplier != 1)
            number *= multiplier;

        /*
         * Detecting whether a double is negative is easy with the exception of the value -0.0. This is a double which
         * has a zero mantissa (and exponent), but a negative sign bit. It is semantically distinct from a zero with a
         * positive sign bit, and this distinction is important to certain kinds of computations. However, it's a little
         * tricky to detect, since (-0.0 == 0.0) and !(-0.0 < 0.0). How then, you may ask, does it behave distinctly
         * from +0.0? Well, 1/(-0.0) == -Infinity. Proper detection of -0.0 is needed to deal with the issues raised by
         * bugs 4106658, 4106667, and 4147706. Liu 7/6/98.
         */
        boolean isNegative = (number < 0.0) || (number == 0.0 && 1 / number < 0.0);
        if (isNegative)
            number = -number;

        // Apply rounding after multiplier
        if (roundingDouble > 0.0) {
            // number = roundingDouble
            // * round(number / roundingDouble, roundingMode, isNegative);
            double newNumber = round(number, roundingDouble, roundingDoubleReciprocal, roundingMode, isNegative);
            if (newNumber == 0.0 && number != newNumber)
                isNegative = false; // if we touched it, then make zero be zero.
            number = newNumber;
        }

        if (Double.isInfinite(number)) {
            int prefixLen = appendAffix(result, isNegative, true, parseAttr);

            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                fieldPosition.setBeginIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setBeginIndex(result.length());
            }

            // [Spark/CDL] Add attribute for infinity here.
            result.append(symbols.getInfinity());
            if (parseAttr) {
                addAttribute(Field.INTEGER, result.length() - symbols.getInfinity().length(), result.length());
            }
            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                fieldPosition.setEndIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setEndIndex(result.length());
            }

            int suffixLen = appendAffix(result, isNegative, false, parseAttr);

            addPadding(result, fieldPosition, prefixLen, suffixLen);
            return result;
        }

        // At this point we are guaranteed a nonnegative finite
        // number.
        synchronized (digitList) {
            digitList.set(number, precision(false), !useExponentialNotation && !areSignificantDigitsUsed());
            return subformat(number, result, fieldPosition, isNegative, false, parseAttr);
        }
    }

    /**
     * Round a double value to the nearest multiple of the given rounding increment, according to the given mode. This
     * is equivalent to rounding value/roundingInc to the nearest integer, according to the given mode, and returning
     * that integer * roundingInc. Note this is changed from the version in 2.4, since division of doubles have
     * inaccuracies. jitterbug 1871.
     * 
     * @param number
     *            the absolute value of the number to be rounded
     * @param roundingInc
     *            the rounding increment
     * @param roundingIncReciprocal
     *            if non-zero, is the
     * @param mode
     *            a BigDecimal rounding mode
     * @param isNegative
     *            true if the number to be rounded is negative
     * @return the absolute value of the rounded result
     */
    private static double round(double number, double roundingInc, double roundingIncReciprocal, int mode,
            boolean isNegative) {

        double div = roundingIncReciprocal == 0.0 ? number / roundingInc : number * roundingIncReciprocal;

        // do the absolute cases first

        switch (mode) {
        case BigDecimal.ROUND_CEILING:
            div = (isNegative ? Math.floor(div + epsilon) : Math.ceil(div - epsilon));
            break;
        case BigDecimal.ROUND_FLOOR:
            div = (isNegative ? Math.ceil(div - epsilon) : Math.floor(div + epsilon));
            break;
        case BigDecimal.ROUND_DOWN:
            div = (Math.floor(div + epsilon));
            break;
        case BigDecimal.ROUND_UP:
            div = (Math.ceil(div - epsilon));
            break;
        case BigDecimal.ROUND_UNNECESSARY:
            if (div != Math.floor(div)) {
                throw new ArithmeticException("Rounding necessary");
            }
            return number;
        default:

            // Handle complex cases, where the choice depends on the closer value.

            // We figure out the distances to the two possible values, ceiling and floor.
            // We then go for the diff that is smaller.
            // Only if they are equal does the mode matter.

            double ceil = Math.ceil(div);
            double ceildiff = ceil - div; // (ceil * roundingInc) - number;
            double floor = Math.floor(div);
            double floordiff = div - floor; // number - (floor * roundingInc);

            // Note that the diff values were those mapped back to the "normal" space
            // by using the roundingInc. I don't have access to the original author of the code
            // but suspect that that was to produce better result in edge cases because of machine
            // precision, rather than simply using the difference between, say, ceil and div.
            // However, it didn't work in all cases. Am trying instead using an epsilon value.

            switch (mode) {
            case BigDecimal.ROUND_HALF_EVEN:
                // We should be able to just return Math.rint(a), but this
                // doesn't work in some VMs.
                // if one is smaller than the other, take the corresponding side
                if (floordiff + epsilon < ceildiff) {
                    div = floor;
                } else if (ceildiff + epsilon < floordiff) {
                    div = ceil;
                } else { // they are equal, so we want to round to whichever is even
                    double testFloor = floor / 2;
                    div = (testFloor == Math.floor(testFloor)) ? floor : ceil;
                }
                break;
            case BigDecimal.ROUND_HALF_DOWN:
                div = ((floordiff <= ceildiff + epsilon) ? floor : ceil);
                break;
            case BigDecimal.ROUND_HALF_UP:
                div = ((ceildiff <= floordiff + epsilon) ? ceil : floor);
                break;
            default:
                throw new IllegalArgumentException("Invalid rounding mode: " + mode);
            }
        }
        number = roundingIncReciprocal == 0.0 ? div * roundingInc : div / roundingIncReciprocal;
        return number;
    }

    private static double epsilon = 0.00000000001;

    /**
     * @stable ICU 2.0
     */
    // [Spark/CDL] Delegate to format_long_StringBuffer_FieldPosition_boolean
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);

        // If we are to do rounding, we need to move into the BigDecimal
        // domain in order to do divide/multiply correctly.
        if (roundingIncrementICU != null) {
            return format(BigDecimal.valueOf(number), result, fieldPosition);
        }

        boolean isNegative = (number < 0);
        if (isNegative)
            number = -number;

        // In general, long values always represent real finite numbers, so
        // we don't have to check for +/- Infinity or NaN. However, there
        // is one case we have to be careful of: The multiplier can push
        // a number near MIN_VALUE or MAX_VALUE outside the legal range. We
        // check for this before multiplying, and if it happens we use BigInteger
        // instead.
        if (multiplier != 1) {
            boolean tooBig = false;
            if (number < 0) { // This can only happen if number == Long.MIN_VALUE
                long cutoff = Long.MIN_VALUE / multiplier;
                tooBig = (number <= cutoff); // number == cutoff can only happen if multiplier == -1
            } else {
                long cutoff = Long.MAX_VALUE / multiplier;
                tooBig = (number > cutoff);
            }
            if (tooBig) {
                // [Spark/CDL] Use
                // format_BigInteger_StringBuffer_FieldPosition_boolean instead
                // parseAttr is used to judge whether to synthesize attributes.
                return format(BigInteger.valueOf(isNegative ? -number : number), result, fieldPosition, parseAttr);
            }
        }

        number *= multiplier;
        synchronized (digitList) {
            digitList.set(number, precision(true));
            return subformat(number, result, fieldPosition, isNegative, true, parseAttr);
        }
    }

    /**
     * Format a BigInteger number.
     * 
     * @stable ICU 2.0
     */
    public StringBuffer format(BigInteger number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(BigInteger number, StringBuffer result, FieldPosition fieldPosition, boolean parseAttr) {
        // If we are to do rounding, we need to move into the BigDecimal
        // domain in order to do divide/multiply correctly.
        if (roundingIncrementICU != null) {
            return format(new BigDecimal(number), result, fieldPosition);
        }

        if (multiplier != 1) {
            number = number.multiply(BigInteger.valueOf(multiplier));
        }

        // At this point we are guaranteed a nonnegative finite
        // number.
        synchronized (digitList) {
            digitList.set(number, precision(true));
            return subformat(number.intValue(), result, fieldPosition, number.signum() < 0, true, parseAttr);
        }
    }

    /**
     * Format a BigDecimal number.
     * 
     * @stable ICU 2.0
     */
    public StringBuffer format(java.math.BigDecimal number, StringBuffer result, FieldPosition fieldPosition) {
        return format(number, result, fieldPosition, false);
    }

    private StringBuffer format(java.math.BigDecimal number, StringBuffer result, FieldPosition fieldPosition,
            boolean parseAttr) {
        if (multiplier != 1) {
            number = number.multiply(java.math.BigDecimal.valueOf(multiplier));
        }

        if (roundingIncrement != null) {
            number = number.divide(roundingIncrement, 0, roundingMode).multiply(roundingIncrement);
        }

        synchronized (digitList) {
            digitList.set(number, precision(false), !useExponentialNotation && !areSignificantDigitsUsed());
            return subformat(number.doubleValue(), result, fieldPosition, number.signum() < 0, false, parseAttr);
        }
    }

    /**
     * Format a BigDecimal number.
     * 
     * @stable ICU 2.0
     */
    public StringBuffer format(BigDecimal number, StringBuffer result, FieldPosition fieldPosition) {
        /*
         * This method is just a copy of the corresponding java.math.BigDecimal method for now. It isn't very efficient
         * since it must create a conversion object to do math on the rounding increment. In the future we may try to
         * clean this up, or even better, limit our support to just one flavor of BigDecimal.
         */
        if (multiplier != 1) {
            number = number.multiply(BigDecimal.valueOf(multiplier), mathContext);
        }

        if (roundingIncrementICU != null) {
            number = number.divide(roundingIncrementICU, 0, roundingMode).multiply(roundingIncrementICU, mathContext);
        }

        synchronized (digitList) {
            digitList.set(number, precision(false), !useExponentialNotation && !areSignificantDigitsUsed());
            return subformat(number.doubleValue(), result, fieldPosition, number.signum() < 0, false, false);
        }
    }

    /**
     * Return true if a grouping separator belongs at the given position, based on whether grouping is in use and the
     * values of the primary and secondary grouping interval.
     * 
     * @param pos
     *            the number of integer digits to the right of the current position. Zero indicates the position after
     *            the rightmost integer digit.
     * @return true if a grouping character belongs at the current position.
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
     * Return the number of fraction digits to display, or the total number of digits for significant digit formats and
     * exponential formats.
     */
    private int precision(boolean isIntegral) {
        if (areSignificantDigitsUsed()) {
            return getMaximumSignificantDigits();
        } else if (useExponentialNotation) {
            return getMinimumIntegerDigits() + getMaximumFractionDigits();
        } else {
            return isIntegral ? 0 : getMaximumFractionDigits();
        }
    }

    private StringBuffer subformat(int number, StringBuffer result, FieldPosition fieldPosition, boolean isNegative,
            boolean isInteger, boolean parseAttr) {
        if (currencySignCount == CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT) {
            return subformat(currencyPluralInfo.select(number), result, fieldPosition, isNegative, isInteger, parseAttr);
        } else {
            return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
        }
    }

    private StringBuffer subformat(double number, StringBuffer result, FieldPosition fieldPosition, boolean isNegative,
            boolean isInteger, boolean parseAttr) {
        if (currencySignCount == CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT) {
            return subformat(currencyPluralInfo.select(number), result, fieldPosition, isNegative, isInteger, parseAttr);
        } else {
            return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
        }
    }

    private StringBuffer subformat(String pluralCount, StringBuffer result, FieldPosition fieldPosition,
            boolean isNegative, boolean isInteger, boolean parseAttr) {
        // There are 2 ways to activate currency plural format:
        // by applying a pattern with 3 currency sign directly,
        // or by instantiate a decimal formatter using PLURALCURRENCYSTYLE.
        // For both cases, the number of currency sign in the pattern is 3.
        // Even if the number of currency sign in the pattern is 3,
        // it does not mean we need to reset the pattern.
        // For 1st case, we do not need to reset pattern.
        // For 2nd case, we might need to reset pattern,
        // if the default pattern (corresponding to plural count 'other')
        // we use is different from the pattern based on 'pluralCount'.
        // 
        // style is only valid when decimal formatter is constructed through
        // DecimalFormat(pattern, symbol, style)
        if (style == NumberFormat.PLURALCURRENCYSTYLE) {
            // May need to reset pattern if the style is PLURALCURRENCYSTYLE.
            String currencyPluralPattern = currencyPluralInfo.getCurrencyPluralPattern(pluralCount);
            if (formatPattern.equals(currencyPluralPattern) == false) {
                applyPatternWithoutExpandAffix(currencyPluralPattern, false);
            }
        }
        // Expand the affix to the right name according to
        // the plural rule.
        // This is only used for currency plural formatting.
        // Currency plural name is not a fixed static one,
        // it is a dynamic name based on the currency plural count.
        // So, the affixes need to be expanded here.
        // For other cases, the affix is a static one based on pattern alone,
        // and it is already expanded during applying pattern,
        // or setDecimalFormatSymbols, or setCurrency.
        expandAffixAdjustWidth(pluralCount);
        return subformat(result, fieldPosition, isNegative, isInteger, parseAttr);
    }

    /**
     * Complete the formatting of a finite number. On entry, the digitList must be filled in with the correct digits.
     */
    private StringBuffer subformat(StringBuffer result, FieldPosition fieldPosition, boolean isNegative,
            boolean isInteger, boolean parseAttr) {
        // NOTE: This isn't required anymore because DigitList takes care of this.
        //
        // // The negative of the exponent represents the number of leading
        // // zeros between the decimal and the first non-zero digit, for
        // // a value < 0.1 (e.g., for 0.00123, -fExponent == 2). If this
        // // is more than the maximum fraction digits, then we have an underflow
        // // for the printed representation. We recognize this here and set
        // // the DigitList representation to zero in this situation.
        //
        // if (-digitList.decimalAt >= getMaximumFractionDigits())
        // {
        // digitList.count = 0;
        // }

        int i;
        char zero = symbols.getZeroDigit();
        int zeroDelta = zero - '0'; // '0' is the DigitList representation of zero
        char grouping = currencySignCount > 0 ? symbols.getMonetaryGroupingSeparator() : symbols.getGroupingSeparator();
        char decimal = currencySignCount > 0 ? symbols.getMonetaryDecimalSeparator() : symbols.getDecimalSeparator();
        boolean useSigDig = areSignificantDigitsUsed();
        int maxIntDig = getMaximumIntegerDigits();
        int minIntDig = getMinimumIntegerDigits();

        /*
         * Per bug 4147706, DecimalFormat must respect the sign of numbers which format as zero. This allows sensible
         * computations and preserves relations such as signum(1/x) = signum(x), where x is +Infinity or -Infinity.
         * Prior to this fix, we always formatted zero values as if they were positive. Liu 7/6/98.
         */
        if (digitList.isZero()) {
            digitList.decimalAt = 0; // Normalize
        }

        int prefixLen = appendAffix(result, isNegative, true, parseAttr);

        if (useExponentialNotation) {
            // Record field information for caller.
            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                fieldPosition.setBeginIndex(result.length());
                fieldPosition.setEndIndex(-1);
            } else if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD) {
                fieldPosition.setBeginIndex(-1);
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setBeginIndex(result.length());
                fieldPosition.setEndIndex(-1);
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
                fieldPosition.setBeginIndex(-1);
            }

            // [Spark/CDL]
            // the begin index of integer part
            // the end index of integer part
            // the begin index of fractional part
            int intBegin = result.length();
            int intEnd = -1;
            int fracBegin = -1;
            int minFracDig = 0;
            if (useSigDig) {
                maxIntDig = minIntDig = 1;
                minFracDig = getMinimumSignificantDigits() - 1;
            } else {
                minFracDig = getMinimumFractionDigits();
                if (maxIntDig > MAX_SCIENTIFIC_INTEGER_DIGITS) {
                    maxIntDig = 1;
                    if (maxIntDig < minIntDig) {
                        maxIntDig = minIntDig;
                    }
                }
                if (maxIntDig > minIntDig) {
                    minIntDig = 1;
                }
            }

            // Minimum integer digits are handled in exponential format by
            // adjusting the exponent. For example, 0.01234 with 3 minimum
            // integer digits is "123.4E-4".

            // Maximum integer digits are interpreted as indicating the
            // repeating range. This is useful for engineering notation, in
            // which the exponent is restricted to a multiple of 3. For
            // example, 0.01234 with 3 maximum integer digits is "12.34e-3".
            // If maximum integer digits are defined and are larger than
            // minimum integer digits, then minimum integer digits are
            // ignored.

            int exponent = digitList.decimalAt;
            if (maxIntDig > 1 && maxIntDig != minIntDig) {
                // A exponent increment is defined; adjust to it.
                exponent = (exponent > 0) ? (exponent - 1) / maxIntDig : (exponent / maxIntDig) - 1;
                exponent *= maxIntDig;
            } else {
                // No exponent increment is defined; use minimum integer digits.
                // If none is specified, as in "#E0", generate 1 integer digit.
                exponent -= (minIntDig > 0 || minFracDig > 0) ? minIntDig : 1;
            }

            // We now output a minimum number of digits, and more if there
            // are more digits, up to the maximum number of digits. We
            // place the decimal point after the "integer" digits, which
            // are the first (decimalAt - exponent) digits.
            int minimumDigits = minIntDig + minFracDig;
            // The number of integer digits is handled specially if the number
            // is zero, since then there may be no digits.
            int integerDigits = digitList.isZero() ? minIntDig : digitList.decimalAt - exponent;
            int totalDigits = digitList.count;
            if (minimumDigits > totalDigits)
                totalDigits = minimumDigits;
            if (integerDigits > totalDigits)
                totalDigits = integerDigits;

            for (i = 0; i < totalDigits; ++i) {
                if (i == integerDigits) {
                    // Record field information for caller.
                    if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                        fieldPosition.setEndIndex(result.length());
                    } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                        fieldPosition.setEndIndex(result.length());
                    }

                    // [Spark/CDL] Add attribute for integer part
                    if (parseAttr) {
                        intEnd = result.length();
                        addAttribute(Field.INTEGER, intBegin, result.length());
                    }
                    result.append(decimal);
                    // [Spark/CDL] Add attribute for decimal separator
                    if (parseAttr) {
                        // Length of decimal separator is 1.
                        int decimalSeparatorBegin = result.length() - 1;
                        addAttribute(Field.DECIMAL_SEPARATOR, decimalSeparatorBegin, result.length());
                        fracBegin = result.length();
                    }
                    // Record field information for caller.
                    if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD) {
                        fieldPosition.setBeginIndex(result.length());
                    } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
                        fieldPosition.setBeginIndex(result.length());
                    }
                }
                result.append((i < digitList.count) ? (char) (digitList.digits[i] + zeroDelta) : zero);
            }

            // For ICU compatibility and format 0 to 0E0 with pattern "#E0" [Richard/GCL]
            if (digitList.isZero() && (totalDigits == 0)) {
                result.append(zero);
            }

            // Record field information
            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                if (fieldPosition.getEndIndex() < 0) {
                    fieldPosition.setEndIndex(result.length());
                }
            } else if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD) {
                if (fieldPosition.getBeginIndex() < 0) {
                    fieldPosition.setBeginIndex(result.length());
                }
                fieldPosition.setEndIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                if (fieldPosition.getEndIndex() < 0) {
                    fieldPosition.setEndIndex(result.length());
                }
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
                if (fieldPosition.getBeginIndex() < 0) {
                    fieldPosition.setBeginIndex(result.length());
                }
                fieldPosition.setEndIndex(result.length());
            }

            // [Spark/CDL] Calcuate the end index of integer part and fractional
            // part if they are not properly processed yet.
            if (parseAttr) {
                if (intEnd < 0) {
                    addAttribute(Field.INTEGER, intBegin, result.length());
                }
                if (fracBegin > 0) {
                    addAttribute(Field.FRACTION, fracBegin, result.length());
                }
            }

            // The exponent is output using the pattern-specified minimum
            // exponent digits. There is no maximum limit to the exponent
            // digits, since truncating the exponent would result in an
            // unacceptable inaccuracy.
            result.append(symbols.getExponentSeparator());
            // [Spark/CDL] For exponent symbol, add an attribute.
            if (parseAttr) {
                addAttribute(Field.EXPONENT_SYMBOL, result.length() - symbols.getExponentSeparator().length(), result
                        .length());
            }
            // For zero values, we force the exponent to zero. We
            // must do this here, and not earlier, because the value
            // is used to determine integer digit count above.
            if (digitList.isZero())
                exponent = 0;

            boolean negativeExponent = exponent < 0;
            if (negativeExponent) {
                exponent = -exponent;
                result.append(symbols.getMinusSign());
                // [Spark/CDL] If exponent has sign, then add an exponent sign
                // attribute.
                if (parseAttr) {
                    // Length of exponent sign is 1.
                    addAttribute(Field.EXPONENT_SIGN, result.length() - 1, result.length());
                }
            } else if (exponentSignAlwaysShown) {
                result.append(symbols.getPlusSign());
                // [Spark/CDL] Add an plus sign attribute.
                if (parseAttr) {
                    // Length of exponent sign is 1.
                    int expSignBegin = result.length() - 1;
                    addAttribute(Field.EXPONENT_SIGN, expSignBegin, result.length());
                }
            }
            int expBegin = result.length();
            digitList.set(exponent);
            {
                int expDig = minExponentDigits;
                if (useExponentialNotation && expDig < 1) {
                    expDig = 1;
                }
                for (i = digitList.decimalAt; i < expDig; ++i)
                    result.append(zero);
            }
            for (i = 0; i < digitList.decimalAt; ++i) {
                result.append((i < digitList.count) ? (char) (digitList.digits[i] + zeroDelta) : zero);
            }
            // [Spark/CDL] Add attribute for exponent part.
            if (parseAttr) {
                addAttribute(Field.EXPONENT, expBegin, result.length());
            }
        } else {
            // [Spark/CDL] Record the integer start index.
            int intBegin = result.length();
            // Record field information for caller.
            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                fieldPosition.setBeginIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setBeginIndex(result.length());
            }

            int sigCount = 0;
            int minSigDig = getMinimumSignificantDigits();
            int maxSigDig = getMaximumSignificantDigits();
            if (!useSigDig) {
                minSigDig = 0;
                maxSigDig = Integer.MAX_VALUE;
            }

            // Output the integer portion. Here 'count' is the total
            // number of integer digits we will display, including both
            // leading zeros required to satisfy getMinimumIntegerDigits,
            // and actual digits present in the number.
            int count = useSigDig ? Math.max(1, digitList.decimalAt) : minIntDig;
            if (digitList.decimalAt > 0 && count < digitList.decimalAt) {
                count = digitList.decimalAt;
            }

            // Handle the case where getMaximumIntegerDigits() is smaller
            // than the real number of integer digits. If this is so, we
            // output the least significant max integer digits. For example,
            // the value 1997 printed with 2 max integer digits is just "97".

            int digitIndex = 0; // Index into digitList.fDigits[]
            if (count > maxIntDig && maxIntDig >= 0) {
                count = maxIntDig;
                digitIndex = digitList.decimalAt - count;
            }

            int sizeBeforeIntegerPart = result.length();
            for (i = count - 1; i >= 0; --i) {
                if (i < digitList.decimalAt && digitIndex < digitList.count && sigCount < maxSigDig) {
                    // Output a real digit
                    byte d = digitList.digits[digitIndex++];
                    result.append((char) (d + zeroDelta));
                    ++sigCount;
                } else {
                    // Output a zero (leading or trailing)
                    result.append(zero);
                    if (sigCount > 0) {
                        ++sigCount;
                    }
                }

                // Output grouping separator if necessary.
                if (isGroupingPosition(i)) {
                    result.append(grouping);
                    // [Spark/CDL] Add grouping separator attribute here.
                    if (parseAttr) {
                        // Length of grouping separator is 1.
                        addAttribute(Field.GROUPING_SEPARATOR, result.length() - 1, result.length());
                    }
                }
            }

            // Record field information for caller.
            if (fieldPosition.getField() == NumberFormat.INTEGER_FIELD) {
                fieldPosition.setEndIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.INTEGER) {
                fieldPosition.setEndIndex(result.length());
            }

            // Determine whether or not there are any printable fractional
            // digits. If we've used up the digits we know there aren't.
            boolean fractionPresent = (!isInteger && digitIndex < digitList.count)
                    || (useSigDig ? (sigCount < minSigDig) : (getMinimumFractionDigits() > 0));

            // If there is no fraction present, and we haven't printed any
            // integer digits, then print a zero. Otherwise we won't print
            // _any_ digits, and we won't be able to parse this string.
            if (!fractionPresent && result.length() == sizeBeforeIntegerPart)
                result.append(zero);
            // [Spark/CDL] Add attribute for integer part.
            if (parseAttr) {
                addAttribute(Field.INTEGER, intBegin, result.length());
            }
            // Output the decimal separator if we always do so.
            if (decimalSeparatorAlwaysShown || fractionPresent) {
                result.append(decimal);
                // [Spark/CDL] Add attribute for decimal separator
                if (parseAttr) {
                    addAttribute(Field.DECIMAL_SEPARATOR, result.length() - 1, result.length());
                }
            }

            // Record field information for caller.
            if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD) {
                fieldPosition.setBeginIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
                fieldPosition.setBeginIndex(result.length());
            }

            // [Spark/CDL] Record the begin index of fraction part.
            int fracBegin = result.length();

            count = useSigDig ? Integer.MAX_VALUE : getMaximumFractionDigits();
            if (useSigDig && (sigCount == maxSigDig || (sigCount >= minSigDig && digitIndex == digitList.count))) {
                count = 0;
            }
            for (i = 0; i < count; ++i) {
                // Here is where we escape from the loop. We escape
                // if we've output the maximum fraction digits
                // (specified in the for expression above). We also
                // stop when we've output the minimum digits and
                // either: we have an integer, so there is no
                // fractional stuff to display, or we're out of
                // significant digits.
                if (!useSigDig && i >= getMinimumFractionDigits() && (isInteger || digitIndex >= digitList.count)) {
                    break;
                }

                // Output leading fractional zeros. These are zeros
                // that come after the decimal but before any
                // significant digits. These are only output if
                // abs(number being formatted) < 1.0.
                if (-1 - i > (digitList.decimalAt - 1)) {
                    result.append(zero);
                    continue;
                }

                // Output a digit, if we have any precision left, or a
                // zero if we don't. We don't want to output noise digits.
                if (!isInteger && digitIndex < digitList.count) {
                    result.append((char) (digitList.digits[digitIndex++] + zeroDelta));
                } else {
                    result.append(zero);
                }

                // If we reach the maximum number of significant
                // digits, or if we output all the real digits and
                // reach the minimum, then we are done.
                ++sigCount;
                if (useSigDig && (sigCount == maxSigDig || (digitIndex == digitList.count && sigCount >= minSigDig))) {
                    break;
                }
            }

            // Record field information for caller.
            if (fieldPosition.getField() == NumberFormat.FRACTION_FIELD) {
                fieldPosition.setEndIndex(result.length());
            } else if (fieldPosition.getFieldAttribute() == NumberFormat.Field.FRACTION) {
                fieldPosition.setEndIndex(result.length());
            }

            // [Spark/CDL] Add attribute information if necessary.
            if (parseAttr && (decimalSeparatorAlwaysShown || fractionPresent)) {
                addAttribute(Field.FRACTION, fracBegin, result.length());
            }
        }

        int suffixLen = appendAffix(result, isNegative, false, parseAttr);

        addPadding(result, fieldPosition, prefixLen, suffixLen);
        return result;
    }

    private final void addPadding(StringBuffer result, FieldPosition fieldPosition, int prefixLen, int suffixLen) {
        if (formatWidth > 0) {
            int len = formatWidth - result.length();
            if (len > 0) {
                char[] padding = new char[len];
                for (int i = 0; i < len; ++i) {
                    padding[i] = pad;
                }
                switch (padPosition) {
                case PAD_AFTER_PREFIX:
                    result.insert(prefixLen, padding);
                    break;
                case PAD_BEFORE_PREFIX:
                    result.insert(0, padding);
                    break;
                case PAD_BEFORE_SUFFIX:
                    result.insert(result.length() - suffixLen, padding);
                    break;
                case PAD_AFTER_SUFFIX:
                    result.append(padding);
                    break;
                }
                if (padPosition == PAD_BEFORE_PREFIX || padPosition == PAD_AFTER_PREFIX) {
                    fieldPosition.setBeginIndex(fieldPosition.getBeginIndex() + len);
                    fieldPosition.setEndIndex(fieldPosition.getEndIndex() + len);
                }
            }
        }
    }

    /**
     * Parse the given string, returning a <code>Number</code> object to represent the parsed value. <code>Double</code>
     * objects are returned to represent non-integral values which cannot be stored in a <code>BigDecimal</code>. These
     * are <code>NaN</code>, infinity, -infinity, and -0.0. If {@link #isParseBigDecimal()} is false (the default), all
     * other values are returned as <code>Long</code>, <code>BigInteger</code>, or <code>BigDecimal</code> values, in
     * that order of preference. If {@link #isParseBigDecimal()} is true, all other values are returned as
     * <code>BigDecimal</code> valuse. If the parse fails, null is returned.
     * 
     * @param text
     *            the string to be parsed
     * @param parsePosition
     *            defines the position where parsing is to begin, and upon return, the position where parsing left off.
     *            If the position has not changed upon return, then parsing failed.
     * @return a <code>Number</code> object with the parsed value or <code>null</code> if the parse failed
     * @stable ICU 2.0
     */
    public Number parse(String text, ParsePosition parsePosition) {
        return (Number) parse(text, parsePosition, false);
    }

    /**
     * Parses text from the given string as a CurrencyAmount. Unlike the parse() method, this method will attempt to
     * parse a generic currency name, searching for a match of this object's locale's currency display names, or for a
     * 3-letter ISO currency code. This method will fail if this format is not a currency format, that is, if it does
     * not contain the currency pattern symbol (U+00A4) in its prefix or suffix.
     * 
     * @param text
     *            the string to parse
     * @param pos
     *            input-output position; on input, the position within text to match; must have 0 <= pos.getIndex() <
     *            text.length(); on output, the position after the last matched character. If the parse fails, the
     *            position in unchanged upon output.
     * @return a CurrencyAmount, or null upon failure
     * @internal
     * @deprecated This API is ICU internal only.
     */
    CurrencyAmount parseCurrency(String text, ParsePosition pos) {
        return (CurrencyAmount) parse(text, pos, true);
    }

    /**
     * Parses the given text as either a Number or a CurrencyAmount.
     * 
     * @param text
     *            the string to parse
     * @param parsePosition
     *            input-output position; on input, the position within text to match; must have 0 <= pos.getIndex() <
     *            text.length(); on output, the position after the last matched character. If the parse fails, the
     *            position in unchanged upon output.
     * @param parseCurrency
     *            if true, a CurrencyAmount is parsed and returned; otherwise a Number is parsed and returned
     * @return a Number or CurrencyAmount or null
     */
    private Object parse(String text, ParsePosition parsePosition, boolean parseCurrency) {
        int backup;
        int i = backup = parsePosition.getIndex();

        // Handle NaN as a special case:

        // Skip padding characters, if around prefix
        if (formatWidth > 0 && (padPosition == PAD_BEFORE_PREFIX || padPosition == PAD_AFTER_PREFIX)) {
            i = skipPadding(text, i);
        }
        if (text.regionMatches(i, symbols.getNaN(), 0, symbols.getNaN().length())) {
            i += symbols.getNaN().length();
            // Skip padding characters, if around suffix
            if (formatWidth > 0 && (padPosition == PAD_BEFORE_SUFFIX || padPosition == PAD_AFTER_SUFFIX)) {
                i = skipPadding(text, i);
            }
            parsePosition.setIndex(i);
            return new Double(Double.NaN);
        }

        // NaN parse failed; start over
        i = backup;

        boolean[] status = new boolean[STATUS_LENGTH];
        Currency[] currency = parseCurrency ? new Currency[1] : null;
        if (currencySignCount > 0) {
            if (!parseForCurrency(text, parsePosition, parseCurrency, currency, status)) {
                return null;
            }
        } else {
            if (!subparse(text, parsePosition, digitList, false, status, currency, negPrefixPattern, negSuffixPattern,
                    posPrefixPattern, posSuffixPattern, Currency.SYMBOL_NAME)) {
                parsePosition.setIndex(backup);
                return null;
            }
        }

        Number n = null;

        // Handle infinity
        if (status[STATUS_INFINITE]) {
            n = new Double(status[STATUS_POSITIVE] ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }

        // Handle underflow
        else if (status[STATUS_UNDERFLOW]) {
            n = status[STATUS_POSITIVE] ? new Double("0.0") : new Double("-0.0");
        }

        // Handle -0.0
        else if (!status[STATUS_POSITIVE] && digitList.isZero()) {
            n = new Double("-0.0");
        }

        else {
            // Do as much of the multiplier conversion as possible without
            // losing accuracy.
            int mult = multiplier; // Don't modify this.multiplier
            while (mult % 10 == 0) {
                --digitList.decimalAt;
                mult /= 10;
            }

            // Handle integral values
            if (!parseBigDecimal && mult == 1 && digitList.isIntegral()) {
                // hack quick long
                if (digitList.decimalAt < 12) { // quick check for long
                    long l = 0;
                    if (digitList.count > 0) {
                        int nx = 0;
                        while (nx < digitList.count) {
                            l = l * 10 + (char) digitList.digits[nx++] - '0';
                        }
                        while (nx++ < digitList.decimalAt) {
                            l *= 10;
                        }
                        if (!status[STATUS_POSITIVE]) {
                            l = -l;
                        }
                    }
                    n = new Long(l);
                } else {
                    BigInteger big = digitList.getBigInteger(status[STATUS_POSITIVE]);
                    n = (big.bitLength() < 64) ? (Number) new Long(big.longValue()) : (Number) big;
                }
            }
            // Handle non-integral values or the case where parseBigDecimal is set
            else {
                BigDecimal big = digitList.getBigDecimalICU(status[STATUS_POSITIVE]);
                n = big;
                if (mult != 1) {
                    n = big.divide(BigDecimal.valueOf(mult), mathContext);
                }
            }
        }

        // Assemble into CurrencyAmount if necessary
        return parseCurrency ? (Object) new CurrencyAmount(n, currency[0]) : (Object) n;
    }

    private boolean parseForCurrency(String text, ParsePosition parsePosition, boolean parseCurrency,
            Currency[] currency, boolean[] status) {
        int origPos = parsePosition.getIndex();
        if (!isReadyForParsing) {
            int savedCurrencySignCount = currencySignCount;
            setupCurrencyAffixForAllPatterns();
            // reset pattern back
            if (savedCurrencySignCount == CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT) {
                applyPatternWithoutExpandAffix(formatPattern, false);
            } else {
                applyPattern(formatPattern, false);
            }
            isReadyForParsing = true;
        }
        int maxPosIndex = origPos;
        int maxErrorPos = -1;
        boolean[] savedStatus = null;
        // First, parse against current pattern.
        // Since current pattern could be set by applyPattern(),
        // it could be an arbitrary pattern, and it may not be the one
        // defined in current locale.
        boolean[] tmpStatus = new boolean[STATUS_LENGTH];
        ParsePosition tmpPos = new ParsePosition(origPos);
        DigitList tmpDigitList = new DigitList();
        boolean found;
        if (style == NumberFormat.PLURALCURRENCYSTYLE) {
            found = subparse(text, tmpPos, tmpDigitList, false, tmpStatus, currency, negPrefixPattern,
                    negSuffixPattern, posPrefixPattern, posSuffixPattern, Currency.LONG_NAME);
        } else {
            found = subparse(text, tmpPos, tmpDigitList, false, tmpStatus, currency, negPrefixPattern,
                    negSuffixPattern, posPrefixPattern, posSuffixPattern, Currency.SYMBOL_NAME);
        }
        if (found) {
            if (tmpPos.getIndex() > maxPosIndex) {
                maxPosIndex = tmpPos.getIndex();
                savedStatus = tmpStatus;
                digitList = tmpDigitList;
            }
        } else {
            maxErrorPos = tmpPos.getErrorIndex();
        }
        // Then, parse against affix patterns.
        // Those are currency patterns and currency plural patterns
        // defined in the locale.
        for (AffixForCurrency affix : affixPatternsForCurrency) {
            tmpStatus = new boolean[STATUS_LENGTH];
            tmpPos = new ParsePosition(origPos);
            tmpDigitList = new DigitList();
            boolean result = subparse(text, tmpPos, tmpDigitList, false, tmpStatus, currency, affix.getNegPrefix(),
                    affix.getNegSuffix(), affix.getPosPrefix(), affix.getPosSuffix(), affix.getPatternType());
            if (result) {
                found = true;
                if (tmpPos.getIndex() > maxPosIndex) {
                    maxPosIndex = tmpPos.getIndex();
                    savedStatus = tmpStatus;
                    digitList = tmpDigitList;
                }
            } else {
                maxErrorPos = (tmpPos.getErrorIndex() > maxErrorPos) ? tmpPos.getErrorIndex() : maxErrorPos;
            }
        }
        // Finally, parse against simple affix to find the match.
        // For example, in TestMonster suite,
        // if the to-be-parsed text is "-\u00A40,00".
        // complexAffixCompare will not find match,
        // since there is no ISO code matches "\u00A4",
        // and the parse stops at "\u00A4".
        // We will just use simple affix comparison (look for exact match)
        // to pass it.
        tmpStatus = new boolean[STATUS_LENGTH];
        tmpPos = new ParsePosition(origPos);
        tmpDigitList = new DigitList();
        int savedCurrencySignCount = currencySignCount;
        // set currencySignCount to 0 so that compareAffix function will
        // fall to compareSimpleAffix path, not compareComplexAffix path.
        currencySignCount = 0;
        boolean result = subparse(text, tmpPos, tmpDigitList, false, tmpStatus, currency, negativePrefix,
                negativeSuffix, positivePrefix, positiveSuffix, Currency.SYMBOL_NAME);
        currencySignCount = savedCurrencySignCount;
        if (result) {
            if (tmpPos.getIndex() > maxPosIndex) {
                maxPosIndex = tmpPos.getIndex();
                savedStatus = tmpStatus;
                digitList = tmpDigitList;
            }
            found = true;
        } else {
            maxErrorPos = (tmpPos.getErrorIndex() > maxErrorPos) ? tmpPos.getErrorIndex() : maxErrorPos;
        }

        if (!found) {
            // parsePosition.setIndex(origPos);
            parsePosition.setErrorIndex(maxErrorPos);
        } else {
            parsePosition.setIndex(maxPosIndex);
            parsePosition.setErrorIndex(-1);
            for (int index = 0; index < STATUS_LENGTH; ++index) {
                status[index] = savedStatus[index];
            }
        }
        return found;
    }

    // Get affix patterns used in locale's currency pattern
    // (NumberPatterns[1]) and currency plural pattern (CurrencyUnitPatterns).
    private void setupCurrencyAffixForAllPatterns() {
        if (currencyPluralInfo == null) {
            currencyPluralInfo = new CurrencyPluralInfo(symbols.getLocale());
        }
        affixPatternsForCurrency = new HashSet<AffixForCurrency>();

        // save the current pattern, since it will be changed by
        // applyPatternWithoutExpandAffix
        String savedFormatPattern = formatPattern;

        // CURRENCYSTYLE and ISOCURRENCYSTYLE should have the same
        // prefix and suffix, so, only need to save one of them.
        // Here, chose onlyApplyPatternWithoutExpandAffix without
        // saving the actualy pattern in 'pattern' data member.
        // TODO: is it uloc?
        applyPatternWithoutExpandAffix(getPattern(symbols.getLocale(), NumberFormat.CURRENCYSTYLE), false);
        AffixForCurrency affixes = new AffixForCurrency(negPrefixPattern, negSuffixPattern, posPrefixPattern,
                posSuffixPattern, Currency.SYMBOL_NAME);
        affixPatternsForCurrency.add(affixes);

        // add plural pattern
        Iterator<String> iter = currencyPluralInfo.pluralPatternIterator();
        Set<String> currencyUnitPatternSet = new HashSet<String>();
        while (iter.hasNext()) {
            String pluralCount = iter.next();
            String currencyPattern = currencyPluralInfo.getCurrencyPluralPattern(pluralCount);
            if (currencyPattern != null && currencyUnitPatternSet.contains(currencyPattern) == false) {
                currencyUnitPatternSet.add(currencyPattern);
                applyPatternWithoutExpandAffix(currencyPattern, false);
                affixes = new AffixForCurrency(negPrefixPattern, negSuffixPattern, posPrefixPattern, posSuffixPattern,
                        Currency.LONG_NAME);
                affixPatternsForCurrency.add(affixes);
            }
        }
        // reset pattern back
        formatPattern = savedFormatPattern;
    }

    private static final int CURRENCY_SIGN_COUNT_IN_SYMBOL_FORMAT = 1;
    private static final int CURRENCY_SIGN_COUNT_IN_ISO_FORMAT = 2;
    private static final int CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT = 3;

    private static final int STATUS_INFINITE = 0;
    private static final int STATUS_POSITIVE = 1;
    private static final int STATUS_UNDERFLOW = 2;
    private static final int STATUS_LENGTH = 3;
    private static final UnicodeSet dotEquivalents = (UnicodeSet) new UnicodeSet(
            "[.\u2024\u3002\uFE12\uFE52\uFF0E\uFF61]").freeze();
    private static final UnicodeSet commaEquivalents = (UnicodeSet) new UnicodeSet(
            "[,\u060C\u066B\u3001\uFE10\uFE11\uFE50\uFE51\uFF0C\uFF64]").freeze();
    private static final UnicodeSet otherGroupingSeparators = (UnicodeSet) new UnicodeSet(
            "[\\ '\u00A0\u066C\u2000-\u200A\u2018\u2019\u202F\u205F\u3000\uFF07]").freeze();

    private static final UnicodeSet strictDotEquivalents = (UnicodeSet) new UnicodeSet("[.\u2024\uFE52\uFF0E\uFF61]")
            .freeze();
    private static final UnicodeSet strictCommaEquivalents = (UnicodeSet) new UnicodeSet("[,\u066B\uFE10\uFE50\uFF0C]")
            .freeze();
    private static final UnicodeSet strictOtherGroupingSeparators = (UnicodeSet) new UnicodeSet(
            "[\\ '\u00A0\u066C\u2000-\u200A\u2018\u2019\u202F\u205F\u3000\uFF07]").freeze();

    private static final UnicodeSet defaultGroupingSeparators = (UnicodeSet) new UnicodeSet(dotEquivalents).addAll(
            commaEquivalents).addAll(otherGroupingSeparators).freeze();
    private static final UnicodeSet strictDefaultGroupingSeparators = (UnicodeSet) new UnicodeSet(strictDotEquivalents)
            .addAll(strictCommaEquivalents).addAll(strictOtherGroupingSeparators).freeze();

    // When parsing a number with big exponential value, it requires to transform
    // the value into a string representation to construct BigInteger instance.
    // We want to set the maximum size because it can easily trigger OutOfMemoryException.
    // PARSE_MAX_EXPONENT is currently set to 1000, which is much bigger than
    // MAX_VALUE of Double (
    // See the problem reported by ticket#5698
    private static final int PARSE_MAX_EXPONENT = 1000;

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong> Parse the given text into a number. The text is
     * parsed beginning at parsePosition, until an unparseable character is seen.
     * 
     * @param text
     *            The string to parse.
     * @param parsePosition
     *            The position at which to being parsing. Upon return, the first unparseable character.
     * @param digits
     *            The DigitList to set to the parsed value.
     * @param isExponent
     *            If true, parse an exponent. This means no infinite values and integer only.
     * @param status
     *            Upon return contains boolean status flags indicating whether the value was infinite and whether it was
     *            positive.
     * @param currency
     *            return value for parsed currency, for generic currency parsing mode, or null for normal parsing. In
     *            generic currency parsing mode, any currency is parsed, not just the currency that this formatter is
     *            set to.
     * @param negPrefix
     *            negative prefix pattern
     * @param negSuffix
     *            negative suffix pattern
     * @param posPrefix
     *            positive prefix pattern
     * @param negSuffix
     *            negative suffix pattern
     * @param type
     *            type of currency to parse against, LONG_NAME only or not.
     */
    private final boolean subparse(String text, ParsePosition parsePosition, DigitList digits, boolean isExponent,
            boolean status[], Currency currency[], String negPrefix, String negSuffix, String posPrefix,
            String posSuffix, int type) {
        int position = parsePosition.getIndex();
        int oldStart = parsePosition.getIndex();

        // Match padding before prefix
        if (formatWidth > 0 && padPosition == PAD_BEFORE_PREFIX) {
            position = skipPadding(text, position);
        }

        // Match positive and negative prefixes; prefer longest match.
        int posMatch = compareAffix(text, position, false, true, posPrefix, type, currency);
        int negMatch = compareAffix(text, position, true, true, negPrefix, type, currency);
        if (posMatch >= 0 && negMatch >= 0) {
            if (posMatch > negMatch) {
                negMatch = -1;
            } else if (negMatch > posMatch) {
                posMatch = -1;
            }
        }
        if (posMatch >= 0) {
            position += posMatch;
        } else if (negMatch >= 0) {
            position += negMatch;
        } else {
            parsePosition.setErrorIndex(position);
            return false;
        }

        // Match padding after prefix
        if (formatWidth > 0 && padPosition == PAD_AFTER_PREFIX) {
            position = skipPadding(text, position);
        }

        // process digits or Inf, find decimal position
        status[STATUS_INFINITE] = false;
        if (!isExponent && text.regionMatches(position, symbols.getInfinity(), 0, symbols.getInfinity().length())) {
            position += symbols.getInfinity().length();
            status[STATUS_INFINITE] = true;
        } else {
            // We now have a string of digits, possibly with grouping symbols,
            // and decimal points. We want to process these into a DigitList.
            // We don't want to put a bunch of leading zeros into the DigitList
            // though, so we keep track of the location of the decimal point,
            // put only significant digits into the DigitList, and adjust the
            // exponent as needed.

            digits.decimalAt = digits.count = 0;
            char zero = symbols.getZeroDigit();
            char decimal = currencySignCount > 0 ? symbols.getMonetaryDecimalSeparator() : symbols
                    .getDecimalSeparator();
            char grouping = symbols.getGroupingSeparator();

            String exponentSep = symbols.getExponentSeparator();
            boolean sawDecimal = false;
            boolean sawExponent = false;
            boolean sawDigit = false;
            long exponent = 0; // Set to the exponent value, if any
            int digit = 0;

            // strict parsing
            boolean strictParse = isParseStrict();
            boolean strictFail = false; // did we exit with a strict parse failure?
            int lastGroup = -1; // where did we last see a grouping separator?
            int gs2 = groupingSize2 == 0 ? groupingSize : groupingSize2;

            // Strict parsing leading zeroes. If a leading zero would
            // be forced by the pattern, then don't fail strict parsing.
            boolean strictLeadingZero = false;
            int leadingZeroPos = 0;
            int leadingZeroCount = 0;

            // equivalent grouping and decimal support

            // TODO markdavis Cache these if it makes a difference in performance.
            UnicodeSet decimalSet = new UnicodeSet(getSimilarDecimals(decimal, strictParse));
            UnicodeSet groupingSet = new UnicodeSet(strictParse ? strictDefaultGroupingSeparators
                    : defaultGroupingSeparators).add(grouping).removeAll(decimalSet);

            // we are guaranteed that
            // decimalSet contains the decimal, and
            // groupingSet contains the groupingSeparator
            // (unless decimal and grouping are the same, which should never happen. But in that case, groupingSet will
            // just be empty.)

            // We have to track digitCount ourselves, because digits.count will
            // pin when the maximum allowable digits is reached.
            int digitCount = 0;

            int backup = -1;
            for (; position < text.length(); ++position) {
                char ch = text.charAt(position);

                /*
                 * We recognize all digit ranges, not only the Latin digit range '0'..'9'. We do so by using the
                 * UCharacter.digit() method, which converts a valid Unicode digit to the range 0..9.
                 * 
                 * The character 'ch' may be a digit. If so, place its value from 0 to 9 in 'digit'. First try using the
                 * locale digit, which may or MAY NOT be a standard Unicode digit range. If this fails, try using the
                 * standard Unicode digit ranges by calling UCharacter.digit(). If this also fails, digit will have a
                 * value outside the range 0..9.
                 */
                digit = ch - zero;
                if (digit < 0 || digit > 9)
                    digit = UCharacter.digit(ch, 10);

                if (digit == 0) {
                    // Cancel out backup setting (see grouping handler below)
                    if (strictParse && backup != -1) {
                        // comma followed by digit, so group before comma is a
                        // secondary group. If there was a group separator
                        // before that, the group must == the secondary group
                        // length, else it can be <= the the secondary group
                        // length.
                        if ((lastGroup != -1 && backup - lastGroup - 1 != gs2)
                                || (lastGroup == -1 && position - oldStart - 1 > gs2)) {
                            strictFail = true;
                            break;
                        }
                        lastGroup = backup;
                    }
                    backup = -1; // Do this BEFORE continue statement below!!!
                    sawDigit = true;

                    // Handle leading zeros
                    if (digits.count == 0) {
                        if (!sawDecimal) {
                            if (strictParse && !isExponent) {
                                // Allow leading zeros in exponents
                                // Count leading zeros for checking later
                                if (!strictLeadingZero)
                                    leadingZeroPos = position + 1;
                                strictLeadingZero = true;
                                ++leadingZeroCount;
                            }
                            // Ignore leading zeros in integer part of number.
                            continue;
                        }

                        // If we have seen the decimal, but no significant digits yet,
                        // then we account for leading zeros by decrementing the
                        // digits.decimalAt into negative values.
                        --digits.decimalAt;
                    } else {
                        ++digitCount;
                        digits.append((char) (digit + '0'));
                    }
                } else if (digit > 0 && digit <= 9) // [sic] digit==0 handled above
                {
                    if (strictParse) {
                        if (backup != -1) {
                            if ((lastGroup != -1 && backup - lastGroup - 1 != gs2)
                                    || (lastGroup == -1 && position - oldStart - 1 > gs2)) {
                                strictFail = true;
                                break;
                            }
                            lastGroup = backup;
                        }
                    }

                    sawDigit = true;
                    ++digitCount;
                    digits.append((char) (digit + '0'));

                    // Cancel out backup setting (see grouping handler below)
                    backup = -1;
                } else if (!isExponent && decimalSet.contains(ch)) {
                    if (strictParse) {
                        if (backup != -1 || (lastGroup != -1 && position - lastGroup != groupingSize + 1)) {
                            strictFail = true;
                            break;
                        }
                    }
                    // If we're only parsing integers, or if we ALREADY saw the
                    // decimal, then don't parse this one.
                    if (isParseIntegerOnly() || sawDecimal)
                        break;
                    digits.decimalAt = digitCount; // Not digits.count!
                    sawDecimal = true;

                    // Once we see a decimal character, we only accept that decimal character from then on.
                    decimalSet.set(ch, ch);
                } else if (!isExponent && isGroupingUsed() && groupingSet.contains(ch)) {
                    if (sawDecimal) {
                        break;
                    }
                    if (strictParse) {
                        if ((!sawDigit || backup != -1)) {
                            // leading group, or two group separators in a row
                            strictFail = true;
                            break;
                        }
                    }
                    // Once we see a grouping character, we only accept that grouping character from then on.
                    groupingSet.set(ch, ch);

                    // Ignore grouping characters, if we are using them, but require
                    // that they be followed by a digit. Otherwise we backup and
                    // reprocess them.
                    backup = position;
                } else if (!isExponent && !sawExponent
                        && text.regionMatches(position, exponentSep, 0, exponentSep.length())) {
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
                            /*
                             * Can't parse "[1E0]" when pattern is "0.###E0;[0.###E0]" Should update reassign the value
                             * of 'ch' in the code: digit = Character.digit(ch, 10); [Richard/GCL]
                             */
                            digit = UCharacter.digit(text.charAt(pos), 10);
                        }
                        if (digit >= 0 && digit <= 9) {
                            exponentDigits.append((char) (digit + '0'));
                            ++pos;
                        } else {
                            break;
                        }
                    }

                    if (exponentDigits.count > 0) {
                        // defer strict parse until we know we have a bona-fide exponent
                        if (strictParse) {
                            if (backup != -1 || lastGroup != -1) {
                                strictFail = true;
                                break;
                            }
                        }

                        // Quick overflow check for exponential part.
                        // Actual limit check will be done later in this code.
                        if (exponentDigits.count > 10 /* maximum decimal digits for int */) {
                            if (negExp) {
                                // set underflow flag
                                status[STATUS_UNDERFLOW] = true;
                            } else {
                                // set infinite flag
                                status[STATUS_INFINITE] = true;
                            }
                        } else {
                            exponentDigits.decimalAt = exponentDigits.count;
                            exponent = exponentDigits.getLong();
                            if (negExp) {
                                exponent = -exponent;
                            }
                        }
                        position = pos; // Advance past the exponent
                        sawExponent = true;
                    }

                    break; // Whether we fail or succeed, we exit this loop
                } else
                    break;
            }

            if (backup != -1)
                position = backup;

            // If there was no decimal point we have an integer
            if (!sawDecimal)
                digits.decimalAt = digitCount; // Not digits.count!

            // check for strict parse errors
            if (strictParse && strictLeadingZero) {
                if ((leadingZeroCount + digits.decimalAt) > this.getMinimumIntegerDigits()) {
                    parsePosition.setIndex(oldStart);
                    parsePosition.setErrorIndex(leadingZeroPos);
                    return false;
                }
            }
            if (strictParse && !sawDecimal) {
                if (lastGroup != -1 && position - lastGroup != groupingSize + 1) {
                    strictFail = true;
                }
            }
            if (strictFail) {
                // only set with strictParse and a leading zero error
                // leading zeros are an error with strict parsing except
                // immediately before nondigit (except group separator
                // followed by digit), or end of text.

                parsePosition.setIndex(oldStart);
                parsePosition.setErrorIndex(position);
                return false;
            }

            // Adjust for exponent, if any
            exponent += digits.decimalAt;
            if (exponent < -PARSE_MAX_EXPONENT) {
                status[STATUS_UNDERFLOW] = true;
            } else if (exponent > PARSE_MAX_EXPONENT) {
                status[STATUS_INFINITE] = true;
            } else {
                digits.decimalAt = (int) exponent;
            }

            // If none of the text string was recognized. For example, parse
            // "x" with pattern "#0.00" (return index and error index both 0)
            // parse "$" with pattern "$#0.00". (return index 0 and error index
            // 1).
            if (!sawDigit && digitCount == 0) {
                parsePosition.setIndex(oldStart);
                parsePosition.setErrorIndex(oldStart);
                return false;
            }
        }

        // Match padding before suffix
        if (formatWidth > 0 && padPosition == PAD_BEFORE_SUFFIX) {
            position = skipPadding(text, position);
        }

        // Match positive and negative suffixes; prefer longest match.
        if (posMatch >= 0) {
            posMatch = compareAffix(text, position, false, false, posSuffix, type, currency);
        }
        if (negMatch >= 0) {
            negMatch = compareAffix(text, position, true, false, negSuffix, type, currency);
        }
        if (posMatch >= 0 && negMatch >= 0) {
            if (posMatch > negMatch) {
                negMatch = -1;
            } else if (negMatch > posMatch) {
                posMatch = -1;
            }
        }

        // Fail if neither or both
        if ((posMatch >= 0) == (negMatch >= 0)) {
            parsePosition.setErrorIndex(position);
            return false;
        }

        position += (posMatch >= 0 ? posMatch : negMatch);

        // Match padding after suffix
        if (formatWidth > 0 && padPosition == PAD_AFTER_SUFFIX) {
            position = skipPadding(text, position);
        }

        parsePosition.setIndex(position);

        status[STATUS_POSITIVE] = (posMatch >= 0);

        if (parsePosition.getIndex() == oldStart) {
            parsePosition.setErrorIndex(position);
            return false;
        }
        return true;
    }

    /**
     * Return characters that are used where this decimal is used.
     * 
     * @param decimal
     * @param strictParse
     * @return
     */
    private UnicodeSet getSimilarDecimals(char decimal, boolean strictParse) {
        if (dotEquivalents.contains(decimal)) {
            return strictParse ? strictDotEquivalents : dotEquivalents;
        }
        if (commaEquivalents.contains(decimal)) {
            return strictParse ? strictCommaEquivalents : commaEquivalents;
        }
        // if there is no match, return the character itself
        return new UnicodeSet().add(decimal);
    }

    /**
     * Starting at position, advance past a run of pad characters, if any. Return the index of the first character after
     * position that is not a pad character. Result is >= position.
     */
    private final int skipPadding(String text, int position) {
        while (position < text.length() && text.charAt(position) == pad) {
            ++position;
        }
        return position;
    }

    /*
     * Return the length matched by the given affix, or -1 if none. Runs of white space in the affix, match runs of
     * white space in the input. Pattern white space and input white space are determined differently; see code.
     * 
     * @param text input text
     * 
     * @param pos offset into input at which to begin matching
     * 
     * @param isNegative
     * 
     * @param isPrefix
     * 
     * @param affixPat affix pattern used for currency affix comparison
     * 
     * @param type compare against currency type, LONG_NAME only or not.
     * 
     * @param currency return value for parsed currency, for generic currency parsing mode, or null for normal parsing.
     * In generic currency parsing mode, any currency is parsed, not just the currency that this formatter is set to.
     * 
     * @return length of input that matches, or -1 if match failure
     */
    private int compareAffix(String text, int pos, boolean isNegative, boolean isPrefix, String affixPat, int type,
            Currency[] currency) {
        if (currency != null || currencyChoice != null || currencySignCount > 0) {
            return compareComplexAffix(affixPat, text, pos, type, currency);
        }
        if (isPrefix) {
            return compareSimpleAffix(isNegative ? negativePrefix : positivePrefix, text, pos);
        } else {
            return compareSimpleAffix(isNegative ? negativeSuffix : positiveSuffix, text, pos);
        }

    }

    /**
     * Return the length matched by the given affix, or -1 if none. Runs of white space in the affix, match runs of
     * white space in the input. Pattern white space and input white space are determined differently; see code.
     * 
     * @param affix
     *            pattern string, taken as a literal
     * @param input
     *            input text
     * @param pos
     *            offset into input at which to begin matching
     * @return length of input that matches, or -1 if match failure
     */
    private static int compareSimpleAffix(String affix, String input, int pos) {
        int start = pos;
        for (int i = 0; i < affix.length();) {
            int c = UTF16.charAt(affix, i);
            int len = UTF16.getCharCount(c);
            if (UCharacterProperty.isRuleWhiteSpace(c)) {
                // We may have a pattern like: \u200F
                // and input text like: \u200F
                // Note that U+200F and U+0020 are RuleWhiteSpace but only
                // U+0020 is UWhiteSpace. So we have to first do a direct
                // match of the run of RULE whitespace in the pattern,
                // then match any extra characters.
                boolean literalMatch = false;
                while (pos < input.length() && UTF16.charAt(input, pos) == c) {
                    literalMatch = true;
                    i += len;
                    pos += len;
                    if (i == affix.length()) {
                        break;
                    }
                    c = UTF16.charAt(affix, i);
                    len = UTF16.getCharCount(c);
                    if (!UCharacterProperty.isRuleWhiteSpace(c)) {
                        break;
                    }
                }

                // Advance over run in affix
                i = skipRuleWhiteSpace(affix, i);

                // Advance over run in input text
                // Must see at least one white space char in input,
                // unless we've already matched some characters literally.
                int s = pos;
                pos = skipUWhiteSpace(input, pos);
                if (pos == s && !literalMatch) {
                    return -1;
                }
                // If we skip UWhiteSpace in the input text, we need to skip it in the pattern.
                // Otherwise, the previous lines may have skipped over text (such as U+00A0) that
                // is also in the affix.
                i = skipUWhiteSpace(affix, i);
            } else {
                if (pos < input.length() && UTF16.charAt(input, pos) == c) {
                    i += len;
                    pos += len;
                } else {
                    return -1;
                }
            }
        }
        return pos - start;
    }

    /**
     * Skip over a run of zero or more isRuleWhiteSpace() characters at pos in text.
     */
    private static int skipRuleWhiteSpace(String text, int pos) {
        while (pos < text.length()) {
            int c = UTF16.charAt(text, pos);
            if (!UCharacterProperty.isRuleWhiteSpace(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    /**
     * Skip over a run of zero or more isUWhiteSpace() characters at pos in text.
     */
    private static int skipUWhiteSpace(String text, int pos) {
        while (pos < text.length()) {
            int c = UTF16.charAt(text, pos);
            if (!UCharacter.isUWhiteSpace(c)) {
                break;
            }
            pos += UTF16.getCharCount(c);
        }
        return pos;
    }

    /*
     * Return the length matched by the given affix, or -1 if none.
     * 
     * @param affixPat pattern string
     * 
     * @param text input text
     * 
     * @param pos offset into input at which to begin matching
     * 
     * @param type parse against currency type, LONG_NAME only or not.
     * 
     * @param currency return value for parsed currency, for generic currency parsing mode, or null for normal parsing.
     * In generic currency parsing mode, any currency is parsed, not just the currency that this formatter is set to.
     * 
     * @return position after the matched text, or -1 if match failure
     */
    private int compareComplexAffix(String affixPat, String text, int pos, int type, Currency[] currency) {
        int start = pos;
        for (int i = 0; i < affixPat.length() && pos >= 0;) {
            char c = affixPat.charAt(i++);
            if (c == QUOTE) {
                for (;;) {
                    int j = affixPat.indexOf(QUOTE, i);
                    if (j == i) {
                        pos = match(text, pos, QUOTE);
                        i = j + 1;
                        break;
                    } else if (j > i) {
                        pos = match(text, pos, affixPat.substring(i, j));
                        i = j + 1;
                        if (i < affixPat.length() && affixPat.charAt(i) == QUOTE) {
                            pos = match(text, pos, QUOTE);
                            ++i;
                            // loop again
                        } else {
                            break;
                        }
                    } else {
                        // Unterminated quote; should be caught by apply
                        // pattern.
                        throw new RuntimeException();
                    }
                }
                continue;
            }

            switch (c) {
            case CURRENCY_SIGN:
                // since the currency names in choice format is saved
                // the same way as other currency names,
                // do not need to do currency choice parsing here.
                // the general currency parsing parse against all names,
                // including names in choice format.
                // assert(currency != null ||
                // (getCurrency() != null && currencyChoice != null));
                boolean intl = i < affixPat.length() && affixPat.charAt(i) == CURRENCY_SIGN;
                if (intl) {
                    ++i;
                }
                boolean plural = i < affixPat.length() && affixPat.charAt(i) == CURRENCY_SIGN;
                if (plural) {
                    ++i;
                    intl = false;
                }
                // Parse generic currency -- anything for which we
                // have a display name, or any 3-letter ISO code.
                // Try to parse display name for our locale; first
                // determine our locale.
                // TODO: use locale in CurrencyPluralInfo
                ULocale uloc = getLocale(ULocale.VALID_LOCALE);
                if (uloc == null) {
                    // applyPattern has been called; use the symbols
                    uloc = symbols.getLocale(ULocale.VALID_LOCALE);
                }
                // Delegate parse of display name => ISO code to Currency
                ParsePosition ppos = new ParsePosition(pos);
                // using Currency.parse to handle mixed style parsing.
                String iso = Currency.parse(uloc, text, type, ppos);

                // If parse succeeds, populate currency[0]
                if (iso != null) {
                    if (currency != null) {
                        currency[0] = Currency.getInstance(iso);
                    }
                    pos = ppos.getIndex();
                } else {
                    pos = -1;
                }
                continue;
            case PATTERN_PERCENT:
                c = symbols.getPercent();
                break;
            case PATTERN_PER_MILLE:
                c = symbols.getPerMill();
                break;
            case PATTERN_MINUS:
                c = symbols.getMinusSign();
                break;
            }
            pos = match(text, pos, c);
            if (UCharacterProperty.isRuleWhiteSpace(c)) {
                i = skipRuleWhiteSpace(affixPat, i);
            }
        }

        return pos - start;
    }

    /**
     * Match a single character at text[pos] and return the index of the next character upon success. Return -1 on
     * failure. If isRuleWhiteSpace(ch) then match a run of white space in text.
     */
    static final int match(String text, int pos, int ch) {
        if (pos >= text.length()) {
            return -1;
        }
        if (UCharacterProperty.isRuleWhiteSpace(ch)) {
            // Advance over run of white space in input text
            // Must see at least one white space char in input
            int s = pos;
            pos = skipUWhiteSpace(text, pos);
            if (pos == s) {
                return -1;
            }
            return pos;
        }
        return (pos >= 0 && UTF16.charAt(text, pos) == ch) ? (pos + UTF16.getCharCount(ch)) : -1;
    }

    /**
     * Match a string at text[pos] and return the index of the next character upon success. Return -1 on failure. Match
     * a run of white space in str with a run of white space in text.
     */
    static final int match(String text, int pos, String str) {
        for (int i = 0; i < str.length() && pos >= 0;) {
            int ch = UTF16.charAt(str, i);
            i += UTF16.getCharCount(ch);
            pos = match(text, pos, ch);
            if (UCharacterProperty.isRuleWhiteSpace(ch)) {
                i = skipRuleWhiteSpace(str, i);
            }
        }
        return pos;
    }

    /**
     * Returns a copy of the decimal format symbols used by this format.
     * 
     * @return desired DecimalFormatSymbols
     * @see DecimalFormatSymbols
     * @stable ICU 2.0
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
     * Sets the decimal format symbols used by this format. The format uses a copy of the provided symbols.
     * 
     * @param newSymbols
     *            desired DecimalFormatSymbols
     * @see DecimalFormatSymbols
     * @stable ICU 2.0
     */
    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        symbols = (DecimalFormatSymbols) newSymbols.clone();
        setCurrencyForSymbols();
        expandAffixes(null);
    }

    /**
     * Update the currency object to match the symbols. This method is used only when the caller has passed in a symbols
     * object that may not be the default object for its locale.
     */
    private void setCurrencyForSymbols() {
        /*
         * Bug 4212072 Update the affix strings according to symbols in order to keep the affix strings up to date.
         * [Richard/GCL]
         */

        // With the introduction of the Currency object, the currency
        // symbols in the DFS object are ignored. For backward
        // compatibility, we check any explicitly set DFS object. If it
        // is a default symbols object for its locale, we change the
        // currency object to one for that locale. If it is custom,
        // we set the currency to null.
        DecimalFormatSymbols def = new DecimalFormatSymbols(symbols.getLocale());

        if (symbols.getCurrencySymbol().equals(def.getCurrencySymbol())
                && symbols.getInternationalCurrencySymbol().equals(def.getInternationalCurrencySymbol())) {
            setCurrency(Currency.getInstance(symbols.getLocale()));
        } else {
            setCurrency(null);
        }
    }

    /**
     * Get the positive prefix.
     * <P>
     * Examples: +123, $123, sFr123
     * 
     * @stable ICU 2.0
     */
    public String getPositivePrefix() {
        return positivePrefix;
    }

    /**
     * Set the positive prefix.
     * <P>
     * Examples: +123, $123, sFr123
     * 
     * @stable ICU 2.0
     */
    public void setPositivePrefix(String newValue) {
        positivePrefix = newValue;
        posPrefixPattern = null;
    }

    /**
     * Get the negative prefix.
     * <P>
     * Examples: -123, ($123) (with negative suffix), sFr-123
     * 
     * @stable ICU 2.0
     */
    public String getNegativePrefix() {
        return negativePrefix;
    }

    /**
     * Set the negative prefix.
     * <P>
     * Examples: -123, ($123) (with negative suffix), sFr-123
     * 
     * @stable ICU 2.0
     */
    public void setNegativePrefix(String newValue) {
        negativePrefix = newValue;
        negPrefixPattern = null;
    }

    /**
     * Get the positive suffix.
     * <P>
     * Example: 123%
     * 
     * @stable ICU 2.0
     */
    public String getPositiveSuffix() {
        return positiveSuffix;
    }

    /**
     * Set the positive suffix.
     * <P>
     * Example: 123%
     * 
     * @stable ICU 2.0
     */
    public void setPositiveSuffix(String newValue) {
        positiveSuffix = newValue;
        posSuffixPattern = null;
    }

    /**
     * Get the negative suffix.
     * <P>
     * Examples: -123%, ($123) (with positive suffixes)
     * 
     * @stable ICU 2.0
     */
    public String getNegativeSuffix() {
        return negativeSuffix;
    }

    /**
     * Set the positive suffix.
     * <P>
     * Examples: 123%
     * 
     * @stable ICU 2.0
     */
    public void setNegativeSuffix(String newValue) {
        negativeSuffix = newValue;
        negSuffixPattern = null;
    }

    /**
     * Get the multiplier for use in percent, permill, etc. For a percentage, set the suffixes to have "%" and the
     * multiplier to be 100. (For Arabic, use arabic percent symbol). For a permill, set the suffixes to have "\u2031"
     * and the multiplier to be 1000.
     * <P>
     * Examples: with 100, 1.23 -> "123", and "123" -> 1.23
     * 
     * @stable ICU 2.0
     */
    public int getMultiplier() {
        return multiplier;
    }

    /**
     * Set the multiplier for use in percent, permill, etc. For a percentage, set the suffixes to have "%" and the
     * multiplier to be 100. (For Arabic, use arabic percent symbol). For a permill, set the suffixes to have "\u2031"
     * and the multiplier to be 1000.
     * <P>
     * Examples: with 100, 1.23 -> "123", and "123" -> 1.23
     * 
     * @stable ICU 2.0
     */
    public void setMultiplier(int newValue) {
        if (newValue == 0) {
            throw new IllegalArgumentException("Bad multiplier: " + newValue);
        }
        multiplier = newValue;
    }

    /**
     * Get the rounding increment.
     * 
     * @return A positive rounding increment, or <code>null</code> if rounding is not in effect.
     * @see #setRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     * @stable ICU 2.0
     */
    public java.math.BigDecimal getRoundingIncrement() {
        if (roundingIncrementICU == null)
            return null;
        return roundingIncrementICU.toBigDecimal();
    }

    /**
     * Set the rounding increment. This method also controls whether rounding is enabled.
     * 
     * @param newValue
     *            A positive rounding increment, or <code>null</code> or <code>BigDecimal(0.0)</code> to disable
     *            rounding.
     * @exception IllegalArgumentException
     *                if <code>newValue</code> is < 0.0
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     * @stable ICU 2.0
     */
    public void setRoundingIncrement(java.math.BigDecimal newValue) {
        if (newValue == null) {
            setRoundingIncrement((BigDecimal) null);
        } else {
            setRoundingIncrement(new BigDecimal(newValue));
        }
    }

    /**
     * Set the rounding increment. This method also controls whether rounding is enabled.
     * 
     * @param newValue
     *            A positive rounding increment, or <code>null</code> or <code>BigDecimal(0.0)</code> to disable
     *            rounding.
     * @exception IllegalArgumentException
     *                if <code>newValue</code> is < 0.0
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     * @stable ICU 3.6
     */
    public void setRoundingIncrement(BigDecimal newValue) {
        int i = newValue == null ? 0 : newValue.compareTo(BigDecimal.ZERO);
        if (i < 0) {
            throw new IllegalArgumentException("Illegal rounding increment");
        }
        if (i == 0) {
            setInternalRoundingIncrement(null);
        } else {
            setInternalRoundingIncrement(newValue);
        }
        setRoundingDouble();
    }

    /**
     * Set the rounding increment. This method also controls whether rounding is enabled.
     * 
     * @param newValue
     *            A positive rounding increment, or 0.0 to disable rounding.
     * @exception IllegalArgumentException
     *                if <code>newValue</code> is < 0.0
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     * @stable ICU 2.0
     */
    public void setRoundingIncrement(double newValue) {
        if (newValue < 0.0) {
            throw new IllegalArgumentException("Illegal rounding increment");
        }
        roundingDouble = newValue;
        roundingDoubleReciprocal = 0.0d;
        if (newValue == 0.0d) {
            setRoundingIncrement((BigDecimal) null);
        } else {
            roundingDouble = newValue;
            if (roundingDouble < 1.0d) {
                double rawRoundedReciprocal = 1.0d / roundingDouble;
                setRoundingDoubleReciprocal(rawRoundedReciprocal);
            }
            setInternalRoundingIncrement(new BigDecimal(newValue));
        }
    }

    private void setRoundingDoubleReciprocal(double rawRoundedReciprocal) {
        roundingDoubleReciprocal = Math.rint(rawRoundedReciprocal);
        if (Math.abs(rawRoundedReciprocal - roundingDoubleReciprocal) > roundingIncrementEpsilon) {
            roundingDoubleReciprocal = 0.0d;
        }
    }

    static final double roundingIncrementEpsilon = 0.000000001;

    /**
     * Get the rounding mode.
     * 
     * @return A rounding mode, between <code>BigDecimal.ROUND_UP</code> and <code>BigDecimal.ROUND_UNNECESSARY</code>.
     * @see #setRoundingIncrement
     * @see #getRoundingIncrement
     * @see #setRoundingMode
     * @see java.math.BigDecimal
     * @stable ICU 2.0
     */
    public int getRoundingMode() {
        return roundingMode;
    }

    /**
     * Set the rounding mode. This has no effect unless the rounding increment is greater than zero.
     * 
     * @param roundingMode
     *            A rounding mode, between <code>BigDecimal.ROUND_UP</code> and
     *            <code>BigDecimal.ROUND_UNNECESSARY</code>.
     * @exception IllegalArgumentException
     *                if <code>roundingMode</code> is unrecognized.
     * @see #setRoundingIncrement
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @see java.math.BigDecimal
     * @stable ICU 2.0
     */
    public void setRoundingMode(int roundingMode) {
        if (roundingMode < BigDecimal.ROUND_UP || roundingMode > BigDecimal.ROUND_UNNECESSARY) {
            throw new IllegalArgumentException("Invalid rounding mode: " + roundingMode);
        }

        this.roundingMode = roundingMode;

        if (getRoundingIncrement() == null) {
            setRoundingIncrement(Math.pow(10.0, (double) -getMaximumFractionDigits()));
        }
    }

    /**
     * Get the width to which the output of <code>format()</code> is padded. The width is counted in 16-bit code units.
     * 
     * @return the format width, or zero if no padding is in effect
     * @see #setFormatWidth
     * @see #getPadCharacter
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     * @stable ICU 2.0
     */
    public int getFormatWidth() {
        return formatWidth;
    }

    /**
     * Set the width to which the output of <code>format()</code> is padded. The width is counted in 16-bit code units.
     * This method also controls whether padding is enabled.
     * 
     * @param width
     *            the width to which to pad the result of <code>format()</code>, or zero to disable padding
     * @exception IllegalArgumentException
     *                if <code>width</code> is < 0
     * @see #getFormatWidth
     * @see #getPadCharacter
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     * @stable ICU 2.0
     */
    public void setFormatWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("Illegal format width");
        }
        formatWidth = width;
    }

    /**
     * Get the character used to pad to the format width. The default is ' '.
     * 
     * @return the pad character
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     * @stable ICU 2.0
     */
    public char getPadCharacter() {
        return pad;
    }

    /**
     * Set the character used to pad to the format width. If padding is not enabled, then this will take effect if
     * padding is later enabled.
     * 
     * @param padChar
     *            the pad character
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #getPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     * @stable ICU 2.0
     */
    public void setPadCharacter(char padChar) {
        pad = padChar;
    }

    /**
     * Get the position at which padding will take place. This is the location at which padding will be inserted if the
     * result of <code>format()</code> is shorter than the format width.
     * 
     * @return the pad position, one of <code>PAD_BEFORE_PREFIX</code>, <code>PAD_AFTER_PREFIX</code>,
     *         <code>PAD_BEFORE_SUFFIX</code>, or <code>PAD_AFTER_SUFFIX</code>.
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadCharacter
     * @see #setPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @see #PAD_AFTER_SUFFIX
     * @stable ICU 2.0
     */
    public int getPadPosition() {
        return padPosition;
    }

    /**
     * Set the position at which padding will take place. This is the location at which padding will be inserted if the
     * result of <code>format()</code> is shorter than the format width. This has no effect unless padding is enabled.
     * 
     * @param padPos
     *            the pad position, one of <code>PAD_BEFORE_PREFIX</code>, <code>PAD_AFTER_PREFIX</code>,
     *            <code>PAD_BEFORE_SUFFIX</code>, or <code>PAD_AFTER_SUFFIX</code>.
     * @exception IllegalArgumentException
     *                if the pad position in unrecognized
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadCharacter
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @see #PAD_AFTER_SUFFIX
     * @stable ICU 2.0
     */
    public void setPadPosition(int padPos) {
        if (padPos < PAD_BEFORE_PREFIX || padPos > PAD_AFTER_SUFFIX) {
            throw new IllegalArgumentException("Illegal pad position");
        }
        padPosition = padPos;
    }

    /**
     * Return whether or not scientific notation is used.
     * 
     * @return true if this object formats and parses scientific notation
     * @see #setScientificNotation
     * @see #getMinimumExponentDigits
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    public boolean isScientificNotation() {
        return useExponentialNotation;
    }

    /**
     * Set whether or not scientific notation is used. When scientific notation is used, the effective maximum number of
     * integer digits is <= 8. If the maximum number of integer digits is set to more than 8, the effective maximum will
     * be 1. This allows this call to generate a 'default' scientific number format without additional changes.
     * 
     * @param useScientific
     *            true if this object formats and parses scientific notation
     * @see #isScientificNotation
     * @see #getMinimumExponentDigits
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    public void setScientificNotation(boolean useScientific) {
        useExponentialNotation = useScientific;
    }

    /**
     * Return the minimum exponent digits that will be shown.
     * 
     * @return the minimum exponent digits that will be shown
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    public byte getMinimumExponentDigits() {
        return minExponentDigits;
    }

    /**
     * Set the minimum exponent digits that will be shown. This has no effect unless scientific notation is in use.
     * 
     * @param minExpDig
     *            a value >= 1 indicating the fewest exponent digits that will be shown
     * @exception IllegalArgumentException
     *                if <code>minExpDig</code> < 1
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #getMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    public void setMinimumExponentDigits(byte minExpDig) {
        if (minExpDig < 1) {
            throw new IllegalArgumentException("Exponent digits must be >= 1");
        }
        minExponentDigits = minExpDig;
    }

    /**
     * Return whether the exponent sign is always shown.
     * 
     * @return true if the exponent is always prefixed with either the localized minus sign or the localized plus sign,
     *         false if only negative exponents are prefixed with the localized minus sign.
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #getMinimumExponentDigits
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    public boolean isExponentSignAlwaysShown() {
        return exponentSignAlwaysShown;
    }

    /**
     * Set whether the exponent sign is always shown. This has no effect unless scientific notation is in use.
     * 
     * @param expSignAlways
     *            true if the exponent is always prefixed with either the localized minus sign or the localized plus
     *            sign, false if only negative exponents are prefixed with the localized minus sign.
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #getMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    public void setExponentSignAlwaysShown(boolean expSignAlways) {
        exponentSignAlwaysShown = expSignAlways;
    }

    /**
     * Return the grouping size. Grouping size is the number of digits between grouping separators in the integer
     * portion of a number. For example, in the number "123,456.78", the grouping size is 3.
     * 
     * @see #setGroupingSize
     * @see NumberFormat#isGroupingUsed
     * @see DecimalFormatSymbols#getGroupingSeparator
     * @stable ICU 2.0
     */
    public int getGroupingSize() {
        return groupingSize;
    }

    /**
     * Set the grouping size. Grouping size is the number of digits between grouping separators in the integer portion
     * of a number. For example, in the number "123,456.78", the grouping size is 3.
     * 
     * @see #getGroupingSize
     * @see NumberFormat#setGroupingUsed
     * @see DecimalFormatSymbols#setGroupingSeparator
     * @stable ICU 2.0
     */
    public void setGroupingSize(int newValue) {
        groupingSize = (byte) newValue;
    }

    /**
     * Return the secondary grouping size. In some locales one grouping interval is used for the least significant
     * integer digits (the primary grouping size), and another is used for all others (the secondary grouping size). A
     * formatter supporting a secondary grouping size will return a positive integer unequal to the primary grouping
     * size returned by <code>getGroupingSize()</code>. For example, if the primary grouping size is 4, and the
     * secondary grouping size is 2, then the number 123456789 formats as "1,23,45,6789", and the pattern appears as
     * "#,##,###0".
     * 
     * @return the secondary grouping size, or a value less than one if there is none
     * @see #setSecondaryGroupingSize
     * @see NumberFormat#isGroupingUsed
     * @see DecimalFormatSymbols#getGroupingSeparator
     * @stable ICU 2.0
     */
    public int getSecondaryGroupingSize() {
        return groupingSize2;
    }

    /**
     * Set the secondary grouping size. If set to a value less than 1, then secondary grouping is turned off, and the
     * primary grouping size is used for all intervals, not just the least significant.
     * 
     * @see #getSecondaryGroupingSize
     * @see NumberFormat#setGroupingUsed
     * @see DecimalFormatSymbols#setGroupingSeparator
     * @stable ICU 2.0
     */
    public void setSecondaryGroupingSize(int newValue) {
        groupingSize2 = (byte) newValue;
    }

    /**
     * Returns the MathContext used by this format.
     * 
     * @return desired MathContext
     * @see #mathContext
     * @see #getMathContext
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public MathContext getMathContextICU() {
        return mathContext;
    }

    /**
     * Returns the MathContext used by this format.
     * 
     * @return desired MathContext
     * @see #mathContext
     * @see #getMathContext
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public java.math.MathContext getMathContext() {
        try {
            // don't allow multiple references
            return mathContext == null ? null : new java.math.MathContext(mathContext.getDigits(),
                    java.math.RoundingMode.valueOf(mathContext.getRoundingMode()));
        } catch (Exception foo) {
            return null; // should never happen
        }
    }

    /**
     * Sets the MathContext used by this format.
     * 
     * @param newValue
     *            desired MathContext
     * @see #mathContext
     * @see #getMathContext
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public void setMathContextICU(MathContext newValue) {
        mathContext = newValue;
    }

    /**
     * Sets the MathContext used by this format.
     * 
     * @param newValue
     *            desired MathContext
     * @see #mathContext
     * @see #getMathContext
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public void setMathContext(java.math.MathContext newValue) {
        mathContext = new MathContext(newValue.getPrecision(), MathContext.SCIENTIFIC, false, (newValue
                .getRoundingMode()).ordinal());
    }

    /**
     * Allows you to get the behavior of the decimal separator with integers. (The decimal separator will always appear
     * with decimals.)
     * <P>
     * Example: Decimal ON: 12345 -> 12345.; OFF: 12345 -> 12345
     * 
     * @stable ICU 2.0
     */
    public boolean isDecimalSeparatorAlwaysShown() {
        return decimalSeparatorAlwaysShown;
    }

    /**
     * Allows you to set the behavior of the decimal separator with integers. (The decimal separator will always appear
     * with decimals.)
     * 
     * <p>
     * This only affects formatting, and only where there might be no digits after the decimal point, e.g., if true,
     * 3456.00 -> "3,456." if false, 3456.00 -> "3456" This is independent of parsing. If you want parsing to stop at
     * the decimal point, use setParseIntegerOnly.
     * 
     * <P>
     * Example: Decimal ON: 12345 -> 12345.; OFF: 12345 -> 12345
     * 
     * @stable ICU 2.0
     */
    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        decimalSeparatorAlwaysShown = newValue;
    }

    /**
     * Returns a copy of the CurrencyPluralInfo used by this format. It might return null if the decimal format is not a
     * plural type currency decimal format. Plural type currency decimal format means either the pattern in the decimal
     * format contains 3 currency signs, or the decimal format is initialized with PLURALCURRENCYSTYLE.
     * 
     * @return desired CurrencyPluralInfo
     * @see CurrencyPluralInfo
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public CurrencyPluralInfo getCurrencyPluralInfo() {
        try {
            // don't allow multiple references
            return currencyPluralInfo == null ? null : (CurrencyPluralInfo) currencyPluralInfo.clone();
        } catch (Exception foo) {
            return null; // should never happen
        }
    }

    /**
     * Sets the CurrencyPluralInfo used by this format. The format uses a copy of the provided information.
     * 
     * @param newInfo
     *            desired CurrencyPluralInfo
     * @see CurrencyPluralInfo
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public void setCurrencyPluralInfo(CurrencyPluralInfo newInfo) {
        currencyPluralInfo = (CurrencyPluralInfo) newInfo.clone();
        isReadyForParsing = false;
    }

    /**
     * Standard override; no change in semantics.
     * 
     * @stable ICU 2.0
     */
    public Object clone() {
        try {
            DecimalFormat other = (DecimalFormat) super.clone();
            other.symbols = (DecimalFormatSymbols) symbols.clone();
            other.digitList = new DigitList(); // fix for JB#5358
            if (currencyPluralInfo != null) {
                other.currencyPluralInfo = (CurrencyPluralInfo) currencyPluralInfo.clone();
            }
            /*
             * TODO: We need to figure out whether we share a single copy of DigitList by multiple cloned copies.
             * format/subformat are designed to use a single instance, but parse/subparse implementation is not.
             */
            return other;
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    /**
     * Overrides equals
     * 
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!super.equals(obj))
            return false; // super does class check

        DecimalFormat other = (DecimalFormat) obj;
        /*
         * Add the comparison of the four new added fields ,they are posPrefixPattern, posSuffixPattern,
         * negPrefixPattern, negSuffixPattern. [Richard/GCL]
         */
        // following are added to accomodate changes for currency plural format.
        return currencySignCount == other.currencySignCount
                && (style != NumberFormat.PLURALCURRENCYSTYLE || equals(posPrefixPattern, other.posPrefixPattern)
                        && equals(posSuffixPattern, other.posSuffixPattern)
                        && equals(negPrefixPattern, other.negPrefixPattern)
                        && equals(negSuffixPattern, other.negSuffixPattern))
                && multiplier == other.multiplier
                && groupingSize == other.groupingSize
                && groupingSize2 == other.groupingSize2
                && decimalSeparatorAlwaysShown == other.decimalSeparatorAlwaysShown
                && useExponentialNotation == other.useExponentialNotation
                && (!useExponentialNotation || minExponentDigits == other.minExponentDigits)
                && useSignificantDigits == other.useSignificantDigits
                && (!useSignificantDigits || minSignificantDigits == other.minSignificantDigits
                        && maxSignificantDigits == other.maxSignificantDigits) && symbols.equals(other.symbols)
                && Utility.objectEquals(currencyPluralInfo, other.currencyPluralInfo);
    }

    // method to unquote the strings and compare
    private boolean equals(String pat1, String pat2) {
        if (pat1 == null || pat2 == null) {
            return (pat1 == null && pat2 == null);
        }
        // fast path
        if (pat1.equals(pat2)) {
            return true;
        }
        return unquote(pat1).equals(unquote(pat2));
    }

    private String unquote(String pat) {
        StringBuffer buf = new StringBuffer(pat.length());
        int i = 0;
        while (i < pat.length()) {
            char ch = pat.charAt(i++);
            if (ch != QUOTE) {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    // protected void handleToString(StringBuffer buf) {
    // buf.append("\nposPrefixPattern: '" + posPrefixPattern + "'\n");
    // buf.append("positivePrefix: '" + positivePrefix + "'\n");
    // buf.append("posSuffixPattern: '" + posSuffixPattern + "'\n");
    // buf.append("positiveSuffix: '" + positiveSuffix + "'\n");
    // buf.append("negPrefixPattern: '" + com.ibm.icu.impl.Utility.format1ForSource(negPrefixPattern) + "'\n");
    // buf.append("negativePrefix: '" + com.ibm.icu.impl.Utility.format1ForSource(negativePrefix) + "'\n");
    // buf.append("negSuffixPattern: '" + negSuffixPattern + "'\n");
    // buf.append("negativeSuffix: '" + negativeSuffix + "'\n");
    // buf.append("multiplier: '" + multiplier + "'\n");
    // buf.append("groupingSize: '" + groupingSize + "'\n");
    // buf.append("groupingSize2: '" + groupingSize2 + "'\n");
    // buf.append("decimalSeparatorAlwaysShown: '" + decimalSeparatorAlwaysShown + "'\n");
    // buf.append("useExponentialNotation: '" + useExponentialNotation + "'\n");
    // buf.append("minExponentDigits: '" + minExponentDigits + "'\n");
    // buf.append("useSignificantDigits: '" + useSignificantDigits + "'\n");
    // buf.append("minSignificantDigits: '" + minSignificantDigits + "'\n");
    // buf.append("maxSignificantDigits: '" + maxSignificantDigits + "'\n");
    // buf.append("symbols: '" + symbols + "'");
    // }

    /**
     * Overrides hashCode
     * 
     * @stable ICU 2.0
     */
    public int hashCode() {
        return super.hashCode() * 37 + positivePrefix.hashCode();
        // just enough fields for a reasonable distribution
    }

    /**
     * Synthesizes a pattern string that represents the current state of this Format object.
     * 
     * @see #applyPattern
     * @stable ICU 2.0
     */
    public String toPattern() {
        if (style == NumberFormat.PLURALCURRENCYSTYLE) {
            // the prefix or suffix pattern might not be defined yet,
            // so they can not be synthesized,
            // instead, get them directly.
            // but it might not be the actual pattern used in formatting.
            // the actual pattern used in formatting depends on the
            // formatted number's plural count.
            return formatPattern;
        }
        return toPattern(false);
    }

    /**
     * Synthesizes a localized pattern string that represents the current state of this Format object.
     * 
     * @see #applyPattern
     * @stable ICU 2.0
     */
    public String toLocalizedPattern() {
        if (style == NumberFormat.PLURALCURRENCYSTYLE) {
            return formatPattern;
        }
        return toPattern(true);
    }

    /**
     * Expand the affix pattern strings into the expanded affix strings. If any affix pattern string is null, do not
     * expand it. This method should be called any time the symbols or the affix patterns change in order to keep the
     * expanded affix strings up to date. This method also will be called before formatting if format currency plural
     * names, since the plural name is not a static one, it is based on the currency plural count, the affix will be
     * known only after the currency plural count is know. In which case, the parameter 'pluralCount' will be a non-null
     * currency plural count. In all other cases, the 'pluralCount' is null, which means it is not needed.
     */
    // Bug 4212072 [Richard/GCL]
    private void expandAffixes(String pluralCount) {
        // expandAffix() will set currencyChoice to a non-null value if
        // appropriate AND if it is null.
        currencyChoice = null;

        // Reuse one StringBuffer for better performance
        StringBuffer buffer = new StringBuffer();
        if (posPrefixPattern != null) {
            expandAffix(posPrefixPattern, pluralCount, buffer, false);
            positivePrefix = buffer.toString();
        }
        if (posSuffixPattern != null) {
            expandAffix(posSuffixPattern, pluralCount, buffer, false);
            positiveSuffix = buffer.toString();
        }
        if (negPrefixPattern != null) {
            expandAffix(negPrefixPattern, pluralCount, buffer, false);
            negativePrefix = buffer.toString();
        }
        if (negSuffixPattern != null) {
            expandAffix(negSuffixPattern, pluralCount, buffer, false);
            negativeSuffix = buffer.toString();
        }
    }

    /**
     * Expand an affix pattern into an affix string. All characters in the pattern are literal unless bracketed by
     * QUOTEs. The following characters outside QUOTE are recognized: PATTERN_PERCENT, PATTERN_PER_MILLE, PATTERN_MINUS,
     * and CURRENCY_SIGN. If CURRENCY_SIGN is doubled, it is interpreted as an international currency sign. If
     * CURRENCY_SIGN is tripled, it is interpreted as currency plural long names, such as "US Dollars". Any other
     * character outside QUOTE represents itself. Quoted text must be well-formed.
     * 
     * This method is used in two distinct ways. First, it is used to expand the stored affix patterns into actual
     * affixes. For this usage, doFormat must be false. Second, it is used to expand the stored affix patterns given a
     * specific number (doFormat == true), for those rare cases in which a currency format references a ChoiceFormat
     * (e.g., en_IN display name for INR). The number itself is taken from digitList.
     * 
     * When used in the first way, this method has a side effect: It sets currencyChoice to a ChoiceFormat object, if
     * the currency's display name in this locale is a ChoiceFormat pattern (very rare). It only does this if
     * currencyChoice is null to start with.
     * 
     * @param pattern
     *            the non-null, possibly empty pattern
     * @param pluralCount
     *            the plural count. It is only used for currency plural format. In which case, it is the plural count of
     *            the currency amount. For example, in en_US, it is the singular "one", or the plural "other". For all
     *            other cases, it is null, and is not being used.
     * @param buffer
     *            a scratch StringBuffer; its contents will be lost
     * @param doFormat
     *            if false, then the pattern will be expanded, and if a currency symbol is encountered that expands to a
     *            ChoiceFormat, the currencyChoice member variable will be initialized if it is null. If doFormat is
     *            true, then it is assumed that the currencyChoice has been created, and it will be used to format the
     *            value in digitList.
     * @return the expanded equivalent of pattern
     */
    // Bug 4212072 [Richard/GCL]
    private void expandAffix(String pattern, String pluralCount, StringBuffer buffer, boolean doFormat) {
        buffer.setLength(0);
        for (int i = 0; i < pattern.length();) {
            char c = pattern.charAt(i++);
            if (c == QUOTE) {
                for (;;) {
                    int j = pattern.indexOf(QUOTE, i);
                    if (j == i) {
                        buffer.append(QUOTE);
                        i = j + 1;
                        break;
                    } else if (j > i) {
                        buffer.append(pattern.substring(i, j));
                        i = j + 1;
                        if (i < pattern.length() && pattern.charAt(i) == QUOTE) {
                            buffer.append(QUOTE);
                            ++i;
                            // loop again
                        } else {
                            break;
                        }
                    } else {
                        // Unterminated quote; should be caught by apply
                        // pattern.
                        throw new RuntimeException();
                    }
                }
                continue;
            }

            switch (c) {
            case CURRENCY_SIGN:
                // As of ICU 2.2 we use the currency object, and
                // ignore the currency symbols in the DFS, unless
                // we have a null currency object. This occurs if
                // resurrecting a pre-2.2 object or if the user
                // sets a custom DFS.
                boolean intl = i < pattern.length() && pattern.charAt(i) == CURRENCY_SIGN;
                boolean plural = false;
                if (intl) {
                    ++i;
                    if (i < pattern.length() && pattern.charAt(i) == CURRENCY_SIGN) {
                        plural = true;
                        intl = false;
                        ++i;
                    }
                }
                String s = null;
                Currency currency = getCurrency();
                if (currency != null) {
                    // plural name is only needed when pluralCount != null,
                    // which means when formatting currency plural names.
                    // For other cases, pluralCount == null,
                    // and plural names are not needed.
                    if (plural && pluralCount != null) {
                        boolean isChoiceFormat[] = new boolean[1];
                        s = currency.getName(symbols.getULocale(), Currency.PLURAL_LONG_NAME, pluralCount,
                                isChoiceFormat);
                    } else if (!intl) {
                        boolean isChoiceFormat[] = new boolean[1];
                        s = currency.getName(symbols.getULocale(), Currency.SYMBOL_NAME, isChoiceFormat);
                        if (isChoiceFormat[0]) {
                            // Two modes here: If doFormat is false, we set up
                            // currencyChoice. If doFormat is true, we use the
                            // previously created currencyChoice to format the
                            // value in digitList.
                            if (!doFormat) {
                                // If the currency is handled by a ChoiceFormat,
                                // then we're not going to use the expanded
                                // patterns. Instantiate the ChoiceFormat and
                                // return.
                                if (currencyChoice == null) {
                                    currencyChoice = new ChoiceFormat(s);
                                }
                                // We could almost return null or "" here, since the
                                // expanded affixes are almost not used at all
                                // in this situation. However, one method --
                                // toPattern() -- still does use the expanded
                                // affixes, in order to set up a padding
                                // pattern. We use the CURRENCY_SIGN as a
                                // placeholder.
                                s = String.valueOf(CURRENCY_SIGN);
                            } else {
                                FieldPosition pos = new FieldPosition(0); // ignored
                                currencyChoice.format(digitList.getDouble(), buffer, pos);
                                continue;
                            }
                        }
                    } else {
                        s = currency.getCurrencyCode();
                    }
                } else {
                    s = intl ? symbols.getInternationalCurrencySymbol() : symbols.getCurrencySymbol();
                }
                buffer.append(s);
                continue;
            case PATTERN_PERCENT:
                c = symbols.getPercent();
                break;
            case PATTERN_PER_MILLE:
                c = symbols.getPerMill();
                break;
            case PATTERN_MINUS:
                c = symbols.getMinusSign();
                break;
            }
            buffer.append(c);
        }
    }

    /**
     * Append an affix to the given StringBuffer.
     * 
     * @param buf
     *            buffer to append to
     * @param isNegative
     * @param isPrefix
     */
    private int appendAffix(StringBuffer buf, boolean isNegative, boolean isPrefix, boolean parseAttr) {
        if (currencyChoice != null) {
            String affixPat = null;
            if (isPrefix) {
                affixPat = isNegative ? negPrefixPattern : posPrefixPattern;
            } else {
                affixPat = isNegative ? negSuffixPattern : posSuffixPattern;
            }
            StringBuffer affixBuf = new StringBuffer();
            expandAffix(affixPat, null, affixBuf, true);
            buf.append(affixBuf.toString());
            return affixBuf.length();
        }

        String affix = null;
        if (isPrefix) {
            affix = isNegative ? negativePrefix : positivePrefix;
        } else {
            affix = isNegative ? negativeSuffix : positiveSuffix;
        }
        // [Spark/CDL] Invoke formatAffix2Attribute to add attributes for affix
        if (parseAttr) {
            int offset = affix.indexOf(symbols.getCurrencySymbol());
            if (-1 == offset) {
                offset = affix.indexOf(symbols.getPercent());
                if (-1 == offset) {
                    offset = 0;
                }
            }
            formatAffix2Attribute(affix, buf.length() + offset, buf.length() + affix.length());
        }
        buf.append(affix);
        return affix.length();
    }

    /*
     * [Spark/CDL] This is a newly added method, used to add attributes for prefix and suffix.
     */
    private void formatAffix2Attribute(String affix, int begin, int end) {
        // [Spark/CDL] It is the invoker's responsibility to ensure that, before
        // the invocation of
        // this method, attributes is not null.
        // if( attributes == null ) return;
        if (affix.indexOf(symbols.getCurrencySymbol()) > -1) {
            addAttribute(Field.CURRENCY, begin, end);
        } else if (affix.indexOf(symbols.getMinusSign()) > -1) {
            addAttribute(Field.SIGN, begin, end);
        } else if (affix.indexOf(symbols.getPercent()) > -1) {
            addAttribute(Field.PERCENT, begin, end);
        } else if (affix.indexOf(symbols.getPerMill()) > -1) {
            addAttribute(Field.PERMILLE, begin, end);
        }
    }

    /*
     * [Spark/CDL] Use this method to add attribute.
     */
    private void addAttribute(Field field, int begin, int end) {
        FieldPosition pos = new FieldPosition(field);
        pos.setBeginIndex(begin);
        pos.setEndIndex(end);
        attributes.add(pos);
    }

    /**
     * Format the object to an attributed string, and return the corresponding iterator Overrides superclass method.
     * 
     * @stable ICU 3.6
     */
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        if (!(obj instanceof Number))
            throw new IllegalArgumentException();
        Number number = (Number) obj;
        StringBuffer text = null;
        attributes.clear();
        if (obj instanceof BigInteger) {
            text = format((BigInteger) number, new StringBuffer(), new FieldPosition(0), true);
        } else if (obj instanceof java.math.BigDecimal) {
            text = format((java.math.BigDecimal) number, new StringBuffer(), new FieldPosition(0), true);
        } else if (obj instanceof Double) {
            text = format(number.doubleValue(), new StringBuffer(), new FieldPosition(0), true);
        } else if (obj instanceof Integer || obj instanceof Long) {
            text = format(number.longValue(), new StringBuffer(), new FieldPosition(0), true);
        }

        AttributedString as = new AttributedString(text.toString());

        // add NumberFormat field attributes to the AttributedString
        for (int i = 0; i < attributes.size(); i++) {
            FieldPosition pos = attributes.get(i);
            Format.Field attribute = pos.getFieldAttribute();
            as.addAttribute(attribute, attribute, pos.getBeginIndex(), pos.getEndIndex());
        }

        // return the CharacterIterator from AttributedString
        return as.getIterator();
    }

    /**
     * Append an affix pattern to the given StringBuffer. Localize unquoted specials.
     */
    private void appendAffixPattern(StringBuffer buffer, boolean isNegative, boolean isPrefix, boolean localized) {
        String affixPat = null;
        if (isPrefix) {
            affixPat = isNegative ? negPrefixPattern : posPrefixPattern;
        } else {
            affixPat = isNegative ? negSuffixPattern : posSuffixPattern;
        }

        // When there is a null affix pattern, we use the affix itself.
        if (affixPat == null) {
            String affix = null;
            if (isPrefix) {
                affix = isNegative ? negativePrefix : positivePrefix;
            } else {
                affix = isNegative ? negativeSuffix : positiveSuffix;
            }
            // Do this crudely for now: Wrap everything in quotes.
            buffer.append(QUOTE);
            for (int i = 0; i < affix.length(); ++i) {
                char ch = affix.charAt(i);
                if (ch == QUOTE) {
                    buffer.append(ch);
                }
                buffer.append(ch);
            }
            buffer.append(QUOTE);
            return;
        }

        if (!localized) {
            buffer.append(affixPat);
        } else {
            int i, j;
            for (i = 0; i < affixPat.length(); ++i) {
                char ch = affixPat.charAt(i);
                switch (ch) {
                case QUOTE:
                    j = affixPat.indexOf(QUOTE, i + 1);
                    if (j < 0) {
                        throw new IllegalArgumentException("Malformed affix pattern: " + affixPat);
                    }
                    buffer.append(affixPat.substring(i, j + 1));
                    i = j;
                    continue;
                case PATTERN_PER_MILLE:
                    ch = symbols.getPerMill();
                    break;
                case PATTERN_PERCENT:
                    ch = symbols.getPercent();
                    break;
                case PATTERN_MINUS:
                    ch = symbols.getMinusSign();
                    break;
                }
                // check if char is same as any other symbol
                if (ch == symbols.getDecimalSeparator() || ch == symbols.getGroupingSeparator()) {
                    buffer.append(QUOTE);
                    buffer.append(ch);
                    buffer.append(QUOTE);
                } else {
                    buffer.append(ch);
                }
            }
        }
    }

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong> Does the real work of generating a pattern.
     */
    private String toPattern(boolean localized) {
        StringBuffer result = new StringBuffer();
        char zero = localized ? symbols.getZeroDigit() : PATTERN_ZERO_DIGIT;
        char digit = localized ? symbols.getDigit() : PATTERN_DIGIT;
        char sigDigit = 0;
        boolean useSigDig = areSignificantDigitsUsed();
        if (useSigDig) {
            sigDigit = localized ? symbols.getSignificantDigit() : PATTERN_SIGNIFICANT_DIGIT;
        }
        char group = localized ? symbols.getGroupingSeparator() : PATTERN_GROUPING_SEPARATOR;
        int i;
        int roundingDecimalPos = 0; // Pos of decimal in roundingDigits
        String roundingDigits = null;
        int padPos = (formatWidth > 0) ? padPosition : -1;
        String padSpec = (formatWidth > 0) ? new StringBuffer(2).append(
                localized ? symbols.getPadEscape() : PATTERN_PAD_ESCAPE).append(pad).toString() : null;
        if (roundingIncrementICU != null) {
            i = roundingIncrementICU.scale();
            roundingDigits = roundingIncrementICU.movePointRight(i).toString();
            roundingDecimalPos = roundingDigits.length() - i;
        }
        for (int part = 0; part < 2; ++part) {
            // variable not used int partStart = result.length();
            if (padPos == PAD_BEFORE_PREFIX) {
                result.append(padSpec);
            }
            /*
             * Use original symbols read from resources in pattern eg. use "\u00A4" instead of "$" in Locale.US
             * [Richard/GCL]
             */
            appendAffixPattern(result, part != 0, true, localized);
            if (padPos == PAD_AFTER_PREFIX) {
                result.append(padSpec);
            }
            int sub0Start = result.length();
            int g = isGroupingUsed() ? Math.max(0, groupingSize) : 0;
            if (g > 0 && groupingSize2 > 0 && groupingSize2 != groupingSize) {
                g += groupingSize2;
            }
            int maxDig = 0, minDig = 0, maxSigDig = 0;
            if (useSigDig) {
                minDig = getMinimumSignificantDigits();
                maxDig = maxSigDig = getMaximumSignificantDigits();
            } else {
                minDig = getMinimumIntegerDigits();
                maxDig = getMaximumIntegerDigits();
            }
            if (useExponentialNotation) {
                if (maxDig > MAX_SCIENTIFIC_INTEGER_DIGITS) {
                    maxDig = 1;
                }
            } else if (useSigDig) {
                maxDig = Math.max(maxDig, g + 1);
            } else {
                maxDig = Math.max(Math.max(g, getMinimumIntegerDigits()), roundingDecimalPos) + 1;
            }
            for (i = maxDig; i > 0; --i) {
                if (!useExponentialNotation && i < maxDig && isGroupingPosition(i)) {
                    result.append(group);
                }
                if (useSigDig) {
                    // #@,@### (maxSigDig == 5, minSigDig == 2)
                    // 65 4321 (1-based pos, count from the right)
                    // Use # if pos > maxSigDig or 1 <= pos <= (maxSigDig - minSigDig)
                    // Use @ if (maxSigDig - minSigDig) < pos <= maxSigDig
                    result.append((maxSigDig >= i && i > (maxSigDig - minDig)) ? sigDigit : digit);
                } else {
                    if (roundingDigits != null) {
                        int pos = roundingDecimalPos - i;
                        if (pos >= 0 && pos < roundingDigits.length()) {
                            result.append((char) (roundingDigits.charAt(pos) - '0' + zero));
                            continue;
                        }
                    }
                    result.append(i <= minDig ? zero : digit);
                }
            }
            if (!useSigDig) {
                if (getMaximumFractionDigits() > 0 || decimalSeparatorAlwaysShown) {
                    result.append(localized ? symbols.getDecimalSeparator() : PATTERN_DECIMAL_SEPARATOR);
                }
                int pos = roundingDecimalPos;
                for (i = 0; i < getMaximumFractionDigits(); ++i) {
                    if (roundingDigits != null && pos < roundingDigits.length()) {
                        result.append(pos < 0 ? zero : (char) (roundingDigits.charAt(pos) - '0' + zero));
                        ++pos;
                        continue;
                    }
                    result.append(i < getMinimumFractionDigits() ? zero : digit);
                }
            }
            if (useExponentialNotation) {
                if (localized) {
                    result.append(symbols.getExponentSeparator());
                } else {
                    result.append(PATTERN_EXPONENT);
                }
                if (exponentSignAlwaysShown) {
                    result.append(localized ? symbols.getPlusSign() : PATTERN_PLUS_SIGN);
                }
                for (i = 0; i < minExponentDigits; ++i) {
                    result.append(zero);
                }
            }
            if (padSpec != null && !useExponentialNotation) {
                int add = formatWidth
                        - result.length()
                        + sub0Start
                        - ((part == 0) ? positivePrefix.length() + positiveSuffix.length() : negativePrefix.length()
                                + negativeSuffix.length());
                while (add > 0) {
                    result.insert(sub0Start, digit);
                    ++maxDig;
                    --add;
                    // Only add a grouping separator if we have at least
                    // 2 additional characters to be added, so we don't
                    // end up with ",###".
                    if (add > 1 && isGroupingPosition(maxDig)) {
                        result.insert(sub0Start, group);
                        --add;
                    }
                }
            }
            if (padPos == PAD_BEFORE_SUFFIX) {
                result.append(padSpec);
            }
            /*
             * Use original symbols read from resources in pattern eg. use "\u00A4" instead of "$" in Locale.US
             * [Richard/GCL]
             */
            appendAffixPattern(result, part != 0, false, localized);
            if (padPos == PAD_AFTER_SUFFIX) {
                result.append(padSpec);
            }
            if (part == 0) {
                if (negativeSuffix.equals(positiveSuffix) && negativePrefix.equals(PATTERN_MINUS + positivePrefix)) {
                    break;
                } else {
                    result.append(localized ? symbols.getPatternSeparator() : PATTERN_SEPARATOR);
                }
            }
        }
        return result.toString();
    }

    /**
     * Apply the given pattern to this Format object. A pattern is a short-hand specification for the various formatting
     * properties. These properties can also be changed individually through the various setter methods.
     * <p>
     * There is no limit to integer digits are set by this routine, since that is the typical end-user desire; use
     * setMaximumInteger if you want to set a real value. For negative numbers, use a second pattern, separated by a
     * semicolon
     * <P>
     * Example "#,#00.0#" -> 1,234.56
     * <P>
     * This means a minimum of 2 integer digits, 1 fraction digit, and a maximum of 2 fraction digits.
     * <p>
     * Example: "#,#00.0#;(#,#00.0#)" for negatives in parentheses.
     * <p>
     * In negative patterns, the minimum and maximum counts are ignored; these are presumed to be set in the positive
     * pattern.
     * 
     * @stable ICU 2.0
     */
    public void applyPattern(String pattern) {
        applyPattern(pattern, false);
    }

    /**
     * Apply the given pattern to this Format object. The pattern is assumed to be in a localized notation. A pattern is
     * a short-hand specification for the various formatting properties. These properties can also be changed
     * individually through the various setter methods.
     * <p>
     * There is no limit to integer digits are set by this routine, since that is the typical end-user desire; use
     * setMaximumInteger if you want to set a real value. For negative numbers, use a second pattern, separated by a
     * semicolon
     * <P>
     * Example "#,#00.0#" -> 1,234.56
     * <P>
     * This means a minimum of 2 integer digits, 1 fraction digit, and a maximum of 2 fraction digits.
     * <p>
     * Example: "#,#00.0#;(#,#00.0#)" for negatives in parantheses.
     * <p>
     * In negative patterns, the minimum and maximum counts are ignored; these are presumed to be set in the positive
     * pattern.
     * 
     * @stable ICU 2.0
     */
    public void applyLocalizedPattern(String pattern) {
        applyPattern(pattern, true);
    }

    /**
     * <strong><font face=helvetica color=red>CHANGED</font></strong> Does the real work of applying a pattern.
     */
    private void applyPattern(String pattern, boolean localized) {
        applyPatternWithoutExpandAffix(pattern, localized);
        expandAffixAdjustWidth(null);
    }

    private void expandAffixAdjustWidth(String pluralCount) {
        /*
         * Bug 4212072 Update the affix strings according to symbols in order to keep the affix strings up to date.
         * [Richard/GCL]
         */
        expandAffixes(pluralCount);

        // Now that we have the actual prefix and suffix, fix up formatWidth
        if (formatWidth > 0) {
            formatWidth += positivePrefix.length() + positiveSuffix.length();
        }
    }

    private void applyPatternWithoutExpandAffix(String pattern, boolean localized) {
        char zeroDigit = PATTERN_ZERO_DIGIT; // '0'
        char sigDigit = PATTERN_SIGNIFICANT_DIGIT; // '@'
        char groupingSeparator = PATTERN_GROUPING_SEPARATOR;
        char decimalSeparator = PATTERN_DECIMAL_SEPARATOR;
        char percent = PATTERN_PERCENT;
        char perMill = PATTERN_PER_MILLE;
        char digit = PATTERN_DIGIT; // '#'
        char separator = PATTERN_SEPARATOR;
        String exponent = String.valueOf(PATTERN_EXPONENT);
        char plus = PATTERN_PLUS_SIGN;
        char padEscape = PATTERN_PAD_ESCAPE;
        char minus = PATTERN_MINUS; // Bug 4212072 [Richard/GCL]
        if (localized) {
            zeroDigit = symbols.getZeroDigit();
            sigDigit = symbols.getSignificantDigit();
            groupingSeparator = symbols.getGroupingSeparator();
            decimalSeparator = symbols.getDecimalSeparator();
            percent = symbols.getPercent();
            perMill = symbols.getPerMill();
            digit = symbols.getDigit();
            separator = symbols.getPatternSeparator();
            exponent = symbols.getExponentSeparator();
            plus = symbols.getPlusSign();
            padEscape = symbols.getPadEscape();
            minus = symbols.getMinusSign(); // Bug 4212072 [Richard/GCL]
        }
        char nineDigit = (char) (zeroDigit + 9);

        boolean gotNegative = false;

        int pos = 0;
        // Part 0 is the positive pattern. Part 1, if present, is the negative
        // pattern.
        for (int part = 0; part < 2 && pos < pattern.length(); ++part) {
            // The subpart ranges from 0 to 4: 0=pattern proper, 1=prefix,
            // 2=suffix, 3=prefix in quote, 4=suffix in quote. Subpart 0 is
            // between the prefix and suffix, and consists of pattern
            // characters. In the prefix and suffix, percent, permille, and
            // currency symbols are recognized and translated.
            int subpart = 1, sub0Start = 0, sub0Limit = 0, sub2Limit = 0;

            // It's important that we don't change any fields of this object
            // prematurely. We set the following variables for the multiplier,
            // grouping, etc., and then only change the actual object fields if
            // everything parses correctly. This also lets us register
            // the data from part 0 and ignore the part 1, except for the
            // prefix and suffix.
            StringBuffer prefix = new StringBuffer();
            StringBuffer suffix = new StringBuffer();
            int decimalPos = -1;
            int multpl = 1;
            int digitLeftCount = 0, zeroDigitCount = 0, digitRightCount = 0, sigDigitCount = 0;
            byte groupingCount = -1;
            byte groupingCount2 = -1;
            int padPos = -1;
            char padChar = 0;
            int incrementPos = -1;
            long incrementVal = 0;
            byte expDigits = -1;
            boolean expSignAlways = false;

            // The affix is either the prefix or the suffix.
            StringBuffer affix = prefix;

            int start = pos;

            PARTLOOP: for (; pos < pattern.length(); ++pos) {
                char ch = pattern.charAt(pos);
                switch (subpart) {
                case 0: // Pattern proper subpart (between prefix & suffix)
                    // Process the digits, decimal, and grouping characters. We
                    // record five pieces of information. We expect the digits
                    // to occur in the pattern ####00.00####, and we record the
                    // number of left digits, zero (central) digits, and right
                    // digits. The position of the last grouping character is
                    // recorded (should be somewhere within the first two blocks
                    // of characters), as is the position of the decimal point,
                    // if any (should be in the zero digits). If there is no
                    // decimal point, then there should be no right digits.
                    if (ch == digit) {
                        if (zeroDigitCount > 0 || sigDigitCount > 0) {
                            ++digitRightCount;
                        } else {
                            ++digitLeftCount;
                        }
                        if (groupingCount >= 0 && decimalPos < 0) {
                            ++groupingCount;
                        }
                    } else if ((ch >= zeroDigit && ch <= nineDigit) || ch == sigDigit) {
                        if (digitRightCount > 0) {
                            patternError("Unexpected '" + ch + '\'', pattern);
                        }
                        if (ch == sigDigit) {
                            ++sigDigitCount;
                        } else {
                            ++zeroDigitCount;
                            if (ch != zeroDigit) {
                                int p = digitLeftCount + zeroDigitCount + digitRightCount;
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
                        }
                        if (groupingCount >= 0 && decimalPos < 0) {
                            ++groupingCount;
                        }
                    } else if (ch == groupingSeparator) {
                        /*
                         * Bug 4212072 process the Localized pattern like "'Fr. '#'##0.05;'Fr.-'#'##0.05" (Locale="CH",
                         * groupingSeparator == QUOTE) [Richard/GCL]
                         */
                        if (ch == QUOTE && (pos + 1) < pattern.length()) {
                            char after = pattern.charAt(pos + 1);
                            if (!(after == digit || (after >= zeroDigit && after <= nineDigit))) {
                                // A quote outside quotes indicates either the opening
                                // quote or two quotes, which is a quote literal. That is,
                                // we have the first quote in 'do' or o''clock.
                                if (after == QUOTE) {
                                    ++pos;
                                    // Fall through to append(ch)
                                } else {
                                    if (groupingCount < 0) {
                                        subpart = 3; // quoted prefix subpart
                                    } else {
                                        // Transition to suffix subpart
                                        subpart = 2; // suffix subpart
                                        affix = suffix;
                                        sub0Limit = pos--;
                                    }
                                    continue;
                                }
                            }
                        }

                        if (decimalPos >= 0) {
                            patternError("Grouping separator after decimal", pattern);
                        }
                        groupingCount2 = groupingCount;
                        groupingCount = 0;
                    } else if (ch == decimalSeparator) {
                        if (decimalPos >= 0) {
                            patternError("Multiple decimal separators", pattern);
                        }
                        // Intentionally incorporate the digitRightCount,
                        // even though it is illegal for this to be > 0
                        // at this point. We check pattern syntax below.
                        decimalPos = digitLeftCount + zeroDigitCount + digitRightCount;
                    } else {
                        if (pattern.regionMatches(pos, exponent, 0, exponent.length())) {
                            if (expDigits >= 0) {
                                patternError("Multiple exponential symbols", pattern);
                            }
                            if (groupingCount >= 0) {
                                patternError("Grouping separator in exponential", pattern);
                            }
                            pos += exponent.length();
                            // Check for positive prefix
                            if (pos < pattern.length() && pattern.charAt(pos) == plus) {
                                expSignAlways = true;
                                ++pos;
                            }
                            // Use lookahead to parse out the exponential part of the
                            // pattern, then jump into suffix subpart.
                            expDigits = 0;
                            while (pos < pattern.length() && pattern.charAt(pos) == zeroDigit) {
                                ++expDigits;
                                ++pos;
                            }

                            // 1. Require at least one mantissa pattern digit
                            // 2. Disallow "#+ @" in mantissa
                            // 3. Require at least one exponent pattern digit
                            if (((digitLeftCount + zeroDigitCount) < 1 && (sigDigitCount + digitRightCount) < 1)
                                    || (sigDigitCount > 0 && digitLeftCount > 0) || expDigits < 1) {
                                patternError("Malformed exponential", pattern);
                            }
                        }
                        // Transition to suffix subpart
                        subpart = 2; // suffix subpart
                        affix = suffix;
                        sub0Limit = pos--; // backup: for() will increment
                        continue;
                    }
                    break;
                case 1: // Prefix subpart
                case 2: // Suffix subpart
                    // Process the prefix / suffix characters
                    // Process unquoted characters seen in prefix or suffix
                    // subpart.

                    // Several syntax characters implicitly begins the
                    // next subpart if we are in the prefix; otherwise
                    // they are illegal if unquoted.
                    if (ch == digit || ch == groupingSeparator || ch == decimalSeparator
                            || (ch >= zeroDigit && ch <= nineDigit) || ch == sigDigit) {
                        // Any of these characters implicitly begins the
                        // next subpart if we are in the prefix
                        if (subpart == 1) { // prefix subpart
                            subpart = 0; // pattern proper subpart
                            sub0Start = pos--; // Reprocess this character
                            continue;
                        } else if (ch == QUOTE) {
                            /*
                             * Bug 4212072 process the Localized pattern like "'Fr. '#'##0.05;'Fr.-'#'##0.05"
                             * (Locale="CH", groupingSeparator == QUOTE) [Richard/GCL]
                             */
                            // A quote outside quotes indicates either the opening
                            // quote or two quotes, which is a quote literal. That is,
                            // we have the first quote in 'do' or o''clock.
                            if ((pos + 1) < pattern.length() && pattern.charAt(pos + 1) == QUOTE) {
                                ++pos;
                                affix.append(ch);
                            } else {
                                subpart += 2; // open quote
                            }
                            continue;
                        }
                        patternError("Unquoted special character '" + ch + '\'', pattern);
                    } else if (ch == CURRENCY_SIGN) {
                        // Use lookahead to determine if the currency sign is
                        // doubled or not.
                        boolean doubled = (pos + 1) < pattern.length() && pattern.charAt(pos + 1) == CURRENCY_SIGN;
                        /*
                         * Bug 4212072 To meet the need of expandAffix(String, StirngBuffer) [Richard/GCL]
                         */
                        if (doubled) {
                            ++pos; // Skip over the doubled character
                            affix.append(ch); // append two: one here, one below
                            if ((pos + 1) < pattern.length() && pattern.charAt(pos + 1) == CURRENCY_SIGN) {
                                ++pos; // Skip over the tripled character
                                affix.append(ch); // append again
                                currencySignCount = CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT;
                            } else {
                                currencySignCount = CURRENCY_SIGN_COUNT_IN_ISO_FORMAT;
                            }
                        } else {
                            currencySignCount = CURRENCY_SIGN_COUNT_IN_SYMBOL_FORMAT;
                        }
                        // Fall through to append(ch)
                    } else if (ch == QUOTE) {
                        // A quote outside quotes indicates either the opening
                        // quote or two quotes, which is a quote literal. That is,
                        // we have the first quote in 'do' or o''clock.
                        if ((pos + 1) < pattern.length() && pattern.charAt(pos + 1) == QUOTE) {
                            ++pos;
                            affix.append(ch); // append two: one here, one below
                        } else {
                            subpart += 2; // open quote
                        }
                        // Fall through to append(ch)
                    } else if (ch == separator) {
                        // Don't allow separators in the prefix, and don't allow
                        // separators in the second pattern (part == 1).
                        if (subpart == 1 || part == 1) {
                            patternError("Unquoted special character '" + ch + '\'', pattern);
                        }
                        sub2Limit = pos++;
                        break PARTLOOP; // Go to next part
                    } else if (ch == percent || ch == perMill) {
                        // Next handle characters which are appended directly.
                        if (multpl != 1) {
                            patternError("Too many percent/permille characters", pattern);
                        }
                        multpl = (ch == percent) ? 100 : 1000;
                        // Convert to non-localized pattern
                        ch = (ch == percent) ? PATTERN_PERCENT : PATTERN_PER_MILLE;
                        // Fall through to append(ch)
                    } else if (ch == minus) {
                        // Convert to non-localized pattern
                        ch = PATTERN_MINUS;
                        // Fall through to append(ch)
                    } else if (ch == padEscape) {
                        if (padPos >= 0) {
                            patternError("Multiple pad specifiers", pattern);
                        }
                        if ((pos + 1) == pattern.length()) {
                            patternError("Invalid pad specifier", pattern);
                        }
                        padPos = pos++; // Advance past pad char
                        padChar = pattern.charAt(pos);
                        continue;
                    }
                    affix.append(ch);
                    break;
                case 3: // Prefix subpart, in quote
                case 4: // Suffix subpart, in quote
                    // A quote within quotes indicates either the closing
                    // quote or two quotes, which is a quote literal. That is,
                    // we have the second quote in 'do' or 'don''t'.
                    if (ch == QUOTE) {
                        if ((pos + 1) < pattern.length() && pattern.charAt(pos + 1) == QUOTE) {
                            ++pos;
                            affix.append(ch);
                        } else {
                            subpart -= 2; // close quote
                        }
                        // Fall through to append(ch)
                    }
                    // NOTE: In ICU 2.2 there was code here to parse quoted
                    // percent and permille characters _within quotes_ and give
                    // them special meaning. This is incorrect, since quoted
                    // characters are literals without special meaning.
                    affix.append(ch);
                    break;
                }
            }

            if (subpart == 3 || subpart == 4) {
                patternError("Unterminated quote", pattern);
            }

            if (sub0Limit == 0) {
                sub0Limit = pattern.length();
            }

            if (sub2Limit == 0) {
                sub2Limit = pattern.length();
            }

            /*
             * Handle patterns with no '0' pattern character. These patterns are legal, but must be recodified to make
             * sense. "##.###" -> "#0.###". ".###" -> ".0##".
             * 
             * We allow patterns of the form "####" to produce a zeroDigitCount of zero (got that?); although this seems
             * like it might make it possible for format() to produce empty strings, format() checks for this condition
             * and outputs a zero digit in this situation. Having a zeroDigitCount of zero yields a minimum integer
             * digits of zero, which allows proper round-trip patterns. We don't want "#" to become "#0" when
             * toPattern() is called (even though that's what it really is, semantically).
             */
            if (zeroDigitCount == 0 && sigDigitCount == 0 && digitLeftCount > 0 && decimalPos >= 0) {
                // Handle "###.###" and "###." and ".###"
                int n = decimalPos;
                if (n == 0)
                    ++n; // Handle ".###"
                digitRightCount = digitLeftCount - n;
                digitLeftCount = n - 1;
                zeroDigitCount = 1;
            }

            // Do syntax checking on the digits, decimal points, and quotes.
            if ((decimalPos < 0 && digitRightCount > 0 && sigDigitCount == 0)
                    || (decimalPos >= 0 && (sigDigitCount > 0 || decimalPos < digitLeftCount || decimalPos > (digitLeftCount + zeroDigitCount)))
                    || groupingCount == 0 || groupingCount2 == 0 || (sigDigitCount > 0 && zeroDigitCount > 0)
                    || subpart > 2) { // subpart > 2 == unmatched quote
                patternError("Malformed pattern", pattern);
            }

            // Make sure pad is at legal position before or after affix.
            if (padPos >= 0) {
                if (padPos == start) {
                    padPos = PAD_BEFORE_PREFIX;
                } else if (padPos + 2 == sub0Start) {
                    padPos = PAD_AFTER_PREFIX;
                } else if (padPos == sub0Limit) {
                    padPos = PAD_BEFORE_SUFFIX;
                } else if (padPos + 2 == sub2Limit) {
                    padPos = PAD_AFTER_SUFFIX;
                } else {
                    patternError("Illegal pad position", pattern);
                }
            }

            if (part == 0) {
                // Set negative affixes temporarily to match the positive
                // affixes. Fix this up later after processing both parts.
                /*
                 * Bug 4212072 To meet the need of expandAffix(String, StirngBuffer) [Richard/GCL]
                 */
                posPrefixPattern = negPrefixPattern = prefix.toString();
                posSuffixPattern = negSuffixPattern = suffix.toString();

                useExponentialNotation = (expDigits >= 0);
                if (useExponentialNotation) {
                    minExponentDigits = expDigits;
                    exponentSignAlwaysShown = expSignAlways;
                }
                int digitTotalCount = digitLeftCount + zeroDigitCount + digitRightCount;
                // The effectiveDecimalPos is the position the decimal is at or
                // would be at if there is no decimal. Note that if
                // decimalPos<0, then digitTotalCount == digitLeftCount +
                // zeroDigitCount.
                int effectiveDecimalPos = decimalPos >= 0 ? decimalPos : digitTotalCount;
                boolean useSigDig = (sigDigitCount > 0);
                setSignificantDigitsUsed(useSigDig);
                if (useSigDig) {
                    setMinimumSignificantDigits(sigDigitCount);
                    setMaximumSignificantDigits(sigDigitCount + digitRightCount);
                } else {
                    int minInt = effectiveDecimalPos - digitLeftCount;
                    setMinimumIntegerDigits(minInt);
                    /*
                     * Upper limit on integer and fraction digits for a Java double [Richard/GCL]
                     */
                    setMaximumIntegerDigits(useExponentialNotation ? digitLeftCount + minInt : DOUBLE_INTEGER_DIGITS);
                    setMaximumFractionDigits(decimalPos >= 0 ? (digitTotalCount - decimalPos) : 0);
                    setMinimumFractionDigits(decimalPos >= 0 ? (digitLeftCount + zeroDigitCount - decimalPos) : 0);
                }
                setGroupingUsed(groupingCount > 0);
                this.groupingSize = (groupingCount > 0) ? groupingCount : 0;
                this.groupingSize2 = (groupingCount2 > 0 && groupingCount2 != groupingCount) ? groupingCount2 : 0;
                this.multiplier = multpl;
                setDecimalSeparatorAlwaysShown(decimalPos == 0 || decimalPos == digitTotalCount);
                if (padPos >= 0) {
                    padPosition = padPos;
                    formatWidth = sub0Limit - sub0Start; // to be fixed up below
                    pad = padChar;
                } else {
                    formatWidth = 0;
                }
                if (incrementVal != 0) {
                    // BigDecimal scale cannot be negative (even though
                    // this makes perfect sense), so we need to handle this.
                    int scale = incrementPos - effectiveDecimalPos;
                    roundingIncrementICU = BigDecimal.valueOf(incrementVal, scale > 0 ? scale : 0);
                    if (scale < 0) {
                        roundingIncrementICU = roundingIncrementICU.movePointRight(-scale);
                    }
                    setRoundingDouble();
                    roundingMode = BigDecimal.ROUND_HALF_EVEN;
                } else {
                    setRoundingIncrement((BigDecimal) null);
                }
            } else {
                /*
                 * Bug 4212072 To meet the need of expandAffix(String, StirngBuffer) [Richard/GCL]
                 */
                negPrefixPattern = prefix.toString();
                negSuffixPattern = suffix.toString();
                gotNegative = true;
            }
        }

        /*
         * Bug 4140009 Process the empty pattern [Richard/GCL]
         */
        if (pattern.length() == 0) {
            posPrefixPattern = posSuffixPattern = "";
            setMinimumIntegerDigits(0);
            setMaximumIntegerDigits(DOUBLE_INTEGER_DIGITS);
            setMinimumFractionDigits(0);
            setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        }

        // If there was no negative pattern, or if the negative pattern is
        // identical to the positive pattern, then prepend the minus sign to the
        // positive pattern to form the negative pattern.
        /*
         * Bug 4212072 To meet the need of expandAffix(String, StirngBuffer) [Richard/GCL]
         */
        if (!gotNegative || (negPrefixPattern.equals(posPrefixPattern) && negSuffixPattern.equals(posSuffixPattern))) {
            negSuffixPattern = posSuffixPattern;
            negPrefixPattern = PATTERN_MINUS + posPrefixPattern;
        }
        setLocale(null, null);
        // save the pattern
        formatPattern = pattern;
        // initialize currencyPluralInfo if needed
        if (currencySignCount == CURRENCY_SIGN_COUNT_IN_PLURAL_FORMAT && currencyPluralInfo == null) {
            currencyPluralInfo = new CurrencyPluralInfo(symbols.getLocale());
        }
    }

    /**
     * Centralizes the setting of the roundingDouble and roundingDoubleReciprocal.
     */
    private void setRoundingDouble() {
        if (roundingIncrementICU == null) {
            roundingDouble = 0.0d;
            roundingDoubleReciprocal = 0.0d;
        } else {
            roundingDouble = roundingIncrementICU.doubleValue();
            setRoundingDoubleReciprocal(BigDecimal.ONE.divide(roundingIncrementICU, BigDecimal.ROUND_HALF_EVEN)
                    .doubleValue());
        }
    }

    private void patternError(String msg, String pattern) {
        throw new IllegalArgumentException(msg + " in pattern \"" + pattern + '"');
    }

    /*
     * Rewrite the following 4 "set" methods Upper limit on integer and fraction digits for a Java double [Richard/GCL]
     */
    /**
     * Sets the maximum number of digits allowed in the integer portion of a number. This override limits the integer
     * digit count to 309.
     * 
     * @see NumberFormat#setMaximumIntegerDigits
     * @stable ICU 2.0
     */
    public void setMaximumIntegerDigits(int newValue) {
        super.setMaximumIntegerDigits(Math.min(newValue, DOUBLE_INTEGER_DIGITS));
    }

    /**
     * Sets the minimum number of digits allowed in the integer portion of a number. This override limits the integer
     * digit count to 309.
     * 
     * @see NumberFormat#setMinimumIntegerDigits
     * @stable ICU 2.0
     */
    public void setMinimumIntegerDigits(int newValue) {
        super.setMinimumIntegerDigits(Math.min(newValue, DOUBLE_INTEGER_DIGITS));
    }

    /**
     * Returns the minimum number of significant digits that will be displayed. This value has no effect unless
     * areSignificantDigitsUsed() returns true.
     * 
     * @return the fewest significant digits that will be shown
     * @stable ICU 3.0
     */
    public int getMinimumSignificantDigits() {
        return minSignificantDigits;
    }

    /**
     * Returns the maximum number of significant digits that will be displayed. This value has no effect unless
     * areSignificantDigitsUsed() returns true.
     * 
     * @return the most significant digits that will be shown
     * @stable ICU 3.0
     */
    public int getMaximumSignificantDigits() {
        return maxSignificantDigits;
    }

    /**
     * Sets the minimum number of significant digits that will be displayed. If <code>min</code> is less than one then
     * it is set to one. If the maximum significant digits count is less than <code>min</code>, then it is set to
     * <code>min</code>. This value has no effect unless areSignificantDigitsUsed() returns true.
     * 
     * @param min
     *            the fewest significant digits to be shown
     * @stable ICU 3.0
     */
    public void setMinimumSignificantDigits(int min) {
        if (min < 1) {
            min = 1;
        }
        // pin max sig dig to >= min
        int max = Math.max(maxSignificantDigits, min);
        minSignificantDigits = min;
        maxSignificantDigits = max;
    }

    /**
     * Sets the maximum number of significant digits that will be displayed. If <code>max</code> is less than one then
     * it is set to one. If the minimum significant digits count is greater than <code>max</code>, then it is set to
     * <code>max</code>. This value has no effect unless areSignificantDigitsUsed() returns true.
     * 
     * @param max
     *            the most significant digits to be shown
     * @stable ICU 3.0
     */
    public void setMaximumSignificantDigits(int max) {
        if (max < 1) {
            max = 1;
        }
        // pin min sig dig to 1..max
        int min = Math.min(minSignificantDigits, max);
        minSignificantDigits = min;
        maxSignificantDigits = max;
    }

    /**
     * Returns true if significant digits are in use or false if integer and fraction digit counts are in use.
     * 
     * @return true if significant digits are in use
     * @stable ICU 3.0
     */
    public boolean areSignificantDigitsUsed() {
        return useSignificantDigits;
    }

    /**
     * Sets whether significant digits are in use, or integer and fraction digit counts are in use.
     * 
     * @param useSignificantDigits
     *            true to use significant digits, or false to use integer and fraction digit counts
     * @stable ICU 3.0
     */
    public void setSignificantDigitsUsed(boolean useSignificantDigits) {
        this.useSignificantDigits = useSignificantDigits;
    }

    /**
     * Sets the <tt>Currency</tt> object used to display currency amounts. This takes effect immediately, if this format
     * is a currency format. If this format is not a currency format, then the currency object is used if and when this
     * object becomes a currency format through the application of a new pattern.
     * 
     * @param theCurrency
     *            new currency object to use. Must not be null.
     * @stable ICU 2.2
     */
    public void setCurrency(Currency theCurrency) {
        // If we are a currency format, then modify our affixes to
        // encode the currency symbol for the given currency in our
        // locale, and adjust the decimal digits and rounding for the
        // given currency.

        super.setCurrency(theCurrency);
        if (theCurrency != null) {
            boolean[] isChoiceFormat = new boolean[1];
            String s = theCurrency.getName(symbols.getULocale(), Currency.SYMBOL_NAME, isChoiceFormat);
            symbols.setCurrencySymbol(s);
            symbols.setInternationalCurrencySymbol(theCurrency.getCurrencyCode());
        }

        if (currencySignCount > 0) {
            if (theCurrency != null) {
                setRoundingIncrement(theCurrency.getRoundingIncrement());
                int d = theCurrency.getDefaultFractionDigits();
                setMinimumFractionDigits(d);
                setMaximumFractionDigits(d);
            }
            expandAffixes(null);
        }
    }

    /**
     * Returns the currency in effect for this formatter. Subclasses should override this method as needed. Unlike
     * getCurrency(), this method should never return null.
     * 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected Currency getEffectiveCurrency() {
        Currency c = getCurrency();
        if (c == null) {
            c = Currency.getInstance(symbols.getInternationalCurrencySymbol());
        }
        return c;
    }

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a number. This override limits the fraction
     * digit count to 340.
     * 
     * @see NumberFormat#setMaximumFractionDigits
     * @stable ICU 2.0
     */
    public void setMaximumFractionDigits(int newValue) {
        super.setMaximumFractionDigits(Math.min(newValue, DOUBLE_FRACTION_DIGITS));
    }

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a number. This override limits the fraction
     * digit count to 340.
     * 
     * @see NumberFormat#setMinimumFractionDigits
     * @stable ICU 2.0
     */
    public void setMinimumFractionDigits(int newValue) {
        super.setMinimumFractionDigits(Math.min(newValue, DOUBLE_FRACTION_DIGITS));
    }

    /**
     * Sets whether {@link #parse(String, ParsePosition)} method returns BigDecimal. The default value is false.
     * 
     * @param value
     *            true if {@link #parse(String, ParsePosition)} method returns BigDecimal.
     * @stable ICU 3.6
     */
    public void setParseBigDecimal(boolean value) {
        parseBigDecimal = value;
    }

    /**
     * Returns whether {@link #parse(String, ParsePosition)} method returns BigDecimal.
     * 
     * @return true if {@link #parse(String, ParsePosition)} method returns BigDecimal.
     * @stable ICU 3.6
     */
    public boolean isParseBigDecimal() {
        return parseBigDecimal;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        // Doug, do we need this anymore?
        // if (roundingIncrementICU != null) {
        // roundingIncrement = roundingIncrementICU.toBigDecimal();
        // }

        // Ticket#6449
        // Format.Field instances are not serializable. When formatToCharacterIterator
        // is called, attributes (ArrayList) stores FieldPosition instances with
        // NumberFormat.Field. Because NumberFormat.Field is not serializable, we need
        // to clear the contents of the list when writeObject is called. We could remove
        // the field or make it transient, but it will break serialization compatibility.
        attributes.clear();

        stream.defaultWriteObject();
    }

    /**
     * First, read the default serializable fields from the stream. Then if <code>serialVersionOnStream</code> is less
     * than 1, indicating that the stream was written by JDK 1.1, initialize <code>useExponentialNotation</code> to
     * false, since it was not present in JDK 1.1. Finally, set serialVersionOnStream back to the maximum allowed value
     * so that default serialization will work properly if this object is streamed out again.
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        /*
         * Bug 4185761 validate fields [Richard/GCL]
         */
        // We only need to check the maximum counts because NumberFormat
        // .readObject has already ensured that the maximum is greater than the
        // minimum count.
        /*
         * Commented for compatibility with previous version, and reserved for further use if (getMaximumIntegerDigits()
         * > DOUBLE_INTEGER_DIGITS || getMaximumFractionDigits() > DOUBLE_FRACTION_DIGITS) { throw new
         * InvalidObjectException("Digit count out of range"); }
         */
        /*
         * Truncate the maximumIntegerDigits to DOUBLE_INTEGER_DIGITS and maximumFractionDigits to
         * DOUBLE_FRACTION_DIGITS
         */
        if (getMaximumIntegerDigits() > DOUBLE_INTEGER_DIGITS) {
            setMaximumIntegerDigits(DOUBLE_INTEGER_DIGITS);
        }
        if (getMaximumFractionDigits() > DOUBLE_FRACTION_DIGITS) {
            setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        }
        if (serialVersionOnStream < 2) {
            exponentSignAlwaysShown = false;
            setInternalRoundingIncrement(null);
            setRoundingDouble();
            roundingMode = BigDecimal.ROUND_HALF_EVEN;
            formatWidth = 0;
            pad = ' ';
            padPosition = PAD_BEFORE_PREFIX;
            if (serialVersionOnStream < 1) {
                // Didn't have exponential fields
                useExponentialNotation = false;
            }
        }
        if (serialVersionOnStream < 3) {
            // Versions prior to 3 do not store a currency object.
            // Create one to match the DecimalFormatSymbols object.
            setCurrencyForSymbols();
        }
        serialVersionOnStream = currentSerialVersion;
        digitList = new DigitList();

        if (roundingIncrement != null) {
            setInternalRoundingIncrement(new BigDecimal(roundingIncrement));
            setRoundingDouble();
        }
    }

    private void setInternalRoundingIncrement(BigDecimal value) {
        roundingIncrementICU = value;
        roundingIncrement = value == null ? null : value.toBigDecimal();
    }

    // ----------------------------------------------------------------------
    // INSTANCE VARIABLES
    // ----------------------------------------------------------------------

    private transient DigitList digitList = new DigitList();

    /**
     * The symbol used as a prefix when formatting positive numbers, e.g. "+".
     * 
     * @serial
     * @see #getPositivePrefix
     */
    private String positivePrefix = "";

    /**
     * The symbol used as a suffix when formatting positive numbers. This is often an empty string.
     * 
     * @serial
     * @see #getPositiveSuffix
     */
    private String positiveSuffix = "";

    /**
     * The symbol used as a prefix when formatting negative numbers, e.g. "-".
     * 
     * @serial
     * @see #getNegativePrefix
     */
    private String negativePrefix = "-";

    /**
     * The symbol used as a suffix when formatting negative numbers. This is often an empty string.
     * 
     * @serial
     * @see #getNegativeSuffix
     */
    private String negativeSuffix = "";

    /**
     * The prefix pattern for non-negative numbers. This variable corresponds to <code>positivePrefix</code>.
     * 
     * <p>
     * This pattern is expanded by the method <code>expandAffix()</code> to <code>positivePrefix</code> to update the
     * latter to reflect changes in <code>symbols</code>. If this variable is <code>null</code> then
     * <code>positivePrefix</code> is taken as a literal value that does not change when <code>symbols</code> changes.
     * This variable is always <code>null</code> for <code>DecimalFormat</code> objects older than stream version 2
     * restored from stream.
     * 
     * @serial
     */
    // [Richard/GCL]
    private String posPrefixPattern;

    /**
     * The suffix pattern for non-negative numbers. This variable corresponds to <code>positiveSuffix</code>. This
     * variable is analogous to <code>posPrefixPattern</code>; see that variable for further documentation.
     * 
     * @serial
     */
    // [Richard/GCL]
    private String posSuffixPattern;

    /**
     * The prefix pattern for negative numbers. This variable corresponds to <code>negativePrefix</code>. This variable
     * is analogous to <code>posPrefixPattern</code>; see that variable for further documentation.
     * 
     * @serial
     */
    // [Richard/GCL]
    private String negPrefixPattern;

    /**
     * The suffix pattern for negative numbers. This variable corresponds to <code>negativeSuffix</code>. This variable
     * is analogous to <code>posPrefixPattern</code>; see that variable for further documentation.
     * 
     * @serial
     */
    // [Richard/GCL]
    private String negSuffixPattern;

    /**
     * Formatter for ChoiceFormat-based currency names. If this field is not null, then delegate to it to format
     * currency symbols.
     * 
     * @since ICU 2.6
     */
    private ChoiceFormat currencyChoice;

    /**
     * The multiplier for use in percent, permill, etc.
     * 
     * @serial
     * @see #getMultiplier
     */
    private int multiplier = 1;

    /**
     * The number of digits between grouping separators in the integer portion of a number. Must be greater than 0 if
     * <code>NumberFormat.groupingUsed</code> is true.
     * 
     * @serial
     * @see #getGroupingSize
     * @see NumberFormat#isGroupingUsed
     */
    private byte groupingSize = 3; // invariant, > 0 if useThousands

    /**
     * The secondary grouping size. This is only used for Hindi numerals, which use a primary grouping of 3 and a
     * secondary grouping of 2, e.g., "12,34,567". If this value is less than 1, then secondary grouping is equal to the
     * primary grouping.
     * 
     */
    private byte groupingSize2 = 0;

    /**
     * If true, forces the decimal separator to always appear in a formatted number, even if the fractional part of the
     * number is zero.
     * 
     * @serial
     * @see #isDecimalSeparatorAlwaysShown
     */
    private boolean decimalSeparatorAlwaysShown = false;

    /**
     * The <code>DecimalFormatSymbols</code> object used by this format. It contains the symbols used to format numbers,
     * e.g. the grouping separator, decimal separator, and so on.
     * 
     * @serial
     * @see #setDecimalFormatSymbols
     * @see DecimalFormatSymbols
     */
    private DecimalFormatSymbols symbols = null; // LIU new DecimalFormatSymbols();

    /**
     * True to use significant digits rather than integer and fraction digit counts.
     * 
     * @serial
     * @since ICU 3.0
     */
    private boolean useSignificantDigits = false;

    /**
     * The minimum number of significant digits to show. Must be >= 1 and <= maxSignificantDigits. Ignored unless
     * useSignificantDigits == true.
     * 
     * @serial
     * @since ICU 3.0
     */
    private int minSignificantDigits = 1;

    /**
     * The maximum number of significant digits to show. Must be >= minSignficantDigits. Ignored unless
     * useSignificantDigits == true.
     * 
     * @serial
     * @since ICU 3.0
     */
    private int maxSignificantDigits = 6;

    /**
     * True to force the use of exponential (i.e. scientific) notation when formatting numbers.
     * <p>
     * Note that the JDK 1.2 public API provides no way to set this field, even though it is supported by the
     * implementation and the stream format. The intent is that this will be added to the API in the future.
     * 
     * @serial
     */
    private boolean useExponentialNotation; // Newly persistent in JDK 1.2

    /**
     * The minimum number of digits used to display the exponent when a number is formatted in exponential notation.
     * This field is ignored if <code>useExponentialNotation</code> is not true.
     * <p>
     * Note that the JDK 1.2 public API provides no way to set this field, even though it is supported by the
     * implementation and the stream format. The intent is that this will be added to the API in the future.
     * 
     * @serial
     */
    private byte minExponentDigits; // Newly persistent in JDK 1.2

    /**
     * If true, the exponent is always prefixed with either the plus sign or the minus sign. Otherwise, only negative
     * exponents are prefixed with the minus sign. This has no effect unless <code>useExponentialNotation</code> is
     * true.
     * 
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private boolean exponentSignAlwaysShown = false;

    /**
     * The value to which numbers are rounded during formatting. For example, if the rounding increment is 0.05, then
     * 13.371 would be formatted as 13.350, assuming 3 fraction digits. Has the value <code>null</code> if rounding is
     * not in effect, or a positive value if rounding is in effect. Default value <code>null</code>.
     * 
     * @serial
     * @since AlphaWorks NumberFormat
     */
    // Note: this is kept in sync with roundingIncrementICU.
    // it is only kept around to avoid a conversion when formatting a java.math.BigDecimal
    private java.math.BigDecimal roundingIncrement = null;

    /**
     * The value to which numbers are rounded during formatting. For example, if the rounding increment is 0.05, then
     * 13.371 would be formatted as 13.350, assuming 3 fraction digits. Has the value <code>null</code> if rounding is
     * not in effect, or a positive value if rounding is in effect. Default value <code>null</code>. WARNING: the
     * roundingIncrement value is the one serialized.
     * 
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private transient BigDecimal roundingIncrementICU = null;

    /**
     * The rounding increment as a double. If this value is <= 0, then no rounding is done. This value is
     * <code>roundingIncrementICU.doubleValue()</code>. Default value 0.0.
     */
    private transient double roundingDouble = 0.0;

    /**
     * If the roundingDouble is the reciprocal of an integer (the most common case!), this is set to be that integer.
     * Otherwise it is 0.0.
     */
    private transient double roundingDoubleReciprocal = 0.0;

    /**
     * The rounding mode. This value controls any rounding operations which occur when applying a rounding increment or
     * when reducing the number of fraction digits to satisfy a maximum fraction digits limit. The value may assume any
     * of the <code>BigDecimal</code> rounding mode values. Default value <code>BigDecimal.ROUND_HALF_EVEN</code>.
     * 
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private int roundingMode = BigDecimal.ROUND_HALF_EVEN;

    /**
     * Operations on <code>BigDecimal</code> numbers are controlled by a {@link MathContext} object, which provides the
     * context (precision and other information) for the operation. The default <code>MathContext</code> settings are
     * <code>digits=0, form=PLAIN, lostDigits=false, 
     * roundingMode=ROUND_HALF_UP</code>; these settings perform fixed point arithmetic with unlimited precision, as
     * defined for the original BigDecimal class in Java 1.1 and Java 1.2
     */
    private MathContext mathContext = new MathContext(0, MathContext.PLAIN); // context for plain unlimited math

    /**
     * The padded format width, or zero if there is no padding. Must be >= 0. Default value zero.
     * 
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private int formatWidth = 0;

    /**
     * The character used to pad the result of format to <code>formatWidth</code>, if padding is in effect. Default
     * value ' '.
     * 
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private char pad = ' ';

    /**
     * The position in the string at which the <code>pad</code> character will be inserted, if padding is in effect.
     * Must have a value from <code>PAD_BEFORE_PREFIX</code> to <code>PAD_AFTER_SUFFIX</code>. Default value
     * <code>PAD_BEFORE_PREFIX</code>.
     * 
     * @serial
     * @since AlphaWorks NumberFormat
     */
    private int padPosition = PAD_BEFORE_PREFIX;

    /**
     * True if {@link #parse(String, ParsePosition)} to return BigDecimal rather than Long, Double or BigDecimal except
     * special values. This property is introduced for J2SE 5 compatibility support.
     * 
     * @serial
     * @since ICU 3.6
     * @see #setParseBigDecimal(boolean)
     * @see #isParseBigDecimal()
     */
    private boolean parseBigDecimal = false;

    // ----------------------------------------------------------------------

    static final int currentSerialVersion = 3;

    /**
     * The internal serial version which says which version was written Possible values are:
     * <ul>
     * <li><b>0</b> (default): versions before JDK 1.2 <li><b>1</b>: version from JDK 1.2 and later, which includes the
     * two new fields <code>useExponentialNotation</code> and <code>minExponentDigits</code>. <li><b>2</b>: version on
     * AlphaWorks, which adds roundingMode, formatWidth, pad, padPosition, exponentSignAlwaysShown, roundingIncrement.
     * <li><b>3</b>: ICU 2.2. Adds currency object.
     * </ul>
     * 
     * @serial
     */
    private int serialVersionOnStream = currentSerialVersion;

    // ----------------------------------------------------------------------
    // CONSTANTS
    // ----------------------------------------------------------------------

    /**
     * Constant for <code>getPadPosition()</code> and <code>setPadPosition()</code> specifying pad characters inserted
     * before the prefix.
     * 
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @see #PAD_AFTER_SUFFIX
     * @stable ICU 2.0
     */
    public static final int PAD_BEFORE_PREFIX = 0;

    /**
     * Constant for <code>getPadPosition()</code> and <code>setPadPosition()</code> specifying pad characters inserted
     * after the prefix.
     * 
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @see #PAD_AFTER_SUFFIX
     * @stable ICU 2.0
     */
    public static final int PAD_AFTER_PREFIX = 1;

    /**
     * Constant for <code>getPadPosition()</code> and <code>setPadPosition()</code> specifying pad characters inserted
     * before the suffix.
     * 
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_AFTER_SUFFIX
     * @stable ICU 2.0
     */
    public static final int PAD_BEFORE_SUFFIX = 2;

    /**
     * Constant for <code>getPadPosition()</code> and <code>setPadPosition()</code> specifying pad characters inserted
     * after the suffix.
     * 
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @stable ICU 2.0
     */
    public static final int PAD_AFTER_SUFFIX = 3;

    // Constants for characters used in programmatic (unlocalized) patterns.
    private static final char PATTERN_ZERO_DIGIT = '0';
    private static final char PATTERN_GROUPING_SEPARATOR = ',';
    private static final char PATTERN_DECIMAL_SEPARATOR = '.';
    private static final char PATTERN_DIGIT = '#';
    static final char PATTERN_SIGNIFICANT_DIGIT = '@';
    static final char PATTERN_EXPONENT = 'E';
    static final char PATTERN_PLUS_SIGN = '+';

    // Affix
    private static final char PATTERN_PER_MILLE = '\u2030';
    private static final char PATTERN_PERCENT = '%';
    static final char PATTERN_PAD_ESCAPE = '*';
    /*
     * Bug 4212072 To meet the need of expandAffix(String, StirngBuffer) [Richard/GCL]
     */
    private static final char PATTERN_MINUS = '-';

    // Other
    private static final char PATTERN_SEPARATOR = ';';

    // Pad escape is package private to allow access by DecimalFormatSymbols.
    // Also plus sign. Also exponent.

    /**
     * The CURRENCY_SIGN is the standard Unicode symbol for currency. It is used in patterns and substitued with either
     * the currency symbol, or if it is doubled, with the international currency symbol. If the CURRENCY_SIGN is seen in
     * a pattern, then the decimal separator is replaced with the monetary decimal separator.
     * 
     * The CURRENCY_SIGN is not localized.
     */
    private static final char CURRENCY_SIGN = '\u00A4';

    private static final char QUOTE = '\'';

    /*
     * Upper limit on integer and fraction digits for a Java double [Richard/GCL]
     */
    static final int DOUBLE_INTEGER_DIGITS = 309;
    static final int DOUBLE_FRACTION_DIGITS = 340;

    /**
     * When someone turns on scientific mode, we assume that more than this number of digits is due to flipping from
     * some other mode that didn't restrict the maximum, and so we force 1 integer digit. We don't bother to track and
     * see if someone is using exponential notation with more than this number, it wouldn't make sense anyway, and this
     * is just to make sure that someone turning on scientific mode with default settings doesn't end up with lots of
     * zeroes.
     */
    static final int MAX_SCIENTIFIC_INTEGER_DIGITS = 8;

    // Proclaim JDK 1.1 serial compatibility.
    private static final long serialVersionUID = 864413376551465018L;

    private ArrayList<FieldPosition> attributes = new ArrayList<FieldPosition>();

    /*
     * Following are used in currency format
     */
    /*
     * // triple currency sign char array private static final char[] tripleCurrencySign = {0xA4, 0xA4, 0xA4}; // triple
     * currency sign string private static final String tripleCurrencyStr = new String(tripleCurrencySign);
     * 
     * // default currency plural pattern char array private static final char[] defaultCurrencyPluralPatternChar = {0,
     * '.', '#', '#', ' ', 0xA4, 0xA4, 0xA4}; // default currency plural pattern string private static final String
     * defaultCurrencyPluralPattern = new String(defaultCurrencyPluralPatternChar);
     */

    // pattern used in this formatter
    private String formatPattern = "";
    // style is only valid when decimal formatter is constructed by
    // DecimalFormat(pattern, decimalFormatSymbol, style)
    private int style = NumberFormat.NUMBERSTYLE;
    /*
     * Represents whether this is a currency format, and which currency format style. 0: not currency format type; 1:
     * currency style -- symbol name, such as "$" for US dollar. 2: currency style -- ISO name, such as USD for US
     * dollar. 3: currency style -- plural long name, such as "US Dollar" for "1.00 US Dollar", or "US Dollars" for
     * "3.00 US Dollars".
     */
    private int currencySignCount = 0;

    /*
     * For parsing purose, Need to remember all prefix patterns and suffix patterns of every currency format pattern,
     * including the pattern of default currecny style, ISO currency style, and plural currency style. And the patterns
     * are set through applyPattern. Following are used to represent the affix patterns in currency plural formats.
     */
    private static final class AffixForCurrency {
        // negative prefix pattern
        private String negPrefixPatternForCurrency = null;
        // negative suffix pattern
        private String negSuffixPatternForCurrency = null;
        // positive prefix pattern
        private String posPrefixPatternForCurrency = null;
        // positive suffix pattern
        private String posSuffixPatternForCurrency = null;
        private int patternType;

        public AffixForCurrency() {
            patternType = Currency.SYMBOL_NAME;
        }

        public AffixForCurrency(String negPrefix, String negSuffix, String posPrefix, String posSuffix, int type) {
            negPrefixPatternForCurrency = negPrefix;
            negSuffixPatternForCurrency = negSuffix;
            posPrefixPatternForCurrency = posPrefix;
            posSuffixPatternForCurrency = posSuffix;
            patternType = type;
        }

        public String getNegPrefix() {
            return negPrefixPatternForCurrency;
        }

        public String getNegSuffix() {
            return negSuffixPatternForCurrency;
        }

        public String getPosPrefix() {
            return posPrefixPatternForCurrency;
        }

        public String getPosSuffix() {
            return posSuffixPatternForCurrency;
        }

        public int getPatternType() {
            return patternType;
        }
    }

    // Affix patter set for currency.
    // It is a set of AffixForCurrency,
    // each element of the set saves the negative prefix,
    // negative suffix, positive prefix, and positive suffix of a pattern.
    private transient Set<AffixForCurrency> affixPatternsForCurrency = null;

    // For currency parsing, since currency parsing need to parse
    // against all currency patterns, before the parsing, need to set up
    // the affix patterns for currency.
    private transient boolean isReadyForParsing = false;

    // Information needed for DecimalFormat to format/parse currency plural.
    private CurrencyPluralInfo currencyPluralInfo = null;

}

// eof
