// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;

/**
 * A class that defines the notation style to be used when formatting numbers in NumberFormatter.
 *
 * @stable ICU 60
 * @see NumberFormatter
 */
public class Notation {

    // TODO: Support engineering intervals other than 3?
    private static final ScientificNotation SCIENTIFIC = new ScientificNotation(1,
            false,
            1,
            SignDisplay.AUTO);
    private static final ScientificNotation ENGINEERING = new ScientificNotation(3,
            false,
            1,
            SignDisplay.AUTO);
    private static final CompactNotation COMPACT_SHORT = new CompactNotation(CompactStyle.SHORT);
    private static final CompactNotation COMPACT_LONG = new CompactNotation(CompactStyle.LONG);
    private static final SimpleNotation SIMPLE = new SimpleNotation();

    /* package-private */ Notation() {
    }

    /**
     * Print the number using scientific notation (also known as scientific form, standard index form, or
     * standard form in the UK). The format for scientific notation varies by locale; for example, many
     * Western locales display the number in the form "#E0", where the number is displayed with one digit
     * before the decimal separator, zero or more digits after the decimal separator, and the
     * corresponding power of 10 displayed after the "E".
     *
     * <p>
     * Example outputs in <em>en-US</em> when printing 8.765E4 through 8.765E-3:
     *
     * <pre>
     * 8.765E4
     * 8.765E3
     * 8.765E2
     * 8.765E1
     * 8.765E0
     * 8.765E-1
     * 8.765E-2
     * 8.765E-3
     * 0E0
     * </pre>
     *
     * @return A ScientificNotation for chaining or passing to the NumberFormatter notation() setter.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static ScientificNotation scientific() {
        return SCIENTIFIC;
    }

    /**
     * Print the number using engineering notation, a variant of scientific notation in which the
     * exponent must be divisible by 3.
     *
     * <p>
     * Example outputs in <em>en-US</em> when printing 8.765E4 through 8.765E-3:
     *
     * <pre>
     * 87.65E3
     * 8.765E3
     * 876.5E0
     * 87.65E0
     * 8.765E0
     * 876.5E-3
     * 87.65E-3
     * 8.765E-3
     * 0E0
     * </pre>
     *
     * @return A ScientificNotation for chaining or passing to the NumberFormatter notation() setter.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static ScientificNotation engineering() {
        return ENGINEERING;
    }

    /**
     * Print the number using short-form compact notation.
     *
     * <p>
     * <em>Compact notation</em>, defined in Unicode Technical Standard #35 Part 3 Section 2.4.1, prints
     * numbers with localized prefixes or suffixes corresponding to different powers of ten. Compact
     * notation is similar to engineering notation in how it scales numbers.
     *
     * <p>
     * Compact notation is ideal for displaying large numbers (over ~1000) to humans while at the same
     * time minimizing screen real estate.
     *
     * <p>
     * In short form, the powers of ten are abbreviated. In <em>en-US</em>, the abbreviations are "K" for
     * thousands, "M" for millions, "B" for billions, and "T" for trillions. Example outputs in
     * <em>en-US</em> when printing 8.765E7 through 8.765E0:
     *
     * <pre>
     * 88M
     * 8.8M
     * 876K
     * 88K
     * 8.8K
     * 876
     * 88
     * 8.8
     * </pre>
     *
     * <p>
     * When compact notation is specified without an explicit rounding strategy, numbers are rounded off
     * to the closest integer after scaling the number by the corresponding power of 10, but with a digit
     * shown after the decimal separator if there is only one digit before the decimal separator. The
     * default compact notation rounding strategy is equivalent to:
     *
     * <pre>
     * Rounder.integer().withMinDigits(2)
     * </pre>
     *
     * @return A CompactNotation for passing to the NumberFormatter notation() setter.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static CompactNotation compactShort() {
        return COMPACT_SHORT;
    }

    /**
     * Print the number using long-form compact notation. For more information on compact notation, see
     * {@link #compactShort}.
     *
     * <p>
     * In long form, the powers of ten are spelled out fully. Example outputs in <em>en-US</em> when
     * printing 8.765E7 through 8.765E0:
     *
     * <pre>
     * 88 million
     * 8.8 million
     * 876 thousand
     * 88 thousand
     * 8.8 thousand
     * 876
     * 88
     * 8.8
     * </pre>
     *
     * @return A CompactNotation for passing to the NumberFormatter notation() setter.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static CompactNotation compactLong() {
        return COMPACT_LONG;
    }

    /**
     * Print the number using simple notation without any scaling by powers of ten. This is the default
     * behavior.
     *
     * <p>
     * Since this is the default behavior, this method needs to be called only when it is necessary to
     * override a previous setting.
     *
     * <p>
     * Example outputs in <em>en-US</em> when printing 8.765E7 through 8.765E0:
     *
     * <pre>
     * 87,650,000
     * 8,765,000
     * 876,500
     * 87,650
     * 8,765
     * 876.5
     * 87.65
     * 8.765
     * </pre>
     *
     * @return A SimpleNotation for passing to the NumberFormatter notation() setter.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static SimpleNotation simple() {
        return SIMPLE;
    }
}
