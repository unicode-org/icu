/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/test/calendar/Attic/IslamicTest.java,v $ 
 * $Date: 2000/11/18 00:17:58 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.test.calendar;

import com.ibm.test.*;
import java.util.*;
import java.text.*;
import com.ibm.util.*;

/**
 * Tests for the <code>IslamicCalendar</code> class.
 */
public class IslamicTest extends CalendarTest {
    public static void main(String args[]) throws Exception {
        new IslamicTest().run(args);
    }

    /** Constants to save typing. */
    public static final int MUHARRAM = IslamicCalendar.MUHARRAM;
    public static final int SAFAR =  IslamicCalendar.SAFAR;
    public static final int RABI_1 =  IslamicCalendar.RABI_1;
    public static final int RABI_2 =  IslamicCalendar.RABI_2;
    public static final int JUMADA_1 =  IslamicCalendar.JUMADA_1;
    public static final int JUMADA_2 =  IslamicCalendar.JUMADA_2;
    public static final int RAJAB =  IslamicCalendar.RAJAB;
    public static final int SHABAN =  IslamicCalendar.SHABAN;
    public static final int RAMADAN =  IslamicCalendar.RAMADAN;
    public static final int SHAWWAL =  IslamicCalendar.SHAWWAL;
    public static final int QIDAH =  IslamicCalendar.DHU_AL_QIDAH;
    public static final int HIJJAH =  IslamicCalendar.DHU_AL_HIJJAH;

    public void TestRoll() {
        int[][] tests = new int[][] {
            //       input                roll by          output
            //  year  month     day     field amount    year  month     day
    
            {   0001, QIDAH,     2,     MONTH,   1,     0001, HIJJAH,    2 },   // non-leap years
            {   0001, QIDAH,     2,     MONTH,   2,     0001, MUHARRAM,  2 },
            {   0001, QIDAH,     2,     MONTH,  -1,     0001, SHAWWAL,   2 },
            {   0001, MUHARRAM,  2,     MONTH,  12,     0001, MUHARRAM,  2 },
            {   0001, MUHARRAM,  2,     MONTH,  13,     0001, SAFAR,     2 },

            {   0001, HIJJAH,    1,     DATE,   30,     0001, HIJJAH,    2 },   // 29-day month
            {   0002, HIJJAH,    1,     DATE,   31,     0002, HIJJAH,    2 },   // 30-day month

            // Try some rolls that require other fields to be adjusted
            {   0001, MUHARRAM, 30,     MONTH,   1,     0001, SAFAR,    29 },
            {   0002, HIJJAH,   30,     YEAR,   -1,     0001, HIJJAH,   29 },
        };
       
        IslamicCalendar cal = newCivil();

        doRollAdd(ROLL, cal, tests);
    }

    /**
     * A huge list of test cases to make sure that computeTime and computeFields
     * work properly for a wide range of data in the civil calendar.
     */
    public void TestCivilCases()
    {
        final TestCase[] tests = {
            //
            // Most of these test cases were taken from the back of
            // "Calendrical Calculations", with some extras added to help
            // debug a few of the problems that cropped up in development.
            //
            // The months in this table are 1-based rather than 0-based,
            // because it's easier to edit that way.
            //                       Islamic
            //          Julian Day  Era  Year  Month Day  WkDay Hour Min Sec
            new TestCase(1507231.5,  0, -1245,   12,   9,  SUN,   0,  0,  0),
            new TestCase(1660037.5,  0,  -813,    2,  23,  WED,   0,  0,  0),
            new TestCase(1746893.5,  0,  -568,    4,   1,  WED,   0,  0,  0),
            new TestCase(1770641.5,  0,  -501,    4,   6,  SUN,   0,  0,  0),
            new TestCase(1892731.5,  0,  -157,   10,  17,  WED,   0,  0,  0),
            new TestCase(1931579.5,  0,   -47,    6,   3,  MON,   0,  0,  0),
            new TestCase(1974851.5,  0,    75,    7,  13,  SAT,   0,  0,  0),
            new TestCase(2091164.5,  0,   403,   10,   5,  SUN,   0,  0,  0),
            new TestCase(2121509.5,  0,   489,    5,  22,  SUN,   0,  0,  0),
            new TestCase(2155779.5,  0,   586,    2,   7,  FRI,   0,  0,  0),
            new TestCase(2174029.5,  0,   637,    8,   7,  SAT,   0,  0,  0),
            new TestCase(2191584.5,  0,   687,    2,  20,  FRI,   0,  0,  0),
            new TestCase(2195261.5,  0,   697,    7,   7,  SUN,   0,  0,  0),
            new TestCase(2229274.5,  0,   793,    7,   1,  SUN,   0,  0,  0),
            new TestCase(2245580.5,  0,   839,    7,   6,  WED,   0,  0,  0),
            new TestCase(2266100.5,  0,   897,    6,   1,  SAT,   0,  0,  0),
            new TestCase(2288542.5,  0,   960,    9,  30,  SAT,   0,  0,  0),
            new TestCase(2290901.5,  0,   967,    5,  27,  SAT,   0,  0,  0),
            new TestCase(2323140.5,  0,  1058,    5,  18,  WED,   0,  0,  0),
            new TestCase(2334848.5,  0,  1091,    6,   2,  SUN,   0,  0,  0),
            new TestCase(2348020.5,  0,  1128,    8,   4,  FRI,   0,  0,  0),
            new TestCase(2366978.5,  0,  1182,    2,   3,  SUN,   0,  0,  0),
            new TestCase(2385648.5,  0,  1234,   10,  10,  MON,   0,  0,  0),
            new TestCase(2392825.5,  0,  1255,    1,  11,  WED,   0,  0,  0),
            new TestCase(2416223.5,  0,  1321,    1,  21,  SUN,   0,  0,  0),
            new TestCase(2425848.5,  0,  1348,    3,  19,  SUN,   0,  0,  0),
            new TestCase(2430266.5,  0,  1360,    9,   8,  MON,   0,  0,  0),
            new TestCase(2430833.5,  0,  1362,    4,  13,  MON,   0,  0,  0),
            new TestCase(2431004.5,  0,  1362,   10,   7,  THU,   0,  0,  0),
            new TestCase(2448698.5,  0,  1412,    9,  13,  TUE,   0,  0,  0),
            new TestCase(2450138.5,  0,  1416,   10,   5,  SUN,   0,  0,  0),
            new TestCase(2465737.5,  0,  1460,   10,  12,  WED,   0,  0,  0),
            new TestCase(2486076.5,  0,  1518,    3,   5,  SUN,   0,  0,  0),
        };
        
        IslamicCalendar civilCalendar = newCivil();
        civilCalendar.setLenient(true);
        doTestCases(tests, civilCalendar);
    }

    public void TestBasic() {
        IslamicCalendar cal = newCivil();
        cal.clear();
        cal.set(1000, 0, 30);
        logln("1000/0/30 -> " +
              cal.get(YEAR) + "/" +
              cal.get(MONTH) + "/" + 
              cal.get(DATE));
        cal.clear();
        cal.set(1, 0, 30);
        logln("1/0/30 -> " +
              cal.get(YEAR) + "/" +
              cal.get(MONTH) + "/" + 
              cal.get(DATE));
    }

    private static IslamicCalendar newCivil() {
        IslamicCalendar civilCalendar = new IslamicCalendar();
        civilCalendar.setCivil(true);
        return civilCalendar;
    }
    
};
