/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/CalendarData_ms_MY.java,v $
 * $Date: 2002/08/13 23:36:19 $
 * $Revision: 1.4 $
 *
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
