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
:start_(-1),
limit_(-1),
ruleStatus_(-1),
source_(u"")
{}

Segment::Segment(int32_t start, int32_t limit, int32_t ruleStatus, std::u16string_view source)
:start_(start),
limit_(limit),
ruleStatus_(ruleStatus),
source_(source)
{
}

int32_t Segment::getStart() {
    return start_;
}

int32_t Segment::getLimit() {
    return limit_;
}

int32_t Segment::getRuleStatus() {
    return ruleStatus_;
}

std::u16string_view Segment::getSource() {
    return source_;
}

Segment Segment::emptySegment() {
    Segment s;
    return s;
}

}  // namespace segmenter

U_NAMESPACE_END