/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/ICULocaleService.java,v $
 * $Date: 2002/09/07 00:15:33 $
 * $Revision: 1.6 $
 *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

public class ICULocaleService extends ICUService {
    Locale fallbackLocale;
    String fallbackLocaleName;

    /**
     * Construct an ICULocaleService with a fallback locale string based on the current
     * default locale at the time of construction.
     */
    public ICULocaleService() {
	fallbackLocale = Locale.getDefault();
	fallbackLocaleName = LocaleUtility.canonicalLocaleString(fallbackLocale.toString());
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
    public Object get(Locale locale, Locale[] actualLocaleReturn) {
        if (actualLocaleReturn == null) {
            return get(locale.toString());
        }
        String[] temp = new String[1];
        Object result = get(locale.toString(), temp);
        actualLocaleReturn[0] = LocaleUtility.getLocaleFromName(temp[0]);
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
     * Convenience method for callers using locales.  This is the typical
     * current API for this operation.
     */
    public Locale[] getAvailableLocales() {
        TreeSet sort = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        sort.addAll(getVisibleIDs());
        Iterator iter = sort.iterator();
        Locale[] locales = new Locale[sort.size()];
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
     * client, and the fallback locale to search for is the current default
     * locale.  This is instantiated by ICULocaleService.</p>
     *
     * <p>Canonicalization adjusts the locale string so that the
     * section before the first understore is in lower case, and the rest
     * is in upper case, with no trailing underscores.</p>
     */
    public static class LocaleKey extends ICUService.Key {
	private String primaryID;
	private String fallbackID;
	private String currentID;

	/**
	 * Convenience method for createWithCanonical that canonicalizes both the
	 * primary and fallback IDs first.
	 */
	public static LocaleKey create(String primaryID, String fallbackID) {
	    String canonicalPrimaryID = LocaleUtility.canonicalLocaleString(primaryID);
	    String canonicalFallbackID = LocaleUtility.canonicalLocaleString(fallbackID);
	    return new LocaleKey(primaryID, canonicalPrimaryID, canonicalFallbackID);
	}

	/**
	 * Convenience method for createWithCanonical that canonicalizes the
	 * primary ID first, the fallback is assumed to already be canonical.
	 */
	public static LocaleKey createWithCanonicalFallback(String primaryID, String canonicalFallbackID) {
	    String canonicalPrimaryID = LocaleUtility.canonicalLocaleString(primaryID);
	    return new LocaleKey(primaryID, canonicalPrimaryID, canonicalFallbackID);
	}

	/**
	 * Create a LocaleKey with canonical primary and fallback IDs.
	 */
	public static LocaleKey createWithCanonical(String canonicalPrimaryID, String canonicalFallbackID) {
	    return new LocaleKey(canonicalPrimaryID, canonicalPrimaryID, canonicalFallbackID);
	}
	    
	/**
	 * PrimaryID is the user's requested locale string,
	 * canonicalPrimaryID is this string in canonical form,
	 * fallbackID is the current default locale's string in
	 * canonical form.
	 */
        protected LocaleKey(String primaryID, String canonicalPrimaryID, String canonicalFallbackID) {
	    super(primaryID);
	    
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
	 * Return the (canonical) original ID.
	 */
	public String canonicalID() {
	    return primaryID;
	}

        /**
         * Return the (canonical) current ID.
         */
        public String currentID() {
	    return currentID;
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
	    String current = currentID();
	    int x = current.lastIndexOf('_');
	    if (x != -1) {
		currentID = current.substring(0, x);
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
	private boolean included;

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
	 * we do know we understand, if included is true.  Note that if
	 * the cache has been freed by gc, we reset the included flag, so
	 * it must not be tested before this method is called.
	 */
	private HashSet getCache() {
	    HashSet cache = null;
	    if (cacheref != null) {
		cache = (HashSet)cacheref.get();
	    }
	    if (cache == null) {
		cache = new HashSet();
		cacheref = new SoftReference(cache);
		included = false;
	    }
	    return cache;
	}

	/**
	 * Get the cache of IDs we understand.
	 */
	private HashSet getIncludedCache() {
	    HashSet cache = getCache();
	    if (!included) {
		cache.clear();
		handleUpdateVisibleIDs(cache);
		included = true;
	    }
	    return cache;
	}

	public final Object create(Key key) {
	    Object result = null;
	    String id = key.currentID();
	    HashSet cache = getCache();
	    if (cache.contains(id) == included) {
		result = handleCreate(key);
		if (!included && result == null) {
		    cache.add(id);
		}
	    }
	    return result;
	}

	public final void updateVisibleIDs(Map result) {
	    if (visible) {
		Set cache = getIncludedCache();
		Iterator iter = cache.iterator();
		while (iter.hasNext()) {
		    result.put((String)iter.next(), this);
		}
	    }
	}

	public final String getDisplayName(String id, Locale locale) {
	    if (visible) {
		Set cache = getIncludedCache();
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
	 * Subclasses implement this instead of updateVisibleIDs.  Any
	 * id known to and handled by this class should be added to
	 * result.  
	 */
	protected abstract void handleUpdateVisibleIDs(Set result);

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
	protected final String[] requiredContents;

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
	    super(visible);

	    this.name = name;
	    if (requiredContents != null) {
		ArrayList list = new ArrayList();
		for (int i = 0, len = requiredContents.length();;) {
		    while (i < len && requiredContents.charAt(i) == ';') {
			++i;
		    }
		    if (i == len) {
		      break;
		    }
		    int j = requiredContents.indexOf(';', i);
		    if (j == -1) {
		      j = len;
		    }
		    list.add(requiredContents.substring(i, j));
		    i = j;
		}
		this.requiredContents = (String[])list.toArray(new String[list.size()]);
	    } else {
		this.requiredContents = null;
	    }
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
	protected void handleUpdateVisibleIDs(Set result) {
	    Locale[] locales = ICULocaleData.getAvailableLocales(name);
	    for (int i = 0; i < locales.length; ++i) {
		Locale locale = locales[i];
		if (acceptsLocale(locale)) {
		    result.add(LocaleUtility.canonicalLocaleString(locale.toString()));
		}
	    }
	}

	/**
	 * Return a localized name for the locale represented by id.
	 */
	protected String handleGetDisplayName(String id, Locale locale) {
	    // use java's display name formatting for now
	    return LocaleUtility.getLocaleFromName(id).getDisplayName(locale);
	}

	/**
	 * We only accept the locale if there is a bundle for this exact locale and if
	 * all the required resources are directly in this bundle (none is from an
	 * inherited bundle);
	 */
	protected boolean acceptsLocale(Locale loc) {
	    try {
		ResourceBundle bundle = ICULocaleData.loadResourceBundle(name, loc); // single resource bundle lookup
		if (requiredContents != null) {
		    for (int i = 0; i < requiredContents.length; ++i) {
			if (bundle.getObject(requiredContents[i]) == null) {
			    return false;
			}
		    }
		}
		return true;
	    }
	    catch (Exception e) {
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

    protected Key createKey(String id) {
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
	    
	return LocaleKey.createWithCanonicalFallback(id, fallbackLocaleName);
    }
}
