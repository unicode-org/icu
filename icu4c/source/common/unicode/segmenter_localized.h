// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __SEGEMENTER_LOCALIZED_H__
#define __SEGEMENTER_LOCALIZED_H__

/**
 * \file
 * \brief C++ API: Localized (locale based) Segmenter impl class.
 */

#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/brkiter.h"
#include "unicode/locid.h"
#include "unicode/parseerr.h"
#include "unicode/segmenter.h"

#ifndef U_HIDE_DRAFT_API

U_NAMESPACE_BEGIN

namespace segmenter {

// ---------------------------------------------
// forward declarations
// ---------------------------------------------

class U_COMMON_API_CLASS LocalizedSegmenterBuilder;

// ---------------------------------------------
// enums
// ---------------------------------------------

enum SegmentationType {
    // Q: is it okay to include UNKNOWN? not present in Java version
    // because enums could be initialized to null and checked by
    // builder during validation phase.
    UNKNOWN,
    GRAPHEME_CLUSTER,
    WORD,
    LINE,
    SENTENCE,
};

// ---------------------------------------------
// LocalizedSegmenter
// ---------------------------------------------

class U_COMMON_API_CLASS LocalizedSegmenter : public Segmenter {

public:
    ~LocalizedSegmenter() override;

    LocalizedSegmenter(LocalizedSegmenter&& other) noexcept;

    LocalizedSegmenter& operator=(LocalizedSegmenter&& other) noexcept;

    std::unique_ptr<Segments> segment(std::u16string_view s, UErrorCode & /*errorCode*/) override;

private:
    friend class LocalizedSegmenterBuilder;

    LocalizedSegmenter();

    // move constructor to allow builder to build from a configured BreakIterator instance
    LocalizedSegmenter(Locale locale, SegmentationType segmentationType, UErrorCode &errorCode);

    std::unique_ptr<BreakIterator> breakIterPrototype_;
};

// ---------------------------------------------
// LocalizedSegmenterBuilder
// ---------------------------------------------

class U_COMMON_API_CLASS LocalizedSegmenterBuilder : public UObject {
public:
    LocalizedSegmenterBuilder();

    virtual ~LocalizedSegmenterBuilder();

    LocalizedSegmenterBuilder& setLocale(Locale locale);

    LocalizedSegmenterBuilder& setSegmentationType(SegmentationType segmentationType);

    LocalizedSegmenter build(UErrorCode& errorCode);

private:
    Locale locale_;

    SegmentationType segmentationType_;

    LocalizedSegmenter makeEmptySegmenter();

    UErrorCode errorCode_;
};


}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif  // __SEGEMENTER_LOCALIZED_H__
