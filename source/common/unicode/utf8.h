/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utf8.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep13
*   created by: Markus W. Scherer
*
*   This file defines macros to deal with UTF-8 code units and code points.
*   Signatures and semantics are the same as for the similarly named macros
*   in utf16.h.
*   utf8.h is included by utf.h after unicode/umachine.h
*   and some common definitions.
*/

#ifndef __UTF8_H__
#define __UTF8_H__

/* internal definitions ----------------------------------------------------- */

U_CAPI uint8_t U_EXPORT2
utf8_countTrailBytes[256];

/*
 * Count the trail bytes for a lead byte -
 * this macro should be used so that the assembler code
 * that is mentioned in utf_impl.c could be used here.
 */
#define UTF8_COUNT_TRAIL_BYTES(leadByte) (utf8_countTrailBytes[(uint8_t)leadByte])

/* use a macro here, too - there may be a simpler way with some machines */
#define UTF8_MASK_LEAD_BYTE(leadByte, countTrailBytes) ((leadByte)&=(1<<(6-(countTrailBytes)))-1)

U_CAPI UChar32 U_EXPORT2
utf8_nextCharSafeBody(const uint8_t *s, UTextOffset *pi, UTextOffset length, UChar32 c, UBool strict);

U_CAPI UTextOffset U_EXPORT2
utf8_appendCharSafeBody(uint8_t *s, UTextOffset i, UTextOffset length, UChar32 c);

U_CAPI UChar32 U_EXPORT2
utf8_prevCharSafeBody(const uint8_t *s, UTextOffset start, UTextOffset *pi, UChar32 c, UBool strict);

U_CAPI UTextOffset U_EXPORT2
utf8_back1SafeBody(const uint8_t *s, UTextOffset start, UTextOffset i);

/*
 * For the semantics of all of these macros, see utf16.h.
 * The UTF-8 macros favor sequences more the shorter they are.
 * Sometimes, only the single-byte case is covered by a macro,
 * while longer sequences are handled by a function call.
 */

/* single-code point definitions -------------------------------------------- */

/* classes of code unit values */
#define UTF8_IS_SINGLE(uchar) ((uchar)&0x80==0)
#define UTF8_IS_LEAD(uchar) ((uint8_t)((uchar)-0xc0)<0x3e)
#define UTF8_IS_TRAIL(uchar) ((uchar)&0xc0==0x80)

/* number of code units per code point */
#define UTF8_NEED_MULTIPLE_UCHAR(c) ((uint32_t)(c)>0x7f)

/*
 * ICU does not deal with code points >0x10ffff
 * unless necessary for advancing in the byte stream.
 *
 * These length macros take into account that for values >0x10ffff
 * the "safe" append macros would write the error code point 0xffff
 * with 3 bytes.
 * Code point comparisons need to be in uint32_t because UChar32
 * may be a signed type, and negative values must be recognized.
 */
#if 1
#   define UTF8_CHAR_LENGTH(c) \
        ((uint32_t)(c)<=0x7f ? 1 : \
            ((uint32_t)(c)<=0x7ff ? 2 : \
                ((uint32_t)((c)-0x10000)>0xfffff ? 3 : 4) \
            ) \
        )
#else
#   define UTF8_CHAR_LENGTH(c) \
        ((uint32_t)(c)<=0x7f ? 1 : \
            ((uint32_t)(c)<=0x7ff ? 2 : \
                ((uint32_t)(c)<=0xffff ? 3 : \
                    ((uint32_t)(c)<=0x10ffff ? 4 : \
                        ((uint32_t)(c)<=0x3ffffff ? 5 : \
                            ((uint32_t)(c)<=0x7fffffff ? 6 : 3) \
                        ) \
                    ) \
                ) \
            ) \
        )
#endif

#define UTF8_MAX_CHAR_LENGTH 4

/* average number of code units compared to UTF-16 */
#define UTF8_ARRAY_SIZE(size) ((5*(size))/2)

#define UTF8_GET_CHAR_UNSAFE(s, i, c) { \
    UTextOffset __I=(UTextOffset)(i); \
    UTF8_SET_CHAR_START_UNSAFE(s, __I); \
    UTF8_NEXT_CHAR_UNSAFE(s, __I, c); \
}

#define UTF8_GET_CHAR_SAFE(s, start, i, length, c, strict) { \
    UTextOffset __I=(UTextOffset)(i); \
    UTF8_SET_CHAR_START_SAFE(s, start, __I); \
    UTF8_NEXT_CHAR_SAFE(s, __I, length, c, strict); \
}

/* definitions with forward iteration --------------------------------------- */

/*
 * Read a Unicode scalar value from an array of UTF-8 bytes.
 * Only values <=0x10ffff are accepted, and if an error occurs,
 * then c will be set such that UTF_IS_ERROR(c).
 * The _UNSAFE macro is fast and does not check for errors.
 * The _SAFE macro checks for errors and optionally for
 * irregular sequences, too, i.e., for sequences that
 * are longer than necessary, such as <c0 80> instead of <0>.
 * The strict checks also check for surrogates and
 * for 0xXXXXfffe and 0xXXXXffff.
 */
#define UTF8_NEXT_CHAR_UNSAFE(s, i, c) { \
    (c)=(s)[(i)++]; \
    if((uint8_t)((c)-0xc0)<0x35) { \
        uint8_t __count=UTF8_COUNT_TRAIL_BYTES(c); \
        UTF8_MASK_LEAD_BYTE(c, __count); \
        switch(__count) { \
        /* each following branch falls through to the next one */ \
        case 3: \
            (c)=((c)<<6)|((s)[(i)++]&0x3f); \
        case 2: \
            (c)=((c)<<6)|((s)[(i)++]&0x3f); \
        case 1: \
            (c)=((c)<<6)|((s)[(i)++]&0x3f); \
        /* no other branches to optimize switch() */ \
            break; \
        } \
    } \
}

#define UTF8_APPEND_CHAR_UNSAFE(s, i, c) { \
    if((uint32_t)(c)<=0x7f) { \
        (s)[(i)++]=(uint8_t)(c); \
    } else { \
        if((uint32_t)(c)<=0x7ff) { \
            (s)[(i)++]=(uint8_t)((c)>>6)|0xc0; \
        } else { \
            if((uint32_t)(c)<=0xffff) { \
                (s)[(i)++]=(uint8_t)((c)>>12)|0xe0; \
            } else { \
                (s)[(i)++]=(uint8_t)((c)>>18)|0xf0; \
                (s)[(i)++]=(uint8_t)((c)>>12)&0x3f|0x80; \
            } \
            (s)[(i)++]=(uint8_t)((c)>>6)&0x3f|0x80; \
        } \
        (s)[(i)++]=(uint8_t)(c)&0x3f|0x80; \
    } \
}

#define UTF8_FWD_1_UNSAFE(s, i) { \
    (i)+=1+UTF8_COUNT_TRAIL_BYTES((s)[i]); \
}

#define UTF8_FWD_N_UNSAFE(s, i, n) { \
    UTextOffset __N=(n); \
    while(__N>0) { \
        UTF8_FWD_1_UNSAFE(s, i); \
        --__N; \
    } \
}

#define UTF8_SET_CHAR_START_UNSAFE(s, i) { \
    while(UTF8_IS_TRAIL((s)[i])) { --(i); } \
}

#define UTF8_NEXT_CHAR_SAFE(s, i, length, c, strict) { \
    (c)=(s)[(i)++]; \
    if(UTF8_IS_LEAD(c)) { \
        (c)=utf8_nextCharSafeBody(s, &(i), (UTextOffset)(length), c, strict); \
    } \
}

#define UTF8_APPEND_CHAR_SAFE(s, i, length, c) { \
    if((uint32_t)(c)<=0x7f) { \
        (s)[(i)++]=(uint8_t)(c); \
    } else { \
        (i)=utf8_appendCharSafeBody(s, (UTextOffset)(i), (UTextOffset)(length), c); \
    } \
}

#define UTF8_FWD_1_SAFE(s, i, length) { \
    uint8_t __b=(s)[(i)++]; \
    if(UTF8_IS_LEAD(__b)) { \
        uint8_t __count=UTF8_COUNT_TRAIL_BYTES(__b); \
        if((i)+__count>(length)) { \
            __count=(length)-(i); \
        } \
        while(__count>0 && UTF8_IS_TRAIL((s)[i])) { \
            ++(i); \
            --__count; \
        } \
    } \
}

#define UTF8_FWD_N_SAFE(s, i, length, n) { \
    UTextOffset __N=(n); \
    while(__N>0 && (i)<(length)) { \
        UTF8_FWD_1_SAFE(s, i, length); \
        --__N; \
    } \
}

#define UTF8_SET_CHAR_START_SAFE(s, start, i) { \
    if(UTF8_IS_TRAIL((s)[(i)])) { \
        (i)=utf8_back1SafeBody(s, start, (UTextOffset)(i)); \
    } \
}

/* definitions with backward iteration -------------------------------------- */

#define UTF8_PREV_CHAR_UNSAFE(s, i, c) { \
    (c)=(s)[--(i)]; \
    if(UTF8_IS_TRAIL(c)) { \
        uint8_t __b, __count=1, __shift=6; \
\
        /* c is a trail byte */ \
        (c)&=0x3f; \
        for(;;) { \
            __b=(s)[--(i)]; \
            if(__b>=0xc0) { \
                UTF8_MASK_LEAD_BYTE(__b, __count); \
                (c)|=(UChar32)__b<<__shift; \
                break; \
            } else { \
                (c)|=(UChar32)(__b&0x3f)<<__shift; \
                ++__count; \
                __shift+=6; \
            } \
        } \
    } \
}

#define UTF8_BACK_1_UNSAFE(s, i) { \
    while(UTF8_IS_TRAIL((s)[--(i)])) {} \
}

#define UTF8_BACK_N_UNSAFE(s, i, n) { \
    UTextOffset __N=(n); \
    while(__N>0) { \
        UTF8_BACK_1_UNSAFE(s, i); \
        --__N; \
    } \
}

#define UTF8_SET_CHAR_LIMIT_UNSAFE(s, i) { \
    UTF8_BACK_1_UNSAFE(s, i); \
    UTF8_FWD_1_UNSAFE(s, i); \
}

#define UTF8_PREV_CHAR_SAFE(s, start, i, c, strict) { \
    (c)=(s)[--(i)]; \
    if(UTF8_IS_TRAIL((c))) { \
        (c)=utf8_prevCharSafeBody(s, start, &(i), c, strict); \
    } \
}

#define UTF8_BACK_1_SAFE(s, start, i) { \
    if(UTF8_IS_TRAIL((s)[--(i)])) { \
        (i)=utf8_back1SafeBody(s, start, (UTextOffset)(i)); \
    } \
}

#define UTF8_BACK_N_SAFE(s, start, i, n) { \
    UTextOffset __N=(n); \
    while(__N>0 && (i)>(start)) { \
        UTF8_BACK_1_SAFE(s, start, i); \
        --__N; \
    } \
}

#define UTF8_SET_CHAR_LIMIT_SAFE(s, start, i, length) { \
    if((start)<(i) && (i)<(length)) { \
        UTF8_BACK_1_SAFE(s, start, i); \
        (i)+=1+UTF8_COUNT_TRAIL_BYTES((s)[i]); \
        if((i)>(length)) { \
            (i)=(length); \
        } \
    } \
}

#endif
