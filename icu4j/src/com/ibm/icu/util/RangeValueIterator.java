/*
******************************************************************************
* Copyright (C) 1996-2002, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/RangeValueIterator.java,v $
* $Date: 2002/02/08 01:12:45 $
* $Revision: 1.1 $
*
******************************************************************************
*/

package com.ibm.icu.util;

import com.ibm.text.UCharacter;
import com.ibm.text.UTF16;

/**
 * <p>Interface for enabling iteration over any set of integers, giving
 * back a maximum continuous range of integer result with a common value. 
 * The result is represented by [start, limit, value] where</p>
 * <ul>
 * <li> start is the starting integer of the result range
 * <li> limit is 1 after the maximum integer that follows start, such that
 *      all integers between start and (limit - 1), inclusive, have the same 
 *      associated integer value.
 * <li> value is the integer value that all integers from start to (limit - 1) 
 *      share in common.
 * </ul>
 * <p>
 * Hence value(start) = value(start + 1) = .... = value(start + n) = .... =
 * value(limit - 1). However value(start -1) != value(start) and 
 * value(limit) != value(start).
 * </p>
 * <p>Most implementations will be created by factory methods, such as the
 * character type iterator in UCharacter.getTypeIterator. See example below.
 * </p>
 * Example of use:<br>
 * <pre>
 * RangeValueIterator iterator = UCharacter.getTypeIterator();
 * while (iterator.next()) {
 *     System.out.println("Codepoint \\u" + 
 *                        Integer.toHexString(iterator.getStart()) + 
 *                        " to codepoint \\u" +
 *                        Integer.toHexString(iterator.getLimit() - 1) + 
 *                        " has the character type " + 
 *                        iterator.getValue());
 * }
 * </pre>
 * @author synwee
 * @since release 2.1, Jan 17 2002
 */
public interface RangeValueIterator

{
    // public methods -------------------------------------------------
    
    /**
    * <p>Returns true if we are not at the end of the iteration, false 
    * otherwise.</p>
    * <p>The next set of integers with the same value will be 
    * calculated during this call. To retrieve the set of integers and 
    * their common value, the methods getStart(), getLimit() and getValue()
    * can be called.</p>
    * @return true if we are not at the end of the iteration, false otherwise.
    * @see #getStart()
    * @see #getLimit()
    * @see #getValue()
    * @draft 2.1
    */
    public boolean next();
    
    /**
    * Gets the starting integer of the result range with the same value, after
    * the last call to next(). This method will return Integer.MIN_VALUE
    * if next() has never been called or if reset() was the last call before 
    * the getter methods.
    * @return start codepoint of the result range
    * @draft 2.1
    */
    public int getStart();
    
    /**
    * Gets the (end + 1) integer of result range with the same value, after
    * the last call to next(). This method will return Integer.MIN_VALUE
    * if next() has never been called or if reset() was the last call before 
    * the getter methods.
    * @return (end + 1) codepoint of the result range
    * @draft 2.1
    */
    public int getLimit();
    
    /**
    * Gets the common value of the result range, after
    * the last call to next(). This method will return Integer.MIN_VALUE
    * if next() has never been called or if reset() was the last call before 
    * the getter methods.
    * @return common value of the codepoints in the result range
    * @draft 2.1
    */
    public int getValue();
    
    /**
    * Resets the iterator to the beginning of the iteration and initializes
    * start, limit and value to Integer.MIN_VALUE.
    * @draft 2.1
    */
    public void reset();
}