/*
 *******************************************************************************
 * Copyright (C) 2004-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceTypeMismatchException;

class ICUResourceBundleImpl extends ICUResourceBundle {
    protected ICUResourceBundleImpl(ICUResourceBundleReader reader, String key, String resPath, int resource,
                                    ICUResourceBundleImpl container) {
        super(reader, key, resPath, resource, container);
    }
    protected final ICUResourceBundle createBundleObject(String _key,
                                                         int _resource,
                                                         HashMap<String, String> table,
                                                         UResourceBundle requested,
                                                         boolean[] isAlias) {
        if (isAlias != null) {
            isAlias[0] = false;
        }
        String _resPath = resPath + "/" + _key;
        switch(ICUResourceBundleReader.RES_GET_TYPE(_resource)) {
        case STRING :
        case STRING_V2:
            return new ICUResourceBundleImpl.ResourceString(reader, _key, _resPath, _resource, this);
        case BINARY:
            return new ICUResourceBundleImpl.ResourceBinary(reader, _key, _resPath, _resource, this);
        case ALIAS:
            if (isAlias != null) {
                isAlias[0] = true;
            }
            return findResource(_key, _resPath, _resource, table, requested);
        case INT:
            return new ICUResourceBundleImpl.ResourceInt(reader, _key, _resPath, _resource, this);
        case INT_VECTOR:
            return new ICUResourceBundleImpl.ResourceIntVector(reader, _key, _resPath, _resource, this);
        case ARRAY:
        case ARRAY16:
            return new ICUResourceBundleImpl.ResourceArray(reader, _key, _resPath, _resource, this);
        case TABLE:
        case TABLE16:
        case TABLE32:
            return new ICUResourceBundleImpl.ResourceTable(reader, _key, _resPath, _resource, this);
        default :
            throw new IllegalStateException("The resource type is unknown");
        }
    }

    // Scalar values ------------------------------------------------------- ***

    private static final class ResourceBinary extends ICUResourceBundleImpl {
        public ByteBuffer getBinary() {
            return reader.getBinary(resource);
        }
        public byte [] getBinary(byte []ba) {
            return reader.getBinary(resource, ba);
        }
        ResourceBinary(ICUResourceBundleReader reader, String key, String resPath, int resource,
                       ICUResourceBundleImpl container) {
            super(reader, key, resPath, resource, container);
        }
    }
    private static final class ResourceInt extends ICUResourceBundleImpl {
        public int getInt() {
            return ICUResourceBundleReader.RES_GET_INT(resource);
        }
        public int getUInt() {
            return ICUResourceBundleReader.RES_GET_UINT(resource);
        }
        ResourceInt(ICUResourceBundleReader reader, String key, String resPath, int resource,
                    ICUResourceBundleImpl container) {
            super(reader, key, resPath, resource, container);
        }
    }
    private static final class ResourceString extends ICUResourceBundleImpl {
        private String value;
        public String getString() {
            if (value != null) {
                return value;
            }
            return reader.getString(resource);
        }
        ResourceString(ICUResourceBundleReader reader, String key, String resPath, int resource,
                       ICUResourceBundleImpl container) {
            super(reader, key, resPath, resource, container);
            String s = reader.getString(resource);
            // Allow the reader cache's SoftReference to do its job.
            if (s.length() < ICUResourceBundleReader.LARGE_SIZE / 2) {
                value = s;
            }
        }
    }
    private static final class ResourceIntVector extends ICUResourceBundleImpl {
        public int[] getIntVector() {
            return reader.getIntVector(resource);
        }
        ResourceIntVector(ICUResourceBundleReader reader, String key, String resPath, int resource,
                          ICUResourceBundleImpl container) {
            super(reader, key, resPath, resource, container);
        }
    }

    // Container values ---------------------------------------------------- ***

    private static class ResourceContainer extends ICUResourceBundleImpl {
        protected ICUResourceBundleReader.Container value;

        public int getSize() {
            return value.getSize();
        }
        @Override
        public String getString(int index) {
            int res = value.getContainerResource(reader, index);
            if (res == RES_BOGUS) {
                throw new IndexOutOfBoundsException();
            }
            String s = reader.getString(res);
            if (s != null) {
                return s;
            }
            return super.getString(index);
        }
        protected int getContainerResource(int index) {
            return value.getContainerResource(reader, index);
        }
        protected UResourceBundle createBundleObject(int index, String resKey, HashMap<String, String> table,
                                                     UResourceBundle requested, boolean[] isAlias) {
            int item = getContainerResource(index);
            if (item == RES_BOGUS) {
                throw new IndexOutOfBoundsException();
            }
            return createBundleObject(resKey, item, table, requested, isAlias);
        }
        ResourceContainer(ICUResourceBundleReader reader, String key, String resPath, int resource,
                          ICUResourceBundleImpl container) {
            super(reader, key, resPath, resource, container);
        }
    }
    private static class ResourceArray extends ResourceContainer {
        protected String[] handleGetStringArray() {
            int length = value.getSize();
            String[] strings = new String[length];
            for (int i = 0; i < length; ++i) {
                String s = reader.getString(value.getContainerResource(reader, i));
                if (s == null) {
                    throw new UResourceTypeMismatchException("");
                }
                strings[i] = s;
            }
            return strings;
        }
        public String[] getStringArray() {
            return handleGetStringArray();
        }
        protected UResourceBundle handleGetImpl(String indexStr, HashMap<String, String> table,
                                                UResourceBundle requested,
                                                int[] index, boolean[] isAlias) {
            int i = indexStr.length() > 0 ? Integer.valueOf(indexStr).intValue() : -1;
            if(index != null) {
                index[0] = i;
            }
            if (i < 0) {
                throw new UResourceTypeMismatchException("Could not get the correct value for index: "+ indexStr);
            }
            return createBundleObject(i, indexStr, table, requested, isAlias);
        }
        protected UResourceBundle handleGetImpl(int index, HashMap<String, String> table,
                                                UResourceBundle requested, boolean[] isAlias) {
            return createBundleObject(index, Integer.toString(index), table, requested, isAlias);
        }
        ResourceArray(ICUResourceBundleReader reader, String key, String resPath, int resource,
                      ICUResourceBundleImpl container) {
            super(reader, key, resPath, resource, container);
            value = reader.getArray(resource);
            createLookupCache(); // Use bundle cache to access array entries
        }
    }
    static class ResourceTable extends ResourceContainer {
        protected String getKey(int index) {
            return ((ICUResourceBundleReader.Table)value).getKey(reader, index);
        }
        protected Set<String> handleKeySet() {
            TreeSet<String> keySet = new TreeSet<String>();
            ICUResourceBundleReader.Table table = (ICUResourceBundleReader.Table)value;
            for (int i = 0; i < table.getSize(); ++i) {
                keySet.add(table.getKey(reader, i));
            }
            return keySet;
        }
        protected int getTableResource(String resKey) {
            return ((ICUResourceBundleReader.Table)value).getTableResource(reader, resKey);
        }
        protected int getTableResource(int index) {
            return getContainerResource(index);
        }
        protected UResourceBundle handleGetImpl(String resKey, HashMap<String, String> table,
                                                UResourceBundle requested,
                                                int[] index, boolean[] isAlias) {
            int i = ((ICUResourceBundleReader.Table)value).findTableItem(reader, resKey);
            if(index != null) {
                index[0] = i;
            }
            if (i < 0) {
                return null;
            }
            return createBundleObject(i, resKey, table, requested, isAlias);
        }
        protected UResourceBundle handleGetImpl(int index, HashMap<String, String> table,
                                                UResourceBundle requested, boolean[] isAlias) {
            String itemKey = ((ICUResourceBundleReader.Table)value).getKey(reader, index);
            if (itemKey == null) {
                throw new IndexOutOfBoundsException();
            }
            return createBundleObject(index, itemKey, table, requested, isAlias);
        }
        @Override
        protected Object handleGetObject(String key) {
            // Fast path for common cases: Avoid creating UResourceBundles if possible.
            // It would be even better if we could override getString(key)/getStringArray(key),
            // so that we know the expected object type,
            // but those are final in java.util.ResourceBundle.
            int index = ((ICUResourceBundleReader.Table)value).findTableItem(reader, key);
            if (index >= 0) {
                int res = value.getContainerResource(reader, index);
                // getString(key)
                String s = reader.getString(res);
                if (s != null) {
                    return s;
                }
                // getStringArray(key)
                ICUResourceBundleReader.Container array = reader.getArray(res);
                if (array != null) {
                    int length = array.getSize();
                    String[] strings = new String[length];
                    for (int j = 0;; ++j) {
                        if (j == length) {
                            return strings;
                        }
                        s = reader.getString(array.getContainerResource(reader, j));
                        if (s == null) {
                            // Equivalent to resolveObject(key, requested):
                            // If this is not a string array,
                            // then build and return a UResourceBundle.
                            break;
                        }
                        strings[j] = s;
                    }
                }
            }
            return super.handleGetObject(key);
        }
        /**
         * Returns a String if found, or null if not found or if the key item is not a string.
         */
        String getStringOrNull(String key) {
            int index = ((ICUResourceBundleReader.Table)value).findTableItem(reader, key);
            if (index < 0) {
                return null;
            }
            return reader.getString(value.getContainerResource(reader, index));
        }
        ResourceTable(ICUResourceBundleReader reader, String key, String resPath, int resource,
                      ICUResourceBundleImpl container) {
            super(reader, key, resPath, resource, container);
            value = reader.getTable(resource);
            createLookupCache(); // Use bundle cache to access table entries
        }
    }
}
