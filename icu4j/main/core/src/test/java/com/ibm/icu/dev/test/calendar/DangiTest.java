// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DangiCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DangiTest extends CalendarTestFmwk {
    /** Test basic mapping to and from Gregorian. */
    @Test
    public void TestMapping() {
        final int[] DATA = {
            // (Note: months are 1-based)
            // Gregorian    Korean (Dan-gi)
            1964, 9, 4, 4297, 9, 0, 4,
            1964, 9, 5, 4297, 9, 0, 5,
            1964, 9, 6, 4297, 9, 0, 6,
            1964, 9, 7, 4297, 9, 0, 7,
            1961, 12, 25, 4294, 12, 0, 25,
            1999, 6, 4, 4332, 6, 0, 4,
            1990, 5, 23, 4323, 5, 0, 23,
            1990, 5, 24, 4323, 5, 0, 24,
            1990, 6, 22, 4323, 6, 0, 22,
            1990, 6, 23, 4323, 6, 0, 23,
            1990, 7, 20, 4323, 7, 0, 20,
            1990, 7, 21, 4323, 7, 0, 21,
            1990, 7, 22, 4323, 7, 0, 22,

            // Some tricky dates (where GMT+8 doesn't agree with GMT+9)
            //
            // The list is from http://www.math.snu.ac.kr/~kye/others/lunar.html ('kye ref').
            // However, for some dates disagree with the above reference so KASI's
            // calculation was cross-referenced:
            //  http://astro.kasi.re.kr/Life/ConvertSolarLunarForm.aspx?MenuID=115
            1880, 11, 3, 4213, 10, 0, 1, // astronomer's GMT+8 / KASI disagrees with the kye ref
            1882, 12, 10, 4215, 11, 0, 1,
            1883, 7, 4, 4216, 6, 0, 1,
            1884, 4, 25, 4217, 4, 0, 1,
            1885, 5, 14, 4218, 4, 0, 1,
            1891, 1, 10, 4223, 12, 0, 1,
            1893, 4, 16, 4226, 3, 0, 1,
            1894, 5, 5, 4227, 4, 0, 1,
            1896, 1, 1, 4229, 1, 0, 1,
            1897, 7, 29, 4230, 7, 0, 29,
            1897, 10, 11, 4230, 10, 0, 11,
            1897, 10, 12, 4230, 10, 0, 12,
            1903, 10, 20, 4236, 10, 0, 20,
            1904, 1, 17, 4237, 1, 0, 17,
            1904, 11, 7, 4237, 11, 0, 7,
            1905, 5, 4, 4238, 5, 0, 4,
            1907, 7, 10, 4240, 7, 0, 10,
            1908, 4, 30, 4241, 4, 0, 30,
            1908, 9, 25, 4241, 9, 0, 25,
            1909, 9, 14, 4242, 9, 0, 14,
            1911, 12, 20, 4244, 12, 0, 20,
            1976, 11, 22, 4309, 11, 0, 22,
        };

        Calendar cal = Calendar.getInstance(new ULocale("ko_KR@calendar=dangi"));
        StringBuilder buf = new StringBuilder();

        logln("Gregorian -> Korean Lunar (Dangi)");

        Calendar grego = Calendar.getInstance();
        grego.clear();
        for (int i = 0; i < DATA.length; ) {
            grego.set(DATA[i++], DATA[i++] - 1, DATA[i++]);
            Date date = grego.getTime();
            cal.setTime(date);
            int y = cal.get(Calendar.EXTENDED_YEAR);
            int m = cal.get(Calendar.MONTH) + 1; // 0-based -> 1-based
            int L = cal.get(Calendar.IS_LEAP_MONTH);
            int d = cal.get(Calendar.DAY_OF_MONTH);
            int yE = DATA[i++]; // Expected y, m, isLeapMonth, d
            int mE = DATA[i++]; // 1-based
            int LE = DATA[i++];
            int dE = DATA[i++];
            buf.setLength(0);
            buf.append(date + " -> ");
            buf.append(y + "/" + m + (L == 1 ? "(leap)" : "") + "/" + d);
            if (y == yE && m == mE && L == LE && d == dE) {
                logln("OK: " + buf.toString());
            } else {
                errln(
                        "Fail: "
                                + buf.toString()
                                + ", expected "
                                + yE
                                + "/"
                                + mE
                                + (LE == 1 ? "(leap)" : "")
                                + "/"
                                + dE);
            }
        }

        logln("Korean Lunar (Dangi) -> Gregorian");
        for (int i = 0; i < DATA.length; ) {
            grego.set(DATA[i++], DATA[i++] - 1, DATA[i++]);
            Date dexp = grego.getTime();
            int cyear = DATA[i++];
            int cmonth = DATA[i++];
            int cisleapmonth = DATA[i++];
            int cdayofmonth = DATA[i++];
            cal.clear();
            cal.set(Calendar.EXTENDED_YEAR, cyear);
            cal.set(Calendar.MONTH, cmonth - 1);
            cal.set(Calendar.IS_LEAP_MONTH, cisleapmonth);
            cal.set(Calendar.DAY_OF_MONTH, cdayofmonth);
            Date date = cal.getTime();
            buf.setLength(0);
            buf.append(
                    cyear + "/" + cmonth + (cisleapmonth == 1 ? "(leap)" : "") + "/" + cdayofmonth);
            buf.append(" -> " + date);
            if (date.equals(dexp)) {
                logln("OK: " + buf.toString());
            } else {
                errln("Fail: " + buf.toString() + ", expected " + dexp);
            }
        }

        grego.set(1896, Calendar.JANUARY, 1);
        Date cutover = grego.getTime();
        cal.clear();
        cal.set(Calendar.EXTENDED_YEAR, 4228);
        cal.set(Calendar.MONTH, Calendar.NOVEMBER);
        cal.set(Calendar.IS_LEAP_MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 17);
        assertEquals(
                "Historical lunar cutover input should still resolve to Gregorian 1896-01-01",
                cutover,
                cal.getTime());
    }

    /**
     * Make sure no Gregorian dates map to Chinese 1-based day of month zero. This was a problem
     * with some of the astronomical new moon determinations.
     */
    @Test
    public void TestZeroDOM() {
        Calendar cal = Calendar.getInstance(new ULocale("ko_KR@calendar=dangi"));
        GregorianCalendar greg = new GregorianCalendar(1989, Calendar.SEPTEMBER, 1);
        logln("Start: " + greg.getTime());
        for (int i = 0; i < 1000; ++i) {
            cal.setTimeInMillis(greg.getTimeInMillis());
            if (cal.get(Calendar.DAY_OF_MONTH) == 0) {
                errln(
                        "Fail: "
                                + greg.getTime()
                                + " -> "
                                + cal.get(Calendar.YEAR)
                                + "/"
                                + cal.get(Calendar.MONTH)
                                + (cal.get(Calendar.IS_LEAP_MONTH) == 1 ? "(leap)" : "")
                                + "/"
                                + cal.get(Calendar.DAY_OF_MONTH));
            }
            greg.add(Calendar.DAY_OF_YEAR, 1);
        }
        logln("End: " + greg.getTime());
    }

    /** Test minimum and maximum functions. */
    @Test
    public void TestLimits() {
        // The number of days and the start date can be adjusted
        // arbitrarily to either speed up the test or make it more
        // thorough, but try to test at least a full year, preferably a
        // full non-leap and a full leap year.

        // Final parameter is either number of days, if > 0, or test
        // duration in seconds, if < 0.
        Calendar tempcal = Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1989, Calendar.NOVEMBER, 1);
        Calendar dangi = Calendar.getInstance(new ULocale("ko_KR@calendar=dangi"));
        doLimitsTest(dangi, null, tempcal.getTime());
        doTheoreticalLimitsTest(dangi, true);
    }

    /** Make sure IS_LEAP_MONTH participates in field resolution. */
    @Test
    public void TestResolution() {
        Calendar cal = Calendar.getInstance(new ULocale("ko_KR@calendar=dangi"));
        DateFormat fmt = DateFormat.getDateInstance(cal, DateFormat.DEFAULT);

        // June 22 1895 = y4228 m5 d30 doy148
        // June 23 1895 = y4228 m5* d1 doy149

        final int THE_YEAR = 4228;
        final int END = -1;

        int[] DATA = {
            // Format:
            // (field, value)+, END, exp.month, exp.isLeapMonth, exp.DOM
            // Note: exp.month is ONE-BASED

            // If we set DAY_OF_YEAR only, that should be used
            Calendar.DAY_OF_YEAR,
            1,
            END,
            1,
            0,
            1, // Expect 1-1

            // If we set MONTH only, that should be used
            Calendar.IS_LEAP_MONTH,
            1,
            Calendar.DAY_OF_MONTH,
            1,
            Calendar.MONTH,
            4,
            END,
            5,
            1,
            1, // Expect 5*-1

            // If we set the DOY last, that should take precedence
            Calendar.MONTH,
            1, // Should ignore
            Calendar.IS_LEAP_MONTH,
            1, // Should ignore
            Calendar.DAY_OF_MONTH,
            1, // Should ignore
            Calendar.DAY_OF_YEAR,
            151,
            END,
            5,
            1,
            3, // Expect 5*-3

            // If we set IS_LEAP_MONTH last, that should take precedence
            Calendar.MONTH,
            4,
            Calendar.DAY_OF_MONTH,
            1,
            Calendar.DAY_OF_YEAR,
            5, // Should ignore
            Calendar.IS_LEAP_MONTH,
            1,
            END,
            5,
            1,
            1, // Expect 5*-1
        };

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < DATA.length; ) {
            cal.clear();
            cal.set(Calendar.EXTENDED_YEAR, THE_YEAR);
            buf.setLength(0);
            buf.append("EXTENDED_YEAR=" + THE_YEAR);
            while (DATA[i] != END) {
                cal.set(DATA[i++], DATA[i++]);
                buf.append(" " + fieldName(DATA[i - 2]) + "=" + DATA[i - 1]);
            }
            ++i; // Skip over END mark
            int expMonth = DATA[i++] - 1;
            int expIsLeapMonth = DATA[i++];
            int expDOM = DATA[i++];
            int month = cal.get(Calendar.MONTH);
            int isLeapMonth = cal.get(Calendar.IS_LEAP_MONTH);
            int dom = cal.get(Calendar.DAY_OF_MONTH);
            if (expMonth == month && expIsLeapMonth == isLeapMonth && dom == expDOM) {
                logln("OK: " + buf + " => " + fmt.format(cal.getTime()));
            } else {
                String s = fmt.format(cal.getTime());
                cal.clear();
                cal.set(Calendar.EXTENDED_YEAR, THE_YEAR);
                cal.set(Calendar.MONTH, expMonth);
                cal.set(Calendar.IS_LEAP_MONTH, expIsLeapMonth);
                cal.set(Calendar.DAY_OF_MONTH, expDOM);
                errln(
                        "Fail: "
                                + buf
                                + " => "
                                + s
                                + "="
                                + (month + 1)
                                + ","
                                + isLeapMonth
                                + ","
                                + dom
                                + ", expected "
                                + fmt.format(cal.getTime())
                                + "="
                                + (expMonth + 1)
                                + ","
                                + expIsLeapMonth
                                + ","
                                + expDOM);
            }
        }
    }

    /** Test the behavior of fields that are out of range. */
    @Test
    public void TestOutOfRange() {
        int[] DATA =
                new int[] {
                    // Input       Output
                    4334, 13, 1, 4335, 1, 1,
                    4334, 18, 1, 4335, 6, 1,
                    4335, 0, 1, 4334, 12, 1,
                    4335, -6, 1, 4334, 6, 1,
                    4334, 1, 32, 4334, 2, 1,
                    4334, 2, -1, 4334, 1, 30,
                };
        Calendar cal = Calendar.getInstance(new ULocale("ko_KR@calendar=dangi"));
        for (int i = 0; i < DATA.length; ) {
            int y1 = DATA[i++];
            int m1 = DATA[i++] - 1;
            int d1 = DATA[i++];
            int y2 = DATA[i++];
            int m2 = DATA[i++] - 1;
            int d2 = DATA[i++];
            cal.clear();
            cal.set(Calendar.EXTENDED_YEAR, y1);
            cal.set(MONTH, m1);
            cal.set(DATE, d1);
            int y = cal.get(Calendar.EXTENDED_YEAR);
            int m = cal.get(MONTH);
            int d = cal.get(DATE);
            if (y != y2 || m != m2 || d != d2) {
                errln(
                        "Fail: "
                                + y1
                                + "/"
                                + (m1 + 1)
                                + "/"
                                + d1
                                + " resolves to "
                                + y
                                + "/"
                                + (m + 1)
                                + "/"
                                + d
                                + ", expected "
                                + y2
                                + "/"
                                + (m2 + 1)
                                + "/"
                                + d2);
            } else if (isVerbose()) {
                logln(
                        "OK: "
                                + y1
                                + "/"
                                + (m1 + 1)
                                + "/"
                                + d1
                                + " resolves to "
                                + y
                                + "/"
                                + (m + 1)
                                + "/"
                                + d);
            }
        }
    }

    /**
     * Test the behavior of KoreanLunarCalendar.add(). The only real nastiness with roll is the
     * MONTH field around leap months.
     */
    @Test
    public void TestAdd() {
        int[][] tests =
                new int[][] {
                    // MONTHS ARE 1-BASED HERE
                    // input               add           output
                    // year  mon    day    field amount  year  mon    day
                    {4338, 3, 0, 15, MONTH, 3, 4338, 6, 0, 15}, // normal
                    {4335, 12, 0, 15, MONTH, 1, 4336, 1, 0, 15}, // across year
                    {4336, 1, 0, 15, MONTH, -1, 4335, 12, 0, 15}, // across year
                    {4334, 1, 0, 31, MONTH, 1, 4334, 2, 0, 28}, // dom should pin
                    {4337, 1, 0, 31, MONTH, 1, 4337, 2, 0, 29}, // leap Gregorian year
                    {4228, 11, 0, 17, MONTH, 1, 4229, 2, 0, 1}, // historical lunar input
                };

        Calendar cal = Calendar.getInstance(new ULocale("ko_KR@calendar=dangi"));
        doRollAddDangi(ADD, cal, tests);
    }

    /**
     * Test the behavior of KoreanLunarCalendar.roll(). The only real nastiness with roll is the
     * MONTH field around leap months.
     */
    @Test
    public void TestRoll() {
        int[][] tests =
                new int[][] {
                    // MONTHS ARE 1-BASED HERE
                    // input               add           output
                    // year  mon    day    field amount  year  mon    day
                    {4338, 3, 0, 15, MONTH, 3, 4338, 6, 0, 15}, // normal
                    {4338, 3, 0, 15, MONTH, 11, 4338, 2, 0, 15}, // normal
                    {4335, 12, 0, 15, MONTH, 1, 4335, 1, 0, 15}, // across year
                    {4336, 1, 0, 15, MONTH, -1, 4336, 12, 0, 15}, // across year
                    {4334, 1, 0, 31, MONTH, 1, 4334, 2, 0, 28}, // dom should pin
                    {4337, 1, 0, 31, MONTH, 1, 4337, 2, 0, 29}, // leap Gregorian year
                    {4334, 3, 0, 30, MONTH, -1, 4334, 2, 0, 28}, // dom should pin
                };

        Calendar cal = Calendar.getInstance(new ULocale("ko_KR@calendar=dangi"));
        doRollAddDangi(ROLL, cal, tests);
    }

    void doRollAddDangi(boolean roll, Calendar cal, int[][] tests) {
        String name = roll ? "rolling" : "adding";

        for (int i = 0; i < tests.length; i++) {
            int[] test = tests[i];

            cal.clear();
            cal.set(Calendar.EXTENDED_YEAR, test[0]);
            cal.set(Calendar.MONTH, test[1] - 1);
            cal.set(Calendar.IS_LEAP_MONTH, test[2]);
            cal.set(Calendar.DAY_OF_MONTH, test[3]);
            if (roll) {
                cal.roll(test[4], test[5]);
            } else {
                cal.add(test[4], test[5]);
            }
            if (cal.get(Calendar.EXTENDED_YEAR) != test[6]
                    || cal.get(MONTH) != (test[7] - 1)
                    || cal.get(Calendar.IS_LEAP_MONTH) != test[8]
                    || cal.get(DATE) != test[9]) {
                errln(
                        "Fail: "
                                + name
                                + " "
                                + ymdToString(test[0], test[1] - 1, test[2], test[3])
                                + " "
                                + fieldName(test[4])
                                + " by "
                                + test[5]
                                + ": expected "
                                + ymdToString(test[6], test[7] - 1, test[8], test[9])
                                + ", got "
                                + ymdToString(cal));
            } else if (isVerbose()) {
                logln(
                        "OK: "
                                + name
                                + " "
                                + ymdToString(test[0], test[1] - 1, test[2], test[3])
                                + " "
                                + fieldName(test[4])
                                + " by "
                                + test[5]
                                + ": got "
                                + ymdToString(cal));
            }
        }
    }

    /**
     * Convert year,month,day values to the form "year/month/day". On input the month value is
     * zero-based, but in the result string it is one-based.
     */
    public static String ymdToString(int year, int month, int isLeapMonth, int day) {
        return "" + year + "/" + (month + 1) + ((isLeapMonth != 0) ? "(leap)" : "") + "/" + day;
    }

    @Test
    public void TestCoverage() {
        // DangiCalendar()
        // DangiCalendar(Date)
        // DangiCalendar(TimeZone, ULocale)
        Date d = new Date();

        DangiCalendar cal1 = new DangiCalendar();
        cal1.setTime(d);

        DangiCalendar cal2 = new DangiCalendar(d);

        DangiCalendar cal3 = new DangiCalendar(TimeZone.getDefault(), ULocale.getDefault());
        cal3.setTime(d);

        assertEquals("DangiCalendar() and DangiCalendar(Date)", cal1, cal2);
        assertEquals("DangiCalendar() and DangiCalendar(TimeZone,ULocale)", cal1, cal3);

        // String getType()
        String type = cal1.getType();
        assertEquals("getType()", "dangi", type);
    }

    @Test
    public void TestInitWithCurrentTime() {
        // If the chinese calendar current millis isn't called, the default year is wrong.
        // this test is assuming the 'year' is the current cycle
        // so when we cross a cycle boundary, the target will need to change
        // that shouldn't be for awhile yet...

        Calendar cc = Calendar.getInstance(new ULocale("ko_KR@calendar=dangi"));
        cc.set(Calendar.EXTENDED_YEAR, 4338);
        cc.set(Calendar.MONTH, 0);
        // need to set leap month flag off, otherwise, the test case always fails when
        // current time is in a leap month
        cc.set(Calendar.IS_LEAP_MONTH, 0);
        cc.set(Calendar.DATE, 19);
        cc.set(Calendar.HOUR_OF_DAY, 0);
        cc.set(Calendar.MINUTE, 0);
        cc.set(Calendar.SECOND, 0);
        cc.set(Calendar.MILLISECOND, 0);

        cc.add(Calendar.DATE, 1);

        Calendar cal = new GregorianCalendar(2005, Calendar.JANUARY, 20);
        Date target = cal.getTime();
        Date result = cc.getTime();

        assertEquals("chinese and gregorian date should match", target, result);
    }
}
