/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   06/07/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/name2uni.h"
#include "unicode/unifilt.h"
#include "unicode/unicode.h"
#include "unicode/convert.h"

const char* NameUnicodeTransliterator::_ID = "Name-Any";

// As of Unicode 3.0.0, the longest name is 83 characters long.
#define LONGEST_NAME 83

/**
 * Constructs a transliterator.
 */
NameUnicodeTransliterator::NameUnicodeTransliterator(
                                 UChar32 openDelim, UChar32 closeDelim,
                                 UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter),
    openDelimiter(openDelim),
    closeDelimiter(closeDelim) {
}

/**
 * Constructs a transliterator with the default delimiters '{' and
 * '}'.
 */
NameUnicodeTransliterator::NameUnicodeTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter),
    openDelimiter((UChar) 0x007B /*{*/),
    closeDelimiter((UChar) 0x007D /*}*/) {
}

/**
 * Destructor.
 */
NameUnicodeTransliterator::~NameUnicodeTransliterator() {}

/**
 * Copy constructor.
 */
NameUnicodeTransliterator::NameUnicodeTransliterator(const NameUnicodeTransliterator& o) :
    Transliterator(o),
    openDelimiter(o.openDelimiter),
    closeDelimiter(o.closeDelimiter) {}

/**
 * Assignment operator.
 */
NameUnicodeTransliterator& NameUnicodeTransliterator::operator=(
                             const NameUnicodeTransliterator& o) {
    Transliterator::operator=(o);
    openDelimiter = o.openDelimiter;
    closeDelimiter = o.closeDelimiter;
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* NameUnicodeTransliterator::clone(void) const {
    return new NameUnicodeTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void NameUnicodeTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                                    UBool isIncremental) const {
    // Accomodate the longest possible name plus padding
    UChar buf[LONGEST_NAME + 8];
    char cbuf[LONGEST_NAME + 8]; // Default converter

    // The only characters used in names are (as of Unicode 3.0.0):
    //  -0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ
    // (first character is a space).
    
    int32_t cursor = offsets.start;
    int32_t limit = offsets.limit;

    // Modes:
    // 0 - looking for open delimiter
    // 1 - after open delimiter
    int32_t mode = 0;
    int32_t ibuf = 0;
    int32_t openPos = offsets.start; // position of openDelimiter

    UnicodeString str;

    UnicodeConverter converter; // default converter

    for (; cursor < limit; ++cursor) {
        UChar c = filteredCharAt(text, cursor);

        switch (mode) {
        case 0: // looking for open delimiter
            if (c == openDelimiter) {
                openPos = cursor;
                mode = 1;
                ibuf = 0;
            }
            break;

        case 1: // after open delimiter
            // Look for [-a-zA-Z0-9].  If \w+ is found, convert it
            // to a single space.  If closeDelimiter is found, exit
            // the loop.  If any other character is found, exit the
            // loop.  If the limit is found, exit the loop.
            if (Unicode::isWhitespace(c)) {
                // Ignore leading whitespace
                if (ibuf != 0 && buf[ibuf-1] != (UChar)0x0020) {
                    buf[ibuf++] = (UChar)0x0020 /* */;
                    // If we go a bit past the longest possible name then abort
                    if (ibuf == (LONGEST_NAME + 4)) {
                        mode = 0;
                    }
                }
                continue;
            }

            if (c == closeDelimiter) {
                // Delete trailing space, if any
                if (ibuf > 0 && buf[ibuf-1] == (UChar)0x0020) {
                    --ibuf;
                }
                buf[ibuf] = 0; // Add terminating zero
                UErrorCode status = U_ZERO_ERROR;

                // Convert UChar to char
                char *out = cbuf;
                const UChar *in = buf;
                converter.fromUnicode(out, cbuf+sizeof(cbuf),
                                      in, buf+ibuf, NULL, TRUE, status);
                *out = 0;

                UChar32 ch = u_charFromName(U_UNICODE_CHAR_NAME, cbuf, &status);
                if (ch != (UChar32) 0xFFFF && U_SUCCESS(status)) {
                    // Lookup succeeded
                    str.truncate(0);
                    str.append(ch);
                    text.handleReplaceBetween(openPos, cursor+1, str);

                    // Adjust indices for the change in the length of
                    // the string.  Do not assume that str.length() ==
                    // 1, in case of surrogates.
                    int32_t delta = cursor + 1 - openPos - str.length();
                    cursor -= delta;
                    limit -= delta;
                    // assert(cursor == openPos + str.length());
                }
                // If the lookup failed, we leave things as-is and
                // still switch to mode 0 and continue.
                mode = 0;
                continue;
            }
            
            //if (c >= (UChar)0x0061 && c <= (UChar)0x007A) {
            //    c -= 0x0020; // [a-z] => [A-Z]
            //}

            // Check if c =~ [-A-Z0-9]
            if (c == (UChar)0x002D ||
                (c >= (UChar)0x0041 && c <= (UChar)0x005A) ||
                (c >= (UChar)0x0030 && c <= (UChar)0x0039)) {
                buf[ibuf++] = (char) c;
                // If we go a bit past the longest possible name then abort
                if (ibuf == (LONGEST_NAME + 4)) {
                    mode = 0;
                }
            }
            
            // Invalid character
            else {
                --cursor; // Backup and reprocess this character
                mode = 0;
            }

            break;
        }
    }
        
    offsets.contextLimit += limit - offsets.limit;
    offsets.limit = limit;
    // In incremental mode, only advance the cursor up to the last
    // open delimiter, if we are in mode 1.
    offsets.start = (mode == 1 && isIncremental) ? openPos : cursor;
}
