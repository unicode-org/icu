package com.ibm.util.resources;
import java.util.ListResourceBundle;

// Hebrew, Israel
public class CalendarData_iw_IL extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Weekend",
                new String[] { // Friday:Saturday
                    "6", "0", // onset dow, millis in day
                    "1", "0"  // cease dow, millis in day
                }
            },
        };
    }
}
