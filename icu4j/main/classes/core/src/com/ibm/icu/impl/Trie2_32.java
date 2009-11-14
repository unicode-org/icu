/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author aheninger
 *
 * A read-only Trie2, holding 32 bit data values.
 * 
 * A Trie2 is a highly optimized data structure for mapping from Unicode
 * code points (values ranging from 0 to 0x10ffff) to a 16 or 32 bit value.
 *
 * See class Trie2 for descriptions of the API for accessing the contents of a trie.
 * 
 * The fundamental data access methods are declared final in this class, with
 * the intent that applications might gain a little extra performance, when compared
 * with calling the same methods via the abstract UTrie2 base class.
 */

public class Trie2_32 extends Trie2 {
    
    /**
     * Internal constructor, not for general use.
     */
    Trie2_32() {
    }
    
    
    /**
     * Create a Trie2 from its serialized form.  Inverse of utrie2_serialize().
     * The serialized format is identical between ICU4C and ICU4J, so this function
     * will work with serialized Trie2s from either.
     * 
     * The serialized Trie2 on the stream may be in either little or big endian byte order.
     * This allows using serialized Tries from ICU4C without needing to consider the
     * byte order of the system that created them.
     *
     * @param is an input stream to the serialized form of a UTrie2.  
     * @return An unserialized Trie_32, ready for use.
     * @throws IllegalArgumentException if the stream does not contain a serialized Trie2.
     * @throws IOException if a read error occurs on the InputStream.
     * @throws ClassCastException if the stream contains a serialized Trie2_16
     */
    public static Trie2_32  createFromSerialized(InputStream is) throws IOException {
        return (Trie2_32) Trie2.createFromSerialized(is);
    }

    /**
     * Get the value for a code point as stored in the Trie2.
     *
     * @param codePoint the code point
     * @return the value
     */
    @Override
    public final int get(int codePoint) {
        int value;
        int ix;
        
        if (codePoint >= 0) {
            if (codePoint < 0x0d800 || (codePoint > 0x0dbff && codePoint <= 0x0ffff)) {
                // Ordinary BMP code point, excluding leading surrogates.
                // BMP uses a single level lookup.  BMP index starts at offset 0 in the Trie2 index.
                // 32 bit data is stored in the index array itself.
                ix = index[codePoint >> UTRIE2_SHIFT_2];
                ix = (ix << UTRIE2_INDEX_SHIFT) + (codePoint & UTRIE2_DATA_MASK);
                value = data32[ix];
                return value;
            } 
            if (codePoint <= 0xffff) {
                // Lead Surrogate Code Point.  A Separate index section is stored for
                // lead surrogate code units and code points.
                //   The main index has the code unit data.
                //   For this function, we need the code point data.
                // Note: this expression could be refactored for slightly improved efficiency, but
                //       surrogate code points will be so rare in practice that it's not worth it.
                ix = index[UTRIE2_LSCP_INDEX_2_OFFSET + ((codePoint - 0xd800) >> UTRIE2_SHIFT_2)];
                ix = (ix << UTRIE2_INDEX_SHIFT) + (codePoint & UTRIE2_DATA_MASK);
                value = data32[ix];
                return value;
            }
            if (codePoint < highStart) {
                // Supplemental code point, use two-level lookup.
                ix = (UTRIE2_INDEX_1_OFFSET - UTRIE2_OMITTED_BMP_INDEX_1_LENGTH) + (codePoint >> UTRIE2_SHIFT_1);
                ix = index[ix];
                ix += (codePoint >> UTRIE2_SHIFT_2) & UTRIE2_INDEX_2_MASK;
                ix = index[ix];
                ix = (ix << UTRIE2_INDEX_SHIFT) + (codePoint & UTRIE2_DATA_MASK);
                value = data32[ix];
                return value;
            }
            if (codePoint <= 0x10ffff) {
                value = data32[highValueIndex];
                return value;
            }
        }
        
        // Fall through.  The code point is outside of the legal range of 0..0x10ffff.
        return errorValue;
    }

    
    /**
     * Get a Trie2 value for a UTF-16 code unit.
     * 
     * This function returns the same value as get() if the input 
     * character is outside of the lead surrogate range
     * 
     * There are two values stored in a Trie2 for inputs in the lead
     * surrogate range.  This function returns the alternate value,
     * while Trie2.get() returns the main value.
     * 
     * @param codeUnit a 16 bit code unit or lead surrogate value.
     * @return the value
     */
    @Override
    public int getFromU16SingleLead(char codeUnit){
        int value;
        int ix;
        
        ix = index[codeUnit >> UTRIE2_SHIFT_2];
        ix = (ix << UTRIE2_INDEX_SHIFT) + (codeUnit & UTRIE2_DATA_MASK);
        value = data32[ix];
        return value;

    }
    
    /**
     * Serialize a Trie2_32 onto an OutputStream.
     * 
     * A Trie2 can be serialized multiple times.
     * The serialized data is compatible with ICU4C UTrie2 serialization.
     * Trie2 serialization is unrelated to Java object serialization.
     *  
     * @param os the stream to which the serialized Trie2 data will be written.
     * @return the number of bytes written.
     * @throw IOException on an error writing to the OutputStream.
     */
    public int serialize(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        int  bytesWritten = 0;
        
        bytesWritten += serializeHeader(dos);        
        for (int i=0; i<dataLength; i++) {
            dos.writeInt(data32[i]);
        }
        bytesWritten += dataLength*4;
        return bytesWritten;
    }
}

