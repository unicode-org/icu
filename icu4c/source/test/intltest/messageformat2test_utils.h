#ifndef _TESTMESSAGEFORMAT2_UTILS
#define _TESTMESSAGEFORMAT2_UTILS

#include "unicode/messageformat2_function_registry.h"
#include "unicode/messageformat2_macros.h"
#include "unicode/messageformat2.h"
#include "intltest.h"

#if !UCONFIG_NO_FORMATTING

U_NAMESPACE_BEGIN namespace message2 {

class TestCase : public UMemory {
    public:
    const UnicodeString testName;
    const UnicodeString pattern;
    const Locale locale;
    // Not owned by this!
    const MessageArguments* arguments;

    private:
    const UErrorCode expectedError;
    const UErrorCode expectedWarning;
    const bool expectedNoSyntaxError;
    const bool hasExpectedOutput;
    const UnicodeString& expected;
    const bool hasLineNumberAndOffset;
    const uint32_t lineNumber;
    const uint32_t offset;
    const bool ignoreError;
    // Function registry is not owned by the TestCase object
    const FunctionRegistry* functionRegistry;

    public:
    bool expectSuccess() const {
        return (!ignoreError && U_SUCCESS(expectedError));
    }
    bool expectFailure() const {
        return (!ignoreError && U_FAILURE(expectedError));
    }
    bool expectWarning() const {
        return (!ignoreError && (expectedWarning != U_ZERO_ERROR));
    }
    bool expectNoSyntaxError() const {
        return expectedNoSyntaxError;
    }
    UErrorCode expectedErrorCode() const {
        U_ASSERT(!expectSuccess());
        return expectedError;
    }
    UErrorCode expectedWarningCode() const {
        U_ASSERT(expectWarning());
        return expectedWarning;
    }
    bool lineNumberAndOffsetMatch(uint32_t actualLine, uint32_t actualOffset) const {
        return (!hasLineNumberAndOffset ||
                ((actualLine == lineNumber) && actualOffset == offset));
    }
    bool outputMatches(const UnicodeString& result) const {
        return (!hasExpectedOutput || (expected == result));
    }
    const UnicodeString& expectedOutput() const {
        U_ASSERT(hasExpectedOutput);
        return expected;
    }
    uint32_t getLineNumber() const {
        U_ASSERT(hasLineNumberAndOffset);
        return lineNumber;
    }
    uint32_t getOffset() const {
        U_ASSERT(hasLineNumberAndOffset);
        return offset;
    }
    bool hasCustomRegistry() const { return functionRegistry != nullptr; }
    const FunctionRegistry* getCustomRegistry() const {
        U_ASSERT(hasCustomRegistry());
        return functionRegistry;
    }
 
    class Builder : public UMemory {
        friend class TestCase;

        public:
        Builder& setName(UnicodeString name) { testName = name; return *this; }
        Builder& setPattern(UnicodeString pat) { pattern = pat; return *this; }
        Builder& setArgument(const UnicodeString& k, const UnicodeString& val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);
            arguments->add(k, val, errorCode);
            return *this;
        }
        Builder& setArgument(const UnicodeString& k, const UnicodeString* val, int32_t count, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);
            U_ASSERT(val != nullptr);
            arguments->add(k, val, count, errorCode);
            return *this;
        }
        Builder& setArgument(const UnicodeString& k, double val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            arguments->addDouble(k, val, errorCode);
            return *this;
        }
        Builder& setArgument(const UnicodeString& k, int64_t val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            arguments->addInt64(k, val, errorCode);
            return *this;
        }
        Builder& setDateArgument(const UnicodeString& k, UDate date, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            arguments->addDate(k, date, errorCode);
            return *this;
        }

        // val has to be uniquely owned because the copy constructor for
        // a Formattable of an object doesn't work
        Builder& setArgument(const UnicodeString& k, UObject* val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);
            U_ASSERT(val != nullptr);

            arguments->addObject(k, val, errorCode);
            return *this;
        }
        Builder& clearArguments(UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);
            if (arguments.isValid()) {
                arguments.adoptInstead(MessageArguments::builder(errorCode));
            };
            return *this;
        }
        Builder& setExpected(UnicodeString e) {
            hasExpectedOutput = true;
            expected = e;
            return *this;
        }
        Builder& clearExpected() {
            hasExpectedOutput = false;
            return *this;
        }
        Builder& setExpectedError(UErrorCode errorCode) {
            expectedError = U_SUCCESS(errorCode) ? U_ZERO_ERROR : errorCode;
            return *this;
        }
        Builder& setExpectedWarning(UErrorCode errorCode) {
            expectedWarning = errorCode;
            return *this;
        }
        Builder& setNoSyntaxError() {
            expectNoSyntaxError = true;
            return *this;
        }
        Builder& setExpectSuccess() {
            return setExpectedWarning(U_ZERO_ERROR)
                  .setExpectedError(U_ZERO_ERROR);
        }
        Builder& clearLocale() {
            if (locale.isValid()) {
                locale.orphan();
            }
            return *this;
        }

        Builder& setLocale(Locale loc, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);
            Locale* l = new Locale(loc);
            if (l == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            } else {
                locale.adoptInstead(l);
            }
            return *this;
        }
        Builder& setExpectedLineNumberAndOffset(uint32_t line, uint32_t o) {
            hasLineNumberAndOffset = true;
            lineNumber = line;
            offset = o;
            return *this;
        }
        Builder& setIgnoreError() {
            ignoreError = true;
            return *this;
        }
        Builder& clearIgnoreError() {
            ignoreError = false;
            return *this;
        }
        Builder& setFunctionRegistry(FunctionRegistry* reg) {
            U_ASSERT(reg != nullptr);
            functionRegistry.adoptInstead(reg);
            return *this;
        }
        TestCase* build(UErrorCode& errorCode) const {
            NULL_ON_ERROR(errorCode);
            LocalPointer<TestCase> result(new TestCase(*this, errorCode));
            NULL_ON_ERROR(errorCode);
            return result.orphan();
        }

        private:
        UnicodeString testName;
        UnicodeString pattern;
        LocalPointer<Locale> locale;
        LocalPointer<MessageArguments::Builder> arguments;
        bool hasExpectedOutput;
        UnicodeString expected;
        UErrorCode expectedError;
        UErrorCode expectedWarning;
        bool expectNoSyntaxError;
        bool hasLineNumberAndOffset;
        uint32_t lineNumber;
        uint32_t offset;
        bool ignoreError;
        LocalPointer<FunctionRegistry> functionRegistry;

        Builder(UErrorCode& errorCode) : pattern(""), arguments(MessageArguments::builder(errorCode)), hasExpectedOutput(false), expected(""), expectedError(U_ZERO_ERROR), expectedWarning(U_ZERO_ERROR), expectNoSyntaxError(false), hasLineNumberAndOffset(false), ignoreError(false) {}
    };

    private:
    TestCase(const Builder& builder, UErrorCode& errorCode) :
        testName(builder.testName),
        pattern(builder.pattern),
        locale(!builder.locale.isValid() ? Locale::getDefault() : *builder.locale),
        arguments(builder.arguments->build(errorCode)),
        expectedError(builder.expectedError),
        expectedWarning(builder.expectedWarning),
        expectedNoSyntaxError(builder.expectNoSyntaxError),
        hasExpectedOutput(builder.hasExpectedOutput),
        expected(builder.expected),
        hasLineNumberAndOffset(builder.hasLineNumberAndOffset),
        lineNumber(builder.hasLineNumberAndOffset ? builder.lineNumber : 0),
        offset(builder.hasLineNumberAndOffset ? builder.offset : 0),
        ignoreError(builder.ignoreError),
        functionRegistry(builder.functionRegistry.getAlias()) {
        U_ASSERT(builder.arguments.isValid());
        // If an error is not expected, then the expected
        // output should be present
        U_ASSERT(expectFailure() || expectWarning() || expectNoSyntaxError() || hasExpectedOutput);
    }
    public:
    static TestCase::Builder* builder(UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        return new Builder(errorCode);
    }
}; // class TestCase

class TestUtils {
    public:

    // Runs a single test case
    static void runTestCase(IntlTest& tmsg,
                            const TestCase& testCase,
                            IcuTestErrorCode& errorCode) {
        CHECK_ERROR(errorCode);

        LocalPointer<MessageFormatter::Builder> mfBuilder(MessageFormatter::builder(errorCode));
        CHECK_ERROR(errorCode);
        mfBuilder->setPattern(testCase.pattern, errorCode)
            .setLocale(testCase.locale);

        if (testCase.hasCustomRegistry()) {
            mfBuilder->setFunctionRegistry(testCase.getCustomRegistry());
        }
        UParseError parseError;
        LocalPointer<MessageFormatter> mf(mfBuilder->build(parseError, errorCode));
        UnicodeString result;

        if (U_SUCCESS(errorCode)) {
            mf->formatToString(*(testCase.arguments), errorCode, result);
        }

        if (testCase.expectNoSyntaxError()) {
            if (errorCode == U_SYNTAX_WARNING) {
                failSyntaxError(tmsg, testCase);
            }
            errorCode.reset();
            return;
        }
        if ((testCase.expectSuccess() || testCase.expectWarning()) && U_FAILURE(errorCode)) {
            failExpectedSuccess(tmsg, testCase, errorCode);
            return;
        }
        if (testCase.expectWarning() && errorCode != testCase.expectedWarningCode()) {
            failExpectedWarning(tmsg, testCase, errorCode);
            return;
        }
        if (testCase.expectFailure() && errorCode != testCase.expectedErrorCode()) {
            failExpectedFailure(tmsg, testCase, errorCode);
            return;
        }
        if (!testCase.lineNumberAndOffsetMatch(parseError.line, parseError.offset)) {
            failWrongOffset(tmsg, testCase, parseError.line, parseError.offset);
        }
        if (!testCase.outputMatches(result)) {
            failWrongOutput(tmsg, testCase, result);
            return;
        }
        errorCode.reset();
    }

    static void failSyntaxError(IntlTest& tmsg, const TestCase& testCase) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " failed test with pattern: " + testCase.pattern + " and error code U_SYNTAX_WARNING; expected no syntax error");
    }

    static void failExpectedSuccess(IntlTest& tmsg, const TestCase& testCase, IcuTestErrorCode& errorCode) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " failed test with pattern: " + testCase.pattern + " and error code " + ((int32_t) errorCode));
        errorCode.reset();
    }
    static void failExpectedFailure(IntlTest& tmsg, const TestCase& testCase, IcuTestErrorCode& errorCode) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " failed test with wrong error code; pattern: " + testCase.pattern + " and error code " + ((int32_t) errorCode) + "(expected error code: " + ((int32_t) testCase.expectedErrorCode()) + " )");
        errorCode.reset();
    }
    static void failExpectedWarning(IntlTest& tmsg, const TestCase& testCase, IcuTestErrorCode& errorCode) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " was expected to pass with a warning; failed test with wrong error code; pattern: " + testCase.pattern + " and error code " + ((int32_t) errorCode) + "(expected warning code: " + ((int32_t) testCase.expectedWarningCode()) + " )");
        errorCode.reset();
    }

    static void failWrongOutput(IntlTest& tmsg, const TestCase& testCase, const UnicodeString& result) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " failed test with wrong output; pattern: " + testCase.pattern + " and expected output = " + testCase.expectedOutput() + " and actual output = " + result);
    }

    static void failWrongOffset(IntlTest& tmsg, const TestCase& testCase, uint32_t actualLine, uint32_t actualOffset) {
        tmsg.dataerrln("Test failed with wrong line or character offset in parse error; expected (line %d, offset %d), got (line %d, offset %d)", testCase.getLineNumber(), testCase.getOffset(),
                  actualLine, actualOffset);
        tmsg.logln(UnicodeString(testCase.testName) + " pattern = " + testCase.pattern + " - failed by returning the wrong line number or offset in the parse error");
    }
}; // class TestUtils

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
