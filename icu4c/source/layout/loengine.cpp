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
        zyyyScriptCode,    // (no EUnicodeScript w/ vaule 0)
        latnScriptCode,    // kBasicLatin
        latnScriptCode,    // kLatin1Supplement
        latnScriptCode,    // kLatinExtendedA
        latnScriptCode,    // kLatinExtendedB
        latnScriptCode,    // kIPAExtension
        qaaiScriptCode,    // kSpacingModifier
        qaaiScriptCode,    // kCombiningDiacritical
        grekScriptCode,    // kGreek
        cyrlScriptCode,    // kCyrillic
        armnScriptCode,    // kArmenian
        hebrScriptCode,    // kHebrew
        arabScriptCode,    // kArabic
        syrcScriptCode,    // kSyriac
        thaaScriptCode,    // kThaana
        devaScriptCode,    // kDevanagari
        bengScriptCode,    // kBengali
        guruScriptCode,    // kGurmukhi
        gujrScriptCode,    // kGujarati
        oryaScriptCode,    // kOriya
        tamlScriptCode,    // kTamil
        teluScriptCode,    // kTelugu
        kndaScriptCode,    // kKannada
        mlymScriptCode,    // kMalayalam
        sinhScriptCode,    // kSinhala
        thaiScriptCode,    // kThai
        laooScriptCode,    // kLao
        tibtScriptCode,    // kTibetan
        mymrScriptCode,    // kMyanmar
        georScriptCode,    // kGeorgian
        hangScriptCode,    // kHangulJamo
        ethiScriptCode,    // kEthiopic
        cherScriptCode,    // kCherokee
        cansScriptCode,    // kUnifiedCanadianAboriginalSyllabics
        ogamScriptCode,    // kogham
        runrScriptCode,    // kRunic
        khmrScriptCode,    // kKhmer
        mongScriptCode,    // kMongolian
        latnScriptCode,    // kLatinExtendedAdditional
        grekScriptCode,    // kGreekExtended
        qaaiScriptCode,    // kGeneralPunctuation
        qaaiScriptCode,    // kSuperSubScript
        qaaiScriptCode,    // kCurrencySymbolScript
        qaaiScriptCode,    // kSymbolCombiningMark
        qaaiScriptCode,    // kLetterlikeSymbol
        qaaiScriptCode,    // kNumberForm
        qaaiScriptCode,    // kArrow
        qaaiScriptCode,    // kMathOperator
        qaaiScriptCode,    // kMiscTechnical
        qaaiScriptCode,    // kControlPicture
        qaaiScriptCode,    // kOpticalCharacter
        qaaiScriptCode,    // kEnclosedAlphanumeric
        qaaiScriptCode,    // kBoxDrawing
        qaaiScriptCode,    // kBlockElement
        qaaiScriptCode,    // kGeometricShape
        qaaiScriptCode,    // kMiscSymbol
        qaaiScriptCode,    // kDingbat
        zyyyScriptCode,    // kBraillePatterns
        haniScriptCode,    // kCJKRadicalsSupplement
        haniScriptCode,    // kKangxiRadicals
        zyyyScriptCode,    // kIdeographicDescriptionCharacters
        haniScriptCode,    // kCJKSymbolPunctuation
        hiraScriptCode,    // kHiragana
        kataScriptCode,    // kKatakana
        bopoScriptCode,    // kBopomofo
        hangScriptCode,    // kHangulCompatibilityJamo
        zyyyScriptCode,    // kKanbun
        bopoScriptCode,    // kBopomofoExtended
        haniScriptCode,    // kEnclosedCJKLetterMonth
        haniScriptCode,    // kCJKCompatibility
        haniScriptCode,    // kCJKUnifiedIdeographExtensionA
        haniScriptCode,    // kCJKUnifiedIdeograph
        yiiiScriptCode,    // kYiSyllables
        yiiiScriptCode,    // kYiRadicals
        hangScriptCode,    // kHangulSyllable
        zyyyScriptCode,    // kHighSurrogate
        zyyyScriptCode,    // kHighPrivateUseSurrogate
        zyyyScriptCode,    // kLowSurrogate
        zyyyScriptCode,    // kPrivateUse
        haniScriptCode,    // kCJKCompatibilityIdeograph
        qaaiScriptCode,    // kAlphabeticPresentation
        arabScriptCode,    // kArabicPresentationA
        qaaiScriptCode,    // kCombiningHalfMark
        haniScriptCode,    // kCJKCompatibilityForm
        qaaiScriptCode,    // kSmallFormVariant
        arabScriptCode,    // kArabicPresentationB
        qaaiScriptCode,    // kNoScript
        qaaiScriptCode     // kHalfwidthFullwidthForm **** FIXME: should be ASCII, CJK, KANA, HANGUL ****
};

