/*
 * $RCSfile: IslamicCalendarSymbols_iw.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:56 $
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
 * Default Date Format symbols for the Islamic Calendar
 */
public class IslamicCalendarSymbols_iw extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
            "\u05DE\u05D5\u05D7\u05E8\u05DD",                                               // Muharram
            "\u05E1\u05E4\u05E8",                                                           // Safar
            "\u05E8\u05D1\u05D9\u05E2 \u05D0\u05DC-\u05D0\u05D5\u05D5\u05D0\u05DC",         // Rabi' al-awwal
            "\u05E8\u05D1\u05D9\u05E2 \u05D0\u05DC-\u05EA\u05E0\u05D9",                     // Rabi' al-thani
            "\u05D2'\u05D5\u05DE\u05D3\u05D4 \u05D0\u05DC-\u05D0\u05D5\u05D5\u05D0\u05DC",  // Jumada al-awwal
            "\u05D2'\u05D5\u05DE\u05D3\u05D4 \u05D0\u05DC-\u05EA\u05E0\u05D9",              // Jumada al-thani
            "\u05E8\u05D2'\u05D0\u05D1",                                                    // Rajab
            "\u05E9\u05E2\u05D1\u05D0\u05DF",                                               // Sha'ban
            "\u05E8\u05D0\u05DE\u05D3\u05DF",                                               // Ramadan
            "\u05E9\u05D5\u05D5\u05D0\u05DC",                                               // Shawwal
            "\u05E9\u05D5\u05D5\u05D0\u05DC",                                               // Dhu al-Qi'dah
            "\u05D6\u05D5 \u05D0\u05DC-\u05D7\u05D9\u05D2'\u05D4",                          // Dhu al-Hijjah
            } },
        { "Eras", new String[] {
            "\u05E9\u05E0\u05EA \u05D4\u05D9\u05D2'\u05E8\u05D4"
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};