/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.text.CharacterIterator;
import java.io.InputStream;
import java.io.IOException;

/**
 * <p>A subclass of BreakIterator whose behavior is specified using a list of rules.</p>
 * @stable ICU 2.0
 */

public class RuleBasedBreakIterator extends BreakIterator {

    private RuleBasedBreakIterator delegatedThis = null;

    //=======================================================================
    // constructors
    //=======================================================================

    /**
     * Constructs a RuleBasedBreakIterator_Old according to the description
     * provided.  If the description is malformed, throws an
     * IllegalArgumentException.  Normally, instead of constructing a
     * RuleBasedBreakIterator_Old directory, you'll use the factory methods
     * on BreakIterator to create one indirectly from a description
     * in the framework's resource files.  You'd use this when you want
     * special behavior not provided by the built-in iterators.
     * @stable ICU 2.0
     */
    public RuleBasedBreakIterator(String description) {
        delegatedThis = new RuleBasedBreakIterator_Old(description);
    }

    /**
     * This default constructor is used when creating derived classes
     * of RulesBasedBreakIterator.  Not intended for use by normal
     * clients of break iterators.
     * @internal ICU 3.0
     */
    protected RuleBasedBreakIterator() {
        delegatedThis = this;
    }
    
    /**
     * Get a break iterator based on a set of pre-compiled break rules.
     * 
     * @param is An input stream that supplies the compiled rule data.  The
     * format of the rule data on the stream is that of a rule data file
     * produced by the ICU4C tool "genbrk".
     * @return A RuleBasedBreakIterator based on the supplied break rules.
     * @throws IOException
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static RuleBasedBreakIterator getInstanceFromCompiledRules(InputStream is) throws IOException {
        return RuleBasedBreakIterator_New.getInstanceFromCompiledRules(is);      
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
        RuleBasedBreakIterator result = (RuleBasedBreakIterator) super.clone();
        result.delegatedThis = result;
        if (delegatedThis != this) {
            // We will come here if "this" is an actual RuleBasedBreakIterator that
            //  was instantiated via a constructor.  In this situation, delegatedThis
            //  refers to a separately instantiated RuleBasedBreakIterator_old, to
            //  which all operations are delegated.  For the more common case of a
            //  break iterator instantiated from one of the factory methods, the
            //  actual object type is RuleBasedBreakIterator_New, and delegatedThis
            //  for the object refers to itself.
            result.delegatedThis = (RuleBasedBreakIterator) delegatedThis.clone();
        }
        return result;
    }

    /**
     * Returns true if both BreakIterators are of the same class, have the same
     * rules, and iterate over the same text.
     * @stable ICU 2.0
     */
    public boolean equals(Object that) {
        return delegatedThis.equals(that);
    }

    /**
     * Returns the description used to create this iterator
     * @stable ICU 2.0
     */
    public String toString() {
        return delegatedThis.toString();
    }

    /**
     * Compute a hashcode for this BreakIterator
     * @return A hash code
     * @stable ICU 2.0
     */
    public int hashCode()
    {
        return delegatedThis.hashCode();
    }

    /** 
     * Tag value for "words" that do not fit into any of other categories. 
     * Includes spaces and most punctuation. 
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_NONE           = 0;

    /**
     * Upper bound for tags for uncategorized words. 
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_NONE_LIMIT     = 100;

    /**
     * Tag value for words that appear to be numbers, lower limit. 
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_NUMBER         = 100;

    /** 
     * Tag value for words that appear to be numbers, upper limit.
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_NUMBER_LIMIT   = 200;

    /** 
     * Tag value for words that contain letters, excluding
     * hiragana, katakana or ideographic characters, lower limit. 
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_LETTER         = 200;

    /** 
     * Tag value for words containing letters, upper limit 
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_LETTER_LIMIT   = 300;

    /** 
     * Tag value for words containing kana characters, lower limit
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_KANA           = 300;

    /** 
     * Tag value for words containing kana characters, upper limit
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_KANA_LIMIT     = 400;

    /**
     * Tag value for words containing ideographic characters, lower limit
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_IDEO           = 400;

    /**
     * Tag value for words containing ideographic characters, upper limit
     * @draft ICU 3.0 
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final int WORD_IDEO_LIMIT     = 500;

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
        return delegatedThis.first();
    }

    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
     * @stable ICU 2.0
     */
    public int last() {
        return delegatedThis.last();
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
        return delegatedThis.next(n);
    }

    /**
     * Advances the iterator to the next boundary position.
     * @return The position of the first boundary after this one.
     * @stable ICU 2.0
     */
    public int next() {
        return delegatedThis.next();
    }

    /**
     * Advances the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     * @stable ICU 2.0
     */
    public int previous() {
        return delegatedThis.previous();
    }

    /**
     * Sets the iterator to refer to the first boundary position following
     * the specified position.
     * @param offset The position from which to begin searching for a break position.
     * @return The position of the first break after the current position.
     * @stable ICU 2.0
     */
    public int following(int offset) {
        return delegatedThis.following(offset);
    }

    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @param offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     * @stable ICU 2.0
     */
    public int preceding(int offset) {
        return delegatedThis.preceding(offset);
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
        return delegatedThis.isBoundary(offset);
    }

    /**
     * Returns the current iteration position.
     * @return The current iteration position.
     * @stable ICU 2.0
     */
    public int current() {
        return delegatedThis.current();
    }

    /**
     * Return the status tag from the break rule that determined the most recently
     * returned break position.  The values appear in the rule source
     * within brackets, {123}, for example.  For rules that do not specify a
     * status, a default value of 0 is returned.  If more than one rule applies,
     * the numerically largest of the possible status values is returned.
     * <p>
     * The values used by the standard ICU break rules are defined as
     * constants in this class, and allow distinguishing between words
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
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public int  getRuleStatus() {
        return delegatedThis.getRuleStatus();
    }

    /**
     * Get the status (tag) values from the break rule(s) that determined the most 
     * recently returned break position.  The values appear in the rule source
     * within brackets, {123}, for example.  The default status value for rules
     * that do not explicitly provide one is zero.
     * <p>
     * The values used by the standard ICU rules are defined as contants in
     * this class.
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
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public int getRuleStatusVec(int[] fillInArray) {
        return delegatedThis.getRuleStatusVec(fillInArray);
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
        return delegatedThis.getText();
    }

    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText An iterator over the text to analyze.
     * @stable ICU 2.0
     */
    public void setText(CharacterIterator newText) {
        delegatedThis.setText(newText);
    }
}
