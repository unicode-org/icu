/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import com.ibm.icu.util.TimeZone;
import java.util.Date;
import java.util.Locale;

/** 
 * <code>TaiwanCalendar</code> is a subclass of <code>GregorianCalendar</code>
 * that numbers years since 1912. 
 * <p>
 * The Taiwan calendar is identical to the Gregorian calendar in all respects
 * except for the year and era.  Years are numbered since 1912 AD (Gregorian).
 * <p>
 * The Taiwan Calendar has one era: <code>MINGUO</code>.
 * <p>
 * This class should not be subclassed.</p>
 * <p>
 * TaiwanCalendar usually should be instantiated using 
 * {@link com.ibm.icu.util.Calendar#getInstance(ULocale)} passing in a <code>ULocale</code>
 * with the tag <code>"@calendar=Taiwan"</code>.</p>
 * 
 * @see com.ibm.icu.util.Calendar
 * @see com.ibm.icu.util.GregorianCalendar
 *
 * @author Laura Werner
 * @author Alan Liu
 * @author Steven R. Loomis
 * @draft ICU 3.8
 * @provisional This API might change or be removed in a future release.
 */
public class TaiwanCalendar extends GregorianCalendar {
    // jdk1.4.2 serialver
    private static final long serialVersionUID = 2583005278132380631L;

    //-------------------------------------------------------------------------
    // Constructors...
    //-------------------------------------------------------------------------

    /**
     * Constant for the Taiwan Era.  This is the only allowable <code>ERA</code>
     * value for the Taiwan calendar.
     *
     * @see com.ibm.icu.util.Calendar#ERA
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final int MINGUO = 0;
    
    /**
     * Constructs a <code>TaiwanCalendar</code> using the current time
     * in the default time zone with the default locale.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar() {
        super();
    }

    /**
     * Constructs a <code>TaiwanCalendar</code> based on the current time
     * in the given time zone with the default locale.
     *
     * @param zone the given time zone.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar(TimeZone zone) {
        super(zone);
    }

    /**
     * Constructs a <code>TaiwanCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale the given locale.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar(Locale aLocale) {
        super(aLocale);
    }

    /**
     * Constructs a <code>TaiwanCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param locale the given ulocale.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar(ULocale locale) {
        super(locale);
    }

    /**
     * Constructs a <code>TaiwanCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     *
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }

    /**
     * Constructs a <code>TaiwanCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     * @param locale the given ulocale.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
    }

    /**
     * Constructs a <code>TaiwanCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar(Date date) {
        this();
        setTime(date);
    }

    /**
     * Constructs a <code>TaiwanCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for January.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar(int year, int month, int date) {
        super(year, month, date);
    }

    /**
     * Constructs a TaiwanCalendar with the given date
     * and time set for the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for January.
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     * @param hour      The value used to set the calendar's {@link #HOUR_OF_DAY HOUR_OF_DAY} time field.
     * @param minute    The value used to set the calendar's {@link #MINUTE MINUTE} time field.
     * @param second    The value used to set the calendar's {@link #SECOND SECOND} time field.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TaiwanCalendar(int year, int month, int date, int hour,
                             int minute, int second)
    {
        super(year, month, date, hour, minute, second);
    }


    //-------------------------------------------------------------------------
    // The only practical difference from a Gregorian calendar is that years
    // are numbered since 1912, inclusive.  A couple of overrides will
    // take care of that....
    //-------------------------------------------------------------------------
    
    private static final int Taiwan_ERA_START = 1911; // 0=1911, 1=1912

    /**
     * {@inheritDoc}
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
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

    // Return JD of start of given month/year
    /**
     * {@inheritDoc}
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */    
    protected int handleComputeMonthStart(int eyear, int month, boolean useMonth) {
        return super.handleComputeMonthStart(eyear + Taiwan_ERA_START, month, useMonth);
    }

    /**
     * {@inheritDoc}
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    protected void handleComputeFields(int julianDay) {
        super.handleComputeFields(julianDay);
        int y = internalGet(EXTENDED_YEAR) - Taiwan_ERA_START;
        internalSet(EXTENDED_YEAR, y);
        internalSet(ERA, 0);
        internalSet(YEAR, y);
    }

    /**
     * Override GregorianCalendar.  There is only one Taiwan ERA.  We
     * should really handle YEAR, YEAR_WOY, and EXTENDED_YEAR here too to
     * implement the 1..5000000 range, but it's not critical.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    protected int handleGetLimit(int field, int limitType) {
        if (field == ERA) {
            return MINGUO;
        }
        return super.handleGetLimit(field, limitType);
    }
    
    /**
     * {@inheritDoc}
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String getType() {
        return "taiwan";
    }

    /*
    private static CalendarFactory factory;
    public static CalendarFactory factory() {
        if (factory == null) {
            factory = new CalendarFactory() {
                public Calendar create(TimeZone tz, ULocale loc) {
                    return new TaiwanCalendar(tz, loc);
                }

                public String factoryName() {
                    return "Taiwan";
                }
            };
        }
        return factory;
    }
    */
}
