// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2009-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.impl;

import java.util.MissingResourceException;

import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.VersionInfo;

public final class ICUDataVersion {
    private static final String U_ICU_VERSION_BUNDLE = "icuver";
    
    private static final String U_ICU_DATA_KEY = "DataVersion";
    
    /**
     * This function retrieves the data version from icuver and returns a VersionInfo object with that version information.
     *
     * @return Current icu data version
     */
    public static VersionInfo getDataVersion() {
        UResourceBundle icudatares = null;
        try {
            icudatares = UResourceBundle.getBundleInstance(
                    ICUData.ICU_BASE_NAME,
                    ICUDataVersion.U_ICU_VERSION_BUNDLE,
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            icudatares = icudatares.get(ICUDataVersion.U_ICU_DATA_KEY);
        } catch (MissingResourceException ex) {
            return null;
        }
        
        return  VersionInfo.getInstance(icudatares.getString());
    }
}
