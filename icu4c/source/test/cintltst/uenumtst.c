/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uenumtst.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:2
*
*   created on: 2002jul08
*   created by: Vladimir Weinstein
*/

#include "cintltst.h"
#include "uenumimp.h"
#include "cmemory.h"
#include "cstring.h"

static const char* test1[] = {
    "first",
    "second",
    "third",
    "fourth"
};

struct chArrayContext {
    int32_t currIndex;
    int32_t maxIndex;
    char *currChar;
    UChar *currUChar;
    char **array;
};

typedef struct chArrayContext chArrayContext;

#define cont ((chArrayContext *)en->context)

static void U_CALLCONV
chArrayClose(UEnumeration *en) {
    if(cont->currUChar != NULL) {
        free(cont->currUChar);
    }
    free(en);
}

static int32_t U_CALLCONV
chArrayCount(UEnumeration *en, UErrorCode *status) {
    return cont->maxIndex;
}

static const UChar* U_CALLCONV 
chArrayUNext(UEnumeration *en, int32_t *resultLength, UErrorCode *status) {
    if(cont->currIndex >= cont->maxIndex) {
        return NULL;
    }
    
    if(cont->currUChar == NULL) {
        cont->currUChar = (UChar *)malloc(1024*sizeof(UChar));
    }
    
    cont->currChar = (cont->array)[cont->currIndex];
    *resultLength = uprv_strlen(cont->currChar);
    u_charsToUChars(cont->currChar, cont->currUChar, *resultLength);
    cont->currIndex++;
    return cont->currUChar;
}

static const char* U_CALLCONV
chArrayNext(UEnumeration *en, int32_t *resultLength, UErrorCode *status) {
    if(cont->currIndex >= cont->maxIndex) {
        return NULL;
    }
    
    cont->currChar = (cont->array)[cont->currIndex];
    *resultLength = uprv_strlen(cont->currChar);
    cont->currIndex++;
    return cont->currChar;
}

static void U_CALLCONV
chArrayReset(UEnumeration *en, UErrorCode *status) {
    cont->currIndex = 0;
}

chArrayContext myCont = {
    0, 0,
    NULL, NULL,
    NULL
};

UEnumeration chEnum = {
    NULL,
    &myCont,
    chArrayClose,
    chArrayCount,
    chArrayUNext,
    chArrayNext,
    chArrayReset
};

static const UEnumeration emptyEnumerator = {
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
};

static const UEnumeration emptyPartialEnumerator = {
    NULL,
    NULL,
    NULL,
    NULL,
    uenum_unextDefault,
    NULL,
    NULL,
};


static UEnumeration *getchArrayEnum(const char** source, int32_t size) {
    UEnumeration *en = (UEnumeration *)malloc(sizeof(UEnumeration));
    memcpy(en, &chEnum, sizeof(UEnumeration));
    cont->array = (char **)source;
    cont->maxIndex = size;
    return en;
}

static void EnumerationTest(void) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t len = 0;
    UEnumeration *en = getchArrayEnum(test1, sizeof(test1)/sizeof(test1[0]));
    const char *string = NULL;
    const UChar *uString = NULL;
    while ((string = uenum_next(en, &len, &status))) {
        log_verbose("read %s, length %i\n", string, len);
    }
    uenum_reset(en, &status);
    while ((uString = uenum_unext(en, &len, &status))) {
        log_verbose("read uchar of len %i\n", len);
    }
    
    uenum_close(en);
}

static void EmptyEnumerationTest(void) {
    UErrorCode status = U_ZERO_ERROR;
    UEnumeration *emptyEnum = uprv_malloc(sizeof(UEnumeration));

    uprv_memcpy(emptyEnum, &emptyEnumerator, sizeof(UEnumeration));
    if (uenum_count(emptyEnum, &status) != -1 || status != U_UNSUPPORTED_ERROR) {
        log_err("uenum_count failed\n");
    }
    status = U_ZERO_ERROR;
    if (uenum_next(emptyEnum, NULL, &status) != NULL || status != U_UNSUPPORTED_ERROR) {
        log_err("uenum_next failed\n");
    }
    status = U_ZERO_ERROR;
    if (uenum_unext(emptyEnum, NULL, &status) != NULL || status != U_UNSUPPORTED_ERROR) {
        log_err("uenum_unext failed\n");
    }
    status = U_ZERO_ERROR;
    uenum_reset(emptyEnum, &status);
    if (status != U_UNSUPPORTED_ERROR) {
        log_err("uenum_reset failed\n");
    }
    uenum_close(emptyEnum);

    status = U_ZERO_ERROR;
    if (uenum_next(NULL, NULL, &status) != NULL || status != U_ZERO_ERROR) {
        log_err("uenum_next(NULL) failed\n");
    }
    status = U_ZERO_ERROR;
    if (uenum_unext(NULL, NULL, &status) != NULL || status != U_ZERO_ERROR) {
        log_err("uenum_unext(NULL) failed\n");
    }
    status = U_ZERO_ERROR;
    uenum_reset(NULL, &status);
    if (status != U_ZERO_ERROR) {
        log_err("uenum_reset(NULL) failed\n");
    }

    emptyEnum = uprv_malloc(sizeof(UEnumeration));
    uprv_memcpy(emptyEnum, &emptyPartialEnumerator, sizeof(UEnumeration));
    status = U_ZERO_ERROR;
    if (uenum_unext(emptyEnum, NULL, &status) != NULL || status != U_UNSUPPORTED_ERROR) {
        log_err("partial uenum_unext failed\n");
    }
    uenum_close(emptyEnum);
}

void addEnumerationTest(TestNode** root);

void addEnumerationTest(TestNode** root)
{
    addTest(root, &EnumerationTest, "tsutil/uenumtst/EnumerationTest");
    addTest(root, &EmptyEnumerationTest, "tsutil/uenumtst/EmptyEnumerationTest");
}
