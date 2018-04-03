// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.number.Multiplier;

/**
 * Wraps a {@link Multiplier} for use in the number parsing pipeline.
 */
public class MultiplierParseHandler extends ValidationMatcher {

    private final Multiplier multiplier;

    public MultiplierParseHandler(Multiplier multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        if (result.quantity != null) {
            multiplier.applyReciprocalTo(result.quantity);
            // NOTE: It is okay if the multiplier was negative.
        }
    }

    @Override
    public String toString() {
        return "<MultiplierHandler " + multiplier + ">";
    }
}
