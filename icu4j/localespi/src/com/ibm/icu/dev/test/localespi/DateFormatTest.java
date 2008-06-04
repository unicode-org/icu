/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.DateFormat;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

public class DateFormatTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new DateFormatTest().run(args);
    }

    public void TestGetInstance() {
        for (Locale loc : TestUtil.getICULocales()) {
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
        if (TestUtil.isICUOnly(loc)) {
            if (!(df instanceof com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU)) {
                errln("FAIL: " + method + " returned JDK DateFormat for locale " + loc);
            }
        } else {
            if (df instanceof com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU) {
                logln("INFO: " + method + " returned ICU DateFormat for locale " + loc);
            } else {
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                if (dstyle < 0) {
                    df = DateFormat.getTimeInstance(DateFormat.SHORT, iculoc);
                } else if (tstyle < 0) {
                    df = DateFormat.getDateInstance(DateFormat.MEDIUM, iculoc);
                } else {
                    df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, iculoc);
                }
                if (!(df instanceof com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU)) {
                    errln("FAIL: " + method + " returned JDK DateFormat for locale " + iculoc);
                }
            }
        }
    }
}
