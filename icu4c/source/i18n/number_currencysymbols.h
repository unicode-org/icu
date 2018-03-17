// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __SOURCE_NUMBER_CURRENCYSYMBOLS_H__
#define __SOURCE_NUMBER_CURRENCYSYMBOLS_H__

#include "numparse_types.h"
#include "charstr.h"
#include "number_decimfmtprops.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {


class CurrencySymbols {
  public:
    CurrencySymbols() = default; // default constructor: leaves class in valid but undefined state

    explicit CurrencySymbols(CurrencyUnit currency, const Locale& locale, UErrorCode& status);

    virtual UnicodeString getCurrencySymbol(UErrorCode& status) const = 0;

    virtual UnicodeString getIntlCurrencySymbol(UErrorCode& status) const = 0;

    // Put narrow and plural symbols in the base class since there is no API for overriding them
    UnicodeString getNarrowCurrencySymbol(UErrorCode& status) const;

    UnicodeString getPluralName(StandardPlural::Form plural, UErrorCode& status) const;

  protected:
    CurrencyUnit fCurrency;
    CharString fLocaleName;

    UnicodeString loadSymbol(UCurrNameStyle selector, UErrorCode& status) const;
};


class CurrencyDataSymbols : public CurrencySymbols, public UMemory {
  public:
    CurrencyDataSymbols() = default; // default constructor: leaves class in valid but undefined state

    CurrencyDataSymbols(CurrencyUnit currency, const Locale& locale, UErrorCode& status);

    UnicodeString getCurrencySymbol(UErrorCode& status) const U_OVERRIDE;

    UnicodeString getIntlCurrencySymbol(UErrorCode& status) const U_OVERRIDE;
};


class CurrencyCustomSymbols : public CurrencySymbols, public UMemory {
  public:
    CurrencyCustomSymbols() = default; // default constructor: leaves class in valid but undefined state

    CurrencyCustomSymbols(CurrencyUnit currency, const Locale& locale, const DecimalFormatSymbols& symbols,
                          UErrorCode& status);

    UnicodeString getCurrencySymbol(UErrorCode& status) const U_OVERRIDE;

    UnicodeString getIntlCurrencySymbol(UErrorCode& status) const U_OVERRIDE;

  private:
    UnicodeString fCurrencySymbol;
    UnicodeString fIntlCurrencySymbol;
};


/**
 * Resolves the effective currency from the property bag.
 */
CurrencyUnit
resolveCurrency(const DecimalFormatProperties& properties, const Locale& locale, UErrorCode& status);


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__SOURCE_NUMBER_CURRENCYSYMBOLS_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
