/*
 **********************************************************************
 * Copyright (c) 2002-2008, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Alan Liu
 * Created: December 18 2002
 * Since: ICU 2.4
 **********************************************************************
 */

package com.ibm.icu.dev.test.util;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Date;

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

	public void TestAvailableCurrencyCodes()
	{
		// local Variables
		String[] currency;

		// Cycle through historical currencies 
		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AM"), new Date(-630720000000L)); // pre 1961
		if (currency != null)
		{
			errln("FAIL: didn't return null for eo_AM");
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AM"), new Date(0L)); // 1970
		if (currency.length != 1)
		{
			errln("FAIL: didn't return 1 for eo_AM returned: " + currency.length);
		}
		if (!"SUR".equals(currency[0]))
		{
			errln("didn't return SUR for eo_AM returned: " + currency[0]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AM"), new Date(693792000000L)); // 1992
		if (currency.length != 1)
		{
			errln("FAIL: didn't return 1 for eo_AM returned: " + currency.length);
		}
		if (!"RUR".equals(currency[0]))
		{
			errln("didn't return RUR for eo_AM returned: " + currency[0]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AM"), new Date(977616000000L)); // post 1993
		if (currency.length != 1)
		{
			errln("FAIL: didn't return 1 for eo_AM returned: " + currency.length);
		}
		if (!"AMD".equals(currency[0]))
		{
			errln("didn't return AMD for eo_AM returned: " + currency[0]);
		}

		// Locale AD has multiple currencies at once
		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AD"), new Date(977616000000L)); // 2001
		if (currency.length != 4)
		{
			errln("FAIL: didn't return 4 for eo_AD returned: " + currency.length);
		}
		if (!"EUR".equals(currency[0]))
		{
			errln("didn't return EUR for eo_AD returned: " + currency[0]);
		}
		if (!"ESP".equals(currency[1]))
		{
			errln("didn't return ESP for eo_AD returned: " + currency[1]);
		}
		if (!"FRF".equals(currency[2]))
		{
			errln("didn't return FRF for eo_AD returned: " + currency[2]);
		}
		if (!"ADP".equals(currency[3]))
		{
			errln("didn't return ADP for eo_AD returned: " + currency[3]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AD"), new Date(0L)); // 1970
		if (currency.length != 3)
		{
			errln("FAIL: didn't return 3 for eo_AD returned: " + currency.length);
		}
		if (!"ESP".equals(currency[0]))
		{
			errln("didn't return ESP for eo_AD returned: " + currency[0]);
		}
		if (!"FRF".equals(currency[1]))
		{
			errln("didn't return FRF for eo_AD returned: " + currency[1]);
		}
		if (!"ADP".equals(currency[2]))
		{
			errln("didn't return ADP for eo_AD returned: " + currency[2]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AD"), new Date(-630720000000L)); // 1950
		if (currency.length != 2)
		{
			errln("FAIL: didn't return 2 for eo_AD returned: " + currency.length);
		}
		if (!"ESP".equals(currency[0]))
		{
			errln("didn't return ESP for eo_AD returned: " + currency[0]);
		}
		if (!"ADP".equals(currency[1]))
		{
			errln("didn't return ADP for eo_AD returned: " + currency[1]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AD"), new Date(-2207520000000L)); // 1900
		if (currency.length != 1)
		{
			errln("FAIL: didn't return 1 for eo_AD returned: " + currency.length);
		}
		if (!"ESP".equals(currency[0]))
		{
			errln("didn't return ESP for eo_AD returned: " + currency[0]);
		}

		// Locale UA has gap between years 1994 - 1996
		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_UA"), new Date(788400000000L));
		if (currency != null)
		{
			errln("FAIL: didn't return null for eo_UA");
		}

		// Test for bogus locale
		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_QQ"), new Date(0L));
		if (currency != null)
		{
			errln("FAIL: didn't return null for eo_QQ");
		}

		// Cycle through historical currencies
		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AO"), new Date(977616000000L)); // 2001
		if (currency.length != 1)
		{
			errln("FAIL: didn't return 1 for eo_AO returned: " + currency.length);
		}
		if (!"AOA".equals(currency[0]))
		{
			errln("didn't return AOA for eo_AO returned: " + currency[0]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AO"), new Date(819936000000L)); // 1996
		if (currency.length != 2)
		{
			errln("FAIL: didn't return 2 for eo_AO returned: " + currency.length);
		}
		if (!"AOR".equals(currency[0]))
		{
			errln("didn't return AOR for eo_AO returned: " + currency[0]);
		}
		if (!"AON".equals(currency[1]))
		{
			errln("didn't return AON for eo_AO returned: " + currency[1]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AO"), new Date(662256000000L)); // 
		if (currency.length != 2)
		{
			errln("FAIL: didn't return 2 for eo_AO returned: " + currency.length);
		}
		if (!"AON".equals(currency[0]))
		{
			errln("didn't return AON for eo_AO returned: " + currency[0]);
		}
		if (!"AOK".equals(currency[1]))
		{
			errln("didn't return AOK for eo_AO returned: " + currency[1]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AO"), new Date(315360000000L)); // 1980
		if (currency.length != 1)
		{
			errln("FAIL: didn't return 1 for eo_AO returned: " + currency.length);
		}
		if (!"AOK".equals(currency[0]))
		{
			errln("didn't return AOK for eo_AO returned: " + currency[0]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_AO"), new Date(0L)); // 1970
		if (currency != null)
		{
			errln("FAIL: didn't return null for eo_AO");
		}

		// Test with currency keyword override
		currency = Currency.getAvailableCurrencyCodes(new ULocale("eo_DE@currency=DEM"), new Date(977616000000L)); // 2001
		if (currency.length != 2)
		{
			errln("FAIL: didn't return 2 for eo_DE@currency=DEM returned: " + currency.length);
		}
		if (!"EUR".equals(currency[0]))
		{
			errln("didn't return EUR for eo_DE@currency=DEM returned: " + currency[0]);
		}
		if (!"DEM".equals(currency[1]))
		{
			errln("didn't return DEM for eo_DE@currency=DEM returned: " + currency[1]);
		}

		// Test Euro Support
		currency = Currency.getAvailableCurrencyCodes(new ULocale("en_US"), new Date(System.currentTimeMillis()));
		if (!"USD".equals(currency[0]))
		{
			errln("didn't return USD for en_US returned: " + currency[0]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("en_US_PREEURO"), new Date(System.currentTimeMillis()));
		if (!"USD".equals(currency[0]))
		{
			errln("didn't return USD for en_US_PREEURO returned: " + currency[0]);
		}

		currency = Currency.getAvailableCurrencyCodes(new ULocale("en_US_Q"), new Date(System.currentTimeMillis()));
		if (!"USD".equals(currency[0]))
		{
			errln("didn't return USD for en_US_Q returned: " + currency[0]);
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
        ArrayList got = new ArrayList();
        ArrayList expected = new ArrayList();
        
        String expectedResult = "";
        String gotResult = "";
        
        String inputLocale[] = {
            "zh__PINYIN",
            "zh_TW_STROKE",
            "zh_MO",
            "zh",
            "zh_Hant_MO",
            "uk_UA",
            "sr_Latn_ME",
            "sr_Latn",
            "sr",
            "de",
            "de__PHONEBOOK",
            "no_NO",
            "pa_Guru_IN",
            "es",
            "es__TRADITIONAL",
            "ko_KR",
            "kok",
            "ms_MY",
            "ab_AA_jdhdj@collation=xyz",
            "de__PHONEBOOK@calendar=japanese",
        };
        
        String currency[][]={
                {"CNY"},
                {"TWD"},
                {"MOP"},
                {"CNY"},
                {"MOP"},
                {"UAH"},
                {"EUR"},
                {"RSD"},
                {"RSD"},
                {"EUR"},
                {"EUR"},
                {"NOK"},
                {"INR"},
                {"EUR"},
                {"EUR"},
                {"KRW"},
                {"INR"},
                {"MYR"},
                {},
                {"EUR"}
        };
        
        logln("Starting preferred currency keyword value test");
        
        for(int i=0;i<inputLocale.length;i++){
            ULocale loc = new ULocale(inputLocale[i]);
            for(int j=0;j<currency[i].length;j++){
                expected.add(currency[i][j]);
                expectedResult += currency[i][j]+" ";
               
            }
            String[] s = Currency.getKeywordValues("currency", loc, true);
            String s1;
            for(int j=0;j<s.length;j++){
                got.add((s1=s[j]));
                gotResult +=s1+" ";
            }
            Collections.sort(got);
            Collections.sort(expected);
            if(got.equals(expected)){
                logln("PASS: Locale :"+inputLocale[i]);
                logln("EXPECTED :"+expectedResult);
                logln("GOT      :"+gotResult);
            }else{
                errln("FAIL: Locale :"+inputLocale[i]+" EXPECTED :"+expectedResult+" GOT :"+gotResult);
            }
            gotResult=expectedResult="";
            got.clear();
            expected.clear();
            
        } 
        
        logln("Starting all available currency keyword value test");
        
        for(int i=0;i<inputLocale.length;i++){
            ULocale loc = new ULocale(inputLocale[i]);
            
            String[] s = Currency.getKeywordValues("currency", loc, false);
            if(s.length==160){
                logln("PASS: Locale :"+inputLocale[i]);
            }else{
                errln("FAIL: Locale :"+inputLocale[i]);
            }
            
        } 
    }
}
