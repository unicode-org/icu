/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/translit/TestAll.java,v $
 * $Date: 2003/01/28 18:55:35 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.translit;
import com.ibm.icu.dev.test.TestFmwk;
import java.util.TimeZone;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestFmwk {
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        new TestAll().run(args);
    }
    public void TestTranslit() throws Exception {
        run( new TestFmwk[] {
            new com.ibm.icu.dev.test.translit.TransliteratorTest(),
            new com.ibm.icu.dev.test.translit.UnicodeSetTest(),
            new com.ibm.icu.dev.test.translit.CompoundTransliteratorTest(),
            new com.ibm.icu.dev.test.translit.UnicodeToHexTransliteratorTest(),
            new com.ibm.icu.dev.test.translit.HexToUnicodeTransliteratorTest(),
            new com.ibm.icu.dev.test.translit.JamoTest(),
            new com.ibm.icu.dev.test.translit.ErrorTest(),
            new com.ibm.icu.dev.test.translit.RoundTripTest(),
            new com.ibm.icu.dev.test.translit.ReplaceableTest()
        });
    }

}