/*
*******************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/
/*===============================================================================
 *
 * File cmpshrta.cpp
 *
 * Modification History:
 *
 *  Date        Name        Description
 *  2/5/97      aliu        Added CompactIntArray streamIn and streamOut methods.
 *  3/4/97      aliu        Tuned performance of CompactIntArray constructor,
 * 05/07/97     helena      Added isBogus()
 *                          based on performance data indicating that this_obj was slow.
 * 07/15/98        erm            Synched with Java 1.2 CompactShortArray.java.
 * 07/30/98        erm            Added changes from 07/29/98 code review.
 * 11/01/99		weiv		Added getArray, getIndex and getCount based on Jitterbug 4
 *===============================================================================
 */
#include "ucmp16.h"
#include "cmemory.h"





#define arrayRegionMatches(source, sourceStart, target, targetStart, len) (uprv_memcmp(&source[sourceStart], &target[targetStart], len * sizeof(int16_t)) != 0)

/* internal constants*/
#define UCMP16_kMaxUnicode_int 65535
#define UCMP16_kUnicodeCount_int (UCMP16_kMaxUnicode_int + 1)
#define UCMP16_kBlockShift_int 7
#define UCMP16_kBlockCount_int (1 << UCMP16_kBlockShift_int)
#define UCMP16_kBlockBytes_int (UCMP16_kBlockCount_int * sizeof(int16_t))
#define UCMP16_kIndexShift_int (16 - UCMP16_kBlockShift_int)
#define UCMP16_kIndexCount_int (1 << UCMP16_kIndexShift_int)
#define UCMP16_kBlockMask_int (UCMP16_kBlockCount_int - 1)


const int32_t UCMP16_kMaxUnicode = UCMP16_kMaxUnicode_int;
const int32_t UCMP16_kUnicodeCount = UCMP16_kUnicodeCount_int;
const int32_t UCMP16_kBlockShift = UCMP16_kBlockShift_int;
const int32_t UCMP16_kBlockCount = UCMP16_kBlockCount_int;
const int32_t UCMP16_kBlockBytes = UCMP16_kBlockBytes_int;
const int32_t UCMP16_kIndexShift = UCMP16_kIndexShift_int;
const int32_t UCMP16_kIndexCount = UCMP16_kIndexCount_int;
const uint32_t UCMP16_kBlockMask = UCMP16_kBlockMask_int;

/**
 * Sets the array to the invalid memory state.
 */
static CompactShortArray* setToBogus(CompactShortArray* array);
static void touchBlock(CompactShortArray* this_obj,
               int32_t i,
               int16_t value);
static bool_t blockTouched(const CompactShortArray* this_obj,
               int32_t i);


/* debug flags*/
/*=======================================================*/

int32_t ucmp16_getkUnicodeCount()
{return UCMP16_kUnicodeCount;}

int32_t ucmp16_getkBlockCount()
{return UCMP16_kBlockCount;}

CompactShortArray* ucmp16_open(int16_t defaultValue)
{
  int32_t i;
  CompactShortArray* this_obj = (CompactShortArray*) uprv_malloc(sizeof(CompactShortArray));
  if (this_obj == NULL) return NULL;
  
  this_obj->fStructSize = sizeof(CompactShortArray);
  this_obj->fCount = UCMP16_kUnicodeCount;
  this_obj->fCompact = FALSE; 
  this_obj->fBogus = FALSE;
  this_obj->fArray = NULL;
  this_obj->fAlias = FALSE;
  this_obj->fIndex = NULL;
  this_obj->fHashes = NULL; 
  this_obj->fIAmOwned = FALSE;
  this_obj->fDefaultValue = defaultValue;
  
  this_obj->fArray = (int16_t*)uprv_malloc(UCMP16_kUnicodeCount * sizeof(int16_t));
  if (this_obj->fArray == NULL)
    {
      this_obj->fBogus = TRUE;
      return NULL;
    }
  
  this_obj->fIndex = (uint16_t*)uprv_malloc(UCMP16_kIndexCount * sizeof(uint16_t));
  if (this_obj->fIndex == NULL)
    {
      uprv_free(this_obj->fArray);
      this_obj->fArray = NULL;
      
      this_obj->fBogus = TRUE;
      return NULL;
    }
  
  this_obj->kBlockShift = UCMP16_kBlockShift;
  this_obj->kBlockMask = UCMP16_kBlockMask;
  for (i = 0; i < UCMP16_kUnicodeCount; i += 1)
    {
      this_obj->fArray[i] = defaultValue;
    }
  
  this_obj->fHashes =(int32_t*)uprv_malloc(UCMP16_kIndexCount * sizeof(int32_t));
  if (this_obj->fHashes == NULL)
    {
      uprv_free(this_obj->fArray);
      uprv_free(this_obj->fIndex);
      this_obj->fBogus = TRUE;
      return NULL;
    }
  
  for (i = 0; i < UCMP16_kIndexCount; i += 1)
    {
      this_obj->fIndex[i] = (uint16_t)(i << UCMP16_kBlockShift);
      this_obj->fHashes[i] = 0;
    }
  
  return this_obj;
}


void ucmp16_init(CompactShortArray *this_obj, int16_t defaultValue)
{
  int32_t i;

  this_obj->fStructSize = sizeof(CompactShortArray);
  this_obj->fCount = UCMP16_kUnicodeCount;
  this_obj->fCompact = FALSE; 
  this_obj->fBogus = FALSE;
  this_obj->fArray = NULL;
  this_obj->fAlias = FALSE;
  this_obj->fIndex = NULL;
  this_obj->fHashes = NULL; 
  this_obj->fIAmOwned = TRUE;
  this_obj->fDefaultValue = defaultValue;
  
  this_obj->fArray = (int16_t*)uprv_malloc(UCMP16_kUnicodeCount * sizeof(int16_t));
  if (this_obj->fArray == NULL)
    {
      this_obj->fBogus = TRUE;
      return;
    }
  
  this_obj->fIndex = (uint16_t*)uprv_malloc(UCMP16_kIndexCount * sizeof(uint16_t));
  if (this_obj->fIndex == NULL)
    {
      uprv_free(this_obj->fArray);
      this_obj->fArray = NULL;
      
      this_obj->fBogus = TRUE;
      return;
    }
  
  this_obj->kBlockShift = UCMP16_kBlockShift;
  this_obj->kBlockMask = UCMP16_kBlockMask;
  for (i = 0; i < UCMP16_kUnicodeCount; i += 1)
    {
      this_obj->fArray[i] = defaultValue;
    }
  
  this_obj->fHashes =(int32_t*)uprv_malloc(UCMP16_kIndexCount * sizeof(int32_t));
  if (this_obj->fHashes == NULL)
    {
      uprv_free(this_obj->fArray);
      uprv_free(this_obj->fIndex);
      this_obj->fBogus = TRUE;
      return;
    }
  
  for (i = 0; i < UCMP16_kIndexCount; i += 1)
    {
      this_obj->fIndex[i] = (uint16_t)(i << UCMP16_kBlockShift);
      this_obj->fHashes[i] = 0;
    }
}

CompactShortArray* ucmp16_openAdopt(uint16_t *indexArray,
                    int16_t *newValues, 
                    int32_t count,
                    int16_t defaultValue)
{
  CompactShortArray* this_obj = (CompactShortArray*) uprv_malloc(sizeof(CompactShortArray));
  if (this_obj == NULL) return NULL;
  this_obj->fHashes = NULL;
  this_obj->fCount = count; 
  this_obj->fDefaultValue = defaultValue;
  this_obj->fBogus = FALSE;
  this_obj->fArray = newValues;
  this_obj->fIndex = indexArray;
  this_obj->fCompact = count < UCMP16_kUnicodeCount;
  this_obj->fStructSize = sizeof(CompactShortArray);
  this_obj->kBlockShift = UCMP16_kBlockShift;
  this_obj->kBlockMask = UCMP16_kBlockMask;
  this_obj->fAlias = FALSE;
  this_obj->fIAmOwned = FALSE;

  return this_obj;
}


CompactShortArray* ucmp16_openAdoptWithBlockShift(uint16_t *indexArray,
                          int16_t *newValues,
                          int32_t count,
                          int16_t defaultValue,
                          int32_t blockShift)
{
  CompactShortArray* this_obj = ucmp16_openAdopt(indexArray,
                         newValues,
                         count,
                         defaultValue);
  if (this_obj == NULL) return NULL;
  
  this_obj->kBlockShift  = blockShift;
  this_obj->kBlockMask = (uint32_t) (((uint32_t)1 << (uint32_t)blockShift) - (uint32_t)1);
  
  return this_obj;
}


CompactShortArray* ucmp16_openAlias(uint16_t *indexArray,
                    int16_t *newValues, 
                    int32_t count,
                    int16_t defaultValue)
{
  CompactShortArray* this_obj = (CompactShortArray*) uprv_malloc(sizeof(CompactShortArray));
  if (this_obj == NULL) return NULL;
  this_obj->fHashes = NULL;
  this_obj->fCount = count; 
  this_obj->fDefaultValue = defaultValue;
  this_obj->fBogus = FALSE;
  this_obj->fArray = newValues;
  this_obj->fIndex = indexArray;
  this_obj->fCompact = count < UCMP16_kUnicodeCount;
  this_obj->fStructSize = sizeof(CompactShortArray);
  this_obj->kBlockShift = UCMP16_kBlockShift;
  this_obj->kBlockMask = UCMP16_kBlockMask;
  this_obj->fAlias = TRUE;
  this_obj->fIAmOwned = FALSE;

  return this_obj;
}

/*=======================================================*/
 
void ucmp16_close(CompactShortArray* this_obj)
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
    if(this_obj->fHashes != NULL) {
      uprv_free(this_obj->fHashes);
    }
    if(!this_obj->fIAmOwned)
      {
        uprv_free(this_obj);
      }
  }
}

CompactShortArray* setToBogus(CompactShortArray* this_obj)
{
  if(this_obj != NULL) {
    if(!this_obj->fAlias) {
      uprv_free(this_obj->fArray);
      this_obj->fArray = NULL;
  
      uprv_free(this_obj->fIndex);
      this_obj->fIndex = NULL;
    }
    uprv_free(this_obj->fHashes);
    this_obj->fHashes = NULL;

    this_obj->fCount = 0;
    this_obj->fCompact = FALSE;
    this_obj->fBogus = TRUE;
  }
  
  return this_obj;
}


void ucmp16_expand(CompactShortArray* this_obj)
{
  if (this_obj->fCompact)
    {
      int32_t i;
      int16_t *tempArray = (int16_t*)uprv_malloc(UCMP16_kUnicodeCount * sizeof(int16_t));
      
      if (tempArray == NULL)
    {
      this_obj->fBogus = TRUE;
      return;
    }
      
      for (i = 0; i < UCMP16_kUnicodeCount; i += 1)
    {
      tempArray[i] = ucmp16_get(this_obj, (UChar)i);  /* HSYS : How expand?*/
        }
      
      for (i = 0; i < (1 << (16 - this_obj->kBlockShift)); i += 1)
    {
      this_obj->fIndex[i] = (uint16_t)(i<<this_obj->kBlockShift);
        }
      
      uprv_free(this_obj->fArray);
      this_obj->fArray = tempArray;
      this_obj->fCompact = FALSE;
    }
}

void ucmp16_set(CompactShortArray* this_obj,
        UChar c,
        int16_t value)
{
  if (this_obj->fCompact)
    {
      ucmp16_expand(this_obj);
      if (this_obj->fBogus) return;
    }
  
  this_obj->fArray[(int32_t)c] = value;
  
  if (value != this_obj->fDefaultValue)
    {
      touchBlock(this_obj, c >> this_obj->kBlockShift, value);
    }
}


void ucmp16_setRange(CompactShortArray* this_obj, 
             UChar start,
             UChar end,
             int16_t value)
{
  int32_t i;
  if (this_obj->fCompact)
    {
      ucmp16_expand(this_obj);
      if (this_obj->fBogus)  return;
    }
  if (value != this_obj->fDefaultValue)
    {
      for (i = start; i <= end; i += 1)
    {
       this_obj->fArray[i] = value;
      touchBlock(this_obj, i >> this_obj->kBlockShift, value);
    }
    }
  else
    {
      for (i = start; i <= end; i += 1)      this_obj->fArray[i] = value;
    }
}


/*=======================================================*/
void ucmp16_compact(CompactShortArray* this_obj)
{
  if (!this_obj->fCompact)
    {
      int32_t limitCompacted = 0;
      int32_t i, iBlockStart;
      int16_t iUntouched = -1;
      
      for (i = 0, iBlockStart = 0; i < (1 << (16 - this_obj->kBlockShift)); i += 1, iBlockStart += (1 << this_obj->kBlockShift))
    {
      bool_t touched = blockTouched(this_obj, i);
      
      this_obj->fIndex[i] = 0xFFFF;
      
      if (!touched && iUntouched != -1)
        {
          /* If no values in this_obj block were set, we can just set its
           * index to be the same as some other block with no values
           * set, assuming we've seen one yet.
           */
          this_obj->fIndex[i] = iUntouched;
            }
      else
        {
          int32_t j, jBlockStart;
          
          for (j = 0, jBlockStart = 0;
           j < limitCompacted;
           j += 1, jBlockStart += (1 << this_obj->kBlockShift))
        {
          if (this_obj->fHashes[i] == this_obj->fHashes[j] &&
              arrayRegionMatches(this_obj->fArray,
                     iBlockStart,
                     this_obj->fArray, 
                     jBlockStart,
                     (1 << this_obj->kBlockShift)))
            {
              this_obj->fIndex[i] = (int16_t)jBlockStart;
                    }
                }
          
                /* TODO: verify this_obj is correct*/
          if (this_obj->fIndex[i] == 0xFFFF)
        {
          /* we didn't match, so copy & update*/
          uprv_memcpy(&(this_obj->fArray[jBlockStart]), 
                 &(this_obj->fArray[iBlockStart]),
                 (1 << this_obj->kBlockShift)*sizeof(int16_t));
          
          this_obj->fIndex[i] = (int16_t)jBlockStart;
          this_obj->fHashes[j] = this_obj->fHashes[i];
          limitCompacted += 1;
          
          if (!touched)
            {
              /* If this_obj is the first untouched block we've seen,*/
              /* remember its index.*/
              iUntouched = (int16_t)jBlockStart;
                    }
                }
            }
        }

        /* we are done compacting, so now make the array shorter*/
      {
    int32_t newSize = limitCompacted * (1 << this_obj->kBlockShift);
    int16_t *result = (int16_t*) uprv_malloc(sizeof(int16_t) * newSize);
    
    uprv_memcpy(result, this_obj->fArray, newSize * sizeof(int16_t));

    uprv_free(this_obj->fArray);
    this_obj->fArray = result;
    this_obj->fCount = newSize;
    uprv_free(this_obj->fHashes);
    this_obj->fHashes = NULL;

    this_obj->fCompact = TRUE;
      }
    }
}

/**
 * Query whether a specified block was "touched", i.e. had a value set.
 * Untouched blocks can be skipped when compacting the array
 */

int16_t ucmp16_getDefaultValue(const CompactShortArray* this_obj)
{
  return this_obj->fDefaultValue;
}


void touchBlock(CompactShortArray* this_obj,
        int32_t i,
        int16_t value)
{
  this_obj->fHashes[i] = (this_obj->fHashes[i] + (value << 1)) | 1;
}

bool_t blockTouched(const CompactShortArray* this_obj, int32_t i)
{
  return (this_obj->fHashes[i] != 0);
}

uint32_t ucmp16_getCount(const CompactShortArray* this_obj)
{
    return this_obj->fCount;
}

const int16_t* ucmp16_getArray(const CompactShortArray* this_obj)
{
    return this_obj->fArray;
}

const uint16_t* ucmp16_getIndex(const CompactShortArray* this_obj)
{
    return this_obj->fIndex;
}


/* We use sizeof(*array), etc so that this code can be as portable as 
   possible between the ucmpX_ family.  Check lines marked 'SIZE'.
*/

U_CAPI  void U_EXPORT2 ucmp16_initFromData(CompactShortArray *this_obj, const uint8_t **source, UErrorCode *status)
{
  uint32_t i;
  const uint8_t *oldSource = *source;

  if(U_FAILURE(*status))
    return;

 this_obj->fArray = NULL;
 this_obj->fIndex = NULL; 
 this_obj->fBogus = FALSE;
 this_obj->fStructSize = sizeof(CompactShortArray);
 this_obj->fCompact = TRUE;
 this_obj->fAlias = TRUE;
 this_obj->fIAmOwned = TRUE;
 this_obj->fHashes = NULL;
 this_obj->fDefaultValue = 0x0000; /* not used */
  
 i = * ((const uint32_t*) *source);
 (*source) += 4;

 if(i != ICU_UCMP16_VERSION)
 {
   *status = U_INVALID_FORMAT_ERROR;
   return;
 }
  
 this_obj->fCount = * ((const uint32_t*)*source);
 (*source) += 4;

 this_obj->kBlockShift = * ((const uint32_t*)*source);
 (*source) += 4;

 this_obj->kBlockMask = * ((const uint32_t*)*source);
 (*source) += 4;

 this_obj->fIndex = (uint16_t*) *source;
 (*source) += sizeof(this_obj->fIndex[0])*UCMP16_kIndexCount;

 this_obj->fArray = (int16_t*) *source;
 (*source) += sizeof(this_obj->fArray[0])*this_obj->fCount;

 /* eat up padding */
 while((*source-(oldSource))%4)
    (*source)++;
}


