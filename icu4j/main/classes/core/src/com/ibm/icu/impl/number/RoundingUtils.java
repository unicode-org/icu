// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.Scale;
import com.ibm.icu.text.PluralRules;

/** @author sffc */
public class RoundingUtils {

    public static final int SECTION_LOWER = 1;
    public static final int SECTION_MIDPOINT = 2;
    public static final int SECTION_UPPER = 3;

    /**
     * The default rounding mode.
     */
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;

    /**
     * The maximum number of fraction places, integer numerals, or significant digits. TODO: This does
     * not feel like the best home for this value.
     */
    public static final int MAX_INT_FRAC_SIG = 999;

    /**
     * Converts a rounding mode and metadata about the quantity being rounded to a boolean determining
     * whether the value should be rounded toward infinity or toward zero.
     *
     * <p>
     * The parameters are of type int because benchmarks on an x86-64 processor against OpenJDK showed
     * that ints were demonstrably faster than enums in switch statements.
     *
     * @param isEven
     *            Whether the digit immediately before the rounding magnitude is even.
     * @param isNegative
     *            Whether the quantity is negative.
     * @param section
     *            Whether the part of the quantity to the right of the rounding magnitude is exactly
     *            halfway between two digits, whether it is in the lower part (closer to zero), or
     *            whether it is in the upper part (closer to infinity). See {@link #SECTION_LOWER},
     *            {@link #SECTION_MIDPOINT}, and {@link #SECTION_UPPER}.
     * @param roundingMode
     *            The integer version of the {@link RoundingMode}, which you can get via
     *            {@link RoundingMode#ordinal}.
     * @param reference
     *            A reference object to be used when throwing an ArithmeticException.
     * @return true if the number should be rounded toward zero; false if it should be rounded toward
     *         infinity.
     */
    public static boolean getRoundingDirection(
            boolean isEven,
            boolean isNegative,
            int section,
            int roundingMode,
            Object reference) {
        switch (roundingMode) {
        case BigDecimal.ROUND_UP:
            // round away from zero
            return false;

        case BigDecimal.ROUND_DOWN:
            // round toward zero
            return true;

        case BigDecimal.ROUND_CEILING:
            // round toward positive infinity
            return isNegative;

        case BigDecimal.ROUND_FLOOR:
            // round toward negative infinity
            return !isNegative;

        case BigDecimal.ROUND_HALF_UP:
            switch (section) {
            case SECTION_MIDPOINT:
                return false;
            case SECTION_LOWER:
                return true;
            case SECTION_UPPER:
                return false;
            }
            break;

        case BigDecimal.ROUND_HALF_DOWN:
            switch (section) {
            case SECTION_MIDPOINT:
                return true;
            case SECTION_LOWER:
                return true;
            case SECTION_UPPER:
                return false;
            }
            break;

        case BigDecimal.ROUND_HALF_EVEN:
            switch (section) {
            case SECTION_MIDPOINT:
                return isEven;
            case SECTION_LOWER:
                return true;
            case SECTION_UPPER:
                return false;
            }
            break;
        }

        // Rounding mode UNNECESSARY
        throw new ArithmeticException("Rounding is required on " + reference.toString());
    }

    /**
     * Gets whether the given rounding mode's rounding boundary is at the midpoint. The rounding boundary
     * is the point at which a number switches from being rounded down to being rounded up. For example,
     * with rounding mode HALF_EVEN, HALF_UP, or HALF_DOWN, the rounding boundary is at the midpoint, and
     * this function would return true. However, for UP, DOWN, CEILING, and FLOOR, the rounding boundary
     * is at the "edge", and this function would return false.
     *
     * @param roundingMode
     *            The integer version of the {@link RoundingMode}.
     * @return true if rounding mode is HALF_EVEN, HALF_UP, or HALF_DOWN; false otherwise.
     */
    public static boolean roundsAtMidpoint(int roundingMode) {
        switch (roundingMode) {
        case BigDecimal.ROUND_UP:
        case BigDecimal.ROUND_DOWN:
        case BigDecimal.ROUND_CEILING:
        case BigDecimal.ROUND_FLOOR:
            return false;

        default:
            return true;
        }
    }

    private static final MathContext[] MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED = new MathContext[RoundingMode
            .values().length];

    private static final MathContext[] MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS = new MathContext[RoundingMode
            .values().length];

    static {
        for (int i = 0; i < MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS.length; i++) {
            MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED[i] = new MathContext(0, RoundingMode.valueOf(i));
            MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS[i] = new MathContext(34);
        }
    }

    /** The default MathContext, unlimited-precision version. */
    public static final MathContext DEFAULT_MATH_CONTEXT_UNLIMITED
            = MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED[DEFAULT_ROUNDING_MODE.ordinal()];

    /** The default MathContext, 34-digit version. */
    public static final MathContext DEFAULT_MATH_CONTEXT_34_DIGITS
            = MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS[DEFAULT_ROUNDING_MODE.ordinal()];

    /**
     * Gets the user-specified math context out of the property bag. If there is none, falls back to a
     * math context with unlimited precision and the user-specified rounding mode, which defaults to
     * HALF_EVEN (the IEEE 754R default).
     *
     * @param properties
     *            The property bag.
     * @return A {@link MathContext}. Never null.
     */
    public static MathContext getMathContextOrUnlimited(DecimalFormatProperties properties) {
        MathContext mathContext = properties.getMathContext();
        if (mathContext == null) {
            RoundingMode roundingMode = properties.getRoundingMode();
            if (roundingMode == null)
                roundingMode = RoundingMode.HALF_EVEN;
            mathContext = MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED[roundingMode.ordinal()];
        }
        return mathContext;
    }

    /**
     * Gets the user-specified math context out of the property bag. If there is none, falls back to a
     * math context with 34 digits of precision (the 128-bit IEEE 754R default) and the user-specified
     * rounding mode, which defaults to HALF_EVEN (the IEEE 754R default).
     *
     * @param properties
     *            The property bag.
     * @return A {@link MathContext}. Never null.
     */
    public static MathContext getMathContextOr34Digits(DecimalFormatProperties properties) {
        MathContext mathContext = properties.getMathContext();
        if (mathContext == null) {
            RoundingMode roundingMode = properties.getRoundingMode();
            if (roundingMode == null)
                roundingMode = RoundingMode.HALF_EVEN;
            mathContext = MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS[roundingMode.ordinal()];
        }
        return mathContext;
    }

    /**
     * Gets a MathContext with unlimited precision and the specified RoundingMode. Equivalent to "new
     * MathContext(0, roundingMode)", but pulls from a singleton to prevent object thrashing.
     *
     * @param roundingMode
     *            The {@link RoundingMode} to use.
     * @return The corresponding {@link MathContext}.
     */
    public static MathContext mathContextUnlimited(RoundingMode roundingMode) {
        return MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED[roundingMode.ordinal()];
    }

    public static Scale scaleFromProperties(DecimalFormatProperties properties) {
        MathContext mc = getMathContextOr34Digits(properties);
        if (properties.getMagnitudeMultiplier() != 0) {
            return Scale.powerOfTen(properties.getMagnitudeMultiplier()).withMathContext(mc);
        } else if (properties.getMultiplier() != null) {
            return Scale.byBigDecimal(properties.getMultiplier()).withMathContext(mc);
        } else {
            return null;
        }
    }

    /**
     * Computes the plural form after copying the number and applying rounding rules.
     */
    public static StandardPlural getPluralSafe(
            Precision rounder, PluralRules rules, DecimalQuantity dq) {
        if (rounder == null) {
            return dq.getStandardPlural(rules);
        }
        // TODO(ICU-20500): Avoid the copy?
        DecimalQuantity copy = dq.createCopy();
        rounder.apply(copy);
        return copy.getStandardPlural(rules);
    }
}
