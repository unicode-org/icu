/**
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/RawCollationKey.java,v $ 
 * $Date: 2003/09/23 04:16:47 $ 
 * $Revision: 1.2 $
 *
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
 * @draft ICU 2.8
 * @see RuleBasedCollator
 * @see CollationKey
 */
public final class RawCollationKey extends ByteArrayWrapper 
{
    // public constructors --------------------------------------------------
    
    /**
     * Default constructor, internal byte array is null and its size set to 0.
     * @draft ICU 2.8
     */
    public RawCollationKey() 
    {
    }

    /**
     * RawCollationKey created with an empty internal byte array of length 
     * capacity. Size of the internal byte array will be set to 0.
     * @param capacity length of internal byte array
     * @draft ICU 2.8
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
    
    // public method --------------------------------------------------------

    /**
     * <p>
     * Compares this RawCollationKey object to the target RawCollationKey 
     * object. The collation rules of the Collator that created this key are
     * applied.
     * </p>
     * <p><strong>Note:</strong> Comparison between RawCollationKeys created by 
     * different Collators might return incorrect results.  
     * See class documentation.</p>
     * @param target RawCollationKey to be compared with
     * @return 0 if the sort order is the same,
     *         &lt; 0 if this RawCollationKey has a smaller sort order than 
     *                target,
     *         &gt; 0 if this RawCollationKey has a bigger sort order than 
     *                target.
     * @draft ICU 2.8 
     */
    public int compareTo(RawCollationKey target) 
    {
        int i = 0;
        while (bytes[i] != 0 && target.bytes[i] != 0) {
            byte key = bytes[i];
            byte targetkey = target.bytes[i];
            if (key == targetkey) {
                i ++;
                continue;
            }
            if (key >= 0) {
                if (targetkey < 0 || key < targetkey) {
                    return -1;
                }
                // target key has to be positive and less than key
                return 1;
            }
            else {
                // key is negative
                if (targetkey >= 0 || key > targetkey) {
                    return 1;
                }
                return -1;
            }
        }
        // last comparison if we encounter a 0
        if (bytes[i] == target.bytes[i]) {
            return 0;
        }
        if (bytes[i] == 0) {
            return -1;
        }
        // target is 0
        return 1;
    }
}
