/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Hungarian date format symbols for the Islamic Calendar
 */
public class IslamicCalendarSymbols_hu extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
            "Moharrem",                 // Muharram
            "Safar",                    // Safar
            "R\u00E9bi el avvel",       // Rabi' al-awwal
            "R\u00E9bi el accher",      // Rabi' al-thani
            "Dsem\u00E1di el avvel",    // Jumada al-awwal
            "Dsem\u00E1di el accher",   // Jumada al-thani
            "Redseb",                   // Rajab
            "Sab\u00E1n",               // Sha'ban
            "Ramad\u00E1n",             // Ramadan
            "Sevv\u00E1l",              // Shawwal
            "Ds\u00FCl kade",           // Dhu al-Qi'dah
            "Ds\u00FCl hedse",          // Dhu al-Hijjah
            } },
        { "Eras", new String[] {
            "MF"
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
