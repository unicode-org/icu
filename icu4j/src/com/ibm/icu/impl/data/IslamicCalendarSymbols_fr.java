/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * French date format symbols for the Islamic Calendar
 * This data actually applies to French Canadian.  If we receive
 * official French data from our France office, we should move the 
 * French Canadian data (if it's different) down into _fr_CA
 */
public class IslamicCalendarSymbols_fr extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
            "Mouharram",            // Muharram
            "Safar",                // Safar
            "Rabi'-oul-Aououal",    // Rabi' al-awwal
            "Rabi'-out-Tani",       // Rabi' al-thani
            "Djoumada-l-Oula",      // Jumada al-awwal
            "Djoumada-t-Tania",     // Jumada al-thani
            "Radjab",               // Rajab
            "Cha'ban",              // Sha'ban
            "Ramadan",              // Ramadan
            "Chaououal",            // Shawwal
            "Dou-l-Qa'da",          // Dhu al-Qi'dah
            "Dou-l-Hidjja",         // Dhu al-Hijjah
            } },
        { "Eras", new String[] {
            "AH"        // Anno Hijri
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
