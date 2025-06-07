// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.CalendarAstronomer;
import com.ibm.icu.impl.CalendarAstronomer.Equatorial;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// TODO: try finding next new moon after  07/28/1984 16:00 GMT

@RunWith(JUnit4.class)
public class AstroTest extends CoreTestFmwk {
    static final double PI = Math.PI;

    @Test
    public void TestSolarLongitude() {
        GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
        // year, month, day, hour, minute, longitude (radians), ascension(radians),
        // declination(radians)
        final double tests[][] = {
            {1980, 7, 27, 00, 00, 2.166442986535465, 2.2070499713207730, 0.3355704075759270},
            {1988, 7, 27, 00, 00, 2.167484927693959, 2.2081183335606176, 0.3353093444275315},
        };
        logln("");
        for (int i = 0; i < tests.length; i++) {
            gc.clear();
            gc.set(
                    (int) tests[i][0],
                    (int) tests[i][1] - 1,
                    (int) tests[i][2],
                    (int) tests[i][3],
                    (int) tests[i][4]);

            CalendarAstronomer astro = new CalendarAstronomer(gc.getTimeInMillis());

            double longitude = astro.getSunLongitude();
            if (longitude != tests[i][5]) {
                if ((float) longitude == (float) tests[i][5]) {
                    logln(
                            "longitude("
                                    + longitude
                                    + ") !=  tests[i][5]("
                                    + tests[i][5]
                                    + ") in double for test "
                                    + i);
                } else {
                    errln(
                            "FAIL: longitude("
                                    + longitude
                                    + ") !=  tests[i][5]("
                                    + tests[i][5]
                                    + ") for test "
                                    + i);
                }
            }
        }
    }

    @Test
    public void TestLunarPosition() {
        GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
        // year, month, day, hour, minute, ascension(radians), declination(radians)
        final double tests[][] = {
            {1979, 2, 26, 16, 00, -0.3778379118188744, -0.1399698825594198},
        };
        logln("");

        for (int i = 0; i < tests.length; i++) {
            gc.clear();
            gc.set(
                    (int) tests[i][0],
                    (int) tests[i][1] - 1,
                    (int) tests[i][2],
                    (int) tests[i][3],
                    (int) tests[i][4]);
            CalendarAstronomer astro = new CalendarAstronomer(gc.getTimeInMillis());

            Equatorial result = astro.getMoonPosition();
            if (result.ascension != tests[i][5]) {
                if ((float) result.ascension == (float) tests[i][5]) {
                    logln(
                            "result.ascension("
                                    + result.ascension
                                    + ") !=  tests[i][5]("
                                    + tests[i][5]
                                    + ") in double for test "
                                    + i);
                } else {
                    errln(
                            "FAIL: result.ascension("
                                    + result.ascension
                                    + ") !=  tests[i][5]("
                                    + tests[i][5]
                                    + ") for test "
                                    + i);
                }
            }
            if (result.declination != tests[i][6]) {
                if ((float) result.declination == (float) tests[i][6]) {
                    logln(
                            "result.declination("
                                    + result.declination
                                    + ") !=  tests[i][6]("
                                    + tests[i][6]
                                    + ") in double for test "
                                    + i);
                } else {
                    errln(
                            "FAIL: result.declination("
                                    + result.declination
                                    + ") !=  tests[i][6]("
                                    + tests[i][6]
                                    + ") for test "
                                    + i);
                }
            }
        }
    }

    @Test
    public void TestCoordinates() {
        CalendarAstronomer astro = new CalendarAstronomer();
        Equatorial result =
                astro.eclipticToEquatorial(139.686111 * PI / 180.0, 4.875278 * PI / 180.0);
        logln("result is " + result + ";  " + result.toHmsString());
    }

    @Test
    public void TestCoverage() {
        GregorianCalendar cal = new GregorianCalendar(1958, Calendar.AUGUST, 15);
        CalendarAstronomer myastro = new CalendarAstronomer(cal.getTimeInMillis());

        // Latitude:  34 degrees 05' North
        // Longitude:  118 degrees 22' West
        double laLat = 34 + 5d / 60, laLong = 360 - (118 + 22d / 60);

        double eclLat = laLat * Math.PI / 360;
        double eclLong = laLong * Math.PI / 360;

        CalendarAstronomer[] astronomers = {
            myastro, myastro, myastro // check cache
        };

        for (int i = 0; i < astronomers.length; ++i) {
            CalendarAstronomer astro = astronomers[i];

            logln("astro: " + astro);
            logln("   time: " + astro.getTime());
            logln("   date: " + astro.getDate());
            logln("   equ long: " + astro.eclipticToEquatorial(eclLat, eclLong));
        }
    }

    @Test
    public void TestBasics() {
        // Check that our JD computation is the same as the book's (p. 88)
        GregorianCalendar cal3 = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
        DateFormat d3 =
                DateFormat.getDateTimeInstance(
                        cal3, DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
        cal3.clear();
        cal3.set(Calendar.YEAR, 1980);
        cal3.set(Calendar.MONTH, Calendar.JULY);
        cal3.set(Calendar.DATE, 27);
        CalendarAstronomer astro = new CalendarAstronomer(cal3.getTimeInMillis());
        double jd = astro.getJulianDay() - 2447891.5;
        double exp = -3444;
        if (jd == exp) {
            logln(d3.format(cal3.getTime()) + " => " + jd);
        } else {
            errln("FAIL: " + d3.format(cal3.getTime()) + " => " + jd + ", expected " + exp);
        }

        //        cal3.clear();
        //        cal3.set(cal3.YEAR, 1990);
        //        cal3.set(cal3.MONTH, Calendar.JANUARY);
        //        cal3.set(cal3.DATE, 1);
        //        cal3.add(cal3.DATE, -1);
        //        astro.setDate(cal3.getTime());
        //        astro.foo();
    }

    @Test
    public void TestMoonAge() {
        GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "GMT"));
        // more testcases are around the date 05/20/2012
        // ticket#3785  UDate ud0 = 1337557623000.0;
        double testcase[][] = {
            {2012, 5, 20, 16, 48, 59},
            {2012, 5, 20, 16, 47, 34},
            {2012, 5, 21, 00, 00, 00},
            {2012, 5, 20, 14, 55, 59},
            {2012, 5, 21, 7, 40, 40},
            {2023, 9, 25, 10, 00, 00},
            {2008, 7, 7, 15, 00, 33},
            {1832, 9, 24, 2, 33, 41},
            {2016, 1, 31, 23, 59, 59},
            {2099, 5, 20, 14, 55, 59}
        };
        // Moon phase angle - Got from http://www.moonsystem.to/checkupe.htm
        double angle[] = {
            356.8493418421329,
            356.8386760059673,
            0.09625415252237701,
            355.9986960782416,
            3.5714026601303317,
            124.26906744384183,
            59.80247650195558,
            357.54163205513123,
            268.41779281511094,
            4.82340276581624
        };
        double precision = PI / 32;
        for (int i = 0; i < testcase.length; i++) {
            gc.clear();
            String testString =
                    "CASE["
                            + i
                            + "]: Year "
                            + (int) testcase[i][0]
                            + " Month "
                            + (int) testcase[i][1]
                            + " Day "
                            + (int) testcase[i][2]
                            + " Hour "
                            + (int) testcase[i][3]
                            + " Minutes "
                            + (int) testcase[i][4]
                            + " Seconds "
                            + (int) testcase[i][5];
            gc.set(
                    (int) testcase[i][0],
                    (int) testcase[i][1] - 1,
                    (int) testcase[i][2],
                    (int) testcase[i][3],
                    (int) testcase[i][4],
                    (int) testcase[i][5]);
            CalendarAstronomer calastro = new CalendarAstronomer(gc.getTimeInMillis());
            double expectedAge = (angle[i] * PI) / 180;
            double got = calastro.getMoonAge();
            logln(testString);
            if (!(got > expectedAge - precision && got < expectedAge + precision)) {
                errln("FAIL: expected " + expectedAge + " got " + got);
            } else {
                logln("PASS: expected " + expectedAge + " got " + got);
            }
        }
    }
}
