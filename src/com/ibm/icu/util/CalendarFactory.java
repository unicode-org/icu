package com.ibm.icu.util;

import java.util.Locale;
/**
 * @prototype
 */
interface CalendarFactory {
    public Calendar create(TimeZone tz, Locale loc);
    public String factoryName();
}
        
