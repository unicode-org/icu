// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2000-2016, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @test 1.32 99/11/14
 * @bug 4031502 4035301 4040996 4051765 4059654 4061476 4070502 4071197 4071385 4073929 4083167
 *     4086724 4092362 4095407 4096231 4096539 4100311 4103271 4106136 4108764 4114578 4118384
 *     4125881 4125892 4136399 4141665 4142933 4145158 4145983 4147269 4149677 4162587 4165343
 *     4166109 4167060 4173516 4174361 4177484 4197699 4209071 4288792
 */
@RunWith(JUnit4.class)
public class CalendarRegressionTest extends CoreTestFmwk {
    static final String[] FIELD_NAME = {
        "ERA",
        "YEAR",
        "MONTH",
        "WEEK_OF_YEAR",
        "WEEK_OF_MONTH",
        "DAY_OF_MONTH",
        "DAY_OF_YEAR",
        "DAY_OF_WEEK",
        "DAY_OF_WEEK_IN_MONTH",
        "AM_PM",
        "HOUR",
        "HOUR_OF_DAY",
        "MINUTE",
        "SECOND",
        "MILLISECOND",
        "ZONE_OFFSET",
        "DST_OFFSET",
        "YEAR_WOY",
        "DOW_LOCAL",
        "EXTENDED_YEAR",
        "JULIAN_DAY",
        "MILLISECONDS_IN_DAY",
        "IS_LEAP_YEAR",
        "ORDINAL_MONTH"
    };

    /*
    Synopsis: java.sql.Timestamp constructor works wrong on Windows 95

    ==== Here is the test ====
    public static void main (String args[]) {
      java.sql.Timestamp t= new java.sql.Timestamp(0,15,5,5,8,13,123456700);
      logln("expected=1901-04-05 05:08:13.1234567");
      logln(" result="+t);
    }

    ==== Here is the output of the test on Solaris or NT ====
    expected=1901-04-05 05:08:13.1234567
    result=1901-04-05 05:08:13.1234567

    ==== Here is the output of the test on Windows95 ====
    expected=1901-04-05 05:08:13.1234567
    result=1901-04-05 06:08:13.1234567
    */

    @Test
    public void Test4031502() {
        try {
            // This bug actually occurs on Windows NT as well, and doesn't
            // require the host zone to be set; it can be set in Java.
            String[] ids = TimeZone.getAvailableIDs();
            boolean bad = false;
            for (int i = 0; i < ids.length; ++i) {
                TimeZone zone = TimeZone.getTimeZone(ids[i]);
                GregorianCalendar cal = new GregorianCalendar(zone);
                cal.clear();
                cal.set(1900, 15, 5, 5, 8, 13);
                if (cal.get(Calendar.HOUR) != 5) {
                    logln(
                            "Fail: "
                                    + zone.getID()
                                    + " "
                                    + zone.useDaylightTime()
                                    + "; DST_OFFSET = "
                                    + cal.get(Calendar.DST_OFFSET) / (60 * 60 * 1000.0)
                                    + "; ZONE_OFFSET = "
                                    + cal.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000.0)
                                    + "; getRawOffset() = "
                                    + zone.getRawOffset() / (60 * 60 * 1000.0)
                                    + "; HOUR = "
                                    + cal.get(Calendar.HOUR));
                    cal.clear();
                    cal.set(1900, 15, 5, 5, 8, 13);
                    if (cal.get(Calendar.HOUR) != 5) {
                        logln(
                                "Fail: "
                                        + zone.getID()
                                        + " "
                                        + zone.useDaylightTime()
                                        + "; DST_OFFSET = "
                                        + cal.get(Calendar.DST_OFFSET) / (60 * 60 * 1000.0)
                                        + "; ZONE_OFFSET = "
                                        + cal.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000.0)
                                        + "; getRawOffset() = "
                                        + zone.getRawOffset() / (60 * 60 * 1000.0)
                                        + "; HOUR = "
                                        + cal.get(Calendar.HOUR));
                        cal.clear();
                        cal.set(1900, 15, 5, 5, 8, 13);
                        logln("ms = " + cal.getTime() + " (" + cal.getTime().getTime() + ")");
                        cal.get(Calendar.HOUR);
                        java.util.GregorianCalendar cal2 =
                                new java.util.GregorianCalendar(
                                        java.util.TimeZone.getTimeZone(ids[i]));
                        cal2.clear();
                        cal2.set(1900, 15, 5, 5, 8, 13);
                        cal2.get(Calendar.HOUR);
                        logln(
                                "java.util.GC: "
                                        + zone.getID()
                                        + " "
                                        + zone.useDaylightTime()
                                        + "; DST_OFFSET = "
                                        + cal2.get(Calendar.DST_OFFSET) / (60 * 60 * 1000.0)
                                        + "; ZONE_OFFSET = "
                                        + cal2.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000.0)
                                        + "; getRawOffset() = "
                                        + zone.getRawOffset() / (60 * 60 * 1000.0)
                                        + "; HOUR = "
                                        + cal.get(Calendar.HOUR));
                        logln("ms = " + cal2.getTime() + " (" + cal2.getTime().getTime() + ")");
                        bad = true;
                    } else if (false) { // Change to true to debug
                        logln(
                                "OK: "
                                        + zone.getID()
                                        + " "
                                        + zone.useDaylightTime()
                                        + " "
                                        + cal.get(Calendar.DST_OFFSET) / (60 * 60 * 1000)
                                        + " "
                                        + zone.getRawOffset() / (60 * 60 * 1000)
                                        + ": HOUR = "
                                        + cal.get(Calendar.HOUR));
                    }
                }
                if (bad) errln("TimeZone problems with GC");
            }
        } catch (MissingResourceException e) {
            warnln("Could not load data. " + e.getMessage());
        }
    }

    @Test
    @SuppressWarnings("SelfEquals")
    public void Test4035301() {

        try {
            GregorianCalendar c = new GregorianCalendar(98, 8, 7);
            GregorianCalendar d = new GregorianCalendar(98, 8, 7);
            if (c.after(d)
                    || c.after(c)
                    || c.before(d)
                    || c.before(c)
                    || !c.equals(c)
                    || !c.equals(d)) errln("Fail");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            warnln("Could not load data. " + e.getMessage());
        }
    }

    @Test
    public void Test4040996() {
        try {
            String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
            SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
            pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
            pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
            Calendar calendar = new GregorianCalendar(pdt);

            calendar.set(Calendar.MONTH, 3);
            calendar.set(Calendar.DAY_OF_MONTH, 18);
            calendar.set(Calendar.SECOND, 30);

            logln("MONTH: " + calendar.get(Calendar.MONTH));
            logln("DAY_OF_MONTH: " + calendar.get(Calendar.DAY_OF_MONTH));
            logln("MINUTE: " + calendar.get(Calendar.MINUTE));
            logln("SECOND: " + calendar.get(Calendar.SECOND));

            calendar.add(Calendar.SECOND, 6);
            // This will print out todays date for MONTH and DAY_OF_MONTH
            // instead of the date it was set to.
            // This happens when adding MILLISECOND or MINUTE also
            logln("MONTH: " + calendar.get(Calendar.MONTH));
            logln("DAY_OF_MONTH: " + calendar.get(Calendar.DAY_OF_MONTH));
            logln("MINUTE: " + calendar.get(Calendar.MINUTE));
            logln("SECOND: " + calendar.get(Calendar.SECOND));
            if (calendar.get(Calendar.MONTH) != 3
                    || calendar.get(Calendar.DAY_OF_MONTH) != 18
                    || calendar.get(Calendar.SECOND) != 36) errln("Fail: Calendar.add misbehaves");
        } catch (Exception e) {
            warnln("Could not load data. " + e.getMessage());
        }
    }

    @Test
    public void Test4051765() {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setLenient(false);
            cal.set(Calendar.DAY_OF_WEEK, 0);
            try {
                cal.getTime();
                errln("Fail: DAY_OF_WEEK 0 should be disallowed");
            } catch (IllegalArgumentException e) {
                return;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            warnln("Could not load data. " + e.getMessage());
        }
    }

    /*
     * User error - no bug here public void Test4059524() { // Create calendar
     * for April 10, 1997 GregorianCalendar calendar = new GregorianCalendar(); //
     * print out a bunch of interesting things logln("ERA: " +
     * calendar.get(calendar.ERA)); logln("YEAR: " +
     * calendar.get(calendar.YEAR)); logln("MONTH: " +
     * calendar.get(calendar.MONTH)); logln("WEEK_OF_YEAR: " +
     * calendar.get(calendar.WEEK_OF_YEAR)); logln("WEEK_OF_MONTH: " +
     * calendar.get(calendar.WEEK_OF_MONTH)); logln("DATE: " +
     * calendar.get(calendar.DATE)); logln("DAY_OF_MONTH: " +
     * calendar.get(calendar.DAY_OF_MONTH)); logln("DAY_OF_YEAR: " +
     * calendar.get(calendar.DAY_OF_YEAR)); logln("DAY_OF_WEEK: " +
     * calendar.get(calendar.DAY_OF_WEEK)); logln("DAY_OF_WEEK_IN_MONTH: " +
     * calendar.get(calendar.DAY_OF_WEEK_IN_MONTH)); logln("AM_PM: " +
     * calendar.get(calendar.AM_PM)); logln("HOUR: " +
     * calendar.get(calendar.HOUR)); logln("HOUR_OF_DAY: " +
     * calendar.get(calendar.HOUR_OF_DAY)); logln("MINUTE: " +
     * calendar.get(calendar.MINUTE)); logln("SECOND: " +
     * calendar.get(calendar.SECOND)); logln("MILLISECOND: " +
     * calendar.get(calendar.MILLISECOND)); logln("ZONE_OFFSET: " +
     * (calendar.get(calendar.ZONE_OFFSET)/(60*60*1000))); logln("DST_OFFSET: " +
     * (calendar.get(calendar.DST_OFFSET)/(60*60*1000))); calendar = new
     * GregorianCalendar(1997,3,10); calendar.getTime(); logln("April 10,
     * 1997"); logln("ERA: " + calendar.get(calendar.ERA)); logln("YEAR: " +
     * calendar.get(calendar.YEAR)); logln("MONTH: " +
     * calendar.get(calendar.MONTH)); logln("WEEK_OF_YEAR: " +
     * calendar.get(calendar.WEEK_OF_YEAR)); logln("WEEK_OF_MONTH: " +
     * calendar.get(calendar.WEEK_OF_MONTH)); logln("DATE: " +
     * calendar.get(calendar.DATE)); logln("DAY_OF_MONTH: " +
     * calendar.get(calendar.DAY_OF_MONTH)); logln("DAY_OF_YEAR: " +
     * calendar.get(calendar.DAY_OF_YEAR)); logln("DAY_OF_WEEK: " +
     * calendar.get(calendar.DAY_OF_WEEK)); logln("DAY_OF_WEEK_IN_MONTH: " +
     * calendar.get(calendar.DAY_OF_WEEK_IN_MONTH)); logln("AM_PM: " +
     * calendar.get(calendar.AM_PM)); logln("HOUR: " +
     * calendar.get(calendar.HOUR)); logln("HOUR_OF_DAY: " +
     * calendar.get(calendar.HOUR_OF_DAY)); logln("MINUTE: " +
     * calendar.get(calendar.MINUTE)); logln("SECOND: " +
     * calendar.get(calendar.SECOND)); logln("MILLISECOND: " +
     * calendar.get(calendar.MILLISECOND)); logln("ZONE_OFFSET: " +
     * (calendar.get(calendar.ZONE_OFFSET)/(60*60*1000))); // in hours
     * logln("DST_OFFSET: " + (calendar.get(calendar.DST_OFFSET)/(60*60*1000))); //
     * in hours }
     */

    @Test
    public void Test4059654() {
        //     try {
        // work around bug for jdk1.4 on solaris 2.6, which uses funky
        // timezone names
        // jdk1.4.1 will drop support for 2.6 so we should be ok when it
        // comes out
        java.util.TimeZone javazone = java.util.TimeZone.getTimeZone("GMT");
        TimeZone icuzone = TimeZone.getTimeZone("GMT");

        GregorianCalendar gc = new GregorianCalendar(icuzone);

        gc.set(1997, 3, 1, 15, 16, 17); // April 1, 1997

        gc.set(Calendar.HOUR, 0);
        gc.set(Calendar.AM_PM, Calendar.AM);
        gc.set(Calendar.MINUTE, 0);
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);

        Date cd = gc.getTime();
        java.util.Calendar cal = java.util.Calendar.getInstance(javazone);
        cal.clear();
        cal.set(1997, 3, 1, 0, 0, 0);
        Date exp = cal.getTime();
        if (!cd.equals(exp)) errln("Fail: Calendar.set broken. Got " + cd + " Want " + exp);
        //     } catch (RuntimeException e) {
        // TODO Auto-generated catch block
        //         e.printStackTrace();
        //      }
    }

    @Test
    public void Test4061476() {
        SimpleDateFormat fmt = new SimpleDateFormat("ddMMMyy", Locale.UK);
        Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.UK);
        fmt.setCalendar(cal);
        try {
            Date date = fmt.parse("29MAY97");
            cal.setTime(date);
        } catch (Exception e) {
            System.out.print("");
        }
        cal.set(Calendar.HOUR_OF_DAY, 13);
        logln("Hour: " + cal.get(Calendar.HOUR_OF_DAY));
        cal.add(Calendar.HOUR_OF_DAY, 6);
        logln("Hour: " + cal.get(Calendar.HOUR_OF_DAY));
        if (cal.get(Calendar.HOUR_OF_DAY) != 19)
            errln("Fail: Want 19 Got " + cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void Test4070502() {
        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1998, 0, 30);
        Date d = getAssociatedDate(tempcal.getTime());
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            errln("Fail: Want weekday Got " + d);
    }

    /**
     * Get the associated date starting from a specified date NOTE: the unnecessary "getTime()'s"
     * below are a work-around for a bug in jdk 1.1.3 (and probably earlier versions also)
     *
     * <p>
     *
     * @param d The date to start from
     */
    public static Date getAssociatedDate(Date d) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(d);
        // cal.add(field, amount); //<-- PROBLEM SEEN WITH field = DATE,MONTH
        // cal.getTime(); // <--- REMOVE THIS TO SEE BUG
        while (true) {
            int wd = cal.get(Calendar.DAY_OF_WEEK);
            if (wd == Calendar.SATURDAY || wd == Calendar.SUNDAY) {
                cal.add(Calendar.DATE, 1);
                // cal.getTime();
            } else break;
        }
        return cal.getTime();
    }

    @Test
    public void Test4071197() {
        dowTest(false);
        dowTest(true);
    }

    void dowTest(boolean lenient) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(1997, Calendar.AUGUST, 12); // Wednesday
        // cal.getTime(); // Force update
        cal.setLenient(lenient);
        cal.set(1996, Calendar.DECEMBER, 1); // Set the date to be December 1,
        // 1996
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int min = cal.getMinimum(Calendar.DAY_OF_WEEK);
        int max = cal.getMaximum(Calendar.DAY_OF_WEEK);
        logln(cal.getTime().toString());
        if (min != Calendar.SUNDAY || max != Calendar.SATURDAY) errln("FAIL: Min/max bad");
        if (dow < min || dow > max) errln("FAIL: Day of week " + dow + " out of range");
        if (dow != Calendar.SUNDAY) errln("FAIL: Day of week should be SUNDAY Got " + dow);
    }

    @Test
    public void Test4071385() {
        // work around bug for jdk1.4 on solaris 2.6, which uses funky timezone
        // names
        // jdk1.4.1 will drop support for 2.6 so we should be ok when it comes out
        java.util.TimeZone javazone = java.util.TimeZone.getTimeZone("GMT");
        TimeZone icuzone = TimeZone.getTimeZone("GMT");

        Calendar cal = Calendar.getInstance(icuzone);
        java.util.Calendar tempcal = java.util.Calendar.getInstance(javazone);
        tempcal.clear();
        tempcal.set(1998, Calendar.JUNE, 24);
        cal.setTime(tempcal.getTime());
        cal.set(Calendar.MONTH, Calendar.NOVEMBER); // change a field
        logln(cal.getTime().toString());
        tempcal.set(1998, Calendar.NOVEMBER, 24);
        if (!cal.getTime().equals(tempcal.getTime())) errln("Fail");
    }

    @Test
    public void Test4073929() {
        GregorianCalendar foo1 = new GregorianCalendar(1997, 8, 27);
        foo1.add(Calendar.DAY_OF_MONTH, +1);
        int testyear = foo1.get(Calendar.YEAR);
        int testmonth = foo1.get(Calendar.MONTH);
        int testday = foo1.get(Calendar.DAY_OF_MONTH);
        if (testyear != 1997 || testmonth != 8 || testday != 28)
            errln("Fail: Calendar not initialized");
    }

    @Test
    public void Test4083167() {
        TimeZone saveZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Date firstDate = new Date();
            Calendar cal = new GregorianCalendar();
            cal.setTime(firstDate);
            long firstMillisInDay =
                    cal.get(Calendar.HOUR_OF_DAY) * 3600000L
                            + cal.get(Calendar.MINUTE) * 60000L
                            + cal.get(Calendar.SECOND) * 1000L
                            + cal.get(Calendar.MILLISECOND);

            logln("Current time: " + firstDate.toString());

            for (int validity = 0; validity < 30; validity++) {
                Date lastDate =
                        new Date(firstDate.getTime() + (long) validity * 1000 * 24 * 60 * 60);
                cal.setTime(lastDate);
                long millisInDay =
                        cal.get(Calendar.HOUR_OF_DAY) * 3600000L
                                + cal.get(Calendar.MINUTE) * 60000L
                                + cal.get(Calendar.SECOND) * 1000L
                                + cal.get(Calendar.MILLISECOND);
                if (firstMillisInDay != millisInDay) errln("Day has shifted " + lastDate);
            }
        } finally {
            TimeZone.setDefault(saveZone);
        }
    }

    @Test
    public void Test4086724() {
        SimpleDateFormat date;
        TimeZone saveZone = TimeZone.getDefault();
        Locale saveLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.UK);
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            date = new SimpleDateFormat("dd MMM yyy (zzzz) 'is in week' ww");
            Calendar cal = Calendar.getInstance();
            cal.set(1997, Calendar.SEPTEMBER, 30);
            Date now = cal.getTime();
            logln(date.format(now));
            cal.set(1997, Calendar.JANUARY, 1);
            now = cal.getTime();
            logln(date.format(now));
            cal.set(1997, Calendar.JANUARY, 8);
            now = cal.getTime();
            logln(date.format(now));
            cal.set(1996, Calendar.DECEMBER, 31);
            now = cal.getTime();
            logln(date.format(now));
        } finally {
            Locale.setDefault(saveLocale);
            TimeZone.setDefault(saveZone);
        }
        logln("*** THE RESULTS OF THIS TEST MUST BE VERIFIED MANUALLY ***");
    }

    @Test
    public void Test4092362() {
        GregorianCalendar cal1 = new GregorianCalendar(1997, 10, 11, 10, 20, 40);
        /*
         * cal1.set( Calendar.YEAR, 1997 ); cal1.set( Calendar.MONTH, 10 );
         * cal1.set( Calendar.DATE, 11 ); cal1.set( Calendar.HOUR, 10 );
         * cal1.set( Calendar.MINUTE, 20 ); cal1.set( Calendar.SECOND, 40 );
         */

        logln(" Cal1 = " + cal1.getTime().getTime());
        logln(" Cal1 time in ms = " + cal1.get(Calendar.MILLISECOND));
        for (int k = 0; k < 100; k++) {
            System.out.print("");
        }

        GregorianCalendar cal2 = new GregorianCalendar(1997, 10, 11, 10, 20, 40);
        /*
         * cal2.set( Calendar.YEAR, 1997 ); cal2.set( Calendar.MONTH, 10 );
         * cal2.set( Calendar.DATE, 11 ); cal2.set( Calendar.HOUR, 10 );
         * cal2.set( Calendar.MINUTE, 20 ); cal2.set( Calendar.SECOND, 40 );
         */

        logln(" Cal2 = " + cal2.getTime().getTime());
        logln(" Cal2 time in ms = " + cal2.get(Calendar.MILLISECOND));
        if (!cal1.equals(cal2)) errln("Fail: Milliseconds randomized");
    }

    @Test
    public void Test4095407() {
        GregorianCalendar a = new GregorianCalendar(1997, Calendar.NOVEMBER, 13);
        int dow = a.get(Calendar.DAY_OF_WEEK);
        if (dow != Calendar.THURSDAY) errln("Fail: Want THURSDAY Got " + dow);
    }

    @Test
    public void Test4096231() {
        TimeZone GMT = TimeZone.getTimeZone("GMT");
        TimeZone PST = TimeZone.getTimeZone("PST");
        int sec = 0, min = 0, hr = 0, day = 1, month = 10, year = 1997;

        Calendar cal1 = new GregorianCalendar(PST);
        cal1.setTime(new Date(880698639000L));
        int p;
        logln("PST 1 is: " + (p = cal1.get(Calendar.HOUR_OF_DAY)));
        cal1.setTimeZone(GMT);
        // Issue 1: Changing the timezone doesn't change the
        //          represented time.
        int h1, h2;
        logln("GMT 1 is: " + (h1 = cal1.get(Calendar.HOUR_OF_DAY)));
        cal1.setTime(new Date(880698639000L));
        logln("GMT 2 is: " + (h2 = cal1.get(Calendar.HOUR_OF_DAY)));
        // Note: This test had a bug in it. It wanted h1!=h2, when
        // what was meant was h1!=p. Fixed this concurrent with fix
        // to 4177484.
        if (p == h1 || h1 != h2) errln("Fail: Hour same in different zones");

        Calendar cal2 = new GregorianCalendar(GMT);
        Calendar cal3 = new GregorianCalendar(PST);
        cal2.set(Calendar.MILLISECOND, 0);
        cal3.set(Calendar.MILLISECOND, 0);

        cal2.set(
                cal1.get(Calendar.YEAR),
                cal1.get(Calendar.MONTH),
                cal1.get(Calendar.DAY_OF_MONTH),
                cal1.get(Calendar.HOUR_OF_DAY),
                cal1.get(Calendar.MINUTE),
                cal1.get(Calendar.SECOND));

        long t1, t2, t3, t4;
        logln("RGMT 1 is: " + (t1 = cal2.getTime().getTime()));
        cal3.set(year, month, day, hr, min, sec);
        logln("RPST 1 is: " + (t2 = cal3.getTime().getTime()));
        cal3.setTimeZone(GMT);
        logln("RGMT 2 is: " + (t3 = cal3.getTime().getTime()));
        cal3.set(
                cal1.get(Calendar.YEAR),
                cal1.get(Calendar.MONTH),
                cal1.get(Calendar.DAY_OF_MONTH),
                cal1.get(Calendar.HOUR_OF_DAY),
                cal1.get(Calendar.MINUTE),
                cal1.get(Calendar.SECOND));
        // Issue 2: Calendar continues to use the timezone in its
        //          constructor for set() conversions, regardless
        //          of calls to setTimeZone()
        logln("RGMT 3 is: " + (t4 = cal3.getTime().getTime()));
        if (t1 == t2 || t1 != t4 || t2 != t3) errln("Fail: Calendar zone behavior faulty");
    }

    @Test
    public void Test4096539() {
        int[] y = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        for (int x = 0; x < 12; x++) {
            GregorianCalendar gc = new GregorianCalendar(1997, x, y[x]);
            int m1, m2;
            log(
                    (m1 = gc.get(Calendar.MONTH) + 1)
                            + "/"
                            + gc.get(Calendar.DATE)
                            + "/"
                            + gc.get(Calendar.YEAR)
                            + " + 1mo = ");

            gc.add(Calendar.MONTH, 1);
            logln(
                    (m2 = gc.get(Calendar.MONTH) + 1)
                            + "/"
                            + gc.get(Calendar.DATE)
                            + "/"
                            + gc.get(Calendar.YEAR));
            int m = (m1 % 12) + 1;
            if (m2 != m) errln("Fail: Want " + m + " Got " + m2);
        }
    }

    @Test
    public void Test4100311() {
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        cal.set(Calendar.YEAR, 1997);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        Date d = cal.getTime(); // Should be Jan 1
        logln(d.toString());
        if (cal.get(Calendar.DAY_OF_YEAR) != 1) errln("Fail: DAY_OF_YEAR not set");
    }

    @Test
    public void Test4103271() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        int numYears = 40, startYear = 1997, numDays = 15;
        String output, testDesc;
        GregorianCalendar testCal = (GregorianCalendar) Calendar.getInstance();
        testCal.clear();
        sdf.setCalendar(testCal);
        sdf.applyPattern("d MMM yyyy");
        boolean fail = false;
        for (int firstDay = 1; firstDay <= 2; firstDay++) {
            for (int minDays = 1; minDays <= 7; minDays++) {
                testCal.setMinimalDaysInFirstWeek(minDays);
                testCal.setFirstDayOfWeek(firstDay);
                testDesc = ("Test" + String.valueOf(firstDay) + String.valueOf(minDays));
                logln(
                        testDesc
                                + " => 1st day of week="
                                + String.valueOf(firstDay)
                                + ", minimum days in first week="
                                + String.valueOf(minDays));
                for (int j = startYear; j <= startYear + numYears; j++) {
                    testCal.set(j, 11, 25);
                    for (int i = 0; i < numDays; i++) {
                        testCal.add(Calendar.DATE, 1);
                        String calWOY;
                        int actWOY = testCal.get(Calendar.WEEK_OF_YEAR);
                        if (actWOY < 1 || actWOY > 53) {
                            Date d = testCal.getTime();
                            calWOY = String.valueOf(actWOY);
                            output = testDesc + " - " + sdf.format(d) + "\t";
                            output = output + "\t" + calWOY;
                            logln(output);
                            fail = true;
                        }
                    }
                }
            }
        }

        int[] DATA = {
            3, 52, 52, 52, 52, 52, 52, 52, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2,
            4, 52, 52, 52, 52, 52, 52, 52, 53, 53, 53, 53, 53, 53, 53, 1, 1, 1, 1, 1, 1, 1,
        };
        testCal.setFirstDayOfWeek(Calendar.SUNDAY);
        for (int j = 0; j < DATA.length; j += 22) {
            logln("Minimal days in first week = " + DATA[j] + "  Week starts on Sunday");
            testCal.setMinimalDaysInFirstWeek(DATA[j]);
            testCal.set(1997, Calendar.DECEMBER, 21);
            for (int i = 0; i < 21; ++i) {
                int woy = testCal.get(Calendar.WEEK_OF_YEAR);
                log(testCal.getTime() + " " + woy);
                if (woy != DATA[j + 1 + i]) {
                    log(" ERROR");
                    fail = true;
                }
                // logln();

                // Now compute the time from the fields, and make sure we
                // get the same answer back. This is a round-trip test.
                Date save = testCal.getTime();
                testCal.clear();
                testCal.set(Calendar.YEAR, DATA[j + 1 + i] < 25 ? 1998 : 1997);
                testCal.set(Calendar.WEEK_OF_YEAR, DATA[j + 1 + i]);
                testCal.set(Calendar.DAY_OF_WEEK, (i % 7) + Calendar.SUNDAY);
                if (!testCal.getTime().equals(save)) {
                    logln("  Parse failed: " + testCal.getTime());
                    fail = true;
                }

                testCal.setTime(save);
                testCal.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        Date d[] = new Date[8];
        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1997, Calendar.DECEMBER, 28);
        d[0] = tempcal.getTime();
        tempcal.set(1998, Calendar.JANUARY, 10);
        d[1] = tempcal.getTime();
        tempcal.set(1998, Calendar.DECEMBER, 31);
        d[2] = tempcal.getTime();
        tempcal.set(1999, Calendar.JANUARY, 1);
        d[3] = tempcal.getTime();
        // Test field disambiguation with a few special hard-coded cases.
        // This shouldn't fail if the above cases aren't failing.
        Object[] DISAM = {
            1998, 1, Calendar.SUNDAY, d[0],
            1998, 2, Calendar.SATURDAY, d[1],
            1998, 53, Calendar.THURSDAY, d[2],
            1998, 53, Calendar.FRIDAY, d[3],
        };
        testCal.setMinimalDaysInFirstWeek(3);
        testCal.setFirstDayOfWeek(Calendar.SUNDAY);
        for (int i = 0; i < DISAM.length; i += 4) {
            int y = ((Integer) DISAM[i]).intValue();
            int woy = ((Integer) DISAM[i + 1]).intValue();
            int dow = ((Integer) DISAM[i + 2]).intValue();
            Date exp = (Date) DISAM[i + 3];
            testCal.clear();
            testCal.set(Calendar.YEAR, y);
            testCal.set(Calendar.WEEK_OF_YEAR, woy);
            testCal.set(Calendar.DAY_OF_WEEK, dow);
            log(y + "-W" + woy + "-DOW" + dow + " expect:" + exp + " got:" + testCal.getTime());
            if (!testCal.getTime().equals(exp)) {
                log("  FAIL");
                fail = true;
            }
            // logln();
        }

        // Now try adding and rolling
        Object ADD = new Object();
        Object ROLL = new Object();
        tempcal.set(1998, Calendar.DECEMBER, 25);
        d[0] = tempcal.getTime();
        tempcal.set(1999, Calendar.JANUARY, 1);
        d[1] = tempcal.getTime();
        tempcal.set(1997, Calendar.DECEMBER, 28);
        d[2] = tempcal.getTime();
        tempcal.set(1998, Calendar.JANUARY, 4);
        d[3] = tempcal.getTime();
        tempcal.set(1998, Calendar.DECEMBER, 27);
        d[4] = tempcal.getTime();
        tempcal.set(1997, Calendar.DECEMBER, 28);
        d[5] = tempcal.getTime();
        tempcal.set(1999, Calendar.JANUARY, 2);
        d[6] = tempcal.getTime();
        tempcal.set(1998, Calendar.JANUARY, 3);
        d[7] = tempcal.getTime();

        Object[] ADDROLL = {
            ADD, 1, d[0], d[1],
            ADD, 1, d[2], d[3],
            ROLL, 1, d[4], d[5],
            ROLL, 1, d[6], d[7],
        };
        testCal.setMinimalDaysInFirstWeek(3);
        testCal.setFirstDayOfWeek(Calendar.SUNDAY);
        for (int i = 0; i < ADDROLL.length; i += 4) {
            int amount = ((Integer) ADDROLL[i + 1]).intValue();
            Date before = (Date) ADDROLL[i + 2];
            Date after = (Date) ADDROLL[i + 3];

            testCal.setTime(before);
            if (ADDROLL[i] == ADD) testCal.add(Calendar.WEEK_OF_YEAR, amount);
            else testCal.roll(Calendar.WEEK_OF_YEAR, amount);
            log(
                    (ADDROLL[i] == ADD ? "add(WOY," : "roll(WOY,")
                            + amount
                            + ") "
                            + before
                            + " => "
                            + testCal.getTime());
            if (!after.equals(testCal.getTime())) {
                logln("  exp:" + after + "  FAIL");
                fail = true;
            } else logln(" ok");

            testCal.setTime(after);
            if (ADDROLL[i] == ADD) testCal.add(Calendar.WEEK_OF_YEAR, -amount);
            else testCal.roll(Calendar.WEEK_OF_YEAR, -amount);
            log(
                    (ADDROLL[i] == ADD ? "add(WOY," : "roll(WOY,")
                            + (-amount)
                            + ") "
                            + after
                            + " => "
                            + testCal.getTime());
            if (!before.equals(testCal.getTime())) {
                logln("  exp:" + before + "  FAIL");
                fail = true;
            } else logln(" ok");
        }

        if (fail) errln("Fail: Week of year misbehaving");
    }

    @Test
    public void Test4106136() {
        Locale saveLocale = Locale.getDefault();
        String[] names = {"Calendar", "DateFormat", "NumberFormat"};
        try {
            Locale[] locales = {Locale.CHINESE, Locale.CHINA};
            for (int i = 0; i < locales.length; ++i) {
                Locale.setDefault(locales[i]);
                int[] n = {
                    Calendar.getAvailableLocales().length,
                    DateFormat.getAvailableLocales().length,
                    NumberFormat.getAvailableLocales().length
                };
                for (int j = 0; j < n.length; ++j) {
                    if (n[j] == 0) errln("Fail: " + names[j] + " has no locales for " + locales[i]);
                }
            }
        } finally {
            Locale.setDefault(saveLocale);
        }
    }

    @Test
    public void Test4108764() {
        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1997, Calendar.FEBRUARY, 15, 12, 00, 00);
        Date d00 = tempcal.getTime();
        tempcal.set(1997, Calendar.FEBRUARY, 15, 12, 00, 56);
        Date d01 = tempcal.getTime();
        tempcal.set(1997, Calendar.FEBRUARY, 15, 12, 34, 00);
        Date d10 = tempcal.getTime();
        tempcal.set(1997, Calendar.FEBRUARY, 15, 12, 34, 56);
        Date d11 = tempcal.getTime();
        tempcal.set(1997, Calendar.JANUARY, 15, 12, 34, 56);
        Date dM = tempcal.getTime();
        tempcal.clear();
        tempcal.set(1970, Calendar.JANUARY, 1);
        Date epoch = tempcal.getTime();

        Calendar cal = Calendar.getInstance();
        cal.setTime(d11);

        cal.clear(Calendar.MINUTE);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(d01)) {
            errln("Fail: " + d11 + " clear(MINUTE) => expect " + d01 + ", got " + cal.getTime());
        }

        cal.set(Calendar.SECOND, 0);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(d00)) errln("Fail: set(SECOND, 0) broken");

        cal.setTime(d11);
        cal.set(Calendar.SECOND, 0);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(d10)) errln("Fail: set(SECOND, 0) broken #2");

        cal.clear(Calendar.MINUTE);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(d00)) errln("Fail: clear(MINUTE) broken #2");

        cal.clear();
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(epoch))
            errln("Fail: after clear() expect " + epoch + ", got " + cal.getTime());

        cal.setTime(d11);
        cal.clear(Calendar.MONTH);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(dM)) {
            errln("Fail: " + d11 + " clear(MONTH) => expect " + dM + ", got " + cal.getTime());
        }
    }

    @Test
    public void Test4114578() {
        int ONE_HOUR = 60 * 60 * 1000;
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("PST"));

        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1998, Calendar.APRIL, 5, 1, 0);
        long onset = tempcal.getTime().getTime() + ONE_HOUR;
        tempcal.set(1998, Calendar.OCTOBER, 25, 0, 0);
        long cease = tempcal.getTime().getTime() + 2 * ONE_HOUR;

        boolean fail = false;

        final int ADD = 1;
        final int ROLL = 2;

        long[] DATA = {
            // Start Action Amt Expected_change
            onset - ONE_HOUR,
            ADD,
            1,
            ONE_HOUR,
            onset,
            ADD,
            -1,
            -ONE_HOUR,
            onset - ONE_HOUR,
            ROLL,
            1,
            ONE_HOUR,
            onset,
            ROLL,
            -1,
            -ONE_HOUR,
            cease - ONE_HOUR,
            ADD,
            1,
            ONE_HOUR,
            cease,
            ADD,
            -1,
            -ONE_HOUR,
            cease - ONE_HOUR,
            ROLL,
            1,
            ONE_HOUR,
            cease,
            ROLL,
            -1,
            -ONE_HOUR,
        };

        for (int i = 0; i < DATA.length; i += 4) {
            Date date = new Date(DATA[i]);
            int amt = (int) DATA[i + 2];
            long expectedChange = DATA[i + 3];

            log(date.toString());
            cal.setTime(date);

            switch ((int) DATA[i + 1]) {
                case ADD:
                    log(" add (HOUR," + (amt < 0 ? "" : "+") + amt + ")= ");
                    cal.add(Calendar.HOUR, amt);
                    break;
                case ROLL:
                    log(" roll(HOUR," + (amt < 0 ? "" : "+") + amt + ")= ");
                    cal.roll(Calendar.HOUR, amt);
                    break;
            }

            log(cal.getTime().toString());

            long change = cal.getTime().getTime() - date.getTime();
            if (change != expectedChange) {
                fail = true;
                logln(" FAIL");
            } else logln(" OK");
        }

        if (fail) errln("Fail: roll/add misbehaves around DST onset/cease");
    }

    /** Make sure maximum for HOUR field is 11, not 12. */
    @Test
    public void Test4118384() {
        Calendar cal = Calendar.getInstance();
        if (cal.getMaximum(Calendar.HOUR) != 11
                || cal.getLeastMaximum(Calendar.HOUR) != 11
                || cal.getActualMaximum(Calendar.HOUR) != 11)
            errln("Fail: maximum of HOUR field should be 11");
    }

    /** Check isLeapYear for BC years. */
    @Test
    public void Test4125881() {
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        DateFormat fmt = new SimpleDateFormat("MMMM d, yyyy G");
        cal.clear();
        for (int y = -20; y <= 10; ++y) {
            cal.set(Calendar.ERA, y < 1 ? GregorianCalendar.BC : GregorianCalendar.AD);
            cal.set(Calendar.YEAR, y < 1 ? 1 - y : y);
            logln(y + " = " + fmt.format(cal.getTime()) + " " + cal.isLeapYear(y));
            if (cal.isLeapYear(y) != ((y + 40) % 4 == 0)) errln("Leap years broken");
        }
    }

    // I am disabling this test -- it is currently failing because of a bug
    // in Sun's latest change to STZ.getOffset(). I have filed a Sun bug
    // against this problem.

    // Re-enabled after 'porting' TZ and STZ from java.util to com.ibm.icu.util.
    /**
     * Prove that GregorianCalendar is proleptic (it used to cut off at 45 BC, and not have leap
     * years before then).
     */
    @Test
    public void Test4125892() {
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        // DateFormat fmt = new SimpleDateFormat("MMMM d, yyyy G");
        // fmt = null;
        cal.clear();
        cal.set(Calendar.ERA, GregorianCalendar.BC);
        cal.set(Calendar.YEAR, 81); // 81 BC is a leap year (proleptically)
        cal.set(Calendar.MONTH, Calendar.FEBRUARY);
        cal.set(Calendar.DATE, 28);
        cal.add(Calendar.DATE, 1);
        if (cal.get(Calendar.DATE) != 29 || !cal.isLeapYear(-80)) // -80 == 81 BC
        errln("Calendar not proleptic");
    }

    /**
     * Calendar and GregorianCalendar hashCode() methods need improvement. Calendar needs a good
     * implementation that subclasses can override, and GregorianCalendar should use that
     * implementation.
     */
    @Test
    public void Test4136399() {
        /*
         * Note: This test is actually more strict than it has to be.
         * Technically, there is no requirement that unequal objects have
         * unequal hashes. We only require equal objects to have equal hashes.
         * It is desirable for unequal objects to have distributed hashes, but
         * there is no hard requirement here.
         *
         * In this test we make assumptions about certain attributes of calendar
         * objects getting represented in the hash, which need not always be the
         * case (although it does work currently with the given test).
         */
        Calendar a = Calendar.getInstance();
        Calendar b = a.clone();
        assertEquals("Calendar hash code unequal for cloned objects", a.hashCode(), b.hashCode());
        TimeZone atz1 = a.getTimeZone();
        TimeZone atz2 = atz1.clone();
        assertEquals("The clone timezones are not equal", atz1, atz2);
        assertEquals(
                "TimeZone hash code unequal for cloned objects", atz1.hashCode(), atz2.hashCode());
        b.setMinimalDaysInFirstWeek(7 - a.getMinimalDaysInFirstWeek());
        assertNotEquals(
                "Calendar hash code ignores minimal days in first week",
                a.hashCode(),
                b.hashCode());
        b.setMinimalDaysInFirstWeek(a.getMinimalDaysInFirstWeek());

        b.setFirstDayOfWeek((a.getFirstDayOfWeek() % 7) + 1); // Next day
        assertNotEquals("Calendar hash code ignores first day of week", a.hashCode(), b.hashCode());
        b.setFirstDayOfWeek(a.getFirstDayOfWeek());

        b.setLenient(!a.isLenient());
        assertNotEquals("Calendar hash code ignores lenient setting", a.hashCode(), b.hashCode());
        b.setLenient(a.isLenient());

        // Assume getTimeZone() returns a reference, not a clone
        // of a reference -- this is true as of this writing
        TimeZone atz = a.getTimeZone();
        TimeZone btz = b.getTimeZone();

        btz.setRawOffset(atz.getRawOffset() + 60 * 60 * 1000);
        assertNotEquals(atz.hashCode() + "==" + btz.hashCode(), atz.hashCode(), btz.hashCode());
        if (a.hashCode() == b.hashCode()) {
            assertEquals("Calendar hash code ignores zone", a.getTimeZone(), b.getTimeZone());
        }
        b.getTimeZone().setRawOffset(a.getTimeZone().getRawOffset());

        GregorianCalendar c = new GregorianCalendar();
        GregorianCalendar d = c.clone();
        assertEquals(
                "GregorianCalendar hash code unequal for clones objects",
                c.hashCode(),
                d.hashCode());
        Date cutover = c.getGregorianChange();
        d.setGregorianChange(new Date(cutover.getTime() + 24 * 60 * 60 * 1000));
        assertNotEquals("GregorianCalendar hash code ignores cutover", c.hashCode(), d.hashCode());
    }

    /** GregorianCalendar.equals() ignores cutover date */
    @Test
    public void Test4141665() {
        GregorianCalendar cal = new GregorianCalendar();
        GregorianCalendar cal2 = cal.clone();
        Date cut = cal.getGregorianChange();
        Date cut2 = new Date(cut.getTime() + 100 * 24 * 60 * 60 * 1000L); // 100 days
        // later
        if (!cal.equals(cal2)) {
            errln("Cloned GregorianCalendars not equal");
        }
        cal2.setGregorianChange(cut2);
        if (cal.equals(cal2)) {
            errln("GregorianCalendar.equals() ignores cutover");
        }
    }

    /**
     * Bug states that ArrayIndexOutOfBoundsException is thrown by GregorianCalendar.roll() when
     * IllegalArgumentException should be.
     */
    @Test
    public void Test4142933() {
        GregorianCalendar calendar = new GregorianCalendar();
        try {
            calendar.roll(-1, true);
            errln("Test failed, no exception trown");
        } catch (IllegalArgumentException e) {
            // OK: Do nothing
            // logln("Test passed");
            System.out.print("");
        } catch (Exception e) {
            errln("Test failed. Unexpected exception is thrown: " + e);
            e.printStackTrace();
        }
    }

    /**
     * GregorianCalendar handling of Dates Long.MIN_VALUE and Long.MAX_VALUE is confusing; unless
     * the time zone has a raw offset of zero, one or the other of these will wrap. We've modified
     * the test given in the bug report to therefore only check the behavior of a calendar with a
     * zero raw offset zone.
     */
    @Test
    public void Test4145158() {
        GregorianCalendar calendar = new GregorianCalendar();

        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

        calendar.setTime(new Date(Long.MIN_VALUE));
        int year1 = calendar.get(Calendar.YEAR);
        int era1 = calendar.get(Calendar.ERA);

        calendar.setTime(new Date(Long.MAX_VALUE));
        int year2 = calendar.get(Calendar.YEAR);
        int era2 = calendar.get(Calendar.ERA);

        if (year1 == year2 && era1 == era2) {
            errln("Fail: Long.MIN_VALUE or Long.MAX_VALUE wrapping around");
        }
    }

    /** Maximum value for YEAR field wrong. */
    @Test
    public void Test4145983() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date[] DATES = {new Date(Long.MAX_VALUE), new Date(Long.MIN_VALUE)};
        for (int i = 0; i < DATES.length; ++i) {
            calendar.setTime(DATES[i]);
            int year = calendar.get(Calendar.YEAR);
            int maxYear = calendar.getMaximum(Calendar.YEAR);
            if (year > maxYear) {
                errln(
                        "Failed for "
                                + DATES[i].getTime()
                                + " ms: year="
                                + year
                                + ", maxYear="
                                + maxYear);
            }
        }
    }

    /**
     * This is a bug in the validation code of GregorianCalendar. As reported, the bug seems worse
     * than it really is, due to a bug in the way the bug report test was written. In reality the
     * bug is restricted to the DAY_OF_YEAR field. - liu 6/29/98
     */
    @Test
    public void Test4147269() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setLenient(false);
        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(1996, Calendar.JANUARY, 3); // Arbitrary date
        Date date = tempcal.getTime();
        for (int field = 0; field < calendar.getFieldCount(); field++) {
            calendar.setTime(date);
            // Note: In the bug report, getActualMaximum() was called instead
            // of getMaximum() -- this was an error. The validation code doesn't
            // use getActualMaximum(), since that's too costly.
            int max = calendar.getMaximum(field);
            int value = max + 1;
            calendar.set(field, value);
            try {
                calendar.getTime(); // Force time computation
                // We expect an exception to be thrown. If we fall through
                // to the next line, then we have a bug.
                errln(
                        "Test failed with field "
                                + FIELD_NAME[field]
                                + ", date before: "
                                + date
                                + ", date after: "
                                + calendar.getTime()
                                + ", value: "
                                + value
                                + " (max = "
                                + max
                                + ")");
            } catch (IllegalArgumentException e) {
                System.out.print("");
            }
        }
    }

    /**
     * Reported bug is that a GregorianCalendar with a cutover of Date(Long.MAX_VALUE) doesn't
     * behave as a pure Julian calendar. CANNOT REPRODUCE THIS BUG
     */
    @Test
    public void Test4149677() {
        TimeZone[] zones = {
            TimeZone.getTimeZone("GMT"), TimeZone.getTimeZone("PST"), TimeZone.getTimeZone("EAT")
        };
        for (int i = 0; i < zones.length; ++i) {
            GregorianCalendar calendar = new GregorianCalendar(zones[i]);

            // Make sure extreme values don't wrap around
            calendar.setTime(new Date(Long.MIN_VALUE));
            if (calendar.get(Calendar.ERA) != GregorianCalendar.BC) {
                errln("Fail: Long.MIN_VALUE ms has an AD year");
            }
            calendar.setTime(new Date(Long.MAX_VALUE));
            if (calendar.get(Calendar.ERA) != GregorianCalendar.AD) {
                errln("Fail: Long.MAX_VALUE ms has a BC year");
            }

            calendar.setGregorianChange(new Date(Long.MAX_VALUE));
            // to obtain a pure Julian calendar

            boolean is100Leap = calendar.isLeapYear(100);
            if (!is100Leap) {
                errln("test failed with zone " + zones[i].getID());
                errln(" cutover date is Calendar.MAX_DATE");
                errln(" isLeapYear(100) returns: " + is100Leap);
            }
        }
    }

    /**
     * Calendar and Date HOUR broken. If HOUR is out-of-range, Calendar and Date classes will
     * misbehave.
     */
    @Test
    public void Test4162587() {
        TimeZone saveZone = TimeZone.getDefault();

        try {
            TimeZone tz = TimeZone.getTimeZone("PST");
            TimeZone.setDefault(tz);
            GregorianCalendar cal = new GregorianCalendar(tz);
            Date d;

            for (int i = 0; i < 5; ++i) {
                if (i > 0) logln("---");

                cal.clear();
                cal.set(1998, Calendar.APRIL, 5, i, 0);
                d = cal.getTime();
                String s0 = d.toString();
                logln("0 " + i + ": " + s0);

                cal.clear();
                cal.set(1998, Calendar.APRIL, 4, i + 24, 0);
                d = cal.getTime();
                String sPlus = d.toString();
                logln("+ " + i + ": " + sPlus);

                cal.clear();
                cal.set(1998, Calendar.APRIL, 6, i - 24, 0);
                d = cal.getTime();
                String sMinus = d.toString();
                logln("- " + i + ": " + sMinus);

                if (!s0.equals(sPlus) || !s0.equals(sMinus)) {
                    errln("Fail: All three lines must match");
                }
            }
        } finally {
            TimeZone.setDefault(saveZone);
        }
    }

    /** Adding 12 months behaves differently from adding 1 year */
    @Test
    public void Test4165343() {
        GregorianCalendar calendar = new GregorianCalendar(1996, Calendar.FEBRUARY, 29);
        Date start = calendar.getTime();
        logln("init date: " + start);
        calendar.add(Calendar.MONTH, 12);
        Date date1 = calendar.getTime();
        logln("after adding 12 months: " + date1);
        calendar.setTime(start);
        calendar.add(Calendar.YEAR, 1);
        Date date2 = calendar.getTime();
        logln("after adding one year : " + date2);
        if (date1.equals(date2)) {
            logln("Test passed");
        } else {
            errln("Test failed");
        }
    }

    /** GregorianCalendar.getActualMaximum() does not account for first day of week. */
    @Test
    public void Test4166109() {
        /*
         * Test month:
         *
         * March 1998 Su Mo Tu We Th Fr Sa 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
         * 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
         */
        boolean passed = true;
        int field = Calendar.WEEK_OF_MONTH;

        GregorianCalendar calendar = new GregorianCalendar(Locale.US);
        calendar.set(1998, Calendar.MARCH, 1);
        calendar.setMinimalDaysInFirstWeek(1);
        logln("Date:  " + calendar.getTime());

        int firstInMonth = calendar.get(Calendar.DAY_OF_MONTH);

        for (int firstInWeek = Calendar.SUNDAY; firstInWeek <= Calendar.SATURDAY; firstInWeek++) {
            calendar.setFirstDayOfWeek(firstInWeek);
            int returned = calendar.getActualMaximum(field);
            int expected = (31 + ((firstInMonth - firstInWeek + 7) % 7) + 6) / 7;

            logln(
                    "First day of week = "
                            + firstInWeek
                            + "  getActualMaximum(WEEK_OF_MONTH) = "
                            + returned
                            + "  expected = "
                            + expected
                            + ((returned == expected) ? "  ok" : "  FAIL"));

            if (returned != expected) {
                passed = false;
            }
        }
        if (!passed) {
            errln("Test failed");
        }
    }

    /** Calendar.getActualMaximum(YEAR) works wrong. */
    @Test
    public void Test4167060() {
        int field = Calendar.YEAR;
        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy G", Locale.US);

        GregorianCalendar calendars[] = {
            new GregorianCalendar(100, Calendar.NOVEMBER, 1),
            new GregorianCalendar(-99 /* 100BC */, Calendar.JANUARY, 1),
            new GregorianCalendar(1996, Calendar.FEBRUARY, 29),
        };

        String[] id = {"Hybrid", "Gregorian", "Julian"};

        for (int k = 0; k < 3; ++k) {
            logln("--- " + id[k] + " ---");

            for (int j = 0; j < calendars.length; ++j) {
                GregorianCalendar calendar = calendars[j];
                if (k == 1) {
                    calendar.setGregorianChange(new Date(Long.MIN_VALUE));
                } else if (k == 2) {
                    calendar.setGregorianChange(new Date(Long.MAX_VALUE));
                }

                format.setCalendar(calendar.clone());

                Date dateBefore = calendar.getTime();

                int maxYear = calendar.getActualMaximum(field);
                logln("maxYear: " + maxYear + " for " + format.format(calendar.getTime()));
                logln("date before: " + format.format(dateBefore));

                int years[] = {2000, maxYear - 1, maxYear, maxYear + 1};

                for (int i = 0; i < years.length; i++) {
                    boolean valid = years[i] <= maxYear;
                    calendar.set(field, years[i]);
                    Date dateAfter = calendar.getTime();
                    int newYear = calendar.get(field);
                    calendar.setTime(dateBefore); // restore calendar for next
                    // use

                    logln(
                            " Year "
                                    + years[i]
                                    + (valid ? " ok " : " bad")
                                    + " => "
                                    + format.format(dateAfter));
                    if (valid && newYear != years[i]) {
                        errln(
                                "  FAIL: "
                                        + newYear
                                        + " should be valid; date, month and time shouldn't change");
                    } else if (!valid && newYear == years[i]) {
                        // We no longer require strict year maxima. That is, the
                        // calendar
                        // algorithm may work for values > the stated maximum.
                        // errln(" FAIL: " + newYear + " should be invalid");
                        logln("  Note: " + newYear + " > maximum, but still valid");
                    }
                }
            }
        }
    }

    /** Calendar.roll broken This bug relies on the TimeZone bug 4173604 to also be fixed. */
    @Test
    public void Test4173516() {
        int fieldsList[][] = {
            {1997, Calendar.FEBRUARY, 1, 10, 45, 15, 900},
            {1999, Calendar.DECEMBER, 22, 23, 59, 59, 999}
        };
        int limit = 40;
        GregorianCalendar cal = new GregorianCalendar();

        cal.setTime(new Date(0));
        cal.roll(Calendar.HOUR, 0x7F000000);
        cal.roll(Calendar.HOUR, -0x7F000000);
        if (cal.getTime().getTime() != 0) {
            errln("Hour rolling broken");
        }

        for (int op = 0; op < 2; ++op) {
            logln("Testing GregorianCalendar " + (op == 0 ? "add" : "roll"));
            for (int field = 0; field < cal.getFieldCount(); ++field) {
                if (field != Calendar.ZONE_OFFSET
                        && field != Calendar.DST_OFFSET
                        && field != Calendar.IS_LEAP_MONTH) {
                    for (int j = 0; j < fieldsList.length; ++j) {
                        int fields[] = fieldsList[j];
                        cal.clear();
                        cal.set(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
                        cal.set(Calendar.MILLISECOND, fields[6]);
                        cal.setMinimalDaysInFirstWeek(1);
                        for (int i = 0; i < 2 * limit; i++) {
                            if (op == 0) {
                                cal.add(field, i < limit ? 1 : -1);
                            } else {
                                cal.roll(field, i < limit ? 1 : -1);
                            }
                        }
                        if (cal.get(Calendar.YEAR) != fields[0]
                                || cal.get(Calendar.MONTH) != fields[1]
                                || cal.get(Calendar.DATE) != fields[2]
                                || cal.get(Calendar.HOUR_OF_DAY) != fields[3]
                                || cal.get(Calendar.MINUTE) != fields[4]
                                || cal.get(Calendar.SECOND) != fields[5]
                                || cal.get(Calendar.MILLISECOND) != fields[6]) {
                            errln(
                                    "Field "
                                            + field
                                            + " "
                                            + (op == 0 ? "add" : "roll")
                                            + " ("
                                            + FIELD_NAME[field]
                                            + ") FAIL, expected "
                                            + fields[0]
                                            + "/"
                                            + (fields[1] + 1)
                                            + "/"
                                            + fields[2]
                                            + " "
                                            + fields[3]
                                            + ":"
                                            + fields[4]
                                            + ":"
                                            + fields[5]
                                            + "."
                                            + fields[6]
                                            + ", got "
                                            + cal.get(Calendar.YEAR)
                                            + "/"
                                            + (cal.get(Calendar.MONTH) + 1)
                                            + "/"
                                            + cal.get(Calendar.DATE)
                                            + " "
                                            + cal.get(Calendar.HOUR_OF_DAY)
                                            + ":"
                                            + cal.get(Calendar.MINUTE)
                                            + ":"
                                            + cal.get(Calendar.SECOND)
                                            + "."
                                            + cal.get(Calendar.MILLISECOND));
                            cal.clear();
                            cal.set(
                                    fields[0], fields[1], fields[2], fields[3], fields[4],
                                    fields[5]);
                            cal.set(Calendar.MILLISECOND, fields[6]);
                            logln(
                                    "Start date: "
                                            + cal.get(Calendar.YEAR)
                                            + "/"
                                            + (cal.get(Calendar.MONTH) + 1)
                                            + "/"
                                            + cal.get(Calendar.DATE)
                                            + " "
                                            + cal.get(Calendar.HOUR_OF_DAY)
                                            + ":"
                                            + cal.get(Calendar.MINUTE)
                                            + ":"
                                            + cal.get(Calendar.SECOND)
                                            + "."
                                            + cal.get(Calendar.MILLISECOND));
                            long prev = cal.getTime().getTime();
                            for (int i = 0; i < 2 * limit; i++) {
                                if (op == 0) {
                                    cal.add(field, i < limit ? 1 : -1);
                                } else {
                                    cal.roll(field, i < limit ? 1 : -1);
                                }
                                long t = cal.getTime().getTime();
                                long delta = t - prev;
                                prev = t;
                                logln(
                                        (op == 0 ? "add(" : "roll(")
                                                + FIELD_NAME[field]
                                                + (i < limit ? ", +1) => " : ", -1) => ")
                                                + cal.get(Calendar.YEAR)
                                                + "/"
                                                + (cal.get(Calendar.MONTH) + 1)
                                                + "/"
                                                + cal.get(Calendar.DATE)
                                                + " "
                                                + cal.get(Calendar.HOUR_OF_DAY)
                                                + ":"
                                                + cal.get(Calendar.MINUTE)
                                                + ":"
                                                + cal.get(Calendar.SECOND)
                                                + "."
                                                + cal.get(Calendar.MILLISECOND)
                                                + " delta="
                                                + delta
                                                + " ms");
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void Test4174361() {
        GregorianCalendar calendar = new GregorianCalendar(1996, 1, 29);

        calendar.add(Calendar.MONTH, 10);
        // Date date1 = calendar.getTime();
        // date1 = null;
        int d1 = calendar.get(Calendar.DAY_OF_MONTH);

        calendar = new GregorianCalendar(1996, 1, 29);
        calendar.add(Calendar.MONTH, 11);
        // Date date2 = calendar.getTime();
        // date2 = null;
        int d2 = calendar.get(Calendar.DAY_OF_MONTH);

        if (d1 != d2) {
            errln("adding months to Feb 29 broken");
        }
    }

    /** Calendar does not update field values when setTimeZone is called. */
    @Test
    public void Test4177484() {
        TimeZone PST = TimeZone.getTimeZone("PST");
        TimeZone EST = TimeZone.getTimeZone("EST");

        Calendar cal = Calendar.getInstance(PST, Locale.US);
        cal.clear();
        cal.set(1999, 3, 21, 15, 5, 0); // Arbitrary
        int h1 = cal.get(Calendar.HOUR_OF_DAY);
        cal.setTimeZone(EST);
        int h2 = cal.get(Calendar.HOUR_OF_DAY);
        if (h1 == h2) {
            errln("FAIL: Fields not updated after setTimeZone");
        }

        // getTime() must NOT change when time zone is changed.
        // getTime() returns zone-independent time in ms.
        cal.clear();
        cal.setTimeZone(PST);
        cal.set(Calendar.HOUR_OF_DAY, 10);
        Date pst10 = cal.getTime();
        cal.setTimeZone(EST);
        Date est10 = cal.getTime();
        if (!pst10.equals(est10)) {
            errln("FAIL: setTimeZone changed time");
        }
    }

    /** Week of year is wrong at the start and end of the year. */
    @Test
    public void Test4197699() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        @SuppressWarnings("MisusedDayOfYear")
        DateFormat fmt = new SimpleDateFormat("E dd MMM yyyy  'DOY='D 'WOY='w");
        fmt.setCalendar(cal);

        int[] DATA = {
            2000, Calendar.JANUARY, 1, 52,
            2001, Calendar.DECEMBER, 31, 1,
        };

        for (int i = 0; i < DATA.length; ) {
            cal.set(DATA[i++], DATA[i++], DATA[i++]);
            int expWOY = DATA[i++];
            int actWOY = cal.get(Calendar.WEEK_OF_YEAR);
            if (expWOY == actWOY) {
                logln("Ok: " + fmt.format(cal.getTime()));
            } else {
                errln("FAIL: " + fmt.format(cal.getTime()) + ", expected WOY=" + expWOY);
                cal.add(Calendar.DATE, -8);
                for (int j = 0; j < 14; ++j) {
                    cal.add(Calendar.DATE, 1);
                    logln(fmt.format(cal.getTime()));
                }
            }
        }
    }

    /**
     * Calendar DAY_OF_WEEK_IN_MONTH fields->time broken. The problem is in the field disambiguation
     * code in GregorianCalendar. This code is supposed to choose the most recent set of fields
     * among the following:
     *
     * <p>MONTH + DAY_OF_MONTH MONTH + WEEK_OF_MONTH + DAY_OF_WEEK MONTH + DAY_OF_WEEK_IN_MONTH +
     * DAY_OF_WEEK DAY_OF_YEAR WEEK_OF_YEAR + DAY_OF_WEEK
     */
    @Test
    public void Test4209071() {
        Calendar cal = Calendar.getInstance(Locale.US);

        // General field setting test
        int Y = 1995;

        Date d[] = new Date[13];
        java.util.Calendar tempcal = java.util.Calendar.getInstance();
        tempcal.clear();
        tempcal.set(Y, Calendar.JANUARY, 1);
        d[0] = tempcal.getTime();
        tempcal.set(Y, Calendar.MARCH, 1);
        d[1] = tempcal.getTime();
        tempcal.set(Y, Calendar.JANUARY, 4);
        d[2] = tempcal.getTime();
        tempcal.set(Y, Calendar.JANUARY, 18);
        d[3] = tempcal.getTime();
        tempcal.set(Y, Calendar.JANUARY, 18);
        d[4] = tempcal.getTime();
        tempcal.set(Y - 1, Calendar.DECEMBER, 22);
        d[5] = tempcal.getTime();
        tempcal.set(Y, Calendar.JANUARY, 26);
        d[6] = tempcal.getTime();
        tempcal.set(Y, Calendar.JANUARY, 26);
        d[7] = tempcal.getTime();
        tempcal.set(Y, Calendar.MARCH, 1);
        d[8] = tempcal.getTime();
        tempcal.set(Y, Calendar.OCTOBER, 6);
        d[9] = tempcal.getTime();
        tempcal.set(Y, Calendar.OCTOBER, 13);
        d[10] = tempcal.getTime();
        tempcal.set(Y, Calendar.AUGUST, 10);
        d[11] = tempcal.getTime();
        tempcal.set(Y, Calendar.DECEMBER, 7);
        d[12] = tempcal.getTime();

        Object[] FIELD_DATA = {
            // Add new test cases as needed.

            // 0
            new int[] {},
            d[0],
            // 1
            new int[] {Calendar.MONTH, Calendar.MARCH},
            d[1],
            // 2
            new int[] {Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY},
            d[2],
            // 3
            new int[] {
                Calendar.DAY_OF_WEEK, Calendar.THURSDAY, Calendar.DAY_OF_MONTH, 18,
            },
            d[3],
            // 4
            new int[] {
                Calendar.DAY_OF_MONTH, 18, Calendar.DAY_OF_WEEK, Calendar.THURSDAY,
            },
            d[4],
            // 5 (WOM -1 is in previous month)
            new int[] {
                Calendar.DAY_OF_MONTH, 18,
                Calendar.WEEK_OF_MONTH, -1,
                Calendar.DAY_OF_WEEK, Calendar.THURSDAY,
            },
            d[5],
            // 6
            new int[] {
                Calendar.DAY_OF_MONTH, 18,
                Calendar.WEEK_OF_MONTH, 4,
                Calendar.DAY_OF_WEEK, Calendar.THURSDAY,
            },
            d[6],
            // 7 (DIM -1 is in same month)
            new int[] {
                Calendar.DAY_OF_MONTH, 18,
                Calendar.DAY_OF_WEEK_IN_MONTH, -1,
                Calendar.DAY_OF_WEEK, Calendar.THURSDAY,
            },
            d[7],
            // 8
            new int[] {
                Calendar.WEEK_OF_YEAR, 9, Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY,
            },
            d[8],
            // 9
            new int[] {
                Calendar.MONTH, Calendar.OCTOBER,
                Calendar.DAY_OF_WEEK_IN_MONTH, 1,
                Calendar.DAY_OF_WEEK, Calendar.FRIDAY,
            },
            d[9],
            // 10
            new int[] {
                Calendar.MONTH, Calendar.OCTOBER,
                Calendar.WEEK_OF_MONTH, 2,
                Calendar.DAY_OF_WEEK, Calendar.FRIDAY,
            },
            d[10],
            // 11
            new int[] {
                Calendar.MONTH, Calendar.OCTOBER,
                Calendar.DAY_OF_MONTH, 15,
                Calendar.DAY_OF_YEAR, 222,
            },
            d[11],
            // 12
            new int[] {
                Calendar.DAY_OF_WEEK, Calendar.THURSDAY,
                Calendar.MONTH, Calendar.DECEMBER,
            },
            d[12],
        };

        for (int i = 0; i < FIELD_DATA.length; i += 2) {
            int[] fields = (int[]) FIELD_DATA[i];
            Date exp = (Date) FIELD_DATA[i + 1];

            cal.clear();
            cal.set(Calendar.YEAR, Y);
            for (int j = 0; j < fields.length; j += 2) {
                cal.set(fields[j], fields[j + 1]);
            }

            Date act = cal.getTime();
            if (!act.equals(exp)) {
                errln(
                        "FAIL: Test "
                                + (i / 2)
                                + " got "
                                + act
                                + ", want "
                                + exp
                                + " (see test/java/util/Calendar/CalendarRegressionTest.java");
            }
        }

        tempcal.set(1997, Calendar.JANUARY, 5);
        d[0] = tempcal.getTime();
        tempcal.set(1997, Calendar.JANUARY, 26);
        d[1] = tempcal.getTime();
        tempcal.set(1997, Calendar.FEBRUARY, 23);
        d[2] = tempcal.getTime();
        tempcal.set(1997, Calendar.JANUARY, 26);
        d[3] = tempcal.getTime();
        tempcal.set(1997, Calendar.JANUARY, 5);
        d[4] = tempcal.getTime();
        tempcal.set(1996, Calendar.DECEMBER, 8);
        d[5] = tempcal.getTime();
        // Test specific failure reported in bug
        Object[] DATA = {
            1, d[0], 4, d[1],
            8, d[2], -1, d[3],
            -4, d[4], -8, d[5],
        };
        for (int i = 0; i < DATA.length; i += 2) {
            cal.clear();
            cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, ((Number) DATA[i]).intValue());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.YEAR, 1997);
            Date actual = cal.getTime();
            if (!actual.equals(DATA[i + 1])) {
                errln(
                        "FAIL: Sunday "
                                + DATA[i]
                                + " of Jan 1997 -> "
                                + actual
                                + ", want "
                                + DATA[i + 1]);
            }
        }
    }

    /**
     * WEEK_OF_YEAR computed incorrectly. A failure of this test can indicate a problem in several
     * different places in the
     */
    @Test
    public void Test4288792() throws Exception {
        TimeZone savedTZ = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        GregorianCalendar cal = new GregorianCalendar();

        for (int i = 1900; i < 2100; i++) {
            for (int j1 = 1; j1 <= 7; j1++) {
                // Loop for MinimalDaysInFirstWeek: 1..7
                for (int j = Calendar.SUNDAY; j <= Calendar.SATURDAY; j++) {
                    // Loop for FirstDayOfWeek: SUNDAY..SATURDAY
                    cal.clear();
                    cal.setMinimalDaysInFirstWeek(j1);
                    cal.setFirstDayOfWeek(j);
                    // Set the calendar to the first day of the last week
                    // of the year. This may overlap some of the start of
                    // the next year; that is, the last week of 1999 may
                    // include some of January 2000. Use the add() method
                    // to advance through the week. For each day, call
                    // get(WEEK_OF_YEAR). The result should be the same
                    // for the whole week. Note that a bug in
                    // getActualMaximum() will break this test.

                    // Set date to the mid year first before getActualMaximum(WEEK_OF_YEAR).
                    // getActualMaximum(WEEK_OF_YEAR) is based on the current calendar's
                    // year of week of year. After clear(), calendar is set to January 1st,
                    // which may belongs to previous year of week of year.
                    cal.set(i, Calendar.JULY, 1);
                    int maxWeek = cal.getActualMaximum(Calendar.WEEK_OF_YEAR);
                    cal.set(Calendar.WEEK_OF_YEAR, maxWeek);
                    cal.set(Calendar.DAY_OF_WEEK, j);
                    for (int k = 1; k < 7; k++) {
                        cal.add(Calendar.DATE, 1);
                        int WOY = cal.get(Calendar.WEEK_OF_YEAR);
                        if (WOY != maxWeek) {
                            errln(
                                    cal.getTime()
                                            + ",got="
                                            + WOY
                                            + ",expected="
                                            + maxWeek
                                            + ",min="
                                            + j1
                                            + ",first="
                                            + j);
                        }
                    }
                    // Now advance the calendar one more day. This should
                    // put it at the first day of week 1 of the next year.
                    cal.add(Calendar.DATE, 1);
                    int WOY = cal.get(Calendar.WEEK_OF_YEAR);
                    if (WOY != 1) {
                        errln(
                                cal.getTime()
                                        + ",got="
                                        + WOY
                                        + ",expected=1,min="
                                        + j1
                                        + ",first"
                                        + j);
                    }
                }
            }
        }
        TimeZone.setDefault(savedTZ);
    }

    /** Test fieldDifference(). */
    @Test
    public void TestJ438() throws Exception {
        int DATA[] = {
            2000, Calendar.JANUARY, 20, 2010, Calendar.JUNE, 15,
            2010, Calendar.JUNE, 15, 2000, Calendar.JANUARY, 20,
            1964, Calendar.SEPTEMBER, 7, 1999, Calendar.JUNE, 4,
            1999, Calendar.JUNE, 4, 1964, Calendar.SEPTEMBER, 7,
        };
        Calendar cal = Calendar.getInstance(Locale.US);
        for (int i = 0; i < DATA.length; i += 6) {
            int y1 = DATA[i];
            int m1 = DATA[i + 1];
            int d1 = DATA[i + 2];
            int y2 = DATA[i + 3];
            int m2 = DATA[i + 4];
            int d2 = DATA[i + 5];

            cal.clear();
            cal.set(y1, m1, d1);
            Date date1 = cal.getTime();
            cal.set(y2, m2, d2);
            Date date2 = cal.getTime();

            cal.setTime(date1);
            int dy = cal.fieldDifference(date2, Calendar.YEAR);
            int dm = cal.fieldDifference(date2, Calendar.MONTH);
            int dd = cal.fieldDifference(date2, Calendar.DATE);

            logln("" + date2 + " - " + date1 + " = " + dy + "y " + dm + "m " + dd + "d");

            cal.setTime(date1);
            cal.add(Calendar.YEAR, dy);
            cal.add(Calendar.MONTH, dm);
            cal.add(Calendar.DATE, dd);
            Date date22 = cal.getTime();
            if (!date2.equals(date22)) {
                errln(
                        "FAIL: " + date1 + " + " + dy + "y " + dm + "m " + dd + "d = " + date22
                                + ", exp " + date2);
            } else {
                logln("Ok: " + date1 + " + " + dy + "y " + dm + "m " + dd + "d = " + date22);
            }
        }
    }

    @Test
    public void TestT5555() throws Exception {
        Calendar cal = Calendar.getInstance();

        // Set date to Wednesday, February 21, 2007
        cal.set(2007, Calendar.FEBRUARY, 21);

        try {
            // Advance month by three years
            cal.add(Calendar.MONTH, 36);

            // Move to last Wednesday of month.
            cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);

            cal.getTime();
        } catch (Exception e) {
            errln("Got an exception calling getTime().");
        }

        int yy, mm, dd, ee;

        yy = cal.get(Calendar.YEAR);
        mm = cal.get(Calendar.MONTH);
        dd = cal.get(Calendar.DATE);
        ee = cal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

        if (yy != 2010 || mm != Calendar.FEBRUARY || dd != 24 || ee != Calendar.WEDNESDAY) {
            errln("Got date " + yy + "/" + (mm + 1) + "/" + dd + ", expected 2010/2/24");
        }
    }

    /** Set behavior of DST_OFFSET field. ICU4J Jitterbug 9. */
    @Test
    public void TestJ9() {
        int HOURS = 60 * 60 * 1000;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("PST"), Locale.US);

        final int END_FIELDS = 0x1234;

        int[] DATA = {
            // With no explicit ZONE/DST expect 12:00 am
            Calendar.MONTH,
            Calendar.JUNE,
            END_FIELDS,
            0,
            0, // expected hour, min

            // Normal ZONE/DST for June 1 Pacific is 8:00/1:00
            Calendar.MONTH,
            Calendar.JUNE,
            Calendar.ZONE_OFFSET,
            -8 * HOURS,
            Calendar.DST_OFFSET,
            HOURS,
            END_FIELDS,
            0,
            0, // expected hour, min

            // With ZONE/DST of 8:00/0:30 expect time of 12:30 am
            Calendar.MONTH,
            Calendar.JUNE,
            Calendar.ZONE_OFFSET,
            -8 * HOURS,
            Calendar.DST_OFFSET,
            HOURS / 2,
            END_FIELDS,
            0,
            30, // expected hour, min

            // With ZONE/DST of 8:00/UNSET expect time of 1:00 am
            Calendar.MONTH,
            Calendar.JUNE,
            Calendar.ZONE_OFFSET,
            -8 * HOURS,
            END_FIELDS,
            1,
            0, // expected hour, min

            // With ZONE/DST of UNSET/0:30 expect 4:30 pm (day before)
            Calendar.MONTH,
            Calendar.JUNE,
            Calendar.DST_OFFSET,
            HOURS / 2,
            END_FIELDS,
            16,
            30, // expected hour, min
        };

        for (int i = 0; i < DATA.length; ) {
            int start = i;
            cal.clear();

            // Set fields
            while (DATA[i] != END_FIELDS) {
                cal.set(DATA[i++], DATA[i++]);
            }
            ++i; // skip over END_FIELDS

            // Get hour/minute
            int h = cal.get(Calendar.HOUR_OF_DAY);
            int m = cal.get(Calendar.MINUTE);

            // Check
            if (h != DATA[i] || m != DATA[i + 1]) {
                errln(
                        "Fail: expected "
                                + DATA[i]
                                + ":"
                                + DATA[i + 1]
                                + ", got "
                                + h
                                + ":"
                                + m
                                + " after:");
                while (DATA[start] != END_FIELDS) {
                    logln("set(" + FIELD_NAME[DATA[start++]] + ", " + DATA[start++] + ");");
                }
            }

            i += 2; // skip over expected hour, min
        }
    }

    /**
     * DateFormat class mistakes date style and time style as follows: -
     * DateFormat.getDateTimeInstance takes date style as time style, and time style as date style -
     * If a Calendar is passed to DateFormat.getDateInstance, it returns time instance - If a
     * Calendar is passed to DateFormat.getTimeInstance, it returns date instance
     */
    @Test
    public void TestDateFormatFactoryJ26() {
        TimeZone zone = TimeZone.getDefault();
        try {
            Locale loc = Locale.US;
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            java.util.Calendar tempcal = java.util.Calendar.getInstance();
            tempcal.set(2001, Calendar.APRIL, 5, 17, 43, 53);
            Date date = tempcal.getTime();
            Calendar cal = Calendar.getInstance(loc);
            Object[] DATA = {
                DateFormat.getDateInstance(DateFormat.SHORT, loc),
                "DateFormat.getDateInstance(DateFormat.SHORT, loc)",
                "4/5/01",
                DateFormat.getTimeInstance(DateFormat.SHORT, loc),
                "DateFormat.getTimeInstance(DateFormat.SHORT, loc)",
                "5:43\u202FPM",
                DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, loc),
                "DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, loc)",
                "Thursday, April 5, 2001 at 5:43\u202FPM",
                DateFormat.getDateInstance(cal, DateFormat.SHORT, loc),
                "DateFormat.getDateInstance(cal, DateFormat.SHORT, loc)",
                "4/5/01",
                DateFormat.getTimeInstance(cal, DateFormat.SHORT, loc),
                "DateFormat.getTimeInstance(cal, DateFormat.SHORT, loc)",
                "5:43\u202FPM",
                DateFormat.getDateTimeInstance(cal, DateFormat.FULL, DateFormat.SHORT, loc),
                "DateFormat.getDateTimeInstance(cal, DateFormat.FULL, DateFormat.SHORT, loc)",
                "Thursday, April 5, 2001 at 5:43\u202FPM",
                cal.getDateTimeFormat(DateFormat.SHORT, DateFormat.FULL, loc),
                "cal.getDateTimeFormat(DateFormat.SHORT, DateFormat.FULL, loc)",
                "4/5/01, 5:43:53\u202FPM Pacific Daylight Time",
                cal.getDateTimeFormat(DateFormat.FULL, DateFormat.SHORT, loc),
                "cal.getDateTimeFormat(DateFormat.FULL, DateFormat.SHORT, loc)",
                "Thursday, April 5, 2001 at 5:43\u202FPM",
            };
            for (int i = 0; i < DATA.length; i += 3) {
                DateFormat df = (DateFormat) DATA[i];
                String desc = (String) DATA[i + 1];
                String exp = (String) DATA[i + 2];
                String got = df.format(date);
                if (got.equals(exp)) {
                    logln("Ok: " + desc + " => " + got);
                } else {
                    errln("FAIL: " + desc + " => " + got + ", expected " + exp);
                }
            }
        } finally {
            TimeZone.setDefault(zone);
        }
    }

    @Test
    public void TestRegistration() {
        /*
         * Set names = Calendar.getCalendarFactoryNames();
         *
         * TimeZone tz = TimeZone.getDefault(); Locale loc =
         * Locale.getDefault(); Iterator iter = names.iterator(); while
         * (iter.hasNext()) { String name = (String)iter.next(); logln("Testing
         * factory: " + name);
         *
         * Calendar cal = Calendar.getInstance(tz, loc, name); logln("Calendar
         * class: " + cal.getClass());
         *
         * DateFormat fmt = cal.getDateTimeFormat(DateFormat.LONG,
         * DateFormat.LONG, loc);
         *
         * logln("Date: " + fmt.format(cal.getTime())); }
         *  // register new default for our locale logln("\nTesting
         * registration"); loc = new Locale("en", "US"); Object key =
         * Calendar.register(JapaneseCalendar.factory(), loc, true);
         *
         * loc = new Locale("en", "US", "TEST"); Calendar cal =
         * Calendar.getInstance(loc); logln("Calendar class: " +
         * cal.getClass()); DateFormat fmt =
         * cal.getDateTimeFormat(DateFormat.LONG, DateFormat.LONG, loc);
         * logln("Date: " + fmt.format(cal.getTime()));
         *  // force to use other default anyway logln("\nOverride
         * registration"); cal = Calendar.getInstance(tz, loc, "Gregorian"); fmt =
         * cal.getDateTimeFormat(DateFormat.LONG, DateFormat.LONG, loc);
         * logln("Date: " + fmt.format(cal.getTime()));
         *  // unregister default logln("\nUnregistration"); logln("Unregister
         * returned: " + Calendar.unregister(key)); cal =
         * Calendar.getInstance(tz, loc, "Gregorian"); fmt =
         * cal.getDateTimeFormat(DateFormat.LONG, DateFormat.LONG, loc);
         * logln("Date: " + fmt.format(cal.getTime()));
         */
    }

    /**
     * test serialize-and-modify.
     *
     * @throws ClassNotFoundException
     */
    @Test
    public void TestSerialization3474() {
        try {
            ByteArrayOutputStream icuStream = new ByteArrayOutputStream();

            logln("icu Calendar");

            com.ibm.icu.util.GregorianCalendar icuCalendar =
                    new com.ibm.icu.util.GregorianCalendar();

            icuCalendar.setTimeInMillis(1187912555931L);
            long expectMillis = 1187912520931L; // with seconds (not ms) cleared.

            logln("instantiated: " + icuCalendar);
            logln("getMillis: " + icuCalendar.getTimeInMillis());
            icuCalendar.set(com.ibm.icu.util.GregorianCalendar.SECOND, 0);
            logln("setSecond=0: " + icuCalendar);
            {
                long gotMillis = icuCalendar.getTimeInMillis();
                if (gotMillis != expectMillis) {
                    errln("expect millis " + expectMillis + " but got " + gotMillis);
                } else {
                    logln("getMillis: " + gotMillis);
                }
            }
            ObjectOutputStream icuOut = new ObjectOutputStream(icuStream);
            icuOut.writeObject(icuCalendar);
            icuOut.flush();
            icuOut.close();

            ObjectInputStream icuIn =
                    new ObjectInputStream(new ByteArrayInputStream(icuStream.toByteArray()));
            icuCalendar = null;
            icuCalendar = (com.ibm.icu.util.GregorianCalendar) icuIn.readObject();

            logln("serialized back in: " + icuCalendar);
            {
                long gotMillis = icuCalendar.getTimeInMillis();
                if (gotMillis != expectMillis) {
                    errln("expect millis " + expectMillis + " but got " + gotMillis);
                } else {
                    logln("getMillis: " + gotMillis);
                }
            }

            icuCalendar.set(com.ibm.icu.util.GregorianCalendar.SECOND, 0);

            logln("setSecond=0: " + icuCalendar);
            {
                long gotMillis = icuCalendar.getTimeInMillis();
                if (gotMillis != expectMillis) {
                    errln(
                            "expect millis "
                                    + expectMillis
                                    + " after stream and setSecond but got "
                                    + gotMillis);
                } else {
                    logln("getMillis after stream and setSecond: " + gotMillis);
                }
            }
        } catch (IOException e) {
            errln(e.toString());
            e.printStackTrace();
        } catch (ClassNotFoundException cnf) {
            errln(cnf.toString());
            cnf.printStackTrace();
        }

        // JDK works correctly, etc etc.
        //        ByteArrayOutputStream jdkStream = new ByteArrayOutputStream();

        //        logln("\nSUN Calendar");
        //
        //        java.util.GregorianCalendar sunCalendar =
        //            new java.util.GregorianCalendar();
        //
        //        logln("instanzieren: "+sunCalendar);
        //        logln("getMillis: "+sunCalendar.getTimeInMillis());
        //        sunCalendar.set(java.util.GregorianCalendar.SECOND, 0);
        //        logln("setSecond=0: "+sunCalendar);
        //        logln("getMillis: "+sunCalendar.getTimeInMillis());
        //
        //        ObjectOutputStream sunOut =
        //            new ObjectOutputStream(jdkStream);
        //        sunOut.writeObject(sunCalendar);
        //        sunOut.flush();
        //        sunOut.close();
        //
        //        ObjectInputStream sunIn =
        //            new ObjectInputStream(new ByteArrayInputStream(jdkStream.toByteArray()));
        //        sunCalendar = null;
        //        sunCalendar = (java.util.GregorianCalendar)sunIn.readObject();
        //
        //        logln("serialized: "+sunCalendar);
        //        logln("getMillis: "+sunCalendar.getTimeInMillis());
        //
        //        sunCalendar.set(java.util.GregorianCalendar.SECOND, 0);
        //        logln("setSecond=0: "+sunCalendar);
        //        logln("getMillis: "+sunCalendar.getTimeInMillis());

    }

    @Test
    public void TestYearJump3279() {
        final long time = 1041148800000L;
        Calendar c = new GregorianCalendar();
        DateFormat fmt =
                DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US);

        c.setTimeInMillis(time);
        int year1 = c.get(Calendar.YEAR);

        logln("time: " + fmt.format(new Date(c.getTimeInMillis())));

        logln("setting DOW to " + c.getFirstDayOfWeek());
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        logln("week: " + c.getTime());
        logln("week adjust: " + fmt.format(new Date(c.getTimeInMillis())));
        int year2 = c.get(Calendar.YEAR);

        if (year1 != year2) {
            errln("Error: adjusted day of week, and year jumped from " + year1 + " to " + year2);
        } else {
            logln("Year remained " + year2 + " - PASS.");
        }
    }

    @Test
    public void TestCalendarType6816() {
        Locale loc = new Locale("en", "TH");
        Calendar cal = Calendar.getInstance(loc);
        String calType = cal.getType();
        if (!calType.equals("buddhist")) {
            errln("FAIL: Calendar type for en_TH should still be buddhist");
        }
    }

    @Test
    public void TestGetKeywordValuesForLocale() {

        final String[][] PREFERRED = {
            {"root", "gregorian"},
            {"und", "gregorian"},
            {"en_US", "gregorian"},
            {"en_029", "gregorian"},
            {"th_TH", "buddhist", "gregorian"},
            {"und_TH", "buddhist", "gregorian"},
            {"en_TH", "buddhist", "gregorian"},
            {"he_IL", "gregorian", "hebrew", "islamic", "islamic-civil", "islamic-tbla"},
            {"ar_EG", "gregorian", "coptic", "islamic", "islamic-civil", "islamic-tbla"},
            {"ja", "gregorian", "japanese"},
            {"ps_Guru_IN", "gregorian", "indian"},
            {"th@calendar=gregorian", "buddhist", "gregorian"},
            {"en@calendar=islamic", "gregorian"},
            {"zh_TW", "gregorian", "roc", "chinese"},
            {"ar_IR", "persian", "gregorian", "islamic", "islamic-civil", "islamic-tbla"},
            {"th@rg=SAZZZZ", "gregorian", "islamic-umalqura", "islamic", "islamic-rgsa"},

            // tests for ICU-22364
            {"zh_CN@rg=TW", "gregorian", "chinese"}, // invalid subdivision code
            {
                "zh_CN@rg=TWzzzz", "gregorian", "roc", "chinese",
            }, // whole region
            {
                "zh_TW@rg=TWxxxx", "gregorian", "roc", "chinese"
            }, // invalid subdivision code (ignored)
            {"zh_TW@rg=ARa", "gregorian"}, // single-letter subdivision code
            {"zh_TW@rg=AT1", "gregorian"}, // single-digit subdivision code
            {"zh_TW@rg=USca", "gregorian"}, // two-letter subdivision code
            {"zh_TW@rg=IT53", "gregorian"}, // two-digit subdivision code
            {"zh_TW@rg=AUnsw", "gregorian"}, // three-letter subdivision code
            {"zh_TW@rg=EE130", "gregorian"}, // three-digit subdivision code
        };

        String[] ALL = Calendar.getKeywordValuesForLocale("calendar", ULocale.getDefault(), false);
        HashSet<String> ALLSET = new HashSet<>();
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].equals("unknown")) {
                errln("Calendar.getKeywordValuesForLocale should not return \"unknown\"");
            }
            ALLSET.add(ALL[i]);
        }

        for (int i = 0; i < PREFERRED.length; i++) {
            ULocale loc = new ULocale(PREFERRED[i][0]);
            String[] expected = new String[PREFERRED[i].length - 1];
            System.arraycopy(PREFERRED[i], 1, expected, 0, expected.length);

            String[] pref = Calendar.getKeywordValuesForLocale("calendar", loc, true);
            boolean matchPref = false;
            if (pref.length == expected.length) {
                matchPref = true;
                for (int j = 0; j < pref.length; j++) {
                    if (!pref[j].equals(expected[j])) {
                        matchPref = false;
                    }
                }
            }
            if (!matchPref) {
                errln(
                        "FAIL: Preferred values for locale "
                                + loc
                                + " got:"
                                + Arrays.toString(pref)
                                + " expected:"
                                + Arrays.toString(expected));
            }

            String[] all = Calendar.getKeywordValuesForLocale("calendar", loc, false);
            boolean matchAll = false;
            if (all.length == ALLSET.size()) {
                matchAll = true;
                for (int j = 0; j < all.length; j++) {
                    if (!ALLSET.contains(all[j])) {
                        matchAll = false;
                        break;
                    }
                }
            }
            if (!matchAll) {
                errln("FAIL: All values for locale " + loc + " got:" + Arrays.toString(all));
            }
        }
    }

    @Test
    public void TestTimeStamp() {
        long start = 0, time;

        // Create a new Gregorian Calendar.
        Calendar cal = Calendar.getInstance(Locale.US);

        for (int i = 0; i < 20000; i++) {
            cal.set(2009, Calendar.JULY, 3, 0, 49, 46);

            time = cal.getTime().getTime();

            if (i == 0) {
                start = time;
            } else {
                if (start != time) {
                    errln("start and time not equal");
                    return;
                }
            }
        }
    }

    /*
     * Test case for add/roll with non-lenient calendar reported by ticket#8057.
     * Calendar#add may result internal fields out of valid range. ICU used to
     * trigger field range validation also for internal field changes triggered
     * by add/roll, then throws IllegalArgumentException. The field value range
     * validation should be done only for fields set by user code explicitly
     * in non-lenient mode.
     */
    @Test
    public void TestT8057() {
        // Set the calendar to the last day in a leap year
        GregorianCalendar cal = new GregorianCalendar();
        cal.setLenient(false);
        cal.clear();
        cal.set(2008, Calendar.DECEMBER, 31);

        // Force calculating then fields once.
        long t = cal.getTimeInMillis();

        long expected = 1262246400000L; // 2009-12-31 00:00 PST

        try {
            cal.add(Calendar.YEAR, 1);
            t = cal.getTimeInMillis();
            if (t != expected) {
                errln("FAIL: wrong date after add: expected=" + expected + " returned=" + t);
            }
        } catch (IllegalArgumentException e) {
            errln("FAIL: add method should not throw IllegalArgumentException");
        }
    }

    /*
     * Test case for ticket#8596.
     * Setting an year followed by getActualMaximum(Calendar.WEEK_OF_YEAR)
     * may result wrong maximum week.
     */
    @Test
    public void TestT8596() {
        GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("Etc/GMT"));
        gc.setFirstDayOfWeek(Calendar.MONDAY);
        gc.setMinimalDaysInFirstWeek(4);

        // Force the calendar to resolve the fields once.
        // The maximum week number in 2011 is 52.
        gc.set(Calendar.YEAR, 2011);
        gc.get(Calendar.YEAR);

        // Set a date in year 2009, but not calling get to resolve
        // the calendar's internal field yet.
        gc.set(2009, Calendar.JULY, 1);

        // Then call getActuamMaximum for week of year.
        // #8596 was caused by conflict between year set
        // above and internal work calendar field resolution.
        int maxWeeks = gc.getActualMaximum(Calendar.WEEK_OF_YEAR);
        if (maxWeeks != 53) {
            errln("FAIL: Max week in 2009 in ISO calendar is 53, but got " + maxWeeks);
        }
    }

    /** Test case for ticket:9019 */
    @Test
    public void Test9019() {
        GregorianCalendar cal1 = new GregorianCalendar(TimeZone.GMT_ZONE, ULocale.US);
        GregorianCalendar cal2 = new GregorianCalendar(TimeZone.GMT_ZONE, ULocale.US);
        cal1.clear();
        cal2.clear();
        cal1.set(2011, Calendar.MAY, 06);
        cal2.set(2012, Calendar.JANUARY, 06);
        cal1.setLenient(false);
        cal1.add(Calendar.MONTH, 8);
        if (!cal1.getTime().equals(cal2.getTime())) {
            errln("Error: Calendar is " + cal1.getTime() + " but expected " + cal2.getTime());
        } else {
            logln("Pass: rolled calendar is " + cal1.getTime());
        }
    }

    /**
     * Test case for ticket 9452 Calendar addition fall onto the missing date - 2011-12-30 in Samoa
     */
    @Test
    public void TestT9452() {
        TimeZone samoaTZ = TimeZone.getTimeZone("Pacific/Apia");
        GregorianCalendar cal = new GregorianCalendar(samoaTZ);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
        sdf.setTimeZone(samoaTZ);

        // Set date to 2011-12-29 00:00
        cal.clear();
        cal.set(2011, Calendar.DECEMBER, 29, 0, 0, 0);

        Date d = cal.getTime();
        String dstr = sdf.format(d);
        logln("Initial date: " + dstr);

        // Add 1 day
        cal.add(Calendar.DATE, 1);
        d = cal.getTime();
        dstr = sdf.format(d);
        logln("+1 day: " + dstr);
        assertEquals("Add 1 day", "2011-12-31T00:00:00+14:00", dstr);

        // Subtract 1 day
        cal.add(Calendar.DATE, -1);
        d = cal.getTime();
        dstr = sdf.format(d);
        logln("-1 day: " + dstr);
        assertEquals("Subtract 1 day", "2011-12-29T00:00:00-10:00", dstr);
    }

    /**
     * Test case for ticket 9403 semantic API change when attempting to call setTimeInMillis(long)
     * with a value outside the bounds. In strict mode an IllegalIcuArgumentException will be thrown
     * In lenient mode the value will be pinned to the relative min/max
     */
    @Test
    public void TestT9403() {
        Calendar myCal = Calendar.getInstance();
        long dateBit1, dateBit2, testMillis = 0L;
        boolean missedException = true;

        testMillis = -184303902611600000L;
        logln("Testing invalid setMillis value in lienent mode - using value: " + testMillis);

        try {
            myCal.setTimeInMillis(testMillis);
        } catch (IllegalArgumentException e) {
            logln("Fail: detected as bad millis");
            missedException = false;
        }
        assertTrue("Fail: out of bound millis did not trigger exception!", missedException);
        dateBit1 = myCal.get(Calendar.MILLISECOND);
        assertNotEquals("Fail: millis not changed to MIN_MILLIS", testMillis, dateBit1);

        logln("Testing invalid setMillis value in strict mode - using value: " + testMillis);
        myCal.setLenient(false);
        try {
            myCal.setTimeInMillis(testMillis);
        } catch (IllegalArgumentException e) {
            logln("Pass: correctly detected bad millis");
            missedException = false;
        }
        dateBit1 = myCal.get(Calendar.DAY_OF_MONTH);
        dateBit2 = myCal.getTimeInMillis();
        assertFalse(
                "Fail: error in setMillis, allowed invalid value : "
                        + testMillis
                        + "...returned dayOfMonth : "
                        + dateBit1
                        + " millis : "
                        + dateBit2,
                missedException);
    }

    /**
     * Test case for ticket 9968 subparse fails to return an error indication when start pos is 0
     */
    @Test
    public void TestT9968() {
        SimpleDateFormat sdf0 = new SimpleDateFormat("-MMMM");
        ParsePosition pos0 = new ParsePosition(0);
        /* Date d0 = */ sdf0.parse("-September", pos0);
        logln("sdf0: " + pos0.getErrorIndex() + "/" + pos0.getIndex());
        assertTrue("Fail: failed a good test", pos0.getErrorIndex() == -1);

        SimpleDateFormat sdf1 = new SimpleDateFormat("-MMMM");
        ParsePosition pos1 = new ParsePosition(0);
        /* Date d1 = */ sdf1.parse("-????", pos1);
        logln("sdf1: " + pos1.getErrorIndex() + "/" + pos1.getIndex());
        assertTrue("Fail: failed to detect bad parse", pos1.getErrorIndex() == 1);

        SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM");
        ParsePosition pos2 = new ParsePosition(0);
        /* Date d2 = */ sdf2.parse("????", pos2);
        logln("sdf2: " + pos2.getErrorIndex() + "/" + pos2.getIndex());
        assertTrue("Fail: failed to detect bad parse", pos2.getErrorIndex() == 0);
    }

    @Test
    public void TestWeekendData_10560() {
        final Calendar.WeekData worldWeekData = new Calendar.WeekData(2, 1, 7, 0, 1, 86400000);
        final Calendar.WeekData usWeekData = new Calendar.WeekData(1, 1, 7, 0, 1, 86400000);
        final Calendar.WeekData testWeekData = new Calendar.WeekData(1, 2, 3, 4, 5, 86400000);

        assertEquals("World", worldWeekData, Calendar.getWeekDataForRegion("001"));
        assertEquals(
                "Illegal code => world",
                Calendar.getWeekDataForRegion("001"),
                Calendar.getWeekDataForRegion("xx"));
        assertEquals(
                "FR = DE",
                Calendar.getWeekDataForRegion("FR"),
                Calendar.getWeekDataForRegion("DE"));
        assertNotEquals(
                "IN ≠ world",
                Calendar.getWeekDataForRegion("001"),
                Calendar.getWeekDataForRegion("IN"));
        assertNotEquals(
                "FR ≠ EG",
                Calendar.getWeekDataForRegion("FR"),
                Calendar.getWeekDataForRegion("EG"));

        Calendar aCalendar = Calendar.getInstance(Locale.US);
        assertEquals("US", usWeekData, aCalendar.getWeekData());
        Calendar rgusCalendar = Calendar.getInstance(new ULocale("hi_IN@rg=USzzzz"));
        assertEquals("IN@rg=US", usWeekData, rgusCalendar.getWeekData());

        aCalendar.setWeekData(testWeekData);
        assertEquals("Custom", testWeekData, aCalendar.getWeekData());
    }

    /**
     * Test case for Ticket 13080 DateFormat#getDateTimeInstance() with the desired calendar type
     * ignores the first parameter Calendar cal.
     */
    @Test
    public void TestT13080() {
        Calendar setCal = Calendar.getInstance();
        setCal.clear();
        setCal.set(Calendar.YEAR, 2017);
        setCal.set(Calendar.MONTH, 4);
        setCal.set(Calendar.DATE, 5);
        Calendar calArr[] = {
            Calendar.getInstance(),
            new IslamicCalendar(),
            new HebrewCalendar(),
            new GregorianCalendar()
        };

        String[] expectedFormat = {
            "Friday, May 5, 2017",
            "Friday, Shaʻban 8, 1438 AH",
            "Friday, 9 Iyar 5777",
            "Friday, May 5, 2017"
        };

        List<String> formattedDateList = new ArrayList<String>();
        for (Calendar cal : calArr) {
            cal.setTime(setCal.getTime());
            DateFormat format =
                    DateFormat.getDateInstance(cal, DateFormat.FULL, Locale.getDefault());
            formattedDateList.add(format.format(cal.getTime()));
        }

        String[] actualFormat = formattedDateList.toArray(new String[0]);
        assertEquals(
                "Fail: dateformat doesn't interpret calendar correctly",
                expectedFormat,
                actualFormat);
    }

    @Test
    public void TestTicket11632() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.HOUR, 596);
        // hour value set upto 596 lies within the integer range for millisecond calculations
        assertEquals(
                "Incorrect time for integer range milliseconds",
                "Sun Jan 25 20:00:00 PST 1970",
                cal.getTime().toString());
        cal.clear();
        //  hour value set above 596 lies outside the integer range for millisecond calculations.
        // This will invoke
        // the long version of the compute millis in day method in the ICU internal API
        cal.set(Calendar.HOUR, 597);
        assertEquals(
                "Incorrect time for long range milliseconds",
                "Sun Jan 25 21:00:00 PST 1970",
                cal.getTime().toString());
        cal.clear();
        cal.set(Calendar.HOUR, 597);
        cal.set(Calendar.MINUTE, 60 * 24);
        assertEquals(
                "Incorrect time for long range milliseconds",
                "Mon Jan 26 21:00:00 PST 1970",
                cal.getTime().toString());
        cal.clear();
        cal.set(Calendar.HOUR_OF_DAY, 597);
        assertEquals(
                "Incorrect time for long range milliseconds",
                "Sun Jan 25 21:00:00 PST 1970",
                cal.getTime().toString());
    }

    @Test
    public void TestPersianCalOverflow() {
        String localeID = "bs_Cyrl@calendar=persian";
        Calendar cal = Calendar.getInstance(new ULocale(localeID));
        int maxMonth = cal.getMaximum(Calendar.MONTH);
        int maxDayOfMonth = cal.getMaximum(Calendar.DATE);
        int jd, month, dayOfMonth;
        for (jd = 67023580;
                jd <= 67023584;
                jd++) { // year 178171, int32_t overflow if jd >= 67023582
            cal.clear();
            cal.set(Calendar.JULIAN_DAY, jd);
            month = cal.get(Calendar.MONTH);
            dayOfMonth = cal.get(Calendar.DATE);
            if (month > maxMonth || dayOfMonth > maxDayOfMonth) {
                errln(
                        "Error: localeID "
                                + localeID
                                + ", julianDay "
                                + jd
                                + "; maxMonth "
                                + maxMonth
                                + ", got month "
                                + month
                                + "; maxDayOfMonth "
                                + maxDayOfMonth
                                + ", got dayOfMonth "
                                + dayOfMonth);
            }
        }
    }

    @Test
    public void TestIslamicCalOverflow() {
        String localeID = "ar@calendar=islamic-civil";
        Calendar cal = Calendar.getInstance(new ULocale(localeID));
        int maxMonth = cal.getMaximum(Calendar.MONTH);
        int maxDayOfMonth = cal.getMaximum(Calendar.DATE);
        int jd, year, month, dayOfMonth;
        for (jd = 73530872;
                jd <= 73530876;
                jd++) { // year 202002, int32_t overflow if jd >= 73530874
            cal.clear();
            cal.set(Calendar.JULIAN_DAY, jd);
            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
            dayOfMonth = cal.get(Calendar.DATE);
            if (month > maxMonth || dayOfMonth > maxDayOfMonth) {
                errln(
                        "Error: localeID "
                                + localeID
                                + ", julianDay "
                                + jd
                                + "; got year "
                                + year
                                + "; maxMonth "
                                + maxMonth
                                + ", got month "
                                + month
                                + "; maxDayOfMonth "
                                + maxDayOfMonth
                                + ", got dayOfMonth "
                                + dayOfMonth);
            }
        }
    }

    @Test
    public void TestWeekOfYear13548() {
        int year = 2000;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, 4);

        int resultYear = cal.get(Calendar.YEAR);
        if (year != resultYear) {
            errln("Fail: Expected year=" + year + ", actual=" + resultYear);
        }
    }

    @Test
    public void TestTimeZoneInLocale20465() {
        String TESTS[][] = {
            {"en-u-tz-usden", "America/Denver", "gregorian"},
            {"es-u-tz-usden", "America/Denver", "gregorian"},
            {"ms-u-tz-mykul", "Asia/Kuala_Lumpur", "gregorian"},
            {"zh-u-tz-mykul", "Asia/Kuala_Lumpur", "gregorian"},
            {"fr-u-ca-buddhist-tz-phmnl", "Asia/Manila", "buddhist"},
            {"th-u-ca-chinese-tz-gblon", "Europe/London", "chinese"},
            {"de-u-ca-coptic-tz-ciabj", "Africa/Abidjan", "coptic"},
            {"ja-u-ca-dangi-tz-hkhkg", "Asia/Hong_Kong", "dangi"},
            {"da-u-ca-ethioaa-tz-ruunera", "Asia/Ust-Nera", "ethiopic-amete-alem"},
            {"ko-u-ca-ethiopic-tz-cvrai", "Atlantic/Cape_Verde", "ethiopic"},
            {"fil-u-ca-gregory-tz-aubne", "Australia/Brisbane", "gregorian"},
            {"fa-u-ca-hebrew-tz-brrbr", "America/Rio_Branco", "hebrew"},
            {"gr-u-ca-indian-tz-lccas", "America/St_Lucia", "indian"},
            {"or-u-ca-islamic-tz-cayyn", "America/Swift_Current", "islamic"},
            {"my-u-ca-islamic-umalqura-tz-kzala", "Asia/Almaty", "islamic-umalqura"},
            {"lo-u-ca-islamic-tbla-tz-bmbda", "Atlantic/Bermuda", "islamic-tbla"},
            {"km-u-ca-islamic-civil-tz-aqplm", "Antarctica/Palmer", "islamic-civil"},
            {"kk-u-ca-islamic-rgsa-tz-usanc", "America/Anchorage", "islamic"},
            {"ar-u-ca-iso8601-tz-bjptn", "Africa/Porto-Novo", "gregorian"},
            {"he-u-ca-japanese-tz-tzdar", "Africa/Dar_es_Salaam", "japanese"},
            {"bs-u-ca-persian-tz-etadd", "Africa/Addis_Ababa", "persian"},
            {"it-u-ca-roc-tz-aruaq", "America/Argentina/San_Juan", "roc"},
        };
        TimeZone other = TimeZone.getTimeZone("America/Louisville");
        for (int i = 0; i < TESTS.length; ++i) {
            ULocale ulocale = new ULocale(TESTS[i][0]);
            Locale locale = new Locale(TESTS[i][0]);
            Calendar cal = Calendar.getInstance(locale);
            assertEquals(
                    "TimeZone from Calendar.getInstance(Locale loc=\"" + TESTS[i][0] + "\")",
                    TESTS[i][1],
                    cal.getTimeZone().getID());
            assertEquals(
                    "Calendar from Calendar.getInstance(Locale loc=\"" + TESTS[i][0] + "\")",
                    TESTS[i][2],
                    cal.getType());

            cal = Calendar.getInstance(ulocale);
            assertEquals(
                    "TimeZone from Calendar.getInstance(ULocale loc=\"" + TESTS[i][0] + "\")",
                    TESTS[i][1],
                    cal.getTimeZone().getID());
            assertEquals(
                    "Calendar from Calendar.getInstance(ULocale loc=\"" + TESTS[i][0] + "\")",
                    TESTS[i][2],
                    cal.getType());

            cal = Calendar.getInstance(other, locale);
            assertEquals(
                    "TimeZone from Calendar.getInstance(TimeZone zone=\"uslui\", Locale loc=\""
                            + TESTS[i][0]
                            + "\")",
                    other.getID(),
                    cal.getTimeZone().getID());
            assertEquals(
                    "Calendar from Calendar.getInstance(TimeZone zone=\"uslui\", Locale loc=\""
                            + TESTS[i][0]
                            + "\")",
                    TESTS[i][2],
                    cal.getType());

            cal = Calendar.getInstance(other, ulocale);
            assertEquals(
                    "TimeZone from Calendar.getInstance(TimeZone zone=\"uslui\", ULocale loc=\""
                            + TESTS[i][0]
                            + "\")",
                    other.getID(),
                    cal.getTimeZone().getID());
            assertEquals(
                    "Calendar from Calendar.getInstance(TimeZone zone=\"uslui\", ULocale loc=\""
                            + TESTS[i][0]
                            + "\")",
                    TESTS[i][2],
                    cal.getType());
        }
    }

    void VerifyNoAssertWithSetGregorianChange(String timezone) {
        TimeZone zone = TimeZone.getTimeZone(timezone);
        GregorianCalendar cal = new GregorianCalendar(zone, Locale.ENGLISH);
        cal.setTime(new Date());
        // The beginning of ECMAScript time, namely -(2**53)
        long startOfTime = -9007199254740992L;

        cal.setGregorianChange(new Date(startOfTime));
        cal.get(Calendar.ZONE_OFFSET);
        cal.get(Calendar.DST_OFFSET);
    }

    @Test
    public void TestAsiaManilaAfterSetGregorianChange22043() {
        VerifyNoAssertWithSetGregorianChange("Asia/Manila");
        for (String id : TimeZone.getAvailableIDs()) {
            VerifyNoAssertWithSetGregorianChange(id);
        }
    }

    @Test
    public void TestRespectUExtensionFw() { // ICU-22226
        String[] localeIds = {
            "en-US",
            "en-US-u-fw-xyz",
            "en-US-u-fw-sun",
            "en-US-u-fw-mon",
            "en-US-u-fw-thu",
            "en-US-u-fw-sat",
            // ICU-22434
            "en-US-u-ca-iso8601-fw-sun",
            "en-US-u-ca-iso8601-fw-mon",
            "en-US-u-ca-iso8601-fw-tue",
            "en-US-u-ca-iso8601-fw-wed",
            "en-US-u-ca-iso8601-fw-thu",
            "en-US-u-ca-iso8601-fw-fri",
            "en-US-u-ca-iso8601-fw-sat",
        };
        int[] expectedValues = {
            Calendar.SUNDAY,
            Calendar.SUNDAY,
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.THURSDAY,
            Calendar.SATURDAY,
            // ICU-22434
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
        };

        assertEquals(
                "The localeIds count matches the expectedValues count",
                localeIds.length,
                expectedValues.length);

        for (int i = 0; i < localeIds.length; i++) {
            assertEquals(
                    "Calendar.getFirstDayOfWeek() does not seem to respect fw extension u in locale id "
                            + localeIds[i],
                    expectedValues[i],
                    Calendar.getInstance(Locale.forLanguageTag(localeIds[i])).getFirstDayOfWeek());
        }
    }

    @Test
    public void TestIslamicUmalquraCalendarSlow() { // ICU-22513
        Locale loc = new Locale("th@calendar=islamic-umalqura");
        Calendar cal = Calendar.getInstance(loc);
        cal.clear();
        cal.add(Calendar.YEAR, 1229080905);
        cal.roll(Calendar.WEEK_OF_MONTH, 1499050699);
        cal.fieldDifference(new Date(0), Calendar.YEAR_WOY);
    }

    @Test
    public void TestMaxActualLimitsWithoutGet23006() {
        Calendar calendar = Calendar.getInstance(new Locale("zh_zh@calendar=chinese"));
        // set day equal to 8th August 2025 in Gregorian calendar
        // this is a leap month in Chinese calendar
        GregorianCalendar gc = new GregorianCalendar(TimeZone.GMT_ZONE);
        gc.clear();
        gc.set(2025, Calendar.AUGUST, 8);
        calendar.setTimeInMillis(gc.getTimeInMillis());
        int actualMaximumBeforeCallingGet = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        assertTrue(
                "get(ERA)",
                calendar.get(Calendar.ERA) > 0); // calling get will cause to compute fields
        int actualMaximumAfterCallingGet = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        assertEquals(
                "calling getActualMaximum before/after calling get should be the same",
                actualMaximumBeforeCallingGet,
                actualMaximumAfterCallingGet);
        assertEquals(
                "calling getActualMaximum before should return 29",
                29,
                actualMaximumBeforeCallingGet);

        gc.set(2026, Calendar.AUGUST, 8);
        calendar.setTimeInMillis(gc.getTimeInMillis());
        actualMaximumBeforeCallingGet = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        assertTrue(
                "get(ERA)",
                calendar.get(Calendar.ERA) > 0); // calling get will cause to compute fields
        actualMaximumAfterCallingGet = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        assertEquals(
                "calling getActualMaximum before/after calling get should be the same",
                actualMaximumBeforeCallingGet,
                actualMaximumAfterCallingGet);
        assertEquals(
                "calling getActualMaximum before should return 30",
                30,
                actualMaximumBeforeCallingGet);
    }

    // A calendar explicitly given the standard papal cutover date must
    // compute the same instants as the default calendar, which uses that
    // same date implicitly. This is not automatic: cutoverJulianDay holds a
    // true Julian Day by default, so setGregorianChange() must convert to
    // that same unit for the two calendars to agree.
    @Test
    public void TestExplicitCutoverMatchesDefault23489() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.GMT_ZONE);

        GregorianCalendar defaultCal = new GregorianCalendar(TimeZone.GMT_ZONE);
        GregorianCalendar explicitCal = new GregorianCalendar(TimeZone.GMT_ZONE);
        explicitCal.setGregorianChange(new Date(-12219292800000L));

        for (int month = Calendar.JANUARY; month <= Calendar.DECEMBER; ++month) {
            for (int wom = 1; wom <= 5; ++wom) {
                defaultCal.setFirstDayOfWeek(Calendar.SUNDAY);
                defaultCal.setMinimalDaysInFirstWeek(1);
                defaultCal.clear();
                defaultCal.set(Calendar.YEAR, 1582);
                defaultCal.set(Calendar.MONTH, month);
                defaultCal.set(Calendar.WEEK_OF_MONTH, wom);
                Date expected = defaultCal.getTime();

                explicitCal.setFirstDayOfWeek(Calendar.SUNDAY);
                explicitCal.setMinimalDaysInFirstWeek(1);
                explicitCal.clear();
                explicitCal.set(Calendar.YEAR, 1582);
                explicitCal.set(Calendar.MONTH, month);
                explicitCal.set(Calendar.WEEK_OF_MONTH, wom);
                Date actual = explicitCal.getTime();

                assertEquals("Explicit setGregorianChange, MONTH=" + month, expected, actual);
            }
        }
    }

    // Test case for ticket 23489.
    // In the year of the Gregorian cutover, only the month that contains the
    // cutover point loses days, so only that month's weeks are shifted. Every
    // other month of that year must resolve WEEK_OF_MONTH like an ordinary
    // month, as ICU4J does.
    @Test
    public void TestWeekOfMonthInCutoverYear23489() {
        class TestData {
            int year;
            int month;
            int wom;
            int expYear;
            int expMonth;
            int expDay;

            TestData(int year, int month, int wom, int expYear, int expMonth, int expDay) {
                this.year = year;
                this.month = month;
                this.wom = wom;
                this.expYear = expYear;
                this.expMonth = expMonth;
                this.expDay = expDay;
            }
        }
        ;

        TestData[] kData = {
            // October 1582: the only month that actually loses days to the
            // cutover (October 5-14, 1582 do not exist), so it needs the
            // compensating shift that other months of the same year do not.
            // Week 1 falls entirely before the cutover point (October 15,
            // 1582), so it is expressed as a Julian calendar date; that same
            // moment in time is printed here as September 30, 1582.
            new TestData(1582, Calendar.OCTOBER, 1, 1582, Calendar.SEPTEMBER, 30),
            new TestData(1582, Calendar.OCTOBER, 2, 1582, Calendar.OCTOBER, 17),
            new TestData(1582, Calendar.OCTOBER, 3, 1582, Calendar.OCTOBER, 24),
            new TestData(1582, Calendar.OCTOBER, 4, 1582, Calendar.OCTOBER, 31),
            new TestData(1582, Calendar.OCTOBER, 5, 1582, Calendar.NOVEMBER, 7),
            // November 1582
            new TestData(1582, Calendar.NOVEMBER, 1, 1582, Calendar.OCTOBER, 31),
            new TestData(1582, Calendar.NOVEMBER, 2, 1582, Calendar.NOVEMBER, 7),
            new TestData(1582, Calendar.NOVEMBER, 3, 1582, Calendar.NOVEMBER, 14),
            new TestData(1582, Calendar.NOVEMBER, 4, 1582, Calendar.NOVEMBER, 21),
            new TestData(1582, Calendar.NOVEMBER, 5, 1582, Calendar.NOVEMBER, 28),
            // December 1582
            new TestData(1582, Calendar.DECEMBER, 1, 1582, Calendar.NOVEMBER, 28),
            new TestData(1582, Calendar.DECEMBER, 2, 1582, Calendar.DECEMBER, 5),
            new TestData(1582, Calendar.DECEMBER, 3, 1582, Calendar.DECEMBER, 12),
            new TestData(1582, Calendar.DECEMBER, 4, 1582, Calendar.DECEMBER, 19),
            new TestData(1582, Calendar.DECEMBER, 5, 1582, Calendar.DECEMBER, 26),
            // January 1583
            new TestData(1583, Calendar.JANUARY, 1, 1582, Calendar.DECEMBER, 26),
            new TestData(1583, Calendar.JANUARY, 2, 1583, Calendar.JANUARY, 2),
            new TestData(1583, Calendar.JANUARY, 3, 1583, Calendar.JANUARY, 9),
            new TestData(1583, Calendar.JANUARY, 4, 1583, Calendar.JANUARY, 16),
            new TestData(1583, Calendar.JANUARY, 5, 1583, Calendar.JANUARY, 23),
            // January 2024 (sanity check well outside the cutover year)
            new TestData(2024, Calendar.JANUARY, 1, 2023, Calendar.DECEMBER, 31),
            new TestData(2024, Calendar.JANUARY, 2, 2024, Calendar.JANUARY, 7),
            new TestData(2024, Calendar.JANUARY, 3, 2024, Calendar.JANUARY, 14),
            new TestData(2024, Calendar.JANUARY, 4, 2024, Calendar.JANUARY, 21),
            new TestData(2024, Calendar.JANUARY, 5, 2024, Calendar.JANUARY, 28),
        };

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        {
            GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
            cal.setFirstDayOfWeek(Calendar.SUNDAY);
            cal.setMinimalDaysInFirstWeek(1);

            GregorianCalendar expCal = new GregorianCalendar(TimeZone.GMT_ZONE);

            for (int i = 0; i < kData.length; ++i) {
                cal.clear();
                cal.set(Calendar.YEAR, kData[i].year);
                cal.set(Calendar.MONTH, kData[i].month);
                cal.set(Calendar.WEEK_OF_MONTH, kData[i].wom);
                Date actual = cal.getTime();

                expCal.clear();
                expCal.set(kData[i].expYear, kData[i].expMonth, kData[i].expDay);
                Date expected = expCal.getTime();

                assertEquals(
                        "year="
                                + kData[i].year
                                + ", month="
                                + (kData[i].month + 1)
                                + ", WEEK_OF_MONTH="
                                + kData[i].wom
                                + ": got "
                                + sdf.format(actual)
                                + ", expected "
                                + sdf.format(expected),
                        expected,
                        actual);
            }
        }

        // The five weeks of October 1582 must be five distinct moments in time,
        // each exactly 7 days after the previous one. This specifically catches
        // a regression where weeks 4 and 5 collapse onto the same instants as
        // weeks 2 and 3 (i.e. WEEK_OF_MONTH=4 and 2 -- and 5 and 3 -- resolving
        // to the same date).
        {
            Date previous = null;
            for (int wom = 1; wom <= 5; ++wom) {
                GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);

                cal.setFirstDayOfWeek(Calendar.SUNDAY);
                cal.setMinimalDaysInFirstWeek(1);
                cal.clear();
                cal.set(Calendar.YEAR, 1582);
                cal.set(Calendar.MONTH, Calendar.OCTOBER);
                cal.set(Calendar.WEEK_OF_MONTH, wom);
                Date current = cal.getTime();

                if (previous != null) {
                    long diffMillis = current.getTime() - previous.getTime();
                    long diffDays = diffMillis / (24L * 60 * 60 * 1000);
                    assertEquals(
                            "October 1582 WEEK_OF_MONTH="
                                    + (wom - 1)
                                    + " to WEEK_OF_MONTH="
                                    + wom
                                    + " should be exactly 7 days apart, got "
                                    + diffDays
                                    + " days",
                            7,
                            diffDays);
                }
                previous = current;
            }
        }

        // The month can be resolved from Calendar.ORDINAL_MONTH rather than
        // Calendar.MONTH. Both paths must name the same month to the cutover check,
        // and so must produce the same instant.
        {
            GregorianCalendar monthCal = new GregorianCalendar(TimeZone.GMT_ZONE);
            GregorianCalendar ordinalCal = new GregorianCalendar(TimeZone.GMT_ZONE);

            monthCal.setFirstDayOfWeek(Calendar.SUNDAY);
            monthCal.setMinimalDaysInFirstWeek(1);
            monthCal.clear();
            monthCal.set(Calendar.YEAR, 1582);
            monthCal.set(Calendar.MONTH, Calendar.NOVEMBER);
            monthCal.set(Calendar.WEEK_OF_MONTH, 3);
            Date expected = monthCal.getTime();

            ordinalCal.setFirstDayOfWeek(Calendar.SUNDAY);
            ordinalCal.setMinimalDaysInFirstWeek(1);
            ordinalCal.clear();
            ordinalCal.set(Calendar.YEAR, 1582);
            ordinalCal.set(Calendar.ORDINAL_MONTH, 10);
            ordinalCal.set(Calendar.WEEK_OF_MONTH, 3);
            Date actual = ordinalCal.getTime();

            assertEquals(
                    "YEAR=1582, ORDINAL_MONTH=10, WEEK_OF_MONTH=3: got "
                            + sdf.format(actual)
                            + ", expected "
                            + sdf.format(expected),
                    expected,
                    actual);
        }

        // Regression test: a month value outside 0..11 must not be treated as
        // the cutover month even if it would normalize into it -- the guard
        // compares the raw requested month with no range normalization.
        {
            GregorianCalendar overflowCal = new GregorianCalendar(TimeZone.GMT_ZONE);
            GregorianCalendar normalizedCal = new GregorianCalendar(TimeZone.GMT_ZONE);

            overflowCal.setFirstDayOfWeek(Calendar.SUNDAY);
            overflowCal.setMinimalDaysInFirstWeek(1);
            overflowCal.clear();
            overflowCal.set(Calendar.YEAR, 1582);
            overflowCal.set(Calendar.MONTH, 14);
            overflowCal.set(Calendar.WEEK_OF_MONTH, 1);
            Date actual = overflowCal.getTime();

            normalizedCal.setFirstDayOfWeek(Calendar.SUNDAY);
            normalizedCal.setMinimalDaysInFirstWeek(1);
            normalizedCal.clear();
            normalizedCal.set(Calendar.YEAR, 1583);
            normalizedCal.set(Calendar.MONTH, Calendar.MARCH);
            normalizedCal.set(Calendar.WEEK_OF_MONTH, 1);
            Date expected = normalizedCal.getTime();

            assertEquals(
                    "YEAR=1582, MONTH=14, WEEK_OF_MONTH=1: got "
                            + sdf.format(actual)
                            + ", expected "
                            + sdf.format(expected),
                    expected,
                    actual);
        }
    }

    // Test case for ticket 23489.
    // handleComputeJulianDay (fields -> time) numbers DAY_OF_YEAR
    // continuously across the gap in the cutover year (the missing days are
    // not counted). handleComputeFields (time -> fields) must mirror that
    // same shift, or get() and set() disagree for every day after the
    // cutover in the cutover year. WEEK_OF_YEAR, which
    // Calendar.computeWeekFields derives from DAY_OF_YEAR, breaks the same
    // way when this is missed.
    @Test
    public void TestDayOfYearRoundTripInCutoverYear23489() {
        // A pure proleptic Gregorian calendar (cutover pushed to the
        // beginning of time) used only to build the Gregorian-change
        // instants below and to locate physical Julian Days by calendar
        // date; JULIAN_DAY itself is a plain physical day count, so it means
        // the same thing on this calendar as on the calendars under test.
        GregorianCalendar proleptic = new GregorianCalendar(TimeZone.GMT_ZONE);
        proleptic.setGregorianChange(new Date(Long.MIN_VALUE));

        // Known limitation, same in ICU4C and ICU4J, tracked in ICU-3350:
        // with Monday/4, the WEEK_OF_YEAR round trip fails on January 1-2
        // after some cutover years, because the cutover year's length is
        // reported as 365 instead of its true, shorter length. Those two
        // dates are skipped for the cutovers where this happens, and nothing
        // else is.
        class Cutover {
            final int year; // the resulting gregorianCutoverYear
            final Date change;
            final HashSet<Integer> mondayFourWoySkips = new HashSet<>();

            Cutover(int year, int month, int day, boolean skipJanuary1And2After) {
                this.year = year;
                proleptic.clear();
                proleptic.set(year, month, day);
                this.change = proleptic.getTime();
                if (skipJanuary1And2After) {
                    for (int dom = 1; dom <= 2; ++dom) {
                        proleptic.clear();
                        proleptic.set(year + 1, Calendar.JANUARY, dom);
                        mondayFourWoySkips.add(proleptic.get(Calendar.JULIAN_DAY));
                    }
                }
            }
        }

        Cutover[] cutovers = {
            new Cutover(1582, Calendar.OCTOBER, 15, true), // default cutover
            new Cutover(1700, Calendar.MARCH, 1, true),
            new Cutover(1752, Calendar.SEPTEMBER, 14, false),
            new Cutover(1918, Calendar.FEBRUARY, 14, false),
        };

        int[][] weekSettings = {
            {Calendar.SUNDAY, 1},
            {Calendar.MONDAY, 4},
        };

        for (Cutover cutover : cutovers) {
            for (int[] weekSetting : weekSettings) {
                boolean isMondayFour = weekSetting[0] == Calendar.MONDAY && weekSetting[1] == 4;

                GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
                // Leave the 1582 calendar on its built-in default cutover, so that
                // the default-constructed state is what gets tested there.
                if (cutover.year != 1582) {
                    cal.setGregorianChange(cutover.change);
                }
                cal.setFirstDayOfWeek(weekSetting[0]);
                cal.setMinimalDaysInFirstWeek(weekSetting[1]);

                proleptic.clear();
                proleptic.set(cutover.year - 1, Calendar.JANUARY, 1);
                int startJd = proleptic.get(Calendar.JULIAN_DAY);
                proleptic.clear();
                proleptic.set(cutover.year + 1, Calendar.DECEMBER, 31);
                int endJd = proleptic.get(Calendar.JULIAN_DAY);

                for (int jd = startJd; jd <= endJd; ++jd) {
                    cal.clear();
                    cal.set(Calendar.JULIAN_DAY, jd);
                    int extYear = cal.get(Calendar.EXTENDED_YEAR);
                    int doy = cal.get(Calendar.DAY_OF_YEAR);
                    int yearWoy = cal.get(Calendar.YEAR_WOY);
                    int woy = cal.get(Calendar.WEEK_OF_YEAR);
                    int dow = cal.get(Calendar.DAY_OF_WEEK);

                    String label =
                            "cutover="
                                    + cutover.year
                                    + ", firstDayOfWeek="
                                    + weekSetting[0]
                                    + ", minimalDaysInFirstWeek="
                                    + weekSetting[1]
                                    + ", julianDay="
                                    + jd;

                    GregorianCalendar byDoy = (GregorianCalendar) cal.clone();
                    byDoy.clear();
                    byDoy.set(Calendar.EXTENDED_YEAR, extYear);
                    byDoy.set(Calendar.DAY_OF_YEAR, doy);
                    assertEquals(
                            label + ": EXTENDED_YEAR/DAY_OF_YEAR round trip",
                            jd,
                            byDoy.get(Calendar.JULIAN_DAY));

                    if (isMondayFour && cutover.mondayFourWoySkips.contains(jd)) {
                        continue;
                    }

                    GregorianCalendar byWoy = (GregorianCalendar) cal.clone();
                    byWoy.clear();
                    byWoy.set(Calendar.YEAR_WOY, yearWoy);
                    byWoy.set(Calendar.WEEK_OF_YEAR, woy);
                    byWoy.set(Calendar.DAY_OF_WEEK, dow);
                    assertEquals(
                            label + ": YEAR_WOY/WEEK_OF_YEAR/DAY_OF_WEEK round trip",
                            jd,
                            byWoy.get(Calendar.JULIAN_DAY));
                }
            }
        }

        // Pin absolute values too. ICU4C and ICU4J agree on these, and they are
        // also what java.util.GregorianCalendar returns for the default cutover.
        GregorianCalendar defaultCal = new GregorianCalendar(TimeZone.GMT_ZONE);
        defaultCal.setFirstDayOfWeek(Calendar.SUNDAY);
        defaultCal.setMinimalDaysInFirstWeek(1);
        defaultCal.clear();
        defaultCal.set(1582, Calendar.OCTOBER, 4);
        assertEquals("1582-10-04 DAY_OF_YEAR", 277, defaultCal.get(Calendar.DAY_OF_YEAR));

        defaultCal.clear();
        defaultCal.set(1582, Calendar.OCTOBER, 15);
        assertEquals("1582-10-15 DAY_OF_YEAR", 278, defaultCal.get(Calendar.DAY_OF_YEAR));
        assertEquals("1582-10-15 WEEK_OF_YEAR", 40, defaultCal.get(Calendar.WEEK_OF_YEAR));

        GregorianCalendar britishCal = new GregorianCalendar(TimeZone.GMT_ZONE);
        britishCal.setGregorianChange(cutovers[2].change);
        britishCal.clear();
        britishCal.set(1752, Calendar.SEPTEMBER, 14);
        assertEquals(
                "1752-09-14 DAY_OF_YEAR, 1752 cutover", 247, britishCal.get(Calendar.DAY_OF_YEAR));
    }

    // WEEK_OF_MONTH in the cutover year is numbered from the hybrid month's
    // own first day, which depends on the cutover date, the
    // first-day-of-week, and the minimal-days-in-first-week setting. This
    // covers non-1582 cutovers, both week settings, and cutovers whose gap
    // falls in a different month or exactly on a month boundary;
    // TestWeekOfMonthInCutoverYear23489 above covers the default 1582-10-15
    // cutover with the month that actually loses days. Expected values follow
    // directly from applying the definition of WEEK_OF_MONTH to the actual
    // (possibly split, truncated, or year-shifted) hybrid month.
    @Test
    public void TestWeekOfMonthNon1582Cutover3350() {
        class TestData {
            Date cutover; // null means the default 1582-10-15 cutover
            int year;
            int month;
            int firstDayOfWeek;
            int minimalDaysInFirstWeek;
            // Start (year, month, day) of WEEK_OF_MONTH 1..5.
            int[][] weekStarts;

            TestData(
                    Date cutover,
                    int year,
                    int month,
                    int firstDayOfWeek,
                    int minimalDaysInFirstWeek,
                    int[]... weekStarts) {
                this.cutover = cutover;
                this.year = year;
                this.month = month;
                this.firstDayOfWeek = firstDayOfWeek;
                this.minimalDaysInFirstWeek = minimalDaysInFirstWeek;
                this.weekStarts = weekStarts;
            }
        }

        // The cutover point is the first Gregorian day, i.e. the date passed to
        // setGregorianChange(), constructed with the default (1582) cutover
        // since 1700/1752/1918 are all ordinary Gregorian dates under it.
        GregorianCalendar cutoverBuilder = new GregorianCalendar(TimeZone.GMT_ZONE);
        cutoverBuilder.clear();
        cutoverBuilder.set(1700, Calendar.MARCH, 1);
        Date denmark = cutoverBuilder.getTime();
        cutoverBuilder.clear();
        cutoverBuilder.set(1752, Calendar.SEPTEMBER, 14);
        Date gb = cutoverBuilder.getTime();
        cutoverBuilder.clear();
        cutoverBuilder.set(1918, Calendar.FEBRUARY, 14);
        Date russia = cutoverBuilder.getTime();
        cutoverBuilder.clear();
        cutoverBuilder.set(1753, Calendar.MARCH, 1);
        Date sweden = cutoverBuilder.getTime();
        cutoverBuilder.clear();
        cutoverBuilder.set(1600, Calendar.JANUARY, 1);
        Date yearBoundary1600 = cutoverBuilder.getTime();
        cutoverBuilder.clear();
        cutoverBuilder.set(1584, Calendar.JANUARY, 5);
        Date yearBoundary1584 = cutoverBuilder.getTime();

        TestData[] kData = {
            // Default 1582-10-15 cutover: same instants as
            // TestWeekOfMonthInCutoverYear23489, checked here with both week
            // settings for cross-coverage with the general (non-1582) path.
            new TestData(
                    null,
                    1582,
                    Calendar.OCTOBER,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1582, Calendar.SEPTEMBER, 30},
                    new int[] {1582, Calendar.OCTOBER, 17},
                    new int[] {1582, Calendar.OCTOBER, 24},
                    new int[] {1582, Calendar.OCTOBER, 31},
                    new int[] {1582, Calendar.NOVEMBER, 7}),
            new TestData(
                    null,
                    1582,
                    Calendar.OCTOBER,
                    Calendar.MONDAY,
                    4,
                    new int[] {1582, Calendar.OCTOBER, 1},
                    new int[] {1582, Calendar.OCTOBER, 18},
                    new int[] {1582, Calendar.OCTOBER, 25},
                    new int[] {1582, Calendar.NOVEMBER, 1},
                    new int[] {1582, Calendar.NOVEMBER, 8}),
            // November 1582 is not affected by the cutover (control).
            new TestData(
                    null,
                    1582,
                    Calendar.NOVEMBER,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1582, Calendar.OCTOBER, 31},
                    new int[] {1582, Calendar.NOVEMBER, 7},
                    new int[] {1582, Calendar.NOVEMBER, 14},
                    new int[] {1582, Calendar.NOVEMBER, 21},
                    new int[] {1582, Calendar.NOVEMBER, 28}),
            new TestData(
                    null,
                    1582,
                    Calendar.NOVEMBER,
                    Calendar.MONDAY,
                    4,
                    new int[] {1582, Calendar.NOVEMBER, 1},
                    new int[] {1582, Calendar.NOVEMBER, 8},
                    new int[] {1582, Calendar.NOVEMBER, 15},
                    new int[] {1582, Calendar.NOVEMBER, 22},
                    new int[] {1582, Calendar.NOVEMBER, 29}),

            // March and September 1582: far enough from the cutover that every
            // week's base day stays on the same side of it, so each resolves
            // like an ordinary, single-calendar month (Julian throughout).
            // Pins the shift at zero for months of the cutover year that
            // merely share the year with the affected month.
            new TestData(
                    null,
                    1582,
                    Calendar.MARCH,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1582, Calendar.FEBRUARY, 25},
                    new int[] {1582, Calendar.MARCH, 4},
                    new int[] {1582, Calendar.MARCH, 11},
                    new int[] {1582, Calendar.MARCH, 18},
                    new int[] {1582, Calendar.MARCH, 25}),
            new TestData(
                    null,
                    1582,
                    Calendar.MARCH,
                    Calendar.MONDAY,
                    4,
                    new int[] {1582, Calendar.FEBRUARY, 26},
                    new int[] {1582, Calendar.MARCH, 5},
                    new int[] {1582, Calendar.MARCH, 12},
                    new int[] {1582, Calendar.MARCH, 19},
                    new int[] {1582, Calendar.MARCH, 26}),
            new TestData(
                    null,
                    1582,
                    Calendar.SEPTEMBER,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1582, Calendar.AUGUST, 26},
                    new int[] {1582, Calendar.SEPTEMBER, 2},
                    new int[] {1582, Calendar.SEPTEMBER, 9},
                    new int[] {1582, Calendar.SEPTEMBER, 16},
                    new int[] {1582, Calendar.SEPTEMBER, 23}),
            new TestData(
                    null,
                    1582,
                    Calendar.SEPTEMBER,
                    Calendar.MONDAY,
                    4,
                    new int[] {1582, Calendar.SEPTEMBER, 3},
                    new int[] {1582, Calendar.SEPTEMBER, 10},
                    new int[] {1582, Calendar.SEPTEMBER, 17},
                    new int[] {1582, Calendar.SEPTEMBER, 24},
                    new int[] {1582, Calendar.OCTOBER, 1}),

            // Denmark: 1700-03-01. The whole gap falls inside February, so
            // March 1700 is an intact, ordinary Gregorian month whose week 1
            // nonetheless starts in February -- and none of its weeks take a
            // shift, even though March is the month the cutover date itself
            // falls in.
            new TestData(
                    denmark,
                    1700,
                    Calendar.FEBRUARY,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1700, Calendar.JANUARY, 28},
                    new int[] {1700, Calendar.FEBRUARY, 4},
                    new int[] {1700, Calendar.FEBRUARY, 11},
                    new int[] {1700, Calendar.FEBRUARY, 18},
                    new int[] {1700, Calendar.MARCH, 7}),
            new TestData(
                    denmark,
                    1700,
                    Calendar.FEBRUARY,
                    Calendar.MONDAY,
                    4,
                    new int[] {1700, Calendar.JANUARY, 29},
                    new int[] {1700, Calendar.FEBRUARY, 5},
                    new int[] {1700, Calendar.FEBRUARY, 12},
                    new int[] {1700, Calendar.MARCH, 1},
                    new int[] {1700, Calendar.MARCH, 8}),
            new TestData(
                    denmark,
                    1700,
                    Calendar.MARCH,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1700, Calendar.FEBRUARY, 18},
                    new int[] {1700, Calendar.MARCH, 7},
                    new int[] {1700, Calendar.MARCH, 14},
                    new int[] {1700, Calendar.MARCH, 21},
                    new int[] {1700, Calendar.MARCH, 28}),
            new TestData(
                    denmark,
                    1700,
                    Calendar.MARCH,
                    Calendar.MONDAY,
                    4,
                    new int[] {1700, Calendar.MARCH, 1},
                    new int[] {1700, Calendar.MARCH, 8},
                    new int[] {1700, Calendar.MARCH, 15},
                    new int[] {1700, Calendar.MARCH, 22},
                    new int[] {1700, Calendar.MARCH, 29}),

            // Great Britain: 1752-09-14. September is split, with a gap in
            // the middle relative to a plain single-calendar month (Sep 3-13
            // do not exist): Julian Sep 1-2, then Gregorian Sep 14-30.
            new TestData(
                    gb,
                    1752,
                    Calendar.SEPTEMBER,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1752, Calendar.AUGUST, 30},
                    new int[] {1752, Calendar.SEPTEMBER, 17},
                    new int[] {1752, Calendar.SEPTEMBER, 24},
                    new int[] {1752, Calendar.OCTOBER, 1},
                    new int[] {1752, Calendar.OCTOBER, 8}),
            new TestData(
                    gb,
                    1752,
                    Calendar.SEPTEMBER,
                    Calendar.MONDAY,
                    4,
                    new int[] {1752, Calendar.AUGUST, 31},
                    new int[] {1752, Calendar.SEPTEMBER, 18},
                    new int[] {1752, Calendar.SEPTEMBER, 25},
                    new int[] {1752, Calendar.OCTOBER, 2},
                    new int[] {1752, Calendar.OCTOBER, 9}),

            // Russia: 1918-02-14. The gap falls exactly on the Julian/Gregorian
            // month boundary, so January is unaffected, but February starts on
            // the 14th instead of the 1st.
            new TestData(
                    russia,
                    1918,
                    Calendar.JANUARY,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1917, Calendar.DECEMBER, 31},
                    new int[] {1918, Calendar.JANUARY, 7},
                    new int[] {1918, Calendar.JANUARY, 14},
                    new int[] {1918, Calendar.JANUARY, 21},
                    new int[] {1918, Calendar.JANUARY, 28}),
            new TestData(
                    russia,
                    1918,
                    Calendar.JANUARY,
                    Calendar.MONDAY,
                    4,
                    new int[] {1918, Calendar.JANUARY, 1},
                    new int[] {1918, Calendar.JANUARY, 8},
                    new int[] {1918, Calendar.JANUARY, 15},
                    new int[] {1918, Calendar.JANUARY, 22},
                    new int[] {1918, Calendar.JANUARY, 29}),
            new TestData(
                    russia,
                    1918,
                    Calendar.FEBRUARY,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1918, Calendar.JANUARY, 28},
                    new int[] {1918, Calendar.FEBRUARY, 17},
                    new int[] {1918, Calendar.FEBRUARY, 24},
                    new int[] {1918, Calendar.MARCH, 3},
                    new int[] {1918, Calendar.MARCH, 10}),
            new TestData(
                    russia,
                    1918,
                    Calendar.FEBRUARY,
                    Calendar.MONDAY,
                    4,
                    new int[] {1918, Calendar.JANUARY, 29},
                    new int[] {1918, Calendar.FEBRUARY, 18},
                    new int[] {1918, Calendar.FEBRUARY, 25},
                    new int[] {1918, Calendar.MARCH, 4},
                    new int[] {1918, Calendar.MARCH, 11}),

            // Sweden: 1753-03-01, structurally identical to Denmark above.
            // February 1753 loses its last 11 days to the gap. March 1753 is
            // intact but, like Denmark's March, has a week 1 that starts in
            // February and must not take any shift.
            new TestData(
                    sweden,
                    1753,
                    Calendar.FEBRUARY,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1753, Calendar.JANUARY, 31},
                    new int[] {1753, Calendar.FEBRUARY, 7},
                    new int[] {1753, Calendar.FEBRUARY, 14},
                    new int[] {1753, Calendar.MARCH, 4},
                    new int[] {1753, Calendar.MARCH, 11}),
            new TestData(
                    sweden,
                    1753,
                    Calendar.FEBRUARY,
                    Calendar.MONDAY,
                    4,
                    new int[] {1753, Calendar.FEBRUARY, 1},
                    new int[] {1753, Calendar.FEBRUARY, 8},
                    new int[] {1753, Calendar.FEBRUARY, 15},
                    new int[] {1753, Calendar.MARCH, 5},
                    new int[] {1753, Calendar.MARCH, 12}),
            new TestData(
                    sweden,
                    1753,
                    Calendar.MARCH,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1753, Calendar.FEBRUARY, 14},
                    new int[] {1753, Calendar.MARCH, 4},
                    new int[] {1753, Calendar.MARCH, 11},
                    new int[] {1753, Calendar.MARCH, 18},
                    new int[] {1753, Calendar.MARCH, 25}),
            new TestData(
                    sweden,
                    1753,
                    Calendar.MARCH,
                    Calendar.MONDAY,
                    4,
                    new int[] {1753, Calendar.FEBRUARY, 15},
                    new int[] {1753, Calendar.MARCH, 5},
                    new int[] {1753, Calendar.MARCH, 12},
                    new int[] {1753, Calendar.MARCH, 19},
                    new int[] {1753, Calendar.MARCH, 26}),

            // Cutover 1600-01-01: the month that loses days is DECEMBER OF THE
            // PREVIOUS YEAR (1599), one extended year below gregorianCutoverYear
            // (1600), not a month of gregorianCutoverYear itself. January 1600
            // is an ordinary, unshifted month, included alongside it.
            new TestData(
                    yearBoundary1600,
                    1599,
                    Calendar.DECEMBER,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1599, Calendar.NOVEMBER, 25},
                    new int[] {1599, Calendar.DECEMBER, 2},
                    new int[] {1599, Calendar.DECEMBER, 9},
                    new int[] {1599, Calendar.DECEMBER, 16},
                    new int[] {1600, Calendar.JANUARY, 2}),
            new TestData(
                    yearBoundary1600,
                    1599,
                    Calendar.DECEMBER,
                    Calendar.MONDAY,
                    4,
                    new int[] {1599, Calendar.DECEMBER, 3},
                    new int[] {1599, Calendar.DECEMBER, 10},
                    new int[] {1599, Calendar.DECEMBER, 17},
                    new int[] {1600, Calendar.JANUARY, 3},
                    new int[] {1600, Calendar.JANUARY, 10}),
            new TestData(
                    yearBoundary1600,
                    1600,
                    Calendar.JANUARY,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1599, Calendar.DECEMBER, 16},
                    new int[] {1600, Calendar.JANUARY, 2},
                    new int[] {1600, Calendar.JANUARY, 9},
                    new int[] {1600, Calendar.JANUARY, 16},
                    new int[] {1600, Calendar.JANUARY, 23}),
            new TestData(
                    yearBoundary1600,
                    1600,
                    Calendar.JANUARY,
                    Calendar.MONDAY,
                    4,
                    new int[] {1600, Calendar.JANUARY, 3},
                    new int[] {1600, Calendar.JANUARY, 10},
                    new int[] {1600, Calendar.JANUARY, 17},
                    new int[] {1600, Calendar.JANUARY, 24},
                    new int[] {1600, Calendar.JANUARY, 31}),

            // Cutover 1584-01-05 (Julian 1583-12-26): same year-boundary shape
            // as 1600-01-01, with the cutover a few days into January instead
            // of exactly on January 1. December 1583 is Julian Dec 1-25;
            // January 1584 is Gregorian Jan 5-31.
            new TestData(
                    yearBoundary1584,
                    1583,
                    Calendar.DECEMBER,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1583, Calendar.DECEMBER, 1},
                    new int[] {1583, Calendar.DECEMBER, 8},
                    new int[] {1583, Calendar.DECEMBER, 15},
                    new int[] {1583, Calendar.DECEMBER, 22},
                    new int[] {1584, Calendar.JANUARY, 8}),
            new TestData(
                    yearBoundary1584,
                    1583,
                    Calendar.DECEMBER,
                    Calendar.MONDAY,
                    4,
                    new int[] {1583, Calendar.DECEMBER, 2},
                    new int[] {1583, Calendar.DECEMBER, 9},
                    new int[] {1583, Calendar.DECEMBER, 16},
                    new int[] {1583, Calendar.DECEMBER, 23},
                    new int[] {1584, Calendar.JANUARY, 9}),
            new TestData(
                    yearBoundary1584,
                    1584,
                    Calendar.JANUARY,
                    Calendar.SUNDAY,
                    1,
                    new int[] {1583, Calendar.DECEMBER, 22},
                    new int[] {1584, Calendar.JANUARY, 8},
                    new int[] {1584, Calendar.JANUARY, 15},
                    new int[] {1584, Calendar.JANUARY, 22},
                    new int[] {1584, Calendar.JANUARY, 29}),
            new TestData(
                    yearBoundary1584,
                    1584,
                    Calendar.JANUARY,
                    Calendar.MONDAY,
                    4,
                    new int[] {1583, Calendar.DECEMBER, 23},
                    new int[] {1584, Calendar.JANUARY, 9},
                    new int[] {1584, Calendar.JANUARY, 16},
                    new int[] {1584, Calendar.JANUARY, 23},
                    new int[] {1584, Calendar.JANUARY, 30}),
        };

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.GMT_ZONE);

        for (TestData data : kData) {
            GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
            if (data.cutover != null) {
                cal.setGregorianChange(data.cutover);
            }
            cal.setFirstDayOfWeek(data.firstDayOfWeek);
            cal.setMinimalDaysInFirstWeek(data.minimalDaysInFirstWeek);

            // The expected instants are built from the same cutover, so that
            // year/month/day are interpreted with the same hybrid labeling as
            // the calendar under test.
            GregorianCalendar expCal = new GregorianCalendar(TimeZone.GMT_ZONE);
            if (data.cutover != null) {
                expCal.setGregorianChange(data.cutover);
            }

            String rowLabel =
                    "cutover="
                            + (data.cutover == null ? "1582-10-15" : sdf.format(data.cutover))
                            + ", year="
                            + data.year
                            + ", month="
                            + (data.month + 1)
                            + ", firstDayOfWeek="
                            + data.firstDayOfWeek
                            + ", minimalDaysInFirstWeek="
                            + data.minimalDaysInFirstWeek;

            for (int wom = 1; wom <= data.weekStarts.length; ++wom) {
                cal.clear();
                cal.set(Calendar.YEAR, data.year);
                cal.set(Calendar.MONTH, data.month);
                cal.set(Calendar.WEEK_OF_MONTH, wom);
                cal.set(Calendar.DAY_OF_WEEK, data.firstDayOfWeek);
                Date actual = cal.getTime();

                int[] ymd = data.weekStarts[wom - 1];
                expCal.clear();
                expCal.set(ymd[0], ymd[1], ymd[2]);
                Date expected = expCal.getTime();

                assertEquals(
                        rowLabel
                                + ", WEEK_OF_MONTH="
                                + wom
                                + ": got "
                                + sdf.format(actual)
                                + ", expected "
                                + sdf.format(expected),
                        expected,
                        actual);

                // Also check a non-first day of week within the same week: the
                // whole week must move together with its start.
                int k = 2;
                cal.clear();
                cal.set(Calendar.YEAR, data.year);
                cal.set(Calendar.MONTH, data.month);
                cal.set(Calendar.WEEK_OF_MONTH, wom);
                cal.set(Calendar.DAY_OF_WEEK, ((data.firstDayOfWeek - 1 + k) % 7) + 1);
                Date kActual = cal.getTime();
                Date kExpected = new Date(expected.getTime() + k * 24L * 60 * 60 * 1000);

                assertEquals(
                        rowLabel
                                + ", WEEK_OF_MONTH="
                                + wom
                                + ", DAY_OF_WEEK=start+"
                                + k
                                + ": got "
                                + sdf.format(kActual)
                                + ", expected "
                                + sdf.format(kExpected),
                        kExpected,
                        kActual);
            }
        }

        // gregorianCutoverYear is derived from local time while the cutover
        // Julian day is derived from UTC, so near a year boundary they can
        // disagree; for the 1600-01-01T00:00Z cutover, gregorianCutoverYear
        // is 1599 in America/New_York instead of 1600. WEEK_OF_MONTH must
        // resolve to the same hybrid calendar day regardless: the result
        // depends only on how close the normalized month is to
        // gregorianCutoverYear, and 1600 is still within a year of 1599.
        {
            GregorianCalendar nyCal =
                    new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
            nyCal.setGregorianChange(yearBoundary1600);
            nyCal.setFirstDayOfWeek(Calendar.SUNDAY);
            nyCal.setMinimalDaysInFirstWeek(1);
            nyCal.clear();
            nyCal.set(Calendar.YEAR, 1600);
            nyCal.set(Calendar.MONTH, Calendar.JANUARY);
            nyCal.set(Calendar.WEEK_OF_MONTH, 1);
            nyCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

            String label =
                    "America/New_York, cutover=1600-01-01, YEAR=1600, MONTH=JANUARY,"
                            + " WEEK_OF_MONTH=1, SUNDAY: got "
                            + nyCal.get(Calendar.YEAR)
                            + "-"
                            + (nyCal.get(Calendar.MONTH) + 1)
                            + "-"
                            + nyCal.get(Calendar.DAY_OF_MONTH)
                            + ", expected 1599-12-16 (same hybrid day as the GMT answer)";
            assertEquals(label, 1599, nyCal.get(Calendar.YEAR));
            assertEquals(label, Calendar.DECEMBER, nyCal.get(Calendar.MONTH));
            assertEquals(label, 16, nyCal.get(Calendar.DAY_OF_MONTH));
        }

        // An out-of-range month is normalized the way handleComputeMonthStart
        // normalizes it: MONTH=-3 of YEAR=1583 is October 1582, and must
        // resolve exactly like MONTH=OCTOBER of YEAR=1582 above (default
        // cutover, first row).
        {
            GregorianCalendar overflowCal = new GregorianCalendar(TimeZone.GMT_ZONE);
            overflowCal.setFirstDayOfWeek(Calendar.SUNDAY);
            overflowCal.setMinimalDaysInFirstWeek(1);
            overflowCal.clear();
            overflowCal.set(Calendar.YEAR, 1583);
            overflowCal.set(Calendar.MONTH, -3);
            overflowCal.set(Calendar.WEEK_OF_MONTH, 1);
            overflowCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            Date actual = overflowCal.getTime();

            GregorianCalendar normalizedCal = new GregorianCalendar(TimeZone.GMT_ZONE);
            normalizedCal.setFirstDayOfWeek(Calendar.SUNDAY);
            normalizedCal.setMinimalDaysInFirstWeek(1);
            normalizedCal.clear();
            normalizedCal.set(Calendar.YEAR, 1582);
            normalizedCal.set(Calendar.MONTH, Calendar.OCTOBER);
            normalizedCal.set(Calendar.WEEK_OF_MONTH, 1);
            normalizedCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            Date expected = normalizedCal.getTime();

            assertEquals(
                    "YEAR=1583, MONTH=-3, WEEK_OF_MONTH=1: got "
                            + sdf.format(actual)
                            + ", expected "
                            + sdf.format(expected),
                    expected,
                    actual);
        }
    }

    // A MONTH value outside 0..11 must normalize into the adjacent year
    // exactly like the internal month-start computation does elsewhere:
    // YEAR=1583, MONTH=-3 is October of the previous (cutover) year.
    @Test
    public void TestWeekOfMonthOutOfRangeMonth3350() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.GMT_ZONE);

        GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
        cal.setFirstDayOfWeek(Calendar.SUNDAY);
        cal.setMinimalDaysInFirstWeek(1);
        cal.clear();
        cal.set(Calendar.YEAR, 1583);
        cal.set(Calendar.MONTH, -3);
        cal.set(Calendar.WEEK_OF_MONTH, 1);
        Date actual = cal.getTime();

        GregorianCalendar expCal = new GregorianCalendar(TimeZone.GMT_ZONE);
        expCal.setFirstDayOfWeek(Calendar.SUNDAY);
        expCal.setMinimalDaysInFirstWeek(1);
        expCal.clear();
        expCal.set(Calendar.YEAR, 1582);
        expCal.set(Calendar.MONTH, Calendar.OCTOBER);
        expCal.set(Calendar.WEEK_OF_MONTH, 1);
        Date expected = expCal.getTime();

        assertEquals(
                "YEAR=1583, MONTH=-3, WEEK_OF_MONTH=1: got "
                        + sdf.format(actual)
                        + ", expected "
                        + sdf.format(expected),
                expected,
                actual);
    }

    // WEEK_OF_MONTH must resolve the month the same way the rest of
    // handleComputeJulianDay does when MONTH/ORDINAL_MONTH is unset: via
    // getDefaultMonthInYear(), which a subclass may override, not always
    // January. JapaneseCalendar returns the era-start month, so setting only
    // ERA, YEAR, WEEK_OF_MONTH, and DAY_OF_WEEK for the first year of an era
    // must resolve within that month.
    @Test
    public void TestWeekOfMonthDefaultMonth3350() {
        GregorianCalendar cutoverBuilder = new GregorianCalendar(TimeZone.GMT_ZONE);
        cutoverBuilder.clear();
        cutoverBuilder.set(1868, Calendar.MARCH, 1);
        Date meijiCutover = cutoverBuilder.getTime();

        JapaneseCalendar explicitCutover = new JapaneseCalendar(TimeZone.GMT_ZONE);
        explicitCutover.setGregorianChange(meijiCutover);
        explicitCutover.clear();
        explicitCutover.set(Calendar.ERA, JapaneseCalendar.MEIJI);
        explicitCutover.set(Calendar.YEAR, 1);
        explicitCutover.set(Calendar.WEEK_OF_MONTH, 3);
        explicitCutover.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);

        JapaneseCalendar defaultCutover = new JapaneseCalendar(TimeZone.GMT_ZONE);
        defaultCutover.clear();
        defaultCutover.set(Calendar.ERA, JapaneseCalendar.MEIJI);
        defaultCutover.set(Calendar.YEAR, 1);
        defaultCutover.set(Calendar.WEEK_OF_MONTH, 3);
        defaultCutover.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);

        GregorianCalendar expCal = new GregorianCalendar(TimeZone.GMT_ZONE);
        expCal.clear();
        expCal.set(1868, Calendar.OCTOBER, 14);
        Date expected = expCal.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.GMT_ZONE);

        assertEquals(
                "MEIJI 1, WEEK_OF_MONTH=3, WEDNESDAY, explicit cutover 1868-03-01: got "
                        + sdf.format(explicitCutover.getTime())
                        + ", expected "
                        + sdf.format(expected),
                expected,
                explicitCutover.getTime());
        assertEquals(
                "MEIJI 1, WEEK_OF_MONTH=3, WEDNESDAY, default cutover: got "
                        + sdf.format(defaultCutover.getTime())
                        + ", expected "
                        + sdf.format(expected),
                expected,
                defaultCutover.getTime());
    }

    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;

    // Local (test-only) reproduction of the "phantom day" block algorithm
    // documented in GregorianCalendar.roll()'s WEEK_OF_MONTH case
    // (calendar-agnostic, driven only by day-of-week/day-of-month/month
    // length), to independently check that cDayOfMonth/cMonthLen are correct
    // for a hybrid month.
    private static int expectedRolledDom(
            int dom, int dow, int monthLen, int amount, int minimalDaysInFirstWeek) {
        int fdm = (dow - dom + 1) % 7;
        if (fdm < 0) fdm += 7;
        int start = ((7 - fdm) < minimalDaysInFirstWeek) ? (8 - fdm) : (1 - fdm);
        int ldm = (monthLen - dom + dow) % 7;
        int limit = monthLen + 7 - ldm;
        int gap = limit - start;
        // amount*7L: amount*7 can overflow an int for |amount| > 306783378.
        int newDom = (int) ((dom + amount * 7L - start) % gap);
        if (newDom < 0) {
            newDom += gap;
        }
        newDom += start;
        if (newDom < 1) {
            newDom = 1;
        }
        if (newDom > monthLen) {
            newDom = monthLen;
        }
        return newDom;
    }

    // roll(DAY_OF_MONTH) and roll(WEEK_OF_MONTH) within a month of the
    // cutover year (or of the year immediately before or after it) that is
    // shortened or split by an arbitrary (non-1582) cutover must cycle over
    // exactly that month's existing days, staying within the hybrid month's
    // Julian Day range. For every scenario below the resulting MONTH label
    // also never changes; that stops being guaranteed only for a very
    // early-era cutover whose repeated day labels cross a month boundary
    // (see TestRollEarlyEraCutover3350).
    @Test
    public void TestRollInCutoverMonth3350() {
        class Scenario {
            final String name;
            final boolean useDefaultCutover;
            final int cutY, cutM, cutD; // Gregorian date of the first Gregorian day
            final int year, month;
            final int firstDOM; // DAY_OF_MONTH of the hybrid month's first existing day
            final int length; // expected length, in days, of the hybrid month

            Scenario(
                    String name,
                    boolean useDefaultCutover,
                    int cutY,
                    int cutM,
                    int cutD,
                    int year,
                    int month,
                    int firstDOM,
                    int length) {
                this.name = name;
                this.useDefaultCutover = useDefaultCutover;
                this.cutY = cutY;
                this.cutM = cutM;
                this.cutD = cutD;
                this.year = year;
                this.month = month;
                this.firstDOM = firstDOM;
                this.length = length;
            }
        }

        Scenario[] scenarios = {
            new Scenario("1582-10 (default cutover)", true, 0, 0, 0, 1582, Calendar.OCTOBER, 1, 21),
            new Scenario(
                    "Denmark 1700-02",
                    false,
                    1700,
                    Calendar.MARCH,
                    1,
                    1700,
                    Calendar.FEBRUARY,
                    1,
                    18),
            new Scenario(
                    "GB 1752-09",
                    false,
                    1752,
                    Calendar.SEPTEMBER,
                    14,
                    1752,
                    Calendar.SEPTEMBER,
                    1,
                    19),
            new Scenario(
                    "Russia 1918-02",
                    false,
                    1918,
                    Calendar.FEBRUARY,
                    14,
                    1918,
                    Calendar.FEBRUARY,
                    14,
                    15),
            // The affected month is DECEMBER OF THE PREVIOUS YEAR, since the
            // cutover falls on January 1.
            new Scenario(
                    "1600-01-01 cutover: Dec 1599",
                    false,
                    1600,
                    Calendar.JANUARY,
                    1,
                    1599,
                    Calendar.DECEMBER,
                    1,
                    21),
            new Scenario(
                    "1584-01-05 cutover: Dec 1583",
                    false,
                    1584,
                    Calendar.JANUARY,
                    5,
                    1583,
                    Calendar.DECEMBER,
                    1,
                    25),
            new Scenario(
                    "1584-01-05 cutover: Jan 1584",
                    false,
                    1584,
                    Calendar.JANUARY,
                    5,
                    1584,
                    Calendar.JANUARY,
                    5,
                    27),
        };

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.GMT_ZONE);

        for (Scenario sc : scenarios) {
            GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
            if (!sc.useDefaultCutover) {
                GregorianCalendar cutoverCal = new GregorianCalendar(TimeZone.GMT_ZONE);
                cutoverCal.clear();
                cutoverCal.set(sc.cutY, sc.cutM, sc.cutD);
                cal.setGregorianChange(cutoverCal.getTime());
            }
            // Fixed (not locale-default) so that the roll(WEEK_OF_MONTH)
            // expectations below, computed for firstDayOfWeek == SUNDAY and
            // minimalDaysInFirstWeek == 1, are deterministic.
            cal.setFirstDayOfWeek(Calendar.SUNDAY);
            cal.setMinimalDaysInFirstWeek(1);
            cal.clear();
            cal.set(sc.year, sc.month, sc.firstDOM);
            long first = cal.getTimeInMillis();

            // roll(DAY_OF_MONTH, +1) from every existing day visits exactly
            // length(m) distinct days and returns to the start; it never
            // changes the YEAR or MONTH.
            for (int i = 0; i < sc.length; i++) {
                cal.setTimeInMillis(first + i * ONE_DAY_MILLIS);
                cal.roll(Calendar.DAY_OF_MONTH, 1);
                long actual = cal.getTimeInMillis();
                long expected = first + ((i + 1) % sc.length) * ONE_DAY_MILLIS;
                int rolledYear = cal.get(Calendar.EXTENDED_YEAR);
                int rolledMonth = cal.get(Calendar.MONTH);
                String label = "[" + sc.name + "]: roll(DAY_OF_MONTH,+1) from day " + i;
                assertEquals(
                        label
                                + ": got offset "
                                + ((actual - first) / ONE_DAY_MILLIS)
                                + " days, extended year "
                                + rolledYear
                                + ", month "
                                + (rolledMonth + 1)
                                + "; expected offset "
                                + ((expected - first) / ONE_DAY_MILLIS)
                                + " days",
                        expected,
                        actual);
                assertEquals(label + ": changed the year", sc.year, rolledYear);
                assertEquals(label + ": changed the month", sc.month, rolledMonth);
            }

            // roll(DAY_OF_MONTH, -1) from the first day wraps around to the
            // last day of the (hybrid) month.
            {
                cal.setTimeInMillis(first);
                cal.roll(Calendar.DAY_OF_MONTH, -1);
                long actual = cal.getTimeInMillis();
                long expected = first + (sc.length - 1) * ONE_DAY_MILLIS;
                assertEquals(
                        "["
                                + sc.name
                                + "]: roll(DAY_OF_MONTH,-1) from the first day: got "
                                + sdf.format(new Date(actual))
                                + ", expected "
                                + sdf.format(new Date(expected)),
                        expected,
                        actual);
            }

            // roll(DAY_OF_MONTH, amount) for amounts near the int range
            // boundary: amount*7 (used internally by the WEEK_OF_MONTH case)
            // overflows for |amount| > 306783378, so Integer.MAX_VALUE/
            // MIN_VALUE are exercised for both fields; the DOM computation
            // itself uses long arithmetic throughout and is not itself at
            // risk, but is checked here too since it shares the same
            // starting position.
            for (int amount : new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                cal.setTimeInMillis(first); // cDayOfMonth == 1, 0-based position 0
                cal.roll(Calendar.DAY_OF_MONTH, amount);
                long actual = cal.getTimeInMillis();
                long pos = (long) amount % sc.length;
                if (pos < 0) {
                    pos += sc.length;
                }
                long expected = first + pos * ONE_DAY_MILLIS;
                assertEquals(
                        "["
                                + sc.name
                                + "]: roll(DAY_OF_MONTH,"
                                + amount
                                + ") from day 1: got "
                                + sdf.format(new Date(actual))
                                + ", expected "
                                + sdf.format(new Date(expected)),
                        expected,
                        actual);
            }

            // roll(WEEK_OF_MONTH, amount) from several start days, for
            // several amounts (not just +-1, and including the int range
            // boundary): the exact resulting day is pinned via the
            // independent expectedRolledDom() re-implementation of the
            // documented algorithm, not just checked to have stayed in the
            // same year/month (which would pass even with cMonthLen off by
            // a couple of days).
            int[] startDomIndices = {1, sc.length / 2 + 1, sc.length};
            int[] amounts = {-2, -1, 1, 2, Integer.MAX_VALUE, Integer.MIN_VALUE};
            for (int startDom : startDomIndices) {
                for (int amount : amounts) {
                    cal.setTimeInMillis(first + (startDom - 1) * ONE_DAY_MILLIS);
                    int dowField = cal.get(Calendar.DAY_OF_WEEK);
                    int dow0 = dowField - Calendar.SUNDAY; // 0-based, 0 == firstDayOfWeek

                    cal.roll(Calendar.WEEK_OF_MONTH, amount);
                    long actual = cal.getTimeInMillis();
                    int rolledYear = cal.get(Calendar.EXTENDED_YEAR);
                    int rolledMonth = cal.get(Calendar.MONTH);

                    int expectedDom = expectedRolledDom(startDom, dow0, sc.length, amount, 1);
                    long expected = first + (expectedDom - 1) * ONE_DAY_MILLIS;

                    String label =
                            "["
                                    + sc.name
                                    + "]: roll(WEEK_OF_MONTH,"
                                    + amount
                                    + ") from day "
                                    + startDom;
                    assertEquals(
                            label
                                    + ": got "
                                    + sdf.format(new Date(actual))
                                    + " (extended year "
                                    + rolledYear
                                    + ", month "
                                    + (rolledMonth + 1)
                                    + "), expected "
                                    + sdf.format(new Date(expected)),
                            expected,
                            actual);
                    assertEquals(label + ": changed the year", sc.year, rolledYear);
                    assertEquals(label + ": changed the month", sc.month, rolledMonth);
                }
            }
        }

        // [ICU-3350 code review] set(JULIAN_DAY) alone (without complete()) left DAY_OF_MONTH and
        // other date fields stale, so a subsequent set() of a different field, with no intervening
        // get(), silently discarded the preceding roll(). Default cutover, GMT, SUNDAY/1 week
        // settings.
        {
            GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
            cal.setFirstDayOfWeek(Calendar.SUNDAY);
            cal.setMinimalDaysInFirstWeek(1);

            cal.clear();
            cal.set(1582, Calendar.OCTOBER, 20);
            cal.getTime();
            cal.roll(Calendar.DATE, 1);
            cal.set(Calendar.MONTH, Calendar.OCTOBER);
            assertEquals(
                    "roll(DATE,+1) then set(MONTH,OCTOBER) from 1582-10-20",
                    21,
                    cal.get(Calendar.DATE));

            cal.clear();
            cal.set(1582, Calendar.OCTOBER, 20);
            cal.getTime();
            cal.roll(Calendar.WEEK_OF_MONTH, 1);
            cal.set(Calendar.MONTH, Calendar.OCTOBER);
            assertEquals(
                    "roll(WEEK_OF_MONTH,+1) then set(MONTH,OCTOBER) from 1582-10-20",
                    27,
                    cal.get(Calendar.DATE));
        }
    }

    // [ICU-3350 code review] GregorianCalendar.roll(field, 0) must behave exactly like it did
    // before this class grew cutover-month handling for DAY_OF_MONTH/WEEK_OF_MONTH: a pure no-op
    // for every field, identical to the base Calendar.roll(field, 0). Java-only: ICU4C's
    // GregorianCalendar::roll() already returns immediately for amount == 0 (pre-existing, not
    // touched by this change), so it has no equivalent regression to guard against.
    @Test
    public void TestRollZeroAmount3350() {
        // Lenient: a pending out-of-range DAY_OF_MONTH must survive roll(HOUR, 0) untouched, then
        // resolve normally against the field set afterward.
        GregorianCalendar c = new GregorianCalendar(TimeZone.GMT_ZONE);
        c.clear();
        c.set(2023, Calendar.FEBRUARY, 1);
        c.getTime();
        c.set(Calendar.DAY_OF_MONTH, 31);
        c.roll(Calendar.HOUR, 0);
        c.set(Calendar.MONTH, Calendar.MARCH);
        assertEquals(
                "roll(HOUR,0) must not resolve a pending DATE=31 via February",
                31,
                c.get(Calendar.DATE));
        assertEquals(
                "roll(HOUR,0) must not resolve a pending DATE=31 via February",
                Calendar.MARCH,
                c.get(Calendar.MONTH));

        // Non-lenient: roll(MINUTE, 0) on a calendar with a pending invalid field must not validate
        // (and thus must not throw), matching the base Calendar.roll(field, 0)'s early return.
        GregorianCalendar d = new GregorianCalendar(TimeZone.GMT_ZONE);
        d.setLenient(false);
        d.clear();
        d.set(2023, Calendar.FEBRUARY, 1);
        d.getTime();
        d.set(Calendar.DAY_OF_MONTH, 31);
        d.roll(Calendar.MINUTE, 0);

        // [ICU-3350 code review] In an affected month (default cutover, October 1582),
        // roll(DATE, 0) and roll(WEEK_OF_MONTH, 0) must be pure no-ops too: they must not run the
        // cutover-month logic (which calls set(JULIAN_DAY) + complete()) just because amount == 0,
        // matching the "if (amount != 0)" skip around the cutover-month detection in roll(). A
        // pending, out-of-range DAY_OF_MONTH (October 1582 has no 5-14) must survive both rolls
        // untouched, then resolve normally against the field set afterward.
        GregorianCalendar e = new GregorianCalendar(TimeZone.GMT_ZONE);
        e.clear();
        e.set(1582, Calendar.OCTOBER, 1);
        e.getTime();
        e.set(Calendar.DAY_OF_MONTH, 10);
        e.roll(Calendar.DATE, 0);
        e.set(Calendar.MONTH, Calendar.NOVEMBER);
        assertEquals(
                "roll(DATE,0) in October 1582 must not resolve a pending DATE=10",
                10,
                e.get(Calendar.DATE));
        assertEquals(
                "roll(DATE,0) in October 1582 must not resolve a pending DATE=10",
                Calendar.NOVEMBER,
                e.get(Calendar.MONTH));

        GregorianCalendar f = new GregorianCalendar(TimeZone.GMT_ZONE);
        f.clear();
        f.set(1582, Calendar.OCTOBER, 1);
        f.getTime();
        f.set(Calendar.DAY_OF_MONTH, 10);
        f.roll(Calendar.WEEK_OF_MONTH, 0);
        f.set(Calendar.MONTH, Calendar.NOVEMBER);
        assertEquals(
                "roll(WEEK_OF_MONTH,0) in October 1582 must not resolve a pending DATE=10",
                10,
                f.get(Calendar.DATE));
        assertEquals(
                "roll(WEEK_OF_MONTH,0) in October 1582 must not resolve a pending DATE=10",
                Calendar.NOVEMBER,
                f.get(Calendar.MONTH));

        // Non-lenient: roll(DATE, 0) / roll(WEEK_OF_MONTH, 0) in the same affected month, with a
        // pending invalid field, must not validate (and thus must not throw).
        GregorianCalendar g = new GregorianCalendar(TimeZone.GMT_ZONE);
        g.setLenient(false);
        g.clear();
        g.set(1582, Calendar.OCTOBER, 1);
        g.getTime();
        g.set(Calendar.DAY_OF_MONTH, 32);
        g.roll(Calendar.DATE, 0);

        GregorianCalendar h = new GregorianCalendar(TimeZone.GMT_ZONE);
        h.setLenient(false);
        h.clear();
        h.set(1582, Calendar.OCTOBER, 1);
        h.getTime();
        h.set(Calendar.DAY_OF_MONTH, 32);
        h.roll(Calendar.WEEK_OF_MONTH, 0);
    }

    // roll(DAY_OF_MONTH) and roll(WEEK_OF_MONTH) in a month shortened or
    // split by the cutover must keep local wall time invariant across a DST
    // transition, and must resolve a repeated or skipped wall time exactly
    // like Calendar.roll() does for an ordinary month: honoring
    // getRepeatedWallTimeOption()/getSkippedWallTimeOption().
    @Test
    public void TestRollDstAcrossCutoverMonth3350() {
        // America/New_York, cutover Gregorian 2000-06-01T00:00Z: March 2000 is
        // entirely before the cutover, but an intact, ordinary Julian month
        // (not shortened or split), so this rolls via plain Calendar.roll() --
        // a control confirming the setup is DST-safe to begin with.
        {
            GregorianCalendar cutoverCal =
                    new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
            cutoverCal.clear();
            cutoverCal.set(2000, Calendar.JUNE, 1);
            Date cutoverMillis = cutoverCal.getTime();

            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
            cal.setGregorianChange(cutoverMillis);

            class Check {
                final int startMonth, startDay, startHour, startMinute;
                final int field, amount;
                final int expMonth, expDay, expHour, expMinute;

                Check(
                        int startMonth,
                        int startDay,
                        int startHour,
                        int startMinute,
                        int field,
                        int amount,
                        int expMonth,
                        int expDay,
                        int expHour,
                        int expMinute) {
                    this.startMonth = startMonth;
                    this.startDay = startDay;
                    this.startHour = startHour;
                    this.startMinute = startMinute;
                    this.field = field;
                    this.amount = amount;
                    this.expMonth = expMonth;
                    this.expDay = expDay;
                    this.expHour = expHour;
                    this.expMinute = expMinute;
                }
            }

            // DST begins 2000-04-02 02:00 local (spring forward).
            Check[] checks = {
                new Check(Calendar.MARCH, 25, 0, 30, Calendar.DATE, -24, Calendar.MARCH, 1, 0, 30),
                new Check(Calendar.MARCH, 1, 23, 30, Calendar.DATE, 25, Calendar.MARCH, 26, 23, 30),
                new Check(
                        Calendar.MARCH,
                        4,
                        0,
                        30,
                        Calendar.WEEK_OF_MONTH,
                        3,
                        Calendar.MARCH,
                        25,
                        0,
                        30),
            };
            for (Check c : checks) {
                cal.clear();
                cal.set(2000, c.startMonth, c.startDay, c.startHour, c.startMinute);
                cal.roll(c.field, c.amount);
                String label =
                        "America/New_York, 2000-"
                                + (c.startMonth + 1)
                                + "-"
                                + c.startDay
                                + " "
                                + c.startHour
                                + ":"
                                + c.startMinute
                                + ", roll("
                                + c.field
                                + ","
                                + c.amount
                                + ")";
                assertEquals(label + ": month", c.expMonth, cal.get(Calendar.MONTH));
                assertEquals(label + ": day", c.expDay, cal.get(Calendar.DATE));
                assertEquals(label + ": hour", c.expHour, cal.get(Calendar.HOUR_OF_DAY));
                assertEquals(label + ": minute", c.expMinute, cal.get(Calendar.MINUTE));
            }
        }

        // Default 1582-10-15 cutover, with a zone whose DST starts the 2nd
        // Sunday of March: March 1582 is not affected by the cutover (far from
        // it), so this is again a plain Calendar.roll() control, with a zone
        // whose DST rules apply even to a date this old.
        {
            int oneHourMs = 60 * 60 * 1000;
            SimpleTimeZone stz =
                    new SimpleTimeZone(
                            -5 * oneHourMs,
                            "StaticDstTest",
                            Calendar.MARCH,
                            8,
                            -Calendar.SUNDAY,
                            2 * oneHourMs,
                            Calendar.NOVEMBER,
                            1,
                            -Calendar.SUNDAY,
                            2 * oneHourMs);
            GregorianCalendar cal = new GregorianCalendar(stz); // default 1582-10-15 cutover
            cal.clear();
            cal.set(1582, Calendar.MARCH, 25, 0, 30);
            cal.roll(Calendar.DATE, -24);

            String label = "default cutover, 1582-03-25 00:30, roll(DATE,-24)";
            assertEquals(label + ": year", 1582, cal.get(Calendar.YEAR));
            assertEquals(label + ": month", Calendar.MARCH, cal.get(Calendar.MONTH));
            assertEquals(label + ": day", 1, cal.get(Calendar.DATE));
            assertEquals(label + ": hour", 0, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(label + ": minute", 30, cal.get(Calendar.MINUTE));
        }

        // Default 1582-10-15 cutover, with a zone whose DST transition falls
        // inside October 1582 itself (the split cutover month, Oct 1..4 and
        // Oct 15..31): rolling across the transition must still preserve local
        // wall time and land on the correct day of the (21-day) hybrid month.
        {
            int oneHourMs = 60 * 60 * 1000;
            SimpleTimeZone stz =
                    new SimpleTimeZone(
                            -5 * oneHourMs,
                            "SplitMonthDstTest",
                            Calendar.OCTOBER,
                            3,
                            Calendar.THURSDAY,
                            2 * oneHourMs,
                            Calendar.DECEMBER,
                            1,
                            Calendar.THURSDAY,
                            2 * oneHourMs);
            GregorianCalendar cal = new GregorianCalendar(stz); // default 1582-10-15 cutover
            cal.clear();
            cal.set(1582, Calendar.OCTOBER, 31, 0, 30);
            cal.roll(Calendar.DATE, -20); // day 21 of 21 -> day 1

            String label = "default cutover, 1582-10-31 00:30, roll(DATE,-20)";
            assertEquals(label + ": year", 1582, cal.get(Calendar.YEAR));
            assertEquals(label + ": month", Calendar.OCTOBER, cal.get(Calendar.MONTH));
            assertEquals(label + ": day", 1, cal.get(Calendar.DATE));
            assertEquals(label + ": hour", 0, cal.get(Calendar.HOUR_OF_DAY));
            assertEquals(label + ": minute", 30, cal.get(Calendar.MINUTE));
        }

        // Default 1582-10-15 cutover, rolling across a REPEATED wall time
        // (fall-back) within the split October 1582 month: DOM and WOM, under
        // both WALLTIME_LAST and WALLTIME_FIRST, compared against the
        // identical roll on an ordinary month (1600) in the same zone.
        {
            int oneHourMs = 60 * 60 * 1000;
            for (int rep : new int[] {Calendar.WALLTIME_LAST, Calendar.WALLTIME_FIRST}) {
                SimpleTimeZone stzHybrid =
                        new SimpleTimeZone(
                                -5 * oneHourMs,
                                "RepeatedWallTimeTest",
                                Calendar.MARCH,
                                8,
                                -Calendar.SUNDAY,
                                2 * oneHourMs,
                                Calendar.OCTOBER,
                                15,
                                -Calendar.THURSDAY,
                                2 * oneHourMs);
                GregorianCalendar hybrid =
                        new GregorianCalendar(stzHybrid); // default 1582-10-15 cutover
                hybrid.setRepeatedWallTimeOption(rep);
                hybrid.setFirstDayOfWeek(Calendar.SUNDAY);
                hybrid.setMinimalDaysInFirstWeek(1);

                SimpleTimeZone stzOrdinary =
                        new SimpleTimeZone(
                                -5 * oneHourMs,
                                "RepeatedWallTimeTest",
                                Calendar.MARCH,
                                8,
                                -Calendar.SUNDAY,
                                2 * oneHourMs,
                                Calendar.OCTOBER,
                                15,
                                -Calendar.THURSDAY,
                                2 * oneHourMs);
                GregorianCalendar ordinary = new GregorianCalendar(stzOrdinary);
                ordinary.setRepeatedWallTimeOption(rep);
                ordinary.setFirstDayOfWeek(Calendar.SUNDAY);
                ordinary.setMinimalDaysInFirstWeek(1);

                class Check {
                    final String name;
                    final int startDay;
                    final int field;
                    final int amount;
                    final int expDay;
                    final int expOffsetHours; // combined zone+DST offset, in hours

                    Check(
                            String name,
                            int startDay,
                            int field,
                            int amount,
                            int expDay,
                            int expOffsetHours) {
                        this.name = name;
                        this.startDay = startDay;
                        this.field = field;
                        this.amount = amount;
                        this.expDay = expDay;
                        this.expOffsetHours = expOffsetHours;
                    }
                }

                // 1582-10-17 (unambiguous) -> roll to 1582-10-21, which falls
                // in the repeated hour (DST ends "the Thursday on or before
                // Oct 15", computed per-year, landing on Oct 21 for 1582 and
                // Oct 19 for the ordinary month's year 1600).
                int expOffset = rep == Calendar.WALLTIME_LAST ? -5 : -4;
                Check[] checks = {
                    new Check("DOM", 17, Calendar.DATE, 4, 21, expOffset),
                    new Check("WOM", 28, Calendar.WEEK_OF_MONTH, -1, 21, expOffset),
                };
                for (Check c : checks) {
                    hybrid.clear();
                    hybrid.set(1582, Calendar.OCTOBER, c.startDay, 1, 30);
                    hybrid.roll(c.field, c.amount);
                    int hDay = hybrid.get(Calendar.DATE);
                    int hHour = hybrid.get(Calendar.HOUR_OF_DAY);
                    int hMinute = hybrid.get(Calendar.MINUTE);
                    int hOffset =
                            (hybrid.get(Calendar.ZONE_OFFSET) + hybrid.get(Calendar.DST_OFFSET))
                                    / oneHourMs;

                    int ordStartDay = c.startDay - 2; // 1600's transition falls 2 days earlier
                    ordinary.clear();
                    ordinary.set(1600, Calendar.OCTOBER, ordStartDay, 1, 30);
                    ordinary.roll(c.field, c.amount);
                    int oHour = ordinary.get(Calendar.HOUR_OF_DAY);
                    int oMinute = ordinary.get(Calendar.MINUTE);
                    int oOffset =
                            (ordinary.get(Calendar.ZONE_OFFSET) + ordinary.get(Calendar.DST_OFFSET))
                                    / oneHourMs;

                    String repName = rep == Calendar.WALLTIME_LAST ? "LAST" : "FIRST";
                    String label = "repeated wall time, rep=" + repName + ", " + c.name;
                    assertEquals(label + ": day", c.expDay, hDay);
                    assertEquals(label + ": hour", 1, hHour);
                    assertEquals(label + ": minute", 30, hMinute);
                    assertEquals(label + ": offset", c.expOffsetHours, hOffset);
                    // The 1582 and 1600 transitions fall on different absolute
                    // days (offset by 2, since the rule is computed per year),
                    // so only wall-clock time and offset -- not the day number
                    // -- must match between the hybrid and ordinary months.
                    assertEquals(label + ": hybrid vs ordinary hour", oHour, hHour);
                    assertEquals(label + ": hybrid vs ordinary minute", oMinute, hMinute);
                    assertEquals(label + ": hybrid vs ordinary offset", oOffset, hOffset);
                }
            }
        }

        // Default 1582-10-15 cutover, rolling across a SKIPPED wall time
        // (spring-forward) within the split October 1582 month: DOM and WOM,
        // under all three skipped-wall-time options, compared against the
        // identical roll on an ordinary month (1600) in the same zone.
        {
            int oneHourMs = 60 * 60 * 1000;
            for (int sk :
                    new int[] {
                        Calendar.WALLTIME_LAST,
                        Calendar.WALLTIME_FIRST,
                        Calendar.WALLTIME_NEXT_VALID
                    }) {
                SimpleTimeZone stzHybrid =
                        new SimpleTimeZone(
                                -5 * oneHourMs,
                                "SkippedWallTimeTest",
                                Calendar.OCTOBER,
                                15,
                                -Calendar.THURSDAY,
                                2 * oneHourMs,
                                Calendar.DECEMBER,
                                1,
                                -Calendar.SUNDAY,
                                2 * oneHourMs);
                GregorianCalendar hybrid =
                        new GregorianCalendar(stzHybrid); // default 1582-10-15 cutover
                hybrid.setSkippedWallTimeOption(sk);
                hybrid.setFirstDayOfWeek(Calendar.SUNDAY);
                hybrid.setMinimalDaysInFirstWeek(1);

                SimpleTimeZone stzOrdinary =
                        new SimpleTimeZone(
                                -5 * oneHourMs,
                                "SkippedWallTimeTest",
                                Calendar.OCTOBER,
                                15,
                                -Calendar.THURSDAY,
                                2 * oneHourMs,
                                Calendar.DECEMBER,
                                1,
                                -Calendar.SUNDAY,
                                2 * oneHourMs);
                GregorianCalendar ordinary = new GregorianCalendar(stzOrdinary);
                ordinary.setSkippedWallTimeOption(sk);
                ordinary.setFirstDayOfWeek(Calendar.SUNDAY);
                ordinary.setMinimalDaysInFirstWeek(1);

                int expHour, expMinute, expOffsetHours;
                switch (sk) {
                    case Calendar.WALLTIME_LAST:
                        expHour = 3;
                        expMinute = 30;
                        expOffsetHours = -4;
                        break;
                    case Calendar.WALLTIME_FIRST:
                        expHour = 1;
                        expMinute = 30;
                        expOffsetHours = -5;
                        break;
                    default: // NEXT_VALID
                        expHour = 3;
                        expMinute = 0;
                        expOffsetHours = -4;
                        break;
                }

                class Check {
                    final String name;
                    final int startDay;
                    final int field;
                    final int amount;

                    Check(String name, int startDay, int field, int amount) {
                        this.name = name;
                        this.startDay = startDay;
                        this.field = field;
                        this.amount = amount;
                    }
                }

                // The 2:00-3:00 wall-clock hour on 1582-10-21 (1600-10-19 for
                // the ordinary month) does not exist.
                Check[] checks = {
                    new Check("DOM", 25, Calendar.DATE, -4),
                    new Check("WOM", 28, Calendar.WEEK_OF_MONTH, -1),
                };
                for (Check c : checks) {
                    hybrid.clear();
                    hybrid.set(1582, Calendar.OCTOBER, c.startDay, 2, 30);
                    hybrid.roll(c.field, c.amount);
                    int hDay = hybrid.get(Calendar.DATE);
                    int hHour = hybrid.get(Calendar.HOUR_OF_DAY);
                    int hMinute = hybrid.get(Calendar.MINUTE);
                    int hOffset =
                            (hybrid.get(Calendar.ZONE_OFFSET) + hybrid.get(Calendar.DST_OFFSET))
                                    / oneHourMs;

                    ordinary.clear();
                    ordinary.set(1600, Calendar.OCTOBER, c.startDay - 2, 2, 30);
                    ordinary.roll(c.field, c.amount);
                    int oHour = ordinary.get(Calendar.HOUR_OF_DAY);
                    int oMinute = ordinary.get(Calendar.MINUTE);
                    int oOffset =
                            (ordinary.get(Calendar.ZONE_OFFSET) + ordinary.get(Calendar.DST_OFFSET))
                                    / oneHourMs;

                    String skName =
                            sk == Calendar.WALLTIME_LAST
                                    ? "LAST"
                                    : (sk == Calendar.WALLTIME_FIRST ? "FIRST" : "NEXT_VALID");
                    String label = "skipped wall time, sk=" + skName + ", " + c.name;
                    assertEquals(label + ": day", 21, hDay);
                    assertEquals(label + ": hour", expHour, hHour);
                    assertEquals(label + ": minute", expMinute, hMinute);
                    assertEquals(label + ": offset", expOffsetHours, hOffset);
                    // (See the repeated-wall-time block above: only wall-clock
                    // time and offset need to match, not the day number.)
                    assertEquals(label + ": hybrid vs ordinary hour", oHour, hHour);
                    assertEquals(label + ": hybrid vs ordinary minute", oMinute, hMinute);
                    assertEquals(label + ": hybrid vs ordinary offset", oOffset, hOffset);
                }
            }
        }
    }

    // Two eras where a cutover does not remove days: a cutover year where the
    // Julian and Gregorian calendars agree on the day count but disagree on
    // leap status (a non-400 century year is a Julian leap year but not a
    // Gregorian one, so an intact Julian February must still roll correctly
    // rather than fall back to Calendar.roll(), which would use the wrong,
    // Gregorian-reported length); and an era where the cutover repeats day
    // labels instead of removing them, giving a month whose hybrid range spans
    // more days than either calendar's own version of that month (year 50's
    // hybrid March is 33 consecutive Julian Days, with some Julian and
    // Gregorian labels repeating). A third case below shows that this label
    // repeat can straddle a month boundary: roll() then moves the MONTH field
    // even though it stays within the hybrid month's own Julian Day range.
    @Test
    public void TestRollEarlyEraCutover3350() {
        class Scenario {
            final String name;
            final int cutY, cutM, cutD; // Gregorian date of the first Gregorian day
            final int firstJD; // Julian Day of the hybrid month's first day
            final int length; // length, in days, of the hybrid month

            Scenario(String name, int cutY, int cutM, int cutD, int firstJD, int length) {
                this.name = name;
                this.cutY = cutY;
                this.cutM = cutM;
                this.cutD = cutD;
                this.firstJD = firstJD;
                this.length = length;
            }
        }

        Scenario[] scenarios = {
            // Hybrid March 50 = Julian Mar 1, Mar 2, Gregorian Mar 1..31: 33
            // consecutive (but label-repeating) Julian Days.
            new Scenario(
                    "year 50 cutover (Gregorian 0050-03-01)", 50, Calendar.MARCH, 1, 1739380, 33),
            // Hybrid February -100 is an intact, pure Julian February (the
            // cutover is in June): 29 days, since -100 is a Julian leap year.
            new Scenario(
                    "year -100 cutover (Gregorian -100-06-01)",
                    -100,
                    Calendar.JUNE,
                    1,
                    1684564,
                    29),
        };

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.GMT_ZONE);

        for (Scenario sc : scenarios) {
            // A *default*-cutover calendar would misinterpret set(cutY, cutM,
            // cutD) as Julian for these (pre-1582) years, giving the wrong
            // instant; force pure-Gregorian interpretation instead with an
            // extreme (very early) cutover of its own.
            GregorianCalendar cutoverCal = new GregorianCalendar(TimeZone.GMT_ZONE);
            cutoverCal.setGregorianChange(new Date(Long.MIN_VALUE));
            cutoverCal.clear();
            cutoverCal.set(sc.cutY, sc.cutM, sc.cutD);
            Date cutoverMillis = cutoverCal.getTime();

            GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
            cal.setGregorianChange(cutoverMillis);

            cal.clear();
            cal.set(Calendar.JULIAN_DAY, sc.firstJD);
            long first = cal.getTimeInMillis();

            // roll(DAY_OF_MONTH, +1) from every existing day visits exactly
            // length(m) distinct, consecutive Julian Days and returns to the
            // start.
            for (int i = 0; i < sc.length; i++) {
                cal.setTimeInMillis(first + i * ONE_DAY_MILLIS);
                cal.roll(Calendar.DAY_OF_MONTH, 1);
                long actual = cal.getTimeInMillis();
                long expected = first + ((i + 1) % sc.length) * ONE_DAY_MILLIS;
                assertEquals(
                        "["
                                + sc.name
                                + "]: roll(DAY_OF_MONTH,+1) from JD "
                                + (sc.firstJD + i)
                                + ": got "
                                + sdf.format(new Date(actual))
                                + ", expected "
                                + sdf.format(new Date(expected)),
                        expected,
                        actual);
            }
        }

        // Specific check for year 50: rolling +1 day from the instant labelled
        // Gregorian March 9 (JD 1739390, offset 10 into the 33-day hybrid
        // month) lands on the instant labelled Gregorian March 10 (JD 1739391).
        {
            GregorianCalendar cutoverCal = new GregorianCalendar(TimeZone.GMT_ZONE);
            cutoverCal.setGregorianChange(new Date(Long.MIN_VALUE));
            cutoverCal.clear();
            cutoverCal.set(50, Calendar.MARCH, 1);
            Date cutoverMillis = cutoverCal.getTime();

            GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
            cal.setGregorianChange(cutoverMillis);
            cal.clear();
            cal.set(Calendar.JULIAN_DAY, 1739390); // Gregorian March 9, 50
            long before = cal.getTimeInMillis();

            cal.roll(Calendar.DAY_OF_MONTH, 1);
            long after = cal.getTimeInMillis();

            long expected = before + ONE_DAY_MILLIS;
            assertEquals(
                    "year 50 cutover, G Mar 9: roll(DAY_OF_MONTH,+1) from "
                            + sdf.format(new Date(before))
                            + " got "
                            + sdf.format(new Date(after))
                            + ", expected "
                            + sdf.format(new Date(expected)),
                    expected,
                    after);
        }

        // The cutover need not fall exactly on a month boundary: with the
        // cutover one day earlier (Gregorian 0050-02-28), the instant
        // labelled 0050-03-01 in the hybrid calendar is the LAST JULIAN day
        // (the repeated Gregorian instant, two days later, is also labelled
        // 0050-03-01). Rolling DAY_OF_MONTH by +1 from there stays within
        // hybrid March's own Julian Day range (see computeHybridMonth() in
        // GregorianCalendar), but that range extends one Julian Day into
        // what is labelled February, so the result is 0050-02-28: an
        // early-era repeat that crosses a month boundary. This is
        // documented, expected behaviour: for a cutover early enough that
        // the switch repeats day labels (roughly before 200 AD),
        // roll(DAY_OF_MONTH)/roll(WEEK_OF_MONTH) around the cutover may
        // land on a day labelled with an adjacent month or year, or skip
        // days; this test pins this specific case as current behavior, not
        // a general guarantee that roll() stays within the hybrid month's
        // own JD range.
        {
            GregorianCalendar cutoverCal = new GregorianCalendar(TimeZone.GMT_ZONE);
            cutoverCal.setGregorianChange(new Date(Long.MIN_VALUE));
            cutoverCal.clear();
            cutoverCal.set(50, Calendar.FEBRUARY, 28);
            Date cutoverMillis = cutoverCal.getTime();

            GregorianCalendar cal = new GregorianCalendar(TimeZone.GMT_ZONE);
            cal.setGregorianChange(cutoverMillis);
            cal.clear();
            cal.set(Calendar.JULIAN_DAY, 1739380); // hybrid-labelled 0050-03-01 (last Julian day)

            cal.roll(Calendar.DATE, 1);

            int year = cal.get(Calendar.EXTENDED_YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DATE);
            int jd = cal.get(Calendar.JULIAN_DAY);
            String label =
                    "year 50 cutover Gregorian 0050-02-28, roll(DATE,+1) from the instant labelled"
                            + " 0050-03-01 (JD 1739380)";
            assertEquals(label + ": year", 50, year);
            assertEquals(label + ": month", Calendar.FEBRUARY, month);
            assertEquals(label + ": day", 28, day);
            assertEquals(label + ": JD", 1739381, jd);
        }
    }
}
// eof
