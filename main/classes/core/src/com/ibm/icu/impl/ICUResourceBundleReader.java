/*
 *******************************************************************************
 * Copyright (C) 2004-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
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
public final class ICUResourceBundleReader {
    /**
     * File format version that this class understands.
     * "ResB"
     */
    private static final int DATA_FORMAT = 0x52657342;
    private static final class IsAcceptable implements ICUBinary.Authenticate {
        // @Override when we switch to Java 6
        public boolean isDataVersionAcceptable(byte formatVersion[]) {
            return (formatVersion[0] == 1 || formatVersion[0] == 2);
        }
    }
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();

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
    private static final int URES_INDEX_MAX_TABLE_LENGTH = 4;   /* max. length of any table */
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

    private static final CharBuffer EMPTY_16_BIT_UNITS = CharBuffer.wrap("\0");  // read-only

    /**
     * Objects with more value bytes are stored in SoftReferences.
     * Smaller objects (which are not much larger than a SoftReference)
     * are stored directly, avoiding the overhead of the reference.
     */
    static final int LARGE_SIZE = 24;

    private static final boolean DEBUG = false;

    private int /* formatVersion, */ dataVersion;

    // See the ResourceData struct in ICU4C/source/common/uresdata.h.
    /**
     * Buffer of all of the resource bundle bytes after the header.
     * (equivalent of C++ pRoot)
     */
    private ByteBuffer bytes;
    private CharBuffer b16BitUnits;
    private ByteBuffer poolBundleKeys;
    private int rootRes;
    private int localKeyLimit;
    private boolean noFallback; /* see URES_ATT_NO_FALLBACK */
    private boolean isPoolBundle;
    private boolean usesPoolBundle;
    private int poolCheckSum;

    private ResourceCache resourceCache;

    private static ReaderCache CACHE = new ReaderCache();
    private static final ICUResourceBundleReader NULL_READER = new ICUResourceBundleReader();

    private static class ReaderInfo {
        final String baseName;
        final String localeID;
        final ClassLoader loader;

        ReaderInfo(String baseName, String localeID, ClassLoader loader) {
            this.baseName = (baseName == null) ? "" : baseName;
            this.localeID = (localeID == null) ? "" : localeID;
            this.loader = loader;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReaderInfo)) {
                return false;
            }
            ReaderInfo info = (ReaderInfo)obj;
            return this.baseName.equals(info.baseName)
                    && this.localeID.equals(info.localeID)
                    && this.loader.equals(info.loader);
        }

        public int hashCode() {
            return baseName.hashCode() ^ localeID.hashCode() ^ loader.hashCode();
        }
    }

    private static class ReaderCache extends SoftCache<ReaderInfo, ICUResourceBundleReader, ReaderInfo> {
        /* (non-Javadoc)
         * @see com.ibm.icu.impl.CacheBase#createInstance(java.lang.Object, java.lang.Object)
         */
        @Override
        protected ICUResourceBundleReader createInstance(ReaderInfo key, ReaderInfo data) {
            String fullName = ICUResourceBundleReader.getFullName(data.baseName, data.localeID);
            try {
                ByteBuffer inBytes;
                if (data.baseName != null && data.baseName.startsWith(ICUData.ICU_BASE_NAME)) {
                    String itemPath = fullName.substring(ICUData.ICU_BASE_NAME.length() + 1);
                    inBytes = ICUBinary.getData(data.loader, fullName, itemPath);
                    if (inBytes == null) {
                        return NULL_READER;
                    }
                } else {
                    @SuppressWarnings("resource")  // Closed by getByteBufferFromInputStreamAndCloseStream().
                    InputStream stream = ICUData.getStream(data.loader, fullName);
                    if (stream == null) {
                        return NULL_READER;
                    }
                    inBytes = ICUBinary.getByteBufferFromInputStreamAndCloseStream(stream);
                }
                return new ICUResourceBundleReader(inBytes, data.baseName, data.localeID, data.loader);
            } catch (IOException ex) {
                throw new ICUUncheckedIOException("Data file " + fullName + " is corrupt - " + ex.getMessage(), ex);
            }
        }
    }

    /*
     * Default constructor, just used for NULL_READER.
     */
    private ICUResourceBundleReader() {
    }

    private ICUResourceBundleReader(ByteBuffer inBytes,
            String baseName, String localeID,
            ClassLoader loader) throws IOException {
        init(inBytes);

        // set pool bundle keys if necessary
        if (usesPoolBundle) {
            ICUResourceBundleReader poolBundleReader = getReader(baseName, "pool", loader);
            if (!poolBundleReader.isPoolBundle) {
                throw new IllegalStateException("pool.res is not a pool bundle");
            }
            if (poolBundleReader.poolCheckSum != poolCheckSum) {
                throw new IllegalStateException("pool.res has a different checksum than this bundle");
            }
            poolBundleKeys = poolBundleReader.bytes;
        }
    }

    static ICUResourceBundleReader getReader(String baseName, String localeID, ClassLoader root) {
        ReaderInfo info = new ReaderInfo(baseName, localeID, root);
        ICUResourceBundleReader reader = CACHE.getInstance(info, info);
        if (reader == NULL_READER) {
            return null;
        }
        return reader;
    }

    // See res_init() in ICU4C/source/common/uresdata.c.
    private void init(ByteBuffer inBytes) throws IOException {
        dataVersion = ICUBinary.readHeader(inBytes, DATA_FORMAT, IS_ACCEPTABLE);
        boolean isFormatVersion10 = inBytes.get(16) == 1 && inBytes.get(17) == 0;
        bytes = ICUBinary.sliceWithOrder(inBytes);
        int dataLength = bytes.remaining();

        if(DEBUG) System.out.println("The ByteBuffer is direct (memory-mapped): " + bytes.isDirect());
        if(DEBUG) System.out.println("The available bytes in the buffer before reading the data: " + dataLength);

        rootRes = bytes.getInt(0);

        if(isFormatVersion10) {
            localKeyLimit = 0x10000;  /* greater than any 16-bit key string offset */
            resourceCache = new ResourceCache(dataLength / 4 - 1);
            return;
        }

        // read the variable-length indexes[] array
        int indexes0 = getIndexesInt(URES_INDEX_LENGTH);
        int indexLength = indexes0 & 0xff;
        if(indexLength <= URES_INDEX_MAX_TABLE_LENGTH) {
            throw new ICUException("not enough indexes");
        }
        int bundleTop;
        if(dataLength < ((1 + indexLength) << 2) ||
                dataLength < ((bundleTop = getIndexesInt(URES_INDEX_BUNDLE_TOP)) << 2)) {
            throw new ICUException("not enough bytes");
        }
        int maxOffset = bundleTop - 1;

        if(indexLength > URES_INDEX_ATTRIBUTES) {
            // determine if this resource bundle falls back to a parent bundle
            // along normal locale ID fallback
            int att = getIndexesInt(URES_INDEX_ATTRIBUTES);
            noFallback = (att & URES_ATT_NO_FALLBACK) != 0;
            isPoolBundle = (att & URES_ATT_IS_POOL_BUNDLE) != 0;
            usesPoolBundle = (att & URES_ATT_USES_POOL_BUNDLE) != 0;
        }

        // Read the array of 16-bit units.
        if(indexLength > URES_INDEX_16BIT_TOP) {
            int keysTop = getIndexesInt(URES_INDEX_KEYS_TOP);
            int _16BitTop = getIndexesInt(URES_INDEX_16BIT_TOP);
            if(_16BitTop > keysTop) {
                int num16BitUnits = (_16BitTop - keysTop) * 2;
                bytes.position(keysTop << 2);
                b16BitUnits = bytes.asCharBuffer();
                b16BitUnits.limit(num16BitUnits);
                maxOffset |= num16BitUnits - 1;
            } else {
                b16BitUnits = EMPTY_16_BIT_UNITS;
            }
        } else {
            b16BitUnits = EMPTY_16_BIT_UNITS;
        }

        if(indexLength > URES_INDEX_POOL_CHECKSUM) {
            poolCheckSum = getIndexesInt(URES_INDEX_POOL_CHECKSUM);
        }

        // Handle key strings last:
        // If this is a pool bundle, then we shift all bytes down,
        // and getIndexesInt() will not work any more.
        if(getIndexesInt(URES_INDEX_KEYS_TOP) > (1 + indexLength)) {
            if(isPoolBundle) {
                // Shift the key strings down:
                // Pool bundle key strings are used with a 0-based index,
                // unlike regular bundles' key strings for which indexes
                // are based on the start of the bundle data.
                bytes.position((1 + indexLength) << 2);
                bytes = ICUBinary.sliceWithOrder(bytes);
            } else {
                localKeyLimit = getIndexesInt(URES_INDEX_KEYS_TOP) << 2;
            }
        }

        if(!isPoolBundle) {
            resourceCache = new ResourceCache(maxOffset);
        }
    }

    private int getIndexesInt(int i) {
        return bytes.getInt((1 + i) << 2);
    }

    VersionInfo getVersion() {
        return ICUBinary.getVersionInfoFromCompactInt(dataVersion);
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
        return offset << 2;
    }
    /* get signed and unsigned integer values directly from the Resource handle */
    static int RES_GET_INT(int res) {
        return (res << 4) >> 4;
    }
    static int RES_GET_UINT(int res) {
        return res & 0x0fffffff;
    }
    static boolean URES_IS_ARRAY(int type) {
        return type == UResourceBundle.ARRAY || type == ICUResourceBundle.ARRAY16;
    }
    static boolean URES_IS_TABLE(int type) {
        return type==UResourceBundle.TABLE || type==ICUResourceBundle.TABLE16 || type==ICUResourceBundle.TABLE32;
    }

    private static final byte[] emptyBytes = new byte[0];
    private static final ByteBuffer emptyByteBuffer = ByteBuffer.allocate(0).asReadOnlyBuffer();
    private static final char[] emptyChars = new char[0];
    private static final int[] emptyInts = new int[0];
    private static final String emptyString = "";
    private static final Container EMPTY_ARRAY = new Container();
    private static final Table EMPTY_TABLE = new Table();

    private char getChar(int offset) {
        return bytes.getChar(offset);
    }
    private char[] getChars(int offset, int count) {
        char[] chars = new char[count];
        for(int i = 0; i < count; offset += 2, ++i) {
            chars[i] = bytes.getChar(offset);
        }
        return chars;
    }
    private int getInt(int offset) {
        return bytes.getInt(offset);
    }
    private int[] getInts(int offset, int count) {
        int[] ints = new int[count];
        for(int i = 0; i < count; offset += 4, ++i) {
            ints[i] = bytes.getInt(offset);
        }
        return ints;
    }
    private char[] getTable16KeyOffsets(int offset) {
        int length = b16BitUnits.charAt(offset++);
        if(length > 0) {
            char[] result = new char[length];
            if(length <= 16) {
                for(int i = 0; i < length; ++i) {
                    result[i] = b16BitUnits.charAt(offset++);
                }
            } else {
                CharBuffer temp = b16BitUnits.duplicate();
                temp.position(offset);
                temp.get(result);
            }
            return result;
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

    private static String makeKeyStringFromBytes(ByteBuffer keyBytes, int keyOffset) {
        StringBuilder sb = new StringBuilder();
        byte b;
        while((b = keyBytes.get(keyOffset)) != 0) {
            ++keyOffset;
            sb.append((char)b);
        }
        return sb.toString();
    }
    private String getKey16String(int keyOffset) {
        if(keyOffset < localKeyLimit) {
            return makeKeyStringFromBytes(bytes, keyOffset);
        } else {
            return makeKeyStringFromBytes(poolBundleKeys, keyOffset - localKeyLimit);
        }
    }
    private String getKey32String(int keyOffset) {
        if(keyOffset >= 0) {
            return makeKeyStringFromBytes(bytes, keyOffset);
        } else {
            return makeKeyStringFromBytes(poolBundleKeys, keyOffset & 0x7fffffff);
        }
    }
    private int compareKeys(CharSequence key, char keyOffset) {
        if(keyOffset < localKeyLimit) {
            return ICUBinary.compareKeys(key, bytes, keyOffset);
        } else {
            return ICUBinary.compareKeys(key, poolBundleKeys, keyOffset - localKeyLimit);
        }
    }
    private int compareKeys32(CharSequence key, int keyOffset) {
        if(keyOffset >= 0) {
            return ICUBinary.compareKeys(key, bytes, keyOffset);
        } else {
            return ICUBinary.compareKeys(key, poolBundleKeys, keyOffset & 0x7fffffff);
        }
    }

    String getString(int res) {
        int offset=RES_GET_OFFSET(res);
        if(res != offset /* RES_GET_TYPE(res) != URES_STRING */ &&
                RES_GET_TYPE(res) != ICUResourceBundle.STRING_V2) {
            return null;
        }
        if(offset == 0) {
            return emptyString;
        }
        Object value = resourceCache.get(res);
        if(value != null) {
            return (String)value;
        }
        String s;
        if(res != offset) {  // STRING_V2
            int first = b16BitUnits.charAt(offset);
            if((first&0xfffffc00)!=0xdc00) {  // C: if(!U16_IS_TRAIL(first)) {
                if(first==0) {
                    return emptyString;  // Should not occur, but is not forbidden.
                }
                StringBuilder sb = new StringBuilder();
                sb.append((char)first);
                char c;
                while((c = b16BitUnits.charAt(++offset)) != 0) {
                    sb.append(c);
                }
                s = sb.toString();
            } else {
                int length;
                if(first<0xdfef) {
                    length=first&0x3ff;
                    ++offset;
                } else if(first<0xdfff) {
                    length=((first-0xdfef)<<16)|b16BitUnits.charAt(offset+1);
                    offset+=2;
                } else {
                    length=((int)b16BitUnits.charAt(offset+1)<<16)|b16BitUnits.charAt(offset+2);
                    offset+=3;
                }
                // Cast up to CharSequence to insulate against the CharBuffer.subSequence() return type change
                // which makes code compiled for a newer JDK not run on an older one.
                s = ((CharSequence) b16BitUnits).subSequence(offset, offset + length).toString();
            }
        } else {
            offset=getResourceByteOffset(offset);
            int length = getInt(offset);
            s = new String(getChars(offset+4, length));
        }
        return (String)resourceCache.putIfAbsent(res, s, s.length() * 2);
    }

    String getAlias(int res) {
        int offset=RES_GET_OFFSET(res);
        int length;
        if(RES_GET_TYPE(res)==ICUResourceBundle.ALIAS) {
            if(offset==0) {
                return emptyString;
            } else {
                Object value = resourceCache.get(res);
                if(value != null) {
                    return (String)value;
                }
                offset=getResourceByteOffset(offset);
                length=getInt(offset);
                String s = new String(getChars(offset + 4, length));
                return (String)resourceCache.putIfAbsent(res, s, length * 2);
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
                if(length==0) {
                    return emptyBytes;
                }
                // Not cached: The array would have to be cloned anyway because
                // the cache must not be writable via the returned reference.
                if(ba==null || ba.length!=length) {
                    ba=new byte[length];
                }
                offset += 4;
                if(length <= 16) {
                    for(int i = 0; i < length; ++i) {
                        ba[i] = bytes.get(offset++);
                    }
                } else {
                    ByteBuffer temp = bytes.duplicate();
                    temp.position(offset);
                    temp.get(ba);
                }
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
                // Not cached: The returned buffer is small (shares its bytes with the bundle)
                // and usually quickly discarded after use.
                // Also, even a cached buffer would have to be cloned because it is mutable
                // (position & mark).
                offset=getResourceByteOffset(offset);
                length=getInt(offset);
                if(length == 0) {
                    return emptyByteBuffer.duplicate();
                }
                offset += 4;
                ByteBuffer result = bytes.duplicate();
                result.position(offset).limit(offset + length);
                result = ICUBinary.sliceWithOrder(result);
                if(!result.isReadOnly()) {
                    result = result.asReadOnlyBuffer();
                }
                return result;
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
                // Not cached: The array would have to be cloned anyway because
                // the cache must not be writable via the returned reference.
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
        if(!URES_IS_ARRAY(type)) {
            return null;
        }
        int offset=RES_GET_OFFSET(res);
        if(offset == 0) {
            return EMPTY_ARRAY;
        }
        Object value = resourceCache.get(res);
        if(value != null) {
            return (Container)value;
        }
        Container array = (type == UResourceBundle.ARRAY) ?
                new Array(this, offset) : new Array16(this, offset);
        return (Container)resourceCache.putIfAbsent(res, array, 0);
    }

    Table getTable(int res) {
        int type = RES_GET_TYPE(res);
        if(!URES_IS_TABLE(type)) {
            return null;
        }
        int offset = RES_GET_OFFSET(res);
        if(offset == 0) {
            return EMPTY_TABLE;
        }
        Object value = resourceCache.get(res);
        if(value != null) {
            return (Table)value;
        }
        Table table;
        int size;  // Use size = 0 to never use SoftReferences for Tables?
        if(type == UResourceBundle.TABLE) {
            table = new Table1632(this, offset);
            size = table.getSize() * 2;
        } else if(type == ICUResourceBundle.TABLE16) {
            table = new Table16(this, offset);
            size = table.getSize() * 2;
        } else /* type == ICUResourceBundle.TABLE32 */ {
            table = new Table32(this, offset);
            size = table.getSize() * 4;
        }
        return (Table)resourceCache.putIfAbsent(res, table, size);
    }

    // Container value classes --------------------------------------------- ***

    static class Container {
        protected int size;
        protected int itemsOffset;

        int getSize() {
            return size;
        }
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return ICUResourceBundle.RES_BOGUS;
        }
        protected int getContainer16Resource(ICUResourceBundleReader reader, int index) {
            if (index < 0 || size <= index) {
                return ICUResourceBundle.RES_BOGUS;
            }
            return (ICUResourceBundle.STRING_V2 << 28) |
                   reader.b16BitUnits.charAt(itemsOffset + index);
        }
        protected int getContainer32Resource(ICUResourceBundleReader reader, int index) {
            if (index < 0 || size <= index) {
                return ICUResourceBundle.RES_BOGUS;
            }
            return reader.getInt(itemsOffset + 4 * index);
        }
        int getResource(ICUResourceBundleReader reader, String resKey) {
            return getContainerResource(reader, Integer.parseInt(resKey));
        }
        Container() {
        }
    }
    private static final class Array extends Container {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer32Resource(reader, index);
        }
        Array(ICUResourceBundleReader reader, int offset) {
            offset = reader.getResourceByteOffset(offset);
            size = reader.getInt(offset);
            itemsOffset = offset + 4;
        }
    }
    private static final class Array16 extends Container {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer16Resource(reader, index);
        }
        Array16(ICUResourceBundleReader reader, int offset) {
            size = reader.b16BitUnits.charAt(offset);
            itemsOffset = offset + 1;
        }
    }
    static class Table extends Container {
        protected char[] keyOffsets;
        protected int[] key32Offsets;

        String getKey(ICUResourceBundleReader reader, int index) {
            if (index < 0 || size <= index) {
                return null;
            }
            return keyOffsets != null ?
                        reader.getKey16String(keyOffsets[index]) :
                        reader.getKey32String(key32Offsets[index]);
        }
        private static final int URESDATA_ITEM_NOT_FOUND = -1;
        int findTableItem(ICUResourceBundleReader reader, CharSequence key) {
            int mid, start, limit;
            int result;

            /* do a binary search for the key */
            start=0;
            limit=size;
            while(start<limit) {
                mid = (start + limit) >>> 1;
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
        @Override
        int getResource(ICUResourceBundleReader reader, String resKey) {
            return getContainerResource(reader, findTableItem(reader, resKey));
        }
        Table() {
        }
    }
    private static final class Table1632 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer32Resource(reader, index);
        }
        Table1632(ICUResourceBundleReader reader, int offset) {
            offset = reader.getResourceByteOffset(offset);
            keyOffsets = reader.getTableKeyOffsets(offset);
            size = keyOffsets.length;
            itemsOffset = offset + 2 * ((size + 2) & ~1);  // Skip padding for 4-alignment.
        }
    }
    private static final class Table16 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer16Resource(reader, index);
        }
        Table16(ICUResourceBundleReader reader, int offset) {
            keyOffsets = reader.getTable16KeyOffsets(offset);
            size = keyOffsets.length;
            itemsOffset = offset + 1 + size;
        }
    }
    private static final class Table32 extends Table {
        @Override
        int getContainerResource(ICUResourceBundleReader reader, int index) {
            return getContainer32Resource(reader, index);
        }
        Table32(ICUResourceBundleReader reader, int offset) {
            offset = reader.getResourceByteOffset(offset);
            key32Offsets = reader.getTable32KeyOffsets(offset);
            size = key32Offsets.length;
            itemsOffset = offset + 4 * (1 + size);
        }
    }

    // Resource cache ------------------------------------------------------ ***

    /**
     * Cache of some of one resource bundle's resources.
     * Avoids creating multiple Java objects for the same resource items,
     * including multiple copies of their contents.
     *
     * <p>Mutable objects must not be cached and then returned to the caller
     * because the cache must not be writable via the returned reference.
     *
     * <p>Resources are mapped by their resource integers.
     * Empty resources with offset 0 cannot be mapped.
     * Integers need not and should not be cached.
     * Multiple .res items may share resource offsets (genrb eliminates some duplicates).
     *
     * <p>This cache uses int[] and Object[] arrays to minimize object creation
     * and avoid auto-boxing.
     *
     * <p>Large resource objects are stored in SoftReferences.
     *
     * <p>For few resources, a small table is used with binary search.
     * When more resources are cached, then the data structure changes to be faster
     * but also use more memory.
     */
    private static final class ResourceCache {
        // Number of items to be stored in a simple array with binary search and insertion sort.
        private static final int SIMPLE_LENGTH = 32;

        // When more than SIMPLE_LENGTH items are cached,
        // then switch to a trie-like tree of levels with different array lengths.
        private static final int ROOT_BITS = 7;
        private static final int NEXT_BITS = 6;

        // Simple table, used when length >= 0.
        private int[] keys = new int[SIMPLE_LENGTH];
        private Object[] values = new Object[SIMPLE_LENGTH];
        private int length;

        // Trie-like tree of levels, used when length < 0.
        private int maxOffsetBits;
        /**
         * Number of bits in each level, each stored in a nibble.
         */
        private int levelBitsList;
        private Level rootLevel;

        @SuppressWarnings("unchecked")
        private static final Object putIfCleared(Object[] values, int index, Object item, int size) {
            Object value = values[index];
            if(!(value instanceof SoftReference)) {
                assert size < LARGE_SIZE;  // Caller should be consistent for each resource.
                return value;
            }
            assert size >= LARGE_SIZE;
            value = ((SoftReference<Object>)value).get();
            if(value != null) {
                return value;
            }
            values[index] = new SoftReference<Object>(item);
            return item;
        }

        private static final class Level {
            int levelBitsList;
            int shift;
            int mask;
            int[] keys;
            Object[] values;

            Level(int levelBitsList, int shift) {
                this.levelBitsList = levelBitsList;
                this.shift = shift;
                int bits = levelBitsList & 0xf;
                assert bits != 0;
                int length = 1 << bits;
                mask = length - 1;
                keys = new int[length];
                values = new Object[length];
            }

            Object get(int key) {
                int index = (key >> shift) & mask;
                int k = keys[index];
                if(k == key) {
                    return values[index];
                }
                if(k == 0) {
                    Level level = (Level)values[index];
                    if(level != null) {
                        return level.get(key);
                    }
                }
                return null;
            }

            Object putIfAbsent(int key, Object item, int size) {
                int index = (key >> shift) & mask;
                int k = keys[index];
                if(k == key) {
                    return putIfCleared(values, index, item, size);
                }
                if(k == 0) {
                    Level level = (Level)values[index];
                    if(level != null) {
                        return level.putIfAbsent(key, item, size);
                    }
                    keys[index] = key;
                    values[index] = (size >= LARGE_SIZE) ? new SoftReference<Object>(item) : item;
                    return item;
                }
                // Collision: Add a child level, move the old item there,
                // and then insert the current item.
                Level level = new Level(levelBitsList >> 4, shift + (levelBitsList & 0xf));
                int i = (k >> level.shift) & level.mask;
                level.keys[i] = k;
                level.values[i] = values[index];
                keys[index] = 0;
                values[index] = level;
                return level.putIfAbsent(key, item, size);
            }
        }

        ResourceCache(int maxOffset) {
            assert maxOffset != 0;
            maxOffsetBits = 28;
            while(maxOffset <= 0x7ffffff) {
                maxOffset <<= 1;
                --maxOffsetBits;
            }
            int keyBits = maxOffsetBits + 2;  // +2 for mini type: at most 30 bits used in a key
            // Precompute for each level the number of bits it handles.
            if(keyBits <= ROOT_BITS) {
                levelBitsList = keyBits;
            } else if(keyBits < (ROOT_BITS + 3)) {
                levelBitsList = 0x30 | (keyBits - 3);
            } else {
                levelBitsList = ROOT_BITS;
                keyBits -= ROOT_BITS;
                int shift = 4;
                for(;;) {
                    if(keyBits <= NEXT_BITS) {
                        levelBitsList |= keyBits << shift;
                        break;
                    } else if(keyBits < (NEXT_BITS + 3)) {
                        levelBitsList |= (0x30 | (keyBits - 3)) << shift;
                        break;
                    } else {
                        levelBitsList |= NEXT_BITS << shift;
                        keyBits -= NEXT_BITS;
                        shift += 4;
                    }
                }
            }
        }

        /**
         * Turns a resource integer (with unused bits in the middle)
         * into a key with fewer bits (at most keyBits).
         */
        private int makeKey(int res) {
            // It is possible for resources of different types in the 16-bit array
            // to share a start offset; distinguish between those with a 2-bit value,
            // as a tie-breaker in the bits just above the highest possible offset.
            // It is not possible for "regular" resources to share a start offset with each other,
            // but offsets for 16-bit and "regular" resources overlap;
            // use 2-bit value 0 for "regular" resources.
            int type = RES_GET_TYPE(res);
            int miniType =
                    (type == ICUResourceBundle.STRING_V2) ? 1 :
                        (type == ICUResourceBundle.TABLE16) ? 3 :
                            (type == ICUResourceBundle.ARRAY16) ? 2 : 0;
            return RES_GET_OFFSET(res) | (miniType << maxOffsetBits);
        }

        private int findSimple(int key) {
            // With Java 6, return Arrays.binarySearch(keys, 0, length, key).
            int start = 0;
            int limit = length;
            while((limit - start) > 8) {
                int mid = (start + limit) / 2;
                if(key < keys[mid]) {
                    limit = mid;
                } else {
                    start = mid;
                }
            }
            // For a small number of items, linear search should be a little faster.
            while(start < limit) {
                int k = keys[start];
                if(key < k) {
                    return ~start;
                }
                if(key == k) {
                    return start;
                }
                ++start;
            }
            return ~start;
        }

        @SuppressWarnings("unchecked")
        synchronized Object get(int res) {
            // Integers and empty resources need not be cached.
            // The cache itself uses res=0 for "no match".
            assert RES_GET_OFFSET(res) != 0;
            Object value;
            if(length >= 0) {
                int index = findSimple(res);
                if(index >= 0) {
                    value = values[index];
                } else {
                    return null;
                }
            } else {
                value = rootLevel.get(makeKey(res));
                if(value == null) {
                    return null;
                }
            }
            if(value instanceof SoftReference) {
                value = ((SoftReference<Object>)value).get();
            }
            return value;  // null if the reference was cleared
        }

        synchronized Object putIfAbsent(int res, Object item, int size) {
            if(length >= 0) {
                int index = findSimple(res);
                if(index >= 0) {
                    return putIfCleared(values, index, item, size);
                } else if(length < SIMPLE_LENGTH) {
                    index = ~index;
                    if(index < length) {
                        System.arraycopy(keys, index, keys, index + 1, length - index);
                        System.arraycopy(values, index, values, index + 1, length - index);
                    }
                    ++length;
                    keys[index] = res;
                    values[index] = (size >= LARGE_SIZE) ? new SoftReference<Object>(item) : item;
                    return item;
                } else /* not found && length == SIMPLE_LENGTH */ {
                    // Grow to become trie-like.
                    rootLevel = new Level(levelBitsList, 0);
                    for(int i = 0; i < SIMPLE_LENGTH; ++i) {
                        rootLevel.putIfAbsent(makeKey(keys[i]), values[i], 0);
                    }
                    keys = null;
                    values = null;
                    length = -1;
                }
            }
            return rootLevel.putIfAbsent(makeKey(res), item, size);
        }
    }

    private static final String ICU_RESOURCE_SUFFIX = ".res";

    /**
     * Gets the full name of the resource with suffix.
     */
    public static String getFullName(String baseName, String localeName) {
        if (baseName == null || baseName.length() == 0) {
            if (localeName.length() == 0) {
                return localeName = ULocale.getDefault().toString();
            }
            return localeName + ICU_RESOURCE_SUFFIX;
        } else {
            if (baseName.indexOf('.') == -1) {
                if (baseName.charAt(baseName.length() - 1) != '/') {
                    return baseName + "/" + localeName + ICU_RESOURCE_SUFFIX;
                } else {
                    return baseName + localeName + ICU_RESOURCE_SUFFIX;
                }
            } else {
                baseName = baseName.replace('.', '/');
                if (localeName.length() == 0) {
                    return baseName + ICU_RESOURCE_SUFFIX;
                } else {
                    return baseName + "_" + localeName + ICU_RESOURCE_SUFFIX;
                }
            }
        }
    }
}
