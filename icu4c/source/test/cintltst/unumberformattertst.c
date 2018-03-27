// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "unicode/unumberformatter.h"
#include "unicode/umisc.h"
#include "unicode/unum.h"
#include "cintltst.h"

static void TestSkeletonFormatToString();

static void TestSkeletonFormatToFields();

void addUNumberFormatterTest(TestNode** root);

void addUNumberFormatterTest(TestNode** root) {
    addTest(root, &TestSkeletonFormatToString, "unumberformatter/TestSkeletonFormatToString");
    addTest(root, &TestSkeletonFormatToFields, "unumberformatter/TestSkeletonFormatToFields");
}


static void TestSkeletonFormatToString() {
    UErrorCode ec = U_ZERO_ERROR;
    static const int32_t CAPACITY = 30;
    UChar buffer[CAPACITY];

    // setup:
    UNumberFormatter* f = unumf_openFromSkeletonAndLocale(
            u"round-integer currency/USD sign-accounting", -1, "en", &ec);
    assertSuccess("Should create without error", &ec);
    UFormattedNumber* result = unumf_openResult(&ec);
    assertSuccess("Should create result without error", &ec);

    // int64 test:
    unumf_formatInt(f, -444444, result, &ec);
    assertSuccess("Should format integer without error", &ec);
    unumf_resultToString(result, buffer, CAPACITY, &ec);
    assertSuccess("Should print string to buffer without error", &ec);
    assertUEquals("Should produce expected string result", u"($444,444)", buffer);

    // double test:
    unumf_formatDouble(f, -5142.3, result, &ec);
    assertSuccess("Should format double without error", &ec);
    unumf_resultToString(result, buffer, CAPACITY, &ec);
    assertSuccess("Should print string to buffer without error", &ec);
    assertUEquals("Should produce expected string result", u"($5,142)", buffer);

    // decnumber test:
    unumf_formatDecimal(f, "9.876E2", -1, result, &ec);
    assertSuccess("Should format decimal without error", &ec);
    unumf_resultToString(result, buffer, CAPACITY, &ec);
    assertSuccess("Should print string to buffer without error", &ec);
    assertUEquals("Should produce expected string result", u"$988", buffer);

    // cleanup:
    unumf_closeResult(result);
    unumf_close(f);
}


static void TestSkeletonFormatToFields() {
    UErrorCode ec = U_ZERO_ERROR;

    // setup:
    UNumberFormatter* uformatter = unumf_openFromSkeletonAndLocale(
            u".00 measure-unit/length-meter sign-always", -1, "en", &ec);
    assertSuccess("Should create without error", &ec);
    UFormattedNumber* uresult = unumf_openResult(&ec);
    assertSuccess("Should create result without error", &ec);
    unumf_formatInt(uformatter, 9876543210L, uresult, &ec); // "+9,876,543,210.00 m"

    // field position test:
    UFieldPosition ufpos = {UNUM_DECIMAL_SEPARATOR_FIELD};
    unumf_resultGetField(uresult, &ufpos, &ec);
    assertIntEquals("Field position should be correct", 14, ufpos.beginIndex);
    assertIntEquals("Field position should be correct", 15, ufpos.endIndex);

    // field position iterator test:
    UFieldPositionIterator* ufpositer = ufieldpositer_open(&ec);
    assertSuccess("Should create iterator without error", &ec);
    unumf_resultGetAllFields(uresult, ufpositer, &ec);
    static const UFieldPosition expectedFields[] = {
            // Field, begin index, end index
            {UNUM_SIGN_FIELD, 0, 1},
            {UNUM_GROUPING_SEPARATOR_FIELD, 2, 3},
            {UNUM_GROUPING_SEPARATOR_FIELD, 6, 7},
            {UNUM_GROUPING_SEPARATOR_FIELD, 10, 11},
            {UNUM_INTEGER_FIELD, 1, 14},
            {UNUM_DECIMAL_SEPARATOR_FIELD, 14, 15},
            {UNUM_FRACTION_FIELD, 15, 17}};
    UFieldPosition actual;
    for (int32_t i = 0; i < sizeof(expectedFields) / sizeof(*expectedFields); i++) {
        // Iterate using the UFieldPosition to hold state...
        UFieldPosition expected = expectedFields[i];
        actual.field = ufieldpositer_next(ufpositer, &actual.beginIndex, &actual.endIndex);
        assertTrue("Should not return a negative index yet", actual.field >= 0);
        if (expected.field != actual.field) {
            log_err(
                    "FAIL: iteration %d; expected field %d; got %d\n", i, expected.field, actual.field);
        }
        if (expected.beginIndex != actual.beginIndex) {
            log_err(
                    "FAIL: iteration %d; expected beginIndex %d; got %d\n",
                    i,
                    expected.beginIndex,
                    actual.beginIndex);
        }
        if (expected.endIndex != actual.endIndex) {
            log_err(
                    "FAIL: iteration %d; expected endIndex %d; got %d\n",
                    i,
                    expected.endIndex,
                    actual.endIndex);
        }
    }
    actual.field = ufieldpositer_next(ufpositer, &actual.beginIndex, &actual.endIndex);
    assertTrue("No more fields; should return a negative index", actual.field < 0);

    // cleanup:
    unumf_closeResult(uresult);
    unumf_close(uformatter);
    ufieldpositer_close(ufpositer);
}


#endif /* #if !UCONFIG_NO_FORMATTING */
