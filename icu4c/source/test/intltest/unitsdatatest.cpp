// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#if !UCONFIG_NO_FORMATTING

#include "unitsdata.h"
#include "intltest.h"

class UnitsDataTest : public IntlTest {
  public:
    UnitsDataTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = NULL);

    void testGetConversionRateInfo();
};

extern IntlTest *createUnitsDataTest() { return new UnitsDataTest(); }

void UnitsDataTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) { logln("TestSuite UnitsDataTest: "); }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testGetConversionRateInfo);
    TESTCASE_AUTO_END;
}

void UnitsDataTest::testGetConversionRateInfo() {
    const int MAX_NUM_RATES = 5;
    struct {
        // The source unit passed to getConversionRateInfo.
        const char *sourceUnit;
        // The target unit passed to getConversionRateInfo.
        const char *targetUnit;
        // Expected: units whose conversion rates are expected in the results.
        const char *expectedOutputs[MAX_NUM_RATES];
        // Expected "base unit", to serve as pivot between source and target.
        const char *expectedBaseUnit;
    } testCases[]{
        {"centimeter-per-square-milligram",
         "inch-per-square-ounce",
         {"meter", "gram", "inch", "ounce", NULL},
         "meter-per-square-kilogram"},

        {"liter", "gallon", {"liter", "gallon", NULL, NULL, NULL}, "cubic-meter"},

        // Sequence
        {"stone-and-pound", "ton", {"pound", "stone", "ton", NULL, NULL}, "kilogram"},

        {"mile-per-hour",
         "dekameter-per-hour",
         {"mile", "hour", "meter", NULL, NULL},
         "meter-per-second"},

        // Power: watt
        {"watt",
         "horsepower",
         {"watt", "horsepower", NULL, NULL, NULL},
         "kilogram-square-meter-per-cubic-second"},

        // Energy: joule
        {"therm-us",
         "kilogram-square-meter-per-square-second",
         {"therm-us", "kilogram", "meter", "second", NULL},
         "kilogram-square-meter-per-square-second"},

        // WIP/FIXME(hugovdm): I think I found a bug in targetBaseUnit.product():
        // Target Base: <kilogram-square-meter-per-square-second> x <one-per-meter> => <meter>
        //
        // // Joule-per-meter
        // {"therm-us-per-meter",
        //  "joule-per-meter",
        //  {"therm-us", "joule", "meter", NULL, NULL},
        //  "kilogram-meter-per-square-second"},

        // TODO: include capacitance test case with base unit:
        // pow4-second-square-ampere-per-kilogram-square-meter;
    };
    for (const auto &t : testCases) {
        logln("---testing: source=\"%s\", target=\"%s\", expectedBaseUnit=\"%s\"", t.sourceUnit,
              t.targetUnit, t.expectedBaseUnit);
        IcuTestErrorCode status(*this, "testGetConversionRateInfo");

        MeasureUnit baseCompoundUnit;
        MeasureUnit sourceUnit = MeasureUnit::forIdentifier(t.sourceUnit, status);
        MeasureUnit targetUnit = MeasureUnit::forIdentifier(t.targetUnit, status);
        MaybeStackVector<ConversionRateInfo> conversionInfo =
            getConversionRatesInfo(sourceUnit, targetUnit, &baseCompoundUnit, status);
        if (status.errIfFailureAndReset("getConversionRatesInfo(<%s>, <%s>, ...)",
                                        sourceUnit.getIdentifier(), targetUnit.getIdentifier())) {
            continue;
        }

        assertEquals("baseCompoundUnit returned by getConversionRatesInfo", t.expectedBaseUnit,
                     baseCompoundUnit.getIdentifier());
        int countExpected;
        for (countExpected = 0; countExpected < MAX_NUM_RATES; countExpected++) {
            auto expected = t.expectedOutputs[countExpected];
            if (expected == NULL) break;
            // Check if this conversion rate was expected
            bool found = false;
            for (int i = 0; i < conversionInfo.length(); i++) {
                auto cri = conversionInfo[i];
                if (strcmp(expected, cri->sourceUnit.data()) == 0) {
                    found = true;
                    break;
                }
            }
            assertTrue(UnicodeString("<") + expected + "> expected", found);
        }
        assertEquals("number of conversion rates", countExpected, conversionInfo.length());

        // Convenience output for debugging
        for (int i = 0; i < conversionInfo.length(); i++) {
            ConversionRateInfo *cri = conversionInfo[i];
            logln("* conversionInfo %d: source=\"%s\", baseUnit=\"%s\", factor=\"%s\", "
                  "offset=\"%s\"",
                  i, cri->sourceUnit.data(), cri->baseUnit.data(), cri->factor.data(),
                  cri->offset.data());
        }
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
