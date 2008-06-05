/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

public class DateFormatTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new DateFormatTest().run(args);
    }

    /*
     * Check if getInstance returns the ICU implementation.
     */
    public void TestGetInstance() {
        for (Locale loc : DateFormat.getAvailableLocales()) {
            checkGetInstance(DateFormat.FULL, DateFormat.LONG, loc);
            checkGetInstance(DateFormat.MEDIUM, -1, loc);
            checkGetInstance(1, DateFormat.SHORT, loc);
        }
    }

    private void checkGetInstance(int dstyle, int tstyle, Locale loc) {
        DateFormat df;
        String method;
        if (dstyle < 0) {
            df = DateFormat.getTimeInstance(DateFormat.SHORT, loc);
            method = "getTimeInstance";
        } else if (tstyle < 0) {
            df = DateFormat.getDateInstance(DateFormat.MEDIUM, loc);
            method = "getDateInstance";
        } else {
            df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, loc);
            method = "getDateTimeInstance";
        }

        boolean isIcuImpl = (df instanceof com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU);

        if (TestUtil.isICUExtendedLocale(loc)) {
            if (!isIcuImpl) {
                errln("FAIL: " + method + " returned JDK DateFormat for locale " + loc);
            }
        } else {
            if (isIcuImpl) {
                logln("INFO: " + method + " returned ICU DateFormat for locale " + loc);
            }
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);
            DateFormat dfIcu;
            if (dstyle < 0) {
                dfIcu = DateFormat.getTimeInstance(DateFormat.SHORT, iculoc);
            } else if (tstyle < 0) {
                dfIcu = DateFormat.getDateInstance(DateFormat.MEDIUM, iculoc);
            } else {
                dfIcu = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, iculoc);
            }
            if (isIcuImpl) {
                if (!df.equals(dfIcu)) {
                    errln("FAIL: " + method + " returned ICU DateFormat for locale " + loc
                            + ", but different from the one for locale " + iculoc);
                }
            } else {
                if (!(dfIcu instanceof com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU)) {
                    errln("FAIL: " + method + " returned JDK DateFormat for locale " + iculoc);
                }
            }
        }
    }

    /*
     * Check if ICU DateFormatProvider uses Thai native digit for Locale
     * th_TH_TH.
     */
    public void TestThaiDigit() {
        Locale thTHTH = new Locale("th", "TH", "TH");
        String pattern = "yyyy-MM-dd";

        DateFormat dfmt = DateFormat.getDateInstance(DateFormat.FULL, thTHTH);
        DateFormat dfmtIcu = DateFormat.getDateInstance(DateFormat.FULL, TestUtil.toICUExtendedLocale(thTHTH));

        ((java.text.SimpleDateFormat)dfmt).applyPattern(pattern);
        ((java.text.SimpleDateFormat)dfmtIcu).applyPattern(pattern);

        Date d = new Date();
        String str1 = dfmt.format(d);
        String str2 = dfmtIcu.format(d);

        if (!str1.equals(str2)) {
            errln("FAIL: ICU DateFormat returned a result different from JDK for th_TH_TH");
        }
    }
}
