// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.modifiers;

import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.impl.number.Modifier.AffixModifier;

// TODO: This class is currently unused.  Should probably be deleted.

/** A class containing a positive form and a negative form of {@link ConstantAffixModifier}. */
public class PositiveNegativeAffixModifier implements Modifier.PositiveNegativeModifier {
  private final AffixModifier positive;
  private final AffixModifier negative;

  /**
   * Constructs an instance using the two {@link ConstantMultiFieldModifier} classes for positive
   * and negative.
   *
   * @param positive The positive-form Modifier.
   * @param negative The negative-form Modifier.
   */
  public PositiveNegativeAffixModifier(AffixModifier positive, AffixModifier negative) {
    this.positive = positive;
    this.negative = negative;
  }

  @Override
  public Modifier getModifier(boolean isNegative) {
    return isNegative ? negative : positive;
  }
}
