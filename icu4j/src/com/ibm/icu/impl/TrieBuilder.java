/*
******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/TrieBuilder.java,v $ 
* $Date: 2002/06/22 00:02:42 $ 
* $Revision: 1.5 $
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
 * @version            $Revision: 1.5 $
 * @author             Syn Wee Quek
 */
public final class TrieBuilder
{
	// public inner class ------------------------------------------------
	
	public static class BuildTable 
	{
		// public constructor ----------------------------------------------
		
		public BuildTable()
		{
			m_index_ = new int[MAX_INDEX_LENGTH_];
			m_map_ = new int[MAX_BUILD_TIME_DATA_LENGTH_ >> SHIFT_];
		}
        
        public BuildTable(BuildTable table)
        {
            m_indexLength_ = table.m_indexLength_;
            m_index_ = new int[m_indexLength_];
            System.arraycopy(table.m_index_, 0, m_index_, 0, m_indexLength_);
            m_dataCapacity_ = table.m_dataCapacity_;
            m_dataLength_ = table.m_dataLength_;
            m_data_ = new int[m_dataCapacity_];
            System.arraycopy(table.m_data_, 0, m_data_, 0, m_dataLength_);
            m_map_ = new int[table.m_map_.length];
            System.arraycopy(table.m_map_, 0, m_map_, 0, m_map_.length);
            m_isDataAllocated_ = table.m_isDataAllocated_;
            m_isLatin1Linear_ = table.m_isLatin1Linear_;
            m_isCompacted_ = table.m_isCompacted_;
            m_initialValue_ = table.m_initialValue_;
        }
		
		// public data member ---------------------------------------------
		
	    /**
	     * Index values at build-time are 32 bits wide for easier processing.
	     * Bit 31 is set if the data block is used by multiple index values 
	     * (from setRange()).
	     */
	    public int m_index_[];
	    public int m_data_[];
	
	    public int m_indexLength_; 
	    public int m_dataCapacity_; 
	    public int m_dataLength_;
	    public boolean m_isDataAllocated_;
	    public boolean m_isLatin1Linear_;
	    public boolean m_isCompacted_;
        public int m_initialValue_;
	
	    /**
	     * Map of adjusted indexes, used in utrie_compact().
	     * Maps from original indexes to new ones.
	     */
	    public int m_map_[];
	};

    // public methods ----------------------------------------------------
  
    /**
     * Opens a build table
     * @param table build table to open
     * @param aliasdata data to be filled into table
     * @param maxdatalength maximum data length allowed in table
     * @param initialvalue inital data value
     * @param latin1linear is latin 1 to be linear
     * @return updated table
     */
    public static BuildTable open(BuildTable table, int aliasdata[], 
                                  int maxdatalength, int initialvalue, 
                                  boolean latin1linear) 
    {
	    if (maxdatalength < DATA_BLOCK_LENGTH_ || (latin1linear 
	                                               && maxdatalength < 1024)) {
	        return null;
	    }
	
	    if (table == null) {
	        table = new BuildTable();
	    }
	    if (aliasdata != null) {
	        table.m_data_ = aliasdata;
	    } 
	    else {
	        table.m_data_ = new int[maxdatalength];
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
	            table.m_index_[i ++] = j;
	            j += DATA_BLOCK_LENGTH_;
	        } while (i < (256 >> SHIFT_));
	    }
	
        table.m_dataLength_ = j;
	    // reset the initially allocated blocks to the initial value
        Arrays.fill(table.m_data_, 0, table.m_dataLength_, initialvalue);
	
	    table.m_indexLength_ = MAX_INDEX_LENGTH_;
	    table.m_dataCapacity_ = maxdatalength;
	    table.m_isLatin1Linear_ = latin1linear;
	    table.m_isCompacted_ = false;
        table.m_initialValue_ = initialvalue;
	    return table;
	}

    /**
     * Gets a 32 bit data from the table data
     * @param table build table to retrieve
     * @param ch codepoint which data is to be retrieved
     * @return the 32 bit data
     */
    public static int get32(BuildTable table, int ch) 
    {
        // valid, uncompacted trie and valid c?
        if (table == null || table.m_isCompacted_ || !UCharacter.isLegal(ch)) {
            return 0;
        }
    
        int block = table.m_index_[ch >> SHIFT_];
        return table.m_data_[Math.abs(block) + (ch & MASK_)];
    }
    
    /**
     * Sets a 32 bit data in the table data
     * @param table build table to set
     * @param ch codepoint which data is to be set
     * @param value to set
     * @return true if the set is successful, otherwise if table is null or
     *              if the table has been compacted return false
     */
    public static boolean set32(BuildTable table, int ch, int value) 
    {
        // valid, uncompacted trie and valid c? 
        if (table == null || table.m_isCompacted_ || !UCharacter.isLegal(ch)) {
            return false;
        }
    
        int block = TrieBuilder.get32DataBlock(table, ch);
        if (block < 0) {
            return false;
        }
    
        table.m_data_[block + (ch & MASK_)] = value;
        return true;
    }
    
    /**
     * Serializes the build table with 32 bit data
     * @param trie build table
     * @param datamanipulate fold method implementation
     * @return a new trie
     */
    public static IntTrie createIntTrie(BuildTable trie, 
                                        Trie.DataManipulate datamanipulate)
    {
        if (trie == null || datamanipulate == null) {
            throw new IllegalArgumentException("Parameters can not be null");
        }
        // fold and compact if necessary, also checks that indexLength is 
        // within limits 
        if (!trie.m_isCompacted_) {
            // compact once without overlap to improve folding
            compact(trie, false);
            // fold the supplementary part of the index array
            fold(trie, datamanipulate);
            // compact again with overlap for minimum data array length
            compact(trie, true);
            trie.m_isCompacted_ = true;
        }
        // is dataLength within limits? 
        if (trie.m_dataLength_ >= MAX_DATA_LENGTH_) {
            throw new ArrayIndexOutOfBoundsException("Data length too small");
        }
    
        char index[] = new char[trie.m_indexLength_];
        int data[] = new int[trie.m_dataLength_];
        // write the index (stage 1) array and the 32-bit data (stage 2) array
        // write 16-bit index values shifted right by INDEX_SHIFT_ 
        for (int i = 0; i < trie.m_indexLength_; i ++) {
            index[i] = (char)(trie.m_index_[i] >>> INDEX_SHIFT_);
        }
        // write 32-bit data values
        System.arraycopy(trie.m_data_, 0, data, 0, trie.m_dataLength_);
        
        int options = SHIFT_ | (INDEX_SHIFT_ << OPTIONS_INDEX_SHIFT_);
        options |= OPTIONS_DATA_IS_32_BIT_;
        if (trie.m_isLatin1Linear_) {
            options |= OPTIONS_LATIN1_IS_LINEAR_;
        }
        return new IntTrie(index, data, trie.m_initialValue_, options, 
                           datamanipulate);
    }
    
    // package private method -----------------------------------------------
    
	  /**
	  * Takes argument array and forms a compact array into the result arrays.
	  * The result will be 
	  * <code>
	  *   array[index] == valuearray[indexarray[index]]
	  * </code>.
	  * Note : This method is generic, it only takes values from the array. 
	  * @param array value array to be manipulated
	  * @param start index of the array to process
	  * @param length of array to process.
	  * @param blocksize size of each blocks existing in valuearray
	  * @param indexarray result index array with length = array.length, with 
	  *        values which indexes to valuearray.
	  * @param valuearray result value array compact value array
	  * @return size of valuearray
	  * @deprecated release 2.1, since icu4c has written their own tool
	  */
	  static int build(byte array[], int start, int length, int blocksize, 
	                   int indexarray[], byte valuearray[])
	  {
	    int valuesize = 0;
	    int valueindex;
	    int blockcount = 0;  
	    int index = 0;
	    int min;
	    
	    while (start < length) {
	      // for a block of blocksize in the array
	      // we try to find a similar block in valuearray
	      for (valueindex = 0; valueindex < valuesize; valueindex ++) {
	        // testing each block of blocksize at index valueindex in valuearray
	        // if it is == to array blocks
	        min = Math.min(blocksize, valuesize - valueindex);
	        for (blockcount = 0; blockcount < min;blockcount ++) {
	          if (array[start + blockcount] != 
	                                        valuearray[valueindex + blockcount]) {
	            break;
	          }
	        }
	        
	        if (blockcount == blocksize || valueindex + blockcount == valuesize) {
	          break;
	        }
	      }
	
	      // if no similar block is found in value array
	      // we populate the result arrays with data
	      for (min = Math.min(blocksize, length - start); blockcount < min; 
	                                                              blockcount ++) {
	        valuearray[valuesize ++] = array[start + blockcount];
	      }
	        
	      indexarray[index ++] = valueindex;
	      start += blocksize;
	    }
	    
	    return valuesize;
	  }
	  
	  /**
	  * Takes argument array and forms a compact array into the result arrays.
	  * The result will be 
	  * <code>
	  *   array[index] == valuearray[indexarray[index]]
	  * </code>.
	  * Note : This method is generic, it only takes values from the array. 
	  * @param array value array to be manipulated
	  * @param start index of the array to process
	  * @param length of array to process.
	  * @param blocksize size of each blocks existing in valuearray
	  * @param indexarray result index array with length = array.length, with 
	  *        values which indexes to valuearray.
	  * @param valuearray result value array compact value array
	  * @return size of valuearray
	  * @deprecated release 2.1, since icu4c has written their own tool
	  */
	  static int build(char array[], int start, int length, int blocksize, 
	                   int indexarray[], char valuearray[])
	  {
	    int valuesize = 0;
	    int valueindex;
	    int blockcount = 0;  
	    int index = 0;
	    int min;
	    
	    while (start < length) {
	      // for a block of blocksize in the array
	      // we try to find a similar block in valuearray
	      for (valueindex = 0; valueindex < valuesize; valueindex ++) {
	        // testing each block of blocksize at index valueindex in valuearray
	        // if it is == to array blocks
	        min = Math.min(blocksize, valuesize - valueindex);
	        for (blockcount = 0; blockcount < min;blockcount ++) {
	          if (array[start + blockcount] != 
	                                        valuearray[valueindex + blockcount]) {
	            break;
	          }
	        }
	        
	        if (blockcount == blocksize || valueindex + blockcount == valuesize) {
	          break;
	        }
	      }
	
	      // if no similar block is found in value array
	      // we populate the result arrays with data
	      for (min = Math.min(blocksize, length - start); blockcount < min; 
	                                                              blockcount ++) {
	        valuearray[valuesize ++] = array[start + blockcount];
	      }
	        
	      indexarray[index ++] = valueindex;
	      start += blocksize;
	    }
	    
	    return valuesize;
	  }
	  
	  /**
	  * Takes argument array and forms a compact array into the result arrays.
	  * The result will be 
	  * <code>
	  *   array[index] == valuearray[indexarray[index]]
	  * </code>.
	  * Note : This method is generic, it only takes values from the array. 
	  * @param array value array to be manipulated
	  * @param start index of the array to process
	  * @param length of array to process.
	  * @param blocksize size of each blocks existing in valuearray
	  * @param indexarray result index array with length = array.length, with 
	  *        values which indexes to valuearray.
	  * @param valuearray result value array compact value array
	  * @return size of valuearray 
	  * @deprecated release 2.1, since icu4c has written their own tool
	  */
	  static int build(int array[], int start, int length, int blocksize, 
	                   int indexarray[], int valuearray[])
	  {
	    int valuesize = 0;
	    int valueindex;
	    int blockcount = 0;  
	    int index = 0;
	    int min;
	    
	    while (start < length) {
	      // for a block of blocksize in the array
	      // we try to find a similar block in valuearray
	      for (valueindex = 0; valueindex < valuesize; valueindex ++) {
	        // testing each block of blocksize at index valueindex in valuearray
	        // if it is == to array blocks
	        min = Math.min(blocksize, valuesize - valueindex);
	        for (blockcount = 0; blockcount < min; blockcount ++) {
	          if (array[start + blockcount] != 
	                                        valuearray[valueindex + blockcount]) {
	            break;
	          }
	        }
	        
	        if (blockcount == blocksize || valueindex + blockcount == valuesize) {
	          break;
	        }
	      }
	
	      // if no similar block is found in value array
	      // we populate the result arrays with data
	      min = Math.min(blocksize, length - start);
	      for (; blockcount < min; blockcount ++) {
	        valuearray[valuesize ++] = array[start + blockcount];
	      }
	        
	      indexarray[index ++] = valueindex;
	      start += blocksize;
	    }
	    
	    return valuesize;
	  }
	
	// private data member ------------------------------------------------
	/**
    * Mask for getting the lower bits from the input index.
    * DATA_BLOCK_LENGTH_ - 1.
    */
    private static final int MASK_ = Trie.INDEX_STAGE_3_MASK_;
	/**
	 * Shift size for shifting right the input index. 1..9 
	 */
	private static final int SHIFT_ = Trie.INDEX_STAGE_1_SHIFT_;
	/** 
	 * Number of data values in a stage 2 (data array) block. 2, 4, 8, .., 
	 * 0x200 
	 */
    private static final int DATA_BLOCK_LENGTH_ = 1 << SHIFT_;
	/**
     * Shift size for shifting left the index array values.
     * Increases possible data size with 16-bit index values at the cost
     * of compactability.
     * This requires blocks of stage 2 data to be aligned by UTRIE_DATA_GRANULARITY.
     * 0..UTRIE_SHIFT
     */
    private static final int INDEX_SHIFT_ = Trie.INDEX_STAGE_2_SHIFT_;
	/**
     * Length of the index (stage 1) array before folding.
     * Maximum number of Unicode code points (0x110000) shifted right by 
     * SHIFT.
     */
    private static final int MAX_INDEX_LENGTH_ = (0x110000 >> SHIFT_);	
	/**
	 * Maximum length of the runtime data (stage 2) array.
	 * Limited by 16-bit index values that are left-shifted by INDEX_SHIFT_.
	 */
	private static final int MAX_DATA_LENGTH_ = (0x10000 << INDEX_SHIFT_);
	/**
	 * Maximum length of the build-time data (stage 2) array.
	 * The maximum length is 0x110000 + DATA_BLOCK_LENGTH_ + 0x400.
	 * (Number of Unicode code points + one all-initial-value block +
	 *  possible duplicate entries for 1024 lead surrogates.)
	 */
	private static final int MAX_BUILD_TIME_DATA_LENGTH_ = 
	                                    0x110000 + DATA_BLOCK_LENGTH_ + 0x400;
    /** 
     * The alignment size of a stage 2 data block. Also the granularity for 
     * compaction. 
     */
    private static final int DATA_GRANULARITY_ = 1 << INDEX_SHIFT_;
    /** 
     * Number of bits of a trail surrogate that are used in index table lookups. 
     */
    private static final int SURROGATE_BLOCK_BITS_ = 10 - SHIFT_;
    /**
     * Number of index (stage 1) entries per lead surrogate.
     * Same as number of indexe entries for 1024 trail surrogates,
     * ==0x400>>UTRIE_SHIFT
     */
    private static final int SURROGATE_BLOCK_COUNT_ 
                                                 = 1 << SURROGATE_BLOCK_BITS_;
    /** 
     * Length of the BMP portion of the index (stage 1) array. 
     */
    private static final int BMP_INDEX_LENGTH_ = 0x10000 >> SHIFT_;
    
    /**
     * Shifting to position the index value in options
     */
    private static final int OPTIONS_INDEX_SHIFT_ = 4;
    /** 
     * If set, then the data (stage 2) array is 32 bits wide. 
     */
    private static final int OPTIONS_DATA_IS_32_BIT_ = 0x100;
    /**
     * If set, then Latin-1 data (for U+0000..U+00ff) is stored in the data 
     * (stage 2) array as a simple, linear array at data + DATA_BLOCK_LENGTH.
     */
    private static final int OPTIONS_LATIN1_IS_LINEAR_ = 0x200;
                                         
    // private methods ------------------------------------------------------
   
    /**
     * No error checking for illegal arguments.
     * @param table build table to look in
     * @param ch codepoint to look for
     * @return -1 if no new data block available (out of memory in data array)
     */
    private static int get32DataBlock(BuildTable table, int ch) 
    {
        ch >>= SHIFT_;
        int indexValue = table.m_index_[ch];
        if (indexValue > 0) {
            return indexValue;
        }
    
        // allocate a new data block
        int newBlock = table.m_dataLength_;
        int newTop = newBlock + DATA_BLOCK_LENGTH_;
        if (newTop > table.m_dataCapacity_) {
            // out of memory in the data array 
            return -1;
        }
        table.m_dataLength_ = newTop;
        table.m_index_[ch] = newBlock;
    
        // copy-on-write for a block from a setRange()
        Arrays.fill(table.m_data_, newBlock, newBlock + DATA_BLOCK_LENGTH_, 
                    table.m_initialValue_);
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
     * @param trie to compact
     * @param overlap flag
     */
    private static void compact(BuildTable trie, boolean overlap) 
    {
        // valid, uncompacted trie? 
        if (trie == null) {
            throw new IllegalArgumentException("Trie argument can not be null");
        }
        if (trie.m_isCompacted_) {
            return; // nothing left to do
        }
    
        // compaction
        // initialize the index map with "block is used/unused" flags
        _findUnusedBlocks(trie);
    
        // if Latin-1 is preallocated and linear, then do not compact Latin-1 
        // data
        int overlapStart = DATA_BLOCK_LENGTH_;
        if (trie.m_isLatin1Linear_ && SHIFT_ <= 8) {
            overlapStart += 256;
        }
       
        int newStart = DATA_BLOCK_LENGTH_;
        int prevEnd = newStart - 1;
        for (int start = newStart; start < trie.m_dataLength_;) {
            // start: index of first entry of current block
            // prevEnd: index to last entry of previous block
            // newStart: index where the current block is to be moved
            // skip blocks that are not used 
            if (trie.m_map_[start >> SHIFT_] < 0) {
                // advance start to the next block 
                start += DATA_BLOCK_LENGTH_;
                // leave prevEnd and newStart with the previous block!
                continue;
            }
            // search for an identical block
            if (start >= overlapStart) {
                int i = _findSameDataBlock(trie.m_data_, newStart, start,
                             overlap ? DATA_GRANULARITY_ : DATA_BLOCK_LENGTH_);
                if (i >= 0) {
                    // found an identical block, set the other block's index 
                    // value for the current block
                    trie.m_map_[start >> SHIFT_] = i;
                    // advance start to the next block
                    start += DATA_BLOCK_LENGTH_;
                    // leave prevEnd and newStart with the previous block!
                    continue;
                }
            }
            // see if the beginning of this block can be overlapped with the 
            // end of the previous block
            // x: first value in the current block 
            int x = trie.m_data_[start];
            int i = 0;
            if (x == trie.m_data_[prevEnd] && overlap && start >= overlapStart) 
            {
                // overlap by at least one
                for (i = 1; i < DATA_BLOCK_LENGTH_ 
                     && x == trie.m_data_[start + i] 
                     && x == trie.m_data_[prevEnd - i]; ++ i) 
                {
                }
    
                // overlap by i, rounded down for the data block granularity
                i &= ~(DATA_GRANULARITY_ - 1);
            } 
            if (i > 0) {
                // some overlap
                trie.m_map_[start >> SHIFT_] = newStart - i;
                // move the non-overlapping indexes to their new positions
                start += i;
                for (i = DATA_BLOCK_LENGTH_ - i; i > 0; -- i) {
                    trie.m_data_[newStart ++] = trie.m_data_[start ++];
                }
            } 
            else if (newStart < start) {
                // no overlap, just move the indexes to their new positions
                trie.m_map_[start >> SHIFT_] = newStart;
                for (i = DATA_BLOCK_LENGTH_; i > 0; -- i) {
                    trie.m_data_[newStart ++] = trie.m_data_[start ++];
                }
            } 
            else { // no overlap && newStart==start
                trie.m_map_[start >> SHIFT_] = start;
                newStart += DATA_BLOCK_LENGTH_;
                start = newStart;
            }
    
            prevEnd = newStart - 1;
        }
    
        // now adjust the index (stage 1) table
        for (int i = 0; i < trie.m_indexLength_; ++ i) {
            trie.m_index_[i] = trie.m_map_[Math.abs(trie.m_index_[i])
                                                                   >> SHIFT_];
        }
        trie.m_dataLength_ = newStart;
    }

    /**
     * Find the same data block
     * @param data array
     * @param dataLength
     * @param otherBlock
     * @param step
     */
    private static final int _findSameDataBlock(int data[], int dataLength,
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
     * Set a value in the trie index map to indicate which data block
     * is referenced and which one is not.
     * utrie_compact() will remove data blocks that are not used at all.
     * Set
     * - 0 if it is used
     * - -1 if it is not used
     */
    private static final void _findUnusedBlocks(BuildTable trie) 
    {
        // fill the entire map with "not used" 
        Arrays.fill(trie.m_map_, 0xff);
    
        // mark each block that _is_ used with 0
        for (int i = 0; i < trie.m_indexLength_; ++ i) {
            trie.m_map_[Math.abs(trie.m_index_[i]) >> SHIFT_] = 0;
        }
    
        // never move the all-initial-value block 0
        trie.m_map_[0] = 0;
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
     * @param trie build table
     * @param manipulate fold implementation
     */
    private static final void fold(BuildTable trie, 
                                   Trie.DataManipulate manipulate) 
    {
        int leadIndexes[] = new int[SURROGATE_BLOCK_COUNT_];
        int index[] = trie.m_index_;
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
                int block = _findSameIndexBlock(index, indexLength, 
                                                c >> SHIFT_);
                // get a folded value for [c..c+0x400[ and, if 0, set it for 
                // the lead surrogate 
                int value = manipulate.getFoldingOffset(c);
                if (value != 0) {
                    if (!set32(trie, 0xd7c0 + (c >> 10), value)) {
                        // data table overflow 
                        throw new ArrayIndexOutOfBoundsException(
                                                        "Data table overflow");
                    }
                    // if we did not find an identical index block...
                    if (block == indexLength) {
                        // move the actual index (stage 1) entries from the 
                        // supplementary position to the new one
                        System.arraycopy(index, c >> SHIFT_, index, indexLength,
                                         SURROGATE_BLOCK_COUNT_ << 2);
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
        trie.m_indexLength_ = indexLength;
    }
    
    /**
     * Finds the same index block as the otherBlock
     * @param index array
     * @param indexLength size of index
     * @param otherBlock
     * @return same index block
     */
    private static final int _findSameIndexBlock(int index[], int indexLength,
                                                 int otherBlock) 
    {
        for (int block = BMP_INDEX_LENGTH_; block < indexLength; 
                                             block += SURROGATE_BLOCK_COUNT_) {
            int i = 0;
            for (; i < SURROGATE_BLOCK_COUNT_; ++ i) {
                if (index[block + i] != index[otherBlock + i]) {
                    break;
                }
            }
            if (i == SURROGATE_BLOCK_COUNT_) {
                return block;
            }
        }
        return indexLength;
    }
}