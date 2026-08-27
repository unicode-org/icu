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

    TESTCASE_AUTO(testSegments);
    TESTCASE_AUTO(testMultipleSegmentObjectsFromSegmenter);

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

void SegmentIterTest::testSegments() {
    IcuTestErrorCode errorCode(*this, "testSegments");

    icu::segmenter::LocalizedSegmenter enWordSegmenter =
        icu::segmenter::LocalizedSegmenterBuilder()
            .setLocale(Locale::getEnglish())
            .setSegmentationType(icu::segmenter::SegmentationType::WORD)
            .build(errorCode);

    std::u16string_view source1 = u"The quick brown fox jumped over the lazy dog.";

    // Create new Segments for source1
    auto segments1 = enWordSegmenter.segment(source1, errorCode);
    auto segmentRange = segments1->segments();

    std::vector<icu::segmenter::Segment> segmentVec;
    for (auto segmentIter = segmentRange.begin(); segmentIter != segmentRange.end(); ++segmentIter) {
        segmentVec.push_back(*segmentIter);
    }
    assertEquals("first segment start", 0, segmentVec[0].getStart());
    assertEquals("first segment limit", 3, segmentVec[0].getLimit());
    assertEquals("second segment start", 3, segmentVec[1].getStart());
    assertEquals("second segment limit", 4, segmentVec[1].getLimit());
}

void SegmentIterTest::testMultipleSegmentObjectsFromSegmenter() {
    IcuTestErrorCode errorCode(*this, "testMultipleSegmentObjectsFromSegmenter");

    icu::segmenter::LocalizedSegmenter enWordSegmenter =
        icu::segmenter::LocalizedSegmenterBuilder()
            .setLocale(Locale::getEnglish())
            .setSegmentationType(icu::segmenter::SegmentationType::WORD)
            .build(errorCode);
    
    std::u16string_view source1 = u"The quick brown fox jumped over the lazy dog.";
    std::u16string_view source2 = u"Sphinx of black quartz, judge my vow.";
    std::u16string_view source3 = u"How vexingly quick daft zebras jump!";

    std::vector<std::u16string_view> exp1{u"The", u" ", u"quick", u" ", u"brown", u" ", u"fox", u" ", u"jumped", u" ", u"over",
                        u" ", u"the", u" ", u"lazy", u" ", u"dog", u"."};
    std::vector<std::u16string_view> exp2{u"Sphinx", u" ", u"of", u" ", u"black", u" ", u"quartz", u",", u" ", u"judge", u" ",
                        u"my", u" ", u"vow", u"."};
    std::vector<std::u16string_view> exp3{
                        u"How",
                        u" ",
                        u"vexingly",
                        u" ",
                        u"quick",
                        u" ",
                        u"daft",
                        u" ",
                        u"zebras",
                        u" ",
                        u"jump",
                        u"!"};

    // Create new Segments for source1
    auto segments1 = enWordSegmenter.segment(source1, errorCode);
    auto segmentRange1 = segments1->segments();
    std::vector<std::u16string_view> segmentStrVec1;
    for (auto segmentIter = segmentRange1.begin(); segmentIter != segmentRange1.end(); ++segmentIter) {
        segmentStrVec1.push_back((*segmentIter).getSubstr());
    }   
    assertTrue("segment strings from SegmentIterator 1", segmentStrVec1 == exp1);

    // Create new Segments for source2
    auto segments2 = enWordSegmenter.segment(source2, errorCode);
    auto segmentRange2 = segments2->segments();
    std::vector<std::u16string_view> segmentStrVec2;
    for (auto segmentIter = segmentRange2.begin(); segmentIter != segmentRange2.end(); ++segmentIter) {
        segmentStrVec2.push_back((*segmentIter).getSubstr());
    }   
    assertTrue("segment strings from SegmentIterator 2", segmentStrVec2 == exp2);

    // Check that Segments for source1 is unaffected
    segmentStrVec1.clear();
    for (auto segmentIter = segmentRange1.begin(); segmentIter != segmentRange1.end(); ++segmentIter) {
        segmentStrVec1.push_back((*segmentIter).getSubstr());
    }   
    assertTrue("segment strings from SegmentIterator 1 unaffected after 2", segmentStrVec1 == exp1);

    // Create new Segments for source3
    auto segments3 = enWordSegmenter.segment(source3, errorCode);
    auto segmentRange3 = segments3->segments();
    std::vector<std::u16string_view> segmentStrVec3;
    for (auto segmentIter = segmentRange3.begin(); segmentIter != segmentRange3.end(); ++segmentIter) {
        segmentStrVec3.push_back((*segmentIter).getSubstr());
    }
    assertTrue("segment strings from SegmentIterator 3", segmentStrVec3 == exp3);

    // Check that Segments for source1 is unaffected
    segmentStrVec1.clear();
    for (auto segmentIter = segmentRange1.begin(); segmentIter != segmentRange1.end(); ++segmentIter) {
        segmentStrVec1.push_back((*segmentIter).getSubstr());
    }   
    assertTrue("segment strings from SegmentIterator unaffected after 3", segmentStrVec1 == exp1);

    // Check that Segments for source2 is unaffected
    segmentStrVec2.clear();
    for (auto segmentIter = segmentRange2.begin(); segmentIter != segmentRange2.end(); ++segmentIter) {
        segmentStrVec2.push_back((*segmentIter).getSubstr());
    }   
    assertTrue("segment strings from SegmentIterator 2 unaffected after 3", segmentStrVec2 == exp2);
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