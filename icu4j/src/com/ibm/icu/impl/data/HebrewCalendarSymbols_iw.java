/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/Attic/HebrewCalendarSymbols_iw.java,v $ 
 * $Date: 2002/02/16 03:05:43 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Hebrew-language date format symbols for the Hebrew Calendar
 */
public class HebrewCalendarSymbols_iw extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] { 
                "\u05EA\u05E9\u05E8\u05D9",                 // Tishri
                "\u05D7\u05E9\u05D5\u05DF",                 // Heshvan
                "\u05DB\u05E1\u05DC\u05D5",                 // Kislev
                "\u05D8\u05D1\u05EA",                       // Tevet
                "\u05E9\u05D1\u05D8",                       // Shevat
                "\u05E9\u05D1\u05D8",                       // Adar I
                "\u05D0\u05D3\u05E8 \u05E9\u05E0\u05D9",    // Adar
                "\u05E0\u05D9\u05E1\u05DF",                 // Nisan
                "\u05D0\u05D9\u05D9\u05E8",                 // Iyar
                "\u05E1\u05D9\u05D5\u05DF",                 // Sivan
                "\u05EA\u05DE\u05D5\u05D6",                 // Tamuz
                "\u05D0\u05D1",                             // Av
                "\u05D0\u05DC\u05D5\u05DC",                 // Elul
            } },
        { "Eras", new String[] { 
                "\u05DC\u05D1\u05D4\042\u05E2"
            } },
    };
        
    public synchronized Object[][] getContents() {
        return fContents;
    }
};
