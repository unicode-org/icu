// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "plurrule_impl.h"

#include "unicode/listformatter.h"
#include "unicode/messageformat2.h"
#include "intltest.h"
#include "messageformat2test.h"

using namespace message2;
using namespace pluralimpl;

/*
Tests reflect the syntax specified in

  https://github.com/unicode-org/message-format-wg/commits/main/spec/message.abnf

as of the following commit from 2023-05-09:
  https://github.com/unicode-org/message-format-wg/commit/194f6efcec5bf396df36a19bd6fa78d1fa2e0867
*/

static FunctionRegistry* personFunctionRegistry(UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    LocalPointer<FunctionRegistry::Builder> builder(FunctionRegistry::builder(errorCode));
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    return builder->setFormatter(FunctionName("person"), new PersonNameFormatterFactory(), errorCode)
        .build(errorCode);
}

void TestMessageFormat2::checkResult(const UnicodeString& testName,
                 const UnicodeString& pattern,
                 const UnicodeString& result,
                 const UnicodeString& expected,
                 IcuTestErrorCode& errorCode,
                 UErrorCode expectedErrorCode) {

    bool bad = false;
    // Can't compare error codes directly because of warnings
    if (expectedErrorCode == U_ZERO_ERROR) {
        bad = U_FAILURE(errorCode);
    } else {
        bad = errorCode != expectedErrorCode;
    }

  if (bad) {
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
      // TODO: ??
      ((UErrorCode&)errorCode) = U_ILLEGAL_ARGUMENT_ERROR;
      return;
  }
}

void TestMessageFormat2::testWithPatternAndArguments(const UnicodeString& testName,
                                                     FunctionRegistry* customRegistry,
                                                     const UnicodeString& pattern,
                                                     const UnicodeString& argName,
                                                     const UnicodeString& argValue,
                                                     const UnicodeString& expected,
                                                     Locale loc,
                                                     IcuTestErrorCode& errorCode) {
    testWithPatternAndArguments(testName,
                                customRegistry,
                                pattern,
                                argName,
                                argValue,
                                expected,
                                loc,
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
                                                     Locale loc,
                                                     IcuTestErrorCode& errorCode,
                                                     UErrorCode expectedErrorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    LocalPointer<UnicodeString> argValuePointer(new UnicodeString(argValue));
    if (!argValuePointer.isValid()) {
        ((UErrorCode&) errorCode) = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    LocalPointer<Hashtable> arguments(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    arguments->put(argName, argValuePointer.orphan(), errorCode);
    testWithPatternAndArguments(testName, customRegistry, pattern,
                                *arguments, expected, loc, errorCode, expectedErrorCode);
}

void TestMessageFormat2::testWithPatternAndArguments(const UnicodeString& testName,
                                                     FunctionRegistry* customRegistry,
                                                     const UnicodeString& pattern,
                                                     const UnicodeString& argName1,
                                                     const UnicodeString& argValue1,
                                                     const UnicodeString& argName2,
                                                     const UnicodeString& argValue2,
                                                     const UnicodeString& expected,
                                                     Locale loc,
                                                     IcuTestErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    LocalPointer<UnicodeString> argValuePointer1(new UnicodeString(argValue1));
    if (!argValuePointer1.isValid()) {
        ((UErrorCode&) errorCode) = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    LocalPointer<UnicodeString> argValuePointer2(new UnicodeString(argValue2));
    if (!argValuePointer2.isValid()) {
        ((UErrorCode&) errorCode) = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    LocalPointer<Hashtable> arguments(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    arguments->put(argName1, argValuePointer1.orphan(), errorCode);
    arguments->put(argName2, argValuePointer2.orphan(), errorCode);
    testWithPatternAndArguments(testName, customRegistry, pattern,
                                *arguments, expected, loc, errorCode, U_ZERO_ERROR);
}

void TestMessageFormat2::testWithPatternAndArguments(const UnicodeString& testName,
                                                     FunctionRegistry* customRegistry,
                                                     const UnicodeString& pattern,
                                                     const Hashtable& arguments,
                                                     const UnicodeString& expected,
                                                     Locale loc,
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
                        .setLocale(loc)
                        .setFunctionRegistry(customRegistry)
                        .build(parseError, errorCode));
    } else {
        mf.adoptInstead(builder
                        ->setPattern(pattern, errorCode)
                        .setLocale(loc)
                        .build(parseError, errorCode));
    }

    if (U_FAILURE(errorCode)) {
        return;
    }

    UnicodeString result;
    mf->formatToString(arguments, errorCode, result);

    checkResult(testName, pattern, result, expected, errorCode, expectedErrorCode);
}

void TestMessageFormat2::testPersonFormatter(IcuTestErrorCode& errorCode) {
    LocalPointer<FunctionRegistry> customRegistry(personFunctionRegistry(errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    UnicodeString name = "name";
    UnicodeString person = "\"Mr.\", \"John\", \"Doe\"";
    Locale locale = "en";

    testWithPatternAndArguments("testPersonFormatter",
                                nullptr,
                                "{Hello {$name :person formality=formal}}",
                                name,
                                person,
                                "Hello {$name}",
                                locale,
                                errorCode,
                                U_UNKNOWN_FUNCTION_WARNING);

    testWithPatternAndArguments("testPersonFormatter",
                                nullptr,
                                "{Hello {$name :person formality=informal}}",
                                name,
                                person,
                                "Hello {$name}",
                                locale,
                                errorCode,
                                U_UNKNOWN_FUNCTION_WARNING);

    testWithPatternAndArguments("testPersonFormatter",
                                customRegistry.orphan(),
                                "{Hello {$name :person formality=formal}}",
                                name,
                                person,
                                "Hello Mr. Doe",
                                locale,
                                errorCode);

    // recreate custom registry
    customRegistry.adoptInstead(personFunctionRegistry(errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    testWithPatternAndArguments("testPersonFormatter",
                                customRegistry.orphan(),
                                "{Hello {$name :person formality=informal}}",
                                name,
                                person,
                                "Hello John",
                                locale,
                                errorCode);

    // recreate custom registry
    customRegistry.adoptInstead(personFunctionRegistry(errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    testWithPatternAndArguments("testPersonFormatter",
                                customRegistry.orphan(),
                                "{Hello {$name :person formality=formal length=long}}",
                                name,
                                person,
                                "Hello Mr. John Doe",
                                locale,
                                errorCode);
    customRegistry.adoptInstead(personFunctionRegistry(errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    testWithPatternAndArguments("testPersonFormatter",
                                customRegistry.orphan(),
                                "{Hello {$name :person formality=formal length=medium}}",
                                name,
                                person,
                                "Hello John Doe",
                                locale,
                                errorCode);
    customRegistry.adoptInstead(personFunctionRegistry(errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    testWithPatternAndArguments("testPersonFormatter",
                                customRegistry.orphan(),
                                "{Hello {$name :person formality=formal length=short}}",
                                name,
                                person,
                                "Hello Mr. Doe",
                                locale,
                                errorCode);
}


void TestMessageFormat2::testCustomFunctions() {
  IcuTestErrorCode errorCode(*this, "testCustomFunctions");

  testPersonFormatter(errorCode);
  // TODO: add equivalent of testCustomFunctionsComplexMessage()

  testGrammarCasesFormatter(errorCode);
  testListFormatter(errorCode);
  testMessageRefFormatter(errorCode);
}


// -------------- Custom function implementations

Formatter* PersonNameFormatterFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    // Locale not used
    (void) locale;
    // Fixed options not used -- unlike in ICU4J version of this
    // test, all options are passed in using `variableOptions`
    // TODO
    (void) fixedOptions;

    Formatter* result = new PersonNameFormatter();
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

// Trims start/end " marks from s,
// returning true if open/close quotes are present
// and false otherwise
bool parsePart(UnicodeString& s) {
    int32_t len = s.length();
    if (len < 2) {
        return false;
    }
    if (s[0] == '\"') {
        if (s[len - 1] == '\"') {
            s.extract(1, len - 2, s);
            return true;
        }
    }
    return false;
}

// Returns true iff `toFormat` can be parsed as a person (comma-separated list of (title, firstname, lastname))
bool parsePerson(const UnicodeString& toFormat, UnicodeString& title, UnicodeString& firstName, UnicodeString& lastName) {
    uint32_t pos = SplitString::FIRST;
    if (!SplitString::nextPart(toFormat, title, pos)) {
        return false;
    }
    if (!parsePart(title)) {
        return false;
    }
    if (!SplitString::nextPart(toFormat, firstName, pos)) {
        return false;
    }
    if (!parsePart(firstName)) {
        return false;
    }
    if (!SplitString::nextPart(toFormat, lastName, pos)) {
        return false;
    }
    if (!parsePart(lastName)) {
        return false;
    }
    return (pos == SplitString::LAST);
}

void PersonNameFormatter::format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return;
    }

/*
  Note: this test diverges from the ICU4J version of it a bit by using variable options
  to pass both "formality" and "length"
*/
    const UnicodeString* formalityOpt = (const UnicodeString*) variableOptions.get("formality");
    const UnicodeString* lengthOpt = (const UnicodeString*) variableOptions.get("length");

    bool useFormal = formalityOpt != nullptr && *formalityOpt == "formal";
    UnicodeString length = lengthOpt == nullptr ? "short" : *lengthOpt;

    UnicodeString title;
    UnicodeString firstName;
    UnicodeString lastName;
    bool isPerson = parsePerson(toFormat, title, firstName, lastName);

    if (U_FAILURE(errorCode)) {
        return;
    }

    if (isPerson) {
            if (length == "long") {
                result += title;
                result += " ";
                result += firstName;
                result += " ";
                result += lastName;
            } else if (length == "medium") {
                if (useFormal) {
                    result += firstName;
                    result += " ";
                    result += lastName;
                } else {
                    result += title;
                    result += " ";
                    result += firstName;
                }
            } else if (useFormal) {
                // Default to "short" length
                result += title;
                result += " ";
                result += lastName;
            } else {
                result += firstName;
            }
    }
    else {
        result = toFormat;
    }
}

// Utilities
// Iterator for parts of a comma-separated string
// Each part is assumed to be quoted
/* static */ bool SplitString::nextPart(const UnicodeString& in, UnicodeString& out, uint32_t& pos) {
    if (pos == SplitString::LAST) {
        return false;
    }
    U_ASSERT(((int32_t) pos) < in.length());
    int32_t nextComma = in.indexOf(",", pos);
    if (nextComma == -1) {
        // Assume this is the last piece and return it
        in.extract(pos, (in.length() - pos), out);
        out.trim();
        pos = SplitString::LAST;
        return true;
    }
    in.extract(pos, (nextComma - pos), out);
    out.trim();
    pos = nextComma + 1;
    return true;
}

/*
  See ICU4J: CustomFormatterGrammarCaseTest.java
*/
Formatter* GrammarCasesFormatterFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    // Locale not used
    (void) locale;
    // Fixed options not used -- unlike in ICU4J version of this
    // test, all options are passed in using `variableOptions`
    // TODO
    (void) fixedOptions;

    Formatter* result = new GrammarCasesFormatter();
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}


/* static */ void GrammarCasesFormatter::getDativeAndGenitive(const UnicodeString& value, UnicodeString& result) const {
    UnicodeString postfix;
    if (value.endsWith("ana")) {
        value.extract(0,  value.length() - 3, postfix);
        postfix += "nei";
    }
    else if (value.endsWith("ca")) {
        value.extract(0, value.length() - 2, postfix);
        postfix += "căi";
    }
    else if (value.endsWith("ga")) {
        value.extract(0, value.length() - 2, postfix);
        postfix += "găi";
    }
    else if (value.endsWith("a")) {
        value.extract(0, value.length() - 1, postfix);
        postfix += "ei";
    }
    else {
        postfix = "lui " + value;
    }
    result += postfix;
}

void GrammarCasesFormatter::format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return;
    }
    const UnicodeString* grammarCase = (UnicodeString*) variableOptions.get("case");
    if (grammarCase != nullptr && (*grammarCase == "dative" || *grammarCase == "genitive")) {
        getDativeAndGenitive(toFormat, result);
    } else {
        result += toFormat;
    }
}

/* static */ FunctionRegistry* GrammarCasesFormatter::customRegistry(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<FunctionRegistry::Builder> frBuilder(FunctionRegistry::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    return(frBuilder->
            setFormatter(FunctionName("grammarBB"), new GrammarCasesFormatterFactory(), errorCode)
            .build(errorCode));
}

void TestMessageFormat2::runTest(const MessageFormatter& mf, const UnicodeString& testName, 
                                 const UnicodeString& expected,
                                 const UnicodeString& name,
                                 const UnicodeString& value,
                                 UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    LocalPointer<Hashtable> args(Args::of(name, value, errorCode));
    CHECK_ERROR(errorCode);
    runTest(mf, testName, expected, *args, errorCode);
}

void TestMessageFormat2::runTest(const MessageFormatter& mf, const UnicodeString& testName, 
                                 const UnicodeString& expected,
                                 const UnicodeString& name1,
                                 const UnicodeString& value1,
                                 const UnicodeString& name2,
                                 const UnicodeString& value2,
                                 UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    UnicodeString result;
    LocalPointer<Hashtable> args(Args::of(name1, value1, name2, value2, errorCode));
    CHECK_ERROR(errorCode);
    runTest(mf, testName, expected, *args, errorCode);
}

void TestMessageFormat2::runTest(const MessageFormatter& mf, const UnicodeString& testName, 
                                 const UnicodeString& expected,
                                 const Hashtable& args,
                                 UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    UnicodeString result;
    mf.formatToString(args, errorCode, result);
    if (U_SUCCESS(errorCode)) {
        assertEquals(testName, expected, result);
    }
    if (U_FAILURE(errorCode)) {
        dataerrln(testName);
        logln(testName + UnicodeString(" failed test with error code ") + (int32_t)errorCode);
        return;
    }
}

void TestMessageFormat2::testGrammarCasesFormatter(IcuTestErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    UParseError parseErr;
    LocalPointer<MessageFormatter> mf(MessageFormatter::builder(errorCode)
                ->setFunctionRegistry(GrammarCasesFormatter::customRegistry(errorCode))
                .setLocale(Locale("ro"))
                .setPattern("{Cartea {$owner :grammarBB case=genitive}}", errorCode)
                .build(parseErr, errorCode));

    runTest(*mf, "case - genitive", "Cartea Mariei", "owner", "Maria", errorCode);
    runTest(*mf, "case - genitive", "Cartea Rodicăi", "owner", "Rodica", errorCode);
    runTest(*mf, "case - genitive", "Cartea Ilenei", "owner", "Ileana", errorCode);
    runTest(*mf, "case - genitive", "Cartea lui Petre", "owner", "Petre", errorCode);

    mf.adoptInstead(MessageFormatter::builder(errorCode)
                    ->setFunctionRegistry(GrammarCasesFormatter::customRegistry(errorCode))
                    .setLocale(Locale("ro"))
                    .setPattern("{M-a sunat {$owner :grammarBB case=nominative}}", errorCode)
                    .build(parseErr, errorCode));

    runTest(*mf, "case - nominative", "M-a sunat Maria", "owner", "Maria", errorCode);
    runTest(*mf, "case - nominative", "M-a sunat Rodica", "owner", "Rodica", errorCode);
    runTest(*mf, "case - nominative", "M-a sunat Ileana", "owner", "Ileana", errorCode);
    runTest(*mf, "case - nominative", "M-a sunat Petre", "owner", "Petre", errorCode);
}

/* static */ FunctionRegistry* message2::ListFormatter::customRegistry(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<FunctionRegistry::Builder> frBuilder(FunctionRegistry::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    return(frBuilder->
            setFormatter(FunctionName("listformat"), new ListFormatterFactory(), errorCode)
            .build(errorCode));
}

/*
  See ICU4J: CustomFormatterListTest.java
*/
Formatter* ListFormatterFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    // Fixed options not used -- unlike in ICU4J version of this
    // test, all options are passed in using `variableOptions`
    // TODO
    (void) fixedOptions;

    Formatter* result = new ListFormatter(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static const UnicodeString* parseList(const UnicodeString& toFormat, size_t& n_items, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    n_items = 0;
    for (size_t i = 0; ((int32_t) i) < toFormat.length(); i++) {
        if (toFormat[i] == COMMA) {
            n_items++;
        }
    }
    // Account for last item
    n_items++;
    UnicodeString* result = new UnicodeString[n_items];
    size_t j = 0;
    for (size_t i = 0; i < n_items; i++) {
        U_ASSERT(((int32_t) j) < toFormat.length());
        UnicodeString temp;
        U_ASSERT(toFormat[j] == '\"');
        j++; // Consume the opening '\"'
        while (toFormat[j] != '\"') {
            temp += toFormat[j++];
        }
        U_ASSERT(toFormat[j] == '\"');
        j++; // Consume the closing '\"'
        if (i < (n_items - 1)) {
            U_ASSERT(toFormat[j] == COMMA);
            j++; // Consume the comma
            U_ASSERT(toFormat[j] == SPACE);
            j++; // Consume the space
        }
        result[i] = temp;
    }
    return result;
}

void message2::ListFormatter::format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return;
    }

    UnicodeString* optType = (UnicodeString*) variableOptions.get("type");
    UListFormatterType type = UListFormatterType::ULISTFMT_TYPE_AND;
    if (optType != nullptr) {
        if (*optType == "OR") {
            type = UListFormatterType::ULISTFMT_TYPE_OR;
        } else if (*optType == "UNITS") {
            type = UListFormatterType::ULISTFMT_TYPE_UNITS;
        }
    }
    UnicodeString* optWidth = (UnicodeString*) variableOptions.get("width");
    UListFormatterWidth width = UListFormatterWidth::ULISTFMT_WIDTH_WIDE;
    if (optWidth != nullptr) {
        if (*optWidth == "SHORT") {
            width = UListFormatterWidth::ULISTFMT_WIDTH_SHORT;
        } else if (*optType == "NARROW") {
            width = UListFormatterWidth::ULISTFMT_WIDTH_NARROW;
        }
    }
    LocalPointer<icu::ListFormatter> lf(icu::ListFormatter::createInstance(locale, type, width, errorCode));
    CHECK_ERROR(errorCode);

    // Parse toFormat...
    size_t n_items;
    LocalArray<const UnicodeString> listToFormat(parseList(toFormat, n_items, errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }
    lf->format(listToFormat.getAlias(), n_items, result, errorCode);
}

void TestMessageFormat2::testListFormatter(IcuTestErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    UnicodeString progLanguages = "\"C/C++\", \"Java\", \"Python\"";
    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));

    LocalPointer<Hashtable> arguments(Args::of("languages", progLanguages, errorCode));
    CHECK_ERROR(errorCode);
    LocalPointer<TestCase> test(testBuilder->setName("testListFormatter")
                                .setPattern("{I know {$languages :listformat type=AND}!}")
                                .setArguments(arguments.orphan())
                                .setExpected("I know C/C++, Java, and Python!")
                                .build(errorCode));
    TestUtils::runTestCase(*this, ListFormatter::customRegistry(errorCode), *test, errorCode);

    arguments.adoptInstead(Args::of("languages", progLanguages, errorCode));
    CHECK_ERROR(errorCode);
    test.adoptInstead(testBuilder->setName("testListFormatter")
                      .setPattern("{You are allowed to use {$languages :listformat type=OR}!}")
                      .setArguments(arguments.orphan())
                      .setExpected("You are allowed to use C/C++, Java, or Python!")
                      .build(errorCode));
    TestUtils::runTestCase(*this, ListFormatter::customRegistry(errorCode), *test, errorCode);
}

/*
  See ICU4J: CustomFormatterMessageRefTest.java
*/

/* static */ FunctionRegistry* message2::ResourceManager::customRegistry(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<FunctionRegistry::Builder> frBuilder(FunctionRegistry::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    return(frBuilder->
            setFormatter(FunctionName("msgRef"), new ResourceManagerFactory(), errorCode)
            .build(errorCode));
}

/* static */ Hashtable* message2::ResourceManager::properties(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Hashtable> result(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
    NULL_ON_ERROR(errorCode);

    LocalPointer<UnicodeString> value(new UnicodeString("match {$gcase :select} when genitive {Firefoxin} when * {Firefox}"));
    if (!value.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    result->put("firefox", value.orphan(), errorCode);
    value.adoptInstead(new UnicodeString("match {$gcase :select} when genitive {Chromen} when * {Chrome}"));
    if (!value.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    result->put("chrome", value.orphan(), errorCode);
    value.adoptInstead(new UnicodeString("match {$gcase :select} when genitive {Safarin} when * {Safari}"));
    if (!value.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    result->put("safari", value.orphan(), errorCode);
    return result.orphan();
}

/* static */ UnicodeString ResourceManager::propertiesAsString(const Hashtable& properties) {
    UnicodeString result;
    int32_t pos = UHASH_FIRST;
    while(true) {
        bool leadingComma = true;
        if (pos == UHASH_FIRST) {
            leadingComma = false;
        }
        const UHashElement* element = properties.nextElement(pos);
        if (element == nullptr) {
            break;
        }
        if (leadingComma) {
            result += COMMA;
            result += SPACE;
        }
        result += '/' + *((UnicodeString*) element->key.pointer) + "/ : /" + *((UnicodeString*) element->value.pointer) + '/';
    }
    return result;
}

/* static */ Hashtable* ResourceManager::parseProperties(const UnicodeString& properties, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    LocalPointer<Hashtable> result(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    bool error = false;
    if (properties.length() < 1) {
        error = true;
    }
    size_t i = 0;
    while (((int32_t) i) < properties.length()) {
        if (properties[i] != '/') {
            error = true;
            break;
        }
        i++;
        UnicodeString key;
        while (properties[i] != '/') {
            key += properties[i++];
        }
        if (key.length() == 0) {
            error = true;
            break;
        }
        i++; // Consume closing '/'
        if (properties[i] != SPACE) {
            error = true;
            break;
        }
        i++; // Consume space
        if (properties[i] != COLON) {
            error = true;
            break;
        }
        i++; // Consume colon
        if (properties[i] != SPACE) {
            error = true;
            break;
        }
        i++; // Consume space
        if (properties[i] != '/') {
            error = true;
            break;
        }
        i++; // Consume opening '/' for value
        UnicodeString value;
        while (properties[i] != '/') {
            value += properties[i++];
        }
        i++; // Consume closing '/' for value
        // Value may be empty
        if (((int32_t) i) < (properties.length() - 1)) {
            // Consume comma and space
            if (properties[i] != COMMA) {
                error = true;
                break;
            }
            i++;
            if (properties[i] != SPACE) {
                error = true;
                break;
            }
            i++;
        }
        // Add key/value pair to the hash table
        LocalPointer<UnicodeString> valuePtr(new UnicodeString(value));
        if (!valuePtr.isValid()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        result->put(key, valuePtr.orphan(), errorCode);
    }
    if (error) {
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return nullptr;
    }
    return result.orphan();
}

Formatter* ResourceManagerFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    // Locale not used
    (void) locale;

    Formatter* result = new ResourceManager(fixedOptions);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

static void addAll(const Hashtable& source, Hashtable& dest, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    int32_t pos = UHASH_FIRST;
    while (true) {
        const UHashElement* element = source.nextElement(pos);
        if (element == nullptr) {
            break;
        }
        UnicodeString *key = static_cast<UnicodeString *>(element->key.pointer);
        UnicodeString* value = static_cast<UnicodeString*>(element->value.pointer);
        U_ASSERT(key != nullptr && value != nullptr);
        dest.put(*key, value, errorCode);
    }
}

void ResourceManager::format(const UnicodeString& toFormat, const Hashtable& variableOptions, UnicodeString& result, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return;
    }

    UnicodeString* propsStr = (UnicodeString*) variableOptions.get("resbundle");
    // If properties were provided, look up the given string in the properties,
    // yielding a message
    if (propsStr != nullptr) {
        LocalPointer<Hashtable> props(parseProperties(*propsStr, errorCode));
        if (U_FAILURE(errorCode)) {
            return;
        }
        UnicodeString* msg = (UnicodeString*) props->get(toFormat);
        if (msg == nullptr) {
            // No message given for this key -- just format the key
            result += toFormat;
            return;
        }
        LocalPointer<MessageFormatter::Builder> mfBuilder(MessageFormatter::builder(errorCode));
        if (U_FAILURE(errorCode)) {
            return;
        }
        UParseError parseErr;
        // Any parse/data model errors will be propagated
        LocalPointer<MessageFormatter> mf(mfBuilder
                                          ->setPattern(*msg, errorCode)
                                          .build(parseErr, errorCode));
        if (U_FAILURE(errorCode)) {
            return;
        }
        // We want to include any variable options for `msgRef` as fixed
        // options for the contained message. So create a new map
        // and add all arguments and variable options into it
        // Create a new map and add both the arguments and variable options into it
        LocalPointer<Hashtable> mergedOptions(new Hashtable(compareVariableName, nullptr, errorCode));
        CHECK_ERROR(errorCode);
        addAll(fixedOptions, *mergedOptions, errorCode);
        addAll(variableOptions, *mergedOptions, errorCode);
        mf->formatToString(*mergedOptions, errorCode, result);
        // Here, we want to ignore errors (this matches the behavior in the ICU4J test).
        // For example: we want $gcase to default to "$gcase" if the gcase option was
        // omitted.
        if (U_FAILURE(errorCode)) {
            errorCode = U_ZERO_ERROR;
        }
        return;
    }
    // No properties provided -- just format the key
    result += toFormat;
    return;

}


void TestMessageFormat2::testMessageRefFormatter(IcuTestErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    UParseError parseErr;

    LocalPointer<Hashtable> properties(ResourceManager::properties(errorCode));
    LocalPointer<MessageFormatter::Builder> mfBuilder(MessageFormatter::builder(errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    LocalPointer<MessageFormatter> mf(mfBuilder
                                      ->setPattern(*((UnicodeString*) properties->get("firefox")), errorCode)
                                      .build(parseErr,errorCode));
    runTest(*mf, "cust-grammar", "Firefox", "gcase", "whatever", errorCode);
    runTest(*mf, "cust-grammar", "Firefoxin", "gcase", "genitive", errorCode);
    mf.adoptInstead(mfBuilder
                ->setPattern(*((UnicodeString*) properties->get("chrome")), errorCode)
                .build(parseErr, errorCode));
    runTest(*mf, "cust-grammar", "Chrome", "gcase", "whatever", errorCode);
    runTest(*mf, "cust-grammar", "Chromen", "gcase", "genitive", errorCode);

    LocalPointer<MessageFormatter> mf1(MessageFormatter::builder(errorCode)
                ->setFunctionRegistry(ResourceManager::customRegistry(errorCode))
                .setPattern("{Please start {$browser :msgRef gcase=genitive resbundle=$res}}", errorCode)
                .build(parseErr, errorCode));
    LocalPointer<MessageFormatter> mf2(MessageFormatter::builder(errorCode)
                ->setFunctionRegistry(ResourceManager::customRegistry(errorCode))
                .setPattern("{Please start {$browser :msgRef resbundle=$res}}", errorCode)
                .build(parseErr, errorCode));

    UnicodeString propertiesStr = ResourceManager::propertiesAsString(*properties);
    runTest(*mf1, "cust-grammar", "Please start Firefoxin", "browser", "firefox", "res", propertiesStr, errorCode);
    runTest(*mf2, "cust-grammar", "Please start Firefox", "browser", "firefox", "res", propertiesStr, errorCode);
    runTest(*mf1, "cust-grammar", "Please start Chromen", "browser", "chrome", "res", propertiesStr, errorCode);
    runTest(*mf2, "cust-grammar", "Please start Chrome", "browser", "chrome", "res", propertiesStr, errorCode);
    runTest(*mf1, "cust-grammar", "Please start Safarin", "browser", "safari", "res", propertiesStr, errorCode);
    runTest(*mf2, "cust-grammar", "Please start Safari", "browser", "safari", "res", propertiesStr, errorCode);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
