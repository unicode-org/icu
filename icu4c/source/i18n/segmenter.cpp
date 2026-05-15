// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "segmenter.h"

U_NAMESPACE_BEGIN

namespace segmenter {

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