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


namespace {

inline const UnicodeSet& minusSignSet() {
    return *unisets::get(unisets::MINUS_SIGN);
}

inline const UnicodeSet& plusSignSet() {
    return *unisets::get(unisets::PLUS_SIGN);
}

} // namespace


ScientificMatcher::ScientificMatcher(const DecimalFormatSymbols& dfs, const Grouper& grouper)
        : fExponentSeparatorString(dfs.getConstSymbol(DecimalFormatSymbols::kExponentialSymbol)),
          fExponentMatcher(dfs, grouper, PARSE_FLAG_INTEGER_ONLY | PARSE_FLAG_GROUPING_DISABLED) {

    const UnicodeString& minusSign = dfs.getConstSymbol(DecimalFormatSymbols::kMinusSignSymbol);
    if (minusSignSet().contains(minusSign)) {
        fCustomMinusSign.setToBogus();
    } else {
        fCustomMinusSign = minusSign;
    }

    const UnicodeString& plusSign = dfs.getConstSymbol(DecimalFormatSymbols::kPlusSignSymbol);
    if (plusSignSet().contains(plusSign)) {
        fCustomPlusSign.setToBogus();
    } else {
        fCustomPlusSign = plusSign;
    }
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
        if (segment.length() == overlap1) {
            return true;
        }
        segment.adjustOffset(overlap1);

        // Allow a sign, and then try to match digits.
        int8_t exponentSign = 1;
        if (segment.startsWith(minusSignSet())) {
            exponentSign = -1;
            segment.adjustOffsetByCodePoint();
        } else if (segment.startsWith(plusSignSet())) {
            segment.adjustOffsetByCodePoint();
        } else if (segment.startsWith(fCustomMinusSign)) {
            int32_t overlap2 = segment.getCommonPrefixLength(fCustomMinusSign);
            if (overlap2 != fCustomMinusSign.length()) {
                // Partial custom sign match; un-match the exponent separator.
                segment.adjustOffset(-overlap1);
                return true;
            }
            exponentSign = -1;
            segment.adjustOffset(overlap2);
        } else if (segment.startsWith(fCustomPlusSign)) {
            int32_t overlap2 = segment.getCommonPrefixLength(fCustomPlusSign);
            if (overlap2 != fCustomPlusSign.length()) {
                // Partial custom sign match; un-match the exponent separator.
                segment.adjustOffset(-overlap1);
                return true;
            }
            segment.adjustOffset(overlap2);
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
