// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/brkiter.h"
#include "unicode/parseerr.h"
#include "unicode/rbbi.h"
#include "unicode/segmenter_rulebased.h"
#include "unicode/segmenter_segments_rulebased.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

namespace segmenter {

// ---------------------------------------------

RuleBasedSegmenter::RuleBasedSegmenter():
    breakIter_(nullptr)
{
    
}

RuleBasedSegmenter::RuleBasedSegmenter(std::unique_ptr<BreakIterator> && other) noexcept 
    : breakIter_(std::move(other))
{
    
}

RuleBasedSegmenter::RuleBasedSegmenter(RuleBasedSegmenter&& other) noexcept
    : breakIter_(std::move(other.breakIter_))
{

}

RuleBasedSegmenter& RuleBasedSegmenter::operator=(RuleBasedSegmenter&& other) noexcept {
    if (this != &other) {
        breakIter_ = std::move(other.breakIter_);
    }

    return *this;
}

RuleBasedSegmenter::~RuleBasedSegmenter() {
    
    breakIter_ = nullptr;
}

std::unique_ptr<Segments> RuleBasedSegmenter::segment(std::u16string_view s, UErrorCode &errorCode) {
    RuleBasedSegments segments((*breakIter_), s);
    std::unique_ptr<RuleBasedSegments> segmentsPtr = std::make_unique<RuleBasedSegments>(std::move(segments));
    return segmentsPtr;
}

// ---------------------------------------------

RuleBasedSegmenterBuilder::RuleBasedSegmenterBuilder() :
    breakIter_(nullptr),
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
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return makeEmptySegmenter();
    }

    UParseError parseError;

    UnicodeString rulesUniStr(rules_);

    breakIter_ = std::make_unique<icu::RuleBasedBreakIterator>(rulesUniStr, parseError, errorCode_);

    if (U_FAILURE(errorCode_)) {
        errorCode = errorCode_;
        return makeEmptySegmenter();
    }

    RuleBasedSegmenter rbSegmenter(std::move(breakIter_));

    return rbSegmenter;
}

// ---------------------------------------------

}  // namespace segmenter

U_NAMESPACE_END