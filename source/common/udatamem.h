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
#ifndef __UDATAMEM_H__
#define __UDATAMEM_H__

#include "unicode/udata.h"
#include "ucmndata.h"

struct UDataMemory {
    const commonDataFuncs  *vFuncs;      /* Function Pointers for accessing TOC             */

    const DataHeader *pHeader;     /* Header of the memory being described by this    */
                                   /*   UDataMemory object.                           */
    const void       *toc;         /* For common memory, table of contents for        */
                                   /*   the pieces within.                            */
    UBool             heapAllocated;  /* True if this UDataMemory Object is on the    */
                                   /*  heap and thus needs to be deleted when closed. */

    void             *mapAddr;     /* For mapped or allocated memory, the start addr. */
                                   /* Only non-null if a close operation should unmap */
                                   /*  the associated data.                           */
    void             *map;         /* Handle, or other data, OS dependent.            */
                                   /* Only non-null if a close operation should unmap */
                                   /*  the associated data, and additional info       */
                                   /*   beyond the mapAddr is needed to do that.      */
};

UDataMemory     *UDataMemory_createNewInstance(UErrorCode *pErr);
void             UDatamemory_assign  (UDataMemory *dest, UDataMemory *source);
void             UDataMemory_init    (UDataMemory *This);
UBool            UDataMemory_isLoaded(UDataMemory *This);
void             UDataMemory_setData (UDataMemory *This, const void *dataAddr);


const DataHeader *UDataMemory_normalizeDataPointer(const void *p);
#endif

