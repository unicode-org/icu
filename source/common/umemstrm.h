/*
******************************************************************************
*
*   Copyright (C) 1997-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File UMEMSTRM.H
*
* Contains UMemoryStream interface
*
* @author       Vladimir Weinstein
*
* Modification History:
*
*   Date        Name        Description
*   5/17/00      weiv          Created.
*
******************************************************************************
*/

#ifndef UMEMSTRM_H
#define UMEMSTRM_H

#ifndef _UTYPES
#include "unicode/utypes.h"
#endif

struct UMemoryStream;

typedef struct UMemoryStream UMemoryStream;

struct  UMemoryStream{
    uint8_t *fStart;
    int32_t fSize;
    int32_t fPos;
    int32_t fReadPos;
    UBool fReadOnly;
    UBool fError;
  UBool fEof;
};

U_CAPI UMemoryStream * U_EXPORT2 uprv_mstrm_openNew(int32_t size);
U_CAPI UMemoryStream * U_EXPORT2 uprv_mstrm_openBuffer(const uint8_t *buffer, int32_t len);
U_CAPI void U_EXPORT2 uprv_mstrm_close(UMemoryStream *MS);
U_CAPI UBool U_EXPORT2 uprv_mstrm_setError(UMemoryStream *MS);
U_CAPI UBool U_EXPORT2 uprv_mstrm_error(const UMemoryStream *MS);
U_CAPI UBool U_EXPORT2 uprv_mstrm_eof(const UMemoryStream *MS);
U_CAPI int32_t U_EXPORT2 uprv_mstrm_read(UMemoryStream *MS, void* addr, int32_t len);
U_CAPI int32_t U_EXPORT2 uprv_mstrm_write(UMemoryStream *MS, const void *buffer, int32_t len);
U_CAPI const uint8_t * U_EXPORT2 uprv_mstrm_getBuffer(const UMemoryStream *MS, int32_t *len);
U_CAPI const uint8_t * U_EXPORT2 uprv_mstrm_getCurrentBuffer(const UMemoryStream *MS, int32_t *len);
U_CAPI int32_t U_EXPORT2 uprv_mstrm_skip(UMemoryStream *MS, int32_t len);
U_CAPI int32_t U_EXPORT2 uprv_mstrm_jump(UMemoryStream *MS, const uint8_t *where);

U_CAPI void U_EXPORT2 uprv_mstrm_write8(UMemoryStream *MS, uint8_t byte);
U_CAPI void U_EXPORT2 uprv_mstrm_write16(UMemoryStream *MS, uint16_t word);
U_CAPI void U_EXPORT2 uprv_mstrm_write32(UMemoryStream *MS, uint32_t wyde);
U_CAPI void U_EXPORT2 uprv_mstrm_writeBlock(UMemoryStream *MS, const void *s, int32_t length);
U_CAPI void U_EXPORT2 uprv_mstrm_writePadding(UMemoryStream *MS, int32_t length);
U_CAPI void U_EXPORT2 uprv_mstrm_writeString(UMemoryStream *MS, const char *s, int32_t length);
U_CAPI void U_EXPORT2 uprv_mstrm_writeUString(UMemoryStream *MS, const UChar *s, int32_t length);

#endif /* _FILESTRM*/




