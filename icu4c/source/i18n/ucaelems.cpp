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
*/

#include "ucaelems.h"

void uprv_uca_reverseElement(ExpansionTable *expansions, UCAElements *el) {
    int32_t i = 0;
    UChar temp;
    uint32_t tempCE = 0, expansion = 0;
    UErrorCode status = U_ZERO_ERROR;

    for(i = 0; i<el->cSize/2; i++) {
        temp = el->cPoints[i];
        el->cPoints[i] = el->cPoints[el->cSize-i-1];
        el->cPoints[el->cSize-i-1] = temp;
    }
    el->codepoint = el->cPoints[0];

    if(el->noOfCEs>1) { /* this is an expansion that needs to be reversed and added - also, we need to change the mapValue */
      for(i = 0; i<el->noOfCEs/2; i++) {
          tempCE = el->CEs[i];
          el->CEs[i] = el->CEs[el->noOfCEs-i-1];
          el->CEs[el->noOfCEs-i-1] = tempCE;
      }
      expansion = UCOL_SPECIAL_FLAG | (EXPANSION_TAG<<UCOL_TAG_SHIFT) 
        | ((uprv_uca_addExpansion(expansions, el->CEs[0], &status)+(paddedsize(sizeof(UCATableHeader))>>2))<<4)
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
            fprintf(stderr, "out of memory for expansions\n");
            *status = U_MEMORY_ALLOCATION_ERROR;
            return -1;
        }
        expansions->CEs = newData;
        expansions->size *= 2;
    }

    expansions->CEs[expansions->position] = value;
    return(expansions->position++);
}

tempUCATable * uprv_uca_initTempTable(UCATableHeader *image, UErrorCode *status) {
  tempUCATable *t = (tempUCATable *)uprv_malloc(sizeof(tempUCATable));
  t->image = image;
  t->expansions = (ExpansionTable *)uprv_malloc(sizeof(ExpansionTable));
  uprv_memset(t->expansions, 0, sizeof(ExpansionTable));
  t->mapping = ucmp32_open(UCOL_NOT_FOUND);
  t->contractions = uprv_cnttab_open(t->mapping, status);
  return t;
}

void uprv_uca_closeTempTable(tempUCATable *t) {
  uprv_free(t->expansions);
  uprv_cnttab_close(t->contractions);
  ucmp32_close(t->mapping);

  uprv_free(t);
}

/* This adds a read element, while testing for existence */
uint32_t uprv_uca_addAnElement(tempUCATable *t, UCAElements *element, UErrorCode *status) {
  CompactIntArray *mapping = t->mapping;
  ExpansionTable *expansions = t->expansions;
  CntTable *contractions = t->contractions; 

  int32_t i = 1;
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
        | ((uprv_uca_addExpansion(expansions, element->CEs[0], status)+(paddedsize(sizeof(UCATableHeader))>>2))<<4) 
        | 0x1;
      element->mapCE = expansion;
    }
  } else {     
    expansion = UCOL_SPECIAL_FLAG | (EXPANSION_TAG<<UCOL_TAG_SHIFT) 
      | ((uprv_uca_addExpansion(expansions, element->CEs[0], status)+(paddedsize(sizeof(UCATableHeader))>>2))<<4)
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
  }

  CE = ucmp32_get(mapping, element->cPoints[0]);

  if(element->cSize > 1) { /* we're adding a contraction */
    /* and we need to deal with it */
    /* we could aready have something in table - or we might not */
    /* The fact is that we want to add or modify an existing contraction */
    /* and add it backwards then */
    uint32_t result = uprv_uca_processContraction(contractions, element, CE, TRUE, status);
    if(CE == UCOL_NOT_FOUND || !isContraction(CE)) {
      ucmp32_set(mapping, element->cPoints[0], result);
    }
    /* add the reverse order */
    uprv_uca_reverseElement(expansions, element);
    CE = ucmp32_get(mapping, element->cPoints[0]);
    result = uprv_uca_processContraction(contractions, element, CE, FALSE, status);
    if(CE == UCOL_NOT_FOUND || !isContraction(CE)) {
      ucmp32_set(mapping, element->cPoints[0], result);
    }
  } else { /* easy case, */
    if( CE != UCOL_NOT_FOUND) {
        if(isContraction(CE)) { /* adding a non contraction element (thai, expansion, single) to already existing contraction */
            uprv_cnttab_setContraction(contractions, CE, 0, 0, element->mapCE, TRUE, status);
            /* This loop has to change the CE at the end of contraction REDO!*/
            uprv_cnttab_changeLastCE(contractions, CE, element->mapCE, TRUE, status);
        } else {
          //fprintf(stderr, "Warning - trying to overwrite already existing data for codepoint %04X\n", element->cPoints[0]);
          //*status = U_ILLEGAL_ARGUMENT_ERROR;
        }
    } else {
      ucmp32_set(mapping, element->cPoints[0], element->mapCE);
    }
  }


  return CE;
}

uint32_t uprv_uca_processContraction(CntTable *contractions, UCAElements *element, uint32_t existingCE, UBool forward, UErrorCode *status) {
    int32_t i = 0;
    UBool gotContractionOffset = FALSE;
    int32_t firstContractionOffset = 0;
    int32_t contractionOffset = 0;
    uint32_t contractionElement = UCOL_NOT_FOUND;

    if(U_FAILURE(*status)) {
        return UCOL_NOT_FOUND;
    }

    /* end of recursion */
    if(element->cSize == 1) {
      return element->mapCE;
    }

    /* this recursion currently feeds on the only element we have... We will have to copy it in order to accomodate */
    /* for both backward and forward cycles */

    /* we encountered either an empty space or a non-contraction element */
    /* this means we are constructing a new contraction sequence */
    if(existingCE == UCOL_NOT_FOUND || !isContraction(existingCE)) { 
      /* if it wasn't contraction, we wouldn't end up here*/
      firstContractionOffset = uprv_cnttab_addContraction(contractions, -1, 0, existingCE, forward, status);
      if(forward == FALSE) {
          uprv_cnttab_addContraction(contractions, firstContractionOffset, 0, existingCE, TRUE, status);
          uprv_cnttab_addContraction(contractions, firstContractionOffset, 0xFFFF, existingCE, TRUE, status);
      } 

      UChar toAdd = element->cPoints[1];
      element->cPoints++;
      element->cSize--;
      uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, forward, status);
      element->cPoints--;
      element->cSize++;
      contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, toAdd, newCE, forward, status);
      contractionOffset = uprv_cnttab_addContraction(contractions, firstContractionOffset, 0xFFFF, existingCE, forward, status);
      contractionElement =  constructContractCE(firstContractionOffset);
      return contractionElement;
    } else { /* we are adding to existing contraction */
      /* there were already some elements in the table, so we need to add a new contraction */
      /* Two things can happen here: either the codepoint is already in the table, or it is not */
      uint32_t position = uprv_cnttab_findCP(contractions, existingCE, *(element->cPoints+1), forward, status);
      element->cPoints++;
      element->cSize--;
      if(position != 0) {       /* if it is we just continue down the chain */
        uint32_t eCE = uprv_cnttab_getCE(contractions, existingCE, position, forward, status);
        uint32_t newCE = uprv_uca_processContraction(contractions, element, eCE, forward, status);
        uprv_cnttab_setContraction(contractions, existingCE, position, *(element->cPoints), newCE, forward, status);
      } else {                  /* if it isn't, we will have to create a new sequence */
        uint32_t newCE = uprv_uca_processContraction(contractions, element, UCOL_NOT_FOUND, forward, status);
        uprv_cnttab_insertContraction(contractions, existingCE, *(element->cPoints), newCE, forward, status);
      }
      element->cPoints--;
      element->cSize++;
      return existingCE;
    }
}

UCATableHeader *uprv_uca_reassembleTable(tempUCATable *t, UCATableHeader *mD, UErrorCode *status) {
    CompactIntArray *mapping = t->mapping;
    ExpansionTable *expansions = t->expansions;
    CntTable *contractions = t->contractions; 

    if(U_FAILURE(*status)) {
        return NULL;
    }

    uint32_t beforeContractions = (paddedsize(sizeof(UCATableHeader))+paddedsize(expansions->position*sizeof(uint32_t)))/sizeof(UChar);

    int32_t contractionsSize = 0;
    if(mD == NULL) {
      contractionsSize = uprv_cnttab_constructTable(contractions, beforeContractions, status);
    } else {
      contractionsSize = mD->contractionSize;
    }

    ucmp32_compact(mapping, 1);
    UMemoryStream *ms = uprv_mstrm_openNew(8192);
    int32_t mappingSize = ucmp32_flattenMem(mapping, ms);
    const uint8_t *flattened = uprv_mstrm_getBuffer(ms, &mappingSize);

    uint32_t tableOffset = 0;
    uint8_t *dataStart;

    uint32_t toAllocate = paddedsize(sizeof(UCATableHeader))+paddedsize(expansions->position*sizeof(uint32_t))+paddedsize(mappingSize)+paddedsize(contractionsSize*(sizeof(UChar)+sizeof(uint32_t))+paddedsize(0x100*sizeof(uint32_t)));

    if(mD == NULL) {
      dataStart = (uint8_t *)malloc(toAllocate);
    } else {
      dataStart = (uint8_t *)realloc(mD, toAllocate);
    }

    UCATableHeader *myData = (UCATableHeader *)dataStart;
    myData->contractionSize = contractionsSize;

    tableOffset += paddedsize(sizeof(UCATableHeader));

    /* copy expansions */
    /*myData->expansion = (uint32_t *)dataStart+tableOffset;*/
    myData->expansion = tableOffset;
    memcpy(dataStart+tableOffset, expansions->CEs, expansions->position*sizeof(uint32_t));
    tableOffset += paddedsize(expansions->position*sizeof(uint32_t));

    /* contractions block */
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

    if(tableOffset != toAllocate) {
        fprintf(stderr, "calculation screwup!!! Expected to write %i but wrote %i instead!!!\n", toAllocate, tableOffset);
        *status = U_INTERNAL_PROGRAM_ERROR;
        free(dataStart);
        return 0;
    }

    myData->size = tableOffset;
    
    myData->variableTopValue = t->image->variableTopValue;
    myData->strength = t->image->strength;
    myData->frenchCollation = t->image->frenchCollation;
    myData->alternateHandling = t->image->alternateHandling; /* attribute for handling variable elements*/
    myData->caseFirst = t->image->caseFirst;         /* who goes first, lower case or uppercase */
    myData->caseLevel = t->image->caseLevel;         /* do we have an extra case level */
    myData->normalizationMode = t->image->normalizationMode; /* attribute for normalization */


    /* This should happen upon ressurection */
    const uint8_t *mapPosition = (uint8_t*)myData+myData->mappingPosition;
    myData->mapping = ucmp32_openFromData(&mapPosition, status);
    return myData;
}

UCATableHeader *uprv_uca_assembleTable(tempUCATable *t, UErrorCode *status) {
  return uprv_uca_reassembleTable(t, 0, status);
}

