// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.RoundingUtils;

/**
 * A class that defines a quantity by which a number should be multiplied when formatting.
 *
 * <p>
 * To create a Multiplier, use one of the factory methods.
 *
 * @stable ICU 62
 * @see NumberFormatter
 */
public class Scale {

    private static final Scale DEFAULT = new Scale(0, null);
    private static final Scale HUNDRED = new Scale(2, null);
    private static final Scale THOUSAND = new Scale(3, null);

    private static final BigDecimal BIG_DECIMAL_100 = BigDecimal.valueOf(100);
    private static final BigDecimal BIG_DECIMAL_1000 = BigDecimal.valueOf(1000);

    final int magnitude;
    final BigDecimal arbitrary;
    final BigDecimal reciprocal;
    final MathContext mc;

    private Scale(int magnitude, BigDecimal arbitrary) {
        this(magnitude, arbitrary, RoundingUtils.DEFAULT_MATH_CONTEXT_34_DIGITS);
    }

    private Scale(int magnitude, BigDecimal arbitrary, MathContext mc) {
        if (arbitrary != null) {
            // Attempt to convert the BigDecimal to a magnitude multiplier.
            // ICU-20000: JDKs have inconsistent behavior on stripTrailingZeros() for Zero.
            arbitrary =
                arbitrary.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : arbitrary.stripTrailingZeros();
            if (arbitrary.precision() == 1 && arbitrary.unscaledValue().equals(BigInteger.ONE)) {
                // Success!
                magnitude -= arbitrary.scale();
                arbitrary = null;
            }
        }

        this.magnitude = magnitude;
        this.arbitrary = arbitrary;
        this.mc = mc;
        // We need to use a math context in order to prevent non-terminating decimal expansions.
        // This is only used when dividing by the multiplier.
        if (arbitrary != null && BigDecimal.ZERO.compareTo(arbitrary) != 0) {
            this.reciprocal = BigDecimal.ONE.divide(arbitrary, mc);
        } else {
            this.reciprocal = null;
        }
    }

    /**
     * Do not change the value of numbers when formatting or parsing.
     *
     * @return A Multiplier to prevent any multiplication.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Scale none() {
        return DEFAULT;
    }

    /**
     * Multiply numbers by 100 before formatting. Useful for combining with a percent unit:
     *
     * <pre>
     * NumberFormatter.with().unit(NoUnit.PERCENT).multiplier(Multiplier.powerOfTen(2))
     * </pre>
     *
     * @return A Multiplier for passing to the setter in NumberFormatter.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Scale powerOfTen(int power) {
        if (power == 0) {
            return DEFAULT;
        } else if (power == 2) {
            return HUNDRED;
        } else if (power == 3) {
            return THOUSAND;
        } else {
            return new Scale(power, null);
        }
    }

    /**
     * Multiply numbers by an arbitrary value before formatting. Useful for unit conversions.
     * <p>
     * This method takes a BigDecimal; also see the version that takes a double.
     *
     * @return A Multiplier for passing to the setter in NumberFormatter.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Scale byBigDecimal(BigDecimal multiplicand) {
        if (multiplicand.compareTo(BigDecimal.ONE) == 0) {
            return DEFAULT;
        } else if (multiplicand.compareTo(BIG_DECIMAL_100) == 0) {
            return HUNDRED;
        } else if (multiplicand.compareTo(BIG_DECIMAL_1000) == 0) {
            return THOUSAND;
        } else {
            return new Scale(0, multiplicand);
        }
    }

    /**
     * Multiply numbers by an arbitrary value before formatting. Useful for unit conversions.
     * <p>
     * This method takes a double; also see the version that takes a BigDecimal.
     *
     * @return A Multiplier for passing to the setter in NumberFormatter.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Scale byDouble(double multiplicand) {
        if (multiplicand == 1) {
            return DEFAULT;
        } else if (multiplicand == 100.0) {
            return HUNDRED;
        } else if (multiplicand == 1000.0) {
            return THOUSAND;
        } else {
            return new Scale(0, BigDecimal.valueOf(multiplicand));
        }
    }

    /**
     * Multiply a number by both a power of ten and by an arbitrary double value before formatting.
     *
     * @return A Multiplier for passing to the setter in NumberFormatter.
     * @stable ICU 62
     * @see NumberFormatter
     */
    public static Scale byDoubleAndPowerOfTen(double multiplicand, int power) {
        return new Scale(power, BigDecimal.valueOf(multiplicand));
    }

    /**
     * Returns whether the multiplier will change the number.
     */
    boolean isValid() {
        return magnitude != 0 || arbitrary != null;
    }

    /**
     * @internal
     * @deprecated ICU 62 This API is ICU internal only.
     */
    @Deprecated
    public Scale withMathContext(MathContext mc) {
        // TODO: Make this public?
        if (this.mc.equals(mc)) {
            return this;
        }
        return new Scale(magnitude, arbitrary, mc);
    }

    /**
     * @internal
     * @deprecated ICU 62 This API is ICU internal only.
     */
    @Deprecated
    public void applyTo(DecimalQuantity quantity) {
        quantity.adjustMagnitude(magnitude);
        if (arbitrary != null) {
            quantity.multiplyBy(arbitrary);
        }
    }

    /**
     * @internal
     * @deprecated ICU 62 This API is ICU internal only.
     */
    @Deprecated
    public void applyReciprocalTo(DecimalQuantity quantity) {
        quantity.adjustMagnitude(-magnitude);
        if (reciprocal != null) {
            quantity.multiplyBy(reciprocal);
            quantity.roundToMagnitude(quantity.getMagnitude() - mc.getPrecision(), mc);
        }
    }

}
