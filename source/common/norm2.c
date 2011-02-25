/* look for TODO  or 'unimp'  */

/*
*******************************************************************************
*
*   Copyright (C) 2009-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  normalizer2.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009nov22
*   created by: Markus W. Scherer
*
*   ported from normalizer2.cpp on 2011-feb-15 by srl into C
*/

#include "unicode/utypes.h"

#include "unicode/unorm.h"
#include "umutex.h"
/*#include "normalizer2impl.h"*/
#include "ucln_cmn.h"
#include "uhash.h"
#include "cmemory.h"
#include "udatamem.h"

#if  defined(ICU4C0)
#ifndef UNORM_DEBUG
/* #define UNORM_DEBUG 1 */
#endif
#ifdef UNORM_DEBUG
#include <stdio.h>
#include <ctype.h>
static char  dbg_buf[256];
const char *dbg_uchars(UChar *u) {
  int32_t n;
  int32_t i;
  int32_t c=0;
  dbg_buf[0]=0;
  for(i=0;u[i]&&(i<20);i++) {
    UChar ch = u[i];
    c+=strlen(dbg_buf+c);
    if(ch<0x7f && isprint((char)ch)) {
      printf(dbg_buf+c,"'%c', ", (char)ch);
    } else {
      printf(dbg_buf+c,"U+%04X, ", ch);
    }
  }
  return dbg_buf;
}
#endif

#define UNORM_ENABLE_FCD 0  /* enables FCD and other modes. Not implemented. */
#include "ustr_imp.h"
#include "unicode/ustring.h"
#include "norm2imp.h"

#if 0

#ifndef UNORM_DEBUG
/* for unimp */
#include <stdio.h>
#endif

static UBool _unimp(UErrorCode *e, const char *f, int l) {
  printf("%s:%d: ERROR: unimplemented!!!\n", f, l);
  *e = U_REGEX_UNIMPLEMENTED;
  return FALSE;
}

#define unimp(e)  _unimp(e,__FILE__,__LINE__)
#else
/* no definition of unimp */
#endif

#define fcdTrie() (_this->newFCDTrie)
#define getFCD16(c)  UTRIE2_GET16(_this->newFCDTrie, c)
#define getFCD16FromSingleLead(c) UTRIE2_GET16_FROM_U16_SINGLE_LEAD(fcdTrie(), c)
#define getFCD16FromSupplementary(c) UTRIE2_GET16_FROM_SUPP(fcdTrie(), c)
#define getFCD16FromSurrogatePair(c,x) getFCD16FromSupplementary(U16_GET_SUPPLEMENTARY(c, x))
#define getMapping(x) (_this->extraData+(x))
#define getNorm16(x) (UTRIE2_GET16(_this->normTrie,(x)))
#define isCompYesAndZeroCC(x) ((x)<_this->minNoNo)
#define isMaybeOrNonZeroCC(norm16) ((norm16)>=_this->minMaybeYes)
#define isDecompNoAlgorithmic(norm16) ((norm16)>=_this->limitNoNo)
#define isInert(norm16) ((norm16)==0)
#define isMaybe(norm16) (_this->minMaybeYes<=(norm16) && (norm16)<=JAMO_VT)
#define mapAlgorithmic(c, norm16) ((c)+(norm16)-(_this->minMaybeYes-MAX_DELTA-1))


/* some prototypes - not all */
static UBool ReorderingBuffer_appendZeroCCStr(ReorderingBuffer *buffer, const UChar *s, const UChar *sLimit, UErrorCode *errorCode);

static UBool
Normalizer2_comp_compose(Normalizer2 *_this, const UChar *src, const UChar *limit,
                         UBool onlyContiguous,
                         UBool doCompose,
                         ReorderingBuffer *buffer,
                         UErrorCode *errorCode)  ;


U_DRAFT const UNormalizer2 * U_EXPORT2
unorm2_get2Instance(const char *packageName,
                   const char *name,
                   UNormalizationMode mode,
                    UErrorCode *errorCode);

static const UChar *
Normalizer2_decomp_decompose(Normalizer2 *_this, const UChar *src, const UChar *limit,
                           ReorderingBuffer *buffer,
                             UErrorCode *errorCode);

static const UChar *Normalizer2Impl_findPreviousCompBoundary(Normalizer2 *_this, const UChar *start, const UChar *p);

static const UChar *Normalizer2Impl_findNextCompBoundary(Normalizer2 *_this, const UChar *p, const UChar *limit);

/** end prototypes **/

#ifdef UNORM_DEBUG
#define MODENAME_STR \
  "____\0" \
  "NONE\0" \
  "NFD \0" \
  "NFKD\0" \
  "NFC \0" \
  "NFKC\0" \
  "FCD \0" \
  "!!!!\0"

#define MODENAME(x)  (MODENAME_STR+((int)x)*5)

#define MODE2NAME_STR \
  "COMP\0" \
  "DECM\0" \
  "FCD \0" \
  "FCC \0" \
  "???1\0" \
  "???2\0" \
  "???3\0" \
  "!!!!\0"

#define MODE2NAME(x)  (MODE2NAME_STR+((int)x-(int)UNORM2_COMPOSE)*5)
#endif

/* ---- FACTORY ---- */


static Normalizer2 **singletons = NULL;

static UNormalizer2 *getSingleton(UNormalizationMode mode, const char *str, UErrorCode *errorCode) {
  Normalizer2 *ret = NULL;
  Normalizer2 *newOne = NULL;
  Normalizer2 **theSingletons = NULL;
  UMTX_CHECK(NULL,singletons,theSingletons);
  if(theSingletons == NULL) {
    Normalizer2 **list = (Normalizer2**)uprv_malloc(sizeof(Normalizer2*)*UNORM_MODE_COUNT);
    uprv_memset(list, sizeof(Normalizer2*)*UNORM_MODE_COUNT,0);
    umtx_lock(NULL);
    if(singletons == NULL) {
      singletons=list;
      list=NULL;
    }
    umtx_unlock(NULL);
    if(list!=NULL) {
      uprv_free(list); /* someone beat us to it. */
    }
    if(singletons==NULL) {
      *errorCode = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  }

  UMTX_CHECK(NULL,(singletons[(int)mode]),ret);
  if(ret!=NULL) {
    return (UNormalizer2*)ret;
  }
  
  /* make up */
  newOne = (Normalizer2*)unorm2_get2Instance(NULL,str,mode,errorCode);
  if(U_FAILURE(*errorCode)) {
    unorm2_close((UNormalizer2*)newOne);
    return NULL;
  } else if(newOne==NULL) {
    *errorCode = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }
  ret = newOne;
  /* put it in the cache */
  umtx_lock(NULL);
  if(singletons[(int)mode]==NULL) {
    singletons[(int)mode] = newOne;
    newOne = NULL;
  } else {
    ret = singletons[(int)mode];
  }
  umtx_unlock(NULL);
  if(newOne!=NULL) {
    unorm2_close((UNormalizer2*)newOne);
  }

  return (UNormalizer2*)ret;
}

static const UNormalizer2 *
Normalizer2Factory_getInstance(UNormalizationMode mode, UErrorCode *errorCode) {
    if(U_FAILURE(*errorCode)) {
        return NULL;
    }
    switch(mode) {
#if UNORM_ENABLE_FCD
    case UNORM_NFD:
      return getSingleton(mode, "nfc", errorCode);
    case UNORM_NFKD:
      return getSingleton(mode, "nfkc", errorCode);
#endif
    case UNORM_NFC:
      return getSingleton(mode, "nfc", errorCode);
#if UNORM_ENABLE_FCD
    case UNORM_NFKC:
      return getSingleton(mode, "nfkc", errorCode);
    case UNORM_FCD:
      return getSingleton(mode, "nfc", errorCode);
#endif
    default:  /* UNORM_NONE */
      *errorCode = U_REGEX_UNIMPLEMENTED; /* not implemented */
#if defined(UNORM_DEBUG)
      fprintf(stderr, "Loading noop for mode #%d=%s\n",(int)mode, MODENAME(mode));
#endif
    case UNORM_NONE:
      return getSingleton(mode, NULL, errorCode);
    }
}

/* INSTANCE */

static UBool U_CALLCONV
_isAcceptable(void *context,
                              const char *type, const char *name,
                              const UDataInfo *pInfo) {
    if(
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x4e &&    /* dataFormat="Nrm2" */
        pInfo->dataFormat[1]==0x72 &&
        pInfo->dataFormat[2]==0x6d &&
        pInfo->dataFormat[3]==0x32 &&
        pInfo->formatVersion[0]==1
    ) {
        Normalizer2 *me=(Normalizer2 *)context;
        uprv_memcpy(me->dataVersion, pInfo->dataVersion, 4);
        return TRUE;
    } else {
        return FALSE;
    }
}

static void Normalizer2_load(Normalizer2 *_this, const char *packageName, const char *name,  UErrorCode *errorCode) {
  /* from normalizer2impl.cpp: Normalizr2Impl::load */
  _this->memory=udata_openChoice(packageName, "nrm", name, _isAcceptable, _this, errorCode);

  if(U_FAILURE(*errorCode)) {
#if defined(UNORM_DEBUG)
    fprintf(stderr, "%s:%d: error %s, can't open data %s/%s.%s\n", __FILE__, __LINE__, u_errorName(*errorCode), packageName?packageName:"<NULL>", name, "nrm");
#endif
    return;
  }

  {
    const uint8_t *inBytes=(const uint8_t *)udata_getMemory(_this->memory);
    const int32_t *inIndexes=(const int32_t *)inBytes;
    int32_t indexesLength=inIndexes[IX_NORM_TRIE_OFFSET]/4;
    if(indexesLength<=IX_MIN_MAYBE_YES) {
      *errorCode=U_INVALID_FORMAT_ERROR;  /* Not enough indexes. */
      return;
    }
    
    _this->minDecompNoCP=inIndexes[IX_MIN_DECOMP_NO_CP];
    _this->minCompNoMaybeCP=inIndexes[IX_MIN_COMP_NO_MAYBE_CP];

    _this->minYesNo=inIndexes[IX_MIN_YES_NO];
    _this->minNoNo=inIndexes[IX_MIN_NO_NO];
    _this->limitNoNo=inIndexes[IX_LIMIT_NO_NO];
    _this->minMaybeYes=inIndexes[IX_MIN_MAYBE_YES];

    {
      int32_t offset=inIndexes[IX_NORM_TRIE_OFFSET];
      int32_t nextOffset=inIndexes[IX_EXTRA_DATA_OFFSET];
      _this->normTrie=utrie2_openFromSerialized(UTRIE2_16_VALUE_BITS,
                                                inBytes+offset, nextOffset-offset, NULL,
                                                errorCode);
      if(U_FAILURE(*errorCode)) {
        return;
      }

      offset=nextOffset;
      _this->maybeYesCompositions=(const uint16_t *)(inBytes+offset);
      _this->extraData=_this->maybeYesCompositions+(MIN_NORMAL_MAYBE_YES-_this->minMaybeYes);
    }
  }
}

#if UNORM_ENABLE_FCD

static void Normalizer2Impl_setFCD16FromNorm16(Normalizer2 *_this, UChar32 start, UChar32 end, uint16_t norm16,
                                               UTrie2 *newFCDTrie, UErrorCode *errorCode)  {
    /*  Only loops for 1:1 algorithmic mappings. */
    for(;;) {
        if(norm16>=MIN_NORMAL_MAYBE_YES) {
            norm16&=0xff;
            norm16|=norm16<<8;
        } else if(norm16<=_this->minYesNo || _this->minMaybeYes<=norm16) {
            /*  no decomposition or Hangul syllable, all zeros */
            break;
        } else if(_this->limitNoNo<=norm16) {
            int32_t delta=norm16-(_this->minMaybeYes-MAX_DELTA-1);
            if(start==end) {
                start+=delta;
                norm16=getNorm16(start);
            } else {
                /*  the same delta leads from different original characters to different mappings */
                do {
                    UChar32 c=start+delta;
                    Normalizer2Impl_setFCD16FromNorm16(_this,c, c, getNorm16(c), newFCDTrie, errorCode);
                } while(++start<=end);
                break;
            }
        } else {
            /*  c decomposes, get everything from the variable-length extra data */
          const uint16_t *mapping= _this->extraData+norm16; /* getMapping(norm16); */
            uint16_t firstUnit=*mapping;
            if((firstUnit&MAPPING_LENGTH_MASK)==0) {
                /*  A character that is deleted (maps to an empty string) must */
                /*  get the worst-case lccc and tccc values because arbitrary */
                /*  characters on both sides will become adjacent. */
                norm16=0x1ff;
            } else {
                if(firstUnit&MAPPING_HAS_CCC_LCCC_WORD) {
                    norm16=mapping[1]&0xff00;  /*  lccc */
                } else {
                    norm16=0;
                }
                norm16|=firstUnit>>8;  /*  tccc */
            }
        }
        utrie2_setRange32(newFCDTrie, start, end, norm16, TRUE, errorCode);
        break;
    }
}



/* Collect (OR together) the FCD values for a range of supplementary characters, */
/* for their lead surrogate code unit. */
static UBool U_CALLCONV
enumRangeOrValue(const void *context, UChar32 start, UChar32 end, uint32_t value) {
    *((uint32_t *)context)|=value;
    return TRUE;
}


/* Set the FCD value for a range of same-norm16 characters. */
static UBool U_CALLCONV
enumRangeHandler(void *context, UChar32 start, UChar32 end, uint32_t value) {
  Normalizer2 *_this = (Normalizer2*)context;
  if(value!=0) {
    Normalizer2Impl_setFCD16FromNorm16(_this, start, end, (uint16_t)value, _this->newFCDTrie, &(_this->fcdErrorCode));
  }
  return (U_SUCCESS(_this->fcdErrorCode));
}

static UTrie2 *FCDTrieSingleton_createInstance(Normalizer2 *me, UErrorCode *errorCode) {
    me->newFCDTrie=utrie2_open(0, 0, errorCode);
    if(U_SUCCESS(*errorCode)) {
      UChar lead;
      utrie2_enum(me->normTrie, NULL, enumRangeHandler, me);
      for(lead=0xd800; lead<0xdc00; ++lead) {
        uint32_t oredValue=utrie2_get32(me->newFCDTrie, lead);
        utrie2_enumForLeadSurrogate(me->newFCDTrie, lead, NULL, enumRangeOrValue, &oredValue);
        if(oredValue!=0) {
          /* Set a "bad" value for makeFCD() to break the quick check loop */
          /* and look up the value for the supplementary code point. */
          /* If there is any lccc, then set the worst-case lccc of 1. */
          /* The ORed-together value's tccc is already the worst case. */
          if(oredValue>0xff) {
            oredValue=0x100|(oredValue&0xff);
          }
          utrie2_set32ForLeadSurrogateCodeUnit(me->newFCDTrie, lead, oredValue, errorCode);
        }
      }
      utrie2_freeze(me->newFCDTrie, UTRIE2_16_VALUE_BITS, errorCode);
      if(U_SUCCESS(*errorCode)) {
        return me->newFCDTrie;
      }
    }
    utrie2_close(me->newFCDTrie);
    me->newFCDTrie=NULL;
    return NULL;
}

#endif

static  void U_CALLCONV Normalizer2_close(struct Normalizer2* _this) {
#if UNORM_ENABLE_FCD
  utrie2_close(_this->newFCDTrie);
#endif
  udata_close(_this->memory);
  utrie2_close(_this->normTrie);
}

static uint8_t getCCFromYesOrMaybe(uint16_t norm16) {
  return norm16>=MIN_NORMAL_MAYBE_YES ? (uint8_t)norm16 : 0;
}
static UBool isMostDecompYesAndZeroCC(Normalizer2* _this, uint16_t norm16)  {
        return norm16<_this->minYesNo || norm16==MIN_NORMAL_MAYBE_YES || norm16==JAMO_VT;
    }

static UBool isDecompYes(Normalizer2* _this,uint16_t norm16)  { return norm16<_this->minYesNo || _this->minMaybeYes<=norm16; }

static UNormalizationCheckResult U_CALLCONV Normalizer2_noop_quickCheck(struct Normalizer2* n, const UChar *s, int32_t length, UErrorCode *pErrorCode) {
  return UNORM_YES; 
}


static int32_t Hangul_decompose(UChar32 c, UChar buffer[3]) {
  c-=HANGUL_BASE;
  {
  UChar32 c2=c%JAMO_T_COUNT;
  c/=JAMO_T_COUNT;
  buffer[0]=(UChar)(JAMO_L_BASE+c/JAMO_V_COUNT);
  buffer[1]=(UChar)(JAMO_V_BASE+c%JAMO_V_COUNT);
  if(c2==0) {
    return 2;
  } else {
    buffer[2]=(UChar)(JAMO_T_BASE+c2);
    return 3;
  }
  }
}

static UChar *ReorderingBuffer_getLimit(ReorderingBuffer* buffer) {
  return buffer->limit;
}
static UChar *ReorderingBuffer_getStart(ReorderingBuffer* buffer) {
  return buffer->start;
}
static UBool ReorderingBuffer_isEmpty(ReorderingBuffer* buffer) {
  return buffer->start==buffer->limit;
}
static void ReorderingBuffer_setLastChar(ReorderingBuffer* buffer, UChar c) {
  *(buffer->limit-1)=c;
}
static void ReorderingBuffer_setReorderingLimit(ReorderingBuffer* buffer, UChar* newLimit) {
  buffer->remainingCapacity+=(int32_t)(buffer->limit-newLimit);
  buffer->reorderStart=buffer->limit=newLimit;
  buffer->lastCC=0;
}

static void ReorderingBuffer_construct(ReorderingBuffer *buffer, Normalizer2 *n, UChar *dest, int32_t capacity) {
  buffer->impl = n;
  buffer->str = dest;
  buffer->lastCC=0;
  buffer->limit = dest;
  buffer->reorderStart=NULL;
  buffer->start=dest;
  buffer->capacity = capacity;
  buffer->remainingCapacity=capacity;
}

static UBool ReorderingBuffer_resize(ReorderingBuffer *buffer, int32_t appendLength, UErrorCode *errorCode) {
    int32_t reorderStartIndex=(int32_t)(buffer->reorderStart-buffer->start);
    int32_t length=(int32_t)(buffer->limit-buffer->start);
    /* str.releaseBuffer(length); */
    int32_t newCapacity=length+appendLength;
    int32_t doubleCapacity=2*buffer->capacity;
    if(newCapacity<doubleCapacity) {
        newCapacity=doubleCapacity;
    }
    if(newCapacity<256) {
        newCapacity=256;
    }
    if(buffer->start!=buffer->str) {
      /* it's not the string passedin -- resize it */
      buffer->start=uprv_realloc(buffer->start,newCapacity);
    } else {
      /* ran out of room- make a new buffer */
      buffer->start=uprv_malloc(newCapacity);
      memcpy(buffer->start,buffer->str,length*sizeof(buffer->start[0]));
    }
    if(buffer->start==NULL) {
        /* getBuffer() already did str.setToBogus() */
        *errorCode=U_MEMORY_ALLOCATION_ERROR;
        return FALSE;
    }
    buffer->capacity = newCapacity;
    buffer->reorderStart=buffer->start+reorderStartIndex;
    buffer->limit=buffer->start+length;
    buffer->remainingCapacity=buffer->capacity-length;
    return TRUE;
}

static UBool ReorderingBuffer_equals(ReorderingBuffer *b, const UChar *oStart, const UChar *oLimit) {
  int32_t length=(int32_t)(b->limit-b->start);
  return
    length==(int32_t)(oLimit-oStart) &&
        0==u_memcmp(b->start, oStart, length);
}

static void ReorderingBuffer_remove(ReorderingBuffer *b) {
  b->reorderStart=b->limit=b->start;
  b->remainingCapacity=b->capacity;
  b->lastCC=0;
}

static void ReorderingBuffer_writeCodePoint(UChar *p, UChar32 c) {
  if(c<=0xffff) {
    *p=(UChar)c;
  } else {
    p[0]=U16_LEAD(c);
    p[1]=U16_TRAIL(c);
  }
}

#define setIterator()   (buffer->codePointStart=buffer->limit)
static void ReorderingBuffer_skipPrevious(ReorderingBuffer *buffer)
{
    buffer->codePointLimit=buffer->codePointStart;
    {
      UChar c=*--(buffer->codePointStart);
      if(U16_IS_TRAIL(c) && buffer->start<buffer->codePointStart && U16_IS_LEAD(*(buffer->codePointStart-1))) {
        --(buffer->codePointStart);
      }
    }
}
static uint8_t ReorderingBuffer_previousCC(ReorderingBuffer *buffer) {
  Normalizer2 *_this = buffer->impl;
  buffer->codePointLimit=buffer->codePointStart;
    if(buffer->reorderStart>=buffer->codePointStart) {
        return 0;
    }
    {
      UChar32 c=*--(buffer->codePointStart);
      if(c</* Normalizer2Impl::*/ MIN_CCC_LCCC_CP) {
        return 0;
      }
      {
        UChar c2;
        if(U16_IS_TRAIL(c) && buffer->start<buffer->codePointStart && U16_IS_LEAD(c2=*(buffer->codePointStart-1))) {
          --(buffer->codePointStart);
          c=U16_GET_SUPPLEMENTARY(c2, c);
        }
      }
      return getCCFromYesOrMaybe(/* _this. */getNorm16(c));
    }
}


static void ReorderingBuffer_insert(ReorderingBuffer *buffer, UChar32 c, uint8_t cc) {
    for(setIterator(), ReorderingBuffer_skipPrevious(buffer); ReorderingBuffer_previousCC(buffer)>cc;) {}
    /* insert c at codePointLimit, after the character with prevCC<=cc */
    {
      UChar *q=buffer->limit;
      {
      UChar *r=buffer->limit+=U16_LENGTH(c);
      do {
        *--r=*--q;
      } while(buffer->codePointLimit!=q);
      ReorderingBuffer_writeCodePoint(q, c);
      if(cc<=1) {
        buffer->reorderStart=r;
      }
    }
    }
}


static void ReorderingBuffer_removeSuffix(ReorderingBuffer *b, int32_t suffixLength) {
    if(suffixLength<(b->limit-b->start)) {
        b->limit-=suffixLength;
        b->remainingCapacity+=suffixLength;
    } else {
        b->limit=b->start;
        b->remainingCapacity=b->capacity;
    }
    b->lastCC=0;
    b->reorderStart=b->limit;
}

static int32_t ReorderingBuffer_length(ReorderingBuffer *buffer) {
  return (int32_t)(buffer->limit-buffer->start);
}

static uint8_t ReorderingBuffer_getLastCC(ReorderingBuffer *buffer) {
  return buffer->lastCC;
}

static UBool ReorderingBuffer_appendSupplementary(ReorderingBuffer *buffer, UChar32 c, uint8_t cc, UErrorCode *errorCode) {
  if(buffer->remainingCapacity<2 && !ReorderingBuffer_resize(buffer, 2, errorCode)) {
        return FALSE;
    }
    if(buffer->lastCC<=cc || cc==0) {
        buffer->limit[0]=U16_LEAD(c);
        buffer->limit[1]=U16_TRAIL(c);
        buffer->limit+=2;
        buffer->lastCC=cc;
        if(cc<=1) {
            buffer->reorderStart=buffer->limit;
        }
    } else {
      ReorderingBuffer_insert(buffer, c, cc);
    }
    buffer->remainingCapacity-=2;
    return TRUE;
}

    /* s must be in NFD, otherwise change the implementation. */
static UBool ReorderingBuffer_appendBMP(ReorderingBuffer *buffer, UChar c, uint8_t cc, UErrorCode *errorCode) {
  if(buffer->remainingCapacity==0 && !ReorderingBuffer_resize(buffer, 1, errorCode)) {
            return FALSE;
        }
        if(buffer->lastCC<=cc || cc==0) {
          *(buffer->limit)++=c;
            buffer->lastCC=cc;
            if(cc<=1) {
                buffer->reorderStart=buffer->limit;
            }
        } else {
          ReorderingBuffer_insert(buffer, c, cc);
        }
        --(buffer->remainingCapacity);
        return TRUE;
    }

static UBool ReorderingBuffer_append(ReorderingBuffer *buffer, UChar32 c, uint8_t cc, UErrorCode *errorCode) {
  return (c<=0xffff) ?
    ReorderingBuffer_appendBMP(buffer,(UChar)c, cc, errorCode) :
    ReorderingBuffer_appendSupplementary(buffer,c, cc, errorCode);
}









static void ReorderingBuffer_close(ReorderingBuffer *buffer) {
  if(buffer!=NULL
     && buffer->start!=NULL 
     && (buffer->start!=buffer->str)) {  /* don't close the buffer if it's "str" (user's original buffer) */
    uprv_free(buffer->start);
  }
}

static UBool ReorderingBuffer_init(ReorderingBuffer *buffer, int32_t destCapacity, UErrorCode *pErrorCode) {
  return TRUE; /* not needed. see ReorderingBuffer_construct */
}

static int32_t ReorderingBuffer_extract(ReorderingBuffer *buffer, Normalizer2 *n, UChar *dest, int32_t capacity, UErrorCode *pErrorCode) {
  int32_t length = buffer->limit - buffer->start;
  if(buffer->str!=buffer->start) { /* did we make a new buffer? then copy */
    int32_t tlen = length;
    if(tlen > capacity) {
      tlen = capacity;
    }
    u_strncpy(dest,buffer->start,tlen);
  }
  return u_terminateUChars(dest,capacity,length,pErrorCode);
}

static UBool ReorderingBuffer_appendZeroCCStr(ReorderingBuffer *buffer, const UChar *s, const UChar *sLimit, UErrorCode *errorCode) {
    int32_t length=(int32_t)(sLimit-s);
    if(s==sLimit) {
        return TRUE;
    }
    if(buffer->remainingCapacity<length && !ReorderingBuffer_resize(buffer, length, errorCode)) {
        return FALSE;
    }
    u_memcpy(buffer->limit, s, length);
    buffer->limit+=length;
    buffer->remainingCapacity-=length;
    buffer->lastCC=0;
    buffer->reorderStart=buffer->limit;
    return TRUE;
}

static UBool ReorderingBuffer_appendLeadTrail(ReorderingBuffer *buffer, const UChar *s, int32_t length,
                               uint8_t leadCC, uint8_t trailCC,
                               UErrorCode *errorCode) {
    if(length==0) {
        return TRUE;
    }
    if(buffer->remainingCapacity<length && !ReorderingBuffer_resize(buffer, length, errorCode)) {
        return FALSE;
    }
    buffer->remainingCapacity-=length;
    if(buffer->lastCC<=leadCC || leadCC==0) {
        const UChar *sLimit=s+length;
        if(trailCC<=1) {
            buffer->reorderStart=buffer->limit+length;
        } else if(leadCC<=1) {
          buffer->reorderStart=buffer->limit+1;  /* Ok if not a code point boundary. */
        }
        do { *buffer->limit++=*s++; } while(s!=sLimit);
        buffer->lastCC=trailCC;
    } else {
        int32_t i=0;
        UChar32 c;
        U16_NEXT(s, i, length, c);
        ReorderingBuffer_insert(buffer, c, leadCC);  /* insert first code point */
        while(i<length) {
            U16_NEXT(s, i, length, c);
            if(i<length) {
                /* s must be in NFD, otherwise we need to use getCC(). */
              Normalizer2 *_this = buffer->impl;
                leadCC=getCCFromYesOrMaybe(getNorm16(c));
            } else {
                leadCC=trailCC;
            }
            ReorderingBuffer_append(buffer, c, leadCC, errorCode);
        }
    }
    return TRUE;
}




#if 0
    UBool isEmpty() const { return start==limit; }
    int32_t length() const { return (int32_t)(limit-start); }
    UChar *getStart() { return start; }
    UChar *getLimit() { return limit; }
    uint8_t getLastCC() const { return lastCC; }

    UBool equals(const UChar *start, const UChar *limit) const;

    /*  For Hangul composition, replacing the Leading consonant Jamo with the syllable. */
    void setLastChar(UChar c) {
        *(limit-1)=c;
    }

    UBool append(UChar32 c, uint8_t cc, UErrorCode &errorCode) {
        return (c<=0xffff) ?
            appendBMP((UChar)c, cc, errorCode) :
            appendSupplementary(c, cc, errorCode);
    }
    /*  s must be in NFD, otherwise change the implementation. */
    UBool append(const UChar *s, int32_t length,
                 uint8_t leadCC, uint8_t trailCC,
                 UErrorCode &errorCode);
    UBool appendBMP(UChar c, uint8_t cc, UErrorCode &errorCode) {
        if(remainingCapacity==0 && !resize(1, errorCode)) {
            return FALSE;
        }
        if(lastCC<=cc || cc==0) {
            *limit++=c;
            lastCC=cc;
            if(cc<=1) {
                reorderStart=limit;
            }
        } else {
            insert(c, cc);
        }
        --remainingCapacity;
        return TRUE;
    }
    UBool appendZeroCC(UChar32 c, UErrorCode &errorCode);
    UBool appendZeroCC(const UChar *s, const UChar *sLimit, UErrorCode &errorCode);
    void remove();
    void removeSuffix(int32_t suffixLength);
    void setReorderingLimit(UChar *newLimit) {
        remainingCapacity+=(int32_t)(limit-newLimit);
        reorderStart=limit=newLimit;
        lastCC=0;
    }
    UBool appendSupplementary(UChar32 c, uint8_t cc, UErrorCode &errorCode);
    void insert(UChar32 c, uint8_t cc);
    static void writeCodePoint(UChar *p, UChar32 c) {
        if(c<=0xffff) {
            *p=(UChar)c;
        } else {
            p[0]=U16_LEAD(c);
            p[1]=U16_TRAIL(c);
        }
    }
    UBool resize(int32_t appendLength, UErrorCode &errorCode);

    const Normalizer2Impl &impl;
    UnicodeString &str;
    UChar *start, *reorderStart, *limit;
    int32_t remainingCapacity;
    uint8_t lastCC;

    /*  private backward iterator */
    void setIterator() { codePointStart=limit; }
    void skipPrevious();  /*  Requires start<codePointStart. */
    uint8_t previousCC();  /*  Returns 0 if there is no previous character. */

    UChar *codePointStart, *codePointLimit;

#endif



static int32_t U_CALLCONV Normalizer2_comp_normalize (struct Normalizer2 *_this,
                                                 const UChar *src, int32_t length,
                                                 UChar *dest, int32_t capacity,
                                                 UErrorCode *pErrorCode) {
  int32_t tlen = length;
  ReorderingBuffer buffer;

  if(U_FAILURE(*pErrorCode)) {
/* #if defined(UNORM_DEBUG) */
/*     fprintf(stderr,"normalize noop: err %s\n", u_errorName(*pErrorCode)); */
/* #endif */
    return 0;
  }
  
  ReorderingBuffer_construct(&buffer, _this, dest, capacity);
  if(ReorderingBuffer_init(&buffer, capacity, pErrorCode)) {
    Normalizer2_comp_compose(_this, src, length>=0 ? src+length : NULL, _this->onlyContiguous, TRUE, &buffer, pErrorCode);
  }
  
  tlen =  ReorderingBuffer_extract(&buffer, _this, dest, capacity, pErrorCode);
  ReorderingBuffer_close(&buffer);
  return tlen;
}

static int32_t U_CALLCONV Normalizer2_noop_normalize (struct Normalizer2 *n,
                                                 const UChar *src, int32_t length,
                                                 UChar *dest, int32_t capacity,
                                                 UErrorCode *pErrorCode) {
  int32_t tlen = length;

  if(U_FAILURE(*pErrorCode)) {
/* #if defined(UNORM_DEBUG) */
/*     fprintf(stderr,"normalize noop: err %s\n", u_errorName(*pErrorCode)); */
/* #endif */
    return 0;
  }
  if(tlen == -1) {
    tlen = u_strlen(src);
  }
  if(capacity<length) {
    tlen = capacity;
  }
  u_strncpy(dest,src,tlen);
  return u_terminateUChars(dest,capacity,length,pErrorCode);
}

static const UChar *
Normalizer2_fcd_copyLowPrefixFromNulTerminated(Normalizer2 *_this, const UChar *src,
                                                UChar32 minNeedDataCP,
                                                ReorderingBuffer *buffer,
                                                UErrorCode *errorCode)  {
    /*  Make some effort to support NUL-terminated strings reasonably. */
    /*  Take the part of the fast quick check loop that does not look up */
    /*  data and check the first part of the string. */
    /*  After this prefix, determine the string length to simplify the rest */
    /*  of the code. */
    UChar c;
    const UChar *prevSrc=src;
    while((c=*src++)<minNeedDataCP && c!=0) {}
    /*  Back out the last character for full processing. */
    /*  Copy this prefix. */
    if(--src!=prevSrc) {
        if(buffer!=NULL) {
          ReorderingBuffer_appendZeroCCStr(buffer, prevSrc, src, errorCode);
        }
    }
    return src;
}

static const uint16_t *Normalizer2_getCompositionsListForDecompYes(Normalizer2 *_this, uint16_t norm16)  {
        if(norm16==0 || MIN_NORMAL_MAYBE_YES<=norm16) {
            return NULL;
        } else if(norm16<_this->minMaybeYes) {
            return _this->extraData+norm16;  /*  for yesYes; if Jamo L: harmless empty list */
        } else {
            return _this->maybeYesCompositions+norm16-_this->minMaybeYes;
        }
    }
static    const uint16_t *Normalizer2_getCompositionsListForComposite(Normalizer2 *_this, uint16_t norm16)  {
        const uint16_t *list=_this->extraData+norm16;  /*  composite has both mapping & compositions list */
        return list+  /*  mapping pointer */
            1+  /*  +1 to skip the first unit with the mapping lenth */
            (*list&MAPPING_LENGTH_MASK)+  /*  + mapping length */
            ((*list>>7)&1);  /*  +1 if MAPPING_HAS_CCC_LCCC_WORD */
    }


#if UNORM_ENABLE_FCD

/*  Dual functionality: */
/*  buffer!=NULL: normalize */
/*  buffer==NULL: isNormalized/quickCheck/spanQuickCheckYes */
const UChar *
Normalizer2_fcd_makeFCD(Normalizer2 *_this, const UChar *src, const UChar *limit,
                         ReorderingBuffer *buffer,
                         UErrorCode *errorCode) {
    const UChar *prevBoundary=src;
    const UChar *prevSrc;
    UChar32 c=0;
    int32_t prevFCD16=0;
    uint16_t fcd16=0;

    const UTrie2 *trie=_this->newFCDTrie;
  if(limit==NULL) {
    src=Normalizer2_fcd_copyLowPrefixFromNulTerminated(_this,src, MIN_CCC_LCCC_CP, buffer, errorCode);
    if(U_FAILURE(*errorCode)) {
      return src;
    }
    limit=u_strchr(src, 0);
  }

    /*  Note: In this function we use buffer->appendZeroCC() because we track */
    /*  the lead and trail combining classes here, rather than leaving it to */
    /*  the ReorderingBuffer. */
    /*  The exception is the call to decomposeShort() which uses the buffer */
    /*  in the normal way. */


    /*  Tracks the last FCD-safe boundary, before lccc=0 or after properly-ordered tccc<=1. */
    /*  Similar to the prevBoundary in the compose() implementation. */
    for(;;) {
        /*  count code units with lccc==0 */
        for(prevSrc=src; src!=limit;) {
            if((c=*src)<MIN_CCC_LCCC_CP) {
                prevFCD16=~c;
                ++src;
            } else if((fcd16=UTRIE2_GET16_FROM_U16_SINGLE_LEAD(trie, c))<=0xff) {
                prevFCD16=fcd16;
                ++src;
            } else if(!U16_IS_SURROGATE(c)) {
                break;
            } else {
                UChar c2;
                if(U16_IS_SURROGATE_LEAD(c)) {
                    if((src+1)!=limit && U16_IS_TRAIL(c2=src[1])) {
                        c=U16_GET_SUPPLEMENTARY(c, c2);
                    }
                } else /* trail surrogate */ {
                    if(prevSrc<src && U16_IS_LEAD(c2=*(src-1))) {
                        --src;
                        c=U16_GET_SUPPLEMENTARY(c2, c);
                    }
                }
                if((fcd16=getFCD16(c))<=0xff) {
                    prevFCD16=fcd16;
                    src+=U16_LENGTH(c);
                } else {
                    break;
                }
            }
        }
        /*  copy these code units all at once */
        if(src!=prevSrc) {
          if(buffer!=NULL && unimp(errorCode) /* !buffer->appendZeroCC(prevSrc, src, errorCode) */) { /* FCD */
                break;
            }
            if(src==limit) {
                break;
            }
            prevBoundary=src;
            /*  We know that the previous character's lccc==0. */
            if(prevFCD16<0) {
                /*  Fetching the fcd16 value was deferred for this below-U+0300 code point. */
                prevFCD16=getFCD16FromSingleLead((UChar)~prevFCD16);
                if(prevFCD16>1) {
                    --prevBoundary;
                }
            } else {
                const UChar *p=src-1;
                if(U16_IS_TRAIL(*p) && prevSrc<p && U16_IS_LEAD(*(p-1))) {
                    --p;
                    /*  Need to fetch the previous character's FCD value because */
                    /*  prevFCD16 was just for the trail surrogate code point. */
                    prevFCD16=getFCD16FromSurrogatePair(p[0], p[1]);
                    /*  Still known to have lccc==0 because its lead surrogate unit had lccc==0. */
                }
                if(prevFCD16>1) {
                    prevBoundary=p;
                }
            }
            /*  The start of the current character (c). */
            prevSrc=src;
        } else if(src==limit) {
            break;
        }

        src+=U16_LENGTH(c);
        /*  The current character (c) at [prevSrc..src[ has a non-zero lead combining class. */
        /*  Check for proper order, and decompose locally if necessary. */
        if((prevFCD16&0xff)<=(fcd16>>8)) {
            /*  proper order: prev tccc <= current lccc */
            if((fcd16&0xff)<=1) {
                prevBoundary=src;
            }
            if(buffer!=NULL && unimp(errorCode) /* !buffer->appendZeroCC(c, errorCode) */) { /* FCD */
                break;
            }
            prevFCD16=fcd16;
            continue;
        } else if(buffer==NULL) {
            return prevBoundary;  /*  quick check "no" */
        } else {
          unimp(errorCode); /* FCD */
#if 0
            /*
             * Back out the part of the source that we copied or appended
             * already but is now going to be decomposed.
             * prevSrc is set to after what was copied/appended.
             */
            buffer->removeSuffix((int32_t)(prevSrc-prevBoundary));
            /*
             * Find the part of the source that needs to be decomposed,
             * up to the next safe boundary.
             */
            src=findNextFCDBoundary(src, limit);
            /*
             * The source text does not fulfill the conditions for FCD.
             * Decompose and reorder a limited piece of the text.
             */
            if(!decomposeShort(prevBoundary, src, *buffer, errorCode)) {
                break;
            }
            prevBoundary=src;
            prevFCD16=0;
#endif
        }
    }
    return src;
}

#endif

static UBool Normalizer2Impl_decomposeChar(Normalizer2 *_this, UChar32 c, uint16_t norm16,
                                 ReorderingBuffer *buffer,
                                 UErrorCode *errorCode)  {
    /* Only loops for 1:1 algorithmic mappings. */
    for(;;) {
        /* get the decomposition and the lead and trail cc's */
      if(isDecompYes(_this,norm16)) {
            /* c does not decompose */
          return ReorderingBuffer_append(buffer, c, getCCFromYesOrMaybe(norm16), errorCode);
        } else if(isHangul(norm16)) {
            /* Hangul syllable: decompose algorithmically */
            UChar jamos[3];
            return ReorderingBuffer_appendZeroCCStr(buffer, jamos, jamos+Hangul_decompose(c, jamos), errorCode);
        } else if(isDecompNoAlgorithmic(norm16)) {
            c=mapAlgorithmic(c, norm16);
            norm16=getNorm16(c);
        } else {
            /* c decomposes, get everything from the variable-length extra data */
          const uint16_t *mapping;
          uint16_t firstUnit;
            int32_t length;
            uint8_t leadCC, trailCC;

            mapping=getMapping(norm16);
            firstUnit=*mapping++;
            length=firstUnit&MAPPING_LENGTH_MASK;
            trailCC=(uint8_t)(firstUnit>>8);
            if(firstUnit&MAPPING_HAS_CCC_LCCC_WORD) {
                leadCC=(uint8_t)(*mapping++>>8);
            } else {
                leadCC=0;
            }
            return ReorderingBuffer_appendLeadTrail(buffer, (const UChar *)mapping, length, leadCC, trailCC, errorCode);
        }
    }
}


/*  Dual functionality: */
/*  buffer!=NULL: normalize */
/*  buffer==NULL: isNormalized/spanQuickCheckYes */
static const UChar *
Normalizer2_decomp_decompose(Normalizer2 *_this, const UChar *src, const UChar *limit,
                           ReorderingBuffer *buffer,
                           UErrorCode *errorCode) {
    /*  only for quick check */
    const UChar *prevBoundary=src;
    uint8_t prevCC=0;
    const UChar *prevSrc;
    UChar32 c=0;
    uint16_t norm16=0;
    UChar32 minNoCP=_this->minDecompNoCP;
    if(limit==NULL) {
      src=Normalizer2_fcd_copyLowPrefixFromNulTerminated(_this, src, minNoCP, buffer, errorCode);
      if(U_FAILURE(*errorCode)) {
            return src;
        }
        limit=u_strchr(src, 0);
    }

    for(;;) {
        /*  count code units below the minimum or with irrelevant data for the quick check */
        for(prevSrc=src; src!=limit;) {
            if( (c=*src)<minNoCP ||
                isMostDecompYesAndZeroCC(_this, norm16=UTRIE2_GET16_FROM_U16_SINGLE_LEAD(_this->normTrie, c))
            ) {
                ++src;
            } else if(!U16_IS_SURROGATE(c)) {
                break;
            } else {
                UChar c2;
                if(U16_IS_SURROGATE_LEAD(c)) {
                    if((src+1)!=limit && U16_IS_TRAIL(c2=src[1])) {
                        c=U16_GET_SUPPLEMENTARY(c, c2);
                    }
                } else /* trail surrogate */ {
                    if(prevSrc<src && U16_IS_LEAD(c2=*(src-1))) {
                        --src;
                        c=U16_GET_SUPPLEMENTARY(c2, c);
                    }
                }
                if(isMostDecompYesAndZeroCC(_this, norm16=getNorm16(c))) {
                    src+=U16_LENGTH(c);
                } else {
                    break;
                }
            }
        }
        /*  copy these code units all at once */
        if(src!=prevSrc) {
          if(buffer!=NULL) { 
            if(ReorderingBuffer_appendZeroCCStr(buffer, prevSrc, src, errorCode)) { 
              break; 
            } 
          } else {
            prevCC=0;
            prevBoundary=src;
          }
        }
        if(src==limit) {
            break;
        }

        /*  Check one above-minimum, relevant code point. */
        src+=U16_LENGTH(c);
        if(buffer!=NULL) {
          if(!Normalizer2Impl_decomposeChar(_this, c, norm16, buffer, errorCode)) {
            break;
          }
        } else {
          if(isDecompYes(_this,norm16)) {
                uint8_t cc=getCCFromYesOrMaybe(norm16);
                if(prevCC<=cc || cc==0) {
                    prevCC=cc;
                    if(cc<=1) {
                        prevBoundary=src;
                    }
                    continue;
                }
            }
          return prevBoundary;  /*  "no" or cc out of order*/
        }
    }
    return src;
}



static uint8_t Normalizer2_getTrailCCFromCompYesAndZeroCC(Normalizer2 *_this, const UChar *cpStart, const UChar *cpLimit)  {
    UChar32 c;
    uint16_t prevNorm16;
    if(cpStart==(cpLimit-1)) {
        c=*cpStart;
    } else {
        c=U16_GET_SUPPLEMENTARY(cpStart[0], cpStart[1]);
    }
    prevNorm16=getNorm16(c);
    if(prevNorm16<=_this->minYesNo) {
      return 0;  /* yesYes and Hangul LV/LVT have ccc=tccc=0 */
    } else {
      return (uint8_t)(*getMapping(prevNorm16)>>8);  /* tccc from yesNo */
    }
}


static void Normalizer2Impl_composeAndAppend(Normalizer2 *_this, const UChar *src, const UChar *limit,
                                       UBool doCompose,
                                       UBool onlyContiguous,
                                       ReorderingBuffer *buffer,
                                       UErrorCode *errorCode)  {
    if(!ReorderingBuffer_isEmpty(buffer)) {
      const UChar *firstStarterInSrc=Normalizer2Impl_findNextCompBoundary(_this,src, limit);
        if(src!=firstStarterInSrc) {
          UChar *middleStart;
          int32_t middleLength;

          const UChar *lastStarterInDest=Normalizer2Impl_findPreviousCompBoundary(_this, ReorderingBuffer_getStart(buffer),
                                                                    ReorderingBuffer_getLimit(buffer));
          
          middleLength = (int32_t)(ReorderingBuffer_getLimit(buffer)-lastStarterInDest)+   /* middle = [lastStarterInDest..limit] */
            (int32_t)(firstStarterInSrc-src);  /* middle append [src..firstStarterInSrc] */
          middleStart = uprv_malloc(middleLength*sizeof(middleStart[0]));

          if(middleStart==NULL) {
            *errorCode=U_MEMORY_ALLOCATION_ERROR;
            return;
          }
          
          /* C++: UnicodeString middle(lastStarterInDest,(int32_t)(buffer.getLimit()-lastStarterInDest)); (copy chars) */
          /* >>>: middle := [lastStarterInDest..limit] */
          uprv_memcpy(middleStart,lastStarterInDest,
                      sizeof(middleStart[0])*(ReorderingBuffer_getLimit(buffer)-lastStarterInDest)); /* copy */

          ReorderingBuffer_removeSuffix(buffer, (int32_t)(ReorderingBuffer_getLimit(buffer)-lastStarterInDest));
          
          /* C++: middle.append(src, (int32_t)(firstStarterInSrc-src)); */
          /* >>>: middle append [src..firstStarterInSrc] */
          uprv_memcpy(middleStart+(ReorderingBuffer_getLimit(buffer)-lastStarterInDest),
                      src,
                      sizeof(middleStart[0]*(firstStarterInSrc-src))); /* append */

          /* C++: const UChar *middleStart=middle.getBuffer(); */
          /* >>>: middleStart:  beginning of 'middle' buffer  (already done)*/

          /* C++: compose(middleStart, middleStart+middle.length(), onlyContiguous, TRUE, buffer, errorCode); */
          Normalizer2_comp_compose(_this, middleStart, middleStart+middleLength, onlyContiguous, TRUE, buffer, errorCode);
          
          uprv_free(middleStart);

          if(U_FAILURE(*errorCode)) {
            return;
          }
          src=firstStarterInSrc;
        }
    }
    if(doCompose) {
      Normalizer2_comp_compose(_this, src, limit, onlyContiguous, TRUE, buffer, errorCode);
    } else {
      ReorderingBuffer_appendZeroCCStr(buffer, src, limit, errorCode);
    }
}

/**
 * Does c have a composition boundary before it?
 * True if its decomposition begins with a character that has
 * ccc=0 && NFC_QC=Yes (isCompYesAndZeroCC()).
 * As a shortcut, this is true if c itself has ccc=0 && NFC_QC=Yes
 * (isCompYesAndZeroCC()) so we need not decompose.
 */
static UBool Normalizer2Impl_hasCompBoundaryBefore(Normalizer2 *_this, UChar32 c, uint16_t norm16) {
    for(;;) {
        if(isCompYesAndZeroCC(norm16)) {
            return TRUE;
        } else if(isMaybeOrNonZeroCC(norm16)) {
            return FALSE;
        } else if(isDecompNoAlgorithmic(norm16)) {
            c=mapAlgorithmic(c, norm16);
            norm16=getNorm16(c);
        } else {
            /*  c decomposes, get everything from the variable-length extra data */
            int32_t i=0;
            UChar32 c;
            const uint16_t *mapping=getMapping(norm16);
            {
              uint16_t firstUnit=*mapping++;
              if((firstUnit&MAPPING_LENGTH_MASK)==0) {
                return FALSE;
              }
              if((firstUnit&MAPPING_HAS_CCC_LCCC_WORD) && (*mapping++&0xff00)) {
                return FALSE;  /*  non-zero leadCC */
              }
              U16_NEXT_UNSAFE(mapping, i, c);
              return isCompYesAndZeroCC(getNorm16(c));
            }
        }
    }
}

static UBool Normalizer2Impl_hasCompBoundaryAfter(Normalizer2 *_this, UChar32 c, UBool onlyContiguous, UBool testInert)  {
    for(;;) {
        uint16_t norm16=getNorm16(c);
        if(isInert(norm16)) {
            return TRUE;
        } else if(norm16<=_this->minYesNo) {
            /*  Hangul LVT (==minYesNo) has a boundary after it. */
            /*  Hangul LV and non-inert yesYes characters combine forward. */
          return isHangul(norm16) && !/*Hangul::*/isHangulWithoutJamoT((UChar)c);
        } else if(norm16>= (testInert ? _this->minNoNo : _this->minMaybeYes)) {
            return FALSE;
        } else if(isDecompNoAlgorithmic(norm16)) {
            c=mapAlgorithmic(c, norm16);
        } else {
            /*  c decomposes, get everything from the variable-length extra data. */
            /*  If testInert, then c must be a yesNo character which has lccc=0, */
            /*  otherwise it could be a noNo. */
            const uint16_t *mapping=getMapping(norm16);
            {
              uint16_t firstUnit=*mapping;
              /*  TRUE if */
              /*       c is not deleted, and */
              /*       it and its decomposition do not combine forward, and it has a starter, and */
              /*       if FCC then trailCC<=1 */
              return
                (firstUnit&MAPPING_LENGTH_MASK)!=0 &&
                (firstUnit&(MAPPING_PLUS_COMPOSITION_LIST|MAPPING_NO_COMP_BOUNDARY_AFTER))==0 &&
                (!_this->onlyContiguous || firstUnit<=0x1ff);
            }
        }
    }
}

typedef struct {
    const UTrie2 *trie;
    const UChar *codePointStart, *codePointLimit;
    UChar32 codePoint;
    const UChar *start;
  
}  BackwardsUTrie2StringIterator;

static void BackwardsUTrie2StringIterator_init(BackwardsUTrie2StringIterator *iter, const UTrie2*t, const UChar *s, const UChar *p) {
  iter->trie = t;
  iter->codePointStart=p;
  iter->codePointLimit=p;
  iter->codePoint=U_SENTINEL;
  iter->start=s;
}

static uint16_t BackwardsUTrie2StringIterator_previous16(BackwardsUTrie2StringIterator *iter) {
    uint16_t result;
    iter->codePointLimit=iter->codePointStart;
    if(iter->start>=iter->codePointStart) {
        iter->codePoint=U_SENTINEL;
        return 0;
    }
    UTRIE2_U16_PREV16(iter->trie, iter->start, iter->codePointStart, iter->codePoint, result);
    return result;
}

typedef struct {
    const UTrie2 *trie;
    const UChar *codePointStart, *codePointLimit;
    UChar32 codePoint;
    const UChar *limit;
  
}  ForwardUTrie2StringIterator;

static void ForwardUTrie2StringIterator_init(ForwardUTrie2StringIterator *iter, const UTrie2*t, const UChar *p, const UChar *l) {
  iter->trie = t;
  iter->codePointStart=p;
  iter->codePointLimit=p;
  iter->codePoint=U_SENTINEL;
  iter->limit=l;
}

static uint16_t ForwardUTrie2StringIterator_next16(ForwardUTrie2StringIterator *iter) {
    uint16_t result;
    iter->codePointStart=iter->codePointLimit;
    if(iter->limit == iter->codePointLimit) {
        iter->codePoint=U_SENTINEL;
        return 0;
    }
    UTRIE2_U16_NEXT16(iter->trie, iter->codePointLimit, iter->limit, iter->codePoint, result);
    return result;
}

static const UChar *Normalizer2Impl_findPreviousCompBoundary(Normalizer2 *_this, const UChar *start, const UChar *p) {
  BackwardsUTrie2StringIterator iter;
  uint16_t norm16;
  BackwardsUTrie2StringIterator_init(&iter, _this->normTrie, start, p);
    do {
        norm16=BackwardsUTrie2StringIterator_previous16(&iter);
    } while(!Normalizer2Impl_hasCompBoundaryBefore(_this, iter.codePoint, norm16));
    /*  We could also test hasCompBoundaryAfter() and return iter.codePointLimit, */
    /*  but that's probably not worth the extra cost. */
    return iter.codePointStart;
}

static const UChar *Normalizer2Impl_findNextCompBoundary(Normalizer2 *_this, const UChar *p, const UChar *limit)  {
    uint16_t norm16;
    ForwardUTrie2StringIterator iter;

    ForwardUTrie2StringIterator_init(&iter, _this->normTrie, p, limit);
    do {
        norm16=ForwardUTrie2StringIterator_next16(&iter);
    } while(!Normalizer2Impl_hasCompBoundaryBefore(_this, iter.codePoint, norm16));
    return iter.codePointStart;
}


/*
 * Finds the recomposition result for
 * a forward-combining "lead" character,
 * specified with a pointer to its compositions list,
 * and a backward-combining "trail" character.
 *
 * If the lead and trail characters combine, then this function returns
 * the following "compositeAndFwd" value:
 * Bits 21..1  composite character
 * Bit      0  set if the composite is a forward-combining starter
 * otherwise it returns -1.
 *
 * The compositions list has (trail, compositeAndFwd) pair entries,
 * encoded as either pairs or triples of 16-bit units.
 * The last entry has the high bit of its first unit set.
 *
 * The list is sorted by ascending trail characters (there are no duplicates).
 * A linear search is used.
 *
 * See normalizer2impl.h for a more detailed description
 * of the compositions list format.
 */
static int32_t Normalizer2Impl_combine(Normalizer2 *_this, const uint16_t *list, UChar32 trail) {
    uint16_t key1, firstUnit;
    if(trail<COMP_1_TRAIL_LIMIT) {
        /*  trail character is 0..33FF */
        /*  result entry may have 2 or 3 units */
        key1=(uint16_t)(trail<<1);
        while(key1>(firstUnit=*list)) {
            list+=2+(firstUnit&COMP_1_TRIPLE);
        }
        if(key1==(firstUnit&COMP_1_TRAIL_MASK)) {
            if(firstUnit&COMP_1_TRIPLE) {
                return ((int32_t)list[1]<<16)|list[2];
            } else {
                return list[1];
            }
        }
    } else {
        /*  trail character is 3400..10FFFF */
        /*  result entry has 3 units */
        uint16_t secondUnit;
        uint16_t key2;
        key1=(uint16_t)(COMP_1_TRAIL_LIMIT+
                        ((trail>>COMP_1_TRAIL_SHIFT))&
                         ~COMP_1_TRIPLE);

        key2 =(uint16_t)(trail<<COMP_2_TRAIL_SHIFT);
        for(;;) {
            if(key1>(firstUnit=*list)) {
                list+=2+(firstUnit&COMP_1_TRIPLE);
            } else if(key1==(firstUnit&COMP_1_TRAIL_MASK)) {
                if(key2>(secondUnit=list[1])) {
                    if(firstUnit&COMP_1_LAST_TUPLE) {
                        break;
                    } else {
                        list+=3;
                    }
                } else if(key2==(secondUnit&COMP_2_TRAIL_MASK)) {
                    return ((int32_t)(secondUnit&~COMP_2_TRAIL_MASK)<<16)|list[2];
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }
    return -1;
}


/*
 * Recomposes the buffer text starting at recomposeStartIndex
 * (which is in NFD - decomposed and canonically ordered),
 * and truncates the buffer contents.
 *
 * Note that recomposition never lengthens the text:
 * Any character consists of either one or two code units;
 * a composition may contain at most one more code unit than the original starter,
 * while the combining mark that is removed has at least one code unit.
 */
static void Normalizer2Impl_recompose(Normalizer2 *_this, ReorderingBuffer *buffer, int32_t recomposeStartIndex,
                                UBool onlyContiguous) {
  UChar *p;
  UChar *limit;
  UChar *starter, *pRemove, *q, *r;
  const uint16_t *compositionsList;
  UChar32 c, compositeAndFwd;
  uint16_t norm16;
  uint8_t cc, prevCC;
  UBool starterIsSupplementary;
  
  
  p=ReorderingBuffer_getStart(buffer)+recomposeStartIndex;
  limit=ReorderingBuffer_getLimit(buffer);
  

    if(p==limit) {
        return;
    }

    /*  Some of the following variables are not used until we have a forward-combining starter */
    /*  and are only initialized now to avoid compiler warnings. */
    compositionsList=NULL;  /*  used as indicator for whether we have a forward-combining starter */
    starter=NULL;
    starterIsSupplementary=FALSE;
    prevCC=0;

    for(;;) {
        UTRIE2_U16_NEXT16(_this->normTrie, p, limit, c, norm16);
        cc=getCCFromYesOrMaybe(norm16);
        if( /*  this character combines backward and */
            isMaybe(norm16) &&
            /*  we have seen a starter that combines forward and */
            compositionsList!=NULL &&
            /*  the backward-combining character is not blocked */
            (prevCC<cc || prevCC==0)
        ) {
            if(isJamoVT(norm16)) {
                /*  c is a Jamo V/T, see if we can compose it with the previous character. */
                if(c</*Hangul::*/JAMO_T_BASE) {
                    /*  c is a Jamo Vowel, compose with previous Jamo L and following Jamo T. */
                    UChar prev=(UChar)(*starter-/*Hangul::*/JAMO_L_BASE);
                    if(prev</*Hangul::*/JAMO_L_COUNT) {
                        UChar t;
                        UChar syllable=(UChar)
                            (/*Hangul::*/HANGUL_BASE+
                             (prev*/*Hangul::*/JAMO_V_COUNT+(c-/*Hangul::*/JAMO_V_BASE))*
                             /*Hangul::*/JAMO_T_COUNT);
                        pRemove=p-1;
                        if(p!=limit && (t=(UChar)(*p-/*Hangul::*/JAMO_T_BASE))</*Hangul::*/JAMO_T_COUNT) {
                            ++p;
                            syllable+=t;  /*  The next character was a Jamo T. */
                        }
                        *starter=syllable;
                        /*  remove the Jamo V/T */
                        q=pRemove;
                        r=p;
                        while(r<limit) {
                            *q++=*r++;
                        }
                        limit=q;
                        p=pRemove;
                    }
                }
                /*
                 * No "else" for Jamo T:
                 * Since the input is in NFD, there are no Hangul LV syllables that
                 * a Jamo T could combine with.
                 * All Jamo Ts are combined above when handling Jamo Vs.
                 */
                if(p==limit) {
                    break;
                }
                compositionsList=NULL;
                continue;
            } else if((compositeAndFwd=Normalizer2Impl_combine(_this, compositionsList, c))>=0) {
                /*  The starter and the combining mark (c) do combine. */
              UChar32 composite;

              composite =compositeAndFwd>>1;

                /*  Replace the starter with the composite, remove the combining mark. */
                pRemove=p-U16_LENGTH(c);  /*  pRemove & p: start & limit of the combining mark */
                if(starterIsSupplementary) {
                    if(U_IS_SUPPLEMENTARY(composite)) {
                        /*  both are supplementary */
                        starter[0]=U16_LEAD(composite);
                        starter[1]=U16_TRAIL(composite);
                    } else {
                        *starter=(UChar)composite;
                        /*  The composite is shorter than the starter, */
                        /*  move the intermediate characters forward one. */
                        starterIsSupplementary=FALSE;
                        q=starter+1;
                        r=q+1;
                        while(r<pRemove) {
                            *q++=*r++;
                        }
                        --pRemove;
                    }
                } else if(U_IS_SUPPLEMENTARY(composite)) {
                    /*  The composite is longer than the starter, */
                    /*  move the intermediate characters back one. */
                    starterIsSupplementary=TRUE;
                    ++starter;  /*  temporarily increment for the loop boundary */
                    q=pRemove;
                    r=++pRemove;
                    while(starter<q) {
                        *--r=*--q;
                    }
                    *starter=U16_TRAIL(composite);
                    *--starter=U16_LEAD(composite);  /*  undo the temporary increment */
                } else {
                    /*  both are on the BMP */
                    *starter=(UChar)composite;
                }

                /* remove the combining mark by moving the following text over it */
                if(pRemove<p) {
                    q=pRemove;
                    r=p;
                    while(r<limit) {
                        *q++=*r++;
                    }
                    limit=q;
                    p=pRemove;
                }
                /*  Keep prevCC because we removed the combining mark. */

                if(p==limit) {
                    break;
                }
                /*  Is the composite a starter that combines forward? */
                if(compositeAndFwd&1) {
                    compositionsList=
                      Normalizer2_getCompositionsListForComposite(_this, getNorm16(composite));
                } else {
                    compositionsList=NULL;
                }

                /*  We combined; continue with looking for compositions. */
                continue;
            }
        }

        /*  no combination this time */
        prevCC=cc;
        if(p==limit) {
            break;
        }

        /*  If c did not combine, then check if it is a starter. */
        if(cc==0) {
            /*  Found a new starter. */
          if((compositionsList=Normalizer2_getCompositionsListForDecompYes(_this, norm16))!=NULL) {
                /*  It may combine with something, prepare for it. */
                if(U_IS_BMP(c)) {
                    starterIsSupplementary=FALSE;
                    starter=p-1;
                } else {
                    starterIsSupplementary=TRUE;
                    starter=p-2;
                }
            }
        } else if(onlyContiguous) {
            /*  FCC: no discontiguous compositions; any intervening character blocks. */
            compositionsList=NULL;
        }
    }
    ReorderingBuffer_setReorderingLimit(buffer, limit);
}

/* Decompose a short piece of text which is likely to contain characters that */
/* fail the quick check loop and/or where the quick check loop's overhead */
/* is unlikely to be amortized. */
/* Called by the compose() and makeFCD() implementations. */
static UBool Normalizer2Impl_decomposeShort(Normalizer2 *_this, const UChar *src, const UChar *limit,
                                      ReorderingBuffer *buffer,
                                      UErrorCode *errorCode)  {
    while(src<limit) {
        UChar32 c;
        uint16_t norm16;
        UTRIE2_U16_NEXT16(_this->normTrie, src, limit, c, norm16);
        if(!Normalizer2Impl_decomposeChar(_this, c, norm16, buffer, errorCode)) {
            return FALSE;
        }
    }
    return TRUE;
}


/*  Very similar to composeQuickCheck(): Make the same changes in both places if relevant. */
/*  doCompose: normalize */
/*  !doCompose: isNormalized (buffer must be empty and initialized) */
static UBool
Normalizer2_comp_compose(Normalizer2 *_this, const UChar *src, const UChar *limit,
                         UBool onlyContiguous,
                         UBool doCompose,
                         ReorderingBuffer *buffer,
                         UErrorCode *errorCode)  {
    /*
     * prevBoundary points to the last character before the current one
     * that has a composition boundary before it with ccc==0 and quick check "yes".
     * Keeping track of prevBoundary saves us looking for a composition boundary
     * when we find a "no" or "maybe".
     *
     * When we back out from prevSrc back to prevBoundary,
     * then we also remove those same characters (which had been simply copied
     * or canonically-order-inserted) from the ReorderingBuffer.
     * Therefore, at all times, the [prevBoundary..prevSrc[ source units
     * must correspond 1:1 to destination units at the end of the destination buffer.
     */

  const UChar *prevBoundary=src;
  const UChar *prevSrc;
  UChar32 c=0;
  uint16_t norm16=0;

    /*  only for isNormalized */
  uint8_t prevCC=0;
  UChar32 minNoMaybeCP=_this->minCompNoMaybeCP;
  
    if(limit==NULL) {
        UErrorCode errorCode2=U_ZERO_ERROR;
        src=Normalizer2_fcd_copyLowPrefixFromNulTerminated(_this, src, minNoMaybeCP, NULL, &errorCode2);
        limit=u_strchr(src, 0);
    }


    for(;;) {
        int32_t recomposeStartIndex;
        /*  count code units below the minimum or with irrelevant data for the quick check */
        for(prevSrc=src; src!=limit;) {
            if( (c=*src)<minNoMaybeCP ||
                isCompYesAndZeroCC(norm16=UTRIE2_GET16_FROM_U16_SINGLE_LEAD(_this->normTrie, c))
            ) {
                ++src;
            } else if(!U16_IS_SURROGATE(c)) {
                break;
            } else {
                UChar c2;
                if(U16_IS_SURROGATE_LEAD(c)) {
                    if((src+1)!=limit && U16_IS_TRAIL(c2=src[1])) {
                        c=U16_GET_SUPPLEMENTARY(c, c2);
                    }
                } else /* trail surrogate */ {
                    if(prevSrc<src && U16_IS_LEAD(c2=*(src-1))) {
                        --src;
                        c=U16_GET_SUPPLEMENTARY(c2, c);
                    }
                }
                if(isCompYesAndZeroCC(norm16=getNorm16(c))) {
                    src+=U16_LENGTH(c);
                } else {
                    break;
                }
            }
        }
        /*  copy these code units all at once */
        if(src!=prevSrc) {
            if(doCompose) {
              if(!ReorderingBuffer_appendZeroCCStr(buffer, prevSrc, src, errorCode)) {
                    break;
                }
            } else {
                prevCC=0;
            }
            if(src==limit) {
                break;
            }
            /*  Set prevBoundary to the last character in the quick check loop. */
            prevBoundary=src-1;
            if( U16_IS_TRAIL(*prevBoundary) && prevSrc<prevBoundary &&
                U16_IS_LEAD(*(prevBoundary-1))
            ) {
                --prevBoundary;
            }
            /*  The start of the current character (c). */
            prevSrc=src;
        } else if(src==limit) {
            break;
        }

        src+=U16_LENGTH(c);
        /*
         * isCompYesAndZeroCC(norm16) is false, that is, norm16>=minNoNo.
         * c is either a "noNo" (has a mapping) or a "maybeYes" (combines backward)
         * or has ccc!=0.
         * Check for Jamo V/T, then for regular characters.
         * c is not a Hangul syllable or Jamo L because those have "yes" properties.
         */
        if(isJamoVT(norm16) && prevBoundary!=prevSrc) {
            UBool needToDecompose=FALSE;
            UChar prev=*(prevSrc-1);
            if(c</* Hangul:: */JAMO_T_BASE) {
                /*  c is a Jamo Vowel, compose with previous Jamo L and following Jamo T. */
                prev=(UChar)(prev-/* Hangul:: */JAMO_L_BASE);
                if(prev</* Hangul:: */JAMO_L_COUNT) {
                    UChar t;
                    UChar syllable=(UChar)
                        (/* Hangul:: */HANGUL_BASE+
                         (prev*/* Hangul:: */JAMO_V_COUNT+(c-/* Hangul:: */JAMO_V_BASE))*
                         /* Hangul:: */JAMO_T_COUNT);
                    if(!doCompose) {
                        return FALSE;
                    }
                    if(src!=limit && (t=(UChar)(*src-/* Hangul:: */JAMO_T_BASE))</* Hangul:: */JAMO_T_COUNT) {
                        ++src;
                        syllable+=t;  /*  The next character was a Jamo T. */
                        prevBoundary=src;
                        ReorderingBuffer_setLastChar(buffer,syllable);
                        continue;
                    }
                    /*  If we see L+V+x where x!=T then we drop to the slow path, */
                    /*  decompose and recompose. */
                    /*  This is to deal with NFKC finding normal L and V but a */
                    /*  compatibility variant of a T. We need to either fully compose that */
                    /*  combination here (which would complicate the code and may not work */
                    /*  with strange custom data) or use the slow path -- or else our replacing */
                    /*  two input characters (L+V) with one output character (LV syllable) */
                    /*  would violate the invariant that [prevBoundary..prevSrc[ has the same */
                    /*  length as what we appended to the buffer since prevBoundary. */
                    needToDecompose=TRUE;
                }
            } else if(/* Hangul:: */isHangulWithoutJamoT(prev)) {
                /*  c is a Jamo Trailing consonant, */
                /*  compose with previous Hangul LV that does not contain a Jamo T. */
                if(!doCompose) {
                    return FALSE;
                }
                ReorderingBuffer_setLastChar(buffer, (UChar)(prev+c-/* Hangul:: */JAMO_T_BASE));
                prevBoundary=src;
                continue;
            }
            if(!needToDecompose) {
                /*  The Jamo V/T did not compose into a Hangul syllable. */
                if(doCompose) {
                  if(!ReorderingBuffer_appendBMP(buffer, (UChar)c, 0, errorCode)) {
                        break;
                    }
                } else {
                    prevCC=0;
                }
                continue;
            }
        }
        /*
         * Source buffer pointers:
         *
         *  all done      quick check   current char  not yet
         *                "yes" but     (c)           processed
         *                may combine
         *                forward
         * [-------------[-------------[-------------[-------------[
         * |             |             |             |             |
         * orig. src     prevBoundary  prevSrc       src           limit
         *
         *
         * Destination buffer pointers inside the ReorderingBuffer:
         *
         *  all done      might take    not filled yet
         *                characters for
         *                reordering
         * [-------------[-------------[-------------[
         * |             |             |             |
         * start         reorderStart  limit         |
         *                             +remainingCap.+
         */
        if(norm16>=MIN_YES_YES_WITH_CC) {
            uint8_t cc=(uint8_t)norm16;  /*  cc!=0 */
            if( onlyContiguous &&  /*  FCC */
                (doCompose ? ReorderingBuffer_getLastCC(buffer) : prevCC)==0 &&
                prevBoundary<prevSrc &&
                /*  ReorderingBuffer_getLastCC(buffer)==0 && prevBoundary<prevSrc tell us that */
                /*  [prevBoundary..prevSrc[ (which is exactly one character under these conditions) */
                /*  passed the quick check "yes && ccc==0" test. */
                /*  Check whether the last character was a "yesYes" or a "yesNo". */
                /*  If a "yesNo", then we get its trailing ccc from its */
                /*  mapping and check for canonical order. */
                /*  All other cases are ok. */
                Normalizer2_getTrailCCFromCompYesAndZeroCC(_this,prevBoundary, prevSrc)>cc
            ) {
                /*  Fails FCD test, need to decompose and contiguously recompose. */
                if(!doCompose) {
                    return FALSE;
                }
            } else if(doCompose) {
              if(!ReorderingBuffer_append(buffer, c, cc, errorCode)) {
                    break;
                }
                continue;
            } else if(prevCC<=cc) {
                prevCC=cc;
                continue;
            } else {
                return FALSE;
            }
        } else if(!doCompose && !isMaybeOrNonZeroCC(norm16)) {
            return FALSE;
        }

        /*
         * Find appropriate boundaries around this character,
         * decompose the source text from between the boundaries,
         * and recompose it.
         *
         * We may need to remove the last few characters from the ReorderingBuffer
         * to account for source text that was copied or appended
         * but needs to take part in the recomposition.
         */

        /*
         * Find the last composition boundary in [prevBoundary..src[.
         * It is either the decomposition of the current character (at prevSrc),
         * or prevBoundary.
         */
        if(Normalizer2Impl_hasCompBoundaryBefore(_this,c, norm16)) {
            prevBoundary=prevSrc;
        } else if(doCompose) {
            ReorderingBuffer_removeSuffix(buffer, (int32_t)(prevSrc-prevBoundary));
        }

        /*  Find the next composition boundary in [src..limit[ - */
        /*  modifies src to point to the next starter. */
        src=(UChar *)Normalizer2Impl_findNextCompBoundary(_this,src, limit);

        /*  Decompose [prevBoundary..src[ into the buffer and then recompose that part of it. */
          recomposeStartIndex=ReorderingBuffer_length(buffer);
          if(!Normalizer2Impl_decomposeShort(_this, prevBoundary, src, buffer, errorCode)) {
            break;
        }
          Normalizer2Impl_recompose(_this, buffer, recomposeStartIndex, _this->onlyContiguous);
        if(!doCompose) {
            if(!ReorderingBuffer_equals(buffer, prevBoundary, src)) {
                return FALSE;
            }
            ReorderingBuffer_remove(buffer);
            prevCC=0;
        }

        /*  Move to the next starter. We never need to look back before this point again. */
        prevBoundary=src;
    }
    return TRUE;
}


/*  Very similar to compose(): Make the same changes in both places if relevant. */
/*  pQCResult==NULL: spanQuickCheckYes */
/*  pQCResult!=NULL: quickCheck (*pQCResult must be UNORM_YES) */
static const UChar *
Normalizer2_comp_composeQuickCheck(Normalizer2 *_this, const UChar *src, const UChar *limit,
                                   UBool onlyContiguous,
                                   UNormalizationCheckResult *pQCResult)  {
    /*
     * prevBoundary points to the last character before the current one
     * that has a composition boundary before it with ccc==0 and quick check "yes".
     */
    const UChar *prevBoundary=src;
    const UChar *prevSrc;
    UChar32 c=0;
    uint16_t norm16=0;
    uint8_t prevCC=0;
    UChar32 minNoMaybeCP=_this->minCompNoMaybeCP;

    if(limit==NULL) {
        UErrorCode errorCode=U_ZERO_ERROR;
        src=Normalizer2_fcd_copyLowPrefixFromNulTerminated(_this, src, minNoMaybeCP, NULL, &errorCode);
        limit=u_strchr(src, 0);
    }


    for(;;) {
        /*  count code units below the minimum or with irrelevant data for the quick check */
        for(prevSrc=src;;) {
            if(src==limit) {
                return src;
            }
            if( (c=*src)<minNoMaybeCP ||
                isCompYesAndZeroCC(norm16=UTRIE2_GET16_FROM_U16_SINGLE_LEAD(_this->normTrie, c))
            ) {
                ++src;
            } else if(!U16_IS_SURROGATE(c)) {
                break;
            } else {
                UChar c2;
                if(U16_IS_SURROGATE_LEAD(c)) {
                    if((src+1)!=limit && U16_IS_TRAIL(c2=src[1])) {
                        c=U16_GET_SUPPLEMENTARY(c, c2);
                    }
                } else /* trail surrogate */ {
                    if(prevSrc<src && U16_IS_LEAD(c2=*(src-1))) {
                        --src;
                        c=U16_GET_SUPPLEMENTARY(c2, c);
                    }
                }
                if(isCompYesAndZeroCC(norm16=getNorm16(c))) {
                    src+=U16_LENGTH(c);
                } else {
                    break;
                }
            }
        }
        if(src!=prevSrc) {
            /*  Set prevBoundary to the last character in the quick check loop. */
            prevBoundary=src-1;
            if( U16_IS_TRAIL(*prevBoundary) && prevSrc<prevBoundary &&
                U16_IS_LEAD(*(prevBoundary-1))
            ) {
                --prevBoundary;
            }
            prevCC=0;
            /*  The start of the current character (c). */
            prevSrc=src;
        }

        src+=U16_LENGTH(c);
        /*
         * isCompYesAndZeroCC(norm16) is false, that is, norm16>=minNoNo.
         * c is either a "noNo" (has a mapping) or a "maybeYes" (combines backward)
         * or has ccc!=0.
         */
        if(isMaybeOrNonZeroCC(norm16)) {
            uint8_t cc=getCCFromYesOrMaybe(norm16);
            if( onlyContiguous &&  /*  FCC */
                cc!=0 &&
                prevCC==0 &&
                prevBoundary<prevSrc &&
                /*  prevCC==0 && prevBoundary<prevSrc tell us that */
                /*  [prevBoundary..prevSrc[ (which is exactly one character under these conditions) */
                /*  passed the quick check "yes && ccc==0" test. */
                /*  Check whether the last character was a "yesYes" or a "yesNo". */
                /*  If a "yesNo", then we get its trailing ccc from its */
                /*  mapping and check for canonical order. */
                /*  All other cases are ok. */
                Normalizer2_getTrailCCFromCompYesAndZeroCC(_this,prevBoundary, prevSrc)>cc
            ) {
                /*  Fails FCD test. */
            } else if(prevCC<=cc || cc==0) {
                prevCC=cc;
                if(norm16<MIN_YES_YES_WITH_CC) {
                    if(pQCResult!=NULL) {
                        *pQCResult=UNORM_MAYBE;
                    } else {
                        return prevBoundary;
                    }
                }
                continue;
            }
        }
        if(pQCResult!=NULL) {
            *pQCResult=UNORM_NO;
        }
        return prevBoundary;
    }
}


#if UNORM_ENABLE_FCD
static UChar* Normalizer2_fcd_spanQuickCheckYes(struct Normalizer2* n, const UChar *s, const UChar* limit, UErrorCode *pErrorCode) {
  return Normalizer2_fcd_makeFCD(n, s, limit, NULL, pErrorCode);
}

static UChar* Normalizer2_decomp_spanQuickCheckYes(struct Normalizer2* n, const UChar *s, const UChar* limit, UErrorCode *pErrorCode) {
  return Normalizer2_decomp_decompose(n, s, limit, NULL, pErrorCode);
}
#endif
static const UChar* Normalizer2_comp_spanQuickCheckYes(struct Normalizer2* n, const UChar *s, const UChar* limit, UErrorCode *pErrorCode) {
  return Normalizer2_comp_composeQuickCheck(n, s, limit, n->onlyContiguous, NULL);
}

#if UNORM_ENABLE_FCD
static UBool Normalizer2_fcd_isNormalized(struct Normalizer2* n, const UChar *s, int32_t length, UErrorCode *pErrorCode) {
  return((s+length)==Normalizer2_fcd_spanQuickCheckYes(n,s,s+length,pErrorCode));
}
static UBool Normalizer2_decomp_isNormalized(struct Normalizer2* n, const UChar *s, int32_t length, UErrorCode *pErrorCode) {
  return((s+length)==Normalizer2_decomp_spanQuickCheckYes(n,s,s+length,pErrorCode));
}
#endif

#if 0
static UBool Normalizer2_comp_isNormalized(struct Normalizer2* n, const UChar *s, int32_t length, UErrorCode *pErrorCode) {
  return((s+length)==Normalizer2_comp_spanQuickCheckYes(n,s,s+length,pErrorCode));
}
#endif


#if UNORM_ENABLE_FCD
static UNormalizationCheckResult U_CALLCONV Normalizer2_fcd_quickCheck(struct Normalizer2* n, const UChar *s, int32_t length, UErrorCode *pErrorCode) {
  return Normalizer2_fcd_isNormalized(n, s, length, pErrorCode)?UNORM_YES:UNORM_NO;
}
#endif

#if UNORM_ENABLE_FCD
static UNormalizationCheckResult U_CALLCONV Normalizer2_decomp_quickCheck(struct Normalizer2* n, const UChar *s, int32_t length, UErrorCode *pErrorCode) {
  return Normalizer2_decomp_isNormalized(n, s, length, pErrorCode)?UNORM_YES:UNORM_NO;
}
#endif
static UNormalizationCheckResult U_CALLCONV Normalizer2_comp_quickCheck(struct Normalizer2* n, const UChar *s, int32_t length, UErrorCode *pErrorCode) {
  UNormalizationCheckResult qcResult=UNORM_YES;
  Normalizer2_comp_composeQuickCheck(n, s, s+length, n->onlyContiguous, &qcResult);
  return qcResult;
}




U_DRAFT const UNormalizer2 * U_EXPORT2
unorm2_get2Instance(const char *packageName,
                   const char *name,
                   UNormalizationMode mode,
                   UErrorCode *errorCode) {
  Normalizer2 *_this = NULL;
  if(U_FAILURE(*errorCode)) {
    return NULL;
  }
  _this =  uprv_malloc(sizeof(Normalizer2));
  if(_this==NULL) {
    *errorCode = U_MEMORY_ALLOCATION_ERROR;
    goto cleanup;
  }
  uprv_memset(_this,sizeof(Normalizer2),0); /* zero out */

  if(name == NULL) {
    /* no-op */
    _this->quickCheck = Normalizer2_noop_quickCheck;
    _this->normalize  = Normalizer2_noop_normalize;
  } else {
    Normalizer2_load(_this, packageName, name, errorCode); 
    
    _this->mode = mode;

    /* Set up functions */
    _this->close = Normalizer2_close;

    _this->onlyContiguous = FALSE; /* maybe true for FCC? */

    switch(mode) {
#if UNORM_ENABLE_FCD
    case UNORM_FCD:
      {
        FCDTrieSingleton_createInstance(_this, errorCode);
        _this->quickCheck = Normalizer2_fcd_quickCheck;
        _this->normalize = Normalizer2_noop_normalize;        
      }
      break;
    case UNORM_NFD:
      {
        _this->quickCheck = Normalizer2_decomp_quickCheck;
        _this->normalize = Normalizer2_noop_normalize;        
      }
      break;
#endif
    case UNORM_NFC:
      {
        _this->quickCheck = Normalizer2_comp_quickCheck;
        _this->normalize = Normalizer2_comp_normalize;      
#if defined(UNORM_DEBUG)
        fprintf(stderr, "setting NFC for mode=%s\n", MODENAME(mode));
#endif
      }
      break;
    default:
      {
        _this->quickCheck = Normalizer2_noop_quickCheck;
        _this->normalize = Normalizer2_noop_normalize;
      }
      break;
    }

    if(_this->normalize == Normalizer2_noop_normalize) {
#if defined(UNORM_DEBUG)
      fprintf(stderr, "IMP: using noop for %d=%s [name=%s] normalize\n", (int)mode, MODENAME(mode),name);
#endif
    }
    if(_this->quickCheck == Normalizer2_noop_quickCheck) {
#if defined(UNORM_DEBUG)
      fprintf(stderr, "IMP: using noop for %d=%s [name=%s] quickCheck\n", (int)mode, MODENAME(mode),name);
#endif
    }
    

  }
  
  if(U_FAILURE(*errorCode)) {
    goto cleanup;
  }
  
  return (UNormalizer2*)_this;
 cleanup:
  if(_this !=NULL) {
    unorm2_close((UNormalizer2*)_this);
    /* uprv_free(_this);*/
  }
  return NULL;
}
U_DRAFT const UNormalizer2 * U_EXPORT2
unorm2_getInstance(const char *packageName,
                   const char *name,
                   UNormalization2Mode mode,
                   UErrorCode *errorCode) {
  if(U_FAILURE(*errorCode)) return NULL;
  switch(mode) {
  case UNORM2_COMPOSE:
#if defined(UNORM_DEBUG)
    printf("using UNORM_NFC for: unorm2_getInstance(%s,%s,%s...\n", 
           packageName,name,MODE2NAME(mode));
#endif
    return unorm2_get2Instance(packageName,name,UNORM_NFC, errorCode);
  default:
#if defined(UNORM_DEBUG)
    printf("Unimplemented: unorm2_getInstance(%s,%s,%s...\n", 
           packageName,name,MODE2NAME(mode));
#endif
    *errorCode = U_REGEX_UNIMPLEMENTED;
    return NULL;
  }
}


U_DRAFT void U_EXPORT2
unorm2_close(UNormalizer2 *norm2) {
  Normalizer2 *norm = (Normalizer2*)norm2;
  if(norm==NULL) return;
  if((norm->close)!=NULL) norm->close(norm);
  uprv_free(norm2);
}


U_DRAFT UNormalizationCheckResult U_EXPORT2
unorm2_quickCheck(const UNormalizer2 *norm2,
                  const UChar *s, int32_t length,
                  UErrorCode *pErrorCode) {
  Normalizer2 *norm = (Normalizer2*)norm2;
    if(U_FAILURE(*pErrorCode)) {
        return UNORM_NO;
    }
    if(s==NULL || length<-1) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return UNORM_NO;
    }
  return norm->quickCheck(norm, s, length, pErrorCode);
}


U_DRAFT int32_t U_EXPORT2
unorm2_normalize(const UNormalizer2 *norm2,
                 const UChar *src, int32_t length,
                 UChar *dest, int32_t capacity,
                 UErrorCode *pErrorCode) {
  Normalizer2 *norm = (Normalizer2*)norm2;
  if(U_FAILURE(*pErrorCode)) {
    return 0;
  }
  if(src==NULL || length<-1 || capacity<0 || (dest==NULL && capacity>0) || src==dest) {
    *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }
  return norm->normalize(norm,src,length,dest,capacity,pErrorCode);
}



/** UNORM  { for tests.. } */



U_CAPI UNormalizationCheckResult U_EXPORT2
unorm_quickCheck(const UChar *src,
                 int32_t srcLength, 
                 UNormalizationMode mode,
                 UErrorCode *pErrorCode) {
  const UNormalizer2 *n2= Normalizer2Factory_getInstance(mode, pErrorCode); 
  return unorm2_quickCheck(n2, src, srcLength, pErrorCode);
}

/** Public API for normalizing. */
U_CAPI int32_t U_EXPORT2
unorm_normalize(const UChar *src, int32_t srcLength,
                UNormalizationMode mode, int32_t options,
                UChar *dest, int32_t destCapacity,
                UErrorCode *pErrorCode) {
  const UNormalizer2 *n2= Normalizer2Factory_getInstance(mode, pErrorCode); 
  /* if(options&UNORM_UNICODE_3_2) { */
  /*   FilteredNormalizer2 fn2(*n2, *uniset_getUnicode32Instance(*pErrorCode)); */
  /*   return unorm2_normalize((const UNormalizer2 *)&fn2, */
  /*                           src, srcLength, dest, destCapacity, pErrorCode); */
  /* } else */ {
    return unorm2_normalize(n2,
                            src, srcLength, dest, destCapacity, pErrorCode);
  }
}




#endif
