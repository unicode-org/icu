/*
 * @(#)TimeZone.java	1.51 00/01/19
 *
 *
 *   Copyright (C) 1996-2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 */


package com.ibm.icu.util;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

/**
 * <code>TimeZone</code> represents a time zone offset, and also figures out daylight
 * savings.
 *
 * <p>
 * Typically, you get a <code>TimeZone</code> using <code>getDefault</code>
 * which creates a <code>TimeZone</code> based on the time zone where the program
 * is running. For example, for a program running in Japan, <code>getDefault</code>
 * creates a <code>TimeZone</code> object based on Japanese Standard Time.
 *
 * <p>
 * You can also get a <code>TimeZone</code> using <code>getTimeZone</code>
 * along with a time zone ID. For instance, the time zone ID for the
 * U.S. Pacific Time zone is "America/Los_Angeles". So, you can get a
 * U.S. Pacific Time <code>TimeZone</code> object with:
 * <blockquote>
 * <pre>
 * TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
 * </pre>
 * </blockquote>
 * You can use <code>getAvailableIDs</code> method to iterate through
 * all the supported time zone IDs. You can then choose a
 * supported ID to get a <code>TimeZone</code>.
 * If the time zone you want is not represented by one of the
 * supported IDs, then you can create a custom time zone ID with
 * the following syntax:
 *
 * <blockquote>
 * <pre>
 * GMT[+|-]hh[[:]mm]
 * </pre>
 * </blockquote>
 *
 * For example, you might specify GMT+14:00 as a custom
 * time zone ID.  The <code>TimeZone</code> that is returned
 * when you specify a custom time zone ID does not include
 * daylight savings time.
 * <p>
 * For compatibility with JDK 1.1.x, some other three-letter time zone IDs
 * (such as "PST", "CTT", "AST") are also supported. However, <strong>their
 * use is deprecated</strong> because the same abbreviation is often used
 * for multiple time zones (for example, "CST" could be U.S. "Central Standard
 * Time" and "China Standard Time"), and the Java platform can then only
 * recognize one of them.
 *
 *
 * @see          Calendar
 * @see          GregorianCalendar
 * @see          SimpleTimeZone
 * @author       Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu
 * @stable ICU 2.0
 */
abstract public class TimeZone implements Serializable, Cloneable {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     * @stable ICU 2.0
     */
    public TimeZone() {
    }

    /**
     * A style specifier for <code>getDisplayName()</code> indicating
     * a short name, such as "PST."
     * @see #LONG
     * @stable ICU 2.0
     */
    public static final int SHORT = 0;

    /**
     * A style specifier for <code>getDisplayName()</code> indicating
     * a long name, such as "Pacific Standard Time."
     * @see #SHORT
     * @stable ICU 2.0
     */
    public static final int LONG  = 1;

    // Constants used internally; unit is milliseconds
    private static final int ONE_MINUTE = 60*1000;
    private static final int ONE_HOUR   = 60*ONE_MINUTE;

    /**
     * Cache to hold the SimpleDateFormat objects for a Locale.
     */
    private static Hashtable cachedLocaleData = new Hashtable(3);

    // Proclaim serialization compatibility with JDK 1.1
    static final long serialVersionUID = 3581463369166924961L;

    /**
     * Gets the time zone offset, for current date, modified in case of
     * daylight savings. This is the offset to add *to* UTC to get local time.
     * @param era the era of the given date.
     * @param year the year in the given date.
     * @param month the month in the given date.
     * Month is 0-based. e.g., 0 for January.
     * @param day the day-in-month of the given date.
     * @param dayOfWeek the day-of-week of the given date.
     * @param milliseconds the millis in day in <em>standard</em> local time.
     * @return the offset to add *to* GMT to get local time.
     * @stable ICU 2.0
     */
    abstract public int getOffset(int era, int year, int month, int day,
                                  int dayOfWeek, int milliseconds);

    // (The following is never called because it is always overridden
    // by the concrete SimpleTimeZone subclass.  It cannot be made
    // abstract without breaking other existing client subclasses.
    // 2003-06-11 ICU 2.6 Alan)
    ///CLOVER:OFF
    /**
     * Gets the time zone offset, for current date, modified in case of
     * daylight savings. This is the offset to add *to* UTC to get local time.
     * @param era the era of the given date.
     * @param year the year in the given date.
     * @param month the month in the given date.
     * Month is 0-based. e.g., 0 for January.
     * @param day the day-in-month of the given date.
     * @param dayOfWeek the day-of-week of the given date.
     * @param milliseconds the millis in day in <em>standard</em> local time.
     * @param monthLength the length of the given month in days.
     * @param prevMonthLength the length of the previous month in days.
     * @return the offset to add *to* GMT to get local time.
     */
    int getOffset(int era, int year, int month, int day,
		  int dayOfWeek, int milliseconds, int monthLength, int prevMonthLength) {
	// Default implementation which ignores the monthLength.
	// SimpleTimeZone overrides this and actually uses monthLength.
	return getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }
    ///CLOVER:ON
    
    /**
     * NEWCAL
     * Gets the local time zone offset for this zone, for the given date.
     * @param eyear the extended Gregorian year, with 0 = 1 BC, -1 = 2 BC,
     * etc.
     */
    int getOffset(int eyear, int month, int dayOfMonth, int dayOfWeek,
                  int milliseconds, int monthLength, int prevMonthLength) {
        // TEMPORARY: Convert the eyear to an era/year and call old API
        int era = GregorianCalendar.AD;
        if (eyear < 1) {
            era = GregorianCalendar.BC;
            eyear = 1 - eyear;
        }
        return getOffset(era, eyear, month, dayOfMonth, dayOfWeek,
                         milliseconds, monthLength, prevMonthLength);
    }

    /**
     * Sets the base time zone offset to GMT.
     * This is the offset to add *to* UTC to get local time.
     * @param offsetMillis the given base time zone offset to GMT.
     * @stable ICU 2.0
     */
    abstract public void setRawOffset(int offsetMillis);

    /**
     * Gets unmodified offset, NOT modified in case of daylight savings.
     * This is the offset to add *to* UTC to get local time.
     * @return the unmodified offset to add *to* UTC to get local time.
     * @stable ICU 2.0
     */
    abstract public int getRawOffset();

    /**
     * Gets the ID of this time zone.
     * @return the ID of this time zone.
     * @stable ICU 2.0
     */
    public String getID()
    {
        return ID;
    }

    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @param ID the new time zone ID.
     * @stable ICU 2.0
     */
    public void setID(String ID)
    {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the default locale.
     * This method returns the long name, not including daylight savings.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @return the human-readable name of this time zone in the default locale.
     * @stable ICU 2.0
     */
    public final String getDisplayName() {
        return getDisplayName(false, LONG, Locale.getDefault());
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * This method returns the long name, not including daylight savings.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default locale if the given locale is not recognized.
     * @stable ICU 2.0
     */
    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, LONG, locale);
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the default locale.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param daylight if true, return the daylight savings name.
     * @param style either <code>LONG</code> or <code>SHORT</code>
     * @return the human-readable name of this time zone in the default locale.
     * @stable ICU 2.0
     */
    public final String getDisplayName(boolean daylight, int style) {
        return getDisplayName(daylight, style, Locale.getDefault());
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param daylight if true, return the daylight savings name.
     * @param style either <code>LONG</code> or <code>SHORT</code>
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default locale if the given locale is not recognized.
     * @exception IllegalArgumentException style is invalid.
     * @stable ICU 2.0
     */
    public String getDisplayName(boolean daylight, int style, Locale locale) {
        /* NOTES:
         * (1) We use SimpleDateFormat for simplicity; we could do this
         * more efficiently but it would duplicate the SimpleDateFormat code
         * here, which is undesirable.
         * (2) Attempts to move the code from SimpleDateFormat to here also run
         * aground because this requires SimpleDateFormat to keep a Locale
         * object around, which it currently doesn't; to synthesize such a
         * locale upon resurrection; and to somehow handle the special case of
         * construction from a DateFormatSymbols object.
         */
        if (style != SHORT && style != LONG) {
            throw new IllegalArgumentException("Illegal style: " + style);
        }
        // We keep a cache, indexed by locale.  The cache contains a
        // SimpleDateFormat object, which we create on demand.
        SoftReference data = (SoftReference)cachedLocaleData.get(locale);
        SimpleDateFormat format;
        if (data == null ||
            (format = (SimpleDateFormat)data.get()) == null) {
            format = new SimpleDateFormat(null, locale);
            cachedLocaleData.put(locale, new SoftReference(format));
        }
        // Create a new SimpleTimeZone as a stand-in for this zone; the stand-in
        // will have no DST, or DST during January, but the same ID and offset,
        // and hence the same display name.  We don't cache these because
        // they're small and cheap to create.
        SimpleTimeZone tz;
        if (daylight && useDaylightTime()) {
            int savings = ONE_HOUR;
            try {
                savings = ((SimpleTimeZone) this).getDSTSavings();
            } catch (ClassCastException e) {}
            tz = new SimpleTimeZone(getRawOffset(), getID(),
                                    Calendar.JANUARY, 1, 0, 0,
                                    Calendar.FEBRUARY, 1, 0, 0,
                                    savings);
        } else {
            tz = new SimpleTimeZone(getRawOffset(), getID());
        }
        format.applyPattern(style == LONG ? "zzzz" : "z");      
        format.setTimeZone(tz);
        // Format a date in January.  We use the value 10*ONE_DAY == Jan 11 1970
        // 0:00 GMT.
        return format.format(new Date(864000000L));
    }

    /**
     * Queries if this time zone uses daylight savings time.
     * @return true if this time zone uses daylight savings time,
     * false, otherwise.
     * @stable ICU 2.0
     */
    abstract public boolean useDaylightTime();

    /**
     * Queries if the given date is in daylight savings time in
     * this time zone.
     * @param date the given Date.
     * @return true if the given date is in daylight savings time,
     * false, otherwise.
     * @stable ICU 2.0
     */
    abstract public boolean inDaylightTime(Date date);

    /**
     * Gets the <code>TimeZone</code> for the given ID.
     *
     * @param ID the ID for a <code>TimeZone</code>, either an abbreviation
     * such as "PST", a full name such as "America/Los_Angeles", or a custom
     * ID such as "GMT-8:00". Note that the support of abbreviations is
     * for JDK 1.1.x compatibility only and full names should be used.
     *
     * @return the specified <code>TimeZone</code>, or the GMT zone if the given ID
     * cannot be understood.
     * @stable ICU 2.0
     */
    public static synchronized TimeZone getTimeZone(String ID) {
        /* We first try to lookup the zone ID in our hashtable.  If this fails,
         * we try to parse it as a custom string GMT[+-]hh:mm.  This allows us
         * to recognize zones in user.timezone that otherwise cannot be
         * identified.  We do the recognition here, rather than in getDefault(),
         * so that the default zone is always the result of calling
         * getTimeZone() with the property user.timezone.
         *
         * If all else fails, we return GMT, which is probably not what the user
         * wants, but at least is a functioning TimeZone object. */
        TimeZone zone = TimeZoneData.get(ID);
        if (zone == null) zone = parseCustomTimeZone(ID);
        if (zone == null) zone = (TimeZone)GMT.clone();
        return zone;
    }

    /**
     * Return a new String array containing all system TimeZone IDs
     * with the given raw offset from GMT.  These IDs may be passed to
     * <code>get()</code> to construct the corresponding TimeZone
     * object.
     * @param rawOffset the offset in milliseconds from GMT
     * @return an array of IDs for system TimeZones with the given
     * raw offset.  If there are none, return a zero-length array.
     * @stable ICU 2.0
     */
    public static String[] getAvailableIDs(int rawOffset) {
        return TimeZoneData.getAvailableIDs(rawOffset);
    }

    /**
     * Return a new String array containing all system TimeZone IDs
     * associated with the given country.  These IDs may be passed to
     * <code>get()</code> to construct the corresponding TimeZone
     * object.
     * @param a two-letter ISO 3166 country code, or <code>null</code>
     * to return zones not associated with any country
     * @return an array of IDs for system TimeZones in the given
     * country.  If there are none, return a zero-length array.
     * @stable ICU 2.0
     */
    public static String[] getAvailableIDs(String country) {
        return TimeZoneData.getAvailableIDs(country);
    }

    /**
     * Return a new String array containing all system TimeZone IDs.
     * These IDs (and only these IDs) may be passed to
     * <code>get()</code> to construct the corresponding TimeZone
     * object.
     * @return an array of all system TimeZone IDs
     * @stable ICU 2.0
     */
    public static String[] getAvailableIDs() {
        return TimeZoneData.getAvailableIDs();
    }
    
    /**
     * Returns the number of IDs in the equivalency group that
     * includes the given ID.  An equivalency group contains zones
     * that have the same GMT offset and rules.
     *
     * <p>The returned count includes the given ID; it is always >= 1
     * for valid IDs.  The given ID must be a system time zone.  If it
     * is not, returns zero.
     * @param id a system time zone ID
     * @return the number of zones in the equivalency group containing
     * 'id', or zero if 'id' is not a valid system ID
     * @see #getEquivalentID
     * @stable ICU 2.0
     */
    public static int countEquivalentIDs(String id) {
        return TimeZoneData.countEquivalentIDs(id);
    }

    /**
     * Returns an ID in the equivalency group that
     * includes the given ID.  An equivalency group contains zones
     * that have the same GMT offset and rules.
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
     * @stable ICU 2.0
     */
    public static String getEquivalentID(String id, int index) {
        return TimeZoneData.getEquivalentID(id, index);
    }

    /**
     * Gets the platform defined TimeZone ID.
     **/
//    private static native String getSystemTimeZoneID(String javaHome, 
//						     String region);

    /**
     * Gets the default <code>TimeZone</code> for this host.
     * The source of the default <code>TimeZone</code> 
     * may vary with implementation.
     * @return a default <code>TimeZone</code>.
     * @stable ICU 2.0
     */
    public static synchronized TimeZone getDefault() {
        if (defaultZone == null) {
            // The following causes an AccessControlException when run in an
            // applet.  To get around this, we don't read the user.timezone
            // property; instead we defer to TimeZone.getDefault(). - Liu
            //!// get the time zone ID from the system properties
            //!String zoneID = (String) AccessController.doPrivileged(
            //!    new GetPropertyAction("user.timezone"));
            //!
	    //!// if the time zone ID is not set (yet), perform the
	    //!// platform to Java time zone ID mapping.
	    //!if (zoneID == null || zoneID.equals("")) { 
		//String region = (String) AccessController.doPrivileged(
		//    new GetPropertyAction("user.region"));
		//String javaHome = (String) AccessController.doPrivileged(
		//    new GetPropertyAction("java.home"));
		//zoneID = getSystemTimeZoneID(javaHome, region);

                // [icu4j We get the default zone by querying java.util.TimeZone,
                //        and then attempting to map the ID. - liu ]
                java.util.TimeZone _default = java.util.TimeZone.getDefault();
                if (false) System.out.println("java.util.TZ.default " + _default);
                String zoneID = _default.getID();
                defaultZone = TimeZoneData.get(zoneID);
                if (defaultZone == null) {
                    // [icu4j This means that the zone returned by the JDK does
                    // not exist in our table.  We will, for the moment, map to
                    // a std zone that has the same raw offset.  In the future
                    // we might find it worthwhile to extract the rules from the
                    // system default zone, but this is too much trouble for
                    // now.  It will be easier to extend our mapping table to
                    // match the JDKs we want to support. - liu ]
                    try {
                        java.util.SimpleTimeZone s = (java.util.SimpleTimeZone) _default;
                        defaultZone = new SimpleTimeZone(s.getRawOffset(), s.getID());
                    } catch (ClassCastException e) {}
                } else {
                    if (zoneID == null) {
                        zoneID = GMT_ID;
                    }
                    // Again, we can't do this from outside the JDK in an applet.
                    //!final String id = zoneID;
                    //!AccessController.doPrivileged(new PrivilegedAction() {
                    //!    public Object run() {
                    //!        System.setProperty("user.timezone", id);
                    //!        return null;
                    //!    }
                    //!});
                }
            //!}
            if (defaultZone == null) {
                defaultZone = getTimeZone(zoneID);
            }
            if (false) System.out.println("com.ibm.icu.util.TZ.default " + defaultZone);
        }
        return (TimeZone)defaultZone.clone();
    }

    /**
     * Sets the <code>TimeZone</code> that is
     * returned by the <code>getDefault</code> method.  If <code>zone</code>
     * is null, reset the default to the value it had originally when the
     * VM first started.
     * @param zone the new default time zone
     * @stable ICU 2.0
     */
    public static synchronized void setDefault(TimeZone zone)
    {
        defaultZone = zone;
        // [icu4j Keep java.util.TimeZone default in sync so java.util.Date
        //        can interoperate with com.ibm.icu.util classes.  This solution
        //        is _imperfect_; see SimpleTimeZoneAdapter. - liu]
        try {
            java.util.TimeZone.setDefault(
                new SimpleTimeZoneAdapter((SimpleTimeZone) zone));
        } catch (ClassCastException e) {}
    }

    /**
     * Returns true if this zone has the same rule and offset as another zone.
     * That is, if this zone differs only in ID, if at all.  Returns false
     * if the other zone is null.
     * @param other the <code>TimeZone</code> object to be compared with
     * @return true if the other zone is not null and is the same as this one,
     * with the possible exception of the ID
     * @stable ICU 2.0
     */
    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset() &&
            useDaylightTime() == other.useDaylightTime();
    }

    /**
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone()
    {
        try {
            TimeZone other = (TimeZone) super.clone();
            other.ID = ID;
            return other;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    // =======================privates===============================

    /**
     * The string identifier of this <code>TimeZone</code>.  This is a
     * programmatic identifier used internally to look up <code>TimeZone</code>
     * objects from the system table and also to map them to their localized
     * display names.  <code>ID</code> values are unique in the system
     * table but may not be for dynamically created zones.
     * @serial
     */
    private String           ID;
    private static TimeZone  defaultZone = null;

    static final String         GMT_ID        = "GMT";
    private static final int    GMT_ID_LENGTH = 3;
    private static final String CUSTOM_ID     = "Custom";

    private static NumberFormat numberFormat = null;

    private static final TimeZone GMT = new SimpleTimeZone(0, GMT_ID);

    /**
     * Parse a custom time zone identifier and return a corresponding zone.
     * @param id a string of the form GMT[+-]hh:mm, GMT[+-]hhmm, or
     * GMT[+-]hh.
     * @return a newly created SimpleTimeZone with the given offset and
     * no daylight savings time, or null if the id cannot be parsed.
     */
    private static final SimpleTimeZone parseCustomTimeZone(String id) {
        if (id.length() > GMT_ID_LENGTH &&
            id.regionMatches(true, 0, GMT_ID, 0, GMT_ID_LENGTH)) {
            ParsePosition pos = new ParsePosition(GMT_ID_LENGTH);
            boolean negative = false;
            int offset;

            if (id.charAt(pos.getIndex()) == '-')
                negative = true;
            else if (id.charAt(pos.getIndex()) != '+')
                return null;
            pos.setIndex(pos.getIndex() + 1);

            // Create NumberFormat if necessary
            synchronized (TimeZone.class) {
                if (numberFormat == null) {
                    numberFormat = NumberFormat.getInstance();
                    numberFormat.setParseIntegerOnly(true);
                }
            }

            synchronized (numberFormat) {
                // Look for either hh:mm, hhmm, or hh
                int start = pos.getIndex();
                Number n = numberFormat.parse(id, pos);
                if (n == null) return null;
                offset = n.intValue();

                if (pos.getIndex() < id.length() &&
                    id.charAt(pos.getIndex()) == ':') {
                    // hh:mm
                    offset *= 60;
                    pos.setIndex(pos.getIndex() + 1);
                    n = numberFormat.parse(id, pos);
                    if (n == null) return null;
                    offset += n.intValue();
                }
                else {
                    // hhmm or hh

                    // Be strict about interpreting something as hh; it must be
                    // an offset < 30, and it must be one or two digits. Thus
                    // 0010 is interpreted as 00:10, but 10 is interpreted as
                    // 10:00.
                    if (offset < 30 && (pos.getIndex() - start) <= 2)
                        offset *= 60; // hh, from 00 to 29; 30 is 00:30
                    else
                        offset = offset % 100 + offset / 100 * 60; // hhmm
                }

                if (negative) offset = -offset;
                return new SimpleTimeZone(offset * 60000, CUSTOM_ID);
            }
        }

        return null;
    }

    // Internal Implementation Notes [LIU]
    //
    // TimeZone data is stored in two parts.  The first is an encoding of the
    // rules for each TimeZone.  A TimeZone rule includes the offset of a zone
    // in milliseconds from GMT, the starting month and day for daylight savings
    // time, if there is any, and the ending month and day for daylight savings
    // time.  The starting and ending days are specified in terms of the n-th
    // day of the week, for instance, the first Sunday or the last ("-1"-th)
    // Sunday of the month.  The rules are stored as statically-constructed
    // SimpleTimeZone objects in the TimeZone class.
    //
    // Each rule has a unique internal identifier string which is used to
    // specify it.  This identifier string is arbitrary, and is not to be shown
    // to the user -- it is for programmatic use only.  In order to instantiate
    // a TimeZone object, you pass its identifier string to
    // TimeZone.getTimeZone().  (This identifier is also used to index the
    // localized string data.)
    //
    // The second part of the data consists of localized string names used by
    // DateFormat to describe various TimeZones.  A TimeZone may have up to four
    // names: The abbreviated and long name for standard time in that zone, and
    // the abbreviated and long name for daylight savings time in that zone.
    // The data also includes a representative city.  For example, [ "PST",
    // "Pacific Standard Time", "PDT", "Pacific Daylight Time", "Los Angeles" ]
    // might be one such set of string names in the en_US locale.  These strings
    // are intended to be shown to the user.  The string data is indexed in the
    // system by a pair (String id, Locale locale).  The id is the unique string
    // identifier for the rule for the given TimeZone (as passed to
    // TimeZone.getTimeZone()).  String names are stored as localized resource
    // data (in jdk 1.2 and 1.3) of the class LocaleData???  where ??? is
    // the Locale specifier (e.g., DateFormatZoneData_en_US).  This data is a
    // two-dimensional array of strings with N rows and 6 columns.  The columns
    // are id, short standard name, long standard name, short daylight name,
    // long daylight name, representative city name.
    //
    // The mapping between rules (SimpleTimeZone objects) and localized string
    // names (DateFormatZoneData objects) is one-to-many.  That is, there will
    // sometimes be more than one localized string name sets associated with
    // each rule.
    //
    // Each locale can potentially have localized name data for all time zones.
    // Since we support approximately 90 time zones and approximately 50
    // locales, there can be over 4500 sets of localized names.  In practice,
    // only a fraction of these names are provided.  If a time zone needs to be
    // displayed to the user in a given locale, and there is no string data in
    // that locale for that time zone, then the default representation will be
    // shown.  This is a string of the form GMT+HHMM or GMT-HHMM, where HHMM
    // represents the offset in hours and minutes with respect to GMT.  This
    // format is used because it is recognized in all locales.  In order to make
    // this mechanism to work, the root resource data (in the class
    // DateFormatZoneData) is left empty.
    //
    // The current default TimeZone is determined via the system property
    // user.timezone.  This is set by the platform-dependent native code to
    // a three-letter abbreviation.  We interpret these into our own internal
    // IDs using a lookup table.
}

//eof
