/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
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
#include "uassert.h"

// StringEnumeration implementation ---------------------------------------- ***

StringEnumeration::StringEnumeration()
    : chars(charsBuffer), charsCapacity(sizeof(charsBuffer)) {
}

StringEnumeration::~StringEnumeration() {
    if (chars != NULL && chars != charsBuffer) {
        uprv_free(chars);
    }
}

// StringEnumeration base class clone() default implementation, does not clone
StringEnumeration *
StringEnumeration::clone() const {
  return NULL;
}

const char *
StringEnumeration::next(int32_t *resultLength, UErrorCode &status) {
    const UnicodeString *s=snext(status);
    if(s!=NULL) {
        unistr=*s;
        ensureCharsCapacity(unistr.length()+1, status);
        if(U_SUCCESS(status)) {
            if(resultLength!=NULL) {
                *resultLength=unistr.length();
            }
            unistr.extract(0, INT32_MAX, chars, charsCapacity, "");
            return chars;
        }
    }

    return NULL;
}

const UChar *
StringEnumeration::unext(int32_t *resultLength, UErrorCode &status) {
    const UnicodeString *s=snext(status);
    if(s!=NULL) {
        unistr=*s;
        if(U_SUCCESS(status)) {
            if(resultLength!=NULL) {
                *resultLength=unistr.length();
            }
            return unistr.getTerminatedBuffer();
        }
    }

    return NULL;
}

void
StringEnumeration::ensureCharsCapacity(int32_t capacity, UErrorCode &status) {
    if(U_SUCCESS(status) && capacity>charsCapacity) {
        if(capacity<(charsCapacity+charsCapacity/2)) {
            // avoid allocation thrashing
            capacity=charsCapacity+charsCapacity/2;
        }
        if(chars!=charsBuffer) {
            uprv_free(chars);
        }
        chars=(char *)uprv_malloc(capacity);
        if(chars==NULL) {
            chars=charsBuffer;
            charsCapacity=sizeof(charsBuffer);
            status=U_MEMORY_ALLOCATION_ERROR;
        } else {
            charsCapacity=capacity;
        }
    }
}

UnicodeString *
StringEnumeration::setChars(const char *s, int32_t length, UErrorCode &status) {
    if(U_SUCCESS(status) && s!=NULL) {
        if(length<0) {
            length=uprv_strlen(s);
        }

        UChar *buffer=unistr.getBuffer(length+1);
        if(buffer!=NULL) {
            u_charsToUChars(s, buffer, length);
            buffer[length]=0;
            unistr.releaseBuffer(length);
            return &unistr;
        } else {
            status=U_MEMORY_ALLOCATION_ERROR;
        }
    }

    return NULL;
}

// UStringEnumeration implementation --------------------------------------- ***

UStringEnumeration::UStringEnumeration(UEnumeration* _uenum) :
    uenum(_uenum) {
    U_ASSERT(_uenum != 0);
}

UStringEnumeration::~UStringEnumeration() {
    uenum_close(uenum);
}

int32_t UStringEnumeration::count(UErrorCode& status) const {
    return uenum_count(uenum, &status);
}

const UnicodeString* UStringEnumeration::snext(UErrorCode& status) {
    int32_t length;
    const UChar* str = uenum_unext(uenum, &length, &status);
    if (str == 0 || U_FAILURE(status)) {
        return 0;
    }
    return &unistr.setTo(str, length);
}

void UStringEnumeration::reset(UErrorCode& status) {
    uenum_reset(uenum, &status);
}

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(UStringEnumeration/*, StringEnumeration*/)

// C wrapper --------------------------------------------------------------- ***

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
static const UEnumeration USTRENUM_VT = {
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
            uprv_memcpy(result, &USTRENUM_VT, sizeof(USTRENUM_VT));
            result->context = adopted;
        }
    }
    if (result == NULL) {
        delete adopted;
    }
    return result;
}

// C wrapper --------------------------------------------------------------- ***

U_CDECL_BEGIN

typedef struct UCharStringEnumeration {
    UEnumeration uenum;
    int32_t index, count;
} UCharStringEnumeration;

static void U_CALLCONV
ucharstrenum_close(UEnumeration* en) {
    uprv_free(en);
}

static int32_t U_CALLCONV
ucharstrenum_count(UEnumeration* en,
                   UErrorCode* /*ec*/) {
    return ((UCharStringEnumeration*)en)->count;
}

static const char* U_CALLCONV
ucharstrenum_next(UEnumeration* en,
                  int32_t* resultLength,
                  UErrorCode* /*ec*/) {
    UCharStringEnumeration *e = (UCharStringEnumeration*) en;
    if (e->index >= e->count) {
        return NULL;
    }
    const char* result = ((const char**)e->uenum.context)[e->index++];
    if (resultLength) {
        *resultLength = uprv_strlen(result);
    }
    return result;
}

static void U_CALLCONV
ucharstrenum_reset(UEnumeration* en,
                   UErrorCode* /*ec*/) {
    ((UCharStringEnumeration*)en)->index = 0;
}

static const UEnumeration UCHARSTRENUM_VT = {
    NULL,
    NULL, // store StringEnumeration pointer here
    ucharstrenum_close,
    ucharstrenum_count,
    uenum_unextDefault,
    ucharstrenum_next,
    ucharstrenum_reset
};

U_CDECL_END

U_CAPI UEnumeration* U_EXPORT2
uenum_openCharStringsEnumeration(const char** strings, int32_t count,
                                 UErrorCode* ec) {
    UCharStringEnumeration* result = NULL;
    if (U_SUCCESS(*ec) && count >= 0 && (count == 0 || strings != 0)) {
        result = (UCharStringEnumeration*) uprv_malloc(sizeof(UCharStringEnumeration));
        if (result == NULL) {
            *ec = U_MEMORY_ALLOCATION_ERROR;
        } else {
            U_ASSERT((char*)result==(char*)(&result->uenum));
            uprv_memcpy(result, &UCHARSTRENUM_VT, sizeof(UCHARSTRENUM_VT));
            result->uenum.context = strings;
            result->index = 0;
            result->count = count;
        }
    }
    return (UEnumeration*) result;
}

// The following has not been tested and is not used yet. [alan]
// 
// // StringArrayEnumeration implementation ----------------------------------- ***
// 
// StringArrayEnumeration::StringArrayEnumeration(const UChar** _strings, int32_t _count,
//                                                UErrorCode& status) :
//     strings(0), stringCount(-1), pos(0) {
//     if (U_SUCCESS(status)) {
//         if ((_count > 0 && _strings == 0) ||
//             (_count < 0)) {
//             status = U_ILLEGAL_ARGUMENT_ERROR;
//         } else {
//             stringCount = _count;
//             strings = _strings;
//         }
//     }
// }
// 
// StringArrayEnumeration::~StringArrayEnumeration() {
//     // nothing to do; strings is an alias
// }
// 
// StringEnumeration *StringArrayEnumeration::clone() const {
//     UErrorCode ec = U_ZERO_ERROR;
//     StringEnumeration *e = new StringArrayEnumeration(strings, stringCount, ec);
//     if (U_FAILURE(ec)) {
//         delete e;
//         e = NULL;
//     }
//     return e;
// }
// 
// int32_t StringArrayEnumeration::count(UErrorCode& /*status*/) const {
//     return stringCount;
// }
// 
// const UnicodeString* StringArrayEnumeration::snext(UErrorCode& status) {
//     if (U_FAILURE(status)) {
//         return NULL;
//     }
//     if (pos < stringCount) {
//         return &unistr.setTo(strings[pos++], -1);
//     } else {
//         return NULL;
//     }
// }
// 
// void StringArrayEnumeration::reset(UErrorCode& /*status*/) {
//     pos = 0;
// }
// 
// UOBJECT_DEFINE_RTTI_IMPLEMENTATION(StringArrayEnumeration/*, StringEnumeration*/)

//eof
