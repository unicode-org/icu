/*
******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/ValueIterator.java,v $
* $Date: 2002/03/13 04:48:59 $
* $Revision: 1.4 $
*
******************************************************************************
*/

package com.ibm.icu.util;

/**
 * <p>Interface for enabling iteration over any set of integers, giving
 * back a integer result with a value. 
 * The result is represented by [integer, value] where</p>
 * <ul>
 * <li> integer is the current integer in the iteration.
 * <li> value is the value that the result integers is associated with.
 * </ul>
 * <p>Most implementations will be created by factory methods, such as the
 * character name iterator in UCharacter.getNameIterator. See example below.
 * </p>
 * Example of use:<br>
 * <pre>
 * ValueIterator iterator = UCharacter.getNameIterator();
 * ValueIterator.Element result = new ValueIterator.Element();
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
    * Return result wrapper for com.ibm.icu.util.RangeValueIterator.
    * Stores the start and limit of the continous result range and the
    * common value all integers between [start, limit - 1] has.
    * @draft 2.1
    */
    public static final class Element
    {
        /**
        * Integer of the current iteration
        * @draft 2.1
        */
        public int integer;
        /**
        * Gets the value of integer
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
    * Resets the iterator to the beginning of the iteration.
    * @draft 2.1
    */
    public void reset();
    
    /**
     * <p>Restricts the range of integers to iterate.</p>
     * <p>If setRange() is not performed before next() is called, the 
     * iteration will start from a minimum integer index and ends at a 
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