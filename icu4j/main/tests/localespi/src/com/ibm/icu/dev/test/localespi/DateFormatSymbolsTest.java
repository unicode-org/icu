// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.DateFormatSymbols;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class DateFormatSymbolsTest extends TestFmwk {
    /*
     * Check if getInstance returns the ICU implementation.
     */
    @Test
    public void TestGetInstance() {
        for (Locale loc : DateFormatSymbols.getAvailableLocales()) {
            if (TestUtil.isExcluded(loc)) {
                logln("Skipped " + loc);
                continue;
            }

            DateFormatSymbols dfs = DateFormatSymbols.getInstance(loc);
            boolean isIcuImpl = (dfs instanceof com.ibm.icu.impl.jdkadapter.DateFormatSymbolsICU);

            if (TestUtil.isICUExtendedLocale(loc)) {
                if (!isIcuImpl) {
                    errln("FAIL: getInstance returned JDK DateFormatSymbols for locale " + loc);
                }
            } else if (isIcuImpl) {
                logln("INFO: getInstance returned ICU DateFormatSymbols for locale " + loc);
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                DateFormatSymbols dfsIcu = DateFormatSymbols.getInstance(iculoc);
                if (!dfs.equals(dfsIcu)) {
                    errln("FAIL: getInstance returned ICU DateFormatSymbols for locale " + loc
                            + ", but different from the one for locale " + iculoc);
                }
            }
        }
    }

    /*
     * Testing the contents of DateFormatSymbols between ICU instance and its
     * equivalent created via the Locale SPI framework.
     */
    @Test
    public void TestICUEquivalent() {
        Locale[] TEST_LOCALES = {
                new Locale("en", "US"),
                new Locale("es", "ES"),
                new Locale("ja", "JP", "JP"),
                new Locale("th", "TH"),
        };

        for (Locale loc : TEST_LOCALES) {
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);
            DateFormatSymbols jdkDfs = DateFormatSymbols.getInstance(iculoc);
            com.ibm.icu.text.DateFormatSymbols icuDfs = com.ibm.icu.text.DateFormatSymbols.getInstance(loc);

            compareArrays(jdkDfs.getAmPmStrings(), icuDfs.getAmPmStrings(), loc, "getAmPmStrings");
            compareArrays(jdkDfs.getEras(), icuDfs.getEras(), loc, "getEras");
            compareArrays(jdkDfs.getMonths(), icuDfs.getMonths(), loc, "getMonths");
            compareArrays(jdkDfs.getShortMonths(), icuDfs.getShortMonths(), loc, "getShortMonths");
            compareArrays(jdkDfs.getShortWeekdays(), icuDfs.getShortWeekdays(), loc, "getShortWeekdays");
            compareArrays(jdkDfs.getWeekdays(), icuDfs.getWeekdays(), loc, "getWeekdays");
            compareArrays(jdkDfs.getZoneStrings(), icuDfs.getZoneStrings(), loc, "getZoneStrings");
        }
    }

    /*
     * Testing setters
     */
    @Test
    public void TestSetSymbols() {
        // ICU's JDK DateFormatSymbols implementation for ja_JP locale
        DateFormatSymbols dfs = DateFormatSymbols.getInstance(new Locale("ja", "JP", "ICU"));

        // en_US is supported by JDK, so this is the JDK's own DateFormatSymbols
        Locale loc = new Locale("en", "US");
        DateFormatSymbols dfsEnUS = DateFormatSymbols.getInstance(loc);

        // Copying over all symbols
        dfs.setAmPmStrings(dfsEnUS.getAmPmStrings());
        dfs.setEras(dfsEnUS.getEras());
        dfs.setMonths(dfsEnUS.getMonths());
        dfs.setShortMonths(dfsEnUS.getShortMonths());
        dfs.setShortWeekdays(dfsEnUS.getShortWeekdays());
        dfs.setWeekdays(dfsEnUS.getWeekdays());
        dfs.setZoneStrings(dfsEnUS.getZoneStrings());

        compareArrays(dfs.getAmPmStrings(), dfsEnUS.getAmPmStrings(), loc, "getAmPmStrings");
        compareArrays(dfs.getEras(), dfsEnUS.getEras(), loc, "getEras");
        compareArrays(dfs.getMonths(), dfsEnUS.getMonths(), loc, "getMonths");
        compareArrays(dfs.getShortMonths(), dfsEnUS.getShortMonths(), loc, "getShortMonths");
        compareArrays(dfs.getShortWeekdays(), dfsEnUS.getShortWeekdays(), loc, "getShortWeekdays");
        compareArrays(dfs.getWeekdays(), dfsEnUS.getWeekdays(), loc, "getWeekdays");
        compareArrays(dfs.getZoneStrings(), dfsEnUS.getZoneStrings(), loc, "getZoneStrings");
    }

    private void compareArrays(Object jarray, Object iarray, Locale loc, String method) {
        if (jarray instanceof String[][]) {
            String[][] jaa = (String[][])jarray;
            String[][] iaa = (String[][])iarray;

            if (jaa.length != iaa.length || jaa[0].length != iaa[0].length) {
                errln("FAIL: Different array size returned by " + method + "for locale "
                        + loc + "(jdksize=" + jaa.length + "x" + jaa[0].length
                        + ",icusize=" + iaa.length + "x" + iaa[0].length + ")");
            }

            for (int i = 0; i < jaa.length; i++) {
                for (int j = 0; j < jaa[i].length; j++) {
                    if (!TestUtil.equals(jaa[i][j], iaa[i][j])) {
                        errln("FAIL: Different symbols returned by " + method + "for locale "
                                + loc + " at index " + i + "," + j
                                + " (jdk=" + jaa[i][j] + ",icu=" + iaa[i][j] + ")");
                    }
                }
            }

        } else {
            String[] ja = (String[])jarray;
            String[] ia = (String[])iarray;

            if (ja.length != ia.length) {
                errln("FAIL: Different array size returned by " + method + "for locale "
                        + loc + "(jdksize=" + ja.length
                        + ",icusize=" + ia.length + ")");
            } else {
                for (int i = 0; i < ja.length; i++) {
                    if (!TestUtil.equals(ja[i], ia[i])) {
                        errln("FAIL: Different symbols returned by " + method + "for locale "
                                + loc + " at index " + i + " (jdk=" + ja[i] + ",icu=" + ia[i] + ")");
                    }
                }
            }
        }
    }

    /*
     * Testing Nynorsk locales
     */
    @Test
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

    @Test
    public void TestCalendarKeyword() {
        // ICU provider variant is appended
        ULocale uloc = new ULocale("en_US_" + TestUtil.ICU_VARIANT + "@calendar=japanese");
        Locale loc = uloc.toLocale();
        DateFormatSymbols jdkDfs = DateFormatSymbols.getInstance(loc);
        com.ibm.icu.text.DateFormatSymbols icuDfs = com.ibm.icu.text.DateFormatSymbols.getInstance(uloc);

        // Check the length of era, so we can check if Japanese calendar is picked up
        if (jdkDfs.getEras().length != icuDfs.getEras().length) {
            errln("FAIL: Calendar keyword was ignored");
        }
    }
}
