/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/SearchIterator.java,v $ 
 * $Date: 2000/03/10 04:07:23 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

package com.ibm.text;

import java.text.CharacterIterator;
import java.text.BreakIterator;

/**
 * <code>SearchIterator</code> is an abstract base class that provides methods
 * to search for a pattern within a text string.  Instances of
 * <code>SearchIterator</code> maintain a current position and scan over
 * the target text, returning the indices the pattern is matched
 * and the length of each match.
 * <p>
 * <code>SearchIterator</code> is an abstract base class that defines a
 * protocol for text searching.  Subclasses provide concrete implementations of
 * various search algorithms.  For example, {@link StringSearch}
 * implements language-sensitive pattern matching based on the comparison rules
 * defined in a {@link java.text.RuleBasedCollator RuleBasedCollator} object.
 * <p>
 * Internally, <code>SearchIterator</code> scans text using a
 * {@link CharacterIterator}, and is thus able to scan text held
 * by any object implementing that protocol. A <code>StringCharacterIterator</code>
 * is used to scan <code>String</code> objects passed to <code>setText</code>.
 * <p>
 * <code>SearchIterator</code> provides an API that is similar to that of
 * other text iteration classes such as <code>BreakIterator</code>.  Using this
 * class, it is easy to scan through text looking for all occurances of a
 * given pattern.  The following example uses a <code>StringSearch</code> object to
 * find all instances of "fox" in the target string.  Any other subclass of
 * <code>SearchIterator</code> can be used in an identical manner.
 * <pre><code>
 * String target = "The quick brown fox jumped over the lazy fox";
 * String pattern = "fox";
 *
 * SearchIterator iter = new StringSearch(pattern, target);
 *
 * for (int pos = iter.first(); pos != SearchIterator.DONE; pos = iter.next()) {
 *     System.out.println("Found match at " + pos +
 *                        ", length is " + iter.getMatchLength());
 * }
 * </code></pre>
 *
 * @see StringSearch
 */
public abstract class SearchIterator {
    /**
     * DONE is returned by previous() and next() after all valid
     * matches have been returned, and by first() and last() if
     * there are no matches at all.
     */
    public static final int DONE = -1;
    
    /**
     * Private value indicating that the iterator is pointing
     * before the beginning of the target text.
     */
    private static final int BEFORE = -2;

    /**
     * Return the first index at which the target text matches the search
     * pattern.  The iterator is adjusted so that its current index
     * (as returned by {@link #getIndex}) is the match posisition if one was found
     * and <code>DONE</code> if one was not.
     *
     * @return The character index of the first match, or <code>DONE</code> if there
     *          are no matches.
     */
    final public int first() {
        setIndex(BEFORE);
        return next();
    }

    /**
     * Return the first index greater than <tt>pos</tt> at which the target
     * text matches the search pattern.   The iterator is adjusted so that its current index
     * (as returned by {@link #getIndex}) is the match posisition if one was found
     * and <code>DONE</code> if one was not.
     *
     * @return The character index of the first match following <code>pos</code>,
     *          or <tt>DONE</tt> if there are no matches.
     */
    final public int following(int pos) {
        setIndex(pos);
        return next();
    }
    
    /**
     * Return the last index in the target text at which it matches
     * the search pattern and adjusts the iteration to point to that position.
     *
     * @return The index of the first match, or <tt>DONE</tt> if there
     *          are no matches.
     */
    final public int last() {
        setIndex(DONE);
        return previous();
    }

    /**
     * Return the first index less than <code>pos</code> at which the target
     * text matches the search pattern.   The iterator is adjusted so that its current index
     * (as returned by {@link #getIndex}) is the match posisition if one was found
     * and <tt>DONE</tt> if one was not.
     *
     * @return The character index of the first match preceding <code>pos</code>,
     *          or <code>DONE</code> if there are no matches.
     */
    final public int preceding(int pos) {
        setIndex(pos);
        return previous();
    }
    
    /**
     * Return the index of the next point at which the text matches the
     * search pattern, starting from the current position
     * <p>
     * @return The index of the next match after the current position,
     *          or <code>DONE</code> if there are no more matches.
     *
     * @see #first
     */
    public int next() {
        if (index == BEFORE){
            // Starting at the beginning of the text
            index = target.getBeginIndex();
        } else if (length > 0) {
            // Finding the next match after a previous one
            index += overlap ? 1 : length;
        }
        index -= 1;
        
        do {
            length = 0;
            index = handleNext(index + 1);
        } while (index != DONE && !isBreakUnit(index, index+length));
        
        return index;
    }

    /**
     * Return the index of the previous point at which the text matches
     * the search pattern, starting at the current position
     *
     * @return The index of the previous match before the current position,
     *          or <code>DONE</code> if there are no more matches.
     */
    public int previous() {
        if (index == DONE) {
            index = target.getEndIndex();
        } else if (length > 0) {
            // Finding the previous match before a following one
            index = overlap ? index + length - 1 : index;
        }
        index += 1;
        
        do {
            length = 0;
            index = handlePrev(index - 1);
        } while (index != DONE && !isBreakUnit(index, index+length));

        if (index == DONE) {
            index = BEFORE;
        }
        return getIndex();
    }



    /**
     * Return the current index in the text being searched.
     * If the iteration has gone past the end of the text
     * (or past the beginning for a backwards search), 
     * {@link #DONE} is returned.
     */
    public int getIndex() {
        return index == BEFORE ? DONE : index;
    }

    /**
     * Determines whether overlapping matches are returned.  If this
     * property is <code>true</code>, matches that begin within the
     * boundry of the previous match are considered valid and will
     * be returned.  For example, when searching for "abab" in the
     * target text "ababab", both offsets 0 and 2 will be returned
     * as valid matches if this property is <code>true</code>.
     * <p>
     * The default setting of this property is <tt>true</tt>
     */
    public void setOverlapping(boolean allowOverlap) {
        overlap = allowOverlap;
    }
    
    /**
     * Determines whether overlapping matches are returned.
     *
     * @see #setOverlapping
     */
    public boolean isOverlapping() {
        return overlap;
    }
    
    /**
     * Returns the length of text in the target which matches the search
     * pattern.  This call returns a valid result only after a successful
     * call to {@link #first}, {@link #next}, {@link #previous}, or {@link #last}.
     * Just after construction, or after a searching method returns
     * <tt>DONE</tt>, this method will return 0.
     *
     * @return The length of the match in the target text, or 0 if there
     *          is no match currently.
     */
    public int getMatchLength() {
        return length;
    }

    /**
     * Set the BreakIterator that will be used to restrict the points
     * at which matches are detected.
     *
     * @param breaker   A {@link java.text.BreakIterator BreakIterator}
     *                  that will be used to restrict the points
     *                  at which matches are detected.  If a match is found, but the match's start
     *                  or end index is not a boundary as determined by
     *                  the <tt>BreakIterator</tt>, the match will be rejected and
     *                  another will be searched for.
     *
     *                  If this parameter is <tt>null</tt>, no break
     *                  detection is attempted.
     *
     * @see #getBreakIterator
     */
    public void setBreakIterator(BreakIterator iterator) {
        breaker = iterator;
        if (breaker != null) {
            breaker.setText(target);
        }
    }
    
    /**
     * Returns the BreakIterator that is used to restrict the points
     * at which matches are detected.  This will be the same object
     * that was passed to the constructor or to <code>setBreakIterator</code>.
     * Note that <tt>null</tt> is a legal value; it means that break
     * detection should not be attempted.
     *
     * @see #setBreakIterator
     */
    public BreakIterator getBreakIterator() {
        return breaker;
    }
    
    /**
     * Set the target text which should be searched and resets the
     * iterator's position to point before the start of the target text.
     * This method is useful if you want to re-use an iterator to
     * search for the same pattern within a different body of text.
     *
     * @see #getTarget
     */
    public void setTarget(CharacterIterator iterator) {
        target = iterator;
        if (breaker != null) {
            breaker.setText(target);
        }
        setIndex(BEFORE);
    }
    
    /**
     * Return the target text which is being searched
     *
     * @see #setTarget
     */
    public CharacterIterator getTarget() {
        return target;
    }
    
    /**
     * Returns the text that was matched by the most recent call to 
     * {@link #first}, {@link #next}, {@link #previous}, or {@link #last}.
     * If the iterator is not pointing at a valid match (e.g. just after
     * construction or after <tt>DONE</tt> has been returned, returns
     * an empty string.
     */
    public String getMatchedText() {
        StringBuffer buffer = new StringBuffer();
        
        if (length > 0) {
            int i = 0;
            for (char c = target.setIndex(index); i < length; c = target.next(), i++)
            {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    //-------------------------------------------------------------------
    // Protected interface for subclasses
    //-------------------------------------------------------------------

    /**
     * Constructor for use by subclasses
     * <p>
     * @param target    The target text to be searched.  This is for internal
     *                  use by this class.  Subclasses need to maintain their
     *                  own reference to or iterator over the target text
     *                  for use by their {@link #handleNext handleNext} and
     *                  {@link #handlePrev handlePrev} methods.
     *
     * @param breaker   A {@link BreakIterator} that is used to restrict the points
     *                  at which matches are detected.  If <tt>handleNext</tt> or
     *                  <tt>handlePrev</tt> finds a match, but the match's start
     *                  or end index is not a boundary as determined by
     *                  the <tt>BreakIterator</tt>, the match is rejected and 
     *                  <tt>handleNext</tt> or <tt>handlePrev</tt> is called again.
     *                  If this parameter is <tt>null</tt>, no break
     *                  detection is attempted.
     *                  
     */
    protected SearchIterator(CharacterIterator target, BreakIterator breaker)
    {
        this.target = target;
        
        if (breaker != null) {
            this.breaker = (BreakIterator)breaker.clone();
            this.breaker.setText(target);
        }
        
        index = target.getBeginIndex();
        length = 0;
    }

    /**
     * Abstract method which subclasses override to provide the mechanism
     * for finding the next match in the target text.  This allows different
     * subclasses to provide different search algorithms.
     * <p>
     * If a match is found, the implementation should return the index at
     * which the match starts and should call {@link #setMatchLength setMatchLength}
     * with the number of characters in the target
     * text that make up the match.  If no match is found, the method
     * should return DONE and should not call <tt>setMatchLength</tt>.
     * <p>
     * @param startAt   The index in the target text at which the search
     *                  should start.
     *
     * @see #setMatchLength
     */
    protected abstract int handleNext(int startAt);

    /**
     * Abstract method which subclasses override to provide the mechanism
     * for finding the previous match in the target text.  This allows different
     * subclasses to provide different search algorithms.
     * <p>
     * If a match is found, the implementation should return the index at
     * which the match starts and should call {@link #setMatchLength setMatchLength}
     * with the number of characters in the target
     * text that make up the match.  If no match is found, the method
     * should return DONE and should not call <tt>setMatchLength</tt>.
     * <p>
     * @param startAt   The index in the target text at which the search
     *                  should start.
     *
     * @see #setMatchLength
     */
    protected abstract int handlePrev(int startAt);

    /**
     * Sets the length of the currently matched string in the target text.
     * Subclasses' <code>handleNext</code> and <code>handlePrev</code>
     * methods should call this when they find a match in the target text.
     */
    protected void setMatchLength(int length) {
        this.length = length;
    }

    //-------------------------------------------------------------------
    // Privates
    //
    
    /**
     * Internal method used by preceding and following.  Sets the index
     * to point to the given position, and clears any state that's
     * affected.
     */
    private void setIndex(int pos) {
        index = pos;
        length = 0;
    }
    
    /**
     * Determine whether the target text bounded by <code>start</code> and
     * <code>end</code> is one or more whole units of text as determined by
     * the current <code>BreakIterator</code>.
     */
    private boolean isBreakUnit(int start, int end)
    {
        if (breaker == null) {
            return true;
        } 
        boolean startBound = breaker.isBoundary(start);
        boolean endBound = (end == target.getEndIndex()) || breaker.isBoundary(end);
        
        return startBound && endBound;
    }
    
    //-------------------------------------------------------------------------
    // Private data...
    //-------------------------------------------------------------------------
    private int                 index;          // Current position in the target text
    private int                 length;         // Length of matched text, or 0
    private boolean             overlap = true; // Return overlapping matches?
    private CharacterIterator   target;         // Target text to be searched
    private BreakIterator       breaker;        // Break iterator to constrain matches

    //-------------------------------------------------------------------------
    // Debugging support...
    //-------------------------------------------------------------------------

    static private final boolean DEBUG = false;

    static void debug(String str) {
        System.err.println(str);
    }
};