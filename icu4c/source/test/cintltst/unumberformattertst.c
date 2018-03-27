// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "unicode/unumberformatter.h"
#include "cintltst.h"

static void TestSkeletonFormatToString();

void addUNumberFormatterTest(TestNode** root);

void addUNumberFormatterTest(TestNode** root) {
    addTest(root, &TestSkeletonFormatToString, "unumberformatter/TestSkeletonFormatToString");
}


static void TestSkeletonFormatToString() {
    UErrorCode ec = U_ZERO_ERROR;
    static const int32_t CAPACITY = 30;
    UChar buffer[CAPACITY];

    // SETUP:
    UNumberFormatter* f = unumf_openFromSkeletonAndLocale(
            u"round-integer currency/USD sign-accounting", -1, "en", &ec);
    assertSuccess("Should create without error", &ec);
    UFormattedNumber* result = unumf_openResult(&ec);
    assertSuccess("Should create result without error", &ec);

    // INT TEST:
    unumf_formatInt(f, -444444, result, &ec);
    assertSuccess("Should format integer without error", &ec);
    unumf_resultToString(result, buffer, CAPACITY, &ec);
    assertSuccess("Should print string to buffer without error", &ec);
    assertUEquals("Should produce expected string result", u"($444,444)", buffer);

    // DOUBLE TEST:
    unumf_formatDouble(f, -5142.3, result, &ec);
    assertSuccess("Should format double without error", &ec);
    unumf_resultToString(result, buffer, CAPACITY, &ec);
    assertSuccess("Should print string to buffer without error", &ec);
    assertUEquals("Should produce expected string result", u"($5,142)", buffer);

    // DECIMAL TEST:
    unumf_formatDecimal(f, "9.876E2", -1, result, &ec);
    assertSuccess("Should format decimal without error", &ec);
    unumf_resultToString(result, buffer, CAPACITY, &ec);
    assertSuccess("Should print string to buffer without error", &ec);
    assertUEquals("Should produce expected string result", u"$988", buffer);

    // CLEANUP:
    unumf_closeResult(result, &ec);
    assertSuccess("Should close without error", &ec);
    unumf_close(f, &ec);
    assertSuccess("Should close without error", &ec);
}


#endif /* #if !UCONFIG_NO_FORMATTING */
