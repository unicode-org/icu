// HolidayBundle_fr_FR

package com.ibm.util.resources;

import com.ibm.util.*;
import java.util.Calendar;
import java.util.ListResourceBundle;

public class HolidayBundle_fr_FR extends ListResourceBundle {
    static private final Holiday[] fHolidays = {
        SimpleHoliday.NEW_YEARS_DAY,
        new SimpleHoliday(Calendar.MAY,        1,  0, "Labor Day"),
        new SimpleHoliday(Calendar.MAY,        8,  0, "Victory Day"),
        new SimpleHoliday(Calendar.JULY,      14,  0, "Bastille Day"),
        SimpleHoliday.ASSUMPTION,
        SimpleHoliday.ALL_SAINTS_DAY,
        new SimpleHoliday(Calendar.NOVEMBER,  11,  0, "Armistice Day"),
        SimpleHoliday.CHRISTMAS,

        // Easter and related holidays
        EasterHoliday.EASTER_SUNDAY,
        EasterHoliday.EASTER_MONDAY,
        EasterHoliday.ASCENSION,
        EasterHoliday.WHIT_SUNDAY,
        EasterHoliday.WHIT_MONDAY,
    };
    static private final Object[][] fContents = {
        { "holidays",   fHolidays },
    };
    public synchronized Object[][] getContents() { return fContents; }
};
