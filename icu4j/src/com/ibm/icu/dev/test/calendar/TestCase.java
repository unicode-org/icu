/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/calendar/TestCase.java,v $ 
 * $Date: 2000/10/17 18:32:50 $ 
 * $Revision: 1.5 $
 *
 *****************************************************************************************
 */
package com.ibm.test.calendar;

import com.ibm.test.*;
import com.ibm.util.Calendar;
import com.ibm.util.GregorianCalendar;
import java.util.Date;
import com.ibm.util.SimpleTimeZone;
import java.util.Locale;

/**
 * A dummy <code>Calendar</code> subclass that is useful for testing
 * new calendars.  A <code>TestCase</code> object is used to hold the
 * field and millisecond values that the calendar should have at one
 * particular instant in time.  The applyFields and applyTime
 * methods are used to apply these settings to the calendar object being
 * tested, and the equals and fieldsEqual methods are used to ensure
 * that the calendar has ended up in the right state.
 */
public class TestCase extends Calendar {
    
    /**
     * Initialize a TestCase object using a julian day number and
     * the corresponding fields for the calendar being tested.
     *
     * @param era       The ERA field of tested calendar on the given julian day
     * @param year      The YEAR field of tested calendar on the given julian day
     * @param month     The MONTH (0-based) field of tested calendar on the given julian day
     * @param day       The DAY_OF_MONTH field of tested calendar on the given julian day
     * @param dayOfWeek The DAY_OF_WEEK field of tested calendar on the given julian day
     * @param hour      The HOUR field of tested calendar on the given julian day
     * @param min       The MINUTE field of tested calendar on the given julian day
     * @param sec       The SECOND field of tested calendar on the given julian day
     */
    public TestCase(double julian,
                   int era, int year, int month, int day,
                   int dayOfWeek,
                   int hour, int min, int sec)
    {
        super(UTC, Locale.getDefault());
        
        setTime(new Date(JULIAN_EPOCH + (long)(DAY_MS * julian)));
        
        set(ERA, era);
        set(YEAR, year);
        set(MONTH, month - 1);
        set(DATE, day);
        set(DAY_OF_WEEK, dayOfWeek);
        set(HOUR, hour);
        set(MINUTE, min);
        set(SECOND, sec);
    }

    /**
     * Initialize a TestCase object using a Gregorian year/month/day and
     * the corresponding fields for the calendar being tested.
     *
     * @param gregYear  The Gregorian year of the date to be tested
     * @param gregMonth The Gregorian month of the date to be tested
     * @param gregDay   The Gregorian day of the month of the date to be tested
     *
     * @param era       The ERA field of tested calendar on the given gregorian date
     * @param year      The YEAR field of tested calendar on the given gregorian date
     * @param month     The MONTH (0-based) field of tested calendar on the given gregorian date
     * @param day       The DAY_OF_MONTH field of tested calendar on the given gregorian date
     * @param dayOfWeek The DAY_OF_WEEK field of tested calendar on the given gregorian date
     * @param hour      The HOUR field of tested calendar on the given gregorian date
     * @param min       The MINUTE field of tested calendar on the given gregorian date
     * @param sec       The SECOND field of tested calendar on the given gregorian date
     */
    public TestCase(int gregYear, int gregMonth, int gregDay,
                   int era, int year, int month, int day,
                   int dayOfWeek,
                   int hour, int min, int sec)
    {
        super(UTC, Locale.getDefault());
        
        GregorianCalendar greg = new GregorianCalendar(UTC, Locale.getDefault());
        greg.clear();
        greg.set(gregYear, gregMonth-1, gregDay);
        setTime(greg.getTime());
        
        set(ERA, era);
        set(YEAR, year);
        set(MONTH, month - 1);
        set(DATE, day);
        set(DAY_OF_WEEK, dayOfWeek);
        set(HOUR, hour);
        set(MINUTE, min);
        set(SECOND, sec);
    }
    
    /**
     * Apply this test case's field values to another calendar
     * by calling its set method for each field.  This is useful in combination
     * with the equal method.
     *
     * @see #equal
     */
    public void applyFields(Calendar c) {
        c.set(ERA,     fields[ERA]);
        c.set(YEAR,    fields[YEAR]);
        c.set(MONTH,   fields[MONTH]);
        c.set(DATE,    fields[DATE]);
        c.set(HOUR,    fields[HOUR]);
        c.set(MINUTE,  fields[MINUTE]);
        c.set(SECOND,  fields[SECOND]);
    }
    
    /**
     * Apply this test case's time in milliseconds to another calendar
     * by calling its setTime method.  This is useful in combination
     * with fieldsEqual
     *
     * @see #fieldsEqual
     */
    public void applyTime(Calendar c) {
        c.setTime(new Date(time));
    }

    /**
     * Determine whether the fields of this calendar
     * are the same as that of the other calendar.  This method is useful
     * for determining whether the other calendar's computeFields method
     * works properly.  For example:
     * <pre>
     *    Calendar testCalendar = ...
     *    TestCase case = ...
     *    case.applyTime(testCalendar);
     *    if (!case.fieldsEqual(testCalendar)) {
     *        // Error!
     *    }
     * </pre>
     * 
     * @see #applyTime
     */
    public boolean fieldsEqual(Calendar c) {
        for (int i=0; i < Calendar.FIELD_COUNT; i++) {
            if (isSet(i) && get(i) != c.get(i)) {
                System.out.println("field " + i + ": expected " + get(i) + ", got " + c.get(i));
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Determine whether time in milliseconds of this calendar
     * is the same as that of the other calendar.  This method is useful
     * for determining whether the other calendar's computeTime method
     * works properly.  For example:
     * <pre>
     *    Calendar testCalendar = ...
     *    TestCase case = ...
     *    case.applyFields(testCalendar);
     *    if (!case.equals(testCalendar)) {
     *        // Error!
     *    }
     * </pre>
     * 
     * @see #applyFields
     */
    public boolean equals(Object obj) {
        return time == ((Calendar)obj).getTime().getTime();
    }
    
    /**
     * Determine whether time of this calendar (as returned by getTime)
     * is before that of the specified calendar
     */
    public boolean before(Object obj) {
        return time < ((Calendar)obj).getTime().getTime();
    }

    /**
     * Determine whether time of this calendar (as returned by getTime)
     * is after that of the specified calendar
     */
    public boolean after(Object obj) {
        return time > ((Calendar)obj).getTime().getTime();
    }
    
    // This object is only pretending to be a Calendar; it doesn't do any real
    // calendar computatations.  But we have to pretend it does, because Calendar
    // declares all of these abstract methods....
    protected void computeTime() {}
    protected void computeFields() {}
    public void add(int field, int amt) {}
    public int getMinimum(int field) { return 0; }
    public int getMaximum(int field) { return 0; }
    public int getGreatestMinimum(int field) { return 0; }
    public int getLeastMaximum(int field) { return 0; }
    
    private static final int  SECOND_MS = 1000;
    private static final int  MINUTE_MS = 60*SECOND_MS;
    private static final int  HOUR_MS   = 60*MINUTE_MS;
    private static final long DAY_MS    = 24*HOUR_MS;
    private static final long JULIAN_EPOCH = -210866760000000L;   // 1/1/4713 BC 12:00

    public final static SimpleTimeZone UTC = new SimpleTimeZone(0, "GMT");
}
