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
 * <a href="http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/bnf_rb.txt">BNF definition</a>.
 * More on resource bundle concepts and syntax can be found in the 
 * <a href="http://oss.software.ibm.com/icu/userguide/ResourceManagement.html">Users Guide</a>.
 * <P>
 * 
 * The packaging of ICU *.res files can be of two types
 * ICU4C:
 * <code>
 *       root.res
 *         |
 *      --------
 *     |        |
 *   fr.res  en.res
 *     |
 *   --------
 *  |        |
 * fr_CA.res fr_FR.res     
 * </code>
 * JAVA/JDK:
 * <code>
 *    LocaleElements.res
 *         |
 *      -------------------
 *     |                   |
 * LocaleElements_fr.res  LocaleElements_en.res
 *     |
 *   ---------------------------
 *  |                            |
 * LocaleElements_fr_CA.res   LocaleElements_fr_FR.res
 * </code>
 * Depending on the organization of your resources, the syntax to getBundleInstance will change.
 * To open ICU style organization use:
 * <code>
 *      UResourceBundle bundle = UResourceBundle.getBundleInstance("com/ibm/icu/impl/data/icudt30b", "en_US");
 * </code>
 * To open Java/JDK style organization use:
 * <code>
 *      UResourceBundle bundle = UResourceBundle.getBundleInstance("com.ibm.icu.impl.data.LocaleElements", "en_US");
 * </code>
 * @draft ICU 3.0
 * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final UResourceBundle getBundleInstance(String baseName, String localeName){
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root){
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     * 
     */    
    protected static UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root, boolean disableFallback){
        return instantiateBundle(baseName, localeName, root, disableFallback);   
    }
    

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    protected UResourceBundle() {   }
    
    
    /**
     * Creates a UResourceBundle for the locale specified, from which users can extract resources by using
     * their corresponding keys.
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     * @return a resource bundle for the given locale               
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final UResourceBundle getBundleInstance(ULocale locale){
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
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */    
    public static final UResourceBundle getBundleInstance(String baseName){
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */

    public static final UResourceBundle getBundleInstance(String baseName, Locale locale){
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final UResourceBundle getBundleInstance(String baseName, ULocale locale){
         return getBundleInstance(baseName, locale.toString(),ICUResourceBundle.ICU_DATA_CLASS_LOADER);  
    }
    

    /**
     * Returns the RFC 3066 conformant locale id of this resource bundle. 
     * This method can be used after a call to getBundleInstance() to
     * determine whether the resource bundle returned really
     * corresponds to the requested locale or is a fallback.
     *
     * @return the locale of this resource bundle
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public abstract ULocale getULocale(); 
    
    /**
     * Gets the localeID
     * @return The string representation of the localeID
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    protected abstract String getLocaleID();
    /**
     * Gets the base name of the resource bundle
     * @return The string representation of the base name
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    protected abstract String getBaseName();
    /**
     * Gets the parent bundle
     * @return The parent bundle
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    protected abstract UResourceBundle getParent();
    
    
    /**
     * Get the locale of this bundle
     * @return the locale of this resource bundle
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    protected static synchronized UResourceBundle instantiateBundle(String baseName, String localeName, ClassLoader root, boolean disableFallback){
        // first try to create an ICUResourceBundle
        // the expectation is that most client using 
        // this interface will open an *.res file
        UResourceBundle b = null;
        if(disableFallback){
            ULocale defaultLocale = ULocale.getDefault();
            String fullName = ICUResourceBundleReader.getFullName(baseName, localeName);
            cacheKey.setKeyValues(root, fullName, defaultLocale);
            b = loadFromCache(cacheKey);
            if(b==null){
                b =  ICUResourceBundle.createBundle(baseName, localeName, root);
                cacheKey.setKeyValues(root, fullName, defaultLocale);
                addToCache(cacheKey, b);
            }
        }else{
            b = instantiateICUResource(baseName,localeName,root);
        }
        if(b==null){
           // we can't find an *.res file .. so fallback to
           // Java ResourceBundle loadeing 
           b = new ResourceBundleWrapper(baseName, localeName, root);
        }
        if(b==null){
            throw new MissingResourceException("Could not find the bundle ", baseName,localeName );   
        }
        return b;
    }
    
    /**
     * @internal
     */
    protected abstract void setLoadingStatus(int newStatus);
    
    /**
     * Creates a new ICUResourceBundle for the given locale, baseName and class loader
     * @param baseName the base name of the resource bundle, a fully qualified class name
     * @param localeID the locale for which a resource bundle is desired
     * @param root the class object from which to load the resource bundle
     * @exception MissingResourceException
     *     if no resource bundle for the specified base name can be found
     * @return a resource bundle for the given base name and locale
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    //  recursively build bundle
    protected static UResourceBundle instantiateICUResource(String baseName,String localeID, ClassLoader root){
        ULocale defaultLocale = ULocale.getDefault();
        String localeName = ULocale.getBaseName(localeID);
        String fullName = ICUResourceBundleReader.getFullName(baseName, localeName);
        cacheKey.setKeyValues(root, fullName, defaultLocale);
        UResourceBundle b = loadFromCache(cacheKey);
        // here we assume that java type resource bundle organization
        // is required then the base name contains '.' else 
        // the resource organization is of ICU type
        // so clients can instantiate resources of the type
        // com.mycompany.data.MyLocaleElements_en.res and 
        // com.mycompany.data.MyLocaleElements.res
        //
        final String rootLocale = (baseName.indexOf('.')==-1) ? "root" : "";
        final String defaultID = ULocale.getDefault().toString();
        if(localeName.equals("")){
            localeName = rootLocale;   
        }
        if (b == null) {
            b = ICUResourceBundle.createBundle(baseName, localeName, root);
            if(b==null){
                int i = localeName.lastIndexOf('_');
                if (i != -1) {
                    b = instantiateICUResource(baseName, localeName.substring(0, i), root);
                    if(b!=null && b.getULocale().equals(localeName)){
                        b.setLoadingStatus(ICUResourceBundle.FROM_FALLBACK);
                    }
                }else{
                    if(defaultID.indexOf(localeName)==-1){
                        b = instantiateICUResource(baseName, defaultID, root);
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
                localeName = b.getLocaleID();
                int i = localeName.lastIndexOf('_');
                cacheKey.setKeyValues(root, fullName, defaultLocale);
                addToCache(cacheKey, b);
                UResourceBundle parent = null;
                if (i != -1) {
                    parent = instantiateICUResource(baseName, localeName.substring(0, i), root);
                }else{
                    parent = ICUResourceBundle.createBundle(baseName, rootLocale, root);   
                }
                if(!b.equals(parent)){
                    b.setParent(parent);
                }
            }      
        }
        return b;
    }
    /*
    protected static UResourceBundle instantiateResource(String baseName,String localeID, ClassLoader root, boolean required){
        // first try
        try{
        }catch(MissingResourceException e){  
            if(required){
                throw e;   
            }
        }
    }
   */
}
