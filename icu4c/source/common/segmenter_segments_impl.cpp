// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#include "unicode/brkiter.h"
#include "unicode/segmenter_segments_impl.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

namespace segmenter {

SegmentsImpl::SegmentsImpl(const BreakIterator & breakIter, std::u16string_view source)
    : breakIterPrototype_(std::unique_ptr<BreakIterator>(breakIter.clone())),
    source_(UnicodeString::readOnlyAlias(source))
{
    breakIterPrototype_->setText(source_);
}

bool SegmentsImpl::isBoundary(int32_t i) {
    return breakIterPrototype_->clone()->isBoundary(i);
}

}  // namespace segmenter

U_NAMESPACE_END