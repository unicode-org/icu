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

using ::icu::number::impl::AffixPatternProvider;
using ::icu::number::impl::TokenConsumer;


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
    AffixTokenMatcherWarehouse() = default;  // WARNING: Leaves the object in an unusable state

    AffixTokenMatcherWarehouse(const UChar* currencyCode, const UnicodeString* currency1,
                               const UnicodeString* currency2, const DecimalFormatSymbols* dfs,
                               IgnorablesMatcher* ignorables, const Locale* locale);

    AffixTokenMatcherWarehouse(AffixTokenMatcherWarehouse&& src) = default;

    ~AffixTokenMatcherWarehouse();

    NumberParseMatcher& minusSign();

    NumberParseMatcher& plusSign();

    NumberParseMatcher& percent();

    NumberParseMatcher& permille();

    NumberParseMatcher& currency(UErrorCode& status);

    NumberParseMatcher& nextCodePointMatcher(UChar32 cp);

  private:
    // NOTE: The following fields may be unsafe to access after construction is done!
    UChar currencyCode[4];
    const UnicodeString* currency1;
    const UnicodeString* currency2;
    const DecimalFormatSymbols* dfs;
    IgnorablesMatcher* ignorables;
    const Locale* locale;

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


class AffixPatternMatcherBuilder : public TokenConsumer {
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
    AffixPatternMatcher() = default;  // WARNING: Leaves the object in an unusable state

    static AffixPatternMatcher fromAffixPattern(const UnicodeString& affixPattern,
                                                AffixTokenMatcherWarehouse& warehouse,
                                                parse_flags_t parseFlags, bool* success,
                                                UErrorCode& status);

    UnicodeString getPattern() const;

    bool operator==(const AffixPatternMatcher& other) const;

  private:
    CompactUnicodeString<4> fPattern;

    AffixPatternMatcher(MatcherArray& matchers, int32_t matchersLen, const UnicodeString& pattern);

    friend class AffixPatternMatcherBuilder;
};


class AffixMatcher : public NumberParseMatcher, public UMemory {
  public:
    AffixMatcher() = default;  // WARNING: Leaves the object in an unusable state

    AffixMatcher(AffixPatternMatcher* prefix, AffixPatternMatcher* suffix, result_flags_t flags);

    // static void createMatchers() is the constructor for AffixMatcherWarehouse in C++

    bool match(StringSegment& segment, ParsedNumber& result, UErrorCode& status) const override;

    void postProcess(ParsedNumber& result) const override;

    const UnicodeSet& getLeadCodePoints() override;

  private:
    AffixPatternMatcher* fPrefix;
    AffixPatternMatcher* fSuffix;
    result_flags_t fFlags;

    /**
     * Helper method to return whether the given AffixPatternMatcher equals the given pattern string.
     * Either both arguments must be null or the pattern string inside the AffixPatternMatcher must equal
     * the given pattern string.
     */
    static bool matched(const AffixPatternMatcher* affix, const UnicodeString& patternString);
};


/**
 * A C++-only class to retain ownership of the AffixMatchers needed for parsing.
 */
class AffixMatcherWarehouse {
  public:
    AffixMatcherWarehouse() = default;  // WARNING: Leaves the object in an unusable state

    // in Java, this is AffixMatcher#createMatchers()
    AffixMatcherWarehouse(const AffixPatternProvider& patternInfo, NumberParserImpl& output,
                          AffixTokenMatcherWarehouse& warehouse, const IgnorablesMatcher& ignorables,
                          parse_flags_t parseFlags, UErrorCode& status);

  private:
    // 9 is the limit: positive, zero, and negative, each with prefix, suffix, and prefix+suffix
    AffixMatcher fAffixMatchers[9];
    // 6 is the limit: positive, zero, and negative, a prefix and a suffix for each
    AffixPatternMatcher fAffixPatternMatchers[6];
    // Store all the tokens used by the AffixPatternMatchers
    AffixTokenMatcherWarehouse fAffixTokenMatcherWarehouse;

    static bool isInteresting(const AffixPatternProvider& patternInfo, const IgnorablesMatcher& ignorables,
                              parse_flags_t parseFlags, UErrorCode& status);

    /**
     * Helper method to return whether (1) both lhs and rhs are null/invalid, or (2) if they are both
     * valid, whether they are equal according to operator==.  Similar to Java Objects.equals()
     */
    static bool equals(const AffixPatternMatcher* lhs, const AffixPatternMatcher* rhs);
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_AFFIXES_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
