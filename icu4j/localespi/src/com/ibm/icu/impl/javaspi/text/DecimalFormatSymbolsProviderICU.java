/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.text;

import java.text.DecimalFormatSymbols;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.impl.jdkadapter.DecimalFormatSymbolsICU;

public class DecimalFormatSymbolsProviderICU extends
        DecimalFormatSymbolsProvider {

    @Override
    public DecimalFormatSymbols getInstance(Locale locale) {
        com.ibm.icu.text.DecimalFormatSymbols icuDecfs = com.ibm.icu.text.DecimalFormatSymbols.getInstance(
                ICULocaleServiceProvider.canonicalize(locale));
        return DecimalFormatSymbolsICU.wrap(icuDecfs);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
