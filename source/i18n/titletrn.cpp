/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/uchar.h"
#include "unicode/titletrn.h"

/**
 * ID for this transliterator.
 */
const char* TitlecaseTransliterator::_ID = "Any-Title";

TitlecaseTransliterator::TitlecaseTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter) {
    // Need to look back 2 characters in the case of "can't"
    setMaximumContextLength(2);
}

/**
 * Destructor.
 */
TitlecaseTransliterator::~TitlecaseTransliterator() {}

/**
 * Copy constructor.
 */
TitlecaseTransliterator::TitlecaseTransliterator(const TitlecaseTransliterator& o) :
    Transliterator(o) {}

/**
 * Assignment operator.
 */
TitlecaseTransliterator& TitlecaseTransliterator::operator=(
                             const TitlecaseTransliterator& o) {
    Transliterator::operator=(o);
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* TitlecaseTransliterator::clone(void) const {
    return new TitlecaseTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void TitlecaseTransliterator::handleTransliterate(
                                  Replaceable& text, UTransPosition& offsets,
                                  UBool isIncremental) const {

    // NOTE: This method contains some special case code to handle
    // apostrophes between alpha characters.  We want to have
    // "can't" => "Can't" (not "Can'T").  This may be incorrect
    // for some locales, e.g., "l'arbre" => "L'Arbre" (?).
    // TODO: Revisit this.

    // Determine if there is a preceding letter character in the
    // left context (if there is any left context).
    UBool wasLastCharALetter = FALSE;
    if (offsets.start > offsets.contextStart) {
        UChar c = text.charAt(offsets.start - 1);
        // Handle the case "Can'|t", where the | marks the context
        // boundary.  We only handle a single apostrophe.
        if (c == 0x0027 /*'*/ && (offsets.start-2) >= offsets.contextStart) {
            c = text.charAt(offsets.start - 2);
        }
        wasLastCharALetter = u_isalpha(c);
    }

    // The buffer used to batch up changes to be made
    UnicodeString buffer;
    int32_t bufStart = 0;
    int32_t bufLimit = -1;

    int32_t start;
    for (start = offsets.start; start < offsets.limit; ++start) {
        // For each character, if the preceding character was a
        // non-letter, and this character is a letter, then apply
        // the titlecase transformation.  Otherwise apply the
        // lowercase transformation.
        UChar32 c = text.charAt(start);
        if (u_isalpha(c)) {
            UChar32 newChar;
            if (wasLastCharALetter) {
                newChar = u_tolower(c);
            } else {
                newChar = u_totitle(c);
            }
            if (c != newChar) {
                // This is the simple way of doing this:
                //text.replace(start, start+1,
                //             String.valueOf((char) newChar));

                // Instead, we do something more complicated that
                // minimizes the number of calls to
                // Replaceable.replace().  We batch up the changes
                // we want to make in a buffer, recording
                // our position and dumping the buffer out when a
                // non-contiguous change arrives.
                if (bufLimit == start) {
                    ++bufLimit;
                    // Fall through and append newChar below
                } else {
                    if (buffer.length() > 0) {
                        text.handleReplaceBetween(bufStart, bufLimit, buffer);
                        buffer.truncate(0);
                    }
                    bufStart = start;
                    bufLimit = start+1;
                    // Fall through and append newChar below
                }
                buffer.append(newChar);
            }
            wasLastCharALetter = TRUE;
        } else if (c == 0x0027 /*'*/ && wasLastCharALetter) {
            // Ignore a single embedded apostrophe, so that "can't" =>
            // "Can't", not "Can'T".
        } else {
            wasLastCharALetter = FALSE;
        }
    }
    // assert(start == offsets.limit);
    offsets.start = start;

    if (buffer.length() > 0) {
        text.handleReplaceBetween(bufStart, bufLimit, buffer);
    }
}
