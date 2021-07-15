// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import static com.ibm.icu.impl.StaticUnicodeSets.get;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.StaticUnicodeSets.Key;

/**
 * NOTE: The test testSetCoverage can be found in ExhaustiveNumberTest.java
 *
 * @author sffc
 */
public class StaticUnicodeSetsTest {

    @Test
    public void testFrozen() {
        for (Key key : Key.values()) {
            assertTrue(get(key).isFrozen());
        }
    }

    @Test
    public void testNonEmpty() {
        for (Key key : Key.values()) {
            // NOTE: No key EMPTY in Java
            assertTrue(get(key).isFrozen());
        }
    }
}
