// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import com.ibm.icu.impl.number.FormatQuantity;

public interface QuantityChain {
  QuantityChain chain(QuantityChain parent);
  MicroProps withQuantity(FormatQuantity quantity);
}