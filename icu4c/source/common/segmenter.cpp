// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/brkiter.h"
#include "unicode/segmenter.h"
#include "unicode/segmenter_segmentiter.h"

U_NAMESPACE_BEGIN

namespace segmenter {

//----------
// Segment
//----------

Segment::~Segment() {}

Segment::Segment()
:start_(BreakIterator::DONE),
limit_(BreakIterator::DONE),
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

std::u16string_view Segment::getSubstr() {
    return source_.substr(start_, (limit_ - start_));
}

Segment Segment::emptySegment() {
    Segment s;
    return s;
}

//----------
// Segments
//----------

SegmentIterator Segments::segments() {
    return segmentsFrom(0);
}

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

}  // namespace segmenter

U_NAMESPACE_END