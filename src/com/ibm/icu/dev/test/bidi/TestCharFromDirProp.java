/*
*******************************************************************************
*   Copyright (C) 2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.test.bidi;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.impl.Utility;

/**
 * Regression test for Bidi charFromDirProp
 *
 * @author Lina Kemmel, Matitiahu Allouche
 */

public class TestCharFromDirProp extends BidiTest {

    /* verify that the exemplar characters have the expected bidi classes */
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

    public static void main(String[] args) {
        try {
            new TestCharFromDirProp().run(args);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
