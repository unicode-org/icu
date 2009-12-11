/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.util.ULocale;
import java.util.Map;

/**
 * Returns information about currency display names in a locale.
 */
public abstract class CurrencyDisplayNames {
    /**
     * Return an instance of CurrencyDisplayNames that provides information 
     * localized for display in the provided locale.
     * @param locale the locale into which to localize the names
     * @return a CurrencyDisplayNames
     */
    public static CurrencyDisplayNames getInstance(ULocale locale) {
        return CurrencyData.provider.getInstance(locale, true);
    }
    
    /**
     * Returns true if currency display name data is available.
     * @return true if currency display name data is available.
     */
    public static boolean hasData() {
        return CurrencyData.provider.hasData();
    }
    
    /**
     * Returns the locale used to determine how to translate the currency names.
     * @return the display locale
     */
    public abstract ULocale getLocale();
    
    /**
     * Returns the symbol for the currency with the provided ISO code.
     * @param isoCode the three-letter ISO code.
     * @return the display name.
     */
    public abstract String getSymbol(String isoCode);
    
    /**
     * Returns the 'long name' for the currency with the provided ISO code.
     * @param isoCode the three-letter ISO code
     * @return the display name
     */
    public abstract String getName(String isoCode);
    
    /**
     * Returns a 'plural name' for the currency with the provided ISO code corresponding to
     * the pluralKey.
     * @param isoCode the three-letter ISO code
     * @param pluralKey the plural key, for example "one", "other"
     * @return the display name
     */
    public abstract String getPluralName(String isoCode, String pluralKey);
    
    /**
     * Returns a mapping from localized symbols and currency codes to currency codes.
     * The returned map is unmodifiable.
     */
    public abstract Map<String, String> symbolMap();
    
    /**
     * Returns a mapping from localized names (standard and plural) to currency codes.
     * The returned map is unmodifiable.
     */
    public abstract Map<String, String> nameMap();
}
