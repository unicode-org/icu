/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File reslist.h
*
* Modification History:
*
*   Date        Name        Description
*   02/21/00    weiv        Creation.
*******************************************************************************
*/

#ifndef RESLIST_H
#define RESLIST_H

#define KEY_SPACE_SIZE 65532
#define MAX_INT_VECTOR 2048

#include "unicode/utypes.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"
#include "uresdata.h"
#include "cmemory.h"
#include "cstring.h"
#include "unewdata.h"

/* Resource bundle root table */
struct SRBRoot {
  char *fLocale;
  uint16_t fKeyPoint;
  char *fKeys;
  int32_t fCount;
  struct SResource *fRoot; 
};

struct SRBRoot *bundle_open(UErrorCode *status);
/*void bundle_write(struct SRBRoot *bundle, const char *outputDir, const char *filename, UErrorCode *status);*/
void bundle_write(struct SRBRoot *bundle, const char *outputDir, UErrorCode *status);
void bundle_close(struct SRBRoot *bundle, UErrorCode *status);
void bundle_setlocale(struct SRBRoot *bundle, UChar *locale, UErrorCode *status);
uint16_t bundle_addtag(struct SRBRoot *bundle, char *tag, UErrorCode *status);

/* Various resource types */

struct SResTable {
    uint16_t fCount;
    uint32_t fChildrenSize;
    struct SResource *fFirst;
    struct SRBRoot *fRoot;
};

struct SResource* table_open(struct SRBRoot *bundle, char *tag, UErrorCode *status);
void table_close(struct SResource *table, UErrorCode *status);
void table_add(struct SResource *table, struct SResource *res, UErrorCode *status);
struct SResource *table_get(struct SResource *table, char *key, UErrorCode *status);

struct SResArray {
    uint32_t fCount;
    uint32_t fChildrenSize;
    struct SResource *fFirst;
    struct SResource *fLast;
};

struct SResource* array_open(struct SRBRoot *bundle, char *tag, UErrorCode *status);
void array_close(struct SResource *array, UErrorCode *status);
void array_add(struct SResource *array, struct SResource *res, UErrorCode *status);

struct SResString {
    uint32_t fLength;
    UChar *fChars;
};

struct SResource *string_open(struct SRBRoot *bundle, char *tag, UChar *value, int32_t len, UErrorCode *status);
void string_close(struct SResource *string, UErrorCode *status);

struct SResIntVector {
    uint32_t fCount;
    uint32_t *fArray;
};

struct SResource* intvector_open(struct SRBRoot *bundle, char *tag, UErrorCode *status);
void intvector_close(struct SResource *intvector, UErrorCode *status);
void intvector_add(struct SResource *intvector, int32_t value, UErrorCode *status);

struct SResInt {
    uint32_t fValue;
};

struct SResource *int_open(struct SRBRoot *bundle, char *tag, int32_t value, UErrorCode *status);
void int_close(struct SResource *intres, UErrorCode *status);

struct SResBinary {
    uint32_t fLength;
    uint8_t *fData;
};

struct SResource *bin_open(struct SRBRoot *bundle, char *tag, uint32_t length, uint8_t *data, UErrorCode *status);
void bin_close(struct SResource *binres, UErrorCode *status);

/* Resource place holder */

struct SResource {
    UResType fType;
    uint16_t fKey;
    uint32_t fSize; /* Size in bytes outside the header part */
    struct SResource *fNext; /*This is for internal chaining while building*/
    union {
        struct SResTable fTable;
        struct SResArray fArray;
        struct SResString fString;
        struct SResIntVector fIntVector;
        struct SResInt fIntValue;
        struct SResBinary fBinaryValue;
    } u;
};

void res_close(struct SResource *res, UErrorCode *status);




#endif /* #ifndef RESLIST_H */
