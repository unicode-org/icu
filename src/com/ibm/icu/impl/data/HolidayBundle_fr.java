/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/data/HolidayBundle_fr.java,v $ 
 * $Date: 2003/06/03 18:49:33 $ 
 * $Revision: 1.6 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.impl.data;

import java.util.ListResourceBundle;

public class HolidayBundle_fr extends ListResourceBundle {
    static private final Object[][] fContents = {
        {   "All Saints' Day",      "Toussaint" },
        {   "Armistice Day",        "Jour de l'Armistice" },
        {   "Ascension",            "Ascension" },
        {   "Bastille Day",         "F\u00EAte de la Bastille" },
        {   "Benito Ju\u00E1rez Day",    "F\u00EAte de Benito Ju\u00E1rez" },
        {   "Boxing Day",           "Lendemain de No\u00EBl" },
        {   "Christmas Eve",        "Veille de No\u00EBl" },
        {   "Christmas",            "No\u00EBl" },
        {   "Easter Monday",        "P\u00E2ques lundi" },
        {   "Easter Sunday",        "P\u00E2ques" },
        {   "Epiphany",             "l'\u00C9piphanie" },
        {   "Flag Day",             "F\u00EAte du Drapeau" },
        {   "Good Friday",          "Vendredi Saint" },
        {   "Halloween",            "Veille de la Toussaint" },
        {   "All Saints' Day",      "Toussaint" },
        {   "Independence Day",     "F\u00EAte Ind\u00E9pendance" },
        {   "Maundy Thursday",      "Jeudi Saint" },
        {   "Mother's Day",         "F\u00EAte des m\u00E8res" },
        {   "National Day",         "F\u00EAte Nationale" },
        {   "New Year's Day",       "Jour de l'an" },
        {   "Palm Sunday",          "les Rameaux" },
        {   "Pentecost",            "Pentec\u00F4te" },
        {   "Shrove Tuesday",       "Mardi Gras" },
        {   "St. Stephen's Day",    "Saint-\u00C9tienne" },
        {   "Victoria Day",         "F\u00EAte de la Victoria" },
        {   "Victory Day",          "F\u00EAte de la Victoire" },
    };

    public synchronized Object[][] getContents() { return fContents; }
};
