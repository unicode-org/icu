/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

// Bahasa Malaysia, Malaysia
public class CalendarData_ms_MY extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Weekend",
                new String[] { // Saturday - half day in some offices:Sunday
                    "0", "0", // onset dow, millis in day
                    "0", "0"  // cease dow, millis in day
                }
            },
        };
    }
}
