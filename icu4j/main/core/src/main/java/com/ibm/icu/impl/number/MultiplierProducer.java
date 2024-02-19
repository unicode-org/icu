// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

/**
 * An interface used by compact notation and scientific notation to choose a multiplier while rounding.
 */
public interface MultiplierProducer {
    /**
     * Maps a magnitude to a multiplier in powers of ten. For example, in compact notation in English, a
     * magnitude of 5 (e.g., 100,000) should return a multiplier of -3, since the number is displayed in
     * thousands.
     *
     * @param magnitude
     *            The power of ten of the input number.
     * @return The shift in powers of ten.
     */
    int getMultiplier(int magnitude);
}
