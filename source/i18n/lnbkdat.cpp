/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File LNBKDAT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.
*                           Recoded kRawMapping table for Unicode::getType() type codes.
*                           Made static data members const where appropriate.
*   03/25/97    aliu        Moved into TextBoundaryData; no longer a subclass.
*   04/15/97    aliu        Worked around bug in AIX xlC compiler which occurs if static
*                           arrays contain const elements.
*   05/06/97    aliu        Made SpecialMapping an array of objects instead of pointers,
*                           to help out non-compliant compilers.
*   08/14/98    helena      Sync-up JDK1.2.
*   07/12/99    helena      HPUX 11 CC port.
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file LineBreakData.java
// *****************************************************************************

#include "txtbdat.h"
#include "wdbktbl.h"
#include "unicdcm.h"

// *****************************************************************************
// class LineBreakData
// The following tables contain the transition state data for line break.
// For more detailed explanation on the boundary break state machine, please
// see the internal documentation of wdbktbl.cpp.
// *****************************************************************************

// The forward transition states of line boundary data.
TextBoundaryData::Node TextBoundaryData::kLineForwardData[] =
{
        // brk          bl              cr              nBl
        // op           kan             prJ             poJ
        // dgt          np              curr            nsm
        // nbsp         EOS
        /*00*/
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,
        /*01*/
        kSI_4,          kSI_2,          kSI_7,          kSI_3,
        kSI_6,          kSI_5,          kSI_1,          kSI_8,
        kSI_9,          kSI_8,          kSI_1,          kSI_1,
        kSI_1,          kSI_Stop,
        /*02*/
        kSI_4,          kSI_2,          kSI_7,          kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_2,
        kSI_1,          kSI_Stop,
        /*03*/
        kSI_4,          kSI_2,          kSI_7,          kSI_3,
        kSI_6,          kSI_Stop,       kSI_Stop,       kSI_8,
        kSI_9,          kSI_8,          kSI_Stop,       kSI_3,
        kSI_1,          kSI_Stop,
        /*04*/
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,
        /*05*/
        kSI_4,          kSI_2,          kSI_7,          kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_8,
        kSI_Stop,       kSI_8,          kSI_Stop,       kSI_5,
        kSI_1,          kSI_Stop,
        /*06*/
        kSI_4,          kSI_Stop,       kSI_7,          kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_9,          kSI_Stop,       kSI_11,         kSI_6,
        kSI_1,          kSI_Stop,
        /*07*/
        kSI_4,          kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,
        /*08*/
        kSI_4,          kSI_2,          kSI_7,          kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_8,
        kSI_Stop,       kSI_8,          kSI_Stop,       kSI_8,
        kSI_1,          kSI_Stop,
        /*09*/
        kSI_4,          kSI_2,          kSI_7,          kSI_3,
        kSI_6,          kSI_Stop,       kSI_Stop,       kSI_8,
        kSI_9,          kSI_10,         kSI_10,         kSI_9,
        kSI_1,          kSI_Stop,
        /*10*/
        kSI_4,          kSI_2,          kSI_7,          kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_8,
        kSI_9,          kSI_8,          kSI_Stop,       kSI_10,
        kSI_1,          kSI_Stop,
        /*11*/
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kSI_9,          kStop,          kStop,          11,
        kSI_1,          kStop
};

const int32_t TextBoundaryData::kLineForwardData_length =
    sizeof(TextBoundaryData::kLineForwardData) / sizeof(TextBoundaryData::kLineForwardData[0]);

WordBreakTable* TextBoundaryData::kLineForward =
    new WordBreakTable(kLineCol_count, kLineForwardData, kLineForwardData_length);

// The backward transition states of line boundary data.
TextBoundaryData::Node TextBoundaryData::kLineBackwardData[] =
{
        // brk          bl              cr              nBl
        // op           kan             prJ             poJ
        // dgt          np              curr            nsm
        // nbsp         EOS
        /*00*/
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,
        /*01*/
        kSI_1,          kSI_1,          kSI_1,          kSI_2,
        kSI_2,          kSI_4,          kSI_2,          kSI_3,
        kSI_2,          kSI_3,          kSI_2,          kSI_1,
        kSI_2,          kStop,
        /*02*/
        kStop,          kStop,          kStop,          kSI_2,
        kSI_2,          kStop,          kSI_2,          kSI_3,
        kSI_2,          kSI_3,          kSI_2,          kSI_2,
        kSI_2,          kStop,
        /*03*/
        kStop,          kStop,          kStop,          kSI_2,
        kStop,          kSI_4,          kSI_2,          kSI_3,
        kSI_2,          kSI_3,          kSI_2,          kSI_3,
        kSI_2,          kStop,
        /*04*/
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kSI_2,          kStop,
        kStop,          kStop,          kSI_2,          kSI_4,
        kSI_4,          kStop
};

const int32_t TextBoundaryData::kLineBackwardData_length =
    sizeof(TextBoundaryData::kLineBackwardData) / sizeof(TextBoundaryData::kLineBackwardData[0]);

WordBreakTable* TextBoundaryData::kLineBackward =
    new WordBreakTable(kLineCol_count, kLineBackwardData, kLineBackwardData_length);

// The line type mapping of the break table.
TextBoundaryData::Type TextBoundaryData::kLineRawMapping[] = {
    // Re-coded to match Unicode 2 types [LIU]
    kLineNonBlank,  // UNASSIGNED               = 0,
    kLineNonBlank,  // UPPERCASE_LETTER         = 1,
    kLineNonBlank,  // LOWERCASE_LETTER         = 2,
    kLineNonBlank,  // TITLECASE_LETTER         = 3,
    kLineNonBlank,  // MODIFIER_LETTER          = 4,
    kLineNonBlank,  // OTHER_LETTER             = 5,
    kLineNsm,       // NON_SPACING_MARK         = 6,
    kLineNsm,       // ENCLOSING_MARK           = 7,
    kLineNonBlank,  // COMBINING_SPACING_MARK   = 8,
    kLineDigit,     // DECIMAL_DIGIT_NUMBER     = 9,
    kLineNonBlank,  // LETTER_NUMBER            = 10,
    kLineDigit,     // OTHER_NUMBER             = 11,
    kLineBlank,     // SPACE_SEPARATOR          = 12,
    kLineBlank,     // LINE_SEPARATOR           = 13,
    kLineBlank,     // PARAGRAPH_SEPARATOR      = 14,
    kLineBlank,     // CONTROL                  = 15,
    kLineNonBlank,  // FORMAT                   = 16,
    kLineNonBlank,  // PRIVATE_USE              = 17,
    kLineNonBlank,  // SURROGATE                = 18,
    kLineOp,        // DASH_PUNCTUATION         = 19,
    kLinePreJwrd,   // START_PUNCTUATION        = 20,
    kLinePostJwrd,  // END_PUNCTUATION          = 21,
    kLineNonBlank,  // CONNECTOR_PUNCTUATION    = 22,
    kLineNonBlank,  // OTHER_PUNCTUATION        = 23,
    kLineNonBlank,  // MATH_SYMBOL              = 24,
    kLinePreJwrd,   // CURRENCY_SYMBOL          = 25,
    kLineNonBlank,  // MODIFIER_SYMBOL          = 26,
    kLineNonBlank,  // OTHER_SYMBOL             = 27,
    kLineNonBlank   // UNDEFINED                = 28
};

const int32_t TextBoundaryData::kLineRawMapping_length =
    sizeof(TextBoundaryData::kLineRawMapping)/sizeof(TextBoundaryData::kLineRawMapping[0]);

// The exceptions of the line break data.
SpecialMapping TextBoundaryData::kLineExceptionChar[] = {
        //note: the ranges in this table must be sorted in ascending order as
        //      required by the UnicodeClassMapping class.
        SpecialMapping(TextBoundaryData::ASCII_END_OF_TEXT, TextBoundaryData::kLineBreak),
        SpecialMapping(TextBoundaryData::ASCII_HORIZONTAL_TABULATION,
                       TextBoundaryData::ASCII_FORM_FEED, TextBoundaryData::kLineBreak),
        SpecialMapping(TextBoundaryData::ASCII_CARRIAGE_RETURN, TextBoundaryData::kLineCR),
        SpecialMapping(TextBoundaryData::ASCII_EXCLAMATION_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::ASCII_DOLLAR_SIGN, TextBoundaryData::kLinePreJwrd),
        SpecialMapping(TextBoundaryData::ASCII_PERCENT, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::ASCII_COMMA, TextBoundaryData::kLineNumPunct),
        SpecialMapping(TextBoundaryData::ASCII_FULL_STOP, TextBoundaryData::kLineNumPunct),
        SpecialMapping(TextBoundaryData::ASCII_COLON, TextBoundaryData::ASCII_SEMICOLON, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::ASCII_QUESTION_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::ASCII_NONBREAKING_SPACE, TextBoundaryData::kLineNbsp),
        SpecialMapping(TextBoundaryData::ASCII_CENT_SIGN, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::LATIN1_SOFTHYPHEN, TextBoundaryData::kLineOp),
        SpecialMapping(TextBoundaryData::LATIN1_DEGREE_SIGN, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::ARABIC_PERCENT_SIGN, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::FIGURE_SPACE, TextBoundaryData::kLineNbsp),
        SpecialMapping(TextBoundaryData::NONBREAKING_HYPHEN, TextBoundaryData::kLineNbsp),
        SpecialMapping(TextBoundaryData::PUNCTUATION_LINE_SEPARATOR,
                       TextBoundaryData::PUNCTUATION_PARAGRAPH_SEPARATOR, TextBoundaryData::kLineBreak),
        SpecialMapping(TextBoundaryData::PER_MILLE_SIGN, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::PER_TEN_THOUSAND_SIGN, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::PRIME, TextBoundaryData::TRIPLE_PRIME, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::DEGREE_CELSIUS, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::DEGREE_FAHRENHEIT, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::PUNCTUATION_IDEOGRAPHIC_COMMA,
                       TextBoundaryData::PUNCTUATION_IDEOGRAPHIC_FULL_STOP, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::IDEOGRAPHIC_ITERATION_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_A, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_A, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_I, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_I, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_U, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_U, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_E, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_E, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_O, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_O, TextBoundaryData::HIRAGANA_LETTER_DI, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_TU, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_TU, TextBoundaryData::HIRAGANA_LETTER_MO, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_YA, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_YA, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_YU, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_YU, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_YO, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_YO, TextBoundaryData::HIRAGANA_LETTER_RO, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_WA, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_WA, TextBoundaryData::HIRAGANA_LETTER_VU, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::COMBINING_KATAKANA_HIRAGANA_VOICED_SOUND_MARK,
                       TextBoundaryData::HIRAGANA_SEMIVOICED_SOUND_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::HIRAGANA_ITERATION_MARK, TextBoundaryData::HIRAGANA_VOICED_ITERATION_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_A, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_A, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_I, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_I, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_U, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_U, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_E, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_E, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_O, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_O, TextBoundaryData::KATAKANA_LETTER_DI, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_TU, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_TU, TextBoundaryData::KATAKANA_LETTER_MO, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_YA, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_YA, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_YU, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_YU, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_YO, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_YO, TextBoundaryData::KATAKANA_LETTER_RO, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_WA, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_WA, TextBoundaryData::KATAKANA_LETTER_VU, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_KA, TextBoundaryData::KATAKANA_LETTER_SMALL_KE, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_LETTER_VA, TextBoundaryData::KATAKANA_LETTER_VO, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_HIRAGANA_PROLONGED_SOUND_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::KATAKANA_ITERATION_MARK, TextBoundaryData::KATAKANA_VOICED_ITERATION_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::UNICODE_LOW_BOUND_HAN, TextBoundaryData::UNICODE_HIGH_BOUND_HAN,TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::CJK_COMPATIBILITY_F900,
                       TextBoundaryData::CJK_COMPATIBILITY_FA2D, TextBoundaryData::kLineJwrd),
        SpecialMapping(TextBoundaryData::UNICODE_ZERO_WIDTH_NON_BREAKING_SPACE, TextBoundaryData::kLineNbsp),
        SpecialMapping(TextBoundaryData::FULLWIDTH_EXCLAMATION_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::FULLWIDTH_FULL_STOP, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::FULLWIDTH_QUESTION_MARK, TextBoundaryData::kLinePostJwrd),
        SpecialMapping(TextBoundaryData::END_OF_STRING, TextBoundaryData::kLineEOS)
};

const int32_t TextBoundaryData::kLineExceptionChar_length =
    sizeof(TextBoundaryData::kLineExceptionChar)/sizeof(TextBoundaryData::kLineExceptionChar[0]);

const bool_t TextBoundaryData::kLineExceptionFlags[] = {
        FALSE,          // kNonCharacter            = 0,
        FALSE,          // kUppercaseLetter         = 1,
        FALSE,          // kLowercaseLetter         = 2,
        FALSE,          // kTitlecaseLetter         = 3,
        TRUE,           // kModifierLetter          = 4,
        TRUE,           // kOtherLetter             = 5,
        TRUE,           // kNonSpacingMark          = 6,
        FALSE,          // kEnclosingMark           = 7,
        FALSE,          // kCombiningSpacingMark    = 8,
        FALSE,          // kDecimalNumber           = 9,
        FALSE,          // kLetterNumber            = 10,
        FALSE,          // kOtherNumber             = 11,
        TRUE,           // kSpaceSeparator          = 12,
        TRUE,           // kLineSeparator           = 13,
        TRUE,           // kParagraphSeparator      = 14,
        TRUE,           // kControlCharacter        = 15,
        TRUE,           // kFormatCharacter         = 16,
        FALSE,          // kPrivateUseCharacter     = 17,
        FALSE,          // kSurrogate               = 18,
        TRUE,           // kDashPunctuation         = 19,
        FALSE,          // kOpenPunctuation         = 20,
        FALSE,          // kClosePunctuation        = 21,
        FALSE,          // kConnectorPunctuation    = 22,
        TRUE,           // kOtherPunctuation        = 23,
        FALSE,          // kMathSymbol              = 24,
        TRUE,           // kCurrencySymbol          = 25,
        FALSE,          // kModifierSymbol          = 26,
        TRUE,           // kOtherSymbol             = 27
        FALSE           // UNDEFINED                = 28,
    };

TextBoundaryData::Type TextBoundaryData::kLineAsciiValues[] = {

        //  null            soh             stx             etx             eot             enq             ask             bell
            kLineBlank,     kLineBlank,     kLineBlank,     kLineBreak,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,
        //  bs              ht              lf              vt              ff              cr              so              si
            kLineBlank,     kLineBreak,     kLineBreak,     kLineBreak,     kLineBreak,     kLineCR,        kLineBlank,     kLineBlank,
        //  dle             dc1             dc2             dc3             dc4             nak             syn             etb
            kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,
        //  can             em              sub             esc             fs              gs              rs              us
            kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,
        //  sp              !               "               #               $               %               &               '
            kLineBlank,     kLinePostJwrd,  kLineNonBlank,  kLineNonBlank,  kLineCurrency,  kLinePostJwrd,  kLineNonBlank,  kLineNonBlank,
        //  (               )               *               +               ,               -               .               /
            kLinePreJwrd,   kLinePostJwrd,  kLineNonBlank,  kLineNonBlank,  kLineNumPunct,  kLineOp,        kLineNumPunct,  kLineNonBlank,
        //  0               1               2               3               4               5               6               7
            kLineDigit,     kLineDigit,     kLineDigit,     kLineDigit,     kLineDigit,     kLineDigit,     kLineDigit,     kLineDigit,
        //  8               9               :               ;               <               =               >               ?
            kLineDigit,     kLineDigit,     kLinePostJwrd,  kLinePostJwrd,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,   kLinePostJwrd,
        //  @               A               B               C               D               E               F               G
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  H               I               J               K               L               M               N               O
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  P               Q               R               S               T               U               V               W
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  X               Y               Z               [               \               ]               ^               _
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLinePreJwrd,   kLineNonBlank,  kLinePostJwrd,  kLineNonBlank,  kLineNonBlank,
        //  `               a               b               c               d               e               f               g
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  h               i               j               k               l               m               n               o
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  p               q               r               s               t               u               v               w
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  x               y               z               {               |               }               ~               del
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLinePreJwrd,   kLineNonBlank,  kLinePostJwrd,  kLineNonBlank,  kLineBlank,
        //  ctrl            ctrl            ctrl            ctrl            ctrl            ctrl            ctrl            ctrl
            kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,
        //  ctrl            ctrl            ctrl            ctrl            ctrl            ctrl            ctrl            ctrl
            kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,
        //  ctrl            ctrl            ctrl            ctrl            ctrl            ctrl            ctrl            ctrl
            kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,
        //  ctrl            ctrl            ctrl            ctrl            ctrl            ctrl            ctrl            ctrl
            kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,     kLineBlank,
        //  kLineNbsp       inv-!           cents           pounds          currency        yen             broken-bar      section
            kLineNbsp,      kLineNonBlank,  kLinePostJwrd,  kLineCurrency,  kLineCurrency,  kLineCurrency,  kLineNonBlank,  kLineNonBlank,
        //  umlaut          copyright       super-a         gui-left        not             soft-hyph       registered      macron
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLinePreJwrd,   kLineNonBlank,  kLineOp,        kLineNonBlank,  kLineNonBlank,
        //  degree          +/-             super-2         super-3         acute           micro           paragraph       bullet
            kLinePostJwrd,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  cedilla         super-1         super-o         gui-right       1/4             1/2             3/4             inv-?
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLinePostJwrd,  kLineDigit,     kLineDigit,     kLineDigit,     kLineNonBlank,
        //  A-grave         A-acute         A-hat           A-tilde         A-umlaut        A-ring          AE              C-cedilla
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  E-grave         E-acute         E-hat           E-umlaut        I-grave         I-acute         I-hat           I-umlaut
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  Edh             N-tilde         O-grave         O-acute         O-hat           O-tilde         O-umlaut        times
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  O=slash         U-grave         U-acute         U-hat           U-umlaut        Y-acute         Thorn           ess-zed
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  a-grave         a-acute         a-hat           a-tilde         a-umlaut        a-ring          ae              c-cedilla
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  e-grave         e-acute         e-hat           e-umlaut        i-grave         i-acute         i-hat           i-umlaut
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  edh             n-tilde         o-grave         o-acute         o-hat           o-tilde         o-umlaut        over
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,
        //  o-slash         u-grave         u-acute         u-hat           u-umlaut        y-acute         thorn           y-umlaut
            kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank,  kLineNonBlank
    };


UnicodeClassMapping* TextBoundaryData::kLineMap =
    new UnicodeClassMapping(kLineRawMapping, kLineRawMapping_length, 
                            kLineExceptionChar, kLineExceptionChar_length,
                            kLineExceptionFlags,
                            kLineAsciiValues );

/**
 * This is the single instance of TextBoundaryData containing line
 * break data.
 */
const TextBoundaryData TextBoundaryData::kLineBreakData(TextBoundaryData::kLineForward,
                                                        TextBoundaryData::kLineBackward,
                                                        TextBoundaryData::kLineMap);

//eof
