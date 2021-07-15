// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.text;

import java.text.DateFormatSymbols;
import java.text.spi.DateFormatSymbolsProvider;
import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.impl.jdkadapter.DateFormatSymbolsICU;

public class DateFormatSymbolsProviderICU extends DateFormatSymbolsProvider {

    @Override
    public DateFormatSymbols getInstance(Locale locale) {
        com.ibm.icu.text.DateFormatSymbols icuDfs = com.ibm.icu.text.DateFormatSymbols.getInstance(
                ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale));
        return DateFormatSymbolsICU.wrap(icuDfs);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
