// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import java.math.BigDecimal;

public abstract class NumberTestBase extends CoreTestFmwk {
    static boolean doubleEquals(double d1, double d2) {
        return (Math.abs(d1 - d2) < 1e-6) || (Math.abs((d1 - d2) / d1) < 1e-6);
    }

    void assertDoubleEquals(String message, double d1, double d2) {
        boolean equal = doubleEquals(d1, d2);
        handleAssert(equal, message, d1, d2, null, false);
    }

    void assertBigDecimalEquals(String message, String d1, BigDecimal d2) {
        assertBigDecimalEquals(message, new BigDecimal(d1), d2);
    }

    void assertBigDecimalEquals(String message, BigDecimal d1, BigDecimal d2) {
        boolean equal = d1.compareTo(d2) == 0;
        handleAssert(equal, message, d1, d2, null, false);
    }

    void assertToStringAndHealth(DecimalQuantity_DualStorageBCD fq, String expected) {
        String actual = fq.toString();
        assertEquals("DecimalQuantity toString", expected, actual);
        String health = fq.checkHealth();
        assertNull("DecimalQuantity health", health);
    }
}
