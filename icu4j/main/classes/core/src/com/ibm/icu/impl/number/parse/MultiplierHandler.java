// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * @author sffc
 *
 */
public class MultiplierHandler extends ValidationMatcher {

    private final BigDecimal multiplier;
    private final MathContext mc;
    private final boolean isNegative;

    public MultiplierHandler(BigDecimal multiplier, MathContext mc) {
        this.multiplier = BigDecimal.ONE.divide(multiplier, mc).abs();
        this.mc = mc;
        isNegative = multiplier.signum() < 0;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        if (result.quantity != null) {
            result.quantity.multiplyBy(multiplier);
            result.quantity.roundToMagnitude(result.quantity.getMagnitude() - mc.getPrecision(), mc);
            if (isNegative) {
                result.flags ^= ParsedNumber.FLAG_NEGATIVE;
            }
        }
    }

    @Override
    public String toString() {
        return "<MultiplierHandler " + multiplier + ">";
    }
}
