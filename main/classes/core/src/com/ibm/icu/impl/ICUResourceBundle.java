/*
 * *****************************************************************************
 * Copyright (C) 2005-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 * *****************************************************************************
 */

package com.ibm.icu.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.icu.impl.URLHandler.URLVisitor;
import com.ibm.icu.util.StringTokenizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;
import com.ibm.icu.util.VersionInfo;

public  class ICUResourceBundle extends UResourceBundle {
    /**
     * The data path to be used with getBundleInstance API
     */
    protected static final String ICU_DATA_PATH = "com/ibm/icu/impl/";
    /**
     * The data path to be used with getBundleInstance API
     */
    public static final String ICU_BUNDLE = "data/icudt" + VersionInfo.ICU_DATA_VERSION_PATH;

    /**
     * The base name of ICU data to be used with getBundleInstance API
     */
    public static final String ICU_BASE_NAME = ICU_DATA_PATH + ICU_BUNDLE;

    /**
     * The base name of collation data to be used with getBundleInstance API
     */
    public static final String ICU_COLLATION_BASE_NAME = ICU_BASE_NAME + "/coll";

    /**
     * The base name of rbbi data to be used with getData API
     */
    public static final String ICU_BRKITR_NAME = "/brkitr";

    /**
     * The base name of rbbi data to be used with getBundleInstance API
     */
    public static final String ICU_BRKITR_BASE_NAME = ICU_BASE_NAME + ICU_BRKITR_NAME;

    /**
     * The base name of rbnf data to be used with getBundleInstance API
     */
    public static final String ICU_RBNF_BASE_NAME = ICU_BASE_NAME + "/rbnf";

    /**
     * The base name of transliterator data to be used with getBundleInstance API
     */
    public static final String ICU_TRANSLIT_BASE_NAME = ICU_BASE_NAME + "/translit";

    public static final String ICU_LANG_BASE_NAME = ICU_BASE_NAME + "/lang";
    public static final String ICU_CURR_BASE_NAME = ICU_BASE_NAME + "/curr";
    public static final String ICU_REGION_BASE_NAME = ICU_BASE_NAME + "/region";
    public static final String ICU_ZONE_BASE_NAME = ICU_BASE_NAME + "/zone";

    /**
     * The actual path of the resource
     */
    protected String resPath;

    /**
     * The class loader constant to be used with getBundleInstance API
     */
    public static final ClassLoader ICU_DATA_CLASS_LOADER;
    static {
        ClassLoader loader = ICUData.class.getClassLoader();
        if (loader == null) {
            loader = Utility.getFallbackClassLoader();
        }
        ICU_DATA_CLASS_LOADER = loader;
    }

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
     * Returns the respath of this bundle
     * @return the respath of the bundle
     */
    public String getResPath(){
        return resPath;
    }

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
        ULocale locales[] = createULocaleList(baseName, ICU_DATA_CLASS_LOADER);
        int i;

        for (i = 0; i < locales.length; i++) {
            try {
                UResourceBundle b = UResourceBundle.getBundleInstance(baseName, locales[i]);
                // downcast to ICUResourceBundle?
                ICUResourceBundle irb = (ICUResourceBundle) (b.getObject(keyword));
                Enumeration<String> e = irb.getKeys();
                while (e.hasMoreElements()) {
                    String s = e.nextElement();
                    if (!DEFAULT_TAG.equals(s)) {
                        // don't add 'default' items
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
        ICUResourceBundle result = null;
        ICUResourceBundle actualBundle = this;

        // now recurse to pick up sub levels of the items
        result = findResourceWithFallback(path, actualBundle, null);

        if (result == null) {
            throw new MissingResourceException(
                "Can't find resource for bundle "
                + this.getClass().getName() + ", key " + getType(),
                path, getKey());
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

    // will throw type mismatch exception if the resource is not a string
    public String getStringWithFallback(String path) throws MissingResourceException {
        return getWithFallback(path).getString();
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

    // Flag for enabling/disabling debugging code
    private static final boolean DEBUG = ICUDebug.enabled("localedata");

    // Cache for getAvailableLocales
    private static SoftReference<Map<String, AvailEntry>> GET_AVAILABLE_CACHE;
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

    private static final Locale[] createLocaleList(String baseName, ClassLoader loader) {
        ULocale[] ulocales = getAvailEntry(baseName, loader).getULocaleList();
        return getLocaleList(ulocales);
    }

    private static final String[] createLocaleNameArray(String baseName,
            ClassLoader root) {
        ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.instantiateBundle( baseName, ICU_RESOURCE_INDEX, root, true);
        bundle = (ICUResourceBundle)bundle.get(INSTALLED_LOCALES);
        int length = bundle.getSize();
        int i = 0;
        String[] locales = new String[length];
        UResourceBundleIterator iter = bundle.getIterator();
        iter.reset();
        while (iter.hasNext()) {
            String locstr = iter.next(). getKey();
            if (locstr.equals("root")) {
                locales[i++] = ULocale.ROOT.toString();
            } else {
                locales[i++] = locstr;
            }
        }
        bundle = null;
        return locales;
    }

    private static final List<String> createFullLocaleNameArray(
            final String baseName, final ClassLoader root) {

        List<String> list = java.security.AccessController
            .doPrivileged(new java.security.PrivilegedAction<List<String>>() {
                public List<String> run() {
                    // WebSphere class loader will return null for a raw
                    // directory name without trailing slash
                    String bn = baseName.endsWith("/")
                        ? baseName
                        : baseName + "/";

                    List<String> resList = null;

                    // scan available locale resources under the base url first
                    try {
                        Enumeration<URL> urls = root.getResources(bn);
                        while (urls.hasMoreElements()) {
                            URL url = urls.nextElement();
                            URLHandler handler = URLHandler.get(url);
                            if (handler != null) {
                                final List<String> lst = new ArrayList<String>();
                                URLVisitor v = new URLVisitor() {
                                        public void visit(String s) {
                                            //TODO: This is ugly hack.  We have to figure out how
                                            // we can distinguish locale data from others
                                            if (s.endsWith(".res")) {
                                                String locstr = s.substring(0, s.length() - 4);
                                                if (locstr.contains("_") && !locstr.equals("res_index")) {
                                                    // locale data with country/script contain "_",
                                                    // except for res_index.res
                                                    lst.add(locstr);
                                                } else if (locstr.length() == 2 || locstr.length() == 3) {
                                                    // all 2-letter or 3-letter entries are all locale
                                                    // data at least for now
                                                    lst.add(locstr);
                                                } else if (locstr.equalsIgnoreCase("root")) {
                                                    // root locale is a special case
                                                    lst.add(ULocale.ROOT.toString());
                                                }
                                            }
                                        }
                                    };
                                handler.guide(v, false);

                                if (resList == null) {
                                    resList = new ArrayList<String>(lst);
                                } else {
                                    resList.addAll(lst);
                                }
                            } else {
                                if (DEBUG) System.out.println("handler for " + url + " is null");
                            }
                        }
                    } catch (IOException e) {
                        if (DEBUG) System.out.println("ouch: " + e.getMessage());
                        resList = null;
                    }

                    if (resList == null) {
                        // look for prebuilt indices next
                        try {
                            InputStream s = root.getResourceAsStream(bn + ICU_RESOURCE_INDEX + ".txt");
                            if (s != null) {
                                resList = new ArrayList<String>();
                                BufferedReader br = new BufferedReader(new InputStreamReader(s, "ASCII"));
                                String line;
                                while ((line = br.readLine()) != null) {
                                    if (line.length() != 0 && !line.startsWith("#")) {
                                        if (line.equalsIgnoreCase("root")) {
                                            resList.add(ULocale.ROOT.toString());
                                        } else {
                                            resList.add(line);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            // swallow it
                        }
                    }

                    return resList;
                }
            });

        return list;
    }

    private static Set<String> createFullLocaleNameSet(String baseName, ClassLoader loader) {
        List<String> list = createFullLocaleNameArray(baseName, loader);
        if(list == null){
            if (DEBUG) System.out.println("createFullLocaleNameArray returned null");
            // Use locale name set as the last resort fallback
            return createLocaleNameSet(baseName, loader);
        }
        HashSet<String> set = new HashSet<String>();
        set.addAll(list);
        return Collections.unmodifiableSet(set);
    }

    private static Set<String> createLocaleNameSet(String baseName, ClassLoader loader) {
        try {
            String[] locales = createLocaleNameArray(baseName, loader);

            HashSet<String> set = new HashSet<String>();
            set.addAll(Arrays.asList(locales));
            return Collections.unmodifiableSet(set);
        } catch (MissingResourceException e) {
            if (DEBUG) {
                System.out.println("couldn't find index for bundleName: " + baseName);
                Thread.dumpStack();
            }
        }
        return Collections.emptySet();
    }

    /**
     * Holds the prefix, and lazily creates the Locale[] list or the locale name
     * Set as needed.
     */
    private static final class AvailEntry {
        private String prefix;
        private ClassLoader loader;
        private ULocale[] ulocales;
        private Locale[] locales;
        private Set<String> nameSet;
        private Set<String> fullNameSet;

        AvailEntry(String prefix, ClassLoader loader) {
            this.prefix = prefix;
            this.loader = loader;
        }

        ULocale[] getULocaleList() {
            if (ulocales == null) {
                ulocales = createULocaleList(prefix, loader);
            }
            return ulocales;
        }
        Locale[] getLocaleList() {
            if (locales == null) {
              locales = createLocaleList(prefix, loader);
            }
            return locales;
        }
        Set<String> getLocaleNameSet() {
            if (nameSet == null) {
              nameSet = createLocaleNameSet(prefix, loader);
            }
            return nameSet;
        }
        Set<String> getFullLocaleNameSet() {
            if (fullNameSet == null) {
              fullNameSet = createFullLocaleNameSet(prefix, loader);
            }
            return fullNameSet;
        }
    }

    /**
     * Stores the locale information in a cache accessed by key (bundle prefix).
     * The cached objects are AvailEntries. The cache is held by a SoftReference
     * so it can be GC'd.
     */
  private static AvailEntry getAvailEntry(String key, ClassLoader loader) {
        AvailEntry ae = null;
        Map<String, AvailEntry> lcache = null;
        if (GET_AVAILABLE_CACHE != null) {
            lcache = GET_AVAILABLE_CACHE.get();
            if (lcache != null) {
                ae = lcache.get(key);
            }
        }

        if (ae == null) {
          ae = new AvailEntry(key, loader);
            if (lcache == null) {
                lcache = new HashMap<String, AvailEntry>();
                lcache.put(key, ae);
                GET_AVAILABLE_CACHE = new SoftReference<Map<String, AvailEntry>>(lcache);
            } else {
                lcache.put(key, ae);
            }
        }

        return ae;
    }

    protected static final ICUResourceBundle findResourceWithFallback(String path,
            UResourceBundle actualBundle, UResourceBundle requested) {
        ICUResourceBundle sub = null;
        if (requested == null) {
            requested = actualBundle;
        }
        while (actualBundle != null) {
            ICUResourceBundle current = (ICUResourceBundle) actualBundle;
            if (path.indexOf('/') == -1) { // skip the tokenizer
                sub = (ICUResourceBundle) current.handleGet(path, null, requested);
                if (sub != null) {
                    current = sub;
                    break;
                }
            } else {
                StringTokenizer st = new StringTokenizer(path, "/");
                while (st.hasMoreTokens()) {
                    String subKey = st.nextToken();
                    sub = (ICUResourceBundle) current.handleGet(subKey, null, requested);
                    if (sub == null) {
                        break;
                    }
                    current = sub;
                }
                if (sub != null) {
                    //we found it
                    break;
                }
            }
            if (((ICUResourceBundle)actualBundle).resPath.length() != 0) {
                path = ((ICUResourceBundle)actualBundle).resPath + "/" + path;
            }
            // if not try the parent bundle
            actualBundle = ((ICUResourceBundle) actualBundle).getParent();

        }
        if(sub != null){
            sub.setLoadingStatus(((ICUResourceBundle)requested).getLocaleID());
        }
        return sub;
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
    // This method is for super class's instantiateBundle method
    public static UResourceBundle getBundleInstance(String baseName, String localeID,
                                                    ClassLoader root, boolean disableFallback){
        UResourceBundle b = instantiateBundle(baseName, localeID, root, disableFallback);
        if(b==null){
            throw new MissingResourceException("Could not find the bundle "+ baseName+"/"+ localeID+".res","","");
        }
        return b;
    }
    //  recursively build bundle .. over-ride super class method.
    protected synchronized static UResourceBundle instantiateBundle(String baseName, String localeID,
                                                                    ClassLoader root, boolean disableFallback){
        ULocale defaultLocale = ULocale.getDefault();
        String localeName = localeID;
        if(localeName.indexOf('@')>0){
            localeName = ULocale.getBaseName(localeID);
        }
        String fullName = getFullName(baseName, localeName);
        ICUResourceBundle b = (ICUResourceBundle)loadFromCache(root, fullName, defaultLocale);

        // here we assume that java type resource bundle organization
        // is required then the base name contains '.' else
        // the resource organization is of ICU type
        // so clients can instantiate resources of the type
        // com.mycompany.data.MyLocaleElements_en.res and
        // com.mycompany.data.MyLocaleElements.res
        //
        final String rootLocale = (baseName.indexOf('.')==-1) ? "root" : "";
        final String defaultID = defaultLocale.toString();

        if(localeName.equals("")){
            localeName = rootLocale;
        }
        if(DEBUG) System.out.println("Creating "+fullName+ " currently b is "+b);
        if (b == null) {
            b = ICUResourceBundle.createBundle(baseName, localeName, root);

            if(DEBUG)System.out.println("The bundle created is: "+b+" and disableFallback="+disableFallback+" and bundle.getNoFallback="+(b!=null && b.getNoFallback()));
            if(disableFallback || (b!=null && b.getNoFallback())){
                // no fallback because the caller said so or because the bundle says so
                return addToCache(root, fullName, defaultLocale, b);
            }

            // fallback to locale ID parent
            if(b == null){
                int i = localeName.lastIndexOf('_');
                if (i != -1) {
                    String temp = localeName.substring(0, i);
                    b = (ICUResourceBundle)instantiateBundle(baseName, temp, root, disableFallback);
                    if(b!=null && b.getULocale().equals(temp)){
                        b.setLoadingStatus(ICUResourceBundle.FROM_FALLBACK);
                    }
                }else{
                    if(defaultID.indexOf(localeName)==-1){
                        b = (ICUResourceBundle)instantiateBundle(baseName, defaultID, root, disableFallback);
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

                boolean ParentIsRoot = false;
                if (b.getTableResource("%%ParentIsRoot") != RES_BOGUS) {
                    ParentIsRoot = true;
                }

                if (i != -1 && !ParentIsRoot) {
                    parent = instantiateBundle(baseName, localeName.substring(0, i), root, disableFallback);
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
    UResourceBundle get(String aKey, HashMap<String, String> table, UResourceBundle requested) {
        ICUResourceBundle obj = (ICUResourceBundle)handleGet(aKey, table, requested);
        if (obj == null) {
            obj = (ICUResourceBundle)getParent();
            if (obj != null) {
                //call the get method to recursively fetch the resource
                obj = (ICUResourceBundle)obj.get(aKey, table, requested);
            }
            if (obj == null) {
                String fullName = getFullName(getBaseName(), getLocaleID());
                throw new MissingResourceException(
                        "Can't find resource for bundle " + fullName + ", key "
                                + aKey, this.getClass().getName(), aKey);
            }
        }
        obj.setLoadingStatus(((ICUResourceBundle)requested).getLocaleID());
        return obj;
    }

    private static final String ICU_RESOURCE_SUFFIX = ".res";
    /**
     * Gets the full name of the resource with suffix.
     */
    public static String getFullName(String baseName, String localeName){
        if(baseName==null || baseName.length()==0){
            if(localeName.length()==0){
                return localeName=ULocale.getDefault().toString();   
            }
            return localeName+ICU_RESOURCE_SUFFIX;
        }else{
            if(baseName.indexOf('.')==-1){
                if(baseName.charAt(baseName.length()-1)!= '/'){
                    return baseName+"/"+localeName+ICU_RESOURCE_SUFFIX;
                }else{
                    return baseName+localeName+ICU_RESOURCE_SUFFIX;   
                }
            }else{
                baseName = baseName.replace('.','/');
                if(localeName.length()==0){
                    return baseName+ICU_RESOURCE_SUFFIX;   
                }else{
                    return baseName+"_"+localeName+ICU_RESOURCE_SUFFIX;
                }
            }
        }
    }

    protected String localeID;
    protected String baseName;
    protected ULocale ulocale;
    protected ClassLoader loader;

    /**
     * Access to the bits and bytes of the resource bundle.
     * Hides low-level details.
     */
    protected ICUResourceBundleReader reader;
    /** Data member where the subclasses store the key. */
    protected String key;
    /** Data member where the subclasses store the offset within resource data. */
    protected int resource;

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

    private static final ConcurrentHashMap<String, ICUResourceBundle> cache = 
        new ConcurrentHashMap<String, ICUResourceBundle>();
    private static final ICUResourceBundle NULL_BUNDLE = 
        new ICUResourceBundle(null, null, null, 0, null) {
        public int hashCode() {
            return 0;
        }
        public boolean equals(Object rhs) {
            return this == rhs;
        }
    };
    
   /**
    *
    * @param baseName The name for the bundle.
    * @param localeID The locale identification.
    * @param root The ClassLoader object root.
    * @return the new bundle
    */
    public static ICUResourceBundle createBundle(String baseName, String localeID, 
            ClassLoader root) {
        
        String resKey = Integer.toHexString(root.hashCode()) + baseName + localeID;
        ICUResourceBundle b = cache.get(resKey);
        if (b == null) {
            String resolvedName = getFullName(baseName, localeID);
            ICUResourceBundleReader reader = ICUResourceBundleReader.getReader(resolvedName, root);
            // could not open the .res file so return null
            if (reader == null) {
                b = NULL_BUNDLE;
            } else {
                b = getBundle(reader, baseName, localeID, root);
            }
            cache.put(resKey, b);
        }
        return b == NULL_BUNDLE ? null : b;
    }

    protected String getLocaleID() {
        return localeID;
    }

    protected String getBaseName() {
        return baseName;
    }

    public ULocale getULocale() {
        return ulocale;
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

    private static final int[] gPublicTypes = new int[] {
        STRING,
        BINARY,
        TABLE,
        ALIAS,

        TABLE,      /* TABLE32 */
        TABLE,      /* TABLE16 */
        STRING,     /* STRING_V2 */
        INT,

        ARRAY,
        ARRAY,      /* ARRAY16 */
        NONE,
        NONE,

        NONE,
        NONE,
        INT_VECTOR,
        NONE
    };

    public int getType() {
        return gPublicTypes[ICUResourceBundleReader.RES_GET_TYPE(resource)];
    }

    /**
     * Get the noFallback flag specified in the loaded bundle.
     * @return The noFallback flag.
     */
    private boolean getNoFallback() {
        return reader.getNoFallback();
    }

    private static ICUResourceBundle getBundle(ICUResourceBundleReader reader,
                                               String baseName, String localeID,
                                               ClassLoader loader) {
        ICUResourceBundleImpl bundle;
        int rootRes = reader.getRootResource();
        if(gPublicTypes[ICUResourceBundleReader.RES_GET_TYPE(rootRes)] == TABLE) {
            bundle = new ICUResourceBundleImpl.ResourceTable(reader, null, "", rootRes, null);
        } else {
            throw new IllegalStateException("Invalid format error");
        }
        bundle.baseName = baseName;
        bundle.localeID = localeID;
        bundle.ulocale = new ULocale(localeID);
        bundle.loader = loader;
        if(bundle.reader.getUsesPoolBundle()) {
            bundle.reader.setPoolBundleKeys(
                ((ICUResourceBundleImpl)getBundleInstance(baseName, "pool", loader, true)).reader);
        }
        UResourceBundle alias = bundle.handleGetImpl("%%ALIAS", null, bundle, null, null); // handleGet will cache the bundle with no parent set
        if(alias != null) {
            return (ICUResourceBundle)UResourceBundle.getBundleInstance(baseName, alias.getString());
        } else {
            return bundle;
        }
    }
    // constructor for inner classes
    protected ICUResourceBundle(ICUResourceBundleReader reader, String key, String resPath, int resource,
                                ICUResourceBundle container) {
        this.reader = reader;
        this.key = key;
        this.resPath = resPath;
        this.resource = resource;
        if(container != null) {
            baseName = container.baseName;
            localeID = container.localeID;
            ulocale = container.ulocale;
            loader = container.loader;
            this.parent = container.parent;
        }
    }

    private String getAliasValue(int res) {
        String result = reader.getAlias(res);
        return result != null ? result : "";
    }
    private static final char RES_PATH_SEP_CHAR = '/';
    private static final String RES_PATH_SEP_STR = "/";
    private static final String ICUDATA = "ICUDATA";
    private static final char HYPHEN = '-';
    private static final String LOCALE = "LOCALE";

    protected ICUResourceBundle findResource(String _key, int _resource,
                                             HashMap<String, String> table,
                                             UResourceBundle requested) {
        ClassLoader loaderToUse = loader;
        String locale = null, keyPath = null;
        String bundleName;
        String rpath = getAliasValue(_resource);
        if (table == null) {
            table = new HashMap<String, String>();
        }
        if (table.get(rpath) != null) {
            throw new IllegalArgumentException(
                    "Circular references in the resource bundles");
        }
        table.put(rpath, "");
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
            keyPath = rpath.substring(i + 1);
            if (i != -1) {
                locale = rpath.substring(0, i);
            } else {
                locale = keyPath;
                keyPath = null;//keyPath.substring(i, keyPath.length());
            }
            bundleName = baseName;
        }
        ICUResourceBundle bundle = null;
        ICUResourceBundle sub = null;
        if(bundleName.equals(LOCALE)){
            bundleName = baseName;
            bundle = (ICUResourceBundle)requested;
            keyPath = rpath.substring(LOCALE.length() + 2/* prepending and appending / */, rpath.length());
            locale = ((ICUResourceBundle)requested).getLocaleID();
            sub = ICUResourceBundle.findResourceWithFallback(keyPath, requested, null);
            if (sub != null) {
                sub.resPath = "/" + sub.getLocaleID() + "/" + keyPath;
            }
        }else{
            if (locale == null) {
                // {dlf} must use requestor's class loader to get resources from same jar
                bundle = (ICUResourceBundle) getBundleInstance(bundleName, "",
                         loaderToUse, false);
            } else {
                bundle = (ICUResourceBundle) getBundleInstance(bundleName, locale,
                         loaderToUse, false);
            }
            if (keyPath != null) {
                StringTokenizer st = new StringTokenizer(keyPath, "/");
                ICUResourceBundle current = bundle;
                while (st.hasMoreTokens()) {
                    String subKey = st.nextToken();
                    sub = (ICUResourceBundle)current.get(subKey, table, requested);
                    if (sub == null) {
                        break;
                    }
                    current = sub;
                }
            } else {
                // if the sub resource is not found
                // try fetching the sub resource with
                // the key of this alias resource
                sub = (ICUResourceBundle)bundle.get(_key);
            }
            if (sub != null) {
                sub.resPath = rpath;
            }
        }
        if (sub == null) {
            throw new MissingResourceException(localeID, baseName, _key);
        }
        return sub;
    }

    // Resource bundle lookup cache, which may be used by subclasses
    // which have nested resources
    protected ICUCache<Object, UResourceBundle> lookup;
    private static final int MAX_INITIAL_LOOKUP_SIZE = 64;

    protected void createLookupCache() {
        lookup = new SimpleCache<Object, UResourceBundle>(ICUCache.WEAK, Math.max(getSize()*2, MAX_INITIAL_LOOKUP_SIZE));
    }

    protected UResourceBundle handleGet(String resKey, HashMap<String, String> table, UResourceBundle requested) {
        UResourceBundle res = null;
        if (lookup != null) {
            res = lookup.get(resKey);
        }
        if (res == null) {
            int[] index = new int[1];
            boolean[] alias = new boolean[1];
            res = handleGetImpl(resKey, table, requested, index, alias);
            if (res != null && lookup != null && !alias[0]) {
                // We do not want to cache a result from alias entry
                lookup.put(resKey, res);
                lookup.put(Integer.valueOf(index[0]), res);
            }
        }
        return res;
    }

    protected UResourceBundle handleGet(int index, HashMap<String, String> table, UResourceBundle requested) {
        UResourceBundle res = null;
        Integer indexKey = null;
        if (lookup != null) {
            indexKey = Integer.valueOf(index);
            res = lookup.get(indexKey);
        }
        if (res == null) {
            boolean[] alias = new boolean[1];
            res = handleGetImpl(index, table, requested, alias);
            if (res != null && lookup != null && !alias[0]) {
                // We do not want to cache a result from alias entry
                lookup.put(res.getKey(), res);
                lookup.put(indexKey, res);
            }
        }
        return res;
    }

    // Subclass which supports key based resource access to implement this method
    protected UResourceBundle handleGetImpl(String resKey, HashMap<String, String> table, UResourceBundle requested,
            int[] index, boolean[] isAlias) {
        return null;
    }

    // Subclass which supports index based resource access to implement this method
    protected UResourceBundle handleGetImpl(int index, HashMap<String, String> table, UResourceBundle requested,
            boolean[] isAlias) {
        return null;
    }


     // TODO Below is a set of workarounds created for org.unicode.cldr.icu.ICU2LDMLWriter
     /* 
      * Calling getKeys() on a table that has alias's can throw a NullPointerException if parent is not set, 
      * see trac bug: 6514
      * -Brian Rower - IBM - Sept. 2008
      */
    
    /**
     * Returns the resource handle for the given key within the calling resource table.
     * 
     * @internal
     * @deprecated This API is ICU internal only and a workaround see ticket #6514.
     * @author Brian Rower
     */
    protected int getTableResource(String resKey) {
        return RES_BOGUS;
    }
    protected int getTableResource(int index) {
        return RES_BOGUS;
    }

    /**
     * Determines if the object at the specified index of the calling resource table
     * is an alias. If it is, returns true
     * 
     * @param index The index of the resource to check
     * @returns True if the resource at 'index' is an alias, false otherwise.
     * 
     * @internal
     * @deprecated This API is ICU internal only and part of a work around see ticket #6514
     * @author Brian Rower
     */
    public boolean isAlias(int index)
    {
        //TODO this is part of a workaround for ticket #6514
        //if index is out of the resource, return false.
        return ICUResourceBundleReader.RES_GET_TYPE(getTableResource(index)) == ALIAS;
    }

    /**
     * 
     * @internal
     * @deprecated This API is ICU internal only and part of a workaround see ticket #6514.
     * @author Brian Rower
     */
    public boolean isAlias()
    {
        //TODO this is part of a workaround for ticket #6514
        return ICUResourceBundleReader.RES_GET_TYPE(resource) == ALIAS;
    }

    /**
     * Determines if the object with the specified key 
     * is an alias. If it is, returns true
     * 
     * @returns True if the resource with 'key' is an alias, false otherwise.
     * 
     * @internal
     * @deprecated This API is ICU internal only and part of a workaround see ticket #6514.
     * @author Brian Rower
     */
    public boolean isAlias(String k)
    {
        //TODO this is part of a workaround for ticket #6514
        //this only applies to tables
        return ICUResourceBundleReader.RES_GET_TYPE(getTableResource(k)) == ALIAS;
    }

    /**
     * This method can be used to retrieve the underlying alias path (aka where the alias points to)
     * This method was written to allow conversion from ICU back to LDML format.
     * 
     * @param index The index where the alias path points to.
     * @return The alias path.
     * @author Brian Rower
     * @internal
     * @deprecated This API is ICU internal only.
     * @author Brian Rower
     */
    public String getAliasPath(int index)
    {
        return getAliasValue(getTableResource(index));
    }

    /**
     * 
     * @internal
     * @deprecated This API is ICU internal only
     * @author Brian Rower
     */
    public String getAliasPath()
    {
        //TODO cannot allow alias path to end up in public API
        return getAliasValue(resource);
    }

    /**
     * 
     * @internal
     * @deprecated This API is ICU internal only
     * @author Brian Rower
     */
    public String getAliasPath(String k)
    {
        //TODO cannot allow alias path to end up in public API
        return getAliasValue(getTableResource(k));
    }
    
    /*
     * Helper method for getKeysSafe
     */
    protected String getKey(int index) {
        return null;
    }

    /**
     * Returns an Enumeration of the keys belonging to this table or array.
     * This method differs from the getKeys() method by not following alias paths. This method exposes 
     * underlying alias's. For all general purposes of the ICU resource bundle please use getKeys().
     * 
     * @return Keys in this table or array.
     * @internal
     * @deprecated This API is ICU internal only and a workaround see ticket #6514.
     * @author Brian Rower
     */
    public Enumeration<String> getKeysSafe()
    {
        //TODO this is part of a workaround for ticket #6514
        //the safeness only applies to tables, so use the other method if it's not a table
        if(!ICUResourceBundleReader.URES_IS_TABLE(resource))
        {
            return getKeys();
        }
        Vector<String> v = new Vector<String>();
        int size = getSize();
        for(int index = 0; index < size; index++)
        {
            String curKey = getKey(index); 
            v.add(curKey);
        }
        return v.elements();
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
        return resPath.length() == 0;
    }
}
