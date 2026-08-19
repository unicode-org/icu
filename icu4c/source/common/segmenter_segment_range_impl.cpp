// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#include "unicode/brkiter.h"
#include "segmenter_segment_range_impl.h"

U_NAMESPACE_BEGIN

namespace segmenter {

SegmentRangeImpl::SegmentRangeImpl(const BreakIterator & breakIter)
: breakIterPrototype_(std::unique_ptr<BreakIterator>(breakIter.clone()))
{
}

SegmentIterator SegmentRangeImpl::begin() {
    std::unique_ptr<BreakIterator> breakIter = std::unique_ptr<BreakIterator>(breakIterPrototype_->clone());
    SegmentIterator segIter(std::move(breakIter), 0, source_);
    return segIter;
}

SegmentIterator SegmentRangeImpl::end() {
    std::unique_ptr<BreakIterator> breakIter = std::unique_ptr<BreakIterator>(breakIterPrototype_->clone());
    SegmentIterator segIter(std::move(breakIter), source_.length(), source_);
    return segIter;
}

}  // namespace segmenter

U_NAMESPACE_END