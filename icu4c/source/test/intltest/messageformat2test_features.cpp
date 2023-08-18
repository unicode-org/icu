// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/gregocal.h"
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
    UDate expiration = cal->getTime(errorCode);
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test(testBuilder.setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                                .setExpected("Your card expires on Thu, Oct 27, 2022!")
                                .setDateArgument("exp", expiration, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime datestyle=full}!}")
                      .setExpected("Your card expires on Thursday, October 27, 2022!")
                      .setDateArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime datestyle=long}!}")
                      .setExpected("Your card expires on October 27, 2022!")
                      .setDateArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime datestyle=medium}!}")
                      .setExpected("Your card expires on Oct 27, 2022!")
                      .setDateArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime datestyle=short}!}")
                      .setExpected("Your card expires on 10/27/22!")
                      .setDateArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

/*
  TODO: for now, this doesn't work, because Formattable includes a date tag but not calendar

  and I'm not sure how to use RTTI to pass it as an object and then chek the tag in DateTime::format...
  if that's even allowed

    cal.adoptInstead(new GregorianCalendar(2022, Calendar::OCTOBER, 27, errorCode));
    if (cal.isValid()) {
        Formattable calArg(cal.orphan());
        test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                          .setExpected("Your card expires on Thu, Oct 27, 2022!")
                          .setArgument("exp", calArg, errorCode)
                          .build(errorCode));
        TestUtils::runTestCase(*this, *test, errorCode);
    }
*/
    // TODO: Last few test cases involve implicit formatters based on type of object --
    // we haven't implemented that
}

void TestMessageFormat2::testPlural(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    UnicodeString message = "match {$count :plural}\n\
                when 1 {You have one notification.}\n           \
                when * {You have {$count} notifications.}\n";

    long count = 1;
    LocalPointer<TestCase> test(testBuilder.setPattern(message)
                                .setExpected("You have one notification.")
                                .setArgument("count", count, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    count = 42;
    test.adoptInstead(testBuilder.setExpected("You have 42 notifications.")
                      .setArgument("count", count, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    count = 1;
    test.adoptInstead(testBuilder.setPattern(message)
                      .setExpected("You have one notification.")
                      .setArgument("count", count, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    count = 42;
    test.adoptInstead(testBuilder.setExpected("You have 42 notifications.")
                      .setArgument("count", count, errorCode)
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

TemperatureFormatter::TemperatureFormatter(Locale l, TemperatureFormatterFactory& c, UErrorCode& errorCode) : locale(l), counter(c) {
    CHECK_ERROR(errorCode);

    cachedFormatters.adoptInstead(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
    if (!cachedFormatters.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
    // The environment owns the values
    //   cachedFormatters->setValueDeleter(uprv_deleteUObject);
    counter.constructCount++;
}
 
Formatter* TemperatureFormatterFactory::createFormatter(Locale locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Formatter> result(new TemperatureFormatter(locale, *this, errorCode));
    NULL_ON_ERROR(errorCode)
    return result.orphan();
}

// TODO: copy-pasted
static void getStringOpt(const Hashtable& opts, const UnicodeString& key, UnicodeString& result, bool& exists) {
    // Returns null if key is absent or is not a string
    if (opts.containsKey(key)) {
        FormattedPlaceholder* val = (FormattedPlaceholder*) opts.get(key);
        U_ASSERT(val != nullptr);
        if (val->getType() == FormattedPlaceholder::Type::STRING) {
            result = val->getString();
            exists = true;
            return;
        }
        if (val->getType() == FormattedPlaceholder::Type::DYNAMIC && val->getInput().getType() == Formattable::Type::kString) {
            // TODO: this is why it would be good to not have two different string representations
            result = val->getInput().getString();
            exists = true;
            return;
        }
    }
    exists = false;
}

FormattedPlaceholder* TemperatureFormatter::format(FormattedPlaceholder* arg, const Hashtable& variableOptions, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    if (arg == nullptr) {
        errorCode = U_FORMATTING_ERROR;
        return nullptr;
    }
    // Assume arg is not-yet-formatted
    const Formattable& toFormat = arg->getInput();

    counter.formatCount++;

    UnicodeString unit;
    bool unitExists = false;
    getStringOpt(variableOptions, "unit", unit, unitExists);
    U_ASSERT(unitExists);
    UnicodeString skeleton;
    bool skeletonExists = false;
    getStringOpt(variableOptions, "skeleton", skeleton, skeletonExists);

    number::LocalizedNumberFormatter* realNfCached = (number::LocalizedNumberFormatter*) cachedFormatters->get(unit);
    number::LocalizedNumberFormatter realNf;
    if (realNfCached == nullptr) {
        number::LocalizedNumberFormatter nf = skeletonExists
                    ? number::NumberFormatter::forSkeleton(skeleton, errorCode).locale(locale)
                    : number::NumberFormatter::withLocale(locale);

        if (unit == "C") {
            counter.cFormatterCount++;
            realNf = nf.unit(MeasureUnit::getCelsius());
        } else if (unit == "F") {
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
        cachedFormatters->put(unit, realNfCached, errorCode);
    } else {
        realNf = *realNfCached;
    }

    number::FormattedNumber result;
    LocalPointer<Formattable> copied(new Formattable(toFormat));
    if (!copied.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
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
            return FormattedPlaceholder::create(copied.orphan(), UnicodeString(), errorCode);
        }
    }
    return FormattedPlaceholder::create(copied.orphan(), std::move(result), errorCode);
}

TemperatureFormatter::~TemperatureFormatter() {}

void putFormattableArg(Hashtable& arguments, const UnicodeString& k, const UnicodeString& arg, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    Formattable* valPtr(new Formattable(arg));
    if (valPtr == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    } else {
        arguments.put(k, valPtr, errorCode);
    }
}

void putFormattableArg(Hashtable& arguments, const UnicodeString& k, long arg, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    Formattable* valPtr(new Formattable(arg));
    if (valPtr == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    } else {
        arguments.put(k, valPtr, errorCode);
    }
}

void TestMessageFormat2::testFormatterIsCreatedOnce(IcuTestErrorCode& errorCode) {
/*
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

    LocalPointer<FunctionRegistry::Builder> frBuilder(FunctionRegistry::builder(errorCode));
    CHECK_ERROR(errorCode);

    // Counter will be adopted by function registry
    TemperatureFormatterFactory* counter = new TemperatureFormatterFactory();
    if (counter == nullptr) {
        ((UErrorCode&) errorCode) = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    LocalPointer<FunctionRegistry> reg(frBuilder->setFormatter(FunctionName("temp"),
                                                               counter, errorCode)
                                       .build(errorCode));
    CHECK_ERROR(errorCode);
    UnicodeString message = "{Testing {$count :temp unit=$unit skeleton=|.00/w|}.}";

    LocalPointer<MessageFormatter::Builder> mfBuilder(MessageFormatter::builder(errorCode));
    CHECK_ERROR(errorCode);
    mfBuilder->setPattern(message, errorCode);
    mfBuilder->setFunctionRegistry(reg.getAlias());
    UParseError parseError;
    LocalPointer<MessageFormatter> mf(mfBuilder->build(parseError, errorCode));
    UnicodeString result;
    UnicodeString countKey("count");
    UnicodeString unitKey("unit");

    const size_t maxCount = 10;
    char expected[20];
    LocalPointer<Hashtable> arguments(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
    CHECK_ERROR(errorCode);
    for (size_t count = 0; count < maxCount; count++) {
        long arg = (long) count;
        snprintf(expected, sizeof(expected), "Testing %zu°C.", count);

        putFormattableArg(*arguments, countKey, arg, errorCode);
        putFormattableArg(*arguments, unitKey, "C", errorCode);
        CHECK_ERROR(errorCode);

        mf->formatToString(*arguments, errorCode, result);
        assertEquals("temperature formatter", expected, result);
        result.remove();

        snprintf(expected, sizeof(expected), "Testing %zu°F.", count);
        putFormattableArg(*arguments, countKey, arg, errorCode);
        putFormattableArg(*arguments, unitKey, "F", errorCode);
        CHECK_ERROR(errorCode);
        
        mf->formatToString(*arguments, errorCode, result);
        assertEquals("temperature formatter", expected, result);
        result.remove();
    }

    assertEquals("cached formatter", 1, counter->constructCount);
    assertEquals("cached formatter", ((int32_t) (maxCount * 2)), counter->formatCount);
    assertEquals("cached formatter", 1, counter->fFormatterCount);
    assertEquals("cached formatter", 1, counter->cFormatterCount);
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
    testFormatterIsCreatedOnce(errorCode);
    testPluralWithOffset(*testBuilder, errorCode);
    testPluralWithOffsetAndLocalVar(*testBuilder, errorCode);
    testDeclareBeforeUse(*testBuilder, errorCode);
    testVariableOptionsInSelector(*testBuilder, errorCode);
    testVariableOptionsInSelectorWithLocalVar(*testBuilder, errorCode);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
