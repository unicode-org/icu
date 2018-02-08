// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "numparse_types.h"
#include "numparse_symbols.h"
#include "numparse_utils.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;


SymbolMatcher::SymbolMatcher(const UnicodeString& symbolString, unisets::Key key) {
    fUniSet = unisets::get(key);
    fOwnsUniSet = false;
    if (fUniSet->contains(symbolString)) {
        fString.setToBogus();
    } else {
        fString = symbolString;
    }
}

SymbolMatcher::~SymbolMatcher() {
    if (fOwnsUniSet) {
        delete fUniSet;
        fUniSet = nullptr;
    }
}

const UnicodeSet* SymbolMatcher::getSet() {
    return fUniSet;
}

bool SymbolMatcher::match(StringSegment& segment, ParsedNumber& result, UErrorCode&) const {
    // Smoke test first; this matcher might be disabled.
    if (isDisabled(result)) {
        return false;
    }

    // Test the string first in order to consume trailing chars greedily.
    int overlap = 0;
    if (!fString.isEmpty()) {
        overlap = segment.getCommonPrefixLength(fString);
        if (overlap == fString.length()) {
            segment.adjustOffset(fString.length());
            accept(segment, result);
            return false;
        }
    }

    int cp = segment.getCodePoint();
    if (cp != -1 && fUniSet->contains(cp)) {
        segment.adjustOffset(U16_LENGTH(cp));
        accept(segment, result);
        return false;
    }

    return overlap == segment.length();
}

const UnicodeSet* SymbolMatcher::getLeadCodePoints() const {
    if (fString.isEmpty()) {
        // Assumption: for sets from UnicodeSetStaticCache, uniSet == leadCodePoints.
        return new UnicodeSet(*fUniSet);
    }

    UnicodeSet* leadCodePoints = new UnicodeSet();
    utils::putLeadCodePoints(fUniSet, leadCodePoints);
    utils::putLeadCodePoint(fString, leadCodePoints);
    leadCodePoints->freeze();
    return leadCodePoints;
}


MinusSignMatcher::MinusSignMatcher(const DecimalFormatSymbols& dfs, bool allowTrailing) : SymbolMatcher(
        dfs.getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol),
        unisets::MINUS_SIGN), fAllowTrailing(allowTrailing) {
}

bool MinusSignMatcher::isDisabled(const ParsedNumber& result) const {
    return 0 != (result.flags & FLAG_NEGATIVE) ||
           (fAllowTrailing ? false : result.seenNumber());
}

void MinusSignMatcher::accept(StringSegment& segment, ParsedNumber& result) const {
    result.flags |= FLAG_NEGATIVE;
    result.setCharsConsumed(segment);
}


#endif /* #if !UCONFIG_NO_FORMATTING */
