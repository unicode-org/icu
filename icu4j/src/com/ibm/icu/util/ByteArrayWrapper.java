/**
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

/**
 * <p>
 * A simple utility class to wrap a byte array.
 * </p>
 * <p>
 * Generally passed as an argument object into a method. The method takes
 * responsibility of writing into the internal byte array and increasing its
 * size when necessary.
 * </p> 
 * @author syn wee
 * @draft ICU 2.8
 */
public class ByteArrayWrapper 
{
    // public data member ------------------------------------------------
    
    /**
     * Internal byte array.
     * @draft ICU 2.8
     */
    public byte[] bytes;
    /**
     * Size of the internal byte array used. 
     * Different from bytes.length, size will be &lt;= bytes.length. 
     * Semantics of size is similar to java.util.Vector.size().
     * @draft ICU 2.8
     */
    public int size;
    
    // public constructor ------------------------------------------------

    /** 
     * Construct a new ByteArrayWrapper with no data.
     * @draft ICU 2.8
     */
    public ByteArrayWrapper() {
    }

    // public methods ----------------------------------------------------

    /**
     * Ensure that the internal byte array is at least of length capacity.     
     * If the byte array is null or its length is less than capacity, a new 
     * byte array of length capacity will be allocated.  
     * The contents of the array (between 0 and size) remain unchanged. 
     * @param capacity minimum length of internal byte array.
     * @draft ICU 2.8 
     */
    public void ensureCapacity(int capacity) 
    {
        if (bytes == null || bytes.length < capacity) {
            byte[] newbytes = new byte[capacity];
            copyBytes(bytes, 0, newbytes, 0, size);
            bytes = newbytes;
        }
    }
    
    /**
     * Set the internal byte array from offset 0 to (limit - start) with the 
     * contents of src from offset start to limit. If the byte array is null or its length is less than capacity, a new 
     * byte array of length (limit - start) will be allocated.  
     * This resets the size of the internal byte array to (limit - start).
     * @param src source byte array to copy from
     * @param start start offset of src to copy from
     * @param limit end + 1 offset of src to copy from
     * @draft ICU 2.8
     */
    public final void set(byte[] src, int start, int limit) 
    {
        size = 0;
        append(src, start, limit);
    }
    
    /**
     * Appends the internal byte array from offset size with the 
     * contents of src from offset start to limit. This increases the size of
     * the internal byte array to (size + limit - start).
     * @param src source byte array to copy from
     * @param start start offset of src to copy from
     * @param limit end + 1 offset of src to copy from
     * @draft ICU 2.8
     */
    public final void append(byte[] src, int start, int limit) 
    {
        int len = limit - start;
        ensureCapacity(size + len);
        copyBytes(src, start, bytes, size, len);
        size += len;
    }

    /**
     * Releases the internal byte array to the caller, resets the internal
     * byte array to null and its size to 0.
     * @return internal byte array.
     * @draft ICU 2.8
     */
    public final byte[] releaseBytes()
    {
        byte result[] = bytes;
        bytes = null;
        size = 0;
        return result;
    }   
    
    // private methods -----------------------------------------------------
    
    /**
     * Copies the contents of src byte array from offset srcoff to the 
     * target of tgt byte array at the offset tgtoff.
     * @param src source byte array to copy from
     * @param srcoff start offset of src to copy from
     * @param tgt target byte array to copy to
     * @param tgtoff start offset of tgt to copy to
     * @param length size of contents to copy
     */
    private static final void copyBytes(byte[] src, int srcoff, byte[] tgt, 
                                       int tgtoff, int length) {
        if (length < 64) {
            for (int i = srcoff, n = tgtoff; -- length >= 0; ++ i, ++ n) {
                tgt[n] = src[i];
            }
        } 
        else {
            System.arraycopy(src, srcoff, tgt, tgtoff, length);
        }
    }      
}
