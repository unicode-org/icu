/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.impl.ZoneMeta;
import com.ibm.icu.util.ULocale;

import java.io.Serializable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class abstracts access to calendar (Calendar and DateFormat) data.
 * @internal ICU 3.0
 */
public class CalendarData {
    /**
     * Construct a CalendarData from the given locale.
     * @param loc locale to use. The 'calendar' keyword will be ignored.
     * @param type calendar type. NULL indicates the gregorian calendar. 
     * No default lookup is done.
     * @param status error code
     */
    public CalendarData(ULocale loc, String type) {
        this((ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, loc), type);
    }
    
    public CalendarData(ICUResourceBundle b, String type) {
        fBundle = b;
        if((type == null) || (type.equals("")) || (type.equals("gregorian"))) {
            fMainType = "gregorian";
            fFallbackType = null;
        } else {
            fMainType = type;
            fFallbackType ="gregorian";
        }
    }
    
    /**
     * Load data for calendar. Note, this object owns the resources, do NOT call ures_close()!
     *
     * @param key Resource key to data
     * @param status Error Status
     * @internal
     */
    public ICUResourceBundle get(String key) {
        try {
            return (ICUResourceBundle)fBundle.getWithFallback("calendar/" + fMainType + "/" + key);
        } catch(MissingResourceException m) {
            if(fFallbackType != null) {
                return (ICUResourceBundle)fBundle.getWithFallback("calendar/" + fFallbackType + "/" + key);
            } else {
                throw m;
            }
        }       
    }

    /**
     * Load data for calendar. Note, this object owns the resources, do NOT call ures_close()!
     * There is an implicit key of 'format'
     * data is located in:   "calendar/key/format/subKey"
     * for example,  calendar/dayNames/format/abbreviated
     *
     * @param key Resource key to data
     * @param subKey Resource key to data
     * @param status Error Status
     * @internal
     */
    public ICUResourceBundle get(String key, String subKey) {
        try {
            return (ICUResourceBundle)fBundle.getWithFallback("calendar/" + fMainType + "/" + key + "/format/" + subKey);
        } catch(MissingResourceException m) {
            if(fFallbackType != null) {
                return (ICUResourceBundle)fBundle.getWithFallback("calendar/" + fFallbackType + "/" + key + "/format/" + subKey);
            } else {
                throw m;
            }
        }       
    }
    
    public String[] getStringArray(String key) {
        return get(key).getStringArray();
    }

    public String[] getStringArray(String key, String subKey) {
        return get(key, subKey).getStringArray();
    }

    public ULocale getULocale() {
        return fBundle.getULocale();
    }

    private ICUResourceBundle fBundle;
    private String fMainType;
    private String fFallbackType;
}
