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
      high = (0xFF << (32-hibytes*8));
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

U_CFUNC uint32_t ucol_getFirstCE(const UCollator *coll, UChar u, UErrorCode *status) {
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
    uint32_t caseCE = ucol_getFirstCE(src->UCA, el.cPoints[0], status);
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

uint32_t ucol_getDynamicCEs(UColTokenParser *src, tempUCATable *t, UChar *decomp, uint32_t noOfDec, uint32_t *result, uint32_t resultSize, UErrorCode *status) {
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
        CE = ucol_getNextCE(src->UCA, &colIt, status);
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

  tempUCATable *t = uprv_uca_initTempTable(src->image, src->UCA, status);


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

