/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/xformtrn.h"
#include "unicode/unifilt.h"

/**
 * Constructs a transliterator.  For use by subclasses.
 */
TransformTransliterator::TransformTransliterator(const UnicodeString& id,
                                                 UnicodeFilter* adoptedFilter) :
    Transliterator(id, adoptedFilter) {
}


/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void TransformTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                                  UBool isIncremental) const {
    
    int32_t start;
    for (start = offsets.start; start < offsets.limit; ++start) {
        // Scan for the first character that is != its transform.
        // If there are none, we fall out without doing anything.
        UChar32 c = filteredCharAt(text, start);
        if (hasTransform(c)) {
            // There is a transforming character at start.  Break
            // up the remaining string, from start to
            // offsets.limit, into segments of unfiltered and
            // filtered characters.  Only transform the unfiltered
            // characters.  As always, minimize the number of
            // calls to Replaceable.replace().

            int32_t len = offsets.limit - start;
            // assert(len >= 1);

            int32_t base = start;

            int32_t segStart = 0;
            int32_t segLimit;
            const UnicodeFilter* filt = getFilter();

            // lenDelta is the accumulated length difference for
            // all transformed segments.  It is new length - old
            // length.
            int32_t lenDelta = 0;

            // Temporary string used to do transformations
            UnicodeString str;

            // Set segStart, segLimit to the unfiltered segment
            // starting with start.  If the filter is null, then
            // segStart/Limit will be set to the whole string,
            // that is, 0/len.
            do {
                // Set segLimit to the first filtered char at or
                // after segStart.
                if (filt != 0) {
                    segLimit = segStart;
                    UChar c;
                    while (segLimit < len &&
                           filt->contains(c=text.charAt(base + segLimit))) {
                        ++segLimit;
                        str.append(c);
                    }
                }

                // If there is no filter then we'll do everthing at
                // once, and we'll only make one iteration of this do
                // loop.  Copy the entire range to the string.
                else {
                    segLimit = len;
                    int32_t i;
                    for (i=start; i<offsets.limit; ++i) {
                        str.append(text.charAt(i));
                    }
                }

                // Transform the unfiltered chars between segStart
                // and segLimit.
                int32_t segLen = segLimit - segStart;
                if (segLen != 0) {
                    transform(str);
                    text.handleReplaceBetween(start, start + segLen, str);
                    start += str.length();
                    lenDelta += str.length() - segLen;
                    str.truncate(0);
                }

                // Set segStart to the first unfiltered char at or
                // after segLimit.
                segStart = segLimit;
                if (filt != 0) {
                    while (segStart < len &&
                           !filt->contains(text.charAt(base + segStart))) {
                        ++segStart;
                    }
                }
                start += segStart - segLimit;

            } while (segStart < len);

            offsets.limit += lenDelta;
            offsets.contextLimit += lenDelta;
            offsets.start = offsets.limit;
            return;
        }
    }
    // assert(start == offsets.limit);
    offsets.start = start;
}
