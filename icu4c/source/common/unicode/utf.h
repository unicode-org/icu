/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utf.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep09
*   created by: Markus W. Scherer
*
*   This file defines the UChar and UChar32 data types for Unicode code units
*   and code points, as well as macros for efficiently getting code points
*   in and out of a string.
*   utf.h is included by utypes.h and itself includes the utfXX.h after some
*   common definitions. Those files define the macros for each UTF-size.
*/

#ifndef __UTF_H__
#define __UTF_H__

#include <wchar.h>
#include "unicode/umachine.h"
/* include the utfXX.h after the following definitions */

/* If there is no compiler option for the preferred UTF size, then default to UTF-16. */
#ifndef UTF_SIZE
#   define UTF_SIZE 16
#endif

#define U_SIZEOF_UCHAR (UTF_SIZE>>3)

/* U_SIZEOF_WCHAR_T==sizeof(wchar_t) */
#ifndef U_SIZEOF_WCHAR_T
#   define U_SIZEOF_WCHAR_T 4
#endif

/* Define UChar32 to be compatible with wchar_t if possible. */
#if U_SIZEOF_WCHAR_T==4
    typedef wchar_t UChar32;
#else
    typedef uint32_t UChar32;
#endif

/* Unicode string and array offset and index type */
typedef int32_t UTextOffset;

/* Specify which macro versions are the default ones - safe or fast. */
#if !defined(UTF_SAFE) && !defined(UTF_STRICT) && !defined(UTF_UNSAFE)
#   define UTF_SAFE
#endif

/* internal definitions ----------------------------------------------------- */

/*
 * Special error values for UTF-8,
 * which need 1 or 2 bytes in UTF-8:
 * U+0015 = NAK = Negative Acknowledge, C0 control character
 * U+009f = highest C1 control character
 *
 * These are used by ("safe") UTF-8 macros so that they can return an error value
 * that needs the same number of code units (bytes) as were seen by
 * a macro.
 */
#define UTF8_ERROR_VALUE_1 0x15
#define UTF8_ERROR_VALUE_2 0x9f

/* error value for all UTFs */
#define UTF_ERROR_VALUE 0xffff

/* single-code point definitions -------------------------------------------- */

/* is this code unit a surrogate? */
#define UTF_IS_SURROGATE(uchar) (((uchar)&0xf800)==0xd800)

/*
 * Is a given 32-bit code point/Unicode scalar value
 * actually a valid Unicode (abstract) character?
 */
#define UTF_IS_UNICODE_CHAR(c) \
    ((uint32_t)(c)<=0x10ffff && \
     !UTF_IS_SURROGATE(c) && ((c)&0xfffe)!=0xfffe)

/*
 * Is a given 32-bit code an error value
 * as returned by one of the macros for any UTF?
 */
#define UTF_IS_ERROR(c) \
    (((c)&0xfffe)==0xfffe || (c)==UTF8_ERROR_VALUE_1 || (c)==UTF8_ERROR_VALUE_2)

/* This is a combined macro: is c a valid Unicode value _and_ not an error code? */
#define UTF_IS_VALID(c) \
    ((uint32_t)(c)<=0x10ffff && \
     !UTF_IS_SURROGATE(c) && \
     ((c)&0xfffe)!=0xfffe && \
     (c)!=UTF8_ERROR_VALUE_1 && (c)!=UTF8_ERROR_VALUE_2)

/* include the utfXX.h ------------------------------------------------------ */

#include "unicode/utf8.h"
#include "unicode/utf16.h"
#include "unicode/utf32.h"

/* Define types and macros according to the selected UTF size. -------------- */

#if UTF_SIZE==8

#   error UTF-8 is not implemented, undefine UTF_SIZE or define it to 16

    typedef uint8_t UChar;

#elif UTF_SIZE==16

    /* Define UChar to be compatible with wchar_t if possible. */
#   if U_SIZEOF_WCHAR_T==2
        typedef wchar_t UChar;
#   else
        typedef uint16_t UChar;
#   endif

#   define UTF_IS_SINGLE(uchar)                         UTF16_IS_SINGLE(uchar)
#   define UTF_IS_LEAD(uchar)                           UTF16_IS_LEAD(uchar)
#   define UTF_IS_TRAIL(uchar)                          UTF16_IS_TRAIL(uchar)

#   define UTF_NEED_MULTIPLE_UCHAR(c)                   UTF16_NEED_MULTIPLE_UCHAR(c)
#   define UTF_CHAR_LENGTH(c)                           UTF16_CHAR_LENGTH(c)

#   define UTF_GET_CHAR_UNSAFE(s, i, c)                 UTF16_GET_CHAR_UNSAFE(s, i, c)
#   define UTF_GET_CHAR_SAFE(s, i, length, c, strict)   UTF16_GET_CHAR_SAFE(s, i, length, c, strict)

#   define UTF_NEXT_CHAR_UNSAFE(s, i, c)                UTF16_NEXT_CHAR_UNSAFE(s, i, c)
#   define UTF_NEXT_CHAR_SAFE(s, i, length, c, strict)  UTF16_NEXT_CHAR_SAFE(s, i, length, c, strict)

#   define UTF_APPEND_CHAR_UNSAFE(s, i, c)              UTF16_APPEND_CHAR_UNSAFE(s, i, c)
#   define UTF_APPEND_CHAR_SAFE(s, i, length, c)        UTF16_APPEND_CHAR_SAFE(s, i, length, c)

#   define UTF_FWD_1_UNSAFE(s, i)                       UTF16_FWD_1_UNSAFE(s, i)
#   define UTF_FWD_1_SAFE(s, i, length)                 UTF16_FWD_1_SAFE(s, i, length)

#   define UTF_FWD_N_UNSAFE(s, i, n)                    UTF16_FWD_N_UNSAFE(s, i, n)
#   define UTF_FWD_N_SAFE(s, i, length, n)              UTF16_FWD_N_SAFE(s, i, length, n)

#   define UTF_SET_CHAR_START_UNSAFE(s, i)              UTF16_SET_CHAR_START_UNSAFE(s, i)
#   define UTF_SET_CHAR_START_SAFE(s, i)                UTF16_SET_CHAR_START_SAFE(s, i)

#   define UTF_PREV_CHAR_UNSAFE(s, i, c)                UTF16_PREV_CHAR_UNSAFE(s, i, c)
#   define UTF_PREV_CHAR_SAFE(s, i, c, strict)          UTF16_PREV_CHAR_SAFE(s, i, c, strict)

#   define UTF_BACK_1_UNSAFE(s, i)                      UTF16_BACK_1_UNSAFE(s, i)
#   define UTF_BACK_1_SAFE(s, i)                        UTF16_BACK_1_SAFE(s, i)

#   define UTF_BACK_N_UNSAFE(s, i, n)                   UTF16_BACK_N_UNSAFE(s, i, n)
#   define UTF_BACK_N_SAFE(s, i, n)                     UTF16_BACK_N_SAFE(s, i, n)

#   define UTF_SET_CHAR_LIMIT_UNSAFE(s, i)              UTF16_SET_CHAR_LIMIT_UNSAFE(s, i)
#   define UTF_SET_CHAR_LIMIT_SAFE(s, i, length)        UTF16_SET_CHAR_LIMIT_SAFE(s, i, length)

#elif UTF_SIZE==32

#   error UTF-32 is not implemented, undefine UTF_SIZE or define it to 16

    typedef UChar32 UChar;

#else
#   error UTF_SIZE must be undefined or one of { 8, 16, 32 } - only 16 is implemented
#endif

/* Define the default macros for handling UTF characters. ------------------- */

#ifdef UTF_SAFE

#   define UTF_GET_CHAR(s, i, length, c)        UTF_GET_CHAR_SAFE(s, i, length, c, FALSE)

#   define UTF_NEXT_CHAR(s, i, length, c)       UTF_NEXT_CHAR_SAFE(s, i, length, c, FALSE)
#   define UTF_APPEND_CHAR(s, i, length, c)     UTF_APPEND_CHAR_SAFE(s, i, length, c)
#   define UTF_FWD_1(s, i, length)              UTF_FWD_1_SAFE(s, i, length)
#   define UTF_FWD_N(s, i, length, n)           UTF_FWD_N_SAFE(s, i, length, n)
#   define UTF_SET_CHAR_START(s, i)             UTF_SET_CHAR_START_SAFE(s, i)

#   define UTF_PREV_CHAR(s, i, c)               UTF_PREV_CHAR_SAFE(s, i, c, FALSE)
#   define UTF_BACK_1(s, i)                     UTF_BACK_1_SAFE(s, i)
#   define UTF_BACK_N(s, i, n)                  UTF_BACK_N_SAFE(s, i, n)
#   define UTF_SET_CHAR_LIMIT(s, i, length)     UTF_SET_CHAR_LIMIT_SAFE(s, i, length)

#elif defined(UTF_STRICT)

#   define UTF_GET_CHAR(s, i, length, c)        UTF_GET_CHAR_SAFE(s, i, length, c, TRUE)

#   define UTF_NEXT_CHAR(s, i, length, c)       UTF_NEXT_CHAR_SAFE(s, i, length, c, TRUE)
#   define UTF_APPEND_CHAR(s, i, length, c)     UTF_APPEND_CHAR_SAFE(s, i, length, c)
#   define UTF_FWD_1(s, i, length)              UTF_FWD_1_SAFE(s, i, length)
#   define UTF_FWD_N(s, i, length, n)           UTF_FWD_N_SAFE(s, i, length, n)
#   define UTF_SET_CHAR_START(s, i)             UTF_SET_CHAR_START_SAFE(s, i)

#   define UTF_PREV_CHAR(s, i, c)               UTF_PREV_CHAR_SAFE(s, i, c, TRUE)
#   define UTF_BACK_1(s, i)                     UTF_BACK_1_SAFE(s, i)
#   define UTF_BACK_N(s, i, n)                  UTF_BACK_N_SAFE(s, i, n)
#   define UTF_SET_CHAR_LIMIT(s, i, length)     UTF_SET_CHAR_LIMIT_SAFE(s, i, length)

#else /* UTF_UNSAFE */

#   define UTF_GET_CHAR(s, i, length, c)        UTF_GET_CHAR_UNSAFE(s, i, c)

#   define UTF_NEXT_CHAR(s, i, length, c)       UTF_NEXT_CHAR_UNSAFE(s, i, c)
#   define UTF_APPEND_CHAR(s, i, length, c)     UTF_APPEND_CHAR_UNSAFE(s, i, c)
#   define UTF_FWD_1(s, i, length)              UTF_FWD_1_UNSAFE(s, i)
#   define UTF_FWD_N(s, i, length, n)           UTF_FWD_N_UNSAFE(s, i, n)
#   define UTF_SET_CHAR_START(s, i)             UTF_SET_CHAR_START_UNSAFE(s, i)

#   define UTF_PREV_CHAR(s, i, c)               UTF_PREV_CHAR_UNSAFE(s, i, c)
#   define UTF_BACK_1(s, i)                     UTF_BACK_1_UNSAFE(s, i)
#   define UTF_BACK_N(s, i, n)                  UTF_BACK_N_UNSAFE(s, i, n)
#   define UTF_SET_CHAR_LIMIT(s, i, length)     UTF_SET_CHAR_LIMIT_UNSAFE(s, i)

#endif

#endif
