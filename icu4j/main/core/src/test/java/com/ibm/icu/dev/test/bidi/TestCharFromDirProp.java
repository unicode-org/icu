// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2007-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.test.bidi;

import org.junit.Test;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;

/**
 * Regression test for Bidi charFromDirProp
 *
 * @author Lina Kemmel, Matitiahu Allouche
 */

public class TestCharFromDirProp extends BidiFmwk {

    /* verify that the exemplar characters have the expected bidi classes */
    @Test
    public void testCharFromDirProp() {

        logln("\nEntering TestCharFromDirProp");
        int i = UCharacterDirection.CHAR_DIRECTION_COUNT;
        while (i-- > 0) {
            char c = charFromDirProp[i];
            int dir = UCharacter.getDirection(c);
            assertEquals("UCharacter.getDirection(TestData.charFromDirProp[" + i
                    + "] == U+" + Utility.hex(c) + ") failed", i, dir);
        }
        logln("\nExiting TestCharFromDirProp");
    }
}
