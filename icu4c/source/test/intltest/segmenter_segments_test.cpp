// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#if !UCONFIG_NO_BREAK_ITERATION


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

    TESTCASE_AUTO(TestHelloWorld);

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

void SegmentsTest::TestHelloWorld() {
    std::cout << "hello" << std::endl;

    // UErrorCode  errorCode = U_ZERO_ERROR;

    std::unique_ptr<icu::segmenter::Segment> segment(new icu::segmenter::Segment());

    // TODO: modify signature to match ICU4J Segmenter API design
    std::unique_ptr<icu::segmenter::Segmenter> rbSegmenter(new icu::segmenter::RuleBasedSegmenter());

    assertEquals("this assertion should fail", 0, 1);
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */