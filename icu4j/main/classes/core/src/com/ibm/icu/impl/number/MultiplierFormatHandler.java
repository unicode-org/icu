// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.number.Multiplier;

/**
 * Wraps a {@link Multiplier} for use in the number formatting pipeline.
 */
public class MultiplierFormatHandler implements MicroPropsGenerator {
    final Multiplier multiplier;
    final MicroPropsGenerator parent;

    public MultiplierFormatHandler(Multiplier multiplier, MicroPropsGenerator parent) {
        this.multiplier = multiplier;
        this.parent = parent;
    }

    @Override
    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = parent.processQuantity(quantity);
        multiplier.applyTo(quantity);
        return micros;
    }
}
