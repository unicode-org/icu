/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/calendar/CalendarTest.java,v $ 
 * $Date: 2000/10/17 18:32:50 $ 
 * $Revision: 1.5 $
 *
 *****************************************************************************************
 */

package com.ibm.test.calendar;

import com.ibm.test.*;
import com.ibm.text.DateFormat;
import com.ibm.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.ibm.util.*;

/**
 * A base class for classes that test individual Calendar subclasses.
 * Defines various useful utility methods and constants
 */
public class CalendarTest extends TestFmwk {
    
    // Constants for use by subclasses, solely to save typing
    public final static int SUN = Calendar.SUNDAY;
    public final static int MON = Calendar.MONDAY;
    public final static int TUE = Calendar.TUESDAY;
    public final static int WED = Calendar.WEDNESDAY;
    public final static int THU = Calendar.THURSDAY;
    public final static int FRI = Calendar.FRIDAY;
    public final static int SAT = Calendar.SATURDAY;

    public final static int ERA     = Calendar.ERA;
    public final static int YEAR    = Calendar.YEAR;
    public final static int MONTH   = Calendar.MONTH;
    public final static int DATE    = Calendar.DATE;
    public final static int HOUR    = Calendar.HOUR;
    public final static int MINUTE  = Calendar.MINUTE;
    public final static int SECOND  = Calendar.SECOND;
    public final static int DOY     = Calendar.DAY_OF_YEAR;
    public final static int WOY     = Calendar.WEEK_OF_YEAR;
    public final static int WOM     = Calendar.WEEK_OF_MONTH;
    public final static int DOW     = Calendar.DAY_OF_WEEK;
    public final static int DOWM    = Calendar.DAY_OF_WEEK_IN_MONTH;
    
    public final static SimpleTimeZone UTC = new SimpleTimeZone(0, "GMT");

    final String pattern = "E, MM/dd/yyyy G HH:mm:ss.S z";
    
    /**
     * Iterates through a list of calendar <code>TestCase</code> objects and
     * makes sure that the time-to-fields and fields-to-time calculations work
     * correnctly for the values in each test case.
     */
    public void doTestCases(TestCase[] cases, Calendar cal)
    {
        cal.setTimeZone(UTC);
        
        // Get a format to use for printing dates in the calendar system we're testing
        // TODO: This is kind of ugly right now . 
        DateFormat format = Calendar.getDateTimeFormat(cal, DateFormat.SHORT, -1, Locale.getDefault());
        
        ((SimpleDateFormat)format).applyPattern(pattern);
        DateFormat testFmt = (DateFormat)format.clone();

        // This format is used for pringing Gregorian dates.  This one is easier
        DateFormat gregFormat = new SimpleDateFormat(pattern);
        gregFormat.setTimeZone(UTC);
        
        // Now iterate through the test cases and see what happens
        for (int i = 0; i < cases.length; i++)
        {
            TestCase test = cases[i];
            testFmt.setCalendar(test);
            
            //
            // First we want to make sure that the millis -> fields calculation works
            // test.applyTime will call setTime() on the calendar object, and
            // test.fieldsEqual will retrieve all of the field values and make sure
            // that they're the same as the ones in the testcase
            //
            test.applyTime(cal);
            if (!test.fieldsEqual(cal)) {
                errln("ERROR: millis --> fields calculation incorrect for "
                        + gregFormat.format(test.getTime()));
                logln("  expected " + testFmt.format(test.getTime()));
                logln("  got      " + format.format(cal.getTime()) );
            }
            else {
                //
                // If that was OK, check the fields -> millis calculation
                // test.applyFields will set all of the calendar's fields to 
                // match those in the test case.
                //
                cal.setTime(new Date(0));
                test.applyFields(cal);
                
                if (!test.equals(cal)) {
                    errln("ERROR: fields --> millis calculation incorrect for "
                        + testFmt.format(test.getTime()));
                    logln("  expected " + test.getTime().getTime());
                    logln("  got      " + cal.getTime().getTime() );
                }
            }
        }
    }
    
    static public final boolean ROLL = true;
    static public final boolean ADD = false;
    
    /**
     * Process test cases for <code>add</code> and <code>roll</code> methods.
     * Each test case is an array of integers, as follows:
     * <ul>
     *  <li>0: input year
     *  <li>1:       month  (zero-based)
     *  <li>2:       day
     *  <li>3: field to roll or add to
     *  <li>4: amount to roll or add
     *  <li>5: result year
     *  <li>6:        month (zero-based)
     *  <li>7:        day
     * </ul>
     * For example:
     * <pre>
     *   //       input                add by          output
     *   //  year  month     day     field amount    year  month     day
     *   {   5759, HESHVAN,   2,     MONTH,   1,     5759, KISLEV,    2 },
     * </pre>
     *
     * @param roll  <code>true</code> or <code>ROLL</code> to test the <code>roll</code> method;
     *              <code>false</code> or <code>ADD</code> to test the <code>add</code method
     */
    public void doRollAdd(boolean roll, Calendar cal, int[][] tests)
    {
        String name = roll ? "rolling" : "adding";
        
        for (int i = 0; i < tests.length; i++) {
            int[] test = tests[i];

            cal.clear();
            cal.set(test[0], test[1], test[2]);
            
            if (roll) {
                cal.roll(test[3], test[4]);
            } else {
                cal.add(test[3], test[4]);
            }
            
            if (cal.get(YEAR) != test[5] || cal.get(MONTH) != test[6]
                    || cal.get(DATE) != test[7])
            {
                errln("Error " + name + " "+ ymdToString(test[0], test[1], test[2])
                    + " field " + test[3] + " by " + test[4]
                    + ": expected " + ymdToString(test[5], test[6], test[7])
                    + ", got " + ymdToString(cal.get(YEAR), cal.get(MONTH), cal.get(DATE)));
            }
        }
    }

    /**
     * Convert year,month,day values to the form "year/month/day".
     * On input the month value is zero-based, but in the result string it is one-based.
     */
    static public String ymdToString(int year, int month, int day) {
        return "" + year + "/" + (month+1) + "/" + day;
    }
};
