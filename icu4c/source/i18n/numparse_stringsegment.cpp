// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "numparse_types.h"
#include "numparse_stringsegment.h"
#include "putilimp.h"
#include "unicode/utf16.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;


StringSegment::StringSegment(const UnicodeString &str) : fStr(str), fStart(0), fEnd(str.length()) {}

int32_t StringSegment::getOffset() const {
    return fStart;
}

void StringSegment::setOffset(int32_t start) {
    fStart = start;
}

void StringSegment::adjustOffset(int32_t delta) {
    fStart += delta;
}

void StringSegment::setLength(int32_t length) {
    fEnd = fStart + length;
}

void StringSegment::resetLength() {
    fEnd = fStr.length();
}

int32_t StringSegment::length() const {
    return fEnd - fStart;
}

char16_t StringSegment::charAt(int32_t index) const {
    return fStr.charAt(index + fStart);
}

UChar32 StringSegment::codePointAt(int32_t index) const {
    return fStr.char32At(index + fStart);
}

UnicodeString StringSegment::toUnicodeString() const {
    return UnicodeString(fStr, fStart, fEnd - fStart);
}

UChar32 StringSegment::getCodePoint() const {
    char16_t lead = fStr.charAt(fStart);
    if (U16_IS_LEAD(lead) && fStart + 1 < fEnd) {
        return fStr.char32At(fStart);
    } else if (U16_IS_SURROGATE(lead)) {
        return -1;
    } else {
        return lead;
    }
}

int32_t StringSegment::getCommonPrefixLength(const UnicodeString &other) {
    int32_t offset = 0;
    for (; offset < uprv_min(length(), other.length());) {
        if (charAt(offset) != other.charAt(offset)) {
            break;
        }
        offset++;
    }
    return offset;
}


#endif /* #if !UCONFIG_NO_FORMATTING */
