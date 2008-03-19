/*
******************************************************************************
* Copyright (C) 1996-2007, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
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
 * RangeValueIterator iterator = UCharacter.getTypeIterator();
 * RangeValueIterator.Element element = new RangeValueIterator.Element();
 * while (iterator.next(element)) {
 *     System.out.println("Codepoint \\u" + 
 *                        Integer.toHexString(element.start) + 
 *                        " to codepoint \\u" +
 *                        Integer.toHexString(element.limit - 1) + 
 *                        " has the character type " + 
 *                        element.value);
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
        return value & UCharacterProperty.TYPE_MASK;
    }
}