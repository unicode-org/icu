/*
*****************************************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*
* File WDBKDAT.CPP
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
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file WordBreakData.java
// *****************************************************************************

#include "txtbdat.h"
#include "wdbktbl.h"
#include "unicdcm.h"

// *****************************************************************************
// class WordBreakData
// This class contains the following transition state data for word break.
// For more detailed explanation on the boundary break state machine, please
// see the internal documentation of wdbktbl.cpp.
// *****************************************************************************

// The forward transition states of word boundary data.
TextBoundaryData::Node TextBoundaryData::kWordForwardData[] = {
        // brk          let             num             mLe             mLN
        // prN          poN             mNu             pMN             blk
        // lf           kat             hir             kan             dia
        // cr           nsm             EOS

        // 0
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,

        // 1
        kSI_14,         kSI_2,          kSI_3,          kSI_14,         kSI_14,
        kSI_5,          kSI_14,         kSI_14,         kSI_5,          kSI_6,
        kSI_4,          kSI_10,         kSI_11,         kSI_12,         kSI_9,
        kSI_13,         1,              kSI_Stop,

        // 2
        kSI_Stop,       kSI_2,          kSI_3,          kSI_7,          kSI_7,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_7,          kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       2,              kSI_Stop,

        // 3
        kSI_Stop,       kSI_2,          kSI_3,          kSI_Stop,       kSI_8,
        kSI_Stop,       kSI_14,         kSI_8,          kSI_8,          kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       3,              kSI_Stop,

        // 4
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,

        // 5
        kSI_Stop,       kSI_Stop,       kSI_3,          kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       5,              kSI_Stop,

        // 6
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_6,
        kSI_4,          kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_13,         6,              kSI_Stop,

        // 7
        kStop,          kSI_2,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          7,              kStop,

        // 8
        kStop,          kStop,          kSI_3,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          8,              kStop,

        // 9
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_10,         kSI_11,         kSI_Stop,       kSI_9,
        kSI_Stop,       9,              kSI_Stop,

        // 10
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_10,         kSI_Stop,       kSI_Stop,       kSI_10,
        kSI_Stop,       10,             kSI_Stop,

        // 11
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_11,         kSI_Stop,       kSI_11,
        kSI_Stop,       11,             kSI_Stop,

        // 12
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_12,         kSI_Stop,
        kSI_Stop,       12,             kSI_Stop,

        // 13
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_4,          kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,

        // 14
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       14,             kSI_Stop
};

const int32_t TextBoundaryData::kWordForwardData_length =
    sizeof(TextBoundaryData::kWordForwardData) / sizeof(TextBoundaryData::kWordForwardData[0]);

WordBreakTable* TextBoundaryData::kWordForward = new WordBreakTable(kWordCol_count, kWordForwardData, kWordForwardData_length);

// The forward transition states of word boundary data.
TextBoundaryData::Node TextBoundaryData::kWordBackwardData[] = {
        // brk          let             num             mLe             mLN
        // prN          poN             mNu             pMN             blk
        // lf           kat             hir             kan             dia
        // cr           nsm             EOS

        // 0
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,

        // 1
        kSI_6,          kSI_2,          kSI_3,          kSI_4,          kSI_5,
        kSI_6,          kSI_7,          kSI_7,          kSI_5,          kSI_8,
        kSI_8,          kSI_9,          kSI_10,         kSI_12,         kSI_11,
        kSI_8,          1,              kStop,

        // 2
        kStop,          kSI_2,          kSI_3,          4,              4,
        kStop,          kStop,          kStop,          4,              kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          2,              kStop,

        // 3
        kStop,          kSI_2,          kSI_3,          kStop,          7,
        kSI_Stop,       kStop,          7,              kSI_7,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          3,              kStop,

        // 4
        kStop,          kSI_2,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          4,              kStop,

        // 5
        kStop,          kSI_2,          kSI_3,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          5,              kStop,

        // 6
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          6,              kStop,

        // 7
        kStop,          kStop,          kSI_3,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          7,              kStop,

        // 8
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kSI_8,
        kSI_8,          kStop,          kStop,          kStop,          kStop,
        kSI_8,          8,              kStop,

        // 9
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kSI_9,          kStop,          kStop,          9,
        kStop,          9,              kStop,

        // 10
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kSI_10,         kStop,          10,
        kStop,          10,             kStop,

        // 11
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kSI_9,          kSI_10,         kStop,          kSI_11,
        kStop,          11,             kStop,

        // 12
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kSI_12,         kStop,
        kStop,          12,             kStop
};

const int32_t TextBoundaryData::kWordBackwardData_length =
    sizeof(TextBoundaryData::kWordBackwardData) / sizeof(TextBoundaryData::kWordBackwardData[0]);

WordBreakTable* TextBoundaryData::kWordBackward = new WordBreakTable(kWordCol_count, kWordBackwardData, kWordBackwardData_length);

// The word type mapping of the break table.
TextBoundaryData::Type TextBoundaryData::kWordRawMapping[] = {
    // Re-coded to match Unicode 2 types [LIU]
    kBreak,     // UNASSIGNED               = 0,
    kLetter,    // UPPERCASE_LETTER         = 1,
    kLetter,    // LOWERCASE_LETTER         = 2,
    kLetter,    // TITLECASE_LETTER         = 3,
    kLetter,    // MODIFIER_LETTER          = 4,
    kLetter,    // OTHER_LETTER             = 5,
    kNsm,       // NON_SPACING_MARK         = 6,
    kNsm,       // ENCLOSING_MARK           = 7,
    kBreak,     // COMBINING_SPACING_MARK   = 8,
    kNumber,    // DECIMAL_DIGIT_NUMBER     = 9,
    kLetter,    // LETTER_NUMBER            = 10,
    kNumber,    // OTHER_NUMBER             = 11,
    kBlank,     // SPACE_SEPARATOR          = 12,
    kBreak,     // LINE_SEPARATOR           = 13,
    kBreak,     // PARAGRAPH_SEPARATOR      = 14,
    kBreak,     // CONTROL                  = 15,
    kBreak,     // FORMAT                   = 16,
    kBreak,     // PRIVATE_USE              = 17,
    kBreak,     // SURROGATE                = 18,
    kMidLetter, // DASH_PUNCTUATION       = 19,
    kBreak,     // START_PUNCTUATION        = 20,
    kBreak,     // END_PUNCTUATION          = 21,
    kBreak,     // CONNECTOR_PUNCTUATION    = 22,
    kBreak,     // OTHER_PUNCTUATION        = 23,
    kBreak,     // MATH_SYMBOL              = 24,
    kPreNum,    // CURRENCY_SYMBOL          = 25,
    kBreak,     // MODIFIER_SYMBOL          = 26,
    kBreak,     // OTHER_SYMBOL             = 27,
    kBreak      // UNDEFINED                = 28
};

const int32_t TextBoundaryData::kWordRawMapping_length =
    sizeof(TextBoundaryData::kWordRawMapping) / sizeof(TextBoundaryData::kWordRawMapping[0]);

// The exceptions of the word break data.
SpecialMapping TextBoundaryData::kWordExceptionChar[] = {
        //note: the ranges in this table must be sorted in ascending order
        //as required by the UnicodeClassMapping class.
    SpecialMapping(TextBoundaryData::ASCII_HORIZONTAL_TABULATION, TextBoundaryData::kBlank),
    SpecialMapping(TextBoundaryData::ASCII_LINEFEED, TextBoundaryData::kLF),
    SpecialMapping(TextBoundaryData::ASCII_FORM_FEED, TextBoundaryData::kLF),
    SpecialMapping(TextBoundaryData::ASCII_CARRIAGE_RETURN, TextBoundaryData::kCR),
    SpecialMapping(TextBoundaryData::ASCII_QUOTATION_MARK, TextBoundaryData::kMidLetNum),
    SpecialMapping(TextBoundaryData::ASCII_NUMBER_SIGN, TextBoundaryData::kPreNum),
    SpecialMapping(TextBoundaryData::ASCII_PERCENT, TextBoundaryData::kPostNum),
    SpecialMapping(TextBoundaryData::ASCII_AMPERSAND, TextBoundaryData::kPostNum),
    SpecialMapping(TextBoundaryData::ASCII_APOSTROPHE, TextBoundaryData::kMidLetNum),
    SpecialMapping(TextBoundaryData::ASCII_COMMA, TextBoundaryData::kMidNum),
    SpecialMapping(TextBoundaryData::ASCII_FULL_STOP, TextBoundaryData::kPreMidNum),
    SpecialMapping(TextBoundaryData::ASCII_CENT_SIGN, TextBoundaryData::kPostNum),
    SpecialMapping(TextBoundaryData::LATIN1_SOFTHYPHEN, TextBoundaryData::kMidLetter),
    SpecialMapping(TextBoundaryData::ARABIC_PERCENT_SIGN, TextBoundaryData::kPostNum),
    SpecialMapping(TextBoundaryData::ARABIC_DECIMAL_SEPARATOR, TextBoundaryData::kMidNum),
    SpecialMapping(TextBoundaryData::PUNCTUATION_HYPHENATION_POINT, TextBoundaryData::kMidLetter),
    SpecialMapping(TextBoundaryData::PUNCTUATION_LINE_SEPARATOR,
                   TextBoundaryData::PUNCTUATION_PARAGRAPH_SEPARATOR, TextBoundaryData::kLF),
    SpecialMapping(TextBoundaryData::PER_MILLE_SIGN, TextBoundaryData::kPostNum),
    SpecialMapping(TextBoundaryData::PER_TEN_THOUSAND_SIGN, TextBoundaryData::kPostNum),
    SpecialMapping(TextBoundaryData::IDEOGRAPHIC_ITERATION_MARK, TextBoundaryData::kKanji),
    SpecialMapping(TextBoundaryData::HIRAGANA_LETTER_SMALL_A, 
                   TextBoundaryData::HIRAGANA_LETTER_VU, TextBoundaryData::kHira),
    SpecialMapping(TextBoundaryData::COMBINING_KATAKANA_HIRAGANA_VOICED_SOUND_MARK,
                   TextBoundaryData::HIRAGANA_SEMIVOICED_SOUND_MARK, TextBoundaryData::kDiacrit),
    SpecialMapping(TextBoundaryData::KATAKANA_LETTER_SMALL_A,
                   TextBoundaryData::KATAKANA_LETTER_SMALL_KE, TextBoundaryData::kKata),
    SpecialMapping(TextBoundaryData::UNICODE_LOW_BOUND_HAN,
                   TextBoundaryData::UNICODE_HIGH_BOUND_HAN, TextBoundaryData::kKanji),
    SpecialMapping(TextBoundaryData::HANGUL_SYL_LOW, 
                   TextBoundaryData::HANGUL_SYL_HIGH, TextBoundaryData::kLetter),
    SpecialMapping(TextBoundaryData::CJK_COMPATIBILITY_F900,
                   TextBoundaryData::CJK_COMPATIBILITY_FA2D, TextBoundaryData::kKanji),
    SpecialMapping(TextBoundaryData::END_OF_STRING, TextBoundaryData::kwEOS)
};

const bool_t TextBoundaryData::kWordExceptionFlags[] = {
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
    FALSE,          // kSpaceSeparator          = 12,
    TRUE,           // kLineSeparator           = 13,
    TRUE,           // kParagraphSeparator      = 14,
    TRUE,           // kControlCharacter        = 15,
    FALSE,          // kFormatCharacter         = 16,
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
    FALSE,          // kOtherSymbol             = 27
    FALSE           // UNDEFINED                = 28,
};

const int32_t TextBoundaryData::kWordExceptionChar_length =
    sizeof(TextBoundaryData::kWordExceptionChar) / sizeof(TextBoundaryData::kWordExceptionChar[0]);

TextBoundaryData::Type TextBoundaryData::kWordAsciiValues[] = {
      
        //  null        soh         stx         etx         eot         enq         ask         bell
            kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  bs          ht          kLF         vt          ff          cr          so          si
            kBreak,     kBlank,     kLF,        kBreak,     kLF,        kCR,        kBreak,     kBreak,
        //  dle         dc1         dc2         dc3         dc4         nak         syn         etb
            kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  can         em          sub         esc         fs          gs          rs          us
            kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  sp          !           "           #           $           %           &           '
            kBlank,     kBreak,     kMidLetNum, kPreNum,    kPreNum,    kPostNum,   kPostNum,   kMidLetNum,
        //  (           )           *           +           ,           -           .           /
            kBreak,     kBreak,     kBreak,     kBreak,     kMidNum,    kMidLetter, kPreMidNum, kBreak,
        //  0           1           2           3           4           5           6           7
            kNumber,    kNumber,    kNumber,    kNumber,    kNumber,    kNumber,    kNumber,    kNumber,
        //  8           9           :           ;           <           =           >           ?
            kNumber,    kNumber,    kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  @           A           B           C           D           E           F           G
            kBreak,     kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  H           I           J           K           L           M           N           O
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  P           Q           R           S           T           U           V           W
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  X           Y           Z           [           \           ]           ^           _
            kLetter,    kLetter,    kLetter,    kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  `           a           b           c           d           e           f           g
            kBreak,     kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  h           i           j           k           l           m           n           o
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  p           q           r           s           t           u           v           w
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  x           y           z           {           |           }           ~           del
            kLetter,    kLetter,    kLetter,    kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl
            kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl
            kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl
            kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl        ctrl
            kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,     kBreak,
        //  nbsp        inv-!       cents       pounds      currency    yen         broken-bar  section
            kBlank,     kBreak,     kPostNum,   kPreNum,    kPreNum,    kPreNum,    kBreak,     kBreak,
        //  umlaut      copyright   super-a     gui-left    not         soft-hyph   registered  macron
            kBreak,     kBreak,     kLetter,    kBreak,     kBreak,     kMidLetter, kBreak,     kBreak,
        //  degree      +/-         super-2     super-3     acute       micro       paragraph   bullet
            kBreak,     kBreak,     kNumber,    kNumber,    kBreak,     kLetter,    kBreak,     kBreak,
        //  cedilla     super-1     super-o     gui-right   1/4         1/2         3/4         inv-?
            kBreak,     kLetter,    kBreak,     kBreak,     kNumber,    kNumber,    kNumber,    kBreak,
        //  A-grave     A-acute     A-hat       A-tilde     A-umlaut    A-ring      AE          C-cedilla
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  E-grave     E-acute     E-hat       E-umlaut    I-grave     I-acute     I-hat       I-umlaut
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  Edh         N-tilde     O-grave     O-acute     O-hat       O-tilde     O-umlaut    times
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kBreak,
        //  O-slash     U-grave     U-acute     U-hat       U-umlaut    Y-acute     Thorn       ess-zed
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  a-grave     a-acute     a-hat       a-tilde     a-umlaut    a-ring      ae          c-cedilla
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  e-grave     e-acute     e-hat       e-umlaut    i-grave     i-acute     i-hat       i-umlaut
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,
        //  edh         n-tilde     o-grave     o-acute     o-hat       o-tilde     o-umlaut    over
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kBreak,
        //  o-slash     u-grave     u-acute     u-hat       u-umlaut    y-acute     thorn       y-umlaut
            kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter,    kLetter
    };


UnicodeClassMapping* TextBoundaryData::kWordMap = 
    new UnicodeClassMapping(kWordRawMapping, kWordRawMapping_length, 
                            kWordExceptionChar, kWordExceptionChar_length,
                            kWordExceptionFlags,
                            kWordAsciiValues );

/**
 * This is the single instance of TextBoundaryData containing word
 * break data.
 */
const TextBoundaryData TextBoundaryData::kWordBreakData(TextBoundaryData::kWordForward,
                                                        TextBoundaryData::kWordBackward,
                                                        TextBoundaryData::kWordMap);

//eof
