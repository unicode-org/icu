/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

public class HolidayBundle_da extends ListResourceBundle
{
    static private final Object[][] fContents =
    {
        {   "Armistice Day",        "v\u00e5benhvile" },
        {   "Ascension",            "himmelfart" },
        {   "Boxing Day",           "anden juledag" },
        {   "Christmas Eve",        "juleaften" },
        {   "Easter",               "p\u00e5ske" },
        {   "Epiphany",             "helligtrekongersdag" },
        {   "Good Friday",          "langfredag" },
        {   "Halloween",            "allehelgensaften" },
        {   "Maundy Thursday",      "sk\u00e6rtorsdag" },
        {   "Palm Sunday",          "palmes\u00f8ndag" },
        {   "Pentecost",            "pinse" },
        {   "Shrove Tuesday",       "hvidetirsdag" },
    };
    public synchronized Object[][] getContents() { return fContents; }
}
