/*********************************************************************
 * Copyright (C) 2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 *********************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/calendar/ChineseTest.java,v $
 * $Date: 2000/11/21 20:20:44 $
 * $Revision: 1.3 $
 */
package com.ibm.test.calendar;
import com.ibm.util.*;
import com.ibm.text.*;
import java.util.Date;
import java.util.Locale;

public class ChineseTest extends CalendarTest {

    public static void main(String args[]) throws Exception {
        new ChineseTest().run(args);
    }

    /**
     * Test basic mapping to and from Gregorian.
     */
    public void TestMapping() {

        final int[] DATA = {
            // (Note: months are 1-based)
            // Gregorian    Chinese
            1964,  9,  4,   4601,  7,0, 28,
            1964,  9,  5,   4601,  7,0, 29,
            1964,  9,  6,   4601,  8,0,  1,
            1964,  9,  7,   4601,  8,0,  2,
            1961, 12, 25,   4598, 11,0, 18,
            1999,  6,  4,   4636,  4,0, 21,
            
            1990,  5, 23,   4627,  4,0, 29,
            1990,  5, 24,   4627,  5,0,  1,
            1990,  6, 22,   4627,  5,0, 30,
            1990,  6, 23,   4627,  5,1,  1,
            1990,  7, 20,   4627,  5,1, 28,
            1990,  7, 21,   4627,  5,1, 29,
            1990,  7, 22,   4627,  6,0,  1,
        };

        ChineseCalendar cal = new ChineseCalendar();
        StringBuffer buf = new StringBuffer();

        logln("Gregorian -> Chinese");
        for (int i=0; i<DATA.length; ) {
            Date date = new Date(DATA[i++]-1900, DATA[i++]-1, DATA[i++]);
            cal.setTime(date);
            int y = cal.get(Calendar.EXTENDED_YEAR);
            int m = cal.get(Calendar.MONTH)+1; // 0-based -> 1-based
            int L = cal.get(ChineseCalendar.IS_LEAP_MONTH);
            int d = cal.get(Calendar.DAY_OF_MONTH);
            int yE = DATA[i++]; // Expected y, m, isLeapMonth, d
            int mE = DATA[i++]; // 1-based
            int LE = DATA[i++];
            int dE = DATA[i++];
            buf.setLength(0);
            buf.append(date + " -> ");
            buf.append(y + "/" + m + (L==1?"(leap)":"") + "/" + d);
            if (y == yE && m == mE && L == LE && d == dE) {
                logln("OK: " + buf.toString());
            } else {
                errln("Fail: " + buf.toString() + ", expected " +
                      yE + "/" + mE + (LE==1?"(leap)":"") + "/" + dE);
            }
        }

        logln("Chinese -> Gregorian");
        for (int i=0; i<DATA.length; ) {
            Date dexp = new Date(DATA[i++]-1900, DATA[i++]-1, DATA[i++]);
            int cyear = DATA[i++];
            int cmonth = DATA[i++];
            int cisleapmonth = DATA[i++];
            int cdayofmonth = DATA[i++];
            cal.clear();
            cal.set(Calendar.EXTENDED_YEAR, cyear);
            cal.set(Calendar.MONTH, cmonth-1);
            cal.set(ChineseCalendar.IS_LEAP_MONTH, cisleapmonth);
            cal.set(Calendar.DAY_OF_MONTH, cdayofmonth);
            Date date = cal.getTime();
            buf.setLength(0);
            buf.append(cyear + "/" + cmonth +
                       (cisleapmonth==1?"(leap)":"") + "/" + cdayofmonth);
            buf.append(" -> " + date);
            if (date.equals(dexp)) {
                logln("OK: " + buf.toString());
            } else {
                errln("Fail: " + buf.toString() + ", expected " + dexp);
            }
        }
    }

    /**
     * Make sure no Gregorian dates map to Chinese 1-based day of
     * month zero.  This was a problem with some of the astronomical
     * new moon determinations.
     */
    public void TestZeroDOM() {
        ChineseCalendar cal = new ChineseCalendar();
        GregorianCalendar greg = new GregorianCalendar(1989, Calendar.SEPTEMBER, 1);
        logln("Start: " + greg.getTime());
        for (int i=0; i<1000; ++i) {
            cal.setTimeInMillis(greg.getTimeInMillis());
            if (cal.get(Calendar.DAY_OF_MONTH) == 0) {
                errln("Fail: " + greg.getTime() + " -> " +
                      cal.get(Calendar.EXTENDED_YEAR) + "/" +
                      cal.get(Calendar.MONTH) +
                      (cal.get(ChineseCalendar.IS_LEAP_MONTH)==1?"(leap)":"") +
                      "/" + cal.get(Calendar.DAY_OF_MONTH));
            }
            greg.add(Calendar.DAY_OF_YEAR, 1);
        }
        logln("End: " + greg.getTime());
    }

    /**
     * Test minimum and maximum functions.
     */
    public void TestLimits() {
        // The number of days and the start date can be adjusted
        // arbitrarily to either speed up the test or make it more
        // thorough, but try to test at least a full year, preferably a
        // full non-leap and a full leap year.

        // Final parameter is either number of days, if > 0, or test
        // duration in seconds, if < 0.
        doLimitsTest(new ChineseCalendar(), null,
                     new Date(1989-1900, Calendar.NOVEMBER, 1), -10);
    }

    /**
     * Run through several standard tests from Dershowitz & Reingold.
     */
    public void TestJulianDayMapping() {

        final TestCase[] tests = {
            //
            // From Dershowitz & Reingold, "Calendrical Calculations".
            //
            // The months in this table are 1-based rather than 0-based.
            //
            // * Failing fields->millis
            // ** Millis->fields gives 0-based month -1
            // These failures were fixed by changing the start search date
            // for the winter solstice from Dec 15 to Dec 1.
            // 
            //                  Julian Day   Era  Year Month  Leap   DOM WkDay
            new ChineseTestCase(1507231.5,   35,   11,    6, false,   12,  SUN),
            new ChineseTestCase(1660037.5,   42,    9,   10, false,   27,  WED),
            new ChineseTestCase(1746893.5,   46,    7,    8, false,    4,  WED),
            new ChineseTestCase(1770641.5,   47,   12,    8, false,    9,  SUN),
            new ChineseTestCase(1892731.5,   52,   46,   11, false,   20,  WED),
            new ChineseTestCase(1931579.5,   54,   33,    4, false,    5,  MON),
            new ChineseTestCase(1974851.5,   56,   31,   10, false,   15,  SAT),
            new ChineseTestCase(2091164.5,   61,   50,    3, false,    7,  SUN),
            new ChineseTestCase(2121509.5,   63,   13,    4, false,   24,  SUN),
            new ChineseTestCase(2155779.5,   64,   47,    2, false,    9,  FRI),
            new ChineseTestCase(2174029.5,   65,   37,    2, false,    9,  SAT),
            new ChineseTestCase(2191584.5,   66,   25,    2, false,   23,  FRI),
            new ChineseTestCase(2195261.5,   66,   35,    3, false,    9,  SUN), //*
            new ChineseTestCase(2229274.5,   68,    8,    5, false,    2,  SUN), //*
            new ChineseTestCase(2245580.5,   68,   53,    1, false,    8,  WED), //**
            new ChineseTestCase(2266100.5,   69,   49,    3, false,    4,  SAT), 
            new ChineseTestCase(2288542.5,   70,   50,    8, false,    2,  SAT), //*
            new ChineseTestCase(2290901.5,   70,   57,    1, false,   29,  SAT), //*
            new ChineseTestCase(2323140.5,   72,   25,    4,  true,   20,  WED), //*
            new ChineseTestCase(2334848.5,   72,   57,    6, false,    5,  SUN),
            new ChineseTestCase(2348020.5,   73,   33,    6, false,    6,  FRI),
            new ChineseTestCase(2366978.5,   74,   25,    5, false,    5,  SUN),
            new ChineseTestCase(2385648.5,   75,   16,    6, false,   12,  MON),
            new ChineseTestCase(2392825.5,   75,   36,    2, false,   13,  WED),
            new ChineseTestCase(2416223.5,   76,   40,    3, false,   22,  SUN),
            new ChineseTestCase(2425848.5,   77,    6,    7, false,   21,  SUN),
            new ChineseTestCase(2430266.5,   77,   18,    8, false,    9,  MON),
            new ChineseTestCase(2430833.5,   77,   20,    3, false,   15,  MON),
            new ChineseTestCase(2431004.5,   77,   20,    9, false,    9,  THU),
            new ChineseTestCase(2448698.5,   78,    9,    2, false,   14,  TUE),
            new ChineseTestCase(2450138.5,   78,   13,    1, false,    7,  SUN),
            new ChineseTestCase(2465737.5,   78,   55,   10, false,   14,  WED),
            new ChineseTestCase(2486076.5,   79,   51,    6, false,    7,  SUN),

            // Additional tests not from D&R
            new ChineseTestCase(2467496.5,   78,   60,    8, false,    2,  FRI), // year 60
        };

        ChineseCalendar cal = new ChineseCalendar();
        cal.setLenient(true);
        doTestCases(tests, cal);
    }

    /**
     * Test formatting.
     *
     * Leap months in this century:
     * Wed May 23 2001 = Month 4(leap), Day 1, Year 18, Cycle 78
     * Sun Mar 21 2004 = Month 2(leap), Day 1, Year 21, Cycle 78
     * Thu Aug 24 2006 = Month 7(leap), Day 1, Year 23, Cycle 78
     * Tue Jun 23 2009 = Month 5(leap), Day 1, Year 26, Cycle 78
     * Mon May 21 2012 = Month 4(leap), Day 1, Year 29, Cycle 78
     * Fri Oct 24 2014 = Month 9(leap), Day 1, Year 31, Cycle 78
     * Sun Jul 23 2017 = Month 6(leap), Day 1, Year 34, Cycle 78
     * Sat May 23 2020 = Month 4(leap), Day 1, Year 37, Cycle 78
     * Wed Mar 22 2023 = Month 2(leap), Day 1, Year 40, Cycle 78
     * Fri Jul 25 2025 = Month 6(leap), Day 1, Year 42, Cycle 78
     * Fri Jun 23 2028 = Month 5(leap), Day 1, Year 45, Cycle 78
     * Tue Apr 22 2031 = Month 3(leap), Day 1, Year 48, Cycle 78
     * Thu Dec 22 2033 = Month 11(leap), Day 1, Year 50, Cycle 78
     * Wed Jul 23 2036 = Month 6(leap), Day 1, Year 53, Cycle 78
     * Wed Jun 22 2039 = Month 5(leap), Day 1, Year 56, Cycle 78
     * Sat Mar 22 2042 = Month 2(leap), Day 1, Year 59, Cycle 78
     * Tue Aug 23 2044 = Month 7(leap), Day 1, Year 01, Cycle 79
     * Sun Jun 23 2047 = Month 5(leap), Day 1, Year 04, Cycle 79
     * Thu Apr 21 2050 = Month 3(leap), Day 1, Year 07, Cycle 79
     * Mon Sep 23 2052 = Month 8(leap), Day 1, Year 09, Cycle 79
     * Sat Jul 24 2055 = Month 6(leap), Day 1, Year 12, Cycle 79
     * Wed May 22 2058 = Month 4(leap), Day 1, Year 15, Cycle 79
     * Wed Apr 20 2061 = Month 3(leap), Day 1, Year 18, Cycle 79
     * Fri Aug 24 2063 = Month 7(leap), Day 1, Year 20, Cycle 79
     * Wed Jun 23 2066 = Month 5(leap), Day 1, Year 23, Cycle 79
     * Tue May 21 2069 = Month 4(leap), Day 1, Year 26, Cycle 79
     * Thu Sep 24 2071 = Month 8(leap), Day 1, Year 28, Cycle 79
     * Tue Jul 24 2074 = Month 6(leap), Day 1, Year 31, Cycle 79
     * Sat May 22 2077 = Month 4(leap), Day 1, Year 34, Cycle 79
     * Sat Apr 20 2080 = Month 3(leap), Day 1, Year 37, Cycle 79
     * Mon Aug 24 2082 = Month 7(leap), Day 1, Year 39, Cycle 79
     * Fri Jun 22 2085 = Month 5(leap), Day 1, Year 42, Cycle 79
     * Fri May 21 2088 = Month 4(leap), Day 1, Year 45, Cycle 79
     * Sun Sep 24 2090 = Month 8(leap), Day 1, Year 47, Cycle 79
     * Thu Jul 23 2093 = Month 6(leap), Day 1, Year 50, Cycle 79
     * Tue May 22 2096 = Month 4(leap), Day 1, Year 53, Cycle 79
     * Sun Mar 22 2099 = Month 2(leap), Day 1, Year 56, Cycle 79
     */
    public void TestFormat() {
        ChineseCalendar cal = new ChineseCalendar();
        DateFormat fmt = DateFormat.getDateTimeInstance(cal,
                                    DateFormat.DEFAULT, DateFormat.DEFAULT);

        Date[] DATA = {
            new Date(2001-1900, Calendar.MAY, 22),
            new Date(2001-1900, Calendar.MAY, 23)
        };
        
        for (int i=0; i<DATA.length; ++i) {
            String s = fmt.format(DATA[i]);
            try {
                Date e = fmt.parse(s);
                if (e.equals(DATA[i])) {
                    logln("Ok: " + DATA[i] + " -> " + s + " -> " + e);
                } else {
                    errln("FAIL: " + DATA[i] + " -> " + s + " -> " + e);
                }
            } catch (java.text.ParseException e) {
                errln("Fail: " + s + " -> parse failure at " + e.getErrorOffset());
                errln(e.toString());
            }
        }
    }
}
