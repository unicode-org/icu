// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CopticCalendar;
import com.ibm.icu.util.EthiopicCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class OrdinalMonthTest extends com.ibm.icu.dev.test.TestFmwk {

    private void VerifyMonth(String message, Calendar cc, int expectedMonth,
                 int expectedOrdinalMonth, boolean expectedLeapMonth,
                 String expectedMonthCode) {
         assertEquals(message + " get(MONTH)",
             expectedMonth, cc.get(Calendar.MONTH));
         assertEquals(message + " get(ORDINAL_MONTH)",
             expectedOrdinalMonth, cc.get(Calendar.ORDINAL_MONTH));
         assertEquals(message + " get(IS_LEAP_MONTH)",
              expectedLeapMonth ? 1 : 0, cc.get(Calendar.IS_LEAP_MONTH));
         assertEquals(message + " getTemporalMonthCode()",
             expectedMonthCode, cc.getTemporalMonthCode());
    }

    void assertSetTemporalMonthCodeThrowIllegalArgumentException(Calendar cal, String monthCode) {
       try {
           cal.setTemporalMonthCode(monthCode);
                fail("setTemporalMonthCode(\"" + monthCode + "\") should get IllegalArgumentException");
       } catch (IllegalArgumentException expected) {
           // expect to catch IllegalArgumentException
       }
    }

    void assertSetTemporalMonthCodeThrowIllegalArgumentException(
        Calendar cal, String [] invalidMonthCodes) {
        for (String monthCode : invalidMonthCodes) {
            assertSetTemporalMonthCodeThrowIllegalArgumentException(cal, monthCode);
        }
    }

    @Test
    public void TestMostCalendarsSet() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(2022, Calendar.DECEMBER, 16);
        String [] calendars = Calendar.getKeywordValuesForLocale(
            "calendar", ULocale.ROOT, false);
        for (String calendar : calendars) {
            // Test these three calendars differently.
            if (calendar == "chinese") continue; // work around ICU-22444
            if (calendar == "dangi") continue; // work around ICU-22444
            if (calendar == "hebrew") continue; // work around ICU-22444
            Calendar cc1 = Calendar.getInstance(
                ULocale.ROOT.setKeywordValue("calendar", calendar));
            Calendar cc2 = (Calendar)cc1.clone();
            Calendar cc3 = (Calendar)cc1.clone();

            cc1.set(Calendar.EXTENDED_YEAR, 2134);
            cc2.set(Calendar.EXTENDED_YEAR, 2134);
            cc3.set(Calendar.EXTENDED_YEAR, 2134);
            cc1.set(Calendar.MONTH, 5);
            cc2.set(Calendar.ORDINAL_MONTH, 5);
            cc3.setTemporalMonthCode("M06");
            cc1.set(Calendar.DATE, 23);
            cc2.set(Calendar.DATE, 23);
            cc3.set(Calendar.DATE, 23);
            assertEquals("M06 cc2==cc1 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
                cc1, cc2);
            assertEquals("M06 cc2==cc3 set month by Calendar.MONTH and setTemporalMonthCode",
                cc2, cc3);
            VerifyMonth("cc1", cc1, 5, 5, false, "M06");
            VerifyMonth("cc2", cc2, 5, 5, false, "M06");
            VerifyMonth("cc3", cc3, 5, 5, false, "M06");

            cc1.set(Calendar.ORDINAL_MONTH, 6);
            cc2.setTemporalMonthCode("M07");
            cc3.set(Calendar.MONTH, 6);
            assertEquals("M07 cc1==cc3 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
                cc1, cc3);
            assertEquals("M07 cc2==cc3 set month by Calendar.MONTH and setTemporalMonthCode",
                cc2, cc3);
            VerifyMonth("cc1", cc1, 6, 6, false, "M07");
            VerifyMonth("cc2", cc2, 6, 6, false, "M07");
            VerifyMonth("cc3", cc3, 6, 6, false, "M07");

            cc1.setTemporalMonthCode("M08");
            cc2.set(Calendar.MONTH, 7);
            cc3.set(Calendar.ORDINAL_MONTH, 7);
            assertEquals("M08 cc1==cc2 set month by Calendar.MONTH and setTemporalMonthCode",
                cc1, cc2);
            assertEquals("M08 cc2==cc3 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
                cc2, cc3);
            VerifyMonth("cc1", cc1, 7, 7, false, "M08");
            VerifyMonth("cc2", cc2, 7, 7, false, "M08");
            VerifyMonth("cc3", cc3, 7, 7, false, "M08");

            cc1.set(Calendar.DATE, 3);
            // For "M13", do not return error for these three calendars.
            if (calendar == "coptic" || calendar == "ethiopic" ||
                calendar == "ethiopic-amete-alem") {
                cc1.setTemporalMonthCode("M13");
                assertEquals("get(Calendar.MONTH) after setTemporalMonthCode(\"M13\")",
                             12, cc1.get(Calendar.MONTH));
                assertEquals("get(Calendar.ORDINAL_MONTH) after setTemporalMonthCode(\"M13\")",
                             12, cc1.get(Calendar.ORDINAL_MONTH));
            } else {
                assertSetTemporalMonthCodeThrowIllegalArgumentException(cc1, "M13");
            }

            // Out of bound monthCodes should return error.
            // These are not valid for calendar do not have a leap month
            String [] invalidMonthCodes = {
              "M00", "M14", "M01L", "M02L", "M03L", "M04L", "M05L", "M06L", "M07L",
              "M08L", "M09L", "M10L", "M11L", "M12L"
            };
            assertSetTemporalMonthCodeThrowIllegalArgumentException(cc1, invalidMonthCodes);
        }
    }

    private void RunTestChineseCalendarSet(String calendar, int notLeapYear, int leapMarchYear) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(2022, Calendar.DECEMBER, 16);
        Calendar cc1 = Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", calendar));
        Calendar cc2 = (Calendar)cc1.clone();
        Calendar cc3 = (Calendar)cc1.clone();
        cc1.set(Calendar.EXTENDED_YEAR, leapMarchYear);
        cc2.set(Calendar.EXTENDED_YEAR, leapMarchYear);
        cc3.set(Calendar.EXTENDED_YEAR, leapMarchYear);

        cc1.set(Calendar.MONTH, Calendar.MARCH);
        cc1.set(Calendar.IS_LEAP_MONTH, 1);
        cc2.set(Calendar.ORDINAL_MONTH, 3);
        cc3.setTemporalMonthCode("M03L");
        cc1.set(Calendar.DATE, 1);
        cc2.set(Calendar.DATE, 1);
        cc3.set(Calendar.DATE, 1);
        assertEquals("" + leapMarchYear + " M03L cc2==cc1 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
            cc1, cc2);
        assertEquals("" + leapMarchYear + " M03L cc2==cc3 set month by Calendar.ORDINAL_MONTH and setTemporalMonthCode",
            cc2, cc3);
        VerifyMonth("" +  leapMarchYear + " M03L cc1", cc1, Calendar.MARCH, 3, true, "M03L");
        VerifyMonth("" +  leapMarchYear + " M03L cc2", cc2, Calendar.MARCH, 3, true, "M03L");
        VerifyMonth("" +  leapMarchYear + " M03L cc3", cc3, Calendar.MARCH, 3, true, "M03L");

        cc1.set(Calendar.EXTENDED_YEAR, notLeapYear);
        cc2.set(Calendar.EXTENDED_YEAR, notLeapYear);
        cc3.set(Calendar.EXTENDED_YEAR, notLeapYear);
        cc1.set(Calendar.ORDINAL_MONTH, 5);
        cc2.setTemporalMonthCode("M06");
        cc3.set(Calendar.MONTH, Calendar.JUNE);
        cc3.set(Calendar.IS_LEAP_MONTH, 0);
        assertEquals("" + notLeapYear + " M06 cc1==cc2 set month by Calendar.ORDINAL_MONTH and setTemporalMonthCode",
            cc1, cc2);
        assertEquals("" + notLeapYear + " M06 cc2==cc3 set month by Calendar.MONTH and setTemporalMonthCode",
            cc2, cc3);
        VerifyMonth("" + notLeapYear + " M06 cc1", cc1, Calendar.JUNE, 5, false, "M06");
        VerifyMonth("" + notLeapYear + " M06 cc2", cc2, Calendar.JUNE, 5, false, "M06");
        VerifyMonth("" + notLeapYear + " M06 cc3", cc3, Calendar.JUNE, 5, false, "M06");

        cc1.set(Calendar.EXTENDED_YEAR, leapMarchYear);
        cc2.set(Calendar.EXTENDED_YEAR, leapMarchYear);
        cc3.set(Calendar.EXTENDED_YEAR, leapMarchYear);
        cc1.setTemporalMonthCode("M04");
        cc2.set(Calendar.MONTH, Calendar.APRIL);
        cc2.set(Calendar.IS_LEAP_MONTH, 0);
        cc3.set(Calendar.ORDINAL_MONTH, 4);
        assertEquals("" + leapMarchYear + " M04 cc2==cc1 set month by setTemporalMonthCode and Calendar.MONTH",
            cc1, cc2);
        assertEquals("" + leapMarchYear + " M04 cc2==cc3 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
            cc2, cc3);
        // 4592 has leap March so April is the 5th month in that year.
        VerifyMonth("" + leapMarchYear + " M04 cc1", cc1, Calendar.APRIL, 4, false, "M04");
        VerifyMonth("" + leapMarchYear + " M04 cc2", cc2, Calendar.APRIL, 4, false, "M04");
        VerifyMonth("" + leapMarchYear + " M04 cc3", cc3, Calendar.APRIL, 4, false, "M04");

        cc1.set(Calendar.EXTENDED_YEAR, notLeapYear);
        cc2.set(Calendar.EXTENDED_YEAR, notLeapYear);
        cc3.set(Calendar.EXTENDED_YEAR, notLeapYear);
        assertEquals("" + notLeapYear + " M04 cc2==cc1 set month by setTemporalMonthCode and Calendar.MONTH",
            cc1, cc2);
        assertEquals("" + notLeapYear + " M04 cc2==cc3 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
            cc2, cc3);
        // 4592 has no leap month before April so April is the 4th month in that year.
        VerifyMonth("" + leapMarchYear + " M04 cc1", cc1, Calendar.APRIL, 3, false, "M04");
        VerifyMonth("" + leapMarchYear + " M04 cc2", cc2, Calendar.APRIL, 3, false, "M04");
        VerifyMonth("" + leapMarchYear + " M04 cc3", cc3, Calendar.APRIL, 3, false, "M04");

        // Out of bound monthCodes should return error.
        // These are not valid for calendar do not have a leap month
        String [] invalidMonthCodes = {"M00", "M13", "M14"};
        assertSetTemporalMonthCodeThrowIllegalArgumentException(cc1, invalidMonthCodes);
    }

    @Test
    public void TestChineseCalendarSet() {
        RunTestChineseCalendarSet("chinese", 4591, 4592);
    }

    @Test
    public void TestDangiCalendarSet() {
        RunTestChineseCalendarSet("dangi", 4287, 4288);
    }

    @Test
    public void TestHebrewCalendarSet() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(2022, Calendar.DECEMBER, 16);
        Calendar cc1 = Calendar.getInstance(ULocale.ROOT.setKeywordValue("calendar", "hebrew"));
        Calendar cc2 = (Calendar)cc1.clone();
        Calendar cc3 = (Calendar)cc1.clone();
        // 5782 is leap year, 5781 is NOT.
        int leapYear = 5782;
        int notLeapYear = 5781;
        cc1.set(Calendar.EXTENDED_YEAR, leapYear);
        cc2.set(Calendar.EXTENDED_YEAR, leapYear);
        cc3.set(Calendar.EXTENDED_YEAR, leapYear);

        cc1.set(Calendar.MONTH, HebrewCalendar.ADAR_1);
        cc2.set(Calendar.ORDINAL_MONTH, 5);
        cc3.setTemporalMonthCode("M05L");
        cc1.set(Calendar.DATE, 1);
        cc2.set(Calendar.DATE, 1);
        cc3.set(Calendar.DATE, 1);
        assertEquals("5782 M05L cc2==cc1 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
            cc1, cc2);
        assertEquals("5782 M05L cc2==cc3 set month by Calendar.MONTH and setTemporalMonthCode",
            cc2, cc3);
        VerifyMonth("cc1", cc1, HebrewCalendar.ADAR_1, 5, false, "M05L");
        VerifyMonth("cc2", cc2, HebrewCalendar.ADAR_1, 5, false, "M05L");
        VerifyMonth("cc3", cc3, HebrewCalendar.ADAR_1, 5, false, "M05L");

        cc1.set(Calendar.ORDINAL_MONTH, 4);
        cc2.setTemporalMonthCode("M05");
        cc3.set(Calendar.MONTH, HebrewCalendar.SHEVAT);
        assertEquals("5782 M05 cc2==cc3 set month by Calendar.ORDINAL_MONTH and setTemporalMonthCode",
            cc1, cc2);
        assertEquals("5782 M05 cc2==cc3 set month by Calendar.MONTH and setTemporalMonthCode",
            cc2, cc3);
        VerifyMonth("cc1", cc1, HebrewCalendar.SHEVAT, 4, false, "M05");
        VerifyMonth("cc2", cc2, HebrewCalendar.SHEVAT, 4, false, "M05");
        VerifyMonth("cc3", cc3, HebrewCalendar.SHEVAT, 4, false, "M05");

        cc1.set(Calendar.EXTENDED_YEAR, notLeapYear);
        cc2.set(Calendar.EXTENDED_YEAR, notLeapYear);
        cc3.set(Calendar.EXTENDED_YEAR, notLeapYear);
        cc1.setTemporalMonthCode("M06");
        cc2.set(Calendar.MONTH, HebrewCalendar.ADAR);
        cc3.set(Calendar.ORDINAL_MONTH, 5);
        assertEquals("5781 M06 cc1==cc2 set month by Calendar.MONTH and setTemporalMonthCode",
            cc1, cc2);
        assertEquals("5781 M06 cc2==cc3 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
            cc2, cc3);
        VerifyMonth("cc1", cc1, HebrewCalendar.ADAR, 5, false, "M06");
        VerifyMonth("cc2", cc2, HebrewCalendar.ADAR, 5, false, "M06");
        VerifyMonth("cc3", cc3, HebrewCalendar.ADAR, 5, false, "M06");

        cc1.set(Calendar.EXTENDED_YEAR, leapYear);
        cc2.set(Calendar.EXTENDED_YEAR, leapYear);
        cc3.set(Calendar.EXTENDED_YEAR, leapYear);
        assertEquals("5782 M06 cc1==cc2 set month by Calendar.MONTH and setTemporalMonthCode",
            cc1, cc2);
        assertEquals("5782 M06 cc2==cc3 set month by Calendar.MONTH and Calendar.ORDINAL_MONTH",
            cc2, cc3);
        assertEquals("5782 M06 cc2==cc3 set month by Calendar.MONTH and setTemporalMonthCode",
            cc2, cc3);
        VerifyMonth("cc1", cc1, HebrewCalendar.ADAR, 6, false, "M06");
        VerifyMonth("cc2", cc2, HebrewCalendar.ADAR, 6, false, "M06");
        VerifyMonth("cc3", cc3, HebrewCalendar.ADAR, 6, false, "M06");

        cc1.set(Calendar.ORDINAL_MONTH, 7);
        cc2.setTemporalMonthCode("M07");
        cc3.set(Calendar.MONTH, HebrewCalendar.NISAN);
        assertEquals("5782 M07 cc1==cc2 set month by Calendar.ORDINAL_MONTH and setTemporalMonthCode",
           cc1, cc2);
        assertEquals("5782 M07 cc2==cc3 set month by Calendar.MONTH and setTemporalMonthCode",
            cc2, cc3);
        VerifyMonth("cc1", cc1, HebrewCalendar.NISAN, 7, false, "M07");
        VerifyMonth("cc2", cc2, HebrewCalendar.NISAN, 7, false, "M07");
        VerifyMonth("cc3", cc3, HebrewCalendar.NISAN, 7, false, "M07");

        // Out of bound monthCodes should return error.
        // These are not valid for calendar do not have a leap month
        String [] invalidMonthCodes = {
          "M00", "M13", "M14", "M01L", "M02L", "M03L", "M04L",
          /* M05L could be legal */
          "M06L", "M07L", "M08L", "M09L", "M10L", "M11L", "M12L"};
        assertSetTemporalMonthCodeThrowIllegalArgumentException(cc1, invalidMonthCodes);
    }

    @Test
    public void TestAdd() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(2022, Calendar.DECEMBER, 16);
        String [] calendars = Calendar.getKeywordValuesForLocale(
            "calendar", ULocale.ROOT, false);
        for (String calendar : calendars) {
            Calendar cc1 = Calendar.getInstance(
                ULocale.ROOT.setKeywordValue("calendar", calendar));
            cc1.setTime(gc.getTime());
            Calendar cc2 = (Calendar)cc1.clone();
            for (int i = 0; i < 8; i++) {
                for (int j = 1; j < 8; j++) {
                    cc1.add(Calendar.MONTH, j);
                    cc2.add(Calendar.ORDINAL_MONTH, j);
                    assertEquals("two add produce the same result", cc1, cc2);
                }
                for (int j = 1; j < 8; j++) {
                    cc1.add(Calendar.MONTH, -j);
                    cc2.add(Calendar.ORDINAL_MONTH, -j);
                    assertEquals("two add produce the same result", cc1, cc2);
                }
            }
        }
    }

    @Test
    public void TestRoll() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(2022, Calendar.DECEMBER, 16);
        String [] calendars = Calendar.getKeywordValuesForLocale(
            "calendar", ULocale.ROOT, false);
        for (String calendar : calendars) {
            Calendar cc1 = Calendar.getInstance(
                ULocale.ROOT.setKeywordValue("calendar", calendar));
            cc1.setTime(gc.getTime());
            Calendar cc2 = (Calendar)cc1.clone();
            for (int i = 0; i < 8; i++) {
                for (int j = 1; j < 8; j++) {
                    cc1.roll(Calendar.MONTH, j);
                    cc2.roll(Calendar.ORDINAL_MONTH, j);
                    assertEquals("two roll produce the same result", cc1, cc2);
                }
                for (int j = 1; j < 8; j++) {
                    cc1.roll(Calendar.MONTH, -j);
                    cc2.roll(Calendar.ORDINAL_MONTH, -j);
                    assertEquals("two roll produce the same result", cc1, cc2);
                }
                for (int j = 1; j < 3; j++) {
                    cc1.roll(Calendar.MONTH, true);
                    cc2.roll(Calendar.ORDINAL_MONTH, true);
                    assertEquals("two roll produce the same result", cc1, cc2);
                }
                for (int j = 1; j < 3; j++) {
                    cc1.roll(Calendar.MONTH, false);
                    cc2.roll(Calendar.ORDINAL_MONTH, false);
                    assertEquals("two roll produce the same result", cc1, cc2);
                }
            }
        }
    }

    @Test
    public void TestLimits() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(2022, Calendar.DECEMBER, 16);
        String [] calendars = Calendar.getKeywordValuesForLocale(
            "calendar", ULocale.ROOT, false);
        Object[][] cases = {
            { "gregorian", 0, 11, 0, 11 },
            { "japanese", 0, 11, 0, 11 },
            { "buddhist", 0, 11, 0, 11 },
            { "roc", 0, 11, 0, 11 },
            { "persian", 0, 11, 0, 11 },
            { "islamic-civil", 0, 11, 0, 11 },
            { "islamic", 0, 11, 0, 11 },
            { "hebrew", 0, 12, 0, 11 },
            { "chinese", 0, 12, 0, 11 },
            { "indian", 0, 11, 0, 11 },
            { "coptic", 0, 12, 0, 12 },
            { "ethiopic", 0, 12, 0, 12 },
            { "ethiopic-amete-alem", 0, 12, 0, 12 },
            { "iso8601", 0, 11, 0, 11 },
            { "dangi", 0, 12, 0, 11 },
            { "islamic-umalqura", 0, 11, 0, 11 },
            { "islamic-tbla", 0, 11, 0, 11 },
            { "islamic-rgsa", 0, 11, 0, 11 },
        };
        for (String calendar : calendars) {
            Calendar cc1 = Calendar.getInstance(
                ULocale.ROOT.setKeywordValue("calendar", calendar));
            boolean found = false;
            for (Object[] cas : cases) {
                if (calendar.equals((String) cas[0])) {
                    int min = (Integer) cas[1];
                    int max = (Integer) cas[2];
                    int greatestMin = (Integer) cas[3];
                    int leastMax = (Integer) cas[4];
                    assertEquals("getMinimum(Calendar.ORDINAL_MONTH)",
                                 min, cc1.getMinimum(Calendar.ORDINAL_MONTH));
                    assertEquals("getMaximum(Calendar.ORDINAL_MONTH)",
                                 max, cc1.getMaximum(Calendar.ORDINAL_MONTH));
                    assertEquals("getMinimum(Calendar.ORDINAL_MONTH)",
                                 greatestMin, cc1.getGreatestMinimum(Calendar.ORDINAL_MONTH));
                    assertEquals("getMinimum(Calendar.ORDINAL_MONTH)",
                                 leastMax, cc1.getLeastMaximum(Calendar.ORDINAL_MONTH));
                    found = true;
                    break;
                }
            }
            if (!found) {
                errln("Cannot find expectation" + calendar);
            }
        }
    }

    @Test
    public void TestActaulLimits() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(2022, Calendar.DECEMBER, 16);
        Object[][] cases = {
            { "gregorian", 2021, 0, 11 },
            { "gregorian", 2022, 0, 11 },
            { "gregorian", 2023, 0, 11 },
            { "japanese", 2021, 0, 11 },
            { "japanese", 2022, 0, 11 },
            { "japanese", 2023, 0, 11 },
            { "buddhist", 2021, 0, 11 },
            { "buddhist", 2022, 0, 11 },
            { "buddhist", 2023, 0, 11 },
            { "roc", 2021, 0, 11 },
            { "roc", 2022, 0, 11 },
            { "roc", 2023, 0, 11 },
            { "persian", 1400, 0, 11 },
            { "persian", 1401, 0, 11 },
            { "persian", 1402, 0, 11 },
            { "hebrew", 5782, 0, 12 },
            { "hebrew", 5783, 0, 11 },
            { "hebrew", 5789, 0, 11 },
            { "hebrew", 5790, 0, 12 },
            { "chinese", 4645, 0, 11 },
            { "chinese", 4646, 0, 12 },
            { "chinese", 4647, 0, 11 },
            { "dangi", 4645 + 304, 0, 11 },
            { "dangi", 4646 + 304, 0, 12 },
            { "dangi", 4647 + 304, 0, 11 },
            { "indian", 1944, 0, 11 },
            { "indian", 1945, 0, 11 },
            { "indian", 1946, 0, 11 },
            { "coptic", 1737, 0, 12 },
            { "coptic", 1738, 0, 12 },
            { "coptic", 1739, 0, 12 },
            { "ethiopic", 2013, 0, 12 },
            { "ethiopic", 2014, 0, 12 },
            { "ethiopic", 2015, 0, 12 },
            { "ethiopic-amete-alem", 2014, 0, 12 },
            { "ethiopic-amete-alem", 2015, 0, 12 },
            { "ethiopic-amete-alem", 2016, 0, 12 },
            { "iso8601", 2022, 0, 11 },
            { "islamic-civil", 1443, 0, 11 },
            { "islamic-civil", 1444, 0, 11 },
            { "islamic-civil", 1445, 0, 11 },
            { "islamic", 1443, 0, 11 },
            { "islamic", 1444, 0, 11 },
            { "islamic", 1445, 0, 11 },
            { "islamic-umalqura", 1443, 0, 11 },
            { "islamic-umalqura", 1444, 0, 11 },
            { "islamic-umalqura", 1445, 0, 11 },
            { "islamic-tbla", 1443, 0, 11 },
            { "islamic-tbla", 1444, 0, 11 },
            { "islamic-tbla", 1445, 0, 11 },
            { "islamic-rgsa", 1443, 0, 11 },
            { "islamic-rgsa", 1444, 0, 11 },
            { "islamic-rgsa", 1445, 0, 11 },
        };
        for (Object[] cas : cases) {
            String calendar = (String) cas[0];
            int extended_year = (Integer) cas[1];
            int actualMinOrdinalMonth = (Integer) cas[2];
            int actualMaxOrdinalMonth = (Integer) cas[3];
            Calendar cc1 = Calendar.getInstance(
                ULocale.ROOT.setKeywordValue("calendar", calendar));
            cc1.set(Calendar.EXTENDED_YEAR, extended_year);
            cc1.set(Calendar.ORDINAL_MONTH, 0);
            cc1.set(Calendar.DATE, 1);
            assertEquals("getActualMinimum(Calendar.ORDINAL_MONTH)",
                         actualMinOrdinalMonth, cc1.getActualMinimum(Calendar.ORDINAL_MONTH));
            assertEquals("getActualMaximum(Calendar.ORDINAL_MONTH)",
                         actualMaxOrdinalMonth, cc1.getActualMaximum(Calendar.ORDINAL_MONTH));
        }
    }
}
