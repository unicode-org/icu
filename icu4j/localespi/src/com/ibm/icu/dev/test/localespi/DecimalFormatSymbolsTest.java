/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DateFormatSymbols;

public class DecimalFormatSymbolsTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new DecimalFormatSymbolsTest().run(args);
    }

    /*
     * Check if getInstance returns the ICU implementation.
     */
    public void TestGetInstance() {
        for (Locale loc : DateFormatSymbols.getAvailableLocales()) {
            DecimalFormatSymbols decfs = DecimalFormatSymbols.getInstance(loc);

            boolean isIcuImpl = (decfs instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatSymbolsICU);

            if (TestUtil.isICUExtendedLocale(loc)) {
                if (!isIcuImpl) {
                    errln("FAIL: getInstance returned JDK DecimalFormatSymbols for locale " + loc);
                }
            } else {
                if (isIcuImpl) {
                    logln("INFO: getInstance returned ICU DecimalFormatSymbols for locale " + loc);
                }
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                DecimalFormatSymbols decfsIcu = DecimalFormatSymbols.getInstance(iculoc);
                if (isIcuImpl) {
                    if (!decfs.equals(decfsIcu)) {
                        errln("FAIL: getInstance returned ICU DecimalFormatSymbols for locale " + loc
                                + ", but different from the one for locale " + iculoc);
                    }
                } else {
                    if (!(decfsIcu instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatSymbolsICU)) {
                        errln("FAIL: getInstance returned JDK DecimalFormatSymbols for locale " + iculoc);
                    }
                }
            }
        }
    }
}
