/*
 * @(#)TimeZone.java    1.51 00/01/19
 *
 * Copyright (C) 1996-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 */

package com.ibm.icu.util;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import com.ibm.icu.impl.JDKTimeZone;
import com.ibm.icu.impl.TimeZoneAdapter;
import com.ibm.icu.impl.ZoneMeta;
import com.ibm.icu.text.SimpleDateFormat;

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
     * Default constructor.  (For invocation by subclass constructors,
     * typically implicit.)
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
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

    /**
     * Cache to hold the SimpleDateFormat objects for a Locale.
     */
    private static Hashtable cachedLocaleData = new Hashtable(3);

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
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public int getOffset(long date) {
    int[] result = new int[2];
    getOffset(date, false, result);
    return result[0]+result[1];
    }

    /**
     * Returns the time zone raw and GMT offset for the given moment
     * in time.  Upon return, local-millis = GMT-millis + rawOffset +
     * dstOffset.  All computations are performed in the proleptic
     * Gregorian calendar.  The default implementation in the TimeZone
     * class delegates to the 8-argument getOffset().
     *
     * @param date moment in time for which to return offsets, in
     * units of milliseconds from January 1, 1970 0:00 GMT, either GMT
     * time or local wall time, depending on `local'.
     * @param local if true, `date' is local wall time; otherwise it
     * is in GMT time.
     * @param offsets output parameter to receive the raw offset, that
     * is, the offset not including DST adjustments, in offsets[0],
     * and the DST offset, that is, the offset to be added to
     * `rawOffset' to obtain the total offset between local and GMT
     * time, in offsets[1]. If DST is not in effect, the DST offset is
     * zero; otherwise it is a positive value, typically one hour.
     *
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public void getOffset(long date, boolean local, int[] offsets) {
        offsets[0] = getRawOffset();
        
        // Convert to local wall millis if necessary
        if (!local) {
            date += offsets[0]; // now in local standard millis
        }

        // When local==FALSE, we might have to recompute. This loop is
        // executed once, unless a recomputation is required; then it is
        // executed twice.
        for (int pass=0; ; ++pass) {
            int fields[] = new int[4];
            long day = floorDivide(date, MILLIS_PER_DAY, fields);
            int millis = fields[0];
            
            computeGregorianFields(day, fields);
            
            offsets[1] = getOffset(GregorianCalendar.AD,
                                   fields[0], fields[1], fields[2],
                                   fields[3], millis) - offsets[0];
            
            // Recompute if local==FALSE, dstOffset!=0, and addition of
            // the dstOffset puts us in a different day.
            if (pass!=0 || local || offsets[1]==0) {
                break;
            }
            date += offsets[1];
            if (floorDivide(date, MILLIS_PER_DAY) == day) {
                break;
            }
        }
    }
    
    /**
     * Divide two long integers, returning the floor of the quotient.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0
     * but <code>floorDivide(-1,4)</code> => -1.
     * TODO: This duplicates a method in Calendar; clean up and
     * consolidate in ICU 3.0.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @return the floor of the quotient.
     */
    static long floorDivide(long numerator, long denominator) {
        // We do this computation in order to handle
        // a numerator of Long.MIN_VALUE correctly
        return (numerator >= 0) ?
            numerator / denominator :
            ((numerator + 1) / denominator) - 1;
    }

    /**
     * Divide two integers, returning the floor of the quotient, and
     * the modulus remainder.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0 and <code>-1%4</code> => -1,
     * but <code>floorDivide(-1,4)</code> => -1 with <code>remainder[0]</code> => 3.
     * TODO: This duplicates a method in Calendar; clean up and
     * consolidate in ICU 3.0.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @param remainder an array of at least one element in which the value
     * <code>numerator mod denominator</code> is returned. Unlike <code>numerator
     * % denominator</code>, this will always be non-negative.
     * @return the floor of the quotient.
     */
    static int floorDivide(long numerator, int denominator, int[] remainder) {
        if (numerator >= 0) {
            remainder[0] = (int)(numerator % denominator);
            return (int)(numerator / denominator);
        }
        int quotient = (int)(((numerator + 1) / denominator) - 1);
        remainder[0] = (int)(numerator - (quotient * denominator));
        return quotient;
    }

    /**
     * Compute the Gregorian calendar year, month, and day of month
     * from the epoch day, and return them in the given array.
     * TODO: This duplicates a method in Calendar; clean up and
     * consolidate in ICU 3.0.
     */
    static void computeGregorianFields(long day, int fields[]) {
        int year, month, dayOfMonth, dayOfYear;

        // Convert from 1970 CE epoch to 1 CE epoch (Gregorian calendar)
        // JULIAN_1_CE    = 1721426; // January 1, 1 CE Gregorian
        // JULIAN_1970_CE = 2440588; // January 1, 1970 CE Gregorian
        day += (2440588 - 1721426);

        // Here we convert from the day number to the multiple radix
        // representation.  We use 400-year, 100-year, and 4-year cycles.
        // For example, the 4-year cycle has 4 years + 1 leap day; giving
        // 1461 == 365*4 + 1 days.
        int[] rem = new int[1];
        int n400 = floorDivide(day, 146097, rem); // 400-year cycle length
        int n100 = floorDivide(rem[0], 36524, rem); // 100-year cycle length
        int n4 = floorDivide(rem[0], 1461, rem); // 4-year cycle length
        int n1 = floorDivide(rem[0], 365, rem);
        year = 400*n400 + 100*n100 + 4*n4 + n1;
        dayOfYear = rem[0]; // zero-based day of year
        if (n100 == 4 || n1 == 4) {
            dayOfYear = 365; // Dec 31 at end of 4- or 400-yr cycle
        } else {
            ++year;
        }

        boolean isLeap = ((year&0x3) == 0) && // equiv. to (year%4 == 0)
            (year%100 != 0 || year%400 == 0);

        int correction = 0;
        int march1 = isLeap ? 60 : 59; // zero-based DOY for March 1
        if (dayOfYear >= march1) correction = isLeap ? 1 : 2;
        month = (12 * (dayOfYear + correction) + 6) / 367; // zero-based month
        dayOfMonth = dayOfYear -
            GREGORIAN_MONTH_COUNT[month][isLeap?1:0] + 1; // one-based DOM

        // Jan 1 1 CE is Monday
        int dayOfWeek = (int) ((day + Calendar.MONDAY) % 7);
        if (dayOfWeek < Calendar.SUNDAY) {
            dayOfWeek += 7;
        }

        fields[0] = year;
        fields[1] = month; // 0-based already
        fields[2] = dayOfMonth; // 1-based already
        fields[3] = dayOfWeek; // 1-based already
        //fields[4] = dayOfYear + 1; // Convert from 0-based to 1-based
    }

    /**
     * The number of milliseconds in one day.
     */
    static final int MILLIS_PER_DAY = 24*60*60*1000;

    /**
     * For each month, the days in a non-leap year before the start
     * the of month, and the days in a leap year before the start of
     * the month.
     * TODO: This duplicates data in Calendar.java; clean up and
     * consolidate in ICU 3.0.
     */
    static final int[][] GREGORIAN_MONTH_COUNT = {
        {   0,   0 }, // Jan
        {  31,  31 }, // Feb
        {  59,  60 }, // Mar
        {  90,  91 }, // Apr
        { 120, 121 }, // May
        { 151, 152 }, // Jun
        { 181, 182 }, // Jul
        { 212, 213 }, // Aug
        { 243, 244 }, // Sep
        { 273, 274 }, // Oct
        { 304, 305 }, // Nov
        { 334, 335 }  // Dec
    };

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
    public String getID() {
        return ID;
    }

    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @param ID the new time zone ID.
     * @stable ICU 2.0
     */
    public void setID(String ID) {
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
        return getDisplayName(false, LONG, ULocale.getDefault());
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
        return getDisplayName(false, LONG, ULocale.forLocale(locale));
    }

    /**
     * Returns a name of this time zone suitable for presentation to the user
     * in the specified locale.
     * This method returns the long name, not including daylight savings.
     * If the display name is not available for the locale,
     * then this method returns a string in the format
     * <code>GMT[+-]hh:mm</code>.
     * @param locale the ulocale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale
     * or in the default ulocale if the given ulocale is not recognized.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final String getDisplayName(ULocale locale) {
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
        return getDisplayName(daylight, style, ULocale.getDefault());
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
        return getDisplayName(daylight, style, ULocale.forLocale(locale));
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
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String getDisplayName(boolean daylight, int style, ULocale locale) {
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
            int savings = getDSTSavings();
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
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
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
        return JDKTimeZone.wrap(java.util.TimeZone.getTimeZone(ID));
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
     * Return a new String array containing all system TimeZone IDs
     * associated with the given country.  These IDs may be passed to
     * <code>get()</code> to construct the corresponding TimeZone
     * object.
     * @param country a two-letter ISO 3166 country code, or <code>null</code>
     * to return zones not associated with any country
     * @return an array of IDs for system TimeZones in the given
     * country.  If there are none, return a zero-length array.
     * @stable ICU 2.0
     */
    public static String[] getAvailableIDs(String country) {
        return ZoneMeta.getAvailableIDs(country);
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
        return ZoneMeta.countEquivalentIDs(id);
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
        return ZoneMeta.getEquivalentID(id, index);
    }

    /**
     * Gets the default <code>TimeZone</code> for this host.
     * The source of the default <code>TimeZone</code> 
     * may vary with implementation.
     * @return a default <code>TimeZone</code>.
     * @stable ICU 2.0
     */
    public static synchronized TimeZone getDefault() {
        if (defaultZone == null) {
            defaultZone = JDKTimeZone.wrap(java.util.TimeZone.getDefault());
        }
        return (TimeZone) defaultZone.clone();
    }

    /**
     * Sets the <code>TimeZone</code> that is
     * returned by the <code>getDefault</code> method.  If <code>zone</code>
     * is null, reset the default to the value it had originally when the
     * VM first started.
     * @param tz the new default time zone
     * @stable ICU 2.0
     */
    public static synchronized void setDefault(TimeZone tz) {
        defaultZone = tz;
        // Keep java.util.TimeZone default in sync so java.util.Date
        // can interoperate with com.ibm.icu.util classes.
        java.util.TimeZone jdkZone = null;
        if (tz != null) {
            jdkZone = TimeZoneAdapter.wrap(tz);
        }
        java.util.TimeZone.setDefault(jdkZone);
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
        return other != null &&
            getRawOffset() == other.getRawOffset() &&
            useDaylightTime() == other.useDaylightTime();
    }

    /**
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone() {
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

    /**
     * The default time zone, or null if not set.
     */
    private static TimeZone  defaultZone = null;
}

//eof
