/*
******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/ValueIterator.java,v $
* $Date: 2002/03/08 02:02:39 $
* $Revision: 1.1 $
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
 */
public interface ValueIterator
{
    // public inner class ---------------------------------------------
    
    /**
    * Return result wrapper for com.ibm.icu.util.RangeValueIterator.
    * Stores the start and limit of the continous result range and the
    * common value all integers between [start, limit - 1] has.
    */
    public class Element
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
     * Sets the range of integers to iterate.
     * If range is not set, a default range defined by implementation 
     * will be used.
     * @param start first integer in range to iterate
     * @param limit 1 integer after the last integer in range 
     */
    public void setRange(int start, int end);
}