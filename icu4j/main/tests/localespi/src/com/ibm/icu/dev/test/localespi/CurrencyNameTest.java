// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class CurrencyNameTest extends TestFmwk {
    private static final Set<Currency> AVAILABLE_CURRENCIES;
    private static final Method GETDISPLAYNAME_METHOD;

    static {
        Method mGetDisplayName = null;
        Set<Currency> currencies = null;
        try {
            mGetDisplayName = Currency.class.getMethod("getDisplayName", new Class[] {Locale.class});
            Method mGetAvailableCurrencies = Currency.class.getMethod("getAvailableCurrencies", (Class[]) null);
            currencies = (Set<Currency>)mGetAvailableCurrencies.invoke(null, (Object[]) null);
        } catch (Exception e) {
            // fall through
        }

        if (currencies == null) {
            // Make a set of unique currencies
            currencies = new HashSet<Currency>();
            for (Locale l : Locale.getAvailableLocales()) {
                if (l.getCountry().length() == 0) {
                    continue;
                }
                try {
                    Currency currency = Currency.getInstance(l);
                    if (currency != null) {
                        currencies.add(currency);
                    }
                } catch (IllegalArgumentException iae) {
                    // ignore
                }
            }
        }
        GETDISPLAYNAME_METHOD = mGetDisplayName;
        AVAILABLE_CURRENCIES = Collections.unmodifiableSet(currencies);
    }

    @Test
    public void TestCurrencySymbols() {
        for (Currency currency : AVAILABLE_CURRENCIES) {
            String currencyCode = currency.getCurrencyCode();
            com.ibm.icu.util.Currency currencyIcu = com.ibm.icu.util.Currency.getInstance(currencyCode);
            for (Locale loc : Locale.getAvailableLocales()) {
                if (TestUtil.isExcluded(loc)) {
                    logln("Skipped " + loc);
                    continue;
                }

                String curSymbol = currency.getSymbol(loc);
                String curSymbolIcu = currencyIcu.getSymbol(loc);

                if (curSymbolIcu.equals(currencyCode)) {
                    // No data in ICU
                    if (!curSymbol.equals(currencyCode)) {
                        logln("INFO: JDK has currency symbol " + curSymbol + " for locale " +
                                loc + ", but ICU does not");
                    }
                    continue;
                }

                if (TestUtil.isICUExtendedLocale(loc)) {
                    if (!curSymbol.equals(curSymbolIcu)) {
                        errln("FAIL: Currency symbol for " + currencyCode + " by ICU is " + curSymbolIcu
                                + ", but got " + curSymbol + " in locale " + loc);
                    }
                } else {
                    if (!curSymbol.equals(curSymbolIcu)) {
                        logln("INFO: Currency symbol for " + currencyCode +  " by ICU is " + curSymbolIcu
                                + ", but " + curSymbol + " by JDK in locale " + loc);
                    }
                    // Try explicit ICU locale (xx_yy_ICU)
                    Locale locIcu = TestUtil.toICUExtendedLocale(loc);
                    curSymbol = currency.getSymbol(locIcu);
                    if (!curSymbol.equals(curSymbolIcu)) {
                        errln("FAIL: Currency symbol for " + currencyCode + " by ICU is " + curSymbolIcu
                                + ", but got " + curSymbol + " in locale " + locIcu);
                    }
                }
            }
        }
    }

    @Test
    public void TestCurrencyDisplayNames() {
        if (GETDISPLAYNAME_METHOD == null) {
            logln("INFO: Currency#getDisplayName(String,Locale) is not available.");
            return;
        }

        for (Currency currency : AVAILABLE_CURRENCIES) {
            String currencyCode = currency.getCurrencyCode();
            com.ibm.icu.util.Currency currencyIcu = com.ibm.icu.util.Currency.getInstance(currencyCode);
            for (Locale loc : Locale.getAvailableLocales()) {
                if (TestUtil.isExcluded(loc)) {
                    logln("Skipped " + loc);
                    continue;
                }

                String curName = null;
                try {
                    curName = (String)GETDISPLAYNAME_METHOD.invoke(currency, new Object[] {loc});
                } catch (Exception e) {
                    errln("FAIL: JDK Currency#getDisplayName(\"" + currency + "\", \"" + loc + "\") throws exception: " + e.getMessage());
                    continue;
                }

                String curNameIcu = currencyIcu.getDisplayName(loc);

                if (curNameIcu.equals(currencyCode)) {
                    // No data in ICU
                    if (!curName.equals(currencyCode)) {
                        logln("INFO: JDK has currency display name " + curName + " for locale " +
                                loc + ", but ICU does not");
                    }
                    continue;
                }

                if (TestUtil.isICUExtendedLocale(loc)) {
                    if (!curName.equals(curNameIcu)) {
                        errln("FAIL: Currency display name for " + currencyCode + " by ICU is " + curNameIcu
                                + ", but got " + curName + " in locale " + loc);
                    }
                } else {
                    if (!curName.equals(curNameIcu)) {
                        logln("INFO: Currency display name for " + currencyCode +  " by ICU is " + curNameIcu
                                + ", but " + curName + " by JDK in locale " + loc);
                    }
                    // Try explicit ICU locale (xx_yy_ICU)
                    Locale locIcu = TestUtil.toICUExtendedLocale(loc);
                    try {
                        curName = (String)GETDISPLAYNAME_METHOD.invoke(currency, new Object[] {locIcu});
                    } catch (Exception e) {
                        errln("FAIL: JDK Currency#getDisplayName(\"" + currency + "\", \"" + locIcu + "\") throws exception: " + e.getMessage());
                        continue;
                    }
                    if (!curName.equals(curNameIcu)) {
                        errln("FAIL: Currency display name for " + currencyCode + " by ICU is " + curNameIcu
                                + ", but got " + curName + " in locale " + locIcu);
                    }
                }
            }
        }
    }

}
