// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "charstr.h"
#include "intltest.h"
#include "number_decnum.h"
#include "unicode/ctest.h"
#include "unicode/measunit.h"
#include "unicode/unistr.h"
#include "unicode/unum.h"
#include "unitconverter.h"
#include "uparse.h"
#include <iostream>

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

    void testConversions();
    void testBasic();
    void testSiPrefixes();
    void testMass();
    void testTemperature();
    void testArea();
    void testComplicatedUnits();
    void testCLDRUnitsTests();
    void testCLDRUnitsTests2();

    // TODO(younies): remove after using CLDR test cases.
    void verifyTestCase(const UnitConversionTestCase &testCase);
};

extern IntlTest *createUnitsTest() { return new UnitsTest(); }

void UnitsTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) { logln("TestSuite UnitsTest: "); }

    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testConversions);
    TESTCASE_AUTO(testBasic);
    TESTCASE_AUTO(testSiPrefixes);
    TESTCASE_AUTO(testMass);
    TESTCASE_AUTO(testTemperature);
    TESTCASE_AUTO(testArea);
    TESTCASE_AUTO(testComplicatedUnits);
    TESTCASE_AUTO(testCLDRUnitsTests);
    TESTCASE_AUTO(testCLDRUnitsTests2);
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
 * Returns a StringPiece pointing at the given field with space prefixes and
 * postfixes trimmed off.
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
static void U_CALLCONV unitsTestDataLineFn(void *context, char *fields[][2], int32_t fieldCount,
                                           UErrorCode *pErrorCode) {
    (void)fieldCount; // unused UParseLineFn variable
    IcuTestErrorCode status(*(UnitsTest *)context, "unitsTestDatalineFn");

    StringPiece quantity = trimField(fields[0]);
    StringPiece x = trimField(fields[1]);
    StringPiece y = trimField(fields[2]);
    StringPiece commentConversionFormula = trimField(fields[3]);
    StringPiece utf8Expected = trimField(fields[4]);

    UNumberFormat *nf = unum_open(UNUM_DEFAULT, NULL, -1, "en_US", NULL, pErrorCode);
    UnicodeString uExpected = UnicodeString::fromUTF8(utf8Expected);
    double expected = unum_parseDouble(nf, uExpected.getBuffer(), uExpected.length(), 0, pErrorCode);
    unum_close(nf);

    MeasureUnit sourceUnit = MeasureUnit::forIdentifier(x, status);
    if (status.errIfFailureAndReset("forIdentifier(\"%.*s\")", x.length(), x.data())) { return; }

    MeasureUnit targetUnit = MeasureUnit::forIdentifier(y, status);
    if (status.errIfFailureAndReset("forIdentifier(\"%.*s\")", y.length(), y.data())) { return; }

    // WIP(hugovdm): hook this up to actual tests.
    //
    // Possible after merging in younies/tryingdouble:
    // UnitConverter converter(sourceUnit, targetUnit, *pErrorCode);
    // double got = converter.convert(1000, *pErrorCode);
    // ((UnitsTest*)context)->assertEqualsNear(quantity.data(), expected, got, 0.0001);
    //
    // In the meantime, printing to stderr.
    fprintf(stderr,
            "Quantity (Category): \"%.*s\", "
            "Expected value of \"1000 %.*s in %.*s\": %f, "
            "commentConversionFormula: \"%.*s\", "
            "expected field: \"%.*s\"\n",
            quantity.length(), quantity.data(), x.length(), x.data(), y.length(), y.data(), expected,
            commentConversionFormula.length(), commentConversionFormula.data(), utf8Expected.length(),
            utf8Expected.data());
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
    path.appendPathPart("unitsTest.txt", errorCode);

    u_parseDelimitedFile(path.data(), ';', fields, kNumFields, unitsTestDataLineFn, this, errorCode);
    if (errorCode.errIfFailureAndReset("error parsing %s: %s\n", filename, u_errorName(errorCode))) {
        return;
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
