//##header J2SE15
/*
 *******************************************************************************
 * Copyright (C) 2004-2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;



/**
 * This class reads the *.res resource bundle format
 *
 * (For the latest version of the file format documentation see
 * ICU4C's source/common/uresdata.h file.)
 *
 * File format for .res resource bundle files (formatVersion=1.2)
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
 * Direct values:
 * - Empty Unicode strings have an offset value of 0 in the Resource handle itself.
 * - Integer values are 28-bit values stored in the Resource handle itself;
 *   the interpretation of unsigned vs. signed integers is up to the application.
 *
 * All other types and values use 28-bit offsets to point to the item's data.
 * The offset is an index to the first 32-bit word of the value, relative to the
 * start of the resource data (i.e., the root item handle is at offset 0).
 * To get byte offsets, the offset is multiplied by 4 (or shifted left by 2 bits).
 * All resource item values are 4-aligned.
 *
 * The structures (memory layouts) for the values for each item type are listed
 * in the table above.
 *
 * Nested, hierarchical structures: -------------
 *
 * Table items contain key-value pairs where the keys are 16-bit offsets to char * key strings.
 * Key string offsets are also relative to the start of the resource data (of the root handle),
 * i.e., the first string has an offset of 4 (after the 4-byte root handle).
 *
 * The values of these pairs are Resource handles.
 *
 * Array items are simple vectors of Resource handles.
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
 * Type Name            Memory layout of values
 *                      (in parentheses: scalar, non-offset values)
 *
 * 0  Unicode String:   int32_t length, UChar[length], (UChar)0, (padding)
 *                  or  (empty string ("") if offset==0)
 * 1  Binary:           int32_t length, uint8_t[length], (padding)
 *                      - this value should be 32-aligned -
 * 2  Table:            uint16_t count, uint16_t keyStringOffsets[count], (uint16_t padding), Resource[count]
 * 3  Alias:            (physically same value layout as string, new in ICU 2.4)
 * 4  Table32:          int32_t count, int32_t keyStringOffsets[count], Resource[count]
 *                      (new in formatVersion 1.1/ICU 2.8)
 *
 * 7  Integer:          (28-bit offset is integer value)
 * 8  Array:            int32_t count, Resource[count]
 *
 * 14 Integer Vector:   int32_t length, int32_t[length]
 * 15 Reserved:         This value denotes special purpose resources and is for internal use.
 *
 * Note that there are 3 types with data vector values:
 * - Vectors of 8-bit bytes stored as type Binary.
 * - Vectors of 16-bit words stored as type Unicode String
 *                     (no value restrictions, all values 0..ffff allowed!).
 * - Vectors of 32-bit words stored as type Integer Vector.
 *
 *
 */
public final class ICUResourceBundleReader implements ICUBinary.Authenticate{

    /**
     * File format version that this class understands.
     * "ResB"
     */
    private static final byte DATA_FORMAT_ID[] = {(byte)0x52, (byte)0x65, 
                                                     (byte)0x73, (byte)0x42};

    private static final String ICU_RESOURCE_SUFFIX = ".res";
    
    /* indexes[] value names; indexes are generally 32-bit (Resource) indexes */
    private static final int    URES_INDEX_LENGTH           = 0;        /* [0] contains URES_INDEX_TOP==the length of indexes[] */
    //private static final int    URES_INDEX_STRINGS_TOP      = 1;        /* [1] contains the top of the strings, */
                                                                        /*     same as the bottom of resources, rounded up */
    //private static final int    URES_INDEX_RESOURCES_TOP    = 2;        /* [2] contains the top of all resources */
    private static final int    URES_INDEX_BUNDLE_TOP       = 3;        /* [3] contains the top of the bundle, */
                                                                        /*     in case it were ever different from [2] */
    //private static final int    URES_INDEX_MAX_TABLE_LENGTH = 4;        /* [4] max. length of any table */
    private static final int    URES_INDEX_ATTRIBUTES       = 5;        /* [5] attributes bit set, see URES_ATT_* (new in formatVersion 1.2) */
    //private static final int    URES_INDEX_TOP              = 6;

    //private static final int    URES_STRINGS_BOTTOM=(1+URES_INDEX_TOP)*4;

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

    private static final boolean DEBUG = false;
    
    private byte[] /* formatVersion, */ dataVersion;

    private int rootRes;
    private int[] indexes;
    private boolean noFallback; /* see URES_ATT_NO_FALLBACK */

    private byte[] data;

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
//#if defined(FOUNDATION10) || defined(J2SE13)
//##            throw new RuntimeException("Data file "+ resolvedName+ " is corrupt.");   
//#else
            throw new RuntimeException("Data file "+ resolvedName+ " is corrupt.", ex);   
//#endif
        }
    }
    public static ICUResourceBundleReader getReader(String baseName, String localeName, ClassLoader root){
        String resolvedName = getFullName(baseName, localeName);
        InputStream stream = ICUData.getStream(root,resolvedName);
        
        if(stream==null){
            return null;
        }
        ICUResourceBundleReader reader = new ICUResourceBundleReader(stream, resolvedName);
        return reader;
    }

    private static void writeInt(int i, byte[] bytes, int offset) {
        bytes[offset++]=(byte)(i>>24);
        bytes[offset++]=(byte)(i>>16);
        bytes[offset++]=(byte)(i>>8);
        bytes[offset]=(byte)i;
    }

    private void readData(InputStream stream)
            throws IOException{
        
        DataInputStream ds = new DataInputStream(stream);

        if(DEBUG) System.out.println("The DataInputStream class is: " + ds.getClass().getName());
        if(DEBUG) System.out.println("The available bytes in the stream before reading the data: "+ds.available());

        /*
         * The following will read two integers before ds.mark().
         * Later, the two integers need to be placed into data[],
         * then ds.reset(), then ds.readFully(into rest of data[]).
         *
         * This is necessary because we don't know the readLimit for ds.mark()
         * until we have read the second integer (indexLength).
         */
        rootRes = ds.readInt();

        // read the variable-length indexes[] array
        int indexLength = ds.readInt();
        ds.mark((indexLength-1)*4);

        indexes = new int[indexLength];
        indexes[URES_INDEX_LENGTH] = indexLength;

        for(int i=1; i<indexLength; i++){
            indexes[i] = ds.readInt();   
        }

        // determine if this resource bundle falls back to a parent bundle
        // along normal locale ID fallback
        noFallback =
            indexLength > URES_INDEX_ATTRIBUTES &&
            (indexes[URES_INDEX_ATTRIBUTES]&URES_ATT_NO_FALLBACK)!=0;

        // read the entire bundle (after the header) into data[]
        // put rootRes and indexLength into data[0..7]
        // and the rest of the data into data[8..length-1]
        int length = indexes[URES_INDEX_BUNDLE_TOP]*4;
        if(DEBUG) System.out.println("The number of bytes in the bundle: "+length);
    
        data = new byte[length];
        writeInt(rootRes, data, 0);
        writeInt(indexLength, data, 4);

        // now reset to the mark, which was set after reading rootRes and indexLength
        ds.reset();
        ds.readFully(data, 8, length-8);
    }

    /**
     * Gets the full name of the resource with suffix.
     */
    public static String getFullName(String baseName, String localeName){
        if(baseName==null || baseName.length()==0){
            if(localeName.length()==0){
                return ULocale.getDefault().toString()+ICU_RESOURCE_SUFFIX;   
            }else{
                return localeName+ICU_RESOURCE_SUFFIX;
            }
        }else{
            if(baseName.indexOf('.')==-1){
                if(baseName.charAt(baseName.length()-1)!= '/'){
                    return baseName+"/"+localeName+ICU_RESOURCE_SUFFIX;
                }else{
                    return baseName+localeName+ICU_RESOURCE_SUFFIX;   
                }
            }else{
                baseName = baseName.replace('.','/');
                if(localeName.length()==0){
                    return baseName+ICU_RESOURCE_SUFFIX;   
                }else{
                    return baseName+"_"+localeName+ICU_RESOURCE_SUFFIX;
                }
            }
        }
    }
    
    public VersionInfo getVersion(){
        return VersionInfo.getInstance(dataVersion[0],dataVersion[1],dataVersion[2],dataVersion[3]);   
    }
    public boolean isDataVersionAcceptable(byte version[]){
        // while ICU4C can read formatVersion 1.0 and up,
        // ICU4J requires 1.1 as a minimum
        // formatVersion = version;
        return version[0] == 1 && version[1] >= 1;
    }
    
    public byte[] getData(){
        return data;   
    }
    public int getRootResource() {
        return rootRes;
    }
    public boolean getNoFallback() {
        return noFallback;
    }
}
