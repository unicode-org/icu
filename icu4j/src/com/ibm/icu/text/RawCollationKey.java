/**
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.util.ByteArrayWrapper;

/**
 * <p>
 * Simple class wrapper to store the internal byte representation of a 
 * CollationKey. Unlike the CollationKey, this class do not contain information 
 * on the source string the sort order represents. RawCollationKey is mutable 
 * and users can reuse its objects with the method in 
 * RuleBasedCollator.getRawCollationKey(..).
 * </p>
 * <p>
 * Please refer to the documentation on CollationKey for a detail description
 * on the internal byte representation. Note the internal byte representation 
 * is always null-terminated.
 * </p> 
 * <code>
 * Example of use:<br>
 * String str[] = {.....};
 * RuleBasedCollator collator = (RuleBasedCollator)Collator.getInstance();
 * RawCollationKey key = new RawCollationKey(128);
 * for (int i = 0; i &lt; str.length; i ++) {
 *     collator.getRawCollationKey(str[i], key);
 *     // do something with key.bytes
 * }
 * </code>
 * <p><strong>Note:</strong> Comparison between RawCollationKeys created by 
 * different Collators might return incorrect results.  
 * See class documentation for Collator.</p>
 * @draft ICU 2.8
 * @deprecated This is a draft API and might change in a future release of ICU.
 * @see RuleBasedCollator
 * @see CollationKey
 */
public final class RawCollationKey extends ByteArrayWrapper
{
    // public constructors --------------------------------------------------
    
    /**
     * Default constructor, internal byte array is null and its size set to 0.
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public RawCollationKey() 
    {
    }

    /**
     * RawCollationKey created with an empty internal byte array of length 
     * capacity. Size of the internal byte array will be set to 0.
     * @param capacity length of internal byte array
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public RawCollationKey(int capacity) 
    {
        bytes = new byte[capacity];
    }

    /**
     * RawCollationKey created, adopting bytes as the internal byte array.
     * Size of the internal byte array will be set to 0.
     * @param bytes byte array to be adopted by RawCollationKey
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public RawCollationKey(byte[] bytes) 
    {
        this.bytes = bytes;
    }
    
    /**
     * RawCollationKey created, adopting bytes as the internal byte array.
     * Size of the internal byte array will be set to size.
     * @param bytes byte array to be adopted by RawCollationKey
     * @param size non-negative designated size of bytes
     * @exception ArrayIndexOutOfBoundsException thrown if size is &gt; 
     *            bytes.length
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public RawCollationKey(byte[] bytes, int size) 
    {
        if (size < 0 || (size != 0 && bytes == null) 
            || (bytes.length < size)) {
            throw new ArrayIndexOutOfBoundsException(
                                            "Expected bytes.length >= size");
        }
        this.bytes = bytes;
        this.size = size;
    }
    
}
