/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucol_tok.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created 02/22/2001
*   created by: Vladimir Weinstein
*
* This module builds a collator based on the rule set.
* 
*/

#include "ucol_bld.h"
/* checkout this one - it might be replaceable by something faster */
#include "dcmpdata.h"

static const InverseTableHeader* invUCA = NULL;


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

U_CAPI int32_t U_EXPORT2 ucol_inv_getNextCE(uint32_t CE, uint32_t contCE, 
                                            uint32_t *nextCE, uint32_t *nextContCE, 
                                            uint32_t strength) {
  uint32_t *CETable = (uint32_t *)((uint8_t *)invUCA+invUCA->table);
  int32_t iCE;

  iCE = ucol_inv_findCE(CE, contCE);

  if(iCE<0) {
    *nextCE = UCOL_NOT_FOUND;
    return -1;
  }

  CE &= strengthMask[strength];
  contCE &= strengthMask[strength];

  *nextCE = CE;
  *nextContCE = contCE;

  while((*nextCE  & strengthMask[strength]) == CE 
    && (*nextContCE  & strengthMask[strength]) == contCE) {
    *nextCE = (*(CETable+3*(++iCE)));
    *nextContCE = (*(CETable+3*(iCE)+1));
  }

  return iCE;
}

U_CAPI int32_t U_EXPORT2 ucol_inv_getPrevCE(uint32_t CE, uint32_t contCE, 
                                            uint32_t *prevCE, uint32_t *prevContCE, 
                                            uint32_t strength) {
  uint32_t *CETable = (uint32_t *)((uint8_t *)invUCA+invUCA->table);
  int32_t iCE;

  iCE = ucol_inv_findCE(CE, contCE);

  if(iCE<0) {
    *prevCE = UCOL_NOT_FOUND;
    return -1;
  }

  CE &= strengthMask[strength];
  contCE &= strengthMask[strength];

  *prevCE = CE;
  *prevContCE = contCE;

  while((*prevCE  & strengthMask[strength]) == CE 
    && (*prevContCE  & strengthMask[strength])== contCE) {
    *prevCE = (*(CETable+3*(--iCE)));
    *prevContCE = (*(CETable+3*(iCE)+1));
  }

  return iCE;
}

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

U_CFUNC uint32_t ucol_getNextGenerated(ucolCEGenerator *g, UErrorCode *status) {
  if(U_SUCCESS(*status)) {
  g->current = ucol_nextWeight(g->ranges, &g->noOfRanges);
  }
  return g->current;
}

static uint32_t fbHigh[3] = {0, /*0,*/UCOL_COMMON_TOP2, 0};
static uint32_t fbLow[3] = {0, /*0,*/UCOL_COMMON_BOT2, 0};

U_CFUNC uint32_t ucol_getSimpleCEGenerator(ucolCEGenerator *g, UColToken *tok, uint32_t strength, UErrorCode *status) {

  uint32_t high, low, count=1;

  if(strength == UCOL_SECONDARY) {
    low = UCOL_COMMON_TOP2<<24;
    high = 0xFFFFFFFF;
    count = 0xFF - UCOL_COMMON_TOP2;
  } else {
    low = 0x03000000;
    high = 0x40000000;
    count = 0x40 - 0x30;
  }

  if(tok->next != NULL && tok->next->strength == strength) {
    count = tok->next->toInsert;
  } 

  g->noOfRanges = ucol_allocWeights(low, high, count, g->ranges);
  g->current = 0x03000000;

  if(g->noOfRanges == 0) {
    *status = U_INTERNAL_PROGRAM_ERROR;
  }
  return g->current;
}

U_CFUNC uint32_t ucol_getCEGenerator(ucolCEGenerator *g, uint32_t* lows, uint32_t* highs, UColToken *tok, uint32_t fStrength, UErrorCode *status) {
  uint32_t strength = tok->strength;
  uint32_t low = lows[fStrength*3+strength];
  uint32_t high = highs[fStrength*3+strength];

  uint32_t count = tok->toInsert+(fbHigh[strength]-fbLow[strength]);

  if(low == high && strength > UCOL_PRIMARY) {
    int32_t s = strength;
    for(;;) {
      s--;
      if(lows[fStrength*3+s] != highs[fStrength*3+s]) {
        if(strength == UCOL_SECONDARY) {
          low = UCOL_COMMON_TOP2<<24;
          high = 0xFFFFFFFF;
        } else {
          low = 0x02000000;
          high = 0x40000000;
        }
        break;
      }
      if(s<0) {
        *status = U_INTERNAL_PROGRAM_ERROR;
        return 0;
      }
    }
  } 

  if(low == 0) {
    low = 0x01000000;
  }

  if(strength == UCOL_SECONDARY) { /* similar as simple */
    if(low >= UCOL_COMMON_BOT2<<24 && low < UCOL_COMMON_TOP2<<24) {
      low = UCOL_COMMON_TOP2<<24;
    }
    if(high > UCOL_COMMON_BOT2<<24 && high < UCOL_COMMON_TOP2<<24) {
      high = UCOL_COMMON_TOP2<<24;
    } 
    if(low < UCOL_COMMON_BOT2<<24) {
      g->noOfRanges = ucol_allocWeights(UCOL_COMMON_TOP2<<24, high, count, g->ranges);
      g->current = UCOL_COMMON_BOT2;
      return g->current;
    }
  } 

  g->noOfRanges = ucol_allocWeights(low, high, count, g->ranges);
  if(g->noOfRanges == 0) {
    *status = U_INTERNAL_PROGRAM_ERROR;
  }
  g->current = ucol_nextWeight(g->ranges, &g->noOfRanges);
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
  ucol_inv_getGapPositions(lh);

#if UCOL_DEBUG
  fprintf(stderr, "BaseCE: %08X %08X\n", lh->baseCE, lh->baseContCE);
  int32_t j = 2;
  for(j = 2; j >= 0; j--) {
    fprintf(stderr, "gapsLo[%i] [%08X %08X %08X]\n", j, lh->gapsLo[j*3], lh->gapsLo[j*3+1], lh->gapsLo[j*3+2]);
    fprintf(stderr, "gapsHi[%i] [%08X %08X %08X]\n", j, lh->gapsHi[j*3], lh->gapsHi[j*3+1], lh->gapsHi[j*3+2]);
  }
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
#endif

  tok = lh->first[UCOL_TOK_POLARITY_POSITIVE];
  uint32_t fStrength = UCOL_IDENTICAL;
  uint32_t initStrength = UCOL_IDENTICAL;


  CEparts[UCOL_PRIMARY] = (lh->baseCE & UCOL_PRIMARYMASK) | (lh->baseContCE & UCOL_PRIMARYMASK) >> 16;
  CEparts[UCOL_SECONDARY] = (lh->baseCE & UCOL_SECONDARYMASK) << 16 | (lh->baseContCE & UCOL_SECONDARYMASK) << 8;
  CEparts[UCOL_TERTIARY] = (UCOL_TERTIARYORDER(lh->baseCE)) << 24 | (UCOL_TERTIARYORDER(lh->baseContCE)) << 16;

  while (tok != NULL && U_SUCCESS(*status)) {
    fStrength = tok->strength;
    if(fStrength < initStrength) {
      initStrength = fStrength;
      if(lh->pos[fStrength] == -1) {
        while(lh->pos[fStrength] == -1 && fStrength > 0) {
          fStrength--;
        }
        if(lh->pos[fStrength] == -1) {
          *status = U_INTERNAL_PROGRAM_ERROR;
          return;
        }
      }
      if(initStrength == UCOL_TERTIARY) { /* starting with tertiary */
        CEparts[UCOL_PRIMARY] = lh->gapsLo[fStrength*3];
        CEparts[UCOL_SECONDARY] = lh->gapsLo[fStrength*3+1];
        /*CEparts[UCOL_TERTIARY] = ucol_getCEGenerator(&Gens[2], lh->gapsLo[fStrength*3+2], lh->gapsHi[fStrength*3+2], tok, UCOL_TERTIARY); */
        CEparts[UCOL_TERTIARY] = ucol_getCEGenerator(&Gens[UCOL_TERTIARY], lh->gapsLo, lh->gapsHi, tok, fStrength, status); 
      } else if(initStrength == UCOL_SECONDARY) { /* secondaries */
        CEparts[UCOL_PRIMARY] = lh->gapsLo[fStrength*3];
        /*CEparts[1] = ucol_getCEGenerator(&Gens[1], lh->gapsLo[fStrength*3+1], lh->gapsHi[fStrength*3+1], tok, 1);*/
        CEparts[UCOL_SECONDARY] = ucol_getCEGenerator(&Gens[UCOL_SECONDARY], lh->gapsLo, lh->gapsHi, tok, fStrength,  status);
        CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[UCOL_TERTIARY], tok, UCOL_TERTIARY, status);
      } else { /* primaries */
        /*CEparts[UCOL_PRIMARY] = ucol_getCEGenerator(&Gens[0], lh->gapsLo[0], lh->gapsHi[0], tok, UCOL_PRIMARY);*/
        CEparts[UCOL_PRIMARY] = ucol_getCEGenerator(&Gens[UCOL_PRIMARY], lh->gapsLo, lh->gapsHi, tok, fStrength,  status);
        CEparts[UCOL_SECONDARY] = ucol_getSimpleCEGenerator(&Gens[UCOL_SECONDARY], tok, UCOL_SECONDARY, status);
        CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[UCOL_TERTIARY], tok, UCOL_TERTIARY, status);
      }
    } else {
      if(tok->strength == UCOL_TERTIARY) {
        CEparts[UCOL_TERTIARY] = ucol_getNextGenerated(&Gens[UCOL_TERTIARY], status);
      } else if(tok->strength == UCOL_SECONDARY) {
        CEparts[UCOL_SECONDARY] = ucol_getNextGenerated(&Gens[UCOL_SECONDARY], status);
        CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[UCOL_TERTIARY], tok, UCOL_TERTIARY, status);
      } else if(tok->strength == UCOL_PRIMARY) {
        CEparts[UCOL_PRIMARY] = ucol_getNextGenerated(&Gens[UCOL_PRIMARY], status);
        CEparts[UCOL_SECONDARY] = ucol_getSimpleCEGenerator(&Gens[UCOL_SECONDARY], tok, UCOL_SECONDARY, status);
        CEparts[UCOL_TERTIARY] = ucol_getSimpleCEGenerator(&Gens[UCOL_TERTIARY], tok, UCOL_TERTIARY, status);
      }
    }
    ucol_doCE(CEparts, tok, tailored, status);
    tok = tok->next;
  }
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
        init_collIterate(src->UCA, source, len, &s, FALSE);

        for(;;) {
          UCOL_GETNEXTCE(order, src->UCA, s, status);
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

    UChar buff[128];
    uint32_t decompSize;
    uprv_memcpy(buff, (tok->source & 0x00FFFFFF) + src->source, (tok->source >> 24)*sizeof(UChar));
    decompSize = unorm_normalize(buff, tok->source >> 24, UNORM_NFD, 0, el.uchars, 128, status);
    el.cSize = decompSize; /*(tok->source >> 24); *//* + (tok->expansion >> 24);*/
    el.cPoints = el.uchars;

    if(UCOL_ISTHAIPREVOWEL(el.cPoints[0])) {
      el.isThai = TRUE;
    } else {
      el.isThai = FALSE;
    }

    if(src->UCA != NULL) {
      for(i = 0; i<el.cSize; i++) {
        if(UCOL_ISJAMO(el.cPoints[i])) {
          t->image->jamoSpecial = TRUE;
        }
      }
    }

    /* we also need a case bit here, and we'll fish it out from the UCA for the first codepoint */
    uint32_t caseCE = ucol_getFirstCE(src->UCA, el.cPoints[0], status);
    if((caseCE & 0x40) != 0) {
      el.caseBit = TRUE;
/*      for(i = 0; i<el.noOfCEs; i++) {*/
/* we don't want to change the case of expansion CEs */
      for(i = 0; i<tok->noOfCEs; i++) {
        el.CEs[i] |= 0x40;
      }
    } else {
      el.caseBit = FALSE;
/*      for(i = 0; i<el.noOfCEs; i++) {*/
/* we don't want to change the case of expansion CEs */
      for(i = 0; i<tok->noOfCEs; i++) {
        el.CEs[i] &= 0xFFFFFFBF;
      }
    }


    /* and then, add it */
#if UCOL_DEBUG==2
    fprintf(stderr, "Adding: %04X with %08X\n", el.cPoints[0], el.CEs[0]);
#endif
    uprv_uca_addAnElement(t, &el, status);
#if UCOL_DEBUG_DUPLICATES
    if(*status != U_ZERO_ERROR) {
      fprintf(stderr, "replaced CE for %04X with CE for %04X\n", el.cPoints[0], tok->debugSource);
      *status = U_ZERO_ERROR;
    }
#endif

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
            resSize = uprv_fillOutputString(res, result, 356, &status);

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

uint32_t ucol_getDynamicCEs(UColTokenParser *src, tempUCATable *t, UChar *decomp, uint32_t noOfDec, uint32_t *result, uint32_t resultSize, UErrorCode *status) {
  uint32_t j = 0, i = 0;
  uint32_t CE = 0, firstFound = UCOL_NOT_FOUND;
  uint32_t firstIndex = 0;
  uint32_t resLen = 0;
  collIterate colIt;
  UBool lastNotFound = FALSE;

  
  while(j<noOfDec) {
    CE = ucmp32_get(t->mapping, decomp[j]);
    if(CE == UCOL_NOT_FOUND || lastNotFound) { /* get it from the UCA */
      if(lastNotFound) {
        lastNotFound = FALSE;
        j = firstIndex;
      }
      if(firstFound == UCOL_NOT_FOUND) {
        init_collIterate(src->UCA, decomp+j, 1, &colIt, TRUE);
        while(CE != UCOL_NO_MORE_CES) {
          CE = ucol_getNextCE(src->UCA, &colIt, status);
          if(CE != UCOL_NO_MORE_CES) {
            result[resLen++] = CE;
          }
        }
      } else { /* there was some stuff found in contraction */
        result[resLen++] = firstFound;
        j = firstIndex;
        firstFound = UCOL_NOT_FOUND;
        //firstIndex = 0;
        continue;
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
              for(i = 0; i<size; i++) {
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
              if((tag = getCETag(CE)) == CONTRACTION_TAG) { 
              /* this is tricky - we're not closed, so for Japanese, */
              /*  we want to record the first success */
              /* i.e. 0x30D0 decomposes to 0x30CF 0x3099 */
              /* 0x30CF is contraction in table */
              /* there are no 0x30CF 0x3099 in table, but there are   */
              /* longer contractions. If we don't note that we're already */
              /* had something, we'll return not found and pick the wrong */
              /* guys from UCA. I think getComplicatedCE needs to be checked */
              /* for this type of error */
                if(ctb->CEs[0] != UCOL_NOT_FOUND) {
                  firstFound = ctb->CEs[0];
                  firstIndex = j-1;
                }
              }
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
    if(!lastNotFound) {
      firstIndex = j;
    }
  }
  return resLen;
}
  
UCATableHeader *ucol_assembleTailoringTable(UColTokenParser *src, UErrorCode *status) {
  uint32_t i = 0;
  if(U_FAILURE(*status)) {
    return NULL;
  }
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
  UCATableHeader *image = (UCATableHeader *)uprv_malloc(sizeof(UCATableHeader));
  uprv_memcpy(image, src->UCA->image, sizeof(UCATableHeader));

  for(i = 0; i<src->resultLen; i++) {
    /* now we need to generate the CEs */ 
    /* We stuff the initial value in the buffers, and increase the appropriate buffer */
    /* According to strength                                                          */
    if(U_SUCCESS(*status)) {
      ucol_initBuffers(&src->lh[i], tailored, status);
    }
  }

  tempUCATable *t = uprv_uca_initTempTable(image, src->opts, src->UCA, status);


  /* After this, we have assigned CE values to all regular CEs      */
  /* now we will go through list once more and resolve expansions,  */
  /* make UCAElements structs and add them to table                 */
  for(i = 0; i<src->resultLen; i++) {
    /* now we need to generate the CEs */ 
    /* We stuff the initial value in the buffers, and increase the appropriate buffer */
    /* According to strength                                                          */
    if(U_SUCCESS(*status)) {
      ucol_createElements(src, t, &src->lh[i], tailored, status);
    }
  }

  UCATableHeader *myData = NULL;
  {
    UChar decomp[256];
    uint32_t noOfDec = 0, CE = UCOL_NOT_FOUND;
    UChar u = 0;
    UCAElements el;
    el.isThai = FALSE;
    collIterate colIt;
    /*uint32_t decompCE[256];*/
    uint32_t compCE[256];
    uint32_t compRes = 0;

    if(U_SUCCESS(*status)) {
      /* produce canonical closure */
      for(u = 0; u < 0xFFFF; u++) {
        if((noOfDec = unorm_normalize(&u, 1, UNORM_NFD, 0, decomp, 256, status)) > 1
          || (noOfDec == 1 && *decomp != (UChar)u))
        /*if((noOfDec = uprv_ucol_decompose ((UChar)u, decomp)) > 1 || (noOfDec == 1 && *decomp != (UChar)u))*/
        {
          compRes = ucol_getDynamicCEs(src, t, (UChar *)&u, 1, compCE, 256, status);
          el.noOfCEs = ucol_getDynamicCEs(src, t, decomp, noOfDec, el.CEs, 128, status);

          if((compRes != el.noOfCEs) || (uprv_memcmp(compCE, el.CEs, compRes*sizeof(uint32_t)) != 0)) {
              el.uchars[0] = (UChar)u;
              el.cPoints = el.uchars;
              el.cSize = 1;

              uprv_uca_addAnElement(t, &el, status);
          }
        }
      }
    }

    /* still need to produce compatibility closure */

  /* add latin-1 stuff */
    if(U_SUCCESS(*status)) {
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
          init_collIterate(src->UCA, decomp, 1, &colIt, TRUE);
          while(CE != UCOL_NO_MORE_CES) {
            CE = ucol_getNextCE(src->UCA, &colIt, status);
            /*UCOL_GETNEXTCE(CE, temp, colIt, status);*/
            if(CE != UCOL_NO_MORE_CES) {
              el.CEs[el.noOfCEs++] = CE;
            }
          }
          uprv_uca_addAnElement(t, &el, status);
        }
      }
    }
  }

  myData = uprv_uca_assembleTable(t, status);  

  uhash_close(tailored);
  uprv_uca_closeTempTable(t);    

  return myData;
}

const InverseTableHeader *ucol_initInverseUCA(UErrorCode *status) {
  if(U_FAILURE(*status)) return NULL;

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
  return invUCA;
}

