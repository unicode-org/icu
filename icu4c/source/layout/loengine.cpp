/*
 * @(#)loengine.cpp	1.0 00/12/07
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "unicode/loengine.h"
#include "layout/LEScripts.h"

//
// This table maps ICU's UScriptCode values
// into the LayoutEngines script codes, as defined
// in LEScripts.h.
//
// NOTE: it's important to keep this list in synch
// both with UScriptCode and LEScripts...
//
// NOTE: REALLY, LEScripts.h should be adjusted to
// match UScriptCode, so this isn't even necessary...
//
int32_t ICULayoutEngine::le_scriptCodes[] = {
      qaaiScriptCode,   // USCRIPT_COMMON (zyyyScriptCode is -1!)
      qaaiScriptCode,   // USCRIPT_INHERITED
      arabScriptCode,   // USCRIPT_ARABIC
      armnScriptCode,   // USCRIPT_ARMENIAN
      bengScriptCode,   // USCRIPT_BENGALI
      bopoScriptCode,   // USCRIPT_BOPOMOFO
      cherScriptCode,   // USCRIPT_CHEROKEE
      qaaiScriptCode,   // USCRIPT_COPTIC (no qaacScriptCode)
      cyrlScriptCode,   // USCRIPT_CYRILLIC
      dsrtScriptCode,   // USCRIPT_DESERET
      devaScriptCode,   // USCRIPT_DEVANAGARI
      ethiScriptCode,   // USCRIPT_ETHIOPIC
      georScriptCode,   // USCRIPT_GEORGIAN
      gothScriptCode,   // USCRIPT_GOTHIC
      grekScriptCode,   // USCRIPT_GREEK
      gujrScriptCode,   // USCRIPT_GUJARATI
      guruScriptCode,   // USCRIPT_GURMUKHI
      haniScriptCode,   // USCRIPT_HAN
      hangScriptCode,   // USCRIPT_HANGUL
      hebrScriptCode,   // USCRIPT_HEBREW
      hiraScriptCode,   // USCRIPT_HIRAGANA
      kndaScriptCode,   // USCRIPT_KANNADA
      kataScriptCode,   // USCRIPT_KATAKANA
      khmrScriptCode,   // USCRIPT_KHMER
      laooScriptCode,   // USCRIPT_LAO
      latnScriptCode,   // USCRIPT_LATIN
      mlymScriptCode,   // USCRIPT_MALAYALAM
      mongScriptCode,   // USCRIPT_MONGOLIAN
      mymrScriptCode,   // USCRIPT_MYANMAR
      ogamScriptCode,   // USCRIPT_OGHAM
      italScriptCode,   // USCRIPT_OLD_ITALIC
      oryaScriptCode,   // USCRIPT_ORIYA
      runrScriptCode,   // USCRIPT_RUNIC
      sinhScriptCode,   // USCRIPT_SINHALA
      syrcScriptCode,   // USCRIPT_SYRIAC
      tamlScriptCode,   // USCRIPT_TAMIL
      teluScriptCode,   // USCRIPT_TELUGU
      thaaScriptCode,   // USCRIPT_THAANA
      thaiScriptCode,   // USCRIPT_THAI
      tibtScriptCode,   // USCRIPT_TIBETAN
      cansScriptCode,   // USCRIPT_UCAS
      yiiiScriptCode    // USCRIPT_YI
};

