/*
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/23/00    aliu        Creation.
**********************************************************************
*/
#ifndef TESTUTIL_H
#define TESTUTIL_H

#include "unicode/utypes.h"
class UnicodeString;

/**
 * Utility methods.  Everything in this class is static -- do not
 * attempt to instantiate.
 */
class TestUtility {

public:
    static int8_t getScript(UChar c);

    static int8_t getScript(int8_t block);

    static int8_t getBlock(UChar c);

    // returns next letter of script, or 0xFFFF if done

    static UChar getNextLetter(UChar c, int8_t script);

    static UnicodeString hex(UChar ch);

    static UnicodeString hex(const UnicodeString& s);

    static UnicodeString hex(const UnicodeString& s, UChar sep);

    enum { // SCRIPT CODE
        COMMON_SCRIPT = 0,
        LATIN_SCRIPT = 1,
        GREEK_SCRIPT = 2,
        CYRILLIC_SCRIPT = 3,
        ARMENIAN_SCRIPT = 4,
        HEBREW_SCRIPT = 5,
        ARABIC_SCRIPT = 6,
        SYRIAC_SCRIPT = 7,
        THAANA_SCRIPT = 8, 
        DEVANAGARI_SCRIPT = 9,
        BENGALI_SCRIPT = 10,
        GURMUKHI_SCRIPT = 11,
        GUJARATI_SCRIPT = 12,
        ORIYA_SCRIPT = 13,
        TAMIL_SCRIPT = 14,
        TELUGU_SCRIPT = 15,
        KANNADA_SCRIPT = 16,
        MALAYALAM_SCRIPT = 17,
        SINHALA_SCRIPT = 18,
        THAI_SCRIPT = 19,
        LAO_SCRIPT = 20,
        TIBETAN_SCRIPT = 21,
        MYANMAR_SCRIPT = 22,
        GEORGIAN_SCRIPT = 23,
        JAMO_SCRIPT = 24,
        HANGUL_SCRIPT = 25,
        ETHIOPIC_SCRIPT = 26,
        CHEROKEE_SCRIPT = 27,
        ABORIGINAL_SCRIPT = 28,
        OGHAM_SCRIPT = 29,
        RUNIC_SCRIPT = 30,
        KHMER_SCRIPT = 31,
        MONGOLIAN_SCRIPT = 32,
        HIRAGANA_SCRIPT = 33,
        KATAKANA_SCRIPT = 34,
        BOPOMOFO_SCRIPT = 35,
        HAN_SCRIPT = 36,
        YI_SCRIPT = 37
    };

    enum { // block code
        RESERVED_BLOCK = 0,
        BASIC_LATIN = 1,
        LATIN_1_SUPPLEMENT = 2,
        LATIN_EXTENDED_A = 3,
        LATIN_EXTENDED_B = 4,
        IPA_EXTENSIONS = 5,
        SPACING_MODIFIER_LETTERS = 6,
        COMBINING_DIACRITICAL_MARKS = 7,
        GREEK = 8,
        CYRILLIC = 9,
        ARMENIAN = 10,
        HEBREW = 11,
        ARABIC = 12,
        SYRIAC = 13,
        THAANA = 14,
        DEVANAGARI = 15,
        BENGALI = 16,
        GURMUKHI = 17,
        GUJARATI = 18,
        ORIYA = 19,
        TAMIL = 20,
        TELUGU = 21,
        KANNADA = 22,
        MALAYALAM = 23,
        SINHALA = 24,
        THAI = 25,
        LAO = 26,
        TIBETAN = 27,
        MYANMAR = 28,
        GEORGIAN = 29,
        HANGUL_JAMO = 30,
        ETHIOPIC = 31,
        CHEROKEE = 32,
        UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS = 33,
        OGHAM = 34,
        RUNIC = 35,
        KHMER = 36,
        MONGOLIAN = 37,
        LATIN_EXTENDED_ADDITIONAL = 38,
        GREEK_EXTENDED = 39,
        GENERAL_PUNCTUATION = 40,
        SUPERSCRIPTS_AND_SUBSCRIPTS = 41,
        CURRENCY_SYMBOLS = 42,
        COMBINING_MARKS_FOR_SYMBOLS = 43,
        LETTERLIKE_SYMBOLS = 44,
        NUMBER_FORMS = 45,
        ARROWS = 46,
        MATHEMATICAL_OPERATORS = 47,
        MISCELLANEOUS_TECHNICAL = 48,
        CONTROL_PICTURES = 49,
        OPTICAL_CHARACTER_RECOGNITION = 50,
        ENCLOSED_ALPHANUMERICS = 51,
        BOX_DRAWING = 52,
        BLOCK_ELEMENTS = 53,
        GEOMETRIC_SHAPES = 54,
        MISCELLANEOUS_SYMBOLS = 55,
        DINGBATS = 56,
        BRAILLE_PATTERNS = 57,
        CJK_RADICALS_SUPPLEMENT = 58,
        KANGXI_RADICALS = 59,
        IDEOGRAPHIC_DESCRIPTION_CHARACTERS = 60,
        CJK_SYMBOLS_AND_PUNCTUATION = 61,
        HIRAGANA = 62,
        KATAKANA = 63,
        BOPOMOFO = 64,
        HANGUL_COMPATIBILITY_JAMO = 65,
        KANBUN = 66,
        BOPOMOFO_EXTENDED = 67,
        ENCLOSED_CJK_LETTERS_AND_MONTHS = 68,
        CJK_COMPATIBILITY = 69,
        CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A = 70,
        CJK_UNIFIED_IDEOGRAPHS = 71,
        YI_SYLLABLES = 72,
        YI_RADICALS = 73,
        HANGUL_SYLLABLES = 74,
        HIGH_SURROGATES = 75,
        HIGH_PRIVATE_USE_SURROGATES = 76,
        LOW_SURROGATES = 77,
        PRIVATE_USE = 78,
        CJK_COMPATIBILITY_IDEOGRAPHS = 79,
        ALPHABETIC_PRESENTATION_FORMS = 80,
        ARABIC_PRESENTATION_FORMS_A = 81,
        COMBINING_HALF_MARKS = 82,
        CJK_COMPATIBILITY_FORMS = 83,
        SMALL_FORM_VARIANTS = 84,
        ARABIC_PRESENTATION_FORMS_B = 85,
        SPECIALS = 86,
        HALFWIDTH_AND_FULLWIDTH_FORMS = 87
    };
        
private:

    static const int8_t blockToScript[];
        
    struct Split {
        UChar ch;
        int8_t i1;
        int8_t i2;
    };
    static const Split split[];
        
    static const int8_t charToBlock[];

    TestUtility() {} // Prevent instantiation
};

#endif
