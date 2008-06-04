/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.DateFormatSymbols;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

public class DateFormatSymbolsTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new DateFormatSymbolsTest().run(args);
    }

    public void TestGetInstance() {
        for (Locale loc : TestUtil.getICULocales()) {
            DateFormatSymbols dfs = DateFormatSymbols.getInstance(loc);
            if (TestUtil.isICUOnly(loc)) {
                if (!(dfs instanceof com.ibm.icu.impl.jdkadapter.DateFormatSymbolsICU)) {
                    errln("FAIL: getInstance returned JDK DateFormatSymbols for locale " + loc);
                }
            } else {
                if (dfs instanceof com.ibm.icu.impl.jdkadapter.DateFormatSymbolsICU) {
                    logln("INFO: getInstance returned ICU DateFormatSymbols for locale " + loc);
                } else {
                    Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                    dfs = DateFormatSymbols.getInstance(iculoc);
                    if (!(dfs instanceof com.ibm.icu.impl.jdkadapter.DateFormatSymbolsICU)) {
                        errln("FAIL: getInstance returned JDK DateFormatSymbols for locale " + iculoc);
                    }
                }
            }
        }
    }
}
