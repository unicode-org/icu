// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#if !UCONFIG_NO_BREAK_ITERATION


#include "unicode/segmenter.h"
#include "segmenter_segments_test.h"

#include <iostream>

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
    assertEquals("this assertion should fail", 0, 1);
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */