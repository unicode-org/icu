/*
**********************************************************************
* Copyright (c) 2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
* Created: May 22 2003
* Since: ICU 2.6
**********************************************************************
*/
package com.ibm.icu.dev.demo.number;
import com.ibm.icu.util.Currency;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.impl.Utility;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * Demonstration code to illustrate how to obtain ICU 2.6-like currency
 * behavior using pre-ICU 2.6 ICU4J.
 * @author Mark Davis
 */
public class CurrencyDemo {

    public static void main(String[] args) {
        testFormatHack(true);
    }

    static NumberFormat getCurrencyFormat(Currency currency,
                                          Locale displayLocale,
                                          boolean ICU26) {
        // code for ICU 2.6
        if (ICU26) {
            NumberFormat result = NumberFormat.getCurrencyInstance(displayLocale);
            result.setCurrency(currency);
            return result;
        }

        // ugly work-around for 2.4
        DecimalFormat result = (DecimalFormat)NumberFormat.getCurrencyInstance(displayLocale);
        HackCurrencyInfo hack = (HackCurrencyInfo)(hackData.get(currency.getCurrencyCode()));
        result.setMinimumFractionDigits(hack.decimals);
        result.setMaximumFractionDigits(hack.decimals);
        result.setRoundingIncrement(hack.rounding);
        DecimalFormatSymbols symbols = result.getDecimalFormatSymbols();
        symbols.setCurrencySymbol(hack.symbol);
        result.setDecimalFormatSymbols(symbols);
        return result;
    }
        
    static Map hackData = new HashMap();
    static class HackCurrencyInfo {
        int decimals;
        double rounding;
        String symbol;
        HackCurrencyInfo(int decimals, double rounding, String symbol) {
            this.decimals = decimals;
            this.rounding = rounding;
            this.symbol = symbol;
        }
    }
    static {
        hackData.put("USD", new HackCurrencyInfo(2, 0, "$"));
        hackData.put("GBP", new HackCurrencyInfo(2, 0, "\u00A3"));
        hackData.put("JPY", new HackCurrencyInfo(0, 0, "\u00A5"));
        hackData.put("EUR", new HackCurrencyInfo(2, 0, "\u20AC"));
    }

    /**
     * Walk through all locales and compare the output of the ICU26
     * currency format with the "hacked" currency format.
     * @param quiet if true, only display discrepancies.  Otherwise,
     * display all results.
     */
    static void testFormatHack(boolean quiet) {
        String[] testCurrencies = {"USD","GBP","JPY","EUR"};
        Locale[] testLocales = NumberFormat.getAvailableLocales();
        for (int i = 0; i < testLocales.length; ++i) {
            // since none of this should vary by country, we'll just do by language
            if (!testLocales[i].getCountry().equals("")) continue;
            boolean noOutput = true;
            if (!quiet) {
                System.out.println(testLocales[i].getDisplayName());
                noOutput = false;
            }
            for (int j = 0; j < testCurrencies.length; ++j) {
                NumberFormat nf26 = getCurrencyFormat(Currency.getInstance(testCurrencies[j]), testLocales[i], true);
                String str26 = nf26.format(1234.567);
                if (!quiet) {
                    System.out.print("\t" + Utility.escape(str26));
                }
                NumberFormat nf24 = getCurrencyFormat(Currency.getInstance(testCurrencies[j]), testLocales[i], false);
                String str24 = nf24.format(1234.567);
                if (!str24.equals(str26)) {
                    if (noOutput) {
                        System.out.println(testLocales[i].getDisplayName());
                        noOutput = false;
                    }
                    if (quiet) {
                        System.out.print("\t" + Utility.escape(str26));
                    }
                    System.out.print(" (" + Utility.escape(str24) + ")");
                }
            }
            if (!noOutput) {
                System.out.println();
            }
        }
    }
}
