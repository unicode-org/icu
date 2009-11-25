/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Static utility functions for probing resource tables, used by ULocale and
 * LocaleDisplayNames.
 */
public class ICUResourceTableAccess {
    /**
     * Utility to fetch locale display data from resource bundle tables.  Convenience
     * wrapper for {@link #getTableString(ICUResourceBundle, String, String, String)}.
     */
    public static String getTableString(String path, ULocale locale, String tableName,
            String itemName) {
        ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.
            getBundleInstance(path, locale.getBaseName());
        return getTableString(bundle, tableName, null, itemName);
    }

    /**
     * Utility to fetch locale display data from resource bundle tables.  Uses fallback
     * through the "Fallback" resource if available.
     */
    public static String getTableString(ICUResourceBundle bundle, String tableName,
            String subtableName, String item) {
        try {
            for (;;) {
                // special case currency
                if ("currency".equals(subtableName)) {
                    ICUResourceBundle table = bundle.getWithFallback("Currencies");
                    table = table.getWithFallback(item);
                    return table.getString(1);
                } else {
                    ICUResourceBundle table = lookup(bundle, tableName);
                    if (table == null) {
                        return item;
                    }
                    ICUResourceBundle stable = table;
                    if (subtableName != null) {
                        stable = lookup(table, subtableName);
                    }
                    if (stable != null) {
                        ICUResourceBundle sbundle = lookup(stable, item);
                        if (sbundle != null) {
                            return sbundle.getString(); // possible real exception
                        }
                    }

                    // if we get here, stable was null, or sbundle was null
                    if (subtableName == null) {
                        // may be a deprecated code
                        String currentName = null;
                        if (tableName.equals("Countries")) {
                            currentName = LocaleIDs.getCurrentCountryID(item);
                        } else if (tableName.equals("Languages")) {
                            currentName = LocaleIDs.getCurrentLanguageID(item);
                        }
                        ICUResourceBundle sbundle = lookup(table, currentName);
                        if (sbundle != null) {
                            return sbundle.getString(); // possible real exception
                        }
                    }

                    // still can't figure it out? try the fallback mechanism
                    ICUResourceBundle fbundle = lookup(table, "Fallback");
                    if (fbundle == null) {
                        return item;
                    }

                    String fallbackLocale = fbundle.getString(); // again, possible exception
                    if (fallbackLocale.length() == 0) {
                        fallbackLocale = "root";
                    }

                    if (fallbackLocale.equals(table.getULocale().getName())) {
                        return item;
                    }

                    bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(
                            bundle.getBaseName(), fallbackLocale);
                }
            }
        } catch (Exception e) {
            // If something is seriously wrong, we might call getString on a resource that is
            // not a string.  That will throw an exception, which we catch and ignore here.
        }

        return item;
    }

    // utility to make the call sites in the above code cleaner
    private static ICUResourceBundle lookup(ICUResourceBundle bundle, String resName) {
        return ICUResourceBundle.findResourceWithFallback(resName, bundle, null);
    }
}
