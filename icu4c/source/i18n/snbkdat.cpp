/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File SNBKDAT.CPP
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
*    09/04/98    stephen        Sync with 8/31 JDK 1.2
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file SentenceBreakData.java
// *****************************************************************************

#include "txtbdat.h"
#include "wdbktbl.h"
#include "unicdcm.h"

// *****************************************************************************
// class SentenceBreakData
// The following tables contain the transition state data for sentence break.
// For more detailed explanation on the boundary break state machine, please
// see the internal documentation of wdbktbl.cpp.
// *****************************************************************************

// The forward transition states of sentence boundary data.
TextBoundaryData::Node TextBoundaryData::kSentenceForwardData[] = {
        // other       space          terminator     ambTerm
        // open        close          CJK            PB
        // lower       upper          digit          Quote
        // nsm            EOS

        // 0
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,

        // 1
        kSI_1,          kSI_1,          kSI_2,          kSI_5,
        kSI_1,          kSI_1,          kSI_1,          kSI_4,
        kSI_1,          kSI_1,          kSI_1,          kSI_1,
        kSI_1,          kSI_Stop,

        // 2
        kSI_Stop,       kSI_3,          kSI_2,          kSI_5,
        kSI_Stop,       kSI_2,          kSI_Stop,       kSI_4,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_2,
        kSI_2,          kSI_Stop,

        // 3
        kSI_Stop,       kSI_3,          kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_4,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_3,          kSI_Stop,

        // 4
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,

        // 5
        kSI_1,          kSI_6,          kSI_2,          kSI_5,
        kSI_1,          kSI_5,          kSI_Stop,       kSI_4,
        kSI_1,          kSI_1,          kSI_1,          kSI_5,
        kSI_5,          kSI_Stop,

        // 6
        kSI_Stop,       kSI_6,          kSI_Stop,       kSI_Stop,
        kSI_7,          kSI_1,          kSI_Stop,       kSI_4,
        kSI_1,          kSI_Stop,       kSI_1,          kSI_Stop,
        kSI_6,          kSI_Stop,

        // 7
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        7,              kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_1,          kStop,          kSI_Stop,       kSI_Stop,
        kSI_7,          kSI_Stop,

        // 8
        kSI_1,          kSI_1,          kSI_2,          kSI_8,
        kSI_1,          kSI_5,          kSI_1,          kSI_4,
        kSI_1,          kSI_8,          kSI_9,          kSI_5,
        kSI_8,          kSI_Stop,

        // 9
        kSI_1,          kSI_1,          kSI_2,          kSI_9,
        kSI_1,          kSI_5,          kSI_1,          kSI_4,
        kSI_1,          kSI_1,          kSI_9,          kSI_5,
        kSI_9,          kSI_Stop
};

const int32_t TextBoundaryData::kSentenceForwardData_length =
    sizeof(TextBoundaryData::kSentenceForwardData) / sizeof(TextBoundaryData::kSentenceForwardData[0]);

WordBreakTable* TextBoundaryData::kSentenceForward = 
    new WordBreakTable(kSentenceCol_count, kSentenceForwardData, kSentenceForwardData_length);

// The backward transition states of sentence boundary data.
TextBoundaryData::Node TextBoundaryData::kSentenceBackwardData[] = {
        // other       space          terminator     ambTerm
        // open        close          CJK            PB
        // lower       upper          digit          quote
        // nsm            EOS

        // 0
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,

        // 1
        kSI_2,          kSI_2,          kSI_2,          kSI_2,
        kSI_2,          kSI_2,          kSI_3,          kStop,
        kSI_2,          kSI_3,          kSI_2,          kSI_2,
        kSI_1,          kStop,

        // 2
        kSI_2,          kSI_2,          kSI_2,          kSI_2,
        kSI_2,          kSI_2,          kSI_3,          kStop,
        kSI_2,          kSI_3,          kSI_2,          kSI_2,
        kSI_2,          kStop,

        // 3
        kSI_2,          kSI_4,          kSI_2,          kSI_2,
        kSI_2,          kSI_2,          kSI_3,          kStop,
        kSI_3,          kSI_2,          kSI_2,          kSI_2,
        kSI_3,          kStop,

        // 4
        kSI_2,          kSI_4,          kSI_Stop,       kSI_Stop,
        kSI_2,          kSI_2,          kSI_3,          kStop,
        kSI_2,          kSI_3,          kSI_2,      kSI_2,
        kSI_4,          kStop
};

const int32_t TextBoundaryData::kSentenceBackwardData_length =
    sizeof(TextBoundaryData::kSentenceBackwardData) / sizeof(TextBoundaryData::kSentenceBackwardData[0]);

WordBreakTable* TextBoundaryData::kSentenceBackward = new WordBreakTable(kSentenceCol_count, kSentenceBackwardData, kSentenceBackwardData_length);

// The sentence type mapping of the break table.
TextBoundaryData::Type TextBoundaryData::kSentenceRawMapping[] = {
    // Re-coded to match Unicode 2 types [LIU]
    kOther,         // UNASSIGNED               = 0,
    kUpperCase,     // UPPERCASE_LETTER         = 1,
    kLowerCase,     // LOWERCASE_LETTER         = 2,
    kOther,         // TITLECASE_LETTER         = 3,
    kOther,         // MODIFIER_LETTER          = 4,
    kOther,         // OTHER_LETTER             = 5,
    ksNsm,          // NON_SPACING_MARK         = 6,
    ksNsm,          // ENCLOSING_MARK           = 7,
    kOther,         // COMBINING_SPACING_MARK   = 8,
    ksNumber,       // DECIMAL_DIGIT_NUMBER     = 9,
    ksNumber,       // LETTER_NUMBER            = 10,
    ksNumber,       // OTHER_NUMBER             = 11,
    kSpace,         // SPACE_SEPARATOR          = 12,
    kSpace,         // LINE_SEPARATOR           = 13,
    kSpace,         // PARAGRAPH_SEPARATOR      = 14,
    kOther,         // CONTROL                  = 15,
    kOther,         // FORMAT                   = 16,
    kOther,         // PRIVATE_USE              = 17,
    kOther,         // SURROGATE                = 18,
    kOther,         // DASH_PUNCTUATION         = 19,
    kOpenBracket,   // START_PUNCTUATION        = 20,
    kCloseBracket,  // END_PUNCTUATION          = 21,
    kOther,         // CONNECTOR_PUNCTUATION    = 22,
    kOther,         // OTHER_PUNCTUATION        = 23,
    kOther,         // MATH_SYMBOL              = 24,
    kOther,         // CURRENCY_SYMBOL          = 25,
    kOther,         // MODIFIER_SYMBOL          = 26,
    kOther,         // OTHER_SYMBOL             = 27,
    kOther          // UNDEFINED                = 28
};

const int32_t TextBoundaryData::kSentenceRawMapping_length =
    sizeof(TextBoundaryData::kSentenceRawMapping) / sizeof(TextBoundaryData::kSentenceRawMapping[0]);

// The exceptions of the sentence break data.
SpecialMapping TextBoundaryData::kSentenceExceptionChar[] = {
        //note: the ranges in this table must be sorted in ascending order
        //as required by the UnicodeClassMapping class.
    SpecialMapping(TextBoundaryData::ASCII_HORIZONTAL_TABULATION, TextBoundaryData::kSpace),
    SpecialMapping(TextBoundaryData::ASCII_LINEFEED, TextBoundaryData::kSpace),
    SpecialMapping(TextBoundaryData::ASCII_FORM_FEED, TextBoundaryData::kTerminator),
    SpecialMapping(TextBoundaryData::ASCII_CARRIAGE_RETURN, TextBoundaryData::kSpace),
    SpecialMapping(TextBoundaryData::ASCII_EXCLAMATION_MARK, TextBoundaryData::kTerminator),
    SpecialMapping(TextBoundaryData::ASCII_QUOTATION_MARK, TextBoundaryData::kQuote),
    SpecialMapping(TextBoundaryData::ASCII_APOSTROPHE, TextBoundaryData::kQuote),
    SpecialMapping(TextBoundaryData::ASCII_FULL_STOP, TextBoundaryData::kAmbiguousTerm),
    SpecialMapping(TextBoundaryData::ASCII_QUESTION_MARK, TextBoundaryData::kTerminator),
    SpecialMapping(TextBoundaryData::ASCII_NONBREAKING_SPACE, TextBoundaryData::kOther),
    SpecialMapping(TextBoundaryData::PUNCTUATION_LINE_SEPARATOR, TextBoundaryData::kSpace),
    SpecialMapping(TextBoundaryData::PUNCTUATION_PARAGRAPH_SEPARATOR, TextBoundaryData::kParagraphBreak),
    SpecialMapping(TextBoundaryData::PUNCTUATION_IDEOGRAPHIC_FULL_STOP, TextBoundaryData::kTerminator),
    SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_A, 
                   TextBoundaryData::HIRAGANA_LETTER_VU, TextBoundaryData::kCJK),
    SpecialMapping(TextBoundaryData::COMBINING_KATAKANA_HIRAGANA_VOICED_SOUND_MARK,
                   TextBoundaryData::HIRAGANA_SEMIVOICED_SOUND_MARK, TextBoundaryData::kCJK),         // cjk
    SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_A, 
                   TextBoundaryData::KATAKANA_LETTER_SMALL_KE, TextBoundaryData::kCJK),   // cjk
    SpecialMapping(TextBoundaryData::UNICODE_LOW_BOUND_HAN, 
                   TextBoundaryData::UNICODE_HIGH_BOUND_HAN, TextBoundaryData::kCJK),
    SpecialMapping(TextBoundaryData::CJK_COMPATIBILITY_F900,
                   TextBoundaryData::CJK_COMPATIBILITY_FA2D,TextBoundaryData::kCJK),
    SpecialMapping(TextBoundaryData::UNICODE_ZERO_WIDTH_NON_BREAKING_SPACE, TextBoundaryData::kOther),
    SpecialMapping(TextBoundaryData::FULLWIDTH_EXCLAMATION_MARK, TextBoundaryData::kTerminator),
    SpecialMapping(TextBoundaryData::FULLWIDTH_FULL_STOP, TextBoundaryData::kAmbiguousTerm),
    SpecialMapping(TextBoundaryData::FULLWIDTH_QUESTION_MARK, TextBoundaryData::kTerminator),
    SpecialMapping(TextBoundaryData::END_OF_STRING, TextBoundaryData::ksEOS)
};

const bool_t TextBoundaryData::kSentenceExceptionFlags[] = {
    FALSE,            // kNonCharacter         = 0,
    FALSE,            // kUppercaseLetter      = 1,
    FALSE,            // kLowercaseLetter      = 2,
    FALSE,            // kTitlecaseLetter      = 3,
    FALSE,            // kModifierLetter       = 4,
    TRUE,             // kOtherLetter          = 5,
    TRUE,             // kNonSpacingMark       = 6,
    FALSE,            // kEnclosingMark        = 7,
    FALSE,            // kCombiningSpacingMark = 8,
    FALSE,            // kDecimalNumber        = 9,
    FALSE,            // kLetterNumber         = 10,
    FALSE,            // kOtherNumber          = 11,
    TRUE,             // kSpaceSeparator       = 12,
    TRUE,             // kLineSeparator        = 13,
    TRUE,             // kParagraphSeparator   = 14,
    TRUE,             // kControlCharacter     = 15,
    TRUE,             // kFormatCharacter      = 16,
    FALSE,            // kPrivateUseCharacter  = 17,
    FALSE,            // kSurrogate            = 18,
    FALSE,            // kDashPunctuation      = 19,
    FALSE,            // kOpenPunctuation      = 20,
    FALSE,            // kClosePunctuation     = 21,
    FALSE,            // kConnectorPunctuation = 22,
    TRUE,             // kOtherPunctuation     = 23,
    FALSE,            // kMathSymbol           = 24,
    FALSE,            // kCurrencySymbol       = 25,
    FALSE,            // kModifierSymbol       = 26,
    FALSE,             // kOtherSymbol          = 27
    FALSE            // UNDEFINED             = 28,
};
const int32_t TextBoundaryData::kSentenceExceptionChar_length =
    sizeof(TextBoundaryData::kSentenceExceptionChar) / sizeof(TextBoundaryData::kSentenceExceptionChar[0]);

TextBoundaryData::Type TextBoundaryData::kSentenceAsciiValues[] = {
        //  null        soh         stx         etx         eot         enq         ask         bell
            kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,
        //  bs          ht          lf          vt          ff          cr          so          si
            kOther,     kSpace,     kSpace,     kOther,     kTerminator, kSpace,    kOther,     kOther,
        //  dle         dc1         dc2         dc3         dc4         nak         syn         etb
            kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,
        //  can         em          sub         esc         fs          gs          rs          us
            kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,
        //  sp          !           "           #           $           %           &           '
            kSpace,     kTerminator, kQuote,    kOther,     kOther,     kOther,     kOther,     kQuote,
        //  (           )           *           +           ,           -           .           /
            kOpenBracket, kCloseBracket, kOther, kOther,    kOther,     kOther,      kAmbiguousTerm, kOther,
        //  0           1           2           3           4           5           6           7
            ksNumber,   ksNumber,   ksNumber,   ksNumber,   ksNumber,   ksNumber,   ksNumber,   ksNumber,
        //  8           9           :           ;           <           =           >           ?
            ksNumber,   ksNumber,   kOther,     kOther,     kOther,     kOther,     kOther,     kTerminator,
        //  @           A           B           C           D           E           F           G
            kOther,     kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase,
        //  H           I           J           K           L           M           N           O
            kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase,
        //  P           Q           R           S           T           U           V           W
            kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase,
        //  X           Y           Z           [           \           ]           ^           _
            kUpperCase, kUpperCase, kUpperCase, kOpenBracket, kOther,   kCloseBracket, kOther,  kOther,
        //  `           a           b           c           d           e           f           g
            kOther,     kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase,
        //  h           i           j           k           l           m           n           o
            kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase,
        //  p           q           r           s           t           u           v           w
            kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase,
        //  x           y           z           {           |           }           ~           del
            kLowerCase, kLowerCase, kLowerCase, kOpenBracket, kOther,   kCloseBracket, kOther,  kOther,
        //  ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl
            kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,
        //  ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl
            kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,
        //  ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl
            kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,
        //  ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl
            kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,
        //  nbsp        inv-!       cents       pounds      currency    yen         broken-bar  section
            kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,     kOther,
        //  umlaut      copyright   super-a     gui-left    not         soft-hyph   registered  macron
            kOther,     kOther,     kLowerCase, kOpenBracket, kOther,   kOther,     kOther,     kOther,
        //  degree      +/-         super-2     super-3     acute       micro       paragraph   bullet
            kOther,     kOther,     ksNumber,   ksNumber,   kOther,     kLowerCase, kOther,     kOther,
        //  cedilla     super-1     super-o     gui-right   1/4         1/2         3/4         inv-?
            kOther,     kLowerCase, kOther,     kCloseBracket, ksNumber, ksNumber,  ksNumber,   kOther,
        //  A-grave     A-acute     A-hat       A-tilde     A-umlaut    A-ring      AE          C-cedilla
            kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase,
        //  E-grave     E-acute     E-hat       E-umlaut    I-grave     I-acute     I-hat       I-umlaut
            kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase,
        //  Edh         N-tilde     O-grave     O-acute     O-hat       O-tilde     O-umlaut    times
            kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kOther,
        //  O=slash     U-grave     U-acute     U-hat       U-umlaut    Y-acute     Thorn       ess-zed
            kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kUpperCase, kLowerCase,
        //  a-grave     a-acute     a-hat       a-tilde     a-umlaut    a-ring      ae          c-cedilla
            kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase,
        //  e-grave     e-acute     e-hat       e-umlaut    i-grave     i-acute     i-hat       i-umlaut
            kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase,
        //  edh         n-tilde     o-grave     o-acute     o-hat       o-tilde     o-umlaut    over
            kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kOther,
        //  o-slash     u-grave     u-acute     u-hat       u-umlaut    y-acute     thorn       y-umlaut
            kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase, kLowerCase
    };

UnicodeClassMapping* TextBoundaryData::kSentenceMap = 
    new UnicodeClassMapping(kSentenceRawMapping, kSentenceRawMapping_length, 
                            kSentenceExceptionChar, kSentenceExceptionChar_length,
                            kSentenceExceptionFlags,
                            kSentenceAsciiValues);

/**
 * This is the single instance of TextBoundaryData containing sentence
 * break data.
 */
const TextBoundaryData TextBoundaryData::kSentenceBreakData(TextBoundaryData::kSentenceForward,
                                                            TextBoundaryData::kSentenceBackward,
                                                            TextBoundaryData::kSentenceMap);

//eof
