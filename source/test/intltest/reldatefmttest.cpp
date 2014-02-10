/*
*******************************************************************************
* Copyright (C) 2013-2014, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File RELDATEFMTTEST.CPP
*
*******************************************************************************
*/
#include <stdio.h>
#include <stdlib.h>

#include "intltest.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/localpointer.h"
#include "unicode/numfmt.h"
#include "unicode/reldatefmt.h"

#define LENGTHOF(array) (int32_t)(sizeof(array) / sizeof((array)[0]))

static const char *DirectionStr(UDateDirection direction);
static const char *RelativeUnitStr(UDateRelativeUnit unit);
static const char *AbsoluteUnitStr(UDateAbsoluteUnit unit);

typedef struct WithQuantityExpected {
    double value;
    UDateDirection direction;
    UDateRelativeUnit unit;
    const char *expected;
} WithQuantityExpected;

typedef struct WithoutQuantityExpected {
    UDateDirection direction;
    UDateAbsoluteUnit unit;
    const char *expected;
} WithoutQuantityExpected;

static WithQuantityExpected kEnglish[] = {
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_SECONDS, "in 0 seconds"},
        {0.5, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_SECONDS, "in 0.5 seconds"},
        {1.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_SECONDS, "in 1 second"},
        {2.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_SECONDS, "in 2 seconds"},
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MINUTES, "in 0 minutes"},
        {0.5, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MINUTES, "in 0.5 minutes"},
        {1.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MINUTES, "in 1 minute"},
        {2.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MINUTES, "in 2 minutes"},
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_HOURS, "in 0 hours"},
        {0.5, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_HOURS, "in 0.5 hours"},
        {1.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_HOURS, "in 1 hour"},
        {2.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_HOURS, "in 2 hours"},
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_DAYS, "in 0 days"},
        {0.5, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_DAYS, "in 0.5 days"},
        {1.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_DAYS, "in 1 day"},
        {2.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_DAYS, "in 2 days"},
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_WEEKS, "in 0 weeks"},
        {0.5, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_WEEKS, "in 0.5 weeks"},
        {1.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_WEEKS, "in 1 week"},
        {2.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_WEEKS, "in 2 weeks"},
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MONTHS, "in 0 months"},
        {0.5, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MONTHS, "in 0.5 months"},
        {1.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MONTHS, "in 1 month"},
        {2.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MONTHS, "in 2 months"},
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_YEARS, "in 0 years"},
        {0.5, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_YEARS, "in 0.5 years"},
        {1.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_YEARS, "in 1 year"},
        {2.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_YEARS, "in 2 years"},
                
        {0.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_SECONDS, "0 seconds ago"},
        {0.5, UDAT_DIRECTION_LAST, UDAT_RELATIVE_SECONDS, "0.5 seconds ago"},
        {1.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_SECONDS, "1 second ago"},
        {2.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_SECONDS, "2 seconds ago"},
        {0.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_MINUTES, "0 minutes ago"},
        {0.5, UDAT_DIRECTION_LAST, UDAT_RELATIVE_MINUTES, "0.5 minutes ago"},
        {1.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_MINUTES, "1 minute ago"},
        {2.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_MINUTES, "2 minutes ago"},
        {0.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_HOURS, "0 hours ago"},
        {0.5, UDAT_DIRECTION_LAST, UDAT_RELATIVE_HOURS, "0.5 hours ago"},
        {1.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_HOURS, "1 hour ago"},
        {2.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_HOURS, "2 hours ago"},
        {0.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_DAYS, "0 days ago"},
        {0.5, UDAT_DIRECTION_LAST, UDAT_RELATIVE_DAYS, "0.5 days ago"},
        {1.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_DAYS, "1 day ago"},
        {2.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_DAYS, "2 days ago"},
        {0.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_WEEKS, "0 weeks ago"},
        {0.5, UDAT_DIRECTION_LAST, UDAT_RELATIVE_WEEKS, "0.5 weeks ago"},
        {1.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_WEEKS, "1 week ago"},
        {2.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_WEEKS, "2 weeks ago"},
        {0.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_MONTHS, "0 months ago"},
        {0.5, UDAT_DIRECTION_LAST, UDAT_RELATIVE_MONTHS, "0.5 months ago"},
        {1.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_MONTHS, "1 month ago"},
        {2.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_MONTHS, "2 months ago"},
        {0.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_YEARS, "0 years ago"},
        {0.5, UDAT_DIRECTION_LAST, UDAT_RELATIVE_YEARS, "0.5 years ago"},
        {1.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_YEARS, "1 year ago"},
        {2.0, UDAT_DIRECTION_LAST, UDAT_RELATIVE_YEARS, "2 years ago"} 
};

static WithQuantityExpected kEnglishDecimal[] = {
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_SECONDS, "in 0.0 seconds"},
        {0.5, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_SECONDS, "in 0.5 seconds"},
        {1.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_SECONDS, "in 1.0 seconds"},
        {2.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_SECONDS, "in 2.0 seconds"}
};

static WithQuantityExpected kSerbian[] = {
        {0.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MONTHS, "\\u0437\\u0430 0 \\u043c\\u0435\\u0441\\u0435\\u0446\\u0438"},
        {1.2, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MONTHS, "\\u0437\\u0430 1,2 \\u043c\\u0435\\u0441\\u0435\\u0446\\u0430"},
        {21.0, UDAT_DIRECTION_NEXT, UDAT_RELATIVE_MONTHS, "\\u0437\\u0430 21 \\u043c\\u0435\\u0441\\u0435\\u0446"}
};

static WithoutQuantityExpected kEnglishNoQuantity[] = {
        {UDAT_DIRECTION_NEXT_2, UDAT_ABSOLUTE_DAY, ""},
                
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_DAY, "tomorrow"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_WEEK, "next week"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_MONTH, "next month"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_YEAR, "next year"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_MONDAY, "next Monday"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_TUESDAY, "next Tuesday"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_WEDNESDAY, "next Wednesday"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_THURSDAY, "next Thursday"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_FRIDAY, "next Friday"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_SATURDAY, "next Saturday"},
        {UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_SUNDAY, "next Sunday"},
        
        {UDAT_DIRECTION_LAST_2, UDAT_ABSOLUTE_DAY, ""},
        
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_DAY, "yesterday"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_WEEK, "last week"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_MONTH, "last month"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_YEAR, "last year"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_MONDAY, "last Monday"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_TUESDAY, "last Tuesday"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_WEDNESDAY, "last Wednesday"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_THURSDAY, "last Thursday"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_FRIDAY, "last Friday"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_SATURDAY, "last Saturday"},
        {UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_SUNDAY, "last Sunday"},
         
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_DAY, "today"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_WEEK, "this week"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_MONTH, "this month"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_YEAR, "this year"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_MONDAY, "this Monday"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_TUESDAY, "this Tuesday"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_WEDNESDAY, "this Wednesday"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_THURSDAY, "this Thursday"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_FRIDAY, "this Friday"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_SATURDAY, "this Saturday"},
        {UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_SUNDAY, "this Sunday"},
        
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_DAY, "day"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_WEEK, "week"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_MONTH, "month"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_YEAR, "year"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_MONDAY, "Monday"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_TUESDAY, "Tuesday"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_WEDNESDAY, "Wednesday"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_THURSDAY, "Thursday"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_FRIDAY, "Friday"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_SATURDAY, "Saturday"},
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_SUNDAY, "Sunday"},
        
        {UDAT_DIRECTION_PLAIN, UDAT_ABSOLUTE_NOW, "now"}
};

static WithoutQuantityExpected kSpanishNoQuantity[] = {
        {UDAT_DIRECTION_NEXT_2, UDAT_ABSOLUTE_DAY, "pasado ma\\u00F1ana"},
        {UDAT_DIRECTION_LAST_2, UDAT_ABSOLUTE_DAY, "antes de ayer"}
};

class RelativeDateTimeFormatterTest : public IntlTest {
public:
    RelativeDateTimeFormatterTest() {
    }

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=0);
private:
    void TestEnglish();
    void TestSerbian();
    void TestEnglishNoQuantity();
    void TestSpanishNoQuantity();
    void TestFormatWithQuantityIllegalArgument();
    void TestFormatWithoutQuantityIllegalArgument();
    void TestCustomNumberFormat();
    void TestCombineDateAndTime();
    void RunTest(
            const Locale& locale,
            const WithQuantityExpected* expectedResults,
            int32_t expectedResultLength);
    void RunTest(
            const Locale& locale,
            const WithoutQuantityExpected* expectedResults,
            int32_t expectedResultLength);
    void RunTest(
            const RelativeDateTimeFormatter& fmt,
            const WithQuantityExpected* expectedResults,
            int32_t expectedResultLength,
            const char *description);
    void RunTest(
            const RelativeDateTimeFormatter& fmt,
            const WithoutQuantityExpected* expectedResults,
            int32_t expectedResultLength,
            const char *description);
    void CheckExpectedResult(
            const RelativeDateTimeFormatter& fmt,
            const WithQuantityExpected& expectedResult,
            const char* description);
    void CheckExpectedResult(
            const RelativeDateTimeFormatter& fmt,
            const WithoutQuantityExpected& expectedResult,
            const char* description);
    void VerifyIllegalArgument(
            const RelativeDateTimeFormatter& fmt,
            UDateDirection direction,
            UDateRelativeUnit unit);
    void VerifyIllegalArgument(
            const RelativeDateTimeFormatter& fmt,
            UDateDirection direction,
            UDateAbsoluteUnit unit);
};

void RelativeDateTimeFormatterTest::runIndexedTest(
        int32_t index, UBool exec, const char *&name, char *) {
    if (exec) {
        logln("TestSuite RelativeDateTimeFormatterTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestEnglish);
    TESTCASE_AUTO(TestSerbian);
    TESTCASE_AUTO(TestEnglishNoQuantity);
    TESTCASE_AUTO(TestSpanishNoQuantity);
    TESTCASE_AUTO(TestFormatWithQuantityIllegalArgument);
    TESTCASE_AUTO(TestFormatWithoutQuantityIllegalArgument);
    TESTCASE_AUTO(TestCustomNumberFormat);
    TESTCASE_AUTO(TestCombineDateAndTime);
    TESTCASE_AUTO_END;
}

void RelativeDateTimeFormatterTest::TestEnglish() {
    RunTest("en", kEnglish, LENGTHOF(kEnglish));
}

void RelativeDateTimeFormatterTest::TestSerbian() {
    RunTest("sr", kSerbian, LENGTHOF(kSerbian));
}

void RelativeDateTimeFormatterTest::TestEnglishNoQuantity() {
    RunTest("en", kEnglishNoQuantity, LENGTHOF(kEnglishNoQuantity));
}

void RelativeDateTimeFormatterTest::TestSpanishNoQuantity() {
    RunTest("es", kSpanishNoQuantity, LENGTHOF(kSpanishNoQuantity));
}

void RelativeDateTimeFormatterTest::TestFormatWithQuantityIllegalArgument() {
    UErrorCode status = U_ZERO_ERROR;
    RelativeDateTimeFormatter fmt("en", status);
    if (U_FAILURE(status)) {
        dataerrln("Failure creating format object - %s", u_errorName(status));
        return;
    }
    VerifyIllegalArgument(fmt, UDAT_DIRECTION_PLAIN, UDAT_RELATIVE_DAYS);
    VerifyIllegalArgument(fmt, UDAT_DIRECTION_THIS, UDAT_RELATIVE_DAYS);
}

void RelativeDateTimeFormatterTest::TestFormatWithoutQuantityIllegalArgument() {
    UErrorCode status = U_ZERO_ERROR;
    RelativeDateTimeFormatter fmt("en", status);
    if (U_FAILURE(status)) {
        dataerrln("Failure creating format object - %s", u_errorName(status));
        return;
    }
    VerifyIllegalArgument(fmt, UDAT_DIRECTION_LAST, UDAT_ABSOLUTE_NOW);
    VerifyIllegalArgument(fmt, UDAT_DIRECTION_NEXT, UDAT_ABSOLUTE_NOW);
    VerifyIllegalArgument(fmt, UDAT_DIRECTION_THIS, UDAT_ABSOLUTE_NOW);
}

void RelativeDateTimeFormatterTest::TestCustomNumberFormat() {
    NumberFormat *nf;
    UErrorCode status = U_ZERO_ERROR;
    {
        RelativeDateTimeFormatter fmt("en", status);
        if (U_FAILURE(status)) {
            dataerrln(
                    "Failure creating format object - %s", u_errorName(status));
            return;
        }
        nf = (NumberFormat *) fmt.getNumberFormat().clone();
    }
    nf->setMinimumFractionDigits(1);
    nf->setMaximumFractionDigits(1);
    RelativeDateTimeFormatter fmt("en", nf, status);

    // Test copy constructor.
    RelativeDateTimeFormatter fmt2(fmt);
    RunTest(fmt2, kEnglishDecimal, LENGTHOF(kEnglishDecimal), "en decimal digits");

    // Test assignment
    fmt = RelativeDateTimeFormatter("es", status);
    RunTest(fmt, kSpanishNoQuantity, LENGTHOF(kSpanishNoQuantity), "assignment operator");

}

void RelativeDateTimeFormatterTest::TestCombineDateAndTime() {
    UErrorCode status = U_ZERO_ERROR;
    RelativeDateTimeFormatter fmt("en", status);
    if (U_FAILURE(status)) {
        dataerrln("Failure creating format object - %s", u_errorName(status));
        return;
    }
    UnicodeString actual;
    fmt.combineDateAndTime(
        UnicodeString("yesterday"),
        UnicodeString("3:50"),
        actual,
        status);
    UnicodeString expected("yesterday, 3:50");
    if (expected != actual) {
        errln("Expected "+expected+", got "+actual);
    }
}
    

void RelativeDateTimeFormatterTest::RunTest(
        const Locale& locale,
        const WithQuantityExpected* expectedResults,
        int32_t expectedResultLength) {
    UErrorCode status = U_ZERO_ERROR;
    RelativeDateTimeFormatter fmt(locale, status);
    if (U_FAILURE(status)) {
        dataerrln("Unable to create format object - %s", u_errorName(status));
        return;
   }
    RunTest(fmt, expectedResults, expectedResultLength, locale.getName());
}

void RelativeDateTimeFormatterTest::RunTest(
        const Locale& locale,
        const WithoutQuantityExpected* expectedResults,
        int32_t expectedResultLength) {
    UErrorCode status = U_ZERO_ERROR;
    RelativeDateTimeFormatter fmt(locale, status);
    if (U_FAILURE(status)) {
        dataerrln("Unable to create format object - %s", u_errorName(status));
        return;
    }
    RunTest(fmt, expectedResults, expectedResultLength, locale.getName());
}

void RelativeDateTimeFormatterTest::RunTest(
        const RelativeDateTimeFormatter& fmt,
        const WithQuantityExpected* expectedResults,
        int32_t expectedResultLength,
        const char *description) {
    for (int32_t i = 0; i < expectedResultLength; ++i) {
        CheckExpectedResult(fmt, expectedResults[i], description);
    }
}

void RelativeDateTimeFormatterTest::RunTest(
        const RelativeDateTimeFormatter& fmt,
        const WithoutQuantityExpected* expectedResults,
        int32_t expectedResultLength,
        const char *description) {
    for (int32_t i = 0; i < expectedResultLength; ++i) {
        CheckExpectedResult(fmt, expectedResults[i], description);
    }
}

void RelativeDateTimeFormatterTest::CheckExpectedResult(
        const RelativeDateTimeFormatter& fmt,
        const WithQuantityExpected& expectedResult,
        const char* description) {
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString actual;
    fmt.format(expectedResult.value, expectedResult.direction, expectedResult.unit, actual, status);
    UnicodeString expected(expectedResult.expected, -1, US_INV);
    expected = expected.unescape();
    char buffer[256];
    sprintf(
            buffer,
            "%s, %f, %s, %s",
            description,
            expectedResult.value,
            DirectionStr(expectedResult.direction),
            RelativeUnitStr(expectedResult.unit));
    if (actual != expected) {
        errln(UnicodeString("Fail: Expected: ") + expected
                + ", Got: " + actual
                + ", For: " + buffer);
    }
}

void RelativeDateTimeFormatterTest::CheckExpectedResult(
        const RelativeDateTimeFormatter& fmt,
        const WithoutQuantityExpected& expectedResult,
        const char* description) {
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString actual;
    fmt.format(expectedResult.direction, expectedResult.unit, actual, status);
    UnicodeString expected(expectedResult.expected, -1, US_INV);
    expected = expected.unescape();
    char buffer[256];
    sprintf(
            buffer,
            "%s, %s, %s",
            description,
            DirectionStr(expectedResult.direction),
            AbsoluteUnitStr(expectedResult.unit));
    if (actual != expected) {
        errln(UnicodeString("Fail: Expected: ") + expected
                + ", Got: " + actual
                + ", For: " + buffer);
    }
}

void RelativeDateTimeFormatterTest::VerifyIllegalArgument(
        const RelativeDateTimeFormatter& fmt,
        UDateDirection direction,
        UDateRelativeUnit unit) {
    UnicodeString appendTo;
    UErrorCode status = U_ZERO_ERROR;
    fmt.format(1.0, direction, unit, appendTo, status);
    if (status != U_ILLEGAL_ARGUMENT_ERROR) {
        errln("Expected U_ILLEGAL_ARGUMENT_ERROR, got %s", u_errorName(status));
    }
}

void RelativeDateTimeFormatterTest::VerifyIllegalArgument(
        const RelativeDateTimeFormatter& fmt,
        UDateDirection direction,
        UDateAbsoluteUnit unit) {
    UnicodeString appendTo;
    UErrorCode status = U_ZERO_ERROR;
    fmt.format(direction, unit, appendTo, status);
    if (status != U_ILLEGAL_ARGUMENT_ERROR) {
        errln("Expected U_ILLEGAL_ARGUMENT_ERROR, got %s", u_errorName(status));
    }
}

static const char *kLast2 = "Last_2";
static const char *kLast = "Last";
static const char *kThis = "This";
static const char *kNext = "Next";
static const char *kNext2 = "Next_2";
static const char *kPlain = "Plain";

static const char *kSeconds = "Seconds";
static const char *kMinutes = "Minutes";
static const char *kHours = "Hours";
static const char *kDays = "Days";
static const char *kWeeks = "Weeks";
static const char *kMonths = "Months";
static const char *kYears = "Years";

static const char *kSunday = "Sunday";
static const char *kMonday = "Monday";
static const char *kTuesday = "Tuesday";
static const char *kWednesday = "Wednesday";
static const char *kThursday = "Thursday";
static const char *kFriday = "Friday";
static const char *kSaturday = "Saturday";
static const char *kDay = "Day";
static const char *kWeek = "Week";
static const char *kMonth = "Month";
static const char *kYear = "Year";
static const char *kNow = "Now";

static const char *kUndefined = "Undefined";

static const char *DirectionStr(
        UDateDirection direction) {
    switch (direction) {
        case UDAT_DIRECTION_LAST_2:
            return kLast2;
        case UDAT_DIRECTION_LAST:
            return kLast;
        case UDAT_DIRECTION_THIS:
            return kThis;
        case UDAT_DIRECTION_NEXT:
            return kNext;
        case UDAT_DIRECTION_NEXT_2:
            return kNext2;
        case UDAT_DIRECTION_PLAIN:
            return kPlain;
        default:
            return kUndefined;
    }
    return kUndefined;
}

static const char *RelativeUnitStr(
        UDateRelativeUnit unit) {
    switch (unit) {
        case UDAT_RELATIVE_SECONDS:
            return kSeconds;
        case UDAT_RELATIVE_MINUTES:
            return kMinutes;
        case UDAT_RELATIVE_HOURS:
            return kHours;
        case UDAT_RELATIVE_DAYS:
            return kDays;
        case UDAT_RELATIVE_WEEKS:
            return kWeeks;
        case UDAT_RELATIVE_MONTHS:
            return kMonths;
        case UDAT_RELATIVE_YEARS:
            return kYears;
        default:
            return kUndefined;
    }
    return kUndefined;
}

static const char *AbsoluteUnitStr(
        UDateAbsoluteUnit unit) {
    switch (unit) {
        case UDAT_ABSOLUTE_SUNDAY:
            return kSunday;
        case UDAT_ABSOLUTE_MONDAY:
            return kMonday;
        case UDAT_ABSOLUTE_TUESDAY:
            return kTuesday;
        case UDAT_ABSOLUTE_WEDNESDAY:
            return kWednesday;
        case UDAT_ABSOLUTE_THURSDAY:
            return kThursday;
        case UDAT_ABSOLUTE_FRIDAY:
            return kFriday;
        case UDAT_ABSOLUTE_SATURDAY:
            return kSaturday;
        case UDAT_ABSOLUTE_DAY:
            return kDay;
        case UDAT_ABSOLUTE_WEEK:
            return kWeek;
        case UDAT_ABSOLUTE_MONTH:
            return kMonth;
        case UDAT_ABSOLUTE_YEAR:
            return kYear;
        case UDAT_ABSOLUTE_NOW:
            return kNow;
        default:
            return kUndefined;
    }
    return kUndefined;
}

extern IntlTest *createRelativeDateTimeFormatterTest() {
    return new RelativeDateTimeFormatterTest();
}

#endif
