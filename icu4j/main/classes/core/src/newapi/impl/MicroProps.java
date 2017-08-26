// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.text.DecimalFormatSymbols;

import newapi.Grouper;
import newapi.IntegerWidth;
import newapi.NumberFormatter;
import newapi.Rounder;
import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;

public class MicroProps implements Cloneable, QuantityChain {
  // Populated globally:
  public SignDisplay sign;
  public DecimalFormatSymbols symbols;
  public Padder padding;
  public DecimalMarkDisplay decimal;
  public IntegerWidth integerWidth;

  // Populated by notation/unit:
  public Modifier modOuter;
  public Modifier modMiddle;
  public Modifier modInner;
  public Rounder rounding;
  public Grouper grouping;
  public int multiplier;
  public boolean useCurrency;

  private final boolean immutable;

  /**
   * @param immutable Whether this MicroProps should behave as an immutable after construction with
   *     respect to the quantity chain.
   */
  public MicroProps(boolean immutable) {
    this.immutable = immutable;
  }

  @Override
  public MicroProps withQuantity(FormatQuantity quantity) {
    if (immutable) {
      return (MicroProps) this.clone();
    } else {
      return this;
    }
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }
}
