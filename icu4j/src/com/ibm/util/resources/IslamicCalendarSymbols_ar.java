/*
 * $RCSfile: IslamicCalendarSymbols_ar.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:56 $
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