/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.util.ULocale;

public class ICULocaleService extends ICUService {
    private Locale fallbackLocale;
    private String fallbackLocaleName;

    /**
     * Construct an ICULocaleService.
     */
    public ICULocaleService() {
    }

    /**
     * Construct an ICULocaleService with a name (useful for debugging).
     */
    public ICULocaleService(String name) {
        super(name);
    }

    /**
     * Convenience override for callers using locales.  This calls
     * get(Locale, int, Locale[]) with KIND_ANY for kind and null for
     * actualReturn.
     */
    public Object get(Locale locale) {
        return get(locale, LocaleKey.KIND_ANY, null);
    }

    /**
     * Convenience override for callers using locales.  This calls
     * get(Locale, int, Locale[]) with a null actualReturn.
     */
    public Object get(Locale locale, int kind) {
        return get(locale, kind, null);
    }

    /**
     * Convenience override for callers using locales. This calls
     * get(Locale, int, Locale[]) with KIND_ANY.
     */
    public Object get(Locale locale, Locale[] actualReturn) {
        return get(locale, LocaleKey.KIND_ANY, actualReturn);
    }
                   
    /**
     * Convenience override for callers using locales. This calls
     * get(ULocale, int, ULocale[]) with KIND_ANY.
     */
    public Object get(ULocale locale, ULocale[] actualReturn) {
        return get(locale, LocaleKey.KIND_ANY, actualReturn);
    }
                   
    /**
     * Convenience override for callers using locales.  This uses
     * createKey(ULocale.toString(), kind) to create a key, calls getKey, and then
     * if actualReturn is not null, returns the actualResult from
     * getKey (stripping any prefix) into a ULocale.  
     */
    public Object get(ULocale locale, int kind, ULocale[] actualReturn) {
        String name = locale.getName();
        if (name.length() > 0) { // arrgh, canonicalize turns "" into en_US_POSIX
            name = ULocale.canonicalize(name);
        }
        Key key = createKey(name, kind);
        if (actualReturn == null) {
            return getKey(key);
        }

        String[] temp = new String[1];
        Object result = getKey(key, temp);
        if (result != null) {
            int n = temp[0].indexOf("/");
            if (n >= 0) {
                temp[0] = temp[0].substring(n+1);
            }
            actualReturn[0] = new ULocale(temp[0]);
        }
        return result;
    }

    /**
     * Convenience override for callers using locales.  This uses
     * createKey(Locale.toString(), kind) to create a key, calls getKey, and then
     * if actualReturn is not null, returns the actualResult from
     * getKey (stripping any prefix) into a Locale.  
     */
    public Object get(Locale locale, int kind, Locale[] actualReturn) {
        Key key = createKey(locale.toString(), kind);
        if (actualReturn == null) {
            return getKey(key);
        }

        String[] temp = new String[1];
        Object result = getKey(key, temp);
        if (result != null) {
            int n = temp[0].indexOf("/");
            if (n >= 0) {
                temp[0] = temp[0].substring(n+1);
            }
            actualReturn[0] = LocaleUtility.getLocaleFromName(temp[0]);
        }
        return result;
    }

    /**
     * Convenience override for callers using locales.  This calls
     * registerObject(Object, Locale, int kind, int coverage)
     * passing KIND_ANY for the kind, and VISIBLE for the coverage.
     */
    public Factory registerObject(Object obj, Locale locale) {
        return registerObject(obj, locale, LocaleKey.KIND_ANY, LocaleKeyFactory.VISIBLE);
    }

    /**
     * Convenience function for callers using locales.  This calls
     * registerObject(Object, Locale, int kind, int coverage)
     * passing VISIBLE for the coverage.
     */
    public Factory registerObject(Object obj, Locale locale, int kind) {
        return registerObject(obj, locale, kind, LocaleKeyFactory.VISIBLE);
    }

    /**
     * Convenience function for callers using locales.  This  instantiates
     * a SimpleLocaleKeyFactory, and registers the factory.
     */
    public Factory registerObject(Object obj, Locale locale, int kind, int coverage) {
        Factory factory = new SimpleLocaleKeyFactory(obj, locale, kind, coverage);
        return registerFactory(factory);
    }

    /**
     * Convenience method for callers using locales.  This returns the standard
     * Locale list, built from the Set of visible ids.
     */
    public Locale[] getAvailableLocales() {
        // TODO make this wrap getAvailableULocales later
        Set visIDs = getVisibleIDs();
        Iterator iter = visIDs.iterator();
        Locale[] locales = new Locale[visIDs.size()];
        int n = 0;
        while (iter.hasNext()) {
            Locale loc = LocaleUtility.getLocaleFromName((String)iter.next());
            locales[n++] = loc;
        }
        return locales;
    }

    /**
     * Convenience method for callers using locales.  This returns the standard
     * ULocale list, built from the Set of visible ids.
     */
    public ULocale[] getAvailableULocales() {
        Set visIDs = getVisibleIDs();
        Iterator iter = visIDs.iterator();
        ULocale[] locales = new ULocale[visIDs.size()];
        int n = 0;
        while (iter.hasNext()) {
            locales[n++] = new ULocale((String)iter.next());
        }
        return locales;
    }
        
    /**
     * A subclass of Key that implements a locale fallback mechanism.
     * The first locale to search for is the locale provided by the
     * client, and the fallback locale to search for is the current
     * default locale.  If a prefix is present, the currentDescriptor
     * includes it before the locale proper, separated by "/".  This
     * is the default key instantiated by ICULocaleService.</p>
     *
     * <p>Canonicalization adjusts the locale string so that the
     * section before the first understore is in lower case, and the rest
     * is in upper case, with no trailing underscores.</p> 
     */
    public static class LocaleKey extends ICUService.Key {
        private int kind;
        private int varstart;
        private String primaryID;
        private String fallbackID;
        private String currentID;

        public static final int KIND_ANY = -1;

        /**
         * Create a LocaleKey with canonical primary and fallback IDs.
         */
        public static LocaleKey createWithCanonicalFallback(String primaryID, String canonicalFallbackID) {
            return createWithCanonicalFallback(primaryID, canonicalFallbackID, KIND_ANY);
        }
            
        /**
         * Create a LocaleKey with canonical primary and fallback IDs.
         */
        public static LocaleKey createWithCanonicalFallback(String primaryID, String canonicalFallbackID, int kind) {
            if (primaryID == null) {
                return null;
            }
            String canonicalPrimaryID = ULocale.getName(primaryID);
            return new LocaleKey(primaryID, canonicalPrimaryID, canonicalFallbackID, kind);
        }
            
        /**
         * PrimaryID is the user's requested locale string,
         * canonicalPrimaryID is this string in canonical form,
         * fallbackID is the current default locale's string in
         * canonical form.
         */
        protected LocaleKey(String primaryID, String canonicalPrimaryID, String canonicalFallbackID, int kind) {
            super(primaryID);

            this.kind = kind;
            if (canonicalPrimaryID == null) {
                this.primaryID = "";
            } else {
                this.primaryID = canonicalPrimaryID;
                this.varstart = this.primaryID.indexOf('@');
            }
            if (this.primaryID == "") {
                this.fallbackID = null;
            } else {
                if (canonicalFallbackID == null || this.primaryID.equals(canonicalFallbackID)) {
                    this.fallbackID = "";
                } else {
                    this.fallbackID = canonicalFallbackID;
                }
            }

            this.currentID = varstart == -1 ? this.primaryID : this.primaryID.substring(0, varstart);
        }

        /**
         * Return the prefix associated with the kind, or null if the kind is KIND_ANY.
         */
        public String prefix() {
            return kind == KIND_ANY ? null : Integer.toString(kind());
        }

        /**
         * Return the kind code associated with this key.
         */
        public int kind() {
            return kind;
        }

        /**
         * Return the (canonical) original ID.
         */
        public String canonicalID() {
            return primaryID;
        }

        /**
         * Return the (canonical) current ID, or null if no current id.
         */
        public String currentID() {
            return currentID;
        }

        /**
         * Return the (canonical) current descriptor, or null if no current id.
         * Includes the keywords, whereas the ID does not include keywords.
         */
        public String currentDescriptor() {
            String result = currentID();
            if (result != null) {
                result = "/" + result;
                if (varstart != -1) {
                    result += primaryID.substring(varstart);
                }
                if (kind != KIND_ANY) {
                    result = prefix() + result;
                }
            }
            return result;
        }

        /**
         * Convenience method to return the locale corresponding to the (canonical) original ID.
         */
        public Locale canonicalLocale() {
            return LocaleUtility.getLocaleFromName(primaryID);
        }

        /**
         * Convenience method to return the locale corresponding to the (canonical) current ID.
         */
        public Locale currentLocale() {
            return LocaleUtility.getLocaleFromName(currentID);
        }

        /**
         * Convenience method to return the ulocale corresponding to the (canonical) currentID.
         */
        public ULocale currentULocale() {
            if (varstart == -1) {
                return new ULocale(currentID);
            } else {
                return new ULocale(currentID + primaryID.substring(varstart));
            }
        }

        /**
         * If the key has a fallback, modify the key and return true,
         * otherwise return false.</p>
         *
         * <p>First falls back through the primary ID, then through
         * the fallbackID.  The final fallback is the empty string,
         * unless the primary id was the empty string, in which case
         * there is no fallback.  
         */
        public boolean fallback() {
            int x = currentID.lastIndexOf('_');
            if (x != -1) {
                currentID = currentID.substring(0, x);
                return true;
            }
            if (fallbackID != null) {
                currentID = fallbackID;
                fallbackID = fallbackID.length() == 0 ? null : "";
                return true;
            }
            currentID = null;
            return false;
        }

        /**
         * If a key created from id would eventually fallback to match the 
         * canonical ID of this key, return true.
         */
        public boolean isFallbackOf(String id) {
            return LocaleUtility.isFallbackOf(canonicalID(), id);
        }
    }

    /**
     * A subclass of Factory that uses LocaleKeys, and is able to
     * 'cover' more specific locales with more general locales that it
     * supports.  
     *
     * <p>Coverage may be either of the values VISIBLE or INVISIBLE.
     *
     * <p>'Visible' indicates that the specific locale(s) supported by
     * the factory are registered in getSupportedIDs, 'Invisible'
     * indicates that they are not.
     *
     * <p>Localization of visible ids is handled
     * by the handling factory, regardless of kind.
     */
    public static abstract class LocaleKeyFactory implements Factory {
        protected final String name;
        protected final int coverage;

        /**
         * Coverage value indicating that the factory makes
         * its locales visible, and does not cover more specific 
         * locales.
         */
        public static final int VISIBLE = 0;

        /**
         * Coverage value indicating that the factory does not make
         * its locales visible, and does not cover more specific
         * locales.
         */
        public static final int INVISIBLE = 1;

        // undefine these since hiding other factories opens a big bag of worms
        /*
         * Coverage value indicating that the factory makes
         * its locales visible, covers more specific 
         * locales, and provides localization for the covered
         * locales.
         *
         public static final int VISIBLE_COVERS = 2;

         /**
         * Coverage value indicating that the factory does not
         * make its locales visible, covers more specific
         * locales, and also does not allow the locales it
         * covers to be visible.
         *
         public static final int INVISIBLE_COVERS = 3;

         /**
         * Coverage value indicating that the factory makes
         * its locales visible, covers more specific 
         * locales, but does not allow the locales it covers
         * to be visible.
         *
         public static final int VISIBLE_COVERS_REMOVE = 6;
        */

        
        /**
         * Constructor used by subclasses.
         */
        protected LocaleKeyFactory(int coverage) {
            this.coverage = coverage;
            this.name = null;
        }

        /**
         * Constructor used by subclasses.
         */
        protected LocaleKeyFactory(int coverage, String name) {
            this.coverage = coverage;
            this.name = name;
        }

        /**
         * Implement superclass abstract method.  This checks the currentID of
         * the key against the supported IDs, and passes the canonicalLocale and
         * kind off to handleCreate (which subclasses must implement).
         */
        public Object create(Key key, ICUService service) {
            if (handlesKey(key)) {
                LocaleKey lkey = (LocaleKey)key;
                int kind = lkey.kind();
                
                if (supportsULocale()) {
                    ULocale uloc = lkey.currentULocale();
                    return handleCreate(uloc, kind, service);
                } else {
                    Locale loc = lkey.currentLocale();
                    return handleCreate(loc, kind, service);
                }
            } else {
                // System.out.println("factory: " + this + " did not support id: " + key.currentID());
                // System.out.println("supported ids: " + getSupportedIDs());
            }
            return null;
        }

        protected boolean handlesKey(Key key) {
            if (key == null) {
                return false;
            }

            String id = key.currentID();
            Set supported = getSupportedIDs();
            return supported.contains(id);
            /*
             * coverage not supported

             if (supported.contains(id)) {
             return true;
             }
             if ((coverage & 0x2) != 0) { 
             Iterator iter = supported.iterator();
             while (iter.hasNext()) {
             String s = (String)iter.next();
             if (LocaleUtility.isFallbackOf(s, id)) {
             return true;
             }
             }
             }
             return false;
            */
        }

        /**
         * Override of superclass method.  This adjusts the result based
         * on the coverage rule for this factory.
         */
        public void updateVisibleIDs(Map result) {
            Set cache = getSupportedIDs();
            
            boolean visible = (coverage & 0x1) == 0;
            // boolean covers = (coverage & 0x2) != 0;
            // boolean removes = !visible || (coverage & 0x4) != 0;

            // System.out.println("vis: " + visible + " covers: " + covers + " removes: " + removes);
            Map toRemap = new HashMap();
            Iterator iter = cache.iterator();
            while (iter.hasNext()) {
                String id = (String)iter.next();
                /*
                 * Coverage not supported
                 if (covers) {
                 int idlen = id.length();
                 Iterator miter = result.keySet().iterator();
                 while (miter.hasNext()) {
                 String mid = (String)miter.next();
                 if (mid.startsWith(id) &&
                 (mid.length() == idlen ||
                 mid.charAt(idlen) == '_')) {

                 if (removes) {
                 miter.remove();
                 } else {
                 toRemap.put(mid, this);
                 }
                 }
                 }
                 }
                */
                if (!visible) {
                    result.remove(id);
                } else {
                    toRemap.put(id, this);
                }

            }                    
            result.putAll(toRemap);
        }

        /**
         * Return a localized name for the locale represented by id.
         */
        public String getDisplayName(String id, Locale locale) {
            // assume if the user called this on us, we must have handled some fallback of this id
            //          if (isSupportedID(id)) {
            if (locale == null) {
                return id;
            }
            Locale loc = LocaleUtility.getLocaleFromName(id);
            return loc.getDisplayName(locale);
            //              }
            //          return null;
        }

        /**
         * Return true if this object supports ULocale.  If so, then
         * handleCreate(ULocale,...) will be called to instantiate objects.
         * Otherwise handleCreate(Locale,...) will be called.  By default,
         * subclasses do NOT support ULocale.
         *
         * SUBCLASSES should either:
         *
         * [1] implement handleCreate(Locale,...)
         *
         * or (preferably)
         *
         * [2] implement handleCreate(ULocale,...) and override
         *     supportsULocale() to return true
         */
        protected boolean supportsULocale() {
            return false;   
        }
    
        ///CLOVER:OFF
        /**
         * Utility method used by create(Key, ICUService).  Subclasses can
         * implement this instead of create.  Subclasses should implement
         * either this method or handleCreate(ULocale,...), but NOT BOTH.
         */
        protected Object handleCreate(Locale loc, int kind, ICUService service) {
            return null;
        }
        ///CLOVER:ON

        ///CLOVER:OFF
        /**
         * Utility method used by create(Key, ICUService).  Subclasses can
         * implement this instead of create.  Subclasses should implement
         * either this method or handleCreate(Locale,...), but NOT BOTH.
         * If a subclass implements this method, it should also override
         * supportULocale() to return true.
         */
        protected Object handleCreate(ULocale loc, int kind, ICUService service) {
            return null;
        }
        ///CLOVER:ON

        /**
         * Return true if this id is one the factory supports (visible or 
         * otherwise).
         */
        protected boolean isSupportedID(String id) {
            return getSupportedIDs().contains(id);
        }
        
        /**
         * Return the set of ids that this factory supports (visible or 
         * otherwise).  This can be called often and might need to be
         * cached if it is expensive to create.
         */
        protected Set getSupportedIDs() {
            return Collections.EMPTY_SET;
        }

        /**
         * For debugging.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer(super.toString());
            if (name != null) {
                buf.append(", name: ");
                buf.append(name);
            }
            buf.append(", coverage: ");
            String[] coverage_names = {
                "visible", "invisible", "visible_covers", "invisible_covers", "????", "visible_covers_remove"
            };
            buf.append(coverage_names[coverage]);
            return buf.toString();
        }
    }

    /**
     * A LocaleKeyFactory that just returns a single object for a kind/locale.
     */
    public static class SimpleLocaleKeyFactory extends LocaleKeyFactory {
        private final Object obj;
        private final String id;
        private final int kind;

        public SimpleLocaleKeyFactory(Object obj, Locale locale, int kind, int coverage) {
            this(obj, locale, kind, coverage, null);
        }

        public SimpleLocaleKeyFactory(Object obj, Locale locale, int kind, int coverage, String name) {
            super(coverage, name);
            
            this.obj = obj;
            this.id = LocaleUtility.canonicalLocaleString(locale.toString());
            this.kind = kind;
        }

        /**
         * Returns the service object if kind/locale match.  Service is not used.
         */
        public Object create(Key key, ICUService service) {
            LocaleKey lkey = (LocaleKey)key;
            if (kind == LocaleKey.KIND_ANY || kind == lkey.kind()) {
                String keyID = lkey.currentID();
                if (id.equals(keyID)) {
                    return obj;
                }
            }
            return null;
        }

        protected boolean isSupportedID(String id) {
            return this.id.equals(id);
        }

        public void updateVisibleIDs(Map result) {
            if ((coverage & 0x1) == 0) {
                result.put(id, this);
            } else {
                result.remove(id);
            }
        }

        public String toString() {
            StringBuffer buf = new StringBuffer(super.toString());
            buf.append(", id: ");
            buf.append(id);
            buf.append(", kind: ");
            buf.append(kind);
            return buf.toString();
        }
    }

    /**
     * A LocaleKeyFactory that creates a service based on the ICU locale data.
     * This is a base class for most ICU factories.  Subclasses instantiate it
     * with a constructor that takes a bundle name, which determines the supported
     * IDs.  Subclasses then override handleCreate to create the actual service
     * object.  The default implementation returns a resource bundle.
     */
    public static class ICUResourceBundleFactory extends LocaleKeyFactory {
        protected final String bundleName;

        /**
         * Convenience constructor that uses the main ICU bundle name.
         */
        public ICUResourceBundleFactory() {
            this(ICUResourceBundle.ICU_BASE_NAME);
        }

        /**
         * A service factory based on ICU resource data in resources
         * with the given name.
         */
        public ICUResourceBundleFactory(String bundleName) {
            super(VISIBLE);

            this.bundleName = bundleName;
        }

        /**
         * Return the supported IDs.  This is the set of all locale names in ICU_BASE_NAME.
         */
        protected Set getSupportedIDs() {
            return ICUResourceBundle.getAvailableLocaleNameSet(bundleName);
        }

        /**
         * Create the service.  The default implementation returns the resource bundle
         * for the locale, ignoring kind, and service.
         */
        protected Object handleCreate(Locale loc, int kind, ICUService service) {
            return ICUResourceBundle.getBundleInstance(bundleName, loc);
        }

        public String toString() {
            return super.toString() + ", bundle: " + bundleName;
        }
    }

    /**
     * Return the name of the current fallback locale.  If it has changed since this was
     * last accessed, the service cache is cleared.
     */
    public String validateFallbackLocale() {
        Locale loc = Locale.getDefault();
        if (loc != fallbackLocale) {
            synchronized (this) {
                if (loc != fallbackLocale) {
                    fallbackLocale = loc;
                    fallbackLocaleName = LocaleUtility.canonicalLocaleString(loc.toString());
                    clearServiceCache();
                }
            }
        }
        return fallbackLocaleName;
    }

    public Key createKey(String id) {
        return LocaleKey.createWithCanonicalFallback(id, validateFallbackLocale());
    }

    public Key createKey(String id, int kind) {
        return LocaleKey.createWithCanonicalFallback(id, validateFallbackLocale(), kind);
    }
}
