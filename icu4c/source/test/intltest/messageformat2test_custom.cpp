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
                                .setExpectedError(U_UNKNOWN_FUNCTION_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{Hello {$name :person formality=informal}}")
                                .setArgument(name, person.getAlias(), errorCode)
                                .setExpected("Hello {$name}")
                                .setExpectedError(U_UNKNOWN_FUNCTION_ERROR)
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

void TestMessageFormat2::testCustomFunctionsComplexMessage(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<FunctionRegistry> customRegistry(personFunctionRegistry(errorCode));
    UnicodeString host = "host";
    UnicodeString hostGender = "hostGender";
    UnicodeString guest = "guest";
    UnicodeString guestCount = "guestCount";

    LocalPointer<Person> jane(new Person(UnicodeString("Ms."), UnicodeString("Jane"), UnicodeString("Doe")));
    LocalPointer<Person> john(new Person(UnicodeString("Mr."), UnicodeString("John"), UnicodeString("Doe")));
    LocalPointer<Person> anonymous(new Person(UnicodeString("Mx."), UnicodeString("Anonymous"), UnicodeString("Doe")));
   if (!jane.isValid() || !john.isValid() || !anonymous.isValid()) {
       ((UErrorCode&) errorCode) = U_MEMORY_ALLOCATION_ERROR;
       return;
   }

    UnicodeString message = "let $hostName = {$host :person length=long}\n\
                let $guestName = {$guest :person length=long}\n\
                let $guestsOther = {$guestCount :number offset=1}\n\
                match {$hostGender :gender} {$guestCount :plural}\n\
                when female 0 {{$hostName} does not give a party.}\n\
                when female 1 {{$hostName} invites {$guestName} to her party.}\n\
                when female 2 {{$hostName} invites {$guestName} and one other person to her party.}\n\
                when female * {{$hostName} invites {$guestName} and {$guestsOther} other people to her party.}\n\
                when male 0 {{$hostName} does not give a party.}\n\
                when male 1 {{$hostName} invites {$guestName} to his party.}\n\
                when male 2 {{$hostName} invites {$guestName} and one other person to his party.}\n\
                when male * {{$hostName} invites {$guestName} and {$guestsOther} other people to his party.}\n\
                when * 0 {{$hostName} does not give a party.}\n\
                when * 1 {{$hostName} invites {$guestName} to their party.}\n\
                when * 2 {{$hostName} invites {$guestName} and one other person to their party.}\n\
                when * * {{$hostName} invites {$guestName} and {$guestsOther} other people to their party.}\n";


    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    CHECK_ERROR(errorCode);
    testBuilder->setName("testCustomFunctionsComplexMessage");
    testBuilder->setLocale(Locale("en"), errorCode);
    testBuilder->setPattern(message);
    testBuilder->setFunctionRegistry(customRegistry.orphan());

    LocalPointer<TestCase> test(testBuilder->setArgument(host, jane.getAlias(), errorCode)
                                .setArgument(hostGender, "female", errorCode)
                                .setArgument(guest, john.getAlias(), errorCode)
                                .setArgument(guestCount, (int64_t) 3, errorCode)
                                .setExpected("Ms. Jane Doe invites Mr. John Doe and 2 other people to her party.")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument(host, jane.getAlias(), errorCode)
                                .setArgument(hostGender, "female", errorCode)
                                .setArgument(guest, john.getAlias(), errorCode)
                                .setArgument(guestCount, (int64_t) 2, errorCode)
                                .setExpected("Ms. Jane Doe invites Mr. John Doe and one other person to her party.")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument(host, jane.getAlias(), errorCode)
                                .setArgument(hostGender, "female", errorCode)
                                .setArgument(guest, john.getAlias(), errorCode)
                                .setArgument(guestCount, (int64_t) 1, errorCode)
                                .setExpected("Ms. Jane Doe invites Mr. John Doe to her party.")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument(host, john.getAlias(), errorCode)
                                .setArgument(hostGender, "male", errorCode)
                                .setArgument(guest, jane.getAlias(), errorCode)
                                .setArgument(guestCount, (int64_t) 3, errorCode)
                                .setExpected("Mr. John Doe invites Ms. Jane Doe and 2 other people to his party.")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setArgument(host, anonymous.getAlias(), errorCode)
                                .setArgument(hostGender, "unknown", errorCode)
                                .setArgument(guest, jane.getAlias(), errorCode)
                                .setArgument(guestCount, (int64_t) 2, errorCode)
                                .setExpected("Mx. Anonymous Doe invites Ms. Jane Doe and one other person to their party.")
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testCustomFunctions() {
  IcuTestErrorCode errorCode(*this, "testCustomFunctions");

  testPersonFormatter(errorCode);
  testCustomFunctionsComplexMessage(errorCode);
  testGrammarCasesFormatter(errorCode);
  testListFormatter(errorCode);
  testMessageRefFormatter(errorCode);
}


// -------------- Custom function implementations

Formatter* PersonNameFormatterFactory::createFormatter(const Locale& locale, UErrorCode& errorCode) {
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

void PersonNameFormatter::format(FormattingContext& context, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    if (!context.hasObjectInput()) {
        return;
    }

    UnicodeString formalityOpt, lengthOpt;
    bool hasFormality, hasLength;
    hasFormality = context.getStringOption(UnicodeString("formality"), formalityOpt);
    hasLength = context.getStringOption(UnicodeString("length"), lengthOpt);

    bool useFormal = hasFormality && formalityOpt == "formal";
    UnicodeString length = hasLength ? lengthOpt : "short";

    const Person& p = static_cast<const Person&>(context.getObjectInput());

    UnicodeString title = p.title;
    UnicodeString firstName = p.firstName;
    UnicodeString lastName = p.lastName;

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

    context.setOutput(result);
}

Person::~Person() {}

/*
  See ICU4J: CustomFormatterGrammarCaseTest.java
*/
Formatter* GrammarCasesFormatterFactory::createFormatter(const Locale& locale, UErrorCode& errorCode) {
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

void GrammarCasesFormatter::format(FormattingContext& context, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Argument must be     present
    if (!context.hasFormattableInput()) {
        context.setFormattingError("grammarBB", errorCode);
        return;
    }

    // Assumes the argument is not-yet-formatted
    const Formattable& toFormat = context.getFormattableInput();
    UnicodeString result;

    switch (toFormat.getType()) {
        case Formattable::Type::kString: {
            const UnicodeString& in = toFormat.getString();
            UnicodeString grammarCase;
            bool hasCase = context.getStringOption(UnicodeString("case"), grammarCase);
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

    context.setOutput(result);
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
Formatter* ListFormatterFactory::createFormatter(const Locale& locale, UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }

    Formatter* result = new ListFormatter(locale);
    if (result == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

void message2::ListFormatter::format(FormattingContext& context, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Argument must be present
    if (!context.hasFormattableInput()) {
        context.setFormattingError("listformat", errorCode);
        return;
    }
    // Assumes arg is not-yet-formatted
    const Formattable& toFormat = context.getFormattableInput();

    UnicodeString optType;
    bool hasType = context.getStringOption(UnicodeString("type"), optType);
    UListFormatterType type = UListFormatterType::ULISTFMT_TYPE_AND;
    if (hasType) {
        if (optType == "OR") {
            type = UListFormatterType::ULISTFMT_TYPE_OR;
        } else if (optType == "UNITS") {
            type = UListFormatterType::ULISTFMT_TYPE_UNITS;
        }
    }
    UnicodeString optWidth;
    bool hasWidth = context.getStringOption(UnicodeString("width"), optWidth);
    UListFormatterWidth width = UListFormatterWidth::ULISTFMT_WIDTH_WIDE;
    if (hasWidth) {
        if (optWidth == "SHORT") {
            width = UListFormatterWidth::ULISTFMT_WIDTH_SHORT;
        } else if (optWidth == "NARROW") {
            width = UListFormatterWidth::ULISTFMT_WIDTH_NARROW;
        }
    }
    LocalPointer<icu::ListFormatter> lf(icu::ListFormatter::createInstance(locale, type, width, errorCode));
    CHECK_ERROR(errorCode);

    UnicodeString result;

    switch (toFormat.getType()) {
        case Formattable::Type::kArray: {
            int32_t n_items;
            const Formattable* objs = toFormat.getArray(n_items);
            if (objs == nullptr) {
                context.setFormattingError("listformatter", errorCode);
                return;
            }
            LocalArray<UnicodeString> parts(new UnicodeString[n_items]);
            if (!parts.isValid()) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            for (int32_t i = 0; i < n_items; i++) {
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

    context.setOutput(result);
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

Formatter* ResourceManagerFactory::createFormatter(const Locale& locale, UErrorCode& errorCode) {
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

static Arguments* localToGlobal(const FormattingContext& context, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);
    LocalPointer<Arguments::Builder> args(Arguments::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    int32_t pos = context.firstOption();
    UnicodeString optionName;
    while (true) {
        const Formattable* optionValue = context.nextOption(pos, optionName);
        if (optionValue == nullptr) {
            break;
        }
        switch (optionValue->getType()) {
            case Formattable::Type::kString: {
                // add it as a string arg
                args->add(optionName, optionValue->getString(), errorCode);
                break;
            }
            case Formattable::Type::kDouble: {
                args->addDouble(optionName, optionValue->getDouble(), errorCode);
                break;
            }
            case Formattable::Type::kInt64: {
                args->addInt64(optionName, optionValue->getInt64(), errorCode);
                break;
            }
            case Formattable::Type::kLong: {
                args->addInt64(optionName, (int64_t) optionValue->getLong(), errorCode);
                break;
            }
            case Formattable::Type::kDate: {
                args->addDate(optionName, optionValue->getDate(), errorCode);
                break;
            }
            default: {
                // Ignore other types
                continue;
            }
            }
    }
    return args->build(errorCode);
}

void ResourceManager::format(FormattingContext& context, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Argument must be present
    if (!context.hasFormattableInput()) {
        context.setFormattingError("msgref", errorCode);
        return;
    }

    // Assumes arg is not-yet-formatted
    const Formattable& toFormat = context.getFormattableInput();
    UnicodeString in;
    switch (toFormat.getType()) {
        case Formattable::Type::kString: {
            in = toFormat.getString();
            break;
        }
        default: {
            // Ignore non-strings
            return;
        }
    }

    UnicodeString resbundle("resbundle");
    bool hasProperties = context.hasObjectOption(resbundle);
    // If properties were provided, look up the given string in the properties,
    // yielding a message
    if (hasProperties) {
        const Hashtable& properties = reinterpret_cast<const Hashtable&>(context.getObjectOption(resbundle));
        UnicodeString* msg = (UnicodeString*) properties.get(in);
        if (msg == nullptr) {
            // No message given for this key -- error out
            context.setFormattingError("msgref", errorCode);
            return;
        }
        LocalPointer<MessageFormatter::Builder> mfBuilder(MessageFormatter::builder(errorCode));
        CHECK_ERROR(errorCode);
        UParseError parseErr;
        // Any parse/data model errors will be propagated
        LocalPointer<MessageFormatter> mf(mfBuilder->setPattern(*msg)
                                          .build(parseErr, errorCode));
        CHECK_ERROR(errorCode);
        UnicodeString result;

        LocalPointer<Arguments> arguments(localToGlobal(context, errorCode));
        CHECK_ERROR(errorCode);

        UErrorCode savedStatus = errorCode;
        mf->formatToString(*arguments, errorCode, result);
        // Here, we want to ignore errors (this matches the behavior in the ICU4J test).
        // For example: we want $gcase to default to "$gcase" if the gcase option was
        // omitted.
        if (U_FAILURE(errorCode)) {
            errorCode = savedStatus;
        }
       context.setOutput(result);
    } else {
        // Properties must be provided
        context.setFormattingError("msgref", errorCode);
    }
    return;
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

    testBuilder->setArgument("res", (UObject*) properties.orphan(), errorCode);

    testBuilder->setPattern("{Please start {$browser :msgRef gcase=genitive resbundle=$res}}");
    test.adoptInstead(testBuilder->setArgument("browser", "firefox", errorCode)
                                .setExpected("Please start Firefoxin")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder->setArgument("browser", "chrome", errorCode)
                                .setExpected("Please start Chromen")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder->setArgument("browser", "safari", errorCode)
                                .setExpected("Please start Safarin")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    testBuilder->setPattern("{Please start {$browser :msgRef resbundle=$res}}");
    test.adoptInstead(testBuilder->setArgument("browser", "firefox", errorCode)
                                .setExpected("Please start Firefox")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder->setArgument("browser", "chrome", errorCode)
                                .setExpected("Please start Chrome")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
   test.adoptInstead(testBuilder->setArgument("browser", "safari", errorCode)
                                .setExpected("Please start Safari")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
