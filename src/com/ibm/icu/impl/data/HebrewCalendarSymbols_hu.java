/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/HebrewCalendarSymbols_hu.java,v $ 
 * $Date: 2002/02/16 03:05:43 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Hungarian date format symbols for the Hebrew Calendar
 */
public class HebrewCalendarSymbols_hu extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
                "Tisri",                        // Tishri
                "Hesv\u00E1n",                  // Heshvan
                "Kiszl\u00E9v",                 // Kislev
                "T\u00E9v\u00E9sz",             // Tevet
                "Sv\u00E1t",                    // Shevat
                "\u00C1d\u00E1r ris\u00F3n",    // Adar I
                "\u00C1d\u00E1r s\u00E9ni",     // Adar
                "Nisz\u00E1n",                  // Nisan
                "Ij\u00E1r",                    // Iyar
                "Sziv\u00E1n",                  // Sivan
                "Tamuz",                        // Tamuz
                "\u00C1v",                      // Av
                "Elul",                         // Elul
            } },
        { "Eras", new String[] {
                "T\u00C9"
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
