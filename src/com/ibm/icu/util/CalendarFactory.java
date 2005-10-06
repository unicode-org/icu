/**
*******************************************************************************
* Copyright (C) 2002-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.util;

import com.ibm.icu.util.TimeZone;
import java.util.Locale;
/**
 * @prototype
 */
interface CalendarFactory {
    public Calendar create(TimeZone tz, ULocale loc);
    public String factoryName();
}
        
