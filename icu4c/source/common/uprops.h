/*
*******************************************************************************
*
*   Copyright (C) 2002-2004, International Business Machines
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

#include "unicode/utypes.h"
#include "unicode/uset.h"
#include "udataswp.h"

/* indexes[] entries */
enum {
    UPROPS_PROPS32_INDEX,
    UPROPS_EXCEPTIONS_INDEX,
    UPROPS_EXCEPTIONS_TOP_INDEX,

    UPROPS_ADDITIONAL_TRIE_INDEX,
    UPROPS_ADDITIONAL_VECTORS_INDEX,
    UPROPS_ADDITIONAL_VECTORS_COLUMNS_INDEX,

    UPROPS_RESERVED_INDEX, /* 6 */

    /* maximum values for code values in vector word 0 */
    UPROPS_MAX_VALUES_INDEX=10,
    /* maximum values for code values in vector word 2 */
    UPROPS_MAX_VALUES_2_INDEX,

    UPROPS_INDEX_COUNT=16
};

/* definitions for the main properties words */
enum {
    /* general category shift==0                                0 (5 bits) */
    UPROPS_EXCEPTION_SHIFT=5,                               /*  5 (1 bit)  */
    UPROPS_BIDI_SHIFT,                                      /*  6 (5 bits) */
    UPROPS_MIRROR_SHIFT=UPROPS_BIDI_SHIFT+5,                /* 11 (1 bit)  */
    UPROPS_NUMERIC_TYPE_SHIFT,                              /* 12 (3 bits) */
    UPROPS_CASE_SENSITIVE_SHIFT=UPROPS_NUMERIC_TYPE_SHIFT+3,/* 15 (1 bit) format version 3.2 */
    UPROPS_RESERVED_SHIFT,                                  /* 16 (4 bits) */
    UPROPS_VALUE_SHIFT=20,                                  /* 20 */

    UPROPS_EXCEPTION_BIT=1UL<<UPROPS_EXCEPTION_SHIFT,
    UPROPS_VALUE_BITS=32-UPROPS_VALUE_SHIFT,

    UPROPS_MIN_VALUE=-(1L<<(UPROPS_VALUE_BITS-1)),
    UPROPS_MAX_VALUE=(1L<<(UPROPS_VALUE_BITS-1))-1,
    UPROPS_MAX_EXCEPTIONS_COUNT=1L<<UPROPS_VALUE_BITS
};

#define PROPS_VALUE_IS_EXCEPTION(props) ((props)&UPROPS_EXCEPTION_BIT)
#define GET_CATEGORY(props) ((props)&0x1f)
#define GET_BIDI_CLASS(props) ((props>>UPROPS_BIDI_SHIFT)&0x1f)
#define GET_NUMERIC_TYPE(props) (((props)>>UPROPS_NUMERIC_TYPE_SHIFT)&7)
#define GET_UNSIGNED_VALUE(props) ((props)>>UPROPS_VALUE_SHIFT)
#define GET_SIGNED_VALUE(props) ((int32_t)(props)>>UPROPS_VALUE_SHIFT)
#define GET_EXCEPTIONS(props) (exceptionsTable+GET_UNSIGNED_VALUE(props))

#define CAT_MASK(props) U_MASK(GET_CATEGORY(props))

enum {
    EXC_UPPERCASE,
    EXC_LOWERCASE,
    EXC_TITLECASE,
    EXC_UNUSED,
    EXC_NUMERIC_VALUE,
    EXC_DENOMINATOR_VALUE,
    EXC_MIRROR_MAPPING,
    EXC_SPECIAL_CASING,
    EXC_CASE_FOLDING
};

/* number of properties vector words */
#define UPROPS_VECTOR_WORDS     3

/*
 * Properties in vector word 0
 * Bits
 * 31..24   DerivedAge version major/minor one nibble each
 * 23       reserved
 * 22..18   Line Break
 * 17..15   East Asian Width
 * 14.. 7   UBlockCode
 *  6.. 0   UScriptCode
 */

/* derived age: one nibble each for major and minor version numbers */
#define UPROPS_AGE_MASK         0xff000000
#define UPROPS_AGE_SHIFT        24

#define UPROPS_LB_MASK          0x007C0000
#define UPROPS_LB_SHIFT         18

#define UPROPS_EA_MASK          0x00038000
#define UPROPS_EA_SHIFT         15

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
 *
 * ICU 2.6/uprops format version 3.2 stores full properties instead of "Other_".
 */
enum {
    UPROPS_WHITE_SPACE,
    UPROPS_BIDI_CONTROL,
    UPROPS_JOIN_CONTROL,
    UPROPS_DASH,
    UPROPS_HYPHEN,
    UPROPS_QUOTATION_MARK,
    UPROPS_TERMINAL_PUNCTUATION,
    UPROPS_MATH,
    UPROPS_HEX_DIGIT,
    UPROPS_ASCII_HEX_DIGIT,
    UPROPS_ALPHABETIC,
    UPROPS_IDEOGRAPHIC,
    UPROPS_DIACRITIC,
    UPROPS_EXTENDER,
    UPROPS_LOWERCASE,
    UPROPS_UPPERCASE,
    UPROPS_NONCHARACTER_CODE_POINT,
    UPROPS_GRAPHEME_EXTEND,
    UPROPS_GRAPHEME_LINK,
    UPROPS_IDS_BINARY_OPERATOR,
    UPROPS_IDS_TRINARY_OPERATOR,
    UPROPS_RADICAL,
    UPROPS_UNIFIED_IDEOGRAPH,
    UPROPS_DEFAULT_IGNORABLE_CODE_POINT,
    UPROPS_DEPRECATED,
    UPROPS_SOFT_DOTTED,
    UPROPS_LOGICAL_ORDER_EXCEPTION,
    UPROPS_XID_START,
    UPROPS_XID_CONTINUE,
    UPROPS_ID_START,                            /* ICU 2.6, uprops format version 3.2 */
    UPROPS_ID_CONTINUE,
    UPROPS_GRAPHEME_BASE,
    UPROPS_BINARY_1_TOP                         /* ==32 - full! */
};

/*
 * Properties in vector word 2
 * Bits
 * 31..24   More binary properties
 * 13..11   Joining Type
 * 10.. 5   Joining Group
 *  4.. 0   Decomposition Type
 */
#define UPROPS_JT_MASK          0x00003800
#define UPROPS_JT_SHIFT         11

#define UPROPS_JG_MASK          0x000007e0
#define UPROPS_JG_SHIFT         5

#define UPROPS_DT_MASK          0x0000001f

enum {
    UPROPS_V2_S_TERM=24,                        /* new in ICU 3.0 and Unicode 4.0.1 */
    UPROPS_V2_VARIATION_SELECTOR,
    UPROPS_V2_TOP                               /* must be <=32 */
};

/**
 * Get a properties vector word for a code point.
 * Implemented in uchar.c for uprops.c.
 * column==-1 gets the 32-bit main properties word instead.
 * @return 0 if no data or illegal argument
 */
U_CFUNC uint32_t
u_getUnicodeProperties(UChar32 c, int32_t column);

/**
 * Get the the maximum values for some enum/int properties.
 * Use the same column numbers as for u_getUnicodeProperties().
 * The returned value will contain maximum values stored in the same bit fields
 * as where the enum values are stored in the u_getUnicodeProperties()
 * return values for the same columns.
 *
 * Valid columns are those for properties words that contain enumerated values.
 * (ICU 2.6: columns 0 and 2)
 * For other column numbers, this function will return 0.
 *
 * @internal
 */
U_CFUNC int32_t
uprv_getMaxValues(int32_t column);

/**
 * \var uprv_comparePropertyNames
 * Unicode property names and property value names are compared "loosely".
 *
 * UCD.html 4.0.1 says:
 *   For all property names, property value names, and for property values for
 *   Enumerated, Binary, or Catalog properties, use the following
 *   loose matching rule:
 *
 *   LM3. Ignore case, whitespace, underscore ('_'), and hyphens.
 *
 * This function does just that, for (char *) name strings.
 * It is almost identical to ucnv_compareNames() but also ignores
 * C0 White_Space characters (U+0009..U+000d, and U+0085 on EBCDIC).
 *
 * @internal
 */

U_CAPI int32_t U_EXPORT2
uprv_compareASCIIPropertyNames(const char *name1, const char *name2);

U_CAPI int32_t U_EXPORT2
uprv_compareEBCDICPropertyNames(const char *name1, const char *name2);

#if U_CHARSET_FAMILY==U_ASCII_FAMILY
#   define uprv_comparePropertyNames uprv_compareASCIIPropertyNames
#elif U_CHARSET_FAMILY==U_EBCDIC_FAMILY
#   define uprv_comparePropertyNames uprv_compareEBCDICPropertyNames
#else
#   error U_CHARSET_FAMILY is not valid
#endif

/** Turn a bit index into a bit flag. @internal */
#define FLAG(n) ((uint32_t)1<<(n))

/** Flags for general categories in the order of UCharCategory. @internal */
#define _Cn     FLAG(U_GENERAL_OTHER_TYPES)
#define _Lu     FLAG(U_UPPERCASE_LETTER)
#define _Ll     FLAG(U_LOWERCASE_LETTER)
#define _Lt     FLAG(U_TITLECASE_LETTER)
#define _Lm     FLAG(U_MODIFIER_LETTER)
#define _Lo     FLAG(U_OTHER_LETTER)
#define _Mn     FLAG(U_NON_SPACING_MARK)
#define _Me     FLAG(U_ENCLOSING_MARK)
#define _Mc     FLAG(U_COMBINING_SPACING_MARK)
#define _Nd     FLAG(U_DECIMAL_DIGIT_NUMBER)
#define _Nl     FLAG(U_LETTER_NUMBER)
#define _No     FLAG(U_OTHER_NUMBER)
#define _Zs     FLAG(U_SPACE_SEPARATOR)
#define _Zl     FLAG(U_LINE_SEPARATOR)
#define _Zp     FLAG(U_PARAGRAPH_SEPARATOR)
#define _Cc     FLAG(U_CONTROL_CHAR)
#define _Cf     FLAG(U_FORMAT_CHAR)
#define _Co     FLAG(U_PRIVATE_USE_CHAR)
#define _Cs     FLAG(U_SURROGATE)
#define _Pd     FLAG(U_DASH_PUNCTUATION)
#define _Ps     FLAG(U_START_PUNCTUATION)
#define _Pe     FLAG(U_END_PUNCTUATION)
#define _Pc     FLAG(U_CONNECTOR_PUNCTUATION)
#define _Po     FLAG(U_OTHER_PUNCTUATION)
#define _Sm     FLAG(U_MATH_SYMBOL)
#define _Sc     FLAG(U_CURRENCY_SYMBOL)
#define _Sk     FLAG(U_MODIFIER_SYMBOL)
#define _So     FLAG(U_OTHER_SYMBOL)
#define _Pi     FLAG(U_INITIAL_PUNCTUATION)
#define _Pf     FLAG(U_FINAL_PUNCTUATION)

/** Some code points. @internal */
enum {
    TAB     =0x0009,
    LF      =0x000a,
    FF      =0x000c,
    CR      =0x000d,
    U_A     =0x0041,
    U_Z     =0x005a,
    U_a     =0x0061,
    U_z     =0x007a,
    DEL     =0x007f,
    NL      =0x0085,
    NBSP    =0x00a0,
    CGJ     =0x034f,
    FIGURESP=0x2007,
    HAIRSP  =0x200a,
    ZWNJ    =0x200c,
    ZWJ     =0x200d,
    RLM     =0x200f,
    NNBSP   =0x202f,
    WJ      =0x2060,
    INHSWAP =0x206a,
    NOMDIG  =0x206f,
    ZWNBSP  =0xfeff
};

/**
 * Is this character a "white space" in the sense of ICU rule parsers?
 * @internal
 */
U_CAPI UBool U_EXPORT2
uprv_isRuleWhiteSpace(UChar32 c);

/**
 * Get the set of "white space" characters in the sense of ICU rule
 * parsers.  Caller must close/delete result.
 * @internal
 */
U_CAPI USet* U_EXPORT2
uprv_openRuleWhiteSpaceSet(UErrorCode* ec);

/**
 * Get the maximum length of a (regular/1.0/extended) character name.
 * @return 0 if no character names available.
 */
U_CAPI int32_t U_EXPORT2
uprv_getMaxCharNameLength(void);

#if 0
/* 
Currently not used but left for future use. Probably by UnicodeSet. 
urename.h and unames.c changed accordingly. 
*/
/**
 * Get the maximum length of an ISO comment.
 * @return 0 if no ISO comments available.
 */
U_CAPI int32_t U_EXPORT2
uprv_getMaxISOCommentLength();
#endif

/**
 * Fills set with characters that are used in Unicode character names.
 * Includes all characters that are used in regular/Unicode 1.0/extended names.
 * Just empties the set if no character names are available.
 * @param set USet to receive characters. Existing contents are deleted.
 */
U_CAPI void U_EXPORT2
uprv_getCharNameCharacters(USet* set);

#if 0
/* 
Currently not used but left for future use. Probably by UnicodeSet. 
urename.h and unames.c changed accordingly. 
*/
/**
 * Fills set with characters that are used in Unicode character names.
 * Just empties the set if no ISO comments are available.
 * @param set USet to receive characters. Existing contents are deleted.
 */
U_CAPI void U_EXPORT2
uprv_getISOCommentCharacters(USet* set);
*/
#endif

/**
 * Enumerate each core properties data trie and add the
 * start of each range of same properties to the set.
 * @internal
 */
U_CAPI void U_EXPORT2
uchar_addPropertyStarts(USet *set, UErrorCode *pErrorCode);

/**
 * Return a set of characters for property enumeration.
 * For each two consecutive characters (start, limit) in the set,
 * all of the properties for start..limit-1 are all the same.
 *
 * @param set USet to receive result. Existing contents are lost.
 * @internal
 */
U_CAPI void U_EXPORT2
uprv_getInclusions(USet* set, UErrorCode *pErrorCode);

/**
 * Swap the ICU Unicode properties file. See uchar.c.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
uprops_swap(const UDataSwapper *ds,
            const void *inData, int32_t length, void *outData,
            UErrorCode *pErrorCode);

/**
 * Swap the ICU Unicode character names file. See uchar.c.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
uchar_swapNames(const UDataSwapper *ds,
                const void *inData, int32_t length, void *outData,
                UErrorCode *pErrorCode);

#endif
