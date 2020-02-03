// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "../../i18n/unitconverter.h"
#include "decNumber.h"
#include "intltest.h"
#include "number_decnum.h"
#include "unicode/measunit.h"
#include "unicode/unistr.h"

class UnitsTest : public IntlTest {
  public:
    UnitsTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = NULL);

    void testBasic();
    void testSiPrefixes();
    void testMass();
    void testTemperature();
    void testArea();

    // TODO(younies): fix this.
    void assertConversion(StringPiece message, StringPiece source, StringPiece target, double inputValue,
                          double expectedValue);
};

extern IntlTest *createUnitsTest() { return new UnitsTest(); }

void UnitsTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) {
        logln("TestSuite UnitsTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testBasic);
    TESTCASE_AUTO(testSiPrefixes);
    TESTCASE_AUTO(testMass);
    TESTCASE_AUTO(testTemperature);
    TESTCASE_AUTO(testArea);
    TESTCASE_AUTO_END;
}

// Assert if two dec numbers are equal.
void UnitsTest::assertConversion(StringPiece message, StringPiece source, StringPiece target,
                                 double inputValue, double expectedValue) {
    UErrorCode status;

    MeasureUnit sourceUnit = MeasureUnit::forIdentifier(source, status);
    MeasureUnit targetUnit = MeasureUnit::forIdentifier(target, status);

    // assertSuccess("test Convert: Units Creation", status);

    UnitConverter converter(sourceUnit, targetUnit, status);
    // assertSuccess("test Convert: Unit Converter Creation", status);

    decNumber actualConversionResult = converter.convert(inputValue, status);
    // assertSuccess("test Convert: Converter Operation has been done!", status);

    number::impl::DecNum decNum;
    decNum.setTo(expectedValue, status);
    decNumber expectedValueDecNumber = *(decNum.getRawDecNumber());

    assertEquals(message.data(), actualConversionResult.digits, expectedValueDecNumber.digits);
    assertEquals(message.data(), actualConversionResult.bits, expectedValueDecNumber.bits);
    assertEquals(message.data(), actualConversionResult.exponent, expectedValueDecNumber.exponent);
    // TODO(younies): fix
    // assertEquals(message.data(), actualConversionResult.lsu, expectedValueDecNumber.lsu);
}

void UnitsTest::testBasic() {
    IcuTestErrorCode status(*this, "Units testBasic");

    // Test Cases
    struct TestCase {
        const StringPiece source;
        const StringPiece target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {"meter", "foot", 1.0, 3.28084},    //
        {"kilometer", "foot", 1.0, 328.084} //
    };

    for (const auto &testCase : testCases) {
        assertConversion("test Conversion", testCase.source, testCase.target, testCase.inputValue,
                         testCase.expectedValue);
    }
}

void UnitsTest::testSiPrefixes() {
    IcuTestErrorCode status(*this, "Units testSiPrefixes");
    // Test Cases
    struct TestCase {
        const StringPiece source;
        const StringPiece target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {"gram", "kilogram", 1.0, 0.001},            //
        {"milligram", "kilogram", 1.0, 0.000001},    //
        {"microgram", "kilogram", 1.0, 0.000000001}, //
        {"megawatt", "watt", 1, 1000000},            //
        {"megawatt", "kilowatt", 1.0, 1000},         //
        {"gigabyte", "byte", 1, 1000000000}          //
    };

    for (const auto &testCase : testCases) {
        assertConversion("test Conversion", testCase.source, testCase.target, testCase.inputValue,
                         testCase.expectedValue);
    }
}

void UnitsTest::testMass() {
    IcuTestErrorCode status(*this, "Units testMass");

    // Test Cases
    struct TestCase {
        const StringPiece source;
        const StringPiece target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {"gram", "kilogram", 1.0, 0.001},      //
        {"pound", "kilogram", 1.0, 0.453592},  //
        {"pound", "kilogram", 2.0, 0.907185},  //
        {"ounce", "pound", 16.0, 1.0},         //
        {"ounce", "kilogram", 16.0, 0.453592}, //
        {"ton", "pound", 1.0, 2000},           //
        {"stone", "pound", 1.0, 14},           //
        {"stone", "kilogram", 1.0, 6.35029}    //
    };

    for (const auto &testCase : testCases) {
        assertConversion("test Conversion", testCase.source, testCase.target, testCase.inputValue,
                         testCase.expectedValue);
    }
}

void UnitsTest::testTemperature() {
    IcuTestErrorCode status(*this, "Units testTemperature");
    // Test Cases
    struct TestCase {
        const StringPiece source;
        const StringPiece target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {"celsius", "fahrenheit", 0.0, 32.0},   //
        {"celsius", "fahrenheit", 10.0, 50.0},  //
        {"fahrenheit", "celsius", 32.0, 0.0},   //
        {"fahrenheit", "celsius", 89.6, 32},    //
        {"kelvin", "fahrenheit", 0.0, -459.67}, //
        {"kelvin", "fahrenheit", 300, 80.33},   //
        {"kelvin", "celsius", 0.0, -273.15},    //
        {"kelvin", "celsius", 300.0, 26.85}     //
    };

    for (const auto &testCase : testCases) {
        assertConversion("test Conversion", testCase.source, testCase.target, testCase.inputValue,
                         testCase.expectedValue);
    }
}

void UnitsTest::testArea() {
    IcuTestErrorCode status(*this, "Units Area");

    // Test Cases
    struct TestCase {
        const StringPiece source;
        const StringPiece target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {"square-meter", "square-yard", 10.0, 11.9599}, //
        {"hectare", "square-yard", 1.0, 11959.9},       //
        {"square-mile", "square-foot", 0.0001, 2787.84} //
    };

    for (const auto &testCase : testCases) {
        assertConversion("test Conversion", testCase.source, testCase.target, testCase.inputValue,
                         testCase.expectedValue);
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
