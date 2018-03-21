// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMPARSE_IMPL_H__
#define __NUMPARSE_IMPL_H__

#include "numparse_types.h"
#include "numparse_decimal.h"
#include "numparse_symbols.h"
#include "numparse_scientific.h"
#include "unicode/uniset.h"
#include "numparse_currency.h"
#include "numparse_affixes.h"
#include "number_decimfmtprops.h"
#include "unicode/localpointer.h"
#include "numparse_validators.h"

U_NAMESPACE_BEGIN namespace numparse {
namespace impl {

class NumberParserImpl : public MutableMatcherCollection {
  public:
    virtual ~NumberParserImpl();

    static NumberParserImpl* createSimpleParser(const Locale& locale, const UnicodeString& patternString,
                                                parse_flags_t parseFlags, UErrorCode& status);

    static NumberParserImpl* createParserFromProperties(
            const number::impl::DecimalFormatProperties& properties, const DecimalFormatSymbols& symbols,
            bool parseCurrency, bool optimize, UErrorCode& status);

    void addMatcher(NumberParseMatcher& matcher) override;

    void freeze();

    void parse(const UnicodeString& input, bool greedy, ParsedNumber& result, UErrorCode& status) const;

    void parse(const UnicodeString& input, int32_t start, bool greedy, ParsedNumber& result,
               UErrorCode& status) const;

    UnicodeString toString() const;

  private:
    parse_flags_t fParseFlags;
    int32_t fNumMatchers = 0;
    // NOTE: The stack capacity for fMatchers and fLeads should be the same
    MaybeStackArray<const NumberParseMatcher*, 10> fMatchers;
    MaybeStackArray<const UnicodeSet*, 10> fLeads;
    bool fComputeLeads;
    bool fFrozen = false;

    // WARNING: All of these matchers start in an undefined state (default-constructed).
    // You must use an assignment operator on them before using.
    struct {
        IgnorablesMatcher ignorables;
        InfinityMatcher infinity;
        MinusSignMatcher minusSign;
        NanMatcher nan;
        PaddingMatcher padding;
        PercentMatcher percent;
        PermilleMatcher permille;
        PlusSignMatcher plusSign;
        DecimalMatcher decimal;
        ScientificMatcher scientific;
        CurrencyNamesMatcher currencyNames;
        CurrencyCustomMatcher currencyCustom;
        AffixMatcherWarehouse affixMatcherWarehouse;
        AffixTokenMatcherWarehouse affixTokenMatcherWarehouse;
    } fLocalMatchers;
    struct {
        RequireAffixValidator affix;
        RequireCurrencyValidator currency;
        RequireDecimalSeparatorValidator decimalSeparator;
        RequireExponentValidator exponent;
        RequireNumberValidator number;
    } fLocalValidators;

    NumberParserImpl(parse_flags_t parseFlags, bool computeLeads);

    void addLeadCodePointsForMatcher(NumberParseMatcher& matcher);

    void parseGreedyRecursive(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const;

    void parseLongestRecursive(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const;
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_IMPL_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
