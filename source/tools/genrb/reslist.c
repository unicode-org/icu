/*
*******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File reslist.c
*
* Modification History:
*
*   Date        Name        Description
*   02/21/00    weiv        Creation.
*******************************************************************************
*/

#include <assert.h>
#include "reslist.h"
#include "unewdata.h"
#include "unicode/ures.h"
#include "errmsg.h"

#define BIN_ALIGNMENT 16

static UBool gIncludeCopyright = FALSE;

uint32_t res_write(UNewDataMemory *mem, struct SResource *res,
                   uint32_t usedOffset, UErrorCode *status);

static const UDataInfo dataInfo= {
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x52, 0x65, 0x73, 0x42},     /* dataFormat="resb" */
    {1, 0, 0, 0},                 /* formatVersion */
    {1, 4, 0, 0}                  /* dataVersion take a look at version inside parsed resb*/
};

static uint8_t calcPadding(uint32_t size) {
    /* returns space we need to pad */
    return (uint8_t) ((size % sizeof(uint32_t)) ? (sizeof(uint32_t) - (size % sizeof(uint32_t))) : 0);

}

void setIncludeCopyright(UBool val){
    gIncludeCopyright=val;
}

UBool getIncludeCopyright(void){
    return gIncludeCopyright;
}

/* Writing Functions */
static uint32_t string_write(UNewDataMemory *mem, struct SResource *res,
                             uint32_t usedOffset, UErrorCode *status) {
    udata_write32(mem, res->u.fString.fLength);
    udata_writeUString(mem, res->u.fString.fChars, res->u.fString.fLength + 1);
    udata_writePadding(mem, calcPadding(res->fSize));

    return usedOffset;
}

/* Writing Functions */
static uint32_t alias_write(UNewDataMemory *mem, struct SResource *res,
                             uint32_t usedOffset, UErrorCode *status) {
    udata_write32(mem, res->u.fString.fLength);
    udata_writeUString(mem, res->u.fString.fChars, res->u.fString.fLength + 1);
    udata_writePadding(mem, calcPadding(res->fSize));

    return usedOffset;
}

static uint32_t array_write(UNewDataMemory *mem, struct SResource *res,
                            uint32_t usedOffset, UErrorCode *status) {
    uint32_t *resources = NULL;
    uint32_t  i         = 0;

    struct SResource *current = NULL;

    if (U_FAILURE(*status)) {
        return 0;
    }

    if (res->u.fArray.fCount > 0) {
        resources = (uint32_t *) uprv_malloc(sizeof(uint32_t) * res->u.fArray.fCount);

        if (resources == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return 0;
        }

        current = res->u.fArray.fFirst;
        i = 0;

        while (current != NULL) {
            if (current->fType == URES_INT) {
                resources[i] = (current->fType << 28) | (current->u.fIntValue.fValue & 0xFFFFFFF);
            } else if (current->fType == URES_BINARY) {
                uint32_t uo = usedOffset;

                usedOffset    = res_write(mem, current, usedOffset, status);
                resources[i]  = (current->fType << 28) | (usedOffset >> 2);
                usedOffset   += current->fSize + calcPadding(current->fSize) - (usedOffset - uo);
            } else {
                usedOffset    = res_write(mem, current, usedOffset, status);
                resources[i]  = (current->fType << 28) | (usedOffset >> 2);
                usedOffset   += current->fSize + calcPadding(current->fSize);
            }

            i++;
            current = current->fNext;
        }

        /* usedOffset += res->fSize + pad; */

        udata_write32(mem, res->u.fArray.fCount);
        udata_writeBlock(mem, resources, sizeof(uint32_t) * res->u.fArray.fCount);
        uprv_free(resources);
    } else {
        /* array is empty */
        udata_write32(mem, 0);
    }

    return usedOffset;
}

static uint32_t intvector_write(UNewDataMemory *mem, struct SResource *res,
                                uint32_t usedOffset, UErrorCode *status) {
  uint32_t i = 0;
    udata_write32(mem, res->u.fIntVector.fCount);
    for(i = 0; i<res->u.fIntVector.fCount; i++) {
      udata_write32(mem, res->u.fIntVector.fArray[i]);
    }

    return usedOffset;
}

static uint32_t bin_write(UNewDataMemory *mem, struct SResource *res,
                          uint32_t usedOffset, UErrorCode *status) {
    uint32_t pad       = 0;
    uint32_t extrapad  = calcPadding(res->fSize);
    uint32_t dataStart = usedOffset + sizeof(res->u.fBinaryValue.fLength);

    if (dataStart % BIN_ALIGNMENT) {
        pad = (BIN_ALIGNMENT - dataStart % BIN_ALIGNMENT);
        udata_writePadding(mem, pad);
        usedOffset += pad;
    }

    udata_write32(mem, res->u.fBinaryValue.fLength);
    if (res->u.fBinaryValue.fLength > 0) {
        udata_writeBlock(mem, res->u.fBinaryValue.fData, res->u.fBinaryValue.fLength);
    }
    udata_writePadding(mem, (BIN_ALIGNMENT - pad + extrapad));

    return usedOffset;
}

static uint32_t int_write(UNewDataMemory *mem, struct SResource *res,
                          uint32_t usedOffset, UErrorCode *status) {
    return usedOffset;
}

static uint32_t table_write(UNewDataMemory *mem, struct SResource *res,
                            uint32_t usedOffset, UErrorCode *status) {
    uint8_t   pad       = 0;
    uint32_t  i         = 0;
    uint16_t *keys      = NULL;
    uint32_t *resources = NULL;

    struct SResource *current = NULL;

    if (U_FAILURE(*status)) {
        return 0;
    }

    pad = calcPadding(res->fSize);

    if (res->u.fTable.fCount > 0) {
        keys = (uint16_t *) uprv_malloc(sizeof(uint16_t) * res->u.fTable.fCount);

        if (keys == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return 0;
        }

        resources = (uint32_t *) uprv_malloc(sizeof(uint32_t) * res->u.fTable.fCount);

        if (resources == NULL) {
            uprv_free(keys);
            *status = U_MEMORY_ALLOCATION_ERROR;
            return 0;
        }

        current = res->u.fTable.fFirst;
        i       = 0;

        while (current != NULL) {
            assert(i < res->u.fTable.fCount);

            /* where the key is plus root pointer */
            keys[i] = (uint16_t) (current->fKey + sizeof(uint32_t));

            if (current->fType == URES_INT) {
                resources[i] = (current->fType << 28) | (current->u.fIntValue.fValue & 0xFFFFFFF);
            } else if (current->fType == URES_BINARY) {
                uint32_t uo = usedOffset;

                usedOffset    = res_write(mem, current, usedOffset, status);
                resources[i]  = (current->fType << 28) | (usedOffset >> 2);
                usedOffset   += current->fSize + calcPadding(current->fSize) - (usedOffset - uo);
            } else {
                usedOffset    = res_write(mem, current, usedOffset, status);
                resources[i]  = (current->fType << 28) | (usedOffset >> 2);
                usedOffset   += current->fSize + calcPadding(current->fSize);
            }

            i++;
            current = current->fNext;
        }

        udata_write16(mem, res->u.fTable.fCount);

        udata_writeBlock(mem, keys, sizeof(uint16_t) * res->u.fTable.fCount);
        udata_writePadding(mem, pad);
        udata_writeBlock(mem, resources, sizeof(uint32_t) * res->u.fTable.fCount);

        uprv_free(keys);
        uprv_free(resources);
    } else {
        /* table is empty */
        udata_write16(mem, 0);
        udata_writePadding(mem, pad);
    }

    return usedOffset;
}

uint32_t res_write(UNewDataMemory *mem, struct SResource *res,
                   uint32_t usedOffset, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return 0;
    }

    if (res != NULL) {
        switch (res->fType) {
        case URES_STRING:
            return string_write    (mem, res, usedOffset, status);
        case URES_ALIAS:
            return alias_write    (mem, res, usedOffset, status);
        case URES_INT_VECTOR:
            return intvector_write (mem, res, usedOffset, status);
        case URES_BINARY:
            return bin_write       (mem, res, usedOffset, status);
        case URES_INT:
            return int_write       (mem, res, usedOffset, status);
        case URES_ARRAY:
            return array_write     (mem, res, usedOffset, status);
        case URES_TABLE:
            return table_write     (mem, res, usedOffset, status);

        default:
            break;
        }
    }

    *status = U_INTERNAL_PROGRAM_ERROR;
    return 0;
}

void bundle_write(struct SRBRoot *bundle, const char *outputDir, const char *outputPkg, char *writtenFilename, int writtenFilenameLen, UErrorCode *status) {
    UNewDataMemory *mem        = NULL;
    uint8_t         pad        = 0;
    uint32_t        root       = 0;
    uint32_t        usedOffset = 0;
    char            dataName[1024];

    if (writtenFilename && writtenFilenameLen) {
        *writtenFilename = 0;
    }

    if (U_FAILURE(*status)) {
        return;
    }

    if (writtenFilename) {
       int32_t off = 0, len = 0;
       if (outputDir) {
           len = (int32_t)uprv_strlen(outputDir);
           if (len > writtenFilenameLen) {
               len = writtenFilenameLen;
           }
           uprv_strncpy(writtenFilename, outputDir, len);
       }
       if (writtenFilenameLen -= len) {
           off += len;
           writtenFilename[off] = U_FILE_SEP_CHAR;
           if (--writtenFilenameLen) {
               ++off;
               if(outputPkg != NULL)
               {
                   uprv_strcpy(writtenFilename+off, outputPkg);
                   off += uprv_strlen(outputPkg);
                   writtenFilename[off] = '_';
                   ++off;
               }

               len = (int32_t)uprv_strlen(bundle->fLocale);
               if (len > writtenFilenameLen) {
                   len = writtenFilenameLen;
               }
               uprv_strncpy(writtenFilename + off, bundle->fLocale, len);
               if (writtenFilenameLen -= len) {
                   off += len;
                   len = 5;
                   if (len > writtenFilenameLen) {
                       len = writtenFilenameLen;
                   }
                   uprv_strncpy(writtenFilename +  off, ".res", len);
               }
           }
       }
    }

    if(outputPkg)
    {
        uprv_strcpy(dataName, outputPkg);
        uprv_strcat(dataName, "_");
        uprv_strcat(dataName, bundle->fLocale);
    }
    else
    {
        uprv_strcpy(dataName, bundle->fLocale);
    }

    mem = udata_create(outputDir, "res", dataName, &dataInfo, (gIncludeCopyright==TRUE)? U_COPYRIGHT_STRING:NULL, status);
    if(U_FAILURE(*status)){
        return;
    }
    pad = calcPadding(bundle->fKeyPoint);

    usedOffset = sizeof(uint32_t) + bundle->fKeyPoint + pad ; /*this is how much root and keys are taking up*/

    root = ((usedOffset + bundle->fRoot->u.fTable.fChildrenSize) >> 2) | (URES_TABLE << 28); /* we're gonna put the main table at the end */

    udata_write32(mem, root);

    udata_writeBlock(mem, bundle->fKeys, bundle->fKeyPoint);

    udata_writePadding(mem, pad);

    usedOffset = res_write(mem, bundle->fRoot, usedOffset, status);

    udata_finish(mem, status);
}

/* Opening Functions */
struct SResource* table_open(struct SRBRoot *bundle, char *tag, UErrorCode *status) {
    struct SResource *res;

    if (U_FAILURE(*status)) {
        return NULL;
    }

    res = (struct SResource *) uprv_malloc(sizeof(struct SResource));

    if (res == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    res->fType = URES_TABLE;
    res->fKey  = bundle_addtag(bundle, tag, status);

    if (U_FAILURE(*status)) {
        uprv_free(res);
        return NULL;
    }

    res->fNext = NULL;
    res->fSize = sizeof(uint16_t);

    res->u.fTable.fCount        = 0;
    res->u.fTable.fChildrenSize = 0;
    res->u.fTable.fFirst        = NULL;
    res->u.fTable.fRoot         = bundle;

    return res;
}

struct SResource* array_open(struct SRBRoot *bundle, char *tag, UErrorCode *status) {
    struct SResource *res;

    if (U_FAILURE(*status)) {
        return NULL;
    }

    res = (struct SResource *) uprv_malloc(sizeof(struct SResource));

    if (res == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    res->fType = URES_ARRAY;
    res->fKey  = bundle_addtag(bundle, tag, status);

    if (U_FAILURE(*status)) {
        uprv_free(res);
        return NULL;
    }

    res->fNext = NULL;
    res->fSize = sizeof(int32_t);

    res->u.fArray.fCount        = 0;
    res->u.fArray.fChildrenSize = 0;
    res->u.fArray.fFirst        = NULL;
    res->u.fArray.fLast         = NULL;

    return res;
}

struct SResource *string_open(struct SRBRoot *bundle, char *tag, const UChar *value, int32_t len, UErrorCode *status) {
    struct SResource *res;

    if (U_FAILURE(*status)) {
        return NULL;
    }

    res = (struct SResource *) uprv_malloc(sizeof(struct SResource));

    if (res == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    res->fType = URES_STRING;
    res->fKey  = bundle_addtag(bundle, tag, status);

    if (U_FAILURE(*status)) {
        uprv_free(res);
        return NULL;
    }

    res->fNext = NULL;

    res->u.fString.fLength = len;
    res->u.fString.fChars  = (UChar *) uprv_malloc(sizeof(UChar) * (len + 1));

    if (res->u.fString.fChars == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        uprv_free(res);
        return NULL;
    }

    uprv_memcpy(res->u.fString.fChars, value, sizeof(UChar) * (len + 1));
    res->fSize = sizeof(int32_t) + sizeof(UChar) * (len+1);

    return res;
}

/* TODO: make alias_open and string_open use the same code */
struct SResource *alias_open(struct SRBRoot *bundle, char *tag, UChar *value, int32_t len, UErrorCode *status) {
    struct SResource *res;

    if (U_FAILURE(*status)) {
        return NULL;
    }

    res = (struct SResource *) uprv_malloc(sizeof(struct SResource));

    if (res == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    res->fType = URES_ALIAS;
    res->fKey  = bundle_addtag(bundle, tag, status);

    if (U_FAILURE(*status)) {
        uprv_free(res);
        return NULL;
    }

    res->fNext = NULL;

    res->u.fString.fLength = len;
    res->u.fString.fChars  = (UChar *) uprv_malloc(sizeof(UChar) * (len + 1));

    if (res->u.fString.fChars == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        uprv_free(res);
        return NULL;
    }

    uprv_memcpy(res->u.fString.fChars, value, sizeof(UChar) * (len + 1));
    res->fSize = sizeof(int32_t) + sizeof(UChar) * (len + 1);

    return res;
}


struct SResource* intvector_open(struct SRBRoot *bundle, char *tag, UErrorCode *status) {
    struct SResource *res;

    if (U_FAILURE(*status)) {
        return NULL;
    }

    res = (struct SResource *) uprv_malloc(sizeof(struct SResource));

    if (res == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    res->fType = URES_INT_VECTOR;
    res->fKey  = bundle_addtag(bundle, tag, status);

    if (U_FAILURE(*status)) {
        uprv_free(res);
        return NULL;
    }

    res->fNext = NULL;
    res->fSize = sizeof(int32_t);

    res->u.fIntVector.fCount = 0;
    res->u.fIntVector.fArray = (uint32_t *) uprv_malloc(sizeof(uint32_t) * RESLIST_MAX_INT_VECTOR);

    if (res->u.fIntVector.fArray == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        uprv_free(res);
        return NULL;
    }

    return res;
}

struct SResource *int_open(struct SRBRoot *bundle, char *tag, int32_t value, UErrorCode *status) {
    struct SResource *res;

    if (U_FAILURE(*status)) {
        return NULL;
    }

    res = (struct SResource *) uprv_malloc(sizeof(struct SResource));

    if (res == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    res->fType = URES_INT;
    res->fKey  = bundle_addtag(bundle, tag, status);

    if (U_FAILURE(*status)) {
        uprv_free(res);
        return NULL;
    }

    res->fSize              = 0;
    res->fNext              = NULL;
    res->u.fIntValue.fValue = value;

    return res;
}

struct SResource *bin_open(struct SRBRoot *bundle, const char *tag, uint32_t length, uint8_t *data,const char* fileName,UErrorCode *status) {
    struct SResource *res;

    if (U_FAILURE(*status)) {
        return NULL;
    }

    res = (struct SResource *) uprv_malloc(sizeof(struct SResource));

    if (res == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    res->fType = URES_BINARY;
    res->fKey  = bundle_addtag(bundle, tag, status);

    if (U_FAILURE(*status)) {
        uprv_free(res);
        return NULL;
    }
    
    res->fNext = NULL;

    res->u.fBinaryValue.fLength = length;
    res->u.fBinaryValue.fFileName = NULL;
    if(fileName!=NULL && uprv_strcmp(fileName, "") !=0){
        res->u.fBinaryValue.fFileName = (char*) uprv_malloc(sizeof(char) * (uprv_strlen(fileName)+1));
        uprv_strcpy(res->u.fBinaryValue.fFileName,fileName);
    }
    if (length > 0) {
        res->u.fBinaryValue.fData   = (uint8_t *) uprv_malloc(sizeof(uint8_t) * length);

        if (res->u.fBinaryValue.fData == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            uprv_free(res);
            return NULL;
        }

        uprv_memcpy(res->u.fBinaryValue.fData, data, length);
    }
    else {
        res->u.fBinaryValue.fData = NULL;
    }

    res->fSize = sizeof(int32_t) + sizeof(uint8_t) * length + BIN_ALIGNMENT;

    return res;
}

struct SRBRoot *bundle_open(UErrorCode *status) {
    struct SRBRoot *bundle = NULL;

    if (U_FAILURE(*status)) {
        return NULL;
    }

    bundle = (struct SRBRoot *) uprv_malloc(sizeof(struct SRBRoot));

    if (bundle == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }

    bundle->fLocale   = NULL;
    bundle->fKeyPoint = 0;
    bundle->fKeys     = (char *) uprv_malloc(sizeof(char) * KEY_SPACE_SIZE);

    if (bundle->fKeys == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        uprv_free(bundle);
        return NULL;
    }

    bundle->fCount = 0;
    bundle->fRoot  = table_open(bundle, NULL, status);

    if (bundle->fRoot == NULL || U_FAILURE(*status)) {
        *status = U_MEMORY_ALLOCATION_ERROR;

        uprv_free(bundle->fKeys);
        uprv_free(bundle);

        return NULL;
    }

    return bundle;
}

/* Closing Functions */
void table_close(struct SResource *table, UErrorCode *status) {
    struct SResource *current = NULL;
    struct SResource *prev    = NULL;

    current = table->u.fTable.fFirst;

    while (current != NULL) {
        prev    = current;
        current = current->fNext;

        res_close(prev, status);
    }

    table->u.fTable.fFirst = NULL;
}

void array_close(struct SResource *array, UErrorCode *status) {
    struct SResource *current = NULL;
    struct SResource *prev    = NULL;

    current = array->u.fArray.fFirst;

    while (current != NULL) {
        prev    = current;
        current = current->fNext;

        res_close(prev, status);
    }
    array->u.fArray.fFirst = NULL;
}

void string_close(struct SResource *string, UErrorCode *status) {
    if (string->u.fString.fChars != NULL) {
        uprv_free(string->u.fString.fChars);
        string->u.fString.fChars =NULL;
    }
}

void alias_close(struct SResource *alias, UErrorCode *status) {
    if (alias->u.fString.fChars != NULL) {
        uprv_free(alias->u.fString.fChars);
        alias->u.fString.fChars =NULL;
    }
}

void intvector_close(struct SResource *intvector, UErrorCode *status) {
    if (intvector->u.fIntVector.fArray != NULL) {
        uprv_free(intvector->u.fIntVector.fArray);
        intvector->u.fIntVector.fArray =NULL;
    }
}

void int_close(struct SResource *intres, UErrorCode *status) {
    /* Intentionally left blank */
}

void bin_close(struct SResource *binres, UErrorCode *status) {
    if (binres->u.fBinaryValue.fData != NULL) {
        uprv_free(binres->u.fBinaryValue.fData);
        binres->u.fBinaryValue.fData = NULL;
    }
}

void res_close(struct SResource *res, UErrorCode *status) {
    if (res != NULL) {
        switch(res->fType) {
        case URES_STRING:
            string_close(res, status);
            break;
        case URES_ALIAS:
            alias_close(res, status);
            break;
        case URES_INT_VECTOR:
            intvector_close(res, status);
            break;
        case URES_BINARY:
            bin_close(res, status);
            break;
        case URES_INT:
            int_close(res, status);
            break;
        case URES_ARRAY:
            array_close(res, status);
            break;
        case URES_TABLE :
            table_close(res, status);
            break;
        default:
            /* Shouldn't happen */
            break;
        }

        uprv_free(res);
    }
}

void bundle_close(struct SRBRoot *bundle, UErrorCode *status) {
    struct SResource *current = NULL;
    struct SResource *prev    = NULL;

    if (bundle->fRoot != NULL) {
        current = bundle->fRoot->u.fTable.fFirst;

        while (current != NULL) {
            prev    = current;
            current = current->fNext;

            res_close(prev, status);
        }

        uprv_free(bundle->fRoot);
    }

    if (bundle->fLocale != NULL) {
        uprv_free(bundle->fLocale);
    }

    if (bundle->fKeys != NULL) {
        uprv_free(bundle->fKeys);
    }

    uprv_free(bundle);
}

/* Adding Functions */
void table_add(struct SResource *table, struct SResource *res, int linenumber, UErrorCode *status) {
    struct SResource *current = NULL;
    struct SResource *prev    = NULL;
    struct SResTable *list;

    if (U_FAILURE(*status)) {
        return;
    }

    /* remember this linenumber to report to the user if there is a duplicate key */
    res->line = linenumber;

    /* here we need to traverse the list */
    list = &(table->u.fTable);

    ++(list->fCount);
    table->fSize += sizeof(uint32_t) + sizeof(uint16_t);

    table->u.fTable.fChildrenSize += res->fSize + calcPadding(res->fSize);

    if (res->fType == URES_TABLE) {
        table->u.fTable.fChildrenSize += res->u.fTable.fChildrenSize;
    } else if (res->fType == URES_ARRAY) {
        table->u.fTable.fChildrenSize += res->u.fArray.fChildrenSize;
    }

    /* is list still empty? */
    if (list->fFirst == NULL) {
        list->fFirst = res;
        res->fNext   = NULL;
        return;
    }

    current = list->fFirst;

    while (current != NULL) {
        if (uprv_strcmp(((list->fRoot->fKeys) + (current->fKey)), ((list->fRoot->fKeys) + (res->fKey))) < 0) {
            prev    = current;
            current = current->fNext;
        } else if (uprv_strcmp(((list->fRoot->fKeys) + (current->fKey)), ((list->fRoot->fKeys) + (res->fKey))) > 0) {
            /* we're either in front of list, or in middle */
            if (prev == NULL) {
                /* front of the list */
                list->fFirst = res;
            } else {
                /* middle of the list */
                prev->fNext = res;
            }

            res->fNext = current;
            return;
        } else {
            /* Key already exists! ERROR! */
            error(linenumber, "duplicate key '%s' in table, first appeared at line %d", list->fRoot->fKeys + current->fKey, current->line);
            *status = U_UNSUPPORTED_ERROR;
            return;
        }
    }

    /* end of list */
    prev->fNext = res;
    res->fNext  = NULL;
}

void array_add(struct SResource *array, struct SResource *res, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return;
    }

    if (array->u.fArray.fFirst == NULL) {
        array->u.fArray.fFirst = res;
        array->u.fArray.fLast  = res;
    } else {
        array->u.fArray.fLast->fNext = res;
        array->u.fArray.fLast        = res;
    }

    (array->u.fArray.fCount)++;

    array->fSize += sizeof(uint32_t);
    array->u.fArray.fChildrenSize += res->fSize + calcPadding(res->fSize);

    if (res->fType == URES_TABLE) {
        array->u.fArray.fChildrenSize += res->u.fTable.fChildrenSize;
    } else if (res->fType == URES_ARRAY) {
        array->u.fArray.fChildrenSize += res->u.fArray.fChildrenSize;
    }
}

void intvector_add(struct SResource *intvector, int32_t value, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return;
    }

    *(intvector->u.fIntVector.fArray + intvector->u.fIntVector.fCount) = value;
    intvector->u.fIntVector.fCount++;

    intvector->fSize += sizeof(uint32_t);
}

/* Misc Functions */

void bundle_setlocale(struct SRBRoot *bundle, UChar *locale, UErrorCode *status) {

    if(U_FAILURE(*status)) {
        return;
    }

    if (bundle->fLocale!=NULL) {
        uprv_free(bundle->fLocale);
    }

    bundle->fLocale= (char*) uprv_malloc(sizeof(char) * (u_strlen(locale)+1));

    if(bundle->fLocale == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    /*u_strcpy(bundle->fLocale, locale);*/
    u_UCharsToChars(locale, bundle->fLocale, u_strlen(locale)+1);

}

uint16_t bundle_addtag(struct SRBRoot *bundle, const char *tag, UErrorCode *status) {
    uint16_t keypos;

    if (U_FAILURE(*status)) {
        return (uint16_t) - 1;
    }

    if (tag == NULL) {
        return (uint16_t) - 1;
    }

    keypos = (uint16_t)bundle->fKeyPoint;

    bundle->fKeyPoint += (uint16_t) (uprv_strlen(tag) + 1);

    if (bundle->fKeyPoint > KEY_SPACE_SIZE) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return (uint16_t) - 1;
    }

    uprv_strcpy(bundle->fKeys + keypos, tag);

    return keypos;
}
