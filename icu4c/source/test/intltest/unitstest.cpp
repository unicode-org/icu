// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "../../i18n/unitconverter.h"
#include "intltest.h"
#include "number_decnum.h"

struct UnitConversionTestCase {
    const StringPiece source;
    const StringPiece target;
    const double inputValue;
    const double expectedValue;
};

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
    void verifyTestCase(const UnitConversionTestCase &testCase);
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

void UnitsTest::verifyTestCase(const UnitConversionTestCase &testCase) {
    UErrorCode status = U_ZERO_ERROR;

    MeasureUnit sourceUnit = MeasureUnit::forIdentifier(testCase.source, status);
    MeasureUnit targetUnit = MeasureUnit::forIdentifier(testCase.target, status);

    UnitConverter converter(sourceUnit, targetUnit, status);

    number::impl::DecNum inputValue;
    inputValue.setTo(testCase.inputValue, status);

    number::impl::DecNum expectedValue;
    expectedValue.setTo(testCase.expectedValue, status);

    number::impl::DecNum actualConversionResult;
    converter.convert(inputValue, actualConversionResult, status);

    assertEqualsNear("test Conversion", expectedValue, actualConversionResult, 0.01);
}

void UnitsTest::testBasic() {
    IcuTestErrorCode status(*this, "Units testBasic");

    UnitConversionTestCase testCases[]{
        {"meter", "foot", 1.0, 3.28084},    //
        {"kilometer", "foot", 1.0, 3280.84} //
    };

    for (const auto &testCase : testCases) {
        UErrorCode status = U_ZERO_ERROR;

        MeasureUnit sourceUnit = MeasureUnit::forIdentifier(testCase.source, status);
        MeasureUnit targetUnit = MeasureUnit::forIdentifier(testCase.target, status);

        UnitConverter converter(sourceUnit, targetUnit, status);

        number::impl::DecNum inputValue;
        inputValue.setTo(testCase.inputValue, status);

        number::impl::DecNum expectedValue;
        expectedValue.setTo(testCase.expectedValue, status);

        number::impl::DecNum actualConversionResult;
        converter.convert(inputValue, actualConversionResult, status);

        assertEqualsNear("test Conversion", expectedValue, actualConversionResult, 0.01);
    }
}

void UnitsTest::testSiPrefixes() {
    IcuTestErrorCode status(*this, "Units testSiPrefixes");

    UnitConversionTestCase testCases[]{
        {"gram", "kilogram", 1.0, 0.001},            //
        {"milligram", "kilogram", 1.0, 0.000001},    //
        {"microgram", "kilogram", 1.0, 0.000000001}, //
        {"megawatt", "watt", 1, 1000000},            //
        {"megawatt", "kilowatt", 1.0, 1000},         //
        {"gigabyte", "byte", 1, 1000000000}          //
    };

    for (const auto &testCase : testCases) {
        verifyTestCase(testCase);
    }
}

void UnitsTest::testMass() {
    IcuTestErrorCode status(*this, "Units testMass");

    UnitConversionTestCase testCases[]{
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
        verifyTestCase(testCase);
    }
}

void UnitsTest::testTemperature() {
    IcuTestErrorCode status(*this, "Units testTemperature");

    UnitConversionTestCase testCases[]{
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
        verifyTestCase(testCase);
    }
}

void UnitsTest::testArea() {
    IcuTestErrorCode status(*this, "Units Area");

    UnitConversionTestCase testCases[]{
        {"square-meter", "square-yard", 10.0, 11.9599} //
        ,
        {"hectare", "square-yard", 1.0, 11959.9} //
        ,
        {"hectare", "square-meter", 1.0, 10000} //
        ,
        {"hectare", "square-meter", 0.0, 0.0} //
        ,
        {"square-mile", "square-foot", 0.0001, 2787.84} //
        ,
        {"square-yard", "square-foot", 10, 90} //
        ,
        {"square-yard", "square-foot", 0, 0} //
        ,
        {"square-yard", "square-foot", 0.000001, 0.000009} //
        ,
        {"square-mile", "square-foot", 0.0, 0.0} //
    };

    for (const auto &testCase : testCases) {
        verifyTestCase(testCase);
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
