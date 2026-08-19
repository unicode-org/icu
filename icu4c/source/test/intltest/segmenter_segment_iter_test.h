// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#ifndef SEGMENTER_SEGMENTITER_TEST
#define SEGMENTER_SEGMENTITER_TEST

#include "intltest.h"
#include "unicode/segmenter_segment_iter.h"

/**
 * Segments test
 */
class SegmentIterTest: public IntlTest {
public:
    SegmentIterTest();
    virtual ~SegmentIterTest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = nullptr ) override;

    void testHelloWorld();

private:
    // Test parameters, from the test framework and test invocation.
    const char* fTestParams;

    void zigzag(icu::segmenter::SegmentIterator & iter, const icu::segmenter::SegmentIterator & begin, const icu::segmenter::SegmentIterator & end);

    // TODO: create systematic tests that use this to extend test coverage
    void zigzag(icu::segmenter::SegmentIterator & iter, const icu::segmenter::SegmentIterator & begin, const icu::segmenter::SegmentIterator & end, const char* path);


};

#endif