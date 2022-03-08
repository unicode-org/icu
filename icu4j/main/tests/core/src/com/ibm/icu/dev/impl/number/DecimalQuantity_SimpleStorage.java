// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.impl.number;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.FieldPosition;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.Modifier.Signum;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.text.UFieldPosition;

/**
 * This is an older implementation of DecimalQuantity. A newer, faster implementation is
 * DecimalQuantity2. I kept this implementation around because it was useful for testing purposes
 * (being able to compare the output of one implementation with the other).
 *
 * <p>This class is NOT IMMUTABLE and NOT THREAD SAFE and is intended to be used by a single thread
 * to format a number through a formatter, which is thread-safe.
 */
public class DecimalQuantity_SimpleStorage implements DecimalQuantity {
  // Four positions: left optional '(', left required '[', right required ']', right optional ')'.
  // These four positions determine which digits are displayed in the output string.  They do NOT
  // affect rounding.  These positions are internal-only and can be specified only by the public
  // endpoints like setFractionLength, setIntegerLength, and setSignificantDigits, among others.
  //
  //   * Digits between lReqPos and rReqPos are in the "required zone" and are always displayed.
  //   * Digits between lOptPos and rOptPos but outside the required zone are in the "optional zone"
  //     and are displayed unless they are trailing off the left or right edge of the number and
  //     have a numerical value of zero.  In order to be "trailing", the digits need to be beyond
  //     the decimal point in their respective directions.
  //   * Digits outside of the "optional zone" are never displayed.
  //
  // See the table below for illustrative examples.
  //
  // +---------+---------+---------+---------+------------+------------------------+--------------+
  // | lOptPos | lReqPos | rReqPos | rOptPos |   number   |        positions       | en-US string |
  // +---------+---------+---------+---------+------------+------------------------+--------------+
  // |    5    |    2    |   -1    |   -5    |   1234.567 |     ( 12[34.5]67  )    |   1,234.567  |
  // |    3    |    2    |   -1    |   -5    |   1234.567 |      1(2[34.5]67  )    |     234.567  |
  // |    3    |    2    |   -1    |   -2    |   1234.567 |      1(2[34.5]6)7      |     234.56   |
  // |    6    |    4    |    2    |   -5    | 123456789. |  123(45[67]89.     )   | 456,789.     |
  // |    6    |    4    |    2    |    1    | 123456789. |     123(45[67]8)9.     | 456,780.     |
  // |   -1    |   -1    |   -3    |   -4    | 0.123456   |     0.1([23]4)56       |        .0234 |
  // |    6    |    4    |   -2    |   -2    |     12.3   |     (  [  12.3 ])      |    0012.30   |
  // +---------+---------+---------+---------+------------+------------------------+--------------+
  //
  private int lOptPos = Integer.MAX_VALUE;
  private int lReqPos = 0;
  private int rReqPos = 0;
  private int rOptPos = Integer.MIN_VALUE;

  // Internally, attempt to use a long to store the number. A long can hold numbers between 18 and
  // 19 digits, covering the vast majority of use cases. We store three values: the long itself,
  // the "scale" of the long (the power of 10 represented by the rightmost digit in the long), and
  // the "precision" (the number of digits in the long). "primary" and "primaryScale" are the only
  // two variables that are required for representing the number in memory. "primaryPrecision" is
  // saved only for the sake of performance enhancements when performing certain operations. It can
  // always be re-computed from "primary" and "primaryScale".
  private long primary;
  private int primaryScale;
  private int primaryPrecision;

  // If the decimal can't fit into the long, fall back to a BigDecimal.
  private BigDecimal fallback;

  // Other properties
  private int flags;
  private static final int NEGATIVE_FLAG = 1;
  private static final int INFINITY_FLAG = 2;
  private static final int NAN_FLAG = 4;
  private static final long[] POWERS_OF_TEN = {
    1L,
    10L,
    100L,
    1000L,
    10000L,
    100000L,
    1000000L,
    10000000L,
    100000000L,
    1000000000L,
    10000000000L,
    100000000000L,
    1000000000000L,
    10000000000000L,
    100000000000000L,
    1000000000000000L,
    10000000000000000L,
    100000000000000000L,
    1000000000000000000L
  };

  private int origPrimaryScale;

  @Override
  public int maxRepresentableDigits() {
    return Integer.MAX_VALUE;
  }

  public DecimalQuantity_SimpleStorage(long input) {
    if (input < 0) {
      setNegative(true);
      input *= -1;
    }

    primary = input;
    primaryScale = 0;
    primaryPrecision = computePrecision(primary);
    fallback = null;
    origPrimaryScale = primaryScale;
  }

  /**
   * Creates a DecimalQuantity from the given double value. Internally attempts several strategies
   * for converting the double to an exact representation, falling back on a BigDecimal if it fails
   * to do so.
   *
   * @param input The double to represent by this DecimalQuantity.
   */
  public DecimalQuantity_SimpleStorage(double input) {
    if (input < 0) {
      setNegative(true);
      input *= -1;
    }

    // First try reading from IEEE bits. This is trivial only for doubles in [2^52, 2^64). If it
    // fails, we wasted only a few CPU cycles.
    long ieeeBits = Double.doubleToLongBits(input);
    int exponent = (int) ((ieeeBits & 0x7ff0000000000000L) >> 52) - 0x3ff;
    if (exponent >= 52 && exponent <= 63) {
      // We can convert this double directly to a long.
      long mantissa = (ieeeBits & 0x000fffffffffffffL) + 0x0010000000000000L;
      primary = (mantissa << (exponent - 52));
      primaryScale = 0;
      primaryPrecision = computePrecision(primary);
      return;
    }

    // Now try parsing the string produced by Double.toString().
    String temp = Double.toString(input);
    try {
      if (temp.length() == 3 && temp.equals("0.0")) {
        // Case 1: Zero.
        primary = 0L;
        primaryScale = 0;
        primaryPrecision = 0;
      } else if (temp.indexOf('E') != -1) {
        // Case 2: Exponential notation.
        assert temp.indexOf('.') == 1;
        int expPos = temp.indexOf('E');
        primary = Long.parseLong(temp.charAt(0) + temp.substring(2, expPos));
        primaryScale = Integer.parseInt(temp.substring(expPos + 1)) - (expPos - 1) + 1;
        primaryPrecision = expPos - 1;
      } else if (temp.charAt(0) == '0') {
        // Case 3: Fraction-only number.
        assert temp.indexOf('.') == 1;
        primary = Long.parseLong(temp.substring(2)); // ignores leading zeros
        primaryScale = 2 - temp.length();
        primaryPrecision = computePrecision(primary);
      } else if (temp.charAt(temp.length() - 1) == '0') {
        // Case 4: Integer-only number.
        assert temp.indexOf('.') == temp.length() - 2;
        int rightmostNonzeroDigitIndex = temp.length() - 3;
        while (temp.charAt(rightmostNonzeroDigitIndex) == '0') {
          rightmostNonzeroDigitIndex -= 1;
        }
        primary = Long.parseLong(temp.substring(0, rightmostNonzeroDigitIndex + 1));
        primaryScale = temp.length() - rightmostNonzeroDigitIndex - 3;
        primaryPrecision = rightmostNonzeroDigitIndex + 1;
      } else if (temp.equals("Infinity")) {
        // Case 5: Infinity.
        primary = 0;
        setInfinity(true);
      } else if (temp.equals("NaN")) {
        // Case 6: NaN.
        primary = 0;
        setNaN(true);
      } else {
        // Case 7: Number with both a fraction and an integer.
        int decimalPos = temp.indexOf('.');
        primary = Long.parseLong(temp.substring(0, decimalPos) + temp.substring(decimalPos + 1));
        primaryScale = decimalPos - temp.length() + 1;
        primaryPrecision = temp.length() - 1;
      }
    } catch (NumberFormatException e) {
      // The digits of the double can't fit into the long.
      primary = -1;
      fallback = new BigDecimal(temp);
    }

    origPrimaryScale = primaryScale;
  }

  static final double LOG_2_OF_TEN = 3.32192809489;

  public DecimalQuantity_SimpleStorage(double input, boolean fast) {
    if (input < 0) {
      setNegative(true);
      input *= -1;
    }

    // Our strategy is to read all digits that are *guaranteed* to be valid without delving into
    // the IEEE rounding rules.  This strategy might not end up with a perfect representation of
    // the fractional part of the double.
    long ieeeBits = Double.doubleToLongBits(input);
    int exponent = (int) ((ieeeBits & 0x7ff0000000000000L) >> 52) - 0x3ff;
    long mantissa = (ieeeBits & 0x000fffffffffffffL) + 0x0010000000000000L;
    if (exponent > 63) {
      throw new IllegalArgumentException(); // FIXME
    } else if (exponent >= 52) {
      primary = (mantissa << (exponent - 52));
      primaryScale = 0;
      primaryPrecision = computePrecision(primary);
      return;
    } else if (exponent >= 0) {
      int shift = 52 - exponent;
      primary = (mantissa >> shift); // integer part
      int fractionCount = (int) (shift / LOG_2_OF_TEN);
      long fraction = (mantissa - (primary << shift)) + 1L; // TODO: Explain the +1L
      primary *= POWERS_OF_TEN[fractionCount];
      for (int i = 0; i < fractionCount; i++) {
        long times10 = (fraction * 10L);
        long digit = times10 >> shift;
        assert digit >= 0 && digit < 10;
        primary += digit * POWERS_OF_TEN[fractionCount - i - 1];
        fraction = times10 & ((1L << shift) - 1);
      }
      primaryScale = -fractionCount;
      primaryPrecision = computePrecision(primary);
    } else {
      throw new IllegalArgumentException(); // FIXME
    }
  }

  public DecimalQuantity_SimpleStorage(BigDecimal decimal) {
    setToBigDecimal(decimal);
  }

  public DecimalQuantity_SimpleStorage(DecimalQuantity_SimpleStorage other) {
    copyFrom(other);
  }

  @Override
  public void setToBigDecimal(BigDecimal decimal) {
    if (decimal.compareTo(BigDecimal.ZERO) < 0) {
      setNegative(true);
      decimal = decimal.negate();
    }

    primary = -1;
    if (decimal.compareTo(BigDecimal.ZERO) == 0) {
      fallback = BigDecimal.ZERO;
    } else {
      fallback = decimal;
    }
  }

  @Override
  public DecimalQuantity_SimpleStorage createCopy() {
    return new DecimalQuantity_SimpleStorage(this);
  }

  /**
   * Make the internal state of this DecimalQuantity equal to another DecimalQuantity.
   *
   * @param other The template DecimalQuantity. All properties from this DecimalQuantity will be
   *     copied into this DecimalQuantity.
   */
  @Override
  public void copyFrom(DecimalQuantity other) {
    // TODO: Check before casting
    DecimalQuantity_SimpleStorage _other = (DecimalQuantity_SimpleStorage) other;
    lOptPos = _other.lOptPos;
    lReqPos = _other.lReqPos;
    rReqPos = _other.rReqPos;
    rOptPos = _other.rOptPos;
    primary = _other.primary;
    primaryScale = _other.primaryScale;
    primaryPrecision = _other.primaryPrecision;
    fallback = _other.fallback;
    flags = _other.flags;
    origPrimaryScale = _other.origPrimaryScale;
  }

  @Override
  public long getPositionFingerprint() {
    long fingerprint = 0;
    fingerprint ^= lOptPos;
    fingerprint ^= (lReqPos << 16);
    fingerprint ^= ((long) rReqPos << 32);
    fingerprint ^= ((long) rOptPos << 48);
    return fingerprint;
  }

  /**
   * Utility method to compute the number of digits ("precision") in a long.
   *
   * @param input The long (which can't contain more than 19 digits).
   * @return The precision of the long.
   */
  private static int computePrecision(long input) {
    int precision = 0;
    while (input > 0) {
      input /= 10;
      precision++;
    }
    return precision;
  }

  /**
   * Changes the internal representation from a long to a BigDecimal. Used only for operations that
   * don't support longs.
   */
  private void convertToBigDecimal() {
    if (primary == -1) {
      return;
    }

    fallback = new BigDecimal(primary).scaleByPowerOfTen(primaryScale);
    primary = -1;
  }

  @Override
  public long toLong(boolean truncateIfOverflow) {
    BigDecimal temp = toBigDecimal().setScale(0, RoundingMode.FLOOR);
    if (truncateIfOverflow) {
      return temp.longValue();
    } else {
      return temp.longValueExact();
    }
  }

  @Override
  public void setMinInteger(int minInt) {
    // Graceful failures for bogus input
    minInt = Math.max(0, minInt);

    // Save values into internal state
    // Negation is safe for minFrac/maxFrac because -Integer.MAX_VALUE > Integer.MIN_VALUE
    lReqPos = minInt;
  }

  @Override
  public void setMinFraction(int minFrac) {
    // Graceful failures for bogus input
    minFrac = Math.max(0, minFrac);

    // Save values into internal state
    // Negation is safe for minFrac/maxFrac because -Integer.MAX_VALUE > Integer.MIN_VALUE
    rReqPos = -minFrac;
  }

  @Override
  public void applyMaxInteger(int maxInt) {
    BigDecimal d;
    if (primary != -1) {
      d = BigDecimal.valueOf(primary).scaleByPowerOfTen(primaryScale);
    } else {
      d = fallback;
    }
    d = d.scaleByPowerOfTen(-maxInt).remainder(BigDecimal.ONE).scaleByPowerOfTen(maxInt);
    if (primary != -1) {
      primary = d.scaleByPowerOfTen(-primaryScale).longValueExact();
    } else {
      fallback = d;
    }
  }

  @Override
  public void roundToIncrement(BigDecimal roundingInterval, MathContext mathContext) {
    BigDecimal d =
        (primary == -1) ? fallback : new BigDecimal(primary).scaleByPowerOfTen(primaryScale);
    if (isNegative()) d = d.negate();
    d = d.divide(roundingInterval, 0, mathContext.getRoundingMode()).multiply(roundingInterval);
    if (isNegative()) d = d.negate();
    fallback = d;
    primary = -1;
  }

  @Override
  public void roundToNickel(int roundingMagnitude, MathContext mathContext) {
    BigDecimal nickel = BigDecimal.valueOf(5).scaleByPowerOfTen(roundingMagnitude);
    roundToIncrement(nickel, mathContext);
  }

  @Override
  public void roundToMagnitude(int roundingMagnitude, MathContext mathContext) {
    if (roundingMagnitude < -1000) {
      roundToInfinity();
      return;
    }
    if (primary == -1) {
      if (isNegative()) fallback = fallback.negate();
      fallback = fallback.setScale(-roundingMagnitude, mathContext.getRoundingMode());
      if (isNegative()) fallback = fallback.negate();
      // Enforce the math context.
      fallback = fallback.round(mathContext);
    } else {
      int relativeScale = primaryScale - roundingMagnitude;
      if (relativeScale < -18) {
        // No digits will remain after rounding the number.
        primary = 0L;
        primaryScale = roundingMagnitude;
        primaryPrecision = 0;
      } else if (relativeScale < 0) {
        // This is the harder case, when we need to perform the rounding logic.
        // First check if the rightmost digits are already zero, where we can skip rounding.
        if ((primary % POWERS_OF_TEN[0 - relativeScale]) == 0) {
          // No rounding is necessary.
        } else {
          // TODO: Make this more efficient. Temporarily, convert to a BigDecimal and back again.
          BigDecimal temp = new BigDecimal(primary).scaleByPowerOfTen(primaryScale);
          if (isNegative()) temp = temp.negate();
          temp = temp.setScale(-roundingMagnitude, mathContext.getRoundingMode());
          if (isNegative()) temp = temp.negate();
          temp = temp.scaleByPowerOfTen(-roundingMagnitude);
          primary = temp.longValueExact(); // should never throw
          primaryScale = roundingMagnitude;
          primaryPrecision = computePrecision(primary);
        }
      } else {
        // No rounding is necessary. All digits are to the left of the rounding magnitude.
      }
      // Enforce the math context.
      primary = new BigDecimal(primary).round(mathContext).longValueExact();
      primaryPrecision = computePrecision(primary);
    }
  }

  @Override
  public void roundToInfinity() {
    // noop
  }

  /**
   * Multiply the internal number by the specified multiplicand. This method forces the internal
   * representation into a BigDecimal. If you are multiplying by a power of 10, use {@link
   * #adjustMagnitude} instead.
   *
   * @param multiplicand The number to be passed to {@link BigDecimal#multiply}.
   */
  @Override
  public void multiplyBy(BigDecimal multiplicand) {
    convertToBigDecimal();
    fallback = fallback.multiply(multiplicand);
    if (fallback.compareTo(BigDecimal.ZERO) < 0) {
      setNegative(!isNegative());
      fallback = fallback.negate();
    }
  }

  @Override
  public void negate() {
    flags ^= NEGATIVE_FLAG;
  }

  /**
   * Divide the internal number by the specified quotient. This method forces the internal
   * representation into a BigDecimal. If you are dividing by a power of 10, use {@link
   * #adjustMagnitude} instead.
   *
   * @param divisor The number to be passed to {@link BigDecimal#divide}.
   * @param scale The scale of the final rounded number. More negative means more decimal places.
   * @param mathContext The math context to use if rounding is necessary.
   */
  @SuppressWarnings("unused")
  private void divideBy(BigDecimal divisor, int scale, MathContext mathContext) {
    convertToBigDecimal();
    // Negate the scale because BigDecimal's scale is defined as the inverse of our scale
    fallback = fallback.divide(divisor, -scale, mathContext.getRoundingMode());
    if (fallback.compareTo(BigDecimal.ZERO) < 0) {
      setNegative(!isNegative());
      fallback = fallback.negate();
    }
  }

  @Override
  public boolean isZeroish() {
    if (primary == -1) {
      return fallback.compareTo(BigDecimal.ZERO) == 0;
    } else {
      return primary == 0;
    }
  }

  /** @return The power of ten of the highest digit represented by this DecimalQuantity */
  @Override
  public int getMagnitude() throws ArithmeticException {
    int scale = (primary == -1) ? scaleBigDecimal(fallback) : primaryScale;
    int precision = (primary == -1) ? precisionBigDecimal(fallback) : primaryPrecision;
    if (precision == 0) {
      throw new ArithmeticException("Magnitude is not well-defined for zero");
    } else {
      return scale + precision - 1;
    }
  }

  /**
   * Changes the magnitude of this DecimalQuantity. If the indices of the represented digits had been
   * previously specified, those indices are moved relative to the DecimalQuantity.
   *
   * <p>This method does NOT perform rounding.
   *
   * @param delta The number of powers of ten to shift (positive shifts to the left).
   */
  @Override
  public void adjustMagnitude(int delta) {
    if (primary == -1) {
      fallback = fallback.scaleByPowerOfTen(delta);
    } else {
      primaryScale = addOrMaxValue(primaryScale, delta);
    }
  }

  private static int addOrMaxValue(int a, int b) {
    // Check for overflow, and return min/max value if overflow occurs.
    if (b < 0 && a + b > a) {
      return Integer.MIN_VALUE;
    } else if (b > 0 && a + b < a) {
      return Integer.MAX_VALUE;
    }
    return a + b;
  }

  /** @return If the number represented by this DecimalQuantity is less than zero */
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

  private void setNegative(boolean isNegative) {
    flags = (flags & (~NEGATIVE_FLAG)) | (isNegative ? NEGATIVE_FLAG : 0);
  }

  @Override
  public boolean isInfinite() {
    return (flags & INFINITY_FLAG) != 0;
  }

  private void setInfinity(boolean isInfinity) {
    flags = (flags & (~INFINITY_FLAG)) | (isInfinity ? INFINITY_FLAG : 0);
  }

  @Override
  public boolean isNaN() {
    return (flags & NAN_FLAG) != 0;
  }

  private void setNaN(boolean isNaN) {
    flags = (flags & (~NAN_FLAG)) | (isNaN ? NAN_FLAG : 0);
  }

  /**
   * Returns a representation of this DecimalQuantity as a double, with possible loss of information.
   */
  @Override
  public double toDouble() {
    double result;
    if (primary == -1) {
      result = fallback.doubleValue();
    } else {
      // TODO: Make this more efficient
      result = primary;
      for (int i = 0; i < primaryScale; i++) {
        result *= 10.;
      }
      for (int i = 0; i > primaryScale; i--) {
        result /= 10.;
      }
    }
    return isNegative() ? -result : result;
  }

  @Override
  public BigDecimal toBigDecimal() {
    BigDecimal result;
    if (primary != -1) {
      result = new BigDecimal(primary).scaleByPowerOfTen(primaryScale);
    } else {
      result = fallback;
    }
    return isNegative() ? result.negate() : result;
  }

  @Override
  public StandardPlural getStandardPlural(PluralRules rules) {
    if (rules == null) {
      // Fail gracefully if the user didn't provide a PluralRules
      return StandardPlural.OTHER;
    } else {
      // TODO: Avoid converting to a double for the sake of PluralRules
      String ruleString = rules.select(toDouble());
      return StandardPlural.orOtherFromString(ruleString);
    }
  }

  @Override
  public double getPluralOperand(Operand operand) {
    // TODO: This is a temporary hack.
    return new PluralRules.FixedDecimal(toDouble()).getPluralOperand(operand);
  }

  public boolean hasNextFraction() {
    if (rReqPos < 0) {
      // We are in the required zone.
      return true;
    } else if (rOptPos >= 0) {
      // We are in the forbidden zone.
      return false;
    } else {
      // We are in the optional zone.
      if (primary == -1) {
        return fallback.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) > 0;
      } else {
        if (primaryScale <= -19) {
          // The number is a fraction so small that it consists of only fraction digits.
          return primary > 0;
        } else if (primaryScale < 0) {
          // Check if we have a fraction part.
          long factor = POWERS_OF_TEN[0 - primaryScale];
          return ((primary % factor) != 0);
        } else {
          // The lowest digit in the long has magnitude greater than -1.
          return false;
        }
      }
    }
  }

  public byte nextFraction() {
    byte returnValue;
    if (primary == -1) {
      BigDecimal temp = fallback.multiply(BigDecimal.TEN);
      returnValue = temp.setScale(0, RoundingMode.FLOOR).remainder(BigDecimal.TEN).byteValue();
      fallback = fallback.setScale(0, RoundingMode.FLOOR).add(temp.remainder(BigDecimal.ONE));
    } else {
      if (primaryScale <= -20) {
        // The number is a fraction so small that it has no first fraction digit.
        primaryScale += 1;
        returnValue = 0;
      } else if (primaryScale < 0) {
        // Extract the fraction digit out of the middle of the long.
        long factor = POWERS_OF_TEN[0 - primaryScale - 1];
        long temp1 = primary / factor;
        long temp2 = primary % factor;
        returnValue = (byte) (temp1 % 10); // not necessarily nonzero
        primary = ((temp1 / 10) * factor) + temp2;
        primaryScale += 1;
        if (temp1 != 0) {
          primaryPrecision -= 1;
        }
      } else {
        // The lowest digit in the long has magnitude greater than -1.
        returnValue = 0;
      }
    }

    // Update digit brackets
    if (lOptPos < 0) {
      lOptPos += 1;
    }
    if (lReqPos < 0) {
      lReqPos += 1;
    }
    if (rReqPos < 0) {
      rReqPos += 1;
    }
    if (rOptPos < 0) {
      rOptPos += 1;
    }

    assert returnValue >= 0;
    return returnValue;
  }

  public boolean hasNextInteger() {
    if (lReqPos > 0) {
      // We are in the required zone.
      return true;
    } else if (lOptPos <= 0) {
      // We are in the forbidden zone.
      return false;
    } else {
      // We are in the optional zone.
      if (primary == -1) {
        return fallback.setScale(0, RoundingMode.FLOOR).compareTo(BigDecimal.ZERO) > 0;
      } else {
        if (primaryScale < -18) {
          // The number is a fraction so small that it has no integer part.
          return false;
        } else if (primaryScale < 0) {
          // Check if we have an integer part.
          long factor = POWERS_OF_TEN[0 - primaryScale];
          return ((primary % factor) != primary); // equivalent: ((primary / 10) != 0)
        } else {
          // The lowest digit in the long has magnitude of at least 0.
          return primary != 0;
        }
      }
    }
  }

  private int integerCount() {
    int digitsRemaining;
    if (primary == -1) {
      digitsRemaining = precisionBigDecimal(fallback) + scaleBigDecimal(fallback);
    } else {
      digitsRemaining = primaryPrecision + primaryScale;
    }
    return Math.min(Math.max(digitsRemaining, lReqPos), lOptPos);
  }

  private int fractionCount() {
    // TODO: This is temporary.
    DecimalQuantity_SimpleStorage copy = new DecimalQuantity_SimpleStorage(this);
    int fractionCount = 0;
    while (copy.hasNextFraction()) {
      copy.nextFraction();
      fractionCount++;
    }
    return fractionCount;
  }

  @Override
  public int getUpperDisplayMagnitude() {
    return integerCount() - 1;
  }

  @Override
  public int getLowerDisplayMagnitude() {
    return -fractionCount();
  }

  //  @Override
  //  public byte getIntegerDigit(int index) {
  //    return getDigitPos(index);
  //  }
  //
  //  @Override
  //  public byte getFractionDigit(int index) {
  //    return getDigitPos(-index - 1);
  //  }

  @Override
  public byte getDigit(int magnitude) {
    // TODO: This is temporary.
    DecimalQuantity_SimpleStorage copy = new DecimalQuantity_SimpleStorage(this);
    if (magnitude < 0) {
      for (int p = -1; p > magnitude; p--) {
        copy.nextFraction();
      }
      return copy.nextFraction();
    } else {
      for (int p = 0; p < magnitude; p++) {
        copy.nextInteger();
      }
      return copy.nextInteger();
    }
  }

  public byte nextInteger() {
    byte returnValue;
    if (primary == -1) {
      returnValue = fallback.setScale(0, RoundingMode.FLOOR).remainder(BigDecimal.TEN).byteValue();
      BigDecimal temp = fallback.divide(BigDecimal.TEN).setScale(0, RoundingMode.FLOOR);
      fallback = fallback.remainder(BigDecimal.ONE).add(temp);
    } else {
      if (primaryScale < -18) {
        // The number is a fraction so small that it has no integer part.
        returnValue = 0;
      } else if (primaryScale < 0) {
        // Extract the integer digit out of the middle of the long. In many ways, this is the heart
        // of the digit iterator algorithm.
        long factor = POWERS_OF_TEN[0 - primaryScale];
        if ((primary % factor) != primary) { // equivalent: ((primary / 10) != 0)
          returnValue = (byte) ((primary / factor) % 10);
          long temp = (primary / 10);
          primary = temp - (temp % factor) + (primary % factor);
          primaryPrecision -= 1;
        } else {
          returnValue = 0;
        }
      } else if (primaryScale == 0) {
        // Fast-path for primaryScale == 0 (otherwise equivalent to previous step).
        if (primary != 0) {
          returnValue = (byte) (primary % 10);
          primary /= 10;
          primaryPrecision -= 1;
        } else {
          returnValue = 0;
        }
      } else {
        // The lowest digit in the long has magnitude greater than 0.
        primaryScale -= 1;
        returnValue = 0;
      }
    }

    // Update digit brackets
    if (lOptPos > 0) {
      lOptPos -= 1;
    }
    if (lReqPos > 0) {
      lReqPos -= 1;
    }
    if (rReqPos > 0) {
      rReqPos -= 1;
    }
    if (rOptPos > 0) {
      rOptPos -= 1;
    }

    assert returnValue >= 0;
    return returnValue;
  }

  /**
   * Helper method to compute the precision of a BigDecimal by our definition of precision, which is
   * that the number zero gets precision zero.
   *
   * @param decimal The BigDecimal whose precision to compute.
   * @return The precision by our definition.
   */
  private static int precisionBigDecimal(BigDecimal decimal) {
    if (decimal.compareTo(BigDecimal.ZERO) == 0) {
      return 0;
    } else {
      return decimal.precision();
    }
  }

  /**
   * Helper method to compute the scale of a BigDecimal by our definition of scale, which is that
   * deeper fractions result in negative scales as opposed to positive scales.
   *
   * @param decimal The BigDecimal whose scale to compute.
   * @return The scale by our definition.
   */
  private static int scaleBigDecimal(BigDecimal decimal) {
    return -decimal.scale();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<DecimalQuantity1 ");
    if (primary == -1) {
      sb.append(lOptPos > 1000 ? "max" : lOptPos);
      sb.append(":");
      sb.append(lReqPos);
      sb.append(":");
      sb.append(rReqPos);
      sb.append(":");
      sb.append(rOptPos < -1000 ? "min" : rOptPos);
      sb.append(" ");
      sb.append(fallback.toString());
    } else {
      String digits = Long.toString(primary);
      int iDec = digits.length() + primaryScale;
      int iLP = iDec - toRange(lOptPos, -1000, 1000);
      int iLB = iDec - toRange(lReqPos, -1000, 1000);
      int iRB = iDec - toRange(rReqPos, -1000, 1000);
      int iRP = iDec - toRange(rOptPos, -1000, 1000);
      iDec = Math.max(Math.min(iDec, digits.length() + 1), -1);
      iLP = Math.max(Math.min(iLP, digits.length() + 1), -1);
      iLB = Math.max(Math.min(iLB, digits.length() + 1), -1);
      iRB = Math.max(Math.min(iRB, digits.length() + 1), -1);
      iRP = Math.max(Math.min(iRP, digits.length() + 1), -1);

      for (int i = -1; i <= digits.length() + 1; i++) {
        if (i == iLP) sb.append('(');
        if (i == iLB) sb.append('[');
        if (i == iDec) sb.append('.');
        if (i == iRB) sb.append(']');
        if (i == iRP) sb.append(')');
        if (i >= 0 && i < digits.length()) sb.append(digits.charAt(i));
        else sb.append('\u00A0');
      }
    }
    sb.append(">");
    return sb.toString();
  }

  @Override
  public String toPlainString() {
      // NOTE: This logic is duplicated between here and DecimalQuantity_AbstractBCD.
      StringBuilder sb = new StringBuilder();
      if (isNegative()) {
          sb.append('-');
      }
      int upper = getUpperDisplayMagnitude();
      int lower = getLowerDisplayMagnitude();
      int p = upper;
      for (; p >= 0; p--) {
          sb.append((char) ('0' + getDigit(p)));
      }
      if (lower < 0) {
          sb.append('.');
      }
      for(; p >= lower; p--) {
          sb.append((char) ('0' + getDigit(p)));
      }
      return sb.toString();
  }

  private static int toRange(int i, int lo, int hi) {
    if (i < lo) {
      return lo;
    } else if (i > hi) {
      return hi;
    } else {
      return i;
    }
  }

  @Override
  public void populateUFieldPosition(FieldPosition fp) {
    if (fp instanceof UFieldPosition) {
      ((UFieldPosition) fp)
          .setFractionDigits((int) getPluralOperand(Operand.v), (long) getPluralOperand(Operand.f));
    }
  }

  @Override
  public int getExponent() {
    return origPrimaryScale;
  }

  @Override
  public void adjustExponent(int delta) {
      origPrimaryScale = origPrimaryScale + delta;
  }

  @Override
  public void resetExponent() {
      adjustMagnitude(origPrimaryScale);
      origPrimaryScale = 0;
  }

  @Override
  public boolean isHasIntegerValue() {
    return scaleBigDecimal(toBigDecimal()) >= 0;
  }

  @Override public String toExponentString() {
    return this.toPlainString();
  }
}
