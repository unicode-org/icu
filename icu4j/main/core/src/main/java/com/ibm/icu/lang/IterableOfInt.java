// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.lang;

import java.util.PrimitiveIterator;

/**
 * Subinterface of Iterable whose iterator() returns a {@link PrimitiveIterator.OfInt}. Allows
 * direct use of the primitive iterator without downcasting.
 *
 * @draft ICU 78
 */
public interface IterableOfInt extends Iterable<Integer> {
    /**
     * @return a {@link PrimitiveIterator.OfInt}
     * @draft ICU 78
     */
    @Override
    public PrimitiveIterator.OfInt iterator();
}
