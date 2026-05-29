// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/segmenter_rulebased.h"

U_NAMESPACE_BEGIN

namespace segmenter {

RuleBasedSegmenter::RuleBasedSegmenter() {}

RuleBasedSegmenter::~RuleBasedSegmenter() {}

RuleBasedSegmenterBuilder::RuleBasedSegmenterBuilder() :
    UObject(),
    rules(u"")
{

}

RuleBasedSegmenterBuilder::~RuleBasedSegmenterBuilder() {}

RuleBasedSegmenterBuilder& RuleBasedSegmenterBuilder::setRules(std::u16string_view rules) {
    this->rules = rules;

    return *this;
}

}  // namespace segmenter

U_NAMESPACE_END