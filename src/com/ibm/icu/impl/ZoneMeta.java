/*
**********************************************************************
* Copyright (c) 2003-2008 International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: September 4 2003
* Since: ICU 2.8
**********************************************************************
*/
package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.text.ParsePosition;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.Vector;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

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
        if(!getOlsonMeta()){
            return EMPTY;
        }
        try{
            UResourceBundle top = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle regions = top.get(kREGIONS);
            UResourceBundle names = top.get(kNAMES); // dereference Zones section
            UResourceBundle temp = regions.get(country);
            int[] vector = temp.getIntVector();
            if (ASSERT) Assert.assrt("vector.length>0", vector.length>0);
            String[] ret = new String[vector.length];
            for (int i=0; i<vector.length; ++i) {
                if (ASSERT) Assert.assrt("vector[i] >= 0 && vector[i] < OLSON_ZONE_COUNT", 
                        vector[i] >= 0 && vector[i] < OLSON_ZONE_COUNT);
                ret[i] = names.getString(vector[i]);
            }
            return ret;
        }catch(MissingResourceException ex){
            //throw away the exception
        }
        return EMPTY;
    }
    public static synchronized String[] getAvailableIDs() {
        if(!getOlsonMeta()){
            return EMPTY;
        }
        try{
            UResourceBundle top = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle names = top.get(kNAMES); // dereference Zones section
            return names.getStringArray();
        }catch(MissingResourceException ex){
            //throw away the exception
        }
        return EMPTY;
    }
    public static synchronized String[] getAvailableIDs(int offset){
        Vector vector = new Vector();
        for (int i=0; i<OLSON_ZONE_COUNT; ++i) {
            String unistr;
            if ((unistr=getID(i))!=null) {
                // This is VERY inefficient.
                TimeZone z = TimeZone.getTimeZone(unistr);
                // Make sure we get back the ID we wanted (if the ID is
                // invalid we get back GMT).
                if (z != null && z.getID().equals(unistr) &&
                    z.getRawOffset() == offset) {
                    vector.add(unistr);
                }
            }
        }
        if(!vector.isEmpty()){
            String[] strings = new String[vector.size()];
            return (String[])vector.toArray(strings);
        }
        return EMPTY;
    }
    private static String getID(int i) {
        try{
            UResourceBundle top = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle names = top.get(kNAMES); // dereference Zones section
            return names.getString(i);
        }catch(MissingResourceException ex){
            //throw away the exception
        }
        return null;
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

        UResourceBundle res = openOlsonResource(id);
        int size = res.getSize();
        if (size == 4 || size == 6) {
            UResourceBundle r=res.get(size-1);
            //result = ures_getSize(&r); // doesn't work
            int[] v = r.getIntVector();
            return v.length;
        }
        return 0;
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
        String result="";
        UResourceBundle res = openOlsonResource(id);
        if (res != null) {
            int zone = -1;
            int size = res.getSize();
            if (size == 4 || size == 6) {
                UResourceBundle r = res.get(size-1);
                int[] v = r.getIntVector();
                if (index >= 0 && index < v.length) {
                    zone = v[index];
                }
            }
            if (zone >= 0) {
                try {
                    UResourceBundle top = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo",
                            ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                    UResourceBundle ares = top.get(kNAMES); // dereference Zones section
                    result = ares.getString(zone);
                } catch (MissingResourceException e) {
                    result = "";
                }
            }
        }
        return result;
    }

    private static String[] getCanonicalInfo(String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        if (canonicalMap == null) {
            Map m = new HashMap();
            Set s = new HashSet();
            try {
                UResourceBundle supplementalDataBundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
    
                UResourceBundle zoneFormatting = supplementalDataBundle.get("zoneFormatting");
                UResourceBundleIterator it = zoneFormatting.getIterator();
    
                while ( it.hasNext()) {
                    UResourceBundle temp = it.next();
                    int resourceType = temp.getType();
    
                    switch(resourceType) {
                        case UResourceBundle.TABLE:
                            String [] result = { "", "" };
                            UResourceBundle zoneInfo = temp;
                            String canonicalID = zoneInfo.getKey().replace(':','/');
                            String territory = zoneInfo.get("territory").getString();
                            result[0] = canonicalID;
                            if ( territory.equals("001")) {
                                result[1] = null;
                            }
                            else {
                                result[1] = territory;
                            }
                            m.put(canonicalID,result);
                            try {
                                UResourceBundle aliasBundle = zoneInfo.get("aliases");
                                String [] aliases = aliasBundle.getStringArray();
                                for (int i=0 ; i<aliases.length; i++) {
                                   m.put(aliases[i],result);
                                }
                            } catch(MissingResourceException ex){
                                // Disregard if there are no aliases
                            }
                            break;
                        case UResourceBundle.ARRAY:
                            String[] territoryList = temp.getStringArray();
                            for (int i=0 ; i < territoryList.length; i++) {
                                s.add(territoryList[i]);
                            }
                            break;
                    }
                }
            } catch (MissingResourceException e) {
                // throws away the exception - maps are empty for this case
            }

            // Some available Olson zones are not included in CLDR data (such as Asia/Riyadh87).
            // Also, when we update Olson tzdata, new zones may be added.
            // This code scans all available zones in zoneinfo.res, and if any of them are
            // missing, add them to the map.
            try{
                UResourceBundle top = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,
                        "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle names = top.get(kNAMES);
                String[] ids = names.getStringArray();
                for (int i = 0; i < ids.length; i++) {
                    if (m.containsKey(ids[i])) {
                        // Already included in CLDR data
                        continue;
                    }
                    // Not in CLDR data, but it could be new one whose alias is
                    // available in CLDR
                    String[] tmpinfo = null;
                    int nTzdataEquivalent = TimeZone.countEquivalentIDs(ids[i]);
                    for (int j = 0; j < nTzdataEquivalent; j++) {
                        String alias = TimeZone.getEquivalentID(ids[i], j);
                        if (alias.equals(ids[i])) {
                            continue;
                        }
                        tmpinfo = (String[])m.get(alias);
                        if (tmpinfo != null) {
                            break;
                        }
                    }
                    if (tmpinfo == null) {
                        // Set dereferenced zone ID as the canonical ID
                        UResourceBundle res = getZoneByName(top, ids[i]);
                        String derefID = (res.getSize() == 1) ? ids[res.getInt()] : ids[i];
                        m.put(ids[i], new String[] {derefID, null});
                    } else {
                        // Use the canonical ID in the existing entry
                        m.put(ids[i], tmpinfo);
                    }
                }
            } catch (MissingResourceException ex) {
                //throw away the exception
            }

            synchronized (ZoneMeta.class) {
                canonicalMap = m;
                multiZoneTerritories = s;
            }
        }

        return (String[])canonicalMap.get(id);
    }

    private static Map canonicalMap = null;
    private static Set multiZoneTerritories = null;

    /**
     * Return the canonical id for this system tzid, which might be the id itself.
     * If the given system tzid is not know, return null.
     */
    public static String getCanonicalSystemID(String tzid) {
        String[] info = getCanonicalInfo(tzid);
        if (info != null) {
            return info[0];
        }
        return null;
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
        if (info != null && info[1] != null && !multiZoneTerritories.contains(info[1])) {
            return info[1];
        }
        return null;
    }

    /**
     * Returns a time zone location(region) format string defined by UTR#35.
     * e.g. "Italy Time", "United States (Los Angeles) Time"
     */
    public static String getLocationFormat(String tzid, String city, ULocale locale) {
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
            try {
                ICUResourceBundle rb = 
                    (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
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
            ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance(locale);
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
     * Empty string array.
     */
    private static final String[] EMPTY = new String[0];



    /**
     * Given an ID, open the appropriate resource for the given time zone.
     * Dereference aliases if necessary.
     * @param id zone id
     * @return top-level resource bundle
     */
    public static UResourceBundle openOlsonResource(String id)
    {
        UResourceBundle res = null;
        try {
            ICUResourceBundle top = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            res = getZoneByName(top, id);
            // Dereference if this is an alias.  Docs say result should be 1
            // but it is 0 in 2.8 (?).
             if (res.getSize() <= 1) {
                int deref = res.getInt() + 0;
                UResourceBundle ares = top.get(kZONES); // dereference Zones section
                res = (ICUResourceBundle) ares.get(deref);
            }
        } catch (MissingResourceException e) {
            res = null;
        }
        return res;
    }

    /**
     * Fetch a specific zone by name.  Replaces the getByKey call. 
     * @param top Top timezone resource
     * @param id Time zone ID
     * @return the zone's bundle if found, or undefined if error.  Reuses oldbundle.
     */
    private static UResourceBundle getZoneByName(UResourceBundle top, String id) throws MissingResourceException {
        // load the Rules object
        UResourceBundle tmp = top.get(kNAMES);
        
        // search for the string
        int idx = findInStringArray(tmp, id);
        
        if((idx == -1)) {
            // not found 
            throw new MissingResourceException(kNAMES, ((ICUResourceBundle)tmp).getResPath(), id);
            //ures_close(oldbundle);
            //oldbundle = NULL;
        } else {
            tmp = top.get(kZONES); // get Zones object from top
            tmp = tmp.get(idx); // get nth Zone object
        }
        return tmp;
    }
    private static int findInStringArray(UResourceBundle array, String id){
        int start = 0;
        int limit = array.getSize();
        int mid;
        String u = null;
        int lastMid = Integer.MAX_VALUE;
        if((limit < 1)) { 
            return -1;
        }
        for (;;) {
            mid = (int)((start + limit) / 2);
            if (lastMid == mid) {   /* Have we moved? */
                break;  /* We haven't moved, and it wasn't found. */
            }
            lastMid = mid;
            u = array.getString(mid);
            if(u==null){
                break;
            }
            int r = id.compareTo(u);
            if(r==0) {
                return mid;
            } else if(r<0) {
                limit = mid;
            } else {
                start = mid;
            }
        }
        return -1;
    }
    private static final String kREGIONS  = "Regions";
    private static final String kZONES    = "Zones";
    private static final String kNAMES    = "Names";
    private static final String kGMT_ID   = "GMT";
    private static final String kCUSTOM_TZ_PREFIX = "GMT";
    private static ICUCache zoneCache = new SimpleCache();
    /**
     * The Olson data is stored the "zoneinfo" resource bundle.
     * Sub-resources are organized into three ranges of data: Zones, final
     * rules, and country tables.  There is also a meta-data resource
     * which has 3 integers: The number of zones, rules, and countries,
     * respectively.  The country count includes the non-country 'Default'.
     */
    static int OLSON_ZONE_START = -1; // starting index of zones
    static int OLSON_ZONE_COUNT = 0;  // count of zones

    /**
     * Given a pointer to an open "zoneinfo" resource, load up the Olson
     * meta-data. Return true if successful.
     */
    private static boolean getOlsonMeta(ICUResourceBundle top) {
        if (OLSON_ZONE_START < 0 && top != null) {
            try {
                UResourceBundle res = top.get(kZONES);
                OLSON_ZONE_COUNT = res.getSize();
                OLSON_ZONE_START = 0;
            } catch (MissingResourceException e) {
                // throws away the exception
            }
        }
        return (OLSON_ZONE_START >= 0);
    }

    /**
     * Load up the Olson meta-data. Return true if successful.
     */
    private static boolean getOlsonMeta() {
        if (OLSON_ZONE_START < 0) {
            try {
                ICUResourceBundle top = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                getOlsonMeta(top);
            } catch (MissingResourceException e) {
                // throws away the exception
            }
        }
        return (OLSON_ZONE_START >= 0);
    }

    /**
     * Lookup the given name in our system zone table.  If found,
     * instantiate a new zone of that name and return it.  If not
     * found, return 0.
     */
    public static TimeZone getSystemTimeZone(String id) {
        TimeZone z = (TimeZone)zoneCache.get(id);
        if (z == null) {
            try{
                UResourceBundle top = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
                UResourceBundle res = openOlsonResource(id);
                z = new OlsonTimeZone(top, res);
                z.setID(id);
                zoneCache.put(id, z);
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
        // Create normalized time zone ID - GMT[+|-]hhmm[ss]
        StringBuffer zid = new StringBuffer(kCUSTOM_TZ_PREFIX);
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
            if (min < 10) {
                zid.append('0');
            }
            zid.append(min);

            if (sec != 0) {
                // Optional second field
                if (sec < 10) {
                    zid.append('0');
                }
                zid.append(sec);
            }
        }
        return zid.toString();
    }

    private static SoftReference OLSON_TO_META_REF;
    private static SoftReference META_TO_OLSON_REF;

    static class OlsonToMetaMappingEntry {
        String mzid;
        long from;
        long to;
    }

    private static class MetaToOlsonMappingEntry {
        String id;
        String territory;
    }

    static Map getOlsonToMetaMap() {
        Map olsonToMeta = null;
        synchronized(ZoneMeta.class) {
            if (OLSON_TO_META_REF != null) {
                olsonToMeta = (HashMap)OLSON_TO_META_REF.get();
            }
            if (olsonToMeta == null) {
                olsonToMeta = createOlsonToMetaMap();
                if (olsonToMeta == null) {
                    // We need to return non-null Map to avoid disaster
                    olsonToMeta = new HashMap();
                }
                OLSON_TO_META_REF = new SoftReference(olsonToMeta);
            }
        }
        return olsonToMeta;
    }

    /*
     * Create olson tzid to metazone mappings from metazoneInfo.res (3.8.1 or later)
     */
    private static Map createOlsonToMetaMap() {
        // Create olson id to metazone mapping table
        HashMap olsonToMeta = null;
        UResourceBundle metazoneMappingsBundle = null;
        try {
            UResourceBundle bundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "metazoneInfo");
            metazoneMappingsBundle = bundle.get("metazoneMappings");
        } catch (MissingResourceException mre) {
            // do nothing
        }
        if (metazoneMappingsBundle != null) {
            String[] tzids = getAvailableIDs();
            for (int i = 0; i < tzids.length; i++) {
                // Skip aliases
                String canonicalID = TimeZone.getCanonicalID(tzids[i]);
                if (canonicalID == null || !tzids[i].equals(canonicalID)) {
                    continue;
                }
                String tzkey = tzids[i].replace('/', ':');
                try {
                    UResourceBundle zoneBundle = metazoneMappingsBundle.get(tzkey);
                    LinkedList mzMappings = new LinkedList();
                    for (int idx = 0; ; idx++) {
                        try {
                            UResourceBundle mz = zoneBundle.get("mz" + idx);
                            String[] mzstr = mz.getStringArray();
                            if (mzstr == null || mzstr.length != 3) {
                                continue;
                            }
                            OlsonToMetaMappingEntry mzmap = new OlsonToMetaMappingEntry();
                            mzmap.mzid = mzstr[0].intern();
                            mzmap.from = parseDate(mzstr[1]);
                            mzmap.to = parseDate(mzstr[2]);

                            // Add this mapping to the list
                            mzMappings.add(mzmap);
                        } catch (MissingResourceException nomz) {
                            // we're done
                            break;
                        } catch (IllegalArgumentException baddate) {
                            // skip this
                        }
                    }
                    if (mzMappings.size() != 0) {
                        // Add to the olson-to-meta map
                        if (olsonToMeta == null) {
                            olsonToMeta = new HashMap();
                        }
                        olsonToMeta.put(tzids[i], mzMappings);
                    }
                } catch (MissingResourceException noum) {
                    // Does not use metazone, just skip this.
                }
            }
        }
        return olsonToMeta;
    }

    /**
     * Returns a CLDR metazone ID for the given Olson tzid and time.
     */
    public static String getMetazoneID(String olsonID, long date) {
        String mzid = null;
        Map olsonToMeta = getOlsonToMetaMap();
        List mappings = (List)olsonToMeta.get(olsonID);
        if (mappings == null) {
            // The given ID might be an alias - try its canonical id
            String canonicalID = getCanonicalSystemID(olsonID);
            if (canonicalID != null && !canonicalID.equals(olsonID)) {
                mappings = (List)olsonToMeta.get(canonicalID);
            }
        }
        if (mappings != null) {
            for (int i = 0; i < mappings.size(); i++) {
                OlsonToMetaMappingEntry mzm = (OlsonToMetaMappingEntry)mappings.get(i);
                if (date >= mzm.from && date < mzm.to) {
                    mzid = mzm.mzid;
                    break;
                }
            }
        }
        return mzid;
    }

    private static Map getMetaToOlsonMap() {
        HashMap metaToOlson = null;
        synchronized(ZoneMeta.class) {
            if (META_TO_OLSON_REF != null) {
                metaToOlson = (HashMap)META_TO_OLSON_REF.get();
            }
            if (metaToOlson == null) {
                metaToOlson = new HashMap();
                UResourceBundle metazonesBundle = null;
                try {
                    UResourceBundle supplementalBundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,
                        "supplementalData");
                    UResourceBundle  mapTimezonesBundle = supplementalBundle.get("mapTimezones");
                    metazonesBundle = mapTimezonesBundle.get("metazones");
                } catch (MissingResourceException mre) {
                    // do nothing
                }
                if (metazonesBundle != null) {
                    Enumeration mzenum = metazonesBundle.getKeys();
                    while (mzenum.hasMoreElements()) {
                        String mzkey = (String)mzenum.nextElement();
                        if (!mzkey.startsWith("meta:")) {
                            continue;
                        }
                        String tzid = null;
                        try {
                            tzid = metazonesBundle.getString(mzkey);
                        } catch (MissingResourceException mre) {
                            // It should not happen..
                        }
                        if (tzid != null) {
                            int territoryIdx = mzkey.lastIndexOf('_');
                            if (territoryIdx > 0) {
                                String mzid = mzkey.substring(5 /* "meta:".length() */, territoryIdx);
                                String territory = mzkey.substring(territoryIdx + 1);
                                List mappings = (List)metaToOlson.get(mzid);
                                if (mappings == null) {
                                    mappings = new LinkedList();
                                    metaToOlson.put(mzid, mappings);
                                }
                                MetaToOlsonMappingEntry olsonmap = new MetaToOlsonMappingEntry();
                                olsonmap.id = tzid;
                                olsonmap.territory = territory;
                                mappings.add(olsonmap);
                            }
                        }
                    }
                }
                META_TO_OLSON_REF = new SoftReference(metaToOlson);
            }
        }
        return metaToOlson;
    }

    /**
     * Returns an Olson ID for the ginve metazone and region
     */
    public static String getZoneIdByMetazone(String metazoneID, String region) {
        String tzid = null;
        Map metaToOlson = getMetaToOlsonMap();
        List mappings = (List)metaToOlson.get(metazoneID);
        if (mappings != null) {
            for (int i = 0; i < mappings.size(); i++) {
                MetaToOlsonMappingEntry olsonmap = (MetaToOlsonMappingEntry)mappings.get(i);
                if (olsonmap.territory.equals(region)) {
                    tzid = olsonmap.id;
                    break;
                } else if (olsonmap.territory.equals("001")) {
                    tzid = olsonmap.id;
                }
            }
        }
        return tzid;
    }

//    /**
//     * Returns an Olson ID for the given metazone and locale
//     */
//    public static String getZoneIdByMetazone(String metazoneID, ULocale loc) {
//        String region = loc.getCountry();
//        if (region.length() == 0) {
//            // Get likely region
//            ULocale tmp = ULocale.addLikelySubtag(loc);
//            region = tmp.getCountry();
//        }
//        return getZoneIdByMetazone(metazoneID, region);
//    }

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
}
