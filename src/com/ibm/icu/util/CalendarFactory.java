/**
*******************************************************************************
* Copyright (C) 2002-2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/CalendarFactory.java,v $ 
* $Date: 2003/12/20 03:07:08 $ 
* $Revision: 1.6 $
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
        
