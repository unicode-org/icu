/*
 *******************************************************************************
 * Copyright (C) 2004-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.MissingResourceException;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;
import com.ibm.icu.util.UResourceTypeMismatchException;

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

        protected UResourceBundle handleGetImpl(String indexStr, HashMap<String, String> table, UResourceBundle requested,
                int[] index, boolean[] isAlias) {
            index[0] = getIndex(indexStr);
            if (index[0] > -1) {
                return handleGetImpl(index[0], table, requested, isAlias);
            }
            throw new UResourceTypeMismatchException("Could not get the correct value for index: "+ index);
        }

        protected UResourceBundle handleGetImpl(int index, HashMap<String, String> table, UResourceBundle requested,
                boolean[] isAlias) {
            if (index > size) {
                throw new IndexOutOfBoundsException();
            }
            int offset = RES_GET_OFFSET(resource);
            int itemOffset = offset + getIntOffset(index + 1);
            long itemResource = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData,itemOffset);
            String path = (isTopLevel == true) ? Integer.toString(index) : resPath + "/" + index;

            return createBundleObject(Integer.toString(index), itemResource, path, table, requested, this, isAlias);
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
            createLookupCache(); // Use bundle cache to access array entries
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
            //if (ASSERT) Assert.assrt("byteOffset+length < rawData.length", byteOffset+length < rawData.length);
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
            //int byteLength = getIntOffset(length);

            //if (ASSERT) Assert.assrt("(intOffset+byteLength)<rawData.length", (intOffset+byteLength)<rawData.length);

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

        protected UResourceBundle handleGetImpl(String resKey, HashMap<String, String> table, UResourceBundle requested,
                int[] index, boolean[] isAlias) {
            if(size<=0){
                return null;
            }
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset
            int currentOffset = (offset) + getCharOffset(1);
            //int keyOffset = rawData.getChar(currentOffset);
            /* do a binary search for the key */
            index[0] = findKey(size, currentOffset, this, resKey);
            if (index[0] == -1) {
                //throw new MissingResourceException(ICUResourceBundleReader.getFullName(baseName, localeID),
                //                                    localeID,
                //                                    key);
                return null;
            }
            currentOffset += getCharOffset(size + (~size & 1))
                    + getIntOffset(index[0]);
            long resOffset = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData, currentOffset);
            String path = (isTopLevel == true) ? resKey : resPath + "/" + resKey;

            return createBundleObject(resKey, resOffset, path, table, requested, this, isAlias);
        }

        public int getOffset(int currentOffset, int index) {
            return getChar(rawData, currentOffset + getCharOffset(index));
        }
        protected UResourceBundle handleGetImpl(int index, HashMap<String, String> table, UResourceBundle requested,
                boolean[] isAlias) {
            if (index > size) {
                throw new IndexOutOfBoundsException();
            }
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset
            int currentOffset = (offset) + getCharOffset(1);
            int betterOffset = getOffset(currentOffset, index);
            String itemKey = RES_GET_KEY(rawData, betterOffset).toString();
            currentOffset += getCharOffset(size + (~size & 1))
                    + getIntOffset(index);
            long resOffset = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData,currentOffset);
            String path = (isTopLevel == true) ? itemKey : resPath + "/" + itemKey;

            return createBundleObject(itemKey, resOffset, path, table, requested, this, isAlias);
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
        void initialize(String resKey, String resourcePath, long resOffset,
                ICUResourceBundle bundle, boolean topLevel){
            if(bundle!=null){
                assign(this, bundle);
            }
            key = resKey;
            resource = resOffset;
            isTopLevel = topLevel;
            size = countItems();
            resPath = resourcePath;
            createLookupCache(); // Use bundle cache to access nested resources
        }
        ResourceTable(String key, String resPath, long resource,
                ICUResourceBundle bundle, boolean isTopLevel) {
            initialize(key, resPath, resource, bundle, isTopLevel);
        }
    }
    static final class ResourceTable32 extends ICUResourceBundle{

        protected UResourceBundle handleGetImpl(String resKey, HashMap<String, String> table, UResourceBundle requested,
                int[] index, boolean[] isAlias) {
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset
            int currentOffset = (offset) + getIntOffset(1);
            //int keyOffset = rawData.getChar(currentOffset);
            /* do a binary search for the key */
            index[0] = findKey(size, currentOffset, this, resKey);
            if (index[0] == -1) {
                throw new MissingResourceException(
                        "Could not find resource ",
                        ICUResourceBundleReader.getFullName(baseName, localeID),
                        resKey);
            }
            currentOffset += getIntOffset(size) + getIntOffset(index[0]);
            long resOffset = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData,currentOffset);
            String path = (isTopLevel == true) ? resKey : resPath + "/" + resKey;

            return createBundleObject(resKey, resOffset, path, table, requested, this, isAlias);
        }

        public int getOffset(int currentOffset, int index) {
            return ICUResourceBundle.getInt(rawData, currentOffset + getIntOffset(index));
        }
        protected UResourceBundle handleGetImpl(int index, HashMap<String, String> table, UResourceBundle requested,
                boolean[] isAlias) {
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
            int betterOffset = getOffset(currentOffset, 0);
            String itemKey = RES_GET_KEY(rawData, betterOffset).toString();
            currentOffset += getIntOffset(size);
            long resOffset = (UNSIGNED_INT_MASK) & ICUResourceBundle.getInt(rawData,currentOffset);
            String path = (isTopLevel == true) ? Integer.toString(index) : resPath + "/" + index;

            return createBundleObject(itemKey, resOffset, path, table, requested, this, isAlias);
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
        void initialize(String resKey, String resourcePath, long resOffset,
                ICUResourceBundle bundle, boolean topLevel){
            if(bundle!=null){
                assign(this, bundle);
            }
            key = resKey;
            resource = resOffset;
            isTopLevel = topLevel;
            size = countItems();
            resPath = resourcePath;
            createLookupCache(); // Use bundle cache to access nested resources
        }
        ResourceTable32(String key, String resPath, long resource,
                ICUResourceBundle bundle, boolean isTopLevel) {
            initialize(key, resPath, resource, bundle, isTopLevel);
        }
    }
}
