/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/Attic/BuddhistCalendar.java,v $ 
 * $Date: 2000/10/17 18:26:44 $ 
 * $Revision: 1.5 $
 *
 *****************************************************************************************
 */

package com.ibm.util;

import java.util.Date;
import com.ibm.util.GregorianCalendar;
import java.util.Locale;

/**
 * <code>BuddhistCalendar</code> is a subclass of <code>GregorianCalendar</code>
 * that numbers years since the birth of the Buddha.  This is the civil calendar
 * in some predominantly Buddhist countries such as Thailand, and it is used for
 * religious purposes elsewhere.
 * <p>
 * The Buddhist calendar is identical to the Gregorian calendar in all respects
 * except for the year and era.  Years are numbered since the birth of the
 * Buddha in 543 BC (Gregorian), so that 1 AD (Gregorian) is equivalent to 544
 * BE (Buddhist Era) and 1998 AD is 2541 BE.
 * <p>
 * The Buddhist Calendar has only one allowable era: <code>BE</code>.  If the
 * calendar is not in lenient mode (see <code>setLenient</code>), dates before
 * 1/1/1 BE are rejected with an <code>IllegalArgumentException</code>.
 *
 * @see com.ibm.util.GregorianCalendar
 *
 * @author Laura Werner
 */
public class BuddhistCalendar extends GregorianCalendar {
    
    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    //-------------------------------------------------------------------------
    // Constructors...
    //-------------------------------------------------------------------------

    /**
     * Constant for the Buddhist Era.  This is the only allowable <code>ERA</code>
     * value for the Buddhist calendar.
     *
     * @see com.ibm.util.Calendar#ERA
     */
    public static final int BE = 0;
    
    /**
     * Constructs a <code>BuddhistCalendar</code> using the current time
     * in the default time zone with the default locale.
     */
    public BuddhistCalendar() {
        super();
    }

    /**
     * Constructs a <code>BuddhistCalendar</code> based on the current time
     * in the given time zone with the default locale.
     *
     * @param zone the given time zone.
     */
    public BuddhistCalendar(TimeZone zone) {
        super(zone);
    }

    /**
     * Constructs a <code>BuddhistCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale the given locale.
     */
    public BuddhistCalendar(Locale aLocale) {
        super(aLocale);
    }

    /**
     * Constructs a <code>BuddhistCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     *
     * @param aLocale the given locale.
     */
    public BuddhistCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }

    /**
     * Constructs a <code>BuddhistCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     */
    public BuddhistCalendar(Date date) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.setTime(date);
    }

    /**
     * Constructs a <code>BuddhistCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for January.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     */
    public BuddhistCalendar(int year, int month, int date) {
        super(year, month, date);
    }

    /**
     * Constructs a BuddhistCalendar with the given date
     * and time set for the default time zone with the default locale.
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
     */
    public BuddhistCalendar(int year, int month, int date, int hour,
                             int minute, int second)
    {
        super(year, month, date, hour, minute, second);
    }


    //-------------------------------------------------------------------------
    // The only practical difference from a Gregorian calendar is that years
    // are numbered since the birth of the Buddha.  A couple of overrides will
    // take care of that....
    //-------------------------------------------------------------------------
    
    /**
     * Override of the <code>GregorianCalendar</code> method that computes the
     * fields such as <code>YEAR</code>, <code>MONTH</code>, and <code>DATE</code>
     * from the date in milliseconds since 1/1/1970 AD.
     *
     * This method calls {@link GregorianCalendar#computeFields} to do most
     * of the work,
     * then adjusts the {@link #YEAR YEAR} and {@link #ERA ERA} fields to use the
     * Buddhist Era rather than the Gregorian {@link #AD AD} or {@link #BC BC}.
     */
    protected void computeFields() {
        // Let GregorianCalendar do its thing.
        super.computeFields();
        
        // Now adjust the year and era to the proper Buddhist values
        fromGregorian();
        
        //
        // If we're in strict mode and the year is less than 1, fail.
        // But do this after setting both the year and era to the right values
        // anyway, so that this object is in a consistent state.
        //
        if (!isLenient() && fields[YEAR] < 1) {
            throw new IllegalArgumentException("Time before start of Buddhist era");
        }
    }
    
    /**
     * Override of the <code>GregorianCalendar</code> method that computes the 
     * elapsed time in milliseconds since 1/1/1970 AD from the fields such
     * as <code>YEAR</code>, <code>MONTH</code>, and <code>DATE</code>.
     *
     * This method adjusts the {@link #YEAR YEAR} and {@link #ERA ERA} from their
     * values in the Buddhist calendar to the corresponding Gregorian values, calls
     * {@link GregorianCalendar#computeTime} to do the real millisecond
     * calculation, and then restores the Buddhist <code>YEAR</code> and
     * <code>ERA</code>.
     */
    protected void computeTime() {
        int year = fields[YEAR];
        int era = fields[ERA];
        
        if (!isLenient()) {
            if (era != BE) {
                throw new IllegalArgumentException("Illegal value for ERA");
            }
            if (year < 1) {
                throw new IllegalArgumentException("YEAR must be greater than 0");
            }
        }

        try {
            toGregorian();
            super.computeTime();
        }
        finally {
            // Set the year and era back to the Buddhist values, even if
            // GregorianCalendar fails because other fields are invalid.
            fields[YEAR] = year;
            fields[ERA] = era;
        }
    }
    
    public void add(int field, int amount) {
        toGregorian();
        try {
            super.add(field, amount);
        }
        finally {
            fromGregorian();
        }
    }
    
    public void roll(int field, int amount) {
        toGregorian();
        try {
            super.roll(field, amount);
        }
        finally {
            fromGregorian();
        }
    }
    
    //-------------------------------------------------------------------------
    // Methods for converting between Gregorian and Buddhist calendars
    //-------------------------------------------------------------------------

    private static int BUDDHIST_ERA_START = -543;    // Starts in -543 AD, ie 544 BC
    
    /**
     * Convert the YEAR and ERA fields from Buddhist to Gregorian values
     * Return the (Buddhist) value of the YEAR field on input;
     */
    private void toGregorian() {
        int year = fields[YEAR];
        
        if (year > 0) {
            fields[YEAR] = year + BUDDHIST_ERA_START;
            fields[ERA] = AD;
        } else {
            fields[YEAR] = 1 - year - BUDDHIST_ERA_START;
            fields[ERA] = BC;
        }
    }
    
    /**
     * Adjust the year and era from Gregorian to Buddhist values
     */
    private void fromGregorian() {
        // Now adjust the year and era to the proper Buddhist values
        int year = fields[YEAR];
        
        if (fields[ERA] == BC) {
            fields[YEAR] = 1 - year - BUDDHIST_ERA_START;
        } else {
            fields[YEAR] = year - BUDDHIST_ERA_START;
        }
        fields[ERA] = BE;
    }
    
};
