/*
 * $RCSfile: IslamicCalendarSymbols_fi.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:56 $
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