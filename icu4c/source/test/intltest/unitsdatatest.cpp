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
};

extern IntlTest *createUnitsDataTest() { return new UnitsDataTest(); }

void UnitsDataTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) { logln("TestSuite UnitsDataTest: "); }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testGetAllConversionRates);
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

#endif /* #if !UCONFIG_NO_FORMATTING */
