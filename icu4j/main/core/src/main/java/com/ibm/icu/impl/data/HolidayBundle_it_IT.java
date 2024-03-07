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

public class HolidayBundle_it_IT extends ListResourceBundle {
    static private final Holiday[] fHolidays = {
        SimpleHoliday.NEW_YEARS_DAY,
        SimpleHoliday.EPIPHANY,
        new SimpleHoliday(Calendar.APRIL,      1,  0,    "Liberation Day"),
        new SimpleHoliday(Calendar.MAY,        1,  0,    "Labor Day"),
        SimpleHoliday.ASSUMPTION,
        SimpleHoliday.ALL_SAINTS_DAY,
        SimpleHoliday.IMMACULATE_CONCEPTION,
        SimpleHoliday.CHRISTMAS,
        new SimpleHoliday(Calendar.DECEMBER,  26,  0,    "St. Stephens Day"),
        SimpleHoliday.NEW_YEARS_EVE,

        // Easter and related holidays
        EasterHoliday.EASTER_SUNDAY,
        EasterHoliday.EASTER_MONDAY,
    };
    static private final Object[][] fContents = {
        { "holidays",           fHolidays },
    };
    @Override
    public synchronized Object[][] getContents() { return fContents; }
}
