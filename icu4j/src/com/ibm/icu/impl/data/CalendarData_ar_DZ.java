/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/CalendarData_ar_DZ.java,v $
 * $Date: 2002/08/13 23:36:19 $
 * $Revision: 1.3 $
 *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

// Arabic, Algeria
public class CalendarData_ar_DZ extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Weekend",
                new String[] { // Thursday:Friday
                    "5", "0", // onset dow, millis in day
                    "7", "0"  // cease dow, millis in day
                }
            },
        };
    }
}
