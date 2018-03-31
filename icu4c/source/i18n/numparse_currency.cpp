// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "numparse_types.h"
#include "numparse_currency.h"
#include "ucurrimp.h"
#include "unicode/errorcode.h"
#include "numparse_utils.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;


CombinedCurrencyMatcher::CombinedCurrencyMatcher(const CurrencySymbols& currencySymbols,
                                                 const DecimalFormatSymbols& dfs, UErrorCode& status)
        : fCurrency1(currencySymbols.getCurrencySymbol(status)),
          fCurrency2(currencySymbols.getIntlCurrencySymbol(status)),
          afterPrefixInsert(dfs.getPatternForCurrencySpacing(UNUM_CURRENCY_INSERT, false, status)),
          beforeSuffixInsert(dfs.getPatternForCurrencySpacing(UNUM_CURRENCY_INSERT, true, status)),
          fLocaleName(dfs.getLocale().getName(), -1, status) {
    utils::copyCurrencyCode(fCurrencyCode, currencySymbols.getIsoCode());

    // Compute the full set of characters that could be the first in a currency to allow for
    // efficient smoke test.
    fLeadCodePoints.add(fCurrency1.char32At(0));
    fLeadCodePoints.add(fCurrency2.char32At(0));
    fLeadCodePoints.add(beforeSuffixInsert.char32At(0));
    uprv_currencyLeads(fLocaleName.data(), fLeadCodePoints, status);
    // Always apply case mapping closure for currencies
    fLeadCodePoints.closeOver(USET_ADD_CASE_MAPPINGS);
    fLeadCodePoints.freeze();
}

bool CombinedCurrencyMatcher::match(StringSegment& segment, ParsedNumber& result,
                                    UErrorCode& status) const {
    if (result.currencyCode[0] != 0) {
        return false;
    }

    // Try to match a currency spacing separator.
    int32_t initialOffset = segment.getOffset();
    bool maybeMore = false;
    if (result.seenNumber()) {
        int32_t overlap = segment.getCommonPrefixLength(beforeSuffixInsert);
        if (overlap == beforeSuffixInsert.length()) {
            segment.adjustOffset(overlap);
            // Note: let currency spacing be a weak match. Don't update chars consumed.
        }
        maybeMore = maybeMore || overlap == segment.length();
    }

    // Match the currency string, and reset if we didn't find one.
    maybeMore = maybeMore || matchCurrency(segment, result, status);
    if (result.currencyCode[0] == 0) {
        segment.setOffset(initialOffset);
        return maybeMore;
    }

    // Try to match a currency spacing separator.
    if (!result.seenNumber()) {
        int32_t overlap = segment.getCommonPrefixLength(afterPrefixInsert);
        if (overlap == afterPrefixInsert.length()) {
            segment.adjustOffset(overlap);
            // Note: let currency spacing be a weak match. Don't update chars consumed.
        }
        maybeMore = maybeMore || overlap == segment.length();
    }

    return maybeMore;
}

bool CombinedCurrencyMatcher::matchCurrency(StringSegment& segment, ParsedNumber& result,
                                            UErrorCode& status) const {

    int32_t overlap1 = segment.getCommonPrefixLength(fCurrency1);
    if (overlap1 == fCurrency1.length()) {
        utils::copyCurrencyCode(result.currencyCode, fCurrencyCode);
        segment.adjustOffset(overlap1);
        result.setCharsConsumed(segment);
        return segment.length() == 0;
    }

    int32_t overlap2 = segment.getCommonPrefixLength(fCurrency2);
    if (overlap2 == fCurrency2.length()) {
        utils::copyCurrencyCode(result.currencyCode, fCurrencyCode);
        segment.adjustOffset(overlap2);
        result.setCharsConsumed(segment);
        return segment.length() == 0;
    }

    // NOTE: This call site should be improved with #13584.
    const UnicodeString segmentString = segment.toTempUnicodeString();

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

    if (U_SUCCESS(status) && ppos.getIndex() != 0) {
        // Complete match.
        // NOTE: The currency code should already be saved in the ParsedNumber.
        segment.adjustOffset(ppos.getIndex());
        result.setCharsConsumed(segment);
    }

    return overlap1 == segment.length() || overlap2 == segment.length() ||
           partialMatchLen == segment.length();
}

bool CombinedCurrencyMatcher::smokeTest(const StringSegment& segment) const {
    return segment.startsWith(fLeadCodePoints);
}

UnicodeString CombinedCurrencyMatcher::toString() const {
    return u"<CombinedCurrencyMatcher>";
}


#endif /* #if !UCONFIG_NO_FORMATTING */
