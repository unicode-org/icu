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
    void featureTests(void);
    void messageFormat1Tests(void);
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
    void testWithPatternAndArguments(const UnicodeString& testName, FunctionRegistry*, const UnicodeString& pattern, const UnicodeString& argName, Formattable* argValue, const UnicodeString& expected, Locale, IcuTestErrorCode&, UErrorCode);
    void testWithPatternAndArguments(const UnicodeString& testName, FunctionRegistry*, const UnicodeString& pattern, const UnicodeString& argName, Formattable* argValue, const UnicodeString& expected, Locale, IcuTestErrorCode&);
    void testWithPatternAndArguments(const UnicodeString& testName, FunctionRegistry*, const UnicodeString& pattern, const UnicodeString& argName, double argValue, const UnicodeString& expected, Locale, IcuTestErrorCode&);
    void testWithPatternAndArguments(const UnicodeString&, FunctionRegistry*, const UnicodeString& pattern, const UnicodeString& argName1, Formattable* argValue1, const UnicodeString& argName2, Formattable* argValue2, const UnicodeString& expected, Locale, IcuTestErrorCode&);
    void testWithPatternAndArguments(const UnicodeString&, FunctionRegistry*, const UnicodeString& pattern, const Hashtable&, const UnicodeString& expected, Locale, IcuTestErrorCode&, UErrorCode);
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
    TemperatureFormatterFactory() {}

    size_t constructCount;
    size_t formatCount;
    size_t fFormatterCount;
    size_t cFormatterCount;
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
