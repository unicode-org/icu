/*
********************************************************************************
*   Copyright (C) 1996-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File UCHAR.C
*
* Modification History:
*
*   Date        Name        Description
*   04/02/97    aliu        Creation.
*   4/15/99     Madhu       Updated all the function definitions for C Implementation
*   5/20/99     Madhu       Added the function u_getVersion()
*   8/19/1999   srl         Upgraded scripts to Unicode3.0 
*   11/11/1999  weiv        added u_isalnum(), cleaned comments
*   01/11/2000  helena      Renamed u_getVersion to u_getUnicodeVersion.
*   06/20/2000  helena      OS/400 port changes; mostly typecast.
******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/udata.h"
#include "unicode/uloc.h"
#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "ustr_imp.h"

/*
 * Since genprops overrides the general category for some control codes,
 * we need to hardcode ISO 8 controls for u_iscntrl(), u_isprint(), etc.
 */
#define IS_ISO_8_CONTROL(c) ((uint32_t)(c)<0x20 || (uint32_t)((c)-0x7f)<=0x20)

/* dynamically loaded Unicode character properties -------------------------- */

/* fallback properties for the ASCII range if the data cannot be loaded */
/* these are printed by genprops in verbose mode */
static uint32_t staticProps32Table[]={
    /* 0x00 */ 0x48f,
    /* 0x01 */ 0x48f,
    /* 0x02 */ 0x48f,
    /* 0x03 */ 0x48f,
    /* 0x04 */ 0x48f,
    /* 0x05 */ 0x48f,
    /* 0x06 */ 0x48f,
    /* 0x07 */ 0x48f,
    /* 0x08 */ 0x48f,
    /* 0x09 */ 0x20c,
    /* 0x0a */ 0x1ce,
    /* 0x0b */ 0x20c,
    /* 0x0c */ 0x24d,
    /* 0x0d */ 0x1ce,
    /* 0x0e */ 0x48f,
    /* 0x0f */ 0x48f,
    /* 0x10 */ 0x48f,
    /* 0x11 */ 0x48f,
    /* 0x12 */ 0x48f,
    /* 0x13 */ 0x48f,
    /* 0x14 */ 0x48f,
    /* 0x15 */ 0x48f,
    /* 0x16 */ 0x48f,
    /* 0x17 */ 0x48f,
    /* 0x18 */ 0x48f,
    /* 0x19 */ 0x48f,
    /* 0x1a */ 0x48f,
    /* 0x1b */ 0x48f,
    /* 0x1c */ 0x1ce,
    /* 0x1d */ 0x1ce,
    /* 0x1e */ 0x1ce,
    /* 0x1f */ 0x20c,
    /* 0x20 */ 0x24c,
    /* 0x21 */ 0x297,
    /* 0x22 */ 0x297,
    /* 0x23 */ 0x117,
    /* 0x24 */ 0x119,
    /* 0x25 */ 0x117,
    /* 0x26 */ 0x297,
    /* 0x27 */ 0x297,
    /* 0x28 */ 0x100a94,
    /* 0x29 */ 0xfff00a95,
    /* 0x2a */ 0x297,
    /* 0x2b */ 0x118,
    /* 0x2c */ 0x197,
    /* 0x2d */ 0x113,
    /* 0x2e */ 0x197,
    /* 0x2f */ 0xd7,
    /* 0x30 */ 0x89,
    /* 0x31 */ 0x100089,
    /* 0x32 */ 0x200089,
    /* 0x33 */ 0x300089,
    /* 0x34 */ 0x400089,
    /* 0x35 */ 0x500089,
    /* 0x36 */ 0x600089,
    /* 0x37 */ 0x700089,
    /* 0x38 */ 0x800089,
    /* 0x39 */ 0x900089,
    /* 0x3a */ 0x197,
    /* 0x3b */ 0x297,
    /* 0x3c */ 0x200a98,
    /* 0x3d */ 0x298,
    /* 0x3e */ 0xffe00a98,
    /* 0x3f */ 0x297,
    /* 0x40 */ 0x297,
    /* 0x41 */ 0x2000001,
    /* 0x42 */ 0x2000001,
    /* 0x43 */ 0x2000001,
    /* 0x44 */ 0x2000001,
    /* 0x45 */ 0x2000001,
    /* 0x46 */ 0x2000001,
    /* 0x47 */ 0x2000001,
    /* 0x48 */ 0x2000001,
    /* 0x49 */ 0x2000001,
    /* 0x4a */ 0x2000001,
    /* 0x4b */ 0x2000001,
    /* 0x4c */ 0x2000001,
    /* 0x4d */ 0x2000001,
    /* 0x4e */ 0x2000001,
    /* 0x4f */ 0x2000001,
    /* 0x50 */ 0x2000001,
    /* 0x51 */ 0x2000001,
    /* 0x52 */ 0x2000001,
    /* 0x53 */ 0x2000001,
    /* 0x54 */ 0x2000001,
    /* 0x55 */ 0x2000001,
    /* 0x56 */ 0x2000001,
    /* 0x57 */ 0x2000001,
    /* 0x58 */ 0x2000001,
    /* 0x59 */ 0x2000001,
    /* 0x5a */ 0x2000001,
    /* 0x5b */ 0x200a94,
    /* 0x5c */ 0x297,
    /* 0x5d */ 0xffe00a95,
    /* 0x5e */ 0x29a,
    /* 0x5f */ 0x296,
    /* 0x60 */ 0x29a,
    /* 0x61 */ 0x2000002,
    /* 0x62 */ 0x2000002,
    /* 0x63 */ 0x2000002,
    /* 0x64 */ 0x2000002,
    /* 0x65 */ 0x2000002,
    /* 0x66 */ 0x2000002,
    /* 0x67 */ 0x2000002,
    /* 0x68 */ 0x2000002,
    /* 0x69 */ 0x2000002,
    /* 0x6a */ 0x2000002,
    /* 0x6b */ 0x2000002,
    /* 0x6c */ 0x2000002,
    /* 0x6d */ 0x2000002,
    /* 0x6e */ 0x2000002,
    /* 0x6f */ 0x2000002,
    /* 0x70 */ 0x2000002,
    /* 0x71 */ 0x2000002,
    /* 0x72 */ 0x2000002,
    /* 0x73 */ 0x2000002,
    /* 0x74 */ 0x2000002,
    /* 0x75 */ 0x2000002,
    /* 0x76 */ 0x2000002,
    /* 0x77 */ 0x2000002,
    /* 0x78 */ 0x2000002,
    /* 0x79 */ 0x2000002,
    /* 0x7a */ 0x2000002,
    /* 0x7b */ 0x200a94,
    /* 0x7c */ 0x298,
    /* 0x7d */ 0xffe00a95,
    /* 0x7e */ 0x298,
    /* 0x7f */ 0x48f,
    /* 0x80 */ 0x48f,
    /* 0x81 */ 0x48f,
    /* 0x82 */ 0x48f,
    /* 0x83 */ 0x48f,
    /* 0x84 */ 0x48f,
    /* 0x85 */ 0x1ce,
    /* 0x86 */ 0x48f,
    /* 0x87 */ 0x48f,
    /* 0x88 */ 0x48f,
    /* 0x89 */ 0x48f,
    /* 0x8a */ 0x48f,
    /* 0x8b */ 0x48f,
    /* 0x8c */ 0x48f,
    /* 0x8d */ 0x48f,
    /* 0x8e */ 0x48f,
    /* 0x8f */ 0x48f,
    /* 0x90 */ 0x48f,
    /* 0x91 */ 0x48f,
    /* 0x92 */ 0x48f,
    /* 0x93 */ 0x48f,
    /* 0x94 */ 0x48f,
    /* 0x95 */ 0x48f,
    /* 0x96 */ 0x48f,
    /* 0x97 */ 0x48f,
    /* 0x98 */ 0x48f,
    /* 0x99 */ 0x48f,
    /* 0x9a */ 0x48f,
    /* 0x9b */ 0x48f,
    /* 0x9c */ 0x48f,
    /* 0x9d */ 0x48f,
    /* 0x9e */ 0x48f,
    /* 0x9f */ 0x48f
};

/*
 * loaded uprops.dat -
 * for a description of the file format, see icu/source/tools/genprops/store.c
 */
#define DATA_NAME "uprops"
#define DATA_TYPE "dat"

static UDataMemory *propsData=NULL;

static uint8_t formatVersion[4]={ 0, 0, 0, 0 };
static UVersionInfo dataVersion={ 3, 0, 0, 0 };

static const uint16_t *propsTable=NULL;
#define props32Table ((uint32_t *)propsTable)

static const UChar *ucharsTable=NULL;

static int8_t havePropsData=0;

/* index values loaded from uprops.dat */
static uint16_t indexes[8];

enum {
    INDEX_STAGE_2_BITS,
    INDEX_STAGE_3_BITS,
    INDEX_EXCEPTIONS,
    INDEX_STAGE_3_INDEX,
    INDEX_PROPS,
    INDEX_UCHARS
};

#ifdef UCHAR_VARIABLE_TRIE_BITS
    /* access values calculated from indexes */
    static uint16_t stage23Bits, stage2Mask, stage3Mask;
#   define stage3Bits   indexes[INDEX_STAGE_3_BITS]
#else
    /* We are now hardcoding the bit distribution for the trie table access. */
#   define stage23Bits  10
#   define stage2Mask   0x3f
#   define stage3Mask   0xf
#   define stage3Bits   4
#endif

static UBool
isAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    if(
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x55 &&   /* dataFormat="UPro" */
        pInfo->dataFormat[1]==0x50 &&
        pInfo->dataFormat[2]==0x72 &&
        pInfo->dataFormat[3]==0x6f &&
        pInfo->formatVersion[0]==1
    ) {
        uprv_memcpy(formatVersion, pInfo->formatVersion, 4);
        uprv_memcpy(dataVersion, pInfo->dataVersion, 4);
        return TRUE;
    } else {
        return FALSE;
    }
}

static int8_t
loadPropsData() {
    /* load Unicode character properties data from file if necessary */
    if(havePropsData==0) {
        UErrorCode errorCode=U_ZERO_ERROR;
        UDataMemory *data;
        const uint16_t *p=NULL;

        /* open the data outside the mutex block */
        data=udata_openChoice(NULL, DATA_TYPE, DATA_NAME, isAcceptable, NULL, &errorCode);
        if(U_FAILURE(errorCode)) {
            return havePropsData=-1;
        }

        p=(const uint16_t *)udata_getMemory(data);

#ifndef UCHAR_VARIABLE_TRIE_BITS
        /*
         * We are now hardcoding the bit distribution for the trie table access.
         * Check that the file is stored accordingly.
         */
        if(p[INDEX_STAGE_2_BITS]!=6 || p[INDEX_STAGE_3_BITS]!=4) {
            udata_close(data);
            errorCode=U_INVALID_FORMAT_ERROR;
            return havePropsData=-1;
        }
#endif

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if(propsData==NULL) {
            propsData=data;
            data=NULL;
            propsTable=p;
            p=NULL;
        }
        umtx_unlock(NULL);

        /* initialize some variables */
        uprv_memcpy(indexes, propsTable, 16);
#ifdef UCHAR_VARIABLE_TRIE_BITS
        stage23Bits=(uint16_t)(indexes[INDEX_STAGE_2_BITS]+indexes[INDEX_STAGE_3_BITS]);
        stage2Mask=(uint16_t)((1<<indexes[INDEX_STAGE_2_BITS])-1);
        stage3Mask=(uint16_t)((1<<indexes[INDEX_STAGE_3_BITS])-1);
#endif
        ucharsTable=(const UChar *)(props32Table+indexes[INDEX_UCHARS]);
        havePropsData=1;

        /* if a different thread set it first, then close the extra data */
        if(data!=NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return havePropsData;
}

/* constants and macros for access to the data */
enum {
    EXC_UPPERCASE,
    EXC_LOWERCASE,
    EXC_TITLECASE,
    EXC_DIGIT_VALUE,
    EXC_NUMERIC_VALUE,
    EXC_DENOMINATOR_VALUE,
    EXC_MIRROR_MAPPING,
    EXC_SPECIAL_CASING,
    EXC_CASE_FOLDING
};

enum {
    EXCEPTION_SHIFT=5,
    BIDI_SHIFT,
    MIRROR_SHIFT=BIDI_SHIFT+5,
    VALUE_SHIFT=20,

    VALUE_BITS=32-VALUE_SHIFT
};

/* getting a uint32_t properties word from the data */
#define HAVE_DATA (havePropsData>0 || (havePropsData==0 && loadPropsData()>0))
#define VALIDATE(c) (((uint32_t)(c))<=0x10ffff && HAVE_DATA)
#define GET_PROPS_UNSAFE(c) \
    props32Table[ \
        propsTable[ \
            propsTable[ \
                propsTable[8+((c)>>stage23Bits)]+ \
                (((c)>>stage3Bits)&stage2Mask)]+ \
            ((c)&stage3Mask) \
        ] \
    ]
#define GET_PROPS(c) \
    (((uint32_t)(c))<=0x10ffff ? \
        HAVE_DATA ? \
            GET_PROPS_UNSAFE(c) \
        : (c)<=0x9f ? \
            staticProps32Table[c] \
        : 0 \
    : 0)
#define PROPS_VALUE_IS_EXCEPTION(props) ((props)&(1UL<<EXCEPTION_SHIFT))
#define GET_CATEGORY(props) ((props)&0x1f)
#define GET_UNSIGNED_VALUE(props) ((props)>>VALUE_SHIFT)
#define GET_SIGNED_VALUE(props) ((int32_t)(props)>>VALUE_SHIFT)
#define GET_EXCEPTIONS(props) (props32Table+indexes[INDEX_EXCEPTIONS]+GET_UNSIGNED_VALUE(props))

/* finding an exception value */
#define HAVE_EXCEPTION_VALUE(flags, index) ((flags)&(1UL<<(index)))

/* number of bits in an 8-bit integer value */
#define EXC_GROUP 8
static uint8_t flagsOffset[256]={
    0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
    1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
    1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
    1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
    2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
    3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
    3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
    4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8
};

#define ADD_EXCEPTION_OFFSET(flags, index, offset) { \
    if((index)>=EXC_GROUP) { \
        (offset)+=flagsOffset[(flags)&((1<<EXC_GROUP)-1)]; \
        (flags)>>=EXC_GROUP; \
        (index)-=EXC_GROUP; \
    } \
    (offset)+=flagsOffset[(flags)&((1<<(index))-1)]; \
}

U_CFUNC UBool
uprv_haveProperties() {
    return HAVE_DATA;
}

/* API functions ------------------------------------------------------------ */

/* Gets the Unicode character's general category.*/
U_CAPI int8_t U_EXPORT2
u_charType(UChar32 c) {
    return (int8_t)GET_CATEGORY(GET_PROPS(c));
}

/* Checks if ch is a lower case letter.*/
U_CAPI UBool U_EXPORT2
u_islower(UChar32 c) {
    return (UBool)(GET_CATEGORY(GET_PROPS(c))==U_LOWERCASE_LETTER);
}

/* Checks if ch is an upper case letter.*/
U_CAPI UBool U_EXPORT2
u_isupper(UChar32 c) {
    return (UBool)(GET_CATEGORY(GET_PROPS(c))==U_UPPERCASE_LETTER);
}

/* Checks if ch is a title case letter; usually upper case letters.*/
U_CAPI UBool U_EXPORT2
u_istitle(UChar32 c) {
    return (UBool)(GET_CATEGORY(GET_PROPS(c))==U_TITLECASE_LETTER);
}

/* Checks if ch is a decimal digit. */
U_CAPI UBool U_EXPORT2
u_isdigit(UChar32 c) {
    return (UBool)(((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_OTHER_NUMBER|1UL<<U_LETTER_NUMBER)
           )!=0);
}

/* Checks if the Unicode character is a letter.*/
U_CAPI UBool U_EXPORT2
u_isalpha(UChar32 c) {
    return (UBool)(((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER)
           )!=0);
}

/* Checks if ch is a letter or a decimal digit */
U_CAPI UBool U_EXPORT2
u_isalnum(UChar32 c) {
    return (UBool)(((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_OTHER_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER)
           )!=0);
}

/* Checks if ch is a unicode character with assigned character type.*/
U_CAPI UBool U_EXPORT2
u_isdefined(UChar32 c) {
    return (UBool)(GET_PROPS(c)!=0);
}

/* Checks if the Unicode character is a base form character that can take a diacritic.*/
U_CAPI UBool U_EXPORT2
u_isbase(UChar32 c) {
    return (UBool)(((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_OTHER_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_NON_SPACING_MARK|1UL<<U_ENCLOSING_MARK|1UL<<U_COMBINING_SPACING_MARK)
           )!=0);
}

/* Checks if the Unicode character is a control character.*/
U_CAPI UBool U_EXPORT2
u_iscntrl(UChar32 c) {
    return (UBool)(
           IS_ISO_8_CONTROL(c) ||
           ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_CONTROL_CHAR|1UL<<U_FORMAT_CHAR|1UL<<U_LINE_SEPARATOR|1UL<<U_PARAGRAPH_SEPARATOR)
           )!=0);
}

/* Checks if the Unicode character is a space character.*/
UBool
u_isspace(UChar32 c) {
    return (UBool)(((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_SPACE_SEPARATOR|1UL<<U_LINE_SEPARATOR|1UL<<U_PARAGRAPH_SEPARATOR)
           )!=0);
}

/* Checks if the Unicode character is a whitespace character.*/
U_CAPI UBool U_EXPORT2
u_isWhitespace(UChar32 c) {
    return (UBool)(((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_SPACE_SEPARATOR|1UL<<U_LINE_SEPARATOR|1UL<<U_PARAGRAPH_SEPARATOR)
           )!=0 &&
           c!=0xa0 && c!=0x202f && c!=0xfeff); /* exclude no-break spaces */
}

/* Checks if the Unicode character is printable.*/
U_CAPI UBool U_EXPORT2
u_isprint(UChar32 c) {
    return (UBool)(
            !IS_ISO_8_CONTROL(c) &&
            ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            ~(1UL<<U_UNASSIGNED|
              1UL<<U_CONTROL_CHAR|1UL<<U_FORMAT_CHAR|1UL<<U_PRIVATE_USE_CHAR|1UL<<U_SURROGATE|
              1UL<<U_GENERAL_OTHER_TYPES|1UL<<31)
           )!=0);
}

/* Checks if the Unicode character can start a Unicode identifier.*/
U_CAPI UBool U_EXPORT2
u_isIDStart(UChar32 c) {
    /* same as u_isalpha() */
    return (UBool)(((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER)
           )!=0);
}

/* Checks if the Unicode character can be a Unicode identifier part other than starting the
 identifier.*/
U_CAPI UBool U_EXPORT2
u_isIDPart(UChar32 c) {
    return (UBool)(
           ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_CONNECTOR_PUNCTUATION|1UL<<U_COMBINING_SPACING_MARK|1UL<<U_NON_SPACING_MARK)
           )!=0 ||
           u_isIDIgnorable(c));
}

/*Checks if the Unicode character can be ignorable in a Java or Unicode identifier.*/
U_CAPI UBool U_EXPORT2
u_isIDIgnorable(UChar32 c) {
    return (UBool)((uint32_t)c<=8 ||
           (uint32_t)(c-0xe)<=(0x1b-0xe) ||
           (uint32_t)(c-0x7f)<=(0x9f-0x7f) ||
           (uint32_t)(c-0x200a)<=(0x200f-0x200a) ||
           (uint32_t)(c-0x206a)<=(0x206f-0x206a) ||
           c==0xfeff);
}

/*Checks if the Unicode character can start a Java identifier.*/
U_CAPI UBool U_EXPORT2
u_isJavaIDStart(UChar32 c) {
    return (UBool)(
           ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_CURRENCY_SYMBOL|1UL<<U_CONNECTOR_PUNCTUATION)
           )!=0);
}

/*Checks if the Unicode character can be a Java identifier part other than starting the
 * identifier.
 */
U_CAPI UBool U_EXPORT2
u_isJavaIDPart(UChar32 c) {
    return (UBool)(
           ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_CURRENCY_SYMBOL|1UL<<U_CONNECTOR_PUNCTUATION|
             1UL<<U_COMBINING_SPACING_MARK|1UL<<U_NON_SPACING_MARK)
           )!=0 ||
           u_isIDIgnorable(c));
}

/* Transforms the Unicode character to its lower case equivalent.*/
U_CAPI UChar32 U_EXPORT2
u_tolower(UChar32 c) {
    uint32_t props=GET_PROPS(c);
    if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        if((1UL<<GET_CATEGORY(props))&(1UL<<U_UPPERCASE_LETTER|1UL<<U_TITLECASE_LETTER)) {
            return c+GET_SIGNED_VALUE(props);
        }
    } else {
        uint32_t *pe=GET_EXCEPTIONS(props);
        uint32_t firstExceptionValue=*pe;
        if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_LOWERCASE)) {
            int i=EXC_LOWERCASE;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            return (UChar32)*pe;
        }
    }
    return c; /* no mapping - return c itself */
}
    
/* Transforms the Unicode character to its upper case equivalent.*/
U_CAPI UChar32 U_EXPORT2
u_toupper(UChar32 c) {
    uint32_t props=GET_PROPS(c);
    if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        if(GET_CATEGORY(props)==U_LOWERCASE_LETTER) {
            return c-GET_SIGNED_VALUE(props);
        }
    } else {
        uint32_t *pe=GET_EXCEPTIONS(props);
        uint32_t firstExceptionValue=*pe;
        if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_UPPERCASE)) {
            int i=EXC_UPPERCASE;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            return (UChar32)*pe;
        }
    }
    return c; /* no mapping - return c itself */
}

/* Transforms the Unicode character to its title case equivalent.*/
U_CAPI UChar32 U_EXPORT2
u_totitle(UChar32 c) {
    uint32_t props=GET_PROPS(c);
    if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        if(GET_CATEGORY(props)==U_LOWERCASE_LETTER) {
            /* here, titlecase is same as uppercase */
            return c-GET_SIGNED_VALUE(props);
        }
    } else {
        uint32_t *pe=GET_EXCEPTIONS(props);
        uint32_t firstExceptionValue=*pe;
        if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_TITLECASE)) {
            int i=EXC_TITLECASE;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            return (UChar32)*pe;
        } else if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_UPPERCASE)) {
            /* here, titlecase is same as uppercase */
            int i=EXC_UPPERCASE;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            return (UChar32)*pe;
        }
    }
    return c; /* no mapping - return c itself */
}

U_CAPI int32_t U_EXPORT2
u_charDigitValue(UChar32 c) {
    uint32_t props=GET_PROPS(c);
    if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        if(GET_CATEGORY(props)==U_DECIMAL_DIGIT_NUMBER) {
            return GET_SIGNED_VALUE(props);
        }
    } else {
        uint32_t *pe=GET_EXCEPTIONS(props);
        uint32_t firstExceptionValue=*pe;
        if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_DIGIT_VALUE)) {
            int32_t value;
            int i=EXC_DIGIT_VALUE;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            value=(int32_t)(int16_t)*pe; /* the digit value is in bits 15..0 */
            if(value!=-1) {
                return value;
            }
        }
    }

    /* if there is no value in the properties table, then check for some special characters */
    switch(c) {
    case 0x3007:    return 0; /* Han Zero*/
    case 0x4e00:    return 1; /* Han One*/
    case 0x4e8c:    return 2; /* Han Two*/
    case 0x4e09:    return 3; /* Han Three*/
    case 0x56d8:    return 4; /* Han Four*/
    case 0x4e94:    return 5; /* Han Five*/
    case 0x516d:    return 6; /* Han Six*/
    case 0x4e03:    return 7; /* Han Seven*/
    case 0x516b:    return 8; /* Han Eight*/
    case 0x4e5d:    return 9; /* Han Nine*/
    default:        return -1; /* no value */
    }
}

/* Gets the character's linguistic directionality.*/
U_CAPI UCharDirection U_EXPORT2
u_charDirection(UChar32 c) {   
    uint32_t props=GET_PROPS(c);
    if(props!=0) {
        return (UCharDirection)((props>>BIDI_SHIFT)&0x1f);
    } else {
        return U_BOUNDARY_NEUTRAL;
    }
}

U_CAPI UBool U_EXPORT2
u_isMirrored(UChar32 c) {
    return (UBool)(GET_PROPS(c)&(1UL<<MIRROR_SHIFT) ? TRUE : FALSE);
}

U_CAPI UChar32 U_EXPORT2
u_charMirror(UChar32 c) {
    uint32_t props=GET_PROPS(c);
    if((props&(1UL<<MIRROR_SHIFT))==0) {
        /* not mirrored - the value is not a mirror offset */
        return c;
    } else if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        return c+GET_SIGNED_VALUE(props);
    } else {
        uint32_t *pe=GET_EXCEPTIONS(props);
        uint32_t firstExceptionValue=*pe;
        if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_MIRROR_MAPPING)) {
            int i=EXC_MIRROR_MAPPING;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            return (UChar32)*pe;
        } else {
            return c;
        }
    }
}

U_CFUNC uint8_t
u_internalGetCombiningClass(UChar32 c) {
    uint32_t props=GET_PROPS_UNSAFE(c);
    if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        if(GET_CATEGORY(props)==U_NON_SPACING_MARK) {
            return (uint8_t)GET_UNSIGNED_VALUE(props);
        } else {
            return 0;
        }
    } else {
        /* the combining class is in bits 23..16 of the first exception value */
        return (uint8_t)(*GET_EXCEPTIONS(props)>>16);
    }
}

U_CAPI uint8_t U_EXPORT2
u_getCombiningClass(UChar32 c) {
    uint32_t props=GET_PROPS(c);
    if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        if(GET_CATEGORY(props)==U_NON_SPACING_MARK) {
            return (uint8_t)GET_UNSIGNED_VALUE(props);
        } else {
            return 0;
        }
    } else {
        /* the combining class is in bits 23..16 of the first exception value */
        return (uint8_t)(*GET_EXCEPTIONS(props)>>16);
    }
}

/* static data tables ------------------------------------------------------- */

struct BlockScriptMap {
    UChar        fFirstCode;
    UChar        fLastCode;
};
typedef struct BlockScriptMap BlockScriptMap;

static const BlockScriptMap fScriptIndex[] = {
/* Generated from the Unicode-3.0-beta blocks.txt file */
  { 0x0000, 0x007F }, /*BASIC_LATIN */
  { 0x0080, 0x00FF }, /*LATIN_1_SUPPLEMENT */
  { 0x0100, 0x017F }, /*LATIN_EXTENDED_A */
  { 0x0180, 0x024F }, /*LATIN_EXTENDED_B */
  { 0x0250, 0x02AF }, /*IPA_EXTENSIONS */
  { 0x02B0, 0x02FF }, /*SPACING_MODIFIER_LETTERS */
  { 0x0300, 0x036F }, /*COMBINING_DIACRITICAL_MARKS */
  { 0x0370, 0x03FF }, /*GREEK */
  { 0x0400, 0x04FF }, /*CYRILLIC */
  { 0x0530, 0x058F }, /*ARMENIAN */
  { 0x0590, 0x05FF }, /*HEBREW */
  { 0x0600, 0x06FF }, /*ARABIC */
  { 0x0700, 0x074F }, /*SYRIAC */
  { 0x0780, 0x07BF }, /*THAANA */
  { 0x0900, 0x097F }, /*DEVANAGARI */
  { 0x0980, 0x09FF }, /*BENGALI */
  { 0x0A00, 0x0A7F }, /*GURMUKHI */
  { 0x0A80, 0x0AFF }, /*GUJARATI */
  { 0x0B00, 0x0B7F }, /*ORIYA */
  { 0x0B80, 0x0BFF }, /*TAMIL */
  { 0x0C00, 0x0C7F }, /*TELUGU */
  { 0x0C80, 0x0CFF }, /*KANNADA */
  { 0x0D00, 0x0D7F }, /*MALAYALAM */
  { 0x0D80, 0x0DFF }, /*SINHALA */
  { 0x0E00, 0x0E7F }, /*THAI */
  { 0x0E80, 0x0EFF }, /*LAO */
  { 0x0F00, 0x0FFF }, /*TIBETAN */
  { 0x1000, 0x109F }, /*MYANMAR */
  { 0x10A0, 0x10FF }, /*GEORGIAN */
  { 0x1100, 0x11FF }, /*HANGUL_JAMO */
  { 0x1200, 0x137F }, /*ETHIOPIC */
  { 0x13A0, 0x13FF }, /*CHEROKEE */
  { 0x1400, 0x167F }, /*UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS */
  { 0x1680, 0x169F }, /*OGHAM */
  { 0x16A0, 0x16FF }, /*RUNIC */
  { 0x1780, 0x17FF }, /*KHMER */
  { 0x1800, 0x18AF }, /*MONGOLIAN */
  { 0x1E00, 0x1EFF }, /*LATIN_EXTENDED_ADDITIONAL */
  { 0x1F00, 0x1FFF }, /*GREEK_EXTENDED */
  { 0x2000, 0x206F }, /*GENERAL_PUNCTUATION */
  { 0x2070, 0x209F }, /*SUPERSCRIPTS_AND_SUBSCRIPTS */
  { 0x20A0, 0x20CF }, /*CURRENCY_SYMBOLS */
  { 0x20D0, 0x20FF }, /*COMBINING_MARKS_FOR_SYMBOLS */
  { 0x2100, 0x214F }, /*LETTERLIKE_SYMBOLS */
  { 0x2150, 0x218F }, /*NUMBER_FORMS */
  { 0x2190, 0x21FF }, /*ARROWS */
  { 0x2200, 0x22FF }, /*MATHEMATICAL_OPERATORS */
  { 0x2300, 0x23FF }, /*MISCELLANEOUS_TECHNICAL */
  { 0x2400, 0x243F }, /*CONTROL_PICTURES */
  { 0x2440, 0x245F }, /*OPTICAL_CHARACTER_RECOGNITION */
  { 0x2460, 0x24FF }, /*ENCLOSED_ALPHANUMERICS */
  { 0x2500, 0x257F }, /*BOX_DRAWING */
  { 0x2580, 0x259F }, /*BLOCK_ELEMENTS */
  { 0x25A0, 0x25FF }, /*GEOMETRIC_SHAPES */
  { 0x2600, 0x26FF }, /*MISCELLANEOUS_SYMBOLS */
  { 0x2700, 0x27BF }, /*DINGBATS */
  { 0x2800, 0x28FF }, /*BRAILLE_PATTERNS */
  { 0x2E80, 0x2EFF }, /*CJK_RADICALS_SUPPLEMENT */
  { 0x2F00, 0x2FDF }, /*KANGXI_RADICALS */
  { 0x2FF0, 0x2FFF }, /*IDEOGRAPHIC_DESCRIPTION_CHARACTERS */
  { 0x3000, 0x303F }, /*CJK_SYMBOLS_AND_PUNCTUATION */
  { 0x3040, 0x309F }, /*HIRAGANA */
  { 0x30A0, 0x30FF }, /*KATAKANA */
  { 0x3100, 0x312F }, /*BOPOMOFO */
  { 0x3130, 0x318F }, /*HANGUL_COMPATIBILITY_JAMO */
  { 0x3190, 0x319F }, /*KANBUN */
  { 0x31A0, 0x31BF }, /*BOPOMOFO_EXTENDED */
  { 0x3200, 0x32FF }, /*ENCLOSED_CJK_LETTERS_AND_MONTHS */
  { 0x3300, 0x33FF }, /*CJK_COMPATIBILITY */
  { 0x3400, 0x4DB5 }, /*CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A */
  { 0x4E00, 0x9FFF }, /*CJK_UNIFIED_IDEOGRAPHS */
  { 0xA000, 0xA48F }, /*YI_SYLLABLES */
  { 0xA490, 0xA4CF }, /*YI_RADICALS */
  { 0xAC00, 0xD7A3 }, /*HANGUL_SYLLABLES */
  { 0xD800, 0xDB7F }, /*HIGH_SURROGATES */
  { 0xDB80, 0xDBFF }, /*HIGH_PRIVATE_USE_SURROGATES */
  { 0xDC00, 0xDFFF }, /*LOW_SURROGATES */
  { 0xE000, 0xF8FF }, /*PRIVATE_USE */
  { 0xF900, 0xFAFF }, /*CJK_COMPATIBILITY_IDEOGRAPHS */
  { 0xFB00, 0xFB4F }, /*ALPHABETIC_PRESENTATION_FORMS */
  { 0xFB50, 0xFDFF }, /*ARABIC_PRESENTATION_FORMS_A */
  { 0xFE20, 0xFE2F }, /*COMBINING_HALF_MARKS */
  { 0xFE30, 0xFE4F }, /*CJK_COMPATIBILITY_FORMS */
  { 0xFE50, 0xFE6F }, /*SMALL_FORM_VARIANTS */
  { 0xFE70, 0xFEFE }, /*ARABIC_PRESENTATION_FORMS_B */
  { 0xFEFF, 0xFEFF }, /*U_SPECIALS */
  { 0xFF00, 0xFFEF }, /*HALFWIDTH_AND_FULLWIDTH_FORMS */
  { 0xFFF0, 0xFFFD }, /*SPECIALS_2 = "U_CHAR_SCRIPT_COUNT" (really specials) */
  { 0xFFFF, 0xFFFF } /* END */
};

const UChar cellWidthRanges[] =
    {
        0x0000, /* general scripts area*/
        0x1100, /* combining Hangul choseong*/
        0x1160, /* combining Hangul jungseong and jongseong*/
        0x1e00, /* Latin Extended Additional, Greek Extended*/
        0x2000, /* symbols and punctuation*/
        0x3000, /* CJK phonetics & symbols, CJK ideographs, Hangul syllables*/
        0xd800, /* surrogates, private use*/
        0xf900, /* CJK compatibility ideographs*/
        0xfb00, /* alphabetic presentation forms, Arabic presentations forms A, combining half marks*/
        0xfe30, /* CJK compatibility forms, small form variants*/
        0xfe70, /* Arabic presentation forms B*/
        0xff00, /* fullwidth ASCII*/
        0xff60, /* halfwidth, CJK punctuation, Katakana, Hangul Jamo*/
        0xffe0, /* fullwidth punctuation and currency signs*/
        0xffe8, /* halfwidth forms, arrows, and shapes*/
        0xfff0  /* specials*/
    };

const UChar cellWidthValues[] =
    {
        U_HALF_WIDTH,    /* general scripts area*/
        U_FULL_WIDTH,    /* combining Hangul choseong*/
        U_ZERO_WIDTH,    /* combining Hangul jungseong and jongseong*/
        U_HALF_WIDTH,    /* Latin extended aAdditional, Greek extended*/
        U_NEUTRAL_WIDTH, /* symbols and punctuation*/
        U_FULL_WIDTH,    /* CJK phonetics & symbols, CJK ideographs, Hangul syllables*/
        U_NEUTRAL_WIDTH, /* surrogates, private use*/
        U_FULL_WIDTH,    /* CJK compatibility ideographs*/
        U_HALF_WIDTH,    /* alphabetic presentation forms, Arabic presentations forms A, combining half marks*/
        U_FULL_WIDTH,    /* CJK compatibility forms, small form variants*/
        U_HALF_WIDTH,    /* Arabic presentation forms B*/
        U_FULL_WIDTH,    /* fullwidth ASCII*/
        U_HALF_WIDTH,    /* halfwidth CJK punctuation, Katakana, Hangul Jamo*/
        U_FULL_WIDTH,    /* fullwidth punctuation and currency signs*/
        U_HALF_WIDTH,    /* halfwidth forms, arrows, and shapes*/
        U_ZERO_WIDTH     /* specials*/
    };

const int16_t numCellWidthValues = 16;

/* Get the script associated with the character*/
UCharScript
u_charScript(UChar32 ch)
{
    int32_t i, j;
    UCharScript returnValue = U_NO_SCRIPT;

    /* surrogate support is still incomplete */
    if((uint32_t)ch>0xffff) {
        return U_NO_SCRIPT;
    }

    /* ### a binary search would be faster; maybe this should go into a data file, too */
    i = -1;
    for( j = 0; i == -1 && fScriptIndex[j].fFirstCode != 0xFFFF; ++j )
        if( fScriptIndex[j].fFirstCode <= ch && ch <= fScriptIndex[j].fLastCode ) {
            i = j;
            if(j == U_CHAR_SCRIPT_COUNT) /* "U_SPECIALS 2" */
                i = U_SPECIALS;
        }
    if(i >= U_CHAR_SCRIPT_COUNT) {
        returnValue = U_NO_SCRIPT;
    }
    else if( i != -1 ) {
        returnValue = (UCharScript)i;
    } 

    return returnValue;
}

/* Gets table cell width of the Unicode character.*/
uint16_t
u_charCellWidth(UChar32 ch)
{
    int16_t i;
    int32_t type = u_charType(ch);

    /* surrogate support is still incomplete */
    if((uint32_t)ch>0xffff) {
        return U_ZERO_WIDTH;
    }

    /* these Unicode character types are scattered throughout the Unicode range, so
     special-case for them*/
    if(IS_ISO_8_CONTROL(ch)) {
        return U_ZERO_WIDTH;
    }
    switch (type) {
        case U_UNASSIGNED:
        case U_NON_SPACING_MARK:
        case U_ENCLOSING_MARK:
        case U_LINE_SEPARATOR:
        case U_PARAGRAPH_SEPARATOR:
        case U_CONTROL_CHAR:
        case U_FORMAT_CHAR:
            return U_ZERO_WIDTH;

        default:
            /* for all remaining characters, find out which Unicode range they belong to using
               the table above, and then look up the appropriate return value in that table*/
            for (i = 0; i < numCellWidthValues; ++i)
                if (ch < cellWidthRanges[i])
                    break;
            --i;
            return cellWidthValues[i];
    }
}

void u_getUnicodeVersion(UVersionInfo versionArray) {
    if(versionArray!=NULL) {
        uprv_memcpy(versionArray, dataVersion, U_MAX_VERSION_LENGTH);
    }
}

/* string casing ------------------------------------------------------------ */

/*
 * These internal string case mapping functions are here instead of ustring.c
 * because we need efficient access to the character properties.
 */

enum {
    LOC_ROOT,
    LOC_TURKISH,
    LOC_LITHUANIAN
};

static int32_t
getCaseLocale(const char *locale) {
    if(locale==NULL) {
        locale=uloc_getDefault();
        if(locale==NULL) {
            return LOC_ROOT;
        }
    }
    if( ((locale[0]=='t' && locale[1]=='r') ||
         (locale[0]=='a' && locale[1]=='z')) &&
        (locale[2]=='_' || locale[2]==0)
    ) {
        return LOC_TURKISH;
    } else if(locale[0]=='l' && locale[1]=='t' && (locale[2]=='_' || locale[2]==0)) {
        return LOC_LITHUANIAN;
    } else {
        return LOC_ROOT;
    }
}

U_CFUNC int32_t
u_internalStrToLower(UChar *dest, int32_t destCapacity,
                     const UChar *src, int32_t srcLength,
                     const char *locale,
                     GrowBuffer *growBuffer, void *context,
                     UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    uint32_t *pe;
    const UChar *u;
    uint32_t props, firstExceptionValue;
    int32_t srcIndex, destIndex, i, loc;
    UChar32 c;
    UBool canGrow;

    /* do not attempt to grow if there is no growBuffer function or if it has failed before */
    canGrow= growBuffer!=NULL;

    /* test early, once, if there is a data file */
    if(!HAVE_DATA) {
        /*
         * If we do not have real character properties data,
         * then we only do a fixed-length ASCII case mapping.
         */
        if(srcLength<=destCapacity ||
            /* attempt to grow the buffer */
            (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, srcLength, 0)))
        ) {
            destIndex=srcLength;
            *pErrorCode=U_USING_DEFAULT_ERROR;
        } else {
            destIndex=destCapacity;
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }

        for(srcIndex=0; srcIndex<destIndex; ++srcIndex) {
            c=src[srcIndex];
            if((uint32_t)(c-0x41)<26) {
                dest[srcIndex]=(UChar)(c+0x20);
            } else {
                dest[srcIndex]=(UChar)c;
            }
        }

        return srcLength;
    }

    /* set up local variables */
    loc=getCaseLocale(locale);

    /* case mapping loop */
    srcIndex=destIndex=0;
    while(srcIndex<srcLength) {
        UTF_NEXT_CHAR(src, srcIndex, srcLength, c);
        props=GET_PROPS_UNSAFE(c);
        if(!PROPS_VALUE_IS_EXCEPTION(props)) {
            if((1UL<<GET_CATEGORY(props))&(1UL<<U_UPPERCASE_LETTER|1UL<<U_TITLECASE_LETTER)) {
                c+=GET_SIGNED_VALUE(props);
            }
        } else {
            pe=GET_EXCEPTIONS(props);
            firstExceptionValue=*pe;
            if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_SPECIAL_CASING)) {
                i=EXC_SPECIAL_CASING;
                ++pe;
                ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
                props=*pe;
                /* fill u and i with the case mapping result string */
                if(props&0x80000000) {
                    /* use hardcoded conditions and mappings */
                    u=buffer;
                    if(c==0x49) {
                        if(loc==LOC_TURKISH) {
                            /* turkish: I maps to dotless i */
                            buffer[0]=0x131;
                        } else {
                            /* other languages: I maps to i */
                            buffer[0]=0x69;
                        }
                        i=1;
                    } else if(c==0x3a3) {
                        /* greek capital sigma maps depending on whether the following character is a letter (L*) */
                        /* get the following character and check its general category */
                        if(srcIndex<srcLength) {
                            i=srcIndex;
                            UTF_NEXT_CHAR(src, i, srcLength, c);
                            if( /* is letter: is L* (Lu, Ll, Lt, Lm, or Lo) */
                                (1UL<<GET_CATEGORY(GET_PROPS_UNSAFE(c)))&
                                (1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER)
                            ) {
                                /* NONFINAL: the following is a letter */
                                buffer[0]=0x3c3; /* greek small sigma */
                            } else {
                                /* FINAL: the following is not a letter */
                                buffer[0]=0x3c2; /* greek small final sigma */
                            }
                        } else {
                            /* FINAL: this is the last character in the string */
                            buffer[0]=0x3c2; /* greek small final sigma */
                        }
                        i=1;
                    } else {
                        /* no known conditional special case mapping, output the code point itself */
                        i=0;
                        UTF_APPEND_CHAR_UNSAFE(buffer, i, c);
                    }
                } else {
                    /* get the special case mapping string from the data file */
                    u=ucharsTable+(props&0xffff);
                    i=(int32_t)(*u++)&0x1f;
                }

                /* output this case mapping result string */
                if( (destIndex+i)<=destCapacity ||
                    /* attempt to grow the buffer */
                    (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+i)+20, destIndex)))
                ) {
                    /* copy the case mapping to the destination */
                    while(i>0) {
                        dest[destIndex++]=*u++;
                        --i;
                    }
                } else {
                    /* buffer overflow */
                    /* copy as much as possible */
                    while(destIndex<destCapacity) {
                        dest[destIndex++]=*u++;
                        --i;
                    }
                    /* keep incrementing the destIndex for preflighting */
                    destIndex+=i;
                }

                /* do not fall through to the output of c */
                continue;
            } else if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_LOWERCASE)) {
                i=EXC_LOWERCASE;
                ++pe;
                ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
                c=(UChar32)*pe;
            }
        }

        /* handle 1:1 code point mappings from UnicodeData.txt */
        if(c<=0xffff) {
            if( destIndex<destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+1)+20, destIndex)))
            ) {
                dest[destIndex++]=(UChar)c;
            } else {
                /* buffer overflow */
                /* keep incrementing the destIndex for preflighting */
                ++destIndex;
            }
        } else {
            if( (destIndex+2)<=destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+2)+20, destIndex)))
            ) {
                dest[destIndex++]=(UChar)(0xd7c0+(c>>10));
                dest[destIndex++]=(UChar)(0xdc00|(c&0x3ff));
            } else {
                /* buffer overflow */
                /* write the first surrogate if possible */
                if(destIndex<destCapacity) {
                    dest[destIndex]=(UChar)(0xd7c0+(c>>10));
                }
                /* keep incrementing the destIndex for preflighting */
                destIndex+=2;
            }
        }
    }

    if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
    return destIndex;
}

U_CFUNC int32_t
u_internalStrToUpper(UChar *dest, int32_t destCapacity,
                     const UChar *src, int32_t srcLength,
                     const char *locale,
                     GrowBuffer *growBuffer, void *context,
                     UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    uint32_t *pe;
    const UChar *u;
    uint32_t props, firstExceptionValue;
    int32_t srcIndex, destIndex, i, loc;
    UChar32 c;
    UBool canGrow;

    /* do not attempt to grow if there is no growBuffer function or if it has failed before */
    canGrow= growBuffer!=NULL;

    /* test early, once, if there is a data file */
    if(!HAVE_DATA) {
        /*
         * If we do not have real character properties data,
         * then we only do a fixed-length ASCII case mapping.
         */
        if(srcLength<=destCapacity ||
            /* attempt to grow the buffer */
            (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, srcLength, 0)))
        ) {
            destIndex=srcLength;
            *pErrorCode=U_USING_DEFAULT_ERROR;
        } else {
            destIndex=destCapacity;
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }

        for(srcIndex=0; srcIndex<destIndex; ++srcIndex) {
            c=src[srcIndex];
            if((uint32_t)(c-0x61)<26) {
                dest[srcIndex]=(UChar)(c-0x20);
            } else {
                dest[srcIndex]=(UChar)c;
            }
        }

        return srcLength;
    }

    /* set up local variables */
    loc=getCaseLocale(locale);

    /* case mapping loop */
    srcIndex=destIndex=0;
    while(srcIndex<srcLength) {
        UTF_NEXT_CHAR(src, srcIndex, srcLength, c);
        props=GET_PROPS_UNSAFE(c);
        if(!PROPS_VALUE_IS_EXCEPTION(props)) {
            if(GET_CATEGORY(props)==U_LOWERCASE_LETTER) {
                c-=GET_SIGNED_VALUE(props);
            }
        } else {
            pe=GET_EXCEPTIONS(props);
            firstExceptionValue=*pe;
            if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_SPECIAL_CASING)) {
                i=EXC_SPECIAL_CASING;
                ++pe;
                ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
                props=*pe;
                /* fill u and i with the case mapping result string */
                if(props&0x80000000) {
                    /* use hardcoded conditions and mappings */
                    u=buffer;
                    if(c==0x69) {
                        if(loc==LOC_TURKISH) {
                            /* turkish: i maps to dotted I */
                            buffer[0]=0x130;
                        } else {
                            /* other languages: i maps to I */
                            buffer[0]=0x49;
                        }
                        i=1;
                    } else if(c==0x307) {
                        if(loc==LOC_LITHUANIAN) {
                            /* lithuanian: remove DOT ABOVE after U+0069 "i" with upper or titlecase */
                            /* ### TODO: test this with Unicode 3.1 SpecialCasing.txt */
                            /* search backwards for the base letter - this works only because src and dest do not overlap! */
                            i=srcIndex;
                            while(i>0) {
                                UTF_PREV_CHAR(src, 0, i, c);
                                props=GET_PROPS_UNSAFE(c);
                                if(GET_CATEGORY(props)!=U_NON_SPACING_MARK) { /* Mn */
                                    break;
                                }
                            }
                            /* is the base letter an 'i' (U+0069)? */
                            if(c==0x69) {
                                /* yes, remove the dot (continue without output) */
                                continue;
                            } else {
                                /* no, keep the dot */
                                buffer[0]=0x307;
                            }
                        } else {
                            /* other languages: keep the dot */
                            buffer[0]=0x307;
                        }
                        i=1;
                    } else {
                        /* no known conditional special case mapping, output the code point itself */
                        i=0;
                        UTF_APPEND_CHAR_UNSAFE(buffer, i, c);
                    }
                } else {
                    /* get the special case mapping string from the data file */
                    u=ucharsTable+(props&0xffff);
                    i=(int32_t)*u++;

                    /* skip the lowercase result string */
                    u+=i&0x1f;
                    i=(i>>5)&0x1f;
                }

                /* output this case mapping result string */
                if( (destIndex+i)<=destCapacity ||
                    /* attempt to grow the buffer */
                    (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+i)+20, destIndex)))
                ) {
                    /* copy the case mapping to the destination */
                    while(i>0) {
                        dest[destIndex++]=*u++;
                        --i;
                    }
                } else {
                    /* buffer overflow */
                    /* copy as much as possible */
                    while(destIndex<destCapacity) {
                        dest[destIndex++]=*u++;
                        --i;
                    }
                    /* keep incrementing the destIndex for preflighting */
                    destIndex+=i;
                }

                /* do not fall through to the output of c */
                continue;
            } else if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_UPPERCASE)) {
                i=EXC_UPPERCASE;
                ++pe;
                ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
                c=(UChar32)*pe;
            }
        }

        /* handle 1:1 code point mappings from UnicodeData.txt */
        if(c<=0xffff) {
            if( destIndex<destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+1)+20, destIndex)))
            ) {
                dest[destIndex++]=(UChar)c;
            } else {
                /* buffer overflow */
                /* keep incrementing the destIndex for preflighting */
                ++destIndex;
            }
        } else {
            if( (destIndex+2)<=destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+2)+20, destIndex)))
            ) {
                dest[destIndex++]=(UChar)(0xd7c0+(c>>10));
                dest[destIndex++]=(UChar)(0xdc00|(c&0x3ff));
            } else {
                /* buffer overflow */
                /* write the first surrogate if possible */
                if(destIndex<destCapacity) {
                    dest[destIndex]=(UChar)(0xd7c0+(c>>10));
                }
                /* keep incrementing the destIndex for preflighting */
                destIndex+=2;
            }
        }
    }

    if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
    return destIndex;
}

/* case folding ------------------------------------------------------------- */

/*
 * Case folding is similar to lowercasing.
 * The result may be a simple mapping, i.e., a single code point, or
 * a full mapping, i.e., a string.
 * If the case folding for a code point is the same as its simple (1:1) lowercase mapping,
 * then only the lowercase mapping is stored.
 */

/* return the simple case folding mapping for c */
U_CAPI UChar32 U_EXPORT2
u_foldCase(UChar32 c, uint32_t options) {
    uint32_t props=GET_PROPS(c);
    if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        if((1UL<<GET_CATEGORY(props))&(1UL<<U_UPPERCASE_LETTER|1UL<<U_TITLECASE_LETTER)) {
            return c+GET_SIGNED_VALUE(props);
        }
    } else {
        uint32_t *pe=GET_EXCEPTIONS(props);
        uint32_t firstExceptionValue=*pe;
        if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_CASE_FOLDING)) {
            uint32_t *oldPE=pe;
            int i=EXC_CASE_FOLDING;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            props=*pe;
            if(props!=0) {
                /* return the simple mapping, if there is one */
                const UChar *uchars=ucharsTable+(props&0xffff);
                UChar32 simple;
                i=0;
                UTF_NEXT_CHAR_UNSAFE(uchars, i, simple);
                if(simple!=0) {
                    return simple;
                }
                /* fall through to use the lowercase exception value if there is no simple mapping */
                pe=oldPE;
            } else {
                /* special case folding mappings, hardcoded */
                if(options==U_FOLD_CASE_DEFAULT && (uint32_t)(c-0x130)<=1) {
                    /* map dotted I and dotless i to U+0069 small i */
                    return 0x69;
                }
                /* return c itself because it is excluded from case folding */
                return c;
            }
        }
        /* not else! - allow to fall through from above */
        if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_LOWERCASE)) {
            int i=EXC_LOWERCASE;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            return (UChar32)*pe;
        }
    }
    return c; /* no mapping - return c itself */
}

/* internal, return the full case folding mapping for c, must be used only if uprv_haveProperties() is true */
U_CFUNC int32_t
u_internalFoldCase(UChar32 c, UChar dest[32], uint32_t options) {
    uint32_t props=GET_PROPS_UNSAFE(c);
    int32_t i;

    if(!PROPS_VALUE_IS_EXCEPTION(props)) {
        if((1UL<<GET_CATEGORY(props))&(1UL<<U_UPPERCASE_LETTER|1UL<<U_TITLECASE_LETTER)) {
            c+=GET_SIGNED_VALUE(props);
        }
    } else {
        uint32_t *pe=GET_EXCEPTIONS(props);
        uint32_t firstExceptionValue=*pe;
        if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_CASE_FOLDING)) {
            i=EXC_CASE_FOLDING;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            props=*pe;
            if(props!=0) {
                /* return the full mapping */
                const UChar *uchars=ucharsTable+(props&0xffff)+2;
                props>>=24;

                /* copy the result string */
                i=0;
                while(i<(int32_t)props) {
                    dest[i++]=*uchars++;
                }
                return i;
            } else {
                /* special case folding mappings, hardcoded */
                if(options==U_FOLD_CASE_DEFAULT && (uint32_t)(c-0x130)<=1) {
                    /* map dotted I and dotless i to U+0069 small i */
                    dest[0]=0x69;
                    return 1;
                }
                /* return c itself because it is excluded from case folding */
            }
        } else if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_LOWERCASE)) {
            i=EXC_LOWERCASE;
            ++pe;
            ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
            c=(UChar32)*pe;
        }
    }

    /* write c to dest */
    i=0;
    UTF_APPEND_CHAR_UNSAFE(dest, i, c);
    return i;
}

/* case-fold the source string using the full mappings */
U_CFUNC int32_t
u_internalStrFoldCase(UChar *dest, int32_t destCapacity,
                      const UChar *src, int32_t srcLength,
                      uint32_t options,
                      GrowBuffer *growBuffer, void *context,
                      UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    uint32_t *pe;
    const UChar *uchars, *u;
    uint32_t props, firstExceptionValue;
    int32_t srcIndex, destIndex, i;
    UChar32 c;
    UBool canGrow;

    /* do not attempt to grow if there is no growBuffer function or if it has failed before */
    canGrow= growBuffer!=NULL;

    /* test early, once, if there is a data file */
    if(!HAVE_DATA) {
        /*
         * If we do not have real character properties data,
         * then we only do a fixed-length ASCII case mapping.
         */
        if(srcLength<=destCapacity ||
            /* attempt to grow the buffer */
            (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, srcLength, 0)))
        ) {
            destIndex=srcLength;
            *pErrorCode=U_USING_DEFAULT_ERROR;
        } else {
            destIndex=destCapacity;
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }

        for(srcIndex=0; srcIndex<destIndex; ++srcIndex) {
            c=src[srcIndex];
            if((uint32_t)(c-0x41)<26) {
                dest[srcIndex]=(UChar)(c+0x20);
            } else {
                dest[srcIndex]=(UChar)c;
            }
        }

        return srcLength;
    }

    /* set up local variables */
    /* add 2 because we always need to skip the 2 UChars for the simple mappings */
    uchars=ucharsTable+2;

    /* case mapping loop */
    srcIndex=destIndex=0;
    while(srcIndex<srcLength) {
        UTF_NEXT_CHAR(src, srcIndex, srcLength, c);
        props=GET_PROPS_UNSAFE(c);
        if(!PROPS_VALUE_IS_EXCEPTION(props)) {
            if((1UL<<GET_CATEGORY(props))&(1UL<<U_UPPERCASE_LETTER|1UL<<U_TITLECASE_LETTER)) {
                c+=GET_SIGNED_VALUE(props);
            }
        } else {
            pe=GET_EXCEPTIONS(props);
            firstExceptionValue=*pe;
            if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_CASE_FOLDING)) {
                i=EXC_CASE_FOLDING;
                ++pe;
                ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
                props=*pe;
                /* fill u and i with the case mapping result string */
                if(props!=0) {
                    /* get the case folding string from the data file */
                    u=uchars+(props&0xffff);
                    i=(int32_t)(props>>24);
                } else {
                    /* special case folding mappings, hardcoded */
                    u=buffer;
                    if(options==U_FOLD_CASE_DEFAULT && (uint32_t)(c-0x130)<=1) {
                        /* map dotted I and dotless i to U+0069 small i */
                        buffer[0]=0x69;
                        i=1;
                    } else {
                        /* output c itself because it is excluded from case folding */
                        i=0;
                        UTF_APPEND_CHAR_UNSAFE(buffer, i, c);
                    }
                }

                /* output this case mapping result string */
                if( (destIndex+i)<=destCapacity ||
                    /* attempt to grow the buffer */
                    (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+i)+20, destIndex)))
                ) {
                    /* copy the case mapping to the destination */
                    while(i>0) {
                        dest[destIndex++]=*u++;
                        --i;
                    }
                } else {
                    /* buffer overflow */
                    /* copy as much as possible */
                    while(destIndex<destCapacity) {
                        dest[destIndex++]=*u++;
                        --i;
                    }
                    /* keep incrementing the destIndex for preflighting */
                    destIndex+=i;
                }

                /* do not fall through to the output of c */
                continue;
            } else if(HAVE_EXCEPTION_VALUE(firstExceptionValue, EXC_LOWERCASE)) {
                i=EXC_LOWERCASE;
                ++pe;
                ADD_EXCEPTION_OFFSET(firstExceptionValue, i, pe);
                c=(UChar32)*pe;
            }
        }

        /* handle 1:1 code point mappings from UnicodeData.txt */
        if(c<=0xffff) {
            if( destIndex<destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+1)+20, destIndex)))
            ) {
                dest[destIndex++]=(UChar)c;
            } else {
                /* buffer overflow */
                /* keep incrementing the destIndex for preflighting */
                ++destIndex;
            }
        } else {
            if( (destIndex+2)<=destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity, destCapacity+2*(srcLength-srcIndex+2)+20, destIndex)))
            ) {
                dest[destIndex++]=(UChar)(0xd7c0+(c>>10));
                dest[destIndex++]=(UChar)(0xdc00|(c&0x3ff));
            } else {
                /* buffer overflow */
                /* write the first surrogate if possible */
                if(destIndex<destCapacity) {
                    dest[destIndex]=(UChar)(0xd7c0+(c>>10));
                }
                /* keep incrementing the destIndex for preflighting */
                destIndex+=2;
            }
        }
    }

    if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
    return destIndex;
}
