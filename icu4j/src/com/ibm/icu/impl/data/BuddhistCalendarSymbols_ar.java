/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.data;

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
