/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucaelems.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created 02/22/2001
*   created by: Vladimir Weinstein
*
*   This program reads the Franctional UCA table and generates
*   internal format for UCA table as well as inverse UCA table.
*   It then writes binary files containing the data: ucadata.dat 
*   & invuca.dat
* 
*   date        name       comments
*   03/02/2001  synwee     added setMaxExpansion
*   03/07/2001  synwee     merged UCA's maxexpansion and tailoring's
*/

#include "ucol_elm.h"
#include "unicode/uchar.h"

static uint32_t uprv_uca_processContraction(CntTable *contractions, UCAElements *element, uint32_t existingCE, UErrorCode *status);

static int32_t prefixLookupHash(const UHashKey e) {
  UCAElements *element = (UCAElements *)e.pointer;
  UHashKey key;
  key.pointer = element->cPoints;
  element->cPoints[element->cSize] = 0;
  return uhash_hashUChars(key);
}

static int8_t prefixLookupComp(const UHashKey e1, const UHashKey e2) {
  UCAElements *element1 = (UCAElements *)e1.pointer;
  UCAElements *element2 = (UCAElements *)e2.pointer;
  UHashKey key1;
  UHashKey key2;
  key1.pointer = element1->cPoints;
  key2.pointer = element2->cPoints;
  element1->cPoints[element1->cSize] = 0;
  element2->cPoints[element2->cSize] = 0;
  return uhash_compareUChars(key1, key2);
}

static void prefixLookupDeleter(void *element) {
  uprv_free(element);
}

static void uprv_uca_reverseElement(UCAElements *el) {
    uint32_t i = 0;
    UChar temp;

    for(i = 0; i<el->cSize/2; i++) {
        temp = el->cPoints[i];
        el->cPoints[i] = el->cPoints[el->cSize-i-1];
        el->cPoints[el->cSize-i-1] = temp;
    }
}

static int32_t uprv_uca_addExpansion(ExpansionTable *expansions, uint32_t value, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return 0;
    }
    if(expansions->CEs == NULL) {
        expansions->CEs = (uint32_t *)malloc(INIT_EXP_TABLE_SIZE*sizeof(uint32_t));
        expansions->size = INIT_EXP_TABLE_SIZE;
        expansions->position = 0;
    }

    if(expansions->position == expansions->size) {
        uint32_t *newData = (uint32_t *)realloc(expansions->CEs, 2*expansions->size*sizeof(uint32_t));
        if(newData == NULL) {
#ifdef UCOL_DEBUG
            fprintf(stderr, "out of memory for expansions\n");
#endif
            *status = U_MEMORY_ALLOCATION_ERROR;
            return -1;
        }
        expansions->CEs = newData;
        expansions->size *= 2;
    }

    expansions->CEs[expansions->position] = value;
    return(expansions->position++);
}

tempUCATable * uprv_uca_initTempTable(UCATableHeader *image, UColOptionSet *opts, const UCollator *UCA, UColCETags initTag, UErrorCode *status) {
  tempUCATable *t = (tempUCATable *)uprv_malloc(sizeof(tempUCATable));
  MaxExpansionTable *maxet  = (MaxExpansionTable *)uprv_malloc(
                                                   sizeof(MaxExpansionTable));
  MaxJamoExpansionTable *maxjet = (MaxJamoExpansionTable *)uprv_malloc(
                                               sizeof(MaxJamoExpansionTable));
  t->image = image;
  t->options = opts;

  t->UCA = UCA;
  t->expansions = (ExpansionTable *)uprv_malloc(sizeof(ExpansionTable));
  uprv_memset(t->expansions, 0, sizeof(ExpansionTable));
  t->mapping = ucmpe32_open(UCOL_SPECIAL_FLAG | (initTag<<24), UCOL_SPECIAL_FLAG | (SURROGATE_TAG<<24), status);
  t->prefixLookup = uhash_open(prefixLookupHash, prefixLookupComp, status);
  uhash_setValueDeleter(t->prefixLookup, prefixLookupDeleter);

  t->contractions = uprv_cnttab_open(t->mapping, status);

  /* copy UCA's maxexpansion and merge as we go along */
  t->maxExpansions       = maxet;
  if (UCA != NULL) {
    /* adding an extra initial value for easier manipulation */
    maxet->size            = (UCA->lastEndExpansionCE - UCA->endExpansionCE) 
                             + 2;
    maxet->position        = maxet->size - 1;
    maxet->endExpansionCE  = 
                      (uint32_t *)uprv_malloc(sizeof(uint32_t) * maxet->size);
    maxet->expansionCESize =
                        (uint8_t *)uprv_malloc(sizeof(uint8_t) * maxet->size);
    /* initialized value */
    *(maxet->endExpansionCE)  = 0;
    *(maxet->expansionCESize) = 0;
    uprv_memcpy(maxet->endExpansionCE + 1, UCA->endExpansionCE, 
                sizeof(uint32_t) * (maxet->size - 1));
    uprv_memcpy(maxet->expansionCESize + 1, UCA->expansionCESize, 
                sizeof(uint8_t) * (maxet->size - 1));
  }
  else {
    maxet->size     = 0;
  }
  t->maxJamoExpansions = maxjet;
  maxjet->endExpansionCE = NULL;
  maxjet->isV = NULL;
  maxjet->size = 0;
  maxjet->position = 0;
  maxjet->maxLSize = 1;
  maxjet->maxVSize = 1;
  maxjet->maxTSize = 1;

  t->unsafeCP = (uint8_t *)uprv_malloc(UCOL_UNSAFECP_TABLE_SIZE);
  t->contrEndCP = (uint8_t *)uprv_malloc(UCOL_UNSAFECP_TABLE_SIZE);
  uprv_memset(t->unsafeCP, 0, UCOL_UNSAFECP_TABLE_SIZE);
  uprv_memset(t->contrEndCP, 0, UCOL_UNSAFECP_TABLE_SIZE);
return t;
}

tempUCATable *uprv_uca_cloneTempTable(tempUCATable *t, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return NULL;
  }

  tempUCATable *r = (tempUCATable *)uprv_malloc(sizeof(tempUCATable));
  uprv_memset(r, 0, sizeof(tempUCATable));

  /* mapping */
  if(t->mapping != NULL) {
    r->mapping = ucmpe32_clone(t->mapping, status);
  }

  // a hashing clone function would be very nice. We have none currently...
  // However, we should be good, as closing should not produce any prefixed elements.
  r->prefixLookup = NULL; // prefixes are not used in closing

  /* expansions */
  if(t->expansions != NULL) {
    r->expansions = (ExpansionTable *)uprv_malloc(sizeof(ExpansionTable));
    r->expansions->position = t->expansions->position;
    r->expansions->size = t->expansions->size;
    if(t->expansions->CEs != NULL) {
      r->expansions->CEs = (uint32_t *)uprv_malloc(sizeof(uint32_t)*t->expansions->size);
      uprv_memcpy(r->expansions->CEs, t->expansions->CEs, sizeof(uint32_t)*t->expansions->size);
    } else {
      t->expansions->CEs = NULL;
    }
  }

  if(t->contractions != NULL) {
    r->contractions = uprv_cnttab_clone(t->contractions);
    r->contractions->mapping = r->mapping;
  }

  if(t->maxExpansions != NULL) {
    r->maxExpansions = (MaxExpansionTable *)uprv_malloc(sizeof(MaxExpansionTable));
    r->maxExpansions->size = t->maxExpansions->size;
    r->maxExpansions->position = t->maxExpansions->position;
    if(t->maxExpansions->endExpansionCE != NULL) {
      r->maxExpansions->endExpansionCE = (uint32_t *)uprv_malloc(sizeof(uint32_t)*t->maxExpansions->size);
      uprv_memcpy(r->maxExpansions->endExpansionCE, t->maxExpansions->endExpansionCE, t->maxExpansions->size*sizeof(uint32_t));
    } else {
      r->maxExpansions->endExpansionCE = NULL;
    }
    if(t->maxExpansions->expansionCESize != NULL) {
      r->maxExpansions->expansionCESize = (uint8_t *)uprv_malloc(sizeof(uint8_t)*t->maxExpansions->size);
      uprv_memcpy(r->maxExpansions->expansionCESize, t->maxExpansions->expansionCESize, t->maxExpansions->size*sizeof(uint8_t));
    } else {
      r->maxExpansions->expansionCESize = NULL;
    }
  }

  if(t->maxJamoExpansions != NULL) {
    r->maxJamoExpansions = (MaxJamoExpansionTable *)uprv_malloc(sizeof(MaxJamoExpansionTable));
    r->maxJamoExpansions->size = t->maxJamoExpansions->size;
    r->maxJamoExpansions->position = t->maxJamoExpansions->position;
    r->maxJamoExpansions->maxLSize = t->maxJamoExpansions->maxLSize;
    r->maxJamoExpansions->maxVSize = t->maxJamoExpansions->maxVSize;
    r->maxJamoExpansions->maxTSize = t->maxJamoExpansions->maxTSize;
    if(t->maxJamoExpansions->size != 0) {
      r->maxJamoExpansions->endExpansionCE = (uint32_t *)uprv_malloc(sizeof(uint32_t)*t->maxJamoExpansions->size);
      uprv_memcpy(r->maxJamoExpansions->endExpansionCE, t->maxJamoExpansions->endExpansionCE, t->maxJamoExpansions->size*sizeof(uint32_t));
      r->maxJamoExpansions->isV = (UBool *)uprv_malloc(sizeof(UBool)*t->maxJamoExpansions->size);
      uprv_memcpy(r->maxJamoExpansions->isV, t->maxJamoExpansions->isV, t->maxJamoExpansions->size*sizeof(UBool));
    } else {
      r->maxJamoExpansions->endExpansionCE = NULL;
      r->maxJamoExpansions->isV = NULL;
    }
  }

  if(t->unsafeCP != NULL) {
    r->unsafeCP = (uint8_t *)uprv_malloc(UCOL_UNSAFECP_TABLE_SIZE);
    uprv_memcpy(r->unsafeCP, t->unsafeCP, UCOL_UNSAFECP_TABLE_SIZE);
  }

  if(t->contrEndCP != NULL) {
    r->contrEndCP = (uint8_t *)uprv_malloc(UCOL_UNSAFECP_TABLE_SIZE);
    uprv_memcpy(r->contrEndCP, t->contrEndCP, UCOL_UNSAFECP_TABLE_SIZE);
  }

  r->UCA = t->UCA;
  r->image = t->image;
  r->options = t->options;

  return r;
}


void uprv_uca_closeTempTable(tempUCATable *t) {
  uprv_free(t->expansions->CEs);
  uprv_free(t->expansions);
  if(t->contractions != NULL) {
    uprv_cnttab_close(t->contractions);
  }
  ucmpe32_close(t->mapping);

  if(t->prefixLookup != NULL) {
    uhash_close(t->prefixLookup);
  }

  uprv_free(t->maxExpansions->endExpansionCE);
  uprv_free(t->maxExpansions->expansionCESize);
  uprv_free(t->maxExpansions);

  if (t->maxJamoExpansions->size > 0) {
    uprv_free(t->maxJamoExpansions->endExpansionCE);
    uprv_free(t->maxJamoExpansions->isV);
  }
  uprv_free(t->maxJamoExpansions);

  uprv_free(t->unsafeCP);
  uprv_free(t->contrEndCP);

  uprv_free(t);
}

/**
* Looks for the maximum length of all expansion sequences ending with the same
* collation element. The size required for maxexpansion and maxsize is 
* returned if the arrays are too small.
* @param endexpansion the last expansion collation element to be added
* @param expansionsize size of the expansion
* @param maxexpansion data structure to store the maximum expansion data.
* @param status error status
* @returns size of the maxexpansion and maxsize used.
*/
int uprv_uca_setMaxExpansion(uint32_t           endexpansion,
                             uint8_t            expansionsize,
                             MaxExpansionTable *maxexpansion,
                             UErrorCode        *status)
{
  if (maxexpansion->size == 0) {
    /* we'll always make the first element 0, for easier manipulation */
    maxexpansion->endExpansionCE = 
               (uint32_t *)uprv_malloc(INIT_EXP_TABLE_SIZE * sizeof(int32_t));
    *(maxexpansion->endExpansionCE) = 0;
    maxexpansion->expansionCESize =
               (uint8_t *)uprv_malloc(INIT_EXP_TABLE_SIZE * sizeof(uint8_t));
    *(maxexpansion->expansionCESize) = 0;
    maxexpansion->size     = INIT_EXP_TABLE_SIZE;
    maxexpansion->position = 0;
  }

  if (maxexpansion->position + 1 == maxexpansion->size) {
    uint32_t *neweece = (uint32_t *)uprv_realloc(maxexpansion->endExpansionCE, 
                                   2 * maxexpansion->size * sizeof(uint32_t));
    uint8_t  *neweces = (uint8_t *)uprv_realloc(maxexpansion->expansionCESize, 
                                    2 * maxexpansion->size * sizeof(uint8_t));
    if (neweece == NULL || neweces == NULL) {
#ifdef UCOL_DEBUG
      fprintf(stderr, "out of memory for maxExpansions\n");
#endif
      *status = U_MEMORY_ALLOCATION_ERROR;
      return -1;
    }
    maxexpansion->endExpansionCE  = neweece;
    maxexpansion->expansionCESize = neweces;
    maxexpansion->size *= 2;
  }

  uint32_t *pendexpansionce = maxexpansion->endExpansionCE;
  uint8_t  *pexpansionsize  = maxexpansion->expansionCESize;
  int      pos              = maxexpansion->position;

  uint32_t *start = pendexpansionce;
  uint32_t *limit = pendexpansionce + pos;

  /* using binary search to determine if last expansion element is 
     already in the array */
  uint32_t *mid;                                                        
  int       result = -1;
  while (start < limit - 1) {                                                
    mid = start + ((limit - start) >> 1);                                    
    if (endexpansion <= *mid) {                                                   
      limit = mid;                                                           
    }                                                                        
    else {                                                                   
      start = mid;                                                           
    }                                                                        
  } 
      
  if (*start == endexpansion) {                                                     
    result = start - pendexpansionce;  
  }                                                                          
  else                                                                       
    if (*limit == endexpansion) {                                                     
      result = limit - pendexpansionce;      
    }                                            
      
  if (result > -1) {
    /* found the ce in expansion, we'll just modify the size if it is 
       smaller */
    uint8_t *currentsize = pexpansionsize + result;
    if (*currentsize < expansionsize) {
      *currentsize = expansionsize;
    }
  }
  else {
    /* we'll need to squeeze the value into the array. 
       initial implementation. */
    /* shifting the subarray down by 1 */
    int      shiftsize     = (pendexpansionce + pos) - start;
    uint32_t *shiftpos     = start + 1;
    uint8_t  *sizeshiftpos = pexpansionsize + (shiftpos - pendexpansionce);
    
    /* okay need to rearrange the array into sorted order */
    if (shiftsize == 0 || *(pendexpansionce + pos) < endexpansion) {
      *(pendexpansionce + pos + 1) = endexpansion;
      *(pexpansionsize + pos + 1)  = expansionsize;
    }
    else {
      uprv_memmove(shiftpos + 1, shiftpos, shiftsize * sizeof(int32_t));
      uprv_memmove(sizeshiftpos + 1, sizeshiftpos, 
                                                shiftsize * sizeof(uint8_t));
      *shiftpos     = endexpansion;
      *sizeshiftpos = expansionsize;
    }
    maxexpansion->position ++;

#ifdef UCOL_DEBUG
    int   temp;
    UBool found = FALSE;
    for (temp = 0; temp < maxexpansion->position; temp ++) {
      if (pendexpansionce[temp] >= pendexpansionce[temp + 1]) {
        fprintf(stderr, "expansions %d\n", temp);
      }
      if (pendexpansionce[temp] == endexpansion) {
        found =TRUE;
        if (pexpansionsize[temp] < expansionsize) {
          fprintf(stderr, "expansions size %d\n", temp);
        }
      }
    }
    if (pendexpansionce[temp] == endexpansion) {
        found =TRUE;
        if (pexpansionsize[temp] < expansionsize) {
          fprintf(stderr, "expansions size %d\n", temp);
        }
      }
    if (!found)
      fprintf(stderr, "expansion not found %d\n", temp);
#endif
  }

  return maxexpansion->position;
}

/**
* Sets the maximum length of all jamo expansion sequences ending with the same
* collation element. The size required for maxexpansion and maxsize is 
* returned if the arrays are too small.
* @param ch the jamo codepoint
* @param endexpansion the last expansion collation element to be added
* @param expansionsize size of the expansion
* @param maxexpansion data structure to store the maximum expansion data.
* @param status error status
* @returns size of the maxexpansion and maxsize used.
*/
int uprv_uca_setMaxJamoExpansion(UChar                  ch,
                                 uint32_t               endexpansion,
                                 uint8_t                expansionsize,
                                 MaxJamoExpansionTable *maxexpansion,
                                 UErrorCode            *status)
{
  UBool isV = TRUE;
  if (((uint32_t)ch - 0x1100) <= (0x1112 - 0x1100)) {
      /* determines L for Jamo, doesn't need to store this since it is never
      at the end of a expansion */
      if (maxexpansion->maxLSize < expansionsize) {
          maxexpansion->maxLSize = expansionsize;
      }
      return maxexpansion->position;
  }

  if (((uint32_t)ch - 0x1161) <= (0x1175 - 0x1161)) {
      /* determines V for Jamo */
      if (maxexpansion->maxVSize < expansionsize) {
          maxexpansion->maxVSize = expansionsize;
      }
  }

  if (((uint32_t)ch - 0x11A8) <= (0x11C2 - 0x11A8)) {
      isV = FALSE;
      /* determines T for Jamo */
      if (maxexpansion->maxTSize < expansionsize) {
          maxexpansion->maxTSize = expansionsize;
      }
  }

  if (maxexpansion->size == 0) {
    /* we'll always make the first element 0, for easier manipulation */
    maxexpansion->endExpansionCE = 
               (uint32_t *)uprv_malloc(INIT_EXP_TABLE_SIZE * sizeof(uint32_t));
    *(maxexpansion->endExpansionCE) = 0;
    maxexpansion->isV = 
                 (UBool *)uprv_malloc(INIT_EXP_TABLE_SIZE * sizeof(UBool));
    *(maxexpansion->isV) = 0;
    maxexpansion->size     = INIT_EXP_TABLE_SIZE;
    maxexpansion->position = 0;
  }

  if (maxexpansion->position + 1 == maxexpansion->size) {
    uint32_t *neweece = (uint32_t *)uprv_realloc(maxexpansion->endExpansionCE, 
                                   2 * maxexpansion->size * sizeof(uint32_t));
    UBool    *newisV  = (UBool *)uprv_realloc(maxexpansion->isV, 
                                   2 * maxexpansion->size * sizeof(UBool));
    if (neweece == NULL || newisV == NULL) {
#ifdef UCOL_DEBUG
      fprintf(stderr, "out of memory for maxExpansions\n");
#endif
      *status = U_MEMORY_ALLOCATION_ERROR;
      return -1;
    }
    maxexpansion->endExpansionCE  = neweece;
    maxexpansion->isV             = newisV;
    maxexpansion->size *= 2;
  }

  uint32_t *pendexpansionce = maxexpansion->endExpansionCE;
  int       pos             = maxexpansion->position;

  while (pos > 0) {
      pos --;
      if (*(pendexpansionce + pos) == endexpansion) {
          return maxexpansion->position;
      }
  }

  *(pendexpansionce + maxexpansion->position) = endexpansion;
  *(maxexpansion->isV + maxexpansion->position) = isV;
  maxexpansion->position ++;
  
  return maxexpansion->position;
}


static void ContrEndCPSet(uint8_t *table, UChar c) {
    uint32_t    hash;
    uint8_t     *htByte;

    hash = c;
    if (hash >= UCOL_UNSAFECP_TABLE_SIZE*8) {
        hash = (hash & UCOL_UNSAFECP_TABLE_MASK) + 256;
    }
    htByte = &table[hash>>3];
    *htByte |= (1 << (hash & 7));
}


static void unsafeCPSet(uint8_t *table, UChar c) {
    uint32_t    hash;
    uint8_t     *htByte;

    hash = c;
    if (hash >= UCOL_UNSAFECP_TABLE_SIZE*8) {
        if (hash >= 0xd800 && hash <= 0xf8ff) {
            /*  Part of a surrogate, or in private use area.            */
            /*   These don't go in the table                            */
            return;
        }
        hash = (hash & UCOL_UNSAFECP_TABLE_MASK) + 256;
    }
    htByte = &table[hash>>3];
    *htByte |= (1 << (hash & 7));
}


/*  to the UnsafeCP hash table, add all chars with combining class != 0     */
void uprv_uca_unsafeCPAddCCNZ(tempUCATable *t) {
    UChar       c;
    for (c=0; c<0xffff; c++) {
        if (u_getCombiningClass(c) != 0)
            unsafeCPSet(t->unsafeCP, c);
    }
}

uint32_t uprv_uca_addPrefix(tempUCATable *t, uint32_t CE, 
                                 UCAElements *element, UErrorCode *status) {
  // currently the longest prefix we're supporting in Japanese is two characters
  // long. Although this table could quite easily mimic complete contraction stuff
  // there is no good reason to make a general solution, as it would require some 
  // error prone messing.
    CntTable *contractions = t->contractions;
    UChar32 cp;
    uint32_t cpsize = 0;
    UChar *oldCP = element->cPoints;
    uint32_t oldCPSize = element->cSize;


    contractions->currentTag = SPEC_PROC_TAG;

    // I'm quite unhappy with the two following loops, as they probably affect prefix analysis
    // in strcoll. Basically, if we have a contraction we add the starting contraction character
    // to the unsafe table, so that backward contraction skips it, as it has to pick the whole 
    // prefix, which won't happen if start is safe.
    uint32_t j = 0;
    //if(element->cSize > 1) {
      if(!(UTF_IS_TRAIL(element->cPoints[0]))) {
        unsafeCPSet(t->unsafeCP, element->cPoints[0]);
      }
    //}

    // The second loop I'm unhappy with as it increases the number of unsafe characters. 
    // Now, all the characters in a prefix are unsafe and that will pick the whole contraction,
    // and the prefixes for forward processing.
      
      if(!(UTF_IS_TRAIL(element->prefix[0]))) {
        unsafeCPSet(t->unsafeCP, element->prefix[0]);
      }
      // This should affect only prefixes larger than 3
      for (j=1; j<element->prefixSize-1; j++) {   /* First add contraction chars to unsafe CP hash table */
      // Unless it is a trail surrogate, which is handled algoritmically and 
      // shouldn't take up space in the table.
      if(!(UTF_IS_TRAIL(element->prefix[j]))) {
        unsafeCPSet(t->unsafeCP, element->prefix[j]);
      }
    }

    element->cPoints = element->prefix;
    element->cSize = element->prefixSize;

    // Add the last char of the contraction to the contraction-end hash table.
    // unless it is a trail surrogate, which is handled algorithmically and 
    // shouldn't be in the table
    if(!(UTF_IS_TRAIL(element->cPoints[element->cSize -1]))) {
      ContrEndCPSet(t->contrEndCP, element->cPoints[element->cSize -1]);
    }

    // First we need to check if contractions starts with a surrogate
    UTF_NEXT_CHAR(element->cPoints, cpsize, element->cSize, cp);

    // If there are any Jamos in the contraction, we should turn on special 
    // processing for Jamos
    if(UCOL_ISJAMO(element->prefix[0])) {
      t->image->jamoSpecial = TRUE;
    }
    /* then we need to deal with it */
    /* we could aready have something in table - or we might not */

    if(!isPrefix(CE)) { 
      /* if it wasn't contraction, we wouldn't end up here*/
      int32_t firstContractionOffset = 0;
      int32_t contractionOffset = 0;
      firstContractionOffset = uprv_cnttab_addContraction(contractions, UPRV_CNTTAB_NEWELEMENT, 0, CE, status);
      uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, status);
      contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, *element->prefix, newCE, status);
      contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, 0xFFFF, CE, status);
      CE =  constructContractCE(SPEC_PROC_TAG, firstContractionOffset);
    } else { /* we are adding to existing contraction */
      /* there were already some elements in the table, so we need to add a new contraction */
      /* Two things can happen here: either the codepoint is already in the table, or it is not */
      int32_t position = uprv_cnttab_findCP(contractions, CE, *element->prefix, status);
      if(position > 0) {       /* if it is we just continue down the chain */
        uint32_t eCE = uprv_cnttab_getCE(contractions, CE, position, status);
        uint32_t newCE = uprv_uca_processContraction(contractions, element, eCE, status);
        uprv_cnttab_setContraction(contractions, CE, position, *(element->prefix), newCE, status);
      } else {                  /* if it isn't, we will have to create a new sequence */
        uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, status);
        uprv_cnttab_insertContraction(contractions, CE, *(element->prefix), element->mapCE, status);
      }
    }

    element->cPoints = oldCP;
    element->cSize = oldCPSize;

    return CE;
}

// Note regarding surrogate handling: We are interested only in the single
// or leading surrogates in a contraction. If a surrogate is somewhere else
// in the contraction, it is going to be handled as a pair of code units,
// as it doesn't affect the performance AND handling surrogates specially
// would complicate code way too much.
uint32_t uprv_uca_addContraction(tempUCATable *t, uint32_t CE, 
                                 UCAElements *element, UErrorCode *status) {
    CntTable *contractions = t->contractions;
    UChar32 cp;
    uint32_t cpsize = 0;

    contractions->currentTag = CONTRACTION_TAG;

    // First we need to check if contractions starts with a surrogate
    UTF_NEXT_CHAR(element->cPoints, cpsize, element->cSize, cp);

    if(cpsize<element->cSize) { // This is a real contraction, if there are other characters after the first
      uint32_t j = 0;
      for (j=1; j<element->cSize; j++) {   /* First add contraction chars to unsafe CP hash table */
        // Unless it is a trail surrogate, which is handled algoritmically and 
        // shouldn't take up space in the table.
        if(!(UTF_IS_TRAIL(element->cPoints[j]))) {
          unsafeCPSet(t->unsafeCP, element->cPoints[j]);
        }
      }
      // Add the last char of the contraction to the contraction-end hash table.
      // unless it is a trail surrogate, which is handled algorithmically and 
      // shouldn't be in the table
      if(!(UTF_IS_TRAIL(element->cPoints[element->cSize -1]))) {
        ContrEndCPSet(t->contrEndCP, element->cPoints[element->cSize -1]);
      }

      // If there are any Jamos in the contraction, we should turn on special 
      // processing for Jamos
      if(UCOL_ISJAMO(element->cPoints[0])) {
        t->image->jamoSpecial = TRUE;
      }
      /* then we need to deal with it */
      /* we could aready have something in table - or we might not */
      element->cPoints+=cpsize;
      element->cSize-=cpsize;
      if(!isContraction(CE)) { 
        /* if it wasn't contraction, we wouldn't end up here*/
        int32_t firstContractionOffset = 0;
        int32_t contractionOffset = 0;
        firstContractionOffset = uprv_cnttab_addContraction(contractions, UPRV_CNTTAB_NEWELEMENT, 0, CE, status);
        uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, status);
        contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, *element->cPoints, newCE, status);
        contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, 0xFFFF, CE, status);
        CE =  constructContractCE(CONTRACTION_TAG, firstContractionOffset);
      } else { /* we are adding to existing contraction */
        /* there were already some elements in the table, so we need to add a new contraction */
        /* Two things can happen here: either the codepoint is already in the table, or it is not */
        int32_t position = uprv_cnttab_findCP(contractions, CE, *element->cPoints, status);
        if(position > 0) {       /* if it is we just continue down the chain */
          uint32_t eCE = uprv_cnttab_getCE(contractions, CE, position, status);
          uint32_t newCE = uprv_uca_processContraction(contractions, element, eCE, status);
          uprv_cnttab_setContraction(contractions, CE, position, *(element->cPoints), newCE, status);
        } else {                  /* if it isn't, we will have to create a new sequence */
          uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, status);
          uprv_cnttab_insertContraction(contractions, CE, *(element->cPoints), newCE, status);
        }
      }
      element->cPoints-=cpsize;
      element->cSize+=cpsize;
      ucmpe32_set(t->mapping, cp, CE);
    } else if(!isContraction(CE)) { /* this is just a surrogate, and there is no contraction */
      ucmpe32_set(t->mapping, cp, element->mapCE);
    } else { /* fill out the first stage of the contraction with the surrogate CE */
      uprv_cnttab_changeContraction(contractions, CE, 0, element->mapCE, status);
      uprv_cnttab_changeContraction(contractions, CE, 0xFFFF, element->mapCE, status);
    }
    return CE;
}


static uint32_t uprv_uca_processContraction(CntTable *contractions, UCAElements *element, uint32_t existingCE, UErrorCode *status) {
    int32_t firstContractionOffset = 0;
    int32_t contractionOffset = 0;
//    uint32_t contractionElement = UCOL_NOT_FOUND;

    if(U_FAILURE(*status)) {
        return UCOL_NOT_FOUND;
    }

    /* end of recursion */
    if(element->cSize == 1) {
      if(isCntTableElement(existingCE) && ((UColCETags)getCETag(existingCE) == contractions->currentTag)) {
        uprv_cnttab_changeContraction(contractions, existingCE, 0, element->mapCE, status);
        uprv_cnttab_changeContraction(contractions, existingCE, 0xFFFF, element->mapCE, status);
        return existingCE;
      } else {
        return element->mapCE; /*can't do just that. existingCe might be a contraction, meaning that we need to do another step */
      }
    }

    /* this recursion currently feeds on the only element we have... We will have to copy it in order to accomodate */
    /* for both backward and forward cycles */

    /* we encountered either an empty space or a non-contraction element */
    /* this means we are constructing a new contraction sequence */
    element->cPoints++;
    element->cSize--;
    if(!isCntTableElement(existingCE)) { 
      /* if it wasn't contraction, we wouldn't end up here*/
      firstContractionOffset = uprv_cnttab_addContraction(contractions, UPRV_CNTTAB_NEWELEMENT, 0, existingCE, status);
      uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, status);
      contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, *element->cPoints, newCE, status);
      contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, 0xFFFF, existingCE, status);
      existingCE =  constructContractCE(contractions->currentTag, firstContractionOffset);
    } else { /* we are adding to existing contraction */
      /* there were already some elements in the table, so we need to add a new contraction */
      /* Two things can happen here: either the codepoint is already in the table, or it is not */
      int32_t position = uprv_cnttab_findCP(contractions, existingCE, *element->cPoints, status);
      if(position > 0) {       /* if it is we just continue down the chain */
        uint32_t eCE = uprv_cnttab_getCE(contractions, existingCE, position, status);
        uint32_t newCE = uprv_uca_processContraction(contractions, element, eCE, status);
        uprv_cnttab_setContraction(contractions, existingCE, position, *(element->cPoints), newCE, status);
      } else {                  /* if it isn't, we will have to create a new sequence */
        uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, status);
        uprv_cnttab_insertContraction(contractions, existingCE, *(element->cPoints), newCE, status);
      }
    }
    element->cPoints--;
    element->cSize++;
    return existingCE;
}

/* Set a range of elements to a value */
uint32_t uprv_uca_setRange(tempUCATable *t, UChar32 rangeStart, UChar32 rangeEnd, int32_t value, UErrorCode *status) {
  if(U_FAILURE(*status) || (rangeEnd < rangeStart)) {
    return 0;
  }

  UChar32 counter = rangeStart;
  uint32_t i = 0;

  for(counter = rangeStart; counter <= rangeEnd; counter++) {
    ucmpe32_set32(t->mapping, counter, value);
    i++;
  }

  return i; 
}
/* This adds a read element, while testing for existence */
uint32_t uprv_uca_addAnElement(tempUCATable *t, UCAElements *element, UErrorCode *status) {
  CompactEIntArray *mapping = t->mapping;
  ExpansionTable *expansions = t->expansions;
  CntTable *contractions = t->contractions; 

  uint32_t i = 1;
  uint32_t expansion = 0;
  uint32_t CE;

  if(U_FAILURE(*status)) {
      return 0xFFFF;
  }
  if(element->noOfCEs == 1) {
    if(element->isThai == FALSE) {
      element->mapCE = element->CEs[0];
    } else { /* add thai - totally bad here */
      expansion = (uint32_t)(UCOL_SPECIAL_FLAG | (THAI_TAG<<UCOL_TAG_SHIFT) 
        | ((uprv_uca_addExpansion(expansions, element->CEs[0], status)+(headersize>>2))<<4) 
        | 0x1);
      element->mapCE = expansion;
    }
  } else {     
    expansion = (uint32_t)(UCOL_SPECIAL_FLAG | (EXPANSION_TAG<<UCOL_TAG_SHIFT) 
      | ((uprv_uca_addExpansion(expansions, element->CEs[0], status)+(headersize>>2))<<4)
      & 0xFFFFF0);

    for(i = 1; i<element->noOfCEs; i++) {
      uprv_uca_addExpansion(expansions, element->CEs[i], status);
    }
    if(element->noOfCEs <= 0xF) {
      expansion |= element->noOfCEs;
    } else {
      uprv_uca_addExpansion(expansions, 0, status);
    }
    element->mapCE = expansion;
    uprv_uca_setMaxExpansion(element->CEs[element->noOfCEs - 1],
                             (uint8_t)element->noOfCEs,
                             t->maxExpansions,
                             status);
    if(UCOL_ISJAMO(element->cPoints[0])) {
      t->image->jamoSpecial = TRUE;
      uprv_uca_setMaxJamoExpansion(element->cPoints[0],
                               element->CEs[element->noOfCEs - 1],
                               (uint8_t)element->noOfCEs,
                               t->maxJamoExpansions,
                               status);
    }
  }

  // here we want to add the prefix structure.
  // I will try to process it as a reverse contraction, if possible.
  // prefix buffer is already reversed.

  if(element->prefixSize!=0) {
    // This is CRAP! We cannot find the good CE unless go over contractions
    // Just the first CP will confuse single CPs and contractions.
    // if it is NOT_FOUND, that is more - less ok. However, it is 
    // problematic in other cases. Some sort of cacheing is required
    // WE CANNOT LOOK SIMPLY IN THE CE TABLE!
    // The current solution is to keep the added elements in a hashtable
    // keys would be codepoints, but we use the whole element as a key.
    // NOTE: hasher & comparer will zero terminate codepoints array.
    if(t->prefixLookup != NULL) {
      UCAElements *uCE = (UCAElements *)uhash_get(t->prefixLookup, element);
      if(uCE != NULL) { // there is already a set of code points here
        element->mapCE = uprv_uca_addPrefix(t, uCE->mapCE, element, status);
      } else { // no code points, so this spot is clean
        element->mapCE = uprv_uca_addPrefix(t, UCOL_NOT_FOUND, element, status);
        uCE = (UCAElements *)uprv_malloc(sizeof(UCAElements));
        uprv_memcpy(uCE, element, sizeof(UCAElements));
        uCE->cPoints = uCE->uchars;
        uhash_put(t->prefixLookup, uCE, uCE, status);
      }
    }
  }


  if(element->cSize > 1) { /* we're adding a contraction */
    uint32_t i = 0;
    UChar32 cp;

    UTF_NEXT_CHAR(element->cPoints, i, element->cSize, cp);
    CE = ucmpe32_get(mapping, cp);

    UCAElements *composed = (UCAElements *)uprv_malloc(sizeof(UCAElements));
    uprv_memcpy(composed, element, sizeof(UCAElements));
    composed->cPoints = composed->uchars;
    *composed->cPoints = *element->cPoints;
    composed->cSize = unorm_normalize(element->cPoints+1, element->cSize-1, UNORM_NFC, 0, composed->cPoints+1, 128, status);
    composed->cSize++;
    if(composed->cSize != element->cSize || uprv_memcmp(composed->cPoints+1, element->cPoints+1, element->cSize-1)) {
      // do it!
      CE = uprv_uca_addContraction(t, CE, composed, status);
#ifdef UCOL_DEBUG
      fprintf(stderr, "Adding composed for %04X\n", *element->cPoints);
#endif
    }
    uprv_free(composed);

    CE = uprv_uca_addContraction(t, CE, element, status);
  } else { /* easy case, */
    CE = ucmpe32_get(mapping, element->cPoints[0]);

    if( CE != UCOL_NOT_FOUND) {
      if(isCntTableElement(CE) /*isContraction(CE)*/) { /* adding a non contraction element (thai, expansion, single) to already existing contraction */
            uprv_cnttab_setContraction(contractions, CE, 0, 0, element->mapCE, status);
            /* This loop has to change the CE at the end of contraction REDO!*/
            uprv_cnttab_changeLastCE(contractions, CE, element->mapCE, status);
        } else {
          ucmpe32_set(mapping, element->cPoints[0], element->mapCE);
#ifdef UCOL_DEBUG
          fprintf(stderr, "Warning - trying to overwrite existing data %08X for cp %04X with %08X\n", CE, element->cPoints[0], element->CEs[0]);
          //*status = U_ILLEGAL_ARGUMENT_ERROR;
#endif
        }
    } else {
      ucmpe32_set(mapping, element->cPoints[0], element->mapCE);
    }
  }


  return CE;
}


void uprv_uca_getMaxExpansionJamo(CompactEIntArray       *mapping, 
                                  MaxExpansionTable     *maxexpansion,
                                  MaxJamoExpansionTable *maxjamoexpansion,
                                  UBool                  jamospecial,
                                  UErrorCode            *status)
{
  const uint32_t VBASE  = 0x1161;
  const uint32_t TBASE  = 0x11A8;
  const uint32_t VCOUNT = 21;
  const uint32_t TCOUNT = 28;
  
  uint32_t v = VBASE + VCOUNT - 1;
  uint32_t t = TBASE + TCOUNT - 1;
  uint32_t ce;

  while (v >= VBASE) {
      ce = ucmpe32_get(mapping, v);
      if (ce < UCOL_SPECIAL_FLAG) {
          uprv_uca_setMaxExpansion(ce, 2, maxexpansion, status);
      }
      v --;
  }

  while (t >= TBASE)
  {
      ce = ucmpe32_get(mapping, t);
      if (ce < UCOL_SPECIAL_FLAG) {
          uprv_uca_setMaxExpansion(ce, 3, maxexpansion, status);
      }
      t --;
  }
  /*  According to the docs, 99% of the time, the Jamo will not be special */
  if (jamospecial) {
      /* gets the max expansion in all unicode characters */
      int     count    = maxjamoexpansion->position;
      uint8_t maxTSize = (uint8_t)(maxjamoexpansion->maxLSize + 
                                   maxjamoexpansion->maxVSize +
                                   maxjamoexpansion->maxTSize);
      uint8_t maxVSize = (uint8_t)(maxjamoexpansion->maxLSize + 
                                   maxjamoexpansion->maxVSize);

      while (count > 0) {
          count --;
          if (*(maxjamoexpansion->isV + count) == TRUE) {
                uprv_uca_setMaxExpansion(
                                   *(maxjamoexpansion->endExpansionCE + count), 
                                   maxVSize, maxexpansion, status);
          }
          else {
                uprv_uca_setMaxExpansion(
                                   *(maxjamoexpansion->endExpansionCE + count), 
                                   maxTSize, maxexpansion, status);
          }
      }
  }
}


UCATableHeader *uprv_uca_assembleTable(tempUCATable *t, UErrorCode *status) {
    CompactEIntArray *mapping = t->mapping;
    ExpansionTable *expansions = t->expansions;
    CntTable *contractions = t->contractions;
    MaxExpansionTable *maxexpansion = t->maxExpansions;

    if(U_FAILURE(*status)) {
        return NULL;
    }

    uint32_t beforeContractions = (uint32_t)((headersize+paddedsize(expansions->position*sizeof(uint32_t)))/sizeof(UChar));

    int32_t contractionsSize = 0;
    contractionsSize = uprv_cnttab_constructTable(contractions, beforeContractions, status);

    ucmpe32_compact(mapping);
    UMemoryStream *ms = uprv_mstrm_openNew(8192);
    int32_t mappingSize = ucmpe32_flattenMem(mapping, ms);
    const uint8_t *flattened = uprv_mstrm_getBuffer(ms, &mappingSize);

    /* sets jamo expansions */
    uprv_uca_getMaxExpansionJamo(mapping, maxexpansion, t->maxJamoExpansions,
                                 t->image->jamoSpecial, status);

    uint32_t tableOffset = 0;
    uint8_t *dataStart;

    uint32_t toAllocate =(uint32_t)(headersize+                                    
                                    paddedsize(expansions->position*sizeof(uint32_t))+
                                    paddedsize(mappingSize)+
                                    paddedsize(contractionsSize*(sizeof(UChar)+sizeof(uint32_t)))+
                                    paddedsize(0x100*sizeof(uint32_t))  
                                     /* maxexpansion array */
                                     + paddedsize(maxexpansion->position * sizeof(uint32_t)) +
                                     /* maxexpansion size array */
                                     paddedsize(maxexpansion->position * sizeof(uint8_t)) +
                                     paddedsize(UCOL_UNSAFECP_TABLE_SIZE) +   /*  Unsafe chars             */
                                     paddedsize(UCOL_UNSAFECP_TABLE_SIZE));    /*  Contraction Ending chars */


    dataStart = (uint8_t *)malloc(toAllocate);

    UCATableHeader *myData = (UCATableHeader *)dataStart;
    uprv_memcpy(myData, t->image, sizeof(UCATableHeader));

#if 0
    /* above memcpy should save us from problems */
    myData->variableTopValue = t->image->variableTopValue;
    myData->strength = t->image->strength;
    myData->frenchCollation = t->image->frenchCollation;
    myData->alternateHandling = t->image->alternateHandling; /* attribute for handling variable elements*/
    myData->caseFirst = t->image->caseFirst;         /* who goes first, lower case or uppercase */
    myData->caseLevel = t->image->caseLevel;         /* do we have an extra case level */
    myData->normalizationMode = t->image->normalizationMode; /* attribute for normalization */
    myData->version[0] = t->image->version[0];
    myData->version[1] = t->image->version[1];  
#endif

    myData->contractionSize = contractionsSize;

    tableOffset += (uint32_t)(paddedsize(sizeof(UCATableHeader)));

    myData->options = tableOffset;
    memcpy(dataStart+tableOffset, t->options, sizeof(UColOptionSet));
    tableOffset += (uint32_t)(paddedsize(sizeof(UColOptionSet)));

    /* copy expansions */
    /*myData->expansion = (uint32_t *)dataStart+tableOffset;*/
    myData->expansion = tableOffset;
    memcpy(dataStart+tableOffset, expansions->CEs, expansions->position*sizeof(uint32_t));
    tableOffset += (uint32_t)(paddedsize(expansions->position*sizeof(uint32_t)));

    /* contractions block */
    if(contractionsSize != 0) {
      /* copy contraction index */
      /*myData->contractionIndex = (UChar *)(dataStart+tableOffset);*/
      myData->contractionIndex = tableOffset;
      memcpy(dataStart+tableOffset, contractions->codePoints, contractionsSize*sizeof(UChar));
      tableOffset += (uint32_t)(paddedsize(contractionsSize*sizeof(UChar)));

      /* copy contraction collation elements */
      /*myData->contractionCEs = (uint32_t *)(dataStart+tableOffset);*/
      myData->contractionCEs = tableOffset;
      memcpy(dataStart+tableOffset, contractions->CEs, contractionsSize*sizeof(uint32_t));
      tableOffset += (uint32_t)(paddedsize(contractionsSize*sizeof(uint32_t)));
    } else {
      myData->contractionIndex = 0;
      myData->contractionIndex = 0;
    }

    /* copy mapping table */
    /*myData->mappingPosition = dataStart+tableOffset;*/
    myData->mappingPosition = tableOffset;
    memcpy(dataStart+tableOffset, flattened, mappingSize);
    tableOffset += paddedsize(mappingSize);

    /* construct the fast tracker for latin one*/
    myData->latinOneMapping = tableOffset;
    uint32_t *store = (uint32_t*)(dataStart+tableOffset);
    int32_t i = 0;
    for(i = 0; i<=0xFF; i++) {
        *(store++) = ucmpe32_get(mapping,i);
        tableOffset+=(uint32_t)(sizeof(uint32_t));
    }

    /* copy max expansion table */
    myData->endExpansionCE      = tableOffset;
    myData->endExpansionCECount = maxexpansion->position;
    /* not copying the first element which is a dummy */
    uprv_memcpy(dataStart + tableOffset, maxexpansion->endExpansionCE + 1, 
                maxexpansion->position * sizeof(uint32_t));
    tableOffset += (uint32_t)(paddedsize(maxexpansion->position * sizeof(uint32_t)));
    myData->expansionCESize = tableOffset;
    uprv_memcpy(dataStart + tableOffset, maxexpansion->expansionCESize + 1, 
                maxexpansion->position * sizeof(uint8_t));
    tableOffset += (uint32_t)(paddedsize(maxexpansion->position * sizeof(uint8_t)));

    /* Unsafe chars table.  Finish it off, then copy it. */
    uprv_uca_unsafeCPAddCCNZ(t);
    if (t->UCA != 0) {              /* Or in unsafebits from UCA, making a combined table.    */
       for (i=0; i<UCOL_UNSAFECP_TABLE_SIZE; i++) {    
           t->unsafeCP[i] |= t->UCA->unsafeCP[i];
       }
    }
    myData->unsafeCP = tableOffset;
    uprv_memcpy(dataStart + tableOffset, t->unsafeCP, UCOL_UNSAFECP_TABLE_SIZE);
    tableOffset += paddedsize(UCOL_UNSAFECP_TABLE_SIZE);


    /* Finish building Contraction Ending chars hash table and then copy it out.  */
    if (t->UCA != 0) {              /* Or in unsafebits from UCA, making a combined table.    */
        for (i=0; i<UCOL_UNSAFECP_TABLE_SIZE; i++) {    
            t->contrEndCP[i] |= t->UCA->contrEndCP[i];
        }
    }
    myData->contrEndCP = tableOffset;
    uprv_memcpy(dataStart + tableOffset, t->contrEndCP, UCOL_UNSAFECP_TABLE_SIZE);
    tableOffset += paddedsize(UCOL_UNSAFECP_TABLE_SIZE);

    if(tableOffset != toAllocate) {
#ifdef UCOL_DEBUG
        fprintf(stderr, "calculation screwup!!! Expected to write %i but wrote %i instead!!!\n", toAllocate, tableOffset);
#endif
        *status = U_INTERNAL_PROGRAM_ERROR;
        free(dataStart);
        return 0;
    }

    myData->size = tableOffset;
    /* This should happen upon ressurection */
    /*const uint8_t *mapPosition = (uint8_t*)myData+myData->mappingPosition;*/
    uprv_mstrm_close(ms);
    return myData;
}


