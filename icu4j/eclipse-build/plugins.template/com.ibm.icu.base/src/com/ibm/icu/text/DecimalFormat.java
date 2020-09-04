// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.math.BigInteger;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;

/**
 * {@icuenhanced java.text.DecimalFormat}.{@icu _usage_}
 *
 * <code>DecimalFormat</code> is a concrete subclass of {@link NumberFormat} that formats
 * decimal numbers. It has a variety of features designed to make it possible to parse and
 * format numbers in any locale, including support for Western, Arabic, or Indic digits.
 * It also supports different flavors of numbers, including integers ("123"), fixed-point
 * numbers ("123.4"), scientific notation ("1.23E4"), percentages ("12%"), and currency
 * amounts ("$123.00", "USD123.00", "123.00 US dollars").  All of these flavors can be
 * easily localized.
 *
 * <p>To obtain a {@link NumberFormat} for a specific locale (including the default
 * locale) call one of <code>NumberFormat</code>'s factory methods such as {@link
 * NumberFormat#getInstance}. Do not call the <code>DecimalFormat</code> constructors
 * directly, unless you know what you are doing, since the {@link NumberFormat} factory
 * methods may return subclasses other than <code>DecimalFormat</code>. If you need to
 * customize the format object, do something like this:
 *
 * <blockquote><pre>
 * NumberFormat f = NumberFormat.getInstance(loc);
 * if (f instanceof DecimalFormat) {
 *     ((DecimalFormat) f).setDecimalSeparatorAlwaysShown(true);
 * }</pre></blockquote>
 *
 * <p><strong>Example Usage</strong>
 *
 * Print out a number using the localized number, currency, and percent
 * format for each locale.
 *
 * <blockquote><pre>
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
 * <p>Another example use getInstance(style).<br/>
 * Print out a number using the localized number, currency, percent,
 * scientific, integer, iso currency, and plural currency format for each locale.
 *
 * <blockquote><pre>
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
 * <em>symbols</em>.  The pattern may be set directly using {@link #applyPattern}, or
 * indirectly using other API methods which manipulate aspects of the pattern, such as the
 * minimum number of integer digits.  The symbols are stored in a {@link
 * DecimalFormatSymbols} object.  When using the {@link NumberFormat} factory methods, the
 * pattern and symbols are read from ICU's locale data.
 *
 * <h4>Special Pattern Characters</h4>
 *
 * <p>Many characters in a pattern are taken literally; they are matched during parsing
 * and output unchanged during formatting.  Special characters, on the other hand, stand
 * for other characters, strings, or classes of characters.  For example, the '#'
 * character is replaced by a localized digit.  Often the replacement character is the
 * same as the pattern character; in the U.S. locale, the ',' grouping character is
 * replaced by ','.  However, the replacement is still happening, and if the symbols are
 * modified, the grouping character changes.  Some special characters affect the behavior
 * of the formatter by their presence; for example, if the percent character is seen, then
 * the value is multiplied by 100 before being displayed.
 *
 * <p>To insert a special character in a pattern as a literal, that is, without any
 * special meaning, the character must be quoted.  There are some exceptions to this which
 * are noted below.
 *
 * <p>The characters listed here are used in non-localized patterns.  Localized patterns
 * use the corresponding characters taken from this formatter's {@link
 * DecimalFormatSymbols} object instead, and these characters lose their special status.
 * Two exceptions are the currency sign and quote, which are not localized.
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
 * <p>A <code>DecimalFormat</code> pattern contains a postive and negative subpattern, for
 * example, "#,##0.00;(#,##0.00)".  Each subpattern has a prefix, a numeric part, and a
 * suffix.  If there is no explicit negative subpattern, the negative subpattern is the
 * localized minus sign prefixed to the positive subpattern. That is, "0.00" alone is
 * equivalent to "0.00;-0.00".  If there is an explicit negative subpattern, it serves
 * only to specify the negative prefix and suffix; the number of digits, minimal digits,
 * and other characteristics are ignored in the negative subpattern. That means that
 * "#,##0.0#;(#)" has precisely the same result as "#,##0.0#;(#,##0.0#)".
 *
 * <p>The prefixes, suffixes, and various symbols used for infinity, digits, thousands
 * separators, decimal separators, etc. may be set to arbitrary values, and they will
 * appear properly during formatting.  However, care must be taken that the symbols and
 * strings do not conflict, or parsing will be unreliable.  For example, either the
 * positive and negative prefixes or the suffixes must be distinct for {@link #parse} to
 * be able to distinguish positive from negative values.  Another example is that the
 * decimal separator and thousands separator should be distinct characters, or parsing
 * will be impossible.
 *
 * <p>The <em>grouping separator</em> is a character that separates clusters of integer
 * digits to make large numbers more legible.  It commonly used for thousands, but in some
 * locales it separates ten-thousands.  The <em>grouping size</em> is the number of digits
 * between the grouping separators, such as 3 for "100,000,000" or 4 for "1 0000
 * 0000". There are actually two different grouping sizes: One used for the least
 * significant integer digits, the <em>primary grouping size</em>, and one used for all
 * others, the <em>secondary grouping size</em>.  In most locales these are the same, but
 * sometimes they are different. For example, if the primary grouping interval is 3, and
 * the secondary is 2, then this corresponds to the pattern "#,##,##0", and the number
 * 123456789 is formatted as "12,34,56,789".  If a pattern contains multiple grouping
 * separators, the interval between the last one and the end of the integer defines the
 * primary grouping size, and the interval between the last two defines the secondary
 * grouping size. All others are ignored, so "#,##,###,####" == "###,###,####" ==
 * "##,#,###,####".
 *
 * <p>Illegal patterns, such as "#.#.#" or "#.###,###", will cause
 * <code>DecimalFormat</code> to throw an {@link IllegalArgumentException} with a message
 * that describes the problem.
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
 * <ul>
 *
 * <li>The grouping separator ',' can occur inside the integer and sigDigits
 * elements, between any two pattern characters of that element, as long as the integer or
 * sigDigits element is not followed by the exponent element.
 *
 * <li>Two grouping intervals are recognized: That between the decimal point and the first
 * grouping symbol, and that between the first and second grouping symbols. These
 * intervals are identical in most locales, but in some locales they differ. For example,
 * the pattern &quot;#,##,###&quot; formats the number 123456789 as
 * &quot;12,34,56,789&quot;.
 *
 * <li>The pad specifier <code>padSpec</code> may appear before the prefix, after the
 * prefix, before the suffix, after the suffix, or not at all.
 *
 * <li>In place of '0', the digits '1' through '9' may be used to indicate a rounding
 * increment.
 *
 * </ul>
 *
 * <h4>Parsing</h4>
 *
 * <p><code>DecimalFormat</code> parses all Unicode characters that represent decimal
 * digits, as defined by {@link UCharacter#digit}.  In addition,
 * <code>DecimalFormat</code> also recognizes as digits the ten consecutive characters
 * starting with the localized zero digit defined in the {@link DecimalFormatSymbols}
 * object.  During formatting, the {@link DecimalFormatSymbols}-based digits are output.
 *
 * <p>During parsing, grouping separators are ignored.
 *
 * <p>For currency parsing, the formatter is able to parse every currency style formats no
 * matter which style the formatter is constructed with.  For example, a formatter
 * instance gotten from NumberFormat.getInstance(ULocale, NumberFormat.CURRENCYSTYLE) can
 * parse formats such as "USD1.00" and "3.00 US dollars".
 *
 * <p>If {@link #parse(String, ParsePosition)} fails to parse a string, it returns
 * <code>null</code> and leaves the parse position unchanged.  The convenience method
 * {@link #parse(String)} indicates parse failure by throwing a {@link
 * java.text.ParseException}.
 *
 * <h4>Formatting</h4>
 *
 * <p>Formatting is guided by several parameters, all of which can be specified either
 * using a pattern or using the API.  The following description applies to formats that do
 * not use <a href="#sci">scientific notation</a> or <a href="#sigdig">significant
 * digits</a>.
 *
 * <ul><li>If the number of actual integer digits exceeds the <em>maximum integer
 * digits</em>, then only the least significant digits are shown.  For example, 1997 is
 * formatted as "97" if the maximum integer digits is set to 2.
 *
 * <li>If the number of actual integer digits is less than the <em>minimum integer
 * digits</em>, then leading zeros are added.  For example, 1997 is formatted as "01997"
 * if the minimum integer digits is set to 5.
 *
 * <li>If the number of actual fraction digits exceeds the <em>maximum fraction
 * digits</em>, then half-even rounding it performed to the maximum fraction digits.  For
 * example, 0.125 is formatted as "0.12" if the maximum fraction digits is 2.  This
 * behavior can be changed by specifying a rounding increment and a rounding mode.
 *
 * <li>If the number of actual fraction digits is less than the <em>minimum fraction
 * digits</em>, then trailing zeros are added.  For example, 0.125 is formatted as
 * "0.1250" if the mimimum fraction digits is set to 4.
 *
 * <li>Trailing fractional zeros are not displayed if they occur <em>j</em> positions
 * after the decimal, where <em>j</em> is less than the maximum fraction digits. For
 * example, 0.10004 is formatted as "0.1" if the maximum fraction digits is four or less.
 * </ul>
 *
 * <p><strong>Special Values</strong>
 *
 * <p><code>NaN</code> is represented as a single character, typically
 * <code>&#92;uFFFD</code>.  This character is determined by the {@link
 * DecimalFormatSymbols} object.  This is the only value for which the prefixes and
 * suffixes are not used.
 *
 * <p>Infinity is represented as a single character, typically <code>&#92;u221E</code>,
 * with the positive or negative prefixes and suffixes applied.  The infinity character is
 * determined by the {@link DecimalFormatSymbols} object.
 *
 * <a name="sci"><h4>Scientific Notation</h4></a>
 *
 * <p>Numbers in scientific notation are expressed as the product of a mantissa and a
 * power of ten, for example, 1234 can be expressed as 1.234 x 10<sup>3</sup>. The
 * mantissa is typically in the half-open interval [1.0, 10.0) or sometimes [0.0, 1.0),
 * but it need not be.  <code>DecimalFormat</code> supports arbitrary mantissas.
 * <code>DecimalFormat</code> can be instructed to use scientific notation through the API
 * or through the pattern.  In a pattern, the exponent character immediately followed by
 * one or more digit characters indicates scientific notation.  Example: "0.###E0" formats
 * the number 1234 as "1.234E3".
 *
 * <ul>
 *
 * <li>The number of digit characters after the exponent character gives the minimum
 * exponent digit count.  There is no maximum.  Negative exponents are formatted using the
 * localized minus sign, <em>not</em> the prefix and suffix from the pattern.  This allows
 * patterns such as "0.###E0 m/s".  To prefix positive exponents with a localized plus
 * sign, specify '+' between the exponent and the digits: "0.###E+0" will produce formats
 * "1E+1", "1E+0", "1E-1", etc.  (In localized patterns, use the localized plus sign
 * rather than '+'.)
 *
 * <li>The minimum number of integer digits is achieved by adjusting the exponent.
 * Example: 0.00123 formatted with "00.###E0" yields "12.3E-4".  This only happens if
 * there is no maximum number of integer digits.  If there is a maximum, then the minimum
 * number of integer digits is fixed at one.
 *
 * <li>The maximum number of integer digits, if present, specifies the exponent grouping.
 * The most common use of this is to generate <em>engineering notation</em>, in which the
 * exponent is a multiple of three, e.g., "##0.###E0".  The number 12345 is formatted
 * using "##0.####E0" as "12.345E3".
 *
 * <li>When using scientific notation, the formatter controls the digit counts using
 * significant digits logic.  The maximum number of significant digits limits the total
 * number of integer and fraction digits that will be shown in the mantissa; it does not
 * affect parsing.  For example, 12345 formatted with "##0.##E0" is "12.3E3".  See the
 * section on significant digits for more details.
 *
 * <li>The number of significant digits shown is determined as follows: If
 * areSignificantDigitsUsed() returns false, then the minimum number of significant digits
 * shown is one, and the maximum number of significant digits shown is the sum of the
 * <em>minimum integer</em> and <em>maximum fraction</em> digits, and is unaffected by the
 * maximum integer digits.  If this sum is zero, then all significant digits are shown.
 * If areSignificantDigitsUsed() returns true, then the significant digit counts are
 * specified by getMinimumSignificantDigits() and getMaximumSignificantDigits().  In this
 * case, the number of integer digits is fixed at one, and there is no exponent grouping.
 *
 * <li>Exponential patterns may not contain grouping separators.
 *
 * </ul>
 *
 * <a name="sigdig"><h4>Significant Digits</h4></a>
 *
 * <code>DecimalFormat</code> has two ways of controlling how many digits are shows: (a)
 * significant digits counts, or (b) integer and fraction digit counts.  Integer and
 * fraction digit counts are described above.  When a formatter is using significant
 * digits counts, the number of integer and fraction digits is not specified directly, and
 * the formatter settings for these counts are ignored.  Instead, the formatter uses
 * however many integer and fraction digits are required to display the specified number
 * of significant digits.  Examples:
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
 *
 * <li>Significant digit counts may be expressed using patterns that specify a minimum and
 * maximum number of significant digits.  These are indicated by the <code>'@'</code> and
 * <code>'#'</code> characters.  The minimum number of significant digits is the number of
 * <code>'@'</code> characters.  The maximum number of significant digits is the number of
 * <code>'@'</code> characters plus the number of <code>'#'</code> characters following on
 * the right.  For example, the pattern <code>"@@@"</code> indicates exactly 3 significant
 * digits.  The pattern <code>"@##"</code> indicates from 1 to 3 significant digits.
 * Trailing zero digits to the right of the decimal separator are suppressed after the
 * minimum number of significant digits have been shown.  For example, the pattern
 * <code>"@##"</code> formats the number 0.1203 as <code>"0.12"</code>.
 *
 * <li>If a pattern uses significant digits, it may not contain a decimal separator, nor
 * the <code>'0'</code> pattern character.  Patterns such as <code>"@00"</code> or
 * <code>"@.###"</code> are disallowed.
 *
 * <li>Any number of <code>'#'</code> characters may be prepended to the left of the
 * leftmost <code>'@'</code> character.  These have no effect on the minimum and maximum
 * significant digits counts, but may be used to position grouping separators.  For
 * example, <code>"#,#@#"</code> indicates a minimum of one significant digits, a maximum
 * of two significant digits, and a grouping size of three.
 *
 * <li>In order to enable significant digits formatting, use a pattern containing the
 * <code>'@'</code> pattern character.  Alternatively, call {@link
 * #setSignificantDigitsUsed setSignificantDigitsUsed(true)}.
 *
 * <li>In order to disable significant digits formatting, use a pattern that does not
 * contain the <code>'@'</code> pattern character. Alternatively, call {@link
 * #setSignificantDigitsUsed setSignificantDigitsUsed(false)}.
 *
 * <li>The number of significant digits has no effect on parsing.
 *
 * <li>Significant digits may be used together with exponential notation. Such patterns
 * are equivalent to a normal exponential pattern with a minimum and maximum integer digit
 * count of one, a minimum fraction digit count of <code>getMinimumSignificantDigits() -
 * 1</code>, and a maximum fraction digit count of <code>getMaximumSignificantDigits() -
 * 1</code>. For example, the pattern <code>"@@###E0"</code> is equivalent to
 * <code>"0.0###E0"</code>.
 *
 * <li>If signficant digits are in use, then the integer and fraction digit counts, as set
 * via the API, are ignored.  If significant digits are not in use, then the signficant
 * digit counts, as set via the API, are ignored.
 *
 * </ul>
 *
 * <h4>Padding</h4>
 *
 * <p><code>DecimalFormat</code> supports padding the result of {@link #format} to a
 * specific width.  Padding may be specified either through the API or through the pattern
 * syntax.  In a pattern the pad escape character, followed by a single pad character,
 * causes padding to be parsed and formatted.  The pad escape character is '*' in
 * unlocalized patterns, and can be localized using {@link
 * DecimalFormatSymbols#setPadEscape}.  For example, <code>"$*x#,##0.00"</code> formats
 * 123 to <code>"$xx123.00"</code>, and 1234 to <code>"$1,234.00"</code>.
 *
 * <ul>
 *
 * <li>When padding is in effect, the width of the positive subpattern, including prefix
 * and suffix, determines the format width.  For example, in the pattern <code>"* #0
 * o''clock"</code>, the format width is 10.
 *
 * <li>The width is counted in 16-bit code units (Java <code>char</code>s).
 *
 * <li>Some parameters which usually do not matter have meaning when padding is used,
 * because the pattern width is significant with padding.  In the pattern "*
 * ##,##,#,##0.##", the format width is 14.  The initial characters "##,##," do not affect
 * the grouping size or maximum integer digits, but they do affect the format width.
 *
 * <li>Padding may be inserted at one of four locations: before the prefix, after the
 * prefix, before the suffix, or after the suffix.  If padding is specified in any other
 * location, {@link #applyPattern} throws an {@link IllegalArgumentException}.  If there
 * is no prefix, before the prefix and after the prefix are equivalent, likewise for the
 * suffix.
 *
 * <li>When specified in a pattern, the 16-bit <code>char</code> immediately following the
 * pad escape is the pad character. This may be any character, including a special pattern
 * character. That is, the pad escape <em>escapes</em> the following character. If there
 * is no character after the pad escape, then the pattern is illegal.
 *
 * </ul>
 *
 * <p>
 * <strong>Rounding</strong>
 *
 * <p><code>DecimalFormat</code> supports rounding to a specific increment.  For example,
 * 1230 rounded to the nearest 50 is 1250.  1.234 rounded to the nearest 0.65 is 1.3.  The
 * rounding increment may be specified through the API or in a pattern.  To specify a
 * rounding increment in a pattern, include the increment in the pattern itself.  "#,#50"
 * specifies a rounding increment of 50.  "#,##0.05" specifies a rounding increment of
 * 0.05.
 *
 * <ul>
 *
 * <li>Rounding only affects the string produced by formatting.  It does not affect
 * parsing or change any numerical values.
 *
 * <li>A <em>rounding mode</em> determines how values are rounded; see the {@link
 * com.ibm.icu.math.BigDecimal} documentation for a description of the modes.  Rounding
 * increments specified in patterns use the default mode, {@link
 * com.ibm.icu.math.BigDecimal#ROUND_HALF_EVEN}.
 *
 * <li>Some locales use rounding in their currency formats to reflect the smallest
 * currency denomination.
 *
 * <li>In a pattern, digits '1' through '9' specify rounding, but otherwise behave
 * identically to digit '0'.
 *
 * </ul>
 *
 * <h4>Synchronization</h4>
 *
 * <p><code>DecimalFormat</code> objects are not synchronized.  Multiple threads should
 * not access one formatter concurrently.
 *
 * @see          java.text.Format
 * @see          NumberFormat
 * @author       Mark Davis
 * @author       Alan Liu
 * @stable ICU 2.0
 */
public class DecimalFormat extends NumberFormat {

    private static final long serialVersionUID = 1L;
    /**
     * @internal
     * @param delegate the NumberFormat to which to delegate
     */
    public DecimalFormat(java.text.DecimalFormat delegate) {
        super(delegate);
    }

    /**
     * Creates a DecimalFormat using the default pattern and symbols for the default
     * locale. This is a convenient way to obtain a DecimalFormat when
     * internationalization is not the main concern.
     *
     * <p>To obtain standard formats for a given locale, use the factory methods on
     * NumberFormat such as getNumberInstance.  These factories will return the most
     * appropriate sub-class of NumberFormat for a given locale.
     *
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     * @stable ICU 2.0
     */
    public DecimalFormat() {
        // There is no way to construct java.text.DecimalFormat with an
        // explicit Locale.
        this(new java.text.DecimalFormat());

        if (!ULocale.getDefault(Category.FORMAT).toLocale().equals(Locale.getDefault())) {
            // On Java 6 or older JRE, ULocale's FORMAT default might be different
            // from the locale used for constructing java.text.DecimalFormat
            java.text.NumberFormat jdkNfmt = java.text.NumberFormat.getInstance(ULocale.getDefault(Category.FORMAT).toLocale());
            if (jdkNfmt instanceof java.text.DecimalFormat) {
                ((java.text.DecimalFormat)numberFormat).applyPattern(((java.text.DecimalFormat)jdkNfmt).toPattern());
                ((java.text.DecimalFormat)numberFormat).setDecimalFormatSymbols(((java.text.DecimalFormat)jdkNfmt).getDecimalFormatSymbols());
            }
        }
    }

    /**
     * Creates a DecimalFormat from the given pattern and the symbols for the default
     * locale. This is a convenient way to obtain a DecimalFormat when
     * internationalization is not the main concern.
     *
     * <p>To obtain standard formats for a given locale, use the factory methods on
     * NumberFormat such as getNumberInstance.  These factories will return the most
     * appropriate sub-class of NumberFormat for a given locale.
     *
     * @param pattern A non-localized pattern string.
     * @throws IllegalArgumentException if the given pattern is invalid.
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     * @stable ICU 2.0
     */
    public DecimalFormat(String pattern) {
        this(new java.text.DecimalFormat(
                pattern,
                new java.text.DecimalFormatSymbols(ULocale.getDefault(Category.FORMAT).toLocale())));
    }

    /**
     * Creates a DecimalFormat from the given pattern and symbols. Use this constructor
     * when you need to completely customize the behavior of the format.
     *
     * <p>To obtain standard formats for a given locale, use the factory methods on
     * NumberFormat such as getInstance or getCurrencyInstance. If you need only minor
     * adjustments to a standard format, you can modify the format returned by a
     * NumberFormat factory method.
     *
     * @param pattern a non-localized pattern string
     * @param symbols the set of symbols to be used
     * @exception IllegalArgumentException if the given pattern is invalid
     * @see NumberFormat#getInstance
     * @see NumberFormat#getNumberInstance
     * @see NumberFormat#getCurrencyInstance
     * @see NumberFormat#getPercentInstance
     * @see DecimalFormatSymbols
     * @stable ICU 2.0
     */
    public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
        this(new java.text.DecimalFormat(pattern, symbols.dfs));
    }

//    /**
//     * Creates a DecimalFormat from the given pattern, symbols, information used for
//     * currency plural format, and format style. Use this constructor when you need to
//     * completely customize the behavior of the format.
//     *
//     * <p>To obtain standard formats for a given locale, use the factory methods on
//     * NumberFormat such as getInstance or getCurrencyInstance.
//     *
//     * <p>If you need only minor adjustments to a standard format, you can modify the
//     * format returned by a NumberFormat factory method using the setters.
//     *
//     * <p>If you want to completely customize a decimal format, using your own
//     * DecimalFormatSymbols (such as group separators) and your own information for
//     * currency plural formatting (such as plural rule and currency plural patterns), you
//     * can use this constructor.
//     *
//     * @param pattern a non-localized pattern string
//     * @param symbols the set of symbols to be used
//     * @param infoInput the information used for currency plural format, including
//     * currency plural patterns and plural rules.
//     * @param style the decimal formatting style, it is one of the following values:
//     * NumberFormat.NUMBERSTYLE; NumberFormat.CURRENCYSTYLE; NumberFormat.PERCENTSTYLE;
//     * NumberFormat.SCIENTIFICSTYLE; NumberFormat.INTEGERSTYLE;
//     * NumberFormat.ISOCURRENCYSTYLE; NumberFormat.PLURALCURRENCYSTYLE;
//     * @stable ICU 4.2
//     */
//    public DecimalFormat(String pattern, DecimalFormatSymbols symbols, CurrencyPluralInfo infoInput,
//                         int style) {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

    /**
     * {@inheritDoc}
     * @stable ICU 2.0
     */
    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        return super.format(number, result, fieldPosition);
    }

    /**
     * @stable ICU 2.0
     */
    // [Spark/CDL] Delegate to format_long_StringBuffer_FieldPosition_boolean
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        return super.format(number, result, fieldPosition);
    }

    /**
     * Formats a BigInteger number.
     *
     * @stable ICU 2.0
     */
    public StringBuffer format(BigInteger number, StringBuffer result,
                               FieldPosition fieldPosition) {
        return super.format(number, result, fieldPosition);
    }

    /**
     * Formats a BigDecimal number.
     *
     * @stable ICU 2.0
     */
    public StringBuffer format(java.math.BigDecimal number, StringBuffer result,
                               FieldPosition fieldPosition) {
        return super.format(number, result, fieldPosition);
    }

    /**
     * Formats a BigDecimal number.
     *
     * @stable ICU 2.0
     */
    public StringBuffer format(BigDecimal number, StringBuffer result,
                               FieldPosition fieldPosition) {
        return super.format(number, result, fieldPosition);
    }

    /**
     * Parses the given string, returning a <code>Number</code> object to represent the
     * parsed value. <code>Double</code> objects are returned to represent non-integral
     * values which cannot be stored in a <code>BigDecimal</code>. These are
     * <code>NaN</code>, infinity, -infinity, and -0.0. If {@link #isParseBigDecimal()} is
     * false (the default), all other values are returned as <code>Long</code>,
     * <code>BigInteger</code>, or <code>BigDecimal</code> values, in that order of
     * preference. If {@link #isParseBigDecimal()} is true, all other values are returned
     * as <code>BigDecimal</code> valuse. If the parse fails, null is returned.
     *
     * @param text the string to be parsed
     * @param parsePosition defines the position where parsing is to begin, and upon
     * return, the position where parsing left off. If the position has not changed upon
     * return, then parsing failed.
     * @return a <code>Number</code> object with the parsed value or
     * <code>null</code> if the parse failed
     * @stable ICU 2.0
     */
    public Number parse(String text, ParsePosition parsePosition) {
        return super.parse(text, parsePosition);
    }

//    /**
//     * Parses text from the given string as a CurrencyAmount. Unlike the parse() method,
//     * this method will attempt to parse a generic currency name, searching for a match of
//     * this object's locale's currency display names, or for a 3-letter ISO currency
//     * code. This method will fail if this format is not a currency format, that is, if it
//     * does not contain the currency pattern symbol (U+00A4) in its prefix or suffix.
//     *
//     * @param text the string to parse
//     * @param pos input-output position; on input, the position within text to match; must
//     *  have 0 <= pos.getIndex() < text.length(); on output, the position after the last
//     *  matched character. If the parse fails, the position in unchanged upon output.
//     * @return a CurrencyAmount, or null upon failure
//     */
//    CurrencyAmount parseCurrency(String text, ParsePosition pos) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns a copy of the decimal format symbols used by this format.
     *
     * @return desired DecimalFormatSymbols
     * @see DecimalFormatSymbols
     * @stable ICU 2.0
     */
    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return new DecimalFormatSymbols(((java.text.DecimalFormat)numberFormat).getDecimalFormatSymbols());
    }

    /**
     * Sets the decimal format symbols used by this format. The format uses a copy of the
     * provided symbols.
     *
     * @param newSymbols desired DecimalFormatSymbols
     * @see DecimalFormatSymbols
     * @stable ICU 2.0
     */
    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        ((java.text.DecimalFormat)numberFormat).setDecimalFormatSymbols(newSymbols.dfs);
    }

    /**
     * Returns the positive prefix.
     *
     * <p>Examples: +123, $123, sFr123
     * @return the prefix
     * @stable ICU 2.0
     */
    public String getPositivePrefix() {
        return ((java.text.DecimalFormat)numberFormat).getPositivePrefix();
    }

    /**
     * Sets the positive prefix.
     *
     * <p>Examples: +123, $123, sFr123
     * @param newValue the prefix
     * @stable ICU 2.0
     */
    public void setPositivePrefix(String newValue) {
        ((java.text.DecimalFormat)numberFormat).setPositivePrefix(newValue);
    }

    /**
     * Returns the negative prefix.
     *
     * <p>Examples: -123, ($123) (with negative suffix), sFr-123
     *
     * @return the prefix
     * @stable ICU 2.0
     */
    public String getNegativePrefix() {
        return ((java.text.DecimalFormat)numberFormat).getNegativePrefix();
    }

    /**
     * Sets the negative prefix.
     *
     * <p>Examples: -123, ($123) (with negative suffix), sFr-123
     * @param newValue the prefix
     * @stable ICU 2.0
     */
    public void setNegativePrefix(String newValue) {
        ((java.text.DecimalFormat)numberFormat).setNegativePrefix(newValue);
    }

    /**
     * Returns the positive suffix.
     *
     * <p>Example: 123%
     *
     * @return the suffix
     * @stable ICU 2.0
     */
    public String getPositiveSuffix() {
        return ((java.text.DecimalFormat)numberFormat).getPositiveSuffix();
    }

    /**
     * Sets the positive suffix.
     *
     * <p>Example: 123%
     * @param newValue the suffix
     * @stable ICU 2.0
     */
    public void setPositiveSuffix(String newValue) {
        ((java.text.DecimalFormat)numberFormat).setPositiveSuffix(newValue);
    }

    /**
     * Returns the negative suffix.
     *
     * <p>Examples: -123%, ($123) (with positive suffixes)
     *
     * @return the suffix
     * @stable ICU 2.0
     */
    public String getNegativeSuffix() {
        return ((java.text.DecimalFormat)numberFormat).getNegativeSuffix();
    }

    /**
     * Sets the positive suffix.
     *
     * <p>Examples: 123%
     * @param newValue the suffix
     * @stable ICU 2.0
     */
    public void setNegativeSuffix(String newValue) {
        ((java.text.DecimalFormat)numberFormat).setNegativeSuffix(newValue);
    }

    /**
     * Returns the multiplier for use in percent, permill, etc. For a percentage, set the
     * suffixes to have "%" and the multiplier to be 100. (For Arabic, use arabic percent
     * symbol). For a permill, set the suffixes to have "\u2031" and the multiplier to be
     * 1000.
     *
     * <p>Examples: with 100, 1.23 -> "123", and "123" -> 1.23
     *
     * @return the multiplier
     * @stable ICU 2.0
     */
    public int getMultiplier() {
        return ((java.text.DecimalFormat)numberFormat).getMultiplier();
    }

    /**
     * Sets the multiplier for use in percent, permill, etc. For a percentage, set the
     * suffixes to have "%" and the multiplier to be 100. (For Arabic, use arabic percent
     * symbol). For a permill, set the suffixes to have "\u2031" and the multiplier to be
     * 1000.
     *
     * <p>Examples: with 100, 1.23 -> "123", and "123" -> 1.23
     *
     * @param newValue the multiplier
     * @stable ICU 2.0
     */
    public void setMultiplier(int newValue) {
        ((java.text.DecimalFormat)numberFormat).setMultiplier(newValue);
    }

//    /**
//     * {@icu} Returns the rounding increment.
//     *
//     * @return A positive rounding increment, or <code>null</code> if rounding is not in
//     * effect.
//     * @see #setRoundingIncrement
//     * @see #getRoundingMode
//     * @see #setRoundingMode
//     * @stable ICU 2.0
//     */
//    public BigDecimal getRoundingIncrement() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the rounding increment. This method also controls whether rounding is
//     * enabled.
//     *
//     * @param newValue A positive rounding increment, or <code>null</code> or
//     * <code>BigDecimal(0.0)</code> to disable rounding.
//     * @throws IllegalArgumentException if <code>newValue</code> is < 0.0
//     * @see #getRoundingIncrement
//     * @see #getRoundingMode
//     * @see #setRoundingMode
//     * @stable ICU 2.0
//     */
//    public void setRoundingIncrement(java.math.BigDecimal newValue) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the rounding increment. This method also controls whether rounding is
//     * enabled.
//     *
//     * @param newValue A positive rounding increment, or <code>null</code> or
//     * <code>BigDecimal(0.0)</code> to disable rounding.
//     * @throws IllegalArgumentException if <code>newValue</code> is < 0.0
//     * @see #getRoundingIncrement
//     * @see #getRoundingMode
//     * @see #setRoundingMode
//     * @stable ICU 3.6
//     */
//    public void setRoundingIncrement(BigDecimal newValue) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the rounding increment. This method also controls whether rounding is
//     * enabled.
//     *
//     * @param newValue A positive rounding increment, or 0.0 to disable rounding.
//     * @throws IllegalArgumentException if <code>newValue</code> is < 0.0
//     * @see #getRoundingIncrement
//     * @see #getRoundingMode
//     * @see #setRoundingMode
//     * @stable ICU 2.0
//     */
//    public void setRoundingIncrement(double newValue) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the rounding mode.
//     *
//     * @return A rounding mode, between <code>BigDecimal.ROUND_UP</code> and
//     * <code>BigDecimal.ROUND_UNNECESSARY</code>.
//     * @see #setRoundingIncrement
//     * @see #getRoundingIncrement
//     * @see #setRoundingMode
//     * @see java.math.BigDecimal
//     * @stable ICU 2.0
//     */
//    public int getRoundingMode() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Sets the rounding mode. This has no effect unless the rounding increment is greater
//     * than zero.
//     *
//     * @param roundingMode A rounding mode, between <code>BigDecimal.ROUND_UP</code> and
//     * <code>BigDecimal.ROUND_UNNECESSARY</code>.
//     * @exception IllegalArgumentException if <code>roundingMode</code> is unrecognized.
//     * @see #setRoundingIncrement
//     * @see #getRoundingIncrement
//     * @see #getRoundingMode
//     * @see java.math.BigDecimal
//     * @stable ICU 2.0
//     */
//    public void setRoundingMode(int roundingMode) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the width to which the output of <code>format()</code> is padded. The width is
//     * counted in 16-bit code units.
//     *
//     * @return the format width, or zero if no padding is in effect
//     * @see #setFormatWidth
//     * @see #getPadCharacter
//     * @see #setPadCharacter
//     * @see #getPadPosition
//     * @see #setPadPosition
//     * @stable ICU 2.0
//     */
//    public int getFormatWidth() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Sets the width to which the output of <code>format()</code> is
//     * padded. The width is counted in 16-bit code units.  This method
//     * also controls whether padding is enabled.
//     *
//     * @param width the width to which to pad the result of
//     * <code>format()</code>, or zero to disable padding
//     * @exception IllegalArgumentException if <code>width</code> is < 0
//     * @see #getFormatWidth
//     * @see #getPadCharacter
//     * @see #setPadCharacter
//     * @see #getPadPosition
//     * @see #setPadPosition
//     * @stable ICU 2.0
//     */
//    public void setFormatWidth(int width) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the character used to pad to the format width. The default is ' '.
//     *
//     * @return the pad character
//     * @see #setFormatWidth
//     * @see #getFormatWidth
//     * @see #setPadCharacter
//     * @see #getPadPosition
//     * @see #setPadPosition
//     * @stable ICU 2.0
//     */
//    public char getPadCharacter() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the character used to pad to the format width. If padding is not
//     * enabled, then this will take effect if padding is later enabled.
//     *
//     * @param padChar the pad character
//     * @see #setFormatWidth
//     * @see #getFormatWidth
//     * @see #getPadCharacter
//     * @see #getPadPosition
//     * @see #setPadPosition
//     * @stable ICU 2.0
//     */
//    public void setPadCharacter(char padChar) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the position at which padding will take place. This is the location at
//     * which padding will be inserted if the result of <code>format()</code> is shorter
//     * than the format width.
//     *
//     * @return the pad position, one of <code>PAD_BEFORE_PREFIX</code>,
//     *         <code>PAD_AFTER_PREFIX</code>, <code>PAD_BEFORE_SUFFIX</code>, or
//     *         <code>PAD_AFTER_SUFFIX</code>.
//     * @see #setFormatWidth
//     * @see #getFormatWidth
//     * @see #setPadCharacter
//     * @see #getPadCharacter
//     * @see #setPadPosition
//     * @see #PAD_BEFORE_PREFIX
//     * @see #PAD_AFTER_PREFIX
//     * @see #PAD_BEFORE_SUFFIX
//     * @see #PAD_AFTER_SUFFIX
//     * @stable ICU 2.0
//     */
//    public int getPadPosition() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the position at which padding will take place. This is the location at
//     * which padding will be inserted if the result of <code>format()</code> is shorter
//     * than the format width. This has no effect unless padding is enabled.
//     *
//     * @param padPos the pad position, one of <code>PAD_BEFORE_PREFIX</code>,
//     * <code>PAD_AFTER_PREFIX</code>, <code>PAD_BEFORE_SUFFIX</code>, or
//     * <code>PAD_AFTER_SUFFIX</code>.
//     * @exception IllegalArgumentException if the pad position in unrecognized
//     * @see #setFormatWidth
//     * @see #getFormatWidth
//     * @see #setPadCharacter
//     * @see #getPadCharacter
//     * @see #getPadPosition
//     * @see #PAD_BEFORE_PREFIX
//     * @see #PAD_AFTER_PREFIX
//     * @see #PAD_BEFORE_SUFFIX
//     * @see #PAD_AFTER_SUFFIX
//     * @stable ICU 2.0
//     */
//    public void setPadPosition(int padPos) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns whether or not scientific notation is used.
//     *
//     * @return true if this object formats and parses scientific notation
//     * @see #setScientificNotation
//     * @see #getMinimumExponentDigits
//     * @see #setMinimumExponentDigits
//     * @see #isExponentSignAlwaysShown
//     * @see #setExponentSignAlwaysShown
//     * @stable ICU 2.0
//     */
//    public boolean isScientificNotation() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets whether or not scientific notation is used. When scientific notation is
//     * used, the effective maximum number of integer digits is <= 8. If the maximum number
//     * of integer digits is set to more than 8, the effective maximum will be 1. This
//     * allows this call to generate a 'default' scientific number format without
//     * additional changes.
//     *
//     * @param useScientific true if this object formats and parses scientific notation
//     * @see #isScientificNotation
//     * @see #getMinimumExponentDigits
//     * @see #setMinimumExponentDigits
//     * @see #isExponentSignAlwaysShown
//     * @see #setExponentSignAlwaysShown
//     * @stable ICU 2.0
//     */
//    public void setScientificNotation(boolean useScientific) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the minimum exponent digits that will be shown.
//     *
//     * @return the minimum exponent digits that will be shown
//     * @see #setScientificNotation
//     * @see #isScientificNotation
//     * @see #setMinimumExponentDigits
//     * @see #isExponentSignAlwaysShown
//     * @see #setExponentSignAlwaysShown
//     * @stable ICU 2.0
//     */
//    public byte getMinimumExponentDigits() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the minimum exponent digits that will be shown. This has no effect
//     * unless scientific notation is in use.
//     *
//     * @param minExpDig a value >= 1 indicating the fewest exponent
//     * digits that will be shown
//     * @exception IllegalArgumentException if <code>minExpDig</code> < 1
//     * @see #setScientificNotation
//     * @see #isScientificNotation
//     * @see #getMinimumExponentDigits
//     * @see #isExponentSignAlwaysShown
//     * @see #setExponentSignAlwaysShown
//     * @stable ICU 2.0
//     */
//    public void setMinimumExponentDigits(byte minExpDig) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns whether the exponent sign is always shown.
//     *
//     * @return true if the exponent is always prefixed with either the localized minus
//     * sign or the localized plus sign, false if only negative exponents are prefixed with
//     * the localized minus sign.
//     * @see #setScientificNotation
//     * @see #isScientificNotation
//     * @see #setMinimumExponentDigits
//     * @see #getMinimumExponentDigits
//     * @see #setExponentSignAlwaysShown
//     * @stable ICU 2.0
//     */
//    public boolean isExponentSignAlwaysShown() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets whether the exponent sign is always shown. This has no effect unless
//     * scientific notation is in use.
//     *
//     * @param expSignAlways true if the exponent is always prefixed with either the
//     * localized minus sign or the localized plus sign, false if only negative exponents
//     * are prefixed with the localized minus sign.
//     * @see #setScientificNotation
//     * @see #isScientificNotation
//     * @see #setMinimumExponentDigits
//     * @see #getMinimumExponentDigits
//     * @see #isExponentSignAlwaysShown
//     * @stable ICU 2.0
//     */
//    public void setExponentSignAlwaysShown(boolean expSignAlways) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns the grouping size. Grouping size is the number of digits between grouping
     * separators in the integer portion of a number. For example, in the number
     * "123,456.78", the grouping size is 3.
     *
     * @see #setGroupingSize
     * @see NumberFormat#isGroupingUsed
     * @see DecimalFormatSymbols#getGroupingSeparator
     * @stable ICU 2.0
     */
    public int getGroupingSize() {
        return ((java.text.DecimalFormat)numberFormat).getGroupingSize();
    }

    /**
     * Sets the grouping size. Grouping size is the number of digits between grouping
     * separators in the integer portion of a number. For example, in the number
     * "123,456.78", the grouping size is 3.
     *
     * @see #getGroupingSize
     * @see NumberFormat#setGroupingUsed
     * @see DecimalFormatSymbols#setGroupingSeparator
     * @stable ICU 2.0
     */
    public void setGroupingSize(int newValue) {
        ((java.text.DecimalFormat)numberFormat).setGroupingSize(newValue);
    }

//    /**
//     * {@icu} Returns the secondary grouping size. In some locales one grouping interval
//     * is used for the least significant integer digits (the primary grouping size), and
//     * another is used for all others (the secondary grouping size). A formatter
//     * supporting a secondary grouping size will return a positive integer unequal to the
//     * primary grouping size returned by <code>getGroupingSize()</code>. For example, if
//     * the primary grouping size is 4, and the secondary grouping size is 2, then the
//     * number 123456789 formats as "1,23,45,6789", and the pattern appears as "#,##,###0".
//     *
//     * @return the secondary grouping size, or a value less than one if there is none
//     * @see #setSecondaryGroupingSize
//     * @see NumberFormat#isGroupingUsed
//     * @see DecimalFormatSymbols#getGroupingSeparator
//     * @stable ICU 2.0
//     */
//    public int getSecondaryGroupingSize() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the secondary grouping size. If set to a value less than 1, then
//     * secondary grouping is turned off, and the primary grouping size is used for all
//     * intervals, not just the least significant.
//     *
//     * @see #getSecondaryGroupingSize
//     * @see NumberFormat#setGroupingUsed
//     * @see DecimalFormatSymbols#setGroupingSeparator
//     * @stable ICU 2.0
//     */
//    public void setSecondaryGroupingSize(int newValue) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the MathContext used by this format.
//     *
//     * @return desired MathContext
//     * @see #getMathContext
//     * @stable ICU 4.2
//     */
//    public MathContext getMathContextICU() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the MathContext used by this format.
//     *
//     * @return desired MathContext
//     * @see #getMathContext
//     * @stable ICU 4.2
//     */
//    public java.math.MathContext getMathContext() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the MathContext used by this format.
//     *
//     * @param newValue desired MathContext
//     * @see #getMathContext
//     * @stable ICU 4.2
//     */
//    public void setMathContextICU(MathContext newValue) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the MathContext used by this format.
//     *
//     * @param newValue desired MathContext
//     * @see #getMathContext
//     * @stable ICU 4.2
//     */
//    public void setMathContext(java.math.MathContext newValue) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns the behavior of the decimal separator with integers. (The decimal
     * separator will always appear with decimals.)  <p> Example: Decimal ON: 12345 ->
     * 12345.; OFF: 12345 -> 12345
     *
     * @stable ICU 2.0
     */
    public boolean isDecimalSeparatorAlwaysShown() {
        return ((java.text.DecimalFormat)numberFormat).isDecimalSeparatorAlwaysShown();
    }

    /**
     * Sets the behavior of the decimal separator with integers. (The decimal separator
     * will always appear with decimals.)
     *
     * <p>This only affects formatting, and only where there might be no digits after the
     * decimal point, e.g., if true, 3456.00 -> "3,456." if false, 3456.00 -> "3456" This
     * is independent of parsing. If you want parsing to stop at the decimal point, use
     * setParseIntegerOnly.
     *
     * <p>
     * Example: Decimal ON: 12345 -> 12345.; OFF: 12345 -> 12345
     *
     * @stable ICU 2.0
     */
    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        ((java.text.DecimalFormat)numberFormat).setDecimalSeparatorAlwaysShown(newValue);
    }

//    /**
//     * {@icu} Returns a copy of the CurrencyPluralInfo used by this format. It might
//     * return null if the decimal format is not a plural type currency decimal
//     * format. Plural type currency decimal format means either the pattern in the decimal
//     * format contains 3 currency signs, or the decimal format is initialized with
//     * PLURALCURRENCYSTYLE.
//     *
//     * @return desired CurrencyPluralInfo
//     * @see CurrencyPluralInfo
//     * @stable ICU 4.2
//     */
//    public CurrencyPluralInfo getCurrencyPluralInfo() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the CurrencyPluralInfo used by this format. The format uses a copy of
//     * the provided information.
//     *
//     * @param newInfo desired CurrencyPluralInfo
//     * @see CurrencyPluralInfo
//     * @stable ICU 4.2
//     */
//    public void setCurrencyPluralInfo(CurrencyPluralInfo newInfo) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Overrides clone.
     * @stable ICU 2.0
     */
    public Object clone() {
        return new DecimalFormatSymbols((java.text.DecimalFormatSymbols)numberFormat.clone());
    }

    /**
     * Overrides equals.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Overrides hashCode.
     * @stable ICU 2.0
     */
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Synthesizes a pattern string that represents the current state of this Format
     * object.
     *
     * @see #applyPattern
     * @stable ICU 2.0
     */
    public String toPattern() {
        return ((java.text.DecimalFormat)numberFormat).toPattern();
    }

    /**
     * Synthesizes a localized pattern string that represents the current state of this
     * Format object.
     *
     * @see #applyPattern
     * @stable ICU 2.0
     */
    public String toLocalizedPattern() {
        return ((java.text.DecimalFormat)numberFormat).toLocalizedPattern();
    }

    /**
     * Formats the object to an attributed string, and return the corresponding iterator.
     *
     * @stable ICU 3.6
     */
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        AttributedCharacterIterator it = numberFormat.formatToCharacterIterator(obj);

        // Extract formatted String first
        StringBuilder sb = new StringBuilder();
        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            sb.append(c);
        }

        // Create AttributedString
        AttributedString attrstr = new AttributedString(sb.toString());

        // Map JDK Field to ICU Field
        int idx = 0;
        it.first();
        while (idx < it.getEndIndex()) {
            int end = it.getRunLimit();
            Map<Attribute, Object> attributes = it.getAttributes();
            if (attributes != null) {
                for (Entry<Attribute, Object> entry : attributes.entrySet()) {
                    Attribute attr = entry.getKey();
                    Object val = entry.getValue();
                    if (attr.equals(java.text.NumberFormat.Field.CURRENCY)) {
                        val = attr = Field.CURRENCY;
                    } else if (attr.equals(java.text.NumberFormat.Field.DECIMAL_SEPARATOR)) {
                        val = attr = Field.DECIMAL_SEPARATOR;
                    } else if (attr.equals(java.text.NumberFormat.Field.EXPONENT)) {
                        val = attr = Field.EXPONENT;
                    } else if (attr.equals(java.text.NumberFormat.Field.EXPONENT_SIGN)) {
                        val = attr = Field.EXPONENT_SIGN;
                    } else if (attr.equals(java.text.NumberFormat.Field.EXPONENT_SYMBOL)) {
                        val = attr = Field.EXPONENT_SYMBOL;
                    } else if (attr.equals(java.text.NumberFormat.Field.FRACTION)) {
                        val = attr = Field.FRACTION;
                    } else if (attr.equals(java.text.NumberFormat.Field.GROUPING_SEPARATOR)) {
                        val = attr = Field.GROUPING_SEPARATOR;
                    } else if (attr.equals(java.text.NumberFormat.Field.INTEGER)) {
                        val = attr = Field.INTEGER;
                    } else if (attr.equals(java.text.NumberFormat.Field.PERCENT)) {
                        val = attr = Field.PERCENT;
                    } else if (attr.equals(java.text.NumberFormat.Field.PERMILLE)) {
                        val = attr = Field.PERMILLE;
                    } else if (attr.equals(java.text.NumberFormat.Field.SIGN)) {
                        val = attr = Field.SIGN;
                    }
                    attrstr.addAttribute(attr, val, idx, end);
                }
            }
            idx = end;
            while (it.getIndex() < idx) {
                it.next();
            }
        }

        return attrstr.getIterator();
    }

    /**
     * Applies the given pattern to this Format object. A pattern is a short-hand
     * specification for the various formatting properties. These properties can also be
     * changed individually through the various setter methods.
     *
     * <p>There is no limit to integer digits are set by this routine, since that is the
     * typical end-user desire; use setMaximumInteger if you want to set a real value. For
     * negative numbers, use a second pattern, separated by a semicolon
     *
     * <p>Example "#,#00.0#" -> 1,234.56
     *
     * <p>This means a minimum of 2 integer digits, 1 fraction digit, and a maximum of 2
     * fraction digits.
     *
     * <p>Example: "#,#00.0#;(#,#00.0#)" for negatives in parentheses.
     *
     * <p>In negative patterns, the minimum and maximum counts are ignored; these are
     * presumed to be set in the positive pattern.
     *
     * @stable ICU 2.0
     */
    public void applyPattern(String pattern) {
        ((java.text.DecimalFormat)numberFormat).applyPattern(pattern);
    }

    /**
     * Applies the given pattern to this Format object. The pattern is assumed to be in a
     * localized notation. A pattern is a short-hand specification for the various
     * formatting properties. These properties can also be changed individually through
     * the various setter methods.
     *
     * <p>There is no limit to integer digits are set by this routine, since that is the
     * typical end-user desire; use setMaximumInteger if you want to set a real value. For
     * negative numbers, use a second pattern, separated by a semicolon
     *
     * <p>Example "#,#00.0#" -> 1,234.56
     *
     * <p>This means a minimum of 2 integer digits, 1 fraction digit, and a maximum of 2
     * fraction digits.
     *
     * <p>Example: "#,#00.0#;(#,#00.0#)" for negatives in parantheses.
     *
     * <p>In negative patterns, the minimum and maximum counts are ignored; these are
     * presumed to be set in the positive pattern.
     *
     * @stable ICU 2.0
     */
    public void applyLocalizedPattern(String pattern) {
        ((java.text.DecimalFormat)numberFormat).applyLocalizedPattern(pattern);
    }

    /**
     * Sets the maximum number of digits allowed in the integer portion of a number. This
     * override limits the integer digit count to 309.
     *
     * @see NumberFormat#setMaximumIntegerDigits
     * @stable ICU 2.0
     */
    public void setMaximumIntegerDigits(int newValue) {
        super.setMaximumIntegerDigits(newValue);
    }

    /**
     * Sets the minimum number of digits allowed in the integer portion of a number. This
     * override limits the integer digit count to 309.
     *
     * @see NumberFormat#setMinimumIntegerDigits
     * @stable ICU 2.0
     */
    public void setMinimumIntegerDigits(int newValue) {
        super.setMinimumIntegerDigits(newValue);
    }

//    /**
//     * {@icu} Returns the minimum number of significant digits that will be
//     * displayed. This value has no effect unless {@link #areSignificantDigitsUsed()}
//     * returns true.
//     *
//     * @return the fewest significant digits that will be shown
//     * @stable ICU 3.0
//     */
//    public int getMinimumSignificantDigits() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the maximum number of significant digits that will be
//     * displayed. This value has no effect unless {@link #areSignificantDigitsUsed()}
//     * returns true.
//     *
//     * @return the most significant digits that will be shown
//     * @stable ICU 3.0
//     */
//    public int getMaximumSignificantDigits() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the minimum number of significant digits that will be displayed. If
//     * <code>min</code> is less than one then it is set to one. If the maximum significant
//     * digits count is less than <code>min</code>, then it is set to
//     * <code>min</code>. This value has no effect unless {@link #areSignificantDigitsUsed()}
//     * returns true.
//     *
//     * @param min the fewest significant digits to be shown
//     * @stable ICU 3.0
//     */
//    public void setMinimumSignificantDigits(int min) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets the maximum number of significant digits that will be displayed. If
//     * <code>max</code> is less than one then it is set to one. If the minimum significant
//     * digits count is greater than <code>max</code>, then it is set to
//     * <code>max</code>. This value has no effect unless {@link #areSignificantDigitsUsed()}
//     * returns true.
//     *
//     * @param max the most significant digits to be shown
//     * @stable ICU 3.0
//     */
//    public void setMaximumSignificantDigits(int max) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns true if significant digits are in use or false if integer and
//     * fraction digit counts are in use.
//     *
//     * @return true if significant digits are in use
//     * @stable ICU 3.0
//     */
//    public boolean areSignificantDigitsUsed() {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets whether significant digits are in use, or integer and fraction digit
//     * counts are in use.
//     *
//     * @param useSignificantDigits true to use significant digits, or false to use integer
//     * and fraction digit counts
//     * @stable ICU 3.0
//     */
//    public void setSignificantDigitsUsed(boolean useSignificantDigits) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Sets the <tt>Currency</tt> object used to display currency amounts. This takes
     * effect immediately, if this format is a currency format. If this format is not a
     * currency format, then the currency object is used if and when this object becomes a
     * currency format through the application of a new pattern.
     *
     * @param theCurrency new currency object to use. Must not be null.
     * @stable ICU 2.2
     */
    public void setCurrency(Currency theCurrency) {
        super.setCurrency(theCurrency);
    }

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a number. This
     * override limits the fraction digit count to 340.
     *
     * @see NumberFormat#setMaximumFractionDigits
     * @stable ICU 2.0
     */
    public void setMaximumFractionDigits(int newValue) {
        super.setMaximumFractionDigits(newValue);
    }

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a number. This
     * override limits the fraction digit count to 340.
     *
     * @see NumberFormat#setMinimumFractionDigits
     * @stable ICU 2.0
     */
    public void setMinimumFractionDigits(int newValue) {
        super.setMinimumFractionDigits(newValue);
    }

    /**
     * Sets whether {@link #parse(String, ParsePosition)} returns BigDecimal. The
     * default value is false.
     *
     * @param value true if {@link #parse(String, ParsePosition)}
     * returns BigDecimal.
     * @stable ICU 3.6
     */
    public void setParseBigDecimal(boolean value) {
        ((java.text.DecimalFormat)numberFormat).setParseBigDecimal(value);
    }

    /**
     * Returns whether {@link #parse(String, ParsePosition)} returns BigDecimal.
     *
     * @return true if {@link #parse(String, ParsePosition)} returns BigDecimal.
     * @stable ICU 3.6
     */
    public boolean isParseBigDecimal() {
        return ((java.text.DecimalFormat)numberFormat).isParseBigDecimal();
    }

    // ----------------------------------------------------------------------
    // CONSTANTS
    // ----------------------------------------------------------------------

    /**
     * {@icu} Constant for {@link #getPadPosition()} and {@link #setPadPosition(int)} to
     * specify pad characters inserted before the prefix.
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
     * {@icu} Constant for {@link #getPadPosition()} and {@link #setPadPosition(int)} to
     * specify pad characters inserted after the prefix.
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
     * {@icu} Constant for {@link #getPadPosition()} and {@link #setPadPosition(int)} to
     * specify pad characters inserted before the suffix.
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
     * {@icu} Constant for {@link #getPadPosition()} and {@link #setPadPosition(int)} to
     * specify pad characters inserted after the suffix.
     *
     * @see #setPadPosition
     * @see #getPadPosition
     * @see #PAD_BEFORE_PREFIX
     * @see #PAD_AFTER_PREFIX
     * @see #PAD_BEFORE_SUFFIX
     * @stable ICU 2.0
     */
    public static final int PAD_AFTER_SUFFIX = 3;
}

// eof
