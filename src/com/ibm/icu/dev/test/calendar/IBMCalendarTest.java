/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import java.util.Date;
import java.util.Locale;
import java.text.ParseException;

import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.impl.ZoneMeta;
import com.ibm.icu.impl.CalendarAstronomer;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.*;

/**
 * @test
 * @summary Tests of new functionality in IBMCalendar
 */
public class IBMCalendarTest extends CalendarTest {

    public static void main(String[] args) throws Exception {
        new IBMCalendarTest().run(args);
    }

    /**
     * Test weekend support in IBMCalendar.
     *
     * NOTE: This test will have to be updated when the isWeekend() etc.
     *       API is finalized later.
     *
     *       In particular, the test will have to be rewritten to instantiate
     *       a Calendar in the given locale (using getInstance()) and call
     *       that Calendar's isWeekend() etc. methods.
     */
    public void TestWeekend() {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE MMM dd yyyy G HH:mm:ss.SSS");
        
        // NOTE
        // This test tests for specific locale data.  This is probably okay
        // as far as US data is concerned, but if the Arabic/Bahrain data
        // changes, this test will have to be updated.

        // Test specific days
        Object[] DATA1 = {
            Locale.US, new int[] { // Saturday:Sunday
                2000, Calendar.MARCH, 17, 23,  0, 0, // Fri 23:00
                2000, Calendar.MARCH, 18,  0, -1, 0, // Fri 23:59:59.999
                2000, Calendar.MARCH, 18,  0,  0, 1, // Sat 00:00
                2000, Calendar.MARCH, 18, 15,  0, 1, // Sat 15:00
                2000, Calendar.MARCH, 19, 23,  0, 1, // Sun 23:00
                2000, Calendar.MARCH, 20,  0, -1, 1, // Sun 23:59:59.999
                2000, Calendar.MARCH, 20,  0,  0, 0, // Mon 00:00
                2000, Calendar.MARCH, 20,  8,  0, 0, // Mon 08:00
            },
            new Locale("ar", "BH"), new int[] { // Thursday:Friday
                2000, Calendar.MARCH, 15, 23,  0, 0, // Wed 23:00
                2000, Calendar.MARCH, 16,  0, -1, 0, // Wed 23:59:59.999
                2000, Calendar.MARCH, 16,  0,  0, 1, // Thu 00:00
                2000, Calendar.MARCH, 16, 15,  0, 1, // Thu 15:00
                2000, Calendar.MARCH, 17, 23,  0, 1, // Fri 23:00
                2000, Calendar.MARCH, 18,  0, -1, 1, // Fri 23:59:59.999
                2000, Calendar.MARCH, 18,  0,  0, 0, // Sat 00:00
                2000, Calendar.MARCH, 18,  8,  0, 0, // Sat 08:00
            },
        };

        // Test days of the week
        Object[] DATA2 = {
            Locale.US, new int[] {
                Calendar.MONDAY,   Calendar.WEEKDAY,
                Calendar.FRIDAY,   Calendar.WEEKDAY,
                Calendar.SATURDAY, Calendar.WEEKEND,
                Calendar.SUNDAY,   Calendar.WEEKEND_CEASE,
            },
            new Locale("ar", "BH"), new int[] { // Thursday:Friday
                Calendar.WEDNESDAY,Calendar.WEEKDAY,
                Calendar.SATURDAY, Calendar.WEEKDAY,
                Calendar.THURSDAY, Calendar.WEEKEND,
                Calendar.FRIDAY,   Calendar.WEEKEND_CEASE,
            },
        };

        // We only test the getDayOfWeekType() and isWeekend() APIs.
        // The getWeekendTransition() API is tested indirectly via the
        // isWeekend() API, which calls it.

        for (int i1=0; i1<DATA1.length; i1+=2) {
            Locale loc = (Locale)DATA1[i1];
            int[] data = (int[]) DATA1[i1+1];
            Calendar cal = Calendar.getInstance(loc);
            logln("Locale: " + loc);
            for (int i=0; i<data.length; i+=6) {
                cal.clear();
                cal.set(data[i], data[i+1], data[i+2], data[i+3], 0, 0);
                if (data[i+4] != 0) {
                    cal.setTime(new Date(cal.getTime().getTime() + data[i+4]));
                }
                boolean isWeekend = cal.isWeekend();
                boolean ok = isWeekend == (data[i+5] != 0);
                if (ok) {
                    logln("Ok:   " + fmt.format(cal.getTime()) + " isWeekend=" + isWeekend);
                } else {
                    errln("FAIL: " + fmt.format(cal.getTime()) + " isWeekend=" + isWeekend +
                          ", expected=" + (!isWeekend));
                }
            }
        }

        for (int i2=0; i2<DATA2.length; i2+=2) {
            Locale loc = (Locale)DATA2[i2];
            int[] data = (int[]) DATA2[i2+1];
            logln("Locale: " + loc);
            Calendar cal = Calendar.getInstance(loc);
            for (int i=0; i<data.length; i+=2) {
                int type = cal.getDayOfWeekType(data[i]);
                int exp  = data[i+1];
                if (type == exp) {
                    logln("Ok:   DOW " + data[i] + " type=" + type);
                } else {
                    errln("FAIL: DOW " + data[i] + " type=" + type +
                          ", expected=" + exp);
                }
            }
        }
    }

    /**
     * Run a test of a quasi-Gregorian calendar.  This is a calendar
     * that behaves like a Gregorian but has different year/era mappings.
     * The int[] data array should have the format:
     * 
     * { era, year, gregorianYear, month, dayOfMonth, ... }
     */
    void quasiGregorianTest(Calendar cal, int[] data) {
        // As of JDK 1.4.1_01, using the Sun JDK GregorianCalendar as
        // a reference throws us off by one hour.  This is most likely
        // due to the JDK 1.4 incorporation of historical time zones.
        //java.util.Calendar grego = java.util.Calendar.getInstance();
        Calendar grego = Calendar.getInstance();
        for (int i=0; i<data.length; ) {
            int era = data[i++];
            int year = data[i++];
            int gregorianYear = data[i++];
            int month = data[i++];
            int dayOfMonth = data[i++];

            grego.clear();
            grego.set(gregorianYear, month, dayOfMonth);
            Date D = grego.getTime();

            cal.clear();
            cal.set(Calendar.ERA, era);
            cal.set(year, month, dayOfMonth);
            Date d = cal.getTime();
            if (d.equals(D)) {
                logln("OK: " + era + ":" + year + "/" + (month+1) + "/" + dayOfMonth +
                      " => " + d);
            } else {
                errln("Fail: " + era + ":" + year + "/" + (month+1) + "/" + dayOfMonth +
                      " => " + d + ", expected " + D);
            }

            cal.clear();
            cal.setTime(D);
            int e = cal.get(Calendar.ERA);
            int y = cal.get(Calendar.YEAR);
            if (y == year && e == era) {
                logln("OK: " + D + " => " + cal.get(Calendar.ERA) + ":" +
                      cal.get(Calendar.YEAR) + "/" +
                      (cal.get(Calendar.MONTH)+1) + "/" + cal.get(Calendar.DATE));
            } else {
                logln("Fail: " + D + " => " + cal.get(Calendar.ERA) + ":" +
                      cal.get(Calendar.YEAR) + "/" +
                      (cal.get(Calendar.MONTH)+1) + "/" + cal.get(Calendar.DATE) +
                      ", expected " + era + ":" + year + "/" + (month+1) + "/" +
                      dayOfMonth);
            }
        }
    }

    /**
     * Verify that BuddhistCalendar shifts years to Buddhist Era but otherwise
     * behaves like GregorianCalendar.
     */
    public void TestBuddhist() {
        quasiGregorianTest(new BuddhistCalendar(),
                           new int[] {
                               // BE 2542 == 1999 CE
                               0, 2542, 1999, Calendar.JUNE, 4
                           });
    }

    public void TestBuddhistCoverage() {
    {    
        // new BuddhistCalendar(ULocale)
        BuddhistCalendar cal = new BuddhistCalendar(ULocale.getDefault());
        if(cal == null){        
            errln("could not create BuddhistCalendar with ULocale");
        }
    }
    
    {    
        // new BuddhistCalendar(TimeZone,ULocale)
        BuddhistCalendar cal = new BuddhistCalendar(TimeZone.getDefault(),ULocale.getDefault());
        if(cal == null){        
            errln("could not create BuddhistCalendar with TimeZone ULocale");
        }
    }    
    
    {
        // new BuddhistCalendar(TimeZone)
        BuddhistCalendar cal = new BuddhistCalendar(TimeZone.getDefault());
        if(cal == null){
            errln("could not create BuddhistCalendar with TimeZone");
        }
    }
    
    {
        // new BuddhistCalendar(Locale)
        BuddhistCalendar cal = new BuddhistCalendar(Locale.getDefault());
        if(cal == null){
            errln("could not create BuddhistCalendar with Locale");
        }
    }

    {
        // new BuddhistCalendar(TimeZone, Locale)
        BuddhistCalendar cal = new BuddhistCalendar(TimeZone.getDefault(), Locale.getDefault());
        if(cal == null){
            errln("could not create BuddhistCalendar with TimeZone and Locale");
        }
    }

    {
        // new BuddhistCalendar(Date)
        BuddhistCalendar cal = new BuddhistCalendar(new Date());
        if(cal == null){
            errln("could not create BuddhistCalendar with Date");
        }
    }

    {
        // new BuddhistCalendar(int year, int month, int date)
        BuddhistCalendar cal = new BuddhistCalendar(2543, Calendar.MAY, 22);
        if(cal == null){
            errln("could not create BuddhistCalendar with year,month,data");
        }
    }

    {
        // new BuddhistCalendar(int year, int month, int date, int hour, int minute, int second)
        BuddhistCalendar cal = new BuddhistCalendar(2543, Calendar.MAY, 22, 1, 1, 1);
        if(cal == null){
            errln("could not create BuddhistCalendar with year,month,date,hour,minute,second");
        }
    }

    {
        // data
        BuddhistCalendar cal = new BuddhistCalendar(2543, Calendar.MAY, 22);
        Date time = cal.getTime();

        String[] calendarLocales = {
        "th_TH"
        };

        String[] formatLocales = {
        "en", "ar", "hu", "th"
        };

        for (int i = 0; i < calendarLocales.length; ++i) {
        String calLocName = calendarLocales[i];
        Locale calLocale = LocaleUtility.getLocaleFromName(calLocName);
        cal = new BuddhistCalendar(calLocale);

        for (int j = 0; j < formatLocales.length; ++j) {
            String locName = formatLocales[j];
            Locale formatLocale = LocaleUtility.getLocaleFromName(locName);
            DateFormat format = DateFormat.getDateTimeInstance(cal, DateFormat.FULL, DateFormat.FULL, formatLocale);
            logln(calLocName + "/" + locName + " --> " + format.format(time));
        }
        }
    }
    }

    /**
     * Verify that JapaneseCalendar shifts years to Buddhist Era but otherwise
     * behaves like GregorianCalendar.
     */
    public void TestJapanese() {
        // First make sure this test works for GregorianCalendar
        int[] control = {
            GregorianCalendar.AD, 1868, 1868, Calendar.SEPTEMBER, 8,
            GregorianCalendar.AD, 1868, 1868, Calendar.SEPTEMBER, 9,
            GregorianCalendar.AD, 1869, 1869, Calendar.JUNE, 4,
            GregorianCalendar.AD, 1912, 1912, Calendar.JULY, 29,
            GregorianCalendar.AD, 1912, 1912, Calendar.JULY, 30,
            GregorianCalendar.AD, 1912, 1912, Calendar.AUGUST, 1,
        };
        quasiGregorianTest(new GregorianCalendar(), control);

        int[] data = {
            JapaneseCalendar.MEIJI, 1, 1868, Calendar.SEPTEMBER, 8,
            JapaneseCalendar.MEIJI, 1, 1868, Calendar.SEPTEMBER, 9,
            JapaneseCalendar.MEIJI, 2, 1869, Calendar.JUNE, 4,
            JapaneseCalendar.MEIJI, 45, 1912, Calendar.JULY, 29,
            JapaneseCalendar.TAISHO, 1, 1912, Calendar.JULY, 30,
            JapaneseCalendar.TAISHO, 1, 1912, Calendar.AUGUST, 1,
        };
        quasiGregorianTest(new JapaneseCalendar(), data);
    }

    /**
     * Test limits of the Gregorian calendar.
     */
    public void TestGregorianLimits() {
        // Final parameter is either number of days, if > 0, or test
        // duration in seconds, if < 0.
        Calendar cal = Calendar.getInstance();
        cal.set(2004, Calendar.JANUARY, 1);
        doLimitsTest(new GregorianCalendar(), null, cal.getTime(), -10);
    }
    
    /**
     * Test behavior of fieldDifference around leap years.  Also test a large
     * field difference to check binary search.
     */
    public void TestLeapFieldDifference() {
        Calendar cal = Calendar.getInstance();
        cal.set(2004, Calendar.FEBRUARY, 29);
        Date date2004 = cal.getTime();
        cal.set(2000, Calendar.FEBRUARY, 29);
        Date date2000 = cal.getTime();
        int y = cal.fieldDifference(date2004, Calendar.YEAR);
        int d = cal.fieldDifference(date2004, Calendar.DAY_OF_YEAR);
        if (d == 0) {
            logln("Ok: 2004/Feb/29 - 2000/Feb/29 = " + y + " years, " + d + " days");
        } else {
            errln("FAIL: 2004/Feb/29 - 2000/Feb/29 = " + y + " years, " + d + " days");
        }
        cal.setTime(date2004);
        y = cal.fieldDifference(date2000, Calendar.YEAR);
        d = cal.fieldDifference(date2000, Calendar.DAY_OF_YEAR);
        if (d == 0) {
            logln("Ok: 2000/Feb/29 - 2004/Feb/29 = " + y + " years, " + d + " days");
        } else {
            errln("FAIL: 2000/Feb/29 - 2004/Feb/29 = " + y + " years, " + d + " days");
        }
        // Test large difference
        cal.set(2001, Calendar.APRIL, 5); // 2452005
        Date ayl = cal.getTime();
        cal.set(1964, Calendar.SEPTEMBER, 7); // 2438646
        Date asl = cal.getTime();
        d = cal.fieldDifference(ayl, Calendar.DAY_OF_MONTH);
        cal.setTime(ayl);
        int d2 = cal.fieldDifference(asl, Calendar.DAY_OF_MONTH);
        if (d == -d2 && d == 13359) {
            logln("Ok: large field difference symmetrical " + d);
        } else {
            logln("FAIL: large field difference incorrect " + d + ", " + d2 +
                  ", expect +/- 13359");
        }
    }

    /**
     * Test ms_MY "Malay (Malaysia)" locale.  Bug 1543.
     */
    public void TestMalaysianInstance() {
        Locale loc = new Locale("ms", "MY");  // Malay (Malaysia)
        Calendar cal = Calendar.getInstance(loc);
        if(cal == null){
            errln("could not create Malaysian instance");
        }
    }

    /**
     * setFirstDayOfWeek and setMinimalDaysInFirstWeek may change the
     * field <=> time mapping, since they affect the interpretation of
     * the WEEK_OF_MONTH or WEEK_OF_YEAR fields.
     */
    public void TestWeekShift() {
        Calendar cal = new GregorianCalendar(
                             TimeZone.getTimeZone("America/Los_Angeles"),
                             new Locale("en", "US"));
        cal.setTime(new Date(997257600000L)); // Wed Aug 08 01:00:00 PDT 2001
        // In pass one, change the first day of week so that the weeks
        // shift in August 2001.  In pass two, change the minimal days
        // in the first week so that the weeks shift in August 2001.
        //     August 2001     
        // Su Mo Tu We Th Fr Sa
        //           1  2  3  4
        //  5  6  7  8  9 10 11
        // 12 13 14 15 16 17 18
        // 19 20 21 22 23 24 25
        // 26 27 28 29 30 31   
        for (int pass=0; pass<2; ++pass) {
            if (pass==0) {
                cal.setFirstDayOfWeek(Calendar.WEDNESDAY);
                cal.setMinimalDaysInFirstWeek(4);
            } else {
                cal.setFirstDayOfWeek(Calendar.SUNDAY);
                cal.setMinimalDaysInFirstWeek(4);
            }
            cal.add(Calendar.DATE, 1); // Force recalc
            cal.add(Calendar.DATE, -1);

            Date time1 = cal.getTime(); // Get time -- should not change

            // Now change a week parameter and then force a recalc.
            // The bug is that the recalc should not be necessary --
            // calendar should do so automatically.
            if (pass==0) {
                cal.setFirstDayOfWeek(Calendar.THURSDAY);
            } else {
                cal.setMinimalDaysInFirstWeek(5);
            }

            int woy1 = cal.get(Calendar.WEEK_OF_YEAR);
            int wom1 = cal.get(Calendar.WEEK_OF_MONTH);

            cal.add(Calendar.DATE, 1); // Force recalc
            cal.add(Calendar.DATE, -1);

            int woy2 = cal.get(Calendar.WEEK_OF_YEAR);
            int wom2 = cal.get(Calendar.WEEK_OF_MONTH);

            Date time2 = cal.getTime();

            if (!time1.equals(time2)) {
                errln("FAIL: shifting week should not alter time");
            } else {
                logln(time1.toString());
            }
            if (woy1 == woy2 && wom1 == wom2) {
                logln("Ok: WEEK_OF_YEAR: " + woy1 +
                      ", WEEK_OF_MONTH: " + wom1);
            } else {
                errln("FAIL: WEEK_OF_YEAR: " + woy1 + " => " + woy2 +
                      ", WEEK_OF_MONTH: " + wom1 + " => " + wom2 +
                      " after week shift");
            }
        }
    }

    /**
     * Make sure that when adding a day, we actually wind up in a
     * different day.  The DST adjustments we use to keep the hour
     * constant across DST changes can backfire and change the day.
     */
    public void TestTimeZoneTransitionAdd() {
        Locale locale = Locale.US; // could also be CHINA
        SimpleDateFormat dateFormat =
            new SimpleDateFormat("MM/dd/yyyy HH:mm z", locale);

        String tz[] = TimeZone.getAvailableIDs();

        for (int z=0; z<tz.length; ++z) {
            TimeZone t = TimeZone.getTimeZone(tz[z]);
            dateFormat.setTimeZone(t);

            Calendar cal = Calendar.getInstance(t, locale);
            cal.clear();
            // Scan the year 2003, overlapping the edges of the year
            cal.set(Calendar.YEAR, 2002);
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 25);

            for (int i=0; i<365+10; ++i) {
                Date yesterday = cal.getTime();
                int yesterday_day = cal.get(Calendar.DAY_OF_MONTH);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                if (yesterday_day == cal.get(Calendar.DAY_OF_MONTH)) {
                    errln(tz[z] + " " +
                          dateFormat.format(yesterday) + " +1d= " +
                          dateFormat.format(cal.getTime()));
                }
            }
        }
    }

    public void TestJB1684() {
        class TestData {
            int year;
            int month;
            int date;
            int womyear;
            int wommon;
            int wom;
            int dow;
            String data;
            String normalized;

            public TestData(int year, int month, int date,
                            int womyear, int wommon, int wom, int dow,
                            String data, String normalized) {
                this.year = year;
                this.month = month-1;
                this.date = date;
                this.womyear = womyear;
                this.wommon = wommon-1;
                this.wom = wom;
                this.dow = dow;
                this.data = data; // year, month, week of month, day
                this.normalized = data;
                if (normalized != null) this.normalized = normalized;
            }
        };

        //      July 2001            August 2001           January 2002    
        // Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
        //  1  2  3  4  5  6  7            1  2  3  4         1  2  3  4  5
        //  8  9 10 11 12 13 14   5  6  7  8  9 10 11   6  7  8  9 10 11 12
        // 15 16 17 18 19 20 21  12 13 14 15 16 17 18  13 14 15 16 17 18 19
        // 22 23 24 25 26 27 28  19 20 21 22 23 24 25  20 21 22 23 24 25 26
        // 29 30 31              26 27 28 29 30 31     27 28 29 30 31      
        TestData[] tests = {
            new TestData(2001, 8,  6,  2001,8,2,Calendar.MONDAY,    "2001 08 02 Mon", null),
            new TestData(2001, 8,  7,  2001,8,2,Calendar.TUESDAY,   "2001 08 02 Tue", null),
            new TestData(2001, 8,  5,/*12,*/ 2001,8,2,Calendar.SUNDAY,    "2001 08 02 Sun", null),
            new TestData(2001, 8,6, /*7,  30,*/ 2001,7,6,Calendar.MONDAY,    "2001 07 06 Mon", "2001 08 02 Mon"),
            new TestData(2001, 8,7, /*7,  31,*/ 2001,7,6,Calendar.TUESDAY,   "2001 07 06 Tue", "2001 08 02 Tue"),
            new TestData(2001, 8,  5,  2001,7,6,Calendar.SUNDAY,    "2001 07 06 Sun", "2001 08 02 Sun"),
            new TestData(2001, 7,  30, 2001,8,1,Calendar.MONDAY,    "2001 08 01 Mon", "2001 07 05 Mon"),
            new TestData(2001, 7,  31, 2001,8,1,Calendar.TUESDAY,   "2001 08 01 Tue", "2001 07 05 Tue"),
            new TestData(2001, 7,29, /*8,  5,*/  2001,8,1,Calendar.SUNDAY,    "2001 08 01 Sun", "2001 07 05 Sun"),
            new TestData(2001, 12, 31, 2001,12,6,Calendar.MONDAY,   "2001 12 06 Mon", null),
            new TestData(2002, 1,  1,  2002,1,1,Calendar.TUESDAY,   "2002 01 01 Tue", null),
            new TestData(2002, 1,  2,  2002,1,1,Calendar.WEDNESDAY, "2002 01 01 Wed", null),
            new TestData(2002, 1,  3,  2002,1,1,Calendar.THURSDAY,  "2002 01 01 Thu", null),
            new TestData(2002, 1,  4,  2002,1,1,Calendar.FRIDAY,    "2002 01 01 Fri", null),
            new TestData(2002, 1,  5,  2002,1,1,Calendar.SATURDAY,  "2002 01 01 Sat", null),
            new TestData(2001,12,30, /*2002, 1,  6,*/  2002,1,1,Calendar.SUNDAY,    "2002 01 01 Sun", "2001 12 06 Sun"),
        };

        int pass = 0, error = 0, warning = 0;

        final String pattern = "yyyy MM WW EEE";
        GregorianCalendar cal = new GregorianCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setCalendar(cal);

        cal.setFirstDayOfWeek(Calendar.SUNDAY);
        cal.setMinimalDaysInFirstWeek(1);

        for (int i = 0; i < tests.length; ++i) {
            TestData test = tests[i];
            log("\n-----\nTesting round trip of " + test.year +
                  " " + (test.month + 1) +
                  " " + test.date +
                  " (written as) " + test.data);

            cal.clear();
            cal.set(test.year, test.month, test.date);
            Date ms = cal.getTime();

            cal.clear();
            cal.set(Calendar.YEAR, test.womyear);
            cal.set(Calendar.MONTH, test.wommon);
            cal.set(Calendar.WEEK_OF_MONTH, test.wom);
            cal.set(Calendar.DAY_OF_WEEK, test.dow);
            Date ms2 = cal.getTime();

            if (!ms2.equals(ms)) {
                log("\nError: GregorianCalendar.DOM gave " + ms +
                    "\n       GregorianCalendar.WOM gave " + ms2);
                error++;
            } else {
                pass++;
            }

            ms2 = null;
            try {
                ms2 = sdf.parse(test.data);
            }
            catch (ParseException e) {
                errln("parse exception: " + e);
            }

            if (!ms2.equals(ms)) {
                log("\nError: GregorianCalendar gave      " + ms +
                    "\n       SimpleDateFormat.parse gave " + ms2);
                error++;
            } else {
                pass++;
            }

            String result = sdf.format(ms);
            if (!result.equals(test.normalized)) {
                log("\nWarning: format of '" + test.data + "' gave" +
                    "\n                   '" + result + "'" +
                    "\n          expected '" + test.normalized + "'");
                warning++;
            } else {
                pass++;
            }

            Date ms3 = null;
            try {
                ms3 = sdf.parse(result);
            }
            catch (ParseException e) {
                errln("parse exception 2: " + e);
            }

            if (!ms3.equals(ms)) {
                error++;
                log("\nError: Re-parse of '" + result + "' gave time of " +
                    "\n        " + ms3 +
                    "\n    not " + ms);
            } else {
                pass++;
            }
        }
        String info = "\nPassed: " + pass + ", Warnings: " + warning + ", Errors: " + error;
        if (error > 0) {
            errln(info);
        } else {
            logln(info);
        }
    }

    /**
     * Test the ZoneMeta API.
     */
    public void TestZoneMeta() {
        // Test index by country API

        // Format: {country, zone1, zone2, ..., zoneN}
        String COUNTRY[][] = { {"", "GMT", "UTC"},
                               {"US", "America/Los_Angeles", "PST"} };
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<COUNTRY.length; ++i) {
            String[] a = ZoneMeta.getAvailableIDs(COUNTRY[i][0]);
            buf.setLength(0);
            buf.append("Country \"" + COUNTRY[i][0] + "\": [");
            // Use bitmask to track which of the expected zones we see
            int mask = 0;
            for (int j=0; j<a.length; ++j) {
                if (j!=0) buf.append(", ");
                buf.append(a[j]);
                for (int k=1; k<COUNTRY[i].length; ++k) {
                    if ((mask & (1<<k)) == 0 &&
                        a[j] == COUNTRY[i][k]) {
                        mask |= (1<<k);
                    }
                }
            }
            buf.append("]");
            mask >>= 1;
            // Check bitmask to see if we saw all expected zones
            if (mask == (1 << (COUNTRY[i].length-1))-1) {
                logln(buf.toString());
            } else {
                errln(buf.toString());
            }
        }

        // Test equivalent IDs API

        int n = ZoneMeta.countEquivalentIDs("PST");
        boolean ok = false;
        buf.setLength(0);
        buf.append("Equivalent to PST: ");
        for (int i=0; i<n; ++i) {
            String id = ZoneMeta.getEquivalentID("PST", i);
            if (id == "America/Los_Angeles") {
                ok = true;
            }
            if (i!=0) buf.append(", ");
            buf.append(id);
        }
        if (ok) {
            logln(buf.toString());
        } else {
            errln(buf.toString());
        }
    }

    /**
     * Miscellaneous tests to increase coverage.
     */
    public void TestCoverage() {
        // BuddhistCalendar
        BuddhistCalendar bcal = new BuddhistCalendar();
        /*int i =*/ bcal.getMinimum(Calendar.ERA);
        bcal.add(Calendar.YEAR, 1);
        bcal.add(Calendar.MONTH, 1);
        /*Date d = */bcal.getTime();

        // CalendarAstronomer
        // (This class should probably be made package-private.)
        CalendarAstronomer astro = new CalendarAstronomer();
        String s = astro.local(0);

        // ChineseCalendar
        ChineseCalendar ccal = new ChineseCalendar(TimeZone.getDefault(),
                                                   Locale.getDefault());
        ccal.add(Calendar.MONTH, 1);
        ccal.add(Calendar.YEAR, 1);
        ccal.roll(Calendar.MONTH, 1);
        ccal.roll(Calendar.YEAR, 1);
        ccal.getTime();

        // ICU 2.6
        Calendar cal = Calendar.getInstance(Locale.US);
        logln(cal.toString());
        logln(cal.getDisplayName(Locale.US));
        int weekendOnset=-1;
        int weekendCease=-1;
        for (int i=Calendar.SUNDAY; i<=Calendar.SATURDAY; ++i) {
            if (cal.getDayOfWeekType(i) == Calendar.WEEKEND_ONSET) {
                weekendOnset = i;
            }
            if (cal.getDayOfWeekType(i) == Calendar.WEEKEND_CEASE) {
                weekendCease = i;
            }
        }
        // can't call this unless we get a transition day (unusual),
        // but make the call anyway for coverage reasons
        try {
            /*int x=*/ cal.getWeekendTransition(weekendOnset);
            /*int x=*/ cal.getWeekendTransition(weekendCease);
        } catch (IllegalArgumentException e) {}
        /*int x=*/ cal.isWeekend(new Date());
        
        // new GregorianCalendar(ULocale)
        GregorianCalendar gcal = new GregorianCalendar(ULocale.getDefault());
        if(gcal==null){
            errln("could not create GregorianCalendar with ULocale");
        } else {
            logln("Calendar display name: " + gcal.getDisplayName(ULocale.getDefault()));
        }
        
        //cover getAvailableULocales
        final ULocale[] locales = Calendar.getAvailableULocales();
        long count = locales.length;
        if (count == 0)
            errln("getAvailableULocales return empty list");
        logln("" + count + " available ulocales in Calendar.");
    }
}
