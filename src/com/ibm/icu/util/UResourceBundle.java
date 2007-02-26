/*
 *******************************************************************************
 * Copyright (C) 2004-2007, International Business Machines Corporation and    *
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

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICUResourceBundleReader;
import com.ibm.icu.impl.ResourceBundleWrapper;
import com.ibm.icu.util.ULocale;

/**
 * A class representing a collection of resource information pertaining to a given
 * locale. A resource bundle provides a way of accessing locale- specfic information in
 * a data file. You create a resource bundle that manages the resources for a given
 * locale and then ask it for individual resources.
 * <P>
 * In ResourceBundle class, an object is created 
 * and the sub items are fetched using getString, getObject methods. 
 * In UResourceBundle,each individual element of a resource is a resource by itself.
 * 
 * <P>
 * Resource bundles in ICU are currently defined using text files which conform to the following
 * <a href="http://dev.icu-project.org/cgi-bin/viewcvs.cgi/icuhtml/design/bnf_rb.txt">BNF definition</a>.
 * More on resource bundle concepts and syntax can be found in the 
 * <a href="http://icu.sourceforge.net/userguide/ResourceManagement.html">Users Guide</a>.
 * <P>
 * 
 * The packaging of ICU *.res files can be of two types
 * ICU4C:
 * <pre>
 *       root.res
 *         |
 *      --------
 *     |        |
 *   fr.res  en.res
 *     |
 *   --------
 *  |        |
 * fr_CA.res fr_FR.res     
 * </pre>
 * JAVA/JDK:
 * <pre>
 *    LocaleElements.res
 *         |
 *      -------------------
 *     |                   |
 * LocaleElements_fr.res  LocaleElements_en.res
 *     |
 *   ---------------------------
 *  |                            |
 * LocaleElements_fr_CA.res   LocaleElements_fr_FR.res
 * </pre>
 * Depending on the organization of your resources, the syntax to getBundleInstance will change.
 * To open ICU style organization use:
 * <pre>
 *      UResourceBundle bundle = UResourceBundle.getBundleInstance("com/ibm/icu/impl/data/icudt30b", "en_US");
 * </pre>
 * To open Java/JDK style organization use:
 * <pre>
 *      UResourceBundle bundle = UResourceBundle.getBundleInstance("com.ibm.icu.impl.data.LocaleElements", "en_US");
 * </pre>
 * @stable ICU 3.0
 * @author ram
 */
public abstract class UResourceBundle extends ResourceBundle{

    
    /**
     * Creates a resource bundle using the specified base name and locale.
     * ICU_DATA_CLASS is used as the default root.
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param localeName the locale for which a resource bundle is desired
     * @exception MissingResourceException
     *     if no resource bundle for the specified base name can be found
     * @return a resource bundle for the given base name and locale
     * @stable ICU 3.0
     */
    public static UResourceBundle getBundleInstance(String baseName, String localeName){
        return getBundleInstance(baseName, localeName, ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
    }
    
    /**
     * Creates a resource bundle using the specified base name, locale, and class root.
     *
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param localeName the locale for which a resource bundle is desired
     * @param root the class object from which to load the resource bundle
     * @exception MissingResourceException
     *     if no resource bundle for the specified base name can be found
     * @return a resource bundle for the given base name and locale
     * @stable ICU 3.0
     */
    public static UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root){
        return getBundleInstance(baseName, localeName, root, false);
    }
    
    /**
     * Creates a resource bundle using the specified base name, locale, and class root.
     *
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param localeName the locale for which a resource bundle is desired
     * @param root the class object from which to load the resource bundle
     * @param disableFallback Option to disable locale inheritence. 
     *                          If true the fallback chain will not be built.
     * @exception MissingResourceException
     *     if no resource bundle for the specified base name can be found
     * @return a resource bundle for the given base name and locale
     * @stable ICU 3.0
     * 
     */    
    protected static UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root, boolean disableFallback){
        return instantiateBundle(baseName, localeName, root, disableFallback);   
    }

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)  This is public for compatibility with Java, whose compiler
     * will generate public default constructors for an abstract class.
     * @stable ICU 3.0
     */
    public UResourceBundle() {   
    }
    
    /**
     * Creates a UResourceBundle for the locale specified, from which users can extract resources by using
     * their corresponding keys.
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     * @return a resource bundle for the given locale               
     * @stable ICU 3.0
     */
    public static UResourceBundle getBundleInstance(ULocale locale){
        if(locale==null){
            locale = ULocale.getDefault();   
        }
        return getBundleInstance( ICUResourceBundle.ICU_BASE_NAME, locale.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER );   
    }
    /**
     * Creates a UResourceBundle for the default locale and specified base name,
     * from which users can extract resources by using their corresponding keys.
     * @param baseName  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     * @return a resource bundle for the given base name and default locale              
     * @stable ICU 3.0 
     */    
    public static UResourceBundle getBundleInstance(String baseName){
        return getBundleInstance( baseName, ULocale.getDefault().toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER );
    }
    /**
     * Creates a UResourceBundle for the specified locale and specified base name,
     * from which users can extract resources by using their corresponding keys.
     * @param baseName  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.  
     * @return a resource bundle for the given base name and locale
     * @stable ICU 3.0
     */

    public static UResourceBundle getBundleInstance(String baseName, Locale locale){
        return getBundleInstance(baseName, ULocale.forLocale(locale));
    }
   
    /**
     * Creates a UResourceBundle, from which users can extract resources by using
     * their corresponding keys.
     * @param baseName string containing the name of the data package.
     *                    If null the default ICU package name is used.
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     * @return a resource bundle for the given base name and locale               
     * @stable ICU 3.0
     */
    public static UResourceBundle getBundleInstance(String baseName, ULocale locale){
         return getBundleInstance(baseName, locale.toString(),ICUResourceBundle.ICU_DATA_CLASS_LOADER);  
    }
    
    /**
     * Creates a UResourceBundle for the specified locale and specified base name,
     * from which users can extract resources by using their corresponding keys.
     * @param baseName  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.  
     * @param loader  the loader to use
     * @return a resource bundle for the given base name and locale
     * @internal revisit for ICU 3.6
     * @deprecated This API is ICU internal only.
     */
    public static UResourceBundle getBundleInstance(String baseName, Locale locale, ClassLoader loader){
        return getBundleInstance(baseName, ULocale.forLocale(locale), loader);
    }
   
    /**
     * Creates a UResourceBundle, from which users can extract resources by using
     * their corresponding keys.
     * @param baseName string containing the name of the data package.
     *                    If null the default ICU package name is used.
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.
     * @param loader  the loader to use
     * @return a resource bundle for the given base name and locale               
     * @internal revisit for ICU 3.6
     * @deprecated This API is ICU internal only.
     */
    public static UResourceBundle getBundleInstance(String baseName, ULocale locale, ClassLoader loader){
         return getBundleInstance(baseName, locale.toString(),loader);  
    }
    
    /**
     * Returns the RFC 3066 conformant locale id of this resource bundle. 
     * This method can be used after a call to getBundleInstance() to
     * determine whether the resource bundle returned really
     * corresponds to the requested locale or is a fallback.
     *
     * @return the locale of this resource bundle
     * @stable ICU 3.0
     */
    public abstract ULocale getULocale(); 
    
    /**
     * Gets the localeID
     * @return The string representation of the localeID
     * @stable ICU 3.0
     */
    protected abstract String getLocaleID();
    /**
     * Gets the base name of the resource bundle
     * @return The string representation of the base name
     * @stable ICU 3.0
     */
    protected abstract String getBaseName();
    /**
     * Gets the parent bundle
     * @return The parent bundle
     * @stable ICU 3.0
     */
    protected abstract UResourceBundle getParent();
    
    
    /**
     * Get the locale of this bundle
     * @return the locale of this resource bundle
     * @stable ICU 3.0
     */
    public Locale getLocale(){
        return getULocale().toLocale();   
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

    /**
     * @internal revisit for ICU 3.6
     * @deprecated This API is ICU internal only.
     */
    protected static void addToCache(ClassLoader cl, String fullName, ULocale defaultLocale,  UResourceBundle b){
        synchronized(cacheKey){
            cacheKey.setKeyValues(cl, fullName, defaultLocale);
            addToCache((ResourceCacheKey)cacheKey.clone(), b);
        }
    }
    /**
     * @internal revisit for ICU 3.6
     * @deprecated This API is ICU internal only.
     */
    protected static UResourceBundle loadFromCache(ClassLoader cl, String fullName, ULocale defaultLocale){
        synchronized(cacheKey){
            cacheKey.setKeyValues(cl, fullName, defaultLocale);
            return loadFromCache(cacheKey);
        }
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
        ///CLOVER:OFF
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
                throw new IllegalStateException();
            }
        }
        ///CLOVER:ON
        private synchronized void setKeyValues(ClassLoader root, String searchName, ULocale defaultLocale) {
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
        /*private void clear() {
            setKeyValues(null, "", null);
        }*/
    }
    
    private static final ResourceCacheKey cacheKey = new ResourceCacheKey();
                             
    private static final int ROOT_MISSING = 0;
    private static final int ROOT_ICU = 1;
    private static final int ROOT_JAVA = 2;
    
    private static SoftReference ROOT_CACHE;

    private static int getRootType(String baseName, ClassLoader root)
    {
        Map m = null;
        Integer rootType;
        
        if (ROOT_CACHE != null) {
            m = (Map) ROOT_CACHE.get();
        }
        
        if (m == null) {
            m = new HashMap();
            ROOT_CACHE = new SoftReference(m);
        }

        rootType = (Integer) m.get(baseName);
        
        if (rootType == null) {
            String rootLocale = (baseName.indexOf('.')==-1) ? "root" : "";
            int rt = ROOT_MISSING; // value set on success
            try{
                ICUResourceBundle.getBundleInstance(baseName, rootLocale, root, true);
                rt = ROOT_ICU; 
            }catch(MissingResourceException ex){
                try{
                    ResourceBundleWrapper.getBundleInstance(baseName, rootLocale, root, true);
                    rt = ROOT_JAVA;
                }catch(MissingResourceException e){
                    //throw away the exception
                }
            }
            
            rootType = new Integer(rt);
            m.put(baseName, rootType);
        }
        
        return rootType.intValue();
    }
    
    private static void setRootType(String baseName, int rootType)
    {
        Integer rt = new Integer(rootType);
        Map m = null;
        
        if (ROOT_CACHE != null) {
            m = (Map) ROOT_CACHE.get();
        } else {
            m = new HashMap();
            ROOT_CACHE = new SoftReference(m);
        }
        
        m.put(baseName, rt);
    }
    
    /**
     * Loads a new resource bundle for the give base name, locale and class loader.
     * Optionally will disable loading of fallback bundles.
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param localeName the locale for which a resource bundle is desired
     * @param root the class object from which to load the resource bundle
     * @param disableFallback disables loading of fallback lookup chain
     * @exception MissingResourceException
     *     if no resource bundle for the specified base name can be found
     * @return a resource bundle for the given base name and locale
     * @stable ICU 3.0
     */
    protected static UResourceBundle instantiateBundle(String baseName, String localeName, 
                                                       ClassLoader root, boolean disableFallback){
        UResourceBundle b = null;
        int rootType = getRootType(baseName, root);

        ULocale defaultLocale = ULocale.getDefault();
        
        switch (rootType)
        {
        case ROOT_ICU:
            if(disableFallback) {
                String fullName = ICUResourceBundleReader.getFullName(baseName, localeName);        
                synchronized(cacheKey){
                    cacheKey.setKeyValues(root, fullName, defaultLocale);
                    b = loadFromCache(cacheKey);     
                }
                
                if (b == null) {
                    b = ICUResourceBundle.getBundleInstance(baseName, localeName, root, disableFallback);
                    //cacheKey.setKeyValues(root, fullName, defaultLocale);
                    addToCache(cacheKey, b);
                }
            } else {
                b = ICUResourceBundle.getBundleInstance(baseName, localeName, root, disableFallback);
            }
            
            return b;
            
        case ROOT_JAVA:
            return ResourceBundleWrapper.getBundleInstance(baseName, localeName, root, disableFallback);
            
        default:
            try{
                b = ICUResourceBundle.getBundleInstance(baseName, localeName, root, disableFallback);
                setRootType(baseName, ROOT_ICU);
            }catch(MissingResourceException ex){
                b = ResourceBundleWrapper.getBundleInstance(baseName, localeName, root, disableFallback);
                setRootType(baseName, ROOT_JAVA);
            }
            return b;
        }
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected abstract void setLoadingStatus(int newStatus);
}
