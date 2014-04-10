/*
*******************************************************************************
* Copyright (C) 2014, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File SCIFORMATHELPERTEST.CPP
*
*******************************************************************************
*/
#include <stdio.h>
#include <stdlib.h>

#include "intltest.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/scientificformathelper.h"
#include "unicode/numfmt.h"
#include "unicode/decimfmt.h"

#define LENGTHOF(array) (int32_t)(sizeof(array) / sizeof((array)[0]))

class ScientificFormatHelperTest : public IntlTest {
public:
    ScientificFormatHelperTest() {
    }

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=0);
private:
    void TestBasic();
    void TestPlusSignInExponent();
};

void ScientificFormatHelperTest::runIndexedTest(
        int32_t index, UBool exec, const char *&name, char *) {
    if (exec) {
        logln("TestSuite ScientificFormatHelperTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestBasic);
    TESTCASE_AUTO(TestPlusSignInExponent);
    TESTCASE_AUTO_END;
}

void ScientificFormatHelperTest::TestBasic() {
    UErrorCode status = U_ZERO_ERROR;
    LocalPointer<DecimalFormat> decfmt((DecimalFormat *) NumberFormat::createScientificInstance("en", status));
    UnicodeString appendTo("String: ");
    FieldPositionIterator fpositer;
    decfmt->format(1.23456e-78, appendTo, &fpositer, status);
    FieldPositionIterator fpositer2(fpositer);
    FieldPositionIterator fpositer3(fpositer);
    ScientificFormatHelper helper(*decfmt->getDecimalFormatSymbols(), status);
    UnicodeString result;
    const char *expected = "String: 1.23456\\u00d710<sup>-78</sup>";
    assertEquals(
            "insetMarkup",
            UnicodeString(expected).unescape(),
            helper.insetMarkup(appendTo, fpositer, "<sup>", "</sup>", result, status));
    result.remove();
    expected = "String: 1.23456\\u00d710\\u207b\\u2077\\u2078";
    assertEquals(
            "toSuperscriptExponentDigits",
            UnicodeString(expected).unescape(),
            helper.toSuperscriptExponentDigits(appendTo, fpositer2, result, status));
    assertSuccess("", status);
    result.remove();

    // The 'a' is an invalid exponent character.
    helper.toSuperscriptExponentDigits("String: 1.23456e-7a", fpositer3, result, status);
    if (status != U_INVALID_CHAR_FOUND) {
        errln("Expected U_INVALID_CHAR_FOUND");
    }
}

void ScientificFormatHelperTest::TestPlusSignInExponent() {
    UErrorCode status = U_ZERO_ERROR;
    LocalPointer<DecimalFormat> decfmt((DecimalFormat *) NumberFormat::createScientificInstance("en", status));
    decfmt->applyPattern("0.00E+0", status);
    assertSuccess("1", status);
    UnicodeString appendTo;
    FieldPositionIterator fpositer;
    decfmt->format(6.02e23, appendTo, &fpositer, status);
    ScientificFormatHelper helper(*decfmt->getDecimalFormatSymbols(), status);
    UnicodeString result;
    const char *expected = "6.02\\u00d710\\u207a\\u00b2\\u00b3";
    assertEquals(
            "",
            UnicodeString(expected).unescape(),
            helper.toSuperscriptExponentDigits(appendTo, fpositer, result, status));
    assertSuccess("", status);
}

extern IntlTest *createScientificFormatHelperTest() {
    return new ScientificFormatHelperTest();
}

#endif /* !UCONFIG_NO_FORMATTING */
