/*
**********************************************************************
* Copyright (c) 2003-2005, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: September 4 2003
* Since: ICU 2.8
**********************************************************************
*/
package com.ibm.icu.impl;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * This class, not to be instantiated, implements the meta-data
 * missing from the underlying core JDK implementation of time zones.
 * There are two missing features: Obtaining a list of available zones
 * for a given country (as defined by the Olson database), and
 * obtaining a list of equivalent zones for a given zone (as defined
 * by Olson links).
 *
 * This class uses a data class, ZoneMetaData, which is created by the
 * tool tz2icu.
 *
 * @author Alan Liu
 * @since ICU 2.8
 */
public final class ZoneMeta {

    /**
     * Returns a String array containing all system TimeZone IDs
     * associated with the given country.  These IDs may be passed to
     * <code>TimeZone.getTimeZone()</code> to construct the
     * corresponding TimeZone object.
     * @param country a two-letter ISO 3166 country code, or <code>null</code>
     * to return zones not associated with any country
     * @return an array of IDs for system TimeZones in the given
     * country.  If there are none, return a zero-length array.
     */
    public static synchronized String[] getAvailableIDs(String country) {
        if (country == null) {
            country = "";
        }
        if (COUNTRY_MAP == null) {
            Set valid = getValidIDs();
            Set unused = new TreeSet(valid);

            ArrayList list = new ArrayList(); // reuse this below

            COUNTRY_MAP = new TreeMap();
            for (int i=0; i<ZoneMetaData.COUNTRY.length; ++i) {
                String[] z = ZoneMetaData.COUNTRY[i];

                // Add all valid IDs to list
                list.clear();
                for (int j=1; j<z.length; ++j) {
                    if (valid.contains(z[j])) {
                        list.add(z[j]);
                        unused.remove(z[j]);
                    }
                }
                
                COUNTRY_MAP.put(z[0], list.toArray(EMPTY));
            }

            // If there are zones in the underlying JDK that are NOT
            // in our metadata, then assign them to the non-country.
            // (Better than nothing.)
            if (unused.size() > 0) {
                list.clear();
                list.addAll(Arrays.asList((String[]) COUNTRY_MAP.get("")));
                list.addAll(unused);
                Collections.sort(list);                
                COUNTRY_MAP.put("", list.toArray(EMPTY));
            }
        }
        String[] result = (String[]) COUNTRY_MAP.get(country);
        if (result == null) {
            result = EMPTY; // per API spec
        }
        return result;
    }

    /**
     * Returns the number of IDs in the equivalency group that
     * includes the given ID.  An equivalency group contains zones
     * that behave identically to the given zone.
     *
     * <p>If there are no equivalent zones, then this method returns
     * 0.  This means either the given ID is not a valid zone, or it
     * is and there are no other equivalent zones.
     * @param id a system time zone ID
     * @return the number of zones in the equivalency group containing
     * 'id', or zero if there are no equivalent zones.
     * @see #getEquivalentID
     */
    public static synchronized int countEquivalentIDs(String id) {
        if (EQUIV_MAP == null) {
            createEquivMap();
        }
        String[] result = (String[]) EQUIV_MAP.get(id);
        return (result == null) ? 0 : result.length;
    }

    /**
     * Returns an ID in the equivalency group that includes the given
     * ID.  An equivalency group contains zones that behave
     * identically to the given zone.
     *
     * <p>The given index must be in the range 0..n-1, where n is the
     * value returned by <code>countEquivalentIDs(id)</code>.  For
     * some value of 'index', the returned value will be equal to the
     * given id.  If the given id is not a valid system time zone, or
     * if 'index' is out of range, then returns an empty string.
     * @param id a system time zone ID
     * @param index a value from 0 to n-1, where n is the value
     * returned by <code>countEquivalentIDs(id)</code>
     * @return the ID of the index-th zone in the equivalency group
     * containing 'id', or an empty string if 'id' is not a valid
     * system ID or 'index' is out of range
     * @see #countEquivalentIDs
     */
    public static synchronized String getEquivalentID(String id, int index) {
        if (EQUIV_MAP == null) {
            createEquivMap();
        }
        String[] a = (String[]) EQUIV_MAP.get(id);
        return (a != null && index >= 0 && index < a.length) ?
            a[index] : "";
    }

    /**
     * Create the equivalency map.
     */
    private static void createEquivMap() {
        EQUIV_MAP = new TreeMap();

        // try leaving all ids as valid
//         Set valid = getValidIDs();

        ArrayList list = new ArrayList(); // reuse this below

        for (int i=0; i<ZoneMetaData.EQUIV.length; ++i) {
            String[] z = ZoneMetaData.EQUIV[i];
            list.clear();
            for (int j=0; j<z.length; ++j) {
//                  if (valid.contains(z[j])) {
                    list.add(z[j]);
//                  }
            }
            if (list.size() > 1) {
                String[] a = (String[]) list.toArray(EMPTY);
                for (int j=0; j<a.length; ++j) {
                    EQUIV_MAP.put(a[j], a);
                }
            }
        }
    }

    private static String[] getCanonicalInfo(String id) {
        if (canonicalMap == null) {
            Map m = new HashMap();
            for (int i = 0; i < ZoneInfoExt.CLDR_INFO.length; ++i) {
                String[] clist = ZoneInfoExt.CLDR_INFO[i];
                String c = clist[0];
                m.put(c, clist);
                for (int j = 3; j < clist.length; ++j) {
                    m.put(clist[j], clist);
                }
            }
            synchronized (ZoneMeta.class) {
                canonicalMap = m;
            }
        }

        return (String[])canonicalMap.get(id);
    }
    private static Map canonicalMap = null;

    /**
     * Return the canonical id for this tzid, which might be the id itself.
     * If there is no canonical id for it, return the passed-in id.
     */
    public static String getCanonicalID(String tzid) {
        String[] info = getCanonicalInfo(tzid);
        if (info != null) {
            return info[0];
        }
        return tzid;
    }

    /**
     * Return the canonical country code for this tzid.  If we have none, or if the time zone
     * is not associated with a country, return null.
     */
    public static String getCanonicalCountry(String tzid) {
        String[] info = getCanonicalInfo(tzid);
        if (info != null) {
            return info[1];
        }
        return null;
    }

    /**
     * Return the country code if this is a 'single' time zone that can fallback to just
     * the country, otherwise return null.  (Note, one must also check the locale data
     * to see that there is a localization for the country in order to implement
     * tr#35 appendix J step 5.)
     */
    public static String getSingleCountry(String tzid) {
        String[] info = getCanonicalInfo(tzid);
        if (info != null && info[2] != null) {
            return info[1];
        }
        return null;
    }

    /**
     * Handle fallbacks for generic time (rules E.. G)
     */
    public static String displayFallback(String tzid, String city, ULocale locale) {
        String[] info = getCanonicalInfo(tzid);
        if (info == null) {
            return null; // error
        }

        String country_code = info[1];
        if (country_code == null) {
                return null; // error!   
        }

        String country = null;
        if (country_code != null) {
            ICUResourceBundle rb = 
                (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
            if (rb.getLoadingStatus() != rb.FROM_ROOT && rb.getLoadingStatus() != rb.FROM_DEFAULT) {
                country = ULocale.getDisplayCountry("xx_" + country_code, locale);
            }
            if (country == null || country.length() == 0) country = country_code;
        }
        
        // This is not behavior specified in tr35, but behavior added by Mark.  
        // TR35 says to display the country _only_ if there is a localization.
        if (info[2] != null) { // single country
            return displayRegion(country, locale);
        }

        if (city == null) {
            city = tzid.substring(tzid.lastIndexOf('/')+1).replace('_',' ');
        }

        String flbPat = getTZLocalizationInfo(locale, FALLBACK_FORMAT);
        MessageFormat mf = new MessageFormat(flbPat);

        return mf.format(new Object[] { city, country });
    }

    public static String displayRegion(String cityOrCountry, ULocale locale) {
        String regPat = getTZLocalizationInfo(locale, REGION_FORMAT);
        MessageFormat mf = new MessageFormat(regPat);
        return mf.format(new Object[] { cityOrCountry });
    }

    public static String displayGMT(long value, ULocale locale) {
        String msgpat = getTZLocalizationInfo(locale, GMT);
        String dtepat = getTZLocalizationInfo(locale, HOUR);
        
        int n = dtepat.indexOf(';');
        if (n != -1) {
            if (value < 0) {
                value = - value;
                dtepat = dtepat.substring(n+1);
            } else {
                dtepat = dtepat.substring(0, n);
            }
        }

        final long mph = 3600000;
        final long mpm = 60000;

        SimpleDateFormat sdf = new SimpleDateFormat(dtepat, locale);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String res = sdf.format(new Long(value));
        MessageFormat mf = new MessageFormat(msgpat);
        res = mf.format(new Object[] { res });
        return res;
    }

    public static final int
        PREFIX = 0,
        HOUR = 1,
        GMT = 2,
        REGION_FORMAT = 3,
        FALLBACK_FORMAT = 4;
     
    /**
     * Get the index'd tz datum for this locale.  Index must be one of the 
     * values PREFIX, HOUR, GMT, REGION_FORMAT, FALLBACK_FORMAT
     */
    public static String getTZLocalizationInfo(ULocale locale, int index) {
        String baseName = locale.getBaseName();
        for (int i = 0; i < TZ_LOCALIZATION_INFO.length; ++i) {
            String[] info = TZ_LOCALIZATION_INFO[i];
            String prefix = info[PREFIX];
            if (prefix == null
                || (index < info.length
                    && info[index] != null
                    && baseName.indexOf(prefix) == 0)) {

                return info[index];
            }
        }

        throw new InternalError(); // should never get here
    }

    // temporary for icu4j 3.4
    // locale, hour, gmt, region, fallback
    private static final String[][] TZ_LOCALIZATION_INFO = {
        { "am", "+HHmm;-HHmm" },
        { "bg", "+HHmm;-HHmm", "\u0413\u0440\u0438\u0438\u043d\u0443\u0438\u0447{0}" },
        { "cy", "+HHmm;-HHmm" },
        { "el", "+HHmm;-HHmm" },
        { "hr", "+HHmm;-HHmm" },
        { "ja", "+HHmm;-HHmm", null, "{0}\u6642\u9593", "{0} ({1})\u6642\u9593" },
        { "nn", "+HH.mm;-HH.mm" },
        { "sk", "+HHmm;-HHmm" },
        { "sl", "+HHmm;-HHmm" },
        { "sr", "+HHmm;-HHmm" },
        { "sv", "+HH.mm;-HH.mm", "GMT" },
        { "th", "+HHmm;-HHmm" },
        { "uk", "+HHmm;-HHmm" },
        { "zh_Hant", "+HH:mm;-HH:mm" },
        { "zh", "+HHmm;-HHmm" },
        { null, "+HH:mm;-HH:mm", "GMT{0}", "{0}", "{0} ({1})" }
    };

    private static Set getValidIDs() {
        // Construct list of time zones that are valid, according
        // to the current underlying core JDK.  We have to do this
        // at runtime since we don't know what we're running on.
        Set valid = new TreeSet();
        valid.addAll(Arrays.asList(java.util.TimeZone.getAvailableIDs()));
        return valid;
    }

    /**
     * Empty string array.
     */
    private static final String[] EMPTY = new String[0];

    /**
     * Map of country codes to zone lists.
     */
    private static Map COUNTRY_MAP = null;

    /**
     * Map of zones to equivalent zone lists.
     */
    private static Map EQUIV_MAP = null;
}
