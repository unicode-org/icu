/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.Locale;

import com.ibm.icu.impl.LocaleDisplayNamesImpl;
import com.ibm.icu.util.ULocale;

/**
 * Returns display names of ULocales and components of ULocales.
 */
public abstract class LocaleDisplayNames {
    public enum DialectHandling {
        STANDARD, USE_DIALECT_NAMES
    }
    // factory methods
    public static LocaleDisplayNames getInstance(ULocale locale) {
        return getInstance(locale, DialectHandling.STANDARD);
    };

    public static LocaleDisplayNames getInstance(ULocale locale, DialectHandling dialectHandling) {
        return LocaleDisplayNamesImpl.getInstance(locale, dialectHandling);
    }

    // getters for state
    public abstract ULocale getLocale();
    public abstract DialectHandling getDialectHandling();

    // names for entire locales
    public abstract String localeDisplayName(ULocale locale);
    public abstract String localeDisplayName(Locale locale);
    public abstract String localeDisplayName(String localeId);

    // names for components of a locale id
    public abstract String languageDisplayName(String lang);
    public abstract String scriptDisplayName(String script);
    public abstract String scriptDisplayName(int scriptCode);
    public abstract String regionDisplayName(String region);
    public abstract String variantDisplayName(String variant);
    public abstract String keyDisplayName(String key);
    public abstract String keyValueDisplayName(String key, String value);
}
