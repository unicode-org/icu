/**
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/RawCollationKey.java,v $ 
 * $Date: 2003/09/22 06:24:20 $ 
 * $Revision: 1.1 $
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
 * on the internal byte representation.
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
     * Default constructor, internal byte array is null.
     * @draft ICU 2.8
     */
    public RawCollationKey() 
    {
    }

    /**
     * RawCollationKey created with an empty internal byte array of length 
     * capacity 
     * @param capacity length of internal byte array
     * @draft ICU 2.8
     */
    public RawCollationKey(int capacity) 
    {
        bytes = new byte[capacity];
    }

    /**
     * RawCollationKey created taking bytes as the internal byte array 
     * @param bytes
     * @draft ICU 2.8
     */
    public RawCollationKey(byte[] bytes) 
    {
        this.bytes = bytes;
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
