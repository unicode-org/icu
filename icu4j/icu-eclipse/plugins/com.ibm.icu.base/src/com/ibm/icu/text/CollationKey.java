/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
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
 * @stable ICU 2.8 
 */
public final class CollationKey implements Comparable {
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

    /**
     * Return the source string that this CollationKey represents.
     * @return source string that this CollationKey represents
     * @stable ICU 2.8
     */
    public String getSourceString() {
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
    public byte[] toByteArray() {
        return key.toByteArray();
    }

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
    public int compareTo(CollationKey target) {
        return key.compareTo(target.key);
    }

    /**
     * <p>Compare this CollationKey with the specified Object.  The
     * collation rules of the Collator that created this key are
     * applied.</p>
     * 
     * <p>See note in compareTo(CollationKey) for warnings about possible
     * incorrect results.</p>
     *
     * @param obj the Object to be compared to.
     * @return Returns a negative integer, zero, or a positive integer 
     *         respectively if this CollationKey is less than, equal to, or 
     *         greater than the given Object.
     * @exception ClassCastException is thrown when the argument is not 
     *            a CollationKey.  NullPointerException is thrown when the argument 
     *            is null.
     * @see #compareTo(CollationKey)
     * @stable ICU 2.8 
     */
    public int compareTo(Object o) {
        return compareTo((CollationKey)o);
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
    public boolean equals(Object target) {
        try {
            return key.equals(((CollationKey)target).key);
        }
        catch (Exception e) {
            return false;
        }
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
    public boolean equals(CollationKey target) {
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
    public int hashCode() {
        return key.hashCode();
    }

    /**
     * Return a description of the CollationKey.
     * @return a description of the CollationKey, used for debugging
     * @stable ICU 3.4.2
     */
    public String toString() {
        return key.toString();
    }
}
