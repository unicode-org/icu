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

/* definitions for the main properties words */
enum {
    /* general category shift==0                            0 (5 bits) */
    UPROPS_EXCEPTION_SHIFT=5,                           /*  5 (1 bit)  */
    UPROPS_BIDI_SHIFT,                                  /*  6 (5 bits) */
    UPROPS_MIRROR_SHIFT=UPROPS_BIDI_SHIFT+5,            /* 11 (1 bit)  */
    UPROPS_NUMERIC_TYPE_SHIFT,                          /* 12 (3 bits) */
    UPROPS_RESERVED_SHIFT=UPROPS_NUMERIC_TYPE_SHIFT+3,  /* 15 (5 bits) */
    UPROPS_VALUE_SHIFT=20,                              /* 20 */

    UPROPS_EXCEPTION_BIT=1UL<<UPROPS_EXCEPTION_SHIFT,
    UPROPS_VALUE_BITS=32-UPROPS_VALUE_SHIFT,

    UPROPS_MIN_VALUE=-(1L<<(UPROPS_VALUE_BITS-1)),
    UPROPS_MAX_VALUE=(1L<<(UPROPS_VALUE_BITS-1))-1,
    UPROPS_MAX_EXCEPTIONS_COUNT=1L<<UPROPS_VALUE_BITS
};

/* number of properties vector words */
#define UPROPS_VECTOR_WORDS     2

/*
 * Properties in vector word 0
 * Bits
 * 31..24   DerivedAge version major/minor one nibble each
 * 23..18   reserved
 * 17..15   East Asian Width
 * 14.. 7   UBlockCode
 *  6.. 0   UScriptCode
 */

/* derived age: one nibble each for major and minor version numbers */
#define UPROPS_AGE_MASK         0xff000000
#define UPROPS_AGE_SHIFT        24

#define UPROPS_EA_WIDTH_MASK    0x00038000
#define UPROPS_EA_WIDTH_SHIFT   15

#define UPROPS_BLOCK_MASK       0x00007f80
#define UPROPS_BLOCK_SHIFT      7

#define UPROPS_SCRIPT_MASK      0x0000007f

/*
 * Properties in vector word 1
 * Each bit encodes one binary property.
 * The following constants represent the bit number, use 1<<UPROPS_XYZ.
 * UPROPS_BINARY_1_TOP<=32!
 *
 * Keep this list of property enums in sync with
 * propListNames[] in icu/source/tools/genprops/props2.c!
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

    /* derivedPropListNames[] in genprops/props2.c, not easily derivable */
    UPROPS_XID_START,
    UPROPS_XID_CONTINUE,

    UPROPS_BINARY_1_TOP
};

/**
 * Get a properties vector word for a code point.
 * Implemented in uchar.c for uprops.c.
 * @return 0 if no data or illegal argument
 */
U_CFUNC uint32_t
u_getUnicodeProperties(UChar32 c, int32_t column);

/* ### TODO check with PropertyValueAliases.txt and move to uchar.h, @draft ICU 2.x */
/**
 * East Asian Widths constants.
 * Keep in sync with names list in genprops/props2.c.
 */
enum UEAWidthCode {
    U_EA_NEUTRAL,
    U_EA_AMBIGUOUS,
    U_EA_HALF_WIDTH,
    U_EA_FULL_WIDTH,
    U_EA_NARROW,
    U_EA_WIDE,
    U_EA_TOP
};
typedef enum UEAWidthCode UEAWidthCode;

/**
 * Unicode property names and property value names are compared
 * "loosely". Property[Value]Aliases.txt say:
 *   "With loose matching of property names, the case distinctions, whitespace,
 *    and '_' are ignored."
 *
 * This function does just that, for ASCII (char *) name strings.
 * It is almost identical to ucnv_compareNames() but also ignores
 * ASCII White_Space characters (U+0009..U+000d).
 *
 * @internal
 */
U_CAPI int32_t U_EXPORT2
uprv_comparePropertyNames(const char *name1, const char *name2);

#endif
