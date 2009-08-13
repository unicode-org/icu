/*
*******************************************************************************
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.impl;

import com.ibm.icu.util.VersionInfo;
import com.ibm.icu.util.UResourceBundle;

import java.util.MissingResourceException;

public final class ICUDataVersion {
    private static final String U_ICU_VERSION_BUNDLE = "icuver";
    private static final String U_ICU_STD_BUNDLE = "icustd";
    
    private static final String U_ICU_DATA_KEY = "DataVersion";
    
    /**
     * This function loads up icuver and compares the data version to the wired-in ICU_DATA_VERSION.
     * If icuver shows something less than ICU_DATA_VERSION it returns TRUE, else FALSE. The version
     * found will be returned in the first fillin parameter (if non-null), and *isModified will be set
     * to TRUE if "icustd" is NOT found. Thus, if the data has been repackaged or modified, "icustd"
     * (standard ICU) will be missing, and the function will alert the caller that the data is not standard.
     * 
     * @param dataVersionFillin icuver data version information to be filled in if not-null
     * @param status stores the error code from the calls to resource bundle
     *
     * @return TRUE if ICU_DATA_VERSION is newer than icuver, else FALSE
     * 
     * @draft ICU 4.4
     */
    public static boolean isDataOlder(VersionInfo dataVersionFillin) {
        boolean result = true;
        
        VersionInfo dataVersion = getDataVersion();
        
        if (dataVersion!= null) {
            if (dataVersion.compareTo(VersionInfo.ICU_DATA_VERSION) != -1) {
                result = false;
            }
            
            if (dataVersionFillin != null) {
                dataVersionFillin = VersionInfo.getInstance(dataVersion.toString());
            }
        }
        
        return result;
    }
    
    /**
     * This function tests whether "icustd" is available in the data. If the data has been repackaged or modified, "icustd"
     * (standard ICU) will be missing, and the function will alert the caller that the data is not standard.
     *
     * @return TRUE if data has been modified, else FALSE
     * 
     * @draft ICU 4.4
     */
    public static boolean isDataModified() {
        if (hasICUSTDBundle()) {
            return false;
        }
        return true;
    }
    
    /**
     * This function retrieves the data version from icuver and returns a VersionInfo object with that version information.
     *
     * @return Current icu data version
     * 
     * @draft ICU 4.4
     */
    public static VersionInfo getDataVersion() {
        UResourceBundle icudatares = null;
        try {
            icudatares = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, ICUDataVersion.U_ICU_VERSION_BUNDLE, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            icudatares = icudatares.get(ICUDataVersion.U_ICU_DATA_KEY);
        } catch (MissingResourceException ex) {
            return null;
        }
        
        return  VersionInfo.getInstance(icudatares.getString());
    }
    
    private static boolean hasICUSTDBundle() {
        try {
            UResourceBundle.getBundleInstance(ICUDataVersion.U_ICU_STD_BUNDLE);
        } catch (MissingResourceException ex) {
            return false;
        }
        
        return true;
    }
    
}
