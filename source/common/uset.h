/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uset.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002mar07
*   created by: Markus W. Scherer
*
*   Poor man's C version of UnicodeSet, with only basic functions.
*   See uset.c for more details.
*/

#ifndef __USET_H__
#define __USET_H__

#include "unicode/utypes.h"

struct USet;
typedef struct USet USet;

struct USerializedSet {
    const uint16_t *array;
    int32_t bmpLength, length;
};
typedef struct USerializedSet USerializedSet;

U_CAPI USet * U_EXPORT2
uset_open(UChar32 start, UChar32 limit);

U_CAPI void U_EXPORT2
uset_close(USet *set);

U_CAPI UBool U_EXPORT2
uset_add(USet *set, UChar32 c);

U_CAPI void U_EXPORT2
uset_remove(USet *set, UChar32 c);

U_CAPI UBool U_EXPORT2
uset_isEmpty(const USet *set);

U_CAPI UBool U_EXPORT2
uset_contains(const USet *set, UChar32 c);

U_CAPI int32_t U_EXPORT2
uset_countRanges(const USet *set);

U_CAPI UBool U_EXPORT2
uset_getRange(const USet *set, int32_t rangeIndex,
              UChar32 *pStart, UChar32 *pLimit);

U_CAPI int32_t U_EXPORT2
uset_serialize(const USet *set, uint16_t *dest, int32_t destCapacity, UErrorCode *pErrorCode);

U_CAPI UBool U_EXPORT2
uset_getSerializedSet(USerializedSet *fillSet, const uint16_t *src, int32_t srcCapacity);

U_CAPI UBool U_EXPORT2
uset_serializedContains(const USerializedSet *set, UChar32 c);

U_CAPI int32_t U_EXPORT2
uset_countSerializedRanges(const USerializedSet *set);

U_CAPI UBool U_EXPORT2
uset_getSerializedRange(const USerializedSet *set, int32_t rangeIndex,
                        UChar32 *pStart, UChar32 *pLimit);

#endif
