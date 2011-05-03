/*
 *******************************************************************************
 * Copyright (C) 2011, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.icu.impl.ICUConfig;
import com.ibm.icu.impl.SoftCache;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * <code>TimeZoneNames</code> is an abstract class representing the time zone display name data model defined
 * by <a href="http://www.unicode.org/reports/tr35/">UTS#35 Unicode Locale Data Markup Language (LDML)</a>.
 * The model defines meta zone, which is used for storing a set of display names. A meta zone can be shared
 * by multiple time zones. Also a time zone may have multiple meta zone historic mappings.
 * <p>
 * For example, people in the United States refer the zone used by the east part of North America as "Eastern Time".
 * The tz database contains multiple time zones "America/New_York", "America/Detroit", "America/Montreal" and some
 * others that belong to "Eastern Time". However, assigning different display names to these time zones does not make
 * much sense for most of people.
 * <p>
 * In <a href="http://cldr.unicode.org/">CLDR</a> (which uses LDML for representing locale data), the display name
 * "Eastern Time" is stored as long generic display name of a meta zone identified by the ID "America_Eastern".
 * Then, there is another table maintaining the historic mapping to meta zones for each time zone. The time zones in
 * the above example ("America/New_York", "America/Detroit"...) are mapped to the meta zone "America_Eastern".
 * <p>
 * Sometimes, a time zone is mapped to a different time zone in the past. For example, "America/Indiana/Knox"
 * had been moving "Eastern Time" and "Central Time" back and forth. Therefore, it is necessary that time zone
 * to meta zones mapping data are stored by date range.
 * 
 * <p><b>Note:</b>
 * <p>
 * {@link TimeZoneFormat} assumes an instance of <code>TimeZoneNames</code> is immutable. If you want to provide
 * your own <code>TimeZoneNames</code> implementation and use it with {@link TimeZoneFormat}, you must follow
 * the contract.
 * <p>
 * The methods in this class assume that time zone IDs are already canonicalized. For example, you may not get proper
 * result returned by a method with time zone ID "America/Indiana/Indianapolis", because it's not a canonical time zone
 * ID (the canonical time zone ID for the time zone is "America/Indianapolis". See
 * {@link TimeZone#getCanonicalID(String)} about ICU canonical time zone IDs.
 * 
 * <p>
 * In CLDR, most of time zone display names except location names are provided through meta zones. But a time zone may
 * have a specific name that is not shared with other time zones.
 *
 * For example, time zone "Europe/London" has English long name for standard time "Greenwich Mean Time", which is also
 * shared with other time zones. However, the long name for daylight saving time is "British Summer Time", which is only
 * used for "Europe/London".
 * 
 * <p>
 * {@link #getTimeZoneDisplayName(String, NameType)} is designed for accessing a name only used by a single time zone.
 * But is not necessarily mean that a subclass implementation use the same model with CLDR. A subclass implementation
 * may provide time zone names only through {@link #getTimeZoneDisplayName(String, NameType)}, or only through
 * {@link #getMetaZoneDisplayName(String, NameType)}, or both.
 * 
 * @internal ICU 4.8 technology preview
 * @deprecated This API might change or be removed in a future release. 
 */
public abstract class TimeZoneNames implements Serializable {

    private static final long serialVersionUID = -9180227029248969153L;

    /**
     * Time zone display name types
     * 
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public enum NameType {
        /**
         * Long display name, such as "Eastern Time".
         * 
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release. 
         */
        LONG_GENERIC,
        /**
         * Long display name for standard time, such as "Eastern Standard Time".
         * 
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        LONG_STANDARD,
        /**
         * Long display name for daylight saving time, such as "Eastern Daylight Time".
         * 
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        LONG_DAYLIGHT,
        /**
         * Short display name, such as "ET".
         * 
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        SHORT_GENERIC,
        /**
         * Short display name for standard time, such as "EST".
         * 
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        SHORT_STANDARD,
        /**
         * Short display name for daylight saving time, such as "EDT".
         * 
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        SHORT_DAYLIGHT,
        /**
         * Short display name for standard time, such as "EST".
         * <p><b>Note:</b> The short abbreviation might not be well understood by people not familiar with the zone.
         * Unlike {@link #SHORT_STANDARD}, this type excludes short standard names not commonly used by the region.
         * 
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        SHORT_STANDARD_COMMONLY_USED,
        /**
         * Short display name for daylight saving time, such as "EDT".
         * <p><b>Note:</b> The short abbreviation might not be well understood by people not familiar with the zone.
         * Unlike {@link #SHORT_DAYLIGHT}, this type excludes short daylight names not commonly used by the region.
         * 
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        SHORT_DAYLIGHT_COMMONLY_USED
    }

    private static Cache TZNAMES_CACHE = new Cache();

    private static final Factory TZNAMES_FACTORY;
    private static final String FACTORY_NAME_PROP = "com.ibm.icu.text.TimeZoneNames.Factory.impl";
    private static final String DEFAULT_FACTORY_CLASS = "com.ibm.icu.impl.TimeZoneNamesFactoryImpl";
    private static final Pattern LOC_EXCLUSION_PATTERN = Pattern.compile("Etc/.*|SystemV/.*|.*/Riyadh8[7-9]");

    static {
        Factory factory = null;
        String classname = ICUConfig.get(FACTORY_NAME_PROP, DEFAULT_FACTORY_CLASS);
        while (true) {
            try {
                factory = (Factory) Class.forName(classname).newInstance();
                break;
            } catch (ClassNotFoundException cnfe) {
                // fall through
            } catch (IllegalAccessException iae) {
                // fall through
            } catch (InstantiationException ie) {
                // fall through
            }
            if (classname.equals(DEFAULT_FACTORY_CLASS)) {
                break;
            }
            classname = DEFAULT_FACTORY_CLASS;
        }

        if (factory == null) {
            factory = new DefaultTimeZoneNames.FactoryImpl();
        }
        TZNAMES_FACTORY = factory;
    }

    /**
     * Returns an instance of <code>TimeZoneDisplayNames</code> for the specified locale.
     * 
     * @param locale
     *            The locale.
     * @return An instance of <code>TimeZoneDisplayNames</code>
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public static TimeZoneNames getInstance(ULocale locale) {
        String key = locale.getBaseName();
        return TZNAMES_CACHE.getInstance(key, locale);
    }

    /**
     * Returns an immutable set of all available meta zone IDs.
     * @return An immutable set of all available meta zone IDs.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public abstract Set<String> getAvailableMetaZoneIDs();

    /**
     * Returns an immutable set of all available meta zone IDs used by the given time zone.
     * 
     * @param tzID
     *            The canonical time zone ID.
     * @return An immutable set of all available meta zone IDs used by the given time zone.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public abstract Set<String> getAvailableMetaZoneIDs(String tzID);

    /**
     * Returns the meta zone ID for the given canonical time zone ID at the given date.
     * 
     * @param tzID
     *            The canonical time zone ID.
     * @param date
     *            The date.
     * @return The meta zone ID for the given time zone ID at the given date. If the time zone does not have a
     *         corresponding meta zone at the given date or the implementation does not support meta zones, null is
     *         returned.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public abstract String getMetaZoneID(String tzID, long date);

    /**
     * Returns the reference zone ID for the given meta zone ID for the region.
     * 
     * @param mzID
     *            The meta zone ID.
     * @param region
     *            The region.
     * @return The reference zone ID ("golden zone" in the LDML specification) for the given time zone ID for the
     *         region. If the meta zone is unknown or the implementation does not support meta zones, null is returned.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public abstract String getReferenceZoneID(String mzID, String region);

    /**
     * Returns the display name of the meta zone.
     * 
     * @param mzID
     *            The meta zone ID.
     * @param type
     *            The display name type. See {@link TimeZoneNames.NameType}.
     * @return The display name of the meta zone. When this object does not have a localized display name for the given
     *         meta zone with the specified type or the implementation does not provide any display names associated
     *         with meta zones, null is returned.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public abstract String getMetaZoneDisplayName(String mzID, NameType type);

    /**
     * Returns the display name of the time zone at the given date.
     * 
     * <p>
     * <b>Note:</b> This method calls the subclass's {@link #getTimeZoneDisplayName(String, NameType)} first. When the
     * result is null, this method calls {@link #getMetaZoneID(String, long)} to get the meta zone ID mapped from the
     * time zone, then calls {@link #getMetaZoneDisplayName(String, NameType)}.
     * 
     * @param tzID
     *            The canonical time zone ID.
     * @param type
     *            The display name type. See {@link TimeZoneNames.NameType}.
     * @param date
     *            The date
     * @return The display name for the time zone at the given date. When this object does not have a localized display
     *         name for the time zone with the specified type and date, null is returned.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public final String getDisplayName(String tzID, NameType type, long date) {
        String name = getTimeZoneDisplayName(tzID, type);
        if (name == null) {
            String mzID = getMetaZoneID(tzID, date);
            name = getMetaZoneDisplayName(mzID, type);
        }
        return name;
    }

    /**
     * Returns the display name of the time zone. Unlike {@link #getDisplayName(String, NameType, long)},
     * this method does not get a name from a meta zone used by the time zone.
     * 
     * @param tzID
     *            The canonical time zone ID.
     * @param type
     *            The display name type. See {@link TimeZoneNames.NameType}.
     * @return The display name for the time zone. When this object does not have a localized display name for the given
     *         time zone with the specified type, null is returned.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public abstract String getTimeZoneDisplayName(String tzID, NameType type);

    /**
     * Returns the exemplar location name for the given time zone. When this object does not have a localized location
     * name, the default implementation may still returns a programmatically generated name with the logic described
     * below.
     * <ol>
     * <li>Check if the ID contains "/". If not, return null.
     * <li>Check if the ID does not start with "Etc/" or "SystemV/". If it does, return null.
     * <li>Extract a substring after the last occurrence of "/".
     * <li>Replace "_" with " ".
     * </ol>
     * For example, "New York" is returned for the time zone ID "America/New_York" when this object does not have the
     * localized location name.
     * 
     * @param tzID
     *            The canonical time zone ID
     * @return The exemplar location name for the given time zone, or null when a localized location name is not
     *         available and the fallback logic described above cannot extract location from the ID.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public String getExemplarLocationName(String tzID) {
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

    /**
     * Finds time zone name prefix matches for the input text at the
     * given offset and returns a collection of the matches.
     * 
     * @param text the text.
     * @param start the starting offset within the text.
     * @param types the set of name types, or <code>null</code> for all name types.
     * @return A collection of matches.
     * @see NameType
     * @see MatchInfo
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public Collection<MatchInfo> find(String text, int start, EnumSet<NameType> types) {
        throw new UnsupportedOperationException("The method is not implemented in TimeZoneNames base class.");
    }

    /**
     * A <code>MatchInfo</code> represents a time zone name match used by
     * {@link TimeZoneNames#find(String, int, EnumSet)}.
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public static class MatchInfo {
        private NameType _nameType;
        private String _tzID;
        private String _mzID;
        private int _matchLength;

        /**
         * Constructing a <code>MatchInfo</code>.
         * 
         * @param nameType the name type enum.
         * @param tzID the time zone ID, or null
         * @param mzID the meta zone ID, or null
         * @param matchLength the match length.
         * @throws IllegalArgumentException when 1) <code>nameType</code> is <code>null</code>,
         * or 2) both <code>tzID</code> and <code>mzID</code> are <code>null</code>,
         * or 3) <code>matchLength</code> is 0 or smaller.
         * @see NameType
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        public MatchInfo(NameType nameType, String tzID, String mzID, int matchLength) {
            if (nameType == null) {
                throw new IllegalArgumentException("nameType is null");
            }
            if (tzID == null && mzID == null) {
                throw new IllegalArgumentException("Either tzID or mzID must be available");
            }
            if (matchLength <= 0) {
                throw new IllegalArgumentException("matchLength must be positive value");
            }
            _nameType = nameType;
            _tzID = tzID;
            _mzID = mzID;
            _matchLength = matchLength;
        }

        /**
         * Returns the time zone ID, or <code>null</code> if not available.
         * 
         * <p><b>Note</b>: A <code>MatchInfo</code> must have either a time zone ID
         * or a meta zone ID.
         * 
         * @return the time zone ID, or <code>null</code>.
         * @see #mzID()
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        public String tzID() {
            return _tzID;
        }

        /**
         * Returns the meta zone ID, or <code>null</code> if not available.
         * 
         * <p><b>Note</b>: A <code>MatchInfo</code> must have either a time zone ID
         * or a meta zone ID.
         * 
         * @return the meta zone ID, or <code>null</code>.
         * @see #tzID()
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        public String mzID() {
            return _mzID;
        }

        /**
         * Returns the time zone name type.
         * @return the time zone name type enum.
         * @see NameType
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        public NameType nameType() {
            return _nameType;
        }

        /**
         * Returns the match length.
         * @return the match length.
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        public int matchLength() {
            return _matchLength;
        }
    }

    /**
     * Sole constructor for invocation by subclass constructors.
     * 
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release. 
     */
    protected TimeZoneNames() {
    }

    /**
     * The super class of <code>TimeZoneNames</code> service factory classes.
     * 
     * @internal
     */
    public static abstract class Factory {
        /**
         * The factory method of <code>TimeZoneNames</code>.
         * 
         * @param locale
         *            The display locale
         * @return An instance of <code>TimeZoneNames</code>.
         * @internal
         */
        public abstract TimeZoneNames getTimeZoneNames(ULocale locale);
    }

    /**
     * TimeZoneNames cache used by {@link TimeZoneNames#getInstance(ULocale)}
     */
    private static class Cache extends SoftCache<String, TimeZoneNames, ULocale> {

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.icu.impl.CacheBase#createInstance(java.lang.Object, java.lang.Object)
         */
        @Override
        protected TimeZoneNames createInstance(String key, ULocale data) {
            return TZNAMES_FACTORY.getTimeZoneNames(data);
        }

    }

    /**
     * The default implementation of <code>TimeZoneNames</code> used by {@link TimeZoneNames#getInstance(ULocale)} when
     * the ICU4J tznamedata component is not available.
     */
    private static class DefaultTimeZoneNames extends TimeZoneNames {

        private static final long serialVersionUID = -995672072494349071L;

        public static final DefaultTimeZoneNames INSTANCE = new DefaultTimeZoneNames();

        /* (non-Javadoc)
         * @see com.ibm.icu.text.TimeZoneNames#getAvailableMetaZoneIDs()
         */
        @Override
        public Set<String> getAvailableMetaZoneIDs() {
            return Collections.emptySet();
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.text.TimeZoneNames#getAvailableMetaZoneIDs(java.lang.String)
         */
        @Override
        public Set<String> getAvailableMetaZoneIDs(String tzID) {
            return Collections.emptySet();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.icu.text.TimeZoneNames#getMetaZoneID (java.lang.String, long)
         */
        @Override
        public String getMetaZoneID(String tzID, long date) {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.icu.text.TimeZoneNames#getReferenceZoneID(java.lang.String, java.lang.String)
         */
        @Override
        public String getReferenceZoneID(String mzID, String region) {
            return null;
        }

        /*
         *  (non-Javadoc)
         * @see com.ibm.icu.text.TimeZoneNames#getMetaZoneDisplayName(java.lang.String, com.ibm.icu.text.TimeZoneNames.NameType)
         */
        @Override
        public String getMetaZoneDisplayName(String mzID, NameType type) {
            return null;
        }

        /*
         * (non-Javadoc)
         * @see com.ibm.icu.text.TimeZoneNames#getTimeZoneDisplayName(java.lang.String, com.ibm.icu.text.TimeZoneNames.NameType)
         */
        @Override
        public String getTimeZoneDisplayName(String tzID, NameType type) {
            return null;
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.text.TimeZoneNames#find(java.lang.String, int, com.ibm.icu.text.TimeZoneNames.NameType[])
         */
        @Override
        public Collection<MatchInfo> find(String text, int start, EnumSet<NameType> nameTypes) {
            return Collections.emptyList();
        }

        /**
         * The default <code>TimeZoneNames</code> factory called from {@link TimeZoneNames#getInstance(ULocale)} when
         * the ICU4J tznamedata component is not available.
         */
        public static class FactoryImpl extends Factory {

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.icu.text.TimeZoneNames.Factory#getTimeZoneNames (com.ibm.icu.util.ULocale)
             */
            @Override
            public TimeZoneNames getTimeZoneNames(ULocale locale) {
                return DefaultTimeZoneNames.INSTANCE;
            }
        }
    }
}
