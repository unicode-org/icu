/*
*****************************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
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
*****************************************************************************************
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
};

U_CAPI UMemoryStream * U_EXPORT2 uprv_mstrm_openNew(int32_t size);
U_CAPI UMemoryStream * U_EXPORT2 uprv_mstrm_openBuffer(uint8_t *buffer, int32_t len);
U_CAPI void U_EXPORT2 uprv_mstrm_close(UMemoryStream *MS);
U_CAPI UBool U_EXPORT2 uprv_mstrm_setError(UMemoryStream *MS);
U_CAPI UBool U_EXPORT2 uprv_mstrm_error(UMemoryStream *MS);
U_CAPI int32_t U_EXPORT2 uprv_mstrm_read(UMemoryStream *MS, void* addr, int32_t len);
U_CAPI int32_t U_EXPORT2 uprv_mstrm_write(UMemoryStream *MS, uint8_t *buffer, int32_t len);
U_CAPI uint8_t * U_EXPORT2 uprv_mstrm_getBuffer(UMemoryStream *MS, int32_t *len);

#endif /* _FILESTRM*/




