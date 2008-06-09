/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.util;

import java.util.Locale;
import java.util.spi.LocaleNameProvider;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.util.ULocale;

public class LocaleNameProviderICU extends LocaleNameProvider {

    @Override
    public String getDisplayCountry(String countryCode, Locale locale) {
        String id = "und_" + countryCode;
        String disp = ULocale.getDisplayCountry(id, ULocale.forLocale(ICULocaleServiceProvider.canonicalize(locale)));
        if (disp.length() == 0 || disp.equals(countryCode)) {
            return null;
        }
        return disp;
    }

    @Override
    public String getDisplayLanguage(String languageCode, Locale locale) {
        String disp = ULocale.getDisplayLanguage(languageCode, ULocale.forLocale(ICULocaleServiceProvider.canonicalize(locale)));
        if (disp.length() == 0 || disp.equals(languageCode)) {
            return null;
        }
        return disp;
    }

    @Override
    public String getDisplayVariant(String variant, Locale locale) {
        // ICU does not support JDK Locale variant names
        return null;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
