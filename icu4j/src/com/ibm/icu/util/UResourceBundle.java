/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICUResourceBundleReader;
import com.ibm.icu.util.ULocale;

/**
 * A class representing a collection of resource information pertaining to a given
 * locale. A resource bundle provides a way of accessing locale- specfic information in
 * a data file. You create a resource bundle that manages the resources for a given
 * locale and then ask it for individual resources.
 * <P>
 * This API is analogous to JDK {@link #ResourceBundle java.util ResourceBundle}, 
 * but the semantics are considerably different. In ResourceBundle class, an object is created 
 * and the sub items are fetched using getString, getObject methods. 
 * In UResource,each individual element of a resource is a resource by itself.
 * 
 * <P>
 * Resource bundles in ICU are currently defined using text files which conform to the following
 * <a href="http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/bnf_rb.txt">BNF definition</a>.
 * More on resource bundle concepts and syntax can be found in the 
 * <a href="http://oss.software.ibm.com/icu/userguide/ResourceManagement.html">Users Guide</a>.
 * <P>
 *
 * The UResource class is not suitable for subclassing.
 *
 * @draft ICU 3.0
 * @author ram
 */
public abstract class UResourceBundle extends ResourceBundle{
    
	protected static final String ICU_DATA_PATH = "com/ibm/icu/impl/data";

	private static final String ICU_BUNDLE = "icudt"+VersionInfo.ICU_DATA_VERSION;
    
    
    /**
     * @draft ICU 3.0
     */
    public static final String ICU_BASE_NAME= ICU_DATA_PATH+"/"+ICU_BUNDLE;
    
    /**
     * @draft ICU 3.0
     */
    public static final String ICU_COLLATION_BASE_NAME = ICU_BASE_NAME + "/" + "coll";
    
    /**
     * @draft ICU 3.0
     */
    public static final ClassLoader ICU_DATA_CLASS_LOADER = ICUData.class.getClassLoader();

    public static final String INSTALLED_LOCALES = "InstalledLocales";
    

    protected boolean hasFallback;
    
    /**
     * Gets a resource bundle using the specified base name and locale.
     * ICU_DATA_CLASS is used as the default root.
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param locale the locale for which a resource bundle is desired
     * @exception MissingResourceException
     *     if no resource bundle for the specified base name can be found
     * @return a resource bundle for the given base name and locale
     * @draft ICU 3.0
     */
    public static final UResourceBundle getBundleInstance(String baseName, String localeName){
        return getBundleInstance(baseName, localeName, ICU_DATA_CLASS_LOADER, false);
    }
    
    /**
     * Gets a resource bundle using the specified base name, locale, and class root.
     *
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param locale the locale for which a resource bundle is desired
     * @param root the class object from which to load the resource bundle
     * @exception MissingResourceException
     *     if no resource bundle for the specified base name can be found
     * @return a resource bundle for the given base name and locale
     * @draft ICU 3.0
     */
    public static final UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root){
        return getBundleInstance(baseName, localeName, root, false);
    }
    /**
     * Gets a resource bundle using the specified base name, locale, and class root.
     *
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param locale the locale for which a resource bundle is desired
     * @param root the class object from which to load the resource bundle
     * @param disableFallback Option to disable locale inheritence. 
     *                          If true the fallback chain will not be built.
     * @exception MissingResourceException
     *     if no resource bundle for the specified base name can be found
     * @return a resource bundle for the given base name and locale
     * @draft ICU 3.0
     * 
     */    
    protected static final UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root, boolean disableFallback){
        return instantiateBundle(baseName, localeName, root, disableFallback);   
    }
    

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)String baseName, String localeName, ClassLoader root
     * @draft ICU 3.0
     */
    protected UResourceBundle() {   }
    
    
    /**
     * Creates a UResourceBundle for the locale specified, from which users can extract strings by using
     * their corresponding keys.
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     *                
     * @draft ICU 3.0
     */
    public static final UResourceBundle getBundleInstance(ULocale locale){
        if(locale==null){
            locale = ULocale.getDefault();   
        }
        return getBundleInstance( ICU_BASE_NAME, locale.toString(), ICU_DATA_CLASS_LOADER );   
    }
    /**
     * Creates a UResourceBundle for the default locale and specified base name, from which users can extract strings by using
     * their corresponding keys.
     * @param baseName  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     *                
     * @draft ICU 3.0 
     */    
    public static final ResourceBundle getBundleInstance(String baseName){
        return getBundleInstance( baseName, ULocale.getDefault().toString(), ICU_DATA_CLASS_LOADER );
    }
    public static final ResourceBundle getBundleInstance(String baseName, Locale locale){
        return getBundleInstance(baseName, new ULocale(locale));
    }
   
    /**
     * Creates a UResourceBundle, from which users can extract strings by using
     * their corresponding keys.
     * @param baseName string containing the name of the data package.
     *                    If null the default ICU package name is used.
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     *                
     * @draft ICU 3.0
     */
    public static final UResourceBundle getBundleInstance(String baseName, ULocale locale){
         return getBundleInstance(baseName, locale.toString(),ICU_DATA_CLASS_LOADER);  
    }
    

    /**
     * Returns the locale of this resource bundle. This method can be used after a
     * call to getBundleInstance() to determine whether the resource bundle returned really
     * corresponds to the requested locale or is a fallback.
     *
     * @return the locale of this resource bundle
     */
    public ULocale getULocale() {
        return new ULocale(getLocale());
    }    

    protected void setParent(UResourceBundle parent){
        this.parent = parent;   
    }
    // Cache for ResourceBundle instantiation
    private static SoftReference BUNDLE_CACHE;

    private static void addToCache(ResourceCacheKey key, UResourceBundle b) {
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
    
    private static UResourceBundle loadFromCache(ResourceCacheKey key) {
        if (BUNDLE_CACHE != null) {
            Map m = (Map)BUNDLE_CACHE.get();
            if (m != null) {
                return (UResourceBundle)m.get(key);
            }
        }
        return null;
    }   
    


    /**
	 * Key used for cached resource bundles.  The key checks
	 * the resource name, the class root, and the default
	 * locale to determine if the resource is a match to the
	 * requested one. The root may be null, but the
	 * searchName and the default locale must have a non-null value.
	 * Note that the default locale may change over time, and
	 * lookup should always be based on the current default
	 * locale (if at all).
	 */
	private static final class ResourceCacheKey implements Cloneable {
		private SoftReference loaderRef;
		private String searchName;
		private ULocale defaultLocale;
		private int hashCodeCache;
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			try {
				final ResourceCacheKey otherEntry = (ResourceCacheKey) other;
				//quick check to see if they are not equal
				if (hashCodeCache != otherEntry.hashCodeCache) {
					return false;
				}
				//are the names the same?
				if (!searchName.equals(otherEntry.searchName)) {
					return false;
				}
				// are the default locales the same?
				if (defaultLocale == null) {
					if (otherEntry.defaultLocale != null) {
						return false;
					}
				} else {
					if (!defaultLocale.equals(otherEntry.defaultLocale)) {
						return false;
					}
				}
				//are refs (both non-null) or (both null)?
				if (loaderRef == null) {
					return otherEntry.loaderRef == null;
				} else {
					return (otherEntry.loaderRef != null)
							&& (loaderRef.get() == otherEntry.loaderRef.get());
				}
			} catch (NullPointerException e) {
				return false;
			} catch (ClassCastException e) {
				return false;
			}
		}
		public int hashCode() {
			return hashCodeCache;
		}
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				//this should never happen
				throw new InternalError();
			}
		}
		public void setKeyValues(ClassLoader root, String searchName, ULocale defaultLocale) {
			this.searchName = searchName;
			hashCodeCache = searchName.hashCode();
			this.defaultLocale = defaultLocale;
			if (defaultLocale != null) {
				hashCodeCache ^= defaultLocale.hashCode();
			}
			if (root == null) {
				this.loaderRef = null;
			} else {
				loaderRef = new SoftReference(root);
				hashCodeCache ^= root.hashCode();
			}
		}
		public void clear() {
			setKeyValues(null, "", null);
		}
	}
    
    private static final ResourceCacheKey cacheKey = new ResourceCacheKey();
    
    protected static synchronized UResourceBundle instantiateBundle(String baseName, String localeName, ClassLoader root, boolean disableFallback){
        if(disableFallback){
            ULocale defaultLocale = ULocale.getDefault();
            String fullName = ICUResourceBundleReader.getFullName(baseName, localeName);
            cacheKey.setKeyValues(root, fullName, defaultLocale);
            UResourceBundle b = loadFromCache(cacheKey);
            if(b==null){
                b =  ICUResourceBundle.createBundle(baseName, localeName, root);
                cacheKey.setKeyValues(root, fullName, defaultLocale);
                addToCache(cacheKey, b);
            }
            b.hasFallback = disableFallback;
            return b;
        }else{
            return instantiateICUResource(baseName,localeName,root);
        }
    }
    //  recursively build bundle
    protected static UResourceBundle instantiateICUResource(String baseName,String localeName, ClassLoader root){
        ULocale defaultLocale = ULocale.getDefault();
        String fullName = ICUResourceBundleReader.getFullName(baseName, ULocale.getBaseName(localeName));
        cacheKey.setKeyValues(root, fullName, defaultLocale);
        UResourceBundle b = loadFromCache(cacheKey);
        if (b == null) {
            UResourceBundle parent = null;
            int i = localeName.lastIndexOf('_');
            final String rootLocale = "root";
 
            if (i != -1) {
                parent = instantiateICUResource(baseName, localeName.substring(0, i), root);
            }else{
                parent = ICUResourceBundle.createBundle(baseName, rootLocale, root);   
            }
            try {
                UResourceBundle b1 = ICUResourceBundle.createBundle(baseName, localeName, root);

                if (parent != null) {
                    b1.setParent(parent);
                }
                
                b = b1;
                
                cacheKey.setKeyValues(root, fullName, defaultLocale);
                addToCache(cacheKey, b);
            }catch (MissingResourceException e) {

                // if a bogus locale is passed then the parent should be
                // the default locale not the root locale!
                if(localeName.indexOf('_')==-1){    
                    String defaultName = defaultLocale.toString();
                        
                    if(!localeName.equals(rootLocale.toString()) &&
                       defaultName.indexOf(localeName)==-1){
                       parent = instantiateICUResource(baseName, defaultName, root);
                    } 
                }
                b = parent;
            }
        }
        return b;
    }


}
