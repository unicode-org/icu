/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ustr.h
*
* Modification History:
*
*   Date        Name        Description
*   05/28/99    stephen     Creation.
*******************************************************************************
*/

#ifndef USTR_H
#define USTR_H 1

#include "unicode/utypes.h"

/* A C representation of a string "object" (to avoid realloc all the time) */
struct UString {
  UChar *fChars;
  int32_t fLength;
  int32_t fCapacity;
};

void ustr_init(struct UString *s);

void ustr_deinit(struct UString *s);

void ustr_setlen(struct UString *s, int32_t len, UErrorCode *status);

void ustr_cpy(struct UString *dst, const struct UString *src, 
	      UErrorCode *status);

void ustr_cat(struct UString *dst, const struct UString *src,
	      UErrorCode *status);

void ustr_ncat(struct UString *dst, const struct UString *src, 
	       int32_t n, UErrorCode *status);

void ustr_ucat(struct UString *dst, UChar c, UErrorCode *status);

#endif
