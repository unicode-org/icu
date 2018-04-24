// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static com.ibm.icu.impl.number.parse.UnicodeSetStaticCache.get;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.number.parse.UnicodeSetStaticCache.Key;

/**
 * This test class is thin; most of it was moved to ExhaustiveNumberTest.
 * @author sffc
 */
public class UnicodeSetStaticCacheTest {

    @Test
    public void testFrozen() {
        for (Key key : Key.values()) {
            assertTrue(get(key).isFrozen());
        }
    }
}
