package com.ibm.util.resources;
import java.util.ListResourceBundle;

/**
 * Default Date Format symbols for the Chinese calendar.  This is a
 * temporary class that may be removed when the ChineseDateFormat is
 * finished.
 */
public class ChineseCalendarSymbols extends ListResourceBundle {

    static final Object[][] fContents = {
        { "", "" },
    };

    public synchronized Object[][] getContents() {
        return fContents;
    }
}
