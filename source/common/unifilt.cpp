/*
* Copyright (C) 2001, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/18/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/unifilt.h"
#include "unicode/rep.h"

U_NAMESPACE_BEGIN

/**
 * UnicodeFunctor API.  Cast 'this' to a UnicodeMatcher* pointer
 * and return the pointer.
 */
UnicodeMatcher* UnicodeFilter::toMatcher() const {
    return (UnicodeMatcher*) this;
}

/**
 * Default implementation of UnicodeMatcher::matches() for Unicode
 * filters.  Matches a single code point at offset (either one or
 * two 16-bit code units).
 */
UMatchDegree UnicodeFilter::matches(const Replaceable& text,
                                    int32_t& offset,
                                    int32_t limit,
                                    UBool incremental) {
    UChar32 c;
    if (offset < limit &&
        contains(c = text.char32At(offset))) {
        offset += UTF_CHAR_LENGTH(c);
        return U_MATCH;
    }
    if (offset > limit &&
        contains(c = text.char32At(offset))) {
        // Backup offset by 1, unless the preceding character is a
        // surrogate pair -- then backup by 2 (keep offset pointing at
        // the lead surrogate).
        --offset;
        if (offset >= 0) {
            offset -= UTF_CHAR_LENGTH(text.char32At(offset)) - 1;
        }
        return U_MATCH;
    }
    if (incremental && offset == limit) {
        return U_PARTIAL_MATCH;
    }
    return U_MISMATCH;
}

// Stub this out for filters that do not implement a pattern
UnicodeString& UnicodeFilter::toPattern(UnicodeString& result,
                                        UBool escapeUnprintable) const {
    return result;
}

// Stub this out for filters that do not implement indexing
UBool UnicodeFilter::matchesIndexValue(uint8_t v) const {
    return FALSE;
}

// Stub this out for filters that do not implement this
void UnicodeFilter::addMatchSetTo(UnicodeSet& toUnionTo) const {
}

U_NAMESPACE_END

//eof
