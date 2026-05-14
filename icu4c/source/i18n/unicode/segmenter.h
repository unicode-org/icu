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
class Segment;

class U_COMMON_API_CLASS Segmenter : public UObject {
public:
    ~Segmenter() override;
    // TODO: discuss if we want to take input type of UnicodeString or std::u16string_view
    virtual Segments segment(const std::u16string_view &s) = 0;

};

class U_COMMON_API_CLASS Segments {
    virtual bool isBoundary(int32_t offset) = 0;
};

class U_COMMON_API_CLASS Segment {
public:
    const int32_t start;
    const int32_t limit;
    const int32_t ruleStatus;
};

}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // __SEGEMENTER_H__