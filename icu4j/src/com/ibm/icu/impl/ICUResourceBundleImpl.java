/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package com.ibm.icu.impl;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.icu.util.StringTokenizer;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceTypeMismatchException;
/**
 * @author ram
 */
public class ICUResourceBundleImpl extends ICUResourceBundle {
	//protected byte[] version;
	private ByteBuffer rawData;
	private long rootResource;
	/**
	 * 
	 * @param baseName
	 * @param localeID
	 * @param root
	 * @return
	 */
	public static ICUResourceBundle createBundle(String baseName,
			String localeID, ClassLoader root) {
        ICUResourceBundleReader reader = new ICUResourceBundleReader(baseName, localeID, root);
        ByteBuffer rawData = reader.getData();
        long rootResource = (UNSIGNED_INT_MASK)& rawData.getInt(0);
        ICUResourceBundleImpl bundle = new ICUResourceBundleImpl(rawData, baseName, localeID, rootResource);
        return bundle.getBundle();
	}
    private ICUResourceBundle getBundle(){
        int type = RES_GET_TYPE(rootResource);
        
        if(type==TABLE){
            // %%ALIAS is such a hack! I can understand the
            // ICU4C legacy .. do we need to port it?
            ResourceTable table = new  ResourceTable(null, rootResource, true);   
            
            ICUResourceBundle b = table.handleGet(0);
            String itemKey = b.getKey();
            if(itemKey.equals("%%ALIAS")){
                String locale = b.getString();
                ICUResourceBundle actual =  (ICUResourceBundle)UResourceBundle.getBundleInstance(baseName, locale);
                return (ResourceTable)actual;
            }  else {
                return table;
            }
            
        }else if(type == TABLE32){
            return new ResourceTable32(null, rootResource, true); 
        }else{
             throw new RuntimeException("Invalid format error"); 
        }
    }
	private ICUResourceBundleImpl(ByteBuffer rawData, String baseName,
                        			String localeID, long rootResource) {
		this.rawData = rawData;
		this.rootResource = rootResource;
		this.baseName = baseName;
		this.localeID = localeID;
	}
	private static final int RES_GET_TYPE(long res) {
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
	private static final StringBuffer RES_GET_KEY(ByteBuffer rawData, int keyOffset) {
		char ch = 0xFFFF; //sentinel
		StringBuffer key = new StringBuffer();
		while ((ch = (char) rawData.get(keyOffset)) != 0) {
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
	private final ICUResourceBundle createBundleObject(String key, long resource, Hashtable table) {
		if (resource != RES_BOGUS) {
			switch (RES_GET_TYPE(resource)) {
				case STRING :
					{
						return new ResourceString(key, resource);
					}
				case BINARY :
					{
						return new ResourceBinary(key, resource);
					}
				case ALIAS :
					{
						return findResource(key,resource, table);
					}
				case INT :
					{
						return new ResourceInt(key, resource);
					}
				case INT_VECTOR :
					{
						return new ResourceIntVector(key, resource);
					}
				case ARRAY :
					{
						return new ResourceArray(key, resource);
					}
				case TABLE32 :
					{
						return new ResourceTable32(key, resource);
					}
				case TABLE :
					{
						return new ResourceTable(key, resource);
					}
				default :
					throw new InternalError("The resource type is unknown");
			}
		}
		return null;
	}
	private static final String getTableKey(ByteBuffer rawData, int size, int currentOffset, int index, int type) {
		switch (type) {
			case TABLE32 :
				{
					int charOffset = currentOffset + getIntOffset(index);
					int keyOffset = rawData.getInt(charOffset);
					return RES_GET_KEY(rawData, keyOffset).toString();
				}
			case TABLE :
			default :
				{
					int charOffset = currentOffset + getCharOffset(index);
					int keyOffset = rawData.getChar(charOffset);
					return RES_GET_KEY(rawData, keyOffset).toString();
				}
		}
	}
	private static int findKey(ByteBuffer rawData, int size, int currentOffset, int type, String target) {
		int mid = 0, start = 0, limit = size, rc;
		int lastMid = -1;
		//int myCharOffset = 0, keyOffset = 0;
		for (;;) {
			mid = ((start + limit) / 2);
			if (lastMid == mid) { /* Have we moved? */
				break; /* We haven't moved, and it wasn't found. */
			}
			lastMid = mid;
			String comp = getTableKey(rawData, size, currentOffset, mid, type);
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
    private ResourceBundle parent(){
        return parent;
    }
    private String baseName(){
        return baseName;   
    }
    private String localeID(){
        return localeID;   
    }
	private class ResourceTable extends ICUResourceBundle {

		public ICUResourceBundle handleGet(String key) {
			return handleGet(key, null);
		}
		protected ICUResourceBundle handleGet(String key, Hashtable table) {
			int offset = RES_GET_OFFSET(resource);
			// offset+0 contains number of entries
			// offset+1 contains the keyOffset  
			int currentOffset = (offset) + getCharOffset(1);
			//int keyOffset = rawData.getChar(currentOffset);
			/* do a binary search for the key */
			int foundOffset = findKey(rawData, size, currentOffset, type, key);
			if (foundOffset == -1) {
				//throw new MissingResourceException(ICUResourceBundleReader.getFullName(baseName, localeID),
				//                                    localeID,
				//                                    key);
				return null;
			}
			currentOffset += getCharOffset(size + (~size & 1))
					+ getIntOffset(foundOffset);
			long resource = (UNSIGNED_INT_MASK) & rawData.getInt(currentOffset);
			return createBundleObject(key, resource, table);
		}
		public ICUResourceBundle handleGet(int index) {
			return handleGet(index, null);
		}
		public ICUResourceBundle handleGet(int index, Hashtable table) {
			if (index > size) {
				throw new IndexOutOfBoundsException();
			}
			int offset = RES_GET_OFFSET(resource);
			// offset+0 contains number of entries
			// offset+1 contains the keyOffset  
			int currentOffset = (offset) + getCharOffset(1);
			String itemKey = getTableKey(rawData, size, currentOffset, index,
					type);
			currentOffset += getCharOffset(size + (~size & 1))
					+ getIntOffset(index);
			long resource = (UNSIGNED_INT_MASK) & rawData.getInt(currentOffset);
			return createBundleObject(itemKey, resource, table);
		}
        private int countItems(){   
            int offset = RES_GET_OFFSET(resource);
            int value = rawData.getChar(offset);
            return value;  
        }
		private ResourceTable(String key, long resource) {
			this(key, resource, false);
		}
		private ResourceTable(String key, long resource, boolean isTopLevel) {
			this.key = key;
			this.resource = resource;
			this.isTopLevel = isTopLevel;
			this.size = countItems();
            this.baseName = baseName();
            this.localeID = localeID();
            this.parent  = parent();
		}
	}
	private class ResourceTable32 extends ICUResourceBundle {

		public ICUResourceBundle get(String key) {
			return get(key, null);
		}
		public ICUResourceBundle get(String key, Hashtable table) {
			int offset = RES_GET_OFFSET(resource);
			// offset+0 contains number of entries
			// offset+1 contains the keyOffset  
			int currentOffset = (offset) + getIntOffset(1);
			//int keyOffset = rawData.getChar(currentOffset);
			/* do a binary search for the key */
			int foundOffset = findKey(rawData, size, currentOffset, TABLE32,
					key);
			if (foundOffset == -1) {
				throw new MissingResourceException(
						"Could not find resource ",
						ICUResourceBundleReader.getFullName(baseName, localeID),
						key);
			}
			currentOffset += getIntOffset(size) + getIntOffset(foundOffset);
			long resource = (UNSIGNED_INT_MASK) & rawData.getInt(currentOffset);
			return createBundleObject(key, resource, table);
		}
		public ICUResourceBundle get(int index) {
			return get(index, null);
		}
		public ICUResourceBundle get(int index, Hashtable table) {
			if (index > size) {
				throw new IndexOutOfBoundsException();
			}
			int offset = RES_GET_OFFSET(resource);
			// offset+0 contains number of entries
			// offset+1 contains the keyOffset  
			int currentOffset = (offset) + getIntOffset(1)
					+ getIntOffset(index);
			String itemKey = getTableKey(rawData, size, currentOffset, 0,
					TABLE32);
			currentOffset += getIntOffset(size);
			long resource = (UNSIGNED_INT_MASK) & rawData.getInt(currentOffset);
			return createBundleObject(itemKey, resource, table);
		}
        private int countItems(){
            int offset = RES_GET_OFFSET(resource);
            int value = rawData.getInt(offset);
            return value;   
        }
		private ResourceTable32(String key, long resource, boolean isTopLevel) {
			this.resource = resource;
			this.key = key;
			this.type = TABLE;//Mask the table32's real type
			this.isTopLevel = isTopLevel;
			this.size = countItems();
            this.baseName = baseName();
            this.localeID = localeID();
            this.parent  = parent();;
		}
		private ResourceTable32(String key, long resource) {
			this(key, resource, false);
		}
	}
	private class ResourceString extends ICUResourceBundle {
        private String value;
        public String getString(){
            return value;
        }
		private ResourceString(String key, long resource) {
			value = getStringValue(resource);
			this.key = key;
		    this.resource = resource;
			this.type = RES_GET_TYPE(resource);
            this.baseName = baseName();
            this.localeID = localeID();
            this.parent  = parent();
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
		private ResourceInt(String key, long resource) {
			this.key = key;
			this.resource = resource;
            this.type = RES_GET_TYPE(resource);
            this.baseName = baseName();
            this.localeID = localeID();
            this.parent  = parent();
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
		protected ICUResourceBundle handleGet(String index) {
			return handleGet(index, null);
		}
		protected ICUResourceBundle handleGet(String index, Hashtable table) {
			int val = getIndex(index);
			if (val > -1) {
				return handleGet(val, table);
			}
			throw new UResourceTypeMismatchException("");
		}
		protected ICUResourceBundle handleGet(int index) {
			return handleGet(index, null);
		}
		protected ICUResourceBundle handleGet(int index, Hashtable table) {
			if (index > size) {
				throw new IndexOutOfBoundsException();
			}
			int offset = RES_GET_OFFSET(resource);
			int itemOffset = offset + getIntOffset(index + 1);
			long itemResource = (UNSIGNED_INT_MASK)
					& rawData.getInt(itemOffset);
			return createBundleObject( null, itemResource, table);
		}
        private int countItems(){
            int offset = RES_GET_OFFSET(resource);
            int value = rawData.getInt(offset);
            return value;   
        }
		private ResourceArray(String key, long resource) {
			this.resource = resource;
			this.key = key;
			this.type = RES_GET_TYPE(resource);
			this.size = countItems();
            this.baseName = baseName();
            this.localeID = localeID();
            this.parent  = parent();
            
		}
	}
	private class ResourceBinary extends ICUResourceBundle {
		private ByteBuffer value;
		public ByteBuffer getBinary() {
			value.rewind();
			return value;
		}
		private ByteBuffer getValue() {
			int offset = RES_GET_OFFSET(resource);
			int length = rawData.getInt(offset);
			int byteOffset = offset + getIntOffset(1);
			ByteBuffer val = ByteBuffer.allocate(length);
			for (int i = 0; i < length; i++) {
				val.put(rawData.get(byteOffset + i));
			}
			return val;
		}
		public ResourceBinary(String key, long resource) {
			this.resource = resource;
			this.key = key;
			this.type = RES_GET_TYPE(resource);
            this.baseName = baseName();
            this.localeID = localeID();
            this.parent  = parent();
			value = getValue();
		}
	}
	private class ResourceIntVector extends ICUResourceBundle {
		private int[] value;
		public int[] getIntVector() {
			return value;
		}
		private int[] getValue() {
			int offset = RES_GET_OFFSET(resource);
			int length = rawData.getInt(offset);
			int intOffset = offset + getIntOffset(1);
			int[] val = new int[length];
			for (int i = 0; i < length; i++) {
				val[i] = rawData.getInt(intOffset + getIntOffset(i));
			}
			return val;
		}
		public ResourceIntVector(String key, long resource) {
			this.key = key;
			this.resource = resource;
			this.size = 1;
			this.type = RES_GET_TYPE(resource);
			value = getValue();
            this.baseName = baseName();
            this.localeID = localeID();
            this.parent  = parent();
		}
	}
	private String getStringValue(long resource) {
		int offset = RES_GET_OFFSET(resource);
		int length = rawData.getInt(offset);
		int stringOffset = offset + getIntOffset(1);
		StringBuffer val = new StringBuffer();
		for (int i = 0; i < length; i++) {
			val.append(rawData.getChar(stringOffset + getCharOffset(i)));
		}
		return val.toString();
	}
	private static final char RES_PATH_SEP_CHAR = '/';
	private static final String ICUDATA = "ICUDATA";
	private static final int getIndex(String s) {
		if (s.length() == 1) {
			char c = s.charAt(0);
			if (Character.isDigit(c)) {
				return Integer.valueOf(s).intValue();
			}
		}
		return -1;
	}
	private ICUResourceBundle findResource(String key, long resource, Hashtable table) {
		String locale = null, keyPath = null;
		String bundleName;
		String resPath = getStringValue(resource);
		if (table == null) {
			table = new Hashtable();
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
		if (locale == null) {
			bundle = (ICUResourceBundle) getBundleInstance(bundleName, "",
					ICU_DATA_CLASS_LOADER, false);
		} else {
			bundle = (ICUResourceBundle) getBundleInstance(bundleName, locale,
					ICU_DATA_CLASS_LOADER, false);
		}
		ICUResourceBundle sub = null;
		if (keyPath != null) {
			StringTokenizer st = new StringTokenizer(keyPath, "/");
			ICUResourceBundle current = bundle;
			while (st.hasMoreTokens()) {
				String subKey = st.nextToken();
				sub = current.handleGet(subKey, table);
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
		if (sub == null) {
			throw new MissingResourceException(localeID, baseName, key);
		}
		sub.resPath = resPath;
		return sub;
	}
}