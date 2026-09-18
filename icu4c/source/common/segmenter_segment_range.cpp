// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/segmenter_segment_range.h"

U_NAMESPACE_BEGIN

namespace segmenter {

SegmentRange::~SegmentRange() {}

SegmentRange::SegmentRange(const BreakIterator & breakIter, std::u16string_view source)
: breakIterPrototype_(std::unique_ptr<BreakIterator>(breakIter.clone())),
source_(source)
{
}

SegmentRange::SegmentRange(SegmentRange && other)
: breakIterPrototype_(std::move(other.breakIterPrototype_)),
source_(other.source_)
{
}

SegmentIterator SegmentRange::begin() {
    std::unique_ptr<BreakIterator> breakIter = std::unique_ptr<BreakIterator>(breakIterPrototype_->clone());
    SegmentIterator segIter(std::move(breakIter), 0, source_);
    return segIter;
}

SegmentIterator SegmentRange::end() {
    std::unique_ptr<BreakIterator> breakIter = std::unique_ptr<BreakIterator>(breakIterPrototype_->clone());
    SegmentIterator segIter(std::move(breakIter), source_.length(), source_);
    return segIter;
}

}  // namespace segmenter

U_NAMESPACE_END