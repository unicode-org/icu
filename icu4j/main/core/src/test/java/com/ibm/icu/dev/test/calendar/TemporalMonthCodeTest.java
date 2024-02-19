// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CopticCalendar;
import com.ibm.icu.util.EthiopicCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class TemporalMonthCodeTest extends CoreTestFmwk {
    @Test
    public void TestChineseCalendarGetTemporalMonthCode() {
        RunChineseGetTemporalMonthCode(
            Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "chinese")));
    }

    @Test
    public void TestDangiCalendarGetTemporalMonthCode() {
        RunChineseGetTemporalMonthCode(
            Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "dangi")));
    }

    private String MonthCode(int month, boolean leap) {
        return String.format("M%02d%s", month, leap ? "L" : "");
    }

    private String HebrewMonthCode(int month) {
        if (month == HebrewCalendar.ADAR_1) {
            return MonthCode(month, true);
        }
        return MonthCode(month < HebrewCalendar.ADAR_1 ? month+1 : month, false);
    }

    private void RunChineseGetTemporalMonthCode(Calendar cal) {
        GregorianCalendar gc = new GregorianCalendar();
        // Start our test from 1900, Jan 1.
        // Check every 29 days in exhausted mode.
        int incrementDays = 29;
        int startYear = 1900;
        int stopYear = 2400;

        boolean quick = TestFmwk.getExhaustiveness() <= 5;
        if (quick) {
            incrementDays = 317;
            startYear = 1950;
            stopYear = 2050;
        }
        for (gc.set(startYear, Calendar.JANUARY, 1);
             gc.get(Calendar.YEAR) <= stopYear;
             gc.add(Calendar.DATE, incrementDays)) {
            cal.setTime(gc.getTime());
            int cal_month = cal.get(Calendar.MONTH);
            String expected = MonthCode(cal_month+1, cal.get(Calendar.IS_LEAP_MONTH) != 0);
            assertEquals("getTemporalMonthCode", expected, cal.getTemporalMonthCode());
        }
    }

    @Test
    public void TestHebrewCalendarGetTemporalMonthCode() {
        Calendar cal = Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "hebrew"));
        GregorianCalendar gc = new GregorianCalendar();
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
        for (gc.set(startYear, Calendar.JANUARY, 1);
             gc.get(Calendar.YEAR) <= stopYear;
             gc.add(Calendar.DATE, incrementDays)) {
            cal.setTime(gc.getTime());
            int cal_month = cal.get(Calendar.MONTH);
            String expected = HebrewMonthCode(cal_month);
            assertEquals("getTemporalMonthCode", expected, cal.getTemporalMonthCode());
        }
    }

    private void RunCEGetTemporalMonthCode(Calendar cal) {
        GregorianCalendar gc = new GregorianCalendar();
        // Start our test from 1900, Jan 1.
        //  // Start testing from 1900
        gc.set(1900, Calendar.JANUARY, 1);
        cal.setTime(gc.getTime());
        int year = cal.get(Calendar.YEAR);
        for (int m = 0; m < 13; m++) {
            String expected = MonthCode(m+1, false);
            for (int y = year; y < year + 500 ; y++) {
                cal.set(y, m, 1);
                assertEquals("getTemporalMonthCode", expected, cal.getTemporalMonthCode());
            }
        }
    }

    @Test
    public void TestCopticCalendarGetTemporalMonthCode() {
        RunCEGetTemporalMonthCode(
            Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "coptic")));
    }
    @Test
    public void TestEthiopicCalendarGetTemporalMonthCode() {
        RunCEGetTemporalMonthCode(
            Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "ethiopic")));
    }
    @Test
    public void TestEthiopicAmeteAlemCalendarGetTemporalMonthCode() {
        RunCEGetTemporalMonthCode(
            Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "ethiopic-amete-alem")));
    }

    @Test
    public void TestGregorianCalendarSetTemporalMonthCode() {
        Object[][] cases = {
            { 1911, Calendar.JANUARY, 31, "M01", 0 },
            { 1970, Calendar.FEBRUARY, 22, "M02", 1 },
            { 543, Calendar.MARCH, 3, "M03", 2 },
            { 2340, Calendar.APRIL, 21, "M04", 3 },
            { 1234, Calendar.MAY, 21, "M05", 4 },
            { 1931, Calendar.JUNE, 17, "M06", 5 },
            { 2000, Calendar.JULY, 1, "M07", 6 },
            { 2033, Calendar.AUGUST, 3, "M08", 7 },
            { 2013, Calendar.SEPTEMBER, 9, "M09", 8 },
            { 1849, Calendar.OCTOBER, 31, "M10", 9 },
            { 1433, Calendar.NOVEMBER, 30, "M11", 10 },
            { 2022, Calendar.DECEMBER, 25, "M12", 11 },
        };
        GregorianCalendar gc1 = new GregorianCalendar();
        GregorianCalendar gc2 = new GregorianCalendar();
        for (Object[] cas : cases) {
            int year = (Integer) cas[0];
            int month = (Integer) cas[1];
            int date = (Integer) cas[2];
            String monthCode = (String) cas[3];
            int ordinalMonth = (Integer) cas[4];
            gc1.clear();
            gc2.clear();
            gc1.set(year, month, date);
            gc2.set(Calendar.YEAR, year);
            gc2.setTemporalMonthCode(monthCode);
            gc2.set(Calendar.DATE, date);
            assertEquals("by set and setTemporalMonthCode()", gc1, gc2);
            String actualMonthCode1 = gc1.getTemporalMonthCode();
            String actualMonthCode2 = gc2.getTemporalMonthCode();
            assertEquals("getTemporalMonthCode()", actualMonthCode1, actualMonthCode2);
            assertEquals("getTemporalMonthCode()", monthCode, actualMonthCode2);
            assertEquals("ordinalMonth", ordinalMonth, gc2.get(Calendar.ORDINAL_MONTH));
            assertEquals("ordinalMonth",
                gc1.get(Calendar.ORDINAL_MONTH), gc2.get(Calendar.ORDINAL_MONTH));
        }
    }

    @Test
    public void TestChineseCalendarSetTemporalMonthCode() {
        Calendar cc1 = Calendar.getInstance(
            ULocale.ROOT.setKeywordValue("calendar", "chinese"));
        Calendar cc2 = (Calendar)cc1.clone();
        GregorianCalendar gc1 = new GregorianCalendar();
        Object[][] cases = {
            // https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2022.pdf
            { 2022, Calendar.DECEMBER, 15, 4659, Calendar.NOVEMBER, 22, "M11", false, 10},
            // M01L is very hard to find. Cannot find a year has M01L in these several
            // centuries.
            // M02L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2004.pdf
            { 2004, Calendar.MARCH, 20, 4641, Calendar.FEBRUARY, 30, "M02", false, 1},
            { 2004, Calendar.MARCH, 21, 4641, Calendar.FEBRUARY, 1, "M02L", true, 2},
            { 2004, Calendar.APRIL, 18, 4641, Calendar.FEBRUARY, 29, "M02L", true, 2},
            { 2004, Calendar.APRIL, 19, 4641, Calendar.MARCH, 1, "M03", false, 3},
            // M03L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/1995.pdf
            { 1955, Calendar.APRIL, 21, 4592, Calendar.MARCH, 29, "M03", false, 2},
            { 1955, Calendar.APRIL, 22, 4592, Calendar.MARCH, 1, "M03L", true, 3},
            { 1955, Calendar.MAY, 21, 4592, Calendar.MARCH, 30, "M03L", true, 3},
            { 1955, Calendar.MAY, 22, 4592, Calendar.APRIL, 1, "M04", false, 4},
            // M12 https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/1996.pdf
            { 1956, Calendar.FEBRUARY, 11, 4592, Calendar.DECEMBER, 30, "M12", false, 12},
            // M04L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2001.pdf
            { 2001, Calendar.MAY, 22, 4638, Calendar.APRIL, 30, "M04", false, 3},
            { 2001, Calendar.MAY, 23, 4638, Calendar.APRIL, 1, "M04L", true, 4},
            { 2001, Calendar.JUNE, 20, 4638, Calendar.APRIL, 29, "M04L", true, 4},
            { 2001, Calendar.JUNE, 21, 4638, Calendar.MAY, 1, "M05", false, 5},
            // M05L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2009.pdf
            { 2009, Calendar.JUNE, 22, 4646, Calendar.MAY, 30, "M05", false, 4},
            { 2009, Calendar.JUNE, 23, 4646, Calendar.MAY, 1, "M05L", true, 5},
            { 2009, Calendar.JULY, 21, 4646, Calendar.MAY, 29, "M05L", true, 5},
            { 2009, Calendar.JULY, 22, 4646, Calendar.JUNE, 1, "M06", false, 6},
            // M06L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2017.pdf
            { 2017, Calendar.JULY, 22, 4654, Calendar.JUNE, 29, "M06", false, 5},
            { 2017, Calendar.JULY, 23, 4654, Calendar.JUNE, 1, "M06L", true, 6},
            { 2017, Calendar.AUGUST, 21, 4654, Calendar.JUNE, 30, "M06L", true, 6},
            { 2017, Calendar.AUGUST, 22, 4654, Calendar.JULY, 1, "M07", false, 7},
            // M07L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2006.pdf
            { 2006, Calendar.AUGUST, 23, 4643, Calendar.JULY, 30, "M07", false, 6},
            { 2006, Calendar.AUGUST, 24, 4643, Calendar.JULY, 1, "M07L", true, 7},
            { 2006, Calendar.SEPTEMBER, 21, 4643, Calendar.JULY, 29, "M07L", true, 7},
            { 2006, Calendar.SEPTEMBER, 22, 4643, Calendar.AUGUST, 1, "M08", false, 8},
            // M08L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/1995.pdf
            { 1995, Calendar.SEPTEMBER, 24, 4632, Calendar.AUGUST, 30, "M08", false, 7},
            { 1995, Calendar.SEPTEMBER, 25, 4632, Calendar.AUGUST, 1, "M08L", true, 8},
            { 1995, Calendar.OCTOBER, 23, 4632, Calendar.AUGUST, 29, "M08L", true, 8},
            { 1995, Calendar.OCTOBER, 24, 4632, Calendar.SEPTEMBER, 1, "M09", false, 9},
            // M09L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2014.pdf
            { 2014, Calendar.OCTOBER, 23, 4651, Calendar.SEPTEMBER, 30, "M09", false, 8},
            { 2014, Calendar.OCTOBER, 24, 4651, Calendar.SEPTEMBER, 1, "M09L", true, 9},
            { 2014, Calendar.NOVEMBER, 21, 4651, Calendar.SEPTEMBER, 29, "M09L", true, 9},
            { 2014, Calendar.NOVEMBER, 22, 4651, Calendar.OCTOBER, 1, "M10", false, 10},
            // M10L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/1984.pdf
            { 1984, Calendar.NOVEMBER, 22, 4621, Calendar.OCTOBER, 30, "M10", false, 9},
            { 1984, Calendar.NOVEMBER, 23, 4621, Calendar.OCTOBER, 1, "M10L", true, 10},
            { 1984, Calendar.DECEMBER, 21, 4621, Calendar.OCTOBER, 29, "M10L", true, 10},
            { 1984, Calendar.DECEMBER, 22, 4621, Calendar.NOVEMBER, 1, "M11", false, 11},
            // M11L https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2033.pdf
            //      https://www.hko.gov.hk/tc/gts/time/calendar/pdf/files/2034.pdf
            { 2033, Calendar.DECEMBER, 21, 4670, Calendar.NOVEMBER, 30, "M11", false, 10},
            { 2033, Calendar.DECEMBER, 22, 4670, Calendar.NOVEMBER, 1, "M11L", true, 11},
            { 2034, Calendar.JANUARY, 19, 4670, Calendar.NOVEMBER, 29, "M11L", true, 11},
            { 2034, Calendar.JANUARY, 20, 4670, Calendar.DECEMBER, 1, "M12", false, 12},
            // M12L is very hard to find. Cannot find a year has M01L in these several
            // centuries.
        };
        for (Object[] cas : cases) {
            int gYear = (Integer) cas[0];
            int gMonth = (Integer) cas[1];
            int gDate = (Integer) cas[2];
            int cYear = (Integer) cas[3];
            int cMonth = (Integer) cas[4];
            int cDate = (Integer) cas[5];
            String cMonthCode = (String) cas[6];
            boolean cLeapMonth = (Boolean) cas[7];
            int cOrdinalMonth = (Integer) cas[8];
            gc1.clear();
            cc1.clear();
            cc2.clear();
            gc1.set(gYear, gMonth, gDate);
            cc1.setTime(gc1.getTime());
            cc2.set(Calendar.EXTENDED_YEAR, cYear);
            cc2.setTemporalMonthCode(cMonthCode);
            cc2.set(Calendar.DATE, cDate);
            assertEquals("year", cYear, cc1.get(Calendar.EXTENDED_YEAR));
            assertEquals("month", cMonth, cc1.get(Calendar.MONTH));
            assertEquals("date", cDate, cc1.get(Calendar.DATE));
            assertEquals("is_leap_month", cLeapMonth ? 1 : 0,
                cc1.get(Calendar.IS_LEAP_MONTH));
            assertEquals("getTemporalMonthCode()", cMonthCode,
                cc1.getTemporalMonthCode());
            assertEquals("ordinalMonth", cOrdinalMonth, cc1.get(Calendar.ORDINAL_MONTH));
            assertEquals("by set() and setTemporalMonthCode()", cc1, cc2);
         }
    }

    @Test
    public void TestHebrewCalendarSetTemporalMonthCode() {
        Calendar hc1 = Calendar.getInstance(
            ULocale.ROOT.setKeywordValue("calendar", "hebrew"));
        Calendar hc2 = (Calendar)hc1.clone();
        GregorianCalendar gc1 = new GregorianCalendar();
        Object[][] cases = {
            { 2022, Calendar.JANUARY, 11, 5782, HebrewCalendar.SHEVAT, 9, "M05", 4},
            { 2022, Calendar.FEBRUARY, 12, 5782, HebrewCalendar.ADAR_1, 11, "M05L", 5},
            { 2022, Calendar.MARCH, 13, 5782, HebrewCalendar.ADAR, 10, "M06", 6},
            { 2022, Calendar.APRIL, 14, 5782, HebrewCalendar.NISAN, 13, "M07", 7},
            { 2022, Calendar.MAY, 15, 5782, HebrewCalendar.IYAR, 14, "M08", 8},
            { 2022, Calendar.JUNE, 16, 5782, HebrewCalendar.SIVAN, 17, "M09", 9},
            { 2022, Calendar.JULY, 17, 5782, HebrewCalendar.TAMUZ, 18, "M10", 10},
            { 2022, Calendar.AUGUST, 18, 5782, HebrewCalendar.AV, 21, "M11", 11},
            { 2022, Calendar.SEPTEMBER, 19, 5782, HebrewCalendar.ELUL, 23, "M12", 12},
            { 2022, Calendar.OCTOBER, 20, 5783, HebrewCalendar.TISHRI, 25, "M01", 0},
            { 2022, Calendar.NOVEMBER, 21, 5783, HebrewCalendar.HESHVAN, 27, "M02", 1},
            { 2022, Calendar.DECEMBER, 22, 5783, HebrewCalendar.KISLEV, 28, "M03", 2},
            { 2023, Calendar.JANUARY, 20, 5783, HebrewCalendar.TEVET, 27, "M04", 3},
        };
        for (Object[] cas : cases) {
            int gYear = (Integer) cas[0];
            int gMonth = (Integer) cas[1];
            int gDate = (Integer) cas[2];
            int hYear = (Integer) cas[3];
            int hMonth = (Integer) cas[4];
            int hDate = (Integer) cas[5];
            String hMonthCode = (String) cas[6];
            int hOrdinalMonth = (Integer) cas[7];
            gc1.clear();
            hc1.clear();
            hc2.clear();
            gc1.set(gYear, gMonth, gDate);
            hc1.setTime(gc1.getTime());
            hc2.set(Calendar.EXTENDED_YEAR, hYear);
            hc2.setTemporalMonthCode(hMonthCode);
            hc2.set(Calendar.DATE, hDate);
            assertEquals("year", hYear, hc1.get(Calendar.EXTENDED_YEAR));
            assertEquals("month", hMonth, hc1.get(Calendar.MONTH));
            assertEquals("date", hDate, hc1.get(Calendar.DATE));
            assertEquals("getTemporalMonthCode()", hMonthCode,
                hc1.getTemporalMonthCode());
            assertEquals("by set() and setTemporalMonthCode()", hc1, hc2);
            assertEquals("ordinalMonth", hOrdinalMonth, hc1.get(Calendar.ORDINAL_MONTH));
            assertEquals("ordinalMonth", hOrdinalMonth, hc2.get(Calendar.ORDINAL_MONTH));
        }
    }

    @Test
    public void TestCopticCalendarSetTemporalMonthCode() {
        Calendar cc1 = Calendar.getInstance(
            ULocale.ROOT.setKeywordValue("calendar", "coptic"));
        Calendar cc2 = (Calendar)cc1.clone();
        GregorianCalendar gc1 = new GregorianCalendar();
        Object[][] cases = {
            { 1900, Calendar.JANUARY, 1, 1616, CopticCalendar.KIAHK, 23, "M04", 3},
            { 1900, Calendar.SEPTEMBER, 6, 1616, CopticCalendar.NASIE, 1, "M13", 12},
            { 1900, Calendar.SEPTEMBER, 10, 1616, CopticCalendar.NASIE, 5, "M13", 12},
            { 1900, Calendar.SEPTEMBER, 11, 1617, CopticCalendar.TOUT, 1, "M01", 0},
      
            { 2022, Calendar.JANUARY, 11, 1738, CopticCalendar.TOBA, 3, "M05", 4},
            { 2022, Calendar.FEBRUARY, 12, 1738, CopticCalendar.AMSHIR, 5, "M06", 5},
            { 2022, Calendar.MARCH, 13, 1738, CopticCalendar.BARAMHAT, 4, "M07", 6},
            { 2022, Calendar.APRIL, 14, 1738, CopticCalendar.BARAMOUDA, 6, "M08", 7},
            { 2022, Calendar.MAY, 15, 1738, CopticCalendar.BASHANS, 7, "M09", 8},
            { 2022, Calendar.JUNE, 16, 1738, CopticCalendar.PAONA, 9, "M10", 9},
            { 2022, Calendar.JULY, 17, 1738, CopticCalendar.EPEP, 10, "M11", 10},
            { 2022, Calendar.AUGUST, 18, 1738, CopticCalendar.MESRA, 12, "M12", 11},
            { 2022, Calendar.SEPTEMBER, 6, 1738, CopticCalendar.NASIE, 1, "M13", 12},
            { 2022, Calendar.SEPTEMBER, 10, 1738, CopticCalendar.NASIE, 5, "M13", 12},
            { 2022, Calendar.SEPTEMBER, 11, 1739, CopticCalendar.TOUT, 1, "M01", 0},
            { 2022, Calendar.SEPTEMBER, 19, 1739, CopticCalendar.TOUT, 9, "M01", 0},
            { 2022, Calendar.OCTOBER, 20, 1739, CopticCalendar.BABA, 10, "M02", 1},
            { 2022, Calendar.NOVEMBER, 21, 1739, CopticCalendar.HATOR, 12, "M03", 2},
            { 2022, Calendar.DECEMBER, 22, 1739, CopticCalendar.KIAHK, 13, "M04", 3},
      
            { 2023, Calendar.JANUARY, 1, 1739, CopticCalendar.KIAHK, 23, "M04", 3},
            { 2023, Calendar.SEPTEMBER, 6, 1739, CopticCalendar.NASIE, 1, "M13", 12},
            { 2023, Calendar.SEPTEMBER, 11, 1739, CopticCalendar.NASIE, 6, "M13", 12},
            { 2023, Calendar.SEPTEMBER, 12, 1740, CopticCalendar.TOUT, 1, "M01", 0},
      
            { 2030, Calendar.JANUARY, 1, 1746, CopticCalendar.KIAHK, 23, "M04", 3},
            { 2030, Calendar.SEPTEMBER, 6, 1746, CopticCalendar.NASIE, 1, "M13", 12},
            { 2030, Calendar.SEPTEMBER, 10, 1746, CopticCalendar.NASIE, 5, "M13", 12},
            { 2030, Calendar.SEPTEMBER, 11, 1747, CopticCalendar.TOUT, 1, "M01", 0},
        };
        for (Object[] cas : cases) {
            int gYear = (Integer) cas[0];
            int gMonth = (Integer) cas[1];
            int gDate = (Integer) cas[2];
            int cYear = (Integer) cas[3];
            int cMonth = (Integer) cas[4];
            int cDate = (Integer) cas[5];
            String cMonthCode = (String) cas[6];
            int cOrdinalMonth = (Integer) cas[7];
            gc1.clear();
            cc1.clear();
            cc2.clear();
            gc1.set(gYear, gMonth, gDate);
            cc1.setTime(gc1.getTime());
            cc2.set(Calendar.EXTENDED_YEAR, cYear);
            cc2.setTemporalMonthCode(cMonthCode);
            cc2.set(Calendar.DATE, cDate);
            assertEquals("year", cYear, cc1.get(Calendar.EXTENDED_YEAR));
            assertEquals("month", cMonth, cc1.get(Calendar.MONTH));
            assertEquals("date", cDate, cc1.get(Calendar.DATE));
            assertEquals("getTemporalMonthCode()", cMonthCode,
                cc1.getTemporalMonthCode());

            assertEquals("getTimeInMillis()", cc1.getTimeInMillis(), cc2.getTimeInMillis());
            assertEquals("by set() and setTemporalMonthCode()", cc1, cc2);
            assertEquals("ordinalMonth", cOrdinalMonth, cc1.get(Calendar.ORDINAL_MONTH));
            assertEquals("ordinalMonth", cOrdinalMonth, cc2.get(Calendar.ORDINAL_MONTH));
        }
    }

    @Test
    public void TestEthiopicCalendarSetTemporalMonthCode() {
        Calendar ec1 = Calendar.getInstance(
            ULocale.ROOT.setKeywordValue("calendar", "ethiopic"));
        Calendar ec2 = (Calendar)ec1.clone();
        GregorianCalendar gc1 = new GregorianCalendar();
        Object[][] cases = {
            { 1900, Calendar.JANUARY, 1, 1892, EthiopicCalendar.TAHSAS, 23, "M04", 3},
            { 1900, Calendar.SEPTEMBER, 6, 1892, EthiopicCalendar.PAGUMEN, 1, "M13", 12},
            { 1900, Calendar.SEPTEMBER, 10, 1892, EthiopicCalendar.PAGUMEN, 5, "M13", 12},
            { 1900, Calendar.SEPTEMBER, 11, 1893, EthiopicCalendar.MESKEREM, 1, "M01", 0},
      
            { 2022, Calendar.JANUARY, 11, 2014, EthiopicCalendar.TER, 3, "M05", 4},
            { 2022, Calendar.FEBRUARY, 12, 2014, EthiopicCalendar.YEKATIT, 5, "M06", 5},
            { 2022, Calendar.MARCH, 13, 2014, EthiopicCalendar.MEGABIT, 4, "M07", 6},
            { 2022, Calendar.APRIL, 14, 2014, EthiopicCalendar.MIAZIA, 6, "M08", 7},
            { 2022, Calendar.MAY, 15, 2014, EthiopicCalendar.GENBOT, 7, "M09", 8},
            { 2022, Calendar.JUNE, 16, 2014, EthiopicCalendar.SENE, 9, "M10", 9},
            { 2022, Calendar.JULY, 17, 2014, EthiopicCalendar.HAMLE, 10, "M11", 10},
            { 2022, Calendar.AUGUST, 18, 2014, EthiopicCalendar.NEHASSE, 12, "M12", 11},
            { 2022, Calendar.SEPTEMBER, 6, 2014, EthiopicCalendar.PAGUMEN, 1, "M13", 12},
            { 2022, Calendar.SEPTEMBER, 10, 2014, EthiopicCalendar.PAGUMEN, 5, "M13", 12},
            { 2022, Calendar.SEPTEMBER, 11, 2015, EthiopicCalendar.MESKEREM, 1, "M01", 0},
            { 2022, Calendar.SEPTEMBER, 19, 2015, EthiopicCalendar.MESKEREM, 9, "M01", 0},
            { 2022, Calendar.OCTOBER, 20, 2015, EthiopicCalendar.TEKEMT, 10, "M02", 1},
            { 2022, Calendar.NOVEMBER, 21, 2015, EthiopicCalendar.HEDAR, 12, "M03", 2},
            { 2022, Calendar.DECEMBER, 22, 2015, EthiopicCalendar.TAHSAS, 13, "M04", 3},
      
            { 2023, Calendar.JANUARY, 1, 2015, EthiopicCalendar.TAHSAS, 23, "M04", 3},
            { 2023, Calendar.SEPTEMBER, 6, 2015, EthiopicCalendar.PAGUMEN, 1, "M13", 12},
            { 2023, Calendar.SEPTEMBER, 11, 2015, EthiopicCalendar.PAGUMEN, 6, "M13", 12},
            { 2023, Calendar.SEPTEMBER, 12, 2016, EthiopicCalendar.MESKEREM, 1, "M01", 0},
      
            { 2030, Calendar.JANUARY, 1, 2022, EthiopicCalendar.TAHSAS, 23, "M04", 3},
            { 2030, Calendar.SEPTEMBER, 6, 2022, EthiopicCalendar.PAGUMEN, 1, "M13", 12},
            { 2030, Calendar.SEPTEMBER, 10, 2022, EthiopicCalendar.PAGUMEN, 5, "M13", 12},
            { 2030, Calendar.SEPTEMBER, 11, 2023, EthiopicCalendar.MESKEREM, 1, "M01", 0},
        };
        for (Object[] cas : cases) {
            int gYear = (Integer) cas[0];
            int gMonth = (Integer) cas[1];
            int gDate = (Integer) cas[2];
            int eYear = (Integer) cas[3];
            int eMonth = (Integer) cas[4];
            int eDate = (Integer) cas[5];
            String eMonthCode = (String) cas[6];
            int eOrdinalMonth = (Integer) cas[7];
            gc1.clear();
            ec1.clear();
            ec2.clear();
            gc1.set(gYear, gMonth, gDate);
            ec1.setTime(gc1.getTime());

            ec2.set(Calendar.EXTENDED_YEAR, eYear);
            ec2.setTemporalMonthCode(eMonthCode);
            ec2.set(Calendar.DATE, eDate);

            assertEquals("year", eYear, ec1.get(Calendar.EXTENDED_YEAR));
            assertEquals("month", eMonth, ec1.get(Calendar.MONTH));
            assertEquals("date", eDate, ec1.get(Calendar.DATE));
            assertEquals("getTemporalMonthCode()", eMonthCode,
                ec1.getTemporalMonthCode());
            assertEquals("by set() and setTemporalMonthCode()",
                ec1, ec2);
            assertEquals("ordinalMonth", eOrdinalMonth, ec1.get(Calendar.ORDINAL_MONTH));
            assertEquals("ordinalMonth", eOrdinalMonth, ec2.get(Calendar.ORDINAL_MONTH));
        }
    }
}
