/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utf32.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep20
*   created by: Markus W. Scherer
*
*   This file defines macros to deal with UTF-32 code units and code points.
*   Signatures and semantics are the same as for the similarly named macros
*   in utf16.h.
*   utf32.h is included by utf.h after unicode/umachine.h
*   and some common definitions.
*/

#ifndef __UTF32_H__
#define __UTF32_H__

/* internal definitions ----------------------------------------------------- */

#define UTF32_IS_SAFE(c, strict) \
    ((uint32_t)(c)<=0x10ffff && \
     (!(strict) || !UTF_IS_SURROGATE(c) && ((c)&0xfffe)!=0xfffe))

/*
 * For the semantics of all of these macros, see utf16.h.
 * The UTF-32 versions are trivial because any code point is
 * encoded using exactly one code unit.
 */

/* single-code point definitions -------------------------------------------- */

/* classes of code unit values */
#define UTF32_IS_SINGLE(uchar) 1
#define UTF32_IS_LEAD(uchar) 0
#define UTF32_IS_TRAIL(uchar) 0

/* number of code units per code point */
#define UTF32_NEED_MULTIPLE_UCHAR(c) 0
#define UTF32_CHAR_LENGTH(c) 1
#define UTF32_MAX_CHAR_LENGTH 1

/* average number of code units compared to UTF-16 */
#define UTF32_ARRAY_SIZE(size) (size)

#define UTF32_GET_CHAR_UNSAFE(s, i, c) { \
    (c)=(s)[i]; \
}

#define UTF32_GET_CHAR_SAFE(s, start, i, length, c, strict) { \
    (c)=(s)[i]; \
    if(!UTF32_IS_SAFE(c, strict)) { \
        (c)=UTF_ERROR_VALUE; \
    } \
}

/* definitions with forward iteration --------------------------------------- */

#define UTF32_NEXT_CHAR_UNSAFE(s, i, c) { \
    (c)=(s)[(i)++]; \
}

#define UTF32_APPEND_CHAR_UNSAFE(s, i, c) { \
    (s)[(i)++]=(c); \
}

#define UTF32_FWD_1_UNSAFE(s, i) { \
    ++(i); \
}

#define UTF32_FWD_N_UNSAFE(s, i, n) { \
    (i)+=(n); \
}

#define UTF32_SET_CHAR_START_UNSAFE(s, i) { \
}

#define UTF32_NEXT_CHAR_SAFE(s, i, length, c, strict) { \
    (c)=(s)[(i)++]; \
    if(!UTF32_IS_SAFE(c, strict)) { \
        (c)=UTF_ERROR_VALUE; \
    } \
}

#define UTF32_APPEND_CHAR_SAFE(s, i, length, c) { \
    if((uint32_t)(c)<=0x10ffff) { \
        (s)[(i)++]=(c); \
    } else /* c>0x10ffff, write 0xfffd */ { \
        (s)[(i)++]=0xfffd; \
    } \
}

#define UTF32_FWD_1_SAFE(s, i, length) { \
    ++(i); \
}

#define UTF32_FWD_N_SAFE(s, i, length, n) { \
    if(((i)+=(n))>(length)) { \
        (i)=(length); \
    } \
}

#define UTF32_SET_CHAR_START_SAFE(s, start, i) { \
}

/* definitions with backward iteration -------------------------------------- */

#define UTF32_PREV_CHAR_UNSAFE(s, i, c) { \
    (c)=(s)[--(i)]; \
}

#define UTF32_BACK_1_UNSAFE(s, i) { \
    --(i); \
}

#define UTF32_BACK_N_UNSAFE(s, i, n) { \
    (i)-=(n); \
}

#define UTF32_SET_CHAR_LIMIT_UNSAFE(s, i) { \
}

#define UTF32_PREV_CHAR_SAFE(s, start, i, c, strict) { \
    (c)=(s)[--(i)]; \
    if(!UTF32_IS_SAFE(c, strict)) { \
        (c)=UTF_ERROR_VALUE; \
    } \
}

#define UTF32_BACK_1_SAFE(s, start, i) { \
    --(i); \
}

#define UTF32_BACK_N_SAFE(s, start, i, n) { \
    (i)-=(n); \
    if((i)<(start)) { \
        (i)=(start); \
    } \
}

#define UTF32_SET_CHAR_LIMIT_SAFE(s, i, length) { \
}

#endif
