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

#include "ucmndata.h"

typedef struct UDataMemory {
    void             *map;         /* Handle, or whatever.  OS dependent.             */
                                   /* Only set if a close operation should unmap the  */
                                   /*  associated data.                               */
    const void       *mapAddr;     /* For mapped or allocated memory, the start addr. */
                                   /*   Needed to allow unmapping.                    */


    commonDataFuncs  *vFuncs;      /* Function Pointers for accessing TOC             */
    const void       *toc;         /* For common memory, to find pieces within.       */
    const DataHeader *pHeader;     /* Header.  For common data, header is at top of file */
    UBool             heapAllocated;  /* True if this UDataMemObject is on the heap   */
                                      /*  and thus needs to be deleted when closed.   */
} UDataMemory;

UDataMemory     *UDataMemory_createNewInstance(UErrorCode *pErr);
void             UDatamemory_assign  (UDataMemory *dest, UDataMemory *source);
void             UDataMemory_init    (UDataMemory *This);
UBool            UDataMemory_isLoaded(UDataMemory *This);
void             UDataMemory_setData (UDataMemory *This, const void *dataAddr);


const DataHeader *normalizeDataPointer(const void *p);
#endif
