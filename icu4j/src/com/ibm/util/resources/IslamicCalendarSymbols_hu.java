/*
 * $RCSfile: IslamicCalendarSymbols_hu.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:56 $
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
 * Hungarian date format symbols for the Islamic Calendar
 */
public class IslamicCalendarSymbols_hu extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
            "Moharrem",                 // Muharram
            "Safar",                    // Safar
            "R\u00E9bi el avvel",       // Rabi' al-awwal
            "R\u00E9bi el accher",      // Rabi' al-thani
            "Dsem\u00E1di el avvel",    // Jumada al-awwal
            "Dsem\u00E1di el accher",   // Jumada al-thani
            "Redseb",                   // Rajab
            "Sab\u00E1n",               // Sha'ban
            "Ramad\u00E1n",             // Ramadan
            "Sevv\u00E1l",              // Shawwal
            "Ds\u00FCl kade",           // Dhu al-Qi'dah
            "Ds\u00FCl hedse",          // Dhu al-Hijjah
            } },
        { "Eras", new String[] {
            "MF"
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};