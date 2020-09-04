// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import com.ibm.icu.impl.number.RoundingUtils;

/**
 * A class that defines a rounding strategy based on a number of fraction places and optionally
 * significant digits to be used when formatting numbers in NumberFormatter.
 *
 * <p>
 * To create a FractionPrecision, use one of the factory methods on Precision.
 *
 * @stable ICU 60
 * @see NumberFormatter
 */
public abstract class FractionPrecision extends Precision {

    /* package-private */ FractionPrecision() {
    }

    /**
     * Ensure that no less than this number of significant digits are retained when rounding according to
     * fraction rules.
     *
     * <p>
     * For example, with integer rounding, the number 3.141 becomes "3". However, with minimum figures
     * set to 2, 3.141 becomes "3.1" instead.
     *
     * <p>
     * This setting does not affect the number of trailing zeros. For example, 3.01 would print as "3",
     * not "3.0".
     *
     * @param minSignificantDigits
     *            The number of significant figures to guarantee.
     * @return A Precision for chaining or passing to the NumberFormatter rounding() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 1.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public Precision withMinDigits(int minSignificantDigits) {
        if (minSignificantDigits >= 1 && minSignificantDigits <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return constructFractionSignificant(this, minSignificantDigits, -1);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 1 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Ensure that no more than this number of significant digits are retained when rounding according to
     * fraction rules.
     *
     * <p>
     * For example, with integer rounding, the number 123.4 becomes "123". However, with maximum figures
     * set to 2, 123.4 becomes "120" instead.
     *
     * <p>
     * This setting does not affect the number of trailing zeros. For example, with fixed fraction of 2,
     * 123.4 would become "120.00".
     *
     * @param maxSignificantDigits
     *            Round the number to no more than this number of significant figures.
     * @return A Precision for chaining or passing to the NumberFormatter rounding() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 1.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public Precision withMaxDigits(int maxSignificantDigits) {
        if (maxSignificantDigits >= 1 && maxSignificantDigits <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return constructFractionSignificant(this, -1, maxSignificantDigits);
        } else {
            throw new IllegalArgumentException("Significant digits must be between 1 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }
}
