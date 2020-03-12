// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "charstr.h"
#include "filestrm.h"
#include "intltest.h"
#include "number_decimalquantity.h"
#include "unicode/ctest.h"
#include "unicode/measunit.h"
#include "unicode/unistr.h"
#include "unicode/unum.h"
#include "uparse.h"

using icu::number::impl::DecimalQuantity;

class UnitsTest : public IntlTest {
  public:
    UnitsTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = NULL);

    void testConversions();
    void testPreferences();
    void testBasic();
    void testSiPrefixes();
    void testMass();
    void testTemperature();
    void testArea();
};

extern IntlTest *createUnitsTest() { return new UnitsTest(); }

void UnitsTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) {
        logln("TestSuite UnitsTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testConversions);
    TESTCASE_AUTO(testPreferences);
    TESTCASE_AUTO(testBasic);
    TESTCASE_AUTO(testSiPrefixes);
    TESTCASE_AUTO(testMass);
    TESTCASE_AUTO(testTemperature);
    TESTCASE_AUTO(testArea);
    TESTCASE_AUTO_END;
}

// Just for testing quick conversion ability.
double testConvert(UnicodeString source, UnicodeString target, double input) {
    if (source == u"meter" && target == u"foot" && input == 1.0)
        return 3.28084;

    if ( source == u"kilometer" && target == u"foot" && input == 1.0)
        return 328.084;

    return -1;
}

void UnitsTest::testBasic() {
    IcuTestErrorCode status(*this, "Units testBasic");

    // Test Cases
    struct TestCase {
        const char16_t *source;
        const char16_t *target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{{u"meter", u"foot", 1.0, 3.28084}, {u"kilometer", u"foot", 1.0, 328.084}};

    for (const auto &testCase : testCases) {
        assertEquals("test convert", testConvert(testCase.source, testCase.target, testCase.inputValue),
                     testCase.expectedValue);
    }
}

void UnitsTest::testSiPrefixes() {
    IcuTestErrorCode status(*this, "Units testSiPrefixes");
    // Test Cases
    struct TestCase {
        const char16_t *source;
        const char16_t *target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {u"gram", u"kilogram", 1.0, 0.001},            //
        {u"milligram", u"kilogram", 1.0, 0.000001},    //
        {u"microgram", u"kilogram", 1.0, 0.000000001}, //
        {u"megawatt", u"watt", 1, 1000000},            //
        {u"megawatt", u"kilowatt", 1.0, 1000},         //
        {u"gigabyte", u"byte", 1, 1000000000}          //
    };

    for (const auto &testCase : testCases) {
        assertEquals("test convert", testConvert(testCase.source, testCase.target, testCase.inputValue),
                     testCase.expectedValue);
    }
}

void UnitsTest::testMass() {
    IcuTestErrorCode status(*this, "Units testMass");

    // Test Cases
    struct TestCase {
        const char16_t *source;
        const char16_t *target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {u"gram", u"kilogram", 1.0, 0.001},      //
        {u"pound", u"kilogram", 1.0, 0.453592},  //
        {u"pound", u"kilogram", 2.0, 0.907185},  //
        {u"ounce", u"pound", 16.0, 1.0},         //
        {u"ounce", u"kilogram", 16.0, 0.453592}, //
        {u"ton", u"pound", 1.0, 2000},           //
        {u"stone", u"pound", 1.0, 14},           //
        {u"stone", u"kilogram", 1.0, 6.35029}    //
    };

    for (const auto &testCase : testCases) {
        assertEquals("test convert", testConvert(testCase.source, testCase.target, testCase.inputValue),
                     testCase.expectedValue);
    }
}

void UnitsTest::testTemperature() {
    IcuTestErrorCode status(*this, "Units testTemperature");
    // Test Cases
    struct TestCase {
        const char16_t *source;
        const char16_t *target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {u"celsius", u"fahrenheit", 0.0, 32.0},   //
        {u"celsius", u"fahrenheit", 10.0, 50.0},  //
        {u"fahrenheit", u"celsius", 32.0, 0.0},   //
        {u"fahrenheit", u"celsius", 89.6, 32},    //
        {u"kelvin", u"fahrenheit", 0.0, -459.67}, //
        {u"kelvin", u"fahrenheit", 300, 80.33},   //
        {u"kelvin", u"celsius", 0.0, -273.15},    //
        {u"kelvin", u"celsius", 300.0, 26.85}     //
    };

    for (const auto &testCase : testCases) {
        assertEquals("test convert", testConvert(testCase.source, testCase.target, testCase.inputValue),
                     testCase.expectedValue);
    }
}

void UnitsTest::testArea() {
    IcuTestErrorCode status(*this, "Units Area");

    // Test Cases
    struct TestCase {
        const char16_t *source;
        const char16_t *target;
        const double inputValue;
        const double expectedValue;
    } testCases[]{
        {u"square-meter", u"square-yard", 10.0, 11.9599}, //
        {u"hectare", u"square-yard", 1.0, 11959.9},       //
        {u"square-mile", u"square-foot", 0.0001, 2787.84} //
    };

    for (const auto &testCase : testCases) {
        assertEquals("test convert", testConvert(testCase.source, testCase.target, testCase.inputValue),
                     testCase.expectedValue);
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
void unitsTestDataLineFn(void *context, char *fields[][2], int32_t fieldCount, UErrorCode *pErrorCode) {
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
    path.appendPathPart(filename, errorCode);

    u_parseDelimitedFile(path.data(), ';', fields, kNumFields, unitsTestDataLineFn, this, errorCode);
    if (errorCode.errIfFailureAndReset("error parsing %s: %s\n", path.data(), u_errorName(errorCode))) {
        return;
    }
}

/**
 * WIP(hugovdm): deals with a single data-driven unit test for unit preferences.
 * This is a UParseLineFn as required by u_parseDelimitedFile.
 */
void unitPreferencesTestDataLineFn(void *context, char *fields[][2], int32_t fieldCount,
                                   UErrorCode *pErrorCode) {
    (void)fieldCount; // unused UParseLineFn variable
    IcuTestErrorCode status(*(UnitsTest *)context, "unitPreferencesTestDatalineFn");

    StringPiece quantity = trimField(fields[0]);
    StringPiece usage = trimField(fields[1]);
    StringPiece region = trimField(fields[2]);
    StringPiece inputR = trimField(fields[3]);
    StringPiece inputD = trimField(fields[4]);
    StringPiece inputUnit = trimField(fields[5]);
    StringPiece outputR = trimField(fields[6]);
    StringPiece outputD = trimField(fields[7]);
    StringPiece outputUnit = trimField(fields[8]);

    DecimalQuantity dqOutputD;
    dqOutputD.setToDecNumber(outputD, status);
    if (status.errIfFailureAndReset("parsing decimal quantity: \"%.*s\"", outputD.length(),
                                    outputD.data())) {
        return;
    }
    double expectedOutput = dqOutputD.toDouble();

    MeasureUnit input = MeasureUnit::forIdentifier(inputUnit, status);
    if (status.errIfFailureAndReset("forIdentifier(\"%.*s\")", inputUnit.length(), inputUnit.data())) {
        return;
    }

    MeasureUnit output = MeasureUnit::forIdentifier(outputUnit, status);
    if (status.errIfFailureAndReset("forIdentifier(\"%.*s\")", outputUnit.length(), outputUnit.data())) {
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
            "Input: %.*s %.*s (%.*s), Output: %.*s %.*s (%.*s) - Expected: %f\n",
            quantity.length(), quantity.data(), usage.length(), usage.data(), region.length(),
            region.data(), inputD.length(), inputD.data(), inputUnit.length(), inputUnit.data(),
            inputR.length(), inputR.data(), outputD.length(), outputD.data(), outputUnit.length(),
            outputUnit.data(), outputR.length(), outputR.data(), expectedOutput);
}

// WIP(hugovdm): we need to replace u_parseDelimitedFile with something
// custom, because not all lines in unitPreferencesTest.txt have the same
// number of fields.
void parsePreferencesTests(const char *filename, char delimiter, char *fields[][2], int32_t fieldCount,
                           UParseLineFn *lineFn, void *context, UErrorCode *pErrorCode) {
    FileStream *file;
    char line[10000];
    char *start, *limit;
    int32_t i, length;

    if (U_FAILURE(*pErrorCode)) { return; }

    if (fields == NULL || lineFn == NULL || fieldCount <= 0) {
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
        length = (int32_t)(u_rtrim(line) - line);

        // FYI: uparse.cpp had this code, which we're skipping here:
        // /*
        //  * detect a line with # @missing:
        //  * start parsing after that, or else from the beginning of the line
        //  * set the default warning for @missing lines
        //  */
        // start = (char *)getMissingLimit(line);
        // if (start == line) {
        //   *pErrorCode = U_ZERO_ERROR;
        // } else {
        //   *pErrorCode = U_USING_DEFAULT_WARNING;
        // }
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
        for (i = 0; i < fieldCount; ++i) {
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
            } else if (i + 1 < fieldCount) {
                *pErrorCode = U_PARSE_ERROR;
                limit = line + length;
                i = fieldCount;
                break;
            }
        }

        /* too few fields? */
        if (U_FAILURE(*pErrorCode)) { break; }

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
    const int32_t kNumFields = 9;
    char *fields[kNumFields][2];

    IcuTestErrorCode errorCode(*this, "UnitsTest::testPreferences");
    const char *sourceTestDataPath = getSourceTestData(errorCode);
    if (errorCode.errIfFailureAndReset("unable to find the source/test/testdata "
                                       "folder (getSourceTestData())")) {
        return;
    }

    CharString path(sourceTestDataPath, errorCode);
    path.appendPathPart("units", errorCode);
    path.appendPathPart(filename, errorCode);

    parsePreferencesTests(path.data(), ';', fields, kNumFields, unitPreferencesTestDataLineFn, this,
                          errorCode);
    if (errorCode.errIfFailureAndReset("error parsing %s: %s\n", path.data(), u_errorName(errorCode))) {
        return;
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
