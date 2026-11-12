// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/brkiter.h"
#include "unicode/segmenter.h"
#include "unicode/segmenter_segment_iter.h"

U_NAMESPACE_BEGIN

namespace segmenter {

// ---------------------------------------------
// Segments
// ---------------------------------------------

// ---------------------------------------------
// Segmenter
// ---------------------------------------------

Segmenter::Segmenter() {}

Segmenter::~Segmenter() {}

std::unique_ptr<Segments> Segmenter::segment(std::u16string_view /*s*/, UErrorCode &errorCode) {
    if (U_SUCCESS(errorCode)) {
        errorCode = U_UNSUPPORTED_ERROR;
    }
    return nullptr;
}

std::unique_ptr<SegmentsUTF8> Segmenter::segment(StringPiece /*s*/, UErrorCode &errorCode) {
    if (U_SUCCESS(errorCode)) {
        errorCode = U_UNSUPPORTED_ERROR;
    }

    return nullptr;
}

}  // namespace segmenter

U_NAMESPACE_END