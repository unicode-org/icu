/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/format/TestAll.java,v $
 * $Date: 2003/01/28 18:55:33 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.format;
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

    public void TestRuleBasedNumberFormat() throws Exception {
            run(new TestFmwk[] {
                new com.ibm.icu.dev.test.format.RbnfTest(),
                new com.ibm.icu.dev.test.format.RbnfRoundTripTest()
                    });
    }

    public void TestNumberFormat() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.dev.test.format.IntlTestNumberFormat(),
            new com.ibm.icu.dev.test.format.IntlTestNumberFormatAPI(),
            new com.ibm.icu.dev.test.format.NumberFormatTest(),
            new com.ibm.icu.dev.test.format.NumberFormatRoundTripTest(),
            new com.ibm.icu.dev.test.format.NumberRegression(),
            new com.ibm.icu.dev.test.format.NumberFormatRegressionTest(),
            new com.ibm.icu.dev.test.format.IntlTestDecimalFormatAPI(),
            new com.ibm.icu.dev.test.format.IntlTestDecimalFormatAPIC(),
            new com.ibm.icu.dev.test.format.IntlTestDecimalFormatSymbols(),
            new com.ibm.icu.dev.test.format.IntlTestDecimalFormatSymbolsC()
                });
    }

    public void TestDateFormat() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.dev.test.format.DateFormatMiscTests(),
            new com.ibm.icu.dev.test.format.DateFormatRegressionTest(),
            new com.ibm.icu.dev.test.format.DateFormatRoundTripTest(),
            new com.ibm.icu.dev.test.format.DateFormatTest(),
            new com.ibm.icu.dev.test.format.IntlTestDateFormat(),
            new com.ibm.icu.dev.test.format.IntlTestDateFormatAPI(),
            new com.ibm.icu.dev.test.format.IntlTestDateFormatAPIC(),
            new com.ibm.icu.dev.test.format.IntlTestDateFormatSymbols(),
            new com.ibm.icu.dev.test.format.IntlTestSimpleDateFormatAPI(),
            new com.ibm.icu.dev.test.format.DateFormatRegressionTestJ()
                });
    }
}