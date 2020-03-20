// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include <iostream>

#include "charstr.h"
#include "filestrm.h"
#include "intltest.h"
#include "number_decimalquantity.h"
#include "unicode/ctest.h"
#include "unicode/measunit.h"
#include "unicode/unistr.h"
#include "unicode/unum.h"
#include "unitconverter.h"
#include "unitsrouter.h"
#include "uparse.h"

struct UnitConversionTestCase {
    const StringPiece source;
    const StringPiece target;
    const double inputValue;
    const double expectedValue;
};

using icu::number::impl::DecimalQuantity;

class UnitsTest : public IntlTest {
  public:
    UnitsTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = NULL);

    void testConversions();
    void testPreferences();
    void testGetUnitsData();

    void testBasic();
    void testSiPrefixes();
    void testMass();
    void testTemperature();
    void testArea();
    void testComplicatedUnits();
    void testCLDRUnitsTests();
    void testCLDRUnitsTests2();
    void testStatus();

    // TODO(younies): remove after using CLDR test cases.
    void verifyTestCase(const UnitConversionTestCase &testCase);
};

extern IntlTest *createUnitsTest() { return new UnitsTest(); }

void UnitsTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) { logln("TestSuite UnitsTest: "); }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testConversions);
    TESTCASE_AUTO(testPreferences);
    TESTCASE_AUTO(testGetUnitsData);

    TESTCASE_AUTO(testBasic);
    TESTCASE_AUTO(testSiPrefixes);
    TESTCASE_AUTO(testMass);
    TESTCASE_AUTO(testTemperature);
    TESTCASE_AUTO(testArea);
    TESTCASE_AUTO(testComplicatedUnits);
    TESTCASE_AUTO(testCLDRUnitsTests);
    TESTCASE_AUTO(testCLDRUnitsTests2);
    TESTCASE_AUTO(testStatus);
    TESTCASE_AUTO_END;
}

void UnitsTest::verifyTestCase(const UnitConversionTestCase &testCase) {
    UErrorCode status = U_ZERO_ERROR;
    MeasureUnit sourceUnit = MeasureUnit::forIdentifier(testCase.source, status);
    MeasureUnit targetUnit = MeasureUnit::forIdentifier(testCase.target, status);

    UnitConverter converter(sourceUnit, targetUnit, status);

    double actual = converter.convert(testCase.inputValue);

    assertEqualsNear("test Conversion", testCase.expectedValue, actual, 0.0001);
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

        double actual = converter.convert(testCase.inputValue);

        assertEqualsNear("test Conversion", testCase.expectedValue, actual, 0.0001);
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

void UnitsTest::testComplicatedUnits() {
    IcuTestErrorCode status(*this, "Units Area");

    UnitConversionTestCase testCases[]{
        {"meter-per-second", "meter-per-square-millisecond", 1000000.0, 1.0} //
    };

    for (const auto &testCase : testCases) {
        verifyTestCase(testCase);
    }
}

// TODO(younies): remove after using CLDR test cases.
double strToDouble(StringPiece strNum) {
    std::string charNum;
    for (int i = 0; i < strNum.length(); i++) {
        charNum += strNum.data()[i];
    }

    char *end;
    return std::strtod(charNum.c_str(), &end);
}

void UnitsTest::testCLDRUnitsTests() {
    struct {
        const StringPiece category;
        const StringPiece source;
        const StringPiece target;
        const StringPiece inputValue;
        const StringPiece expectedValue;
    } testCases[]{
        {"acceleration", "meter-per-square-second", "meter-per-square-second", "1000", "1000.0"},
        {"acceleration", "g-force", "meter-per-square-second", "1000", "9806.65"},
        {"angle", "arc-second", "revolution", "1000", "0.0007716049"},
        {"angle", "arc-minute", "revolution", "1000", "0.0462963"},
        {"angle", "degree", "revolution", "1000", "2.777778"},
        {"angle", "radian", "revolution", "1000", "159.1549"},
        {"angle", "revolution", "revolution", "1000", "1000.0"},
        {"area", "square-centimeter", "square-meter", "1000", "0.1"},
        {"area", "square-inch", "square-meter", "1000", "0.64516"},
        {"area", "square-foot", "square-meter", "1000", "92.90304"},
        {"area", "square-yard", "square-meter", "1000", "836.1274"},
        {"area", "square-meter", "square-meter", "1000", "1000.0"},
        {"area", "dunam", "square-meter", "1000", "1000000.0"},
        {"area", "acre", "square-meter", "1000", "4046856.0"},
        {"area", "hectare", "square-meter", "1000", "10000000.0"},
        {"area", "square-kilometer", "square-meter", "1000", "1000000000.0"},
        {"area", "square-mile", "square-meter", "1000", "2589988000.0"},
        //   {"concentration", "millimole-per-liter", "item-per-cubic-meter", "1000", "6.022141e+26"},
        //  {"consumption", "liter-per-100-kilometer", "cubic-meter-per-meter", "1000", "1e-05"},
        //  {"consumption", "liter-per-kilometer", "cubic-meter-per-meter", "1000", "0.001"},
        //  {"consumption-inverse", "mile-per-gallon-imperial", "meter-per-cubic-meter", "1000",
        //  "354006200.0"},
        //   {"consumption-inverse", "mile-per-gallon", "meter-per-cubic-meter", "1000", "425143700.0"},
        {"digital", "bit", "bit", "1000", "1000.0"},
        {"digital", "byte", "bit", "1000", "8000.0"},
        {"digital", "kilobit", "bit", "1000", "1000000.0"},
        {"digital", "kilobyte", "bit", "1000", "8000000.0"},
        {"digital", "megabit", "bit", "1000", "1000000000.0"},
        {"digital", "megabyte", "bit", "1000", "8000000000.0"},
        {"digital", "gigabit", "bit", "1000", "1e+12"},
        {"digital", "gigabyte", "bit", "1000", "8e+12"},
        {"digital", "terabit", "bit", "1000", "1e+15"},
        {"digital", "terabyte", "bit", "1000", "8e+15"},
        {"digital", "petabyte", "bit", "1000", "8e+18"},
        {"duration", "nanosecond", "second", "1000", "1e-06"},
        {"duration", "microsecond", "second", "1000", "0.001"},
        {"duration", "millisecond", "second", "1000", "1.0"},
        {"duration", "second", "second", "1000", "1000.0"},
        {"duration", "minute", "second", "1000", "60000.0"},
        {"duration", "hour", "second", "1000", "3600000.0"},
        {"duration", "day", "second", "1000", "86400000.0"},
        {"duration", "day-person", "second", "1000", "86400000.0"},
        {"duration", "week", "second", "1000", "604800000.0"},
        {"duration", "week-person", "second", "1000", "604800000.0"},
        {"electric-current", "milliampere", "ampere", "1000", "1.0"},
        {"electric-current", "ampere", "ampere", "1000", "1000.0"},
        {"electric-resistance", "ohm", "kilogram-square-meter-per-cubic-second-square-ampere", "1000",
         "1000.0"},
        {"energy", "electronvolt", "kilogram-square-meter-per-square-second", "1000", "1.602177e-16"},
        {"energy", "dalton", "kilogram-square-meter-per-square-second", "1000", "1.492418e-07"},
        {"energy", "joule", "kilogram-square-meter-per-square-second", "1000", "1000.0"},
        {"energy", "newton-meter", "kilogram-square-meter-per-square-second", "1000", "1000.0"},
        {"energy", "pound-force-foot", "kilogram-square-meter-per-square-second", "1000", "1355.818"},
        {"energy", "calorie", "kilogram-square-meter-per-square-second", "1000", "4184.0"},
        {"energy", "kilojoule", "kilogram-square-meter-per-square-second", "1000", "1000000.0"},
        {"energy", "british-thermal-unit", "kilogram-square-meter-per-square-second", "1000",
         "1055060.0"},
        {"energy", "foodcalorie", "kilogram-square-meter-per-square-second", "1000", "4184000.0"},
        {"energy", "kilocalorie", "kilogram-square-meter-per-square-second", "1000", "4184000.0"},
        {"energy", "kilowatt-hour", "kilogram-square-meter-second-per-cubic-second", "1000",
         "3600000000.0"},
        {"energy", "therm-us", "kilogram-square-meter-per-square-second", "1000", "1.05506e+11"},
        {"force", "newton", "kilogram-meter-per-square-second", "1000", "1000.0"},
        {"force", "pound-force", "kilogram-meter-per-square-second", "1000", "4448.222"},
        {"frequency", "hertz", "revolution-per-second", "1000", "1000.0"},
        {"frequency", "kilohertz", "revolution-per-second", "1000", "1000000.0"},
        {"frequency", "megahertz", "revolution-per-second", "1000", "1000000000.0"},
        {"frequency", "gigahertz", "revolution-per-second", "1000", "1e+12"},
        {"graphics", "pixel", "pixel", "1000", "1000.0"},
        {"graphics", "megapixel", "pixel", "1000", "1000000000.0"},
        {"length", "picometer", "meter", "1000", "1e-09"},
        {"length", "nanometer", "meter", "1000", "1e-06"},
        {"length", "micrometer", "meter", "1000", "0.001"},
        {"length", "point", "meter", "1000", "0.3527778"},
        {"length", "millimeter", "meter", "1000", "1.0"},
        {"length", "centimeter", "meter", "1000", "10.0"},
        {"length", "inch", "meter", "1000", "25.4"},
        {"length", "decimeter", "meter", "1000", "100.0"},
        {"length", "foot", "meter", "1000", "304.8"},
        {"length", "yard", "meter", "1000", "914.4"},
        {"length", "meter", "meter", "1000", "1000.0"},
        {"length", "fathom", "meter", "1000", "1828.8"},
        {"length", "furlong", "meter", "1000", "201168.0"},
        {"length", "kilometer", "meter", "1000", "1000000.0"},
        {"length", "mile", "meter", "1000", "1609344.0"},
        {"length", "nautical-mile", "meter", "1000", "1852000.0"},
        {"length", "mile-scandinavian", "meter", "1000", "10000000.0"},
        {"length", "solar-radius", "meter", "1000", "6.957e+11"},
        {"length", "astronomical-unit", "meter", "1000", "1.495979e+14"},
        {"length", "light-year", "meter", "1000", "9.46073e+18"},
        {"length", "parsec", "meter", "1000", "3.085678e+19"},
        // {"luminous-flux", "lux", "candela-square-meter-per-square-meter", "1000", "1000.0"},
        {"mass", "microgram", "kilogram", "1000", "1e-06"},
        {"mass", "milligram", "kilogram", "1000", "0.001"},
        {"mass", "carat", "kilogram", "1000", "0.2"},
        {"mass", "gram", "kilogram", "1000", "1.0"},
        {"mass", "ounce", "kilogram", "1000", "28.34952"},
        {"mass", "ounce-troy", "kilogram", "1000", "31.10348"},
        {"mass", "pound", "kilogram", "1000", "453.5924"},
        {"mass", "kilogram", "kilogram", "1000", "1000.0"},
        {"mass", "stone", "kilogram", "1000", "6350.293"},
        {"mass", "ton", "kilogram", "1000", "907184.7"},
        {"mass", "metric-ton", "kilogram", "1000", "1000000.0"},
        {"mass", "earth-mass", "kilogram", "1000", "5.9722e+27"},
        {"mass", "solar-mass", "kilogram", "1000", "1.98847e+33"},
        {"mass-density", "milligram-per-deciliter", "kilogram-per-cubic-meter", "1000", "10.0"},
        //{"portion", "part-per-million", "portion", "1000", "0.001"},
        // {"portion", "permyriad", "portion", "1000", "0.1"},
        // {"portion", "permille", "portion", "1000", "1.0"},
        // {"portion", "percent", "portion", "1000", "10.0"},
        // {"portion", "karat", "portion", "1000", "41.66667"},
        {"power", "milliwatt", "kilogram-square-meter-per-cubic-second", "1000", "1.0"},
        {"power", "watt", "kilogram-square-meter-per-cubic-second", "1000", "1000.0"},
        {"power", "horsepower", "kilogram-square-meter-per-cubic-second", "1000", "745699.9"},
        {"power", "kilowatt", "kilogram-square-meter-per-cubic-second", "1000", "1000000.0"},
        {"power", "megawatt", "kilogram-square-meter-per-cubic-second", "1000", "1000000000.0"},
        {"power", "gigawatt", "kilogram-square-meter-per-cubic-second", "1000", "1e+12"},
        {"power", "solar-luminosity", "kilogram-square-meter-per-cubic-second", "1000", "3.828e+29"},
        {"pressure", "pascal", "kilogram-per-meter-square-second", "1000", "1000.0"},
        // TODO(problem in MeasureUnit) //{"pressure", "millimeter-of-mercury",
        // "kilogram-per-meter-square-second", "1000", "13332.24"},
        {"pressure", "hectopascal", "kilogram-per-meter-square-second", "1000", "100000.0"},
        {"pressure", "millibar", "kilogram-per-meter-square-second", "1000", "100000.0"},
        {"pressure", "kilopascal", "kilogram-per-meter-square-second", "1000", "1000000.0"},
        {"pressure", "inch-hg", "kilogram-per-meter-square-second", "1000", "3386389.0"},
        {"pressure", "pound-force-per-square-inch", "kilogram-meter-per-square-meter-square-second",
         "1000", "6894757.0"},
        {"pressure", "bar", "kilogram-per-meter-square-second", "1000", "100000000.0"},
        {"pressure", "atmosphere", "kilogram-per-meter-square-second", "1000", "101325000.0"},
        {"pressure", "megapascal", "kilogram-per-meter-square-second", "1000", "1000000000.0"},
        {"resolution", "dot-per-inch", "pixel-per-meter", "1000", "39370.08"},
        {"resolution", "pixel-per-inch", "pixel-per-meter", "1000", "39370.08"},
        {"resolution", "dot-per-centimeter", "pixel-per-meter", "1000", "100000.0"},
        {"resolution", "pixel-per-centimeter", "pixel-per-meter", "1000", "100000.0"},
        {"speed", "kilometer-per-hour", "meter-per-second", "1000", "277.7778"},
        {"speed", "mile-per-hour", "meter-per-second", "1000", "447.04"},
        {"speed", "knot", "meter-per-second", "1000", "514.4444"},
        {"speed", "meter-per-second", "meter-per-second", "1000", "1000.0"},
        //    {"substance-amount", "mole", "item", "1000", "6.022141e+26"},
        {"temperature", "fahrenheit", "kelvin", "1000", "810.9278"},
        {"temperature", "kelvin", "kelvin", "1000", "1000.0"},
        {"temperature", "celsius", "kelvin", "1000", "1273.15"},
        {"typewidth", "em", "em", "1000", "1000.0"},
        {"voltage", "volt", "kilogram-square-meter-per-cubic-second-ampere", "1000", "1000.0"},
        {"volume", "cubic-centimeter", "cubic-meter", "1000", "0.001"},
        {"volume", "milliliter", "cubic-meter", "1000", "0.001"},
        {"volume", "teaspoon", "cubic-meter", "1000", "0.004928922"},
        {"volume", "centiliter", "cubic-meter", "1000", "0.01"},
        {"volume", "tablespoon", "cubic-meter", "1000", "0.01478676"},
        {"volume", "cubic-inch", "cubic-meter", "1000", "0.01638706"},
        {"volume", "fluid-ounce-imperial", "cubic-meter", "1000", "0.02841306"},
        {"volume", "fluid-ounce", "cubic-meter", "1000", "0.02957353"},
        {"volume", "deciliter", "cubic-meter", "1000", "0.1"},
        {"volume", "cup", "cubic-meter", "1000", "0.2365882"},
        {"volume", "cup-metric", "cubic-meter", "1000", "0.25"},
        {"volume", "pint", "cubic-meter", "1000", "0.4731765"},
        {"volume", "pint-metric", "cubic-meter", "1000", "0.5"},
        {"volume", "quart", "cubic-meter", "1000", "0.9463529"},
        {"volume", "liter", "cubic-meter", "1000", "1.0"},
        {"volume", "gallon", "cubic-meter", "1000", "3.785412"},
        {"volume", "gallon-imperial", "cubic-meter", "1000", "4.54609"},
        {"volume", "cubic-foot", "cubic-meter", "1000", "28.31685"},
        {"volume", "bushel", "cubic-meter", "1000", "35.23907"},
        {"volume", "hectoliter", "cubic-meter", "1000", "100.0"},
        {"volume", "barrel", "cubic-meter", "1000", "158.9873"},
        {"volume", "cubic-yard", "cubic-meter", "1000", "764.5549"},
        {"volume", "cubic-meter", "cubic-meter", "1000", "1000.0"},
        {"volume", "megaliter", "cubic-meter", "1000", "1000000.0"},
        {"volume", "acre-foot", "cubic-meter", "1000", "1233482.0"},
        {"volume", "cubic-kilometer", "cubic-meter", "1000", "1e+12"},
        {"volume", "cubic-mile", "cubic-meter", "1000", "4.168182e+12"},
        {"year-duration", "month", "year", "1000", "83.33333"},
        {"year-duration", "month-person", "year", "1000", "83.33333"},
        {"year-duration", "year", "year", "1000", "1000.0"},
        {"year-duration", "year-person", "year", "1000", "1000.0"},
        {"year-duration", "decade", "year", "1000", "10000.0"},
        {"year-duration", "century", "year", "1000", "100000.0"},
    };

    for (const auto &testCase : testCases) {
        // std::cerr << testCase.source.data() << " " << testCase.target.data() << std::endl;

        UErrorCode status = U_ZERO_ERROR;
        MeasureUnit sourceUnit = MeasureUnit::forIdentifier(testCase.source, status);
        MeasureUnit targetUnit = MeasureUnit::forIdentifier(testCase.target, status);

        UnitConverter converter(sourceUnit, targetUnit, status);

        double actual = converter.convert(strToDouble(testCase.inputValue));

        assertEqualsNear(testCase.category.data(), strToDouble(testCase.expectedValue), actual, 0.0001);
    }
}

void UnitsTest::testCLDRUnitsTests2() {
    struct {
        const StringPiece category;
        const StringPiece source;
        const StringPiece target;
        const StringPiece inputValue;
        const StringPiece expectedValue;
    } testCases[]{
        {"resolution", "dot-per-centimeter", "pixel-per-meter", "1000", "100000.0"},
    };

    for (const auto &testCase : testCases) {
        UErrorCode status = U_ZERO_ERROR;
        MeasureUnit sourceUnit = MeasureUnit::forIdentifier(testCase.source, status);
        MeasureUnit targetUnit = MeasureUnit::forIdentifier(testCase.target, status);

        UnitConverter converter(sourceUnit, targetUnit, status);

        double actual = converter.convert(strToDouble(testCase.inputValue));

        assertEqualsNear(testCase.category.data(), strToDouble(testCase.expectedValue), actual, 0.0001);
    }
}

/**
 * Trims whitespace (spaces only) off of the specified string.
 * @param field is two pointers pointing at the start and end of the string.
 * @return A StringPiece with initial and final space characters trimmed off.
 */
StringPiece trimField(char *(&field)[2]) {
    char *start = field[0];
    while (start < field[1] && (start[0]) == ' ') {
        start++;
    }
    int32_t length = (int32_t)(field[1] - start);
    while (length > 0 && (start[length - 1]) == ' ') {
        length--;
    }
    return StringPiece(start, length);
}

/**
 * WIP(hugovdm): deals with a single data-driven unit test for unit conversions.
 * This is a UParseLineFn as required by u_parseDelimitedFile.
 */
void runDataDrivenConversionTest(void *context, char *fields[][2], int32_t fieldCount,
                                 UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) { return; }
    (void)fieldCount; // unused UParseLineFn variable
    IcuTestErrorCode status(*(UnitsTest *)context, "unitsTestDatalineFn");

    StringPiece quantity = trimField(fields[0]);
    StringPiece x = trimField(fields[1]);
    StringPiece y = trimField(fields[2]);
    StringPiece commentConversionFormula = trimField(fields[3]);
    StringPiece utf8Expected = trimField(fields[4]);

    UNumberFormat *nf = unum_open(UNUM_DEFAULT, NULL, -1, "en_US", NULL, status);
    if (status.errIfFailureAndReset("unum_open failed")) { return; }
    UnicodeString uExpected = UnicodeString::fromUTF8(utf8Expected);
    double expected = unum_parseDouble(nf, uExpected.getBuffer(), uExpected.length(), 0, status);
    unum_close(nf);
    if (status.errIfFailureAndReset("unum_parseDouble(\"%.*s\") failed", uExpected.length(),
                                    uExpected.getBuffer())) {
        return;
    }

    MeasureUnit sourceUnit = MeasureUnit::forIdentifier(x, status);
    if (status.errIfFailureAndReset("forIdentifier(\"%.*s\")", x.length(), x.data())) { return; }

    MeasureUnit targetUnit = MeasureUnit::forIdentifier(y, status);
    if (status.errIfFailureAndReset("forIdentifier(\"%.*s\")", y.length(), y.data())) { return; }

    // WIP(hugovdm): Debug branch is for useful output while UnitConverter is still segfaulting.
    UBool FIXME_skip_UnitConverter = FALSE;
    if (FIXME_skip_UnitConverter) {
        fprintf(stderr,
                "FIXME: skipping constructing UnitConverter(«%s», «%s», status) because it is "
                "segfaulting.\n",
                sourceUnit.getIdentifier(), targetUnit.getIdentifier());

        fprintf(stderr,
                "Quantity/Category: \"%.*s\", "
                "Converting: \"1000 %.*s\" to \"%.*s\", Expecting: %f, "
                "commentConversionFormula: \"%.*s\"\n",
                quantity.length(), quantity.data(), x.length(), x.data(), y.length(), y.data(), expected,
                commentConversionFormula.length(), commentConversionFormula.data());
    } else {
        UnitConverter converter(sourceUnit, targetUnit, status);
        if (status.errIfFailureAndReset("constructor: UnitConverter(<%s>, <%s>, status)",
                                        sourceUnit.getIdentifier(), targetUnit.getIdentifier())) {
            return;
        }
        double got = converter.convert(1000);
        ((UnitsTest*)context)->assertEqualsNear(fields[0][0], expected, got, 0.0001);
    }
}

/**
 * Runs data-driven unit tests for unit conversion. It looks for the test cases
 * in source/test/testdata/units/unitsTest.txt, which originates in CLDR.
 */
void UnitsTest::testConversions() {
    const char *filename = "unitsTest.txt";
    const int32_t kNumFields = 5;
    char *fields[kNumFields][2];

    IcuTestErrorCode errorCode(*this, "UnitsTest::testConversions");
    const char *sourceTestDataPath = getSourceTestData(errorCode);
    if (errorCode.errIfFailureAndReset("unable to find the source/test/testdata "
                                       "folder (getSourceTestData())")) {
        return;
    }

    CharString path(sourceTestDataPath, errorCode);
    path.appendPathPart("units", errorCode);
    path.appendPathPart(filename, errorCode);
    u_parseDelimitedFile(path.data(), ';', fields, kNumFields, runDataDrivenConversionTest, this,
                         errorCode);
    if (errorCode.errIfFailureAndReset("error parsing %s: %s\n", path.data(), u_errorName(errorCode))) {
        return;
    }
}

/**
 * This class represents the output fields from unitPreferencesTest.txt. Please
 * see the documentation at the top of that file for details.
 *
 * For "mixed units" output, there are more (repeated) output fields. The last
 * output unit has the expected output specified as both a rational fraction and
 * a decimal fraction. This class ignores rational fractions, and expects to
 * find a decimal fraction for each output unit.
 */
class ExpectedOutput {
  private:
    // Counts number of units in the output. When this is more than one, we have
    // "mixed units" in the expected output.
    int _compoundCount = 0;

    // Counts how many fields were skipped: we expect to skip only one per
    // output unit type (the rational fraction).
    int _skippedFields = 0;

    // The expected output units: more than one for "mixed units".
    MeasureUnit _measureUnits[3];

    // The amounts of each of the output units.
    double _amounts[3];

  public:
    /**
     * Parse an expected output field from the test data file.
     *
     * @param output may be a string representation of an integer, a rational
     * fraction, a decimal fraction, or it may be a unit identifier. Whitespace
     * should already be trimmed. This function ignores rational fractions,
     * saving only decimal fractions and their unit identifiers.
     * @return true if the field was successfully parsed, false if parsing
     * failed.
     */
    void parseOutputField(StringPiece output, UErrorCode &errorCode) {
        if (U_FAILURE(errorCode)) return;
        DecimalQuantity dqOutputD;

        dqOutputD.setToDecNumber(output, errorCode);
        if (U_SUCCESS(errorCode)) {
            _amounts[_compoundCount] = dqOutputD.toDouble();
            return;
        } else if (errorCode == U_DECIMAL_NUMBER_SYNTAX_ERROR) {
            // Not a decimal fraction, it might be a rational fraction or a unit
            // identifier: continue.
            errorCode = U_ZERO_ERROR;
        } else {
            // Unexpected error, so we propagate it.
            return;
        }

        _measureUnits[_compoundCount] = MeasureUnit::forIdentifier(output, errorCode);
        if (U_SUCCESS(errorCode)) {
            _compoundCount++;
            _skippedFields = 0;
            return;
        }
        _skippedFields++;
        if (_skippedFields < 2) {
            // We are happy skipping one field per output unit: we want to skip
            // rational fraction fiels like "11 / 10".
            errorCode = U_ZERO_ERROR;
            return;
        } else {
            // Propagate the error.
            return;
        }
    }

    /**
     * Produces an output string for debug purposes.
     */
    std::string toDebugString() {
        std::string result;
        for (int i = 0; i < _compoundCount; i++) {
            result += std::to_string(_amounts[i]);
            result += " ";
            result += _measureUnits[i].getIdentifier();
            result += " ";
        }
        return result;
    }
};

/**
 * WIP(hugovdm): deals with a single data-driven unit test for unit preferences.
 * This is a UParseLineFn as required by u_parseDelimitedFile.
 */
void unitPreferencesTestDataLineFn(void *context, char *fields[][2], int32_t fieldCount,
                                   UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) return;
    UnitsTest *intltest = (UnitsTest *)context;
    IcuTestErrorCode status(*(UnitsTest *)context, "unitPreferencesTestDatalineFn");

    if (!intltest->assertTrue(u"unitPreferencesTestDataLineFn expects 9 fields for simple and 11 "
                              u"fields for compound. Other field counts not yet supported. ",
                              fieldCount == 9 || fieldCount == 11)) {
        return;
    }

    StringPiece quantity = trimField(fields[0]);
    StringPiece usage = trimField(fields[1]);
    StringPiece region = trimField(fields[2]);
    // Unused // StringPiece inputR = trimField(fields[3]);
    StringPiece inputD = trimField(fields[4]);
    StringPiece inputUnit = trimField(fields[5]);
    ExpectedOutput output;
    for (int i = 6; i < fieldCount; i++) {
        output.parseOutputField(trimField(fields[i]), status);
    }
    if (status.errIfFailureAndReset("parsing unitPreferencesTestData.txt test case: %s", fields[0][0])) {
        return;
    }

    DecimalQuantity dqInputD;
    dqInputD.setToDecNumber(inputD, status);
    if (status.errIfFailureAndReset("parsing decimal quantity: \"%.*s\"", inputD.length(),
                                    inputD.data())) {
        *pErrorCode = U_PARSE_ERROR;
        return;
    }
    double inputAmount = dqInputD.toDouble();

    MeasureUnit inputMeasureUnit = MeasureUnit::forIdentifier(inputUnit, status);
    if (status.errIfFailureAndReset("forIdentifier(\"%.*s\")", inputUnit.length(), inputUnit.data())) {
        *pErrorCode = U_PARSE_ERROR;
        return;
    }

    // WIP(hugovdm): hook this up to actual tests.
    //
    // Possible after merging in younies/tryingdouble:
    // UnitConverter converter(sourceUnit, targetUnit, *pErrorCode);
    // double got = converter.convert(1000, *pErrorCode);
    // ((UnitsTest*)context)->assertEqualsNear(quantity.data(), expected, got, 0.0001);
    //
    // In the meantime, printing to stderr.
    fprintf(stderr,
            "Quantity (Category): \"%.*s\", Usage: \"%.*s\", Region: \"%.*s\", "
            "Input: \"%f %s\", Expected Output: %s\n",
            quantity.length(), quantity.data(), usage.length(), usage.data(), region.length(),
            region.data(), inputAmount, inputMeasureUnit.getIdentifier(),
            output.toDebugString().c_str());
}

/**
 * Parses the format used by unitPreferencesTest.txt, calling lineFn for each
 * line.
 *
 * This is a modified version of u_parseDelimitedFile, customised for
 * unitPreferencesTest.txt, due to it having a variable number of fields per
 * line.
 */
void parsePreferencesTests(const char *filename, char delimiter, char *fields[][2],
                           int32_t maxFieldCount, UParseLineFn *lineFn, void *context,
                           UErrorCode *pErrorCode) {
    FileStream *file;
    char line[10000];
    char *start, *limit;
    int32_t i;

    if (U_FAILURE(*pErrorCode)) { return; }

    if (fields == NULL || lineFn == NULL || maxFieldCount <= 0) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if (filename == NULL || *filename == 0 || (*filename == '-' && filename[1] == 0)) {
        filename = NULL;
        file = T_FileStream_stdin();
    } else {
        file = T_FileStream_open(filename, "r");
    }
    if (file == NULL) {
        *pErrorCode = U_FILE_ACCESS_ERROR;
        return;
    }

    while (T_FileStream_readLine(file, line, sizeof(line)) != NULL) {
        /* remove trailing newline characters */
        u_rtrim(line);

        start = line;
        *pErrorCode = U_ZERO_ERROR;

        /* skip this line if it is empty or a comment */
        if (*start == 0 || *start == '#') { continue; }

        /* remove in-line comments */
        limit = uprv_strchr(start, '#');
        if (limit != NULL) {
            /* get white space before the pound sign */
            while (limit > start && U_IS_INV_WHITESPACE(*(limit - 1))) {
                --limit;
            }

            /* truncate the line */
            *limit = 0;
        }

        /* skip lines with only whitespace */
        if (u_skipWhitespace(start)[0] == 0) { continue; }

        /* for each field, call the corresponding field function */
        for (i = 0; i < maxFieldCount; ++i) {
            /* set the limit pointer of this field */
            limit = start;
            while (*limit != delimiter && *limit != 0) {
                ++limit;
            }

            /* set the field start and limit in the fields array */
            fields[i][0] = start;
            fields[i][1] = limit;

            /* set start to the beginning of the next field, if any */
            start = limit;
            if (*start != 0) {
                ++start;
            } else {
                break;
            }
        }
        if (i == maxFieldCount) { *pErrorCode = U_PARSE_ERROR; }
        int fieldCount = i + 1;

        /* call the field function */
        lineFn(context, fields, fieldCount, pErrorCode);
        if (U_FAILURE(*pErrorCode)) { break; }
    }

    if (filename != NULL) { T_FileStream_close(file); }
}

/**
 * Runs data-driven unit tests for unit preferences.
 */
void UnitsTest::testPreferences() {
    const char *filename = "unitPreferencesTest.txt";
    const int32_t maxFields = 11;
    char *fields[maxFields][2];

    IcuTestErrorCode errorCode(*this, "UnitsTest::testPreferences");
    const char *sourceTestDataPath = getSourceTestData(errorCode);
    if (errorCode.errIfFailureAndReset("unable to find the source/test/testdata "
                                       "folder (getSourceTestData())")) {
        return;
    }

    CharString path(sourceTestDataPath, errorCode);
    path.appendPathPart("units", errorCode);
    path.appendPathPart(filename, errorCode);

    parsePreferencesTests(path.data(), ';', fields, maxFields, unitPreferencesTestDataLineFn, this,
                          errorCode);
    if (errorCode.errIfFailureAndReset("error parsing %s: %s\n", path.data(), u_errorName(errorCode))) {
        return;
    }
}

void UnitsTest::testGetUnitsData() {
    struct {
        const char *outputRegion;
        const char *usage;
        const char *inputUnit;
    } testCases[]{
        {"US", "fluid", "centiliter"},
        {"BZ", "weather", "celsius"},
        {"ZA", "road", "yard"},
        {"XZ", "zz_nonexistant", "dekagram"},
    };
    for (const auto &t : testCases) {
        logln("test case: %s %s %s\n", t.outputRegion, t.usage, t.inputUnit);
        // UErrorCode status = U_ZERO_ERROR;
        IcuTestErrorCode status(*this, "testGetUnitsData");
        MeasureUnit inputUnit = MeasureUnit::forIdentifier(t.inputUnit, status);

        CharString category;
        MeasureUnit baseUnit;
        MaybeStackVector<ConversionRateInfo> conversionInfo;
        MaybeStackVector<UnitPreference> unitPreferences;
        getUnitsData(t.outputRegion, t.usage, inputUnit, category, baseUnit, conversionInfo,
                     unitPreferences, status);
        if (status.errIfFailureAndReset("getUnitsData(\"%s\", \"%s\", \"%s\", ...)", t.outputRegion,
                                        t.usage, t.inputUnit)) {
            continue;
        }
        logln("category: \"%s\", baseUnit: \"%s\"", category.data(), baseUnit.getIdentifier());
        for (int i = 0; i < conversionInfo.length(); i++) {
            ConversionRateInfo *cri;
            cri = conversionInfo[i];
            logln("conversionInfo %d: source=\"%s\", target=\"%s\", factor=\"%s\", offset=\"%s\"", i,
                  cri->source.data(), cri->target.data(), cri->factor.data(), cri->offset.data());
        }
        for (int i = 0; i < unitPreferences.length(); i++) {
            UnitPreference *up;
            up = unitPreferences[i];
            logln("unitPreference %d: \"%s\", geq=%f, skeleton=\"%s\"", i, up->unit.data(), up->geq,
                  up->skeleton.data());
        }
    }
}

/**
 * Tests different return statuses depending on the input.
 */
void UnitsTest::testStatus() {}

#endif /* #if !UCONFIG_NO_FORMATTING */
