// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "numparse_types.h"
#include "numparse_compositions.h"
#include "unicode/uniset.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;


bool AnyMatcher::match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const {
    int32_t initialOffset = segment.getOffset();
    bool maybeMore = false;

    // NOTE: The range-based for loop calls the virtual begin() and end() methods.
    for (auto& matcher : *this) {
        maybeMore = maybeMore || matcher->match(segment, result, status);
        if (segment.getOffset() != initialOffset) {
            // Match succeeded.
            // NOTE: Except for a couple edge cases, if a matcher accepted string A, then it will
            // accept any string starting with A. Therefore, there is no possibility that matchers
            // later in the list may be evaluated on longer strings, and we can exit the loop here.
            break;
        }
    }

    // None of the matchers succeeded.
    return maybeMore;
}

void AnyMatcher::postProcess(ParsedNumber& result) const {
    // NOTE: The range-based for loop calls the virtual begin() and end() methods.
    for (auto* matcher : *this) {
        matcher->postProcess(result);
    }
}


bool SeriesMatcher::match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const {
    ParsedNumber backup(result);

    int32_t initialOffset = segment.getOffset();
    bool maybeMore = true;
    for (auto* it = begin(); it < end();) {
        const NumberParseMatcher* matcher = *it;
        int matcherOffset = segment.getOffset();
        if (segment.length() != 0) {
            maybeMore = matcher->match(segment, result, status);
        } else {
            // Nothing for this matcher to match; ask for more.
            maybeMore = true;
        }

        bool success = (segment.getOffset() != matcherOffset);
        bool isFlexible = matcher->isFlexible();
        if (success && isFlexible) {
            // Match succeeded, and this is a flexible matcher. Re-run it.
        } else if (success) {
            // Match succeeded, and this is NOT a flexible matcher. Proceed to the next matcher.
            it++;
        } else if (isFlexible) {
            // Match failed, and this is a flexible matcher. Try again with the next matcher.
            it++;
        } else {
            // Match failed, and this is NOT a flexible matcher. Exit.
            segment.setOffset(initialOffset);
            result = backup;
            return maybeMore;
        }
    }

    // All matchers in the series succeeded.
    return maybeMore;
}

void SeriesMatcher::postProcess(ParsedNumber& result) const {
    // NOTE: The range-based for loop calls the virtual begin() and end() methods.
    for (auto* matcher : *this) {
        matcher->postProcess(result);
    }
}


ArraySeriesMatcher::ArraySeriesMatcher()
        : fMatchersLen(0) {
}

ArraySeriesMatcher::ArraySeriesMatcher(MatcherArray& matchers, int32_t matchersLen)
        : fMatchers(std::move(matchers)), fMatchersLen(matchersLen) {
}

const UnicodeSet& ArraySeriesMatcher::getLeadCodePoints() {
    // SeriesMatchers are never allowed to start with a Flexible matcher.
    U_ASSERT(!fMatchers[0]->isFlexible());
    return fMatchers[0]->getLeadCodePoints();
}

int32_t ArraySeriesMatcher::length() const {
    return fMatchersLen;
}

const NumberParseMatcher* const* ArraySeriesMatcher::begin() const {
    return fMatchers.getAlias();
}

const NumberParseMatcher* const* ArraySeriesMatcher::end() const {
    return fMatchers.getAlias() + fMatchersLen;
}

UnicodeString ArraySeriesMatcher::toString() const {
    return u"<ArraySeries>";
}


#endif /* #if !UCONFIG_NO_FORMATTING */
