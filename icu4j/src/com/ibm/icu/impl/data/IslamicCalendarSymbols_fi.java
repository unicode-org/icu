/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/IslamicCalendarSymbols_fi.java,v $ 
 * $Date: 2002/02/16 03:05:49 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Finnish date format symbols for the Islamic Calendar
 */
public class IslamicCalendarSymbols_fi extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
            "Muh\u00E1rram",                // Muharram
            "S\u00E1far",                   // Safar
            "Rab\u00ED' al-\u00E1wwal",     // Rabi' al-awwal
            "Rab\u00ED' al-\u00E1khir",     // Rabi' al-thani
            "D\u017Eumada-l-\u00FAla",      // Jumada al-awwal
            "D\u017Eumada-l-\u00E1khira",   // Jumada al-thani
            "Rad\u017Eab",                  // Rajab
            "\u0160a'b\u00E1n",             // Sha'ban
            "Ramad\u00E1n",                 // Ramadan
            "\u0160awwal",                  // Shawwal
            "Dhu-l-qada",                   // Dhu al-Qi'dah
            "Dhu-l-hidd\u017Ea",            // Dhu al-Hijjah
            } },
        { "Eras", new String[] {
            "AH"                            // Anno Hid\u017Era
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
