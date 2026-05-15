// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __SEGEMENTER_RULEBASED_H__
#define __SEGEMENTER_RULEBASED_H__

#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/segmenter.h"

#ifndef U_HIDE_DRAFT_API

U_NAMESPACE_BEGIN

namespace segmenter {

class U_COMMON_API_CLASS RuleBasedSegmenter : public Segmenter {

public:
    ~RuleBasedSegmenter() override;

    // Segments segment(const std::u16string_view &s) override;
    
};

}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif  // __SEGEMENTER_RULEBASED_H__
