/*
*******************************************************************************
*
*   Copyright (C) 2002-2003, International Business Machines
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
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/uscript.h"
#include "cstring.h"
#include "unormimp.h"
#include "uprops.h"

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

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
uprv_comparePropertyNames(const char *name1, const char *name2) {
    int32_t rc;
    unsigned char c1, c2;

    for(;;) {
        /* Ignore delimiters '-', '_', and ASCII White_Space */
        while((c1=(unsigned char)*name1)=='-' || c1=='_' ||
              c1==' ' || c1=='\t' || c1=='\n' || c1=='\v' || c1=='\f' || c1=='\r'
        ) {
            ++name1;
        }
        while((c2=(unsigned char)*name2)=='-' || c2=='_' ||
              c2==' ' || c2=='\t' || c2=='\n' || c2=='\v' || c2=='\f' || c2=='\r'
        ) {
            ++name2;
        }

        /* If we reach the ends of both strings then they match */
        if((c1|c2)==0) {
            return 0;
        }
        
        /* Case-insensitive comparison */
        if(c1!=c2) {
            rc=(int32_t)(unsigned char)uprv_tolower(c1)-(int32_t)(unsigned char)uprv_tolower(c2);
            if(rc!=0) {
                return rc;
            }
        }

        ++name1;
        ++name2;
    }
}

/* API functions ------------------------------------------------------------ */

U_CAPI void U_EXPORT2
u_charAge(UChar32 c, UVersionInfo versionArray) {
    if(versionArray!=NULL) {
        uint32_t version=u_getUnicodeProperties(c, 0)>>UPROPS_AGE_SHIFT;
        versionArray[0]=(uint8_t)(version>>4);
        versionArray[1]=(uint8_t)(version&0xf);
        versionArray[2]=versionArray[3]=0;
    }
}

U_CAPI UScriptCode U_EXPORT2
uscript_getScript(UChar32 c, UErrorCode *pErrorCode) {
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if((uint32_t)c>0x10ffff) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    return (UScriptCode)(u_getUnicodeProperties(c, 0)&UPROPS_SCRIPT_MASK);
}

U_CAPI UBlockCode U_EXPORT2
ublock_getCode(UChar32 c) {
    return (UBlockCode)((u_getUnicodeProperties(c, 0)&UPROPS_BLOCK_MASK)>>UPROPS_BLOCK_SHIFT);
}

static const struct {
    int32_t column;
    uint32_t mask;
} binProps[]={
    /*
     * column and mask values for binary properties from u_getUnicodeProperties().
     * Must be in order of corresponding UProperty,
     * and there must be exacly one entry per binary UProperty.
     */
    {  1, U_MASK(UPROPS_ALPHABETIC) },
    {  1, U_MASK(UPROPS_ASCII_HEX_DIGIT) },
    {  1, U_MASK(UPROPS_BIDI_CONTROL) },
    { -1, U_MASK(UPROPS_MIRROR_SHIFT) },
    {  1, U_MASK(UPROPS_DASH) },
    {  1, U_MASK(UPROPS_DEFAULT_IGNORABLE_CODE_POINT) },
    {  1, U_MASK(UPROPS_DEPRECATED) },
    {  1, U_MASK(UPROPS_DIACRITIC) },
    {  1, U_MASK(UPROPS_EXTENDER) },
    {  0, 0 },                                  /* UCHAR_FULL_COMPOSITION_EXCLUSION */
    {  1, U_MASK(UPROPS_GRAPHEME_BASE) },
    {  1, U_MASK(UPROPS_GRAPHEME_EXTEND) },
    {  1, U_MASK(UPROPS_GRAPHEME_LINK) },
    {  1, U_MASK(UPROPS_HEX_DIGIT) },
    {  1, U_MASK(UPROPS_HYPHEN) },
    {  1, U_MASK(UPROPS_ID_CONTINUE) },
    {  1, U_MASK(UPROPS_ID_START) },
    {  1, U_MASK(UPROPS_IDEOGRAPHIC) },
    {  1, U_MASK(UPROPS_IDS_BINARY_OPERATOR) },
    {  1, U_MASK(UPROPS_IDS_TRINARY_OPERATOR) },
    {  1, U_MASK(UPROPS_JOIN_CONTROL) },
    {  1, U_MASK(UPROPS_LOGICAL_ORDER_EXCEPTION) },
    {  1, U_MASK(UPROPS_LOWERCASE) },
    {  1, U_MASK(UPROPS_MATH) },
    {  1, U_MASK(UPROPS_NONCHARACTER_CODE_POINT) },
    {  1, U_MASK(UPROPS_QUOTATION_MARK) },
    {  1, U_MASK(UPROPS_RADICAL) },
    {  1, U_MASK(UPROPS_SOFT_DOTTED) },
    {  1, U_MASK(UPROPS_TERMINAL_PUNCTUATION) },
    {  1, U_MASK(UPROPS_UNIFIED_IDEOGRAPH) },
    {  1, U_MASK(UPROPS_UPPERCASE) },
    {  1, U_MASK(UPROPS_WHITE_SPACE) },
    {  1, U_MASK(UPROPS_XID_CONTINUE) },
    {  1, U_MASK(UPROPS_XID_START) },
    { -1, U_MASK(UPROPS_CASE_SENSITIVE_SHIFT) }
};

U_CAPI UBool U_EXPORT2
u_hasBinaryProperty(UChar32 c, UProperty which) {
    /* c is range-checked in the functions that are called from here */
    if(which<UCHAR_BINARY_START || UCHAR_BINARY_LIMIT<=which) {
        /* not a known binary property */
        return FALSE;
    } else if(which==UCHAR_FULL_COMPOSITION_EXCLUSION) {
#if !UCONFIG_NO_NORMALIZATION
        return unorm_internalIsFullCompositionExclusion(c);
#else
        return FALSE;
#endif
    } else {
        /* systematic, directly stored properties */
        return (u_getUnicodeProperties(c, binProps[which].column)&binProps[which].mask)!=0;
    }
}

U_CAPI UBool U_EXPORT2
u_isUAlphabetic(UChar32 c) {
    return u_hasBinaryProperty(c, UCHAR_ALPHABETIC);
}

U_CAPI UBool U_EXPORT2
u_isULowercase(UChar32 c) {
    return u_hasBinaryProperty(c, UCHAR_LOWERCASE);
}

U_CAPI UBool U_EXPORT2
u_isUUppercase(UChar32 c) {
    return u_hasBinaryProperty(c, UCHAR_UPPERCASE);
}

U_CAPI UBool U_EXPORT2
u_isUWhiteSpace(UChar32 c) {
    return u_hasBinaryProperty(c, UCHAR_WHITE_SPACE);
}

U_CAPI UBool U_EXPORT2
uprv_isRuleWhiteSpace(UChar32 c) {
    /* "white space" in the sense of ICU rule parsers: Cf+White_Space */
    return
        u_charType(c)==U_FORMAT_CHAR ||
        u_hasBinaryProperty(c, UCHAR_WHITE_SPACE);
}

static const UChar _PATTERN[] = {
    /* "[[:Cf:][:WSpace:]]" */
    91, 91, 58, 67, 102, 58, 93, 91, 58, 87,
    83, 112, 97, 99, 101, 58, 93, 93, 0
};

U_CAPI USet* U_EXPORT2
uprv_openRuleWhiteSpaceSet(UErrorCode* ec) {
    return uset_openPattern(_PATTERN,
                            sizeof(_PATTERN)/sizeof(_PATTERN[0])-1, ec);
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
            /* purely algorithmic; hardcode known characters, check for assigned new ones */
            if(c<JAMO_L_BASE) {
                /* U_HST_NOT_APPLICABLE */
            } else if(c<=0x11ff) {
                /* Jamo range */
                if(c<=0x115f) {
                    /* Jamo L range, HANGUL CHOSEONG ... */
                    if(c==0x115f || c<=0x1159 || u_charType(c)==U_OTHER_LETTER) {
                        return U_HST_LEADING_JAMO;
                    }
                } else if(c<=0x11a7) {
                    /* Jamo V range, HANGUL JUNGSEONG ... */
                    if(c<=0x11a2 || u_charType(c)==U_OTHER_LETTER) {
                        return U_HST_VOWEL_JAMO;
                    }
                } else {
                    /* Jamo T range */
                    if(c<=0x11f9 || u_charType(c)==U_OTHER_LETTER) {
                        return U_HST_TRAILING_JAMO;
                    }
                }
            } else if((c-=HANGUL_BASE)<0) {
                /* U_HST_NOT_APPLICABLE */
            } else if(c<HANGUL_COUNT) {
                /* Hangul syllable */
                return c%JAMO_T_COUNT==0 ? U_HST_LV_SYLLABLE : U_HST_LVT_SYLLABLE;
            }
            return U_HST_NOT_APPLICABLE;
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
        default:
            return -1; /* undefined */
        }
    } else {
        return -1; /* undefined */
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
 */
U_CAPI void U_EXPORT2
uprv_getInclusions(USet* set, UErrorCode *pErrorCode) {
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    uset_clear(set);

#if !UCONFIG_NO_NORMALIZATION
    unorm_addPropertyStarts(set, pErrorCode);
#endif
    uchar_addPropertyStarts(set, pErrorCode);
}
