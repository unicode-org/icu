// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef _TESTMESSAGEFORMAT2
#define _TESTMESSAGEFORMAT2

#include "unicode/rep.h"
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "messageformat2test_utils.h"
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

struct TestResult {
    const UnicodeString pattern;
    const UnicodeString output;
};

struct TestResultError {
    const UnicodeString pattern;
    const UnicodeString output;
    UErrorCode expected;
};

class TestMessageFormat2: public IntlTest {
public:
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL ) override;

    /** 
     * test MessageFormat2 with various given patterns
     **/
    void testVariousPatterns(void);
    void featureTests(void);
    void messageFormat1Tests(void);
    void testAPICustomFunctions(void);
    // Test custom functions
    void testCustomFunctions(void);
    // Test standard functions
    void testBuiltInFunctions(void);
    void testDataModelErrors(void);
    void testResolutionErrors(void);
    // Test the data model API
    void testAPI(void);
    void testInvalidPatterns(void);
    void testAPISimple(void);

private:
    void testSemanticallyInvalidPattern(uint32_t, const UnicodeString&, UErrorCode);
    void testRuntimeErrorPattern(uint32_t, const UnicodeString&, UErrorCode);
    void testRuntimeWarningPattern(uint32_t, const UnicodeString&, const UnicodeString&, UErrorCode);
    void testInvalidPattern(uint32_t, const UnicodeString&);
    void testInvalidPattern(uint32_t, const UnicodeString&, uint32_t);
    void testInvalidPattern(uint32_t, const UnicodeString&, uint32_t, uint32_t);
    void testValidPatterns(const TestResult*, int32_t, IcuTestErrorCode&);
    void testResolutionErrors(IcuTestErrorCode&);
    void testNoSyntaxErrors(const UnicodeString*, int32_t, IcuTestErrorCode&);
    void jsonTests(IcuTestErrorCode&);
 
    // Built-in function testing
    void testDateTime(IcuTestErrorCode&);
    void testNumbers(IcuTestErrorCode&);

    // Custom function testing
    void testPersonFormatter(IcuTestErrorCode&);
    void testGrammarCasesFormatter(IcuTestErrorCode&);
    void testListFormatter(IcuTestErrorCode&);
    void testMessageRefFormatter(IcuTestErrorCode&);

    // Feature tests
    void testEmptyMessage(TestCase::Builder&, IcuTestErrorCode&);
    void testPlainText(TestCase::Builder&, IcuTestErrorCode&);
    void testPlaceholders(TestCase::Builder&, IcuTestErrorCode&);
    void testArgumentMissing(TestCase::Builder&, IcuTestErrorCode&);
    void testDefaultLocale(TestCase::Builder&, IcuTestErrorCode&);
    void testSpecialPluralWithDecimals(TestCase::Builder&, IcuTestErrorCode&);
    void testDefaultFunctionAndOptions(TestCase::Builder&, IcuTestErrorCode&);
    void testSimpleSelection(TestCase::Builder&, IcuTestErrorCode&);
    void testComplexSelection(TestCase::Builder&, IcuTestErrorCode&);
    void testSimpleLocalVariable(TestCase::Builder&, IcuTestErrorCode&);
    void testLocalVariableWithSelect(TestCase::Builder&, IcuTestErrorCode&);
    void testDateFormat(TestCase::Builder&, IcuTestErrorCode&);
    void testPlural(TestCase::Builder&, IcuTestErrorCode&);

    void testPluralOrdinal(TestCase::Builder&, IcuTestErrorCode&);
    void testFormatterIsCreatedOnce(IcuTestErrorCode&);
    void testPluralWithOffset(TestCase::Builder&, IcuTestErrorCode&);
    void testPluralWithOffsetAndLocalVar(TestCase::Builder&, IcuTestErrorCode&);
    void testDeclareBeforeUse(TestCase::Builder&, IcuTestErrorCode&);
    void testVariableOptionsInSelector(TestCase::Builder&, IcuTestErrorCode&);
    void testVariableOptionsInSelectorWithLocalVar(TestCase::Builder&, IcuTestErrorCode&);

    // MessageFormat 1 tests
    void testSample(TestCase::Builder&, IcuTestErrorCode&);
    void testStaticFormat(TestCase::Builder&, IcuTestErrorCode&);
    void testSimpleFormat(TestCase::Builder&, IcuTestErrorCode&);
    void testSelectFormatToPattern(TestCase::Builder&, IcuTestErrorCode&);
    void testMessageFormatDateTimeSkeleton(TestCase::Builder&, IcuTestErrorCode&);
    void testMf1Behavior(TestCase::Builder&, IcuTestErrorCode&);

}; // class TestMessageFormat2


// Custom function classes
class PersonNameFormatterFactory : public FormatterFactory {
    
    public:
    Formatter* createFormatter(const Locale&, UErrorCode&);
};

class Person : public UObject {
    public:
    UnicodeString title;
    UnicodeString firstName;
    UnicodeString lastName;
    Person(UnicodeString t, UnicodeString f, UnicodeString l) : title(t), firstName(f), lastName(l) {}
    ~Person();
};

class PersonNameFormatter : public Formatter {
    public:
    void format(FormattingContext&, UErrorCode& errorCode) const;
};

class GrammarCasesFormatterFactory : public FormatterFactory {
    public:
    Formatter* createFormatter(const Locale&, UErrorCode&);
};

class GrammarCasesFormatter : public Formatter {
    public:
    void format(FormattingContext&, UErrorCode& errorCode) const;
    static FunctionRegistry* customRegistry(UErrorCode&);
    private:
    void getDativeAndGenitive(const UnicodeString&, UnicodeString& result) const;
};

class ListFormatterFactory : public FormatterFactory {
    public:
    Formatter* createFormatter(const Locale&, UErrorCode&);
};

class ListFormatter : public Formatter {
    public:
    void format(FormattingContext&, UErrorCode& errorCode) const;
    static FunctionRegistry* customRegistry(UErrorCode&);
    private:
    friend class ListFormatterFactory;
    const Locale& locale;
    ListFormatter(const Locale& loc) : locale(loc) {}
};

class ResourceManagerFactory : public FormatterFactory {
    public:
    Formatter* createFormatter(const Locale&, UErrorCode&);
};

class ResourceManager : public Formatter {
    public:
    void format(FormattingContext&, UErrorCode& errorCode) const;
    static FunctionRegistry* customRegistry(UErrorCode&);
    static Hashtable* properties(UErrorCode&);
    static UnicodeString propertiesAsString(const Hashtable&);
    static Hashtable* parseProperties(const UnicodeString&, UErrorCode&);

    private:
    friend class ResourceManagerFactory;
    ResourceManager(const Locale& loc) : locale(loc) {}
    const Locale& locale;
};

class TemperatureFormatterFactory : public FormatterFactory {
    public:
    Formatter* createFormatter(const Locale&, UErrorCode&);
    TemperatureFormatterFactory() : constructCount(0), formatCount(0), fFormatterCount(0), cFormatterCount(0) {}

    int32_t constructCount;
    int32_t formatCount;
    int32_t fFormatterCount;
    int32_t cFormatterCount;
};

class TemperatureFormatter : public Formatter {
    public:
    void format(FormattingContext&, UErrorCode& errorCode) const;
    static FunctionRegistry* customRegistry(UErrorCode&);
    ~TemperatureFormatter();
    private:
    friend class TemperatureFormatterFactory;
    const Locale& locale;
    TemperatureFormatterFactory& counter;
    LocalPointer<Hashtable> cachedFormatters;

    TemperatureFormatter(const Locale&, TemperatureFormatterFactory&, UErrorCode&);
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
