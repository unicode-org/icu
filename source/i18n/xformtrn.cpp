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

U_NAMESPACE_BEGIN

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
        UChar32 c = text.charAt(start);
        if (hasTransform(c)) {
            // There is a transforming character at start.

            int32_t len = offsets.limit - start;
            // assert(len >= 1);

            // Temporary string used to do transformations
            UnicodeString str;
            for (int32_t i=start; i<offsets.limit; ++i) {
                str.append(text.charAt(i));
            }
            
            // Transform the characters
            transform(str);
            text.handleReplaceBetween(start, start + len, str);
            start += str.length();

            int32_t lenDelta = str.length() - len;
            offsets.limit += lenDelta;
            offsets.contextLimit += lenDelta;
            offsets.start = offsets.limit;
            return;
        }
    }
    // assert(start == offsets.limit);
    offsets.start = offsets.limit;
}

U_NAMESPACE_END
