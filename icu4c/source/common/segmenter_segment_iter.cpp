// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#include "unicode/brkiter.h"
#include "unicode/segmenter_segment_iter.h"


U_NAMESPACE_BEGIN

namespace segmenter {

SegmentIterator::SegmentIterator(std::unique_ptr<BreakIterator> breakIter, int32_t startIdx, std::u16string_view source)
: startIdx_(startIdx),
limitIdx_(startIdx),
source_(source),
breakIter_(std::move(breakIter)),
iterLogicState_(IterLogicalState::HERE)
{
}

bool SegmentIterator::operator==(const SegmentIterator &other) const {
    return startIdx_ == other.startIdx_
        && source_ == other.source_;
}

bool SegmentIterator::operator!=(const SegmentIterator &other) const {
    return !(*this == other);
}

Segment SegmentIterator::operator*() const {    
    if (iterLogicState_ == IterLogicalState::HERE) {

        // Note: BreakIterator.isBoundary() is a stateful operation. It resets the position in the
        // BreakIterator, and thus doesn't just return whether the input is on a boundary.
        bool startIdxIsBoundary = breakIter_->isBoundary(startIdx_);
        
        if (startIdxIsBoundary) {
            limitIdx_ = breakIter_->next();
            ruleStatus_ = breakIter_->getRuleStatus();
        } else {
            // if startIdx wasn't on a boundary, then the call to isBoundary will have advanced
            // it to the next boundary, which is the limit of the segment
            limitIdx_ = breakIter_->current();
            ruleStatus_ = breakIter_->getRuleStatus();
            // go back to get the start of the segment
            startIdx_ = breakIter_->previous();
            // reset current position of BreakIterator to be limit of segment
            breakIter_->isBoundary(limitIdx_);
        }
        iterLogicState_ = IterLogicalState::FORWARDS;
    }

    Segment s(startIdx_, limitIdx_, ruleStatus_, source_);
    return s;
}

SegmentIterator & SegmentIterator::operator++() {
    if (iterLogicState_ == IterLogicalState::FORWARDS) {
        startIdx_ = limitIdx_;
        iterLogicState_ = IterLogicalState::HERE;
    } else if (iterLogicState_ == IterLogicalState::HERE) {
        // Note: BreakIterator.isBoundary() is a stateful operation. It resets the position in the
        // BreakIterator, and thus doesn't just return whether the input is on a boundary.
        bool startIdxIsBoundary = breakIter_->isBoundary(startIdx_);

        if (startIdxIsBoundary) {
            limitIdx_ = breakIter_->next();
            ruleStatus_ = breakIter_->getRuleStatus();
            startIdx_ = limitIdx_;
        }
    } else /* iterLogicState_ == IterLogicalState::BACKWARDS */ {
        startIdx_ = limitIdx_;
        // TODO: ask Robin if breakIter_->isBoundary(i) or breakIter_->next()
        // is faster, and whether that affects the ability to compute rule status
        breakIter_->isBoundary(limitIdx_);
        ruleStatus_ = breakIter_->getRuleStatus();
        iterLogicState_ = IterLogicalState::HERE;
    }

    return *this;
}

SegmentIterator & SegmentIterator::operator--() {
    if (iterLogicState_ == IterLogicalState::FORWARDS) {
        breakIter_->isBoundary(startIdx_);
        iterLogicState_ = IterLogicalState::HERE;
    } else {
        ruleStatus_ = breakIter_->getRuleStatus();
        limitIdx_ = startIdx_;
        startIdx_ = breakIter_->preceding(startIdx_); 
        iterLogicState_ = IterLogicalState::BACKWARDS;
    }

    return *this;
}

bool SegmentIterator::isCurrentSegmentValid() const {
    return (
        startIdx_ != BreakIterator::DONE
            && limitIdx_ != BreakIterator::DONE
    );
}

}  // namespace segmenter

U_NAMESPACE_END