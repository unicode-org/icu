
/*
********************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
********************************************************************
*/

#ifndef _STDLIB_H
#include <stdlib.h>
#endif

#ifndef _STDIO_H
#include <stdio.h>
#endif


#include "ucmp8.h"
#include "cmemory.h"

static int32_t findOverlappingPosition(CompactByteArray* this,
                       uint32_t start, 
                       const UChar *tempIndex, 
                       int32_t tempIndexCount, 
                       uint32_t cycle);

/* internal constants*/

#define kUnicodeCount_int 65536
#define kBlockShift_int 7
#define kBlockCount_int (1<<kBlockShift_int)
#define kIndexShift_int (16-kBlockShift_int)
#define kIndexCount_int (1<<kIndexShift_int)
#define kBlockMask_int (kBlockCount_int-1)

const int32_t UCMP8_kUnicodeCount = kUnicodeCount_int;
const int32_t UCMP8_kBlockShift = kBlockShift_int;
const int32_t UCMP8_kBlockCount = kBlockCount_int;
const int32_t UCMP8_kIndexShift = kIndexShift_int;
const int32_t UCMP8_kIndexCount = kIndexCount_int;
const uint32_t UCMP8_kBlockMask = kBlockMask_int;


int32_t ucmp8_getkUnicodeCount() { return UCMP8_kUnicodeCount;}
int32_t ucmp8_getkBlockCount() { return UCMP8_kBlockCount;}
int32_t ucmp8_getkIndexCount(){ return UCMP8_kIndexCount;}
/* debug flags*/
/*=======================================================*/
U_CAPI int8_t ucmp8_get(CompactByteArray* array, uint16_t index) 
{
    return (array->fArray[(array->fIndex[index >> UCMP8_kBlockShift] & 0xFFFF) + (index & UCMP8_kBlockMask)]);
}
U_CAPI uint8_t ucmp8_getu(CompactByteArray* array, uint16_t index)
{
    return (uint8_t)ucmp8_get(array,index);
}

CompactByteArray* ucmp8_open(int8_t defaultValue)
{
/* set up the index array and the data array.
 * the index array always points into particular parts of the data array
 * it is initially set up to point at regular block boundaries
 * The following example uses blocks of 4 for simplicity
 * Example: Expanded
 * INDEX# 0   1   2   3   4
 * INDEX  0   4   8   12  16 ...
 * ARRAY  abcdeababcedzyabcdea...
 *        |   |   |   |   |   |...
 * whenever you set an element in the array, it unpacks to this state
 * After compression, the index will point to various places in the data array
 * wherever there is a runs of the same elements as in the original
 * Example: Compressed
 * INDEX# 0   1   2   3   4
 * INDEX  0   4   1   8   2 ...
 * ARRAY  abcdeabazyabc...
 * If you look at the example, index# 2 in the expanded version points
 * to data position number 8, which has elements "bced". In the compressed
 * version, index# 2 points to data position 1, which also has "bced"
 */
  CompactByteArray* this = (CompactByteArray*) icu_malloc(sizeof(CompactByteArray));
  int32_t i;
  
  if (this == NULL) return NULL;

  this->fArray = NULL; 
  this->fIndex = NULL;
  this->fCount = UCMP8_kUnicodeCount;
  this->fCompact = FALSE; 
  this->fBogus = FALSE;


  this->fArray = (int8_t*) icu_malloc(sizeof(int8_t) * UCMP8_kUnicodeCount);
  if (!this->fArray) 
    {
      this->fBogus = TRUE;
      return NULL;
    }
  this->fIndex = (uint16_t*) icu_malloc(sizeof(uint16_t) * UCMP8_kIndexCount);
  if (!this->fIndex) 
    {
      icu_free(this->fArray);
      this->fArray = NULL;
      this->fBogus = TRUE;
      return NULL;
    }
  for (i = 0; i < UCMP8_kUnicodeCount; ++i) 
    {
      this->fArray[i] = defaultValue;
    }
  for (i = 0; i < UCMP8_kIndexCount; ++i) 
    {
      this->fIndex[i] = (uint16_t)(i << UCMP8_kBlockShift);
    }

  return this;
}

CompactByteArray* ucmp8_openAdopt(uint16_t *indexArray,
                  int8_t *newValues,
                  int32_t count)
{
  CompactByteArray* this = (CompactByteArray*) icu_malloc(sizeof(CompactByteArray));
  if (!this) return NULL;
  
  this->fArray = NULL;
  this->fIndex = NULL; 
  this->fCount = count;
  this->fBogus = FALSE;
  
  this->fArray = newValues;
  this->fIndex = indexArray;
  this->fCompact = (count < UCMP8_kUnicodeCount) ? TRUE : FALSE;

  return this;
}

/*=======================================================*/
 
void ucmp8_close(CompactByteArray* this) 
{
  icu_free(this->fArray);
  this->fArray = NULL;
  icu_free(this->fIndex);
  this->fIndex = NULL;
  this->fCount = 0;
  this->fCompact = FALSE;
  icu_free(this);
}


/*=======================================================*/
 
void ucmp8_expand(CompactByteArray* this) 
{
  /* can optimize later.
   * if we have to expand, then walk through the blocks instead of using Get
   * this code unpacks the array by copying the blocks to the normalized position.
   * Example: Compressed
   * INDEX# 0   1   2   3   4
   * INDEX  0   4   1   8   2 ...
   * ARRAY  abcdeabazyabc...
   *  turns into
   * Example: Expanded
   * INDEX# 0   1   2   3   4
   * INDEX  0   4   8   12  16 ...
   * ARRAY  abcdeababcedzyabcdea...
   */
  int32_t i;
  if (this->fCompact) 
    {
      int8_t* tempArray;
      tempArray = (int8_t*) icu_malloc(sizeof(int8_t) * UCMP8_kUnicodeCount);
      if (!tempArray) 
    {
      this->fBogus = TRUE;
      return;
    }
      for (i = 0; i < UCMP8_kUnicodeCount; ++i) 
    {
      tempArray[i] = ucmp8_get(this,(UChar)i);  /* HSYS : How expand?*/
        }
      for (i = 0; i < UCMP8_kIndexCount; ++i) 
    {
      this->fIndex[i] = (uint16_t)(i<< UCMP8_kBlockShift);
        }
      icu_free(this->fArray);
      this->fArray = tempArray;
      this->fCompact = FALSE;
    }
}
 

/*=======================================================*/
/* this->fArray:    an array to be overlapped
 * start and count: specify the block to be overlapped
 * tempIndex:   the overlapped array (actually indices back into inputContents)
 * inputHash:   an index of hashes for tempIndex, where
 *      inputHash[i] = XOR of values from i-count+1 to i
 */
int32_t
findOverlappingPosition(CompactByteArray* this, 
            uint32_t start,
            const UChar* tempIndex,
            int32_t tempIndexCount,
            uint32_t cycle) 
{
  /* this is a utility routine for finding blocks that overlap.
   * IMPORTANT: the cycle number is very important. Small cycles take a lot
   * longer to work. In some cases, they may be able to get better compaction.
   */
    
  int32_t i;
  int32_t j;
  int32_t currentCount;
  
  for (i = 0; i < tempIndexCount; i += cycle) 
    {
      currentCount = UCMP8_kBlockCount;
      if (i + UCMP8_kBlockCount > tempIndexCount) 
    {
      currentCount = tempIndexCount - i;
        } 
      for (j = 0; j < currentCount; ++j) 
    {
      if (this->fArray[start + j] != this->fArray[tempIndex[i + j]]) break;
        }
      if (j == currentCount) break;
    }
  
  return i;
}

bool_t
ucmp8_isBogus(const CompactByteArray* this)
{
  return this->fBogus;
}

const int8_t*
ucmp8_getArray(const CompactByteArray* this)
{
  return this->fArray;
}

const uint16_t*
ucmp8_getIndex(const CompactByteArray* this)
{
  return this->fIndex;
}

int32_t 
ucmp8_getCount(const CompactByteArray* this)
{
  return this->fCount;
}


void 
ucmp8_set(CompactByteArray* this,
      UChar c,
      int8_t value)
{
  if (this->fCompact == TRUE) 
    {
      ucmp8_expand(this);
      if (this->fBogus) return;
    }
  this->fArray[(int32_t)c] = value;
}


void 
ucmp8_setRange(CompactByteArray* this,
           UChar start,
           UChar end,
           int8_t value)
{
  int32_t i;
  if (this->fCompact == TRUE) 
    {
      ucmp8_expand(this);
      if (this->fBogus) return;
    }
  for (i = start; i <= end; ++i) 
    {
      this->fArray[i] = value;
    }
}


/*=======================================================*/
 
void 
ucmp8_compact(CompactByteArray* this,
          uint32_t cycle) 
{
  if (!this->fCompact) 
    {
      /* this actually does the compaction.
       * it walks throught the contents of the expanded array, finding the
       * first block in the data that matches the contents of the current index.
       * As it works, it keeps an updated pointer to the last position,
       * so that it knows how big to make the final array
       * If the matching succeeds, then the index will point into the data
       * at some earlier position.
       * If the matching fails, then last position pointer will be bumped,
       * and the index will point to that last block of data.
       */
      UChar*    tempIndex;
      int32_t     tempIndexCount;
      int8_t*     tempArray;
      int32_t     iBlock, iIndex;
      
      /* fix cycle, must be 0 < cycle <= blockcount*/
      if (cycle < 0) cycle = 1;
      else if (cycle > (uint32_t)UCMP8_kBlockCount) cycle = UCMP8_kBlockCount;
      
      /* make temp storage, larger than we need*/
      tempIndex = (UChar*) icu_malloc(sizeof(UChar)* UCMP8_kUnicodeCount);
      if (!tempIndex) 
    {
      this->fBogus = TRUE;
      return;
        }               
      /* set up first block.*/
      tempIndexCount = UCMP8_kBlockCount;
      for (iIndex = 0; iIndex < UCMP8_kBlockCount; ++iIndex) 
    {
      tempIndex[iIndex] = (uint16_t)iIndex;
        }; /* endfor (iIndex = 0; .....)*/
      this->fIndex[0] = 0;
      
      /* for each successive block, find out its first position in the compacted array*/
      for (iBlock = 1; iBlock < UCMP8_kIndexCount; ++iBlock) 
    {
      int32_t newCount, firstPosition, block;
      block = iBlock << UCMP8_kBlockShift;
      /*      if (debugSmall) if (block > debugSmallLimit) break;*/
      firstPosition = findOverlappingPosition(this, 
                          block,
                          tempIndex,
                          tempIndexCount,
                          cycle);
      
      /* if not contained in the current list, copy the remainder
       * invariant; cumulativeHash[iBlock] = XOR of values from iBlock-kBlockCount+1 to iBlock
       * we do this by XORing out cumulativeHash[iBlock-kBlockCount]
       */
      newCount = firstPosition + UCMP8_kBlockCount;
      if (newCount > tempIndexCount) 
        {
          for (iIndex = tempIndexCount; iIndex < newCount; ++iIndex) 
        {
          tempIndex[iIndex] = (uint16_t)(iIndex - firstPosition + block);
        } /* endfor (iIndex = tempIndexCount....)*/
                tempIndexCount = newCount;
            } /* endif (newCount > tempIndexCount)*/
      this->fIndex[iBlock] = (uint16_t)firstPosition;
        } /* endfor (iBlock = 1.....)*/
      
      /* now allocate and copy the items into the array*/
      tempArray = (int8_t*) icu_malloc(tempIndexCount * sizeof(int8_t));
      if (!tempArray) 
    {
      this->fBogus = TRUE;
      icu_free(tempIndex);
      return;
        }
      for (iIndex = 0; iIndex < tempIndexCount; ++iIndex) 
    {
      tempArray[iIndex] = this->fArray[tempIndex[iIndex]];
        }
      icu_free(this->fArray);
      this->fArray = tempArray;
      this->fCount = tempIndexCount;
      
      
      /* free up temp storage*/
      icu_free(tempIndex);
      this->fCompact = TRUE;
    } /* endif (!this->fCompact)*/
}



