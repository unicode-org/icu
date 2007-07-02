/*
*******************************************************************************
*   Copyright (C) 2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.test.bidi;

import com.ibm.icu.text.Bidi;

/**
 * Regression test for Bidi failure recovery
 *
 * @author Lina Kemmel, Matitiahu Allouche
 */

public class TestFailureRecovery extends BidiTest {

    public void testFailureRecovery()
    {
        logln("\nEntering TestFailureRecovery\n");
        Bidi bidi = new Bidi();
        try {
            bidi.setPara("abc", (byte)(Bidi.LEVEL_DEFAULT_LTR - 1), null);
            errln("Bidi.setPara did not fail when passed illegal para level");
        } catch (IllegalArgumentException e) {
            logln("OK: Got exception for bidi.setPara(..., Bidi.LEVEL_DEFAULT_LTR - 1"
                    + " as expected: " + e.getMessage());
        }
        try {
            Bidi.writeReverse(null, (short)0);
            errln("Bidi.writeReverse did not fail when passed a null string");
        } catch (IllegalArgumentException e) {
            logln("OK: Got exception for Bidi.writeReverse(null) as expected: "
                    + e.getMessage());
        }
        logln("\nExiting TestFailureRecovery\n");
    }


    public static void main(String[] args) {
        try {
            new TestFailureRecovery().run(args);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
