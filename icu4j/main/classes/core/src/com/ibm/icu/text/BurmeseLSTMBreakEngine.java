// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
//
/**
 *
 */
package com.ibm.icu.text;

import com.ibm.icu.lang.UScript;

class BurmeseLSTMBreakEngine extends LSTMBreakEngine {
    public BurmeseLSTMBreakEngine() {
        super("Burmese_graphclust_model5_heavy",
            "[[:Mymr:]&[:LineBreak=SA:]]", UScript.MYANMAR);
    }

    @Override
    public boolean equals(Object obj) {
        // Normally is a singleton, but it's possible to have duplicates
        //   during initialization. All are equivalent.
        return obj instanceof BurmeseLSTMBreakEngine;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
