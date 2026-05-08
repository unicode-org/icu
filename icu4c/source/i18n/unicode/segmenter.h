// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if U_SHOW_CPLUSPLUS_API

#ifndef U_HIDE_DRAFT_API

// TODO: rewrite file to match brkiter.h
#ifndef __SEGEMENTER_H__
#define __SEGEMENTER_H__

#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

namespace segmenter {  // icu::segmenter

class U_COMMON_API_CLASS Segmenter : public UObject {
public:
    ~Segmenter() override;
    // TODO: discuss if we want to take input type of UnicodeString or std::u16string_view
    virtual Segments segment(const UnicodeString &s) = 0;

};

class Segments {
    virtual bool isBoundary(int32_t offset) = 0;
};

class Segment {

};

}  // namespace segmenter

U_NAMESPACE_END

#endif // __SEGEMENTER_H__

#endif // U_HIDE_DRAFT_API

#endif /* U_SHOW_CPLUSPLUS_API */

#endif /* #if !UCONFIG_NO_FORMATTING */