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


void ucnv_cbFromUWriteBytes (UConverterFromUnicodeArgs *args,
                       const char* target,
                       int32_t length,
                       UErrorCode * err)
{

}

void ucnv_cbFromUWriteSub (UConverterFromUnicodeArgs *args,
                       UErrorCode * err)
{
}

void ucnv_cbToUWriteUChars (UConverterToUnicodeArgs *args,
                       UChar* target,
                       int32_t length,
                       UErrorCode * err)
{
}

void ucnv_cbToUWriteSub (UConverterToUnicodeArgs *args,
                       UErrorCode * err)
{
}
