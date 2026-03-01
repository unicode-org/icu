// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.FieldPosition;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.number.Modifier.Signum;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.text.UFieldPosition;

/**
 * Represents numbers and digit display properties using Binary Coded Decimal (BCD).
 *
 * @implements {@link DecimalQuantity}
 */
public abstract class DecimalQuantity_AbstractBCD implements DecimalQuantity {

    /**
     * The power of ten corresponding to the least significant digit in the BCD. For example, if this
     * object represents the number "3.14", the BCD will be "0x314" and the scale will be -2.
     *
     * <p>
     * Note that in {@link java.math.BigDecimal}, the scale is defined differently: the number of digits
     * after the decimal place, which is the negative of our definition of scale.
     */
    protected int scale;

    /**
     * The number of digits in the BCD. For example, "1007" has BCD "0x1007" and precision 4. A long
     * cannot represent precisions greater than 16.
     *
     * <p>
     * This value must be re-calculated whenever the value in bcd changes by using
     * {@link #computePrecisionAndCompact()}.
     */
    protected int precision;

    /**
     * A bitmask of properties relating to the number represented by this object.
     *
     * @see #NEGATIVE_FLAG
     * @see #INFINITY_FLAG
     * @see #NAN_FLAG
     */
    protected byte flags;

    protected static final int NEGATIVE_FLAG = 1;
    protected static final int INFINITY_FLAG = 2;
    protected static final int NAN_FLAG = 4;

    // The following three fields relate to the double-to-ascii fast path algorithm.
    // When a double is given to DecimalQuantityBCD, it is converted to using a fast algorithm. The
    // fast algorithm guarantees correctness to only the first ~12 digits of the double. The process
    // of rounding the number ensures that the converted digits are correct, falling back to a slow-
    // path algorithm if required. Therefore, if a DecimalQuantity is constructed from a double, it
    // is *required* that roundToMagnitude(), roundToIncrement(), or roundToInfinity() is called. If
    // you don't round, assertions will fail in certain other methods if you try calling them.

    /**
     * The original number provided by the user and which is represented in BCD. Used when we need to
     * re-compute the BCD for an exact double representation.
     */
    protected double origDouble;

    /**
     * The change in magnitude relative to the original double. Used when we need to re-compute the BCD
     * for an exact double representation.
     */
    protected int origDelta;

    /**
     * Whether the value in the BCD comes from the double fast path without having been rounded to ensure
     * correctness
     */
    protected boolean isApproximate;

    // Positions to keep track of leading and trailing zeros.
    // lReqPos is the magnitude of the first required leading zero.
    // rReqPos is the magnitude of the last required trailing zero.
    protected int lReqPos = 0;
    protected int rReqPos = 0;

    /**
     * The value of the (suppressed) exponent after the number has been put into
     * a notation with exponents (ex: compact, scientific).
     */
    protected int exponent = 0;

    @Override
    public void copyFrom(DecimalQuantity _other) {
        copyBcdFrom(_other);
        DecimalQuantity_AbstractBCD other = (DecimalQuantity_AbstractBCD) _other;
        lReqPos = other.lReqPos;
        rReqPos = other.rReqPos;
        scale = other.scale;
        precision = other.precision;
        flags = other.flags;
        origDouble = other.origDouble;
        origDelta = other.origDelta;
        isApproximate = other.isApproximate;
        exponent = other.exponent;
    }

    public DecimalQuantity_AbstractBCD clear() {
        lReqPos = 0;
        rReqPos = 0;
        flags = 0;
        setBcdToZero(); // sets scale, precision, hasDouble, origDouble, origDelta, exponent, and BCD data
        return this;
    }

    @Override
    public void setMinInteger(int minInt) {
        // Validation should happen outside of DecimalQuantity, e.g., in the Rounder class.
        assert minInt >= 0;

        // Special behavior: do not set minInt to be less than what is already set.
        // This is so significant digits rounding can set the integer length.
        if (minInt < lReqPos) {
            minInt = lReqPos;
        }

        // Save values into internal state
        lReqPos = minInt;
    }

    @Override
    public void setMinFraction(int minFrac) {
        // Validation should happen outside of DecimalQuantity, e.g., in the Rounder class.
        assert minFrac >= 0;

        // Save values into internal state
        // Negation is safe for minFrac/maxFrac because -Integer.MAX_VALUE > Integer.MIN_VALUE
        rReqPos = -minFrac;
    }

    @Override
    public void applyMaxInteger(int maxInt) {
        // Validation should happen outside of DecimalQuantity, e.g., in the Precision class.
        assert maxInt >= 0;

        if (precision == 0) {
            return;
        }

        if (maxInt <= scale) {
            setBcdToZero();
            return;
        }

        int magnitude = getMagnitude();
        if (maxInt <= magnitude) {
            popFromLeft(magnitude - maxInt + 1);
            compact();
        }
    }

    @Override
    public long getPositionFingerprint() {
        long fingerprint = 0;
        fingerprint ^= (lReqPos << 16);
        fingerprint ^= ((long) rReqPos << 32);
        return fingerprint;
    }

    @Override
    public void roundToIncrement(BigDecimal roundingIncrement, MathContext mathContext) {
        // Do not call this method with an increment having only a 1 or a 5 digit!
        // Use a more efficient call to either roundToMagnitude() or roundToNickel().
        // Note: The check, which is somewhat expensive, is performed in an assertion
        // to disable it in production.
        assert roundingIncrement.stripTrailingZeros().precision() != 1
                || roundingIncrement.stripTrailingZeros().unscaledValue().intValue() != 5
                || roundingIncrement.stripTrailingZeros().unscaledValue().intValue() != 1;
        BigDecimal temp = toBigDecimal();
        temp = temp.divide(roundingIncrement, 0, mathContext.getRoundingMode())
                .multiply(roundingIncrement)
                .round(mathContext);
        if (temp.signum() == 0) {
            setBcdToZero(); // keeps negative flag for -0.0
        } else {
            setToBigDecimal(temp);
        }
    }

    @Override
    public void multiplyBy(BigDecimal multiplicand) {
        if (isZeroish()) {
            return;
        }
        BigDecimal temp = toBigDecimal();
        temp = temp.multiply(multiplicand);
        setToBigDecimal(temp);
    }

    @Override
    public void negate() {
      flags ^= NEGATIVE_FLAG;
    }

    @Override
    public int getMagnitude() throws ArithmeticException {
        if (precision == 0) {
            throw new ArithmeticException("Magnitude is not well-defined for zero");
        } else {
            return scale + precision - 1;
        }
    }

    @Override
    public void adjustMagnitude(int delta) {
        if (precision != 0) {
            scale = Utility.addExact(scale, delta);
            origDelta = Utility.addExact(origDelta, delta);
            // Make sure that precision + scale won't overflow, either
            Utility.addExact(scale, precision);
        }
    }

    @Override
    public int getExponent() {
        return exponent;
    }

    @Override
    public void adjustExponent(int delta) {
        exponent = exponent + delta;
    }

    @Override
    public boolean isHasIntegerValue() {
        return scale >= 0;
    }

    @Override
    public StandardPlural getStandardPlural(PluralRules rules) {
        if (rules == null) {
            // Fail gracefully if the user didn't provide a PluralRules
            return StandardPlural.OTHER;
        } else {
            @SuppressWarnings("deprecation")
            String ruleString = rules.select(this);
            return StandardPlural.orOtherFromString(ruleString);
        }
    }

    @Override
    public double getPluralOperand(Operand operand) {
        // If this assertion fails, you need to call roundToInfinity() or some other rounding method.
        // See the comment at the top of this file explaining the "isApproximate" field.
        assert !isApproximate;

        switch (operand) {
        case i:
            // Invert the negative sign if necessary
            return isNegative() ? -toLong(true) : toLong(true);
        case f:
            return toFractionLong(true);
        case t:
            return toFractionLong(false);
        case v:
            return fractionCount();
        case w:
            return fractionCountWithoutTrailingZeros();
        case e:
            return getExponent();
        case c:
            // Plural operand `c` is currently an alias for `e`.
            return getExponent();
        default:
            return Math.abs(toDouble());
        }
    }

    @Override
    public void populateUFieldPosition(FieldPosition fp) {
        if (fp instanceof UFieldPosition) {
            ((UFieldPosition) fp).setFractionDigits((int) getPluralOperand(Operand.v),
                    (long) getPluralOperand(Operand.f));
        }
    }

    @Override
    public int getUpperDisplayMagnitude() {
        // If this assertion fails, you need to call roundToInfinity() or some other rounding method.
        // See the comment at the top of this file explaining the "isApproximate" field.
        assert !isApproximate;

        int magnitude = scale + precision;
        int result = (lReqPos > magnitude) ? lReqPos : magnitude;
        return result - 1;
    }

    @Override
    public int getLowerDisplayMagnitude() {
        // If this assertion fails, you need to call roundToInfinity() or some other rounding method.
        // See the comment at the top of this file explaining the "isApproximate" field.
        assert !isApproximate;

        int magnitude = scale;
        int result = (rReqPos < magnitude) ? rReqPos : magnitude;
        return result;
    }

    @Override
    public byte getDigit(int magnitude) {
        // If this assertion fails, you need to call roundToInfinity() or some other rounding method.
        // See the comment at the top of this file explaining the "isApproximate" field.
        assert !isApproximate;

        return getDigitPos(magnitude - scale);
    }

    private int fractionCount() {
        return Math.max(0, -getLowerDisplayMagnitude() - exponent);
    }

    private int fractionCountWithoutTrailingZeros() {
        return Math.max(-scale - exponent, 0);
    }

    @Override
    public boolean isNegative() {
        return (flags & NEGATIVE_FLAG) != 0;
    }

    @Override
    public Signum signum() {
        boolean isZero = (isZeroish() && !isInfinite());
        boolean isNeg = isNegative();
        if (isZero && isNeg) {
            return Signum.NEG_ZERO;
        } else if (isZero) {
            return Signum.POS_ZERO;
        } else if (isNeg) {
            return Signum.NEG;
        } else {
            return Signum.POS;
        }
    }

    @Override
    public boolean isInfinite() {
        return (flags & INFINITY_FLAG) != 0;
    }

    @Override
    public boolean isNaN() {
        return (flags & NAN_FLAG) != 0;
    }

    @Override
    public boolean isZeroish() {
        return precision == 0;
    }

    public void setToInt(int n) {
        setBcdToZero();
        flags = 0;
        if (n < 0) {
            flags |= NEGATIVE_FLAG;
            n = -n;
        }
        if (n != 0) {
            _setToInt(n);
            compact();
        }
    }

    private void _setToInt(int n) {
        if (n == Integer.MIN_VALUE) {
            readLongToBcd(-(long) n);
        } else {
            readIntToBcd(n);
        }
    }

    public void setToLong(long n) {
        setBcdToZero();
        flags = 0;
        if (n < 0) {
            flags |= NEGATIVE_FLAG;
            n = -n;
        }
        if (n != 0) {
            _setToLong(n);
            compact();
        }
    }

    private void _setToLong(long n) {
        if (n == Long.MIN_VALUE) {
            readBigIntegerToBcd(BigInteger.valueOf(n).negate());
        } else if (n <= Integer.MAX_VALUE) {
            readIntToBcd((int) n);
        } else {
            readLongToBcd(n);
        }
    }

    public void setToBigInteger(BigInteger n) {
        setBcdToZero();
        flags = 0;
        if (n.signum() == -1) {
            flags |= NEGATIVE_FLAG;
            n = n.negate();
        }
        if (n.signum() != 0) {
            _setToBigInteger(n);
            compact();
        }
    }

    private void _setToBigInteger(BigInteger n) {
        if (n.bitLength() < 32) {
            readIntToBcd(n.intValue());
        } else if (n.bitLength() < 64) {
            readLongToBcd(n.longValue());
        } else {
            readBigIntegerToBcd(n);
        }
    }

    /**
     * Sets the internal BCD state to represent the value in the given double.
     *
     * @param n
     *            The value to consume.
     */
    public void setToDouble(double n) {
        setBcdToZero();
        flags = 0;
        // The sign bit is the top bit in both double and long, so we can
        // get the long bits for the double and compare it to zero to check
        // the sign of the double.
        if (Double.doubleToRawLongBits(n) < 0) {
            flags |= NEGATIVE_FLAG;
            n = -n;
        }
        if (Double.isNaN(n)) {
            flags |= NAN_FLAG;
        } else if (Double.isInfinite(n)) {
            flags |= INFINITY_FLAG;
        } else if (n != 0) {
            _setToDoubleFast(n);
            compact();
        }
    }

    private static final double[] DOUBLE_MULTIPLIERS = {
            1e0,
            1e1,
            1e2,
            1e3,
            1e4,
            1e5,
            1e6,
            1e7,
            1e8,
            1e9,
            1e10,
            1e11,
            1e12,
            1e13,
            1e14,
            1e15,
            1e16,
            1e17,
            1e18,
            1e19,
            1e20,
            1e21 };

    /**
     * Uses double multiplication and division to get the number into integer space before converting to
     * digits. Since double arithmetic is inexact, the resulting digits may not be accurate.
     */
    private void _setToDoubleFast(double n) {
        isApproximate = true;
        origDouble = n;
        origDelta = 0;

        // NOTE: Unlike ICU4C, doubles are always IEEE 754 doubles.
        long ieeeBits = Double.doubleToLongBits(n);
        int exponent = (int) ((ieeeBits & 0x7ff0000000000000L) >> 52) - 0x3ff;

        // Not all integers can be represented exactly for exponent > 52
        if (exponent <= 52 && (long) n == n) {
            _setToLong((long) n);
            return;
        }

        if (exponent == -1023 || exponent == 1024) {
            // The extreme values of exponent are special; use slow path.
            convertToAccurateDouble();
            return;
        }

        // 3.3219... is log2(10)
        int fracLength = (int) ((52 - exponent) / 3.32192809488736234787031942948939017586);
        if (fracLength >= 0) {
            int i = fracLength;
            // 1e22 is the largest exact double.
            for (; i >= 22; i -= 22)
                n *= 1e22;
            n *= DOUBLE_MULTIPLIERS[i];
        } else {
            int i = fracLength;
            // 1e22 is the largest exact double.
            for (; i <= -22; i += 22)
                n /= 1e22;
            n /= DOUBLE_MULTIPLIERS[-i];
        }
        long result = Math.round(n);
        if (result != 0) {
            _setToLong(result);
            scale -= fracLength;
        }
    }

    /**
     * Uses Double.toString() to obtain an exact accurate representation of the double, overwriting it
     * into the BCD. This method can be called at any point after {@link #_setToDoubleFast} while
     * {@link #isApproximate} is still true.
     */
    private void convertToAccurateDouble() {
        double n = origDouble;
        assert n != 0;
        int delta = origDelta;
        setBcdToZero();

        // Call the slow oracle function (Double.toString in Java, sprintf in C++).
        String dstr = Double.toString(n);

        if (dstr.indexOf('E') != -1) {
            // Case 1: Exponential notation.
            assert dstr.indexOf('.') == 1;
            int expPos = dstr.indexOf('E');
            _setToLong(Long.parseLong(dstr.charAt(0) + dstr.substring(2, expPos)));
            scale += Integer.parseInt(dstr.substring(expPos + 1)) - (expPos - 1) + 1;
        } else if (dstr.charAt(0) == '0') {
            // Case 2: Fraction-only number.
            assert dstr.indexOf('.') == 1;
            _setToLong(Long.parseLong(dstr.substring(2)));
            scale += 2 - dstr.length();
        } else if (dstr.charAt(dstr.length() - 1) == '0') {
            // Case 3: Integer-only number.
            // Note: this path should not normally happen, because integer-only numbers are captured
            // before the approximate double logic is performed.
            assert dstr.indexOf('.') == dstr.length() - 2;
            assert dstr.length() - 2 <= 18;
            _setToLong(Long.parseLong(dstr.substring(0, dstr.length() - 2)));
            // no need to adjust scale
        } else {
            // Case 4: Number with both a fraction and an integer.
            int decimalPos = dstr.indexOf('.');
            _setToLong(Long.parseLong(dstr.substring(0, decimalPos) + dstr.substring(decimalPos + 1)));
            scale += decimalPos - dstr.length() + 1;
        }

        scale += delta;
        compact();
        explicitExactDouble = true;
    }

    /**
     * Whether this {@link DecimalQuantity_DualStorageBCD} has been explicitly converted to an exact
     * double. true if backed by a double that was explicitly converted via convertToAccurateDouble;
     * false otherwise. Used for testing.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public boolean explicitExactDouble = false;

    /**
     * Sets the internal BCD state to represent the value in the given BigDecimal.
     *
     * @param n
     *            The value to consume.
     */
    @Override
    public void setToBigDecimal(BigDecimal n) {
        setBcdToZero();
        flags = 0;
        if (n.signum() == -1) {
            flags |= NEGATIVE_FLAG;
            n = n.negate();
        }
        if (n.signum() != 0) {
            _setToBigDecimal(n);
            compact();
        }
    }

    private void _setToBigDecimal(BigDecimal n) {
        int fracLength = n.scale();
        n = n.scaleByPowerOfTen(fracLength);
        BigInteger bi = n.toBigInteger();
        _setToBigInteger(bi);
        scale -= fracLength;
    }

    @Override
    public long toLong(boolean truncateIfOverflow) {
        // NOTE: Call sites should be guarded by fitsInLong(), like this:
        // if (dq.fitsInLong()) { /* use dq.toLong() */ } else { /* use some fallback */ }
        // Fallback behavior upon truncateIfOverflow is to truncate at 17 digits.
        assert(truncateIfOverflow || fitsInLong());
        long result = 0L;
        int upperMagnitude = exponent + scale + precision - 1;
        if (truncateIfOverflow) {
            upperMagnitude = Math.min(upperMagnitude, 17);
        }
        for (int magnitude = upperMagnitude; magnitude >= 0; magnitude--) {
            result = result * 10 + getDigitPos(magnitude - scale - exponent);
        }
        if (isNegative()) {
            result = -result;
        }
        return result;
    }

    /**
     * This returns a long representing the fraction digits of the number, as required by PluralRules.
     * For example, if we represent the number "1.20" (including optional and required digits), then this
     * function returns "20" if includeTrailingZeros is true or "2" if false.
     * Note: this method incorporates the value of {@code exponent}
     * (for cases such as compact notation) to return the proper long value
     * represented by the result.
     */
    public long toFractionLong(boolean includeTrailingZeros) {
        long result = 0L;
        int magnitude = -1 - exponent;
        int lowerMagnitude = scale;
        if (includeTrailingZeros) {
            lowerMagnitude = Math.min(lowerMagnitude, rReqPos);
        }
        // NOTE: Java has only signed longs, so we check result <= 1e17 instead of 1e18
        for (; magnitude >= lowerMagnitude && result <= 1e17; magnitude--) {
            result = result * 10 + getDigitPos(magnitude - scale);
        }
        // Remove trailing zeros; this can happen during integer overflow cases.
        if (!includeTrailingZeros) {
            while (result > 0 && (result % 10) == 0) {
                result /= 10;
            }
        }
        return result;
    }

    static final byte[] INT64_BCD = { 9, 2, 2, 3, 3, 7, 2, 0, 3, 6, 8, 5, 4, 7, 7, 5, 8, 0, 8 };

    /**
     * Returns whether or not a Long can fully represent the value stored in this DecimalQuantity.
     */
    public boolean fitsInLong() {
        if (isInfinite() || isNaN()) {
            return false;
        }
        if (isZeroish()) {
            return true;
        }
        if (exponent + scale < 0) {
            return false;
        }
        int magnitude = getMagnitude();
        if (magnitude < 18) {
            return true;
        }
        if (magnitude > 18) {
            return false;
        }
        // Hard case: the magnitude is 10^18.
        // The largest int64 is: 9,223,372,036,854,775,807
        for (int p = 0; p < precision; p++) {
            byte digit = getDigit(18 - p);
            if (digit < INT64_BCD[p]) {
                return true;
            } else if (digit > INT64_BCD[p]) {
                return false;
            }
        }
        // Exactly equal to max long plus one.
        return isNegative();
    }

    /**
     * Returns a double approximating the internal BCD. The double may not retain all of the information
     * encoded in the BCD if the BCD represents a number out of range of a double.
     *
     * @return A double representation of the internal BCD.
     */
    @Override
    public double toDouble() {
        // If this assertion fails, you need to call roundToInfinity() or some other rounding method.
        // See the comment at the top of this file explaining the "isApproximate" field.
        assert !isApproximate;

        if (isNaN()) {
            return Double.NaN;
        } else if (isInfinite()) {
            return isNegative() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }

        StringBuilder sb = new StringBuilder();
        toScientificString(sb);
        return Double.valueOf(sb.toString());
    }

    @Override
    public BigDecimal toBigDecimal() {
        if (isApproximate) {
            // Converting to a BigDecimal requires Double.toString().
            convertToAccurateDouble();
        }
        return bcdToBigDecimal();
    }

    private static int safeSubtract(int a, int b) {
        int diff = a - b;
        if (b < 0 && diff < a)
            return Integer.MAX_VALUE;
        if (b > 0 && diff > a)
            return Integer.MIN_VALUE;
        return diff;
    }

    private static final int SECTION_LOWER_EDGE = -1;
    private static final int SECTION_UPPER_EDGE = -2;

    /** Removes all fraction digits. */
    public void truncate() {
        if (scale < 0) {
            shiftRight(-scale);
            scale = 0;
            compact();
        }
    }

    @Override
    public void roundToNickel(int magnitude, MathContext mathContext) {
        roundToMagnitude(magnitude, mathContext, true);
    }

    @Override
    public void roundToMagnitude(int magnitude, MathContext mathContext) {
        roundToMagnitude(magnitude, mathContext, false);
    }

    private void roundToMagnitude(int magnitude, MathContext mathContext, boolean nickel) {
        // The position in the BCD at which rounding will be performed; digits to the right of position
        // will be rounded away.
        int position = safeSubtract(magnitude, scale);

        // Enforce the number of digits required by the MathContext.
        int _mcPrecision = mathContext.getPrecision();
        if (_mcPrecision > 0 && precision - _mcPrecision > position) {
            position = precision - _mcPrecision;
        }

        // "trailing" = least significant digit to the left of rounding
        byte trailingDigit = getDigitPos(position);

        if (position <= 0 && !isApproximate && (!nickel || trailingDigit == 0 || trailingDigit == 5)) {
            // All digits are to the left of the rounding magnitude.
        } else if (precision == 0) {
            // No rounding for zero.
        } else {
            // Perform rounding logic.
            // "leading" = most significant digit to the right of rounding
            byte leadingDigit = getDigitPos(safeSubtract(position, 1));

            // Compute which section of the number we are in.
            // EDGE means we are at the bottom or top edge, like 1.000 or 1.999 (used by doubles)
            // LOWER means we are between the bottom edge and the midpoint, like 1.391
            // MIDPOINT means we are exactly in the middle, like 1.500
            // UPPER means we are between the midpoint and the top edge, like 1.916
            int section;
            if (!isApproximate) {
                if (nickel && trailingDigit != 2 && trailingDigit != 7) {
                    // Nickel rounding, and not at .02x or .07x
                    if (trailingDigit < 2) {
                        // .00, .01 => down to .00
                        section = RoundingUtils.SECTION_LOWER;
                    } else if (trailingDigit < 5) {
                        // .03, .04 => up to .05
                        section = RoundingUtils.SECTION_UPPER;
                    } else if (trailingDigit < 7) {
                        // .05, .06 => down to .05
                        section = RoundingUtils.SECTION_LOWER;
                    } else {
                        // .08, .09 => up to .10
                        section = RoundingUtils.SECTION_UPPER;
                    }
                } else if (leadingDigit < 5) {
                    // Includes nickel rounding .020-.024 and .070-.074
                    section = RoundingUtils.SECTION_LOWER;
                } else if (leadingDigit > 5) {
                    // Includes nickel rounding .026-.029 and .076-.079
                    section = RoundingUtils.SECTION_UPPER;
                } else {
                    // Includes nickel rounding .025 and .075
                    section = RoundingUtils.SECTION_MIDPOINT;
                    for (int p = safeSubtract(position, 2); p >= 0; p--) {
                        if (getDigitPos(p) != 0) {
                            section = RoundingUtils.SECTION_UPPER;
                            break;
                        }
                    }
                }
            } else {
                int p = safeSubtract(position, 2);
                int minP = Math.max(0, precision - 14);
                if (leadingDigit == 0 && (!nickel || trailingDigit == 0 || trailingDigit == 5)) {
                    section = SECTION_LOWER_EDGE;
                    for (; p >= minP; p--) {
                        if (getDigitPos(p) != 0) {
                            section = RoundingUtils.SECTION_LOWER;
                            break;
                        }
                    }
                } else if (leadingDigit == 4 && (!nickel || trailingDigit == 2 || trailingDigit == 7)) {
                    section = RoundingUtils.SECTION_MIDPOINT;
                    for (; p >= minP; p--) {
                        if (getDigitPos(p) != 9) {
                            section = RoundingUtils.SECTION_LOWER;
                            break;
                        }
                    }
                } else if (leadingDigit == 5 && (!nickel || trailingDigit == 2 || trailingDigit == 7)) {
                    section = RoundingUtils.SECTION_MIDPOINT;
                    for (; p >= minP; p--) {
                        if (getDigitPos(p) != 0) {
                            section = RoundingUtils.SECTION_UPPER;
                            break;
                        }
                    }
                } else if (leadingDigit == 9 && (!nickel || trailingDigit == 4 || trailingDigit == 9)) {
                    section = SECTION_UPPER_EDGE;
                    for (; p >= minP; p--) {
                        if (getDigitPos(p) != 9) {
                            section = RoundingUtils.SECTION_UPPER;
                            break;
                        }
                    }
                } else if (nickel && trailingDigit != 2 && trailingDigit != 7) {
                    // Nickel rounding, and not at .02x or .07x
                    if (trailingDigit < 2) {
                        // .00, .01 => down to .00
                        section = RoundingUtils.SECTION_LOWER;
                    } else if (trailingDigit < 5) {
                        // .03, .04 => up to .05
                        section = RoundingUtils.SECTION_UPPER;
                    } else if (trailingDigit < 7) {
                        // .05, .06 => down to .05
                        section = RoundingUtils.SECTION_LOWER;
                    } else {
                        // .08, .09 => up to .10
                        section = RoundingUtils.SECTION_UPPER;
                    }
                } else if (leadingDigit < 5) {
                    // Includes nickel rounding .020-.024 and .070-.074
                    section = RoundingUtils.SECTION_LOWER;
                } else {
                    // Includes nickel rounding .026-.029 and .076-.079
                    section = RoundingUtils.SECTION_UPPER;
                }

                boolean roundsAtMidpoint = RoundingUtils
                        .roundsAtMidpoint(mathContext.getRoundingMode().ordinal());
                if (safeSubtract(position, 1) < precision - 14
                        || (roundsAtMidpoint && section == RoundingUtils.SECTION_MIDPOINT)
                        || (!roundsAtMidpoint && section < 0 /* i.e. at upper or lower edge */)) {
                    // Oops! This means that we have to get the exact representation of the double,
                    // because the zone of uncertainty is along the rounding boundary.
                    convertToAccurateDouble();
                    roundToMagnitude(magnitude, mathContext, nickel); // start over
                    return;
                }

                // Turn off the approximate double flag, since the value is now confirmed to be exact.
                isApproximate = false;
                origDouble = 0.0;
                origDelta = 0;

                if (position <= 0 && (!nickel || trailingDigit == 0 || trailingDigit == 5)) {
                    // All digits are to the left of the rounding magnitude.
                    return;
                }

                // Good to continue rounding.
                if (section == SECTION_LOWER_EDGE)
                    section = RoundingUtils.SECTION_LOWER;
                if (section == SECTION_UPPER_EDGE)
                    section = RoundingUtils.SECTION_UPPER;
            }

            // Nickel rounding "half even" goes to the nearest whole (away from the 5).
            boolean isEven = nickel
                    ? (trailingDigit < 2 || trailingDigit > 7
                            || (trailingDigit == 2 && section != RoundingUtils.SECTION_UPPER)
                            || (trailingDigit == 7 && section == RoundingUtils.SECTION_UPPER))
                    : (trailingDigit % 2) == 0;

            boolean roundDown = RoundingUtils.getRoundingDirection(isEven,
                    isNegative(),
                    section,
                    mathContext.getRoundingMode().ordinal(),
                    this);

            // Perform truncation
            if (position >= precision) {
                setBcdToZero();
                scale = magnitude;
            } else {
                shiftRight(position);
            }

            if (nickel) {
                if (trailingDigit < 5 && roundDown) {
                    setDigitPos(0, (byte) 0);
                    compact();
                    return;
                } else if (trailingDigit >= 5 && !roundDown) {
                    setDigitPos(0, (byte) 9);
                    trailingDigit = 9;
                    // do not return: use the bubbling logic below
                } else {
                    setDigitPos(0, (byte) 5);
                    // compact not necessary: digit at position 0 is nonzero
                    return;
                }
            }

            // Bubble the result to the higher digits
            if (!roundDown) {
                if (trailingDigit == 9) {
                    int bubblePos = 0;
                    // Note: in the long implementation, the most digits BCD can have at this point is
                    // 15, so bubblePos <= 15 and getDigitPos(bubblePos) is safe.
                    for (; getDigitPos(bubblePos) == 9; bubblePos++) {
                    }
                    shiftRight(bubblePos); // shift off the trailing 9s
                }
                byte digit0 = getDigitPos(0);
                assert digit0 != 9;
                setDigitPos(0, (byte) (digit0 + 1));
                precision += 1; // in case an extra digit got added
            }

            compact();
        }
    }

    @Override
    public void roundToInfinity() {
        if (isApproximate) {
            convertToAccurateDouble();
        }
    }

    /**
     * Appends a digit, optionally with one or more leading zeros, to the end of the value represented by
     * this DecimalQuantity.
     *
     * <p>
     * The primary use of this method is to construct numbers during a parsing loop. It allows parsing to
     * take advantage of the digit list infrastructure primarily designed for formatting.
     *
     * @param value
     *            The digit to append.
     * @param leadingZeros
     *            The number of zeros to append before the digit. For example, if the value in this
     *            instance starts as 12.3, and you append a 4 with 1 leading zero, the value becomes
     *            12.304.
     * @param appendAsInteger
     *            If true, increase the magnitude of existing digits to make room for the new digit. If
     *            false, append to the end like a fraction digit. If true, there must not be any fraction
     *            digits already in the number.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void appendDigit(byte value, int leadingZeros, boolean appendAsInteger) {
        assert leadingZeros >= 0;

        // Zero requires special handling to maintain the invariant that the least-significant digit
        // in the BCD is nonzero.
        if (value == 0) {
            if (appendAsInteger && precision != 0) {
                scale += leadingZeros + 1;
            }
            return;
        }

        // Deal with trailing zeros
        if (scale > 0) {
            leadingZeros += scale;
            if (appendAsInteger) {
                scale = 0;
            }
        }

        // Append digit
        shiftLeft(leadingZeros + 1);
        setDigitPos(0, value);

        // Fix scale if in integer mode
        if (appendAsInteger) {
            scale += leadingZeros + 1;
        }
    }

    @Override
    public String toPlainString() {
        StringBuilder sb = new StringBuilder();
        toPlainString(sb);
        return sb.toString();
    }

    public void toPlainString(StringBuilder result) {
        assert(!isApproximate);
        if (isNegative()) {
            result.append('-');
        }
        if (precision == 0) {
            result.append('0');
            return;
        }

        int upper = scale + precision + exponent - 1;
        int lower = scale + exponent;
        if (upper < lReqPos - 1) {
            upper = lReqPos - 1;
        }
        if (lower > rReqPos) {
            lower = rReqPos;
        }

        int p = upper;
        if (p < 0) {
            result.append('0');
        }
        for (; p >= 0; p--) {
            result.append((char) ('0' + getDigitPos(p - scale - exponent)));
        }
        if (lower < 0) {
            result.append('.');
        }
        for(; p >= lower; p--) {
            result.append((char) ('0' + getDigitPos(p - scale - exponent)));
        }
    }

    public String toScientificString() {
        StringBuilder sb = new StringBuilder();
        toScientificString(sb);
        return sb.toString();
    }

    public void toScientificString(StringBuilder result) {
        assert(!isApproximate);
        if (isNegative()) {
            result.append('-');
        }
        if (precision == 0) {
            result.append("0E+0");
            return;
        }
        // NOTE: It is not safe to add to lOptPos (aka maxInt) or subtract from
        // rOptPos (aka -maxFrac) due to overflow.
        int upperPos = precision - 1;
        int lowerPos = 0;
        int p = upperPos;
        result.append((char) ('0' + getDigitPos(p)));
        if ((--p) >= lowerPos) {
            result.append('.');
            for (; p >= lowerPos; p--) {
                result.append((char) ('0' + getDigitPos(p)));
            }
        }
        result.append('E');
        int _scale = upperPos + scale + exponent;
        if (_scale == Integer.MIN_VALUE) {
            result.append("-2147483648");
            return;
        } else if (_scale < 0) {
            _scale *= -1;
            result.append('-');
        } else {
            result.append('+');
        }
        if (_scale == 0) {
            result.append('0');
        }
        int insertIndex = result.length();
        while (_scale > 0) {
            int quot = _scale / 10;
            int rem = _scale % 10;
            result.insert(insertIndex, (char) ('0' + rem));
            _scale = quot;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof DecimalQuantity_AbstractBCD)) {
            return false;
        }
        DecimalQuantity_AbstractBCD _other = (DecimalQuantity_AbstractBCD) other;

        boolean basicEquals =
                scale == _other.scale
                && precision == _other.precision
                && flags == _other.flags
                && lReqPos == _other.lReqPos
                && rReqPos == _other.rReqPos
                && isApproximate == _other.isApproximate;
        if (!basicEquals) {
            return false;
        }

        if (precision == 0) {
            return true;
        } else if (isApproximate) {
            return origDouble == _other.origDouble && origDelta == _other.origDelta;
        } else {
            for (int m = getUpperDisplayMagnitude(); m >= getLowerDisplayMagnitude(); m--) {
                if (getDigit(m) != _other.getDigit(m)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Returns a single digit from the BCD list. No internal state is changed by calling this method.
     *
     * @param position
     *            The position of the digit to pop, counted in BCD units from the least significant
     *            digit. If outside the range supported by the implementation, zero is returned.
     * @return The digit at the specified location.
     */
    protected abstract byte getDigitPos(int position);

    /**
     * Sets the digit in the BCD list. This method only sets the digit; it is the caller's responsibility
     * to call {@link #compact} after setting the digit.
     *
     * @param position
     *            The position of the digit to pop, counted in BCD units from the least significant
     *            digit. If outside the range supported by the implementation, an AssertionError is
     *            thrown.
     * @param value
     *            The digit to set at the specified location.
     */
    protected abstract void setDigitPos(int position, byte value);

    /**
     * Adds zeros to the end of the BCD list. This will result in an invalid BCD representation; it is
     * the caller's responsibility to do further manipulation and then call {@link #compact}.
     *
     * @param numDigits
     *            The number of zeros to add.
     */
    protected abstract void shiftLeft(int numDigits);

    /**
     * Removes digits from the end of the BCD list. This may result in an invalid BCD representation; it
     * is the caller's responsibility to follow-up with a call to {@link #compact}.
     *
     * @param numDigits
     *            The number of digits to remove.
     */
    protected abstract void shiftRight(int numDigits);

    /**
     * Directly removes digits from the front of the BCD list.
     * Updates precision.
     *
     * CAUTION: it is the caller's responsibility to call {@link #compact} after this method.
     */
    protected abstract void popFromLeft(int numDigits);

    /**
     * Sets the internal representation to zero. Clears any values stored in scale, precision, hasDouble,
     * origDouble, origDelta, exponent, and BCD data.
     */
    protected abstract void setBcdToZero();

    /**
     * Sets the internal BCD state to represent the value in the given int. The int is guaranteed to be
     * either positive. The internal state is guaranteed to be empty when this method is called.
     *
     * @param n
     *            The value to consume.
     */
    protected abstract void readIntToBcd(int input);

    /**
     * Sets the internal BCD state to represent the value in the given long. The long is guaranteed to be
     * either positive. The internal state is guaranteed to be empty when this method is called.
     *
     * @param n
     *            The value to consume.
     */
    protected abstract void readLongToBcd(long input);

    /**
     * Sets the internal BCD state to represent the value in the given BigInteger. The BigInteger is
     * guaranteed to be positive, and it is guaranteed to be larger than Long.MAX_VALUE. The internal
     * state is guaranteed to be empty when this method is called.
     *
     * @param n
     *            The value to consume.
     */
    protected abstract void readBigIntegerToBcd(BigInteger input);

    /**
     * Returns a BigDecimal encoding the internal BCD value.
     *
     * @return A BigDecimal representation of the internal BCD.
     */
    protected abstract BigDecimal bcdToBigDecimal();

    protected abstract void copyBcdFrom(DecimalQuantity _other);

    /**
     * Removes trailing zeros from the BCD (adjusting the scale as required) and then computes the
     * precision. The precision is the number of digits in the number up through the greatest nonzero
     * digit.
     *
     * <p>
     * This method must always be called when bcd changes in order for assumptions to be correct in
     * methods like {@link #fractionCount()}.
     */
    protected abstract void compact();
}
