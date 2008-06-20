/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

public class CurrencyNameTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new CurrencyNameTest().run(args);
    }

    public void TestCurrencySymbols() {
        // Make a set of unique currencies
        HashSet<Currency> currencies = new HashSet<Currency>();
        for (Locale l : Locale.getAvailableLocales()) {
            if (l.getCountry().length() == 0) {
                continue;
            }
            Currency currency = Currency.getInstance(l);
            if (currency == null) {
                continue;
            }
            currencies.add(currency);
        }

        for (Currency currency : currencies) {
            String currencyCode = currency.getCurrencyCode();
            com.ibm.icu.util.Currency currencyIcu = com.ibm.icu.util.Currency.getInstance(currencyCode);
            if (currencyIcu == null) {
                logln("INFO: Currency code " + currencyCode + " is not supported by ICU");
                continue;
            }
            for (Locale loc : Locale.getAvailableLocales()) {
                if (TestUtil.isProblematicIBMLocale(loc)) {
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
                        if (!curSymbol.equals(curSymbolIcu)) {
                            errln("FAIL: Currency symbol for " + currencyCode + " by ICU is " + curSymbolIcu
                                    + ", but got " + curSymbol + " in locale " + loc);
                        }
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
}
