// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef _TESTMESSAGEFORMAT2
#define _TESTMESSAGEFORMAT2

#include "unicode/rep.h"
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_function_registry.h"
#include "unicode/messageformat2.h"
#include "unicode/unistr.h"
#include "unicode/fmtable.h"
#include "unicode/parseerr.h"
#include "intltest.h"

/**
 * TestMessageFormat2 tests MessageFormat2
 */

U_NAMESPACE_BEGIN namespace message2 {

class Args {
    public:
    static Hashtable* of(UnicodeString name, UnicodeString value, UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        LocalPointer<Hashtable> result(newHashtable(errorCode));
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        addWithCopy(result.getAlias(), name, value, errorCode);
        return result.orphan();
    }
    static Hashtable* of(UnicodeString name1, UnicodeString value1,
                         UnicodeString name2, UnicodeString value2,
                         UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        LocalPointer<Hashtable> result(newHashtable(errorCode));
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        addWithCopy(result.getAlias(), name1, value1, errorCode);
        addWithCopy(result.getAlias(), name2, value2, errorCode);
        return result.orphan();
    }
    static Hashtable* of(UnicodeString name1, UnicodeString value1,
                         UnicodeString name2, UnicodeString value2,
                         UnicodeString name3, UnicodeString value3,
                         UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        LocalPointer<Hashtable> result(newHashtable(errorCode));
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        addWithCopy(result.getAlias(), name1, value1, errorCode);
        addWithCopy(result.getAlias(), name2, value2, errorCode);
        addWithCopy(result.getAlias(), name3, value3, errorCode);
        return result.orphan();
    }
    private:
    static void addWithCopy(Hashtable* result,
                            UnicodeString name,
                            UnicodeString value,
                            UErrorCode& errorCode) {
        CHECK_ERROR(errorCode);
        LocalPointer<UnicodeString> val(new UnicodeString(value));
        if (!val.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        result->put(name, val.orphan(), errorCode);
    }
    static Hashtable* newHashtable(UErrorCode& errorCode) {
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        LocalPointer<Hashtable> result(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
        if (U_FAILURE(errorCode)) {
            return nullptr;
        }
        return result.orphan();
    }
};

class TestMessageFormat2: public IntlTest {
public:
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL ) override;

    /** 
     * test MessageFormat2 with various given patterns
     **/
    void testStaticFormat2(void);
    void testComplexMessage(void);
    void testValidPatterns(void);
    void testValidJsonPatterns(void);
    void testInvalidPatterns(void);
    void testDataModelErrors(void);
    void testResolutionErrors(void);
    // Test the data model API
    void testAPI(void);
    void testAPISimple(void);
    void testAPICustomFunctions(void);
    // Test standard functions
    void testBuiltInFunctions(void);
    // Test custom functions
    void testCustomFunctions(void);

private:
    void testMessageFormatter(const UnicodeString&, UParseError&, UErrorCode&);
    void testMessageFormatting(const UnicodeString&, UParseError&, UErrorCode&);
    void testMessageFormatting(const UnicodeString&, UParseError&, UnicodeString&, UErrorCode&);
    void testPattern(const UnicodeString&, uint32_t, const char*);
    template<size_t N>
    void testPatterns(const UnicodeString(&) [N], const char*);
    void testSemanticallyInvalidPattern(uint32_t, const UnicodeString&, UErrorCode);
    void testRuntimeErrorPattern(uint32_t, const UnicodeString&, UErrorCode);
    void testRuntimeWarningPattern(uint32_t, const UnicodeString&, const UnicodeString&, UErrorCode);
    void testInvalidPattern(uint32_t, const UnicodeString&);
    void testInvalidPattern(uint32_t, const UnicodeString&, uint32_t);
    void testInvalidPattern(uint32_t, const UnicodeString&, uint32_t, uint32_t);
    void runTest(const MessageFormatter&, const UnicodeString&, const UnicodeString&, const UnicodeString&, const UnicodeString&, UErrorCode&);
    void runTest(const MessageFormatter&, const UnicodeString& testName, const UnicodeString& expected, const UnicodeString& name1, const UnicodeString& value1, const UnicodeString& name2, const UnicodeString& value2, UErrorCode&);
    void runTest(const MessageFormatter&, const UnicodeString& testName, const UnicodeString& expected, const Hashtable& args, UErrorCode&);

    void jsonTests(IcuTestErrorCode&);
 
    // Built-in function testing
    void testDateTime(IcuTestErrorCode&);
    void testNumbers(IcuTestErrorCode&);

    // Custom function testing
    void checkResult(const UnicodeString&, const UnicodeString&, const UnicodeString&, const UnicodeString&, IcuTestErrorCode&, UErrorCode);
    void testWithPatternAndArguments(const UnicodeString& testName, FunctionRegistry*, const UnicodeString& pattern, const UnicodeString& argName, const UnicodeString& argValue, const UnicodeString& expected, Locale, IcuTestErrorCode&, UErrorCode);
    void testWithPatternAndArguments(const UnicodeString& testName, FunctionRegistry*, const UnicodeString& pattern, const UnicodeString& argName, const UnicodeString& argValue, const UnicodeString& expected, Locale, IcuTestErrorCode&);
    void testWithPatternAndArguments(const UnicodeString&, FunctionRegistry*, const UnicodeString& pattern, const UnicodeString& argName1, const UnicodeString& argValue1, const UnicodeString& argName2, const UnicodeString& argValue2, const UnicodeString& expected, Locale, IcuTestErrorCode&);
    void testWithPatternAndArguments(const UnicodeString&, FunctionRegistry*, const UnicodeString& pattern, const Hashtable&, const UnicodeString& expected, Locale, IcuTestErrorCode&, UErrorCode);
    void testPersonFormatter(IcuTestErrorCode&);
    void testGrammarCasesFormatter(IcuTestErrorCode&);
    void testListFormatter(IcuTestErrorCode&);
    void testMessageRefFormatter(IcuTestErrorCode&);
}; // class TestMessageFormat2

class TestCase : public UMemory {
    public:
    const UnicodeString testName;
    const UnicodeString pattern;
    const Locale locale;
    const LocalPointer<Hashtable> arguments;

    private:
    const UErrorCode expectedError;
    const UErrorCode expectedWarning;
    const bool hasExpectedOutput;
    const UnicodeString& expected;
    const bool hasLineNumberAndOffset;
    const uint32_t lineNumber;
    const uint32_t offset;
    const bool ignoreError;

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
    private:
    friend class TestMessageFormat2;


    class Builder : public UMemory {
        friend class TestCase;

        public:
        Builder& setName(UnicodeString name) { testName = name; return *this; }
        Builder& setPattern(UnicodeString pat) { pattern = pat; return *this; }
        Builder& setArgument(const UnicodeString& k, const UnicodeString& val, UErrorCode& errorCode) {
            THIS_ON_ERROR(errorCode);

            if (!arguments.isValid()) {
                initArgs(errorCode);
                THIS_ON_ERROR(errorCode);
            }
            UnicodeString* valPtr = new UnicodeString(val);
            if (valPtr == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            } else {
                arguments->put(k, valPtr, errorCode);
            }
            return *this;
        }
        Builder& setArguments(Hashtable* args) { arguments.adoptInstead(args); return *this; }
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
        TestCase* build(UErrorCode& errorCode) {
            NULL_ON_ERROR(errorCode);
            if (!arguments.isValid()) {
                // Initialize to empty hash
                initArgs(errorCode);
                NULL_ON_ERROR(errorCode);
            }
            TestCase* result = new TestCase(*this);
            if (result == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
            }
            return result;
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

        void initArgs(UErrorCode& errorCode) {
            CHECK_ERROR(errorCode);
            arguments.adoptInstead(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
        }

        Builder() : pattern(""), hasExpectedOutput(false), expected(""), expectedError(U_ZERO_ERROR), expectedWarning(U_ZERO_ERROR), hasLineNumberAndOffset(false), ignoreError(false) {}
    };

    TestCase(Builder& builder) :
        testName(builder.testName),
        pattern(builder.pattern),
        locale(!builder.locale.isValid() ? Locale::getDefault() : *builder.locale),
        arguments(builder.arguments.orphan()),
        expectedError(builder.expectedError),
        expectedWarning(builder.expectedWarning),
        hasExpectedOutput(builder.hasExpectedOutput),
        expected(builder.expected),
        hasLineNumberAndOffset(builder.hasLineNumberAndOffset),
        lineNumber(builder.hasLineNumberAndOffset ? builder.lineNumber : 0),
        offset(builder.hasLineNumberAndOffset ? builder.offset : 0),
        ignoreError(builder.ignoreError) {
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

    // Runs a single test case with no custom function registry
    static void runTestCase(TestMessageFormat2& tmsg,
                            TestCase& testCase,
                            IcuTestErrorCode& errorCode) {
        runTestCase(tmsg, nullptr, testCase, errorCode);
    }

    // Runs a single test case
    static void runTestCase(TestMessageFormat2& tmsg,
                            FunctionRegistry* customRegistry,
                            TestCase& testCase,
                            IcuTestErrorCode& errorCode) {
        CHECK_ERROR(errorCode);

        LocalPointer<MessageFormatter::Builder> mfBuilder(MessageFormatter::builder(errorCode));
        CHECK_ERROR(errorCode);
        mfBuilder->setPattern(testCase.pattern, errorCode)
            .setLocale(testCase.locale);

        if (customRegistry != nullptr) {
            mfBuilder->setFunctionRegistry(customRegistry);
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

    static void failExpectedSuccess(TestMessageFormat2& tmsg, const TestCase& testCase, IcuTestErrorCode& errorCode) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " failed test with pattern: " + testCase.pattern + " and error code " + ((int32_t) errorCode));
        errorCode.reset();
    }
    static void failExpectedFailure(TestMessageFormat2& tmsg, const TestCase& testCase, IcuTestErrorCode& errorCode) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " failed test with wrong error code; pattern: " + testCase.pattern + " and error code " + ((int32_t) errorCode) + "(expected error code: " + ((int32_t) testCase.expectedErrorCode()) + " )");
        errorCode.reset();
    }
    static void failExpectedWarning(TestMessageFormat2& tmsg, const TestCase& testCase, IcuTestErrorCode& errorCode) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " was expected to pass with a warning; failed test with wrong error code; pattern: " + testCase.pattern + " and error code " + ((int32_t) errorCode) + "(expected warning code: " + ((int32_t) testCase.expectedWarningCode()) + " )");
        errorCode.reset();
    }

    static void failWrongOutput(TestMessageFormat2& tmsg, const TestCase& testCase, const UnicodeString& result) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " failed test with wrong output; pattern: " + testCase.pattern + " and expected output = " + testCase.expectedOutput() + " and actual output = " + result);
    }

    static void failWrongOffset(TestMessageFormat2& tmsg, const TestCase& testCase, uint32_t actualLine, uint32_t actualOffset) {
        tmsg.dataerrln("Test failed with wrong line or character offset in parse error; expected (line %d, offset %d), got (line %d, offset %d)", testCase.getLineNumber(), testCase.getOffset(),
                  actualLine, actualOffset);
        tmsg.logln(UnicodeString(testCase.testName) + " pattern = " + testCase.pattern + " - failed by returning the wrong line number or offset in the parse error");
    }
}; // class TestUtils

// Custom function classes
class PersonNameFormatterFactory : public FormatterFactory {
    
    public:
    Formatter* createFormatter(Locale, const Hashtable&, UErrorCode&) const;
};

class PersonNameFormatter : public Formatter {
    public:
    void format(const UnicodeString&, const Hashtable&, UnicodeString&, UErrorCode& errorCode) const;
};

class GrammarCasesFormatterFactory : public FormatterFactory {
    public:
    Formatter* createFormatter(Locale, const Hashtable&, UErrorCode&) const;
};

class GrammarCasesFormatter : public Formatter {
    public:
    void format(const UnicodeString&, const Hashtable&, UnicodeString&, UErrorCode& errorCode) const;
    static FunctionRegistry* customRegistry(UErrorCode&);
    private:
    void getDativeAndGenitive(const UnicodeString&, UnicodeString& result) const;
};

class ListFormatterFactory : public FormatterFactory {
    public:
    Formatter* createFormatter(Locale, const Hashtable&, UErrorCode&) const;
};

class ListFormatter : public Formatter {
    public:
    void format(const UnicodeString&, const Hashtable&, UnicodeString&, UErrorCode& errorCode) const;
    static FunctionRegistry* customRegistry(UErrorCode&);
    private:
    friend class ListFormatterFactory;
    Locale locale;
    ListFormatter(Locale loc) : locale(loc) {}
};

class ResourceManagerFactory : public FormatterFactory {
    public:
    Formatter* createFormatter(Locale, const Hashtable&, UErrorCode&) const;
};

class ResourceManager : public Formatter {
    public:
    void format(const UnicodeString&, const Hashtable&, UnicodeString&, UErrorCode& errorCode) const;
    static FunctionRegistry* customRegistry(UErrorCode&);
    static Hashtable* properties(UErrorCode&);
    static UnicodeString propertiesAsString(const Hashtable&);
    static Hashtable* parseProperties(const UnicodeString&, UErrorCode&);
    private:
    friend class ResourceManagerFactory;

    const Hashtable& fixedOptions;
    ResourceManager(const Hashtable& opts) : fixedOptions(opts) {} 
};

// Custom function test utilities
class SplitString {
    public:
    static const uint32_t FIRST = 0;
    static const uint32_t LAST = -1;
    static bool nextPart(const UnicodeString&, UnicodeString&, uint32_t&);
};

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
