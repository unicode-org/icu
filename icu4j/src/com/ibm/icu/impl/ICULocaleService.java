/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/ICULocaleService.java,v $
 * $Date: 2002/09/14 21:36:30 $
 * $Revision: 1.7 $
 *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

public class ICULocaleService extends ICUService {
    private Locale fallbackLocale;
    private String fallbackLocaleName;

    /**
     * Construct an ICULocaleService.  This uses the current default locale as a fallback.
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
     * Convenience override for callers using locales.
     */
    public Object get(Locale locale) {
        return get(locale, null);
    }

    /**
     * Convenience override for callers using locales.
     */
    public Object get(Locale locale, Locale[] actualReturn) {
        if (actualReturn == null) {
            return get(locale.toString());
        }
        String[] temp = new String[1];
        Object result = get(locale.toString(), temp);
        if (result != null) {
            actualReturn[0] = LocaleUtility.getLocaleFromName(temp[0]);
        }
        return result;
    }

    /**
     * Convenience override for callers using locales.
     */
    public Factory registerObject(Object obj, Locale locale) {
        return registerObject(obj, locale, true);
    }

    /**
     * Convenience override for callers using locales.
     */
    public Factory registerObject(Object obj, Locale locale, boolean visible) {
        return registerObject(obj, locale.toString(), visible);
    }

    /**
     * Convenience method for callers using locales.  This is the 
     * current typical API for this operation, though perhaps it should change.
     */
    public Locale[] getAvailableLocales() {
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
        private String prefix;
	private String primaryID;
	private String fallbackID;
	private String currentID;

	/**
	 * Create a LocaleKey with canonical primary and fallback IDs.
	 */
	public static LocaleKey createWithCanonicalFallback(String primaryID, String canonicalFallbackID) {
            return createWithCanonicalFallback(primaryID, canonicalFallbackID, null);
	}
	    
	/**
	 * Create a LocaleKey with canonical primary and fallback IDs.
	 */
	public static LocaleKey createWithCanonicalFallback(String primaryID, String canonicalFallbackID, String prefix) {
            String canonicalPrimaryID = LocaleUtility.canonicalLocaleString(primaryID);
	    return new LocaleKey(primaryID, canonicalPrimaryID, canonicalFallbackID, prefix);
	}
	    
	/**
	 * PrimaryID is the user's requested locale string,
	 * canonicalPrimaryID is this string in canonical form,
	 * fallbackID is the current default locale's string in
	 * canonical form.
	 */
        protected LocaleKey(String primaryID, String canonicalPrimaryID, String canonicalFallbackID, String prefix) {
	    super(primaryID);

            this.prefix = prefix;
	    
	    if (canonicalPrimaryID == null) {
		this.primaryID = "";
	    } else {
		this.primaryID = canonicalPrimaryID;
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

	    this.currentID = this.primaryID;
        }

        /**
         * Return the prefix, or null if none was defined. 
         */
        public String prefix() {
            return prefix;
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
         */
        public String currentDescriptor() {
            String result = currentID();
            if (result != null && prefix != null) {
                result = prefix + "/" + result;
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
    }

    /**
     * This is a factory that handles multiple keys, and records
     * information about the keys it handles or doesn't handle.  This
     * allows it to quickly filter subsequent queries on keys it has
     * seen before.  Subclasses implement handleCreate instead of
     * create.  Before updateVisibleIDs is called, it keeps track of
     * keys that it doesn't handle.  If its ids are visible, once
     * updateVisibleIDs is called, it builds a set of all the keys it
     * does handle and keeps track of the keys it does handle.  
     */
    public static abstract class MultipleKeyFactory implements ICUService.Factory {
	protected final boolean visible;
	private SoftReference cacheref;

        private static final class CacheInfo {
            final Set cache;
            final boolean included;

            CacheInfo() {
                this.cache = new HashSet();
                this.included = false;
            }

            CacheInfo(Set cache) {
                this.cache = cache;
                this.included = true;
            }

            /**
             * Return true if we're known to support id, or not known to not support id.
             */
            boolean tryCreate(String id) {
                boolean result = cache.contains(id) == included;
                return result;
            }

            /**
             * Update information about whether we support this id.  Since if we are storing
             * information on included ids, we already know all of them, we only need to
             * update if we're storing information on ids we don't support and we don't
             * support the id (the result is null).
             */
            void addCreate(String id, Object result) {
                if (!included && result == null) {
                    cache.add(id);
                }
            }
        }

	/**
	 * Convenience overload of MultipleKeyFactory(boolean) that defaults
	 * visible to true.
	 */
	public MultipleKeyFactory() {
	    this(true);
	}

	/**
	 * Constructs a MultipleKeyFactory whose ids are visible iff visible is true.
	 */
	public MultipleKeyFactory(boolean visible) {
	    this.visible = visible;
	}

	/**
	 * Get the cache of IDs.  These are either the ids that we know we
	 * don't understand, if included is false, or the entire set of ids
	 * we do know we understand, if included is true.  If the cache has
         * been flushed, included is false.
	 */
	private CacheInfo getCache() {
	    CacheInfo result = null;
	    if (cacheref != null) {
		result = (CacheInfo)cacheref.get();
	    }
	    if (result == null) {
		result = new CacheInfo();
                cacheref = new SoftReference(result);
	    }
	    return result;
	}

	/**
	 * Get the cache of IDs we understand.
	 */
	protected Set getSupportedIDs() {
	    CacheInfo ci = getCache();
            Set result = ci.cache;
	    if (!ci.included) {
                result = handleGetSupportedIDs();
		cacheref = new SoftReference(new CacheInfo(result));
            }
            
	    return result;
	}

	public Object create(Key key) {
	    Object result = null;
	    String id = key.currentID();
	    CacheInfo ci = getCache();
            if (ci.tryCreate(id)) {
		result = handleCreate(key);
                ci.addCreate(id, result);
	    }
	    return result;
	}

	public void updateVisibleIDs(Map result) {
	    if (visible) {
                Iterator iter = getSupportedIDs().iterator();
                while (iter.hasNext()) {
                    result.put(iter.next(), this);
                }
	    }
	}

	public String getDisplayName(String id, Locale locale) {
	    if (visible) {
		Set cache = getSupportedIDs();
		if (cache.contains(id)) {
		    return handleGetDisplayName(id, locale);
		}
	    }
	    return null;
	}

	/**
	 * Subclasses implement this instead of create.
	 */
	protected abstract Object handleCreate(Key key);

	/**
	 * Subclasses implement this instead of getSupportedIDs.  Any
	 * id known to and handled by this class should be included in
         * the returned Set.
	 */
	protected abstract Set handleGetSupportedIDs();

	/**
	 * Subclasses implement this instead of getDisplayName.
	 * Return the display name for the (visible) id in the
	 * provided locale.  The default implementation just returns
	 * the id.  
	 */
	protected String handleGetDisplayName(String id, Locale locale) {
	    return id;
	}
    }

    /**
     * A subclass of MultipleKeyFactory that uses LocaleKeys.  It is
     * able to optionally 'hide' more specific locales with more general
     * locales that it supports.
     */
    public static abstract class LocaleKeyFactory extends MultipleKeyFactory {
        protected final boolean hides;

        /**
         * Create a LocaleKeyFactory.
         */
        public LocaleKeyFactory(boolean visible, boolean hides) {
            super(visible);

            this.hides = hides;
        }

        /**
         * Override of superclass method.  If this is visible, it will update
         * result with the ids it supports.  If this hides ids, more specific
         * ids already in result will be remapped to this.
         */
	public void updateVisibleIDs(Map result) {
	    if (visible) {
		Set cache = getSupportedIDs();
                Map toRemap = new HashMap();
		Iterator iter = cache.iterator();
		while (iter.hasNext()) {
                    String id = (String)iter.next();
                    if (hides) {
                        int idlen = id.length();
                        Iterator miter = result.keySet().iterator();
                        while (miter.hasNext()) {
                            String mid = (String)miter.next();
                            if (mid.startsWith(id) &&
                                (mid.length() == idlen ||
                                 mid.charAt(idlen) == '_')) {

                                toRemap.put(mid, this);
                                miter.remove();
                            }
                        }                            
                    }
                    toRemap.put(id, this);
		}
                result.putAll(toRemap);
	    }
	}

	/**
	 * Return a localized name for the locale represented by id.
	 */
	protected String handleGetDisplayName(String id, Locale locale) {
	    // use java's display name formatting for now
	    return LocaleUtility.getLocaleFromName(id).getDisplayName(locale);
	}
    }

    /**
     * A factory that creates a service based on the ICU locale data.
     * Subclasses specify a prefix (default is LocaleElements), a
     * semicolon-separated list of required resources, and a visible flag.
     * This factory will search the ICU locale data for a bundle with
     * the exact prefix.  Then it will test whether the required resources
     * are all in this exact bundle.  If so, it instantiates the full
     * resource bundle, and hands it to createServiceFromResource, which
     * subclasses must implement.  Otherwise it returns null.
     */
    public static class ICUResourceBundleFactory extends MultipleKeyFactory {
	protected final String name;
	protected final String[][] requiredContents;

        public ICUResourceBundleFactory(boolean visible) {
            this((String)null, visible);
        }

	/**
	 * A service factory based on ICU resource data in the LocaleElements resources.
	 */
	public ICUResourceBundleFactory(String requiredContents, boolean visible) {
	    this(ICULocaleData.LOCALE_ELEMENTS, requiredContents, visible);
	}

	/**
	 * A service factory based on ICU resource data in resources
	 * with the given name.  If requiredContents is not null, all
	 * listed resources must come directly from the same bundle.  
	 */
	public ICUResourceBundleFactory(String name, String requiredContents, boolean visible) {
            this(name, buildRcAndOr(requiredContents), true, visible);
        }

        private static class Node {
            public boolean test(ResourceBundle rb) {
                return rb != null;
            }
        }

        private static class ResourceNode {
            String name;
        }

        private static class BoolNode extends Node {
            BoolNode car;
            BoolNode cdr;
        }

        private static String[][] buildRcAndOr(String requiredContents) {
            String[][] rcAndOr = null;
            if (requiredContents != null) {
                rcAndOr = new String[][] { parseDelimitedString(requiredContents) };
            }
            return rcAndOr;
        }

        public ICUResourceBundleFactory(String[] rcOr, boolean visible) {
            this(ICULocaleData.LOCALE_ELEMENTS, rcOr, visible);
        }

        public ICUResourceBundleFactory(String name, String[] rcOr, boolean visible) {
            this(name, buildRcAndOr(rcOr), true, visible);
        }

        private static String[][] buildRcAndOr(String[] rcOr) {
            String[][] rcOrAnd = null;
            if (rcOr != null) {
                rcOrAnd = new String[rcOr.length][];
                for (int i = 0; i < rcOr.length; ++i) {
                    rcOrAnd[i] = parseDelimitedString(rcOr[i]);
                }
            }
            return rcOrAnd;
        }

        public ICUResourceBundleFactory(String[][] rcOrAnd, boolean adopt, boolean visible) {
            this(ICULocaleData.LOCALE_ELEMENTS, rcOrAnd, adopt, visible);
        }

        private static String[] parseDelimitedString(String str) {
            if (str != null) {
		ArrayList list = new ArrayList();
		for (int i = 0, len = str.length();;) {
		    while (i < len && str.charAt(i) == ';') {
			++i;
		    }
		    if (i == len) {
		      break;
		    }
		    int j = str.indexOf(';', i);
		    if (j == -1) {
		      j = len;
		    }
		    list.add(str.substring(i, j));
		    i = j;
		}
		return (String[])list.toArray(new String[list.size()]);
	    }
            return null;
        }

        public ICUResourceBundleFactory(String name, String[][] rcOrAnd, boolean adopt, boolean visible) {
	    super(visible);

	    this.name = name;
            
            if (!adopt && rcOrAnd != null) {
                rcOrAnd = (String[][])rcOrAnd.clone();
                for (int i = 0; i < rcOrAnd.length; ++i) {
                    rcOrAnd[i] = (String[])(rcOrAnd[i].clone());
                }
            }
            this.requiredContents = rcOrAnd;
	}

	/**
	 * Overrides parent handleCreate call.  Parent will filter out keys that it
	 * knows are not accepted by this factory before calling this method.
	 */
	protected Object handleCreate(Key key) {
	    Locale loc = LocaleUtility.getLocaleFromName(key.currentID());
	    if (acceptsLocale(loc)) {
		ResourceBundle bundle = ICULocaleData.getResourceBundle(name, loc); // full resource bundle tree lookup
		return createFromBundle(bundle, key);
	    }
	    return null;
	}

	/**
	 * Queries all the available locales in ICU and adds the names
	 * of those which it accepts to result.  This is quite
	 * time-consuming so we don't want to do it more than once if
	 * we have to.  This is only called if we are visible.  
	 */
	protected Set handleGetSupportedIDs() {
            Set result = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	    Locale[] locales = ICULocaleData.getAvailableLocales(name);
	    for (int i = 0; i < locales.length; ++i) {
		Locale locale = locales[i];
		if (acceptsLocale(locale)) {
                    String str = LocaleUtility.canonicalLocaleString(locale.toString());
		    result.add(str);
		}
	    }
            return result;
	}

	/**
	 * Return a localized name for the locale represented by id.
	 */
	protected String handleGetDisplayName(String id, Locale locale) {
	    return LocaleUtility.getLocaleFromName(id).getDisplayName(locale);
	}

	/**
	 * We only accept the locale if there is a bundle for this exact locale and if
	 * all the required resources are directly in this bundle (none is from an
	 * inherited bundle);
	 */
	protected boolean acceptsLocale(Locale loc) {
            boolean debug = false;
            if (debug) System.out.println("al name: " + name + " loc: '" + loc + "'");
            try {
                ResourceBundle bundle = ICULocaleData.loadResourceBundle(name, loc);
                if (bundle == null) {
                    if (debug) System.out.println("no bundle");
                    return false;
                }
                if (requiredContents == null) {
                    if (debug) System.out.println("always accepts");
                    return true;
                }

            loop: 
                for (int i = 0; i < requiredContents.length; ++i) {
                    String[] andRC = requiredContents[i];

                    for (int j = 0; j < andRC.length; ++j) {
                        try {
                            if (debug) System.out.println("al["+i+"]["+j+"] " + andRC[j]);
                            bundle.getObject(andRC[j]);
                        }
                        catch (MissingResourceException ex) {
                            if (debug) System.out.println("nope");
                            continue loop;
                        }
                    }
                    if (debug) System.out.println("ok");
                    return true;
                }
	    }
	    catch (Exception e) {
                Thread.dumpStack();
                if (debug) System.out.println("whoops: " + e);
                System.exit(0);
	    }
	    return false;
	}

	/**
	 * Subclassers implement this to create their service object based on the bundle and key.
	 * The default implementation just returns the bundle.
	 */
         protected Object createFromBundle(ResourceBundle bundle, Key key) {
	     return bundle;
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

    protected Key createKey(String id) {
	return LocaleKey.createWithCanonicalFallback(id, validateFallbackLocale());
    }
}
