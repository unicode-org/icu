/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/HolidayBundle_ja_JP.java,v $ 
 * $Date: 2002/02/16 03:05:48 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.impl.data;

import com.ibm.icu.util.*;
import java.util.Calendar;
import java.util.ListResourceBundle;

public class HolidayBundle_ja_JP extends ListResourceBundle {
    static private final Holiday[] fHolidays = {
        new SimpleHoliday(Calendar.FEBRUARY,  11,  0,    "National Foundation Day"),
    };
    static private final Object[][] fContents = {
        {   "holidays",         fHolidays   },
    };
    public synchronized Object[][] getContents() { return fContents; }
};
