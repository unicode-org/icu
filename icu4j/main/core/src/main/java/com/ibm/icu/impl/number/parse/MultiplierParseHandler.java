// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.number.Scale;

/**
 * Wraps a {@link Scale} for use in the number parsing pipeline.
 */
public class MultiplierParseHandler extends ValidationMatcher {

    private final Scale multiplier;

    public MultiplierParseHandler(Scale multiplier) {
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
