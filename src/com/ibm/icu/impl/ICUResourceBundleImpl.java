//##header
/*

 ******************************************************************************

 * Copyright (C) 2004-2006, International Business Machines Corporation and   *

 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.icu.util.StringTokenizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceTypeMismatchException;

//#ifndef FOUNDATION
import java.nio.ByteBuffer;
//#endif

/**
 * @author ram
 */
public class ICUResourceBundleImpl extends ICUResourceBundle {
    //protected byte[] version;

    private byte[] rawData;

    private long rootResource;

    private boolean noFallback;

    private String localeID;

    private String baseName;

    private ULocale ulocale;

    private ClassLoader loader;

    private static final boolean ASSERT = false;
    
    /**
     * 
     * @param baseName
     * @param localeID
     * @param root
     * @return the new bundle
     */
    public static ICUResourceBundle createBundle(String baseName,
            String localeID, ClassLoader root) {

        ICUResourceBundleReader reader = ICUResourceBundleReader.getReader(
                                                                    baseName, localeID, root);

        // could not open the .res file so return null
        if (reader == null) {
            return null;

        }



        ICUResourceBundleImpl bundle = new ICUResourceBundleImpl(reader,
                baseName, localeID, root);
        return bundle.getBundle();

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



    /**
     * Get the noFallback flag specified in the loaded bundle.
     * @return The noFallback flag.
     */
    protected boolean getNoFallback() {
        return noFallback;
    }

    private ICUResourceBundle getBundle() {

        int type = RES_GET_TYPE(rootResource);

        if (type == TABLE) {

            ResourceTable table = new ResourceTable(null, rootResource, "", true);
            if(table.size==1){
                ICUResourceBundle b = table.handleGet(0, table);
                String itemKey = b.getKey();
                
                // %%ALIAS is such a hack!
                if (itemKey.equals("%%ALIAS")) {
                    String locale = b.getString();
                    ICUResourceBundle actual = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, locale);
                    return (ResourceTable) actual;
                }else{
                    return table;
                }
            }else {
                return table;
            }
        } else if (type == TABLE32) {

            // genrb does not generate Table32 with %%ALIAS
            return new ResourceTable32(null, rootResource, "", true);
        } else {
            throw new IllegalStateException("Invalid format error");

        }

    }

    private ICUResourceBundleImpl(ICUResourceBundleReader reader, String baseName,
            String localeID, ClassLoader loader) {
        this.rawData = reader.getData();
        this.rootResource = (UNSIGNED_INT_MASK) & reader.getRootResource();
        this.noFallback = reader.getNoFallback();
        this.baseName = baseName;

        this.localeID = localeID;

        this.ulocale = new ULocale(localeID);

        this.loader = loader;
    }
    static final int RES_GET_TYPE(long res) {
        return (int) ((res) >> 28L);
    }
    private static final int RES_GET_OFFSET(long res) {
        return (int) ((res & 0x0fffffff) * 4);
    }
    /* get signed and unsigned integer values directly from the Resource handle */
    private static final int RES_GET_INT(long res) {
        return (((int) ((res) << 4L)) >> 4L);
    }
    private static final long RES_GET_UINT(long res) {
        long t = ((res) & 0x0fffffffL);
        return t;
    }
    private static final StringBuffer RES_GET_KEY(byte[] rawData,
            int keyOffset) {
        char ch = 0xFFFF; //sentinel
        StringBuffer key = new StringBuffer();
        while ((ch = (char) rawData[keyOffset]) != 0) {
            key.append(ch);
            keyOffset++;
        }
        return key;
    }
    private static final int getIntOffset(int offset) {
        return (offset * 4);
    }
    private static final int getCharOffset(int offset) {
        return (offset * 2);
    }
    private final ICUResourceBundle createBundleObject(String key,
            long resource, String resPath, HashMap table, ICUResourceBundle requested) {
        //if (resource != RES_BOGUS) {
        switch (RES_GET_TYPE(resource)) {
            case STRING : {
                return new ResourceString(key, resPath, resource);
            }
            case BINARY : {
                return new ResourceBinary(key, resPath, resource);
            }
            case ALIAS : {
                return findResource(key, resource, table, requested);
            }
            case INT : {
                return new ResourceInt(key, resPath, resource);
            }
            case INT_VECTOR : {
                return new ResourceIntVector(key, resPath, resource);
            }
            case ARRAY : {
                return new ResourceArray(key, resPath, resource);
            }
            case TABLE32 : {
                return new ResourceTable32(key, resPath, resource);
            }
            case TABLE : {
                return new ResourceTable(key, resPath, resource);
            }
            default :
                throw new IllegalStateException("The resource type is unknown");
        }
        //}
        //return null;
    }
    private int findKey(int size, int currentOffset, Resource res, String target) {
        int mid = 0, start = 0, limit = size, rc;
        int lastMid = -1;
        //int myCharOffset = 0, keyOffset = 0;
        for (;;) {
            mid = ((start + limit) / 2);
            if (lastMid == mid) { /* Have we moved? */
                break; /* We haven't moved, and it wasn't found. */
            }
            lastMid = mid;
            String comp = res.getKey(currentOffset, mid);
            rc = target.compareTo(comp);
            if (rc < 0) {
                limit = mid;
            } else if (rc > 0) {
                start = mid;
            } else {
                return mid;
            }
        }
        return -1;
    }
    private interface Resource {
        public String getKey(int currentOfset, int index);
    }
    private class ResourceTable extends ICUResourceBundle implements Resource {

        protected ICUResourceBundle handleGet(String key, ICUResourceBundle requested) {
            return handleGet(key, null, requested);
        }
        protected ICUResourceBundle handleGet(String key, HashMap table, ICUResourceBundle requested) {
            if(size<=0){
                return null;
            }
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset  
            int currentOffset = (offset) + getCharOffset(1);
            //int keyOffset = rawData.getChar(currentOffset);
            /* do a binary search for the key */
            int foundOffset = findKey(size, currentOffset, this, key);
            if (foundOffset == -1) {
                //throw new MissingResourceException(ICUResourceBundleReader.getFullName(baseName, localeID),
                //                                    localeID,
                //                                    key);
                return null;
            }
            currentOffset += getCharOffset(size + (~size & 1))
                    + getIntOffset(foundOffset);
            long resource = (UNSIGNED_INT_MASK) & ICUResourceBundleImpl.getInt(rawData, currentOffset);
            String path = (isTopLevel == true) ? key : resPath + "/" + key;
            return createBundleObject(key, resource, path, table, requested);
        }
        protected ICUResourceBundle handleGet(int index, ICUResourceBundle requested) {
            return handleGet(index, null, requested);
        }
        public String getKey(int currentOffset, int index) {
            int charOffset = currentOffset + getCharOffset(index);
            int keyOffset = getChar(rawData,charOffset);
            return RES_GET_KEY(rawData, keyOffset).toString();
        }
        protected ICUResourceBundle handleGet(int index, HashMap table, ICUResourceBundle requested) {
            if (index > size) {
                throw new IndexOutOfBoundsException();
            }
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset  
            int currentOffset = (offset) + getCharOffset(1);
            String itemKey = getKey(currentOffset, index);
            currentOffset += getCharOffset(size + (~size & 1))
                    + getIntOffset(index);
            long resource = (UNSIGNED_INT_MASK) & ICUResourceBundleImpl.getInt(rawData,currentOffset);
            String path = (isTopLevel == true)
                    ? Integer.toString(index)
                    : resPath + "/" + index;
            return createBundleObject(itemKey, resource, path, table, requested);
        }
        private int countItems() {
            int offset = RES_GET_OFFSET(resource);
            int value = getChar(rawData,offset);
            return value;
        }
        private ResourceTable(String key, String resPath, long resource) {
            this(key, resource, resPath, false);
        }
        private ResourceTable(String key, long resource, String resPath,
                boolean isTopLevel) {
            this.key = key;
            this.resource = resource;
            this.isTopLevel = isTopLevel;
            this.size = countItems();
            this.resPath = resPath;
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
            return ICUResourceBundleImpl.this.getParent();
        }
        protected void setParent(ResourceBundle parent) {
            ICUResourceBundleImpl.this.setParent(parent);
        }
    }
    private class ResourceTable32 extends ICUResourceBundle implements Resource {

        protected ICUResourceBundle handleGet(String key, ICUResourceBundle requested) {
            if(size<=0){
                return null;
            }
            return handleGet(key, null, requested);
        }
        protected ICUResourceBundle handleGet(String key, HashMap table, ICUResourceBundle requested) {
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset  
            int currentOffset = (offset) + getIntOffset(1);
            //int keyOffset = rawData.getChar(currentOffset);
            /* do a binary search for the key */
            int foundOffset = findKey(size, currentOffset, this, key);
            if (foundOffset == -1) {
                throw new MissingResourceException(
                        "Could not find resource ",
                        ICUResourceBundleReader.getFullName(baseName, localeID),
                        key);
            }
            currentOffset += getIntOffset(size) + getIntOffset(foundOffset);
            long resource = (UNSIGNED_INT_MASK) & ICUResourceBundleImpl.getInt(rawData,currentOffset);
            String path = (isTopLevel == true) ? key : resPath + "/" + key;
            return createBundleObject(key, resource, path, table, requested);
        }
        protected ICUResourceBundle handleGet(int index, ICUResourceBundle requested) {
            return handleGet(index, null, requested);
        }
        public String getKey(int currentOffset, int index) {
            int charOffset = currentOffset + getIntOffset(index);
            int keyOffset = ICUResourceBundleImpl.getInt(rawData,charOffset);
            return RES_GET_KEY(rawData, keyOffset).toString();
        }
        protected ICUResourceBundle handleGet(int index, HashMap table, ICUResourceBundle requested) {
            if (index > size) {
                throw new IndexOutOfBoundsException();
            }
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset  
            int currentOffset = (offset) + getIntOffset(1)
                    + getIntOffset(index);
            String itemKey = getKey(currentOffset, 0);
            currentOffset += getIntOffset(size);
            long resource = (UNSIGNED_INT_MASK) & ICUResourceBundleImpl.getInt(rawData,currentOffset);
            String path = (isTopLevel == true)
                    ? Integer.toString(index)
                    : resPath + "/" + index;
            return createBundleObject(itemKey, resource, path, table, requested);
        }
        private int countItems() {
            int offset = RES_GET_OFFSET(resource);
            int value = ICUResourceBundleImpl.getInt(rawData, offset);
            return value;
        }
        private ResourceTable32(String key, long resource, String resPath,
                boolean isTopLevel) {
            this.resource = resource;
            this.key = key;
            this.isTopLevel = isTopLevel;
            this.size = countItems();
            this.resPath = resPath;
        }
        private ResourceTable32(String key, String resPath, long resource) {
            this(key, resource, resPath, false);
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
            return ICUResourceBundleImpl.this.getParent();
        }
        protected void setParent(ResourceBundle parent) {
            ICUResourceBundleImpl.this.setParent(parent);
        }

    }
    private class ResourceString extends ICUResourceBundle {
        private String value;
        public String getString() {
            return value;
        }
        private ResourceString(String key, String resPath, long resource) {
            value = getStringValue(resource);
            this.key = key;
            this.resource = resource;
            this.resPath = resPath;
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
            return ICUResourceBundleImpl.this.getParent();
        }
    }
    private class ResourceInt extends ICUResourceBundle {
        public int getInt() {
            return RES_GET_INT(resource);
        }
        public int getUInt() {
            long ret = RES_GET_UINT(resource);
            return (int) ret;
        }
        private ResourceInt(String key, String resPath, long resource) {
            this.key = key;
            this.resource = resource;
            this.resPath = resPath;
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
            return ICUResourceBundleImpl.this.getParent();
        }
    }
    private class ResourceArray extends ICUResourceBundle {
        protected String[] handleGetStringArray() {
            String[] strings = new String[size];
            ICUResourceBundleIterator iter = getIterator();
            int i = 0;
            while (iter.hasNext()) {
                strings[i++] = iter.next().getString();
            }
            return strings;
        }
        /**
         * @internal ICU 3.0
         */
        public String[] getStringArray() {
            return handleGetStringArray();
        }
        protected ICUResourceBundle handleGet(String index, ICUResourceBundle requested) {
            return handleGet(index, null, requested);
        }
        protected ICUResourceBundle handleGet(String index, HashMap table, ICUResourceBundle requested) {
            int val = getIndex(index);
            if (val > -1) {
                return handleGet(val, table, requested);
            }
            throw new UResourceTypeMismatchException("Could not get the correct value for index: "+ index);
        }
        protected ICUResourceBundle handleGet(int index, ICUResourceBundle requested) {
            return handleGet(index, null, requested);
        }
        protected ICUResourceBundle handleGet(int index, HashMap table, ICUResourceBundle requested) {
            if (index > size) {
                throw new IndexOutOfBoundsException();
            }
            int offset = RES_GET_OFFSET(resource);
            int itemOffset = offset + getIntOffset(index + 1);
            long itemResource = (UNSIGNED_INT_MASK) & ICUResourceBundleImpl.getInt(rawData,itemOffset);
            String path = (isTopLevel == true) ? Integer.toString(index) : resPath + "/" + index;
            return createBundleObject(null, itemResource, path, table, requested);
        }
        private int countItems() {
            int offset = RES_GET_OFFSET(resource);
            int value = ICUResourceBundleImpl.getInt(rawData,offset);
            return value;
        }
        private ResourceArray(String key, String resPath, long resource) {
            this.resource = resource;
            this.key = key;
            this.size = countItems();
            this.resPath = resPath;
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
            return ICUResourceBundleImpl.this.getParent();
        }
    }
    private static char makeChar(byte b1, byte b0) {
        return (char)((b1 << 8) | (b0 & 0xff));
    }
    private static char getChar(byte[]data, int offset){
        return makeChar(data[offset], data[offset+1]);
    }
    private static int makeInt(byte b3, byte b2, byte b1, byte b0) {
        return (int)((((b3 & 0xff) << 24) |
                  ((b2 & 0xff) << 16) |
                  ((b1 & 0xff) <<  8) |
                  ((b0 & 0xff) <<  0)));
    }
    
    private static int getInt(byte[] data, int offset){
        if (ASSERT) Assert.assrt("offset < data.length", offset < data.length);
        return makeInt(data[offset], data[offset+1], 
                       data[offset+2], data[offset+3]);
    }
    
    private class ResourceBinary extends ICUResourceBundle {
        private byte[] value;
        public ByteBuffer getBinary() {

            return ByteBuffer.wrap(value);
        }
        private byte[] getValue() {

            int offset = RES_GET_OFFSET(resource);

            int length = ICUResourceBundleImpl.getInt(rawData,offset);

            int byteOffset = offset + getIntOffset(1);
            byte[] dst = new byte[length];
            if (ASSERT) Assert.assrt("byteOffset+length < rawData.length", byteOffset+length < rawData.length);
            System.arraycopy(rawData, byteOffset, dst, 0, length);
            return dst;
        }
        public ResourceBinary(String key, String resPath, long resource) {
            this.resource = resource;
            this.key = key;
            this.resPath = resPath;
            value = getValue();
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
            return ICUResourceBundleImpl.this.getParent();
        }
    }
    private class ResourceIntVector extends ICUResourceBundle {
        private int[] value;
        public int[] getIntVector() {
            return value;
        }
        private int[] getValue() {
            int offset = RES_GET_OFFSET(resource);
            int length = ICUResourceBundleImpl.getInt(rawData,offset);
            int intOffset = offset + getIntOffset(1);
            int[] val = new int[length];
            int byteLength = getIntOffset(length);
            
            if (ASSERT) Assert.assrt("(intOffset+byteLength)<rawData.length", (intOffset+byteLength)<rawData.length);
            
            for(int i=0; i<length;i++){
                val[i]=ICUResourceBundleImpl.getInt(rawData, intOffset+getIntOffset(i));
            }
            return val;
        }
        public ResourceIntVector(String key, String resPath, long resource) {
            this.key = key;
            this.resource = resource;
            this.size = 1;
            this.resPath = resPath;
            value = getValue();
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
            return ICUResourceBundleImpl.this.getParent();
        }
    }
    private String getStringValue(long resource) {
        int offset = RES_GET_OFFSET(resource);
        int length = getInt(rawData,offset);
        int stringOffset = offset + getIntOffset(1);
        char[] dst = new char[length];
        if (ASSERT) Assert.assrt("(stringOffset+getCharOffset(length)) < rawData.length", (stringOffset+getCharOffset(length)) < rawData.length);
        for(int i=0; i<length; i++){
            dst[i]=getChar(rawData, stringOffset+getCharOffset(i));
        }
        return new String(dst);
    }
    private static final char RES_PATH_SEP_CHAR = '/';
    private static final String ICUDATA = "ICUDATA";
    private static final String LOCALE = "LOCALE";
    
    private static final int getIndex(String s) {
        if (s.length() >= 1) {
            return Integer.valueOf(s).intValue();
        }
        return -1;
    }
    private ICUResourceBundle findResource(String key, long resource,
                                            HashMap table, 
                                            ICUResourceBundle requested) {
        ClassLoader loaderToUse = loader;
        String locale = null, keyPath = null;
        String bundleName;
        String resPath = getStringValue(resource);
        if (table == null) {
            table = new HashMap();
        }
        if (table.get(resPath) != null) {
            throw new IllegalArgumentException(
                    "Circular references in the resource bundles");
        }
        table.put(resPath, "");
        if (resPath.indexOf(RES_PATH_SEP_CHAR) == 0) {
            int i = resPath.indexOf(RES_PATH_SEP_CHAR, 1);
            int j = resPath.indexOf(RES_PATH_SEP_CHAR, i + 1);
            bundleName = resPath.substring(1, i);
            locale = resPath.substring(i + 1);
            if (j != -1) {
                locale = resPath.substring(i + 1, j);
                keyPath = resPath.substring(j + 1, resPath.length());
            }
            //there is a path included
            if (bundleName.equals(ICUDATA)) {
                bundleName = ICU_BASE_NAME;
                loaderToUse = ICU_DATA_CLASS_LOADER;
            }
        } else {
            //no path start with locale
            int i = resPath.indexOf(RES_PATH_SEP_CHAR);
            keyPath = resPath.substring(i + 1);
            if (i != -1) {
                locale = resPath.substring(0, i);
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
            bundle = requested;
            keyPath = resPath.substring(LOCALE.length() + 2/* prepending and appending / */, resPath.length());
            locale = requested.getLocaleID();
            sub = ICUResourceBundle.findResourceWithFallback(keyPath, requested, null);
            sub.resPath = "/" + sub.getLocaleID() + "/" + keyPath;
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
                sub = current.getImpl(subKey, table, requested);
                if (sub == null) {
                    break;
                }
                current = sub;
            }
        } else {
            // if the sub resource is not found
            // try fetching the sub resource with
            // the key of this alias resource
            sub = bundle.get(key);
        }
            sub.resPath = resPath;
        }
        if (sub == null) {
            throw new MissingResourceException(localeID, baseName, key);
        }
        return sub;
    }
}
