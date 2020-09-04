// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 * @(#)TimeZone.java    1.51 00/01/19
 *
 * Copyright (C) 1996-2016, International Business Machines
 * Corporation and others.  All Rights Reserved.
 */

package com.ibm.icu.util;

import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;

import com.ibm.icu.util.ULocale.Category;

/**
 * {@icuenhanced java.util.TimeZone}.{@icu _usage_}
 *
 * <p><code>TimeZone</code> represents a time zone offset, and also computes daylight
 * savings.
 *
 * <p>Typically, you get a <code>TimeZone</code> using {@link #getDefault()}
 * which creates a <code>TimeZone</code> based on the time zone where the program
 * is running. For example, for a program running in Japan, <code>getDefault</code>
 * creates a <code>TimeZone</code> object based on Japanese Standard Time.
 *
 * <p>You can also get a <code>TimeZone</code> using {@link #getTimeZone(String)}
 * along with a time zone ID. For instance, the time zone ID for the
 * U.S. Pacific Time zone is "America/Los_Angeles". So, you can get a
 * U.S. Pacific Time <code>TimeZone</code> object with:
 *
 * <blockquote>
 * <pre>
 * TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
 * </pre>
 * </blockquote>
 * You can use the {@link #getAvailableIDs()} method to iterate through
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
 *
 * <p>For compatibility with JDK 1.1.x, some other three-letter time zone IDs
 * (such as "PST", "CTT", "AST") are also supported. However, <strong>their
 * use is deprecated</strong> because the same abbreviation is often used
 * for multiple time zones (for example, "CST" could be U.S. "Central Standard
 * Time" and "China Standard Time"), and the Java platform can then only
 * recognize one of them.
 *
 * <p><strong>Note:</strong> Starting from ICU4J 4.0, you can optionally choose
 * JDK <code>TimeZone</code> as the time zone implementation.  The TimeZone factory
 * method <code>getTimeZone</code> creates an instance of ICU's own <code>TimeZone</code>
 * subclass by default.  If you want to use the JDK implementation always, you can
 * set the default time zone implementation type by the new method
 * <code>setDefaultTimeZoneType</code>.  Alternatively, you can change the initial
 * default implementation type by setting a property below.
 *
 * <blockquote>
 * <pre>
 * #
 * # The default TimeZone implementation type used by the ICU TimeZone
 * # factory method. [ ICU | JDK ]
 * #
 * com.ibm.icu.util.TimeZone.DefaultTimeZoneType = ICU
 * </pre>
 * </blockquote>
 *
 * <p>This property is included in ICUConfig.properties in com.ibm.icu package.  When the
 * <code>TimeZone</code> class is loaded, the initialization code checks if the property
 * <code>com.ibm.icu.util.TimeZone.DefaultTimeZoneType=xxx</code> is defined by the system
 * properties.  If not available, then it loads ICUConfig.properties to get the default
 * time zone implementation type.  The property setting is only used for the initial
 * default value and you can change the default type by calling
 * <code>setDefaultTimeZoneType</code> at runtime.
 *
 * @see          Calendar
 * @see          GregorianCalendar
 * @see          SimpleTimeZone
 * @author       Mark Davis, Deborah Goldsmith, Chen-Lieh Huang, Alan Liu
 * @stable ICU 2.0
 */
public class TimeZone implements Serializable, Cloneable, Freezable<TimeZone> {
    private static final long serialVersionUID = 1L;
    
    /**
     * @internal
     */
    public final java.util.TimeZone timeZone;
        
    /**
     * @internal
     * @param delegate the TimeZone to which to delegate
     */
    public TimeZone(java.util.TimeZone delegate) {
        this.timeZone = delegate;
    }

//    /**
//     * {@icu} A logger for TimeZone. Will be null if logging is not on by way of system
//     * property: "icu4j.debug.logging"
//     * @draft ICU 4.4
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static ICULogger TimeZoneLogger = ICULogger.getICULogger(TimeZone.class.getName());

    /**
     * Default constructor.  (For invocation by subclass constructors,
     * typically implicit.)
     * @stable ICU 2.8
     */
    public TimeZone() {
        this.timeZone = java.util.TimeZone.getDefault();
    }

    /**
     * {@icu} A time zone implementation type indicating ICU's own TimeZone used by
     * <code>getTimeZone</code>, <code>setDefaultTimeZoneType</code>
     * and <code>getDefaultTimeZoneType</code>.
     * @stable ICU 4.0
     */
    public static final int TIMEZONE_ICU = 0;
    /**
     * {@icu} A time zone implementation type indicating JDK TimeZone used by
     * <code>getTimeZone</code>, <code>setDefaultTimeZoneType</code>
     * and <code>getDefaultTimeZoneType</code>.
     * @stable ICU 4.0
     */
    public static final int TIMEZONE_JDK = 1;

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

//    /**
//     * {@icu} A style specifier for <code>getDisplayName()</code> indicating
//     * a short generic name, such as "PT."
//     * @see #LONG_GENERIC
//     * @draft ICU 4.4
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static final int SHORT_GENERIC = 2;
//
//    /**
//     * {@icu} A style specifier for <code>getDisplayName()</code> indicating
//     * a long generic name, such as "Pacific Time."
//     * @see #SHORT_GENERIC
//     * @draft ICU 4.4
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static final int LONG_GENERIC = 3;
//
//    /**
//     * {@icu} A style specifier for <code>getDisplayName()</code> indicating
//     * a short name derived from the timezone's offset, such as "-0800."
//     * @see #LONG_GMT
//     * @draft ICU 4.4
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static final int SHORT_GMT = 4;
//
//    /**
//     * {@icu} A style specifier for <code>getDisplayName()</code> indicating
//     * a long name derived from the timezone's offset, such as "GMT-08:00."
//     * @see #SHORT_GMT
//     * @draft ICU 4.4
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static final int LONG_GMT = 5;
//
//    /**
//     * {@icu} A style specifier for <code>getDisplayName()</code> indicating
//     * a short name derived from the timezone's short standard or daylight
//     * timezone name ignoring commonlyUsed, such as "PDT."
//     * @draft ICU 4.4
//     * @provisional This API might change or be removed in a future release.
//     */
//
//    public static final int SHORT_COMMONLY_USED = 6;
//
//    /**
//     * {@icu} A style specifier for <code>getDisplayName()</code> indicating
//     * a long name derived from the timezone's fallback name, such as
//     * "United States (Los Angeles)."
//     * @draft ICU 4.4
//     * @provisional This API might change or be removed in a future release.
//     */
//    public static final int GENERIC_LOCATION = 7;
//
//    /**
//     * Gets the time zone offset, for current date, modified in case of
//     * daylight savings. This is the offset to add *to* UTC to get local time.
//     * @param era the era of the given date.
//     * @param year the year in the given date.
//     * @param month the month in the given date.
//     * Month is 0-based. e.g., 0 for January.
//     * @param day the day-in-month of the given date.
//     * @param dayOfWeek the day-of-week of the given date.
//     * @param milliseconds the millis in day in <em>standard</em> local time.
//     * @return the offset to add *to* GMT to get local time.
//     * @stable ICU 2.0
//     */
    
//    /**
//     * {@icu} System time zone type constants used by filtering zones in
//     * {@link TimeZone#getAvailableIDs(SystemTimeZoneType, String, Integer)}
//     *
//     * @draft ICU 4.8
//     * @provisional This API might change or be removed in a future release.
//     */
//    public enum SystemTimeZoneType {
//        /**
//         * Any system zones.
//         * @draft ICU 4.8
//         * @provisional This API might change or be removed in a future release.
//         */
//        ANY,
//
//        /**
//         * Canonical system zones.
//         * @draft ICU 4.8
//         * @provisional This API might change or be removed in a future release.
//         */
//        CANONICAL,
//
//        /**
//         * Canonical system zones associated with actual locations.
//         * @draft ICU 4.8
//         * @provisional This API might change or be removed in a future release.
//         */
//        CANONICAL_LOCATION,
//    }
    
    public int getOffset(int era, int year, int month, int day,
                                  int dayOfWeek, int milliseconds) {
        return timeZone.getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }


    /**
     * Returns the offset of this time zone from UTC at the specified
     * date. If Daylight Saving Time is in effect at the specified
     * date, the offset value is adjusted with the amount of daylight
     * saving.
     *
     * @param date the date represented in milliseconds since January 1, 1970 00:00:00 GMT
     * @return the amount of time in milliseconds to add to UTC to get local time.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     * @see #getOffset(long, boolean, int[])
     * @stable ICU 2.8
     */
    public int getOffset(long date) {
        return timeZone.getOffset(date);
    }

//    /**
//     * Returns the time zone raw and GMT offset for the given moment
//     * in time.  Upon return, local-millis = GMT-millis + rawOffset +
//     * dstOffset.  All computations are performed in the proleptic
//     * Gregorian calendar.  The default implementation in the TimeZone
//     * class delegates to the 8-argument getOffset().
//     *
//     * @param date moment in time for which to return offsets, in
//     * units of milliseconds from January 1, 1970 0:00 GMT, either GMT
//     * time or local wall time, depending on `local'.
//     * @param local if true, `date' is local wall time; otherwise it
//     * is in GMT time.
//     * @param offsets output parameter to receive the raw offset, that
//     * is, the offset not including DST adjustments, in offsets[0],
//     * and the DST offset, that is, the offset to be added to
//     * `rawOffset' to obtain the total offset between local and GMT
//     * time, in offsets[1]. If DST is not in effect, the DST offset is
//     * zero; otherwise it is a positive value, typically one hour.
//     *
//     * @stable ICU 2.8
//     */
//    public void getOffset(long date, boolean local, int[] offsets) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Sets the base time zone offset to GMT.
     * This is the offset to add *to* UTC to get local time.
     * @param offsetMillis the given base time zone offset to GMT.
     * @stable ICU 2.0
     */
    public void setRawOffset(int offsetMillis) {
        if (isFrozen) {
            throw new UnsupportedOperationException("Attempt to modify a frozen TimeZone instance.");
        }
        timeZone.setRawOffset(offsetMillis);
    }

    /**
     * Gets unmodified offset, NOT modified in case of daylight savings.
     * This is the offset to add *to* UTC to get local time.
     * @return the unmodified offset to add *to* UTC to get local time.
     * @stable ICU 2.0
     */
    public int getRawOffset() {
        return timeZone.getRawOffset();
    }

    /**
     * Gets the ID of this time zone.
     * @return the ID of this time zone.
     * @stable ICU 2.0
     */
    public String getID() {
        return timeZone.getID();
    }

    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @param ID the new time zone ID.
     * @stable ICU 2.0
     */
    public void setID(String ID) {
        if (isFrozen) {
            throw new UnsupportedOperationException("Attempt to modify a frozen TimeZone instance.");
        }
        timeZone.setID(ID);
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the default locale.
     * This method returns the long generic name.
     * If the display name is not available for the locale,
     * a fallback based on the country, city, or time zone id will be used.
     * @return the human-readable name of this time zone in the default locale.
     * @stable ICU 2.0
     */
    public final String getDisplayName() {
        return timeZone.getDisplayName(ULocale.getDefault(Category.DISPLAY).toLocale());
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * This method returns the long generic name.
     * If the display name is not available for the locale,
     * a fallback based on the country, city, or time zone id will be used.
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default locale if the given locale is not recognized.
     * @stable ICU 2.0
     */
    public final String getDisplayName(Locale locale) {
        return timeZone.getDisplayName(locale);
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * This method returns the long name, not including daylight savings.
     * If the display name is not available for the locale,
     * a fallback based on the country, city, or time zone id will be used.
     * @param locale the ulocale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default ulocale if the given ulocale is not recognized.
     * @stable ICU 3.2
     */
    public final String getDisplayName(ULocale locale) {
        return timeZone.getDisplayName(locale.toLocale());
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the default locale.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param daylight if true, return the daylight savings name.
     * @param style the output style of the display name.  Valid styles are
     * <code>SHORT</code>, <code>LONG</code>, <code>SHORT_GENERIC</code>,
     * <code>LONG_GENERIC</code>, <code>SHORT_GMT</code>, <code>LONG_GMT</code>,
     * <code>SHORT_COMMONLY_USED</code> or <code>GENERIC_LOCATION</code>.
     * @return the human-readable name of this time zone in the default locale.
     * @stable ICU 2.0
     */
    public final String getDisplayName(boolean daylight, int style) {
        return getDisplayName(daylight, style, ULocale.getDefault(Category.DISPLAY));
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param daylight if true, return the daylight savings name.
     * @param style the output style of the display name.  Valid styles are
     * <code>SHORT</code>, <code>LONG</code>, <code>SHORT_GENERIC</code>,
     * <code>LONG_GENERIC</code>, <code>SHORT_GMT</code>, <code>LONG_GMT</code>,
     * <code>SHORT_COMMONLY_USED</code> or <code>GENERIC_LOCATION</code>.
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default locale if the given locale is not recognized.
     * @exception IllegalArgumentException style is invalid.
     * @stable ICU 2.0
     */
    public String getDisplayName(boolean daylight, int style, Locale locale) {
        return getDisplayName(daylight, style, ULocale.forLocale(locale));
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param daylight if true, return the daylight savings name.
     * @param style the output style of the display name.  Valid styles are
     * <code>SHORT</code>, <code>LONG</code>, <code>SHORT_GENERIC</code>,
     * <code>LONG_GENERIC</code>, <code>SHORT_GMT</code>, <code>LONG_GMT</code>,
     * <code>SHORT_COMMONLY_USED</code> or <code>GENERIC_LOCATION</code>.
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default locale if the given locale is not recognized.
     * @exception IllegalArgumentException style is invalid.
     * @stable ICU 3.2
     */
    public String getDisplayName(boolean daylight, int style, ULocale locale) {
        if (style == SHORT) {
            return timeZone.getDisplayName(daylight, java.util.TimeZone.SHORT, locale.toLocale());
        } else if (style == LONG) {
            return timeZone.getDisplayName(daylight, java.util.TimeZone.LONG, locale.toLocale());
        } else {
            throw new UnsupportedOperationException("Specified time zone format style is not supported by com.ibm.icu.base");
        }
    }

    /**
     * Returns the amount of time to be added to local standard time
     * to get local wall clock time.
     * <p>
     * The default implementation always returns 3600000 milliseconds
     * (i.e., one hour) if this time zone observes Daylight Saving
     * Time. Otherwise, 0 (zero) is returned.
     * <p>
     * If an underlying TimeZone implementation subclass supports
     * historical Daylight Saving Time changes, this method returns
     * the known latest daylight saving value.
     *
     * @return the amount of saving time in milliseconds
     * @stable ICU 2.8
     */
    public int getDSTSavings() {
        return timeZone.getDSTSavings();
    }

    /**
     * Queries if this time zone uses daylight savings time.
     * @return true if this time zone uses daylight savings time,
     * false, otherwise.
     * @stable ICU 2.0
     */
    public boolean useDaylightTime() {
        return timeZone.useDaylightTime();
    }

//    /**
//     * Queries if this time zone is in daylight saving time or will observe
//     * daylight saving time at any future time.
//     * <p>The default implementation in this class returns <code>true</code> if {@link #useDaylightTime()}
//     * or {@link #inDaylightTime(Date) inDaylightTime(new Date())} returns <code>true</code>.
//     * <p>
//     * <strong>Note:</strong> This method was added for JDK compatibility support.
//     * The JDK's <code>useDaylightTime()</code> only checks the last known rule(s), therefore
//     * it may return false even the zone observes daylight saving time currently. JDK added
//     * <code>observesDaylightTime()</code> to resolve the issue. In ICU, {@link #useDaylightTime()}
//     * works differently. The ICU implementation checks if the zone uses daylight saving time
//     * in the current calendar year. Therefore, it will never return <code>false</code> if
//     * daylight saving time is currently used.
//     * <p>
//     * ICU's TimeZone subclass implementations override this method to support the same behavior
//     * with JDK's <code>observesDaylightSavingTime()</code>. Unlike {@link #useDaylightTime()},
//     * the implementation does not take past daylight saving time into account, so
//     * that this method may return <code>false</code> even when {@link #useDaylightTime()} returns
//     * <code>true</code>.
//     * 
//     * @return <code>true</code> if this time zone is in daylight saving time or will observe
//     * daylight saving time at any future time.
//     * @see #useDaylightTime
//     * @stable ICU 49
//     */
//    public boolean observesDaylightTime() {
//        throw new UnsupportedOperationException("Method not supproted by com.ibm.icu.base");
//    }

    /**
     * Queries if the given date is in daylight savings time in
     * this time zone.
     * @param date the given Date.
     * @return true if the given date is in daylight savings time,
     * false, otherwise.
     * @stable ICU 2.0
     */
    public boolean inDaylightTime(Date date) {
        return timeZone.inDaylightTime(date);
    }

    /**
     * Gets the <code>TimeZone</code> for the given ID.
     *
     * @param ID the ID for a <code>TimeZone</code>, such as "America/Los_Angeles",
     * or a custom ID such as "GMT-8:00". Note that the support of abbreviations,
     * such as "PST", is for JDK 1.1.x compatibility only and full names should be used.
     *
     * @return the specified <code>TimeZone</code>, or the GMT zone if the given ID
     * cannot be understood.
     * @stable ICU 2.0
     */
    public static synchronized TimeZone getTimeZone(String ID) {
        return new TimeZone(java.util.TimeZone.getTimeZone(ID));
    }

    /**
     * Gets the <code>TimeZone</code> for the given ID. The instance of <code>TimeZone</code>
     * returned by this method is immutable. Any methods mutate the instance({@link #setID(String)},
     * {@link #setRawOffset(int)}) will throw <code>UnsupportedOperationException</code> upon its
     * invocation.
     *
     * @param ID the ID for a <code>TimeZone</code>, such as "America/Los_Angeles",
     * or a custom ID such as "GMT-8:00". Note that the support of abbreviations,
     * such as "PST", is for JDK 1.1.x compatibility only and full names should be used.
     *
     * @return the specified <code>TimeZone</code>, or the UNKNOWN_ZONE
     * if the given ID cannot be understood.
     * @see #UNKNOWN_ZONE
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public static TimeZone getFrozenTimeZone(String ID) {
        return getTimeZone(ID).freeze();
    }

    /**
     * Gets the <code>TimeZone</code> for the given ID and the timezone type.
     * @param ID the ID for a <code>TimeZone</code>, such as "America/Los_Angeles", or a
     * custom ID such as "GMT-8:00". Note that the support of abbreviations, such as
     * "PST", is for JDK 1.1.x compatibility only and full names should be used.
     * @param type Time zone type, either <code>TIMEZONE_ICU</code> or
     * <code>TIMEZONE_JDK</code>.
     * @return the specified <code>TimeZone</code>, or the GMT zone if the given ID
     * cannot be understood.
     * @stable ICU 4.0
     */
    public static synchronized TimeZone getTimeZone(String ID, int type) {
        if (type == TIMEZONE_JDK) {
            return new TimeZone(java.util.TimeZone.getTimeZone(ID));
        }
        throw new UnsupportedOperationException("TIMEZONE_ICU not supported by com.ibm.icu.base");
    }

    /**
     * Sets the default time zone type used by <code>getTimeZone</code>.
     * @param type time zone type, either <code>TIMEZONE_ICU</code> or
     * <code>TIMEZONE_JDK</code>.
     * @stable ICU 4.0
     */
    public static synchronized void setDefaultTimeZoneType(int type) {
        if (type != TIMEZONE_JDK) {
            throw new UnsupportedOperationException("TimeZone type other than TIMEZONE_JDK is not supported by com.ibm.icu.base");
        }
    }

    /**
     * {@icu} Returns the default time zone type currently used.
     * @return The default time zone type, either <code>TIMEZONE_ICU</code> or
     * <code>TIMEZONE_JDK</code>.
     * @stable ICU 4.0
     */
    public static int getDefaultTimeZoneType() {
        return TIMEZONE_JDK;
    }

//    /** 
//     * {@icu} Returns a set of time zone ID strings with the given filter conditions. 
//     * <p><b>Note:</b>A <code>Set</code> returned by this method is
//     * immutable.
//     * @param zoneType      The system time zone type.
//     * @param region        The ISO 3166 two-letter country code or UN M.49 three-digit area code. 
//     *                      When null, no filtering done by region. 
//     * @param rawOffset     An offset from GMT in milliseconds, ignoring the effect of daylight savings 
//     *                      time, if any. When null, no filtering done by zone offset. 
//     * @return an immutable set of system time zone IDs.
//     * @see SystemTimeZoneType
//     * 
//     * @draft ICU 4.8
//     * @provisional This API might change or be removed in a future release.
//     */ 
//    public static Set<String> getAvailableIDs(SystemTimeZoneType zoneType,
//            String region, Integer rawOffset) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }
    
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
        return java.util.TimeZone.getAvailableIDs(rawOffset);

    }


//    /**
//     * Return a new String array containing all system TimeZone IDs
//     * associated with the given country.  These IDs may be passed to
//     * <code>get()</code> to construct the corresponding TimeZone
//     * object.
//     * @param country a two-letter ISO 3166 country code, or <code>null</code>
//     * to return zones not associated with any country
//     * @return an array of IDs for system TimeZones in the given
//     * country.  If there are none, return a zero-length array.
//     * @stable ICU 2.0
//     */
//    public static String[] getAvailableIDs(String country) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Return a new String array containing all system TimeZone IDs.
     * These IDs (and only these IDs) may be passed to
     * <code>get()</code> to construct the corresponding TimeZone
     * object.
     * @return an array of all system TimeZone IDs
     * @stable ICU 2.0
     */
    public static String[] getAvailableIDs() {
        return java.util.TimeZone.getAvailableIDs();
    }

//    /**
//     * {@icu} Returns the number of IDs in the equivalency group that
//     * includes the given ID.  An equivalency group contains zones
//     * that have the same GMT offset and rules.
//     *
//     * <p>The returned count includes the given ID; it is always >= 1
//     * for valid IDs.  The given ID must be a system time zone.  If it
//     * is not, returns zero.
//     * @param id a system time zone ID
//     * @return the number of zones in the equivalency group containing
//     * 'id', or zero if 'id' is not a valid system ID
//     * @see #getEquivalentID
//     * @stable ICU 2.0
//     */
//    public static int countEquivalentIDs(String id) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns an ID in the equivalency group that
//     * includes the given ID.  An equivalency group contains zones
//     * that have the same GMT offset and rules.
//     *
//     * <p>The given index must be in the range 0..n-1, where n is the
//     * value returned by <code>countEquivalentIDs(id)</code>.  For
//     * some value of 'index', the returned value will be equal to the
//     * given id.  If the given id is not a valid system time zone, or
//     * if 'index' is out of range, then returns an empty string.
//     * @param id a system time zone ID
//     * @param index a value from 0 to n-1, where n is the value
//     * returned by <code>countEquivalentIDs(id)</code>
//     * @return the ID of the index-th zone in the equivalency group
//     * containing 'id', or an empty string if 'id' is not a valid
//     * system ID or 'index' is out of range
//     * @see #countEquivalentIDs
//     * @stable ICU 2.0
//     */
//    public static String getEquivalentID(String id, int index) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Gets the default <code>TimeZone</code> for this host.
     * The source of the default <code>TimeZone</code>
     * may vary with implementation.
     * @return a default <code>TimeZone</code>.
     * @stable ICU 2.0
     */
    public static TimeZone getDefault() {
        return new TimeZone(java.util.TimeZone.getDefault());
    }

    /**
     * Sets the <code>TimeZone</code> that is
     * returned by the <code>getDefault</code> method.  If <code>zone</code>
     * is null, reset the default to the value it had originally when the
     * VM first started.
     * @param tz the new default time zone
     * @stable ICU 2.0
     */
    public static void setDefault(TimeZone tz) {
        java.util.TimeZone.setDefault(tz.timeZone);
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
        return timeZone.hasSameRules(other.timeZone);
    }

    /**
     * Overrides clone.
     * @stable ICU 2.0
     */
    public Object clone() {
        return new TimeZone((java.util.TimeZone)timeZone.clone());
    }

    /**
     * Overrides equals.
     * @stable ICU 3.6
     */
    public boolean equals(Object obj){
        try {
            return timeZone.equals(((TimeZone)obj).timeZone);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Overrides hashCode.
     * @stable ICU 3.6
     */
    public int hashCode(){
        return timeZone.hashCode();
    }

//    /**
//     * {@icu} Returns the time zone data version currently used by ICU.
//     *
//     * @return the version string, such as "2007f"
//     * @throws MissingResourceException if ICU time zone resource bundle
//     * is missing or the version information is not available.
//     *
//     * @stable ICU 3.8
//     */
//    public static synchronized String getTZDataVersion() {
//        throw new UnsupportedOperationException("Method not supproted by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the canonical system time zone ID or the normalized
//     * custom time zone ID for the given time zone ID.
//     * @param id The input time zone ID to be canonicalized.
//     * @return The canonical system time zone ID or the custom time zone ID
//     * in normalized format for the given time zone ID.  When the given time zone ID
//     * is neither a known system time zone ID nor a valid custom time zone ID,
//     * null is returned.
//     * @stable ICU 4.0
//     */
//    public static String getCanonicalID(String id) {
//        throw new UnsupportedOperationException("Method not supproted by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Returns the canonical system time zone ID or the normalized
//     * custom time zone ID for the given time zone ID.
//     * @param id The input time zone ID to be canonicalized.
//     * @param isSystemID When non-null boolean array is specified and
//     * the given ID is a known system time zone ID, true is set to <code>isSystemID[0]</code>
//     * @return The canonical system time zone ID or the custom time zone ID
//     * in normalized format for the given time zone ID.  When the given time zone ID
//     * is neither a known system time zone ID nor a valid custom time zone ID,
//     * null is returned.
//     * @stable ICU 4.0
//     */
//    public static String getCanonicalID(String id, boolean[] isSystemID) {
//        throw new UnsupportedOperationException("Method not supproted by com.ibm.icu.base");
//    }

//    /** 
//     * {@icu} Returns the region code associated with the given 
//     * system time zone ID. The region code is either ISO 3166 
//     * 2-letter country code or UN M.49 3-digit area code. 
//     * When the time zone is not associated with a specific location, 
//     * for example - "Etc/UTC", "EST5EDT", then this method returns 
//     * "001" (UN M.49 area code for World). 
//     * @param id the system time zone ID. 
//     * @return the region code associated with the given 
//     * system time zone ID. 
//     * @throws IllegalArgumentException if <code>id</code> is not a known system ID. 
//     * @see #getAvailableIDs(String) 
//     * 
//     * @draft ICU 4.8
//     * @provisional This API might change or be removed in a future release.
//     */ 
//    public static String getRegion(String id) {
//    	throw new UnsupportedOperationException("Method not supproted by com.ibm.icu.base");
//    }

    private transient boolean isFrozen = false;

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isFrozen() {
        return isFrozen;
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZone freeze() {
        isFrozen = true;
        return this;
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZone cloneAsThawed() {
        try {
            TimeZone other = (TimeZone) super.clone();
            other.isFrozen = false;
            return other;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}

//eof
