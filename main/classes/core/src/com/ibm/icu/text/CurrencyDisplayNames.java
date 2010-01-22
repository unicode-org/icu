/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.CurrencyData;
import com.ibm.icu.util.ULocale;
import java.util.Map;

/**
 * Returns currency names localized for a locale.
 * @draft ICU 4.4
 * @provisional This API might change or be removed in a future release.
 */
public abstract class CurrencyDisplayNames {
    /**
     * Return an instance of CurrencyDisplayNames that provides information
     * localized for display in the provided locale.
     * @param locale the locale into which to localize the names
     * @return a CurrencyDisplayNames
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static CurrencyDisplayNames getInstance(ULocale locale) {
        return CurrencyData.provider.getInstance(locale, true);
    }

    /**
     * Returns true if currency display name data is available.
     * @return true if currency display name data is available
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static boolean hasData() {
        return CurrencyData.provider.hasData();
    }

    /**
     * Returns the locale used to determine how to translate the currency names.
     * This is not necessarily the same locale passed to {@link #getInstance(ULocale)}.
     * If hasData is false, returns {@link com.ibm.icu.util.ULocale#ROOT}.
     * @return the display locale
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract ULocale getLocale();

    /**
     * Returns the symbol for the currency with the provided ISO code.
     * If hasData is false, returns the provided ISO code.
     * @param isoCode the three-letter ISO code.
     * @return the display name.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract String getSymbol(String isoCode);

    /**
     * Returns the 'long name' for the currency with the provided ISO code.
     * If hasData is false, returns the provided ISO code.
     * @param isoCode the three-letter ISO code
     * @return the display name
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract String getName(String isoCode);

    /**
     * Returns a 'plural name' for the currency with the provided ISO code corresponding to
     * the pluralKey. If hasData is false, returns the provided ISO code.
     * @param isoCode the three-letter ISO code
     * @param pluralKey the plural key, for example "one", "other"
     * @return the display name
     * @see com.ibm.icu.text.PluralRules
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract String getPluralName(String isoCode, String pluralKey);

    /**
     * Returns a mapping from localized symbols and currency codes to currency codes.
     * If hasData is false, returns an empty map.
     * The returned map is unmodifiable.
     * @return the map
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract Map<String, String> symbolMap();

    /**
     * Returns a mapping from localized names (standard and plural) to currency codes.
     * If hasData is false, returns an empty map.
     * The returned map is unmodifiable.
     * @return the map
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public abstract Map<String, String> nameMap();
}
