//##header J2SE15
/*
 *******************************************************************************
 * Copyright (C) 2004-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.lang.ref.SoftReference;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICUResourceBundleReader;
import com.ibm.icu.impl.ResourceBundleWrapper;
import com.ibm.icu.util.ULocale;

//#if defined(FOUNDATION10) || defined(J2SE13) || defined(ECLIPSE_FRAGMENT)
//##import com.ibm.icu.impl.ByteBuffer;
//#else
import java.nio.ByteBuffer;
//#endif

/**
 * A class representing a collection of resource information pertaining to a given
 * locale. A resource bundle provides a way of accessing locale- specific information in
 * a data file. You create a resource bundle that manages the resources for a given
 * locale and then ask it for individual resources.
 * <P>
 * In ResourceBundle class, an object is created 
 * and the sub items are fetched using getString, getObject methods. 
 * In UResourceBundle,each individual element of a resource is a resource by itself.
 * 
 * <P>
 * Resource bundles in ICU are currently defined using text files which conform to the following
 * <a href="http://source.icu-project.org/repos/icu/icuhtml/trunk/design/bnf_rb.txt">BNF definition</a>.
 * More on resource bundle concepts and syntax can be found in the 
 * <a href="http://www.icu-project.org/userguide/ResourceManagement.html">Users Guide</a>.
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
 *      UResourceBundle bundle = UResourceBundle.getBundleInstance("com/mycompany/resources", "en_US", myClassLoader);
 * </pre>
 * To open Java/JDK style organization use:
 * <pre>
 *      UResourceBundle bundle = UResourceBundle.getBundleInstance("com.mycompany.resources.LocaleElements", "en_US", myClassLoader);
 * </pre>
 * <note>
 * Please use pass a class loader for loading non-ICU resources. Java security does not
 * allow loading of resources across jar files. You must provide your class loader
 * to load the resources
 * </note>
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
    protected static UResourceBundle getBundleInstance(String baseName, String localeName, ClassLoader root, boolean disableFallback) {
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
    public static UResourceBundle getBundleInstance(ULocale locale) {
        if (locale==null) {
            locale = ULocale.getDefault();   
        }
        return getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);   
    }

    /**
     * Creates a UResourceBundle for the default locale and specified base name,
     * from which users can extract resources by using their corresponding keys.
     * @param baseName  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.              
     * @return a resource bundle for the given base name and default locale              
     * @stable ICU 3.0 
     */    
    public static UResourceBundle getBundleInstance(String baseName) {
	if (baseName == null) {
	    baseName = ICUResourceBundle.ICU_BASE_NAME;
	}
	ULocale uloc = ULocale.getDefault();
        return getBundleInstance(baseName, uloc.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
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

    public static UResourceBundle getBundleInstance(String baseName, Locale locale) {
	if (baseName == null) {
	    baseName = ICUResourceBundle.ICU_BASE_NAME;
	}
	ULocale uloc = locale == null ? ULocale.getDefault() : ULocale.forLocale(locale);

        return getBundleInstance(baseName, uloc.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);
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
    public static UResourceBundle getBundleInstance(String baseName, ULocale locale) {
 	if (baseName == null) {
	    baseName = ICUResourceBundle.ICU_BASE_NAME;
	}
	if (locale == null) {
	    locale = ULocale.getDefault();
	}
        return getBundleInstance(baseName, locale.toString(), ICUResourceBundle.ICU_DATA_CLASS_LOADER, false);  
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
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static UResourceBundle getBundleInstance(String baseName, Locale locale, ClassLoader loader) {
 	if (baseName == null) {
	    baseName = ICUResourceBundle.ICU_BASE_NAME;
	}
	ULocale uloc = locale == null ? ULocale.getDefault() : ULocale.forLocale(locale);
        return getBundleInstance(baseName, uloc.toString(), loader, false);
    }
   
    /**
     * Creates a UResourceBundle, from which users can extract resources by using
     * their corresponding keys.<br><br>
     * Note: Please use this API for loading non-ICU resources. Java security does not
     * allow loading of resources across jar files. You must provide your class loader
     * to load the resources
     * @param baseName string containing the name of the data package.
     *                    If null the default ICU package name is used.
     * @param locale  specifies the locale for which we want to open the resource.
     *                If null the bundle for default locale is opened.
     * @param loader  the loader to use
     * @return a resource bundle for the given base name and locale               
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static UResourceBundle getBundleInstance(String baseName, ULocale locale, ClassLoader loader) {
 	if (baseName == null) {
	    baseName = ICUResourceBundle.ICU_BASE_NAME;
	}
	if (locale == null) {
	    locale = ULocale.getDefault();
	}
        return getBundleInstance(baseName, locale.toString(), loader, false);  
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
     * Method used by subclasses to add the a particular resource bundle object to the managed cache
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
     * Method used by sub classes to load a resource bundle object from the managed cache
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
     * Returns a binary data from a binary resource.
     *
     * @return a pointer to a chuck of unsigned bytes which live in a memory mapped/DLL file.
     * @see #getIntVector
     * @see #getInt
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public ByteBuffer getBinary() {
        throw new UResourceTypeMismatchException("");
    }
    
    /**
     * Returns a string from a string resource type
     *
     * @return a string
     * @see #getBinary()
     * @see #getIntVector
     * @see #getInt
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String getString() {
        throw new UResourceTypeMismatchException("");
    }

    /**
     * Returns a string array from a array resource type
     *
     * @return a string
     * @see #getString()
     * @see #getIntVector
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String[] getStringArray() {
        throw new UResourceTypeMismatchException("");
    }

    /**
     * Returns a binary data from a binary resource.
     *
     * @param ba  The byte array to write the bytes to. A null variable is OK.  
     * @return an array bytes containing the binary data from the resource.
     * @see #getIntVector
     * @see #getInt
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public byte[] getBinary(byte[] ba) {
        throw new UResourceTypeMismatchException("");
    }
    
    /**
     * Returns a 32 bit integer array from a resource.
     *
     * @return a pointer to a chunk of unsigned bytes which live in a memory mapped/DLL file.
     * @see #getBinary()
     * @see #getInt
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public int[] getIntVector() {
        throw new UResourceTypeMismatchException("");
    }

    /**
     * Returns a signed integer from a resource.
     *
     * @return an integer value
     * @see #getIntVector
     * @see #getBinary()
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public int getInt() {
        throw new UResourceTypeMismatchException("");
    }

    /**
     * Returns a unsigned integer from a resource.
     * This integer is originally 28 bit and the sign gets propagated.
     *
     * @return an integer value
     * @see #getIntVector
     * @see #getBinary()
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public int getUInt() {
        throw new UResourceTypeMismatchException("");
    }
    
    /**
     * Returns a resource in a given resource that has a given key.
     *
     * @param key               a key associated with the wanted resource
     * @return                  a resource bundle object representing rhe resource
     * @throws MissingResourceException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public UResourceBundle get(String key) {
        UResourceBundle obj = handleGet(key, null, this);
        UResourceBundle res = this;
        if (obj == null) {
            while((res=res.getParent())!=null && obj==null){
                //call the get method to recursively fetch the resource
                obj = res.handleGet(key, null, this);
            }
            if (obj == null) {
                String fullName = ICUResourceBundleReader.getFullName(
                        getBaseName(), getLocaleID());
                throw new MissingResourceException(
                        "Can't find resource for bundle " + fullName + ", key "
                                + key, this.getClass().getName(), key);
            }
        }
        ICUResourceBundle.setLoadingStatus(obj, getLocaleID());
        return obj;
    }
    
    /**
     * Returns the string in a given resource at the specified index.
     *
     * @param index            an index to the wanted string.
     * @return                  a string which lives in the resource.
     * @throws IndexOutOfBoundsException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String getString(int index) {
        ICUResourceBundle temp = (ICUResourceBundle)get(index);
        if (temp.getType() == STRING) {
            return temp.getString();
        }
        throw new UResourceTypeMismatchException("");
    }

    /**
     * Returns the resource in a given resource at the specified index.
     *
     * @param index             an index to the wanted resource.
     * @return                  the sub resource UResourceBundle object
     * @throws IndexOutOfBoundsException
     * @throws MissingResourceException
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public UResourceBundle get(int index) {
        UResourceBundle obj = handleGet(index, null, this);
        if (obj == null) {
            obj = (ICUResourceBundle) getParent();
            if (obj != null) {
                obj = obj.get(index);
            }
            if (obj == null)
                throw new MissingResourceException(
                        "Can't find resource for bundle "
                                + this.getClass().getName() + ", key "
                                + getKey(), this.getClass().getName(), getKey());
        }
        ICUResourceBundle.setLoadingStatus(obj, getLocaleID());
        return obj;
    }
    /**
     * Returns the keys in this bundle as an enumeration
     * @return an enumeration containing key strings
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public Enumeration getKeys() {
        initKeysVector();
        return keys.elements();
    }
    
    private Vector keys = null;
    private synchronized void initKeysVector(){
        if(keys!=null){
            return;
        }
        //ICUResourceBundle current = this;
        keys = new Vector();
        Enumeration e = this.handleGetKeys();
        while(e.hasMoreElements()){
            String elem = (String)e.nextElement();
            if(!keys.contains(elem)){
                keys.add(elem);
            }
        }
    }
    
    /**
     * Returns the size of a resource. Size for scalar types is always 1,
     * and for vector/table types is the number of child resources.
     * <br><b><font color='red'>Warning: </font></b> Integer array is treated as a scalar type. There are no
     *          APIs to access individual members of an integer array. It
     *          is always returned as a whole.
     * @return number of resources in a given resource.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the type of a resource.
     * Available types are {@link #INT INT}, {@link #ARRAY ARRAY},
     * {@link #BINARY BINARY}, {@link #INT_VECTOR INT_VECTOR},
     * {@link #STRING STRING}, {@link #TABLE TABLE}.
     *
     * @return type of the given resource.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public int getType() {
        int type = ICUResourceBundle.RES_GET_TYPE(resource);
        if(type==TABLE32){
            return TABLE; //Mask the table32's real type
        }
        return type;
    }

    /**
     * Return the version number associated with this UResourceBundle as an
     * VersionInfo object.
     * @return VersionInfo object containing the version of the bundle
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public VersionInfo getVersion() {
        return null;
    }
    
    /**
     * Returns the iterator which iterates over this
     * resource bundle
     * @return UResourceBundleIterator that iterates over the resources in the bundle
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public UResourceBundleIterator getIterator() {
        return new UResourceBundleIterator(this);
    }
    /**
     * Returns the key associated with a given resource. Not all the resources have a key - only
     * those that are members of a table.
     * @return a key associated to this resource, or null if it doesn't have a key
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String getKey() {
        return key;
    }
    /**
     * Resource type constant for "no resource".
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final int NONE = -1;

    /**
     * Resource type constant for strings.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final int STRING = 0;

    /**
     * Resource type constant for binary data.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final int BINARY = 1;

    /**
     * Resource type constant for tables of key-value pairs.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final int TABLE = 2;

    /**
     * Resource type constant for aliases;
     * internally stores a string which identifies the actual resource
     * storing the data (can be in a different resource bundle).
     * Resolved internally before delivering the actual resource through the API.
     * @internal ICU 3.8
     * @deprecated This API is for internal ICU use only
     */
    protected static final int ALIAS = 3;

    /**
     * Internal use only.
     * Alternative resource type constant for tables of key-value pairs.
     * Never returned by getType().
     * @internal ICU 3.8
     * @deprecated This API is for internal ICU use only
     */
    protected static final int TABLE32 = 4;

    /**
     * Resource type constant for a single 28-bit integer, interpreted as
     * signed or unsigned by the getInt() function.
     * @see #getInt
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final int INT = 7;

    /**
     * Resource type constant for arrays of resources.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final int ARRAY = 8;

    /**
     * Resource type constant for vectors of 32-bit integers.
     * @see #getIntVector
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public static final int INT_VECTOR = 14;

    //====== protected members ==============
    /**
     * Data member where the subclasses store the key
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected String key;
    /**
     * Data member where the subclasses store the size of resources
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected int size = 1;
    /**
     * Data member where the subclasses store the offset within resource data
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected long resource = RES_BOGUS;
    /**
     * Data member where the subclasses store whether the resource is top level
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected boolean isTopLevel = false;

    private static final long RES_BOGUS = 0xffffffff;

    /**
     * Actual worker method for fetching a resource based on the given key.
     * Sub classes must override this method if they support resources with keys.
     * @param key the key string of the resource to be fetched
     * @param table hastable object to hold references of resources already seen
     * @param requested the original resource bundle object on which the get method was invoked.
     *                  The requested bundle and the bundle on which this method is invoked
     *                  are the same, except in the cases where aliases are involved.
     * @return UResourceBundle a resource assoicated with the key
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    protected UResourceBundle handleGet(String key, HashMap table, UResourceBundle requested) {
        return null;
    }
    
    /**
     * Actual worker method for fetching a resource based on the given index.
     * Sub classes must override this method if they support arrays of resources.
     * @param index the index of the resource to be fetched
     * @param table hastable object to hold references of resources already seen
     * @param requested the original resource bundle object on which the get method was invoked.
     *                  The requested bundle and the bundle on which this method is invoked
     *                  are the same, except in the cases where aliases are involved.
     * @return UResourceBundle a resource assoicated with the index 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    protected UResourceBundle handleGet(int index, HashMap table, UResourceBundle requested) {
        return null;
    }
    
    /**
     * Actual worker method for fetching the array of strings in a resource.
     * Sub classes must override this method if they support arrays of strings.
     * @return String[] An array of strings containing strings 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    protected String[] handleGetStringArray() {
        return null;
    }
    
    /**
     * Actual worker method for fetching the keys of resources contained in the resource.
     * Sub classes must override this method if they support keys and associated resources.
     * 
     * @return Enumeration An enumeration of all the keys in this resource.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    protected Enumeration handleGetKeys(){
        Vector keys = new Vector();
        UResourceBundle item = null;
        for (int i = 0; i < size; i++) {
            item = get(i);
            keys.add(item.getKey());
        }
        return keys.elements();
    }
    
    /**
     * {@inheritDoc}
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    // this method is declared in ResourceBundle class
    // so cannot change the signature
    // Override this method
    protected Object handleGetObject(String key) {
        return handleGetObjectImpl(key, this);
    }

    /**
     * Override the superclass method
     */
    // To facilitate XPath style aliases we need a way to pass the reference
    // to requested locale. The only way I could figure out is to implement
    // the look up logic here. This has a disadvantage that if the client
    // loads an ICUResourceBundle, calls ResourceBundle.getObject method
    // with a key that does not exist in the bundle then the lookup is
    // done twice before throwing a MissingResourceExpection.
    private Object handleGetObjectImpl(String key, UResourceBundle requested) {
        Object obj = resolveObject(key, requested);
        if (obj == null) {
            UResourceBundle parent = getParent();
            if (parent != null) {
                obj = parent.handleGetObjectImpl(key, requested);
            }
            if (obj == null)
                throw new MissingResourceException(
                    "Can't find resource for bundle "
                    + this.getClass().getName() + ", key " + key,
                    this.getClass().getName(), key);
        }
        return obj;
    }
    
    // Routine for figuring out the type of object to be returned
    // string or string array
    private Object resolveObject(String key, UResourceBundle requested) {
        if (getType() == STRING) {
            return getString();
        }
        UResourceBundle obj = handleGet(key, null, requested);
        if (obj != null) {
            if (obj.getType() == STRING) {
                return obj.getString();
            }
            try {
                if (obj.getType() == ARRAY) {
                    return obj.handleGetStringArray();
                }
            } catch (UResourceTypeMismatchException ex) {
                return obj;
            }
        }
        return obj;
    }

    /**
     * This method is for setting the loading status of the resource. 
     * The status is analogous to the warning status in ICU4C.
     * @internal ICU 3.8
     * @deprecated This API is for internal ICU use only
     */
    protected abstract void setLoadingStatus(int newStatus);
}
