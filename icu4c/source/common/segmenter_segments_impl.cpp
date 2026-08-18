// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#include "unicode/brkiter.h"
#include "unicode/unistr.h"

#include "segmenter_segments_impl.h"

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

Segment SegmentsImpl::segmentAt(int32_t i, UErrorCode &errorCode) {
    auto breakIter = breakIterPrototype_->clone();
    int32_t start;
    int32_t limit;
    int32_t ruleStatus;

    if (i < 0 || i >= source_.length()) {
        errorCode = U_INDEX_OUTOFBOUNDS_ERROR;
        return Segment::emptySegment();
    }

    bool isBoundary = breakIter->isBoundary(i);

    if (isBoundary) {
        start = i;
        limit = breakIter->next();
        ruleStatus = breakIter->getRuleStatus();
    } else {
        // BreakIterator.isBoundary(i) will advance forwards to the next boundary if the
        // argument
        // is not a boundary.
        limit = breakIter->current();
        ruleStatus = breakIter->getRuleStatus();
        start = breakIter->previous();
    }

    if (start == BreakIterator::DONE || limit == BreakIterator::DONE) {
        errorCode = U_INTERNAL_PROGRAM_ERROR;
        return Segment::emptySegment();
    }

    Segment s(start, limit, ruleStatus, source_);

    return s;
}


}  // namespace segmenter

U_NAMESPACE_END