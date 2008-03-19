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
 * Implement the Ethiopic calendar system.
 * <p>
 * EthiopicCalendar usually should be instantiated using 
 * {@link com.ibm.icu.util.Calendar#getInstance(ULocale)} passing in a <code>ULocale</code>
 * with the tag <code>"@calendar=ethiopic"</code>.</p>
 *
 * @see com.ibm.icu.util.Calendar
 * @stable ICU 3.4
 */
public final class EthiopicCalendar extends CECalendar 
{
    //jdk1.4.2 serialver
    private static final long serialVersionUID = -2438495771339315608L;

    /** 
     * Constant for &#x1218;&#x1235;&#x12a8;&#x1228;&#x121d;, the 1st month of the Ethiopic year.
     * @stable ICU 3.4
     */
    public static final int MESKEREM = 0;

    /** 
     * Constant for &#x1325;&#x1245;&#x121d;&#x1275;, the 2nd month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int TEKEMT = 1;

    /** 
     * Constant for &#x1285;&#x12f3;&#x122d;, the 3rd month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int HEDAR = 2;

    /** 
     * Constant for &#x1273;&#x1285;&#x1223;&#x1225;, the 4th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int TAHSAS = 3;

    /** 
     * Constant for &#x1325;&#x122d;, the 5th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int TER = 4;

    /** 
     * Constant for &#x12e8;&#x12ab;&#x1272;&#x1275;, the 6th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int YEKATIT = 5;

    /** 
     * Constant for &#x1218;&#x130b;&#x1262;&#x1275;, the 7th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int MEGABIT = 6;

    /** 
     * Constant for &#x121a;&#x12eb;&#x12dd;&#x12eb;, the 8th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int MIAZIA = 7;

    /** 
     * Constant for &#x130d;&#x1295;&#x1266;&#x1275;, the 9th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int GENBOT = 8;

    /** 
     * Constant for &#x1230;&#x1294;, the 10th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int SENE = 9;

    /** 
     * Constant for &#x1210;&#x121d;&#x120c;, the 11th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int HAMLE = 10;

    /** 
     * Constant for &#x1290;&#x1210;&#x1234;, the 12th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int NEHASSE = 11;

    /** 
     * Constant for &#x1333;&#x1309;&#x121c;&#x1295;, the 13th month of the Ethiopic year. 
     * @stable ICU 3.4
     */
    public static final int PAGUMEN = 12;
 
    // Up until the end of the 19th century the prevailant convention was to
    // reference the Ethiopic Calendar from the creation of the world, 
    // \u12d3\u1218\u1270\u1361\u12d3\u1208\u121d
    // (Amete Alem 5500 BC).  As Ethiopia modernized the reference epoch from
    // the birth of Christ (\u12d3\u1218\u1270\u1361\u121d\u1215\u1228\u1275) 
    // began to displace the creation of the
    // world reference point.  However, years before the birth of Christ are
    // still referenced in the creation of the world system.   
    // Thus -100 \u12d3/\u121d
    // would be rendered as 5400  \u12d3/\u12d3.
    //
    // The creation of the world in Ethiopic cannon was 
    // Meskerem 1, -5500  \u12d3/\u121d 00:00:00
    // applying the birth of Christ reference and Ethiopian time conventions.  This is
    // 6 hours less than the Julian epoch reference point (noon).  In Gregorian
    // the date and time was July 18th -5493 BC 06:00 AM.

    // Julian Days relative to the 
    // \u12d3\u1218\u1270\u1361\u121d\u1215\u1228\u1275 epoch
    private static final int JD_EPOCH_OFFSET_AMETE_ALEM = -285019;

    // Julian Days relative to the 
    // \u12d3\u1218\u1270\u1361\u12d3\u1208\u121d epoch
    private static final int JD_EPOCH_OFFSET_AMETE_MIHRET = 1723856;

    // initialize base class constant, common to all constructors
    {
        jdEpochOffset = JD_EPOCH_OFFSET_AMETE_MIHRET;
    }

    /**
     * Constructs a default <code>EthiopicCalendar</code> using the current time
     * in the default time zone with the default locale.
     *
     * @stable ICU 3.4
     */
    public EthiopicCalendar() {
        super();
    }

    /**
     * Constructs a <code>EthiopicCalendar</code> based on the current time
     * in the given time zone with the default locale.
     *
     * @param zone The time zone for the new calendar.
     *
     * @stable ICU 3.4
     */
    public EthiopicCalendar(TimeZone zone) {
        super(zone);
    }

    /**
     * Constructs a <code>EthiopicCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale The locale for the new calendar.
     * @stable ICU 3.4
     */
    public EthiopicCalendar(Locale aLocale) {
        super(aLocale);
    }

    /**
     * Constructs a <code>EthiopicCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param locale The icu locale for the new calendar.
     *
     * @stable ICU 3.4
     */
    public EthiopicCalendar(ULocale locale) {
        super(locale);
    }

    /**
     * Constructs a <code>EthiopicCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone The time zone for the new calendar.
     * @param aLocale The locale for the new calendar.
     *
     * @stable ICU 3.4
     */
    public EthiopicCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }
    
    /**
     * Constructs a <code>EthiopicCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone The time zone for the new calendar.
     * @param locale The icu locale for the new calendar.
     *
     * @stable ICU 3.4
     */
    public EthiopicCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
    }
    
    /**
     * Constructs a <code>EthiopicCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for Meskerem.
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     *
     * @stable ICU 3.4
     */
    public EthiopicCalendar(int year, int month, int date) {
        super(year, month, date);
    }

    /**
     * Constructs a <code>EthiopicCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     *
     * @stable ICU 3.4
     */
    public EthiopicCalendar(Date date) {
        super(date);
    }

    /**
     * Constructs a <code>EthiopicCalendar</code> with the given date
     * and time set for the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for Meskerem.
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     * @param hour      The value used to set the calendar's {@link #HOUR_OF_DAY HOUR_OF_DAY} time field.
     * @param minute    The value used to set the calendar's {@link #MINUTE MINUTE} time field.
     * @param second    The value used to set the calendar's {@link #SECOND SECOND} time field.
     *
     * @stable ICU 3.4
     */
    public EthiopicCalendar(int year, int month, int date, int hour,
                            int minute, int second)
    {
        super(year, month, date, hour, minute, second);
    }

    /**
     * Convert an Ethiopic year, month, and day to a Julian day.
     *
     * @param year the year
     * @param month the month
     * @param date the day
     *
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public static int EthiopicToJD(long year, int month, int date) {
        return ceToJD(year, month, date, JD_EPOCH_OFFSET_AMETE_MIHRET);
    }
    
    /**
     * @internal ICU 3.4
     * @deprecated This API is ICU internal only.
     */
    public static Integer[] getDateFromJD(int julianDay) {
        return getDateFromJD(julianDay, JD_EPOCH_OFFSET_AMETE_MIHRET);
    }
    
    /**
     * Set Alem or Mihret era.
     *
     * @param onOff Set Amete Alem era if true, otherwise set Amete Mihret era.
     *
     * @stable ICU 3.4
     */
    public void setAmeteAlemEra(boolean onOff) {
        this.jdEpochOffset = onOff 
            ? JD_EPOCH_OFFSET_AMETE_ALEM 
            : JD_EPOCH_OFFSET_AMETE_MIHRET;
    }
    
    /**
     * Return true if this calendar is set to the Amete Alem era.
     *
     * @return true if set to the Amete Alem era.
     *
     * @stable ICU 3.4
     */
    public boolean isAmeteAlemEra() {
        return this.jdEpochOffset == JD_EPOCH_OFFSET_AMETE_ALEM;
    }

    /**
     * {@inheritDoc}
     * @return type of calendar
     * @draft ICU 3.8
     */
    public String getType() {
        return "ethiopic";
    }
}

