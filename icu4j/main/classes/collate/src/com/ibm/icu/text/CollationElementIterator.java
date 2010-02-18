/**
*******************************************************************************
* Copyright (C) 1996-2010, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*
*******************************************************************************
*/
package com.ibm.icu.text;

/***
 * import java.text.StringCharacterIterator;
 * import java.text.CharacterIterator;
 */
import com.ibm.icu.impl.Norm2AllModes;
import com.ibm.icu.impl.Normalizer2Impl;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.StringUCharacterIterator;
import com.ibm.icu.impl.CharacterIteratorWrapper;
import com.ibm.icu.impl.ICUDebug;
import com.ibm.icu.lang.UCharacter;
import java.text.CharacterIterator;
import java.util.MissingResourceException;

/**
 * <p><code>CollationElementIterator</code> is an iterator created by
 * a RuleBasedCollator to walk through a string. The return result of
 * each iteration is a 32-bit collation element that defines the
 * ordering priority of the next character or sequence of characters
 * in the source string.</p>
 *
 * <p>For illustration, consider the following in Spanish:
 * <blockquote>
 * <pre>
 * "ca" -> the first collation element is collation_element('c') and second
 *         collation element is collation_element('a').
 *
 * Since "ch" in Spanish sorts as one entity, the below example returns one
 * collation element for the two characters 'c' and 'h'
 *
 * "cha" -> the first collation element is collation_element('ch') and second
 *          collation element is collation_element('a').
 * </pre>
 * </blockquote>
 * And in German,
 * <blockquote>
 * <pre>
 * Since the character '&#230;' is a composed character of 'a' and 'e', the
 * iterator returns two collation elements for the single character '&#230;'
 *
 * "&#230;b" -> the first collation element is collation_element('a'), the
 *              second collation element is collation_element('e'), and the
 *              third collation element is collation_element('b').
 * </pre>
 * </blockquote>
 * </p>
 *
 * <p>For collation ordering comparison, the collation element results
 * can not be compared simply by using basic arithmetric operators,
 * e.g. &lt;, == or &gt;, further processing has to be done. Details
 * can be found in the ICU
 * <a href="http://www.icu-project.org/userguide/Collate_ServiceArchitecture.html">
 * user guide</a>. An example of using the CollationElementIterator
 * for collation ordering comparison is the class
 * <a href=StringSearch.html> com.ibm.icu.text.StringSearch</a>.</p>
 *
 * <p>To construct a CollationElementIterator object, users
 * call the method getCollationElementIterator() on a
 * RuleBasedCollator that defines the desired sorting order.</p>
 *
 * <p> Example:
 * <blockquote>
 * <pre>
 *  String testString = "This is a test";
 *  RuleBasedCollator rbc = new RuleBasedCollator("&amp;a&lt;b");
 *  CollationElementIterator iterator = rbc.getCollationElementIterator(testString);
 *  int primaryOrder = iterator.IGNORABLE;
 *  while (primaryOrder != iterator.NULLORDER) {
 *      int order = iterator.next();
 *      if (order != iterator.IGNORABLE &&
 *          order != iterator.NULLORDER) {
 *          // order is valid, not ignorable and we have not passed the end
 *          // of the iteration, we do something
 *          primaryOrder = CollationElementIterator.primaryOrder(order);
 *          System.out.println("Next primary order 0x" +
 *                             Integer.toHexString(primaryOrder));
 *      }
 *  }
 * </pre>
 * </blockquote>
 * </p>
 * <p>
 * This class is not subclassable
 * </p>
 * @see Collator
 * @see RuleBasedCollator
 * @see StringSearch
 * @author Syn Wee Quek
 * @stable ICU 2.8
 */
public final class CollationElementIterator
{
  
    
    // public data members --------------------------------------------------

    /**
     * <p>This constant is returned by the iterator in the methods
     * next() and previous() when the end or the beginning of the
     * source string has been reached, and there are no more valid
     * collation elements to return.</p>
     *
     * <p>See class documentation for an example of use.</p>
     * @stable ICU 2.8
     * @see #next
     * @see #previous */
    public final static int NULLORDER = 0xffffffff;

    /**
     * <p>This constant is returned by the iterator in the methods
     * next() and previous() when a collation element result is to be
     * ignored.</p>
     *
     * <p>See class documentation for an example of use.</p>
     * @stable ICU 2.8
     * @see #next
     * @see #previous */
    public static final int IGNORABLE = 0;

    // public methods -------------------------------------------------------

    // public getters -------------------------------------------------------

    /**
     * <p>Returns the character offset in the source string
     * corresponding to the next collation element. I.e., getOffset()
     * returns the position in the source string corresponding to the
     * collation element that will be returned by the next call to
     * next(). This value could be any of:
     * <ul>
     * <li> The index of the <b>first</b> character corresponding to
     * the next collation element. (This means that if
     * <code>setOffset(offset)</code> sets the index in the middle of
     * a contraction, <code>getOffset()</code> returns the index of
     * the first character in the contraction, which may not be equal
     * to the original offset that was set. Hence calling getOffset()
     * immediately after setOffset(offset) does not guarantee that the
     * original offset set will be returned.)
     * <li> If normalization is on, the index of the <b>immediate</b>
     * subsequent character, or composite character with the first
     * character, having a combining class of 0.
     * <li> The length of the source string, if iteration has reached
     * the end.
     *</ul>
     * </p>
     * @return The character offset in the source string corresponding to the
     *         collation element that will be returned by the next call to
     *         next().
     * @stable ICU 2.8
     */
    public int getOffset()
    {
        if (m_bufferOffset_ != -1) {
            if (m_isForwards_) {
                return m_FCDLimit_;
            }
            return m_FCDStart_;
        }
        return m_source_.getIndex();
    }


    /**
     * <p> Returns the maximum length of any expansion sequence that ends with
     * the specified collation element. If there is no expansion with this
     * collation element as the last element, returns 1.
     * </p>
     * @param ce a collation element returned by previous() or next().
     * @return the maximum length of any expansion sequence ending
     *         with the specified collation element.
     * @stable ICU 2.8
     */
    public int getMaxExpansion(int ce)
    {
        int start = 0;
        int limit = m_collator_.m_expansionEndCE_.length;
        long unsignedce = ce & 0xFFFFFFFFl;
        while (start < limit - 1) {
            int mid = start + ((limit - start) >> 1);
            long midce = m_collator_.m_expansionEndCE_[mid] & 0xFFFFFFFFl;
            if (unsignedce <= midce) {
                limit = mid;
            }
            else {
                start = mid;
            }
        }
        int result = 1;
        if (m_collator_.m_expansionEndCE_[start] == ce) {
            result = m_collator_.m_expansionEndCEMaxSize_[start];
        }
        else if (limit < m_collator_.m_expansionEndCE_.length &&
                 m_collator_.m_expansionEndCE_[limit] == ce) {
            result = m_collator_.m_expansionEndCEMaxSize_[limit];
        }
        else if ((ce & 0xFFFF) == 0x00C0) {
            result = 2;
        }
        return result;
    }

    // public other methods -------------------------------------------------

    /**
     * <p> Resets the cursor to the beginning of the string. The next
     * call to next() or previous() will return the first and last
     * collation element in the string, respectively.</p>
     *
     * <p>If the RuleBasedCollator used by this iterator has had its
     * attributes changed, calling reset() will reinitialize the
     * iterator to use the new attributes.</p>
     *
     * @stable ICU 2.8
     */
    public void reset()
    {
        m_source_.setToStart();
        updateInternalState();
    }

    /**
     * <p>Get the next collation element in the source string.</p>
     *
     * <p>This iterator iterates over a sequence of collation elements
     * that were built from the string. Because there isn't
     * necessarily a one-to-one mapping from characters to collation
     * elements, this doesn't mean the same thing as "return the
     * collation element [or ordering priority] of the next character
     * in the string".</p>
     *
     * <p>This function returns the collation element that the
     * iterator is currently pointing to, and then updates the
     * internal pointer to point to the next element.  Previous()
     * updates the pointer first, and then returns the element. This
     * means that when you change direction while iterating (i.e.,
     * call next() and then call previous(), or call previous() and
     * then call next()), you'll get back the same element twice.</p>
     *
     * @return the next collation element or NULLORDER if the end of the
     *         iteration has been reached.
     * @stable ICU 2.8
     */
    public int next()
    {
        m_isForwards_ = true;
        if (m_CEBufferSize_ > 0) {
            if (m_CEBufferOffset_ < m_CEBufferSize_) {
                // if there are expansions left in the buffer, we return it
                return m_CEBuffer_[m_CEBufferOffset_ ++];
            }
            m_CEBufferSize_ = 0;
            m_CEBufferOffset_ = 0;
        }
 
        int ch_int = nextChar();
        
        if (ch_int == UCharacterIterator.DONE) {
            return NULLORDER;
        }
        char ch = (char)ch_int;
        if (m_collator_.m_isHiragana4_) {
            /* Codepoints \u3099-\u309C are both Hiragana and Katakana. Set the flag
             * based on whether the previous codepoint was Hiragana or Katakana.
             */
            m_isCodePointHiragana_ = (m_isCodePointHiragana_ && (ch >= 0x3099 && ch <= 0x309C)) || 
                                     ((ch >= 0x3040 && ch <= 0x309e) && !(ch > 0x3094 && ch < 0x309d));
        }

        int result = NULLORDER;
        if (ch <= 0xFF) {
            // For latin-1 characters we never need to fall back to the UCA
            // table because all of the UCA data is replicated in the
            // latinOneMapping array
            result = m_collator_.m_trie_.getLatin1LinearValue(ch);
            if (RuleBasedCollator.isSpecial(result)) {
                result = nextSpecial(m_collator_, result, ch);
            }
        }
        else {
            result = m_collator_.m_trie_.getLeadValue(ch);
            //System.out.println(Integer.toHexString(result));
            if (RuleBasedCollator.isSpecial(result)) {
                // surrogate leads are handled as special ces
                result = nextSpecial(m_collator_, result, ch);
            }
            if (result == CE_NOT_FOUND_ && RuleBasedCollator.UCA_ != null) {
                // couldn't find a good CE in the tailoring
                // if we got here, the codepoint MUST be over 0xFF - so we look
                // directly in the UCA
                result = RuleBasedCollator.UCA_.m_trie_.getLeadValue(ch);
                if (RuleBasedCollator.isSpecial(result)) {
                    // UCA also gives us a special CE
                    result = nextSpecial(RuleBasedCollator.UCA_, result, ch);
                }
            }
        }
        if(result == CE_NOT_FOUND_) { 
            // maybe there is no UCA, unlikely in Java, but ported for consistency
            result = nextImplicit(ch); 
        }
        return result;
    }

    /**
     * <p>Get the previous collation element in the source string.</p>
     *
     * <p>This iterator iterates over a sequence of collation elements
     * that were built from the string. Because there isn't
     * necessarily a one-to-one mapping from characters to collation
     * elements, this doesn't mean the same thing as "return the
     * collation element [or ordering priority] of the previous
     * character in the string".</p>
     *
     * <p>This function updates the iterator's internal pointer to
     * point to the collation element preceding the one it's currently
     * pointing to and then returns that element, while next() returns
     * the current element and then updates the pointer. This means
     * that when you change direction while iterating (i.e., call
     * next() and then call previous(), or call previous() and then
     * call next()), you'll get back the same element twice.</p>
     *
     * @return the previous collation element, or NULLORDER when the start of
     *             the iteration has been reached.
     * @stable ICU 2.8
     */
    public int previous()
    {
        if (m_source_.getIndex() <= 0 && m_isForwards_) {
            // if iterator is new or reset, we can immediate perform  backwards
            // iteration even when the offset is not right.
            m_source_.setToLimit();
            updateInternalState();
        }
        m_isForwards_ = false;
        int result = NULLORDER;
        if (m_CEBufferSize_ > 0) {
            if (m_CEBufferOffset_ > 0) {
                return m_CEBuffer_[-- m_CEBufferOffset_];
            }
            m_CEBufferSize_ = 0;
            m_CEBufferOffset_ = 0;
        }
        int ch_int = previousChar();
        if (ch_int == UCharacterIterator.DONE) {
            return NULLORDER;
        }
        char ch = (char)ch_int;
        if (m_collator_.m_isHiragana4_) {
            m_isCodePointHiragana_ = (ch >= 0x3040 && ch <= 0x309f);
        }
        if (m_collator_.isContractionEnd(ch) && !isBackwardsStart()) {
            result = previousSpecial(m_collator_, CE_CONTRACTION_, ch);
        }
        else {
            if (ch <= 0xFF) {
                result = m_collator_.m_trie_.getLatin1LinearValue(ch);
            }
            else {
                result = m_collator_.m_trie_.getLeadValue(ch);
            }
            if (RuleBasedCollator.isSpecial(result)) {
                result = previousSpecial(m_collator_, result, ch);
            }
            if (result == CE_NOT_FOUND_) {
                if (!isBackwardsStart()
                    && m_collator_.isContractionEnd(ch)) {
                    result = CE_CONTRACTION_;
                }
                else {
                    if(RuleBasedCollator.UCA_ != null) {
                        result = RuleBasedCollator.UCA_.m_trie_.getLeadValue(ch);
                    }
                }

                if (RuleBasedCollator.isSpecial(result)) {
                    if(RuleBasedCollator.UCA_ != null) {                    
                        result = previousSpecial(RuleBasedCollator.UCA_, result, ch);
                    }
                }
            }
        }
        if(result == CE_NOT_FOUND_) {
            result = previousImplicit(ch);
        }
        return result;
    }

    /**
     * Return the primary order of the specified collation element,
     * i.e. the first 16 bits.  This value is unsigned.
     * @param ce the collation element
     * @return the element's 16 bits primary order.
     * @stable ICU 2.8
     */
    public final static int primaryOrder(int ce)
    {
        return (ce & RuleBasedCollator.CE_PRIMARY_MASK_)
            >>> RuleBasedCollator.CE_PRIMARY_SHIFT_;
    }
    /**
     * Return the secondary order of the specified collation element,
     * i.e. the 16th to 23th bits, inclusive.  This value is unsigned.
     * @param ce the collation element
     * @return the element's 8 bits secondary order
     * @stable ICU 2.8
     */
    public final static int secondaryOrder(int ce)
    {
        return (ce & RuleBasedCollator.CE_SECONDARY_MASK_)
            >> RuleBasedCollator.CE_SECONDARY_SHIFT_;
    }

    /**
     * Return the tertiary order of the specified collation element, i.e. the last
     * 8 bits.  This value is unsigned.
     * @param ce the collation element
     * @return the element's 8 bits tertiary order
     * @stable ICU 2.8
     */
    public final static int tertiaryOrder(int ce)
    {
        return ce & RuleBasedCollator.CE_TERTIARY_MASK_;
    }

    /**
     * <p> Sets the iterator to point to the collation element
     * corresponding to the character at the specified offset. The
     * value returned by the next call to next() will be the collation
     * element corresponding to the characters at offset.</p>
     *
     * <p>If offset is in the middle of a contracting character
     * sequence, the iterator is adjusted to the start of the
     * contracting sequence. This means that getOffset() is not
     * guaranteed to return the same value set by this method.</p>
     *
     * <p>If the decomposition mode is on, and offset is in the middle
     * of a decomposible range of source text, the iterator may not
     * return a correct result for the next forwards or backwards
     * iteration.  The user must ensure that the offset is not in the
     * middle of a decomposible range.</p>
     *
     * @param offset the character offset into the original source string to
     *        set. Note that this is not an offset into the corresponding
     *        sequence of collation elements.
     * @stable ICU 2.8
     */
    public void setOffset(int offset)
    {
        m_source_.setIndex(offset);
        int ch_int = m_source_.current();
        char ch = (char)ch_int;
        if (ch_int != UCharacterIterator.DONE && m_collator_.isUnsafe(ch)) {
            // if it is unsafe we need to check if it is part of a contraction
            // or a surrogate character
            if (UTF16.isTrailSurrogate(ch)) {
                // if it is a surrogate pair we move up one character
                char prevch = (char)m_source_.previous();
                if (!UTF16.isLeadSurrogate(prevch)) {
                    m_source_.setIndex(offset); // go back to the same index
                }
            }
            else {
                // could be part of a contraction
                // backup to a safe point and iterate till we pass offset
                while (m_source_.getIndex() > 0) {
                    if (!m_collator_.isUnsafe(ch)) {
                        break;
                    }
                    ch = (char)m_source_.previous();
                }
                updateInternalState();
                int prevoffset = 0;
                while (m_source_.getIndex() <= offset) {
                    prevoffset = m_source_.getIndex();
                    next();
                }
                m_source_.setIndex(prevoffset);
            }
        }
        updateInternalState();
        // direction code to prevent next and previous from returning a 
        // character if we are already at the ends
        offset = m_source_.getIndex();
        if (offset == 0/* m_source_.getBeginIndex() */) {
            // preventing previous() from returning characters from the end of 
            // the string again if we are at the beginning
            m_isForwards_ = false; 
        }
        else if (offset == m_source_.getLength()) {
            // preventing next() from returning characters from the start of 
            // the string again if we are at the end
            m_isForwards_ = true;
        }
    }

    /**
     * <p>Set a new source string for iteration, and reset the offset
     * to the beginning of the text.</p>
     *
     * @param source the new source string for iteration.
     * @stable ICU 2.8
     */
    public void setText(String source)
    {
        m_srcUtilIter_.setText(source);
        m_source_ = m_srcUtilIter_;
        updateInternalState();
    }
    
    /**
     * <p>Set a new source string iterator for iteration, and reset the
     * offset to the beginning of the text.
     * </p>
     * <p>The source iterator's integrity will be preserved since a new copy
     * will be created for use.</p>
     * @param source the new source string iterator for iteration.
     * @stable ICU 2.8
     */
    public void setText(UCharacterIterator source)
    {
        m_srcUtilIter_.setText(source.getText());
        m_source_ = m_srcUtilIter_;
        updateInternalState(); 
    }

    /**
     * <p>Set a new source string iterator for iteration, and reset the
     * offset to the beginning of the text.
     * </p>
     * @param source the new source string iterator for iteration.
     * @stable ICU 2.8
     */
    public void setText(CharacterIterator source)
    {
        m_source_ = new CharacterIteratorWrapper(source);
        m_source_.setToStart();
        updateInternalState();
    }

    // public miscellaneous methods -----------------------------------------

    /**
     * Tests that argument object is equals to this CollationElementIterator.
     * Iterators are equal if the objects uses the same RuleBasedCollator,
     * the same source text and have the same current position in iteration.
     * @param that object to test if it is equals to this
     *             CollationElementIterator
     * @stable ICU 2.8
     */
    public boolean equals(Object that)
    {
        if (that == this) {
            return true;
        }
        if (that instanceof CollationElementIterator) {
            CollationElementIterator thatceiter
                                              = (CollationElementIterator)that;
            if (!m_collator_.equals(thatceiter.m_collator_)) {
                return false;
            }
            // checks the text 
            return m_source_.getIndex() == thatceiter.m_source_.getIndex()
                   && m_source_.getText().equals(
                                            thatceiter.m_source_.getText());
        }
        return false;
    }

    // package private constructors ------------------------------------------

    private CollationElementIterator(RuleBasedCollator collator) {
        m_utilStringBuffer_ = new StringBuilder();
        m_collator_ = collator;
        m_CEBuffer_ = new int[CE_BUFFER_INIT_SIZE_];
        m_buffer_ = new StringBuilder();
        m_utilSpecialBackUp_ = new Backup();
        m_nfcImpl_.getFCDTrie();  // ensure the FCD data is initialized
    }

    /**
     * <p>CollationElementIterator constructor. This takes a source
     * string and a RuleBasedCollator. The iterator will walk through
     * the source string based on the rules defined by the
     * collator. If the source string is empty, NULLORDER will be
     * returned on the first call to next().</p>
     *
     * @param source the source string.
     * @param collator the RuleBasedCollator
     * @stable ICU 2.8
     */
    CollationElementIterator(String source, RuleBasedCollator collator)
    {
        this(collator);
        m_source_ = m_srcUtilIter_ = new StringUCharacterIterator(source);
        updateInternalState();
    }

    /**
     * <p>CollationElementIterator constructor. This takes a source
     * character iterator and a RuleBasedCollator. The iterator will
     * walk through the source string based on the rules defined by
     * the collator. If the source string is empty, NULLORDER will be
     * returned on the first call to next().</p>
     *
     * @param source the source string iterator.
     * @param collator the RuleBasedCollator
     * @stable ICU 2.8
     */
    CollationElementIterator(CharacterIterator source,
                             RuleBasedCollator collator)
    {
        this(collator);
        m_srcUtilIter_ = new StringUCharacterIterator();
        m_source_ = new CharacterIteratorWrapper(source);
        updateInternalState();
    }
    
    /**
     * <p>CollationElementIterator constructor. This takes a source
     * character iterator and a RuleBasedCollator. The iterator will
     * walk through the source string based on the rules defined by
     * the collator. If the source string is empty, NULLORDER will be
     * returned on the first call to next().</p>
     *
     * @param source the source string iterator.
     * @param collator the RuleBasedCollator
     * @stable ICU 2.8
     */
    CollationElementIterator(UCharacterIterator source,
                             RuleBasedCollator collator)
    {
        this(collator);
        m_srcUtilIter_ = new StringUCharacterIterator();
        m_srcUtilIter_.setText(source.getText());
        m_source_ = m_srcUtilIter_;
        updateInternalState();
    }

    // package private data members -----------------------------------------

    /**
     * true if current codepoint was Hiragana
     */
    boolean m_isCodePointHiragana_;
    /**
     * Position in the original string that starts with a non-FCD sequence
     */
    int m_FCDStart_;
    /**
     * This is the CE from CEs buffer that should be returned.
     * Initial value is 0.
     * Forwards iteration will end with m_CEBufferOffset_ == m_CEBufferSize_,
     * backwards will end with m_CEBufferOffset_ == 0.
     * The next/previous after we reach the end/beginning of the m_CEBuffer_
     * will cause this value to be reset to 0.
     */
    int m_CEBufferOffset_;

    /**
     * This is the position to which we have stored processed CEs.
     * Initial value is 0.
     * The next/previous after we reach the end/beginning of the m_CEBuffer_
     * will cause this value to be reset to 0.
     */
    int m_CEBufferSize_;
    static final int CE_NOT_FOUND_ = 0xF0000000;
    static final int CE_EXPANSION_TAG_ = 1;
    static final int CE_CONTRACTION_TAG_ = 2;
    /** 
     * Collate Digits As Numbers (CODAN) implementation
     */
    static final int CE_DIGIT_TAG_ = 13;

    // package private methods ----------------------------------------------

    /**
     * Sets the collator used.
     * Internal use, all data members will be reset to the default values
     * @param collator to set
     */
    void setCollator(RuleBasedCollator collator)
    {
        m_collator_ = collator;
        updateInternalState();
    }

    /**
     * <p>Sets the iterator to point to the collation element corresponding to
     * the specified character (the parameter is a CHARACTER offset in the
     * original string, not an offset into its corresponding sequence of
     * collation elements). The value returned by the next call to next()
     * will be the collation element corresponding to the specified position
     * in the text. Unlike the public method setOffset(int), this method does
     * not try to readjust the offset to the start of a contracting sequence.
     * getOffset() is guaranteed to return the same value as was passed to a
     * preceding call to setOffset().</p>
     * @param offset new character offset into the original text to set.
     */
    void setExactOffset(int offset)
    {
        m_source_.setIndex(offset);
        updateInternalState();
    }

    /**
     * Checks if iterator is in the buffer zone
     * @return true if iterator is in buffer zone, false otherwise
     */
    boolean isInBuffer()
    {
        return m_bufferOffset_ > 0;
    }

   
    /**
     * <p>Sets the iterator to point to the collation element corresponding to
     * the specified character (the parameter is a CHARACTER offset in the
     * original string, not an offset into its corresponding sequence of
     * collation elements). The value returned by the next call to next()
     * will be the collation element corresponding to the specified position
     * in the text. Unlike the public method setOffset(int), this method does
     * not try to readjust the offset to the start of a contracting sequence.
     * getOffset() is guaranteed to return the same value as was passed to a
     * preceding call to setOffset().</p>
     * </p>
     * @param source the new source string iterator for iteration.
     * @param offset to the source
     */
    void setText(UCharacterIterator source, int offset)
    {
        m_srcUtilIter_.setText(source.getText());
        m_source_ = m_srcUtilIter_;
        m_source_.setIndex(offset);
        updateInternalState();
    }

    // private inner class --------------------------------------------------

    /**
     * Backup data class
     */
    private static final class Backup
    {
        // protected data members -------------------------------------------

        /**
         * Backup non FCD sequence limit
         */
        protected int m_FCDLimit_;
        /**
         * Backup non FCD sequence start
         */
        protected int m_FCDStart_;
        /**
         * Backup if previous Codepoint is Hiragana quatenary
         */
        protected boolean m_isCodePointHiragana_;
        /**
         * Backup buffer position
         */
        protected int m_bufferOffset_;
        /**
         * Backup source iterator offset
         */
        protected int m_offset_;
        /**
         * Backup buffer contents
         */
        protected StringBuffer m_buffer_;

        // protected constructor --------------------------------------------

        /**
         * Empty constructor
         */
        protected Backup()
        {
            m_buffer_ = new StringBuffer();
        }
    }
    // end inner class ------------------------------------------------------

    /**
     * Direction of travel
     */
    private boolean m_isForwards_;
    /**
     * Source string iterator
     */
    private UCharacterIterator m_source_;
    /**
     * This is position to the m_buffer_, -1 if iterator is not in m_buffer_
     */
    private int m_bufferOffset_;
    /**
     * Buffer for temporary storage of normalized characters, discontiguous
     * characters and Thai characters
     */
    private StringBuilder m_buffer_;
    /**
     * Position in the original string to continue forward FCD check from.
     */
    private int m_FCDLimit_;
    /**
     * The collator this iterator is based on
     */
    private RuleBasedCollator m_collator_;
    /**
     * true if Hiragana quatenary is on
     */
    //private boolean m_isHiragana4_;
    /**
     * CE buffer
     */
    private int m_CEBuffer_[];
    /**
     * In reality we should not have to deal with expansion sequences longer
     * then 16. However this value can be change if a bigger buffer is needed.
     * Note, if the size is change to too small a number, BIG trouble.
     * Reasonable small value is around 10, if there's no Arabic or other
     * funky collations that have long expansion sequence. This is the longest
     * expansion sequence this can handle without bombing out.
     */
    private static final int CE_BUFFER_INIT_SIZE_ = 512;
    /**
     * Backup storage for special processing inner cases
     */
    private Backup m_utilSpecialBackUp_;
    /**
     * Backup storage in special processing entry state
     */
    private Backup m_utilSpecialEntryBackUp_;
    /**
     * Backup storage in special processing discontiguous state
     */
    private Backup m_utilSpecialDiscontiguousBackUp_;
    /**
     * Utility
     */
    private StringUCharacterIterator m_srcUtilIter_;
    private StringBuilder m_utilStringBuffer_;
    private StringBuilder m_utilSkippedBuffer_;
    private CollationElementIterator m_utilColEIter_;
    private static final Normalizer2Impl m_nfcImpl_ = Norm2AllModes.getNFCInstance().impl;
    private StringBuilder m_unnormalized_;
    private Normalizer2Impl.ReorderingBuffer m_n2Buffer_;
    /**
     * The first non-zero combining class character
     */
    private static final int FULL_ZERO_COMBINING_CLASS_FAST_LIMIT_ = 0xC0;
    /**
     * One character before the first character with leading non-zero combining
     * class
     */
    private static final int LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_ = 0x300;
    /**
     * Mask for the last byte
     */
    private static final int LAST_BYTE_MASK_ = 0xFF;
    /**
     * Shift value for the second last byte
     */
    private static final int SECOND_LAST_BYTE_SHIFT_ = 8;

    // special ce values and tags -------------------------------------------
    
//    private static final int CE_EXPANSION_ = 0xF1000000;
    private static final int CE_CONTRACTION_ = 0xF2000000;
    /**
     * Indicates the last ce has been consumed. Compare with NULLORDER.
     * NULLORDER is returned if error occurs.
     */
/*    private static final int CE_NO_MORE_CES_ = 0x00010101;
    private static final int CE_NO_MORE_CES_PRIMARY_ = 0x00010000;
    private static final int CE_NO_MORE_CES_SECONDARY_ = 0x00000100;
    private static final int CE_NO_MORE_CES_TERTIARY_ = 0x00000001;
*/
    private static final int CE_NOT_FOUND_TAG_ = 0;
    /**
     * Charset processing, not yet implemented
     */
    private static final int CE_CHARSET_TAG_ = 4;
    /**
     * AC00-D7AF
     */
    private static final int CE_HANGUL_SYLLABLE_TAG_ = 6;
    /**
     * D800-DBFF
     */
    private static final int CE_LEAD_SURROGATE_TAG_ = 7;
    /**
     * DC00-DFFF
     */
    private static final int CE_TRAIL_SURROGATE_TAG_ = 8;
    /**
     * 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D
     */
    private static final int CE_CJK_IMPLICIT_TAG_ = 9;
    private static final int CE_IMPLICIT_TAG_ = 10;
    static final int CE_SPEC_PROC_TAG_ = 11;
    /**
     * This is a 3 byte primary with starting secondaries and tertiaries.
     * It fits in a single 32 bit CE and is used instead of expansion to save
     * space without affecting the performance (hopefully).
     */
    private static final int CE_LONG_PRIMARY_TAG_ = 12;
                        
//    private static final int CE_CE_TAGS_COUNT = 14;
    private static final int CE_BYTE_COMMON_ = 0x05;

    // end special ce values and tags ---------------------------------------

    private static final int HANGUL_SBASE_ = 0xAC00;
    private static final int HANGUL_LBASE_ = 0x1100;
    private static final int HANGUL_VBASE_ = 0x1161;
    private static final int HANGUL_TBASE_ = 0x11A7;
    private static final int HANGUL_VCOUNT_ = 21;
    private static final int HANGUL_TCOUNT_ = 28;

    // CJK stuff ------------------------------------------------------------

/*    private static final int CJK_BASE_ = 0x4E00;
    private static final int CJK_LIMIT_ = 0x9FFF+1;
    private static final int CJK_COMPAT_USED_BASE_ = 0xFA0E;
    private static final int CJK_COMPAT_USED_LIMIT_ = 0xFA2F + 1;
    private static final int CJK_A_BASE_ = 0x3400;
    private static final int CJK_A_LIMIT_ = 0x4DBF + 1;
    private static final int CJK_B_BASE_ = 0x20000;
    private static final int CJK_B_LIMIT_ = 0x2A6DF + 1;
    private static final int NON_CJK_OFFSET_ = 0x110000;
*/
    private static final boolean DEBUG  =  ICUDebug.enabled("collator");
    
    // private methods ------------------------------------------------------

    /**
     * Reset the iterator internally
     */
    private void updateInternalState()
    {
        m_isCodePointHiragana_ = false;
        m_buffer_.setLength(0);
        m_bufferOffset_ = -1;
        m_CEBufferOffset_ = 0;
        m_CEBufferSize_ = 0;
        m_FCDLimit_ = -1;
        m_FCDStart_ = m_source_.getLength();
        //m_isHiragana4_ = m_collator_.m_isHiragana4_;
        m_isForwards_ = true;
    }

    /**
     * Backup the current internal state
     * @param backup object to store the data
     */
    private void backupInternalState(Backup backup)
    {
        backup.m_offset_ = m_source_.getIndex();
        backup.m_FCDLimit_ = m_FCDLimit_;
        backup.m_FCDStart_ = m_FCDStart_;
        backup.m_isCodePointHiragana_ = m_isCodePointHiragana_;
        backup.m_bufferOffset_ = m_bufferOffset_;
        backup.m_buffer_.setLength(0);
        if (m_bufferOffset_ >= 0) {
            backup.m_buffer_.append(m_buffer_);
        }
    }

    /**
     * Update the iterator internally with backed-up state
     * @param backup object that stored the data
     */
    private void updateInternalState(Backup backup)
    {
        m_source_.setIndex(backup.m_offset_);
        m_isCodePointHiragana_ = backup.m_isCodePointHiragana_;
        m_bufferOffset_ = backup.m_bufferOffset_;
        m_FCDLimit_ = backup.m_FCDLimit_;
        m_FCDStart_ = backup.m_FCDStart_;
        m_buffer_.setLength(0);
        if (m_bufferOffset_ >= 0) {
            m_buffer_.append(backup.m_buffer_);
        }
    }

    /**
     * A fast combining class retrieval system.
     * @param ch UTF16 character
     * @return combining class of ch
     */
    private int getCombiningClass(int ch)
    {
        if (ch >= LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_ &&
            m_collator_.isUnsafe((char)ch) || ch > 0xFFFF
        ) {
            return m_nfcImpl_.getCC(m_nfcImpl_.getNorm16(ch));
        }
        return 0;
    }

    /**
     * <p>Incremental normalization, this is an essential optimization.
     * Assuming FCD checks has been done, normalize the non-FCD characters into
     * the buffer.
     * Source offsets points to the current processing character.
     * </p>
     */
    private void normalize()
    {
        if (m_unnormalized_ == null) {
            m_unnormalized_ = new StringBuilder();
            m_n2Buffer_ = new Normalizer2Impl.ReorderingBuffer(m_nfcImpl_, m_buffer_, 10);
        } else {
            m_unnormalized_.setLength(0);
            m_n2Buffer_.remove();
        }
        int size = m_FCDLimit_ - m_FCDStart_;
        m_source_.setIndex(m_FCDStart_);
        for (int i = 0; i < size; i ++) {
            m_unnormalized_.append((char)m_source_.next());
        }
        m_nfcImpl_.decomposeShort(m_unnormalized_, 0, size, m_n2Buffer_);
    }

    /**
     * <p>Incremental FCD check and normalization. Gets the next base character
     * position and determines if the in-between characters needs normalization.
     * </p>
     * <p>When entering, the state is known to be this:
     * <ul>
     * <li>We are working on source string, not the buffer.
     * <li>The leading combining class from the current character is 0 or the
     *     trailing combining class of the previous char was zero.
     * </ul>
     * Incoming source offsets points to the current processing character.
     * Return source offsets points to the current processing character.
     * </p>
     * @param ch current character (lead unit)
     * @param offset offset of ch +1
     * @return true if FCDCheck passes, false otherwise
     */
    private boolean FCDCheck(int ch, int offset)
    {
        boolean result = true;

        // Get the trailing combining class of the current character.
        // If it's zero, we are OK.
        m_FCDStart_ = offset - 1;
        m_source_.setIndex(offset);
        // trie access
        int fcd = m_nfcImpl_.getFCD16FromSingleLead((char)ch);
        if (fcd != 0 && Character.isHighSurrogate((char)ch)) {
            int c2 = m_source_.next(); 
            if (c2 < 0) {
                fcd = 0;  // end of input
            } else if (Character.isLowSurrogate((char)c2)) {
                fcd = m_nfcImpl_.getFCD16(Character.toCodePoint((char)ch, (char)c2));
            } else {
                m_source_.moveIndex(-1);
                fcd = 0;
            }
        }

        int prevTrailCC = fcd & LAST_BYTE_MASK_;

        if (prevTrailCC == 0) {
            offset = m_source_.getIndex();
        } else {
            // The current char has a non-zero trailing CC. Scan forward until
            // we find a char with a leading cc of zero.
            while (true) {
                ch = m_source_.nextCodePoint();
                if (ch < 0) {
                    offset = m_source_.getIndex();
                    break;
                }
                // trie access
                fcd = m_nfcImpl_.getFCD16(ch);
                int leadCC = fcd >> SECOND_LAST_BYTE_SHIFT_;
                if (leadCC == 0) {
                    // this is a base character, we stop the FCD checks
                    offset = m_source_.getIndex() - Character.charCount(ch);
                    break;
                }

                if (leadCC < prevTrailCC) {
                    result = false;
                }

                prevTrailCC = fcd & LAST_BYTE_MASK_;
            }
        }
        m_FCDLimit_ = offset;
        m_source_.setIndex(m_FCDStart_ + 1);
        return result;
    }

    /**
     * <p>Method tries to fetch the next character that is in fcd form.</p>
     * <p>Normalization is done if required.</p>
     * <p>Offsets are returned at the next character.</p>
     * @return next fcd character
     */
    private int nextChar()
    {
        int result;

        // loop handles the next character whether it is in the buffer or not.
        if (m_bufferOffset_ < 0) {
            // we're working on the source and not normalizing. fast path.
            // note Thai pre-vowel reordering uses buffer too
            result = m_source_.next();
        }
        else {
            // we are in the buffer, buffer offset will never be 0 here
            if (m_bufferOffset_ >= m_buffer_.length()) {
                // Null marked end of buffer, revert to the source string and
                // loop back to top to try again to get a character.
                m_source_.setIndex(m_FCDLimit_);
                m_bufferOffset_ = -1;
                m_buffer_.setLength(0);
                return nextChar();
            }
            return m_buffer_.charAt(m_bufferOffset_ ++);
        }
        int startoffset = m_source_.getIndex();
        if (result < FULL_ZERO_COMBINING_CLASS_FAST_LIMIT_
            // Fast fcd safe path. trail combining class == 0.
            || m_collator_.getDecomposition() == Collator.NO_DECOMPOSITION
            || m_bufferOffset_ >= 0 || m_FCDLimit_ >= startoffset) {
            // skip the fcd checks
            return result;
        }

        if (result < LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_) {
            // We need to peek at the next character in order to tell if we are
            // FCD
            int next = m_source_.current();
            if (next == UCharacterIterator.DONE
                || next < LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_) {
                return result; // end of source string and if next character
                // starts with a base character is always fcd.
            }
        }

        // Need a more complete FCD check and possible normalization.
        if (!FCDCheck(result, startoffset)) {
            normalize();
            result = m_buffer_.charAt(0);
            m_bufferOffset_ = 1;
        }
        return result;
    }

    /**
     * <p>Incremental normalization, this is an essential optimization.
     * Assuming FCD checks has been done, normalize the non-FCD characters into
     * the buffer.
     * Source offsets points to the current processing character.</p>
     */
    private void normalizeBackwards()
    {
        normalize();
        m_bufferOffset_ = m_buffer_.length();
    }

    /**
     * <p>Incremental backwards FCD check and normalization. Gets the previous
     * base character position and determines if the in-between characters
     * needs normalization.
     * </p>
     * <p>When entering, the state is known to be this:
     * <ul>
     * <li>We are working on source string, not the buffer.
     * <li>The trailing combining class from the current character is 0 or the
     *     leading combining class of the next char was zero.
     * </ul>
     * Input source offsets points to the previous character.
     * Return source offsets points to the current processing character.
     * </p>
     * @param ch current character
     * @param offset current character offset
     * @return true if FCDCheck passes, false otherwise
     */
    private boolean FCDCheckBackwards(int ch, int offset)
    {
        int fcd;
        m_FCDLimit_ = offset + 1;
        m_source_.setIndex(offset);
        if (!UTF16.isSurrogate((char)ch)) {
            fcd = m_nfcImpl_.getFCD16FromSingleLead((char)ch);
        } else {
            fcd = 0;
            if (!Normalizer2Impl.UTF16Plus.isSurrogateLead(ch)) {
                int c2 = m_source_.previous();
                if (c2 < 0) {
                    // start of input
                } else if (Character.isHighSurrogate((char)c2)) {
                    ch = Character.toCodePoint((char)c2, (char)ch);
                    fcd = m_nfcImpl_.getFCD16(ch);
                    --offset;
                } else {
                    m_source_.moveIndex(1);
                }
            }
        }

        // Scan backward until we find a char with a leading cc of zero.
        boolean result = true;
        if (fcd != 0) {
            int leadCC;
            for (;;) {
                leadCC = fcd >> SECOND_LAST_BYTE_SHIFT_;
                if (leadCC == 0 || (ch = m_source_.previousCodePoint()) < 0) {
                    offset = m_source_.getIndex();
                    break;
                }
                fcd = m_nfcImpl_.getFCD16(ch);
                int prevTrailCC = fcd & LAST_BYTE_MASK_;
                if (leadCC < prevTrailCC) {
                    result = false;
                } else if (fcd == 0) {
                    offset = m_source_.getIndex() + Character.charCount(ch);
                    break;
                }
            }
        }

        // storing character with 0 lead fcd or the 1st accent with a base
        // character before it
        m_FCDStart_ = offset;
        m_source_.setIndex(m_FCDLimit_);
        return result;
    }

    /**
     * <p>Method tries to fetch the previous character that is in fcd form.</p>
     * <p>Normalization is done if required.</p>
     * <p>Offsets are returned at the current character.</p>
     * @return previous fcd character
     */
    private int previousChar()
    {
        if (m_bufferOffset_ >= 0) {
            m_bufferOffset_ --;
            if (m_bufferOffset_ >= 0) {
                return m_buffer_.charAt(m_bufferOffset_);
            }
            else {
                // At the start of buffer, route back to string.
                m_buffer_.setLength(0);
                if (m_FCDStart_ == 0) {
                    m_FCDStart_ = -1;
                    m_source_.setIndex(0);
                    return UCharacterIterator.DONE;
                }
                else {
                    m_FCDLimit_ = m_FCDStart_;
                    m_source_.setIndex(m_FCDStart_);
                    return previousChar();
                }
            }
        }
        int result = m_source_.previous();
        int startoffset = m_source_.getIndex();
        if (result < LEAD_ZERO_COMBINING_CLASS_FAST_LIMIT_
            || m_collator_.getDecomposition() == Collator.NO_DECOMPOSITION
            || m_FCDStart_ <= startoffset || m_source_.getIndex() == 0) {
            return result;
        }
        int ch = m_source_.previous();
        if (ch < FULL_ZERO_COMBINING_CLASS_FAST_LIMIT_) {
            // if previous character is FCD
            m_source_.next();
            return result;
        }
        // Need a more complete FCD check and possible normalization.
        if (!FCDCheckBackwards(result, startoffset)) {
            normalizeBackwards();
            m_bufferOffset_ --;
            result = m_buffer_.charAt(m_bufferOffset_);
        }
        else {
            // fcd checks always reset m_source_ to the limit of the FCD
            m_source_.setIndex(startoffset);
        }
        return result;
    }

    /**
     * Determines if it is at the start of source iteration
     * @return true if iterator at the start, false otherwise
     */
    private final boolean isBackwardsStart()
    {
        return (m_bufferOffset_ < 0 && m_source_.getIndex() == 0)
            || (m_bufferOffset_ == 0 && m_FCDStart_ <= 0);
    }

    /**
     * Checks if iterator is at the end of its source string.
     * @return true if it is at the end, false otherwise
     */
    private final boolean isEnd()
    {
        if (m_bufferOffset_ >= 0) {
            if (m_bufferOffset_ != m_buffer_.length()) {
                return false;
            }
            else {
                // at end of buffer. check if fcd is at the end
                return m_FCDLimit_ == m_source_.getLength();
            }
        }
        return m_source_.getLength() == m_source_.getIndex();
    }

    /**
     * <p>Special CE management for surrogates</p>
     * <p>Lead surrogate is encountered. CE to be retrieved by using the
     * following code unit. If next character is a trail surrogate, both
     * characters will be combined to retrieve the CE, otherwise completely
     * ignorable (UCA specification) is returned.</p>
     * @param collator collator to use
     * @param ce current CE
     * @param trail character
     * @return next CE for the surrogate characters
     */
    private final int nextSurrogate(RuleBasedCollator collator, int ce,
                                    char trail)
    {
        if (!UTF16.isTrailSurrogate(trail)) {
            updateInternalState(m_utilSpecialBackUp_);
            return IGNORABLE;
        }
        // TODO: CE contain the data from the previous CE + the mask.
        // It should at least be unmasked
        int result = collator.m_trie_.getTrailValue(ce, trail);
        if (result == CE_NOT_FOUND_) {
            updateInternalState(m_utilSpecialBackUp_);
        }
        return result;
    }

    /**
     * Gets the CE expansion offset
     * @param collator current collator
     * @param ce ce to test
     * @return expansion offset
     */
    private int getExpansionOffset(RuleBasedCollator collator, int ce)
    {
        return ((ce & 0xFFFFF0) >> 4) - collator.m_expansionOffset_;
    }


    /**
     * Gets the contraction ce offset
     * @param collator current collator
     * @param ce current ce
     * @return contraction offset
     */
    private int getContractionOffset(RuleBasedCollator collator, int ce)
    {
        return (ce & 0xFFFFFF) - collator.m_contractionOffset_;
    }

    /**
     * Checks if CE is a special tag CE
     * @param ce to check
     * @return true if CE is a special tag CE, false otherwise
     */
    private boolean isSpecialPrefixTag(int ce)
    {
        return RuleBasedCollator.isSpecial(ce) &&
            RuleBasedCollator.getTag(ce) == CE_SPEC_PROC_TAG_;
    }

    /**
     * <p>Special processing getting a CE that is preceded by a certain
     * prefix.</p>
     * <p>Used for optimizing Japanese length and iteration marks. When a
     * special processing tag is encountered, iterate backwards to see if
     * there's a match.</p>
     * <p>Contraction tables are used, prefix data is stored backwards in the
     * table.</p>
     * @param collator collator to use
     * @param ce current ce
     * @param entrybackup entry backup iterator status
     * @return next collation element
     */
    private int nextSpecialPrefix(RuleBasedCollator collator, int ce,
                                  Backup entrybackup)
    {
        backupInternalState(m_utilSpecialBackUp_);
        updateInternalState(entrybackup);
        previousChar();
        // We want to look at the character where we entered

        while (true) {
            // This loop will run once per source string character, for as
            // long as we are matching a potential contraction sequence
            // First we position ourselves at the begining of contraction
            // sequence
            int entryoffset = getContractionOffset(collator, ce);
            int offset = entryoffset;
            if (isBackwardsStart()) {
                ce = collator.m_contractionCE_[offset];
                break;
            }
            char previous = (char)previousChar();
            while (previous > collator.m_contractionIndex_[offset]) {
                // contraction characters are ordered, skip smaller characters
                offset ++;
            }

            if (previous == collator.m_contractionIndex_[offset]) {
                // Found the source string char in the table.
                // Pick up the corresponding CE from the table.
                ce = collator.m_contractionCE_[offset];
            }
            else {
                // Source string char was not in the table, prefix not found
                ce = collator.m_contractionCE_[entryoffset];
            }

            if (!isSpecialPrefixTag(ce)) {
                // The source string char was in the contraction table, and
                // the corresponding CE is not a prefix CE. We found the
                // prefix, break out of loop, this CE will end up being
                // returned. This is the normal way out of prefix handling
                // when the source actually contained the prefix.
                break;
            }
        }
        if (ce != CE_NOT_FOUND_) {
            // we found something and we can merilly continue
            updateInternalState(m_utilSpecialBackUp_);
        }
        else { // prefix search was a failure, we have to backup all the way to
            // the start
            updateInternalState(entrybackup);
        }
        return ce;
    }

    /**
     * Checks if the ce is a contraction tag
     * @param ce ce to check
     * @return true if ce is a contraction tag, false otherwise
     */
    private boolean isContractionTag(int ce)
    {
        return RuleBasedCollator.isSpecial(ce) &&
            RuleBasedCollator.getTag(ce) == CE_CONTRACTION_TAG_;
    }

    /**
     * Method to copy skipped characters into the buffer and sets the fcd
     * position. To ensure that the skipped characters are considered later,
     * we need to place it in the appropriate position in the buffer and
     * reassign the source index. simple case if index reside in string,
     * simply copy to buffer and fcdposition = pos, pos = start of buffer.
     * if pos in normalization buffer, we'll insert the copy infront of pos
     * and point pos to the start of the buffer. why am i doing these copies?
     * well, so that the whole chunk of codes in the getNextCE,
     * ucol_prv_getSpecialCE does not require any changes, which will be
     * really painful.
     * @param skipped character buffer
     */
    private void setDiscontiguous(StringBuilder skipped)
    {
        if (m_bufferOffset_ >= 0) {
            m_buffer_.replace(0, m_bufferOffset_, skipped.toString());
        }
        else {
            m_FCDLimit_ = m_source_.getIndex();
            m_buffer_.setLength(0);
            m_buffer_.append(skipped.toString());
        }

        m_bufferOffset_ = 0;
    }

    /**
     * Returns the current character for forward iteration
     * @return current character
     */
    private int currentChar()
    {
        if (m_bufferOffset_ < 0) {
            m_source_.previous();
            return m_source_.next();
        }

        // m_bufferOffset_ is never 0 in normal circumstances except after a
        // discontiguous contraction since it is always returned and moved
        // by 1 when we do nextChar()
        return m_buffer_.charAt(m_bufferOffset_ - 1);
    }

    /**
     * Method to get the discontiguous collation element within the source.
     * Note this function will set the position to the appropriate places.
     * Passed in character offset points to the second combining character
     * after the start character.
     * @param collator current collator used
     * @param entryoffset index to the start character in the contraction table
     * @return discontiguous collation element offset
     */
    private int nextDiscontiguous(RuleBasedCollator collator, int entryoffset)
    {
        int offset = entryoffset;
        boolean multicontraction = false;
        // since it will be stuffed into this iterator and ran over again
        if (m_utilSkippedBuffer_ == null) {
            m_utilSkippedBuffer_ = new StringBuilder();
        }
        else {
            m_utilSkippedBuffer_.setLength(0);
        }
        char ch = (char)currentChar();
        m_utilSkippedBuffer_.append((char)currentChar());
        // accent after the first character
        if (m_utilSpecialDiscontiguousBackUp_ == null) {
            m_utilSpecialDiscontiguousBackUp_ = new Backup();
        }
        backupInternalState(m_utilSpecialDiscontiguousBackUp_);
        char nextch = ch;
        while (true) {
            ch = nextch;
            int ch_int = nextChar();
            nextch = (char)ch_int;
            if (ch_int == UCharacterIterator.DONE
                || getCombiningClass(nextch) == 0) {
                // if there are no more accents to move around
                // we don't have to shift previousChar, since we are resetting
                // the offset later
                if (multicontraction) {
                    if (ch_int != UCharacterIterator.DONE) {
                        previousChar(); // backtrack
                    }
                    setDiscontiguous(m_utilSkippedBuffer_);
                    return collator.m_contractionCE_[offset];
                }
                break;
            }

            offset ++; // skip the combining class offset
            while ((offset < collator.m_contractionIndex_.length) &&
                   (nextch > collator.m_contractionIndex_[offset])) {
                offset ++;
            }

            int ce = CE_NOT_FOUND_;
            if ( offset >= collator.m_contractionIndex_.length)  {
                break;
            }
            if ( nextch != collator.m_contractionIndex_[offset]
                 || getCombiningClass(nextch) == getCombiningClass(ch)) {
                    // unmatched or blocked character
                if ( (m_utilSkippedBuffer_.length()!= 1) ||
                     ((m_utilSkippedBuffer_.charAt(0)!= nextch) &&
                      (m_bufferOffset_<0) )) { // avoid push to skipped buffer twice
                    m_utilSkippedBuffer_.append(nextch);
                }
                offset = entryoffset;  // Restore the offset before checking next character.
                continue;
            }
            else {
                ce = collator.m_contractionCE_[offset];
            }

            if (ce == CE_NOT_FOUND_) {
                break;
            }
            else if (isContractionTag(ce)) {
                // this is a multi-contraction
                offset = getContractionOffset(collator, ce);
                if (collator.m_contractionCE_[offset] != CE_NOT_FOUND_) {
                    multicontraction = true;
                    backupInternalState(m_utilSpecialDiscontiguousBackUp_);
                }
            }
            else {
                setDiscontiguous(m_utilSkippedBuffer_);
                return ce;
            }
        }

        updateInternalState(m_utilSpecialDiscontiguousBackUp_);
        // backup is one forward of the base character, we need to move back
        // one more
        previousChar();
        return collator.m_contractionCE_[entryoffset];
    }

    /**
     * Gets the next contraction ce
     * @param collator collator to use
     * @param ce current ce
     * @return ce of the next contraction
     */
    private int nextContraction(RuleBasedCollator collator, int ce)
    {
        backupInternalState(m_utilSpecialBackUp_);
        int entryce = collator.m_contractionCE_[getContractionOffset(collator, ce)]; //CE_NOT_FOUND_;
        while (true) {
            int entryoffset = getContractionOffset(collator, ce);
            int offset = entryoffset;

            if (isEnd()) {
                ce = collator.m_contractionCE_[offset];
                if (ce == CE_NOT_FOUND_) {
                    // back up the source over all the chars we scanned going
                    // into this contraction.
                    ce = entryce;
                    updateInternalState(m_utilSpecialBackUp_);
                }
                break;
            }

            // get the discontiguos maximum combining class
            int maxCC = (collator.m_contractionIndex_[offset] & 0xFF);
            // checks if all characters have the same combining class
            byte allSame = (byte)(collator.m_contractionIndex_[offset] >> 8);
            char ch = (char)nextChar();
            offset ++;
            while (ch > collator.m_contractionIndex_[offset]) {
                // contraction characters are ordered, skip all smaller
                offset ++;
            }

            if (ch == collator.m_contractionIndex_[offset]) {
                // Found the source string char in the contraction table.
                //  Pick up the corresponding CE from the table.
                ce = collator.m_contractionCE_[offset];
            }
            else {
                // Source string char was not in contraction table.
                // Unless it is a discontiguous contraction, we are done
                int miss = ch;
                if(UTF16.isLeadSurrogate(ch)) { // in order to do the proper detection, we
                    // need to see if we're dealing with a supplementary
                    miss = UCharacterProperty.getRawSupplementary(ch, (char) nextChar());
                  }
                int sCC;
                if (maxCC == 0 || (sCC = getCombiningClass(miss)) == 0
                    || sCC > maxCC || (allSame != 0 && sCC == maxCC) ||
                    isEnd()) {
                    // Contraction can not be discontiguous, back up by one
                    previousChar();
                    if(miss > 0xFFFF) {
                        previousChar();
                    }
                    ce = collator.m_contractionCE_[entryoffset];
                }
                else {
                    // Contraction is possibly discontiguous.
                    // find the next character if ch is not a base character
                    int ch_int = nextChar();
                    if (ch_int != UCharacterIterator.DONE) {
                        previousChar();
                    }
                    char nextch = (char)ch_int;
                    if (getCombiningClass(nextch) == 0) {
                        previousChar();
                        if(miss > 0xFFFF) {
                            previousChar();
                        }    
                        // base character not part of discontiguous contraction
                        ce = collator.m_contractionCE_[entryoffset];
                    }
                    else {
                        ce = nextDiscontiguous(collator, entryoffset);
                    }
                }
            }

            if (ce == CE_NOT_FOUND_) {
                // source did not match the contraction, revert back original
                updateInternalState(m_utilSpecialBackUp_);
                ce = entryce;
                break;
            }

            // source was a contraction
            if (!isContractionTag(ce)) {
                break;
            }

            // ccontinue looping to check for the remaining contraction.
            if (collator.m_contractionCE_[entryoffset] != CE_NOT_FOUND_) {
                // there are further contractions to be performed, so we store
                // the so-far completed ce, so that if we fail in the next
                // round we just return this one.
                entryce = collator.m_contractionCE_[entryoffset];
                backupInternalState(m_utilSpecialBackUp_);
                if (m_utilSpecialBackUp_.m_bufferOffset_ >= 0) {
                    m_utilSpecialBackUp_.m_bufferOffset_ --;
                }
                else {
                    m_utilSpecialBackUp_.m_offset_ --;
                }
            }
        }
        return ce;
    }

    /**
     * Gets the next ce for long primaries, stuffs the rest of the collation
     * elements into the ce buffer
     * @param ce current ce
     * @return next ce
     */
    private int nextLongPrimary(int ce)
    {
        m_CEBuffer_[1] = ((ce & 0xFF) << 24)
            | RuleBasedCollator.CE_CONTINUATION_MARKER_;
        m_CEBufferOffset_ = 1;
        m_CEBufferSize_ = 2;
        m_CEBuffer_[0] = ((ce & 0xFFFF00) << 8) | (CE_BYTE_COMMON_ << 8) |
            CE_BYTE_COMMON_;
        return m_CEBuffer_[0];
    }

    /**
     * Gets the number of expansion
     * @param ce current ce
     * @return number of expansion
     */
    private int getExpansionCount(int ce)
    {
        return ce & 0xF;
    }

    /**
     * Gets the next expansion ce and stuffs the rest of the collation elements
     * into the ce buffer
     * @param collator current collator
     * @param ce current ce
     * @return next expansion ce
     */
    private int nextExpansion(RuleBasedCollator collator, int ce)
    {
        // NOTE: we can encounter both continuations and expansions in an
        // expansion!
        // I have to decide where continuations are going to be dealt with
        int offset = getExpansionOffset(collator, ce);
        m_CEBufferSize_ = getExpansionCount(ce);
        m_CEBufferOffset_ = 1;
        m_CEBuffer_[0] = collator.m_expansion_[offset];
        if (m_CEBufferSize_ != 0) {
            // if there are less than 16 elements in expansion
            for (int i = 1; i < m_CEBufferSize_; i ++) {
                m_CEBuffer_[i] = collator.m_expansion_[offset + i];
            }
        }
        else {
            // ce are terminated
            m_CEBufferSize_ = 1;
            while (collator.m_expansion_[offset] != 0) {
                m_CEBuffer_[m_CEBufferSize_ ++] =
                    collator.m_expansion_[++ offset];
            }
        }
        // in case of one element expansion, we 
        // want to immediately return CEpos
        if (m_CEBufferSize_ == 1) {
            m_CEBufferSize_ = 0;
            m_CEBufferOffset_ = 0;
        }
        return m_CEBuffer_[0];
    }
    
    /**
     * Gets the next digit ce
     * @param collator current collator
     * @param ce current collation element
     * @param cp current codepoint
     * @return next digit ce
     */
    private int nextDigit(RuleBasedCollator collator, int ce, int cp)
    {
        // We do a check to see if we want to collate digits as numbers; 
        // if so we generate a custom collation key. Otherwise we pull out 
        // the value stored in the expansion table.

        if (m_collator_.m_isNumericCollation_){
            int collateVal = 0;
            int trailingZeroIndex = 0;
            boolean nonZeroValReached = false;

            // I just need a temporary place to store my generated CEs.
            // icu4c uses a unsigned byte array, i'll use a stringbuffer here
            // to avoid dealing with the sign problems and array allocation
            // clear and set initial string buffer length
            m_utilStringBuffer_.setLength(3);
        
            // We parse the source string until we hit a char that's NOT a 
            // digit.
            // Use this u_charDigitValue. This might be slow because we have 
            // to handle surrogates...
            int digVal = UCharacter.digit(cp); 
            // if we have arrived here, we have already processed possible 
            // supplementaries that trigered the digit tag -
            // all supplementaries are marked in the UCA.
            // We  pad a zero in front of the first element anyways. 
            // This takes care of the (probably) most common case where 
            // people are sorting things followed by a single digit
            int digIndx = 1;
            for (;;) {
                // Make sure we have enough space.
                if (digIndx >= ((m_utilStringBuffer_.length() - 2) << 1)) {
                    m_utilStringBuffer_.setLength(m_utilStringBuffer_.length() 
                                                  << 1);
                }
                // Skipping over leading zeroes.        
                if (digVal != 0 || nonZeroValReached) {
                    if (digVal != 0 && !nonZeroValReached) {
                        nonZeroValReached = true;
                    }    
                    // We parse the digit string into base 100 numbers 
                    // (this fits into a byte).
                    // We only add to the buffer in twos, thus if we are 
                    // parsing an odd character, that serves as the 
                    // 'tens' digit while the if we are parsing an even 
                    // one, that is the 'ones' digit. We dumped the 
                    // parsed base 100 value (collateVal) into a buffer. 
                    // We multiply each collateVal by 2 (to give us room) 
                    // and add 5 (to avoid overlapping magic CE byte 
                    // values). The last byte we subtract 1 to ensure it is 
                    // less than all the other bytes.
                    if (digIndx % 2 == 1) {
                        collateVal += digVal;  
                        // This removes trailing zeroes.
                        if (collateVal == 0 && trailingZeroIndex == 0) {
                            trailingZeroIndex = ((digIndx - 1) >>> 1) + 2;
                        }
                        else if (trailingZeroIndex != 0) {
                            trailingZeroIndex = 0;
                        }
                        m_utilStringBuffer_.setCharAt(
                                            ((digIndx - 1) >>> 1) + 2,
                                            (char)((collateVal << 1) + 6));
                        collateVal = 0;
                    }
                    else {
                        // We drop the collation value into the buffer so if 
                        // we need to do a "front patch" we don't have to 
                        // check to see if we're hitting the last element.
                        collateVal = digVal * 10;
                        m_utilStringBuffer_.setCharAt((digIndx >>> 1) + 2, 
                                                (char)((collateVal << 1) + 6));
                    }
                    digIndx ++;
                }
            
                // Get next character.
                if (!isEnd()){
                    backupInternalState(m_utilSpecialBackUp_);
                    int char32 = nextChar();
                    char ch = (char)char32;
                    if (UTF16.isLeadSurrogate(ch)){
                        if (!isEnd()) {
                            char trail = (char)nextChar();
                            if (UTF16.isTrailSurrogate(trail)) {
                               char32 = UCharacterProperty.getRawSupplementary(
                                                                   ch, trail);
                            } 
                            else {
                                goBackOne();
                            }
                        }
                    }
                    
                    digVal = UCharacter.digit(char32);
                    if (digVal == -1) {
                        // Resetting position to point to the next unprocessed 
                        // char. We overshot it when doing our test/set for 
                        // numbers.
                        updateInternalState(m_utilSpecialBackUp_);
                        break;
                    }
                } 
                else {
                    break;
                }
            }
        
            if (nonZeroValReached == false){
                digIndx = 2;
                m_utilStringBuffer_.setCharAt(2, (char)6);
            }
        
            int endIndex = trailingZeroIndex != 0 ? trailingZeroIndex 
                                             : (digIndx >>> 1) + 2;              
            if (digIndx % 2 != 0){
                // We missed a value. Since digIndx isn't even, stuck too many 
                // values into the buffer (this is what we get for padding the 
                // first byte with a zero). "Front-patch" now by pushing all 
                // nybbles forward.
                // Doing it this way ensures that at least 50% of the time 
                // (statistically speaking) we'll only be doing a single pass 
                // and optimizes for strings with single digits. I'm just 
                // assuming that's the more common case.
                for (int i = 2; i < endIndex; i ++){
                    m_utilStringBuffer_.setCharAt(i, 
                        (char)((((((m_utilStringBuffer_.charAt(i) - 6) >>> 1) 
                                  % 10) * 10) 
                                 + (((m_utilStringBuffer_.charAt(i + 1) - 6) 
                                      >>> 1) / 10) << 1) + 6));
                }
                -- digIndx;
            }
        
            // Subtract one off of the last byte. 
            m_utilStringBuffer_.setCharAt(endIndex - 1, 
                         (char)(m_utilStringBuffer_.charAt(endIndex - 1) - 1));            
                
            // We want to skip over the first two slots in the buffer. 
            // The first slot is reserved for the header byte CODAN_PLACEHOLDER. 
            // The second slot is for the sign/exponent byte: 
            // 0x80 + (decimalPos/2) & 7f.
            m_utilStringBuffer_.setCharAt(0, (char)RuleBasedCollator.CODAN_PLACEHOLDER);
            m_utilStringBuffer_.setCharAt(1, 
                                     (char)(0x80 + ((digIndx >>> 1) & 0x7F)));
        
            // Now transfer the collation key to our collIterate struct.
            // The total size for our collation key is endIndx bumped up to the next largest even value divided by two.
            ce = (((m_utilStringBuffer_.charAt(0) << 8)
                       // Primary weight 
                       | m_utilStringBuffer_.charAt(1)) 
                                    << RuleBasedCollator.CE_PRIMARY_SHIFT_)
                       //  Secondary weight 
                       | (RuleBasedCollator.BYTE_COMMON_ 
                          << RuleBasedCollator.CE_SECONDARY_SHIFT_) 
                       | RuleBasedCollator.BYTE_COMMON_; // Tertiary weight.
            int i = 2; // Reset the index into the buffer.
            
            m_CEBuffer_[0] = ce;
            m_CEBufferSize_ = 1;
            m_CEBufferOffset_ = 1;
            while (i < endIndex)
            {
                int primWeight = m_utilStringBuffer_.charAt(i ++) << 8;
                if (i < endIndex) {
                    primWeight |= m_utilStringBuffer_.charAt(i ++);
                }
                m_CEBuffer_[m_CEBufferSize_ ++] 
                    = (primWeight << RuleBasedCollator.CE_PRIMARY_SHIFT_) 
                      | RuleBasedCollator.CE_CONTINUATION_MARKER_;
            }
            return ce;
        } 
        
        // no numeric mode, we'll just switch to whatever we stashed and 
        // continue
        // find the offset to expansion table
        return collator.m_expansion_[getExpansionOffset(collator, ce)];
    }

    /**
     * Gets the next implicit ce for codepoints
     * @param codepoint current codepoint
     * @return implicit ce
     */
    private int nextImplicit(int codepoint)
    {
        if (!UCharacter.isLegal(codepoint)) {
            // synwee to check with vladimir on the range of isNonChar()
            // illegal code value, use completely ignoreable!
            return IGNORABLE;
        }
        int result = RuleBasedCollator.impCEGen_.getImplicitFromCodePoint(codepoint);
        m_CEBuffer_[0] = (result & RuleBasedCollator.CE_PRIMARY_MASK_)
                         | 0x00000505;
        m_CEBuffer_[1] = ((result & 0x0000FFFF) << 16) | 0x000000C0;
        m_CEBufferOffset_ = 1;
        m_CEBufferSize_ = 2;
        return m_CEBuffer_[0];
    }

    /**
     * Returns the next ce associated with the following surrogate characters
     * @param ch current character
     * @return ce
     */
    private int nextSurrogate(char ch)
    {
        int ch_int = nextChar();
        char nextch = (char)ch_int;
        if (ch_int != CharacterIterator.DONE &&
            UTF16.isTrailSurrogate(nextch)) {
            int codepoint = UCharacterProperty.getRawSupplementary(ch, nextch);
            return nextImplicit(codepoint);
        }
        if (nextch != CharacterIterator.DONE) {
            previousChar(); // reverts back to the original position
        }
        return IGNORABLE; // completely ignorable
    }

    /**
     * Returns the next ce for a hangul character, this is an implicit
     * calculation
     * @param collator current collator
     * @param ch current character
     * @return hangul ce
     */
    private int nextHangul(RuleBasedCollator collator, char ch)
    {
        char L = (char)(ch - HANGUL_SBASE_);

        // divide into pieces
        // do it in this order since some compilers can do % and / in one
        // operation
        char T = (char)(L % HANGUL_TCOUNT_);
        L /= HANGUL_TCOUNT_;
        char V = (char)(L % HANGUL_VCOUNT_);
        L /= HANGUL_VCOUNT_;

        // offset them
        L += HANGUL_LBASE_;
        V += HANGUL_VBASE_;
        T += HANGUL_TBASE_;

        // return the first CE, but first put the rest into the expansion
        // buffer
        m_CEBufferSize_ = 0;
        if (!collator.m_isJamoSpecial_) { // FAST PATH
            m_CEBuffer_[m_CEBufferSize_ ++] =
                collator.m_trie_.getLeadValue(L);
            m_CEBuffer_[m_CEBufferSize_ ++] =
                collator.m_trie_.getLeadValue(V);

            if (T != HANGUL_TBASE_) {
                m_CEBuffer_[m_CEBufferSize_ ++] =
                    collator.m_trie_.getLeadValue(T);
            }
            m_CEBufferOffset_ = 1;
            return m_CEBuffer_[0];
        }
        else {
            // Jamo is Special
            // Since Hanguls pass the FCD check, it is guaranteed that we
            // won't be in the normalization buffer if something like this
            // happens
            // Move Jamos into normalization buffer
            m_buffer_.append(L);
            m_buffer_.append(V);
            if (T != HANGUL_TBASE_) {
                m_buffer_.append(T);
            }
            m_FCDLimit_ = m_source_.getIndex();
            m_FCDStart_ = m_FCDLimit_ - 1;
            // Indicate where to continue in main input string after
            // exhausting the buffer
            return IGNORABLE;
        }
    }

    /**
     * <p>Special CE management. Expansions, contractions etc...</p>
     * @param collator can be plain UCA
     * @param ce current ce
     * @param ch current character
     * @return next special ce
     */
    private int nextSpecial(RuleBasedCollator collator, int ce, char ch)
    {
        int codepoint = ch;
        Backup entrybackup = m_utilSpecialEntryBackUp_;
        // this is to handle recursive looping
        if (entrybackup != null) {
            m_utilSpecialEntryBackUp_ = null;
        }
        else {
            entrybackup = new Backup();
        }
        backupInternalState(entrybackup);
        try { // forces it to assign m_utilSpecialEntryBackup_
            while (true) {
                // This loop will repeat only in the case of contractions,
                // surrogate
                switch(RuleBasedCollator.getTag(ce)) {
                case CE_NOT_FOUND_TAG_:
                    // impossible case for icu4j
                    return ce;
                case RuleBasedCollator.CE_SURROGATE_TAG_:
                    if (isEnd()) {
                        return IGNORABLE;
                    }
                    backupInternalState(m_utilSpecialBackUp_);
                    char trail = (char)nextChar();
                    ce = nextSurrogate(collator, ce, trail);
                    // calculate the supplementary code point value,
                    // if surrogate was not tailored we go one more round
                    codepoint =
                        UCharacterProperty.getRawSupplementary(ch, trail);
                    break;
                case CE_SPEC_PROC_TAG_:
                    ce = nextSpecialPrefix(collator, ce, entrybackup);
                    break;
                case CE_CONTRACTION_TAG_:
                    ce = nextContraction(collator, ce);
                    break;
                case CE_LONG_PRIMARY_TAG_:
                    return nextLongPrimary(ce);
                case CE_EXPANSION_TAG_:
                    return nextExpansion(collator, ce);
                case CE_DIGIT_TAG_:
                    ce = nextDigit(collator, ce, codepoint);
                    break;
                    // various implicits optimization
                case CE_CJK_IMPLICIT_TAG_:
                    // 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D
                    return nextImplicit(codepoint);
                case CE_IMPLICIT_TAG_: // everything that is not defined
                    return nextImplicit(codepoint);
                case CE_TRAIL_SURROGATE_TAG_:
                    return IGNORABLE; // DC00-DFFF broken surrogate
                case CE_LEAD_SURROGATE_TAG_:  // D800-DBFF
                    return nextSurrogate(ch);
                case CE_HANGUL_SYLLABLE_TAG_: // AC00-D7AF
                    return nextHangul(collator, ch);
                case CE_CHARSET_TAG_:
                                    // not yet implemented probably after 1.8
                    return CE_NOT_FOUND_;
                default:
                    ce = IGNORABLE;
                    // synwee todo, throw exception or something here.
                }
                if (!RuleBasedCollator.isSpecial(ce)) {
                    break;
                }
            }
        } 
        finally {
            m_utilSpecialEntryBackUp_ = entrybackup;
        }
        return ce;
    }

    /**
     * Special processing is getting a CE that is preceded by a certain prefix.
     * Currently this is only needed for optimizing Japanese length and
     * iteration marks. When we encouter a special processing tag, we go
     * backwards and try to see if we have a match. Contraction tables are used
     * - so the whole process is not unlike contraction. prefix data is stored
     * backwards in the table.
     * @param collator current collator
     * @param ce current ce
     * @return previous ce
     */
    private int previousSpecialPrefix(RuleBasedCollator collator, int ce)
    {
        backupInternalState(m_utilSpecialBackUp_);
        while (true) {
            // position ourselves at the begining of contraction sequence
            int offset = getContractionOffset(collator, ce);
            int entryoffset = offset;
            if (isBackwardsStart()) {
                ce = collator.m_contractionCE_[offset];
                break;
            }
            char prevch = (char)previousChar();
            while (prevch > collator.m_contractionIndex_[offset]) {
                // since contraction codepoints are ordered, we skip all that
                // are smaller
                offset ++;
            }
            if (prevch == collator.m_contractionIndex_[offset]) {
                ce = collator.m_contractionCE_[offset];
            }
            else {
                // if there is a completely ignorable code point in the middle
                // of a prefix, we need to act as if it's not there assumption:
                // 'real' noncharacters (*fffe, *ffff, fdd0-fdef are set to
                // zero)
                // lone surrogates cannot be set to zero as it would break
                // other processing
                int isZeroCE = collator.m_trie_.getLeadValue(prevch);
                // it's easy for BMP code points
                if (isZeroCE == 0) {
                    continue;
                }
                else if (UTF16.isTrailSurrogate(prevch)
                         || UTF16.isLeadSurrogate(prevch)) {
                    // for supplementary code points, we have to check the next one
                    // situations where we are going to ignore
                    // 1. beginning of the string: schar is a lone surrogate
                    // 2. schar is a lone surrogate
                    // 3. schar is a trail surrogate in a valid surrogate
                    //    sequence that is explicitly set to zero.
                    if (!isBackwardsStart()) {
                        char lead = (char)previousChar();
                        if (UTF16.isLeadSurrogate(lead)) {
                            isZeroCE = collator.m_trie_.getLeadValue(lead);
                            if (RuleBasedCollator.getTag(isZeroCE)
                                == RuleBasedCollator.CE_SURROGATE_TAG_) {
                                int finalCE = collator.m_trie_.getTrailValue(
                                                                      isZeroCE,
                                                                      prevch);
                                if (finalCE == 0) {
                                    // this is a real, assigned completely
                                    // ignorable code point
                                    continue;
                                }
                            }
                        }
                        else {
                            nextChar(); // revert to original offset
                            // lone surrogate, completely ignorable
                            continue;
                        }
                        nextChar(); // revert to original offset
                    }
                    else {
                         // lone surrogate at the beggining, completely ignorable
                         continue;
                    }
                }

                // char was not in the table. prefix not found
                ce = collator.m_contractionCE_[entryoffset];
            }

            if (!isSpecialPrefixTag(ce)) {
                // char was in the contraction table, and the corresponding ce
                // is not a prefix ce.  We found the prefix, break out of loop,
                // this ce will end up being returned.
                break;
            }
        }
        updateInternalState(m_utilSpecialBackUp_);
        return ce;
    }

    /**
     * Retrieves the previous contraction ce. To ensure that the backwards and
     * forwards iteration matches, we take the current region of most possible
     * match and pass it through the forward iteration. This will ensure that
     * the obstinate problem of overlapping contractions will not occur.
     * @param collator current collator
     * @param ce current ce
     * @param ch current character
     * @return previous contraction ce
     */
    private int previousContraction(RuleBasedCollator collator, int ce, char ch)
    {
        m_utilStringBuffer_.setLength(0);
        // since we might encounter normalized characters (from the thai
        // processing) we can't use peekCharacter() here.
        char prevch = (char)previousChar();
        boolean atStart = false;
        // TODO: address the comment above - maybe now we *can* use peekCharacter
        //while (collator.isUnsafe(ch) || isThaiPreVowel(prevch)) {
        while (collator.isUnsafe(ch)) {
            m_utilStringBuffer_.insert(0, ch);
            ch = prevch;
            if (isBackwardsStart()) {
                atStart = true;
                break;
            }
            prevch = (char)previousChar();
        }
        if (!atStart) {
            // undo the previousChar() if we didn't reach the beginning 
            nextChar();
        }
        // adds the initial base character to the string
        m_utilStringBuffer_.insert(0, ch);

        // a new collation element iterator is used to simply things, since
        // using the current collation element iterator will mean that the
        // forward and backwards iteration will share and change the same
        // buffers. it is going to be painful.
        int originaldecomp = collator.getDecomposition();
        // for faster access, since string would have been normalized above
        collator.setDecomposition(Collator.NO_DECOMPOSITION);
        if (m_utilColEIter_ == null) {
            m_utilColEIter_ = new CollationElementIterator(
                                                m_utilStringBuffer_.toString(),
                                                collator);
        }
        else {
            m_utilColEIter_.m_collator_ = collator;
            m_utilColEIter_.setText(m_utilStringBuffer_.toString());
        }
        ce = m_utilColEIter_.next();
        m_CEBufferSize_ = 0;
        while (ce != NULLORDER) {
            if (m_CEBufferSize_ == m_CEBuffer_.length) {
                try {
                    // increasing cebuffer size
                    int tempbuffer[] = new int[m_CEBuffer_.length + 50];
                    System.arraycopy(m_CEBuffer_, 0, tempbuffer, 0,
                                     m_CEBuffer_.length);
                    m_CEBuffer_ = tempbuffer;
                }
                catch( MissingResourceException e)
                {
                    throw e;
                }
                catch (Exception e) {
                    if(DEBUG){
                        e.printStackTrace();
                    }
                    return NULLORDER;
                }
            }
            m_CEBuffer_[m_CEBufferSize_ ++] = ce;
            ce = m_utilColEIter_.next();
        }
        collator.setDecomposition(originaldecomp);
        m_CEBufferOffset_ = m_CEBufferSize_ - 1;
        return m_CEBuffer_[m_CEBufferOffset_];
    }

    /**
     * Returns the previous long primary ces
     * @param ce long primary ce
     * @return previous long primary ces
     */
    private int previousLongPrimary(int ce)
    {
        m_CEBufferSize_ = 0;
        m_CEBuffer_[m_CEBufferSize_ ++] =
            ((ce & 0xFFFF00) << 8) | (CE_BYTE_COMMON_ << 8) | CE_BYTE_COMMON_;
        m_CEBuffer_[m_CEBufferSize_ ++] = ((ce & 0xFF) << 24)
            | RuleBasedCollator.CE_CONTINUATION_MARKER_;
        m_CEBufferOffset_ = m_CEBufferSize_ - 1;
        return m_CEBuffer_[m_CEBufferOffset_];
    }

    /**
     * Returns the previous expansion ces
     * @param collator current collator
     * @param ce current ce
     * @return previous expansion ce
     */
    private int previousExpansion(RuleBasedCollator collator, int ce)
    {
        // find the offset to expansion table
        int offset = getExpansionOffset(collator, ce);
        m_CEBufferSize_ = getExpansionCount(ce);
        if (m_CEBufferSize_ != 0) {
            // less than 16 elements in expansion
            for (int i = 0; i < m_CEBufferSize_; i ++) {
                m_CEBuffer_[i] = collator.m_expansion_[offset + i];
            }

        }
        else {
            // null terminated ces
            while (collator.m_expansion_[offset + m_CEBufferSize_] != 0) {
                m_CEBuffer_[m_CEBufferSize_] =
                    collator.m_expansion_[offset + m_CEBufferSize_];
                m_CEBufferSize_ ++;
            }
        }
        m_CEBufferOffset_ = m_CEBufferSize_ - 1;
        return m_CEBuffer_[m_CEBufferOffset_];
    }
    
    /**
     * Getting the digit collation elements
     * @param collator
     * @param ce current collation element
     * @param ch current code point
     * @return digit collation element
     */
    private int previousDigit(RuleBasedCollator collator, int ce, char ch)
    {
        // We do a check to see if we want to collate digits as numbers; if so we generate
        //  a custom collation key. Otherwise we pull out the value stored in the expansion table.
        if (m_collator_.m_isNumericCollation_){
            int leadingZeroIndex = 0;
            int collateVal = 0;
            boolean nonZeroValReached = false;

            // clear and set initial string buffer length
            m_utilStringBuffer_.setLength(3);
        
            // We parse the source string until we hit a char that's NOT a digit
            // Use this u_charDigitValue. This might be slow because we have to 
            // handle surrogates...
            int char32 = ch;
            if (UTF16.isTrailSurrogate(ch)) {
                if (!isBackwardsStart()){
                    char lead = (char)previousChar();
                    if (UTF16.isLeadSurrogate(lead)) {
                        char32 = UCharacterProperty.getRawSupplementary(lead,
                                                                        ch);
                    } 
                    else {
                        goForwardOne();
                    }
                }
            } 
            int digVal = UCharacter.digit(char32);
            int digIndx = 0;
            for (;;) {
                // Make sure we have enough space.
                if (digIndx >= ((m_utilStringBuffer_.length() - 2) << 1)) {
                    m_utilStringBuffer_.setLength(m_utilStringBuffer_.length() 
                                                  << 1);
                }
                // Skipping over "trailing" zeroes but we still add to digIndx.
                if (digVal != 0 || nonZeroValReached) {
                    if (digVal != 0 && !nonZeroValReached) {
                        nonZeroValReached = true;
                    }
                
                    // We parse the digit string into base 100 numbers (this 
                    // fits into a byte).
                    // We only add to the buffer in twos, thus if we are 
                    // parsing an odd character, that serves as the 'tens' 
                    // digit while the if we are parsing an even one, that is 
                    // the 'ones' digit. We dumped the parsed base 100 value 
                    // (collateVal) into a buffer. We multiply each collateVal 
                    // by 2 (to give us room) and add 5 (to avoid overlapping 
                    // magic CE byte values). The last byte we subtract 1 to 
                    // ensure it is less than all the other bytes. 
                    // Since we're doing in this reverse we want to put the 
                    // first digit encountered into the ones place and the 
                    // second digit encountered into the tens place.
                
                    if (digIndx % 2 == 1){
                        collateVal += digVal * 10;
                    
                        // This removes leading zeroes.
                        if (collateVal == 0 && leadingZeroIndex == 0) {
                           leadingZeroIndex = ((digIndx - 1) >>> 1) + 2;
                        }
                        else if (leadingZeroIndex != 0) {
                            leadingZeroIndex = 0;
                        }
                                            
                        m_utilStringBuffer_.setCharAt(((digIndx - 1) >>> 1) + 2, 
                                                (char)((collateVal << 1) + 6));
                        collateVal = 0;
                    }
                    else {
                        collateVal = digVal;    
                    }
                }
                digIndx ++;
            
                if (!isBackwardsStart()){
                    backupInternalState(m_utilSpecialBackUp_);
                    char32 = previousChar();
                    if (UTF16.isTrailSurrogate(ch)){
                        if (!isBackwardsStart()) {
                            char lead = (char)previousChar();
                            if (UTF16.isLeadSurrogate(lead)) {
                                char32 
                                    = UCharacterProperty.getRawSupplementary(
                                                                    lead, ch);
                            } 
                            else {
                                updateInternalState(m_utilSpecialBackUp_);
                            }
                        }
                    }
                    
                    digVal = UCharacter.digit(char32);
                    if (digVal == -1) {
                        updateInternalState(m_utilSpecialBackUp_);
                        break;
                    }
                }
                else {
                    break;
                }
            }

            if (nonZeroValReached == false) {
                digIndx = 2;
                m_utilStringBuffer_.setCharAt(2, (char)6);
            }
            
            if (digIndx % 2 != 0) {
                if (collateVal == 0 && leadingZeroIndex == 0) {
                    // This removes the leading 0 in a odd number sequence of 
                    // numbers e.g. avery001
                    leadingZeroIndex = ((digIndx - 1) >>> 1) + 2;
                }
                else {
                    // this is not a leading 0, we add it in
                    m_utilStringBuffer_.setCharAt((digIndx >>> 1) + 2,
                                                (char)((collateVal << 1) + 6));
                    digIndx ++; 
                }               
            }
                     
            int endIndex = leadingZeroIndex != 0 ? leadingZeroIndex 
                                               : ((digIndx >>> 1) + 2) ;  
            digIndx = ((endIndex - 2) << 1) + 1; // removing initial zeros         
            // Subtract one off of the last byte. 
            // Really the first byte here, but it's reversed...
            m_utilStringBuffer_.setCharAt(2, 
                                    (char)(m_utilStringBuffer_.charAt(2) - 1));          
            // We want to skip over the first two slots in the buffer. 
            // The first slot is reserved for the header byte CODAN_PLACEHOLDER. 
            // The second slot is for the sign/exponent byte: 
            // 0x80 + (decimalPos/2) & 7f.
            m_utilStringBuffer_.setCharAt(0, (char)RuleBasedCollator.CODAN_PLACEHOLDER);
            m_utilStringBuffer_.setCharAt(1, 
                                    (char)(0x80 + ((digIndx >>> 1) & 0x7F)));
        
            // Now transfer the collation key to our collIterate struct.
            // The total size for our collation key is endIndx bumped up to the 
            // next largest even value divided by two.
            m_CEBufferSize_ = 0;
            m_CEBuffer_[m_CEBufferSize_ ++] 
                        = (((m_utilStringBuffer_.charAt(0) << 8)
                            // Primary weight 
                            | m_utilStringBuffer_.charAt(1)) 
                              << RuleBasedCollator.CE_PRIMARY_SHIFT_)
                            // Secondary weight 
                            | (RuleBasedCollator.BYTE_COMMON_ 
                               << RuleBasedCollator.CE_SECONDARY_SHIFT_)
                            // Tertiary weight. 
                            | RuleBasedCollator.BYTE_COMMON_; 
             int i = endIndex - 1; // Reset the index into the buffer.
             while (i >= 2) {
                int primWeight = m_utilStringBuffer_.charAt(i --) << 8;
                if (i >= 2) {
                    primWeight |= m_utilStringBuffer_.charAt(i --);
                }
                m_CEBuffer_[m_CEBufferSize_ ++] 
                    = (primWeight << RuleBasedCollator.CE_PRIMARY_SHIFT_) 
                      | RuleBasedCollator.CE_CONTINUATION_MARKER_;
             }
             m_CEBufferOffset_ = m_CEBufferSize_ - 1;
             return m_CEBuffer_[m_CEBufferOffset_];
         }
         else {
             return collator.m_expansion_[getExpansionOffset(collator, ce)];
         }
    } 

    /**
     * Returns previous hangul ces
     * @param collator current collator
     * @param ch current character
     * @return previous hangul ce
     */
    private int previousHangul(RuleBasedCollator collator, char ch)
    {
        char L = (char)(ch - HANGUL_SBASE_);
        // we do it in this order since some compilers can do % and / in one
        // operation
        char T = (char)(L % HANGUL_TCOUNT_);
        L /= HANGUL_TCOUNT_;
        char V = (char)(L % HANGUL_VCOUNT_);
        L /= HANGUL_VCOUNT_;

        // offset them
        L += HANGUL_LBASE_;
        V += HANGUL_VBASE_;
        T += HANGUL_TBASE_;

        m_CEBufferSize_ = 0;
        if (!collator.m_isJamoSpecial_) {
            m_CEBuffer_[m_CEBufferSize_ ++] =
                collator.m_trie_.getLeadValue(L);
            m_CEBuffer_[m_CEBufferSize_ ++] =
                collator.m_trie_.getLeadValue(V);
            if (T != HANGUL_TBASE_) {
                m_CEBuffer_[m_CEBufferSize_ ++] =
                    collator.m_trie_.getLeadValue(T);
            }
            m_CEBufferOffset_ = m_CEBufferSize_ - 1;
            return m_CEBuffer_[m_CEBufferOffset_];
        }
        else {
            // Since Hanguls pass the FCD check, it is guaranteed that we won't
            // be in the normalization buffer if something like this happens
            // Move Jamos into normalization buffer
            m_buffer_.append(L);
            m_buffer_.append(V);
            if (T != HANGUL_TBASE_) {
                m_buffer_.append(T);
            }

            m_FCDStart_ = m_source_.getIndex();
            m_FCDLimit_ = m_FCDStart_ + 1;
            return IGNORABLE;
        }
    }

    /**
     * Gets implicit codepoint ces
     * @param codepoint current codepoint
     * @return implicit codepoint ces
     */
    private int previousImplicit(int codepoint)
    {
        if (!UCharacter.isLegal(codepoint)) {
            return IGNORABLE; // illegal code value, completely ignoreable!
        }
        int result = RuleBasedCollator.impCEGen_.getImplicitFromCodePoint(codepoint);
        m_CEBufferSize_ = 2;
        m_CEBufferOffset_ = 1;
        m_CEBuffer_[0] = (result & RuleBasedCollator.CE_PRIMARY_MASK_)
                         | 0x00000505;
        m_CEBuffer_[1] = ((result & 0x0000FFFF) << 16) | 0x000000C0;
        return m_CEBuffer_[1];
    }

    /**
     * Gets the previous surrogate ce
     * @param ch current character
     * @return previous surrogate ce
     */
    private int previousSurrogate(char ch)
    {
        if (isBackwardsStart()) {
            // we are at the start of the string, wrong place to be at
            return IGNORABLE;
        }
        char prevch = (char)previousChar();
        // Handles Han and Supplementary characters here.
        if (UTF16.isLeadSurrogate(prevch)) {
            return previousImplicit(
                          UCharacterProperty.getRawSupplementary(prevch, ch));
        }
        if (prevch != CharacterIterator.DONE) {
            nextChar();
        }
        return IGNORABLE; // completely ignorable
    }

    /**
     * <p>Special CE management. Expansions, contractions etc...</p>
     * @param collator can be plain UCA
     * @param ce current ce
     * @param ch current character
     * @return previous special ce
     */
    private int previousSpecial(RuleBasedCollator collator, int ce, char ch)
    {
        while(true) {
            // the only ces that loops are thai, special prefix and
            // contractions
            switch (RuleBasedCollator.getTag(ce)) {
            case CE_NOT_FOUND_TAG_:  // this tag always returns
                return ce;
            case RuleBasedCollator.CE_SURROGATE_TAG_:
                                // essentialy a disengaged lead surrogate. a broken
                                // sequence was encountered and this is an error
                return IGNORABLE;
            case CE_SPEC_PROC_TAG_:
                ce = previousSpecialPrefix(collator, ce);
                break;
            case CE_CONTRACTION_TAG_:
                // may loop for first character e.g. "0x0f71" for english
                if (isBackwardsStart()) {
                    // start of string or this is not the end of any contraction
                    ce = collator.m_contractionCE_[
                                            getContractionOffset(collator, ce)];
                    break;
                }
                return previousContraction(collator, ce, ch); // else
            case CE_LONG_PRIMARY_TAG_:
                return previousLongPrimary(ce);
            case CE_EXPANSION_TAG_: // always returns
                return previousExpansion(collator, ce);
            case CE_DIGIT_TAG_:
                ce = previousDigit(collator, ce, ch);
                break;
            case CE_HANGUL_SYLLABLE_TAG_: // AC00-D7AF
                return previousHangul(collator, ch);
            case CE_LEAD_SURROGATE_TAG_:  // D800-DBFF
                return IGNORABLE; // broken surrogate sequence
            case CE_TRAIL_SURROGATE_TAG_: // DC00-DFFF
                return previousSurrogate(ch);
            case CE_CJK_IMPLICIT_TAG_:
                // 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D
                return previousImplicit(ch);
            case CE_IMPLICIT_TAG_: // everything that is not defined
                // UCA is filled with these. Tailorings are NOT_FOUND
                return previousImplicit(ch);
            case CE_CHARSET_TAG_: // this tag always returns
                return CE_NOT_FOUND_;
            default: // this tag always returns
                ce = IGNORABLE;
            }
            if (!RuleBasedCollator.isSpecial(ce)) {
                break;
            }
        }
        return ce;
    }

    /**
     * GET IMPLICIT PRIMARY WEIGHTS
     * @param cp codepoint
     * @param value is left justified primary key
     */
//    private static final int getImplicitPrimary(int cp)
//    {
//        cp = swapCJK(cp);
//
//        //if (DEBUG) System.out.println("CJK swapped: " + Utility.hex(cp));
//        // we now have a range of numbers from 0 to 21FFFF.
//        // we must skip all 00, 01, 02 bytes, so most bytes have 253 values
//        // we must leave a gap of 01 between all values of the last byte, so
//        // the last byte has 126 values (3 byte case)
//        // we shift so that HAN all has the same first primary, for
//        // compression.
//        // for the 4 byte case, we make the gap as large as we can fit.
//        // Three byte forms are EC xx xx, ED xx xx, EE xx xx (with a gap of 1)
//        // Four byte forms (most supplementaries) are EF xx xx xx (with a gap
//        // of LAST2_MULTIPLIER == 14)
//
//        int last0 = cp - RuleBasedCollator.IMPLICIT_4BYTE_BOUNDARY_;
//        if (last0 < 0) {
//            int last1 = cp / RuleBasedCollator.LAST_COUNT_;
//            last0 = cp % RuleBasedCollator.LAST_COUNT_;
//
//            int last2 = last1 / RuleBasedCollator.OTHER_COUNT_;
//            last1 %= RuleBasedCollator.OTHER_COUNT_;
//            return RuleBasedCollator.IMPLICIT_BASE_3BYTE_ + (last2 << 24)
//                   + (last1 << 16)
//                   + ((last0 * RuleBasedCollator.LAST_MULTIPLIER_) << 8);
//        }
//        else {
//            int last1 = last0 / RuleBasedCollator.LAST_COUNT2_;
//            last0 %= RuleBasedCollator.LAST_COUNT2_;
//
//            int last2 = last1 / RuleBasedCollator.OTHER_COUNT_;
//            last1 %= RuleBasedCollator.OTHER_COUNT_;
//
//            int last3 = last2 / RuleBasedCollator.OTHER_COUNT_;
//            last2 %= RuleBasedCollator.OTHER_COUNT_;
//            return RuleBasedCollator.IMPLICIT_BASE_4BYTE_ + (last3 << 24)
//                   + (last2 << 16) + (last1 << 8)
//                   + (last0 * RuleBasedCollator.LAST2_MULTIPLIER_);
//        }
//    }

//    /**
//     * Swapping CJK characters for implicit ces
//     * @param cp codepoint CJK
//     * @return swapped result
//     */
//    private static final int swapCJK(int cp)
//    {
//        if (cp >= CJK_BASE_) {
//            if (cp < CJK_LIMIT_) {
//                return cp - CJK_BASE_;
//            }
//            if (cp < CJK_COMPAT_USED_BASE_) {
//                return cp + NON_CJK_OFFSET_;
//            }
//            if (cp < CJK_COMPAT_USED_LIMIT_) {
//                return cp - CJK_COMPAT_USED_BASE_ + (CJK_LIMIT_ - CJK_BASE_);
//            }
//            if (cp < CJK_B_BASE_) {
//                return cp + NON_CJK_OFFSET_;
//            }
//            if (cp < CJK_B_LIMIT_) {
//                return cp; // non-BMP-CJK
//            }
//            return cp + NON_CJK_OFFSET_; // non-CJK
//        }
//        if (cp < CJK_A_BASE_) {
//            return cp + NON_CJK_OFFSET_;
//        }
//        if (cp < CJK_A_LIMIT_) {
//            return cp - CJK_A_BASE_ + (CJK_LIMIT_ - CJK_BASE_)
//                   + (CJK_COMPAT_USED_LIMIT_ - CJK_COMPAT_USED_BASE_);
//        }
//        return cp + NON_CJK_OFFSET_; // non-CJK
//    }
    
//    /** 
//     * Gets a character from the source string at a given offset.
//     * Handles both normal and iterative cases.
//     * No error checking and does not access the normalization buffer 
//     * - caller beware!
//     * @param offset offset from current position which character is to be 
//     *               retrieved
//     * @return character at current position + offset
//     */
//    private char peekCharacter(int offset) 
//    {
//        if (offset != 0) {
//            int currentoffset = m_source_.getIndex();
//            m_source_.setIndex(currentoffset + offset);
//            char result = (char)m_source_.current();
//            m_source_.setIndex(currentoffset);
//            return result;
//        } 
//        else {
//            return (char)m_source_.current();
//        }
//    }

    /**
     * Moves back 1 position in the source string. This is slightly less 
     * complicated than previousChar in that it doesn't normalize while 
     * moving back. Boundary checks are not performed.
     * This method is to be used with caution, with the assumption that 
     * moving back one position will not exceed the source limits.
     * Use only with nextChar() and never call this API twice in a row without
     * nextChar() in the middle.
     */
    private void goBackOne() 
    {
        if (m_bufferOffset_ >= 0) {
            m_bufferOffset_ --;
        }
        else {
            m_source_.setIndex(m_source_.getIndex() - 1);
        }
    }
    
    /**
     * Moves forward 1 position in the source string. This is slightly less 
     * complicated than nextChar in that it doesn't normalize while 
     * moving back. Boundary checks are not performed.
     * This method is to be used with caution, with the assumption that 
     * moving back one position will not exceed the source limits.
     * Use only with previousChar() and never call this API twice in a row 
     * without previousChar() in the middle.
     */
    private void goForwardOne() 
    {
        if (m_bufferOffset_ < 0) {
            // we're working on the source and not normalizing. fast path.
            // note Thai pre-vowel reordering uses buffer too
            m_source_.setIndex(m_source_.getIndex() + 1);
        }
        else {
            // we are in the buffer, buffer offset will never be 0 here
            m_bufferOffset_ ++;
        }
    }
}
