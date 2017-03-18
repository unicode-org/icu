// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.rounders;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Rounder;

/** Sets the integer and fraction length based on the properties, but does not perform rounding. */
public final class NoRounder extends Rounder {

  public static NoRounder getInstance(IBasicRoundingProperties properties) {
    return new NoRounder(properties);
  }

  private NoRounder(IBasicRoundingProperties properties) {
    super(properties);
  }

  @Override
  public void apply(FormatQuantity input) {
    applyDefaults(input);
    input.roundToInfinity();
  }
}
