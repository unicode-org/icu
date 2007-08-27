//##header J2SE15
/**
 *******************************************************************************
 * Copyright (C) 2004-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.HashMap;
import java.util.MissingResourceException;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;
import com.ibm.icu.util.UResourceTypeMismatchException;

//#if defined(FOUNDATION10) || defined(J2SE13) || defined(ECLIPSE_FRAGMENT)
//#else
import java.nio.ByteBuffer; 
//#endif 

class ICUResourceBundleImpl {
    
    static final class ResourceArray extends ICUResourceBundle {
        protected String[] handleGetStringArray() {
            String[] strings = new String[size];
            UResourceBundleIterator iter = getIterator();
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

        protected UResourceBundle handleGet(String index, HashMap table, UResourceBundle requested) {
            int val = getIndex(index);
            if (val > -1) {
                return handleGet(val, table, requested);
            }
            throw new UResourceTypeMismatchException("Could not get the correct value for index: "+ index);
        }

        protected UResourceBundle handleGet(int index, HashMap table, UResourceBundle requested) {
            if (index > size) {
                throw new IndexOutOfBoundsException();
            }
            int offset = RES_GET_OFFSET(resource);
            int itemOffset = offset + getIntOffset(index + 1);
            long itemResource = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData,itemOffset);
            String path = (isTopLevel == true) ? Integer.toString(index) : resPath + "/" + index;
            return createBundleObject(null, itemResource, path, table, requested, this);
        }
        private int countItems() {
            int offset = RES_GET_OFFSET(resource);
            int value = getInt(rawData,offset);
            return value;
        }
        ResourceArray(String key, String resPath, long resource, ICUResourceBundle bundle) {
            assign(this, bundle);
            this.resource = resource;
            this.key = key;
            this.size = countItems();
            this.resPath = resPath;
        }
    }
    static final class ResourceBinary extends ICUResourceBundle {
        private byte[] value;
        public ByteBuffer getBinary() {
            return ByteBuffer.wrap(value);
        }      
        public byte [] getBinary(byte []ba) {
            return value;
        }
        private byte[] getValue() {
            int offset = RES_GET_OFFSET(resource);
            int length = ICUResourceBundle.getInt(rawData,offset);
            int byteOffset = offset + getIntOffset(1);
            byte[] dst = new byte[length];
            if (ASSERT) Assert.assrt("byteOffset+length < rawData.length", byteOffset+length < rawData.length);
            System.arraycopy(rawData, byteOffset, dst, 0, length);
            return dst;
        }
        ResourceBinary(String key, String resPath, long resource, ICUResourceBundle bundle) {
            assign(this, bundle);
            this.resource = resource;
            this.key = key;
            this.resPath = resPath;
            value = getValue();
            
        }
    }
    static final class ResourceInt extends ICUResourceBundle {
        public int getInt() {
            return RES_GET_INT(resource);
        }
        public int getUInt() {
            long ret = RES_GET_UINT(resource);
            return (int) ret;
        }
        ResourceInt(String key, String resPath, long resource, ICUResourceBundle bundle) {
            assign(this, bundle);
            this.key = key;
            this.resource = resource;
            this.resPath = resPath;   
        }
    }
    
    static final class ResourceString extends ICUResourceBundle {
        private String value;
        public String getString() {
            return value;
        }
        ResourceString(String key, String resPath, long resource, ICUResourceBundle bundle) {
            assign(this, bundle);
            value = getStringValue(resource);
            this.key = key;
            this.resource = resource;
            this.resPath = resPath;
        }
    }
    
    static final class ResourceIntVector extends ICUResourceBundle {
        private int[] value;
        public int[] getIntVector() {
            return value;
        }
        private int[] getValue() {
            int offset = RES_GET_OFFSET(resource);
            int length = ICUResourceBundle.getInt(rawData,offset);
            int intOffset = offset + getIntOffset(1);
            int[] val = new int[length];
            int byteLength = getIntOffset(length);
            
            if (ASSERT) Assert.assrt("(intOffset+byteLength)<rawData.length", (intOffset+byteLength)<rawData.length);
            
            for(int i=0; i<length;i++){
                val[i]=ICUResourceBundle.getInt(rawData, intOffset+getIntOffset(i));
            }
            return val;
        }
        ResourceIntVector(String key, String resPath, long resource, ICUResourceBundle bundle) {
            assign(this, bundle);
            this.key = key;
            this.resource = resource;
            this.size = 1;
            this.resPath = resPath;
            value = getValue();
        }
    }
    
    static final class ResourceTable extends ICUResourceBundle {

        protected UResourceBundle handleGet(String key, HashMap table, UResourceBundle requested) {
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
            long resource = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData, currentOffset);
            String path = (isTopLevel == true) ? key : resPath + "/" + key;
            return createBundleObject(key, resource, path, table, requested, this);
        }

        public String getKey(int currentOffset, int index) {
            int charOffset = currentOffset + getCharOffset(index);
            int keyOffset = getChar(rawData,charOffset);
            return RES_GET_KEY(rawData, keyOffset).toString();
        }
        protected UResourceBundle handleGet(int index, HashMap table, UResourceBundle requested) {
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
            long resource = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData,currentOffset);
            String path = (isTopLevel == true)
                    ? Integer.toString(index)
                    : resPath + "/" + index;
            return createBundleObject(itemKey, resource, path, table, requested, this);
        }
        private int countItems() {
            int offset = RES_GET_OFFSET(resource);
            int value = getChar(rawData,offset);
            return value;
        }
        ResourceTable(String key, String resPath, long resource, ICUResourceBundle bundle) {
            this(key, resPath, resource, bundle, false);
        }
        ResourceTable(ICUResourceBundleReader reader, String baseName, String localeID, ClassLoader loader) {

            this.rawData = reader.getData();
            this.rootResource = (UNSIGNED_INT_MASK) & reader.getRootResource();
            this.noFallback = reader.getNoFallback();
            this.baseName = baseName;
            this.localeID = localeID;
            this.ulocale = new ULocale(localeID);
            this.loader = loader;
            initialize(null, "", rootResource, null, isTopLevel);
        }
        void initialize(String key, String resPath, long resource, 
                ICUResourceBundle bundle, boolean isTopLevel){
            if(bundle!=null){
                assign(this, bundle);
            }
            this.key = key;
            this.resource = resource;
            this.isTopLevel = isTopLevel;
            this.size = countItems();
            this.resPath = resPath;
        }
        ResourceTable(String key, String resPath, long resource, 
                ICUResourceBundle bundle, boolean isTopLevel) {
            initialize(key, resPath, resource, bundle, isTopLevel);
        }
    }
    static final class ResourceTable32 extends ICUResourceBundle{

        protected UResourceBundle handleGet(String key, HashMap table, UResourceBundle requested) {
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
            long resource = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData,currentOffset);
            String path = (isTopLevel == true) ? key : resPath + "/" + key;
            return createBundleObject(key, resource, path, table, requested, this);
        }

        public String getKey(int currentOffset, int index) {
            int charOffset = currentOffset + getIntOffset(index);
            int keyOffset = ICUResourceBundle.getInt(rawData,charOffset);
            return RES_GET_KEY(rawData, keyOffset).toString();
        }
        protected UResourceBundle handleGet(int index, HashMap table, UResourceBundle requested) {
            if(size<=0){
                return null;
            }
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
            long resource = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData,currentOffset);
            String path = (isTopLevel == true)
                    ? Integer.toString(index)
                    : resPath + "/" + index;
            return createBundleObject(itemKey, resource, path, table, requested, this);
        }
        private int countItems() {
            int offset = RES_GET_OFFSET(resource);
            int value = ICUResourceBundle.getInt(rawData, offset);
            return value;
        }
        ResourceTable32(String key, String resPath, long resource, ICUResourceBundle bundle) {
            this(key, resPath, resource, bundle, false);
        }
        ResourceTable32(ICUResourceBundleReader reader, String baseName, String localeID, ClassLoader loader) {

            this.rawData = reader.getData();
            this.rootResource = (UNSIGNED_INT_MASK) & reader.getRootResource();
            this.noFallback = reader.getNoFallback();
            this.baseName = baseName;
            this.localeID = localeID;
            this.ulocale = new ULocale(localeID);
            this.loader = loader;
            initialize(null, "", rootResource, null, isTopLevel);
        }
        void initialize(String key, String resPath, long resource, 
                ICUResourceBundle bundle, boolean isTopLevel){
            if(bundle!=null){
                assign(this, bundle);
            }
            this.key = key;
            this.resource = resource;
            this.isTopLevel = isTopLevel;
            this.size = countItems();
            this.resPath = resPath;
            
        }
        ResourceTable32(String key, String resPath, long resource, 
                ICUResourceBundle bundle, boolean isTopLevel) {
            initialize(key, resPath, resource, bundle, isTopLevel);
        }
    }
}
