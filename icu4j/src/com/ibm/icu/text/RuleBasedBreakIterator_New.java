/*
 *******************************************************************************
 * Copyright (C) 2004 International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * @author andy
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RuleBasedBreakIterator_New extends RuleBasedBreakIterator {
    //=======================================================================
    // boilerplate
    //=======================================================================
    /**
     * Clones this iterator.
     * @return A newly-constructed RuleBasedBreakIterator with the same
     * behavior as this one.
     * @stable ICU 2.0
     */
    public Object clone()
    {
    	RuleBasedBreakIterator_New result = (RuleBasedBreakIterator_New) super.clone();
        // TODO: real clone code
        return result;
    }

    /**
     * Returns true if both BreakIterators are of the same class, have the same
     * rules, and iterate over the same text.
     * @stable ICU 2.0
     */
    public boolean equals(Object that) {
    	return false;  // TODO:
    }

    /**
     * Returns the description used to create this iterator
     * @stable ICU 2.0
     */
    public String toString() {
        return "";      // TODO:
    }

    /**
     * Compute a hashcode for this BreakIterator
     * @return A hash code
     * @stable ICU 2.0
     */
    public int hashCode()
    {
        return 0;        // TODO
    }

    //=======================================================================
    // BreakIterator overrides
    //=======================================================================

    /**
     * Sets the current iteration position to the beginning of the text.
     * (i.e., the CharacterIterator's starting offset).
     * @return The offset of the beginning of the text.
     * @stable ICU 2.0
     */
	public int first() {
		return 0;             // TODO;
	}
    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
     * @stable ICU 2.0
     */
	public int last() {
		return 0;             // TODO:
	}
    /**
     * Advances the iterator either forward or backward the specified number of steps.
     * Negative values move backward, and positive values move forward.  This is
     * equivalent to repeatedly calling next() or previous().
     * @param n The number of steps to move.  The sign indicates the direction
     * (negative is backwards, and positive is forwards).
     * @return The character offset of the boundary position n boundaries away from
     * the current one.
     * @stable ICU 2.0
     */
	public int next(int n) {
		return  0;             // TODO:
	}
    /**
     * Advances the iterator to the next boundary position.
     * @return The position of the first boundary after this one.
     * @stable ICU 2.0
     */
	public int next() {
		return  0;             // TODO:
	}
    /**
     * Advances the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     * @stable ICU 2.0
     */
	public int previous() {
		return  0;             // TODO:
	}
    /**
     * Sets the iterator to refer to the first boundary position following
     * the specified position.
     * @param offset The position from which to begin searching for a break position.
     * @return The position of the first break after the current position.
     * @stable ICU 2.0
     */
	public int following(int offset) {
		return  0;             // TODO:
	}
    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @param offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     * @stable ICU 2.0
     */
    public int preceding(int offset) {
    	return  0;             // TODO:
    }

/**
 * Returns true if the specfied position is a boundary position.  As a side
 * effect, leaves the iterator pointing to the first boundary position at
 * or after "offset".
 * @param offset the offset to check.
 * @return True if "offset" is a boundary position.
 * @stable ICU 2.0
 */
public boolean isBoundary(int offset) {
	return true;    // TODO:
}

/**
 * Returns the current iteration position.
 * @return The current iteration position.
 * @stable ICU 2.0
 */
public int current() {
		return 0;             // TODO:
	}



/**
 * Return the status tag from the break rule that determined the most recently
 * returned break position.  The values appear in the rule source
 * within brackets, {123}, for example.  For rules that do not specify a
 * status, a default value of 0 is returned.  If more than one rule applies,
 * the numerically largest of the possible status values is returned.
 * <p>
 * Of the standard types of ICU break iterators, only the word break
 * iterator provides status values.  The values are defined in
 * <code>enum UWordBreak</code>, and allow distinguishing between words
 * that contain alphabetic letters, "words" that appear to be numbers,
 * punctuation and spaces, words containing ideographic characters, and
 * more.  Call <code>getRuleStatus</code> after obtaining a boundary
 * position from <code>next()<code>, <code>previous()</code>, or 
 * any other break iterator functions that returns a boundary position.
 * <p>
 * @return the status from the break rule that determined the most recently
 * returned break position.
 *
 * @draft ICU 3.0
 */
public int  getRuleStatus() {
	return  0;             // TODO:
}



/**
 * Get the status (tag) values from the break rule(s) that determined the most 
 * recently returned break position.  The values appear in the rule source
 * within brackets, {123}, for example.  The default status value for rules
 * that do not explicitly provide one is zero.
 * <p>
 * For word break iterators, the possible values are defined in enum UWordBreak.
 * <p>
 * If the size  of the output array is insufficient to hold the data,
 *  the output will be truncated to the available length.  No exception
 *  will be thrown.
 *
 * @param fillInArray an array to be filled in with the status values.  
 * @return          The number of rule status values from rules that determined 
 *                  the most recent boundary returned by the break iterator.
 *                  In the event that the array is too small, the return value
 *                  is the total number of status values that were available,
 *                  not the reduced number that were actually returned.
 * @draft ICU 3.0
 */
public int getRuleStatusVec(int[] fillInArray) {
    if (fillInArray != null && fillInArray.length >= 1) {    // TODO:
        fillInArray[0] = 0;
    }
    return 1;
 }


/**
 * Return a CharacterIterator over the text being analyzed.  This version
 * of this method returns the actual CharacterIterator we're using internally.
 * Changing the state of this iterator can have undefined consequences.  If
 * you need to change it, clone it first.
 * @return An iterator over the text being analyzed.
 * @stable ICU 2.0
 */
	public CharacterIterator getText() {
		return new StringCharacterIterator("");
	}


    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText An iterator over the text to analyze.
     * @stable ICU 2.0
     */
	public void setText(CharacterIterator newText) {
	}

}
