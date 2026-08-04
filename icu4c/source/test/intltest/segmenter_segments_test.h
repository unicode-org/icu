// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#ifndef SEGMENTER_SEGMENTS_TEST
#define SEGMENTER_SEGMENTS_TEST

#include "intltest.h"

/**
 * Segments test
 */
class SegmentsTest: public IntlTest {
public:
    SegmentsTest();
    virtual ~SegmentsTest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = nullptr ) override;

    void testHelloWorld();

    void testMoveConstructor();

    void testMoveAssignment();

private:
    // Test parameters, from the test framework and test invocation.
    const char* fTestParams;

};

#endif