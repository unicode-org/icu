package com.ibm.util.resources;
import java.util.ListResourceBundle;

// Default
public class CalendarData extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Weekend",
                new String[] { // Saturday:Sunday
                    "7", "0", // onset dow, millis in day
                    "2", "0"  // cease dow, millis in day
                }
            },
        };
    }
}
