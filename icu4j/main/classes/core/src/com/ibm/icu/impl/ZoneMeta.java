/*
**********************************************************************
* Copyright (c) 2003-2010 International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: September 4 2003
* Since: ICU 2.8
**********************************************************************
*/
package com.ibm.icu.impl;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.SimpleTimeZone;
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
    private static final boolean ASSERT = false;

    private static final String ZONEINFORESNAME = "zoneinfo64";
    private static final String kREGIONS  = "Regions";
    private static final String kZONES    = "Zones";
    private static final String kNAMES    = "Names";

    private static final String kGMT_ID   = "GMT";
    private static final String kCUSTOM_TZ_PREFIX = "GMT";

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
        String[] ids = null;

        try{
            UResourceBundle top = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(
                    ICUResourceBundle.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle regions = top.get(kREGIONS);

            // Create a list of zones associated with the country
            List<String> countryZones = new ArrayList<String>();

            for (int i = 0; i < regions.getSize(); i++) {
                if (country.equals(regions.getString(i))) {
                    String zoneName = getZoneID(i);
                    countryZones.add(zoneName);
                }
            }
            if (countryZones.size() > 0) {
                ids = countryZones.toArray(new String[countryZones.size()]);
            }
        } catch (MissingResourceException ex){
            //throw away the exception
        }

        if (ids == null) {
            ids = new String[0];
        }
        return ids;
    }

    public static synchronized String[] getAvailableIDs() {
        String[] ids = getZoneIDs();
        if (ids == null) {
            return new String[0];
        }
        return ids.clone();
    }

    public static synchronized String[] getAvailableIDs(int offset){
        String[] ids = null;
        String[] all = getZoneIDs();
        if (all != null) {
            ArrayList<String> zones = new ArrayList<String>();
            for (String zid : all) {
                // This is VERY inefficient.
                TimeZone z = TimeZone.getTimeZone(zid);
                // Make sure we get back the ID we wanted (if the ID is
                // invalid we get back GMT).
                if (z != null && z.getID().equals(zid) && z.getRawOffset() == offset) {
                    zones.add(zid);
                }
            }
            if (zones.size() > 0) {
                ids = zones.toArray(new String[zones.size()]);
            }
        }
        if (ids == null) {
            ids = new String[0];
        }
        return ids;
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
        int count = 0;
        try {
            UResourceBundle res = openOlsonResource(null, id);
            UResourceBundle links = res.get("links");
            int[] v = links.getIntVector();
            count = v.length;
        } catch (MissingResourceException ex) {
            // throw away
        }
        return count;
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
        String result = "";
        int zoneIdx = -1;

        if (index >= 0) {
            try {
                UResourceBundle res = openOlsonResource(null, id);
                UResourceBundle links = res.get("links");
                int[] zones = links.getIntVector();
                if (index < zones.length) {
                    zoneIdx = zones[index];
                }
            } catch (MissingResourceException ex) {
                // throw away
                zoneIdx = -1;
            }
        }
        if (zoneIdx >= 0) {
            String tmp = getZoneID(zoneIdx);
            if (tmp != null) {
                result = tmp;
            }
        }
        return result;
    }

    private static String[] ZONEIDS = null;

    /*
     * ICU frequently refers the zone ID array in zoneinfo resource
     */
    private static synchronized String[] getZoneIDs() {
        if (ZONEIDS == null) {
            try {
                UResourceBundle top = UResourceBundle.getBundleInstance(
                        ICUResourceBundle.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle names = top.get(kNAMES);
                ZONEIDS = names.getStringArray();
            } catch (MissingResourceException ex) {
                // throw away..
            }
        }
        if (ZONEIDS == null) {
            ZONEIDS = new String[0];
        }
        return ZONEIDS;
    }

    private static String getZoneID(int idx) {
        if (idx >= 0) {
            String[] ids = getZoneIDs();
            if (idx < ids.length) {
                return ids[idx];
            }
        }
        return null;
    }

    private static int getZoneIndex(String zid) {
        int zoneIdx = -1;

        String[] all = getZoneIDs();
        if (all.length > 0) {
            int start = 0;
            int limit = all.length;

            int lastMid = Integer.MAX_VALUE;
            for (;;) {
                int mid = (start + limit) / 2;
                if (lastMid == mid) {   /* Have we moved? */
                    break;  /* We haven't moved, and it wasn't found. */
                }
                lastMid = mid;
                int r = zid.compareTo(all[mid]);
                if (r == 0) {
                    zoneIdx = mid;
                    break;
                } else if(r < 0) {
                    limit = mid;
                } else {
                    start = mid;
                }
            }
        }

        return zoneIdx;
    }


    private static ICUCache<String, String> CANONICAL_ID_CACHE = new SimpleCache<String, String>();
    private static ICUCache<String, String> REGION_CACHE = new SimpleCache<String, String>();
    private static ICUCache<String, Boolean> SINGLE_COUNTRY_CACHE = new SimpleCache<String, Boolean>();

    /**
     * Return the canonical id for this system tzid, which might be the id itself.
     * If the given system tzid is not know, return null.
     */
    public static String getCanonicalSystemID(String tzid) {
        String canonical = CANONICAL_ID_CACHE.get(tzid);
        if (canonical == null) {
            int zoneIdx = getZoneIndex(tzid);
            if (zoneIdx >= 0) {
                try {
                    UResourceBundle top = UResourceBundle.getBundleInstance(
                            ICUResourceBundle.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                    UResourceBundle zones = top.get(kZONES);
                    UResourceBundle zone = zones.get(zoneIdx);
                    if (zone.getType() == UResourceBundle.INT) {
                        // resolve link
                        String tmp = getZoneID(zone.getInt());
                        if (tmp != null) {
                            canonical = tmp;
                        }
                    } else {
                        canonical = tzid;
                    }
                    // check canonical mapping in CLDR
                    UResourceBundle keyTypeData = UResourceBundle.getBundleInstance(
                            ICUResourceBundle.ICU_BASE_NAME, "keyTypeData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                    UResourceBundle typeAlias = keyTypeData.get("typeAlias");
                    UResourceBundle aliasesForKey = typeAlias.get("timezone");
                    String cldrCanonical = aliasesForKey.getString(canonical.replace('/', ':'));
                    if (cldrCanonical != null) {
                        canonical = cldrCanonical;
                    }
                } catch (MissingResourceException e) {
                    // fall through
                }
            }
            if (canonical != null) {
                CANONICAL_ID_CACHE.put(tzid, canonical);
            }
        }
        return canonical;
    }

    /**
     * Return the canonical country code for this tzid.  If we have none, or if the time zone
     * is not associated with a country, return null.
     */
    public static String getCanonicalCountry(String tzid) {
        String region = REGION_CACHE.get(tzid);
        if (region == null) {
            int zoneIdx = getZoneIndex(tzid);
            if (zoneIdx >= 0) {
                try {
                    UResourceBundle top = UResourceBundle.getBundleInstance(
                            ICUResourceBundle.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                    UResourceBundle regions = top.get(kREGIONS);
                    if (zoneIdx < regions.getSize()) {
                        region = regions.getString(zoneIdx);
                    }
                } catch (MissingResourceException e) {
                    // throw away
                }
                if (region != null) {
                    REGION_CACHE.put(tzid, region);
                }
            }
        }
        if (region.equals("001")) {
            return null;
        }
        return region;
    }

    /**
     * Return the country code if this is a 'single' time zone that can fallback to just
     * the country, otherwise return null.  (Note, one must also check the locale data
     * to see that there is a localization for the country in order to implement
     * tr#35 appendix J step 5.)
     */
    public static String getSingleCountry(String tzid) {
        String country = getCanonicalCountry(tzid);
        if (country != null) {
            Boolean isSingle = SINGLE_COUNTRY_CACHE.get(tzid);
            if (isSingle == null) {
                // This is not so efficient
                boolean isSingleCountryZone = true;
                String[] ids = TimeZone.getAvailableIDs(country);
                if (ids.length > 1) {
                    // Check if there are multiple canonical zones included
                    String canonical = getCanonicalSystemID(ids[0]);
                    for (int i = 1; i < ids.length; i++) {
                        if (!canonical.equals(getCanonicalSystemID(ids[i]))) {
                            isSingleCountryZone = false;
                            break;
                        }
                    }
                }
                isSingle = Boolean.valueOf(isSingleCountryZone);
                SINGLE_COUNTRY_CACHE.put(tzid, isSingle);
            }
            if (!isSingle) {
                country = null;
            }
        }
        return country;
    }

    /**
     * Returns a time zone location(region) format string defined by UTR#35.
     * e.g. "Italy Time", "United States (Los Angeles) Time"
     */
    public static String getLocationFormat(String tzid, String city, ULocale locale) {
        String country_code = getCanonicalCountry(tzid);
        if (country_code == null) {
            // no location is associated
            return null;
        }

        String country = null;
        try {
            ICUResourceBundle rb = 
                (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_REGION_BASE_NAME, locale);
//
// TODO: There is a design bug in UResourceBundle and getLoadingStatus() does not work well.
//
//                if (rb.getLoadingStatus() != ICUResourceBundle.FROM_ROOT && rb.getLoadingStatus() != ICUResourceBundle.FROM_DEFAULT) {
//                    country = ULocale.getDisplayCountry("xx_" + country_code, locale);
//                }
// START WORKAROUND
            ULocale rbloc = rb.getULocale();
            if (!rbloc.equals(ULocale.ROOT) && rbloc.getLanguage().equals(locale.getLanguage())) {
                country = ULocale.getDisplayCountry("xx_" + country_code, locale);
            }
// END WORKAROUND
        } catch (MissingResourceException e) {
            // fall through
        }
        if (country == null || country.length() == 0) {
            country = country_code;
        }

        // This is not behavior specified in tr35, but behavior added by Mark.  
        // TR35 says to display the country _only_ if there is a localization.
        if (getSingleCountry(tzid) != null) { // single country
            String regPat = getTZLocalizationInfo(locale, REGION_FORMAT);
            if (regPat == null) {
                regPat = DEF_REGION_FORMAT;
            }
            MessageFormat mf = new MessageFormat(regPat);
            return mf.format(new Object[] { country });
        }

        if (city == null) {
            city = tzid.substring(tzid.lastIndexOf('/')+1).replace('_',' ');
        }

        String flbPat = getTZLocalizationInfo(locale, FALLBACK_FORMAT);
        if (flbPat == null) {
            flbPat = DEF_FALLBACK_FORMAT;
        }
        MessageFormat mf = new MessageFormat(flbPat);

        return mf.format(new Object[] { city, country });
    }

    private static final String DEF_REGION_FORMAT = "{0}";
    private static final String DEF_FALLBACK_FORMAT = "{1} ({0})";

    public static final String
        HOUR = "hourFormat",
        GMT = "gmtFormat",
        REGION_FORMAT = "regionFormat",
        FALLBACK_FORMAT = "fallbackFormat",
        ZONE_STRINGS = "zoneStrings",
        FORWARD_SLASH = "/";
     
    /**
     * Get the index'd tz datum for this locale.  Index must be one of the 
     * values PREFIX, HOUR, GMT, REGION_FORMAT, FALLBACK_FORMAT
     */
    public static String getTZLocalizationInfo(ULocale locale, String format) {
        String result = null;
        try {
            ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance(
                ICUResourceBundle.ICU_ZONE_BASE_NAME, locale);
            result = bundle.getStringWithFallback(ZONE_STRINGS+FORWARD_SLASH+format);
        } catch (MissingResourceException e) {
            result = null;
        }
        return result;
    }

//    private static Set getValidIDs() {
//        // Construct list of time zones that are valid, according
//        // to the current underlying core JDK.  We have to do this
//        // at runtime since we don't know what we're running on.
//        Set valid = new TreeSet();
//        valid.addAll(Arrays.asList(java.util.TimeZone.getAvailableIDs()));
//        return valid;
//    }


    /**
     * Given an ID and the top-level resource of the zoneinfo resource,
     * open the appropriate resource for the given time zone.
     * Dereference links if necessary.
     * @param top the top level resource of the zoneinfo resource or null.
     * @param id zone id
     * @return the corresponding zone resource or null if not found
     */
    public static UResourceBundle openOlsonResource(UResourceBundle top, String id)
    {
        UResourceBundle res = null;
        int zoneIdx = getZoneIndex(id);
        if (zoneIdx >= 0) {
            try {
                if (top == null) {
                    top = UResourceBundle.getBundleInstance(
                            ICUResourceBundle.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                }
                UResourceBundle zones = top.get(kZONES);
                UResourceBundle zone = zones.get(zoneIdx);
                if (zone.getType() == UResourceBundle.INT) {
                    // resolve link
                    zone = zones.get(zone.getInt());
                }
                res = zone;
            } catch (MissingResourceException e) {
                res = null;
            }
        }
        return res;
    }


    private static ICUCache<String, TimeZone> SYSTEM_ZONE_CACHE = new SimpleCache<String, TimeZone>();

    /**
     * Lookup the given name in our system zone table.  If found,
     * instantiate a new zone of that name and return it.  If not
     * found, return 0.
     */
    public static TimeZone getSystemTimeZone(String id) {
        TimeZone z = SYSTEM_ZONE_CACHE.get(id);
        if (z == null) {
            try{
                UResourceBundle top = UResourceBundle.getBundleInstance(
                        ICUResourceBundle.ICU_BASE_NAME, ZONEINFORESNAME, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle res = openOlsonResource(top, id);
                z = new OlsonTimeZone(top, res);
                z.setID(id);
                SYSTEM_ZONE_CACHE.put(id, z);
            }catch(Exception ex){
                return null;
            }
        }
        return (TimeZone)z.clone();
    }

    public static TimeZone getGMT(){
        TimeZone z = new SimpleTimeZone(0, kGMT_ID);
        z.setID(kGMT_ID);
        return z;
    }

    // Maximum value of valid custom time zone hour/min
    private static final int kMAX_CUSTOM_HOUR = 23;
    private static final int kMAX_CUSTOM_MIN = 59;
    private static final int kMAX_CUSTOM_SEC = 59;

    /**
     * Parse a custom time zone identifier and return a corresponding zone.
     * @param id a string of the form GMT[+-]hh:mm, GMT[+-]hhmm, or
     * GMT[+-]hh.
     * @return a newly created SimpleTimeZone with the given offset and
     * no Daylight Savings Time, or null if the id cannot be parsed.
    */
    public static TimeZone getCustomTimeZone(String id){
        int[] fields = new int[4];
        if (parseCustomID(id, fields)) {
            String zid = formatCustomID(fields[1], fields[2], fields[3], fields[0] < 0);
            int offset = fields[0] * ((fields[1] * 60 + fields[2]) * 60 + fields[3]) * 1000;
            return new SimpleTimeZone(offset, zid);
        }
        return null;
    }

    /**
     * Parse a custom time zone identifier and return the normalized
     * custom time zone identifier for the given custom id string.
     * @param id a string of the form GMT[+-]hh:mm, GMT[+-]hhmm, or
     * GMT[+-]hh.
     * @return The normalized custom id string.
    */
    public static String getCustomID(String id) {
        int[] fields = new int[4];
        if (parseCustomID(id, fields)) {
            return formatCustomID(fields[1], fields[2], fields[3], fields[0] < 0);
        }
        return null;
    }

    /*
     * Parses the given custom time zone identifier
     * @param id id A string of the form GMT[+-]hh:mm, GMT[+-]hhmm, or
     * GMT[+-]hh.
     * @param fields An array of int (length = 4) to receive the parsed
     * offset time fields.  The sign is set to fields[0] (-1 or 1),
     * hour is set to fields[1], minute is set to fields[2] and second is
     * set to fields[3].
     * @return Returns true when the given custom id is valid.
     */
    static boolean parseCustomID(String id, int[] fields) {
        NumberFormat numberFormat = null;
        String idUppercase = id.toUpperCase();

        if (id != null && id.length() > kGMT_ID.length() &&
            idUppercase.startsWith(kGMT_ID)) {
            ParsePosition pos = new ParsePosition(kGMT_ID.length());
            int sign = 1;
            int hour = 0;
            int min = 0;
            int sec = 0;

            if (id.charAt(pos.getIndex()) == 0x002D /*'-'*/) {
                sign = -1;
            } else if (id.charAt(pos.getIndex()) != 0x002B /*'+'*/) {
                return false;
            }
            pos.setIndex(pos.getIndex() + 1);

            numberFormat = NumberFormat.getInstance();
            numberFormat.setParseIntegerOnly(true);

            // Look for either hh:mm, hhmm, or hh
            int start = pos.getIndex();

            Number n = numberFormat.parse(id, pos);
            if (pos.getIndex() == start) {
                return false;
            }
            hour = n.intValue();

            if (pos.getIndex() < id.length()){
                if (pos.getIndex() - start > 2
                        || id.charAt(pos.getIndex()) != 0x003A /*':'*/) {
                    return false;
                }
                // hh:mm
                pos.setIndex(pos.getIndex() + 1);
                int oldPos = pos.getIndex();
                n = numberFormat.parse(id, pos);
                if ((pos.getIndex() - oldPos) != 2) {
                    // must be 2 digits
                    return false;
                }
                min = n.intValue();
                if (pos.getIndex() < id.length()) {
                    if (id.charAt(pos.getIndex()) != 0x003A /*':'*/) {
                        return false;
                    }
                    // [:ss]
                    pos.setIndex(pos.getIndex() + 1);
                    oldPos = pos.getIndex();
                    n = numberFormat.parse(id, pos);
                    if (pos.getIndex() != id.length()
                            || (pos.getIndex() - oldPos) != 2) {
                        return false;
                    }
                    sec = n.intValue();
                }
            } else {
                // Supported formats are below -
                //
                // HHmmss
                // Hmmss
                // HHmm
                // Hmm
                // HH
                // H

                int length = pos.getIndex() - start;
                if (length <= 0 || 6 < length) {
                    // invalid length
                    return false;
                }
                switch (length) {
                    case 1:
                    case 2:
                        // already set to hour
                        break;
                    case 3:
                    case 4:
                        min = hour % 100;
                        hour /= 100;
                        break;
                    case 5:
                    case 6:
                        sec = hour % 100;
                        min = (hour/100) % 100;
                        hour /= 10000;
                        break;
                }
            }

            if (hour <= kMAX_CUSTOM_HOUR && min <= kMAX_CUSTOM_MIN && sec <= kMAX_CUSTOM_SEC) {
                if (fields != null) {
                    if (fields.length >= 1) {
                        fields[0] = sign;
                    }
                    if (fields.length >= 2) {
                        fields[1] = hour;
                    }
                    if (fields.length >= 3) {
                        fields[2] = min;
                    }
                    if (fields.length >= 4) {
                        fields[3] = sec;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a custom zone for the offset
     * @param offset GMT offset in milliseconds
     * @return A custom TimeZone for the offset with normalized time zone id
     */
    public static TimeZone getCustomTimeZone(int offset) {
        boolean negative = false;
        int tmp = offset;
        if (offset < 0) {
            negative = true;
            tmp = -offset;
        }

        int hour, min, sec, millis;

        millis = tmp % 1000;
        if (ASSERT) {
            Assert.assrt("millis!=0", millis != 0);
        }
        tmp /= 1000;
        sec = tmp % 60;
        tmp /= 60;
        min = tmp % 60;
        hour = tmp / 60;

        // Note: No millisecond part included in TZID for now
        String zid = formatCustomID(hour, min, sec, negative);

        return new SimpleTimeZone(offset, zid);
    }

    /*
     * Returns the normalized custom TimeZone ID
     */
    static String formatCustomID(int hour, int min, int sec, boolean negative) {
        // Create normalized time zone ID - GMT[+|-]hh:mm[:ss]
        StringBuilder zid = new StringBuilder(kCUSTOM_TZ_PREFIX);
        if (hour != 0 || min != 0) {
            if(negative) {
                zid.append('-');
            } else {
                zid.append('+');
            }
            // Always use US-ASCII digits
            if (hour < 10) {
                zid.append('0');
            }
            zid.append(hour);
            zid.append(':');
            if (min < 10) {
                zid.append('0');
            }
            zid.append(min);

            if (sec != 0) {
                // Optional second field
                zid.append(':');
                if (sec < 10) {
                    zid.append('0');
                }
                zid.append(sec);
            }
        }
        return zid.toString();
    }

    /**
     * Returns a CLDR metazone ID for the given Olson tzid and time.
     */
    public static String getMetazoneID(String olsonID, long date) {
        String mzid = null;
        List<OlsonToMetaMappingEntry> mappings = getOlsonToMatazones(olsonID);
        if (mappings != null) {
            for (int i = 0; i < mappings.size(); i++) {
                OlsonToMetaMappingEntry mzm = mappings.get(i);
                if (date >= mzm.from && date < mzm.to) {
                    mzid = mzm.mzid;
                    break;
                }
            }
        }
        return mzid;
    }

    private static ICUCache<String, List<OlsonToMetaMappingEntry>> OLSON_TO_META_CACHE =
        new SimpleCache<String, List<OlsonToMetaMappingEntry>>();

    static class OlsonToMetaMappingEntry {
        String mzid;
        long from;
        long to;
    }

    static List<OlsonToMetaMappingEntry> getOlsonToMatazones(String tzid) {
        List<OlsonToMetaMappingEntry> mzMappings = OLSON_TO_META_CACHE.get(tzid);
        if (mzMappings == null) {
            try {
                UResourceBundle bundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "metaZones");
                UResourceBundle metazoneInfoBundle = bundle.get("metazoneInfo");

                String canonicalID = TimeZone.getCanonicalID(tzid);
                if (canonicalID == null) {
                    return null;
                }
                String tzkey = canonicalID.replace('/', ':');
                UResourceBundle zoneBundle = metazoneInfoBundle.get(tzkey);

                mzMappings = new LinkedList<OlsonToMetaMappingEntry>();

                for (int idx = 0; idx < zoneBundle.getSize(); idx++) {
                    UResourceBundle mz = zoneBundle.get(idx);
                    String mzid = mz.getString(0);
                    String from = "1970-01-01 00:00";
                    String to = "9999-12-31 23:59";
                    if (mz.getSize() == 3) {
                        from = mz.getString(1);
                        to = mz.getString(2);
                    }
                    OlsonToMetaMappingEntry mzmap = new OlsonToMetaMappingEntry();
                    mzmap.mzid = mzid.intern();
                    try {
                        mzmap.from = parseDate(from);
                        mzmap.to = parseDate(to);
                    } catch (IllegalArgumentException baddate) {
                        // skip this
                        continue;
                    }
                    // Add this mapping to the list
                    mzMappings.add(mzmap);
                }

            } catch (MissingResourceException mre) {
                // fall through
            }
            if (mzMappings != null) {
                OLSON_TO_META_CACHE.put(tzid, mzMappings);
            }
        }
        return mzMappings;
    }

    /*
     * Convert a date string used by metazone mappings to long.
     * The format used by CLDR metazone mapping is "yyyy-MM-dd HH:mm".
     * We do not want to use SimpleDateFormat to parse the metazone
     * mapping range strings in createOlsonToMeta, because it might be
     * called from SimpleDateFormat initialization code.
     */
     static long parseDate (String text) throws IllegalArgumentException {
        int year = 0, month = 0, day = 0, hour = 0, min = 0;
        int idx;
        int n;

        // "yyyy" (0 - 3)
        for (idx = 0; idx <= 3; idx++) {
            n = text.charAt(idx) - '0';
            if (n >= 0 && n < 10) {
                year = 10*year + n;
            } else {
                throw new IllegalArgumentException("Bad year");
            }
        }
        // "MM" (5 - 6)
        for (idx = 5; idx <= 6; idx++) {
            n = text.charAt(idx) - '0';
            if (n >= 0 && n < 10) {
                month = 10*month + n;
            } else {
                throw new IllegalArgumentException("Bad month");
            }
        }
        // "dd" (8 - 9)
        for (idx = 8; idx <= 9; idx++) {
            n = text.charAt(idx) - '0';
            if (n >= 0 && n < 10) {
                day = 10*day + n;
            } else {
                throw new IllegalArgumentException("Bad day");
            }
        }
        // "HH" (11 - 12)
        for (idx = 11; idx <= 12; idx++) {
            n = text.charAt(idx) - '0';
            if (n >= 0 && n < 10) {
                hour = 10*hour + n;
            } else {
                throw new IllegalArgumentException("Bad hour");
            }
        }
        // "mm" (14 - 15)
        for (idx = 14; idx <= 15; idx++) {
            n = text.charAt(idx) - '0';
            if (n >= 0 && n < 10) {
                min = 10*min + n;
            } else {
                throw new IllegalArgumentException("Bad minute");
            }
        }

        long date = Grego.fieldsToDay(year, month - 1, day) * Grego.MILLIS_PER_DAY
                    + hour * Grego.MILLIS_PER_HOUR + min * Grego.MILLIS_PER_MINUTE;
        return date;
     }

     private static ICUCache<String, String> META_TO_OLSON_CACHE =
         new SimpleCache<String, String>();

     /**
      * Returns an Olson ID for the ginve metazone and region
      */
     public static String getZoneIdByMetazone(String metazoneID, String region) {
         String tzid = null;
         String keyWithRegion = (region == null || region.length() == 0) ? null : metazoneID + ":" + region;

         // look up in the cache first
         if (keyWithRegion != null) {
             tzid = META_TO_OLSON_CACHE.get(metazoneID + ":" + region);
         }
         if (tzid == null) {
             tzid = META_TO_OLSON_CACHE.get(metazoneID);
         }

         // look up in the resource bundle
         if (tzid == null) {
             try {
                 UResourceBundle bundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "metaZones");
                 UResourceBundle mapTimezones = bundle.get("mapTimezones");

                 if (keyWithRegion != null) {
                     try {
                         tzid = mapTimezones.getString(keyWithRegion);
                         META_TO_OLSON_CACHE.put(keyWithRegion, tzid);
                     } catch (MissingResourceException e) {
                         // fall through
                     }
                 }
                 if (tzid == null) {
                     tzid = mapTimezones.getString(metazoneID);
                     META_TO_OLSON_CACHE.put(metazoneID, tzid);
                 }
             } catch (MissingResourceException mre) {
                 // do nothing
             }
         }

         return tzid;
     }
}
