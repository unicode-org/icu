/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: January 14 2004
* Since: ICU 2.8
**********************************************************************
*/
package com.ibm.icu.dev.test.util;

import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import com.ibm.icu.dev.test.TestFmwk;
import java.util.Locale;

public class ULocaleTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new ULocaleTest().run(args);
    }
    
    public void TestCurrency() {
        
        String DATA[] = {
            // The tests are data driven.  Each line represents
            // a test event.  Order is important.

            // Possible tests (each test is specified by 4 strings;
            // sometimes the later strings will be null):

            // 1. To test the result for a given locale:
            // requested locale, exp. currency, exp. valid loc., exp. actual loc.

            // 2. To register a currency against a locale:
            // "r", locale to register, currency to register, (ignored)

            // 3. To unregister the last currency registered:
            // "u", (ignored), (ignored), (ignored)

            "en_US", "USD", "en_US", "",

            "en_US_CALIFORNIA", "USD", "en_US", "",

            "r", "en_US_CALIFORNIA", "USD", null,

            "en_US_CALIFORNIA", "USD", "en_US_CALIFORNIA", "en_US_CALIFORNIA",

            "u", null, null, null,

            "en_US_CALIFORNIA", "USD", "en_US", "",
        };

        Object regKeys[] = new Object[20]; // adjust len as needed
        int regKeyCount = 0;

        try {
            for (int i=0; i<DATA.length; i+=4) {
                if (DATA[i].equals("r")) {
                    String locname = DATA[i+1];
                    String curname = DATA[i+2];
                    Currency cur = Currency.getInstance(curname);
                    Locale loc = getLocale(locname);
                    Object obj = Currency.registerInstance(cur, loc);
                    regKeys[regKeyCount++] = obj;
                    logln("(registering " + locname + " => " + curname + ")");
                } else if (DATA[i].equals("u")) {
                    Currency.unregister(regKeys[--regKeyCount]);
                    regKeys[regKeyCount] = null;
                    logln("(unregistering)");
                } else {
                    Locale loc = getLocale(DATA[i]);
                    String curname = DATA[i+1];
                    Locale expValid = getLocale(DATA[i+2]);
                    Locale expActual = getLocale(DATA[i+3]);
                    Currency cur = Currency.getInstance(loc);

                    boolean ok = true;

                    if (!cur.getCurrencyCode().equals(curname)) {
                        errln("FAIL: Currency.getInstance(" + DATA[i] +
                              ").getCurrencyCode() => " + cur.getCurrencyCode() +
                              ", exp. " +
                              curname);
                        ok = false;
                    }
                    
                    ULocale valid = cur.getLocale(ULocale.VALID_LOCALE);
                    if (doValidTest()) {
                        if (!valid.toLocale().equals(expValid)) {
                            errln("FAIL: Currency.getInstance(" + DATA[i] +
                                  ").getLocale(VALID) => " + valid + ", exp. " +
                                  expValid);
                            ok = false;
                        }
                    }

                    ULocale actual = cur.getLocale(ULocale.ACTUAL_LOCALE);
                    if (!actual.toLocale().equals(expActual)) {
                        errln("FAIL: Currency.getInstance(" + DATA[i] +
                              ").getLocale(ACTUAL) => " + actual + ", exp. " +
                              expActual);
                        ok = false;
                    }

                    if (ok) {
                        logln("Ok: Currency.getInstance(" + DATA[i] +
                              ") => " + cur.getCurrencyCode() + ", valid=" +
                              valid + ", actual=" + actual);
                    }
                }
            }
        } finally {
            for (int i=0; i<regKeys.length; ++i) {
                if (regKeys[i] != null) {
                    Currency.unregister(regKeys[i]);
                }
            }
        }
    }

    /**
     * Factory that constructs a locale from an ID (this should be in
     * Locale).
     */
    static Locale getLocale(String ID) {
        String language=ID, country="", variant="";
        int i = ID.indexOf('_');
        if (i>=0) {
            language = ID.substring(0, i);
            int j = ID.indexOf('_', i+1);
            if (j<0) {
                country = ID.substring(i+1);
            } else {
                country = ID.substring(i+1, j);
                variant = ID.substring(j+1);
            }
        }
        return new Locale(language, country, variant);
    }

    // Time bomb code to temporarily modify the behavior of this test
    // to account for the fact that the valid locale is unavailable in
    // ICU 2.8.

    static boolean IS_AFTER_2_8 =
        VersionInfo.ICU_VERSION.compareTo(VersionInfo.getInstance(2,8,0,0)) > 0;

    static boolean doValidTest() { return IS_AFTER_2_8; }
}
