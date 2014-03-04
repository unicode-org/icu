/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.text.CharacterIterator;

/**
 * <p>SearchIterator is an abstract base class that defines a protocol
 * for text searching. Subclasses provide concrete implementations of
 * various search algorithms.  A concrete subclass, StringSearch, is
 * provided that implements language-sensitive pattern matching based
 * on the comparison rules defined in a RuleBasedCollator
 * object. Instances of SearchIterator maintain a current position and
 * scan over the target text, returning the indices where a match is
 * found and the length of each match. Generally, the sequence of forward 
 * matches will be equivalent to the sequence of backward matches.One
 * case where this statement may not hold is when non-overlapping mode 
 * is set on and there are continuous repetitive patterns in the text. 
 * Consider the case searching for pattern "aba" in the text 
 * "ababababa", setting overlapping mode off will produce forward matches
 * at offsets 0, 4. However when a backwards search is done, the
 * results will be at offsets 6 and 2.</p>
 * 
 * <p>If matches searched for have boundary restrictions. BreakIterators 
 * can be used to define the valid boundaries of such a match. Once a 
 * BreakIterator is set, potential matches will be tested against the
 * BreakIterator to determine if the boundaries are valid and that all
 * characters in the potential match are equivalent to the pattern 
 * searched for. For example, looking for the pattern "fox" in the text
 * "foxy fox" will produce match results at offset 0 and 5 with length 3
 * if no BreakIterators were set. However if a WordBreakIterator is set,
 * the only match that would be found will be at the offset 5. Since,
 * the SearchIterator guarantees that if a BreakIterator is set, all its
 * matches will match the given pattern exactly, a potential match that 
 * passes the BreakIterator might still not produce a valid match. For 
 * instance the pattern "e" will not be found in the string 
 * "&#92;u00e9" (latin small letter e with acute) if a 
 * CharacterBreakIterator is used. Even though "e" is
 * a part of the character "&#92;u00e9" and the potential match at
 * offset 0 length 1 passes the CharacterBreakIterator test, "&#92;u00e9"
 * is not equivalent to "e", hence the SearchIterator rejects the potential
 * match. By default, the SearchIterator
 * does not impose any boundary restriction on the matches, it will 
 * return all results that match the pattern. Illustrating with the 
 * above example, "e" will
 * be found in the string "&#92;u00e9" if no BreakIterator is
 * specified.</p>
 * 
 * <p>SearchIterator also provides a means to handle overlapping
 * matches via the API setOverlapping(boolean). For example, if
 * overlapping mode is set, searching for the pattern "abab" in the
 * text "ababab" will match at positions 0 and 2, whereas if
 * overlapping is not set, SearchIterator will only match at position
 * 0. By default, overlapping mode is not set.</p>
 * 
 * <p>The APIs in SearchIterator are similar to that of other text
 * iteration classes such as BreakIterator. Using this class, it is
 * easy to scan through text looking for all occurances of a
 * match.</p>
 * <p>
 * Example of use:<br>
 * <pre>
 * String target = "The quick brown fox jumped over the lazy fox";
 * String pattern = "fox";
 * SearchIterator iter = new StringSearch(pattern, target);
 * for (int pos = iter.first(); pos != SearchIterator.DONE; 
 *                                                       pos = iter.next()) {
 *     // println matches at offset 16 and 41 with length 3
 *     System.out.println("Found match at " + pos + ", length is " 
 *                        + iter.getMatchLength());
 * }
 * target = "ababababa";
 * pattern = "aba";
 * iter.setTarget(new StringCharacterIterator(pattern));
 * iter.setOverlapping(false);
 * System.out.println("Overlapping mode set to false");
 * System.out.println("Forward matches of pattern " + pattern + " in text "
 *                    + text + ": ");
 * for (int pos = iter.first(); pos != SearchIterator.DONE; 
 *                                                       pos = iter.next()) {
 *     // println matches at offset 0 and 4 with length 3
 *     System.out.println("offset " + pos + ", length " 
 *                        + iter.getMatchLength());
 * }
 * System.out.println("Backward matches of pattern " + pattern + " in text "
 *                    + text + ": ");
 * for (int pos = iter.last(); pos != SearchIterator.DONE; 
 *                                                    pos = iter.previous()) {
 *     // println matches at offset 6 and 2 with length 3
 *     System.out.println("offset " + pos + ", length " 
 *                        + iter.getMatchLength());
 * }
 * System.out.println("Overlapping mode set to true");
 * System.out.println("Index set to 2");
 * iter.setIndex(2);
 * iter.setOverlapping(true);
 * System.out.println("Forward matches of pattern " + pattern + " in text "
 *                    + text + ": ");
 * for (int pos = iter.first(); pos != SearchIterator.DONE; 
 *                                                       pos = iter.next()) {
 *     // println matches at offset 2, 4 and 6 with length 3
 *     System.out.println("offset " + pos + ", length " 
 *                        + iter.getMatchLength());
 * }
 * System.out.println("Index set to 2");
 * iter.setIndex(2);
 * System.out.println("Backward matches of pattern " + pattern + " in text "
 *                    + text + ": ");
 * for (int pos = iter.last(); pos != SearchIterator.DONE; 
 *                                                    pos = iter.previous()) {
 *     // println matches at offset 0 with length 3
 *     System.out.println("offset " + pos + ", length " 
 *                        + iter.getMatchLength());
 * }
 * </pre>
 * </p>
 * @author Laura Werner, synwee
 * @stable ICU 2.0
 * @see BreakIterator
 */
public abstract class SearchIterator 
{
    /**
     * The BreakIterator to define the boundaries of a logical match.
     * This value can be a null.
     * See class documentation for more information.
     * @see #setBreakIterator(BreakIterator)
     * @see #getBreakIterator
     * @see BreakIterator
     * @stable ICU 2.0
     */
    protected BreakIterator breakIterator; 

    /**
     * Target text for searching.
     * @see #setTarget(CharacterIterator)
     * @see #getTarget
     * @stable ICU 2.0
     */
    protected CharacterIterator targetText;
    /**
     * Length of the most current match in target text. 
     * Value 0 is the default value.
     * @see #setMatchLength
     * @see #getMatchLength
     * @stable ICU 2.0
     */
    protected int matchLength;

    /**
     * Java port of ICU4C struct USearch (usrchimp.h)
     * 
     * Note:
     * 
     *  ICU4J already exposed some protected members such as
     * targetText, brekIterator and matchedLength as a part of stable
     * APIs. In ICU4C, they are exposed through USearch struct, 
     * although USearch struct itself is internal API.
     * 
     *  This class was created for making ICU4J code in parallel to
     * ICU4C implementation. ICU4J implementation access member
     * fields like C struct (e.g. search_.isOverlap_) mostly, except
     * fields already exposed as protected member (e.g. search_.text()).
     * 
     */
    final class Search {

        CharacterIterator text() {
            return SearchIterator.this.targetText;
        }

        void setTarget(CharacterIterator text) {
            SearchIterator.this.targetText = text;
        }

        /** Flag to indicate if overlapping search is to be done.
            E.g. looking for "aa" in "aaa" will yield matches at offset 0 and 1. */
        boolean isOverlap_;

        boolean isCanonicalMatch_;

        ElementComparisonType elementComparisonType_;

        BreakIterator internalBreakIter_;

        BreakIterator breakIter() {
            return SearchIterator.this.breakIterator;
        }

        void setBreakIter(BreakIterator breakIter) {
            SearchIterator.this.breakIterator = breakIter;
        }

        int matchedIndex_;

        int matchedLength() {
            return SearchIterator.this.matchLength;
        }

        void setMatchedLength(int matchedLength) {
            SearchIterator.this.matchLength = matchedLength;
        }

        /** Flag indicates if we are doing a forwards search */
        boolean isForwardSearching_;

        /** Flag indicates if we are at the start of a string search.
            This indicates that we are in forward search and at the start of m_text. */ 
        boolean reset_;

        // Convenient methods for accessing begin/end index of the
        // target text. These are ICU4J only and are not data fields.
        int beginIndex() {
            if (targetText == null) {
                return 0;
            }
            return targetText.getBeginIndex();
        }

        int endIndex() {
            if (targetText == null) {
                return 0;
            }
            return targetText.getEndIndex();
        }
    }

    Search search_ = new Search();

    // public data members -------------------------------------------------
    
    /**
     * DONE is returned by previous() and next() after all valid matches have 
     * been returned, and by first() and last() if there are no matches at all.
     * @see #previous
     * @see #next
     * @stable ICU 2.0
     */
    public static final int DONE = -1;
    
    // public methods -----------------------------------------------------
    
    // public setters -----------------------------------------------------
    
    /**
     * <p>
     * Sets the position in the target text at which the next search will start.
     * This method clears any previous match.
     * </p>
     * @param position position from which to start the next search
     * @exception IndexOutOfBoundsException thrown if argument position is out
     *            of the target text range.
     * @see #getIndex
     * @stable ICU 2.8
     */
    public void setIndex(int position) {
        if (position < search_.beginIndex() 
            || position > search_.endIndex()) {
            throw new IndexOutOfBoundsException(
                "setIndex(int) expected position to be between " +
                search_.beginIndex() + " and " + search_.endIndex());
        }
        search_.reset_ = false;
        search_.setMatchedLength(0);
        search_.matchedIndex_ = DONE;
    }
    
    /**
     * <p>
     * Determines whether overlapping matches are returned. See the class 
     * documentation for more information about overlapping matches.
     * </p>
     * <p>
     * The default setting of this property is false
     * </p>
     * @param allowOverlap flag indicator if overlapping matches are allowed
     * @see #isOverlapping
     * @stable ICU 2.8
     */
    public void setOverlapping(boolean allowOverlap)
    {
        search_.isOverlap_ = allowOverlap;
    }
    
    /**
     * Set the BreakIterator that is used to restrict the points at which 
     * matches are detected.
     * Using <tt>null</tt> as the parameter is legal; it means that break 
     * detection should not be attempted.
     * See class documentation for more information.
     * @param breakiter A BreakIterator that will be used to restrict the 
     *                     points at which matches are detected.
     * @see #getBreakIterator
     * @see BreakIterator
     * @stable ICU 2.0
     */
    public void setBreakIterator(BreakIterator breakiter) 
    {
        search_.setBreakIter(breakiter);
        if (search_.breakIter() != null) {
            // Create a clone of CharacterItearator, so it won't
            // affect the position currently held by search_.text()
            if (search_.text() != null) {
                search_.breakIter().setText((CharacterIterator)search_.text().clone());
            }
        }
    }

    /**
     * Set the target text to be searched. Text iteration will then begin at 
      * the start of the text string. This method is useful if you want to 
     * reuse an iterator to search within a different body of text.
     * @param text new text iterator to look for match, 
     * @exception IllegalArgumentException thrown when text is null or has
     *               0 length
     * @see #getTarget
     * @stable ICU 2.4
     */
    public void setTarget(CharacterIterator text)
    {
        if (text == null || text.getEndIndex() == text.getIndex()) {
            throw new IllegalArgumentException("Illegal null or empty text");
        }

        text.setIndex(text.getBeginIndex());
        search_.setTarget(text);
        search_.matchedIndex_ = DONE;
        search_.setMatchedLength(0);
        search_.reset_ = true;
        search_.isForwardSearching_ = true;
        if (search_.breakIter() != null) {
            // Create a clone of CharacterItearator, so it won't
            // affect the position currently held by search_.text()
            search_.breakIter().setText((CharacterIterator)text.clone());
        }
        if (search_.internalBreakIter_ != null) {
            search_.internalBreakIter_.setText((CharacterIterator)text.clone());
        }
    }

    //TODO: We should add APIs below to match ICU4C APIs
    // setCanonicalMatch
    // setElementComparison

    // public getters ----------------------------------------------------
    
    /**
     * <p>
     * Returns the index of the most recent match in the target text.
     * This call returns a valid result only after a successful call to 
     * {@link #first}, {@link #next}, {@link #previous}, or {@link #last}.
     * Just after construction, or after a searching method returns 
     * <tt>DONE</tt>, this method will return <tt>DONE</tt>.
     * </p>
     * <p>
     * Use <tt>getMatchLength</tt> to get the length of the matched text.
     * <tt>getMatchedText</tt> will return the subtext in the searched 
     * target text from index getMatchStart() with length getMatchLength(). 
     * </p>
     * @return index to a substring within the text string that is being 
     *         searched.
     * @see #getMatchLength
     * @see #getMatchedText
     * @see #first
     * @see #next
     * @see #previous
     * @see #last
     * @see #DONE
     * @stable ICU 2.8
     */
    public int getMatchStart()
    {
        return search_.matchedIndex_;
    }

    /**
     * Return the index in the target text at which the iterator is currently
     * positioned. 
     * If the iteration has gone past the end of the target text, or past 
     * the beginning for a backwards search, {@link #DONE} is returned.
     * @return index in the target text at which the iterator is currently 
     *         positioned.
     * @stable ICU 2.8
     * @see #first
     * @see #next
     * @see #previous
     * @see #last
     * @see #DONE
     */
    public abstract int getIndex();
    
    /**
     * <p>
     * Returns the length of the most recent match in the target text. 
     * This call returns a valid result only after a successful
     * call to {@link #first}, {@link #next}, {@link #previous}, or 
     * {@link #last}.
     * Just after construction, or after a searching method returns
     * <tt>DONE</tt>, this method will return 0. See getMatchStart() for 
     * more details.
     * </p>
     * @return The length of the most recent match in the target text, or 0 if 
     *         there is no match.
     * @see #getMatchStart
     * @see #getMatchedText
     * @see #first
     * @see #next
     * @see #previous
     * @see #last
     * @see #DONE
     * @stable ICU 2.0
     */
    public int getMatchLength() 
    {
        return search_.matchedLength();
    }
    
    /**
     * Returns the BreakIterator that is used to restrict the indexes at which 
     * matches are detected. This will be the same object that was passed to 
     * the constructor or to <code>setBreakIterator</code>.
     * If the BreakIterator has not been set, <tt>null</tt> will be returned.
     * See setBreakIterator for more information.
     * @return the BreakIterator set to restrict logic matches
     * @see #setBreakIterator
     * @see BreakIterator
     * @stable ICU 2.0
     */
    public BreakIterator getBreakIterator() 
    {
        return search_.breakIter();
    }
    
    /**
     * Return the target text that is being searched.
     * @return target text being searched.
     * @see #setTarget
     * @stable ICU 2.0
     */
    public CharacterIterator getTarget() 
    {
        return search_.text();
    }
    
    /**
     * Returns the text that was matched by the most recent call to 
     * {@link #first}, {@link #next}, {@link #previous}, or {@link #last}. 
     * If the iterator is not pointing at a valid match, for instance just 
     * after construction or after <tt>DONE</tt> has been returned, an empty 
     * String will be returned. See getMatchStart for more information
     * @see #getMatchStart
     * @see #getMatchLength
     * @see #first
     * @see #next
     * @see #previous
     * @see #last
     * @see #DONE
     * @return the substring in the target text of the most recent match 
     * @stable ICU 2.0
     */
    public String getMatchedText() 
    {
        if (search_.matchedLength() > 0) {
            int limit = search_.matchedIndex_ + search_.matchedLength();
            StringBuilder result = new StringBuilder(search_.matchedLength());
            CharacterIterator it = search_.text();
            it.setIndex(search_.matchedIndex_);
            while (it.getIndex() < limit) {
                result.append(it.current());
                it.next();
            }
            it.setIndex(search_.matchedIndex_);
            return result.toString();
        }
        return null;
    }

    // miscellaneous public methods -----------------------------------------
        
    /**
     * Search <b>forwards</b> in the target text for the next valid match,
     * starting the search from the current iterator position. The iterator is 
     * adjusted so that its current index, as returned by {@link #getIndex},
     * is the starting position of the match if one was found. If a match is 
     * found, the index of the match is returned, otherwise <tt>DONE</tt> is
     * returned.  If overlapping mode is set, the beginning of the found match
     * can be before the end of the current match, if any.
     * @return The starting index of the next forward match after the current 
     *         iterator position, or 
     *         <tt>DONE</tt> if there are no more matches.
     * @see #getMatchStart
     * @see #getMatchLength
     * @see #getMatchedText
     * @see #following
     * @see #preceding
     * @see #previous
     * @see #first
     * @see #last
     * @see #DONE
     * @stable ICU 2.0
     */
    public int next()
    {
        int index = getIndex(); // offset = getOffset() in ICU4C
        int matchindex = search_.matchedIndex_;
        int matchlength = search_.matchedLength();
        search_.reset_ = false;
        if (search_.isForwardSearching_) {
            int endIdx = search_.endIndex();
            if (index == endIdx || matchindex == endIdx ||
                    (matchindex != DONE &&
                    matchindex + matchlength >= endIdx)) {
                setMatchNotFound();
                return DONE;
            }
        } else {
            // switching direction.
            // if matchedIndex == DONE, it means that either a 
            // setIndex (setOffset in C) has been called or that previous ran off the text
            // string. the iterator would have been set to offset 0 if a 
            // match is not found.
            search_.isForwardSearching_ = true;
            if (search_.matchedIndex_ != DONE) {
                // there's no need to set the collation element iterator
                // the next call to next will set the offset.
                return matchindex;
            }
        }

        if (matchlength > 0) {
            // if matchlength is 0 we are at the start of the iteration
            if (search_.isOverlap_) {
                index++;
            } else {
                index += matchlength;
            }
        }

        return handleNext(index);
    }

    /**
     * Search <b>backwards</b> in the target text for the next valid match,
     * starting the search from the current iterator position. The iterator is 
     * adjusted so that its current index, as returned by {@link #getIndex},
     * is the starting position of the match if one was found. If a match is 
     * found, the index is returned, otherwise <tt>DONE</tt> is returned.  If
     * overlapping mode is set, the end of the found match can be after the
     * beginning of the previous match, if any.
     * @return The starting index of the next backwards match after the current 
     *         iterator position, or 
     *         <tt>DONE</tt> if there are no more matches.
     * @see #getMatchStart
     * @see #getMatchLength
     * @see #getMatchedText
     * @see #following
     * @see #preceding
     * @see #next
     * @see #first
     * @see #last
     * @see #DONE
     * @stable ICU 2.0
     */
    public int previous()
    {
        int index;  // offset in ICU4C
        if (search_.reset_) {
            index = search_.endIndex();   // m_search_->textLength in ICU4C
            search_.isForwardSearching_ = false;
            search_.reset_ = false;
            setIndex(index);
        } else {
            index = getIndex();
        }

        int matchindex = search_.matchedIndex_;
        if (search_.isForwardSearching_) {
            // switching direction. 
            // if matchedIndex == DONE, it means that either a 
            // setIndex (setOffset in C) has been called or that next ran off the text
            // string. the iterator would have been set to offset textLength if 
            // a match is not found.
            search_.isForwardSearching_ = false;
            if (matchindex != DONE) {
                return matchindex;
            }
        } else {
            int startIdx = search_.beginIndex();
            if (index == startIdx || matchindex == startIdx) {
                // not enough characters to match
                setMatchNotFound();
                return DONE; 
            }
        }

        if (matchindex != DONE) {
            if (search_.isOverlap_) {
                matchindex += search_.matchedLength() - 2;
            }

            return handlePrevious(matchindex);
        }

        return handlePrevious(index);
    }

    /**
     * Return true if the overlapping property has been set.
     * See setOverlapping(boolean) for more information.
     * @see #setOverlapping
     * @return true if the overlapping property has been set, false otherwise
     * @stable ICU 2.8
     */
    public boolean isOverlapping() 
    {
        return search_.isOverlap_;
    }

    //TODO: We should add APIs below to match ICU4C APIs
    // isCanonicalMatch
    // getElementComparison

    /** 
     * <p>
     * Resets the search iteration. All properties will be reset to their
     * default values.
     * </p>
     * <p>
     * If a forward iteration is initiated, the next search will begin at the
     * start of the target text. Otherwise, if a backwards iteration is initiated,
     * the next search will begin at the end of the target text.
     * </p>
     * @stable ICU 2.8
     */
    public void reset()
    {
        setMatchNotFound();
        setIndex(search_.beginIndex());
        search_.isOverlap_ = false;
        search_.isCanonicalMatch_ = false;
        search_.elementComparisonType_ = ElementComparisonType.STANDARD_ELEMENT_COMPARISON;
        search_.isForwardSearching_ = true;
        search_.reset_ = true;
    }
    
    /**
     * Return the index of the first <b>forward</b> match in the target text. 
     * This method sets the iteration to begin at the start of the 
     * target text and searches forward from there.
     * @return The index of the first forward match, or <code>DONE</code> 
     *            if there are no matches.
     * @see #getMatchStart
     * @see #getMatchLength
     * @see #getMatchedText
     * @see #following
     * @see #preceding
     * @see #next
     * @see #previous
     * @see #last
     * @see #DONE
     * @stable ICU 2.0
     */
    public final int first() 
    {
        int startIdx = search_.beginIndex();
        setIndex(startIdx);
        return handleNext(startIdx);
    }

    /**
     * Return the index of the first <b>forward</b> match in target text that 
     * is at or after argument <tt>position</tt>. 
     * This method sets the iteration to begin at the specified
     * position in the the target text and searches forward from there.
     * @return The index of the first forward match, or <code>DONE</code> 
     *         if there are no matches.
     * @see #getMatchStart
     * @see #getMatchLength
     * @see #getMatchedText
     * @see #first
     * @see #preceding
     * @see #next
     * @see #previous
     * @see #last
     * @see #DONE
     * @stable ICU 2.0
     */
    public final int following(int position) 
    {
        setIndex(position);
        return handleNext(position);
    }
    
    /**
     * Return the index of the first <b>backward</b> match in target text. 
     * This method sets the iteration to begin at the end of the 
     * target text and searches backwards from there.
     * @return The starting index of the first backward match, or 
     *         <code>DONE</code> if there are no matches.
     * @see #getMatchStart
     * @see #getMatchLength
     * @see #getMatchedText
     * @see #first
     * @see #preceding
     * @see #next
     * @see #previous
     * @see #following
     * @see #DONE
     * @stable ICU 2.0
     */
    public final int last() 
    {
        int endIdx = search_.endIndex();
        setIndex(endIdx);
        return handlePrevious(endIdx);
    }
     
    /**
     * Return the index of the first <b>backwards</b> match in target 
     * text that ends at or before argument <tt>position</tt>. 
     * This method sets the iteration to begin at the argument
     * position index of the target text and searches backwards from there.
     * @return The starting index of the first backwards match, or 
     *         <code>DONE</code> 
     *         if there are no matches.
     * @see #getMatchStart
     * @see #getMatchLength
     * @see #getMatchedText
     * @see #first
     * @see #following
     * @see #next
     * @see #previous
     * @see #last
     * @see #DONE
     * @stable ICU 2.0
     */
    public final int preceding(int position) 
    {
        setIndex(position);
        return handlePrevious(position);
    }

    // protected constructor ----------------------------------------------
    
    /**
     * Protected constructor for use by subclasses.
     * Initializes the iterator with the argument target text for searching 
     * and sets the BreakIterator.
     * See class documentation for more details on the use of the target text
     * and BreakIterator.
     * @param target The target text to be searched.
     * @param breaker A {@link BreakIterator} that is used to determine the 
     *                boundaries of a logical match. This argument can be null.
     * @exception IllegalArgumentException thrown when argument target is null,
     *            or of length 0
     * @see BreakIterator  
     * @stable ICU 2.0
     */
    protected SearchIterator(CharacterIterator target, BreakIterator breaker)
    {
        if (target == null 
            || (target.getEndIndex() - target.getBeginIndex()) == 0) {
                throw new IllegalArgumentException(
                                   "Illegal argument target. " +
                                   " Argument can not be null or of length 0");
        }

        search_.setTarget(target);
        search_.setBreakIter(breaker);
        if (search_.breakIter() != null) {
            search_.breakIter().setText((CharacterIterator)target.clone());
        }
        search_.isOverlap_ = false;
        search_.isCanonicalMatch_ = false;
        search_.elementComparisonType_ = ElementComparisonType.STANDARD_ELEMENT_COMPARISON;
        search_.isForwardSearching_ = true;
        search_.reset_ = true;
        search_.matchedIndex_ = DONE;
        search_.setMatchedLength(0);
    }    

    // protected methods --------------------------------------------------

   
    /**
     * Sets the length of the most recent match in the target text. 
     * Subclasses' handleNext() and handlePrevious() methods should call this 
     * after they find a match in the target text.    
     * @param length new length to set
     * @see #handleNext
     * @see #handlePrevious
     * @stable ICU 2.0
     */
    protected void setMatchLength(int length)
    {
        search_.setMatchedLength(length);
    }

    /**
     * <p>
     * Abstract method that subclasses override to provide the mechanism 
     * for finding the next <b>forwards</b> match in the target text. This 
     * allows different subclasses to provide different search algorithms.
     * </p> 
     * <p>
     * If a match is found, this function must call setMatchLength(int) to
     * set the length of the result match.
     * The iterator is adjusted so that its current index, as returned by 
     * {@link #getIndex}, is the starting position of the match if one was 
     * found. If a match is not found, <tt>DONE</tt> will be returned.
     * </p> 
     * @param start index in the target text at which the forwards search 
     *        should begin.
     * @return the starting index of the next forwards match if found, DONE 
     *         otherwise
     * @see #setMatchLength(int)
     * @see #handlePrevious(int)
     * @see #DONE
     * @stable ICU 2.0
     */
    protected abstract int handleNext(int start);
    
    /**
     * <p>
     * Abstract method which subclasses override to provide the mechanism 
     * for finding the next <b>backwards</b> match in the target text. 
     * This allows different 
     * subclasses to provide different search algorithms. 
     * </p> 
     * <p>
     * If a match is found, this function must call setMatchLength(int) to
     * set the length of the result match.
     * The iterator is adjusted so that its current index, as returned by 
     * {@link #getIndex}, is the starting position of the match if one was 
     * found. If a match is not found, <tt>DONE</tt> will be returned.
     * </p> 
     * @param startAt index in the target text at which the backwards search 
     *        should begin.
     * @return the starting index of the next backwards match if found, 
     *         DONE otherwise
     * @see #setMatchLength(int)
     * @see #handleNext(int)
     * @see #DONE
     * @stable ICU 2.0
     */
    protected abstract int handlePrevious(int startAt);

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    //TODO: This protected method is @stable 2.0 in ICU4C
    protected void setMatchNotFound() {
        search_.matchedIndex_ = DONE;
        search_.setMatchedLength(0);
    }

    /**
     * Option to control how collation elements are compared.
     * The default value will be {@link #STANDARD_ELEMENT_COMPARISON}.
     * 
     * @see #setElementComparisonType(ElementComparisonType)
     * @see #getElementComparisonType()
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public enum ElementComparisonType {
        /**
         * Standard collation element comparison at the specified collator strength.
         * 
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        STANDARD_ELEMENT_COMPARISON,
        /**
         * <p>Collation element comparison is modified to effectively provide behavior
         * between the specified strength and strength - 1.</p>
         * 
         * <p>Collation elements in the pattern that have the base weight for the specified
         * strength are treated as "wildcards" that match an element with any other
         * weight at that collation level in the searched text. For example, with a
         * secondary-strength English collator, a plain 'e' in the pattern will match
         * a plain e or an e with any diacritic in the searched text, but an e with
         * diacritic in the pattern will only match an e with the same diacritic in
         * the searched text.<p>
         * 
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        PATTERN_BASE_WEIGHT_IS_WILDCARD,

        /**
         * <p>Collation element comparison is modified to effectively provide behavior
         * between the specified strength and strength - 1.</p>
         * 
         * <p>Collation elements in either the pattern or the searched text that have the
         * base weight for the specified strength are treated as "wildcards" that match
         * an element with any other weight at that collation level. For example, with
         * a secondary-strength English collator, a plain 'e' in the pattern will match
         * a plain e or an e with any diacritic in the searched text, but an e with
         * diacritic in the pattern will only match an e with the same diacritic or a
         * plain e in the searched text.</p>
         * 
         * @draft ICU 53
         * @provisional This API might change or be removed in a future release.
         */
        ANY_BASE_WEIGHT_IS_WILDCARD
    }

    /**
     * <p>Sets the collation element comparison type.</p>
     * 
     * <p>The default comparison type is {@link ElementComparisonType#STANDARD_ELEMENT_COMPARISON}.</p>
     * 
     * @see ElementComparisonType
     * @see #getElementComparisonType()
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public void setElementComparisonType(ElementComparisonType type) {
        search_.elementComparisonType_ = type;
    }

    /**
     * <p>Returns the collation element comparison type.</p>
     * 
     * @see ElementComparisonType
     * @see #setElementComparisonType(ElementComparisonType)
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ElementComparisonType getElementComparisonType() {
        return search_.elementComparisonType_;
    }
}
