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
*   Implementations for mostly non-core Unicode character properties
*   stored in uprops.icu.
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/uscript.h"
#include "cstring.h"
#include "unormimp.h"
#include "uprops.h"

/* helper definitions ------------------------------------------------------- */

#define CGJ     0x034f
#define ZWNJ    0x200C
#define ZWJ     0x200D

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
        return USCRIPT_INVALID_CODE;
    }
    if((uint32_t)c>0x10ffff) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return USCRIPT_INVALID_CODE;
    }

    return (UScriptCode)(u_getUnicodeProperties(c, 0)&UPROPS_SCRIPT_MASK);
}

U_CAPI UBlockCode U_EXPORT2
ublock_getCode(UChar32 c) {
    uint32_t b;

    if((uint32_t)c>0x10ffff) {
        return UBLOCK_INVALID_CODE;
    }

    b=(u_getUnicodeProperties(c, 0)&UPROPS_BLOCK_MASK)>>UPROPS_BLOCK_SHIFT;
    if(b==0) {
        return UBLOCK_INVALID_CODE;
    } else {
        return (UBlockCode)b;
    }
}

U_CAPI UBool U_EXPORT2
u_hasBinaryProperty(UChar32 c, UProperty which) {
    uint32_t props;

    /* c is range-checked in the functions that are called from here */
    switch(which) {
    case UCHAR_ALPHABETIC:
        /* Lu+Ll+Lt+Lm+Lo+Nl+Other_Alphabetic */
        return (FLAG(u_charType(c))&(_Lu|_Ll|_Lt|_Lm|_Lo|_Nl))!=0 ||
                (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_OTHER_ALPHABETIC))!=0;
    case UCHAR_ASCII_HEX_DIGIT:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_ASCII_HEX_DIGIT))!=0;
    case UCHAR_BIDI_CONTROL:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_BIDI_CONTROL))!=0;
    case UCHAR_BIDI_MIRRORED:
        return u_isMirrored(c);
    case UCHAR_DASH:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_DASH))!=0;
    case UCHAR_DEFAULT_IGNORABLE_CODE_POINT:
        /* <2060..206F, FFF0..FFFB, E0000..E0FFF>+Other_Default_Ignorable_Code_Point+(Cf+Cc+Cs-White_Space) */
        if( (0x2060<=c && c<=0x206f) ||
            (0xfff0<=c && c<=0xfffb) ||
            (0xe0000<=c && c<=0xe0fff)
        ) {
            return TRUE;
        }

        props=u_getUnicodeProperties(c, 1);
        return (props&FLAG(UPROPS_OTHER_DEFAULT_IGNORABLE_CODE_POINT))!=0 ||
                    ((props&FLAG(UPROPS_WHITE_SPACE))==0 &&
                    (FLAG(u_charType(c))&(_Cf|_Cc|_Cs))!=0);
    case UCHAR_DEPRECATED:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_DEPRECATED))!=0;
    case UCHAR_DIACRITIC:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_DIACRITIC))!=0;
    case UCHAR_EXTENDER:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_EXTENDER))!=0;
    case UCHAR_FULL_COMPOSITION_EXCLUSION:
        return unorm_internalIsFullCompositionExclusion(c);
    case UCHAR_GRAPHEME_BASE:
        /*
         * [0..10FFFF]-Cc-Cf-Cs-Co-Cn-Zl-Zp-Grapheme_Link-Grapheme_Extend-CGJ ==
         * [0..10FFFF]-Cc-Cf-Cs-Co-Cn-Zl-Zp-Grapheme_Link-(Me+Mn+Mc+Other_Grapheme_Extend)-CGJ ==
         * [0..10FFFF]-Cc-Cf-Cs-Co-Cn-Zl-Zp-Me-Mn-Mc-Grapheme_Link-Other_Grapheme_Extend-CGJ
         *
         * u_charType(c out of range) returns Cn so we need not check for the range
         */
        return c!=CGJ &&
                (FLAG(u_charType(c))&(_Cc|_Cf|_Cs|_Co|_Cn|_Zl|_Zp|_Me|_Mn|_Mc))==0 &&
                ((u_getUnicodeProperties(c, 1)&
                    (FLAG(UPROPS_GRAPHEME_LINK)|FLAG(UPROPS_OTHER_GRAPHEME_EXTEND)))==0);
    case UCHAR_GRAPHEME_EXTEND:
        /* Me+Mn+Mc+Other_Grapheme_Extend-Grapheme_Link-CGJ */
        if(c==CGJ) {
            return FALSE; /* fastest check first */
        }

        props=u_getUnicodeProperties(c, 1);
        return (props&FLAG(UPROPS_GRAPHEME_LINK))==0 &&
                    ((props&FLAG(UPROPS_OTHER_GRAPHEME_EXTEND))!=0 ||
                    (FLAG(u_charType(c))&(_Me|_Mn|_Mc))!=0);
    case UCHAR_GRAPHEME_LINK:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_GRAPHEME_LINK))!=0;
    case UCHAR_HEX_DIGIT:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_HEX_DIGIT))!=0;
    case UCHAR_HYPHEN:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_HYPHEN))!=0;
    case UCHAR_ID_CONTINUE:
        /* ID_Start+Mn+Mc+Nd+Pc == Lu+Ll+Lt+Lm+Lo+Nl+Mn+Mc+Nd+Pc */
        return (FLAG(u_charType(c))&(_Lu|_Ll|_Lt|_Lm|_Lo|_Nl|_Mn|_Mc|_Nd|_Pc))!=0;
    case UCHAR_ID_START:
        /* Lu+Ll+Lt+Lm+Lo+Nl */
        return (FLAG(u_charType(c))&(_Lu|_Ll|_Lt|_Lm|_Lo|_Nl))!=0;
    case UCHAR_IDEOGRAPHIC:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_IDEOGRAPHIC))!=0;
    case UCHAR_IDS_BINARY_OPERATOR:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_IDS_BINARY_OPERATOR))!=0;
    case UCHAR_IDS_TRINARY_OPERATOR:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_IDS_TRINARY_OPERATOR))!=0;
    case UCHAR_JOIN_CONTROL:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_JOIN_CONTROL))!=0;
    case UCHAR_LOGICAL_ORDER_EXCEPTION:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_LOGICAL_ORDER_EXCEPTION))!=0;
    case UCHAR_LOWERCASE:
        /* Ll+Other_Lowercase */
        return u_charType(c)==U_LOWERCASE_LETTER ||
                (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_OTHER_LOWERCASE))!=0;
    case UCHAR_MATH:
        /* Sm+Other_Math */
        return u_charType(c)==U_MATH_SYMBOL ||
                (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_OTHER_MATH))!=0;
    case UCHAR_NONCHARACTER_CODE_POINT:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_NONCHARACTER_CODE_POINT))!=0;
    case UCHAR_QUOTATION_MARK:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_QUOTATION_MARK))!=0;
    case UCHAR_RADICAL:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_RADICAL))!=0;
    case UCHAR_SOFT_DOTTED:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_SOFT_DOTTED))!=0;
    case UCHAR_TERMINAL_PUNCTUATION:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_TERMINAL_PUNCTUATION))!=0;
    case UCHAR_UNIFIED_IDEOGRAPH:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_UNIFIED_IDEOGRAPH))!=0;
    case UCHAR_UPPERCASE:
        /* Lu+Other_Uppercase */
        return u_charType(c)==U_UPPERCASE_LETTER ||
                (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_OTHER_UPPERCASE))!=0;
    case UCHAR_WHITE_SPACE:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_WHITE_SPACE))!=0;
    case UCHAR_XID_CONTINUE:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_XID_CONTINUE))!=0;
    case UCHAR_XID_START:
        return (u_getUnicodeProperties(c, 1)&FLAG(UPROPS_XID_START))!=0;
    default:
        /* not a known binary property */
        return FALSE;
    };
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

U_CAPI int32_t U_EXPORT2
u_getIntPropertyValue(UChar32 c, UProperty which) {
    UErrorCode errorCode;
    int32_t i;
    int8_t type;

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
            return u_getCombiningClass(c);
        case UCHAR_DECOMPOSITION_TYPE:
            return (int32_t)(u_getUnicodeProperties(c, 2)&UPROPS_DT_MASK);
        case UCHAR_EAST_ASIAN_WIDTH:
            return (int32_t)(u_getUnicodeProperties(c, 0)&UPROPS_EA_MASK)>>UPROPS_EA_SHIFT;
        case UCHAR_GENERAL_CATEGORY:
            return (int32_t)u_charType(c);
        case UCHAR_JOINING_GROUP:
            return (int32_t)(u_getUnicodeProperties(c, 2)&UPROPS_JG_MASK)>>UPROPS_JG_SHIFT;
        case UCHAR_JOINING_TYPE:
            /*
             * ArabicShaping.txt:
             * Note: Characters of joining type T and most characters of 
             * joining type U are not explicitly listed in this file.
             *
             * Characters of joining type T can [be] derived by the following formula:
             *   T = Mn + Cf - ZWNJ - ZWJ
             */
            i=(int32_t)(u_getUnicodeProperties(c, 2)&UPROPS_JT_MASK)>>UPROPS_JT_SHIFT;
            if(i==0 && c!=ZWNJ && c!=ZWJ && (FLAG(u_charType(c))&(_Mn|_Cf))!=0) {
                i=(int32_t)U_JT_TRANSPARENT;
            }
            return i;
        case UCHAR_LINE_BREAK:
            /*
             * LineBreak.txt:
             *  - Assigned characters that are not listed explicitly are given the value
             *    "AL".
             *  - Unassigned characters are given the value "XX".
             * ...
             * E000..F8FF;XX # <Private Use, First>..<Private Use, Last>
             * F0000..FFFFD;XX # <Plane 15 Private Use, First>..<Plane 15 Private Use, Last>
             * 100000..10FFFD;XX # <Plane 16 Private Use, First>..<Plane 16 Private Use, Last>
             */
            i=(int32_t)(u_getUnicodeProperties(c, 0)&UPROPS_LB_MASK)>>UPROPS_LB_SHIFT;
            if(i==0 && (type=u_charType(c))!=0 && type!=(int8_t)U_PRIVATE_USE_CHAR) {
                i=(int32_t)U_LB_ALPHABETIC;
            }
            return i;
        case UCHAR_NUMERIC_TYPE:
            return (int32_t)GET_NUMERIC_TYPE(u_getUnicodeProperties(c, -1));
        case UCHAR_SCRIPT:
            errorCode=U_ZERO_ERROR;
            return (int32_t)uscript_getScript(c, &errorCode);
        default:
            return 0; /* undefined */
        }
    } else {
        return 0; /* undefined */
    }
}

U_CAPI int32_t U_EXPORT2
u_getIntPropertyMinValue(UProperty which) {
    switch(which) {
    case UCHAR_BLOCK:
        return UBLOCK_INVALID_CODE;
    case UCHAR_SCRIPT:
        return USCRIPT_INVALID_CODE;
    default:
        return 0; /* undefined; and: all other properties have a minimum value of 0 */
    }
}

U_CAPI int32_t U_EXPORT2
u_getIntPropertyMaxValue(UProperty which) {
    int32_t max;

    if(which<UCHAR_BINARY_START) {
        return 0; /* undefined */
    } else if(which<UCHAR_BINARY_LIMIT) {
        return 1; /* maximum TRUE for all binary properties */
    } else if(which<UCHAR_INT_START) {
        return 0; /* undefined */
    } else if(which<UCHAR_INT_LIMIT) {
        switch(which) {
        case UCHAR_BIDI_CLASS:
            return (int32_t)U_CHAR_DIRECTION_COUNT-1;
        case UCHAR_BLOCK:
            max=(uprv_getMaxValues()&UPROPS_BLOCK_MASK)>>UPROPS_BLOCK_SHIFT;
            if(max==0) {
                max=(int32_t)UBLOCK_COUNT-1;
            }
            return max;
        case UCHAR_CANONICAL_COMBINING_CLASS:
            return 0xff; /* TODO do we need to be more precise, getting the actual maximum? */
        case UCHAR_DECOMPOSITION_TYPE:
            return (int32_t)U_DT_COUNT-1;
        case UCHAR_EAST_ASIAN_WIDTH:
            return (int32_t)U_EA_COUNT-1;
        case UCHAR_GENERAL_CATEGORY:
            return (int32_t)U_CHAR_CATEGORY_COUNT-1;
        case UCHAR_JOINING_GROUP:
            return (int32_t)U_JG_COUNT-1;
        case UCHAR_JOINING_TYPE:
            return (int32_t)U_JT_COUNT-1;
        case UCHAR_LINE_BREAK:
            return (int32_t)U_LB_COUNT-1;
        case UCHAR_NUMERIC_TYPE:
            return (int32_t)U_NT_COUNT-1;
        case UCHAR_SCRIPT:
            max=uprv_getMaxValues()&UPROPS_SCRIPT_MASK;
            if(max==0) {
                max=(int32_t)USCRIPT_CODE_LIMIT-1;
            }
            return max;
        default:
            return 0; /* undefined */
        }
    } else {
        return 0; /* undefined */
    }
}
