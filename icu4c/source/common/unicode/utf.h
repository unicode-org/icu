/*
*******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
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
*/

/**
* \file
* \brief C API: UChar and UChar32 data types and UTF macros for C Unicode string handling
*
*   <p>This file defines the UChar and UChar32 data types for Unicode code units
*   and code points, as well as macros for efficiently getting code points
*   in and out of a string.</p>
*
*   <p>utf.h is included by utypes.h and itself includes the utfXX.h after some
*   common definitions. Those files define the macros for each UTF-size.</p>
*
*   <p>ICU allows in principle to set which UTF (UTF-8/16/32) is used internally
*   by defining UTF_SIZE to either 8, 16, or 32. utf.h would then define the UChar type
*   accordingly. UTF-16 is the default.<br>
*   In praxis, since a lot of the ICU source code &mdash; especially low-level code like
*   conversion and collation &mdash; assumes UTF-16, utf.h enforces the default of UTF-16.
*   This is unlikely to change in the future. Only some files (like ubidi.h and most of unistr.h) should work with any UTF.</p>
*
*   <p>Accordinly, utf.h defines UChar to be an unsigned 16-bit integer. If this matches wchar_t, then
*   UChar is defined to be exactly wchar_t, otherwise uint16_t.</p>
*
*   <p>UChar32 is always defined to be a 32-bit integer to be large enough for a 21-bit
*   Unicode code point (Unicode scalar value, 0..0x10ffff). If wchar_t is a 32-bit type, then
*   UChar32 is defined to be exactly wchar_t, <em>regardless of whether wchar_t is signed or unsigned.
*   This means that UChar32 may be signed or unsigned depending on the platform!</em>
*   If wchar_t is not a 32-bit type, then UChar32 is defined to be uint32_t.</p>
*
*   <p>utf.h also defines a number of C macros for handling single Unicode code points and
*   for using UTF Unicode strings. It includes utf8.h, utf16.h, and utf32.h for the actual
*   implementations of those macros and then aliases one set of them (for UTF-16) for general use.
*   The UTF-specific macros have the UTF size in the macro name prefixes (UTF16_...), while
*   the general alias macros always begin with UTF_...</p>
*
*   <p>Many string operations can be done with or without error checking.
*   Where such a distinction is useful, there are two versions of the macros, "unsafe" and "safe"
*   ones with ..._UNSAFE and ..._SAFE suffixes. The unsafe macros are fast but may cause
*   program failures if the strings are not well-formed. The safe macros have an additional, boolean
*   parameter "strict". If strict is FALSE, then only illegal sequences are detected.
*   Otherwise, irregular sequences are detected as well (like single surrogates in UTF-8/32).
*   Safe macros return special error code points for illegal/irregular sequences:
*   Typically, U+ffff, or for UTF-8 values that would result in a byte sequence of the same length
*   as the illegal input sequence.<br>
*   Note that _UNSAFE macros have fewer parameters: They do not have the strictness parameter, and
*   they do not have start/length parameters for boundary checking.</p>
*
*   <p>Here, the macros are aliased in two steps:
*   In the first step, the UTF-specific macros with UTF16_ prefix and _UNSAFE and _SAFE suffixes are
*   aliased according to the UTF_SIZE to macros with UTF_ prefix and the same suffixes and signatures.
*   Then, in a second step, the default, general alias macros are set to use either the unsafe or
*   the safe/not strict (default) or the safe/strict macro;
*   these general macros do not have a strictness parameter.</p>
*
*   <p>It is possible to change the default choice for the general alias macros to be unsafe, safe/not strict or safe/strict.
*   The default is safe/not strict. It is not recommended to select the unsafe macros as the basis for
*   Unicode string handling in ICU! To select this, define UTF_SAFE, UTF_STRICT, or UTF_UNSAFE.</p>
*
*   <p>For general use, one should use the default, general macros with UTF_ prefix and no _SAFE/_UNSAFE suffix.
*   Only in some cases it may be necessary to control the choice of macro directly and use a less generic alias.
*   For example, if it can be assumed that a string is well-formed and the index will stay within the bounds,
*   then the _UNSAFE version may be used.
*   If a UTF-8 string is to be processed, then the macros with UTF8_ prefixes need to be used.</p>
*   <b>Usage:</b>  ICU coding guidelines for if() statements should be followed when using these macros
*                  Compound statements (curly braces {}) must be used  for if-else-while... 
*                  bodies and all macro statements should be terminated with semicolon.
*/

#ifndef __UTF_H__
#define __UTF_H__

/*
 * ANSI C headers:
 * stddef.h defines wchar_t
 */
#include <stddef.h>
#include "unicode/umachine.h"
/* include the utfXX.h after the following definitions */

/* If there is no compiler option for the preferred UTF size, then default to UTF-16. */
#ifndef UTF_SIZE
    /** Number of bits in a Unicode string code unit, same as x in UTF-x (8, 16, or 32). */
#   define UTF_SIZE 16
#endif

/** Number of bytes in a UChar (sizeof(UChar)). */
#define U_SIZEOF_UCHAR (UTF_SIZE>>3)

/*!
 * \def U_SIZEOF_WCHAR_T
 * Do we have wchar.h on this platform? It is there on most platforms.
 */
#ifndef U_HAVE_WCHAR_H
#   define U_HAVE_WCHAR_H 1
#endif

/* U_SIZEOF_WCHAR_T==sizeof(wchar_t) (0 means it is not defined or autoconf could not set it) */
#if U_SIZEOF_WCHAR_T==0
#   undef U_SIZEOF_WCHAR_T
    /** U_SIZEOF_WCHAR_T==sizeof(wchar_t). */
#   define U_SIZEOF_WCHAR_T 4
#endif

/*!
 * \var UChar32
 * Define UChar32 to be wchar_t if that is 32 bits wide; may be signed or unsigned!
 * If wchar_t is not 32 bits wide, then define UChar32 to be uint32_t.
 */
#if U_SIZEOF_WCHAR_T==4
    typedef wchar_t UChar32;
#else
    typedef uint32_t UChar32;
#endif

/**
 * Unicode string and array offset and index type.
 * ICU always counts Unicode code units (UChars) for string offsets, indexes, and lengths, not Unicode code points.
 */
typedef int32_t UTextOffset;

/* Specify which macro versions are the default ones - safe or fast. */
#if !defined(UTF_SAFE) && !defined(UTF_STRICT) && !defined(UTF_UNSAFE)
    /**
     * The default choice for general Unicode string macros is to use the ..._SAFE macro implementations
     * with strict=FALSE. See the utf.h file description.
     */
#   define UTF_SAFE
#endif

/* internal definitions ----------------------------------------------------- */

/**
 * <p>UTF8_ERROR_VALUE_1 and UTF8_ERROR_VALUE_2 are special error values for UTF-8,
 * which need 1 or 2 bytes in UTF-8:<br>
 * U+0015 = NAK = Negative Acknowledge, C0 control character<br>
 * U+009f = highest C1 control character</p>
 *
 * <p>These are used by ("safe") UTF-8 macros so that they can return an error value
 * that needs the same number of code units (bytes) as were seen by
 * a macro. They should be tested with UTF_IS_ERROR() or UTF_IS_VALID().</p>
 *
 * @internal
 */
#define UTF8_ERROR_VALUE_1 0x15
#define UTF8_ERROR_VALUE_2 0x9f

/**
 * Error value for all UTFs. This code point value will be set by macros with error
 * checking if an error is detected.
 */
#define UTF_ERROR_VALUE 0xffff

/* single-code point definitions -------------------------------------------- */

/** Is this code unit or code point a surrogate (U+d800..U+dfff)? */
#define UTF_IS_SURROGATE(uchar) (((uchar)&0xfffff800)==0xd800)

/**
 * Is a given 32-bit code point/Unicode scalar value
 * actually a valid Unicode (abstract) character?
 */
#define UTF_IS_UNICODE_CHAR(c) \
    ((uint32_t)(c)<=0x10ffff && \
     !UTF_IS_SURROGATE(c) && ((c)&0xfffe)!=0xfffe)

/**
 * Is a given 32-bit code an error value
 * as returned by one of the macros for any UTF?
 */
#define UTF_IS_ERROR(c) \
    (((c)&0xfffe)==0xfffe || (c)==UTF8_ERROR_VALUE_1 || (c)==UTF8_ERROR_VALUE_2)

/** This is a combined macro: Is c a valid Unicode value _and_ not an error code? */
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

/*!
 * \var UChar
 * Define UChar to be wchar_t if that is 16 bits wide; always assumed to be unsigned.
 * If wchar_t is not 16 bits wide, then define UChar to be uint16_t.
 */

#if UTF_SIZE==8

#   error UTF-8 is not implemented, undefine UTF_SIZE or define it to 16

/*
 * ANSI C header:
 * limits.h defines CHAR_MAX
 */
#   include <limits.h>

    /* Define UChar to be compatible with char if possible. */
#   if CHAR_MAX>=255
        typedef char UChar;
#   else
        typedef uint8_t UChar;
#   endif

#elif UTF_SIZE==16

    /* Define UChar to be compatible with wchar_t if possible. */
#   if U_SIZEOF_WCHAR_T==2
        typedef wchar_t UChar;
#   else
        typedef uint16_t UChar;
#   endif

    /** Does this code unit alone encode a code point? */
#   define UTF_IS_SINGLE(uchar)                         UTF16_IS_SINGLE(uchar)
    /** Is this code unit the first one of several? */
#   define UTF_IS_LEAD(uchar)                           UTF16_IS_LEAD(uchar)
    /** Is this code unit one of several but not the first one? */
#   define UTF_IS_TRAIL(uchar)                          UTF16_IS_TRAIL(uchar)

    /** Does this code point require multiple code units? */
#   define UTF_NEED_MULTIPLE_UCHAR(c)                   UTF16_NEED_MULTIPLE_UCHAR(c)
    /** How many code units are used to encode this code point? */
#   define UTF_CHAR_LENGTH(c)                           UTF16_CHAR_LENGTH(c)
    /** How many code units are used at most for any Unicode code point? */
#   define UTF_MAX_CHAR_LENGTH                          UTF16_MAX_CHAR_LENGTH
    /** Estimate the number of code units for a string based on the number of UTF-16 code units. */
#   define UTF_ARRAY_SIZE(size)                         UTF16_ARRAY_SIZE(size)

    /** See file documentation and UTF_GET_CHAR. */
#   define UTF_GET_CHAR_UNSAFE(s, i, c)                 UTF16_GET_CHAR_UNSAFE(s, i, c)
    /** See file documentation and UTF_GET_CHAR. */
#   define UTF_GET_CHAR_SAFE(s, start, i, length, c, strict) UTF16_GET_CHAR_SAFE(s, start, i, length, c, strict)

    /** See file documentation and UTF_NEXT_CHAR. */
#   define UTF_NEXT_CHAR_UNSAFE(s, i, c)                UTF16_NEXT_CHAR_UNSAFE(s, i, c)
    /** See file documentation and UTF_NEXT_CHAR. */
#   define UTF_NEXT_CHAR_SAFE(s, i, length, c, strict)  UTF16_NEXT_CHAR_SAFE(s, i, length, c, strict)

    /** See file documentation and UTF_APPEND_CHAR. */
#   define UTF_APPEND_CHAR_UNSAFE(s, i, c)              UTF16_APPEND_CHAR_UNSAFE(s, i, c)
    /** See file documentation and UTF_APPEND_CHAR. */
#   define UTF_APPEND_CHAR_SAFE(s, i, length, c)        UTF16_APPEND_CHAR_SAFE(s, i, length, c)

    /** See file documentation and UTF_FWD_1. */
#   define UTF_FWD_1_UNSAFE(s, i)                       UTF16_FWD_1_UNSAFE(s, i)
    /** See file documentation and UTF_FWD_1. */
#   define UTF_FWD_1_SAFE(s, i, length)                 UTF16_FWD_1_SAFE(s, i, length)

    /** See file documentation and UTF_FWD_N. */
#   define UTF_FWD_N_UNSAFE(s, i, n)                    UTF16_FWD_N_UNSAFE(s, i, n)
    /** See file documentation and UTF_FWD_N. */
#   define UTF_FWD_N_SAFE(s, i, length, n)              UTF16_FWD_N_SAFE(s, i, length, n)

    /** See file documentation and UTF_SET_CHAR_START. */
#   define UTF_SET_CHAR_START_UNSAFE(s, i)              UTF16_SET_CHAR_START_UNSAFE(s, i)
    /** See file documentation and UTF_SET_CHAR_START. */
#   define UTF_SET_CHAR_START_SAFE(s, start, i)         UTF16_SET_CHAR_START_SAFE(s, start, i)

    /** See file documentation and UTF_PREV_CHAR. */
#   define UTF_PREV_CHAR_UNSAFE(s, i, c)                UTF16_PREV_CHAR_UNSAFE(s, i, c)
    /** See file documentation and UTF_PREV_CHAR. */
#   define UTF_PREV_CHAR_SAFE(s, start, i, c, strict)   UTF16_PREV_CHAR_SAFE(s, start, i, c, strict)

    /** See file documentation and UTF_BACK_1. */
#   define UTF_BACK_1_UNSAFE(s, i)                      UTF16_BACK_1_UNSAFE(s, i)
    /** See file documentation and UTF_BACK_1. */
#   define UTF_BACK_1_SAFE(s, start, i)                 UTF16_BACK_1_SAFE(s, start, i)

    /** See file documentation and UTF_BACK_N. */
#   define UTF_BACK_N_UNSAFE(s, i, n)                   UTF16_BACK_N_UNSAFE(s, i, n)
    /** See file documentation and UTF_BACK_N. */
#   define UTF_BACK_N_SAFE(s, start, i, n)              UTF16_BACK_N_SAFE(s, start, i, n)

    /** See file documentation and UTF_SET_CHAR_LIMIT. */
#   define UTF_SET_CHAR_LIMIT_UNSAFE(s, i)              UTF16_SET_CHAR_LIMIT_UNSAFE(s, i)
    /** See file documentation and UTF_SET_CHAR_LIMIT. */
#   define UTF_SET_CHAR_LIMIT_SAFE(s, start, i, length) UTF16_SET_CHAR_LIMIT_SAFE(s, start, i, length)

#elif UTF_SIZE==32

#   error UTF-32 is not implemented, undefine UTF_SIZE or define it to 16

    typedef UChar32 UChar;

#else
#   error UTF_SIZE must be undefined or one of { 8, 16, 32 } - only 16 is implemented
#endif

/* Define the default macros for handling UTF characters. ------------------- */

/**
 * \def UTF_GET_CHAR(s, start, i, length, c)
 *
 * Set c to the code point that contains the code unit i.
 * i could point to the first, the last, or an intermediate code unit.
 * i is not modified.
 * \pre 0<=i<length
 */

/**
 * \def UTF_NEXT_CHAR(s, i, length, c)
 *
 * Set c to the code point that starts at code unit i
 * and advance i to beyond the code units of this code point (post-increment).
 * i must point to the first code unit of a code point.
 * \pre 0<=i<length
 * \post 0<i<=length
 */

/**
 * \def UTF_APPEND_CHAR(s, i, length, c)
 *
 * Append the code units of code point c to the string at index i
 * and advance i to beyond the new code units (post-increment).
 * The code units beginning at index i will be overwritten.
 * \pre 0<=c<=0x10ffff
 * \pre 0<=i<length
 * \post 0<i<=length
 */

/**
 * \def UTF_FWD_1(s, i, length)
 *
 * Advance i to beyond the code units of the code point that begins at i.
 * I.e., advance i by one code point.
 * i must point to the first code unit of a code point.
 * \pre 0<=i<length
 * \post 0<i<=length
 */

/**
 * \def UTF_FWD_N(s, i, length, n)
 *
 * Advance i to beyond the code units of the n code points where the first one begins at i.
 * I.e., advance i by n code points.
 * i must point to the first code unit of a code point.
 * \pre 0<=i<length
 * \post 0<i<=length
 */

/**
 * \def UTF_SET_CHAR_START(s, start, i)
 *
 * Take the random-access index i and adjust it so that it points to the beginning
 * of a code point.
 * The input index points to any code unit of a code point and is moved to point to
 * the first code unit of the same code point. i is never incremented.
 * This can be used to start an iteration with UTF_NEXT_CHAR() from a random index.
 * \pre start<=i<length
 * \post start<=i<length
 */

/**
 * \def UTF_PREV_CHAR(s, start, i, c)
 *
 * Set c to the code point that has code units before i
 * and move i backward (towards the beginning of the string)
 * to the first code unit of this code point (pre-increment).
 * i must point to the first code unit after the last unit of a code point (i==length is allowed).
 * \pre start<i<=length
 * \post start<=i<length
 */

/**
 * \def UTF_BACK_1(s, start, i)
 *
 * Move i backward (towards the beginning of the string)
 * to the first code unit of the code point that has code units before i.
 * I.e., move i backward by one code point.
 * i must point to the first code unit after the last unit of a code point (i==length is allowed).
 * \pre start<i<=length
 * \post start<=i<length
 */

/**
 * \def UTF_BACK_N(s, start, i, n)
 *
 * Move i backward (towards the beginning of the string)
 * to the first code unit of the n code points that have code units before i.
 * I.e., move i backward by n code points.
 * i must point to the first code unit after the last unit of a code point (i==length is allowed).
 * \pre start<i<=length
 * \post start<=i<length
 */

/**
 * \def UTF_SET_CHAR_LIMIT(s, start, i, length)
 *
 * Take the random-access index i and adjust it so that it points beyond
 * a code point. The input index points beyond any code unit
 * of a code point and is moved to point beyond the last code unit of the same
 * code point. i is never decremented.
 * This can be used to start an iteration with UTF_PREV_CHAR() from a random index.
 * \pre start<i<=length
 * \post start<i<=length
 */

#ifdef UTF_SAFE

#   define UTF_GET_CHAR(s, start, i, length, c) UTF_GET_CHAR_SAFE(s, start, i, length, c, FALSE)

#   define UTF_NEXT_CHAR(s, i, length, c)       UTF_NEXT_CHAR_SAFE(s, i, length, c, FALSE)
#   define UTF_APPEND_CHAR(s, i, length, c)     UTF_APPEND_CHAR_SAFE(s, i, length, c)
#   define UTF_FWD_1(s, i, length)              UTF_FWD_1_SAFE(s, i, length)
#   define UTF_FWD_N(s, i, length, n)           UTF_FWD_N_SAFE(s, i, length, n)
#   define UTF_SET_CHAR_START(s, start, i)      UTF_SET_CHAR_START_SAFE(s, start, i)

#   define UTF_PREV_CHAR(s, start, i, c)        UTF_PREV_CHAR_SAFE(s, start, i, c, FALSE)
#   define UTF_BACK_1(s, start, i)              UTF_BACK_1_SAFE(s, start, i)
#   define UTF_BACK_N(s, start, i, n)           UTF_BACK_N_SAFE(s, start, i, n)
#   define UTF_SET_CHAR_LIMIT(s, start, i, length) UTF_SET_CHAR_LIMIT_SAFE(s, start, i, length)

#elif defined(UTF_STRICT)

#   define UTF_GET_CHAR(s, start, i, length, c) UTF_GET_CHAR_SAFE(s, start, i, length, c, TRUE)

#   define UTF_NEXT_CHAR(s, i, length, c)       UTF_NEXT_CHAR_SAFE(s, i, length, c, TRUE)
#   define UTF_APPEND_CHAR(s, i, length, c)     UTF_APPEND_CHAR_SAFE(s, i, length, c)
#   define UTF_FWD_1(s, i, length)              UTF_FWD_1_SAFE(s, i, length)
#   define UTF_FWD_N(s, i, length, n)           UTF_FWD_N_SAFE(s, i, length, n)
#   define UTF_SET_CHAR_START(s, start, i)      UTF_SET_CHAR_START_SAFE(s, start, i)

#   define UTF_PREV_CHAR(s, start, i, c)        UTF_PREV_CHAR_SAFE(s, start, i, c, TRUE)
#   define UTF_BACK_1(s, start, i)              UTF_BACK_1_SAFE(s, start, i)
#   define UTF_BACK_N(s, start, i, n)           UTF_BACK_N_SAFE(s, start, i, n)
#   define UTF_SET_CHAR_LIMIT(s, start, i, length) UTF_SET_CHAR_LIMIT_SAFE(s, start, i, length)

#else /* UTF_UNSAFE */

#   define UTF_GET_CHAR(s, start, i, length, c) UTF_GET_CHAR_UNSAFE(s, i, c)

#   define UTF_NEXT_CHAR(s, i, length, c)       UTF_NEXT_CHAR_UNSAFE(s, i, c)
#   define UTF_APPEND_CHAR(s, i, length, c)     UTF_APPEND_CHAR_UNSAFE(s, i, c)
#   define UTF_FWD_1(s, i, length)              UTF_FWD_1_UNSAFE(s, i)
#   define UTF_FWD_N(s, i, length, n)           UTF_FWD_N_UNSAFE(s, i, n)
#   define UTF_SET_CHAR_START(s, start, i)      UTF_SET_CHAR_START_UNSAFE(s, i)

#   define UTF_PREV_CHAR(s, start, i, c)        UTF_PREV_CHAR_UNSAFE(s, i, c)
#   define UTF_BACK_1(s, start, i)              UTF_BACK_1_UNSAFE(s, i)
#   define UTF_BACK_N(s, start, i, n)           UTF_BACK_N_UNSAFE(s, i, n)
#   define UTF_SET_CHAR_LIMIT(s, start, i, length) UTF_SET_CHAR_LIMIT_UNSAFE(s, i)

#endif

#endif
