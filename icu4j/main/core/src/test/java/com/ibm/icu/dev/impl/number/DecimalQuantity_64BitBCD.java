// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.impl.number;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_AbstractBCD;

public final class DecimalQuantity_64BitBCD extends DecimalQuantity_AbstractBCD {

  /**
   * The BCD of the 16 digits of the number represented by this object. Every 4 bits of the long map
   * to one digit. For example, the number "12345" in BCD is "0x12345".
   *
   * <p>Whenever bcd changes internally, {@link #compact()} must be called, except in special cases
   * like setting the digit to zero.
   */
  private long bcd;

  @Override
  public int maxRepresentableDigits() {
    return 16;
  }

  public DecimalQuantity_64BitBCD(long input) {
    setToLong(input);
  }

  public DecimalQuantity_64BitBCD(int input) {
    setToInt(input);
  }

  public DecimalQuantity_64BitBCD(double input) {
    setToDouble(input);
  }

  public DecimalQuantity_64BitBCD(BigInteger input) {
    setToBigInteger(input);
  }

  public DecimalQuantity_64BitBCD(BigDecimal input) {
    setToBigDecimal(input);
  }

  public DecimalQuantity_64BitBCD(DecimalQuantity_64BitBCD other) {
    copyFrom(other);
  }

  @Override
  public DecimalQuantity createCopy() {
    return new DecimalQuantity_64BitBCD(this);
  }

  @Override
  protected byte getDigitPos(int position) {
    if (position < 0 || position >= 16) return 0;
    return (byte) ((bcd >>> (position * 4)) & 0xf);
  }

  @Override
  protected void setDigitPos(int position, byte value) {
    assert position >= 0 && position < 16;
    int shift = position * 4;
    bcd = bcd & ~(0xfL << shift) | ((long) value << shift);
  }

  @Override
  protected void shiftLeft(int numDigits) {
    assert precision + numDigits <= 16;
    bcd <<= (numDigits * 4);
    scale -= numDigits;
    precision += numDigits;
  }

  @Override
  protected void shiftRight(int numDigits) {
    bcd >>>= (numDigits * 4);
    scale += numDigits;
    precision -= numDigits;
  }

  @Override
  protected void popFromLeft(int numDigits) {
      bcd &= (1L << ((precision - numDigits) * 4)) - 1;
      precision -= numDigits;
  }

  @Override
  protected void setBcdToZero() {
    bcd = 0L;
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
    long result = 0L;
    int i = 16;
    for (; n != 0; n /= 10, i--) {
      result = (result >>> 4) + (((long) n % 10) << 60);
    }
    // ints can't overflow the 16 digits in the BCD, so scale is always zero
    bcd = result >>> (i * 4);
    scale = 0;
    precision = 16 - i;
  }

  @Override
  protected void readLongToBcd(long n) {
    assert n != 0;
    long result = 0L;
    int i = 16;
    for (; n != 0L; n /= 10L, i--) {
      result = (result >>> 4) + ((n % 10) << 60);
    }
    int adjustment = (i > 0) ? i : 0;
    bcd = result >>> (adjustment * 4);
    scale = (i < 0) ? -i : 0;
    precision = 16 - i;
  }

  @Override
  protected void readBigIntegerToBcd(BigInteger n) {
    assert n.signum() != 0;
    long result = 0L;
    int i = 16;
    for (; n.signum() != 0; i--) {
      BigInteger[] temp = n.divideAndRemainder(BigInteger.TEN);
      result = (result >>> 4) + (temp[1].longValue() << 60);
      n = temp[0];
    }
    int adjustment = (i > 0) ? i : 0;
    bcd = result >>> (adjustment * 4);
    scale = (i < 0) ? -i : 0;
  }

  @Override
  protected BigDecimal bcdToBigDecimal() {
    long tempLong = 0L;
    for (int shift = (precision - 1); shift >= 0; shift--) {
      tempLong = tempLong * 10 + getDigitPos(shift);
    }
    BigDecimal result = BigDecimal.valueOf(tempLong);
    result = result.scaleByPowerOfTen(scale);
    if (isNegative()) result = result.negate();
    return result;
  }

  @Override
  protected void compact() {
    // Special handling for 0
    if (bcd == 0L) {
      scale = 0;
      precision = 0;
      return;
    }

    // Compact the number (remove trailing zeros)
    int delta = Long.numberOfTrailingZeros(bcd) / 4;
    bcd >>>= delta * 4;
    scale += delta;

    // Compute precision
    precision = 16 - (Long.numberOfLeadingZeros(bcd) / 4);
  }

  @Override
  protected void copyBcdFrom(DecimalQuantity _other) {
    DecimalQuantity_64BitBCD other = (DecimalQuantity_64BitBCD) _other;
    bcd = other.bcd;
  }

  @Override
  public String toString() {
    return String.format(
        "<DecimalQuantity2 %d:%d %016XE%d>",
        lReqPos,
        rReqPos,
        bcd,
        scale);
  }
}
