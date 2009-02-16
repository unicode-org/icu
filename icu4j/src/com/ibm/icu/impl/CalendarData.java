/*
 *******************************************************************************
 * Copyright (C) 2004-2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl;

import java.util.ArrayList;
import java.util.MissingResourceException;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;



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
     * @internal
     */
    public ICUResourceBundle get(String key) {
        try {
            return fBundle.getWithFallback("calendar/" + fMainType + "/" + key);
        } catch(MissingResourceException m) {
            if(fFallbackType != null) {
                return fBundle.getWithFallback("calendar/" + fFallbackType + "/" + key);
            }
            throw m;
            
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
     * @internal
     */
    public ICUResourceBundle get(String key, String subKey) {
        try {
            return fBundle.getWithFallback("calendar/" + fMainType + "/" + key + "/format/" + subKey);
        } catch(MissingResourceException m) {
            if(fFallbackType != null) {
                return fBundle.getWithFallback("calendar/" + fFallbackType + "/" + key + "/format/" + subKey);
            }
            throw m;
            
        }       
    }

    /**
     * Load data for calendar. Note, this object owns the resources, do NOT call ures_close()!
     * data is located in:   "calendar/key/contextKey/subKey"
     * for example,  calendar/dayNames/stand-alone/narrow
     *
     * @param key Resource key to data
     * @param contextKey Resource key to data
     * @param subKey Resource key to data
     * @internal
     */
    public ICUResourceBundle get(String key, String contextKey, String subKey) {
        try {
            return fBundle.getWithFallback("calendar/" + fMainType + "/" + key + "/" + contextKey + "/" + subKey);
        } catch(MissingResourceException m) {
            if(fFallbackType != null) {
                return fBundle.getWithFallback("calendar/" + fFallbackType + "/" + key + "/" + contextKey + "/" + subKey);
            }
            throw m;
            
        }       
    }
    
    public String[] getStringArray(String key) {
        return get(key).getStringArray();
    }

    public String[] getStringArray(String key, String subKey) {
        return get(key, subKey).getStringArray();
    }

    public String[] getStringArray(String key, String contextKey, String subKey) {
        return get(key, contextKey, subKey).getStringArray();
    }
    public String[] getEras(String subkey){
        ICUResourceBundle bundle = get("eras/"+subkey);
        return bundle.getStringArray();
    }
    public String[] getDateTimePatterns(){
        ICUResourceBundle bundle = get("DateTimePatterns");
        ArrayList list = new ArrayList();
        UResourceBundleIterator iter = bundle.getIterator();
        while (iter.hasNext()) {
            UResourceBundle patResource = iter.next();
            int resourceType = patResource.getType();
            switch (resourceType) {
                case UResourceBundle.STRING:
                    list.add(patResource.getString());
                    break;
                case UResourceBundle.ARRAY:
                    String[] items = patResource.getStringArray();
                    list.add(items[0]);
                    break;
            }
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    public String[] getOverrides(){
        ICUResourceBundle bundle = get("DateTimePatterns");
        ArrayList list = new ArrayList();
        UResourceBundleIterator iter = bundle.getIterator();
        while (iter.hasNext()) {
            UResourceBundle patResource = iter.next();
            int resourceType = patResource.getType();
            switch (resourceType) {
                case UResourceBundle.STRING:
                    list.add(null);
                    break;
                case UResourceBundle.ARRAY:
                    String[] items = patResource.getStringArray();
                    list.add(items[1]);
                    break;
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    public ULocale getULocale() {
        return fBundle.getULocale();
    }

    private ICUResourceBundle fBundle;
    private String fMainType;
    private String fFallbackType;
}
