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
    
    private static final int  START_STATE = 1;     // The state number of the starting state
    private static final int  STOP_STATE  = 0;     // The state-transition value indicating "stop"

    /**
     * The character iterator through which this BreakIterator accesses the text
     * @internal
     */
    private CharacterIterator   fText;
    
    /**
     * The rule data for this BreakIterator instance
     * @internal
     */
    private RBBIDataWrapper     fData;

    /** Index of the Rule {tag} values for the most recent match. 
     *  @internal
    */
    private int                 fLastRuleStatusIndex;

    /**
     * Rule tag value valid flag.
     * Some iterator operations don't intrinsically set the correct tag value.
     * This flag lets us lazily compute the value if we are ever asked for it.
     * @internal
     */
    private boolean             fLastStatusIndexValid;
    
    /**
     * Debugging flag.  Trace operation of state machine when true.
     * @internal
     */
    public static boolean       fTrace;


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
     * Returns the description (rules) used to create this iterator.
     * (In ICU4C, the same function is RuleBasedBreakIterator::getRules())
     * @stable ICU 2.0
     */
    public String toString() {
        String   retStr = null;
        if (fData != null) {
            retStr =  fData.fRuleSource;
        }
        return retStr;
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
        fLastRuleStatusIndex  = 0;
        fLastStatusIndexValid = true;
        if (fText == null) {
            return BreakIterator.DONE;
        }
        fText.first();
        return fText.getIndex();
	}
    
    
    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
     * @stable ICU 2.0
     */
	public int last() {
        if (fText == null) {
            fLastRuleStatusIndex  = 0;
            fLastStatusIndexValid = true;
            return BreakIterator.DONE;
        }

        // I'm not sure why, but t.last() returns the offset of the last character,
        // rather than the past-the-end offset
        //
        //   (It's so a loop like for(p=it.last(); p!=DONE; p=it.previous()) ...
        //     will work correctly.)


        fLastStatusIndexValid = false;
        int pos = fText.getEndIndex();
        fText.setIndex(pos);
        return pos;
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
        int result = current();
        while (n > 0) {
            result = handleNext();
            --n;
        }
        while (n < 0) {
            result = previous();
            ++n;
        }
        return result;
	}
    
    
    /**
     * Advances the iterator to the next boundary position.
     * @return The position of the first boundary after this one.
     * @stable ICU 2.0
     */
	public int next() {
		return  handleNext();
	}
    
    
    /**
     * Moves the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     * @stable ICU 2.0
     */
	public int previous() {
        // if we're already sitting at the beginning of the text, return DONE
        if (fText == null || current() == fText.getBeginIndex()) {
            fLastRuleStatusIndex  = 0;
            fLastStatusIndexValid = true;
            return BreakIterator.DONE;
        }

        if (fData.fSRTable != null || fData.fSFTable != null) {
            return handlePrevious(fData.fRTable);
        }

        // old rule syntax
        // set things up.  handlePrevious() will back us up to some valid
        // break position before the current position (we back our internal
        // iterator up one step to prevent handlePrevious() from returning
        // the current position), but not necessarily the last one before
        // where we started

        int       start = current();

        CIPrevious32(fText);
        int       lastResult    = handlePrevious();
        int       result        = lastResult;
        int       lastTag       = 0;
        boolean   breakTagValid = false;

        // iterate forward from the known break position until we pass our
        // starting point.  The last break position before the starting
        // point is our return value

        for (;;) {
            result         = handleNext();
            if (result == BreakIterator.DONE || result >= start) {
                break;
            }
            lastResult     = result;
            lastTag        = fLastRuleStatusIndex;
            breakTagValid  = true;
        }

        // fLastBreakTag wants to have the value for section of text preceding
        // the result position that we are to return (in lastResult.)  If
        // the backwards rules overshot and the above loop had to do two or more
        // handleNext()s to move up to the desired return position, we will have a valid
        // tag value. But, if handlePrevious() took us to exactly the correct result positon,
        // we wont have a tag value for that position, which is only set by handleNext().

        // set the current iteration position to be the last break position
        // before where we started, and then return that value
        fText.setIndex(lastResult);
        fLastRuleStatusIndex  = lastTag;       // for use by getRuleStatus()
        fLastStatusIndexValid = breakTagValid;
        return lastResult;
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
		return fText;
	}


    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText An iterator over the text to analyze.
     * @stable ICU 2.0
     */
	public void setText(CharacterIterator newText) {
        fText = newText;
        this.first();
	}
    
    
    private static int CINext32(CharacterIterator ci) {
        int retVal = 0;
        char cLead = ci.next();
        retVal = (int)cLead;
        if (UTF16.isLeadSurrogate(cLead)) {
            char cTrail = ci.next();
            if (UTF16.isTrailSurrogate(cTrail)) {
                retVal = ((int)cLead  - UTF16.LEAD_SURROGATE_MIN_VALUE) << 10 +
                         ((int)cTrail - UTF16.TRAIL_SURROGATE_MIN_VALUE);
            } else {
                ci.previous();
            }           
        }
        return retVal;
    }
    
    private static int CIPrevious32(CharacterIterator ci) {
        int retVal = 0;
        char cTrail = ci.previous();
        retVal = (int)cTrail;
        if (UTF16.isTrailSurrogate(cTrail)) {
            char cLead = ci.previous();
            if (UTF16.isLeadSurrogate(cLead)) {
                retVal = ((int)cLead  - UTF16.LEAD_SURROGATE_MIN_VALUE) << 10 +
                         ((int)cTrail - UTF16.TRAIL_SURROGATE_MIN_VALUE);
            } else {
                ci.next();
            }           
        }
        return retVal;
    }
    /**
     * Internal implementation of next() for RBBI.
     * @internal
     */
    private int handleNext() {
        // TODO:
        return 0;
    }

    
    private int  handlePrevious() {
        // TODO:
        return 0;
    }
    
    
    private int handlePrevious(short statetable[]) {
        // TODO:
        return 0;
    }

}





