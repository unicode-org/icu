/*
 **********************************************************************
 *   Copyright (C) 2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 *   file name:  ucmpe32.c
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2001aug03
 *   created by: Vladimir Weinstein
 *
 *   This is basically a rip-off of trie developed by Markus for 
 *   normalization data, but using a reduced ucmp interface
 *   Interface is implemented as much as required by the collation
 *   framework.
 *   This table is slow on data addition, but should support surrogates
 *   nicely.
 */

#include "ucmpe32.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include <stdio.h>


/* tool memory helper ------------------------------------------------------- */

static UToolMemory *
utm_open(const char *name, uint32_t count, uint32_t size) {
    UToolMemory *mem=(UToolMemory *)uprv_malloc(sizeof(UToolMemory)+count*size);
    if(mem==NULL) {
        fprintf(stderr, "error: %s - out of memory\n", name);
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
    uprv_strcpy(mem->name, name);
    mem->count=count;
    mem->size=size;
    mem->index=0;
    return mem;
}

/* we don't use this - we don't clean up memory here... */
static void
utm_close(UToolMemory *mem) {
    if(mem!=NULL) {
        uprv_free(mem);
    }
}

static void *
utm_getStart(UToolMemory *mem) {
    return (char *)mem->array;
}

static void *
utm_alloc(UToolMemory *mem) {
    char *p=(char *)mem->array+mem->index*mem->size;
    if(++mem->index<=mem->count) {
        uprv_memset(p, 0, mem->size);
        return p;
    } else {
        fprintf(stderr, "error: %s - trying to use more than %ld preallocated units\n",
                mem->name, (long)mem->count);
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
}

static void *
utm_allocN(UToolMemory *mem, int32_t n) {
    char *p=(char *)mem->array+mem->index*mem->size;
    if((mem->index+=(uint32_t)n)<=mem->count) {
        uprv_memset(p, 0, n*mem->size);
        return p;
    } else {
        fprintf(stderr, "error: %s - trying to use more than %ld preallocated units\n",
                mem->name, (long)mem->count);
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
}

/* builder data ------------------------------------------------------------- */

CompactEIntArray* ucmpe32_open(int32_t defaultValue)
{
  uint32_t  i;
  int32_t *p, *p_end;
  uint16_t *q, *q_end;
  uint32_t *bla;
  CompactEIntArray* this_obj = (CompactEIntArray*) uprv_malloc(sizeof(CompactEIntArray));
  if (this_obj == NULL) return NULL;
  
                /* reset stage 1 of the trie */
                uprv_memset(this_obj->stage1, 0, sizeof(this_obj->stage1));

                /* allocate stage 2 of the trie and reset the first block */
                this_obj->stage2Mem=utm_open("gennorm trie stage 2", 60000, sizeof(*(this_obj->stage2)));
                this_obj->stage2=utm_allocN(this_obj->stage2Mem, _UCMPE32_STAGE_2_BLOCK_COUNT);
                for(bla = this_obj->stage2; bla<this_obj->stage2+_UCMPE32_STAGE_2_BLOCK_COUNT; bla++) {
                  *bla = 0xF0000000;
                }

  this_obj->fStructSize = sizeof(CompactEIntArray);
  this_obj->fArray = NULL;
  this_obj->fIndex = NULL;
  this_obj->fCount = UCMPE32_kUnicodeCount;
  this_obj->fCompact = FALSE; 
  this_obj->fBogus = FALSE;
  this_obj->fAlias = FALSE;
  this_obj->fIAmOwned = FALSE;
  
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
 this_obj->fArray = (int32_t*)uprv_malloc(UCMPE32_kUnicodeCount * sizeof(int32_t));
      if (this_obj->fArray == NULL)    {
      this_obj->fBogus = TRUE;
      return NULL;
    }
  
    this_obj->fIndex = (uint16_t*)uprv_malloc(UCMPE32_kIndexCount * sizeof(uint16_t));
    if (!this_obj->fIndex) {
        uprv_free(this_obj->fArray); 
        this_obj->fArray = NULL;
        this_obj->fBogus = TRUE;
        return NULL;
    }
    p = this_obj->fArray;
    p_end = p + UCMPE32_kUnicodeCount;
    while (p < p_end) *p++ = defaultValue;

    q = this_obj->fIndex;

    q_end = q + UCMPE32_kIndexBMPCount;
    i = 0;
    while (q < q_end)
    {
        *q++ = (uint16_t)(i >> UCMPE32_kBlockShift);
        i += (1 << UCMPE32_kBlockShift);
    }

    q_end = this_obj->fIndex + UCMPE32_kIndexCount;

    while (q < q_end)
    {
        *q++ = (uint16_t)(i >> UCMPE32_kBlockShift);
        i += (1 << UCMPE32_kBlockShift);
    }

    return this_obj;
}



/*
 * get or create a Norm unit;
 * get or create the intermediate trie entries for it as well
 */
/********* THIS IS THE ADD FUNCTION ********************/
void  ucmpe32_set32(CompactEIntArray* this_obj, UChar32 code, int32_t value)
{
    uint16_t stage2Block, k;

    if (this_obj->fCompact == TRUE) {
      return;
    }

    {
        uint32_t i;
        uint16_t j;

        i=code>>_UCMPE32_TRIE_SHIFT;
        j=this_obj->stage1[i];
        if(j==0) {
            /* allocate a stage 2 block */
            uint32_t *p, *bla;           

            p=(uint32_t *)utm_allocN(this_obj->stage2Mem, _UCMPE32_STAGE_2_BLOCK_COUNT);
            for(bla = p; bla<p+_UCMPE32_STAGE_2_BLOCK_COUNT; bla++) {
              *bla = 0xF0000000;
            }
            this_obj->stage1[i]=j=(uint16_t)(p-this_obj->stage2);
        }
        stage2Block=j;
    }

    k=(uint16_t)(stage2Block+(code&_UCMPE32_STAGE_2_MASK));

    this_obj->stage2[k] = value;
}

void  ucmpe32_setSurrogate(CompactEIntArray* this_obj, UChar lead, UChar trail, int32_t value)
{
    if (this_obj->fCompact == TRUE) {
      return;
    }
    ucmpe32_set(this_obj, (int32_t)UTF16_GET_PAIR_VALUE(lead, trail), value);
}


int32_t ucmpe32_get32(CompactEIntArray *this_obj, UChar32 index) {
  int32_t index_lookup = this_obj->stage1[index >> _UCMPE32_TRIE_SHIFT] ;
  int32_t addition = (index & _UCMPE32_STAGE_2_MASK);

  return (this_obj->stage2[index_lookup + addition]);
/*
  int32_t index_lookup = array->fIndex[(index >> UCMPE32_kBlockShift)] << UCMPE32_kBlockShift;
  int32_t addition = (index & UCMPE32_kBlockMask);

  return (array->fArray[index_lookup + addition]);
*/
}

int32_t ucmpe32_getSurrogate(CompactEIntArray *array, UChar lead, UChar trail) {
  return(ucmpe32_get32(array, (int32_t)UTF16_GET_PAIR_VALUE(lead, trail)));
#if 0
  int32_t leadValue32 = ucmpe32_get(array, lead);
  int32_t c = ((leadValue32 & 0xffc00) | (trail & 0x3ff)); 
  /* Lead surrogate data needs to be in the following format: */
  /* F50XXY000 - where X mask is 1111 (F) and Y mask is 1100 (C) */
  /* The ten bits for access will be in the middle of the field  */
  int32_t fixed_addition = UCMPE32_kIndexBMPCount;
  int32_t index_lookup = array->fIndex[fixed_addition + (c >> UCMPE32_kBlockShift)];
  int32_t addition = (c & UCMPE32_kBlockMask);
  return (stage2[index_lookup+ addition]);
  /*return (array->fArray[FUNKY_CONST + array->fIndex[c >> UCMPE32_kBlockShift]+ (c & UCMPE32_kBlockMask)]);*/
#endif
}

/*
 * Fold the supplementary code point data for one lead surrogate.
 */
static uint16_t
foldLeadSurrogate(uint16_t *parent, uint16_t parentCount,
                  uint32_t *stage, uint16_t *pStageCount,
                  uint32_t base) {
    uint32_t leadNorm32=0;
    uint32_t i, j, s2;
    uint32_t leadSurrogate=0xd7c0+(base>>10);

    printf("supplementary data for lead surrogate U+%04lx\n", (long)leadSurrogate);

    /* calculate the 32-bit data word for the lead surrogate */
    for(i=0; i<_UCMPE32_SURROGATE_BLOCK_COUNT; ++i) {
        s2=parent[(base>>_UCMPE32_TRIE_SHIFT)+i];
        if(s2!=0) {
            for(j=0; j<_UCMPE32_STAGE_2_BLOCK_COUNT; ++j) {
                /* basically, or all 32-bit data into the one for the lead surrogate */
                leadNorm32|=stage[s2+j];
            }
        }
    }

    if(leadNorm32==0) {
        /* FCD: nothing to do */
        return 0;
    }

    /*
     * For FCD, replace the entire combined value by the surrogate index
     * and make sure that it is not 0 (by not offsetting it by the BMP top,
     * since here we have enough bits for this);
     * lead surrogates are tested at runtime on the character code itself
     * instead on special values of the trie data -
     * this is because 16 bits in the FCD trie data do not allow for anything
     * but the two leading and trailing combining classes of the canonical decomposition.
     */
    leadNorm32=parentCount>>_UCMPE32_SURROGATE_BLOCK_BITS;

    /* enter the lead surrogate's data */
    s2=parent[leadSurrogate>>_UCMPE32_TRIE_SHIFT];
    if(s2==0) {
        /* allocate a new stage 2 block in stage (the memory is there from makeAll32()/makeFCD()) */
        s2=parent[leadSurrogate>>_UCMPE32_TRIE_SHIFT]=*pStageCount;
        *pStageCount+=_UCMPE32_STAGE_2_BLOCK_COUNT;
    }
    stage[s2+(leadSurrogate&_UCMPE32_STAGE_2_MASK)]=leadNorm32;

    /* move the actual stage 1 indexes from the supplementary position to the new one */
    uprv_memmove(parent+parentCount, parent+(base>>_UCMPE32_TRIE_SHIFT), _UCMPE32_SURROGATE_BLOCK_COUNT*2);

    /* increment stage 1 top */
    return _UCMPE32_SURROGATE_BLOCK_COUNT;
}

/*
 * Fold the normalization data for supplementary code points into
 * a compact area on top of the BMP-part of the trie index,
 * with the lead surrogates indexing this compact area.
 *
 * Use after makeAll32().
 */
static uint16_t
foldSupplementary(uint16_t *parent, uint16_t parentCount,
                  uint32_t *stage, uint16_t *pStageCount) {
    uint32_t c;
    uint16_t i;

    /* search for any stage 1 entries for supplementary code points */
    for(c=0x10000; c<0x110000;) {
        i=parent[c>>_UCMPE32_TRIE_SHIFT];
        if(i!=0) {
            /* there is data, treat the full block for a lead surrogate */
            c&=~0x3ff;
            parentCount+=foldLeadSurrogate(parent, parentCount, stage, pStageCount, c);
            c+=0x400;
        } else {
            c+=_UCMPE32_STAGE_2_BLOCK_COUNT;
        }
    }

    printf("trie index count: BMP %u  all Unicode %lu  folded %u\n",
           _UCMPE32_STAGE_1_BMP_COUNT, (long)_UCMPE32_STAGE_1_MAX_COUNT, parentCount);
    return parentCount;
}

static uint16_t
compact(uint16_t *parent, uint16_t parentCount,
        uint32_t *stage, uint16_t stageCount) {
    /*
     * This function is the common implementation for compacting
     * the stage 2 tables of 32-bit values.
     * It is a copy of genprops/store.c's compactStage() adapted for the 32-bit stage 2 tables.
     */
    static uint16_t map[0x10000>>_UCMPE32_TRIE_SHIFT];
    uint32_t x;
    uint16_t i, start, prevEnd, newStart;

    map[0]=0;
    newStart=_UCMPE32_STAGE_2_BLOCK_COUNT;
    for(start=newStart; start<stageCount;) {
        prevEnd=(uint16_t)(newStart-1);
        x=stage[start];
        if(x==stage[prevEnd]) {
            /* overlap by at least one */
            for(i=1; i<_UCMPE32_STAGE_2_BLOCK_COUNT && x==stage[start+i] && x==stage[prevEnd-i]; ++i) {}

            /* overlap by i */
            map[start>>_UCMPE32_TRIE_SHIFT]=(uint16_t)(newStart-i);

            /* move the non-overlapping indexes to their new positions */
            start+=i;
            for(i=(uint16_t)(_UCMPE32_STAGE_2_BLOCK_COUNT-i); i>0; --i) {
                stage[newStart++]=stage[start++];
            }
        } else if(newStart<start) {
            /* move the indexes to their new positions */
            map[start>>_UCMPE32_TRIE_SHIFT]=newStart;
            for(i=_UCMPE32_STAGE_2_BLOCK_COUNT; i>0; --i) {
                stage[newStart++]=stage[start++];
            }
        } else /* no overlap && newStart==start */ {
            map[start>>_UCMPE32_TRIE_SHIFT]=start;
            newStart+=_UCMPE32_STAGE_2_BLOCK_COUNT;
            start=newStart;
        }
    }

    /* now adjust the parent table */
    for(i=0; i<parentCount; ++i) {
        parent[i]=map[parent[i]>>_UCMPE32_TRIE_SHIFT];
    }

    /* we saved some space */
    printf("compacting trie: count of 32-bit words %lu->%lu\n",
            (long)stageCount, (long)newStart);
    return newStart;
}

void ucmpe32_compact(CompactEIntArray* this_obj, int32_t cycle) {
  uint16_t top = (uint16_t)this_obj->stage2Mem->index;
    /* FCD: fold supplementary code points into lead surrogates */
    this_obj->stage1Top=foldSupplementary(this_obj->stage1, _UCMPE32_STAGE_1_BMP_COUNT, this_obj->stage2, &top);

    /* FCD: compact stage 2 */
    top=compact(this_obj->stage1, this_obj->stage1Top, this_obj->stage2, top);
}



extern void
cleanUpData(CompactEIntArray* this_obj) {
    utm_close(this_obj->stage2Mem);
}

int32_t ucmpe32_getkUnicodeCount() { return UCMPE32_kUnicodeCount;}
int32_t ucmpe32_getkBlockCount() { return UCMPE32_kBlockCount;}

U_CAPI void ucmpe32_streamIn(CompactEIntArray* this_obj, FileStream* is)
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
        else if (len == UCMPE32_kIndexCount)
        {
            if (this_obj->fIndex == 0) 
                this_obj->fIndex =(uint16_t*)uprv_malloc(UCMPE32_kIndexCount * sizeof(uint16_t));
            if (!this_obj->fIndex) {
                this_obj->fBogus = TRUE;
                uprv_free(this_obj->fArray); 
                this_obj->fArray = 0;
                return;
            }
            T_FileStream_read(is, this_obj->fIndex, sizeof(*(this_obj->fIndex)) * UCMPE32_kIndexCount);
        }
        else
        {
            this_obj->fBogus = TRUE;
            return;
        }
        /* char instead of int8_t for Mac compilation*/
        T_FileStream_read(is, (char*)&c, sizeof(c));
        this_obj->fCompact = (UBool)(c != 0);
    }
}

U_CAPI void ucmpe32_streamOut(CompactEIntArray* this_obj, FileStream* os)
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
            int32_t len = UCMPE32_kIndexCount;
            T_FileStream_write(os, &len, sizeof(len));
            T_FileStream_write(os, this_obj->fIndex, sizeof(*(this_obj->fIndex)) * UCMPE32_kIndexCount);
        }
        c = (char)(this_obj->fCompact ? 1 : 0);  /* char instead of int8_t for Mac compilation*/
        T_FileStream_write(os, (const char*)&c, sizeof(c));
    }
}

U_CAPI void ucmpe32_streamMemIn(CompactEIntArray* this_obj, UMemoryStream* is)
{
int32_t  newCount, len;
char c;
    if (!uprv_mstrm_error(is))
    {
        
        uprv_mstrm_read(is, &newCount, sizeof(newCount));
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
        uprv_mstrm_read(is, this_obj->fArray, sizeof(*(this_obj->fArray)) * this_obj->fCount);
        uprv_mstrm_read(is, &len, sizeof(len));
        if (len == 0)
        {
            uprv_free(this_obj->fIndex);
            this_obj->fIndex = 0;
        }
        else if (len == UCMPE32_kIndexCount)
        {
            if (this_obj->fIndex == 0) 
                this_obj->fIndex =(uint16_t*)uprv_malloc(UCMPE32_kIndexCount * sizeof(uint16_t));
            if (!this_obj->fIndex) {
                this_obj->fBogus = TRUE;
                uprv_free(this_obj->fArray); 
                this_obj->fArray = 0;
                return;
            }
            uprv_mstrm_read(is, this_obj->fIndex, sizeof(*(this_obj->fIndex)) * UCMPE32_kIndexCount);
        }
        else
        {
            this_obj->fBogus = TRUE;
            return;
        }
        /* char instead of int8_t for Mac compilation*/
        uprv_mstrm_read(is, (char*)&c, sizeof(c));
        this_obj->fCompact = (UBool)(c != 0);
    }
}

U_CAPI void ucmpe32_streamMemOut(CompactEIntArray* this_obj, UMemoryStream* os)
{
    char c;
if (!uprv_mstrm_error(os))
    {
        if (this_obj->fCount != 0 && this_obj->fArray != 0)
        {
            uprv_mstrm_write(os, (uint8_t *)&(this_obj->fCount), sizeof(this_obj->fCount));
            uprv_mstrm_write(os, (uint8_t *)this_obj->fArray, sizeof(*(this_obj->fArray)) * this_obj->fCount);
        }
        else
        {
            int32_t  zero = 0;
            uprv_mstrm_write(os, (uint8_t *)&zero, sizeof(zero));
        }

        if (this_obj->fIndex == 0)
        {
            int32_t len = 0;
            uprv_mstrm_write(os, (uint8_t *)&len, sizeof(len));
        }
        else
        {
            int32_t len = UCMPE32_kIndexCount;
            uprv_mstrm_write(os, (uint8_t *)&len, sizeof(len));
            uprv_mstrm_write(os, (uint8_t *)this_obj->fIndex, sizeof(*(this_obj->fIndex)) * UCMPE32_kIndexCount);
        }
        c = (char)(this_obj->fCompact ? 1 : 0);  /* char instead of int8_t for Mac compilation*/
        uprv_mstrm_write(os, (uint8_t *)&c, sizeof(c));
    }
}


CompactEIntArray* ucmpe32_openAdopt(uint16_t *indexArray,
                                  int32_t *newValues,
                                  int32_t count)
{
  CompactEIntArray* this_obj = (CompactEIntArray*) uprv_malloc(sizeof(CompactEIntArray));

  ucmpe32_initAdopt(this_obj, indexArray, newValues, count);
  this_obj->fIAmOwned = FALSE;
  return this_obj;
}

CompactEIntArray* ucmpe32_openAlias(uint16_t *indexArray,
                  int32_t *newValues,
                  int32_t count)
{
  CompactEIntArray* this_obj = (CompactEIntArray*) uprv_malloc(sizeof(CompactEIntArray));

  ucmpe32_initAlias(this_obj, indexArray, newValues, count);
  this_obj->fIAmOwned = FALSE;
  return this_obj;
}

CompactEIntArray* ucmpe32_openFromData(      const uint8_t **source, 
                                           UErrorCode *status)
{
  CompactEIntArray* this_obj = (CompactEIntArray*) uprv_malloc(sizeof(CompactEIntArray));

  ucmpe32_initFromData(this_obj, source, status);
  this_obj->fIAmOwned = FALSE;
  return this_obj;
}
/*=======================================================*/
 
CompactEIntArray* ucmpe32_initAdopt(CompactEIntArray* this_obj,
                                  uint16_t *indexArray,
                                  int32_t *newValues,
                                  int32_t  count)
{
  if (this_obj) {
    this_obj->fCount = count; 
    this_obj->fBogus = FALSE;
    this_obj->fStructSize = sizeof(CompactEIntArray);

    this_obj->fArray = newValues;
    this_obj->fIndex = indexArray;
    this_obj->fCompact = (UBool)((count < UCMPE32_kUnicodeCount) ? TRUE : FALSE);
    this_obj->fAlias = FALSE;
    this_obj->fIAmOwned = TRUE;
  }

  return this_obj;
}

CompactEIntArray* ucmpe32_initAlias(CompactEIntArray* this_obj,
                                  uint16_t *indexArray,
                                  int32_t *newValues,
                                  int32_t  count)
{
  if (this_obj) {
    this_obj->fCount = count; 
    this_obj->fBogus = FALSE;
    this_obj->fStructSize = sizeof(CompactEIntArray);

    this_obj->fArray = newValues;
    this_obj->fIndex = indexArray;
    this_obj->fCompact = (UBool)((count < UCMPE32_kUnicodeCount) ? TRUE : FALSE);
    this_obj->fAlias = TRUE;
    this_obj->fIAmOwned = TRUE;
  }

  return this_obj;
}
/*=======================================================*/

void ucmpe32_close(CompactEIntArray* this_obj) 
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
    if(!this_obj->fIAmOwned) { /* Called if 'init' was called instead of 'open'. */
        uprv_free(this_obj);
    }
  }
}

UBool ucmpe32_isBogus(const CompactEIntArray* this_obj)
{
    return (UBool)(this_obj == NULL || this_obj->fBogus);
}

void ucmpe32_expand(CompactEIntArray* this_obj) {
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
        tempArray = (int32_t*)uprv_malloc(UCMPE32_kUnicodeCount * sizeof(int32_t));
        if (tempArray == NULL) {
            this_obj->fBogus = TRUE;
            return;
        }
        for (i = 0; i < UCMPE32_kUnicodeCount; ++i) {
            tempArray[i] = ucmpe32_get(this_obj, (UChar)i);  /* HSYS : How expand?*/
        }
        for (i = 0; i < UCMPE32_kIndexCount; ++i) {
            this_obj->fIndex[i] = (uint16_t)(i<<UCMPE32_kBlockShift);
        }
        uprv_free(this_obj->fArray); 
        this_obj->fArray = tempArray;
        this_obj->fCompact = FALSE;
    }
}

uint32_t ucmpe32_getCount(const CompactEIntArray* this_obj)
{
    return this_obj->fCount;
}

const int32_t* ucmpe32_getArray(const CompactEIntArray* this_obj)
{
    return this_obj->fArray;
}

const uint16_t* ucmpe32_getIndex(const CompactEIntArray* this_obj)
{
    return this_obj->fIndex;
}


void ucmpe32_setRange(CompactEIntArray* this_obj, UChar start, UChar end, int32_t value)
{
    int32_t i;
    if (this_obj->fCompact == TRUE) {
        ucmpe32_expand(this_obj);
        if (this_obj->fBogus) return;

    }
    for (i = start; i <= end; ++i) {
        this_obj->fArray[i] = value;
    }
}

U_CAPI  uint32_t U_EXPORT2 ucmpe32_flattenMem (const CompactEIntArray* array, UMemoryStream *MS)
{
  int32_t size = 0;

  uprv_mstrm_write32(MS, ICU_UCMPE32_VERSION);
  size += 4;
  
  uprv_mstrm_write32(MS, array->fCount);
  size += 4;

  uprv_mstrm_writeBlock(MS, array->fIndex, sizeof(array->fIndex[0])*UCMPE32_kIndexCount);
  size += sizeof(array->fIndex[0])*UCMPE32_kIndexCount;
  
  uprv_mstrm_writeBlock(MS, array->fArray, sizeof(array->fArray[0])*array->fCount);
  size += sizeof(array->fArray[0])*array->fCount;
  
  while(size%4) /* end padding */
  {
      uprv_mstrm_writePadding(MS, 1); /* Pad total so far to even size */
      size += 1;
  }

  return size;
}

U_CAPI  void U_EXPORT2 ucmpe32_initFromData(CompactEIntArray *this_obj, const uint8_t **source, UErrorCode *status)
{
  uint32_t i;
  const uint8_t *oldSource = *source;

  if(U_FAILURE(*status))
    return;

 this_obj->fArray = NULL;
 this_obj->fIndex = NULL; 
 this_obj->fBogus = FALSE;
 this_obj->fStructSize = sizeof(CompactEIntArray);
 this_obj->fCompact = TRUE;
 this_obj->fAlias = TRUE;
 this_obj->fIAmOwned = TRUE;
  
 i = * ((const uint32_t*) *source);
 (*source) += 4;

 if(i != ICU_UCMPE32_VERSION)
 {
   *status = U_INVALID_FORMAT_ERROR;
   return;
 }
  
 this_obj->fCount = * ((const uint32_t*)*source);
 (*source) += 4;

 this_obj->fIndex = (uint16_t*) *source;
 (*source) += sizeof(this_obj->fIndex[0])*UCMPE32_kIndexCount;

 this_obj->fArray = (int32_t*) *source;
 (*source) += sizeof(this_obj->fArray[0])*this_obj->fCount;

 /* eat up padding */
 while((*source-(oldSource))%4)
    (*source)++;
}


/* Stuff that might become handy later. From Markuses code*/

#if 0
                                  extern void
                                  generateData(const char *dataDir) {
                                      UNewDataMemory *pData;
                                      uint16_t *p16;
                                      UErrorCode errorCode=U_ZERO_ERROR;
                                      uint32_t size, dataLength;
                                      uint16_t i;

                                      size=
                                          _UCMPE32_INDEX_TOP*2+
                                          stage1Top*2+
                                          norm32TableTop*4+
                                          extraMem->index*2+
                                          combiningTableTop*2+
                                          fcdStage1Top*2+
                                          fcdTableTop*2;

                                      printf("size of " DATA_NAME "." DATA_TYPE " contents: %lu bytes\n", (long)size);
                                      /* adjust the stage 1 indexes to offset stage 2 from the beginning of stage 1 */

                                      /* stage1/norm32Table */
                                      for(i=0; i<stage1Top; ++i) {
                                          stage1[i]+=stage1Top/2; /* stage 2 is 32-bit indexed */
                                      }

                                      /* fcdStage1/fcdTable */
                                      for(i=0; i<fcdStage1Top; ++i) {
                                          fcdStage1[i]+=fcdStage1Top; /* FCD stage 2 is 16-bit indexed */
                                      }

                                      /* reduce the contents of fcdTable from 32-bit values to 16-bit values, in-place (destructive!) */
                                      p16=(uint16_t *)fcdTable;
                                      for(i=0; i<fcdTableTop; ++i) {
                                          p16[i]=(uint16_t)fcdTable[i];
                                      }

                                      /* write the data */
                                      pData=udata_create(dataDir, DATA_TYPE, DATA_NAME, &dataInfo,
                                                         haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
                                      if(U_FAILURE(errorCode)) {
                                          fprintf(stderr, "gennorm: unable to create the output file, error %d\n", errorCode);
                                          exit(errorCode);
                                      }

                                      udata_writeBlock(pData, indexes, sizeof(indexes));
                                      udata_writeBlock(pData, stage1, stage1Top*2);
                                      udata_writeBlock(pData, norm32Table, norm32TableTop*4);
                                      udata_writeBlock(pData, utm_getStart(extraMem), extraMem->index*2);
                                      udata_writeBlock(pData, combiningTable, combiningTableTop*2);
                                      udata_writeBlock(pData, fcdStage1, fcdStage1Top*2);
                                      udata_writeBlock(pData, fcdTable, fcdTableTop*2);

                                      /* finish up */
                                      dataLength=udata_finish(pData, &errorCode);
                                      if(U_FAILURE(errorCode)) {
                                          fprintf(stderr, "gennorm: error %d writing the output file\n", errorCode);
                                          exit(errorCode);
                                      }

                                      if(dataLength!=size) {
                                          fprintf(stderr, "gennorm: data length %lu != calculated size %lu\n",
                                              (long)dataLength, (long)size);
                                          exit(U_INTERNAL_PROGRAM_ERROR);
                                      }
                                  }

                                  /* get an existing Norm unit */
                                  static Norm *
                                  getNorm(uint32_t code) {
                                      uint32_t i;
                                      uint16_t j;

                                      /* access stage 1 and get the stage 2 block start index */
                                      i=code>>_UCMPE32_TRIE_SHIFT;
                                      j=stage1[i];
                                      if(j==0) {
                                          return NULL;
                                      }

                                      /* access stage 2 and get the Norm unit */
                                      i=(uint16_t)(j+(code&_UCMPE32_STAGE_2_MASK));
                                      j=stage2[i];
                                      if(j==0) {
                                          return NULL;
                                      } else {
                                          return norms+j;
                                      }
                                  }

#endif
