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

    /*
     * Check if getInstance returns the ICU implementation.
     */
    public void TestGetInstance() {
        for (Locale loc : DateFormatSymbols.getAvailableLocales()) {
            DateFormatSymbols dfs = DateFormatSymbols.getInstance(loc);

            boolean isIcuImpl = (dfs instanceof com.ibm.icu.impl.jdkadapter.DateFormatSymbolsICU);

            if (TestUtil.isICUExtendedLocale(loc)) {
                if (!isIcuImpl) {
                    errln("FAIL: getInstance returned JDK DateFormatSymbols for locale " + loc);
                }
            } else {
                if (isIcuImpl) {
                    logln("INFO: getInstance returned ICU DateFormatSymbols for locale " + loc);
                }
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                DateFormatSymbols dfsIcu = DateFormatSymbols.getInstance(iculoc);
                if (isIcuImpl) {
                    if (!dfs.equals(dfsIcu)) {
                        errln("FAIL: getInstance returned ICU DateFormatSymbols for locale " + loc
                                + ", but different from the one for locale " + iculoc);
                    }
                } else {
                    if (!(dfsIcu instanceof com.ibm.icu.impl.jdkadapter.DateFormatSymbolsICU)) {
                        errln("FAIL: getInstance returned JDK DateFormatSymbols for locale " + iculoc);
                    }
                }
            }
        }
    }

    /*
     * Testing Nynorsk locales
     */
    public void TestNynorsk() {
        Locale nnNO = new Locale("nn", "NO");
        Locale noNONY = new Locale("no", "NO", "NY");

        DateFormatSymbols dfs_nnNO = DateFormatSymbols.getInstance(nnNO);
        DateFormatSymbols dfs_nnNO_ICU = DateFormatSymbols.getInstance(TestUtil.toICUExtendedLocale(nnNO));
        DateFormatSymbols dfs_noNONY_ICU = DateFormatSymbols.getInstance(TestUtil.toICUExtendedLocale(noNONY));

        // Weekday names should be identical for these three.
        // If data is taken from no/nb, then this check will fail.
        String[] dow_nnNO = dfs_nnNO.getWeekdays();
        String[] dow_nnNO_ICU = dfs_nnNO_ICU.getWeekdays();
        String[] dow_noNONY_ICU = dfs_noNONY_ICU.getWeekdays();

        for (int i = 1; i < dow_nnNO.length; i++) {
            if (!dow_nnNO[i].equals(dow_nnNO_ICU[i])) {
                errln("FAIL: Different weekday name - index=" + i
                        + ", nn_NO:" + dow_nnNO[i] + ", nn_NO_ICU:" + dow_nnNO_ICU[i]);
            }
        }
        for (int i = 1; i < dow_nnNO.length; i++) {
            if (!dow_nnNO[i].equals(dow_noNONY_ICU[i])) {
                errln("FAIL: Different weekday name - index=" + i
                        + ", nn_NO:" + dow_nnNO[i] + ", no_NO_NY_ICU:" + dow_nnNO_ICU[i]);
            }
        }
    }
}
