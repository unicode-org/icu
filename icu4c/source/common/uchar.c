/*
********************************************************************************
*   Copyright (C) 1996-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File UCHAR.CPP
*
* Modification History:
*
*   Date        Name        Description
*   04/02/97    aliu        Creation.
*
*   4/15/99     Madhu       Updated all the function definitions for C Implementation
*   5/20/99     Madhu		Added the function u_getVersion()
*   8/19/1999   srl         Upgraded scripts to Unicode3.0 
*   11/11/1999  weiv        added u_isalnum(), cleaned comments
*   01/11/2000  helena      Renamed u_getVersion to u_getUnicodeVersion.
********************************************************************************************
*/
#include "unicode/utypes.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "umutex.h"
#include "unicode/uchar.h"
#include "unicode/udata.h"
#include "cmemory.h"
#include "cstring.h"

/* dynamically loaded Unicode character properties -------------------------- */

/* fallback properties for the ASCII range if the data cannot be loaded */
/* these are printed by genprops in verbose mode */
static uint32_t staticProps32Table[0xa0]={
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

static int8_t havePropsData=0;

/* index values loaded from uprops.dat */
static uint16_t indexes[8];

enum {
    INDEX_STAGE_2_BITS,
    INDEX_STAGE_3_BITS,
    INDEX_EXCEPTIONS
};

/* access values calculated from indexes */
static uint16_t stage23Bits, stage2Mask, stage3Mask;

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
        stage23Bits=indexes[INDEX_STAGE_2_BITS]+indexes[INDEX_STAGE_3_BITS];
        stage2Mask=(1<<indexes[INDEX_STAGE_2_BITS])-1;
        stage3Mask=(1<<indexes[INDEX_STAGE_3_BITS])-1;
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

    EXC_MIRROR_MAPPING
};

enum {
    EXCEPTION_SHIFT=5,
    BIDI_SHIFT,
    MIRROR_SHIFT=BIDI_SHIFT+5,
    VALUE_SHIFT=20,

    VALUE_BITS=32-VALUE_SHIFT
};

/* getting a uint32_t properties word from the data */
#define HAVE_DATA (havePropsData>0 || havePropsData==0 && loadPropsData()>0)
#define VALIDATE(c) (((uint32_t)(c))<=0x10ffff && HAVE_DATA)
#define GET_PROPS(c) \
    (((uint32_t)(c))<=0x10ffff ? \
        HAVE_DATA ? \
            props32Table[ \
                propsTable[ \
                    propsTable[ \
                        propsTable[8+(c>>stage23Bits)]+ \
                        (c>>indexes[INDEX_STAGE_3_BITS]&stage2Mask)]+ \
                    (c&stage3Mask)] \
            ] \
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

/* API functions ------------------------------------------------------------ */

/* Gets the Unicode character's general category.*/
U_CAPI int8_t U_EXPORT2
u_charType(UChar32 c) {
    return (int8_t)GET_CATEGORY(GET_PROPS(c));
}

/* Checks if ch is a lower case letter.*/
U_CAPI UBool U_EXPORT2
u_islower(UChar32 c) {
    return GET_CATEGORY(GET_PROPS(c))==U_LOWERCASE_LETTER;
}

/* Checks if ch is an upper case letter.*/
U_CAPI UBool U_EXPORT2
u_isupper(UChar32 c) {
    return GET_CATEGORY(GET_PROPS(c))==U_UPPERCASE_LETTER;
}

/* Checks if ch is a title case letter; usually upper case letters.*/
U_CAPI UBool U_EXPORT2
u_istitle(UChar32 c) {
    return GET_CATEGORY(GET_PROPS(c))==U_TITLECASE_LETTER;
}

/* Checks if ch is a decimal digit. */
U_CAPI UBool U_EXPORT2
u_isdigit(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_OTHER_NUMBER|1UL<<U_LETTER_NUMBER)
           )!=0;
}

/* Checks if the Unicode character is a letter.*/
U_CAPI UBool U_EXPORT2
u_isalpha(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER)
           )!=0;
}

/* Checks if ch is a letter or a decimal digit */
U_CAPI UBool U_EXPORT2
u_isalnum(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_OTHER_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER)
           )!=0;
}

/* Checks if ch is a unicode character with assigned character type.*/
U_CAPI UBool U_EXPORT2
u_isdefined(UChar32 c) {
    return GET_PROPS(c)!=0;
}

/* Checks if the Unicode character is a base form character that can take a diacritic.*/
U_CAPI UBool U_EXPORT2
u_isbase(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_OTHER_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_NON_SPACING_MARK|1UL<<U_ENCLOSING_MARK|1UL<<U_COMBINING_SPACING_MARK)
           )!=0;
}

/* Checks if the Unicode character is a control character.*/
U_CAPI UBool U_EXPORT2
u_iscntrl(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_CONTROL_CHAR|1UL<<U_FORMAT_CHAR|1UL<<U_LINE_SEPARATOR|1UL<<U_PARAGRAPH_SEPARATOR)
           )!=0;
}

/* Checks if the Unicode character is a space character.*/
UBool
u_isspace(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_SPACE_SEPARATOR|1UL<<U_LINE_SEPARATOR|1UL<<U_PARAGRAPH_SEPARATOR)
           )!=0;
}

/* Checks if the Unicode character is a whitespace character.*/
U_CAPI UBool U_EXPORT2
u_isWhitespace(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_SPACE_SEPARATOR|1UL<<U_LINE_SEPARATOR|1UL<<U_PARAGRAPH_SEPARATOR)
           )!=0 &&
           c!=0xa0 && c!=0xfeff;
}

/* Checks if the Unicode character is printable.*/
U_CAPI UBool U_EXPORT2
u_isprint(UChar32 c) {    
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_OTHER_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_NON_SPACING_MARK|1UL<<U_ENCLOSING_MARK|1UL<<U_COMBINING_SPACING_MARK|
             1UL<<U_SPACE_SEPARATOR|1UL<<U_LINE_SEPARATOR|1UL<<U_PARAGRAPH_SEPARATOR|
             1UL<<U_DASH_PUNCTUATION|1UL<<U_START_PUNCTUATION|1UL<<U_END_PUNCTUATION|1UL<<U_CONNECTOR_PUNCTUATION|1UL<<U_OTHER_PUNCTUATION|
             1UL<<U_MATH_SYMBOL|1UL<<U_CURRENCY_SYMBOL|1UL<<U_MODIFIER_SYMBOL|1UL<<U_OTHER_SYMBOL)
           )!=0;
}

/* Checks if the Unicode character can start a Unicode identifier.*/
U_CAPI UBool U_EXPORT2
u_isIDStart(UChar32 c) {
    /* same as u_isalpha() */
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER)
           )!=0;
}

/* Checks if the Unicode character can be a Unicode identifier part other than starting the
 identifier.*/
U_CAPI UBool U_EXPORT2
u_isIDPart(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_CONNECTOR_PUNCTUATION|1UL<<U_COMBINING_SPACING_MARK|1UL<<U_NON_SPACING_MARK)
           )!=0 ||
           u_isIDIgnorable(c);
}

/*Checks if the Unicode character can be ignorable in a Java or Unicode identifier.*/
U_CAPI UBool U_EXPORT2
u_isIDIgnorable(UChar32 c) {
    return (uint32_t)c<=8 ||
           (uint32_t)(c-0xe)<=(0x1b-0xe) ||
           (uint32_t)(c-0x7f)<=(0x9f-0x7f) ||
           (uint32_t)(c-0x200a)<=(0x200f-0x200a) ||
           (uint32_t)(c-0x206a)<=(0x206f-0x206a) ||
           c==0xfeff;
}

/*Checks if the Unicode character can start a Java identifier.*/
U_CAPI UBool U_EXPORT2
u_isJavaIDStart(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_CURRENCY_SYMBOL|1UL<<U_CONNECTOR_PUNCTUATION)
           )!=0;
}

/*Checks if the Unicode character can be a Java identifier part other than starting the
 * identifier.
 */
U_CAPI UBool U_EXPORT2
u_isJavaIDPart(UChar32 c) {
    return ((1UL<<GET_CATEGORY(GET_PROPS(c)))&
            (1UL<<U_DECIMAL_DIGIT_NUMBER|1UL<<U_LETTER_NUMBER|
             1UL<<U_UPPERCASE_LETTER|1UL<<U_LOWERCASE_LETTER|1UL<<U_TITLECASE_LETTER|1UL<<U_MODIFIER_LETTER|1UL<<U_OTHER_LETTER|
             1UL<<U_CURRENCY_SYMBOL|1UL<<U_CONNECTOR_PUNCTUATION|
             1UL<<U_COMBINING_SPACING_MARK|1UL<<U_NON_SPACING_MARK)
           )!=0 ||
           u_isIDIgnorable(c);
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
        return (props>>BIDI_SHIFT)&0x1f;
    } else {
        return U_BOUNDARY_NEUTRAL;
    }
}

U_CAPI UBool U_EXPORT2
u_isMirrored(UChar32 c) {
    return GET_PROPS(c)&(1UL<<MIRROR_SHIFT) ? TRUE : FALSE;
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

/* ### this function will become public */
U_CFUNC void
u_versionFromString(UVersionInfo versionArray, const char *versionString);

void u_getUnicodeVersion(UVersionInfo versionArray) {
    if(versionArray!=NULL) {
        uprv_memcpy(versionArray, dataVersion, U_MAX_VERSION_LENGTH);
    }
}
