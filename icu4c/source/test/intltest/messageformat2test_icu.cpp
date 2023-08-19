// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/gregocal.h"
#include "unicode/messageformat2.h"
#include "unicode/msgfmt.h"
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

    LocalPointer<TestCase> test(testBuilder.setPattern("{There are {$count} files on {$where}}")
                                .setArgument("count", "abc", errorCode)
                                .setArgument("where", "def", errorCode)
                                .setExpected("There are abc files on def")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testStaticFormat(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test(testBuilder.setPattern("{At {$when :datetime timestyle=default} on {$when :datetime datestyle=default}, \
there was {$what} on planet {$planet :number kind=integer}.}")
                                .setArgument("planet", (long) 7, errorCode)
                                .setArgument("when", (UDate) 871068000000, errorCode)
                                .setArgument("what", "a disturbance in the Force", errorCode)
                                .setExpected("At 12:20:00\u202FPM on Aug 8, 1997, there was a disturbance in the Force on planet 7.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testSimpleFormat(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    
    testBuilder.setPattern("{The disk \"{$diskName}\" contains {$fileCount} file(s).}");
    testBuilder.setArgument("diskName", "MyDisk", errorCode);

    LocalPointer<TestCase> test(testBuilder.setArgument("fileCount", (long) 0, errorCode)
                                .setExpected("The disk \"MyDisk\" contains 0 file(s).")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setArgument("fileCount", (long) 1, errorCode)
                      .setExpected("The disk \"MyDisk\" contains 1 file(s).")
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setArgument("fileCount", (long) 12, errorCode)
                      .setExpected("The disk \"MyDisk\" contains 12 file(s).")
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testSelectFormatToPattern(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    UnicodeString pattern = "match {$userGender :select}\n\
                 when female {{$userName} est all\u00E9e \u00E0 Paris.}\n\
                 when  *     {{$userName} est all\u00E9 \u00E0 Paris.}";

    testBuilder.setPattern(pattern);

    LocalPointer<TestCase> test(testBuilder.setArgument("userName", "Charlotte", errorCode)
                                .setArgument("userGender", "female", errorCode)
                                .setExpected("Charlotte est allée à Paris.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setArgument("userName", "Guillaume", errorCode)
                                .setArgument("userGender", "male", errorCode)
                                .setExpected("Guillaume est allé à Paris.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setArgument("userName", "Dominique", errorCode)
                                .setArgument("userGender", "unknown", errorCode)
                                .setExpected("Dominique est allé à Paris.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}


void TestMessageFormat2::testMessageFormatDateTimeSkeleton(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<GregorianCalendar> cal(new GregorianCalendar(2021, Calendar::NOVEMBER, 23, 16, 42, 55, errorCode));
    CHECK_ERROR(errorCode);
    UDate date = cal->getTime(errorCode);
    testBuilder.setLocale(Locale::forLanguageTag("en", errorCode), errorCode);
    testBuilder.setDateArgument("when", date, errorCode);
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test(testBuilder.setPattern("{{$when :datetime skeleton=MMMMd}}")
                                .setExpected("November 23")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{{$when :datetime skeleton=yMMMMdjm}}")
                                .setExpected("November 23, 2021 at 4:42\u202FPM")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{{$when :datetime skeleton=|   yMMMMd   |}}")
                                .setExpected("November 23, 2021")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{{$when :datetime skeleton=yMMMMd}}")
                                .setExpected("23 novembre 2021")
                                .setLocale(Locale::forLanguageTag("fr", errorCode), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Expiration: {$when :datetime skeleton=yMMM}!}")
                                .setExpected("Expiration: Nov 2021!")
                                .setLocale(Locale::forLanguageTag("en", errorCode), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{{$when :datetime pattern=|'::'yMMMMd|}}")
                                .setExpected("::2021November23")
                                .setLocale(Locale::forLanguageTag("en", errorCode), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testMf1Behavior(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    UDate testDate = UDate(1671782400000); // 2022-12-23
    UnicodeString user = "John";
    UnicodeString badArgumentsNames[] = {
        "userX", "todayX"
    };
    UnicodeString goodArgumentsNames[] = {
        "user", "today"
    };
    Formattable argumentsValues[] = {
        Formattable(user), Formattable(testDate, Formattable::kIsDate)
    };
    UnicodeString expectedGood = "Hello John, today is December 23, 2022.";

    LocalPointer<MessageFormat> mf1(new MessageFormat("Hello {user}, today is {today,date,long}.", errorCode));
    CHECK_ERROR(errorCode);

    UnicodeString result;
    mf1->format(badArgumentsNames, argumentsValues, 2, result, errorCode);
    assert(U_SUCCESS(errorCode));
    assertEquals("old icu test", "Hello {user}, today is {today}.", result);
    result.remove();
    mf1->format(goodArgumentsNames, argumentsValues, 2, result, errorCode);
    assert(U_SUCCESS(errorCode));
    assertEquals("old icu test", expectedGood, result);

    LocalPointer<TestCase> test(testBuilder.setPattern("{Hello {$user}, today is {$today :datetime datestyle=long}.}")
                                .setArgument(badArgumentsNames[0], user, errorCode)
                                .setDateArgument(badArgumentsNames[1], testDate, errorCode)
                                .setExpected("Hello {$user}, today is {$today}.")
                                .setExpectedWarning(U_UNRESOLVED_VARIABLE_WARNING)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.clearArguments()
                      .setExpectSuccess()
                      .setArgument(goodArgumentsNames[0], user, errorCode)
                      .setDateArgument(goodArgumentsNames[1], testDate, errorCode)
                      .setExpected(expectedGood)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::messageFormat1Tests() {
    IcuTestErrorCode errorCode(*this, "featureTests");

    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    testBuilder->setName("messageFormat1Tests");

    testSample(*testBuilder, errorCode);
    testStaticFormat(*testBuilder, errorCode);
    testSimpleFormat(*testBuilder, errorCode);
    testSelectFormatToPattern(*testBuilder, errorCode);
    testMessageFormatDateTimeSkeleton(*testBuilder, errorCode);
    testMf1Behavior(*testBuilder, errorCode);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
