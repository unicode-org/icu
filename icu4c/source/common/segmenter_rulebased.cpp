// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#include "unicode/brkiter.h"
#include "unicode/parseerr.h"
#include "unicode/rbbi.h"
#include "unicode/segmenter_rulebased.h"

#include "segmenter_segments_impl.h"

U_NAMESPACE_BEGIN

namespace segmenter {

// ---------------------------------------------

RuleBasedSegmenter::RuleBasedSegmenter():
    breakIterPrototype_(nullptr)
{
    
}

RuleBasedSegmenter::RuleBasedSegmenter(std::unique_ptr<BreakIterator> && other) noexcept 
    : breakIterPrototype_(std::move(other))
{
    
}

RuleBasedSegmenter::RuleBasedSegmenter(RuleBasedSegmenter&& other) noexcept
    : breakIterPrototype_(std::move(other.breakIterPrototype_))
{

}

RuleBasedSegmenter& RuleBasedSegmenter::operator=(RuleBasedSegmenter&& other) noexcept {
    if (this != &other) {
        breakIterPrototype_ = std::move(other.breakIterPrototype_);
    }

    return *this;
}

RuleBasedSegmenter::~RuleBasedSegmenter() {
    
    breakIterPrototype_ = nullptr;
}

std::unique_ptr<Segments> RuleBasedSegmenter::segment(std::u16string_view s, UErrorCode & /*errorCode*/) {
    SegmentsImpl segments((*breakIterPrototype_), s);
    std::unique_ptr<SegmentsImpl> segmentsPtr = std::make_unique<SegmentsImpl>(std::move(segments));
    return segmentsPtr;
}

// ---------------------------------------------

RuleBasedSegmenterBuilder::RuleBasedSegmenterBuilder() :
    rules_(u""),
    errorCode_(U_ZERO_ERROR)
{

}

RuleBasedSegmenterBuilder::~RuleBasedSegmenterBuilder()
{}

RuleBasedSegmenterBuilder& RuleBasedSegmenterBuilder::setRules(std::u16string_view rules) {
    rules_ = rules;

    return *this;
}

RuleBasedSegmenter RuleBasedSegmenterBuilder::makeEmptySegmenter() {
    icu::segmenter::RuleBasedSegmenter empty;
    return empty;
}

RuleBasedSegmenter RuleBasedSegmenterBuilder::build(UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return makeEmptySegmenter();
    }
    if (U_FAILURE(errorCode_)) {
        errorCode = errorCode_;
        return makeEmptySegmenter();
    }
    if (rules_.empty()) {
        errorCode = U_BRK_RULE_SYNTAX;
        return makeEmptySegmenter();
    }

    UParseError parseError;

    UnicodeString rulesUniStr(rules_);

    auto breakIter = std::make_unique<RuleBasedBreakIterator>(rulesUniStr, parseError, errorCode_);

    if (U_FAILURE(errorCode_)) {
        errorCode = errorCode_;
        return makeEmptySegmenter();
    }

    RuleBasedSegmenter rbSegmenter(std::move(breakIter));

    return rbSegmenter;
}

// ---------------------------------------------

}  // namespace segmenter

U_NAMESPACE_END