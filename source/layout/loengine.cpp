/*
 * @(#)loengine.cpp	1.0 00/12/07
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "unicode/loengine.h"
#include "layout/LEScripts.h"

//
// This table maps ICU's EUnicodeScript values,
// which are really Unicode blocks and not scripts,
// into the LayoutEngines script codes, as defined
// in LEScripts.h.
//
// NOTE: it's important to keep this list in synch
// both with EUnicodeScripts and LEScripts...
//
int32_t ICULayoutEngine::le_scriptCodes[] = {
        latnScriptCode,    // kBasicLatin
        latnScriptCode,    // kLatin1Supplement
        latnScriptCode,    // kLatinExtendedA
        latnScriptCode,    // kLatinExtendedB
        latnScriptCode,    // kIPAExtension
        neutScriptCode,    // kSpacingModifier
        neutScriptCode,    // kCombiningDiacritical
        grekScriptCode,    // kGreek
        cyrlScriptCode,    // kCyrillic
        armnScriptCode,    // kArmenian
        hebrScriptCode,    // kHebrew
        arabScriptCode,    // kArabic
        neutScriptCode,    // kSyriac **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kThaana **** FIXME: need LEScriptCode ****
        devaScriptCode,    // kDevanagari
        bengScriptCode,    // kBengali
        punjScriptCode,    // kGurmukhi
        gujrScriptCode,    // kGujarati
        oryaScriptCode,    // kOriya
        tamlScriptCode,    // kTamil
        teluScriptCode,    // kTelugu
        kndaScriptCode,    // kKannada
        mlymScriptCode,    // kMalayalam
        neutScriptCode,    // kSinhala **** FIXME: need LEScriptCode ****
        thaiScriptCode,    // kThai
        laoScriptCode,     // kLao
        tibtScriptCode,    // kTibetan
        neutScriptCode,    // kMyanmar **** FIXME: need LEScriptCode ****
        grgnScriptCode,    // kGeorgian
        hangScriptCode,    // kHangulJamo
        neutScriptCode,    // kEthiopic **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kCherokee **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kUnifiedCanadianAboriginalSyllabics **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kogham **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kRunic **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kKhmer **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kMongolian **** FIXME: need LEScriptCode ****
        latnScriptCode,    // kLatinExtendedAdditional
        grekScriptCode,    // kGreekExtended
        neutScriptCode,    // kGeneralPunctuation
        neutScriptCode,    // kSuperSubScript
        neutScriptCode,    // kCurrencySymbolScript
        neutScriptCode,    // kSymbolCombiningMark
        neutScriptCode,    // kLetterlikeSymbol
        neutScriptCode,    // kNumberForm
        neutScriptCode,    // kArrow
        neutScriptCode,    // kMathOperator
        neutScriptCode,    // kMiscTechnical
        neutScriptCode,    // kControlPicture
        neutScriptCode,    // kOpticalCharacter
        neutScriptCode,    // kEnclosedAlphanumeric
        neutScriptCode,    // kBoxDrawing
        neutScriptCode,    // kBlockElement
        neutScriptCode,    // kGeometricShape
        neutScriptCode,    // kMiscSymbol
        neutScriptCode,    // kDingbat
        neutScriptCode,    // kBraillePatterns **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kCJKRadicalsSupplement **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kKangxiRadicals **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kIdeographicDescriptionCharacters **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kCJKSymbolPunctuation
        kanaScriptCode,    // kHiragana
        kanaScriptCode,    // kKatakana
        bpmfScriptCode,    // kBopomofo
        hangScriptCode,    // kHangulCompatibilityJamo
        knbnScriptCode,    // kKanbun
        bpmfScriptCode,    // kBopomofoExtended
        haniScriptCode,    // kEnclosedCJKLetterMonth
        haniScriptCode,    // kCJKCompatibility
        haniScriptCode,    // kCJKUnifiedIdeographExtensionA
        haniScriptCode,    // kCJKUnifiedIdeograph
        neutScriptCode,    // kYiSyllables **** FIXME: need LEScriptCode ****
        neutScriptCode,    // kYiRadicals **** FIXME: need LEScriptCode ****
        hangScriptCode,    // kHangulSyllable
        surrScriptCode,    // kHighSurrogate
        surrScriptCode,    // kHighPrivateUseSurrogate
        surrScriptCode,    // kLowSurrogate
        puseScriptCode,    // kPrivateUse
        haniScriptCode,    // kCJKCompatibilityIdeograph
        neutScriptCode,    // kAlphabeticPresentation
        arabScriptCode,    // kArabicPresentationA
        neutScriptCode,    // kCombiningHalfMark
        haniScriptCode,    // kCJKCompatibilityForm
        neutScriptCode,    // kSmallFormVariant
        arabScriptCode,    // kArabicPresentationB
        neutScriptCode,    // kNoScript
        neutScriptCode     // kHalfwidthFullwidthForm **** FIXME: should be ASCII, CJK, KANA, HANGUL ****
};

