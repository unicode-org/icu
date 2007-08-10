/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.calendar;
import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other calendar tests as a batch.
 */
public class TestAll extends TestGroup {
    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(
              new String[] {
                  "AstroTest",
                  "CalendarRegression",
                  "CompatibilityTest",
                  "CopticTest",
                  "EthiopicTest",
                  "HebrewTest",
                  "IBMCalendarTest",
                  "IslamicTest",
                  "JapaneseTest",
                  "ChineseTest",
                  "IndianTest",
                  "HolidayTest",
                  "DataDrivenCalendarTest"
              },
              "Calendars, Holiday, and Astro tests"
              );
    }

    public static final String CLASS_TARGET_NAME = "Calendar";
}
