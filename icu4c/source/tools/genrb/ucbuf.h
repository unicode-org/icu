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
    int32_t fileLen;
    int32_t remaining;
    FileStream* in;
    UConverter* conv;
};

typedef struct UCHARBUF UCHARBUF;
#define U_EOF 0xFFFF

UChar32 
ucbuf_getc(UCHARBUF* buf,UErrorCode* err);

UChar32 
ucbuf_getcx(UCHARBUF* buf,UErrorCode* err);

void 
ucbuf_rewind(UCHARBUF* buf);

UCHARBUF* 
ucbuf_open(FileStream* in,const char* cp,UErrorCode* err);

void 
ucbuf_close(UCHARBUF* buf);

void 
ucbuf_ungetc(UChar32 ungetChar,UCHARBUF* buf);

void 
ucbuf_closebuf(UCHARBUF* buf);

UBool 
ucbuf_autodetect(FileStream* in,const char** cp);

#endif
