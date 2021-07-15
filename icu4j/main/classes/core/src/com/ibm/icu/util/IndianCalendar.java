// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.util.ULocale.Category;

/**
 * <code>IndianCalendar</code> is a subclass of <code>GregorianCalendar</code>
 * that numbers years since the birth of the Buddha.  This is the civil calendar
 * which is accepted by government of India as Indian National Calendar. 
 * The two calendars most widely used in India today are the Vikrama calendar 
 * followed in North India and the Shalivahana or Saka calendar which is followed 
 * in South India and Maharashtra.

 * A variant of the Shalivahana Calendar was reformed and standardized as the 
 * Indian National calendar in 1957.
 * <p>
 * Some details of Indian National Calendar (to be implemented) :
 * The Months
 * Month          Length      Start date (Gregorian)
 * =================================================
 * 1 Chaitra      30/31          March 22*
 * 2 Vaisakha     31             April 21
 * 3 Jyaistha     31             May 22
 * 4 Asadha       31             June 22
 * 5 Sravana      31             July 23
 * 6 Bhadra       31             August 23
 * 7 Asvina       30             September 23
 * 8 Kartika      30             October 23
 * 9 Agrahayana   30             November 22
 * 10 Pausa       30             December 22
 * 11 Magha       30             January 21
 * 12 Phalguna    30             February 20

 * In leap years, Chaitra has 31 days and starts on March 21 instead.
 * The leap years of Gregorian calendar and Indian National Calendar are in synchornization. 
 * So When its a leap year in Gregorian calendar then Chaitra has 31 days.
 *
 * The Years
 * Years are counted in the Saka Era, which starts its year 0 in 78AD (by gregorian calendar).
 * So for eg. 9th June 2006 by Gregorian Calendar, is same as 19th of Jyaistha in 1928 of Saka 
 * era by Indian National Calendar.
 * <p>
 * The Indian Calendar has only one allowable era: <code>Saka Era</code>.  If the
 * calendar is not in lenient mode (see <code>setLenient</code>), dates before
 * 1/1/1 Saka Era are rejected with an <code>IllegalArgumentException</code>.
 * <p>
 * This class should not be subclassed.</p>
 * <p>
 * IndianCalendar usually should be instantiated using 
 * {@link com.ibm.icu.util.Calendar#getInstance(ULocale)} passing in a <code>ULocale</code>
 * with the tag <code>"@calendar=Indian"</code>.</p>
 * 
 * @see com.ibm.icu.util.Calendar
 * @see com.ibm.icu.util.GregorianCalendar
 *
 * @stable ICU 3.8
 */
public class IndianCalendar extends Calendar {
    // jdk1.4.2 serialver
    private static final long serialVersionUID = 3617859668165014834L;

    /** 
     * Constant for Chaitra, the 1st month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int CHAITRA = 0;

    /** 
     * Constant for Vaisakha, the 2nd month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int VAISAKHA = 1;

    /** 
     * Constant for Jyaistha, the 3rd month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int JYAISTHA = 2;

    /** 
     * Constant for Asadha, the 4th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int ASADHA = 3; 

    /** 
     * Constant for Sravana, the 5th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int SRAVANA = 4 ;

    /** 
     * Constant for Bhadra, the 6th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int BHADRA = 5 ;

    /** 
     * Constant for Asvina, the 7th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int ASVINA = 6 ;

    /** 
     * Constant for Kartika, the 8th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int KARTIKA = 7 ;

    /** 
     * Constant for Agrahayana, the 9th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int AGRAHAYANA = 8 ;

    /** 
     * Constant for Pausa, the 10th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int PAUSA = 9 ;

    /** 
     * Constant for Magha, the 11th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int MAGHA = 10;

    /** 
     * Constant for Phalguna, the 12th month of the Indian year. 
     * @stable ICU 3.8
     */
    public static final int PHALGUNA = 11;
    
    //-------------------------------------------------------------------------
    // Constructors...
    //-------------------------------------------------------------------------

    /**
     * Constant for the Indian Era.  This is the only allowable <code>ERA</code>
     * value for the Indian calendar.
     *
     * @see com.ibm.icu.util.Calendar#ERA
     * @stable ICU 3.8
     */
    public static final int IE = 0;
    
    /**
     * Constructs a <code>IndianCalendar</code> using the current time
     * in the default time zone with the default <code>FORMAT</code> locale.
     * @see Category#FORMAT
     * @stable ICU 3.8
     */
    public IndianCalendar() {
       this(TimeZone.getDefault(), ULocale.getDefault(Category.FORMAT));
    }

    /**
     * Constructs a <code>IndianCalendar</code> based on the current time
     * in the given time zone with the default <code>FORMAT</code> locale.
     *
     * @param zone the given time zone.
     * @see Category#FORMAT
     * @stable ICU 3.8
     */
    public IndianCalendar(TimeZone zone) {
       this(zone, ULocale.getDefault(Category.FORMAT));
    }

    /**
     * Constructs a <code>IndianCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale the given locale.
     * @stable ICU 3.8
     */
    public IndianCalendar(Locale aLocale) {
        this(TimeZone.forLocaleOrDefault(aLocale), aLocale);
    }

    /**
     * Constructs a <code>IndianCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param locale the given ulocale.
     * @stable ICU 3.8
     */
    public IndianCalendar(ULocale locale) {
        this(TimeZone.forULocaleOrDefault(locale), locale);
    }

    /**
     * Constructs a <code>IndianCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     *
     * @param aLocale the given locale.
     * @stable ICU 3.8
     */
    public IndianCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Constructs a <code>IndianCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     *
     * @param locale the given ulocale.
     * @stable ICU 3.8
     */
    public IndianCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Constructs a <code>IndianCalendar</code> with the given date set
     * in the default time zone with the default <code>FORMAT</code> locale.
     *
     * @param date      The date to which the new calendar is set.
     * @see Category#FORMAT
     * @stable ICU 3.8
     */
    public IndianCalendar(Date date) {
        super(TimeZone.getDefault(), ULocale.getDefault(Category.FORMAT));
        this.setTime(date);
    }

    /**
     * Constructs a <code>IndianCalendar</code> with the given date set
     * in the default time zone with the default <code>FORMAT</code> locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for January.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     * @see Category#FORMAT
     * @stable ICU 3.8
     */
    public IndianCalendar(int year, int month, int date) {
       super(TimeZone.getDefault(), ULocale.getDefault(Category.FORMAT));
       this.set(Calendar.YEAR, year);
       this.set(Calendar.MONTH, month);
       this.set(Calendar.DATE, date);

    }

    /**
     * Constructs a IndianCalendar with the given date
     * and time set for the default time zone with the default <code>FORMAT</code> locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for January.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     *
     * @param hour      The value used to set the calendar's {@link #HOUR_OF_DAY HOUR_OF_DAY} time field.
     *
     * @param minute    The value used to set the calendar's {@link #MINUTE MINUTE} time field.
     *
     * @param second    The value used to set the calendar's {@link #SECOND SECOND} time field.
     * @see Category#FORMAT
     * @stable ICU 3.8
     */
    public IndianCalendar(int year, int month, int date, int hour,
                             int minute, int second)
    {
       super(TimeZone.getDefault(), ULocale.getDefault(Category.FORMAT));
       this.set(Calendar.YEAR, year);
       this.set(Calendar.MONTH, month);
       this.set(Calendar.DATE, date);
       this.set(Calendar.HOUR_OF_DAY, hour);
       this.set(Calendar.MINUTE, minute);
       this.set(Calendar.SECOND, second);
    }


    //-------------------------------------------------------------------------
    // The only practical difference from a Gregorian calendar is that years
    // are numbered since the Saka Era.  A couple of overrides will
    // take care of that....
    //-------------------------------------------------------------------------
    
    // Starts in 78 AD, 
    private static final int INDIAN_ERA_START = 78;
    
    // The Indian year starts 80 days later than the Gregorian year.
    private static final int INDIAN_YEAR_START = 80;

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    protected int handleGetExtendedYear() {
        int year;
        
        if (newerField(EXTENDED_YEAR, YEAR) == EXTENDED_YEAR) {
            year = internalGet(EXTENDED_YEAR, 1);
        } else {
            // Ignore the era, as there is only one
            year = internalGet(YEAR, 1);
        }
        
        return year;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    protected int handleGetYearLength(int extendedYear) {
       return super.handleGetYearLength(extendedYear);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    protected int handleGetMonthLength(int extendedYear, int month) {
        if (month < 0 || month > 11) {
            int[] remainder = new int[1];
            extendedYear += floorDivide(month, 12, remainder);
            month = remainder[0];
        }

        if(isGregorianLeapYear(extendedYear + INDIAN_ERA_START) && month == 0) {
            return 31;
        }

        if(month >= 1 && month <=5) {
            return 31;
        }

        return 30;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    protected void handleComputeFields(int julianDay){
        double jdAtStartOfGregYear;
        int leapMonth, IndianYear, yday, IndianMonth, IndianDayOfMonth, mday;
        computeGregorianFields(julianDay);
        int gregorianYear = getGregorianYear(); // Stores gregorian date corresponding to Julian day;
        IndianYear = gregorianYear - INDIAN_ERA_START;            // Year in Saka era

        jdAtStartOfGregYear = gregorianToJD(gregorianYear, 0 /* first month in 0 base */, 1); // JD at start of Gregorian year
        yday = (int)(julianDay - jdAtStartOfGregYear);              // Day number in Gregorian year (starting from 0)

        if (yday < INDIAN_YEAR_START) {
            //  Day is at the end of the preceding Saka year
            IndianYear -= 1;
            leapMonth = isGregorianLeapYear(gregorianYear - 1) ? 31 : 30; // Days in leapMonth this year, previous Gregorian year
            yday += leapMonth + (31 * 5) + (30 * 3) + 10;
        } else {
            leapMonth = isGregorianLeapYear(gregorianYear) ? 31 : 30; // Days in leapMonth this year
            yday -= INDIAN_YEAR_START;
        }

        if (yday < leapMonth) {
            IndianMonth = 0;
            IndianDayOfMonth = yday + 1;
        } else {
              mday = yday - leapMonth;
              if (mday < (31 * 5)) {
                 IndianMonth = mday / 31 + 1;
                 IndianDayOfMonth = (mday % 31) + 1;
              } else {
                 mday -= 31 * 5;
                 IndianMonth = mday / 30 + 6;
                 IndianDayOfMonth = (mday % 30) + 1;
              }
        }

        internalSet(ERA, 0);
        internalSet(EXTENDED_YEAR, IndianYear);
        internalSet(YEAR, IndianYear);
        internalSet(MONTH, IndianMonth);
        internalSet(DAY_OF_MONTH, IndianDayOfMonth );
        internalSet(DAY_OF_YEAR, yday + 1); // yday is 0-based
     }

    private static final int LIMITS[][] = {
        // Minimum  Greatest     Least    Maximum
        //           Minimum   Maximum
        {        0,        0,        0,        0}, // ERA
        { -5000000, -5000000,  5000000,  5000000}, // YEAR
        {        0,        0,       11,       11}, // MONTH
        {        1,        1,       52,       53}, // WEEK_OF_YEAR
        {/*                                   */}, // WEEK_OF_MONTH
        {        1,        1,       30,       31}, // DAY_OF_MONTH
        {        1,        1,      365,      366}, // DAY_OF_YEAR
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
        { -5000000, -5000000,  5000000,  5000000}, // YEAR_WOY
        {/*                                   */}, // DOW_LOCAL
        { -5000000, -5000000,  5000000,  5000000}, // EXTENDED_YEAR
        {/*                                   */}, // JULIAN_DAY
        {/*                                   */}, // MILLISECONDS_IN_DAY
    };


    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    protected int handleGetLimit(int field, int limitType) {
       return LIMITS[field][limitType];
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    protected int handleComputeMonthStart(int year, int month, boolean useMonth) {

       //month is 0 based; converting it to 1-based 
       int imonth;
       
       // If the month is out of range, adjust it into range, and adjust the extended year accordingly
       if (month < 0 || month > 11) {
           year += month / 12;
           month %= 12;
       }
       
       imonth = month + 1;  
       
       double jd = IndianToJD(year ,imonth, 1);
       
       return (int)jd;
    }


   
    /*
     * This routine converts an Indian date to the corresponding Julian date"
     * @param year   The year in Saka Era according to Indian calendar.
     * @param month  The month according to Indian calendar (between 1 to 12)
     * @param date   The date in month 
     */
    private double IndianToJD(int year, int month, int date) {
       int leapMonth, gyear, m;
       double start, jd;

       gyear = year + INDIAN_ERA_START;


       if(isGregorianLeapYear(gyear)) {
          leapMonth = 31;
          start = gregorianToJD(gyear, 2 /* third month in 0 based */, 21);
       } else {
          leapMonth = 30;
          start = gregorianToJD(gyear, 2 /* third month in 0 based */, 22);
       }

       if (month == 1) {
          jd = start + (date - 1);
       } else {
          jd = start + leapMonth;
          m = month - 2;
          m = Math.min(m, 5);
          jd += m * 31;
          if (month >= 8) {
             m = month - 7;
             jd += m * 30;
          }
          jd += date - 1;
       }

       return jd;
    }
    
    /*
     * The following function is not needed for basic calendar functioning.
     * This routine converts a gregorian date to the corresponding Julian date"
     * @param year   The year in standard Gregorian calendar (AD/BC) .
     * @param month  The month according to Gregorian calendar (between 0 to 11)
     * @param date   The date in month 
     */
    private double gregorianToJD(int year, int month, int date) {
       return computeGregorianMonthStart(year, month) + date - 0.5;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    public String getType() {
        return "indian";
    }
}
