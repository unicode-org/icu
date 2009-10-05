/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.text;

import java.text.NumberFormat;
import java.text.spi.NumberFormatProvider;
import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.impl.jdkadapter.DecimalFormatICU;
import com.ibm.icu.impl.jdkadapter.NumberFormatICU;

public class NumberFormatProviderICU extends NumberFormatProvider {

    private final int NUMBER = 0;
    private final int INTEGER = 1;
    private final int CURRENCY = 2;
    private final int PERCENT = 3;

    @Override
    public NumberFormat getCurrencyInstance(Locale locale) {
        return getInstance(CURRENCY, locale);
    }

    @Override
    public NumberFormat getIntegerInstance(Locale locale) {
        return getInstance(INTEGER, locale);
    }

    @Override
    public NumberFormat getNumberInstance(Locale locale) {
        return getInstance(NUMBER, locale);
    }

    @Override
    public NumberFormat getPercentInstance(Locale locale) {
        return getInstance(PERCENT, locale);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

    private NumberFormat getInstance(int type, Locale locale) {
        com.ibm.icu.text.NumberFormat icuNfmt;
        Locale actual = ICULocaleServiceProvider.canonicalize(locale);
        switch (type) {
        case NUMBER:
            icuNfmt = com.ibm.icu.text.NumberFormat.getNumberInstance(actual);
            break;
        case INTEGER:
            icuNfmt = com.ibm.icu.text.NumberFormat.getIntegerInstance(actual);
            break;
        case CURRENCY:
            icuNfmt = com.ibm.icu.text.NumberFormat.getCurrencyInstance(actual);
            break;
        case PERCENT:
            icuNfmt = com.ibm.icu.text.NumberFormat.getPercentInstance(actual);
            break;
        default:
            return null;
        }

        if (!(icuNfmt instanceof com.ibm.icu.text.DecimalFormat)) {
            // icuNfmt must be always DecimalFormat
            return null;
        }

        NumberFormat nf = null;
        if (ICULocaleServiceProvider.useDecimalFormat()) {
            nf = DecimalFormatICU.wrap((com.ibm.icu.text.DecimalFormat)icuNfmt);
        } else {
            nf = NumberFormatICU.wrap(icuNfmt);
        }

        com.ibm.icu.text.DecimalFormatSymbols decfs = ICULocaleServiceProvider.getDecimalFormatSymbolsForLocale(actual);
        if (decfs != null) {
            ((com.ibm.icu.text.DecimalFormat)icuNfmt).setDecimalFormatSymbols(decfs);
        }

        return nf;
    }
}
