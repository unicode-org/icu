/*
 **********************************************************************
 * Copyright (c) 2002-2011, International Business Machines
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
import java.util.Map;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.text.CurrencyMetaInfo;
import com.ibm.icu.text.CurrencyDisplayNames;
import com.ibm.icu.impl.CurrencyData;

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
        
        Locale[] locs = Currency.getAvailableLocales();
        found = false;
        for (int i = 0; i < locs.length; ++i) {
            if (locs[i].equals(fu_FU)) {
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
    
    // Provide better code coverage for the CurrencyDisplayNames class
    public void TestCurrencyDisplayNames() {
        if (!CurrencyDisplayNames.hasData()) {
            errln("hasData() should return true.");
        }
    }
    
    // Provide better code coverage for the CurrencyData class
    public void TestCurrencyData() {
        CurrencyData.DefaultInfo info_fallback = (CurrencyData.DefaultInfo)CurrencyData.DefaultInfo.getWithFallback(true);
        if (info_fallback == null) {
            errln("getWithFallback() returned null.");
            return;
        }
        
        CurrencyData.DefaultInfo info_nofallback = (CurrencyData.DefaultInfo)CurrencyData.DefaultInfo.getWithFallback(false);
        if (info_nofallback == null) {
            errln("getWithFallback() returned null.");
            return;
        }
        
        if (!info_fallback.getName("isoCode").equals("isoCode") || info_nofallback.getName("isoCode") != null) {
            errln("Error calling getName().");
            return;
        }
        
        if (!info_fallback.getPluralName("isoCode", "type").equals("isoCode") || info_nofallback.getPluralName("isoCode", "type") != null) {
            errln("Error calling getPluralName().");
            return;
        }
        
        if (!info_fallback.getSymbol("isoCode").equals("isoCode") || info_nofallback.getSymbol("isoCode") != null) {
            errln("Error calling getSymbol().");
            return;
        }
        
        if (!info_fallback.symbolMap().isEmpty()) {
            errln("symbolMap() should return empty map.");
            return;
        }
        
        if (!info_fallback.nameMap().isEmpty()) {
            errln("nameMap() should return empty map.");
            return;
        }
        
        if (!info_fallback.getUnitPatterns().isEmpty() || info_nofallback.getUnitPatterns() != null) {
            errln("Error calling getUnitPatterns().");
            return;
        }
        
        if (!info_fallback.getSpacingInfo().equals((CurrencyData.CurrencySpacingInfo.DEFAULT)) ||
                info_nofallback.getSpacingInfo() != null) {
            errln("Error calling getSpacingInfo().");
            return;
        }
        
        if (info_fallback.getLocale() != ULocale.ROOT) {
            errln("Error calling getLocale().");
            return;
        }
        
        if (info_fallback.getFormatInfo("isoCode") != null) {
            errln("Error calling getFormatInfo().");
            return;
        }
    }
    
    // Provide better code coverage for the CurrencyMetaInfo class
    public void TestCurrencyMetaInfo() {
        CurrencyMetaInfo metainfo = CurrencyMetaInfo.getInstance();
        if (metainfo == null) {
            errln("Unable to get CurrencyMetaInfo instance.");
            return;
        }
        
        if (!CurrencyMetaInfo.hasData()) {
            errln("hasData() should note return false.");
            return;
        }
        
        CurrencyMetaInfo.CurrencyFilter filter;
        CurrencyMetaInfo.CurrencyInfo info;
        CurrencyMetaInfo.CurrencyDigits digits;
        
        { // CurrencyFilter
            filter = CurrencyMetaInfo.CurrencyFilter.onCurrency("currency");
            CurrencyMetaInfo.CurrencyFilter filter2 = CurrencyMetaInfo.CurrencyFilter.onCurrency("test");
            if (filter == null) {
                errln("Unable to create CurrencyFilter.");
                return;
            }
            
            if (filter.equals(new Object())) {
                errln("filter should not equal to Object");
                return;
            }
            
            if (filter.equals(filter2)) {
                errln("filter should not equal filter2");
                return;
            }
            
            if (filter.hashCode() == 0) {
                errln("Error getting filter hashcode");
                return;
            }
            
            if (filter.toString() == null) {
                errln("Error calling toString()");
                return;
            }
        }
            
        { // CurrencyInfo
            info = new CurrencyMetaInfo.CurrencyInfo("region", "code", 0, 1, 1);
            if (info == null) {
                errln("Error creating CurrencyInfo.");
                return;
            }
            
            if (info.toString() == null) {
                errln("Error calling toString()");
                return;
            }
        }
        
        { // CurrencyDigits
            digits = metainfo.currencyDigits("isoCode");
            if (digits == null) {
                errln("Unable to get CurrencyDigits.");
                return;
            }
            
            if (digits.toString() == null) {
                errln("Error calling toString()");
                return;
            }
        }
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

    public void TestIsAvailable() {
        Date d1995 = new Date(788918400000L);   // 1995-01-01 00:00 GMT
        Date d2000 = new Date(946684800000L);   // 2000-01-01 00:00 GMT
        Date d2005 = new Date(1104537600000L);  // 2005-01-01 00:00 GMT

        assertTrue("USD all time", Currency.isAvailable("USD", null, null));
        assertTrue("USD before 1995", Currency.isAvailable("USD", null, d1995));
        assertTrue("USD 1995-2005", Currency.isAvailable("USD", d1995, d2005));
        assertTrue("USD after 2005", Currency.isAvailable("USD", d2005, null));
        assertTrue("USD on 2005-01-01", Currency.isAvailable("USD", d2005, d2005));

        assertTrue("usd all time", Currency.isAvailable("usd", null, null));

        assertTrue("DEM all time", Currency.isAvailable("DEM", null, null));
        assertTrue("DEM before 1995", Currency.isAvailable("DEM", null, d1995));
        assertTrue("DEM 1995-2000", Currency.isAvailable("DEM", d1995, d2000));
        assertTrue("DEM 1995-2005", Currency.isAvailable("DEM", d1995, d2005));
        assertFalse("DEM after 2005", Currency.isAvailable("DEM", d2005, null));
        assertTrue("DEM on 2000-01-01", Currency.isAvailable("DEM", d2000, d2000));
        assertFalse("DEM on 2005-01-01", Currency.isAvailable("DEM", d2005, d2005));

        assertFalse("XXX unknown code", Currency.isAvailable("XXX", null, null));
        assertFalse("USDOLLAR invalid code", Currency.isAvailable("USDOLLAR", null, null));

        // illegal argument combination
        try {
            Currency.isAvailable("USD", d2005, d1995);
            errln("Expected IllegalArgumentException, because lower range is after upper range");
        } catch (IllegalArgumentException e) {
            logln("IllegalArgumentException, because lower range is after upper range");
        }
    }
}
