/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/ChineseCalendarSymbols.java,v $
 * $Date: 2002/08/13 23:37:48 $
 * $Revision: 1.6 $
 *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Default Date Format symbols for the Chinese calendar.
 */
public class ChineseCalendarSymbols extends ListResourceBundle {

    static final Object[][] fContents = {
        { "IsLeapMonth",
            new String[] {
                "",
                "*"
            },
        },
        { "DateTimePatterns",
            new String[] {
                "h:mm:ss a z", // full time pattern
                "h:mm:ss a z", // long time pattern
                "h:mm:ss a", // medium time pattern
                "h:mm a", // short time pattern
                // TODO Fix the following
                "EEEE y'x'G-Ml-d", // full date pattern
                "y'x'G-Ml-d", // long date pattern
                "y'x'G-Ml-d", // medium date pattern
                "y'x'G-Ml-d", // short date pattern
                "{1} {0}" // date-time pattern
            }
        },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
}
