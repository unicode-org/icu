// HolidayBundle_da

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
