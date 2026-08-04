// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/brkiter.h"
#include "unicode/parseerr.h"
#include "unicode/rbbi.h"
#include "unicode/segmenter_rulebased.h"
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

// ---------------------------------------------

RuleBasedSegmenterBuilder::RuleBasedSegmenterBuilder() :
    breakIter_(nullptr),
    UObject(),
    rules_(u""),
    errorCode_(U_ZERO_ERROR)
{

}

RuleBasedSegmenterBuilder::~RuleBasedSegmenterBuilder()
{}

RuleBasedSegmenterBuilder& RuleBasedSegmenterBuilder::setRules(std::u16string_view rules) {
    this->rules_ = rules;

    return *this;
}

RuleBasedSegmenter RuleBasedSegmenterBuilder::makeEmptySegmenter() {
    icu::segmenter::RuleBasedSegmenter empty;
    // Q (elango): this inefficiently incurs a copy, right?
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

    // TODO: implement builder validation logic here & remove
    // placeholder return statement

    UParseError parseError;

    UnicodeString rulesUniStr(this->rules_);

    this->breakIter_ = std::make_unique<icu::RuleBasedBreakIterator>(rulesUniStr, parseError, this->errorCode_);

    RuleBasedSegmenter rbSegmenter(std::move(this->breakIter_));

    // Q (elango): this inefficiently incurs a copy, right?

    return rbSegmenter;
}

// ---------------------------------------------

}  // namespace segmenter

U_NAMESPACE_END