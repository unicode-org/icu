// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
//
/**
 *
 */
package com.ibm.icu.text;

import com.ibm.icu.lang.UScript;

class ThaiLSTMBreakEngine extends LSTMBreakEngine {
    public ThaiLSTMBreakEngine() {
        super("Thai_graphclust_model4_heavy",
              "[[:Thai:]&[:LineBreak=SA:]]", UScript.THAI);
    }

    @Override
    public boolean equals(Object obj) {
        // Normally is a singleton, but it's possible to have duplicates
        //   during initialization. All are equivalent.
        return obj instanceof ThaiLSTMBreakEngine;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
