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
    const UnicodeString& expected;

    bool expectSuccess() const { return U_SUCCESS(expectedError); }
    bool expectedErrorCode() const {
        U_ASSERT(!expectSuccess());
        return expectedError;
    }
    private:
    friend class TestMessageFormat2;

    const UErrorCode expectedError;

    class Builder : public UMemory {
        friend class TestCase;

        public:
        Builder& setName(UnicodeString name) { testName = name; return *this; }
        Builder& setPattern(UnicodeString pat) { pattern = pat; return *this; }
        Builder& setArguments(Hashtable* args) { arguments.adoptInstead(args); return *this; }
        Builder& setExpected(UnicodeString e) { expected = e; return *this; }
        Builder& setExpectedError(UErrorCode errorCode) {
            expectedError = U_SUCCESS(errorCode) ? U_ZERO_ERROR : errorCode;
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
        TestCase* build(UErrorCode& errorCode) {
            NULL_ON_ERROR(errorCode);
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
        UnicodeString expected;
        UErrorCode expectedError;

        Builder() : pattern(""), expected(""), expectedError(U_ZERO_ERROR) {
        }
    };

    TestCase(Builder& builder) :
        testName(builder.testName),
        pattern(builder.pattern),
        locale(!builder.locale.isValid() ? Locale::getDefault() : *builder.locale),
        arguments(builder.arguments.orphan()),
        expected(builder.expected),
        expectedError(builder.expectedError) {
        // If an error is not expected, then the expected
        // output should be non-empty
        U_ASSERT(U_FAILURE(expectedError) || expected != "");
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
        UParseError parseErr;
        LocalPointer<MessageFormatter> mf(mfBuilder->build(parseErr, errorCode));

        UnicodeString result;
        mf->formatToString(*(testCase.arguments), errorCode, result);
        if (testCase.expectSuccess() && U_FAILURE(errorCode)) {
            failExpectedSuccess(tmsg,testCase, errorCode);
            return;
        }
        if (!testCase.expectSuccess() && errorCode != testCase.expectedErrorCode()) {
            failExpectedFailure(tmsg, testCase, errorCode);
            return;
        }
        if (result != testCase.expected) {
            failWrongOutput(tmsg, testCase, result);
            errorCode.set(U_MESSAGE_PARSE_ERROR);
        }

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

    static void failWrongOutput(TestMessageFormat2& tmsg, const TestCase& testCase, const UnicodeString& result) {
        tmsg.dataerrln(testCase.testName);
        tmsg.logln(testCase.testName + " failed test with wrong output; pattern: " + testCase.pattern + " and expected output = " + testCase.expected + " and actual output = " + result);
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
