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
    if (cursor >= limit) return;

    int32_t count;
    if (limit - cursor > 1) {
        count = 5; // debugging spot
    }

    UChar last = filteredCharAt(text, cursor++);
    UnicodeString str("a", 1);
    while (cursor <= limit) {
        UChar next = 0xFFFF; // go over end of string, just in case
        if (cursor < limit) next = filteredCharAt(text, cursor);
        UChar replacement = composeHangul(last, next, count);
        if (replacement != last) {
            str[0] = replacement;
            text.handleReplaceBetween(cursor-1, cursor-1 + count, str);
            limit = limit - count + 1; // fix up limit 2 => -1, 1 => 0
            last = replacement;
            if (next == 0xFFFF) break;
            // don't change cursor, so we revisit char
        } else {
            ++cursor;
            last = next;
        }
    }

    offsets[LIMIT] = limit + 1;
    offsets[CURSOR] = cursor;
}

// These constants are from the Unicode book's algorithm.

#define SBase   (0xAC00)
#define LBase   (0x1100)
#define VBase   (0x1161)
#define TBase   (0x11A7)
#define LCount  (19)
#define VCount  (21)
#define TCount  (28)
#define NCount  (VCount * TCount)   // 588
#define SCount  (LCount * NCount)   // 11172
#define LLimit  (0x1200)

/**
 * Return composed character (if it is a modern jamo)
 * last otherwise.
 * If there is a replacement, returns count[0] = 2 if ch was used, 1 otherwise
 */
UChar JamoHangulTransliterator::composeHangul(UChar last, UChar ch, int32_t& count) {
    count = 2; // default is replace 2 chars
    // check to see if two current characters are L and V
    int32_t LIndex = last - LBase;
    if (0 <= LIndex && LIndex < LCount) {
        int32_t VIndex = ch - VBase;
        if (0 <= VIndex && VIndex < VCount) {
            // make syllable of form LV
            return (UChar)(SBase + (LIndex * VCount + VIndex) * TCount);
        } else {
            // it is isolated, so fix!
            count = 1; // not using ch
            return (UChar)(SBase + (LIndex * VCount) * TCount);
        }
    }
      
    // if neither case was true, see if we have an isolated Jamo we need to fix
    if (LBase <= last && last < LLimit) {
        // need to fix: it is either medial or final!
        int32_t VIndex = last - VBase;
        if (0 <= VIndex && VIndex < VCount) {
            LIndex = 0x110B - LBase; // use empty consonant
            // make syllable of form LV
            count = 1; // not using ch
            return (UChar)(SBase + (LIndex * VCount + VIndex) * TCount);
        }
        // ok, see if final. Use null consonant + a + final
        int32_t TIndex = last - TBase;
        if (0 <= TIndex && TIndex <= TCount) {  // need to fix!
            count = 1; // not using ch
            return (UChar)(0xC544 + TIndex);
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
      
    return last;
}    
