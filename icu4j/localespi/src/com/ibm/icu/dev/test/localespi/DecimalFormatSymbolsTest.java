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

public class DecimalFormatSymbolsTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new DecimalFormatSymbolsTest().run(args);
    }

    public void TestGetInstance() {
        for (Locale loc : TestUtil.getICULocales()) {
            DecimalFormatSymbols decfs = DecimalFormatSymbols.getInstance(loc);
            if (TestUtil.isICUOnly(loc)) {
                if (!(decfs instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatSymbolsICU)) {
                    errln("FAIL: getInstance returned JDK DecimalFormatSymbols for locale " + loc);
                }
            } else {
                if (decfs instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatSymbolsICU) {
                    logln("INFO: getInstance returned ICU DecimalFormatSymbols for locale " + loc);
                } else {
                    Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                    decfs = DecimalFormatSymbols.getInstance(iculoc);
                    if (!(decfs instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatSymbolsICU)) {
                        errln("FAIL: getInstance returned JDK DecimalFormatSymbols for locale " + iculoc);
                    }
                }
            }
        }
    }

}
