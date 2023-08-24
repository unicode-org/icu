// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class DecimalFormatSymbolsTest extends TestFmwk {
    /*
     * Check if getInstance returns the ICU implementation.
     */
    @Test
    public void TestGetInstance() {
        for (Locale loc : DecimalFormatSymbols.getAvailableLocales()) {
            if (TestUtil.isExcluded(loc)) {
                logln("Skipped " + loc);
                continue;
            }

            DecimalFormatSymbols decfs = DecimalFormatSymbols.getInstance(loc);

            boolean isIcuImpl = (decfs instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatSymbolsICU);

            if (TestUtil.isICUExtendedLocale(loc)) {
                if (!isIcuImpl) {
                    errln("FAIL: getInstance returned JDK DecimalFormatSymbols for locale " + loc);
                }
            } else if (isIcuImpl) {
                logln("INFO: getInstance returned ICU DecimalFormatSymbols for locale " + loc);
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                DecimalFormatSymbols decfsIcu = DecimalFormatSymbols.getInstance(iculoc);
                if (!decfs.equals(decfsIcu)) {
                    errln("FAIL: getInstance returned ICU DecimalFormatSymbols for locale " + loc
                            + ", but different from the one for locale " + iculoc);
                }
            }
        }
    }

    /*
     * Testing the contents of DecimalFormatSymbols between ICU instance and its
     * equivalent created via the Locale SPI framework.
     */
    @Test
    public void TestICUEquivalent() {
        Locale[] TEST_LOCALES = {
                new Locale("en", "US"),
                new Locale("pt", "BR"),
                new Locale("ko", "KR"),
        };

        for (Locale loc : TEST_LOCALES) {
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);
            DecimalFormatSymbols jdkDecfs = DecimalFormatSymbols.getInstance(iculoc);
            com.ibm.icu.text.DecimalFormatSymbols icuDecfs = com.ibm.icu.text.DecimalFormatSymbols.getInstance(loc);

            Currency jdkCur = jdkDecfs.getCurrency();
            com.ibm.icu.util.Currency icuCur = icuDecfs.getCurrency();
            if ((jdkCur != null && icuCur == null)
                    || (jdkCur == null && icuCur != null)
                    || !jdkCur.getCurrencyCode().equals(icuCur.getCurrencyCode())) {
                errln("FAIL: Different results returned by getCurrency for locale " + loc);
            }

            checkEquivalence(jdkDecfs.getCurrencySymbol(), icuDecfs.getCurrencySymbol(), loc, "getCurrencySymbol");
            checkEquivalence(jdkDecfs.getDecimalSeparator(), icuDecfs.getDecimalSeparator(), loc, "getDecimalSeparator");
            checkEquivalence(jdkDecfs.getDigit(), icuDecfs.getDigit(), loc, "getDigit");
            checkEquivalence(jdkDecfs.getExponentSeparator(), icuDecfs.getExponentSeparator(), loc, "getExponentSeparator");
            checkEquivalence(jdkDecfs.getGroupingSeparator(), icuDecfs.getGroupingSeparator(), loc, "getGroupingSeparator");
            checkEquivalence(jdkDecfs.getInfinity(), icuDecfs.getInfinity(), loc, "getInfinity");
            checkEquivalence(jdkDecfs.getInternationalCurrencySymbol(), icuDecfs.getInternationalCurrencySymbol(), loc, "getInternationalCurrencySymbol");
            checkEquivalence(jdkDecfs.getMinusSign(), icuDecfs.getMinusSign(), loc, "getMinusSign");
            checkEquivalence(jdkDecfs.getMonetaryDecimalSeparator(), icuDecfs.getMonetaryDecimalSeparator(), loc, "getMonetaryDecimalSeparator");
            checkEquivalence(jdkDecfs.getNaN(), icuDecfs.getNaN(), loc, "getNaN");
            checkEquivalence(jdkDecfs.getPatternSeparator(), icuDecfs.getPatternSeparator(), loc, "getPatternSeparator");
            checkEquivalence(jdkDecfs.getPercent(), icuDecfs.getPercent(), loc, "getPercent");
            checkEquivalence(jdkDecfs.getPerMill(), icuDecfs.getPerMill(), loc, "getPerMill");
            checkEquivalence(jdkDecfs.getZeroDigit(), icuDecfs.getZeroDigit(), loc, "getZeroDigit");
        }
    }

    private void checkEquivalence(Object jo, Object io, Locale loc, String method) {
        if (!jo.equals(io)) {
            errln("FAIL: Different results returned by " + method + " for locale "
                    + loc + " (jdk=" + jo + ",icu=" + io + ")");
        }
    }

    /*
     * Testing setters
     */
    @Test
    public void TestSetSymbols() {
        // ICU's JDK DecimalFormatSymbols implementation for de_DE locale
        DecimalFormatSymbols decfs = DecimalFormatSymbols.getInstance(TestUtil.toICUExtendedLocale(new Locale("de", "DE")));

        // en_US is supported by JDK, so this is the JDK's own DecimalFormatSymbols
        Locale loc = new Locale("en", "US");
        DecimalFormatSymbols decfsEnUS = DecimalFormatSymbols.getInstance(loc);

        // Copying over all symbols
        decfs.setCurrency(decfsEnUS.getCurrency());

        decfs.setCurrencySymbol(decfsEnUS.getCurrencySymbol());
        decfs.setDecimalSeparator(decfsEnUS.getDecimalSeparator());
        decfs.setDigit(decfsEnUS.getDigit());
        decfs.setExponentSeparator(decfsEnUS.getExponentSeparator());
        decfs.setGroupingSeparator(decfsEnUS.getGroupingSeparator());
        decfs.setInfinity(decfsEnUS.getInfinity());
        decfs.setInternationalCurrencySymbol(decfsEnUS.getInternationalCurrencySymbol());
        decfs.setMinusSign(decfsEnUS.getMinusSign());
        decfs.setMonetaryDecimalSeparator(decfsEnUS.getMonetaryDecimalSeparator());
        decfs.setNaN(decfsEnUS.getNaN());
        decfs.setPatternSeparator(decfsEnUS.getPatternSeparator());
        decfs.setPercent(decfsEnUS.getPercent());
        decfs.setPerMill(decfsEnUS.getPerMill());
        decfs.setZeroDigit(decfsEnUS.getZeroDigit());

        // Check
        Currency cur = decfs.getCurrency();
        Currency curEnUS = decfsEnUS.getCurrency();
        if ((cur != null && curEnUS == null)
                || (cur == null && curEnUS != null)
                || !cur.equals(curEnUS)) {
            errln("FAIL: Different results returned by getCurrency");
        }

        checkEquivalence(decfs.getCurrencySymbol(), decfsEnUS.getCurrencySymbol(), loc, "getCurrencySymbol");
        checkEquivalence(decfs.getDecimalSeparator(), decfsEnUS.getDecimalSeparator(), loc, "getDecimalSeparator");
        checkEquivalence(decfs.getDigit(), decfsEnUS.getDigit(), loc, "getDigit");
        checkEquivalence(decfs.getExponentSeparator(), decfsEnUS.getExponentSeparator(), loc, "getExponentSeparator");
        checkEquivalence(decfs.getGroupingSeparator(), decfsEnUS.getGroupingSeparator(), loc, "getGroupingSeparator");
        checkEquivalence(decfs.getInfinity(), decfsEnUS.getInfinity(), loc, "getInfinity");
        checkEquivalence(decfs.getInternationalCurrencySymbol(), decfsEnUS.getInternationalCurrencySymbol(), loc, "getInternationalCurrencySymbol");
        checkEquivalence(decfs.getMinusSign(), decfsEnUS.getMinusSign(), loc, "getMinusSign");
        checkEquivalence(decfs.getMonetaryDecimalSeparator(), decfsEnUS.getMonetaryDecimalSeparator(), loc, "getMonetaryDecimalSeparator");
        checkEquivalence(decfs.getNaN(), decfsEnUS.getNaN(), loc, "getNaN");
        checkEquivalence(decfs.getPatternSeparator(), decfsEnUS.getPatternSeparator(), loc, "getPatternSeparator");
        checkEquivalence(decfs.getPercent(), decfsEnUS.getPercent(), loc, "getPercent");
        checkEquivalence(decfs.getPerMill(), decfsEnUS.getPerMill(), loc, "getPerMill");
        checkEquivalence(decfs.getZeroDigit(), decfsEnUS.getZeroDigit(), loc, "getZeroDigit");
    }

    @Test
    public void TestKeywords() {
        // ICU provider variant is appended
        ULocale uloc = new ULocale("en_US_" + TestUtil.ICU_VARIANT + "@numbers=Arab;currency=EUR");
        Locale loc = uloc.toLocale();
        DecimalFormatSymbols jdkDecfs = DecimalFormatSymbols.getInstance(loc);
        com.ibm.icu.text.DecimalFormatSymbols icuDecfs = com.ibm.icu.text.DecimalFormatSymbols.getInstance(uloc);
        // Check digit 0
        if (jdkDecfs.getDigit() != icuDecfs.getDigit()) {
            errln("FAIL: Different decimal digit - via JDK: " + jdkDecfs.getDigit() + ", with ICU: " + icuDecfs.getDigit());
        }

        String jdkCurrencyCode = jdkDecfs.getCurrency().getCurrencyCode();
        String icuCurrencyCode = icuDecfs.getCurrency().getCurrencyCode();
        if (!jdkCurrencyCode.equals(icuCurrencyCode)) {
            errln("FAIL: Different currency code - via JDK: " + jdkCurrencyCode + ", with ICU: " + icuCurrencyCode);
        }
    }
}
