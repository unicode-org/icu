// HolidayBundle_iw_IL

package com.ibm.util.resources;

import com.ibm.util.*;
import java.util.Calendar;
import java.util.ListResourceBundle;

public class HolidayBundle_iw_IL extends ListResourceBundle {
    static private final Holiday[] fHolidays = {
        HebrewHoliday.ROSH_HASHANAH,
        HebrewHoliday.YOM_KIPPUR,
        HebrewHoliday.HANUKKAH,
        HebrewHoliday.PURIM,
        HebrewHoliday.PASSOVER,
        HebrewHoliday.SHAVUOT,
        HebrewHoliday.SELIHOT,
    };

    static private final Object[][] fContents = {
        { "holidays",   fHolidays },
    };
    public synchronized Object[][] getContents() { return fContents; }
};
