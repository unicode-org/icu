// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "unicode/utypes.h"
#if !UCONFIG_NO_BREAK_ITERATION

#include "segmenter_segment_iter_test.h"

#include "unicode/segmenter_segment_iter.h"

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