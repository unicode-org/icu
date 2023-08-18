// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/gregocal.h"
#include "unicode/messageformat2.h"
#include "messageformat2test.h"

using namespace icu::message2;

/*
  Tests based on ICU4J's Mf2IcuTest.java
*/

/*
  TODO: Tests need to be unified in a single format that
  both ICU4C and ICU4J can use, rather than being embedded in code.
*/

/*
Tests reflect the syntax specified in

  https://github.com/unicode-org/message-format-wg/commits/main/spec/message.abnf

as of the following commit from 2023-05-09:
  https://github.com/unicode-org/message-format-wg/commit/194f6efcec5bf396df36a19bd6fa78d1fa2e0867

*/

void TestMessageFormat2::testSample(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    (void) testBuilder;
}

void TestMessageFormat2::testStaticFormat(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    (void) testBuilder;
}

void TestMessageFormat2::testSimpleFormat(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    (void) testBuilder;
}

void TestMessageFormat2::testSelectFormatToPattern(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    (void) testBuilder;
}


void TestMessageFormat2::testMessageFormatDateTimeSkeleton(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    (void) testBuilder;
}

void TestMessageFormat2::testMf1Behavior(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    (void) testBuilder;
}

void TestMessageFormat2::featureTests() {
    IcuTestErrorCode errorCode(*this, "featureTests");

    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    testBuilder->setName("featureTests");

    testSample(*testBuilder, errorCode);
    testStaticFormat(*testBuilder, errorCode);
    testSimpleFormat(*testBuilder, errorCode);
    testSelectFormatToPattern(*testBuilder, errorCode);
    testMessageFormatDateTimeSkeleton(*testBuilder, errorCode);
    testMf1Behavior(*testBuilder, errorCode);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
