/*
*******************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File UMEMSTRM.C
*
* @author       Vladimir Weinstein
*
* Modification History:
*
*   Date        Name        Description
*   5/17/00      weiv          Created
*******************************************************************************
*/

#include "umemstrm.h"
#include "cmemory.h"

U_CAPI UMemoryStream * U_EXPORT2 uprv_mstrm_openNew(int32_t size) {
    UMemoryStream *MS = (UMemoryStream *)uprv_malloc(sizeof(UMemoryStream));
    if(MS == NULL) {
        return NULL;
    }

    MS->fReadOnly = FALSE;
    if (size == 0) {
        MS->fSize = 0xFFFF;
    } else {
        MS->fSize = size;
    }
    MS->fStart = NULL;
    MS->fPos = 0;
    MS->fReadPos = 0;
    MS->fError = FALSE;
    MS->fStart = (uint8_t *)uprv_malloc(MS->fSize);
    if(MS->fStart == NULL) {
        MS->fError = TRUE;
        uprv_free(MS);
        return NULL;
    }
    return MS;
}

U_CAPI UMemoryStream * U_EXPORT2 uprv_mstrm_openBuffer(uint8_t *buffer, int32_t len){
    UMemoryStream *MS = (UMemoryStream *)uprv_malloc(sizeof(UMemoryStream));
    if(MS == NULL) {
        return NULL;
    }
    MS->fReadOnly = TRUE;
    MS->fStart = buffer;
    MS->fPos = 0;
    MS->fReadPos = 0;
    MS->fError = FALSE;
    return MS;
}

U_CAPI void U_EXPORT2 uprv_mstrm_close(UMemoryStream *MS){
    if(MS->fReadOnly == FALSE && MS->fStart != NULL) {
        uprv_free(MS->fStart);
    }
    uprv_free(MS);
}

U_CAPI bool_t U_EXPORT2 uprv_mstrm_setError(UMemoryStream *MS){
    MS->fError = TRUE;
    return MS->fError;
}

U_CAPI bool_t U_EXPORT2 uprv_mstrm_error(UMemoryStream *MS){
    return MS->fError;
}

U_CAPI int32_t U_EXPORT2 uprv_mstrm_read(UMemoryStream *MS, void* addr, int32_t len) {
    if(MS->fError == FALSE) {
        if(len + MS->fReadPos > MS->fPos) {
            len = MS->fPos - MS->fReadPos;
            MS->fError = TRUE;
        }

        uprv_memcpy(addr, MS->fStart+MS->fReadPos, len);

        return len;
    } else {
        return 0;
    }
}

U_CAPI int32_t U_EXPORT2 uprv_mstrm_write(UMemoryStream *MS, uint8_t *buffer, int32_t len){
    if(MS->fError == FALSE) {
        if(MS->fReadOnly == FALSE) {
            if(len + MS->fPos > MS->fSize) {
                uint8_t *newstart = (uint8_t *)uprv_realloc(MS->fStart, 2*MS->fSize);
                if(newstart != NULL) {
                    MS->fSize*=2;
                    MS->fStart = newstart;
                } else {
                    MS->fError = TRUE;
                    return -1;
                }
            }
            uprv_memcpy(MS->fStart + MS->fPos, buffer, len);
            MS->fPos += len;
            return len;
        } else {
            MS->fError = TRUE;
            return 0;
        }
    } else {
        return 0;
    }
}

U_CAPI uint8_t * U_EXPORT2 uprv_mstrm_getBuffer(UMemoryStream *MS, int32_t *len){
    if(MS->fError == FALSE) {
        *len = MS->fPos;
        return MS->fStart;
    } else {
        *len = 0;
        return NULL;
    }
}
