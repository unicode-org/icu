// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class InTemporalLeapYearTest extends com.ibm.icu.dev.test.TestFmwk {
    @Test
    public void TestGregorian() {
        // test from year 1800 to 2500
        GregorianCalendar gc = new GregorianCalendar();
        for (int year = 1900; year < 2400; ++year) {
            gc.set(year, Calendar.MARCH, 7);
            assertEquals("Calendar::inTemporalLeapYear",
                     gc.isLeapYear(year), gc.inTemporalLeapYear() == true);
        }
    }

    private void RunChinese(Calendar cal) {
        GregorianCalendar gc = new GregorianCalendar();
        Calendar leapTest = (Calendar)cal.clone();
        // Start our test from 1900, Jan 1.
        // Check every 29 days in exhausted mode.
        int incrementDays = 29;
        int startYear = 1900;
        int stopYear = 2400;

        boolean quick = TestFmwk.getExhaustiveness() <= 5;
        if (quick) {
            incrementDays = 317;
            stopYear = 2100;
        }
        int yearForHasLeapMonth = -1;
        boolean hasLeapMonth = false;
        for (gc.set(startYear, Calendar.JANUARY, 1);
             gc.get(Calendar.YEAR) <= stopYear;
             gc.add(Calendar.DATE, incrementDays)) {
            cal.setTime(gc.getTime());
            int cal_year = cal.get(Calendar.EXTENDED_YEAR);
            if (yearForHasLeapMonth != cal_year) {
                leapTest.set(Calendar.EXTENDED_YEAR, cal_year);
                leapTest.set(Calendar.MONTH, 0);
                leapTest.set(Calendar.DATE, 1);
                // seek any leap month
                // check any leap month in the next 12 months.
                for (hasLeapMonth = false;
                     (!hasLeapMonth) && cal_year == leapTest.get(Calendar.EXTENDED_YEAR);
                     leapTest.add(Calendar.MONTH, 1)) {
                    hasLeapMonth = leapTest.get(Calendar.IS_LEAP_MONTH) != 0;
                }
                yearForHasLeapMonth = cal_year;
            }

            boolean actualInLeap =  cal.inTemporalLeapYear();
            if (hasLeapMonth != actualInLeap) {
                logln("Gregorian y=" + gc.get(Calendar.YEAR) +
                    " m=" + gc.get(Calendar.MONTH) +
                    " d=" + gc.get(Calendar.DATE) +
                    " => cal y=" + cal.get(Calendar.EXTENDED_YEAR) +
                    " m=" + (cal.get(Calendar.IS_LEAP_MONTH) == 1 ? "L" : "") +
                    cal.get(Calendar.MONTH) +
                    " d=" + cal.get(Calendar.DAY_OF_MONTH) +
                    " expected:" + (hasLeapMonth ? "true" : "false") +
                    " actual:" + (actualInLeap ? "true" : "false"));
            }
            assertEquals("inTemporalLeapYear", hasLeapMonth, actualInLeap);
        }
    }

    @Test
    public void TestChinese() {
        RunChinese(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "chinese")));
    }

    @Test
    public void TestDangi() {
        RunChinese(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "dangi")));
    }

    @Test
    public void TestHebrew() {
        Calendar cal = Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "hebrew"));

        GregorianCalendar gc = new GregorianCalendar();
        Calendar leapTest = (Calendar)cal.clone();
        // Start our test from 1900, Jan 1.
        // Check every 29 days in exhausted mode.
        int incrementDays = 29;
        int startYear = 1900;
        int stopYear = 2400;

        boolean quick = TestFmwk.getExhaustiveness() <= 5;
        if (quick) {
            incrementDays = 317;
            stopYear = 2100;
        }
        int yearForHasLeapMonth = -1;
        boolean hasLeapMonth = false;
        for (gc.set(startYear, Calendar.JANUARY, 1);
             gc.get(Calendar.YEAR) <= stopYear;
             gc.add(Calendar.DATE, incrementDays)) {
            cal.setTime(gc.getTime());
            int cal_year = cal.get(Calendar.EXTENDED_YEAR);
            if (yearForHasLeapMonth != cal_year) {
                leapTest.set(Calendar.EXTENDED_YEAR, cal_year);
                leapTest.set(Calendar.MONTH, 0);
                leapTest.set(Calendar.DATE, 1);
                leapTest.add(Calendar.MONTH, 10);
                hasLeapMonth = leapTest.get(Calendar.MONTH) == HebrewCalendar.TAMUZ;
                yearForHasLeapMonth = cal_year;
            }
            boolean actualInLeap =  cal.inTemporalLeapYear();
            if (hasLeapMonth != actualInLeap) {
                logln("Gregorian y=" + gc.get(Calendar.YEAR) +
                    " m=" + gc.get(Calendar.MONTH) +
                    " d=" + gc.get(Calendar.DATE) +
                    " => cal y=" + cal.get(Calendar.EXTENDED_YEAR) +
                    " m=" + (cal.get(Calendar.IS_LEAP_MONTH) == 1 ? "L" : "") +
                    cal.get(Calendar.MONTH) +
                    " d=" + cal.get(Calendar.DAY_OF_MONTH) +
                    " expected:" + (hasLeapMonth ? "true" : "false") +
                    " actual:" + (actualInLeap ? "true" : "false"));
            }
            assertEquals("inTemporalLeapYear", hasLeapMonth, actualInLeap);
        }
    }

    private void RunIslamic(Calendar cal) {
        RunXDaysIsLeap(cal, 355);
    }

    @Test
    public void TestIslamic() {
        RunIslamic(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "islamic")));
    }

    @Test
    public void TestIslamicCivil() {
        RunIslamic(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "islamic-civil")));
    }

    @Test
    public void TestIslamicUmalqura() {
        RunIslamic(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "islamic-umalqura")));
    }

    @Test
    public void TestIslamicRGSA() {
        RunIslamic(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "islamic-rgsa")));
    }

    @Test
    public void TestIslamicTBLA() {
        RunIslamic(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "islamic-tbla")));
    }

    private void RunXDaysIsLeap(Calendar cal, int x) {
        GregorianCalendar gc = new GregorianCalendar();
        Calendar leapTest = (Calendar)cal.clone();
        // Start our test from 1900, Jan 1.
        // Check every 29 days in exhausted mode.
        int incrementDays = 29;
        int startYear = 1900;
        int stopYear = 2400;

        boolean quick = TestFmwk.getExhaustiveness() <= 5;
        if (quick) {
            incrementDays = 317;
            stopYear = 2100;
        }
        int yearForHasLeapMonth = -1;
        boolean hasLeapMonth = false;
        for (gc.set(startYear, Calendar.JANUARY, 1);
             gc.get(Calendar.YEAR) <= stopYear;
             gc.add(Calendar.DATE, incrementDays)) {
            cal.setTime(gc.getTime());
            int cal_year = cal.get(Calendar.EXTENDED_YEAR);
            if (yearForHasLeapMonth != cal_year) {
                // If that year has exactly x days, it is a leap year.
                hasLeapMonth = cal.getActualMaximum(Calendar.DAY_OF_YEAR) == x;
                yearForHasLeapMonth = cal_year;
            }

            boolean actualInLeap =  cal.inTemporalLeapYear();
            if (hasLeapMonth != actualInLeap) {
                logln("Gregorian y=" + gc.get(Calendar.YEAR) +
                    " m=" + gc.get(Calendar.MONTH) +
                    " d=" + gc.get(Calendar.DATE) +
                    " => cal y=" + cal.get(Calendar.EXTENDED_YEAR) +
                    " m=" + (cal.get(Calendar.IS_LEAP_MONTH) == 1 ? "L" : "") +
                    cal.get(Calendar.MONTH) +
                    " d=" + cal.get(Calendar.DAY_OF_MONTH) +
                    " expected:" + (hasLeapMonth ? "true" : "false") +
                    " actual:" + (actualInLeap ? "true" : "false"));
            }
            assertEquals("inTemporalLeapYear", hasLeapMonth, actualInLeap);
        }
    }

    @Test
    public void TestTaiwan() {
        RunXDaysIsLeap(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "roc")), 366);
    }

    @Test
    public void TestJapanese() {
        RunXDaysIsLeap(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "japanese")), 366);
    }

    @Test
    public void TestBuddhist() {
        RunXDaysIsLeap(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "buddhist")), 366);
    }

    @Test
    public void TestPersian() {
        RunXDaysIsLeap(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "persian")), 366);
    }

    @Test
    public void TestIndian() {
        RunXDaysIsLeap(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "indian")), 366);
    }

    @Test
    public void TestCoptic() {
        RunXDaysIsLeap(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "coptic")), 366);
    }

    @Test
    public void TestEthiopic() {
        RunXDaysIsLeap(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "ethiopic")), 366);
    }

    @Test
    public void TestEthiopicAmeteAlem() {
        RunXDaysIsLeap(Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "ethiopic-amete-alem")), 366);
    }
}
