/*
*******************************************************************************
*
*   Copyright (C) 2001-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ustrcase.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002feb20
*   created by: Markus W. Scherer
*
*   Implementation file for string casing C API functions.
*   Uses functions from uchar.c for basic functionality that requires access
*   to the Unicode Character Database (uprops.dat).
*/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/ubrk.h"
#include "cmemory.h"
#include "ucase.h"
#include "unormimp.h"
#include "ustr_imp.h"

/* string casing ------------------------------------------------------------ */

/* append a full case mapping result, see UCASE_MAX_STRING_LENGTH */
static U_INLINE int32_t
appendResult(UChar *dest, int32_t destIndex, int32_t destCapacity,
             int32_t result, const UChar *s) {
    UChar32 c;
    int32_t length;

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
            U16_APPEND(dest, destIndex, destCapacity, c, isError);
            if(isError) {
                /* overflow, nothing written */
                destIndex+=U16_LENGTH(c);
            }
        } else {
            /* string */
            if((destIndex+length)<=destCapacity) {
                while(length>0) {
                    dest[destIndex++]=*s++;
                    --length;
                }
            } else {
                /* overflow */
                destIndex+=length;
            }
        }
    } else {
        /* preflight */
        if(length<0) {
            destIndex+=U16_LENGTH(c);
        } else {
            destIndex+=length;
        }
    }
    return destIndex;
}

static UChar32 U_CALLCONV
utf16_caseContextIterator(void *context, int8_t dir) {
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
            U16_PREV((const UChar *)csc->p, csc->start, csc->index, c);
            return c;
        }
    } else {
        if(csc->index<csc->limit) {
            U16_NEXT((const UChar *)csc->p, csc->index, csc->limit, c);
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
 * Lowercases [srcStart..srcLimit[ but takes
 * context [0..srcLength[ into account.
 */
static int32_t
_caseMap(UCaseProps *csp, UCaseMapFull *map,
         UChar *dest, int32_t destCapacity,
         const UChar *src, UCaseContext *csc,
         int32_t srcStart, int32_t srcLimit,
         const char *locale, int32_t *locCache,
         UErrorCode *pErrorCode) {
    const UChar *s;
    UChar32 c;
    int32_t srcIndex, destIndex;

    /* case mapping loop */
    srcIndex=srcStart;
    destIndex=0;
    while(srcIndex<srcLimit) {
        csc->cpStart=srcIndex;
        U16_NEXT(src, srcIndex, srcLimit, c);
        csc->cpLimit=srcIndex;
        c=map(csp, c, utf16_caseContextIterator, csc, &s, locale, locCache);
        destIndex=appendResult(dest, destIndex, destCapacity, c, s);
    }

    if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
    return destIndex;
}

#if !UCONFIG_NO_BREAK_ITERATION

/*
 * Internal titlecasing function.
 *
 * Must get titleIter!=NULL.
 */
static int32_t
_toTitle(UCaseProps *csp,
         UChar *dest, int32_t destCapacity,
         const UChar *src, UCaseContext *csc,
         int32_t srcLength,
         UBreakIterator *titleIter,
         const char *locale, int32_t *locCache,
         UErrorCode *pErrorCode) {
    const UChar *s;
    UChar32 c;
    int32_t prev, index, destIndex;
    UBool isFirstIndex;

    /* set up local variables */
    destIndex=0;
    prev=0;
    isFirstIndex=TRUE;

    /* titlecasing loop */
    while(prev<srcLength) {
        /* find next index where to titlecase */
        if(isFirstIndex) {
            isFirstIndex=FALSE;
            index=ubrk_first(titleIter);
        } else {
            index=ubrk_next(titleIter);
        }
        if(index==UBRK_DONE || index>srcLength) {
            index=srcLength;
        }

        /* lowercase [prev..index[ */
        if(prev<index) {
            destIndex+=
                _caseMap(
                    csp, ucase_toFullLower,
                    dest+destIndex, destCapacity-destIndex,
                    src, csc,
                    prev, index,
                    locale, locCache,
                    pErrorCode);
        }

        if(index>=srcLength) {
            break;
        }

        /* titlecase the character at the found index */
        csc->cpStart=index;
        U16_NEXT(src, index, srcLength, c);
        csc->cpLimit=index;
        c=ucase_toFullTitle(csp, c, utf16_caseContextIterator, csc, &s, locale, locCache);
        destIndex=appendResult(dest, destIndex, destCapacity, c, s);

        prev=index;
    }

    if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
    return destIndex;
}

#endif

/* functions available in the common library (for unistr_case.cpp) */

U_CFUNC int32_t
ustr_toLower(UCaseProps *csp,
             UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             const char *locale,
             UErrorCode *pErrorCode) {
    UCaseContext csc={ NULL };
    int32_t locCache;

    csc.p=(void *)src;
    csc.limit=srcLength;
    locCache=0;

    return _caseMap(csp, ucase_toFullLower,
                    dest, destCapacity,
                    src, &csc, 0, srcLength,
                    locale, &locCache, pErrorCode);
}

U_CFUNC int32_t
ustr_toUpper(UCaseProps *csp,
             UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             const char *locale,
             UErrorCode *pErrorCode) {
    UCaseContext csc={ NULL };
    int32_t locCache;

    csc.p=(void *)src;
    csc.limit=srcLength;
    locCache=0;

    return _caseMap(csp, ucase_toFullUpper,
                    dest, destCapacity,
                    src, &csc, 0, srcLength,
                    locale, &locCache, pErrorCode);
}

U_CFUNC int32_t
ustr_toTitle(UCaseProps *csp,
             UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             UBreakIterator *titleIter,
             const char *locale,
             UErrorCode *pErrorCode) {
    UCaseContext csc={ NULL };
    int32_t locCache;

    csc.p=(void *)src;
    csc.limit=srcLength;
    locCache=0;

    return _toTitle(csp,
                    dest, destCapacity,
                    src, &csc, srcLength,
                    titleIter, locale, &locCache, pErrorCode);
}

U_CFUNC int32_t
ustr_foldCase(UCaseProps *csp,
              UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              uint32_t options,
              UErrorCode *pErrorCode) {
    int32_t srcIndex, destIndex;

    const UChar *s;
    UChar32 c;

    /* case mapping loop */
    srcIndex=destIndex=0;
    while(srcIndex<srcLength) {
        U16_NEXT(src, srcIndex, srcLength, c);
        c=ucase_toFullFolding(csp, c, &s, options);
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
caseMap(UChar *dest, int32_t destCapacity,
        const UChar *src, int32_t srcLength,
        UBreakIterator *titleIter,
        const char *locale,
        uint32_t options,
        int32_t toWhichCase,
        UErrorCode *pErrorCode) {
    UChar buffer[300];
    UChar *temp;

    UCaseProps *csp;

    int32_t destLength;
    UBool ownTitleIter;

    /* check argument values */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
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

    csp=ucase_getSingleton(pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /* get the string length */
    if(srcLength==-1) {
        srcLength=u_strlen(src);
    }

    /* check for overlapping source and destination */
    if( dest!=NULL &&
        ((src>=dest && src<(dest+destCapacity)) ||
         (dest>=src && dest<(src+srcLength)))
    ) {
        /* overlap: provide a temporary destination buffer and later copy the result */
        if(destCapacity<=(sizeof(buffer)/U_SIZEOF_UCHAR)) {
            /* the stack buffer is large enough */
            temp=buffer;
        } else {
            /* allocate a buffer */
            temp=(UChar *)uprv_malloc(destCapacity*U_SIZEOF_UCHAR);
            if(temp==NULL) {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                return 0;
            }
        }
    } else {
        temp=dest;
    }

    ownTitleIter=FALSE;
    destLength=0;

    if(toWhichCase==FOLD_CASE) {
        destLength=ustr_foldCase(csp, temp, destCapacity, src, srcLength,
                                 options, pErrorCode);
    } else {
        UCaseContext csc={ NULL };
        int32_t locCache;

        csc.p=(void *)src;
        csc.limit=srcLength;
        locCache=0;

        if(toWhichCase==TO_LOWER) {
            destLength=_caseMap(csp, ucase_toFullLower,
                                temp, destCapacity,
                                src, &csc,
                                0, srcLength,
                                locale, &locCache, pErrorCode);
        } else if(toWhichCase==TO_UPPER) {
            destLength=_caseMap(csp, ucase_toFullUpper,
                                temp, destCapacity,
                                src, &csc,
                                0, srcLength,
                                locale, &locCache, pErrorCode);
        } else /* if(toWhichCase==TO_TITLE) */ {
    #if UCONFIG_NO_BREAK_ITERATION
            *pErrorCode=U_UNSUPPORTED_ERROR;
    #else
            if(titleIter==NULL) {
                titleIter=ubrk_open(UBRK_WORD, locale,
                                    src, srcLength,
                                    pErrorCode);
                ownTitleIter=(UBool)U_SUCCESS(*pErrorCode);
            }
            if(U_SUCCESS(*pErrorCode)) {
                destLength=_toTitle(csp, temp, destCapacity,
                                    src, &csc, srcLength,
                                    titleIter, locale, &locCache, pErrorCode);
            }
    #endif
        }
    }
    if(temp!=dest) {
        /* copy the result string to the destination buffer */
        if(destLength>0) {
            int32_t copyLength= destLength<=destCapacity ? destLength : destCapacity;
            if(copyLength>0) {
                uprv_memmove(dest, temp, copyLength*U_SIZEOF_UCHAR);
            }
        }
        if(temp!=buffer) {
            uprv_free(temp);
        }
    }

#if !UCONFIG_NO_BREAK_ITERATION
    if(ownTitleIter) {
        ubrk_close(titleIter);
    }
#endif

    return u_terminateUChars(dest, destCapacity, destLength, pErrorCode);
}

/* public API functions */

U_CAPI int32_t U_EXPORT2
u_strToLower(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             const char *locale,
             UErrorCode *pErrorCode) {
    return caseMap(dest, destCapacity,
                   src, srcLength,
                   NULL, locale, 0,
                   TO_LOWER, pErrorCode);
}

U_CAPI int32_t U_EXPORT2
u_strToUpper(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             const char *locale,
             UErrorCode *pErrorCode) {
    return caseMap(dest, destCapacity,
                   src, srcLength,
                   NULL, locale, 0,
                   TO_UPPER, pErrorCode);
}

#if !UCONFIG_NO_BREAK_ITERATION

U_CAPI int32_t U_EXPORT2
u_strToTitle(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             UBreakIterator *titleIter,
             const char *locale,
             UErrorCode *pErrorCode) {
    return caseMap(dest, destCapacity,
                   src, srcLength,
                   titleIter, locale, 0,
                   TO_TITLE, pErrorCode);
}

#endif

U_CAPI int32_t U_EXPORT2
u_strFoldCase(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              uint32_t options,
              UErrorCode *pErrorCode) {
    return caseMap(dest, destCapacity,
                   src, srcLength,
                   NULL, NULL, options,
                   FOLD_CASE, pErrorCode);
}

/* case-insensitive string comparisons */

U_CAPI int32_t U_EXPORT2
u_strCaseCompare(const UChar *s1, int32_t length1,
                 const UChar *s2, int32_t length2,
                 uint32_t options,
                 UErrorCode *pErrorCode) {
    /* argument checking */
    if(pErrorCode==0 || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(s1==NULL || length1<-1 || s2==NULL || length2<-1) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    return unorm_cmpEquivFold(s1, length1, s2, length2,
                              options|U_COMPARE_IGNORE_CASE,
                              pErrorCode);
}

U_CAPI int32_t U_EXPORT2
u_strcasecmp(const UChar *s1, const UChar *s2, uint32_t options) {
    UErrorCode errorCode=U_ZERO_ERROR;
    return unorm_cmpEquivFold(s1, -1, s2, -1,
                              options|U_COMPARE_IGNORE_CASE,
                              &errorCode);
}

U_CAPI int32_t U_EXPORT2
u_memcasecmp(const UChar *s1, const UChar *s2, int32_t length, uint32_t options) {
    UErrorCode errorCode=U_ZERO_ERROR;
    return unorm_cmpEquivFold(s1, length, s2, length,
                              options|U_COMPARE_IGNORE_CASE,
                              &errorCode);
}

U_CAPI int32_t U_EXPORT2
u_strncasecmp(const UChar *s1, const UChar *s2, int32_t n, uint32_t options) {
    UErrorCode errorCode=U_ZERO_ERROR;
    return unorm_cmpEquivFold(s1, n, s2, n,
                              options|(U_COMPARE_IGNORE_CASE|_STRNCMP_STYLE),
                              &errorCode);
}
