/*
**********************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File UCHAR.H
*
* Modification History:
*
*   Date        Name        Description
*   04/02/97    aliu        Creation.
*   03/29/99    helena      Updated for C APIs.
*   4/15/99     Madhu       Updated for C Implementation and Javadoc
*   5/20/99     Madhu       Added the function u_getVersion()
*   8/19/1999   srl         Upgraded scripts to Unicode 3.0
*   8/27/1999   schererm    UCharDirection constants: U_...
*   11/11/1999  weiv        added u_isalnum(), cleaned comments
*   01/11/2000  helena      Renamed u_getVersion to u_getUnicodeVersion().
******************************************************************************
*/

#ifndef UCHAR_H
#define UCHAR_H

#include "unicode/utypes.h"
/*==========================================================================*/
/* Unicode version number                                                   */
/*==========================================================================*/
#define U_UNICODE_VERSION "3.0.0"

/**
 * \file
 * \brief   C API: Unicode Char 
 *
 * <h2> Unicode C API </h2>
 * The Unicode C API allows you to query the properties associated with individual 
 * Unicode character values.  
 * <p>
 * The Unicode character information, provided implicitly by the 
 * Unicode character encoding standard, includes information about the script 
 * (for example, symbols or control characters) to which the character belongs,
 * as well as semantic information such as whether a character is a digit or 
 * uppercase, lowercase, or uncased.
 * <P>
 */

/**
 * Constants.
 */

/** The lowest Unicode code point value. Code points are non-negative. */
#define UCHAR_MIN_VALUE 0

/**
 * The highest Unicode code point value (scalar value) according to
 * The Unicode Standard. This is a 21-bit value (20.1 bits, rounded up).
 * For a single character, UChar32 is a simple type that can hold any code point value.
 */
#define UCHAR_MAX_VALUE 0x10ffff

 /**
 * Data for enumerated Unicode general category types
 * @draft
 */
enum UCharCategory
{
    /** Non-category for unassigned and non-character code points. */
    U_UNASSIGNED              = 0,
    /** Lu */
    U_UPPERCASE_LETTER        = 1,
    /** Ll */
    U_LOWERCASE_LETTER        = 2,
    /** Lt */
    U_TITLECASE_LETTER        = 3,
    /** Lm */
    U_MODIFIER_LETTER         = 4,
    /** Lo */
    U_OTHER_LETTER            = 5,
    /** Mn */
    U_NON_SPACING_MARK        = 6,
    /** Me */
    U_ENCLOSING_MARK          = 7,
    /** Mc */
    U_COMBINING_SPACING_MARK  = 8,
    /** Nd */
    U_DECIMAL_DIGIT_NUMBER    = 9,
    /** Nl */
    U_LETTER_NUMBER           = 10,
    /** No */
    U_OTHER_NUMBER            = 11,
    /** Zs */
    U_SPACE_SEPARATOR         = 12,
    /** Zl */
    U_LINE_SEPARATOR          = 13,
    /** Zp */
    U_PARAGRAPH_SEPARATOR     = 14,
    /** Cc */
    U_CONTROL_CHAR            = 15,
    /** Cf */
    U_FORMAT_CHAR             = 16,
    /** Co */
    U_PRIVATE_USE_CHAR        = 17,
    /** Cs */
    U_SURROGATE               = 18,
    /** Pd */
    U_DASH_PUNCTUATION        = 19,
    /** Ps */
    U_START_PUNCTUATION       = 20,
    /** Pe */
    U_END_PUNCTUATION         = 21,
    /** Pc */
    U_CONNECTOR_PUNCTUATION   = 22,
    /** Po */
    U_OTHER_PUNCTUATION       = 23,
    /** Sm */
    U_MATH_SYMBOL             = 24,
    /** Sc */
    U_CURRENCY_SYMBOL         = 25,
    /** Sk */
    U_MODIFIER_SYMBOL         = 26,
    /** So */
    U_OTHER_SYMBOL            = 27,
    /** Pi */
    U_INITIAL_PUNCTUATION     = 28,
    /** Pf */
    U_FINAL_PUNCTUATION       = 29,
    /** Cn */
    U_GENERAL_OTHER_TYPES     = 30,
    /** One higher than the last enum UCharCategory constant. */
    U_CHAR_CATEGORY_COUNT
};

typedef enum UCharCategory UCharCategory;
/**
 * This specifies the language directional property of a character set.
 */
enum UCharDirection   { 
    /** */
    U_LEFT_TO_RIGHT               = 0, 
    /** */
    U_RIGHT_TO_LEFT               = 1, 
    /** */
    U_EUROPEAN_NUMBER             = 2,
    /** */
    U_EUROPEAN_NUMBER_SEPARATOR   = 3,
    /** */
    U_EUROPEAN_NUMBER_TERMINATOR  = 4,
    /** */
    U_ARABIC_NUMBER               = 5,
    /** */
    U_COMMON_NUMBER_SEPARATOR     = 6,
    /** */
    U_BLOCK_SEPARATOR             = 7,
    /** */
    U_SEGMENT_SEPARATOR           = 8,
    /** */
    U_WHITE_SPACE_NEUTRAL         = 9, 
    /** */
    U_OTHER_NEUTRAL               = 10, 
    /** */
    U_LEFT_TO_RIGHT_EMBEDDING     = 11,
    /** */
    U_LEFT_TO_RIGHT_OVERRIDE      = 12,
    /** */
    U_RIGHT_TO_LEFT_ARABIC        = 13,
    /** */
    U_RIGHT_TO_LEFT_EMBEDDING     = 14,
    /** */
    U_RIGHT_TO_LEFT_OVERRIDE      = 15,
    /** */
    U_POP_DIRECTIONAL_FORMAT      = 16,
    /** */
    U_DIR_NON_SPACING_MARK        = 17,
    /** */
    U_BOUNDARY_NEUTRAL            = 18,
    /** */
    U_CHAR_DIRECTION_COUNT
};

typedef enum UCharDirection UCharDirection;
/**
 * Script range as defined in the Unicode standard.
 */

/* Please keep these values in sync with EUnicodeScript */
/**
 * Generated from Unicode Data files 
 * These are the same values as Unicode::EUnicodeScript
 * @draft
 */
enum UBlockCode {
/* Script names */
    /** */
    UBLOCK_BASIC_LATIN = 1,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_BASIC_LATIN = 1,
    /** */
    UBLOCK_LATIN_1_SUPPLEMENT=2,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_LATIN_1_SUPPLEMENT=2,

    /** */
    UBLOCK_LATIN_EXTENDED_A =3,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_LATIN_EXTENDED_A=3,

    /** */
    UBLOCK_LATIN_EXTENDED_B =4,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_LATIN_EXTENDED_B=4,

    /** */
    UBLOCK_IPA_EXTENSIONS =5,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_IPA_EXTENSIONS=5,
    
    /** */
    UBLOCK_SPACING_MODIFIER_LETTERS =6,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_SPACING_MODIFIER_LETTERS=6,

    /** */
    UBLOCK_COMBINING_DIACRITICAL_MARKS =7,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_COMBINING_DIACRITICAL_MARKS=7,
    
    /** */
    UBLOCK_GREEK =8,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_GREEK=8,

    /** */
    UBLOCK_CYRILLIC =9,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CYRILLIC=9,

    /** */
    UBLOCK_ARMENIAN =10,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ARMENIAN=10,

    /** */
    UBLOCK_HEBREW =11,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_HEBREW=11,

    /** */
    UBLOCK_ARABIC =12,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ARABIC=12,

    /** */
    UBLOCK_SYRIAC =13,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_SYRIAC=13,

    /** */
    UBLOCK_THAANA =14,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_THAANA=14,

    /** */
    UBLOCK_DEVANAGARI =15,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_DEVANAGARI=15,

    /** */
    UBLOCK_BENGALI =16,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_BENGALI=16,

    /** */
    UBLOCK_GURMUKHI =17,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_GURMUKHI=17,

    /** */
    UBLOCK_GUJARATI =18,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_GUJARATI=18,

    /** */
    UBLOCK_ORIYA =19,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ORIYA=19,

    /** */
    UBLOCK_TAMIL =20,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_TAMIL=20,

    /** */
    UBLOCK_TELUGU =21,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_TELUGU=21,

    /** */
    UBLOCK_KANNADA =22,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_KANNADA=22,

    /** */
    UBLOCK_MALAYALAM =23,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_MALAYALAM=23,

    /** */
    UBLOCK_SINHALA =24,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_SINHALA=24,

    /** */
    UBLOCK_THAI =25,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_THAI=25,

    /** */
    UBLOCK_LAO =26,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_LAO=26,

    /** */
    UBLOCK_TIBETAN =27,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_TIBETAN=27,

    /** */
    UBLOCK_MYANMAR =28,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_MYANMAR=28,

    /** */
    UBLOCK_GEORGIAN =29,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_GEORGIAN=29,

    /** */
    UBLOCK_HANGUL_JAMO =30,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_HANGUL_JAMO=30,

    /** */
    UBLOCK_ETHIOPIC =31,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ETHIOPIC=31,

    /** */
    UBLOCK_CHEROKEE =32,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CHEROKEE=32,

    /** */
    UBLOCK_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS =33,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS=33,

    /** */
    UBLOCK_OGHAM =34,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_OGHAM=34,

    /** */
    UBLOCK_RUNIC =35,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_RUNIC=35,

    /** */
    UBLOCK_KHMER =36,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_KHMER=36,

    /** */
    UBLOCK_MONGOLIAN =37,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_MONGOLIAN=37,

    /** */
    UBLOCK_LATIN_EXTENDED_ADDITIONAL =38,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_LATIN_EXTENDED_ADDITIONAL=38,

    /** */
    UBLOCK_GREEK_EXTENDED =39,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_GREEK_EXTENDED=39,

    /** */
    UBLOCK_GENERAL_PUNCTUATION =40,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_GENERAL_PUNCTUATION=40,

    /** */
    UBLOCK_SUPERSCRIPTS_AND_SUBSCRIPTS =41,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_SUPERSCRIPTS_AND_SUBSCRIPTS=41,
    
    /** */
    UBLOCK_CURRENCY_SYMBOLS =42,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CURRENCY_SYMBOLS=42,
    
    /** */
    UBLOCK_COMBINING_MARKS_FOR_SYMBOLS =43,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_COMBINING_MARKS_FOR_SYMBOLS=43,
    
    /** */
    UBLOCK_LETTERLIKE_SYMBOLS =44,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_LETTERLIKE_SYMBOLS=44,
    
    /** */
    UBLOCK_NUMBER_FORMS =45,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_NUMBER_FORMS=45,

    /** */
    UBLOCK_ARROWS =46,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ARROWS=46,

    /** */
    UBLOCK_MATHEMATICAL_OPERATORS =47,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_MATHEMATICAL_OPERATORS=47,

    /** */
    UBLOCK_MISCELLANEOUS_TECHNICAL =48,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_MISCELLANEOUS_TECHNICAL=48,

    /** */
    UBLOCK_CONTROL_PICTURES =49,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CONTROL_PICTURES=49,

    /** */
    UBLOCK_OPTICAL_CHARACTER_RECOGNITION =50,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_OPTICAL_CHARACTER_RECOGNITION=50,

    /** */
    UBLOCK_ENCLOSED_ALPHANUMERICS =51,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ENCLOSED_ALPHANUMERICS=51,

    /** */
    UBLOCK_BOX_DRAWING =52,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_BOX_DRAWING=52,

    /** */
    UBLOCK_BLOCK_ELEMENTS =53,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_BLOCK_ELEMENTS=53,

    /** */
    UBLOCK_GEOMETRIC_SHAPES =54,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_GEOMETRIC_SHAPES=54,

    /** */
    UBLOCK_MISCELLANEOUS_SYMBOLS =55,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_MISCELLANEOUS_SYMBOLS=55,

    /** */
    UBLOCK_DINGBATS =56,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_DINGBATS=56,

    /** */
    UBLOCK_BRAILLE_PATTERNS =57,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_BRAILLE_PATTERNS=57,

    /** */
    UBLOCK_CJK_RADICALS_SUPPLEMENT =58,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CJK_RADICALS_SUPPLEMENT=58,

    /** */
    UBLOCK_KANGXI_RADICALS =59,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_KANGXI_RADICALS=59,

    /** */
    UBLOCK_IDEOGRAPHIC_DESCRIPTION_CHARACTERS =60,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_IDEOGRAPHIC_DESCRIPTION_CHARACTERS=60,

    /** */
    UBLOCK_CJK_SYMBOLS_AND_PUNCTUATION =61,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CJK_SYMBOLS_AND_PUNCTUATION=61,

    /** */
    UBLOCK_HIRAGANA =62,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_HIRAGANA=62,

    /** */
    UBLOCK_KATAKANA =63,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_KATAKANA=63,

    /** */
    UBLOCK_BOPOMOFO =64,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_BOPOMOFO=64,

    /** */
    UBLOCK_HANGUL_COMPATIBILITY_JAMO =65,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_HANGUL_COMPATIBILITY_JAMO=65,

    /** */
    UBLOCK_KANBUN =66,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_KANBUN=66,

    /** */
    UBLOCK_BOPOMOFO_EXTENDED =67,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_BOPOMOFO_EXTENDED=67,

    /** */
    UBLOCK_ENCLOSED_CJK_LETTERS_AND_MONTHS =68,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ENCLOSED_CJK_LETTERS_AND_MONTHS=68,

    /** */
    UBLOCK_CJK_COMPATIBILITY =69,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CJK_COMPATIBILITY=69,

    /** */
    UBLOCK_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A =70,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A=70,

    /** */
    UBLOCK_CJK_UNIFIED_IDEOGRAPHS =71,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CJK_UNIFIED_IDEOGRAPHS=71,

    /** */
    UBLOCK_YI_SYLLABLES =72,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_YI_SYLLABLES=72,

    /** */
    UBLOCK_YI_RADICALS =73,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_YI_RADICALS=73,

    /** */
    UBLOCK_HANGUL_SYLLABLES =74,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_HANGUL_SYLLABLES=74,

    /** */
    UBLOCK_HIGH_SURROGATES =75,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_HIGH_SURROGATES=75,

    /** */
    UBLOCK_HIGH_PRIVATE_USE_SURROGATES =76,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_HIGH_PRIVATE_USE_SURROGATES=76,

    /** */
    UBLOCK_LOW_SURROGATES =77,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_LOW_SURROGATES=77,

    /** */
    UBLOCK_PRIVATE_USE_AREA =78,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_PRIVATE_USE_AREA=78,

    /** */
    UBLOCK_CJK_COMPATIBILITY_IDEOGRAPHS =79,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CJK_COMPATIBILITY_IDEOGRAPHS=79,

    /** */
    UBLOCK_ALPHABETIC_PRESENTATION_FORMS =80,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ALPHABETIC_PRESENTATION_FORMS=80,

    /** */
    UBLOCK_ARABIC_PRESENTATION_FORMS_A =81,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ARABIC_PRESENTATION_FORMS_A=81,

    /** */
    UBLOCK_COMBINING_HALF_MARKS =82,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_COMBINING_HALF_MARKS=82,

    /** */
    UBLOCK_CJK_COMPATIBILITY_FORMS =83,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CJK_COMPATIBILITY_FORMS=83,

    /** */
    UBLOCK_SMALL_FORM_VARIANTS =84,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_SMALL_FORM_VARIANTS=84,

    /** */
    UBLOCK_ARABIC_PRESENTATION_FORMS_B =85,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_ARABIC_PRESENTATION_FORMS_B=85,

    /** */
    UBLOCK_SPECIALS =86,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_SPECIALS=86,

    /** */
    UBLOCK_HALFWIDTH_AND_FULLWIDTH_FORMS =87,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_HALFWIDTH_AND_FULLWIDTH_FORMS=87,

    /** */
    UBLOCK_COUNT=88,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_SCRIPT_COUNT=88,

    /** */
    UBLOCK_NO_SCRIPT=UBLOCK_COUNT,

    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_CHAR_SCRIPT_COUNT =UBLOCK_COUNT,
    /** @deprecated  Use the enum that begins with UBLOCK. Remove after Aug, 2002*/
    U_NO_SCRIPT = UBLOCK_COUNT

};

typedef enum UBlockCode UBlockCode;

/**
 * Values returned by the u_getCellWidth() function.
 * @draft
 */
enum UCellWidth
{
    /** */
    U_ZERO_WIDTH              = 0,
    /** */
    U_HALF_WIDTH              = 1,
    /** */
    U_FULL_WIDTH              = 2,
    /** */
    U_NEUTRAL_WIDTH           = 3,
    /** */
    U_CELL_WIDTH_COUNT
};

typedef enum UCellWidth UCellWidth;

/**
 * Selector constants for u_charName().
 * <code>u_charName() returns either the "modern" name of a
 * Unicode character or the name that was defined in
 * Unicode version 1.0, before the Unicode standard merged
 * with ISO-10646.
 *
 * @see u_charName()
 */
enum UCharNameChoice {
    U_UNICODE_CHAR_NAME,
    U_UNICODE_10_CHAR_NAME,
    U_CHAR_NAME_CHOICE_COUNT
};

typedef enum UCharNameChoice UCharNameChoice;

/**
 * Determines whether the specified UChar is a lowercase character
 * according to UnicodeData.txt.
 *
 * @param ch    the character to be tested
 * @return  true if the character is lowercase; false otherwise.
 * @see UNICODE_VERSION
 * @see u_isupper()
 * @see u_istitle()
 * @see u_islower()
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_islower(UChar32 c);

/**
 * Determines whether the specified character is an uppercase character
 * according to UnicodeData.txt.
 *
 * @param ch    the character to be tested
 * @return  true if the character is uppercase; false otherwise.
 * @see u_islower()
 * @see u_istitle
 * @see u_tolower()
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isupper(UChar32 c);

/**
 * Determines whether the specified character is a titlecase character
 * according to UnicodeData.txt.
 *
 * @param ch    the character to be tested
 * @return  true if the character is titlecase; false otherwise.
 * @see u_isupper()
 * @see u_islower()
 * @see u_totitle()
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_istitle(UChar32 c);

/**
 * Determines whether the specified character is a digit according to UnicodeData.txt.
 *
 *
 * @param ch    the character to be tested
 * @return  true if the character is a digit; false otherwise.
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isdigit(UChar32 c);

/**
 * Determines whether the specified character is an alphanumeric character
 * (letter or digit)according to UnicodeData.txt.
 *
 * @param ch    the character to be tested
 * @return  true if the character is a letter or a digit; false otherwise.
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isalnum(UChar32 c);

/**
 * Determines whether the specified numeric value is actually a defined character
 * according to UnicodeData.txt.
 *
 * @param ch    the character to be tested
 * @return  true if the character has a defined Unicode meaning; false otherwise.
 *
 * @see u_isdigit()
 * @see u_isalpha()
 * @see u_isalnum()
 * @see u_isupper()
 * @see u_islower()
 * @see u_istitle()
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isdefined(UChar32 c);

/**
 * Determines whether the specified character is a letter
 * according to UnicodeData.txt.
 *
 * @param ch    the character to be tested
 * @return  true if the character is a letter; false otherwise.
 *
 * @see u_isdigit()
 * @see u_isalnum()
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isalpha(UChar32 c);

/**
 * Determines if the specified character is a space character or not.
 *
 * @param ch    the character to be tested
 * @return  true if the character is a space character; false otherwise.
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isspace(UChar32 c);

/**
 * Determines if the specified character is white space according to ICU.
 * A character is considered to be an ICU whitespace character if and only
 * if it satisfies one of the following criteria:
 * <ul>
 * <li> It is a Unicode space separator (category "Zs"), but is not
 *      a no-break space (&#92;u00A0 or &#92;uFEFF).
 * <li> It is a Unicode line separator (category "Zl").
 * <li> It is a Unicode paragraph separator (category "Zp").
 * <li> It is &#92;u0009, HORIZONTAL TABULATION.
 * <li> It is &#92;u000A, LINE FEED.
 * <li> It is &#92;u000B, VERTICAL TABULATION.
 * <li> It is &#92;u000C, FORM FEED.
 * <li> It is &#92;u000D, CARRIAGE RETURN.
 * <li> It is &#92;u001C, FILE SEPARATOR.
 * <li> It is &#92;u001D, GROUP SEPARATOR.
 * <li> It is &#92;u001E, RECORD SEPARATOR.
 * <li> It is &#92;u001F, UNIT SEPARATOR.
 * </ul>
 * Note: This method corresponds to the Java method
 * <tt>java.lang.Character.isWhitespace()</tt>.
 *
 * @param   ch  the character to be tested.
 * @return  true if the character is an ICU whitespace character;
 *          false otherwise.
 * @see     #isspace
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isWhitespace(UChar32 c);

/**
 * Determines whether the specified character is a control character or not.
 * A control character is one of the following:
 * - ISO 8-bit control character (U+0000..U+001f and U+007f..U+009f)
 * - U_CONTROL_CHAR (Cc)
 * - U_FORMAT_CHAR (Cf)
 * - U_LINE_SEPARATOR (Zl)
 * - U_PARAGRAPH_SEPARATOR (Zp)
 *
 * @param ch    the character to be tested
 * @return  true if the Unicode character is a control character; false otherwise.
 *
 * @see u_isprint()
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_iscntrl(UChar32 c);


/**
 * Determines whether the specified character is a printable character according 
 * to UnicodeData.txt.
 *
 * @param ch    the character to be tested
 * @return  true if the Unicode character is a printable character; false otherwise.
 *
 * @see u_iscntrl()
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isprint(UChar32 c);

/**
 * Determines whether the specified character is of the base form according 
 * to UnicodeData.txt.
 *
 * @param ch    the character to be tested
 * @return  true if the Unicode character is of the base form; false otherwise.
 *
 * @see u_isalpha()
 * @see u_isdigit()
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isbase(UChar32 c);

/**
  * Returns the linguistic direction property of a character.
  * <P>
  * Returns the linguistic direction property of a character.
  * For example, 0x0041 (letter A) has the LEFT_TO_RIGHT directional 
  * property.
  * @see UCharDirection
  * @draft
  */
U_CAPI UCharDirection U_EXPORT2
u_charDirection(UChar32 c);

/**
 * Determines whether the character has the "mirrored" property.
 * This property is set for characters that are commonly used in
 * Right-To-Left contexts and need to be displayed with a "mirrored"
 * glyph.
 *
 * @param c the character (code point, Unicode scalar value) to be tested
 * @return TRUE if the character has the "mirrored" property
 */
U_CAPI UBool U_EXPORT2
u_isMirrored(UChar32 c);

/**
 * Maps the specified character to a "mirror-image" character.
 * For characters with the "mirrored" property, implementations
 * sometimes need a "poor man's" mapping to another Unicode
 * character (code point) such that the default glyph may serve
 * as the mirror-image of the default glyph of the specified
 * character. This is useful for text conversion to and from
 * codepages with visual order, and for displays without glyph
 * selecetion capabilities.
 *
 * @param c the character (code point, Unicode scalar value) to be mapped
 * @return another Unicode code point that may serve as a mirror-image
 *         substitute, or c itself if there is no such mapping or c
 *         does not have the "mirrored" property
 */
U_CAPI UChar32 U_EXPORT2
u_charMirror(UChar32 c);

/**
 * Returns a value indicating the display-cell width of the character
 * when used in Asian text, according to the Unicode standard (see p. 6-130
 * of The Unicode Standard, Version 2.0).  The results for various characters
 * are as follows:
 * <P>
 *      ZERO_WIDTH: Characters which are considered to take up no display-cell space:
 *          control characters
 *          format characters
 *          line and paragraph separators
 *          non-spacing marks
 *          combining Hangul jungseong
 *          combining Hangul jongseong
 *          unassigned Unicode values
 * <P>
 *      HALF_WIDTH: Characters which take up half a cell in standard Asian text:
 *          all characters in the General Scripts Area except combining Hangul choseong
 *              and the characters called out specifically above as ZERO_WIDTH
 *          alphabetic and Arabic presentation forms
 *          halfwidth CJK punctuation
 *          halfwidth Katakana
 *          halfwidth Hangul Jamo
 *          halfwidth forms, arrows, and shapes
 * <P>
 *      FULL_WIDTH:  Characters which take up a full cell in standard Asian text:
 *          combining Hangul choseong
 *          all characters in the CJK Phonetics and Symbols Area
 *          all characters in the CJK Ideographs Area
 *          all characters in the Hangul Syllables Area
 *          CJK compatibility ideographs
 *          CJK compatibility forms
 *          small form variants
 *          fullwidth ASCII
 *          fullwidth punctuation and currency signs
 * <P>
 *      NEUTRAL:  Characters whose cell width is context-dependent:
 *          all characters in the Symbols Area, except those specifically called out above
 *          all characters in the Surrogates Area
 *          all charcaters in the Private Use Area
 * <P>
 * For Korean text, this algorithm should work properly with properly normalized Korean
 * text.  Precomposed Hangul syllables and non-combining jamo are all considered full-
 * width characters.  For combining jamo, we treat we treat choseong (initial consonants)
 * as double-width characters and junseong (vowels) and jongseong (final consonants)
 * as non-spacing marks.  This will work right in text that uses the precomposed
 * choseong characters instead of teo choseong characters in a row, and which uses the
 * choseong filler character at the beginning of syllables that don't have an initial
 * consonant.  The results may be slightly off with Korean text following different
 * conventions.
 * @draft
 */
U_CAPI uint16_t U_EXPORT2
u_charCellWidth(UChar32 c);

/**
 * Returns a value indicating a character category according to UnicodeData.txt.
 *
 * @param c            the character to be tested
 * @return a value of type int, the character category.
 * @see UCharCategory
 * @draft
 */
U_CAPI int8_t U_EXPORT2
u_charType(UChar32 c);

/**
 * Returns the combining class of the code point as specified in UnicodeData.txt.
 *
 * @param c the code point of the character
 * @return the combining class of the character
 * @draft
 */
U_CAPI uint8_t U_EXPORT2
u_getCombiningClass(UChar32 c);

/**
 * Retrives the decimal numeric value of a digit character.
 *
 * @param c the digit character for which to get the numeric value
 * @return the numeric value of ch in decimal radix.  This method returns
 * -1 if ch is not a valid digit character.
 * @draft
 */
U_CAPI int32_t U_EXPORT2
u_charDigitValue(UChar32 c);

/**
 * Returns the Unicode allocation block that contains the character.
 *
 * @see #UCharBlock
 * @draft
 */
U_CAPI UBlockCode U_EXPORT2
ublock_getCode(UChar32    ch);

/**
 * Retrieve the name of a Unicode character.
 * Depending on <code>nameChoice</code>, the character name written
 * into the buffer is the "modern" name or the name that was defined
 * in Unicode version 1.0.
 * The name contains only "invariant" characters
 * like A-Z, 0-9, space, and '-'.
 * Unicode 1.0 names are only retrieved if they are different from the modern
 * names and if the data file contains the data for them. gennames may or may
 * not be called with a command line option to include 1.0 names in unames.dat.
 *
 * @param code The character (code point) for which to get the name.
 *             It must be <code>0<=code<0x10ffff</code>.
 * @param nameChoice Selector for which name to get.
 * @param buffer Destination address for copying the name.
 *               The name will always be zero-terminated.
 *               If there is no name, then the buffer will be set to the empty string.
 * @param bufferLength <code>==sizeof(buffer)</code>
 * @param pErrorCode Pointer to a UErrorCode variable;
 *        check for <code>U_SUCCESS()</code> after <code>u_charName()</code>
 *        returns.
 * @return The length of the name, or 0 if there is no name for this character.
 *         If the bufferLength is less than or equal to the length, then the buffer
 *         contains the truncated name and the returned length indicates the full
 *         length of the name.
 *         The length does not include the zero-termination.
 *
 * @see UCharNameChoice
 * @see u_charFromName
 * @see u_enumCharNames
 * @draft
 */
U_CAPI UTextOffset U_EXPORT2
u_charName(UChar32 code, UCharNameChoice nameChoice,
           char *buffer, UTextOffset bufferLength,
           UErrorCode *pErrorCode);

/**
 * Find a Unicode character by its name and return its code point value.
 * The name is matched exactly and completely.
 * A Unicode 1.0 name is matched only if it differs from the modern name.
 * Unicode names are all uppercase.
 *
 * @param nameChoice Selector for which name to match.
 * @param name The name to match.
 * @param pErrorCode Pointer to a UErrorCode variable
 * @return The Unicode code point value of the character with the given name,
 *         or 0xffff if there is no such character.
 *
 * @see UCharNameChoice
 * @see u_charName
 * @see u_enumCharNames
 */
U_CAPI UChar32 U_EXPORT2
u_charFromName(UCharNameChoice nameChoice,
               const char *name,
               UErrorCode *pErrorCode);

U_CDECL_BEGIN

/**
 * Type of a callback function for u_enumCharNames() that gets called
 * for each Unicode character with the code point value and
 * the character name.
 * If such a function returns FALSE, then the enumeration is stopped.
 *
 * @param context The context pointer that was passed to u_enumCharNames().
 * @param code The Unicode code point for the character with this name.
 * @param nameChoice Selector for which kind of names is enumerated.
 * @param name The character's name, zero-terminated.
 * @param length The length of the name.
 * @return TRUE if the enumeration should continue, FALSE to stop it.
 *
 * @see UCharNameChoice
 * @see u_enumCharNames
 */
typedef UBool UEnumCharNamesFn(void *context,
                               UChar32 code,
                               UCharNameChoice nameChoice,
                               const char *name,
                               UTextOffset length);

U_CDECL_END

/**
 * Enumerate all assigned Unicode characters between the start and limit
 * code points (start inclusive, limit exclusive) and call a function
 * for each, passing the code point value and the character name.
 * For Unicode 1.0 names, only those are enumerated that differ from the
 * modern names.
 *
 * @param start The first code point in the enumeration range.
 * @param limit One more than the last code point in the enumeration range
 *              (the first one after the range).
 * @param fn The function that is to be called for each character name.
 * @param context An arbitrary pointer that is passed to the function.
 * @param nameChoice Selector for which kind of names to enumerate.
 * @param pErrorCode Pointer to a UErrorCode variable
 *
 * @see UCharNameChoice
 * @see UEnumCharNamesFn
 * @see u_charName
 * @see u_charFromName
 */
U_CAPI void U_EXPORT2
u_enumCharNames(UChar32 start, UChar32 limit,
                UEnumCharNamesFn *fn,
                void *context,
                UCharNameChoice nameChoice,
                UErrorCode *pErrorCode);

/** 
 * The following functions are java specific.
 */
/**
  * A convenience method for determining if a Unicode character 
  * is allowed to start in a Unicode identifier.
  * A character may start a Unicode identifier if and only if
  * it is a letter.
  *
  * @param   c  the Unicode character.
  * @return  TRUE if the character may start a Unicode identifier;
  *          FALSE otherwise.
  * @see     u_isalpha
  * @see     u_isIDPart
  * @draft
  */
U_CAPI UBool U_EXPORT2
u_isIDStart(UChar32 c);
/**
  * A convenience method for determining if a Unicode character
  * may be part of a Unicode identifier other than the starting
  * character.
  * <P>
  * A character may be part of a Unicode identifier if and only if
  * it is one of the following:
  * <ul>
  * <li>  a letter
  * <li>  a connecting punctuation character (such as "_").
  * <li>  a digit
  * <li>  a numeric letter (such as a Roman numeral character)
  * <li>  a combining mark
  * <li>  a non-spacing mark
  * <li>  an ignorable control character
  * </ul>
  * 
  * @param   c  the Unicode character.
  * @return  TRUE if the character may be part of a Unicode identifier;
  *          FALSE otherwise.
  * @see     u_isIDIgnorable
  * @see     u_isIDStart
  * @draft
  */
U_CAPI UBool U_EXPORT2
u_isIDPart(UChar32 c);
/**
  * A convenience method for determining if a Unicode character 
  * should be regarded as an ignorable character 
  * in a Unicode identifier.
  * <P>
  * The following Unicode characters are ignorable in a 
  * Unicode identifier:
  * <table>
  * <tr><td>0x0000 through 0x0008,</td>
  *                                 <td>ISO control characters that</td></tr>
  * <tr><td>0x000E through 0x001B,</td> <td>are not whitespace</td></tr>
  * <tr><td>and 0x007F through 0x009F</td></tr>
  * <tr><td>0x200C through 0x200F</td>  <td>join controls</td></tr>
  * <tr><td>0x200A through 0x200E</td>  <td>bidirectional controls</td></tr>
  * <tr><td>0x206A through 0x206F</td>  <td>format controls</td></tr>
  * <tr><td>0xFEFF</td>               <td>zero-width no-break space</td></tr>
  * </table>
  * 
  * @param   c  the Unicode character.
  * @return  TRUE if the character may be part of a Unicode identifier;
  *          FALSE otherwise.
  * @see     u_isIDPart
  * @draft
  */
U_CAPI UBool U_EXPORT2
u_isIDIgnorable(UChar32 c);
    /**
     * A convenience method for determining if a Unicode character
     * is allowed as the first character in a Java identifier.
     * <P>
     * A character may start a Java identifier if and only if
     * it is one of the following:
     * <ul>
     * <li>  a letter
     * <li>  a currency symbol (such as "$")
     * <li>  a connecting punctuation symbol (such as "_").
     * </ul>
     *
     * @param   c  the Unicode character.
     * @return  TRUE if the character may start a Java identifier;
     *          FALSE otherwise.
     * @see     u_isJavaIDPart
     * @see     u_isalpha
     * @see     u_isIDStart
     * @draft
     */
U_CAPI UBool U_EXPORT2
u_isJavaIDStart(UChar32 c);
    /**
     * A convenience method for determining if a Unicode character 
     * may be part of a Java identifier other than the starting
     * character.
     * <P>
     * A character may be part of a Java identifier if and only if
     * it is one of the following:
     * <ul>
     * <li>  a letter
     * <li>  a currency symbol (such as "$")
     * <li>  a connecting punctuation character (such as "_").
     * <li>  a digit
     * <li>  a numeric letter (such as a Roman numeral character)
     * <li>  a combining mark
     * <li>  a non-spacing mark
     * <li>  an ignorable control character
     * </ul>
     * 
     * @param   c the Unicode character.
     * @return  TRUE if the character may be part of a Unicode identifier; 
     *          FALSE otherwise.
     * @see     u_isIDIgnorable
     * @see     u_isJavaIDStart
     * @see     u_isalpha
     * @see     u_isdigit
     * @see     u_isIDPart
     * @draft
     */

U_CAPI UBool U_EXPORT2
u_isJavaIDPart(UChar32 c);

/**
 * Functions to change character case.
 */

/**
 * The given character is mapped to its lowercase equivalent according to
 * UnicodeData.txt; if the character has no lowercase equivalent, the character 
 * itself is returned.
 * <P>
 * A character has a lowercase equivalent if and only if a lowercase mapping
 * is specified for the character in the UnicodeData.txt attribute table.
 * <P>
 * u_tolower() only deals with the general letter case conversion.
 * For language specific case conversion behavior, use ustrToUpper().
 * For example, the case conversion for dot-less i and dotted I in Turkish,
 * or for final sigma in Greek.
 *
 * @param ch    the character to be converted
 * @return  the lowercase equivalent of the character, if any;
 *      otherwise the character itself.
 * @draft
 */
U_CAPI UChar32 U_EXPORT2
u_tolower(UChar32 c);

/**
 * The given character is mapped to its uppercase equivalent according to UnicodeData.txt;
 * if the character has no uppercase equivalent, the character itself is 
 * returned.
 * <P>
 * u_toupper() only deals with the general letter case conversion.
 * For language specific case conversion behavior, use ustrToUpper().
 * For example, the case conversion for dot-less i and dotted I in Turkish,
 * or ess-zed (i.e., "sharp S") in German.
 *
 * @param ch    the character to be converted
 * @return  the uppercase equivalent of the character, if any;
 *      otherwise the character itself.
 * @draft
 */
U_CAPI UChar32 U_EXPORT2
u_toupper(UChar32 c);
/**
 * The given character is mapped to its titlecase equivalent according to UnicodeData.txt.
 * There are only four Unicode characters that are truly titlecase forms
 * that are distinct from uppercase forms.  As a rule, if a character has no
 * true titlecase equivalent, its uppercase equivalent is returned.
 * <P>
 * A character has a titlecase equivalent if and only if a titlecase mapping
 * is specified for the character in the UnicodeData.txt data.
 *
 * @param ch    the character to be converted
 * @return  the titlecase equivalent of the character, if any;
 *      otherwise the character itself.
 * @draft
 */
U_CAPI UChar32 U_EXPORT2
u_totitle(UChar32 c);

/** Option value for case folding: use all mappings defined in CaseFolding.txt. */
#define U_FOLD_CASE_DEFAULT 0
/** Option value for case folding: exclude the mappings for dotted I and dotless i marked with 'I' in CaseFolding.txt. */
#define U_FOLD_CASE_EXCLUDE_SPECIAL_I 1

/**
 * The given character is mapped to its case folding equivalent according to
 * UnicodeData.txt and CaseFolding.txt; if the character has no case folding equivalent, the character 
 * itself is returned.
 * Only "simple", single-code point case folding mappings are used.
 * "Full" mappings are used by u_strFoldCase().
 *
 * @param c     the character to be converted
 * @param options Either U_FOLD_CASE_DEFAULT or U_FOLD_CASE_EXCLUDE_SPECIAL_I
 * @return      the case folding equivalent of the character, if any;
 *              otherwise the character itself.
 * @draft
 */
U_CAPI UChar32 U_EXPORT2
u_foldCase(UChar32 c, uint32_t options);

/**
 * Returns the numeric value of the character <code>ch</code> in the 
 * specified radix. 
 * <p>
 * If the radix is not in the range <code>2 <= radix <= 36</code> or if the 
 * value of <code>ch</code> is not a valid digit in the specified 
 * radix, <code>-1</code> is returned. A character is a valid digit 
 * if at least one of the following is true:
 * <ul>
 * <li>The method <code>u_isdigit</code> is true of the character 
 *     and the Unicode decimal digit value of the character (or its 
 *     single-character decomposition) is less than the specified radix. 
 *     In this case the decimal digit value is returned. 
 * <li>The character is one of the uppercase Latin letters 
 *     <code>'A'</code> through <code>'Z'</code> and its code is less than
 *     <code>radix + 'A' - 10</code>. 
 *     In this case, <code>ch - 'A' + 10</code> 
 *     is returned. 
 * <li>The character is one of the lowercase Latin letters 
 *     <code>'a'</code> through <code>'z'</code> and its code is less than
 *     <code>radix + 'a' - 10</code>. 
 *     In this case, <code>ch - 'a' + 10</code> 
 *     is returned. 
 * </ul>
 *
 * @param   ch      the character to be converted.
 * @param   radix   the radix.
 * @return  the numeric value represented by the character in the
 *          specified radix.
 *
 * @see     u_forDigit
 * @see     u_charDigitValue
 * @see     u_isdigit
 * @draft
 */
U_CAPI int8_t U_EXPORT2
u_digit(UChar32 ch, int8_t radix);

/**
 * Determines the character representation for a specific digit in 
 * the specified radix. If the value of <code>radix</code> is not a 
 * valid radix, or the value of <code>digit</code> is not a valid 
 * digit in the specified radix, the null character
 * (<code>U+0000</code>) is returned. 
 * <p>
 * The <code>radix</code> argument is valid if it is greater than or 
 * equal to 2 and less than or equal to 36.
 * The <code>digit</code> argument is valid if
 * <code>0 <= digit < radix</code>. 
 * <p>
 * If the digit is less than 10, then 
 * <code>'0' + digit</code> is returned. Otherwise, the value 
 * <code>'a' + digit - 10</code> is returned. 
 *
 * @param   digit   the number to convert to a character.
 * @param   radix   the radix.
 * @return  the <code>char</code> representation of the specified digit
 *          in the specified radix. 
 *
 * @see     u_digit
 * @see     u_charDigitValue
 * @see     u_isdigit
 * @draft
 */
U_CAPI UChar32 U_EXPORT2
u_forDigit(int32_t digit, int8_t radix);

/**
 * Gets the Unicode version information.  The version array stores the version information
 * for the Unicode standard that is currently used by ICU.  For example, release "1.3.31.2" 
 * is then represented as 0x01031F02.
 * @param versionArray the version # information, the result will be filled in
 * @stable
 */
U_CAPI void U_EXPORT2
u_getUnicodeVersion(UVersionInfo info);


/**
 *@deprecated Use u_charBlock instead. Remove after Aug,2002
 */
#define u_charScript ublock_getCode
/** @deprecated  Use the enum UCharBlock instead. Remove after Aug,2002*/
typedef UBlockCode UCharScript;

#endif /*_UCHAR*/
/*eof*/
