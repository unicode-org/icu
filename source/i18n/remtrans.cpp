/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   04/02/2001  aliu        Creation.
**********************************************************************
*/
#include "unicode/remtrans.h"

const UnicodeString RemoveTransliterator::ID = UnicodeString("Remove", "");

Transliterator* RemoveTransliterator::clone(void) const {
    return new RemoveTransliterator();
}

void RemoveTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                               UBool /*isIncremental*/) const {
    // Find runs of unfiltered characters and replace them with the
    // empty string.  This loop has been optimized to what is probably
    // an unnecessary degree.
    UnicodeString empty;
    int32_t start = offsets.start;
    for (;;) {
        // Find first unfiltered character, if any
        while (start < offsets.limit &&
               filteredCharAt(text, start) == 0xFFFE) {
            ++start;
        }
        if (start >= offsets.limit) {
            break;
        }

        // assert(start < offsets.limit &&
        //        filteredCharAt(text, start) != 0xFFFE);

        // Find last unfiltered character
        int32_t limit = start+1; // sic: +1
        while (limit < offsets.limit &&
               filteredCharAt(text, limit) != 0xFFFE) {
            ++limit;
        }

        // assert(start < limit);

        // Remove characters
        text.handleReplaceBetween(start, limit, empty);
        limit -= start; // limit <= deleted length
        offsets.contextLimit -= limit;
        offsets.limit -= limit;
    }
    offsets.start = start;
}
