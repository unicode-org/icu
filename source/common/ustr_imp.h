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
 * Are the Unicode properties loaded?
 * This must be used before internal functions are called that do
 * not perform this check.
 * @internal
 */
U_CFUNC UBool
uprv_haveProperties();

/**
 * Type of a function that may be passed to the internal case mapping functions
 * for growing the destination buffer.
 * @internal
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

/**
 * Internal case folding function.
 * @internal
 */
U_CFUNC int32_t
u_internalStrFoldCase(UChar *dest, int32_t destCapacity,
                      const UChar *src, int32_t srcLength,
                      uint32_t options,
                      GrowBuffer *growBuffer, void *context,
                      UErrorCode *pErrorCode);

/**
 * Return the full case folding mapping for c.
 * Must be used only if uprv_haveProperties() is true.
 * @internal
 */
U_CFUNC int32_t
u_internalFoldCase(UChar32 c, UChar dest[32], uint32_t options);

/**
 * Internal case-insensitive string compare function.
 * @internal
 */
U_CFUNC int32_t
u_internalStrcasecmp(const UChar *s1, int32_t length1,
                     const UChar *s2, int32_t length2,
                     uint32_t options);

/**
 * Internal, somewhat faster version of u_getCombiningClass()
 * for use by normalization quick check etc.
 * First make sure that data is loaded by u_getCombiningClass(0x300)!=0
 * or uprv_haveProperties() is true.
 * @internal
 */
U_CFUNC uint8_t
u_internalGetCombiningClass(UChar32 c);

#endif
