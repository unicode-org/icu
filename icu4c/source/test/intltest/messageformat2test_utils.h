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
    const Hashtable* arguments;

    private:
    const UErrorCode expectedError;
    const UErrorCode expectedWarning;
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

            Formattable* valPtr(new Formattable(val));
            return setArgument(k, valPtr, errorCode);
        }
        Builder& setArgument(const UnicodeString& k, const UnicodeString* val, size_t count, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            LocalArray<Formattable> arr(new Formattable[count]);
            if (!arr.isValid()) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                return *this;
            }
            for (size_t i = 0; i < count; i++) {
                arr[i] = Formattable(val[i]);
            }
            Formattable* valPtr(new Formattable(arr.getAlias(), count));
            return setArgument(k, valPtr, errorCode);
        }
        Builder& setArgument(const UnicodeString& k, double val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            Formattable* valPtr(new Formattable(val));
            return setArgument(k, valPtr, errorCode);
        }
        Builder& setArgument(const UnicodeString& k, long val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            Formattable* valPtr(new Formattable(val));
            return setArgument(k, valPtr, errorCode);
        }
        Builder& setDateArgument(const UnicodeString& k, UDate date, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            Formattable* valPtr(new Formattable(Formattable(date, Formattable::kIsDate)));
            return setArgument(k, valPtr, errorCode);
        }

        // val has to be uniquely owned because the copy constructor for
        // a Formattable of an object doesn't work
        Builder& setArgument(const UnicodeString& k, UObject* val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            Formattable* valPtr(new Formattable(val));
            return setArgument(k, valPtr, errorCode);
        }
        Builder& clearArguments() {
            if (arguments.isValid()) {
                arguments->removeAll();
            };
            return *this;
        }
        Builder& setExpected(UnicodeString e) {
            hasExpectedOutput = true;
            expected = e;
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
        LocalPointer<Hashtable> arguments;
        bool hasExpectedOutput;
        UnicodeString expected;
        UErrorCode expectedError;
        UErrorCode expectedWarning;
        bool hasLineNumberAndOffset;
        uint32_t lineNumber;
        uint32_t offset;
        bool ignoreError;
        LocalPointer<FunctionRegistry> functionRegistry;

        Builder& setArgument(const UnicodeString& k, Formattable* val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            if (!arguments.isValid()) {
                arguments.adoptInstead(initArgs(errorCode));
                THIS_ON_ERROR(errorCode);
            }
            if (val == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            } else {
                arguments->put(k, val, errorCode);
            }
            return *this;
        }

        static Hashtable* initArgs(UErrorCode& errorCode) {
            NULL_ON_ERROR(errorCode);
            Hashtable* result = new Hashtable(uhash_compareUnicodeString, nullptr, errorCode);
            if (result == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
            return result;
        }

        Builder() : pattern(""), hasExpectedOutput(false), expected(""), expectedError(U_ZERO_ERROR), expectedWarning(U_ZERO_ERROR), hasLineNumberAndOffset(false), ignoreError(false) {}
    };

    private:
    TestCase(const Builder& builder, UErrorCode& errorCode) :
        testName(builder.testName),
        pattern(builder.pattern),
        locale(!builder.locale.isValid() ? Locale::getDefault() : *builder.locale),
        arguments(builder.arguments.isValid() ? builder.arguments.getAlias() : Builder::initArgs(errorCode)),
        expectedError(builder.expectedError),
        expectedWarning(builder.expectedWarning),
        hasExpectedOutput(builder.hasExpectedOutput),
        expected(builder.expected),
        hasLineNumberAndOffset(builder.hasLineNumberAndOffset),
        lineNumber(builder.hasLineNumberAndOffset ? builder.lineNumber : 0),
        offset(builder.hasLineNumberAndOffset ? builder.offset : 0),
        ignoreError(builder.ignoreError),
        functionRegistry(builder.functionRegistry.getAlias()) {
        // If an error is not expected, then the expected
        // output should be present
        U_ASSERT(expectFailure() || expectWarning() || hasExpectedOutput);
    }
    public:
    static TestCase::Builder* builder(UErrorCode& errorCode) {
        NULL_ON_ERROR(errorCode);
        Builder* result = new Builder();
        if (result == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
        return result;
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
            // TODO: ??
            errorCode.set(U_ILLEGAL_ARGUMENT_ERROR);
            return;
        }
        errorCode.reset();
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
