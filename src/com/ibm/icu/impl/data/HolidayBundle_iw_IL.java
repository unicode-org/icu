/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/HolidayBundle_iw_IL.java,v $ 
 * $Date: 2002/02/16 03:05:48 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.impl.data;

import com.ibm.icu.util.*;
import java.util.Calendar;
import java.util.ListResourceBundle;

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
};
