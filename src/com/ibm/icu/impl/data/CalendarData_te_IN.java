/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

// Telugu, India
public class CalendarData_te_IN extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Weekend",
                new String[] { // Sunday
                    "1", "0", // onset dow, millis in day
                    "2", "0"  // cease dow, millis in day
                }
            },
        };
    }
}
