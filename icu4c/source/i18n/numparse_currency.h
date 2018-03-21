// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMPARSE_CURRENCY_H__
#define __NUMPARSE_CURRENCY_H__

#include "numparse_types.h"
#include "numparse_compositions.h"
#include "charstr.h"
#include "number_currencysymbols.h"

U_NAMESPACE_BEGIN namespace numparse {
namespace impl {

using ::icu::number::impl::CurrencySymbols;

/**
 * Matches currencies according to all available strings in locale data.
 *
 * The implementation of this class is different between J and C. See #13584 for a follow-up.
 *
 * @author sffc
 */
class CurrencyNamesMatcher : public NumberParseMatcher, public UMemory {
  public:
    CurrencyNamesMatcher() = default;  // WARNING: Leaves the object in an unusable state

    CurrencyNamesMatcher(const Locale& locale, UErrorCode& status);

    bool match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const override;

    const UnicodeSet& getLeadCodePoints() override;

    UnicodeString toString() const override;

  private:
    // We could use Locale instead of CharString here, but
    // Locale has a non-trivial default constructor.
    CharString fLocaleName;

};


class CurrencyCustomMatcher : public NumberParseMatcher, public UMemory {
  public:
    CurrencyCustomMatcher() = default;  // WARNING: Leaves the object in an unusable state

    CurrencyCustomMatcher(const CurrencySymbols& currencySymbols, UErrorCode& status);

    bool match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const override;

    const UnicodeSet& getLeadCodePoints() override;

    UnicodeString toString() const override;

  private:
    UChar fCurrencyCode[4];
    UnicodeString fCurrency1;
    UnicodeString fCurrency2;
};


/**
 * An implementation of AnyMatcher, allowing for either currency data or locale currency matches.
 */
class CurrencyAnyMatcher : public AnyMatcher, public UMemory {
  public:
    CurrencyAnyMatcher();  // WARNING: Leaves the object in an unusable state

    CurrencyAnyMatcher(CurrencyNamesMatcher namesMatcher, CurrencyCustomMatcher customMatcher);

    // Needs custom move constructor/operator since constructor is nontrivial

    CurrencyAnyMatcher(CurrencyAnyMatcher&& src) U_NOEXCEPT;

    CurrencyAnyMatcher& operator=(CurrencyAnyMatcher&& src) U_NOEXCEPT;

    const UnicodeSet& getLeadCodePoints() override;

    UnicodeString toString() const override;

  protected:
    const NumberParseMatcher* const* begin() const override;

    const NumberParseMatcher* const* end() const override;

  private:
    CurrencyNamesMatcher fNamesMatcher;
    CurrencyCustomMatcher fCustomMatcher;

    const NumberParseMatcher* fMatcherArray[2];
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_CURRENCY_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
