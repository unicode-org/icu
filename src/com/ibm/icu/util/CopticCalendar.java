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
 * Implement the Coptic calendar system.
 * <p>
 * CopticCalendar usually should be instantiated using 
 * {@link com.ibm.icu.util.Calendar#getInstance(ULocale)} passing in a <code>ULocale</code>
 * with the tag <code>"@calendar=coptic"</code>.</p>
 *
 * @see com.ibm.icu.util.Calendar
 * @stable ICU 3.8
 */
public final class CopticCalendar extends CECalendar 
{
    // jdk1.4.2 serialver
    private static final long serialVersionUID = 5903818751846742911L;

    /** 
     * Constant for \u03c9\u03bf\u03b3\u03c4/\u062a\ufeee\ufe97,
     * the 1st month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int TOUT = 0;

    /** 
     * Constant for \u03a0\u03b1\u03bf\u03c0\u03b9/\ufeea\ufe91\ufe8e\ufe91,
     * the 2nd month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int BABA = 1;

    /** 
     * Constant for \u0391\u03b8\u03bf\u03c1/\u0631\ufeee\ufe97\ufe8e\ufeeb,
     * the 3rd month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int HATOR = 2;

    /** 
     * Constant for \u03a7\u03bf\u03b9\u03b1\u03ba/\ufeda\ufeec\ufef4\ufedb,
     * the 4th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int KIAHK = 3;

    /** 
     * Constant for \u03a4\u03c9\u03b2\u03b9/\u0637\ufeee\ufe92\ufeeb,
     * the 5th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int TOBA = 4;

    /** 
     * Constant for \u039c\u03b5\u03e3\u03b9\u03c1/\ufeae\ufef4\ufeb8\ufee3\u0623,
     * the 6th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int AMSHIR = 5;

    /** 
     * Constant for \u03a0\u03b1\u03c1\u03b5\u03bc\u03e9\u03b1\u03c4/\u062a\ufe8e\ufeec\ufee3\ufeae\ufe91,
     * the 7th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int BARAMHAT = 6;

    /** 
     * Constant for \u03a6\u03b1\u03c1\u03bc\u03bf\u03b8\u03b9/\u0647\u062f\ufeee\ufee3\ufeae\ufe91, 
     * the 8th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int BARAMOUDA = 7;

    /** 
     * Constant for \u03a0\u03b1\u03e3\u03b1\u03bd/\ufeb2\ufee8\ufeb8\ufe91, 
     * the 9th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int BASHANS = 8;

    /** 
     * Constant for \u03a0\u03b1\u03c9\u03bd\u03b9/\ufeea\ufee7\u0624\ufeee\ufe91, 
     * the 10th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int PAONA = 9;

    /** 
     * Constant for \u0395\u03c0\u03b7\u03c0/\ufe90\ufef4\ufe91\u0623, 
     * the 11th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int EPEP = 10;

    /** 
     * Constant for \u039c\u03b5\u03f2\u03c9\u03c1\u03b7/\u0649\ufeae\ufeb4\ufee3, 
     * the 12th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int MESRA = 11;

    /** 
     * Constant for \u03a0\u03b9\u03ba\u03bf\u03b3\u03eb\u03b9 
     * \u03bc\u03b1\u03b2\u03bf\u03c4/\ufeae\ufef4\ufed0\ufebc\ufedf\u0627 
     * \ufeae\ufeec\ufeb8\ufedf\u0627, 
     * the 13th month of the Coptic year. 
     * @stable ICU 3.8
     */
    public static final int NASIE = 12;
  
    private static final int JD_EPOCH_OFFSET  = 1824665;

    // init base class value, common to all constructors
    {
        jdEpochOffset = JD_EPOCH_OFFSET;
    }

    /**
     * Constructs a default <code>CopticCalendar</code> using the current time
     * in the default time zone with the default locale.
     *
     * @stable ICU 3.8
     */
    public CopticCalendar() {
        super();
    }

    /**
     * Constructs a <code>CopticCalendar</code> based on the current time
     * in the given time zone with the default locale.
     *
     * @param zone The time zone for the new calendar.
     *
     * @stable ICU 3.8
     */
    public CopticCalendar(TimeZone zone) {
        super(zone);
    }

    /**
     * Constructs a <code>CopticCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale The locale for the new calendar.
     * @stable ICU 3.8
     */
    public CopticCalendar(Locale aLocale) {
        super(aLocale);
    }

    /**
     * Constructs a <code>CopticCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param locale The icu locale for the new calendar.
     *
     * @stable ICU 3.8
     */
    public CopticCalendar(ULocale locale) {
        super(locale);
    }

    /**
     * Constructs a <code>CopticCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone The time zone for the new calendar.
     * @param aLocale The locale for the new calendar.
     *
     * @stable ICU 3.8
     */
    public CopticCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }
    
    /**
     * Constructs a <code>CopticCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone The time zone for the new calendar.
     * @param locale The icu locale for the new calendar.
     *
     * @stable ICU 3.8
     */
    public CopticCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
    }
    
    /**
     * Constructs a <code>CopticCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for Tout.
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     *
     * @stable ICU 3.8
     */
    public CopticCalendar(int year, int month, int date) {
        super(year, month, date);
    }

    /**
     * Constructs a <code>CopticCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     *
     * @stable ICU 3.8
     */
    public CopticCalendar(Date date) {
        super(date);
    }

    /**
     * Constructs a <code>CopticCalendar</code> with the given date
     * and time set for the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for Tout.
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     * @param hour      The value used to set the calendar's {@link #HOUR_OF_DAY HOUR_OF_DAY} time field.
     * @param minute    The value used to set the calendar's {@link #MINUTE MINUTE} time field.
     * @param second    The value used to set the calendar's {@link #SECOND SECOND} time field.
     *
     * @stable ICU 3.8
     */
    public CopticCalendar(int year, int month, int date, int hour,
                          int minute, int second) {
        super(year, month, date, hour, minute, second);
    }

    /**
     * Convert an Coptic year, month, and day to a Julian day.
     *
     * @param year the year
     * @param month the month
     * @param date the day
     *
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static int copticToJD(long year, int month, int date) {
        return ceToJD(year, month, date, JD_EPOCH_OFFSET);
    }
    
    /**
     * @internal ICU 3.4
     * @deprecated This API is ICU internal only.
     */
    public static Integer[] getDateFromJD(int julianDay) {
        return getDateFromJD(julianDay, JD_EPOCH_OFFSET);
    }

    /**
     * {@inheritDoc}
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String getType() {
        return "coptic";
    }
}

