/**
*******************************************************************************
* Copyright (C) 2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/CalendarFactory.java,v $ 
* $Date: 2003/09/04 01:00:30 $ 
* $Revision: 1.4 $
*
*******************************************************************************
*/
package com.ibm.icu.util;

import java.util.Locale;
import java.util.TimeZone;
/**
 * @prototype
 */
interface CalendarFactory {
    public Calendar create(TimeZone tz, Locale loc);
    public String factoryName();
}
        
