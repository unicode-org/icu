// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl.data;

import com.ibm.icu.util.EasterHoliday;
import com.ibm.icu.util.Holiday;
import com.ibm.icu.util.SimpleHoliday;
import java.util.Calendar;
import java.util.ListResourceBundle;

public class HolidayBundle_de_DE extends ListResourceBundle {
    private static final Holiday[] fHolidays = {
        SimpleHoliday.NEW_YEARS_DAY,
        SimpleHoliday.MAY_DAY,
        new SimpleHoliday(Calendar.JUNE, 15, Calendar.WEDNESDAY, "Memorial Day"),
        new SimpleHoliday(Calendar.OCTOBER, 3, 0, "Unity Day"),
        SimpleHoliday.ALL_SAINTS_DAY,
        new SimpleHoliday(Calendar.NOVEMBER, 18, 0, "Day of Prayer and Repentance"),
        SimpleHoliday.CHRISTMAS,
        SimpleHoliday.BOXING_DAY,

        // Easter and related holidays
        EasterHoliday.GOOD_FRIDAY,
        EasterHoliday.EASTER_SUNDAY,
        EasterHoliday.EASTER_MONDAY,
        EasterHoliday.ASCENSION,
        EasterHoliday.WHIT_SUNDAY,
        EasterHoliday.WHIT_MONDAY,
    };

    private static final Object[][] fContents = {
        {"holidays", fHolidays},
    };

    @Override
    public synchronized Object[][] getContents() {
        return fContents;
    }
}
