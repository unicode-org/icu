
package com.ibm.icu.impl;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import com.ibm.icu.util.UResourceBundle;

/**
 * just a wrapper for Java ListResourceBundles and 
 * @author ram
 *
 */
public class ResourceBundleWrapper extends UResourceBundle {
    ResourceBundle bundle = null;
    public ResourceBundleWrapper(String baseName, String localeID, ClassLoader loader){
        bundle = ResourceBundle.getBundle(baseName, new Locale(localeID),loader);
        this.localeID = localeID;
        this.baseName = baseName;
    }
    protected Object handleGetObject(String key){
       return bundle.getObject(key);   
    }
    public Enumeration getKeys(){
        return bundle.getKeys();   
    }
}
