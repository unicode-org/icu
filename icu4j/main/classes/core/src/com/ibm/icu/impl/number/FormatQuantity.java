// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.math.BigDecimal;
import java.math.MathContext;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.text.PluralRules;

/**
 * An interface representing a number to be processed by the decimal formatting pipeline. Includes
 * methods for rounding, plural rules, and decimal digit extraction.
 *
 * <p>By design, this is NOT IMMUTABLE and NOT THREAD SAFE. It is intended to be an intermediate
 * object holding state during a pass through the decimal formatting pipeline.
 *
 * <p>Implementations of this interface are free to use any internal storage mechanism.
 *
 * <p>TODO: Should I change this to an abstract class so that logic for min/max digits doesn't need
 * to be copied to every implementation?
 */
public interface FormatQuantity extends PluralRules.IFixedDecimal {

  /**
   * Sets the minimum and maximum digits that this {@link FormatQuantity} should generate. This
   * method does not perform rounding.
   *
   * @param minInt The minimum number of integer digits.
   * @param maxInt The maximum number of integer digits.
   * @param minFrac The minimum number of fraction digits.
   * @param maxFrac The maximum number of fraction digits.
   */
  public void setIntegerFractionLength(int minInt, int maxInt, int minFrac, int maxFrac);

  /**
   * Rounds the number to a specified interval, such as 0.05.
   *
   * <p>If rounding to a power of ten, use the more efficient {@link #roundToMagnitude} instead.
   *
   * @param roundingInterval The increment to which to round.
   * @param mathContext The {@link MathContext} to use if rounding is necessary. Undefined behavior
   *     if null.
   */
  public void roundToIncrement(BigDecimal roundingInterval, MathContext mathContext);

  /**
   * Rounds the number to a specified magnitude (power of ten).
   *
   * @param roundingMagnitude The power of ten to which to round. For example, a value of -2 will
   *     round to 2 decimal places.
   * @param mathContext The {@link MathContext} to use if rounding is necessary. Undefined behavior
   *     if null.
   */
  public void roundToMagnitude(int roundingMagnitude, MathContext mathContext);

  /**
   * Rounds the number to an infinite number of decimal points. This has no effect except for
   * forcing the double in {@link FormatQuantityBCD} to adopt its exact representation.
   */
  public void roundToInfinity();

  /**
   * Multiply the internal value.
   *
   * @param multiplicand The value by which to multiply.
   */
  public void multiplyBy(BigDecimal multiplicand);

  /**
   * Scales the number by a power of ten. For example, if the value is currently "1234.56", calling
   * this method with delta=-3 will change the value to "1.23456".
   *
   * @param delta The number of magnitudes of ten to change by.
   */
  public void adjustMagnitude(int delta);

  /**
   * @return The power of ten corresponding to the most significant nonzero digit.
   * @throws ArithmeticException If the value represented is zero.
   */
  public int getMagnitude() throws ArithmeticException;

  /** @return Whether the value represented by this {@link FormatQuantity} is zero. */
  public boolean isZero();

  /** @return Whether the value represented by this {@link FormatQuantity} is less than zero. */
  public boolean isNegative();

  /** @return Whether the value represented by this {@link FormatQuantity} is infinite. */
  @Override
  public boolean isInfinite();

  /** @return Whether the value represented by this {@link FormatQuantity} is not a number. */
  @Override
  public boolean isNaN();

  /** @return The value contained in this {@link FormatQuantity} approximated as a double. */
  public double toDouble();

  public BigDecimal toBigDecimal();

  public int maxRepresentableDigits();

  // TODO: Should this method be removed, since FormatQuantity implements IFixedDecimal now?
  /**
   * Computes the plural form for this number based on the specified set of rules.
   *
   * @param rules A {@link PluralRules} object representing the set of rules.
   * @return The {@link StandardPlural} according to the PluralRules. If the plural form is not in
   *     the set of standard plurals, {@link StandardPlural#OTHER} is returned instead.
   */
  public StandardPlural getStandardPlural(PluralRules rules);

  //  /**
  //   * @return The number of fraction digits, always in the closed interval [minFrac, maxFrac].
  //   * @see #setIntegerFractionLength(int, int, int, int)
  //   */
  //  public int fractionCount();
  //
  //  /**
  //   * @return The number of integer digits, always in the closed interval [minInt, maxInt].
  //   * @see #setIntegerFractionLength(int, int, int, int)
  //   */
  //  public int integerCount();
  //
  //  /**
  //   * @param index The index of the fraction digit relative to the decimal place, or 1 minus the
  //   *     digit's power of ten.
  //   * @return The digit at the specified index. Undefined if index is greater than maxInt or less
  //   *     than 0.
  //   * @see #fractionCount()
  //   */
  //  public byte getFractionDigit(int index);
  //
  //  /**
  //   * @param index The index of the integer digit relative to the decimal place, or the digit's power
  //   *     of ten.
  //   * @return The digit at the specified index. Undefined if index is greater than maxInt or less
  //   *     than 0.
  //   * @see #integerCount()
  //   */
  //  public byte getIntegerDigit(int index);

  /**
   * Gets the digit at the specified magnitude. For example, if the represented number is 12.3,
   * getDigit(-1) returns 3, since 3 is the digit corresponding to 10^-1.
   *
   * @param magnitude The magnitude of the digit.
   * @return The digit at the specified magnitude.
   */
  public byte getDigit(int magnitude);

  /**
   * Gets the largest power of ten that needs to be displayed. The value returned by this function
   * will be bounded between minInt and maxInt.
   *
   * @return The highest-magnitude digit to be displayed.
   */
  public int getUpperDisplayMagnitude();

  /**
   * Gets the smallest power of ten that needs to be displayed. The value returned by this function
   * will be bounded between -minFrac and -maxFrac.
   *
   * @return The lowest-magnitude digit to be displayed.
   */
  public int getLowerDisplayMagnitude();

  public FormatQuantity clone();

  public void copyFrom(FormatQuantity other);

  /**
   * This method is for internal testing only and should be removed before release.
   *
   * @internal
   */
  public long getPositionFingerprint();
}
