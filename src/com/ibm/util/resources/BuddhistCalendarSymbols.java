/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/resources/Attic/BuddhistCalendarSymbols.java,v $ 
 * $Date: 2000/09/19 18:37:36 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.util.resources;

import java.util.ListResourceBundle;

/**
 * Default Date Format symbols for the Buddhist Calendar
 */
public class BuddhistCalendarSymbols extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "Eras", new String[] {
                "BE"
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
