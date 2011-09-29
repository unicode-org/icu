/*
 *******************************************************************************
 * Copyright (C) 2005-2008, International Business Machines Corporation and    *
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
 * @stable ICU 3.4
 */
public final class CopticCalendar extends CECalendar 
{
    // jdk1.4.2 serialver
    private static final long serialVersionUID = 5903818751846742911L;

    /** 
     * Constant for &#x03c9;&#x03bf;&#x03b3;&#x03c4;/&#x062a;&#xfeee;&#xfe97;,
     * the 1st month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int TOUT = 0;

    /** 
     * Constant for &#x03a0;&#x03b1;&#x03bf;&#x03c0;&#x03b9;/&#xfeea;&#xfe91;&#xfe8e;&#xfe91;,
     * the 2nd month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int BABA = 1;

    /** 
     * Constant for &#x0391;&#x03b8;&#x03bf;&#x03c1;/&#x0631;&#xfeee;&#xfe97;&#xfe8e;&#xfeeb;,
     * the 3rd month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int HATOR = 2;

    /** 
     * Constant for &#x03a7;&#x03bf;&#x03b9;&#x03b1;&#x03ba;/&#xfeda;&#xfeec;&#xfef4;&#xfedb;,
     * the 4th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int KIAHK = 3;

    /** 
     * Constant for &#x03a4;&#x03c9;&#x03b2;&#x03b9;/&#x0637;&#xfeee;&#xfe92;&#xfeeb;,
     * the 5th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int TOBA = 4;

    /** 
     * Constant for &#x039c;&#x03b5;&#x03e3;&#x03b9;&#x03c1;/&#xfeae;&#xfef4;&#xfeb8;&#xfee3;&#x0623;,
     * the 6th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int AMSHIR = 5;

    /** 
     * Constant for &#x03a0;&#x03b1;&#x03c1;&#x03b5;&#x03bc;&#x03e9;&#x03b1;&#x03c4;/&#x062a;&#xfe8e;&#xfeec;&#xfee3;&#xfeae;&#xfe91;,
     * the 7th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int BARAMHAT = 6;

    /** 
     * Constant for &#x03a6;&#x03b1;&#x03c1;&#x03bc;&#x03bf;&#x03b8;&#x03b9;/&#x0647;&#x062f;&#xfeee;&#xfee3;&#xfeae;&#xfe91;, 
     * the 8th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int BARAMOUDA = 7;

    /** 
     * Constant for &#x03a0;&#x03b1;&#x03e3;&#x03b1;&#x03bd;/&#xfeb2;&#xfee8;&#xfeb8;&#xfe91;,
     * the 9th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int BASHANS = 8;

    /** 
     * Constant for &#x03a0;&#x03b1;&#x03c9;&#x03bd;&#x03b9;/&#xfeea;&#xfee7;&#x0624;&#xfeee;&#xfe91;,
     * the 10th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int PAONA = 9;

    /** 
     * Constant for &#x0395;&#x03c0;&#x03b7;&#x03c0;/&#xfe90;&#xfef4;&#xfe91;&#x0623;,
     * the 11th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int EPEP = 10;

    /** 
     * Constant for &#x039c;&#x03b5;&#x03f2;&#x03c9;&#x03c1;&#x03b7;/&#x0649;&#xfeae;&#xfeb4;&#xfee3;,
     * the 12th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int MESRA = 11;

    /** 
     * Constant for &#x03a0;&#x03b9;&#x03ba;&#x03bf;&#x03b3;&#x03eb;&#x03b9;
     * &#x03bc;&#x03b1;&#x03b2;&#x03bf;&#x03c4;/&#xfeae;&#xfef4;&#xfed0;&#xfebc;&#xfedf;&#x0627;
     * &#xfeae;&#xfeec;&#xfeb8;&#xfedf;&#x0627;,
     * the 13th month of the Coptic year. 
     * @stable ICU 3.4
     */
    public static final int NASIE = 12;
  
    private static final int JD_EPOCH_OFFSET  = 1824665;

    // Eras
    private static final int BCE = 0;
    private static final int CE = 1;

    /**
     * Constructs a default <code>CopticCalendar</code> using the current time
     * in the default time zone with the default locale.
     * @stable ICU 3.4
     */
    public CopticCalendar() {
        super();
    }

    /**
     * Constructs a <code>CopticCalendar</code> based on the current time
     * in the given time zone with the default locale.
     *
     * @param zone The time zone for the new calendar.
     * @stable ICU 3.4
     */
    public CopticCalendar(TimeZone zone) {
        super(zone);
    }

    /**
     * Constructs a <code>CopticCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale The locale for the new calendar.
     * @stable ICU 3.4
     */
    public CopticCalendar(Locale aLocale) {
        super(aLocale);
    }

    /**
     * Constructs a <code>CopticCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param locale The icu locale for the new calendar.
     * @stable ICU 3.4
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
     * @stable ICU 3.4
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
     * @stable ICU 3.4
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
     * @stable ICU 3.4
     */
    public CopticCalendar(int year, int month, int date) {
        super(year, month, date);
    }

    /**
     * Constructs a <code>CopticCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     * @stable ICU 3.4
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
     * @stable ICU 3.4
     */
    public CopticCalendar(int year, int month, int date, int hour,
                          int minute, int second) {
        super(year, month, date, hour, minute, second);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    public String getType() {
        return "coptic";
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected int handleGetExtendedYear() {
        int eyear;
        if (newerField(EXTENDED_YEAR, YEAR) == EXTENDED_YEAR) {
            eyear = internalGet(EXTENDED_YEAR, 1); // Default to year 1
        } else {
            // The year defaults to the epoch start, the era to AD
            int era = internalGet(ERA, CE);
            if (era == BCE) {
                eyear = 1 - internalGet(YEAR, 1); // Convert to extended year
            } else {
                eyear = internalGet(YEAR, 1); // Default to year 1
            }
        }
        return eyear;
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected void handleComputeFields(int julianDay) {
        int era, year;
        int[] fields = new int[3];
        jdToCE(julianDay, getJDEpochOffset(), fields);

        // fields[0] eyear
        // fields[1] month
        // fields[2] day

        if (fields[0] <= 0) {
            era = BCE;
            year = 1 - fields[0];
        } else {
            era = CE;
            year = fields[0];
        }

        internalSet(EXTENDED_YEAR, fields[0]);
        internalSet(ERA, era);
        internalSet(YEAR, year);
        internalSet(MONTH, fields[1]);
        internalSet(DAY_OF_MONTH, fields[2]);
        internalSet(DAY_OF_YEAR, (30 * fields[1]) + fields[2]);
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected int getJDEpochOffset() {
        return JD_EPOCH_OFFSET;
    }

    /**
     * Convert an Coptic year, month, and day to a Julian day.
     *
     * @param year the year
     * @param month the month
     * @param date the day
     * @draft ICU 3.4 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    // The equivalent operation can be done by public Calendar API.
    // This API was accidentally marked as @draft, but we have no good
    // reason to keep this.  For now, we leave it as is, but may be
    // removed in future.  2008-03-21 yoshito
    public static int copticToJD(long year, int month, int date) {
        return ceToJD(year, month, date, JD_EPOCH_OFFSET);
    }
}

