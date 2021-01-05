// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
******************************************************************************
* Copyright (C) 1996-2015, International Business Machines Corporation and
* others. All Rights Reserved.
******************************************************************************
*/

package com.ibm.icu.impl;

import java.util.NoSuchElementException;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.RangeValueIterator;

/**
 * <p>Class enabling iteration of the values in a Trie.</p>
 *
 * <p>2015-sep-03 TODO: Only used in test code, move there.
 *
 * <p>Result of each iteration contains the interval of codepoints that have
 * the same value type and the value type itself.</p>
 * <p>The comparison of each codepoint value is done via extract(), which the
 * default implementation is to return the value as it is.</p>
 * <p>Method extract() can be overwritten to perform manipulations on
 * codepoint values in order to perform specialized comparison.</p>
 * <p>TrieIterator is designed to be a generic iterator for the CharTrie
 * and the IntTrie, hence to accommodate both types of data, the return
 * result will be in terms of int (32 bit) values.</p>
 * <p>See com.ibm.icu.text.UCharacterTypeIterator for examples of use.</p>
 * <p>Notes for porting utrie_enum from icu4c to icu4j:<br>
 * Internally, icu4c's utrie_enum performs all iterations in its body. In Java
 * sense, the caller will have to pass a object with a callback function
 * UTrieEnumRange(const void *context, UChar32 start, UChar32 limit,
 * uint32_t value) into utrie_enum. utrie_enum will then find ranges of
 * codepoints with the same value as determined by
 * UTrieEnumValue(const void *context, uint32_t value). for each range,
 * utrie_enum calls the callback function to perform a task. In this way,
 * icu4c performs the iteration within utrie_enum.
 * To follow the JDK model, icu4j is slightly different from icu4c.
 * Instead of requesting the caller to implement an object for a callback.
 * The caller will have to implement a subclass of TrieIterator, fleshing out
 * the method extract(int) (equivalent to UTrieEnumValue). Independent of icu4j,
 * the caller will have to code his own iteration and flesh out the task
 * (equivalent to UTrieEnumRange) to be performed in the iteration loop.
 * </p>
 * <p>There are basically 3 usage scenarios for porting:</p>
 * <p>1) UTrieEnumValue is the only implemented callback then just implement a
 * subclass of TrieIterator and override the extract(int) method. The
 * extract(int) method is analogous to UTrieEnumValue callback.
 * </p>
 * <p>2) UTrieEnumValue and UTrieEnumRange both are implemented then implement
 * a subclass of TrieIterator, override the extract method and iterate, e.g
 * </p>
 * <p>utrie_enum(&normTrie, _enumPropertyStartsValue, _enumPropertyStartsRange,
 *               set);<br>
 * In Java :<br>
 * <pre>
 * class TrieIteratorImpl extends TrieIterator{
 *     public TrieIteratorImpl(Trie data){
 *         super(data);
 *     }
 *     public int extract(int value){
 *         // port the implementation of _enumPropertyStartsValue here
 *     }
 * }
 * ....
 * TrieIterator fcdIter  = new TrieIteratorImpl(fcdTrieImpl.fcdTrie);
 * while(fcdIter.next(result)) {
 *     // port the implementation of _enumPropertyStartsRange
 * }
 * </pre>
 * </p>
 * <p>3) UTrieEnumRange is the only implemented callback then just implement
 * the while loop, when utrie_enum is called
 * <pre>
 * // utrie_enum(&fcdTrie, NULL, _enumPropertyStartsRange, set);
 * TrieIterator fcdIter  = new TrieIterator(fcdTrieImpl.fcdTrie);
 * while(fcdIter.next(result)){
 *     set.add(result.start);
 * }
 * </p>
 * @author synwee
 * @see com.ibm.icu.impl.Trie
 * @since release 2.1, Jan 17 2002
 */
public class TrieIterator implements RangeValueIterator

{
    // public constructor ---------------------------------------------

    /**
    * TrieEnumeration constructor
    * @param trie to be used
    * @exception IllegalArgumentException throw when argument is null.
    */
    public TrieIterator(Trie trie)
    {
        if (trie == null) {
            throw new IllegalArgumentException(
                                          "Argument trie cannot be null");
        }
        m_trie_             = trie;
        // synwee: check that extract belongs to the child class
        m_initialValue_     = extract(m_trie_.getInitialValue());
        reset();
    }

    // public methods -------------------------------------------------

    /**
    * <p>Returns true if we are not at the end of the iteration, false
    * otherwise.</p>
    * <p>The next set of codepoints with the same value type will be
    * calculated during this call and returned in the argument element.</p>
    * @param element return result
    * @return true if we are not at the end of the iteration, false otherwise.
    * @exception NoSuchElementException - if no more elements exist.
    * @see com.ibm.icu.util.RangeValueIterator.Element
    */
    @Override
    public final boolean next(Element element)
    {
        if (m_nextCodepoint_ > UCharacter.MAX_VALUE) {
            return false;
        }
        if (m_nextCodepoint_ < UCharacter.SUPPLEMENTARY_MIN_VALUE &&
            calculateNextBMPElement(element)) {
            return true;
        }
        calculateNextSupplementaryElement(element);
        return true;
    }

    /**
    * Resets the iterator to the beginning of the iteration
    */
    @Override
    public final void reset()
    {
        m_currentCodepoint_ = 0;
        m_nextCodepoint_    = 0;
        m_nextIndex_        = 0;
        m_nextBlock_ = m_trie_.m_index_[0] << Trie.INDEX_STAGE_2_SHIFT_;
        if (m_nextBlock_ == m_trie_.m_dataOffset_) {
            m_nextValue_ = m_initialValue_;
        }
        else {
            m_nextValue_ = extract(m_trie_.getValue(m_nextBlock_));
        }
        m_nextBlockIndex_ = 0;
        m_nextTrailIndexOffset_ = TRAIL_SURROGATE_INDEX_BLOCK_LENGTH_;
    }

    // protected methods ----------------------------------------------

    /**
    * Called by next() to extracts a 32 bit value from a trie value
    * used for comparison.
    * This method is to be overwritten if special manipulation is to be done
    * to retrieve a relevant comparison.
    * The default function is to return the value as it is.
    * @param value a value from the trie
    * @return extracted value
    */
    protected int extract(int value)
    {
        return value;
    }

    // private methods ------------------------------------------------

    /**
    * Set the result values
    * @param element return result object
    * @param start codepoint of range
    * @param limit (end + 1) codepoint of range
    * @param value common value of range
    */
    private final void setResult(Element element, int start, int limit,
                                 int value)
    {
        element.start = start;
        element.limit = limit;
        element.value = value;
    }

    /**
    * Finding the next element.
    * This method is called just before returning the result of
    * next().
    * We always store the next element before it is requested.
    * In the case that we have to continue calculations into the
    * supplementary planes, a false will be returned.
    * @param element return result object
    * @return true if the next range is found, false if we have to proceed to
    *         the supplementary range.
    */
    private final boolean calculateNextBMPElement(Element element)
    {
        int currentValue    = m_nextValue_;
        m_currentCodepoint_ = m_nextCodepoint_;
        m_nextCodepoint_ ++;
        m_nextBlockIndex_ ++;
        if (!checkBlockDetail(currentValue)) {
            setResult(element, m_currentCodepoint_, m_nextCodepoint_,
                      currentValue);
            return true;
        }
        // synwee check that next block index == 0 here
        // enumerate BMP - the main loop enumerates data blocks
        while (m_nextCodepoint_ < UCharacter.SUPPLEMENTARY_MIN_VALUE) {
            // because of the way the character is split to form the index
            // the lead surrogate and trail surrogate can not be in the
            // mid of a block
            if (m_nextCodepoint_ == LEAD_SURROGATE_MIN_VALUE_) {
                // skip lead surrogate code units,
                // go to lead surrogate codepoints
                m_nextIndex_ = BMP_INDEX_LENGTH_;
            }
            else if (m_nextCodepoint_ == TRAIL_SURROGATE_MIN_VALUE_) {
                // go back to regular BMP code points
                m_nextIndex_ = m_nextCodepoint_ >> Trie.INDEX_STAGE_1_SHIFT_;
            } else {
                m_nextIndex_ ++;
            }

            m_nextBlockIndex_ = 0;
            if (!checkBlock(currentValue)) {
                setResult(element, m_currentCodepoint_, m_nextCodepoint_,
                          currentValue);
                return true;
            }
        }
        m_nextCodepoint_ --;   // step one back since this value has not been
        m_nextBlockIndex_ --;  // retrieved yet.
        return false;
    }

    /**
    * Finds the next supplementary element.
    * For each entry in the trie, the value to be delivered is passed through
    * extract().
    * We always store the next element before it is requested.
    * Called after calculateNextBMP() completes its round of BMP characters.
    * There is a slight difference in the usage of m_currentCodepoint_
    * here as compared to calculateNextBMP(). Though both represents the
    * lower bound of the next element, in calculateNextBMP() it gets set
    * at the start of any loop, where-else, in calculateNextSupplementary()
    * since m_currentCodepoint_ already contains the lower bound of the
    * next element (passed down from calculateNextBMP()), we keep it till
    * the end before resetting it to the new value.
    * Note, if there are no more iterations, it will never get to here.
    * Blocked out by next().
    * @param element return result object
    */
    private final void calculateNextSupplementaryElement(Element element)
    {
        int currentValue = m_nextValue_;
        m_nextCodepoint_ ++;
        m_nextBlockIndex_ ++;

        if (UTF16.getTrailSurrogate(m_nextCodepoint_)
                                        != UTF16.TRAIL_SURROGATE_MIN_VALUE) {
            // this piece is only called when we are in the middle of a lead
            // surrogate block
            if (!checkNullNextTrailIndex() && !checkBlockDetail(currentValue)) {
                setResult(element, m_currentCodepoint_, m_nextCodepoint_,
                          currentValue);
                m_currentCodepoint_ = m_nextCodepoint_;
                return;
            }
            // we have cleared one block
            m_nextIndex_ ++;
            m_nextTrailIndexOffset_ ++;
            if (!checkTrailBlock(currentValue)) {
                setResult(element, m_currentCodepoint_, m_nextCodepoint_,
                          currentValue);
                m_currentCodepoint_ = m_nextCodepoint_;
                return;
            }
        }
        int nextLead  = UTF16.getLeadSurrogate(m_nextCodepoint_);
        // enumerate supplementary code points
        while (nextLead < TRAIL_SURROGATE_MIN_VALUE_) {
            // lead surrogate access
            final int leadBlock =
                   m_trie_.m_index_[nextLead >> Trie.INDEX_STAGE_1_SHIFT_] <<
                                                   Trie.INDEX_STAGE_2_SHIFT_;
            if (leadBlock == m_trie_.m_dataOffset_) {
                // no entries for a whole block of lead surrogates
                if (currentValue != m_initialValue_) {
                    m_nextValue_      = m_initialValue_;
                    m_nextBlock_      = leadBlock;  // == m_trie_.m_dataOffset_
                    m_nextBlockIndex_ = 0;
                    setResult(element, m_currentCodepoint_, m_nextCodepoint_,
                              currentValue);
                    m_currentCodepoint_ = m_nextCodepoint_;
                    return;
                }

                nextLead += DATA_BLOCK_LENGTH_;
                // number of total affected supplementary codepoints in one
                // block
                // this is not a simple addition of
                // DATA_BLOCK_SUPPLEMENTARY_LENGTH since we need to consider
                // that we might have moved some of the codepoints
                m_nextCodepoint_ = Character.toCodePoint((char)nextLead, (char)UTF16.TRAIL_SURROGATE_MIN_VALUE);
                continue;
            }
            if (m_trie_.m_dataManipulate_ == null) {
                throw new NullPointerException(
                            "The field DataManipulate in this Trie is null");
            }
            // enumerate trail surrogates for this lead surrogate
            m_nextIndex_ = m_trie_.m_dataManipulate_.getFoldingOffset(
                               m_trie_.getValue(leadBlock +
                                   (nextLead & Trie.INDEX_STAGE_3_MASK_)));
            if (m_nextIndex_ <= 0) {
                // no data for this lead surrogate
                if (currentValue != m_initialValue_) {
                    m_nextValue_      = m_initialValue_;
                    m_nextBlock_      = m_trie_.m_dataOffset_;
                    m_nextBlockIndex_ = 0;
                    setResult(element, m_currentCodepoint_, m_nextCodepoint_,
                              currentValue);
                    m_currentCodepoint_ = m_nextCodepoint_;
                    return;
                }
                m_nextCodepoint_ += TRAIL_SURROGATE_COUNT_;
            } else {
                m_nextTrailIndexOffset_ = 0;
                if (!checkTrailBlock(currentValue)) {
                    setResult(element, m_currentCodepoint_, m_nextCodepoint_,
                              currentValue);
                    m_currentCodepoint_ = m_nextCodepoint_;
                    return;
                }
            }
            nextLead ++;
         }

         // deliver last range
         setResult(element, m_currentCodepoint_, UCharacter.MAX_VALUE + 1,
                   currentValue);
    }

    /**
    * Internal block value calculations
    * Performs calculations on a data block to find codepoints in m_nextBlock_
    * after the index m_nextBlockIndex_ that has the same value.
    * Note m_*_ variables at this point is the next codepoint whose value
    * has not been calculated.
    * But when returned with false, it will be the last codepoint whose
    * value has been calculated.
    * @param currentValue the value which other codepoints are tested against
    * @return true if the whole block has the same value as currentValue or if
    *              the whole block has been calculated, false otherwise.
    */
    private final boolean checkBlockDetail(int currentValue)
    {
        while (m_nextBlockIndex_ < DATA_BLOCK_LENGTH_) {
            m_nextValue_ = extract(m_trie_.getValue(m_nextBlock_ +
                                                    m_nextBlockIndex_));
            if (m_nextValue_ != currentValue) {
                return false;
            }
            ++ m_nextBlockIndex_;
            ++ m_nextCodepoint_;
        }
        return true;
    }

    /**
    * Internal block value calculations
    * Performs calculations on a data block to find codepoints in m_nextBlock_
    * that has the same value.
    * Will call checkBlockDetail() if highlevel check fails.
    * Note m_*_ variables at this point is the next codepoint whose value
    * has not been calculated.
    * @param currentBlock the initial block containing all currentValue
    * @param currentValue the value which other codepoints are tested against
    * @return true if the whole block has the same value as currentValue or if
    *              the whole block has been calculated, false otherwise.
    */
    private final boolean checkBlock(int currentValue)
    {
        int currentBlock = m_nextBlock_;
        m_nextBlock_ = m_trie_.m_index_[m_nextIndex_] <<
                                                  Trie.INDEX_STAGE_2_SHIFT_;
        if (m_nextBlock_ == currentBlock &&
            (m_nextCodepoint_ - m_currentCodepoint_) >= DATA_BLOCK_LENGTH_) {
            // the block is the same as the previous one, filled with
            // currentValue
            m_nextCodepoint_ += DATA_BLOCK_LENGTH_;
        }
        else if (m_nextBlock_ == m_trie_.m_dataOffset_) {
            // this is the all-initial-value block
            if (currentValue != m_initialValue_) {
                m_nextValue_      = m_initialValue_;
                m_nextBlockIndex_ = 0;
                return false;
            }
            m_nextCodepoint_ += DATA_BLOCK_LENGTH_;
        }
        else {
            if (!checkBlockDetail(currentValue)) {
                return false;
            }
        }
        return true;
    }

    /**
    * Internal block value calculations
    * Performs calculations on multiple data blocks for a set of trail
    * surrogates to find codepoints in m_nextBlock_ that has the same value.
    * Will call checkBlock() for internal block checks.
    * Note m_*_ variables at this point is the next codepoint whose value
    * has not been calculated.
    * @param currentValue the value which other codepoints are tested against
    * @return true if the whole block has the same value as currentValue or if
    *              the whole block has been calculated, false otherwise.
    */
    private final boolean checkTrailBlock(int currentValue)
    {
        // enumerate code points for this lead surrogate
        while (m_nextTrailIndexOffset_ < TRAIL_SURROGATE_INDEX_BLOCK_LENGTH_)
        {
            // if we ever reach here, we are at the start of a new block
            m_nextBlockIndex_ = 0;
            // copy of most of the body of the BMP loop
            if (!checkBlock(currentValue)) {
                return false;
            }
            m_nextTrailIndexOffset_ ++;
            m_nextIndex_ ++;
        }
        return true;
    }

    /**
    * Checks if we are beginning at the start of a initial block.
    * If we are then the rest of the codepoints in this initial block
    * has the same values.
    * We increment m_nextCodepoint_ and relevant data members if so.
    * This is used only in for the supplementary codepoints because
    * the offset to the trail indexes could be 0.
    * @return true if we are at the start of a initial block.
    */
    private final boolean checkNullNextTrailIndex()
    {
        if (m_nextIndex_ <= 0) {
            m_nextCodepoint_ += TRAIL_SURROGATE_COUNT_ - 1;
            int nextLead  = UTF16.getLeadSurrogate(m_nextCodepoint_);
            int leadBlock =
                   m_trie_.m_index_[nextLead >> Trie.INDEX_STAGE_1_SHIFT_] <<
                                                   Trie.INDEX_STAGE_2_SHIFT_;
            if (m_trie_.m_dataManipulate_ == null) {
                throw new NullPointerException(
                            "The field DataManipulate in this Trie is null");
            }
            m_nextIndex_ = m_trie_.m_dataManipulate_.getFoldingOffset(
                               m_trie_.getValue(leadBlock +
                                   (nextLead & Trie.INDEX_STAGE_3_MASK_)));
            m_nextIndex_ --;
            m_nextBlockIndex_ =  DATA_BLOCK_LENGTH_;
            return true;
        }
        return false;
    }

    // private data members --------------------------------------------

    /**
    * Size of the stage 1 BMP indexes
    */
    private static final int BMP_INDEX_LENGTH_ =
                                        0x10000 >> Trie.INDEX_STAGE_1_SHIFT_;
    /**
    * Lead surrogate minimum value
    */
    private static final int LEAD_SURROGATE_MIN_VALUE_ = 0xD800;
    /**
    * Trail surrogate minimum value
    */
    private static final int TRAIL_SURROGATE_MIN_VALUE_ = 0xDC00;
    /*
    * Trail surrogate maximum value
    */
    //private static final int TRAIL_SURROGATE_MAX_VALUE_ = 0xDFFF;
    /**
    * Number of trail surrogate
    */
    private static final int TRAIL_SURROGATE_COUNT_ = 0x400;
    /**
    * Number of stage 1 indexes for supplementary calculations that maps to
    * each lead surrogate character.
    * See second pass into getRawOffset for the trail surrogate character.
    * 10 for significant number of bits for trail surrogates, 5 for what we
    * discard during shifting.
    */
    private static final int TRAIL_SURROGATE_INDEX_BLOCK_LENGTH_ =
                                    1 << (10 - Trie.INDEX_STAGE_1_SHIFT_);
    /**
    * Number of data values in a stage 2 (data array) block.
    */
    private static final int DATA_BLOCK_LENGTH_ =
                                              1 << Trie.INDEX_STAGE_1_SHIFT_;
//    /**
//    * Number of codepoints in a stage 2 block
//    */
//    private static final int DATA_BLOCK_SUPPLEMENTARY_LENGTH_ =
//                                                     DATA_BLOCK_LENGTH_ << 10;
    /**
    * Trie instance
    */
    private Trie m_trie_;
    /**
    * Initial value for trie values
    */
    private int m_initialValue_;
    /**
    * Next element results and data.
    */
    private int m_currentCodepoint_;
    private int m_nextCodepoint_;
    private int m_nextValue_;
    private int m_nextIndex_;
    private int m_nextBlock_;
    private int m_nextBlockIndex_;
    private int m_nextTrailIndexOffset_;
}
