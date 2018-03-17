// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "numparse_types.h"
#include "number_currencysymbols.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;


CurrencySymbols::CurrencySymbols(CurrencyUnit currency, const Locale& locale, UErrorCode& status)
        : fCurrency(currency), fLocaleName(locale.getName(), status) {}

UnicodeString CurrencySymbols::getNarrowCurrencySymbol(UErrorCode& status) const {
    return loadSymbol(UCURR_NARROW_SYMBOL_NAME, status);
}

UnicodeString CurrencySymbols::loadSymbol(UCurrNameStyle selector, UErrorCode& status) const {
    UBool ignoredIsChoiceFormatFillIn = FALSE;
    int32_t symbolLen = 0;
    const char16_t* symbol = ucurr_getName(
            fCurrency.getISOCurrency(),
            fLocaleName.data(),
            selector,
            &ignoredIsChoiceFormatFillIn,
            &symbolLen,
            &status);
    // Readonly-aliasing char16_t* constructor:
    return UnicodeString(TRUE, symbol, symbolLen);
}

UnicodeString CurrencySymbols::getPluralName(StandardPlural::Form plural, UErrorCode& status) const {
    UBool isChoiceFormat = FALSE;
    int32_t symbolLen = 0;
    const char16_t* symbol = ucurr_getPluralName(
            fCurrency.getISOCurrency(),
            fLocaleName.data(),
            &isChoiceFormat,
            StandardPlural::getKeyword(plural),
            &symbolLen,
            &status);
    // Readonly-aliasing char16_t* constructor:
    return UnicodeString(TRUE, symbol, symbolLen);
}


CurrencyDataSymbols::CurrencyDataSymbols(CurrencyUnit currency, const Locale& locale, UErrorCode& status)
        : CurrencySymbols(currency, locale, status) {}

UnicodeString CurrencyDataSymbols::getCurrencySymbol(UErrorCode& status) const {
    return loadSymbol(UCURR_SYMBOL_NAME, status);
}

UnicodeString CurrencyDataSymbols::getIntlCurrencySymbol(UErrorCode&) const {
    // Readonly-aliasing char16_t* constructor:
    return UnicodeString(TRUE, fCurrency.getISOCurrency(), 3);
}


CurrencyCustomSymbols::CurrencyCustomSymbols(CurrencyUnit currency, const Locale& locale,
                                             const DecimalFormatSymbols& symbols, UErrorCode& status)
        : CurrencySymbols(currency, locale, status) {
    // Hit the data bundle if the DecimalFormatSymbols version is not custom.
    // Note: the CurrencyDataSymbols implementation waits to hit the data bundle until requested.
    if (symbols.isCustomCurrencySymbol()) {
        fCurrencySymbol = symbols.getConstSymbol(DecimalFormatSymbols::kCurrencySymbol);
    } else {
        fCurrencySymbol = loadSymbol(UCURR_SYMBOL_NAME, status);
    }
    if (symbols.isCustomIntlCurrencySymbol()) {
        fIntlCurrencySymbol = symbols.getConstSymbol(DecimalFormatSymbols::kIntlCurrencySymbol);
    } else {
        // UnicodeString copy constructor since we don't know about the lifetime of the CurrencyUnit
        fIntlCurrencySymbol = UnicodeString(currency.getISOCurrency(), 3);
    }
}

UnicodeString CurrencyCustomSymbols::getCurrencySymbol(UErrorCode&) const {
    return fCurrencySymbol;
}

UnicodeString CurrencyCustomSymbols::getIntlCurrencySymbol(UErrorCode&) const {
    return fIntlCurrencySymbol;
}


CurrencyUnit
icu::number::impl::resolveCurrency(const DecimalFormatProperties& properties, const Locale& locale,
                                   UErrorCode& status) {
    if (!properties.currency.isNull()) {
        return properties.currency.getNoError();
    } else {
        UErrorCode localStatus = U_ZERO_ERROR;
        char16_t buf[4] = {};
        ucurr_forLocale(locale.getName(), buf, 4, &localStatus);
        if (U_SUCCESS(localStatus)) {
            return CurrencyUnit(buf, status);
        } else {
            // Default currency (XXX)
            return CurrencyUnit();
        }
    }
}


#endif /* #if !UCONFIG_NO_FORMATTING */
