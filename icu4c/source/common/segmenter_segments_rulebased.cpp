// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/brkiter.h"
#include "unicode/segmenter_segments_rulebased.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

namespace segmenter {

bool RuleBasedSegments::isBoundary(int32_t i) {
    return breakIterProrotype_->clone()->isBoundary(i);
}

}  // namespace segmenter

U_NAMESPACE_END