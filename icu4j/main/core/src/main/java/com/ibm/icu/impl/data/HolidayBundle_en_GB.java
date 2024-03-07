// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl.data;

import java.util.Calendar;
import java.util.ListResourceBundle;

import com.ibm.icu.util.EasterHoliday;
import com.ibm.icu.util.Holiday;
import com.ibm.icu.util.SimpleHoliday;

public class HolidayBundle_en_GB extends ListResourceBundle
{
    static private final Holiday[] fHolidays = {
        SimpleHoliday.NEW_YEARS_DAY,
        SimpleHoliday.MAY_DAY,
        new SimpleHoliday(Calendar.MAY,        31, -Calendar.MONDAY,    "Spring Holiday"),
        new SimpleHoliday(Calendar.AUGUST,     31, -Calendar.MONDAY,    "Summer Bank Holiday"),
        SimpleHoliday.CHRISTMAS,
        SimpleHoliday.BOXING_DAY,
        new SimpleHoliday(Calendar.DECEMBER,   31, -Calendar.MONDAY,    "Christmas Holiday"),

        // Easter and related holidays
        EasterHoliday.GOOD_FRIDAY,
        EasterHoliday.EASTER_SUNDAY,
        EasterHoliday.EASTER_MONDAY,
    };
    static private final Object[][] fContents = {
        { "holidays",   fHolidays },

        { "Labor Day",  "Labour Day" },
    };
    @Override
    public synchronized Object[][] getContents() { return fContents; }
}
