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

enum CurrentSegmentValidType {
    CURRENT_SEGMENT_UNKNOWN = 0,
    CURRENT_SEGMENT_VALID = 1,
    CURRENT_SEGMENT_INVALID = 2,
};

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
private:
    int32_t startIdx_;
    int32_t limitIdx_;
    int32_t ruleStatus_;
    std::u16string_view source_;
    std::unique_ptr<BreakIterator> breakIter_;
    CurrentSegmentValidType prevSegValid_;
    CurrentSegmentValidType currSegValid_;
    CurrentSegmentValidType nextSegValid_;

    bool isCurrentSegmentValid() const;
};

}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // __SEGMENTER_SEGMENT_ITER_H__