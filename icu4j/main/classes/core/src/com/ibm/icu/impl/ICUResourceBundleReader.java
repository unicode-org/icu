/*
 *******************************************************************************
 * Copyright (C) 2004-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.ibm.icu.util.UResourceBundle;  // For resource type constants.
import com.ibm.icu.util.VersionInfo;

/**
 * This class reads the *.res resource bundle format
 *
 * (For the latest version of the file format documentation see
 * ICU4C's source/common/uresdata.h file.)
 *
 * File format for .res resource bundle files (formatVersion=2, ICU 4.4)
 *
 * New in formatVersion 2 compared with 1.3: -------------
 *
 * Three new resource types -- String-v2, Table16 and Array16 -- have their
 * values stored in a new array of 16-bit units between the table key strings
 * and the start of the other resources.
 *
 * genrb eliminates duplicates among Unicode string-v2 values.
 * Multiple Unicode strings may use the same offset and string data,
 * or a short string may point to the suffix of a longer string. ("Suffix sharing")
 * For example, one string "abc" may be reused for another string "bc" by pointing
 * to the second character. (Short strings-v2 are NUL-terminated
 * and not preceded by an explicit length value.)
 *
 * It is allowed for all resource types to share values.
 * The swapper code (ures_swap()) has been modified so that it swaps each item
 * exactly once.
 *
 * A resource bundle may use a special pool bundle. Some or all of the table key strings
 * of the using-bundle are omitted, and the key string offsets for such key strings refer
 * to offsets in the pool bundle.
 * The using-bundle's and the pool-bundle's indexes[URES_INDEX_POOL_CHECKSUM] values
 * must match.
 * Two bits in indexes[URES_INDEX_ATTRIBUTES] indicate whether a resource bundle
 * is or uses a pool bundle.
 *
 * Table key strings must be compared in ASCII order, even if they are not
 * stored in ASCII.
 *
 * New in formatVersion 1.3 compared with 1.2: -------------
 *
 * genrb eliminates duplicates among key strings.
 * Multiple table items may share one key string, or one item may point
 * to the suffix of another's key string. ("Suffix sharing")
 * For example, one key "abc" may be reused for another key "bc" by pointing
 * to the second character. (Key strings are NUL-terminated.)
 *
 * -------------
 *
 * An ICU4C resource bundle file (.res) is a binary, memory-mappable file
 * with nested, hierarchical data structures.
 * It physically contains the following:
 *
 *   Resource root; -- 32-bit Resource item, root item for this bundle's tree;
 *                     currently, the root item must be a table or table32 resource item
 *   int32_t indexes[indexes[0]]; -- array of indexes for friendly
 *                                   reading and swapping; see URES_INDEX_* above
 *                                   new in formatVersion 1.1 (ICU 2.8)
 *   char keys[]; -- characters for key strings
 *                   (formatVersion 1.0: up to 65k of characters; 1.1: <2G)
 *                   (minus the space for root and indexes[]),
 *                   which consist of invariant characters (ASCII/EBCDIC) and are NUL-terminated;
 *                   padded to multiple of 4 bytes for 4-alignment of the following data
 *   uint16_t 16BitUnits[]; -- resources that are stored entirely as sequences of 16-bit units
 *                             (new in formatVersion 2/ICU 4.4)
 *                             data is indexed by the offset values in 16-bit resource types,
 *                             with offset 0 pointing to the beginning of this array;
 *                             there is a 0 at offset 0, for empty resources;
 *                             padded to multiple of 4 bytes for 4-alignment of the following data
 *   data; -- data directly and indirectly indexed by the root item;
 *            the structure is determined by walking the tree
 *
 * Each resource bundle item has a 32-bit Resource handle (see typedef above)
 * which contains the item type number in its upper 4 bits (31..28) and either
 * an offset or a direct value in its lower 28 bits (27..0).
 * The order of items is undefined and only determined by walking the tree.
 * Leaves of the tree may be stored first or last or anywhere in between,
 * and it is in theory possible to have unreferenced holes in the file.
 *
 * 16-bit-unit values:
 * Starting with formatVersion 2/ICU 4.4, some resources are stored in a special
 * array of 16-bit units. Each resource value is a sequence of 16-bit units,
 * with no per-resource padding to a 4-byte boundary.
 * 16-bit container types (Table16 and Array16) contain Resource16 values
 * which are offsets to String-v2 resources in the same 16-bit-units array.
 *
 * Direct values:
 * - Empty Unicode strings have an offset value of 0 in the Resource handle itself.
 * - Starting with formatVersion 2/ICU 4.4, an offset value of 0 for
 *   _any_ resource type indicates an empty value.
 * - Integer values are 28-bit values stored in the Resource handle itself;
 *   the interpretation of unsigned vs. signed integers is up to the application.
 *
 * All other types and values use 28-bit offsets to point to the item's data.
 * The offset is an index to the first 32-bit word of the value, relative to the
 * start of the resource data (i.e., the root item handle is at offset 0).
 * To get byte offsets, the offset is multiplied by 4 (or shifted left by 2 bits).
 * All resource item values are 4-aligned.
 *
 * New in formatVersion 2/ICU 4.4: Some types use offsets into the 16-bit-units array,
 * indexing 16-bit units in that array.
 *
 * The structures (memory layouts) for the values for each item type are listed
 * in the table below.
 *
 * Nested, hierarchical structures: -------------
 *
 * Table items contain key-value pairs where the keys are offsets to char * key strings.
 * The values of these pairs are either Resource handles or
 * offsets into the 16-bit-units array, depending on the table type.
 *
 * Array items are simple vectors of Resource handles,
 * or of offsets into the 16-bit-units array, depending on the array type.
 *
 * Table key string offsets: -------
 *
 * Key string offsets are relative to the start of the resource data (of the root handle),
 * i.e., the first string has an offset of 4+sizeof(indexes).
 * (After the 4-byte root handle and after the indexes array.)
 *
 * If the resource bundle uses a pool bundle, then some key strings are stored
 * in the pool bundle rather than in the local bundle itself.
 * - In a Table or Table16, the 16-bit key string offset is local if it is
 *   less than indexes[URES_INDEX_KEYS_TOP]<<2.
 *   Otherwise, subtract indexes[URES_INDEX_KEYS_TOP]<<2 to get the offset into
 *   the pool bundle key strings.
 * - In a Table32, the 32-bit key string offset is local if it is non-negative.
 *   Otherwise, reset bit 31 to get the pool key string offset.
 *
 * Unlike the local offset, the pool key offset is relative to
 * the start of the key strings, not to the start of the bundle.
 *
 * An alias item is special (and new in ICU 2.4): --------------
 *
 * Its memory layout is just like for a UnicodeString, but at runtime it resolves to
 * another resource bundle's item according to the path in the string.
 * This is used to share items across bundles that are in different lookup/fallback
 * chains (e.g., large collation data among zh_TW and zh_HK).
 * This saves space (for large items) and maintenance effort (less duplication of data).
 *
 * --------------------------------------------------------------------------
 *
 * Resource types:
 *
 * Most resources have their values stored at four-byte offsets from the start
 * of the resource data. These values are at least 4-aligned.
 * Some resource values are stored directly in the offset field of the Resource itself.
 * See UResType in unicode/ures.h for enumeration constants for Resource types.
 *
 * Some resources have their values stored as sequences of 16-bit units,
 * at 2-byte offsets from the start of a contiguous 16-bit-unit array between
 * the table key strings and the other resources. (new in formatVersion 2/ICU 4.4)
 * At offset 0 of that array is a 16-bit zero value for empty 16-bit resources.
 * Resource16 values in Table16 and Array16 are 16-bit offsets to String-v2
 * resources, with the offsets relative to the start of the 16-bit-units array.
 *
 * Type Name            Memory layout of values
 *                      (in parentheses: scalar, non-offset values)
 *
 * 0  Unicode String:   int32_t length, UChar[length], (UChar)0, (padding)
 *                  or  (empty string ("") if offset==0)
 * 1  Binary:           int32_t length, uint8_t[length], (padding)
 *                      - the start of the bytes is 16-aligned -
 * 2  Table:            uint16_t count, uint16_t keyStringOffsets[count], (uint16_t padding), Resource[count]
 * 3  Alias:            (physically same value layout as string, new in ICU 2.4)
 * 4  Table32:          int32_t count, int32_t keyStringOffsets[count], Resource[count]
 *                      (new in formatVersion 1.1/ICU 2.8)
 * 5  Table16:          uint16_t count, uint16_t keyStringOffsets[count], Resource16[count]
 *                      (stored in the 16-bit-units array; new in formatVersion 2/ICU 4.4)
 * 6  Unicode String-v2:UChar[length], (UChar)0; length determined by the first UChar:
 *                      - if first is not a trail surrogate, then the length is implicit
 *                        and u_strlen() needs to be called
 *                      - if first<0xdfef then length=first&0x3ff (and skip first)
 *                      - if first<0xdfff then length=((first-0xdfef)<<16) | second UChar
 *                      - if first==0xdfff then length=((second UChar)<<16) | third UChar
 *                      (stored in the 16-bit-units array; new in formatVersion 2/ICU 4.4)
 * 7  Integer:          (28-bit offset is integer value)
 * 8  Array:            int32_t count, Resource[count]
 * 9  Array16:          uint16_t count, Resource16[count]
 *                      (stored in the 16-bit-units array; new in formatVersion 2/ICU 4.4)
 * 14 Integer Vector:   int32_t length, int32_t[length]
 * 15 Reserved:         This value denotes special purpose resources and is for internal use.
 *
 * Note that there are 3 types with data vector values:
 * - Vectors of 8-bit bytes stored as type Binary.
 * - Vectors of 16-bit words stored as type Unicode String or Unicode String-v2
 *                     (no value restrictions, all values 0..ffff allowed!).
 * - Vectors of 32-bit words stored as type Integer Vector.
 */
public final class ICUResourceBundleReader implements ICUBinary.Authenticate {
    /**
     * File format version that this class understands.
     * "ResB"
     */
    private static final byte DATA_FORMAT_ID[] = {(byte)0x52, (byte)0x65, 
                                                     (byte)0x73, (byte)0x42};

    /* indexes[] value names; indexes are generally 32-bit (Resource) indexes */
    private static final int URES_INDEX_LENGTH           = 0;   /* contains URES_INDEX_TOP==the length of indexes[];
                                                                 * formatVersion==1: all bits contain the length of indexes[]
                                                                 *   but the length is much less than 0xff;
                                                                 * formatVersion>1:
                                                                 *   only bits  7..0 contain the length of indexes[],
                                                                 *        bits 31..8 are reserved and set to 0 */
    private static final int URES_INDEX_KEYS_TOP         = 1;   /* contains the top of the key strings, */
                                                                /* same as the bottom of resources or UTF-16 strings, rounded up */
    //ivate static final int URES_INDEX_RESOURCES_TOP    = 2;   /* contains the top of all resources */
    private static final int URES_INDEX_BUNDLE_TOP       = 3;   /* contains the top of the bundle, */
                                                                /* in case it were ever different from [2] */
    //ivate static final int URES_INDEX_MAX_TABLE_LENGTH = 4;   /* max. length of any table */
    private static final int URES_INDEX_ATTRIBUTES       = 5;   /* attributes bit set, see URES_ATT_* (new in formatVersion 1.2) */
    private static final int URES_INDEX_16BIT_TOP        = 6;   /* top of the 16-bit units (UTF-16 string v2 UChars, URES_TABLE16, URES_ARRAY16),
                                                                 * rounded up (new in formatVersion 2.0, ICU 4.4) */
    private static final int URES_INDEX_POOL_CHECKSUM    = 7;   /* checksum of the pool bundle (new in formatVersion 2.0, ICU 4.4) */
    //ivate static final int URES_INDEX_TOP              = 8;

    /*
     * Nofallback attribute, attribute bit 0 in indexes[URES_INDEX_ATTRIBUTES].
     * New in formatVersion 1.2 (ICU 3.6).
     *
     * If set, then this resource bundle is a standalone bundle.
     * If not set, then the bundle participates in locale fallback, eventually
     * all the way to the root bundle.
     * If indexes[] is missing or too short, then the attribute cannot be determined
     * reliably. Dependency checking should ignore such bundles, and loading should
     * use fallbacks.
     */
    private static final int URES_ATT_NO_FALLBACK = 1;

    /*
     * Attributes for bundles that are, or use, a pool bundle.
     * A pool bundle provides key strings that are shared among several other bundles
     * to reduce their total size.
     * New in formatVersion 2 (ICU 4.4).
     */
    private static final int URES_ATT_IS_POOL_BUNDLE = 2;
    private static final int URES_ATT_USES_POOL_BUNDLE = 4;

    private static final boolean DEBUG = false;
    
    private byte[] /* formatVersion, */ dataVersion;

    // See the ResourceData struct in ICU4C/source/common/uresdata.h.
    private String s16BitUnits;
    private byte[] poolBundleKeys;
    private String poolBundleKeysAsString;
    private int rootRes;
    private int localKeyLimit;
    private boolean noFallback; /* see URES_ATT_NO_FALLBACK */
    private boolean isPoolBundle;
    private boolean usesPoolBundle;

    // Fields specific to the Java port.
    private int[] indexes;
    private byte[] keyStrings;
    private String keyStringsAsString;  // null except if isPoolBundle
    private byte[] resourceBytes;
    private int resourceBottom;  // File offset where the mixed-type resources start.

    private ICUResourceBundleReader(InputStream stream, String resolvedName){
        BufferedInputStream bs = new BufferedInputStream(stream);
        try{
            if(DEBUG) System.out.println("The InputStream class is: " + stream.getClass().getName());
            if(DEBUG) System.out.println("The BufferedInputStream class is: " + bs.getClass().getName());
            if(DEBUG) System.out.println("The bytes avialable in stream before reading the header: " + bs.available());
            
            dataVersion = ICUBinary.readHeader(bs,DATA_FORMAT_ID,this);

            if(DEBUG) System.out.println("The bytes available in stream after reading the header: " + bs.available());
                 
            readData(bs);
            stream.close();
        }catch(IOException ex){
            throw new RuntimeException("Data file "+ resolvedName+ " is corrupt - " + ex.getMessage());   
        }
    }
    static ICUResourceBundleReader getReader(String resolvedName, ClassLoader root) {
        InputStream stream = ICUData.getStream(root,resolvedName);
        
        if(stream==null){
            return null;
        }
        ICUResourceBundleReader reader = new ICUResourceBundleReader(stream, resolvedName);
        return reader;
    }
    
    void setPoolBundleKeys(ICUResourceBundleReader poolBundleReader) {
        if(!poolBundleReader.isPoolBundle) {
            throw new IllegalStateException("pool.res is not a pool bundle");
        }
        if(poolBundleReader.indexes[URES_INDEX_POOL_CHECKSUM] != indexes[URES_INDEX_POOL_CHECKSUM]) {
            throw new IllegalStateException("pool.res has a different checksum than this bundle");
        }
        poolBundleKeys = poolBundleReader.keyStrings;
        poolBundleKeysAsString = poolBundleReader.keyStringsAsString;
    }

    // See res_init() in ICU4C/source/common/uresdata.c.
    private void readData(InputStream stream) throws IOException {
        DataInputStream ds = new DataInputStream(stream);

        if(DEBUG) System.out.println("The DataInputStream class is: " + ds.getClass().getName());
        if(DEBUG) System.out.println("The available bytes in the stream before reading the data: "+ds.available());

        rootRes = ds.readInt();

        // read the variable-length indexes[] array
        int indexes0 = ds.readInt();
        int indexLength = indexes0 & 0xff;
        indexes = new int[indexLength];
        indexes[URES_INDEX_LENGTH] = indexes0;
        for(int i=1; i<indexLength; i++){
            indexes[i] = ds.readInt();   
        }
        resourceBottom = (1 + indexLength) << 2;

        if(indexLength > URES_INDEX_ATTRIBUTES) {
            // determine if this resource bundle falls back to a parent bundle
            // along normal locale ID fallback
            int att = indexes[URES_INDEX_ATTRIBUTES];
            noFallback = (att & URES_ATT_NO_FALLBACK) != 0;
            isPoolBundle = (att & URES_ATT_IS_POOL_BUNDLE) != 0;
            usesPoolBundle = (att & URES_ATT_USES_POOL_BUNDLE) != 0;
        }

        int length = indexes[URES_INDEX_BUNDLE_TOP]*4;
        if(DEBUG) System.out.println("The number of bytes in the bundle: "+length);

        // Read the local key strings.
        // The keyStrings include NUL characters corresponding to the bytes
        // up to the end of the indexes.
        if(indexes[URES_INDEX_KEYS_TOP] > (1 + indexLength)) {
            int keysBottom = (1 + indexLength) << 2;
            int keysTop = indexes[URES_INDEX_KEYS_TOP] << 2;
            resourceBottom = keysTop;
            if(isPoolBundle) {
                // Shift the key strings down:
                // Pool bundle key strings are used with a 0-based index,
                // unlike regular bundles' key strings for which indexes
                // are based on the start of the bundle data.
                keysTop -= keysBottom;
                keysBottom = 0;
            } else {
                localKeyLimit = keysTop;
            }
            keyStrings = new byte[keysTop];
            ds.readFully(keyStrings, keysBottom, keysTop - keysBottom);
            if(isPoolBundle) {
                // Overwrite trailing padding bytes so that the conversion works.
                while(keysBottom < keysTop && keyStrings[keysTop - 1] == (byte)0xaa) {
                    keyStrings[--keysTop] = 0;
                }
                keyStringsAsString = new String(keyStrings, "US-ASCII");
            }
        }

        // Read the array of 16-bit units.
        // We are not using
        //   new String(keys, "UTF-16BE")
        // because the 16-bit units may not be well-formed Unicode.
        if( indexLength > URES_INDEX_16BIT_TOP &&
            indexes[URES_INDEX_16BIT_TOP] > indexes[URES_INDEX_KEYS_TOP]
        ) {
            int num16BitUnits = (indexes[URES_INDEX_16BIT_TOP] -
                                 indexes[URES_INDEX_KEYS_TOP]) * 2;
            char[] c16BitUnits = new char[num16BitUnits];
            for(int i = 0; i < num16BitUnits; ++i) {
                c16BitUnits[i] = ds.readChar();
            }
            s16BitUnits = new String(c16BitUnits);
            resourceBottom = indexes[URES_INDEX_16BIT_TOP] << 2;
        } else {
            s16BitUnits = "\0";
        }

        // Read the block of bytes for the mixed-type resources.
        resourceBytes = new byte[length - resourceBottom];
        ds.readFully(resourceBytes);
    }

    VersionInfo getVersion(){
        return VersionInfo.getInstance(dataVersion[0],dataVersion[1],dataVersion[2],dataVersion[3]);   
    }
    public boolean isDataVersionAcceptable(byte version[]){
        // while ICU4C can read formatVersion 1.0 and up,
        // ICU4J requires 1.1 as a minimum
        // formatVersion = version;
        return ((version[0] == 1 && version[1] >= 1) || version[0] == 2);
    }
    
    int getRootResource() {
        return rootRes;
    }
    boolean getNoFallback() {
        return noFallback;
    }
    boolean getUsesPoolBundle() {
        return usesPoolBundle;
    }

    static int RES_GET_TYPE(int res) {
        return res >>> 28;
    }
    private static int RES_GET_OFFSET(int res) {
        return res & 0x0fffffff;
    }
    private int getResourceByteOffset(int offset) {
        return (offset << 2) - resourceBottom;
    }
    /* get signed and unsigned integer values directly from the Resource handle */
    static int RES_GET_INT(int res) {
        return (res << 4) >> 4;
    }
    static int RES_GET_UINT(int res) {
        return res & 0x0fffffff;
    }
    static boolean URES_IS_TABLE(int type) {
        return type==UResourceBundle.TABLE || type==ICUResourceBundle.TABLE16 || type==ICUResourceBundle.TABLE32;
    }

    private static byte[] emptyBytes = new byte[0];
    private static ByteBuffer emptyByteBuffer = ByteBuffer.allocate(0).asReadOnlyBuffer();
    private static char[] emptyChars = new char[0];
    private static int[] emptyInts = new int[0];
    private static String emptyString = "";

    private char getChar(int offset) {
        return (char)((resourceBytes[offset] << 8) | (resourceBytes[offset + 1] & 0xff));
    }
    private char[] getChars(int offset, int count) {
        char[] chars = new char[count];
        for(int i = 0; i < count; offset += 2, ++i) {
            chars[i] = (char)(((int)resourceBytes[offset] << 8) | (resourceBytes[offset + 1] & 0xff));
        }
        return chars;
    }
    private int getInt(int offset) {
        return (resourceBytes[offset] << 24) |
                ((resourceBytes[offset+1] & 0xff) << 16) |
                ((resourceBytes[offset+2] & 0xff) << 8) |
                ((resourceBytes[offset+3] & 0xff));
    }
    private int[] getInts(int offset, int count) {
        int[] ints = new int[count];
        for(int i = 0; i < count; offset += 4, ++i) {
            ints[i] = (resourceBytes[offset] << 24) |
                        ((resourceBytes[offset+1] & 0xff) << 16) |
                        ((resourceBytes[offset+2] & 0xff) << 8) |
                        ((resourceBytes[offset+3] & 0xff));
        }
        return ints;
    }
    private char[] getTable16KeyOffsets(int offset) {
        int length = s16BitUnits.charAt(offset++);
        if(length > 0) {
            return s16BitUnits.substring(offset, offset + length).toCharArray();
        } else {
            return emptyChars;
        }
    }
    private char[] getTableKeyOffsets(int offset) {
        int length = getChar(offset);
        if(length > 0) {
            return getChars(offset + 2, length);
        } else {
            return emptyChars;
        }
    }
    private int[] getTable32KeyOffsets(int offset) {
        int length = getInt(offset);
        if(length > 0) {
            return getInts(offset + 4, length);
        } else {
            return emptyInts;
        }
    }

    /** Refers to ASCII key string bytes, for key string matching. */
    private static final class ByteSequence {
        private byte[] bytes;
        private int offset;
        public ByteSequence(byte[] bytes, int offset) {
            this.bytes = bytes;
            this.offset = offset;
        }
        public byte charAt(int index) {
            return bytes[offset + index];
        }
    }
    private String makeKeyStringFromBytes(int keyOffset) {
        StringBuilder sb = new StringBuilder();
        byte b;
        while((b = keyStrings[keyOffset++]) != 0) {
            sb.append((char)b);
        }
        return sb.toString();
    }
    private String makeKeyStringFromString(int keyOffset) {
        int endOffset = keyOffset;
        while(poolBundleKeysAsString.charAt(endOffset) != 0) {
            ++endOffset;
        }
        return poolBundleKeysAsString.substring(keyOffset, endOffset);
    }
    private ByteSequence RES_GET_KEY16(char keyOffset) {
        if(keyOffset < localKeyLimit) {
            return new ByteSequence(keyStrings, keyOffset);
        } else {
            return new ByteSequence(poolBundleKeys, keyOffset - localKeyLimit);
        }
    }
    private String getKey16String(int keyOffset) {
        if(keyOffset < localKeyLimit) {
            return makeKeyStringFromBytes(keyOffset);
        } else {
            return makeKeyStringFromString(keyOffset - localKeyLimit);
        }
    }
    private ByteSequence RES_GET_KEY32(int keyOffset) {
        if(keyOffset >= 0) {
            return new ByteSequence(keyStrings, keyOffset);
        } else {
            return new ByteSequence(poolBundleKeys, keyOffset & 0x7fffffff);
        }
    }
    private String getKey32String(int keyOffset) {
        if(keyOffset >= 0) {
            return makeKeyStringFromBytes(keyOffset);
        } else {
            return makeKeyStringFromString(keyOffset & 0x7fffffff);
        }
    }
    // Compare the length-specified input key with the
    // NUL-terminated tableKey.
    private static int compareKeys(CharSequence key, ByteSequence tableKey) {
        int i;
        for(i = 0; i < key.length(); ++i) {
            int c2 = tableKey.charAt(i);
            if(c2 == 0) {
                return 1;  // key > tableKey because key is longer.
            }
            int diff = (int)key.charAt(i) - c2;
            if(diff != 0) {
                return diff;
            }
        }
        return -(int)tableKey.charAt(i);
    }
    private int compareKeys(CharSequence key, char keyOffset) {
        return compareKeys(key, RES_GET_KEY16(keyOffset));
    }
    private int compareKeys32(CharSequence key, int keyOffset) {
        return compareKeys(key, RES_GET_KEY32(keyOffset));
    }

    String getString(int res) {
        int offset=RES_GET_OFFSET(res);
        int length;
        if(RES_GET_TYPE(res)==ICUResourceBundle.STRING_V2) {
            int first = s16BitUnits.charAt(offset);
            if((first&0xfffffc00)!=0xdc00) {  // C: if(!U16_IS_TRAIL(first)) {
                if(first==0) {
                    return emptyString;
                }
                int endOffset;
                for(endOffset=offset+1; s16BitUnits.charAt(endOffset)!=0; ++endOffset) {}
                return s16BitUnits.substring(offset, endOffset);
            } else if(first<0xdfef) {
                length=first&0x3ff;
                ++offset;
            } else if(first<0xdfff) {
                length=((first-0xdfef)<<16)|s16BitUnits.charAt(offset+1);
                offset+=2;
            } else {
                length=((int)s16BitUnits.charAt(offset+1)<<16)|s16BitUnits.charAt(offset+2);
                offset+=3;
            }
            return s16BitUnits.substring(offset, offset+length);
        } else if(res==offset) /* RES_GET_TYPE(res)==URES_STRING */ {
            if(res==0) {
                return emptyString;
            } else {
                offset=getResourceByteOffset(offset);
                length=getInt(offset);
                return new String(getChars(offset+4, length));
            }
        } else {
            return null;
        }
    }

    String getAlias(int res) {
        int offset=RES_GET_OFFSET(res);
        int length;
        if(RES_GET_TYPE(res)==ICUResourceBundle.ALIAS) {
            if(offset==0) {
                return emptyString;
            } else {
                offset=getResourceByteOffset(offset);
                length=getInt(offset);
                return new String(getChars(offset+4, length));
            }
        } else {
            return null;
        }
    }

    byte[] getBinary(int res, byte[] ba) {
        int offset=RES_GET_OFFSET(res);
        int length;
        if(RES_GET_TYPE(res)==UResourceBundle.BINARY) {
            if(offset==0) {
                return emptyBytes;
            } else {
                offset=getResourceByteOffset(offset);
                length=getInt(offset);
                if(ba==null || ba.length!=length) {
                    ba=new byte[length];
                }
                System.arraycopy(resourceBytes, offset+4, ba, 0, length);
                return ba;
            }
        } else {
            return null;
        }
    }

    ByteBuffer getBinary(int res) {
        int offset=RES_GET_OFFSET(res);
        int length;
        if(RES_GET_TYPE(res)==UResourceBundle.BINARY) {
            if(offset==0) {
                // Don't just
                //   return emptyByteBuffer;
                // in case it matters whether the buffer's mark is defined or undefined.
                return emptyByteBuffer.duplicate();
            } else {
                offset=getResourceByteOffset(offset);
                length=getInt(offset);
                return ByteBuffer.wrap(resourceBytes, offset+4, length).slice().asReadOnlyBuffer();
            }
        } else {
            return null;
        }
    }

    int[] getIntVector(int res) {
        int offset=RES_GET_OFFSET(res);
        int length;
        if(RES_GET_TYPE(res)==UResourceBundle.INT_VECTOR) {
            if(offset==0) {
                return emptyInts;
            } else {
                offset=getResourceByteOffset(offset);
                length=getInt(offset);
                return getInts(offset+4, length);
            }
        } else {
            return null;
        }
    }

    Container getArray(int res) {
        int type=RES_GET_TYPE(res);
        int offset=RES_GET_OFFSET(res);
        switch(type) {
        case UResourceBundle.ARRAY:
        case ICUResourceBundle.ARRAY16:
            if(offset==0) {
                return new Container(this);
            }
            break;
        default:
            return null;
        }
        switch(type) {
        case UResourceBundle.ARRAY:
            return new Array(this, offset);
        case ICUResourceBundle.ARRAY16:
            return new Array16(this, offset);
        default:
            return null;
        }
    }

    Table getTable(int res) {
        int type=RES_GET_TYPE(res);
        int offset=RES_GET_OFFSET(res);
        switch(type) {
        case UResourceBundle.TABLE:
        case ICUResourceBundle.TABLE16:
        case ICUResourceBundle.TABLE32:
            if(offset==0) {
                return new Table(this);
            }
            break;
        default:
            return null;
        }
        switch(type) {
        case UResourceBundle.TABLE:
            return new Table1632(this, offset);
        case ICUResourceBundle.TABLE16:
            return new Table16(this, offset);
        case ICUResourceBundle.TABLE32:
            return new Table32(this, offset);
        default:
            return null;
        }
    }

    // Container value classes --------------------------------------------- ***

    static class Container {
        protected ICUResourceBundleReader reader;
        protected int size;
        protected int itemsOffset;

        int getSize() {
            return size;
        }
        int getContainerResource(int index) {
            return ICUResourceBundle.RES_BOGUS;
        }
        protected int getContainer16Resource(int index) {
            if (index < 0 || size <= index) {
                return ICUResourceBundle.RES_BOGUS;
            }
            return (ICUResourceBundle.STRING_V2 << 28) |
                   reader.s16BitUnits.charAt(itemsOffset + index);
        }
        protected int getContainer32Resource(int index) {
            if (index < 0 || size <= index) {
                return ICUResourceBundle.RES_BOGUS;
            }
            return reader.getInt(itemsOffset + 4 * index);
        }
        Container(ICUResourceBundleReader reader) {
            this.reader = reader;
        }
    }
    private static final class Array extends Container {
        int getContainerResource(int index) {
            return getContainer32Resource(index);
        }
        Array(ICUResourceBundleReader reader, int offset) {
            super(reader);
            offset = reader.getResourceByteOffset(offset);
            size = reader.getInt(offset);
            itemsOffset = offset + 4;
        }
    }
    private static final class Array16 extends Container {
        int getContainerResource(int index) {
            return getContainer16Resource(index);
        }
        Array16(ICUResourceBundleReader reader, int offset) {
            super(reader);
            size = reader.s16BitUnits.charAt(offset);
            itemsOffset = offset + 1;
        }
    }
    static class Table extends Container {
        protected char[] keyOffsets;
        protected int[] key32Offsets;

        String getKey(int index) {
            if (index < 0 || size <= index) {
                return null;
            }
            return keyOffsets != null ?
                        reader.getKey16String(keyOffsets[index]) :
                        reader.getKey32String(key32Offsets[index]);
        }
        private static final int URESDATA_ITEM_NOT_FOUND = -1;
        int findTableItem(CharSequence key) {
            int mid, start, limit;
            int result;

            /* do a binary search for the key */
            start=0;
            limit=size;
            while(start<limit) {
                mid = (start + limit) / 2;
                if (keyOffsets != null) {
                    result = reader.compareKeys(key, keyOffsets[mid]);
                } else {
                    result = reader.compareKeys32(key, key32Offsets[mid]);
                }
                if (result < 0) {
                    limit = mid;
                } else if (result > 0) {
                    start = mid + 1;
                } else {
                    /* We found it! */
                    return mid;
                }
            }
            return URESDATA_ITEM_NOT_FOUND;  /* not found or table is empty. */
        }
        int getTableResource(String resKey) {
            return getContainerResource(findTableItem(resKey));
        }
        Table(ICUResourceBundleReader reader) {
            super(reader);
        }
    }
    private static final class Table1632 extends Table {
        int getContainerResource(int index) {
            return getContainer32Resource(index);
        }
        Table1632(ICUResourceBundleReader reader, int offset) {
            super(reader);
            offset = reader.getResourceByteOffset(offset);
            keyOffsets = reader.getTableKeyOffsets(offset);
            size = keyOffsets.length;
            itemsOffset = offset + 2 * ((size + 2) & ~1);  // Skip padding for 4-alignment.
        }
    }
    private static final class Table16 extends Table {
        int getContainerResource(int index) {
            return getContainer16Resource(index);
        }
        Table16(ICUResourceBundleReader reader, int offset) {
            super(reader);
            keyOffsets = reader.getTable16KeyOffsets(offset);
            size = keyOffsets.length;
            itemsOffset = offset + 1 + size;
        }
    }
    private static final class Table32 extends Table {
        int getContainerResource(int index) {
            return getContainer32Resource(index);
        }
        Table32(ICUResourceBundleReader reader, int offset) {
            super(reader);
            offset = reader.getResourceByteOffset(offset);
            key32Offsets = reader.getTable32KeyOffsets(offset);
            size = key32Offsets.length;
            itemsOffset = offset + 4 * (1 + size);
        }
    }
}
