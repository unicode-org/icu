// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef _TESTMESSAGEFORMAT2
#define _TESTMESSAGEFORMAT2

#include "unicode/rep.h"
#include "unicode/utypes.h"

#if !UCONFIG_NO_NORMALIZATION

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "messageformat2test_utils.h"
#include "unicode/unistr.h"
#include "unicode/messageformat2_formattable.h"
#include "unicode/parseerr.h"
#include "intltest.h"

/**
 * TestMessageFormat2 tests MessageFormat2
 */

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
    void dataDrivenTests(void);
    void testAPICustomFunctions(void);
    // Test custom functions
    void testCustomFunctions(void);
    // Test the data model API
    void testDataModelAPI(void);
    // Test the formatting API
    void testFormatterAPI(void);
    // Test API functions for setting bidi preferences
    void testBidiAPI(void);
    void testAPI(void);
    void testAPISimple(void);

private:
    void jsonTestsFromFiles(IcuTestErrorCode&);

    // Built-in function testing
    void testNumbers(IcuTestErrorCode&);

    // Custom function testing
    void testPersonFormatter(IcuTestErrorCode&);
    void testCustomFunctionsComplexMessage(IcuTestErrorCode&);
    void testGrammarCasesFormatter(IcuTestErrorCode&);
    void testListFormatter(IcuTestErrorCode&);
    void testMessageRefFormatter(IcuTestErrorCode&);
    void testComplexOptions(IcuTestErrorCode&);
    void testSingleEvaluation(IcuTestErrorCode&);

    // Feature tests
    void testEmptyMessage(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testPlainText(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testPlaceholders(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testArgumentMissing(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testDefaultLocale(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testSpecialPluralWithDecimals(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testDefaultFunctionAndOptions(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testSimpleSelection(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testComplexSelection(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testSimpleLocalVariable(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testLocalVariableWithSelect(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testDateFormat(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testPlural(message2::TestCase::Builder&, IcuTestErrorCode&);

    void testPluralOrdinal(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testDeclareBeforeUse(message2::TestCase::Builder&, IcuTestErrorCode&);

    // MessageFormat 1 tests
    void testSample(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testStaticFormat(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testSimpleFormat(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testSelectFormatToPattern(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testMessageFormatDateTimeSkeleton(message2::TestCase::Builder&, IcuTestErrorCode&);
    void testMf1Behavior(message2::TestCase::Builder&, IcuTestErrorCode&);

    void testHighLoneSurrogate(void);
    void testLowLoneSurrogate(void);
    void testLoneSurrogateInQuotedLiteral(void);
}; // class TestMessageFormat2

U_NAMESPACE_BEGIN

namespace message2 {

// Custom function classes

class Person : public FormattableObject {
    public:
    UnicodeString title;
    UnicodeString firstName;
    UnicodeString lastName;
    Person(UnicodeString t, UnicodeString f, UnicodeString l) : title(t), firstName(f), lastName(l), tagName("person") {}
    ~Person();
    const UnicodeString& tag() const override { return tagName; }
    private:
    const UnicodeString tagName;
};

class PersonNameFunction : public Function {
    public:
    LocalPointer<FunctionValue> call(const FunctionContext&,
                                     const FunctionValue&,
                                     const FunctionOptions&,
                                     UErrorCode&) override;
    virtual ~PersonNameFunction();
    PersonNameFunction() {}
};

class PersonNameValue : public FunctionValue {
    public:
    UnicodeString formatToString(UErrorCode&) const override;
    PersonNameValue();
    virtual ~PersonNameValue();
    private:
    friend class PersonNameFunction;

    UnicodeString formattedString;
    PersonNameValue(const FunctionValue&, const FunctionOptions&, UErrorCode&);
}; // class PersonNameValue

class FormattableProperties : public FormattableObject {
    public:
    const UnicodeString& tag() const override { return tagName; }
    FormattableProperties(Hashtable* hash) : properties(hash), tagName("properties") {
        U_ASSERT(hash != nullptr);
    }
    ~FormattableProperties();
    LocalPointer<Hashtable> properties;
private:
    const UnicodeString tagName;
};

class GrammarCasesFunction : public Function {
    public:
    LocalPointer<FunctionValue> call(const FunctionContext&, const FunctionValue&, const FunctionOptions&, UErrorCode&) override;
    static MFFunctionRegistry customRegistry(UErrorCode&);
};

class GrammarCasesValue : public FunctionValue {
    public:
    UnicodeString formatToString(UErrorCode&) const override;
    GrammarCasesValue();
    virtual ~GrammarCasesValue();
    private:
    friend class GrammarCasesFunction;

    UnicodeString formattedString;
    GrammarCasesValue(const FunctionValue&, const FunctionOptions&, UErrorCode&);
    void getDativeAndGenitive(const UnicodeString&, UnicodeString& result) const;
}; // class GrammarCasesValue

class ListFunction : public Function {
    public:
    LocalPointer<FunctionValue> call(const FunctionContext&, const FunctionValue&, const FunctionOptions&, UErrorCode&) override;
    static MFFunctionRegistry customRegistry(UErrorCode&);
    ListFunction() {}
    virtual ~ListFunction();
};

class ListValue : public FunctionValue {
    public:
    UnicodeString formatToString(UErrorCode&) const override;
    virtual ~ListValue();
    private:
    friend class ListFunction;

    UnicodeString formattedString;
    ListValue(const Locale&,
              const FunctionValue&,
              const FunctionOptions&,
              UErrorCode&);
}; // class ListValue

class NounValue : public FunctionValue {
    public:
    UnicodeString formatToString(UErrorCode&) const override;
    NounValue();
    virtual ~NounValue();
    private:
    friend class NounFunction;

    UnicodeString formattedString;
    NounValue(const FunctionValue&,
              const FunctionOptions&,
              UErrorCode&);
}; // class NounValue

class AdjectiveValue : public FunctionValue {
    public:
    UnicodeString formatToString(UErrorCode&) const override;
    AdjectiveValue();
    virtual ~AdjectiveValue();
    private:
    friend class AdjectiveFunction;

    UnicodeString formattedString;
    AdjectiveValue(const FunctionValue&,
                   const FunctionOptions&,
                   UErrorCode&);
}; // class AdjectiveValue


class ResourceManager : public Function {
    public:
    LocalPointer<FunctionValue> call(const FunctionContext&, const FunctionValue&, const FunctionOptions&, UErrorCode&) override;
    static MFFunctionRegistry customRegistry(UErrorCode&);
    static Hashtable* properties(UErrorCode&);
    static UnicodeString propertiesAsString(const Hashtable&);
    static Hashtable* parseProperties(const UnicodeString&, UErrorCode&);
    ResourceManager() {}
    virtual ~ResourceManager();
};

class ResourceManagerValue : public FunctionValue {
    public:
    UnicodeString formatToString(UErrorCode&) const override;
    ResourceManagerValue();
    virtual ~ResourceManagerValue();
    private:
    friend class ResourceManager;

    UnicodeString formattedString;
    ResourceManagerValue(const FunctionValue&,
                         const FunctionOptions&,
                         UErrorCode&);
}; // class ResourceManagerValue

class NounFunction : public Function {
    public:
    LocalPointer<FunctionValue> call(const FunctionContext&, const FunctionValue&, const FunctionOptions&, UErrorCode&) override;
    NounFunction() { }
    virtual ~NounFunction();
};

class AdjectiveFunction : public Function {
    public:
    LocalPointer<FunctionValue> call(const FunctionContext&, const FunctionValue&, const FunctionOptions&, UErrorCode&) override;
    AdjectiveFunction() { }
    virtual ~AdjectiveFunction();
};

class CounterFunction : public Function {
    public:
    LocalPointer<FunctionValue> call(const FunctionContext&, const FunctionValue&, const FunctionOptions&, UErrorCode&) override;
    CounterFunction() { }
    virtual ~CounterFunction();
    private:
    int32_t count = 0; // Number of times the function was called
};

class CounterFunctionValue : public FunctionValue {
    public:
    UnicodeString formatToString(UErrorCode&) const override;
    CounterFunctionValue();
    virtual ~CounterFunctionValue();
    private:
    friend class CounterFunction;
    int32_t& count;

    CounterFunctionValue(int32_t&,
                         const FunctionValue&,
                         const FunctionOptions&,
                         UErrorCode&);
}; // class ResourceManagerValue

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */

#endif
