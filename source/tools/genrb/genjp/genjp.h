/*
*******************************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  genjp.
*   encoding:   US-ASCII
*
* Modification history
* Date        Name      Comments
* 10/13/2001  weiv      created
* 
* The GenJP class is useful for generating various stuff related to Japanese language.
* Right now, it uses ICU to generate rules for JIS X 4061 compliant collation.
* Also, it is useful for getting compatibility versions of the characters.
*/

#ifndef ICU_GENJP
#define ICU_GENJP

#include <stdio.h>

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/unorm.h"
#include "ucmpe32.h"
#include "cmemory.h"

static const uint32_t _bufferSize = 256;
static const UChar _vowels[] = { 0x30A1, 0x30A3, 0x30A5, 0x30A7, 0x30A9, 0 }; // katakana AIUEO
static const UChar _prolongedSoundMark = 0x30FC;
static const UChar _voicedMark = 0x3099;
static const UChar _hiraganaIterationMark = 0x309D;
static const UChar _hiraganaVoicedIterationMark = 0x309E;
static const UChar _katakanaIterationMark = 0x30FD;
static const UChar _katakanaVoicedIterationMark = 0x30FE;

static const UChar _hiraganaStart = 0x3041;
static const UChar _hiraganaEnd = 0x3094;
static const UChar _katakanaStart = 0x30A1;
static const UChar _katakanaEnd = 0x30FA;

static const char *_tertiaryLess = "\"<<<";
static const char *_equal = "  \"=";



class GenJP {
 public:
    GenJP();
    ~GenJP();
    UChar getHalf(UChar u); // Gets the compatibility version of an UChar. 
    // The structure holds halfwidth and fullwidth compatibility characters.
    UBool isSemivoiced(UChar ch, UErrorCode &status); // Is a code point semivoiced
    UBool isVoiced(UChar ch, UErrorCode &status); // Is a code point voiced

    void writeHeader(UErrorCode &status); 
    void processLengthMark(UErrorCode &status); // This will do small vowels and generate rules for the length mark
    void processIterationMark(UErrorCode &status); // This will generate the rules for the iteration mark
    void processCompatibility(UErrorCode &status);  // This will generate the rules for making compatibility chars
    // equal with their normal counter part (only halfwidth and fullwidth).
    void equalKatakanaToHiragana(UErrorCode &status); // This will generate the rules &K=K=hK=H
    void printOutKanji(UErrorCode &status); // Just prints out Kanji ordering...
    void writeFooter(UErrorCode &status);
 
 private:
    const char *getRelation();
    UChar getHiragana(UChar katakana);
    const char *getName(const UChar ch, UErrorCode &status);
    char *printUnicodeStuff(UChar *zTStuff, char *resBuf);
    void processIterationMark(UChar katakana, UErrorCode &status);
    void processVoicedIterationMark(UChar katakana, UErrorCode &status);
    void processVoicedKana(UChar katakana, UErrorCode &status);
    void processSemivoicedKana(UChar katakana, UErrorCode &status);
    CompactEIntArray *kanaToHalf;
    FILE *out;
    char *nameBuff;
    UBool wasReset;
};

inline UChar GenJP::getHiragana(UChar katakana) {
  return katakana - (_katakanaStart - _hiraganaStart);
}

#endif
