/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/resources/Attic/HolidayBundle_da.java,v $ 
 * $Date: 2000/03/10 04:18:02 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.util.resources;

import com.ibm.util.*;
import java.util.Calendar;
import java.util.ListResourceBundle;

public class HolidayBundle_da extends ListResourceBundle
{
    static private final Object[][] fContents =
    {
        {   "Armistice Day",        "v…benhvile" },
        {   "Ascension",            "himmelfart" },
        {   "Boxing Day",           "anden juledag" },
        {   "Christmas Eve",        "juleaften" },
        {   "Easter",               "p…ske" },
        {   "Epiphany",             "helligtrekongersdag" },
        {   "Good Friday",          "langfredag" },
        {   "Halloween",            "allehelgensaften" },
        {   "Maundy Thursday",      "sk†rtorsdag" },
        {   "Palm Sunday",          "palmes°ndag" },
        {   "Pentecost",            "pinse" },
        {   "Shrove Tuesday",       "hvidetirsdag" },
    };
    public synchronized Object[][] getContents() { return fContents; }
};
