/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uenum.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:2
*
*   created on: 2002jul08
*   created by: Vladimir Weinstein
*/

#include "uenumimp.h"
#include "cmemory.h"

/* Layout of the baseContext buffer. */
typedef struct {
    int32_t len;  /* number of bytes available starting at 'data' */
    char    data; /* actual data starts here */
} _UEnumBuffer;

/* Extra bytes to allocate in the baseContext buffer. */
static const int32_t PAD = 8;

/* Return a pointer to the baseContext buffer, possibly allocating
   or reallocating it if at least 'capacity' bytes are not available. */
static void* _getBuffer(UEnumeration* en, int32_t capacity) {

    if (en->baseContext != NULL) {
        if (((_UEnumBuffer*) en->baseContext)->len < capacity) {
            capacity += PAD;
            en->baseContext = uprv_realloc(en->baseContext,
                                           sizeof(int32_t) + capacity);
            if (en->baseContext == NULL) {
                return NULL;
            }
            ((_UEnumBuffer*) en->baseContext)->len = capacity;
        }
    } else {
        capacity += PAD;
        en->baseContext = uprv_malloc(sizeof(int32_t) + capacity);
        if (en->baseContext == NULL) {
            return NULL;
        }
        ((_UEnumBuffer*) en->baseContext)->len = capacity;
    }
    
    return (void*) & ((_UEnumBuffer*) en->baseContext)->data;
}

U_CAPI void U_EXPORT2
uenum_close(UEnumeration* en)
{
    if (en) {
        if (en->close != NULL) {
            if (en->baseContext) {
                uprv_free(en->baseContext);
            }
            en->close(en);
        } else { /* this seems dangerous, but we better kill the object */
            uprv_free(en);
        }
    }
}

U_CAPI int32_t U_EXPORT2
uenum_count(UEnumeration* en, UErrorCode* status)
{
    if (!en || U_FAILURE(*status)) {
        return -1;
    }
    if (en->count != NULL) {
        return en->count(en, status);
    } else {
        *status = U_UNSUPPORTED_ERROR;
        return -1;
    }
}

/* Don't call this directly. Only uenum_unext should be calling this. */
U_CAPI const UChar* U_EXPORT2
uenum_unextDefault(UEnumeration* en,
            int32_t* resultLength,
            UErrorCode* status)
{
    if (en->next != NULL) {
        UChar *tempUCharVal;
        const char *tempCharVal = en->next(en, resultLength, status);
		if (tempCharVal == NULL) {
		    return NULL;
		}
        tempUCharVal = (UChar*)
            _getBuffer(en, (*resultLength+1) * sizeof(UChar));
        if (!tempUCharVal) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        u_charsToUChars(tempCharVal, tempUCharVal, *resultLength + 1);
        return tempUCharVal;
    } else {
        *status = U_UNSUPPORTED_ERROR;
        return NULL;
    }
}

/* Don't call this directly. Only uenum_next should be calling this. */
U_CAPI const char* U_EXPORT2
uenum_nextDefault(UEnumeration* en,
            int32_t* resultLength,
            UErrorCode* status)
{
    if (en->uNext != NULL) {
        char *tempCharVal;
        const UChar *tempUCharVal = en->uNext(en, resultLength, status);
		if (tempUCharVal == NULL) {
		    return NULL;
		}
        tempCharVal = (char*)
            _getBuffer(en, (*resultLength+1) * sizeof(char));
        if (!tempCharVal) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        u_UCharsToChars(tempUCharVal, tempCharVal, *resultLength + 1);
        return tempCharVal;
    } else {
        *status = U_UNSUPPORTED_ERROR;
        return NULL;
    }
}

U_CAPI const UChar* U_EXPORT2
uenum_unext(UEnumeration* en,
            int32_t* resultLength,
            UErrorCode* status)
{
    if (!en || U_FAILURE(*status)) {
        return NULL;
    }
    if (en->uNext != NULL) {
        return en->uNext(en, resultLength, status);
    } else {
        *status = U_UNSUPPORTED_ERROR;
        return NULL;
    }
}

U_CAPI const char* U_EXPORT2
uenum_next(UEnumeration* en,
          int32_t* resultLength,
          UErrorCode* status)
{
    if (!en || U_FAILURE(*status)) {
        return NULL;
    }
    if (en->next != NULL) {
        return en->next(en, resultLength, status);
    } else {
        *status = U_UNSUPPORTED_ERROR;
        return NULL;
    }
}

U_CAPI void U_EXPORT2
uenum_reset(UEnumeration* en, UErrorCode* status)
{
    if (!en || U_FAILURE(*status)) {
        return;
    }
    if (en->reset != NULL) {
        en->reset(en, status);
    } else {
        *status = U_UNSUPPORTED_ERROR;
    }
}
