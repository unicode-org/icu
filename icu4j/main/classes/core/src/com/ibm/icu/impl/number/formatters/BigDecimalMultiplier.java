// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.formatters;

import java.math.BigDecimal;

import com.ibm.icu.impl.number.Format.BeforeFormat;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.ModifierHolder;
import com.ibm.icu.impl.number.Properties;

public class BigDecimalMultiplier extends BeforeFormat {
  public static interface IProperties {

    static BigDecimal DEFAULT_MULTIPLIER = null;

    /** @see #setMultiplier */
    public BigDecimal getMultiplier();

    /**
     * Multiply all numbers by this amount before formatting.
     *
     * @param multiplier The amount to multiply by.
     * @return The property bag, for chaining.
     * @see MagnitudeMultiplier
     */
    public IProperties setMultiplier(BigDecimal multiplier);
  }

  public static boolean useMultiplier(IProperties properties) {
    return properties.getMultiplier() != IProperties.DEFAULT_MULTIPLIER;
  }

  private final BigDecimal multiplier;

  public static BigDecimalMultiplier getInstance(IProperties properties) {
    if (properties.getMultiplier() == null) {
      throw new IllegalArgumentException("The multiplier must be present for MultiplierFormat");
    }
    // TODO: Intelligently fall back to a MagnitudeMultiplier if the multiplier is a power of ten?
    return new BigDecimalMultiplier(properties);
  }

  private BigDecimalMultiplier(IProperties properties) {
    this.multiplier = properties.getMultiplier();
  }

  @Override
  public void before(FormatQuantity input, ModifierHolder mods) {
    input.multiplyBy(multiplier);
  }

  @Override
  public void export(Properties properties) {
    properties.setMultiplier(multiplier);
  }
}
