/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

import com.ibm.icu.util.HebrewHoliday;
import com.ibm.icu.util.Holiday;

public class HolidayBundle_iw_IL extends ListResourceBundle {
    static private final Holiday[] fHolidays = {
        HebrewHoliday.ROSH_HASHANAH,
        HebrewHoliday.YOM_KIPPUR,
        HebrewHoliday.HANUKKAH,
        HebrewHoliday.PURIM,
        HebrewHoliday.PASSOVER,
        HebrewHoliday.SHAVUOT,
        HebrewHoliday.SELIHOT,
    };

    static private final Object[][] fContents = {
        { "holidays",   fHolidays },
    };
    public synchronized Object[][] getContents() { return fContents; }
}
