/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

/**
 * Thai version of the Date Format symbols for the Buddhist calendar
 */
public class BuddhistCalendarSymbols_th extends ListResourceBundle {

    private static String copyright = "Copyright \u00a9 1998 IBM Corp. All Rights Reserved.";

    static final Object[][] fContents = {
        { "Eras", new String[] {
                "\u0e1e.\u0e28."
            } },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
};
