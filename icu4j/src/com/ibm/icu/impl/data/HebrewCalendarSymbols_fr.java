/*
 * $RCSfile: HebrewCalendarSymbols_fr.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:55 $
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
 * French date format symbols for the Hebrew Calendar.
 * This data actually applies to French Canadian.  If we receive
 * official French data from our France office, we should move the 
 * French Canadian data (if it's different) down into _fr_CA
 */
public class HebrewCalendarSymbols_fr extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] {
                "Tisseri",      // Tishri
                "Hesvan",       // Heshvan
                "Kislev",       // Kislev
                "T\u00e9beth",  // Tevet
                "Sch\u00e9bat", // Shevat
                "Adar",         // Adar I
                "Adar II",      // Adar
                "Nissan",       // Nisan
                "Iyar",         // Iyar
                "Sivan",        // Sivan
                "Tamouz",       // Tamuz
                "Ab",           // Av
                "Elloul",       // Elul
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};