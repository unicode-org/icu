/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/resources/Attic/IslamicCalendarSymbols_nl.java,v $ 
 * $Date: 2000/09/19 18:37:36 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.util.resources;

import java.util.ListResourceBundle;

/**
 * Dutch date format symbols for the Islamic Calendar
 */
public class IslamicCalendarSymbols_nl extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
            "Moeharram",            // Muharram
            "Safar",                // Safar
            "Rabi'a al awal ",      // Rabi' al-awwal
            "Rabi'a al thani",      // Rabi' al-thani
            "Joemad'al awal",       // Jumada al-awwal
            "Joemad'al thani",      // Jumada al-thani
            "Rajab",                // Rajab
            "Sja'aban",             // Sha'ban
            "Ramadan",              // Ramadan
            "Sjawal",               // Shawwal
            "Doe al ka'aba",        // Dhu al-Qi'dah
            "Doe al hizja",         // Dhu al-Hijjah
            } },
        { "Eras", new String[] {
            "Sa'na Hizjria"
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
