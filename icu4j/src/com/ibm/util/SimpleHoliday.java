/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/Attic/SimpleHoliday.java,v $ 
 * $Date: 2000/03/21 02:19:33 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */

package com.ibm.util;

import java.util.Date;
import com.ibm.util.Calendar;
import com.ibm.util.GregorianCalendar;

/**
 * A holiday whose date can be represented by a SimpleDateRule object
 *
 * @see SimpleDateRule
 */
public class SimpleHoliday extends Holiday {
    /**
     * Construct an object representing a holiday
     *
     * @param month         The month in which this holiday occurs (0-based)
     * @param dayOfMonth    The date within the month (1-based).
     *
     * @param name  The name of this holiday.  This string is used as a key
     *              to look up the holiday's name a resource bundle.
     *              If the name is not found in the resource bundle,
     *              getDisplayName will return this string instead.
     *
     * @see SimpleDateRule
     * @see Holiday#getDisplayName(java.util.Locale)
     *
     */
    public SimpleHoliday(int month, int dayOfMonth, String name)
    {
        super(name, new SimpleDateRule(month, dayOfMonth));
    }

    /**
     * Construct an object representing a holiday
     *
     * @param month         The month in which this holiday occurs (0-based)
     * @param dayOfMonth    The date within the month (1-based).
     *
     * @param name  The name of this holiday.  This string is used as a key
     *              to look up the holiday's name a resource bundle.
     *              If the name is not found in the resource bundle,
     *              getDisplayName will return this string instead.
     *
     * @see SimpleDateRule
     * @see Holiday#getDisplayName(java.util.Locale)
     *
     */
    public SimpleHoliday(int month, int dayOfMonth, String name,
                            int startYear)
    {
        super(name, null);

        setRange(startYear, 0, new SimpleDateRule(month, dayOfMonth) );
    }

    /**
     * Construct an object representing a holiday
     *
     * @param month         The month in which this holiday occurs (0-based)
     * @param dayOfMonth    The date within the month (1-based).
     *
     * @param name  The name of this holiday.  This string is used as a key
     *              to look up the holiday's name a resource bundle.
     *              If the name is not found in the resource bundle,
     *              getDisplayName will return this string instead.
     *
     * @see SimpleDateRule
     * @see Holiday#getDisplayName(java.util.Locale)
     *
     */
    public SimpleHoliday(int month, int dayOfMonth, String name,
                            int startYear, int endYear)
    {
        super(name, null);

        setRange(startYear, endYear, new SimpleDateRule(month, dayOfMonth) );
    }

    /** // TODO: remove
     * Construct an object representing a holiday
     *
     * @param month The month in which this holiday occurs (0-based)
     *
     * @param dayOfMonth A date within the month (1-based).  The
     *      interpretation of this parameter depends on the value of
     *      <code>dayOfWeek</code>.
     *
     * @param dayOfWeek The day of the week on which this holiday occurs.
     *      The following values are legal: <ul>
     *      <li>dayOfWeek == 0 - use dayOfMonth only
     *      <li>dayOfWeek < 0  - use last -dayOfWeek before or on dayOfMonth
     *      <li>dayOfWeek > 0  - use first dayOfWeek after or on dayOfMonth
     *      </ul>
     *
     * @param name  The name of this holiday.  This string is used as a key
     *              to look up the holiday's name a resource bundle.
     *              If the name is not found in the resource bundle,
     *              getDisplayName will return this string instead.
     *
     * @see SimpleDateRule
     * @see Holiday#getDisplayName(java.util.Locale)
     *
     */
    public SimpleHoliday(int month, int dayOfMonth, int dayOfWeek, String name)
    {
        super(name, new SimpleDateRule(month, dayOfMonth,
                                        dayOfWeek > 0 ? dayOfWeek : - dayOfWeek,
                                        dayOfWeek > 0) );
    }


    public SimpleHoliday(int month, int dayOfMonth, int dayOfWeek, String name,
                        int startYear)
    {
        super(name, null);

        setRange(startYear, 0, new SimpleDateRule(month, dayOfMonth,
                                            dayOfWeek > 0 ? dayOfWeek : - dayOfWeek,
                                            dayOfWeek > 0) );
    }


    public SimpleHoliday(int month, int dayOfMonth, int dayOfWeek, String name,
                        int startYear, int endYear)
    {
        super(name, null);

        setRange(startYear, endYear, new SimpleDateRule(month, dayOfMonth,
                                            dayOfWeek > 0 ? dayOfWeek : - dayOfWeek,
                                            dayOfWeek > 0) );
    }

    private void setRange(int startYear, int endYear, DateRule rule)
    {
        RangeDateRule rangeRule = new RangeDateRule();

        if (startYear != 0) {
            Calendar start = new GregorianCalendar(startYear, Calendar.JANUARY, 1);
            rangeRule.add(start.getTime(), rule);
        } else {
            rangeRule.add(rule);
        }
        if (endYear != 0) {
            Date end = new GregorianCalendar(endYear, Calendar.DECEMBER, 31).getTime();
            rangeRule.add(end, null);
        }

        setRule(rangeRule);
    }

    /* Constants for holidays that are common throughout the Western
     * and Christian worlds.... */

    /**
     * New Year's Day - January 1st
     */
    public static final SimpleHoliday NEW_YEARS_DAY =
        new SimpleHoliday(Calendar.JANUARY,    1,  "New Year's Day");

    /**
     * Epiphany, January 6th
     */
    public static final SimpleHoliday EPIPHANY =
        new SimpleHoliday(Calendar.JANUARY,    6,  "Epiphany");

    /**
     * May Day, May 1st
     */
    public static final SimpleHoliday MAY_DAY =
        new SimpleHoliday(Calendar.MAY,        1,  "May Day");

    /**
     * Assumption, August 15th
     */
    public static final SimpleHoliday ASSUMPTION =
        new SimpleHoliday(Calendar.AUGUST,    15,  "Assumption");

    /**
     * All Saints' Day, November 1st
     */
    public static final SimpleHoliday ALL_SAINTS_DAY =
        new SimpleHoliday(Calendar.NOVEMBER,   1,  "All Saints' Day");

    /**
     * All Souls' Day, November 1st
     */
    public static final SimpleHoliday ALL_SOULS_DAY =
        new SimpleHoliday(Calendar.NOVEMBER,   2,  "All Souls' Day");

    /**
     * Immaculate Conception, December 8th
     */
    public static final SimpleHoliday IMMACULATE_CONCEPTION =
        new SimpleHoliday(Calendar.DECEMBER,   8,  "Immaculate Conception");

    /**
     * Christmas Eve, December 24th
     */
    public static final SimpleHoliday CHRISTMAS_EVE =
        new SimpleHoliday(Calendar.DECEMBER,  24,  "Christmas Eve");

    /**
     * Christmas, December 25th
     */
    public static final SimpleHoliday CHRISTMAS =
        new SimpleHoliday(Calendar.DECEMBER,  25,  "Christmas");

    /**
     * Boxing Day, December 26th
     */
    public static final SimpleHoliday BOXING_DAY =
        new SimpleHoliday(Calendar.DECEMBER,  26,  "Boxing Day");

    /**
     * Saint Stephen's Day, December 26th
     */
    public static final SimpleHoliday ST_STEPHENS_DAY =
        new SimpleHoliday(Calendar.DECEMBER,  26,  "St. Stephen's Day");

    /**
     * New Year's Eve, December 31st
     */
    public static final SimpleHoliday NEW_YEARS_EVE =
        new SimpleHoliday(Calendar.DECEMBER,  31,  "New Year's Eve");

}
