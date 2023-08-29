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

public class HolidayBundle_de_AT extends ListResourceBundle {
    static private final Holiday[] fHolidays = {
        SimpleHoliday.NEW_YEARS_DAY,
        SimpleHoliday.EPIPHANY,
        EasterHoliday.GOOD_FRIDAY,
        EasterHoliday.EASTER_SUNDAY,
        EasterHoliday.EASTER_MONDAY,
        EasterHoliday.ASCENSION,
        EasterHoliday.WHIT_SUNDAY,
        EasterHoliday.WHIT_MONDAY,
        EasterHoliday.CORPUS_CHRISTI,
        SimpleHoliday.ASSUMPTION,
        SimpleHoliday.ALL_SAINTS_DAY,
        SimpleHoliday.IMMACULATE_CONCEPTION,
        SimpleHoliday.CHRISTMAS,
        SimpleHoliday.ST_STEPHENS_DAY,

        new SimpleHoliday(Calendar.MAY,        1,  0,               "National Holiday"),
        new SimpleHoliday(Calendar.OCTOBER,   31, -Calendar.MONDAY, "National Holiday"),
    };

    static private final Object[][] fContents = {
        { "holidays",   fHolidays },

        // Only holidays names different from those used in Germany are listed here
        {   "Christmas",        "Christtag" },
        {   "New Year's Day",   "Neujahrstag" },
    };
    @Override
    public synchronized Object[][] getContents() { return fContents; }
}
