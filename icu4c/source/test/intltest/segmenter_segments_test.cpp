// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#if !UCONFIG_NO_BREAK_ITERATION


#include "unicode/rbbi.h"

#include "unicode/segmenter.h"
#include "unicode/segmenter_rulebased.h"
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

    // TODO: modify signature to match ICU4J Segmenter API design
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

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */