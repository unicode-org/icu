/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File CHBKDAT.CPP
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
// This file was generated from the java source file CharacterBreakData.java
// *****************************************************************************

#include "txtbdat.h"
#include "wdbktbl.h"
#include "unicdcm.h"
// *****************************************************************************
// class CharacterBreakData
// The following tables contain the transition state data for character break.
// Take forward data for example, the state machine looks like,
//    Diagram 1 : the forward state machine for accent and base
//
//                          accent
//                           ----
//            accent  +----+/    \
//           -------> |SI+2|      |
//          /         +----+<----/ 
//    +----+            |        base       +-------+
// 0->|stop|            +-----------------> |SI_stop|
//    +----+\-------> +----+--------------> +-------+
//            base    |SI+2|     base       
//                    +----+
//                    ^    \
//                    |     |
//                    \----/
//                    accent
//
// *****************************************************************************
// The forward transition states of character boundary data.
TextBoundaryData::Node TextBoundaryData::kCharacterForwardData[] = {
        // acct         base            cr              lf
        // cho          jung            jong            EOS
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,

        // 1
        kSI_2,          kSI_2,          kSI_3,          kSI_7,
        kSI_4,          kSI_5,          kSI_6,          kSI_Stop,

        // 2
        kSI_2,          kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,

        // 3
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_7,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,

        // 4
        kSI_2,          kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_4,          kSI_5,          kSI_6,          kSI_Stop,

        // 5
        kSI_2,          kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_5,          kSI_6,          kSI_Stop,

        // 6
        kSI_2,          kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_6,          kSI_Stop,

        // 7
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop,
        kSI_Stop,       kSI_Stop,       kSI_Stop,       kSI_Stop
};

const int32_t TextBoundaryData::kCharacterForwardData_length =
    sizeof(TextBoundaryData::kCharacterForwardData) / sizeof(TextBoundaryData::kCharacterForwardData[0]);

WordBreakTable* TextBoundaryData::kCharacterForward = new WordBreakTable(kCharacterCol_count, kCharacterForwardData, kCharacterForwardData_length);


// *****************************************************************************
//
//    Diagram 2 : the backward state machine for accent and base
//
//                          accent
//                           ----
//            accent  +----+/    \
//           -------> |SI+1|      |
//          /         +----+<----/ 
//    +----+            |        base       +-------+
// 0->|stop|            +-----------------> |SI_stop|
//    +----+\-----------------------------> +-------+
//                      base       
//
// *****************************************************************************
// The backward transition states of character boundary data.
TextBoundaryData::Node TextBoundaryData::kCharacterBackwardData[] = {
        // acct         base            cr              lf
        // cho          jung            jong            EOS
        kStop,          kStop,          kStop,          kStop,
        kStop,          kStop,          kStop,          kStop,

        // 1
        kSI_1,          kSI_Stop,       kSI_Stop,       kSI_1,
        kSI_Stop,       kSI_1,          kSI_1,          kSI_Stop
};

const int32_t TextBoundaryData::kCharacterBackwardData_length =
    sizeof(TextBoundaryData::kCharacterBackwardData) / sizeof(TextBoundaryData::kCharacterBackwardData[0]);

WordBreakTable* TextBoundaryData::kCharacterBackward = new WordBreakTable(kCharacterCol_count, kCharacterBackwardData, kCharacterBackwardData_length);

// The character type mapping of the break table.
TextBoundaryData::Type TextBoundaryData::kCharacterRawMapping[] = {
    // Re-coded to match Unicode 2 types [LIU]
    kBaseForm,          // UNASSIGNED               = 0,
    kBaseForm,          // UPPERCASE_LETTER         = 1,
    kBaseForm,          // LOWERCASE_LETTER         = 2,
    kBaseForm,          // TITLECASE_LETTER         = 3,
    kBaseForm,          // MODIFIER_LETTER          = 4,
    kBaseForm,          // OTHER_LETTER             = 5,
    kAccent_diacritic,  // NON_SPACING_MARK         = 6,
    kAccent_diacritic,  // ENCLOSING_MARK           = 7,
    kBaseForm,          // COMBINING_SPACING_MARK   = 8,
    kBaseForm,          // DECIMAL_DIGIT_NUMBER     = 9,
    kBaseForm,          // LETTER_NUMBER            = 10,
    kBaseForm,          // OTHER_NUMBER             = 11,
    kBaseForm,          // SPACE_SEPARATOR          = 12,
    kBaseForm,          // LINE_SEPARATOR           = 13,
    kBaseForm,          // PARAGRAPH_SEPARATOR      = 14,
    kBaseForm,          // CONTROL                  = 15,
    kBaseForm,          // FORMAT                   = 16,
    kBaseForm,          // PRIVATE_USE              = 17,
    kBaseForm,          // SURROGATE                = 18,
    kBaseForm,          // DASH_PUNCTUATION         = 19,
    kBaseForm,          // START_PUNCTUATION        = 20,
    kBaseForm,          // END_PUNCTUATION          = 21,
    kBaseForm,          // CONNECTOR_PUNCTUATION    = 22,
    kBaseForm,          // OTHER_PUNCTUATION        = 23,
    kBaseForm,          // MATH_SYMBOL              = 24,
    kBaseForm,          // CURRENCY_SYMBOL          = 25,
    kBaseForm,          // MODIFIER_SYMBOL          = 26,
    kBaseForm,          // OTHER_SYMBOL             = 27,
    kBaseForm           // UNDEFINED                = 28
};

const int32_t TextBoundaryData::kCharacterRawMapping_length =
    sizeof(TextBoundaryData::kCharacterRawMapping) / sizeof(TextBoundaryData::kCharacterRawMapping[0]);

SpecialMapping TextBoundaryData::kCharacterExceptionChar[] = {
        SpecialMapping(TextBoundaryData::ASCII_LINEFEED, TextBoundaryData::kBaseLF),
        SpecialMapping(TextBoundaryData::ASCII_CARRIAGE_RETURN, TextBoundaryData::kBaseCR),
        SpecialMapping(TextBoundaryData::HANGUL_CHOSEONG_LOW, TextBoundaryData::HANGUL_CHOSEONG_HIGH, TextBoundaryData::kChoseong),
        SpecialMapping(TextBoundaryData::HANGUL_JUNGSEONG_LOW, TextBoundaryData::HANGUL_JUNGSEONG_HIGH, TextBoundaryData::kJungseong),
        SpecialMapping(TextBoundaryData::HANGUL_JONGSEONG_LOW, TextBoundaryData::HANGUL_JONGSEONG_HIGH, TextBoundaryData::kJongseong),
        SpecialMapping(TextBoundaryData::PUNCTUATION_LINE_SEPARATOR, TextBoundaryData::PUNCTUATION_PARAGRAPH_SEPARATOR, TextBoundaryData::kBaseLF),
        SpecialMapping(TextBoundaryData::END_OF_STRING, TextBoundaryData::kEOS)
};

const int32_t TextBoundaryData::kCharacterExceptionChar_length = 
    sizeof(TextBoundaryData::kCharacterExceptionChar) / sizeof(TextBoundaryData::kCharacterExceptionChar[0]);

const bool_t TextBoundaryData::kCharacterExceptionFlags[] = {
        FALSE,          // kNonCharacter            = 0,
        FALSE,          // kUppercaseLetter         = 1,
        FALSE,          // kLowercaseLetter         = 2,
        FALSE,          // kTitlecaseLetter         = 3,
        FALSE,          // kModifierLetter          = 4,
        TRUE,           // kOtherLetter             = 5,
        FALSE,          // kNonSpacingMark          = 6,
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
        FALSE,          // kDashPunctuation         = 19,
        FALSE,          // kOpenPunctuation         = 20,
        FALSE,          // kClosePunctuation        = 21,
        FALSE,          // kConnectorPunctuation    = 22,
        FALSE,          // kOtherPunctuation        = 23,
        FALSE,          // kMathSymbol              = 24,
        FALSE,          // kCurrencySymbol          = 25,
        FALSE,          // kModifierSymbol          = 26,
        FALSE,          // kOtherSymbol             = 27
        FALSE           // UNDEFINED                = 28,
    };

TextBoundaryData::Type TextBoundaryData::kCharacterAsciiValues[] = {
        //  null       soh        stx        etx        eot        enq        ask        bell
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  bs         ht         lf         vt         ff         cr         so         si
            kBaseForm, kBaseForm, kBaseLF,   kBaseForm, kBaseForm, kBaseCR,   kBaseForm, kBaseForm,
        //  dle        dc1        dc2        dc3        dc4        nak        syn        etb
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  can        em         sub        esc        fs         gs         rs         us
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  sp         !          "          #          $          %          &          '
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  (          )          *          +          ,          -          .          /
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  0          1          2          3          4          5          6          7
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  8          9          :          ;          <          =          >          ?
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  @          A          B          C          D          E          F          G
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  H          I          J          K          L          M          N          O
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  P          Q          R          S          T          U          V          W
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  X          Y          Z          [          \          ]          ^          _
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  `          a          b          c          d          e          f          g
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  h          i          j          k          l          m          n          o
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  p          q          r          s          t          u          v          w
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  x          y          z          {          |          }          ~          del
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  ctrl       ctrl       ctrl       ctrl       ctrl       ctrl       ctrl       ctrl
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  ctrl       ctrl       ctrl       ctrl       ctrl       ctrl       ctrl       ctrl
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  ctrl       ctrl       ctrl       ctrl       ctrl       ctrl       ctrl       ctrl
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  ctrl       ctrl       ctrl       ctrl       ctrl       ctrl       ctrl       ctrl
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  nbsp       ¡          ¢          £          ¤          ¥          ¦
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  ¨          ©          ª          «          ¬          ­          ®          ¯
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  °          ±          ²          ³          ´          µ          ¶          ·
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  ¸          ¹          º          »          ¼          ½          ¾          ¿
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  À          Á          Â          Ã          Ä          Å          Æ          Ç
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  È          É          Ê          Ë          Ì          Í          Î          Ï
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  Ð          Ñ          Ò          Ó          Ô          Õ          Ö          ×
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  Ø          Ù          Ú          Û          Ü          Ý          Þ          ß
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  à          á          â          ã          ä          å          æ          ç
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  è          é          ê          ë          ì          í          î          ï
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  ð          ñ          ò          ó          ô          õ          ö          ÷
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm,
        //  ø          ù          ú          û          ü          ý          þ          ÿ
            kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm, kBaseForm
};



UnicodeClassMapping* TextBoundaryData::kCharacterMap = 
    new UnicodeClassMapping(kCharacterRawMapping, kCharacterRawMapping_length, 
                            kCharacterExceptionChar, kCharacterExceptionChar_length,
                            kCharacterExceptionFlags,
                            kCharacterAsciiValues );

/**
 * This is the single instance of TextBoundaryData containing character
 * break data.
 */
const TextBoundaryData TextBoundaryData::kCharacterBreakData(TextBoundaryData::kCharacterForward,
                                                             TextBoundaryData::kCharacterBackward,
                                                             TextBoundaryData::kCharacterMap);

//eof
