/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ustdio.h
*
* Modification History:
*
*   Date        Name        Description
*   10/16/98    stephen	    Creation.
*   11/06/98	stephen	    Modified per code review.
*   03/12/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef USTDIO_H
#define USTDIO_H 1

#include "unicode/utypes.h"
#include "ufile.h"

#define U_EOF 0xFFFF

UFILE*
u_finit(FileStream *f, const char *cp,
	UErrorCode *status);

void
u_fclose(UFILE *file);

int32_t
u_fputs(const UChar *s,
	UFILE *f,
	UErrorCode *status);

int32_t
u_fputc(UChar uc,
	UFILE *f,
	UErrorCode *status);

int32_t
u_file_write(const UChar *chars, 
	     int32_t count, 
	     UFILE *f,
	     UErrorCode *status);

UChar*
u_fgets(UFILE *f,
	int32_t n,
	UChar *s,
	UErrorCode *status);

UChar
u_fgetc(UFILE *f,
	UErrorCode *status);

UChar
u_fungetc(UChar c,
	  UFILE *f,
	  UErrorCode *status);

int32_t
u_file_read(UChar *chars, 
	    int32_t count, 
	    UFILE *f,
	    UErrorCode *status);

#endif





