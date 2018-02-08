// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMPARSE_SYMBOLS_H__
#define __NUMPARSE_SYMBOLS_H__

#include "numparse_types.h"
#include "unicode/uniset.h"
#include "numparse_unisets.h"

U_NAMESPACE_BEGIN namespace numparse {
namespace impl {


class SymbolMatcher : public NumberParseMatcher, public UMemory {
  public:
    ~SymbolMatcher() override;

    const UnicodeSet* getSet();

    bool match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const override;

    const UnicodeSet* getLeadCodePoints() const override;

    virtual bool isDisabled(const ParsedNumber& result) const = 0;

    virtual void accept(StringSegment& segment, ParsedNumber& result) const = 0;

  protected:
    UnicodeString fString;
    const UnicodeSet* fUniSet;
    bool fOwnsUniSet;

    SymbolMatcher(const UnicodeString& symbolString, unisets::Key key);
};


class MinusSignMatcher : public SymbolMatcher {
  public:
    MinusSignMatcher(const DecimalFormatSymbols& dfs, bool allowTrailing);

  protected:
    bool isDisabled(const ParsedNumber& result) const override;

    void accept(StringSegment& segment, ParsedNumber& result) const override;

  private:
    bool fAllowTrailing;
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_SYMBOLS_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
