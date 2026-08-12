// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/segmenter.h"

U_NAMESPACE_BEGIN

namespace segmenter {

//----------
// Segmenter
//----------

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

//----------
// Segment
//----------

Segment::~Segment() {}

Segment::Segment()
:start(-1),
limit(-1),
ruleStatus(-1),
source(u"")
{}

Segment::Segment(int32_t start, int32_t limit, int32_t ruleStatus, std::u16string_view source)
:start(start),
limit(limit),
ruleStatus(ruleStatus),
source(source)
{
}


Segment Segment::emptySegment() {
    Segment s;
    return s;
}

}  // namespace segmenter

U_NAMESPACE_END