#ifndef UCOL_CNTTABLE_H
#define UCOL_CNTTABLE_H

#include "uhash.h"
#include "UCAData.h"

typedef struct {
	ContractionTable **elements;
    CompactIntArray *mapping;
    UChar *codePoints;
    uint32_t *CEs;
    int32_t *offsets;
    int32_t position;
    int32_t size;
    int32_t capacity;
} CntTable;

CntTable *uprv_cnttab_open(CompactIntArray *mapping, UErrorCode *status);
/* construct the table for output */
int32_t uprv_cnttab_constructTable(CntTable *table, uint32_t mainOffset, UErrorCode *status); 
void uprv_cnttab_close(CntTable *table);
/* adds more contractions in table. If element is non existant, it creates on. Returns element handle */
uint32_t uprv_cnttab_addContraction(CntTable *table, uint32_t element, UChar codePoint, uint32_t value, UBool forward, UErrorCode *status);
/* sets a part of contraction sequence in table. If element is non existant, it creates on. Returns element handle */
uint32_t uprv_cnttab_setContraction(CntTable *table, uint32_t element, int32_t offset, UChar codePoint, uint32_t value, UBool forward, UErrorCode *status);
/* inserts a part of contraction sequence in table. Sequences behind the offset are moved back. If element is non existant, it creates on. Returns element handle */
uint32_t uprv_cnttab_insertContraction(CntTable *table, uint32_t element, UChar codePoint, uint32_t value, UBool forward, UErrorCode *status);
/* this is for adding non contractions */
uint32_t uprv_cnttab_changeLastCE(CntTable *table, uint32_t element, uint32_t value, UBool forward, UErrorCode *status);

uint32_t uprv_cnttab_findCP(CntTable *table, uint32_t element, UChar codePoint, UBool forward, UErrorCode *status);

uint32_t uprv_cnttab_getCE(CntTable *table, uint32_t element, uint32_t position, UBool forward, UErrorCode *status);

#endif
