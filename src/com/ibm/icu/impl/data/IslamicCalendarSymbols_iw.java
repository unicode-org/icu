/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Default Date Format symbols for the Islamic Calendar
 */
public class IslamicCalendarSymbols_iw extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
            "\u05DE\u05D5\u05D7\u05E8\u05DD",                                               // Muharram
            "\u05E1\u05E4\u05E8",                                                           // Safar
            "\u05E8\u05D1\u05D9\u05E2 \u05D0\u05DC-\u05D0\u05D5\u05D5\u05D0\u05DC",         // Rabi' al-awwal
            "\u05E8\u05D1\u05D9\u05E2 \u05D0\u05DC-\u05EA\u05E0\u05D9",                     // Rabi' al-thani
            "\u05D2'\u05D5\u05DE\u05D3\u05D4 \u05D0\u05DC-\u05D0\u05D5\u05D5\u05D0\u05DC",  // Jumada al-awwal
            "\u05D2'\u05D5\u05DE\u05D3\u05D4 \u05D0\u05DC-\u05EA\u05E0\u05D9",              // Jumada al-thani
            "\u05E8\u05D2'\u05D0\u05D1",                                                    // Rajab
            "\u05E9\u05E2\u05D1\u05D0\u05DF",                                               // Sha'ban
            "\u05E8\u05D0\u05DE\u05D3\u05DF",                                               // Ramadan
            "\u05E9\u05D5\u05D5\u05D0\u05DC",                                               // Shawwal
            "\u05E9\u05D5\u05D5\u05D0\u05DC",                                               // Dhu al-Qi'dah
            "\u05D6\u05D5 \u05D0\u05DC-\u05D7\u05D9\u05D2'\u05D4",                          // Dhu al-Hijjah
            } },
        { "Eras", new String[] {
            "\u05E9\u05E0\u05EA \u05D4\u05D9\u05D2'\u05E8\u05D4"
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
