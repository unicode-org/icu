/*
******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/IntTrieBuilder.java,v $ 
* $Date: 2002/10/31 01:09:18 $ 
* $Revision: 1.3 $
*
******************************************************************************
*/

package com.ibm.icu.impl;

import com.ibm.icu.lang.UCharacter;
import java.util.Arrays;

/**
 * Builder lass to manipulate and generate a trie.
 * This is useful for ICU data in primitive types.
 * Provides a compact way to store information that is indexed by Unicode 
 * values, such as character properties, types, keyboard values, etc. This is 
 * very useful when you have a block of Unicode data that contains significant 
 * values while the rest of the Unicode data is unused in the application or 
 * when you have a lot of redundance, such as where all 21,000 Han ideographs 
 * have the same value.  However, lookup is much faster than a hash table.
 * A trie of any primitive data type serves two purposes:
 * <UL type = round>
 *     <LI>Fast access of the indexed values.
 *     <LI>Smaller memory footprint.
 * </UL>
 * This is a direct port from the ICU4C version
 * @version            $Revision: 1.3 $
 * @author             Syn Wee Quek
 */
public class IntTrieBuilder extends TrieBuilder
{
	// public constructor ----------------------------------------------
		
	/**
	 * Copy constructor
	 */
	public IntTrieBuilder(IntTrieBuilder table)
    {
    	super(table);
		m_data_ = new int[m_dataCapacity_];
        System.arraycopy(table.m_data_, 0, m_data_, 0, m_dataLength_);
        m_initialValue_ = table.m_initialValue_;
    }
    
    /**
     * Constructs a build table
     * @param aliasdata data to be filled into table
     * @param maxdatalength maximum data length allowed in table
     * @param initialvalue inital data value
     * @param latin1linear is latin 1 to be linear
     * @return updated table
     */
    public IntTrieBuilder(int aliasdata[], int maxdatalength, 
                          int initialvalue, boolean latin1linear) 
    {
    	super();
    	if (maxdatalength < DATA_BLOCK_LENGTH_ || (latin1linear 
	                                               && maxdatalength < 1024)) {
	        throw new IllegalArgumentException(
	                                   "Argument maxdatalength is too small");
	    }
	    
	    if (aliasdata != null) {
	        m_data_ = aliasdata;
	    } 
	    else {
	        m_data_ = new int[maxdatalength];
	    }
	
	    // preallocate and reset the first data block (block index 0)
	    int j = DATA_BLOCK_LENGTH_;
	
	    if (latin1linear) {
	        // preallocate and reset the first block (number 0) and Latin-1 
	        // (U+0000..U+00ff) after that made sure above that 
	        // maxDataLength >= 1024
	        // set indexes to point to consecutive data blocks
	        int i = 0;
	        do {
	            // do this at least for trie->index[0] even if that block is 
	            // only partly used for Latin-1
	            m_index_[i ++] = j;
	            j += DATA_BLOCK_LENGTH_;
	        } while (i < (256 >> SHIFT_));
	    }
	
        m_dataLength_ = j;
	    // reset the initially allocated blocks to the initial value
        Arrays.fill(m_data_, 0, m_dataLength_, initialvalue);
	    m_initialValue_ = initialvalue;
	    m_dataCapacity_ = maxdatalength;
	    m_isLatin1Linear_ = latin1linear;
	    m_isCompacted_ = false;
	}

	// public methods -------------------------------------------------------
     
    /*public final void print()
    {
        int i = 0;
        int oldvalue = m_index_[i];
        int count = 0;
        System.out.println("index length " + m_indexLength_ 
                           + " --------------------------");
        while (i < m_indexLength_) {
            if (m_index_[i] != oldvalue) {
                System.out.println("index has " + count + " counts of " 
                                   + Integer.toHexString(oldvalue));
                count = 0;
                oldvalue = m_index_[i];
            }
            count ++;
            i ++;
        }
        System.out.println("index has " + count + " counts of " 
                           + Integer.toHexString(oldvalue));
        i = 0;
        oldvalue = m_data_[i];
        count = 0;
        System.out.println("data length " + m_dataLength_ 
                           + " --------------------------");
        while (i < m_dataLength_) {
            if (m_data_[i] != oldvalue) {
                if ((oldvalue & 0xf1000000) == 0xf1000000) {
                    int temp = oldvalue & 0xffffff; 
                    temp += 0x320;
                    oldvalue = 0xf1000000 | temp;
                }
                if ((oldvalue & 0xf2000000) == 0xf2000000) {
                    int temp = oldvalue & 0xffffff; 
                    temp += 0x14a;
                    oldvalue = 0xf2000000 | temp;
                }
                System.out.println("data has " + count + " counts of " 
                                   + Integer.toHexString(oldvalue));
                count = 0;
                oldvalue = m_data_[i];
            }
            count ++;
            i ++;
        }
        if ((oldvalue & 0xf1000000) == 0xf1000000) {
            int temp = oldvalue & 0xffffff; 
            temp += 0x320;
            oldvalue = 0xf1000000 | temp;
        }
        if ((oldvalue & 0xf2000000) == 0xf2000000) {
            int temp = oldvalue & 0xffffff; 
            temp += 0x14a;
            oldvalue = 0xf2000000 | temp;
        }
        System.out.println("data has " + count + " counts of " 
                           + Integer.toHexString(oldvalue));
    }
    */   
    /**
     * Gets a 32 bit data from the table data
     * @param ch codepoint which data is to be retrieved
     * @return the 32 bit data
     */
    public int getValue(int ch) 
    {
        // valid, uncompacted trie and valid c?
        if (m_isCompacted_ || ch > UCharacter.MAX_VALUE || ch < 0) {
            return 0;
        }
    
        int block = m_index_[ch >> SHIFT_];
        return m_data_[Math.abs(block) + (ch & MASK_)];
    }
    
    /**
     * Sets a 32 bit data in the table data
     * @param ch codepoint which data is to be set
     * @param value to set
     * @return true if the set is successful, otherwise 
     *              if the table has been compacted return false
     */
    public boolean setValue(int ch, int value) 
    {
        // valid, uncompacted trie and valid c? 
        if (m_isCompacted_ || ch > UCharacter.MAX_VALUE || ch < 0) {
            return false;
        }
    
        int block = getDataBlock(ch);
        if (block < 0) {
            return false;
        }
    
        m_data_[block + (ch & MASK_)] = value;
        return true;
    }
    
    /**
     * Serializes the build table with 32 bit data
     * @param datamanipulate builder raw fold method implementation
     * @param triedatamanipulate result trie fold method
     * @return a new trie
     */
    public IntTrie serialize(TrieBuilder.DataManipulate datamanipulate, 
                             Trie.DataManipulate triedatamanipulate)
    {
        if (datamanipulate == null) {
            throw new IllegalArgumentException("Parameters can not be null");
        }
        // fold and compact if necessary, also checks that indexLength is 
        // within limits 
        if (!m_isCompacted_) {
            // compact once without overlap to improve folding
            compact(false);
            // fold the supplementary part of the index array
            fold(datamanipulate);
            // compact again with overlap for minimum data array length
            compact(true);
            m_isCompacted_ = true;
        }
        // is dataLength within limits? 
        if (m_dataLength_ >= MAX_DATA_LENGTH_) {
            throw new ArrayIndexOutOfBoundsException("Data length too small");
        }
    
        char index[] = new char[m_indexLength_];
        int data[] = new int[m_dataLength_];
        // write the index (stage 1) array and the 32-bit data (stage 2) array
        // write 16-bit index values shifted right by INDEX_SHIFT_ 
        for (int i = 0; i < m_indexLength_; i ++) {
            index[i] = (char)(m_index_[i] >>> INDEX_SHIFT_);
        }
        // write 32-bit data values
        System.arraycopy(m_data_, 0, data, 0, m_dataLength_);
        
        int options = SHIFT_ | (INDEX_SHIFT_ << OPTIONS_INDEX_SHIFT_);
        options |= OPTIONS_DATA_IS_32_BIT_;
        if (m_isLatin1Linear_) {
            options |= OPTIONS_LATIN1_IS_LINEAR_;
        }
        return new IntTrie(index, data, m_initialValue_, options, 
                           triedatamanipulate);
    }
    
	// public data member ---------------------------------------------
		
	protected int m_data_[];
	protected int m_initialValue_;  
	
	// private methods ------------------------------------------------------
   
    /**
     * No error checking for illegal arguments.
     * @param ch codepoint to look for
     * @return -1 if no new data block available (out of memory in data array)
     */
    private int getDataBlock(int ch) 
    {
        ch >>= SHIFT_;
        int indexValue = m_index_[ch];
        if (indexValue > 0) {
            return indexValue;
        }
    
        // allocate a new data block
        int newBlock = m_dataLength_;
        int newTop = newBlock + DATA_BLOCK_LENGTH_;
        if (newTop > m_dataCapacity_) {
            // out of memory in the data array 
            return -1;
        }
        m_dataLength_ = newTop;
        m_index_[ch] = newBlock;
    
        // copy-on-write for a block from a setRange()
        Arrays.fill(m_data_, newBlock, newBlock + DATA_BLOCK_LENGTH_, 
                    m_initialValue_);
        return newBlock;
    }
    
    /**
     * Compact a folded build-time trie.
     * The compaction
     * - removes blocks that are identical with earlier ones
     * - overlaps adjacent blocks as much as possible (if overlap == true)
     * - moves blocks in steps of the data granularity
     *
     * It does not
     * - try to move and overlap blocks that are not already adjacent
     * - try to move and overlap blocks that overlap with multiple values in 
     * the overlap region
     * @param overlap flag
     */
    private void compact(boolean overlap) 
    {
        if (m_isCompacted_) {
            return; // nothing left to do
        }
    
        // compaction
        // initialize the index map with "block is used/unused" flags
        findUnusedBlocks();
        
        // if Latin-1 is preallocated and linear, then do not compact Latin-1 
        // data
        int overlapStart = DATA_BLOCK_LENGTH_;
        if (m_isLatin1Linear_ && SHIFT_ <= 8) {
            overlapStart += 256;
        }
       
        int newStart = DATA_BLOCK_LENGTH_;
        int prevEnd = newStart - 1;
        for (int start = newStart; start < m_dataLength_;) {
            // start: index of first entry of current block
            // prevEnd: index to last entry of previous block
            // newStart: index where the current block is to be moved
            // skip blocks that are not used 
            if (m_map_[start >> SHIFT_] < 0) {
                // advance start to the next block 
                start += DATA_BLOCK_LENGTH_;
                // leave prevEnd and newStart with the previous block!
                continue;
            }
            // search for an identical block
            if (start >= overlapStart) {
                int i = findSameDataBlock(m_data_, newStart, start,
                             overlap ? DATA_GRANULARITY_ : DATA_BLOCK_LENGTH_);
                if (i >= 0) {
                    // found an identical block, set the other block's index 
                    // value for the current block
                    m_map_[start >> SHIFT_] = i;
                    // advance start to the next block
                    start += DATA_BLOCK_LENGTH_;
                    // leave prevEnd and newStart with the previous block!
                    continue;
                }
            }
            // see if the beginning of this block can be overlapped with the 
            // end of the previous block
            // x: first value in the current block 
            int x = m_data_[start];
            int i = 0;
            if (x == m_data_[prevEnd] && overlap && start >= overlapStart) 
            {
                // overlap by at least one
                for (i = 1; i < DATA_BLOCK_LENGTH_ 
                     && x == m_data_[start + i] 
                     && x == m_data_[prevEnd - i]; ++ i) 
                {
                }
    
                // overlap by i, rounded down for the data block granularity
                i &= ~(DATA_GRANULARITY_ - 1);
            } 
            if (i > 0) {
                // some overlap
                m_map_[start >> SHIFT_] = newStart - i;
                // move the non-overlapping indexes to their new positions
                start += i;
                for (i = DATA_BLOCK_LENGTH_ - i; i > 0; -- i) {
                    m_data_[newStart ++] = m_data_[start ++];
                }
            } 
            else if (newStart < start) {
                // no overlap, just move the indexes to their new positions
                m_map_[start >> SHIFT_] = newStart;
                for (i = DATA_BLOCK_LENGTH_; i > 0; -- i) {
                    m_data_[newStart ++] = m_data_[start ++];
                }
            } 
            else { // no overlap && newStart==start
                m_map_[start >> SHIFT_] = start;
                newStart += DATA_BLOCK_LENGTH_;
                start = newStart;
            }
    
            prevEnd = newStart - 1;
        }
        // now adjust the index (stage 1) table
        for (int i = 0; i < m_indexLength_; ++ i) {
            m_index_[i] = m_map_[m_index_[i] >>> SHIFT_];
        }
        m_dataLength_ = newStart;
    }

    /**
     * Find the same data block
     * @param data array
     * @param dataLength
     * @param otherBlock
     * @param step
     */
    private static final int findSameDataBlock(int data[], int dataLength,
                                                int otherBlock, int step) 
    {
        // ensure that we do not even partially get past dataLength
        dataLength -= DATA_BLOCK_LENGTH_;

        for (int block = 0; block <= dataLength; block += step) {
            int i = 0;
            for (i = 0; i < DATA_BLOCK_LENGTH_; ++ i) {
                if (data[block + i] != data[otherBlock + i]) {
                    break;
                }
            }
            if (i == DATA_BLOCK_LENGTH_) {
                return block;
            }
        }
        return -1;
    }
    
    /**
     * Fold the normalization data for supplementary code points into
     * a compact area on top of the BMP-part of the trie index,
     * with the lead surrogates indexing this compact area.
     *
     * Duplicate the index values for lead surrogates:
     * From inside the BMP area, where some may be overridden with folded values,
     * to just after the BMP area, where they can be retrieved for
     * code point lookups.
     * @param manipulate fold implementation
     */
    private final void fold(DataManipulate manipulate) 
    {
        int leadIndexes[] = new int[SURROGATE_BLOCK_COUNT_];
        int index[] = m_index_;
        // copy the lead surrogate indexes into a temporary array
        System.arraycopy(index, 0xd800 >> SHIFT_, leadIndexes, 0, 
                         SURROGATE_BLOCK_COUNT_);
        
        // to protect the copied lead surrogate values,
        // mark all their indexes as repeat blocks
        // (causes copy-on-write)
        for (char c = 0xd800; c <= 0xdbff; ++ c) {
            int block = index[c >> SHIFT_];
            if (block > 0) {
                index[c >> SHIFT_] =- block;
            }
        }
     
        // Fold significant index values into the area just after the BMP 
        // indexes.
        // In case the first lead surrogate has significant data,
        // its index block must be used first (in which case the folding is a 
        // no-op).
        // Later all folded index blocks are moved up one to insert the copied
        // lead surrogate indexes.
        int indexLength = BMP_INDEX_LENGTH_;
        // search for any index (stage 1) entries for supplementary code points 
        for (int c = 0x10000; c < 0x110000;) {
            if (index[c >> SHIFT_] != 0) {
                // there is data, treat the full block for a lead surrogate
                c &= ~0x3ff;
                // is there an identical index block?
                int block = findSameIndexBlock(index, indexLength, c >> SHIFT_);
                // get a folded value for [c..c+0x400[ and, if 0, set it for 
                // the lead surrogate 
                int value = manipulate.getFoldedValue(c, 
                                                block + SURROGATE_BLOCK_COUNT_);
                if (value != 0) {
                    if (!setValue(0xd7c0 + (c >> 10), value)) {
                        // data table overflow 
                        throw new ArrayIndexOutOfBoundsException(
                                                        "Data table overflow");
                    }
                    // if we did not find an identical index block...
                    if (block == indexLength) {
                        // move the actual index (stage 1) entries from the 
                        // supplementary position to the new one
                        System.arraycopy(index, c >> SHIFT_, index, indexLength,
                                         SURROGATE_BLOCK_COUNT_);
                        indexLength += SURROGATE_BLOCK_COUNT_;
                    }
                }
                c += 0x400;
            } 
            else {
                c += DATA_BLOCK_LENGTH_;
            }
        }
        
        // index array overflow?
        // This is to guarantee that a folding offset is of the form
        // UTRIE_BMP_INDEX_LENGTH+n*UTRIE_SURROGATE_BLOCK_COUNT with n=0..1023.
        // If the index is too large, then n>=1024 and more than 10 bits are 
        // necessary.
        // In fact, it can only ever become n==1024 with completely unfoldable 
        // data and the additional block of duplicated values for lead 
        // surrogates.
        if (indexLength >= MAX_INDEX_LENGTH_) {
            throw new ArrayIndexOutOfBoundsException("Index table overflow");
        }
        // make space for the lead surrogate index block and insert it between 
        // the BMP indexes and the folded ones
        System.arraycopy(index, BMP_INDEX_LENGTH_, index, 
                         BMP_INDEX_LENGTH_ + SURROGATE_BLOCK_COUNT_,
                         indexLength - BMP_INDEX_LENGTH_);
        System.arraycopy(leadIndexes, 0, index, BMP_INDEX_LENGTH_,
                         SURROGATE_BLOCK_COUNT_);
        indexLength += SURROGATE_BLOCK_COUNT_;
        m_indexLength_ = indexLength;
    }
}
    