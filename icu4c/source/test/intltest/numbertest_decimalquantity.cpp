// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "number_decimalquantity.h"
#include "math.h"
#include <cmath>
#include "numbertest.h"

void DecimalQuantityTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char *) {
    if (exec) {
        logln("TestSuite DecimalQuantityTest: ");
    }
    TESTCASE_AUTO_BEGIN;
        TESTCASE_AUTO(testDecimalQuantityBehaviorStandalone);
        TESTCASE_AUTO(testSwitchStorage);
        TESTCASE_AUTO(testAppend);
        TESTCASE_AUTO(testConvertToAccurateDouble);
        TESTCASE_AUTO(testUseApproximateDoubleWhenAble);
        TESTCASE_AUTO(testHardDoubleConversion);
    TESTCASE_AUTO_END;
}

void DecimalQuantityTest::assertDoubleEquals(UnicodeString message, double a, double b) {
    if (a == b) {
        return;
    }

    double diff = a - b;
    diff = diff < 0 ? -diff : diff;
    double bound = a < 0 ? -a * 1e-6 : a * 1e-6;
    if (diff > bound) {
        errln(message + u": " + DoubleToUnicodeString(a) + u" vs " + DoubleToUnicodeString(b) + u" differ by " + DoubleToUnicodeString(diff));
    }
}

void DecimalQuantityTest::assertHealth(const DecimalQuantity &fq) {
    const char16_t* health = fq.checkHealth();
    if (health != nullptr) {
        errln(UnicodeString(u"HEALTH FAILURE: ") + UnicodeString(health) + u": " + fq.toString());
    }
}

void
DecimalQuantityTest::assertToStringAndHealth(const DecimalQuantity &fq, const UnicodeString &expected) {
    UnicodeString actual = fq.toString();
    assertEquals("DecimalQuantity toString failed", expected, actual);
    assertHealth(fq);
}

void DecimalQuantityTest::checkDoubleBehavior(double d, bool explicitRequired) {
    DecimalQuantity fq;
    fq.setToDouble(d);
    if (explicitRequired) {
        assertTrue("Should be using approximate double", !fq.isExplicitExactDouble());
    }
    UnicodeString baseStr = fq.toString();
    assertDoubleEquals(
        UnicodeString(u"Initial construction from hard double: ") + baseStr,
        d, fq.toDouble());
    fq.roundToInfinity();
    UnicodeString newStr = fq.toString();
    if (explicitRequired) {
        assertTrue("Should not be using approximate double", fq.isExplicitExactDouble());
    }
    assertDoubleEquals(
        UnicodeString(u"After conversion to exact BCD (double): ") + baseStr + u" vs " + newStr,
        d, fq.toDouble());
}

void DecimalQuantityTest::testDecimalQuantityBehaviorStandalone() {
    UErrorCode status = U_ZERO_ERROR;
    DecimalQuantity fq;
    assertToStringAndHealth(fq, u"<DecimalQuantity 999:0:0:-999 long 0E0>");
    fq.setToInt(51423);
    assertToStringAndHealth(fq, u"<DecimalQuantity 999:0:0:-999 long 51423E0>");
    fq.adjustMagnitude(-3);
    assertToStringAndHealth(fq, u"<DecimalQuantity 999:0:0:-999 long 51423E-3>");
    fq.setToLong(999999999999000L);
    assertToStringAndHealth(fq, u"<DecimalQuantity 999:0:0:-999 long 999999999999E3>");
    fq.setIntegerLength(2, 5);
    assertToStringAndHealth(fq, u"<DecimalQuantity 5:2:0:-999 long 999999999999E3>");
    fq.setFractionLength(3, 6);
    assertToStringAndHealth(fq, u"<DecimalQuantity 5:2:-3:-6 long 999999999999E3>");
    fq.setToDouble(987.654321);
    assertToStringAndHealth(fq, u"<DecimalQuantity 5:2:-3:-6 long 987654321E-6>");
    fq.roundToInfinity();
    assertToStringAndHealth(fq, u"<DecimalQuantity 5:2:-3:-6 long 987654321E-6>");
    fq.roundToIncrement(0.005, RoundingMode::UNUM_ROUND_HALFEVEN, 3, status);
    assertSuccess("Rounding to increment", status);
    assertToStringAndHealth(fq, u"<DecimalQuantity 5:2:-3:-6 long 987655E-3>");
    fq.roundToMagnitude(-2, RoundingMode::UNUM_ROUND_HALFEVEN, status);
    assertSuccess("Rounding to magnitude", status);
    assertToStringAndHealth(fq, u"<DecimalQuantity 5:2:-3:-6 long 98766E-2>");
}

void DecimalQuantityTest::testSwitchStorage() {
    UErrorCode status = U_ZERO_ERROR;
    DecimalQuantity fq;

    fq.setToLong(1234123412341234L);
    assertFalse("Should not be using byte array", fq.isUsingBytes());
    assertEquals("Failed on initialize", u"1234123412341234E0", fq.toNumberString());
    assertHealth(fq);
    // Long -> Bytes
    fq.appendDigit(5, 0, true);
    assertTrue("Should be using byte array", fq.isUsingBytes());
    assertEquals("Failed on multiply", u"12341234123412345E0", fq.toNumberString());
    assertHealth(fq);
    // Bytes -> Long
    fq.roundToMagnitude(5, RoundingMode::UNUM_ROUND_HALFEVEN, status);
    assertSuccess("Rounding to magnitude", status);
    assertFalse("Should not be using byte array", fq.isUsingBytes());
    assertEquals("Failed on round", u"123412341234E5", fq.toNumberString());
    assertHealth(fq);
}

void DecimalQuantityTest::testAppend() {
    DecimalQuantity fq;
    fq.appendDigit(1, 0, true);
    assertEquals("Failed on append", u"1E0", fq.toNumberString());
    assertHealth(fq);
    fq.appendDigit(2, 0, true);
    assertEquals("Failed on append", u"12E0", fq.toNumberString());
    assertHealth(fq);
    fq.appendDigit(3, 1, true);
    assertEquals("Failed on append", u"1203E0", fq.toNumberString());
    assertHealth(fq);
    fq.appendDigit(0, 1, true);
    assertEquals("Failed on append", u"1203E2", fq.toNumberString());
    assertHealth(fq);
    fq.appendDigit(4, 0, true);
    assertEquals("Failed on append", u"1203004E0", fq.toNumberString());
    assertHealth(fq);
    fq.appendDigit(0, 0, true);
    assertEquals("Failed on append", u"1203004E1", fq.toNumberString());
    assertHealth(fq);
    fq.appendDigit(5, 0, false);
    assertEquals("Failed on append", u"120300405E-1", fq.toNumberString());
    assertHealth(fq);
    fq.appendDigit(6, 0, false);
    assertEquals("Failed on append", u"1203004056E-2", fq.toNumberString());
    assertHealth(fq);
    fq.appendDigit(7, 3, false);
    assertEquals("Failed on append", u"12030040560007E-6", fq.toNumberString());
    assertHealth(fq);
    UnicodeString baseExpected(u"12030040560007");
    for (int i = 0; i < 10; i++) {
        fq.appendDigit(8, 0, false);
        baseExpected.append(u'8');
        UnicodeString expected(baseExpected);
        expected.append(u"E-");
        if (i >= 3) {
            expected.append(u'1');
        }
        expected.append(((7 + i) % 10) + u'0');
        assertEquals("Failed on append", expected, fq.toNumberString());
        assertHealth(fq);
    }
    fq.appendDigit(9, 2, false);
    baseExpected.append(u"009");
    UnicodeString expected(baseExpected);
    expected.append(u"E-19");
    assertEquals("Failed on append", expected, fq.toNumberString());
    assertHealth(fq);
}

void DecimalQuantityTest::testConvertToAccurateDouble() {
    // based on https://github.com/google/double-conversion/issues/28
    static double hardDoubles[] = {
            1651087494906221570.0,
            -5074790912492772E-327,
            83602530019752571E-327,
            2.207817077636718750000000000000,
            1.818351745605468750000000000000,
            3.941719055175781250000000000000,
            3.738609313964843750000000000000,
            3.967735290527343750000000000000,
            1.328025817871093750000000000000,
            3.920967102050781250000000000000,
            1.015235900878906250000000000000,
            1.335227966308593750000000000000,
            1.344520568847656250000000000000,
            2.879127502441406250000000000000,
            3.695838928222656250000000000000,
            1.845344543457031250000000000000,
            3.793952941894531250000000000000,
            3.211402893066406250000000000000,
            2.565971374511718750000000000000,
            0.965156555175781250000000000000,
            2.700004577636718750000000000000,
            0.767097473144531250000000000000,
            1.780448913574218750000000000000,
            2.624839782714843750000000000000,
            1.305290222167968750000000000000,
            3.834922790527343750000000000000,};

    static double integerDoubles[] = {
            51423,
            51423e10,
            4.503599627370496E15,
            6.789512076111555E15,
            9.007199254740991E15,
            9.007199254740992E15};

    for (double d : hardDoubles) {
        checkDoubleBehavior(d, true);
    }

    for (double d : integerDoubles) {
        checkDoubleBehavior(d, false);
    }

    assertDoubleEquals(u"NaN check failed", NAN, DecimalQuantity().setToDouble(NAN).toDouble());
    assertDoubleEquals(
            u"Inf check failed", INFINITY, DecimalQuantity().setToDouble(INFINITY).toDouble());
    assertDoubleEquals(
            u"-Inf check failed", -INFINITY, DecimalQuantity().setToDouble(-INFINITY).toDouble());

    // Generate random doubles
    for (int32_t i = 0; i < 10000; i++) {
        uint8_t bytes[8];
        for (int32_t j = 0; j < 8; j++) {
            bytes[j] = static_cast<uint8_t>(rand() % 256);
        }
        double d;
        uprv_memcpy(&d, bytes, 8);
        if (std::isnan(d) || !std::isfinite(d)) { continue; }
        checkDoubleBehavior(d, false);
    }
}

void DecimalQuantityTest::testUseApproximateDoubleWhenAble() {
    static const struct TestCase {
        double d;
        int32_t maxFrac;
        RoundingMode roundingMode;
        bool usesExact;
    } cases[] = {{1.2345678, 1, RoundingMode::UNUM_ROUND_HALFEVEN, false},
                 {1.2345678, 7, RoundingMode::UNUM_ROUND_HALFEVEN, false},
                 {1.2345678, 12, RoundingMode::UNUM_ROUND_HALFEVEN, false},
                 {1.2345678, 13, RoundingMode::UNUM_ROUND_HALFEVEN, true},
                 {1.235, 1, RoundingMode::UNUM_ROUND_HALFEVEN, false},
                 {1.235, 2, RoundingMode::UNUM_ROUND_HALFEVEN, true},
                 {1.235, 3, RoundingMode::UNUM_ROUND_HALFEVEN, false},
                 {1.000000000000001, 0, RoundingMode::UNUM_ROUND_HALFEVEN, false},
                 {1.000000000000001, 0, RoundingMode::UNUM_ROUND_CEILING, true},
                 {1.235, 1, RoundingMode::UNUM_ROUND_CEILING, false},
                 {1.235, 2, RoundingMode::UNUM_ROUND_CEILING, false},
                 {1.235, 3, RoundingMode::UNUM_ROUND_CEILING, true}};

    UErrorCode status = U_ZERO_ERROR;
    for (TestCase cas : cases) {
        DecimalQuantity fq;
        fq.setToDouble(cas.d);
        assertTrue("Should be using approximate double", !fq.isExplicitExactDouble());
        fq.roundToMagnitude(-cas.maxFrac, cas.roundingMode, status);
        assertSuccess("Rounding to magnitude", status);
        if (cas.usesExact != fq.isExplicitExactDouble()) {
            errln(UnicodeString(u"Using approximate double after rounding: ") + fq.toString());
        }
    }
}

void DecimalQuantityTest::testHardDoubleConversion() {
    static const struct TestCase {
        double input;
        const char16_t* expectedOutput;
    } cases[] = {
            { 512.0000000000017, u"512.0000000000017" },
            { 4095.9999999999977, u"4095.9999999999977" },
            { 4095.999999999998, u"4095.999999999998" },
            { 4095.9999999999986, u"4095.9999999999986" },
            { 4095.999999999999, u"4095.999999999999" },
            { 4095.9999999999995, u"4095.9999999999995" },
            { 4096.000000000001, u"4096.000000000001" },
            { 4096.000000000002, u"4096.000000000002" },
            { 4096.000000000003, u"4096.000000000003" },
            { 4096.000000000004, u"4096.000000000004" },
            { 4096.000000000005, u"4096.000000000005" },
            { 4096.0000000000055, u"4096.0000000000055" },
            { 4096.000000000006, u"4096.000000000006" },
            { 4096.000000000007, u"4096.000000000007" } };

    for (auto& cas : cases) {
        DecimalQuantity q;
        q.setToDouble(cas.input);
        q.roundToInfinity();
        UnicodeString actualOutput = q.toPlainString();
        assertEquals("", cas.expectedOutput, actualOutput);
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
