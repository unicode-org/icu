#include "ucol_cnt.h"
#include "cmemory.h"

void uprv_growTable(ContractionTable *tbl, UErrorCode *status) {
    if(tbl->position == tbl->size) {
        uint32_t *newData = (uint32_t *)realloc(tbl->CEs, 2*tbl->size*sizeof(uint32_t));
        UChar *newCPs = (UChar *)realloc(tbl->codePoints, 2*tbl->size*sizeof(UChar));
        if(newData == NULL || newCPs == NULL) {
            fprintf(stderr, "out of memory for contractions\n");
            *status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        tbl->CEs = newData;
        tbl->codePoints = newCPs;
        tbl->size *= 2;
    }
}

CntTable *uprv_cnttab_open(CompactIntArray *mapping, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return 0;
    }
    CntTable *tbl = (CntTable *)malloc(sizeof(CntTable));
    tbl->mapping = mapping;
    //tbl->elements = uhash_open(uhash_hashLong, uhash_compareLong, status);
    //uhash_setValueDeleter(tbl->elements, deleteCntElement);
    tbl->elements = (ContractionTable **)malloc(INIT_EXP_TABLE_SIZE*sizeof(ContractionTable *));
    tbl->capacity = INIT_EXP_TABLE_SIZE;
    memset(tbl->elements, 0, INIT_EXP_TABLE_SIZE*sizeof(ContractionTable *));
    tbl->size = 0;
    tbl->position = 0;
    tbl->CEs = NULL;
    tbl->codePoints = NULL;
    tbl->offsets = NULL;
    return tbl;
}

ContractionTable *addATableElement(CntTable *table, uint32_t *key, UErrorCode *status) {
    ContractionTable *el = (ContractionTable *)malloc(sizeof(ContractionTable));
    el->CEs = (uint32_t *)malloc(INIT_EXP_TABLE_SIZE*sizeof(uint32_t));
    el->codePoints = (UChar *)malloc(INIT_EXP_TABLE_SIZE*sizeof(UChar));
    el->position = 0;
    el->size = INIT_EXP_TABLE_SIZE;
    el->forward = TRUE;
    memset(el->CEs, 'F', INIT_EXP_TABLE_SIZE*sizeof(uint32_t));
    memset(el->codePoints, 'F', INIT_EXP_TABLE_SIZE*sizeof(UChar));

    el->reversed = (ContractionTable *)malloc(sizeof(ContractionTable));
    el->reversed->CEs = (uint32_t *)malloc(INIT_EXP_TABLE_SIZE*sizeof(uint32_t));
    el->reversed->codePoints = (UChar *)malloc(INIT_EXP_TABLE_SIZE*sizeof(UChar));
    el->reversed->position = 0;
    el->reversed->size = INIT_EXP_TABLE_SIZE;
    el->reversed->forward = FALSE;
    memset(el->reversed->CEs, 'R', INIT_EXP_TABLE_SIZE*sizeof(uint32_t));
    memset(el->reversed->codePoints, 'R', INIT_EXP_TABLE_SIZE*sizeof(UChar));

    table->elements[table->size] = el;

    //uhash_put(table->elements, (void *)table->size, el, status);

    *key = table->size++;

    if(table->size > table->capacity) {
        // do realloc
        *status = U_MEMORY_ALLOCATION_ERROR;
    }

    return el;
}


int32_t uprv_cnttab_moveTable(CntTable *table, uint32_t oldOffset, uint32_t newOffset, UErrorCode *status) {
    uint32_t i, CE;
    int32_t difference = newOffset - oldOffset;
    if(U_FAILURE(*status)) {
        return 0;
    }
    for(i = 0; i<=0xFFFF; i++) {
        CE = ucmp32_get(table->mapping, i);
        if(isContraction(CE)) {
            CE = constructContractCE(getContractOffset(CE)+difference);
            ucmp32_set(table->mapping, (UChar)i, CE);
        }
    }
    return table->position;
}

int32_t uprv_cnttab_constructTable(CntTable *table, uint32_t mainOffset, UErrorCode *status) {
    int32_t i = 0, j = 0;
    if(U_FAILURE(*status)) {
        return 0;
    }

    table->position = 0;

    if(table->offsets != NULL) {
        free(table->offsets);
    }
    table->offsets = (int32_t *)malloc(table->size*sizeof(int32_t));


    /* See how much memory we need */
    for(i = 0; i<table->size; i++) {
        table->offsets[i] = table->position+mainOffset;
        table->position += table->elements[i]->position;
        if(table->elements[i]->reversed->position > 0) {
            table->elements[i]->codePoints[0] = (UChar)(table->elements[i]->position); /* set offset for backwards table */
            table->position += table->elements[i]->reversed->position;
        }
    }

    /* Allocate it */
    if(table->CEs != NULL) {
        free(table->CEs);
    }
    table->CEs = (uint32_t *)malloc(table->position*sizeof(uint32_t));
    memset(table->CEs, '?', table->position*sizeof(uint32_t));
    if(table->codePoints != NULL) {
        free(table->codePoints);
    }
    table->codePoints = (UChar *)malloc(table->position*sizeof(UChar));
    memset(table->codePoints, '?', table->position*sizeof(UChar));

    /* Now stuff the things in*/

    UChar *cpPointer = table->codePoints;
    uint32_t *CEPointer = table->CEs;
    for(i = 0; i<table->size; i++) {
        int32_t size = table->elements[i]->position;
        memcpy(cpPointer, table->elements[i]->codePoints, size*sizeof(UChar));
        memcpy(CEPointer, table->elements[i]->CEs, size*sizeof(uint32_t));
        for(j = 0; j<size; j++) {
            if(isContraction(*(CEPointer+j))) {
                *(CEPointer+j) = constructContractCE(table->offsets[getContractOffset(*(CEPointer+j))]);
            }
        }
        cpPointer += size;
        CEPointer += size;
        if(table->elements[i]->reversed->position > 0) {
            int32_t size2 = table->elements[i]->reversed->position;
            memcpy(cpPointer, (table->elements[i]->reversed->codePoints), size2*sizeof(UChar));
            memcpy(CEPointer, (table->elements[i]->reversed->CEs), size2*sizeof(uint32_t));
            for(j = 0; j<size2; j++) {
                if(isContraction(*(CEPointer+j))) {
                    *(CEPointer+j) = constructContractCE(table->offsets[getContractOffset(*(CEPointer+j))]);
                }
            }
            cpPointer += size2;
            CEPointer += size2;
        }
    }


    uint32_t CE;
    for(i = 0; i<=0xFFFF; i++) {
        CE = ucmp32_get(table->mapping, i);
        if(isContraction(CE)) {
            CE = constructContractCE(table->offsets[getContractOffset(CE)]);
            ucmp32_set(table->mapping, (UChar)i, CE);
        }
    }


    return table->position;
}

void uprv_cnttab_close(CntTable *table) {
    int32_t i = 0;
    for(i = 0; i<table->size; i++) {
        free(table->elements[i]->reversed->CEs);
        free(table->elements[i]->reversed->codePoints);
        free(table->elements[i]->reversed);
        free(table->elements[i]->CEs);
        free(table->elements[i]->codePoints);
        free(table->elements[i]);
    }
    free(table->elements);
    free(table->CEs);
    free(table->offsets);
    free(table->codePoints);
    free(table);
}

/* this is for adding non contractions */
uint32_t uprv_cnttab_changeLastCE(CntTable *table, uint32_t element, uint32_t value, UBool forward, UErrorCode *status) {
    element &= 0xFFFFFF;

    ContractionTable *tbl = NULL;
    if(U_FAILURE(*status)) {
        return 0;
    }

    if((element == 0xFFFFFF) || (tbl = table->elements[element]) == NULL) {
        tbl = addATableElement(table, &element, status);
    }

    if(forward == TRUE) {
        tbl->CEs[tbl->position-1] = value;
    } else {
        tbl->reversed->CEs[tbl->reversed->position-1] = value;
    }

    return(constructContractCE(element));
}


/* inserts a part of contraction sequence in table. Sequences behind the offset are moved back. If element is non existent, it creates on. Returns element handle */
uint32_t uprv_cnttab_insertContraction(CntTable *table, uint32_t element, UChar codePoint, uint32_t value, UBool forward, UErrorCode *status) {

    element &= 0xFFFFFF;
    ContractionTable *tbl = NULL;

    if(U_FAILURE(*status)) {
        return 0;
    }

    if((element == 0xFFFFFF) || (tbl = table->elements[element]) == NULL) {
        tbl = addATableElement(table, &element, status);
    }

    if(forward == FALSE) {
        tbl = tbl->reversed;
    }

    uprv_growTable(tbl, status);

    int32_t offset = 0;


    while(tbl->codePoints[offset] < codePoint && offset<tbl->position) {
        offset++;
    }

    int32_t i = tbl->position;
    for(i = tbl->position; i > offset; i--) {
        tbl->CEs[i] = tbl->CEs[i-1];
        tbl->codePoints[i] = tbl->codePoints[i-1];
    }

    tbl->CEs[offset] = value;
    tbl->codePoints[offset] = codePoint;

    tbl->position++;

    return(constructContractCE(element));
}


/* adds more contractions in table. If element is non existant, it creates on. Returns element handle */
uint32_t uprv_cnttab_addContraction(CntTable *table, uint32_t element, UChar codePoint, uint32_t value, UBool forward, UErrorCode *status) {

    element &= 0xFFFFFF;

    ContractionTable *tbl = NULL;

    if(U_FAILURE(*status)) {
        return 0;
    }

    if((element == 0xFFFFFF) || (tbl = table->elements[element]) == NULL) {
        tbl = addATableElement(table, &element, status);
    } 

    if(forward == FALSE) {
        tbl = tbl->reversed;
    }

    uprv_growTable(tbl, status);

    tbl->CEs[tbl->position] = value;
    tbl->codePoints[tbl->position] = codePoint;

    tbl->position++;

    return(constructContractCE(element));
}

/* sets a part of contraction sequence in table. If element is non existant, it creates on. Returns element handle */
uint32_t uprv_cnttab_setContraction(CntTable *table, uint32_t element, int32_t offset, UChar codePoint, uint32_t value, UBool forward, UErrorCode *status) {

    element &= 0xFFFFFF;
    ContractionTable *tbl = NULL;

    if(U_FAILURE(*status)) {
        return 0;
    }

    if((element == 0xFFFFFF) || (tbl = table->elements[element]) == NULL) {
        tbl = addATableElement(table, &element, status);
    }

    if(forward == FALSE) {
        tbl = tbl->reversed;
    }

    if(offset >= tbl->size) {
        *status = U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    tbl->CEs[offset] = value;
    tbl->codePoints[offset] = codePoint;

    //return(offset);
    return(constructContractCE(element));
}

uint32_t uprv_cnttab_findCP(CntTable *table, uint32_t element, UChar codePoint, UBool forward, UErrorCode *status) {

    element &= 0xFFFFFF;
    ContractionTable *tbl = NULL;

    if(U_FAILURE(*status)) {
        return 0;
    }

    if((element == 0xFFFFFF) || (tbl = table->elements[element]) == NULL) {
      return 0;
    }

    if(forward == FALSE) {
        tbl = tbl->reversed;
    }

    int32_t position = 0;

    while(codePoint > tbl->codePoints[position]) {
      position++;
      if(position > tbl->position) {
        return 0;
      }
    }
    if (codePoint == tbl->codePoints[position]) {
      return position;
    } else {
      return 0;
    }
}

uint32_t uprv_cnttab_getCE(CntTable *table, uint32_t element, int32_t position, UBool forward, UErrorCode *status) {

    element &= 0xFFFFFF;
    ContractionTable *tbl = NULL;

    if(U_FAILURE(*status)) {
        return UCOL_NOT_FOUND;
    }

    if((element == 0xFFFFFF) || (tbl = table->elements[element]) == NULL) {
        return UCOL_NOT_FOUND;
    }

    if(forward == FALSE) {
        tbl = tbl->reversed;
    }


    if(position > tbl->position) {
      return UCOL_NOT_FOUND;
    } else {
      return tbl->CEs[position];
    }
}
