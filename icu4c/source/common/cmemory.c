/*
******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File cmemory.c      ICU Heap allocation.
*                     All ICU heap allocation, both for C and C++ new of ICU
*                     class types, comes through these functions.
*
*                     If you have a need to replace ICU allocation, this is the
*                     place to do it.
*
*                     Note that uprv_malloc(0) returns a non-NULL pointer, and
*                     that a subsequent free of that pointer value is a NOP.
*
******************************************************************************
*/
#include "cmemory.h"

/* uprv_malloc(0) returns a pointer to this read-only data. */                
static const int32_t zeroMem[] = {0, 0, 0, 0, 0, 0};

U_CAPI void * U_EXPORT2
uprv_malloc(size_t s) {
    if (s > 0) {
        return malloc(s);
    } else {
        return (void *)zeroMem;
    }
}

U_CAPI void * U_EXPORT2
uprv_realloc(void * buffer, size_t size) {
    if (buffer == zeroMem) {
        return uprv_malloc(size);
    } else if (size == 0) {
        free(buffer);
        return (void *)zeroMem;
    } else {
        return realloc(buffer, size);
    }
}

U_CAPI void U_EXPORT2
uprv_free(void *buffer) {
    if (buffer != zeroMem) {
        free(buffer);
    }
}

