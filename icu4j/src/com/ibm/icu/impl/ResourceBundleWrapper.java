/*
******************************************************************************
* Copyright (C) 2004, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.impl;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * just a wrapper for Java ListResourceBundles and 
 * @author ram
 *
 */
public class ResourceBundleWrapper extends UResourceBundle {
    ResourceBundle bundle = null;
    public ResourceBundleWrapper(String baseName, String localeID, ClassLoader loader){
        if(baseName.indexOf('.')>-1){
            bundle = ResourceBundle.getBundle(baseName, LocaleUtility.getLocaleFromName(localeID), loader);   
        }else{
            bundle = ICULocaleData.getResourceBundle(baseName, new ULocale(localeID));
            if(bundle==null){
                throw new MissingResourceException("Can't find the bundle "
                        +baseName
                        +", locale "+localeID,
                        this.getClass().getName(),
                        localeID);  
            }
        }
    }
    private int loadingStatus = -1;
    protected void setLoadingStatus(int newStatus){
        loadingStatus = newStatus;
    }
    protected Object handleGetObject(String key){
       return bundle.getObject(key);   
    }
    public Enumeration getKeys(){
        return bundle.getKeys();   
    }
    
    protected String getLocaleID(){
        return bundle.getLocale().toString();   
    }
 
    protected String getBaseName(){
        return bundle.getClass().getName().replace('.','/');   
    }
    
    public ULocale getULocale(){
        return ULocale.forLocale(bundle.getLocale());   
    }
    
    public UResourceBundle getParent(){
        return (UResourceBundle)parent;   
    }

}
