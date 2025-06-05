// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2013, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.calendar;

import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the <code>HebrewCalendar</code> class. */
@RunWith(JUnit4.class)
public class HebrewTest extends CalendarTestFmwk {
    // Constants to save typing.
    public static final int TISHRI = HebrewCalendar.TISHRI;
    public static final int HESHVAN = HebrewCalendar.HESHVAN;
    public static final int KISLEV = HebrewCalendar.KISLEV;
    public static final int TEVET = HebrewCalendar.TEVET;
    public static final int SHEVAT = HebrewCalendar.SHEVAT;
    public static final int ADAR_1 = HebrewCalendar.ADAR_1;
    public static final int ADAR = HebrewCalendar.ADAR;
    public static final int NISAN = HebrewCalendar.NISAN;
    public static final int IYAR = HebrewCalendar.IYAR;
    public static final int SIVAN = HebrewCalendar.SIVAN;
    public static final int TAMUZ = HebrewCalendar.TAMUZ;
    public static final int AV = HebrewCalendar.AV;
    public static final int ELUL = HebrewCalendar.ELUL;

    /**
     * Test the behavior of HebrewCalendar.roll The only real nastiness with roll is the MONTH
     * field, since a year can have a variable number of months.
     */
    @Test
    public void TestRoll() {
        int[][] tests =
                new int[][] {
                    //       input                roll by          output
                    //  year  month     day     field amount    year  month     day

                    {5759, HESHVAN, 2, MONTH, 1, 5759, KISLEV, 2}, // non-leap years
                    {5759, SHEVAT, 2, MONTH, 1, 5759, ADAR, 2},
                    {5759, SHEVAT, 2, MONTH, 2, 5759, NISAN, 2},
                    {5759, SHEVAT, 2, MONTH, 12, 5759, SHEVAT, 2},
                    {5759, AV, 1, MONTH, 12, 5759, AV, 1}, // Alan
                    {5757, HESHVAN, 2, MONTH, 1, 5757, KISLEV, 2}, // leap years
                    {5757, SHEVAT, 2, MONTH, 1, 5757, ADAR_1, 2},
                    {5757, SHEVAT, 2, MONTH, 2, 5757, ADAR, 2},
                    {5757, SHEVAT, 2, MONTH, 3, 5757, NISAN, 2},
                    {5757, SHEVAT, 2, MONTH, 12, 5757, TEVET, 2},
                    {5757, SHEVAT, 2, MONTH, 13, 5757, SHEVAT, 2},
                    {5757, AV, 1, MONTH, 12, 5757, TAMUZ, 1}, // Alan
                    {5757, KISLEV, 1, DATE, 30, 5757, KISLEV, 2}, // 29-day month
                    {5758, KISLEV, 1, DATE, 31, 5758, KISLEV, 2}, // 30-day month

                    // Try some other fields too
                    {5757, TISHRI, 1, YEAR, 1, 5758, TISHRI, 1},

                    // Try some rolls that require other fields to be adjusted
                    {5757, TISHRI, 30, MONTH, 1, 5757, HESHVAN, 29},
                    {5758, KISLEV, 30, YEAR, -1, 5757, KISLEV, 29},
                };
        //        try{
        HebrewCalendar cal = new HebrewCalendar(UTC, Locale.getDefault());

        doRollAdd(ROLL, cal, tests);
        //       }catch(MissingResourceException ex){
        //            warnln("Got Exception: "+ ex.getMessage());
        //       }
    }

    /**
     * Test the behavior of HebrewCalendar.roll The only real nastiness with roll is the MONTH
     * field, since a year can have a variable number of months.
     */
    @Test
    public void TestAdd() {
        int[][] tests =
                new int[][] {
                    //       input                add by          output
                    //  year  month     day     field amount    year  month     day
                    {5759, HESHVAN, 2, MONTH, 1, 5759, KISLEV, 2}, // non-leap years
                    {5759, SHEVAT, 2, MONTH, 1, 5759, ADAR, 2},
                    {5759, SHEVAT, 2, MONTH, 2, 5759, NISAN, 2},
                    {5759, SHEVAT, 2, MONTH, 12, 5760, SHEVAT, 2},
                    {5757, HESHVAN, 2, MONTH, 1, 5757, KISLEV, 2}, // leap years
                    {5757, SHEVAT, 2, MONTH, 1, 5757, ADAR_1, 2},
                    {5757, SHEVAT, 2, MONTH, 2, 5757, ADAR, 2},
                    {5757, SHEVAT, 2, MONTH, 3, 5757, NISAN, 2},
                    {5757, SHEVAT, 2, MONTH, 12, 5758, TEVET, 2},
                    {5757, SHEVAT, 2, MONTH, 13, 5758, SHEVAT, 2},
                    {5762, AV, 1, MONTH, 1, 5762, ELUL, 1}, // JB#2327
                    {5762, AV, 30, DATE, 1, 5762, ELUL, 1}, // JB#2327
                    {5762, ELUL, 1, DATE, -1, 5762, AV, 30}, // JB#2327
                    {5762, ELUL, 1, MONTH, -1, 5762, AV, 1}, // JB#2327
                    {5757, KISLEV, 1, DATE, 30, 5757, TEVET, 2}, // 29-day month
                    {5758, KISLEV, 1, DATE, 31, 5758, TEVET, 2}, // 30-day month
                };
        try {
            HebrewCalendar cal = new HebrewCalendar(UTC, Locale.getDefault());

            doRollAdd(ADD, cal, tests);
        } catch (MissingResourceException ex) {
            warnln("Could not load the locale data");
        }
    }

    /**
     * A huge list of test cases to make sure that computeTime and computeFields work properly for a
     * wide range of data.
     */
    @Test
    public void TestCases() {
        try {
            final TestCase[] testCases = {
                //
                // Most of these test cases were taken from the back of
                // "Calendrical Calculations", with some extras added to help
                // debug a few of the problems that cropped up in development.
                //
                // The months in this table are 1-based rather than 0-based,
                // because it's easier to edit that way.
                //
                //         Julian Day  Era  Year  Month Day  WkDay Hour Min Sec
                new TestCase(1507231.5, 0, 3174, 12, 10, SUN, 0, 0, 0),
                new TestCase(1660037.5, 0, 3593, 3, 25, WED, 0, 0, 0),
                new TestCase(1746893.5, 0, 3831, 1, 3, WED, 0, 0, 0),
                new TestCase(1770641.5, 0, 3896, 1, 9, SUN, 0, 0, 0),
                new TestCase(1892731.5, 0, 4230, 4, 18, WED, 0, 0, 0),
                new TestCase(1931579.5, 0, 4336, 10, 4, MON, 0, 0, 0),
                new TestCase(1974851.5, 0, 4455, 2, 13, SAT, 0, 0, 0),
                new TestCase(2091164.5, 0, 4773, 9, 6, SUN, 0, 0, 0),
                new TestCase(2121509.5, 0, 4856, 9, 23, SUN, 0, 0, 0),
                new TestCase(2155779.5, 0, 4950, 8, 7, FRI, 0, 0, 0),
                new TestCase(2174029.5, 0, 5000, 7, 8, SAT, 0, 0, 0),
                new TestCase(2191584.5, 0, 5048, 8, 21, FRI, 0, 0, 0),
                new TestCase(2195261.5, 0, 5058, 9, 7, SUN, 0, 0, 0),
                new TestCase(2229274.5, 0, 5151, 11, 1, SUN, 0, 0, 0),
                new TestCase(2245580.5, 0, 5196, 5, 7, WED, 0, 0, 0),
                new TestCase(2266100.5, 0, 5252, 8, 3, SAT, 0, 0, 0),
                new TestCase(2288542.5, 0, 5314, 1, 1, SAT, 0, 0, 0),
                new TestCase(2290901.5, 0, 5320, 6, 27, SAT, 0, 0, 0),
                new TestCase(2323140.5, 0, 5408, 10, 20, WED, 0, 0, 0),
                new TestCase(2334551.5, 0, 5440, 1, 1, THU, 0, 0, 0),
                new TestCase(2334581.5, 0, 5440, 2, 1, SAT, 0, 0, 0),
                new TestCase(2334610.5, 0, 5440, 3, 1, SUN, 0, 0, 0),
                new TestCase(2334639.5, 0, 5440, 4, 1, MON, 0, 0, 0),
                new TestCase(2334668.5, 0, 5440, 5, 1, TUE, 0, 0, 0),
                new TestCase(2334698.5, 0, 5440, 6, 1, THU, 0, 0, 0),
                new TestCase(2334728.5, 0, 5440, 7, 1, SAT, 0, 0, 0),
                new TestCase(2334757.5, 0, 5440, 8, 1, SUN, 0, 0, 0),
                new TestCase(2334787.5, 0, 5440, 9, 1, TUE, 0, 0, 0),
                new TestCase(2334816.5, 0, 5440, 10, 1, WED, 0, 0, 0),
                new TestCase(2334846.5, 0, 5440, 11, 1, FRI, 0, 0, 0),
                new TestCase(2334848.5, 0, 5440, 11, 3, SUN, 0, 0, 0),
                new TestCase(2334934.5, 0, 5441, 1, 1, TUE, 0, 0, 0),
                new TestCase(2348020.5, 0, 5476, 12, 5, FRI, 0, 0, 0),
                new TestCase(2366978.5, 0, 5528, 11, 4, SUN, 0, 0, 0),
                new TestCase(2385648.5, 0, 5579, 12, 11, MON, 0, 0, 0),
                new TestCase(2392825.5, 0, 5599, 8, 12, WED, 0, 0, 0),
                new TestCase(2416223.5, 0, 5663, 8, 22, SUN, 0, 0, 0),
                new TestCase(2425848.5, 0, 5689, 12, 19, SUN, 0, 0, 0),
                new TestCase(2430266.5, 0, 5702, 1, 8, MON, 0, 0, 0),
                new TestCase(2430833.5, 0, 5703, 8, 14, MON, 0, 0, 0),
                new TestCase(2431004.5, 0, 5704, 1, 8, THU, 0, 0, 0),
                new TestCase(2448698.5, 0, 5752, 7, 12, TUE, 0, 0, 0),
                new TestCase(2450138.5, 0, 5756, 7, 5, SUN, 0, 0, 0),
                new TestCase(2465737.5, 0, 5799, 2, 12, WED, 0, 0, 0),
                new TestCase(2486076.5, 0, 5854, 12, 5, SUN, 0, 0, 0),

                // Additional test cases for bugs found during development
                //           G.YY/MM/DD  Era  Year  Month Day  WkDay Hour Min Sec
                new TestCase(1013, 9, 8, 0, 4774, 1, 1, TUE, 0, 0, 0),
                new TestCase(1239, 9, 1, 0, 5000, 1, 1, THU, 0, 0, 0),
                new TestCase(1240, 9, 18, 0, 5001, 1, 1, TUE, 0, 0, 0),

                // Test cases taken from a table of 14 "year types" in the Help file
                // of the application "Hebrew Calendar"
                new TestCase(2456187.5, 0, 5773, 1, 1, MON, 0, 0, 0),
                new TestCase(2459111.5, 0, 5781, 1, 1, SAT, 0, 0, 0),
                new TestCase(2453647.5, 0, 5766, 1, 1, TUE, 0, 0, 0),
                new TestCase(2462035.5, 0, 5789, 1, 1, THU, 0, 0, 0),
                new TestCase(2458756.5, 0, 5780, 1, 1, MON, 0, 0, 0),
                new TestCase(2460586.5, 0, 5785, 1, 1, THU, 0, 0, 0),
                new TestCase(2463864.5, 0, 5794, 1, 1, SAT, 0, 0, 0),
                new TestCase(2463481.5, 0, 5793, 1, 1, MON, 0, 0, 0),
                new TestCase(2470421.5, 0, 5812, 1, 1, THU, 0, 0, 0),
                new TestCase(2460203.5, 0, 5784, 1, 1, SAT, 0, 0, 0),
                new TestCase(2459464.5, 0, 5782, 1, 1, TUE, 0, 0, 0),
                new TestCase(2467142.5, 0, 5803, 1, 1, MON, 0, 0, 0),
                new TestCase(2455448.5, 0, 5771, 1, 1, THU, 0, 0, 0),

                // Test cases for JB#2327
                // http://www.fourmilab.com/documents/calendar/
                // http://www.calendarhome.com/converter/
                //                2452465.5, 2002, JULY, 10, 5762, AV, 1,
                //                2452494.5, 2002, AUGUST, 8, 5762, AV, 30,
                //                2452495.5, 2002, AUGUST, 9, 5762, ELUL, 1,
                //                2452523.5, 2002, SEPTEMBER, 6, 5762, ELUL, 29,
                //                2452524.5, 2002, SEPTEMBER, 7, 5763, TISHRI, 1,
                //         Julian Day  Era  Year  Month Day  WkDay Hour Min Sec
                new TestCase(2452465.5, 0, 5762, AV + 1, 1, WED, 0, 0, 0),
                new TestCase(2452494.5, 0, 5762, AV + 1, 30, THU, 0, 0, 0),
                new TestCase(2452495.5, 0, 5762, ELUL + 1, 1, FRI, 0, 0, 0),
                new TestCase(2452523.5, 0, 5762, ELUL + 1, 29, FRI, 0, 0, 0),
                new TestCase(2452524.5, 0, 5763, TISHRI + 1, 1, SAT, 0, 0, 0),
            };
            doTestCases(testCases, new HebrewCalendar());

        } catch (MissingResourceException ex) {
            warnln("Got Exception: " + ex.getMessage());
        }
    }

    /**
     * Problem reported by Armand Bendanan in which setting of the MONTH field in a Hebrew calendar
     * causes the time fields to go negative.
     */
    @Test
    public void TestTimeFields() {
        try {
            HebrewCalendar calendar = new HebrewCalendar(5761, 0, 11, 12, 28, 15);
            calendar.set(Calendar.YEAR, 5717);
            calendar.set(Calendar.MONTH, 2);
            calendar.set(Calendar.DAY_OF_MONTH, 23);
            if (calendar.get(Calendar.HOUR_OF_DAY) != 12) {
                errln("Fail: HebrewCalendar HOUR_OF_DAY = " + calendar.get(Calendar.HOUR_OF_DAY));
            }
        } catch (MissingResourceException ex) {
            warnln("Got Exception: " + ex.getMessage());
        }
    }

    /**
     * Problem reported by Armand Bendanan (armand.bendanan@free.fr) in which setting of the MONTH
     * field in a Hebrew calendar to ELUL on non leap years causes the date to be set on TISHRI next
     * year.
     */
    @Test
    public void TestElulMonth() {
        try {
            HebrewCalendar cal = new HebrewCalendar();
            // Leap years are:
            // 3 6 8 11 14 17 19 (and so on - 19-year cycle)
            for (int year = 1; year < 50; year++) {
                // I hope that year = 0 does not exists
                // because the test fails for it !
                cal.clear();

                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, ELUL);
                cal.set(Calendar.DAY_OF_MONTH, 1);

                int yact = cal.get(Calendar.YEAR);
                int mact = cal.get(Calendar.MONTH);

                if (year != yact || ELUL != mact) {
                    errln("Fail: " + ELUL + "/" + year + " -> " + mact + "/" + yact);
                }
            }
        } catch (MissingResourceException ex) {
            warnln("Got Exception: " + ex.getMessage());
        }
    }

    /**
     * Test of the behavior of the month field. This requires special handling in the Hebrew
     * calendar because of the pattern of leap years.
     */
    @Test
    public void TestMonthMovement() {
        try {
            HebrewCalendar cal = new HebrewCalendar();
            // Leap years are:
            // 3 6 8 11 14 17 19 (and so on - 19-year cycle)
            // We can't test complete() on some lines below because of ADAR_1 -- if
            // the calendar is set to ADAR_1 on a non-leap year, the result is undefined.
            int[] DATA = {
                // m1/y1 - month/year before (month is 1-based)
                // delta - amount to add to month field
                // m2/y2 - month/year after add(MONTH, delta)
                // m3/y3 - month/year after set(MONTH, m1+delta)
                // m1  y1 delta  m2  y2  m3  y3
                10, 2, +24, 9, 4, 9, 4,
                10, 2, +60, 8, 7, 8, 7,
                1, 2, +12, 1, 3, 13, 2, // *set != add; also see '*' below
                3, 18, -24, 4, 16, 4, 16,
                1, 6, -24, 1, 4, 1, 4,
                4, 3, +2, 6, 3, 6, 3, // Leap year - no skip 4,5,6,7,8
                8, 3, -2, 6, 3, 6, 3, // Leap year - no skip
                4, 2, +2, 7, 2, 7, 2, // Skip leap month 4,5,(6),7,8
                8, 2, -2, 5, 2, 7, 2, // *Skip leap month going backward
            };
            for (int i = 0; i < DATA.length; ) {
                int m = DATA[i++], y = DATA[i++];
                int monthDelta = DATA[i++];
                int m2 = DATA[i++], y2 = DATA[i++];
                int m3 = DATA[i++], y3 = DATA[i++];
                int mact, yact;

                cal.clear();
                cal.set(Calendar.YEAR, y);
                cal.set(Calendar.MONTH, m - 1);
                cal.add(Calendar.MONTH, monthDelta);
                yact = cal.get(Calendar.YEAR);
                mact = cal.get(Calendar.MONTH) + 1;
                if (y2 != yact || m2 != mact) {
                    errln(
                            "Fail: "
                                    + m
                                    + "/"
                                    + y
                                    + " -> add(MONTH, "
                                    + monthDelta
                                    + ") -> "
                                    + mact
                                    + "/"
                                    + yact
                                    + ", gregorian "
                                    + m2
                                    + "/"
                                    + y2);
                    cal.clear();
                    cal.set(Calendar.YEAR, y);
                    cal.set(Calendar.MONTH, m - 1);
                    logln("Start: " + m + "/" + y);
                    int delta = monthDelta > 0 ? 1 : -1;
                    for (int c = 0; c != monthDelta; c += delta) {
                        cal.add(Calendar.MONTH, delta);
                        logln(
                                "+ "
                                        + delta
                                        + " MONTH -> "
                                        + (cal.get(Calendar.MONTH) + 1)
                                        + "/"
                                        + cal.get(Calendar.YEAR));
                    }
                }

                cal.clear();
                cal.set(Calendar.YEAR, y);
                cal.set(Calendar.MONTH, m + monthDelta - 1);
                yact = cal.get(Calendar.YEAR);
                mact = cal.get(Calendar.MONTH) + 1;
                if (y3 != yact || m3 != mact) {
                    errln(
                            "Fail: "
                                    + (m + monthDelta)
                                    + "/"
                                    + y
                                    + " -> complete() -> "
                                    + mact
                                    + "/"
                                    + yact
                                    + ", gregorian "
                                    + m3
                                    + "/"
                                    + y3);
                }
            }
        } catch (MissingResourceException ex) {
            warnln("Got Exception: " + ex.getMessage());
        }
    }

    /** Test handling of ADAR_1. */
    /*
    @Test
    public void TestAdar1() {
        HebrewCalendar cal = new HebrewCalendar();
        cal.clear();
        cal.set(Calendar.YEAR, 1903); // leap
        cal.set(Calendar.MONTH, HebrewCalendar.ADAR_1);
        logln("1903(leap)/ADAR_1 => " +
              cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH)+1));

        cal.clear();
        cal.set(Calendar.YEAR, 1904); // non-leap
        cal.set(Calendar.MONTH, HebrewCalendar.ADAR_1);
        logln("1904(non-leap)/ADAR_1 => " +
              cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH)+1));
    }
    */

    /** With no fields set, the calendar should use default values. */
    @Test
    public void TestDefaultFieldValues() {
        try {
            HebrewCalendar cal = new HebrewCalendar();
            cal.clear();
            logln("cal.clear() -> " + cal.getTime());
        } catch (MissingResourceException ex) {
            warnln("could not load the locale data");
        }
    }

    /** Test limits of the Hebrew calendar */
    @Test
    public void TestLimits() {
        Calendar cal = Calendar.getInstance();
        cal.set(2007, Calendar.JANUARY, 1);
        HebrewCalendar hebrew = new HebrewCalendar();
        doLimitsTest(hebrew, null, cal.getTime());
        doTheoreticalLimitsTest(hebrew, true);
    }

    @Test
    public void TestCoverage() {
        try {
            {
                // new HebrewCalendar(TimeZone)
                HebrewCalendar cal = new HebrewCalendar(TimeZone.getDefault());
                if (cal == null) {
                    errln("could not create HebrewCalendar with TimeZone");
                }
            }

            {
                // new HebrewCalendar(ULocale)
                HebrewCalendar cal = new HebrewCalendar(ULocale.getDefault());
                if (cal == null) {
                    errln("could not create HebrewCalendar with ULocale");
                }
            }

            {
                // new HebrewCalendar(Locale)
                HebrewCalendar cal = new HebrewCalendar(Locale.getDefault());
                if (cal == null) {
                    errln("could not create HebrewCalendar with locale");
                }
            }

            {
                // new HebrewCalendar(Date)
                HebrewCalendar cal = new HebrewCalendar(new Date());
                if (cal == null) {
                    errln("could not create HebrewCalendar with date");
                }
            }

            {
                // data
                HebrewCalendar cal = new HebrewCalendar(2800, HebrewCalendar.SHEVAT, 1);
                Date time = cal.getTime();

                String[] calendarLocales = {"iw_IL"};

                String[] formatLocales = {"en", "fi", "fr", "hu", "iw", "nl"};
                for (int i = 0; i < calendarLocales.length; ++i) {
                    String calLocName = calendarLocales[i];
                    Locale calLocale = LocaleUtility.getLocaleFromName(calLocName);
                    cal = new HebrewCalendar(calLocale);

                    for (int j = 0; j < formatLocales.length; ++j) {
                        String locName = formatLocales[j];
                        Locale formatLocale = LocaleUtility.getLocaleFromName(locName);
                        DateFormat format =
                                DateFormat.getDateTimeInstance(
                                        cal, DateFormat.FULL, DateFormat.FULL, formatLocale);
                        logln(calLocName + "/" + locName + " --> " + format.format(time));
                    }
                }
            }
        } catch (MissingResourceException ex) {
            warnln("Could not load the locale data. " + ex.getMessage());
        }
    }

    @Test
    public void Test1624() {

        HebrewCalendar hc = new HebrewCalendar(5742, HebrewCalendar.AV, 22);
        DateFormat df = hc.getDateTimeFormat(DateFormat.FULL, DateFormat.FULL, Locale.getDefault());
        String dateString = df.format(hc.getTime());

        for (int year = 5600; year < 5800; year++) {
            boolean leapYear = HebrewCalendar.isLeapYear(year);
            for (int month = HebrewCalendar.TISHRI; month <= HebrewCalendar.ELUL; month++) {
                // skip the adar 1 month if year is not a leap year
                if (leapYear == false && month == HebrewCalendar.ADAR_1) {
                    continue;
                }
                int day = 15;
                hc = new HebrewCalendar(year, month, day);

                dateString = df.format(hc.getTime());
                int dayHC = hc.get(HebrewCalendar.DATE);
                int monthHC = hc.get(HebrewCalendar.MONTH);
                int yearHC = hc.get(HebrewCalendar.YEAR);

                String header = "year:" + year + " isleap:" + leapYear + " " + dateString;
                if (dayHC != day) {
                    errln(header + " ==> day:" + dayHC + " incorrect, should be:" + day);
                    break;
                }
                if (monthHC != month) {
                    errln(header + " ==> month:" + monthHC + " incorrect, should be:" + month);
                    break;
                }
                if (yearHC != year) {
                    errln(header + " ==> year:" + yearHC + " incorrecte, should be:" + year);
                    break;
                }
            }
        }
    }

    // Test case for Ticket#10313. HebrewCalendar requires
    // special handling for validating month value, because
    // month Adar I is only available in leap years.
    @Test
    public void TestMonthValidation() {
        HebrewCalendar cal = new HebrewCalendar();
        cal.setLenient(false);

        // 5776 is a leap year and has month Adar I
        cal.set(5776, ADAR_1, 1);
        try {
            /* Date d = */ cal.getTime();
        } catch (IllegalArgumentException e) {
            errln("Fail: 5776 Adar I 1 is a valid date.");
        }

        // 5777 is NOT a lear year and does not have month Adar I
        cal.set(5777, ADAR_1, 1);
        try {
            /* Date d = */ cal.getTime();
            errln("Fail: IllegalArgumentException should be thrown for input date 5777 Adar I 1.");
        } catch (IllegalArgumentException e) {
            logln("Info: IllegalArgumentException, because 5777 Adar I 1 is not a valid date.");
        }
    }

    @Test
    public void TestHanukkah() {
        HebrewCalendar hebrew = new HebrewCalendar();
        GregorianCalendar gregorian = new GregorianCalendar(hebrew.getTimeZone());
        hebrew.clear();
        // Based on the Hanukkah data in
        // https://en.wikipedia.org/wiki/Jewish_and_Israeli_holidays_2000%E2%80%932050
        Object[][] cases = {
            {5760, 1999, Calendar.DECEMBER, 4},
            {5761, 2000, Calendar.DECEMBER, 22},
            {5762, 2001, Calendar.DECEMBER, 10},
            {5763, 2002, Calendar.NOVEMBER, 30},
            {5764, 2003, Calendar.DECEMBER, 20},
            {5765, 2004, Calendar.DECEMBER, 8},
            {5766, 2005, Calendar.DECEMBER, 26},
            {5767, 2006, Calendar.DECEMBER, 16},
            {5768, 2007, Calendar.DECEMBER, 5},
            {5769, 2008, Calendar.DECEMBER, 22},
            {5770, 2009, Calendar.DECEMBER, 12},
            {5771, 2010, Calendar.DECEMBER, 2},
            {5772, 2011, Calendar.DECEMBER, 21},
            {5773, 2012, Calendar.DECEMBER, 9},
            {5774, 2013, Calendar.NOVEMBER, 28},
            {5775, 2014, Calendar.DECEMBER, 17},
            {5776, 2015, Calendar.DECEMBER, 7},
            {5777, 2016, Calendar.DECEMBER, 25},
            {5778, 2017, Calendar.DECEMBER, 13},
            {5779, 2018, Calendar.DECEMBER, 3},
            {5780, 2019, Calendar.DECEMBER, 23},
            {5781, 2020, Calendar.DECEMBER, 11},
            {5782, 2021, Calendar.NOVEMBER, 29},
            {5783, 2022, Calendar.DECEMBER, 19},
            {5784, 2023, Calendar.DECEMBER, 8},
            {5785, 2024, Calendar.DECEMBER, 26},
            {5786, 2025, Calendar.DECEMBER, 15},
            {5787, 2026, Calendar.DECEMBER, 5},
            {5788, 2027, Calendar.DECEMBER, 25},
            {5789, 2028, Calendar.DECEMBER, 13},
            {5790, 2029, Calendar.DECEMBER, 2},
            {5791, 2030, Calendar.DECEMBER, 21},
            {5792, 2031, Calendar.DECEMBER, 10},
            {5793, 2032, Calendar.NOVEMBER, 28},
            {5794, 2033, Calendar.DECEMBER, 17},
            {5795, 2034, Calendar.DECEMBER, 7},
            {5796, 2035, Calendar.DECEMBER, 26},
            {5797, 2036, Calendar.DECEMBER, 14},
            {5798, 2037, Calendar.DECEMBER, 3},
            {5799, 2038, Calendar.DECEMBER, 22},
            {5800, 2039, Calendar.DECEMBER, 12},
            {5801, 2040, Calendar.NOVEMBER, 30},
            {5802, 2041, Calendar.DECEMBER, 18},
            {5803, 2042, Calendar.DECEMBER, 8},
            {5804, 2043, Calendar.DECEMBER, 27},
            {5805, 2044, Calendar.DECEMBER, 15},
            {5806, 2045, Calendar.DECEMBER, 4},
            {5807, 2046, Calendar.DECEMBER, 24},
            {5808, 2047, Calendar.DECEMBER, 13},
            {5809, 2048, Calendar.NOVEMBER, 30},
            {5810, 2049, Calendar.DECEMBER, 20},
            {5811, 2050, Calendar.DECEMBER, 10},
        };
        for (Object[] cas : cases) {
            int hebrewYear = (Integer) cas[0];
            int gregorianYear = (Integer) cas[1];
            int gregorianMonth = (Integer) cas[2];
            int gregorianDate = (Integer) cas[3];
            // Test from Hebrew Calendar to Gregorian Calendar.
            // Rosh Hashanah/Hanukkah is the 25th day of Kislev
            hebrew.set(Calendar.YEAR, hebrewYear);
            hebrew.set(Calendar.MONTH, KISLEV);
            hebrew.set(Calendar.DATE, 25);
            gregorian.setTime(hebrew.getTime());
            int y = gregorian.get(Calendar.YEAR);
            int m = gregorian.get(Calendar.MONTH);
            int d = gregorian.get(Calendar.DATE);
            if (y != gregorianYear || m != gregorianMonth || d != gregorianDate) {
                errln(
                        "Fail: Hebrew year "
                                + hebrewYear
                                + " starts at Gregorian Date("
                                + y
                                + "/"
                                + (m + 1)
                                + "/"
                                + d
                                + ") should be Date("
                                + gregorianYear
                                + "/"
                                + (gregorianMonth + 1)
                                + "/"
                                + gregorianDate
                                + ")");
            }
            // Test from Gregorian Calendar to Hebrew Calendar.
            gregorian.clear();
            gregorian.set(Calendar.YEAR, gregorianYear);
            gregorian.set(Calendar.MONTH, gregorianMonth);
            gregorian.set(Calendar.DATE, gregorianDate);
            hebrew.setTime(gregorian.getTime());
            y = hebrew.get(Calendar.YEAR);
            m = hebrew.get(Calendar.MONTH);
            d = hebrew.get(Calendar.DATE);
            if (y != hebrewYear || m != KISLEV || d != 25) {
                errln(
                        "Fail: Gregorian Date("
                                + gregorianYear
                                + "/"
                                + (gregorianMonth + 1)
                                + "/"
                                + gregorianDate
                                + ") should get "
                                + "Hebrew Date("
                                + hebrewYear
                                + "/"
                                + (KISLEV + 1)
                                + "/25) but got Date("
                                + y
                                + "/"
                                + (m + 1)
                                + "/"
                                + d
                                + ")");
            }
        }
    }
}
