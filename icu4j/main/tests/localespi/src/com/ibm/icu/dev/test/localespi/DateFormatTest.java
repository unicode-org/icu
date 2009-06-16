/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.DateFormat;
import java.text.ParseException;
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
            if (TestUtil.isProblematicIBMLocale(loc)) {
                logln("Skipped " + loc);
                continue;
            }
            checkGetInstance(DateFormat.FULL, DateFormat.LONG, loc);
            checkGetInstance(DateFormat.MEDIUM, -1, loc);
            checkGetInstance(1, DateFormat.SHORT, loc);
        }
    }

    private void checkGetInstance(int dstyle, int tstyle, Locale loc) {
        String method[] = new String[1];
        DateFormat df = getJDKInstance(dstyle, tstyle, loc, method);

        boolean isIcuImpl = (df instanceof com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU);

        if (TestUtil.isICUExtendedLocale(loc)) {
            if (!isIcuImpl) {
                errln("FAIL: " + method[0] + " returned JDK DateFormat for locale " + loc);
            }
        } else {
            if (isIcuImpl) {
                logln("INFO: " + method[0] + " returned ICU DateFormat for locale " + loc);
            }
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);
            DateFormat dfIcu = getJDKInstance(dstyle, tstyle, iculoc, null);
            if (isIcuImpl) {
                if (!df.equals(dfIcu)) {
                    errln("FAIL: " + method[0] + " returned ICU DateFormat for locale " + loc
                            + ", but different from the one for locale " + iculoc);
                }
            } else {
                if (!(dfIcu instanceof com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU)) {
                    errln("FAIL: " + method[0] + " returned JDK DateFormat for locale " + iculoc);
                }
            }
        }
    }

    private DateFormat getJDKInstance(int dstyle, int tstyle, Locale loc, String[] methodName) {
        DateFormat df;
        String method;
        if (dstyle < 0) {
            df = DateFormat.getTimeInstance(tstyle, loc);
            method = "getTimeInstance";
        } else if (tstyle < 0) {
            df = DateFormat.getDateInstance(dstyle, loc);
            method = "getDateInstance";
        } else {
            df = DateFormat.getDateTimeInstance(dstyle, tstyle, loc);
            method = "getDateTimeInstance";
        }
        if (methodName != null) {
            methodName[0] = method;
        }
        return df;
    }

    private com.ibm.icu.text.DateFormat getICUInstance(int dstyle, int tstyle, Locale loc, String[] methodName) {
        com.ibm.icu.text.DateFormat icudf;
        String method;
        if (dstyle < 0) {
            icudf = com.ibm.icu.text.DateFormat.getTimeInstance(tstyle, loc);
            method = "getTimeInstance";
        } else if (tstyle < 0) {
            icudf = com.ibm.icu.text.DateFormat.getDateInstance(dstyle, loc);
            method = "getDateInstance";
        } else {
            icudf = com.ibm.icu.text.DateFormat.getDateTimeInstance(dstyle, tstyle, loc);
            method = "getDateTimeInstance";
        }
        if (methodName != null) {
            methodName[0] = method;
        }
        return icudf;
    }

    /*
     * Testing the behavior of date format between ICU instance and its
     * equivalent created via the Locale SPI framework.
     */
    public void TestICUEquivalent() {
        Locale[] TEST_LOCALES = {
                new Locale("en", "US"),
                new Locale("it", "IT"),
                new Locale("iw", "IL"),
                new Locale("ja", "JP", "JP"),
                new Locale("th", "TH"),
                new Locale("zh", "TW"),
        };

        long[] TEST_DATES = {
                1199499330543L, // 2008-01-05T02:15:30.543Z
                1217001308085L, // 2008-07-25T15:55:08.085Z
        };

        for (Locale loc : TEST_LOCALES) {
            for (int dstyle = -1; dstyle <= 3; dstyle++) {
                for (int tstyle = -1; tstyle <= 3; tstyle++) {
                    if (tstyle == -1 && dstyle == -1) {
                        continue;
                    }
                    Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                    DateFormat df = getJDKInstance(dstyle, tstyle, iculoc, null);
                    com.ibm.icu.text.DateFormat icudf = getICUInstance(dstyle, tstyle, loc, null);

                    for (long t : TEST_DATES) {
                        // Format
                        Date d = new Date(t);
                        String dstr1 = df.format(d);
                        String dstr2 = icudf.format(d);

                        if (!dstr1.equals(dstr2)) {
                            errln("FAIL: Different format results for locale " + loc + " (dstyle=" + dstyle
                                    + ",tstyle=" + tstyle + ") at time " + t + " - JDK:" + dstr1
                                    + " ICU:" + dstr2);
                            continue;
                        }

                        // Parse
                        Date d1, d2;
                        try {
                            d1 = df.parse(dstr1);
                        } catch (ParseException e) {
                            errln("FAIL: ParseException thrown for JDK DateFormat for string "
                                    + dstr1 + "(locale=" + iculoc + ",dstyle=" + dstyle + ",tstyle=" + tstyle + ")");
                            continue;
                        }
                        try {
                            d2 = icudf.parse(dstr1);
                        } catch (ParseException e) {
                            errln("FAIL: ParseException thrown for ICU DateFormat for string "
                                    + dstr1 + "(locale=" + loc + ",dstyle=" + dstyle + ",tstyle=" + tstyle + ")");
                            continue;
                        }
                        if (!d1.equals(d2)) {
                            errln("FAIL: Different parse results for locale " + loc
                                    + " for date string " + dstr1 + " (dstyle=" + dstyle
                                    + ",tstyle=" + tstyle + ") at time " + t + " - JDK:" + dstr1
                                    + " ICU:" + dstr2);
                        }
                    }
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
