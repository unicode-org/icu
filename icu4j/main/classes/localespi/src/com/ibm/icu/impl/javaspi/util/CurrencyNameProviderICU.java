// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.util;

import java.util.Locale;
import java.util.spi.CurrencyNameProvider;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.text.CurrencyDisplayNames;

public class CurrencyNameProviderICU extends CurrencyNameProvider {

    @Override
    public String getSymbol(String currencyCode, Locale locale) {
        CurrencyDisplayNames curDispNames = CurrencyDisplayNames.getInstance(ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale));
        String sym = curDispNames.getSymbol(currencyCode);
        if (sym == null || sym.equals(currencyCode)) {
            return null;
        }
        return sym;
    }

    @Override
    public String getDisplayName(String currencyCode, Locale locale) {
        CurrencyDisplayNames curDispNames = CurrencyDisplayNames.getInstance(ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale));
        String name = curDispNames.getName(currencyCode);
        if (name == null || name.equals(currencyCode)) {
            return null;
        }
        return name;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
