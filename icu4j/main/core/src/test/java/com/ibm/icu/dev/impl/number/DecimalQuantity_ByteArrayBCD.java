// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.impl.number;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_AbstractBCD;

public final class DecimalQuantity_ByteArrayBCD extends DecimalQuantity_AbstractBCD {

  /**
   * The BCD of the 16 digits of the number represented by this object. Every 4 bits of the long map
   * to one digit. For example, the number "12345" in BCD is "0x12345".
   *
   * <p>Whenever bcd changes internally, {@link #compact()} must be called, except in special cases
   * like setting the digit to zero.
   */
  private byte[] bcd = new byte[100];

  @Override
  public int maxRepresentableDigits() {
    return Integer.MAX_VALUE;
  }

  public DecimalQuantity_ByteArrayBCD(long input) {
    setToLong(input);
  }

  public DecimalQuantity_ByteArrayBCD(int input) {
    setToInt(input);
  }

  public DecimalQuantity_ByteArrayBCD(double input) {
    setToDouble(input);
  }

  public DecimalQuantity_ByteArrayBCD(BigInteger input) {
    setToBigInteger(input);
  }

  public DecimalQuantity_ByteArrayBCD(BigDecimal input) {
    setToBigDecimal(input);
  }

  public DecimalQuantity_ByteArrayBCD(DecimalQuantity_ByteArrayBCD other) {
    copyFrom(other);
  }

  @Override
  public DecimalQuantity createCopy() {
    return new DecimalQuantity_ByteArrayBCD(this);
  }

  @Override
  protected byte getDigitPos(int position) {
    if (position < 0 || position > precision) return 0;
    return bcd[position];
  }

  @Override
  protected void setDigitPos(int position, byte value) {
    assert position >= 0;
    ensureCapacity(position + 1);
    bcd[position] = value;
  }

  @Override
  protected void shiftLeft(int numDigits) {
    ensureCapacity(precision + numDigits);
    int i = precision + numDigits - 1;
    for (; i >= numDigits; i--) {
      bcd[i] = bcd[i - numDigits];
    }
    for (; i >= 0; i--) {
      bcd[i] = 0;
    }
    scale -= numDigits;
    precision += numDigits;
  }

  @Override
  protected void shiftRight(int numDigits) {
    int i = 0;
    for (; i < precision - numDigits; i++) {
      bcd[i] = bcd[i + numDigits];
    }
    for (; i < precision; i++) {
      bcd[i] = 0;
    }
    scale += numDigits;
    precision -= numDigits;
  }

  @Override
  protected void popFromLeft(int numDigits) {
    int i = precision - 1;
    for (; i >= precision - numDigits; i--) {
      bcd[i] = 0;
    }
    precision -= numDigits;
  }

  @Override
  protected void setBcdToZero() {
    for (int i = 0; i < precision; i++) {
      bcd[i] = (byte) 0;
    }
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
    int i = 0;
    for (; n != 0L; n /= 10L, i++) {
      bcd[i] = (byte) (n % 10);
    }
    scale = 0;
    precision = i;
  }

  private static final byte[] LONG_MIN_VALUE =
      new byte[] {8, 0, 8, 5, 7, 7, 4, 5, 8, 6, 3, 0, 2, 7, 3, 3, 2, 2, 9};

  @Override
  protected void readLongToBcd(long n) {
    assert n != 0;
    if (n == Long.MIN_VALUE) {
      // Can't consume via the normal path.
      System.arraycopy(LONG_MIN_VALUE, 0, bcd, 0, LONG_MIN_VALUE.length);
      scale = 0;
      precision = LONG_MIN_VALUE.length;
      return;
    }
    int i = 0;
    for (; n != 0L; n /= 10L, i++) {
      bcd[i] = (byte) (n % 10);
    }
    scale = 0;
    precision = i;
  }

  @Override
  protected void readBigIntegerToBcd(BigInteger n) {
    assert n.signum() != 0;
    int i = 0;
    for (; n.signum() != 0; i++) {
      BigInteger[] temp = n.divideAndRemainder(BigInteger.TEN);
      ensureCapacity(i + 1);
      bcd[i] = temp[1].byteValue();
      n = temp[0];
    }
    scale = 0;
    precision = i;
  }

  @Override
  protected BigDecimal bcdToBigDecimal() {
    // Converting to a string here is faster than doing BigInteger/BigDecimal arithmetic.
    return new BigDecimal(toDumbString());
  }

  private String toDumbString() {
    StringBuilder sb = new StringBuilder();
    if (isNegative()) sb.append('-');
    if (precision == 0) {
      sb.append('0');
      return sb.toString();
    }
    for (int i = precision - 1; i >= 0; i--) {
      sb.append(getDigitPos(i));
    }
    if (scale != 0) {
      sb.append('E');
      sb.append(scale);
    }
    return sb.toString();
  }

  @Override
  protected void compact() {
    // Special handling for 0
    boolean isZero = true;
    for (int i = 0; i < precision; i++) {
      if (bcd[i] != 0) {
        isZero = false;
        break;
      }
    }
    if (isZero) {
      scale = 0;
      precision = 0;
      return;
    }

    // Compact the number (remove trailing zeros)
    int delta = 0;
    for (; bcd[delta] == 0; delta++) ;
    shiftRight(delta);

    // Compute precision
    int leading = precision - 1;
    for (; leading >= 0 && bcd[leading] == 0; leading--) ;
    precision = leading + 1;
  }

  private void ensureCapacity(int capacity) {
    if (bcd.length >= capacity) return;
    byte[] bcd1 = new byte[capacity * 2];
    System.arraycopy(bcd, 0, bcd1, 0, bcd.length);
    bcd = bcd1;
  }

  @Override
  protected void copyBcdFrom(DecimalQuantity _other) {
    DecimalQuantity_ByteArrayBCD other = (DecimalQuantity_ByteArrayBCD) _other;
    System.arraycopy(other.bcd, 0, bcd, 0, bcd.length);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 30; i >= 0; i--) {
      sb.append(bcd[i]);
    }
    return String.format(
        "<DecimalQuantity3 %d:%d %s%s%d>",
        lReqPos,
        rReqPos,
        sb,
        "E",
        scale);
  }
}
