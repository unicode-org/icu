// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if U_SHOW_CPLUSPLUS_API

#ifndef U_HIDE_DRAFT_API

#ifndef __SEGEMENTER_H__
#define __SEGEMENTER_H__

#include "unicode/unistr.h"

using namespace icu;

namespace segmenter {  // icu::segmenter

class Segmenter {
public:
    virtual Segments segment(const UnicodeString &s) = 0;
};

class Segments {
    virtual bool isBoundary(int32_t offset) = 0;
};

}  // namespace segmenter

#endif // __SEGEMENTER_H__

#endif // U_HIDE_DRAFT_API

#endif /* U_SHOW_CPLUSPLUS_API */

#endif /* #if !UCONFIG_NO_FORMATTING */