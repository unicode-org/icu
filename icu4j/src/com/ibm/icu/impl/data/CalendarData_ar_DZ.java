package com.ibm.util.resources;
import java.util.ListResourceBundle;

// Arabic, Algeria
public class CalendarData_ar_DZ extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Weekend",
                new String[] { // Thursday:Friday
                    "5", "0", // onset dow, millis in day
                    "7", "0"  // cease dow, millis in day
                }
            },
        };
    }
}
