/*
 *******************************************************************************
 * Copyright (C) 2007-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * @author yoshito
 *
 */
public class ZoneStringFormat {
    /**
     * Constructs a ZoneStringFormat by zone strings array.
     * The internal structure of zoneStrings is compatible with
     * the one used by getZoneStrings/setZoneStrings in DateFormatSymbols.
     * 
     * @param zoneStrings zone strings
     */
    public ZoneStringFormat(String[][] zoneStrings) {
        tzidToStrings = new HashMap();
        zoneStringsTrie = new TextTrieMap(true);
        for (int i = 0; i < zoneStrings.length; i++) {
            String tzid = zoneStrings[i][0];
            String[] names = new String[ZSIDX_MAX];
            for (int j = 1; j < zoneStrings[i].length; j++) {
                if (zoneStrings[i][j] != null) {
                    int typeIdx = getNameTypeIndex(j);
                    if (typeIdx != -1) {
                        names[typeIdx] = zoneStrings[i][j];

                        // Put the name into the trie
                        int type = getNameType(typeIdx);
                        ZoneStringInfo zsinfo = new ZoneStringInfo(tzid, zoneStrings[i][j], type);
                        zoneStringsTrie.put(zoneStrings[i][j], zsinfo);
                    }
                    
                }
            }
            ZoneStrings zstrings = new ZoneStrings(names, true, null);
            tzidToStrings.put(tzid, zstrings);
        }
    }

    /**
     * Gets an instance of ZoneStringFormat for the specified locale
     * @param locale the locale
     * @return An instance of ZoneStringFormat for the locale
     */
    public static ZoneStringFormat getInstance(ULocale locale) {
        ZoneStringFormat tzf = (ZoneStringFormat)TZFORMAT_CACHE.get(locale);
        if (tzf == null) {
            tzf = new ZoneStringFormat(locale);
            TZFORMAT_CACHE.put(locale, tzf);
        }
        return tzf;
    }

    public String[][] getZoneStrings() {
        return getZoneStrings(System.currentTimeMillis());
    }

    // APIs used by SimpleDateFormat to get a zone string
    public String getSpecificLongString(Calendar cal) {
        if (cal.get(Calendar.DST_OFFSET) == 0) {
            return getString(cal.getTimeZone().getID(), ZSIDX_LONG_STANDARD, cal.getTimeInMillis(), false /* not used */);
        }
        return getString(cal.getTimeZone().getID(), ZSIDX_LONG_DAYLIGHT, cal.getTimeInMillis(), false /* not used */);
    }

    public String getSpecificShortString(Calendar cal, boolean commonlyUsedOnly) {
        if (cal.get(Calendar.DST_OFFSET) == 0) {
            return getString(cal.getTimeZone().getID(), ZSIDX_SHORT_STANDARD, cal.getTimeInMillis(), commonlyUsedOnly);
        }
        return getString(cal.getTimeZone().getID(), ZSIDX_SHORT_DAYLIGHT, cal.getTimeInMillis(), commonlyUsedOnly);
    }

    public String getGenericLongString(Calendar cal) {
        return getGenericString(cal, false /* long */, false /* not used */);
    }

    public String getGenericShortString(Calendar cal, boolean commonlyUsedOnly) {
        return getGenericString(cal, true /* long */, commonlyUsedOnly);
    }

    public String getGenericLocationString(Calendar cal) {
        return getString(cal.getTimeZone().getID(), ZSIDX_LOCATION, cal.getTimeInMillis(), false /* not used */);
    }

    // APIs used by SimpleDateFormat to lookup a zone string
    public static class ZoneStringInfo {
        private String id;
        private String str;
        private int type;

        private ZoneStringInfo(String id, String str, int type) {
            this.id = id;
            this.str = str;
            this.type = type;
        }

        public String getID() {
            return id;
        }

        public String getString() {
            return str;
        }

        public boolean isStandard() {
            if ((type & STANDARD_LONG) != 0 || (type & STANDARD_SHORT) != 0) {
                return true;
            }
            return false;
        }

        public boolean isDaylight() {
            if ((type & DAYLIGHT_LONG) != 0 || (type & DAYLIGHT_SHORT) != 0) {
                return true;
            }
            return false;
        }

        public boolean isGeneric() {
            return !isStandard() && !isDaylight();
        }

        private int getType() {
            return type;
        }
    }

    public ZoneStringInfo findSpecificLong(String text, int start) {
        return find(text, start, STANDARD_LONG | DAYLIGHT_LONG);
    }
    
    public ZoneStringInfo findSpecificShort(String text, int start) {
        return find(text, start, STANDARD_SHORT | DAYLIGHT_SHORT);
    }

    public ZoneStringInfo findGenericLong(String text, int start) {
        return find(text, start, GENERIC_LONG | STANDARD_LONG | LOCATION);
    }
    
    public ZoneStringInfo findGenericShort(String text, int start) {
        return find(text, start, GENERIC_SHORT | STANDARD_SHORT | LOCATION);
    }

    public ZoneStringInfo findGenericLocation(String text, int start) {
        return find(text, start, LOCATION);
    }

    // Following APIs are not used by SimpleDateFormat, but public for testing purpose
    public String getLongStandard(String tzid, long date) {
        return getString(tzid, ZSIDX_LONG_STANDARD, date, false /* not used */);
    }

    public String getLongDaylight(String tzid, long date) {
        return getString(tzid, ZSIDX_LONG_DAYLIGHT, date, false /* not used */);
    }

    public String getLongGenericNonLocation(String tzid, long date) {
        return getString(tzid, ZSIDX_LONG_GENERIC, date, false /* not used */);
    }

    public String getLongGenericPartialLocation(String tzid, long date) {
        return getGenericPartialLocationString(tzid, false, date, false /* not used */);
    }

    public String getShortStandard(String tzid, long date, boolean commonlyUsedOnly) {
        return getString(tzid, ZSIDX_SHORT_STANDARD, date, commonlyUsedOnly);
    }

    public String getShortDaylight(String tzid, long date, boolean commonlyUsedOnly) {
        return getString(tzid, ZSIDX_SHORT_DAYLIGHT, date, commonlyUsedOnly);
    }

    public String getShortGenericNonLocation(String tzid, long date, boolean commonlyUsedOnly) {
        return getString(tzid, ZSIDX_SHORT_GENERIC, date, commonlyUsedOnly);
    }

    public String getShortGenericPartialLocation(String tzid, long date, boolean commonlyUsedOnly) {
        return getGenericPartialLocationString(tzid, true, date, commonlyUsedOnly);
    }

    public String getGenericLocation(String tzid) {
        return getString(tzid, ZSIDX_LOCATION, 0L /* not used */, false /* not used */);
    }
    
    /**
     * Constructs a ZoneStringFormat by locale.  Because an instance of ZoneStringFormat
     * is read-only, only one instance for a locale is sufficient.  Thus, this
     * constructor is protected and only called from getInstance(ULocale) to
     * create one for a locale.
     * @param locale The locale
     */
    protected ZoneStringFormat(ULocale locale) {
        this.locale = locale;
        tzidToStrings = new HashMap();
        mzidToStrings = new HashMap();
        zoneStringsTrie = new TextTrieMap(true);

        ICUResourceBundle zoneStringsBundle = null;
        try {
            ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
            zoneStringsBundle = bundle.getWithFallback("zoneStrings");
        } catch (MissingResourceException e) {
            // If no locale bundles are available, zoneStringsBundle will be null.
            // We still want to go through the rest of zone strings initialization,
            // because generic location format is generated from tzid for the case.
            // The rest of code should work even zoneStrings is null.
        }

        String[] zoneIDs = TimeZone.getAvailableIDs();
        MessageFormat fallbackFmt = getFallbackFormat(locale);
        MessageFormat regionFmt = getRegionFormat(locale);

        String[] zstrarray = new String[ZSIDX_MAX];
        String[] mzstrarray = new String[ZSIDX_MAX];
        String[][] mzPartialLoc = new String[10][4]; // maximum 10 metazones per zone

        for (int i = 0; i < zoneIDs.length; i++) {
            // Skip aliases
            String tzid = ZoneMeta.getCanonicalSystemID(zoneIDs[i]);
            if (tzid == null || !zoneIDs[i].equals(tzid)) {
                continue;
            }

            String zoneKey = tzid.replace('/', ':');
            zstrarray[ZSIDX_LONG_STANDARD] = getZoneStringFromBundle(zoneStringsBundle, zoneKey, RESKEY_LONG_STANDARD);
            zstrarray[ZSIDX_SHORT_STANDARD] = getZoneStringFromBundle(zoneStringsBundle, zoneKey, RESKEY_SHORT_STANDARD);
            zstrarray[ZSIDX_LONG_DAYLIGHT] = getZoneStringFromBundle(zoneStringsBundle, zoneKey, RESKEY_LONG_DAYLIGHT);
            zstrarray[ZSIDX_SHORT_DAYLIGHT] = getZoneStringFromBundle(zoneStringsBundle, zoneKey, RESKEY_SHORT_DAYLIGHT);
            zstrarray[ZSIDX_LONG_GENERIC] = getZoneStringFromBundle(zoneStringsBundle, zoneKey, RESKEY_LONG_GENERIC);
            zstrarray[ZSIDX_SHORT_GENERIC] = getZoneStringFromBundle(zoneStringsBundle, zoneKey, RESKEY_SHORT_GENERIC);

            // Compose location format string
            String countryCode = ZoneMeta.getCanonicalCountry(tzid);
            String country = null;
            String city = null;
            if (countryCode != null) {
                city = getZoneStringFromBundle(zoneStringsBundle, zoneKey, RESKEY_EXEMPLAR_CITY);
                if (city == null) {
                    city = tzid.substring(tzid.lastIndexOf('/') + 1).replace('_', ' ');
                }
                country = getLocalizedCountry(countryCode, locale);
                if (ZoneMeta.getSingleCountry(tzid) != null) {
                    // If the zone is only one zone in the country, do not add city
                    zstrarray[ZSIDX_LOCATION] = regionFmt.format(new Object[] {country});
                } else {
                    zstrarray[ZSIDX_LOCATION] = fallbackFmt.format(new Object[] {city, country});
                }
            } else {
                if (tzid.startsWith("Etc/")) {
                    // "Etc/xxx" is not associated with a location, so localized GMT format
                    // is always used as generic location format.
                    zstrarray[ZSIDX_LOCATION] = null;
                } else {
                    // When a new time zone ID, which is actually associated with a region,
                    // is added in tzdata, but the current CLDR data does not have the
                    // information yet, ICU creates a generic location string based on 
                    // the ID.  This implementation supports canonical time zone round trip
                    // with format pattern "VVVV".  See #6602 for the details.
                    String location = tzid;
                    int slashIdx = location.lastIndexOf('/');
                    if (slashIdx != -1) {
                        location = tzid.substring(slashIdx + 1);
                    }
                    zstrarray[ZSIDX_LOCATION] = regionFmt.format(new Object[] {location});
                }
            }

            boolean commonlyUsed = isCommonlyUsed(zoneStringsBundle, zoneKey);
            
            // Resolve metazones used by this zone
            int mzPartialLocIdx = 0;
            Map olsonToMeta = ZoneMeta.getOlsonToMetaMap();
            List metazoneMappings = (List)olsonToMeta.get(tzid);
            if (metazoneMappings != null) {
                Iterator it = metazoneMappings.iterator();
                while (it.hasNext()) {
                    ZoneMeta.OlsonToMetaMappingEntry mzmap = (ZoneMeta.OlsonToMetaMappingEntry)it.next();
                    ZoneStrings mzStrings = (ZoneStrings)mzidToStrings.get(mzmap.mzid);
                    if (mzStrings == null) {
                        // If the metazone strings are not yet processed, do it now.
                        String mzkey = "meta:" + mzmap.mzid;
                        boolean mzCommonlyUsed = isCommonlyUsed(zoneStringsBundle, mzkey);
                        mzstrarray[ZSIDX_LONG_STANDARD] = getZoneStringFromBundle(zoneStringsBundle, mzkey, RESKEY_LONG_STANDARD);
                        mzstrarray[ZSIDX_SHORT_STANDARD] = getZoneStringFromBundle(zoneStringsBundle, mzkey, RESKEY_SHORT_STANDARD);
                        mzstrarray[ZSIDX_LONG_DAYLIGHT] = getZoneStringFromBundle(zoneStringsBundle, mzkey, RESKEY_LONG_DAYLIGHT);
                        mzstrarray[ZSIDX_SHORT_DAYLIGHT] = getZoneStringFromBundle(zoneStringsBundle, mzkey, RESKEY_SHORT_DAYLIGHT);
                        mzstrarray[ZSIDX_LONG_GENERIC] = getZoneStringFromBundle(zoneStringsBundle, mzkey, RESKEY_LONG_GENERIC);
                        mzstrarray[ZSIDX_SHORT_GENERIC] = getZoneStringFromBundle(zoneStringsBundle, mzkey, RESKEY_SHORT_GENERIC);
                        mzstrarray[ZSIDX_LOCATION] = null;
                        mzStrings = new ZoneStrings(mzstrarray, mzCommonlyUsed, null);
                        mzidToStrings.put(mzmap.mzid, mzStrings);

                        // Add metazone strings to the zone string trie
                        String preferredIdForLocale = ZoneMeta.getZoneIdByMetazone(mzmap.mzid, getRegion());
                        for (int j = 0; j < mzstrarray.length; j++) {
                            if (mzstrarray[j] != null) {
                                int type = getNameType(j);
                                ZoneStringInfo zsinfo = new ZoneStringInfo(preferredIdForLocale, mzstrarray[j], type);
                                zoneStringsTrie.put(mzstrarray[j], zsinfo);
                            }
                        }
                    }
                    // Compose generic partial location format
                    String lg = mzStrings.getString(ZSIDX_LONG_GENERIC);
                    String sg = mzStrings.getString(ZSIDX_SHORT_GENERIC);
                    if (lg != null || sg != null) {
                        boolean addMzPartialLocationNames = true;
                        for (int j = 0; j < mzPartialLocIdx; j++) {
                            if (mzPartialLoc[j][0].equals(mzmap.mzid)) {
                                // already added
                                addMzPartialLocationNames = false;
                                break;
                            }
                        }
                        if (addMzPartialLocationNames) {
                            String locationPart = null;
                            // Check if the zone is the preferred zone for the territory associated with the zone
                            String preferredID = ZoneMeta.getZoneIdByMetazone(mzmap.mzid, countryCode);
                            if (tzid.equals(preferredID)) {
                                // Use country for the location
                                locationPart = country;
                            } else {
                                // Use city for the location
                                locationPart = city;
                            }
                            mzPartialLoc[mzPartialLocIdx][0] = mzmap.mzid;
                            mzPartialLoc[mzPartialLocIdx][1] = null;
                            mzPartialLoc[mzPartialLocIdx][2] = null;
                            mzPartialLoc[mzPartialLocIdx][3] = null;
                            if (locationPart != null) {
                                if (lg != null) {
                                    mzPartialLoc[mzPartialLocIdx][1] = fallbackFmt.format(new Object[] {locationPart, lg});
                                }
                                if (sg != null) {
                                    mzPartialLoc[mzPartialLocIdx][2] = fallbackFmt.format(new Object[] {locationPart, sg});
                                    boolean shortMzCommonlyUsed = mzStrings.isShortFormatCommonlyUsed();
                                    if (shortMzCommonlyUsed) {
                                        mzPartialLoc[mzPartialLocIdx][3] = "1";
                                    }
                                }
                            }
                            mzPartialLocIdx++;
                        }
                    }
                }
            }
            String[][] genericPartialLocationNames = null;
            if (mzPartialLocIdx != 0) {
                // metazone generic partial location names are collected
                genericPartialLocationNames = new String[mzPartialLocIdx][];
                for (int mzi = 0; mzi < mzPartialLocIdx; mzi++) {
                    genericPartialLocationNames[mzi] = (String[])mzPartialLoc[mzi].clone();
                }
            }
            // Finally, create ZoneStrings instance and put it into the tzidToStinrgs map
            ZoneStrings zstrings = new ZoneStrings(zstrarray, commonlyUsed, genericPartialLocationNames);
            tzidToStrings.put(tzid, zstrings);

            // Also add all available names to the zone string trie
            if (zstrarray != null) {
                for (int j = 0; j < zstrarray.length; j++) {
                    if (zstrarray[j] != null) {
                        int type = getNameType(j);
                        ZoneStringInfo zsinfo = new ZoneStringInfo(tzid, zstrarray[j], type);
                        zoneStringsTrie.put(zstrarray[j], zsinfo);
                    }
                }
            }
            if (genericPartialLocationNames != null) {
                for (int j = 0; j < genericPartialLocationNames.length; j++) {
                    ZoneStringInfo zsinfo;
                    if (genericPartialLocationNames[j][1] != null) {
                        zsinfo = new ZoneStringInfo(tzid, genericPartialLocationNames[j][1], GENERIC_LONG);
                        zoneStringsTrie.put(genericPartialLocationNames[j][1], zsinfo);
                    }
                    if (genericPartialLocationNames[j][2] != null) {
                        zsinfo = new ZoneStringInfo(tzid, genericPartialLocationNames[j][1], GENERIC_SHORT);
                        zoneStringsTrie.put(genericPartialLocationNames[j][2], zsinfo);
                    }
                }
            }
        }
    }

    // Name types, these bit flag are used for zone string lookup
    private static final int LOCATION = 0x0001;
    private static final int GENERIC_LONG = 0x0002;
    private static final int GENERIC_SHORT = 0x0004;
    private static final int STANDARD_LONG = 0x0008;
    private static final int STANDARD_SHORT = 0x0010;
    private static final int DAYLIGHT_LONG = 0x0020;
    private static final int DAYLIGHT_SHORT = 0x0040;
    
    // Name type index, these constants are used for index in ZoneStrings.strings
    private static final int ZSIDX_LOCATION = 0;
    private static final int ZSIDX_LONG_STANDARD = 1;
    private static final int ZSIDX_SHORT_STANDARD = 2;
    private static final int ZSIDX_LONG_DAYLIGHT = 3;
    private static final int ZSIDX_SHORT_DAYLIGHT = 4;
    private static final int ZSIDX_LONG_GENERIC = 5;
    private static final int ZSIDX_SHORT_GENERIC = 6;

    private static final int ZSIDX_MAX = ZSIDX_SHORT_GENERIC + 1;

    // ZoneStringFormat cache
    private static ICUCache TZFORMAT_CACHE = new SimpleCache();

    /*
     * The translation type of the translated zone strings
     */
    private static final String
         RESKEY_SHORT_GENERIC  = "sg",
         RESKEY_SHORT_STANDARD = "ss",
         RESKEY_SHORT_DAYLIGHT = "sd",
         RESKEY_LONG_GENERIC   = "lg",
         RESKEY_LONG_STANDARD  = "ls",
         RESKEY_LONG_DAYLIGHT  = "ld",
         RESKEY_EXEMPLAR_CITY  = "ec",
         RESKEY_COMMONLY_USED  = "cu";

    // Window size used for DST check for a zone in a metazone
    private static final long DST_CHECK_RANGE = 184L*(24*60*60*1000);

    // Map from zone id to ZoneStrings
    private Map tzidToStrings;

    // Map from metazone id to ZoneStrings
    private Map mzidToStrings;

    // Zone string dictionary, used for look up
    private TextTrieMap zoneStringsTrie;

    // Locale used for initializing zone strings
    private ULocale locale;

    // Region used for resolving a zone in a metazone, initialized by locale
    private transient String region;
    
    /*
     * Private method to get a zone string except generic partial location types.
     */
    private String getString(String tzid, int typeIdx, long date, boolean commonlyUsedOnly) {
        String result = null;
        ZoneStrings zstrings = (ZoneStrings)tzidToStrings.get(tzid);
        if (zstrings == null) {
            // ICU's own array does not have entries for aliases
            String canonicalID = ZoneMeta.getCanonicalSystemID(tzid);
            if (canonicalID != null && !canonicalID.equals(tzid)) {
                // Canonicalize tzid here.  The rest of operations
                // require tzid to be canonicalized.
                tzid = canonicalID;
                zstrings = (ZoneStrings)tzidToStrings.get(tzid);
            }
        }
        if (zstrings != null) {
            switch (typeIdx) {
            case ZSIDX_LONG_STANDARD:
            case ZSIDX_LONG_DAYLIGHT:
            case ZSIDX_LONG_GENERIC:
            case ZSIDX_LOCATION:
                result = zstrings.getString(typeIdx);
                break;
            case ZSIDX_SHORT_STANDARD:
            case ZSIDX_SHORT_DAYLIGHT:
            case ZSIDX_SHORT_GENERIC:
                if (!commonlyUsedOnly || zstrings.isShortFormatCommonlyUsed()) {
                    result = zstrings.getString(typeIdx);
                }
                break;
            }
        }
        if (result == null && mzidToStrings != null && typeIdx != ZSIDX_LOCATION) {
            // Try metazone
            String mzid = ZoneMeta.getMetazoneID(tzid, date);
            if (mzid != null) {
                ZoneStrings mzstrings = (ZoneStrings)mzidToStrings.get(mzid);
                if (mzstrings != null) {
                    switch (typeIdx) {
                    case ZSIDX_LONG_STANDARD:
                    case ZSIDX_LONG_DAYLIGHT:
                    case ZSIDX_LONG_GENERIC:
                        result = mzstrings.getString(typeIdx);
                        break;
                    case ZSIDX_SHORT_STANDARD:
                    case ZSIDX_SHORT_DAYLIGHT:
                    case ZSIDX_SHORT_GENERIC:
                        if (!commonlyUsedOnly || mzstrings.isShortFormatCommonlyUsed()) {
                            result = mzstrings.getString(typeIdx);
                        }
                        break;
                    }
                }
            }
        }
        return result;
    }

    /*
     * Private method to get a generic string, with fallback logic involved,
     * that is,
     * 
     * 1. If a generic non-location string is avaiable for the zone, return it.
     * 2. If a generic non-location string is associated with a metazone and 
     *    the zone never use daylight time around the given date, use the standard
     *    string (if available).
     *    
     *    Note: In CLDR1.5.1, the same localization is used for generic and standard.
     *    In this case, we do not use the standard string and do the rest.
     *    
     * 3. If a generic non-location string is associated with a metazone and
     *    the offset at the given time is different from the preferred zone for the
     *    current locale, then return the generic partial location string (if avaiable)
     * 4. If a generic non-location string is not available, use generic location
     *    string.
     */
    private String getGenericString(Calendar cal, boolean isShort, boolean commonlyUsedOnly) {
        String result = null;
        TimeZone tz = cal.getTimeZone();
        String tzid = tz.getID();
        ZoneStrings zstrings = (ZoneStrings)tzidToStrings.get(tzid);
        if (zstrings == null) {
            // ICU's own array does not have entries for aliases
            String canonicalID = ZoneMeta.getCanonicalSystemID(tzid);
            if (canonicalID != null && !canonicalID.equals(tzid)) {
                // Canonicalize tzid here.  The rest of operations
                // require tzid to be canonicalized.
                tzid = canonicalID;
                zstrings = (ZoneStrings)tzidToStrings.get(tzid);
            }
        }
        if (zstrings != null) {
            if (isShort) {
                if (!commonlyUsedOnly || zstrings.isShortFormatCommonlyUsed()) {
                    result = zstrings.getString(ZSIDX_SHORT_GENERIC);
                }
            } else {
                result = zstrings.getString(ZSIDX_LONG_GENERIC);
            }
        }
        if (result == null && mzidToStrings != null) {
            // try metazone
            long time = cal.getTimeInMillis();
            String mzid = ZoneMeta.getMetazoneID(tzid, time);
            if (mzid != null) {
                boolean useStandard = false;
                if (cal.get(Calendar.DST_OFFSET) == 0) {
                    useStandard = true;
                    // Check if the zone actually uses daylight saving time around the time
                    if (tz instanceof BasicTimeZone) {
                        BasicTimeZone btz = (BasicTimeZone)tz;
                        TimeZoneTransition before = btz.getPreviousTransition(time, true);
                        if (before != null
                                && (time - before.getTime() < DST_CHECK_RANGE)
                                && before.getFrom().getDSTSavings() != 0) {
                            useStandard = false;
                        } else {
                            TimeZoneTransition after = btz.getNextTransition(time, false);
                            if (after != null
                                    && (after.getTime() - time < DST_CHECK_RANGE)
                                    && after.getTo().getDSTSavings() != 0) {
                                useStandard = false;
                            }
                        }
                    } else {
                        // If not BasicTimeZone... only if the instance is not an ICU's implementation.
                        // We may get a wrong answer in edge case, but it should practically work OK.
                        int[] offsets = new int[2];
                        tz.getOffset(time - DST_CHECK_RANGE, false, offsets);
                        if (offsets[1] != 0) {
                            useStandard = false;
                        } else {
                            tz.getOffset(time + DST_CHECK_RANGE, false, offsets);
                            if (offsets[1] != 0){
                                useStandard = false;
                            }
                        }
                    }
                }
                if (useStandard) {
                    result = getString(tzid, (isShort ? ZSIDX_SHORT_STANDARD : ZSIDX_LONG_STANDARD),
                            time, commonlyUsedOnly);

                    // Note:
                    // In CLDR 1.5.1, a same localization is used for both generic and standard
                    // for some metazones in some locales.  This is actually data bugs and should
                    // be resolved in later versions of CLDR.  For now, we check if the standard
                    // name is different from its generic name below.
                    if (result != null) {
                        String genericNonLocation = getString(tzid, (isShort ? ZSIDX_SHORT_GENERIC : ZSIDX_LONG_GENERIC),
                                time, commonlyUsedOnly);
                        if (genericNonLocation != null && result.equalsIgnoreCase(genericNonLocation)) {
                            result = null;
                        }
                    }
                }
                if (result == null){
                    ZoneStrings mzstrings = (ZoneStrings)mzidToStrings.get(mzid);
                    if (mzstrings != null) {
                        if (isShort) {
                            if (!commonlyUsedOnly || mzstrings.isShortFormatCommonlyUsed()) {
                                result = mzstrings.getString(ZSIDX_SHORT_GENERIC);
                            }
                        } else {
                            result = mzstrings.getString(ZSIDX_LONG_GENERIC);
                        }
                    }
                    if (result != null) {
                        // Check if the offsets at the given time matches the preferred zone's offsets
                        String preferredId = ZoneMeta.getZoneIdByMetazone(mzid, getRegion());
                        if (!tzid.equals(preferredId)) {
                            // Check if the offsets at the given time are identical with the preferred zone
                            int raw = cal.get(Calendar.ZONE_OFFSET);
                            int sav = cal.get(Calendar.DST_OFFSET);
                            TimeZone preferredZone = TimeZone.getTimeZone(preferredId);
                            int[] preferredOffsets = new int[2];
                            // Check offset in preferred time zone with wall time.
                            // With getOffset(time, false, preferredOffsets),
                            // you may get incorrect results because of time overlap at DST->STD
                            // transition.
                            preferredZone.getOffset(time + raw + sav, true, preferredOffsets);
                            if (raw != preferredOffsets[0] || sav != preferredOffsets[1]) {
                                // Use generic partial location string as fallback
                                result = zstrings.getGenericPartialLocationString(mzid, isShort, commonlyUsedOnly);
                            }
                        }
                    }
                }
            }
        }
        if (result == null) {
            // Use location format as the final fallback
            result = getString(tzid, ZSIDX_LOCATION, cal.getTimeInMillis(), false /* not used */);
        }
        return result;
    }
    
    /*
     * Private method to get a generic partial location string
     */
    private String getGenericPartialLocationString(String tzid, boolean isShort, long date, boolean commonlyUsedOnly) {
        String result = null;
        String mzid = ZoneMeta.getMetazoneID(tzid, date);
        if (mzid != null) {
            ZoneStrings zstrings = (ZoneStrings)tzidToStrings.get(tzid);
            if (zstrings != null) {
                result = zstrings.getGenericPartialLocationString(mzid, isShort, commonlyUsedOnly);
            }
        }
        return result;
    }

    /*
     * Gets zoneStrings compatible with DateFormatSymbols for the
     * specified date.  In CLDR 1.5, zone names can be changed
     * time to time.  This method generates flat 2-dimensional
     * String array including zone ids and its localized strings
     * at the moment.  Thus, even you construct a new ZoneStringFormat
     * by the zone strings array returned by this method, you will
     * loose historic name changes.  Also, commonly used flag for
     * short types is not reflected in the result.
     */
    private String[][] getZoneStrings(long date) {
        Set tzids = tzidToStrings.keySet();
        String[][] zoneStrings = new String[tzids.size()][8];
        int idx = 0;
        Iterator it = tzids.iterator();
        while (it.hasNext()) {
            String tzid = (String)it.next();
            zoneStrings[idx][0] = tzid;
            zoneStrings[idx][1] = getLongStandard(tzid, date);
            zoneStrings[idx][2] = getShortStandard(tzid, date, false);
            zoneStrings[idx][3] = getLongDaylight(tzid, date);
            zoneStrings[idx][4] = getShortDaylight(tzid, date, false);
            zoneStrings[idx][5] = getGenericLocation(tzid);
            zoneStrings[idx][6] = getLongGenericNonLocation(tzid, date);
            zoneStrings[idx][7] = getShortGenericNonLocation(tzid, date, false);
            idx++;
        }
        return zoneStrings;
    }
    
    /*
     * ZoneStrings is an internal implementation class for
     * holding localized name information for a zone/metazone
     */
    private static class ZoneStrings {
        private String[] strings;
        private String[][] genericPartialLocationStrings;
        private boolean commonlyUsed;
 
        private ZoneStrings(String[] zstrarray, boolean commonlyUsed, String[][] genericPartialLocationStrings) {
            if (zstrarray != null) {
                int lastIdx = -1;
                for (int i = 0; i < zstrarray.length; i++) {
                    if (zstrarray[i] != null) {
                        lastIdx = i;
                    }
                }
                if (lastIdx != -1) {
                    strings = new String[lastIdx + 1];
                    System.arraycopy(zstrarray, 0, strings, 0, lastIdx + 1);
                }
            }
            this.commonlyUsed = commonlyUsed;
            this.genericPartialLocationStrings = genericPartialLocationStrings;
        }

        private String getString(int typeIdx) {
            if (strings != null && typeIdx >= 0 && typeIdx < strings.length) {
                return strings[typeIdx];
            }
            return null;
        }

        private boolean isShortFormatCommonlyUsed() {
            return commonlyUsed;
        }

        private String getGenericPartialLocationString(String mzid, boolean isShort, boolean commonlyUsedOnly) {
            String result = null;
            if (genericPartialLocationStrings != null) {
                for (int i = 0; i < genericPartialLocationStrings.length; i++) {
                    if (genericPartialLocationStrings[i][0].equals(mzid)) {
                        if (isShort) {
                            if (!commonlyUsedOnly || genericPartialLocationStrings[i][3] != null) {
                                result = genericPartialLocationStrings[i][2];
                            }
                        } else {
                            result = genericPartialLocationStrings[i][1];
                        }
                        break;
                    }
                }
            }
            return result;
        }
    }

    /*
     * Returns a localized zone string from bundle.
     */
    private static String getZoneStringFromBundle(ICUResourceBundle bundle, String key, String type) {
        String zstring = null;
        if (bundle != null) {
            try {
                zstring = bundle.getStringWithFallback(key + "/" + type);
            } catch (MissingResourceException ex) {
                // throw away the exception
            }
        }
        return zstring;
    }

    /*
     * Returns if the short strings of the zone/metazone is commonly used.
     */
    private static boolean isCommonlyUsed(ICUResourceBundle bundle, String key) {
        boolean commonlyUsed = false;
        if (bundle != null) {
            try {
                UResourceBundle cuRes = bundle.getWithFallback(key + "/" + RESKEY_COMMONLY_USED);
                int cuValue = cuRes.getInt();
                commonlyUsed = (cuValue != 0);
            } catch (MissingResourceException ex) {
                // throw away the exception
            }
        }
        return commonlyUsed;
    }

    /*
     * Returns a localized country string for the country code.  If no actual
     * localized string is found, countryCode itself is returned.
     */
    private static String getLocalizedCountry(String countryCode, ULocale locale) {
        String countryStr = null;
        if (countryCode != null) {
            ICUResourceBundle rb = 
                (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
//
// TODO: There is a design bug in UResourceBundle and getLoadingStatus() does not work well.
//
//            if (rb.getLoadingStatus() != ICUResourceBundle.FROM_ROOT && rb.getLoadingStatus() != ICUResourceBundle.FROM_DEFAULT) {
//                country = ULocale.getDisplayCountry("xx_" + country_code, locale);
//            }
// START WORKAROUND
            ULocale rbloc = rb.getULocale();
            if (!rbloc.equals(ULocale.ROOT) && rbloc.getLanguage().equals(locale.getLanguage())) {
                countryStr = ULocale.getDisplayCountry("xx_" + countryCode, locale);
            }
// END WORKAROUND
            if (countryStr == null || countryStr.length() == 0) {
                countryStr = countryCode;
            }
        }
        return countryStr;
    }

    /*
     * Gets an instance of MessageFormat used for formatting zone fallback string
     */
    private static MessageFormat getFallbackFormat(ULocale locale) {
        String fallbackPattern = ZoneMeta.getTZLocalizationInfo(locale, ZoneMeta.FALLBACK_FORMAT);
        if (fallbackPattern == null) {
            fallbackPattern = "{1} ({0})";
        }
        return new MessageFormat(fallbackPattern, locale);
    }

    /*
     * Gets an instance of MessageFormat used for formatting zone region string
     */
    private static MessageFormat getRegionFormat(ULocale locale) {
        String regionPattern = ZoneMeta.getTZLocalizationInfo(locale, ZoneMeta.REGION_FORMAT);
        if (regionPattern == null) {
            regionPattern = "{0}";
        }
        return new MessageFormat(regionPattern, locale);
    }

    /*
     * Index value mapping between DateFormatSymbols's zoneStrings and
     * the string types defined in this class.
     */
    private static final int[] INDEXMAP = {
        -1,             // 0 - zone id
        ZSIDX_LONG_STANDARD,  // 1 - long standard
        ZSIDX_SHORT_STANDARD, // 2 - short standard
        ZSIDX_LONG_DAYLIGHT,  // 3 - long daylight
        ZSIDX_SHORT_DAYLIGHT, // 4 - short daylight
        ZSIDX_LOCATION,       // 5 - generic location
        ZSIDX_LONG_GENERIC,   // 6 - long generic non-location
        ZSIDX_SHORT_GENERIC   // 7 - short generic non-location
    };

    /*
     * Convert from zone string array index for zoneStrings used by DateFormatSymbols#get/setZoneStrings
     * to the type constants defined by this class, such as ZSIDX_LONG_STANDARD.
     */
    private static int getNameTypeIndex(int i) {
        int idx = -1;
        if (i >= 1 && i < INDEXMAP.length) {
            idx = INDEXMAP[i];
        }
        return idx;
    }

    /*
     * Mapping from name type index to name type
     */
    private static final int[] NAMETYPEMAP = {
        LOCATION,       // ZSIDX_LOCATION
        STANDARD_LONG,  // ZSIDX_LONG_STANDARD
        STANDARD_SHORT, // ZSIDX_SHORT_STANDARD
        DAYLIGHT_LONG,  // ZSIDX_LONG_DAYLIGHT
        DAYLIGHT_SHORT, // ZSIDX_SHORT_DAYLIGHT
        GENERIC_LONG,   // ZSIDX_LONG_GENERIC
        GENERIC_SHORT,  // ZSIDX_SHORT_GENERIC
    };

    private static int getNameType(int typeIdx) {
        int type = -1;
        if (typeIdx >= 0 && typeIdx < NAMETYPEMAP.length) {
            type = NAMETYPEMAP[typeIdx];
        }
        return type;
    }

    /*
     * Returns region used for ZoneMeta#getZoneIdByMetazone.
     */
    private String getRegion() {
        if (region == null) {
            if (locale != null) {
                region = locale.getCountry();
                if (region.length() == 0) {
                    ULocale tmp = ULocale.addLikelySubtag(locale);
                    region = tmp.getCountry();
                }
            } else {
                region = "";
            }
        }
        return region;
    }

    /*
     * Find a prefix matching time zone for the given zone string types.
     * @param text The text contains a time zone string
     * @param start The start index within the text
     * @param types The bit mask representing a set of requested types
     * @return If any zone string matched for the requested types, returns a
     * ZoneStringInfo for the longest match.  If no matches are found for
     * the requested types, returns a ZoneStringInfo for the longest match
     * for any other types.  If nothing matches at all, returns null.
     */
    private ZoneStringInfo find(String text, int start, int types) {
        ZoneStringInfo result = null;
        ZoneStringSearchResultHandler handler = new ZoneStringSearchResultHandler();
        zoneStringsTrie.find(text, start, handler);
        List list = handler.getMatchedZoneStrings();
        ZoneStringInfo fallback = null;
        if (list != null && list.size() > 0) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                ZoneStringInfo tmp = (ZoneStringInfo)it.next();
                if ((types & tmp.getType()) != 0) {
                    if (result == null || result.getString().length() < tmp.getString().length()) {
                        result = tmp;
                    } else if (result.getString().length() == tmp.getString().length()) {
                        // Tie breaker - there are some examples that a
                        // long standard name is identical with a location
                        // name - for example, "Uruguay Time".  In this case,
                        // we interpret it as generic, not specific.
                        if (tmp.isGeneric() && !result.isGeneric()) {
                            result = tmp;
                        }
                    }
                } else if (result == null) {
                    if (fallback == null || fallback.getString().length() < tmp.getString().length()) {
                        fallback = tmp;
                    } else if (fallback.getString().length() == tmp.getString().length()) {
                        if (tmp.isGeneric() && !fallback.isGeneric()) {
                            fallback = tmp;
                        }
                    }
                }
            }
        }
        if (result == null && fallback != null) {
            result = fallback;
        }
        return result;
    }

    private static class ZoneStringSearchResultHandler implements TextTrieMap.ResultHandler {

        private ArrayList resultList;

        public boolean handlePrefixMatch(int matchLength, Iterator values) {
            if (resultList == null) {
                resultList = new ArrayList();
            }
            while (values.hasNext()) {
                ZoneStringInfo zsitem = (ZoneStringInfo)values.next();
                if (zsitem == null) {
                    break;
                }
                int i = 0;
                for (; i < resultList.size(); i++) {
                    ZoneStringInfo tmp = (ZoneStringInfo)resultList.get(i);
                    if (zsitem.getType() == tmp.getType()) {
                        if (matchLength > tmp.getString().length()) {
                            resultList.set(i, zsitem);
                        }
                        break;
                    }
                }
                if (i == resultList.size()) {
                    // not found in the current list
                    resultList.add(zsitem);
                }
            }
            return true;
        }

        List getMatchedZoneStrings() {
            if (resultList == null || resultList.size() == 0) {
                return null;
            }
            return resultList;
        }
    }
}
