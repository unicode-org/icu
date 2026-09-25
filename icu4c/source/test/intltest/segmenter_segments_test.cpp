// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#if !UCONFIG_NO_BREAK_ITERATION


#include "unicode/rbbi.h"

#include "unicode/segmenter.h"
#include "unicode/segmenter_localized.h"
#include "unicode/segmenter_rulebased.h"
#include "unicode/segmenter_segment_iter.h"
#include "unicode/segmenter_segment_range.h"
#include "segmenter_segments_test.h"

#include <iostream>
#include <memory>

//---------------------------------------------
//
// runIndexedTest
//
//---------------------------------------------

void SegmentsTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* params )
{
    if (exec) logln("TestSuite SegmentsTest: ");
    fTestParams = params;

    TESTCASE_AUTO_BEGIN;

    TESTCASE_AUTO(testHelloWorld);
    TESTCASE_AUTO(testMoveConstructor);
    TESTCASE_AUTO(testMoveAssignment);
    TESTCASE_AUTO(testEmptyRules);
    TESTCASE_AUTO(testSegmentAt);
    TESTCASE_AUTO(testSegments);
    TESTCASE_AUTO(testSubstr);

    TESTCASE_AUTO_END;
}

//--------------------------------------------------------------------------------------
//
//    SegmentsTest    constructor and destructor
//
//--------------------------------------------------------------------------------------

SegmentsTest::SegmentsTest() {
}

SegmentsTest::~SegmentsTest() {
}

//---------------------------------------------
//
//     Tests
//
//---------------------------------------------

void SegmentsTest::testHelloWorld() {
    IcuTestErrorCode errorCode(*this, "testHelloWorld");

    icu::segmenter::RuleBasedSegmenterBuilder builder;
    builder.setRules(u"[A-Za-züä]+;");
    auto segmenter = builder.build(errorCode);

    errorCode.errIfFailureAndReset();

    auto segments = segmenter.segment(u"Kühlschränke kühlen Getränke", errorCode);

    errorCode.errIfFailureAndReset();

    assertTrue("Index 0 is boundary", segments->isBoundary(0));
}

void SegmentsTest::testEmptyRules() {
    IcuTestErrorCode errorCode(*this, "testEmptyRules");

    icu::segmenter::RuleBasedSegmenterBuilder builder;
    icu::segmenter::RuleBasedSegmenter rbSegmenter = builder.build(errorCode);

    assertEquals("RuleBasedSegmenter needs non-empty rules", U_BRK_RULE_SYNTAX, errorCode.reset());
}

void SegmentsTest::testMoveConstructor() {
    IcuTestErrorCode errorCode(*this, "testMoveConstructor");

    icu::segmenter::RuleBasedSegmenterBuilder builder;
    builder.setRules(u"[A-Za-züä]+;");
    auto segmenter1 = builder.build(errorCode);
    errorCode.errIfFailureAndReset();
    auto segments1 = segmenter1.segment(u"Kühlschränke kühlen Getränke", errorCode);
    errorCode.errIfFailureAndReset();
    assertTrue( "Index 0  is a boundary",     segments1->isBoundary(0));
    assertFalse("Index 1  is not a boundary", segments1->isBoundary(1));
    assertFalse("Index 2  is not a boundary", segments1->isBoundary(2));
    assertTrue( "Index 12 is a boundary",     segments1->isBoundary(12));

    auto segmenter2(std::move(segmenter1));

    auto segments2 = segmenter2.segment(u"Kühlschränke kühlen Getränke", errorCode);
    errorCode.errIfFailureAndReset();
    assertTrue( "Index 0  is a boundary",     segments1->isBoundary(0));
    assertFalse("Index 1  is not a boundary", segments1->isBoundary(1));
    assertFalse("Index 2  is not a boundary", segments1->isBoundary(2));
    assertTrue( "Index 12 is a boundary",     segments1->isBoundary(12));
}

void SegmentsTest::testMoveAssignment() {
    IcuTestErrorCode errorCode(*this, "testMoveAssignment");

    icu::segmenter::RuleBasedSegmenterBuilder builder1;
    builder1.setRules(u"[A-Za-züä]+;");
    auto segmenter1 = builder1.build(errorCode);
    errorCode.errIfFailureAndReset();
    auto segments1 = segmenter1.segment(u"Kühlschränke kühlen Getränke", errorCode);
    errorCode.errIfFailureAndReset();
    assertTrue( "Index 0  is a boundary",     segments1->isBoundary(0));
    assertFalse("Index 1  is not a boundary", segments1->isBoundary(1));
    assertFalse("Index 2  is not a boundary", segments1->isBoundary(2));
    assertTrue( "Index 12 is a boundary",     segments1->isBoundary(12));

    icu::segmenter::RuleBasedSegmenterBuilder builder2;
    builder2.setRules(u"[a-z]+;");
    auto segmenter2 = builder2.build(errorCode);
    errorCode.errIfFailureAndReset();
    auto segments2 = segmenter2.segment(u"Kühlschränke kühlen Getränke", errorCode);
    errorCode.errIfFailureAndReset();
    assertTrue("Index 0  is a boundary", segments2->isBoundary(0));
    assertTrue("Index 1  is a boundary", segments2->isBoundary(1));
    assertTrue("Index 2  is a boundary", segments2->isBoundary(2));
    assertTrue("Index 12 is a boundary", segments2->isBoundary(12));

    segmenter2 = std::move(segmenter1);
    segments2 = segmenter2.segment(u"Kühlschränke kühlen Getränke", errorCode);
    errorCode.errIfFailureAndReset();
    assertTrue( "Index 0  is a boundary",     segments2->isBoundary(0));
    assertFalse("Index 1  is not a boundary", segments2->isBoundary(1));
    assertFalse("Index 2  is not a boundary", segments2->isBoundary(2));
    assertTrue( "Index 12 is a boundary",     segments2->isBoundary(12));
}

// TODO: create tests for move {constructor, assignment} for LocalizedSegmenter, too

void SegmentsTest::testSegmentAt() {
    IcuTestErrorCode errorCode(*this, "testSegmentAt");

    icu::segmenter::LocalizedSegmenter enWordSegmenter =
        icu::segmenter::LocalizedSegmenterBuilder()
            .setLocale(Locale::getEnglish())
            .setSegmentationType(icu::segmenter::SegmentationType::WORD)
            .build(errorCode);

    std::u16string_view source1 = u"The quick brown fox jumped over the lazy dog.";

    // Create new Segments for source1
    auto segments1 = enWordSegmenter.segment(source1, errorCode);

    icu::segmenter::Segment firstSegment = segments1->segmentAt(0, errorCode);
    errorCode.errIfFailureAndReset();
    assertEquals("first segment start", 0, firstSegment.getStart());
    assertEquals("first segment limit", 3, firstSegment.getLimit());

    icu::segmenter::Segment secondSegment = segments1->segmentAt(3, errorCode);
    errorCode.errIfFailureAndReset();
    assertEquals("second segment start", 3, secondSegment.getStart());
    assertEquals("second segment limit", 4, secondSegment.getLimit());   
}

void SegmentsTest::testSegments() {
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
    auto segmentIter = segmentRange.begin();

    icu::segmenter::Segment firstSegment = *segmentIter;
    assertEquals("first segment start", 0, firstSegment.getStart());
    assertEquals("first segment limit", 3, firstSegment.getLimit());

    ++segmentIter;

    icu::segmenter::Segment secondSegment = *segmentIter;
    assertEquals("second segment start", 3, secondSegment.getStart());
    assertEquals("second segment limit", 4, secondSegment.getLimit());   
}

void SegmentsTest::testSubstr() {
    IcuTestErrorCode errorCode(*this, "testSubstr");

    icu::segmenter::LocalizedSegmenter enWordSegmenter =
        icu::segmenter::LocalizedSegmenterBuilder()
            .setLocale(Locale::getEnglish())
            .setSegmentationType(icu::segmenter::SegmentationType::WORD)
            .build(errorCode);

    std::u16string_view source1 = u"The quick brown fox jumped over the lazy dog.";

    // Create new Segments for source1
    auto segments1 = enWordSegmenter.segment(source1, errorCode);
    auto segmentRange = segments1->segments();
    auto segmentIter = segmentRange.begin();

    icu::segmenter::Segment firstSegment = *segmentIter;
    assertEquals("first segment substr", u"The", firstSegment.getSubstr());

    ++segmentIter;

    icu::segmenter::Segment secondSegment = *segmentIter;
    assertEquals("second segment substr", u" ", secondSegment.getSubstr());
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */