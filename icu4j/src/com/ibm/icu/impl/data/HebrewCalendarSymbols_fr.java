/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * French date format symbols for the Hebrew Calendar.
 * This data actually applies to French Canadian.  If we receive
 * official French data from our France office, we should move the 
 * French Canadian data (if it's different) down into _fr_CA
 */
public class HebrewCalendarSymbols_fr extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
                "Tisseri",      // Tishri
                "Hesvan",       // Heshvan
                "Kislev",       // Kislev
                "T\u00e9beth",  // Tevet
                "Sch\u00e9bat", // Shevat
                "Adar",         // Adar I
                "Adar II",      // Adar
                "Nissan",       // Nisan
                "Iyar",         // Iyar
                "Sivan",        // Sivan
                "Tamouz",       // Tamuz
                "Ab",           // Av
                "Elloul",       // Elul
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
