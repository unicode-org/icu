/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
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
#include "ustr_imp.h"

/* string casing ------------------------------------------------------------ */

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
    } else if(toWhichCase==TO_TITLE) {
        if(titleIter==NULL) {
            titleIter=ubrk_open(UBRK_TITLE, locale,
                                src, srcLength,
                                pErrorCode);
            ownTitleIter=(UBool)U_SUCCESS(*pErrorCode);
        }
        if(U_SUCCESS(*pErrorCode)) {
            destLength=u_internalStrToTitle(temp, destCapacity, src, srcLength,
                                            titleIter, locale, pErrorCode);
        }
    } else {
        destLength=u_internalStrFoldCase(temp, destCapacity, src, srcLength,
                                         options, pErrorCode);
    }
    if(temp!=dest) {
        /* copy the result string to the destination buffer */
        if(destLength>0) {
            uprv_memmove(dest, temp, destLength*U_SIZEOF_UCHAR);
        }
        if(temp!=buffer) {
            uprv_free(temp);
        }
    }

    if(ownTitleIter) {
        ubrk_close(titleIter);
    }

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
u_strcasecmp(const UChar *s1, const UChar *s2, uint32_t options) {
    UChar t1[32], t2[32]; /* temporary buffers holding case-folded parts of s1 and s2 */
    UChar32 c;
    UChar uc;
    int32_t pos1, pos2, len1, len2, result;

    if(!uprv_haveProperties()) {
        /* hardcode ASCII strcasecmp() */
        UChar c1, c2;

        for(;;) {
            c1=*s1++;
            if((uint16_t)(c1-0x41)<26) {
                c1+=0x20;
            }
            c2=*s2++;
            if((uint16_t)(c2-0x41)<26) {
                c2+=0x20;
            }
            result=(int32_t)c1-(int32_t)c2;
            if(result!=0 || c1==0) {
                return result;
            }
        }
    }

    pos1=pos2=len1=len2=0;
    for(;;) {
        /* make sure that the temporary buffers are not empty */
        if(pos1>=len1) {
            c=*s1++;
            if(c!=0) {
                if(UTF_IS_FIRST_SURROGATE(c) && UTF_IS_SECOND_SURROGATE(uc=*s1)) {
                    c=UTF16_GET_PAIR_VALUE(c, uc);
                    ++s1;
                }
                len1=u_internalFoldCase(c, t1, 32, options);
                if(len1<0) {
                    len1=-len1;
                }
                pos1=0;
            } else if(pos2>=len2 && *s2==0) {
                return 0;
            } else {
                return -1;
            }
        }
        if(pos2>=len2) {
            c=*s2++;
            if(c!=0) {
                if(UTF_IS_FIRST_SURROGATE(c) && UTF_IS_SECOND_SURROGATE(uc=*s2)) {
                    c=UTF16_GET_PAIR_VALUE(c, uc);
                    ++s2;
                }
                len2=u_internalFoldCase(c, t2, 32, options);
                if(len2<0) {
                    len2=-len2;
                }
                pos2=0;
            } else {
                return 1;
            }
        }

        /* compare the head code units from both folded strings */
        result=(int32_t)t1[pos1++]-(int32_t)t2[pos2++];
        if(result!=0) {
            return result;
        }
    }
}

U_CFUNC int32_t
u_internalStrcasecmp(const UChar *s1, int32_t length1,
                     const UChar *s2, int32_t length2,
                     uint32_t options) {
    UChar t1[32], t2[32]; /* temporary buffers holding case-folded parts of s1 and s2 */
    UChar32 c;
    UChar uc;
    int32_t pos1, pos2, len1, len2, result;

    if(!uprv_haveProperties()) {
        /* hardcode ASCII strcasecmp() */
        UChar c1, c2;

        for(;;) {
            if(length1<=0) {
                if(length2<=0) {
                    return 0;
                } else {
                    return -1;
                }
            } else if(length2<=0) {
                return 1;
            }

            c1=*s1++;
            if((uint16_t)(c1-0x41)<26) {
                c1+=0x20;
            }
            c2=*s2++;
            if((uint16_t)(c2-0x41)<26) {
                c2+=0x20;
            }
            result=(int32_t)c1-(int32_t)c2;
            if(result!=0) {
                return result;
            }

            --length1;
            --length2;
        }
    }

    pos1=pos2=len1=len2=0;
    for(;;) {
        /* make sure that the temporary buffers are not empty */
        if(pos1>=len1) {
            if(length1>0) {
                c=*s1++;
                if(UTF_IS_FIRST_SURROGATE(c) && length1>1 && UTF_IS_SECOND_SURROGATE(uc=*s1)) {
                    c=UTF16_GET_PAIR_VALUE(c, uc);
                    ++s1;
                    length1-=2;
                } else {
                    --length1;
                }
                len1=u_internalFoldCase(c, t1, 32, options);
                if(len1<0) {
                    len1=-len1;
                }
                pos1=0;
            } else if(pos2>=len2 && length2<=0) {
                return 0;
            } else {
                return -1;
            }
        }
        if(pos2>=len2) {
            if(length2>0) {
                c=*s2++;
                if(UTF_IS_FIRST_SURROGATE(c) && length2>1 && UTF_IS_SECOND_SURROGATE(uc=*s2)) {
                    c=UTF16_GET_PAIR_VALUE(c, uc);
                    ++s2;
                    length2-=2;
                } else {
                    --length2;
                }
                len2=u_internalFoldCase(c, t2, 32, options);
                if(len2<0) {
                    len2=-len2;
                }
                pos2=0;
            } else {
                return 1;
            }
        }

        /* compare the head code units from both folded strings */
        result=(int32_t)t1[pos1++]-(int32_t)t2[pos2++];
        if(result!=0) {
            return result;
        }
    }
}

U_CAPI int32_t U_EXPORT2
u_memcasecmp(const UChar *s1, const UChar *s2, int32_t length, uint32_t options) {
    return u_internalStrcasecmp(s1, length, s2, length, options);
}

U_CAPI int32_t U_EXPORT2
u_strncasecmp(const UChar *s1, const UChar *s2, int32_t n, uint32_t options) {
    /*
     * This is a simple, sub-optimal implementation:
     * Determine the actual lengths of the strings and call u_internalStrcasecmp().
     * This saves us from having an additional variant of the above strcasecmp().
     */
    const UChar *s;
    int32_t length1, length2;

    for(s=s1, length1=0; length1<n && *s!=0; ++s, ++length1) {}
    for(s=s2, length2=0; length2<n && *s!=0; ++s, ++length2) {}

    return u_internalStrcasecmp(s1, length1, s2, length2, options);
}
