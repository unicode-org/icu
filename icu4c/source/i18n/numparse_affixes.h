// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMPARSE_AFFIXES_H__
#define __NUMPARSE_AFFIXES_H__

#include "numparse_types.h"
#include "numparse_symbols.h"
#include "numparse_currency.h"
#include "number_affixutils.h"

U_NAMESPACE_BEGIN namespace numparse {
namespace impl {

// Forward-declaration of implementation classes for friending
class AffixPatternMatcherBuilder;
class AffixPatternMatcher;

class AffixTokenMatcherFactory {
  public:
    AffixTokenMatcherFactory(const UChar* currencyCode, const UnicodeString& currency1,
                             const UnicodeString& currency2, const DecimalFormatSymbols& dfs,
                             IgnorablesMatcher* ignorables, const Locale& locale);

  private:
    UChar currencyCode[4];
    const UnicodeString& currency1;
    const UnicodeString& currency2;
    const DecimalFormatSymbols& dfs;
    IgnorablesMatcher* ignorables;
    const Locale locale;

    // NOTE: These are default-constructed and should not be used until initialized.
    MinusSignMatcher minusSign;
    PlusSignMatcher plusSign;
    PercentMatcher percent;
    PermilleMatcher permille;
    CurrencyAnyMatcher currency;

    friend class AffixPatternMatcherBuilder;
    friend class AffixPatternMatcher;
};


class CodePointMatcher : public NumberParseMatcher, public UMemory {
  public:
    CodePointMatcher() = default;  // WARNING: Leaves the object in an unusable state

    CodePointMatcher(UChar32 cp);

    bool match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const override;

    const UnicodeSet& getLeadCodePoints() override;

  private:
    UChar32 fCp;
};


class AffixPatternMatcherBuilder : public ::icu::number::impl::TokenConsumer {
  public:
    AffixPatternMatcherBuilder(const UnicodeString& pattern, AffixTokenMatcherFactory& factory,
                               IgnorablesMatcher* ignorables);

    void consumeToken(::icu::number::impl::AffixPatternType type, UChar32 cp, UErrorCode& status) override;

    /** NOTE: You can build only once! */
    AffixPatternMatcher build();

  private:
    ArraySeriesMatcher::MatcherArray fMatchers;
    int32_t fMatchersLen;
    int32_t fLastTypeOrCp;

    LocalArray<CodePointMatcher> fCodePointMatchers;
    int32_t fCodePointMatchersLen;

    const UnicodeString& fPattern;
    AffixTokenMatcherFactory& fFactory;
    IgnorablesMatcher* fIgnorables;

    void addMatcher(NumberParseMatcher& matcher);
};


class AffixPatternMatcher : public ArraySeriesMatcher {
  public:
    static AffixPatternMatcher fromAffixPattern(const UnicodeString& affixPattern,
                                                AffixTokenMatcherFactory& factory,
                                                parse_flags_t parseFlags, bool* success,
                                                UErrorCode& status);

  private:
    UnicodeString fPattern;

    // We need to own the variable number of CodePointMatchers.
    LocalArray<CodePointMatcher> fCodePointMatchers;

    AffixPatternMatcher() = default;  // WARNING: Leaves the object in an unusable state

    AffixPatternMatcher(MatcherArray& matchers, int32_t matchersLen, const UnicodeString& pattern,
                        CodePointMatcher* codePointMatchers);

    friend class AffixPatternMatcherBuilder;
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_AFFIXES_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
