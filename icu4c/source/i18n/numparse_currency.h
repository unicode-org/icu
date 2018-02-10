// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMPARSE_CURRENCY_H__
#define __NUMPARSE_CURRENCY_H__

#include "numparse_types.h"
#include "charstr.h"

U_NAMESPACE_BEGIN namespace numparse {
namespace impl {


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

    const UnicodeSet* getLeadCodePoints() const override;

  private:
    // We could use Locale instead of CharString here, but
    // Locale has a non-trivial default constructor.
    CharString fLocaleName;

};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_CURRENCY_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
