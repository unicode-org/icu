// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.math.BigDecimal;
import java.math.MathContext;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.RoundingUtils;

/**
 * A class that defines a quantity by which a number should be multiplied when formatting.
 *
 * <p>
 * To create a Multiplier, use one of the factory methods.
 *
 * @draft ICU 62
 * @provisional This API might change or be removed in a future release.
 * @see NumberFormatter
 */
public class Multiplier {

    private static final Multiplier DEFAULT = new Multiplier(0, null);
    private static final Multiplier HUNDRED = new Multiplier(2, null);
    private static final Multiplier THOUSAND = new Multiplier(3, null);

    private static final BigDecimal BIG_DECIMAL_100 = BigDecimal.valueOf(100);
    private static final BigDecimal BIG_DECIMAL_1000 = BigDecimal.valueOf(1000);

    final int magnitude;
    final BigDecimal arbitrary;
    final BigDecimal reciprocal;
    final MathContext mc;

    private Multiplier(int magnitude, BigDecimal arbitrary) {
        this(magnitude, arbitrary, RoundingUtils.DEFAULT_MATH_CONTEXT_34_DIGITS);
    }

    private Multiplier(int magnitude, BigDecimal arbitrary, MathContext mc) {
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
     * @draft ICU 62
     * @provisional This API might change or be removed in a future release.
     * @see NumberFormatter
     */
    public static Multiplier none() {
        return DEFAULT;
    }

    /**
     * Multiply numbers by 100 before formatting. Useful for combining with a percent unit:
     * <p>
     *
     * <pre>
     * NumberFormatter.with().unit(NoUnit.PERCENT).multiplier(Multiplier.powerOfTen(2))
     * </pre>
     *
     * @return A Multiplier for passing to the setter in NumberFormatter.
     * @draft ICU 62
     * @provisional This API might change or be removed in a future release.
     * @see NumberFormatter
     */
    public static Multiplier powerOfTen(int power) {
        if (power == 0) {
            return DEFAULT;
        } else if (power == 2) {
            return HUNDRED;
        } else if (power == 3) {
            return THOUSAND;
        } else {
            return new Multiplier(power, null);
        }
    }

    /**
     * Multiply numbers by an arbitrary value before formatting. Useful for unit conversions.
     * <p>
     * This method takes a BigDecimal; also see the version that takes a double.
     *
     * @return A Multiplier for passing to the setter in NumberFormatter.
     * @draft ICU 62
     * @provisional This API might change or be removed in a future release.
     * @see NumberFormatter
     */
    public static Multiplier arbitrary(BigDecimal multiplicand) {
        if (multiplicand.compareTo(BigDecimal.ONE) == 0) {
            return DEFAULT;
        } else if (multiplicand.compareTo(BIG_DECIMAL_100) == 0) {
            return HUNDRED;
        } else if (multiplicand.compareTo(BIG_DECIMAL_1000) == 0) {
            return THOUSAND;
        } else {
            return new Multiplier(0, multiplicand);
        }
    }

    /**
     * Multiply numbers by an arbitrary value before formatting. Useful for unit conversions.
     * <p>
     * This method takes a double; also see the version that takes a BigDecimal.
     *
     * @return A Multiplier for passing to the setter in NumberFormatter.
     * @draft ICU 62
     * @provisional This API might change or be removed in a future release.
     * @see NumberFormatter
     */
    public static Multiplier arbitrary(double multiplicand) {
        if (multiplicand == 1) {
            return DEFAULT;
        } else if (multiplicand == 100.0) {
            return HUNDRED;
        } else if (multiplicand == 1000.0) {
            return THOUSAND;
        } else {
            return new Multiplier(0, BigDecimal.valueOf(multiplicand));
        }
    }

    /**
     * @internal
     * @deprecated ICU 62 This API is ICU internal only.
     */
    @Deprecated
    public Multiplier withMathContext(MathContext mc) {
        // TODO: Make this public?
        if (this.mc.equals(mc)) {
            return this;
        }
        return new Multiplier(magnitude, arbitrary, mc);
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
