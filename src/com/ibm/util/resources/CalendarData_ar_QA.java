package com.ibm.util.resources;
import java.util.ListResourceBundle;

// Arabic, Qatar
public class CalendarData_ar_QA extends ListResourceBundle {
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
