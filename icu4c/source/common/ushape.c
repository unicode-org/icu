/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ushape.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jun29
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unicode/ushape.h"

U_CAPI int32_t U_EXPORT2
u_shapeArabic(const UChar *source, int32_t sourceLength,
              UChar *dest, int32_t destSize,
              uint32_t options,
              UErrorCode *pErrorCode) {
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    *pErrorCode=U_UNSUPPORTED_ERROR;
    return 0;

}
