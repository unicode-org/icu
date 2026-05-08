// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __SEGEMENTER_H__
#define __SEGEMENTER_H__

#include "unicode/utypes.h"
#include "unicode/unistr.h"

#if U_SHOW_CPLUSPLUS_API

#ifndef U_HIDE_INTERNAL_API

namespace segmenter {  // icu::segmenter

class Segmenter {
public:
    virtual Segments segment(const UnicodeString &s) = 0;
};

class Segments {
    virtual bool isBoundary(int32_t offset) = 0;
};

}  // namespace segmenter

#endif // U_HIDE_INTERNAL_API

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // __SEGEMENTER_H__