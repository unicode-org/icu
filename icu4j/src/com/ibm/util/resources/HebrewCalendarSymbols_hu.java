/*
 * $RCSfile: HebrewCalendarSymbols_hu.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:55 $
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