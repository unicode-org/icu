// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "number_types.h"
#include "number_patternstring.h"
#include "numparse_types.h"
#include "numparse_impl.h"
#include "numparse_symbols.h"
#include "numparse_decimal.h"
#include "unicode/numberformatter.h"

using namespace icu;
using namespace icu::number;
using namespace icu::number::impl;
using namespace icu::numparse;
using namespace icu::numparse::impl;


NumberParserImpl*
NumberParserImpl::createSimpleParser(const Locale& locale, const UnicodeString& patternString,
                                     parse_flags_t parseFlags, UErrorCode& status) {

    auto* parser = new NumberParserImpl(parseFlags, true);
    DecimalFormatSymbols symbols(locale, status);

//    IgnorablesMatcher* ignorables = IgnorablesMatcher.getDefault();
//
//    MatcherFactory factory = new MatcherFactory();
//    factory.currency = Currency.getInstance("USD");
//    factory.symbols = symbols;
//    factory.ignorables = ignorables;
//    factory.locale = locale;
//    factory.parseFlags = parseFlags;

    ParsedPatternInfo patternInfo;
    PatternParser::parseToPatternInfo(patternString, patternInfo, status);
//    AffixMatcher.createMatchers(patternInfo, parser, factory, ignorables, parseFlags);

    Grouper grouper = Grouper::forStrategy(UNUM_GROUPING_AUTO);
    grouper.setLocaleData(patternInfo, locale);

//    parser.addMatcher({ignorables, false});
    parser->addAndAdoptMatcher(new DecimalMatcher(symbols, grouper, parseFlags));
    parser->addAndAdoptMatcher(new MinusSignMatcher(symbols, false));
//    parser.addMatcher(NanMatcher.getInstance(symbols, parseFlags));
//    parser.addMatcher(ScientificMatcher.getInstance(symbols, grouper, parseFlags));
//    parser.addMatcher(CurrencyTrieMatcher.getInstance(locale));
//    parser.addMatcher(new RequireNumberMatcher());

    parser->freeze();
    return parser;
}

NumberParserImpl::NumberParserImpl(parse_flags_t parseFlags, bool computeLeads)
        : fParseFlags(parseFlags), fComputeLeads(computeLeads) {
}

NumberParserImpl::~NumberParserImpl() {
    for (int32_t i = 0; i < fNumMatchers; i++) {
        delete (fMatchers[i]);
        if (fComputeLeads) {
            delete (fLeads[i]);
        }
    }
    fNumMatchers = 0;
}

void NumberParserImpl::addAndAdoptMatcher(const NumberParseMatcher* matcher) {
    if (fNumMatchers + 1 > fMatchers.getCapacity()) {
        fMatchers.resize(fNumMatchers * 2, fNumMatchers);
        if (fComputeLeads) {
            // The two arrays should grow in tandem:
            U_ASSERT(fNumMatchers >= fLeads.getCapacity());
            fLeads.resize(fNumMatchers * 2, fNumMatchers);
        }
    }

    fMatchers[fNumMatchers] = matcher;

    if (fComputeLeads) {
        fLeads[fNumMatchers] = matcher->getLeadCodePoints();
    }

    fNumMatchers++;
}

void NumberParserImpl::freeze() {
    fFrozen = true;
}

//void
//NumberParserImpl::parse(const UnicodeString& input, int32_t start, bool greedy, ParsedNumber& result,
//                        UErrorCode& status) const {
//    U_ASSERT(frozen);
//    // TODO: Check start >= 0 and start < input.length()
//    StringSegment segment(utils::maybeFold(input, parseFlags));
//    segment.adjustOffset(start);
//    if (greedy) {
//        parseGreedyRecursive(segment, result);
//    } else {
//        parseLongestRecursive(segment, result);
//    }
//    for (NumberParseMatcher matcher : matchers) {
//        matcher.postProcess(result);
//    }
//}


#endif /* #if !UCONFIG_NO_FORMATTING */
