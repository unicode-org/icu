/*
*****************************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
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
 *                          based on performance data indicating that this was slow.
 * 07/15/98        erm            Synched with Java 1.2 CompactShortArray.java.
 * 07/30/98        erm            Added changes from 07/29/98 code review.
 *===============================================================================
 */
#include "ucmp16.h"
#include "cmemory.h"





#define arrayRegionMatches(source, sourceStart, target, targetStart, len) (icu_memcmp(&source[sourceStart], &target[targetStart], len * sizeof(int16_t)) != 0)

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
static void touchBlock(CompactShortArray* this,
               int32_t i,
               int16_t value);
static bool_t blockTouched(const CompactShortArray* this,
               int32_t i);


/* debug flags*/
/*=======================================================*/

int32_t ucmp16_getkUnicodeCount()
{return UCMP16_kUnicodeCount;}

int32_t ucmp16_getkBlockCount()
{return UCMP16_kBlockCount;}

int32_t ucmp16_getkIndexCount()
{ return UCMP16_kIndexCount;}

CompactShortArray* ucmp16_open(int16_t defaultValue)
{
  int32_t i;
  CompactShortArray* this = (CompactShortArray*) icu_malloc(sizeof(CompactShortArray));
  if (this == NULL) return NULL;
  
  this->fCount = UCMP16_kUnicodeCount;
  this->fCompact = FALSE; 
  this->fBogus = FALSE;
  this->fArray = NULL;
  this->fIndex = NULL;
  this->fHashes = NULL; 
  this->fDefaultValue = defaultValue;
  
  this->fArray = (int16_t*)icu_malloc(UCMP16_kUnicodeCount * sizeof(int16_t));
  if (this->fArray == NULL)
    {
      this->fBogus = TRUE;
      return NULL;
    }
  
  this->fIndex = (uint16_t*)icu_malloc(UCMP16_kIndexCount * sizeof(uint16_t));
  if (this->fIndex == NULL)
    {
      icu_free(this->fArray);
      this->fArray = NULL;
      
      this->fBogus = TRUE;
      return NULL;
    }
  
  this->kBlockShift = UCMP16_kBlockShift;
  this->kBlockMask = UCMP16_kBlockMask;
  for (i = 0; i < UCMP16_kUnicodeCount; i += 1)
    {
      this->fArray[i] = defaultValue;
    }
  
  this->fHashes =(int32_t*)icu_malloc(UCMP16_kIndexCount * sizeof(int32_t));
  if (this->fHashes == NULL)
    {
      icu_free(this->fArray);
      icu_free(this->fIndex);
      this->fBogus = TRUE;
      return NULL;
    }
  
  for (i = 0; i < UCMP16_kIndexCount; i += 1)
    {
      this->fIndex[i] = (uint16_t)(i << UCMP16_kBlockShift);
      this->fHashes[i] = 0;
    }
  
  return this;
}

CompactShortArray* ucmp16_openAdopt(uint16_t *indexArray,
                    int16_t *newValues, 
                    int32_t count,
                    int16_t defaultValue)
{
  CompactShortArray* this = (CompactShortArray*) icu_malloc(sizeof(CompactShortArray));
  if (this == NULL) return NULL;
  this->fHashes = NULL;
  this->fCount = count; 
  this->fDefaultValue = defaultValue;
  this->fBogus = FALSE;
  this->fArray = newValues;
  this->fIndex = indexArray;
  this->fCompact = count < UCMP16_kUnicodeCount;
  this->kBlockShift = UCMP16_kBlockShift;
  this->kBlockMask = UCMP16_kBlockMask;

  return this;
}

CompactShortArray* ucmp16_openAdoptWithBlockShift(uint16_t *indexArray,
                          int16_t *newValues,
                          int32_t count,
                          int16_t defaultValue,
                          int32_t blockShift)
{
  CompactShortArray* this = ucmp16_openAdopt(indexArray,
                         newValues,
                         count,
                         defaultValue);
  if (this == NULL) return NULL;
  
  this->kBlockShift  = blockShift;
  this->kBlockMask = (uint32_t) (((uint32_t)1 << (uint32_t)blockShift) - (uint32_t)1);
  
  return this;
}

/*=======================================================*/
 
void ucmp16_close(CompactShortArray* this)
{
  icu_free(this->fArray);
  icu_free(this->fIndex);
  icu_free(this->fHashes);
  icu_free(this);

  return;
}

CompactShortArray* setToBogus(CompactShortArray* this)
{
  icu_free(this->fArray);
  this->fArray = NULL;
  
  icu_free(this->fIndex);
  this->fIndex = NULL;
  
  icu_free(this->fHashes);
  this->fHashes = NULL;
  
  this->fCount = 0;
  this->fCompact = FALSE;
  this->fBogus = TRUE;
  
  return this;
}


void ucmp16_expand(CompactShortArray* this)
{
  if (this->fCompact)
    {
      int32_t i;
      int16_t *tempArray = (int16_t*)icu_malloc(UCMP16_kUnicodeCount * sizeof(int16_t));
      
      if (tempArray == NULL)
    {
      this->fBogus = TRUE;
      return;
    }
      
      for (i = 0; i < UCMP16_kUnicodeCount; i += 1)
    {
      tempArray[i] = ucmp16_get(this, (UChar)i);  /* HSYS : How expand?*/
        }
      
      for (i = 0; i < (1 << (16 - this->kBlockShift)); i += 1)
    {
      this->fIndex[i] = (uint16_t)(i<<this->kBlockShift);
        }
      
      icu_free(this->fArray);
      this->fArray = tempArray;
      this->fCompact = FALSE;
    }
}

void ucmp16_set(CompactShortArray* this,
        UChar c,
        int16_t value)
{
  if (this->fCompact)
    {
      ucmp16_expand(this);
      if (this->fBogus) return;
    }
  
  this->fArray[(int32_t)c] = value;
  
  if (value != this->fDefaultValue)
    {
      touchBlock(this, c >> this->kBlockShift, value);
    }
}


void ucmp16_setRange(CompactShortArray* this, 
             UChar start,
             UChar end,
             int16_t value)
{
  int32_t i;
  if (this->fCompact)
    {
      ucmp16_expand(this);
      if (this->fBogus)  return;
    }
  if (value != this->fDefaultValue)
    {
      for (i = start; i <= end; i += 1)
    {
       this->fArray[i] = value;
      touchBlock(this, i >> this->kBlockShift, value);
    }
    }
  else
    {
      for (i = start; i <= end; i += 1)      this->fArray[i] = value;
    }
}


/*=======================================================*/
void ucmp16_compact(CompactShortArray* this)
{
  if (!this->fCompact)
    {
      int32_t limitCompacted = 0;
      int32_t i, iBlockStart;
      int16_t iUntouched = -1;
      
      for (i = 0, iBlockStart = 0; i < (1 << (16 - this->kBlockShift)); i += 1, iBlockStart += (1 << this->kBlockShift))
    {
      bool_t touched = blockTouched(this, i);
      
      this->fIndex[i] = 0xFFFF;
      
      if (!touched && iUntouched != -1)
        {
          /* If no values in this block were set, we can just set its
           * index to be the same as some other block with no values
           * set, assuming we've seen one yet.
           */
          this->fIndex[i] = iUntouched;
            }
      else
        {
          int32_t j, jBlockStart;
          
          for (j = 0, jBlockStart = 0;
           j < limitCompacted;
           j += 1, jBlockStart += (1 << this->kBlockShift))
        {
          if (this->fHashes[i] == this->fHashes[j] &&
              arrayRegionMatches(this->fArray,
                     iBlockStart,
                     this->fArray, 
                     jBlockStart,
                     (1 << this->kBlockShift)))
            {
              this->fIndex[i] = (int16_t)jBlockStart;
                    }
                }
          
                /* TODO: verify this is correct*/
          if (this->fIndex[i] == 0xFFFF)
        {
          /* we didn't match, so copy & update*/
          icu_memcpy(&(this->fArray[jBlockStart]), 
                 &(this->fArray[iBlockStart]),
                 (1 << this->kBlockShift)*sizeof(int16_t));
          
          this->fIndex[i] = (int16_t)jBlockStart;
          this->fHashes[j] = this->fHashes[i];
          limitCompacted += 1;
          
          if (!touched)
            {
              /* If this is the first untouched block we've seen,*/
              /* remember its index.*/
              iUntouched = (int16_t)jBlockStart;
                    }
                }
            }
        }

        /* we are done compacting, so now make the array shorter*/
      {
    int32_t newSize = limitCompacted * (1 << this->kBlockShift);
    int16_t *result = (int16_t*) icu_malloc(sizeof(int16_t) * newSize);
    
    icu_memcpy(result, this->fArray, newSize * sizeof(int16_t));
    
    icu_free(this->fArray);
    this->fArray = result;
    this->fCount = newSize;
    icu_free(this->fHashes);
    this->fHashes = NULL;
    
    this->fCompact = TRUE;
      }
    }
}

/**
 * Query whether a specified block was "touched", i.e. had a value set.
 * Untouched blocks can be skipped when compacting the array
 */

int16_t ucmp16_getDefaultValue(const CompactShortArray* this)
{
  return this->fDefaultValue;
}


void touchBlock(CompactShortArray* this,
        int32_t i,
        int16_t value)
{
  this->fHashes[i] = (this->fHashes[i] + (value << 1)) | 1;
}

bool_t blockTouched(const CompactShortArray* this, int32_t i)
{
  return (this->fHashes[i] != 0);
}


const int16_t*
ucmp16_getArray(const CompactShortArray* this)
{
    return this->fArray;
}

const uint16_t*
ucmp16_getIndex(const CompactShortArray* this)
{
    return this->fIndex;
}

uint32_t 
ucmp16_getCount(const CompactShortArray* this)
{
    return this->fCount;
}

