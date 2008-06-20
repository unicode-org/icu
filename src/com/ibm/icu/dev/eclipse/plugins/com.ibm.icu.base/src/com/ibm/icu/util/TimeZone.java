/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.io.Serializable;
import java.util.Date;
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
 * @see          Calendar
 * @see          GregorianCalendar
 * @see          SimpleTimeZone
 * @author       Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu
 * @stable ICU 2.0
 */
public class TimeZone implements Serializable, Cloneable {
    private static final long serialVersionUID = 1;
        
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

    /**
     * Default constructor to mirror Java's public
     * constructor.  Java's is not callable as a public API, since
     * their TimeZone is abstract, so this is only useful to
     * subclasses.  In general, subclasses will not work unless
     * they manipulate the delegate.
     */
    public TimeZone() {
        this.timeZone = java.util.TimeZone.getDefault();
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
        if (inDaylightTime(new Date(date))) {
            return getRawOffset() + getDSTSavings();
        }
        return getRawOffset();
    }

   /**
     * Sets the base time zone offset to GMT.
     * This is the offset to add *to* UTC to get local time.
     * @param offsetMillis the given base time zone offset to GMT.
     * @stable ICU 2.0
     */
    public void setRawOffset(int offsetMillis) {
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
        return timeZone.getDisplayName();
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
     * @param style either <code>LONG</code> or <code>SHORT</code>
     * @return the human-readable name of this time zone in the default locale.
     * @stable ICU 2.0
     */
    public final String getDisplayName(boolean daylight, int style) {
        return timeZone.getDisplayName(daylight, style);
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
        return timeZone.getDisplayName(daylight, style, locale);
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
     * @stable ICU 3.2
     */
    public String getDisplayName(boolean daylight, int style, ULocale locale) {
        return timeZone.getDisplayName(daylight, style, locale.toLocale());
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
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
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
     * @param ID the ID for a <code>TimeZone</code>, either an abbreviation
     * such as "PST", a full name such as "America/Los_Angeles", or a custom
     * ID such as "GMT-8:00". Note that the support of abbreviations is
     * for JDK 1.1.x compatibility only and full names should be used.
     *
     * @return the specified <code>TimeZone</code>, or the GMT zone if the given ID
     * cannot be understood.
     * @stable ICU 2.0
     */
    public static TimeZone getTimeZone(String ID) {
        return new TimeZone(java.util.TimeZone.getTimeZone(ID));
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
        return java.util.TimeZone.getAvailableIDs(rawOffset);
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
        return java.util.TimeZone.getAvailableIDs();
    }
  
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
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone() {
        return new TimeZone((java.util.TimeZone)timeZone.clone());
    }
    
    /**
     * Return true if rhs is a TimeZone and has the same time rules as this.
     * @return true if rhs equals this
     * @stable ICU 3.4.2
     */
    public boolean equals(Object rhs){
        try {
            return timeZone.equals(((TimeZone)rhs).timeZone);
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Return a hashCode.
     * @return a hashCode
     * @stable ICU 3.4.2
     */
    public int hashCode(){
        return timeZone.hashCode();
    }
}
