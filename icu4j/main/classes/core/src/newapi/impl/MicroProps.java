// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.text.DecimalFormatSymbols;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;

public class MicroProps implements Cloneable, QuantityChain {
  // Populated globally:
  public SignDisplay sign;
  public DecimalFormatSymbols symbols;
  public PaddingImpl padding;
  public DecimalMarkDisplay decimal;
  public IntegerWidthImpl integerWidth;

  // Populated by notation/unit:
  public Modifier modOuter;
  public Modifier modMiddle;
  public Modifier modInner;
  public RoundingImpl rounding;
  public GroupingImpl grouping;
  public int multiplier;
  public boolean useCurrency;

  private boolean frozen = false;

  public void enableCloneInChain() {
    frozen = true;
  }

  @Override
  public QuantityChain chain(QuantityChain parent) {
    // The MicroProps instance should always be at the top of the chain!
    throw new AssertionError();
  }

  @Override
  public MicroProps withQuantity(FormatQuantity quantity) {
    if (frozen) {
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
      throw new AssertionError();
    }
  }
}
