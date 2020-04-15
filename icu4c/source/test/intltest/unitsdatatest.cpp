// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#if !UCONFIG_NO_FORMATTING

#include "unitsdata.h"
#include "intltest.h"

class UnitsDataTest : public IntlTest {
  public:
    UnitsDataTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = NULL);

    void testGetAllConversionRates();
    void testGetConversionRateInfo();
};

extern IntlTest *createUnitsDataTest() { return new UnitsDataTest(); }

void UnitsDataTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) { logln("TestSuite UnitsDataTest: "); }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testGetAllConversionRates);
    TESTCASE_AUTO(testGetConversionRateInfo);
    TESTCASE_AUTO_END;
}

void UnitsDataTest::testGetAllConversionRates() {
    IcuTestErrorCode status(*this, "testGetAllConversionRates");
    MaybeStackVector<ConversionRateInfo> conversionInfo;
    getAllConversionRates(conversionInfo, status);

    // Convenience output for debugging
    for (int i = 0; i < conversionInfo.length(); i++) {
        ConversionRateInfo *cri = conversionInfo[i];
        logln("* conversionInfo %d: source=\"%s\", baseUnit=\"%s\", factor=\"%s\", offset=\"%s\"", i,
              cri->sourceUnit.data(), cri->baseUnit.data(), cri->factor.data(), cri->offset.data());
        assertTrue("sourceUnit", cri->sourceUnit.length() > 0);
        assertTrue("baseUnit", cri->baseUnit.length() > 0);
        assertTrue("factor", cri->factor.length() > 0);
    }
}

// TODO(hugovdm): drop this test case, maybe even before ever merging it into
// units-staging. Not immediately deleting it to prove backward compatibility:
void UnitsDataTest::testGetConversionRateInfo() {
    const int MAX_NUM_RATES = 5;
    struct {
        // The source unit passed to getConversionRateInfo.
        const char *sourceUnit;
        // The target unit passed to getConversionRateInfo.
        const char *targetUnit;
        // Expected: units whose conversion rates are expected in the results.
        const char *expectedOutputs[MAX_NUM_RATES];
    } testCases[]{
        {"meter", "kilometer", {"meter", NULL, NULL, NULL, NULL}},
        {"liter", "gallon", {"liter", "gallon", NULL, NULL, NULL}},
        {"watt", "horsepower", {"watt", "horsepower", NULL, NULL, NULL}},
        {"mile-per-hour", "dekameter-per-hour", {"mile", "hour", "meter", NULL, NULL}},

        // Sequence
        {"stone-and-pound", "ton", {"pound", "stone", "ton", NULL, NULL}},

        // Compound
        {"centimeter-per-square-milligram",
         "inch-per-square-ounce",
         {"meter", "gram", "inch", "ounce", NULL}},
        {"therm-us",
         "kilogram-square-meter-per-square-second",
         {"therm-us", "kilogram", "meter", "second", NULL}},

        // "Reciprocal" example: consumption and consumption-inverse
        {"liter-per-100-kilometer",
         "mile-per-gallon",
         {"liter", "100-kilometer", "mile", "gallon", NULL}},
    };
    for (const auto &t : testCases) {
        logln("---testing: source=\"%s\", target=\"%s\"", t.sourceUnit, t.targetUnit);
        IcuTestErrorCode status(*this, "testGetConversionRateInfo");

        MaybeStackVector<MeasureUnit> units;
        MeasureUnit *item = units.emplaceBack(MeasureUnit::forIdentifier(t.sourceUnit, status));
        if (!item) {
            status.set(U_MEMORY_ALLOCATION_ERROR);
            return;
        }
        item = units.emplaceBack(MeasureUnit::forIdentifier(t.targetUnit, status));
        if (!item) {
            status.set(U_MEMORY_ALLOCATION_ERROR);
            return;
        }

        MaybeStackVector<ConversionRateInfo> conversionInfo = getConversionRatesInfo(units, status);
        if (status.errIfFailureAndReset("getConversionRatesInfo({<%s>, <%s>}, ...)", t.sourceUnit,
                                        t.targetUnit)) {
            continue;
        }

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
        // We're now fetching all units, not just the needed ones, so this is no
        // longer a valid test:
        // assertEquals("number of conversion rates", countExpected, conversionInfo.length());

        // // Convenience output for debugging
        // for (int i = 0; i < conversionInfo.length(); i++) {
        //     ConversionRateInfo *cri = conversionInfo[i];
        //     logln("* conversionInfo %d: source=\"%s\", baseUnit=\"%s\", factor=\"%s\", "
        //           "offset=\"%s\"",
        //           i, cri->sourceUnit.data(), cri->baseUnit.data(), cri->factor.data(),
        //           cri->offset.data());
        // }
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
