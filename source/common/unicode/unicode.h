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
 * Unicode character encoding standard, includes information about the sript 
 * (for example, symbols or control characters) to which the character belongs,
 * as well as semantic information such as whether a character is a digit or 
 * uppercase, lowercase, or uncased.
 * <P>
 * @subclassing Do not subclass.
 */
class U_COMMON_API Unicode
{
public:
    /**
     * The minimum value a UChar can have.  The lowest value a
     * UChar can have is 0x0000.
     */
    static const UChar   MIN_VALUE;

    /**
     * The maximum value a UChar can have.  The greatest value a
     * UChar can have is 0xffff.
     */
    static const UChar   MAX_VALUE;

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
     * The minimum radix available for conversion to and from Strings.  
     * The constant value of this field is the smallest value permitted 
     * for the radix argument in radix-conversion methods such as the 
     * <code>digit</code> method and the <code>forDigit</code>
     * method. 
     *
     * @see     Unicode#digit
     * @see     Unicode#forDigit
     */
    static const int8_t MIN_RADIX;

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
    static const int8_t MAX_RADIX;

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
     */
    static  bool_t          isLowerCase(UChar     ch);

    /**
     * Determines whether the specified character is an uppercase character
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is uppercase; false otherwise.
     * @see Unicode#isLowerCase
     * @see Unicode#isTitleCase
     * @see Unicode#toUpperCase
     */
    static  bool_t          isUpperCase(UChar ch);

    /**
     * Determines whether the specified character is a titlecase character
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is titlecase; false otherwise.
     * @see Unicode#isUpperCase
     * @see Unicode#isLowerCase
     * @see Unicode#toTitleCase
     */
    static  bool_t          isTitleCase(UChar ch);

    /**
     * Determines whether the specified character is a digit according to Unicode
     * 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is a digit; false otherwise.
     * @see     Unicode#digit
     * @see     Unicode#forDigit
     * @see     Unicode#digitValue
     */
    static  bool_t          isDigit(UChar ch);

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
     */
    static  bool_t          isDefined(UChar       ch);

    /**
     * Determines whether the specified character is a control character according 
     * to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the Unicode character is a control character; false otherwise.
     *
     * @see Unicode#isPrintable
     */
    static  bool_t          isControl(UChar ch);

    /**
     * Determines whether the specified character is a printable character according 
     * to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the Unicode character is a printable character; false otherwise.
     *
     * @see Unicode#isControl
     */
    static  bool_t          isPrintable(UChar ch);

    /**
     * Determines whether the specified character is of the base form according 
     * to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the Unicode character is of the base form; false otherwise.
     *
     * @see Unicode#isLetter
     * @see Unicode#isDigit
     */

     static  bool_t          isBaseForm(UChar ch);
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
     */
    static  bool_t          isLetter(UChar        ch);

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
     */
    static bool_t isJavaIdentifierStart(UChar ch);

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
     */
    static bool_t isJavaIdentifierPart(UChar ch);

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
     */
    static bool_t isUnicodeIdentifierStart(UChar ch);

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
     */
    static bool_t isUnicodeIdentifierPart(UChar ch);

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
     */
    static bool_t isIdentifierIgnorable(UChar ch);

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
     */
   static   UChar         toLowerCase(UChar     ch); 

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
     */
    static  UChar         toUpperCase(UChar     ch);

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
     */
    static  UChar             toTitleCase(UChar     ch);

    /**
     * Determines if the specified character is ISO-LATIN-1 white space. 
     * This method returns <code>true</code> for the following five 
     * characters only: 
     * <table>
     * <tr><td>'\t'</td>            <td>&#92;u0009</td>
     *     <td><code>HORIZONTAL TABULATION</code></td></tr>
     * <tr><td>'\n'</td>            <td>&#92;u000A</td>
     *     <td><code>NEW LINE</code></td></tr>
     * <tr><td>'\f'</td>            <td>&#92;u000C</td>
     *     <td><code>FORM FEED</code></td></tr>
     * <tr><td>'\r'</td>            <td>&#92;u000D</td>
     *     <td><code>CARRIAGE RETURN</code></td></tr>
     * <tr><td>'&nbsp;&nbsp;'</td>  <td>&#92;u0020</td>
     *     <td><code>SPACE</code></td></tr>
     * </table>
     *
     * @param      ch   the character to be tested.
     * @return     <code>true</code> if the character is ISO-LATIN-1 white
     *             space; <code>false</code> otherwise.
     * @see        #isSpaceChar
     * @see        #isWhitespace
     * @deprecated Replaced by isWhitespace(char).
     */
    static bool_t isSpace(UChar ch);

    /**
     * Determines if the specified character is a Unicode space character
     * according to Unicode 2.1.2.
     *
     * @param ch    the character to be tested
     * @return  true if the character is a space character; false otherwise.
     */
    static  bool_t              isSpaceChar(UChar     ch);

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
     *
     * @param   ch	the character to be tested.
     * @return  true if the character is a Java whitespace character;
     *          false otherwise.
     * @see     #isSpaceChar
     */
    static bool_t isWhitespace(UChar ch);

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
     */
    static  int8_t          getType(UChar     ch);

    /**
     * Returns the linguistic direction property of a character.
     * <P>
     * Returns the linguistic direction property of a character.
     * For example, 0x0041 (letter A) has the LEFT_TO_RIGHT directional 
     * property.
     * @see #EDirectionProperty
     */
    static EDirectionProperty characterDirection(UChar ch);

    /**
     * Returns the script associated with a character.
     * @see #EUnicodeScript
     */
    static EUnicodeScript    getScript(UChar    ch);

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
     */
    static uint16_t         getCellWidth(UChar    ch);

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
     */
    static int32_t            digitValue(UChar ch);     

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
     */
    static int8_t digit(UChar ch, int8_t radix);
	
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
     */
    static UChar forDigit(int32_t digit, int8_t radix);

	/**
	 * Retrieves the Unicode Standard Version number that is used
	 * @return the Unicode Standard Version Number.
	 */
	static int32_t   getVersion(uint8_t *versionArray);

protected:
    // These constructors, destructor, and assignment operator must
    // be protected (not private, as they semantically are) to make
    // various UNIX compilers happy. [LIU]
                            Unicode();
                            Unicode(    const   Unicode&    other);
                            ~Unicode();
    const   Unicode&        operator=(  const   Unicode&    other);



};

inline UTextOffset
Unicode::getCharName(uint32_t code,
                     char *buffer, UTextOffset bufferLength,
                     UCharNameChoice nameChoice) {
    UErrorCode errorCode=U_ZERO_ERROR;
    UTextOffset length=u_charName(code, nameChoice, buffer, bufferLength, &errorCode);
    return U_SUCCESS(errorCode) ? length : 0;
}

#endif
