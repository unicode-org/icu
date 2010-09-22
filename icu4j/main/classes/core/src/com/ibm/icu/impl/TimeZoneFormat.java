/*
 *******************************************************************************
 * Copyright (C) 2010, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * @author JCEmmons
 *
 */
public class TimeZoneFormat {

    public ZoneStringFormat zsf;
    
    public static TimeZoneFormat createInstance ( ULocale loc ) {
        TimeZoneFormat tzf = new TimeZoneFormat();
        tzf.zsf = ZoneStringFormat.getInstance(loc);
        return tzf;        
    }
    
    public String format ( TimeZone tz, long date, int style ) {
        String result = null;
        switch ( style ) {
        case TimeZone.SHORT :
        case TimeZone.SHORT_COMMONLY_USED :
            result = zsf.getSpecificShortString(tz, date, style == TimeZone.SHORT_COMMONLY_USED);
            break;
        case TimeZone.LONG :
            result = zsf.getSpecificLongString(tz, date);
            break;
        case TimeZone.SHORT_GENERIC :
            result = zsf.getGenericShortString(tz, date, true);
            break;
        case TimeZone.LONG_GENERIC :
            result = zsf.getGenericLongString(tz, date);
            break;
        case TimeZone.SHORT_GMT :
            result = zsf.getShortGMTString(tz,date);
            break;
        case TimeZone.LONG_GMT :
            result = zsf.getLongGMTString(tz, date);
            break;
        case TimeZone.GENERIC_LOCATION :
            result = zsf.getGenericLocationString(tz, date);
        }
        
        return result;
    }
    
    public String format ( TimeZone tz, int style, boolean daylight ) {
        return format (tz, System.currentTimeMillis(), style, daylight);
    }
    
    public String format ( TimeZone tz, long date , int style , boolean daylight ) {
        String result = null;
        switch ( style ) {
        case TimeZone.LONG :
            if ( daylight ) {
                result = zsf.getLongDaylight(tz.getID(), date);
            } else {
                result = zsf.getLongStandard(tz.getID(),date);
            }
            break;
        
        case TimeZone.SHORT :
        case TimeZone.SHORT_COMMONLY_USED :
            if ( daylight ) {
                result = zsf.getShortDaylight(tz.getID(), date, style == TimeZone.SHORT_COMMONLY_USED);
            } else {
                result = zsf.getShortStandard(tz.getID(), date, style == TimeZone.SHORT_COMMONLY_USED);
            }
            break;
        
        case TimeZone.SHORT_GMT :
            result = zsf.getShortGMTString(tz, date, daylight);
            break;
            
        case TimeZone.LONG_GMT:
            result = zsf.getLongGMTString(tz, date, daylight);
            break;
            
        default :
            result = format ( tz, date, style );
            break;
        }
        return result;
    }

}
