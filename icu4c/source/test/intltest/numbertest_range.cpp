// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "numbertest.h"
#include "unicode/numberrangeformatter.h"

#include <cmath>
#include <numparse_affixes.h>

// Horrible workaround for the lack of a status code in the constructor...
// (Also affects numbertest_api.cpp)
UErrorCode globalNumberRangeFormatterTestStatus = U_ZERO_ERROR;

NumberRangeFormatterTest::NumberRangeFormatterTest()
        : NumberRangeFormatterTest(globalNumberRangeFormatterTestStatus) {
}

NumberRangeFormatterTest::NumberRangeFormatterTest(UErrorCode& status)
        : USD(u"USD", status),
          GBP(u"GBP", status),
          PTE(u"PTE", status) {

    // Check for error on the first MeasureUnit in case there is no data
    LocalPointer<MeasureUnit> unit(MeasureUnit::createMeter(status));
    if (U_FAILURE(status)) {
        dataerrln("%s %d status = %s", __FILE__, __LINE__, u_errorName(status));
        return;
    }
    METER = *unit;

    KILOMETER = *LocalPointer<MeasureUnit>(MeasureUnit::createKilometer(status));
    FAHRENHEIT = *LocalPointer<MeasureUnit>(MeasureUnit::createFahrenheit(status));
    KELVIN = *LocalPointer<MeasureUnit>(MeasureUnit::createKelvin(status));
}

void NumberRangeFormatterTest::runIndexedTest(int32_t index, UBool exec, const char*& name, char*) {
    if (exec) {
        logln("TestSuite NumberRangeFormatterTest: ");
    }
    TESTCASE_AUTO_BEGIN;
        TESTCASE_AUTO(testSanity);
        TESTCASE_AUTO(testBasic);
        TESTCASE_AUTO(testCollapse);
        TESTCASE_AUTO(testIdentity);
        TESTCASE_AUTO(testDifferentFormatters);
    TESTCASE_AUTO_END;
}

void NumberRangeFormatterTest::testSanity() {
    IcuTestErrorCode status(*this, "testSanity");
    LocalizedNumberRangeFormatter lnrf1 = NumberRangeFormatter::withLocale("en-us");
    LocalizedNumberRangeFormatter lnrf2 = NumberRangeFormatter::with().locale("en-us");
    assertEquals("Formatters should have same behavior 1",
        lnrf1.formatFormattableRange(4, 6, status).toString(status),
        lnrf2.formatFormattableRange(4, 6, status).toString(status));
}

void NumberRangeFormatterTest::testBasic() {
    assertFormatRange(
        u"Basic",
        NumberRangeFormatter::with(),
        Locale("en-us"),
        u"1–5",
        u"~5",
        u"~5",
        u"0–3",
        u"~0",
        u"3–3,000",
        u"3,000–5,000",
        u"4,999–5,001",
        u"~5,000",
        u"5,000–5,000,000");

    assertFormatRange(
        u"Basic with units",
        NumberRangeFormatter::with()
            .numberFormatterBoth(NumberFormatter::with().unit(METER)),
        Locale("en-us"),
        u"1–5 m",
        u"~5 m",
        u"~5 m",
        u"0–3 m",
        u"~0 m",
        u"3–3,000 m",
        u"3,000–5,000 m",
        u"4,999–5,001 m",
        u"~5,000 m",
        u"5,000–5,000,000 m");

    assertFormatRange(
        u"Basic with different units",
        NumberRangeFormatter::with()
            .numberFormatterFirst(NumberFormatter::with().unit(METER))
            .numberFormatterSecond(NumberFormatter::with().unit(KILOMETER)),
        Locale("en-us"),
        u"1 m – 5 km",
        u"5 m – 5 km",
        u"5 m – 5 km",
        u"0 m – 3 km",
        u"0 m – 0 km",
        u"3 m – 3,000 km",
        u"3,000 m – 5,000 km",
        u"4,999 m – 5,001 km",
        u"5,000 m – 5,000 km",
        u"5,000 m – 5,000,000 km");
}

void NumberRangeFormatterTest::testCollapse() {
    assertFormatRange(
        u"Default collapse on currency (default rounding)",
        NumberRangeFormatter::with()
            .numberFormatterBoth(NumberFormatter::with().unit(USD)),
        Locale("en-us"),
        u"$1.00 – $5.00",
        u"~$5.00",
        u"~$5.00",
        u"$0.00 – $3.00",
        u"~$0.00",
        u"$3.00 – $3,000.00",
        u"$3,000.00 – $5,000.00",
        u"$4,999.00 – $5,001.00",
        u"~$5,000.00",
        u"$5,000.00 – $5,000,000.00");

    assertFormatRange(
        u"Default collapse on currency",
        NumberRangeFormatter::with()
            .numberFormatterBoth(NumberFormatter::with().unit(USD).precision(Precision::integer())),
        Locale("en-us"),
        u"$1 – $5",
        u"~$5",
        u"~$5",
        u"$0 – $3",
        u"~$0",
        u"$3 – $3,000",
        u"$3,000 – $5,000",
        u"$4,999 – $5,001",
        u"~$5,000",
        u"$5,000 – $5,000,000");

    assertFormatRange(
        u"No collapse on currency",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_NONE)
            .numberFormatterBoth(NumberFormatter::with().unit(USD).precision(Precision::integer())),
        Locale("en-us"),
        u"$1 – $5",
        u"~$5",
        u"~$5",
        u"$0 – $3",
        u"~$0",
        u"$3 – $3,000",
        u"$3,000 – $5,000",
        u"$4,999 – $5,001",
        u"~$5,000",
        u"$5,000 – $5,000,000");

    assertFormatRange(
        u"Unit collapse on currency",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_UNIT)
            .numberFormatterBoth(NumberFormatter::with().unit(USD).precision(Precision::integer())),
        Locale("en-us"),
        u"$1–5",
        u"~$5",
        u"~$5",
        u"$0–3",
        u"~$0",
        u"$3–3,000",
        u"$3,000–5,000",
        u"$4,999–5,001",
        u"~$5,000",
        u"$5,000–5,000,000");

    assertFormatRange(
        u"All collapse on currency",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_ALL)
            .numberFormatterBoth(NumberFormatter::with().unit(USD).precision(Precision::integer())),
        Locale("en-us"),
        u"$1–5",
        u"~$5",
        u"~$5",
        u"$0–3",
        u"~$0",
        u"$3–3,000",
        u"$3,000–5,000",
        u"$4,999–5,001",
        u"~$5,000",
        u"$5,000–5,000,000");

    assertFormatRange(
        u"Default collapse on currency ISO code",
        NumberRangeFormatter::with()
            .numberFormatterBoth(NumberFormatter::with()
                .unit(GBP)
                .unitWidth(UNUM_UNIT_WIDTH_ISO_CODE)
                .precision(Precision::integer())),
        Locale("en-us"),
        u"GBP 1–5",
        u"~GBP 5",  // TODO: Fix this at some point
        u"~GBP 5",
        u"GBP 0–3",
        u"~GBP 0",
        u"GBP 3–3,000",
        u"GBP 3,000–5,000",
        u"GBP 4,999–5,001",
        u"~GBP 5,000",
        u"GBP 5,000–5,000,000");

    assertFormatRange(
        u"No collapse on currency ISO code",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_NONE)
            .numberFormatterBoth(NumberFormatter::with()
                .unit(GBP)
                .unitWidth(UNUM_UNIT_WIDTH_ISO_CODE)
                .precision(Precision::integer())),
        Locale("en-us"),
        u"GBP 1 – GBP 5",
        u"~GBP 5",  // TODO: Fix this at some point
        u"~GBP 5",
        u"GBP 0 – GBP 3",
        u"~GBP 0",
        u"GBP 3 – GBP 3,000",
        u"GBP 3,000 – GBP 5,000",
        u"GBP 4,999 – GBP 5,001",
        u"~GBP 5,000",
        u"GBP 5,000 – GBP 5,000,000");

    assertFormatRange(
        u"Unit collapse on currency ISO code",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_UNIT)
            .numberFormatterBoth(NumberFormatter::with()
                .unit(GBP)
                .unitWidth(UNUM_UNIT_WIDTH_ISO_CODE)
                .precision(Precision::integer())),
        Locale("en-us"),
        u"GBP 1–5",
        u"~GBP 5",  // TODO: Fix this at some point
        u"~GBP 5",
        u"GBP 0–3",
        u"~GBP 0",
        u"GBP 3–3,000",
        u"GBP 3,000–5,000",
        u"GBP 4,999–5,001",
        u"~GBP 5,000",
        u"GBP 5,000–5,000,000");

    assertFormatRange(
        u"All collapse on currency ISO code",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_ALL)
            .numberFormatterBoth(NumberFormatter::with()
                .unit(GBP)
                .unitWidth(UNUM_UNIT_WIDTH_ISO_CODE)
                .precision(Precision::integer())),
        Locale("en-us"),
        u"GBP 1–5",
        u"~GBP 5",  // TODO: Fix this at some point
        u"~GBP 5",
        u"GBP 0–3",
        u"~GBP 0",
        u"GBP 3–3,000",
        u"GBP 3,000–5,000",
        u"GBP 4,999–5,001",
        u"~GBP 5,000",
        u"GBP 5,000–5,000,000");

    // Default collapse on measurement unit is in testBasic()

    assertFormatRange(
        u"No collapse on measurement unit",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_NONE)
            .numberFormatterBoth(NumberFormatter::with().unit(METER)),
        Locale("en-us"),
        u"1 m – 5 m",
        u"~5 m",
        u"~5 m",
        u"0 m – 3 m",
        u"~0 m",
        u"3 m – 3,000 m",
        u"3,000 m – 5,000 m",
        u"4,999 m – 5,001 m",
        u"~5,000 m",
        u"5,000 m – 5,000,000 m");

    assertFormatRange(
        u"Unit collapse on measurement unit",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_UNIT)
            .numberFormatterBoth(NumberFormatter::with().unit(METER)),
        Locale("en-us"),
        u"1–5 m",
        u"~5 m",
        u"~5 m",
        u"0–3 m",
        u"~0 m",
        u"3–3,000 m",
        u"3,000–5,000 m",
        u"4,999–5,001 m",
        u"~5,000 m",
        u"5,000–5,000,000 m");

    assertFormatRange(
        u"All collapse on measurement unit",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_ALL)
            .numberFormatterBoth(NumberFormatter::with().unit(METER)),
        Locale("en-us"),
        u"1–5 m",
        u"~5 m",
        u"~5 m",
        u"0–3 m",
        u"~0 m",
        u"3–3,000 m",
        u"3,000–5,000 m",
        u"4,999–5,001 m",
        u"~5,000 m",
        u"5,000–5,000,000 m");

    assertFormatRange(
        u"Default collapse on measurement unit with compact-short notation",
        NumberRangeFormatter::with()
            .numberFormatterBoth(NumberFormatter::with().notation(Notation::compactShort()).unit(METER)),
        Locale("en-us"),
        u"1–5 m",
        u"~5 m",
        u"~5 m",
        u"0–3 m",
        u"~0 m",
        u"3–3K m",
        u"3K – 5K m",
        u"~5K m",
        u"~5K m",
        u"5K – 5M m");

    assertFormatRange(
        u"No collapse on measurement unit with compact-short notation",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_NONE)
            .numberFormatterBoth(NumberFormatter::with().notation(Notation::compactShort()).unit(METER)),
        Locale("en-us"),
        u"1 m – 5 m",
        u"~5 m",
        u"~5 m",
        u"0 m – 3 m",
        u"~0 m",
        u"3 m – 3K m",
        u"3K m – 5K m",
        u"~5K m",
        u"~5K m",
        u"5K m – 5M m");

    assertFormatRange(
        u"Unit collapse on measurement unit with compact-short notation",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_UNIT)
            .numberFormatterBoth(NumberFormatter::with().notation(Notation::compactShort()).unit(METER)),
        Locale("en-us"),
        u"1–5 m",
        u"~5 m",
        u"~5 m",
        u"0–3 m",
        u"~0 m",
        u"3–3K m",
        u"3K – 5K m",
        u"~5K m",
        u"~5K m",
        u"5K – 5M m");

    assertFormatRange(
        u"All collapse on measurement unit with compact-short notation",
        NumberRangeFormatter::with()
            .collapse(UNUM_RANGE_COLLAPSE_ALL)
            .numberFormatterBoth(NumberFormatter::with().notation(Notation::compactShort()).unit(METER)),
        Locale("en-us"),
        u"1–5 m",
        u"~5 m",
        u"~5 m",
        u"0–3 m",
        u"~0 m",
        u"3–3K m",
        u"3–5K m",  // this one is the key use case for ALL
        u"~5K m",
        u"~5K m",
        u"5K – 5M m");

    // TODO: Test compact currency?
    // The code is not smart enough to differentiate the notation from the unit.
}

void NumberRangeFormatterTest::testIdentity() {
    assertFormatRange(
        u"Identity fallback Range",
        NumberRangeFormatter::with().identityFallback(UNUM_IDENTITY_FALLBACK_RANGE),
        Locale("en-us"),
        u"1–5",
        u"5–5",
        u"5–5",
        u"0–3",
        u"0–0",
        u"3–3,000",
        u"3,000–5,000",
        u"4,999–5,001",
        u"5,000–5,000",
        u"5,000–5,000,000");

    assertFormatRange(
        u"Identity fallback Approximately or Single Value",
        NumberRangeFormatter::with().identityFallback(UNUM_IDENTITY_FALLBACK_APPROXIMATELY_OR_SINGLE_VALUE),
        Locale("en-us"),
        u"1–5",
        u"~5",
        u"5",
        u"0–3",
        u"0",
        u"3–3,000",
        u"3,000–5,000",
        u"4,999–5,001",
        u"5,000",
        u"5,000–5,000,000");

    assertFormatRange(
        u"Identity fallback  Single Value",
        NumberRangeFormatter::with().identityFallback(UNUM_IDENTITY_FALLBACK_SINGLE_VALUE),
        Locale("en-us"),
        u"1–5",
        u"5",
        u"5",
        u"0–3",
        u"0",
        u"3–3,000",
        u"3,000–5,000",
        u"4,999–5,001",
        u"5,000",
        u"5,000–5,000,000");

    assertFormatRange(
        u"Identity fallback Approximately or Single Value with compact notation",
        NumberRangeFormatter::with()
            .identityFallback(UNUM_IDENTITY_FALLBACK_APPROXIMATELY_OR_SINGLE_VALUE)
            .numberFormatterBoth(NumberFormatter::with().notation(Notation::compactShort())),
        Locale("en-us"),
        u"1–5",
        u"~5",
        u"5",
        u"0–3",
        u"0",
        u"3–3K",
        u"3K – 5K",
        u"~5K",
        u"5K",
        u"5K – 5M");
}

void NumberRangeFormatterTest::testDifferentFormatters() {
    assertFormatRange(
        u"Different rounding rules",
        NumberRangeFormatter::with()
            .numberFormatterFirst(NumberFormatter::with().precision(Precision::integer()))
            .numberFormatterSecond(NumberFormatter::with().precision(Precision::fixedDigits(2))),
        Locale("en-us"),
        u"1–5.0",
        u"5–5.0",
        u"5–5.0",
        u"0–3.0",
        u"0–0.0",
        u"3–3,000",
        u"3,000–5,000",
        u"4,999–5,000",
        u"5,000–5,000",  // TODO: Should this one be ~5,000?
        u"5,000–5,000,000");
}

void  NumberRangeFormatterTest::assertFormatRange(
      const char16_t* message,
      const UnlocalizedNumberRangeFormatter& f,
      Locale locale,
      const char16_t* expected_10_50,
      const char16_t* expected_49_51,
      const char16_t* expected_50_50,
      const char16_t* expected_00_30,
      const char16_t* expected_00_00,
      const char16_t* expected_30_3K,
      const char16_t* expected_30K_50K,
      const char16_t* expected_49K_51K,
      const char16_t* expected_50K_50K,
      const char16_t* expected_50K_50M) {
    LocalizedNumberRangeFormatter l = f.locale(locale);
    assertFormattedRangeEquals(message, l, 1, 5, expected_10_50);
    assertFormattedRangeEquals(message, l, 4.9999999, 5.0000001, expected_49_51);
    assertFormattedRangeEquals(message, l, 5, 5, expected_50_50);
    assertFormattedRangeEquals(message, l, 0, 3, expected_00_30);
    assertFormattedRangeEquals(message, l, 0, 0, expected_00_00);
    assertFormattedRangeEquals(message, l, 3, 3000, expected_30_3K);
    assertFormattedRangeEquals(message, l, 3000, 5000, expected_30K_50K);
    assertFormattedRangeEquals(message, l, 4999, 5001, expected_49K_51K);
    assertFormattedRangeEquals(message, l, 5000, 5000, expected_50K_50K);
    assertFormattedRangeEquals(message, l, 5e3, 5e6, expected_50K_50M);
}

void NumberRangeFormatterTest::assertFormattedRangeEquals(
      const char16_t* message,
      const LocalizedNumberRangeFormatter& l,
      double first,
      double second,
      const char16_t* expected) {
    IcuTestErrorCode status(*this, "assertFormattedRangeEquals");
    UnicodeString fullMessage = UnicodeString(message) + u": " + DoubleToUnicodeString(first) + u", " + DoubleToUnicodeString(second);
    status.setScope(fullMessage);
    UnicodeString actual = l.formatFormattableRange(first, second, status).toString(status);
    assertEquals(fullMessage, expected, actual);
}


#endif
