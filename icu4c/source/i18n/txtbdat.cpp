/*
*****************************************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*
* File TXTBDAT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.
*                           Made static data members const where appropriate.
*   05/06/97    aliu        Made kSI, kStop, and kSI_Stop into #defines to help out
*                           non-compliant compilers.
*****************************************************************************************
*/

#include "txtbdat.h"

// *****************************************************************************
// class TextBoundaryData
// *****************************************************************************

// The following is removed and became #define(s) because of compiler problems.
//const TextBoundaryData::Node TextBoundaryData::kSI        = 0x80;
//const TextBoundaryData::Node TextBoundaryData::kStop      = 0;
//const TextBoundaryData::Node TextBoundaryData::kSI_Stop   = kSI + kStop;

// The following Unicode character may need special mappings in a particular
// text boundary.
const UChar TextBoundaryData::ASCII_END_OF_TEXT                   = (UChar)0x0003;
const UChar TextBoundaryData::ASCII_HORIZONTAL_TABULATION         = (UChar)0x0009;
const UChar TextBoundaryData::ASCII_LINEFEED                      = (UChar)0x000A;
const UChar TextBoundaryData::ASCII_VERTICAL_TABULATION           = (UChar)0x000B;
const UChar TextBoundaryData::ASCII_FORM_FEED                     = (UChar)0x000C;
const UChar TextBoundaryData::ASCII_CARRIAGE_RETURN               = (UChar)0x000D;
const UChar TextBoundaryData::ASCII_SPACE                         = (UChar)0x0020;
const UChar TextBoundaryData::ASCII_EXCLAMATION_MARK              = (UChar)0x0021;
const UChar TextBoundaryData::ASCII_QUOTATION_MARK                = (UChar)0x0022;
const UChar TextBoundaryData::ASCII_NUMBER_SIGN                   = (UChar)0x0023;
const UChar TextBoundaryData::ASCII_DOLLAR_SIGN                   = (UChar)0x0024;
const UChar TextBoundaryData::ASCII_PERCENT                       = (UChar)0x0025;
const UChar TextBoundaryData::ASCII_AMPERSAND                     = (UChar)0x0026;
const UChar TextBoundaryData::ASCII_APOSTROPHE                    = (UChar)0x0027;
const UChar TextBoundaryData::ASCII_COMMA                         = (UChar)0x002C;
const UChar TextBoundaryData::ASCII_FULL_STOP                     = (UChar)0x002E;
const UChar TextBoundaryData::ASCII_COLON                         = (UChar)0x003A;
const UChar TextBoundaryData::ASCII_SEMICOLON                     = (UChar)0x003B;
const UChar TextBoundaryData::ASCII_QUESTION_MARK                 = (UChar)0x003F;
const UChar TextBoundaryData::ASCII_NONBREAKING_SPACE             = (UChar)0x00A0;
const UChar TextBoundaryData::ASCII_CENT_SIGN                     = (UChar)0x00A2;
const UChar TextBoundaryData::ASCII_POUND_SIGN                    = (UChar)0x00A3;
const UChar TextBoundaryData::ASCII_YEN_SIGN                      = (UChar)0x00A5;
const UChar TextBoundaryData::LATIN1_SOFTHYPHEN                   = (UChar)0x00AD;
const UChar TextBoundaryData::LATIN1_DEGREE_SIGN                  = (UChar)0x00B0;
const UChar TextBoundaryData::ARABIC_PERCENT_SIGN                 = (UChar)0x066A;
const UChar TextBoundaryData::ARABIC_DECIMAL_SEPARATOR            = (UChar)0x066B;
const UChar TextBoundaryData::HANGUL_CHOSEONG_LOW                 = (UChar)0x1100;
const UChar TextBoundaryData::HANGUL_CHOSEONG_HIGH                = (UChar)0x115F;
const UChar TextBoundaryData::HANGUL_JUNGSEONG_LOW                = (UChar)0x1160;
const UChar TextBoundaryData::HANGUL_JUNGSEONG_HIGH               = (UChar)0x11A7;
const UChar TextBoundaryData::HANGUL_JONGSEONG_LOW                = (UChar)0x11A8;
const UChar TextBoundaryData::HANGUL_JONGSEONG_HIGH               = (UChar)0x11FF;
const UChar TextBoundaryData::FIGURE_SPACE                        = (UChar)0x2007;
const UChar TextBoundaryData::NONBREAKING_HYPHEN                  = (UChar)0x2011;
const UChar TextBoundaryData::PUNCTUATION_HYPHENATION_POINT       = (UChar)0x2027;
const UChar TextBoundaryData::PUNCTUATION_LINE_SEPARATOR          = (UChar)0x2028;
const UChar TextBoundaryData::PUNCTUATION_PARAGRAPH_SEPARATOR     = (UChar)0x2029;
const UChar TextBoundaryData::PER_MILLE_SIGN                      = (UChar)0x2030;
const UChar TextBoundaryData::PER_TEN_THOUSAND_SIGN               = (UChar)0x2031;
const UChar TextBoundaryData::PRIME                               = (UChar)0x2032;
const UChar TextBoundaryData::DOUBLE_PRIME                        = (UChar)0x2033;
const UChar TextBoundaryData::TRIPLE_PRIME                        = (UChar)0x2034;
const UChar TextBoundaryData::DEGREE_CELSIUS                      = (UChar)0x2103;
const UChar TextBoundaryData::DEGREE_FAHRENHEIT                   = (UChar)0x2109;
const UChar TextBoundaryData::PUNCTUATION_IDEOGRAPHIC_COMMA       = (UChar)0x3001;
const UChar TextBoundaryData::PUNCTUATION_IDEOGRAPHIC_FULL_STOP   = (UChar)0x3002;
const UChar TextBoundaryData::IDEOGRAPHIC_ITERATION_MARK          = (UChar)0x3005;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_A             = (UChar)0x3041;
const UChar TextBoundaryData::HIRAGANA_LETTER_A                   = (UChar)0x3042;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_I             = (UChar)0x3043;
const UChar TextBoundaryData::HIRAGANA_LETTER_I                   = (UChar)0x3044;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_U             = (UChar)0x3045;
const UChar TextBoundaryData::HIRAGANA_LETTER_U                   = (UChar)0x3046;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_E             = (UChar)0x3047;
const UChar TextBoundaryData::HIRAGANA_LETTER_E                   = (UChar)0x3048;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_O             = (UChar)0x3049;
const UChar TextBoundaryData::HIRAGANA_LETTER_O                   = (UChar)0x304A;
const UChar TextBoundaryData::HIRAGANA_LETTER_DI                  = (UChar)0x3062;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_TU            = (UChar)0x3063;
const UChar TextBoundaryData::HIRAGANA_LETTER_TU                  = (UChar)0x3064;
const UChar TextBoundaryData::HIRAGANA_LETTER_MO                  = (UChar)0x3082;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_YA            = (UChar)0x3083;
const UChar TextBoundaryData::HIRAGANA_LETTER_YA                  = (UChar)0x3084;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_YU            = (UChar)0x3085;
const UChar TextBoundaryData::HIRAGANA_LETTER_YU                  = (UChar)0x3086;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_YO            = (UChar)0x3087;
const UChar TextBoundaryData::HIRAGANA_LETTER_YO                  = (UChar)0x3088;
const UChar TextBoundaryData::HIRAGANA_LETTER_RO                  = (UChar)0x308D;
const UChar TextBoundaryData::HIRAGANA_LETTER_SMALL_WA            = (UChar)0x308E;
const UChar TextBoundaryData::HIRAGANA_LETTER_WA                  = (UChar)0x308F;
const UChar TextBoundaryData::HIRAGANA_LETTER_VU                  = (UChar)0x3094;
const UChar TextBoundaryData::COMBINING_KATAKANA_HIRAGANA_VOICED_SOUND_MARK = (UChar)0x3099;
const UChar TextBoundaryData::HIRAGANA_SEMIVOICED_SOUND_MARK      = (UChar)0x309C;
const UChar TextBoundaryData::HIRAGANA_ITERATION_MARK             = (UChar)0x309D;
const UChar TextBoundaryData::HIRAGANA_VOICED_ITERATION_MARK      = (UChar)0x309E;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_A             = (UChar)0x30A1;
const UChar TextBoundaryData::KATAKANA_LETTER_A                   = (UChar)0x30A2;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_I             = (UChar)0x30A3;
const UChar TextBoundaryData::KATAKANA_LETTER_I                   = (UChar)0x30A4;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_U             = (UChar)0x30A5;
const UChar TextBoundaryData::KATAKANA_LETTER_U                   = (UChar)0x30A6;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_E             = (UChar)0x30A7;
const UChar TextBoundaryData::KATAKANA_LETTER_E                   = (UChar)0x30A8;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_O             = (UChar)0x30A9;
const UChar TextBoundaryData::KATAKANA_LETTER_O                   = (UChar)0x30AA;
const UChar TextBoundaryData::KATAKANA_LETTER_DI                  = (UChar)0x30C2;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_TU            = (UChar)0x30C3;
const UChar TextBoundaryData::KATAKANA_LETTER_TU                  = (UChar)0x30C4;
const UChar TextBoundaryData::KATAKANA_LETTER_MO                  = (UChar)0x30E2;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_YA            = (UChar)0x30E3;
const UChar TextBoundaryData::KATAKANA_LETTER_YA                  = (UChar)0x30E4;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_YU            = (UChar)0x30E5;
const UChar TextBoundaryData::KATAKANA_LETTER_YU                  = (UChar)0x30E6;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_YO            = (UChar)0x30E7;
const UChar TextBoundaryData::KATAKANA_LETTER_YO                  = (UChar)0x30E8;
const UChar TextBoundaryData::KATAKANA_LETTER_RO                  = (UChar)0x30ED;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_WA            = (UChar)0x30EE;
const UChar TextBoundaryData::KATAKANA_LETTER_WA                  = (UChar)0x30EF;
const UChar TextBoundaryData::KATAKANA_LETTER_VU                  = (UChar)0x30F4;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_KA            = (UChar)0x30F5;
const UChar TextBoundaryData::KATAKANA_LETTER_SMALL_KE            = (UChar)0x30F6;
const UChar TextBoundaryData::KATAKANA_LETTER_VA                  = (UChar)0x30F7;
const UChar TextBoundaryData::KATAKANA_LETTER_VO                  = (UChar)0x30FA;
const UChar TextBoundaryData::KATAKANA_HIRAGANA_PROLONGED_SOUND_MARK = (UChar)0x30FC;
const UChar TextBoundaryData::KATAKANA_ITERATION_MARK             = (UChar)0x30FD;
const UChar TextBoundaryData::KATAKANA_VOICED_ITERATION_MARK      = (UChar)0x30FE;
const UChar TextBoundaryData::UNICODE_LOW_BOUND_HAN               = (UChar)0x4E00;
const UChar TextBoundaryData::UNICODE_HIGH_BOUND_HAN              = (UChar)0x9FA5;
const UChar TextBoundaryData::HANGUL_SYL_LOW                      = (UChar)0xAC00;
const UChar TextBoundaryData::HANGUL_SYL_HIGH                     = (UChar)0xD7A3;
const UChar TextBoundaryData::CJK_COMPATIBILITY_F900              = (UChar)0xF900;
const UChar TextBoundaryData::CJK_COMPATIBILITY_FA2D              = (UChar)0xFA2D;
const UChar TextBoundaryData::UNICODE_ZERO_WIDTH_NON_BREAKING_SPACE = (UChar)0xFEFF;
const UChar TextBoundaryData::FULLWIDTH_EXCLAMATION_MARK          = (UChar)0xFF01;
const UChar TextBoundaryData::FULLWIDTH_FULL_STOP                 = (UChar)0xFF0E;
const UChar TextBoundaryData::FULLWIDTH_QUESTION_MARK             = (UChar)0xFF1F;

    // SimpleTextBoundary has an internal convention that the not-a-Unicode value
    // $FFFF is used to signify the end of the string when looking a proper state
    // transition for the end of the string
const UChar TextBoundaryData::END_OF_STRING                       = (UChar)0xFFFF;

//eof
