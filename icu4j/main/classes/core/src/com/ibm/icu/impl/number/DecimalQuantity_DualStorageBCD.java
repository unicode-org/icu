// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * A DecimalQuantity with internal storage as a 64-bit BCD, with fallback to a byte array for numbers
 * that don't fit into the standard BCD.
 */
public final class DecimalQuantity_DualStorageBCD extends DecimalQuantity_AbstractBCD {

    /**
     * The BCD of the 16 digits of the number represented by this object. Every 4 bits of the long map to
     * one digit. For example, the number "12345" in BCD is "0x12345".
     *
     * <p>
     * Whenever bcd changes internally, {@link #compact()} must be called, except in special cases like
     * setting the digit to zero.
     */
    private byte[] bcdBytes;

    private long bcdLong = 0L;

    private boolean usingBytes = false;

    @Override
    public int maxRepresentableDigits() {
        return Integer.MAX_VALUE;
    }

    public DecimalQuantity_DualStorageBCD() {
        setBcdToZero();
        flags = 0;
    }

    public DecimalQuantity_DualStorageBCD(long input) {
        setToLong(input);
    }

    public DecimalQuantity_DualStorageBCD(int input) {
        setToInt(input);
    }

    public DecimalQuantity_DualStorageBCD(double input) {
        setToDouble(input);
    }

    public DecimalQuantity_DualStorageBCD(BigInteger input) {
        setToBigInteger(input);
    }

    public DecimalQuantity_DualStorageBCD(BigDecimal input) {
        setToBigDecimal(input);
    }

    public DecimalQuantity_DualStorageBCD(DecimalQuantity_DualStorageBCD other) {
        copyFrom(other);
    }

    public DecimalQuantity_DualStorageBCD(Number number) {
        // NOTE: Number type expansion happens both here
        // and in NumberFormat.java
        if (number instanceof Long) {
            setToLong(number.longValue());
        } else if (number instanceof Integer) {
            setToInt(number.intValue());
        } else if (number instanceof Float) {
            setToDouble(number.doubleValue());
        } else if (number instanceof Double) {
            setToDouble(number.doubleValue());
        } else if (number instanceof BigInteger) {
            setToBigInteger((BigInteger) number);
        } else if (number instanceof BigDecimal) {
            setToBigDecimal((BigDecimal) number);
        } else if (number instanceof com.ibm.icu.math.BigDecimal) {
            setToBigDecimal(((com.ibm.icu.math.BigDecimal) number).toBigDecimal());
        } else {
            throw new IllegalArgumentException(
                    "Number is of an unsupported type: " + number.getClass().getName());
        }
    }

    @Override
    public DecimalQuantity createCopy() {
        return new DecimalQuantity_DualStorageBCD(this);
    }

    @Override
    protected byte getDigitPos(int position) {
        if (usingBytes) {
            if (position < 0 || position >= precision)
                return 0;
            return bcdBytes[position];
        } else {
            if (position < 0 || position >= 16)
                return 0;
            return (byte) ((bcdLong >>> (position * 4)) & 0xf);
        }
    }

    @Override
    protected void setDigitPos(int position, byte value) {
        assert position >= 0;
        if (usingBytes) {
            ensureCapacity(position + 1);
            bcdBytes[position] = value;
        } else if (position >= 16) {
            switchStorage();
            ensureCapacity(position + 1);
            bcdBytes[position] = value;
        } else {
            int shift = position * 4;
            bcdLong = bcdLong & ~(0xfL << shift) | ((long) value << shift);
        }
    }

    @Override
    protected void shiftLeft(int numDigits) {
        if (!usingBytes && precision + numDigits > 16) {
            switchStorage();
        }
        if (usingBytes) {
            ensureCapacity(precision + numDigits);
            System.arraycopy(bcdBytes, 0, bcdBytes, numDigits, precision);
            Arrays.fill(bcdBytes, 0, numDigits, (byte) 0);
        } else {
            bcdLong <<= (numDigits * 4);
        }
        scale -= numDigits;
        precision += numDigits;
    }

    @Override
    protected void shiftRight(int numDigits) {
        if (usingBytes) {
            int i = 0;
            for (; i < precision - numDigits; i++) {
                bcdBytes[i] = bcdBytes[i + numDigits];
            }
            for (; i < precision; i++) {
                bcdBytes[i] = 0;
            }
        } else {
            bcdLong >>>= (numDigits * 4);
        }
        scale += numDigits;
        precision -= numDigits;
    }

    @Override
    protected void popFromLeft(int numDigits) {
        assert numDigits <= precision;
        if (usingBytes) {
            int i = precision - 1;
            for (; i >= precision - numDigits; i--) {
                bcdBytes[i] = 0;
            }
        } else {
            bcdLong &= (1L << ((precision - numDigits) * 4)) - 1;
        }
        precision -= numDigits;
    }

    @Override
    protected void setBcdToZero() {
        if (usingBytes) {
            bcdBytes = null;
            usingBytes = false;
        }
        bcdLong = 0L;
        scale = 0;
        precision = 0;
        isApproximate = false;
        origDouble = 0;
        origDelta = 0;
        exponent = 0;
    }

    @Override
    protected void readIntToBcd(int n) {
        assert n != 0;
        // ints always fit inside the long implementation.
        long result = 0L;
        int i = 16;
        for (; n != 0; n /= 10, i--) {
            result = (result >>> 4) + (((long) n % 10) << 60);
        }
        assert !usingBytes;
        bcdLong = result >>> (i * 4);
        scale = 0;
        precision = 16 - i;
    }

    @Override
    protected void readLongToBcd(long n) {
        assert n != 0;
        if (n >= 10000000000000000L) {
            ensureCapacity();
            int i = 0;
            for (; n != 0L; n /= 10L, i++) {
                bcdBytes[i] = (byte) (n % 10);
            }
            assert usingBytes;
            scale = 0;
            precision = i;
        } else {
            long result = 0L;
            int i = 16;
            for (; n != 0L; n /= 10L, i--) {
                result = (result >>> 4) + ((n % 10) << 60);
            }
            assert i >= 0;
            assert !usingBytes;
            bcdLong = result >>> (i * 4);
            scale = 0;
            precision = 16 - i;
        }
    }

    @Override
    protected void readBigIntegerToBcd(BigInteger n) {
        assert n.signum() != 0;
        ensureCapacity(); // allocate initial byte array
        int i = 0;
        for (; n.signum() != 0; i++) {
            BigInteger[] temp = n.divideAndRemainder(BigInteger.TEN);
            ensureCapacity(i + 1);
            bcdBytes[i] = temp[1].byteValue();
            n = temp[0];
        }
        scale = 0;
        precision = i;
    }

    @Override
    protected BigDecimal bcdToBigDecimal() {
        if (usingBytes) {
            // Converting to a string here is faster than doing BigInteger/BigDecimal arithmetic.
            BigDecimal result = new BigDecimal(toNumberString());
            if (isNegative()) {
                result = result.negate();
            }
            return result;
        } else {
            long tempLong = 0L;
            for (int shift = (precision - 1); shift >= 0; shift--) {
                tempLong = tempLong * 10 + getDigitPos(shift);
            }
            BigDecimal result = BigDecimal.valueOf(tempLong);
            // Test that the new scale fits inside the BigDecimal
            long newScale = result.scale() + scale + exponent;
            if (newScale <= Integer.MIN_VALUE) {
                result = BigDecimal.ZERO;
            } else {
                result = result.scaleByPowerOfTen(scale + exponent);
            }
            if (isNegative()) {
                result = result.negate();
            }
            return result;
        }
    }

    @Override
    protected void compact() {
        if (usingBytes) {
            int delta = 0;
            for (; delta < precision && bcdBytes[delta] == 0; delta++)
                ;
            if (delta == precision) {
                // Number is zero
                setBcdToZero();
                return;
            } else {
                // Remove trailing zeros
                shiftRight(delta);
            }

            // Compute precision
            int leading = precision - 1;
            for (; leading >= 0 && bcdBytes[leading] == 0; leading--)
                ;
            precision = leading + 1;

            // Switch storage mechanism if possible
            if (precision <= 16) {
                switchStorage();
            }

        } else {
            if (bcdLong == 0L) {
                // Number is zero
                setBcdToZero();
                return;
            }

            // Compact the number (remove trailing zeros)
            int delta = Long.numberOfTrailingZeros(bcdLong) / 4;
            bcdLong >>>= delta * 4;
            scale += delta;

            // Compute precision
            precision = 16 - (Long.numberOfLeadingZeros(bcdLong) / 4);
        }
    }

    /** Ensure that a byte array of at least 40 digits is allocated. */
    private void ensureCapacity() {
        ensureCapacity(40);
    }

    private void ensureCapacity(int capacity) {
        if (capacity == 0)
            return;
        int oldCapacity = usingBytes ? bcdBytes.length : 0;
        if (!usingBytes) {
            bcdBytes = new byte[capacity];
        } else if (oldCapacity < capacity) {
            byte[] bcd1 = new byte[capacity * 2];
            System.arraycopy(bcdBytes, 0, bcd1, 0, oldCapacity);
            bcdBytes = bcd1;
        }
        usingBytes = true;
    }

    /** Switches the internal storage mechanism between the 64-bit long and the byte array. */
    private void switchStorage() {
        if (usingBytes) {
            // Change from bytes to long
            bcdLong = 0L;
            for (int i = precision - 1; i >= 0; i--) {
                bcdLong <<= 4;
                bcdLong |= bcdBytes[i];
            }
            bcdBytes = null;
            usingBytes = false;
        } else {
            // Change from long to bytes
            ensureCapacity();
            for (int i = 0; i < precision; i++) {
                bcdBytes[i] = (byte) (bcdLong & 0xf);
                bcdLong >>>= 4;
            }
            assert usingBytes;
        }
    }

    @Override
    protected void copyBcdFrom(DecimalQuantity _other) {
        DecimalQuantity_DualStorageBCD other = (DecimalQuantity_DualStorageBCD) _other;
        setBcdToZero();
        if (other.usingBytes) {
            ensureCapacity(other.precision);
            System.arraycopy(other.bcdBytes, 0, bcdBytes, 0, other.precision);
        } else {
            bcdLong = other.bcdLong;
        }
    }

    /**
     * Checks whether the bytes stored in this instance are all valid. For internal unit testing only.
     *
     * @return An error message if this instance is invalid, or null if this instance is healthy.
     * @internal
     * @deprecated This API is for ICU internal use only.
     */
    @Deprecated
    public String checkHealth() {
        if (usingBytes) {
            if (bcdLong != 0)
                return "Value in bcdLong but we are in byte mode";
            if (precision == 0)
                return "Zero precision but we are in byte mode";
            if (precision > bcdBytes.length)
                return "Precision exceeds length of byte array";
            if (getDigitPos(precision - 1) == 0)
                return "Most significant digit is zero in byte mode";
            if (getDigitPos(0) == 0)
                return "Least significant digit is zero in long mode";
            for (int i = 0; i < precision; i++) {
                if (getDigitPos(i) >= 10)
                    return "Digit exceeding 10 in byte array";
                if (getDigitPos(i) < 0)
                    return "Digit below 0 in byte array";
            }
            for (int i = precision; i < bcdBytes.length; i++) {
                if (getDigitPos(i) != 0)
                    return "Nonzero digits outside of range in byte array";
            }
        } else {
            if (bcdBytes != null) {
                for (int i = 0; i < bcdBytes.length; i++) {
                    if (bcdBytes[i] != 0)
                        return "Nonzero digits in byte array but we are in long mode";
                }
            }
            if (precision == 0 && bcdLong != 0)
                return "Value in bcdLong even though precision is zero";
            if (precision > 16)
                return "Precision exceeds length of long";
            if (precision != 0 && getDigitPos(precision - 1) == 0)
                return "Most significant digit is zero in long mode";
            if (precision != 0 && getDigitPos(0) == 0)
                return "Least significant digit is zero in long mode";
            for (int i = 0; i < precision; i++) {
                if (getDigitPos(i) >= 10)
                    return "Digit exceeding 10 in long";
                if (getDigitPos(i) < 0)
                    return "Digit below 0 in long (?!)";
            }
            for (int i = precision; i < 16; i++) {
                if (getDigitPos(i) != 0)
                    return "Nonzero digits outside of range in long";
            }
        }

        return null;
    }

    /**
     * Checks whether this {@link DecimalQuantity_DualStorageBCD} is using its internal byte array
     * storage mechanism.
     *
     * @return true if an internal byte array is being used; false if a long is being used.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public boolean isUsingBytes() {
        return usingBytes;
    }

    @Override
    public String toString() {
        return String.format("<DecimalQuantity %d:%d %s %s%s>",
                lReqPos,
                rReqPos,
                (usingBytes ? "bytes" : "long"),
                (isNegative() ? "-" : ""),
                toNumberString());
    }

    private String toNumberString() {
        StringBuilder sb = new StringBuilder();
        if (usingBytes) {
            if (precision == 0) {
                sb.append('0');
            }
            for (int i = precision - 1; i >= 0; i--) {
                sb.append(bcdBytes[i]);
            }
        } else {
            sb.append(Long.toHexString(bcdLong));
        }
        sb.append("E");
        sb.append(scale);
        return sb.toString();
    }
}
