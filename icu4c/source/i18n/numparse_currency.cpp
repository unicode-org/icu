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


CurrencyNamesMatcher::CurrencyNamesMatcher(const Locale& locale, UErrorCode& status)
        : fLocaleName(locale.getName(), -1, status) {
    uprv_currencyLeads(fLocaleName.data(), fLeadCodePoints, status);
    // Always apply case mapping closure for currencies
    fLeadCodePoints.closeOver(USET_ADD_CASE_MAPPINGS);
    fLeadCodePoints.freeze();
}

bool CurrencyNamesMatcher::match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const {
    if (result.currencyCode[0] != 0) {
        return false;
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

bool CurrencyNamesMatcher::smokeTest(const StringSegment& segment) const {
    return segment.startsWith(fLeadCodePoints);
}

UnicodeString CurrencyNamesMatcher::toString() const {
    return u"<CurrencyNames>";
}


CurrencyCustomMatcher::CurrencyCustomMatcher(const CurrencySymbols& currencySymbols, UErrorCode& status)
        : fCurrency1(currencySymbols.getCurrencySymbol(status)),
          fCurrency2(currencySymbols.getIntlCurrencySymbol(status)) {
    utils::copyCurrencyCode(fCurrencyCode, currencySymbols.getIsoCode());
}

bool CurrencyCustomMatcher::match(StringSegment& segment, ParsedNumber& result, UErrorCode&) const {
    if (result.currencyCode[0] != 0) {
        return false;
    }

    int overlap1 = segment.getCommonPrefixLength(fCurrency1);
    if (overlap1 == fCurrency1.length()) {
        utils::copyCurrencyCode(result.currencyCode, fCurrencyCode);
        segment.adjustOffset(overlap1);
        result.setCharsConsumed(segment);
    }

    int overlap2 = segment.getCommonPrefixLength(fCurrency2);
    if (overlap2 == fCurrency2.length()) {
        utils::copyCurrencyCode(result.currencyCode, fCurrencyCode);
        segment.adjustOffset(overlap2);
        result.setCharsConsumed(segment);
    }

    return overlap1 == segment.length() || overlap2 == segment.length();
}

bool CurrencyCustomMatcher::smokeTest(const StringSegment& segment) const {
    return segment.startsWith(fCurrency1)
           || segment.startsWith(fCurrency2);
}

UnicodeString CurrencyCustomMatcher::toString() const {
    return u"<CurrencyCustom>";
}


CurrencyAnyMatcher::CurrencyAnyMatcher() {
    fMatcherArray[0] = &fNamesMatcher;
    fMatcherArray[1] = &fCustomMatcher;
}

CurrencyAnyMatcher::CurrencyAnyMatcher(CurrencyNamesMatcher namesMatcher,
                                       CurrencyCustomMatcher customMatcher)
        : fNamesMatcher(std::move(namesMatcher)), fCustomMatcher(std::move(customMatcher)) {
    fMatcherArray[0] = &fNamesMatcher;
    fMatcherArray[1] = &fCustomMatcher;
}

CurrencyAnyMatcher::CurrencyAnyMatcher(CurrencyAnyMatcher&& src) U_NOEXCEPT
        : fNamesMatcher(std::move(src.fNamesMatcher)), fCustomMatcher(std::move(src.fCustomMatcher)) {
    fMatcherArray[0] = &fNamesMatcher;
    fMatcherArray[1] = &fCustomMatcher;
}

CurrencyAnyMatcher& CurrencyAnyMatcher::operator=(CurrencyAnyMatcher&& src) U_NOEXCEPT {
    fNamesMatcher = std::move(src.fNamesMatcher);
    fCustomMatcher = std::move(src.fCustomMatcher);
    // Note: do NOT move fMatcherArray
    return *this;
}

const NumberParseMatcher* const* CurrencyAnyMatcher::begin() const {
    return fMatcherArray;
}

const NumberParseMatcher* const* CurrencyAnyMatcher::end() const {
    return fMatcherArray + 2;
}

UnicodeString CurrencyAnyMatcher::toString() const {
    return u"<CurrencyAny>";
}


#endif /* #if !UCONFIG_NO_FORMATTING */
