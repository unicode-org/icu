/*
******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/lang/UCharacterTypeIterator.java,v $
* $Date: 2002/03/15 22:48:07 $
* $Revision: 1.5 $
*
******************************************************************************
*/

package com.ibm.icu.lang;

import com.ibm.icu.impl.TrieIterator;
import com.ibm.icu.impl.UCharacterProperty;

/**
 * Class enabling iteration of the codepoints according to their types.
 * Result of each iteration contains the interval of codepoints that have
 * the same type.
 * Example of use:<br>
 * <pre>
 * UCharacterTypeIterator iterator = UCharacter.getUCharacterTypeIterator();
 * while (iterator.next()) {
 *     System.out.println("Codepoint \\u" + 
 *                        Integer.toHexString(iterator.getStart()) + 
 *                        " to codepoint \\u" +
 *                        Integer.toHexString(iterator.getLimit() - 1) + 
 *                        " has the character type " + iterator.getValue());
 * }
 * </pre>
 * @author synwee
 * @see com.ibm.icu.util.TrieIterator
 * @since release 2.1, Jan 24 2002
 */
class UCharacterTypeIterator extends TrieIterator
{
    // protected constructor ---------------------------------------------
    
    /**
    * TrieEnumeration constructor
    * @param property the unicode character properties to be used
    * @draft 2.1
    */
    protected UCharacterTypeIterator(UCharacterProperty property)
    {
       super(property.m_trie_);
    }
    
    // protected methods ----------------------------------------------
    
    /**
    * Called by nextElement() to extracts a 32 bit value from a trie value
    * used for comparison.
    * This method is to be overwritten if special manipulation is to be done
    * to retrieve a relevant comparison.
    * The default function is to return the value as it is.
    * @param value a value from the trie
    * @return extracted value
    */
    protected int extract(int value)
    {
    	// this is needed because TrieIterator() gets called first and it
    	// in turn calls extract to instantiate this default value
    	// so sometimes m_property_ does not get assigned properly
    	if (m_property_ == null) {
    		m_property_ = UCharacterProperty.getInstance().m_property_;
    	}
    	return UCharacterProperty.getPropType(m_property_[value]);
    }
    
    // private data members ---------------------------------------------
    
    /**
     * Character property
     */
    private int m_property_[];
}