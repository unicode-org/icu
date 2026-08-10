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


    UnicodeString ustrRules(u"[A-Za-züä]+;");
    UnicodeString ustrText(u"Kühlschränke kühlen Getränke");
    UParseError parseError;
    std::unique_ptr<BreakIterator> iter = std::make_unique<RuleBasedBreakIterator>(ustrRules, parseError, errorCode);
    iter->setText(ustrText);
    int32_t firstBoundary = iter->next();
    assertTrue("Index 0 is boundary", iter->isBoundary(firstBoundary));


    // std::unique_ptr<icu::segmenter::Segment> segment(new icu::segmenter::Segment());

    // TODO: modify signature to match ICU4J Segmenter API design
    icu::segmenter::RuleBasedSegmenterBuilder builder;
    builder.setRules(u"[A-Za-züä]+;");
    icu::segmenter::RuleBasedSegmenter rbSegmenter = builder.build(errorCode);

    errorCode.errIfFailureAndReset();

    auto someSegmenter = std::make_unique<icu::segmenter::Segmenter>(builder.build(errorCode));
    auto segments = someSegmenter->segment(u"Kühlschränke kühlen Getränke", errorCode);

    errorCode.errIfFailureAndReset();

    assertTrue("Index 0 is boundary", segments->isBoundary(0));

    // TODO: uncomment once segment() is implemented
    // errorCode.errIfFailureAndReset();
}

void SegmentsTest::testEmptyRules() {
    IcuTestErrorCode errorCode(*this, "testEmptyRules");

    icu::segmenter::RuleBasedSegmenterBuilder builder;
    icu::segmenter::RuleBasedSegmenter rbSegmenter = builder.build(errorCode);

    assertEquals("RuleBasedSegmenter needs non-empty rules", U_ILLEGAL_ARGUMENT_ERROR, errorCode.reset());
}

void SegmentsTest::testMoveConstructor() {
    IcuTestErrorCode errorCode(*this, "testMoveConstructor");

    icu::segmenter::RuleBasedSegmenterBuilder builder;
    builder.setRules(u"[A-Za-züä]+;");
    icu::segmenter::RuleBasedSegmenter rbSegmenter1 = builder.build(errorCode);

    errorCode.errIfFailureAndReset();

    icu::segmenter::RuleBasedSegmenter rbSegmenter2(std::move(rbSegmenter1));

    rbSegmenter2.segment(u"Kühlschränke kühlen Getränke", errorCode);

    assertEquals("segment() is temporarily unsupported", U_UNSUPPORTED_ERROR, errorCode.reset());

    // TODO: uncomment once segment() is implemented
    // errorCode.errIfFailureAndReset();
}

void SegmentsTest::testMoveAssignment() {
    IcuTestErrorCode errorCode(*this, "testMoveAssignment");

    icu::segmenter::RuleBasedSegmenterBuilder builder;
    builder.setRules(u"[A-Za-züä]+;");
    icu::segmenter::RuleBasedSegmenter rbSegmenter1 = builder.build(errorCode);

    errorCode.errIfFailureAndReset();

    icu::segmenter::RuleBasedSegmenter rbSegmenter2 = std::move(rbSegmenter1);

    rbSegmenter2.segment(u"Kühlschränke kühlen Getränke", errorCode);

    assertEquals("segment() is temporarily unsupported", U_UNSUPPORTED_ERROR, errorCode.reset());

    // TODO: uncomment once segment() is implemented
    // errorCode.errIfFailureAndReset();
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */