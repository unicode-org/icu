/*
******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File cmemory.c
*
******************************************************************************
*/
#include "unicode/utypes.h"
#include <stdlib.h>

U_CAPI void * U_EXPORT2
uprv_malloc(size_t s) {
    return malloc(s);
}

U_CAPI void * U_EXPORT2
uprv_realloc(void * buffer, size_t size) {
    return realloc(buffer, size);
}

U_CAPI void U_EXPORT2
uprv_free(void *buffer) {
  free(buffer);
}

