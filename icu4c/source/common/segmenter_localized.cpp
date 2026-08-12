// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/brkiter.h"
#include "unicode/parseerr.h"
#include "unicode/rbbi.h"
#include "unicode/segmenter_localized.h"
#include "unicode/segmenter_segments_impl.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

namespace segmenter {

// ---------------------------------------------

LocalizedSegmenter::LocalizedSegmenter():
    breakIterPrototype_(nullptr)
{
    
}

LocalizedSegmenter::LocalizedSegmenter(Locale locale, SegmentationType segmentationType, UErrorCode &errorCode)
{
    switch (segmentationType) {
        case SegmentationType::LINE:
            breakIterPrototype_.reset(BreakIterator::createLineInstance(locale, errorCode));
            break;
        case SegmentationType::SENTENCE:
            breakIterPrototype_.reset(BreakIterator::createSentenceInstance(locale, errorCode));
            break;
        case SegmentationType::WORD:
            breakIterPrototype_.reset(BreakIterator::createWordInstance(locale, errorCode));
            break;
        case SegmentationType::GRAPHEME_CLUSTER:
        case SegmentationType::UNKNOWN:
            breakIterPrototype_.reset(BreakIterator::createCharacterInstance(locale, errorCode));
            break;
    }
}

LocalizedSegmenter::LocalizedSegmenter(LocalizedSegmenter&& other) noexcept
    : breakIterPrototype_(std::move(other.breakIterPrototype_))
{

}

LocalizedSegmenter& LocalizedSegmenter::operator=(LocalizedSegmenter&& other) noexcept {
    if (this != &other) {
        breakIterPrototype_ = std::move(other.breakIterPrototype_);
    }

    return *this;
}

LocalizedSegmenter::~LocalizedSegmenter() {
    
    breakIterPrototype_ = nullptr;
}

std::unique_ptr<Segments> LocalizedSegmenter::segment(std::u16string_view s, UErrorCode & /*errorCode*/) {
    SegmentsImpl segments((*breakIterPrototype_), s);
    std::unique_ptr<SegmentsImpl> segmentsPtr = std::make_unique<SegmentsImpl>(std::move(segments));
    return segmentsPtr;
}

// ---------------------------------------------

LocalizedSegmenterBuilder::LocalizedSegmenterBuilder() :
    locale_(Locale::getRoot()),
    segmentationType_(SegmentationType::UNKNOWN),
    errorCode_(U_ZERO_ERROR)
{

}

LocalizedSegmenterBuilder::~LocalizedSegmenterBuilder()
{}

LocalizedSegmenterBuilder& LocalizedSegmenterBuilder::setLocale(Locale locale) {
    locale_ = locale;

    return *this;
}

LocalizedSegmenterBuilder& LocalizedSegmenterBuilder::setSegmentationType(SegmentationType segmentationType) {
    segmentationType_ = segmentationType;

    return *this;
}

LocalizedSegmenter LocalizedSegmenterBuilder::makeEmptySegmenter() {
    icu::segmenter::LocalizedSegmenter empty;
    return empty;
}

LocalizedSegmenter LocalizedSegmenterBuilder::build(UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return makeEmptySegmenter();
    }
    if (U_FAILURE(errorCode_)) {
        errorCode = errorCode_;
        return makeEmptySegmenter();
    }
    if (segmentationType_ == SegmentationType::UNKNOWN) {
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return makeEmptySegmenter();
    }

    LocalizedSegmenter segmenter(locale_, segmentationType_, errorCode_);

    if (U_FAILURE(errorCode_)) {
        errorCode = errorCode_;
        return makeEmptySegmenter();
    }

    return segmenter;
}

// ---------------------------------------------

}  // namespace segmenter

U_NAMESPACE_END