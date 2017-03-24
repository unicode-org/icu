// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.rounders;

import java.math.RoundingMode;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.impl.number.Rounder;
import com.ibm.icu.text.DecimalFormat.SignificantDigitsMode;

public class SignificantDigitsRounder extends Rounder {

  public static interface IProperties extends Rounder.IBasicRoundingProperties {

    static int DEFAULT_MINIMUM_SIGNIFICANT_DIGITS = -1;

    /** @see #setMinimumSignificantDigits */
    public int getMinimumSignificantDigits();

    /**
     * Sets the minimum number of significant digits to display. If, after rounding to the number of
     * significant digits specified by {@link #setMaximumSignificantDigits}, the number of remaining
     * significant digits is less than the minimum, the number will be padded with zeros. For
     * example, if minimum significant digits is 3, the number 5.8 will be formatted as "5.80" in
     * locale <em>en-US</em>. Note that minimum significant digits is relevant only when numbers
     * have digits after the decimal point.
     *
     * <p>If both minimum significant digits and minimum integer/fraction digits are set at the same
     * time, both values will be respected, and the one that results in the greater number of
     * padding zeros will be used. For example, formatting the number 73 with 3 minimum significant
     * digits and 2 minimum fraction digits will produce "73.00".
     *
     * <p>The number of significant digits can be specified in a pattern string using the '@'
     * character. For example, the pattern "@@#" corresponds to a minimum of 2 and a maximum of 3
     * significant digits.
     *
     * @param minimumSignificantDigits The minimum number of significant digits to display.
     * @return The property bag, for chaining.
     */
    public IProperties setMinimumSignificantDigits(int minimumSignificantDigits);

    static int DEFAULT_MAXIMUM_SIGNIFICANT_DIGITS = -1;

    /** @see #setMaximumSignificantDigits */
    public int getMaximumSignificantDigits();

    /**
     * Sets the maximum number of significant digits to display. The number of significant digits is
     * equal to the number of digits counted from the leftmost nonzero digit through the rightmost
     * nonzero digit; for example, the number "2010" has 3 significant digits. If the number has
     * more significant digits than specified here, the extra significant digits will be rounded off
     * using the rounding mode specified by {@link #setRoundingMode(RoundingMode)}. For example, if
     * maximum significant digits is 3, the number 1234.56 will be formatted as "1230" in locale
     * <em>en-US</em> with the default rounding mode.
     *
     * <p>If both maximum significant digits and maximum integer/fraction digits are set at the same
     * time, the behavior is undefined.
     *
     * <p>The number of significant digits can be specified in a pattern string using the '@'
     * character. For example, the pattern "@@#" corresponds to a minimum of 2 and a maximum of 3
     * significant digits.
     *
     * @param maximumSignificantDigits The maximum number of significant digits to display.
     * @return The property bag, for chaining.
     */
    public IProperties setMaximumSignificantDigits(int maximumSignificantDigits);

    static SignificantDigitsMode DEFAULT_SIGNIFICANT_DIGITS_MODE = null;

    /** @see #setSignificantDigitsMode */
    public SignificantDigitsMode getSignificantDigitsMode();

    /**
     * Sets the strategy used when reconciling significant digits versus integer and fraction
     * lengths.
     *
     * @param significantDigitsMode One of the options from {@link SignificantDigitsMode}.
     * @return The property bag, for chaining.
     */
    public IProperties setSignificantDigitsMode(SignificantDigitsMode significantDigitsMode);
  }

  public static boolean useSignificantDigits(IProperties properties) {
    return properties.getMinimumSignificantDigits()
            != IProperties.DEFAULT_MINIMUM_SIGNIFICANT_DIGITS
        || properties.getMaximumSignificantDigits()
            != IProperties.DEFAULT_MAXIMUM_SIGNIFICANT_DIGITS
        || properties.getSignificantDigitsMode() != IProperties.DEFAULT_SIGNIFICANT_DIGITS_MODE;
  }

  public static SignificantDigitsRounder getInstance(IProperties properties) {
    return new SignificantDigitsRounder(properties);
  }

  private final int minSig;
  private final int maxSig;
  private final SignificantDigitsMode mode;

  private SignificantDigitsRounder(IProperties properties) {
    super(properties);
    int _minSig = properties.getMinimumSignificantDigits();
    int _maxSig = properties.getMaximumSignificantDigits();
    minSig = _minSig < 1 ? 1 : _minSig > 1000 ? 1000 : _minSig;
    maxSig = _maxSig < 0 ? 1000 : _maxSig < minSig ? minSig : _maxSig > 1000 ? 1000 : _maxSig;
    SignificantDigitsMode _mode = properties.getSignificantDigitsMode();
    mode = _mode == null ? SignificantDigitsMode.OVERRIDE_MAXIMUM_FRACTION : _mode;
  }

  @Override
  public void apply(FormatQuantity input) {

    int magnitude, effectiveMag, magMinSig, magMaxSig;

    if (input.isZero()) {
      // Treat zero as if magnitude corresponded to the minimum number of zeros
      magnitude = minInt - 1;
    } else {
      magnitude = input.getMagnitude();
    }
    effectiveMag = Math.min(magnitude + 1, maxInt);
    magMinSig = effectiveMag - minSig;
    magMaxSig = effectiveMag - maxSig;

    // Step 1: pick the rounding magnitude and apply.
    int roundingMagnitude;
    switch (mode) {
      case OVERRIDE_MAXIMUM_FRACTION:
        // Always round to maxSig.
        // Of the six possible orders:
        //    Case 1: minSig, maxSig, minFrac, maxFrac -- maxSig wins
        //    Case 2: minSig, minFrac, maxSig, maxFrac -- maxSig wins
        //    Case 3: minSig, minFrac, maxFrac, maxSig -- maxSig wins
        //    Case 4: minFrac, minSig, maxSig, maxFrac -- maxSig wins
        //    Case 5: minFrac, minSig, maxFrac, maxSig -- maxSig wins
        //    Case 6: minFrac, maxFrac, minSig, maxSig -- maxSig wins
        roundingMagnitude = magMaxSig;
        break;
      case RESPECT_MAXIMUM_FRACTION:
        // Round to the strongest of maxFrac, maxInt, and maxSig.
        // Of the six possible orders:
        //    Case 1: minSig, maxSig, minFrac, maxFrac -- maxSig wins
        //    Case 2: minSig, minFrac, maxSig, maxFrac -- maxSig wins
        //    Case 3: minSig, minFrac, maxFrac, maxSig -- maxFrac wins --> differs from default
        //    Case 4: minFrac, minSig, maxSig, maxFrac -- maxSig wins
        //    Case 5: minFrac, minSig, maxFrac, maxSig -- maxFrac wins --> differs from default
        //    Case 6: minFrac, maxFrac, minSig, maxSig -- maxFrac wins --> differs from default
        //
        // Math.max() picks the rounding magnitude farthest to the left (most significant).
        // Math.min() picks the rounding magnitude farthest to the right (least significant).
        roundingMagnitude = Math.max(-maxFrac, magMaxSig);
        break;
      case ENSURE_MINIMUM_SIGNIFICANT:
        // Round to the strongest of maxFrac and maxSig, and always ensure minSig.
        // Of the six possible orders:
        //    Case 1: minSig, maxSig, minFrac, maxFrac -- maxSig wins
        //    Case 2: minSig, minFrac, maxSig, maxFrac -- maxSig wins
        //    Case 3: minSig, minFrac, maxFrac, maxSig -- maxFrac wins --> differs from default
        //    Case 4: minFrac, minSig, maxSig, maxFrac -- maxSig wins
        //    Case 5: minFrac, minSig, maxFrac, maxSig -- maxFrac wins --> differs from default
        //    Case 6: minFrac, maxFrac, minSig, maxSig -- minSig wins --> differs from default
        roundingMagnitude = Math.min(magMinSig, Math.max(-maxFrac, magMaxSig));
        break;
      default:
        throw new AssertionError();
    }
    input.roundToMagnitude(roundingMagnitude, mathContext);

    // In case magnitude changed:
    if (input.isZero()) {
      magnitude = minInt - 1;
    } else {
      magnitude = input.getMagnitude();
    }
    effectiveMag = Math.min(magnitude + 1, maxInt);
    magMinSig = effectiveMag - minSig;
    magMaxSig = effectiveMag - maxSig;

    // Step 2: pick the number of visible digits.
    switch (mode) {
      case OVERRIDE_MAXIMUM_FRACTION:
        // Ensure minSig is always displayed.
        input.setIntegerFractionLength(
            minInt, maxInt, Math.max(minFrac, -magMinSig), Integer.MAX_VALUE);
        break;
      case RESPECT_MAXIMUM_FRACTION:
        // Ensure minSig is displayed, unless doing so is in violation of maxFrac.
        input.setIntegerFractionLength(
            minInt, maxInt, Math.min(maxFrac, Math.max(minFrac, -magMinSig)), maxFrac);
        break;
      case ENSURE_MINIMUM_SIGNIFICANT:
        // Follow minInt/minFrac, but ensure all digits are allowed to be visible.
        input.setIntegerFractionLength(minInt, maxInt, minFrac, Integer.MAX_VALUE);
        break;
    }
  }

  @Override
  public void export(Properties properties) {
    super.export(properties);
    properties.setMinimumSignificantDigits(minSig);
    properties.setMaximumSignificantDigits(maxSig);
    properties.setSignificantDigitsMode(mode);
  }
}
