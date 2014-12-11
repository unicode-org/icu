/*
 *******************************************************************************
 * Copyright (C) 2011-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.ibm.icu.impl.TextTrieMap.ResultHandler;
import com.ibm.icu.text.TimeZoneNames;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZone.SystemTimeZoneType;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * The standard ICU implementation of TimeZoneNames
 */
public class TimeZoneNamesImpl extends TimeZoneNames {

    private static final long serialVersionUID = -2179814848495897472L;

    private static final String ZONE_STRINGS_BUNDLE = "zoneStrings";
    private static final String MZ_PREFIX = "meta:";

    private static volatile Set<String> METAZONE_IDS;
    private static final TZ2MZsCache TZ_TO_MZS_CACHE = new TZ2MZsCache();
    private static final MZ2TZsCache MZ_TO_TZS_CACHE = new MZ2TZsCache();

    private transient ICUResourceBundle _zoneStrings;


    // These are hard cache. We create only one TimeZoneNamesImpl per locale
    // and it's stored in SoftCache, so we do not need to worry about the
    // footprint much.
    private transient ConcurrentHashMap<String, ZNames> _mzNamesMap;
    private transient ConcurrentHashMap<String, TZNames> _tzNamesMap;

    private transient TextTrieMap<NameInfo> _namesTrie;
    private transient boolean _namesTrieFullyLoaded;

    public TimeZoneNamesImpl(ULocale locale) {
        initialize(locale);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#getAvailableMetaZoneIDs()
     */
    @Override
    public Set<String> getAvailableMetaZoneIDs() {
        return _getAvailableMetaZoneIDs();
    }

    static Set<String> _getAvailableMetaZoneIDs() {
        if (METAZONE_IDS == null) {
            synchronized (TimeZoneNamesImpl.class) {
                if (METAZONE_IDS == null) {
                    UResourceBundle bundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "metaZones");
                    UResourceBundle mapTimezones = bundle.get("mapTimezones");
                    Set<String> keys = mapTimezones.keySet();
                    METAZONE_IDS = Collections.unmodifiableSet(keys);
                }
            }
        }
        return METAZONE_IDS;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#getAvailableMetaZoneIDs(java.lang.String)
     */
    @Override
    public Set<String> getAvailableMetaZoneIDs(String tzID) {
        return _getAvailableMetaZoneIDs(tzID);
    }

    static Set<String> _getAvailableMetaZoneIDs(String tzID) {
        if (tzID == null || tzID.length() == 0) {
            return Collections.emptySet();
        }
        List<MZMapEntry> maps = TZ_TO_MZS_CACHE.getInstance(tzID, tzID);
        if (maps.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> mzIDs = new HashSet<String>(maps.size());
        for (MZMapEntry map : maps) {
            mzIDs.add(map.mzID());
        }
        // make it unmodifiable because of the API contract. We may cache the results in futre.
        return Collections.unmodifiableSet(mzIDs);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#getMetaZoneID(java.lang.String, long)
     */
    @Override
    public String getMetaZoneID(String tzID, long date) {
        return _getMetaZoneID(tzID, date);
    }

    static String _getMetaZoneID(String tzID, long date) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        String mzID = null;
        List<MZMapEntry> maps = TZ_TO_MZS_CACHE.getInstance(tzID, tzID);
        for (MZMapEntry map : maps) {
            if (date >= map.from() && date < map.to()) {
                mzID = map.mzID();
                break;
            }
        }
        return mzID;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#getReferenceZoneID(java.lang.String, java.lang.String)
     */
    @Override
    public String getReferenceZoneID(String mzID, String region) {
        return _getReferenceZoneID(mzID, region);
    }

    static String _getReferenceZoneID(String mzID, String region) {
        if (mzID == null || mzID.length() == 0) {
            return null;
        }
        String refID = null;
        Map<String, String> regionTzMap = MZ_TO_TZS_CACHE.getInstance(mzID, mzID);
        if (!regionTzMap.isEmpty()) {
            refID = regionTzMap.get(region);
            if (refID == null) {
                refID = regionTzMap.get("001");
            }
        }
        return refID;
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#getMetaZoneDisplayName(java.lang.String, com.ibm.icu.text.TimeZoneNames.NameType)
     */
    @Override
    public String getMetaZoneDisplayName(String mzID, NameType type) {
        if (mzID == null || mzID.length() == 0) {
            return null;
        }
        return loadMetaZoneNames(mzID).getName(type);
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#getTimeZoneDisplayName(java.lang.String, com.ibm.icu.text.TimeZoneNames.NameType)
     */
    @Override
    public String getTimeZoneDisplayName(String tzID, NameType type) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        return loadTimeZoneNames(tzID).getName(type);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#getExemplarLocationName(java.lang.String)
     */
    @Override
    public String getExemplarLocationName(String tzID) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        String locName = loadTimeZoneNames(tzID).getName(NameType.EXEMPLAR_LOCATION);
        return locName;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#find(java.lang.CharSequence, int, java.util.Set)
     */
    @Override
    public synchronized Collection<MatchInfo> find(CharSequence text, int start, EnumSet<NameType> nameTypes) {
        if (text == null || text.length() == 0 || start < 0 || start >= text.length()) {
            throw new IllegalArgumentException("bad input text or range");
        }
        NameSearchHandler handler = new NameSearchHandler(nameTypes);
        _namesTrie.find(text, start, handler);
        if (handler.getMaxMatchLen() == (text.length() - start) || _namesTrieFullyLoaded) {
            // perfect match
            return handler.getMatches();
        }

        // All names are not yet loaded into the trie

        // time zone names
        Set<String> tzIDs = TimeZone.getAvailableIDs(SystemTimeZoneType.CANONICAL, null, null);
        for (String tzID : tzIDs) {
            loadTimeZoneNames(tzID);
        }

        // meta zone names
        Set<String> mzIDs = getAvailableMetaZoneIDs();
        for (String mzID : mzIDs) {
            loadMetaZoneNames(mzID);
        }
        _namesTrieFullyLoaded = true;

        // now, try it again
        handler.resetResults();
        _namesTrie.find(text, start, handler);
        return handler.getMatches();
    }

    /**
     * Initialize the transient fields, called from the constructor and
     * readObject.
     * 
     * @param locale The locale
     */
    private void initialize(ULocale locale) {
        ICUResourceBundle bundle = (ICUResourceBundle)ICUResourceBundle.getBundleInstance(
                ICUResourceBundle.ICU_ZONE_BASE_NAME, locale);
        _zoneStrings = (ICUResourceBundle)bundle.get(ZONE_STRINGS_BUNDLE);

        _tzNamesMap = new ConcurrentHashMap<String, TZNames>();
        _mzNamesMap = new ConcurrentHashMap<String, ZNames>();

        _namesTrie = new TextTrieMap<NameInfo>(true);
        _namesTrieFullyLoaded = false;

        // Preload zone strings for the default time zone
        TimeZone tz = TimeZone.getDefault();
        String tzCanonicalID = ZoneMeta.getCanonicalCLDRID(tz);
        if (tzCanonicalID != null) {
            loadStrings(tzCanonicalID);
        }
    }

    /**
     * Load all strings used by the specified time zone.
     * This is called from the initializer to load default zone's
     * strings.
     * @param tzCanonicalID the canonical time zone ID
     */
    private synchronized void loadStrings(String tzCanonicalID) {
        if (tzCanonicalID == null || tzCanonicalID.length() == 0) {
            return;
        }
        loadTimeZoneNames(tzCanonicalID);

        Set<String> mzIDs = getAvailableMetaZoneIDs(tzCanonicalID);
        for (String mzID : mzIDs) {
            loadMetaZoneNames(mzID);
        }
    }

    /*
     * The custom serialization method.
     * This implementation only preserve locale object used for the names.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        ULocale locale = _zoneStrings.getULocale();
        out.writeObject(locale);
    }

    /*
     * The custom deserialization method.
     * This implementation only read locale object used by the object.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ULocale locale = (ULocale)in.readObject();
        initialize(locale);
    }

    /**
     * Returns a set of names for the given meta zone ID. This method loads
     * the set of names into the internal map and trie for future references.
     * @param mzID the meta zone ID
     * @return An instance of ZNames that includes a set of meta zone display names.
     */
    private synchronized ZNames loadMetaZoneNames(String mzID) {
        ZNames znames = _mzNamesMap.get(mzID);
        if (znames == null) {
            znames = ZNames.getInstance(_zoneStrings, MZ_PREFIX + mzID);
            // put names into the trie
            mzID = mzID.intern();
            for (NameType t : NameType.values()) {
                String name = znames.getName(t);
                if (name != null) {
                    NameInfo info = new NameInfo();
                    info.mzID = mzID;
                    info.type = t;
                    _namesTrie.put(name, info);
                }
            }
            ZNames tmpZnames = _mzNamesMap.putIfAbsent(mzID, znames);
            znames = (tmpZnames == null) ? znames : tmpZnames;
        }
        return znames;
    }

    /**
     * Returns a set of names for the given time zone ID. This method loads
     * the set of names into the internal map and trie for future references.
     * @param tzID the canonical time zone ID
     * @return An instance of TZNames that includes a set of time zone display names.
     */
    private synchronized TZNames loadTimeZoneNames(String tzID) {
        TZNames tznames = _tzNamesMap.get(tzID);
        if (tznames == null) {
            tznames = TZNames.getInstance(_zoneStrings, tzID.replace('/', ':'), tzID);
            // put names into the trie
            tzID = tzID.intern();
            for (NameType t : NameType.values()) {
                String name = tznames.getName(t);
                if (name != null) {
                    NameInfo info = new NameInfo();
                    info.tzID = tzID;
                    info.type = t;
                    _namesTrie.put(name, info);
                }
            }
            TZNames tmpTznames = _tzNamesMap.putIfAbsent(tzID, tznames);
            tznames = (tmpTznames == null) ? tznames : tmpTznames;
        }
        return tznames;
    }

    /**
     * An instance of NameInfo is stored in the zone names trie.
     */
    private static class NameInfo {
        String tzID;
        String mzID;
        NameType type;
    }

    /**
     * NameSearchHandler is used for collecting name matches.
     */
    private static class NameSearchHandler implements ResultHandler<NameInfo> {
        private EnumSet<NameType> _nameTypes;
        private Collection<MatchInfo> _matches;
        private int _maxMatchLen;

        NameSearchHandler(EnumSet<NameType> nameTypes) {
            _nameTypes = nameTypes;
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.impl.TextTrieMap.ResultHandler#handlePrefixMatch(int, java.util.Iterator)
         */
        public boolean handlePrefixMatch(int matchLength, Iterator<NameInfo> values) {
            while (values.hasNext()) {
                NameInfo ninfo = values.next();
                if (_nameTypes != null && !_nameTypes.contains(ninfo.type)) {
                    continue;
                }
                MatchInfo minfo;
                if (ninfo.tzID != null) {
                    minfo = new MatchInfo(ninfo.type, ninfo.tzID, null, matchLength);
                } else {
                    assert(ninfo.mzID != null);
                    minfo = new MatchInfo(ninfo.type, null, ninfo.mzID, matchLength);
                }
                if (_matches == null) {
                    _matches = new LinkedList<MatchInfo>();
                }
                _matches.add(minfo);
                if (matchLength > _maxMatchLen) {
                    _maxMatchLen = matchLength;
                }
            }
            return true;
        }

        /**
         * Returns the match results
         * @return the match results
         */
        public Collection<MatchInfo> getMatches() {
            if (_matches == null) {
                return Collections.emptyList();
            }
            return _matches;
        }

        /**
         * Returns the maximum match length, or 0 if no match was found
         * @return the maximum match length
         */
        public int getMaxMatchLen() {
            return _maxMatchLen;
        }

        /**
         * Resets the match results
         */
        public void resetResults() {
            _matches = null;
            _maxMatchLen = 0;
        }
    }

    /**
     * This class stores name data for a meta zone
     */
    private static class ZNames {
        private static final ZNames EMPTY_ZNAMES = new ZNames(null);

        private String[] _names;

        private static final String[] KEYS = {"lg", "ls", "ld", "sg", "ss", "sd"};

        protected ZNames(String[] names) {
            _names = names;
        }

        public static ZNames getInstance(ICUResourceBundle zoneStrings, String key) {
            String[] names = loadData(zoneStrings, key);
            if (names == null) {
                return EMPTY_ZNAMES;
            }
            return new ZNames(names);
        }

        public String getName(NameType type) {
            if (_names == null) {
                return null;
            }
            String name = null;
            switch (type) {
            case LONG_GENERIC:
                name = _names[0];
                break;
            case LONG_STANDARD:
                name = _names[1];
                break;
            case LONG_DAYLIGHT:
                name = _names[2];
                break;
            case SHORT_GENERIC:
                name = _names[3];
                break;
            case SHORT_STANDARD:
                name = _names[4];
                break;
            case SHORT_DAYLIGHT:
                name = _names[5];
                break;
            case EXEMPLAR_LOCATION:
                name = null;    // implemented by subclass
                break;
            }

            return name;
        }

        protected static String[] loadData(ICUResourceBundle zoneStrings, String key) {
            if (zoneStrings == null || key == null || key.length() == 0) {
                return null;
            }

            ICUResourceBundle table = null;
            try {
                table = zoneStrings.getWithFallback(key);
            } catch (MissingResourceException e) {
                return null;
            }

            boolean isEmpty = true;
            String[] names = new String[KEYS.length];
            for (int i = 0; i < names.length; i++) {
                try {
                    names[i] = table.getStringWithFallback(KEYS[i]);
                    isEmpty = false;
                } catch (MissingResourceException e) {
                    names[i] = null;
                }
            }

            if (isEmpty) {
                return null;
            }

            return names;
        }
    }

    /**
     * This class stores name data for a single time zone
     */
    private static class TZNames extends ZNames {
        private String _locationName;

        private static final TZNames EMPTY_TZNAMES = new TZNames(null, null);

        public static TZNames getInstance(ICUResourceBundle zoneStrings, String key, String tzID) {
            if (zoneStrings == null || key == null || key.length() == 0) {
                return EMPTY_TZNAMES;
            }

            String[] names = loadData(zoneStrings, key);
            String locationName = null;

            ICUResourceBundle table = null;
            try {
                table = zoneStrings.getWithFallback(key);
                locationName = table.getStringWithFallback("ec");
            } catch (MissingResourceException e) {
                // fall through
            }

            if (locationName == null) {
                locationName = getDefaultExemplarLocationName(tzID);
            }

            if (locationName == null && names == null) {
                return EMPTY_TZNAMES;
            }
            return new TZNames(names, locationName);
        }

        public String getName(NameType type) {
            if (type == NameType.EXEMPLAR_LOCATION) {
                return _locationName;
            }
            return super.getName(type);
        }

        private TZNames(String[] names, String locationName) {
            super(names);
            _locationName = locationName;
        }
    }


    //
    // Canonical time zone ID -> meta zone ID
    //

    private static class MZMapEntry {
        private String _mzID;
        private long _from;
        private long _to;

        MZMapEntry(String mzID, long from, long to) {
            _mzID = mzID;
            _from = from;
            _to = to;
        }

        String mzID() {
            return _mzID;
        }

        long from() {
            return _from;
        }

        long to() {
            return _to;
        }
    }

    private static class TZ2MZsCache extends SoftCache<String, List<MZMapEntry>, String> {
        /* (non-Javadoc)
         * @see com.ibm.icu.impl.CacheBase#createInstance(java.lang.Object, java.lang.Object)
         */
        @Override
        protected List<MZMapEntry> createInstance(String key, String data) {
            List<MZMapEntry> mzMaps = null;

            UResourceBundle bundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "metaZones");
            UResourceBundle metazoneInfoBundle = bundle.get("metazoneInfo");

            String tzkey = data.replace('/', ':');
            try {
                UResourceBundle zoneBundle = metazoneInfoBundle.get(tzkey);

                mzMaps = new ArrayList<MZMapEntry>(zoneBundle.getSize());
                for (int idx = 0; idx < zoneBundle.getSize(); idx++) {
                    UResourceBundle mz = zoneBundle.get(idx);
                    String mzid = mz.getString(0);
                    String fromStr = "1970-01-01 00:00";
                    String toStr = "9999-12-31 23:59";
                    if (mz.getSize() == 3) {
                        fromStr = mz.getString(1);
                        toStr = mz.getString(2);
                    }
                    long from, to;
                    from = parseDate(fromStr);
                    to = parseDate(toStr);
                    mzMaps.add(new MZMapEntry(mzid, from, to));
                }

            } catch (MissingResourceException mre) {
                mzMaps = Collections.emptyList();
            }
            return mzMaps;
        }

        /**
         * Private static method parsing the date text used by meta zone to
         * time zone mapping data in locale resource.
         * 
         * @param text the UTC date text in the format of "yyyy-MM-dd HH:mm",
         * for example - "1970-01-01 00:00"
         * @return the date
         */
        private static long parseDate (String text) {
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
                        + (long)hour * Grego.MILLIS_PER_HOUR + (long)min * Grego.MILLIS_PER_MINUTE;
            return date;
         }
    }

    //
    // Meta zone ID -> time zone ID
    //

    private static class MZ2TZsCache extends SoftCache<String, Map<String, String>, String> {

        /* (non-Javadoc)
         * @see com.ibm.icu.impl.CacheBase#createInstance(java.lang.Object, java.lang.Object)
         */
        @Override
        protected Map<String, String> createInstance(String key, String data) {
            Map<String, String> map = null;

            UResourceBundle bundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "metaZones");
            UResourceBundle mapTimezones = bundle.get("mapTimezones");

            try {
                UResourceBundle regionMap = mapTimezones.get(key);

                Set<String> regions = regionMap.keySet();
                map = new HashMap<String, String>(regions.size());

                for (String region : regions) {
                    String tzID = regionMap.getString(region).intern();
                    map.put(region.intern(), tzID);
                }
            } catch (MissingResourceException e) {
                map = Collections.emptyMap();
            }
            return map;
        }
    }

    private static final Pattern LOC_EXCLUSION_PATTERN = Pattern.compile("Etc/.*|SystemV/.*|.*/Riyadh8[7-9]");

    /**
     * Default exemplar location name based on time zone ID
     * @param tzID the time zone ID
     * @return the exemplar location name or null if location is not available.
     */
    public static String getDefaultExemplarLocationName(String tzID) {
        if (tzID == null || tzID.length() == 0 || LOC_EXCLUSION_PATTERN.matcher(tzID).matches()) {
            return null;
        }

        String location = null;
        int sep = tzID.lastIndexOf('/');
        if (sep > 0 && sep + 1 < tzID.length()) {
            location = tzID.substring(sep + 1).replace('_', ' ');
        }

        return location;
    }
}
