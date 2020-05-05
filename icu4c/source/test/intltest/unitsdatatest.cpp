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
    void testGetPreferences();
};

extern IntlTest *createUnitsDataTest() { return new UnitsDataTest(); }

void UnitsDataTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) { logln("TestSuite UnitsDataTest: "); }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testGetAllConversionRates);
    TESTCASE_AUTO(testGetPreferences);
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

class UnitPreferencesOpenedUp : public UnitPreferences {
  public:
    UnitPreferencesOpenedUp(UErrorCode &status) : UnitPreferences(status) {};
    const MaybeStackVector<UnitPreferenceMetadata> *getInternalMetadata() const { return &metadata_; }
    const MaybeStackVector<UnitPreference> *getInternalUnitPrefs() const { return &unitPrefs_; }
};

/**
 * This test is dependent upon CLDR Data: when the preferences change, the test
 * may fail: see the constants for expected Max/Min unit identifiers, for US and
 * World, and for Roads and default lengths.
 */
void UnitsDataTest::testGetPreferences() {
    const char* USRoadMax = "mile";
    const char* USRoadMin = "foot";
    const char* USLenMax = "mile";
    const char* USLenMin = "inch";
    const char* WorldRoadMax = "kilometer";
    const char* WorldRoadMin = "meter";
    const char* WorldLenMax = "kilometer";
    const char* WorldLenMin = "centimeter";
    struct TestCase {
        const char *name;
        const char *category;
        const char *usage;
        const char *region;
        const char *expectedBiggest;
        const char *expectedSmallest;
    } testCases[]{
        {"US road", "length", "road", "US", USRoadMax, USRoadMin},
        {"001 road", "length", "road", "001", WorldRoadMax, WorldRoadMin},
        {"US lengths", "length", "default", "US", USLenMax, USLenMin},
        {"001 lengths", "length", "default", "001", WorldLenMax, WorldLenMin},
        {"XX road falls back to 001", "length", "road", "XX", WorldRoadMax, WorldRoadMin},
        {"XX default falls back to 001", "length", "default", "XX", WorldLenMax, WorldLenMin},
        {"Unknown usage US", "length", "foobar", "US", USLenMax, USLenMin},
        {"Unknown usage 001", "length", "foobar", "XX", WorldLenMax, WorldLenMin},
    };
    IcuTestErrorCode status(*this, "testGetPreferences");
    UnitPreferencesOpenedUp preferences(status);
    auto *metadata = preferences.getInternalMetadata();
    auto *unitPrefs = preferences.getInternalUnitPrefs();
    assertTrue(UnicodeString("Metadata count: ") + metadata->length() + " > 200",
               metadata->length() > 200);
    assertTrue(UnicodeString("Preferences count: ") + unitPrefs->length() + " > 250",
               unitPrefs->length() > 250);

    // Dump all preferences... TODO: remove? This was just debugging/development output.
    logln("Unit Preferences:");
    for (int32_t i = 0; i < metadata->length(); i++) {
        logln("%d: category %s, usage %s, region %s, offset %d, count %d", i,
              (*metadata)[i]->category.data(), (*metadata)[i]->usage.data(),
              (*metadata)[i]->region.data(), (*metadata)[i]->prefsOffset, (*metadata)[i]->prefsCount);
        for (int32_t j = (*metadata)[i]->prefsOffset;
             j < (*metadata)[i]->prefsOffset + (*metadata)[i]->prefsCount; j++) {
            auto p = (*unitPrefs)[j];
            logln("  %d: unit %s, geq %f, skeleton \"%s\"", j, p->unit.data(), p->geq, p->skeleton.data());
        }
    }

    for (const auto &t : testCases) {
        MaybeStackVector<UnitPreference> prefs;
        logln(t.name);
        preferences.getPreferencesFor(t.category, t.usage, t.region, &prefs, status);
        if (prefs.length() > 0) {
            assertEquals(UnicodeString(t.name) + " - max unit", t.expectedBiggest,
                         prefs[0]->unit.data());
            assertEquals(UnicodeString(t.name) + " - min unit", t.expectedSmallest,
                         prefs[prefs.length() - 1]->unit.data());
        } else {
            errln(UnicodeString(t.name) + ": failed to find preferences");
        }
        status.errIfFailureAndReset("testCase '%s'", t.name);
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
