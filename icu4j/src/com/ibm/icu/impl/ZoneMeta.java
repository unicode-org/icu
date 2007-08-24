/*
**********************************************************************
* Copyright (c) 2003-2007 International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: September 4 2003
* Since: ICU 2.8
**********************************************************************
*/
package com.ibm.icu.impl;

import java.text.ParsePosition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.Vector;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
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
        int zone = -1;
        int size = res.getSize();
        if (size == 4 || size == 6) {
            UResourceBundle r = res.get(size-1);
            int[] v = r.getIntVector();
            if (index >= 0 && index < v.length && getOlsonMeta()) {
                zone = v[index];
            }
        }
        if (zone >= 0) {
            UResourceBundle top = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle ares = top.get(kNAMES); // dereference Zones section
            result = ares.getString(zone);

        }
        return result;
    }

    private static String[] getCanonicalInfo(String id) {
        if (canonicalMap == null) {
            Map m = new HashMap();
            Set s = new HashSet();
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
        if (info != null && info[1] != null && !multiZoneTerritories.contains(info[1])) {
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
//
// TODO: There is a design bug in UResourceBundle and getLoadingStatus() does not work well.
//
//            if (rb.getLoadingStatus() != ICUResourceBundle.FROM_ROOT && rb.getLoadingStatus() != ICUResourceBundle.FROM_DEFAULT) {
//                country = ULocale.getDisplayCountry("xx_" + country_code, locale);
//            }
//            if (country == null || country.length() == 0) country = country_code;
// START WORKAROUND
            ULocale rbloc = rb.getULocale();
            if (!rbloc.equals(ULocale.ROOT) && rbloc.getLanguage().equals(locale.getLanguage())) {
                country = ULocale.getDisplayCountry("xx_" + country_code, locale);
            }
            if (country == null || country.length() == 0) {
                country = country_code;
            }
// END WORKAROUND
        }
        
        // This is not behavior specified in tr35, but behavior added by Mark.  
        // TR35 says to display the country _only_ if there is a localization.
        if (getSingleCountry(tzid) != null) { // single country
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
        //TODO: revisit after 3.8

        String msgpat = "GMT{0}"; // Parser code is not quite ready to accept localized GMT string as of 3.8
        //String msgpat = getTZLocalizationInfo(locale, GMT);
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

        //final long mph = 3600000;
        //final long mpm = 60000;

        SimpleDateFormat sdf = new SimpleDateFormat(dtepat, locale);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String res = sdf.format(new Long(value));
        MessageFormat mf = new MessageFormat(msgpat);
        res = mf.format(new Object[] { res });
        return res;
    }

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
        ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance(locale);
        return bundle.getStringWithFallback(ZONE_STRINGS+FORWARD_SLASH+format);
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
        if(!getOlsonMeta()){
            return null;
        }
        ICUResourceBundle top = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle res = getZoneByName(top, id);
        // Dereference if this is an alias.  Docs say result should be 1
        // but it is 0 in 2.8 (?).
         if (res.getSize() <= 1 && getOlsonMeta(top)) {
            int deref = res.getInt() + 0;
            UResourceBundle ares = top.get(kZONES); // dereference Zones section
            res = (ICUResourceBundle) ares.get(deref);
        } 
        return res;
    }
    /**
     * Fetch a specific zone by name.  Replaces the getByKey call. 
     * @param top Top timezone resource
     * @param id Time zone ID
     * @return the zone's bundle if found, or undefined if error.  Reuses oldbundle.
     */
    private static UResourceBundle getZoneByName(UResourceBundle top, String id) {
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
    private static final String kRFC_TZ_PREFIX = "GMT";
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
        if (OLSON_ZONE_START < 0) {
            UResourceBundle res = top.get(kZONES);
            OLSON_ZONE_COUNT = res.getSize();
            OLSON_ZONE_START = 0;
        }
        return (OLSON_ZONE_START >= 0);
    }

    /**
     * Load up the Olson meta-data. Return true if successful.
     */
    private static boolean getOlsonMeta() {
        ICUResourceBundle top = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "zoneinfo", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        if(OLSON_ZONE_START < 0) {
            getOlsonMeta(top);
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
    /**
     * Parse a custom time zone identifier and return a corresponding zone.
     * @param id a string of the form GMT[+-]hh:mm, GMT[+-]hhmm, or
     * GMT[+-]hh.
     * @return a newly created SimpleTimeZone with the given offset and
     * no Daylight Savings Time, or null if the id cannot be parsed.
    */
    public static TimeZone getCustomTimeZone(String id){

        NumberFormat numberFormat = null;
        String idUppercase = id.toUpperCase();

        if (id.length() > kGMT_ID.length() &&
            idUppercase.startsWith(kGMT_ID)) {
            ParsePosition pos = new ParsePosition(kGMT_ID.length());
            boolean negative = false;
            int hour = 0;
            int min = 0;

            if (id.charAt(pos.getIndex()) == 0x002D /*'-'*/) {
                negative = true;
            } else if (id.charAt(pos.getIndex()) != 0x002B /*'+'*/) {
                return null;
            }
            pos.setIndex(pos.getIndex() + 1);

            numberFormat = NumberFormat.getInstance();
            numberFormat.setParseIntegerOnly(true);

            // Look for either hh:mm, hhmm, or hh
            int start = pos.getIndex();

            Number n = numberFormat.parse(id, pos);
            if (pos.getIndex() == start) {
                return null;
            }
            hour = n.intValue();

            if (pos.getIndex() < id.length() &&
                id.charAt(pos.getIndex()) == 0x003A /*':'*/) {
                // hh:mm
                pos.setIndex(pos.getIndex() + 1);
                int oldPos = pos.getIndex();
                n = numberFormat.parse(id, pos);
                if (pos.getIndex() == oldPos) {
                    return null;
                }
                min = n.intValue();
            } else {
                // hhmm or hh

                // Be strict about interpreting something as hh; it must be
                // an offset < 23, and it must be one or two digits. Thus
                // 0010 is interpreted as 00:10, but 10 is interpreted as
                // 10:00.
                if (hour > kMAX_CUSTOM_HOUR || (pos.getIndex() - start) > 2) {
                    min = hour % 100;
                    hour /= 100;
                }
            }

            if (hour > kMAX_CUSTOM_HOUR || min > kMAX_CUSTOM_MIN) {
                return null;
            }

            // Create time zone ID in RFC822 format - GMT[+|-]hhmm
            StringBuffer zid = new StringBuffer(kRFC_TZ_PREFIX);
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
            }

            int offset = (hour * 60 + min) * 60 * 1000;
            if(negative) {
                offset = -offset;
            }
            TimeZone z = new SimpleTimeZone(offset, zid.toString());
            return z;
        }
        return null;
    }
}
