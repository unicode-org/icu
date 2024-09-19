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

static UErrorCode getExpectedRuntimeErrorFromString(const std::string& errorName) {
    if (errorName == "syntax-error") {
        return U_MF_SYNTAX_ERROR;
    }
    if (errorName == "variant-key-mismatch") {
        return U_MF_VARIANT_KEY_MISMATCH_ERROR;
    }
    if (errorName == "missing-fallback-variant") {
        return U_MF_NONEXHAUSTIVE_PATTERN_ERROR;
    }
    if (errorName == "missing-selector-annotation") {
        return U_MF_MISSING_SELECTOR_ANNOTATION_ERROR;
    }
    if (errorName == "unresolved-variable") {
        return U_MF_UNRESOLVED_VARIABLE_ERROR;
    }
    if (errorName == "bad-operand") {
        return U_MF_OPERAND_MISMATCH_ERROR;
    }
    if (errorName == "bad-option") {
        return U_MF_FORMATTING_ERROR;
    }
    if (errorName == "unknown-function") {
        return U_MF_UNKNOWN_FUNCTION_ERROR;
    }
    if (errorName == "duplicate-declaration") {
        return U_MF_DUPLICATE_DECLARATION_ERROR;
    }
    if (errorName == "duplicate-option-name") {
        return U_MF_DUPLICATE_OPTION_NAME_ERROR;
    }
    if (errorName == "duplicate-variant") {
        return U_MF_DUPLICATE_VARIANT_ERROR;
    }
    if (errorName == "bad-selector") {
        return U_MF_SELECTOR_ERROR;
    }
// Arbitrary default
    return U_MF_FORMATTING_ERROR;
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

static bool setArguments(TestMessageFormat2& t,
                         TestCase::Builder& test,
                         const std::vector<json>& params,
                         UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return true;
    }
    bool schemaError = false;
    for (auto argsIter = params.begin(); argsIter != params.end(); ++argsIter) {
        auto j_object = argsIter->template get<json::object_t>();
        if (!j_object["name"].is_null()) {
            const UnicodeString argName = u_str(j_object["name"].template get<std::string>());
            if (!j_object["value"].is_null()) {
                json val = j_object["value"];
                // Determine type of value
                if (val.is_number()) {
                    test.setArgument(argName,
                                     val.template get<double>());
                } else if (val.is_string()) {
                    test.setArgument(argName,
                                     u_str(val.template get<std::string>()));
                } else if (val.is_object()) {
                    // Dates: represent in tests as { "date" : timestamp }, to distinguish
                    // from number values
                    auto obj = val.template get<json::object_t>();
                    if (obj["date"].is_number()) {
                        test.setDateArgument(argName, val["date"]);
                    } else if (obj["decimal"].is_string()) {
                        // Decimal strings: represent in tests as { "decimal" : string },
                        // to distinguish from string values
                        test.setDecimalArgument(argName, obj["decimal"].template get<std::string>(), errorCode);
                    }
                } else if (val.is_boolean() || val.is_null()) {
                    return false; // For now, boolean and null arguments are unsupported
                }
            } else {
               schemaError = true;
               break;
            }
        } else {
            schemaError = true;
            break;
        }
    }
    if (schemaError) {
        t.logln("Warning: test with missing 'name' or 'value' in params");
        if (U_SUCCESS(errorCode)) {
            errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        }
    }
    return true;
}


/*
  Test files are expected to follow the schema in:
  https://github.com/unicode-org/conformance/blob/main/schema/message_fmt2/testgen_schema.json
  as of https://github.com/unicode-org/conformance/pull/255
*/
static void runValidTest(TestMessageFormat2& icuTest,
                         const std::string& testName,
                         const std::string& defaultError,
                         const json& j,
                         IcuTestErrorCode& errorCode) {
    auto j_object = j.template get<json::object_t>();
    std::string messageText;

    // src can be a single string or an array of strings
    if (!j_object["src"].is_null()) {
        if (j_object["src"].is_string()) {
            messageText = j_object["src"].template get<std::string>();
        } else {
            auto strings = j_object["src"].template get<std::vector<std::string>>();
            for (const auto &piece : strings) {
                messageText += piece;
            }
        }
    }
    // Otherwise, it should probably be an error, but we just
    // treat this as the empty string

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
        // `params` is an array of objects
        auto params = j_object["params"].template get<std::vector<json>>();
        if (!setArguments(icuTest, test, params, errorCode)) {
            return; // Skip tests with unsupported arguments
        }
    }

    bool expectedError = false;
    if (!j_object["expErrors"].is_null()) {
        // Map from string to string
        auto errors = j_object["expErrors"].template get<std::vector<std::map<std::string, std::string>>>();
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
        expectedError = true;
    } else if (defaultError.length() > 0) {
        test.setExpectedError(getExpectedRuntimeErrorFromString(defaultError));
        expectedError = true;
    }

    // If no expected result and no error, then set the test builder to expect success
    if (j_object["exp"].is_null() && !expectedError) {
        test.setNoSyntaxError();
    }

    // Check for expected diagnostic values
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
static void runTestsFromJsonFile(TestMessageFormat2& t,
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

    auto j_object = data.template get<json::object_t>();

    // Some files have an expected error
    std::string defaultError;
    if (!j_object["defaultTestProperties"].is_null()
        && !j_object["defaultTestProperties"]["expErrors"].is_null()) {
        auto expErrors = j_object["defaultTestProperties"]["expErrors"];
        // expErrors might also be a boolean, in which case we ignore it --
        // so we have to check if it's an array
        if (expErrors.is_array()) {
            auto expErrorsObj = expErrors.template get<std::vector<json>>();
            if (expErrorsObj.size() > 0) {
                if (!expErrorsObj[0]["type"].is_null()) {
                    defaultError = expErrorsObj[0]["type"].template get<std::string>();
                }
            }
        }
    }

    if (!j_object["tests"].is_null()) {
        auto tests = j_object["tests"].template get<std::vector<json>>();
        for (auto iter = tests.begin(); iter != tests.end(); ++iter) {
            makeTestName(testName, sizeof(testName), fileName, ++testNum);
            t.logln(testName);

            t.logln(u_str(iter->dump()));

            runValidTest(t, testName, defaultError, *iter, errorCode);
        }
    } else {
        // Test doesn't follow schema -- probably an error
        t.logln("Warning: no tests in filename: ");
        t.logln(u_str(fileName));
        (UErrorCode&) errorCode = U_ILLEGAL_ARGUMENT_ERROR;
    }
}

void TestMessageFormat2::jsonTestsFromFiles(IcuTestErrorCode& errorCode) {
    // Spec tests are fairly limited as the spec doesn't dictate formatter
    // output. Tests under testdata/message2/spec are taken from
    // https://github.com/unicode-org/message-format-wg/tree/main/test .
    // Tests directly under testdata/message2 are specific to ICU4C.

    // Do spec tests for syntax errors
    runTestsFromJsonFile(*this, "spec/syntax-errors.json", errorCode);
    runTestsFromJsonFile(*this, "unsupported-expressions.json", errorCode);
    runTestsFromJsonFile(*this, "unsupported-statements.json", errorCode);
    runTestsFromJsonFile(*this, "syntax-errors-reserved.json", errorCode);

    // Do tests for data model errors
    runTestsFromJsonFile(*this, "spec/data-model-errors.json", errorCode);
    runTestsFromJsonFile(*this, "more-data-model-errors.json", errorCode);

    // Do valid spec tests
    runTestsFromJsonFile(*this, "spec/syntax.json", errorCode);

    // Do valid function tests
    runTestsFromJsonFile(*this, "spec/functions/date.json", errorCode);
    runTestsFromJsonFile(*this, "spec/functions/datetime.json", errorCode);
    runTestsFromJsonFile(*this, "spec/functions/integer.json", errorCode);
    runTestsFromJsonFile(*this, "spec/functions/number.json", errorCode);
    runTestsFromJsonFile(*this, "spec/functions/string.json", errorCode);
    runTestsFromJsonFile(*this, "spec/functions/time.json", errorCode);

    // Other tests (non-spec)
    runTestsFromJsonFile(*this, "more-functions.json", errorCode);
    runTestsFromJsonFile(*this, "valid-tests.json", errorCode);
    runTestsFromJsonFile(*this, "resolution-errors.json", errorCode);
    runTestsFromJsonFile(*this, "matches-whitespace.json", errorCode);
    runTestsFromJsonFile(*this, "alias-selector-annotations.json", errorCode);
    runTestsFromJsonFile(*this, "runtime-errors.json", errorCode);

    // Re: the expected output for the first test in this file:
    // Note: the more "correct" fallback output seems like it should be "1.000 3" (ignoring the
    // overriding .input binding of $var2) but that's hard to achieve
    // as so-called "implicit declarations" can only be detected after parsing, at which
    // point the data model can't be modified.
    // Probably this is going to change anyway so that any data model error gets replaced
    // with a fallback for the whole message.
    // The second test has a similar issue with the output.
    runTestsFromJsonFile(*this, "tricky-declarations.json", errorCode);

    // Markup is ignored when formatting to string
    runTestsFromJsonFile(*this, "markup.json", errorCode);

    // TODO(duplicates): currently the expected output is based on using
    // the last definition of the duplicate-declared variable;
    // perhaps it's better to remove all declarations for $foo before formatting.
    // however if https://github.com/unicode-org/message-format-wg/pull/704 lands,
    // it'll be a moot point since the output will be expected to be the fallback string
    // (This applies to the expected output for all the U_DUPLICATE_DECLARATION_ERROR tests)
    runTestsFromJsonFile(*this, "duplicate-declarations.json", errorCode);

    // TODO(options):
    // Bad options. The spec is unclear about this
    // -- see https://github.com/unicode-org/message-format-wg/issues/738
    // The current behavior is to set a U_MF_FORMATTING_ERROR for any invalid options.
    runTestsFromJsonFile(*this, "invalid-options.json", errorCode);

    runTestsFromJsonFile(*this, "syntax-errors-end-of-input.json", errorCode);
    runTestsFromJsonFile(*this, "syntax-errors-diagnostics.json", errorCode);
    runTestsFromJsonFile(*this, "invalid-number-literals-diagnostics.json", errorCode);
    runTestsFromJsonFile(*this, "syntax-errors-diagnostics-multiline.json", errorCode);

    // ICU4J tests
    runTestsFromJsonFile(*this, "icu-test-functions.json", errorCode);
    runTestsFromJsonFile(*this, "icu-parser-tests.json", errorCode);
    runTestsFromJsonFile(*this, "icu-test-selectors.json", errorCode);
    runTestsFromJsonFile(*this, "icu-test-previous-release.json", errorCode);
}

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */
