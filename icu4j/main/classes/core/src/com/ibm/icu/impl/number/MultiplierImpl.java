// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.math.BigDecimal;

public class MultiplierImpl implements MicroPropsGenerator, Cloneable {
  final int magnitudeMultiplier;
  final BigDecimal bigDecimalMultiplier;
  final MicroPropsGenerator parent;

  public MultiplierImpl(int magnitudeMultiplier) {
    this.magnitudeMultiplier = magnitudeMultiplier;
    this.bigDecimalMultiplier = null;
    parent = null;
  }

  public MultiplierImpl(BigDecimal bigDecimalMultiplier) {
    this.magnitudeMultiplier = 0;
    this.bigDecimalMultiplier = bigDecimalMultiplier;
    parent = null;
  }

  private MultiplierImpl(MultiplierImpl base, MicroPropsGenerator parent) {
    this.magnitudeMultiplier = base.magnitudeMultiplier;
    this.bigDecimalMultiplier = base.bigDecimalMultiplier;
    this.parent = parent;
  }

  public MicroPropsGenerator copyAndChain(MicroPropsGenerator parent) {
    return new MultiplierImpl(this, parent);
  }

  @Override
  public MicroProps processQuantity(DecimalQuantity quantity) {
    MicroProps micros = parent.processQuantity(quantity);
    quantity.adjustMagnitude(magnitudeMultiplier);
    if (bigDecimalMultiplier != null) {
      quantity.multiplyBy(bigDecimalMultiplier);
    }
    return micros;
  }
}
