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

void uprv_uca_reverseElement(UCAElements *el) {
    uint32_t i = 0;
    UChar temp;

    for(i = 0; i<el->cSize/2; i++) {
        temp = el->cPoints[i];
        el->cPoints[i] = el->cPoints[el->cSize-i-1];
        el->cPoints[el->cSize-i-1] = temp;
    }

#if 0
    /* Syn Wee does not need reversed expansions at all */
    UErrorCode status = U_ZERO_ERROR;
    uint32_t tempCE = 0, expansion = 0;
    if(el->noOfCEs>1) { /* this is an expansion that needs to be reversed and added - also, we need to change the mapValue */
    uint32_t buffer[256];
#if 0
      /* this is with continuations preserved */
      tempCE = el->CEs[0];
      i = 1;
      while(i<el->noOfCEs) {
        if(!isContinuation(el->CEs[i])) {
          buffer[el->noOfCEs-i] = tempCE;
        } else { /* it is continuation*/
          buffer[el->noOfCEs-i] = el->CEs[i];
          buffer[el->noOfCEs-i-1] = tempCE;
          i++;
        }
        if(i<el->noOfCEs) {
          tempCE = el->CEs[i];
          i++;
        }
      }
      if(i==el->noOfCEs) {
        buffer[0] = tempCE;
      }
      uprv_memcpy(el->CEs, buffer, el->noOfCEs*sizeof(uint32_t));
#endif
#if 0
      /* this is simple reversal */
      for(i = 0; i<el->noOfCEs/2; i++) {
          tempCE = el->CEs[i];
          el->CEs[i] = el->CEs[el->noOfCEs-i-1];
          el->CEs[el->noOfCEs-i-1] = tempCE;
      }
#endif
      expansion = UCOL_SPECIAL_FLAG | (EXPANSION_TAG<<UCOL_TAG_SHIFT) 
        | ((uprv_uca_addExpansion(expansions, el->CEs[0], &status)+(headersize>>2))<<4)
        & 0xFFFFF0;

      for(i = 1; i<el->noOfCEs; i++) {
        uprv_uca_addExpansion(expansions, el->CEs[i], &status);
      }
      if(el->noOfCEs <= 0xF) {
        expansion |= el->noOfCEs;
      } else {
        uprv_uca_addExpansion(expansions, 0, &status);
      }
      el->mapCE = expansion;
    }
#endif
}

int32_t uprv_uca_addExpansion(ExpansionTable *expansions, uint32_t value, UErrorCode *status) {
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

tempUCATable * uprv_uca_initTempTable(UCATableHeader *image, UColOptionSet *opts, const UCollator *UCA, UErrorCode *status) {
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
  t->mapping = ucmp32_open(UCOL_NOT_FOUND);
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
    uint16_t *index = (uint16_t *)uprv_malloc(sizeof(uint16_t)*t->mapping->fCount);
    int32_t *array = (int32_t *)uprv_malloc(sizeof(int32_t)*t->mapping->fCount);

    uprv_memcpy(array, t->mapping->fArray, t->mapping->fCount*sizeof(int32_t));
    uprv_memcpy(index, t->mapping->fIndex, UCMP32_kIndexCount*sizeof(uint16_t));

    r->mapping = ucmp32_openAdopt(index, array, t->mapping->fCount);
  }

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
  ucmp32_close(t->mapping);

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

uint32_t uprv_uca_addContraction(tempUCATable *t, uint32_t CE, UCAElements *element, UErrorCode *status) {
    uint32_t i = 0;

    for (i=1; i<element->cSize; i++) {   /* First add contraction chars to unsafe CP hash table */
        unsafeCPSet(t->unsafeCP, element->cPoints[i]);
    }
    // Add the last char of the contraction to the contraction-end hash table.
    ContrEndCPSet(t->contrEndCP, element->cPoints[element->cSize -1]);

    if(UCOL_ISJAMO(element->cPoints[0])) {
      t->image->jamoSpecial = TRUE;
    }

    /* then we need to deal with it */
    /* we could aready have something in table - or we might not */
    /* The fact is that we want to add or modify an existing contraction */
    /* and add it backwards then */
    uint32_t result = uprv_uca_processContraction(t->contractions, element, CE, status);
    if(CE == UCOL_NOT_FOUND || !isContraction(CE)) {
      ucmp32_set(t->mapping, element->cPoints[0], result);
    }

    return result;
}

/* This adds a read element, while testing for existence */
uint32_t uprv_uca_addAnElement(tempUCATable *t, UCAElements *element, UErrorCode *status) {
  CompactIntArray *mapping = t->mapping;
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
      expansion = UCOL_SPECIAL_FLAG | (THAI_TAG<<UCOL_TAG_SHIFT) 
        | ((uprv_uca_addExpansion(expansions, element->CEs[0], status)+(headersize>>2))<<4) 
        | 0x1;
      element->mapCE = expansion;
    }
  } else {     
    expansion = UCOL_SPECIAL_FLAG | (EXPANSION_TAG<<UCOL_TAG_SHIFT) 
      | ((uprv_uca_addExpansion(expansions, element->CEs[0], status)+(headersize>>2))<<4)
      & 0xFFFFF0;

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

  CE = ucmp32_get(mapping, element->cPoints[0]);

  if(element->cSize > 1) { /* we're adding a contraction */
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
    if( CE != UCOL_NOT_FOUND) {
        if(isContraction(CE)) { /* adding a non contraction element (thai, expansion, single) to already existing contraction */
            uprv_cnttab_setContraction(contractions, CE, 0, 0, element->mapCE, status);
            /* This loop has to change the CE at the end of contraction REDO!*/
            uprv_cnttab_changeLastCE(contractions, CE, element->mapCE, status);
        } else {
          ucmp32_set(mapping, element->cPoints[0], element->mapCE);
#ifdef UCOL_DEBUG
          fprintf(stderr, "Warning - trying to overwrite existing data %08X for cp %04X with %08X\n", CE, element->cPoints[0], element->CEs[0]);
          //*status = U_ILLEGAL_ARGUMENT_ERROR;
#endif
        }
    } else {
      ucmp32_set(mapping, element->cPoints[0], element->mapCE);
    }
  }


  return CE;
}

uint32_t uprv_uca_processContraction(CntTable *contractions, UCAElements *element, uint32_t existingCE, UErrorCode *status) {
    int32_t firstContractionOffset = 0;
    int32_t contractionOffset = 0;
    uint32_t contractionElement = UCOL_NOT_FOUND;

    if(U_FAILURE(*status)) {
        return UCOL_NOT_FOUND;
    }

    /* end of recursion */
    if(element->cSize == 1) {
      if(isContraction(existingCE)) {
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
    if(existingCE == UCOL_NOT_FOUND || !isContraction(existingCE)) { 
      /* if it wasn't contraction, we wouldn't end up here*/
      firstContractionOffset = uprv_cnttab_addContraction(contractions, UPRV_CNTTAB_NEWELEMENT, 0, existingCE, status);

      UChar toAdd = element->cPoints[1];
      element->cPoints++;
      element->cSize--;
      uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, status);
      element->cPoints--;
      element->cSize++;
      contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, toAdd, newCE, status);
      contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, 0xFFFF, existingCE, status);
      contractionElement =  constructContractCE(firstContractionOffset);
      return contractionElement;
    } else { /* we are adding to existing contraction */
      /* there were already some elements in the table, so we need to add a new contraction */
      /* Two things can happen here: either the codepoint is already in the table, or it is not */
      int32_t position = uprv_cnttab_findCP(contractions, existingCE, *(element->cPoints+1), status);
      element->cPoints++;
      element->cSize--;
      if(position > 0) {       /* if it is we just continue down the chain */
        uint32_t eCE = uprv_cnttab_getCE(contractions, existingCE, position, status);
        uint32_t newCE = uprv_uca_processContraction(contractions, element, eCE, status);
        uprv_cnttab_setContraction(contractions, existingCE, position, *(element->cPoints), newCE, status);
      } else {                  /* if it isn't, we will have to create a new sequence */
        uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, status);
        uprv_cnttab_insertContraction(contractions, existingCE, *(element->cPoints), newCE, status);
      }
      element->cPoints--;
      element->cSize++;
      return existingCE;
    }
}

void uprv_uca_getMaxExpansionJamo(CompactIntArray       *mapping, 
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
      ce = ucmp32_get(mapping, v);
      if (ce < UCOL_SPECIAL_FLAG) {
          uprv_uca_setMaxExpansion(ce, 2, maxexpansion, status);
      }
      v --;
  }

  while (t >= TBASE)
  {
      ce = ucmp32_get(mapping, t);
      if (ce < UCOL_SPECIAL_FLAG) {
          uprv_uca_setMaxExpansion(ce, 3, maxexpansion, status);
      }
      t --;
  }
  /*  According to the docs, 99% of the time, the Jamo will not be special */
  if (jamospecial) {
      /* gets the max expansion in all unicode characters */
      int     count    = maxjamoexpansion->position;
      uint8_t maxTSize = maxjamoexpansion->maxLSize + 
                         maxjamoexpansion->maxVSize +
                         maxjamoexpansion->maxTSize;
      uint8_t maxVSize = maxjamoexpansion->maxLSize + 
                         maxjamoexpansion->maxVSize;

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
    CompactIntArray *mapping = t->mapping;
    ExpansionTable *expansions = t->expansions;
    CntTable *contractions = t->contractions; 
    MaxExpansionTable *maxexpansion = t->maxExpansions;

    if(U_FAILURE(*status)) {
        return NULL;
    }

    uint32_t beforeContractions = (headersize+paddedsize(expansions->position*sizeof(uint32_t)))/sizeof(UChar);

    int32_t contractionsSize = 0;
    contractionsSize = uprv_cnttab_constructTable(contractions, beforeContractions, status);

    ucmp32_compact(mapping, 1);
    UMemoryStream *ms = uprv_mstrm_openNew(8192);
    int32_t mappingSize = ucmp32_flattenMem(mapping, ms);
    const uint8_t *flattened = uprv_mstrm_getBuffer(ms, &mappingSize);

    /* sets jamo expansions */
    uprv_uca_getMaxExpansionJamo(mapping, maxexpansion, t->maxJamoExpansions,
                                 t->image->jamoSpecial, status);

    uint32_t tableOffset = 0;
    uint8_t *dataStart;

    uint32_t toAllocate =           headersize+                                    
                                    paddedsize(expansions->position*sizeof(uint32_t))+
                                    paddedsize(mappingSize)+
                                    paddedsize(contractionsSize*(sizeof(UChar)+sizeof(uint32_t)))+
                                    paddedsize(0x100*sizeof(uint32_t))  
                                     /* maxexpansion array */
                                     + paddedsize(maxexpansion->position * sizeof(uint32_t)) +
                                     /* maxexpansion size array */
                                     paddedsize(maxexpansion->position * sizeof(uint8_t)) +
                                     paddedsize(UCOL_UNSAFECP_TABLE_SIZE) +   /*  Unsafe chars             */
                                     paddedsize(UCOL_UNSAFECP_TABLE_SIZE);    /*  Contraction Ending chars */


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

    tableOffset += paddedsize(sizeof(UCATableHeader));

    myData->options = tableOffset;
    memcpy(dataStart+tableOffset, t->options, sizeof(UColOptionSet));
    tableOffset += paddedsize(sizeof(UColOptionSet));

    /* copy expansions */
    /*myData->expansion = (uint32_t *)dataStart+tableOffset;*/
    myData->expansion = tableOffset;
    memcpy(dataStart+tableOffset, expansions->CEs, expansions->position*sizeof(uint32_t));
    tableOffset += paddedsize(expansions->position*sizeof(uint32_t));

    /* contractions block */
    if(contractionsSize != 0) {
      /* copy contraction index */
      /*myData->contractionIndex = (UChar *)(dataStart+tableOffset);*/
      myData->contractionIndex = tableOffset;
      memcpy(dataStart+tableOffset, contractions->codePoints, contractionsSize*sizeof(UChar));
      tableOffset += paddedsize(contractionsSize*sizeof(UChar));

      /* copy contraction collation elements */
      /*myData->contractionCEs = (uint32_t *)(dataStart+tableOffset);*/
      myData->contractionCEs = tableOffset;
      memcpy(dataStart+tableOffset, contractions->CEs, contractionsSize*sizeof(uint32_t));
      tableOffset += paddedsize(contractionsSize*sizeof(uint32_t));
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
        *(store++) = ucmp32_get(mapping,i);
        tableOffset+=sizeof(uint32_t);
    }

    /* copy max expansion table */
    myData->endExpansionCE      = tableOffset;
    myData->endExpansionCECount = maxexpansion->position;
    /* not copying the first element which is a dummy */
    uprv_memcpy(dataStart + tableOffset, maxexpansion->endExpansionCE + 1, 
                maxexpansion->position * sizeof(uint32_t));
    tableOffset += paddedsize(maxexpansion->position * sizeof(uint32_t));
    myData->expansionCESize = tableOffset;
    uprv_memcpy(dataStart + tableOffset, maxexpansion->expansionCESize + 1, 
                maxexpansion->position * sizeof(uint8_t));
    tableOffset += paddedsize(maxexpansion->position * sizeof(uint8_t));

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


