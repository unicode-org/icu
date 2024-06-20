// © 2024 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include <fstream>
#include <string>

#include "charstr.h"
#include "json-json.hpp"
#include "messageformat2test.h"
#include "messageformat2test_utils.h"

using namespace nlohmann;

using namespace icu::message2;

static UErrorCode getExpectedErrorFromString(const std::string& errorName) {
    if (errorName == "Variant Key Mismatch") {
        return U_MF_VARIANT_KEY_MISMATCH_ERROR;
    }
    if (errorName == "Missing Fallback Variant") {
        return U_MF_NONEXHAUSTIVE_PATTERN_ERROR;
    }
    if (errorName == "Missing Selector Annotation") {
        return U_MF_MISSING_SELECTOR_ANNOTATION_ERROR;
    }
    if (errorName == "Duplicate Declaration") {
        return U_MF_DUPLICATE_DECLARATION_ERROR;
    }
    if (errorName == "Unsupported Statement") {
        return U_MF_UNSUPPORTED_STATEMENT_ERROR;
    }
// Arbitrary default
    return U_MF_DUPLICATE_OPTION_NAME_ERROR;
}

static UErrorCode getExpectedRuntimeErrorFromString(const std::string& errorName) {
    if (errorName == "parse-error" || errorName == "empty-token" || errorName == "extra-content") {
        return U_MF_SYNTAX_ERROR;
    }
    if (errorName == "key-mismatch") {
        return U_MF_VARIANT_KEY_MISMATCH_ERROR;
    }
    if (errorName == "missing-var" || errorName == "unresolved-var") {
        return U_MF_UNRESOLVED_VARIABLE_ERROR;
    }
    if (errorName == "unsupported-annotation") {
        return U_MF_UNSUPPORTED_EXPRESSION_ERROR;
    }
    if (errorName == "bad-input" || errorName == "RangeError") {
        return U_MF_OPERAND_MISMATCH_ERROR;
    }
    if (errorName == "bad-option") {
        return U_MF_FORMATTING_ERROR;
    }
    if (errorName == "missing-func") {
        return U_MF_UNKNOWN_FUNCTION_ERROR;
    }
    if (errorName == "duplicate-declaration") {
        return U_MF_DUPLICATE_DECLARATION_ERROR;
    }
    if (errorName == "selector-error") {
        return U_MF_SELECTOR_ERROR;
    }
    if (errorName == "formatting-error") {
        return U_MF_FORMATTING_ERROR;
    }
// Arbitrary default
    return U_MF_UNSUPPORTED_STATEMENT_ERROR;
}

static UnicodeString u_str(std::string s) {
    return UnicodeString::fromUTF8(s);
}

static TestCase::Builder successTest(const std::string& testName,
                                     const std::string& messageText) {
    return TestCase::Builder().setName(u_str(testName))
        .setPattern(u_str(messageText))
        .setExpectSuccess();
}

static void makeTestName(char* buffer, size_t size, std::string fileName, int32_t testNum) {
    snprintf(buffer, size, "test from file: %s[%u]", fileName.c_str(), ++testNum);
}

static bool setArguments(TestCase::Builder& test, const json::object_t& params, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return true;
    }
    for (auto argsIter = params.begin(); argsIter != params.end(); ++argsIter) {
        const UnicodeString argName = u_str(argsIter->first);
        // Determine type of value
        if (argsIter->second.is_number()) {
            test.setArgument(argName,
                             argsIter->second.template get<double>());
        } else if (argsIter->second.is_string()) {
            test.setArgument(argName,
                             u_str(argsIter->second.template get<std::string>()));
        } else if (argsIter->second.is_object()) {
            // Dates: represent in tests as { "date" : timestamp }, to distinguish
            // from number values
            auto obj = argsIter->second.template get<json::object_t>();
            if (obj["date"].is_number()) {
                test.setDateArgument(argName, obj["date"]);
            } else if (obj["decimal"].is_string()) {
                // Decimal strings: represent in tests as { "decimal" : string },
                // to distinguish from string values
                test.setDecimalArgument(argName, obj["decimal"].template get<std::string>(), errorCode);
            }
        } else if (argsIter->second.is_boolean() || argsIter->second.is_null()) {
            return false; // For now, boolean and null arguments are unsupported
        }
    }
    return true;
}

static void runValidTest(TestMessageFormat2& icuTest,
                         const std::string& testName,
                         const json& j,
                         IcuTestErrorCode& errorCode) {
    auto j_object = j.template get<json::object_t>();
    std::string messageText;

    if (!j_object["src"].is_null()) {
        messageText = j_object["src"].template get<std::string>();
    } else {
        if (!j_object["srcs"].is_null()) {
            auto strings = j_object["srcs"].template get<std::vector<std::string>>();
            for (const auto &piece : strings) {
                messageText += piece;
            }
        }
        // Otherwise, it should probably be an error, but we just
        // treat this as the empty string
    }

    TestCase::Builder test = successTest(testName, messageText);

    // Certain ICU4J tests don't work yet in ICU4C.
    // See ICU-22754
    // ignoreCpp => only works in Java
    if (!j_object["ignoreCpp"].is_null()) {
        return;
    }

    if (!j_object["exp"].is_null()) {
        // Set expected result if it's present
        std::string expectedOutput = j["exp"].template get<std::string>();
        test.setExpected(u_str(expectedOutput));
    }

    if (!j_object["locale"].is_null()) {
        std::string localeStr = j_object["locale"].template get<std::string>();
        test.setLocale(Locale(localeStr.c_str()));
    }

    if (!j_object["params"].is_null()) {
        // Map from string to json
        auto params = j_object["params"].template get<json::object_t>();
        if (!setArguments(test, params, errorCode)) {
            return; // Skip tests with unsupported arguments
        }
    }

    if (!j_object["errors"].is_null()) {
        // Map from string to string
        auto errors = j_object["errors"].template get<std::vector<std::map<std::string, std::string>>>();
        // We only emit the first error, so we just hope the first error
        // in the list in the test is also the error we emit
        U_ASSERT(errors.size() > 0);
        std::string errorType = errors[0]["type"];
        if (errorType.length() <= 0) {
            errorType = errors[0]["name"];
        }
        // See TODO(options); ignore these tests for now
        if (errorType == "bad-option") {
            return;
        }
        test.setExpectedError(getExpectedRuntimeErrorFromString(errorType));
    }
    TestCase t = test.build();
    TestUtils::runTestCase(icuTest, t, errorCode);
}

static void runSyntaxErrorTest(TestMessageFormat2& icuTest,
                         const std::string& testName,
                         const json& j,
                         IcuTestErrorCode& errorCode) {
    auto messageText = j["src"].template get<std::string>();

    TestCase::Builder test = successTest(testName, messageText)
        .setExpectedError(U_MF_SYNTAX_ERROR);

    auto j_object = j.template get<json::object_t>();

    int32_t lineNumber = 0;
    int32_t offset = -1;
    if (!j_object["char"].is_null()) {
        offset = j_object["char"].template get<int32_t>();
    }
    if (!j_object["line"].is_null()) {
        lineNumber = j_object["line"].template get<int32_t>();
    }
    if (offset != -1) {
        test.setExpectedLineNumberAndOffset(lineNumber, offset);
    }
    TestCase t = test.build();
    TestUtils::runTestCase(icuTest, t, errorCode);
}

// File name is relative to message2/ in the test data directory
static void runICU4JSyntaxTestsFromJsonFile(TestMessageFormat2& t,
                                            const std::string& fileName,
                                            IcuTestErrorCode& errorCode) {
    const char* testDataDirectory = IntlTest::getSharedTestData(errorCode);
    CHECK_ERROR(errorCode);

    std::string testFileName(testDataDirectory);
    testFileName.append("message2/");
    testFileName.append(fileName);
    std::ifstream testFile(testFileName);
    json data = json::parse(testFile);

    // Map from string to json, where the strings are the function names
    auto tests = data.template get<json::object_t>();
    for (auto iter = tests.begin(); iter != tests.end(); ++iter) {
        int32_t testNum = 0;
        auto categoryName = iter->first;
        t.logln("ICU4J syntax tests:");
        t.logln(u_str(iter->second.dump()));

        // Array of tests
        auto testsForThisCategory = iter->second.template get<std::vector<std::string>>();

        TestCase::Builder test;
        test.setNoSyntaxError();
        for (auto testsIter = testsForThisCategory.begin();
             testsIter != testsForThisCategory.end();
             ++testsIter) {
            char testName[100];
            makeTestName(testName, sizeof(testName), categoryName, ++testNum);
            t.logln(testName);

            // Tests here are just strings, and we test that they run without syntax errors
            test.setPattern(u_str(*testsIter));
            TestCase testCase = test.build();
            TestUtils::runTestCase(t, testCase, errorCode);
        }

    }
}

// File name is relative to message2/ in the test data directory
static void runICU4JSelectionTestsFromJsonFile(TestMessageFormat2& t,
                                            const std::string& fileName,
                                            IcuTestErrorCode& errorCode) {
    const char* testDataDirectory = IntlTest::getSharedTestData(errorCode);
    CHECK_ERROR(errorCode);

    std::string testFileName(testDataDirectory);
    testFileName.append("message2/");
    testFileName.append(fileName);
    std::ifstream testFile(testFileName);
    json data = json::parse(testFile);

    int32_t testNum = 0;

    for (auto iter = data.begin(); iter != data.end(); ++iter) {
        // Each test has a "shared" and a "variations" field
        auto j_object = iter->get<json::object_t>();
        auto shared = j_object["shared"];
        auto variations = j_object["variations"];

        // Skip ignored tests
        if (!j_object["ignoreCpp"].is_null()) {
            return;
        }

        // shared has a "srcs" field
        auto strings = shared["srcs"].template get<std::vector<std::string>>();
        std::string messageText;
        for (const auto &piece : strings) {
            messageText += piece;
        }

        t.logln(u_str("ICU4J selectors tests: " + fileName));
        t.logln(u_str(iter->dump()));

        TestCase::Builder test;
        char testName[100];
        makeTestName(testName, sizeof(testName), fileName, ++testNum);
        test.setName(testName);

        // variations has "params" and "exp" fields, and an optional "locale"
        for (auto variationsIter = variations.begin(); variationsIter != variations.end(); ++variationsIter) {
            auto variation = variationsIter->get<json::object_t>();
            auto params = variation["params"];
            auto exp = variation["exp"];

            test.setExpected(u_str(exp));
            test.setPattern(u_str(messageText));
            test.setExpectSuccess();
            setArguments(test, params, errorCode);

            if (!variation["locale"].is_null()) {
                std::string localeStr = variation["locale"].template get<std::string>();
                test.setLocale(Locale(localeStr.c_str()));
            }

            TestCase testCase = test.build();
            TestUtils::runTestCase(t, testCase, errorCode);
        }
    }
}

// File name is relative to message2/ in the test data directory
static void runValidTestsFromJsonFile(TestMessageFormat2& t,
                                      const std::string& fileName,
                                      IcuTestErrorCode& errorCode) {
    const char* testDataDirectory = IntlTest::getSharedTestData(errorCode);
    CHECK_ERROR(errorCode);

    std::string testFileName(testDataDirectory);
    testFileName.append("message2/");
    testFileName.append(fileName);
    std::ifstream testFile(testFileName);
    json data = json::parse(testFile);

    int32_t testNum = 0;
    char testName[100];

    for (auto iter = data.begin(); iter != data.end(); ++iter) {
        makeTestName(testName, sizeof(testName), fileName, ++testNum);
        t.logln(testName);

        t.logln(u_str(iter->dump()));

        runValidTest(t, testName, *iter, errorCode);
    }
}

// File name is relative to message2/ in the test data directory
static void runDataModelErrorTestsFromJsonFile(TestMessageFormat2& t,
                                               const std::string& fileName,
                                               IcuTestErrorCode& errorCode) {
    const char* testDataDirectory = IntlTest::getSharedTestData(errorCode);
    CHECK_ERROR(errorCode);

    std::string dataModelErrorsFileName(testDataDirectory);
    dataModelErrorsFileName.append("message2/");
    dataModelErrorsFileName.append(fileName);
    std::ifstream dataModelErrorsFile(dataModelErrorsFileName);
    json data = json::parse(dataModelErrorsFile);

    // Do tests for data model errors
    // This file is an object where the keys are error names
    // and the values are arrays of strings.
    // The whole file can be represented
    // as a map from strings to a vector of strings.
    using dataModelErrorType = std::map<std::string, std::vector<std::string>>;
    auto dataModelErrorTests = data.template get<dataModelErrorType>();
    for (auto iter = dataModelErrorTests.begin(); iter != dataModelErrorTests.end(); ++iter) {
        auto errorName = iter->first;
        auto messages = iter->second;

        UErrorCode expectedError = getExpectedErrorFromString(errorName);
        int32_t testNum = 0;
        char testName[100];
        TestCase::Builder testBuilder;
        for (auto messagesIter = messages.begin(); messagesIter != messages.end(); ++messagesIter) {
            makeTestName(testName, sizeof(testName), errorName, testNum);
            testBuilder.setName(testName);
            t.logln(u_str(fileName + ": " + testName));
            testNum++;
            UnicodeString messageText = u_str(*messagesIter);
            t.logln(messageText);

            TestCase test = testBuilder.setPattern(messageText)
                .setExpectedError(expectedError)
                .build();
            TestUtils::runTestCase(t, test, errorCode);
        }
    }

}

// File name is relative to message2/ in the test data directory
static void runSyntaxErrorTestsFromJsonFile(TestMessageFormat2& t,
                                            const std::string& fileName,
                                            IcuTestErrorCode& errorCode) {
    const char* testDataDirectory = IntlTest::getSharedTestData(errorCode);
    CHECK_ERROR(errorCode);

    std::string syntaxErrorsFileName(testDataDirectory);
    syntaxErrorsFileName.append("message2/");
    syntaxErrorsFileName.append(fileName);
    std::ifstream syntaxErrorsFile(syntaxErrorsFileName);
    json data = json::parse(syntaxErrorsFile);

    // Do tests for syntax errors
    // This file is just an array of strings
    int32_t testNum = 0;
    char testName[100];
    TestCase::Builder testBuilder;
    for (auto iter = data.begin(); iter != data.end(); ++iter) {
        makeTestName(testName, sizeof(testName), fileName, ++testNum);
        testBuilder.setName(testName);
        t.logln(testName);

        json json_string = *iter;
        UnicodeString cpp_string = u_str(json_string.template get<std::string>());

        t.logln(cpp_string);
        TestCase test = testBuilder.setPattern(cpp_string)
            .setExpectedError(U_MF_SYNTAX_ERROR)
            .build();
        TestUtils::runTestCase(t, test, errorCode);
    }
}

// File name is relative to message2/ in the test data directory
static void runSyntaxTestsWithDiagnosticsFromJsonFile(TestMessageFormat2& t,
                                                      const std::string& fileName,
                                                      IcuTestErrorCode& errorCode) {
    const char* testDataDirectory = IntlTest::getSharedTestData(errorCode);
    CHECK_ERROR(errorCode);

    std::string testFileName(testDataDirectory);
    testFileName.append("message2/");
    testFileName.append(fileName);
    std::ifstream testFile(testFileName);
    json data = json::parse(testFile);

    int32_t testNum = 0;
    char testName[100];

    for (auto iter = data.begin(); iter != data.end(); ++iter) {
        makeTestName(testName, sizeof(testName), fileName, ++testNum);
        t.logln(testName);
        t.logln(u_str(iter->dump()));

        runSyntaxErrorTest(t, testName, *iter, errorCode);
    }
}

// File name is relative to message2/ in the test data directory
static void runFunctionTestsFromJsonFile(TestMessageFormat2& t,
                                         const std::string& fileName,
                                         IcuTestErrorCode& errorCode) {
    // Get the test data directory
    const char* testDataDirectory = IntlTest::getSharedTestData(errorCode);
    CHECK_ERROR(errorCode);

    std::string functionTestsFileName(testDataDirectory);
    functionTestsFileName.append("message2/");
    functionTestsFileName.append(fileName);
    std::ifstream functionTestsFile(functionTestsFileName);
    json data = json::parse(functionTestsFile);

    // Map from string to json, where the strings are the function names
    auto tests = data.template get<json::object_t>();
    for (auto iter = tests.begin(); iter != tests.end(); ++iter) {
        int32_t testNum = 0;
        auto functionName = iter->first;
        t.logln(u_str("Function tests: " + fileName));
        t.logln(u_str(iter->second.dump()));

        // Array of tests
        auto testsForThisFunction = iter->second.template get<std::vector<json>>();
        for (auto testsIter = testsForThisFunction.begin();
             testsIter != testsForThisFunction.end();
             ++testsIter) {
            char testName[100];
            makeTestName(testName, sizeof(testName), functionName, ++testNum);
            t.logln(testName);

            runValidTest(t, testName, *testsIter, errorCode);
        }

    }

}

void TestMessageFormat2::jsonTestsFromFiles(IcuTestErrorCode& errorCode) {
    // Spec tests are fairly limited as the spec doesn't dictate formatter
    // output. Tests under testdata/message2/spec are taken from
    // https://github.com/unicode-org/message-format-wg/tree/main/test .
    // Tests directly under testdata/message2 are specific to ICU4C.

    // Do spec tests for syntax errors
    runSyntaxErrorTestsFromJsonFile(*this, "spec/syntax-errors.json", errorCode);
    runSyntaxErrorTestsFromJsonFile(*this, "more-syntax-errors.json", errorCode);

    // Do tests for data model errors
    runDataModelErrorTestsFromJsonFile(*this, "spec/data-model-errors.json", errorCode);
    runDataModelErrorTestsFromJsonFile(*this, "more-data-model-errors.json", errorCode);

    // Do valid spec tests
    runValidTestsFromJsonFile(*this, "spec/test-core.json", errorCode);

    // Do valid function tests
    runFunctionTestsFromJsonFile(*this, "spec/test-functions.json", errorCode);

    // Other tests (non-spec)
    runFunctionTestsFromJsonFile(*this, "more-functions.json", errorCode);
    runValidTestsFromJsonFile(*this, "valid-tests.json", errorCode);
    runValidTestsFromJsonFile(*this, "resolution-errors.json", errorCode);
    runValidTestsFromJsonFile(*this, "reserved-syntax.json", errorCode);
    runValidTestsFromJsonFile(*this, "matches-whitespace.json", errorCode);
    runValidTestsFromJsonFile(*this, "alias-selector-annotations.json", errorCode);
    runValidTestsFromJsonFile(*this, "runtime-errors.json", errorCode);

    // Re: the expected output for the first test in this file:
    // Note: the more "correct" fallback output seems like it should be "1.000 3" (ignoring the
    // overriding .input binding of $var2) but that's hard to achieve
    // as so-called "implicit declarations" can only be detected after parsing, at which
    // point the data model can't be modified.
    // Probably this is going to change anyway so that any data model error gets replaced
    // with a fallback for the whole message.
    // The second test has a similar issue with the output.
    runValidTestsFromJsonFile(*this, "tricky-declarations.json", errorCode);

    // Markup is ignored when formatting to string
    runValidTestsFromJsonFile(*this, "markup.json", errorCode);

    // TODO(duplicates): currently the expected output is based on using
    // the last definition of the duplicate-declared variable;
    // perhaps it's better to remove all declarations for $foo before formatting.
    // however if https://github.com/unicode-org/message-format-wg/pull/704 lands,
    // it'll be a moot point since the output will be expected to be the fallback string
    // (This applies to the expected output for all the U_DUPLICATE_DECLARATION_ERROR tests)
    runValidTestsFromJsonFile(*this, "duplicate-declarations.json", errorCode);

    // TODO(options):
    // Bad options. The spec is unclear about this
    // -- see https://github.com/unicode-org/message-format-wg/issues/738
    // The current behavior is to set a U_MF_FORMATTING_ERROR for any invalid options.
    runValidTestsFromJsonFile(*this, "invalid-options.json", errorCode);

    runSyntaxTestsWithDiagnosticsFromJsonFile(*this, "syntax-errors-end-of-input.json", errorCode);
    runSyntaxTestsWithDiagnosticsFromJsonFile(*this, "syntax-errors-diagnostics.json", errorCode);
    runSyntaxTestsWithDiagnosticsFromJsonFile(*this, "invalid-number-literals-diagnostics.json", errorCode);
    runSyntaxTestsWithDiagnosticsFromJsonFile(*this, "syntax-errors-diagnostics-multiline.json", errorCode);

    // ICU4J tests
    runFunctionTestsFromJsonFile(*this, "icu-test-functions.json", errorCode);
    runICU4JSyntaxTestsFromJsonFile(*this, "icu-parser-tests.json", errorCode);
    runICU4JSelectionTestsFromJsonFile(*this, "icu-test-selectors.json", errorCode);
    runValidTestsFromJsonFile(*this, "icu-test-previous-release.json", errorCode);
}

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */
