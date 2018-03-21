// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "numparse_types.h"
#include "numparse_scientific.h"
#include "numparse_unisets.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;


ScientificMatcher::ScientificMatcher(const DecimalFormatSymbols& dfs, const Grouper& grouper)
        : fExponentSeparatorString(dfs.getConstSymbol(DecimalFormatSymbols::kExponentialSymbol)),
          fExponentMatcher(dfs, grouper, PARSE_FLAG_INTEGER_ONLY) {
}

bool ScientificMatcher::match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const {
    // Only accept scientific notation after the mantissa.
    // Most places use result.hasNumber(), but we need a stronger condition here (i.e., exponent is
    // not well-defined after NaN or infinity).
    if (result.quantity.bogus) {
        return false;
    }

    // First match the scientific separator, and then match another number after it.
    int overlap1 = segment.getCommonPrefixLength(fExponentSeparatorString);
    if (overlap1 == fExponentSeparatorString.length()) {
        // Full exponent separator match.

        // First attempt to get a code point, returning true if we can't get one.
        segment.adjustOffset(overlap1);
        if (segment.length() == 0) {
            return true;
        }

        // Allow a sign, and then try to match digits.
        int8_t exponentSign = 1;
        if (segment.startsWith(*unisets::get(unisets::MINUS_SIGN))) {
            exponentSign = -1;
            segment.adjustOffsetByCodePoint();
        } else if (segment.startsWith(*unisets::get(unisets::PLUS_SIGN))) {
            segment.adjustOffsetByCodePoint();
        }

        int digitsOffset = segment.getOffset();
        bool digitsReturnValue = fExponentMatcher.match(segment, result, exponentSign, status);
        if (segment.getOffset() != digitsOffset) {
            // At least one exponent digit was matched.
            result.flags |= FLAG_HAS_EXPONENT;
        } else {
            // No exponent digits were matched; un-match the exponent separator.
            segment.adjustOffset(-overlap1);
        }
        return digitsReturnValue;

    } else if (overlap1 == segment.length()) {
        // Partial exponent separator match
        return true;
    }

    // No match
    return false;
}

bool ScientificMatcher::smokeTest(const StringSegment& segment) const {
    return segment.startsWith(fExponentSeparatorString);
}

UnicodeString ScientificMatcher::toString() const {
    return u"<Scientific>";
}


#endif /* #if !UCONFIG_NO_FORMATTING */
