/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/resources/Attic/HolidayBundle_ja_JP.java,v $ 
 * $Date: 2000/03/10 04:18:05 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.util.resources;

import com.ibm.util.*;
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
