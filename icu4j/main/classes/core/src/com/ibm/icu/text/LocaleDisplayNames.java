/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.Locale;

import com.ibm.icu.impl.LocaleDisplayNamesImpl;
import com.ibm.icu.util.ULocale;

/**
 * Returns display names of ULocales and components of ULocales. For
 * more information on language, script, region, variant, key, and
 * values, see {@link com.ibm.icu.util.ULocale}.
 * @draft ICU 4.4
 */
public abstract class LocaleDisplayNames {
    /**
     * Enum used in {@link #getInstance}.
     * @draft ICU 4.4
     */
    public enum DialectHandling {
        /**
         * Use standard names when generating a locale name,
         * e.g. en_GB displays as 'English (United Kingdom)'.
         * @draft ICU 4.4
         */
        STANDARD,
        /**
         * Use dialect names, when generating a locale name,
         * e.g. en_GB displays as 'British English'.
         * @draft ICU 4.4
         */
        USE_DIALECT_NAMES
    }

    // factory methods
    /**
     * Convenience overload of {@link #getInstance(Ulocale, DialectHandling)} that specifies
     * STANDARD dialect handling.
     * @param locale the display locale
     * @return a LocaleDisplayNames instance
     * @draft ICU 4.4
     */
    public static LocaleDisplayNames getInstance(ULocale locale) {
        return getInstance(locale, DialectHandling.STANDARD);
    };

    /**
     * Returns an instance of LocaleDisplayNames that returns names formatted for the provided locale,
     * using the provided dialectHandling.
     * @param locale the display locale
     * @param dialectHandling how to select names for locales
     * @return a LocaleDisplayNames instance
     * @draft ICU 4.4
     */
    public static LocaleDisplayNames getInstance(ULocale locale, DialectHandling dialectHandling) {
        return LocaleDisplayNamesImpl.getInstance(locale, dialectHandling);
    }

    // getters for state
    /**
     * Returns the locale used to determine the display names. This is not necessarily the same
     * locale passed to {@link #getInstance}.
     * @return the display locale
     * @draft ICU 4.4
     */
    public abstract ULocale getLocale();

    /**
     * Returns the dialect handling used in the display names.
     * @return the dialect handling enum
     * @draft ICU 4.4
     */
    public abstract DialectHandling getDialectHandling();

    // names for entire locales
    /**
     * Returns the display name of the provided ulocale.
     * @return the display name of the provided locale
     * @draft ICU 4.4
     */
    public abstract String localeDisplayName(ULocale locale);

    /**
     * Returns the display name of the provided locale.
     * @return the display name of the provided locale
     * @draft ICU 4.4
     */
    public abstract String localeDisplayName(Locale locale);

    /**
     * Returns the display name of the provided locale id.
     * @return the display name of the provided locale
     * @draft ICU 4.4
     */
    public abstract String localeDisplayName(String localeId);

    // names for components of a locale id
    /**
     * Returns the display name of the provided language code.
     * @return the display name of the provided language code
     * @draft ICU 4.4
     */
    public abstract String languageDisplayName(String lang);

    /**
     * Returns the display name of the provided script code.
     * @return the display name of the provided script code
     * @draft ICU 4.4
     */
    public abstract String scriptDisplayName(String script);

    /**
     * Returns the display name of the provided script code.  See
     * {@link com.ibm.icu.lang.UScript} for recognized script codes.
     * @return the display name of the provided script code
     * @draft ICU 4.4
     */
    public abstract String scriptDisplayName(int scriptCode);

    /**
     * Returns the display name of the provided region code.
     * @return the display name of the provided region code
     * @draft ICU 4.4
     */
    public abstract String regionDisplayName(String region);

    /**
     * Returns the display name of the provided variant.
     * @return the display name of the provided variant
     * @draft ICU 4.4
     */
    public abstract String variantDisplayName(String variant);

    /**
     * Returns the display name of the provided locale key.
     * @return the display name of the provided locale key
     * @draft ICU 4.4
     */
    public abstract String keyDisplayName(String key);

    /**
     * Returns the display name of the provided value (used with the provided key).
     * @return the display name of the provided value
     * @draft ICU 4.4
     */
    public abstract String keyValueDisplayName(String key, String value);
}
