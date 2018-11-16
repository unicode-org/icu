// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
* Copyright (C) 2007-2015, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "tzfmttst.h"

#include "unicode/timezone.h"
#include "unicode/simpletz.h"
#include "unicode/calendar.h"
#include "unicode/strenum.h"
#include "unicode/smpdtfmt.h"
#include "unicode/uchar.h"
#include "unicode/basictz.h"
#include "unicode/tzfmt.h"
#include "unicode/localpointer.h"
#include "unicode/utf16.h"

#include "cstring.h"
#include "cstr.h"
#include "mutex.h"
#include "simplethread.h"
#include "uassert.h"
#include "zonemeta.h"

static const char* PATTERNS[] = {
    "z",
    "zzzz",
    "Z",    // equivalent to "xxxx"
    "ZZZZ", // equivalent to "OOOO"
    "v",
    "vvvv",
    "O",
    "OOOO",
    "X",
    "XX",
    "XXX",
    "XXXX",
    "XXXXX",
    "x",
    "xx",
    "xxx",
    "xxxx",
    "xxxxx",
    "V",
    "VV",
    "VVV",
    "VVVV"
};

static const UChar ETC_UNKNOWN[] = {0x45, 0x74, 0x63, 0x2F, 0x55, 0x6E, 0x6B, 0x6E, 0x6F, 0x77, 0x6E, 0};

static const UChar ETC_SLASH[] = { 0x45, 0x74, 0x63, 0x2F, 0 }; // "Etc/"
static const UChar SYSTEMV_SLASH[] = { 0x53, 0x79, 0x73, 0x74, 0x65, 0x6D, 0x56, 0x2F, 0 }; // "SystemV/
static const UChar RIYADH8[] = { 0x52, 0x69, 0x79, 0x61, 0x64, 0x68, 0x38, 0 }; // "Riyadh8"

static UBool contains(const char** list, const char* str) {
    for (int32_t i = 0; list[i]; i++) {
        if (uprv_strcmp(list[i], str) == 0) {
            return TRUE;
        }
    }
    return FALSE;
}

void
TimeZoneFormatTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) {
        logln("TestSuite TimeZoneFormatTest");
    }
    switch (index) {
        TESTCASE(0, TestTimeZoneRoundTrip);
        TESTCASE(1, TestTimeRoundTrip);
        TESTCASE(2, TestParse);
        TESTCASE(3, TestISOFormat);
        TESTCASE(4, TestFormat);
        TESTCASE(5, TestFormatTZDBNames);
        TESTCASE(6, TestFormatCustomZone);
        TESTCASE(7, TestFormatTZDBNamesAllZoneCoverage);
        TESTCASE(8, TestLocaleTZExtension);
    default: name = ""; break;
    }
}

void
TimeZoneFormatTest::TestTimeZoneRoundTrip(void) {
    UErrorCode status = U_ZERO_ERROR;

    SimpleTimeZone unknownZone(-31415, ETC_UNKNOWN);
    int32_t badDstOffset = -1234;
    int32_t badZoneOffset = -2345;

    int32_t testDateData[][3] = {
        {2007, 1, 15},
        {2007, 6, 15},
        {1990, 1, 15},
        {1990, 6, 15},
        {1960, 1, 15},
        {1960, 6, 15},
    };

    Calendar *cal = Calendar::createInstance(TimeZone::createTimeZone((UnicodeString)"UTC"), status);
    if (U_FAILURE(status)) {
        dataerrln("Calendar::createInstance failed: %s", u_errorName(status));
        return;
    }

    // Set up rule equivalency test range
    UDate low, high;
    cal->set(1900, UCAL_JANUARY, 1);
    low = cal->getTime(status);
    cal->set(2040, UCAL_JANUARY, 1);
    high = cal->getTime(status);
    if (U_FAILURE(status)) {
        errln("getTime failed");
        return;
    }

    // Set up test dates
    UDate DATES[UPRV_LENGTHOF(testDateData)];
    const int32_t nDates = UPRV_LENGTHOF(testDateData);
    cal->clear();
    for (int32_t i = 0; i < nDates; i++) {
        cal->set(testDateData[i][0], testDateData[i][1], testDateData[i][2]);
        DATES[i] = cal->getTime(status);
        if (U_FAILURE(status)) {
            errln("getTime failed");
            return;
        }
    }

    // Set up test locales
    const Locale testLocales[] = {
        Locale("en"),
        Locale("en_CA"),
        Locale("fr"),
        Locale("zh_Hant"),
        Locale("fa"),
        Locale("ccp")
    };

    const Locale *LOCALES;
    int32_t nLocales;

    if (quick) {
        LOCALES = testLocales;
        nLocales = UPRV_LENGTHOF(testLocales);
    } else {
        LOCALES = Locale::getAvailableLocales(nLocales);
    }

    StringEnumeration *tzids = TimeZone::createEnumeration();
    int32_t inRaw, inDst;
    int32_t outRaw, outDst;

    // Run the roundtrip test
    for (int32_t locidx = 0; locidx < nLocales; locidx++) {
        UnicodeString localGMTString;
        SimpleDateFormat gmtFmt(UnicodeString("ZZZZ"), LOCALES[locidx], status);
        if (U_FAILURE(status)) {
            dataerrln("Error creating SimpleDateFormat - %s", u_errorName(status));
            continue;
        }
        gmtFmt.setTimeZone(*TimeZone::getGMT());
        gmtFmt.format(0.0, localGMTString);

        for (int32_t patidx = 0; patidx < UPRV_LENGTHOF(PATTERNS); patidx++) {
            SimpleDateFormat *sdf = new SimpleDateFormat((UnicodeString)PATTERNS[patidx], LOCALES[locidx], status);
            if (U_FAILURE(status)) {
                dataerrln((UnicodeString)"new SimpleDateFormat failed for pattern " +
                    PATTERNS[patidx] + " for locale " + LOCALES[locidx].getName() + " - " + u_errorName(status));
                status = U_ZERO_ERROR;
                continue;
            }

            tzids->reset(status);
            const UnicodeString *tzid;
            while ((tzid = tzids->snext(status))) {
                TimeZone *tz = TimeZone::createTimeZone(*tzid);

                for (int32_t datidx = 0; datidx < nDates; datidx++) {
                    UnicodeString tzstr;
                    FieldPosition fpos(FieldPosition::DONT_CARE);
                    // Format
                    sdf->setTimeZone(*tz);
                    sdf->format(DATES[datidx], tzstr, fpos);

                    // Before parse, set unknown zone to SimpleDateFormat instance
                    // just for making sure that it does not depends on the time zone
                    // originally set.
                    sdf->setTimeZone(unknownZone);

                    // Parse
                    ParsePosition pos(0);
                    Calendar *outcal = Calendar::createInstance(unknownZone, status);
                    if (U_FAILURE(status)) {
                        errln("Failed to create an instance of calendar for receiving parse result.");
                        status = U_ZERO_ERROR;
                        continue;
                    }
                    outcal->set(UCAL_DST_OFFSET, badDstOffset);
                    outcal->set(UCAL_ZONE_OFFSET, badZoneOffset);

                    sdf->parse(tzstr, *outcal, pos);

                    // Check the result
                    const TimeZone &outtz = outcal->getTimeZone();
                    UnicodeString outtzid;
                    outtz.getID(outtzid);

                    tz->getOffset(DATES[datidx], false, inRaw, inDst, status);
                    if (U_FAILURE(status)) {
                        errln((UnicodeString)"Failed to get offsets from time zone" + *tzid);
                        status = U_ZERO_ERROR;
                    }
                    outtz.getOffset(DATES[datidx], false, outRaw, outDst, status);
                    if (U_FAILURE(status)) {
                        errln((UnicodeString)"Failed to get offsets from time zone" + outtzid);
                        status = U_ZERO_ERROR;
                    }

                    if (uprv_strcmp(PATTERNS[patidx], "V") == 0) {
                        // Short zone ID - should support roundtrip for canonical CLDR IDs
                        UnicodeString canonicalID;
                        TimeZone::getCanonicalID(*tzid, canonicalID, status);
                        if (U_FAILURE(status)) {
                            // Uknown ID - we should not get here
                            errln((UnicodeString)"Unknown ID " + *tzid);
                            status = U_ZERO_ERROR;
                        } else if (outtzid != canonicalID) {
                            if (outtzid.compare(ETC_UNKNOWN, -1) == 0) {
                                // Note that some zones like Asia/Riyadh87 does not have
                                // short zone ID and "unk" is used as fallback
                                logln((UnicodeString)"Canonical round trip failed (probably as expected); tz=" + *tzid
                                        + ", locale=" + LOCALES[locidx].getName() + ", pattern=" + PATTERNS[patidx]
                                        + ", time=" + DATES[datidx] + ", str=" + tzstr
                                        + ", outtz=" + outtzid);
                            } else {
                                errln((UnicodeString)"Canonical round trip failed; tz=" + *tzid
                                    + ", locale=" + LOCALES[locidx].getName() + ", pattern=" + PATTERNS[patidx]
                                    + ", time=" + DATES[datidx] + ", str=" + tzstr
                                    + ", outtz=" + outtzid);
                            }
                        }
                    } else if (uprv_strcmp(PATTERNS[patidx], "VV") == 0) {
                        // Zone ID - full roundtrip support
                        if (outtzid != *tzid) {
                            errln((UnicodeString)"Zone ID round trip failued; tz="  + *tzid
                                + ", locale=" + LOCALES[locidx].getName() + ", pattern=" + PATTERNS[patidx]
                                + ", time=" + DATES[datidx] + ", str=" + tzstr
                                + ", outtz=" + outtzid);
                        }
                    } else if (uprv_strcmp(PATTERNS[patidx], "VVV") == 0 || uprv_strcmp(PATTERNS[patidx], "VVVV") == 0) {
                        // Location: time zone rule must be preserved except
                        // zones not actually associated with a specific location.
                        // Time zones in this category do not have "/" in its ID.
                        UnicodeString canonical;
                        TimeZone::getCanonicalID(*tzid, canonical, status);
                        if (U_FAILURE(status)) {
                            // Uknown ID - we should not get here
                            errln((UnicodeString)"Unknown ID " + *tzid);
                            status = U_ZERO_ERROR;
                        } else if (outtzid != canonical) {
                            // Canonical ID did not match - check the rules
                            if (!((BasicTimeZone*)&outtz)->hasEquivalentTransitions((BasicTimeZone&)*tz, low, high, TRUE, status)) {
                                if (canonical.indexOf((UChar)0x27 /*'/'*/) == -1) {
                                    // Exceptional cases, such as CET, EET, MET and WET
                                    logln((UnicodeString)"Canonical round trip failed (as expected); tz=" + *tzid
                                            + ", locale=" + LOCALES[locidx].getName() + ", pattern=" + PATTERNS[patidx]
                                            + ", time=" + DATES[datidx] + ", str=" + tzstr
                                            + ", outtz=" + outtzid);
                                } else {
                                    errln((UnicodeString)"Canonical round trip failed; tz=" + *tzid
                                        + ", locale=" + LOCALES[locidx].getName() + ", pattern=" + PATTERNS[patidx]
                                        + ", time=" + DATES[datidx] + ", str=" + tzstr
                                        + ", outtz=" + outtzid);
                                }
                                if (U_FAILURE(status)) {
                                    errln("hasEquivalentTransitions failed");
                                    status = U_ZERO_ERROR;
                                }
                            }
                        }

                    } else {
                        UBool isOffsetFormat = (*PATTERNS[patidx] == 'Z'
                                                || *PATTERNS[patidx] == 'O'
                                                || *PATTERNS[patidx] == 'X'
                                                || *PATTERNS[patidx] == 'x');
                        UBool minutesOffset = FALSE;
                        if (*PATTERNS[patidx] == 'X' || *PATTERNS[patidx] == 'x') {
                            minutesOffset = (uprv_strlen(PATTERNS[patidx]) <= 3);
                        }

                        if (!isOffsetFormat) {
                            // Check if localized GMT format is used as a fallback of name styles
                            int32_t numDigits = 0;
                            int32_t idx = 0;
                            while (idx < tzstr.length()) {
                                UChar32 cp = tzstr.char32At(idx);
                                if (u_isdigit(cp)) {
                                    numDigits++;
                                }
                                idx += U16_LENGTH(cp);
                            }
                            isOffsetFormat = (numDigits > 0);
                        }
                        if (isOffsetFormat || tzstr == localGMTString) {
                            // Localized GMT or ISO: total offset (raw + dst) must be preserved.
                            int32_t inOffset = inRaw + inDst;
                            int32_t outOffset = outRaw + outDst;
                            int32_t diff = outOffset - inOffset;
                            if (minutesOffset) {
                                diff = (diff / 60000) * 60000;
                            }
                            if (diff != 0) {
                                errln((UnicodeString)"Offset round trip failed; tz=" + *tzid
                                    + ", locale=" + LOCALES[locidx].getName() + ", pattern=" + PATTERNS[patidx]
                                    + ", time=" + DATES[datidx] + ", str=" + tzstr
                                    + ", inOffset=" + inOffset + ", outOffset=" + outOffset);
                            }
                        } else {
                            // Specific or generic: raw offset must be preserved.
                            if (inRaw != outRaw) {
                                errln((UnicodeString)"Raw offset round trip failed; tz=" + *tzid
                                    + ", locale=" + LOCALES[locidx].getName() + ", pattern=" + PATTERNS[patidx]
                                    + ", time=" + DATES[datidx] + ", str=" + tzstr
                                    + ", inRawOffset=" + inRaw + ", outRawOffset=" + outRaw);
                            }
                        }
                    }
                    delete outcal;
                }
                delete tz;
            }
            delete sdf;
        }
    }
    delete cal;
    delete tzids;
}

// Special exclusions in TestTimeZoneRoundTrip.
// These special cases do not round trip time as designed.
static UBool isSpecialTimeRoundTripCase(const char* loc,
                                        const UnicodeString& id,
                                        const char* pattern,
                                        UDate time) {
    struct {
        const char* loc;
        const char* id;
        const char* pattern;
        UDate time;
    } EXCLUSIONS[] = {
        {NULL, "Asia/Chita", "zzzz", 1414252800000.0},
        {NULL, "Asia/Chita", "vvvv", 1414252800000.0},
        {NULL, "Asia/Srednekolymsk", "zzzz", 1414241999999.0},
        {NULL, "Asia/Srednekolymsk", "vvvv", 1414241999999.0},
        {NULL, NULL, NULL, U_DATE_MIN}
    };

    UBool isExcluded = FALSE;
    for (int32_t i = 0; EXCLUSIONS[i].id != NULL; i++) {
        if (EXCLUSIONS[i].loc == NULL || uprv_strcmp(loc, EXCLUSIONS[i].loc) == 0) {
            if (id.compare(EXCLUSIONS[i].id) == 0) {
                if (EXCLUSIONS[i].pattern == NULL || uprv_strcmp(pattern, EXCLUSIONS[i].pattern) == 0) {
                    if (EXCLUSIONS[i].time == U_DATE_MIN || EXCLUSIONS[i].time == time) {
                        isExcluded = TRUE;
                    }
                }
            }
        }
    }
    return isExcluded;
}

// LocaleData. Somewhat misnamed. For TestTimeZoneRoundTrip, specifies the locales and patterns
//             to be tested, and provides an iterator over these for the multi-threaded test
//             functions to pick up the next combination to be tested.
//
//             A single global instance of this struct is shared among all
//             the test threads.
//
//             "locales" is an array of locales to be tested.
//             PATTERNS (a global) is an array of patterns to be tested for each locale.
//             "localeIndex" and "patternIndex" keep track of the iteration through the above.
//             Each of the parallel test threads calls LocaleData::nextTest() in a loop
//                to find out what to test next. It must be thread safe.
struct LocaleData {
    int32_t localeIndex;
    int32_t patternIndex;
    int32_t testCounts;
    UDate times[UPRV_LENGTHOF(PATTERNS)];    // Performance data, Elapsed time for each pattern.
    const Locale* locales;
    int32_t nLocales;
    UDate START_TIME;
    UDate END_TIME;
    int32_t numDone;

    LocaleData() : localeIndex(0), patternIndex(0), testCounts(0), locales(NULL),
                   nLocales(0), START_TIME(0), END_TIME(0), numDone(0) {
        for (int i=0; i<UPRV_LENGTHOF(times); i++) {
            times[i] = 0;
        }
    };

    void resetTestIteration() {
        localeIndex = -1;
        patternIndex = UPRV_LENGTHOF(PATTERNS);
        numDone = 0;
    }

    UBool nextTest(int32_t &rLocaleIndex, int32_t &rPatternIndex) {
        Mutex lock;
        if (patternIndex >= UPRV_LENGTHOF(PATTERNS) - 1) {
            if (localeIndex >= nLocales - 1) {
                return FALSE;
            }
            patternIndex = -1;
            ++localeIndex;
        }
        ++patternIndex;
        rLocaleIndex = localeIndex;
        rPatternIndex = patternIndex;
        ++numDone;
        return TRUE;
    }

    void addTime(UDate amount, int32_t patIdx) {
        Mutex lock;
        U_ASSERT(patIdx < UPRV_LENGTHOF(PATTERNS));
        times[patIdx] += amount;
    }
};

static LocaleData *gLocaleData = NULL;

void
TimeZoneFormatTest::TestTimeRoundTrip(void) {
    UErrorCode status = U_ZERO_ERROR;
    LocalPointer <Calendar> cal(Calendar::createInstance(TimeZone::createTimeZone((UnicodeString) "UTC"), status));
    if (U_FAILURE(status)) {
        dataerrln("Calendar::createInstance failed: %s", u_errorName(status));
        return;
    }

    const char* testAllProp = getProperty("TimeZoneRoundTripAll");
    UBool bTestAll = (testAllProp && uprv_strcmp(testAllProp, "true") == 0);

    UDate START_TIME, END_TIME;
    if (bTestAll || !quick) {
        cal->set(1900, UCAL_JANUARY, 1);
    } else {
        cal->set(1999, UCAL_JANUARY, 1);
    }
    START_TIME = cal->getTime(status);

    cal->set(2022, UCAL_JANUARY, 1);
    END_TIME = cal->getTime(status);

    if (U_FAILURE(status)) {
        errln("getTime failed");
        return;
    }

    LocaleData localeData;
    gLocaleData = &localeData;

    // Set up test locales
    const Locale locales1[] = {Locale("en")};
    const Locale locales2[] = {
        Locale("ar_EG"), Locale("bg_BG"), Locale("ca_ES"), Locale("da_DK"), Locale("de"),
        Locale("de_DE"), Locale("el_GR"), Locale("en"), Locale("en_AU"), Locale("en_CA"),
        Locale("en_US"), Locale("es"), Locale("es_ES"), Locale("es_MX"), Locale("fi_FI"),
        Locale("fr"), Locale("fr_CA"), Locale("fr_FR"), Locale("he_IL"), Locale("hu_HU"),
        Locale("it"), Locale("it_IT"), Locale("ja"), Locale("ja_JP"), Locale("ko"),
        Locale("ko_KR"), Locale("nb_NO"), Locale("nl_NL"), Locale("nn_NO"), Locale("pl_PL"),
        Locale("pt"), Locale("pt_BR"), Locale("pt_PT"), Locale("ru_RU"), Locale("sv_SE"),
        Locale("th_TH"), Locale("tr_TR"), Locale("zh"), Locale("zh_Hans"), Locale("zh_Hans_CN"),
        Locale("zh_Hant"), Locale("zh_Hant_TW"), Locale("fa"), Locale("ccp")
    };

    if (bTestAll) {
        gLocaleData->locales = Locale::getAvailableLocales(gLocaleData->nLocales);
    } else if (quick) {
        gLocaleData->locales = locales1;
        gLocaleData->nLocales = UPRV_LENGTHOF(locales1);
    } else {
        gLocaleData->locales = locales2;
        gLocaleData->nLocales = UPRV_LENGTHOF(locales2);
    }

    gLocaleData->START_TIME = START_TIME;
    gLocaleData->END_TIME = END_TIME;
    gLocaleData->resetTestIteration();

    // start IntlTest.threadCount threads, each running the function RunTimeRoundTripTests().

    ThreadPool<TimeZoneFormatTest> threads(this, threadCount, &TimeZoneFormatTest::RunTimeRoundTripTests);
    threads.start();   // Start all threads.
    threads.join();    // Wait for all threads to finish.

    UDate total = 0;
    logln("### Elapsed time by patterns ###");
    for (int32_t i = 0; i < UPRV_LENGTHOF(PATTERNS); i++) {
        logln(UnicodeString("") + gLocaleData->times[i] + "ms (" + PATTERNS[i] + ")");
        total += gLocaleData->times[i];
    }
    logln((UnicodeString) "Total: " + total + "ms");
    logln((UnicodeString) "Iteration: " + gLocaleData->testCounts);
}


// TimeZoneFormatTest::RunTimeRoundTripTests()
//    This function loops, running time zone format round trip test cases until there are no more, then returns.
//    Threading: multiple invocations of this function are started in parallel 
//               by TimeZoneFormatTest::TestTimeRoundTrip()
//    
void TimeZoneFormatTest::RunTimeRoundTripTests(int32_t threadNumber) {
    UErrorCode status = U_ZERO_ERROR;
    UBool REALLY_VERBOSE = FALSE;

    // These patterns are ambiguous at DST->STD local time overlap
    const char* AMBIGUOUS_DST_DECESSION[] = { "v", "vvvv", "V", "VV", "VVV", "VVVV", 0 };

    // These patterns are ambiguous at STD->STD/DST->DST local time overlap
    const char* AMBIGUOUS_NEGATIVE_SHIFT[] = { "z", "zzzz", "v", "vvvv", "V", "VV", "VVV", "VVVV", 0 };

    // These patterns only support integer minutes offset
    const char* MINUTES_OFFSET[] = { "X", "XX", "XXX", "x", "xx", "xxx", 0 };

    // Workaround for #6338
    //UnicodeString BASEPATTERN("yyyy-MM-dd'T'HH:mm:ss.SSS");
    UnicodeString BASEPATTERN("yyyy.MM.dd HH:mm:ss.SSS");

    // timer for performance analysis
    UDate timer;
    UDate testTimes[4];
    UBool expectedRoundTrip[4];
    int32_t testLen = 0;

    StringEnumeration *tzids = TimeZone::createTimeZoneIDEnumeration(UCAL_ZONE_TYPE_CANONICAL, NULL, NULL, status);
    if (U_FAILURE(status)) {
        if (status == U_MISSING_RESOURCE_ERROR) {
            // This error is generally caused by data not being present.
            dataerrln("TimeZone::createTimeZoneIDEnumeration failed - %s", u_errorName(status));
        } else {
            errln("TimeZone::createTimeZoneIDEnumeration failed: %s", u_errorName(status));
        }
        return;
    }

    int32_t locidx = -1;
    int32_t patidx = -1;

    while (gLocaleData->nextTest(locidx, patidx)) {

        UnicodeString pattern(BASEPATTERN);
        pattern.append(" ").append(PATTERNS[patidx]);
        logln("    Thread %d, Locale %s, Pattern %s", 
                threadNumber, gLocaleData->locales[locidx].getName(), CStr(pattern)());

        SimpleDateFormat *sdf = new SimpleDateFormat(pattern, gLocaleData->locales[locidx], status);
        if (U_FAILURE(status)) {
            errcheckln(status, (UnicodeString) "new SimpleDateFormat failed for pattern " + 
                pattern + " for locale " + gLocaleData->locales[locidx].getName() + " - " + u_errorName(status));
            status = U_ZERO_ERROR;
            continue;
        }

        UBool minutesOffset = contains(MINUTES_OFFSET, PATTERNS[patidx]);

        tzids->reset(status);
        const UnicodeString *tzid;

        timer = Calendar::getNow();

        while ((tzid = tzids->snext(status))) {
            if (uprv_strcmp(PATTERNS[patidx], "V") == 0) {
                // Some zones do not have short ID assigned, such as Asia/Riyadh87.
                // The time roundtrip will fail for such zones with pattern "V" (short zone ID).
                // This is expected behavior.
                const UChar* shortZoneID = ZoneMeta::getShortID(*tzid);
                if (shortZoneID == NULL) {
                    continue;
                }
            } else if (uprv_strcmp(PATTERNS[patidx], "VVV") == 0) {
                // Some zones are not associated with any region, such as Etc/GMT+8.
                // The time roundtrip will fail for such zone with pattern "VVV" (exemplar location).
                // This is expected behavior.
                if (tzid->indexOf((UChar)0x2F) < 0 || tzid->indexOf(ETC_SLASH, -1, 0) >= 0
                    || tzid->indexOf(SYSTEMV_SLASH, -1, 0) >= 0 || tzid->indexOf(RIYADH8, -1, 0) >= 0) {
                    continue;
                }
            }

            if ((*tzid == "Pacific/Apia" || *tzid == "Pacific/Midway" || *tzid == "Pacific/Pago_Pago")
                    && uprv_strcmp(PATTERNS[patidx], "vvvv") == 0
                    && logKnownIssue("11052", "Ambiguous zone name - Samoa Time")) {
                continue;
            }

            BasicTimeZone *tz = (BasicTimeZone*) TimeZone::createTimeZone(*tzid);
            sdf->setTimeZone(*tz);

            UDate t = gLocaleData->START_TIME;
            TimeZoneTransition tzt;
            UBool tztAvail = FALSE;
            UBool middle = TRUE;

            while (t < gLocaleData->END_TIME) {
                if (!tztAvail) {
                    testTimes[0] = t;
                    expectedRoundTrip[0] = TRUE;
                    testLen = 1;
                } else {
                    int32_t fromOffset = tzt.getFrom()->getRawOffset() + tzt.getFrom()->getDSTSavings();
                    int32_t toOffset = tzt.getTo()->getRawOffset() + tzt.getTo()->getDSTSavings();
                    int32_t delta = toOffset - fromOffset;
                    if (delta < 0) {
                        UBool isDstDecession = tzt.getFrom()->getDSTSavings() > 0 && tzt.getTo()->getDSTSavings() == 0;
                        testTimes[0] = t + delta - 1;
                        expectedRoundTrip[0] = TRUE;
                        testTimes[1] = t + delta;
                        expectedRoundTrip[1] = isDstDecession ?
                            !contains(AMBIGUOUS_DST_DECESSION, PATTERNS[patidx]) :
                            !contains(AMBIGUOUS_NEGATIVE_SHIFT, PATTERNS[patidx]);
                        testTimes[2] = t - 1;
                        expectedRoundTrip[2] = isDstDecession ?
                            !contains(AMBIGUOUS_DST_DECESSION, PATTERNS[patidx]) :
                            !contains(AMBIGUOUS_NEGATIVE_SHIFT, PATTERNS[patidx]);
                        testTimes[3] = t;
                        expectedRoundTrip[3] = TRUE;
                        testLen = 4;
                    } else {
                        testTimes[0] = t - 1;
                        expectedRoundTrip[0] = TRUE;
                        testTimes[1] = t;
                        expectedRoundTrip[1] = TRUE;
                        testLen = 2;
                    }
                }
                for (int32_t testidx = 0; testidx < testLen; testidx++) {
                    if (quick) {
                        // reduce regular test time
                        if (!expectedRoundTrip[testidx]) {
                            continue;
                        }
                    }

                    {
                        Mutex lock;
                        gLocaleData->testCounts++;
                    }

                    UnicodeString text;
                    FieldPosition fpos(FieldPosition::DONT_CARE);
                    sdf->format(testTimes[testidx], text, fpos);

                    UDate parsedDate = sdf->parse(text, status);
                    if (U_FAILURE(status)) {
                        errln((UnicodeString) "Parse failure for text=" + text + ", tzid=" + *tzid + ", locale=" + gLocaleData->locales[locidx].getName()
                                + ", pattern=" + PATTERNS[patidx] + ", time=" + testTimes[testidx]);
                        status = U_ZERO_ERROR;
                        continue;
                    }

                    int32_t timeDiff = (int32_t)(parsedDate - testTimes[testidx]);
                    UBool bTimeMatch = minutesOffset ?
                        (timeDiff/60000)*60000 == 0 : timeDiff == 0;
                    if (!bTimeMatch) {
                        UnicodeString msg = (UnicodeString) "Time round trip failed for " + "tzid=" + *tzid
                                + ", locale=" + gLocaleData->locales[locidx].getName() + ", pattern=" + PATTERNS[patidx]
                                + ", text=" + text + ", time=" + testTimes[testidx] + ", restime=" + parsedDate + ", diff=" + (parsedDate - testTimes[testidx]);
                        // Timebomb for TZData update
                        if (expectedRoundTrip[testidx]
                                && !isSpecialTimeRoundTripCase(gLocaleData->locales[locidx].getName(), *tzid,
                                        PATTERNS[patidx], testTimes[testidx])) {
                            errln((UnicodeString) "FAIL: " + msg);
                        } else if (REALLY_VERBOSE) {
                            logln(msg);
                        }
                    }
                }
                tztAvail = tz->getNextTransition(t, FALSE, tzt);
                if (!tztAvail) {
                    break;
                }
                if (middle) {
                    // Test the date in the middle of two transitions.
                    t += (int64_t) ((tzt.getTime() - t) / 2);
                    middle = FALSE;
                    tztAvail = FALSE;
                } else {
                    t = tzt.getTime();
                }
            }
            delete tz;
        }
        UDate elapsedTime = Calendar::getNow() - timer;
        gLocaleData->addTime(elapsedTime, patidx);
        delete sdf;
    }
    delete tzids;
}


typedef struct {
    const char*     text;
    int32_t         inPos;
    const char*     locale;
    UTimeZoneFormatStyle    style;
    uint32_t        parseOptions;
    const char*     expected;
    int32_t         outPos;
    UTimeZoneFormatTimeType timeType;
} ParseTestData;

void
TimeZoneFormatTest::TestParse(void) {
    const ParseTestData DATA[] = {
        //   text               inPos   locale      style
        //      parseOptions                        expected            outPos  timeType
            {"Z",               0,      "en_US",    UTZFMT_STYLE_ISO_EXTENDED_FULL,
                UTZFMT_PARSE_OPTION_NONE,           "Etc/GMT",          1,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"Z",               0,      "en_US",    UTZFMT_STYLE_SPECIFIC_LONG,
                UTZFMT_PARSE_OPTION_NONE,           "Etc/GMT",          1,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"Zambia time",     0,      "en_US",    UTZFMT_STYLE_ISO_EXTENDED_FULL,
                UTZFMT_PARSE_OPTION_ALL_STYLES,     "Etc/GMT",          1,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"Zambia time",     0,      "en_US",    UTZFMT_STYLE_GENERIC_LOCATION,
                UTZFMT_PARSE_OPTION_NONE,           "Africa/Lusaka",    11,     UTZFMT_TIME_TYPE_UNKNOWN},

            {"Zambia time",     0,      "en_US",    UTZFMT_STYLE_ISO_BASIC_LOCAL_FULL,
                UTZFMT_PARSE_OPTION_ALL_STYLES,     "Africa/Lusaka",    11,     UTZFMT_TIME_TYPE_UNKNOWN},

            {"+00:00",          0,      "en_US",    UTZFMT_STYLE_ISO_EXTENDED_FULL,
                UTZFMT_PARSE_OPTION_NONE,           "Etc/GMT",          6,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"-01:30:45",       0,      "en_US",    UTZFMT_STYLE_ISO_EXTENDED_FULL,
                UTZFMT_PARSE_OPTION_NONE,           "GMT-01:30:45",     9,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"-7",              0,      "en_US",    UTZFMT_STYLE_ISO_BASIC_LOCAL_FULL,
                UTZFMT_PARSE_OPTION_NONE,           "GMT-07:00",        2,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"-2222",           0,      "en_US",    UTZFMT_STYLE_ISO_BASIC_LOCAL_FULL,
                UTZFMT_PARSE_OPTION_NONE,           "GMT-22:22",        5,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"-3333",           0,      "en_US",    UTZFMT_STYLE_ISO_BASIC_LOCAL_FULL,
                UTZFMT_PARSE_OPTION_NONE,           "GMT-03:33",        4,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"XXX+01:30YYY",    3,      "en_US",    UTZFMT_STYLE_LOCALIZED_GMT,
                UTZFMT_PARSE_OPTION_NONE,           "GMT+01:30",        9,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"GMT0",            0,      "en_US",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_NONE,           "Etc/GMT",          3,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"EST",             0,      "en_US",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_NONE,           "America/New_York", 3,      UTZFMT_TIME_TYPE_STANDARD},

            {"ESTx",            0,      "en_US",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_NONE,           "America/New_York", 3,      UTZFMT_TIME_TYPE_STANDARD},

            {"EDTx",            0,      "en_US",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_NONE,           "America/New_York", 3,      UTZFMT_TIME_TYPE_DAYLIGHT},

            {"EST",             0,      "en_US",    UTZFMT_STYLE_SPECIFIC_LONG,
                UTZFMT_PARSE_OPTION_NONE,           NULL,               0,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"EST",             0,      "en_US",    UTZFMT_STYLE_SPECIFIC_LONG,
                UTZFMT_PARSE_OPTION_ALL_STYLES,     "America/New_York", 3,      UTZFMT_TIME_TYPE_STANDARD},

            {"EST",             0,      "en_CA",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_NONE,           "America/Toronto",  3,      UTZFMT_TIME_TYPE_STANDARD},

            {"CST",             0,      "en_US",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_NONE,           "America/Chicago",  3,      UTZFMT_TIME_TYPE_STANDARD},

            {"CST",             0,      "en_GB",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_NONE,           NULL,               0,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"CST",             0,      "en_GB",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_TZ_DATABASE_ABBREVIATIONS,  "America/Chicago",  3,  UTZFMT_TIME_TYPE_STANDARD},

            {"--CST--",           2,    "en_GB",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_TZ_DATABASE_ABBREVIATIONS,  "America/Chicago",  5,  UTZFMT_TIME_TYPE_STANDARD},

            {"CST",             0,      "zh_CN",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_TZ_DATABASE_ABBREVIATIONS,  "Asia/Shanghai",    3,  UTZFMT_TIME_TYPE_STANDARD},

            {"AEST",            0,      "en_AU",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_TZ_DATABASE_ABBREVIATIONS,  "Australia/Sydney", 4,  UTZFMT_TIME_TYPE_STANDARD},

            {"AST",             0,      "ar_SA",    UTZFMT_STYLE_SPECIFIC_SHORT,
                UTZFMT_PARSE_OPTION_TZ_DATABASE_ABBREVIATIONS,  "Asia/Riyadh",      3,  UTZFMT_TIME_TYPE_STANDARD},

            {"AQTST",           0,      "en",       UTZFMT_STYLE_SPECIFIC_LONG,
                UTZFMT_PARSE_OPTION_NONE,           NULL,               0,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"AQTST",           0,      "en",       UTZFMT_STYLE_SPECIFIC_LONG,
                UTZFMT_PARSE_OPTION_ALL_STYLES,     NULL,               0,      UTZFMT_TIME_TYPE_UNKNOWN},

            {"AQTST",           0,      "en",       UTZFMT_STYLE_SPECIFIC_LONG,
                UTZFMT_PARSE_OPTION_ALL_STYLES | UTZFMT_PARSE_OPTION_TZ_DATABASE_ABBREVIATIONS, "Asia/Aqtobe",  5,  UTZFMT_TIME_TYPE_DAYLIGHT},

            {NULL,              0,      NULL,       UTZFMT_STYLE_GENERIC_LOCATION,
                UTZFMT_PARSE_OPTION_NONE,           NULL,               0,      UTZFMT_TIME_TYPE_UNKNOWN}
    };

    for (int32_t i = 0; DATA[i].text; i++) {
        UErrorCode status = U_ZERO_ERROR;
        LocalPointer<TimeZoneFormat> tzfmt(TimeZoneFormat::createInstance(Locale(DATA[i].locale), status));
        if (U_FAILURE(status)) {
            dataerrln("Fail TimeZoneFormat::createInstance: %s", u_errorName(status));
            continue;
        }
        UTimeZoneFormatTimeType ttype = UTZFMT_TIME_TYPE_UNKNOWN;
        ParsePosition pos(DATA[i].inPos);
        TimeZone* tz = tzfmt->parse(DATA[i].style, DATA[i].text, pos, DATA[i].parseOptions, &ttype);

        UnicodeString errMsg;
        if (tz) {
            UnicodeString outID;
            tz->getID(outID);
            if (outID != UnicodeString(DATA[i].expected)) {
                errMsg = (UnicodeString)"Time zone ID: " + outID + " - expected: " + DATA[i].expected;
            } else if (pos.getIndex() != DATA[i].outPos) {
                errMsg = (UnicodeString)"Parsed pos: " + pos.getIndex() + " - expected: " + DATA[i].outPos;
            } else if (ttype != DATA[i].timeType) {
                errMsg = (UnicodeString)"Time type: " + ttype + " - expected: " + DATA[i].timeType;
            }
            delete tz;
        } else {
            if (DATA[i].expected) {
                errMsg = (UnicodeString)"Parse failure - expected: " + DATA[i].expected;
            }
        }
        if (errMsg.length() > 0) {
            errln((UnicodeString)"Fail: " + errMsg + " [text=" + DATA[i].text + ", pos=" + DATA[i].inPos + ", style=" + DATA[i].style + "]");
        }
    }
}

void
TimeZoneFormatTest::TestISOFormat(void) {
    const int32_t OFFSET[] = {
        0,          // 0
        999,        // 0.999s
        -59999,     // -59.999s
        60000,      // 1m
        -77777,     // -1m 17.777s
        1800000,    // 30m
        -3600000,   // -1h
        36000000,   // 10h
        -37800000,  // -10h 30m
        -37845000,  // -10h 30m 45s
        108000000,  // 30h
    };

    const char* ISO_STR[][11] = {
        // 0
        {
            "Z", "Z", "Z", "Z", "Z",
            "+00", "+0000", "+00:00", "+0000", "+00:00",
            "+0000"
        },
        // 999
        {
            "Z", "Z", "Z", "Z", "Z",
            "+00", "+0000", "+00:00", "+0000", "+00:00",
            "+0000"
        },
        // -59999
        {
            "Z", "Z", "Z", "-000059", "-00:00:59",
            "+00", "+0000", "+00:00", "-000059", "-00:00:59",
            "-000059"
        },
        // 60000
        {
            "+0001", "+0001", "+00:01", "+0001", "+00:01",
            "+0001", "+0001", "+00:01", "+0001", "+00:01",
            "+0001"
        },
        // -77777
        {
            "-0001", "-0001", "-00:01", "-000117", "-00:01:17",
            "-0001", "-0001", "-00:01", "-000117", "-00:01:17",
            "-000117"
        },
        // 1800000
        {
            "+0030", "+0030", "+00:30", "+0030", "+00:30",
            "+0030", "+0030", "+00:30", "+0030", "+00:30",
            "+0030"
        },
        // -3600000
        {
            "-01", "-0100", "-01:00", "-0100", "-01:00",
            "-01", "-0100", "-01:00", "-0100", "-01:00",
            "-0100"
        },
        // 36000000
        {
            "+10", "+1000", "+10:00", "+1000", "+10:00",
            "+10", "+1000", "+10:00", "+1000", "+10:00",
            "+1000"
        },
        // -37800000
        {
            "-1030", "-1030", "-10:30", "-1030", "-10:30",
            "-1030", "-1030", "-10:30", "-1030", "-10:30",
            "-1030"
        },
        // -37845000
        {
            "-1030", "-1030", "-10:30", "-103045", "-10:30:45",
            "-1030", "-1030", "-10:30", "-103045", "-10:30:45",
            "-103045"
        },
        // 108000000
        {
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0
        }
    };

    const char* PATTERN[] = {
        "X", "XX", "XXX", "XXXX", "XXXXX",
        "x", "xx", "xxx", "xxxx", "xxxxx",
        "Z", // equivalent to "xxxx"
        0
    };

    const int32_t MIN_OFFSET_UNIT[] = {
        60000, 60000, 60000, 1000, 1000,
        60000, 60000, 60000, 1000, 1000,
        1000,
    };

    // Formatting
    UErrorCode status = U_ZERO_ERROR;
    LocalPointer<SimpleDateFormat> sdf(new SimpleDateFormat(status), status);
    if (U_FAILURE(status)) {
        dataerrln("Fail new SimpleDateFormat: %s", u_errorName(status));
        return;
    }
    UDate d = Calendar::getNow();

    for (uint32_t i = 0; i < UPRV_LENGTHOF(OFFSET); i++) {
        SimpleTimeZone* tz = new SimpleTimeZone(OFFSET[i], UnicodeString("Zone Offset:") + OFFSET[i] + "ms");
        sdf->adoptTimeZone(tz);
        for (int32_t j = 0; PATTERN[j] != 0; j++) {
            sdf->applyPattern(UnicodeString(PATTERN[j]));
            UnicodeString result;
            sdf->format(d, result);

            if (ISO_STR[i][j]) {
                if (result != UnicodeString(ISO_STR[i][j])) {
                    errln((UnicodeString)"FAIL: pattern=" + PATTERN[j] + ", offset=" + OFFSET[i] + " -> "
                        + result + " (expected: " + ISO_STR[i][j] + ")");
                }
            } else {
                // Offset out of range
                // Note: for now, there is no way to propagate the error status through
                // the SimpleDateFormat::format above.
                if (result.length() > 0) {
                    errln((UnicodeString)"FAIL: Non-Empty result for pattern=" + PATTERN[j] + ", offset=" + OFFSET[i]
                        + " (expected: empty result)");
                }
            }
        }
    }

    // Parsing
    LocalPointer<Calendar> outcal(Calendar::createInstance(status));
    if (U_FAILURE(status)) {
        dataerrln("Fail new Calendar: %s", u_errorName(status));
        return;
    }
    for (int32_t i = 0; ISO_STR[i][0] != NULL; i++) {
        for (int32_t j = 0; PATTERN[j] != 0; j++) {
            if (ISO_STR[i][j] == 0) {
                continue;
            }
            ParsePosition pos(0);
            SimpleTimeZone* bogusTZ = new SimpleTimeZone(-1, UnicodeString("Zone Offset: -1ms"));
            outcal->adoptTimeZone(bogusTZ);
            sdf->applyPattern(PATTERN[j]);

            sdf->parse(UnicodeString(ISO_STR[i][j]), *(outcal.getAlias()), pos);

            if (pos.getIndex() != (int32_t)uprv_strlen(ISO_STR[i][j])) {
                errln((UnicodeString)"FAIL: Failed to parse the entire input string: " + ISO_STR[i][j]);
            }

            const TimeZone& outtz = outcal->getTimeZone();
            int32_t outOffset = outtz.getRawOffset();
            int32_t adjustedOffset = OFFSET[i] / MIN_OFFSET_UNIT[j] * MIN_OFFSET_UNIT[j];
            if (outOffset != adjustedOffset) {
                errln((UnicodeString)"FAIL: Incorrect offset:" + outOffset + "ms for input string: " + ISO_STR[i][j]
                    + " (expected:" + adjustedOffset + "ms)");
            }
        }
    }
}


typedef struct {
    const char*     locale;
    const char*     tzid;
    UDate           date;
    UTimeZoneFormatStyle    style;
    const char*     expected;
    UTimeZoneFormatTimeType timeType;
} FormatTestData;

void
TimeZoneFormatTest::TestFormat(void) {
    UDate dateJan = 1358208000000.0;    // 2013-01-15T00:00:00Z
    UDate dateJul = 1373846400000.0;    // 2013-07-15T00:00:00Z

    const FormatTestData DATA[] = {
        {
            "en",
            "America/Los_Angeles", 
            dateJan,
            UTZFMT_STYLE_GENERIC_LOCATION,
            "Los Angeles Time",
            UTZFMT_TIME_TYPE_UNKNOWN
        },
        {
            "en",
            "America/Los_Angeles",
            dateJan,
            UTZFMT_STYLE_GENERIC_LONG,
            "Pacific Time",
            UTZFMT_TIME_TYPE_UNKNOWN
        },
        {
            "en",
            "America/Los_Angeles",
            dateJan,
            UTZFMT_STYLE_SPECIFIC_LONG,
            "Pacific Standard Time",
            UTZFMT_TIME_TYPE_STANDARD
        },
        {
            "en",
            "America/Los_Angeles",
            dateJul,
            UTZFMT_STYLE_SPECIFIC_LONG,
            "Pacific Daylight Time",
            UTZFMT_TIME_TYPE_DAYLIGHT
        },
        {
            "ja",
            "America/Los_Angeles",
            dateJan,
            UTZFMT_STYLE_ZONE_ID,
            "America/Los_Angeles",
            UTZFMT_TIME_TYPE_UNKNOWN
        },
        {
            "fr",
            "America/Los_Angeles",
            dateJul,
            UTZFMT_STYLE_ZONE_ID_SHORT,
            "uslax",
            UTZFMT_TIME_TYPE_UNKNOWN
        },
        {
            "en",
            "America/Los_Angeles",
            dateJan,
            UTZFMT_STYLE_EXEMPLAR_LOCATION,
            "Los Angeles",
            UTZFMT_TIME_TYPE_UNKNOWN
        },

        {
            "ja",
            "Asia/Tokyo",
            dateJan,
            UTZFMT_STYLE_GENERIC_LONG,
            "\\u65E5\\u672C\\u6A19\\u6E96\\u6642",
            UTZFMT_TIME_TYPE_UNKNOWN
        },

        {0, 0, 0.0, UTZFMT_STYLE_GENERIC_LOCATION, 0, UTZFMT_TIME_TYPE_UNKNOWN}
    };

    for (int32_t i = 0; DATA[i].locale; i++) {
        UErrorCode status = U_ZERO_ERROR;
        LocalPointer<TimeZoneFormat> tzfmt(TimeZoneFormat::createInstance(Locale(DATA[i].locale), status));
        if (U_FAILURE(status)) {
            dataerrln("Fail TimeZoneFormat::createInstance: %s", u_errorName(status));
            continue;
        }

        LocalPointer<TimeZone> tz(TimeZone::createTimeZone(DATA[i].tzid));
        UnicodeString out;
        UTimeZoneFormatTimeType timeType;

        tzfmt->format(DATA[i].style, *(tz.getAlias()), DATA[i].date, out, &timeType);
        UnicodeString expected(DATA[i].expected, -1, US_INV);
        expected = expected.unescape();

        assertEquals(UnicodeString("Format result for ") + DATA[i].tzid + " (Test Case " + i + ")", expected, out);
        if (DATA[i].timeType != timeType) {
            dataerrln(UnicodeString("Formatted time zone type (Test Case ") + i + "), returned="
                + timeType + ", expected=" + DATA[i].timeType);
        }
    }
}

void
TimeZoneFormatTest::TestFormatTZDBNames(void) {
    UDate dateJan = 1358208000000.0;    // 2013-01-15T00:00:00Z
    UDate dateJul = 1373846400000.0;    // 2013-07-15T00:00:00Z

    const FormatTestData DATA[] = {
        {
            "en",
            "America/Chicago", 
            dateJan,
            UTZFMT_STYLE_SPECIFIC_SHORT,
            "CST",
            UTZFMT_TIME_TYPE_STANDARD
        },
        {
            "en",
            "Asia/Shanghai", 
            dateJan,
            UTZFMT_STYLE_SPECIFIC_SHORT,
            "CST",
            UTZFMT_TIME_TYPE_STANDARD
        },
        {
            "zh_Hans",
            "Asia/Shanghai", 
            dateJan,
            UTZFMT_STYLE_SPECIFIC_SHORT,
            "CST",
            UTZFMT_TIME_TYPE_STANDARD
        },
        {
            "en",
            "America/Los_Angeles",
            dateJul,
            UTZFMT_STYLE_SPECIFIC_LONG,
            "GMT-07:00",    // No long display names
            UTZFMT_TIME_TYPE_DAYLIGHT
        },
        {
            "ja",
            "America/Los_Angeles",
            dateJul,
            UTZFMT_STYLE_SPECIFIC_SHORT,
            "PDT",
            UTZFMT_TIME_TYPE_DAYLIGHT
        },
        {
            "en",
            "Australia/Sydney",
            dateJan,
            UTZFMT_STYLE_SPECIFIC_SHORT,
            "AEDT",
            UTZFMT_TIME_TYPE_DAYLIGHT
        },
        {
            "en",
            "Australia/Sydney",
            dateJul,
            UTZFMT_STYLE_SPECIFIC_SHORT,
            "AEST",
            UTZFMT_TIME_TYPE_STANDARD
        },

        {0, 0, 0.0, UTZFMT_STYLE_GENERIC_LOCATION, 0, UTZFMT_TIME_TYPE_UNKNOWN}
    };

    for (int32_t i = 0; DATA[i].locale; i++) {
        UErrorCode status = U_ZERO_ERROR;
        Locale loc(DATA[i].locale);
        LocalPointer<TimeZoneFormat> tzfmt(TimeZoneFormat::createInstance(loc, status));
        if (U_FAILURE(status)) {
            dataerrln("Fail TimeZoneFormat::createInstance: %s", u_errorName(status));
            continue;
        }
        TimeZoneNames *tzdbNames = TimeZoneNames::createTZDBInstance(loc, status);
        if (U_FAILURE(status)) {
            dataerrln("Fail TimeZoneNames::createTZDBInstance: %s", u_errorName(status));
            continue;
        }
        tzfmt->adoptTimeZoneNames(tzdbNames);

        LocalPointer<TimeZone> tz(TimeZone::createTimeZone(DATA[i].tzid));
        UnicodeString out;
        UTimeZoneFormatTimeType timeType;

        tzfmt->format(DATA[i].style, *(tz.getAlias()), DATA[i].date, out, &timeType);
        UnicodeString expected(DATA[i].expected, -1, US_INV);
        expected = expected.unescape();

        assertEquals(UnicodeString("Format result for ") + DATA[i].tzid + " (Test Case " + i + ")", expected, out);
        if (DATA[i].timeType != timeType) {
            dataerrln(UnicodeString("Formatted time zone type (Test Case ") + i + "), returned="
                + timeType + ", expected=" + DATA[i].timeType);
        }
    }
}

void
TimeZoneFormatTest::TestFormatCustomZone(void) {
    struct {
        const char* id;
        int32_t offset;
        const char* expected;
    } TESTDATA[] = {
        { "abc", 3600000, "GMT+01:00" },                    // unknown ID
        { "$abc", -3600000, "GMT-01:00" },                 // unknown, with ASCII variant char '$'
        { "\\u00c1\\u00df\\u00c7", 5400000, "GMT+01:30"},    // unknown, with non-ASCII chars
        { 0, 0, 0 }
    };

    UDate now = Calendar::getNow();

    for (int32_t i = 0; ; i++) {
        const char *id = TESTDATA[i].id;
        if (id == 0) {
            break;
        }
        UnicodeString tzid = UnicodeString(id, -1, US_INV).unescape();
        SimpleTimeZone tz(TESTDATA[i].offset, tzid);

        UErrorCode status = U_ZERO_ERROR;
        LocalPointer<TimeZoneFormat> tzfmt(TimeZoneFormat::createInstance(Locale("en"), status));
        if (tzfmt.isNull()) {
            dataerrln("FAIL: TimeZoneFormat::createInstance failed for en");
            return;
        }
        UnicodeString tzstr;
        UnicodeString expected = UnicodeString(TESTDATA[i].expected, -1, US_INV).unescape();

        tzfmt->format(UTZFMT_STYLE_SPECIFIC_LONG, tz, now, tzstr, NULL);
        assertEquals(UnicodeString("Format result for ") + tzid, expected, tzstr);
    }
}

void
TimeZoneFormatTest::TestFormatTZDBNamesAllZoneCoverage(void) {
    UErrorCode status = U_ZERO_ERROR;
    LocalPointer<StringEnumeration> tzids(TimeZone::createEnumeration());
    if (tzids.getAlias() == nullptr) {
        dataerrln("%s %d tzids is null", __FILE__, __LINE__);
        return;
    }
    const UnicodeString *tzid;
    LocalPointer<TimeZoneNames> tzdbNames(TimeZoneNames::createTZDBInstance(Locale("en"), status));
    UDate now = Calendar::getNow();
    UnicodeString mzId;
    UnicodeString name;
    while ((tzid = tzids->snext(status))) {
        logln("Zone: " + *tzid);
        LocalPointer<TimeZone> tz(TimeZone::createTimeZone(*tzid));
        tzdbNames->getMetaZoneID(*tzid, now, mzId);
        if (mzId.isBogus()) {
            logln((UnicodeString)"Meta zone: <not available>");
        } else {
            logln((UnicodeString)"Meta zone: " + mzId);
        }

        // mzID could be bogus here
        tzdbNames->getMetaZoneDisplayName(mzId, UTZNM_SHORT_STANDARD, name);
        // name could be bogus here
        if (name.isBogus()) {
            logln((UnicodeString)"Meta zone short standard name: <not available>");
        }
        else {
            logln((UnicodeString)"Meta zone short standard name: " + name);
        }

        tzdbNames->getMetaZoneDisplayName(mzId, UTZNM_SHORT_DAYLIGHT, name);
        // name could be bogus here
        if (name.isBogus()) {
            logln((UnicodeString)"Meta zone short daylight name: <not available>");
        }
        else {
            logln((UnicodeString)"Meta zone short daylight name: " + name);
        }
    }
}

void
TimeZoneFormatTest::TestLocaleTZExtension(void) {
    UDate now = Calendar::getNow();

    static const char* const tz[] = {
        "adalv",
        "aedxb",
        "afkbl",
        "aganu",
        "aiaxa",
        "amevn",
        "ancur",
        "aolad",
        "aqcas",
        "aqdav",
        "aqddu",
        "aqmaw",
        "aqmcm",
        "aqplm",
        "aqsyw",
        "aqvos",
        "arbue",
        "arcor",
        "arirj",
        "arjuj",
        "arluq",
        "armdz",
        "arrgl",
        "arsla",
        "aruaq",
        "arush",
        "asppg",
        "auadl",
        "aubhq",
        "aubne",
        "audrw",
        "aueuc",
        "auhba",
        "aukns",
        "auldc",
        "auldh",
        "aumel",
        "aumqi",
        "auper",
        "ausyd",
        "awaua",
        "azbak",
        "basjj",
        "bbbgi",
        "bddac",
        "bebru",
        "bfoua",
        "bgsof",
        "bhbah",
        "bibjm",
        "bmbda",
        "bnbwn",
        "bolpb",
        "bqkra",
        "braux",
        "brbel",
        "brbvb",
        "brcgb",
        "brcgr",
        "brern",
        "brfen",
        "brfor",
        "brmao",
        "brmcz",
        "brpvh",
        "brrbr",
        "brrec",
        "brsao",
        "brssa",
        "bsnas",
        "bwgbe",
        "bymsq",
        "bzbze",
        "cacfq",
        "caedm",
        "caffs",
        "cafne",
        "caglb",
        "cagoo",
        "cahal",
        "caiql",
        "camon",
        "canpg",
        "careb",
        "careg",
        "casjf",
        "cavan",
        "cawnp",
        "caybx",
        "caycb",
        "cayda",
        "caydq",
        "cayek",
        "cayev",
        "cayxy",
        "cayyn",
        "cayzf",
        "cayzs",
        "cccck",
        "cdfbm",
        "cdfih",
        "cfbgf",
        "cgbzv",
        "chzrh",
        "ciabj",
        "ckrar",
        "clipc",
        "clpuq",
        "clscl",
        "cmdla",
        "cnsha",
        "cnurc",
        "cobog",
        "crsjo",
        "cuhav",
        "cvrai",
        "cxxch",
        "cyfmg",
        "cynic",
        "czprg",
        "deber",
        "debsngn",
        "djjib",
        "dkcph",
        "dmdom",
        "dosdq",
        "dzalg",
        "ecgps",
        "ecgye",
        "egcai",
        "eheai",
        "erasm",
        "esceu",
        "eslpa",
        "esmad",
        "fihel",
        "fimhq",
        "fjsuv",
        "fkpsy",
        "fmksa",
        "fmpni",
        "frpar",
        "galbv",
        "gaza",
        "gblon",
        "gdgnd",
        "gfcay",
        "gggci",
        "ghacc",
        "gigib",
        "gldkshvn",
        "glgoh",
        "globy",
        "gmbjl",
        "gncky",
        "gpbbr",
        "gpmsb",
        "gpsbh",
        "gqssg",
        "gsgrv",
        "gugum",
        "gwoxb",
        "gygeo",
        "hebron",
        "hkhkg",
        "hrzag",
        "hubud",
        "iddjj",
        "idmak",
        "idpnk",
        "iedub",
        "imdgs",
        "inccu",
        "iodga",
        "iqbgw",
        "isrey",
        "jeruslm",
        "jmkin",
        "joamm",
        "kenbo",
        "kgfru",
        "khpnh",
        "kicxi",
        "kipho",
        "kmyva",
        "knbas",
        "kpfnj",
        "krsel",
        "kwkwi",
        "kygec",
        "kzaau",
        "kzakx",
        "kzala",
        "kzguw",
        "kzkzo",
        "kzura",
        "lbbey",
        "lccas",
        "livdz",
        "lkcmb",
        "lrmlw",
        "lsmsu",
        "lulux",
        "lvrix",
        "macas",
        "mcmon",
        "mdkiv",
        "mhkwa",
        "mhmaj",
        "mkskp",
        "mlbko",
        "mmrgn",
        "mncoq",
        "mnhvd",
        "mnuln",
        "momfm",
        "mpspn",
        "mqfdf",
        "mrnkc",
        "msmni",
        "muplu",
        "mvmle",
        "mwblz",
        "mxchi",
        "mxcun",
        "mxhmo",
        "mxmam",
        "mxmex",
        "mxmid",
        "mxoji",
        "mxpvr",
        "mykch",
        "mykul",
        "mzmpm",
        "nawdh",
        "ncnou",
        "nenim",
        "nfnlk",
        "nglos",
        "nimga",
        "nlams",
        "noosl",
        "nrinu",
        "nuiue",
        "nzakl",
        "pelim",
        "pfgmr",
        "pfnhv",
        "pgpom",
        "pgraw",
        "phmnl",
        "pkkhi",
        "plwaw",
        "pmmqc",
        "pnpcn",
        "prsju",
        "pwror",
        "pyasu",
        "qadoh",
        "rereu",
        "robuh",
        "rsbeg",
        "ruasf",
        "rubax",
        "rudyr",
        "rugdx",
        "rukgd",
        "rukhndg",
        "rukra",
        "rukuf",
        "rukvx",
        "rumow",
        "runoz",
        "ruoms",
        "ruovb",
        "rupkc",
        "rusred",
        "ruuly",
        "ruunera",
        "ruuus",
        "ruvog",
        "ruvvo",
        "ruyek",
        "ruyks",
        "rwkgl",
        "saruh",
        "sbhir",
        "scmaw",
        "sgsin",
        "shshn",
        "silju",
        "sjlyr",
        "slfna",
        "smsai",
        "sndkr",
        "somgq",
        "srpbm",
        "ssjub",
        "svsal",
        "sxphi",
        "sydam",
        "szqmn",
        "uaiev",
        "uaozh",
        "uasip",
        "uauzh",
        "ugkla",
        "umawk",
        "umjon",
        "ummdy",
        "unk",
        "usadk",
        "usaeg",
        "usanc",
        "usboi",
        "uschi",
        "usden",
        "ushnl",
        "usind",
        "usinvev",
        "usjnu",
        "usknx",
        "uslax",
        "uslui",
        "usmnm",
        "usmoc",
        "usndnsl",
        "usnyc",
        "usoea",
        "usome",
        "usphx",
        "uswlz",
        "uswsq",
        "usxul",
        "usyak",
        "uymvd",
        "uzskd",
        "vcsvd",
        "veccs",
        "vnsgn",
        "vuvli",
        "wfmau",
        "wsapw",
        "yeade",
        "zajnb",
        "zmlun",
        "zwhre",
    };
    static const char* const timezone[] = {
        "Africa/Abidjan",
        "Africa/Accra",
        "Africa/Addis_Ababa",
        "Africa/Algiers",
        "Africa/Asmara",
        "Africa/Asmera",
        "Africa/Bamako",
        "Africa/Bangui",
        "Africa/Banjul",
        "Africa/Bissau",
        "Africa/Brazzaville",
        "Africa/Bujumbura",
        "Africa/Cairo",
        "Africa/Casablanca",
        "Africa/Conakry",
        "Africa/Dakar",
        "Africa/Dar_es_Salaam",
        "Africa/Douala",
        "Africa/El_Aaiun",
        "Africa/Gaborone",
        "Africa/Harare",
        "Africa/Johannesburg",
        "Africa/Juba",
        "Africa/Kampala",
        "Africa/Kigali",
        "Africa/Kinshasa",
        "Africa/Lagos",
        "Africa/Libreville",
        "Africa/Lome",
        "Africa/Luanda",
        "Africa/Lubumbashi",
        "Africa/Lusaka",
        "Africa/Malabo",
        "Africa/Maseru",
        "Africa/Mbabane",
        "Africa/Mogadishu",
        "Africa/Monrovia",
        "Africa/Nairobi",
        "Africa/Ndjamena",
        "Africa/Niamey",
        "Africa/Ouagadougou",
        "Africa/Sao_Tome",
        "Africa/Tripoli",
        "Africa/Tunis",
        "Africa/Windhoek",
        "America/Adak",
        "America/Anchorage",
        "America/Anguilla",
        "America/Araguaina",
        "America/Aruba",
        "America/Asuncion",
        "America/Bahia",
        "America/Bahia_Banderas",
        "America/Barbados",
        "America/Belem",
        "America/Belize",
        "America/Blanc-Sablon",
        "America/Boise",
        "America/Buenos_Aires",
        "America/Cambridge_Bay",
        "America/Campo_Grande",
        "America/Cancun",
        "America/Caracas",
        "America/Cayenne",
        "America/Cayman",
        "America/Chicago",
        "America/Chihuahua",
        "America/Coral_Harbour",
        "America/Cordoba",
        "America/Cuiaba",
        "America/Curacao",
        "America/Danmarkshavn",
        "America/Dawson",
        "America/Dawson_Creek",
        "America/Denver",
        "America/Dominica",
        "America/Eirunepe",
        "America/El_Salvador",
        "America/Ensenada",
        "America/Glace_Bay",
        "America/Goose_Bay",
        "America/Grand_Turk",
        "America/Grenada",
        "America/Guadeloupe",
        "America/Guayaquil",
        "America/Guyana",
        "America/Halifax",
        "America/Havana",
        "America/Hermosillo",
        "America/Indiana/Indianapolis",
        "America/Indiana/Knox",
        "America/Indiana/Marengo",
        "America/Indianapolis",
        "America/Indiana/Vevay",
        "America/Indiana/Vincennes",
        "America/Indiana/Winamac",
        "America/Inuvik",
        "America/Jamaica",
        "America/Jujuy",
        "America/Juneau",
        "America/Knox_IN",
        "America/Kralendijk",
        "America/La_Paz",
        "America/Lima",
        "America/Los_Angeles",
        "America/Louisville",
        "America/Lower_Princes",
        "America/Maceio",
        "America/Managua",
        "America/Manaus",
        "America/Mendoza",
        "America/Menominee",
        "America/Merida",
        "America/Miquelon",
        "America/Nassau",
        "America/New_York",
        "America/Nipigon",
        "America/Nome",
        "America/Noronha",
        "America/Ojinaga",
        "America/Panama",
        "America/Paramaribo",
        "America/Phoenix",
        "America/Rainy_River",
        "America/Recife",
        "America/Regina",
        "America/Rio_Branco",
        "America/Rosario",
        "America/Sao_Paulo",
        "America/Scoresbysund",
        "America/Shiprock",
        "America/Tegucigalpa",
        "America/Thule",
        "America/Thunder_Bay",
        "America/Tijuana",
        "America/Vancouver",
        "America/Virgin",
        "America/Winnipeg",
        "America/Yellowknife",
        "Asia/Aden",
        "Asia/Amman",
        "Asia/Anadyr",
        "Asia/Ashkhabad",
        "Asia/Baghdad",
        "Asia/Bahrain",
        "Asia/Baku",
        "Asia/Bangkok",
        "Asia/Barnaul",
        "Asia/Bishkek",
        "Asia/Brunei",
        "Asia/Choibalsan",
        "Asia/Chongqing",
        "Asia/Chungking",
        "Asia/Colombo",
        "Asia/Dacca",
        "Asia/Damascus",
        "Asia/Dhaka",
        "Asia/Dili",
        "Asia/Dubai",
        "Asia/Dushanbe",
        "Asia/Gaza",
        "Asia/Harbin",
        "Asia/Hebron",
        "Asia/Ho_Chi_Minh",
        "Asia/Hong_Kong",
        "Asia/Hovd",
        "Asia/Jayapura",
        "Asia/Jerusalem",
        "Asia/Kabul",
        "Asia/Karachi",
        "Asia/Kashgar",
        "Asia/Khandyga",
        "Asia/Krasnoyarsk",
        "Asia/Kuala_Lumpur",
        "Asia/Kuching",
        "Asia/Macao",
        "Asia/Macau",
        "Asia/Magadan",
        "Asia/Makassar",
        "Asia/Manila",
        "Asia/Nicosia",
        "Asia/Novosibirsk",
        "Asia/Omsk",
        "Asia/Oral",
        "Asia/Phnom_Penh",
        "Asia/Pyongyang",
        "Asia/Qyzylorda",
        "Asia/Rangoon",
        "Asia/Riyadh",
        "Asia/Saigon",
        "Asia/Sakhalin",
        "Asia/Samarkand",
        "Asia/Seoul",
        "Asia/Shanghai",
        "Asia/Singapore",
        "Asia/Srednekolymsk",
        "Asia/Taipei",
        "Asia/Tbilisi",
        "Asia/Tehran",
        "Asia/Tel_Aviv",
        "Asia/Thimbu",
        "Asia/Thimphu",
        "Asia/Tokyo",
        "Asia/Tomsk",
        "Asia/Ujung_Pandang",
        "Asia/Urumqi",
        "Asia/Yangon",
        "Asia/Yerevan",
        "Brazil/Acre",
        "Brazil/DeNoronha",
        "Canada/Newfoundland",
        "Canada/Pacific",
        "Canada/Yukon",
        "Europe/Andorra",
        "Europe/Belgrade",
        "Europe/Berlin",
        "Europe/Brussels",
        "Europe/Busingen",
        "Europe/Chisinau",
        "Europe/Copenhagen",
        "Europe/Dublin",
        "Europe/Guernsey",
        "Europe/Helsinki",
        "Europe/Isle_of_Man",
        "Europe/Jersey",
        "Europe/Kaliningrad",
        "Europe/Kiev",
        "Europe/Kirov",
        "Europe/Lisbon",
        "Europe/Ljubljana",
        "Europe/London",
        "Europe/Luxembourg",
        "Europe/Madrid",
        "Europe/Mariehamn",
        "Europe/Minsk",
        "Europe/Monaco",
        "Europe/Moscow",
        "Europe/Nicosia",
        "Europe/Oslo",
        "Europe/Paris",
        "Europe/Podgorica",
        "Europe/Prague",
        "Europe/Riga",
        "Europe/Rome",
        "Europe/Samara",
        "Europe/San_Marino",
        "Europe/Sarajevo",
        "Europe/Simferopol",
        "Europe/Skopje",
        "Europe/Sofia",
        "Europe/Tallinn",
        "Europe/Tirane",
        "Europe/Tiraspol",
        "Europe/Ulyanovsk",
        "Europe/Uzhgorod",
        "Europe/Vaduz",
        "Europe/Vienna",
        "Europe/Vilnius",
        "Europe/Volgograd",
        "Europe/Warsaw",
        "Europe/Zagreb",
        "Europe/Zaporozhye",
        "Europe/Zurich",
        "Indian/Chagos",
        "Indian/Cocos",
        "Indian/Comoro",
        "Indian/Kerguelen",
        "Indian/Mahe",
        "Indian/Maldives",
        "Indian/Reunion",
        "Mexico/BajaSur",
        "Mexico/General",
        "Pacific/Apia",
        "Pacific/Auckland",
        "Pacific/Bougainville",
        "Pacific/Chuuk",
        "Pacific/Enderbury",
        "Pacific/Fakaofo",
        "Pacific/Fiji",
        "Pacific/Galapagos",
        "Pacific/Gambier",
        "Pacific/Guadalcanal",
        "Pacific/Guam",
        "Pacific/Honolulu",
        "Pacific/Kosrae",
        "Pacific/Kwajalein",
        "Pacific/Majuro",
        "Pacific/Marquesas",
        "Pacific/Midway",
        "Pacific/Nauru",
        "Pacific/Niue",
        "Pacific/Norfolk",
        "Pacific/Noumea",
        "Pacific/Pago_Pago",
        "Pacific/Palau",
        "Pacific/Pohnpei",
        "Pacific/Ponape",
        "Pacific/Saipan",
        "Pacific/Samoa",
        "Pacific/Tarawa",
        "Pacific/Truk",
        "Pacific/Wake",
        "Pacific/Wallis",
        "Pacific/Yap",
        "US/Alaska",
        "US/Arizona",
        "US/Hawaii",
        "US/Michigan",
        "US/Pacific",
        "US/Pacific-New",
        "US/Samoa",
    };

    std::string locale;
    for (int32_t tzidx = 0; tzidx < UPRV_LENGTHOF(tz); tzidx++) {
        locale = "en-u-tz-";
        locale += tz[tzidx];
        Locale l(locale.c_str());
        UErrorCode status = U_ZERO_ERROR;
        SimpleDateFormat fmt(UnicodeString("V"), l, status);
        if (U_FAILURE(status)) {
            dataerrln("Error creating SimpleDateFormat(%s) - %s", locale.c_str(), u_errorName(status));
            continue;
        }
        UnicodeString result;
        fmt.format(now, result, status);
        if (U_FAILURE(status)) {
            dataerrln("Error format with locale %s - %s", locale.c_str(), u_errorName(status));
            continue;
        }
        std::string result_str;
        result.toUTF8String(result_str);
        if (result_str != tz[tzidx]) {
            dataerrln("Error format('V') with locale %s - expected %s but got %s", locale.c_str(), tz[tzidx], result_str.c_str());
        }
    }

    for (int32_t tzidx = 0; tzidx < UPRV_LENGTHOF(timezone); tzidx++) {
        locale = "en@timezone=";
        locale += timezone[tzidx];
        Locale l(locale.c_str());
        UErrorCode status = U_ZERO_ERROR;
        SimpleDateFormat fmt(UnicodeString("VV"), l, status);
        if (U_FAILURE(status)) {
            dataerrln("Error creating SimpleDateFormat(%s) - %s", locale.c_str(), u_errorName(status));
            continue;
        }
        UnicodeString result;
        fmt.format(now, result, status);
        if (U_FAILURE(status)) {
            dataerrln("Error format with locale %s - %s", locale.c_str(), u_errorName(status));
            continue;
        }
        std::string result_str;
        result.toUTF8String(result_str);
        if (result_str != timezone[tzidx]) {
            dataerrln("Error format('VV') with locale %s - expected %s but got %s", locale.c_str(), timezone[tzidx], result_str.c_str());
        }
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
