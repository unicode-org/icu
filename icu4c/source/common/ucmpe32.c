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

/* builder data ------------------------------------------------------------- */

U_CAPI CompactEIntArray* U_EXPORT2
ucmpe32_open(int32_t defaultValue, int32_t surrogateValue, int32_t leadSurrogateValue, UErrorCode *status)
{
  int32_t *bla;
  CompactEIntArray* this_obj = NULL;
  if (status == NULL || U_FAILURE(*status)) { 
    return NULL;
  }
  this_obj = (CompactEIntArray*) uprv_malloc(sizeof(CompactEIntArray));
  if (this_obj == NULL) { 
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }

  this_obj->fAlias = FALSE;

  this_obj->fStructSize = sizeof(CompactEIntArray);

  this_obj->stage1Top = _UCMPE32_STAGE_1_MAX_COUNT;
  this_obj->stage1 = (uint16_t *)uprv_malloc(_UCMPE32_STAGE_1_MAX_COUNT*sizeof(uint16_t));
  if(this_obj->stage1 == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(this_obj);
    return NULL;
  }

  
  /* reset stage 1 of the trie */
  uprv_memset(this_obj->stage1, 0, this_obj->stage1Top*sizeof(uint16_t));

  /* allocate stage 2 of the trie and reset the first block */
  this_obj->stage2= (int32_t*)uprv_malloc(INIT_UCMPE32_STAGE2_SIZE*sizeof(*(this_obj->stage2)));
  if(this_obj->stage2 == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(this_obj->stage1);
    uprv_free(this_obj);
    return NULL;
  }
  this_obj->stage2Size = INIT_UCMPE32_STAGE2_SIZE;
  this_obj->stage2DefaultTop = 0;
  this_obj->fDefaultValue = defaultValue;
  this_obj->fSurrogateValue = surrogateValue;
  this_obj->fLeadSurrogateValue = leadSurrogateValue;
  for(bla = this_obj->stage2; bla<this_obj->stage2+INIT_UCMPE32_STAGE2_SIZE; bla++) {
  /*for(bla = this_obj->stage2; bla<this_obj->stage2+_UCMPE32_STAGE_2_BLOCK_COUNT; bla++) {*/
    *bla = this_obj->fDefaultValue;
  }
  this_obj->stage2Top = _UCMPE32_STAGE_2_BLOCK_COUNT;

  this_obj->fCompact = FALSE; 
  this_obj->fBogus = FALSE;
  this_obj->fInitPhase = TRUE;

  return this_obj;
}

/*
 * Set a range of UnicodeChars to the same value 
 */
U_CAPI void U_EXPORT2
ucmpe32_setRange32(CompactEIntArray* this_obj, UChar32 start, UChar32 end, int32_t value) {
    UChar32 code = 0;

    uint32_t s1Start = start>>_UCMPE32_TRIE_SHIFT;
    uint32_t s1End = end>>_UCMPE32_TRIE_SHIFT;

    int32_t *bla;
    uint32_t i = 0;

    /* Allocate a block for repeat stuff */
    int32_t repeatBlock = 0;

    if (this_obj->fInitPhase == FALSE || this_obj->fCompact == TRUE || (start > end)) {
      return;
    }

    /* if there is stuff that doesn't fit */
    if((start & _UCMPE32_STAGE_2_MASK) != 0) { /* at the start */
      s1Start++;
      for(code = start; code < (UChar32)(s1Start<<_UCMPE32_TRIE_SHIFT); code++) {
        ucmpe32_set32(this_obj, code, value);
      }
    }

    repeatBlock = this_obj->stage2Top;
    this_obj->stage2Top += _UCMPE32_STAGE_2_BLOCK_COUNT;
    for(bla = this_obj->stage2+repeatBlock; bla<this_obj->stage2+this_obj->stage2Top; bla++) {
      *bla = value;
    }
    for(i = s1Start; i< s1End; i++) {
      this_obj->stage1[i] = (uint16_t)(repeatBlock);
    }

    if((end & _UCMPE32_STAGE_2_MASK) != 0) { /* at the end */
      for(code = (s1End<<_UCMPE32_TRIE_SHIFT); code <= end; code++) {
        ucmpe32_set32(this_obj, code, value);
      }
    } else {
      this_obj->stage1[s1End] = (uint16_t)(repeatBlock);
    }
    this_obj->stage2DefaultTop = this_obj->stage2Top;
    this_obj->fInitPhase = TRUE;

}


U_CAPI int32_t U_EXPORT2
ucmpe32_get32(CompactEIntArray* this_obj, UChar32 code) {
  int16_t stage1 = (this_obj->stage1[(code >> _UCMPE32_TRIE_SHIFT)]); 
  int32_t offset = (code & _UCMPE32_STAGE_2_MASK);
  int32_t result = this_obj->stage2[stage1 + offset];

  return result;

}

/********* THIS IS THE ADD FUNCTION ********************/
U_CAPI void  U_EXPORT2
ucmpe32_set32(CompactEIntArray* this_obj, UChar32 code, int32_t value)
{
    uint16_t stage2Block, k;

    if (this_obj->fCompact == TRUE) {
      return;
    }

    this_obj->fInitPhase = FALSE;

    {
        uint32_t i;
        uint16_t j;

        i=code>>_UCMPE32_TRIE_SHIFT;
        j=this_obj->stage1[i];
        if(j<=this_obj->stage2DefaultTop) {
            /* allocate a stage 2 block */
            int32_t *p=NULL, bla=0;           
            if(this_obj->stage2Size < (this_obj->stage2Top + _UCMPE32_STAGE_2_BLOCK_COUNT)) {
              this_obj->stage2 = (int32_t *)uprv_realloc(this_obj->stage2, 2*this_obj->stage2Size);
              if(this_obj->stage2 == NULL) {
              }
              this_obj->stage2Size *= 2;
            }
            p = this_obj->stage2+this_obj->stage2Top;
            for(bla = 0; bla<_UCMPE32_STAGE_2_BLOCK_COUNT; bla++) {
              *(p+bla) = this_obj->stage2[j+bla]; 
              /* fill the newly allocated block with the default values for that block */
            }
            this_obj->stage2Top += _UCMPE32_STAGE_2_BLOCK_COUNT;
            
            this_obj->stage1[i]=j=(uint16_t)(p-this_obj->stage2);

	} 
        stage2Block=j;
    }

    k=(uint16_t)(stage2Block+(code&_UCMPE32_STAGE_2_MASK));

    this_obj->stage2[k] = value;
}

U_CAPI void U_EXPORT2
ucmpe32_setSurrogate(CompactEIntArray* this_obj, UChar lead, UChar trail, int32_t value)
{
    if (this_obj->fCompact == TRUE) {
      return;
    }
    ucmpe32_set(this_obj, (int32_t)UTF16_GET_PAIR_VALUE(lead, trail), value);
}


/*
 * Fold the supplementary code point data for one lead surrogate.
 */
static uint16_t
foldLeadSurrogate(CompactEIntArray* this_obj, 
                  uint32_t base, int32_t top) {
    uint32_t leadNorm32=0;
    int32_t i, j, s2;
    uint32_t leadSurrogate=0xd7c0+(base>>10);

#if 0
    printf("supplementary data for lead surrogate U+%04lx\n", (long)leadSurrogate);
#endif
    /* calculate the 32-bit data word for the lead surrogate */
    for(i=0; i<_UCMPE32_SURROGATE_BLOCK_COUNT; ++i) {
        s2=this_obj->stage1[(base>>_UCMPE32_TRIE_SHIFT)+i];
        if(s2!=0) {
            for(j=0; j<_UCMPE32_STAGE_2_BLOCK_COUNT; ++j) {
                /* basically, or all 32-bit data into the one for the lead surrogate */
                leadNorm32|=this_obj->stage2[s2+j];
            }
        }
    }

    if(leadNorm32==0) {
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
    leadNorm32= this_obj->fSurrogateValue | ((top<<_UCMPE32_TRIE_SHIFT)&~_UCMPE32_STAGE_2_MASK);

    /* enter the lead surrogate's data */
    s2=this_obj->stage1[leadSurrogate>>_UCMPE32_TRIE_SHIFT];
    if(s2<=this_obj->stage2DefaultTop) {
        /* allocate a new stage 2 block in stage (the memory is there from makeAll32()/makeFCD()) */
        s2=this_obj->stage1[leadSurrogate>>_UCMPE32_TRIE_SHIFT]=(uint16_t)this_obj->stage2Top;
        for(i = 0; i<_UCMPE32_STAGE_2_BLOCK_COUNT; i++) {
          this_obj->stage2[this_obj->stage2Top+i] = this_obj->fLeadSurrogateValue;
        }
        this_obj->stage2Top+=_UCMPE32_STAGE_2_BLOCK_COUNT;
    }
    this_obj->stage2[s2+(leadSurrogate&_UCMPE32_STAGE_2_MASK)]=leadNorm32;

    /* move the actual stage 1 indexes from the supplementary position to the new one */
    uprv_memmove(this_obj->stage1+top, this_obj->stage1+(base>>_UCMPE32_TRIE_SHIFT), _UCMPE32_SURROGATE_BLOCK_COUNT*2);

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
static uint32_t
foldSupplementary(CompactEIntArray* this_obj, int32_t top) {
    uint32_t c;
    uint16_t i;

    /* search for any stage 1 entries for supplementary code points */
    for(c=0x10000; c<0x110000;) {
        i=this_obj->stage1[c>>_UCMPE32_TRIE_SHIFT];
        if(i!=0) {
            /* there is data, treat the full block for a lead surrogate */
            c&=~0x3ff;
            top+=foldLeadSurrogate(this_obj, c, top);
            c+=0x400;
        } else {
            c+=_UCMPE32_STAGE_2_BLOCK_COUNT;
        }
    }
#if 0
    printf("trie index count: BMP %u  all Unicode %lu  folded %u\n",
           _UCMPE32_STAGE_1_BMP_COUNT, (long)_UCMPE32_STAGE_1_MAX_COUNT, parentCount);
#endif
    return top;
}

U_CAPI void U_EXPORT2
ucmpe32_compact(CompactEIntArray* this_obj) {

  if(this_obj->fCompact == FALSE) { /* compacting can be done only once */
    /*
     * This function is the common implementation for compacting
     * the stage 2 tables of 32-bit values.
     * It is a copy of genprops/store.c's compactStage() adapted for the 32-bit stage 2 tables.
     */
    static uint16_t map[0x10000>>_UCMPE32_TRIE_SHIFT];
    int32_t x;
    uint16_t i, start, prevEnd, newStart;

    /* fold supplementary code points into lead surrogates */
    this_obj->stage1Top=foldSupplementary(this_obj, _UCMPE32_STAGE_1_BMP_COUNT);
    map[0]=0;
    newStart=_UCMPE32_STAGE_2_BLOCK_COUNT;
    for(start=newStart; start<this_obj->stage2Top;) {
        prevEnd=(uint16_t)(newStart-1);
        x=this_obj->stage2[start];
        if(x==this_obj->stage2[prevEnd]) {
            /* overlap by at least one */
            for(i=1; i<_UCMPE32_STAGE_2_BLOCK_COUNT 
              && x==this_obj->stage2[start+i] 
              && x==this_obj->stage2[prevEnd-i]; ++i) {}

            /* overlap by i */
            map[start>>_UCMPE32_TRIE_SHIFT]=(uint16_t)(newStart-i);

            /* move the non-overlapping indexes to their new positions */
            start+=i;
            for(i=(uint16_t)(_UCMPE32_STAGE_2_BLOCK_COUNT-i); i>0; --i) {
                this_obj->stage2[newStart++]=this_obj->stage2[start++];
            }
        } else if(newStart<start) {
            /* move the indexes to their new positions */
            map[start>>_UCMPE32_TRIE_SHIFT]=newStart;
            for(i=_UCMPE32_STAGE_2_BLOCK_COUNT; i>0; --i) {
                this_obj->stage2[newStart++]=this_obj->stage2[start++];
            }
        } else /* no overlap && newStart==start */ {
            map[start>>_UCMPE32_TRIE_SHIFT]=start;
            newStart+=_UCMPE32_STAGE_2_BLOCK_COUNT;
            start=newStart;
        }
    }

    /* now adjust the stage1 table */
    for(i=0; i<this_obj->stage1Top; ++i) {
        this_obj->stage1[i]=map[this_obj->stage1[i]>>_UCMPE32_TRIE_SHIFT];
    }
#if 0
    /* we saved some space */
    printf("compacting trie: count of 32-bit words %lu->%lu\n",
            (long)this_obj->stage2Top, (long)newStart);
#endif
    this_obj->stage2Top = newStart;
    this_obj->fCompact = TRUE;
  }
}


U_CAPI CompactEIntArray* U_EXPORT2
ucmpe32_clone(CompactEIntArray* orig, UErrorCode *status)
{
  CompactEIntArray* this_obj = NULL;
  if (status == NULL || U_FAILURE(*status)) { 
    return NULL;
  }
  this_obj = (CompactEIntArray*) uprv_malloc(sizeof(CompactEIntArray));
  if(orig == NULL || orig->fBogus == TRUE || this_obj == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }
  this_obj->fAlias = FALSE;

  this_obj->fDefaultValue = orig->fDefaultValue;
  this_obj->fSurrogateValue = orig->fSurrogateValue;
  this_obj->fLeadSurrogateValue = orig->fLeadSurrogateValue;

  this_obj->stage1Top = orig->stage1Top;
  this_obj->stage1 = (uint16_t *)uprv_malloc(this_obj->stage1Top*sizeof(uint16_t));
  if(this_obj->stage1 == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(this_obj);
    return NULL;
  }
  uprv_memcpy(this_obj->stage1, orig->stage1, this_obj->stage1Top*sizeof(uint16_t));

  this_obj->stage2Size = orig->stage2Size;
  this_obj->stage2DefaultTop = orig->stage2DefaultTop;
  this_obj->stage2Top = orig->stage2Top; 
  this_obj->stage2 = (int32_t*)uprv_malloc(60000*sizeof(*(this_obj->stage2)));
  if(this_obj->stage2 == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(this_obj->stage1);
    uprv_free(this_obj);
    return NULL;
  }
  uprv_memcpy(this_obj->stage2, orig->stage2, this_obj->stage2Top*sizeof(*(this_obj->stage2)));

  this_obj->fBogus = FALSE;
  this_obj->fStructSize = sizeof(CompactEIntArray);

  this_obj->fCompact = orig->fCompact;
  this_obj->fInitPhase = orig->fInitPhase;

  return this_obj;
}


U_CAPI CompactEIntArray*  U_EXPORT2
ucmpe32_openFromData(      const uint8_t **source, 
                                           UErrorCode *status)
{
  uint32_t i;
  CompactEIntArray* this_obj = NULL;

  if (status == NULL || U_FAILURE(*status)) { 
    return NULL;
  }

  this_obj = (CompactEIntArray*) uprv_malloc(sizeof(CompactEIntArray));

  if(*source == NULL || this_obj == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }

  i = * ((const uint32_t*) *source);
  (*source) += 4;

  if(i != ICU_UCMPE32_VERSION)
  {
   *status = U_INVALID_FORMAT_ERROR;
   return NULL;
  }

  this_obj->fAlias = TRUE;
  this_obj->stage1 = NULL;
  this_obj->stage2 = NULL; 
  this_obj->fBogus = FALSE;
  this_obj->fStructSize = sizeof(CompactEIntArray);
  this_obj->fCompact = TRUE;

  this_obj->stage1Top = * ((const uint32_t*)*source);
  (*source) += 4;

  this_obj->stage1 = (uint16_t*) *source;
  (*source) += sizeof(this_obj->stage1[0])*this_obj->stage1Top;

  this_obj->stage2Top = * ((const uint32_t*)*source);
  (*source) += 4;

  this_obj->stage2 = (int32_t*) *source;
  (*source) += sizeof(this_obj->stage2[0])*this_obj->stage2Top;

  return this_obj;
}

U_CAPI uint32_t U_EXPORT2
ucmpe32_flattenMem (const CompactEIntArray* this_obj, UMemoryStream *MS)
{
  /* This dumps stuff in memory */
  /* there is no padding, as there is always an even number of 16-bit values */
  /* (stage1), so everything is always 32 bit aligned                        */
  
  int32_t size = 0;
  if(this_obj->fCompact == TRUE) {
    uprv_mstrm_write32(MS, ICU_UCMPE32_VERSION);
    size += 4;

    uprv_mstrm_write32(MS, this_obj->stage1Top);
    size += 4;
  
    uprv_mstrm_writeBlock(MS, this_obj->stage1, this_obj->stage1Top*sizeof(this_obj->stage1[0]));
    size += this_obj->stage1Top*sizeof(this_obj->stage1[0]);
  
    uprv_mstrm_write32(MS, this_obj->stage2Top);
    size += 4;

    uprv_mstrm_writeBlock(MS, this_obj->stage2, this_obj->stage2Top*sizeof(this_obj->stage2[0]));
    size += this_obj->stage2Top*sizeof(this_obj->stage2[0]);
  }
  return size;
}

/*=======================================================*/

U_CAPI void  U_EXPORT2
ucmpe32_close(CompactEIntArray* this_obj) 
{
  if(this_obj != NULL) {
    if(this_obj->fAlias == FALSE) {
      if(this_obj->stage1 != NULL) {
        uprv_free(this_obj->stage1);
      }
      if(this_obj->stage2 != NULL) {
        uprv_free(this_obj->stage2);
      }
    }
    uprv_free(this_obj);
  }
}

U_CAPI int32_t  U_EXPORT2
ucmpe32_getSurrogateEx(CompactEIntArray *array, UChar lead, UChar trail) {
  if(array->fCompact == FALSE) {
    return(ucmpe32_get(array, (int32_t)UTF16_GET_PAIR_VALUE(lead, trail)));
  } else {
    return(ucmpe32_getSurrogate(array, ucmpe32_get(array, lead), trail));
  }
}

/*=======================================================*/
/* retrieval stuff as functions */
#if 0
int32_t 
ucmpe32_get32(CompactEIntArray *this_obj, UChar32 index) {
  int32_t index_lookup = this_obj->stage1[index >> _UCMPE32_TRIE_SHIFT] ;
  int32_t addition = (index & _UCMPE32_STAGE_2_MASK);

  return (this_obj->stage2[index_lookup + addition]);
}


/* Lead surrogate data needs to be in the following format: */
/* F50XXY000 - where X mask is 1111 (F) and Y mask is 1100 (C) */
/* The ten bits for access will be in the middle of the field  */
int32_t 
ucmpe32_getSurrogate(CompactEIntArray *array, int32_t leadValue32, UChar trail) {
  int32_t c = ((leadValue32 & 0xffc00) | (trail & 0x3ff)); 
  int32_t index_lookup = array->stage1[(c >> _UCMPE32_TRIE_SHIFT)];
  int32_t addition = (c & _UCMPE32_STAGE_2_MASK);
  return (array->stage2[index_lookup+ addition]);
}
#endif
