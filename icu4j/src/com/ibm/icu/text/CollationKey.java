/**
*******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/CollationKey.java,v $ 
* $Date: 2002/05/16 20:04:49 $ 
* $Revision: 1.5 $
*
*******************************************************************************
*/
package com.ibm.icu.text;

import java.util.Arrays;

/**
 * <p>A <code>CollationKey</code> represents a <code>String</code> under the
 * rules of a specific <code>Collator</code> object. Comparing two
 * <code>CollationKey</code>s returns the relative order of the
 * <code>String</code>s they represent. Using <code>CollationKey</code>s to 
 * compare <code>String</code>s is generally faster than using 
 * <code>Collator.compare</code>. Thus, when the <code>String</code>s must be 
 * compared multiple times, for example when sorting a list of 
 * <code>String</code>s. It's more efficient to use <code>CollationKey</code>s.
 * </p>
 * <p>You can not create <code>CollationKey</code>s directly. Rather, generate 
 * them by calling <code>Collator.getCollationKey(String)</code>. You can only 
 * compare <code>CollationKey</code>s generated from the same 
 * <code>Collator</code> object.</p>
 * <p>Generating a <code>CollationKey</code> for a <code>String</code>
 * involves examining the entire <code>String</code> and converting it to 
 * series of bits that can be compared bitwise. This allows fast comparisons 
 * once the keys are generated. The cost of generating keys is recouped in 
 * faster comparisons when <code>String</code>s need to be compared many 
 * times. On the other hand, the result of a comparison is often determined by 
 * the first couple of characters of each <code>String</code>.
 * <code>Collator.compare(String, String)</code> examines only as many characters as it needs 
 * which allows it to be faster when doing single comparisons.</p>
 * <p>The following example shows how <code>CollationKey</code>s might be used
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
 *
 * @see Collator
 * @see RuleBasedCollator
 * @author Syn Wee Quek
 * @since release 2.2, April 18 2002
 * @draft 2.2
 */
public final class CollationKey implements Comparable 
{
	// public methods -------------------------------------------------------

	// public getters -------------------------------------------------------
	
    /**
     * Returns the String that this CollationKey represents.
     * @return source string that this CollationKey represents
     * @draft 2.2
     */
    public String getSourceString() 
    {
        return m_source_;
    }

    /**
     * <p>Duplicates and returns the value of this CollationKey as a sequence 
     * of big-endian bytes.</p> 
     * <p>If two CollationKeys could be legitimately compared, then one could 
     * compare the byte arrays of each to obtain the same result.</p>  
     * @return CollationKey value in a sequence of big-endian byte bytes.
     * @draft 2.2
     */
    public byte[] toByteArray() 
    {
    	int length = 0;
    	while (true) {
    		if (m_key_[length] == 0) {
    			break;
    		}
    		length ++;
    	}
    	length ++;
    	byte result[] = new byte[length];
    	System.arraycopy(m_key_, 0, result, 0, length);
        return result;
    }

 	// public other methods -------------------------------------------------	
 	
    /**
     * <p>Compare this CollationKey to the target CollationKey. The collation 
     * rules of the Collator object which created these keys are applied.</p>
     * <p><strong>Note:</strong> CollationKeys created by different Collators 
     * can not be compared.</p>
     * @param target target CollationKey
     * @return an integer value, if value is less than zero this CollationKey
     *         is less than than target, if value is zero if they are equal 
     *         and value is greater than zero if this CollationKey is greater 
     *         than target.
     * @see Collator#compare(String, String)
     * @draft 2.2
     */
    public int compareTo(CollationKey target)
    {
    	int i = 0;
    	while (m_key_[i] != 0 && target.m_key_[i] != 0) {
    		int key = m_key_[i] & 0xFF;
    		int targetkey = target.m_key_[i] & 0xFF;
    		if (key < targetkey) {
    			return -1;
    		}
    		if (targetkey < key) {
    			return 1;
    		}
    		i ++;
    	}
    	// last comparison if we encounter a 0
    	int key = m_key_[i] & 0xFF;
    	int targetkey = target.m_key_[i] & 0xFF;
        if (key < targetkey) {
    		return -1;
    	}
    	if (targetkey < key) {
    		return 1;
    	}
        return 0;
    }

    /**
     * <p>Compares this CollationKey with the specified Object.</p>
     * @param obj the Object to be compared.
     * @return Returns a negative integer, zero, or a positive integer 
     *         respectively if this CollationKey is less than, equal to, or 
     *         greater than the given Object.
     * @exception ClassCastException thrown when the specified Object is not a
     *		      CollationKey.
     * @see #compareTo(CollationKey)
     * @draft 2.2
     */
    public int compareTo(Object obj) 
    {
 		return compareTo((CollationKey)obj);
    }

    /**
     * <p>Compare this CollationKey and the target CollationKey for equality.
     * </p>
     * <p>The collation rules of the Collator object which created these keys 
     * are applied.</p>
     * <p><strong>Note:</strong> CollationKeys created by different Collators 
     * can not be compared.</p>
     * @param target the CollationKey to compare to.
     * @return true if two objects are equal, false otherwise.
     * @draft 2.2
     */
    public boolean equals(Object target) 
    {
        if (this == target) {
        	return true;
        }
        if (target == null || !(target instanceof CollationKey)) {
            return false;
        }
        CollationKey other = (CollationKey)target;
        int i = 0;
        while (true) {
        	if (m_key_[i] != other.m_key_[i]) {
        		return false;
        	}
        	if (m_key_[i] == 0) {
        		break;
        	}
        	i ++;
        }
        return true;
    }

    /**
     * <p>Creates a hash code for this CollationKey. The hash value is 
     * calculated on the key itself, not the String from which the key was 
     * created. Thus if x and y are CollationKeys, then 
     * x.hashCode(x) == y.hashCode() if x.equals(y) is true. This allows 
     * language-sensitive comparison in a hash table.</p>
     * <p>See the CollatinKey class description for an example.</p>
     * @return the hash value.
     * @draft 2.2
     */
    public int hashCode() 
    {
    	if (m_hashCode_ == 0) {
    		int size = m_key_.length >> 1;
    		StringBuffer key = new StringBuffer(size);
    		int i = 0;
    		while (m_key_[i] != 0 && m_key_[i + 1] != 0) {
    			key.append((char)((m_key_[i] << 8) | m_key_[i + 1]));
    			i += 2;
    		}
    		if (m_key_[i] != 0) {
    			key.append((char)(m_key_[i] << 8));
    		}
    		m_hashCode_ = key.toString().hashCode();
    	}
        return m_hashCode_;
    }

	// protected constructor ------------------------------------------------
    
    /**
     * Protected CollationKey can only be generated by Collator objects
     * @param source string the CollationKey represents
     * @param key sort key array of bytes
     * @param size of sort key 
     * @draft 2v2
     */
    CollationKey(String source, byte key[])
    {
    	m_source_ = source;
    	m_key_ = key;
    	m_hashCode_ = 0;
    }

	// private data members -------------------------------------------------

	/**
	 * Source string this CollationKey represents
	 */	
    private String m_source_;
    /**
     * Sequence of bytes that represents the sort key
     */
    private byte m_key_[];
    /**
     * Hash code for the key
     */
    private int m_hashCode_;
}