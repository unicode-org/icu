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

void TestMessageFormat2::testPersonFormatter(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<FunctionRegistry> customRegistry(personFunctionRegistry(errorCode));
    UnicodeString name = "name";
    LocalPointer<Person> person(new Person(UnicodeString("Mr."), UnicodeString("John"), UnicodeString("Doe")));
    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    CHECK_ERROR(errorCode);
    testBuilder->setName("testPersonFormatter");
    testBuilder->setLocale(Locale("en"), errorCode);

    LocalPointer<TestCase> test(testBuilder->setPattern("{Hello {$name :person formality=formal}}")
                                .setArgument(name, person.getAlias(), errorCode)
                                .setExpected("Hello {$name}")
                                .setExpectedWarning(U_UNKNOWN_FUNCTION_WARNING)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{Hello {$name :person formality=informal}}")
                                .setArgument(name, person.getAlias(), errorCode)
                                .setExpected("Hello {$name}")
                                .setExpectedWarning(U_UNKNOWN_FUNCTION_WARNING)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    testBuilder->setFunctionRegistry(customRegistry.orphan());

    test.adoptInstead(testBuilder->setPattern("{Hello {$name :person formality=formal}}")
                                .setArgument(name, person.getAlias(), errorCode)
                                .setExpected("Hello Mr. Doe")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{Hello {$name :person formality=informal}}")
                                .setArgument(name, person.getAlias(), errorCode)
                                .setExpected("Hello John")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{Hello {$name :person formality=formal length=long}}")
                                .setArgument(name, person.getAlias(), errorCode)
                                .setExpected("Hello Mr. John Doe")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{Hello {$name :person formality=formal length=medium}}")
                                .setArgument(name, person.getAlias(), errorCode)
                                .setExpected("Hello John Doe")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{Hello {$name :person formality=formal length=short}}")
                                .setArgument(name, person.getAlias(), errorCode)
                                .setExpected("Hello Mr. Doe")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
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

Formatter* PersonNameFormatterFactory::createFormatter(Locale locale, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    // Locale not used
    (void) locale;

    Formatter* result = new PersonNameFormatter();
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

/*
// TODO: don't duplicate this
static void getStringOpt(const Hashtable& opts, const UnicodeString& key, UnicodeString& result, bool& exists) {
    // Returns null if key is absent or is not a string

    // Handle both cases since this may be a Formattable value without previous
    // formatting applied
    if (opts.containsKey(key)) {
        FormattedPlaceholder* val = (FormattedPlaceholder*) opts.get(key);
        U_ASSERT(val != nullptr);
        if (val->getType() == FormattedPlaceholder::Type::STRING) {
            result = val->getString();
            exists = true;
            return;
        }
        if (val->getType() == FormattedPlaceholder::Type::DYNAMIC && val->getInput().getType() == Formattable::Type::kString) {
            result = val->getInput().getString();
            exists = true;
            return;
        }
    }
    exists = false;
}
*/

FormattedPlaceholder* PersonNameFormatter::format(FormattedPlaceholder* arg, const FunctionRegistry::Options& options,  UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    if (arg == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }

    // Assumes what's being passed in is not-yet-formatted
    const Formattable& toFormat = arg->getInput();
/*
  Note: this test diverges from the ICU4J version of it a bit by using variable options
  to pass both "formality" and "length"
*/
    UnicodeString formalityOpt, lengthOpt;
    bool hasFormality, hasLength;
    hasFormality = options.getStringOption(UnicodeString("formality"), formalityOpt);
    hasLength = options.getStringOption(UnicodeString("length"), lengthOpt);

    bool useFormal = hasFormality && formalityOpt == "formal";
    UnicodeString length = hasLength ? lengthOpt : "short";

    Person* p;
    switch (toFormat.getType()) {
        case Formattable::Type::kObject: {
            // Cast to "Person"
            const UObject* asObject = toFormat.getObject();
            if (asObject == nullptr) {
                // Treat the result as empty for null
                return FormattedPlaceholder::create(arg->aliasInput(), UnicodeString(), errorCode);;
            }
            p = (Person*) asObject;
            break;
        }
        default: {
            // If the input is not a person, just return it
            return FormattedPlaceholder::create(arg->aliasInput(), toFormat.getString(), errorCode);
        }
    }

    UnicodeString title = p->title;
    UnicodeString firstName = p->firstName;
    UnicodeString lastName = p->lastName;

    UnicodeString result;
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

    return FormattedPlaceholder::create(arg->aliasInput(), result, errorCode);
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

Person::~Person() {}

/*
  See ICU4J: CustomFormatterGrammarCaseTest.java
*/
Formatter* GrammarCasesFormatterFactory::createFormatter(Locale locale, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    // Locale not used
    (void) locale;

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

FormattedPlaceholder* GrammarCasesFormatter::format(FormattedPlaceholder* arg, const FunctionRegistry::Options& options, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    if (arg == nullptr) {
        errorCode = U_FORMATTING_WARNING;
        return nullptr;
    }

    // Assumes the argument is not-yet-formatted
    const Formattable& toFormat = arg->getInput();
    UnicodeString result;

    switch (toFormat.getType()) {
        case Formattable::Type::kString: {
            const UnicodeString& in = toFormat.getString();
            UnicodeString grammarCase;
            bool hasCase = options.getStringOption(UnicodeString("case"), grammarCase);
            if (hasCase && (grammarCase == "dative" || grammarCase == "genitive")) {
                getDativeAndGenitive(in, result);
            } else {
                result += in;
            }
            break;
        }
        default: {
            result += toFormat.getString();
            break;
        }
    }

    return FormattedPlaceholder::create(arg->aliasInput(), result, errorCode);
}

/* static */ FunctionRegistry* GrammarCasesFormatter::customRegistry(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<FunctionRegistry::Builder> frBuilder(FunctionRegistry::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    return(frBuilder->
            setFormatter(FunctionName("grammarBB"), new GrammarCasesFormatterFactory(), errorCode)
            .build(errorCode));
}

void TestMessageFormat2::testGrammarCasesFormatter(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<FunctionRegistry> customRegistry(GrammarCasesFormatter::customRegistry(errorCode));
    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    CHECK_ERROR(errorCode);
    testBuilder->setName("testGrammarCasesFormatter - genitive");
    testBuilder->setFunctionRegistry(customRegistry.orphan());
    testBuilder->setLocale(Locale("ro"), errorCode);
    testBuilder->setPattern("{Cartea {$owner :grammarBB case=genitive}}");
    LocalPointer<TestCase> test;

    test.adoptInstead(testBuilder->setArgument("owner", "Maria", errorCode)
                                .setExpected("Cartea Mariei")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument("owner", "Rodica", errorCode)
                                .setExpected("Cartea Rodicăi")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument("owner", "Ileana", errorCode)
                                .setExpected("Cartea Ilenei")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument("owner", "Petre", errorCode)
                                .setExpected("Cartea lui Petre")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    testBuilder->setName("testGrammarCasesFormatter - nominative");
    testBuilder->setPattern("{M-a sunat {$owner :grammarBB case=nominative}}");

    test.adoptInstead(testBuilder->setArgument("owner", "Maria", errorCode)
                                .setExpected("M-a sunat Maria")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument("owner", "Rodica", errorCode)
                                .setExpected("M-a sunat Rodica")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument("owner", "Ileana", errorCode)
                                .setExpected("M-a sunat Ileana")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument("owner", "Petre", errorCode)
                                .setExpected("M-a sunat Petre")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
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
Formatter* ListFormatterFactory::createFormatter(Locale locale, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    Formatter* result = new ListFormatter(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

FormattedPlaceholder* message2::ListFormatter::format(FormattedPlaceholder* arg, const FunctionRegistry::Options& options, UErrorCode& errorCode) const {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    if (arg == nullptr) {
        errorCode = U_FORMATTING_WARNING;
        return nullptr;
    }
    // Assumes arg is not-yet-formatted
    const Formattable& toFormat = arg->getInput();

    UnicodeString optType;
    bool hasType = options.getStringOption(UnicodeString("type"), optType);
    UListFormatterType type = UListFormatterType::ULISTFMT_TYPE_AND;
    if (hasType) {
        if (optType == "OR") {
            type = UListFormatterType::ULISTFMT_TYPE_OR;
        } else if (optType == "UNITS") {
            type = UListFormatterType::ULISTFMT_TYPE_UNITS;
        }
    }
    UnicodeString optWidth;
    bool hasWidth = options.getStringOption(UnicodeString("width"), optWidth);
    UListFormatterWidth width = UListFormatterWidth::ULISTFMT_WIDTH_WIDE;
    if (hasWidth) {
        if (optWidth == "SHORT") {
            width = UListFormatterWidth::ULISTFMT_WIDTH_SHORT;
        } else if (optWidth == "NARROW") {
            width = UListFormatterWidth::ULISTFMT_WIDTH_NARROW;
        }
    }
    LocalPointer<icu::ListFormatter> lf(icu::ListFormatter::createInstance(locale, type, width, errorCode));
    NULL_ON_ERROR(errorCode);

    UnicodeString result;

    switch (toFormat.getType()) {
        case Formattable::Type::kArray: {
            int32_t n_items;
            const Formattable* objs = toFormat.getArray(n_items);
            if (objs == nullptr) {
                errorCode = U_FORMATTING_WARNING;
                return nullptr;
            }
            LocalArray<UnicodeString> parts(new UnicodeString[n_items]);
            if (!parts.isValid()) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                return nullptr;
            }
            for (size_t i = 0; ((int32_t) i) < n_items; i++) {
                parts[i] = objs[i].getString();
            }
            lf->format(parts.orphan(), n_items, result, errorCode);
            break;
        }
        default: {
            result += toFormat.getString();
            break;
        }
    }

    return FormattedPlaceholder::create(arg->aliasInput(), result, errorCode);
}

void TestMessageFormat2::testListFormatter(IcuTestErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    const UnicodeString progLanguages[3] = {
        "C/C++",
        "Java",
        "Python"
    };
    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));

    LocalPointer<FunctionRegistry> reg(ListFormatter::customRegistry(errorCode));
    CHECK_ERROR(errorCode);

    testBuilder->setFunctionRegistry(reg.orphan());
    testBuilder->setArgument("languages", progLanguages, 3, errorCode);

    LocalPointer<TestCase> test(testBuilder->setName("testListFormatter")
                                .setPattern("{I know {$languages :listformat type=AND}!}")
                                .setExpected("I know C/C++, Java, and Python!")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setName("testListFormatter")
                      .setPattern("{You are allowed to use {$languages :listformat type=OR}!}")
                      .setExpected("You are allowed to use C/C++, Java, or Python!")
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
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

Formatter* ResourceManagerFactory::createFormatter(Locale locale, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    Formatter* result = new ResourceManager(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

using Arguments = MessageArguments;
using Options = FunctionRegistry::Options;
using Option = FunctionRegistry::Option;

static Arguments* localToGlobal(const Options& options, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<Arguments::Builder> args(Arguments::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    int32_t pos = Options::FIRST;
    UnicodeString optionName;
    while (true) {
        const Option* optionValue = options.nextElement(pos, optionName);
        if (optionValue == nullptr) {
            break;
        }
        switch (optionValue->getType()) {
            case Option::STRING: {
                // add it as a string arg
                args->add(optionName, optionValue->getString(), errorCode);
                break;
            }
            case Option::DOUBLE: {
                args->addDouble(optionName, optionValue->getDouble(), errorCode);
                break;
            }
            case Option::INT64: {
                args->addInt64(optionName, optionValue->getInt64(), errorCode);
                break;
            }
            case Option::LONG: {
                args->addLong(optionName, optionValue->getLong(), errorCode);
                break;
            }
            case Option::DATE: {
                args->addDate(optionName, optionValue->getDate(), errorCode);
                break;
            }
            }
    }
    return args->build(errorCode);
}

FormattedPlaceholder* ResourceManager::format(FormattedPlaceholder* arg, const Options& options, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    if (arg == nullptr) {
        errorCode = U_FORMATTING_WARNING;
        return nullptr;
    }

    // Assumes arg is not-yet-formatted
    const UnicodeString& in = arg->getInput().getString();

    UnicodeString propsStr;
    bool hasPropsStr = options.getStringOption(UnicodeString("resbundle"), propsStr);
    // If properties were provided, look up the given string in the properties,
    // yielding a message
    if (hasPropsStr) {
        LocalPointer<Hashtable> props(parseProperties(propsStr, errorCode));
        NULL_ON_ERROR(errorCode);

        UnicodeString* msg = (UnicodeString*) props->get(in);
        if (msg == nullptr) {
            // No message given for this key -- just format the key
            return FormattedPlaceholder::create(arg->aliasInput(), in, errorCode);
        }
        LocalPointer<MessageFormatter::Builder> mfBuilder(MessageFormatter::builder(errorCode));
        NULL_ON_ERROR(errorCode);
        UParseError parseErr;
        // Any parse/data model errors will be propagated
        LocalPointer<MessageFormatter> mf(mfBuilder
                                          ->setPattern(*msg, errorCode)
                                          .build(parseErr, errorCode));
        NULL_ON_ERROR(errorCode);
/*
        // We want to include any variable options for `msgRef` as fixed
        // options for the contained message. So create a new map
        // and add all arguments and variable options into it
        // Create a new map and add both the arguments and variable options into it
        LocalPointer<Hashtable> mergedOptions(new Hashtable(compareVariableName, nullptr, errorCode));
        NULL_ON_ERROR(errorCode);
        addAll(fixedOptions, *mergedOptions, errorCode);
        addAll(variableOptions, *mergedOptions, errorCode);
*/
        UnicodeString result;

        // variableOptions maps strings to FormattedPlaceholder*, but
        // external arguments maps strings to Formattable*
        LocalPointer<Arguments> arguments(localToGlobal(options, errorCode));
        NULL_ON_ERROR(errorCode);

        // TODO: add formatToParts too, and use that here?
        mf->formatToString(*arguments, errorCode, result);
        // Here, we want to ignore errors (this matches the behavior in the ICU4J test).
        // For example: we want $gcase to default to "$gcase" if the gcase option was
        // omitted.
        if (U_FAILURE(errorCode)) {
            errorCode = U_ZERO_ERROR;
        }
        return FormattedPlaceholder::create(arg->aliasInput(), result, errorCode);
    }
    // No properties provided -- just format the key
    return FormattedPlaceholder::create(arg->aliasInput(), in, errorCode);
}


void TestMessageFormat2::testMessageRefFormatter(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<Hashtable> properties(ResourceManager::properties(errorCode));
    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    CHECK_ERROR(errorCode);
    testBuilder->setLocale(Locale("ro"), errorCode);
    testBuilder->setFunctionRegistry(ResourceManager::customRegistry(errorCode));
    testBuilder->setPattern(*((UnicodeString*) properties->get("firefox")));
    LocalPointer<TestCase> test;
    testBuilder->setName("message-ref");

    test.adoptInstead(testBuilder->setArgument("gcase", "whatever", errorCode)
                                .setExpected("Firefox")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder->setArgument("gcase", "genitive", errorCode)
                                .setExpected("Firefoxin")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    testBuilder->setPattern(*((UnicodeString*) properties->get("chrome")));

    test.adoptInstead(testBuilder->setArgument("gcase", "whatever", errorCode)
                                .setExpected("Chrome")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder->setArgument("gcase", "genitive", errorCode)
                                .setExpected("Chromen")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    // TODO: fix this to pass properties as a hash table
    UnicodeString propertiesStr = ResourceManager::propertiesAsString(*properties);

    testBuilder->setPattern("{Please start {$browser :msgRef gcase=genitive resbundle=$res}}");
    test.adoptInstead(testBuilder->setArgument("browser", "firefox", errorCode)
                                .setArgument("res", propertiesStr, errorCode)
                                .setExpected("Please start Firefoxin")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder->setArgument("browser", "chrome", errorCode)
                                .setArgument("res", propertiesStr, errorCode)
                                .setExpected("Please start Chromen")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder->setArgument("browser", "safari", errorCode)
                                .setArgument("res", propertiesStr, errorCode)
                                .setExpected("Please start Safarin")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    testBuilder->setPattern("{Please start {$browser :msgRef resbundle=$res}}");
    test.adoptInstead(testBuilder->setArgument("browser", "firefox", errorCode)
                                .setArgument("res", propertiesStr, errorCode)
                                .setExpected("Please start Firefox")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder->setArgument("browser", "chrome", errorCode)
                                .setArgument("res", propertiesStr, errorCode)
                                .setExpected("Please start Chrome")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
   test.adoptInstead(testBuilder->setArgument("browser", "safari", errorCode)
                                .setArgument("res", propertiesStr, errorCode)
                                .setExpected("Please start Safari")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
