/*
*******************************************************************************
*
*   Copyright (C) 2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucasemap.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2005may06
*   created by: Markus W. Scherer
*
*   Case mapping service object and functions using it.
*/

#include "unicode/utypes.h"
#include "unicode/uloc.h"
#include "unicode/ustring.h"
#include "unicode/ucasemap.h"
#include "cmemory.h"
#include "cstring.h"
#include "ucase.h"
#include "ustr_imp.h"

/* UCaseMap service object -------------------------------------------------- */

struct UCaseMap {
    const UCaseProps *csp;
    char locale[32];
    int32_t locCache;
    uint32_t options;
};

U_DRAFT UCaseMap * U_EXPORT2
ucasemap_open(const char *locale, uint32_t options, UErrorCode *pErrorCode) {
    UCaseMap *csm;

    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }

    csm=(UCaseMap *)uprv_malloc(sizeof(UCaseMap));
    if(csm==NULL) {
        return NULL;
    }
    uprv_memset(csm, 0, sizeof(UCaseMap));

    csm->csp=ucase_getSingleton(pErrorCode);
    ucasemap_setLocale(csm, locale, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        uprv_free(csm);
        return NULL;
    }

    csm->options=options;
    return csm;
}

U_DRAFT void U_EXPORT2
ucasemap_close(UCaseMap *csm) {
    if(csm!=NULL) {
        uprv_free(csm);
    }
}

U_DRAFT const char * U_EXPORT2
ucasemap_getLocale(const UCaseMap *csm) {
    return csm->locale;
}

U_DRAFT uint32_t U_EXPORT2
ucasemap_getOptions(const UCaseMap *csm) {
    return csm->options;
}

U_DRAFT void U_EXPORT2
ucasemap_setLocale(UCaseMap *csm, const char *locale, UErrorCode *pErrorCode) {
    int32_t length;

    if(U_FAILURE(*pErrorCode)) {
        return;
    }

    length=uloc_getName(locale, csm->locale, (int32_t)sizeof(csm->locale), pErrorCode);
    if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR || length==sizeof(csm->locale)) {
        *pErrorCode=U_ZERO_ERROR;
        /* we only really need the language code for case mappings */
        length=uloc_getLanguage(locale, csm->locale, (int32_t)sizeof(csm->locale), pErrorCode);
    }
    if(length==sizeof(csm->locale)) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
    csm->locCache=0;
    if(U_SUCCESS(*pErrorCode)) {
        ucase_getCaseLocale(csm->locale, &csm->locCache);
    } else {
        csm->locale[0]=0;
    }
}

U_DRAFT void U_EXPORT2
ucasemap_setOptions(UCaseMap *csm, uint32_t options, UErrorCode *pErrorCode) {
    csm->options=options;
}

/* UTF-8 string case mappings ----------------------------------------------- */

/* append a full case mapping result, see UCASE_MAX_STRING_LENGTH */
static U_INLINE int32_t
appendResult(uint8_t *dest, int32_t destIndex, int32_t destCapacity,
             int32_t result, const UChar *s) {
    UChar32 c;
    int32_t length, destLength;
    UErrorCode errorCode;

    /* decode the result */
    if(result<0) {
        /* (not) original code point */
        c=~result;
        length=-1;
    } else if(result<=UCASE_MAX_STRING_LENGTH) {
        c=U_SENTINEL;
        length=result;
    } else {
        c=result;
        length=-1;
    }

    if(destIndex<destCapacity) {
        /* append the result */
        if(length<0) {
            /* code point */
            UBool isError=FALSE;
            U8_APPEND(dest, destIndex, destCapacity, c, isError);
            if(isError) {
                /* overflow, nothing written */
                destIndex+=U8_LENGTH(c);
            }
        } else {
            /* string */
            errorCode=U_ZERO_ERROR;
            u_strToUTF8(
                (char *)(dest+destIndex), destCapacity-destIndex, &destLength,
                s, length,
                &errorCode);
            destIndex+=length;
            /* we might have an overflow, but we know the actual length */
        }
    } else {
        /* preflight */
        if(length<0) {
            destIndex+=U8_LENGTH(c);
        } else {
            errorCode=U_ZERO_ERROR;
            u_strToUTF8(
                NULL, 0, &destLength,
                s, length,
                &errorCode);
            destIndex+=length;
        }
    }
    return destIndex;
}

static UChar32 U_CALLCONV
utf8_caseContextIterator(void *context, int8_t dir) {
    UCaseContext *csc=(UCaseContext *)context;
    UChar32 c;

    if(dir<0) {
        /* reset for backward iteration */
        csc->index=csc->cpStart;
        csc->dir=dir;
    } else if(dir>0) {
        /* reset for forward iteration */
        csc->index=csc->cpLimit;
        csc->dir=dir;
    } else {
        /* continue current iteration direction */
        dir=csc->dir;
    }

    if(dir<0) {
        if(csc->start<csc->index) {
            U8_PREV((const uint8_t *)csc->p, csc->start, csc->index, c);
            return c;
        }
    } else {
        if(csc->index<csc->limit) {
            U8_NEXT((const uint8_t *)csc->p, csc->index, csc->limit, c);
            return c;
        }
    }
    return U_SENTINEL;
}

typedef int32_t U_CALLCONV
UCaseMapFull(const UCaseProps *csp, UChar32 c,
             UCaseContextIterator *iter, void *context,
             const UChar **pString,
             const char *locale, int32_t *locCache);

/*
 * Case-maps [srcStart..srcLimit[ but takes
 * context [0..srcLength[ into account.
 */
static int32_t
_caseMap(const UCaseMap *csm, UCaseMapFull *map,
         uint8_t *dest, int32_t destCapacity,
         const uint8_t *src, UCaseContext *csc,
         int32_t srcStart, int32_t srcLimit,
         UErrorCode *pErrorCode) {
    const UChar *s;
    UChar32 c;
    int32_t srcIndex, destIndex;
    int32_t locCache;

    locCache=csm->locCache;

    /* case mapping loop */
    srcIndex=srcStart;
    destIndex=0;
    while(srcIndex<srcLimit) {
        csc->cpStart=srcIndex;
        U8_NEXT(src, srcIndex, srcLimit, c);
        csc->cpLimit=srcIndex;
        c=map(csm->csp, c, utf8_caseContextIterator, csc, &s, csm->locale, &locCache);
        destIndex=appendResult(dest, destIndex, destCapacity, c, s);
    }

    if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
    return destIndex;
}

/*
 * Implement argument checking and buffer handling
 * for string case mapping as a common function.
 */
enum {
    TO_LOWER,
    TO_UPPER,
    TO_TITLE,
    FOLD_CASE
};

/* common internal function for public API functions */

static int32_t
caseMap(const UCaseMap *csm,
        uint8_t *dest, int32_t destCapacity,
        const uint8_t *src, int32_t srcLength,
        int32_t toWhichCase,
        UErrorCode *pErrorCode) {
    UCaseContext csc={ NULL };
    int32_t destLength;

    /* check argument values */
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if( destCapacity<0 ||
        (dest==NULL && destCapacity>0) ||
        src==NULL ||
        srcLength<-1
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* get the string length */
    if(srcLength==-1) {
        srcLength=uprv_strlen((const char *)src);
    }

    /* check for overlapping source and destination */
    if( dest!=NULL &&
        ((src>=dest && src<(dest+destCapacity)) ||
         (dest>=src && dest<(src+srcLength)))
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    destLength=0;

    csc.p=(void *)src;
    csc.limit=srcLength;

    if(toWhichCase==TO_LOWER) {
        destLength=_caseMap(csm, ucase_toFullLower,
                            dest, destCapacity,
                            src, &csc,
                            0, srcLength,
                            pErrorCode);
    } else /* if(toWhichCase==TO_UPPER) */ {
        destLength=_caseMap(csm, ucase_toFullUpper,
                            dest, destCapacity,
                            src, &csc,
                            0, srcLength,
                            pErrorCode);
    }

    return u_terminateChars((char *)dest, destCapacity, destLength, pErrorCode);
}

/* public API functions */

U_DRAFT int32_t U_EXPORT2
ucasemap_utf8ToLower(const UCaseMap *csm,
                     char *dest, int32_t destCapacity,
                     const char *src, int32_t srcLength,
                     UErrorCode *pErrorCode) {
    return caseMap(csm,
                   (uint8_t *)dest, destCapacity,
                   (const uint8_t *)src, srcLength,
                   TO_LOWER, pErrorCode);
}

U_DRAFT int32_t U_EXPORT2
ucasemap_utf8ToUpper(const UCaseMap *csm,
                     char *dest, int32_t destCapacity,
                     const char *src, int32_t srcLength,
                     UErrorCode *pErrorCode) {
    return caseMap(csm,
                   (uint8_t *)dest, destCapacity,
                   (const uint8_t *)src, srcLength,
                   TO_UPPER, pErrorCode);
}
