/*
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/23/00    aliu        Creation.
**********************************************************************
*/
#include "testutil.h"
#include "unicode/unicode.h"
#include "unicode/unistr.h"

int8_t TestUtility::getScript(UChar c) {
  return getScript(getBlock(c));
}

int8_t TestUtility::getScript(int8_t block) {
  return blockToScript[block];
}

int8_t TestUtility::getBlock(UChar c) {
  int32_t index = c >> 7;
  int8_t block = charToBlock[index];
  while (block < 0) { // take care of exceptions, blocks split across 128 boundaries
      const Split tuple = split[-block-1];
      if (c < tuple.ch) block = tuple.i1;
      else block = tuple.i2;
  }
  return block;
}

// returns next letter of script, or 0xFFFF if done

UChar TestUtility::getNextLetter(UChar c, int8_t script) {
    while (c < 0xFFFF) {
        ++c;
        if (getScript(c) == script && Unicode::isLetter(c)) {
            return c;
        }
    }
    return c;
}

static const UChar HEX[16]={48,49,50,51,52,53,54,55,56,57,65,66,67,68,69,70};

UnicodeString TestUtility::hex(UChar ch) {
    UnicodeString buf;
    buf.append(HEX[0xF&(ch>>12)]);
    buf.append(HEX[0xF&(ch>>8)]);
    buf.append(HEX[0xF&(ch>>4)]);
    buf.append(HEX[0xF&ch]);
    return buf;
}

UnicodeString TestUtility::hex(const UnicodeString& s) {
    return hex(s, 44 /*,*/);
}

UnicodeString TestUtility::hex(const UnicodeString& s, UChar sep) {
    if (s.length() == 0) return "";
    UnicodeString result = hex(s.charAt(0));
    for (int32_t i = 1; i < s.length(); ++i) {
        result.append(sep);
        result.append(hex(s.charAt(i)));
    }
    return result;
}

const int8_t TestUtility::blockToScript[] = {
    COMMON_SCRIPT, // 0, <RESERVED_BLOCK>
    LATIN_SCRIPT, // 1, BASIC_LATIN
    LATIN_SCRIPT, // 2, LATIN_1_SUPPLEMENT
    LATIN_SCRIPT, // 3, LATIN_EXTENDED_A
    LATIN_SCRIPT, // 4, LATIN_EXTENDED_B
    LATIN_SCRIPT, // 5, IPA_EXTENSIONS
    COMMON_SCRIPT, // 6, SPACING_MODIFIER_LETTERS
    COMMON_SCRIPT, // 7, COMBINING_DIACRITICAL_MARKS
    GREEK_SCRIPT, // 8, GREEK
    CYRILLIC_SCRIPT, // 9, CYRILLIC
    ARMENIAN_SCRIPT, // 10, ARMENIAN
    HEBREW_SCRIPT, // 11, HEBREW
    ARABIC_SCRIPT, // 12, ARABIC
    SYRIAC_SCRIPT, // 13, SYRIAC
    THAANA_SCRIPT, // 14, THAANA
    DEVANAGARI_SCRIPT, // 15, DEVANAGARI
    BENGALI_SCRIPT, // 16, BENGALI
    GURMUKHI_SCRIPT, // 17, GURMUKHI
    GUJARATI_SCRIPT, // 18, GUJARATI
    ORIYA_SCRIPT, // 19, ORIYA
    TAMIL_SCRIPT, // 20, TAMIL
    TELUGU_SCRIPT, // 21, TELUGU
    KANNADA_SCRIPT, // 22, KANNADA
    MALAYALAM_SCRIPT, // 23, MALAYALAM
    SINHALA_SCRIPT, // 24, SINHALA
    THAI_SCRIPT, // 25, THAI
    LAO_SCRIPT, // 26, LAO
    TIBETAN_SCRIPT, // 27, TIBETAN
    MYANMAR_SCRIPT, // 28, MYANMAR
    GEORGIAN_SCRIPT, // 29, GEORGIAN
    JAMO_SCRIPT, // 30, HANGUL_JAMO
    ETHIOPIC_SCRIPT, // 31, ETHIOPIC
    CHEROKEE_SCRIPT, // 32, CHEROKEE
    ABORIGINAL_SCRIPT, // 33, UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS
    OGHAM_SCRIPT, // 34, OGHAM
    RUNIC_SCRIPT, // 35, RUNIC
    KHMER_SCRIPT, // 36, KHMER
    MONGOLIAN_SCRIPT, // 37, MONGOLIAN
    LATIN_SCRIPT, // 38, LATIN_EXTENDED_ADDITIONAL
    GREEK_SCRIPT, // 39, GREEK_EXTENDED
    COMMON_SCRIPT, // 40, GENERAL_PUNCTUATION
    COMMON_SCRIPT, // 41, SUPERSCRIPTS_AND_SUBSCRIPTS
    COMMON_SCRIPT, // 42, CURRENCY_SYMBOLS
    COMMON_SCRIPT, // 43, COMBINING_MARKS_FOR_SYMBOLS
    COMMON_SCRIPT, // 44, LETTERLIKE_SYMBOLS
    COMMON_SCRIPT, // 45, NUMBER_FORMS
    COMMON_SCRIPT, // 46, ARROWS
    COMMON_SCRIPT, // 47, MATHEMATICAL_OPERATORS
    COMMON_SCRIPT, // 48, MISCELLANEOUS_TECHNICAL
    COMMON_SCRIPT, // 49, CONTROL_PICTURES
    COMMON_SCRIPT, // 50, OPTICAL_CHARACTER_RECOGNITION
    COMMON_SCRIPT, // 51, ENCLOSED_ALPHANUMERICS
    COMMON_SCRIPT, // 52, BOX_DRAWING
    COMMON_SCRIPT, // 53, BLOCK_ELEMENTS
    COMMON_SCRIPT, // 54, GEOMETRIC_SHAPES
    COMMON_SCRIPT, // 55, MISCELLANEOUS_SYMBOLS
    COMMON_SCRIPT, // 56, DINGBATS
    COMMON_SCRIPT, // 57, BRAILLE_PATTERNS
    HAN_SCRIPT, // 58, CJK_RADICALS_SUPPLEMENT
    HAN_SCRIPT, // 59, KANGXI_RADICALS
    HAN_SCRIPT, // 60, IDEOGRAPHIC_DESCRIPTION_CHARACTERS
    COMMON_SCRIPT, // 61, CJK_SYMBOLS_AND_PUNCTUATION
    HIRAGANA_SCRIPT, // 62, HIRAGANA
    KATAKANA_SCRIPT, // 63, KATAKANA
    BOPOMOFO_SCRIPT, // 64, BOPOMOFO
    JAMO_SCRIPT, // 65, HANGUL_COMPATIBILITY_JAMO
    HAN_SCRIPT, // 66, KANBUN
    BOPOMOFO_SCRIPT, // 67, BOPOMOFO_EXTENDED
    COMMON_SCRIPT, // 68, ENCLOSED_CJK_LETTERS_AND_MONTHS
    COMMON_SCRIPT, // 69, CJK_COMPATIBILITY
    HAN_SCRIPT, // 70, CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
    HAN_SCRIPT, // 71, CJK_UNIFIED_IDEOGRAPHS
    YI_SCRIPT, // 72, YI_SYLLABLES
    YI_SCRIPT, // 73, YI_RADICALS
    HANGUL_SCRIPT, // 74, HANGUL_SYLLABLES
    COMMON_SCRIPT, // 75, HIGH_SURROGATES
    COMMON_SCRIPT, // 76, HIGH_PRIVATE_USE_SURROGATES
    COMMON_SCRIPT, // 77, LOW_SURROGATES
    COMMON_SCRIPT, // 78, PRIVATE_USE
    HAN_SCRIPT, // 79, CJK_COMPATIBILITY_IDEOGRAPHS
    COMMON_SCRIPT, // 80, ALPHABETIC_PRESENTATION_FORMS
    ARABIC_SCRIPT, // 81, ARABIC_PRESENTATION_FORMS_A
    COMMON_SCRIPT, // 82, COMBINING_HALF_MARKS
    COMMON_SCRIPT, // 83, CJK_COMPATIBILITY_FORMS
    COMMON_SCRIPT, // 84, SMALL_FORM_VARIANTS
    ARABIC_SCRIPT, // 85, ARABIC_PRESENTATION_FORMS_B
    COMMON_SCRIPT, // 86, SPECIALS
    COMMON_SCRIPT, // 87, HALFWIDTH_AND_FULLWIDTH_FORMS
    COMMON_SCRIPT, // 88, SPECIALS
};

const TestUtility::Split TestUtility::split[] = {
    {0x0250, 4, 5}, // -1
    {0x02B0, 5, 6}, // -2
    {0x0370, 7, 8}, // -3
    {0x0530, 0, 10}, // -4
    {0x0590, 10, 11}, // -5
    {0x0750, 13, 0}, // -6
    {0x07C0, 14, 0}, // -7
    {0x10A0, 28, 29}, // -8
    {0x13A0, 0, 32}, // -9
    {0x16A0, 34, 35}, // -10
    {0x18B0, 37, 0}, // -11
    {0x2070, 40, 41}, // -12
    {0x20A0, 41, -31}, // -13
    {0x2150, 44, 45}, // -14
    {0x2190, 45, 46}, // -15
    {0x2440, 49, -32}, // -16
    {0x25A0, 53, 54}, // -17
    {0x27C0, 56, 0}, // -18
    {0x2FE0, 59, -33}, // -19
    {0x3040, 61, 62}, // -20
    {0x30A0, 62, 63}, // -21
    {0x3130, 64, 65}, // -22
    {0x3190, 65, -34}, // -23
    {0x4DB6, 70, 0}, // -24
    {0xA490, 72, -35}, // -25
    {0xD7A4, 74, 0}, // -26
    {0xFB50, 80, 81}, // -27
    {0xFE20, 0, -36}, // -28
    {0xFEFF, 85, 86}, // -29
    {0xFFF0, 87, -37}, // -30
    {0x20D0, 42, 43}, // -31
    {0x2460, 50, 51}, // -32
    {0x2FF0, 0, 60}, // -33
    {0x31A0, 66, -38}, // -34
    {0xA4D0, 73, 0}, //-35
    {0xFE30, 82, -39}, //-36
    {0xFFFE, 88, 0}, //-37
    {0x31C0, 67, 0}, // -38
    {0xFE50, 83, -40}, //-39
    {0xFE70, 84, 85} // -40
};

const int8_t TestUtility::charToBlock[] = {
  1, 2, 3, 4, -1, -2, -3, 8, 9, 9, -4, -5, 12, 12, -6, -7,
  0, 0, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 27,
  28, -8, 30, 30, 31, 31, 31, -9, 33, 33, 33, 33, 33, -10, 0, 36,
  37, -11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 38, 38, 39, 39,
  -12, -13, -14, -15, 47, 47, 48, 48, -16, 51, 52, -17, 55, 55, 56, -18,
  57, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 58, 59, -19,
  -20, -21, -22, -23, 68, 68, 69, 69, 70, 70, 70, 70, 70, 70, 70, 70,
  70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70,
  70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70,
  70, 70, 70, 70, 70, 70, 70, 70, 70, 70, 70, -24, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71,
  72, 72, 72, 72, 72, 72, 72, 72, 72, -25, 0, 0, 0, 0, 0, 0,
  0, 0, 0, 0, 0, 0, 0, 0, 74, 74, 74, 74, 74, 74, 74, 74,
  74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74,
  74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74,
  74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74,
  74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74,
  74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, -26,
  75, 75, 75, 75, 75, 75, 75, 76, 77, 77, 77, 77, 77, 77, 77, 77,
  78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78,
  78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78,
  78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78, 78,
  78, 78, 79, 79, 79, 79, -27, 81, 81, 81, 81, 81, -28, -29, 87, -30
};
