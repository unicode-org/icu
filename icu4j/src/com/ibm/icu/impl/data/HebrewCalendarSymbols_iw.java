/*
 * $RCSfile: HebrewCalendarSymbols_iw.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:55 $
 *
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 *
 */
package com.ibm.util.resources;

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