/*
******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************/


/*----------------------------------------------------------------------------------
 *
 *  UDataMemory     A class-like struct that serves as a handle to a piece of memory
 *                  that contains some ICU data (resource, converters, whatever.)
 *
 *                  When an application opens ICU data (with udata_open, for example,
 *                  a UDataMemory * is returned.
 *
 *----------------------------------------------------------------------------------*/

#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/udata.h"

#include "udatamem.h"

void UDataMemory_init(UDataMemory *This) {
    uprv_memset(This, 0, sizeof(UDataMemory));
}


void UDatamemory_assign(UDataMemory *dest, UDataMemory *source) {
    /* UDataMemory Assignment.  Destination UDataMemory must be initialized first.  */
    UBool mallocedFlag = dest->heapAllocated;
    uprv_memcpy(dest, source, sizeof(UDataMemory));
    dest->heapAllocated = mallocedFlag;
}

UDataMemory *UDataMemory_createNewInstance(UErrorCode *pErr) {
    UDataMemory *This;

    if (U_FAILURE(*pErr)) {
        return NULL;
    }
    This = uprv_malloc(sizeof(UDataMemory));
    if (This == NULL) {
        *pErr = U_MEMORY_ALLOCATION_ERROR; }
    else {
        UDataMemory_init(This);
        This->heapAllocated = TRUE;
    }
    return This;
}


const DataHeader *
normalizeDataPointer(const void *p) {
    /* allow the data to be optionally prepended with an alignment-forcing double value */
    const DataHeader *pdh = (const DataHeader *)p;
    if(pdh==NULL || (pdh->dataHeader.magic1==0xda && pdh->dataHeader.magic2==0x27)) {
        return pdh;
    } else {
        return (const DataHeader *)((const double *)p+1);
    }
}


void UDataMemory_setData (UDataMemory *This, const void *dataAddr) {
    This->pHeader = normalizeDataPointer(dataAddr);
}


U_CAPI void U_EXPORT2
udata_close(UDataMemory *pData) {
    if(pData!=NULL) {
        uprv_unmapFile(pData);
        if(pData->heapAllocated ) {
            uprv_free(pData);
        } else {
            UDataMemory_init(pData);
        }
    }
}

U_CAPI const void * U_EXPORT2
udata_getMemory(UDataMemory *pData) {
    if(pData!=NULL && pData->pHeader!=NULL) {
        return (char *)(pData->pHeader)+pData->pHeader->dataHeader.headerSize;
    } else {
        return NULL;
    }
}


UBool  UDataMemory_isLoaded(UDataMemory *This) {
    return This->pHeader != NULL;
}

