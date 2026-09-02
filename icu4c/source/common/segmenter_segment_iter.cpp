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
iterLogicState_(IterLogicalState::ITER_AT_LOGICAL_POS)
{
}

bool SegmentIterator::operator==(const SegmentIterator &other) const {
    return startIdx_ == other.startIdx_
        && limitIdx_ == other.limitIdx_
        // TODO: consider replacing equality check on ruleStatus_ with equality check on breakIter_->getRuleString() or something
        // more precise like that
        && ruleStatus_ == other.ruleStatus_
        && source_ == other.source_;
}

bool SegmentIterator::operator!=(const SegmentIterator &other) const {
    return !(*this == other);
}

Segment SegmentIterator::operator*() const {    
    if (iterLogicState_ == IterLogicalState::ITER_AT_LOGICAL_POS) {
        // Note: BreakIterator.isBoundary() is a stateful operation. It resets the position in the
        // BreakIterator, and thus doesn't just return whether the input is on a boundary.
        bool startIdxIsBoundary = breakIter_->isBoundary(startIdx_);

        if (startIdxIsBoundary) {
            if (startIdx_ == source_.length()) {
                limitIdx_ = startIdx_;
                ruleStatus_ = UBRK_DONE;
            } else {
                limitIdx_ = breakIter_->next();
                ruleStatus_ = breakIter_->getRuleStatus();
            }
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
        iterLogicState_ = IterLogicalState::ITER_AFTER_LOGICAL_POS;
    }

    // if (isCurrentSegmentValid()) {
        Segment s(startIdx_, limitIdx_, ruleStatus_, source_);
        return s;
    // } else {
    //     return Segment::emptySegment();
    // }
}

SegmentIterator & SegmentIterator::operator++() {
    if (iterLogicState_ == IterLogicalState::ITER_AFTER_LOGICAL_POS) {
        iterLogicState_ = IterLogicalState::ITER_AT_LOGICAL_POS;
    } else if (iterLogicState_ == IterLogicalState::ITER_AT_LOGICAL_POS) {
        if (iterLogicState_ == IterLogicalState::ITER_AT_LOGICAL_POS) {
            // Note: BreakIterator.isBoundary() is a stateful operation. It resets the position in the
            // BreakIterator, and thus doesn't just return whether the input is on a boundary.
            bool startIdxIsBoundary = breakIter_->isBoundary(startIdx_);

            if (startIdxIsBoundary) {
                if (startIdx_ == source_.length()) {
                    limitIdx_ = startIdx_;
                    ruleStatus_ = UBRK_DONE;
                } else {
                    limitIdx_ = breakIter_->next();
                    ruleStatus_ = breakIter_->getRuleStatus();
                }
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
            iterLogicState_ = IterLogicalState::ITER_AFTER_LOGICAL_POS;
        }
    } else /* iterLogicState == ITER_BEFORE_LOGICAL_POS */ {
        // operator--() called decAndRead() so we know how far to skip.
        // In other words, reset the breakIter_ position to be the end
        // of our currently cached limit so that it is ready to move
        // forward if/when we call operator++() subsequently.
        breakIter_->isBoundary(limitIdx_);
        iterLogicState_ = IterLogicalState::ITER_AT_LOGICAL_POS;
    }

    return *this;
}

SegmentIterator & SegmentIterator::operator--() {
    if (iterLogicState_ == IterLogicalState::ITER_AFTER_LOGICAL_POS) {
        // operator*() called readAndInc() so p_ is ahead of the logical position.
        breakIter_->isBoundary(startIdx_);
    }

    if (startIdx_ <= 0 || startIdx_ == BreakIterator::DONE) {
        // at the beginning, return sentinel as a 0-length empty segment
        startIdx_ = 0;
        limitIdx_ = 0;
        ruleStatus_ = UBRK_DONE;
    } else {
        // move backwards
        limitIdx_ = breakIter_->current();
        ruleStatus_ = breakIter_->getRuleStatus();
        startIdx_ = breakIter_->previous();        
    }

    iterLogicState_ = IterLogicalState::ITER_BEFORE_LOGICAL_POS;

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