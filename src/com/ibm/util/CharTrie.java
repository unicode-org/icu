/*
******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/util/Attic/CharTrie.java,v $ 
* $Date: 2001/08/22 23:41:28 $ 
* $Revision: 1.2 $
*
******************************************************************************
*/

package com.ibm.util;

import com.ibm.text.UCharacter;

/**
* Class to manipulate and generate a trie.
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
* A trie is composed of 2 index array and value array. Combining the 2 index
* array, we could get the indicies of Unicode characters to the value array.
* The first index array will contain indexes corresponding to the first 11 
* bits of a 21 bit codepoint, the second index array will contain indexes 
* corresponding to the next 6 bits of the code point. The last array will 
* contain the values. Hence to access the value of a codepoint, we can use the
* following program
* <p>
*   int firstindex = ch >> FIRST_11_BITS_SHIFT;<br>
*   int secondindex = index1[firstindex] + 
*                       (ch >> NEXT_6_BITS_SHIFT) & NEXT_6_BITS_MASK;<br>
*   int thirdindex = index2[secondindex] + ch & LAST_FOUR_BITS_MASK;<br>    
*   f(ch) = value[thirdindex];<br>
* </p>
* @version            $Revision: 1.2 $
* @author             Syn Wee Quek
*/
public final class CharTrie
{
  // constructors -----------------------------------------------------
  
  /**
  * constructor 
  * @param array of data to be populated into trie
  */
  public CharTrie(char array[])
  {
    build(array);
  }
  
  /**
  * constructor that assigns trie the argument values. Arrays are not 
  * duplicated.
  * @param stage1 array of the first set of indexes
  * @param stage2 array of the second set of indexes
  * @param stage3 array of data
  */
  public CharTrie(int stage1[], int stage2[], char stage3[])
  {
    m_stage1_ = stage1;
    m_stage2_ = stage2;
    m_stage3_ = stage3;
  }
  
  // public methods ----------------------------------------------------
  
  /**
  * Getting the trie data corresponding to the argument index.
  * @param index to be manipulated into corresponding trie index
  * @return trie value at index
  */
  public char getValue(int index)
  {
    // index of the first access to the database 
    int index1 = index >> STAGE_1_SHIFT_;
    // index of the second access to the database
    int index2 = m_stage1_[index1] + 
                 ((index >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_);
    // index of the third access to the database
    int index3 = m_stage2_[index2] + (index & STAGE_3_MASK_);  
    // retrieves value
    return m_stage3_[index3];
  }
  
  // private data members ------------------------------------------------
  
  /**
  * Stage 1 index array
  */
  private int m_stage1_[];
  
  /**
  * Stage 2 index array
  */
  private int m_stage2_[];
  
  /**
  * Stage 3 value array
  */
  private char m_stage3_[];
  
  /**
  * Stage 1 shift
  */
  private static final int STAGE_1_SHIFT_ = 10;
  
  /**
  * Stage 2 shift
  */
  private static final int STAGE_2_SHIFT_ = 4;
  
  /**
  * Stage 2 mask
  */
  private static final int STAGE_2_MASK_AFTER_SHIFT_ = 0x3F;
  
  /**
  * Stage 3 mask
  */
  private static final int STAGE_3_MASK_ = 0xF;
  
  /**
  * Number of numbers possible from a 4 bit type
  */
  private static final int COUNT_4_BIT_ = 0x10;
  
  /**
  * Number of numbers possible from a 6 bit type
  */
  private static final int COUNT_6_BIT_ = 0x40;
  
  /**
  * Number of numbers possible from the first 17 bits of a codepoint
  */
  private static final int COUNT_CODEPOINT_FIRST_17_BIT_ = 0x110000 >> 4;
  
  /**
  * Number of numbers possible from the first 11 bits of a codepoint
  */
  private static final int COUNT_CODEPOINT_FIRST_11_BIT_ = 0x110000 >> 10;
  
  // private methods -----------------------------------------------------
  
  /**
  * Building the trie from a argument array.
  * Each unicode character will be used to generate data.
  * @param output file path
  */
  private void build(char array[])
  {
    int stage2[] = new int[COUNT_CODEPOINT_FIRST_17_BIT_];
    char stage3[] = new char[UCharacter.MAX_VALUE >> 1];
    int size = TrieBuilder.build(array, 0, array.length, COUNT_4_BIT_, stage2, 
                                 stage3);
    
    m_stage3_ = new char[size];
    System.arraycopy(stage3, 0, m_stage3_, 0, size);
    
    m_stage1_ = new int[COUNT_CODEPOINT_FIRST_11_BIT_];
    size = TrieBuilder.build(stage2, 0, stage2.length, COUNT_6_BIT_, m_stage1_, 
                             stage2);
    m_stage2_ = new int[size];
    System.arraycopy(stage2, 0, m_stage2_, 0, size);
  }
  
  /**
  * Converts trie to a readable format
  * @return string version of the trie
  */
  public String toString()
  {
    int size = m_stage1_.length;
    int count = 0;
    StringBuffer result = new StringBuffer("int m_stage1_[] = {\n");
    for (int i = 0; i < size; i ++) {
      result.append("0x" + Integer.toHexString(m_stage1_[i]));
      if (i != size - 1) {
        result.append(", ");
      }
      count ++;
      if (count == 10) {
        count = 0;
        result.append("\n");
      }
    }
    result.append("\n}\n\n");
    size = m_stage2_.length;
    result.append("int m_stage2_[] = {\n");
    count = 0;
    for (int i = 0; i < size; i ++) {
      result.append("0x" + Integer.toHexString(m_stage2_[i]));
      if (i != size - 1) {
        result.append(", ");
      }
      count ++;
      if (count == 10) {
        count = 0;
        result.append("\n");
      }
    }
    result.append("\n}\n\n");
    size = m_stage3_.length;
    result.append("char m_stage3_[] = {\n");
    count = 0;
    for (int i = 0; i < size; i ++) {
      result.append("0x" + Integer.toHexString(m_stage3_[i]));
      if (i != size - 1) {
        result.append(", ");
      }
      count ++;
      if (count == 10) {
        count = 0;
        result.append("\n");
      }
    }
    result.append("\n}");
    return result.toString();
  }
}