/*============================================================================
 * Copyright (C) 1997-1999, International Business Machines Corporation
 * and others. All Rights Reserved.
 *
 * File cmpshrta.cpp
 *
 * Modification History:
 *
 *  Date        Name        Description
 *  2/5/97      aliu        Added CompactIntArray streamIn and streamOut methods.
 *  3/4/97      aliu        Tuned performance of CompactIntArray constructor,
 *                          based on performance data indicating that this_obj was slow.
 * 05/07/97     helena      Added isBogus()
 * 04/26/99     Madhu       Ported to C for C Implementation
 * 11/12/99     srl         macroized ucmp32_get()
 *===============================================================================
 */
#include "ucmp32.h"
#include "cmemory.h"
#include "filestrm.h"
#include <stdlib.h>



static int32_t ucmp32_findOverlappingPosition(CompactIntArray* this_obj, uint32_t start, 
                                const UChar *tempIndex, 
                                int32_t tempIndexCount, 
                                uint32_t cycle);
      
static  UBool debugSmall = FALSE;
static  uint32_t debugSmallLimit = 30000;

/** debug flags
  *=======================================================
  */

int32_t ucmp32_getkUnicodeCount() { return UCMP32_kUnicodeCount;}
int32_t ucmp32_getkBlockCount() { return UCMP32_kBlockCount;}

U_CAPI void ucmp32_streamIn(CompactIntArray* this_obj, FileStream* is)
{
int32_t  newCount, len;
char c;
    if (!T_FileStream_error(is))
    {
        
        T_FileStream_read(is, &newCount, sizeof(newCount));
        if (this_obj->fCount != newCount)
        {
            this_obj->fCount = newCount;
            uprv_free(this_obj->fArray);
            this_obj->fArray = 0;
            this_obj->fArray = (int32_t*)uprv_malloc(this_obj->fCount * sizeof(int32_t));
            if (!this_obj->fArray) {
                this_obj->fBogus = TRUE;
                return;
            }
        }
        T_FileStream_read(is, this_obj->fArray, sizeof(*(this_obj->fArray)) * this_obj->fCount);
        T_FileStream_read(is, &len, sizeof(len));
        if (len == 0)
        {
            uprv_free(this_obj->fIndex);
            this_obj->fIndex = 0;
        }
        else if (len == UCMP32_kIndexCount)
        {
            if (this_obj->fIndex == 0) 
                this_obj->fIndex =(uint16_t*)uprv_malloc(UCMP32_kIndexCount * sizeof(uint16_t));
            if (!this_obj->fIndex) {
                this_obj->fBogus = TRUE;
                uprv_free(this_obj->fArray); 
                this_obj->fArray = 0;
                return;
            }
            T_FileStream_read(is, this_obj->fIndex, sizeof(*(this_obj->fIndex)) * UCMP32_kIndexCount);
        }
        else
        {
            this_obj->fBogus = TRUE;
            return;
        }
        /* char instead of int8_t for Mac compilation*/
        T_FileStream_read(is, (char*)&c, sizeof(c));
        this_obj->fCompact = (c != 0);
    }
}

U_CAPI void ucmp32_streamOut(CompactIntArray* this_obj, FileStream* os)
{
    char c;
if (!T_FileStream_error(os))
    {
        if (this_obj->fCount != 0 && this_obj->fArray != 0)
        {
            T_FileStream_write(os, &(this_obj->fCount), sizeof(this_obj->fCount));
            T_FileStream_write(os, this_obj->fArray, sizeof(*(this_obj->fArray)) * this_obj->fCount);
        }
        else
        {
            int32_t  zero = 0;
            T_FileStream_write(os, &zero, sizeof(zero));
        }

        if (this_obj->fIndex == 0)
        {
            int32_t len = 0;
            T_FileStream_write(os, &len, sizeof(len));
        }
        else
        {
            int32_t len = UCMP32_kIndexCount;
            T_FileStream_write(os, &len, sizeof(len));
            T_FileStream_write(os, this_obj->fIndex, sizeof(*(this_obj->fIndex)) * UCMP32_kIndexCount);
        }
        c = this_obj->fCompact ? 1 : 0;  /* char instead of int8_t for Mac compilation*/
        T_FileStream_write(os, (const char*)&c, sizeof(c));
    }
}

CompactIntArray* ucmp32_open(int32_t defaultValue)
{
  uint16_t  i;
  int32_t *p, *p_end;
  uint16_t *q, *q_end;
  CompactIntArray* this_obj = (CompactIntArray*) uprv_malloc(sizeof(CompactIntArray));
  if (this_obj == NULL) return NULL;
  
  this_obj->fCount = UCMP32_kUnicodeCount;
  this_obj->fCompact = FALSE; 
  this_obj->fBogus = FALSE;
  this_obj->fArray = NULL;
  this_obj->fIndex = NULL;
  
/*set up the index array and the data array.
 * the index array always points into particular parts of the data array
 * it is initially set up to point at regular block boundaries
 * The following example uses blocks of 4 for simplicity
 * Example: Expanded
 * INDEX# 0   1   2   3   4
 * INDEX  0   4   8   12  16 ...
 * ARRAY  abcdeababcedzyabcdea...
 *        |   |   |   |   |   |...
 * whenever you set an element in the array, it unpacks to this_obj state
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
 this_obj->fArray = (int32_t*)uprv_malloc(UCMP32_kUnicodeCount * sizeof(int32_t));
      if (this_obj->fArray == NULL)    {
      this_obj->fBogus = TRUE;
      return NULL;
    }
  
    this_obj->fIndex = (uint16_t*)uprv_malloc(UCMP32_kIndexCount * sizeof(uint16_t));
    if (!this_obj->fIndex) {
        uprv_free(this_obj->fArray); 
        this_obj->fArray = NULL;
        this_obj->fBogus = TRUE;
        return NULL;
    }
    p = this_obj->fArray;
    p_end = p + UCMP32_kUnicodeCount;
    while (p < p_end) *p++ = defaultValue;

    q = this_obj->fIndex;
    q_end = q + UCMP32_kIndexCount;
    i = 0;
    while (q < q_end)
    {
        *q++ = i;
        i += (1 << UCMP32_kBlockShift);
    }
    return this_obj;
}

CompactIntArray* ucmp32_openAdopt(uint16_t *indexArray, int32_t *newValues, int32_t  count)
{
  CompactIntArray* this_obj = (CompactIntArray*) uprv_malloc(sizeof(CompactIntArray));
  if (this_obj == NULL) return NULL;
  this_obj->fCount = count; 
  this_obj->fBogus = FALSE;
  this_obj->fArray = newValues;
  this_obj->fIndex = indexArray;
  this_obj->fCompact = (count < UCMP32_kUnicodeCount) ? TRUE : FALSE;
  return this_obj;
}

/*=======================================================*/
 
void ucmp32_close(    CompactIntArray* this_obj) 
{
  if(this_obj != NULL) {
    if(this_obj->fArray != NULL) {
      uprv_free(this_obj->fArray);
    }
    if(this_obj->fIndex != NULL) {
      uprv_free(this_obj->fIndex);
    }
    uprv_free(this_obj);
  }
}

UBool ucmp32_isBogus(const CompactIntArray* this_obj)
{
    return this_obj == NULL || this_obj->fBogus;
}

void ucmp32_expand(CompactIntArray* this_obj) {
/* can optimize later.
 * if we have to expand, then walk through the blocks instead of using Get
 * this_obj code unpacks the array by copying the blocks to the normalized position.
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
    int32_t* tempArray;
    if (this_obj->fCompact) {
        tempArray = (int32_t*)uprv_malloc(UCMP32_kUnicodeCount * sizeof(int32_t));
        if (tempArray == NULL) {
            this_obj->fBogus = TRUE;
            return;
        }
        for (i = 0; i < UCMP32_kUnicodeCount; ++i) {
            tempArray[i] = ucmp32_get(this_obj, (UChar)i);  /* HSYS : How expand?*/
        }
        for (i = 0; i < UCMP32_kIndexCount; ++i) {
            this_obj->fIndex[i] = (uint16_t)(i<<UCMP32_kBlockShift);
        }
        uprv_free(this_obj->fArray); 
        this_obj->fArray = tempArray;
        this_obj->fCompact = FALSE;
    }
}

uint32_t ucmp32_getCount(const CompactIntArray* this_obj)
{
    return this_obj->fCount;
}

const int32_t* ucmp32_getArray(const CompactIntArray* this_obj)
{
    return this_obj->fArray;
}

const uint16_t* ucmp32_getIndex(const CompactIntArray* this_obj)
{
    return this_obj->fIndex;
}

void  ucmp32_set(CompactIntArray* this_obj, UChar c, int32_t value)
{
    if (this_obj->fCompact == TRUE) {
        ucmp32_expand(this_obj);
        if (this_obj->fBogus) return;
    }
    this_obj->fArray[(int32_t)c] = value;
}


void ucmp32_setRange(CompactIntArray* this_obj, UChar start, UChar end, int32_t value)
{
    int32_t i;
    if (this_obj->fCompact == TRUE) {
        ucmp32_expand(this_obj);
        if (this_obj->fBogus) return;

    }
    for (i = start; i <= end; ++i) {
        this_obj->fArray[i] = value;
    }
}
/*=======================================================
 * this_obj->fArray:    an array to be overlapped
 * start and count: specify the block to be overlapped
 * tempIndex:   the overlapped array (actually indices back into inputContents)
 * inputHash:   an index of hashes for tempIndex, where
 *      inputHash[i] = XOR of values from i-count+1 to i
 */
 
int32_t ucmp32_findOverlappingPosition(CompactIntArray* this_obj, 
                    uint32_t  start,
                    const UChar* tempIndex,
                    int32_t  tempIndexCount,
                    uint32_t  cycle) {
/* this_obj is a utility routine for finding blocks that overlap.
 * IMPORTANT: the cycle number is very important. Small cycles take a lot
 * longer to work. In some cases, they may be able to get better compaction.
 */
    int32_t  i;
    int32_t  j;
    int32_t  currentCount;
        

    for (i = 0; i < tempIndexCount; i += cycle) {
        currentCount = UCMP32_kBlockCount;
        if (i + UCMP32_kBlockCount > tempIndexCount) {
            currentCount = tempIndexCount - i;
        } 
        for (j = 0; j < currentCount; ++j) {
            if (this_obj->fArray[start + j] != this_obj->fArray[tempIndex[i + j]]) break;
        }
        if (j == currentCount) break;
    }

    return i;
}
/*=======================================================*/
 
void ucmp32_compact(CompactIntArray* this_obj, int32_t cycle) {
/* this_obj actually does the compaction.
 * it walks throught the contents of the expanded array, finding the
 * first block in the data that matches the contents of the current index.
 * As it works, it keeps an updated pointer to the last position,
 * so that it knows how big to make the final array
 * If the matching succeeds, then the index will point into the data
 * at some earlier position.
 * If the matching fails, then last position pointer will be bumped,
 * and the index will point to that last block of data.
 */
   UChar* tempIndex;
   int32_t  tempIndexCount;
   int32_t* tempArray;
   int32_t  iBlock, iIndex;
   int32_t newCount, firstPosition;
   uint32_t block;
    if (!this_obj->fCompact) {
                
        /* fix cycle, must be 0 < cycle <= blockcount*/
        if (cycle < 0) cycle = 1;
        else if (cycle > UCMP32_kBlockCount)
            cycle = UCMP32_kBlockCount;

        /* make temp storage, larger than we need*/
        tempIndex =(UChar*)uprv_malloc(UCMP32_kUnicodeCount * sizeof(uint32_t));
        if (tempIndex == NULL) {
            this_obj->fBogus = TRUE;
            return;
        }               
        /* set up first block.*/
        tempIndexCount = UCMP32_kBlockCount;
        for (iIndex = 0; iIndex < UCMP32_kBlockCount; ++iIndex) {
            tempIndex[iIndex] = (uint16_t)iIndex;
        }; /* endfor (iIndex = 0; .....)*/
        this_obj->fIndex[0] = 0;
    
    /* for each successive block, find out its first position in the compacted array*/
        for (iBlock = 1; iBlock < UCMP32_kIndexCount; ++iBlock) {
            
            block = iBlock<<UCMP32_kBlockShift;
            if (debugSmall) if (block > debugSmallLimit) break;
            firstPosition = ucmp32_findOverlappingPosition(this_obj, block, tempIndex, tempIndexCount, cycle);
            
        /* if not contained in the current list, copy the remainder
         * invariant; cumulativeHash[iBlock] = XOR of values from iBlock-kBlockCount+1 to iBlock
         * we do this_obj by XORing out cumulativeHash[iBlock-kBlockCount]
         */
            newCount = firstPosition + UCMP32_kBlockCount;
            if (newCount > tempIndexCount) {
                for (iIndex = tempIndexCount; iIndex < newCount; ++iIndex) {
                    tempIndex[iIndex] = (uint16_t)(iIndex - firstPosition + block);
                } /* endfor (iIndex = tempIndexCount....)*/
                tempIndexCount = newCount;
            } /*endif (newCount > tempIndexCount)*/
            this_obj->fIndex[iBlock] = (uint16_t)firstPosition;
        } /* endfor (iBlock = 1.....)*/
        

        
    /* now allocate and copy the items into the array*/
        tempArray = (int32_t*)uprv_malloc(tempIndexCount * sizeof(uint32_t));
        if (tempArray == NULL) {
            this_obj->fBogus = TRUE;
            uprv_free(tempIndex);
            return;
        }
        for (iIndex = 0; iIndex < tempIndexCount; ++iIndex) {
            tempArray[iIndex] = this_obj->fArray[tempIndex[iIndex]];
        }
        uprv_free(this_obj->fArray);
        this_obj->fArray = tempArray;
        this_obj->fCount = tempIndexCount;
    

    
    /* free up temp storage*/
        uprv_free(tempIndex);
        this_obj->fCompact = TRUE;

#ifdef _DEBUG
        /*the following line is useful for specific debugging purposes*/
        /*fprintf(stderr, "Compacted to %ld with cycle %d\n", fCount, cycle);*/
#endif
    } /* endif (!this_obj->fCompact)*/
}

