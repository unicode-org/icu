/*
*******************************************************************************
*
*   Copyright (C) 2001-2003, International Business Machines
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
#include "unormimp.h"
#include "ustr_imp.h"

/* string casing ------------------------------------------------------------ */

#if !UCONFIG_NO_BREAK_ITERATION

/*
 * Internal titlecasing function,
 * using u_internalStrToLower() and u_internalToTitle().
 *
 * Must get titleIter!=NULL.
 */
U_CFUNC int32_t
u_internalStrToTitle(UChar *dest, int32_t destCapacity,
                     const UChar *src, int32_t srcLength,
                     UBreakIterator *titleIter,
                     const char *locale,
                     UErrorCode *pErrorCode) {
    UCharIterator iter;
    UChar32 c;
    int32_t prev, index, destIndex, length;
    UBool isFirstIndex;

    /* set up local variables */
    uiter_setString(&iter, src, srcLength);
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
            if(destIndex<destCapacity) {
                length=u_internalStrToLower(dest+destIndex, destCapacity-destIndex,
                                            src, srcLength,
                                            prev, index,
                                            locale,
                                            pErrorCode);
            } else {
                length=u_internalStrToLower(NULL, 0,
                                            src, srcLength,
                                            prev, index,
                                            locale,
                                            pErrorCode);
            }
            destIndex+=length;
        }

        if(index>=srcLength) {
            break;
        }

        /* titlecase the character at the found index */
        UTF_NEXT_CHAR(src, index, srcLength, c);
        iter.move(&iter, index, UITER_ZERO);
        if(destIndex<destCapacity) {
            length=u_internalToTitle(c, &iter,
                                     dest+destIndex, destCapacity-destIndex,
                                     locale);
        } else {
            length=u_internalToTitle(c, &iter, NULL, 0, locale);
        }
        if(length<0) {
            length=-length;
        }
        destIndex+=length;

        prev=index;
    }

    return destIndex;
}

#endif

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

static int32_t
u_strCaseMap(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             UBreakIterator *titleIter,
             const char *locale,
             uint32_t options,
             int32_t toWhichCase,
             UErrorCode *pErrorCode) {
    UChar buffer[300];
    UChar *temp;
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

    if(toWhichCase==TO_LOWER) {
        destLength=u_internalStrToLower(temp, destCapacity,
                                        src, srcLength,
                                        0, srcLength,
                                        locale, pErrorCode);
    } else if(toWhichCase==TO_UPPER) {
        destLength=u_internalStrToUpper(temp, destCapacity, src, srcLength,
                                        locale, pErrorCode);
#if !UCONFIG_NO_BREAK_ITERATION
    } else if(toWhichCase==TO_TITLE) {
        if(titleIter==NULL) {
            titleIter=ubrk_open(UBRK_WORD, locale,
                                src, srcLength,
                                pErrorCode);
            ownTitleIter=(UBool)U_SUCCESS(*pErrorCode);
        }
        if(U_SUCCESS(*pErrorCode)) {
            destLength=u_internalStrToTitle(temp, destCapacity, src, srcLength,
                                            titleIter, locale, pErrorCode);
        }
#endif
    } else {
        destLength=u_internalStrFoldCase(temp, destCapacity, src, srcLength,
                                         options, pErrorCode);
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

U_CAPI int32_t U_EXPORT2
u_strToLower(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             const char *locale,
             UErrorCode *pErrorCode) {
    return u_strCaseMap(dest, destCapacity,
                        src, srcLength,
                        NULL, locale, 0,
                        TO_LOWER, pErrorCode);
}

U_CAPI int32_t U_EXPORT2
u_strToUpper(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             const char *locale,
             UErrorCode *pErrorCode) {
    return u_strCaseMap(dest, destCapacity,
                        src, srcLength,
                        NULL, locale, 0,
                        TO_UPPER, pErrorCode);
}

U_CAPI int32_t U_EXPORT2
u_strToTitle(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             UBreakIterator *titleIter,
             const char *locale,
             UErrorCode *pErrorCode) {
    return u_strCaseMap(dest, destCapacity,
                        src, srcLength,
                        titleIter, locale, 0,
                        TO_TITLE, pErrorCode);
}

U_CAPI int32_t U_EXPORT2
u_strFoldCase(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              uint32_t options,
              UErrorCode *pErrorCode) {
    return u_strCaseMap(dest, destCapacity,
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
