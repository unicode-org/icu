package com.ibm.icu.util;

import java.util.Locale;

public interface CalendarFactory {
    public Calendar create(TimeZone tz, Locale loc);
    public String factoryName();
}
        
