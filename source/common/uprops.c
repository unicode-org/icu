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
*   Implementations for mostly non-core Unicode character properties
*   stored in uprops.icu.
*
*   With the APIs implemented here, almost all properties files and
*   their associated implementation files are used from this file,
*   including those for normalization and case mappings.
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/uscript.h"
#include "cstring.h"
#include "unormimp.h"
#include "uprops.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

/* API functions ------------------------------------------------------------ */

static const struct {
    int32_t column;
    uint32_t mask;
} binProps[UCHAR_BINARY_LIMIT]={
    /*
     * column and mask values for binary properties from u_getUnicodeProperties().
     * Must be in order of corresponding UProperty,
     * and there must be exacly one entry per binary UProperty.
     *
     * Properties with mask 0 are handled in code.
     * For them, column is the UPropertySource value.
     */
    {  1,               U_MASK(UPROPS_ALPHABETIC) },
    {  1,               U_MASK(UPROPS_ASCII_HEX_DIGIT) },
    {  1,               U_MASK(UPROPS_BIDI_CONTROL) },
    { -1,               U_MASK(UPROPS_MIRROR_SHIFT) },
    {  1,               U_MASK(UPROPS_DASH) },
    {  1,               U_MASK(UPROPS_DEFAULT_IGNORABLE_CODE_POINT) },
    {  1,               U_MASK(UPROPS_DEPRECATED) },
    {  1,               U_MASK(UPROPS_DIACRITIC) },
    {  1,               U_MASK(UPROPS_EXTENDER) },
    { UPROPS_SRC_NORM,  0 },                                    /* UCHAR_FULL_COMPOSITION_EXCLUSION */
    {  1,               U_MASK(UPROPS_GRAPHEME_BASE) },
    {  1,               U_MASK(UPROPS_GRAPHEME_EXTEND) },
    {  1,               U_MASK(UPROPS_GRAPHEME_LINK) },
    {  1,               U_MASK(UPROPS_HEX_DIGIT) },
    {  1,               U_MASK(UPROPS_HYPHEN) },
    {  1,               U_MASK(UPROPS_ID_CONTINUE) },
    {  1,               U_MASK(UPROPS_ID_START) },
    {  1,               U_MASK(UPROPS_IDEOGRAPHIC) },
    {  1,               U_MASK(UPROPS_IDS_BINARY_OPERATOR) },
    {  1,               U_MASK(UPROPS_IDS_TRINARY_OPERATOR) },
    {  1,               U_MASK(UPROPS_JOIN_CONTROL) },
    {  1,               U_MASK(UPROPS_LOGICAL_ORDER_EXCEPTION) },
    { UPROPS_SRC_CASE,  0 },                                    /* UCHAR_LOWERCASE */
    {  1,               U_MASK(UPROPS_MATH) },
    {  1,               U_MASK(UPROPS_NONCHARACTER_CODE_POINT) },
    {  1,               U_MASK(UPROPS_QUOTATION_MARK) },
    {  1,               U_MASK(UPROPS_RADICAL) },
    { UPROPS_SRC_CASE,  0 },                                    /* UCHAR_SOFT_DOTTED */
    {  1,               U_MASK(UPROPS_TERMINAL_PUNCTUATION) },
    {  1,               U_MASK(UPROPS_UNIFIED_IDEOGRAPH) },
    { UPROPS_SRC_CASE,  0 },                                    /* UCHAR_UPPERCASE */
    {  1,               U_MASK(UPROPS_WHITE_SPACE) },
    {  1,               U_MASK(UPROPS_XID_CONTINUE) },
    {  1,               U_MASK(UPROPS_XID_START) },
    { UPROPS_SRC_CASE,  0 },                                    /* UCHAR_CASE_SENSITIVE */
    {  2,               U_MASK(UPROPS_V2_S_TERM) },
    {  2,               U_MASK(UPROPS_V2_VARIATION_SELECTOR) },
    { UPROPS_SRC_NORM,  0 },                                    /* UCHAR_NFD_INERT */
    { UPROPS_SRC_NORM,  0 },                                    /* UCHAR_NFKD_INERT */
    { UPROPS_SRC_NORM,  0 },                                    /* UCHAR_NFC_INERT */
    { UPROPS_SRC_NORM,  0 },                                    /* UCHAR_NFKC_INERT */
    { UPROPS_SRC_NORM,  0 }                                     /* UCHAR_SEGMENT_STARTER */
};

U_CAPI UBool U_EXPORT2
u_hasBinaryProperty(UChar32 c, UProperty which) {
    /* c is range-checked in the functions that are called from here */
    if(which<UCHAR_BINARY_START || UCHAR_BINARY_LIMIT<=which) {
        /* not a known binary property */
    } else {
        uint32_t mask=binProps[which].mask;
        int32_t column=binProps[which].column;
        if(mask!=0) {
            /* systematic, directly stored properties */
            return (u_getUnicodeProperties(c, column)&mask)!=0;
        } else {
            if(column==UPROPS_SRC_CASE) {
                /* case mapping properties */
                UErrorCode errorCode=U_ZERO_ERROR;
                UCaseProps *csp=ucase_getSingleton(&errorCode);
                if(U_FAILURE(errorCode)) {
                    return FALSE;
                }
                switch(which) {
                case UCHAR_LOWERCASE:
                    return (UBool)(UCASE_LOWER==ucase_getType(csp, c));
                case UCHAR_UPPERCASE:
                    return (UBool)(UCASE_UPPER==ucase_getType(csp, c));
                case UCHAR_SOFT_DOTTED:
                    return ucase_isSoftDotted(csp, c);
                case UCHAR_CASE_SENSITIVE:
                    return ucase_isCaseSensitive(csp, c);
                default:
                    break;
                }
            } else if(column==UPROPS_SRC_NORM) {
#if !UCONFIG_NO_NORMALIZATION
                /* normalization properties from unorm.icu */
                switch(which) {
                case UCHAR_FULL_COMPOSITION_EXCLUSION:
                    return unorm_internalIsFullCompositionExclusion(c);
                case UCHAR_NFD_INERT:
                case UCHAR_NFKD_INERT:
                case UCHAR_NFC_INERT:
                case UCHAR_NFKC_INERT:
                    return unorm_isNFSkippable(c, (UNormalizationMode)(which-UCHAR_NFD_INERT)+UNORM_NFD);
                case UCHAR_SEGMENT_STARTER:
                    return unorm_isCanonSafeStart(c);
                default:
                    break;
                }
#endif
            }
        }
    }
    return FALSE;
}

U_CAPI int32_t U_EXPORT2
u_getIntPropertyValue(UChar32 c, UProperty which) {
    UErrorCode errorCode;

    if(which<UCHAR_BINARY_START) {
        return 0; /* undefined */
    } else if(which<UCHAR_BINARY_LIMIT) {
        return (int32_t)u_hasBinaryProperty(c, which);
    } else if(which<UCHAR_INT_START) {
        return 0; /* undefined */
    } else if(which<UCHAR_INT_LIMIT) {
        switch(which) {
        case UCHAR_BIDI_CLASS:
            return (int32_t)u_charDirection(c);
        case UCHAR_BLOCK:
            return (int32_t)ublock_getCode(c);
        case UCHAR_CANONICAL_COMBINING_CLASS:
#if !UCONFIG_NO_NORMALIZATION
            return u_getCombiningClass(c);
#else
            return 0;
#endif
        case UCHAR_DECOMPOSITION_TYPE:
            return (int32_t)(u_getUnicodeProperties(c, 2)&UPROPS_DT_MASK);
        case UCHAR_EAST_ASIAN_WIDTH:
            return (int32_t)(u_getUnicodeProperties(c, 0)&UPROPS_EA_MASK)>>UPROPS_EA_SHIFT;
        case UCHAR_GENERAL_CATEGORY:
            return (int32_t)u_charType(c);
        case UCHAR_JOINING_GROUP:
            return (int32_t)(u_getUnicodeProperties(c, 2)&UPROPS_JG_MASK)>>UPROPS_JG_SHIFT;
        case UCHAR_JOINING_TYPE:
            return (int32_t)(u_getUnicodeProperties(c, 2)&UPROPS_JT_MASK)>>UPROPS_JT_SHIFT;
        case UCHAR_LINE_BREAK:
            return (int32_t)(u_getUnicodeProperties(c, 0)&UPROPS_LB_MASK)>>UPROPS_LB_SHIFT;
        case UCHAR_NUMERIC_TYPE:
            return (int32_t)GET_NUMERIC_TYPE(u_getUnicodeProperties(c, -1));
        case UCHAR_SCRIPT:
            errorCode=U_ZERO_ERROR;
            return (int32_t)uscript_getScript(c, &errorCode);
        case UCHAR_HANGUL_SYLLABLE_TYPE:
            return uchar_getHST(c);
#if !UCONFIG_NO_NORMALIZATION
        case UCHAR_NFD_QUICK_CHECK:
        case UCHAR_NFKD_QUICK_CHECK:
        case UCHAR_NFC_QUICK_CHECK:
        case UCHAR_NFKC_QUICK_CHECK:
            return (int32_t)unorm_getQuickCheck(c, (UNormalizationMode)(which-UCHAR_NFD_QUICK_CHECK)+UNORM_NFD);
        case UCHAR_LEAD_CANONICAL_COMBINING_CLASS:
            return unorm_getFCD16FromCodePoint(c)>>8;
        case UCHAR_TRAIL_CANONICAL_COMBINING_CLASS:
            return unorm_getFCD16FromCodePoint(c)&0xff;
#endif
        default:
            return 0; /* undefined */
        }
    } else if(which==UCHAR_GENERAL_CATEGORY_MASK) {
        return U_MASK(u_charType(c));
    } else {
        return 0; /* undefined */
    }
}

U_CAPI int32_t U_EXPORT2
u_getIntPropertyMinValue(UProperty which) {
    return 0; /* all binary/enum/int properties have a minimum value of 0 */
}

U_CAPI int32_t U_EXPORT2
u_getIntPropertyMaxValue(UProperty which) {
    int32_t max;

    if(which<UCHAR_BINARY_START) {
        return -1; /* undefined */
    } else if(which<UCHAR_BINARY_LIMIT) {
        return 1; /* maximum TRUE for all binary properties */
    } else if(which<UCHAR_INT_START) {
        return -1; /* undefined */
    } else if(which<UCHAR_INT_LIMIT) {
        switch(which) {
        case UCHAR_BIDI_CLASS:
            return (int32_t)U_CHAR_DIRECTION_COUNT-1;
        case UCHAR_BLOCK:
            max=(uprv_getMaxValues(0)&UPROPS_BLOCK_MASK)>>UPROPS_BLOCK_SHIFT;
            return max!=0 ? max : (int32_t)UBLOCK_COUNT-1;
        case UCHAR_CANONICAL_COMBINING_CLASS:
        case UCHAR_LEAD_CANONICAL_COMBINING_CLASS:
        case UCHAR_TRAIL_CANONICAL_COMBINING_CLASS:
            return 0xff; /* TODO do we need to be more precise, getting the actual maximum? */
        case UCHAR_DECOMPOSITION_TYPE:
            max=uprv_getMaxValues(2)&UPROPS_DT_MASK;
            return max!=0 ? max : (int32_t)U_DT_COUNT-1;
        case UCHAR_EAST_ASIAN_WIDTH:
            max=(uprv_getMaxValues(0)&UPROPS_EA_MASK)>>UPROPS_EA_SHIFT;
            return max!=0 ? max : (int32_t)U_EA_COUNT-1;
        case UCHAR_GENERAL_CATEGORY:
            return (int32_t)U_CHAR_CATEGORY_COUNT-1;
        case UCHAR_JOINING_GROUP:
            max=(uprv_getMaxValues(2)&UPROPS_JG_MASK)>>UPROPS_JG_SHIFT;
            return max!=0 ? max : (int32_t)U_JG_COUNT-1;
        case UCHAR_JOINING_TYPE:
            max=(uprv_getMaxValues(2)&UPROPS_JT_MASK)>>UPROPS_JT_SHIFT;
            return max!=0 ? max : (int32_t)U_JT_COUNT-1;
        case UCHAR_LINE_BREAK:
            max=(uprv_getMaxValues(0)&UPROPS_LB_MASK)>>UPROPS_LB_SHIFT;
            return max!=0 ? max : (int32_t)U_LB_COUNT-1;
        case UCHAR_NUMERIC_TYPE:
            return (int32_t)U_NT_COUNT-1;
        case UCHAR_SCRIPT:
            max=uprv_getMaxValues(0)&UPROPS_SCRIPT_MASK;
            return max!=0 ? max : (int32_t)USCRIPT_CODE_LIMIT-1;
        case UCHAR_HANGUL_SYLLABLE_TYPE:
            return (int32_t)U_HST_COUNT-1;
#if !UCONFIG_NO_NORMALIZATION
        case UCHAR_NFD_QUICK_CHECK:
        case UCHAR_NFKD_QUICK_CHECK:
            return (int32_t)UNORM_YES; /* these are never "maybe", only "no" or "yes" */
        case UCHAR_NFC_QUICK_CHECK:
        case UCHAR_NFKC_QUICK_CHECK:
            return (int32_t)UNORM_MAYBE;
#endif
        default:
            return -1; /* undefined */
        }
    } else {
        return -1; /* undefined */
    }
}

U_CAPI UPropertySource U_EXPORT2
uprops_getSource(UProperty which) {
    if(which<UCHAR_BINARY_START) {
        return UPROPS_SRC_NONE; /* undefined */
    } else if(which<UCHAR_BINARY_LIMIT) {
        if(binProps[which].mask!=0) {
            return UPROPS_SRC_CHAR;
        } else {
            return (UPropertySource)binProps[which].column;
        }
    } else if(which<UCHAR_INT_START) {
        return UPROPS_SRC_NONE; /* undefined */
    } else if(which<UCHAR_INT_LIMIT) {
        switch(which) {
        case UCHAR_HANGUL_SYLLABLE_TYPE:
            return UPROPS_SRC_HST;
        case UCHAR_CANONICAL_COMBINING_CLASS:
        case UCHAR_NFD_QUICK_CHECK:
        case UCHAR_NFKD_QUICK_CHECK:
        case UCHAR_NFC_QUICK_CHECK:
        case UCHAR_NFKC_QUICK_CHECK:
        case UCHAR_LEAD_CANONICAL_COMBINING_CLASS:
        case UCHAR_TRAIL_CANONICAL_COMBINING_CLASS:
            return UPROPS_SRC_NORM;
        default:
            return UPROPS_SRC_CHAR;
        }
    } else if(which==UCHAR_GENERAL_CATEGORY_MASK) {
        return UPROPS_SRC_CHAR;
    } else {
        return UPROPS_SRC_NONE; /* undefined */
    }
}

/*----------------------------------------------------------------
 * Inclusions list
 *----------------------------------------------------------------*/

/*
 * Return a set of characters for property enumeration.
 * The set implicitly contains 0x110000 as well, which is one more than the highest
 * Unicode code point.
 *
 * This set is used as an ordered list - its code points are ordered, and
 * consecutive code points (in Unicode code point order) in the set define a range.
 * For each two consecutive characters (start, limit) in the set,
 * all of the UCD/normalization and related properties for
 * all code points start..limit-1 are all the same,
 * except for character names and ISO comments.
 *
 * All Unicode code points U+0000..U+10ffff are covered by these ranges.
 * The ranges define a partition of the Unicode code space.
 * ICU uses the inclusions set to enumerate properties for generating
 * UnicodeSets containing all code points that have a certain property value.
 *
 * The Inclusion List is generated from the UCD. It is generated
 * by enumerating the data tries, and code points for hardcoded properties
 * are added as well.
 *
 * --------------------------------------------------------------------------
 *
 * The following are ideas for getting properties-unique code point ranges,
 * with possible optimizations beyond the current implementation.
 * These optimizations would require more code and be more fragile.
 * The current implementation generates one single list (set) for all properties.
 *
 * To enumerate properties efficiently, one needs to know ranges of
 * repetitive values, so that the value of only each start code point
 * can be applied to the whole range.
 * This information is in principle available in the uprops.icu/unorm.icu data.
 *
 * There are two obstacles:
 *
 * 1. Some properties are computed from multiple data structures,
 *    making it necessary to get repetitive ranges by intersecting
 *    ranges from multiple tries.
 *
 * 2. It is not economical to write code for getting repetitive ranges
 *    that are precise for each of some 50 properties.
 *
 * Compromise ideas:
 *
 * - Get ranges per trie, not per individual property.
 *   Each range contains the same values for a whole group of properties.
 *   This would generate currently five range sets, two for uprops.icu tries
 *   and three for unorm.icu tries.
 *
 * - Combine sets of ranges for multiple tries to get sufficient sets
 *   for properties, e.g., the uprops.icu main and auxiliary tries
 *   for all non-normalization properties.
 *
 * Ideas for representing ranges and combining them:
 *
 * - A UnicodeSet could hold just the start code points of ranges.
 *   Multiple sets are easily combined by or-ing them together.
 *
 * - Alternatively, a UnicodeSet could hold each even-numbered range.
 *   All ranges could be enumerated by using each start code point
 *   (for the even-numbered ranges) as well as each limit (end+1) code point
 *   (for the odd-numbered ranges).
 *   It should be possible to combine two such sets by xor-ing them,
 *   but no more than two.
 *
 * The second way to represent ranges may(?!) yield smaller UnicodeSet arrays,
 * but the first one is certainly simpler and applicable for combining more than
 * two range sets.
 *
 * It is possible to combine all range sets for all uprops/unorm tries into one
 * set that can be used for all properties.
 * As an optimization, there could be less-combined range sets for certain
 * groups of properties.
 * The relationship of which less-combined range set to use for which property
 * depends on the implementation of the properties and must be hardcoded
 * - somewhat error-prone and higher maintenance but can be tested easily
 * by building property sets "the simple way" in test code.
 *
 * ---
 *
 * Do not use a UnicodeSet pattern because that causes infinite recursion;
 * UnicodeSet depends on the inclusions set.
 *
 * ---
 *
 * uprv_getInclusions() is commented out starting 2004-sep-13 because
 * uniset_props.cpp now calls the uxyz_addPropertyStarts() directly,
 * and only for the relevant property source.
 */
#if 0

U_CAPI void U_EXPORT2
uprv_getInclusions(USetAdder *sa, UErrorCode *pErrorCode) {
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

#if !UCONFIG_NO_NORMALIZATION
    unorm_addPropertyStarts(sa, pErrorCode);
#endif
    uchar_addPropertyStarts(sa, pErrorCode);
    ucase_addPropertyStarts(ucase_getSingleton(pErrorCode), sa, pErrorCode);
}

#endif
