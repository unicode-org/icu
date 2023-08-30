// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ******************************************************************************
 * Copyright (C) 1996-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************
 */

package com.ibm.icu.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.ibm.icu.text.UTF16;

/**
 * Trie implementation which stores data in int, 32 bits.
 * 2015-sep-03: Used only in CharsetSelector which could be switched to {@link Trie2_32}
 * as long as that does not load ICU4C selector data.
 *
 * @author synwee
 * @see com.ibm.icu.impl.Trie
 * @since release 2.1, Jan 01 2002
 */
public class IntTrie extends Trie
{
    // public constructors ---------------------------------------------

    /**
    * <p>Creates a new Trie with the settings for the trie data.</p>
    * <p>Unserialize the 32-bit-aligned input stream and use the data for the
    * trie.</p>
    * @param bytes file buffer to a ICU data file, containing the trie
    * @param dataManipulate object which provides methods to parse the char
    *                        data
    * @throws IOException thrown when data reading fails
    */
    public IntTrie(ByteBuffer bytes, DataManipulate dataManipulate)
                                                    throws IOException
    {
        super(bytes, dataManipulate);
        if (!isIntTrie()) {
            throw new IllegalArgumentException(
                               "Data given does not belong to a int trie.");
        }
    }

    /**
     * Make a dummy IntTrie.
     * A dummy trie is an empty runtime trie, used when a real data trie cannot
     * be loaded.
     *
     * The trie always returns the initialValue,
     * or the leadUnitValue for lead surrogate code points.
     * The Latin-1 part is always set up to be linear.
     *
     * @param initialValue the initial value that is set for all code points
     * @param leadUnitValue the value for lead surrogate code _units_ that do not
     *                      have associated supplementary data
     * @param dataManipulate object which provides methods to parse the char data
     */
    @SuppressWarnings("all") // No way to ignore dead code warning specifically - see eclipse bug#282770
    public IntTrie(int initialValue, int leadUnitValue, DataManipulate dataManipulate) {
        super(new char[BMP_INDEX_LENGTH+SURROGATE_BLOCK_COUNT], HEADER_OPTIONS_LATIN1_IS_LINEAR_MASK_, dataManipulate);

        int dataLength, latin1Length, i, limit;
        char block;

        /* calculate the actual size of the dummy trie data */

        /* max(Latin-1, block 0) */
        dataLength=latin1Length= INDEX_STAGE_1_SHIFT_<=8 ? 256 : DATA_BLOCK_LENGTH;
        if(leadUnitValue!=initialValue) {
            dataLength+=DATA_BLOCK_LENGTH;
        }
        m_data_=new int[dataLength];
        m_dataLength_=dataLength;

        m_initialValue_=initialValue;

        /* fill the index and data arrays */

        /* indexes are preset to 0 (block 0) */

        /* Latin-1 data */
        for(i=0; i<latin1Length; ++i) {
            m_data_[i]=initialValue;
        }

        if(leadUnitValue!=initialValue) {
            /* indexes for lead surrogate code units to the block after Latin-1 */
            block=(char)(latin1Length>>INDEX_STAGE_2_SHIFT_);
            i=0xd800>>INDEX_STAGE_1_SHIFT_;
            limit=0xdc00>>INDEX_STAGE_1_SHIFT_;
            for(; i<limit; ++i) {
                m_index_[i]=block;
            }

            /* data for lead surrogate code units */
            limit=latin1Length+DATA_BLOCK_LENGTH;
            for(i=latin1Length; i<limit; ++i) {
                m_data_[i]=leadUnitValue;
            }
        }
    }

    // public methods --------------------------------------------------

    /**
    * Gets the value associated with the codepoint.
    * If no value is associated with the codepoint, a default value will be
    * returned.
    * @param ch codepoint
    * @return offset to data
    */
    public final int getCodePointValue(int ch)
    {
        int offset;

        // fastpath for U+0000..U+D7FF
        if(0 <= ch && ch < UTF16.LEAD_SURROGATE_MIN_VALUE) {
            // copy of getRawOffset()
            offset = (m_index_[ch >> INDEX_STAGE_1_SHIFT_] << INDEX_STAGE_2_SHIFT_)
                    + (ch & INDEX_STAGE_3_MASK_);
            return m_data_[offset];
        }

        // handle U+D800..U+10FFFF
        offset = getCodePointOffset(ch);
        return (offset >= 0) ? m_data_[offset] : m_initialValue_;
    }

    /**
    * Gets the value to the data which this lead surrogate character points
    * to.
    * Returned data may contain folding offset information for the next
    * trailing surrogate character.
    * This method does not guarantee correct results for trail surrogates.
    * @param ch lead surrogate character
    * @return data value
    */
    public final int getLeadValue(char ch)
    {
        return m_data_[getLeadOffset(ch)];
    }

    /**
    * Get the value associated with the BMP code point.
    * Lead surrogate code points are treated as normal code points, with
    * unfolded values that may differ from getLeadValue() results.
    * @param ch the input BMP code point
    * @return trie data value associated with the BMP codepoint
    */
    public final int getBMPValue(char ch)
    {
        return m_data_[getBMPOffset(ch)];
    }

    /**
    * Get the value associated with a pair of surrogates.
    * @param lead a lead surrogate
    * @param trail a trail surrogate
    */
    public final int getSurrogateValue(char lead, char trail)
    {
        if (!UTF16.isLeadSurrogate(lead) || !UTF16.isTrailSurrogate(trail)) {
            throw new IllegalArgumentException(
                "Argument characters do not form a supplementary character");
        }
        // get fold position for the next trail surrogate
        int offset = getSurrogateOffset(lead, trail);

        // get the real data from the folded lead/trail units
        if (offset > 0) {
            return m_data_[offset];
        }

        // return m_initialValue_ if there is an error
        return m_initialValue_;
    }

    /**
    * Get a value from a folding offset (from the value of a lead surrogate)
    * and a trail surrogate.
    * @param leadvalue the value of a lead surrogate that contains the
    *        folding offset
    * @param trail surrogate
    * @return trie data value associated with the trail character
    */
    public final int getTrailValue(int leadvalue, char trail)
    {
        if (m_dataManipulate_ == null) {
            throw new NullPointerException(
                             "The field DataManipulate in this Trie is null");
        }
        int offset = m_dataManipulate_.getFoldingOffset(leadvalue);
        if (offset > 0) {
            return m_data_[getRawOffset(offset,
                                         (char)(trail & SURROGATE_MASK_))];
        }
        return m_initialValue_;
    }

    /**
     * <p>Gets the latin 1 fast path value.</p>
     * <p>Note this only works if latin 1 characters have their own linear
     * array.</p>
     * @param ch latin 1 characters
     * @return value associated with latin character
     */
    public final int getLatin1LinearValue(char ch)
    {
        return m_data_[INDEX_STAGE_3_MASK_ + 1 + ch];
    }

    /**
     * Checks if the argument Trie has the same data as this Trie
     * @param other Trie to check
     * @return true if the argument Trie has the same data as this Trie, false
     *         otherwise
     */
    ///CLOVER:OFF
    @Override
    public boolean equals(Object other)
    {
        boolean result = super.equals(other);
        if (result && other instanceof IntTrie) {
            IntTrie othertrie = (IntTrie)other;
            if (m_initialValue_ != othertrie.m_initialValue_
                || !Arrays.equals(m_data_, othertrie.m_data_)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }
    ///CLOVER:ON

    // protected methods -----------------------------------------------

    /**
    * <p>Parses the input stream and stores its trie content into a index and
    * data array</p>
    * @param bytes data buffer containing trie data
    */
    @Override
    protected final void unserialize(ByteBuffer bytes)
    {
        super.unserialize(bytes);
        // one used for initial value
        m_data_ = ICUBinary.getInts(bytes, m_dataLength_, 0);
        m_initialValue_ = m_data_[0];
    }

    /**
    * Gets the offset to the data which the surrogate pair points to.
    * @param lead lead surrogate
    * @param trail trailing surrogate
    * @return offset to data
    */
    @Override
    protected final int getSurrogateOffset(char lead, char trail)
    {
        if (m_dataManipulate_ == null) {
            throw new NullPointerException(
                             "The field DataManipulate in this Trie is null");
        }
        // get fold position for the next trail surrogate
        int offset = m_dataManipulate_.getFoldingOffset(getLeadValue(lead));

        // get the real data from the folded lead/trail units
        if (offset > 0) {
            return getRawOffset(offset, (char)(trail & SURROGATE_MASK_));
        }

        // return -1 if there is an error, in this case we return the default
        // value: m_initialValue_
        return -1;
    }

    /**
    * Gets the value at the argument index.
    * For use internally in TrieIterator
    * @param index value at index will be retrieved
    * @return 32 bit value
    * @see com.ibm.icu.impl.TrieIterator
    */
    @Override
    protected final int getValue(int index)
    {
      return m_data_[index];
    }

    /**
    * Gets the default initial value
    * @return 32 bit value
    */
    @Override
    protected final int getInitialValue()
    {
        return m_initialValue_;
    }

    // package private methods -----------------------------------------

    /**
     * Internal constructor for builder use
     * @param index the index array to be slotted into this trie
     * @param data the data array to be slotted into this trie
     * @param initialvalue the initial value for this trie
     * @param options trie options to use
     * @param datamanipulate folding implementation
     */
    IntTrie(char index[], int data[], int initialvalue, int options,
            DataManipulate datamanipulate)
    {
        super(index, options, datamanipulate);
        m_data_ = data;
        m_dataLength_ = m_data_.length;
        m_initialValue_ = initialvalue;
    }

    // private data members --------------------------------------------

    /**
    * Default value
    */
    private int m_initialValue_;
    /**
    * Array of char data
    */
    private int m_data_[];
}
