/*
*******************************************************************************
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
* Modification history
* Date        Name      Comments
* 02/16/2001  synwee    Added internal method getPrevSpecialCE 
* 03/01/2001  synwee    Added maxexpansion functionality.
*/
#include "ucolimp.h"
#include "ucoltok.h"
#include "ucaelems.h"

#include "unicode/uloc.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/ustring.h"
#include "unicode/normlzr.h"
#include "unicode/unorm.h"
#include "unicode/udata.h"

#include "cpputils.h"
#include "cstring.h"
#include "ucmp32.h"
#include "ucmp16.h"
#include "umutex.h"
#include "uhash.h"
/* checkout this one - it might be replaceable by something faster */
#include "dcmpdata.h"

#include <stdio.h>



static UCollator* UCA = NULL;
static const InverseTableHeader* invUCA = NULL;

extern "C" UBool checkFCD(const UChar*, int32_t, UErrorCode*);

/* Fixup table a la Markus */
/* see http://www.ibm.com/software/developer/library/utf16.html for further explanation */
static uint8_t utf16fixup[32] = {
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0x20, 0xf8, 0xf8, 0xf8, 0xf8
};

static UBool U_CALLCONV
isAcceptableUCA(void *context, 
             const char *type, const char *name,
             const UDataInfo *pInfo){
  /* context, type & name are intentionally not used */
    if( pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x55 &&   /* dataFormat="UCol" */
        pInfo->dataFormat[1]==0x43 &&
        pInfo->dataFormat[2]==0x6f &&
        pInfo->dataFormat[3]==0x6c &&
        pInfo->formatVersion[0]==1 &&
        pInfo->dataVersion[0]==3 &&
        pInfo->dataVersion[1]==0 &&
        pInfo->dataVersion[2]==0 &&
        pInfo->dataVersion[3]==0) {
        return TRUE;
    } else {
        return FALSE;
    }
}

static UBool U_CALLCONV
isAcceptableInvUCA(void *context, 
             const char *type, const char *name,
             const UDataInfo *pInfo){
  /* context, type & name are intentionally not used */
    if( pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x49 &&   /* dataFormat="InvC" */
        pInfo->dataFormat[1]==0x6e &&
        pInfo->dataFormat[2]==0x76 &&
        pInfo->dataFormat[3]==0x43 &&
        pInfo->formatVersion[0]==1 &&
        pInfo->dataVersion[0]==3 &&
        pInfo->dataVersion[1]==0 &&
        pInfo->dataVersion[2]==0 &&
        pInfo->dataVersion[3]==0) {
        return TRUE;
    } else {
        return FALSE;
    }
}

int32_t ucol_inv_findCE(uint32_t CE, uint32_t SecondCE) {
  uint32_t bottom = 0, top = invUCA->tableSize;
  uint32_t i = 0;
  uint32_t first = 0, second = 0;
  uint32_t *CETable = (uint32_t *)((uint8_t *)invUCA+invUCA->table);

  while(bottom < top-1) {
    i = (top+bottom)/2;
    first = *(CETable+3*i);
    second = *(CETable+3*i+1);
    if(first > CE) {
      top = i;
    } else if(first < CE) {
      bottom = i;
    } else {
        if(second > SecondCE) {
          top = i;
        } else if(second < SecondCE) {
          bottom = i;
        } else {
          break;
        }
    }
  }

  if((first == CE && second == SecondCE)) {
    return i;
  } else {
    return -1;
  }
}

static uint32_t strengthMask[UCOL_CE_STRENGTH_LIMIT] = {
  0xFFFF0000,
  0xFFFFFF00,
  0xFFFFFFFF
};

int32_t ucol_inv_getPrevious(UColTokListHeader *lh, uint32_t strength) {

  uint32_t CE = lh->baseCE;
  uint32_t SecondCE = lh->baseContCE; 

  uint32_t *CETable = (uint32_t *)((uint8_t *)invUCA+invUCA->table);
  uint32_t previousCE, previousContCE;
  int32_t iCE;

  iCE = ucol_inv_findCE(CE, SecondCE);

  if(iCE<0) {
    return -1;
  }

  CE &= strengthMask[strength];
  SecondCE &= strengthMask[strength];

  previousCE = CE;
  previousContCE = SecondCE;

  while((previousCE  & strengthMask[strength]) == CE && (previousContCE  & strengthMask[strength])== SecondCE) {
    previousCE = (*(CETable+3*(--iCE)));
    previousContCE = (*(CETable+3*(iCE)+1));
  }
  lh->previousCE = previousCE;
  lh->previousContCE = previousContCE;

  return iCE;
}

int32_t ucol_inv_getNext(UColTokListHeader *lh, uint32_t strength) {
  uint32_t CE = lh->baseCE;
  uint32_t SecondCE = lh->baseContCE; 

  uint32_t *CETable = (uint32_t *)((uint8_t *)invUCA+invUCA->table);
  uint32_t nextCE, nextContCE;
  int32_t iCE;

  iCE = ucol_inv_findCE(CE, SecondCE);

  if(iCE<0) {
    return -1;
  }

  CE &= strengthMask[strength];
  SecondCE &= strengthMask[strength];

  nextCE = CE;
  nextContCE = SecondCE;

  while((nextCE  & strengthMask[strength]) == CE 
    && (nextContCE  & strengthMask[strength]) == SecondCE) {
    nextCE = (*(CETable+3*(++iCE)));
    nextContCE = (*(CETable+3*(iCE)+1));
  }

  lh->nextCE = nextCE;
  lh->nextContCE = nextContCE;

  return iCE;
}

U_CFUNC void ucol_inv_getGapPositions(UColTokListHeader *lh) {
  /* reset all the gaps */
  int32_t i = 0;
  uint32_t *CETable = (uint32_t *)((uint8_t *)invUCA+invUCA->table);
  uint32_t st = 0;
  uint32_t t1, t2;
  int32_t pos;


  UColToken *tok = lh->first[UCOL_TOK_POLARITY_POSITIVE];
  uint32_t tokStrength = tok->strength;

  for(i = 0; i<3; i++) {
    lh->gapsHi[3*i] = 0;
    lh->gapsHi[3*i+1] = 0;
    lh->gapsHi[3*i+2] = 0;
    lh->gapsLo[3*i] = 0;
    lh->gapsLo[3*i+1] = 0;
    lh->gapsLo[3*i+2] = 0;
    lh->numStr[i] = 0;
    lh->fStrToken[i] = NULL;
    lh->lStrToken[i] = NULL;
    lh->pos[i] = -1;
  }

  if(lh->baseCE == UCOL_RESET_TOP_VALUE && lh->baseContCE == 0) {
    lh->pos[0] = 0;
    t1 = UCOL_RESET_TOP_VALUE;
    t2 = 0;
    lh->gapsLo[0] = (t1 & UCOL_PRIMARYMASK);
    lh->gapsLo[1] = (t1 & UCOL_SECONDARYMASK) << 16;
    lh->gapsLo[2] = (UCOL_TERTIARYORDER(t1)) << 24;
    t1 = UCOL_NEXT_TOP_VALUE;
    t2 = 0;
    lh->gapsHi[0] = (t1 & UCOL_PRIMARYMASK);
    lh->gapsHi[1] = (t1 & UCOL_SECONDARYMASK) << 16;
    lh->gapsHi[2] = (UCOL_TERTIARYORDER(t1)) << 24;
  } else {
    for(;;) {
      if(tokStrength < UCOL_CE_STRENGTH_LIMIT) {
        if((lh->pos[tokStrength] = ucol_inv_getNext(lh, tokStrength)) >= 0) {
          lh->fStrToken[tokStrength] = tok;
        } else {
          /* Error */
          fprintf(stderr, "Error! couldn't find the CE!\n");
        }
      }

      while(tok != NULL && tok->strength >= tokStrength) {
        if(tokStrength < UCOL_CE_STRENGTH_LIMIT) {
          lh->lStrToken[tokStrength] = tok;
        }
        tok = tok->next;
      }
      if(tokStrength < UCOL_CE_STRENGTH_LIMIT-1) {
        /* check if previous interval is the same and merge the intervals if it is so */
        if(lh->pos[tokStrength] == lh->pos[tokStrength+1]) {
          lh->fStrToken[tokStrength] = lh->fStrToken[tokStrength+1];
          lh->fStrToken[tokStrength+1] = NULL;
          lh->lStrToken[tokStrength+1] = NULL;
          lh->pos[tokStrength+1] = -1;
        }
      }
      if(tok != NULL) {
        tokStrength = tok->strength;
      } else {
        break;
      }
    }
    for(st = 0; st < 3; st++) {
      if((pos = lh->pos[st]) >= 0) {
        t1 = *(CETable+3*(pos));
        t2 = *(CETable+3*(pos)+1);
        lh->gapsHi[3*st] = (t1 & UCOL_PRIMARYMASK) | (t2 & UCOL_PRIMARYMASK) >> 16;
        lh->gapsHi[3*st+1] = (t1 & UCOL_SECONDARYMASK) << 16 | (t2 & UCOL_SECONDARYMASK) << 8;
        lh->gapsHi[3*st+2] = (UCOL_TERTIARYORDER(t1)) << 24 | (UCOL_TERTIARYORDER(t2)) << 16;
        pos--;
        t1 = *(CETable+3*(pos));
        t2 = *(CETable+3*(pos)+1);
        lh->gapsLo[3*st] = (t1 & UCOL_PRIMARYMASK) | (t2 & UCOL_PRIMARYMASK) >> 16;
        lh->gapsLo[3*st+1] = (t1 & UCOL_SECONDARYMASK) << 16 | (t2 & UCOL_SECONDARYMASK) << 8;
        lh->gapsLo[3*st+2] = (UCOL_TERTIARYORDER(t1)) << 24 | (UCOL_TERTIARYORDER(t2)) << 16;
      }
    }
  }


}

/****************************************************************************/
/* Following are the open/close functions                                   */
/*                                                                          */
/****************************************************************************/
U_CAPI UCollator*
ucol_open(    const    char         *loc,
        UErrorCode      *status)
{
  /* New version */
  if(U_FAILURE(*status)) return 0;

  ucol_initUCA(status);

  UCollator *result = NULL;
  UResourceBundle *b = ures_open(NULL, loc, status);
  /* first take on tailoring version: */
  /* get CollationElements -> Version */
  UResourceBundle *binary = ures_getByKey(b, "%%CollationNew", NULL, status);

  if(*status == U_MISSING_RESOURCE_ERROR) { /* if we don't find tailoring, we'll fallback to UCA */
    *status = U_USING_DEFAULT_ERROR;
    result = ucol_initCollator(UCA->image, result, status); 
    /*result = UCA;*/
    result->hasRealData = FALSE;
  } else if(U_SUCCESS(*status)) { /* otherwise, we'll pick a collation data that exists */
    int32_t len = 0;
    const uint8_t *inData = ures_getBinary(binary, &len, status);
    if((uint32_t)len > sizeof(UCATableHeader)) {
      result = ucol_initCollator((const UCATableHeader *)inData, result, status); 
      result->hasRealData = TRUE;
    } else {
      result = ucol_initCollator(UCA->image, result, status); 
      ucol_setOptionsFromHeader(result, (const UCATableHeader *)inData, status);
      result->hasRealData = FALSE;
    }
  } else { /* There is another error, and we're just gonna clean up */
    ures_close(b);
    return NULL;
  }

  result->rb = b;
  ures_close(binary);

  return result;
}


U_CAPI void
ucol_close(UCollator *coll)
{
  /* Here, it would be advisable to close: */
  /* - UData for UCA (unless we stuff it in the root resb */
  /* Again, do we need additional housekeeping... HMMM! */
  if(coll->freeOnClose == FALSE){
    return; /* for safeClone, if freeOnClose is FALSE, 
            don't free the other instance data */
  }
  if(coll->mapping != NULL) {
      ucmp32_close(coll->mapping);
  }
  if(coll->rules != NULL && coll->freeRulesOnClose) {
    uprv_free((UChar *)coll->rules);
  }
  if(coll->rb != NULL) { /* pointing to read-only memory */
    ures_close(coll->rb);
  } else if(coll->hasRealData == TRUE) {
    uprv_free((UCATableHeader *)coll->image);
  }
  uprv_free(coll);
}

#define ucol_countBytes(value, noOfBytes)   \
{                               \
  uint32_t mask = 0xFFFFFFFF;   \
  (noOfBytes) = 0;              \
  while(mask != 0) {            \
    if(((value) & mask) != 0) { \
      (noOfBytes)++;            \
    }                           \
    mask >>= 8;                 \
  }                             \
}

U_CFUNC uint32_t ucol_getNextGenerated(ucolCEGenerator *g) {
  g->current += (1<<(32-(g->byteSize*8)));
  if(g->current > g->fLow && g->current < g->fHigh) {
    g->current = g->fHigh;
  }
  return g->current;
}

static uint32_t fbHigh[3] = {0, /*0,*/UCOL_COMMON_TOP2, 0};
static uint32_t fbLow[3] = {0, /*0,*/UCOL_COMMON_BOT2, 0};

U_CFUNC uint32_t ucol_getSimpleCEGenerator(ucolCEGenerator *g, uint32_t low, uint32_t high, UColToken *tok, uint32_t strength) {
  uint32_t count = tok->toInsert;
  uint32_t lobytes = 0xFFFFFFFF, hibytes = 0xFFFFFFFF;

  g->fHigh = fbHigh[strength];
  g->fLow = fbLow[strength];

  ucol_countBytes(low, lobytes);
  ucol_countBytes(high, hibytes);

  g->lowCount = high-low;
  g->midCount = 0;
  g->highCount = 0;

  g->count = count + g->fHigh-g->fLow;

  g->byteSize = 0xFFFFFFFF;
  g->start = 0;
  g->limit = 0;

  if(g->lowCount >= g->count) {
    g->byteSize = lobytes;
    g->start = low;
    g->limit = high;
  } else if((g->lowCount)*254 > g->count) {
    g->byteSize = lobytes+1;
    g->start = low | (0x02 << (32-g->byteSize*8));
    g->limit = high;
  }

  g->current = g->start;
  g->fLow = g->fLow << 24;
  g->fHigh = g->fHigh << 24;

  if(g->current > g->fLow && g->current < g->fHigh) {
    g->current = g->fHigh;
  }
  return g->current;

}

U_CFUNC uint32_t ucol_getCEGenerator(ucolCEGenerator *g, uint32_t* lows, uint32_t* highs, UColToken *tok, uint32_t fStrength, uint32_t strength) {
  uint32_t low = lows[fStrength*3+strength];
  uint32_t high = highs[fStrength*3+strength];

  uint32_t count = tok->toInsert;

  g->fHigh = fbHigh[strength];
  g->fLow = fbLow[strength];

  uint32_t lobytes = 0, hibytes = 0;

  ucol_countBytes(low, lobytes);
  ucol_countBytes(high, hibytes);

  if(low == high) {
#ifdef UCOL_DEBUG
    fprintf(stderr, "problem? low:%08X is equal to high:%08X at the strength level %i\n", low, high, strength);
#endif
    if(strength > 0 && lows[fStrength*3+(strength-1)] != highs[fStrength*3+(strength-1)]) {
      low = (0x02 << (32-lobytes*8));
      high = (0x00 << (32-hibytes*8));
#ifdef UCOL_DEBUG
      fprintf(stderr, "resolved! stronger strengths do give solution: %08X != %08X!\n", lows[fStrength*3+(strength-1)], highs[fStrength*3+(strength-1)]);
#endif
    } else {
#ifdef UCOL_DEBUG
      fprintf(stderr, "bad! stronger strengths do not give solution: %08X == %08X!\n", lows[fStrength*3+(strength-1)], highs[fStrength*3+(strength-1)]);
#endif
    }
  }


  g->firstLow = low + (1 << (32-lobytes*8));
  g->lastHigh = high - (1 << (32-hibytes*8));

  if(g->firstLow != g->lastHigh) {

    if(lobytes > 1) {
      g->firstMid = g->firstLow + (1 << (32-(lobytes-1)*8)) & (0xFFFFFF00 << (32-lobytes*8));
      g->lastLow = g->firstMid - (1 << (32-lobytes*8));
    } else if(lobytes < hibytes) {
      g->lastLow = g->lastHigh - (1 << (32-(hibytes-1)*8)) & (0xFFFFFF00 << (32-hibytes*8));
      g->firstMid = g->lastMid = 0;
    } else {
      g->lastLow = g->lastHigh;
      g->firstMid = g->lastMid = 0;
    }

    if(hibytes > 1) {
      g->lastMid = g->lastHigh - (1 << (32-(hibytes-1)*8)) & (0xFFFFFF00 << (32-hibytes*8));
      g->firstHigh = g->lastMid + (1 << (32-(hibytes-1)*8)) + (0x02 << (32-(hibytes)*8));
    } else if(lobytes > hibytes) {
      g->firstHigh = g->firstLow + (1 << (32-(lobytes-1)*8)) & (0xFFFFFF00 << (32-lobytes*8));
      g->firstMid = g->lastMid = 0;
    } else {
      g->firstHigh = g->firstLow;
      g->firstMid = g->lastMid = 0;
    }

    ucol_countBytes(g->lastLow, g->lowByteCount);
    ucol_countBytes(g->lastMid, g->midByteCount);
    ucol_countBytes(g->lastHigh, g->highByteCount);

    if(g->firstLow < low || g->lastLow > high) {
      g->firstLow = g->lastLow = 0;
      g->lowByteCount = 0xFFFF;
    }
    if(g->firstMid < low || g->lastMid > high) {
      g->firstMid = g->lastMid = 0;
      g->midByteCount = 0xFFFF;
    }
    if(g->firstHigh < low || g->lastHigh > high) {
      g->firstHigh = g->lastHigh = 0;
      g->highByteCount = 0xFFFF;
    }


    g->maxCount = g->lowCount = (g->lastLow - g->firstLow) >> (32-g->lowByteCount*8);
    g->midCount = (g->lastMid - g->firstMid) >> (32-g->midByteCount*8);
    if(g->midCount > g->maxCount) {
      g->maxCount = g->midCount;
    }
    g->highCount = (g->lastHigh - g->firstHigh) >> (32-g->highByteCount*8);
    if(g->highCount > g->maxCount) {
      g->maxCount = g->highCount;
    }

    g->count = count;

    g->byteSize = 0xFFFFFFFF;
    g->start = 0;
    g->limit = 0;

    /* Let's get the best one now */
    if(g->maxCount > g->count) {
      if(g->lowCount > count+(g->fHigh - g->fLow) ) {
        g->byteSize = g->lowByteCount;
        g->start = g->firstLow;
        g->limit = g->lastLow;
      }

      if(g->midCount > count+(g->fHigh - g->fLow)  && g->midByteCount < g->byteSize) {
        g->byteSize = g->midByteCount;
        g->start = g->firstMid;
        g->limit = g->lastMid;
      }

      if(g->highCount > count+(g->fHigh - g->fLow)  && g->highByteCount < g->byteSize) {
        g->byteSize = g->highByteCount;
        g->start = g->firstHigh;
        g->limit = g->lastHigh;
      }
    }

    if(g->byteSize == 0xFFFFFFFF && g->maxCount*254 > g->count) { /* Still no solution */
      if((g->lowCount)*254 > count+(g->fHigh - g->fLow) ) {
        g->byteSize = g->lowByteCount+1;
        g->start = g->firstLow | (0x02 << (32-g->byteSize*8));
        g->limit = g->lastLow;
      }

      if((g->midCount)*254 > count+(g->fHigh - g->fLow) && g->midByteCount+1 < g->byteSize) {
        g->byteSize = g->midByteCount+1;
        g->start = g->firstMid | (0x02 << (32-g->byteSize*8));
        g->limit = g->lastMid;
      }

      if((g->highCount)*254 > count+(g->fHigh - g->fLow) && g->highByteCount+1 < g->byteSize) {
        g->byteSize = g->highByteCount+1;
        g->start = g->firstHigh | (0x02 << (32-g->byteSize*8));
        g->limit = g->lastHigh | (0xFF << (32-g->byteSize*8));
      }
    }
    if(g->byteSize == 0xFFFFFFFF) { /* Still no solution, we need to see if we can do anything about it */
#ifdef UCOL_DEBUG
      fprintf(stderr, "Too many elements to fit! %08X in %08X, will try splitting\n", g->count, g->maxCount*254);
#endif
    }

    g->current = g->start;
  } else { /* only trivial space size 1 */
    if(count == 1) {
      g->byteSize = lobytes;
      g->current = g->start = g->limit = g->firstLow;
    } else if(count < 254) {
      g->byteSize = lobytes+1;
      g->current = g->start = g->firstLow | (0x02 << (32-g->byteSize*8));
      g->limit = g->firstLow | (0xFF << (32-g->byteSize*8));
    } else {
      g->byteSize = lobytes+2;
      g->current = g->start = g->firstLow | (0x0202 << (32-g->byteSize*8));
      g->limit = g->firstLow | (0xFFFF << (32-g->byteSize*8));
    }
  }
  g->fLow = g->fLow << 24;
  g->fHigh = g->fHigh << 24;

  if(g->current > g->fLow && g->current < g->fHigh) {
    g->current = g->fHigh;
  }
  return g->current;
}

U_CFUNC void ucol_doCE(uint32_t *CEparts, UColToken *tok, UHashtable *tailored, UErrorCode *status) {
  /* this one makes the table and stuff */
  uint32_t noOfBytes[3];
  uint32_t i;

  for(i = 0; i<3; i++) {
    ucol_countBytes(CEparts[i], noOfBytes[i]);
  }

  /* Here we have to pack CEs from parts */

  uint32_t CEi = 0;
  uint32_t value = 0;

  while(2*CEi<noOfBytes[0] || CEi<noOfBytes[1] || CEi<noOfBytes[2]) {
    if(CEi > 0) {
      value = 0x80; /* Continuation marker */
    } else {
      value = 0;
    }

    if(tok->caseBit == TRUE) {
      value |= 0x40;
    }

    if(2*CEi<noOfBytes[0]) {
      value |= ((CEparts[0]>>(32-16*(CEi+1))) & 0xFFFF) << 16;
    }
    if(CEi<noOfBytes[1]) {
      value |= ((CEparts[1]>>(32-8*(CEi+1))) & 0xFF) << 8;
    }
    if(CEi<noOfBytes[2]) {
      value |= ((CEparts[2]>>(32-8*(CEi+1))) & 0x3F);
    }
    tok->CEs[CEi] = value;
    CEi++;
  }
  if(CEi == 0) { /* totally ignorable */
    tok->noOfCEs = 1;
    tok->CEs[0] = 0;
  } else { /* there is at least something */
    tok->noOfCEs = CEi;
  }


  /* We'll need to handle expansions slightly differently than in */
  /* UCA generation since we don't know if the value for expansion is from UCA or is it tailored */

  uhash_put(tailored, (void *)tok->source, tok, status);


  /* and add them to a data table        */
#if UCOL_DEBUG==2
  fprintf(stderr, "%04X str: %i, [%08X, %08X, %08X]: tok: ", tok->debugSource, tok->strength, CEparts[0] >> (32-8*noOfBytes[0]), CEparts[1] >> (32-8*noOfBytes[1]), CEparts[2]>> (32-8*noOfBytes[2]));
  for(i = 0; i<tok->noOfCEs; i++) {
    fprintf(stderr, "%08X ", tok->CEs[i]);
  }
  fprintf(stderr, "\n");
#endif
}

U_CFUNC void ucol_initBuffers(UColTokListHeader *lh, UHashtable *tailored, UErrorCode *status) {

  ucolCEGenerator Gens[UCOL_CE_STRENGTH_LIMIT];
  uint32_t CEparts[UCOL_CE_STRENGTH_LIMIT];

  uint32_t i = 0;

  UColToken *tok = lh->last[UCOL_TOK_POLARITY_POSITIVE];
  uint32_t t[UCOL_STRENGTH_LIMIT];

  for(i=0; i<UCOL_STRENGTH_LIMIT; i++) {
    t[i] = 0;
  }

  tok->toInsert = 1;
  t[tok->strength] = 1;

  while(tok->previous != NULL) {
    if(tok->previous->strength < tok->strength) { /* going up */
      t[tok->strength] = 0;
      t[tok->previous->strength]++;
    } else if(tok->previous->strength > tok->strength) { /* going down */
      t[tok->previous->strength] = 1;
    } else {
      t[tok->strength]++;
    }
    tok=tok->previous;
    tok->toInsert = t[tok->strength];
  } 

  tok->toInsert = t[tok->strength];
/*
  tok=lh->first[UCOL_TOK_POLARITY_POSITIVE];

  do {
    fprintf(stderr,"%i", tok->strength);
    tok = tok->next;
  } while(tok != NULL);
  fprintf(stderr, "\n");

  tok=lh->first[UCOL_TOK_POLARITY_POSITIVE];

  do {  
    fprintf(stderr,"%i", tok->toInsert);
    tok = tok->next;
  } while(tok != NULL);
*/

  ucol_inv_getGapPositions(lh);
#if UCOL_DEBUG
  fprintf(stderr, "BaseCE: %08X %08X\n", lh->baseCE, lh->baseContCE);
  int32_t j = 2;
  for(j = 2; j >= 0; j--) {
    fprintf(stderr, "gapsLo[%i] [%08X %08X %08X]\n", j, lh->gapsLo[j*3], lh->gapsLo[j*3+1], lh->gapsLo[j*3+2]);
    fprintf(stderr, "gapsHi[%i] [%08X %08X %08X]\n", j, lh->gapsHi[j*3], lh->gapsHi[j*3+1], lh->gapsHi[j*3+2]);
  }
#endif
  /* I strongly believe that this code can be refactored and simplified. */
  /* have to do CE generation now, so let this soak a little bit */

  tok = lh->first[UCOL_TOK_POLARITY_POSITIVE];
  uint32_t fStrength = tok->strength;

  /* Treat starting identicals */
  /* &0 = nula = zero */
  if(tok != NULL && fStrength == UCOL_IDENTICAL) {
    CEparts[0] = (lh->baseCE & UCOL_PRIMARYMASK) | (lh->baseContCE & UCOL_PRIMARYMASK) >> 16;
    CEparts[1] = (lh->baseCE & UCOL_SECONDARYMASK) << 16 | (lh->baseContCE & UCOL_SECONDARYMASK) << 8;
    CEparts[2] = (UCOL_TERTIARYORDER(lh->baseCE)) << 24 | (UCOL_TERTIARYORDER(lh->baseContCE)) << 16;

    while(tok != NULL && tok->strength == UCOL_IDENTICAL) {
      ucol_doCE(CEparts, tok, tailored, status);
      tok = tok->next;
    }

  }

  if(tok != NULL && tok->strength == UCOL_TERTIARY) { /* starting with tertiary */
    fStrength = tok->strength;
    if(lh->pos[fStrength] == -1) {
      while(lh->pos[fStrength] == -1 && fStrength > 0) {
        fStrength--;
      }
      if(lh->pos[fStrength] == -1) {
        fprintf(stderr, "OH MY GOD! NO PLACE TO PUT TERTIARIES!\n");
        exit(-1);
      }
    }
    CEparts[0] = lh->gapsLo[fStrength*3];
    CEparts[1] = lh->gapsLo[fStrength*3+1];
    /*CEparts[UCOL_TERTIARY] = ucol_getCEGenerator(&Gens[2], lh->gapsLo[fStrength*3+2], lh->gapsHi[fStrength*3+2], tok, UCOL_TERTIARY); */
    CEparts[UCOL_TERTIARY] = ucol_getCEGenerator(&Gens[2], lh->gapsLo, lh->gapsHi, tok, fStrength, UCOL_TERTIARY); 

    while(tok != NULL && tok->strength >= UCOL_TERTIARY) {
      ucol_doCE(CEparts, tok, tailored, status);
      tok = tok->next;

      /* Treat identicals in starting tertiaries by NOT changing the tertiary value */
      if(tok != NULL && tok->strength == UCOL_TERTIARY) {
        CEparts[2] = ucol_getNextGenerated(&Gens[2]);
      }
    }     

  }

  if(tok != NULL && tok->strength == UCOL_SECONDARY) { /* secondaries */
    fStrength = tok->strength;
    if(lh->pos[1] == -1) {
      fStrength = 0;
      if(lh->pos[fStrength] == -1) {
        fprintf(stderr, "OH MY GOD! NO PLACE TO PUT SECONDARIES!\n");
        exit(-1);
      }
    }
    if(tok->next != NULL) {
    /* Treat identicals in starting secondaries*/
    /* &0 [, <funny_tertiary_different_zero>] ;  <funny_secondary_different_zero> = FunnySecZero */

      CEparts[0] = lh->gapsLo[fStrength*3];
      /*CEparts[1] = ucol_getCEGenerator(&Gens[1], lh->gapsLo[fStrength*3+1], lh->gapsHi[fStrength*3+1], tok, 1);*/
      CEparts[1] = ucol_getCEGenerator(&Gens[1], lh->gapsLo, lh->gapsHi, tok, fStrength, 1);
      if(tok->next->strength == UCOL_TERTIARY) {
        CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[2], 0x03000000, 0xFF000000, tok->next, UCOL_TERTIARY);
      } else {
        CEparts[UCOL_TERTIARY] = 0x03000000;
      }
    
      ucol_doCE(CEparts, tok, tailored, status);
      tok = tok->next;

      while(tok->next != NULL && tok->next->strength > 0) {
        if(tok->strength == UCOL_TERTIARY) {
          CEparts[2] = ucol_getNextGenerated(&Gens[2]);
          ucol_doCE(CEparts, tok, tailored, status);
        } else if(tok->strength == UCOL_SECONDARY) {
          CEparts[1] = ucol_getNextGenerated(&Gens[1]);
          if(tok->next->strength == UCOL_SECONDARY) {
            CEparts[UCOL_TERTIARY] = 0x03000000;
          } else {
            CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[2], 0x03000000, 0xFF000000, tok->next, UCOL_TERTIARY);
          }
          ucol_doCE(CEparts, tok, tailored, status);
        } else { /* Strength is identical */
          ucol_doCE(CEparts, tok, tailored, status);
        }
        tok = tok->next;
      }

      /* This is the last token in rule */
      if(tok->strength == UCOL_TERTIARY) {
        CEparts[2] = ucol_getNextGenerated(&Gens[2]);
      } else if(tok->strength == UCOL_SECONDARY) {
        CEparts[1] = ucol_getNextGenerated(&Gens[1]);
        CEparts[2] = 0x03000000;
      }
      /* if the strength is identical, it will just repeat the last CE value */
      ucol_doCE(CEparts, tok, tailored, status);
      tok = tok->next;
    } else { /* only one secondary at the end of the rule fragment */
      CEparts[0] = lh->gapsLo[fStrength*3];
      CEparts[1] = lh->gapsLo[fStrength*3+1];
      CEparts[2] = lh->gapsLo[fStrength*3+2];
      ucol_doCE(CEparts, tok, tailored, status);
      tok = NULL;
    }
  }

  /* This is essentialy the main loop. Two loops in front of this one were just for postponing with lower bounding weights */

  if(tok != NULL) { /* regular primaries */
    fStrength = tok->strength;
    if(lh->pos[0] == -1) {
        fprintf(stderr, "OH MY GOD! NO PLACE TO PUT PRIMARIES!\n");
        exit(-1);
    }

    /* what if the next token is identical??? */
    /* How should the things be set up */

    if(tok->next != NULL) {
      /*CEparts[UCOL_PRIMARY] = ucol_getCEGenerator(&Gens[0], lh->gapsLo[0], lh->gapsHi[0], tok, UCOL_PRIMARY);*/
      CEparts[UCOL_PRIMARY] = ucol_getCEGenerator(&Gens[0], lh->gapsLo, lh->gapsHi, tok, fStrength, UCOL_PRIMARY);
      if(tok->next->strength == UCOL_PRIMARY) {
        CEparts[1] = 0x03000000;
        CEparts[2] = 0x03000000;
      } else { /* Secondaries will also be generated */
        CEparts[1] = ucol_getSimpleCEGenerator(&Gens[1], 0x03000000, 0xFF000000, tok->next, 1);
        if(tok->next->strength == UCOL_SECONDARY) {
          CEparts[UCOL_TERTIARY] = 0x03000000;
        } else {
          CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[2], 0x03000000, 0xFF000000, tok->next, UCOL_TERTIARY);
        }
      }

      ucol_doCE(CEparts, tok, tailored, status);

      tok = tok->next;

      while(tok->next != NULL) {
    /* Treat identicals*/
    /* < 1 = one = jedan < 2 = two = dva < 3 = three = tri ... */
        if(tok->strength == UCOL_IDENTICAL) {
          ucol_doCE(CEparts, tok, tailored, status);
        } else if(tok->strength == UCOL_TERTIARY) {
          CEparts[2] = ucol_getNextGenerated(&Gens[2]);
          ucol_doCE(CEparts, tok, tailored, status);
        } else if(tok->strength == UCOL_SECONDARY) {
          CEparts[1] = ucol_getNextGenerated(&Gens[1]);
          if(tok->next->strength == UCOL_TERTIARY) {
            CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[2], 0x03000000, 0xFF000000, tok->next, UCOL_TERTIARY);
          } else { /* UCOL_SECONDARY */
            CEparts[UCOL_TERTIARY] = 0x03000000;
          }
          ucol_doCE(CEparts, tok, tailored, status);
        } else {
          CEparts[0] = ucol_getNextGenerated(&Gens[0]);
          if(tok->next->strength == UCOL_PRIMARY) {
            CEparts[1] = 0x03000000;
            CEparts[UCOL_TERTIARY] = 0x03000000;
          } else {
            if(tok->next->strength == UCOL_SECONDARY) {
              CEparts[UCOL_TERTIARY] = 0x03000000;
            } else { /* UCOL_TERTIARY */
              CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[2], 0x03000000, 0xFF000000, tok->next, UCOL_TERTIARY);
            }
            CEparts[1] = ucol_getSimpleCEGenerator(&Gens[1], 0x03000000, 0xFF000000, tok->next, 1);
          }
          ucol_doCE(CEparts, tok, tailored, status);
        }
        tok = tok->next;
      }
      
      /* OK, there are no next tokens, we just have to wrap up with the last one */
      if(tok->strength == UCOL_TERTIARY) {
        CEparts[2] = ucol_getNextGenerated(&Gens[2]);
      } else if(tok->strength == UCOL_SECONDARY) {
        CEparts[1] = ucol_getNextGenerated(&Gens[1]);
        CEparts[2] = 0x03000000;
      } else if(tok->strength == UCOL_PRIMARY) {
        CEparts[0] = ucol_getNextGenerated(&Gens[0]);
        CEparts[1] = 0x03000000;
        CEparts[2] = 0x03000000;
      } /* else it is identical and do nothing */
      ucol_doCE(CEparts, tok, tailored, status);

    } else { /* there is only one primary in this sequence and it ends with it */
      CEparts[0] = lh->gapsLo[0];
      CEparts[1] = lh->gapsLo[1];
      CEparts[2] = lh->gapsLo[2];
      ucol_doCE(CEparts, tok, tailored, status);
    }
  }
}

U_CFUNC uint32_t ucol_getFirstCE(UCollator *coll, UChar u, UErrorCode *status) {
  collIterate colIt;
  uint32_t order;
  init_collIterate(&u, 1, &colIt, FALSE);
  order = ucol_getNextCE(coll, &colIt, status);
  /*UCOL_GETNEXTCE(order, coll, colIt, status);*/
  return order;
}

U_CFUNC void ucol_createElements(UColTokenParser *src, tempUCATable *t, UColTokListHeader *lh, UHashtable *tailored, UErrorCode *status) {
  UCAElements el;
  UColToken *tok = lh->first[UCOL_TOK_POLARITY_POSITIVE];
  UColToken *expt = NULL;
  uint32_t i = 0;

  while(tok != NULL) {
    /* first, check if there are any expansions */
    if(tok->expansion != 0) {
      if((expt = (UColToken *)uhash_get(tailored, (void *)tok->expansion)) != NULL) { /* expansion is tailored */
        /* just copy CEs from tailored token to this one */
        for(i = 0; i<expt->noOfCEs; i++) {
          tok->expCEs[i] = expt->CEs[i];
        }
        tok->noOfExpCEs = expt->noOfCEs;
      } else { /* need to pick it from the UCA */
        /* first, get the UChars from the rules */
        /* then pick CEs out until there is no more and stuff them into expansion */
        UChar source[256],buff[256];
        collIterate s;
        uint32_t order = 0;
        uint32_t len = tok->expansion >> 24;
        uprv_memcpy(buff, (tok->expansion & 0x00FFFFFF) + src->source, len*sizeof(UChar));
        unorm_normalize(buff, len, UNORM_NFD, 0, source, 256, status);
        init_collIterate(source, len, &s, FALSE);

        for(;;) {
          UCOL_GETNEXTCE(order, UCA, s, status);
          if(order == UCOL_NO_MORE_CES) {
              break;
          }
          tok->expCEs[tok->noOfExpCEs++] = order;
        }
      }
    } else {
      tok->noOfExpCEs = 0;
    }

    /* set the ucaelement with obtained values */
    el.noOfCEs = tok->noOfCEs + tok->noOfExpCEs;
    /* copy CEs */
    for(i = 0; i<tok->noOfCEs; i++) {
      el.CEs[i] = tok->CEs[i];
    }
    for(i = 0; i<tok->noOfExpCEs; i++) {
      el.CEs[i+tok->noOfCEs] = tok->expCEs[i];
    }

    /* copy UChars */
/*
      key.source = newCharsLen << 24 | charsOffset;
      key.expansion = newExtensionsLen << 24 | extensionOffset;
*/
    UChar buff[128];
    uint32_t decompSize;
    uprv_memcpy(buff, (tok->source & 0x00FFFFFF) + src->source, (tok->source >> 24)*sizeof(UChar));
    decompSize = unorm_normalize(buff, tok->source >> 24, UNORM_NFD, 0, el.uchars, 128, status);
    /*uprv_memcpy(el.uchars, (tok->source & 0x00FFFFFF) + src->source, (tok->source >> 24)*sizeof(UChar));*/
    /* I think I don't want to have expansion chars in chars for UCAelement... HMMM! */
    /*uprv_memcpy(el.uchars+(tok->source >> 24), (tok->expansion & 0x00FFFFFF) + src->source, (tok->expansion >> 24)*sizeof(UChar));*/
    el.cSize = decompSize; /*(tok->source >> 24); *//* + (tok->expansion >> 24);*/
    el.cPoints = el.uchars;

    el.caseBit = FALSE; /* how to see if there is case bit - pick it out from the UCA */
    if(UCOL_ISTHAIPREVOWEL(el.cPoints[0])) {
      el.isThai = TRUE;
    } else {
      el.isThai = FALSE;
    }

    /* we also need a case bit here, and we'll fish it out from the UCA for the first codepoint */
    uint32_t caseCE = ucol_getFirstCE(UCA, el.cPoints[0], status);
    if((caseCE & 0x40) != 0) {
      el.caseBit = TRUE;
/*      el.CEs[0] |= 0x40;*/
      for(i = 0; i<el.noOfCEs; i++) {
        el.CEs[i] |= 0x40;
      }
    } else {
      el.caseBit = FALSE;
/*      el.CEs[0] &= 0xFFFFFFBF;*/
      for(i = 0; i<el.noOfCEs; i++) {
        el.CEs[i] &= 0xFFFFFFBF;
      }
    }


    /* and then, add it */
#if UCOL_DEBUG==2
    fprintf(stderr, "Adding: %04X with %08X\n", el.cPoints[0], el.CEs[0]);
#endif
    uprv_uca_addAnElement(t, &el, status);

    tok = tok->next;
  }

}

/* These are some normalizer constants */
#define STR_INDEX_SHIFT  2 //Must agree with the constants used in NormalizerBuilder
#define STR_LENGTH_MASK 0x0003

int32_t uprv_ucol_decompose (UChar curChar, UChar *result) {
    /* either 0 or MAX_COMPAT = 11177 if we want just canonical */
    int32_t minDecomp = 11177;
    int32_t resSize = 0;
    uint16_t offset = ucmp16_getu(DecompData::offsets, curChar);
    uint16_t index  = (uint16_t)(offset & DecompData::DECOMP_MASK);

    if (index > minDecomp) {
        if ((offset & DecompData::DECOMP_RECURSE) != 0) {
            // Let Normalizer::decompose() handle recursive decomp
            UnicodeString temp(curChar);
            UnicodeString res;
            UErrorCode status = U_ZERO_ERROR;
            Normalizer::decompose(temp, minDecomp > 0,
                                  /*hangul ? Normalizer::IGNORE_HANGUL : 0,*/
                                  Normalizer::IGNORE_HANGUL,
                                  res, status);
            T_fillOutputParams(&res, result, 356, &resSize, &status);

        } else {
          const UChar *source = (const UChar*)&(DecompData::contents);
            uint16_t ind = (int16_t)(index >> STR_INDEX_SHIFT);
            uint16_t length = (int16_t)(index & STR_LENGTH_MASK);

            if (length == 0) {
                UChar ch;
                while ((ch = source[ind++]) != 0x0000) {
                    result[resSize++] = ch;
                }
            } else {
                while (length-- > 0) {
                    result[resSize++] = source[ind++];
                }
            }
        }
        return resSize;
    } 
#if 0
    else if (hangul && curChar >= Normalizer::HANGUL_BASE && curChar < Normalizer::HANGUL_LIMIT) {
        Normalizer::hangulToJamo(curChar, result, (uint16_t)minDecomp);
        /* this has something to do with jamo hangul, check tomorrow */
    } 
#endif
    else {
        /*result += curChar;  this doesn't decompose */
      return 0;
    }

}

uint32_t ucol_getDynamicCEs(tempUCATable *t, UChar *decomp, uint32_t noOfDec, uint32_t *result, uint32_t resultSize, UErrorCode *status) {
  uint32_t j = 0, i = 0;
  uint32_t CE = 0;
  uint32_t resLen = 0;
  collIterate colIt;
  UBool lastNotFound = FALSE;

  
  while(j<noOfDec) {
    CE = ucmp32_get(t->mapping, decomp[j]);
    if(CE == UCOL_NOT_FOUND || lastNotFound) { /* get it from the UCA */
      lastNotFound = FALSE;
      init_collIterate(decomp+j, 1, &colIt, TRUE);
      while(CE != UCOL_NO_MORE_CES) {
        CE = ucol_getNextCE(UCA, &colIt, status);
        if(CE != UCOL_NO_MORE_CES) {
          result[resLen++] = CE;
        }
      }     
    } else if(CE < UCOL_NOT_FOUND) { /*normal CE */
      result[resLen++] = CE;
    } else { /* special CE, contraction, expansion or Thai */
      for(;;) {
        uint32_t tag = getCETag(CE);
        if(tag == THAI_TAG || tag == EXPANSION_TAG) {
            uint32_t *CEOffset = t->expansions->CEs+(getExpansionOffset(CE) - (paddedsize(sizeof(UCATableHeader))>>2)); /* find the offset to expansion table */
            uint32_t size = getExpansionCount(CE);
            if(size != 0) { /* if there are less than 16 elements in expansion, we don't terminate */
              for(i = 1; i<size; i++) {
                 result[resLen++] = *CEOffset++;
              }
            } else { /* else, we do */
              while(*CEOffset != 0) {
                result[resLen++] = *CEOffset++;
              }
            }
            break;
        } else if(tag == CONTRACTION_TAG) {
            ContractionTable *ctb = t->contractions->elements[getContractOffset(CE)];
            UChar c = decomp[++j];
            /* what if this is already over */
            i = 0;
            while(c > ctb->codePoints[i] && i < ctb->position) {
              i++;
            }
            if(c == ctb->codePoints[i] && j<noOfDec) {
              CE = ctb->CEs[i];
            } else {
              CE = ctb->CEs[0];
              j--;
            }
            if(CE == UCOL_NOT_FOUND) {
              lastNotFound = TRUE;
              j--;
              break;
            } else if(CE > UCOL_NOT_FOUND) {
              continue;
            } else {
              result[resLen++] = CE;
              break;
            }
        }
      }

    }
    if(resLen >= resultSize) {
      *status = U_MEMORY_ALLOCATION_ERROR;
    }
    j++;
  }
  return resLen;
}
  
UCATableHeader *ucol_assembleTailoringTable(UColTokenParser *src, UErrorCode *status) {
  uint32_t i = 0;
/*
2.  Eliminate the negative lists by doing the following for each non-null negative list: 
    o   if previousCE(baseCE, strongestN) != some ListHeader X's baseCE, 
    create new ListHeader X 
    o   reverse the list, add to the end of X's positive list. Reset the strength of the 
    first item you add, based on the stronger strength levels of the two lists. 
*/
/*
3.  For each ListHeader with a non-null positive list: 
*/
/*
    o   Find all character strings with CEs between the baseCE and the 
    next/previous CE, at the strength of the first token. Add these to the 
    tailoring. 
      ? That is, if UCA has ...  x <<< X << x' <<< X' < y ..., and the 
      tailoring has & x < z... 
      ? Then we change the tailoring to & x  <<< X << x' <<< X' < z ... 
*/
  /* It is possible that this part should be done even while constructing list */
  /* The problem is that it is unknown what is going to be the strongest weight */
  /* So we might as well do it here */

/*
    o   Allocate CEs for each token in the list, based on the total number N of the 
    largest level difference, and the gap G between baseCE and nextCE at that 
    level. The relation * between the last item and nextCE is the same as the 
    strongest strength. 
    o   Example: baseCE < a << b <<< q << c < d < e * nextCE(X,1) 
      ? There are 3 primary items: a, d, e. Fit them into the primary gap. 
      Then fit b and c into the secondary gap between a and d, then fit q 
      into the tertiary gap between b and c. 

    o   Example: baseCE << b <<< q << c * nextCE(X,2) 
      ? There are 2 secondary items: b, c. Fit them into the secondary gap. 
      Then fit q into the tertiary gap between b and c. 
    o   When incrementing primary values, we will not cross high byte 
    boundaries except where there is only a single-byte primary. That is to 
    ensure that the script reordering will continue to work. 
*/
  UHashtable *tailored = uhash_open(uhash_hashLong, uhash_compareLong, status);

  for(i = 0; i<src->resultLen; i++) {
    /* now we need to generate the CEs */ 
    /* We stuff the initial value in the buffers, and increase the appropriate buffer */
    /* According to strength                                                          */
    ucol_initBuffers(&src->lh[i], tailored, status);
  }

  tempUCATable *t = uprv_uca_initTempTable(src->image, UCA, status);


  /* After this, we have assigned CE values to all regular CEs      */
  /* now we will go through list once more and resolve expansions,  */
  /* make UCAElements structs and add them to table                 */
  for(i = 0; i<src->resultLen; i++) {
    /* now we need to generate the CEs */ 
    /* We stuff the initial value in the buffers, and increase the appropriate buffer */
    /* According to strength                                                          */
    ucol_createElements(src, t, &src->lh[i], tailored, status);
  }

  UCATableHeader *myData = NULL;
  {
    UChar decomp[256];
    uint32_t noOfDec = 0, i = 0, CE = UCOL_NOT_FOUND;
    uint32_t u = 0;
    UCAElements el;
    el.isThai = FALSE;
    collIterate colIt;
    /*uint32_t decompCE[256];*/
    uint32_t compCE[256];
    uint32_t compRes = 0;

    /* produce canonical closure */
    for(u = 0; u < 0x10000; u++) {
      /*if((noOfDec = unorm_normalize((const UChar *)&u, 1, UNORM_NFD, 0, decomp, 256, status)) > 1
        || (noOfDec == 1 && *decomp != (UChar)u))*/
      if((noOfDec = uprv_ucol_decompose ((UChar)u, decomp)) > 1 || (noOfDec == 1 && *decomp != (UChar)u)) {
        compRes = ucol_getDynamicCEs(t, (UChar *)&u, 1, compCE, 256, status);
        el.noOfCEs = ucol_getDynamicCEs(t, decomp, noOfDec, el.CEs, 128, status);

        if((compRes != el.noOfCEs) || (uprv_memcmp(compCE, el.CEs, compRes*sizeof(uint32_t)) != 0)) {
            el.uchars[0] = (UChar)u;
            el.cPoints = el.uchars;
            el.cSize = 1;

            uprv_uca_addAnElement(t, &el, status);
        }
      }
    }

    /* still need to produce compatibility closure */

  /* add latin-1 stuff */
    for(u = 0; u<0x100; u++) {
      if((CE = ucmp32_get(t->mapping, u)) == UCOL_NOT_FOUND /*) {*/
        /* this test is for contractions that are missing the starting element. Looks like latin-1 should be done before assembling */
        /* the table, even if it results in more false closure elements */
        || ((isContraction(CE)) &&
        (uprv_cnttab_getCE(t->contractions, CE, 0, TRUE, status) == UCOL_NOT_FOUND))
        ) {
        decomp[0] = (UChar)u;
        el.uchars[0] = (UChar)u;
        el.cPoints = el.uchars;
        el.cSize = 1;
        el.noOfCEs = 0;
        init_collIterate(decomp, 1, &colIt, TRUE);
        while(CE != UCOL_NO_MORE_CES) {
          CE = ucol_getNextCE(UCA, &colIt, status);
          /*UCOL_GETNEXTCE(CE, temp, colIt, status);*/
          if(CE != UCOL_NO_MORE_CES) {
            el.CEs[el.noOfCEs++] = CE;
          }
        }
        uprv_uca_addAnElement(t, &el, status);
      }
    }
  }

  myData = uprv_uca_assembleTable(t, status);  

  uhash_close(tailored);
  uprv_uca_closeTempTable(t);    

  return myData;
}

U_CAPI UCollator*
ucol_openRules(    const    UChar                  *rules,
        int32_t                 rulesLength,
        UNormalizationMode      mode,
        UCollationStrength      strength,
        UErrorCode              *status)
{
  uint32_t listLen = 0;
  UColTokenParser src;

  ucol_initUCA(status);
  ucol_initInverseUCA(status);

  if(U_FAILURE(*status)) return 0;

  Normalizer::EMode normMode;
  switch(mode) {
  case UCOL_NO_NORMALIZATION:
    normMode = Normalizer::NO_OP;
    break;
  case UCOL_DECOMP_CAN:
    normMode = Normalizer::DECOMP;
    break;
  case UCOL_DECOMP_COMPAT:
    normMode = Normalizer::DECOMP_COMPAT;
    break;
  case UCOL_DECOMP_CAN_COMP_COMPAT:
    normMode = Normalizer::COMPOSE;
    break;
  case UCOL_DECOMP_COMPAT_COMP_CAN:
    normMode = Normalizer::COMPOSE_COMPAT;
    break;
  default:
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

  /* do we need to normalize the string beforehand? */

  src.source = rules;
  src.current = rules;
  src.end = rules+rulesLength;
  src.invUCA = invUCA;
  src.UCA = UCA;
  src.resultLen = 0;
  src.lh = 0;

  src.image = (UCATableHeader *)uprv_malloc(sizeof(UCATableHeader));


  uprv_memcpy(src.image, UCA->image, sizeof(UCATableHeader));

  listLen = ucol_tok_assembleTokenList(&src, status);
  if(U_FAILURE(*status)) { 
    /* if status is U_ILLEGAL_ARGUMENT_ERROR, src->current points at the offending option */
    /* if status is U_INVALID_FORMAT_ERROR, src->current points after the problematic part of the rules */
    /* so something might be done here... or on lower level */
#ifdef UCOL_DEBUG
    if(*status == U_ILLEGAL_ARGUMENT_ERROR) {
      fprintf(stderr, "bad option starting at offset %i\n", src.current-src.source);
    } else {
      fprintf(stderr, "invalid rule just before offset %i\n", src.current-src.source);
    }
#endif
    uprv_free(src.image);
    ucol_tok_closeTokenList(&src);
    return NULL;
  }
  UCollator *result = NULL;
  UCATableHeader *table = NULL;

  if(src.resultLen > 0) { /* we have a set of rules, let's make something of it */
    table = ucol_assembleTailoringTable(&src, status);
    result = ucol_initCollator(table,0,status);
    result->hasRealData = TRUE;
  } else { /* no rules, but no error either */
    /* must be only options */
    result = ucol_initCollator(UCA->image,0,status);
    ucol_setOptionsFromHeader(result, src.image, status);
    result->hasRealData = FALSE;
  }
  result->dataInfo.dataVersion[0] = UCOL_BUILDER_VERSION;
  if(U_SUCCESS(*status)) {
    result->rules = (UChar *)uprv_malloc((u_strlen(rules)+1)*sizeof(UChar));
    u_strcpy((UChar *)result->rules, rules);
    result->freeRulesOnClose = TRUE;
    result->rb = 0;
  } else {
    if(table != NULL) {
      uprv_free(table);
      ucol_close(result);
    }
    uprv_free(src.image);
    ucol_tok_closeTokenList(&src);
    return NULL;
  }

  uprv_free(src.image);
  ucol_tok_closeTokenList(&src);

  ucol_setAttribute(result, UCOL_STRENGTH, strength, status);

  return result;
}

/* This one is currently used by genrb & tests. After constructing from rules (tailoring),*/
/* you should be able to get the binary chunk to write out...  Doesn't look very full now */
U_CAPI uint8_t *
ucol_cloneRuleData(UCollator *coll, int32_t *length, UErrorCode *status)
{
  uint8_t *result = NULL;
  if(coll->hasRealData == TRUE) {
    *length = coll->image->size;
    result = (uint8_t *)uprv_malloc(*length);
    uprv_memcpy(result, coll->image, *length);
  } else {
    *length = sizeof(UCATableHeader);
    result = (uint8_t *)uprv_malloc(sizeof(UCATableHeader));
    UCATableHeader *head = (UCATableHeader *)result;
    ucol_putOptionsToHeader(coll, head, status);   
  }
  return result;
}

void ucol_setOptionsFromHeader(UCollator* result, const UCATableHeader * image, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
    result->caseFirst = image->caseFirst;
    result->caseLevel = image->caseLevel;
    result->frenchCollation = image->frenchCollation;
    result->normalizationMode = image->normalizationMode;
    result->strength = image->strength;
    result->variableTopValue = image->variableTopValue;
    result->alternateHandling = image->alternateHandling;

    result->caseFirstisDefault = TRUE;
    result->caseLevelisDefault = TRUE;
    result->frenchCollationisDefault = TRUE;
    result->normalizationModeisDefault = TRUE;
    result->strengthisDefault = TRUE;
    result->variableTopValueisDefault = TRUE;

    ucol_updateInternalState(result);
}

void ucol_putOptionsToHeader(UCollator* result, UCATableHeader * image, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
    image->caseFirst = result->caseFirst;
    image->caseLevel = result->caseLevel;
    image->frenchCollation = result->frenchCollation;
    image->normalizationMode = result->normalizationMode;
    image->strength = result->strength;
    image->variableTopValue = result->variableTopValue;
    image->alternateHandling = result->alternateHandling;
}



static void addCEtoContrHash(uint32_t ce, uint8_t *cTable, UCollator* coll)
{
    uint32_t        offset;
    UChar           c;
    const UChar     *cPtr;
    const uint32_t  *cePtr;

    offset = getContractOffset(ce);              /* Uchar (!) offset from top of data image into   */
    cPtr   = (UChar *)coll->image + offset;       /*   contraction UChars table.         */

    offset = cPtr - coll->contractionIndex;      /* Convert to an integer index that can be   */
                                                 /*  used for both the UChar and CE tables    */
    fprintf(stderr, "  Index to contraction table arrays is %x\n", offset);
    offset++;        

    /* loop through all contraction UChars in this section of the contraction table           */
    /*   add each to the hash table, and recursively do the CE for each.                      */
    cPtr = coll->contractionIndex;
    cePtr = coll->contractionCEs;

    for (c=cPtr[offset]; c != 0xffff; c=cPtr[++offset]) {
        uint32_t    i;
        uint8_t     bitInByte;
        uint32_t    ce;

        fprintf(stderr, "  adding char %x, offset = %x.  CE is %x\n", c, offset, cePtr[offset]);
        if (c < 256)                            /*  hash the UChar                            */
            i = c >> 3;
        else
            i = 32 + ((c & UCOL_UNSAFECP_TABLE_MASK) >> 3);
        bitInByte = 1 << (c & 0x7);             /*  set the bit in the hash table.            */
        //cTable[i] |= bitInByte;

        ce = cePtr[offset];                     /* recursively add the corresponding CE,      */
        if (isContraction(ce))  {               /*   if it's another contraction.             */
            fprintf(stderr, "    Recursing on the CE ...\n");
            addCEtoContrHash(ce, cTable, coll);
            fprintf(stderr, "    Return from recursion.\n");
        }
    }
} 



static void buildUnsafeCPTable(uint8_t *cTable, UCollator* coll) {
    uint32_t   ce;
    UChar32    c;
    int32_t    i;
    uint8_t    bitInByte;
    
    fprintf(stderr, "Building UnSafeCharTable... ");
    uprv_memset(cTable, 0, UCOL_UNSAFECP_TABLE_SIZE); 
    
    for (UChar cp=0; cp<0xffff; cp++) {
        ce = ucmp32_get(coll->mapping, cp);
        // ce = CEs[i];
        if (isContraction(ce)) {
            fprintf(stderr, "char, CE = %x  %x\n", cp, ce);
            addCEtoContrHash(ce, cTable, coll);
        }
    }
    
#if 0
    // Do again for main UCA table.  ???
    if (UCA) {   // But don't do when initing the global UCA itself.
        CEs    = (uint32_t *)ucmp32_getArray(UCA->mapping);
        numCEs = ucmp32_getCount(UCA->mapping);
        for (i=0; i<numCEs; i++) {
            ce = CEs[i];
            if (isContraction(ce))
                addCEtoContrHash(ce, cTable, coll);
        }
    }
#endif       
    // Add all characters with combining class != 0
    for (c=0; c<=0xffff; c++) {
        if (c >= 0xd800 && c <= 0xf7ff)
            continue;
        if (u_getCombiningClass(c) != 0) {
            if (c < 256)  
                i = c >> 3;
            else
                i = 32 + ((c & UCOL_UNSAFECP_TABLE_MASK) >> 3);
            bitInByte = 1 << (c & 0x7);      
            cTable[i] |= bitInByte;
        }
    }
    
    
    int count = 0;
    for (i=0; i<UCOL_UNSAFECP_TABLE_SIZE; i++) {
        uint8_t b = cTable[i];
        for (int bit=0; bit<8; bit++) {
            if (b & 1)
                count ++;
            b >>= 1;
        }
    }
    fprintf(stderr, "done.  %d entries.\n", count);
}



UCollator* ucol_initCollator(const UCATableHeader *image, UCollator *fillIn, UErrorCode *status) {
    UCollator *result = fillIn;
    if(U_FAILURE(*status) || image == NULL) {
        return NULL;
    }

    if(result == NULL) {
        result = (UCollator *)uprv_malloc(sizeof(UCollator));
        if(result == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return result;
        }
        result->freeOnClose = TRUE;
    } else {
        result->freeOnClose = FALSE;
    }

    result->image = image;
    const uint8_t *mapping = (uint8_t*)result->image+result->image->mappingPosition;
    CompactIntArray *newUCAmapping = ucmp32_openFromData(&mapping, status);
    if(U_SUCCESS(*status)) {
        result->mapping = newUCAmapping;
    } else {
        if(result->freeOnClose == TRUE) {
            uprv_free(result);
            result = NULL;
        }
        return result;
    }

    result->latinOneMapping = (uint32_t*)((uint8_t*)result->image+result->image->latinOneMapping);
    result->contractionCEs = (uint32_t*)((uint8_t*)result->image+result->image->contractionCEs);
    result->contractionIndex = (UChar*)((uint8_t*)result->image+result->image->contractionIndex);
    result->expansion = (uint32_t*)((uint8_t*)result->image+result->image->expansion);
    /* set attributes */
    result->caseFirst = result->image->caseFirst;
    result->caseLevel = result->image->caseLevel;
    result->frenchCollation = result->image->frenchCollation;
    result->normalizationMode = result->image->normalizationMode;
    result->strength = result->image->strength;
    result->variableTopValue = result->image->variableTopValue;
    result->alternateHandling = result->image->alternateHandling;

    result->caseFirstisDefault = TRUE;
    result->caseLevelisDefault = TRUE;
    result->frenchCollationisDefault = TRUE;
    result->normalizationModeisDefault = TRUE;
    result->strengthisDefault = TRUE;
    result->variableTopValueisDefault = TRUE;
    result->alternateHandlingisDefault = TRUE;

    uint32_t variableMaxCE = ucmp32_get(result->mapping, result->variableTopValue);
    result->variableMax1 = (uint8_t)((variableMaxCE & 0xFF000000) >> 24);
    result->variableMax2 = (uint8_t)((variableMaxCE & 0x00FF0000) >> 16);

    result->scriptOrder = NULL;

    result->zero = 0;
    result->rules = NULL;
    /* get the version info form UCATableHeader and populate the Collator struct*/
    result->dataInfo.dataVersion[0] = result->image->version[0]; /* UCA Builder version*/
    result->dataInfo.dataVersion[1] = result->image->version[1]; /* UCA Tailoring rules version*/

#if 0
    /* Build the unsafe chars hash table  */
    uint8_t *t;    /*non-const, unlike result->unsafeCP  */
    /* result->unsafeCP = */ t = (uint8_t *)uprv_malloc(UCOL_UNSAFECP_TABLE_SIZE);  
    buildUnsafeCPTable(t, result);
#endif
    result->unsafeCP = (uint8_t *)result->image + result->image->unsafeCP;

    /* max expansion tables */
    result->endExpansionCE = (uint32_t*)((uint8_t*)result->image + 
                                         result->image->endExpansionCE);
    result->lastEndExpansionCE = result->endExpansionCE + 
                                 result->image->endExpansionCECount - 1;
    result->expansionCESize = (uint8_t*)result->image + 
                                               result->image->expansionCESize;

    ucol_updateInternalState(result);
    return result;
}

void ucol_initUCA(UErrorCode *status) {
  if(U_FAILURE(*status)) return;

  if(UCA == NULL) {
    UCollator *newUCA = (UCollator *)uprv_malloc(sizeof(UCollator));
    UDataMemory *result = udata_openChoice(NULL, UCA_DATA_TYPE, UCA_DATA_NAME, isAcceptableUCA, NULL, status);

    if(U_FAILURE(*status)) {
        udata_close(result);
        uprv_free(newUCA);
    }

    if(result != NULL) { /* It looks like sometimes we can fail to find the data file */
      newUCA = ucol_initCollator((const UCATableHeader *)udata_getMemory(result), newUCA, status);
      newUCA->rb = NULL;
      umtx_lock(NULL);
      if(UCA == NULL) {
          UCA = newUCA;
          newUCA = NULL;
      }
      umtx_unlock(NULL);

      if(newUCA != NULL) {
          udata_close(result);
          uprv_free(newUCA);
      }
    }

  }
}

void ucol_initInverseUCA(UErrorCode *status) {
  if(U_FAILURE(*status)) return;

  if(invUCA == NULL) {
    InverseTableHeader *newInvUCA = NULL; /*(InverseTableHeader *)uprv_malloc(sizeof(InverseTableHeader ));*/
    UDataMemory *result = udata_openChoice(NULL, INVC_DATA_TYPE, INVC_DATA_NAME, isAcceptableInvUCA, NULL, status);

    if(U_FAILURE(*status)) {
        udata_close(result);
        uprv_free(newInvUCA);
    }

    if(result != NULL) { /* It looks like sometimes we can fail to find the data file */
      newInvUCA = (InverseTableHeader *)udata_getMemory(result);

      umtx_lock(NULL);
      if(invUCA == NULL) {
          invUCA = newInvUCA;
          newInvUCA = NULL;
      }
      umtx_unlock(NULL);

      if(newInvUCA != NULL) {
          udata_close(result);
          uprv_free(newInvUCA);
      }
    }

  }
}


/****************************************************************************/
/* Following are the CE retrieval functions                                 */
/*                                                                          */
/****************************************************************************/

/* there should be a macro version of this function in the header file */
/* This is the first function that tries to fetch a collation element  */
/* If it's not succesfull or it encounters a more difficult situation  */
/* some more sofisticated and slower functions are invoked             */
uint32_t ucol_getNextCE(const UCollator *coll, collIterate *collationSource, UErrorCode *status) {
    uint32_t order;
    if (collationSource->CEpos > collationSource->toReturn) {       /* Are there any CEs from previous expansions? */
      order = *(collationSource->toReturn++);                         /* if so, return them */
      if(collationSource->CEpos == collationSource->toReturn) {
        collationSource->CEpos = collationSource->toReturn = collationSource->CEs; 
      }
    } else if(collationSource->pos < collationSource->len) {          /* This is the real business now */
      UChar ch = *collationSource->pos++;
      if(ch <= 0xFF) {                                                 /* if it's Latin One, we'll try to fast track it */
        order = coll->latinOneMapping[ch];                            /* by looking in up in an array */
      } else {                                                        /* otherwise, */
        order = ucmp32_get(coll->mapping, ch);                        /* we'll go for slightly slower trie */
      }
      if(order >= UCOL_NOT_FOUND) {                                   /* if a CE is special */
        //*(collationSource->CEpos) = order;                            /* prepare the buffer */
        order = getSpecialCE(coll, order, collationSource, status);       /* and try to get the special CE */
        if(order == UCOL_NOT_FOUND) {   /* We couldn't find a good CE in the tailoring */
          order = ucol_getNextUCA(ch, collationSource, status);
        }
      } 
      //collationSource->pos++; /* we're advancing to the next codepoint */
    } else {
      order = UCOL_NO_MORE_CES;                                       /* if so, we won't play any more        */
    } 
    /* This means that contraction should spit back the last codepoint eaten! */
    return order; /* return the CE */
}

/* This function tries to get a CE from UCA, which should be always around  */
/* UChar is passed in in order to speed things up                           */
/* here is also the generation of implicit CEs                              */
uint32_t ucol_getNextUCA(UChar ch, collIterate *collationSource, UErrorCode *status) {
    uint32_t order;
    if(ch < 0xFF) {               /* so we'll try to find it in the UCA */
      order = UCA->latinOneMapping[ch];
    } else {
      order = ucmp32_get(UCA->mapping, ch);
    }
    if(order >= UCOL_NOT_FOUND) { /* UCA also gives us a special CE */
      order = getSpecialCE(UCA, order, collationSource, status); 
    } 
    if(order == UCOL_NOT_FOUND) { /* This is where we have to resort to algorithmical generation */
      /* We have to check if ch is possibly a first surrogate - then we need to take the next code unit */
      /* and make a bigger CE */
      UChar nextChar;
      const uint32_t 
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount;   // 11172
        //LLimit = LBase + LCount,    // 1113
        //VLimit = VBase + VCount,    // 1176
        //TLimit = TBase + TCount,    // 11C3
        //SLimit = SBase + SCount;    // D7A4

        // once we have failed to find a match for codepoint cp, and are in the implicit code.
 
        uint32_t L = ch - SBase;
        //if (ch < SLimit) { // since it is unsigned, catchs zero case too
        if (L < SCount) { // since it is unsigned, catchs zero case too

          // divide into pieces

          uint32_t T = L % TCount; // we do it in this order since some compilers can do % and / in one operation
          L /= TCount;
          uint32_t V = L % VCount;
          L /= VCount;

          // offset them

          L += LBase;
          V += VBase;
          T += TBase;

          // return the first CE, but first put the rest into the expansion buffer
          if (!collationSource->JamoSpecial) { // FAST PATH
            
            *(collationSource->CEpos++) = ucmp32_get(UCA->mapping, V);
            if (T != TBase) {
                *(collationSource->CEpos++) = ucmp32_get(UCA->mapping, T);
            }
            
            return ucmp32_get(UCA->mapping, L); // return first one

          } else { // Jamo is Special

            // do recursive processing of L, V, and T with fetchCE (but T only if not equal to TBase!!)
            // Since fetchCE returns a CE, and (potentially) stuffs items into the ce buffer,
            // this is how it is done.
/*
            int firstCE = fetchCE(L, ...);
            int* lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
            *lastExpansion = fetchCE(V,...);
            if (T != TBase) {
              lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
              *lastExpansion = fetchCE(T,...);
            }
*/
          }
      }

      if(UTF_IS_FIRST_SURROGATE(ch)) {
        if( (collationSource->pos<collationSource->len) &&
          UTF_IS_SECOND_SURROGATE((nextChar=*collationSource->pos))) {
          uint32_t cp = (((ch)<<10UL)+(nextChar)-((0xd800<<10UL)+0xdc00));
          collationSource->pos++;
          if ((cp & 0xFFFE) == 0xFFFE || (0xD800 <= cp && cp <= 0xDC00)) {
              return 0;  /* illegal code value, use completely ignoreable! */
          }
          /* This is a code point minus 0x10000, that's what algorithm requires */
          order = 0xE0010303 | (cp & 0xFFE00) << 8;

          *(collationSource->CEpos++) = 0x80200080 | (cp & 0x001FF) << 22;
        } else {
          return 0; /* completely ignorable */
        }
      } else {
        /* otherwise */
        if(UTF_IS_SECOND_SURROGATE((ch)) || (ch & 0xFFFE) == 0xFFFE) {
          return 0; /* completely ignorable */
        }
        /* Make up an artifical CE from code point as per UCA */
        order = 0xD0800303 | (ch & 0xF000) << 12 | (ch & 0x0FE0) << 11;
        *(collationSource->CEpos++) = 0x04000080 | (ch & 0x001F) << 27;
      }
    }
    return order; /* return the CE */
}

/* 
* This function tries to get a CE from UCA, which should be always around 
* UChar is passed in in order to speed things up here is also the generation 
* of implicit CEs                              
*/
uint32_t ucol_getPrevUCA(UChar ch, collIterate *collationSource, 
                         uint32_t length, UErrorCode *status) 
{
  uint32_t order;
  if (ch < 0xFF) {
    order = UCA->latinOneMapping[ch];
  }
  else {
    order = ucmp32_get(UCA->mapping, ch);
  }
  
  if (order >= UCOL_NOT_FOUND) {
    order = getSpecialPrevCE(UCA, order, collationSource, length, status); 
  }
  
  if (order == UCOL_NOT_FOUND) 
  { 
    /* 
    This is where we have to resort to algorithmical generation.
    We have to check if ch is possibly a first surrogate - then we need to 
    take the next code unit and make a bigger CE 
    */
    UChar prevChar;
    uint32_t
      SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
      LCount = 19, VCount = 21, TCount = 28,
      NCount = VCount * TCount,   /* 588 */
      SCount = LCount * NCount;   /* 11172 */
      /* 
      LLimit = LBase + LCount,    // 1113
      VLimit = VBase + VCount,    // 1176
      TLimit = TBase + TCount,    // 11C3
      SLimit = SBase + SCount;    // D7A4
      */

    /* 
    once we have failed to find a match for codepoint cp, and are in the 
    implicit code.
    */
 
    uint32_t L = ch - SBase;
    if (L < SCount) 
    { /* since it is unsigned, catchs zero case too */

      /* 
      divide into pieces.
      we do it in this order since some compilers can do % and / in one 
      operation
      */
      uint32_t T = L % TCount; 
      L /= TCount;
      uint32_t V = L % VCount;
      L /= VCount;

      /* offset them */
      L += LBase;
      V += VBase;
      T += TBase;

      /* 
      return the first CE, but first put the rest into the expansion buffer
      */
      if (!collationSource->JamoSpecial) 
      { 
        *(collationSource->CEpos ++) = ucmp32_get(UCA->mapping, L); 
        *(collationSource->CEpos ++) = ucmp32_get(UCA->mapping, V);
        if (T != TBase)
          *(collationSource->CEpos ++) = ucmp32_get(UCA->mapping, T);
        
        collationSource->toReturn = collationSource->CEpos - 1;
        return *(collationSource->toReturn);
      } else { 
        /* 
        synwee :TODO 
        Heh heh.... waiting for vladimir's code, me cut and paste
        Jamo is Special
        do recursive processing of L, V, and T with fetchCE (but T only if not 
        equal to TBase!!)
        Since fetchCE returns a CE, and (potentially) stuffs items into the ce 
        buffer,
        this is how it is done.
        */
        /*
          int firstCE = fetchCE(L, ...);
          // set pointer, leave gap!
          int* lastExpansion = expansionBufferEnd++; 
          *lastExpansion = fetchCE(V,...);
          if (T != TBase) {
            lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
            *lastExpansion = fetchCE(T,...);
          }
        */
        }
    }

    if (UTF_IS_SECOND_SURROGATE(ch)) 
    {
      UChar *temp = collationSource->pos;
      if (((collationSource->string < temp) ||
          (collationSource->writableBuffer < temp)) &&
          (UTF_IS_FIRST_SURROGATE(prevChar = *(collationSource->pos - 1)))) 
      {
        uint32_t cp = ((prevChar << 10UL) + ch - ((0xd800 << 10UL) + 0xdc00));
        collationSource->pos --;
        if ((cp & 0xFFFE) == 0xFFFE || (0xD800 <= cp && cp <= 0xDC00)) {
          return 0;  /* illegal code value, use completely ignoreable! */
        }
        
        /* 
        This is a code point minus 0x10000, that's what algorithm requires 
        */
        *(collationSource->CEpos ++) = 0xE0010303 | (cp & 0xFFE00) << 8;
        order = 0x80200080 | (cp & 0x001FF) << 22;
        collationSource->toReturn = collationSource->CEpos;
        *(collationSource->CEpos ++) = order;
      } 
      else {
        return 0; /* completely ignorable */
      }
    } 
    else 
    {
      /* otherwise */
      if (UTF_IS_FIRST_SURROGATE(ch) || (ch & 0xFFFE) == 0xFFFE) {
        return 0; /* completely ignorable */
      }
      
      /* Make up an artifical CE from code point as per UCA */
      *(collationSource->CEpos ++) = 0xD0800303 | (ch & 0xF000) << 12 | 
                                     (ch & 0x0FE0) << 11;
      collationSource->toReturn = collationSource->CEpos;
      order = 0x04000080 | (ch & 0x001F) << 27;
      *(collationSource->CEpos ++) = order;
    }
  }
  return order; /* return the CE */
}

/* This function handles the special CEs like contractions, expansions, surrogates, Thai */
/* It is called by both getNextCE and getNextUCA                                         */
uint32_t getSpecialCE(const UCollator *coll, uint32_t CE, collIterate *source, UErrorCode *status) {
  uint32_t i = 0; /* general counter */
  //uint32_t CE = *source->CEpos;
  for (;;) {
    const uint32_t *CEOffset = NULL;
    const UChar *UCharOffset = NULL;
    UChar schar, tchar;
    uint32_t size = 0;
    switch(getCETag(CE)) {
    case NOT_FOUND_TAG:
      /* This one is not found, and we'll let somebody else bother about it... no more games */
      return CE;
    case SURROGATE_TAG:
      /* pending surrogate discussion with Markus and Mark */
      return UCOL_NOT_FOUND;
    case THAI_TAG:
      /* Thai/Lao reordering */
      if(source->isThai == TRUE) { /* if we encountered Thai prevowel & the string is not yet touched */
        source->isThai = FALSE;    /* We will touch the string */
        --source->pos;
        if((source->len - source->pos) > UCOL_WRITABLE_BUFFER_SIZE) {
            /* Problematic part - if the stack buffer is too small, we need to allocate */
            /* However, somebody needs to keep track of that allocated space */
            /* And context structure is not good for that */
            /* allocate a new buffer - This is unfortunate and should be way smarter */
            /*source->writableBuffer = (UChar *)ucol_getABuffer(coll, (source->len - source->pos)*sizeof(UChar));*/
        } 
        UChar *sourceCopy = source->pos;
        UChar *targetCopy = source->writableBuffer;
        while(sourceCopy < source->len) {
            if(UCOL_ISTHAIPREVOWEL(*(sourceCopy)) &&      /* This is the combination that needs to be swapped */
                UCOL_ISTHAIBASECONSONANT(*(sourceCopy+1))) {
                *(targetCopy) = *(sourceCopy+1);
                *(targetCopy+1) = *(sourceCopy);
                targetCopy+=2;
                sourceCopy+=2;
            } else {
                *(targetCopy++) = *(sourceCopy++);
            }
        }
        source->pos = source->writableBuffer;
        source->len = targetCopy;
        source->CEpos = source->toReturn = source->CEs;
        CE = UCOL_IGNORABLE;
      } else { /* we have already played with the string, so treat Thai as a length one expansion */
        CEOffset = (uint32_t *)coll->image+getExpansionOffset(CE); /* find the offset to expansion table */
        CE = *CEOffset++;
      }
      break;
    case CONTRACTION_TAG:
      /* This should handle contractions */
      for (;;) {
        /* First we position ourselves at the begining of contraction sequence */
        const UChar *ContractionStart = UCharOffset = (UChar *)coll->image+getContractOffset(CE);

        if (source->pos>=source->len) { /* this is the end of string */
          CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex)); /* So we'll pick whatever we have at the point... */
          break;
        }

        /* we need to convey the notion of having a backward search - most probably through the context object */
        /* if (backwardsSearch) offset += contractionUChars[(int16_t)offset]; else UCharOffset++;  */
        UCharOffset++; /* skip the backward offset, see above */


        schar = *source->pos++;
        while(schar > (tchar = *UCharOffset)) { /* since the contraction codepoints should be ordered, we skip all that are smaller */
          UCharOffset++;
        }
        if(schar != tchar) { /* we didn't find the correct codepoint. We can use either the first or the last CE */
          if(tchar != 0xFFFF) {
            UCharOffset = ContractionStart; /* We're not at the end, bailed out in the middle. Better use starting CE */
          }
          source->pos--; /* Spit out the last char of the string, wasn't tasty enough */
        } 
        CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex));
        if(!isContraction(CE)) {
          break;  
        }
      }
      break;
    case EXPANSION_TAG:
      /* This should handle expansion. */
      /* NOTE: we can encounter both continuations and expansions in an expansion! */
      /* I have to decide where continuations are going to be dealt with */
      CEOffset = (uint32_t *)coll->image+getExpansionOffset(CE); /* find the offset to expansion table */
      size = getExpansionCount(CE);
      CE = *CEOffset++;
      if(size != 0) { /* if there are less than 16 elements in expansion, we don't terminate */
        for(i = 1; i<size; i++) {
          *(source->CEpos++) = *CEOffset++;
        }
      } else { /* else, we do */
        while(*CEOffset != 0) {
          *(source->CEpos++) = *CEOffset++;
        }
      }
      return CE;
    case CHARSET_TAG:
      /* probably after 1.8 */
      return UCOL_NOT_FOUND;
    default:
      *status = U_INTERNAL_PROGRAM_ERROR;
      CE=0;
      break;
    }
    if (CE <= UCOL_NOT_FOUND) break;
  }
  return CE;
}

/** 
* This function handles the special CEs like contractions, expansions, 
* surrogates, Thai.
* It is called by both getPrevCE and getPrevUCA                        
*/
uint32_t getSpecialPrevCE(const UCollator *coll, uint32_t CE, 
                          collIterate *source, uint32_t length, 
                          UErrorCode *status) 
{
        uint32_t count        = 0;
  const uint32_t *CEOffset    = NULL;
  const UChar    *UCharOffset = NULL;
        UChar    schar, 
                 tchar;
  const UChar    *strend      = NULL;
  const UChar    *constart    = NULL;
        uint32_t size;
  for(;;)
  {
    switch (getCETag(CE)) 
    {
    case NOT_FOUND_TAG:
      return CE;
    case SURROGATE_TAG:
      /* pending surrogate discussion with Markus and Mark */
      return UCOL_NOT_FOUND;
    case THAI_TAG:
      if (source->isThai == TRUE) 
      { /* if we encountered Thai prevowel & the string is not yet touched */
        source->isThai = FALSE;
        strend =  source->pos;
        size = strend - source->string;
        if (size > UCOL_WRITABLE_BUFFER_SIZE) 
        {
          /*
          someone else has already allocated something
          */
          if (source->writableBuffer != source->stackWritableBuffer) {
            uprv_free(source->writableBuffer);
          }
          source->writableBuffer = 
            (UChar *)uprv_malloc(size * sizeof(UChar));
          source->isThai = FALSE;
        } 
        UChar *sourceCopy = source->string;
        UChar *targetCopy = source->writableBuffer;
        while (sourceCopy <= strend) {
            if (UCOL_ISTHAIPREVOWEL(*sourceCopy) &&      
            /* This is the combination that needs to be swapped */
                UCOL_ISTHAIBASECONSONANT(*(sourceCopy + 1))) {
                *(targetCopy)     = *(sourceCopy + 1);
                *(targetCopy + 1) = *(sourceCopy);
                targetCopy += 2;
                sourceCopy += 2;
            } 
            else {
                *(targetCopy ++) = *(sourceCopy ++);
            }
        }
        source->pos   = targetCopy;
        source->len   = targetCopy;
        source->CEpos = source->toReturn = source->CEs;
        CE = UCOL_IGNORABLE;
      } 
      else 
      { 
        /* 
        we have already played with the string, so treat Thai as a length one 
        expansion 
        */
        /* find the offset to expansion table */
        CEOffset = (uint32_t *)coll->image + getExpansionOffset(CE); 
        CE = *CEOffset ++;
      }
      break;
    case CONTRACTION_TAG:
      /* This should handle contractions */
      for(;;)
      {
        /* 
        First we position ourselves at the begining of contraction sequence 
        */
        constart = UCharOffset = (UChar *)coll->image + getContractOffset(CE);
        strend = source->len;

        if ((uint32_t)(strend - source->pos) == length) { 
          /* this is the start of string */
          CE = *(coll->contractionCEs + 
                 (UCharOffset - coll->contractionIndex)); 
          break;
        }

        /*
        Progressing to backwards block
        */
        UCharOffset += *UCharOffset; 

        schar = *(source->pos - 1);
        while (schar > (tchar = *UCharOffset)) {
          UCharOffset ++;
        }
        
        if (schar != tchar) {
          /* 
          we didn't find the correct codepoint. We can use either the first or 
          the last CE 
          */
          /* testing if (tchar != 0xFFFF) */
          UCharOffset = constart; 
        } 
        else {
          /* Move up one character */
          source->pos --;
        }
        CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex));
        if (!isContraction(CE)) {
          break;  
        }
      }
      break;
    case EXPANSION_TAG:
      /* 
      This should handle expansion.
      NOTE: we can encounter both continuations and expansions in an expansion! 
      I have to decide where continuations are going to be dealt with 
      */
      /* find the offset to expansion table */
      CEOffset = (uint32_t *)coll->image + getExpansionOffset(CE); 
      size     = getExpansionCount(CE);
      if (size != 0) {
        /* 
        if there are less than 16 elements in expansion, we don't terminate 
        */
        for (count = 0; count < size; count++) {
          *(source->CEpos ++) = *CEOffset++;
        }
      }
      else {
        /* else, we do */
        while (*CEOffset != 0) {
          *(source->CEpos ++) = *CEOffset ++;
        }
      }
      source->toReturn = source->CEpos - 1;
      return *(source->toReturn);
    case CHARSET_TAG:
      /* probably after 1.8 */
      return UCOL_NOT_FOUND;
    default:
      *status = U_INTERNAL_PROGRAM_ERROR;
      CE=0;
      break;
    }
    if (CE <= UCOL_NOT_FOUND) {
      break;
    }
  }
  return CE;
}

/* This should really be a macro        */
/* However, it is used only when stack buffers are not sufficiently big, and then we're messed up performance wise */
/* anyway */
uint8_t *reallocateBuffer(uint8_t **secondaries, uint8_t *secStart, uint8_t *second, uint32_t *secSize, UErrorCode *status) {
  uint8_t *newStart = NULL;

  if(secStart==second) {
    newStart=(uint8_t*)uprv_malloc(*secSize*2);
    if(newStart==NULL) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
    uprv_memcpy(newStart, secStart, *secondaries-secStart);
  } else {
    newStart=(uint8_t*)uprv_realloc(secStart, *secSize*2);
    if(newStart==NULL) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  }
  *secondaries=newStart+(*secondaries-secStart);
  *secSize*=2;
  return newStart;
}


/* This should really be a macro                                                                      */
/* This function is used to reverse parts of a buffer. We need this operation when doing continuation */
/* secondaries in French                                                                              */
/*
void uprv_ucol_reverse_buffer(uint8_t *start, uint8_t *end) {
  uint8_t temp;
  while(start<end) {
    temp = *start;
    *start++ = *end;
    *end-- = temp;
  }
}
*/

#define uprv_ucol_reverse_buffer(TYPE, start, end) { \
  TYPE tempA; \
while((start)<(end)) { \
    tempA = *(start); \
    *(start)++ = *(end); \
    *(end)-- = tempA; \
} \
} 

/****************************************************************************/
/* Following are the sortkey generation functions                           */
/*                                                                          */
/****************************************************************************/

#define MIN_VALUE 0x02
#define UCOL_VARIABLE_MAX 0x20
#define UCOL_NEW_IGNORABLE 0

/* sortkey API */
U_CAPI int32_t
ucol_getSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength)
{
  UErrorCode status = U_ZERO_ERROR;
    return ucol_calcSortKey(coll, source, sourceLength, &result, resultLength, FALSE, &status);
    /*return ucol_calcSortKeySimpleTertiary(coll, source, sourceLength, &result, resultLength, FALSE, &status);*/
}

/* this function is called by the C++ API for sortkey generation */
U_CFUNC uint8_t *ucol_getSortKeyWithAllocation(const UCollator *coll, 
        const    UChar        *source,
        int32_t            sourceLength,
        int32_t *resultLen) {
    uint8_t *result = NULL;
    UErrorCode status = U_ZERO_ERROR;
    *resultLen = ucol_calcSortKey(coll, source, sourceLength, &result, 0, TRUE, &status);
    return result;
}


/* This function tries to get the size of a sortkey. It will be invoked if the size of resulting buffer is 0  */
/* or if we run out of space while making a sortkey and want to return ASAP                                   */
int32_t ucol_getSortKeySize(const UCollator *coll, collIterate *s, int32_t currentSize, UColAttributeValue strength, int32_t len) {
    UErrorCode status = U_ZERO_ERROR;
    uint8_t compareSec   = (uint8_t)((strength >= UCOL_SECONDARY)?0:0xFF);
    uint8_t compareTer   = (uint8_t)((strength >= UCOL_TERTIARY)?0:0xFF);
    uint8_t compareQuad  = (uint8_t)((strength >= UCOL_QUATERNARY)?0:0xFF);
    UBool  compareIdent = (strength == UCOL_IDENTICAL);
    UBool  doCase = (coll->caseLevel == UCOL_ON);
    UBool  shifted = (coll->alternateHandling == UCOL_SHIFTED) && (compareQuad == 0);
    UBool  isFrenchSec = (coll->frenchCollation == UCOL_ON) && (compareSec == 0);

    uint8_t variableMax1 = coll->variableMax1;
    uint8_t variableMax2 = coll->variableMax2;
    uint8_t UCOL_COMMON_BOT4 = (uint8_t)(variableMax1+1);
    uint8_t UCOL_BOT_COUNT4 = (uint8_t)(0xFF - UCOL_COMMON_BOT4);

    int32_t order = UCOL_NO_MORE_CES;
    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
    uint32_t ce = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    int32_t caseShift = 0;
    uint32_t c2 = 0, c3 = 0, c4 = 0; /* variables for compression */

    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;

    UBool wasShifted = FALSE;
    UBool notIsContinuation = FALSE;
    

    for(;;) {
          order = ucol_getNextCE(coll, s, &status);
          /*UCOL_GETNEXTCE(order, coll, *s, &status);*/
          
          if(isCEIgnorable(order)) {
            continue;
          }

          if(order == UCOL_NO_MORE_CES) {
              break;
          }

          /* We're saving order in ce, since we will destroy order in order to get primary, secondary, tertiary in order ;)*/
          ce = order;
          notIsContinuation = !isContinuation(ce);


          order ^= caseSwitch;
          tertiary = (uint8_t)((order & tertiaryMask)); 
          secondary = (uint8_t)((order >>= 8) & 0xFF);
          primary2 = (uint8_t)((order >>= 8) & 0xFF);
          primary1 = (uint8_t)(order >>= 8);


          if(shifted && ((notIsContinuation && primary1 <= variableMax1 && primary1 > 0 
            && (primary1 < variableMax1 || primary1 == variableMax1 && primary2 < variableMax2)) 
            || (!notIsContinuation && wasShifted))) { 
            if(c4 > 0) {
              currentSize += (c2/UCOL_BOT_COUNT4)+1;
              c4 = 0;
            }
            currentSize++;
            if(primary2 != 0) {
              currentSize++;
            }
            wasShifted = TRUE;
          } else {
            wasShifted = FALSE;
            /* Note: This code assumes that the table is well built i.e. not having 0 bytes where they are not supposed to be. */
            /* Usually, we'll have non-zero primary1 & primary2, except in cases of LatinOne and friends, when primary2 will   */
            /* be zero with non zero primary1. primary3 is different than 0 only for long primaries - see above.               */
            if(primary1 != UCOL_NEW_IGNORABLE) {
              currentSize++;
              if(primary2 != UCOL_NEW_IGNORABLE) {
                currentSize++;
              }
            }               

            if(secondary > compareSec) { /* I think that != 0 test should be != IGNORABLE */
              if(!isFrenchSec){
                if (secondary == UCOL_COMMON2 && notIsContinuation) {
                  c2++;
                } else {
                  if(c2 > 0) {
                    if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
                      currentSize += (c2/UCOL_TOP_COUNT2)+1;
                    } else {
                      currentSize += (c2/UCOL_BOT_COUNT2)+1;
                    }
                    c2 = 0;
                  }
                  currentSize++;
                }
              } else {
                currentSize++;
              }
            }

            if(doCase) {
              if (caseShift  == 0) {
                currentSize++;
                caseShift = 7;
              }
              if(tertiary > 0) {
                caseShift--;
              }
            }

            if(tertiary > compareTer) { /* I think that != 0 test should be != IGNORABLE */
              if (tertiary == UCOL_COMMON3 && notIsContinuation) {
                c3++;
              } else {
                if(c3 > 0) {
                  if (tertiary > UCOL_COMMON3) { // not necessary for 4th level.
                    currentSize += (c3/UCOL_TOP_COUNT3)+1;
                  } else {
                    currentSize += (c3/UCOL_BOT_COUNT3)+1;
                  }
                  c3 = 0;
                }
                currentSize++;
              }
            }

            if(shifted  && notIsContinuation) {
              c4++;
            }

          }
    }

    if(c2 > 0) {
      currentSize += (c2/UCOL_BOT_COUNT2)+1;
    }

    if(c3 > 0) {
      currentSize += (c3/UCOL_BOT_COUNT3)+1;
    }

    if(c4 > 0) {
      currentSize += (c4/UCOL_BOT_COUNT4)+1;
    }

    if(compareIdent) {
        currentSize += len*sizeof(UChar);
        UChar *ident = s->string;
        while(ident<s->len) {
            if((*(ident) >> 8) + utf16fixup[*(ident) >> 11]<0x02) {

                currentSize++;
            }
            if((*(ident) & 0xFF)<0x02) {
                currentSize++;
            }
            ident++;
        }

    }

    return currentSize;
    
}
    
/* This is the sortkey work horse function */
int32_t
ucol_calcSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        uint32_t        resultLength,
        UBool allocatePrimary,
        UErrorCode *status)
{
    uint32_t i = 0; /* general purpose counter */

    /* Stack allocated buffers for buffers we use */
    uint8_t second[UCOL_MAX_BUFFER], tert[UCOL_MAX_BUFFER], caseB[UCOL_MAX_BUFFER], quad[UCOL_MAX_BUFFER];

    uint8_t *primaries = *result, *secondaries = second, *tertiaries = tert, *cases = caseB, *quads = quad;

    if(U_FAILURE(*status)) {
      return 0;
    }

    if(primaries == NULL && allocatePrimary == TRUE) {
        primaries = *result = (uint8_t *)uprv_malloc(2*UCOL_MAX_BUFFER);
        resultLength = 2*UCOL_MAX_BUFFER;
    }
    uint8_t *primarySafeEnd = primaries + resultLength - 2;

    uint32_t secSize = UCOL_MAX_BUFFER, terSize = UCOL_MAX_BUFFER, 
      caseSize = UCOL_MAX_BUFFER, quadSize = UCOL_MAX_BUFFER;

    uint32_t sortKeySize = 1; /* it is always \0 terminated */

    UChar normBuffer[UCOL_NORMALIZATION_GROWTH*UCOL_MAX_BUFFER];
    UChar *normSource = normBuffer;
    int32_t normSourceLen = UCOL_NORMALIZATION_GROWTH*UCOL_MAX_BUFFER;

    int32_t len = (sourceLength == -1 ? u_strlen(source) : sourceLength);

    uint8_t variableMax1 = coll->variableMax1;
    uint8_t variableMax2 = coll->variableMax2;
    uint8_t UCOL_COMMON_BOT4 = (uint8_t)(variableMax1+1);
    uint8_t UCOL_BOT_COUNT4 = (uint8_t)(0xFF - UCOL_COMMON_BOT4);

    UColAttributeValue strength = coll->strength;

    uint8_t compareSec   = (uint8_t)((strength >= UCOL_SECONDARY)?0:0xFF);
    uint8_t compareTer   = (uint8_t)((strength >= UCOL_TERTIARY)?0:0xFF);
    uint8_t compareQuad  = (uint8_t)((strength >= UCOL_QUATERNARY)?0:0xFF);
    UBool  compareIdent = (strength == UCOL_IDENTICAL);
    UBool  doCase = (coll->caseLevel == UCOL_ON);
    UBool  isFrenchSec = (coll->frenchCollation == UCOL_ON) && (compareSec == 0);
    UBool  upperFirst = (coll->caseFirst == UCOL_UPPER_FIRST) && (compareTer == 0);
    UBool  shifted = (coll->alternateHandling == UCOL_SHIFTED) && (compareQuad == 0);
    const uint8_t *scriptOrder = coll->scriptOrder;

    /* support for special features like caselevel and funky secondaries */
    uint8_t *frenchStartPtr = NULL;
    uint8_t *frenchEndPtr = NULL;
    uint32_t caseShift = 0;

    sortKeySize += ((compareSec?0:1) + (compareTer?0:1) + (doCase?1:0) + (compareQuad?0:1) + (compareIdent?1:0));

    collIterate s;
    init_collIterate((UChar *)source, len, &s, FALSE);

    /* If we need to normalize, we'll do it all at once at the beggining! */
    UColAttributeValue normMode = coll->normalizationMode;
    if((normMode != UCOL_OFF)
      /* changed by synwee */
      && !checkFCD(source, len, status))
    {
      /*fprintf(stderr, ".");*/
        normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, normSourceLen, status);
        if(U_FAILURE(*status)) {
            *status=U_ZERO_ERROR;
            normSource = (UChar *) uprv_malloc((normSourceLen+1)*sizeof(UChar));
            normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, (normSourceLen+1), status);
        }
        normSource[normSourceLen] = 0;
        s.string = normSource;
        s.pos = normSource;
        s.len = normSource+normSourceLen;
    }

    len = s.len-s.pos;

    if(resultLength == 0) {
        return ucol_getSortKeySize(coll, &s, sortKeySize, strength, len);
    }

    uint32_t minBufferSize = UCOL_MAX_BUFFER;

    uint8_t *primStart = primaries;
    uint8_t *secStart = secondaries;
    uint8_t *terStart = tertiaries;
    uint8_t *caseStart = cases;
    uint8_t *quadStart = quads;

    uint32_t order = 0;
    uint32_t ce = 0;

    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;
    UBool caseBit = FALSE;

    UBool finished = FALSE;
    UBool resultOverflow = FALSE;
    UBool wasShifted = FALSE;
    UBool notIsContinuation = FALSE;

    uint32_t prevBuffSize = 0;

    uint32_t count2 = 0, count3 = 0, count4 = 0;

    for(;;) {
        for(i=prevBuffSize; i<minBufferSize; ++i) {

            order = ucol_getNextCE(coll, &s, status);
            /*UCOL_GETNEXTCE(order, coll, s, status);*/

            if(isCEIgnorable(order)) {
              continue;
            }

            if(order == UCOL_NO_MORE_CES) {
                finished = TRUE;
                break;
            }

            /* We're saving order in ce, since we will destroy order in order to get primary, secondary, tertiary in order ;)*/
            ce = order;
            notIsContinuation = !isContinuation(ce);


            order ^= caseSwitch;
            caseBit = (order & UCOL_CASE_BIT_MASK);
            tertiary = (uint8_t)((order & tertiaryMask)); 
            secondary = (uint8_t)((order >>= 8) & 0xFF);
            primary2 = (uint8_t)((order >>= 8) & 0xFF);
            primary1 = (uint8_t)(order >>= 8);

            if(notIsContinuation) {
              if(scriptOrder != NULL) {
                primary1 = scriptOrder[primary1];
              }
            }


            /* In the code below, every increase in any of buffers is followed by the increase to  */
            /* sortKeySize - this might look tedious, but it is needed so that we can find out if  */
            /* we're using too much space and need to reallocate the primary buffer or easily bail */
            /* out to ucol_getSortKeySizeNew.                                                      */

            if(shifted && ((notIsContinuation && primary1 <= variableMax1 && primary1 > 0 
              && (primary1 < variableMax1 || primary1 == variableMax1 && primary2 < variableMax2)) 
              || (!notIsContinuation && wasShifted))) { 
              if(count4 > 0) {
                while (count4 >= UCOL_BOT_COUNT4) {
                  *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + UCOL_BOT_COUNT4);
                  count4 -= UCOL_BOT_COUNT4;
                }
                *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + count4);
                count4 = 0;
              }
              /* We are dealing with a variable and we're treating them as shifted */
              /* This is a shifted ignorable */
              if(primary1 != 0) {
                *quads++ = primary1;
              }
              if(primary2 != 0) {
                *quads++ = primary2;
              }
              wasShifted = TRUE;
            } else {
              wasShifted = FALSE;
              /* Note: This code assumes that the table is well built i.e. not having 0 bytes where they are not supposed to be. */
              /* Usually, we'll have non-zero primary1 & primary2, except in cases of LatinOne and friends, when primary2 will   */
              /* be zero with non zero primary1. primary3 is different than 0 only for long primaries - see above.               */
              if(primary1 != UCOL_NEW_IGNORABLE) {
                *primaries++ = primary1; /* scriptOrder[primary1]; */ /* This is the script ordering thingie */
                if(primary2 != UCOL_NEW_IGNORABLE) {
                  *primaries++ = primary2; /* second part */
                }
              }               

            if(secondary > compareSec) { 
              if(!isFrenchSec) {
                /* This is compression code. */
                if (secondary == UCOL_COMMON2 && notIsContinuation) {
                  ++count2;
                } else {
                  if (count2 > 0) {
                    if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
                      while (count2 >= UCOL_TOP_COUNT2) {
                        *secondaries++ = UCOL_COMMON_TOP2 - UCOL_TOP_COUNT2;
                        count2 -= UCOL_TOP_COUNT2;
                      }
                      *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - count2);
                    } else {
                      while (count2 >= UCOL_BOT_COUNT2) {
                        *secondaries++ = UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2;
                        count2 -= UCOL_BOT_COUNT2;
                      }
                      *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + count2);
                    }
                    count2 = 0;
                  }
                  *secondaries++ = secondary;
                }
              } else {
                  *secondaries++ = secondary;
                  /* Do the special handling for French secondaries */
                  /* We need to get continuation elements and do intermediate restore */
                  /* abc1c2c3de with french secondaries need to be edc1c2c3ba NOT edc3c2c1ba */
                  if(!notIsContinuation) {
                    if (frenchStartPtr == NULL) {
                      frenchStartPtr = secondaries - 2;
                    }
                    frenchEndPtr = secondaries-1;
                  } else if (frenchStartPtr != NULL) {
                      /* reverse secondaries from frenchStartPtr up to frenchEndPtr */
                    uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
                    frenchStartPtr = NULL;
                  }
                }            
              }

              if(doCase) {
                if (caseShift  == 0) {
                  *cases++ = 0x80;
                  caseShift = 7;
                }
                if(tertiary != 0) {
                  *(cases-1) |= (caseBit!=0) << (caseShift--);
                } else {
                  caseShift--;
                }
              }

              if(tertiary > compareTer) { 
                /* This is compression code. */
                /* sequence size check is included in the if clause */
                if (tertiary == UCOL_COMMON3 && notIsContinuation) {
                  ++count3;
                } else {
                  if(tertiary > UCOL_COMMON3) {
                    tertiary |= UCOL_FLAG_BIT_MASK;
                  }
                  if (count3 > 0) {
                    if (tertiary > UCOL_COMMON3) {
                      while (count3 >= UCOL_TOP_COUNT3) {
                        *tertiaries++ = UCOL_COMMON_TOP3 - UCOL_TOP_COUNT3;
                        count3 -= UCOL_TOP_COUNT3;
                      }
                      *tertiaries++ = (uint8_t)(UCOL_COMMON_TOP3 - count3);
                    } else {
                      while (count3 >= UCOL_BOT_COUNT3) {
                        *tertiaries++ = UCOL_COMMON_BOT3 + UCOL_BOT_COUNT3;
                        count3 -= UCOL_BOT_COUNT3;
                      }
                      *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + count3);
                    }
                    count3 = 0;
                  }
                  *tertiaries++ = tertiary;
                }
              }

              if(shifted && notIsContinuation) {
                count4++;
              }
            }

            if(primaries > primarySafeEnd) { /* We have stepped over the primary buffer */
              int32_t sks = sortKeySize+(primaries - primStart)+(secondaries - secStart)+(tertiaries - terStart)+(cases-caseStart)+(quads-quadStart);
              if(allocatePrimary == FALSE) { /* need to save our butts if we cannot reallocate */
                resultOverflow = TRUE;
                sortKeySize = ucol_getSortKeySize(coll, &s, sks, strength, len);
                *status = U_MEMORY_ALLOCATION_ERROR;
                finished = TRUE;
                break;
              } else { /* It's much nicer if we can actually reallocate */
                uint8_t *newStart;
                newStart = (uint8_t *)uprv_realloc(primStart, 2*sks);
                if(primStart == NULL) {
                  *status = U_MEMORY_ALLOCATION_ERROR;
                  finished = TRUE;
                  break;
                }
                primaries=newStart+(primaries-primStart);
                resultLength = 2*sks;
                primStart = *result = newStart;
                primarySafeEnd = primStart + resultLength - 2;
              }
            }
        }
        if(finished) {
            break;
        } else {
          prevBuffSize = minBufferSize;
          secStart = reallocateBuffer(&secondaries, secStart, second, &secSize, status);
          terStart = reallocateBuffer(&tertiaries, terStart, tert, &terSize, status);
          caseStart = reallocateBuffer(&cases, caseStart, cases, &caseSize, status);
          quadStart = reallocateBuffer(&quads, quadStart, quads, &quadSize, status);
          minBufferSize *= 2;
        }
    }


    if(U_SUCCESS(*status)) {
      sortKeySize += (primaries - primStart);
      /* we have done all the CE's, now let's put them together to form a key */
      if(compareSec == 0) {
        if (count2 > 0) {
          while (count2 >= UCOL_BOT_COUNT2) {
            *secondaries++ = UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2;
            count2 -= UCOL_BOT_COUNT2;
          }
          *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + count2);
        }
        uint32_t secsize = secondaries-secStart;
        sortKeySize += secsize;
        *(primaries++) = UCOL_LEVELTERMINATOR;
        if(isFrenchSec) { /* do the reverse copy */
          /* If there are any unresolved continuation secondaries, reverse them here so that we can reverse the whole secondary thing */
          if(frenchStartPtr != NULL) { 
            uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
          }           
          /* Need overflow test here */
          for(i = 0; i<secsize; i++) {
              *(primaries++) = *(secondaries-i-1);
          }
        } else { 
          /* Need overflow test here */
          uprv_memcpy(primaries, secStart, secsize); 
          primaries += secsize;
        }

      }

      if(doCase) {
        *(primaries++) = UCOL_LEVELTERMINATOR;
        uint32_t casesize = cases - caseStart;
        sortKeySize += casesize;
        /* Need overflow test here */
        uprv_memcpy(primaries, caseStart, casesize);
        primaries += casesize;
      }

      if(compareTer == 0) {
        if (count3 > 0) {
          while (count3 >= UCOL_BOT_COUNT3) {
            *tertiaries++ = UCOL_COMMON_BOT3 + UCOL_BOT_COUNT3;
            count3 -= UCOL_BOT_COUNT3;
          }
          *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + count3);
        }
        *(primaries++) = UCOL_LEVELTERMINATOR;
        uint32_t tersize = tertiaries - terStart;
        sortKeySize += tersize;
        /* Need overflow test here */
        uprv_memcpy(primaries, terStart, tersize);
        primaries += tersize;
        if(compareQuad == 0) {
            if(count4 > 0) {
              while (count4 >= UCOL_BOT_COUNT4) {
                *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + UCOL_BOT_COUNT4);
                count4 -= UCOL_BOT_COUNT4;
              }
              *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + count4);
            }
            *(primaries++) = UCOL_LEVELTERMINATOR;
            uint32_t quadsize = quads - quadStart;
            sortKeySize += quadsize;
            /* Need overflow test here */
            uprv_memcpy(primaries, quadStart, quadsize);
            primaries += quadsize;
        }

        if(compareIdent) {
            UChar *ident = s.string;
/*          const UChar *ident = source;*/
            uint8_t idByte = 0;
            sortKeySize += len * sizeof(UChar);
            *(primaries++) = UCOL_LEVELTERMINATOR;
            if(sortKeySize <= resultLength) {
                while(ident < s.len) {
                    idByte = (uint8_t)((*(ident) >> 8) + utf16fixup[*(ident) >> 11]);
                    if(idByte < 0x02) {
                        if(sortKeySize < resultLength) {
                            *(primaries++) = 0x01;
                            sortKeySize++;
                            *(primaries++) = (uint8_t)(idByte + 1);
                        }
                    } else {
                        *(primaries++) = idByte;
                    }
                    idByte = (uint8_t)((*(ident) & 0xFF));
                    if(idByte < 0x02) {
                        if(sortKeySize < resultLength) {
                            *(primaries++) = 0x01;
                            sortKeySize++;
                            *(primaries++) = (uint8_t)(idByte + 1);
                        }
                    } else {
                        *(primaries++) = idByte;
                    }

                  ident++;
              }
            } else {
                while(ident < s.len) {
                    idByte = (uint8_t)((*(ident) >> 8) + utf16fixup[*(ident) >> 11]);
                    if(idByte < 0x02) {
                        sortKeySize++;
                    }
                    idByte = (uint8_t)((*(ident) & 0xFF));
                    if(idByte < 0x02) {
                        sortKeySize++;
                    }
                  ident++;
                }
            }
          }
      }

      *(primaries++) = '\0';
    } else {
      /* This is wrong - we should return a key size - not set it to zero */
      sortKeySize = 0;
    }

    if(terStart != tert) {
        uprv_free(terStart);
        uprv_free(secStart);
        uprv_free(caseStart);
        uprv_free(quadStart);
    }

    if(normSource != normBuffer) {
        uprv_free(normSource);
    }

    return sortKeySize;
}

int32_t
ucol_calcSortKeySimpleTertiary(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        int32_t        resultLength,
        UBool allocatePrimary,
        UErrorCode *status)
{
    uint32_t i = 0; /* general purpose counter */

    /* Stack allocated buffers for buffers we use */
    uint8_t second[UCOL_MAX_BUFFER], tert[UCOL_MAX_BUFFER];

    uint8_t *primaries = *result, *secondaries = second, *tertiaries = tert;

    if(U_FAILURE(*status)) {
      return 0;
    }

    if(primaries == NULL && allocatePrimary == TRUE) {
        primaries = *result = (uint8_t *)uprv_malloc(2*UCOL_MAX_BUFFER);
        resultLength = 2*UCOL_MAX_BUFFER;
    }
    uint8_t *primarySafeEnd = primaries + resultLength - 2;

    uint32_t secSize = UCOL_MAX_BUFFER, terSize = UCOL_MAX_BUFFER;

    int32_t sortKeySize = 3; /* it is always \0 terminated plus separators for secondary and tertiary */

    UChar normBuffer[UCOL_NORMALIZATION_GROWTH*UCOL_MAX_BUFFER];
    UChar *normSource = normBuffer;
    int32_t normSourceLen = UCOL_NORMALIZATION_GROWTH*UCOL_MAX_BUFFER;

    int32_t len = (sourceLength == -1 ? u_strlen(source) : sourceLength);


    collIterate s;
    init_collIterate((UChar *)source, len, &s, FALSE);

    /* If we need to normalize, we'll do it all at once at the beggining! */
    UColAttributeValue normMode = coll->normalizationMode;
    if((normMode != UCOL_OFF) 
      /* && (unorm_quickCheck(source, len, UNORM_NFD, status) != UNORM_YES)
      && (unorm_quickCheck(source, len, UNORM_NFC, status) != UNORM_YES)) */
      /* changed by synwee */
      && !checkFCD(source, len, status))
    {

        normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, normSourceLen, status);
        if(U_FAILURE(*status)) {
            *status=U_ZERO_ERROR;
            normSource = (UChar *) uprv_malloc((normSourceLen+1)*sizeof(UChar));
            normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, (normSourceLen+1), status);
        }
        normSource[normSourceLen] = 0;
        s.string = normSource;
        s.pos = normSource;
        s.len = normSource+normSourceLen;
    }

    len = s.len-s.pos;

    if(resultLength == 0) {
        return ucol_getSortKeySize(coll, &s, sortKeySize, coll->strength, len);
    }

    uint32_t minBufferSize = UCOL_MAX_BUFFER;

    uint8_t *primStart = primaries;
    uint8_t *secStart = secondaries;
    uint8_t *terStart = tertiaries;

    uint32_t order = 0;
    uint32_t ce = 0;

    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;


    uint32_t prevBuffSize = 0;

    UBool finished = FALSE;
    UBool resultOverflow = FALSE;
    UBool notIsContinuation = FALSE;

    uint32_t count2 = 0, count3 = 0;

    for(;;) {
        for(i=prevBuffSize; i<minBufferSize; ++i) {

            /*order = ucol_getNextCE(coll, &s, status);*/
            UCOL_GETNEXTCE(order, coll, s, status);

            if(isCEIgnorable(order)) {
              continue;
            }

            if(order == UCOL_NO_MORE_CES) {
                finished = TRUE;
                break;
            }

            /* We're saving order in ce, since we will destroy order in order to get primary, secondary, tertiary in order ;)*/
            ce = order;
            notIsContinuation = !isContinuation(ce);

            order ^= caseSwitch;
            tertiary = (uint8_t)((order & tertiaryMask)); /* TODO need case bit here this is temporary - removing case bit */         
            secondary = (uint8_t)((order >>= 8) & 0xFF);
            primary2 = (uint8_t)((order >>= 8) & 0xFF);
            primary1 = (uint8_t)(order >>= 8);

            /* In the code below, every increase in any of buffers is followed by the increase to  */
            /* sortKeySize - this might look tedious, but it is needed so that we can find out if  */
            /* we're using too much space and need to reallocate the primary buffer or easily bail */
            /* out to ucol_getSortKeySizeNew.                                                      */

            /* Note: This code assumes that the table is well built i.e. not having 0 bytes where they are not supposed to be. */
            /* Usually, we'll have non-zero primary1 & primary2, except in cases of LatinOne and friends, when primary2 will   */
            /* be zero with non zero primary1. primary3 is different than 0 only for long primaries - see above.               */
            if(primary1 != UCOL_NEW_IGNORABLE) {
              *primaries++ = primary1; /* scriptOrder[primary1]; */ /* This is the script ordering thingie */
              if(primary2 != UCOL_NEW_IGNORABLE) {
                *primaries++ = primary2; /* second part */
              }
            }               

            /* This is compression code. */
            if (secondary == UCOL_COMMON2 && notIsContinuation) {
              ++count2;
            } else {
              if (count2 > 0) {
                if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
                  while (count2 >= UCOL_TOP_COUNT2) {
                    *secondaries++ = UCOL_COMMON_TOP2 - UCOL_TOP_COUNT2;
                    count2 -= UCOL_TOP_COUNT2;
                  }
                  *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - count2);
                } else {
                  while (count2 >= UCOL_BOT_COUNT2) {
                    *secondaries++ = UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2;
                    count2 -= UCOL_BOT_COUNT2;
                  }
                  *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + count2);
                }
                count2 = 0;
              }
              *secondaries++ = secondary;
            }


            /* This is compression code. */
            /* sequence size check is included in the if clause */
            if (tertiary == UCOL_COMMON3 && notIsContinuation) {
              ++count3;
            } else {
              if(tertiary > UCOL_COMMON3) {
                tertiary |= UCOL_FLAG_BIT_MASK;
              }
              if (count3 > 0) {
                if (tertiary > UCOL_COMMON3) {
                  while (count3 >= UCOL_TOP_COUNT3) {
                    *tertiaries++ = UCOL_COMMON_TOP3 - UCOL_TOP_COUNT3;
                    count3 -= UCOL_TOP_COUNT3;
                  }
                  *tertiaries++ = (uint8_t)(UCOL_COMMON_TOP3 - count3);
                } else {
                  while (count3 >= UCOL_BOT_COUNT3) {
                    *tertiaries++ = UCOL_COMMON_BOT3 + UCOL_BOT_COUNT3;
                    count3 -= UCOL_BOT_COUNT3;
                  }
                  *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + count3);
                }
                count3 = 0;
              }
              *tertiaries++ = tertiary;
            }

            if(primaries > primarySafeEnd) { /* We have stepped over the primary buffer */
              int32_t sks = sortKeySize+(primaries - primStart)+(secondaries - secStart)+(tertiaries - terStart);
              if(allocatePrimary == FALSE) { /* need to save our butts if we cannot reallocate */
                resultOverflow = TRUE;
                sortKeySize = ucol_getSortKeySize(coll, &s, sks, coll->strength, len);
                *status = U_MEMORY_ALLOCATION_ERROR;
                finished = TRUE;
                break;
              } else { /* It's much nicer if we can actually reallocate */
                uint8_t *newStart;
                newStart = (uint8_t *)uprv_realloc(primStart, 2*sks);
                if(primStart == NULL) {
                  *status = U_MEMORY_ALLOCATION_ERROR;
                  finished = TRUE;
                  break;
                }
                primaries=newStart+(primaries-primStart);
                resultLength = 2*sks;
                primStart = *result = newStart;
                primarySafeEnd = primStart + resultLength - 2;
              }
            }
        }
        if(finished) {
            break;
        } else {
          prevBuffSize = minBufferSize;
          secStart = reallocateBuffer(&secondaries, secStart, second, &secSize, status);
          terStart = reallocateBuffer(&tertiaries, terStart, tert, &terSize, status);
          minBufferSize *= 2;
        }
    }

    if(U_SUCCESS(*status)) {
      sortKeySize += (primaries - primStart);
      /* we have done all the CE's, now let's put them together to form a key */
      if (count2 > 0) {
        while (count2 >= UCOL_BOT_COUNT2) {
          *secondaries++ = UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2;
          count2 -= UCOL_BOT_COUNT2;
        }
        *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + count2);
      }
      uint32_t secsize = secondaries-secStart;
      sortKeySize += secsize;
      *(primaries++) = UCOL_LEVELTERMINATOR;
      /* Need overflow test here */
      uprv_memcpy(primaries, secStart, secsize); 
      primaries += secsize;

      if (count3 > 0) {
        while (count3 >= UCOL_BOT_COUNT3) {
          *tertiaries++ = UCOL_COMMON_BOT3 + UCOL_BOT_COUNT3;
          count3 -= UCOL_BOT_COUNT3;
        }
        *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + count3);
      }
      *(primaries++) = UCOL_LEVELTERMINATOR;
      uint32_t tersize = tertiaries - terStart;
      sortKeySize += tersize;
      /* Need overflow test here */
      uprv_memcpy(primaries, terStart, tersize);
      primaries += tersize;

      *(primaries++) = '\0';
    } else {
      /* This is wrong - we should return a key size - not set it to zero */
      sortKeySize = 0;
    }

    if(terStart != tert) {
        uprv_free(terStart);
        uprv_free(secStart);
    }

    if(normSource != normBuffer) {
        uprv_free(normSource);
    }

    return sortKeySize;
}

/* This is a trick string compare function that goes in and uses sortkeys to compare */
/* It is used when compare gets in trouble and needs to bail out                     */
UCollationResult ucol_compareUsingSortKeys(const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
    uint8_t sourceKey[UCOL_MAX_BUFFER], targetKey[UCOL_MAX_BUFFER];
    uint8_t *sourceKeyP = sourceKey;
    uint8_t *targetKeyP = targetKey;
    int32_t sourceKeyLen = UCOL_MAX_BUFFER, targetKeyLen = UCOL_MAX_BUFFER;

    sourceKeyLen = ucol_getSortKey(coll, source, sourceLength, sourceKeyP, sourceKeyLen);
    if(sourceKeyLen > UCOL_MAX_BUFFER) {
        sourceKeyP = (uint8_t*)uprv_malloc(sourceKeyLen*sizeof(uint8_t));
        sourceKeyLen = ucol_getSortKey(coll, source, sourceLength, sourceKeyP, sourceKeyLen);
    }

    targetKeyLen = ucol_getSortKey(coll, target, targetLength, targetKeyP, targetKeyLen);
    if(targetKeyLen > UCOL_MAX_BUFFER) {
        targetKeyP = (uint8_t*)uprv_malloc(targetKeyLen*sizeof(uint8_t));
        targetKeyLen = ucol_getSortKey(coll, target, targetLength, targetKeyP, targetKeyLen);
    }

    int32_t result = uprv_strcmp((const char*)sourceKeyP, (const char*)targetKeyP);

    if(sourceKeyP != sourceKey) {
        uprv_free(sourceKeyP);
    }

    if(targetKeyP != targetKey) {
        uprv_free(targetKeyP);
    }

    if(result<0) {
        return UCOL_LESS;
    } else if(result>0) {
        return UCOL_GREATER;
    } else {
        return UCOL_EQUAL;
    }
}


/****************************************************************************/
/* Following are the functions that deal with the properties of a collator  */
/* there are new APIs and some compatibility APIs                           */
/****************************************************************************/
void ucol_updateInternalState(UCollator *coll) {
      if(coll->caseFirst == UCOL_UPPER_FIRST) {
        coll->caseSwitch = UCOL_CASE_SWITCH;
      } else {
        coll->caseSwitch = UCOL_NO_CASE_SWITCH;
      }
      if(coll->caseLevel == UCOL_ON || coll->caseFirst == UCOL_OFF) {
        coll->tertiaryMask = UCOL_REMOVE_CASE;
      } else {
        coll->tertiaryMask = UCOL_KEEP_CASE;
      }
}

/* Attribute setter API */
U_CAPI void ucol_setAttribute(UCollator *coll, UColAttribute attr, UColAttributeValue value, UErrorCode *status) {
    switch(attr) {
    case UCOL_FRENCH_COLLATION: /* attribute for direction of secondary weights*/
        if(value == UCOL_ON) {
            coll->frenchCollation = UCOL_ON;
            coll->frenchCollationisDefault = FALSE;
        } else if (value == UCOL_OFF) {
            coll->frenchCollation = UCOL_OFF;
            coll->frenchCollationisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->frenchCollationisDefault = TRUE;
            coll->frenchCollation = coll->image->frenchCollation;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_ALTERNATE_HANDLING: /* attribute for handling variable elements*/
        if(value == UCOL_SHIFTED) {
            coll->alternateHandling = UCOL_SHIFTED;
            coll->alternateHandlingisDefault = FALSE;
        } else if (value == UCOL_NON_IGNORABLE) {
            coll->alternateHandling = UCOL_NON_IGNORABLE;
            coll->alternateHandlingisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->alternateHandlingisDefault = TRUE;
            coll->alternateHandling = coll->image->alternateHandling ;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_CASE_FIRST: /* who goes first, lower case or uppercase */
        if(value == UCOL_LOWER_FIRST) {
            coll->caseFirst = UCOL_LOWER_FIRST;
            coll->caseFirstisDefault = FALSE;
        } else if (value == UCOL_UPPER_FIRST) {
            coll->caseFirst = UCOL_UPPER_FIRST;
            coll->caseFirstisDefault = FALSE;
        } else if (value == UCOL_OFF) {
          coll->caseFirst = UCOL_OFF;
          coll->caseFirstisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->caseFirst = coll->image->caseFirst;
            coll->caseFirstisDefault = TRUE;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_CASE_LEVEL: /* do we have an extra case level */
        if(value == UCOL_ON) {
            coll->caseLevel = UCOL_ON;
            coll->caseLevelisDefault = FALSE;
        } else if (value == UCOL_OFF) {
            coll->caseLevel = UCOL_OFF;
            coll->caseLevelisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->caseLevel = coll->image->caseLevel;
            coll->caseLevelisDefault = TRUE;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_NORMALIZATION_MODE: /* attribute for normalization */
        if(value == UCOL_ON) {
            coll->normalizationMode = UCOL_ON;
            coll->normalizationModeisDefault = FALSE;
        } else if (value == UCOL_OFF) {
            coll->normalizationMode = UCOL_OFF;
            coll->normalizationModeisDefault = FALSE;
        } else if (value == UCOL_ON_WITHOUT_HANGUL) {
            coll->normalizationMode = UCOL_ON_WITHOUT_HANGUL ;
            coll->normalizationModeisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->normalizationModeisDefault = TRUE;
            coll->normalizationMode = coll->image->normalizationMode;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_STRENGTH:         /* attribute for strength */
        if (value == UCOL_DEFAULT) {
            coll->strengthisDefault = TRUE;
            coll->strength = coll->image->strength;
        } else if (value <= UCOL_IDENTICAL) {
            coll->strengthisDefault = FALSE;
            coll->strength = value;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_ATTRIBUTE_COUNT:
    default:
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        break;
    }
    ucol_updateInternalState(coll);
}

U_CAPI UColAttributeValue ucol_getAttribute(const UCollator *coll, UColAttribute attr, UErrorCode *status) {
    switch(attr) {
    case UCOL_FRENCH_COLLATION: /* attribute for direction of secondary weights*/
        if(coll->frenchCollationisDefault) {
            return coll->image->frenchCollation;
        } else {
            return coll->frenchCollation;
        }
        break;
    case UCOL_ALTERNATE_HANDLING: /* attribute for handling variable elements*/
        if(coll->alternateHandlingisDefault) {
            return coll->image->alternateHandling;
        } else {
            return coll->alternateHandling;
        }
        break;
    case UCOL_CASE_FIRST: /* who goes first, lower case or uppercase */
        if(coll->caseFirstisDefault) {
            return coll->image->caseFirst;
        } else {
            return coll->caseFirst;
        }
        break;
    case UCOL_CASE_LEVEL: /* do we have an extra case level */
        if(coll->caseLevelisDefault) {
            return coll->image->caseLevel;
        } else {
            return coll->caseLevel;
        }
        break;
    case UCOL_NORMALIZATION_MODE: /* attribute for normalization */
        if(coll->normalizationModeisDefault) {
            return coll->image->normalizationMode;
        } else {
            return coll->normalizationMode;
        }
        break;
    case UCOL_STRENGTH:         /* attribute for strength */
        if(coll->strengthisDefault) {
            return coll->image->strength;
        } else {
            return coll->strength;
        }
        break;
    case UCOL_ATTRIBUTE_COUNT:
    default:
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        break;
    }
    return UCOL_DEFAULT;
}

U_CAPI void
ucol_setNormalization(  UCollator            *coll,
            UNormalizationMode    mode)
{
  UErrorCode status = U_ZERO_ERROR;
  switch(mode) {
  case UCOL_NO_NORMALIZATION:
    ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_OFF, &status);
    break;
  case UCOL_DECOMP_CAN:
    ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
    break;
  default:
    /* Shouldn't get here. */
    /* This is quite a bad API */
    /* deprecate */
    /* *status = U_ILLEGAL_ARGUMENT_ERROR; */
    return;
  }
}

U_CAPI UNormalizationMode
ucol_getNormalization(const UCollator* coll)
{
  UErrorCode status = U_ZERO_ERROR;
  if(ucol_getAttribute(coll, UCOL_NORMALIZATION_MODE, &status) == UCOL_ON) {
    return UCOL_DECOMP_CAN;
  } else {
    return UCOL_NO_NORMALIZATION;
  }
}

U_CAPI void
ucol_setStrength(    UCollator                *coll,
            UCollationStrength        strength)
{
  UErrorCode status = U_ZERO_ERROR;
  ucol_setAttribute(coll, UCOL_STRENGTH, strength, &status);
}

U_CAPI UCollationStrength
ucol_getStrength(const UCollator *coll)
{
  UErrorCode status = U_ZERO_ERROR;
  return ucol_getAttribute(coll, UCOL_STRENGTH, &status);
}

/****************************************************************************/
/* Following are misc functions                                             */
/* there are new APIs and some compatibility APIs                           */
/****************************************************************************/

U_CAPI UCollator *
ucol_safeClone(const UCollator *coll, void *stackBuffer, int32_t * pBufferSize, UErrorCode *status) 
{
    UCollator * localCollator;
    int32_t bufferSizeNeeded = sizeof(UCollator);

    if (status == NULL || U_FAILURE(*status)){
        return 0;
    }
    if (!pBufferSize || !coll){
       *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if (*pBufferSize == 0){ /* 'preflighting' request - set needed size into *pBufferSize */
        *pBufferSize =  bufferSizeNeeded;
        return 0;
    }
    if (*pBufferSize < bufferSizeNeeded || stackBuffer == NULL) { 
        /* allocate one here...*/
        int32_t length;
        const UChar * rules = ucol_getRules(coll, &length);
       
        localCollator = ucol_openRules(rules, 
                                       length, 
                                       ucol_getNormalization(coll),
                                       ucol_getStrength(coll), 
                                       status);
        if (U_SUCCESS(*status))
        {
            *status = U_SAFECLONE_ALLOCATED_ERROR;
        }
    } else {
        localCollator = (UCollator *)stackBuffer;
        memcpy(localCollator, coll, sizeof(UCollator));
        localCollator->freeOnClose = FALSE;
    }
    return localCollator;      
}

U_CAPI int32_t 
ucol_getRulesEx(const UCollator *coll, UColRuleOption delta, UChar *buffer, int32_t bufferLen) {
  int32_t len = 0;
  int32_t UCAlen = 0;
  const UChar* ucaRules;
  const UChar *rules = ucol_getRules(coll, &len);
  *buffer=0;
  if(delta == UCOL_FULL_RULES) {
    UErrorCode status = U_ZERO_ERROR;
    /* take the UCA rules and append real rules at the end */
    /* UCA rules will be probably coming from the root RB */
    ucaRules = ures_getStringByKey(coll->rb,"%%UCARULES",&UCAlen,&status);

  }
  if(bufferLen >= len + UCAlen) {
    u_strcat(buffer, rules);
    if(UCAlen >0)
        u_strcat(buffer,ucaRules);
  } else {
    u_strncat(buffer, rules, (bufferLen-UCAlen)*sizeof(UChar));
  }
  return len+UCAlen;
}

U_CAPI const UChar*
ucol_getRules(    const    UCollator       *coll, 
        int32_t            *length)
{
  if(coll->rules != NULL) {
    *length = u_strlen(coll->rules);
    return coll->rules;
  } else {
    UErrorCode status = U_ZERO_ERROR;
    if(coll->rb != NULL) {
      UResourceBundle *collElem = ures_getByKey(coll->rb, "CollationElements", NULL, &status);
      if(U_SUCCESS(status)) {
        /*Semantic const */
        ((UCollator *)coll)->rules = ures_getStringByKey(collElem, "Sequence", length, &status);
        ((UCollator *)coll)->freeRulesOnClose = FALSE;
        ures_close(collElem);
        return coll->rules;
      }
    }
    *length = 0;
    return &coll->zero;
  }
}

U_CAPI int32_t
ucol_getDisplayName(    const    char        *objLoc,
            const    char        *dispLoc,
            UChar             *result,
            int32_t         resultLength,
            UErrorCode        *status)
{
  if(U_FAILURE(*status)) return -1;
  UnicodeString dst(result, resultLength, resultLength);
  Collator::getDisplayName(Locale(objLoc), Locale(dispLoc), dst);
  int32_t actLen;
  T_fillOutputParams(&dst, result, resultLength, &actLen, status);
  return actLen;
}

U_CAPI const char*
ucol_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

U_CAPI int32_t
ucol_countAvailable()
{
  return uloc_countAvailable();
}

U_CAPI void 
ucol_getVersion(const UCollator* coll, 
                UVersionInfo versionInfo) 
{
    /* RunTime version  */
    uint8_t rtVersion = UCOL_RUNTIME_VERSION;
    /* Builder version*/
    uint8_t bdVersion = coll->dataInfo.dataVersion[0];

    /* Charset Version. Need to get the version from cnv files
     * makeconv should populate cnv files with version and 
     * an api has to be provided in ucnv.h to obtain this version
     */
    uint8_t csVersion = 0;

    /* combine the version info */
    uint16_t cmbVersion = (uint16_t)((rtVersion<<11) | (bdVersion<<6) | (csVersion));

    /* Tailoring rules */
    versionInfo[0] = (uint8_t)(cmbVersion>>8);
    versionInfo[1] = (uint8_t)cmbVersion;
    versionInfo[2] = coll->dataInfo.dataVersion[1];
    versionInfo[3] = UCA->dataInfo.dataVersion[1];
}


static UBool ucol_unsafeCP(UChar c, const UCollator *coll) {
    int32_t  hash = c;
    uint8_t  htbyte;

    if (hash >= UCOL_UNSAFECP_TABLE_SIZE*8) {
        if (hash >= 0xd800 && hash <= 0xf8ff) {
            /*  Part of a surrogate, or in private use area.            */
            /*   These are always considered unsafe.                    */
            return TRUE;
        }
        hash = (hash & UCOL_UNSAFECP_TABLE_MASK) + 256;
    }
    htbyte = coll->unsafeCP[hash>>3];
    if (((htbyte >> (hash & 7)) & 1) == 1)
        return TRUE;   

    /*  TODO:  main UCA table data needs to be merged into tailoring tables,   */
    /*         and this second level of test removed from here.                */
    if (coll == UCA)
        return FALSE;
    
    htbyte = UCA->unsafeCP[hash>>3];
    return ((htbyte >> (hash & 7)) & 1) == 1;
}


/****************************************************************************/
/* Following are the string compare functions                               */
/*                                                                          */
/****************************************************************************/

/* compare two strings... Can get interesting */
U_CAPI UCollationResult
ucol_strcoll(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
    /* check if source and target are valid strings */
    if (source==target  && sourceLength==targetLength)
    {
        return UCOL_EQUAL;
    }

    /*
    sourceLength = sourceLength == -1 ? u_strlen(source) : sourceLength;
    targetLength = targetLength == -1 ? u_strlen(target) : targetLength;

    if(sourceLength == targetLength && uprv_memcmp(source, target, sizeof(UChar)*sourceLength) == 0) {
      return UCOL_EQUAL;
    }
    */
    
    /* Scan the strings.  Find:
    /*    their length, if not given by caller                                              */
    /*    The length of any leading portion that is equal                                   */
    /*    Whether they are exactly equal.  (in which case we just return                    */
    const UChar    *pSrc    = source;
    const UChar    *pTarg   = target;
    
    const UChar    *pSrcEnd = source + sourceLength;
    const UChar    *pTargEnd = target + targetLength;
    
    int32_t        equalLength = 0;
    
    // Scan while the strings are bitwise ==, or until one is exhausted.
    for (;;) {
        if (pSrc == pSrcEnd || pTarg == pTargEnd)
            break;
        if (*pSrc != *pTarg)
            break;
        if (*pSrc == 0 && (sourceLength == -1 || targetLength == -1))
            break;
        equalLength++;
        pSrc++;
        pTarg++;
    }
    
    // If we made it all the way through both strings, we are done.  They are ==
    if ((pSrc ==pSrcEnd  || (pSrcEnd <pSrc  && *pSrc==0))  &&   /* At end of src string, however it was specified. */
        (pTarg==pTargEnd || (pTargEnd<pTarg && *pTarg==0)))     /* and also at end of dest string                  */
        return UCOL_EQUAL;
    
    // If we don't know the length of the src string, continue scanning it to get the length..
    if (sourceLength == -1) {
        while (*pSrc != 0 ) {
            pSrc++;
        }
        sourceLength = pSrc - source;
    }
    
    // If we don't know the length of the targ string, continue scanning it to get the length..
    if (targetLength == -1) {
        while (*pTarg != 0 ) {
            pTarg++;
        }
        targetLength = pTarg - target;
    }
    
    
    if (equalLength > 2) {
        /* There is an identical portion at the beginning of the two strings.        */
        /*   If the identical portion ends within a contraction or a comibining      */
        /*   character sequence, back up to the start of that sequence.              */
        pSrc  = source + equalLength;        /* point to the first differing chars   */
        pTarg = target + equalLength;
        if (pSrc  != source+sourceLength && ucol_unsafeCP(*pSrc, coll) || 
            pTarg != target+targetLength && ucol_unsafeCP(*pTarg, coll))
        {
            // We are stopped in the middle of a contraction.
            // Scan backwards through the == part of the string looking for the start of the contraction.
            //   It doesn't matter which string we scan, since they are the same in this region.
            do 
            {
                equalLength--;
                pSrc--;
            }
            while (equalLength>0 && ucol_unsafeCP(*pSrc, coll));
        }
        
        source += equalLength;
        target += equalLength;
        sourceLength -= equalLength;
        targetLength -= equalLength;
    }
    
    

    UCollationResult result = UCOL_EQUAL;
    UErrorCode status = U_ZERO_ERROR;

    UChar normSource[UCOL_MAX_BUFFER], normTarget[UCOL_MAX_BUFFER];
    UChar *normSourceP = normSource;
    UChar *normTargetP = normTarget;
    uint32_t normSourceLength = UCOL_MAX_BUFFER, normTargetLength = UCOL_MAX_BUFFER;

    collIterate sColl, tColl;

    
    init_collIterate(source, sourceLength, &sColl, FALSE);
    if((coll->normalizationMode == UCOL_ON)
      /* && (unorm_quickCheck( sColl.string, sColl.len - sColl.string, UNORM_NFD, &status) != UNORM_YES)
      && (unorm_quickCheck( sColl.string, sColl.len - sColl.string, UNORM_NFC, &status) != UNORM_YES)) */
      /* changed by synwee */
      && !checkFCD(sColl.string, sColl.len - sColl.string, &status))
    {
        normSourceLength = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, normSourceLength, &status);
        /* if we don't have enough space in buffers, we'll recursively call strcoll, so that we have single point */
        /* of exit - to free buffers we allocated. Otherwise, returns from strcoll are in various places and it   */
        /* would be hard to track all the exit points.                                                            */
        if(U_FAILURE(status)) { /* This would be buffer overflow */
            UColAttributeValue mode = coll->normalizationMode;
            normSourceP = (UChar *)uprv_malloc((normSourceLength+1)*sizeof(UChar));
            status = U_ZERO_ERROR;
            normSourceLength = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSourceP, normSourceLength+1, &status);
            normTargetLength = unorm_normalize(target, targetLength, UNORM_NFD, 0, normTargetP, normTargetLength, &status);
            if(U_FAILURE(status)) { /* This would be buffer overflow */
                normTargetP = (UChar *)uprv_malloc((normTargetLength+1)*sizeof(UChar));
                status = U_ZERO_ERROR;
                normTargetLength = unorm_normalize(target, targetLength, UNORM_NFD, 0, normTargetP, normTargetLength+1, &status);
            }
            ((UCollator *)coll)->normalizationMode = UCOL_OFF;
            UCollationResult result = ucol_strcoll(coll, normSourceP, normSourceLength, normTargetP, normTargetLength);
            ((UCollator *)coll)->normalizationMode = mode;
            uprv_free(normSourceP);
            if(normTargetP != normTarget) {
                uprv_free(normTargetP);
            }
            return result;
        }
        init_collIterate(normSource, normSourceLength, &sColl, TRUE);
    }

    init_collIterate(target, targetLength, &tColl, FALSE);
    if((coll->normalizationMode == UCOL_ON)
      /* && (unorm_quickCheck(tColl.string, tColl.len - tColl.string, UNORM_NFD, &status) != UNORM_YES)
      && (unorm_quickCheck(tColl.string, tColl.len - tColl.string, UNORM_NFC, &status) != UNORM_YES)) */
      /* changed by synwee */
      && !checkFCD(tColl.string, tColl.len - tColl.string, &status))
    {
      normTargetLength = unorm_normalize(target, targetLength, UNORM_NFD, 0, normTarget, normTargetLength, &status);
      if(U_FAILURE(status)) { /* This would be buffer overflow */
          UColAttributeValue mode = coll->normalizationMode;
          normTargetP = (UChar *)uprv_malloc((normTargetLength+1)*sizeof(UChar));
          status = U_ZERO_ERROR;
          normTargetLength = unorm_normalize(target, targetLength, UNORM_NFD, 0, normTargetP, normTargetLength+1, &status);
          ((UCollator *)coll)->normalizationMode = UCOL_OFF;
          UCollationResult result = ucol_strcoll(coll, normSourceP, normSourceLength, normTargetP, normTargetLength);
          ((UCollator *)coll)->normalizationMode = mode;
          uprv_free(normTargetP);
          return result;
      }
      init_collIterate(normTarget, normTargetLength, &tColl, TRUE);
    }

    if (U_FAILURE(status))
    {
        return UCOL_EQUAL;
    }

    UColAttributeValue strength = coll->strength;
    UBool initialCheckSecTer = (strength  >= UCOL_SECONDARY);

    UBool checkSecTer = initialCheckSecTer;
    UBool checkTertiary = (strength  >= UCOL_TERTIARY);
    UBool checkQuad = (strength  >= UCOL_QUATERNARY);
    UBool checkIdent = (strength == UCOL_IDENTICAL);
    UBool checkCase = (coll->caseLevel == UCOL_ON);
    UBool isFrenchSec = (coll->frenchCollation == UCOL_ON) && checkSecTer;
    UBool upperFirst = (coll->caseFirst == UCOL_UPPER_FIRST) && checkTertiary;
    UBool shifted = (coll->alternateHandling == UCOL_SHIFTED) && checkQuad;

    uint32_t sCEsArray[512], tCEsArray[512];
    uint32_t *sCEs = sCEsArray, *tCEs = tCEsArray;
    uint32_t *sCEend = sCEs+512, *tCEend = tCEs+512;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;

    uint32_t LVT = shifted*((coll->variableMax1)<<24 | (coll->variableMax2)<<16);

    uint32_t secS = 0, secT = 0;

    uint32_t sOrder=0, tOrder=0;
    if(!shifted) {
      for(;;) {
        if(sCEs == sCEend || tCEs == tCEend) {
          return ucol_compareUsingSortKeys(coll, source, sourceLength, target, targetLength);
        }

        /* Get the next collation element in each of the strings, unless */
        /* we've been requested to skip it. */
        while(sOrder == 0) {
          /*UCOL_GETNEXTCE(sOrder, coll, sColl, &status);*/
          sOrder = ucol_getNextCE(coll, &sColl, &status);
          sOrder ^= caseSwitch;
          *(sCEs++) = sOrder;
          sOrder &= 0xFFFF0000;
        }

        while(tOrder == 0) {
          /*UCOL_GETNEXTCE(tOrder, coll, tColl, &status);*/
          tOrder = ucol_getNextCE(coll, &tColl, &status);
          tOrder ^= caseSwitch;
          *(tCEs++) = tOrder;
          tOrder &= 0xFFFF0000;
        } 

        if(sOrder == tOrder) {
            if(sOrder == 0x00010000) {

              break;
            } else {
              sOrder = 0; tOrder = 0;
              continue;
            }
        } else if(sOrder < tOrder) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        } 
      } /* no primary difference... do the rest from the buffers */
    } else { /* shifted - do a slightly more complicated processing */
      for(;;) {
        UBool sInShifted = FALSE;
        UBool tInShifted = FALSE;

        if(sCEs == sCEend || tCEs == tCEend) {
          return ucol_compareUsingSortKeys(coll, source, sourceLength, target, targetLength);
        }

/* This is where abridged version for shifted should go */
        for(;;) {
          /*UCOL_GETNEXTCE(sOrder, coll, sColl, &status);*/
          sOrder = ucol_getNextCE(coll, &sColl, &status);
          if(sOrder == UCOL_NO_MORE_CES) {
            *(sCEs++) = sOrder;
            break;
          } else if((sOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(sOrder)) {
            if((sOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(sInShifted) {
                sOrder &= 0xFFFF0000;
                *(sCEs++) = sOrder;
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                break;
              }
            } else { /* Just lower level values */
              if(sInShifted) {
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                continue;
              }
            }
          } else { /* regular */
            if(sOrder > LVT) {
              *(sCEs++) = sOrder;
              break;
            } else {
              if((sOrder & 0xFFFF0000) > 0) {
                sInShifted = TRUE;
                sOrder &= 0xFFFF0000;
                *(sCEs++) = sOrder;
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                continue;
              }
            }
          }
        }
        sOrder &= 0xFFFF0000;
        sInShifted = FALSE;

        for(;;) {
          /*UCOL_GETNEXTCE(tOrder, coll, tColl, &status);*/
          tOrder = ucol_getNextCE(coll, &tColl, &status);
          if(tOrder == UCOL_NO_MORE_CES) {
            *(tCEs++) = tOrder;
            break;
          } else if((tOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(tOrder)) {
            if((tOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(tInShifted) {
                tOrder &= 0xFFFF0000;
                *(tCEs++) = tOrder;
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                break;
              }
            } else { /* Just lower level values */
              if(tInShifted) {
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                continue;
              }
            }
          } else { /* regular */
            if(tOrder > LVT) {
              *(tCEs++) = tOrder;
              break;
            } else {
              if((tOrder & 0xFFFF0000) > 0) {
                tInShifted = TRUE;
                tOrder &= 0xFFFF0000;
                *(tCEs++) = tOrder;
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                continue;
              }
            }
          }
        }
        tOrder &= 0xFFFF0000;
        tInShifted = FALSE;

        if(sOrder == tOrder) {
            if(sOrder == 0x00010000) {
              break;
            } else {
              sOrder = 0; tOrder = 0;
              continue;
            }
        } else if(sOrder < tOrder) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        } 
      } /* no primary difference... do the rest from the buffers */
    }

    /* now, we're gonna reexamine collected CEs */
    sCEend = sCEs;
    tCEend = tCEs;

    /* This is the secondary level of comparison */
    if(checkSecTer) {
      if(!isFrenchSec) { /* normal */
        sCEs = sCEsArray;
        tCEs = tCEsArray;
        for(;;) {
          while (secS == 0) {
            secS = *(sCEs++) & 0xFF00;
          }

          while(secT == 0) {
              secT = *(tCEs++) & 0xFF00;
          }

          if(secS == secT) {
            if(secS == 0x0100) {
              break;
            } else {
              secS = 0; secT = 0; 
              continue;
            }
          } else if(secS < secT) {
            return UCOL_LESS;
          } else {
            return UCOL_GREATER;
          } 
        }
      } else { /* do the French */
        uint32_t *sCESave = NULL;
        uint32_t *tCESave = NULL;
        sCEs = sCEend-2; /* this could also be sCEs-- if needs to be optimized */
        tCEs = tCEend-2;
        for(;;) {
          while (secS == 0 && sCEs >= sCEsArray) {
            if(sCESave == 0) {
              secS = *(sCEs--) & 0xFF80;
              if(isContinuation(secS)) {
                while(isContinuation(secS = *(sCEs--) & 0xFF80));
                /* after this, secS has the start of continuation, and sCEs points before that */
                sCESave = sCEs; /* we save it, so that we know where to come back AND that we need to go forward */
                sCEs+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secS = *(sCEs++) & 0xFF80;
              if(!isContinuation(secS)) { /* This means we have finished with this cont */
                sCEs = sCESave;          /* reset the pointer to before continuation */
                sCESave = 0;
                continue;
              }
            }
            secS &= 0xFF00; /* remove the continuation bit */
          }

          while(secT == 0 && tCEs >= tCEsArray) {
            if(tCESave == 0) {
              secT = *(tCEs--) & 0xFF80;
              if(isContinuation(secT)) {
                while(isContinuation(secT = *(tCEs--) & 0xFF80));
                /* after this, secS has the start of continuation, and sCEs points before that */
                tCESave = tCEs; /* we save it, so that we know where to come back AND that we need to go forward */
                tCEs+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secT = *(tCEs++) & 0xFF80;
              if(!isContinuation(secT)) { /* This means we have finished with this cont */
                tCEs = tCESave;          /* reset the pointer to before continuation */
                tCESave = 0;
                continue;
              }
            }
            secT &= 0xFF00; /* remove the continuation bit */
          }

          if(secS == secT) {
            if(secS == 0x0100 || (sCEs < sCEsArray && tCEs < tCEsArray)) {
              break;
            } else {
              secS = 0; secT = 0; 
              continue;
            }
          } else if(secS < secT) {
            return UCOL_LESS;
          } else {
            return UCOL_GREATER;
          } 
        }
      }
    }

    /* doing the case bit */
    if(checkCase) {
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while((secS & UCOL_REMOVE_CASE) == 0) {
          secS = *(sCEs++) & UCOL_TERT_CASE_MASK;
        }

        while((secT & UCOL_REMOVE_CASE) == 0) {
          secT = *(tCEs++) & UCOL_TERT_CASE_MASK;
        }

        if((secS & UCOL_CASE_BIT_MASK) < (secT & UCOL_CASE_BIT_MASK)) {
          return UCOL_LESS;
        } else if((secS & UCOL_CASE_BIT_MASK) > (secT & UCOL_CASE_BIT_MASK)) {
          return UCOL_GREATER;
        } 

        if((secS & UCOL_REMOVE_CASE) == 0x01 || (secT & UCOL_REMOVE_CASE) == 0x01 ) {
          break;
        } else {
          secS = 0;
          secT = 0;
        }
      }
    }

    /* Tertiary level */
    if(checkTertiary) {
      secS = 0; 
      secT = 0;
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while((secS & UCOL_REMOVE_CASE) == 0) {
          secS = *(sCEs++) & tertiaryMask;
        }

        while((secT & UCOL_REMOVE_CASE)  == 0) {
            secT = *(tCEs++) & tertiaryMask;
        }

        if(secS == secT) {
          if((secS & UCOL_REMOVE_CASE) == 1) {
            break;
          } else {
            secS = 0; secT = 0; 
            continue;
          }
        } else if(secS < secT) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        } 
      }
    }
         

    if(shifted) {
      UBool sInShifted = TRUE;
      UBool tInShifted = TRUE;
      secS = 0; 
      secT = 0;
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while(secS == 0 && secS != 0x00010101 || (isContinuation(secS) && !sInShifted)) {
          secS = *(sCEs++);
          if(isContinuation(secS) && !sInShifted) {
            continue;
          }
          if(secS > LVT || (secS & 0xFFFF0000) == 0) {
            secS = 0xFFFF0000;
            sInShifted = FALSE;
          } else {
            sInShifted = TRUE;
          }
        }
        secS &= 0xFFFF0000;


        while(secT == 0 && secT != 0x00010101 || (isContinuation(secT) && !tInShifted)) {
          secT = *(tCEs++);
          if(isContinuation(secT) && !tInShifted) {
            continue;
          }
          if(secT > LVT || (secT & 0xFFFF0000) == 0) {
            secT = 0xFFFF0000;
            tInShifted = FALSE;
          } else {
            tInShifted = TRUE;
          }
        }
        secT &= 0xFFFF0000;

        if(secS == secT) {
          if(secS == 0x00010000) {
            break;
          } else {
            secS = 0; secT = 0;
            continue;
          }
        } else if(secS < secT) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        } 
      }
    }

    /*  For IDENTICAL comparisons, we use a bitwise character comparison */
    /*  as a tiebreaker if all else is equal */
    /*  NOTE: The java code compares result with 0, and  */
    /*  puts the result of the string comparison directly into result */
    /*    if (result == UCOL_EQUAL && strength == UCOL_IDENTICAL) */
    if(checkIdent)
    {
        UnicodeString sourceDecomp, targetDecomp;

        int8_t comparison;
        
        Normalizer::normalize(UnicodeString(source, sourceLength), ((RuleBasedCollator *)coll)->getDecomposition(), 
                      0, sourceDecomp,  status);

        Normalizer::normalize(UnicodeString(target, targetLength), ((RuleBasedCollator *)coll)->getDecomposition(), 
                      0, targetDecomp,  status);
        
        comparison = sourceDecomp.compare(targetDecomp);

        if (comparison < 0)
        {
            result = UCOL_LESS;
        }
        else if (comparison == 0)
        {
            result = UCOL_EQUAL;
        }
        else
        {
            result = UCOL_GREATER;
        }
    }

    return result;
}


void init_incrementalContext(UCharForwardIterator *source, void *sourceContext, incrementalContext *s) {
    s->len = s->stringP = s->stackString ;
    s->capacity = s->stackString+UCOL_MAX_BUFFER;
    s->CEpos = s->toReturn = s->CEs;
    s->source = source;
    s->sourceContext = sourceContext;
    s->currentChar = 0xFFFF;
    s->lastChar = 0xFFFF;
    s->panic = FALSE;
}

/* This is the incremental function */
U_CAPI UCollationResult ucol_strcollinc(const UCollator *coll, 
                                 UCharForwardIterator *source, void *sourceContext,
                                 UCharForwardIterator *target, void *targetContext)
{
    incrementalContext sColl, tColl;

    init_incrementalContext(source, sourceContext, &sColl);
    init_incrementalContext(target, targetContext, &tColl);

#if 0 
    /* This is Andy's fast preparatory scan */
    /* It's good to have it - once the regular function is working */
    /* Scan the strings.  Find:
    /*    their length, if not given by caller                                              */
    /*    The length of any leading portion that is equal                                   */
    /*    Whether they are exactly equal.  (in which case we just return                    */
    const UChar    *pSrc    = source;
    const UChar    *pTarg   = target;
    
    const UChar    *pSrcEnd = source + sourceLength;
    const UChar    *pTargEnd = target + targetLength;
    
    int32_t        equalLength = 0;
    
    // Scan while the strings are bitwise ==, or until one is exhausted.
    for (;;) {
        if (pSrc == pSrcEnd || pTarg == pTargEnd)
            break;
        if (*pSrc != *pTarg)
            break;
        if (*pSrc == 0 && (sourceLength == -1 || targetLength == -1))
            break;
        equalLength++;
        pSrc++;
        pTarg++;
    }
    
    // If we made it all the way through both strings, we are done.  They are ==
    if ((pSrc ==pSrcEnd  || (pSrcEnd <pSrc  && *pSrc==0))  &&   /* At end of src string, however it was specified. */
        (pTarg==pTargEnd || (pTargEnd<pTarg && *pTarg==0)))     /* and also at end of dest string                  */
        return UCOL_EQUAL;
    
    // If we don't know the length of the src string, continue scanning it to get the length..
    if (sourceLength == -1) {
        while (*pSrc != 0 ) {
            pSrc++;
        }
        sourceLength = pSrc - source;
    }
    
    // If we don't know the length of the targ string, continue scanning it to get the length..
    if (targetLength == -1) {
        while (*pTarg != 0 ) {
            pTarg++;
        }
        targetLength = pTarg - target;
    }
    
    
    if (equalLength > 2) {
        /* There is an identical portion at the beginning of the two strings.        */
        /*   If the identical portion ends within a contraction or a comibining      */
        /*   character sequence, back up to the start of that sequence.              */
        pSrc  = source + equalLength;        /* point to the first differing chars   */
        pTarg = target + equalLength;
        if (pSrc  != source+sourceLength && ucol_unsafeCP(*pSrc, coll) || 
            pTarg != target+targetLength && ucol_unsafeCP(*pTarg, coll))
        {
            // We are stopped in the middle of a contraction.
            // Scan backwards through the == part of the string looking for the start of the contraction.
            //   It doesn't matter which string we scan, since they are the same in this region.
            do 
            {
                equalLength--;
                pSrc--;
            }
            while (equalLength>0 && ucol_unsafeCP(*pSrc, coll));
        }
        
        source += equalLength;
        target += equalLength;
        sourceLength -= equalLength;
        targetLength -= equalLength;
    }
    
#endif

    UCollationResult result = UCOL_EQUAL;
    UErrorCode status = U_ZERO_ERROR;

    UChar normSource[UCOL_MAX_BUFFER], normTarget[UCOL_MAX_BUFFER];
    UChar *normSourceP = normSource;
    UChar *normTargetP = normTarget;
    uint32_t normSourceLength = UCOL_MAX_BUFFER, normTargetLength = UCOL_MAX_BUFFER;

    if(coll->normalizationMode != UCOL_OFF) { /*  run away screaming!!!! */
        return alternateIncrementalProcessing(coll, &sColl, &tColl);
    }

    UColAttributeValue strength = coll->strength;
    UBool initialCheckSecTer = (strength  >= UCOL_SECONDARY);

    UBool checkSecTer = initialCheckSecTer;
    UBool checkTertiary = (strength  >= UCOL_TERTIARY);
    UBool checkQuad = (strength  >= UCOL_QUATERNARY);
    UBool checkIdent = (strength == UCOL_IDENTICAL);
    UBool checkCase = (coll->caseLevel == UCOL_ON);
    UBool isFrenchSec = (coll->frenchCollation == UCOL_ON) && checkSecTer;
    UBool upperFirst = (coll->caseFirst == UCOL_UPPER_FIRST) && checkTertiary;
    UBool shifted = (coll->alternateHandling == UCOL_SHIFTED) && checkQuad;

    uint32_t sCEsArray[512], tCEsArray[512];
    uint32_t *sCEs = sCEsArray, *tCEs = tCEsArray;
    uint32_t *sCEend = sCEs+512, *tCEend = tCEs+512;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;

    uint32_t LVT = shifted*((coll->variableMax1)<<24 | (coll->variableMax2)<<16);

    uint32_t secS = 0, secT = 0;

    uint32_t sOrder=0, tOrder=0;
    if(!shifted) {
      for(;;) {
        if(sCEs == sCEend || tCEs == tCEend) {
          return alternateIncrementalProcessing(coll, &sColl, &tColl);
        }

        /* Get the next collation element in each of the strings, unless */
        /* we've been requested to skip it. */
        while(sOrder == 0) {
          sOrder = ucol_getIncrementalCE(coll, &sColl, &status);
          sOrder ^= caseSwitch;
          *(sCEs++) = sOrder;
          sOrder &= 0xFFFF0000;
        }

        while(tOrder == 0) {
          tOrder = ucol_getIncrementalCE(coll, &tColl, &status);
          tOrder ^= caseSwitch;
          *(tCEs++) = tOrder;
          tOrder &= 0xFFFF0000;
        } 

        if(sOrder == tOrder) {
            if(sOrder == 0x00010000) {

              break;
            } else {
              sOrder = 0; tOrder = 0;
              continue;
            }
        } else if(sOrder < tOrder) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        } 
      } /* no primary difference... do the rest from the buffers */
    } else { /* shifted - do a slightly more complicated processing */
      for(;;) {
        UBool sInShifted = FALSE;
        UBool tInShifted = FALSE;

        if(sCEs == sCEend || tCEs == tCEend) {
          return alternateIncrementalProcessing(coll, &sColl, &tColl);
        }

/* This is where abridged version for shifted should go */
        for(;;) {
          sOrder = ucol_getIncrementalCE(coll, &sColl, &status);
          if(sOrder == UCOL_NO_MORE_CES) {
            *(sCEs++) = sOrder;
            break;
          } else if((sOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(sOrder)) {
            if((sOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(sInShifted) {
                sOrder &= 0xFFFF0000;
                *(sCEs++) = sOrder;
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                break;
              }
            } else { /* Just lower level values */
              if(sInShifted) {
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                continue;
              }
            }
          } else { /* regular */
            if(sOrder > LVT) {
              *(sCEs++) = sOrder;
              break;
            } else {
              if((sOrder & 0xFFFF0000) > 0) {
                sInShifted = TRUE;
                sOrder &= 0xFFFF0000;
                *(sCEs++) = sOrder;
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                continue;
              }
            }
          }
        }
        sOrder &= 0xFFFF0000;
        sInShifted = FALSE;

        for(;;) {
          tOrder = ucol_getIncrementalCE(coll, &tColl, &status);
          if(tOrder == UCOL_NO_MORE_CES) {
            *(tCEs++) = tOrder;
            break;
          } else if((tOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(tOrder)) {
            if((tOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(tInShifted) {
                tOrder &= 0xFFFF0000;
                *(tCEs++) = tOrder;
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                break;
              }
            } else { /* Just lower level values */
              if(tInShifted) {
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                continue;
              }
            }
          } else { /* regular */
            if(tOrder > LVT) {
              *(tCEs++) = tOrder;
              break;
            } else {
              if((tOrder & 0xFFFF0000) > 0) {
                tInShifted = TRUE;
                tOrder &= 0xFFFF0000;
                *(tCEs++) = tOrder;
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                continue;
              }
            }
          }
        }
        tOrder &= 0xFFFF0000;
        tInShifted = FALSE;

        if(sOrder == tOrder) {
            if(sOrder == 0x00010000) {
              break;
            } else {
              sOrder = 0; tOrder = 0;
              continue;
            }
        } else if(sOrder < tOrder) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        } 
      } /* no primary difference... do the rest from the buffers */
    }

    /* now, we're gonna reexamine collected CEs */
    sCEend = sCEs;
    tCEend = tCEs;

    /* This is the secondary level of comparison */
    if(checkSecTer) {
      if(!isFrenchSec) { /* normal */
        sCEs = sCEsArray;
        tCEs = tCEsArray;
        for(;;) {
          while (secS == 0) {
            secS = *(sCEs++) & 0xFF00;
          }

          while(secT == 0) {
              secT = *(tCEs++) & 0xFF00;
          }

          if(secS == secT) {
            if(secS == 0x0100) {
              break;
            } else {
              secS = 0; secT = 0; 
              continue;
            }
          } else if(secS < secT) {
            return UCOL_LESS;
          } else {
            return UCOL_GREATER;
          } 
        }
      } else { /* do the French */
        uint32_t *sCESave = NULL;
        uint32_t *tCESave = NULL;
        sCEs = sCEend-2; /* this could also be sCEs-- if needs to be optimized */
        tCEs = tCEend-2;
        for(;;) {
          while (secS == 0 && sCEs >= sCEsArray) {
            if(sCESave == 0) {
              secS = *(sCEs--) & 0xFF80;
              if(isContinuation(secS)) {
                while(isContinuation(secS = *(sCEs--) & 0xFF80));
                /* after this, secS has the start of continuation, and sCEs points before that */
                sCESave = sCEs; /* we save it, so that we know where to come back AND that we need to go forward */
                sCEs+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secS = *(sCEs++) & 0xFF80;
              if(!isContinuation(secS)) { /* This means we have finished with this cont */
                sCEs = sCESave;          /* reset the pointer to before continuation */
                sCESave = 0;
                continue;
              }
            }
            secS &= 0xFF00; /* remove the continuation bit */
          }

          while(secT == 0 && tCEs >= tCEsArray) {
            if(tCESave == 0) {
              secT = *(tCEs--) & 0xFF80;
              if(isContinuation(secT)) {
                while(isContinuation(secT = *(tCEs--) & 0xFF80));
                /* after this, secS has the start of continuation, and sCEs points before that */
                tCESave = tCEs; /* we save it, so that we know where to come back AND that we need to go forward */
                tCEs+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secT = *(tCEs++) & 0xFF80;
              if(!isContinuation(secT)) { /* This means we have finished with this cont */
                tCEs = tCESave;          /* reset the pointer to before continuation */
                tCESave = 0;
                continue;
              }
            }
            secT &= 0xFF00; /* remove the continuation bit */
          }

          if(secS == secT) {
            if(secS == 0x0100 || (sCEs < sCEsArray && tCEs < tCEsArray)) {
              break;
            } else {
              secS = 0; secT = 0; 
              continue;
            }
          } else if(secS < secT) {
            return UCOL_LESS;
          } else {
            return UCOL_GREATER;
          } 
        }
      }
    }

    /* doing the case bit */
    if(checkCase) {
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while((secS & UCOL_REMOVE_CASE) == 0) {
          secS = *(sCEs++) & UCOL_TERT_CASE_MASK;
        }

        while((secT & UCOL_REMOVE_CASE) == 0) {
          secT = *(tCEs++) & UCOL_TERT_CASE_MASK;
        }

        if((secS & UCOL_CASE_BIT_MASK) < (secT & UCOL_CASE_BIT_MASK)) {
          return UCOL_LESS;
        } else if((secS & UCOL_CASE_BIT_MASK) > (secT & UCOL_CASE_BIT_MASK)) {
          return UCOL_GREATER;
        } 

        if((secS & UCOL_REMOVE_CASE) == 0x01 || (secT & UCOL_REMOVE_CASE) == 0x01 ) {
          break;
        } else {
          secS = 0;
          secT = 0;
        }
      }
    }

    /* Tertiary level */
    if(checkTertiary) {
      secS = 0; 
      secT = 0;
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while((secS & UCOL_REMOVE_CASE) == 0) {
          secS = *(sCEs++) & tertiaryMask;
        }

        while((secT & UCOL_REMOVE_CASE)  == 0) {
            secT = *(tCEs++) & tertiaryMask;
        }

        if(secS == secT) {
          if((secS & UCOL_REMOVE_CASE) == 1) {
            break;
          } else {
            secS = 0; secT = 0; 
            continue;
          }
        } else if(secS < secT) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        } 
      }
    }
         

    if(shifted) {
      UBool sInShifted = TRUE;
      UBool tInShifted = TRUE;
      secS = 0; 
      secT = 0;
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while(secS == 0 && secS != 0x00010101 || (isContinuation(secS) && !sInShifted)) {
          secS = *(sCEs++);
          if(isContinuation(secS) && !sInShifted) {
            continue;
          }
          if(secS > LVT || (secS & 0xFFFF0000) == 0) {
            secS = 0xFFFF0000;
            sInShifted = FALSE;
          } else {
            sInShifted = TRUE;
          }
        }
        secS &= 0xFFFF0000;


        while(secT == 0 && secT != 0x00010101 || (isContinuation(secT) && !tInShifted)) {
          secT = *(tCEs++);
          if(isContinuation(secT) && !tInShifted) {
            continue;
          }
          if(secT > LVT || (secT & 0xFFFF0000) == 0) {
            secT = 0xFFFF0000;
            tInShifted = FALSE;
          } else {
            tInShifted = TRUE;
          }
        }
        secT &= 0xFFFF0000;

        if(secS == secT) {
          if(secS == 0x00010000) {
            break;
          } else {
            secS = 0; secT = 0;
            continue;
          }
        } else if(secS < secT) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        } 
      }
    }

    /*  For IDENTICAL comparisons, we use a bitwise character comparison */
    /*  as a tiebreaker if all else is equal */
    /*  NOTE: The java code compares result with 0, and  */
    /*  puts the result of the string comparison directly into result */
    /*    if (result == UCOL_EQUAL && strength == UCOL_IDENTICAL) */
    if(checkIdent)
    {
        UnicodeString sourceDecomp, targetDecomp;

        int8_t comparison;
        
        Normalizer::normalize(UnicodeString(sColl.stringP, sColl.len-sColl.stringP-1), ((RuleBasedCollator *)coll)->getDecomposition(), 
                      0, sourceDecomp,  status);

        Normalizer::normalize(UnicodeString(tColl.stringP, tColl.len-tColl.stringP-1), ((RuleBasedCollator *)coll)->getDecomposition(), 
                      0, targetDecomp,  status);
        
        comparison = sourceDecomp.compare(targetDecomp);

        if (comparison < 0)
        {
            result = UCOL_LESS;
        }
        else if (comparison == 0)
        {
            result = UCOL_EQUAL;
        }
        else
        {
            result = UCOL_GREATER;
        }
    }

    return result;
}

/* convenience function for comparing strings */
U_CAPI UBool
ucol_greater(    const    UCollator        *coll,
        const    UChar            *source,
        int32_t            sourceLength,
        const    UChar            *target,
        int32_t            targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength) 
      == UCOL_GREATER);
}

/* convenience function for comparing strings */
U_CAPI UBool
ucol_greaterOrEqual(    const    UCollator    *coll,
            const    UChar        *source,
            int32_t        sourceLength,
            const    UChar        *target,
            int32_t        targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength) 
      != UCOL_LESS);
}

/* convenience function for comparing strings */
U_CAPI UBool
ucol_equal(        const    UCollator        *coll,
            const    UChar            *source,
            int32_t            sourceLength,
            const    UChar            *target,
            int32_t            targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength) 
      == UCOL_EQUAL);
}


int32_t ucol_getIncrementalCE(const UCollator *coll, incrementalContext *ctx, UErrorCode *status) {
    uint32_t order;
    if (ctx->CEpos > ctx->toReturn) {       /* Are there any CEs from previous expansions? */
      order = *(ctx->toReturn++);                         /* if so, return them */
      if(ctx->CEpos == ctx->toReturn) {
        ctx->CEpos = ctx->toReturn = ctx->CEs; 
      }
    } else {          /* This is the real business now */
      if(ctx->lastChar == 0xFFFF) {
          ctx->currentChar = ctx->source(ctx->sourceContext);
          incctx_appendChar(ctx, ctx->currentChar);
          if(ctx->currentChar == 0xFFFF) {
              return UCOL_NO_MORE_CES;
          }
      } else {
          ctx->currentChar = ctx->lastChar;
          ctx->lastChar = 0xFFFF;
      }

      UChar ch = ctx->currentChar;
      if(ch <= 0xFF) {                                                 /* if it's Latin One, we'll try to fast track it */
        order = coll->latinOneMapping[ch];                            /* by looking in up in an array */
      } else {                                                        /* otherwise, */
        order = ucmp32_get(coll->mapping, ch);                        /* we'll go for slightly slower trie */
      }
      if(order >= UCOL_NOT_FOUND) {                                   /* if a CE is special */
        order = ucol_getIncrementalSpecialCE(coll, order, ctx, status);       /* and try to get the special CE */
        if(order == UCOL_NOT_FOUND) {   /* We couldn't find a good CE in the tailoring */
          order = ucol_getIncrementalUCA(ch, ctx, status);
        }
      } 
    } 
    /* This means that contraction should spit back the last codepoint eaten! */
    return order; /* return the CE */
}

/* This function tries to get a CE from UCA, which should be always around  */
/* UChar is passed in in order to speed things up                           */
/* here is also the generation of implicit CEs                              */
uint32_t ucol_getIncrementalUCA(UChar ch, incrementalContext *collationSource, UErrorCode *status) {
    uint32_t order;
    if(ch < 0xFF) {               /* so we'll try to find it in the UCA */
      order = UCA->latinOneMapping[ch];
    } else {
      order = ucmp32_get(UCA->mapping, ch);
    }
    if(order >= UCOL_NOT_FOUND) { /* UCA also gives us a special CE */
      order = ucol_getIncrementalSpecialCE(UCA, order, collationSource, status); 
    } 
    if(order == UCOL_NOT_FOUND) { /* This is where we have to resort to algorithmical generation */
      /* We have to check if ch is possibly a first surrogate - then we need to take the next code unit */
      /* and make a bigger CE */
      const uint32_t 
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount;   // 11172
        //LLimit = LBase + LCount,    // 1113
        //VLimit = VBase + VCount,    // 1176
        //TLimit = TBase + TCount,    // 11C3
        //SLimit = SBase + SCount;    // D7A4

        // once we have failed to find a match for codepoint cp, and are in the implicit code.
 
        uint32_t L = ch - SBase;
        //if (ch < SLimit) { // since it is unsigned, catchs zero case too
        if (L < SCount) { // since it is unsigned, catchs zero case too

          // divide into pieces

          uint32_t T = L % TCount; // we do it in this order since some compilers can do % and / in one operation
          L /= TCount;
          uint32_t V = L % VCount;
          L /= VCount;

          // offset them

          L += LBase;
          V += VBase;
          T += TBase;

          // return the first CE, but first put the rest into the expansion buffer
          if (!collationSource->JamoSpecial) { // FAST PATH
            
            *(collationSource->CEpos++) = ucmp32_get(UCA->mapping, V);
            if (T != TBase) {
                *(collationSource->CEpos++) = ucmp32_get(UCA->mapping, T);
            }
            
            return ucmp32_get(UCA->mapping, L); // return first one

          } else { // Jamo is Special

            // do recursive processing of L, V, and T with fetchCE (but T only if not equal to TBase!!)
            // Since fetchCE returns a CE, and (potentially) stuffs items into the ce buffer,
            // this is how it is done.
/*
            int firstCE = fetchCE(L, ...);
            int* lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
            *lastExpansion = fetchCE(V,...);
            if (T != TBase) {
              lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
              *lastExpansion = fetchCE(T,...);
            }
*/
          }
      }

      collationSource->lastChar = collationSource->source(collationSource->sourceContext);
      incctx_appendChar(collationSource, collationSource->lastChar);

      if(UTF_IS_FIRST_SURROGATE(ch)) {
        if( (collationSource->lastChar != 0xFFFF) &&
          UTF_IS_SECOND_SURROGATE((collationSource->lastChar))) {
          uint32_t cp = (((ch)<<10UL)+(collationSource->lastChar)-((0xd800<<10UL)+0xdc00));
          collationSource->lastChar = 0xFFFF; /*used up*/
          if ((cp & 0xFFFE) == 0xFFFE || (0xD800 <= cp && cp <= 0xDC00)) {
              return 0;  /* illegal code value, use completely ignoreable! */
          }
          /* This is a code point minus 0x10000, that's what algorithm requires */
          order = 0xE0010303 | (cp & 0xFFE00) << 8;

          *(collationSource->CEpos++) = 0x80200080 | (cp & 0x001FF) << 22;
        } else {
          return 0; /* completely ignorable */
        }
      } else {
        /* otherwise */
        if(UTF_IS_SECOND_SURROGATE((ch)) || (ch & 0xFFFE) == 0xFFFE) {
          return 0; /* completely ignorable */
        }
        /* Make up an artifical CE from code point as per UCA */
        order = 0xD0800303 | (ch & 0xF000) << 12 | (ch & 0x0FE0) << 11;
        *(collationSource->CEpos++) = 0x04000080 | (ch & 0x001F) << 27;
      }
    }
    return order; /* return the CE */
}


int32_t ucol_getIncrementalSpecialCE(const UCollator *coll, uint32_t CE, incrementalContext *source, UErrorCode *status) {
  int32_t i = 0; /* general counter */

  if(U_FAILURE(*status)) return -1;

  while (TRUE) {
    const uint32_t *CEOffset = NULL;
    const UChar *UCharOffset = NULL;
    UChar schar, tchar;
    uint32_t size = 0;
    switch(getCETag(CE)) {
    case NOT_FOUND_TAG:
      /* This one is not found, and we'll let somebody else bother about it... no more games */
      return CE;
      break;
    case SURROGATE_TAG:
      /* pending surrogate discussion with Markus and Mark */
      return UCOL_NOT_FOUND;
      break;
    case THAI_TAG:
      /* Thai/Lao reordering */
      source->panic = TRUE;
      return UCOL_NO_MORE_CES;
      break;
    case CONTRACTION_TAG:
      /* This should handle contractions */
      for(;;) {
        /* First we position ourselves at the begining of contraction sequence */
        const UChar *ContractionStart = UCharOffset = (UChar *)coll->image+getContractOffset(CE);

        /* we need to convey the notion of having a backward search - most probably through the context object */
        /* if (backwardsSearch) offset += contractionUChars[(int16_t)offset]; else UCharOffset++;  */
        UCharOffset++; /* skip the backward offset, see above */
//!        schar = *(++source->pos);
        schar = source->lastChar = source->source(source->sourceContext);
        incctx_appendChar(source, source->lastChar);

        if (schar == 0xFFFF) { /* this is the end of string */
          CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex)); /* So we'll pick whatever we have at the point... */
//!          source->pos--; /* I think, since we'll advance in the getCE */         
          break;
        }
        while(schar > (tchar = *UCharOffset)) { /* since the contraction codepoints should be ordered, we skip all that are smaller */
          UCharOffset++;
        }
        if(schar != tchar) { /* we didn't find the correct codepoint. We can use either the first or the last CE */
          if(tchar != 0xFFFF) {
            UCharOffset = ContractionStart; /* We're not at the end, bailed out in the middle. Better use starting CE */
          }
//!          source->pos--; /* Spit out the last char of the string, wasn't tasty enough */
        } else {
          source->lastChar = 0xFFFF;
        }
        CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex));
        if(!isContraction(CE)) {
          break;  
        }
      }
      break;
    case EXPANSION_TAG:
      /* This should handle expansion. */
      /* NOTE: we can encounter both continuations and expansions in an expansion! */
      /* I have to decide where continuations are going to be dealt with */
      CEOffset = (uint32_t *)coll->image+getExpansionOffset(CE); /* find the offset to expansion table */
      size = getExpansionCount(CE);
      CE = *CEOffset++;
      if(size != 0) { /* if there are less than 16 elements in expansion, we don't terminate */
        for(i = 1; i<size; i++) {
          *(source->CEpos++) = *CEOffset++;
        }
      } else { /* else, we do */
        while(*CEOffset != 0) {
          *(source->CEpos++) = *CEOffset++;
        }
      }
      /*source->toReturn++;*/
      return CE;
      break;
    case CHARSET_TAG:
      /* probably after 1.8 */
      return UCOL_NOT_FOUND;
      break;
    default:
      *status = U_INTERNAL_PROGRAM_ERROR;
      CE=0;
      break;
    }
    if (CE <= UCOL_NOT_FOUND) break;
  }
  return CE;

}

void incctx_cleanUpContext(incrementalContext *ctx) {
    if(ctx->stringP != ctx->stackString) {
        uprv_free(ctx->stringP);
    }
}

UChar incctx_appendChar(incrementalContext *ctx, UChar c) {
    if(ctx->len == ctx->capacity) { /* bother, said Pooh, we need to reallocate */
        UChar *newStuff;
        if(ctx->stringP == ctx->stackString) { /* we haven't allocated before, need to allocate */
            newStuff = (UChar *)uprv_malloc(2*(ctx->capacity - ctx->stringP)*sizeof(UChar));
            if(newStuff == NULL) {
                /*freak out*/
            }
            uprv_memcpy(newStuff, ctx->stringP, (ctx->capacity - ctx->stringP)*sizeof(UChar));
        } else { /* we have already allocated, need to reallocate */
            newStuff = (UChar *)uprv_realloc(ctx->stringP, 2*(ctx->capacity - ctx->stringP)*sizeof(UChar));
            if(newStuff == NULL) {
                /*freak out*/
            }
        }
        ctx->len=newStuff+(ctx->len - ctx->stringP);
        ctx->capacity = newStuff+2*(ctx->capacity - ctx->stringP);
        ctx->stringP = newStuff;
    }
    *(ctx->len++) = c;
    return c;
}



UCollationResult alternateIncrementalProcessing(const UCollator *coll, incrementalContext *srcCtx, incrementalContext *trgCtx) {
    if(srcCtx->stringP == srcCtx->len || *(srcCtx->len-1) != 0xFFFF) {
        while(incctx_appendChar(srcCtx, srcCtx->source(srcCtx->sourceContext)) != 0xFFFF);
    }
    if(trgCtx->stringP == trgCtx->len || *(trgCtx->len-1) != 0xFFFF) {
        while(incctx_appendChar(trgCtx, trgCtx->source(trgCtx->sourceContext)) != 0xFFFF);
    }
    UCollationResult result = ucol_strcoll(coll, srcCtx->stringP, srcCtx->len-srcCtx->stringP-1, trgCtx->stringP, trgCtx->len-trgCtx->stringP-1);
    incctx_cleanUpContext(srcCtx);
    incctx_cleanUpContext(trgCtx);
    return result;
}

#if 0
        /* This is abridged version of the loop                 */
        /* should work the same, but it's harder to understand  */
        for(;;) {
          /*UCOL_GETNEXTCE(sOrder, coll, sColl, &status);*/
          sOrder = ucol_getNextCE(coll, &sColl, &status);
          sOrder ^= caseSwitch;
          if(sOrder == 0x00010101) {
            *(sCEs++) = sOrder;
            break;
          } else if((sOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(sOrder)) {
            if((sOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(sInShifted) {
                sOrder &= 0xFFFF0000;
              } else {
                *(sCEs++) = sOrder;
                break;
              }
            } else { /* Just lower level values */
              if(sInShifted) {
                continue;
              }
            }
          } else { /* regular */
            if(sOrder > LVT) {
              *(sCEs++) = sOrder;
              break;
            } else {
              if((sOrder & 0xFFFF0000) > 0) {
                sInShifted = TRUE;
                sOrder &= 0xFFFF0000;
              }
            }
          }
          *(sCEs++) = sOrder;
        }
        sOrder &= 0xFFFF0000;
        sInShifted = FALSE;

        for(;;) {
          /*UCOL_GETNEXTCE(tOrder, coll, tColl, &status);*/
          tOrder = ucol_getNextCE(coll, &tColl, &status);*/
          tOrder ^= caseSwitch;
          if(tOrder == 0x00010101) {
            *(tCEs++) = tOrder;
            break;
          } else if((tOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(tOrder)) {
            if((tOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(tInShifted) {
                tOrder &= 0xFFFF0000;
              } else {
                *(tCEs++) = tOrder;
                break;
              }
            } else { /* Just lower level values */
              if(tInShifted) {
                continue;
              } 
            }
          } else { /* regular */
            if(tOrder > LVT) {
              *(tCEs++) = tOrder;
              break;
            } else {
              if((tOrder & 0xFFFF0000) > 0) {
                tInShifted = TRUE;
                tOrder &= 0xFFFF0000;
              }
            }
          }
          *(tCEs++) = tOrder;
        }
        tOrder &= 0xFFFF0000;
        tInShifted = FALSE;
#endif 
