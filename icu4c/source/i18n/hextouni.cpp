/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "unicode/hextouni.h"
#include "unicode/rep.h"
#include "unicode/unifilt.h"
#include "unicode/unicode.h"

/**
 * ID for this transliterator.
 */
const char* HexToUnicodeTransliterator::_ID = "Hex-Unicode";

/**
 * Constructs a transliterator.
 */
HexToUnicodeTransliterator::HexToUnicodeTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter) {
}

/**
 * Copy constructor.
 */
HexToUnicodeTransliterator::HexToUnicodeTransliterator(const HexToUnicodeTransliterator& o) :
    Transliterator(o) {
}

/**
 * Assignment operator.
 */
HexToUnicodeTransliterator& HexToUnicodeTransliterator::operator=(
                                             const HexToUnicodeTransliterator& o) {
    Transliterator::operator=(o);
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* HexToUnicodeTransliterator::clone(void) const {
    return new HexToUnicodeTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleKeyboardTransliterate}.
 */
void HexToUnicodeTransliterator::handleTransliterate(Replaceable& text,
                                                     int32_t offsets[3]) const {
    /**
     * Performs transliteration changing Unicode hexadecimal
     * escapes to characters.  For example, "U+0040" -> '@'.  A fixed
     * set of prefixes is recognized: "&#92;u", "&#92;U", "u+", "U+". 
     */
    int32_t cursor = offsets[CURSOR];
    int32_t limit = offsets[LIMIT];

    int32_t maxCursor = limit - 6;

    while (cursor <= maxCursor) {
        UChar c = filteredCharAt(text, cursor + 5);
        int32_t digit0 = Unicode::digit(c, 16);
        if (digit0 < 0) {
            if (c == '\\') {
                cursor += 5;
            } else if (c == 'U' || c == 'u' || c == '+') {
                cursor += 4;
            } else {
                cursor += 6;
            }
            continue;
        }

        int32_t u = digit0;
        bool_t toTop = FALSE;

        for (int32_t i=4; i>=2; --i) {
            c = filteredCharAt(text, cursor + i);
            int32_t digit = Unicode::digit(c, 16);
            if (digit < 0) {
                if (c == 'U' || c == 'u' || c == '+') {
                    cursor += i-1;
                } else {
                    cursor += 6;
                }
                toTop = TRUE; // This is a little awkward -- it was a "continue loop:"
                break;        // statement in Java, where loop marked the while().
            } else {
                u |= digit << (4 * (5-i));
            }
        }

        if (toTop) {
            continue;
        }

        c = filteredCharAt(text, cursor);
        UChar d = filteredCharAt(text, cursor + 1);
        if (((c == 'U' || c == 'u') && d == '+')
            || (c == '\\' && (d == 'U' || d == 'u'))) {
            
            // At this point, we have a match; replace cursor..cursor+5
            // with u.
            text.handleReplaceBetween(cursor, cursor+6, UnicodeString((UChar)u));
            limit -= 5;
            maxCursor -= 5;

            ++cursor;
        } else {
            cursor += 6;
        }
    }

    offsets[LIMIT] = limit;
    offsets[CURSOR] = cursor;
}
