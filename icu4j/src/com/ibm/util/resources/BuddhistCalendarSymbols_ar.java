/*
 * $RCSfile: BuddhistCalendarSymbols_ar.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:55 $
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
 * Default Date Format symbols for the Buddhist Calendar
 */
public class BuddhistCalendarSymbols_ar extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1999 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "Eras", new String[] {
                "\u0627\u0644\u062A\u0642\u0648\u064A\u0645 \u0627\u0644\u0628\u0648\u0630\u064A"
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};