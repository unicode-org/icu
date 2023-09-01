// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/gregocal.h"
#include "unicode/messageformat2.h"
#include "messageformat2test.h"

using namespace icu::message2;

/*
  Tests based on ICU4J's MessageFormat2Test.java
and Mf2FeaturesTest.java
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

void TestMessageFormat2::testEmptyMessage(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test(testBuilder.setPattern("{}")
                                .setExpected("")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);    
}

void TestMessageFormat2::testPlainText(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test(testBuilder.setPattern("{Hello World!}")
                                .setExpected("Hello World!")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);    
}

void TestMessageFormat2::testPlaceholders(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test(testBuilder.setPattern("{Hello, {$userName}!}")
                                .setExpected("Hello, John!")
                                .setArgument("userName", "John", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);    
}

void TestMessageFormat2::testArgumentMissing(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    UnicodeString message = "{Hello {$name}, today is {$today :datetime skeleton=yMMMMdEEEE}.}";
    LocalPointer<Calendar> cal(Calendar::createInstance(errorCode));
    CHECK_ERROR(errorCode);

    // November 23, 2022 at 7:42:37.123 PM
    cal->set(2022, Calendar::NOVEMBER, 23, 19, 42, 37);
    UDate TEST_DATE = cal->getTime(errorCode);
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test(testBuilder.setPattern(message)
                                .setArgument("name", "John", errorCode)
                                .setDateArgument("today", TEST_DATE, errorCode) 
                                .setExpected("Hello John, today is Wednesday, November 23, 2022.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    // Missing date argument
    test.adoptInstead(testBuilder.setPattern(message)
                                .clearArguments(errorCode)
                                .setArgument("name", "John", errorCode)
                                .setExpected("Hello John, today is {$today}.")
                                .setExpectedError(U_UNRESOLVED_VARIABLE_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setPattern(message)
                                .clearArguments(errorCode)
                                .setDateArgument("today", TEST_DATE, errorCode)
                                .setExpectedError(U_UNRESOLVED_VARIABLE_ERROR)
                                .setExpected("Hello {$name}, today is Wednesday, November 23, 2022.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    // Both arguments missing
    test.adoptInstead(testBuilder.setPattern(message)
                                .clearArguments(errorCode)
                                .setExpectedError(U_UNRESOLVED_VARIABLE_ERROR)
                                .setExpected("Hello {$name}, today is {$today}.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testDefaultLocale(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<Calendar> cal(Calendar::createInstance(errorCode));
    CHECK_ERROR(errorCode);
    // November 23, 2022 at 7:42:37.123 PM
    cal->set(2022, Calendar::NOVEMBER, 23, 19, 42, 37);
    UDate TEST_DATE = cal->getTime(errorCode);
    CHECK_ERROR(errorCode);

    UnicodeString message = "{Date: {$date :datetime skeleton=yMMMMdEEEE}.}";
    UnicodeString expectedEn = "Date: Wednesday, November 23, 2022.";
    UnicodeString expectedRo = "Date: miercuri, 23 noiembrie 2022.";

    testBuilder.setPattern(message);

    LocalPointer<TestCase> test;
    test.adoptInstead(testBuilder.clearArguments(errorCode)
                                .setDateArgument("date", TEST_DATE, errorCode)
                                .setExpected(expectedEn)
                                .setExpectSuccess()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected(expectedRo)
                                .setLocale(Locale("ro"), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    Locale originalLocale = Locale::getDefault();
    Locale::setDefault(Locale::forLanguageTag("ro", errorCode), errorCode);
    CHECK_ERROR(errorCode);

    test.adoptInstead(testBuilder.setExpected(expectedEn)
                                .setLocale(Locale("en", "US"), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected(expectedRo)
                                .clearLocale()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    
    Locale::setDefault(originalLocale, errorCode);
    CHECK_ERROR(errorCode);
}

void TestMessageFormat2::testSpecialPluralWithDecimals(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    UnicodeString message;

    message = "let $amount = {$count :number}\n\
                match {$amount :plural}\n\
                  when 1 {I have {$amount} dollar.}\n\
                  when * {I have {$amount} dollars.}\n";

    LocalPointer<TestCase> test;

    test.adoptInstead(testBuilder.setPattern(message)
                                .clearArguments(errorCode)
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setExpected("I have 1 dollar.")
                                .setLocale(Locale("en", "US"), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    message = "let $amount = {$count :number skeleton=|.00*|}\n\
                match {$amount :plural skeleton=|.00*|}\n\
                  when 1 {I have {$amount} dollar.}\n\
                  when * {I have {$amount} dollars.}\n";

    test.adoptInstead(testBuilder.setPattern(message)
                                .setExpected("I have 1.00 dollar.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testDefaultFunctionAndOptions(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<Calendar> cal(Calendar::createInstance(errorCode));
    CHECK_ERROR(errorCode);
    // November 23, 2022 at 7:42:37.123 PM
    cal->set(2022, Calendar::NOVEMBER, 23, 19, 42, 37);
    UDate TEST_DATE = cal->getTime(errorCode);
    CHECK_ERROR(errorCode);
    LocalPointer<TestCase> test;

    test.adoptInstead(testBuilder.setPattern("{Testing date formatting: {$date}.}")
                                .clearArguments(errorCode)
                                .setDateArgument("date", TEST_DATE, errorCode)
                                .setExpected("Testing date formatting: 23.11.2022, 19:42.")
                                .setLocale(Locale("ro"), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setPattern("{Testing date formatting: {$date :datetime}.}")
                                .setExpected("Testing date formatting: 23.11.2022, 19:42.")
                                .setLocale(Locale("ro"), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testSimpleSelection(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    (void) testBuilder;
    (void) errorCode;

    /* Covered by testPlural */
}

void TestMessageFormat2::testComplexSelection(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test;

    UnicodeString message = "match {$photoCount :plural} {$userGender :select}\n\
                 when 1 masculine {{$userName} added a new photo to his album.}\n\
                 when 1 feminine {{$userName} added a new photo to her album.}\n\
                 when 1 * {{$userName} added a new photo to their album.}\n\
                 when * masculine {{$userName} added {$photoCount} photos to his album.}\n\
                 when * feminine {{$userName} added {$photoCount} photos to her album.}\n\
                 when * * {{$userName} added {$photoCount} photos to their album.}";
    testBuilder.setPattern(message);

    int64_t count = 1;
    test.adoptInstead(testBuilder.setArgument("photoCount", count, errorCode)
                                .setArgument("userGender", "masculine", errorCode)
                                .setArgument("userName", "John", errorCode)
                                .setExpected("John added a new photo to his album.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setArgument("userGender", "feminine", errorCode)
                                .setArgument("userName", "Anna", errorCode)
                                .setExpected("Anna added a new photo to her album.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setArgument("userGender", "unknown", errorCode)
                                .setArgument("userName", "Anonymous", errorCode)
                                .setExpected("Anonymous added a new photo to their album.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    count = 13;
    test.adoptInstead(testBuilder.setArgument("photoCount", count, errorCode)
                                .setArgument("userGender", "masculine", errorCode)
                                .setArgument("userName", "John", errorCode)
                                .setExpected("John added 13 photos to his album.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setArgument("userGender", "feminine", errorCode)
                                .setArgument("userName", "Anna", errorCode)
                                .setExpected("Anna added 13 photos to her album.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setArgument("userGender", "unknown", errorCode)
                                .setArgument("userName", "Anonymous", errorCode)
                                .setExpected("Anonymous added 13 photos to their album.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testSimpleLocalVariable(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test;
    LocalPointer<Calendar> cal(Calendar::createInstance(errorCode));
    CHECK_ERROR(errorCode);
    // November 23, 2022 at 7:42:37.123 PM
    cal->set(2022, Calendar::NOVEMBER, 23, 19, 42, 37);
    UDate TEST_DATE = cal->getTime(errorCode);
    CHECK_ERROR(errorCode);

    testBuilder.setPattern("let $expDate = {$expDate :datetime skeleton=yMMMdE}\n\
                            {Your tickets expire on {$expDate}.}");

    int64_t count = 1;
    test.adoptInstead(testBuilder.setArgument("count", count, errorCode)
                      .setLocale(Locale("en"), errorCode)
                      .setDateArgument("expDate", TEST_DATE, errorCode)
                      .setExpected("Your tickets expire on Wed, Nov 23, 2022.")
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testLocalVariableWithSelect(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test;
    LocalPointer<Calendar> cal(Calendar::createInstance(errorCode));
    CHECK_ERROR(errorCode);
    // November 23, 2022 at 7:42:37.123 PM
    cal->set(2022, Calendar::NOVEMBER, 23, 19, 42, 37);
    UDate TEST_DATE = cal->getTime(errorCode);
    CHECK_ERROR(errorCode);

    testBuilder.setPattern("let $expDate = {$expDate :datetime skeleton=yMMMdE}\n\
                match {$count :plural}\n\
                when 1 {Your ticket expires on {$expDate}.}\n\
                when * {Your {$count} tickets expire on {$expDate}.}\n");

    int64_t count = 1;
    test.adoptInstead(testBuilder.setArgument("count", count, errorCode)
                      .setLocale(Locale("en"), errorCode)
                      .setDateArgument("expDate", TEST_DATE, errorCode)
                      .setExpected("Your ticket expires on Wed, Nov 23, 2022.")
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    count = 3;
    test.adoptInstead(testBuilder.setArgument("count", count, errorCode)
                                .setExpected("Your 3 tickets expire on Wed, Nov 23, 2022.")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testDateFormat(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    LocalPointer<Calendar> cal(Calendar::createInstance(errorCode));
    CHECK_ERROR(errorCode);

    cal->set(2022, Calendar::OCTOBER, 27, 0, 0, 0);
    UDate expiration = cal->getTime(errorCode);
    CHECK_ERROR(errorCode);

    LocalPointer<TestCase> test(testBuilder.setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                                .setLocale(Locale("en"), errorCode)
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
  This test would require the calendar to be passed as a UObject* with the datetime formatter
  doing an RTTI check -- however, that would be awkward, since it would have to check the tag for each
  possible subclass of `Calendar`. datetime currently has no support for formatting any object argument

    cal.adoptInstead(new GregorianCalendar(2022, Calendar::OCTOBER, 27, errorCode));
    if (cal.isValid()) {
        test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                          .setExpected("Your card expires on Thu, Oct 27, 2022!")
                          .setArgument("exp", cal.orphan(), errorCode)
                          .build(errorCode));
        TestUtils::runTestCase(*this, *test, errorCode);
    }
*/

    // Implied function based on type of the object to format
    test.adoptInstead(testBuilder.setPattern("{Your card expires on {$exp}!}")
                      .setExpected("Your card expires on 10/27/22, 12:00\u202FAM!")
                      .setDateArgument("exp", expiration, errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testPlural(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    UnicodeString message = "match {$count :plural}\n\
                when 1 {You have one notification.}\n           \
                when * {You have {$count} notifications.}\n";

    int64_t count = 1;
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
                      .setArgument("count", "1", errorCode)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    count = 42;
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

TemperatureFormatter::TemperatureFormatter(const Locale& l, TemperatureFormatterFactory& c, UErrorCode& errorCode) : locale(l), counter(c) {
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
 
Formatter* TemperatureFormatterFactory::createFormatter(const Locale& locale, UErrorCode& errorCode) {
    NULL_ON_ERROR(errorCode);

    LocalPointer<Formatter> result(new TemperatureFormatter(locale, *this, errorCode));
    NULL_ON_ERROR(errorCode)
    return result.orphan();
}

void TemperatureFormatter::format(FormattingContext& context, UErrorCode& errorCode) const {
    CHECK_ERROR(errorCode);

    // Argument must be present
    if (!context.hasFormattableInput()) {
        context.setFormattingError("temp", errorCode);
        return;
    }
    // Assume arg is not-yet-formatted
    const Formattable& toFormat = context.getFormattableInput();

    counter.formatCount++;

    UnicodeString unit;
    bool unitExists = context.getStringOption(UnicodeString("unit"), unit);
    U_ASSERT(unitExists);
    UnicodeString skeleton;
    bool skeletonExists = context.getStringOption(UnicodeString("skeleton"), skeleton);

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
            return;
        }
        cachedFormatters->put(unit, realNfCached, errorCode);
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
            context.setOutput(UnicodeString());
            return;
        }
    }
    context.setOutput(std::move(result));
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

void putFormattableArg(Hashtable& arguments, const UnicodeString& k, int64_t arg, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    Formattable* valPtr(new Formattable(arg));
    if (valPtr == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    } else {
        arguments.put(k, valPtr, errorCode);
    }
}

void putFormattableArg(Hashtable& arguments, const UnicodeString& k, double arg, UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    Formattable* valPtr(new Formattable(arg));
    if (valPtr == nullptr) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
    } else {
        arguments.put(k, valPtr, errorCode);
    }
}

void TestMessageFormat2::testFormatterIsCreatedOnce(IcuTestErrorCode& errorCode) {
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
    mfBuilder->setPattern(message).setFunctionRegistry(reg.getAlias());
    UParseError parseError;
    LocalPointer<MessageFormatter> mf(mfBuilder->build(parseError, errorCode));
    UnicodeString result;
    UnicodeString countKey("count");
    UnicodeString unitKey("unit");

    const int64_t maxCount = 10;
    char expected[20];
    LocalPointer<MessageArguments::Builder> argumentsBuilder(MessageArguments::builder(errorCode));
    LocalPointer<MessageArguments> arguments;
    CHECK_ERROR(errorCode);
    for (int64_t count = 0; count < maxCount; count++) {
        snprintf(expected, sizeof(expected), "Testing %ld°C.", count);

        argumentsBuilder->addInt64(countKey, count, errorCode);
        argumentsBuilder->add(unitKey, "C", errorCode);
        arguments.adoptInstead(argumentsBuilder->build(errorCode));
        CHECK_ERROR(errorCode);

        mf->formatToString(*arguments, errorCode, result);
        assertEquals("temperature formatter", expected, result);
        result.remove();

        snprintf(expected, sizeof(expected), "Testing %ld°F.", count);
        argumentsBuilder->addInt64(countKey, count, errorCode);
        argumentsBuilder->add(unitKey, "F", errorCode);
        arguments.adoptInstead(argumentsBuilder->build(errorCode));
        CHECK_ERROR(errorCode);
        
        mf->formatToString(*arguments, errorCode, result);
        assertEquals("temperature formatter", expected, result);
        result.remove();
    }

    assertEquals("cached formatter", 1, counter->constructCount);
    assertEquals("cached formatter", (int64_t) maxCount * 2, (int64_t) counter->formatCount);
    assertEquals("cached formatter", 1, counter->fFormatterCount);
    assertEquals("cached formatter", 1, counter->cFormatterCount);

    result.remove();
    argumentsBuilder->addDouble(countKey, 12.0, errorCode);
    argumentsBuilder->add(unitKey, "C", errorCode);
    arguments.adoptInstead(argumentsBuilder->build(errorCode));
    CHECK_ERROR(errorCode);
    mf->formatToString(*arguments, errorCode, result);
    assertEquals("cached formatter", "Testing 12°C.", result);

    result.remove();
    argumentsBuilder->addDouble(countKey, 12.5, errorCode);
    argumentsBuilder->add(unitKey, "F", errorCode);
    arguments.adoptInstead(argumentsBuilder->build(errorCode));
    CHECK_ERROR(errorCode);
    mf->formatToString(*arguments, errorCode, result);
    assertEquals("cached formatter", "Testing 12.50°F.", result);

    result.remove();
    argumentsBuilder->addDouble(countKey, 12.54, errorCode);
    argumentsBuilder->add(unitKey, "C", errorCode);
    arguments.adoptInstead(argumentsBuilder->build(errorCode));
    CHECK_ERROR(errorCode);
    mf->formatToString(*arguments, errorCode, result);
    assertEquals("cached formatter", "Testing 12.54°C.", result);

    result.remove();
    argumentsBuilder->addDouble(countKey, 12.54321, errorCode);
    argumentsBuilder->add(unitKey, "F", errorCode);
    arguments.adoptInstead(argumentsBuilder->build(errorCode));

    CHECK_ERROR(errorCode);
    mf->formatToString(*arguments, errorCode, result);
    assertEquals("cached formatter", "Testing 12.54°F.", result);

    // Check skeleton
    message = "{Testing {$count :temp unit=$unit skeleton=|.0/w|}.}";
    mfBuilder->setPattern(message);
    mf.adoptInstead(mfBuilder->build(parseError, errorCode));

    result.remove();
    argumentsBuilder->addDouble(countKey, 12.0, errorCode);
    argumentsBuilder->add(unitKey, "C", errorCode);
    arguments.adoptInstead(argumentsBuilder->build(errorCode));

    CHECK_ERROR(errorCode);
    mf->formatToString(*arguments, errorCode, result);
    assertEquals("cached formatter", "Testing 12°C.", result);

    result.remove();
    argumentsBuilder->addDouble(countKey, 12.5, errorCode);
    argumentsBuilder->add(unitKey, "F", errorCode);
    arguments.adoptInstead(argumentsBuilder->build(errorCode));

    CHECK_ERROR(errorCode);
    mf->formatToString(*arguments, errorCode, result);
    assertEquals("cached formatter", "Testing 12.5°F.", result);

    result.remove();
    argumentsBuilder->addDouble(countKey, 12.54, errorCode);
    argumentsBuilder->add(unitKey, "C", errorCode);
    arguments.adoptInstead(argumentsBuilder->build(errorCode));

    CHECK_ERROR(errorCode);
    mf->formatToString(*arguments, errorCode, result);
    assertEquals("cached formatter", "Testing 12.5°C.", result);

    result.remove();
    argumentsBuilder->addDouble(countKey, 12.54321, errorCode);
    argumentsBuilder->add(unitKey, "F", errorCode);
    arguments.adoptInstead(argumentsBuilder->build(errorCode));
    CHECK_ERROR(errorCode);
    mf->formatToString(*arguments, errorCode, result);
    assertEquals("cached formatter", "Testing 12.5°F.", result);

}

void TestMessageFormat2::testPluralWithOffset(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    UnicodeString message = "match {$count :plural offset=2}\n\
                  when 1 {Anna}\n\
                  when 2 {Anna and Bob}\n\
                  when one {Anna, Bob, and {$count :number offset=2} other guest}\n\
                  when * {Anna, Bob, and {$count :number offset=2} other guests}\n";

    testBuilder.setPattern(message);
    testBuilder.setName("plural with offset");

    LocalPointer<TestCase> test(testBuilder.setExpected("Anna")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Anna and Bob")
                          .setArgument("count", (int64_t) 2, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Anna, Bob, and 1 other guest")
                          .setArgument("count", (int64_t) 3, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Anna, Bob, and 2 other guests")
                          .setArgument("count", (int64_t) 4, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Anna, Bob, and 10 other guests")
                          .setArgument("count", (int64_t) 12, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testPluralWithOffsetAndLocalVar(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
  
    // $foo should "inherit" the offset
    UnicodeString message = "let $foo = {$count :number offset=2}\
                match {$foo :plural}\n                                 \
                when 1 {Anna}\n                                        \
                when 2 {Anna and Bob}\n                                \
                when one {Anna, Bob, and {$foo} other guest}\n         \
                when * {Anna, Bob, and {$foo} other guests}\n";

    testBuilder.setPattern(message);
    testBuilder.setName("plural with offset and local var");

    LocalPointer<TestCase> test(testBuilder.setExpected("Anna")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Anna and Bob")
                          .setArgument("count", (int64_t) 2, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Anna, Bob, and 1 other guest")
                          .setArgument("count", (int64_t) 3, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Anna, Bob, and 2 other guests")
                          .setArgument("count", (int64_t) 4, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Anna, Bob, and 10 other guests")
                          .setArgument("count", (int64_t) 12, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
  
    message = "let $foo = {$amount :number skeleton=|.00/w|}\n\
                match {$foo :plural}\n\
                when 1 {Last dollar}\n\
                when one {{$foo} dollar}\n\
                when * {{$foo} dollars}\n";
    testBuilder.setPattern(message);
    test.adoptInstead(testBuilder.setExpected("Last dollar")
                          .setArgument("amount", (int64_t) 1, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("2 dollars")
                          .setArgument("amount", (int64_t) 2, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("3 dollars")
                          .setArgument("amount", (int64_t) 3, errorCode)
                          .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testDeclareBeforeUse(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {

    UnicodeString message = "let $foo = {$baz :number}\n\
                 let $bar = {$foo}\n                    \
                 let $baz = {$bar}\n                    \
                 {The message uses {$baz} and works}\n";
    testBuilder.setPattern(message);
    testBuilder.setName("declare-before-use");

    LocalPointer<TestCase> test(testBuilder.setExpected("The message uses {$baz} and works")
                                .setExpectedError(U_UNRESOLVED_VARIABLE_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testVariableOptionsInSelector(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    UnicodeString message = "match {$count :plural offset=$delta}\n\
                when 1 {A}\n\
                when 2 {A and B}\n\
                when one {A, B, and {$count :number offset=$delta} more character}\n\
                when * {A, B, and {$count :number offset=$delta} more characters}\n";

    testBuilder.setPattern(message);
    testBuilder.setName("variable options in selector");
    testBuilder.setExpectSuccess();

    LocalPointer<TestCase> test(testBuilder.setExpected("A")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A and B")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A, B, and 1 more character")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A, B, and 5 more characters")
                                .setArgument("count", (int64_t) 7, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    message = "match {$count :plural offset=$delta}\n\
                  when 1 {Exactly 1}\n\
                  when 2 {Exactly 2}\n\
                  when * {Count = {$count :number offset=$delta} and delta={$delta}.}\n";
    testBuilder.setPattern(message);

    test.adoptInstead(testBuilder.setExpected("Exactly 1")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setArgument("delta", (int64_t) 0, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 1")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setArgument("delta", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 1")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 2")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .setArgument("delta", (int64_t) 0, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 2")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .setArgument("delta", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 2")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 3 and delta=0.")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .setArgument("delta", (int64_t) 0, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 2 and delta=1.")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .setArgument("delta", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 1 and delta=2.")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 23 and delta=0.")
                                .setArgument("count", (int64_t) 23, errorCode)
                                .setArgument("delta", (int64_t) 0, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 22 and delta=1.")
                                .setArgument("count", (int64_t) 23, errorCode)
                                .setArgument("delta", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 21 and delta=2.")
                                .setArgument("count", (int64_t) 23, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}

void TestMessageFormat2::testVariableOptionsInSelectorWithLocalVar(TestCase::Builder& testBuilder, IcuTestErrorCode& errorCode) {
    CHECK_ERROR(errorCode);

    UnicodeString messageFix = "let $offCount = {$count :number offset=2}\n\
                match {$offCount :plural}\n\
                when 1 {A}\n\
                when 2 {A and B}\n\
                when one {A, B, and {$offCount} more character}\n\
                when * {A, B, and {$offCount} more characters}\n";

    testBuilder.setPattern(messageFix);
    testBuilder.setName("variable options in selector with local var");
    testBuilder.setExpectSuccess();

    LocalPointer<TestCase> test(testBuilder.setExpected("A")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A and B")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A, B, and 1 more character")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A, B, and 5 more characters")
                                .setArgument("count", (int64_t) 7, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    UnicodeString messageVar = "let $offCount = {$count :number offset=$delta}\n\
                match {$offCount :plural}\n\
                when 1 {A}\n\
                when 2 {A and B}\n\
                when one {A, B, and {$offCount} more character}\n\
                when * {A, B, and {$offCount} more characters}\n";
    testBuilder.setPattern(messageVar);

    test.adoptInstead(testBuilder.setExpected("A")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A and B")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A, B, and 1 more character")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("A, B, and 5 more characters")
                                .setArgument("count", (int64_t) 7, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    UnicodeString messageVar2 = "let $offCount = {$count :number offset=$delta}\n\
                match {$offCount :plural}\n\
                when 1 {Exactly 1}\n\
                when 2 {Exactly 2}\n\
                when * {Count = {$count}, OffCount = {$offCount}, and delta={$delta}.}\n";
    testBuilder.setPattern(messageVar2);
    test.adoptInstead(testBuilder.setExpected("Exactly 1")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setArgument("delta", (int64_t) 0, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 1")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setArgument("delta", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 1")
                                .setArgument("count", (int64_t) 1, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Exactly 2")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .setArgument("delta", (int64_t) 0, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 2")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .setArgument("delta", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Exactly 2")
                                .setArgument("count", (int64_t) 2, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 3, OffCount = 3, and delta=0.")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .setArgument("delta", (int64_t) 0, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 3, OffCount = 2, and delta=1.")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .setArgument("delta", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 3, OffCount = 1, and delta=2.")
                                .setArgument("count", (int64_t) 3, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder.setExpected("Count = 23, OffCount = 23, and delta=0.")
                                .setArgument("count", (int64_t) 23, errorCode)
                                .setArgument("delta", (int64_t) 0, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 23, OffCount = 22, and delta=1.")
                                .setArgument("count", (int64_t) 23, errorCode)
                                .setArgument("delta", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    test.adoptInstead(testBuilder.setExpected("Count = 23, OffCount = 21, and delta=2.")
                                .setArgument("count", (int64_t) 23, errorCode)
                                .setArgument("delta", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
}


void TestMessageFormat2::featureTests() {
    IcuTestErrorCode errorCode(*this, "featureTests");

    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    testBuilder->setName("featureTests");

    testEmptyMessage(*testBuilder, errorCode);
    testPlainText(*testBuilder, errorCode);
    testPlaceholders(*testBuilder, errorCode);
    testArgumentMissing(*testBuilder, errorCode);
    testDefaultLocale(*testBuilder, errorCode);
    testSpecialPluralWithDecimals(*testBuilder, errorCode);
    testDefaultFunctionAndOptions(*testBuilder, errorCode);
    testSimpleSelection(*testBuilder, errorCode);
    testComplexSelection(*testBuilder, errorCode);
    testSimpleLocalVariable(*testBuilder, errorCode);
    testLocalVariableWithSelect(*testBuilder, errorCode);

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
