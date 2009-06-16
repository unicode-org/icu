/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

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

    /**
     * Test limits of the Islamic calendar
     */
    public void TestLimits() {
        Calendar cal = Calendar.getInstance();
        cal.set(2007, Calendar.JANUARY, 1);
        IslamicCalendar islamic = newCivil();
        doLimitsTest(islamic, null, cal.getTime());
        doTheoreticalLimitsTest(islamic, true);
    }

    public void TestCoverage() {
    {
        // new IslamicCalendar(TimeZone)
        IslamicCalendar cal = new IslamicCalendar(TimeZone.getDefault());
        if(cal == null){
            errln("could not create IslamicCalendar with TimeZone");
        }
    }

    {
        // new IslamicCalendar(ULocale)
        IslamicCalendar cal = new IslamicCalendar(ULocale.getDefault());
        if(cal == null){
            errln("could not create IslamicCalendar with ULocale");
        }
    }
        
    {
        // new IslamicCalendar(Locale)
        IslamicCalendar cal = new IslamicCalendar(Locale.getDefault());
        if(cal == null){
            errln("could not create IslamicCalendar with Locale");
        }
    }

    {
        // new IslamicCalendar(Date)
        IslamicCalendar cal = new IslamicCalendar(new Date());
        if(cal == null){
            errln("could not create IslamicCalendar with Date");
        }
    }

    {
        // new IslamicCalendar(int year, int month, int date)
        IslamicCalendar cal = new IslamicCalendar(800, IslamicCalendar.RAMADAN, 1);
        if(cal == null){
            errln("could not create IslamicCalendar with year,month,date");
        }
    }

    {
        // new IslamicCalendar(int year, int month, int date, int hour, int minute, int second)
        IslamicCalendar cal = new IslamicCalendar(800, IslamicCalendar.RAMADAN, 1, 1, 1, 1);
        if(cal == null){
            errln("could not create IslamicCalendar with year,month,date,hour,minute,second");
        }
    }

    {
        // setCivil/isCivil
        // operations on non-civil calendar
        IslamicCalendar cal = new IslamicCalendar(800, IslamicCalendar.RAMADAN, 1, 1, 1, 1);
        cal.setCivil(false);
        if (cal.isCivil()) {
        errln("islamic calendar is civil");
        }

        Date now = new Date();
        cal.setTime(now);

        Date then = cal.getTime();
        if (!now.equals(then)) {
        errln("get/set time failed with non-civil islamic calendar");
        }

        logln(then.toString());

        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.YEAR, 1);

        logln(cal.getTime().toString());
    }
    
    {
        // data
        IslamicCalendar cal = new IslamicCalendar(800, IslamicCalendar.RAMADAN, 1);
        Date time = cal.getTime();

        String[] calendarLocales = {
        "ar_AE", "ar_BH", "ar_DZ", "ar_EG", "ar_JO", "ar_KW", "ar_OM", 
        "ar_QA", "ar_SA", "ar_SY", "ar_YE", "ms_MY"
        };

        String[] formatLocales = {
        "en", "ar", "fi", "fr", "hu", "iw", "nl"
        };
        for (int i = 0; i < calendarLocales.length; ++i) {
        String calLocName = calendarLocales[i];
        Locale calLocale = LocaleUtility.getLocaleFromName(calLocName);
        cal = new IslamicCalendar(calLocale);

        for (int j = 0; j < formatLocales.length; ++j) {
            String locName = formatLocales[j];
            Locale formatLocale = LocaleUtility.getLocaleFromName(locName);
            DateFormat format = DateFormat.getDateTimeInstance(cal, DateFormat.FULL, DateFormat.FULL, formatLocale);
            logln(calLocName + "/" + locName + " --> " + format.format(time));
        }
        }
    }
    }

    private static IslamicCalendar newCivil() {
        IslamicCalendar civilCalendar = new IslamicCalendar();
        civilCalendar.setCivil(true);
        return civilCalendar;
    }
    
}
