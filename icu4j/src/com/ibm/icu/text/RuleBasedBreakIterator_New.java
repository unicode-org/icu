/*
 *******************************************************************************
 * Copyright (C) 2004 International Business Machines Corporation and          *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.CharacterIterator;
import java.io.IOException;
import java.io.InputStream;


/**
 * Rule Based Break Iterator implementation.
 * @internal
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
    private RBBIDataWrapper     fRData;

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
    
    /**
     * Dump the contents of the state table and character classes for this break iterator.
     * For debugging only.
     * @internal
     */
    public void dump() {
        this.fRData.dump();   
    }


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
        if (fText != null) {
            fText = (CharacterIterator)fText.clone();   
        }
        return result;
    }

    /**
     * Returns true if both BreakIterators are of the same class, have the same
     * rules, and iterate over the same text.
     * @stable ICU 2.0
     */
    public boolean equals(Object that) {
        try {
            RuleBasedBreakIterator_New other = (RuleBasedBreakIterator_New) that;
            if (fRData != other.fRData && (fRData == null || other.fRData == null)) {
                return false;   
            }
            if (fRData != null && other.fRData != null && 
                    (!fRData.fRuleSource.equals(other.fRData.fRuleSource))) {
                return false;
            }
            if (fText == null && other.fText == null) {
                return true;   
            }
            if (fText == null || other.fText == null) {
                return false;   
            }
            return fText.equals(other.fText);
        }
        catch(ClassCastException e) {
            return false;
        }
     }

    /**
     * Returns the description (rules) used to create this iterator.
     * (In ICU4C, the same function is RuleBasedBreakIterator::getRules())
     * @stable ICU 2.0
     */
    public String toString() {
        String   retStr = null;
        if (fRData != null) {
            retStr =  fRData.fRuleSource;
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
        return fRData.fRuleSource.hashCode(); 
    }

    
    //=======================================================================
    // Constructors & Factories
    //=======================================================================
    
    /**
     * Create a break iterator from a precompiled set of rules.
     * @internal
     */
    public static RuleBasedBreakIterator getInstanceFromCompiledRules(InputStream is) throws IOException {
        RuleBasedBreakIterator_New  This = new RuleBasedBreakIterator_New();
        This.fRData = RBBIDataWrapper.get(is);
        This.fText = new java.text.StringCharacterIterator("");  // Note: some old tests fail if fText is null
                                                                 //       on a newly created instance.
        return This;   
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

        if (fRData.fSRTable != null || fRData.fSFTable != null) {
            return handlePrevious(fRData.fRTable);
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
        // if the offset passed in is already past the end of the text,
        // just return DONE; if it's before the beginning, return the
        // text's starting offset
        fLastRuleStatusIndex  = 0;
        fLastStatusIndexValid = true;
        if (fText == null || offset >= fText.getEndIndex()) {
            last();
            return next();
        }
        else if (offset < fText.getBeginIndex()) {
            return first();
        }

        // otherwise, set our internal iteration position (temporarily)
        // to the position passed in.  If this is the _beginning_ position,
        // then we can just use next() to get our return value

        int result = 0;

        if (fRData.fSRTable != null) {
            // Safe Point Reverse rules exist.
            //   This allows us to use the optimum algorithm.
            fText.setIndex(offset);
            // move forward one codepoint to prepare for moving back to a
            // safe point.
            // this handles offset being between a supplementary character
            CINext32(fText);
            // handlePrevious will move most of the time to < 1 boundary away
            handlePrevious(fRData.fSRTable);
            result = next();
            while (result <= offset) {
                result = next();
            }
            return result;
        }
        if (fRData.fSFTable != null) {
            // No Safe point reverse table, but there is a safe pt forward table.
            // 
            fText.setIndex(offset);
            CIPrevious32(fText);
            // handle next will give result >= offset
            handleNext(fRData.fSFTable);
            // previous will give result 0 or 1 boundary away from offset,
            // most of the time
            // we have to
            int oldresult = previous();
            while (oldresult > offset) {
                result = previous();
                if (result <= offset) {
                    return oldresult;
                }
                oldresult = result;
            }
            result = next();
            if (result <= offset) {
                return next();
            }
            return result;
        }
        // otherwise, we have to sync up first.  Use handlePrevious() to back
        // us up to a known break position before the specified position (if
        // we can determine that the specified position is a break position,
        // we don't back up at all).  This may or may not be the last break
        // position at or before our starting position.  Advance forward
        // from here until we've passed the starting position.  The position
        // we stop on will be the first break position after the specified one.
        // old rule syntax

        fText.setIndex(offset);
        if (offset == fText.getBeginIndex()) {
            return handleNext();
        }
        result = previous();

        while (result != BreakIterator.DONE && result <= offset) {
            result = next();
        }

        return result;
	}
    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @param offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     * @stable ICU 2.0
     */
    public int preceding(int offset) {
        // if the offset passed in is already past the end of the text,
        // just return DONE; if it's before the beginning, return the

        // text's starting offset
        if (fText == null || offset > fText.getEndIndex()) {
            // return BreakIterator::DONE;
            return last();
        }
        else if (offset < fText.getBeginIndex()) {
            return first();
        }

        // if we start by updating the current iteration position to the
        // position specified by the caller, we can just use previous()
        // to carry out this operation

        int  result;
        if (fRData.fSFTable != null) {
            /// todo synwee
            // new rule syntax
            fText.setIndex(offset);
            // move backwards one codepoint to prepare for moving forwards to a
            // safe point.
            // this handles offset being between a supplementary character
            CIPrevious32(fText);
            handleNext(fRData.fSFTable);
            result = previous();
            while (result >= offset) {
                result = previous();
            }
            return result;
        }
        if (fRData.fSRTable != null) {
            // backup plan if forward safe table is not available
            fText.setIndex(offset);
            CINext32(fText);
            // handle previous will give result <= offset
            handlePrevious(fRData.fSRTable);

            // next will give result 0 or 1 boundary away from offset,
            // most of the time
            // we have to
            int oldresult = next();
            while (oldresult < offset) {
                result = next();
                if (result >= offset) {
                    return oldresult;
                }
                oldresult = result;
            }
            result = previous();
            if (result >= offset) {
                return previous();
            }
            return result;
        }

        // old rule syntax
        fText.setIndex(offset);
        return previous();
    }

    /**
     * Throw IllegalArgumentException unless begin <= offset < end.
     * TODO:  subclassing interface from old RBBI is not really usable.
     *        What to do with old protected functions tagged as stable?
     * @stable ICU 2.0
     */
    protected static final void checkOffset(int offset, CharacterIterator text) {
        if (offset < text.getBeginIndex() || offset > text.getEndIndex()) {
            throw new IllegalArgumentException("offset out of bounds");
        }
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
    checkOffset(offset, fText);
    
    // the beginning index of the iterator is always a boundary position by definition
    if (offset == fText.getBeginIndex()) {
        first();       // For side effects on current position, tag values.
        return true;
    }

    if (offset == fText.getEndIndex()) {
        last();       // For side effects on current position, tag values.
        return true;
    }

    // out-of-range indexes are never boundary positions
    if (offset < fText.getBeginIndex()) {
        first();       // For side effects on current position, tag values.
        return false;
    }

    if (offset > fText.getEndIndex()) {
        last();        // For side effects on current position, tag values.
        return false;
    }

    // otherwise, we can use following() on the position before the specified
    // one and return true if the position we get back is the one the user
    // specified
    return following(offset - 1) == offset;
}

/**
 * Returns the current iteration position.
 * @return The current iteration position.
 * @stable ICU 2.0
 */
public int current() {
    return (fText != null) ? fText.getIndex() : BreakIterator.DONE;
	}



private void makeRuleStatusValid() {
    if (fLastStatusIndexValid == false) {
        //  No cached status is available.
        if (fText == null || current() == fText.getBeginIndex()) {
            //  At start of text, or there is no text.  Status is always zero.
            fLastRuleStatusIndex = 0;
            fLastStatusIndexValid = true;
        } else {
            //  Not at start of text.  Find status the tedious way.
            int pa = current();
            previous();
            int pb = next();
            if (pa != pb) {
                // TODO:  comment this out.
                System.out.println("RuleBasedBreakIterator::makeRuleStatusValid internal error");
            }
        }
    }
    //U_ASSERT(fLastStatusIndexValid == TRUE);
    //U_ASSERT(fLastRuleStatusIndex >= 0  &&  fLastRuleStatusIndex < fData->fStatusMaxIdx);

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
    makeRuleStatusValid();
    //   Status records have this form:
    //           Count N         <--  fLastRuleStatusIndex points here.
    //           Status val 0
    //           Status val 1
    //              ...
    //           Status val N-1  <--  the value we need to return
    //   The status values are sorted in ascending order.
    //   This function returns the last (largest) of the array of status values.
    int  idx = fLastRuleStatusIndex + fRData.fStatusTable[fLastRuleStatusIndex];
    int  tagVal = fRData.fStatusTable[idx];

    return tagVal;
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
    makeRuleStatusValid();
    int numStatusVals = fRData.fStatusTable[fLastRuleStatusIndex];
    if (fillInArray != null) {  
        int numToCopy = Math.min(numStatusVals, fillInArray.length);
        for (int i=0; i<numToCopy; i++) {
            fillInArray[i] = fRData.fStatusTable[fLastRuleStatusIndex + i + 1];
        }
    }
    return numStatusVals;
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
        //  TODO:  pre-increment is a pain.  Redo all to use post-increment.
        int retVal;
        int curChar = CICurrent32(ci);
        ci.next();
        if (curChar >= UTF16.SUPPLEMENTARY_MIN_VALUE) {
            ci.next();   
        }
        retVal = CICurrent32(ci);
        return retVal;
   }
    
    
    private static int CIPrevious32(CharacterIterator ci) {
        int retVal = 0;
        char cTrail = ci.previous();
        retVal = (int)cTrail;
        if (UTF16.isTrailSurrogate(cTrail)) {
            char cLead = ci.previous();
            if (UTF16.isLeadSurrogate(cLead)) {
                retVal = (((int)cLead  - UTF16.LEAD_SURROGATE_MIN_VALUE) << 10) +
                          ((int)cTrail - UTF16.TRAIL_SURROGATE_MIN_VALUE) +
                          UTF16.SUPPLEMENTARY_MIN_VALUE;
            } else {
                ci.next();
            }           
        }
        return retVal;
    }
    
    private static int CICurrent32(CharacterIterator ci) {
        char  lead   = ci.current();
        int   retVal = lead;
        if (UTF16.isLeadSurrogate(lead)) {
            int  trail = (int)ci.next();
            ci.previous();
            if (UTF16.isTrailSurrogate((char)trail)) {
                retVal = ((lead  - UTF16.LEAD_SURROGATE_MIN_VALUE) << 10) +
                         (trail - UTF16.TRAIL_SURROGATE_MIN_VALUE) +
						 UTF16.SUPPLEMENTARY_MIN_VALUE;
            }
         }
        return retVal;
    }
    
    private static boolean CIHasNext(CharacterIterator ci) {
        if (ci == null) {
            return false;
        }
        if (ci.getIndex() >= ci.getEndIndex()) {
            return false;
        }
        return true;
    }
    
    private static boolean CIHasPrevious(CharacterIterator ci) {
        if (ci == null) {
            return false;
        }
        return (ci.getIndex() != ci.getBeginIndex());
    }
    
    /**
     * Internal implementation of next() for RBBI.
     * @internal
     */
    private int handleNext() {
        return handleNext(fRData.fFTable);
    }

    
    private int handleNext(short stateTable[]) {
        if (fTrace) {
            System.out.println("Handle Next   pos      char  state category");
        }

        // No matter what, handleNext alway correctly sets the break tag value.
        fLastStatusIndexValid = true;

        // if we're already at the end of the text, return DONE.
        if (CIHasNext(fText) == false) {
            fLastRuleStatusIndex = 0;
            return BreakIterator.DONE;
        }

        int initialPosition = fText.getIndex();
        int result          = initialPosition;
        int lookaheadResult = 0;

        // Initialize the state machine.  Begin in state 1
        int               state           = START_STATE;
        short             category;
        int               c               = CICurrent32(fText);
        int               row             = fRData.getRowIndex(state); 
        int               lookaheadStatus = 0;
        int               lookaheadTagIdx = 0;

        fLastRuleStatusIndex = 0;

        // Character Category fetch for starting character.
        //    See comments on character category code within loop, below.
        category = (short)fRData.fTrie.getCodePointValue(c);
        if ((category & 0x4000) != 0)  {
              // fDictionaryCharCount++;
              category &= ~0x4000;
            }

        // loop until we reach the end of the text or transition to state 0
        while (state != STOP_STATE) {
            if (c == CharacterIterator.DONE && CIHasNext(fText)== false) {
                // Reached end of input string.
                //    Note: CharacterIterator::DONE is 0xffff, which is also a legal
                //          character value.  Check for DONE first, because it's quicker,
                //          but also need to check fText->hasNext() to be certain.

                if (lookaheadResult > result) {
                    // We ran off the end of the string with a pending look-ahead match.
                    // Treat this as if the look-ahead condition had been met, and return
                    //  the match at the / position from the look-ahead rule.
                    result               = lookaheadResult;
                    fLastRuleStatusIndex = lookaheadTagIdx;
                    lookaheadStatus      = 0;
                } else if (result == initialPosition) {
                    // Ran off end, no match found.
                    // move forward one
                    fText.setIndex(initialPosition);
                    CINext32(fText);
                    // fText.getIndex();   // Why was this here?
                }
                break;
            }
            // look up the current character's character category, which tells us
            // which column in the state table to look at.
            //
            category = (short)fRData.fTrie.getCodePointValue(c);

            //  Clear the dictionary flag bit in the character's category.
            //  Note:  not using the old style dictionary stuff in this Java engine.
            //         But the bit can be set by the C++ rule compiler, and
            //         we need to clear it out here to be safe.
            category &= ~0x4000;
 
            if (fTrace) {
                System.out.print("            " +  RBBIDataWrapper.intToString(fText.getIndex(), 5)); 
                System.out.print(RBBIDataWrapper.intToHexString(c, 10));
                System.out.println(RBBIDataWrapper.intToString(state,7) + RBBIDataWrapper.intToString(category,6));
            }

            // look up a state transition in the state table
            //     state = row->fNextState[category];
            state = stateTable[row + RBBIDataWrapper.NEXTSTATES + category];
            row   = fRData.getRowIndex(state);

            // Get the next character.  Doing it here positions the iterator
            //    to the correct position for recording matches in the code that
            //    follows.
            c = CINext32(fText);

            if (stateTable[row + RBBIDataWrapper.ACCEPTING] == -1) {
                // Match found, common case, could have lookahead so we move on to check it
                result = fText.getIndex();
                //  Remember the break status (tag) values.
                fLastRuleStatusIndex = stateTable[row + RBBIDataWrapper.TAGIDX];
            }

            if (stateTable[row + RBBIDataWrapper.LOOKAHEAD] != 0) {
                if (lookaheadStatus != 0
                    && stateTable[row + RBBIDataWrapper.ACCEPTING] == lookaheadStatus) {
                    // Lookahead match is completed.  Set the result accordingly, but only
                    // if no other rule has matched further in the mean time.
                    result               = lookaheadResult;
                    fLastRuleStatusIndex = lookaheadTagIdx;
                    lookaheadStatus      = 0;
                    /// i think we have to back up to read the lookahead character again
                    /// fText->setIndex(lookaheadResult);
                    /// TODO: this is a simple hack since reverse rules only have simple
                    /// lookahead rules that we can definitely break out from.
                    /// we need to make the lookahead rules not chain eventually.
                    /// return result;
                    /// this is going to be the longest match again
                    continue;
                }

                int  r = fText.getIndex();
                lookaheadResult = r;
                lookaheadStatus = stateTable[row + RBBIDataWrapper.LOOKAHEAD];
                lookaheadTagIdx = stateTable[row + RBBIDataWrapper.TAGIDX];
                continue;
            }


            if (stateTable[row + RBBIDataWrapper.ACCEPTING] != 0) {
                lookaheadStatus = 0;           // clear out any pending look-ahead matches.
            }
        }        // End of state machine main loop

        // The state machine is done.  Check whether it found a match...

        // If the iterator failed to advance in the match engine, force it ahead by one.
        //   (This really indicates a defect in the break rules.  They should always match
        //    at least one character.)
        if (result == initialPosition) {
            result = fText.setIndex(initialPosition);
            CINext32(fText);
            result = fText.getIndex();
        }

        // Leave the iterator at our result position.
        fText.setIndex(result);
        if (fTrace) {
            System.out.println("result = " + result);
        }
        return result;
    }

    /*
     * handlePrevious
     */
    private int  handlePrevious() {
        if (fText == null || fRData == null) {
            return 0;
        }
        if (fRData.fRTable == null) {
            fText.first();
            return fText.getIndex();
        }

        short          stateTable[]    = fRData.fRTable;
        int            state           = START_STATE;
        int            category;
        int            lastCategory    = 0;
        int            result          = fText.getIndex();
        int            lookaheadStatus = 0;
        int            lookaheadResult = 0;
        int            lookaheadTagIdx = 0;
        int            c               = CICurrent32(fText);
        int            row;

        row = fRData.getRowIndex(state);
        category = (short)fRData.fTrie.getCodePointValue(c);
        category &= ~0x4000;    // Clear the dictionary bit, just in case.

        if (fTrace) {
            System.out.println("Handle Prev   pos   char  state category ");
        }

        // loop until we reach the beginning of the text or transition to state 0
        for (;;) {
            if (c == CharacterIterator.DONE && CIHasPrevious(fText)==false) {
                break;
            }

            // save the last character's category and look up the current
            // character's category
            lastCategory = category;
            category = (short)fRData.fTrie.getCodePointValue(c);

            // Check the dictionary bit in the character's category.
             //    Don't exist in this Java engine implementation.  Clear the bit.
            //
            category &= ~0x4000;

            if (fTrace) {
                System.out.print("             " + fText.getIndex()+ "   ");
                if (0x20<=c && c<0x7f) {
                    System.out.print("  " +  c + "  ");
                } else {
                    System.out.print(" " + Integer.toHexString(c) + " ");
                }
                System.out.println(" " + state + "  " + category + " ");
            }

            // look up a state transition in the backwards state table
            state = stateTable[row + RBBIDataWrapper.NEXTSTATES + category];
            row = fRData.getRowIndex(state);

            continueOn: {
                if (stateTable[row + RBBIDataWrapper.ACCEPTING] == 0 &&
                        stateTable[row + RBBIDataWrapper.LOOKAHEAD] == 0) {
                    break continueOn;
                }
                
                if (stateTable[row + RBBIDataWrapper.ACCEPTING] == -1) {
                    // Match found, common case, no lookahead involved.
                    result = fText.getIndex();
                    lookaheadStatus = 0;     // clear out any pending look-ahead matches.
                    break continueOn;
                }
                
                if (stateTable[row + RBBIDataWrapper.ACCEPTING] == 0 &&
                    stateTable[row + RBBIDataWrapper.LOOKAHEAD] != 0) {
                    // Lookahead match point.  Remember it, but only if no other rule
                    //                         has unconditionally matched to this point.
                    // TODO:  handle case where there's a pending match from a different rule
                    //        where lookaheadStatus != 0  && lookaheadStatus != row->fLookAhead.
                    int  r = fText.getIndex();
                    if (r > result) {
                        lookaheadResult = r;
                        lookaheadStatus = stateTable[row + RBBIDataWrapper.LOOKAHEAD];
                        lookaheadTagIdx = stateTable[row + RBBIDataWrapper.TAGIDX];
                    }
                    break continueOn;
                }
                
                if (stateTable[row + RBBIDataWrapper.ACCEPTING] != 0 &&
                        stateTable[row + RBBIDataWrapper.LOOKAHEAD] != 0) {
                    // Lookahead match is completed.  Set the result accordingly, but only
                    //   if no other rule has matched further in the mean time.
                    //  TODO:  CHECK THIS LOGIC.  It looks backwards.
                    //         These are _reverse_ rules.  
                    if (lookaheadResult > result) {
                        if (stateTable[row + RBBIDataWrapper.ACCEPTING] != lookaheadStatus) {  
                            // TODO:  handle this case of overlapping lookahead matches.
                            //        With correctly written rules, we won't get here.
                            // System.out.println("Trouble in handlePrevious()"); 
                        }
                        result               = lookaheadResult;
                        fLastRuleStatusIndex = lookaheadTagIdx;
                        lookaheadStatus      = 0;
                    }
                    break continueOn;
                }   
            }   // end of continueOn block.

            if (state == STOP_STATE) {
                break;
            }

            // then advance one character backwards
            c = CIPrevious32(fText);
        }

        // Note:  the result position isn't what is returned to the user by previous(),
        //        but where the implementation of previous() turns around and
        //        starts iterating forward again.
        if (c == CharacterIterator.DONE && CIHasPrevious(fText)==false) {
            result = fText.getBeginIndex();
        }
        fText.setIndex(result);

        return result;
    }
    
    
    private int handlePrevious(short stateTable[]) {
        if (fText == null || stateTable == null) {
            return 0;
        }
        // break tag is no longer valid after icu switched to exact backwards
        // positioning.
        fLastStatusIndexValid = false;
        if (stateTable == null) {
            return fText.getBeginIndex();
        }

        int            state              = START_STATE;
        int            category;
        int            lastCategory       = 0;
        boolean        hasPassedStartText = !CIHasPrevious(fText);
        int            c                  = CIPrevious32(fText);
        // previous character
        int            result             = fText.getIndex();
        int            lookaheadStatus    = 0;
        int            lookaheadResult    = 0;
        int            lookaheadTagIdx    = 0;
        boolean        lookAheadHardBreak = 
            (stateTable[RBBIDataWrapper.FLAGS+1] & RBBIDataWrapper.RBBI_LOOKAHEAD_HARD_BREAK) != 0;
  
        int            row = fRData.getRowIndex(state);

        category = (short)fRData.fTrie.getCodePointValue(c);
        category &= ~0x4000;            // Mask off dictionary bit.

        if (fTrace) {
            System.out.println("Handle Prev   pos   char  state category ");
        }

        // loop until we reach the beginning of the text or transition to state 0
        for (;;) {
            if (hasPassedStartText) {
                // if we have already considered the start of the text
                if (stateTable[row + RBBIDataWrapper.LOOKAHEAD] != 0 &&
                        lookaheadResult == 0) {
                    result = 0;
                }
                break;
            }

            // save the last character's category and look up the current
            // character's category
            lastCategory = category;
            category = (short)fRData.fTrie.getCodePointValue(c);

            category &= ~0x4000;    // Clear the dictionary bit flag
                                    //   (Should be unused; holdover from old RBBI)

            if (fTrace) {
                System.out.print("             " + fText.getIndex()+ "   ");
                if (0x20<=c && c<0x7f) {
                    System.out.print("  " +  c + "  ");
                } else {
                    System.out.print(" " + Integer.toHexString(c) + " ");
                }
                System.out.println(" " + state + "  " + category + " ");
            }

            // look up a state transition in the backwards state table
            state = stateTable[row + RBBIDataWrapper.NEXTSTATES + category];
            row = fRData.getRowIndex(state);

            if (stateTable[row + RBBIDataWrapper.ACCEPTING] == -1) {
                // Match found, common case, could have lookahead so we move on to check it
                result = fText.getIndex();
                fLastRuleStatusIndex   = stateTable[row + RBBIDataWrapper.TAGIDX];
            }

            if (stateTable[row + RBBIDataWrapper.LOOKAHEAD] != 0) {
                if (lookaheadStatus != 0
                    && stateTable[row + RBBIDataWrapper.ACCEPTING] == lookaheadStatus) {
                    // Lookahead match is completed.  Set the result accordingly, but only
                    // if no other rule has matched further in the mean time.
                    result               = lookaheadResult;
                    fLastRuleStatusIndex = lookaheadTagIdx;
                    lookaheadStatus      = 0;
                    /// i think we have to back up to read the lookahead character again
                    /// fText->setIndex(lookaheadResult);
                    /// TODO: this is a simple hack since reverse rules only have simple
                    /// lookahead rules that we can definitely break out from.
                    /// we need to make the lookahead rules not chain eventually.
                    /// return result;
                    /// this is going to be the longest match again

                    /// syn wee todo hard coded for line breaks stuff
                    /// needs to provide a tag in rules to ensure a stop.
                    
                    if (lookAheadHardBreak) {
                        break;
                    }
                    category = lastCategory;
                    fText.setIndex(result);
                } else {
                    // Hit a possible look-ahead match.  We are at the
                    // position of the '/'.  Remember this position.
                    lookaheadResult      = fText.getIndex();
                    lookaheadStatus      = stateTable[row + RBBIDataWrapper.LOOKAHEAD];
                    fLastRuleStatusIndex = stateTable[row + RBBIDataWrapper.TAGIDX];
                }
            } else {               
                // not lookahead...                
                if (stateTable[row + RBBIDataWrapper.ACCEPTING] != 0) {
                    // Normal style Accepting state.
                    lookaheadStatus = 0;     //  Clear out any pending look-ahead matches.
                }
            }
            
            if (state == STOP_STATE) {
                break;
            }

            // then move iterator position backwards one character
            hasPassedStartText = !CIHasPrevious(fText);
            c = CIPrevious32(fText);
        }

        // Note:  the result postion isn't what is returned to the user by previous(),
        //        but where the implementation of previous() turns around and
        //        starts iterating forward again.
        fText.setIndex(result);

        return result;
    }

}





