/*
 * $RCSfile: HebrewCalendarSymbols_fi.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:55 $
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
 * Finnish date format symbols for the Hebrew Calendar
 */
public class HebrewCalendarSymbols_fi extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "Name",       "Juutalainen kalenteri" },
        
        { "MonthNames", new String[] {
                "Ti\u0161r\u00ECkuu",       // Tishri
                "He\u0161v\u00E1nkuu",      // Heshvan
                "Kisl\u00E9vkuu",           // Kislev
                "Tev\u00E9tkuu",            // Tevet
                "\u0160evatkuu",            // Shevat
                "Ad\u00E1rkuu",             // Adar I
                "Ad\u00E1rkuu II",          // Adar
                "Nis\u00E1nkuu",            // Nisan
                "Ijj\u00E1rkuu",            // Iyar
                "Siv\u00E1nkuu",            // Sivan
                "Tamm\u00FAzkuu",           // Tamuz
                "Abkuu",                    // Av
                "El\u00FAlkuu",             // Elul
            } },

        { "Eras", new String[] { 
                "AM"                        // Anno Mundi
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};