/*
 * (C) Copyright IBM Corp. 2002 - All Rights Reserved
 */

package com.ibm.icu.impl;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * Provides information about and access to resource bundles in the
 * com.ibm.text.resources package.  Unlike the java version, this does
 * not include resources from any other location.  In particular, it
 * does not look in the boot or system class path.
 */
public class ICULocaleData {
    private static Locale[] localeList;
    private static final String PACKAGE1 = "com.ibm.icu.impl.data";
    private static final String[] packageNames = { PACKAGE1 };
    private static boolean debug = ICUDebug.enabled("localedata");

    /**
     * Returns a list of the installed locales.
     * @param key A resource tag.  Currently, this parameter is ignored.  The obvious
     * intent, however,  is for getAvailableLocales() to return a list of only those
     * locales that contain a resource with the specified resource tag.
     *
     * <p>Before we implement this function this way, however, some thought should be
     * given to whether this is really the right thing to do.  Because of the lookup
     * algorithm, a NumberFormat, for example, is "installed" for all locales.  But if
     * we're trying to put up a list of NumberFormats to choose from, we may want to see
     * only a list of those locales that uniquely define a NumberFormat rather than
     * inheriting one from another locale.  Thus, if fr and fr_CA uniquely define
     * NumberFormat data, but fr_BE doesn't, the user wouldn't see "French (Belgium)" in
     * the list and would go for "French (default)" instead.  Of course, this means
     * "English (United States)" would not be in the list, since it is the default locale.
     * This might be okay, but might be confusing to some users.
     *
     * <p>In addition, the other functions that call getAvailableLocales() don't currently
     * all pass the right thing for "key," meaning that all of these functions should be
     * looked at before anything is done to this function.
     *
     * <p>We recommend that someone take some careful consideration of these issues before
     * modifying this function to pay attention to the "key" parameter.  --rtg 1/26/98
     */
    public static Locale[] getAvailableLocales(String key) {
	// ignore key, just return all locales
	return getAvailableLocales();
    }
    
    /**
     * Return an array of all the locales for which we have resource information.
     */
    public static Locale[] getAvailableLocales() {
        // creating the locale list is expensive, so be careful to do it
        // only once
        if (localeList == null) {
            synchronized(ICULocaleData.class) {
                if (localeList == null) {
                    localeList = createLocaleList();
                }
            }
        }

	return (Locale[])localeList.clone();
    }

    /**
     * The default name for resources containing ICU locale data.
     */
    public static final String LOCALE_ELEMENTS = "LocaleElements";

    /**
     * Gets a LocaleElements resource bundle.
     */
    public static ResourceBundle getLocaleElements(Locale locale) {
        return getResourceBundle(LOCALE_ELEMENTS, locale);
    }

    /**
     * Gets a LocaleElements resource bundle.
     */
    public static ResourceBundle getLocaleElements(String localeName) {
	return getResourceBundle(LOCALE_ELEMENTS, localeName);
    }

    /**
     * Still need permissions to use our own class loader, is there no way
     * to load class resources from new locations that aren't already on the
     * class path?
     */
    private static ResourceBundle instantiateBundle(String name, Locale l) {
	ResourceBundle rb = ResourceBundle.getBundle(name, l);
	return rb;
    }

    /**
     * Get a resource bundle from the lookup chain.
     */
    public static ResourceBundle getResourceBundle(String bundleName, String localeName) {
	Locale locale = LocaleUtility.getLocaleFromName(localeName);
	return getResourceBundle(bundleName, locale);
    }

    /**
     * Get a resource bundle from the lookup chain.
     */
    public static ResourceBundle getResourceBundle(String bundleName, Locale locale) {
	if (locale == null) {
	    locale = Locale.getDefault();
	}
	for (int i = 0; i < packageNames.length; ++i) {
	    try {
		String path = packageNames[i] + "." + bundleName;
		if (debug) System.out.println("calling instantiateBundle: " + path + "_" + locale);
		ResourceBundle rb = instantiateBundle(path, locale);
		return rb;
	    } 
	    catch (MissingResourceException e) {
		if (debug) System.out.println(bundleName + "_" + locale + " not found in " + packageNames[i]);
	    }
	}

	return null;
    }

    /**
     * Get a resource bundle from the lookup chain.
     */
    public static ResourceBundle getResourceBundle(String[] packages, String bundleName, String localeName) {
	    Locale locale = LocaleUtility.getLocaleFromName(localeName);
        if (locale == null) {
	        locale = Locale.getDefault();
	    }
	    for (int i = 0; i < packages.length; ++i) {
	        try {
		        String path = packages[i] + "." + bundleName;
		        if (debug) System.out.println("calling instantiateBundle: " + path + "_" + locale);
		        ResourceBundle rb = instantiateBundle(path, locale);
		        return rb;
	        } 
	        catch (MissingResourceException e) {
	           if (debug) System.out.println(bundleName + "_" + locale + " not found in " + packages[i]);
	        }
	    }
	    return null;
    }

    /**
     * Get a resource bundle from the lookup chain.
     */
    public static ResourceBundle getResourceBundle(String packageName, String bundleName, String localeName) {
        Locale locale = LocaleUtility.getLocaleFromName(localeName);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        
        try {
            String path = packageName + "." + bundleName;
            if (debug) System.out.println("calling instantiateBundle: " + path + "_" + locale);
            ResourceBundle rb = instantiateBundle(path, locale);
            return rb;
        } 
        catch (MissingResourceException e) {
           if (debug) System.out.println(bundleName + "_" + locale + " not found in " + packageName);
        }
       
        return null;
    }    
    /**
     * Get a resource bundle from the resource bundle path.  Unlike getResourceBundle, this
     * returns an 'unparented' bundle that exactly matches the bundle name and locale name.
     */
    public static ResourceBundle loadResourceBundle(String bundleName, Locale locale) {
	if (locale == null) {
	    locale = Locale.getDefault();
	}
	return loadResourceBundle(bundleName, locale.toString());
    }

    /**
     * Get a resource bundle from the resource bundle path.  Unlike getResourceBundle, this
     * returns an 'unparented' bundle that exactly matches the bundle name and locale name.
     */
    public static ResourceBundle loadResourceBundle(String bundleName, String localeName) {
	if (localeName != null && localeName.length() > 0) {
	    bundleName = bundleName + "_" + localeName;
	}
	for (int i = 0; i < packageNames.length; ++i) {
	    String name = packageNames[i] + "." + bundleName;
	    try {
		if (name.indexOf("_zh_") == -1) { // DLF temporary hack
		    Class rbclass = ICULocaleData.class.forName(name);
		    ResourceBundle rb = (ResourceBundle)rbclass.newInstance();
		    return rb;
		}
	    } 
	    catch (ClassNotFoundException e) {
		if (debug) {
		    System.out.println(bundleName + " not found in " + packageNames[i]);
		}
		// ignore, keep looking
	    }
	    catch (Exception e) {
		if (debug) {
		    System.out.println(e.getMessage());
		}
	    }
	}
	if (debug) {
	    System.out.println(bundleName + " not found.");
	}
	
	return null;
    }

    // ========== privates ==========


    private static Locale[] createLocaleList() {
	try {
	    ResourceBundle index = getLocaleElements(LocaleUtility.getLocaleFromName("index"));
	    String[] localeNames = index.getStringArray("InstalledLocales");
	    Locale[] locales = new Locale[localeNames.length];
	    for (int i = 0; i < localeNames.length; ++i) {
		locales[i] = LocaleUtility.getLocaleFromName(localeNames[i]);
	    }
	    return locales;
	}
	catch (MissingResourceException e) {
	}
		
	return new Locale[0];
    }
}
