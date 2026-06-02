// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/segmenter_rulebased.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

namespace segmenter {

RuleBasedSegmenter::RuleBasedSegmenter() {}

RuleBasedSegmenter::~RuleBasedSegmenter() {}

RuleBasedSegmenterBuilder::RuleBasedSegmenterBuilder() :
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

    return makeEmptySegmenter();
}

}  // namespace segmenter

U_NAMESPACE_END