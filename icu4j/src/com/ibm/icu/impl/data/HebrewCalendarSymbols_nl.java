/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Dutch date format symbols for the Hebrew Calendar
 */
public class HebrewCalendarSymbols_nl extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
                "Tisjrie",      // Tishri
                "Chesjwan",     // Heshvan
                "Kislev",       // Kislev
                "Tevet",        // Tevet
                "Sjevat",       // Shevat
                "Adar",         // Adar I
                "Adar B",       // Adar
                "Nisan",        // Nisan
                "Ijar",         // Iyar
                "Sivan",        // Sivan
                "Tammoez",      // Tamuz
                "Av",           // Av
                "Elloel",       // Elul
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
