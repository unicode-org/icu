/*
**********************************************************************
*   Copyright (C) 1997-2000, International Business Machines
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
********************************************************************************
*/

#ifndef UCHAR_H
#define UCHAR_H

#include "unicode/utypes.h"
/*===========================================================================*/
/* Unicode version number                                                    */
/*===========================================================================*/
#define U_UNICODE_VERSION "3.0.0"

/**
 * @name The Unicode C API allows you to query the properties associated with individual 
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
	/** */
	U_UNASSIGNED              = 0,
	/** */
    U_UPPERCASE_LETTER        = 1,
	/** */
    U_LOWERCASE_LETTER        = 2,
	/** */
    U_TITLECASE_LETTER        = 3,
	/** */
    U_MODIFIER_LETTER         = 4,
	/** */
    U_OTHER_LETTER            = 5,
	/** */
    U_NON_SPACING_MARK        = 6,
	/** */
    U_ENCLOSING_MARK          = 7,
	/** */
    U_COMBINING_SPACING_MARK  = 8,
	/** */
    U_DECIMAL_DIGIT_NUMBER    = 9,
	/** */
    U_LETTER_NUMBER           = 10,
	/** */
    U_OTHER_NUMBER            = 11,
	/** */
    U_SPACE_SEPARATOR         = 12,
	/** */
    U_LINE_SEPARATOR          = 13,
	/** */
    U_PARAGRAPH_SEPARATOR     = 14,
	/** */
    U_CONTROL_CHAR            = 15,
	/** */
    U_FORMAT_CHAR             = 16,
	/** */
    U_PRIVATE_USE_CHAR        = 17,
	/** */
    U_SURROGATE               = 18,
	/** */
    U_DASH_PUNCTUATION        = 19,
	/** */
    U_START_PUNCTUATION       = 20,
	/** */
    U_END_PUNCTUATION         = 21,
	/** */
    U_CONNECTOR_PUNCTUATION   = 22,
	/** */
    U_OTHER_PUNCTUATION       = 23,
	/** */
    U_MATH_SYMBOL             = 24,
	/** */
    U_CURRENCY_SYMBOL         = 25,
	/** */
    U_MODIFIER_SYMBOL         = 26,
	/** */
    U_OTHER_SYMBOL            = 27,
	/** */
    U_INITIAL_PUNCTUATION     = 28,
	/** */
    U_FINAL_PUNCTUATION       = 29,
	/** */
    U_GENERAL_OTHER_TYPES     = 30,
	/** */
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

/** Generated from Unicode Data files 
 *  @draft
 */
enum UCharScript {
/* Script names */
	/** */
    U_BASIC_LATIN,
	/** */
    U_LATIN_1_SUPPLEMENT,
	/** */
    U_LATIN_EXTENDED_A,
	/** */
    U_LATIN_EXTENDED_B,
	/** */
    U_IPA_EXTENSIONS,
	/** */
    U_SPACING_MODIFIER_LETTERS,
	/** */
    U_COMBINING_DIACRITICAL_MARKS,
	/** */
    U_GREEK,
	/** */
    U_CYRILLIC,
	/** */
    U_ARMENIAN,
	/** */
    U_HEBREW,
	/** */
    U_ARABIC,
	/** */
    U_SYRIAC,
	/** */
    U_THAANA,
	/** */
    U_DEVANAGARI,
	/** */
    U_BENGALI,
	/** */
    U_GURMUKHI,
	/** */
    U_GUJARATI,
	/** */
    U_ORIYA,
	/** */
    U_TAMIL,
	/** */
    U_TELUGU,
	/** */
    U_KANNADA,
	/** */
    U_MALAYALAM,
	/** */
    U_SINHALA,
	/** */
    U_THAI,
	/** */
    U_LAO,
	/** */
    U_TIBETAN,
	/** */
    U_MYANMAR,
	/** */
    U_GEORGIAN,
	/** */
    U_HANGUL_JAMO,
	/** */
    U_ETHIOPIC,
	/** */
    U_CHEROKEE,
	/** */
    U_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS,
	/** */
    U_OGHAM,
	/** */
    U_RUNIC,
	/** */
    U_KHMER,
	/** */
    U_MONGOLIAN,
	/** */
    U_LATIN_EXTENDED_ADDITIONAL,
	/** */
    U_GREEK_EXTENDED,
	/** */
    U_GENERAL_PUNCTUATION,
	/** */
    U_SUPERSCRIPTS_AND_SUBSCRIPTS,
	/** */
    U_CURRENCY_SYMBOLS,
	/** */
    U_COMBINING_MARKS_FOR_SYMBOLS,
	/** */
    U_LETTERLIKE_SYMBOLS,
	/** */
    U_NUMBER_FORMS,
	/** */
    U_ARROWS,
	/** */
    U_MATHEMATICAL_OPERATORS,
	/** */
    U_MISCELLANEOUS_TECHNICAL,
	/** */
    U_CONTROL_PICTURES,
	/** */
    U_OPTICAL_CHARACTER_RECOGNITION,
	/** */
    U_ENCLOSED_ALPHANUMERICS,
	/** */
    U_BOX_DRAWING,
	/** */
    U_BLOCK_ELEMENTS,
	/** */
    U_GEOMETRIC_SHAPES,
	/** */
    U_MISCELLANEOUS_SYMBOLS,
	/** */
    U_DINGBATS,
	/** */
    U_BRAILLE_PATTERNS,
	/** */
    U_CJK_RADICALS_SUPPLEMENT,
	/** */
    U_KANGXI_RADICALS,
	/** */
    U_IDEOGRAPHIC_DESCRIPTION_CHARACTERS,
	/** */
    U_CJK_SYMBOLS_AND_PUNCTUATION,
	/** */
    U_HIRAGANA,
	/** */
    U_KATAKANA,
	/** */
    U_BOPOMOFO,
	/** */
    U_HANGUL_COMPATIBILITY_JAMO,
	/** */
    U_KANBUN,
	/** */
    U_BOPOMOFO_EXTENDED,
	/** */
    U_ENCLOSED_CJK_LETTERS_AND_MONTHS,
	/** */
    U_CJK_COMPATIBILITY,
	/** */
    U_CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
	/** */
    U_CJK_UNIFIED_IDEOGRAPHS,
	/** */
    U_YI_SYLLABLES,
	/** */
    U_YI_RADICALS,
	/** */
    U_HANGUL_SYLLABLES,
	/** */
    U_HIGH_SURROGATES,
	/** */
    U_HIGH_PRIVATE_USE_SURROGATES,
	/** */
    U_LOW_SURROGATES,
	/** */
    U_PRIVATE_USE_AREA,
	/** */
    U_CJK_COMPATIBILITY_IDEOGRAPHS,
	/** */
    U_ALPHABETIC_PRESENTATION_FORMS,
	/** */
    U_ARABIC_PRESENTATION_FORMS_A,
	/** */
    U_COMBINING_HALF_MARKS,
	/** */
    U_CJK_COMPATIBILITY_FORMS,
	/** */
    U_SMALL_FORM_VARIANTS,
	/** */
    U_ARABIC_PRESENTATION_FORMS_B,
	/** */
    U_SPECIALS,
	/** */
    U_HALFWIDTH_AND_FULLWIDTH_FORMS,
	/** */
    U_CHAR_SCRIPT_COUNT,
	/** */
    U_NO_SCRIPT=U_CHAR_SCRIPT_COUNT
};
typedef enum UCharScript UCharScript;

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
 * Functions to classify characters.
 */

/**
 * Determines whether the specified UChar is a lowercase character
 * according to Unicode 2.1.2.
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
 * according to Unicode 2.1.2.
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
 * according to Unicode 2.1.2.
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
 * Determines whether the specified character is a digit according to Unicode
 * 2.1.2.
 *
 * @param ch    the character to be tested
 * @return  true if the character is a digit; false otherwise.
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isdigit(UChar32 c);

/**
 * Determines whether the specified character is an alphanumeric character
 * (letter or digit)according to Unicode 2.1.2.
 *
 * @param ch    the character to be tested
 * @return  true if the character is a letter or a digit; false otherwise.
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isalnum(UChar32 c);

/**
 * Determines whether the specified numeric value is actually a defined character
 * according to Unicode 2.1.2.
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
 * according to Unicode 2.1.2.
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
 * @param   ch	the character to be tested.
 * @return  true if the character is an ICU whitespace character;
 *          false otherwise.
 * @see     #isspace
 * @draft
 */
U_CAPI UBool U_EXPORT2
u_isWhitespace(UChar32 c);

/**
 * Determines whether the specified character is a control character or not.
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
 * to Unicode 2.1.2.
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
 * to Unicode 2.1.2.
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
 * Returns a value indicating a character category according to Unicode
 * 2.1.2.
 * @param c            the character to be tested
 * @return a value of type int, the character category.
 * @see UCharCategory
 * @draft
 */
U_CAPI int8_t U_EXPORT2
u_charType(UChar32 c);

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
 * Returns the script associated with a character.
 *
 * @see #UCharScript
 * @draft
 */
U_CAPI UCharScript     U_EXPORT2
u_charScript(UChar32    ch);

/**
 * Retrieve the name of a Unicode character.
 * Depending on <code>nameChoice</code>, the character name written
 * into the buffer is the "modern" name or the name that was defined
 * in Unicode version 1.0.
 * The name contains only "invariant" characters
 * like A-Z, 0-9, space, and '-'.
 *
 * @param code The character (code point) for which to get the name.
 *             It must be <code>0&lt;=code&lt;0x10ffff</code>.
 * @param nameChoice Selector for which name to get.
 * @param buffer Destination address for copying the name.
 * @param bufferLength <code>==sizeof(buffer)</code>
 * @param pErrorCode Pointer to a UErrorCode variable;
 *        check for <code>U_SUCCESS()</code> after <code>u_charName()</code>
 *        returns.
 *
 * @see UCharNameChoice
 * @draft
 */
U_CAPI UTextOffset U_EXPORT2
u_charName(uint32_t code, UCharNameChoice nameChoice,
           char *buffer, UTextOffset bufferLength,
           UErrorCode *pErrorCode);

/**
 * Find a Unicode character by its name and return its code point value.
 * ### TBD
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
 * ### TBD
 */
typedef UBool UEnumCharNamesFn(void *context,
                               UChar32 code,
                               UCharNameChoice nameChoice,
                               char *name,
                               UTextOffset length);

U_CDECL_END

/**
 * Enumerate all assigned Unicode characters between the start and limit
 * code points (start inclusive, limit exclusive) and call a function
 * for each, passing the code point value and the character name.
 * ### TBD
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
 * Unicode 2.1.2; if the character has no lowercase equivalent, the character 
 * itself is returned.
 * <P>
 * A character has a lowercase equivalent if and only if a lowercase mapping
 * is specified for the character in the Unicode 2.1.2 attribute table.
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
 * The given character is mapped to its uppercase equivalent according to Unicode
 * 2.1.2; if the character has no uppercase equivalent, the character itself is 
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
 * The given character is mapped to its titlecase equivalent according to Unicode
 * 2.1.2.  There are only four Unicode characters that are truly titlecase forms
 * that are distinct from uppercase forms.  As a rule, if a character has no
 * true titlecase equivalent, its uppercase equivalent is returned.
 * <P>
 * A character has a titlecase equivalent if and only if a titlecase mapping
 * is specified for the character in the Unicode 2.1.2 data.
 *
 * @param ch    the character to be converted
 * @return  the titlecase equivalent of the character, if any;
 *      otherwise the character itself.
 * @draft
 */
U_CAPI UChar32 U_EXPORT2
u_totitle(UChar32 c);

/**
 * Gets the Unicode version information.  The version array stores the version information
 * for the Unicode standard that is currently used by ICU.  For example, release "1.3.31.2" 
 * is then represented as 0x01031F02.
 * @param versionArray the version # information, the result will be filled in
 * @stable
 */
U_CAPI void U_EXPORT2
u_getUnicodeVersion(UVersionInfo info);

#endif /*_UCHAR*/
/*eof*/
