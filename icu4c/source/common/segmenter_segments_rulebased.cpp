// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#include "unicode/brkiter.h"
#include "unicode/segmenter_segments_rulebased.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

namespace segmenter {

RuleBasedSegments::RuleBasedSegments(const BreakIterator & breakIter, std::u16string_view source)
    : breakIterPrototype_(std::unique_ptr<BreakIterator>(breakIter.clone())),
    source_(std::move(UnicodeString::readOnlyAlias(source)))
{
    breakIterPrototype_->setText(source_);
}

bool RuleBasedSegments::isBoundary(int32_t i) {
    return breakIterPrototype_->clone()->isBoundary(i);
}

}  // namespace segmenter

U_NAMESPACE_END