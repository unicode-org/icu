/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
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
                  "HebrewTest",
                  "IBMCalendarTest",
                  "IslamicTest",
                  "JapaneseTest",
                  "ChineseTest",
                  "HolidayTest"
              },
              "Calendars, Holiday, and Astro tests"
              );
    }

    public static final String CLASS_TARGET_NAME = "Calendar";
}
