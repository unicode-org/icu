// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __SEGMENTER_SEGMENT_ITER_H__
#define __SEGMENTER_SEGMENT_ITER_H__

/**
 * \file
 * \brief C++ API: An iterator class for Segment.
 */

#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API

#if UCONFIG_NO_BREAK_ITERATION

U_NAMESPACE_BEGIN

/*
 * Allow the declaration of APIs with pointers to BreakIterator
 * and Segmenter
 * even when break iteration is removed from the build.
 */
class BreakIterator;
class Segmenter;

U_NAMESPACE_END

#else

#include "unicode/brkiter.h"
#include "unicode/segmenter.h"

#ifndef U_HIDE_DRAFT_API

U_NAMESPACE_BEGIN

namespace segmenter {


// ---------------------------------------------
// enums
// ---------------------------------------------

// ---------------------------------------------
// SegmentIterator
// ---------------------------------------------

class U_COMMON_API_CLASS SegmentIterator {
public:
    SegmentIterator() = delete;
    SegmentIterator(std::unique_ptr<BreakIterator> breakIter, int32_t startIdx, std::u16string_view source);

    bool operator==(const SegmentIterator &other) const;
    bool operator!=(const SegmentIterator &other) const;
    Segment operator*() const;
    SegmentIterator & operator++();
    SegmentIterator & operator--();
private:
    enum IterLogicalState {
        // Represents the state in which we know both the start
        // and limit of the segment that we are logically "at"
        // (more precisely: the iterator is logically at the start
        // index), and the actual index of the breakIter_ is at
        // the start
        //
        // Invariant:
        // startIdx_ == breakIter_->current()
        // we have computed startIdx_ and limitIdx_
        BACKWARDS = -1,
        // Represents the state in which we only know the
        // current index (both the logical index of the iterator
        // and the actual index of the breakIter_ are the same),
        // meaning that we don't know the full info needed yet
        // for operator* to return its Segment.
        //
        // Invariant:
        // startIdx_ == limitIdx_ == breakIter_->current()
        //
        // Note: you can think of the logical position of the iterator
        // as tracking the startIdx_.
        // This, in turn, implies that operator== only should & only needs to
        // compare startIdx_.
        // Ex: an iterator in the HERE state with the iterator initialized
        // to index 0 will have
        // startIdx_ == limitIdx_ == breakIter_->current() == 0,
        // while the last segment when iterating backwards will have
        // start = 0, limit = x when the state = BACKWARDS, for some x
        // (length of first segment is x-1),
        // yet they both represent the logical position of the iterator being
        // at the start of the text.
        HERE = 0,
        // Represents the state in which we know both the start
        // and limit of the segment that we are logically "at"
        // (more precisely: the iterator is logically at the start
        // index), and the actual index of the breakIter_ is at
        // the limit
        //
        // Invariant:
        // limitIdx_ == breakIter_->current()
        // we have computed startIdx_ and limitIdx_
        FORWARDS = 1,
    };

    mutable int32_t startIdx_;
    mutable int32_t limitIdx_;
    mutable int32_t ruleStatus_;
    std::u16string_view source_;
    std::unique_ptr<BreakIterator> breakIter_;
    mutable IterLogicalState iterLogicState_;

    bool isCurrentSegmentValid() const;
};

}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // __SEGMENTER_SEGMENT_ITER_H__