/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utf16.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep09
*   created by: Markus W. Scherer
*
*   This file defines macros to deal with UTF-16 code units and code points.
*   "Safe" macros check for length overruns and illegal sequences, and
*   also for irregular sequences when the strict option is set.
*   "Unsafe" macros are designed for maximum speed.
*   utf16.h is included by utf.h after unicode/umachine.h
*   and some common definitions.
*/

#ifndef __UTF16_H__
#define __UTF16_H__

/* single-code point definitions -------------------------------------------- */

/* handle surrogate pairs */
#define UTF_IS_FIRST_SURROGATE(uchar) (((uchar)&0xfffffc00)==0xd800)
#define UTF_IS_SECOND_SURROGATE(uchar) (((uchar)&0xfffffc00)==0xdc00)

#define UTF_IS_SURROGATE_FIRST(c) (((c)&0x400)==0)

/* get the UTF-32 value directly from the surrogate pseudo-characters */
#define UTF_SURROGATE_OFFSET ((0xd800<<10UL)+0xdc00-0x10000)

#define UTF16_GET_PAIR_VALUE(first, second) \
    (((first)<<10UL)+(second)-UTF_SURROGATE_OFFSET)

/* classes of code unit values */
#define UTF16_IS_SINGLE(uchar) !UTF_IS_SURROGATE(uchar)
#define UTF16_IS_LEAD(uchar) UTF_IS_FIRST_SURROGATE(uchar)
#define UTF16_IS_TRAIL(uchar) UTF_IS_SECOND_SURROGATE(uchar)

/* number of code units per code point */
#define UTF16_NEED_MULTIPLE_UCHAR(c) ((uint32_t)(c)>0xffff)
#define UTF16_CHAR_LENGTH(c) ((uint32_t)(c)<=0xffff ? 1 : 2)
#define UTF16_MAX_CHAR_LENGTH 2

/* average number of code units compared to UTF-16 */
#define UTF16_ARRAY_SIZE(size) (size)

/*
 * Get a single code point from an offset that points to any
 * of the code units that belong to that code point.
 * Assume 0<=i<length.
 *
 * This could be used for iteration together with
 * UTF16_CHAR_LENGTH() and UTF_IS_ERROR(),
 * but the use of UTF16_NEXT_CHAR_[UN]SAFE() and
 * UTF16_PREV_CHAR_[UN]SAFE() is more efficient for that.
 */
#define UTF16_GET_CHAR_UNSAFE(s, i, c) { \
    (c)=(s)[i]; \
    if(UTF_IS_SURROGATE(c)) { \
        if(UTF_IS_SURROGATE_FIRST(c)) { \
            (c)=UTF16_GET_PAIR_VALUE((c), (s)[(i)+1]); \
        } else { \
            (c)=UTF16_GET_PAIR_VALUE((s)[(i)-1], (c)); \
        } \
    } \
}

#define UTF16_GET_CHAR_SAFE(s, start, i, length, c, strict) { \
    (c)=(s)[i]; \
    if(UTF_IS_SURROGATE(c)) { \
        uint16_t __c2; \
        if(UTF_IS_SURROGATE_FIRST(c)) { \
            if((i)+1<(length) && UTF_IS_SECOND_SURROGATE(__c2=(s)[(i)+1])) { \
                (c)=UTF16_GET_PAIR_VALUE((c), __c2); \
                /* strict: ((c)&0xfffe)==0xfffe is caught by UTF_IS_ERROR() */ \
            } else if(strict) {\
                /* unmatched first surrogate */ \
                (c)=UTF_ERROR_VALUE; \
            } \
        } else { \
            if((i)>(start) && UTF_IS_FIRST_SURROGATE(__c2=(s)[(i)-1])) { \
                (c)=UTF16_GET_PAIR_VALUE(__c2, (c)); \
                /* strict: ((c)&0xfffe)==0xfffe is caught by UTF_IS_ERROR() */ \
            } else if(strict) {\
                /* unmatched second surrogate */ \
                (c)=UTF_ERROR_VALUE; \
            } \
        } \
    /* else strict: (c)==0xfffe is caught by UTF_IS_ERROR() */ \
    } \
}

/* definitions with forward iteration --------------------------------------- */

/*
 * all the macros that go forward assume that
 * the initial offset is 0<=i<length;
 * they update the offset
 */

/* fast versions, no error-checking */

/*
 * Get a single code point from an offset that points to the first
 * of the code units that belong to that code point.
 * Assume 0<=i<length.
 */
#define UTF16_NEXT_CHAR_UNSAFE(s, i, c) { \
    (c)=(s)[(i)++]; \
    if(UTF_IS_FIRST_SURROGATE(c)) { \
        (c)=UTF16_GET_PAIR_VALUE((c), (s)[(i)++]); \
    } \
}

#define UTF16_APPEND_CHAR_UNSAFE(s, i, c) { \
    if((uint32_t)(c)<=0xffff) { \
        (s)[(i)++]=(uint16_t)(c); \
    } else { \
        (s)[(i)++]=(uint16_t)((c)>>10)+0xd7c0; \
        (s)[(i)++]=(uint16_t)(c)&0x3ff|0xdc00; \
    } \
}

#define UTF16_FWD_1_UNSAFE(s, i) { \
    if(UTF_IS_FIRST_SURROGATE((s)[(i)++])) { \
        ++(i); \
    } \
}

#define UTF16_FWD_N_UNSAFE(s, i, n) { \
    UTextOffset __N=(n); \
    while(__N>0) { \
        UTF16_FWD_1_UNSAFE(s, i); \
        --__N; \
    } \
}

/*
 * Set a random-access offset and adjust it so that
 * it points to the beginning of a Unicode character.
 * The offset that is passed in points to
 * any code unit of a code point
 * and will point to the first code unit after
 * the macro invocation.
 * Never increments the offset.
 */
#define UTF16_SET_CHAR_START_UNSAFE(s, i) { \
    if(UTF_IS_SECOND_SURROGATE((s)[i])) { \
        --(i); \
    } \
}

/* safe versions with error-checking and optional regularity-checking */

#define UTF16_NEXT_CHAR_SAFE(s, i, length, c, strict) { \
    (c)=(s)[(i)++]; \
    if(UTF_IS_FIRST_SURROGATE(c)) { \
        uint16_t __c2; \
        if((i)<(length) && UTF_IS_SECOND_SURROGATE(__c2=(s)[(i)])) { \
            ++(i); \
            (c)=UTF16_GET_PAIR_VALUE((c), __c2); \
            /* strict: ((c)&0xfffe)==0xfffe is caught by UTF_IS_ERROR() */ \
        } else if(strict) {\
            /* unmatched first surrogate */ \
            (c)=UTF_ERROR_VALUE; \
        } \
    } else if(strict && UTF_IS_SECOND_SURROGATE(c)) { \
        /* unmatched second surrogate */ \
        (c)=UTF_ERROR_VALUE; \
    /* else strict: (c)==0xfffe is caught by UTF_IS_ERROR() */ \
    } \
}

#define UTF16_APPEND_CHAR_SAFE(s, i, length, c) { \
    if((uint32_t)(c)<=0xffff) { \
        (s)[(i)++]=(uint16_t)(c); \
    } else if((uint32_t)(c)<=0x10ffff) { \
        if((i)+1<(length)) { \
            (s)[(i)++]=(uint16_t)((c)>>10)+0xd7c0; \
            (s)[(i)++]=(uint16_t)(c)&0x3ff|0xdc00; \
        } else /* not enough space */ { \
            (s)[(i)++]=UTF_ERROR_VALUE; \
        } \
    } else /* c>0x10ffff, write error value */ { \
        (s)[(i)++]=UTF_ERROR_VALUE; \
    } \
}

#define UTF16_FWD_1_SAFE(s, i, length) { \
    if(UTF_IS_FIRST_SURROGATE((s)[(i)++]) && (i)<(length) && UTF_IS_SECOND_SURROGATE((s)[i])) { \
        ++(i); \
    } \
}

#define UTF16_FWD_N_SAFE(s, i, length, n) { \
    UTextOffset __N=(n); \
    while(__N>0 && (i)<(length)) { \
        UTF16_FWD_1_SAFE(s, i, length); \
        --__N; \
    } \
}

#define UTF16_SET_CHAR_START_SAFE(s, start, i) { \
    if(UTF_IS_SECOND_SURROGATE((s)[i]) && (i)>(start) && UTF_IS_FIRST_SURROGATE((s)[(i)-1])) { \
        --(i); \
    } \
}

/* definitions with backward iteration -------------------------------------- */

/*
 * all the macros that go backward assume that
 * the valid buffer range starts at offset 0
 * and that the initial offset is 0<i<=length;
 * they update the offset
 */

/* fast versions, no error-checking */

/*
 * Get a single code point from an offset that points behind the last
 * of the code units that belong to that code point.
 * Assume 0<=i<length.
 */
#define UTF16_PREV_CHAR_UNSAFE(s, i, c) { \
    (c)=(s)[--(i)]; \
    if(UTF_IS_SECOND_SURROGATE(c)) { \
        (c)=UTF16_GET_PAIR_VALUE((s)[--(i)], (c)); \
    } \
}

#define UTF16_BACK_1_UNSAFE(s, i) { \
    if(UTF_IS_SECOND_SURROGATE((s)[--(i)])) { \
        --(i); \
    } \
}

#define UTF16_BACK_N_UNSAFE(s, i, n) { \
    UTextOffset __N=(n); \
    while(__N>0) { \
        UTF16_BACK_1_UNSAFE(s, i); \
        --__N; \
    } \
}

/*
 * Set a random-access offset and adjust it so that
 * it points after the end of a Unicode character.
 * The offset that is passed in points behind
 * any code unit of a code point
 * and will point behind the last code unit after
 * the macro invocation.
 * Never decrements the offset.
 */
#define UTF16_SET_CHAR_LIMIT_UNSAFE(s, i) { \
    if(UTF_IS_FIRST_SURROGATE((s)[(i)-1])) { \
        ++(i); \
    } \
}

/* safe versions with error-checking and optional regularity-checking */

#define UTF16_PREV_CHAR_SAFE(s, start, i, c, strict) { \
    (c)=(s)[--(i)]; \
    if(UTF_IS_SECOND_SURROGATE(c)) { \
        uint16_t __c2; \
        if((i)>(start) && UTF_IS_FIRST_SURROGATE(__c2=(s)[(i)-1])) { \
            --(i); \
            (c)=UTF16_GET_PAIR_VALUE(__c2, (c)); \
            /* strict: ((c)&0xfffe)==0xfffe is caught by UTF_IS_ERROR() */ \
        } else if(strict) {\
            /* unmatched second surrogate */ \
            (c)=UTF_ERROR_VALUE; \
        } \
    } else if(strict && UTF_IS_FIRST_SURROGATE(c)) { \
        /* unmatched first surrogate */ \
        (c)=UTF_ERROR_VALUE; \
    /* else strict: (c)==0xfffe is caught by UTF_IS_ERROR() */ \
    } \
}

#define UTF16_BACK_1_SAFE(s, start, i) { \
    if(UTF_IS_SECOND_SURROGATE((s)[--(i)]) && (i)>(start) && UTF_IS_FIRST_SURROGATE((s)[(i)-1])) { \
        --(i); \
    } \
}

#define UTF16_BACK_N_SAFE(s, start, i, n) { \
    UTextOffset __N=(n); \
    while(__N>0 && (i)>(start)) { \
        UTF16_BACK_1_SAFE(s, start, i); \
        --__N; \
    } \
}

#define UTF16_SET_CHAR_LIMIT_SAFE(s, start, i, length) { \
    if((start)<(i) && (i)<(length) && UTF_IS_FIRST_SURROGATE((s)[(i)-1]) && UTF_IS_SECOND_SURROGATE((s)[i])) { \
        ++(i); \
    } \
}

#endif
