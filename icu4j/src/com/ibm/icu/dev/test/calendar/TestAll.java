/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/calendar/TestAll.java,v $
 * $Date: 2003/01/28 18:55:32 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.calendar;
import com.ibm.icu.dev.test.TestFmwk;
import java.util.TimeZone;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestFmwk {
    public static void main(String[] args) throws Exception {
            TimeZone.setDefault(TimeZone.getTimeZone("PST"));
            new TestAll().run(args);
    }
    public void TestCalendar() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.dev.test.calendar.AstroTest(),
            new com.ibm.icu.dev.test.calendar.CalendarRegression(),
            new com.ibm.icu.dev.test.calendar.CompatibilityTest(),
            new com.ibm.icu.dev.test.calendar.HebrewTest(),
            new com.ibm.icu.dev.test.calendar.IBMCalendarTest(),
            new com.ibm.icu.dev.test.calendar.IslamicTest(),
            new com.ibm.icu.dev.test.calendar.JapaneseTest(),
            new com.ibm.icu.dev.test.calendar.ChineseTest(),
            new com.ibm.icu.dev.test.calendar.HolidayTest()
                });
    }
}