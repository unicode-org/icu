
package com.ibm.icu.impl;

import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.Vector;

import com.ibm.icu.util.StringTokenizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;
import com.ibm.icu.util.UResourceTypeMismatchException;
import com.ibm.icu.util.VersionInfo;

public class ICUResourceBundle extends UResourceBundle{    
    /** 
     * Resource type constant for "no resource". 
     * @draft ICU 3.0 
     */
    public static final int NONE=-1;

    /** 
     * Resource type constant for strings. 
     * @draft ICU 3.0 
     */
    public static final int STRING=0;

    /** 
     * Resource type constant for binary data. 
     * @draft ICU 3.0 
     */
    public static final int BINARY=1;

    /** 
     * Resource type constant for tables of key-value pairs. 
     * @draft ICU 3.0 
     */
    public static final int TABLE=2;

    /**
     * Resource type constant for aliases;
     * internally stores a string which identifies the actual resource
     * storing the data (can be in a different resource bundle).
     * Resolved internally before delivering the actual resource through the API.
     * @draft ICU 3.0
     * @internal
     */
    private static final int ALIAS=3;

    /**
     * Internal use only.
     * Alternative resource type constant for tables of key-value pairs.
     * Never returned by getType().
     * @internal
     * @draft ICU 3.0
     */
    private static final int TABLE32=4;

    /**
     * Resource type constant for a single 28-bit integer, interpreted as
     * signed or unsigned by the getInt() function.
     * @see #getInt
     * @draft ICU 3.0
     */
    public static final int INT=7;

    /** 
     * Resource type constant for arrays of resources. 
     * @draft ICU 3.0 
     */
    public static final int ARRAY=8;

    /**
     * Resource type constant for vectors of 32-bit integers.
     * @see #getIntVector
     * @draft ICU 3.0
     */
    public static final int INT_VECTOR=14;

    private static final String ICU_RESOURCE_INDEX = "res_index";
    
    /**
     * Return the version number associated with this UResourceBundle as an 
     * VersionInfo object.
     * @return VersionInfo object containing the version of the bundle
     * @draft ICU 3.0
     */
    public VersionInfo getVersion(){
        return null;
    }
    
    /**
     * Returns a string from a string resource type
     *
     * @return a string
     * @see #getBinary
     * @see #getIntVector
     * @see #getInt
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.0
     */
    public String getString(){
        ICUResourceBundle sub = (ICUResourceBundle)handleGetObject();
        if(sub.getType()==STRING){
            return sub.getString();   
        }
        throw new UResourceTypeMismatchException("");
    }
    
    /**
     * Returns a string from a string resource type
     * @param key The key whose values needs to be fetched
     * @return a string
     * @see #getBinary
     * @see #getIntVector
     * @see #getInt
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.0
     */  
  //  public String getString(String key) {
  //      throw new UResourceTypeMismatchException("");
  //  }

    
    /**
     * Returns a binary data from a binary resource. 
     *
     * @return a pointer to a chuck of unsigned bytes which live in a memory mapped/DLL file.
     * @see #getString
     * @see #getIntVector
     * @see #getInt
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.0
     */
    public ByteBuffer getBinary(){
        throw new UResourceTypeMismatchException("");   
    }
        
    /**
     * Returns a 32 bit integer array from a resource. 
     *
     * @return a pointer to a chunk of unsigned bytes which live in a memory mapped/DLL file.
     * @see #getBinary
     * @see #getString
     * @see #getInt
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.0
     */
    public int[] getIntVector(){
        throw new UResourceTypeMismatchException("");
    }
    
    /**
     * Returns a signed integer from a resource.
     * 
     * @return an integer value
     * @see #getIntVector
     * @see #getBinary
     * @see #getString
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @stable ICU 2.0
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
     * @see #getBinary
     * @see #getString
     * @throws MissingResourceException
     * @throws UResourceTypeMismatchException
     * @stable ICU 2.0
     */
    public int getUInt() {
        throw new UResourceTypeMismatchException("");
    }
    /**
     * Returns the size of a resource. Size for scalar types is always 1, 
     * and for vector/table types is the number of child resources.
     * <br><b><font color='red'>Warning: </font></b> Integer array is treated as a scalar type. There are no 
     *          APIs to access individual members of an integer array. It
     *          is always returned as a whole.
     * @return number of resources in a given resource.
     * @draft ICU 3.0
     */
    public int getSize(){
        return size;
    }
    
    /**
     * Returns the type of a resource. 
     * Available types are {@link #INT INT}, {@link #ARRAY ARRAY}, 
     * {@link #BINARY BINARY}, {@link #INT_VECTOR INT_VECTOR}, 
     * {@link #STRING STRING}, {@link #TABLE TABLE}.
     *
     * @return type of the given resource.
     * @draft ICU 3.0
     */
    public int getType(){
        return type;
    }
    
    /**
     * Returns the key associated with a given resource. Not all the resources have a key - only 
     * those that are members of a table.
     * @return a key associated to this resource, or NULL if it doesn't have a key
     * @draft ICU 3.0
     */
    public String getKey(){
        return key;
    }

        
    /**
     * Returns the iterator which iterates over this
     * resource bundle
     * @draft ICU 3.0
     */
    public UResourceBundleIterator getIterator(){
        return new UResourceBundleIterator(this);   
    }

    
    /**
     * Returns the resource in a given resource at the specified index.
     *
     * @param index             an index to the wanted resource.
     * @return                  the sub resource UResourceBundle object
     * @throws IndexOutOfBoundsException
     * @draft ICU 3.0
     */
    public ICUResourceBundle get(int index){
       // return handleGetObject(index);
        ICUResourceBundle obj =  handleGet(index);
        if (obj == null) {
            if (parent != null) {
                obj = ((ICUResourceBundle)parent).handleGet(index);
            }
            if (obj == null)
                throw new MissingResourceException("Can't find resource for bundle "
                                                   +this.getClass().getName()
                                                   +", key "+key,
                                                   this.getClass().getName(),
                                                   key);
        }
        return obj;
    }
    protected Object handleGetObject(String key){
        ICUResourceBundle obj =  handleGet(key);
        /*
        if (obj == null) {
            if (parent != null) {
                obj = ((ICUResourceBundle)parent).handleObject(key);
            }
            if (obj == null)
                throw new MissingResourceException("Can't find resource for bundle "
                                                   +this.getClass().getName()
                                                   +", key "+key,
                                                   this.getClass().getName(),
                                                   key);
        }*/
//        if(obj.getType()==STRING){
//            return obj.getString();   
//        }
        return obj;        
    }
    protected ICUResourceBundle handleGet(int index){
        return null;
    }
    protected ICUResourceBundle handleGet(String key){
        return null;   
    }
    
    // abstract UResourceBundle handleGetInt(int index);
    
    /**
     * Returns a resource in a given resource that has a given key.
     *
     * @param key               a key associated with the wanted resource
     * @return                  a resource bundle object representing rhe resource
     * @throws MissingResourceException
     * @draft ICU 3.0
     */
    public ICUResourceBundle get(String key){
        //throw new UResourceTypeMismatchException("");
        return (ICUResourceBundle)handleGetObject(key);
    }
    
    /**
     * Returns the string in a given resource at the specified index.
     *
     * @param indexS            an index to the wanted string.
     * @return                  a string which lives in the resource.
     * @throws IndexOutOfBoundsException
     * @throws UResourceTypeMismatchException
     * @draft ICU 3.0
     */
    public String getString(int index){
        ICUResourceBundle temp = get(index);
        if(temp.type==STRING){
            return temp.getString();   
        }
        throw new UResourceTypeMismatchException("");
    }
    
    /**
     * Returns the parent bundle of this bundle
     * @return UResourceBundle the parent of this bundle. Returns null if none
     * @draft ICU 3.0
     */
    public UResourceBundle getParent(){
        return (UResourceBundle)parent;   
    }
    
    /**
     * Get the set of Locales installed in the specified bundles.
     * @return the list of available locales
     * @draft ICU 3.0
     */
    public static final ULocale[] getAvailableLocales(String baseName){
        return getAvailableLocales(baseName, ICU_DATA_CLASS_LOADER);
    }
    
    
    /**
     * Get the set of Locales installed in the specified bundles.
     * @return the list of available locales
     * @draft ICU 3.0
     */
    public static final ULocale[] getAvailableLocales(String baseName, ClassLoader root){
        ICUResourceBundle bundle = (ICUResourceBundle) instantiateBundle(baseName, ICU_RESOURCE_INDEX, root, true);
        bundle = bundle.get(INSTALLED_LOCALES);
        int length = bundle.getSize();
        int i = 0;
        ULocale[] locales = new ULocale[length];
        UResourceBundleIterator iter = bundle.getIterator();
        iter.reset();
        while(iter.hasNext()){
            locales[i++] = new ULocale(iter.nextString());   
        } 
        bundle = null;
        return locales;
    }
    
    /**
     * Returns a functionally equivalent locale, considering keywords as well, for the specified keyword.
     * @param BASE resource specifier
     * @param resName top level resource to consider (such as "collations")
     * @param keyword a particular keyword to consider (such as "collation" )
     * @param locid The requested locale 
     * @param fillinIsAvailable If non-null, 1-element array of fillin parameter that indicates whether the 
     * requested locale was available. The locale is defined as 'available' if it physically 
     * exists within the specified tree.
     * @return the locale
     * @internal ICU 3.0
     */
    public static final ULocale  getFunctionalEquivalent(String baseName, String resName, String keyword, String locID, boolean fillinIsAvailable[]){
        return null;
    }
    /**
     * 
     * @param baseName
     * @param resName
     * @param keyword
     * @param locale
     * @param fillinIsAvailable
     * @return
     * @internal ICU 3.0
     */
    public static final ULocale getFunctionalEquivalent(String baseName, String resName, String keyword, ULocale locale, boolean fillinIsAvailable[]) {
        return getFunctionalEquivalent(baseName, resName, keyword, locale.toString(), fillinIsAvailable);
    }

    /**
     * Given a tree path and keyword, return a string enumeration of all possible values for that keyword.
     * @param BASE resource specifier
     * @param keyword a particular keyword to consider, must match a top level resource name 
     * within the tree. (i.e. "collations")
     * @internal ICU 3.0
     */
    public static final String[] getKeywordValues(String baseName, String keyword){
        return null;
    }

    protected String  localeID;
    protected String baseName;
    protected int type = NONE;
    protected String key;
    protected int size = NONE;
    protected String resPath;
    protected boolean isTopLevel;
    
    protected ICUResourceBundleReader reader;
    //protected byte[] version;
    protected ByteBuffer rawData;
    protected long resource;
    protected long rootResource;
    
    protected static final long UNSIGNED_INT_MASK = 0xffffffffL;

    public static ICUResourceBundle createBundle(String baseName, String localeID, ClassLoader root){

        ICUResourceBundleReader reader = new ICUResourceBundleReader(baseName, localeID, root);
        ByteBuffer rawData = reader.getData();
        long rootResource = (UNSIGNED_INT_MASK)& rawData.getInt(0);
        int type = RES_GET_TYPE(rootResource);
         if(type==TABLE){
            return  ResourceTable.getTableInstance(rawData, baseName, localeID, null, rootResource, rootResource, true);   
        }else if(type == TABLE32){
            return new ResourceTable32(rawData, baseName, localeID, null, rootResource, rootResource, true); 
        }else{
             throw new RuntimeException("Invalid format error"); 
        }   
        
    }
    
    private  static final long RES_BOGUS = 0xffffffff;

    protected static final int RES_GET_TYPE(long res){
       return (int)((res)>>28L);
    }
    protected static final int RES_GET_OFFSET(long res){
        return (int)((res&0x0fffffff)*4);
    }

    /* get signed and unsigned integer values directly from the Resource handle */
    protected static final int RES_GET_INT(long res) {
        return (((int)((res)<<4L))>>4L);
    }
    protected static final long RES_GET_UINT(long res){
        long t = ((res)&0x0fffffffL);
        return t;
    }
    
    protected static int countItems(final ByteBuffer rawData, final long res) {
        if(res!=RES_BOGUS) {
            switch(RES_GET_TYPE(res)) {
                case STRING:
                case BINARY:
                case ALIAS:
                case INT:
                case INT_VECTOR:
                    return 1;
                case ARRAY:
                case TABLE32: {
                    int offset = RES_GET_OFFSET(res);
                    int value  = rawData.getInt(offset);
                    return value;
                }
                case TABLE: {
                    int offset = RES_GET_OFFSET(res);
                    int value  =  rawData.getChar(offset);
                    return value;
                }
                default:
                    break;
            }
        } 
        return 0;
    }
   
    protected Object handleGetObject(){
        return this;   
    }
    public Enumeration getKeys(){
        Vector keys = new Vector();
        ICUResourceBundle item =null;
        for(int i=0; i<size; i++){
            item = get(i);
            keys.add(item.getKey());
        }
        return keys.elements();
    }
    protected  ICUResourceBundle handleGet(String key, Hashtable table){
        throw new UResourceTypeMismatchException("");   
    }
    protected  ICUResourceBundle handleGet(int index, Hashtable table){
        throw new UResourceTypeMismatchException("");   
    }
    protected ICUResourceBundle(ByteBuffer rawData, String baseName, String localeID, 
                                String key, long resource,long rootResource, int type, 
                                boolean isTopLevel ){ 
        this.rawData = rawData;
        this.key = key;
        this.resource = resource;
        this.rootResource = rootResource;
        this.type = type;
        this.isTopLevel = false;
        this.size = countItems(rawData, resource);
        this.baseName = baseName;
        this.localeID = localeID;
        //this.locale = new ULocale(localeID).toLocale();
       
    }
    
    public ULocale getULocale(){
        return new ULocale(localeID);   
    }


    private static StringBuffer RES_GET_KEY(ByteBuffer rawData, int keyOffset){
        char ch = 0xFFFF; //sentinel
        StringBuffer key = new StringBuffer();
        while((ch=(char)rawData.get(keyOffset))!= 0){
             key.append(ch);  
             keyOffset++;
        }
        return key;
    }
    private static final int getIntOffset(int offset){
        return (offset*4);
    }
    private static final int getCharOffset(int offset){
        return (offset*2);   
    }
  
    private static final ICUResourceBundle createBundleObject(ByteBuffer rawData, String baseName, String localeID, 
                                                              String key, long resource, long rootResource,
                                                              Hashtable table){
        if(resource!=RES_BOGUS) {
            switch(RES_GET_TYPE(resource)) {
                case STRING:{
                    return new ResourceString(rawData, baseName, localeID, key, resource, rootResource);   
                }
                case BINARY:{
                    return new ResourceBinary(rawData, baseName, localeID, key, resource, rootResource);
                }
                case ALIAS:{
                    return findResource(rawData, baseName, localeID, key, resource, table);   
                }
                case INT:{
                    return new ResourceInt(rawData, baseName, localeID, key, resource, rootResource);   
                }
                case INT_VECTOR:{
                    return new ResourceIntVector(rawData, baseName, localeID, key, resource, rootResource);   
                }
                case ARRAY:{
                    return new ResourceArray(rawData, baseName, localeID, key, resource, rootResource);   
                }
                case TABLE32:{
                    return new ResourceTable32(rawData, baseName, localeID, key, resource, rootResource, false);  
                }
                case TABLE: {
                    return new ResourceTable(rawData, baseName, localeID, key, resource, rootResource, false);   
                }
                default:
                    throw new InternalError("The resource type is unknown");
            }
        } 
        return null; 
    }
    private static String getTableKey(ByteBuffer rawData, int size, int currentOffset, int index, int type){
        switch(type){
            case TABLE32:{
                int charOffset = currentOffset+getIntOffset(index);
                int keyOffset =  rawData.getInt(charOffset);
                return RES_GET_KEY(rawData,keyOffset).toString();
            }
            case TABLE:
            default:{
                int charOffset = currentOffset+getCharOffset(index);
                int keyOffset =  rawData.getChar(charOffset);
                return RES_GET_KEY(rawData,keyOffset).toString();
            }
        }
    }
    private static int findKey(ByteBuffer rawData, int size,
                                int currentOffset, int type,
                                String target){
		int mid = 0, start = 0, limit = size, rc;
		int lastMid = -1;
		//int myCharOffset = 0, keyOffset = 0;
		for (;;) {
			mid = ((start + limit) / 2);
			if (lastMid == mid) { /* Have we moved? */
				break; /* We haven't moved, and it wasn't found. */
			}
			lastMid = mid;

            String comp = getTableKey(rawData, size,currentOffset,mid,type);
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
    
    private static class ResourceTable extends ICUResourceBundle{

        public ICUResourceBundle handleGet(String key){
            return handleGet(key, null);   
        }
        protected ICUResourceBundle handleGet(String key, Hashtable table){
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset  
            int currentOffset = (offset)+getCharOffset(1);
            //int keyOffset = rawData.getChar(currentOffset);

            /* do a binary search for the key */
            int foundOffset = findKey(rawData, size, currentOffset, type, key);
            if(foundOffset == -1){
                //throw new MissingResourceException(ICUResourceBundleReader.getFullName(baseName, localeID),
                //                                    localeID,
                //                                    key);
                return null;
            }
            currentOffset += getCharOffset(size+(~size&1))+getIntOffset(foundOffset);
            long resource = (UNSIGNED_INT_MASK) & rawData.getInt(currentOffset);
            return createBundleObject(rawData, baseName, localeID, key, resource, rootResource, table);
            
        }
        public ICUResourceBundle handleGet(int index){
            return handleGet(index, null);
        }
        public ICUResourceBundle handleGet(int index, Hashtable table){
            if(index>size){
                throw new IndexOutOfBoundsException();   
            }
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset  
            int currentOffset = (offset)+getCharOffset(1);
            String itemKey = getTableKey(rawData, size, currentOffset, index, type);
            
            currentOffset +=  getCharOffset(size+(~size&1))+ getIntOffset(index) ;
            long resource = (UNSIGNED_INT_MASK) & rawData.getInt(currentOffset);

            return createBundleObject(rawData, baseName, localeID, itemKey, resource, rootResource, table);
        }

        private ResourceTable(ByteBuffer rawData, String baseName, String localeID, String key, 
                                long resource, long rootResource, boolean isRootResource ){
            super(rawData, baseName, localeID, key, resource, rootResource, STRING, isRootResource);
        }
        
        private static ResourceTable getTableInstance(ByteBuffer rawData, String baseName, String localeID, String key, 
                                                      long resource, long rootResource, boolean isRootResource ){
            
            // kludge: %%ALIAS is such a hack! I can understand the
            // ICU4C legacy .. do we need to port it?
            ResourceTable table = new ResourceTable(rawData, baseName, localeID, key, resource, rootResource, isRootResource);
            //return table;
            
            ICUResourceBundle b = table.handleGet(0);
            String itemKey = b.getKey();
            if(itemKey.equals("%%ALIAS")){
                String locale = b.getString();
                ICUResourceBundle actual =  (ICUResourceBundle)UResourceBundle.getBundleInstance(baseName, locale);
                return (ResourceTable)actual;
            }  else {
                return table;
            }
            
        }
        
    }

    private static class ResourceTable32 extends ICUResourceBundle{

        public Object handleGetObject(String key){
            return get(key);   
        }
        public ICUResourceBundle get(String key){
            return get(key, null);   
        }
        public ICUResourceBundle get(String key, Hashtable table){
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset  
            int currentOffset = (offset)+getIntOffset(1);
            //int keyOffset = rawData.getChar(currentOffset);

            /* do a binary search for the key */
            int foundOffset = findKey(rawData, size, currentOffset, TABLE32, key);
            if(foundOffset == -1){
                throw new MissingResourceException("Could not find resource ",
                                                    ICUResourceBundleReader.getFullName(baseName, localeID),
                                                    key);
            }
            currentOffset += getIntOffset(size)+getIntOffset(foundOffset);
            long resource = (UNSIGNED_INT_MASK) & rawData.getInt(currentOffset);
            
            return createBundleObject(rawData, baseName, localeID, key, resource, rootResource, table);
        }
        public ICUResourceBundle get(int index){
            return get(index, null);   
        }
        public ICUResourceBundle get(int index, Hashtable table){
            if(index>size){
                throw new IndexOutOfBoundsException();   
            }
            int offset = RES_GET_OFFSET(resource);
            // offset+0 contains number of entries
            // offset+1 contains the keyOffset  
            int currentOffset = (offset)+getIntOffset(1)+ getIntOffset(index);
            String itemKey = getTableKey(rawData, size, currentOffset, 0, TABLE32);
            
            currentOffset +=  getIntOffset(size) ;
            long resource = (UNSIGNED_INT_MASK) & rawData.getInt(currentOffset);
            
            return createBundleObject(rawData, baseName, localeID, itemKey, resource, rootResource, table);
        }
        private ResourceTable32(ByteBuffer rawData, String baseName, String localeID,String key, 
                                long resource, long rootResource, boolean isRootResource ){
            super(rawData, key, baseName, localeID, resource, rootResource, TABLE, isRootResource);
        }
    }
    
    private static class ResourceString extends ICUResourceBundle {
        private String value;
        public String getString(){
            return value;
        }
        private ResourceString(ByteBuffer rawData, String baseName, String localeID, 
                               String key, long resource, long rootResource ){
            super(rawData, baseName, localeID, key, resource, rootResource, STRING, false);
            value = getStringValue(rawData, resource);
        }
    }
    private static class ResourceInt extends ICUResourceBundle{
        public int getInt(){
            return RES_GET_INT(resource);
        }
        public int getUInt(){
            long ret = RES_GET_UINT(resource);
            return (int)ret;   
        }

        private ResourceInt(ByteBuffer rawData, String baseName, String localeID, String key, 
                            long resource, long rootResource){
            super(rawData, baseName, localeID, key, resource, rootResource, INT, false);
        }
    }
    private static class ResourceArray extends ICUResourceBundle{
        protected ICUResourceBundle handleGet(String index){
            return handleGet(index, null);
        }
        protected ICUResourceBundle handleGet(String index, Hashtable table){
            int val = getIndex(index);
            if(val>-1){
                return handleGet(val);
            }
            throw new UResourceTypeMismatchException("");
        }
        protected ICUResourceBundle handleGet(int index){
            return handleGet(index, null);   
        }
        protected ICUResourceBundle handleGet(int index, Hashtable table){
            if(index > size){
                throw new IndexOutOfBoundsException();   
            }
            int offset = RES_GET_OFFSET(resource);
            int itemOffset = offset+getIntOffset(index+1);
            long itemResource = (UNSIGNED_INT_MASK)&rawData.getInt(itemOffset);
            return createBundleObject(rawData, baseName, localeID, null,itemResource, rootResource, table);   
        }
        private ResourceArray(ByteBuffer rawData, String baseName, String localeID, String key, 
                long resource, long rootResource ){
            super(rawData, baseName, localeID, key, resource, rootResource, ARRAY, false );
        }
    }
    private static class ResourceBinary extends ICUResourceBundle{
        private ByteBuffer value;
        public ByteBuffer getBinary(){
            value.rewind();
            return value;   
        }
        private ByteBuffer getValue(){
            int offset = RES_GET_OFFSET(resource);
            int length = rawData.getInt(offset);
            int byteOffset = offset+getIntOffset(1);
            ByteBuffer val = ByteBuffer.allocate(length);
            for(int i=0; i<length; i++){
                val.put(rawData.get(byteOffset+i));
            }
            return val.asReadOnlyBuffer();
        }
		public ResourceBinary(ByteBuffer rawData, String baseName, String localeID, String key, 
                              long resource, long rootResource ){
            super(rawData, baseName, localeID, key, resource, rootResource, BINARY, false );
            value = getValue();
		}
    }
    private static class ResourceIntVector extends ICUResourceBundle{
        private int[] value;
        public int[] getIntVector(){
            return value;   
        }  
        private int[] getValue(){
            int offset = RES_GET_OFFSET(resource);
            int length = rawData.getInt(offset);
            int intOffset = offset+getIntOffset(1);
            int[] val = new int[length];
            for(int i=0; i<length; i++){
                val[i] = rawData.getInt(intOffset+getIntOffset(i));
            }
            return val;
        }
        public ResourceIntVector(ByteBuffer rawData, String baseName, String localeID, String key, 
                                long resource, long rootResource){
            super(rawData, baseName, localeID, key, resource, rootResource, ARRAY, false );
            value = getValue();
        }
    }
    private static String getStringValue(ByteBuffer rawData, long resource){
        int offset = RES_GET_OFFSET(resource);
        int length = rawData.getInt(offset);
        int stringOffset = offset+getIntOffset(1);
        
        StringBuffer val = new StringBuffer();
        for(int i=0; i<length; i++){
            val.append(rawData.getChar(stringOffset+getCharOffset(i)));   
        }
        return val.toString();
    }
    
    private static final char RES_PATH_SEP_CHAR ='/';
    private static final String ICUDATA = "ICUDATA";
    
    private static int getIndex(String s){
        if(s.length()==1){
           char c = s.charAt(0);
           if(Character.isDigit(c)){
             return Integer.valueOf(s).intValue();
           }
        }
        return -1;
    }
    
    private static ICUResourceBundle findResource(ByteBuffer rawData, String baseName, 
                                                   String localeID, String key, long resource,
                                                   Hashtable table){
        String locale=null, keyPath=null;
        String bundleName;
        String resPath = getStringValue(rawData, resource);
        if(table==null){
            table = new Hashtable();   
        }
        if(table.get(resPath)!= null){
            throw new IllegalArgumentException("Circular references in the resource bundles");   
        }
        table.put(resPath,"");
        if(resPath.indexOf(RES_PATH_SEP_CHAR)==0){
            int i =resPath.indexOf(RES_PATH_SEP_CHAR,1);
            int j =resPath.indexOf(RES_PATH_SEP_CHAR,i+1);
            bundleName=resPath.substring(1,i);
            locale=resPath.substring(i+1);
            if(j!=-1){
                locale=resPath.substring(i+1,j);
                keyPath=resPath.substring(j+1,resPath.length());
            }
            //there is a path included
            if(bundleName.equals(ICUDATA)){
                bundleName = ICU_BASE_NAME;
            }

        }else{
            //no path start with locale
            int i =resPath.indexOf(RES_PATH_SEP_CHAR);
            keyPath=resPath.substring(i+1);

            if(i!=-1){
                locale = resPath.substring(0,i);
            }else{
                locale=keyPath;
                keyPath = keyPath.substring(i, keyPath.length());
            }
            bundleName = baseName;

        }
        
        ICUResourceBundle bundle = null;
        if(locale==null){
            bundle = (ICUResourceBundle)getBundleInstance(bundleName, "", ICU_DATA_CLASS_LOADER, true);
        }else{
            bundle = (ICUResourceBundle)getBundleInstance(bundleName, locale, ICU_DATA_CLASS_LOADER, true);
        }
        
        
        ICUResourceBundle sub = null;
        if(keyPath!=null){
            StringTokenizer st = new StringTokenizer(keyPath, "/");
            ICUResourceBundle current = bundle;
            while (st.hasMoreTokens()) {
                String subKey = st.nextToken();
                sub = current.handleGet(subKey, table);
                // cannot find bundle about the subKey
                // so try the index
                if(sub==null){
                    int val = getIndex(subKey);
                    if(val>-1){
                        sub = current.get(val);
                    }
                }
                current = sub;
            }
        }else{

            // if the sub resource is not found
            // try fetching the sub resource with
            // the key of this alias resource
            sub = bundle.get(key);   
        }
        if(sub == null){
            throw new MissingResourceException(localeID, baseName, key);   
        }
        sub.resPath = resPath;
        return sub;
    }
}
