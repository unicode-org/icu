// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "numparse_types.h"
#include "numparse_affixes.h"
#include "numparse_utils.h"
#include "number_utils.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;
using namespace icu::number;
using namespace icu::number::impl;


AffixPatternMatcherBuilder::AffixPatternMatcherBuilder(const UnicodeString& pattern,
                                                       AffixTokenMatcherWarehouse& warehouse,
                                                       IgnorablesMatcher* ignorables)
        : fMatchersLen(0),
          fLastTypeOrCp(0),
          fPattern(pattern),
          fWarehouse(warehouse),
          fIgnorables(ignorables) {}

void AffixPatternMatcherBuilder::consumeToken(AffixPatternType type, UChar32 cp, UErrorCode& status) {
    // This is called by AffixUtils.iterateWithConsumer() for each token.

    // Add an ignorables matcher between tokens except between two literals, and don't put two
    // ignorables matchers in a row.
    if (fIgnorables != nullptr && fMatchersLen > 0 &&
        (fLastTypeOrCp < 0 || !fIgnorables->getSet()->contains(fLastTypeOrCp))) {
        addMatcher(*fIgnorables);
    }

    if (type != TYPE_CODEPOINT) {
        // Case 1: the token is a symbol.
        switch (type) {
            case TYPE_MINUS_SIGN:
                addMatcher(fWarehouse.minusSign = {fWarehouse.dfs, true});
                break;
            case TYPE_PLUS_SIGN:
                addMatcher(fWarehouse.plusSign = {fWarehouse.dfs, true});
                break;
            case TYPE_PERCENT:
                addMatcher(fWarehouse.percent = {fWarehouse.dfs});
                break;
            case TYPE_PERMILLE:
                addMatcher(fWarehouse.permille = {fWarehouse.dfs});
                break;
            case TYPE_CURRENCY_SINGLE:
            case TYPE_CURRENCY_DOUBLE:
            case TYPE_CURRENCY_TRIPLE:
            case TYPE_CURRENCY_QUAD:
            case TYPE_CURRENCY_QUINT:
                // All currency symbols use the same matcher
                addMatcher(
                        fWarehouse.currency = {
                                CurrencyNamesMatcher(
                                        fWarehouse.locale, status), CurrencyCustomMatcher(
                                        fWarehouse.currencyCode,
                                        fWarehouse.currency1,
                                        fWarehouse.currency2)});
                break;
            default:
                U_ASSERT(FALSE);
        }

    } else if (fIgnorables != nullptr && fIgnorables->getSet()->contains(cp)) {
        // Case 2: the token is an ignorable literal.
        // No action necessary: the ignorables matcher has already been added.

    } else {
        // Case 3: the token is a non-ignorable literal.
        addMatcher(fWarehouse.nextCodePointMatcher(cp));
    }
    fLastTypeOrCp = type != TYPE_CODEPOINT ? type : cp;
}

void AffixPatternMatcherBuilder::addMatcher(NumberParseMatcher& matcher) {
    if (fMatchersLen >= fMatchers.getCapacity()) {
        fMatchers.resize(fMatchersLen * 2, fMatchersLen);
    }
    fMatchers[fMatchersLen++] = &matcher;
}

AffixPatternMatcher AffixPatternMatcherBuilder::build() {
    return AffixPatternMatcher(fMatchers, fMatchersLen, fPattern);
}


AffixTokenMatcherWarehouse::AffixTokenMatcherWarehouse(const UChar* currencyCode,
                                                       const UnicodeString& currency1,
                                                       const UnicodeString& currency2,
                                                       const DecimalFormatSymbols& dfs,
                                                       IgnorablesMatcher* ignorables, const Locale& locale)
        : currency1(currency1),
          currency2(currency2),
          dfs(dfs),
          ignorables(ignorables),
          locale(locale),
          codePointCount(0),
          codePointNumBatches(0) {
    utils::copyCurrencyCode(this->currencyCode, currencyCode);
}

AffixTokenMatcherWarehouse::~AffixTokenMatcherWarehouse() {
    // Delete the variable number of batches of code point matchers
    for (int32_t i=0; i<codePointNumBatches; i++) {
        delete[] codePointsOverflow[i];
    }
}

CodePointMatcher& AffixTokenMatcherWarehouse::nextCodePointMatcher(UChar32 cp) {
    if (codePointCount < CODE_POINT_STACK_CAPACITY) {
        return codePoints[codePointCount++] = {cp};
    }
    int32_t totalCapacity = CODE_POINT_STACK_CAPACITY + codePointNumBatches * CODE_POINT_BATCH_SIZE;
    if (codePointCount >= totalCapacity) {
        // Need a new batch
        auto* nextBatch = new CodePointMatcher[CODE_POINT_BATCH_SIZE];
        if (codePointNumBatches >= codePointsOverflow.getCapacity()) {
            // Need more room for storing pointers to batches
            codePointsOverflow.resize(codePointNumBatches * 2, codePointNumBatches);
        }
        codePointsOverflow[codePointNumBatches++] = nextBatch;
    }
    return codePointsOverflow[codePointNumBatches - 1][(codePointCount++ - CODE_POINT_STACK_CAPACITY) %
                                                        CODE_POINT_BATCH_SIZE] = {cp};
}


CodePointMatcher::CodePointMatcher(UChar32 cp)
        : fCp(cp) {}

bool CodePointMatcher::match(StringSegment& segment, ParsedNumber& result, UErrorCode&) const {
    if (segment.matches(fCp)) {
        segment.adjustOffsetByCodePoint();
        result.setCharsConsumed(segment);
    }
    return false;
}

const UnicodeSet& CodePointMatcher::getLeadCodePoints() {
    if (fLocalLeadCodePoints.isNull()) {
        auto* leadCodePoints = new UnicodeSet();
        leadCodePoints->add(fCp);
        leadCodePoints->freeze();
        fLocalLeadCodePoints.adoptInstead(leadCodePoints);
    }
    return *fLocalLeadCodePoints;
}


AffixPatternMatcher AffixPatternMatcher::fromAffixPattern(const UnicodeString& affixPattern,
                                                          AffixTokenMatcherWarehouse& warehouse,
                                                          parse_flags_t parseFlags, bool* success,
                                                          UErrorCode& status) {
    if (affixPattern.isEmpty()) {
        *success = false;
        return {};
    }
    *success = true;

    IgnorablesMatcher* ignorables;
    if (0 != (parseFlags & PARSE_FLAG_EXACT_AFFIX)) {
        ignorables = nullptr;
    } else {
        ignorables = warehouse.ignorables;
    }

    AffixPatternMatcherBuilder builder(affixPattern, warehouse, ignorables);
    AffixUtils::iterateWithConsumer(UnicodeStringCharSequence(affixPattern), builder, status);
    return builder.build();
}

AffixPatternMatcher::AffixPatternMatcher(MatcherArray& matchers, int32_t matchersLen,
                                         const UnicodeString& pattern)
        : ArraySeriesMatcher(matchers, matchersLen), fPattern(pattern) {
}


#endif /* #if !UCONFIG_NO_FORMATTING */
