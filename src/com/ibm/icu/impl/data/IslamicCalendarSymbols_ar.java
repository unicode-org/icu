/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/IslamicCalendarSymbols_ar.java,v $ 
 * $Date: 2002/02/16 03:05:48 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Default Date Format symbols for the Islamic Calendar
 */
public class IslamicCalendarSymbols_ar extends ListResourceBundle {
    
    private static String copyright = "Copyright \u00a9 1999 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] { 
            "\u0645\u062D\u0631\u0645",                                             // Muharram
            "\u0635\u0641\u0631",                                                   // Safar
            "\u0631\u0628\u064A\u0639 \u0627\u0644\u0623\u0648\u0644",              // Rabi' I
            "\u0631\u0628\u064A\u0639 \u0627\u0644\u0622\u062E\u0631",              // Rabi' II
            "\u062C\u0645\u0627\u062F\u0649 \u0627\u0644\u0623\u0648\u0644\u0649",  // Jumada I
            "\u062C\u0645\u0627\u062F\u0649 \u0627\u0644\u0622\u062E\u0631\u0629",  // Jumada I
            "\u0631\u062C\u0628",                                                   // Rajab
            "\u0634\u0639\u0628\u0627\u0646",                                       // Sha'ban
            "\u0631\u0645\u0636\u0627\u0646",                                       // Ramadan
            "\u0634\u0648\u0627\u0644",                                             // Shawwal
            "\u0630\u0648 \u0627\u0644\u0642\u0639\u062F\u0629",                    // Dhu'l-Qi'dah
            "\u0630\u0648 \u0627\u0644\u062D\u062C\u0629",                          // Dhu'l-Hijjah
        } },
        { "Eras", new String[] { 
            "\u0647\u200D",  // AH
        } },
    };
        
    public synchronized Object[][] getContents() {
        return fContents;
    }
};
