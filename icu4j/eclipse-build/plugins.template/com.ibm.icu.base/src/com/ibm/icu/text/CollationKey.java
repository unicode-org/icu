// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 1996-2012, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;

/**
 * <p>A <code>CollationKey</code> represents a <code>String</code>
 * under the rules of a specific <code>Collator</code>
 * object. Comparing two <code>CollationKey</code>s returns the
 * relative order of the <code>String</code>s they represent.</p>
 *
 * <p>Since the rule set of <code>Collator</code>s can differ, the
 * sort orders of the same string under two different
 * <code>Collator</code>s might differ.  Hence comparing
 * <code>CollationKey</code>s generated from different
 * <code>Collator</code>s can give incorrect results.</p>
 
 * <p>Both the method
 * <code>CollationKey.compareTo(CollationKey)</code> and the method
 * <code>Collator.compare(String, String)</code> compare two strings
 * and returns their relative order.  The performance characterictics
 * of these two approaches can differ.</p>
 *
 * <p>During the construction of a <code>CollationKey</code>, the
 * entire source string is examined and processed into a series of
 * bits terminated by a null, that are stored in the <code>CollationKey</code>. 
 * When <code>CollationKey.compareTo(CollationKey)</code> executes, it
 * performs bitwise comparison on the bit sequences.  This can incurs
 * startup cost when creating the <code>CollationKey</code>, but once
 * the key is created, binary comparisons are fast.  This approach is
 * recommended when the same strings are to be compared over and over
 * again.</p>
 *
 * <p>On the other hand, implementations of
 * <code>Collator.compare(String, String)</code> can examine and
 * process the strings only until the first characters differing in
 * order.  This approach is recommended if the strings are to be
 * compared only once.</p>
 * 
 * <p>More information about the composition of the bit sequence can
 * be found in the 
 * <a href="http://www.icu-project.org/userguide/Collate_ServiceArchitecture.html">
 * user guide</a>.</p>
 *
 * <p>The following example shows how <code>CollationKey</code>s can be used
 * to sort a list of <code>String</code>s.</p>
 * <blockquote>
 * <pre>
 * // Create an array of CollationKeys for the Strings to be sorted.
 * Collator myCollator = Collator.getInstance();
 * CollationKey[] keys = new CollationKey[3];
 * keys[0] = myCollator.getCollationKey("Tom");
 * keys[1] = myCollator.getCollationKey("Dick");
 * keys[2] = myCollator.getCollationKey("Harry");
 * sort( keys );
 * <br>
 * //...
 * <br>
 * // Inside body of sort routine, compare keys this way
 * if( keys[i].compareTo( keys[j] ) > 0 )
 *    // swap keys[i] and keys[j]
 * <br>
 * //...
 * <br>
 * // Finally, when we've returned from sort.
 * System.out.println( keys[0].getSourceString() );
 * System.out.println( keys[1].getSourceString() );
 * System.out.println( keys[2].getSourceString() );
 * </pre>
 * </blockquote>
 * </p>
 * <p>
 * This class is not subclassable
 * </p>
 * @see Collator
 * @see RuleBasedCollator
 * @author Syn Wee Quek
 * @stable ICU 2.8 
 */
public final class CollationKey implements Comparable<CollationKey>
{
    /**
     * @internal
     */
    final java.text.CollationKey key;

    /**
     * @internal
     */
    CollationKey(java.text.CollationKey delegate) {
        this.key = delegate;
    }

    // public inner classes -------------------------------------------------
    
//    /** 
//     * Options that used in the API CollationKey.getBound() for getting a 
//     * CollationKey based on the bound mode requested.
//     * @stable ICU 2.6
//     */
//    public static final class BoundMode 
//    {
//        /*
//         * do not change the values assigned to the members of this enum. 
//         * Underlying code depends on them having these numbers  
//         */
//         
//        /** 
//         * Lower bound
//         * @stable ICU 2.6
//         */
//        public static final int LOWER = 0;
//
//        /** 
//         * Upper bound that will match strings of exact size
//         * @stable ICU 2.6
//         */
//        public static final int UPPER = 1;
//
//        /** 
//         * Upper bound that will match all the strings that have the same 
//         * initial substring as the given string
//         * @stable ICU 2.6
//         */
//        public static final int UPPER_LONG = 2;
//
//        /**
//         * Number of bound mode
//         * @stable ICU 2.6
//         */
//        public static final int COUNT = 3;
//        
//        /**
//         * Private Constructor
//         */
//        ///CLOVER:OFF
//        private BoundMode(){}
//        ///CLOVER:ON
//    }
    
    // public constructor ---------------------------------------------------
    
//    /**
//     * CollationKey constructor.
//     * This constructor is given public access, unlike the JDK version, to
//     * allow access to users extending the Collator class. See 
//     * {@link Collator#getCollationKey(String)}. 
//     * @param source string this CollationKey is to represent
//     * @param key array of bytes that represent the collation order of argument
//     *            source terminated by a null
//     * @see Collator
//     * @stable ICU 2.8
//     */
//    public CollationKey(String source, byte key[])
//    {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * CollationKey constructor that forces key to release its internal byte 
//     * array for adoption. key will have a null byte array after this 
//     * construction.
//     * @param source string this CollationKey is to represent
//     * @param key RawCollationKey object that represents the collation order of 
//     *            argument source. 
//     * @see Collator
//     * @see RawCollationKey
//     * @stable ICU 2.8 
//     */
//    public CollationKey(String source, RawCollationKey key)
//    {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

    // public getters -------------------------------------------------------
    
    /**
     * Return the source string that this CollationKey represents.
     * @return source string that this CollationKey represents
     * @stable ICU 2.8
     */
    public String getSourceString() 
    {
        return key.getSourceString();
    }

    /**
     * <p>Duplicates and returns the value of this CollationKey as a sequence 
     * of big-endian bytes terminated by a null.</p> 
     *
     * <p>If two CollationKeys can be legitimately compared, then one can
     * compare the byte arrays of each to obtain the same result, e.g.
     * <pre>
     * byte key1[] = collationkey1.toByteArray();
     * byte key2[] = collationkey2.toByteArray();
     * int key, targetkey;
     * int i = 0;
     * do {
     *       key = key1[i] & 0xFF;
     *     targetkey = key2[i] & 0xFF;
     *     if (key &lt; targetkey) {
     *         System.out.println("String 1 is less than string 2");
     *         return;
     *     }
     *     if (targetkey &lt; key) {
     *         System.out.println("String 1 is more than string 2");
     *     }
     *     i ++;
     * } while (key != 0 && targetKey != 0);
     *
     * System.out.println("Strings are equal.");
     * </pre>
     * </p>  
     * @return CollationKey value in a sequence of big-endian byte bytes 
     *         terminated by a null.
     * @stable ICU 2.8
     */
    public byte[] toByteArray() 
    {
        return key.toByteArray();
    }

    // public other methods -------------------------------------------------    
     
    /**
     * <p>Compare this CollationKey to another CollationKey.  The
     * collation rules of the Collator that created this key are
     * applied.</p>
     *
     * <p><strong>Note:</strong> Comparison between CollationKeys
     * created by different Collators might return incorrect
     * results.  See class documentation.</p>
     *
     * @param target target CollationKey
     * @return an integer value.  If the value is less than zero this CollationKey
     *         is less than than target, if the value is zero they are equal, and
     *         if the value is greater than zero this CollationKey is greater 
     *         than target.
     * @exception NullPointerException is thrown if argument is null.
     * @see Collator#compare(String, String)
     * @stable ICU 2.8 
     */
    public int compareTo(CollationKey target)
    {
    	return key.compareTo(target.key);
    }

    /**
     * <p>Compare this CollationKey and the specified Object for
     * equality.  The collation rules of the Collator that created
     * this key are applied.</p>
     *
     * <p>See note in compareTo(CollationKey) for warnings about
     * possible incorrect results.</p>
     *
     * @param target the object to compare to.
     * @return true if the two keys compare as equal, false otherwise.
     * @see #compareTo(CollationKey)
     * @exception ClassCastException is thrown when the argument is not 
     *            a CollationKey.  NullPointerException is thrown when the argument 
     *            is null.
     * @stable ICU 2.8 
     */
    public boolean equals(Object target) 
    {
        if (!(target instanceof CollationKey)) {
            return false;
        }
        
        return equals((CollationKey)target);
    }
    
    /**
     * <p>
     * Compare this CollationKey and the argument target CollationKey for 
     * equality.
     * The collation 
     * rules of the Collator object which created these objects are applied.
     * </p>
     * <p>
     * See note in compareTo(CollationKey) for warnings of incorrect results
     * </p>
     * @param target the CollationKey to compare to.
     * @return true if two objects are equal, false otherwise.
     * @exception NullPointerException is thrown when the argument is null.
     * @stable ICU 2.8
     */
    public boolean equals(CollationKey target) 
    {
    	return key.equals(target.key);
    }

    /**
     * <p>Returns a hash code for this CollationKey. The hash value is calculated 
     * on the key itself, not the String from which the key was created. Thus 
     * if x and y are CollationKeys, then x.hashCode(x) == y.hashCode() 
     * if x.equals(y) is true. This allows language-sensitive comparison in a 
     * hash table.
     * </p>
     * @return the hash value.
     * @stable ICU 2.8
     */
    public int hashCode() 
    {
    	return key.hashCode();
    }
    
//    /**
//     * <p>
//     * Produce a bound for the sort order of a given collation key and a 
//     * strength level. This API does not attempt to find a bound for the 
//     * CollationKey String representation, hence null will be returned in its 
//     * place.
//     * </p>
//     * <p>
//     * Resulting bounds can be used to produce a range of strings that are
//     * between upper and lower bounds. For example, if bounds are produced
//     * for a sortkey of string "smith", strings between upper and lower 
//     * bounds with primary strength would include "Smith", "SMITH", "sMiTh".
//     * </p>
//     * <p>
//     * There are two upper bounds that can be produced. If BoundMode.UPPER
//     * is produced, strings matched would be as above. However, if a bound
//     * is produced using BoundMode.UPPER_LONG is used, the above example will
//     * also match "Smithsonian" and similar.
//     * </p>
//     * <p>
//     * For more on usage, see example in test procedure 
//     * <a href="http://source.icu-project.org/repos/icu/icu4j/trunk/src/com/ibm/icu/dev/test/collator/CollationAPITest.java">
//     * src/com/ibm/icu/dev/test/collator/CollationAPITest/TestBounds.
//     * </a>
//     * </p>
//     * <p>
//     * Collation keys produced may be compared using the <TT>compare</TT> API.
//     * </p>
//     * @param boundType Mode of bound required. It can be BoundMode.LOWER, which 
//     *              produces a lower inclusive bound, BoundMode.UPPER, that 
//     *              produces upper bound that matches strings of the same 
//     *              length or BoundMode.UPPER_LONG that matches strings that 
//     *              have the same starting substring as the source string.
//     * @param noOfLevels Strength levels required in the resulting bound 
//     *                 (for most uses, the recommended value is PRIMARY). This
//     *                 strength should be less than the maximum strength of 
//     *                 this CollationKey.
//     *                 See users guide for explanation on the strength levels a 
//     *                 collation key can have. 
//     * @return the result bounded CollationKey with a valid sort order but 
//     *         a null String representation.
//     * @exception IllegalArgumentException thrown when the strength level 
//     *            requested is higher than or equal to the strength in this
//     *            CollationKey. 
//     *            In the case of an Exception, information 
//     *            about the maximum strength to use will be returned in the 
//     *            Exception. The user can then call getBound() again with the 
//     *            appropriate strength.
//     * @see CollationKey
//     * @see CollationKey.BoundMode
//     * @see Collator#PRIMARY
//     * @see Collator#SECONDARY
//     * @see Collator#TERTIARY
//     * @see Collator#QUATERNARY
//     * @see Collator#IDENTICAL
//     * @stable ICU 2.6
//     */
//    public CollationKey getBound(int boundType, int noOfLevels) 
//    {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }
    
    /** 
     * <p>
     * Merges this CollationKey with another. Only the sorting order of the 
     * CollationKeys will be merged. This API does not attempt to merge the 
     * String representations of the CollationKeys, hence null will be returned
     * as the String representation.
     * </p>
     * <p>
     * The strength levels are merged with their corresponding counterparts 
     * (PRIMARIES with PRIMARIES, SECONDARIES with SECONDARIES etc.). 
     * </p>
     * <p>
     * The merged String representation of the result CollationKey will be a
     * concatenation of the String representations of the 2 source 
     * CollationKeys.
     * </p>
     * <p>
     * Between the values from the same level a separator is inserted.
     * example (uncompressed):
     * <pre> 
     * 191B1D 01 050505 01 910505 00 and 1F2123 01 050505 01 910505 00
     * will be merged as 
     * 191B1D 02 1F212301 050505 02 050505 01 910505 02 910505 00
     * </pre>
     * </p>
     * <p>
     * This allows for concatenating of first and last names for sorting, among 
     * other things.
     * </p>
     * </p>
     * @param source CollationKey to merge with 
     * @return a CollationKey that contains the valid merged sorting order 
     *         with a null String representation, 
     *         i.e. <tt>new CollationKey(null, merge_sort_order)</tt>
     * @exception IllegalArgumentException thrown if source CollationKey
     *            argument is null or of 0 length.
     * @stable ICU 2.6
     */
    public CollationKey merge(CollationKey source)
    {
        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
    }
}
