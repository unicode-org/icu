/**
*******************************************************************************
* Copyright (C) 2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/CalendarFactory.java,v $ 
* $Date: 2003/10/02 20:50:25 $ 
* $Revision: 1.5 $
*
*******************************************************************************
*/
package com.ibm.icu.util;

import com.ibm.icu.util.TimeZone;
import java.util.Locale;
/**
 * @prototype
 */
interface CalendarFactory {
    public Calendar create(TimeZone tz, Locale loc);
    public String factoryName();
}
        
