/*
 * $RCSfile: IslamicCalendarSymbols_nl.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:56 $
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