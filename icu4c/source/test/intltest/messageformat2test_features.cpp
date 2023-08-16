// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "messageformat2test.h"

using namespace icu::message2;

/*
  Tests based on ICU4J's MessageFormat2Test.java
*/

/*
  TODO: Tests need to be unified in a single format that
  both ICU4C and ICU4J can use, rather than being embedded in code.
*/

/*
Tests reflect the syntax specified in

  https://github.com/unicode-org/message-format-wg/commits/main/spec/message.abnf

as of the following commit from 2023-05-09:
  https://github.com/unicode-org/message-format-wg/commit/194f6efcec5bf396df36a19bd6fa78d1fa2e0867

*/

void TestMessageFormat2::testDateFormat(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    LocalPointer<Calendar> cal(Calendar::createInstance(errorCode));
    CHECK_ERROR(errorCode);

    cal->set(2022, Calendar::OCTOBER, 27);
    UDate expirationDate = cal->getTime(errorCode);
    CHECK_ERROR(errorCode);
    char expiration[100];
    snprintf(expiration, sizeof(expiration), "%lf", expirationDate);

    LocalPointer<TestCase> test(testBuilder.setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                                .setExpected("Your card expires on Thu, Oct 27, 2022!")
                                .setArgument("exp", expiration, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime datestyle=full}!}")
                      .setExpected("Your card expires on Thursday, October 27, 2022!")
                      .setArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime datestyle=long}!}")
                      .setExpected("Your card expires on October 27, 2022!")
                      .setArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime datestyle=medium}!}")
                      .setExpected("Your card expires on Oct 27, 2022!")
                      .setArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime datestyle=short}!}")
                      .setExpected("Your card expires on 10/27/22!")
                      .setArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    /* TODO: Omitted last few tests that have non-string arguments */
}

void TestMessageFormat2::testPlural(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    UnicodeString message = "match {$count :plural}\n\
                when 1 {You have one notification.}\n           \
                when * {You have {$count} notifications.}\n";

    LocalPointer<TestCase> test(testBuilder.setPattern(message)
                                .setExpected("You have one notification.")
                                .setArgument("count", "1", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("You have 42 notifications.")
                      .setArgument("count", "42", errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testPluralOrdinal(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    UnicodeString message =  "match {$place :selectordinal}\n\
                when 1 {You got the gold medal}\n            \
                when 2 {You got the silver medal}\n          \
                when 3 {You got the bronze medal}\n\
                when one {You got in the {$place}st place}\n\
                when two {You got in the {$place}nd place}\n \
                when few {You got in the {$place}rd place}\n \
                when * {You got in the {$place}th place}\n";

    LocalPointer<TestCase> test(testBuilder.setPattern(message)
                                .setExpected("You got the gold medal")
                                .setArgument("place", "1", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
        
    test.adoptInstead(testBuilder.setExpected("You got the silver medal")
                          .setArgument("place", "2", errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("You got the bronze medal")
                          .setArgument("place", "3", errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("You got in the 21st place")
                          .setArgument("place", "21", errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("You got in the 32nd place")
                          .setArgument("place", "32", errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("You got in the 23rd place")
                          .setArgument("place", "23", errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("You got in the 15th place")
                          .setArgument("place", "15", errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

/* static */ FunctionRegistry* message2::TemperatureFormatter::customRegistry(UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<FunctionRegistry::Builder> frBuilder(FunctionRegistry::builder(errorCode));
    NULL_ON_ERROR(errorCode);

    return(frBuilder->
            setFormatter(FunctionName("temp"), new TemperatureFormatterFactory(), errorCode)
            .build(errorCode));
}

TemperatureFormatter::TemperatureFormatter(Locale l, TemperatureFormatterFactory& c, UErrorCode& errorCode) : locale(l), counter(c) {
    CHECK_ERROR(errorCode);

    cachedFormatters.adoptInstead(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
    if (!cachedFormatters.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    // The environment owns the values
    cachedFormatters->setValueDeleter(uprv_deleteUObject);
}
 
Formatter* TemperatureFormatterFactory::createFormatter(Locale locale, const Hashtable& fixedOptions, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    // fixedOptions not used
    (void) fixedOptions;

    LocalPointer<Formatter> result(new TemperatureFormatter(locale, *this, errorCode));
    NULL_ON_ERROR(errorCode)
    return result.orphan();
}

FormattedPlaceholder* TemperatureFormatter::format(const Formattable* arg, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    if (arg == nullptr) {
        errorCode = U_FORMATTING_ERROR;
        return nullptr;
    }
    const Formattable& toFormat = *arg;

    counter.formatCount++;

    const UnicodeString* unit = (UnicodeString*) variableOptions.get("unit");
    const UnicodeString* skeleton = (UnicodeString*) variableOptions.get("skeleton");
    U_ASSERT(unit != nullptr);

    number::LocalizedNumberFormatter* realNfCached = (number::LocalizedNumberFormatter*) cachedFormatters->get(*unit);
    number::LocalizedNumberFormatter realNf;
    if (realNfCached == nullptr) {
        number::LocalizedNumberFormatter nf = skeleton != nullptr
                    ? number::NumberFormatter::forSkeleton(*skeleton, errorCode).locale(locale)
                    : number::NumberFormatter::withLocale(locale);

        if (*unit == "C") {
            counter.cFormatterCount++;
            realNf = nf.unit(MeasureUnit::getCelsius());
        } else if (*unit == "F") {
            counter.fFormatterCount++;
            realNf = nf.unit(MeasureUnit::getFahrenheit());
        } else {
            realNf = nf;
        }
        realNfCached = new number::LocalizedNumberFormatter(realNf);
        if (realNfCached == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            return nullptr;
        }
        cachedFormatters->put(*unit, realNfCached, errorCode);
    } else {
        realNf = *realNfCached;
    }

    number::FormattedNumber result;
    switch (toFormat.getType()) {
        case Formattable::Type::kDouble: {
            result = realNf.formatDouble(toFormat.getDouble(),
                                                errorCode);
            break;
        }
        case Formattable::Type::kLong: {
            result = realNf.formatInt(toFormat.getLong(),
                                             errorCode);
            break;
        }
        case Formattable::Type::kInt64: {
            result = realNf.formatInt(toFormat.getInt64(),
                                             errorCode);
            break;
        }
        default: {
            return FormattedPlaceholder::create(UnicodeString(), errorCode);
        }
    }
    return FormattedPlaceholder::create(&result, errorCode);
}

void TestMessageFormat2::testFormatterIsCreatedOnce(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
/*
        // Check that the constructor was only called once,
        // and the formatter as many times as the public call to format.
        assertEquals("cached formatter", 1, counter.constructCount);
        assertEquals("cached formatter", maxCount * 2, counter.formatCount);
        assertEquals("cached formatter", 1, counter.fFormatterCount);
        assertEquals("cached formatter", 1, counter.cFormatterCount);

        // Check that the skeleton is respected
        assertEquals("cached formatter",
                "Testing 12°C.",
                mf2.formatToString(Args.of("count", 12, "unit", "C")));
        assertEquals("cached formatter",
                "Testing 12.50°F.",
                mf2.formatToString(Args.of("count", 12.5, "unit", "F")));
        assertEquals("cached formatter",
                "Testing 12.54°C.",
                mf2.formatToString(Args.of("count", 12.54, "unit", "C")));
        assertEquals("cached formatter",
                "Testing 12.54°F.",
                mf2.formatToString(Args.of("count", 12.54321, "unit", "F")));

        message = "{Testing {$count :temp unit=$unit skeleton=(.0/w)}.}";
        mf2 = MessageFormatter.builder()
                .setFunctionRegistry(registry)
                .setPattern(message)
                .build();
        // Check that the skeleton is respected
        assertEquals("cached formatter",
                "Testing 12°C.",
                mf2.formatToString(Args.of("count", 12, "unit", "C")));
        assertEquals("cached formatter",
                "Testing 12.5°F.",
                mf2.formatToString(Args.of("count", 12.5, "unit", "F")));
        assertEquals("cached formatter",
                "Testing 12.5°C.",
                mf2.formatToString(Args.of("count", 12.54, "unit", "C")));
        assertEquals("cached formatter",
                "Testing 12.5°F.",
                mf2.formatToString(Args.of("count", 12.54321, "unit", "F")));
*/

    TemperatureFormatter::customRegistry(errorCode);
    UnicodeString message = "{Testing {$count :temp unit=$unit skeleton=(.00/w)}.}";
    testBuilder.setFunctionRegistry(TemperatureFormatter::customRegistry(errorCode))
               .setPattern(message);
    
    const size_t maxCount = 10;
    char expected[20];
    char arg[5];
    LocalPointer<TestCase> test;
    for (size_t count = 0; count < maxCount; count++) {
        snprintf(expected, sizeof(expected), "Testing %zu°C.", count);
        snprintf(arg, sizeof(arg), "%zu", count);
        test.adoptInstead(testBuilder.setExpected(expected)
            .setArgument("count", arg, errorCode)
            .setArgument("unit", "C", errorCode)
            .build(errorCode));
        TestUtils::runTestCase(*this, *test, errorCode);

        snprintf(expected, sizeof(expected), "Testing %zu°F.", count);
        test.adoptInstead(testBuilder.setExpected(expected)
            .setArgument("count", arg, errorCode)
            .setArgument("unit", "F", errorCode)
            .build(errorCode));
        TestUtils::runTestCase(*this, *test, errorCode);
    }
}

void TestMessageFormat2::testPluralWithOffset(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {

    (void) testBuilder;
    (void) errorCode;
}

void TestMessageFormat2::testPluralWithOffsetAndLocalVar(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    (void) testBuilder;
    (void) errorCode;

}

void TestMessageFormat2::testDeclareBeforeUse(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    (void) testBuilder;
    (void) errorCode;

}

void TestMessageFormat2::testVariableOptionsInSelector(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    (void) testBuilder;
    (void) errorCode;

}

void TestMessageFormat2::testVariableOptionsInSelectorWithLocalVar(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    (void) testBuilder;
    (void) errorCode;

}


void TestMessageFormat2::featureTests() {
    IcuTestErrorCode errorCode(*this, "featureTests");

    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    testBuilder->setName("featureTests");

    testDateFormat(*testBuilder, errorCode);
    testPlural(*testBuilder, errorCode);
    testPluralOrdinal(*testBuilder, errorCode);
    testFormatterIsCreatedOnce(*testBuilder, errorCode);
    testPluralWithOffset(*testBuilder, errorCode);
    testPluralWithOffsetAndLocalVar(*testBuilder, errorCode);
    testDeclareBeforeUse(*testBuilder, errorCode);
    testVariableOptionsInSelector(*testBuilder, errorCode);
    testVariableOptionsInSelectorWithLocalVar(*testBuilder, errorCode);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
