/*
 * $RCSfile: HebrewCalendarSymbols.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:55 $
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
 * Default Date Format symbols for the Hebrew Calendar
 */
public class HebrewCalendarSymbols extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "MonthNames", new String[] { 
                "Tishri",
                "Heshvan",
                "Kislev",
                "Tevet",
                "Shevat",
                "Adar I",       // Leap years only
                "Adar",
                "Nisan",
                "Iyar",
                "Sivan",
                "Tamuz",
                "Av",
                "Elul",
            } },
        { "Eras", new String[] { 
                "AM"
            } },
    };
        
    public synchronized Object[][] getContents() {
        return fContents;
    }
};