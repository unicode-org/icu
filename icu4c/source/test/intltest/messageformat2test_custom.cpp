// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_NORMALIZATION

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "plurrule_impl.h"

#include "unicode/listformatter.h"
#include "unicode/numberformatter.h"
#include "messageformat2test.h"
#include "hash.h"
#include "intltest.h"


using namespace message2;
using namespace pluralimpl;

/*
Tests reflect the syntax specified in

  https://github.com/unicode-org/message-format-wg/commits/main/spec/message.abnf

as of the following commit from 2023-05-09:
  https://github.com/unicode-org/message-format-wg/commit/194f6efcec5bf396df36a19bd6fa78d1fa2e0867
*/

using namespace data_model;

void TestMessageFormat2::testPersonFormatter(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    MFFunctionRegistry customRegistry(MFFunctionRegistry::Builder(errorCode)
                                      .adoptFunction(FunctionName("person"),
                                                     new PersonNameFunction(),
                                                     errorCode)
                                      .build());
    UnicodeString name = "name";
    LocalPointer<Person> person(new Person(UnicodeString("Mr."), UnicodeString("John"), UnicodeString("Doe")));
    TestCase::Builder testBuilder;
    testBuilder.setName("testPersonFormatter");
    testBuilder.setLocale(Locale("en"));
    testBuilder.setBidiIsolationStrategy(MessageFormatter::U_MF_BIDI_OFF);

    TestCase test = testBuilder.setPattern("Hello {$name :person formality=formal}")
        .setArgument(name, person.getAlias())
        .setExpected("Hello {$name}")
        .setExpectedError(U_MF_UNKNOWN_FUNCTION_ERROR)
        .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setPattern("Hello {$name :person formality=informal}")
                                .setArgument(name, person.getAlias())
                                .setExpected("Hello {$name}")
                                .setExpectedError(U_MF_UNKNOWN_FUNCTION_ERROR)
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    testBuilder.setFunctionRegistry(&customRegistry);

    test = testBuilder.setPattern("Hello {$name :person formality=formal}")
                                .setArgument(name, person.getAlias())
                                .setExpected("Hello Mr. Doe")
                                .setExpectSuccess()
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setPattern("Hello {$name :person formality=informal}")
                                .setArgument(name, person.getAlias())
                                .setExpected("Hello John")
                                .setExpectSuccess()
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setPattern("Hello {$name :person formality=formal length=long}")
                                .setArgument(name, person.getAlias())
                                .setExpected("Hello Mr. John Doe")
                                .setExpectSuccess()
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setPattern("Hello {$name :person formality=formal length=medium}")
                                .setArgument(name, person.getAlias())
                                .setExpected("Hello John Doe")
                                .setExpectSuccess()
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setPattern("Hello {$name :person formality=formal length=short}")
                                .setArgument(name, person.getAlias())
                                .setExpected("Hello Mr. Doe")
                                .setExpectSuccess()
                                .build();

    TestUtils::runTestCase(*this, test, errorCode);

}

void TestMessageFormat2::testCustomFunctionsComplexMessage(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    MFFunctionRegistry customRegistry(MFFunctionRegistry::Builder(errorCode)
                                      .adoptFunction(FunctionName("person"),
                                                     new PersonNameFunction(),
                                                     errorCode)
                                      .build());
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

    UnicodeString message = ".local $hostName = {$host :person length=long}\n\
                .local $guestName = {$guest :person length=long}\n\
                .input {$guestCount :number}\n\
                .input {$hostGender :string}\n\
                .match $hostGender $guestCount\n\
                 female 0 {{{$hostName} does not give a party.}}\n\
                 female 1 {{{$hostName} invites {$guestName} to her party.}}\n\
                 female 2 {{{$hostName} invites {$guestName} and one other person to her party.}}\n\
                 female * {{{$hostName} invites {$guestCount} people, including {$guestName}, to her party.}}\n\
                 male 0 {{{$hostName} does not give a party.}}\n\
                 male 1 {{{$hostName} invites {$guestName} to his party.}}\n\
                 male 2 {{{$hostName} invites {$guestName} and one other person to his party.}}\n\
                 male * {{{$hostName} invites {$guestCount} people, including {$guestName}, to his party.}}\n\
                 * 0 {{{$hostName} does not give a party.}}\n\
                 * 1 {{{$hostName} invites {$guestName} to their party.}}\n\
                 * 2 {{{$hostName} invites {$guestName} and one other person to their party.}}\n\
                 * * {{{$hostName} invites {$guestCount} people, including {$guestName}, to their party.}}";


    TestCase::Builder testBuilder;
    testBuilder.setName("testCustomFunctionsComplexMessage");
    testBuilder.setLocale(Locale("en"));
    testBuilder.setPattern(message);
    testBuilder.setFunctionRegistry(&customRegistry);
    testBuilder.setBidiIsolationStrategy(MessageFormatter::U_MF_BIDI_OFF);

    TestCase test = testBuilder.setArgument(host, jane.getAlias())
        .setArgument(hostGender, "female")
        .setArgument(guest, john.getAlias())
        .setArgument(guestCount, static_cast<int64_t>(3))
        .setExpected("Ms. Jane Doe invites 3 people, including Mr. John Doe, to her party.")
        .setExpectSuccess()
        .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument(host, jane.getAlias())
                                .setArgument(hostGender, "female")
                                .setArgument(guest, john.getAlias())
                                .setArgument(guestCount, static_cast<int64_t>(2))
                                .setExpected("Ms. Jane Doe invites Mr. John Doe and one other person to her party.")
                                .setExpectSuccess()
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument(host, jane.getAlias())
                                .setArgument(hostGender, "female")
                                .setArgument(guest, john.getAlias())
                                .setArgument(guestCount, static_cast<int64_t>(1))
                                .setExpected("Ms. Jane Doe invites Mr. John Doe to her party.")
                                .setExpectSuccess()
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument(host, john.getAlias())
                                .setArgument(hostGender, "male")
                                .setArgument(guest, jane.getAlias())
                                .setArgument(guestCount, static_cast<int64_t>(3))
                                .setExpected("Mr. John Doe invites 3 people, including Ms. Jane Doe, to his party.")
                                .setExpectSuccess()
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument(host, anonymous.getAlias())
                                .setArgument(hostGender, "unknown")
                                .setArgument(guest, jane.getAlias())
                                .setArgument(guestCount, static_cast<int64_t>(2))
                                .setExpected("Mx. Anonymous Doe invites Ms. Jane Doe and one other person to their party.")
                                .setExpectSuccess()
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
}

void TestMessageFormat2::testComplexOptions(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    MFFunctionRegistry customRegistry(MFFunctionRegistry::Builder(errorCode)
                                      .adoptFunction(FunctionName("noun"),
                                                     new NounFunction(),
                                                     errorCode)
                                      .adoptFunction(FunctionName("adjective"),
                                                     new AdjectiveFunction(),
                                                     errorCode)
                                      .build());
    UnicodeString name = "name";
    TestCase::Builder testBuilder;
    testBuilder.setName("testComplexOptions");
    testBuilder.setLocale(Locale("en"));
    testBuilder.setFunctionRegistry(&customRegistry);
    testBuilder.setBidiIsolationStrategy(MessageFormatter::U_MF_BIDI_OFF);

    // Test that options can be values with their own resolved
    // options attached
    TestCase test = testBuilder.setPattern(".input {$item :noun case=accusative count=1} \
                                            .local $colorMatchingGrammaticalNumberGenderCase = {$color :adjective accord=$item} \
                                            {{{$colorMatchingGrammaticalNumberGenderCase}}}")

        .setArgument(UnicodeString("color"), UnicodeString("red"))
        .setArgument(UnicodeString("item"), UnicodeString("balloon"))
        .setExpected("red balloon (accusative, singular adjective)")
        .build();
    TestUtils::runTestCase(*this, test, errorCode);

    // Test that the same noun can be used multiple times
    test = testBuilder.setPattern(".input {$item :noun case=accusative count=1} \
                                            .local $colorMatchingGrammaticalNumberGenderCase = {$color :adjective accord=$item} \
                                            .local $sizeMatchingGrammaticalNumberGenderCase = {$size :adjective accord=$item} \
                                            {{{$colorMatchingGrammaticalNumberGenderCase}, {$sizeMatchingGrammaticalNumberGenderCase}}}")

        .setArgument(UnicodeString("color"), UnicodeString("red"))
        .setArgument(UnicodeString("item"), UnicodeString("balloon"))
        .setArgument(UnicodeString("size"), UnicodeString("huge"))
        .setExpected("red balloon (accusative, singular adjective), \
huge balloon (accusative, singular adjective)")
        .build();
    TestUtils::runTestCase(*this, test, errorCode);

}

void TestMessageFormat2::testCustomFunctions() {
  IcuTestErrorCode errorCode(*this, "testCustomFunctions");

  testPersonFormatter(errorCode);
  testCustomFunctionsComplexMessage(errorCode);
  testGrammarCasesFormatter(errorCode);
  testListFormatter(errorCode);
  testMessageRefFormatter(errorCode);
  testComplexOptions(errorCode);
  testSingleEvaluation(errorCode);
}


// -------------- Custom function implementations

static UnicodeString getStringOption(const FunctionOptionsMap& opt,
                                     const UnicodeString& k) {
    if (opt.count(k) == 0) {
        return {};
    }
    UErrorCode localErrorCode = U_ZERO_ERROR;
    const message2::FunctionValue* optVal = opt.at(k);
    if (optVal == nullptr) {
        return {};
    }
    const UnicodeString& formatted = optVal->formatToString(localErrorCode);
    if (U_SUCCESS(localErrorCode)) {
        return formatted;
    }
    const UnicodeString& original = optVal->unwrap().getString(localErrorCode);
    if (U_SUCCESS(localErrorCode)) {
        return original;
    }
    return {};
}

static bool hasStringOption(const FunctionOptionsMap& opt,
                            const UnicodeString& k, const UnicodeString& v) {
    return getStringOption(opt, k) == v;
}

LocalPointer<FunctionValue> PersonNameFunction::call(const FunctionContext& context,
                                                     const FunctionValue& arg,
                                                     const FunctionOptions& opts,
                                                     UErrorCode& errorCode) {
    (void) context;

    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }
    LocalPointer<FunctionValue> v(new PersonNameValue(arg, std::move(opts), errorCode));
    if (!v.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return v;
}

UnicodeString PersonNameValue::formatToString(UErrorCode& status) const {
    (void) status;
    return formattedString;
}

PersonNameValue::PersonNameValue(const FunctionValue& arg,
                                 const FunctionOptions& options,
                                 UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    innerValue = arg.unwrap();
    opts = options;

    const Formattable* toFormat = &innerValue;
    if (U_FAILURE(errorCode)) {
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
        return;
    }

    FunctionOptionsMap opt = opts.getOptions();

    bool useFormal = hasStringOption(opt, "formality", "formal");
    UnicodeString length = getStringOption(opt, "length");
    if (length.length() == 0) {
        length = "short";
    }

    const FormattableObject* fp = toFormat->getObject(errorCode);
    if (errorCode == U_ILLEGAL_ARGUMENT_ERROR) {
        errorCode = U_MF_FORMATTING_ERROR;
        return;
    }

    if (fp == nullptr || fp->tag() != u"person") {
        errorCode = U_MF_FORMATTING_ERROR;
        return;
    }
    const Person* p = static_cast<const Person*>(fp);

    UnicodeString title = p->title;
    UnicodeString firstName = p->firstName;
    UnicodeString lastName = p->lastName;

    if (length == "long") {
        formattedString += title;
        formattedString += " ";
        formattedString += firstName;
        formattedString += " ";
        formattedString += lastName;
    } else if (length == "medium") {
        if (useFormal) {
            formattedString += firstName;
            formattedString += " ";
            formattedString += lastName;
        } else {
            formattedString += title;
            formattedString += " ";
            formattedString += firstName;
        }
    } else if (useFormal) {
        // Default to "short" length
        formattedString += title;
        formattedString += " ";
        formattedString += lastName;
    } else {
        formattedString += firstName;
    }
}

FormattableProperties::~FormattableProperties() {}
Person::~Person() {}
PersonNameValue::~PersonNameValue() {}

/*
  See ICU4J: CustomFormatterGrammarCaseTest.java
*/

/* static */ void GrammarCasesValue::getDativeAndGenitive(const UnicodeString& value, UnicodeString& result) const {
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

LocalPointer<FunctionValue>
GrammarCasesFunction::call(const FunctionContext& context,
                           const FunctionValue& arg,
                           const FunctionOptions& opts,
                           UErrorCode& errorCode) {
    (void) context;

    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }

    LocalPointer<FunctionValue> v(new GrammarCasesValue(arg, std::move(opts), errorCode));
    if (!v.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return v;
}

UnicodeString GrammarCasesValue::formatToString(UErrorCode& status) const {
    (void) status;
    return formattedString;
}

GrammarCasesValue::GrammarCasesValue(const FunctionValue& val,
                                     const FunctionOptions& opts,
                                     UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    innerValue = val.unwrap();
    // Tests don't cover composition, so no need to merge options
    const Formattable* toFormat = &innerValue;

    UnicodeString result;
    const FunctionOptionsMap opt = opts.getOptions();
    switch (toFormat->getType()) {
        case UFMT_STRING: {
            const UnicodeString& in = toFormat->getString(errorCode);
            bool hasCase = opt.count("case") > 0;
            const Formattable& caseAsFormattable = opt.at("case")->unwrap();
            if (U_FAILURE(errorCode)) {
                errorCode = U_MF_FORMATTING_ERROR;
                return;
            }
            bool caseIsString = caseAsFormattable.getType() == UFMT_STRING;
            if (hasCase && caseIsString) {
                const UnicodeString& caseOpt = caseAsFormattable.getString(errorCode);
                if (caseOpt == "dative" || caseOpt == "genitive") {
                    getDativeAndGenitive(in, result);
                }
                else {
                    result += in;
                }
            }
            U_ASSERT(U_SUCCESS(errorCode));
            break;
        }
        default: {
            result += toFormat->getString(errorCode);
            break;
        }
    }

    formattedString = result;
}

void TestMessageFormat2::testGrammarCasesFormatter(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    MFFunctionRegistry customRegistry = MFFunctionRegistry::Builder(errorCode)
        .adoptFunction(FunctionName("grammarBB"), new GrammarCasesFunction(), errorCode)
        .build();

    TestCase::Builder testBuilder;
    testBuilder.setName("testGrammarCasesFormatter - genitive");
    testBuilder.setFunctionRegistry(&customRegistry);
    testBuilder.setLocale(Locale("ro"));
    testBuilder.setPattern("Cartea {$owner :grammarBB case=genitive}");
    testBuilder.setBidiIsolationStrategy(MessageFormatter::U_MF_BIDI_OFF);

    TestCase test = testBuilder.setArgument("owner", "Maria")
                                .setExpected("Cartea Mariei")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument("owner", "Rodica")
                                .setExpected("Cartea Rodicăi")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument("owner", "Ileana")
                                .setExpected("Cartea Ilenei")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument("owner", "Petre")
                                .setExpected("Cartea lui Petre")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    testBuilder.setName("testGrammarCasesFormatter - nominative");
    testBuilder.setPattern("M-a sunat {$owner :grammarBB case=nominative}");

    test = testBuilder.setArgument("owner", "Maria")
                                .setExpected("M-a sunat Maria")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument("owner", "Rodica")
                                .setExpected("M-a sunat Rodica")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument("owner", "Ileana")
                                .setExpected("M-a sunat Ileana")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setArgument("owner", "Petre")
                                .setExpected("M-a sunat Petre")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
}

GrammarCasesValue::~GrammarCasesValue() {}

/*
  See ICU4J: CustomFormatterListTest.java
*/

LocalPointer<FunctionValue>
ListFunction::call(const FunctionContext& context,
                   const FunctionValue& arg,
                   const FunctionOptions& opts,
                   UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }

    LocalPointer<FunctionValue>
        v(new ListValue(context.getLocale(), arg, std::move(opts), errorCode));
    if (!v.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return v;
}

UnicodeString ListValue::formatToString(UErrorCode& errorCode) const {
    (void) errorCode;

    return formattedString;
}

message2::ListValue::ListValue(const Locale& locale,
                               const FunctionValue& val,
                               const FunctionOptions& opts,
                               UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    innerValue = val.unwrap();
    // Tests don't cover composition, so no need to merge options

    const Formattable* toFormat = &innerValue;
    if (U_FAILURE(errorCode)) {
        // Must have an argument
        errorCode = U_MF_OPERAND_MISMATCH_ERROR;
        return;
    }

    FunctionOptionsMap opt = opts.getOptions();
    UListFormatterType type = UListFormatterType::ULISTFMT_TYPE_AND;
    if (hasStringOption(opt, "type", "OR")) {
        type = UListFormatterType::ULISTFMT_TYPE_OR;
    } else if (hasStringOption(opt, "type", "UNITS")) {
        type = UListFormatterType::ULISTFMT_TYPE_UNITS;
    }
    UListFormatterWidth width = UListFormatterWidth::ULISTFMT_WIDTH_WIDE;
    if (hasStringOption(opt, "width", "SHORT")) {
        width = UListFormatterWidth::ULISTFMT_WIDTH_SHORT;
    } else if (hasStringOption(opt, "width", "NARROW")) {
        width = UListFormatterWidth::ULISTFMT_WIDTH_NARROW;
    }
    LocalPointer<icu::ListFormatter> lf(icu::ListFormatter::createInstance(locale, type, width, errorCode));
    if (U_FAILURE(errorCode)) {
        return;
    }

    switch (toFormat->getType()) {
        case UFMT_ARRAY: {
            int32_t n_items;
            const Formattable* objs = toFormat->getArray(n_items, errorCode);
            if (U_FAILURE(errorCode)) {
                errorCode = U_MF_FORMATTING_ERROR;
                return;
            }
            UnicodeString* parts = new UnicodeString[n_items];
            if (parts == nullptr) {
                errorCode = U_MEMORY_ALLOCATION_ERROR;
                return;
            }
            for (int32_t i = 0; i < n_items; i++) {
                parts[i] = objs[i].getString(errorCode);
            }
            U_ASSERT(U_SUCCESS(errorCode));
            lf->format(parts, n_items, formattedString, errorCode);
            delete[] parts;
            break;
        }
        default: {
            formattedString += toFormat->getString(errorCode);
            U_ASSERT(U_SUCCESS(errorCode));
            break;
        }
    }
}

ListValue::~ListValue() {}
ListFunction::~ListFunction() {}

void TestMessageFormat2::testListFormatter(IcuTestErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }
    const message2::Formattable progLanguages[3] = {
        message2::Formattable("C/C++"),
        message2::Formattable("Java"),
        message2::Formattable("Python")
    };

    TestCase::Builder testBuilder;

    MFFunctionRegistry reg = MFFunctionRegistry::Builder(errorCode)
        .adoptFunction(FunctionName("listformat"), new ListFunction(), errorCode)
        .build();
    CHECK_ERROR(errorCode);

    testBuilder.setLocale(Locale("en"));
    testBuilder.setFunctionRegistry(&reg);
    testBuilder.setArgument("languages", progLanguages, 3);
    testBuilder.setBidiIsolationStrategy(MessageFormatter::U_MF_BIDI_OFF);

    TestCase test = testBuilder.setName("testListFormatter")
        .setPattern("I know {$languages :listformat type=AND}!")
        .setExpected("I know C/C++, Java, and Python!")
        .build();
    TestUtils::runTestCase(*this, test, errorCode);

    test = testBuilder.setName("testListFormatter")
                      .setPattern("You are allowed to use {$languages :listformat type=OR}!")
                      .setExpected("You are allowed to use C/C++, Java, or Python!")
                      .build();
    TestUtils::runTestCase(*this, test, errorCode);
}

/*
  See ICU4J: CustomFormatterMessageRefTest.java
*/

/* static */ Hashtable* message2::ResourceManager::properties(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    UnicodeString* firefox = new UnicodeString(".input {$gcase :string} .match $gcase  genitive {{Firefoxin}}  * {{Firefox}}");
    UnicodeString* chrome = new UnicodeString(".input {$gcase :string} .match $gcase genitive {{Chromen}}  * {{Chrome}}");
    UnicodeString* safari = new UnicodeString(".input {$gcase :string} .match $gcase  genitive {{Safarin}}  * {{Safari}}");

    if (firefox != nullptr && chrome != nullptr && safari != nullptr) {
        Hashtable* result = new Hashtable(uhash_compareUnicodeString, nullptr, errorCode);
        if (result == nullptr) {
            return nullptr;
        }
        result->setValueDeleter(uprv_deleteUObject);
        result->put("safari", safari, errorCode);
        result->put("firefox", firefox, errorCode);
        result->put("chrome", chrome, errorCode);
        return result;
    }

    // Allocation failed
    errorCode = U_MEMORY_ALLOCATION_ERROR;
    if (firefox != nullptr) {
        delete firefox;
    }
    if (chrome != nullptr) {
        delete chrome;
    }
    if (safari != nullptr) {
        delete safari;
    }
    return nullptr;
}

using Arguments = MessageArguments;

static Arguments localToGlobal(const FunctionOptionsMap& opts, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return {};
    }
    std::map<UnicodeString, message2::Formattable> result;
    for (auto iter = opts.cbegin(); iter != opts.cend(); ++iter) {
        result[iter->first] = iter->second->unwrap();
    }
    return MessageArguments(result, status);
}

LocalPointer<FunctionValue>
ResourceManager::call(const FunctionContext&,
                      const FunctionValue& arg,
                      const FunctionOptions& options,
                      UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }

    LocalPointer<FunctionValue>
        result(new ResourceManagerValue(arg, std::move(options), errorCode));

    if (!result.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return result;
}

UnicodeString message2::ResourceManagerValue::formatToString(UErrorCode&) const {
    return formattedString;
}

message2::ResourceManagerValue::ResourceManagerValue(const FunctionValue& arg,
                                                     const FunctionOptions& opts,
                                                     UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    innerValue = arg.unwrap();
    // Tests don't cover composition, so no need to merge options

    const Formattable* toFormat = &innerValue;
    // Check for null or fallback
    if (errorCode == U_ILLEGAL_ARGUMENT_ERROR) {
        errorCode = U_MF_FORMATTING_ERROR;
        return;
    }
    UnicodeString in;
    switch (toFormat->getType()) {
        case UFMT_STRING: {
            in = toFormat->getString(errorCode);
            break;
        }
        default: {
            // Ignore non-strings
            return;
        }
    }
    FunctionOptionsMap opt = opts.getOptions();
    bool hasProperties = opt.count("resbundle") > 0
        && opt["resbundle"]->unwrap().getType() == UFMT_OBJECT
        && opt["resbundle"]->unwrap().getObject(errorCode)->tag() == u"properties";

    // If properties were provided, look up the given string in the properties,
    // yielding a message
    if (hasProperties) {
        const FormattableProperties* properties = reinterpret_cast<const FormattableProperties*>
            (opt["resbundle"]->unwrap().getObject(errorCode));
        U_ASSERT(U_SUCCESS(errorCode));
        UnicodeString* msg = static_cast<UnicodeString*>(properties->properties->get(in));
        if (msg == nullptr) {
            // No message given for this key -- error out
            errorCode = U_MF_FORMATTING_ERROR;
            return;
        }
	MessageFormatter::Builder mfBuilder(errorCode);
        UParseError parseErr;
        // Any parse/data model errors will be propagated
	MessageFormatter mf = mfBuilder.setPattern(*msg, parseErr, errorCode).build(errorCode);
        Arguments arguments = localToGlobal(opt, errorCode);
        if (U_FAILURE(errorCode)) {
            return;
        }

        UErrorCode savedStatus = errorCode;
        UnicodeString result = mf.formatToString(arguments, errorCode);
        // Here, we want to ignore errors (this matches the behavior in the ICU4J test).
        // For example: we want $gcase to default to "$gcase" if the gcase option was
        // omitted.
        if (U_FAILURE(errorCode)) {
            errorCode = savedStatus;
        }
        formattedString = result;
    } else {
        // Properties must be provided
        errorCode = U_MF_FORMATTING_ERROR;
    }
    return;
}

ResourceManager::~ResourceManager() {}
ResourceManagerValue::~ResourceManagerValue() {}

void TestMessageFormat2::testMessageRefFormatter(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    Hashtable* properties = ResourceManager::properties(errorCode);
    CHECK_ERROR(errorCode);
    LocalPointer<FormattableProperties> fProperties(new FormattableProperties(properties));
    if (!fProperties.isValid()) {
        ((UErrorCode&) errorCode) = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    MFFunctionRegistry reg = MFFunctionRegistry::Builder(errorCode)
        .adoptFunction(FunctionName("msgRef"), new ResourceManager(), errorCode)
        .build();
    CHECK_ERROR(errorCode);

    TestCase::Builder testBuilder;
    testBuilder.setLocale(Locale("ro"));
    testBuilder.setFunctionRegistry(&reg);
    testBuilder.setPattern(*static_cast<UnicodeString*>(properties->get("firefox")));
    testBuilder.setName("message-ref");
    testBuilder.setBidiIsolationStrategy(MessageFormatter::U_MF_BIDI_OFF);

    TestCase test = testBuilder.setArgument("gcase", "whatever")
                                .setExpected("Firefox")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
    test = testBuilder.setArgument("gcase", "genitive")
                                .setExpected("Firefoxin")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    testBuilder.setPattern(*static_cast<UnicodeString*>(properties->get("chrome")));

    test = testBuilder.setArgument("gcase", "whatever")
                                .setExpected("Chrome")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
    test = testBuilder.setArgument("gcase", "genitive")
                                .setExpected("Chromen")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    testBuilder.setArgument("res", fProperties.getAlias());

    testBuilder.setPattern("Please start {$browser :msgRef gcase=genitive resbundle=$res}");
    test = testBuilder.setArgument("browser", "firefox")
                                .setExpected("Please start Firefoxin")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
    test = testBuilder.setArgument("browser", "chrome")
                                .setExpected("Please start Chromen")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
    test = testBuilder.setArgument("browser", "safari")
                                .setExpected("Please start Safarin")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);

    testBuilder.setPattern("Please start {$browser :msgRef resbundle=$res}");
    test = testBuilder.setArgument("browser", "firefox")
                                .setExpected("Please start Firefox")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
    test = testBuilder.setArgument("browser", "chrome")
                                .setExpected("Please start Chrome")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
    test = testBuilder.setArgument("browser", "safari")
                                .setExpected("Please start Safari")
                                .build();
    TestUtils::runTestCase(*this, test, errorCode);
}

LocalPointer<FunctionValue>
NounFunction::call(const FunctionContext&,
                   const FunctionValue& arg,
                   const FunctionOptions& opts,
                   UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }

    LocalPointer<FunctionValue>
        v(new NounValue(arg, std::move(opts), errorCode));
    if (!v.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return v;
}

UnicodeString NounValue::formatToString(UErrorCode& status) const {
    (void) status;

    return formattedString;
}

NounValue::NounValue(const FunctionValue& arg,
                     const FunctionOptions& options,
                     UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    innerValue = arg.unwrap();
    opts = options.mergeOptions(arg.getResolvedOptions(), errorCode);

    const Formattable* toFormat = &innerValue;
    FunctionOptionsMap opt = opts.getOptions();

    // very simplified example
    bool useAccusative = hasStringOption(opt, "case", "accusative");
    bool useSingular = hasStringOption(opt, "count", "1");
    const UnicodeString& noun = toFormat->getString(errorCode);
    if (errorCode == U_ILLEGAL_ARGUMENT_ERROR) {
        errorCode = U_MF_FORMATTING_ERROR;
        return;
    }

    if (useAccusative) {
        if (useSingular) {
            formattedString = noun + " accusative, singular noun";
        } else {
            formattedString = noun + " accusative, plural noun";
        }
    } else {
        if (useSingular) {
            formattedString = noun + " dative, singular noun";
        } else {
            formattedString = noun + " dative, plural noun";
        }
    }
}

LocalPointer<FunctionValue>
AdjectiveFunction::call(const FunctionContext&,
                        const FunctionValue& arg,
                        const FunctionOptions& opts,
                        UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }

    LocalPointer<FunctionValue>
        v(new AdjectiveValue(arg, std::move(opts), errorCode));
    if (!v.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    return v;
}

UnicodeString AdjectiveValue::formatToString(UErrorCode& status) const {
    (void) status;

    return formattedString;
}

AdjectiveValue::AdjectiveValue(const FunctionValue& arg,
                               const FunctionOptions& options,
                               UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return;
    }

    innerValue = arg.unwrap();
    opts = options.mergeOptions(arg.getResolvedOptions(), errorCode);

    const Formattable* toFormat = &innerValue;

    const FunctionOptionsMap opt = opts.getOptions();
    // Return empty string if no accord is provided
    if (opt.count("accord") <= 0) {
        return;
    }

    const FunctionValue& accordOpt = *opt.at("accord");
    const Formattable& accordSrc = accordOpt.unwrap();
    UnicodeString accord = accordSrc.getString(errorCode);
    const UnicodeString& adjective = toFormat->getString(errorCode);
    if (errorCode == U_ILLEGAL_ARGUMENT_ERROR) {
        errorCode = U_MF_FORMATTING_ERROR;
        return;
    }

    formattedString = adjective + " " + accord;
    // very simplified example
    FunctionOptionsMap accordOptionsMap = accordOpt.getResolvedOptions().getOptions();
    bool accordIsAccusative = hasStringOption(accordOptionsMap, "case", "accusative");
    bool accordIsSingular = hasStringOption(accordOptionsMap, "count", "1");
    if (accordIsAccusative) {
        if (accordIsSingular) {
            formattedString += " (accusative, singular adjective)";
        } else {
            formattedString += " (accusative, plural adjective)";
        }
    } else {
        if (accordIsSingular) {
            formattedString += " (dative, singular adjective)";
        } else {
            formattedString += " (dative, plural adjective)";
        }
    }
}

NounFunction::~NounFunction() {}
AdjectiveFunction::~AdjectiveFunction() {}
NounValue::~NounValue() {}
AdjectiveValue::~AdjectiveValue() {}

void TestMessageFormat2::testSingleEvaluation(IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    MFFunctionRegistry customRegistry(MFFunctionRegistry::Builder(errorCode)
                                      .adoptFunction(FunctionName("counter"),
                                                     new CounterFunction(),
                                                     errorCode)
                                      .build());
    UnicodeString name = "name";
    TestCase::Builder testBuilder;
    testBuilder.setName("testSingleEvaluation");
    testBuilder.setLocale(Locale("en"));
    testBuilder.setFunctionRegistry(&customRegistry);
    testBuilder.setBidiIsolationStrategy(MessageFormatter::U_MF_BIDI_OFF);

    // Test that the RHS of each declaration is evaluated at most once
    TestCase test = testBuilder.setPattern(".local $x = {:counter}\
                                           {{{$x} {$x}}}")
        .setExpected("1 1")
        .build();
    TestUtils::runTestCase(*this, test, errorCode);
}

LocalPointer<FunctionValue>
CounterFunction::call(const FunctionContext&,
                      const FunctionValue& arg,
                      const FunctionOptions& opts,
                      UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return LocalPointer<FunctionValue>();
    }

    LocalPointer<FunctionValue>
        v(new CounterFunctionValue(count, arg, std::move(opts), errorCode));
    if (!v.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    count++;
    return v;
}

CounterFunctionValue::CounterFunctionValue(int32_t& c,
                                           const FunctionValue&,
                                           const FunctionOptions&,
                                           UErrorCode&) : count(c) {
    // No operand, no options
}

UnicodeString CounterFunctionValue::formatToString(UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return {};
    }
    number::UnlocalizedNumberFormatter nf = number::NumberFormatter::with();
    number::FormattedNumber formattedNumber = nf.locale("en-US").formatInt(count, status);
    return formattedNumber.toString(status);
}

CounterFunction::~CounterFunction() {}
CounterFunctionValue::~CounterFunctionValue() {}

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */
