/*
 *******************************************************************************
 * Copyright (C) 2005-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.util.Date;
import java.util.Locale;

/**
 * Base class for EthiopicCalendar and CopticCalendar.
 * @internal
 */
class CECalendar extends Calendar {
    // jdk1.4.2 serialver
    private static final long serialVersionUID = -999547623066414271L;

    private static final int LIMITS[][] = {
        // Minimum  Greatest    Least  Maximum
        //           Minimum  Maximum
        {        0,        0,       1,       1 }, // ERA
        {        1,        1, 5828963, 5838270 }, // YEAR
        {        0,        0,      12,      12 }, // MONTH
        {        1,        1,      52,      53 }, // WEEK_OF_YEAR
        {        0,        0,       0,       6 }, // WEEK_OF_MONTH
        {        1,        1,       5,      30 }, // DAY_OF_MONTH
        {        1,        1,     365,     366 }, // DAY_OF_YEAR
        {/*                                  */}, // DAY_OF_WEEK
        {       -1,       -1,       1,       5 }, // DAY_OF_WEEK_IN_MONTH
        {/*                                  */}, // AM_PM
        {/*                                  */}, // HOUR
        {/*                                  */}, // HOUR_OF_DAY
        {/*                                  */}, // MINUTE
        {/*                                  */}, // SECOND
        {/*                                  */}, // MILLISECOND
        {/*                                  */}, // ZONE_OFFSET
        {/*                                  */}, // DST_OFFSET
        { -5838270, -5838270, 5828964, 5838271 }, // YEAR_WOY
        {/*                                  */}, // DOW_LOCAL
        { -5838269, -5838269, 5828963, 5838270}, // EXTENDED_YEAR
        {/*                                  */}, // JULIAN_DAY
        {/*                                  */}, // MILLISECONDS_IN_DAY
    };

    /* ceToJD() doesn't use this data */
    /*private static final int[][] ceMONTH_COUNT = {
        //len len2 st  st2
        {30, 30,   0,   0}, // Meskerem
        {30, 30,  30,  30}, // Tekemt 
        {30, 30,  60,  60}, // Hedar 
        {30, 30,  90,  90}, // Tahsas 
        {30, 30, 120, 120}, // Ter 
        {30, 30, 150, 150}, // Yekatit
        {30, 30, 180, 180}, // Megabit
        {30, 30, 210, 210}, // Miazia
        {30, 30, 240, 244}, // Genbot
        {30, 30, 270, 270}, // Sene 
        {30, 30, 300, 300}, // Hamle 
        {30, 30, 330, 330}, // Nehasse 
        { 5,  6, 360, 360}  // Pwagme
        // len  length of month
        // len2 length of month in a leap year
        // st   days in year before start of month
        // st2  days in year before month in leap year
    };*/
    
    // The Coptic and Ethiopic calendars differ only in their epochs.
    // We handle this by setting the jdOffset to the difference between
    // the Julian and Coptic or Ethiopic epoch.
    // This value is set in the class initialization phase of the two
    // subclasses, CopticCalendar and EthiopicCalendar
    /**
     * The difference between the Julian and Coptic epoch.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected int jdEpochOffset  = -1;
    

    protected int handleGetLimit(int field, int limitType) {
        return LIMITS[field][limitType];
    }
    
    //-------------------------------------------------------------------------
    // Constructors...
    //-------------------------------------------------------------------------

    /**
     * Constructs a default <code>CECalendar</code> using the current time
     * in the default time zone with the default locale.
     */
    protected CECalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault());
    }

    /**
     * Constructs a <code>CECalendar</code> based on the current time
     * in the given time zone with the default locale.
     *
     * @param zone The time zone for the new calendar.
     */
    protected CECalendar(TimeZone zone) {
        this(zone, ULocale.getDefault());
    }

    /**
     * Constructs a <code>CECalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale The locale for the new calendar.
     */
    protected CECalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    /**
     * Constructs a <code>CECalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param locale The locale for the new calendar.
     */
    protected CECalendar(ULocale locale) {
        this(TimeZone.getDefault(), locale);
    }

    /**
     * Constructs a <code>CECalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone The time zone for the new calendar.
     *
     * @param aLocale The locale for the new calendar.
     */
    protected CECalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Constructs a <code>CECalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone The time zone for the new calendar.
     *
     * @param locale The locale for the new calendar.
     */
    protected CECalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Constructs a <code>CECalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for Tishri.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     */
    protected CECalendar(int year, int month, int date) {
        super(TimeZone.getDefault(), ULocale.getDefault());
        this.set(year, month, date);
    }

    /**
     * Constructs a <code>CECalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     */
    protected CECalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault());
        this.setTime(date);
    }

    /**
     * Constructs a <code>CECalendar</code> with the given date
     * and time set for the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for Tishri.
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     * @param hour      The value used to set the calendar's {@link #HOUR_OF_DAY HOUR_OF_DAY} time field.
     * @param minute    The value used to set the calendar's {@link #MINUTE MINUTE} time field.
     * @param second    The value used to set the calendar's {@link #SECOND SECOND} time field.
     */
    protected CECalendar(int year, int month, int date, int hour,
                         int minute, int second)
    {
        super(TimeZone.getDefault(), ULocale.getDefault());
        this.set(year, month, date, hour, minute, second);
    }
    
    
    //-------------------------------------------------------------------------
    // Calendar system Converstion methods...
    //-------------------------------------------------------------------------

    /**
     * @internal
     */
    protected int handleComputeMonthStart(int eyear,
                                          int emonth,
                                          boolean useMonth) {
        return ceToJD(eyear, emonth, 0, jdEpochOffset);
    }

    /**
     * @internal
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
     * @internal
     */
    protected void handleComputeFields(int julianDay) {
        Integer[] date = getDateFromJD(julianDay, jdEpochOffset);
        int _year  = date[0].intValue();
        int _month = date[1].intValue();
        int _day   = date[2].intValue();
        int ceyear = 0;

        // Do we want to use EthiopicCalendar.AA, .AM here?
        int era = GregorianCalendar.AD;
        if (_year < 0) { // dlf: this is what the test says to do
            era   = GregorianCalendar.BC;
            ceyear = 1 - _year;
        } else {
            ceyear = _year;
        }

        internalSet(MONTH, _month);
        internalSet(DAY_OF_MONTH, _day);
        internalSet(DAY_OF_YEAR, (30 * _month) + _day);
        internalSet(EXTENDED_YEAR, ceyear);
        internalSet(ERA, era);
        internalSet(YEAR, _year);
    }

    /**
     * @internal
     */
    public static int ceToJD(long year, int month, int date, int jdEpochOffset) {

        // Julian<->Ethiopic algorithms from:
        // "Calendars in Ethiopia", Berhanu Beyene, Manfred Kudlek, International Conference
        // of Ethiopian Studies XV, Hamburg, 2003

        return (int) (
            (jdEpochOffset+365)     // difference from Julian epoch to 1,1,1
            + 365 * (year - 1)      // number of days from years
            + quotient(year, 4)     // extra day of leap year
            + 30 * (month + 1)      // number of days from months
            + date                  // number of days for present month
            - 31                    // slack?
            );
    }

    /**
     * @internal
     * @provisional This API might change or be removed in a future release.
     */
    public static Integer[] getDateFromJD(int julianDay, int jdEpochOffset) {
        // 1461 is the number of days in 4 years
        long r4 = mod(julianDay - jdEpochOffset, 1461); // number of days within a 4 year period
        long  n = mod(r4, 365) + 365 * quotient(r4, 1460);  // days in present year

        long aprime = 4   // number of years in the leap year cycle
            * quotient(julianDay - jdEpochOffset, 1461)  // number of 4 year periods between epochs?
            + quotient(r4, 365)   // number of regular years?
            - quotient(r4, 1460)  // number of 4 year periods?
            - 1;

        int _year   = (int) (aprime + 1);
        int _month  = (int) (quotient(n, 30));
        int _day    = mod(n, 30) + 1;

        return new Integer[]{ new Integer(_year), new Integer(_month), new Integer(_day) };
    }
 
    /**
     * These utility functions can be replaced by equivalent 
     * functions from ICU if available.
     */
    static int mod(long i, int j) {
        return (int) (i - (long) j * quotient(i, j));
    }
    
    static int quotient(long i, int j) {
        return (int) Math.floor((double) i / j);
    }
}
