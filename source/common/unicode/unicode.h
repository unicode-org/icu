/*
*****************************************************************************************
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*/
//  FILE NAME : unicode.h
//
//  CREATED
//      Wednesday, December 11, 1996
//
//  CREATED BY
//      Helena Shih
//
//  CHANGES
//      Thursday, April 15, 1999
//      Modified the definitions of all the functions
//      C++ Wrappers for Unicode
//  CHANGES BY
//      Madhu Katragadda
//   5/20/99     Madhu		Added the function getVersion()
//  11/22/99     aliu       Added MIN_RADIX, MAX_RADIX, digit, forDigit
//********************************************************************************************
   
         

#ifndef UNICODE_H
#define UNICODE_H

#include "unicode/utypes.h"
#include "unicode/uchar.h"

/**
 * The Unicode class allows you to query the properties associated with individual 
 * Unicode character values.  
 * <p>
 * The Unicode character information, provided implicitly by the 
 * <a href="http://www.unicode.org/">Unicode Standard</a>,
 * includes information about the sript
 * (for example, symbols or control characters) to which the character belongs,
 * as well as semantic information such as whether a character is a digit or 
 * uppercase, lowercase, or uncased.
 * <P>
 * @subclassing Do not subclass.
 */
class U_COMMON_API Unicode
{
public:
    /*
     * In C++, static const members actually take up memory and need to be accessed.
     * enum values are more like C #define's.
     * The following is a collection of constants, not an enumeration type.
     */
    enum {
        /** The lowest Unicode code point value. Code points are non-negative. */
        MIN_VALUE=0,

        /**
         * The highest Unicode code point value (scalar value) according to
         * The Unicode Standard. This is a 21-bit value (20.1 bits, rounded up).
         * For a single character, UChar32 is a simple type that can hold any code point value.
         */
        MAX_VALUE=0x10ffff,

        /**
         * The maximum number of code units (UChar's) per code point (UChar32).
         * This depends on the default UTF that the ICU library is compiled for.
         * Currently, the only natively supported UTF is UTF-16, which means that
         * UChar is 16 bits wide and this value is 2 (for surrogate pairs).
         * This may change in the future.
         */
        MAX_CHAR_LENGTH=UTF_MAX_CHAR_LENGTH,

        /**
         * The minimum radix available for conversion to and from Strings.  
         * The constant value of this field is the smallest value permitted 
         * for the radix argument in radix-conversion methods such as the 
         * <code>digit</code> method and the <code>forDigit</code>
         * method. 
         *
         * @see     Unicode#digit
         * @see     Unicode#forDigit
         */
        MIN_RADIX=2,

        /**
         * The maximum radix available for conversion to and from Strings.
         * The constant value of this field is the largest value permitted 
         * for the radix argument in radix-conversion methods such as the 
         * <code>digit</code> method and the <code>forDigit</code>
         * method.
         *
         * @see     Unicode#digit
         * @see     Unicode#forDigit
         */
        MAX_RADIX=36
    };

    /**
     * Public data for enumerated Unicode general category types
     */

    enum EUnicodeGeneralTypes
    {
        UNASSIGNED              = 0,
        UPPERCASE_LETTER        = 1,
        LOWERCASE_LETTER        = 2,
        TITLECASE_LETTER        = 3,
        MODIFIER_LETTER         = 4,
        OTHER_LETTER            = 5,
        NON_SPACING_MARK        = 6,
        ENCLOSING_MARK          = 7,
        COMBINING_SPACING_MARK  = 8,
        DECIMAL_DIGIT_NUMBER    = 9,
        LETTER_NUMBER           = 10,
        OTHER_NUMBER            = 11,
        SPACE_SEPARATOR         = 12,
        LINE_SEPARATOR          = 13,
        PARAGRAPH_SEPARATOR     = 14,
        CONTROL                 = 15,
        FORMAT                  = 16,
        PRIVATE_USE             = 17,
        SURROGATE               = 18,
        DASH_PUNCTUATION        = 19,
        START_PUNCTUATION       = 20,
        END_PUNCTUATION         = 21,
		CONNECTOR_PUNCTUATION   = 22,
        OTHER_PUNCTUATION       = 23,
        MATH_SYMBOL             = 24,
        CURRENCY_SYMBOL         = 25,
        MODIFIER_SYMBOL         = 26,
        OTHER_SYMBOL            = 27,
		INITIAL_PUNCTUATION     = 28,
		FINAL_PUNCTUATION       = 29,
        GENERAL_TYPES_COUNT     = 30
    };

    enum EUnicodeScript 
    {
        kBasicLatin,
        kLatin1Supplement,
        kLatinExtendedA,
        kLatinExtendedB,
        kIPAExtension,
        kSpacingModifier,
        kCombiningDiacritical,
        kGreek,
        kCyrillic,
        kArmenian,
        kHebrew,
        kArabic,
        kDevanagari,
        kBengali,
        kGurmukhi,
        kGujarati,
        kOriya,
        kTamil,
        kTelugu,
        kKannada,
        kMalayalam,
        kThai,
        kLao,
        kTibetan,
        kGeorgian,
        kHangulJamo,
        kLatinExtendedAdditional,
        kGreekExtended,
        kGeneralPunctuation,
        kSuperSubScript,
        kCurrencySymbolScript,
        kSymbolCombiningMark,
        kLetterlikeSymbol,
        kNumberForm,
        kArrow,
        kMathOperator,
        kMiscTechnical,
        kControlPicture,
        kOpticalCharacter,
        kEnclosedAlphanumeric,
        kBoxDrawing,
        kBlockElement,
        kGeometricShape,
        kMiscSymbol,
        kDingbat,
        kCJKSymbolPunctuation,
        kHiragana,
        kKatakana,
        kBopomofo,
        kHangulCompatibilityJamo,
        kKanbun,
        kEnclosedCJKLetterMonth,
        kCJKCompatibility,
        kCJKUnifiedIdeograph,
        kHangulSyllable,
        kHighSurrogate,
        kHighPrivateUseSurrogate,
        kLowSurrogate,
        kPrivateUse,
        kCJKCompatibilityIdeograph,
        kAlphabeticPresentation,
        kArabicPresentationA,
        kCombiningHalfMark,
        kCJKCompatibilityForm,
        kSmallFormVariant,
        kArabicPresentationB,
        kNoScript,
        kHalfwidthFullwidthForm,
        kScriptCount
    };

    /**
     * This specifies the language directional property of a character set.
     */
    enum EDirectionProperty { 
        LEFT_TO_RIGHT               = 0, 
		RIGHT_TO_LEFT               = 1, 
		EUROPEAN_NUMBER             = 2,
		EUROPEAN_NUMBER_SEPARATOR   = 3,
		EUROPEAN_NUMBER_TERMINATOR  = 4,
		ARABIC_NUMBER               = 5,
		COMMON_NUMBER_SEPARATOR     = 6,
		BLOCK_SEPARATOR             = 7,
		SEGMENT_SEPARATOR           = 8,
		WHITE_SPACE_NEUTRAL         = 9, 
		OTHER_NEUTRAL               = 10, 
		LEFT_TO_RIGHT_EMBEDDING     = 11,
		LEFT_TO_RIGHT_OVERRIDE      = 12,
		RIGHT_TO_LEFT_ARABIC        = 13,
		RIGHT_TO_LEFT_EMBEDDING     = 14,
		RIGHT_TO_LEFT_OVERRIDE      = 15,
		POP_DIRECTIONAL_FORMAT      = 16,
		DIR_NON_SPACING_MARK        = 17,
		BOUNDARY_NEUTRAL            = 18
    };
    
    /**
     * Values returned by the getCellWidth() function.
     * @see Unicode#getCellWidth
     */
    enum ECellWidths
    {
        ZERO_WIDTH              = 0,
        HALF_WIDTH              = 1,
        FULL_WIDTH              = 2,
        NEUTRAL                 = 3
    };

    /**
     * Does this code unit alone represent a Unicode code point?
     * If so, then the code point value is the same as the code unit value,
     * or <code>(UChar32)c</code>.
     * Being a single, lead, or trail unit are mutually exclusive properties.
     *
     * @param c The code unit to be tested.
     * @return Boolean value.
     */
    static inline UBool isSingle(UChar c);

    /**
     * Is this code unit the first of a multiple-unit sequence?
     * Being a single, lead, or trail unit are mutually exclusive properties.
     *
     * @param c The code unit to be tested.
     * @return Boolean value.
     */
    static inline UBool isLead(UChar c);

    /**
     * Is this code unit one of, but not the first, of a multiple-unit sequence?
     * Being a single, lead, or trail unit are mutually exclusive properties.
     *
     * @param c The code unit to be tested.
     * @return Boolean value.
     */
    static inline UBool isTrail(UChar c);

    /**
     * Is this code point a surrogate character?
     * Surrogates are not characters; they are reserved for
     * use in UTF-16 strings as leading and trailing code units
     * of multiple-unit sequences for single code points.
     *
     * @param c The code point to be tested.
     * @return Boolean value.
     */
    static inline UBool isSurrogate(UChar32 c);

    /**
     * Is this code point a Unicode character?
     * The value range for Unicode characters is limited to
     * 0x10ffff==MAX_VALUE, and some values within this
     * range are reserved and not characters, too.
     * Those are the surrogate values and all values where the least
     * significant 16 bits are either 0xfffe or 0xffff.
     *
     * @param c The code point to be tested.
     * @return Boolean value.
     */
    static inline UBool isUnicodeChar(UChar32 c);

    /**
     * Is this code point an error value?
     * In ICU, code point access with macros or functions does not result
     * in a UErrorCode to be set if a code unit sequence is illegal
     * or irregular, but instead the resulting code point will be
     * one of few special error values. This function tests for one of those.
     *
     * @param c The code point to be tested.
     * @return Boolean value.
     */
    static inline UBool isError(UChar32 c);

    /**
     * Is this code point a Unicode character, and not an error value?
     * This is an efficient combination of
     * <code>isUnicodeChar(c) && !isError(c)</code>.
     *
     * @param c The code point to be tested.
     * @return Boolean value.
     */
    static inline UBool isValid(UChar32 c);

    /**
     * When writing code units for a given code point, is more than one
     * code unit necessary?
     * If not, then a single UChar value of <code>(UChar)c</code> can
     * be written to a UChar array. Otherwise, multiple code units need to be
     * calculated and written.
     *
     * @param c The code point to be tested.
     * @return Boolean value.
     */
    static inline UBool needMultipleUChar(UChar32 c);

    /**
     * When writing code units for a given code point, how many
     * code units are necessary?
     *
     * @param c The code point to be tested.
     * @return Boolean value.
     */
    static inline int32_t charLength(UChar32 c);

    /**
     * This function returns an average size of a UChar array compared to the
     * size that it would need to hold similar text if UTF-16 were used.
     * With UTF-16, this always returns its argument.
     * With UTF-8, the number returned will be larger, with UTF-32, smaller.
     * It will typically be less than <code>size*MAX_CHAR_LENGTH</code>.
     *
     * @param size The size of the array if UTF-16 were used.
     * @return An average size necessary for the UTF that ICU was compiled for.
     *         (Only UTF-16 is supported right now, therefore,
     *         this will always be <code>size</code> itself. This may change in the future.)
     */
    static inline int32_t arraySize(int32_t size);

   /**
     * Determines whether the specified UChar is a lowercase character
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is lowercase; false otherwise.
     *
     * @see Unicode#isUpperCase
     * @see Unicode#isTitleCase
     * @see Unicode#toLowerCase
     * @draft
     */
    static inline UBool isLowerCase(UChar32 ch);

    /**
     * Determines whether the specified character is an uppercase character
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is uppercase; false otherwise.
     * @see Unicode#isLowerCase
     * @see Unicode#isTitleCase
     * @see Unicode#toUpperCase
     * @draft
     */
    static inline UBool isUpperCase(UChar32 ch);

    /**
     * Determines whether the specified character is a titlecase character
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is titlecase; false otherwise.
     * @see Unicode#isUpperCase
     * @see Unicode#isLowerCase
     * @see Unicode#toTitleCase
     * @draft
     */
    static inline UBool isTitleCase(UChar32 ch);

    /**
     * Determines whether the specified character is a digit according to Unicode
     * 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is a digit; false otherwise.
     * @see     Unicode#digit
     * @see     Unicode#forDigit
     * @see     Unicode#digitValue
     * @draft
     */
    static inline UBool isDigit(UChar32 ch);

    /**
     * Determines whether the specified numeric value is actually a defined character
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character has a defined Unicode meaning; false otherwise.
     *
     * @see Unicode#isDigit
     * @see Unicode#isLetter
     * @see Unicode#isLetterOrDigit
     * @see Unicode#isUpperCase
     * @see Unicode#isLowerCase
     * @see Unicode#isTitleCase
     * @draft
     */
    static inline UBool isDefined(UChar32 ch);

    /**
     * Determines whether the specified character is a control character according 
     * to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the Unicode character is a control character; false otherwise.
     *
     * @see Unicode#isPrintable
     * @draft
     */
    static inline UBool isControl(UChar32 ch);

    /**
     * Determines whether the specified character is a printable character according 
     * to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the Unicode character is a printable character; false otherwise.
     *
     * @see Unicode#isControl
     * @draft
     */
    static inline UBool isPrintable(UChar32 ch);

    /**
     * Determines whether the specified character is of the base form according 
     * to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the Unicode character is of the base form; false otherwise.
     *
     * @see Unicode#isLetter
     * @see Unicode#isDigit
     * @draft
     */
     static inline UBool isBaseForm(UChar32 ch);

    /**
     * Determines whether the specified character is a letter
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is a letter; false otherwise.
     *
     *
     * @see Unicode#isDigit
     * @see Unicode#isLetterOrDigit
     * @see Unicode#isUpperCase
     * @see Unicode#isLowerCase
     * @see Unicode#isTitleCase
     * @draft
     */
    static inline UBool isLetter(UChar32 ch);

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
     * @param   ch  the Unicode character.
     * @return  TRUE if the character may start a Java identifier;
     *          FALSE otherwise.
     * @see     isJavaIdentifierPart
     * @see     isLetter
     * @see     isUnicodeIdentifierStart
     * @draft
     */
    static inline UBool isJavaIdentifierStart(UChar32 ch);

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
     * @param   ch  the Unicode character.
     * @return  TRUE if the character may be part of a Unicode identifier; 
     *          FALSE otherwise.
     * @see     isIdentifierIgnorable
     * @see     isJavaIdentifierStart
     * @see     isLetter
     * @see     isDigit
     * @see     isUnicodeIdentifierPart
     * @draft
     */
    static inline UBool isJavaIdentifierPart(UChar32 ch);

    /**
     * A convenience method for determining if a Unicode character 
     * is allowed to start in a Unicode identifier.
     * A character may start a Unicode identifier if and only if
     * it is a letter.
     *
     * @param   ch  the Unicode character.
     * @return  TRUE if the character may start a Unicode identifier;
     *          FALSE otherwise.
     * @see     isJavaIdentifierStart
     * @see     isLetter
     * @see     isUnicodeIdentifierPart
     * @draft
     */
    static inline UBool isUnicodeIdentifierStart(UChar32 ch);

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
     * @param   ch  the Unicode character.
     * @return  TRUE if the character may be part of a Unicode identifier;
     *          FALSE otherwise.
     * @see     isIdentifierIgnorable
     * @see     isJavaIdentifierPart
     * @see     isLetterOrDigit
     * @see     isUnicodeIdentifierStart
     * @draft
     */
    static inline UBool isUnicodeIdentifierPart(UChar32 ch);

    /**
     * A convenience method for determining if a Unicode character 
     * should be regarded as an ignorable character in a Java 
     * identifier or a Unicode identifier.
     * <P>
     * The following Unicode characters are ignorable in a Java identifier
     * or a Unicode identifier:
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
     * @param   ch  the Unicode character.
     * @return  TRUE if the character may be part of a Unicode identifier;
     *          FALSE otherwise.
     * @see     isJavaIdentifierPart
     * @see     isUnicodeIdentifierPart
     * @draft
     */
    static inline UBool isIdentifierIgnorable(UChar32 ch);

    /**
     * The given character is mapped to its lowercase equivalent according to
     * Unicode 2.1.2; if the character has no lowercase equivalent, the character 
     * itself is returned.
     * <P>
     * A character has a lowercase equivalent if and only if a lowercase mapping
     * is specified for the character in the Unicode 2.0 attribute table.
     * <P>
     * Unicode::toLowerCase() only deals with the general letter case conversion.
     * For language specific case conversion behavior, use UnicodeString::toLower().
     * For example, the case conversion for dot-less i and dotted I in Turkish,
     * or for final sigma in Greek.
     *
     * @param ch    the character to be converted
     * @return  the lowercase equivalent of the character, if any;
     *      otherwise the character itself.
     *
     * @see UnicodeString#toLower
     * @see Unicode#isLowerCase
     * @see Unicode#isUpperCase
     * @see Unicode#toUpperCase
     * @see Unicode#toTitleCase
     * @draft
     */
   static inline UChar32 toLowerCase(UChar32 ch); 

    /**
     * The given character is mapped to its uppercase equivalent according to Unicode
     * 2.1.2; if the character has no uppercase equivalent, the character itself is 
     * returned.
     * <P>
     * Unicode::toUpperCase() only deals with the general letter case conversion.
     * For language specific case conversion behavior, use UnicodeString::toUpper().
     * For example, the case conversion for dot-less i and dotted I in Turkish,
     * or ess-zed (i.e., "sharp S") in German.
     *
     * @param ch    the character to be converted
     * @return  the uppercase equivalent of the character, if any;
     *      otherwise the character itself.
     *
     * @see UnicodeString#toUpper
     * @see Unicode#isUpperCase
     * @see Unicode#isLowerCase
     * @see Unicode#toLowerCase
     * @see Unicode#toTitleCase
     * @draft
     */
    static inline UChar32 toUpperCase(UChar32 ch);

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
     * @see Unicode#isTitleCase
     * @see Unicode#toUpperCase
     * @see Unicode#toLowerCase
     * @draft
     */
    static inline UChar32 toTitleCase(UChar32 ch);

    /**
     * Determines if the specified character is a Unicode space character
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is a space character; false otherwise.
     * @draft
     */
    static inline UBool isSpaceChar(UChar32 ch);

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
     * @see     #isSpaceChar
     * @draft
     */
    static inline UBool isWhitespace(UChar32 ch);

   /**
     * Returns a value indicating a character category according to Unicode
     * 2.1.2.
     * @param ch            the character to be tested
     * @return a value of type int, the character category.
     * @see Unicode#UNASSIGNED
     * @see Unicode#UPPERCASE_LETTER
     * @see Unicode#LOWERCASE_LETTER
     * @see Unicode#TITLECASE_LETTER
     * @see Unicode#MODIFIER_LETTER
     * @see Unicode#OTHER_LETTER
     * @see Unicode#NON_SPACING_MARK
     * @see Unicode#ENCLOSING_MARK
     * @see Unicode#COMBINING_SPACING_MARK
     * @see Unicode#DECIMAL_DIGIT_NUMBER
     * @see Unicode#OTHER_NUMBER
     * @see Unicode#SPACE_SEPARATOR
     * @see Unicode#LINE_SEPARATOR
     * @see Unicode#PARAGRAPH_SEPARATOR
     * @see Unicode#CONTROL
     * @see Unicode#PRIVATE_USE
     * @see Unicode#SURROGATE
     * @see Unicode#DASH_PUNCTUATION
     * @see Unicode#OPEN_PUNCTUATION
     * @see Unicode#CLOSE_PUNCTUATION
     * @see Unicode#CONNECTOR_PUNCTUATION
     * @see Unicode#OTHER_PUNCTUATION
     * @see Unicode#LETTER_NUMBER
     * @see Unicode#MATH_SYMBOL
     * @see Unicode#CURRENCY_SYMBOL
     * @see Unicode#MODIFIER_SYMBOL
     * @see Unicode#OTHER_SYMBOL
     * @draft
     */
    static inline int8_t getType(UChar32 ch);

    /**
     * Returns the linguistic direction property of a character.
     * <P>
     * Returns the linguistic direction property of a character.
     * For example, 0x0041 (letter A) has the LEFT_TO_RIGHT directional 
     * property.
     * @see #EDirectionProperty
     * @draft
     */
    static inline EDirectionProperty characterDirection(UChar32 ch);

    /**
     * Determines whether the character has the "mirrored" property.
     * This property is set for characters that are commonly used in
     * Right-To-Left contexts and need to be displayed with a "mirrored"
     * glyph.
     *
     * @param c the character (code point, Unicode scalar value) to be tested
     * @return TRUE if the character has the "mirrored" property
     */
    static inline UBool isMirrored(UChar32 c);

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
    static inline UChar32 charMirror(UChar32 c);

    /**
     * Returns the script associated with a character.
     * @see #EUnicodeScript
     * @draft
     */
    static inline EUnicodeScript getScript(UChar32 ch);

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
    static inline uint16_t getCellWidth(UChar32 ch);

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
     * @param buffer Destination address for copying the name.
     * @param bufferLength <code>==sizeof(buffer)</code>
     * @param nameChoice Selector for which name to get.
     *
     * @see UCharNameChoice
     *
     * Example:
     * <pre>
     * &#32;   char buffer[100];
     * &#32;   UTextOffset length=Unicode::getCharName(
     * &#32;           0x284, buffer, sizeof(buffer));
     * &#32;   
     * &#32;   // use invariant-character conversion to Unicode
     * &#32;   UnicodeString name(buffer, length, "");
     * </pre>
     * @draft
     */
    static inline UTextOffset
    getCharName(uint32_t code,
                char *buffer, UTextOffset bufferLength,
                UCharNameChoice nameChoice=U_UNICODE_CHAR_NAME);

    /**
     * Retrives the decimal numeric value of a digit character.
     * @param ch the digit character for which to get the numeric value
     * @return the numeric value of ch in decimal radix.  This method returns
     * -1 if ch is not a valid digit character.
     * @see     Unicode#digit
     * @see     Unicode#forDigit
     * @see     Unicode#isDigit
     * @deprecated HSYS: use Unicode::digit instead.
     */
    static inline int32_t digitValue(UChar32 ch);     

    /**
     * Returns the numeric value of the character <code>ch</code> in the 
     * specified radix. 
     * <p>
     * If the radix is not in the range <code>MIN_RADIX</code>&nbsp;&lt;= 
     * <code>radix</code>&nbsp;&lt;= <code>MAX_RADIX</code> or if the 
     * value of <code>ch</code> is not a valid digit in the specified 
     * radix, <code>-1</code> is returned. A character is a valid digit 
     * if at least one of the following is true:
     * <ul>
     * <li>The method <code>isDigit</code> is true of the character 
     *     and the Unicode decimal digit value of the character (or its 
     *     single-character decomposition) is less than the specified radix. 
     *     In this case the decimal digit value is returned. 
     * <li>The character is one of the uppercase Latin letters 
     *     <code>'A'</code> through <code>'Z'</code> and its code is less than
     *     <code>radix&nbsp;+ 'A'&nbsp;-&nbsp;10</code>. 
     *     In this case, <code>ch&nbsp;- 'A'&nbsp;+&nbsp;10</code> 
     *     is returned. 
     * <li>The character is one of the lowercase Latin letters 
     *     <code>'a'</code> through <code>'z'</code> and its code is less than
     *     <code>radix&nbsp;+ 'a'&nbsp;-&nbsp;10</code>. 
     *     In this case, <code>ch&nbsp;- 'a'&nbsp;+&nbsp;10</code> 
     *     is returned. 
     * </ul>
     *
     * @param   ch      the character to be converted.
     * @param   radix   the radix.
     * @return  the numeric value represented by the character in the
     *          specified radix.
     * @see     Unicode#MIN_RADIX
     * @see     Unicode#MAX_RADIX
     * @see     Unicode#forDigit
     * @see     Unicode#digitValue
     * @see     Unicode#isDigit
     * @draft
     */
    static inline int8_t digit(UChar32 ch, int8_t radix);
	
    /**
     * Determines the character representation for a specific digit in 
     * the specified radix. If the value of <code>radix</code> is not a 
     * valid radix, or the value of <code>digit</code> is not a valid 
     * digit in the specified radix, the null character
     * (<code>U+0000</code>) is returned. 
     * <p>
     * The <code>radix</code> argument is valid if it is greater than or 
     * equal to <code>MIN_RADIX</code> and less than or equal to 
     * <code>MAX_RADIX</code>. The <code>digit</code> argument is valid if
     * <code>0&nbsp;&lt;= digit&nbsp;&lt;=&nbsp;radix</code>. 
     * <p>
     * If the digit is less than 10, then 
     * <code>'0'&nbsp;+ digit</code> is returned. Otherwise, the value 
     * <code>'a'&nbsp;+ digit&nbsp;-&nbsp;10</code> is returned. 
     *
     * @param   digit   the number to convert to a character.
     * @param   radix   the radix.
     * @return  the <code>char</code> representation of the specified digit
     *          in the specified radix. 
     * @see     Unicode#MIN_RADIX
     * @see     Unicode#MAX_RADIX
     * @see     Unicode#digit
     * @see     Unicode#digitValue
     * @see     Unicode#isDigit
     * @draft
     */
    static inline UChar32 forDigit(int32_t digit, int8_t radix);

    /**
     * Retrieves the Unicode Standard Version number that is used
     * @param info the version # information, the result will be filled in
     * @draft
     */
	static void getUnicodeVersion(UVersionInfo info);

protected:
    // These constructors, destructor, and assignment operator must
    // be protected (not private, as they semantically are) to make
    // various UNIX compilers happy. [LIU]
    // They should be private to prevent anyone from instantiating or
    // subclassing Unicode.
    Unicode();
    Unicode(const Unicode &other);
    ~Unicode();
    const Unicode &operator=(const Unicode &other);
};

/* inline implementations --------------------------------------------------- */

inline UBool
Unicode::isSingle(UChar c) {
    return UTF_IS_SINGLE(c);
}

inline UBool
Unicode::isLead(UChar c) {
    return UTF_IS_LEAD(c);
}

inline UBool
Unicode::isTrail(UChar c) {
    return UTF_IS_TRAIL(c);
}

inline UBool
Unicode::isSurrogate(UChar32 c) {
    return UTF_IS_SURROGATE(c);
}

inline UBool
Unicode::isUnicodeChar(UChar32 c) {
    return UTF_IS_UNICODE_CHAR(c);
}

inline UBool
Unicode::isError(UChar32 c) {
    return UTF_IS_ERROR(c);
}

inline UBool
Unicode::isValid(UChar32 c) {
    return UTF_IS_VALID(c);
}

inline UBool
Unicode::needMultipleUChar(UChar32 c) {
    return UTF_NEED_MULTIPLE_UCHAR(c);
}

inline int32_t
Unicode::charLength(UChar32 c) {
    return UTF_CHAR_LENGTH(c);
}

inline int32_t
Unicode::arraySize(int32_t size) {
    return UTF_ARRAY_SIZE(size);
}

// Checks if ch is a lower case letter.
inline UBool
Unicode::isLowerCase(UChar32 ch) {
    return u_islower(ch);
}

// Checks if ch is a upper case letter.
inline UBool
Unicode::isUpperCase(UChar32 ch) {
    return u_isupper(ch);
}

// Checks if ch is a title case letter; usually upper case letters.
inline UBool
Unicode::isTitleCase(UChar32 ch) {
    return u_istitle(ch);
}

// Checks if ch is a decimal digit.
inline UBool
Unicode::isDigit(UChar32 ch) {
    return u_isdigit(ch);
}

// Checks if ch is a unicode character with assigned character type.
inline UBool
Unicode::isDefined(UChar32 ch) {
    return u_isdefined(ch);
}

// Checks if the Unicode character is a control character.
inline UBool
Unicode::isControl(UChar32 ch) {
    return u_iscntrl(ch);
}

// Checks if the Unicode character is printable.
inline UBool
Unicode::isPrintable(UChar32 ch) {
    return u_isprint(ch);
}

// Checks if the Unicode character is a base form character that can take a diacritic.
inline UBool
Unicode::isBaseForm(UChar32 ch) {
    return u_isbase(ch);
}

// Checks if the Unicode character is a letter.
inline UBool
Unicode::isLetter(UChar32 ch) {
    return u_isalpha(ch);
}

// Checks if the Unicode character can start a Java identifier.
inline UBool
Unicode::isJavaIdentifierStart(UChar32 ch) {
    return u_isJavaIDStart(ch);
}

// Checks if the Unicode character can be a Java identifier part other than starting the
// identifier.
inline UBool
Unicode::isJavaIdentifierPart(UChar32 ch) {
    return u_isJavaIDPart(ch);
}

// Checks if the Unicode character can start a Unicode identifier.
inline UBool
Unicode::isUnicodeIdentifierStart(UChar32 ch) {
    return u_isIDStart(ch);
}

// Checks if the Unicode character can be a Unicode identifier part other than starting the
// identifier.
inline UBool
Unicode::isUnicodeIdentifierPart(UChar32 ch) {
    return u_isIDPart(ch);
}

// Checks if the Unicode character can be ignorable in a Java or Unicode identifier.
inline UBool
Unicode::isIdentifierIgnorable(UChar32 ch) {
    return u_isIDIgnorable(ch);
}

// Transforms the Unicode character to its lower case equivalent.
inline UChar32       
Unicode::toLowerCase(UChar32 ch) {
    return u_tolower(ch);
}
    
// Transforms the Unicode character to its upper case equivalent.
inline UChar32
Unicode::toUpperCase(UChar32 ch) {
    return u_toupper(ch);
}

// Transforms the Unicode character to its title case equivalent.
inline UChar32
Unicode::toTitleCase(UChar32 ch) {
    return u_totitle(ch);
}

// Checks if the Unicode character is a space character.
inline UBool
Unicode::isSpaceChar(UChar32 ch) {
    return u_isspace(ch);
}

// Determines if the specified character is white space according to ICU.
inline UBool
Unicode::isWhitespace(UChar32 ch) {
    return u_isWhitespace(ch);
}

// Gets if the Unicode character's character property.
inline int8_t
Unicode::getType(UChar32 ch) {
    return u_charType(ch);
}

// Gets the character's linguistic directionality.
inline Unicode::EDirectionProperty
Unicode::characterDirection(UChar32 ch) {
    return (EDirectionProperty)u_charDirection(ch);
}

// Determines if the character has the "mirrored" property.
inline UBool
Unicode::isMirrored(UChar32 ch) {
    return u_isMirrored(ch);
}

// Maps the character to a "mirror-image" character, or to itself.
inline UChar32
Unicode::charMirror(UChar32 ch) {
    return u_charMirror(ch);
}

// Get the script associated with the character
inline Unicode::EUnicodeScript
Unicode::getScript(UChar32 ch) {
    return (EUnicodeScript) u_charScript(ch);
}

// Gets table cell width of the Unicode character.
inline uint16_t
Unicode::getCellWidth(UChar32 ch) {
    return u_charCellWidth(ch);
}

inline UTextOffset
Unicode::getCharName(uint32_t code,
                     char *buffer, UTextOffset bufferLength,
                     UCharNameChoice nameChoice) {
    UErrorCode errorCode=U_ZERO_ERROR;
    UTextOffset length=u_charName(code, nameChoice, buffer, bufferLength, &errorCode);
    return U_SUCCESS(errorCode) ? length : 0;
}

inline int32_t            
Unicode::digitValue(UChar32 ch) {
    return u_charDigitValue(ch);
}

inline int8_t
Unicode::digit(UChar32 ch, int8_t radix) {
    // ### TODO this should probably move to a C u_charDigitValueEx(ch, radix) and be called here
    int8_t value;
    if((uint8_t)(radix-MIN_RADIX)<=(MAX_RADIX-MIN_RADIX)) {
        value=(int8_t)u_charDigitValue(ch);
        if(value<0) {
            // ch is not a decimal digit, try latin letters
            if ((uint32_t)(ch-0x41)<26) {
                value=(int8_t)(ch-(0x41-10)); // A-Z, subtract A
            } else if ((uint32_t)(ch-0x61)<26) {
                value=(int8_t)(ch-(0x61-10)); // a-z, subtract a
            } else {
                return -1; // ch is not a digit character
            }
        }
    } else {
        return -1; // invalid radix
    }
    return (uint8_t)((value<radix) ? value : (uint8_t)(-1));
}

inline UChar32
Unicode::forDigit(int32_t digit, int8_t radix) {
    // ### TODO this should probably move to a C u_forDigit(digit, radix) and be called here
    if((uint8_t)(radix-MIN_RADIX)>(MAX_RADIX-MIN_RADIX) || (uint32_t)digit>=(uint32_t)radix) {
        return 0;
    } else if(digit<10) {
        return (UChar32)(0x30+digit);
    } else {
        return (UChar32)((0x61-10)+digit);
    }
}

inline void
Unicode::getUnicodeVersion(UVersionInfo versionArray) {
	u_getUnicodeVersion(versionArray);
}

#endif
