/*
 *******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.util.Date;
import java.util.Locale;

public final class EthiopicCalendar extends CECalendar 
{
    //jdk1.4.2 serialver
    private static final long serialVersionUID = -2438495771339315608L;

    /** 
     * Constant for \u1218\u1235\u12a8\u1228\u121d, the 1st month of the Ethiopic year. 
     */
    public static final int MESKEREM = 0;

    /** 
     * Constant for \u1325\u1245\u121d\u1275, the 2nd month of the Ethiopic year. 
     */
    public static final int TEKEMT = 1;

    /** 
     * Constant for \u1285\u12f3\u122d, the 3rd month of the Ethiopic year. 
     */
    public static final int HEDAR = 2;

    /** 
     * Constant for \u1273\u1285\u1223\u1225, the 4th month of the Ethiopic year. 
     */
    public static final int TAHSAS = 3;

    /** 
     * Constant for \u1325\u122d, the 5th month of the Ethiopic year. 
     */
    public static final int TER = 4;

    /** 
     * Constant for \u12e8\u12ab\u1272\u1275, the 6th month of the Ethiopic year. 
     */
    public static final int YEKATIT = 5;

    /** 
     * Constant for \u1218\u130b\u1262\u1275, the 7th month of the Ethiopic year. 
     */
    public static final int MEGABIT = 6;

    /** 
     * Constant for \u121a\u12eb\u12dd\u12eb, the 8th month of the Ethiopic year. 
     */
    public static final int MIAZIA = 7;

    /** 
     * Constant for \u130d\u1295\u1266\u1275, the 9th month of the Ethiopic year. 
     */
    public static final int GENBOT = 8;

    /** 
     * Constant for \u1230\u1294, the 10th month of the Ethiopic year. 
     */
    public static final int SENE = 9;

    /** 
     * Constant for \u1210\u121d\u120c, the 11th month of the Ethiopic year. 
     */
    public static final int HAMLE = 10;

    /** 
     * Constant for \u1290\u1210\u1234, the 12th month of the Ethiopic year. 
     */
    public static final int NEHASSE = 11;

    /** 
     * Constant for \u1333\u1309\u121c\u1295, the 13th month of the Ethiopic year. 
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
    
    public EthiopicCalendar() {
    	super();
    }

    public EthiopicCalendar(TimeZone zone) {
        super(zone);
    }

    public EthiopicCalendar(Locale aLocale) {
        super(aLocale);
    }

    public EthiopicCalendar(ULocale locale) {
        super(locale);
    }

    public EthiopicCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }
    
    public EthiopicCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
    }
    
    public EthiopicCalendar(int year, int month, int date) {
    	super(year, month, date);
    }

    public EthiopicCalendar(Date date) {
    	super(date);
    }

    public EthiopicCalendar(int year, int month, int date, int hour,
                            int minute, int second)
    {
    	super(year, month, date, hour, minute, second);
    }

    public static int EthiopicToJD(long year, int month, int date) {
    	return ceToJD(year, month, date, JD_EPOCH_OFFSET_AMETE_MIHRET);
    }
    
    public static Integer[] getDateFromJD(int julianDay) {
    	return getDateFromJD(julianDay, JD_EPOCH_OFFSET_AMETE_MIHRET);
    }
    
    public void setAmeteAlemEra(boolean onOff) {
    	this.jdEpochOffset = onOff 
	    ? JD_EPOCH_OFFSET_AMETE_ALEM 
	    : JD_EPOCH_OFFSET_AMETE_MIHRET;
    }
    
    public boolean isAmeteAlemEra() {
    	return this.jdEpochOffset == JD_EPOCH_OFFSET_AMETE_ALEM;
    }

    /**
     * Return the current Calendar type.
     * @return type of calendar (gregorian, etc.)
     * @internal ICU 3.4
     */
    public String getType() {
        return "ethiopic";
    }
}

