/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: December 18 2002
* Since: ICU 2.4
**********************************************************************
*/
package com.ibm.icu.dev.test.util;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.*;
import java.util.Locale;

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
        int hash = usd.hashCode();
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

        Locale[] avail = Currency.getAvailableLocales();
    }
}
