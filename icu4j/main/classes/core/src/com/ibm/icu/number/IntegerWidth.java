// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import com.ibm.icu.impl.number.RoundingUtils;

/**
 * A class that defines the strategy for padding and truncating integers before the decimal separator.
 *
 * <p>
 * To create an IntegerWidth, use one of the factory methods.
 *
 * @stable ICU 60
 * @see NumberFormatter
 */
public class IntegerWidth {

    /* package-private */ static final IntegerWidth DEFAULT = new IntegerWidth(1, -1);

    final int minInt;
    final int maxInt;

    private IntegerWidth(int minInt, int maxInt) {
        this.minInt = minInt;
        this.maxInt = maxInt;
    }

    /**
     * Pad numbers at the beginning with zeros to guarantee a certain number of numerals before the
     * decimal separator.
     *
     * <p>
     * For example, with minInt=3, the number 55 will get printed as "055".
     *
     * @param minInt
     *            The minimum number of places before the decimal separator.
     * @return An IntegerWidth for chaining or passing to the NumberFormatter integerWidth() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than 0.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static IntegerWidth zeroFillTo(int minInt) {
        if (minInt == 1) {
            return DEFAULT;
        } else if (minInt >= 0 && minInt <= RoundingUtils.MAX_INT_FRAC_SIG) {
            return new IntegerWidth(minInt, -1);
        } else {
            throw new IllegalArgumentException("Integer digits must be between 0 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }

    /**
     * Truncate numbers exceeding a certain number of numerals before the decimal separator.
     *
     * For example, with maxInt=3, the number 1234 will get printed as "234".
     *
     * @param maxInt
     *            The maximum number of places before the decimal separator. maxInt == -1 means no
     *            truncation.
     * @return An IntegerWidth for passing to the NumberFormatter integerWidth() setter.
     * @throws IllegalArgumentException if the input number is too big or smaller than -1.
     * @stable ICU 60
     * @see NumberFormatter
     */
    public IntegerWidth truncateAt(int maxInt) {
        if (maxInt == this.maxInt) {
            return this;
        } else if (maxInt >= 0 && maxInt <= RoundingUtils.MAX_INT_FRAC_SIG && maxInt >= minInt) {
            return new IntegerWidth(minInt, maxInt);
        } else if (minInt == 1 && maxInt == -1) {
            return DEFAULT;
        } else if (maxInt == -1) {
            return new IntegerWidth(minInt, -1);
        } else {
            throw new IllegalArgumentException("Integer digits must be between -1 and "
                    + RoundingUtils.MAX_INT_FRAC_SIG
                    + " (inclusive)");
        }
    }
}
