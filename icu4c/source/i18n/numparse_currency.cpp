// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "numparse_types.h"
#include "numparse_currency.h"
#include "ucurrimp.h"
#include "unicode/errorcode.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;


CurrencyNamesMatcher::CurrencyNamesMatcher(const Locale& locale, UErrorCode& status)
        : fLocaleName(locale.getName(), -1, status) {}

bool CurrencyNamesMatcher::match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const {
    if (result.currencyCode[0] != 0) {
        return false;
    }

    // NOTE: This requires a new UnicodeString to be allocated, instead of using the StringSegment.
    // This should be fixed with #13584.
    UnicodeString segmentString = segment.toUnicodeString();

    // Try to parse the currency
    ParsePosition ppos(0);
    int32_t partialMatchLen = 0;
    uprv_parseCurrency(
            fLocaleName.data(),
            segmentString,
            ppos,
            UCURR_SYMBOL_NAME, // checks for both UCURR_SYMBOL_NAME and UCURR_LONG_NAME
            &partialMatchLen,
            result.currencyCode,
            status);

    // Possible partial match
    bool partialMatch = partialMatchLen == segment.length();

    if (U_SUCCESS(status) && ppos.getIndex() != 0) {
        // Complete match.
        // NOTE: The currency code should already be saved in the ParsedNumber.
        segment.adjustOffset(ppos.getIndex());
        result.setCharsConsumed(segment);
    }

    return partialMatch;
}

const UnicodeSet* CurrencyNamesMatcher::getLeadCodePoints() const {
    ErrorCode status;
    UnicodeSet* leadCodePoints = new UnicodeSet();
    uprv_currencyLeads(fLocaleName.data(), *leadCodePoints, status);
    // Always apply case mapping closure for currencies
    leadCodePoints->closeOver(USET_ADD_CASE_MAPPINGS);
    leadCodePoints->freeze();

    return leadCodePoints;
}


#endif /* #if !UCONFIG_NO_FORMATTING */
