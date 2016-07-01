// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 2004-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
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
        this((ICUResourceBundle)UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, loc), type);
    }
    
    public CalendarData(ICUResourceBundle b, String type) {
        fBundle = b;
        if((type == null) || (type.equals("")) || (type.equals("gregorian"))) {
            fMainType = "gregorian";
            fFallbackType = null;
        } else {
            fMainType = type;
            fFallbackType = "gregorian";
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

    public String[] getDateTimePatterns(){
        ICUResourceBundle bundle = get("DateTimePatterns");
        ArrayList<String> list = new ArrayList<String>();
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

        return list.toArray(new String[list.size()]);
    }
    
    /**
     * Returns the default date-time pattern such as <code>{1}, {0}</code>.
     * {1} is always the date and {0} is always the time.
     */
    public String getDateTimePattern() {
        // this is a hack to get offset 8 from the dateTimePatterns array.
        return _getDateTimePattern(-1);
    }
    
    private String _getDateTimePattern(int offset) {
        String[] patterns = null;
        try {
            patterns = getDateTimePatterns();
        } catch (MissingResourceException ignored) {
            // ignore. patterns remains null.
        }
        if (patterns == null || patterns.length < 9) {
            // Return hard-coded default. patterns array not available or it has too few
            // elements.
            return "{1} {0}";
        }
        if (patterns.length < 13) {
            // Offset 8 contains default pattern if we don't have per style patterns.
            return patterns[8];
        }
        // DateTimePatterns start at index 9 in the array.
        return patterns[9 + offset];
    }
        
    public String[] getOverrides(){
        ICUResourceBundle bundle = get("DateTimePatterns");
        ArrayList<String> list = new ArrayList<String>();
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
        return list.toArray(new String[list.size()]);
    }

    private ICUResourceBundle fBundle;
    private String fMainType;
    private String fFallbackType;
}
