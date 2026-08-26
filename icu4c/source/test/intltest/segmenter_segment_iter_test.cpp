// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#if !UCONFIG_NO_BREAK_ITERATION

#include "segmenter_segment_iter_test.h"

#include <string_view>

#include "unicode/segmenter.h"
#include "unicode/segmenter_localized.h"
#include "unicode/segmenter_segment_iter.h"
#include "unicode/segmenter_segment_range.h"

//---------------------------------------------
//
// runIndexedTest
//
//---------------------------------------------

void SegmentIterTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* params )
{
    if (exec) logln("TestSuite SegmentIterTest: ");
    fTestParams = params;

    TESTCASE_AUTO_BEGIN;

    TESTCASE_AUTO(testHelloWorld);

    TESTCASE_AUTO_END;
}

//--------------------------------------------------------------------------------------
//
//    SegmentIterTest    constructor and destructor
//
//--------------------------------------------------------------------------------------

SegmentIterTest::SegmentIterTest() {
}

SegmentIterTest::~SegmentIterTest() {
}

//---------------------------------------------
//
//     Tests
//
//---------------------------------------------

void SegmentIterTest::testHelloWorld() {
    IcuTestErrorCode errorCode(*this, "testHelloWorld");

    icu::segmenter::LocalizedSegmenter enWordSegmenter =
        icu::segmenter::LocalizedSegmenterBuilder()
            .setLocale(Locale::getEnglish())
            .setSegmentationType(icu::segmenter::SegmentationType::WORD)
            .build(errorCode);

    std::u16string_view source1 = u"The quick brown fox jumped over the lazy dog.";

    // Create new Segments for source1
    auto segments1 = enWordSegmenter.segment(source1, errorCode);
    auto segmentRange = segments1->segments();

    std::vector<std::u16string_view> segmentStrs;

    for (auto segmentIter = segmentRange.begin(); segmentIter != segmentRange.end(); ++segmentIter) {
        segmentStrs.push_back((*segmentIter).getSubstr());
    }

    std::vector<std::u16string_view> expected{u"The", u" ", u"quick", u" ", u"brown", u" ", u"fox", u" ", u"jumped", u" ", u"over",
                        u" ", u"the", u" ", u"lazy", u" ", u"dog", u"."};
    assertTrue("segment strings from SegmentIterator", segmentStrs == expected);
}

//---------------------------------------------
//
//     helper methods
//
//---------------------------------------------

void SegmentIterTest::zigzag(icu::segmenter::SegmentIterator & iter, const icu::segmenter::SegmentIterator & begin, const icu::segmenter::SegmentIterator & end) {
    zigzag(iter, begin, end, "**+*+--*PPp++*p--+P+pP-*-*");
}

void SegmentIterTest::zigzag(icu::segmenter::SegmentIterator & iter, const icu::segmenter::SegmentIterator & begin, const icu::segmenter::SegmentIterator & end, const char* path) {
    // TODO: fill in. Can use utfiteratortest.cpp zigzag() to start from
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */