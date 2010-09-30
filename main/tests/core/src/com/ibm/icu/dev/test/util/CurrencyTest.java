/*
 **********************************************************************
 * Copyright (c) 2002-2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Alan Liu
 * Created: December 18 2002
 * Since: ICU 2.4
 **********************************************************************
 */

package com.ibm.icu.dev.test.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

/**
 * @test
 * @summary General test of Currency
 */
public class CurrencyTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new CurrencyTest().run(args);
    }

    /**
     * Test of basic API.
     */
    public void TestAPI() {
        Currency usd = Currency.getInstance("USD");
        /*int hash = */usd.hashCode();
        Currency jpy = Currency.getInstance("JPY");
        if (usd.equals(jpy)) {
            errln("FAIL: USD == JPY");
        }
        if (usd.equals("abc")) {
            errln("FAIL: USD == (String)");
        }
        if (usd.equals(null)) {
            errln("FAIL: USD == (null)");
        }
        if (!usd.equals(usd)) {
            errln("FAIL: USD != USD");
        }

        try {
            Currency nullCurrency = Currency.getInstance((String)null);
            errln("FAIL: Expected getInstance(null) to throw "
                    + "a NullPointerException, but returned " + nullCurrency);
        } catch (NullPointerException npe) {
            logln("PASS: getInstance(null) threw a NullPointerException");
        }

        try {
            Currency bogusCurrency = Currency.getInstance("BOGUS");
            errln("FAIL: Expected getInstance(\"BOGUS\") to throw "
                    + "an IllegalArgumentException, but returned " + bogusCurrency);
        } catch (IllegalArgumentException iae) {
            logln("PASS: getInstance(\"BOGUS\") threw an IllegalArgumentException");
        }

        Locale[] avail = Currency.getAvailableLocales();
        if(avail==null){
            errln("FAIL: getAvailableLocales returned null");
        }

        try {
            usd.getName(ULocale.US, 5, new boolean[1]);
            errln("expected getName with invalid type parameter to throw exception");
        }
        catch (Exception e) {
            logln("PASS: getName failed as expected");
        }
    }

    /**
     * Test registration.
     */
    public void TestRegistration() {
        final Currency jpy = Currency.getInstance("JPY");
        final Currency usd = Currency.getInstance(Locale.US);

    try {
      Currency.unregister(null); // should fail, coverage
      errln("expected unregister of null to throw exception");
    }
    catch (Exception e) {
        logln("PASS: unregister of null failed as expected");
    }

    if (Currency.unregister("")) { // coverage
      errln("unregister before register erroneously succeeded");
    }

        ULocale fu_FU = new ULocale("fu_FU");

        Object key1 = Currency.registerInstance(jpy, ULocale.US);
        Object key2 = Currency.registerInstance(jpy, fu_FU);

        Currency nus = Currency.getInstance(Locale.US);
        if (!nus.equals(jpy)) {
            errln("expected " + jpy + " but got: " + nus);
        }

        // converage, make sure default factory works
        Currency nus1 = Currency.getInstance(Locale.JAPAN);
        if (!nus1.equals(jpy)) {
            errln("expected " + jpy + " but got: " + nus1);
        }

        ULocale[] locales = Currency.getAvailableULocales();
        boolean found = false;
        for (int i = 0; i < locales.length; ++i) {
            if (locales[i].equals(fu_FU)) {
                found = true;
                break;
            }
        }
        if (!found) {
            errln("did not find locale" + fu_FU + " in currency locales");
        }

        if (!Currency.unregister(key1)) {
            errln("unable to unregister currency using key1");
        }
        if (!Currency.unregister(key2)) {
            errln("unable to unregister currency using key2");
        }

        Currency nus2 = Currency.getInstance(Locale.US);
        if (!nus2.equals(usd)) {
            errln("expected " + usd + " but got: " + nus2);
        }

        locales = Currency.getAvailableULocales();
        found = false;
        for (int i = 0; i < locales.length; ++i) {
            if (locales[i].equals(fu_FU)) {
                found = true;
                break;
            }
        }
        if (found) {
            errln("found locale" + fu_FU + " in currency locales after unregister");
        }
    }

    /**
     * Test names.
     */
    public void TestNames() {
        // Do a basic check of getName()
        // USD { "US$", "US Dollar"            } // 04/04/1792-
        ULocale en = ULocale.ENGLISH;
        boolean[] isChoiceFormat = new boolean[1];
        Currency usd = Currency.getInstance("USD");
        // Warning: HARD-CODED LOCALE DATA in this test.  If it fails, CHECK
        // THE LOCALE DATA before diving into the code.
        if (!noData()) {
            assertEquals("USD.getName(SYMBOL_NAME)",
                         "$",
                         usd.getName(en, Currency.SYMBOL_NAME, isChoiceFormat));
            assertEquals("USD.getName(LONG_NAME)",
                         "US Dollar",
                         usd.getName(en, Currency.LONG_NAME, isChoiceFormat));
        }
        // TODO add more tests later
    }

    public void TestCoverage() {
        Currency usd = Currency.getInstance("USD");
        if (!noData()) {
        assertEquals("USD.getSymbol()",
                "$",
                usd.getSymbol());
        }
        assertEquals("USD.getLocale()",
                ULocale.ROOT,
                usd.getLocale(null));
    }

    public void TestCurrencyKeyword() {
        ULocale locale = new ULocale("th_TH@collation=traditional;currency=QQQ");
        Currency currency = Currency.getInstance(locale);
        String result = currency.getCurrencyCode();
        if (!"QQQ".equals(result)) {
            errln("got unexpected currency: " + result);
        }
    }

    public void TestAvailableCurrencyCodes() {
        String[][] tests = {
            { "eo_AM", "1950-01-05" },
            { "eo_AM", "1969-12-31", "SUR" },
            { "eo_AM", "1991-12-26", "RUR" },
            { "eo_AM", "2000-12-23", "AMD" },
            { "eo_AD", "2000-12-23", "EUR", "ESP", "FRF", "ADP" },
            { "eo_AD", "1969-12-31", "ESP", "FRF", "ADP" },
            { "eo_AD", "1950-01-05", "ESP", "ADP" },
            { "eo_AD", "1900-01-17", "ESP" },
            { "eo_UA", "1994-12-25" },
            { "eo_QQ", "1969-12-31" },
            { "eo_AO", "2000-12-23", "AOA" },
            { "eo_AO", "1995-12-25", "AOR", "AON" },
            { "eo_AO", "1990-12-26", "AON", "AOK" },
            { "eo_AO", "1979-12-29", "AOK" },
            { "eo_AO", "1969-12-31" },
            { "eo_DE@currency=DEM", "2000-12-23", "EUR", "DEM" },
            { "eo-DE-u-cu-dem", "2000-12-23", "EUR", "DEM" },
            { "en_US", null, "USD" },
            { "en_US_PREEURO", null, "USD" },
            { "en_US_Q", null, "USD" },
        };
        
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (String[] test : tests) {
            ULocale locale = new ULocale(test[0]);
            String timeString = test[1];
            Date date;
            if (timeString == null) {
                date = new Date();
                timeString = "today";
            } else {
                try {
                    date = fmt.parse(timeString);
                } catch (Exception e) {
                    fail("could not parse date: " + timeString);
                    continue;
                }
            }
            String[] expected = null;
            if (test.length > 2) {
                expected = new String[test.length - 2];
                System.arraycopy(test, 2, expected, 0, expected.length);
            }
            String[] actual = Currency.getAvailableCurrencyCodes(locale, date);
            
            // Order is not important as of 4.4.  We never documented that it was.
            Set<String> expectedSet = new HashSet<String>();
            if (expected != null) {
                expectedSet.addAll(Arrays.asList(expected));
            }
            Set<String> actualSet = new HashSet<String>();
            if (actual != null) {
                actualSet.addAll(Arrays.asList(actual));
            }
            assertEquals(locale + " on " + timeString, expectedSet, actualSet);
        }
    }

    public void TestDeprecatedCurrencyFormat() {
        // bug 5952
        Locale locale = new Locale("sr", "QQ");
        DecimalFormatSymbols icuSymbols = new 
        com.ibm.icu.text.DecimalFormatSymbols(locale);
        String symbol = icuSymbols.getCurrencySymbol();
        Currency currency = icuSymbols.getCurrency();
        String expectCur = null;
        String expectSym = "\u00A4";
        if(!symbol.toString().equals(expectSym) || currency != null) {
            errln("for " + locale + " expected " + expectSym+"/"+expectCur + " but got " + symbol+"/"+currency);
        } else {
            logln("for " + locale + " expected " + expectSym+"/"+expectCur + " and got " + symbol+"/"+currency);
        }
    }
    
    public void TestGetKeywordValues(){

        final String[][] PREFERRED = {
            {"root",                 },
            {"und",                  },
            {"und_ZZ",               },
            {"en_US",           "USD"},
            {"en_029",               },
            {"en_TH",           "THB"},
            {"de",              "EUR"},
            {"de_DE",           "EUR"},
            {"de_ZZ",                },
            {"ar",              "EGP"},
            {"ar_PS",           "JOD", "ILS"},
            {"en@currency=CAD",     "USD"},
            {"fr@currency=ZZZ",     "EUR"},
            {"de_DE@currency=DEM",  "EUR"},
        };

        String[] ALL = Currency.getKeywordValuesForLocale("currency", ULocale.getDefault(), false);
        HashSet ALLSET = new HashSet();
        for (int i = 0; i < ALL.length; i++) {
            ALLSET.add(ALL[i]);
        }
        
        for (int i = 0; i < PREFERRED.length; i++) {
            ULocale loc = new ULocale(PREFERRED[i][0]);
            String[] expected = new String[PREFERRED[i].length - 1];
            System.arraycopy(PREFERRED[i], 1, expected, 0, expected.length);
            String[] pref = Currency.getKeywordValuesForLocale("currency", loc, true);
            assertEquals(loc.toString(), expected, pref);

            String[] all = Currency.getKeywordValuesForLocale("currency", loc, false);
            // The items in the two collections should match (ignore order, 
            // behavior change from 4.3.3)
            Set<String> returnedSet = new HashSet<String>();
            returnedSet.addAll(Arrays.asList(all));
            assertEquals(loc.toString(), ALLSET, returnedSet);
        }
    }
}
