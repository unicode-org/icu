/*
**********************************************************************
*   Copyright (c) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   01/17/2000  aliu        Ported from Java.
**********************************************************************
*/
#include "unicode/jamohang.h"
#include "unicode/rep.h"
#include "unicode/unifilt.h"
#include "unicode/unicode.h"

/**
 * ID for this transliterator.
 */
const char* JamoHangulTransliterator::_ID = "Jamo-Hangul";

/**
 * Constructs a transliterator.
 */
JamoHangulTransliterator::JamoHangulTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter) {
    setMaximumContextLength(3);
}

/**
 * Copy constructor.
 */
JamoHangulTransliterator::JamoHangulTransliterator(const JamoHangulTransliterator& o) :
    Transliterator(o) {
}

/**
 * Assignment operator.
 */
JamoHangulTransliterator& JamoHangulTransliterator::operator=(
                                             const JamoHangulTransliterator& o) {
    Transliterator::operator=(o);
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* JamoHangulTransliterator::clone(void) const {
    return new JamoHangulTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void JamoHangulTransliterator::handleTransliterate(Replaceable& text,
                                                   int32_t offsets[3]) const {
    /**
     * Performs transliteration changing Jamo to Hangul 
     */
    int32_t cursor = offsets[CURSOR];
    int32_t limit = offsets[LIMIT];
    if (cursor >= limit) {
        return;
    }
    
    // get last character
    UChar last = filteredCharAt(text, cursor++);
    UnicodeString str = UNICODE_STRING("a", 1);
    while (cursor < limit) {
        UChar c = filteredCharAt(text, cursor);
        UChar replacement = composeHangul(last, c);
        if (replacement != 0) {
            str[0] = replacement;
            text.handleReplaceBetween(cursor-1, cursor+1, str);
            last = replacement;
            // leave cursor where it is
            --limit; // fix up limit
        } else {
            ++cursor;
        }
    }

    offsets[LIMIT] = limit + 1;
    offsets[CURSOR] = cursor;
}

// These constants are from the Unicode book's algorithm.
// There's no need to make them class members, since they
// are only used here.

#define SBase   (0xAC00)
#define LBase   (0x1100)
#define VBase   (0x1161)
#define TBase   (0x11A7)
#define LCount  (19)
#define VCount  (21)
#define TCount  (28)
#define NCount  (VCount * TCount)   // 588
#define SCount  (LCount * NCount)   // 11172

/**
 * Return composed character (if it composes)
 * 0 otherwise
 */
UChar JamoHangulTransliterator::composeHangul(UChar last, UChar ch) {
  // check to see if two current characters are L and V
  int32_t LIndex = last - LBase;
  if (0 <= LIndex && LIndex < LCount) {
      int32_t VIndex = ch - VBase;
      if (0 <= VIndex && VIndex < VCount) {
          // make syllable of form LV
          return (UChar)(SBase + (LIndex * VCount + VIndex) * TCount);
      }
  }
  // check to see if two current characters are LV and T
  int32_t SIndex = last - SBase;
  if (0 <= SIndex && SIndex < SCount && (SIndex % TCount) == 0) {
      int32_t TIndex = ch - TBase;
      if (0 <= TIndex && TIndex <= TCount) {
          // make syllable of form LVT
          return (UChar)(last + TIndex);
      }
  }
  // if neither case was true, skip
  return 0x0000;
}    
