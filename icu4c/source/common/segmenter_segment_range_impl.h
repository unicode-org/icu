// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __SEGMENTER_SEGMENTS_IMPL_H__
#define __SEGMENTER_SEGMENTS_IMPL_H__


/**
 * \file
 * \brief C++ API: Common Segments impl class that can be used by all specific types of Segments impls.
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

#include "unicode/segmenter_segment_iter.h"
#include "unicode/segmenter_segment_range.h"
#include "segmenter_segments_impl.h"

#include <memory>

#ifndef U_HIDE_DRAFT_API

U_NAMESPACE_BEGIN

namespace segmenter {

class U_COMMON_API_CLASS SegmentRangeImpl : public SegmentRange {
public:
    SegmentIterator begin() override;
    SegmentIterator end() override;
private:
    friend class SegmentsImpl;

    SegmentRangeImpl(const BreakIterator & breakIter);

    std::unique_ptr<BreakIterator> breakIterPrototype_;
    std::u16string_view source_;
};


}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // __SEGMENTER_SEGMENTS_IMPL_H__