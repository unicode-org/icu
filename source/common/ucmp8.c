/*
********************************************************************
* COPYRIGHT:
* Copyright (c) 1997-2003, International Business Machines Corporation and
* others. All Rights Reserved.
********************************************************************
*/

#include "ucmp8.h"
#include "cmemory.h"

/* internal constants*/


U_CAPI int32_t U_EXPORT2
ucmp8_getkUnicodeCount() { return UCMP8_kUnicodeCount;}

U_CAPI int32_t U_EXPORT2
ucmp8_getkBlockCount() { return UCMP8_kBlockCount;}

U_CAPI void U_EXPORT2
ucmp8_initBogus(CompactByteArray* array)
{
    CompactByteArray* this_obj = array;

    if (this_obj == NULL) return;

    this_obj->fStructSize = sizeof(CompactByteArray);
    this_obj->fArray = NULL;
    this_obj->fIndex = NULL;
    this_obj->fCount = UCMP8_kUnicodeCount;
    this_obj->fCompact = FALSE;
    this_obj->fBogus = TRUE;
    this_obj->fAlias = FALSE;
    this_obj->fIAmOwned = TRUE;
}

/* debug flags*/
/*=======================================================*/
U_CAPI void U_EXPORT2
ucmp8_init(CompactByteArray* array, int8_t defaultValue)
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
    CompactByteArray* this_obj = array;
    int32_t i;

    if (this_obj == NULL) return;

    this_obj->fStructSize = sizeof(CompactByteArray);
    this_obj->fArray = NULL;
    this_obj->fIndex = NULL;
    this_obj->fCount = UCMP8_kUnicodeCount;
    this_obj->fCompact = FALSE;
    this_obj->fBogus = FALSE;
    this_obj->fAlias = FALSE;
    this_obj->fIAmOwned = TRUE;


    this_obj->fArray = (int8_t*) uprv_malloc(sizeof(int8_t) * UCMP8_kUnicodeCount);
    if (!this_obj->fArray)
    {
        this_obj->fBogus = TRUE;
        return;
    }
    this_obj->fIndex = (uint16_t*) uprv_malloc(sizeof(uint16_t) * UCMP8_kIndexCount);
    if (!this_obj->fIndex)
    {
        uprv_free(this_obj->fArray);
        this_obj->fArray = NULL;
        this_obj->fBogus = TRUE;
        return;
    }
    for (i = 0; i < UCMP8_kUnicodeCount; ++i)
    {
        this_obj->fArray[i] = defaultValue;
    }
    for (i = 0; i < UCMP8_kIndexCount; ++i)
    {
        this_obj->fIndex[i] = (uint16_t)(i << UCMP8_kBlockShift);
    }
}

U_CAPI CompactByteArray* U_EXPORT2
ucmp8_open(int8_t defaultValue)
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
    CompactByteArray* this_obj = (CompactByteArray*) uprv_malloc(sizeof(CompactByteArray));
    int32_t i;

    if (this_obj == NULL) return NULL;

    this_obj->fStructSize = sizeof(CompactByteArray);
    this_obj->fArray = NULL;
    this_obj->fIndex = NULL;
    this_obj->fCount = UCMP8_kUnicodeCount;
    this_obj->fCompact = FALSE;
    this_obj->fBogus = FALSE;
    this_obj->fAlias = FALSE;
    this_obj->fIAmOwned = FALSE;


    this_obj->fArray = (int8_t*) uprv_malloc(sizeof(int8_t) * UCMP8_kUnicodeCount);
    if (!this_obj->fArray)
    {
        this_obj->fBogus = TRUE;
        return NULL;
    }
    this_obj->fIndex = (uint16_t*) uprv_malloc(sizeof(uint16_t) * UCMP8_kIndexCount);
    if (!this_obj->fIndex)
    {
        uprv_free(this_obj->fArray);
        this_obj->fArray = NULL;
        this_obj->fBogus = TRUE;
        return NULL;
    }
    for (i = 0; i < UCMP8_kUnicodeCount; ++i)
    {
        this_obj->fArray[i] = defaultValue;
    }
    for (i = 0; i < UCMP8_kIndexCount; ++i)
    {
        this_obj->fIndex[i] = (uint16_t)(i << UCMP8_kBlockShift);
    }

    return this_obj;
}

U_CAPI CompactByteArray* U_EXPORT2
ucmp8_openAdopt(uint16_t *indexArray,
                  int8_t *newValues,
                  int32_t count)
{
    CompactByteArray* this_obj = (CompactByteArray*) uprv_malloc(sizeof(CompactByteArray));
    /* test for NULL */
    if(this_obj == NULL)
        return NULL;
    ucmp8_initAdopt(this_obj, indexArray, newValues, count);
    this_obj->fIAmOwned = FALSE;
    return this_obj;
}

U_CAPI CompactByteArray* U_EXPORT2
ucmp8_openAlias(uint16_t *indexArray,
                  int8_t *newValues,
                  int32_t count)
{
    CompactByteArray* this_obj = (CompactByteArray*) uprv_malloc(sizeof(CompactByteArray));
    /* test for NULL */
    if(this_obj == NULL)
        return NULL;
    ucmp8_initAlias(this_obj, indexArray, newValues, count);
    this_obj->fIAmOwned = FALSE;
    return this_obj;
}

/*=======================================================*/

U_CAPI CompactByteArray* U_EXPORT2
ucmp8_initAdopt(CompactByteArray *this_obj,
                  uint16_t *indexArray,
                  int8_t *newValues,
                  int32_t count)
{
    if (this_obj) {
        this_obj->fCount = count;
        this_obj->fBogus = FALSE;
        this_obj->fStructSize = sizeof(CompactByteArray);

        this_obj->fArray = newValues;
        this_obj->fIndex = indexArray;
        this_obj->fCompact = (UBool)((count < UCMP8_kUnicodeCount) ? TRUE : FALSE);
        this_obj->fAlias = FALSE;
        this_obj->fIAmOwned = TRUE;
    }

    return this_obj;
}

U_CAPI CompactByteArray* U_EXPORT2
ucmp8_initAlias(CompactByteArray *this_obj,
                  uint16_t *indexArray,
                  int8_t *newValues,
                  int32_t count)
{
    if (this_obj) {
        this_obj->fArray = NULL;
        this_obj->fIndex = NULL;
        this_obj->fCount = count;
        this_obj->fBogus = FALSE;
        this_obj->fStructSize = sizeof(CompactByteArray);

        this_obj->fArray = newValues;
        this_obj->fIndex = indexArray;
        this_obj->fCompact = (UBool)((count < UCMP8_kUnicodeCount) ? TRUE : FALSE);
        this_obj->fAlias = TRUE;
        this_obj->fIAmOwned = TRUE;
    }

    return this_obj;
}

/*=======================================================*/

U_CAPI void U_EXPORT2
ucmp8_close(CompactByteArray* this_obj)
{
    if(this_obj != NULL) {
        if(!this_obj->fAlias) {
            if(this_obj->fArray != NULL) {
                uprv_free(this_obj->fArray);
            }
            if(this_obj->fIndex != NULL) {
                uprv_free(this_obj->fIndex);
            }
        }
        if(!this_obj->fIAmOwned) /* Called if 'init' was called instead of 'open'. */
        {
            uprv_free(this_obj);
        }
    }
}


/*=======================================================*/

U_CAPI void U_EXPORT2
ucmp8_expand(CompactByteArray* this_obj)
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
    if (this_obj->fCompact)
    {
      int8_t* tempArray;
      tempArray = (int8_t*) uprv_malloc(sizeof(int8_t) * UCMP8_kUnicodeCount);
      if (!tempArray)
      {
          this_obj->fBogus = TRUE;
          return;
      }
      for (i = 0; i < UCMP8_kUnicodeCount; ++i)
      {
          tempArray[i] = ucmp8_get(this_obj,(UChar)i);  /* HSYS : How expand?*/
      }
      for (i = 0; i < UCMP8_kIndexCount; ++i)
      {
          this_obj->fIndex[i] = (uint16_t)(i<< UCMP8_kBlockShift);
      }
      uprv_free(this_obj->fArray);
      this_obj->fArray = tempArray;
      this_obj->fCompact = FALSE;
      this_obj->fAlias = FALSE;

    }
}


/*=======================================================*/
/* this_obj->fArray:    an array to be overlapped
 * start and count: specify the block to be overlapped
 * tempIndex:   the overlapped array (actually indices back into inputContents)
 * inputHash:   an index of hashes for tempIndex, where
 *      inputHash[i] = XOR of values from i-count+1 to i
 */
static int32_t
findOverlappingPosition(CompactByteArray* this_obj,
            uint32_t start,
            const UChar* tempIndex,
            int32_t tempIndexCount,
            uint32_t cycle)
{
    /* this_obj is a utility routine for finding blocks that overlap.
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
            if (this_obj->fArray[start + j] != this_obj->fArray[tempIndex[i + j]])
                break;
        }
        if (j == currentCount)
            break;
    }

    return i;
}

U_CAPI UBool U_EXPORT2
ucmp8_isBogus(const CompactByteArray* this_obj)
{
    return (UBool)(this_obj == NULL || this_obj->fBogus);
}

U_CAPI const int8_t* U_EXPORT2
ucmp8_getArray(const CompactByteArray* this_obj)
{
    return this_obj->fArray;
}

U_CAPI const uint16_t* U_EXPORT2
ucmp8_getIndex(const CompactByteArray* this_obj)
{
    return this_obj->fIndex;
}

U_CAPI int32_t U_EXPORT2
ucmp8_getCount(const CompactByteArray* this_obj)
{
    return this_obj->fCount;
}


U_CAPI void U_EXPORT2
ucmp8_set(CompactByteArray* this_obj,
      UChar c,
      int8_t value)
{
    if (this_obj->fCompact == TRUE)
    {
        ucmp8_expand(this_obj);
        if (this_obj->fBogus) return;
    }
    this_obj->fArray[(int32_t)c] = value;
}


U_CAPI void U_EXPORT2
ucmp8_setRange(CompactByteArray* this_obj,
           UChar start,
           UChar end,
           int8_t value)
{
    int32_t i;
    if (this_obj->fCompact == TRUE)
    {
        ucmp8_expand(this_obj);
        if (this_obj->fBogus)
            return;
    }
    for (i = start; i <= end; ++i)
    {
        this_obj->fArray[i] = value;
    }
}


/*=======================================================*/

U_CAPI void U_EXPORT2
ucmp8_compact(CompactByteArray* this_obj,
          uint32_t cycle)
{
    if (!this_obj->fCompact)
    {
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
        UChar*    tempIndex;
        int32_t     tempIndexCount;
        int8_t*     tempArray;
        int32_t     iBlock, iIndex;

        /* fix cycle, must be 0 < cycle <= blockcount*/
        if (cycle <= 0)
            cycle = 1;
        else if (cycle > (uint32_t)UCMP8_kBlockCount)
            cycle = UCMP8_kBlockCount;

        /* make temp storage, larger than we need*/
        tempIndex = (UChar*) uprv_malloc(sizeof(UChar)* UCMP8_kUnicodeCount);
        if (!tempIndex)
        {
            this_obj->fBogus = TRUE;
            return;
        }
        /* set up first block.*/
        tempIndexCount = UCMP8_kBlockCount;
        for (iIndex = 0; iIndex < UCMP8_kBlockCount; ++iIndex)
        {
            tempIndex[iIndex] = (uint16_t)iIndex;
        } /* endfor (iIndex = 0; .....)*/
        this_obj->fIndex[0] = 0;

        /* for each successive block, find out its first position in the compacted array*/
        for (iBlock = 1; iBlock < UCMP8_kIndexCount; ++iBlock)
        {
            int32_t newCount, firstPosition, block;
            block = iBlock << UCMP8_kBlockShift;
            /*      if (debugSmall) if (block > debugSmallLimit) break;*/
            firstPosition = findOverlappingPosition(this_obj,
                block,
                tempIndex,
                tempIndexCount,
                cycle);

            /* if not contained in the current list, copy the remainder
            * invariant; cumulativeHash[iBlock] = XOR of values from iBlock-kBlockCount+1 to iBlock
            * we do this_obj by XORing out cumulativeHash[iBlock-kBlockCount]
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
            this_obj->fIndex[iBlock] = (uint16_t)firstPosition;
        } /* endfor (iBlock = 1.....)*/

        /* now allocate and copy the items into the array*/
        tempArray = (int8_t*) uprv_malloc(tempIndexCount * sizeof(int8_t));
        if (!tempArray)
        {
            this_obj->fBogus = TRUE;
            uprv_free(tempIndex);
            return;
        }
        for (iIndex = 0; iIndex < tempIndexCount; ++iIndex)
        {
            tempArray[iIndex] = this_obj->fArray[tempIndex[iIndex]];
        }
        uprv_free(this_obj->fArray);
        this_obj->fArray = tempArray;
        this_obj->fCount = tempIndexCount;


        /* free up temp storage*/
        uprv_free(tempIndex);
        this_obj->fCompact = TRUE;
    } /* endif (!this_obj->fCompact)*/
}

U_CAPI  uint32_t U_EXPORT2 ucmp8_flattenMem (const CompactByteArray* array, UMemoryStream *MS)
{
    int32_t size = 0;

    uprv_mstrm_write32(MS, ICU_UCMP8_VERSION);
    size += 4;

    uprv_mstrm_write32(MS, array->fCount);
    size += 4;

    uprv_mstrm_writeBlock(MS, array->fIndex, sizeof(array->fIndex[0])*UCMP8_kIndexCount);
    size += sizeof(array->fIndex[0])*UCMP8_kIndexCount;

    uprv_mstrm_writeBlock(MS, array->fArray, sizeof(array->fArray[0])*array->fCount);
    size += sizeof(array->fArray[0])*array->fCount;

    while(size%4) /* end padding */
    {
        uprv_mstrm_writePadding(MS, 1); /* Pad total so far to even size */
        size += 1;
    }

    return size;
}

/* We use sizeof(*array), etc so that this code can be as portable as 
   possible between the ucmpX_ family.
*/

U_CAPI  void U_EXPORT2 ucmp8_initFromData(CompactByteArray *this_obj, const uint8_t **source, UErrorCode *status)
{
    uint32_t i;
    const uint8_t *oldSource = *source;

    if(U_FAILURE(*status))
        return;

    this_obj->fArray = NULL;
    this_obj->fIndex = NULL;
    this_obj->fBogus = FALSE;
    this_obj->fStructSize = sizeof(CompactByteArray);
    this_obj->fCompact = TRUE;
    this_obj->fAlias = TRUE;
    this_obj->fIAmOwned = TRUE;

    i = * ((const uint32_t*) *source);
    (*source) += 4;

    if(i != ICU_UCMP8_VERSION)
    {
        *status = U_INVALID_FORMAT_ERROR;
        return;
    }

    this_obj->fCount = * ((const uint32_t*)*source);
    (*source) += 4;

    this_obj->fIndex = (uint16_t*) *source;
    (*source) += sizeof(this_obj->fIndex[0])*UCMP8_kIndexCount;

    this_obj->fArray = (int8_t*) *source;
    (*source) += sizeof(this_obj->fArray[0])*this_obj->fCount;

    /* eat up padding */
    while((*source-(oldSource))%4)
        (*source)++;
}
