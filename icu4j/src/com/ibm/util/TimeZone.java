/*
 * @(#)TimeZone.java	1.51 00/01/19
 *
 * Copyright 1996-2000 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

/*
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package com.ibm.util;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.ibm.text.SimpleDateFormat;
import com.ibm.text.NumberFormat;
import java.text.ParsePosition;
import sun.security.action.GetPropertyAction;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

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
 * @version      1.51 01/19/00
 * @author       Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu
 * @since        JDK1.1
 */
abstract public class TimeZone implements Serializable, Cloneable {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    public TimeZone() {
    }

    /**
     * A style specifier for <code>getDisplayName()</code> indicating
     * a short name, such as "PST."
     * @see #LONG
     * @since 1.2
     */
    public static final int SHORT = 0;

    /**
     * A style specifier for <code>getDisplayName()</code> indicating
     * a long name, such as "Pacific Standard Time."
     * @see #SHORT
     * @since 1.2
     */
    public static final int LONG  = 1;

    // Constants used internally; unit is milliseconds
    private static final int ONE_MINUTE = 60*1000;
    private static final int ONE_HOUR   = 60*ONE_MINUTE;
    private static final int ONE_DAY    = 24*ONE_HOUR;

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
     */
    abstract public int getOffset(int era, int year, int month, int day,
                                  int dayOfWeek, int milliseconds);

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
     */
    abstract public void setRawOffset(int offsetMillis);

    /**
     * Gets unmodified offset, NOT modified in case of daylight savings.
     * This is the offset to add *to* UTC to get local time.
     * @return the unmodified offset to add *to* UTC to get local time.
     */
    abstract public int getRawOffset();

    /**
     * Gets the ID of this time zone.
     * @return the ID of this time zone.
     */
    public String getID()
    {
        return ID;
    }

    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @param ID the new time zone ID.
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
     * @since 1.2
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
     * @since 1.2
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
     * @since 1.2
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
     * @since 1.2
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
     */
    abstract public boolean useDaylightTime();

    /**
     * Queries if the given date is in daylight savings time in
     * this time zone.
     * @param date the given Date.
     * @return true if the given date is in daylight savings time,
     * false, otherwise.
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
     * Gets the available IDs according to the given time zone offset.
     * @param rawOffset the given time zone GMT offset.
     * @return an array of IDs, where the time zone for that ID has
     * the specified GMT offset. For example, "America/Phoenix" and "America/Denver"
     * both have GMT-07:00, but differ in daylight savings behavior.
     */
    public static synchronized String[] getAvailableIDs(int rawOffset) {
	String[] result;
	Vector matched = new Vector();

	/* The array TimeZoneData.zones is no longer sorted by raw offset.
	 * Now scanning through all zone data to match offset.
	 */
	for (int i = 0; i < TimeZoneData.zones.length; ++i) {
	    if (TimeZoneData.zones[i].getRawOffset() == rawOffset)
		matched.add(TimeZoneData.zones[i].getID());
	}
	result = new String[matched.size()];
	matched.toArray(result);

        return result;
    }

    /**
     * Gets all the available IDs supported.
     * @return an array of IDs.
     */
    public static synchronized String[] getAvailableIDs() {
        String[]    resultArray = new String[TimeZoneData.zones.length];
        int         count = 0;
        for (int i = 0; i < TimeZoneData.zones.length; ++i)
            resultArray[count++] = TimeZoneData.zones[i].getID();

        // copy into array of the right size and return
        String[] finalResult = new String[count];
        System.arraycopy(resultArray, 0, finalResult, 0, count);

        return finalResult;
    }
    
    /**
     * Gets the platform defined TimeZone ID.
     **/
    private static native String getSystemTimeZoneID(String javaHome, 
						     String region);

    /**
     * Gets the default <code>TimeZone</code> for this host.
     * The source of the default <code>TimeZone</code> 
     * may vary with implementation.
     * @return a default <code>TimeZone</code>.
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
            if (false) System.out.println("com.ibm.util.TZ.default " + defaultZone);
        }
        return (TimeZone)defaultZone.clone();
    }

    /**
     * Sets the <code>TimeZone</code> that is
     * returned by the <code>getDefault</code> method.  If <code>zone</code>
     * is null, reset the default to the value it had originally when the
     * VM first started.
     * @param zone the new default time zone
     */
    public static synchronized void setDefault(TimeZone zone)
    {
        defaultZone = zone;
        // [icu4j Keep java.util.TimeZone default in sync so java.util.Date
        //        can interoperate with com.ibm.util classes.  This solution
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
     * @since 1.2
     */
    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset() &&
            useDaylightTime() == other.useDaylightTime();
    }

    /**
     * Overrides Cloneable
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
            synchronized (TimeZoneData.class) {
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
    // data of the class java.text.resources.DateFormatZoneData???  where ??? is
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

/**
 * Encapsulates data for international timezones.  This package-private class is for
 * internal use only by TimeZone.  It encapsulates the list of recognized international
 * timezones.  By implementing this as a separate class, the loading and initialization
 * cost for this array is delayed until a TimeZone object is actually created from its ID.
 * This class contains only static variables and static methods; it cannot be instantiated.
 */
class TimeZoneData
{
    static final TimeZone get(String ID) {
        Object o = lookup.get(ID);
        return o == null ? null : (TimeZone)((TimeZone)o).clone(); // [sic]
    }

    // ---------------- BEGIN GENERATED DATA ----------------
    private static final int ONE_HOUR = 60*60*1000;

    static SimpleTimeZone zones[] = {
        // The following data is current as of 1998.
        // Total Unix zones: 343
        // Total Java zones: 289
        // Not all Unix zones become Java zones due to duplication and overlap.
        //----------------------------------------------------------
        new SimpleTimeZone(-11*ONE_HOUR, "Pacific/Niue" /*NUT*/),
        // Pacific/Niue Niue(NU)    -11:00  -   NUT
        //----------------------------------------------------------
        new SimpleTimeZone(-11*ONE_HOUR, "Pacific/Apia" /*WST*/),
        // Pacific/Apia W Samoa(WS) -11:00  -   WST # W Samoa Time
        new SimpleTimeZone(-11*ONE_HOUR, "MIT" /*alias for Pacific/Apia*/),
        //----------------------------------------------------------
        new SimpleTimeZone(-11*ONE_HOUR, "Pacific/Pago_Pago" /*SST*/),
        // Pacific/Pago_Pago    American Samoa(US)  -11:00  -   SST # S=Samoa
        //----------------------------------------------------------
        new SimpleTimeZone(-10*ONE_HOUR, "Pacific/Tahiti" /*TAHT*/),
        // Pacific/Tahiti   French Polynesia(PF)    -10:00  -   TAHT    # Tahiti Time
        //----------------------------------------------------------
        new SimpleTimeZone(-10*ONE_HOUR, "Pacific/Fakaofo" /*TKT*/),
        // Pacific/Fakaofo  Tokelau Is(TK)  -10:00  -   TKT # Tokelau Time
        //----------------------------------------------------------
        new SimpleTimeZone(-10*ONE_HOUR, "Pacific/Honolulu" /*HST*/),
        // Pacific/Honolulu Hawaii(US)  -10:00  -   HST
        new SimpleTimeZone(-10*ONE_HOUR, "HST" /*alias for Pacific/Honolulu*/),
        //----------------------------------------------------------
        new SimpleTimeZone(-10*ONE_HOUR, "America/Adak" /*HA%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule US  1967    max -   Oct lastSun 2:00    0   S
        // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Adak Alaska(US)  -10:00  US  HA%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-10*ONE_HOUR, "Pacific/Rarotonga"),
        // Zone Pacific/Rarotonga	Cook Is(CK)	-10:00	Cook	CK%sT
        //----------------------------------------------------------
        new SimpleTimeZone((int)(-9.5*ONE_HOUR), "Pacific/Marquesas" /*MART*/),
        // Pacific/Marquesas    French Polynesia(PF)    -9:30   -   MART    # Marquesas Time
        //----------------------------------------------------------
        new SimpleTimeZone(-9*ONE_HOUR, "Pacific/Gambier" /*GAMT*/),
        // Pacific/Gambier  French Polynesia(PF)    -9:00   -   GAMT    # Gambier Time
        //----------------------------------------------------------
        new SimpleTimeZone(-9*ONE_HOUR, "America/Anchorage" /*AK%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule US  1967    max -   Oct lastSun 2:00    0   S
        // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Anchorage    Alaska(US)  -9:00   US  AK%sT
        new SimpleTimeZone(-9*ONE_HOUR, "AST" /*alias for America/Anchorage*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone((int)(-8.5*ONE_HOUR), "Pacific/Pitcairn" /*PNT*/),
        // Pacific/Pitcairn Pitcairn(PN)    -8:30   -   PNT # Pitcairn Time
        //----------------------------------------------------------
        new SimpleTimeZone(-8*ONE_HOUR, "America/Vancouver" /*P%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Vanc    1962    max -   Oct lastSun 2:00    0   S
        // Rule Vanc    1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Vancouver    British Columbia(CA)    -8:00   Vanc    P%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-8*ONE_HOUR, "America/Tijuana" /*P%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
        // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
        // America/Tijuana  Mexico(MX)  -8:00   Mexico  P%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-8*ONE_HOUR, "America/Los_Angeles" /*P%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule US  1967    max -   Oct lastSun 2:00    0   S
        // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Los_Angeles  US Pacific time, represented by Los Angeles(US) -8:00   US  P%sT
        new SimpleTimeZone(-8*ONE_HOUR, "PST" /*alias for America/Los_Angeles*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(-7*ONE_HOUR, "America/Dawson_Creek" /*MST*/),
        // America/Dawson_Creek British Columbia(CA)    -7:00   -   MST
        //----------------------------------------------------------
        new SimpleTimeZone(-7*ONE_HOUR, "America/Phoenix" /*MST*/),
        // America/Phoenix  ?(US)   -7:00   -   MST
        new SimpleTimeZone(-7*ONE_HOUR, "PNT" /*alias for America/Phoenix*/),
        //----------------------------------------------------------
        new SimpleTimeZone(-7*ONE_HOUR, "America/Edmonton" /*M%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Edm 1972    max -   Oct lastSun 2:00    0   S
        // Rule Edm 1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Edmonton Alberta(CA) -7:00   Edm M%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-7*ONE_HOUR, "America/Mazatlan" /*M%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
        // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
        // America/Mazatlan Mexico(MX)  -7:00   Mexico  M%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-7*ONE_HOUR, "America/Denver" /*M%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule US  1967    max -   Oct lastSun 2:00    0   S
        // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Denver   US Mountain time, represented by Denver(US) -7:00   US  M%sT
        new SimpleTimeZone(-7*ONE_HOUR, "MST" /*alias for America/Denver*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/Belize" /*C%sT*/),
        // America/Belize   Belize(BZ)  -6:00   -   C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/Regina" /*CST*/),
        // America/Regina   Saskatchewan(CA)    -6:00   -   CST
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "Pacific/Galapagos" /*GALT*/),
        // Pacific/Galapagos    Ecuador(EC) -6:00   -   GALT    # Galapagos Time
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/Guatemala" /*C%sT*/),
        // America/Guatemala    Guatemala(GT)   -6:00   -   C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/Tegucigalpa" /*C%sT*/),
        // America/Tegucigalpa  Honduras(HN)    -6:00   -   C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/El_Salvador" /*C%sT*/),
        // America/El_Salvador  El Salvador(SV) -6:00   -   C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/Costa_Rica" /*C%sT*/),
        // America/Costa_Rica   Costa Rica(CR)  -6:00   -   C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/Winnipeg" /*C%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Winn    1966    max -   Oct lastSun 2:00    0   S
        // Rule Winn    1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Winnipeg Manitoba(CA)    -6:00   Winn    C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "Pacific/Easter" /*EAS%sT*/,
          Calendar.OCTOBER, 9, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR,
          Calendar.MARCH, 9, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule Chile   1969    max -   Oct Sun>=9  0:00    1:00    S
        // Rule Chile   1970    max -   Mar Sun>=9  0:00    0   -
        // Pacific/Easter   Chile(CL)   -6:00   Chile   EAS%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/Mexico_City" /*C%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Mexico  1996    max -   Apr Sun>=1  2:00    1:00    D
        // Rule Mexico  1996    max -   Oct lastSun 2:00    0   S
        // America/Mexico_City  Mexico(MX)  -6:00   Mexico  C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-6*ONE_HOUR, "America/Chicago" /*C%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule US  1967    max -   Oct lastSun 2:00    0   S
        // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Chicago  US Central time, represented by Chicago(US) -6:00   US  C%sT
        new SimpleTimeZone(-6*ONE_HOUR, "CST" /*alias for America/Chicago*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Porto_Acre" /*AST*/),
        // America/Porto_Acre   Brazil(BR)  -5:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Bogota" /*CO%sT*/),
        // America/Bogota   Colombia(CO)    -5:00   -   CO%sT   # Colombia Time
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Guayaquil" /*ECT*/),
        // America/Guayaquil    Ecuador(EC) -5:00   -   ECT # Ecuador Time
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Jamaica" /*EST*/),
        // America/Jamaica  Jamaica(JM) -5:00   -   EST
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Cayman" /*EST*/),
        // America/Cayman   Cayman Is(KY)   -5:00   -   EST
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Managua" /*EST*/),
        // America/Managua  Nicaragua(NI)   -5:00   -   EST
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Panama" /*EST*/),
        // America/Panama   Panama(PA)  -5:00   -   EST
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Lima" /*PE%sT*/),
        // America/Lima Peru(PE)    -5:00   -   PE%sT   # Peru Time
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Indianapolis" /*EST*/),
        // America/Indianapolis Indiana(US) -5:00   -   EST
        new SimpleTimeZone(-5*ONE_HOUR, "IET" /*alias for America/Indianapolis*/),
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Nassau" /*E%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Bahamas 1964    max -   Oct lastSun 2:00    0   S
        // Rule Bahamas 1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Nassau   Bahamas(BS) -5:00   Bahamas E%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Montreal" /*E%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Mont    1957    max -   Oct lastSun 2:00    0   S
        // Rule Mont    1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Montreal Ontario, Quebec(CA) -5:00   Mont    E%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Havana",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 0*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 0*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        // Rule	Cuba	1998	max	-	Mar	lastSun	0:00s	1:00	D
        // Rule	Cuba	1998	max	-	Oct	lastSun	0:00s	0	S
        // Zone America/Havana	Cuba(CU)	-5:00	Cuba	C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Port-au-Prince"),
        // Zone America/Port-au-Prince	Haiti(HT)	-5:00	Haiti	E%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/Grand_Turk" /*E%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule TC  1979    max -   Oct lastSun 0:00    0   S
        // Rule TC  1987    max -   Apr Sun>=1  0:00    1:00    D
        // America/Grand_Turk   Turks and Caicos(TC)    -5:00   TC  E%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-5*ONE_HOUR, "America/New_York" /*E%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule US  1967    max -   Oct lastSun 2:00    0   S
        // Rule US  1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/New_York US Eastern time, represented by New York(US)    -5:00   US  E%sT
        new SimpleTimeZone(-5*ONE_HOUR, "EST" /*alias for America/New_York*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Antigua" /*AST*/),
        // America/Antigua  Antigua and Barbuda(AG) -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Anguilla" /*AST*/),
        // America/Anguilla Anguilla(AI)    -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Curacao" /*AST*/),
        // America/Curacao  Curacao(AN) -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Aruba" /*AST*/),
        // America/Aruba    Aruba(AW)   -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Barbados" /*A%sT*/),
        // America/Barbados Barbados(BB)    -4:00   -   A%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/La_Paz" /*BOT*/),
        // America/La_Paz   Bolivia(BO) -4:00   -   BOT # Bolivia Time
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Manaus" /*WST*/),
        // America/Manaus   Brazil(BR)  -4:00   -   WST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Dominica" /*AST*/),
        // America/Dominica Dominica(DM)    -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Santo_Domingo" /*AST*/),
        // America/Santo_Domingo    Dominican Republic(DO)  -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Grenada" /*AST*/),
        // America/Grenada  Grenada(GD) -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Guadeloupe" /*AST*/),
        // America/Guadeloupe   Guadeloupe(GP)  -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Guyana" /*GYT*/),
        // America/Guyana   Guyana(GY)  -4:00   -   GYT
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/St_Kitts" /*AST*/),
        // America/St_Kitts St Kitts-Nevis(KN)  -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/St_Lucia" /*AST*/),
        // America/St_Lucia St Lucia(LC)    -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Martinique" /*AST*/),
        // America/Martinique   Martinique(MQ)  -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Montserrat" /*AST*/),
        // America/Montserrat   Montserrat(MS)  -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Puerto_Rico" /*AST*/),
        // America/Puerto_Rico  Puerto Rico(PR) -4:00   -   AST
        new SimpleTimeZone(-4*ONE_HOUR, "PRT" /*alias for America/Puerto_Rico*/),
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Port_of_Spain" /*AST*/),
        // America/Port_of_Spain    Trinidad and Tobago(TT) -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/St_Vincent" /*AST*/),
        // America/St_Vincent   St Vincent and the Grenadines(VC)   -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Tortola" /*AST*/),
        // America/Tortola  British Virgin Is(VG)   -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/St_Thomas" /*AST*/),
        // America/St_Thomas    Virgin Is(VI)   -4:00   -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Caracas" /*VET*/),
        // America/Caracas  Venezuela(VE)   -4:00   -   VET
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "Antarctica/Palmer" /*CL%sT*/,
          Calendar.OCTOBER, 9, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR,
          Calendar.MARCH, 9, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule ChileAQ 1969    max -   Oct Sun>=9  0:00    1:00    S
        // Rule ChileAQ 1970    max -   Mar Sun>=9  0:00    0   -
        // Antarctica/Palmer    USA - year-round bases(AQ)  -4:00   ChileAQ CL%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "Atlantic/Bermuda" /*A%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Bahamas 1964    max -   Oct lastSun 2:00    0   S
        // Rule Bahamas 1987    max -   Apr Sun>=1  2:00    1:00    D
        // Atlantic/Bermuda Bermuda(BM) -4:00   Bahamas A%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Cuiaba"),
        // Zone America/Cuiaba	Brazil(BR)	-4:00	-	WST
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Halifax" /*A%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Halifax 1962    max -   Oct lastSun 2:00    0   S
        // Rule Halifax 1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Halifax  ?(CA)   -4:00   Halifax A%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "Atlantic/Stanley" /*FK%sT*/,
          Calendar.SEPTEMBER, 8, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR,
          Calendar.APRIL, 16, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule Falk    1986    max -   Apr Sun>=16 0:00    0   -
        // Rule Falk    1996    max -   Sep Sun>=8  0:00    1:00    S
        // Atlantic/Stanley Falklands(FK)   -4:00   Falk    FK%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Thule" /*A%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Thule   1993    max -   Apr Sun>=1  2:00    1:00    D
        // Rule Thule   1993    max -   Oct lastSun 2:00    0   S
        // America/Thule    ?(GL)   -4:00   Thule   A%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Asuncion",
          Calendar.OCTOBER, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.FEBRUARY, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule	Para	1996	max	-	Oct	Sun>=1	0:00	1:00	S
        // Rule	Para	1999	max	-	Feb	lastSun	0:00	0	-
        // Zone America/Asuncion	Paraguay(PY)	-4:00	Para	PY%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-4*ONE_HOUR, "America/Santiago" /*CL%sT*/,
          Calendar.OCTOBER, 9, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR,
          Calendar.MARCH, 9, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule Chile   1969    max -   Oct Sun>=9  0:00    1:00    S
        // Rule Chile   1970    max -   Mar Sun>=9  0:00    0   -
        // America/Santiago Chile(CL)   -4:00   Chile   CL%sT
        //----------------------------------------------------------
        new SimpleTimeZone((int)(-3.5*ONE_HOUR), "America/St_Johns" /*N%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule StJohns 1960    max -   Oct lastSun 2:00    0   S
        // Rule StJohns 1989    max -   Apr Sun>=1  2:00    1:00    D
        // America/St_Johns Canada(CA)  -3:30   StJohns N%sT
        new SimpleTimeZone((int)(-3.5*ONE_HOUR), "CNT" /*alias for America/St_Johns*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(-3*ONE_HOUR, "America/Fortaleza" /*EST*/),
        // America/Fortaleza    Brazil(BR)  -3:00   -   EST
        //----------------------------------------------------------
        new SimpleTimeZone(-3*ONE_HOUR, "America/Cayenne" /*GFT*/),
        // America/Cayenne  French Guiana(GF)   -3:00   -   GFT
        //----------------------------------------------------------
        new SimpleTimeZone(-3*ONE_HOUR, "America/Paramaribo" /*SRT*/),
        // America/Paramaribo   Suriname(SR)    -3:00   -   SRT
        //----------------------------------------------------------
        new SimpleTimeZone(-3*ONE_HOUR, "America/Montevideo" /*UY%sT*/),
        // America/Montevideo   Uruguay(UY) -3:00   -   UY%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-3*ONE_HOUR, "America/Buenos_Aires" /*AR%sT*/),
        // America/Buenos_Aires Argentina(AR)   -3:00   -   AR%sT
        new SimpleTimeZone(-3*ONE_HOUR, "AGT" /*alias for America/Buenos_Aires*/),
        //----------------------------------------------------------
        new SimpleTimeZone(-3*ONE_HOUR, "America/Godthab",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone America/Godthab	?(GL)	-3:00	EU	WG%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-3*ONE_HOUR, "America/Miquelon" /*PM%sT*/,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Mont    1957    max -   Oct lastSun 2:00    0   S
        // Rule Mont    1987    max -   Apr Sun>=1  2:00    1:00    D
        // America/Miquelon St Pierre and Miquelon(PM)  -3:00   Mont    PM%sT   # Pierre & Miquelon Time
        //----------------------------------------------------------
        new SimpleTimeZone(-3*ONE_HOUR, "America/Sao_Paulo",
          Calendar.OCTOBER, 8, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.FEBRUARY, 15, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule	Brazil	1998	max	-	Oct	Sun>=8	 0:00	1:00	D
        // Rule	Brazil	1999	max	-	Feb	Sun>=15	 0:00	0	S
        // Zone America/Sao_Paulo	Brazil(BR)	-3:00	Brazil	E%sT
        new SimpleTimeZone(-3*ONE_HOUR, "BET" /*alias for America/Sao_Paulo*/,
          Calendar.OCTOBER, 8, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.FEBRUARY, 15, -Calendar.SUNDAY /*DOW>=DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(-2*ONE_HOUR, "America/Noronha" /*FST*/),
        // America/Noronha  Brazil(BR)  -2:00   -   FST
        //----------------------------------------------------------
        new SimpleTimeZone(-2*ONE_HOUR, "Atlantic/South_Georgia" /*GST*/),
        // Atlantic/South_Georgia   South Georgia(GS)   -2:00   -   GST # South Georgia Time
        //----------------------------------------------------------
        new SimpleTimeZone(-1*ONE_HOUR, "Atlantic/Jan_Mayen" /*EGT*/),
        // Atlantic/Jan_Mayen   ?(NO)   -1:00   -   EGT
        //----------------------------------------------------------
        new SimpleTimeZone(-1*ONE_HOUR, "Atlantic/Cape_Verde" /*CVT*/),
        // Atlantic/Cape_Verde  Cape Verde(CV)  -1:00   -   CVT
        //----------------------------------------------------------
        new SimpleTimeZone(-1*ONE_HOUR, "America/Scoresbysund",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone America/Scoresbysund	?(GL)	-1:00	EU	EG%sT
        //----------------------------------------------------------
        new SimpleTimeZone(-1*ONE_HOUR, "Atlantic/Azores",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Atlantic/Azores	Portugal(PT)	-1:00	EU	AZO%sT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Ouagadougou" /*GMT*/),
        // Africa/Ouagadougou   Burkina Faso(BF)    0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Abidjan" /*GMT*/),
        // Africa/Abidjan   Cote D'Ivoire(CI)   0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Accra" /*%s*/),
        // Africa/Accra Ghana(GH)   0:00    -   %s
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Banjul" /*GMT*/),
        // Africa/Banjul    Gambia(GM)  0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Conakry" /*GMT*/),
        // Africa/Conakry   Guinea(GN)  0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Bissau" /*GMT*/),
        // Africa/Bissau    Guinea-Bissau(GW)   0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Atlantic/Reykjavik" /*GMT*/),
        // Atlantic/Reykjavik   Iceland(IS) 0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Monrovia" /*GMT*/),
        // Africa/Monrovia  Liberia(LR) 0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Casablanca" /*WET*/),
        // Africa/Casablanca    Morocco(MA) 0:00    -   WET
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Timbuktu" /*GMT*/),
        // Africa/Timbuktu  Mali(ML)    0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Nouakchott" /*GMT*/),
        // Africa/Nouakchott    Mauritania(MR)  0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Atlantic/St_Helena" /*GMT*/),
        // Atlantic/St_Helena   St Helena(SH)   0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Freetown" /*%s*/),
        // Africa/Freetown  Sierra Leone(SL)    0:00    -   %s
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Dakar" /*GMT*/),
        // Africa/Dakar Senegal(SN) 0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Sao_Tome" /*GMT*/),
        // Africa/Sao_Tome  Sao Tome and Principe(ST)   0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Africa/Lome" /*GMT*/),
        // Africa/Lome  Togo(TG)    0:00    -   GMT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "GMT" /*GMT*/),
        // GMT  -(-)    0:00    -   GMT
        new SimpleTimeZone(0*ONE_HOUR, "UTC" /*alias for GMT*/),
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Atlantic/Faeroe",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Atlantic/Faeroe	Denmark, Faeroe Islands, and Greenland(DK)	0:00	EU	WE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Atlantic/Canary",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Atlantic/Canary	Spain(ES)	0:00	EU	WE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Europe/Dublin",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Dublin	---(IE)	0:00	EU	GMT/IST
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Europe/Lisbon",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Lisbon	Portugal(PT)	0:00	EU	WE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(0*ONE_HOUR, "Europe/London",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/London	---(GB)	0:00	EU	GMT/BST
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Luanda" /*WAT*/),
        // Africa/Luanda    Angola(AO)  1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Porto-Novo" /*WAT*/),
        // Africa/Porto-Novo    Benin(BJ)   1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Bangui" /*WAT*/),
        // Africa/Bangui    Central African Republic(CF)    1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Kinshasa" /*WAT*/),
        // Africa/Kinshasa  Democratic Republic of Congo(CG)    1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Douala" /*WAT*/),
        // Africa/Douala    Cameroon(CM)    1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Libreville" /*WAT*/),
        // Africa/Libreville    Gabon(GA)   1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Malabo" /*WAT*/),
        // Africa/Malabo    Equatorial Guinea(GQ)   1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Niamey" /*WAT*/),
        // Africa/Niamey    Niger(NE)   1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Lagos" /*WAT*/),
        // Africa/Lagos Nigeria(NG) 1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Ndjamena" /*WAT*/),
        // Africa/Ndjamena  Chad(TD)    1:00    -   WAT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Tunis" /*CE%sT*/),
        // Africa/Tunis Tunisia(TN) 1:00    -   CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Algiers" /*CET*/),
        // Africa/Algiers   Algeria(DZ) 1:00    -   CET
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Andorra",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Andorra	Andorra(AD)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Tirane",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Tirane	Albania(AL)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Vienna",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Vienna	Austria(AT)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Brussels",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Brussels	Belgium(BE)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Zurich",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Zurich	Switzerland(CH)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Prague",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Prague	Czech Republic(CZ)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Berlin",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Berlin	Germany(DE)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Copenhagen",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Copenhagen	Denmark, Faeroe Islands, and Greenland(DK)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Madrid",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Madrid	Spain(ES)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Gibraltar",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Gibraltar	Gibraltar(GI)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Budapest",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Budapest	Hungary(HU)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Rome",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Rome	Italy(IT)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Vaduz",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Vaduz	Liechtenstein(LI)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Luxembourg",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Luxembourg	Luxembourg(LU)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Tripoli"),
        // Zone Africa/Tripoli	Libya(LY)	2:00	-	EET
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Monaco",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Monaco	Monaco(MC)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Malta",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Malta	Malta(MT)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Africa/Windhoek" /*WA%sT*/,
          Calendar.SEPTEMBER, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.APRIL, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR, 1*ONE_HOUR),
        // Rule Namibia 1994    max -   Sep Sun>=1  2:00    1:00    S
        // Rule Namibia 1995    max -   Apr Sun>=1  2:00    0   -
        // Africa/Windhoek  Namibia(NA) 1:00    Namibia WA%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Amsterdam",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Amsterdam	Netherlands(NL)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Oslo",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Oslo	Norway(NO)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Warsaw",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule EU      1981    max     -       Mar     lastSun  1:00u  1:00   S
        // Rule EU      1996    max     -       Oct     lastSun  1:00u  0      -
        // Zone Europe/Warsaw   1:00    EU      CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Stockholm",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Stockholm	Sweden(SE)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Belgrade",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Belgrade	Yugoslavia(YU)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Paris",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Paris	France(FR)	1:00	EU	CE%sT
        new SimpleTimeZone(1*ONE_HOUR, "ECT" /*alias for Europe/Paris*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Bujumbura" /*CAT*/),
        // Africa/Bujumbura Burundi(BI) 2:00    -   CAT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Gaborone" /*CAT*/),
        // Africa/Gaborone  Botswana(BW)    2:00    -   CAT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Lubumbashi" /*CAT*/),
        // Africa/Lubumbashi    Democratic Republic of Congo(CG)    2:00    -   CAT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Maseru" /*SAST*/),
        // Africa/Maseru    Lesotho(LS) 2:00    -   SAST
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Blantyre" /*CAT*/),
        // Africa/Blantyre  Malawi(ML)  2:00    -   CAT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Maputo" /*CAT*/),
        // Africa/Maputo    Mozambique(MZ)  2:00    -   CAT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Kigali" /*CAT*/),
        // Africa/Kigali    Rwanda(RW)  2:00    -   CAT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Khartoum" /*CA%sT*/),
        // Africa/Khartoum  Sudan(SD)   2:00    -   CA%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Mbabane" /*SAST*/),
        // Africa/Mbabane   Swaziland(SZ)   2:00    -   SAST
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Lusaka" /*CAT*/),
        // Africa/Lusaka    Zambia(ZM)  2:00    -   CAT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Harare" /*CAT*/),
        // Africa/Harare    Zimbabwe(ZW)    2:00    -   CAT
        new SimpleTimeZone(2*ONE_HOUR, "CAT" /*alias for Africa/Harare*/),
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Johannesburg" /*SAST*/),
        // Africa/Johannesburg  South Africa(ZA)    2:00    -   SAST
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Sofia" /*EE%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
        // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
        // Europe/Sofia Bulgaria(BG)    2:00    E-Eur   EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Minsk" /*EE%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Europe/Minsk Belarus(BY) 2:00    Russia  EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Asia/Nicosia",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EUAsia	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EUAsia	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Asia/Nicosia	Cyprus(CY)	2:00	EUAsia	EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Tallinn",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Tallinn	Estonia(EE)	2:00	EU	EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Africa/Cairo",
          Calendar.APRIL, 22, -Calendar.FRIDAY /*DOW>=DOM*/, 0*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.SEPTEMBER, -1, Calendar.THURSDAY /*DOW_IN_MON*/, 23*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        // Rule	Egypt	1995	max	-	Apr	Fri>=22	 0:00s	1:00	S
        // Rule	Egypt	1995	max	-	Sep	lastThu	23:00s	0	-
        // Zone Africa/Cairo	Egypt(EG)	2:00	Egypt	EE%sT
        new SimpleTimeZone(2*ONE_HOUR, "ART" /*alias for Africa/Cairo*/,
          Calendar.APRIL, 22, -Calendar.FRIDAY /*DOW>=DOM*/, 0*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.SEPTEMBER, -1, Calendar.THURSDAY /*DOW_IN_MON*/, 23*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Helsinki",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Helsinki	Finland(FI)	2:00	EU	EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Athens",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Athens	Greece(GR)	2:00	EU	EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Asia/Jerusalem",
          Calendar.APRIL, 1, -Calendar.FRIDAY /*DOW>=DOM*/, 2*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.SEPTEMBER, 1, -Calendar.FRIDAY /*DOW>=DOM*/, 2*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule	Zion	2000	max	-	Apr	Fri>=1	2:00	1:00	D
        // Rule	Zion	2000	max	-	Sep	Fri>=1	2:00	0	S
        // Zone Asia/Jerusalem	Israel(IL)	2:00	Zion	I%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Asia/Amman"),
        // Zone Asia/Amman      2:00    Jordan  EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Asia/Beirut" /*EE%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR,
          Calendar.SEPTEMBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule Lebanon 1993    max -   Mar lastSun 0:00    1:00    S
        // Rule Lebanon 1993    max -   Sep lastSun 0:00    0   -
        // Asia/Beirut  Lebanon(LB) 2:00    Lebanon EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(1*ONE_HOUR, "Europe/Vilnius",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Vilnius	Lithuania(LT)	1:00	EU	CE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Riga",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Riga	Latvia(LV)	2:00	EU	EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Chisinau" /*EE%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
        // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
        // Europe/Chisinau  Moldova(MD) 2:00    E-Eur   EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Bucharest" /*EE%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule E-Eur   1981    max -   Mar lastSun 0:00    1:00    S
        // Rule E-Eur   1996    max -   Oct lastSun 0:00    0   -
        // Europe/Bucharest Romania(RO) 2:00    E-Eur   EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Kaliningrad" /*EE%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Europe/Kaliningrad   Russia(RU)  2:00    Russia  EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Asia/Damascus" /*EE%sT*/,
          Calendar.APRIL, 1, 0 /*DOM*/, 0*ONE_HOUR,
          Calendar.OCTOBER, 1, 0 /*DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule Syria   1994    max -   Apr 1   0:00    1:00    S
        // Rule Syria   1994    max -   Oct 1   0:00    0   -
        // Asia/Damascus    Syria(SY)   2:00    Syria   EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Kiev",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Kiev	Ukraine(UA)	2:00	EU	EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Istanbul",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Istanbul	Turkey(TR)	2:00	EU	EE%sT
        new SimpleTimeZone(2*ONE_HOUR, "EET" /*alias for Europe/Istanbul*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Asia/Bahrain" /*AST*/),
        // Asia/Bahrain Bahrain(BH) 3:00    -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Africa/Djibouti" /*EAT*/),
        // Africa/Djibouti  Djibouti(DJ)    3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Africa/Asmera" /*EAT*/),
        // Africa/Asmera    Eritrea(ER) 3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Africa/Addis_Ababa" /*EAT*/),
        // Africa/Addis_Ababa   Ethiopia(ET)    3:00    -   EAT
        new SimpleTimeZone(3*ONE_HOUR, "EAT" /*alias for Africa/Addis_Ababa*/),
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Africa/Nairobi" /*EAT*/),
        // Africa/Nairobi   Kenya(KE)   3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Indian/Comoro" /*EAT*/),
        // Indian/Comoro    Comoros(KM) 3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Asia/Kuwait" /*AST*/),
        // Asia/Kuwait  Kuwait(KW)  3:00    -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Indian/Antananarivo" /*EAT*/),
        // Indian/Antananarivo  Madagascar(MK)  3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Asia/Qatar" /*AST*/),
        // Asia/Qatar   Qatar(QA)   3:00    -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Africa/Mogadishu" /*EAT*/),
        // Africa/Mogadishu Somalia(SO) 3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Africa/Dar_es_Salaam" /*EAT*/),
        // Africa/Dar_es_Salaam Tanzania(TZ)    3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Africa/Kampala" /*EAT*/),
        // Africa/Kampala   Uganda(UG)  3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Asia/Aden" /*AST*/),
        // Asia/Aden    Yemen(YE)   3:00    -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Indian/Mayotte" /*EAT*/),
        // Indian/Mayotte   Mayotte(YT) 3:00    -   EAT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Asia/Riyadh" /*AST*/),
        // Asia/Riyadh  Saudi Arabia(SA)    3:00    -   AST
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Asia/Baghdad" /*A%sT*/,
          Calendar.APRIL, 1, 0 /*DOM*/, 3*ONE_HOUR,
          Calendar.OCTOBER, 1, 0 /*DOM*/, 4*ONE_HOUR, 1*ONE_HOUR),
        // Rule Iraq    1991    max -   Apr 1   3:00s   1:00    D
        // Rule Iraq    1991    max -   Oct 1   3:00s   0   D
        // Asia/Baghdad Iraq(IQ)    3:00    Iraq    A%sT
        //----------------------------------------------------------
        new SimpleTimeZone(2*ONE_HOUR, "Europe/Simferopol",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.UTC_TIME, 1*ONE_HOUR),
        // Rule	EU	1981	max	-	Mar	lastSun	 1:00u	1:00	S
        // Rule	EU	1996	max	-	Oct	lastSun	 1:00u	0	-
        // Zone Europe/Simferopol	Ukraine(UA)	2:00	EU	EE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(3*ONE_HOUR, "Europe/Moscow" /*MSK/MSD*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Europe/Moscow    Russia(RU)  3:00    Russia  MSK/MSD
        //----------------------------------------------------------
        new SimpleTimeZone((int)(3.5*ONE_HOUR), "Asia/Tehran",
          Calendar.MARCH, 20, 0 /*DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.SEPTEMBER, 22, 0 /*DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule	Iran	2000	only	-	Mar	20	0:00	1:00	S
        // Rule	Iran	2000	only	-	Sep	22	0:00	0	-
        // Zone Asia/Tehran	Iran(IR)	3:30	Iran	IR%sT
        new SimpleTimeZone((int)(3.5*ONE_HOUR), "MET" /*alias for Asia/Tehran*/,
          Calendar.MARCH, 20, 0 /*DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.SEPTEMBER, 22, 0 /*DOM*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Asia/Dubai" /*GST*/),
        // Asia/Dubai   United Arab Emirates(AE)    4:00    -   GST
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Indian/Mauritius" /*MUT*/),
        // Indian/Mauritius Mauritius(MU)   4:00    -   MUT # Mauritius Time
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Asia/Muscat" /*GST*/),
        // Asia/Muscat  Oman(OM)    4:00    -   GST
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Indian/Reunion" /*RET*/),
        // Indian/Reunion   Reunion(RE) 4:00    -   RET # Reunion Time
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Indian/Mahe" /*SCT*/),
        // Indian/Mahe  Seychelles(SC)  4:00    -   SCT # Seychelles Time
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Asia/Yerevan",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        // Rule RussiaAsia	1993	max	-	Mar	lastSun	 2:00s	1:00	S
        // Rule RussiaAsia	1996	max	-	Oct	lastSun	 2:00s	0	-
        // Zone Asia/Yerevan	Armenia(AM)	4:00 RussiaAsia	AM%sT			
        new SimpleTimeZone(4*ONE_HOUR, "NET" /*alias for Asia/Yerevan*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Asia/Baku",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 1*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule	Azer	1997	max	-	Mar	lastSun	 1:00	1:00	S
        // Rule	Azer	1997	max	-	Oct	lastSun	 1:00	0	-
        // Zone Asia/Baku	Azerbaijan(AZ)	4:00	Azer	AZ%sT
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Asia/Aqtau" /*AQT%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule E-EurAsia   1981    max -   Mar lastSun 0:00    1:00    S
        // Rule E-EurAsia   1996    max -   Oct lastSun 0:00    0   -
        // Asia/Aqtau   Kazakhstan(KZ)  4:00    E-EurAsia   AQT%sT
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Europe/Samara" /*SAM%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Europe/Samara    Russia(RU)  4:00    Russia  SAM%sT
        //----------------------------------------------------------
        new SimpleTimeZone((int)(4.5*ONE_HOUR), "Asia/Kabul" /*AFT*/),
        // Asia/Kabul   Afghanistan(AF) 4:30    -   AFT
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Indian/Kerguelen" /*TFT*/),
        // Indian/Kerguelen France - year-round bases(FR)   5:00    -   TFT # ISO code TF Time
        //----------------------------------------------------------
        new SimpleTimeZone(4*ONE_HOUR, "Asia/Tbilisi",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule E-EurAsia	1981	max	-	Mar	lastSun	 0:00	1:00	S
        // Rule E-EurAsia	1996	max	-	Oct	lastSun	 0:00	0	-
        // Zone Asia/Tbilisi	Georgia(GE)	4:00 E-EurAsia	GE%sT
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Indian/Chagos" /*IOT*/),
        // Indian/Chagos    British Indian Ocean Territory(IO)  5:00    -   IOT # BIOT Time
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Indian/Maldives" /*MVT*/),
        // Indian/Maldives  Maldives(MV)    5:00    -   MVT # Maldives Time
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Asia/Dushanbe" /*TJT*/),
        // Asia/Dushanbe    Tajikistan(TJ)  5:00    -   TJT # Tajikistan Time
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Asia/Ashkhabad" /*TMT*/),
        // Asia/Ashkhabad   Turkmenistan(TM)    5:00    -   TMT # Turkmenistan Time
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Asia/Tashkent" /*UZT*/),
        // Asia/Tashkent    Uzbekistan(UZ)  5:00    -   UZT # Uzbekistan Time
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Asia/Karachi" /*PKT*/),
        // Asia/Karachi Pakistan(PK)    5:00    -   PKT # Pakistan Time
        new SimpleTimeZone(5*ONE_HOUR, "PLT" /*alias for Asia/Karachi*/),
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Asia/Bishkek",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, (int)(2.5*ONE_HOUR), SimpleTimeZone.WALL_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, (int)(2.5*ONE_HOUR), SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule	Kirgiz	1997	max	-	Mar	lastSun	2:30	1:00	S
        // Rule	Kirgiz	1997	max	-	Oct	lastSun	2:30	0	-
        // Zone Asia/Bishkek	Kirgizstan(KG)	5:00	Kirgiz	KG%sT		    
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Asia/Aqtobe" /*AQT%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule E-EurAsia   1981    max -   Mar lastSun 0:00    1:00    S
        // Rule E-EurAsia   1996    max -   Oct lastSun 0:00    0   -
        // Asia/Aqtobe  Kazakhstan(KZ)  5:00    E-EurAsia   AQT%sT
        //----------------------------------------------------------
        new SimpleTimeZone(5*ONE_HOUR, "Asia/Yekaterinburg" /*YEK%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Yekaterinburg   Russia(RU)  5:00    Russia  YEK%sT  # Yekaterinburg Time
        //----------------------------------------------------------
        new SimpleTimeZone((int)(5.5*ONE_HOUR), "Asia/Calcutta" /*IST*/),
        // Asia/Calcutta    India(IN)   5:30    -   IST
        new SimpleTimeZone((int)(5.5*ONE_HOUR), "IST" /*alias for Asia/Calcutta*/),
        //----------------------------------------------------------
        new SimpleTimeZone((int)(5.75*ONE_HOUR), "Asia/Katmandu" /*NPT*/),
        // Asia/Katmandu    Nepal(NP)   5:45    -   NPT # Nepal Time
        //----------------------------------------------------------
        new SimpleTimeZone(6*ONE_HOUR, "Antarctica/Mawson" /*MAWT*/),
        // Antarctica/Mawson    Australia - territories(AQ) 6:00    -   MAWT    # Mawson Time
        //----------------------------------------------------------
        new SimpleTimeZone(6*ONE_HOUR, "Asia/Thimbu" /*BTT*/),
        // Asia/Thimbu  Bhutan(BT)  6:00    -   BTT # Bhutan Time
        //----------------------------------------------------------
        new SimpleTimeZone(6*ONE_HOUR, "Asia/Colombo" /*LKT*/),
        // Asia/Colombo Sri Lanka(LK)   6:00    -   LKT
        //----------------------------------------------------------
        new SimpleTimeZone(6*ONE_HOUR, "Asia/Dacca" /*BDT*/),
        // Asia/Dacca   Bangladesh(BD)  6:00    -   BDT # Bangladesh Time
        new SimpleTimeZone(6*ONE_HOUR, "BST" /*alias for Asia/Dacca*/),
        //----------------------------------------------------------
        new SimpleTimeZone(6*ONE_HOUR, "Asia/Almaty",
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 0*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule E-EurAsia	1981	max	-	Mar	lastSun	 0:00	1:00	S
        // Rule E-EurAsia	1996	max	-	Oct	lastSun	 0:00	0	-
        // Zone Asia/Almaty	6:00	E-EurAsia	ALM%sT
        //----------------------------------------------------------
        new SimpleTimeZone(6*ONE_HOUR, "Asia/Novosibirsk" /*NOV%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Novosibirsk Russia(RU)  6:00    Russia  NOV%sT
        //----------------------------------------------------------
        new SimpleTimeZone((int)(6.5*ONE_HOUR), "Indian/Cocos" /*CCT*/),
        // Indian/Cocos Cocos(CC)   6:30    -   CCT # Cocos Islands Time
        //----------------------------------------------------------
        new SimpleTimeZone((int)(6.5*ONE_HOUR), "Asia/Rangoon" /*MMT*/),
        // Asia/Rangoon Burma / Myanmar(MM) 6:30    -   MMT # Myanmar Time
        //----------------------------------------------------------
        new SimpleTimeZone(7*ONE_HOUR, "Indian/Christmas" /*CXT*/),
        // Indian/Christmas Australian miscellany(AU)   7:00    -   CXT # Christmas Island Time
        //----------------------------------------------------------
        new SimpleTimeZone(7*ONE_HOUR, "Asia/Jakarta" /*JAVT*/),
        // Asia/Jakarta Indonesia(ID)   7:00    -   JAVT
        //----------------------------------------------------------
        new SimpleTimeZone(7*ONE_HOUR, "Asia/Phnom_Penh" /*ICT*/),
        // Asia/Phnom_Penh  Cambodia(KH)    7:00    -   ICT
        //----------------------------------------------------------
        new SimpleTimeZone(7*ONE_HOUR, "Asia/Vientiane" /*ICT*/),
        // Asia/Vientiane   Laos(LA)    7:00    -   ICT
        //----------------------------------------------------------
        new SimpleTimeZone(7*ONE_HOUR, "Asia/Saigon" /*ICT*/),
        // Asia/Saigon  Vietnam(VN) 7:00    -   ICT
        new SimpleTimeZone(7*ONE_HOUR, "VST" /*alias for Asia/Saigon*/),
        //----------------------------------------------------------
        new SimpleTimeZone(7*ONE_HOUR, "Asia/Bangkok" /*ICT*/),
        // Asia/Bangkok Thailand(TH)    7:00    -   ICT
        //----------------------------------------------------------
        new SimpleTimeZone(7*ONE_HOUR, "Asia/Krasnoyarsk" /*KRA%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Krasnoyarsk Russia(RU)  7:00    Russia  KRA%sT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Antarctica/Casey" /*WST*/),
        // Antarctica/Casey Australia - territories(AQ) 8:00    -   WST # Western (Aus) Standard Time
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Australia/Perth" /*WST*/),
        // Australia/Perth  Australia(AU)   8:00    -   WST
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Brunei" /*BNT*/),
        // Asia/Brunei  Brunei(BN)  8:00    -   BNT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Hong_Kong" /*C%sT*/),
        // Asia/Hong_Kong   China(HK)   8:00    -   C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Ujung_Pandang" /*BORT*/),
        // Asia/Ujung_Pandang   Indonesia(ID)   8:00    -   BORT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Macao" /*C%sT*/),
        // Asia/Macao   Macao(MO)   8:00    -   C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Kuala_Lumpur" /*MYT*/),
        // Asia/Kuala_Lumpur    Malaysia(MY)    8:00    -   MYT # Malaysia Time
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Manila" /*PH%sT*/),
        // Asia/Manila  Philippines(PH) 8:00    -   PH%sT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Singapore" /*SGT*/),
        // Asia/Singapore   Singapore(SG)   8:00    -   SGT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Taipei" /*C%sT*/),
        // Asia/Taipei  Taiwan(TW)  8:00    -   C%sT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Shanghai" /*C%sT*/),
        // Asia/Shanghai    China(CN)   8:00    -   C%sT
        new SimpleTimeZone(8*ONE_HOUR, "CTT" /*alias for Asia/Shanghai*/),
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Ulan_Bator" /*ULA%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR,
          Calendar.SEPTEMBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 0*ONE_HOUR, 1*ONE_HOUR),
        // Rule Mongol  1991    max -   Mar lastSun 0:00    1:00    S
        // Rule Mongol  1997    max -   Sep lastSun 0:00    0   -
        // Asia/Ulan_Bator  Mongolia(MN)    8:00    Mongol  ULA%sT
        //----------------------------------------------------------
        new SimpleTimeZone(8*ONE_HOUR, "Asia/Irkutsk" /*IRK%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Irkutsk Russia(RU)  8:00    Russia  IRK%sT
        //----------------------------------------------------------
        new SimpleTimeZone(9*ONE_HOUR, "Asia/Jayapura" /*JAYT*/),
        // Asia/Jayapura    Indonesia(ID)   9:00    -   JAYT
        //----------------------------------------------------------
        new SimpleTimeZone(9*ONE_HOUR, "Asia/Pyongyang" /*KST*/),
        // Asia/Pyongyang   ?(KP)   9:00    -   KST
        //----------------------------------------------------------
        new SimpleTimeZone(9*ONE_HOUR, "Asia/Seoul" /*K%sT*/),
        // Asia/Seoul   ?(KR)   9:00    -   K%sT
        //----------------------------------------------------------
        new SimpleTimeZone(9*ONE_HOUR, "Pacific/Palau" /*PWT*/),
        // Pacific/Palau    Palau(PW)   9:00    -   PWT # Palau Time
        //----------------------------------------------------------
        new SimpleTimeZone(9*ONE_HOUR, "Asia/Tokyo" /*JST*/),
        // Asia/Tokyo   Japan(JP)   9:00    -   JST
        new SimpleTimeZone(9*ONE_HOUR, "JST" /*alias for Asia/Tokyo*/),
        //----------------------------------------------------------
        new SimpleTimeZone(9*ONE_HOUR, "Asia/Yakutsk" /*YAK%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Yakutsk Russia(RU)  9:00    Russia  YAK%sT
        //----------------------------------------------------------
        new SimpleTimeZone((int)(9.5*ONE_HOUR), "Australia/Darwin" /*CST*/),
        // Australia/Darwin Australia(AU)   9:30    -   CST
        new SimpleTimeZone((int)(9.5*ONE_HOUR), "ACT" /*alias for Australia/Darwin*/),
        //----------------------------------------------------------
        new SimpleTimeZone((int)(9.5*ONE_HOUR), "Australia/Adelaide",
          Calendar.AUGUST, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        // Rule AS      2000    only    -       Aug     lastSun 2:00s   1:00   -
        // Rule AS      1995    max     -       Mar     lastSun 2:00s   0      -
        // Zone Australia/Adelaide      9:30    AS      CST
        //----------------------------------------------------------
        new SimpleTimeZone((int)(9.5*ONE_HOUR), "Australia/Broken_Hill",
          Calendar.AUGUST, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        // Rule	AN	2000	only	-	Aug	lastSun	2:00s	1:00	-
        // Rule	AN	1996	max	-	Mar	lastSun	2:00s	0	-
        // Zone Australia/Broken_Hill	9:30	AN	CST
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Australia/Hobart",
          Calendar.OCTOBER, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        // Rule	AT	1991	max	-	Oct	Sun>=1	2:00s	1:00	-
        // Rule	AT	1991	max	-	Mar	lastSun	2:00s	0	-
        // Australia/Hobart 10:00	AT	EST
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Antarctica/DumontDUrville" /*DDUT*/),
        // Antarctica/DumontDUrville    France - year-round bases(AQ)   10:00   -   DDUT    # Dumont-d'Urville Time
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Pacific/Truk" /*TRUT*/),
        // Pacific/Truk Micronesia(FM)  10:00   -   TRUT    # Truk Time
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Pacific/Guam" /*GST*/),
        // Pacific/Guam Guam(GU)    10:00   -   GST
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Pacific/Saipan" /*MPT*/),
        // Pacific/Saipan   N Mariana Is(MP)    10:00   -   MPT
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Pacific/Port_Moresby" /*PGT*/),
        // Pacific/Port_Moresby Papua New Guinea(PG)    10:00   -   PGT # Papua New Guinea Time
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Australia/Brisbane" /*EST*/),
        // Australia/Brisbane   Australia(AU)   10:00   -   EST
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Asia/Vladivostok" /*VLA%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Vladivostok Russia(RU)  10:00   Russia  VLA%sT
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "Australia/Sydney",
          Calendar.AUGUST, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        // Rule AN      2000    only    -       Aug     lastSun 2:00s   1:00   -
        // Rule AN      1996    max     -       Mar     lastSun 2:00s   0      -
        // Zone Australia/Sydney        10:00   AN      EST
        //----------------------------------------------------------
        new SimpleTimeZone(10*ONE_HOUR, "AET" /*alias for Australia/Sydney*/,
          Calendar.AUGUST, 26, 0 /*DOM*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone((int)(10.5*ONE_HOUR), "Australia/Lord_Howe",
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 2*ONE_HOUR, SimpleTimeZone.STANDARD_TIME, (int)(0.5*ONE_HOUR)),
        // Rule	LH	1987	max	-	Oct	lastSun	2:00s	0:30	-
        // Rule	LH	1996	max	-	Mar	lastSun	2:00s	0	-
        // Zone Australia/Lord_Howe	Lord Howe Island(AU)	10:30	LH	LHST
        //----------------------------------------------------------
        new SimpleTimeZone(11*ONE_HOUR, "Pacific/Ponape" /*PONT*/),
        // Pacific/Ponape   Micronesia(FM)  11:00   -   PONT    # Ponape Time
        //----------------------------------------------------------
        new SimpleTimeZone(11*ONE_HOUR, "Pacific/Efate" /*VU%sT*/),
        // Pacific/Efate    Vanuatu(VU) 11:00   -   VU%sT   # Vanuatu Time
        //----------------------------------------------------------
        new SimpleTimeZone(11*ONE_HOUR, "Pacific/Guadalcanal" /*SBT*/),
        // Pacific/Guadalcanal  Solomon Is(SB)  11:00   -   SBT # Solomon Is Time
        new SimpleTimeZone(11*ONE_HOUR, "SST" /*alias for Pacific/Guadalcanal*/),
        //----------------------------------------------------------
        new SimpleTimeZone(11*ONE_HOUR, "Pacific/Noumea"),
        // Zone Pacific/Noumea	New Caledonia(NC)	11:00	NC	NC%sT
        //----------------------------------------------------------
        new SimpleTimeZone(11*ONE_HOUR, "Asia/Magadan" /*MAG%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Magadan Russia(RU)  11:00   Russia  MAG%sT
        //----------------------------------------------------------
        new SimpleTimeZone((int)(11.5*ONE_HOUR), "Pacific/Norfolk" /*NFT*/),
        // Pacific/Norfolk  Norfolk(NF) 11:30   -   NFT # Norfolk Time
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Kosrae" /*KOST*/),
        // Pacific/Kosrae   Micronesia(FM)  12:00   -   KOST    # Kosrae Time
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Tarawa" /*GILT*/),
        // Pacific/Tarawa   Kiribati(KI)    12:00   -   GILT    # Gilbert Is Time
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Majuro" /*MHT*/),
        // Pacific/Majuro   Marshall Is(MH) 12:00   -   MHT
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Nauru" /*NRT*/),
        // Pacific/Nauru    Nauru(NR)   12:00   -   NRT
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Funafuti" /*TVT*/),
        // Pacific/Funafuti Tuvalu(TV)  12:00   -   TVT # Tuvalu Time
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Wake" /*WAKT*/),
        // Pacific/Wake Wake(US)    12:00   -   WAKT    # Wake Time
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Wallis" /*WFT*/),
        // Pacific/Wallis   Wallis and Futuna(WF)   12:00   -   WFT # Wallis & Futuna Time
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Fiji",
          Calendar.NOVEMBER, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR, SimpleTimeZone.WALL_TIME,
          Calendar.FEBRUARY, -1, Calendar.SUNDAY /*DOW_IN_MON*/, 3*ONE_HOUR, SimpleTimeZone.WALL_TIME, 1*ONE_HOUR),
        // Rule	Fiji	1998	max	-	Nov	Sun>=1	2:00	1:00	S
        // Rule	Fiji	1999	max	-	Feb	lastSun	3:00	0	-
        // Zone Pacific/Fiji	Fiji(FJ)	12:00	Fiji	FJ%sT	
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Antarctica/McMurdo" /*NZ%sT*/,
          Calendar.OCTOBER, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.MARCH, 15, -Calendar.SUNDAY /*DOW>=DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule NZAQ    1990    max -   Oct Sun>=1  2:00s   1:00    D
        // Rule NZAQ    1990    max -   Mar Sun>=15 2:00s   0   S
        // Antarctica/McMurdo   USA - year-round bases(AQ)  12:00   NZAQ    NZ%sT
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Asia/Kamchatka" /*PET%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Kamchatka   Russia(RU)  12:00   Russia  PET%sT
        //----------------------------------------------------------
        new SimpleTimeZone(12*ONE_HOUR, "Pacific/Auckland" /*NZ%sT*/,
          Calendar.OCTOBER, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.MARCH, 15, -Calendar.SUNDAY /*DOW>=DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule NZ  1990    max -   Oct Sun>=1  2:00s   1:00    D
        // Rule NZ  1990    max -   Mar Sun>=15 2:00s   0   S
        // Pacific/Auckland New Zealand(NZ) 12:00   NZ  NZ%sT
        new SimpleTimeZone(12*ONE_HOUR, "NST" /*alias for Pacific/Auckland*/,
          Calendar.OCTOBER, 1, -Calendar.SUNDAY /*DOW>=DOM*/, 2*ONE_HOUR,
          Calendar.MARCH, 15, -Calendar.SUNDAY /*DOW>=DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        //----------------------------------------------------------
        new SimpleTimeZone((int)(12.75*ONE_HOUR), "Pacific/Chatham" /*CHA%sT*/,
          Calendar.OCTOBER, 1, -Calendar.SUNDAY /*DOW>=DOM*/, (int)(2.75*ONE_HOUR),
          Calendar.MARCH, 15, -Calendar.SUNDAY /*DOW>=DOM*/, (int)(3.75*ONE_HOUR), 1*ONE_HOUR),
        // Rule Chatham 1990    max -   Oct Sun>=1  2:45s   1:00    D
        // Rule Chatham 1991    max -   Mar Sun>=15 2:45s   0   S
        // Pacific/Chatham  New Zealand(NZ) 12:45   Chatham CHA%sT
        //----------------------------------------------------------
        new SimpleTimeZone(13*ONE_HOUR, "Pacific/Enderbury" /*PHOT*/),
        // Pacific/Enderbury    Kiribati(KI)    13:00   -   PHOT
        //----------------------------------------------------------
        new SimpleTimeZone(13*ONE_HOUR, "Pacific/Tongatapu" /*TOT*/),
        // Pacific/Tongatapu    Tonga(TO)   13:00   -   TOT
        //----------------------------------------------------------
        new SimpleTimeZone(13*ONE_HOUR, "Asia/Anadyr" /*ANA%sT*/,
          Calendar.MARCH, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 2*ONE_HOUR,
          Calendar.OCTOBER, -1, Calendar.SUNDAY /*DOW_IN_DOM*/, 3*ONE_HOUR, 1*ONE_HOUR),
        // Rule Russia  1993    max -   Mar lastSun 2:00s   1:00    S
        // Rule Russia  1996    max -   Oct lastSun 2:00s   0   -
        // Asia/Anadyr  Russia(RU)  13:00   Russia  ANA%sT
        //----------------------------------------------------------
        new SimpleTimeZone(14*ONE_HOUR, "Pacific/Kiritimati" /*LINT*/),
        // Pacific/Kiritimati   Kiribati(KI)    14:00   -   LINT
    };
    // ---------------- END GENERATED DATA ----------------

    private static Hashtable lookup = new Hashtable(zones.length);

    static {
        for (int i=0; i < zones.length; ++i)
            lookup.put(zones[i].getID(), zones[i]);
	TimeZone.getDefault(); // to cache default system time zone
    }
}

//eof
