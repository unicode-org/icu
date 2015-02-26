/*
 * *****************************************************************************
 * Copyright (C) 2005-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 * *****************************************************************************
 */

package com.ibm.icu.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.ibm.icu.impl.URLHandler.URLVisitor;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;
import com.ibm.icu.util.UResourceTypeMismatchException;

public  class ICUResourceBundle extends UResourceBundle {
    /**
     * The data path to be used with getBundleInstance API
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    protected static final String ICU_DATA_PATH = ICUData.ICU_DATA_PATH;
    /**
     * The data path to be used with getBundleInstance API
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_BUNDLE = ICUData.ICU_BUNDLE;

    /**
     * The base name of ICU data to be used with getBundleInstance API
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_BASE_NAME = ICUData.ICU_BASE_NAME;

    /**
     * The base name of collation data to be used with getBundleInstance API
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_COLLATION_BASE_NAME = ICUData.ICU_COLLATION_BASE_NAME;

    /**
     * The base name of rbbi data to be used with getBundleInstance API
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_BRKITR_BASE_NAME = ICUData.ICU_BRKITR_BASE_NAME;

    /**
     * The base name of rbnf data to be used with getBundleInstance API
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_RBNF_BASE_NAME = ICUData.ICU_RBNF_BASE_NAME;

    /**
     * The base name of transliterator data to be used with getBundleInstance API
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_TRANSLIT_BASE_NAME = ICUData.ICU_TRANSLIT_BASE_NAME;

    /**
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_LANG_BASE_NAME = ICUData.ICU_LANG_BASE_NAME;
    /**
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_CURR_BASE_NAME = ICUData.ICU_CURR_BASE_NAME;
    /**
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_REGION_BASE_NAME = ICUData.ICU_REGION_BASE_NAME;
    /**
     * @deprecated because not specific to resource bundles; use the ICUData constants instead
     */
    @Deprecated
    public static final String ICU_ZONE_BASE_NAME = ICUData.ICU_ZONE_BASE_NAME;

    private static final String NO_INHERITANCE_MARKER = "\u2205\u2205\u2205";

    /**
     * The class loader constant to be used with getBundleInstance API
     */
    public static final ClassLoader ICU_DATA_CLASS_LOADER = ClassLoaderUtil.getClassLoader(ICUData.class);

    /**
     * The name of the resource containing the installed locales
     */
    protected static final String INSTALLED_LOCALES = "InstalledLocales";

    public static final int FROM_FALLBACK = 1, FROM_ROOT = 2, FROM_DEFAULT = 3, FROM_LOCALE = 4;

    private int loadingStatus = -1;

    public void setLoadingStatus(int newStatus) {
        loadingStatus = newStatus;
    }
    /**
     * Returns the loading status of a particular resource.
     *
     * @return FROM_FALLBACK if the resource is fetched from fallback bundle
     *         FROM_ROOT if the resource is fetched from root bundle.
     *         FROM_DEFAULT if the resource is fetched from the default locale.
     */
    public int getLoadingStatus() {
        return loadingStatus;
    }

    public void setLoadingStatus(String requestedLocale){
        String locale = getLocaleID();
        if(locale.equals("root")) {
            setLoadingStatus(FROM_ROOT);
        } else if(locale.equals(requestedLocale)) {
            setLoadingStatus(FROM_LOCALE);
        } else {
            setLoadingStatus(FROM_FALLBACK);
        }
     }

    /**
     * Fields for a whole bundle, rather than any specific resource in the bundle.
     * Corresponds roughly to ICU4C/source/common/uresimp.h struct UResourceDataEntry.
     */
    protected static final class WholeBundle {
        WholeBundle(String baseName, String localeID, ClassLoader loader,
                ICUResourceBundleReader reader) {
            this.baseName = baseName;
            this.localeID = localeID;
            this.ulocale = new ULocale(localeID);
            this.loader = loader;
            this.reader = reader;
        }

        String baseName;
        String localeID;
        ULocale ulocale;
        ClassLoader loader;

        /**
         * Access to the bits and bytes of the resource bundle.
         * Hides low-level details.
         */
        ICUResourceBundleReader reader;

        // TODO: Remove topLevelKeys when we upgrade to Java 6 where ResourceBundle caches the keySet().
        Set<String> topLevelKeys;
    }

    WholeBundle wholeBundle;
    private ICUResourceBundle container;

    /**
     * Returns a functionally equivalent locale, considering keywords as well, for the specified keyword.
     * @param baseName resource specifier
     * @param resName top level resource to consider (such as "collations")
     * @param keyword a particular keyword to consider (such as "collation" )
     * @param locID The requested locale
     * @param isAvailable If non-null, 1-element array of fillin parameter that indicates whether the
     * requested locale was available. The locale is defined as 'available' if it physically
     * exists within the specified tree and included in 'InstalledLocales'.
     * @param omitDefault  if true, omit keyword and value if default.
     * 'de_DE\@collation=standard' -> 'de_DE'
     * @return the locale
     * @internal ICU 3.0
     */
    public static final ULocale getFunctionalEquivalent(String baseName, ClassLoader loader,
            String resName, String keyword, ULocale locID,
            boolean isAvailable[], boolean omitDefault) {
        String kwVal = locID.getKeywordValue(keyword);
        String baseLoc = locID.getBaseName();
        String defStr = null;
        ULocale parent = new ULocale(baseLoc);
        ULocale defLoc = null; // locale where default (found) resource is
        boolean lookForDefault = false; // true if kwVal needs to be set
        ULocale fullBase = null; // base locale of found (target) resource
        int defDepth = 0; // depth of 'default' marker
        int resDepth = 0; // depth of found resource;

        if ((kwVal == null) || (kwVal.length() == 0)
                || kwVal.equals(DEFAULT_TAG)) {
            kwVal = ""; // default tag is treated as no keyword
            lookForDefault = true;
        }

        // Check top level locale first
        ICUResourceBundle r = null;

        r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, parent);
        if (isAvailable != null) {
            isAvailable[0] = false;
            ULocale[] availableULocales = getAvailEntry(baseName, loader).getULocaleList();
            for (int i = 0; i < availableULocales.length; i++) {
                if (parent.equals(availableULocales[i])) {
                    isAvailable[0] = true;
                    break;
                }
            }
        }
        // determine in which locale (if any) the currently relevant 'default' is
        do {
            try {
                ICUResourceBundle irb = (ICUResourceBundle) r.get(resName);
                defStr = irb.getString(DEFAULT_TAG);
                if (lookForDefault == true) {
                    kwVal = defStr;
                    lookForDefault = false;
                }
                defLoc = r.getULocale();
            } catch (MissingResourceException t) {
                // Ignore error and continue search.
            }
            if (defLoc == null) {
                r = (ICUResourceBundle) r.getParent();
                defDepth++;
            }
        } while ((r != null) && (defLoc == null));

        // Now, search for the named resource
        parent = new ULocale(baseLoc);
        r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, parent);
        // determine in which locale (if any) the named resource is located
        do {
            try {
                ICUResourceBundle irb = (ICUResourceBundle)r.get(resName);
                /* UResourceBundle urb = */irb.get(kwVal);
                fullBase = irb.getULocale();
                // If the get() completed, we have the full base locale
                // If we fell back to an ancestor of the old 'default',
                // we need to re calculate the "default" keyword.
                if ((fullBase != null) && ((resDepth) > defDepth)) {
                    defStr = irb.getString(DEFAULT_TAG);
                    defLoc = r.getULocale();
                    defDepth = resDepth;
                }
            } catch (MissingResourceException t) {
                // Ignore error,
            }
            if (fullBase == null) {
                r = (ICUResourceBundle) r.getParent();
                resDepth++;
            }
        } while ((r != null) && (fullBase == null));

        if (fullBase == null && // Could not find resource 'kwVal'
                (defStr != null) && // default was defined
                !defStr.equals(kwVal)) { // kwVal is not default
            // couldn't find requested resource. Fall back to default.
            kwVal = defStr; // Fall back to default.
            parent = new ULocale(baseLoc);
            r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, parent);
            resDepth = 0;
            // determine in which locale (if any) the named resource is located
            do {
                try {
                    ICUResourceBundle irb = (ICUResourceBundle)r.get(resName);
                    UResourceBundle urb = irb.get(kwVal);

                    // if we didn't fail before this..
                    fullBase = r.getULocale();

                    // If the fetched item (urb) is in a different locale than our outer locale (r/fullBase)
                    // then we are in a 'fallback' situation. treat as a missing resource situation.
                    if(!fullBase.toString().equals(urb.getLocale().toString())) {
                        fullBase = null; // fallback condition. Loop and try again.
                    }

                    // If we fell back to an ancestor of the old 'default',
                    // we need to re calculate the "default" keyword.
                    if ((fullBase != null) && ((resDepth) > defDepth)) {
                        defStr = irb.getString(DEFAULT_TAG);
                        defLoc = r.getULocale();
                        defDepth = resDepth;
                    }
                } catch (MissingResourceException t) {
                    // Ignore error, continue search.
                }
                if (fullBase == null) {
                    r = (ICUResourceBundle) r.getParent();
                    resDepth++;
                }
            } while ((r != null) && (fullBase == null));
        }

        if (fullBase == null) {
            throw new MissingResourceException(
                "Could not find locale containing requested or default keyword.",
                baseName, keyword + "=" + kwVal);
        }

        if (omitDefault
            && defStr.equals(kwVal) // if default was requested and
            && resDepth <= defDepth) { // default was set in same locale or child
            return fullBase; // Keyword value is default - no keyword needed in locale
        } else {
            return new ULocale(fullBase.toString() + "@" + keyword + "=" + kwVal);
        }
    }

    /**
     * Given a tree path and keyword, return a string enumeration of all possible values for that keyword.
     * @param baseName resource specifier
     * @param keyword a particular keyword to consider, must match a top level resource name
     * within the tree. (i.e. "collations")
     * @internal ICU 3.0
     */
    public static final String[] getKeywordValues(String baseName, String keyword) {
        Set<String> keywords = new HashSet<String>();
        ULocale locales[] = getAvailEntry(baseName, ICU_DATA_CLASS_LOADER).getULocaleList();
        int i;

        for (i = 0; i < locales.length; i++) {
            try {
                UResourceBundle b = UResourceBundle.getBundleInstance(baseName, locales[i]);
                // downcast to ICUResourceBundle?
                ICUResourceBundle irb = (ICUResourceBundle) (b.getObject(keyword));
                Enumeration<String> e = irb.getKeys();
                while (e.hasMoreElements()) {
                    String s = e.nextElement();
                    if (!DEFAULT_TAG.equals(s) && !s.startsWith("private-")) {
                        // don't add 'default' items, nor unlisted types
                        keywords.add(s);
                    }
                }
            } catch (Throwable t) {
                //System.err.println("Error in - " + new Integer(i).toString()
                // + " - " + t.toString());
                // ignore the err - just skip that resource
            }
        }
        return keywords.toArray(new String[0]);
    }

    /**
     * This method performs multilevel fallback for fetching items from the
     * bundle e.g: If resource is in the form de__PHONEBOOK{ collations{
     * default{ "phonebook"} } } If the value of "default" key needs to be
     * accessed, then do: <code>
     *  UResourceBundle bundle = UResourceBundle.getBundleInstance("de__PHONEBOOK");
     *  ICUResourceBundle result = null;
     *  if(bundle instanceof ICUResourceBundle){
     *      result = ((ICUResourceBundle) bundle).getWithFallback("collations/default");
     *  }
     * </code>
     *
     * @param path The path to the required resource key
     * @return resource represented by the key
     * @exception MissingResourceException If a resource was not found.
     */
    public ICUResourceBundle getWithFallback(String path) throws MissingResourceException {
        ICUResourceBundle actualBundle = this;

        // now recurse to pick up sub levels of the items
        ICUResourceBundle result = findResourceWithFallback(path, actualBundle, null);

        if (result == null) {
            throw new MissingResourceException(
                "Can't find resource for bundle "
                + this.getClass().getName() + ", key " + getType(),
                path, getKey());
        }

        if (result.getType() == STRING && result.getString().equals(NO_INHERITANCE_MARKER)) {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", path, getKey());
        }

        return result;
    }
    
    public ICUResourceBundle at(int index) {
        return (ICUResourceBundle) handleGet(index, null, this);
    }
    
    public ICUResourceBundle at(String key) {
        // don't ever presume the key is an int in disguise, like ResourceArray does.
        if (this instanceof ICUResourceBundleImpl.ResourceTable) {
            return (ICUResourceBundle) handleGet(key, null, this);
        }
        return null;
    }
    
    @Override
    public ICUResourceBundle findTopLevel(int index) {
        return (ICUResourceBundle) super.findTopLevel(index);
    }
    
    @Override
    public ICUResourceBundle findTopLevel(String aKey) {
        return (ICUResourceBundle) super.findTopLevel(aKey);
    }
    
    /**
     * Like getWithFallback, but returns null if the resource is not found instead of
     * throwing an exception.
     * @param path the path to the resource
     * @return the resource, or null
     */
    public ICUResourceBundle findWithFallback(String path) {
        return findResourceWithFallback(path, this, null);
    }
    public String findStringWithFallback(String path) {
        return findStringWithFallback(path, this, null);
    }

    // will throw type mismatch exception if the resource is not a string
    public String getStringWithFallback(String path) throws MissingResourceException {
        // Optimized form of getWithFallback(path).getString();
        ICUResourceBundle actualBundle = this;
        String result = findStringWithFallback(path, actualBundle, null);

        if (result == null) {
            throw new MissingResourceException(
                "Can't find resource for bundle "
                + this.getClass().getName() + ", key " + getType(),
                path, getKey());
        }

        if (result.equals(NO_INHERITANCE_MARKER)) {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", path, getKey());
        }
        return result;
    }

    /**
     * Return a set of the locale names supported by a collection of resource
     * bundles.
     *
     * @param bundlePrefix the prefix of the resource bundles to use.
     */
    public static Set<String> getAvailableLocaleNameSet(String bundlePrefix, ClassLoader loader) {
        return getAvailEntry(bundlePrefix, loader).getLocaleNameSet();
    }

    /**
     * Return a set of all the locale names supported by a collection of
     * resource bundles.
     */
    public static Set<String> getFullLocaleNameSet() {
        return getFullLocaleNameSet(ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    /**
     * Return a set of all the locale names supported by a collection of
     * resource bundles.
     *
     * @param bundlePrefix the prefix of the resource bundles to use.
     */
    public static Set<String> getFullLocaleNameSet(String bundlePrefix, ClassLoader loader) {
        return getAvailEntry(bundlePrefix, loader).getFullLocaleNameSet();
    }

    /**
     * Return a set of the locale names supported by a collection of resource
     * bundles.
     */
    public static Set<String> getAvailableLocaleNameSet() {
        return getAvailableLocaleNameSet(ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    /**
     * Get the set of Locales installed in the specified bundles.
     * @return the list of available locales
     */
    public static final ULocale[] getAvailableULocales(String baseName, ClassLoader loader) {
        return getAvailEntry(baseName, loader).getULocaleList();
    }

    /**
     * Get the set of ULocales installed the base bundle.
     * @return the list of available locales
     */
    public static final ULocale[] getAvailableULocales() {
        return getAvailableULocales(ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    /**
     * Get the set of Locales installed in the specified bundles.
     * @return the list of available locales
     */
    public static final Locale[] getAvailableLocales(String baseName, ClassLoader loader) {
        return getAvailEntry(baseName, loader).getLocaleList();
    }

   /**
     * Get the set of Locales installed the base bundle.
     * @return the list of available locales
     */
    public static final Locale[] getAvailableLocales() {
        return getAvailEntry(ICU_BASE_NAME, ICU_DATA_CLASS_LOADER).getLocaleList();
    }

    /**
     * Convert a list of ULocales to a list of Locales.  ULocales with a script code will not be converted
     * since they cannot be represented as a Locale.  This means that the two lists will <b>not</b> match
     * one-to-one, and that the returned list might be shorter than the input list.
     * @param ulocales a list of ULocales to convert to a list of Locales.
     * @return the list of converted ULocales
     */
    public static final Locale[] getLocaleList(ULocale[] ulocales) {
        ArrayList<Locale> list = new ArrayList<Locale>(ulocales.length);
        HashSet<Locale> uniqueSet = new HashSet<Locale>();
        for (int i = 0; i < ulocales.length; i++) {
            Locale loc = ulocales[i].toLocale();
            if (!uniqueSet.contains(loc)) {
                list.add(loc);
                uniqueSet.add(loc);
            }
        }
        return list.toArray(new Locale[list.size()]);
    }

    /**
     * Returns the locale of this resource bundle. This method can be used after
     * a call to getBundle() to determine whether the resource bundle returned
     * really corresponds to the requested locale or is a fallback.
     *
     * @return the locale of this resource bundle
     */
    public Locale getLocale() {
        return getULocale().toLocale();
    }


    // ========== privates ==========
    private static final String ICU_RESOURCE_INDEX = "res_index";

    private static final String DEFAULT_TAG = "default";

    // The name of text file generated by ICU4J build script including all locale names
    // (canonical, alias and root)
    private static final String FULL_LOCALE_NAMES_LIST = "fullLocaleNames.lst";

    // Flag for enabling/disabling debugging code
    private static final boolean DEBUG = ICUDebug.enabled("localedata");

    private static final ULocale[] createULocaleList(String baseName,
            ClassLoader root) {
        // the canned list is a subset of all the available .res files, the idea
        // is we don't export them
        // all. gotta be a better way to do this, since to add a locale you have
        // to update this list,
        // and it's embedded in our binary resources.
        ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.instantiateBundle(baseName, ICU_RESOURCE_INDEX, root, true);

        bundle = (ICUResourceBundle)bundle.get(INSTALLED_LOCALES);
        int length = bundle.getSize();
        int i = 0;
        ULocale[] locales = new ULocale[length];
        UResourceBundleIterator iter = bundle.getIterator();
        iter.reset();
        while (iter.hasNext()) {
            String locstr = iter.next().getKey();
            if (locstr.equals("root")) {
                locales[i++] = ULocale.ROOT;
            } else {
                locales[i++] = new ULocale(locstr);
            }
        }
        bundle = null;
        return locales;
    }

    // Same as createULocaleList() but catches the MissingResourceException
    // and returns the data in a different form.
    private static final void addLocaleIDsFromIndexBundle(String baseName,
            ClassLoader root, Set<String> locales) {
        ICUResourceBundle bundle;
        try {
            bundle = (ICUResourceBundle) UResourceBundle.instantiateBundle(baseName, ICU_RESOURCE_INDEX, root, true);
            bundle = (ICUResourceBundle) bundle.get(INSTALLED_LOCALES);
        } catch (MissingResourceException e) {
            if (DEBUG) {
                System.out.println("couldn't find " + baseName + '/' + ICU_RESOURCE_INDEX + ".res");
                Thread.dumpStack();
            }
            return;
        }
        UResourceBundleIterator iter = bundle.getIterator();
        iter.reset();
        while (iter.hasNext()) {
            String locstr = iter.next(). getKey();
            locales.add(locstr);
        }
    }

    private static final void addBundleBaseNamesFromClassLoader(
            final String bn, final ClassLoader root, final Set<String> names) {
        java.security.AccessController
            .doPrivileged(new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    try {
                        // bn has a trailing slash: The WebSphere class loader would return null
                        // for a raw directory name without it.
                        Enumeration<URL> urls = root.getResources(bn);
                        if (urls == null) {
                            return null;
                        }
                        URLVisitor v = new URLVisitor() {
                            public void visit(String s) {
                                if (s.endsWith(".res")) {
                                    String locstr = s.substring(0, s.length() - 4);
                                    names.add(locstr);
                                }
                            }
                        };
                        while (urls.hasMoreElements()) {
                            URL url = urls.nextElement();
                            URLHandler handler = URLHandler.get(url);
                            if (handler != null) {
                                handler.guide(v, false);
                            } else {
                                if (DEBUG) System.out.println("handler for " + url + " is null");
                            }
                        }
                    } catch (IOException e) {
                        if (DEBUG) System.out.println("ouch: " + e.getMessage());
                    }
                    return null;
                }
            });
    }

    private static void addLocaleIDsFromListFile(String bn, ClassLoader root, Set<String> locales) {
        try {
            InputStream s = root.getResourceAsStream(bn + FULL_LOCALE_NAMES_LIST);
            if (s != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(s, "ASCII"));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.length() != 0 && !line.startsWith("#")) {
                        locales.add(line);
                    }
                }
                br.close();
            }
        } catch (IOException e) {
            // swallow it
        }
    }

    private static Set<String> createFullLocaleNameSet(String baseName, ClassLoader loader) {
        String bn = baseName.endsWith("/") ? baseName : baseName + "/";
        Set<String> set = new HashSet<String>();
        String skipScan = ICUConfig.get("com.ibm.icu.impl.ICUResourceBundle.skipRuntimeLocaleResourceScan", "false");
        if (!skipScan.equalsIgnoreCase("true")) {
            // scan available locale resources under the base url first
            addBundleBaseNamesFromClassLoader(baseName, loader, set);
            if (baseName.startsWith(ICUData.ICU_BASE_NAME)) {
                String folder;
                if (baseName.length() == ICUData.ICU_BASE_NAME.length()) {
                    folder = "";
                } else if (baseName.charAt(ICUData.ICU_BASE_NAME.length()) == '/') {
                    folder = baseName.substring(ICUData.ICU_BASE_NAME.length() + 1);
                } else {
                    folder = null;
                }
                if (folder != null) {
                    ICUBinary.addBaseNamesInFileFolder(folder, ".res", set);
                }
            }
            set.remove(ICU_RESOURCE_INDEX);  // "res_index"
            // HACK: TODO: Figure out how we can distinguish locale data from other data items.
            Iterator<String> iter = set.iterator();
            while (iter.hasNext()) {
                String name = iter.next();
                if ((name.length() == 1 || name.length() > 3) && name.indexOf('_') < 0) {
                    // Does not look like a locale ID.
                    iter.remove();
                }
            }
        }
        // look for prebuilt full locale names list next
        if (set.isEmpty()) {
            if (DEBUG) System.out.println("unable to enumerate data files in " + baseName);
            addLocaleIDsFromListFile(bn, loader, set);
        }
        if (set.isEmpty()) {
            // Use locale name set as the last resort fallback
            addLocaleIDsFromIndexBundle(baseName, loader, set);
        }
        // We need to have the root locale in the set, but not as "root".
        set.remove("root");
        set.add(ULocale.ROOT.toString());  // ""
        return Collections.unmodifiableSet(set);
    }

    private static Set<String> createLocaleNameSet(String baseName, ClassLoader loader) {
        HashSet<String> set = new HashSet<String>();
        addLocaleIDsFromIndexBundle(baseName, loader, set);
        return Collections.unmodifiableSet(set);
    }

    /**
     * Holds the prefix, and lazily creates the Locale[] list or the locale name
     * Set as needed.
     */
    private static final class AvailEntry {
        private String prefix;
        private ClassLoader loader;
        private volatile ULocale[] ulocales;
        private volatile Locale[] locales;
        private volatile Set<String> nameSet;
        private volatile Set<String> fullNameSet;

        AvailEntry(String prefix, ClassLoader loader) {
            this.prefix = prefix;
            this.loader = loader;
        }

        ULocale[] getULocaleList() {
            if (ulocales == null) {
                synchronized(this) {
                    if (ulocales == null) {
                        ulocales = createULocaleList(prefix, loader);
                    }
                }
            }
            return ulocales;
        }
        Locale[] getLocaleList() {
            if (locales == null) {
                getULocaleList();
                synchronized(this) {
                    if (locales == null) {
                        locales = ICUResourceBundle.getLocaleList(ulocales);
                    }
                }
            }
            return locales;
        }
        Set<String> getLocaleNameSet() {
            if (nameSet == null) {
                synchronized(this) {
                    if (nameSet == null) {
                        nameSet = createLocaleNameSet(prefix, loader);
                    }
                }
            }
            return nameSet;
        }
        Set<String> getFullLocaleNameSet() {
            // When there's no prebuilt index, we iterate through the jar files
            // and read the contents to build it.  If many threads try to read the
            // same jar at the same time, java thrashes.  Synchronize here
            // so that we can avoid this problem. We don't synchronize on the
            // other methods since they don't do this.
            //
            // This is the common entry point for access into the code that walks
            // through the resources, and is cached.  So it's a good place to lock
            // access.  Locking in the URLHandler doesn't give us a common object
            // to lock.
            if (fullNameSet == null) {
                synchronized(this) {
                    if (fullNameSet == null) {
                        fullNameSet = createFullLocaleNameSet(prefix, loader);
                    }
                }
            }
            return fullNameSet;
        }
    }


    /*
     * Cache used for AvailableEntry 
     */
    private static CacheBase<String, AvailEntry, ClassLoader> GET_AVAILABLE_CACHE =
        new SoftCache<String, AvailEntry, ClassLoader>()  {
            protected AvailEntry createInstance(String key, ClassLoader loader) {
                return new AvailEntry(key, loader);
            }
        };

    /**
     * Stores the locale information in a cache accessed by key (bundle prefix).
     * The cached objects are AvailEntries. The cache is implemented by SoftCache
     * so it can be GC'd.
     */
    private static AvailEntry getAvailEntry(String key, ClassLoader loader) {
        return GET_AVAILABLE_CACHE.getInstance(key, loader);
    }

    private static final ICUResourceBundle findResourceWithFallback(String path,
            UResourceBundle actualBundle, UResourceBundle requested) {
        if (path.length() == 0) {
            return null;
        }
        ICUResourceBundle sub = null;
        if (requested == null) {
            requested = actualBundle;
        }

        ICUResourceBundle base = (ICUResourceBundle) actualBundle;
        // Collect existing and parsed key objects into an array of keys,
        // rather than assembling and parsing paths.
        int depth = base.getResDepth();
        int numPathKeys = countPathKeys(path);
        assert numPathKeys > 0;
        String[] keys = new String[depth + numPathKeys];
        getResPathKeys(path, numPathKeys, keys, depth);

        for (;;) {  // Iterate over the parent bundles.
            for (;;) {  // Iterate over the keys on the requested path, within a bundle.
                String subKey = keys[depth++];
                sub = (ICUResourceBundle) base.handleGet(subKey, null, requested);
                if (sub == null) {
                    --depth;
                    break;
                }
                if (depth == keys.length) {
                    // We found it.
                    sub.setLoadingStatus(((ICUResourceBundle)requested).getLocaleID());
                    return sub;
                }
                base = sub;
            }
            // Try the parent bundle of the last-found resource.
            ICUResourceBundle nextBase = (ICUResourceBundle)base.getParent();
            if (nextBase == null) {
                return null;
            }
            // If we followed an alias, then we may have switched bundle (locale) and key path.
            // Set the lower parts of the path according to the last-found resource.
            // This relies on a resource found via alias to have its original location information,
            // rather than the location of the alias.
            int baseDepth = base.getResDepth();
            if (depth != baseDepth) {
                String[] newKeys = new String[baseDepth + (keys.length - depth)];
                System.arraycopy(keys, depth, newKeys, baseDepth, keys.length - depth);
                keys = newKeys;
            }
            base.getResPathKeys(keys, baseDepth);
            base = nextBase;
            depth = 0;  // getParent() returned a top level table resource.
        }
    }

    /**
     * Like findResourceWithFallback(...).getString() but with minimal creation of intermediate
     * ICUResourceBundle objects.
     */
    private static final String findStringWithFallback(String path,
            UResourceBundle actualBundle, UResourceBundle requested) {
        if (path.length() == 0) {
            return null;
        }
        if (!(actualBundle instanceof ICUResourceBundleImpl.ResourceContainer)) {
            return null;
        }
        if (requested == null) {
            requested = actualBundle;
        }

        ICUResourceBundle base = (ICUResourceBundle) actualBundle;
        ICUResourceBundleReader reader = base.wholeBundle.reader;
        int res = RES_BOGUS;

        // Collect existing and parsed key objects into an array of keys,
        // rather than assembling and parsing paths.
        int baseDepth = base.getResDepth();
        int depth = baseDepth;
        int numPathKeys = countPathKeys(path);
        assert numPathKeys > 0;
        String[] keys = new String[depth + numPathKeys];
        getResPathKeys(path, numPathKeys, keys, depth);

        for (;;) {  // Iterate over the parent bundles.
            for (;;) {  // Iterate over the keys on the requested path, within a bundle.
                ICUResourceBundleReader.Container readerContainer;
                if (res == RES_BOGUS) {
                    int type = base.getType();
                    if (type == TABLE || type == ARRAY) {
                        readerContainer = ((ICUResourceBundleImpl.ResourceContainer)base).value;
                    } else {
                        break;
                    }
                } else {
                    int type = ICUResourceBundleReader.RES_GET_TYPE(res);
                    if (ICUResourceBundleReader.URES_IS_TABLE(type)) {
                        readerContainer = reader.getTable(res);
                    } else if (ICUResourceBundleReader.URES_IS_ARRAY(type)) {
                        readerContainer = reader.getArray(res);
                    } else {
                        res = RES_BOGUS;
                        break;
                    }
                }
                String subKey = keys[depth++];
                res = readerContainer.getResource(reader, subKey);
                if (res == RES_BOGUS) {
                    --depth;
                    break;
                }
                ICUResourceBundle sub;
                if (ICUResourceBundleReader.RES_GET_TYPE(res) == ALIAS) {
                    base.getResPathKeys(keys, baseDepth);
                    sub = getAliasedResource(base, keys, depth, subKey, res, null, requested);
                } else {
                    sub = null;
                }
                if (depth == keys.length) {
                    // We found it.
                    if (sub != null) {
                        return sub.getString();  // string from alias handling
                    } else {
                        String s = reader.getString(res);
                        if (s == null) {
                            throw new UResourceTypeMismatchException("");
                        }
                        return s;
                    }
                }
                if (sub != null) {
                    base = sub;
                    reader = base.wholeBundle.reader;
                    res = RES_BOGUS;
                    // If we followed an alias, then we may have switched bundle (locale) and key path.
                    // Reserve space for the lower parts of the path according to the last-found resource.
                    // This relies on a resource found via alias to have its original location information,
                    // rather than the location of the alias.
                    baseDepth = base.getResDepth();
                    if (depth != baseDepth) {
                        String[] newKeys = new String[baseDepth + (keys.length - depth)];
                        System.arraycopy(keys, depth, newKeys, baseDepth, keys.length - depth);
                        keys = newKeys;
                        depth = baseDepth;
                    }
                }
            }
            // Try the parent bundle of the last-found resource.
            ICUResourceBundle nextBase = (ICUResourceBundle)base.getParent();
            if (nextBase == null) {
                return null;
            }
            // We probably have not yet set the lower parts of the key path.
            base.getResPathKeys(keys, baseDepth);
            base = nextBase;
            reader = base.wholeBundle.reader;
            depth = baseDepth = 0;  // getParent() returned a top level table resource.
        }
    }

    private int getResDepth() {
        return (container == null) ? 0 : container.getResDepth() + 1;
    }

    /**
     * Fills some of the keys array with the keys on the path to this resource object.
     * Writes the top-level key into index 0 and increments from there.
     *
     * @param keys
     * @param depth must be {@link #getResDepth()}
     */
    private void getResPathKeys(String[] keys, int depth) {
        ICUResourceBundle b = this;
        while (depth > 0) {
            keys[--depth] = b.key;
            b = b.container;
            assert (depth == 0) == (b.container == null);
        }
    }

    private static int countPathKeys(String path) {
        if (path.length() == 0) {
            return 0;
        }
        int num = 1;
        for (int i = 0; i < path.length(); ++i) {
            if (path.charAt(i) == RES_PATH_SEP_CHAR) {
                ++num;
            }
        }
        return num;
    }

    /**
     * Fills some of the keys array (from start) with the num keys from the path string.
     *
     * @param path path string
     * @param num must be {@link #countPathKeys(String)}
     * @param keys
     * @param start index where the first path key is stored
     */
    private static void getResPathKeys(String path, int num, String[] keys, int start) {
        if (num == 0) {
            return;
        }
        if (num == 1) {
            keys[start] = path;
            return;
        }
        int i = 0;
        for (;;) {
            int j = path.indexOf(RES_PATH_SEP_CHAR, i);
            assert j >= i;
            keys[start++] = path.substring(i, j);
            if (num == 2) {
                assert path.indexOf(RES_PATH_SEP_CHAR, j + 1) < 0;
                keys[start] = path.substring(j + 1);
                break;
            } else {
                i = j + 1;
                --num;
            }
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ICUResourceBundle) {
            ICUResourceBundle o = (ICUResourceBundle) other;
            if (getBaseName().equals(o.getBaseName())
                    && getLocaleID().equals(o.getLocaleID())) {
                return true;
            }
        }
        return false;
    }
    
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    public enum OpenType {  // C++ uresbund.cpp: enum UResOpenType
        /**
         * Open a resource bundle for the locale;
         * if there is not even a base language bundle, then fall back to the default locale;
         * if there is no bundle for that either, then load the root bundle.
         *
         * <p>This is the default bundle loading behavior.
         */
        LOCALE_DEFAULT_ROOT,
        // TODO: ICU ticket #11271 "consistent default locale across locale trees"
        // Add an option to look at the main locale tree for whether to
        // fall back to root directly (if the locale has main data) or
        // fall back to the default locale first (if the locale does not even have main data).
        /**
         * Open a resource bundle for the locale;
         * if there is not even a base language bundle, then load the root bundle;
         * never fall back to the default locale.
         *
         * <p>This is used for algorithms that have good pan-Unicode default behavior,
         * such as case mappings, collation, and segmentation (BreakIterator).
         */
        LOCALE_ROOT,
        /**
         * Open a resource bundle for the exact bundle name as requested;
         * no fallbacks, do not load parent bundles.
         *
         * <p>This is used for supplemental (non-locale) data.
         */
        DIRECT
    };

    // This method is for super class's instantiateBundle method
    public static UResourceBundle getBundleInstance(String baseName, String localeID,
                                                    ClassLoader root, boolean disableFallback){
        UResourceBundle b = instantiateBundle(baseName, localeID, root,
                disableFallback ? OpenType.DIRECT : OpenType.LOCALE_DEFAULT_ROOT);
        if(b==null){
            throw new MissingResourceException("Could not find the bundle "+ baseName+"/"+ localeID+".res","","");
        }
        return b;
    }

    protected static UResourceBundle instantiateBundle(String baseName, String localeID,
            ClassLoader root, boolean disableFallback){
        return instantiateBundle(baseName, localeID, root,
                disableFallback ? OpenType.DIRECT : OpenType.LOCALE_DEFAULT_ROOT);
    }

    public static UResourceBundle getBundleInstance(
            String baseName, ULocale locale, OpenType openType) {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return getBundleInstance(baseName, locale.toString(),
                ICUResourceBundle.ICU_DATA_CLASS_LOADER, openType);
    }

    public static UResourceBundle getBundleInstance(String baseName, String localeID,
            ClassLoader root, OpenType openType) {
        if (baseName == null) {
            baseName = ICUData.ICU_BASE_NAME;
        }
        UResourceBundle b = instantiateBundle(baseName, localeID, root, openType);
        if(b==null){
            throw new MissingResourceException(
                    "Could not find the bundle "+ baseName+"/"+ localeID+".res","","");
        }
        return b;
    }

    //  recursively build bundle
    private synchronized static UResourceBundle instantiateBundle(String baseName, String localeID,
            ClassLoader root, OpenType openType) {
        ULocale defaultLocale = ULocale.getDefault();
        String localeName = localeID;
        if(localeName.indexOf('@')>=0){
            localeName = ULocale.getBaseName(localeID);
        }
        String fullName = ICUResourceBundleReader.getFullName(baseName, localeName);
        ICUResourceBundle b = (ICUResourceBundle)loadFromCache(root, fullName, defaultLocale);

        // here we assume that java type resource bundle organization
        // is required then the base name contains '.' else
        // the resource organization is of ICU type
        // so clients can instantiate resources of the type
        // com.mycompany.data.MyLocaleElements_en.res and
        // com.mycompany.data.MyLocaleElements.res
        //
        final String rootLocale = (baseName.indexOf('.')==-1) ? "root" : "";
        final String defaultID = defaultLocale.getBaseName();

        if(localeName.equals("")){
            localeName = rootLocale;
        }
        if(DEBUG) System.out.println("Creating "+fullName+ " currently b is "+b);
        if (b == null) {
            b = ICUResourceBundle.createBundle(baseName, localeName, root);

            if(DEBUG)System.out.println("The bundle created is: "+b+" and openType="+openType+" and bundle.getNoFallback="+(b!=null && b.getNoFallback()));
            if (openType == OpenType.DIRECT || (b != null && b.getNoFallback())) {
                // no fallback because the caller said so or because the bundle says so
                //
                // TODO for b!=null: In C++, ures_openDirect() builds the parent chain
                // for its bundle unless its nofallback flag is set.
                // Otherwise we get test failures.
                // For example, item aliases are followed via ures_openDirect(),
                // and fail if the target bundle needs fallbacks but the chain is not set.
                // Figure out why Java does not build the parent chain
                // for a bundle that does not have nofallback.
                // Are the relevant test cases just disabled?
                // Do item aliases not get followed via "direct" loading?
                return addToCache(root, fullName, defaultLocale, b);
            }

            // fallback to locale ID parent
            if(b == null){
                int i = localeName.lastIndexOf('_');
                if (i != -1) {
                    String temp = localeName.substring(0, i);
                    b = (ICUResourceBundle)instantiateBundle(baseName, temp, root, openType);
                    if(b!=null && b.getULocale().getName().equals(temp)){
                        b.setLoadingStatus(ICUResourceBundle.FROM_FALLBACK);
                    }
                }else{
                    if(openType == OpenType.LOCALE_DEFAULT_ROOT &&
                            !defaultLocale.getLanguage().equals(localeName)) {
                        b = (ICUResourceBundle)instantiateBundle(baseName, defaultID, root, openType);
                        if(b!=null){
                            b.setLoadingStatus(ICUResourceBundle.FROM_DEFAULT);
                        }
                    }else if(rootLocale.length()!=0){
                        b = ICUResourceBundle.createBundle(baseName, rootLocale, root);
                        if(b!=null){
                            b.setLoadingStatus(ICUResourceBundle.FROM_ROOT);
                        }
                    }
                }
            }else{
                UResourceBundle parent = null;
                localeName = b.getLocaleID();
                int i = localeName.lastIndexOf('_');

                b = (ICUResourceBundle)addToCache(root, fullName, defaultLocale, b);

                // TODO: C++ uresbund.cpp also checks for %%ParentIsRoot. Why not Java?
                String parentLocaleName = ((ICUResourceBundleImpl.ResourceTable)b).findString("%%Parent");
                if (parentLocaleName != null) {
                    parent = instantiateBundle(baseName, parentLocaleName, root, openType);
                } else if (i != -1) {
                    parent = instantiateBundle(baseName, localeName.substring(0, i), root, openType);
                } else if (!localeName.equals(rootLocale)){
                    parent = instantiateBundle(baseName, rootLocale, root, true);
                }

                if (!b.equals(parent)){
                    b.setParent(parent);
                }
            }
        }
        return b;
    }
    UResourceBundle get(String aKey, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        ICUResourceBundle obj = (ICUResourceBundle)handleGet(aKey, aliasesVisited, requested);
        if (obj == null) {
            obj = (ICUResourceBundle)getParent();
            if (obj != null) {
                //call the get method to recursively fetch the resource
                obj = (ICUResourceBundle)obj.get(aKey, aliasesVisited, requested);
            }
            if (obj == null) {
                String fullName = ICUResourceBundleReader.getFullName(getBaseName(), getLocaleID());
                throw new MissingResourceException(
                        "Can't find resource for bundle " + fullName + ", key "
                                + aKey, this.getClass().getName(), aKey);
            }
        }
        obj.setLoadingStatus(((ICUResourceBundle)requested).getLocaleID());
        return obj;
    }

    /** Data member where the subclasses store the key. */
    protected String key;

    /**
     * A resource word value that means "no resource".
     * Note: 0xffffffff == -1
     * This has the same value as UResourceBundle.NONE, but they are semantically
     * different and should be used appropriately according to context:
     * NONE means "no type".
     * (The type of RES_BOGUS is RES_RESERVED=15 which was defined in ICU4C ures.h.)
     */
    public static final int RES_BOGUS = 0xffffffff;

    /**
     * Resource type constant for aliases;
     * internally stores a string which identifies the actual resource
     * storing the data (can be in a different resource bundle).
     * Resolved internally before delivering the actual resource through the API.
     */
    public static final int ALIAS = 3;

    /** Resource type constant for tables with 32-bit count, key offsets and values. */
    public static final int TABLE32 = 4;

    /**
     * Resource type constant for tables with 16-bit count, key offsets and values.
     * All values are STRING_V2 strings.
     */
    public static final int TABLE16 = 5;

    /** Resource type constant for 16-bit Unicode strings in formatVersion 2. */
    public static final int STRING_V2 = 6;

    /**
     * Resource type constant for arrays with 16-bit count and values.
     * All values are STRING_V2 strings.
     */
    public static final int ARRAY16 = 9;

    /**
    * Create a bundle using a reader.
    * @param baseName The name for the bundle.
    * @param localeID The locale identification.
    * @param root The ClassLoader object root.
    * @return the new bundle
    */
    public static ICUResourceBundle createBundle(String baseName, String localeID, ClassLoader root) {
        ICUResourceBundleReader reader = ICUResourceBundleReader.getReader(baseName, localeID, root);
        if (reader == null) {
            // could not open the .res file
            return null;
        }
        return getBundle(reader, baseName, localeID, root);
    }

    protected String getLocaleID() {
        return wholeBundle.localeID;
    }

    protected String getBaseName() {
        return wholeBundle.baseName;
    }

    public ULocale getULocale() {
        return wholeBundle.ulocale;
    }

    public UResourceBundle getParent() {
        return (UResourceBundle) parent;
    }

    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    public String getKey() {
        return key;
    }

    /**
     * Get the noFallback flag specified in the loaded bundle.
     * @return The noFallback flag.
     */
    private boolean getNoFallback() {
        return wholeBundle.reader.getNoFallback();
    }

    private static ICUResourceBundle getBundle(ICUResourceBundleReader reader,
                                               String baseName, String localeID,
                                               ClassLoader loader) {
        ICUResourceBundleImpl.ResourceTable rootTable;
        int rootRes = reader.getRootResource();
        if(ICUResourceBundleReader.URES_IS_TABLE(ICUResourceBundleReader.RES_GET_TYPE(rootRes))) {
            WholeBundle wb = new WholeBundle(baseName, localeID, loader, reader);
            rootTable = new ICUResourceBundleImpl.ResourceTable(wb, rootRes);
        } else {
            throw new IllegalStateException("Invalid format error");
        }
        String aliasString = rootTable.findString("%%ALIAS");
        if(aliasString != null) {
            return (ICUResourceBundle)UResourceBundle.getBundleInstance(baseName, aliasString);
        } else {
            return rootTable;
        }
    }
    /**
     * Constructor for the root table of a bundle.
     */
    protected ICUResourceBundle(WholeBundle wholeBundle) {
        this.wholeBundle = wholeBundle;
    }
    // constructor for inner classes
    protected ICUResourceBundle(ICUResourceBundle container, String key) {
        this.key = key;
        wholeBundle = container.wholeBundle;
        this.container = (ICUResourceBundleImpl.ResourceContainer) container;
        parent = container.parent;
    }

    private static final char RES_PATH_SEP_CHAR = '/';
    private static final String RES_PATH_SEP_STR = "/";
    private static final String ICUDATA = "ICUDATA";
    private static final char HYPHEN = '-';
    private static final String LOCALE = "LOCALE";

    /**
     * Returns the resource object referred to from the alias _resource int's path string.
     * Throws MissingResourceException if not found.
     *
     * If the alias path does not contain a key path:
     * If keys != null then keys[:depth] is used.
     * Otherwise the base key path plus the key parameter is used.
     *
     * @param base A direct or indirect container of the alias.
     * @param keys The key path to the alias, or null. (const)
     * @param depth The length of the key path, if keys != null.
     * @param key The alias' own key within this current container, if keys == null.
     * @param _resource The alias resource int.
     * @param aliasesVisited Set of alias path strings already visited, for detecting loops.
     *        We cannot change the type (e.g., to Set<String>) because it is used
     *        in protected/@stable UResourceBundle methods.
     * @param requested The original resource object from which the lookup started,
     *        which is the starting point for "/LOCALE/..." aliases.
     * @return the aliased resource object
     */
    protected static ICUResourceBundle getAliasedResource(
            ICUResourceBundle base, String[] keys, int depth,
            String key, int _resource,
            HashMap<String, String> aliasesVisited,
            UResourceBundle requested) {
        WholeBundle wholeBundle = base.wholeBundle;
        ClassLoader loaderToUse = wholeBundle.loader;
        String locale = null, keyPath = null;
        String bundleName;
        String rpath = wholeBundle.reader.getAlias(_resource);
        if (aliasesVisited == null) {
            aliasesVisited = new HashMap<String, String>();
        }
        if (aliasesVisited.get(rpath) != null) {
            throw new IllegalArgumentException(
                    "Circular references in the resource bundles");
        }
        aliasesVisited.put(rpath, "");
        if (rpath.indexOf(RES_PATH_SEP_CHAR) == 0) {
            int i = rpath.indexOf(RES_PATH_SEP_CHAR, 1);
            int j = rpath.indexOf(RES_PATH_SEP_CHAR, i + 1);
            bundleName = rpath.substring(1, i);
            if (j < 0) {
                locale = rpath.substring(i + 1);
            } else {
                locale = rpath.substring(i + 1, j);
                keyPath = rpath.substring(j + 1, rpath.length());
            }
            //there is a path included
            if (bundleName.equals(ICUDATA)) {
                bundleName = ICU_BASE_NAME;
                loaderToUse = ICU_DATA_CLASS_LOADER;
            }else if(bundleName.indexOf(ICUDATA)>-1){
                int idx = bundleName.indexOf(HYPHEN);
                if(idx>-1){
                    bundleName = ICU_BASE_NAME+RES_PATH_SEP_STR+bundleName.substring(idx+1,bundleName.length());
                    loaderToUse = ICU_DATA_CLASS_LOADER;
                }
            }
        } else {
            //no path start with locale
            int i = rpath.indexOf(RES_PATH_SEP_CHAR);
            if (i != -1) {
                locale = rpath.substring(0, i);
                keyPath = rpath.substring(i + 1);
            } else {
                locale = rpath;
            }
            bundleName = wholeBundle.baseName;
        }
        ICUResourceBundle bundle = null;
        ICUResourceBundle sub = null;
        if(bundleName.equals(LOCALE)){
            bundleName = wholeBundle.baseName;
            keyPath = rpath.substring(LOCALE.length() + 2/* prepending and appending / */, rpath.length());

            // Get the top bundle of the requested bundle
            bundle = (ICUResourceBundle)requested;
            while (bundle.container != null) {
                bundle = bundle.container;
            }
            sub = ICUResourceBundle.findResourceWithFallback(keyPath, bundle, null);
        }else{
            if (locale == null) {
                // {dlf} must use requestor's class loader to get resources from same jar
                bundle = (ICUResourceBundle) getBundleInstance(bundleName, "",
                         loaderToUse, false);
            } else {
                bundle = (ICUResourceBundle) getBundleInstance(bundleName, locale,
                         loaderToUse, false);
            }

            int numKeys;
            if (keyPath != null) {
                numKeys = countPathKeys(keyPath);
                if (numKeys > 0) {
                    keys = new String[numKeys];
                    getResPathKeys(keyPath, numKeys, keys, 0);
                }
            } else if (keys != null) {
                numKeys = depth;
            } else {
                depth = base.getResDepth();
                numKeys = depth + 1;
                keys = new String[numKeys];
                base.getResPathKeys(keys, depth);
                keys[depth] = key;
            }
            if (numKeys > 0) {
                sub = bundle;
                for (int i = 0; sub != null && i < numKeys; ++i) {
                    sub = (ICUResourceBundle)sub.get(keys[i], aliasesVisited, requested);
                }
            }
        }
        if (sub == null) {
            throw new MissingResourceException(wholeBundle.localeID, wholeBundle.baseName, key);
        }
        // TODO: If we know that sub is not cached,
        // then we should set its container and key to the alias' location,
        // so that it behaves as if its value had been copied into the alias location.
        // However, findResourceWithFallback() must reroute its bundle and key path
        // to where the alias data comes from.
        return sub;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public final Set<String> getTopLevelKeySet() {
        return wholeBundle.topLevelKeys;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public final void setTopLevelKeySet(Set<String> keySet) {
        wholeBundle.topLevelKeys = keySet;
    }

    // This is the worker function for the public getKeys().
    // TODO: Now that UResourceBundle uses handleKeySet(), this function is obsolete.
    // It is also not inherited from ResourceBundle, and it is not implemented
    // by ResourceBundleWrapper despite its documentation requiring all subclasses to
    // implement it.
    // Consider deprecating UResourceBundle.handleGetKeys(), and consider making it always return null.
    protected Enumeration<String> handleGetKeys() {
        return Collections.enumeration(handleKeySet());
    }

    protected boolean isTopLevelResource() {
        return container == null;
    }
}
