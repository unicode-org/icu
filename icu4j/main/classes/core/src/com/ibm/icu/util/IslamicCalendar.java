/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.impl.CalendarAstronomer;
import com.ibm.icu.impl.CalendarCache;
import java.util.Date;
import java.util.Locale;

/**
 * <code>IslamicCalendar</code> is a subclass of <code>Calendar</code>
 * that that implements the Islamic civil and religious calendars.  It
 * is used as the civil calendar in most of the Arab world and the
 * liturgical calendar of the Islamic faith worldwide.  This calendar
 * is also known as the "Hijri" calendar, since it starts at the time
 * of Mohammed's emigration (or "hijra") to Medinah on Thursday, 
 * July 15, 622 AD (Julian).
 * <p>
 * The Islamic calendar is strictly lunar, and thus an Islamic year of twelve
 * lunar months does not correspond to the solar year used by most other
 * calendar systems, including the Gregorian.  An Islamic year is, on average,
 * about 354 days long, so each successive Islamic year starts about 11 days
 * earlier in the corresponding Gregorian year.
 * <p>
 * Each month of the calendar starts when the new moon's crescent is visible
 * at sunset.  However, in order to keep the time fields in this class
 * synchronized with those of the other calendars and with local clock time,
 * we treat days and months as beginning at midnight,
 * roughly 6 hours after the corresponding sunset.
 * <p>
 * There are two main variants of the Islamic calendar in existence.  The first
 * is the <em>civil</em> calendar, which uses a fixed cycle of alternating 29-
 * and 30-day months, with a leap day added to the last month of 11 out of
 * every 30 years.  This calendar is easily calculated and thus predictable in
 * advance, so it is used as the civil calendar in a number of Arab countries.
 * This is the default behavior of a newly-created <code>IslamicCalendar</code>
 * object.
 * <p>
 * The Islamic <em>religious</em> calendar, however, is based on the <em>observation</em>
 * of the crescent moon.  It is thus affected by the position at which the
 * observations are made, seasonal variations in the time of sunset, the
 * eccentricities of the moon's orbit, and even the weather at the observation
 * site.  This makes it impossible to calculate in advance, and it causes the
 * start of a month in the religious calendar to differ from the civil calendar
 * by up to three days.
 * <p>
 * Using astronomical calculations for the position of the sun and moon, the
 * moon's illumination, and other factors, it is possible to determine the start
 * of a lunar month with a fairly high degree of certainty.  However, these
 * calculations are extremely complicated and thus slow, so most algorithms,
 * including the one used here, are only approximations of the true astronical
 * calculations.  At present, the approximations used in this class are fairly
 * simplistic; they will be improved in later versions of the code.
 * <p>
 * The {@link #setCivil setCivil} method determines
 * which approach is used to determine the start of a month.  By default, the
 * fixed-cycle civil calendar is used.  However, if <code>setCivil(false)</code>
 * is called, an approximation of the true lunar calendar will be used.
 * <p>
 * This class should not be subclassed.</p>
 * <p>
 * IslamicCalendar usually should be instantiated using 
 * {@link com.ibm.icu.util.Calendar#getInstance(ULocale)} passing in a <code>ULocale</code>
 * with the tag <code>"@calendar=islamic"</code> or <code>"@calendar=islamic-civil"</code>.</p>
 *
 * @see com.ibm.icu.util.GregorianCalendar
 * @see com.ibm.icu.util.Calendar
 *
 * @author Laura Werner
 * @author Alan Liu
 * @stable ICU 2.8
 */
public class IslamicCalendar extends Calendar {
    // jdk1.4.2 serialver
    private static final long serialVersionUID = -6253365474073869325L;

    //-------------------------------------------------------------------------
    // Constants...
    //-------------------------------------------------------------------------
    
    /**
     * Constant for Muharram, the 1st month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int MUHARRAM = 0;

    /**
     * Constant for Safar, the 2nd month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int SAFAR = 1;

    /**
     * Constant for Rabi' al-awwal (or Rabi' I), the 3rd month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int RABI_1 = 2;

    /**
     * Constant for Rabi' al-thani or (Rabi' II), the 4th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int RABI_2 = 3;

    /**
     * Constant for Jumada al-awwal or (Jumada I), the 5th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int JUMADA_1 = 4;

    /**
     * Constant for Jumada al-thani or (Jumada II), the 6th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int JUMADA_2 = 5;

    /**
     * Constant for Rajab, the 7th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int RAJAB = 6;

    /**
     * Constant for Sha'ban, the 8th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int SHABAN = 7;

    /**
     * Constant for Ramadan, the 9th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int RAMADAN = 8;

    /**
     * Constant for Shawwal, the 10th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int SHAWWAL = 9;

    /**
     * Constant for Dhu al-Qi'dah, the 11th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int DHU_AL_QIDAH = 10;

    /**
     * Constant for Dhu al-Hijjah, the 12th month of the Islamic year. 
     * @stable ICU 2.8 
     */
    public static final int DHU_AL_HIJJAH = 11;


    private static final long HIJRA_MILLIS = -42521587200000L;    // 7/16/622 AD 00:00

    //-------------------------------------------------------------------------
    // Constructors...
    //-------------------------------------------------------------------------

    /**
     * Constructs a default <code>IslamicCalendar</code> using the current time
     * in the default time zone with the default locale.
     * @stable ICU 2.8
     */
    public IslamicCalendar()
    {
        this(TimeZone.getDefault(), ULocale.getDefault());
    }

    /**
     * Constructs an <code>IslamicCalendar</code> based on the current time
     * in the given time zone with the default locale.
     * @param zone the given time zone.
     * @stable ICU 2.8
     */
    public IslamicCalendar(TimeZone zone)
    {
        this(zone, ULocale.getDefault());
    }

    /**
     * Constructs an <code>IslamicCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale the given locale.
     * @stable ICU 2.8
     */
    public IslamicCalendar(Locale aLocale)
    {
        this(TimeZone.getDefault(), aLocale);
    }

    /**
     * Constructs an <code>IslamicCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param locale the given ulocale.
     * @stable ICU 3.2
     */
    public IslamicCalendar(ULocale locale)
    {
        this(TimeZone.getDefault(), locale);
    }

    /**
     * Constructs an <code>IslamicCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     * @param aLocale the given locale.
     * @stable ICU 2.8
     */
    public IslamicCalendar(TimeZone zone, Locale aLocale)
    {
        super(zone, aLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Constructs an <code>IslamicCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     * @param locale the given ulocale.
     * @stable ICU 3.2
     */
    public IslamicCalendar(TimeZone zone, ULocale locale)
    {
        super(zone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Constructs an <code>IslamicCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     * @stable ICU 2.8
     */
    public IslamicCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault());
        this.setTime(date);
    }

    /**
     * Constructs an <code>IslamicCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param year the value used to set the {@link #YEAR YEAR} time field in the calendar.
     * @param month the value used to set the {@link #MONTH MONTH} time field in the calendar.
     *              Note that the month value is 0-based. e.g., 0 for Muharram.
     * @param date the value used to set the {@link #DATE DATE} time field in the calendar.
     * @stable ICU 2.8
     */
    public IslamicCalendar(int year, int month, int date)
    {
        super(TimeZone.getDefault(), ULocale.getDefault());
        this.set(Calendar.YEAR, year);
        this.set(Calendar.MONTH, month);
        this.set(Calendar.DATE, date);
    }

    /**
     * Constructs an <code>IslamicCalendar</code> with the given date
     * and time set for the default time zone with the default locale.
     *
     * @param year  the value used to set the {@link #YEAR YEAR} time field in the calendar.
     * @param month the value used to set the {@link #MONTH MONTH} time field in the calendar.
     *              Note that the month value is 0-based. e.g., 0 for Muharram.
     * @param date  the value used to set the {@link #DATE DATE} time field in the calendar.
     * @param hour  the value used to set the {@link #HOUR_OF_DAY HOUR_OF_DAY} time field
     *              in the calendar.
     * @param minute the value used to set the {@link #MINUTE MINUTE} time field
     *              in the calendar.
     * @param second the value used to set the {@link #SECOND SECOND} time field
     *              in the calendar.
     * @stable ICU 2.8
     */
    public IslamicCalendar(int year, int month, int date, int hour,
                             int minute, int second)
    {
        super(TimeZone.getDefault(), ULocale.getDefault());
        this.set(Calendar.YEAR, year);
        this.set(Calendar.MONTH, month);
        this.set(Calendar.DATE, date);
        this.set(Calendar.HOUR_OF_DAY, hour);
        this.set(Calendar.MINUTE, minute);
        this.set(Calendar.SECOND, second);
    }

    /**
     * Determines whether this object uses the fixed-cycle Islamic civil calendar
     * or an approximation of the religious, astronomical calendar.
     *
     * @param beCivil   <code>true</code> to use the civil calendar,
     *                  <code>false</code> to use the astronomical calendar.
     * @stable ICU 2.8
     */
    public void setCivil(boolean beCivil)
    {
        if (civil != beCivil) {
            // The fields of the calendar will become invalid, because the calendar
            // rules are different
            long m = getTimeInMillis();
            civil = beCivil;
            clear();
            setTimeInMillis(m);
        }
    }
    
    /**
     * Returns <code>true</code> if this object is using the fixed-cycle civil
     * calendar, or <code>false</code> if using the religious, astronomical
     * calendar.
     * @stable ICU 2.8
     */
    public boolean isCivil() {
        return civil;
    }
    
    //-------------------------------------------------------------------------
    // Minimum / Maximum access functions
    //-------------------------------------------------------------------------

    // Note: Current IslamicCalendar implementation does not work
    // well with negative years.

    private static final int LIMITS[][] = {
        // Minimum  Greatest     Least   Maximum
        //           Minimum   Maximum
        {        0,        0,        0,        0}, // ERA
        {        1,        1,  5000000,  5000000}, // YEAR
        {        0,        0,       11,       11}, // MONTH
        {        1,        1,       50,       51}, // WEEK_OF_YEAR
        {/*                                   */}, // WEEK_OF_MONTH
        {        1,        1,       29,       30}, // DAY_OF_MONTH
        {        1,        1,      354,      355}, // DAY_OF_YEAR
        {/*                                   */}, // DAY_OF_WEEK
        {       -1,       -1,        5,        5}, // DAY_OF_WEEK_IN_MONTH
        {/*                                   */}, // AM_PM
        {/*                                   */}, // HOUR
        {/*                                   */}, // HOUR_OF_DAY
        {/*                                   */}, // MINUTE
        {/*                                   */}, // SECOND
        {/*                                   */}, // MILLISECOND
        {/*                                   */}, // ZONE_OFFSET
        {/*                                   */}, // DST_OFFSET
        {        1,        1,  5000000,  5000000}, // YEAR_WOY
        {/*                                   */}, // DOW_LOCAL
        {        1,        1,  5000000,  5000000}, // EXTENDED_YEAR
        {/*                                   */}, // JULIAN_DAY
        {/*                                   */}, // MILLISECONDS_IN_DAY
    };

    /**
     * @stable ICU 2.8
     */
    protected int handleGetLimit(int field, int limitType) {
        return LIMITS[field][limitType];
    }

    //-------------------------------------------------------------------------
    // Assorted calculation utilities
    //

// Unused code - Alan 2003-05
//    /**
//     * Find the day of the week for a given day
//     *
//     * @param day   The # of days since the start of the Islamic calendar.
//     */
//    // private and uncalled, perhaps not used yet?
//    private static final int absoluteDayToDayOfWeek(long day)
//    {
//        // Calculate the day of the week.
//        // This relies on the fact that the epoch was a Thursday.
//        int dayOfWeek = (int)(day + THURSDAY) % 7 + SUNDAY;
//        if (dayOfWeek < 0) {
//            dayOfWeek += 7;
//        }
//        return dayOfWeek;
//    }

    /**
     * Determine whether a year is a leap year in the Islamic civil calendar
     */
    private final static boolean civilLeapYear(int year)
    {
        return (14 + 11 * year) % 30 < 11;
        
    }
    
    /**
     * Return the day # on which the given year starts.  Days are counted
     * from the Hijri epoch, origin 0.
     */
    private long yearStart(int year) {
        if (civil) {
            return (year-1)*354 + (long)Math.floor((3+11*year)/30.0);
        } else {
            return trueMonthStart(12*(year-1));
        }
    }
    
    /**
     * Return the day # on which the given month starts.  Days are counted
     * from the Hijri epoch, origin 0.
     *
     * @param year  The hijri year
     * @param month  The hijri month, 0-based
     */
    private long monthStart(int year, int month) {
        if (civil) {
            return (long)Math.ceil(29.5*month)
                    + (year-1)*354 + (long)Math.floor((3+11*year)/30.0);
        } else {
            return trueMonthStart(12*(year-1) + month);
        }
    }
    
    /**
     * Find the day number on which a particular month of the true/lunar
     * Islamic calendar starts.
     *
     * @param month The month in question, origin 0 from the Hijri epoch
     *
     * @return The day number on which the given month starts.
     */
    private static final long trueMonthStart(long month)
    {
        long start = cache.get(month);

        if (start == CalendarCache.EMPTY)
        {
            // Make a guess at when the month started, using the average length
            long origin = HIJRA_MILLIS 
                        + (long)Math.floor(month * CalendarAstronomer.SYNODIC_MONTH) * ONE_DAY;

            double age = moonAge(origin);

            if (moonAge(origin) >= 0) {
                // The month has already started
                do {
                    origin -= ONE_DAY;
                    age = moonAge(origin);
                } while (age >= 0);
            }
            else {
                // Preceding month has not ended yet.
                do {
                    origin += ONE_DAY;
                    age = moonAge(origin);
                } while (age < 0);
            }

            start = (origin - HIJRA_MILLIS) / ONE_DAY + 1;
            
            cache.put(month, start);
        }
        return start;
    }

    /**
     * Return the "age" of the moon at the given time; this is the difference
     * in ecliptic latitude between the moon and the sun.  This method simply
     * calls CalendarAstronomer.moonAge, converts to degrees, 
     * and adjusts the resultto be in the range [-180, 180].
     *
     * @param time  The time at which the moon's age is desired,
     *              in millis since 1/1/1970.
     */
    static final double moonAge(long time)
    {
        double age = 0;
        
        synchronized(astro) {
            astro.setTime(time);
            age = astro.getMoonAge();
        }
        // Convert to degrees and normalize...
        age = age * 180 / Math.PI;
        if (age > 180) {
            age = age - 360;
        }

        return age;
    }

    //-------------------------------------------------------------------------
    // Internal data....
    //
    
    // And an Astronomer object for the moon age calculations
    private static CalendarAstronomer astro = new CalendarAstronomer();
    
    private static CalendarCache cache = new CalendarCache();
    
    /**
     * <code>true</code> if this object uses the fixed-cycle Islamic civil calendar,
     * and <code>false</code> if it approximates the true religious calendar using
     * astronomical calculations for the time of the new moon.
     *
     * @serial
     */
    private boolean civil = true;

    //----------------------------------------------------------------------
    // Calendar framework
    //----------------------------------------------------------------------

    /**
     * Return the length (in days) of the given month.
     *
     * @param extendedYear  The hijri year
     * @param month The hijri month, 0-based
     * @stable ICU 2.8
     */
    protected int handleGetMonthLength(int extendedYear, int month) {

        int length = 0;
        
        if (civil) {
            length = 29 + (month+1) % 2;
            if (month == DHU_AL_HIJJAH && civilLeapYear(extendedYear)) {
                length++;
            }
        } else {
            month = 12*(extendedYear-1) + month;
            length = (int)( trueMonthStart(month+1) - trueMonthStart(month) );
        }
        return length;
    }

    /**
     * Return the number of days in the given Islamic year
     * @stable ICU 2.8
     */
    protected int handleGetYearLength(int extendedYear) {
        if (civil) {
            return 354 + (civilLeapYear(extendedYear) ? 1 : 0);
        } else {
            int month = 12*(extendedYear-1);
            return (int)(trueMonthStart(month + 12) - trueMonthStart(month));
        }
    }
    
    //-------------------------------------------------------------------------
    // Functions for converting from field values to milliseconds....
    //-------------------------------------------------------------------------

    // Return JD of start of given month/year
    /**
     * @stable ICU 2.8
     */
    protected int handleComputeMonthStart(int eyear, int month, boolean useMonth) {
        return (int) monthStart(eyear, month) + 1948439;
    }    

    //-------------------------------------------------------------------------
    // Functions for converting from milliseconds to field values
    //-------------------------------------------------------------------------

    /**
     * @stable ICU 2.8
     */
    protected int handleGetExtendedYear() {
        int year;
        if (newerField(EXTENDED_YEAR, YEAR) == EXTENDED_YEAR) {
            year = internalGet(EXTENDED_YEAR, 1); // Default to year 1
        } else {
            year = internalGet(YEAR, 1); // Default to year 1
        }
        return year;
    }

    /**
     * Override Calendar to compute several fields specific to the Islamic
     * calendar system.  These are:
     *
     * <ul><li>ERA
     * <li>YEAR
     * <li>MONTH
     * <li>DAY_OF_MONTH
     * <li>DAY_OF_YEAR
     * <li>EXTENDED_YEAR</ul>
     * 
     * The DAY_OF_WEEK and DOW_LOCAL fields are already set when this
     * method is called. The getGregorianXxx() methods return Gregorian
     * calendar equivalents for the given Julian day.
     * @stable ICU 2.8
     */
    protected void handleComputeFields(int julianDay) {
        int year, month, dayOfMonth, dayOfYear;
        long monthStart;
        long days = julianDay - 1948440;

        if (civil) {
            // Use the civil calendar approximation, which is just arithmetic
            year  = (int)Math.floor( (30 * days + 10646) / 10631.0 );
            month = (int)Math.ceil((days - 29 - yearStart(year)) / 29.5 );
            month = Math.min(month, 11);
            monthStart = monthStart(year, month);
        } else {
            // Guess at the number of elapsed full months since the epoch
            int months = (int)Math.floor(days / CalendarAstronomer.SYNODIC_MONTH);

            monthStart = (long)Math.floor(months * CalendarAstronomer.SYNODIC_MONTH - 1);

            if ( days - monthStart >= 25 && moonAge(internalGetTimeInMillis()) > 0) {
                // If we're near the end of the month, assume next month and search backwards
                months++;
            }

            // Find out the last time that the new moon was actually visible at this longitude
            // This returns midnight the night that the moon was visible at sunset.
            while ((monthStart = trueMonthStart(months)) > days) {
                // If it was after the date in question, back up a month and try again
                months--;
            }

            year = months / 12 + 1;
            month = months % 12;
        }

        dayOfMonth = (int)(days - monthStart(year, month)) + 1;

        // Now figure out the day of the year.
        dayOfYear = (int)(days - monthStart(year, 0) + 1);

        internalSet(ERA, 0);
        internalSet(YEAR, year);
        internalSet(EXTENDED_YEAR, year);
        internalSet(MONTH, month);
        internalSet(DAY_OF_MONTH, dayOfMonth);
        internalSet(DAY_OF_YEAR, dayOfYear);       
    }    

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    public String getType() {
        return "islamic";
    }

    /*
    private static CalendarFactory factory;
    public static CalendarFactory factory() {
        if (factory == null) {
            factory = new CalendarFactory() {
                public Calendar create(TimeZone tz, ULocale loc) {
                    return new IslamicCalendar(tz, loc);
                }

                public String factoryName() {
                    return "Islamic";
                }
            };
        }
        return factory;
    }
    */
}
