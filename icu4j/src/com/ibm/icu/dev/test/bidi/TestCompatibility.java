/*
*******************************************************************************
*   Copyright (C) 2007, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.dev.test.bidi;

import com.ibm.icu.text.Bidi;

/**
 * Regression test for java.text.Bidi compatibility
 *
 * @author Matitiahu Allouche
 */

public class TestCompatibility extends BidiTest {

    public void testCompatibility()
    {
        logln("\nEntering TestCompatibility\n");

        logln("We should really do something here!\n");

        logln("\nExiting TestCompatibility\n");
    }

    public static void main(String[] args) {
        try {
            new TestCompatibility().run(args);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
