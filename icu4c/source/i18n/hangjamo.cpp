/*
**********************************************************************
*   Copyright (c) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   01/17/2000  aliu        Ported from Java.
**********************************************************************
*/
#include "unicode/hangjamo.h"
#include "unicode/rep.h"
#include "unicode/unifilt.h"
#include "unicode/unicode.h"

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

/**
 * ID for this transliterator.
 */
const char* HangulJamoTransliterator::_ID = "Hangul-Jamo";

/**
 * Constructs a transliterator.
 */
HangulJamoTransliterator::HangulJamoTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter) {
}

/**
 * Copy constructor.
 */
HangulJamoTransliterator::HangulJamoTransliterator(const HangulJamoTransliterator& o) :
    Transliterator(o) {
}

/**
 * Assignment operator.
 */
HangulJamoTransliterator& HangulJamoTransliterator::operator=(
                                             const HangulJamoTransliterator& o) {
    Transliterator::operator=(o);
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* HangulJamoTransliterator::clone(void) const {
    return new HangulJamoTransliterator(*this);
}

/**
 * Transliterates a segment of a string.  <code>Transliterator</code> API.
 * @param text the string to be transliterated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @return the new limit index
 */
int32_t HangulJamoTransliterator::transliterate(Replaceable& text,
                                                int32_t start, int32_t limit) const {
    int32_t offsets[3] = { start, limit, start };
    handleKeyboardTransliterate(text, offsets);
    return offsets[LIMIT];
}

/**
 * Implements {@link Transliterator#handleKeyboardTransliterate}.
 */
void HangulJamoTransliterator::handleKeyboardTransliterate(Replaceable& text,
                                                           int32_t offsets[3]) const {
    int32_t cursor = offsets[CURSOR];
    int32_t limit = offsets[LIMIT];

    UnicodeString replacement;
    while (cursor < limit) {
        UChar c = filteredCharAt(text, cursor);
        if (decomposeHangul(c, replacement)) {
            text.handleReplaceBetween(cursor, cursor+1, replacement);
            cursor += replacement.length(); // skip over replacement
            limit += replacement.length() - 1; // fix up limit
        } else {
            ++cursor;
        }
    }

    offsets[LIMIT] = limit;
    offsets[CURSOR] = cursor;
}

bool_t HangulJamoTransliterator::decomposeHangul(UChar s, UnicodeString& result) {
    int32_t SIndex = s - SBase;
    if (0 > SIndex || SIndex >= SCount) {
        return FALSE;
    }
    int32_t L = LBase + SIndex / NCount;
    int32_t V = VBase + (SIndex % NCount) / TCount;
    int32_t T = TBase + SIndex % TCount;
    result.truncate(0);
    result.append((UChar)L);
    result.append((UChar)V);
    if (T != TBase) {
        result.append((UChar)T);
    }
    return TRUE;
}

UChar HangulJamoTransliterator::filteredCharAt(Replaceable& text, int32_t i) const {
    UChar c;
    const UnicodeFilter* filter = getFilter();
    return (filter == 0) ? text.charAt(i) :
        (filter->isIn(c = text.charAt(i)) ? c : (UChar)0xFFFF);
}

/**
 * Return the length of the longest context required by this transliterator.
 * This is <em>preceding</em> context.
 * @param direction either <code>FORWARD</code> or <code>REVERSE</code>
 * @return maximum number of preceding context characters this
 * transliterator needs to examine
 */
int32_t HangulJamoTransliterator::getMaximumContextLength(void) const {
    return 0;
}
