/*
*******************************************************************************
*
*   Copyright (C) 2008, International Business Machines
*   Corporation, Google and others.  All Rights Reserved.
*
*******************************************************************************
*/
/*
 * Author : eldawy@google.com (Mohamed Eldawy)
 * ucnvsel.h
 *
 * Purpose: To generate a list of encodings capable of handling
 * a given Unicode text
 *
 * Started 09-April-2008
 */

#ifndef __ICU_UCNV_SEL_H__
#define __ICU_UCNV_SEL_H__

#include "unicode/uset.h"
#include "unicode/utypes.h"
#include "unicode/utf16.h"
#include "unicode/uenum.h"
#include "unicode/ucnv.h"


/**
 * \file
 *
 * This is the declarations for the encoding selector.
 * The goal is, given a unicode string, find the encodings
 * this string can be mapped to. 
 *
 */


/**
 * @{
 * The selector data structure
 */
struct UConverterSelector;
typedef struct UConverterSelector UConverterSelector;
/** @} */

/**
 * open a selector. If converterList is NULL, build for all converters. If excludedCodePoints 
 * is NULL, don't exclude any codepoints
 *
 *
 * @param converterList a pointer to encoding names needed to be involved. 
 *  NULL means build a selector for all possible converters
 * @param converterListSize number of encodings in above list. 
 *  Setting converterListSize to 0, builds a selector for all
 *  converters. ucnvsel_open() does not transfer ownership to this
 *  array. Once uncvsel_open() returns, the caller is free to reuse/destroy
 *  the array.
 * @param excludedCodePoints a set of codepoints to be excluded from
 *  consideration. set to NULL to exclude nothing
 * @param whichSet what converter set to use? use this to determine whether
 *                 to construct selector for fallback or for roundtrip only mappings
 * @param status an in/out ICU UErrorCode
 * @return a pointer to the created selector
 *
 * @draft ICU 4.2
 */
U_CAPI UConverterSelector* ucnvsel_open(const char* const*  converterList,
                                      int32_t converterListSize,
                                      const USet* excludedCodePoints,
                                      const UConverterUnicodeSet   whichSet,
                                      UErrorCode* status);

/* close opened selector */
/**
 * closes a selector. and releases allocated memory
 * if any Enumerations were returned by ucnv_select*, they become invalid.
 * They can be closed before or after calling ucnv_closeSelector,
 * but should never be used after selector is closed
 *
 * @see ucnv_selectForString
 * @see ucnv_selectForUTF8
 *
 * @param sel selector to close
 *
 * @draft ICU 4.2
 */
U_CAPI void ucnvsel_close(UConverterSelector *sel);

/**
 * unserialize a selector from a linear buffer. No alignment necessary.
 * the function does NOT take ownership of the given buffer. Caller is free
 * to reuse/destroy buffer immediately after calling this function
 * Unserializing a selector is much faster than creating it from scratch
 * and is nicer on the heap (not as many allocations and frees)
 * ucnvsel_open() is expensive. Therefore, it is desirable to unserialize the data structre
 * rather than building it from scratch.
 *
 * @param buffer pointer to a linear buffer containing serialized data
 * @param length the capacity of this buffer (can be equal to or larger than
                 the actual data length)
 * @param status an in/out ICU UErrorCode
 * @return a pointer to the created selector
 *
 * @draft ICU 4.2
 */
U_CAPI UConverterSelector* ucnvsel_unserialize(const char* buffer,
                                             int32_t length,
                                             UErrorCode* status);

/**
 * serialize a selector into a linear buffer. No alignment necessary
 * The current serialized form is portable to different Endianness, and can
 * travel between ASCII and EBCDIC systems
 *
 * @param sel selector to consider
 * @param buffer pointer to a linear buffer to receive data
 * @param bufferCapacity the capacity of this buffer
 * @param status an in/out ICU UErrorCode
 * @return the required buffer capacity to hold serialize data (even if the call fails
           with a U_BUFFER_OVERFLOW_ERROR, it will return the required capacity)
 *
 * @draft ICU 4.2
 */
U_CAPI int32_t ucnvsel_serialize(const UConverterSelector* sel,
                               char* buffer,
                               int32_t bufferCapacity,
                               UErrorCode* status);

/**
 * check a UTF16 string using the selector. Find out what encodings it can be mapped to
 *
 *
 * @param sel built selector
 * @param s pointer to UTF16 string
 * @param length length of UTF16 string in UChars, or -1 if NULL terminated
 * @param status an in/out ICU UErrorCode
 * @return an enumeration containing encoding names. Returned encoding names
 *  will be the same as supplied to ucnv_openSelector, or will be the 
 *  canonical names if selector was built for all encodings.
 *  The order of encodings will be the same as supplied by the call to
 *  ucnv_openSelector (if encodings were supplied)
 *
 * @draft ICU 4.2
 */
U_CAPI UEnumeration *ucnvsel_selectForString(const UConverterSelector* sel, const UChar *s,
int32_t length, UErrorCode *status);

/**
 * check a UTF8 string using the selector. Find out what encodings it can be
 * mapped to illegal codepoints will be ignored by this function! Only legal
 *  codepoints will be considered for conversion
 *
 * @param sel built selector
 * @param s pointer to UTF8 string
 * @param length length of UTF8 string (in chars), or -1 if NULL terminated
 * @param status an in/out ICU UErrorCode
 * @return an enumeration containing encoding names. Returned encoding names
 *  will be the same as supplied to ucnv_openSelector, or will be the canonical
 *  names if selector was built for all encodings.
 *  The order of encodings will be the same as supplied by the call to
 *  ucnv_openSelector (if encodings were supplied)
 *
 * @draft ICU 4.2
 */
U_CAPI UEnumeration *ucnvsel_selectForUTF8(const UConverterSelector* sel,
                                 const char *s,
                                 int32_t length,
                                 UErrorCode *status);


#endif  /* __ICU_UCNV_SEL_H__ */
