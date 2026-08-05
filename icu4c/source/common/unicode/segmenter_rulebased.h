// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef __SEGEMENTER_RULEBASED_H__
#define __SEGEMENTER_RULEBASED_H__

#include "unicode/utypes.h"

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/brkiter.h"
#include "unicode/parseerr.h"
#include "unicode/segmenter.h"

#ifndef U_HIDE_DRAFT_API

U_NAMESPACE_BEGIN

namespace segmenter {

class U_COMMON_API_CLASS RuleBasedSegmenterBuilder;

// ---------------------------------------------

class U_COMMON_API_CLASS RuleBasedSegmenter : public Segmenter {

public:
    ~RuleBasedSegmenter() override;

    // TODO: implement
    // TODO: create a test
    RuleBasedSegmenter(RuleBasedSegmenter&& other) noexcept;

    // TODO: implement
    // TODO: create a test
    RuleBasedSegmenter& operator=(RuleBasedSegmenter&& other) noexcept;

private:
    friend RuleBasedSegmenterBuilder;
    
    RuleBasedSegmenter();

    // move constructor to allow builder to build from a configured BreakIterator instance
    RuleBasedSegmenter(std::unique_ptr<BreakIterator> && other) noexcept;

    std::unique_ptr<BreakIterator> breakIter_;
};

// ---------------------------------------------

class U_COMMON_API_CLASS RuleBasedSegmenterBuilder : public UObject {
public:
    RuleBasedSegmenterBuilder();

    virtual ~RuleBasedSegmenterBuilder();

    RuleBasedSegmenterBuilder& setRules(std::u16string_view rules);

    RuleBasedSegmenter build(UErrorCode& errorCode);

private:
    std::unique_ptr<BreakIterator> breakIter_;

    std::u16string_view rules_;

    RuleBasedSegmenter makeEmptySegmenter();

    UErrorCode errorCode_;
};

// ---------------------------------------------

}  // namespace segmenter

U_NAMESPACE_END

#endif // U_HIDE_DRAFT_API

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif  // __SEGEMENTER_RULEBASED_H__
