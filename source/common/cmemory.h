/*
******************************************************************************
*
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File CMEMORY.H
*
*  Contains stdlib.h/string.h memory functions
*
* @author       Bertrand A. Damiba
*
* Modification History:
*
*   Date        Name        Description
*   6/20/98     Bertrand    Created.
*  05/03/99     stephen     Changed from functions to macros.
*
******************************************************************************
*/

#ifndef CMEMORY_H
#define CMEMORY_H

#include <stdlib.h>
#include <string.h>



#define uprv_malloc(size) U_STANDARD_CPP_NAMESPACE malloc(size)
#define uprv_realloc(buffer, size) U_STANDARD_CPP_NAMESPACE realloc(buffer, size)
#define uprv_free(buffer) U_STANDARD_CPP_NAMESPACE free(buffer)
#define uprv_memcpy(dst, src, size) U_STANDARD_CPP_NAMESPACE memcpy(dst, src, size)
#define uprv_memmove(dst, src, size) U_STANDARD_CPP_NAMESPACE memmove(dst, src, size)
#define uprv_memset(buffer, mark, size) U_STANDARD_CPP_NAMESPACE memset(buffer, mark, size)
#define uprv_memcmp(buffer1, buffer2, size) U_STANDARD_CPP_NAMESPACE memcmp(buffer1, buffer2,size)

/**
 * This should align the memory properly on any machine.
 * This is very useful for the safeClone functions.
 */
typedef union {
    long    t1;
    double  t2;
    void   *t3;
} UAlignedMemory;

/**
 * Get the amount of bytes that a pointer is off by from
 * the previous aligned pointer
 */
#define U_ALIGNMENT_OFFSET(ptr) (((long)ptr) & (sizeof(UAlignedMemory) - 1))

/**
 * Get the amount of bytes to add to a pointer
 * in order to get the next aligned address
 */
#define U_ALIGNMENT_OFFSET_UP(ptr) (sizeof(UAlignedMemory) - U_ALIGNMENT_OFFSET(ptr))

#endif
