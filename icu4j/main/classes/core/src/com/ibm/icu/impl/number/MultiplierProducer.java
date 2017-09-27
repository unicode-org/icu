// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

/**
 * An interface used by compact notation and scientific notation to choose a multiplier while rounding.
 */
public interface MultiplierProducer {
    int getMultiplier(int magnitude);
}
