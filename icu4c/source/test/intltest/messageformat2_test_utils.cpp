// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "messageformat2test.h"

using namespace icu::message2;


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
                                "Hello $name",
                                locale,
                                errorCode,
                                U_UNKNOWN_FUNCTION);

    testWithPatternAndArguments("testPersonFormatter",
                                nullptr,
                                "{Hello {$name :person formality=informal}}",
                                name,
                                person,
                                "Hello $name",
                                locale,
                                errorCode,
                                U_UNKNOWN_FUNCTION);

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

#endif /* #if !UCONFIG_NO_FORMATTING */
