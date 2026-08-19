// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __SEGMENTER_SEGMENT_RANGE_H__
#define __SEGMENTER_SEGMENT_RANGE_H__

/**
 * \file
 * \brief C++ API: A range class for Segment.
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
#include "unicode/segmenter_segment_iter.h"

#ifndef U_HIDE_DRAFT_API

U_NAMESPACE_BEGIN

namespace segmenter {

class U_COMMON_API_CLASS SegmentRange : public UObject {
public:
    ~SegmentRange() override;
    virtual SegmentIterator begin() = 0;
    virtual SegmentIterator end() = 0;
protected:
    SegmentRange();
};

}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // __SEGMENTER_SEGMENT_RANGE_H__