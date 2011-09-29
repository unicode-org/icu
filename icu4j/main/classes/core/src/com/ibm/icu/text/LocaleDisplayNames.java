/*
 *******************************************************************************
 * Copyright (C) 2009-2011, International Business Machines Corporation and    *
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
 * @stable ICU 4.4
 */
public abstract class LocaleDisplayNames {
    /**
     * Enum used in {@link #getInstance(ULocale, DialectHandling)}.
     * @stable ICU 4.4
     */
    public enum DialectHandling {
        /**
         * Use standard names when generating a locale name,
         * e.g. en_GB displays as 'English (United Kingdom)'.
         * @stable ICU 4.4
         */
        STANDARD_NAMES,
        /**
         * Use dialect names when generating a locale name,
         * e.g. en_GB displays as 'British English'.
         * @stable ICU 4.4
         */
        DIALECT_NAMES
    }

    // factory methods
    /**
     * Convenience overload of {@link #getInstance(ULocale, DialectHandling)} that specifies
     * STANDARD dialect handling.
     * @param locale the display locale
     * @return a LocaleDisplayNames instance
     * @stable ICU 4.4
     */
    public static LocaleDisplayNames getInstance(ULocale locale) {
        return getInstance(locale, DialectHandling.STANDARD_NAMES);
    };

    /**
     * Returns an instance of LocaleDisplayNames that returns names formatted for the provided locale,
     * using the provided dialectHandling.
     * @param locale the display locale
     * @param dialectHandling how to select names for locales
     * @return a LocaleDisplayNames instance
     * @stable ICU 4.4
     */
    public static LocaleDisplayNames getInstance(ULocale locale, DialectHandling dialectHandling) {
        return LocaleDisplayNamesImpl.getInstance(locale, dialectHandling);
    }

    // getters for state
    /**
     * Returns the locale used to determine the display names. This is not necessarily the same
     * locale passed to {@link #getInstance}.
     * @return the display locale
     * @stable ICU 4.4
     */
    public abstract ULocale getLocale();

    /**
     * Returns the dialect handling used in the display names.
     * @return the dialect handling enum
     * @stable ICU 4.4
     */
    public abstract DialectHandling getDialectHandling();

    // names for entire locales
    /**
     * Returns the display name of the provided ulocale.
     * @param locale the locale whose display name to return
     * @return the display name of the provided locale
     * @stable ICU 4.4
     */
    public abstract String localeDisplayName(ULocale locale);

    /**
     * Returns the display name of the provided locale.
     * @param locale the locale whose display name to return
     * @return the display name of the provided locale
     * @stable ICU 4.4
     */
    public abstract String localeDisplayName(Locale locale);

    /**
     * Returns the display name of the provided locale id.
     * @param localeId the id of the locale whose display name to return
     * @return the display name of the provided locale
     * @stable ICU 4.4
     */
    public abstract String localeDisplayName(String localeId);

    // names for components of a locale id
    /**
     * Returns the display name of the provided language code.
     * @param lang the language code
     * @return the display name of the provided language code
     * @stable ICU 4.4
     */
    public abstract String languageDisplayName(String lang);

    /**
     * Returns the display name of the provided script code.
     * @param script the script code
     * @return the display name of the provided script code
     * @stable ICU 4.4
     */
    public abstract String scriptDisplayName(String script);
 
    /**
     * Returns the display name of the provided script code
     * when used in the context of a full locale name.
     * @param script the script code
     * @return the display name of the provided script code
     * @internal ICU 49
     * @deprecated This API is ICU internal only.
     */
    public String scriptDisplayNameInContext(String script) {
        return scriptDisplayName(script);
    }

    /**
     * Returns the display name of the provided script code.  See
     * {@link com.ibm.icu.lang.UScript} for recognized script codes.
     * @param scriptCode the script code number
     * @return the display name of the provided script code
     * @stable ICU 4.4
     */
    public abstract String scriptDisplayName(int scriptCode);

    /**
     * Returns the display name of the provided region code.
     * @param region the region code
     * @return the display name of the provided region code
     * @stable ICU 4.4
     */
    public abstract String regionDisplayName(String region);

    /**
     * Returns the display name of the provided variant.
     * @param variant the variant string
     * @return the display name of the provided variant
     * @stable ICU 4.4
     */
    public abstract String variantDisplayName(String variant);

    /**
     * Returns the display name of the provided locale key.
     * @param key the locale key name
     * @return the display name of the provided locale key
     * @stable ICU 4.4
     */
    public abstract String keyDisplayName(String key);

    /**
     * Returns the display name of the provided value (used with the provided key).
     * @param key the locale key name
     * @param value the locale key's value
     * @return the display name of the provided value
     * @stable ICU 4.4
     */
    public abstract String keyValueDisplayName(String key, String value);

    /**
     * Sole constructor.  (For invocation by subclass constructors,
     * typically implicit.)
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected LocaleDisplayNames() {
    }
}
