/*
 * (C) Copyright IBM Corp. 2002-2004 - All Rights Reserved
 */

package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.util.ULocale;

/**
 * Provides information about and access to resource bundles in the
 * com.ibm.text.resources package.  Unlike the java version, this does
 * not include resources from any other location.  In particular, it
 * does not look in the boot or system class path.
 */
public class ICULocaleData {

    /**
     * The package for ICU locale data.
     */
    private static final String ICU_PACKAGE = "com.ibm.icu.impl.data";

    /**
     * The base name (bundle name) for ICU locale data.
     */
    static final String LOCALE_ELEMENTS = "LocaleElements";

    /**
     * Creates a LocaleElements resource bundle for the given locale
     * in the default ICU package.
     * @param locale the locale of the bundle to retrieve, or
     * null to use the default locale
     * @return a ResourceBundle for the LocaleElements of the given
     * locale, or null on failure
     */
    public static ResourceBundle getLocaleElements(ULocale locale) {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return getResourceBundle(ICU_PACKAGE, LOCALE_ELEMENTS, locale.getBaseName());
    }

    /**
     * Creates a LocaleElements resource bundle for the given locale
     * in the default ICU package.
     * @param localeName the string name of the locale of the bundle
     * to retrieve, e.g., "en_US".
     * @return a ResourceBundle for the LocaleElements of the given
     * locale, or null on failure
     */
    public static ResourceBundle getLocaleElements(String localeName) {
        return getResourceBundle(ICU_PACKAGE, LOCALE_ELEMENTS, localeName);
    }


    /**
     * Creates a resource bundle for the given base name and locale
     * in the default ICU package.
     * @param bundleName the base name of the bundle to retrieve,
     * e.g. "LocaleElements".
     * @param localeName the string name of the locale of the bundle
     * to retrieve, e.g. "en_US".
     * @return a ResourceBundle with the given base name for the given
     * locale, or null on failure
     */
    public static ResourceBundle getResourceBundle(String bundleName, String localeName) {
        return getResourceBundle(ICU_PACKAGE, bundleName, localeName);
    }

    /**
     * Creates a resource bundle for the given base name and locale
     * in the default ICU package.
     * @param bundleName the base name of the bundle to retrieve,
     * e.g. "LocaleElements".
     * @param locale the locale of the bundle to retrieve, or
     * null to use the default locale
     * @return a ResourceBundle with the given base name for the given
     * locale, or null on failure
     */
    public static ResourceBundle getResourceBundle(String bundleName, ULocale locale) {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return getResourceBundle(ICU_PACKAGE, bundleName, locale.getBaseName());
    }

    /**
     * Creates a resource bundle for the given package, base name, and
     * locale.
     * @param packageName a package name, e.g., "com.ibm.icu.impl.data".
     * @param bundleName the base name of the bundle to retrieve,
     * e.g. "LocaleElements".
     * @param localeName the string name of the locale of the bundle
     * to retrieve, e.g. "en_US".
     * @return the first ResourceBundle with the given base name for
     * the given locale found in each of the packages, or null on
     * failure
     */
    public static ResourceBundle getResourceBundle(String packageName, String bundleName, String localeName) {
        try {
            String path = packageName + "." + bundleName;
            if (DEBUG) System.out.println("calling instantiate: " + path + "_" + localeName);
            return instantiate(path, localeName);
        } catch (MissingResourceException e) {
            if (DEBUG) System.out.println(bundleName + "_" + localeName + " not found in " + packageName);
            throw e;
        }
    }

    /**
     * Creates a resource bundle for the given base name and locale in
     * one of the given packages, trying each package in order.
     * @param packages a list of one or more package names
     * @param bundleName the base name of the bundle to retrieve,
     * e.g. "LocaleElements".
     * @param localeName the string name of the locale of the bundle
     * to retrieve, e.g. "en_US".
     * @return the first ResourceBundle with the given base name for
     * the given locale found in each of the packages, or null on
     * failure
     */
    public static ResourceBundle getResourceBundle(String[] packages, String bundleName, String localeName) {
        ResourceBundle r=null;
        for (int i=0; r==null && i<packages.length; ++i) {
            r = getResourceBundle(packages[i], bundleName, localeName);
        }
        return r;
    }

    /**
     * Get a resource bundle from the resource bundle path.  Unlike getResourceBundle, this
     * returns an 'unparented' bundle that exactly matches the bundle name and locale name.
     */
    public static ResourceBundle loadResourceBundle(String bundleName, ULocale locale) {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return loadResourceBundle(bundleName, locale.getBaseName());
    }

    /**
     * Get a resource bundle from the resource bundle path.  Unlike getResourceBundle, this
     * returns an 'unparented' bundle that exactly matches the bundle name and locale name.
     */
    public static ResourceBundle loadResourceBundle(String bundleName, String localeName) {
        if (localeName != null && localeName.length() > 0) {
            bundleName = bundleName + "_" + localeName;
        }
        String name = ICU_PACKAGE + "." + bundleName;
        try {
            if (name.indexOf("_zh_") == -1) { // DLF temporary hack
                Class rbclass = Class.forName(name);
                ResourceBundle rb = (ResourceBundle)rbclass.newInstance();
                return rb;
            }
        }
        catch (ClassNotFoundException e) {
            if (DEBUG) {
                System.out.println(name + " not found");
            }
            // ignore, keep looking
        }
        catch (Exception e) {
            e.printStackTrace();
            if (DEBUG) {
                System.out.println(e.getMessage());
            }
        }
        if (DEBUG) {
            System.out.println(bundleName + " not found.");
        }

        return null;
    }

    // ========== privates ==========

    // Flag for enabling/disabling debugging code
    private static final boolean DEBUG = ICUDebug.enabled("localedata");

    // Cache for getAvailableLocales
    private static SoftReference GET_AVAILABLE_CACHE;

    // Cache for ResourceBundle instantiation
    private static SoftReference BUNDLE_CACHE;

    private static ResourceBundle loadFromCache(String key) {
        if (BUNDLE_CACHE != null) {
            Map m = (Map)BUNDLE_CACHE.get();
            if (m != null) {
                return (ResourceBundle)m.get(key);
            }
        }
        return null;
    }

    private static void addToCache(String key, ResourceBundle b) {
        Map m = null;
        if (BUNDLE_CACHE != null) {
            m = (Map)BUNDLE_CACHE.get();
        }
        if (m == null) {
            m = new HashMap();
            BUNDLE_CACHE = new SoftReference(m);
        }
        m.put(key, b);
    }

    // recursively build bundle
    private static ResourceBundle instantiate(String name) {
        ResourceBundle b = loadFromCache(name);
        if (b == null) {
            ResourceBundle parent = null;
            int i = name.lastIndexOf('_');

            // TODO: convert this to use ULocale
            final Locale rootLocale = new Locale("", "", "");
 
            if (i != -1) {
                parent = instantiate(name.substring(0, i));

            }
            try {
                Locale locale = rootLocale;
                i = name.indexOf('_');
                if (i != -1) {
                    locale = LocaleUtility.getLocaleFromName(name.substring(i+1));
                } else {
                    i = name.length();
                }

                Class cls = ICULocaleData.class.getClassLoader().loadClass(name);
                if (ICUListResourceBundle.class.isAssignableFrom(cls)) {
                    ICUListResourceBundle bx = (ICUListResourceBundle)cls.newInstance();

                    if (parent != null) {
                        bx.setParentX(parent);
                    }
                    bx.icuLocale = locale;
                    b = bx;
                    // System.out.println("iculistresourcebundle: " + name + " is " + b);
                } else {
                    b = ResourceBundle.getBundle(name.substring(0, i), locale);
                    // System.out.println("resourcebundle: " + name + " is " + b);
                }    
                addToCache(name, b);
            }
            catch (ClassNotFoundException e) {

                int j = name.indexOf('_');
                int k = name.lastIndexOf('_');
                // if a bogus locale is passed then the parent should be
                // the default locale not the root locale!
                if(k==j && j!=-1){
                    
                    String locName = name.substring(j+1,name.length());
                    String defaultName = ULocale.getDefault().toString();
                    
                    if(!locName.equals(rootLocale.toString()) &&
                       defaultName.indexOf(locName)==-1){
                        String bundle =  name.substring(0,j);
                        parent = instantiate(bundle+"_"+defaultName);
                    }
                }
                b = parent;
            }
            catch (Exception e) {
                System.out.println("failure");
                System.out.println(e);
            }
        }
        return b;
    }

    /**
     * Still need permissions to use our own class loader, is there no way
     * to load class resources from new locations that aren't already on the
     * class path?
     */
    private synchronized static ResourceBundle instantiate(String name, String localeName) {
        if (localeName.length() != 0) {
            name = name + "_" + localeName;
        }
        return instantiate(name);
    }

    private static Set createLocaleNameSet(String bundleName) {
        try {
            ResourceBundle index = getResourceBundle(bundleName, "index");
            Object[][] localeStrings = (Object[][]) index.getObject("InstalledLocales");
            String[] localeNames = new String[localeStrings.length];

            // barf gag choke spit hack...
            // since java's Locale 'fixes' the locale string for some locales,
            // we have to fix our names to match, otherwise the Locale[] list
            // won't match the locale name set.  What were they thinking?!?
            for (int i = 0; i < localeNames.length; ++i) {
                localeNames[i] = LocaleUtility.getLocaleFromName((String)localeStrings[i][0]).toString();
            }

            HashSet set = new HashSet();
            set.addAll(Arrays.asList(localeNames));
            return Collections.unmodifiableSet(set);
        }
        catch (MissingResourceException e) {
            if (DEBUG) System.out.println("couldn't find index for bundleName: " + bundleName);
            Thread.dumpStack();
        }
        return Collections.EMPTY_SET;
    }

    /*
    private static Locale[] createLocaleList(String bundleName) {
        try {
            ResourceBundle index = getResourceBundle(bundleName, "index");
            Object[][] localeStrings = (Object[][]) index.getObject("InstalledLocales");
            Locale[] locales = new Locale[localeStrings.length];
            for (int i = 0; i < localeStrings.length; ++i) {
                locales[i] = LocaleUtility.getLocaleFromName((String)localeStrings[i][0]);
            }
            return locales;
        }
        catch (MissingResourceException e) {
            if (DEBUG) System.out.println("couldn't find index for bundleName: " + bundleName);
            Thread.dumpStack();
        }
        return new Locale[0];
    }
    */
}
