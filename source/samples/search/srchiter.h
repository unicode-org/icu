/*
**********************************************************************
*   Copyright (C) 1999-2000 IBM and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*  03/22/2000   helena      Creation.
**********************************************************************
*/
#ifndef SRCHITER_H
#define SRCHITER_H

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/brkiter.h"

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
 * defined in a {@link RuleBasedCollator} object.
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
 * UnicodeString target("The quick brown fox jumped over the lazy fox");
 * UnicodeString pattern("fox");
 *
 * SearchIterator *iter = new StringSearch(pattern, target);
 *
 * for (int pos = iter->first(); pos != SearchIterator::DONE; pos = iter->next()) {
 *     printf("Found match at %d pos, length is %d\n", pos, iter.getMatchLength());
 * }
 * </code></pre>
 *
 * @see StringSearch
 */

class SearchIterator {
public:
    /**
     * DONE is returned by previous() and next() after all valid
     * matches have been returned, and by first() and last() if
     * there are no matches at all.
     */
     static  const int32_t DONE;
    
    //=======================================================================
    // boilerplate
    //=======================================================================

    /**
     * Destructor
     */
    virtual ~SearchIterator();

    /** copy constructor */
    SearchIterator(const    SearchIterator&   other);

    /**
     * Equality operator.  Returns TRUE if both BreakIterators are of the
     * same class, have the same behavior, and iterate over the same text.
     */
    virtual bool_t operator==(const SearchIterator& that) const;

    /**
     * Not-equal operator.  If operator== returns TRUE, this returns FALSE,
     * and vice versa.
     */
    bool_t operator!=(const SearchIterator& that) const;

    /**
     * Returns a newly-constructed RuleBasedBreakIterator with the same
     * behavior, and iterating over the same text, as this one.
     */
    virtual SearchIterator* clone(void) const = 0;

    /**
     * Return a polymorphic class ID for this object. Different subclasses
     * will return distinct unequal values.
     * @stable
     */
    virtual UClassID getDynamicClassID(void) const = 0;

    /**
     * Return the first index at which the target text matches the search
     * pattern.  The iterator is adjusted so that its current index
     * (as returned by {@link #getIndex}) is the match posisition if one was found
     * and <code>DONE</code> if one was not.
     *
     * @return The character index of the first match, or <code>DONE</code> if there
     *          are no matches.
     */
     int32_t first(void);

    /**
     * Return the first index greater than <tt>pos</tt> at which the target
     * text matches the search pattern.   The iterator is adjusted so that its current index
     * (as returned by {@link #getIndex}) is the match posisition if one was found
     * and <code>DONE</code> if one was not.
     *
     * @return The character index of the first match following <code>pos</code>,
     *          or <tt>DONE</tt> if there are no matches.
     */
    int32_t following(int32_t pos);
    
    /**
     * Return the last index in the target text at which it matches
     * the search pattern and adjusts the iteration to point to that position.
     *
     * @return The index of the first match, or <tt>DONE</tt> if there
     *          are no matches.
     */
    int32_t last(void);

    /**
     * Return the first index less than <code>pos</code> at which the target
     * text matches the search pattern.   The iterator is adjusted so that its current index
     * (as returned by {@link #getIndex}) is the match posisition if one was found
     * and <tt>DONE</tt> if one was not.
     *
     * @return The character index of the first match preceding <code>pos</code>,
     *          or <code>DONE</code> if there are no matches.
     */
    int32_t preceding(int32_t pos);
    
    /**
     * Return the index of the next point at which the text matches the
     * search pattern, starting from the current position
     * <p>
     * @return The index of the next match after the current position,
     *          or <code>DONE</code> if there are no more matches.
     *
     * @see #first
     */
     int32_t next(void);

    /**
     * Return the index of the previous point at which the text matches
     * the search pattern, starting at the current position
     *
     * @return The index of the previous match before the current position,
     *          or <code>DONE</code> if there are no more matches.
     */
    int32_t previous(void);

    /**
     * Return the current index in the text being searched.
     * If the iteration has gone past the end of the text
     * (or past the beginning for a backwards search), 
     * {@link #DONE} is returned.
     */
    int32_t getIndex(void) const;
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
    void setOverlapping(bool_t allowOverlap);
    
    /**
     * Determines whether overlapping matches are returned.
     *
     * @see #setOverlapping
     */
    bool_t isOverlapping(void) const;
    
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
    int32_t getMatchLength(void) const;

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
    /* HSYS : Check, aliasing or owning */
    void setBreakIterator(const BreakIterator* iterator);
    
    /**
     * Returns the BreakIterator that is used to restrict the points
     * at which matches are detected.  This will be the same object
     * that was passed to the constructor or to <code>setBreakIterator</code>.
     * Note that <tt>null</tt> is a legal value; it means that break
     * detection should not be attempted.
     *
     * @see #setBreakIterator
     */
    const BreakIterator& getBreakIterator(void) const;
    
    /**
     * Set the target text which should be searched and resets the
     * iterator's position to point before the start of the target text.
     * This method is useful if you want to re-use an iterator to
     * search for the same pattern within a different body of text.
     *
     * @see #getTarget
     */
    virtual void setTarget(const UnicodeString& newText);    

    /**
     * Set the target text which should be searched and resets the
     * iterator's position to point before the start of the target text.
     * This method is useful if you want to re-use an iterator to
     * search for the same pattern within a different body of text.
     *
     * @see #getTarget
     */
    virtual void adoptTarget(CharacterIterator* iterator);
    /**
     * Return the target text which is being searched
     *
     * @see #setTarget
     */
    const CharacterIterator& getTarget(void) const;
    
    /** Reset the iteration.
    */
    virtual void reset(void);

    /**
     * Returns the text that was matched by the most recent call to 
     * {@link #first}, {@link #next}, {@link #previous}, or {@link #last}.
     * If the iterator is not pointing at a valid match (e.g. just after
     * construction or after <tt>DONE</tt> has been returned, returns
     * an empty string.
     */
    void getMatchedText(UnicodeString& result);

    //-------------------------------------------------------------------
    // Protected interface for subclasses
    //-------------------------------------------------------------------

protected:
    SearchIterator();

    /**
     * Constructor for use by subclasses
     * <p>
     * @param target    The target text to be searched.  This is for internal
     *                  use by this class.  Subclasses need to maintain their
     *                  own reference to or iterator over the target text
     *                  for use by their {@link #handleNext handleNext} and
     *                  {@link #handlePrev handlePrev} methods.  The target will
     *                  be adopted and owned by the SearchIterator object.
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
    SearchIterator(CharacterIterator* target, 
                   BreakIterator* breaker);
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
    virtual int32_t handleNext(int32_t startAt, UErrorCode& status) = 0;

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
     virtual int32_t handlePrev(int32_t startAt, UErrorCode& status) = 0;

    /**
     * Sets the length of the currently matched string in the target text.
     * Subclasses' <code>handleNext</code> and <code>handlePrev</code>
     * methods should call this when they find a match in the target text.
     */
    void setMatchLength(int32_t length);

    //-------------------------------------------------------------------
    // Privates
    //
private:
    /**
     * Class ID
     */
    static char fgClassID;
private:    
    /**
     * Private value indicating that the iterator is pointing
     * before the beginning of the target text.
     */
     static const int32_t BEFORE;

    /**
     * Internal method used by preceding and following.  Sets the index
     * to point to the given position, and clears any state that's
     * affected.
     */
    void setIndex(int32_t pos);
    
    /**
     * Determine whether the target text bounded by <code>start</code> and
     * <code>end</code> is one or more whole units of text as determined by
     * the current <code>BreakIterator</code>.
     */
    bool_t isBreakUnit(int32_t start, int32_t end);
    
    //-------------------------------------------------------------------------
    // Private data...
    //-------------------------------------------------------------------------
    int32_t                 index;          // Current position in the target text
    int32_t                 length;         // Length of matched text, or 0
    bool_t                  overlap;        // Return overlapping matches?
    CharacterIterator*      target;         // Target text to be searched
    BreakIterator*          breaker;        // Break iterator to constrain matches
    bool_t                  backward;
};

inline bool_t SearchIterator::operator!=(const SearchIterator& that) const
{
   return !operator==(that); 
}

#endif

