// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "messageformat2test.h"

using namespace icu::message2;

/*
Tests reflect the syntax specified in

  https://github.com/unicode-org/message-format-wg/commits/main/spec/message.abnf

as of the following commit from 2023-05-09:
  https://github.com/unicode-org/message-format-wg/commit/194f6efcec5bf396df36a19bd6fa78d1fa2e0867
*/


/*
    public void testAllKindOfDates() {

        // Pattern
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime pattern=(d 'of' MMMM, y 'at' HH:mm)}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 23 of November, 2022 at 19:42.")
                .build());
    }
*/

/*
void TestMessageFormat2::checkResult(const UnicodeString& testName,
                 const UnicodeString& pattern,
                 const UnicodeString& result,
                 const UnicodeString& expected,
                 IcuTestErrorCode& errorCode,
                 UErrorCode expectedErrorCode) {

  if (errorCode != expectedErrorCode) {
        dataerrln(pattern);
        if (U_SUCCESS(expectedErrorCode)) {
            logln(testName + UnicodeString(" failed test with error code ") + (int32_t)errorCode);
        } else if (U_SUCCESS(errorCode)) {
            logln(testName + UnicodeString(" succeeded test, but was expected to fail with ") + (int32_t)expectedErrorCode);
            return;
        } else {
            logln(testName + UnicodeString(" failed test with error code ") + (int32_t)errorCode + ", but was expected to fail with error code " + (int32_t)expectedErrorCode);
            return;
        }
  }

  errorCode.reset();

  if (result != expected) {
      dataerrln(pattern);
      logln("Expected output: " + expected + "\nGot output: " + result);
      ((UErrorCode&)errorCode) = U_MESSAGE_PARSE_ERROR;
      return;
  }
}

void TestMessageFormat2::testWithPatternAndArguments(const UnicodeString& testName,
                                                     FunctionRegistry* customRegistry,
                                                     const UnicodeString& pattern,
                                                     const UnicodeString& argName,
                                                     const UnicodeString& argValue,
                                                     const UnicodeString& expected,
                                                     IcuTestErrorCode& errorCode) {
    testWithPatternAndArguments(testName,
                                customRegistry,
                                pattern,
                                argName,
                                argValue,
                                expected,
                                errorCode,
                                U_ZERO_ERROR);
}

// TODO: only supports a single message argument for now
void TestMessageFormat2::testWithPatternAndArguments(const UnicodeString& testName,
                                                     FunctionRegistry* customRegistry,
                                                     const UnicodeString& pattern,
                                                     const UnicodeString& argName,
                                                     const UnicodeString& argValue,
                                                     const UnicodeString& expected,
                                                     IcuTestErrorCode& errorCode,
                                                     UErrorCode expectedErrorCode) {

    UParseError parseError; // Ignored in these tests

    if (U_FAILURE(errorCode)) {
        return;
    }

    LocalPointer<MessageFormatter::Builder> builder(MessageFormatter::builder(errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    LocalPointer<MessageFormatter> mf;

    if (customRegistry != nullptr) {
        mf.adoptInstead(builder
                        ->setPattern(pattern, errorCode)
                        .setFunctionRegistry(customRegistry)
                        .build(parseError, errorCode));
    } else {
        mf.adoptInstead(builder
                        ->setPattern(pattern, errorCode)
                        .build(parseError, errorCode));
    }

    if (U_FAILURE(errorCode)) {
        return;
    }

    LocalPointer<Hashtable> arguments(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    LocalPointer<UnicodeString> argValuePointer(new UnicodeString(argValue));
    if (!argValuePointer.isValid()) {
        ((UErrorCode&) errorCode) = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    arguments->put(argName, argValuePointer.orphan(), errorCode);
    UnicodeString result;
    mf->formatToString(*arguments, errorCode, result);

    checkResult(testName, pattern, result, expected, errorCode, expectedErrorCode);
}
*/

void TestMessageFormat2::testDateTime(IcuTestErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    // November 23, 2022 at 7:42:37.123 PM
    UnicodeString TEST_DATE = "1669261357123";
    UnicodeString date = "date";
    Locale loc = Locale("ro");

    testWithPatternAndArguments("testDateTime",
                                nullptr,
                                "{Testing date formatting: {$date :datetime}.}",
                                date,
                                TEST_DATE,
                                "Testing date formatting: 23.11.2022, 19:42.",
                                loc,
                                errorCode);

    loc = Locale("ro", "RO");

    // Skeleton
    testWithPatternAndArguments("testDateTime",
                                nullptr,
                                "{Testing date formatting: {$date :datetime skeleton=yMMMMd}.}",
                                date,
                                TEST_DATE,
                                "Testing date formatting: 23 noiembrie 2022.",
                                loc,
                                errorCode);
    testWithPatternAndArguments("testDateTime",
                                nullptr,
                                "{Testing date formatting: {$date :datetime skeleton=jm}.}",
                                date,
                                TEST_DATE,
                                "Testing date formatting: 19:42.",
                                loc,
                                errorCode);

    loc = Locale("en");
    testWithPatternAndArguments("testDateTime",
                                nullptr,
                                "{Testing date formatting: {$date :datetime skeleton=yMMMd}.}",
                                date,
                                TEST_DATE,
                                "Testing date formatting: Nov 23, 2022.",
                                loc,
                                errorCode);
    testWithPatternAndArguments("testDateTime",
                                nullptr,
                                "{Testing date formatting: {$date :datetime skeleton=yMMMdjms}.}",
                                date,
                                TEST_DATE,
                                "Testing date formatting: Nov 23, 2022, 7:42:37\u202FPM.",
                                loc,
                                errorCode);
   testWithPatternAndArguments("testDateTime",
                                nullptr,
                                "{Testing date formatting: {$date :datetime skeleton=jm}.}",
                                date,
                                TEST_DATE,
                                "Testing date formatting: 7:42\u202FPM.",
                                loc,
                                errorCode);
   // Style
  testWithPatternAndArguments("testDateTime",
                              nullptr,
                              "{Testing date formatting: {$date :datetime datestyle=long}.}",
                              date,
                              TEST_DATE,
                              "Testing date formatting: November 23, 2022.",
                              loc,
                              errorCode);
  testWithPatternAndArguments("testDateTime",
                              nullptr,
                              "{Testing date formatting: {$date :datetime datestyle=medium}.}",
                              date,
                              TEST_DATE,
                              "Testing date formatting: Nov 23, 2022.",
                              loc,
                              errorCode);
  testWithPatternAndArguments("testDateTime",
                              nullptr,
                              "{Testing date formatting: {$date :datetime datestyle=short}.}",
                              date,
                              TEST_DATE,
                              "Testing date formatting: 11/23/22.",
                              loc,
                              errorCode);
  testWithPatternAndArguments("testDateTime",
                              nullptr,
                              "{Testing date formatting: {$date :datetime timestyle=long}.}",
                              date,
                              TEST_DATE,
                              "Testing date formatting: 7:42:37\u202FPM PST.",
                              loc,
                              errorCode);
  testWithPatternAndArguments("testDateTime",
                              nullptr,
                              "{Testing date formatting: {$date :datetime timestyle=medium}.}",
                              date,
                              TEST_DATE,
                              "Testing date formatting: 7:42:37\u202FPM.",
                              loc,
                              errorCode);
  testWithPatternAndArguments("testDateTime",
                              nullptr,
                              "{Testing date formatting: {$date :datetime timestyle=short}.}",
                              date,
                              TEST_DATE,
                              "Testing date formatting: 7:42\u202FPM.",
                              loc,
                              errorCode);

  // Pattern
  testWithPatternAndArguments("testDateTime",
                              nullptr,
                              "{Testing date formatting: {$date :datetime pattern=|d 'of' MMMM, y 'at' HH:mm|}.}",
                              date,
                              TEST_DATE,
                              "Testing date formatting: 23 of November, 2022 at 19:42.",
                              loc,
                              errorCode);
}

void TestMessageFormat2::testNumbers(IcuTestErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    UnicodeString value = "1234567890.97531";
    UnicodeString val = "val";
    Locale loc = Locale("ro");

    // Literals
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{From literal: {123456789 :number}!}",
                                val,
                                value,
                                "From literal: 123.456.789!",
                                loc,
                                errorCode);
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{From literal: {|123456789,531| :number}!}",
                                val,
                                value,
                                "From literal: 123.456.789,531!",
                                loc,
                                errorCode);

    loc = Locale("my");
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{From literal: {|\u1041\u1042\u1043,\u1044\u1045\u1046,\u1047\u1048\u1049.\u1045\u1043\u1041| :number}!}",
                                val,
                                value,
                                "From literal: \u1041\u1042\u1043,\u1044\u1045\u1046,\u1047\u1048\u1049.\u1045\u1043\u1041!",
                                loc,
                                errorCode);
    // Skeletons
    loc = Locale("ro");
    value = "1234567890,97531";
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{Skeletons, minFraction: {$val :number skeleton=|.00000000*|}!}",
                                val,
                                value,
                                "Skeletons, minFraction: 1.234.567.890,97531000!",
                                loc,
                                errorCode);
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{Skeletons, maxFraction: {$val :number skeleton=|.###|}!}",
                                val,
                                value,
                                "Skeletons, maxFraction: 1.234.567.890,975!",
                                loc,
                                errorCode);
    // Currency
    loc = Locale("de");
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{Skeletons, currency: {$val :number skeleton=|currency/EUR|}!}",
                                val,
                                value,
                                "Skeletons, currency: 1.234.567.890,98\u00A0\u20AC!",
                                loc,
                                errorCode);
    // Currency as a parameter
    loc = Locale("de");
    UnicodeString skel = "skel";
    UnicodeString skelValue = "currency/EUR";
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{Skeletons, currency: {$val :number skeleton=$skel}!}",
                                val,
                                value,
                                skel,
                                skelValue,
                                "Skeletons, currency: 1.234.567.890,98\u00A0\u20AC!",
                                loc,
                                errorCode);
    skelValue = "currency/JPY";
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{Skeletons, currency: {$val :number skeleton=$skel}!}",
                                val,
                                value,
                                skel,
                                skelValue,
                                "Skeletons, currency: 1.234.567.891\u00A0\u00A5!",
                                loc,
                                errorCode);
    // Various measures
    loc = Locale("ro");
    UnicodeString celsius = "27";
    UnicodeString valC = "valC";
    UnicodeString valF = "valF";
    UnicodeString fahrenheit = "80,6";
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "let $intl = {$valC :number skeleton=|unit/celsius|}\n\
                                 let $us = {$valF :number skeleton=|unit/fahrenheit|}\n\
                                 {Temperature: {$intl} ({$us})}",
                                valC,
                                celsius,
                                valF,
                                fahrenheit,
                                "Temperature: 27 \u00B0C (80,6 \u00B0F)",
                                loc,
                                errorCode);
    UnicodeString len = "len";
    UnicodeString lenVal = "1,75";
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{Height: {$len :number skeleton=|unit/meter|}}",
                                len,
                                lenVal,
                                "Height: 1,75 m",
                                loc,
                                errorCode);
    loc = Locale("de");
    testWithPatternAndArguments("testNumbers",
                                nullptr,
                                "{Skeletons, currency: {$val :number skeleton=|currency/EUR|}!}",
                                val,
                                value,
                                "Skeletons, currency: 1.234.567.890,98\u00A0\u20AC!",
                                loc,
                                errorCode);
}

void TestMessageFormat2::testBuiltInFunctions() {
  IcuTestErrorCode errorCode(*this, "testBuiltInFunctions");

  testDateTime(errorCode);
  testNumbers(errorCode);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
