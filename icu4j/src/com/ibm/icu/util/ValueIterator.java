/*
******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/ValueIterator.java,v $
* $Date: 2002/04/05 00:12:09 $
* $Revision: 1.6 $
*
******************************************************************************
*/

package com.ibm.icu.util;

/**
 * <p>Interface for enabling iteration over sets of [int, Object], where
 * int is the sorted integer index in ascending order and Object, its 
 * associated value.</p>
 * <p>The ValueIterator allows iterations over integer indexes in the range 
 * of Integer.MIN_VALUE to Integer.MAX_VALUE inclusive. Implementations of 
 * ValueIterator should specify their own maximum subrange within the above 
 * range that is meaningful to its applications.</p>
 * <p>Most implementations will be created by factory methods, such as the
 * character name iterator in UCharacter.getNameIterator. See example below.
 * </p>
 * Example of use:<br>
 * <pre>
 * ValueIterator iterator = UCharacter.getNameIterator();
 * ValueIterator.Element result = new ValueIterator.Element();
 * iterator.setRange(UCharacter.MIN_VALUE, UCharacter.MAX_VALUE);
 * while (iterator.next(result)) {
 *     System.out.println("Codepoint \\u" + 
 *                        Integer.toHexString(result.integer) + 
 *                        " has the character name " + (String)result.value);
 * }
 * </pre>
 * @author synwee
 * @since release 2.1, March 5th 2002
 * @draft 2.1
 */
public interface ValueIterator
{
    // public inner class ---------------------------------------------
    
    /**
    * <p>The return result container of each iteration. Stores the next 
    * integer index and its associated value Object.</p> 
    * @draft 2.1
    */
    public static final class Element
    {
        /**
        * Integer index of the current iteration
        * @draft 2.1
        */
        public int integer;
        /**
        * Gets the Object value associated with the integer index.
        * @draft 2.1
        */ 
        public Object value;
    }
    
    // public methods -------------------------------------------------
    
    /**
    * <p>Gets the next result for this iteration and returns 
    * true if we are not at the end of the iteration, false otherwise.</p>
    * <p>If the return boolean is a false, the contents of elements will not
    * be updated.</p>
    * @param element for storing the result range and value
    * @return true if we are not at the end of the iteration, false otherwise.
    * @see Element
    * @draft 2.1
    */
    public boolean next(Element element);
    
    /**
    * <p>Resets the iterator to start iterating from the integer index 
    * Integer.MIN_VALUE or X if a setRange(X, Y) has been called previously.
    * </p>
    * @draft 2.1
    */
    public void reset();
    
    /**
     * <p>Restricts the range of integers to iterate.</p>
     * <p>If setRange() is not performed before next() is called, the 
     * iteration will start from the integer.Integer.MIN_VALUE and ends at a 
     * maximum integer index that is determined by the specific implementation
     * of ValueIterator.</p>
     * <p>For instance the Unicode character name iterator provided by
     * com.ibm.icu.lang.UCharacter.getNameIterator() will iterate the names 
     * from UCharacter.MIN_VALUE to UCharacter.MAX_VALUE if setRange() was 
     * never called.</p>
     * @param start first integer in range to iterate
     * @param limit 1 integer after the last integer in range 
     * @draft 2.1
     */
    public void setRange(int start, int end);
}