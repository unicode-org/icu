/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*
* File TXTBDAT.H
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.
*                           Made static data members const where appropriate.
*   03/25/97    aliu        Removed subclasses, and merged their static data into this
*                           class.  Instantiated four static instances for character,
*                           word, sentence, and line.  Made forward(), backward(), and
*                           map() methods inline.
*   04/15/97    aliu        Worked around bug in AIX xlC compiler which occurs if static
*                           arrays contain const elements.
*   05/06/97    aliu        Made kSI, kStop, and kSI_Stop into #defines to help out
*                           non-compliant compilers.
*****************************************************************************************
*/

#ifndef TXTBDAT_H
#define TXTBDAT_H

#include "utypes.h"
class WordBreakTable;
class UnicodeClassMapping;
class SpecialMapping;

/**
 * This class wraps up the data tables needed for SimpleTextBoundary.
 * It is statically instantiated for each type of text boundary.  This
 * class is not designed to be subclassed.
 */
class TextBoundaryData {
public:
    ~TextBoundaryData() {} // Do not subclass

    // Fast inline accessors
    const WordBreakTable* forward(void) const;
    const WordBreakTable* backward(void) const;
    const UnicodeClassMapping* map(void) const;

    static const TextBoundaryData kCharacterBreakData;
    static const TextBoundaryData kWordBreakData;
    static const TextBoundaryData kLineBreakData;
    static const TextBoundaryData kSentenceBreakData;

    typedef uint8_t Node;
    typedef uint8_t Type;

private:
    static const UChar ASCII_END_OF_TEXT;
    static const UChar ASCII_HORIZONTAL_TABULATION;
    static const UChar ASCII_LINEFEED;
    static const UChar ASCII_VERTICAL_TABULATION;
    static const UChar ASCII_FORM_FEED;
    static const UChar ASCII_CARRIAGE_RETURN;
    static const UChar ASCII_SPACE;
    static const UChar ASCII_EXCLAMATION_MARK;
    static const UChar ASCII_QUOTATION_MARK;
    static const UChar ASCII_NUMBER_SIGN;
    static const UChar ASCII_DOLLAR_SIGN;
    static const UChar ASCII_PERCENT;
    static const UChar ASCII_AMPERSAND;
    static const UChar ASCII_APOSTROPHE;
    static const UChar ASCII_COMMA;
    static const UChar ASCII_FULL_STOP;
    static const UChar ASCII_COLON;
    static const UChar ASCII_SEMICOLON;
    static const UChar ASCII_QUESTION_MARK;
    static const UChar ASCII_NONBREAKING_SPACE;
    static const UChar ASCII_CENT_SIGN;
    static const UChar ASCII_POUND_SIGN;
    static const UChar ASCII_YEN_SIGN;
    static const UChar LATIN1_SOFTHYPHEN;
    static const UChar LATIN1_DEGREE_SIGN;
    static const UChar ARABIC_PERCENT_SIGN;
    static const UChar ARABIC_DECIMAL_SEPARATOR;
    static const UChar HANGUL_CHOSEONG_LOW;
    static const UChar HANGUL_CHOSEONG_HIGH;
    static const UChar HANGUL_JUNGSEONG_LOW;
    static const UChar HANGUL_JUNGSEONG_HIGH;
    static const UChar HANGUL_JONGSEONG_LOW;
    static const UChar HANGUL_JONGSEONG_HIGH;
    static const UChar FIGURE_SPACE;
    static const UChar NONBREAKING_HYPHEN;
    static const UChar PUNCTUATION_HYPHENATION_POINT;
    static const UChar PUNCTUATION_LINE_SEPARATOR;
    static const UChar PUNCTUATION_PARAGRAPH_SEPARATOR;
    static const UChar PER_MILLE_SIGN;
    static const UChar PER_TEN_THOUSAND_SIGN;
    static const UChar PRIME;
    static const UChar DOUBLE_PRIME;
    static const UChar TRIPLE_PRIME;
    static const UChar DEGREE_CELSIUS;
    static const UChar DEGREE_FAHRENHEIT;
    static const UChar PUNCTUATION_IDEOGRAPHIC_COMMA;
    static const UChar PUNCTUATION_IDEOGRAPHIC_FULL_STOP; 
    static const UChar IDEOGRAPHIC_ITERATION_MARK;
    static const UChar HIRAGANA_LETTER_SMALL_A;
    static const UChar HIRAGANA_LETTER_A;
    static const UChar HIRAGANA_LETTER_SMALL_I;
    static const UChar HIRAGANA_LETTER_I;
    static const UChar HIRAGANA_LETTER_SMALL_U;
    static const UChar HIRAGANA_LETTER_U;
    static const UChar HIRAGANA_LETTER_SMALL_E;
    static const UChar HIRAGANA_LETTER_E;
    static const UChar HIRAGANA_LETTER_SMALL_O;
    static const UChar HIRAGANA_LETTER_O;
    static const UChar HIRAGANA_LETTER_DI;
    static const UChar HIRAGANA_LETTER_SMALL_TU;
    static const UChar HIRAGANA_LETTER_TU;
    static const UChar HIRAGANA_LETTER_MO;
    static const UChar HIRAGANA_LETTER_SMALL_YA;
    static const UChar HIRAGANA_LETTER_YA;
    static const UChar HIRAGANA_LETTER_SMALL_YU;
    static const UChar HIRAGANA_LETTER_YU;
    static const UChar HIRAGANA_LETTER_SMALL_YO;
    static const UChar HIRAGANA_LETTER_YO;
    static const UChar HIRAGANA_LETTER_RO;
    static const UChar HIRAGANA_LETTER_SMALL_WA;
    static const UChar HIRAGANA_LETTER_WA;
    static const UChar HIRAGANA_LETTER_VU;
    static const UChar COMBINING_KATAKANA_HIRAGANA_VOICED_SOUND_MARK;
    static const UChar HIRAGANA_SEMIVOICED_SOUND_MARK;
    static const UChar HIRAGANA_ITERATION_MARK;
    static const UChar HIRAGANA_VOICED_ITERATION_MARK;
    static const UChar KATAKANA_LETTER_SMALL_A;
    static const UChar KATAKANA_LETTER_A;
    static const UChar KATAKANA_LETTER_SMALL_I;
    static const UChar KATAKANA_LETTER_I;
    static const UChar KATAKANA_LETTER_SMALL_U;
    static const UChar KATAKANA_LETTER_U;
    static const UChar KATAKANA_LETTER_SMALL_E;
    static const UChar KATAKANA_LETTER_E;
    static const UChar KATAKANA_LETTER_SMALL_O;
    static const UChar KATAKANA_LETTER_O;
    static const UChar KATAKANA_LETTER_DI;
    static const UChar KATAKANA_LETTER_SMALL_TU;
    static const UChar KATAKANA_LETTER_TU;
    static const UChar KATAKANA_LETTER_MO;
    static const UChar KATAKANA_LETTER_SMALL_YA;
    static const UChar KATAKANA_LETTER_YA;
    static const UChar KATAKANA_LETTER_SMALL_YU;
    static const UChar KATAKANA_LETTER_YU;
    static const UChar KATAKANA_LETTER_SMALL_YO;
    static const UChar KATAKANA_LETTER_YO;
    static const UChar KATAKANA_LETTER_RO;
    static const UChar KATAKANA_LETTER_SMALL_WA;
    static const UChar KATAKANA_LETTER_WA;
    static const UChar KATAKANA_LETTER_VU;
    static const UChar KATAKANA_LETTER_SMALL_KA;
    static const UChar KATAKANA_LETTER_SMALL_KE;
    static const UChar KATAKANA_LETTER_VA;
    static const UChar KATAKANA_LETTER_VO;
    static const UChar KATAKANA_HIRAGANA_PROLONGED_SOUND_MARK;
    static const UChar KATAKANA_ITERATION_MARK;
    static const UChar KATAKANA_VOICED_ITERATION_MARK;
    static const UChar UNICODE_LOW_BOUND_HAN;
    static const UChar UNICODE_HIGH_BOUND_HAN;
    static const UChar HANGUL_SYL_LOW;
    static const UChar HANGUL_SYL_HIGH;
    static const UChar CJK_COMPATIBILITY_F900;
    static const UChar CJK_COMPATIBILITY_FA2D;
    static const UChar UNICODE_ZERO_WIDTH_NON_BREAKING_SPACE;
    static const UChar FULLWIDTH_EXCLAMATION_MARK;
    static const UChar FULLWIDTH_FULL_STOP;
    static const UChar FULLWIDTH_QUESTION_MARK;
    static const UChar END_OF_STRING;

private:
    // Character data
    enum CharacterMapping
    {
        // These enum values must occur in this order; do not
        // modify unless you know what you are doing!  The forward
        // and backward data tables are indexed by these enums.
        kAccent_diacritic   = 0,
        kBaseForm           = 1,
        kBaseCR             = 2,
        kBaseLF             = 3,
        kChoseong           = 4,   // Korean initial consonant
        kJungseong          = 5,  // Korean vowel
        kJongseong          = 6,  // Korean final consonant
        kEOS                = 7,
        kCharacterCol_count = 8
    };

    static Node                     kCharacterForwardData[];
    static const int32_t            kCharacterForwardData_length;
    static WordBreakTable*          kCharacterForward;
    static Node                     kCharacterBackwardData[];
    static const int32_t            kCharacterBackwardData_length;
    static WordBreakTable*          kCharacterBackward;
    static Type                     kCharacterRawMapping[];
    static const int32_t            kCharacterRawMapping_length;
    static SpecialMapping           kCharacterExceptionChar[];
    static const int32_t            kCharacterExceptionChar_length;
    static const bool_t             kCharacterExceptionFlags[];
    static UnicodeClassMapping*     kCharacterMap;
    static Type                     kCharacterAsciiValues[];

private:
    // Word data
    enum WordMapping
    {
        // These enum values must occur in this order; do not
        // modify unless you know what you are doing!  The forward
        // and backward data tables are indexed by these enums.
        kBreak          = 0,
        kLetter         = 1,
        kNumber         = 2,
        kMidLetter      = 3,
        kMidLetNum      = 4,
        kPreNum         = 5,
        kPostNum        = 6,
        kMidNum         = 7,
        kPreMidNum      = 8,
        kBlank          = 9,
        kLF             = 10,
        kKata           = 11,
        kHira           = 12,
        kKanji          = 13,
        kDiacrit        = 14,
        kCR             = 15,
        kNsm            = 16,
        kwEOS           = 17,
        kWordCol_count  = 18
    };

    static Node                     kWordForwardData[];
    static const int32_t            kWordForwardData_length;
    static WordBreakTable*          kWordForward;
    static Node                     kWordBackwardData[];
    static const int32_t            kWordBackwardData_length;
    static WordBreakTable*          kWordBackward;
    static Type                     kWordRawMapping[];
    static const int32_t            kWordRawMapping_length;
    static SpecialMapping           kWordExceptionChar[];
    static const int32_t            kWordExceptionChar_length;
    static UnicodeClassMapping*     kWordMap;
    static Type                     kWordAsciiValues[];
    static const bool_t             kWordExceptionFlags[];

private:
    // Sentence data
    enum SentenceMapping
    {
        // These enum values must occur in this order; do not
        // modify unless you know what you are doing!  The forward
        // and backward data tables are indexed by these enums.
        kOther              = 0,
        kSpace              = 1,
        kTerminator         = 2,
        kAmbiguousTerm      = 3,
        kOpenBracket        = 4,
        kCloseBracket       = 5,
        kCJK                = 6,
        kParagraphBreak     = 7,
        kLowerCase          = 8,
        kUpperCase          = 9,
        ksNumber            = 10,
        kQuote              = 11,
        //ksCR,
        ksNsm               = 12,
        ksEOS               = 13,
        kSentenceCol_count  = 14
    };

    static Node                     kSentenceForwardData[];
    static const int32_t            kSentenceForwardData_length;
    static WordBreakTable*          kSentenceForward;
    static Node                     kSentenceBackwardData[];
    static const int32_t            kSentenceBackwardData_length;
    static WordBreakTable*          kSentenceBackward;
    static Type                     kSentenceRawMapping[];
    static const int32_t            kSentenceRawMapping_length;
    static SpecialMapping           kSentenceExceptionChar[];
    static const int32_t            kSentenceExceptionChar_length;
    static UnicodeClassMapping*     kSentenceMap;
    static Type                     kSentenceAsciiValues[];
    static const bool_t             kSentenceExceptionFlags[];

private:
    // Line data
    enum LineMapping
    {
        // These enum values must occur in this order; do not
        // modify unless you know what you are doing!  The forward
        // and backward data tables are indexed by these enums.
        kLineBreak,
        //always breaks (must be present as first item)
        kLineBlank,
        //spaces, tabs, nulls.
        kLineCR,
        //carriage return
        kLineNonBlank,
        //everything not included elsewhere
        kLineOp,
        //hyphens....
        kLineJwrd,
        //hiragana, katakana, and kanji
        kLinePreJwrd,
        //characters that bind to the beginning of a Japanese word
        kLinePostJwrd,
        //characters that bind to the end of a Japanese word
        kLineDigit,
        //digits
        kLineNumPunct,
        //punctuation that can appear within a number
        kLineCurrency,
        //currency symbols that can precede a number
        kLineNsm,
        // non-spacing marks
        kLineNbsp,
        // non-breaking characters
        kLineEOS,
        kLineCol_count
    };

    static Node                     kLineForwardData[];
    static const int32_t            kLineForwardData_length;
    static WordBreakTable*          kLineForward;
    static Node                     kLineBackwardData[];
    static const int32_t            kLineBackwardData_length;
    static WordBreakTable*          kLineBackward;
    static Type                     kLineRawMapping[];
    static const int32_t            kLineRawMapping_length;
    static SpecialMapping           kLineExceptionChar[];
    static const int32_t            kLineExceptionChar_length;
    static const bool_t             kLineExceptionFlags[];
    static UnicodeClassMapping*     kLineMap;
    static Type                     kLineAsciiValues[];

protected:
    /**
     * Copy constructor and assignment operator provided to make
     * compiler happy only. DO NOT CALL.
     */
    TextBoundaryData(const TextBoundaryData&) {}
    TextBoundaryData& operator=(const TextBoundaryData&) { return *this; }
    TextBoundaryData() {} // Do not subclass
    TextBoundaryData(const WordBreakTable* forward,
                     const WordBreakTable* backward,
                     const UnicodeClassMapping* map)
                     : fForward(forward), fBackward(backward), fMap(map) {}
        
private:
    const WordBreakTable*       fForward;
    const WordBreakTable*       fBackward;
    const UnicodeClassMapping*  fMap;
};

inline const WordBreakTable* TextBoundaryData::forward() const
{
    return fForward;
}

inline const WordBreakTable* TextBoundaryData::backward() const
{
    return fBackward;
}

inline const UnicodeClassMapping* TextBoundaryData::map() const
{
    return fMap;
}

// These used to be static consts in the class, but some compilers didn't like that.
#define kStop       (0)
#define kSI         (0x80)
#define kSI_Stop    (kSI+kStop)

#define kSI_1       (kSI+1)
#define kSI_2       (kSI+2)
#define kSI_3       (kSI+3)
#define kSI_4       (kSI+4)
#define kSI_5       (kSI+5)
#define kSI_6       (kSI+6)
#define kSI_7       (kSI+7)
#define kSI_8       (kSI+8)
#define kSI_9       (kSI+9)
#define kSI_10      (kSI+10)
#define kSI_11      (kSI+11)
#define kSI_12      (kSI+12)
#define kSI_13      (kSI+13)
#define kSI_14      (kSI+14)

#endif // _TXTBDAT
//eof
