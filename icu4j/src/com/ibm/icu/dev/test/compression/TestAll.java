/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/compression/TestAll.java,v $
 * $Date: 2003/01/28 18:55:33 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.compression;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.bigdec.DiagBigDecimal;
import java.util.TimeZone;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestFmwk {
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        new TestAll().run(args);
    }

    public void TestCompression() throws Exception{
        run(new TestFmwk[] {
            new com.ibm.icu.dev.test.compression.DecompressionTest(),
            new com.ibm.icu.dev.test.compression.ExhaustiveTest()
                });
    }
}