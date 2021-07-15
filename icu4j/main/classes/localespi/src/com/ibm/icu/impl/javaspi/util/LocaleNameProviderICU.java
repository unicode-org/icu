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
import java.util.spi.LocaleNameProvider;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.impl.locale.AsciiUtil;
import com.ibm.icu.text.LocaleDisplayNames;

public class LocaleNameProviderICU extends LocaleNameProvider {

    @Override
    public String getDisplayCountry(String countryCode, Locale locale) {
        countryCode = AsciiUtil.toUpperString(countryCode);
        String disp = LocaleDisplayNames.getInstance(ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale))
                .regionDisplayName(countryCode);
        if (disp == null || disp.length() == 0 || disp.equals(countryCode)) {
            return null;
        }
        return disp;
    }

    @Override
    public String getDisplayLanguage(String languageCode, Locale locale) {
        languageCode = AsciiUtil.toLowerString(languageCode);
        String disp = LocaleDisplayNames.getInstance(ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale))
                .languageDisplayName(languageCode);
        if (disp == null || disp.length() == 0 || disp.equals(languageCode)) {
            return null;
        }
        return disp;
    }

    @Override
    public String getDisplayScript(String scriptCode, Locale locale) {
        scriptCode = AsciiUtil.toTitleString(scriptCode);
        String disp = LocaleDisplayNames.getInstance(ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale))
                .scriptDisplayName(scriptCode);
        if (disp == null || disp.length() == 0 || disp.equals(scriptCode)) {
            return null;
        }
        return disp;
    }

    @Override
    public String getDisplayVariant(String variant, Locale locale) {
        variant = AsciiUtil.toUpperString(variant);
        String disp = LocaleDisplayNames.getInstance(ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale))
                .variantDisplayName(variant);
        if (disp == null || disp.length() == 0 || disp.equals(variant)) {
            return null;
        }
        return disp;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }
}
