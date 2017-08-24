// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.math.BigDecimal;

import com.ibm.icu.impl.number.FormatQuantity;

public class MultiplierImpl implements QuantityChain, Cloneable {
  final int magnitudeMultiplier;
  final BigDecimal bigDecimalMultiplier;
  final QuantityChain parent;

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

  private MultiplierImpl(MultiplierImpl base, QuantityChain parent) {
    this.magnitudeMultiplier = base.magnitudeMultiplier;
    this.bigDecimalMultiplier = base.bigDecimalMultiplier;
    this.parent = parent;
  }

  public QuantityChain copyAndChain(QuantityChain parent) {
    return new MultiplierImpl(this, parent);
  }

  @Override
  public MicroProps withQuantity(FormatQuantity quantity) {
    MicroProps micros = parent.withQuantity(quantity);
    quantity.adjustMagnitude(magnitudeMultiplier);
    if (bigDecimalMultiplier != null) {
      quantity.multiplyBy(bigDecimalMultiplier);
    }
    return micros;
  }
}
