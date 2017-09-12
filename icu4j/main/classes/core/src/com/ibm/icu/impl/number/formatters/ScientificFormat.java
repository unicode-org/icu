// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.formatters;

import com.ibm.icu.impl.number.Format;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.FormatQuantitySelector;
import com.ibm.icu.impl.number.ModifierHolder;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.impl.number.Rounder;
import com.ibm.icu.impl.number.modifiers.ConstantAffixModifier;
import com.ibm.icu.impl.number.modifiers.PositiveNegativeAffixModifier;
import com.ibm.icu.impl.number.rounders.IncrementRounder;
import com.ibm.icu.impl.number.rounders.SignificantDigitsRounder;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;

public class ScientificFormat extends Format.BeforeFormat implements Rounder.MultiplierGenerator {

  public static interface IProperties
      extends RoundingFormat.IProperties, CurrencyFormat.IProperties {

    static boolean DEFAULT_EXPONENT_SIGN_ALWAYS_SHOWN = false;

    /** @see #setExponentSignAlwaysShown */
    public boolean getExponentSignAlwaysShown();

    /**
     * Sets whether to show the plus sign in the exponent part of numbers with a zero or positive
     * exponent. For example, the number "1200" with the pattern "0.0E0" would be formatted as
     * "1.2E+3" instead of "1.2E3" in <em>en-US</em>.
     *
     * @param exponentSignAlwaysShown Whether to show the plus sign in positive exponents.
     * @return The property bag, for chaining.
     */
    public IProperties setExponentSignAlwaysShown(boolean exponentSignAlwaysShown);

    static int DEFAULT_MINIMUM_EXPONENT_DIGITS = -1;

    /** @see #setMinimumExponentDigits */
    public int getMinimumExponentDigits();

    /**
     * Sets the minimum number of digits to display in the exponent. For example, the number "1200"
     * with the pattern "0.0E00", which has 2 exponent digits, would be formatted as "1.2E03" in
     * <em>en-US</em>.
     *
     * @param minimumExponentDigits The minimum number of digits to display in the exponent field.
     * @return The property bag, for chaining.
     */
    public IProperties setMinimumExponentDigits(int minimumExponentDigits);
  }

  public static boolean useScientificNotation(IProperties properties) {
    return properties.getMinimumExponentDigits() != IProperties.DEFAULT_MINIMUM_EXPONENT_DIGITS;
  }

  private static final ThreadLocal<Properties> threadLocalProperties =
      new ThreadLocal<Properties>() {
        @Override
        protected Properties initialValue() {
          return new Properties();
        }
      };

  public static ScientificFormat getInstance(DecimalFormatSymbols symbols, IProperties properties) {
    // If significant digits or rounding interval are specified through normal means, we use those.
    // Otherwise, we use the special significant digit rules for scientific notation.
    Rounder rounder;
    if (IncrementRounder.useRoundingIncrement(properties)) {
      rounder = IncrementRounder.getInstance(properties);
    } else if (SignificantDigitsRounder.useSignificantDigits(properties)) {
      rounder = SignificantDigitsRounder.getInstance(properties);
    } else {
      Properties rprops = threadLocalProperties.get().clear();

      int minInt = properties.getMinimumIntegerDigits();
      int maxInt = properties.getMaximumIntegerDigits();
      int minFrac = properties.getMinimumFractionDigits();
      int maxFrac = properties.getMaximumFractionDigits();

      // If currency is in use, pull information from CurrencyUsage.
      if (CurrencyFormat.useCurrency(properties)) {
        // Use rprops as the vehicle (it is still clean)
        CurrencyFormat.populateCurrencyRounderProperties(rprops, symbols, properties);
        minFrac = rprops.getMinimumFractionDigits();
        maxFrac = rprops.getMaximumFractionDigits();
        rprops.clear();
      }

      // TODO: Mark/Andy, take a look at this logic and see if it makes sense to you.
      // I fiddled with the settings and fallbacks to make the unit tests pass, but I
      // don't feel that it's the "right way" to do things.

      if (minInt < 0) minInt = 0;
      if (maxInt < minInt) maxInt = minInt;
      if (minFrac < 0) minFrac = 0;
      if (maxFrac < minFrac) maxFrac = minFrac;

      rprops.setRoundingMode(properties.getRoundingMode());

      if (minInt == 0 && maxFrac == 0) {
        // Special case for the pattern "#E0" with no significant digits specified.
        rprops.setMinimumSignificantDigits(1);
        rprops.setMaximumSignificantDigits(Integer.MAX_VALUE);
      } else if (minInt == 0 && minFrac == 0) {
        // Special case for patterns like "#.##E0" with no significant digits specified.
        rprops.setMinimumSignificantDigits(1);
        rprops.setMaximumSignificantDigits(1 + maxFrac);
      } else {
        rprops.setMinimumSignificantDigits(minInt + minFrac);
        rprops.setMaximumSignificantDigits(minInt + maxFrac);
      }
      rprops.setMinimumIntegerDigits(maxInt == 0 ? 0 : Math.max(1, minInt + minFrac - maxFrac));
      rprops.setMaximumIntegerDigits(maxInt);
      rprops.setMinimumFractionDigits(Math.max(0, minFrac + minInt - maxInt));
      rprops.setMaximumFractionDigits(maxFrac);
      rounder = SignificantDigitsRounder.getInstance(rprops);
    }

    return new ScientificFormat(symbols, properties, rounder);
  }

  public static ScientificFormat getInstance(
      DecimalFormatSymbols symbols, IProperties properties, Rounder rounder) {
    return new ScientificFormat(symbols, properties, rounder);
  }

  // Properties
  private final boolean exponentShowPlusSign;
  private final int exponentDigits;
  private final int minInt;
  private final int maxInt;
  private final int interval;
  private final Rounder rounder;
  private final ConstantAffixModifier separatorMod;
  private final PositiveNegativeAffixModifier signMod;

  // Symbols
  private final String[] digitStrings;

  private ScientificFormat(DecimalFormatSymbols symbols, IProperties properties, Rounder rounder) {
    exponentShowPlusSign = properties.getExponentSignAlwaysShown();
    exponentDigits = Math.max(1, properties.getMinimumExponentDigits());

    // Calculate minInt/maxInt for the purposes of engineering notation:
    //   0 <= minInt <= maxInt < 8
    // The values are validated separately for rounding. This scheme needs to prevent OOM issues
    // (see #13118). Note that the bound 8 on integer digits is historic.
    int _maxInt = properties.getMaximumIntegerDigits();
    int _minInt = properties.getMinimumIntegerDigits();
    // Bug #13289: if maxInt > minInt > 1, then minInt should be 1 for the
    // purposes of engineering notatation.
    if (_maxInt > _minInt && _minInt > 1) {
        _minInt = 1;
    }
    minInt = _minInt < 0 ? 0 : _minInt >= 8 ? 1 : _minInt;
    maxInt = _maxInt < _minInt ? _minInt : _maxInt >= 8 ? _minInt : _maxInt;
    assert 0 <= minInt && minInt <= maxInt && maxInt < 8;

    interval = maxInt < 1 ? 1 : maxInt;
    this.rounder = rounder;
    digitStrings = symbols.getDigitStrings(); // makes a copy

    separatorMod =
        new ConstantAffixModifier(
            "", symbols.getExponentSeparator(), NumberFormat.Field.EXPONENT_SYMBOL, true);
    signMod =
        new PositiveNegativeAffixModifier(
            new ConstantAffixModifier(
                "",
                exponentShowPlusSign ? symbols.getPlusSignString() : "",
                NumberFormat.Field.EXPONENT_SIGN,
                true),
            new ConstantAffixModifier(
                "", symbols.getMinusSignString(), NumberFormat.Field.EXPONENT_SIGN, true));
  }

  private static final ThreadLocal<StringBuilder> threadLocalStringBuilder =
      new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
          return new StringBuilder();
        }
      };

  @Override
  public void before(FormatQuantity input, ModifierHolder mods) {

    // Treat zero as if it had magnitude 0
    int exponent;
    if (input.isZero()) {
      rounder.apply(input);
      exponent = 0;
    } else {
      // TODO: Revisit chooseMultiplierAndApply
      exponent = -rounder.chooseMultiplierAndApply(input, this);
    }

    // Format the exponent part of the scientific format.
    // Insert digits starting from the left so that append can be used.
    // TODO: Use thread locals here.
    FormatQuantity exponentQ = FormatQuantitySelector.from(exponent);
    StringBuilder exponentSB = threadLocalStringBuilder.get();
    exponentSB.setLength(0);
    exponentQ.setIntegerFractionLength(exponentDigits, Integer.MAX_VALUE, 0, 0);
    for (int i = exponentQ.getUpperDisplayMagnitude(); i >= 0; i--) {
      exponentSB.append(digitStrings[exponentQ.getDigit(i)]);
    }

    // Add modifiers from the outside in.
    mods.add(
        new ConstantAffixModifier("", exponentSB.toString(), NumberFormat.Field.EXPONENT, true));
    mods.add(signMod.getModifier(exponent < 0));
    mods.add(separatorMod);
  }

  @Override
  public int getMultiplier(int magnitude) {
    int digitsShown = ((magnitude % interval + interval) % interval) + 1;
    if (digitsShown < minInt) {
      digitsShown = minInt;
    } else if (digitsShown > maxInt) {
      digitsShown = maxInt;
    }
    int retval = digitsShown - magnitude - 1;
    return retval;
  }

  @Override
  public void export(Properties properties) {
    properties.setMinimumExponentDigits(exponentDigits);
    properties.setExponentSignAlwaysShown(exponentShowPlusSign);

    // Set the transformed object into the property bag.  This may result in a pattern string that
    // uses different syntax from the original, but it will be functionally equivalent.
    rounder.export(properties);
  }
}
