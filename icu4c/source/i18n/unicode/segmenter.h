// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __SEGEMENTER_H__
#define __SEGEMENTER_H__

/**
 * \file
 * \brief C++ API: Segmenter base class.
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

#include "unicode/uobject.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"

#ifndef U_HIDE_DRAFT_API

U_NAMESPACE_BEGIN

namespace segmenter {

class Segments;
class SegmentsUtf8;
class Segment;

class U_COMMON_API_CLASS Segmenter : public UObject {
public:
    ~Segmenter() override;
    // TODO: discuss if we want to take input type of UnicodeString or std::u16string_view
    // Note: std::u16string_view is mentioned in the design doc, FWIW
    // Note: whether taking a pointer or a 
    virtual Segments segment(const std::u16string_view &s) = 0;
    // Note: this API also is mentioned in the design doc
    virtual SegmentsUtf8 segment(const char &s) = 0;

};

class U_COMMON_API_CLASS Segments : public UObject {
    virtual bool isBoundary(int32_t offset) = 0;
};

// Note: this class is mentioned in the design doc as describing
// an iterator of `char*`, but no details, such as whether there should be
// a templated class based on the encoding form / code unit size
class U_COMMON_API_CLASS SegmentsUtf8 {
    virtual bool isBoundary(int32_t offset) = 0;

    // all other APIs the same as Segments
    //
    // (if the class is templated, then that automatically becomes true)
};

class U_COMMON_API_CLASS Segment {
public:
    const int32_t start;
    const int32_t limit;
    const int32_t ruleStatus;
};

class U_COMMON_API_CLASS SegmentIterator {
public:
    SegmentIterator(const SegmentIterator &other) = default;
    bool operator==(const SegmentIterator &other) const;
    bool operator!=(const SegmentIterator &other) const;
    Segment operator*() const;
    SegmentIterator &operator++();
};

}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // __SEGEMENTER_H__