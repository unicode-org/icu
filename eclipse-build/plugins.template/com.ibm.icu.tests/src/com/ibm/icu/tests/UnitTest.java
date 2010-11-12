/*
 ******************************************************************************
 * Copyright (C) 2005-2008, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package com.ibm.icu.tests;

import junit.framework.TestCase;
import com.ibm.icu.dev.test.TestAll;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestFmwk.TestParams;

//import com.ibm.icu.text.DateFormat;
//import com.ibm.icu.util.Calendar;
//import com.ibm.icu.util.GregorianCalendar;
//import com.ibm.icu.util.TimeZone;
//import com.ibm.icu.util.ULocale;

public class UnitTest extends TestCase {
    
    public void testBidi() throws Exception {
        runUtility("Bidi");
    }

    public void testCalendar() throws Exception {
        runUtility("Calendar");
    }

    public void testCollator() throws Exception {
        runUtility("Collator");
    }

    public void testCompression() throws Exception {
        runUtility("Compression");
    }

    public void testDuration() throws Exception {
        runUtility("Duration");
    }

    public void testDiagBigDecimal() throws Exception {
        runUtility("DiagBigDecimal");
    }

    public void testFormat() throws Exception {
        runUtility("Format");
    }

    public void testImpl() throws Exception {
        runUtility("Impl");
    }

    public void testNormalizer() throws Exception {
        runUtility("Normalizer");
    }

    public void testProperty() throws Exception {
        runUtility("Property");
    }

    public void testRBBI() throws Exception {
        runUtility("RBBI");
    }

    public void testSearchTest() throws Exception {
        runUtility("SearchTest");
    }

    public void testStringPrep() throws Exception {
        runUtility("StringPrep");
    }

    public void testTestCharsetDetector() throws Exception {
        runUtility("TestCharsetDetector");
    }

    public void testTestUCharacterIterator() throws Exception {
        runUtility("TestUCharacterIterator");
    }

    public void testTimeScale() throws Exception {
        runUtility("TimeScale");
    }

    public void testTimeZone() throws Exception {
        runUtility("TimeZone");
    }

    public void testTranslit() throws Exception {
        runUtility("Translit");
    }

    public void testUtil() throws Exception {
        runUtility("Util");
    }

    public void runUtility(String testname) throws Exception {
        TestParams params = TestParams.create("-n", null);
        TestFmwk test = new TestAll();
        test.resolveTarget(params, testname).run();
        if (params.errorCount > 0) {
            fail(params.errorSummary.toString());
        }
    }

    // sample tests from ICU4J test suite

    // Calendar
//    public void testCalendarSimple() throws Exception {
//        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
//        GregorianCalendar gc = new GregorianCalendar(tz);
//        gc.set(2005,9,17,14,15,33);
//        Date time = gc.getTime();
//        
//        final String[] calendars = {
//            "buddhist", "chinese", "coptic", "ethiopic", "gregorian", 
//            "hebrew", "islamic", "islamic-civil", "japanese"
//        };
//        final String[] ustimes = {
//            "Monday, October 17, 2548 BE 2:15:33 PM PDT",
//            "Monday 22x78-9-15 2:15:33 PM PDT",
//            "Monday, Baba 7, 1722 2:15:33 PM PDT",
//            "Monday, Tekemt 7, 1998 2:15:33 PM PDT",
//            "Monday, October 17, 2005 2:15:33 PM PDT",
//            "Monday, Tishri 14, 5766 2:15:33 PM PDT",
//            "Monday, Ramadan 14, 1426 2:15:33 PM PDT",
//            "Monday, Ramadan 14, 1426 2:15:33 PM PDT",
//            "Monday, October 17, 17 Heisei 2:15:33 PM PDT",
//        };
//        final String[] detimes = {
//            "Montag, Oktober 17, 2548 BE 2:15:33 nachm. GMT-07:00",
//            "Montag 22x78-9-15 2:15:33 nachm. GMT-07:00",
//            "Montag, 7. Baba 1722 14:15 Uhr GMT-07:00",
//            "Montag, 7. Tekemt 1998 14:15 Uhr GMT-07:00",
//            "Montag, 17. Oktober 2005 14:15 Uhr GMT-07:00",
//            "Montag, 14. Tishri 5766 14:15 Uhr GMT-07:00",
//            "Montag, 14. Ramadan 1426 14:15 Uhr GMT-07:00",
//            "Montag, 14. Ramadan 1426 14:15 Uhr GMT-07:00",
//            "Montag, Oktober 17, 17 Heisei 2:15:33 nachm. GMT-07:00",
//        };
//        
//        ULocale[] locales = {ULocale.US, ULocale.GERMANY };
//        String[][] times = { ustimes, detimes };
//        for (int j = 0; j < locales.length; ++j) {
//            ULocale ul = new ULocale("en_US");
//            for (int i = 0; i < calendars.length; ++i) {
//                ul = ul.setKeywordValue("calendar", calendars[i]);
//                Calendar cal = Calendar.getInstance(ul);
//                DateFormat fmt = cal.getDateTimeFormat(DateFormat.FULL, DateFormat.FULL, locales[j]);
//                String result = fmt.format(time);
//                System.out.println(calendars[i] + ": " + result);
//                if (!result.equals(times[j][i])) {
//                    fail("calendar: " + calendars[i]);
//                }
//            }
//        }
//    }
}
