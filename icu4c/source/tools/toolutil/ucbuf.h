/*
*******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ucbuf.c
*
* Modification History:
*
*   Date        Name        Description
*   05/10/01    Ram         Creation.
*
* This API reads in files and returns UChars
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "filestrm.h"
#include "cmemory.h"
#include <stdio.h>

#ifndef UCBUF_H
#define UCBUF_H 1

struct UCHARBUF {
    UChar* buffer;
    UChar* currentPos;
    UChar* bufLimit;
    int32_t remaining;
    FileStream* in;
    UConverter* conv;
};

typedef struct UCHARBUF UCHARBUF;
#define U_EOF 0xFFFFFFFF
#define U_ERR 0xFFFFFFFE
  
U_CAPI UChar32 U_EXPORT2
ucbuf_getc(UCHARBUF* buf,UErrorCode* err);

U_CAPI UChar32 U_EXPORT2
ucbuf_getcx(UCHARBUF* buf,UErrorCode* err);

U_CAPI void U_EXPORT2
ucbuf_rewind(UCHARBUF* buf);

U_CAPI UCHARBUF* U_EXPORT2
ucbuf_open(FileStream* in,UErrorCode* err);

U_CAPI void U_EXPORT2
ucbuf_close(UCHARBUF* buf);

U_CAPI void U_EXPORT2
ucbuf_ungetc(UChar32 ungetChar,UCHARBUF* buf);

U_CAPI UBool U_EXPORT2
ucbuf_autodetect(FileStream* in,const char** cp);

#endif
