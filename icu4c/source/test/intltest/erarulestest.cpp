// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/calendar.h"
#include "unicode/gregocal.h"
#include "unicode/localpointer.h"
#include "unicode/unistr.h"
#include "unicode/timezone.h"
#include "erarules.h"
#include "erarulestest.h"

void EraRulesTest::runIndexedTest(int32_t index, UBool exec, const char* &name, char* /*par*/)
{
    if (exec) {
        logln("TestSuite EraRulesTest");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testAPIs);
    TESTCASE_AUTO(testJapanese);
    TESTCASE_AUTO_END;
}

void EraRulesTest::testAPIs() {
    const char * calTypes[] = {
        "gregorian",
        //"iso8601",
        "buddhist",
        "chinese",
        "coptic",
        "dangi",
        "ethiopic",
        "ethiopic-amete-alem",
        "hebrew",
        "indian",
        "islamic",
        "islamic-civil",
        "islamic-rgsa",
        "islamic-tbla",
        "islamic-umalqura",
        "japanese",
        "persian",
        "roc",
        //"unknown",
        nullptr
    };

    for (int32_t i = 0; calTypes[i] != nullptr; i++) {
        UErrorCode status = U_ZERO_ERROR;
        const char *calId = calTypes[i];

        LocalPointer<EraRules> rules1(EraRules::createInstance(calId, false, status));
        if (U_FAILURE(status)) {
            errln(UnicodeString("Era rules for ") + calId + " is not available.");
            continue;
        }

        LocalPointer<EraRules> rules2(EraRules::createInstance(calId, true, status));
        if (U_FAILURE(status)) {
            errln(UnicodeString("Era rules for ") + calId + " (including tentative eras) is not available.");
            continue;
        }

        int32_t numEras1 = rules1->getNumberOfEras();
        if (numEras1 <= 0) {
            errln(UnicodeString("Number of era rules for ") + calId + " is " + numEras1);
        }

        int32_t numEras2 = rules2->getNumberOfEras();
        if (numEras2 < numEras1) {
            errln(UnicodeString("Number of era including tentative eras is fewer than one without tentative eras in calendar: ")
                    + calId);
        }

        LocalPointer<Calendar> cal(Calendar::createInstance(*TimeZone::getGMT(), "en", status));
        if (U_FAILURE(status)) {
            errln("Failed to create a Calendar instance.");
            continue;
        }
        int32_t currentIdx = rules1->getCurrentEraCode();
        int32_t currentYear = cal->get(UCAL_YEAR, status);
        int32_t idx = rules1->getEraCode(
                currentYear, cal->get(UCAL_MONTH, status) + 1,
                cal->get(UCAL_DATE, status), status);
        if (U_FAILURE(status)) {
            errln("Error while getting index of era.");
            continue;
        }
        if (idx != currentIdx) {
            errln(UnicodeString("Current era index:") + currentIdx + " is different from era index of now:" + idx
                    + " in calendar:" + calId);
        }

        int32_t eraStartYear = rules1->getStartYear(currentIdx, status);
        if (U_FAILURE(status)) {
            errln(UnicodeString("Failed to get the start year of era index: ") + currentIdx + " in calendar: " + calId);
        }
        if (currentYear < eraStartYear) {
            errln(UnicodeString("Current era's start year is after the current year in calendar:") + calId);
        }
    }
}

void EraRulesTest::testJapanese() {
    // ICU4C does not define constants for eras
    const int32_t MEIJI = 232;
    const int32_t HEISEI = 235;

    UErrorCode status = U_ZERO_ERROR;
    LocalPointer<EraRules> rules(EraRules::createInstance("japanese", true, status));
    if (U_FAILURE(status)) {
        errln("Failed to get era rules for Japanese calendar.");
        return;
    }
    // Rules should have an era after Heisei
    int32_t maxEra = rules->getMaxEraCode();
    if (maxEra <= HEISEI) {
        errln("Era after Heisei is not available.");
        return;
    }
    int postHeiseiStartYear = rules->getStartYear(HEISEI + 1, status);
    if (U_FAILURE(status)) {
        errln("Failed to get the start year of era after Heisei.");
    }
    if (postHeiseiStartYear != 2019) {
        errln(UnicodeString("Era after Heisei should start in 2019, but got ") + postHeiseiStartYear);
    }

    // Note: Current CLDR data uses 1868-10-23 as the start date of Meiji.
    // This is not really accurate, because it does not count Lunar-solar
    // calendar system used before Meiji 6. This might be changed in future.
    assertEquals("1868-10-23", MEIJI, rules->getEraCode(1868, 10, 23, status));
    assertEquals("1868-10-22", GregorianCalendar::AD, rules->getEraCode(1868, 10, 22, status));
    assertEquals("0001-01-01", GregorianCalendar::AD, rules->getEraCode(1, 1, 1,status));
    assertEquals("0000-12-31", GregorianCalendar::BC, rules->getEraCode(0, 12, 31, status));
    assertEquals("-1000-01-01", GregorianCalendar::BC, rules->getEraCode(-1000, 1, 1, status));

    const int32_t expectedEraCodes[] = {0, 1, 232, 233, 234, 235, 236};
    int32_t era = rules->getMinEraCode();
    assertEquals("Minimum era", expectedEraCodes[0], era);
    for (int32_t i = 1; i < UPRV_LENGTHOF(expectedEraCodes); i++) {
        era = rules->getNextEraCode(era);
        assertEquals(UnicodeString("Era index ") + i, expectedEraCodes[i], era);
    }
    // getNextEraCode returns maximum era when the input era is already
    // maximum era.
    era = rules->getNextEraCode(era);
    assertEquals("Max era", maxEra, era);

    // getNextEracode returns next available era code even the input
    // era data does not exist.
    era = rules->getNextEraCode(100);
    assertEquals("getNextEraCode(100)", 232, era);

    // getNextEracode returns the maximum era code if the input
    // era code is greater than the maximum.
    era = rules->getNextEraCode(maxEra + 1);
    assertEquals("getNextEraCode(maxEra+1)", maxEra, era);

    // getNextEracode returns the minimum era code if the input
    // era code is less than the minimum.
    int32_t minEra = rules->getMinEraCode();
    era = rules->getNextEraCode(minEra - 1);
    assertEquals("getNextEraCode(minEra-1)", minEra, era);

        int numEras = rules->getNumberOfEras();
    assertEquals("Numbers of Eras", UPRV_LENGTHOF(expectedEraCodes), numEras);
}

#endif /* #if !UCONFIG_NO_FORMATTING */

