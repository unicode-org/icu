// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.math.MathContext;
import java.math.RoundingMode;

import com.ibm.icu.impl.number.formatters.CompactDecimalFormat;
import com.ibm.icu.impl.number.formatters.ScientificFormat;

/**
 * The base class for a Rounder used by ICU Decimal Format.
 *
 * <p>A Rounder must implement the method {@link #apply}. An implementation must:
 *
 * <ol>
 *   <li>Either have the code <code>applyDefaults(input);</code> in its apply function, or otherwise
 *       ensure that minFrac, maxFrac, minInt, and maxInt are obeyed, paying special attention to
 *       the case when the input is zero.
 *   <li>Call one of {@link FormatQuantity#roundToIncrement}, {@link
 *       FormatQuantity#roundToMagnitude}, or {@link FormatQuantity#roundToInfinity} on the input.
 * </ol>
 *
 * <p>In order to be used by {@link CompactDecimalFormat} and {@link ScientificFormat}, among
 * others, your rounder must be stable upon <em>decreasing</em> the magnitude of the input number.
 * For example, if your rounder converts "999" to "1000", it must also convert "99.9" to "100" and
 * "0.999" to "1". (The opposite does not need to be the case: you can round "0.999" to "1" but keep
 * "999" as "999".)
 *
 * @see com.ibm.icu.impl.number.rounders.MagnitudeRounder
 * @see com.ibm.icu.impl.number.rounders.IncrementRounder
 * @see com.ibm.icu.impl.number.rounders.SignificantDigitsRounder
 * @see com.ibm.icu.impl.number.rounders.NoRounder
 */
public abstract class Rounder extends Format.BeforeFormat {

  public static interface IBasicRoundingProperties {

    static int DEFAULT_MINIMUM_INTEGER_DIGITS = -1;

    /** @see #setMinimumIntegerDigits */
    public int getMinimumIntegerDigits();

    /**
     * Sets the minimum number of digits to display before the decimal point. If the number has
     * fewer than this number of digits, the number will be padded with zeros. The pattern "#00.0#",
     * for example, corresponds to 2 minimum integer digits, and the number 5.3 would be formatted
     * as "05.3" in locale <em>en-US</em>.
     *
     * @param minimumIntegerDigits The minimum number of integer digits to output.
     * @return The property bag, for chaining.
     */
    public IBasicRoundingProperties setMinimumIntegerDigits(int minimumIntegerDigits);

    static int DEFAULT_MAXIMUM_INTEGER_DIGITS = -1;

    /** @see #setMaximumIntegerDigits */
    public int getMaximumIntegerDigits();

    /**
     * Sets the maximum number of digits to display before the decimal point. If the number has more
     * than this number of digits, the extra digits will be truncated. For example, if maximum
     * integer digits is 2, and you attempt to format the number 1970, you will get "70" in locale
     * <em>en-US</em>. It is not possible to specify the maximum integer digits using a pattern
     * string, except in the special case of a scientific format pattern.
     *
     * @param maximumIntegerDigits The maximum number of integer digits to output.
     * @return The property bag, for chaining.
     */
    public IBasicRoundingProperties setMaximumIntegerDigits(int maximumIntegerDigits);

    static int DEFAULT_MINIMUM_FRACTION_DIGITS = -1;

    /** @see #setMinimumFractionDigits */
    public int getMinimumFractionDigits();

    /**
     * Sets the minimum number of digits to display after the decimal point. If the number has fewer
     * than this number of digits, the number will be padded with zeros. The pattern "#00.0#", for
     * example, corresponds to 1 minimum fraction digit, and the number 456 would be formatted as
     * "456.0" in locale <em>en-US</em>.
     *
     * @param minimumFractionDigits The minimum number of fraction digits to output.
     * @return The property bag, for chaining.
     */
    public IBasicRoundingProperties setMinimumFractionDigits(int minimumFractionDigits);

    static int DEFAULT_MAXIMUM_FRACTION_DIGITS = -1;

    /** @see #setMaximumFractionDigits */
    public int getMaximumFractionDigits();

    /**
     * Sets the maximum number of digits to display after the decimal point. If the number has fewer
     * than this number of digits, the number will be rounded off using the rounding mode specified
     * by {@link #setRoundingMode(RoundingMode)}. The pattern "#00.0#", for example, corresponds to
     * 2 maximum fraction digits, and the number 456.789 would be formatted as "456.79" in locale
     * <em>en-US</em> with the default rounding mode. Note that the number 456.999 would be
     * formatted as "457.0" given the same configurations.
     *
     * @param maximumFractionDigits The maximum number of fraction digits to output.
     * @return The property bag, for chaining.
     */
    public IBasicRoundingProperties setMaximumFractionDigits(int maximumFractionDigits);

    static RoundingMode DEFAULT_ROUNDING_MODE = null;

    /** @see #setRoundingMode */
    public RoundingMode getRoundingMode();

    /**
     * Sets the rounding mode, which determines under which conditions extra decimal places are
     * rounded either up or down. See {@link RoundingMode} for details on the choices of rounding
     * mode. The default if not set explicitly is {@link RoundingMode#HALF_EVEN}.
     *
     * <p>This setting is ignored if {@link #setMathContext} is used.
     *
     * @param roundingMode The rounding mode to use when rounding is required.
     * @return The property bag, for chaining.
     * @see RoundingMode
     * @see #setMathContext
     */
    public IBasicRoundingProperties setRoundingMode(RoundingMode roundingMode);

    static MathContext DEFAULT_MATH_CONTEXT = null;

    /** @see #setMathContext */
    public MathContext getMathContext();

    /**
     * Sets the {@link MathContext} to be used during math and rounding operations. A MathContext
     * encapsulates a RoundingMode and the number of significant digits in the output.
     *
     * @param mathContext The math context to use when rounding is required.
     * @return The property bag, for chaining.
     * @see MathContext
     * @see #setRoundingMode
     */
    public IBasicRoundingProperties setMathContext(MathContext mathContext);
  }

  public static interface MultiplierGenerator {
    public int getMultiplier(int magnitude);
  }

  // Properties available to all rounding strategies
  protected final MathContext mathContext;
  protected final int minInt;
  protected final int maxInt;
  protected final int minFrac;
  protected final int maxFrac;

  /**
   * Constructor that uses integer and fraction digit lengths from IBasicRoundingProperties.
   *
   * @param properties
   */
  protected Rounder(IBasicRoundingProperties properties) {
    mathContext = RoundingUtils.getMathContextOrUnlimited(properties);

    int _maxInt = properties.getMaximumIntegerDigits();
    int _minInt = properties.getMinimumIntegerDigits();
    int _maxFrac = properties.getMaximumFractionDigits();
    int _minFrac = properties.getMinimumFractionDigits();

    // Validate min/max int/frac.
    // For backwards compatibility, minimum overrides maximum if the two conflict.
    // The following logic ensures that there is always a minimum of at least one digit.
    if (_minInt == 0 && _maxFrac != 0) {
      // Force a digit to the right of the decimal point.
      minFrac = _minFrac <= 0 ? 1 : _minFrac;
      maxFrac = _maxFrac < 0 ? Integer.MAX_VALUE : _maxFrac < minFrac ? minFrac : _maxFrac;
      minInt = 0;
      maxInt = _maxInt < 0 ? Integer.MAX_VALUE : _maxInt;
    } else {
      // Force a digit to the left of the decimal point.
      minFrac = _minFrac < 0 ? 0 : _minFrac;
      maxFrac = _maxFrac < 0 ? Integer.MAX_VALUE : _maxFrac < minFrac ? minFrac : _maxFrac;
      minInt = _minInt <= 0 ? 1 : _minInt;
      maxInt = _maxInt < 0 ? Integer.MAX_VALUE : _maxInt < minInt ? minInt : _maxInt;
    }
  }

  /**
   * Perform rounding and specification of integer and fraction digit lengths on the input quantity.
   * Calling this method will change the state of the FormatQuantity.
   *
   * @param input The {@link FormatQuantity} to be modified and rounded.
   */
  public abstract void apply(FormatQuantity input);

  /**
   * Rounding can affect the magnitude. First we attempt to adjust according to the original
   * magnitude, and if the magnitude changes, we adjust according to a magnitude one greater. Note
   * that this algorithm assumes that increasing the multiplier never increases the number of digits
   * that can be displayed.
   *
   * @param input The quantity to be rounded.
   * @param mg The implementation that returns magnitude adjustment based on a given starting
   *     magnitude.
   * @return The multiplier that was chosen to best fit the input.
   */
  public int chooseMultiplierAndApply(FormatQuantity input, MultiplierGenerator mg) {
    // TODO: Avoid the object creation here.
    FormatQuantity copy = input.createCopy();

    int magnitude = input.getMagnitude();
    int multiplier = mg.getMultiplier(magnitude);
    input.adjustMagnitude(multiplier);
    apply(input);
    if (input.getMagnitude() == magnitude + multiplier + 1) {
      magnitude += 1;
      input.copyFrom(copy);
      multiplier = mg.getMultiplier(magnitude);
      input.adjustMagnitude(multiplier);
      assert input.getMagnitude() == magnitude + multiplier - 1;
      apply(input);
      assert input.getMagnitude() == magnitude + multiplier;
    }

    return multiplier;
  }

  /**
   * Implementations can call this method to perform default logic for min/max digits. This method
   * performs logic for handling of a zero input.
   *
   * @param input The digits being formatted.
   */
  protected void applyDefaults(FormatQuantity input) {
    input.setIntegerFractionLength(minInt, maxInt, minFrac, maxFrac);
  }

  @Override
  public void before(FormatQuantity input, ModifierHolder mods) {
    apply(input);
  }

  @Override
  public void export(Properties properties) {
    properties.setMathContext(mathContext);
    properties.setRoundingMode(mathContext.getRoundingMode());
    properties.setMinimumFractionDigits(minFrac);
    properties.setMinimumIntegerDigits(minInt);
    properties.setMaximumFractionDigits(maxFrac);
    properties.setMaximumIntegerDigits(maxInt);
  }
}
