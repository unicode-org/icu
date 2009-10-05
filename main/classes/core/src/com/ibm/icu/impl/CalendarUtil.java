/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.MissingResourceException;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * Calendar utilities.
 * 
 * Date/time format service classes in com.ibm.icu.text packages
 * sometimes need to access calendar internal APIs.  But calendar
 * classes are in com.ibm.icu.util package, so the package local
 * cannot be used.  This class is added in com.ibm.icu.impl
 * package for sharing some calendar internal code for calendar
 * and date format.
 */
public class CalendarUtil {

    private static ICUCache<String, String> CALTYPE_CACHE = new SimpleCache<String, String>();

    private static final String CALKEY = "calendar";
    private static final String DEFCAL = "gregorian";

    /**
     * Returns a calendar type for the given locale.
     * When the given locale has calendar keyword, the
     * value of calendar keyword is returned.  Otherwise,
     * the default calendar type for the locale is returned.
     * @param loc The locale
     * @return Calendar type string, such as "gregorian"
     */
    public static String getCalendarType(ULocale loc) {
        String calType = null;

        calType = loc.getKeywordValue(CALKEY);
        if (calType != null) {
            return calType;
        }

        String baseLoc = loc.getBaseName();

        // Check the cache
        calType = CALTYPE_CACHE.get(baseLoc);
        if (calType != null) {
            return calType;
        }

        // Canonicalize, so grandfathered variant will be transformed to keywords
        ULocale canonical = ULocale.createCanonical(loc.toString());
        calType = canonical.getKeywordValue("calendar");

        if (calType == null) {
            // When calendar keyword is not available, use the locale's
            // region to get the default calendar type
            String region = canonical.getCountry();
            if (region.length() == 0) {
                ULocale fullLoc = ULocale.addLikelySubtags(canonical);
                region = fullLoc.getCountry();
            }

            // Read supplementalData to get the default calendar type for
            // the locale's region
            try {
                UResourceBundle rb = UResourceBundle.getBundleInstance(
                                        ICUResourceBundle.ICU_BASE_NAME,
                                        "supplementalData",
                                        ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle calPref = rb.get("calendarPreferenceData");
                UResourceBundle order = null;
                try {
                    order = calPref.get(region);
                } catch (MissingResourceException mre) {
                    // use "001" as fallback
                    order = calPref.get("001");
                }
                // the first calendar type is the default for the region
                calType = order.getString(0);
            } catch (MissingResourceException mre) {
                // fall through
            }

            if (calType == null) {
                // Use "gregorian" as the last resort fallback.
                calType = DEFCAL;
            }
        }

        // Cache the resolved value for the next time
        CALTYPE_CACHE.put(baseLoc, calType);

        return calType;
    }
}
