/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/CalendarData_te_IN.java,v $
 * $Date: 2002/08/13 23:36:19 $
 * $Revision: 1.3 $
 *
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
