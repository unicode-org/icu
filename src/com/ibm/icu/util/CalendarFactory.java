/**
*******************************************************************************
* Copyright (C) 2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/CalendarFactory.java,v $ 
* $Date: 2002/12/17 07:31:26 $ 
* $Revision: 1.3 $
*
*******************************************************************************
*/
package com.ibm.icu.util;

import java.util.Locale;
/**
 * @prototype
 */
interface CalendarFactory {
    public Calendar create(TimeZone tz, Locale loc);
    public String factoryName();
}
        
