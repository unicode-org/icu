/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

public class HolidayBundle_it extends ListResourceBundle {
    static private final Object[][] fContents =
    {
        {   "All Saints' Day",      "Ognissanti" },
        {   "Armistice Day",        "armistizio" },
        {   "Ascension",            "ascensione" },
        {   "Ash Wednesday",        "mercoled\u00ec delle ceneri" },
        {   "Boxing Day",           "Santo Stefano" },
        {   "Christmas",            "natale" },
        {   "Easter Sunday",        "pasqua" },
        {   "Epiphany",             "Epifania" },
        {   "Good Friday",          "venerd\u00ec santo" },
        {   "Halloween",            "vigilia di Ognissanti" },
        {   "Maundy Thursday",      "gioved\u00ec santo" },
        {   "New Year's Day",       "anno nuovo" },
        {   "Palm Sunday",          "domenica delle palme" },
        {   "Pentecost",            "di Pentecoste" },
        {   "Shrove Tuesday",       "martedi grasso" },
        {   "St. Stephen's Day",    "Santo Stefano" },
        {   "Thanksgiving",         "Giorno del Ringraziamento" },

    };
    public synchronized Object[][] getContents() { return fContents; }
};
