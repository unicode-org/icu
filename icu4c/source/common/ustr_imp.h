/*  
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ustr_imp.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001jan30
*   created by: Markus W. Scherer
*/

#ifndef __USTR_IMP_H__
#define __USTR_IMP_H__

#include "unicode/utypes.h"

/**
 * Type of a function that may be passed to the internal case mapping functions
 * for growing the destination buffer.
 */
typedef UBool
GrowBuffer(void *context,       /* opaque pointer for this function */
           UChar **buffer,      /* in/out destination buffer pointer */
           int32_t *pCapacity,  /* in/out buffer capacity in numbers of UChars */
           int32_t reqCapacity, /* requested capacity */
           int32_t length);     /* number of UChars to be copied to new buffer */

/*
 * Internal string casing functions implementing ustring.c and UnicodeString
 * case mapping functions.
 */
U_CFUNC int32_t
u_internalStrToLower(UChar *dest, int32_t destCapacity,
                     const UChar *src, int32_t srcLength,
                     const char *locale,
                     GrowBuffer *growBuffer, void *context,
                     UErrorCode *pErrorCode);

U_CFUNC int32_t
u_internalStrToUpper(UChar *dest, int32_t destCapacity,
                     const UChar *src, int32_t srcLength,
                     const char *locale,
                     GrowBuffer *growBuffer, void *context,
                     UErrorCode *pErrorCode);

#endif
