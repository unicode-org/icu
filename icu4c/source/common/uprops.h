/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uprops.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002feb24
*   created by: Markus W. Scherer
*
*   Constants for mostly non-core Unicode character properties
*   stored in uprops.dat.
*/

#ifndef __UPROPS_H__
#define __UPROPS_H__

/* indexes[] entries */
enum {
    UPROPS_PROPS32_INDEX,
    UPROPS_EXCEPTIONS_INDEX,
    UPROPS_EXCEPTIONS_TOP_INDEX,

    UPROPS_ADDITIONAL_TRIE_INDEX,
    UPROPS_ADDITIONAL_VECTORS_INDEX,
    UPROPS_ADDITIONAL_VECTORS_COLUMNS_INDEX,

    UPROPS_RESERVED_INDEX,

    UPROPS_INDEX_COUNT=16
};

/* number of properties vector words */
#define UPROPS_VECTOR_WORDS     2

/*
 * Properties in vector word 0
 * Bits
 * 31..24   DerivedAge version major/minor one nibble each
 *  6.. 0   UScriptCode
 */

/* derived age: one nibble each for major and minor version numbers */
#define UPROPS_AGE_MASK         0xff000000
#define UPROPS_AGE_SHIFT        24

#define UPROPS_BLOCK_MASK       0x00007f80
#define UPROPS_BLOCK_SHIFT      7

#define UPROPS_SCRIPT_MASK      0x0000007f

/*
 * Properties in vector word 1
 * Each bit encodes one binary property.
 * The following constants represent the bit number, use 1<<UPROPS_XYZ.
 * UPROPS_BINARY_1_TOP<=32!
 */
enum {
    UPROPS_WHITE_SPACE,
    UPROPS_BIDI_CONTROL,
    UPROPS_JOIN_CONTROL,
    UPROPS_DASH,
    UPROPS_HYPHEN,
    UPROPS_QUOTATION_MARK,
    UPROPS_TERMINAL_PUNCTUATION,
    UPROPS_OTHER_MATH,
    UPROPS_HEX_DIGIT,
    UPROPS_ASCII_HEX_DIGIT,
    UPROPS_OTHER_ALPHABETIC,
    UPROPS_IDEOGRAPHIC,
    UPROPS_DIACRITIC,
    UPROPS_EXTENDER,
    UPROPS_OTHER_LOWERCASE,
    UPROPS_OTHER_UPPERCASE,
    UPROPS_NONCHARACTER_CODE_POINT,
    UPROPS_OTHER_GRAPHEME_EXTEND,
    UPROPS_GRAPHEME_LINK,
    UPROPS_IDS_BINARY_OPERATOR,
    UPROPS_IDS_TRINARY_OPERATOR,
    UPROPS_RADICAL,
    UPROPS_UNIFIED_IDEOGRAPH,
    UPROPS_OTHER_DEFAULT_IGNORABLE_CODE_POINT,
    UPROPS_DEPRECATED,
    UPROPS_SOFT_DOTTED,
    UPROPS_LOGICAL_ORDER_EXCEPTION,
    UPROPS_BINARY_1_TOP
};

/**
 * Get a properties vector word for a code point.
 * Implemented in uchar.c for uprops.c.
 * @return 0 if no data or illegal argument
 */
U_CFUNC uint32_t
u_getUnicodeProperties(UChar32 c, int32_t column);

#endif
