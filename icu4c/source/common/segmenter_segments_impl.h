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

#include "unicode/brkiter.h"
#include "unicode/segmenter.h"
#include "unicode/segmenter_rulebased.h"
#include "unicode/segmenter_segmentiter.h"
#include "unicode/uobject.h"
#include "unicode/unistr.h"

#include <memory>

#ifndef U_HIDE_DRAFT_API

U_NAMESPACE_BEGIN

namespace segmenter {

class U_COMMON_API_CLASS SegmentsImpl : public Segments {
public:
    bool isBoundary(int32_t i) override;

    Segment segmentAt(int32_t i, UErrorCode &errorCode) override;

    SegmentIterator segmentsFrom(int32_t i) override;

private:

    friend class RuleBasedSegmenter;
    friend class LocalizedSegmenter;

    SegmentsImpl(const BreakIterator & breakIter, std::u16string_view source);

    UnicodeString source_;
    std::unique_ptr<BreakIterator> breakIterPrototype_;

};

}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // __SEGMENTER_SEGMENTS_IMPL_H__