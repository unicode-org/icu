/*
******************************************************************************
*
*   Copyright (C) 1997-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File UMEMSTRM.C
*
* @author       Vladimir Weinstein
*
* Modification History:
*
*   Date        Name        Description
*   5/17/00      weiv          Created
******************************************************************************
*/

#include "umemstrm.h"
#include "cmemory.h"
#include "cstring.h" 
#include "unicode/ustring.h" 

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
    MS->fEof = FALSE;
    MS->fStart = (uint8_t *)uprv_malloc(MS->fSize);
    if(MS->fStart == NULL) {
        MS->fError = TRUE;
        uprv_free(MS);
        return NULL;
    }
    return MS;
}

U_CAPI UMemoryStream * U_EXPORT2 uprv_mstrm_openBuffer(const uint8_t *buffer, int32_t len){
    UMemoryStream *MS = (UMemoryStream *)uprv_malloc(sizeof(UMemoryStream));
    if(MS == NULL) {
        return NULL;
    }
    MS->fReadOnly = TRUE;
    MS->fStart = (uint8_t *)buffer; /*functions themselves take care about constness of buffer - see above line*/
    MS->fPos = len;
    MS->fReadPos = 0;
    MS->fError = FALSE;
    MS->fEof = FALSE;
    return MS;
}

U_CAPI void U_EXPORT2 uprv_mstrm_close(UMemoryStream *MS){
    if(MS->fReadOnly == FALSE && MS->fStart != NULL) {
        uprv_free(MS->fStart);
    }
    uprv_free(MS);
}

U_CAPI UBool U_EXPORT2 uprv_mstrm_setError(UMemoryStream *MS){
    MS->fError = TRUE;
    return MS->fError;
}

U_CAPI UBool U_EXPORT2 uprv_mstrm_error(const UMemoryStream *MS){
    return MS->fError;
}

U_CAPI UBool U_EXPORT2 uprv_mstrm_eof(const UMemoryStream *MS){
    return MS->fEof;
}

U_CAPI int32_t U_EXPORT2 uprv_mstrm_read(UMemoryStream *MS, void* addr, int32_t len) {
    if(MS->fError == FALSE) {
        if(len + MS->fReadPos > MS->fPos) {
            len = MS->fPos - MS->fReadPos;
            MS->fError = TRUE;
            MS->fEof = TRUE;
        }

        uprv_memcpy(addr, MS->fStart+MS->fReadPos, len);
        MS->fReadPos+=len;

        return len;
    } else {
        return 0;
    }
}

U_CAPI int32_t U_EXPORT2 uprv_mstrm_write(UMemoryStream *MS, const void *buffer, int32_t len){
    if(MS->fError == FALSE) {
        if(MS->fReadOnly == FALSE) {
            if(len + MS->fPos > MS->fSize) {
                uint8_t *newstart = (uint8_t *)uprv_realloc(MS->fStart, MS->fSize+len);
                if(newstart != NULL) {
                    MS->fSize+=len;
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

U_CAPI const uint8_t * U_EXPORT2 uprv_mstrm_getBuffer(const UMemoryStream *MS, int32_t *len){
    if(MS->fError == FALSE) {
        *len = MS->fPos;
        return MS->fStart;
    } else {
        *len = 0;
        return NULL;
    }
}

U_CAPI const uint8_t * U_EXPORT2 uprv_mstrm_getCurrentBuffer(const UMemoryStream *MS, int32_t *len){
    if(MS->fError == FALSE) {
        *len = MS->fPos-MS->fReadPos;
        return MS->fStart+MS->fReadPos;
    } else {
        *len = 0;
        return NULL;
    }
}

U_CAPI int32_t U_EXPORT2 uprv_mstrm_skip(UMemoryStream *MS, int32_t len){
    if(MS->fError == FALSE) {
        MS->fReadPos+=len;
    } else {
        return 0;
    }
    if(MS->fReadPos>MS->fPos) {
        MS->fError = TRUE;
        return 0;
    } else {
        return len;
    }
}

U_CAPI int32_t U_EXPORT2 uprv_mstrm_jump(UMemoryStream *MS, const uint8_t *where){
    if(MS->fError == FALSE) {
        MS->fReadPos=(where-MS->fStart);
    } else {
        return 0;
    }
    if(MS->fReadPos>MS->fPos) {
        MS->fError = TRUE;
        return 0;
    } else {
        return where-MS->fStart;
    }
}

U_CAPI void U_EXPORT2
uprv_mstrm_write8(UMemoryStream *MS, uint8_t byte) {
    if(MS!=NULL) {
        uprv_mstrm_write(MS, &byte, 1);
    }
}

U_CAPI void U_EXPORT2
uprv_mstrm_write16(UMemoryStream *MS, uint16_t word) {
    if(MS!=NULL) {
        uprv_mstrm_write(MS, &word, 2);
    }
}

U_CAPI void U_EXPORT2
uprv_mstrm_write32(UMemoryStream *MS, uint32_t wyde) {
    if(MS!=NULL) {
        uprv_mstrm_write(MS, &wyde, 4);
    }
}

U_CAPI void U_EXPORT2
uprv_mstrm_writeBlock(UMemoryStream *MS, const void *s, int32_t length) {
    if(MS!=NULL) {
        if(length>0) {
            uprv_mstrm_write(MS, s, length);
        }
    }
}

U_CAPI void U_EXPORT2
uprv_mstrm_writePadding(UMemoryStream *MS, int32_t length) {
    static const uint8_t padding[16]={
        0xaa, 0xaa, 0xaa, 0xaa,
        0xaa, 0xaa, 0xaa, 0xaa,
        0xaa, 0xaa, 0xaa, 0xaa,
        0xaa, 0xaa, 0xaa, 0xaa
    };
    if(MS!=NULL) {
        while(length>=16) {
            uprv_mstrm_write(MS, padding, 16);
            length-=16;
        }
        if(length>0) {
            uprv_mstrm_write(MS, padding, length);
        }
    }
}

U_CAPI void U_EXPORT2
uprv_mstrm_writeString(UMemoryStream *MS, const char *s, int32_t length) {
    if(MS!=NULL) {
        if(length==-1) {
            length=uprv_strlen(s);
        }
        if(length>0) {
            uprv_mstrm_write(MS, s, length);
        }
    }
}

U_CAPI void U_EXPORT2
uprv_mstrm_writeUString(UMemoryStream *MS, const UChar *s, int32_t length) {
    if(MS!=NULL) {
        if(length==-1) {
            length=u_strlen(s);
        }
        if(length>0) {
            uprv_mstrm_write(MS, s, length*sizeof(UChar));
        }
    }
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
