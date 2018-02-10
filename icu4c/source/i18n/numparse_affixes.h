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


class CodePointMatcher : public NumberParseMatcher, public UMemory {
  public:
    CodePointMatcher() = default;  // WARNING: Leaves the object in an unusable state

    CodePointMatcher(UChar32 cp);

    bool match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const override;

    const UnicodeSet& getLeadCodePoints() override;

  private:
    UChar32 fCp;
};


/**
 * Small helper class that generates matchers for individual tokens for AffixPatternMatcher.
 *
 * In Java, this is called AffixTokenMatcherFactory (a "factory"). However, in C++, it is called a
 * "warehouse", because in addition to generating the matchers, it also retains ownership of them. The
 * warehouse must stay in scope for the whole lifespan of the AffixPatternMatcher that uses matchers from
 * the warehouse.
 *
 * @author sffc
 */
class AffixTokenMatcherWarehouse {
  private:
    static constexpr int32_t CODE_POINT_STACK_CAPACITY = 5; // Number of entries directly on the stack
    static constexpr int32_t CODE_POINT_BATCH_SIZE = 10; // Number of entries per heap allocation

  public:
    AffixTokenMatcherWarehouse(const UChar* currencyCode, const UnicodeString& currency1,
                               const UnicodeString& currency2, const DecimalFormatSymbols& dfs,
                               IgnorablesMatcher* ignorables, const Locale& locale);

    ~AffixTokenMatcherWarehouse();

    NumberParseMatcher& minusSign();

    NumberParseMatcher& plusSign();

    NumberParseMatcher& percent();

    NumberParseMatcher& permille();

    NumberParseMatcher& currency(UErrorCode& status);

    NumberParseMatcher& nextCodePointMatcher(UChar32 cp);

  private:
    UChar currencyCode[4];
    const UnicodeString& currency1;
    const UnicodeString& currency2;
    const DecimalFormatSymbols& dfs;
    IgnorablesMatcher* ignorables;
    const Locale locale;

    // NOTE: These are default-constructed and should not be used until initialized.
    MinusSignMatcher fMinusSign;
    PlusSignMatcher fPlusSign;
    PercentMatcher fPercent;
    PermilleMatcher fPermille;
    CurrencyAnyMatcher fCurrency;

    CodePointMatcher codePoints[CODE_POINT_STACK_CAPACITY]; // By value
    MaybeStackArray<CodePointMatcher*, 3> codePointsOverflow; // On heap in "batches"
    int32_t codePointCount; // Total for both the ones by value and on heap
    int32_t codePointNumBatches; // Number of batches in codePointsOverflow

    friend class AffixPatternMatcherBuilder;
    friend class AffixPatternMatcher;
};


class AffixPatternMatcherBuilder : public ::icu::number::impl::TokenConsumer {
  public:
    AffixPatternMatcherBuilder(const UnicodeString& pattern, AffixTokenMatcherWarehouse& warehouse,
                               IgnorablesMatcher* ignorables);

    void consumeToken(::icu::number::impl::AffixPatternType type, UChar32 cp, UErrorCode& status) override;

    /** NOTE: You can build only once! */
    AffixPatternMatcher build();

  private:
    ArraySeriesMatcher::MatcherArray fMatchers;
    int32_t fMatchersLen;
    int32_t fLastTypeOrCp;

    const UnicodeString& fPattern;
    AffixTokenMatcherWarehouse& fWarehouse;
    IgnorablesMatcher* fIgnorables;

    void addMatcher(NumberParseMatcher& matcher);
};


class AffixPatternMatcher : public ArraySeriesMatcher {
  public:
    static AffixPatternMatcher fromAffixPattern(const UnicodeString& affixPattern,
                                                AffixTokenMatcherWarehouse& warehouse,
                                                parse_flags_t parseFlags, bool* success,
                                                UErrorCode& status);

  private:
    UnicodeString fPattern;

    AffixPatternMatcher() = default;  // WARNING: Leaves the object in an unusable state

    AffixPatternMatcher(MatcherArray& matchers, int32_t matchersLen, const UnicodeString& pattern);

    friend class AffixPatternMatcherBuilder;
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_AFFIXES_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
