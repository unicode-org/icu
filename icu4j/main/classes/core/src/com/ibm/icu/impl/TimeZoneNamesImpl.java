/*
 *******************************************************************************
 * Copyright (C) 2011-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
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
import com.ibm.icu.impl.UResource.TableSink;
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
    private static final NameType[] NAME_TYPE_VALUES = NameType.values();

    private static volatile Set<String> METAZONE_IDS;
    private static final TZ2MZsCache TZ_TO_MZS_CACHE = new TZ2MZsCache();
    private static final MZ2TZsCache MZ_TO_TZS_CACHE = new MZ2TZsCache();

    private transient ICUResourceBundle _zoneStrings;


    // These are hard cache. We create only one TimeZoneNamesImpl per locale
    // and it's stored in SoftCache, so we do not need to worry about the
    // footprint much.
    private transient ConcurrentHashMap<String, ZNames> _mzNamesMap;
    private transient ConcurrentHashMap<String, ZNames> _tzNamesMap;
    private transient boolean _namesFullyLoaded;

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
        return loadMetaZoneNames(null, mzID).getName(type);
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
        return loadTimeZoneNames(null, tzID).getName(type);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.TimeZoneNames#getExemplarLocationName(java.lang.String)
     */
    @Override
    public String getExemplarLocationName(String tzID) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        String locName = loadTimeZoneNames(null, tzID).getName(NameType.EXEMPLAR_LOCATION);
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
            // perfect match, or no more names available
            return handler.getMatches();
        }

        // All names are not yet loaded into the trie.
        // We may have loaded names for formatting several time zones,
        // and might be parsing one of those.
        // Populate the parsing trie from all of the already-loaded names.
        addAllNamesIntoTrie();
        handler.resetResults();
        _namesTrie.find(text, start, handler);
        if (handler.getMaxMatchLen() == (text.length() - start)) {
            // perfect match
            return handler.getMatches();
        }

        // Still no match, load all names.
        internalLoadAllDisplayNames();
        addAllNamesIntoTrie();

        // Set default time zone location names
        // for time zones without explicit display names.
        Set<String> tzIDs = TimeZone.getAvailableIDs(SystemTimeZoneType.CANONICAL, null, null);
        for (String tzID : tzIDs) {
            if (!_tzNamesMap.containsKey(tzID)) {
                tzID = tzID.intern();
                ZNames tznames = ZNames.getInstance(null, tzID);
                tznames.addNamesIntoTrie(null, tzID, _namesTrie);
                _tzNamesMap.put(tzID, tznames);
            }
        }
        _namesTrieFullyLoaded = true;

        // now, try it again
        handler.resetResults();
        _namesTrie.find(text, start, handler);
        return handler.getMatches();
    }

    @Override
    public synchronized void loadAllDisplayNames() {
        internalLoadAllDisplayNames();
    }

    @Override
    public void getDisplayNames(String tzID, NameType[] types, long date,
            String[] dest, int destOffset) {
        if (tzID == null || tzID.length() == 0) {
            return;
        }
        ZNames tzNames = loadTimeZoneNames(null, tzID);
        ZNames mzNames = null;
        for (int i = 0; i < types.length; ++i) {
            NameType type = types[i];
            String name = tzNames.getName(type);
            if (name == null) {
                if (mzNames == null) {
                    String mzID = getMetaZoneID(tzID, date);
                    if (mzID == null || mzID.length() == 0) {
                        mzNames = ZNames.EMPTY_ZNAMES;
                    } else {
                        mzNames = loadMetaZoneNames(null, mzID);
                    }
                }
                name = mzNames.getName(type);
            }
            dest[destOffset + i] = name;
        }
    }

    /** Caller must synchronize. */
    private void internalLoadAllDisplayNames() {
        if (!_namesFullyLoaded) {
            new ZoneStringsLoader().load();
            _namesFullyLoaded = true;
        }
    }

    /** Caller must synchronize. */
    private void addAllNamesIntoTrie() {
        for (Map.Entry<String, ZNames> entry : _tzNamesMap.entrySet()) {
            entry.getValue().addNamesIntoTrie(null, entry.getKey(), _namesTrie);
        }
        for (Map.Entry<String, ZNames> entry : _mzNamesMap.entrySet()) {
            entry.getValue().addNamesIntoTrie(entry.getKey(), null, _namesTrie);
        }
    }

    /**
     * Loads all meta zone and time zone names for this TimeZoneNames' locale.
     */
    private final class ZoneStringsLoader extends UResource.TableSink {
        /**
         * Prepare for several hundred time zones and meta zones.
         * _zoneStrings.getSize() is ineffective in a sparsely populated locale like en-GB.
         */
        private static final int INITIAL_NUM_ZONES = 300;
        private HashMap<UResource.Key, ZNamesLoader> keyToLoader =
                new HashMap<UResource.Key, ZNamesLoader>(INITIAL_NUM_ZONES);
        private StringBuilder sb = new StringBuilder(32);

        /** Caller must synchronize. */
        void load() {
            _zoneStrings.getAllTableItemsWithFallback("", this);
            for (Map.Entry<UResource.Key, ZNamesLoader> entry : keyToLoader.entrySet()) {
                UResource.Key key = entry.getKey();
                ZNamesLoader loader = entry.getValue();
                if (loader == ZNamesLoader.DUMMY_LOADER) {
                    // skip
                } else if (key.startsWith(MZ_PREFIX)) {
                    String mzID = mzIDFromKey(key).intern();
                    ZNames mzNames = ZNames.getInstance(loader.getNames(), null);
                    _mzNamesMap.put(mzID, mzNames);
                } else {
                    String tzID = tzIDFromKey(key).intern();
                    ZNames tzNames = ZNames.getInstance(loader.getNames(), tzID);
                    _tzNamesMap.put(tzID, tzNames);
                }
            }
        }

        @Override
        public TableSink getOrCreateTableSink(UResource.Key key, int initialSize) {
            ZNamesLoader loader = keyToLoader.get(key);
            if (loader != null) {
                if (loader == ZNamesLoader.DUMMY_LOADER) {
                    return null;
                }
                return loader;
            }
            ZNamesLoader result = null;
            if (key.startsWith(MZ_PREFIX)) {
                String mzID = mzIDFromKey(key);
                if (_mzNamesMap.containsKey(mzID)) {
                    // We have already loaded the names for this meta zone.
                    loader = ZNamesLoader.DUMMY_LOADER;
                } else {
                    result = loader = ZNamesLoader.forMetaZoneNames();
                }
            } else {
                String tzID = tzIDFromKey(key);
                if (_tzNamesMap.containsKey(tzID)) {
                    // We have already loaded the names for this time zone.
                    loader = ZNamesLoader.DUMMY_LOADER;
                } else {
                    result = loader = ZNamesLoader.forTimeZoneNames();
                }
            }
            keyToLoader.put(key.clone(), loader);
            return result;
        }

        @Override
        public void putNoFallback(UResource.Key key) {
            if (!keyToLoader.containsKey(key)) {
                keyToLoader.put(key.clone(), ZNamesLoader.DUMMY_LOADER);
            }
        }

        /**
         * Equivalent to key.substring(MZ_PREFIX.length())
         * except reuses our StringBuilder.
         */
        private String mzIDFromKey(UResource.Key key) {
            sb.setLength(0);
            for (int i = MZ_PREFIX.length(); i < key.length(); ++i) {
                sb.append(key.charAt(i));
            }
            return sb.toString();
        }

        private String tzIDFromKey(UResource.Key key) {
            sb.setLength(0);
            for (int i = 0; i < key.length(); ++i) {
                char c = key.charAt(i);
                if (c == ':') {
                    c = '/';
                }
                sb.append(c);
            }
            return sb.toString();
        }
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

        // TODO: Access is synchronized, can we use a non-concurrent map?
        _tzNamesMap = new ConcurrentHashMap<String, ZNames>();
        _mzNamesMap = new ConcurrentHashMap<String, ZNames>();
        _namesFullyLoaded = false;

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
        loadTimeZoneNames(null, tzCanonicalID);

        ZNamesLoader loader = ZNamesLoader.forMetaZoneNames();
        Set<String> mzIDs = getAvailableMetaZoneIDs(tzCanonicalID);
        for (String mzID : mzIDs) {
            loadMetaZoneNames(loader, mzID);
        }
        addAllNamesIntoTrie();
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
    private synchronized ZNames loadMetaZoneNames(ZNamesLoader loader, String mzID) {
        ZNames znames = _mzNamesMap.get(mzID);
        if (znames == null) {
            if (loader == null) {
                loader = ZNamesLoader.forMetaZoneNames();
            }
            znames = ZNames.getInstance(loader, _zoneStrings, MZ_PREFIX + mzID, null);
            mzID = mzID.intern();
            if (_namesTrieFullyLoaded) {
                znames.addNamesIntoTrie(mzID, null, _namesTrie);
            }
            _mzNamesMap.put(mzID, znames);
        }
        return znames;
    }

    /**
     * Returns a set of names for the given time zone ID. This method loads
     * the set of names into the internal map and trie for future references.
     * @param tzID the canonical time zone ID
     * @return An instance of TZNames that includes a set of time zone display names.
     */
    private synchronized ZNames loadTimeZoneNames(ZNamesLoader loader, String tzID) {
        ZNames tznames = _tzNamesMap.get(tzID);
        if (tznames == null) {
            if (loader == null) {
                loader = ZNamesLoader.forTimeZoneNames();
            }
            tznames = ZNames.getInstance(loader, _zoneStrings, tzID.replace('/', ':'), tzID);
            tzID = tzID.intern();
            if (_namesTrieFullyLoaded) {
                tznames.addNamesIntoTrie(null, tzID, _namesTrie);
            }
            _tzNamesMap.put(tzID, tznames);
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

    private static final class ZNamesLoader extends UResource.TableSink {
        private static int NUM_META_ZONE_NAMES = 6;
        private static int NUM_TIME_ZONE_NAMES = 7;  // incl. EXEMPLAR_LOCATION

        private static String NO_NAME = "";

        /**
         * Does not load any names, for no-fallback handling.
         */
        private static ZNamesLoader DUMMY_LOADER = new ZNamesLoader(0);

        private String[] names;
        private int numNames;

        private ZNamesLoader(int numNames) {
            this.numNames = numNames;
        }

        static ZNamesLoader forMetaZoneNames() {
            return new ZNamesLoader(NUM_META_ZONE_NAMES);
        }

        static ZNamesLoader forTimeZoneNames() {
            return new ZNamesLoader(NUM_TIME_ZONE_NAMES);
        }

        String[] load(ICUResourceBundle zoneStrings, String key) {
            if (zoneStrings == null || key == null || key.length() == 0) {
                return null;
            }

            try {
                zoneStrings.getAllTableItemsWithFallback(key, this);
            } catch (MissingResourceException e) {
                return null;
            }

            return getNames();
        }

        private static NameType nameTypeFromKey(UResource.Key key) {
            // Avoid key.toString() object creation.
            if (key.length() != 2) {
                return null;
            }
            char c0 = key.charAt(0);
            char c1 = key.charAt(1);
            if (c0 == 'l') {
                return c1 == 'g' ? NameType.LONG_GENERIC :
                        c1 == 's' ? NameType.LONG_STANDARD :
                            c1 == 'd' ? NameType.LONG_DAYLIGHT : null;
            } else if (c0 == 's') {
                return c1 == 'g' ? NameType.SHORT_GENERIC :
                        c1 == 's' ? NameType.SHORT_STANDARD :
                            c1 == 'd' ? NameType.SHORT_DAYLIGHT : null;
            } else if (c0 == 'e' && c1 == 'c') {
                return NameType.EXEMPLAR_LOCATION;
            }
            return null;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value) {
            if (value.getType() == UResourceBundle.STRING) {
                if (names == null) {
                    names = new String[numNames];
                }
                NameType type = nameTypeFromKey(key);
                if (type != null && type.ordinal() < numNames && names[type.ordinal()] == null) {
                    names[type.ordinal()] = value.getString();
                }
            }
        }

        @Override
        public void putNoFallback(UResource.Key key) {
            if (names == null) {
                names = new String[numNames];
            }
            NameType type = nameTypeFromKey(key);
            if (type != null && type.ordinal() < numNames && names[type.ordinal()] == null) {
                names[type.ordinal()] = NO_NAME;
            }
        }

        private String[] getNames() {
            if (names == null) {
                return null;
            }
            int length = 0;
            for (int i = 0; i < numNames; ++i) {
                String name = names[i];
                if (name != null) {
                    if (name == NO_NAME) {
                        names[i] = null;
                    } else {
                        length = i + 1;
                    }
                }
            }
            if (length == 0) {
                return null;
            }
            if (length == numNames || numNames == NUM_TIME_ZONE_NAMES) {
                // Return the full array if the last name is set.
                // Also return the full *time* zone names array,
                // so that the exemplar location can be set.
                String[] result = names;
                names = null;
                return result;
            }
            // Return a shorter array for permanent storage.
            // *Move* all names into a minimal array.
            String[] result = new String[length];
            do {
                --length;
                result[length] = names[length];
                names[length] = null;  // Reset for loading another set of names.
            } while (length > 0);
            return result;
        }
    }

    /**
     * This class stores name data for a meta zone or time zone.
     */
    private static class ZNames {
        private static final ZNames EMPTY_ZNAMES = new ZNames(null);
        // A meta zone names instance never has an exemplar location string.
        private static final int EX_LOC_INDEX = NameType.EXEMPLAR_LOCATION.ordinal();

        private String[] _names;
        private boolean didAddIntoTrie;

        protected ZNames(String[] names) {
            _names = names;
            didAddIntoTrie = names == null;
        }

        public static ZNames getInstance(String[] names, String tzID) {
            if (tzID != null && (names == null || names[EX_LOC_INDEX] == null)) {
                String locationName = getDefaultExemplarLocationName(tzID);
                if (locationName != null) {
                    if (names == null) {
                        names = new String[EX_LOC_INDEX + 1];
                    }
                    names[EX_LOC_INDEX] = locationName;
                }
            }

            if (names == null) {
                return EMPTY_ZNAMES;
            }
            return new ZNames(names);
        }

        public static ZNames getInstance(ZNamesLoader loader,
                ICUResourceBundle zoneStrings, String key, String tzID) {
            return getInstance(loader.load(zoneStrings, key), tzID);
        }

        public String getName(NameType type) {
            if (_names != null && type.ordinal() < _names.length) {
                return _names[type.ordinal()];
            } else {
                return null;
            }
        }

        public void addNamesIntoTrie(String mzID, String tzID, TextTrieMap<NameInfo> trie) {
            if (_names == null || didAddIntoTrie) {
                return;
            }
            for (int i = 0; i < _names.length; ++ i) {
                String name = _names[i];
                if (name != null) {
                    NameInfo info = new NameInfo();
                    info.mzID = mzID;
                    info.tzID = tzID;
                    info.type = NAME_TYPE_VALUES[i];
                    trie.put(name, info);
                }
            }
            didAddIntoTrie = true;
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
