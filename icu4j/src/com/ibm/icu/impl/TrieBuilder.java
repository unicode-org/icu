/*
******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/TrieBuilder.java,v $ 
* $Date: 2001/03/28 00:01:51 $ 
* $Revision: 1.2 $
*
******************************************************************************
*/

package com.ibm.util;

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
* A trie is composed of 2 index array and value array. Combining the 2 index
* array, we could get the indicies of Unicode characters to the value array.
* The first index array will contain indexes corresponding to the first 11 
* bits of a 21 bit codepoint, the second index array will contain indexes 
* corresponding to the next 6 bits of the code point. The last array will 
* contain the values. Hence to access the value of a codepoint, we can use the 
* following program.
* <p>
*   int firstindex = ch >> FIRST_SET_OF_BITS_SHIFT;<br>
*   int secondindex = index1[firstindex] + 
*                  (ch >> NEXT_SET_OF_BITS_SHIFT) & NEXT_SET_OF_BITS_MASK;<br>
*   int thirdindex = index2[secondindex] + ch & LAST_SET_OF_BITS_MASK;<br>    
*   f(ch) = value[thirdindex];<br>
* </p>
* @version            $Revision: 1.2 $
* @author             Syn Wee Quek
*/
final class TrieBuilder
{
  // public methods ----------------------------------------------------
  
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
}