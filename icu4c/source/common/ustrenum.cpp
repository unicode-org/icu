/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: November 11 2002
* Since: ICU 2.4
**********************************************************************
*/
#include "unicode/ustring.h"
#include "unicode/strenum.h"
#include "uenumimp.h"
#include "ustrenum.h"
#include "cstring.h"
#include "cmemory.h"

#define THIS(en) ((StringEnumeration*)(en->context))

U_CDECL_BEGIN

/**
 * Wrapper API to make StringEnumeration look like UEnumeration.
 */
static void U_CALLCONV
ustrenum_close(UEnumeration* en) {
    delete THIS(en);
    uprv_free(en);
}

/**
 * Wrapper API to make StringEnumeration look like UEnumeration.
 */
static int32_t U_CALLCONV
ustrenum_count(UEnumeration* en,
               UErrorCode* ec)
{
    return THIS(en)->count(*ec);
}

/**
 * Wrapper API to make StringEnumeration look like UEnumeration.
 */
static const UChar* U_CALLCONV
ustrenum_unext(UEnumeration* en,
               int32_t* resultLength,
               UErrorCode* ec)
{
    return THIS(en)->unext(resultLength, *ec);
}

/**
 * Wrapper API to make StringEnumeration look like UEnumeration.
 */
static const char* U_CALLCONV
ustrenum_next(UEnumeration* en,
              int32_t* resultLength,
              UErrorCode* ec)
{
    return THIS(en)->next(resultLength, *ec);
}

/**
 * Wrapper API to make StringEnumeration look like UEnumeration.
 */
static void U_CALLCONV
ustrenum_reset(UEnumeration* en,
               UErrorCode* ec)
{
    THIS(en)->reset(*ec);
}

/**
 * Pseudo-vtable for UEnumeration wrapper around StringEnumeration.
 * The StringEnumeration pointer will be stored in 'context'.
 */
static const UEnumeration TEMPLATE = {
    NULL,
    NULL, // store StringEnumeration pointer here
    ustrenum_close,
    ustrenum_count,
    ustrenum_unext,
    ustrenum_next,
    ustrenum_reset
};

U_CDECL_END

/**
 * Given a StringEnumeration, wrap it in a UEnumeration.  The
 * StringEnumeration is adopted; after this call, the caller must not
 * delete it (regardless of error status).
 */
U_CAPI UEnumeration* U_EXPORT2
uenum_openStringEnumeration(StringEnumeration* adopted, UErrorCode* ec) { 
    UEnumeration* result = NULL;
    if (U_SUCCESS(*ec) && adopted != NULL) {
        result = (UEnumeration*) uprv_malloc(sizeof(UEnumeration));
        if (result == NULL) {
            *ec = U_MEMORY_ALLOCATION_ERROR;
        } else {
            uprv_memcpy(result, &TEMPLATE, sizeof(TEMPLATE));
            result->context = adopted;
        }
    }
    if (result == NULL) {
        delete adopted;
    }
    return result;
}

//eof
