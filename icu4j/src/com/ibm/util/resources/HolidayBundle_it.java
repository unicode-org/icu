/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/resources/Attic/HolidayBundle_it.java,v $ 
 * $Date: 2000/09/20 22:37:54 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */

package com.ibm.util.resources;

import com.ibm.util.*;
import java.util.Calendar;
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
