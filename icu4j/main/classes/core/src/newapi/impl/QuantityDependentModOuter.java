// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.util.Map;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.Modifier;
import com.ibm.icu.text.PluralRules;

public class QuantityDependentModOuter implements QuantityChain {
  final Map<StandardPlural, Modifier> data;
  final PluralRules rules;
  /* final */ QuantityChain parent;

  public QuantityDependentModOuter(Map<StandardPlural, Modifier> data, PluralRules rules) {
    this.data = data;
    this.rules = rules;
  }

  @Override
  public QuantityChain chain(QuantityChain parent) {
    this.parent = parent;
    return this;
  }

  @Override
  public MicroProps withQuantity(FormatQuantity quantity) {
    MicroProps micros = parent.withQuantity(quantity);
    // TODO: Avoid the copy here?
    FormatQuantity copy = quantity.createCopy();
    micros.rounding.apply(copy);
    micros.modOuter = data.get(copy.getStandardPlural(rules));
    return micros;
  }
}
