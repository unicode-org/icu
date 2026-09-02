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
        // breakIter_->current() < startIdx_  => 
        //   actual position is before logical position <=>
        //   breakIter position is before current segment
        ITER_BEFORE_LOGICAL_POS = -1,
        // breakIter_->current() == startIdx_  => 
        //   actual position is same logical position <=>
        //   breakIter position on start boundary of current segment
        ITER_AT_LOGICAL_POS = 0,
        // breakIter_->current() > startIdx_  => 
        //   actual position is after logical position <=>
        //   breakIter position is after current segment
        ITER_AFTER_LOGICAL_POS = 1,
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