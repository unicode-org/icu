/*
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
 *  ucnv_cb.c:
 *  External APIs for the ICU's codeset conversion library
 *  Helena Shih
 * 
 * Modification History:
 *
 *   Date        Name        Description
 */

/**
 * @name Character Conversion C API
 *
 */

#include "unicode/ucnv_cb.h"

/* need to update the offsets when the target moves. */
/* Note: Recursion may occur in the cb functions, be sure to update the offsets correctly
if you don't use ucnv_cbXXX functions.  Make sure you don't use the same callback within
the same call stack if the complexity arises. */
void ucnv_cbFromUWriteBytes (UConverterFromUnicodeArgs *args,
                       const char* source,
                       int32_t length,
                       int32_t offsetIndex,
                       UErrorCode * err)
{
}

void ucnv_cbFromUWriteUChars(UConverterFromUnicodeArgs *args,
                             const UChar* source,
                             int32_t length,
                             int32_t offsetIndex,
                             UErrorCode * err)
{
}

void ucnv_cbFromUWriteSub (UConverterFromUnicodeArgs *args,
                           int32_t offsetIndex,
                       UErrorCode * err)
{
}

void ucnv_cbToUWriteUChars (UConverterToUnicodeArgs *args,
                       UChar* source,
                       int32_t length,
                       int32_t offsetIndex,
                       UErrorCode * err)
{
}

void ucnv_cbToUWriteSub (UConverterToUnicodeArgs *args,
                         int32_t offsetIndex,
                       UErrorCode * err)
{
}
