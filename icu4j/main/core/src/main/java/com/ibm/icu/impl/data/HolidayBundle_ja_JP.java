// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl.data;

import com.ibm.icu.util.Holiday;
import com.ibm.icu.util.SimpleHoliday;
import java.util.Calendar;
import java.util.ListResourceBundle;

public class HolidayBundle_ja_JP extends ListResourceBundle {
    private static final Holiday[] fHolidays = {
        new SimpleHoliday(Calendar.FEBRUARY, 11, 0, "National Foundation Day"),
    };
    private static final Object[][] fContents = {
        {"holidays", fHolidays},
    };

    @Override
    public synchronized Object[][] getContents() {
        return fContents;
    }
}
