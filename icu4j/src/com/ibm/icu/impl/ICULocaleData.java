/*
 * (C) Copyright IBM Corp. 2002-2003 - All Rights Reserved
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
     *
     * The following note by Rich is obsolete, see below.  -- dlf 01 Oct. 2002
     * -----
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
     * "Engish (United States)" would not be in the list, since it is the default locale.
     * This might be okay, but might be confusing to some users.
     *
     * <p>In addition, the other functions that call getAvailableLocales() don't currently
     * all pass the right thing for "key," meaning that all of these functions should be
     * looked at before anything is done to this function.
     *
     * <p>We recommend that someone take some careful consideration of these issues before
     * modifying this function to pay attention to the "key" parameter.  --rtg 1/26/98
     * -----
     *
     * Return a list of the locales supported by a collection of resource bundles.
     * All ICULocaleData-based services that use a particular resource bundle support
     * all the locales from that bundle.  If support for a particular service is spotty,
     * a different bundle prefix should be used for that service.
     * @param bundlePrefix the prefix of the resource bundles to use.
     */
    public static Locale[] getAvailableLocales(String bundlePrefix) {
        return (Locale[])getAvailEntry(bundlePrefix).getLocaleList().clone();
    }

    /**
     * Convenience method that returns a list of all the avilable LOCALE_ELEMENTS locales.
     */
    public static Locale[] getAvailableLocales() {
        return getAvailableLocales(LOCALE_ELEMENTS);
    }

    /**
     * Return a set of the locale names supported by a collection of resource bundles.
     * @param bundlePrefix the prefix of the resource bundles to use.
     */
    public static Set getAvailableLocaleNameSet(String bundlePrefix) {
        return getAvailEntry(bundlePrefix).getLocaleNameSet();
    }

    /**
     * Return a set of the locale names supported by a collection of resource bundles.
     * @param bundlePrefix the prefix of the resource bundles to use.
     */
    public static Set getAvailableLocaleNameSet() {
        return getAvailableLocaleNameSet(LOCALE_ELEMENTS);
    }

    /**
     * Holds the prefix, and lazily creates the Locale[] list or the locale name Set as needed.
     */
    private static final class AvailEntry {
        private String prefix;
        private Locale[] locales;
        private Set nameSet;

        AvailEntry(String prefix) {
            this.prefix = prefix;
        }

        Locale[] getLocaleList() {
            if (locales == null) {
                locales = createLocaleList(prefix);
            }
            return locales;
        }

        Set getLocaleNameSet() {
            if (nameSet == null) {
                nameSet = createLocaleNameSet(prefix);
            }
            return nameSet;
        }
    }

    /**
     * Stores the locale information in a cache accessed by key (bundle prefix).  The
     * cached objects are AvailEntries.  The cache is held by a SoftReference
     * so it can be GC'd.
     */
    private static AvailEntry getAvailEntry(String key) {
        AvailEntry ae = null;
        Map lcache = null;
        if (lcacheref != null) {
            lcache = (Map)lcacheref.get();
            if (lcache != null) {
                ae = (AvailEntry)lcache.get(key);
            }
        }

        if (ae == null) {
            ae = new AvailEntry(key);
            if (lcache == null) {
                lcache = new HashMap();
                lcache.put(key, ae);
                lcacheref = new SoftReference(lcache);
            } else {
                lcache.put(key, ae);
            }
        }

        return ae;
    }
    private static SoftReference lcacheref;

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

    private static SoftReference gBundleCache;
    private static ResourceBundle loadFromCache(String key) {
        if (gBundleCache != null) {
            Map m = (Map)gBundleCache.get();
            if (m != null) {
                return (ResourceBundle)m.get(key);
            }
        }
        return null;
    }

    private static void addToCache(String key, ResourceBundle b) {
        Map m = null;
        if (gBundleCache != null) {
            m = (Map)gBundleCache.get();
        }
        if (m == null) {
            m = new HashMap();
            gBundleCache = new SoftReference(m);
        }
        m.put(key, b);
    }

    // recursively build bundle
    private static ResourceBundle instantiate(String name) {
        ResourceBundle b = loadFromCache(name);
        if (b == null) {
            ResourceBundle parent = null;
            int i = name.lastIndexOf('_');
            if (i != -1) {
                parent = instantiate(name.substring(0, i));
            }
            try {
                final Locale rootLocale = new Locale("");
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
//                      System.out.println("iculistresourcebundle: " + name + " is " + b);
                } else {
                    b = ResourceBundle.getBundle(name.substring(0, i), locale);
//                      System.out.println("resourcebundle: " + name + " is " + b);
                }    
                addToCache(name, b);
            }
            catch (ClassNotFoundException e) {
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
    private synchronized static ResourceBundle instantiateBundle(String name, Locale l) {
        return instantiate(name + "_" + l);
        //    ResourceBundle rb = ResourceBundle.getBundle(name, l);
//          try {
//              Class cls = ICULocaleData.class.getClassLoader().loadClass(name + "_" + l);
//              System.out.println("Loaded class: " + cls);
//              return (ResourceBundle)cls.newInstance();
//          }
//          catch (Exception e) {
//              System.out.println("failure");
//              System.out.println(e);
//          }
//          return ResourceBundle.getBundle(name, l);
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
        throw e;
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
               throw e;
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
           throw e;
        }
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
            Class rbclass = Class.forName(name);
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
            System.out.println("couldn't find index for bundleName: " + bundleName);
            Thread.dumpStack();
    }
        return Collections.EMPTY_SET;
    }

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
            System.out.println("couldn't find index for bundleName: " + bundleName);
            Thread.dumpStack();
    }
    return new Locale[0];
    }
}
