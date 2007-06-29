/*
*******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/dtrule.h"
#include "unicode/tzrule.h"
#include "unicode/rbtz.h"
#include "unicode/simpletz.h"
#include "unicode/tzrule.h"
#include "unicode/calendar.h"
#include "unicode/ucal.h"
#include "unicode/unistr.h"
#include "unicode/tztrans.h"
#include "unicode/vtzone.h"
#include "tzrulets.h"

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break
#define HOUR (60*60*1000)

static const char *const TESTZIDS[] = {
        "AGT",
        "America/New_York",
        "America/Los_Angeles",
        "America/Indiana/Indianapolis",
        "America/Havana",
        "Europe/Lisbon",
        "Europe/Paris",
        "Asia/Tokyo",
        "Asia/Sakhalin",
        "Africa/Cairo",
        "Africa/Windhoek",
        "Australia/Sydney",
        "Etc/GMT+8"
};

class TestZIDEnumeration : public StringEnumeration {
public:
    TestZIDEnumeration(UBool all = FALSE);
    ~TestZIDEnumeration();

    virtual int32_t count(UErrorCode& status) const {
        return len;
    }
    virtual const UnicodeString *snext(UErrorCode& status);
    virtual void reset(UErrorCode& status);
    static inline UClassID getStaticClassID() {
        return (UClassID)&fgClassID;
    }
    virtual UClassID getDynamicClassID() const {
        return getStaticClassID();
    }
private:
    static const char fgClassID;
    int32_t idx;
    int32_t len;
    StringEnumeration   *tzenum;
};

const char TestZIDEnumeration::fgClassID = 0;

TestZIDEnumeration::TestZIDEnumeration(UBool all)
: tzenum(NULL), idx(0) {
    UErrorCode status = U_ZERO_ERROR;
    if (all) {
        tzenum = TimeZone::createEnumeration();
        len = tzenum->count(status);
    } else {
        len = (int32_t)sizeof(TESTZIDS)/sizeof(TESTZIDS[0]);
    }
}

TestZIDEnumeration::~TestZIDEnumeration() {
    if (tzenum != NULL) {
        delete tzenum;
    }
}

const UnicodeString*
TestZIDEnumeration::snext(UErrorCode& status) {
    if (tzenum != NULL) {
        return tzenum->snext(status);
    } else if (U_SUCCESS(status) && idx < len) {
        unistr = UnicodeString(TESTZIDS[idx++], "");
        return &unistr;
    }
    return NULL;
}

void
TestZIDEnumeration::reset(UErrorCode& status) {
    if (tzenum != NULL) {
        tzenum->reset(status);
    } else {
        idx = 0;
    }
}


void TimeZoneRuleTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) {
        logln("TestSuite TestTimeZoneRule");
    }
    switch (index) {
        CASE(0, TestSimpleRuleBasedTimeZone);
        CASE(1, TestHistoricalRuleBasedTimeZone);
        CASE(2, TestOlsonTransition);
        CASE(3, TestRBTZTransition);
        CASE(4, TestHasEquivalentTransitions);
        CASE(5, TestVTimeZoneRoundTrip);
        CASE(6, TestVTimeZoneRoundTripPartial);
        CASE(7, TestVTimeZoneSimpleWrite);
        CASE(8, TestVTimeZoneHeaderProps);
        CASE(9, TestGetSimpleRules);
        default: name = ""; break;
    }
}

/*
 * Compare SimpleTimeZone with equivalent RBTZ
 */
void
TimeZoneRuleTest::TestSimpleRuleBasedTimeZone(void) {
    UErrorCode status = U_ZERO_ERROR;
    SimpleTimeZone stz(-1*HOUR, "TestSTZ",
        UCAL_SEPTEMBER, -30, -UCAL_SATURDAY, 1*HOUR, SimpleTimeZone::WALL_TIME,
        UCAL_FEBRUARY, 2, UCAL_SUNDAY, 1*HOUR, SimpleTimeZone::WALL_TIME,
        1*HOUR, status);
    if (U_FAILURE(status)) {
        errln("FAIL: Couldn't create SimpleTimezone.");
    }

    DateTimeRule *dtr;
    AnnualTimeZoneRule *atzr;
    int32_t STARTYEAR = 2000;

    InitialTimeZoneRule *ir = new InitialTimeZoneRule(
        "RBTZ_Initial", // Initial time Name
        -1*HOUR,        // Raw offset
        1*HOUR);        // DST saving amount

    // Original rules
    RuleBasedTimeZone *rbtz1 = new RuleBasedTimeZone("RBTZ1", ir->clone());
    dtr = new DateTimeRule(UCAL_SEPTEMBER, 30, UCAL_SATURDAY, FALSE,
        1*HOUR, DateTimeRule::WALL_TIME); // SUN<=30 in September, at 1AM wall time
    atzr = new AnnualTimeZoneRule("RBTZ_DST1",
        -1*HOUR /*rawOffset*/, 1*HOUR /*dstSavings*/, dtr,
        STARTYEAR, AnnualTimeZoneRule::MAX_YEAR);
    rbtz1->addTransitionRule(atzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 1-1.");
    }
    dtr = new DateTimeRule(UCAL_FEBRUARY, 2, UCAL_SUNDAY,
        1*HOUR, DateTimeRule::WALL_TIME);  // 2nd Sunday in February, at 1AM wall time
    atzr = new AnnualTimeZoneRule("RBTZ_STD1",
        -1*HOUR /*rawOffset*/, 0 /*dstSavings*/, dtr,
        STARTYEAR, AnnualTimeZoneRule::MAX_YEAR);
    rbtz1->addTransitionRule(atzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 1-2.");
    }
    rbtz1->complete(status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't complete RBTZ 1.");
    }

    // Equivalent, but different date rule type
    RuleBasedTimeZone *rbtz2 = new RuleBasedTimeZone("RBTZ2", ir->clone());
    dtr = new DateTimeRule(UCAL_SEPTEMBER, -1, UCAL_SATURDAY,
        1*HOUR, DateTimeRule::WALL_TIME); // Last Sunday in September at 1AM wall time
    atzr = new AnnualTimeZoneRule("RBTZ_DST2", -1*HOUR, 1*HOUR, dtr, STARTYEAR, AnnualTimeZoneRule::MAX_YEAR);
    rbtz2->addTransitionRule(atzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 2-1.");
    }
    dtr = new DateTimeRule(UCAL_FEBRUARY, 8, UCAL_SUNDAY, true,
        1*HOUR, DateTimeRule::WALL_TIME); // SUN>=8 in February, at 1AM wall time
    atzr = new AnnualTimeZoneRule("RBTZ_STD2", -1*HOUR, 0, dtr, STARTYEAR, AnnualTimeZoneRule::MAX_YEAR);
    rbtz2->addTransitionRule(atzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 2-2.");
    }
    rbtz2->complete(status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't complete RBTZ 2");
    }

    // Equivalent, but different time rule type
    RuleBasedTimeZone *rbtz3 = new RuleBasedTimeZone("RBTZ3", ir->clone());
    dtr = new DateTimeRule(UCAL_SEPTEMBER, 30, UCAL_SATURDAY, false,
        2*HOUR, DateTimeRule::UTC_TIME);
    atzr = new AnnualTimeZoneRule("RBTZ_DST3", -1*HOUR, 1*HOUR, dtr, STARTYEAR, AnnualTimeZoneRule::MAX_YEAR);
    rbtz3->addTransitionRule(atzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 3-1.");
    }
    dtr = new DateTimeRule(UCAL_FEBRUARY, 2, UCAL_SUNDAY,
        0*HOUR, DateTimeRule::STANDARD_TIME);
    atzr = new AnnualTimeZoneRule("RBTZ_STD3", -1*HOUR, 0, dtr, STARTYEAR, AnnualTimeZoneRule::MAX_YEAR);
    rbtz3->addTransitionRule(atzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 3-2.");
    }
    rbtz3->complete(status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't complete RBTZ 3");
    }

    // Check equivalency for 10 years
    UDate start = getUTCMillis(STARTYEAR, UCAL_JANUARY, 1);
    UDate until = getUTCMillis(STARTYEAR + 10, UCAL_JANUARY, 1);

    if (!(stz.hasEquivalentTransitions(*rbtz1, start, until, TRUE, status))) {
        errln("FAIL: rbtz1 must be equivalent to the SimpleTimeZone in the time range.");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error returned from hasEquivalentTransitions");
    }
    if (!(stz.hasEquivalentTransitions(*rbtz2, start, until, TRUE, status))) {
        errln("FAIL: rbtz2 must be equivalent to the SimpleTimeZone in the time range.");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error returned from hasEquivalentTransitions");
    }
    if (!(stz.hasEquivalentTransitions(*rbtz3, start, until, TRUE, status))) {
        errln("FAIL: rbtz3 must be equivalent to the SimpleTimeZone in the time range.");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error returned from hasEquivalentTransitions");
    }

    delete ir;
    delete rbtz1;
    delete rbtz2;
    delete rbtz3;
}

/*
 * Test equivalency between OlsonTimeZone and custom RBTZ representing the
 * equivalent rules in a certain time range
 */
void
TimeZoneRuleTest::TestHistoricalRuleBasedTimeZone(void) {
    UErrorCode status = U_ZERO_ERROR;

    // Compare to America/New_York with equivalent RBTZ
    BasicTimeZone *ny = (BasicTimeZone*)TimeZone::createTimeZone("America/New_York");

    //RBTZ
    InitialTimeZoneRule *ir = new InitialTimeZoneRule("EST", -5*HOUR, 0);
    RuleBasedTimeZone *rbtz = new RuleBasedTimeZone("EST5EDT", ir);

    DateTimeRule *dtr;
    AnnualTimeZoneRule *tzr;

    // Standard time
    dtr = new DateTimeRule(UCAL_OCTOBER, -1, UCAL_SUNDAY,
        2*HOUR, DateTimeRule::WALL_TIME); // Last Sunday in October, at 2AM wall time
    tzr = new AnnualTimeZoneRule("EST", -5*HOUR /*rawOffset*/, 0 /*dstSavings*/, dtr, 1967, 2006);
    rbtz->addTransitionRule(tzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 1.");
    }

    dtr = new DateTimeRule(UCAL_NOVEMBER, 1, UCAL_SUNDAY,
        true, 2*HOUR, DateTimeRule::WALL_TIME); // SUN>=1 in November, at 2AM wall time
    tzr = new AnnualTimeZoneRule("EST", -5*HOUR, 0, dtr, 2007, AnnualTimeZoneRule::MAX_YEAR);
    rbtz->addTransitionRule(tzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 2.");
    }

    // Daylight saving time
    dtr = new DateTimeRule(UCAL_APRIL, -1, UCAL_SUNDAY,
        2*HOUR, DateTimeRule::WALL_TIME); // Last Sunday in April, at 2AM wall time
    tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1967, 1973);
    rbtz->addTransitionRule(tzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 3.");
    }

    dtr = new DateTimeRule(UCAL_JANUARY, 6,
        2*HOUR, DateTimeRule::WALL_TIME); // January 6, at 2AM wall time
    tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1974, 1974);
    rbtz->addTransitionRule(tzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 4.");
    }
    
    dtr = new DateTimeRule(UCAL_FEBRUARY, 23,
        2*HOUR, DateTimeRule::WALL_TIME); // February 23, at 2AM wall time
    tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1975, 1975);
    rbtz->addTransitionRule(tzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 5.");
    }

    dtr = new DateTimeRule(UCAL_APRIL, -1, UCAL_SUNDAY,
        2*HOUR, DateTimeRule::WALL_TIME); // Last Sunday in April, at 2AM wall time
    tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1976, 1986);
    rbtz->addTransitionRule(tzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 6.");
    }

    dtr = new DateTimeRule(UCAL_APRIL, 1, UCAL_SUNDAY,
        true, 2*HOUR, DateTimeRule::WALL_TIME); // SUN>=1 in April, at 2AM wall time
    tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1987, 2006);
    rbtz->addTransitionRule(tzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 7.");
    }

    dtr = new DateTimeRule(UCAL_MARCH, 8, UCAL_SUNDAY,
        true, 2*HOUR, DateTimeRule::WALL_TIME); // SUN>=8 in March, at 2AM wall time
    tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 2007, AnnualTimeZoneRule::MAX_YEAR);
    rbtz->addTransitionRule(tzr, status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't add AnnualTimeZoneRule 7.");
    }

    rbtz->complete(status);
    if (U_FAILURE(status)) {
        errln("FAIL: couldn't complete RBTZ.");
    }

    // hasEquivalentTransitions
    UDate jan1_1950 = getUTCMillis(1950, UCAL_JANUARY, 1);
    UDate jan1_1967 = getUTCMillis(1971, UCAL_JANUARY, 1);
    UDate jan1_2010 = getUTCMillis(2010, UCAL_JANUARY, 1);        

    if (!ny->hasEquivalentTransitions(*rbtz, jan1_1967, jan1_2010, TRUE, status)) {
        errln("FAIL: The RBTZ must be equivalent to America/New_York between 1967 and 2010");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error returned from hasEquivalentTransitions");
    }
    if (ny->hasEquivalentTransitions(*rbtz, jan1_1950, jan1_2010, TRUE, status)) {
        errln("FAIL: The RBTZ must not be equivalent to America/New_York between 1950 and 2010");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error returned from hasEquivalentTransitions");
    }

    // Same with above, but calling RBTZ#hasEquivalentTransitions against OlsonTimeZone
    if (!rbtz->hasEquivalentTransitions(*ny, jan1_1967, jan1_2010, TRUE, status)) {
        errln("FAIL: The RBTZ must be equivalent to America/New_York between 1967 and 2010");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error returned from hasEquivalentTransitions");
    }
    if (rbtz->hasEquivalentTransitions(*ny, jan1_1950, jan1_2010, TRUE, status)) {
        errln("FAIL: The RBTZ must not be equivalent to America/New_York between 1950 and 2010");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error returned from hasEquivalentTransitions");
    }

    delete ny;
    delete rbtz;
}

/*
 * Check if transitions returned by getNextTransition/getPreviousTransition
 * are actual time transitions.
 */
void
TimeZoneRuleTest::TestOlsonTransition(void) {

    const int32_t TESTYEARS[][2] = {
        {1895, 1905}, // including int32 minimum second
        {1965, 1975}, // including the epoch
        {1995, 2015}, // practical year range
        {0,0}
    };

    UErrorCode status = U_ZERO_ERROR;
    TestZIDEnumeration tzenum(!quick);
    while (TRUE) {
        const UnicodeString *tzid = tzenum.snext(status);
        if (tzid == NULL) {
            break;
        }
        if (U_FAILURE(status)) {
            errln("FAIL: error returned while enumerating timezone IDs.");
            break;
        }
        BasicTimeZone *tz = (BasicTimeZone*)TimeZone::createTimeZone(*tzid);
        for (int32_t i = 0; TESTYEARS[i][0] != 0 || TESTYEARS[i][1] != 0; i++) {
            UDate lo = getUTCMillis(TESTYEARS[i][0], UCAL_JANUARY, 1);
            UDate hi = getUTCMillis(TESTYEARS[i][1], UCAL_JANUARY, 1);
            verifyTransitions(*tz, lo, hi);
        }
        delete tz;
    }
}

/*
 * Check if an OlsonTimeZone and its equivalent RBTZ have the exact same
 * transitions.
 */
void
TimeZoneRuleTest::TestRBTZTransition(void) {
    const int32_t STARTYEARS[] = {
        1900,
        1960,
        1990,
        2010,
        0
    };

    UErrorCode status = U_ZERO_ERROR;
    TestZIDEnumeration tzenum(!quick);
    while (TRUE) {
        const UnicodeString *tzid = tzenum.snext(status);
        if (tzid == NULL) {
            break;
        }
        if (U_FAILURE(status)) {
            errln("FAIL: error returned while enumerating timezone IDs.");
            break;
        }
        BasicTimeZone *tz = (BasicTimeZone*)TimeZone::createTimeZone(*tzid);
        int32_t ruleCount = tz->countTransitionRules(status);

        const InitialTimeZoneRule *initial;
        const TimeZoneRule **trsrules = new const TimeZoneRule*[ruleCount];
        tz->getTimeZoneRules(initial, trsrules, ruleCount, status);
        if (U_FAILURE(status)) {
            errln((UnicodeString)"FAIL: failed to get the TimeZoneRules from time zone " + *tzid);
        }
        RuleBasedTimeZone *rbtz = new RuleBasedTimeZone(*tzid, initial->clone());
        if (U_FAILURE(status)) {
            errln((UnicodeString)"FAIL: failed to get the transition rule count from time zone " + *tzid);
        }
        for (int32_t i = 0; i < ruleCount; i++) {
            rbtz->addTransitionRule(trsrules[i]->clone(), status);
            if (U_FAILURE(status)) {
                errln((UnicodeString)"FAIL: failed to add a transition rule at index " + i + " to the RBTZ for " + *tzid);
            }
        }
        rbtz->complete(status);
        if (U_FAILURE(status)) {
            errln((UnicodeString)"FAIL: complete() failed for the RBTZ for " + *tzid);
        }

        for (int32_t idx = 0; STARTYEARS[idx] != 0; idx++) {
            UDate start = getUTCMillis(STARTYEARS[idx], UCAL_JANUARY, 1);
            UDate until = getUTCMillis(STARTYEARS[idx] + 20, UCAL_JANUARY, 1);
            // Compare the original OlsonTimeZone with the RBTZ starting the startTime for 20 years

            // Ascending
            compareTransitionsAscending(*tz, *rbtz, start, until, FALSE);
            // Ascending/inclusive
            compareTransitionsAscending(*tz, *rbtz, start + 1, until, TRUE);
            // Descending
            compareTransitionsDescending(*tz, *rbtz, start, until, FALSE);
            // Descending/inclusive
            compareTransitionsDescending(*tz, *rbtz, start + 1, until, TRUE);
        }
        delete trsrules;
        delete rbtz;
        delete tz;
    }
}

void
TimeZoneRuleTest::TestHasEquivalentTransitions(void) {
    // America/New_York and America/Indiana/Indianapolis are equivalent
    // since 2006
    UErrorCode status = U_ZERO_ERROR;
    BasicTimeZone *newyork = (BasicTimeZone*)TimeZone::createTimeZone("America/New_York");
    BasicTimeZone *indianapolis = (BasicTimeZone*)TimeZone::createTimeZone("America/Indiana/Indianapolis");
    BasicTimeZone *gmt_5 = (BasicTimeZone*)TimeZone::createTimeZone("Etc/GMT+5");

    UDate jan1_1971 = getUTCMillis(1971, UCAL_JANUARY, 1);
    UDate jan1_2005 = getUTCMillis(2005, UCAL_JANUARY, 1);
    UDate jan1_2006 = getUTCMillis(2006, UCAL_JANUARY, 1);
    UDate jan1_2007 = getUTCMillis(2007, UCAL_JANUARY, 1);
    UDate jan1_2011 = getUTCMillis(2010, UCAL_JANUARY, 1);

    if (newyork->hasEquivalentTransitions(*indianapolis, jan1_2005, jan1_2011, TRUE, status)) {
        errln("FAIL: New_York is not equivalent to Indianapolis between 2005 and 2010");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error status is returned from hasEquivalentTransition");
    }
    if (!newyork->hasEquivalentTransitions(*indianapolis, jan1_2006, jan1_2011, TRUE, status)) {
        errln("FAIL: New_York is equivalent to Indianapolis between 2006 and 2010");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error status is returned from hasEquivalentTransition");
    }

    if (!indianapolis->hasEquivalentTransitions(*gmt_5, jan1_1971, jan1_2006, TRUE, status)) {
        errln("FAIL: Indianapolis is equivalent to GMT+5 between 1971 and 2005");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error status is returned from hasEquivalentTransition");
    }
    if (indianapolis->hasEquivalentTransitions(*gmt_5, jan1_1971, jan1_2007, TRUE, status)) {
        errln("FAIL: Indianapolis is not equivalent to GMT+5 between 1971 and 2006");
    }
    if (U_FAILURE(status)) {
        errln("FAIL: error status is returned from hasEquivalentTransition");
    }

    delete newyork;
    delete indianapolis;
    delete gmt_5;
}

/*
 * Write out time zone rules of OlsonTimeZone into VTIMEZONE format, create a new
 * VTimeZone from the VTIMEZONE data, then compare transitions
 */
void
TimeZoneRuleTest::TestVTimeZoneRoundTrip(void) {
    UDate startTime = getUTCMillis(1850, UCAL_JANUARY, 1);
    UDate endTime = getUTCMillis(2050, UCAL_JANUARY, 1);

    UErrorCode status = U_ZERO_ERROR;
    TestZIDEnumeration tzenum(!quick);
    while (TRUE) {
        const UnicodeString *tzid = tzenum.snext(status);
        if (tzid == NULL) {
            break;
        }
        if (U_FAILURE(status)) {
            errln("FAIL: error returned while enumerating timezone IDs.");
            break;
        }
        BasicTimeZone *tz = (BasicTimeZone*)TimeZone::createTimeZone(*tzid);
        VTimeZone *vtz_org = VTimeZone::createVTimeZoneByID(*tzid);
        VTimeZone *vtz_new = NULL;
        UnicodeString vtzdata;
        // Write out VTIMEZONE data
        vtz_org->write(vtzdata, status);
        if (U_FAILURE(status)) {
            errln((UnicodeString)"FAIL: error returned while writing time zone rules for " +
                *tzid + " into VTIMEZONE format.");
        } else {
            // Read VTIMEZONE data
            vtz_new = VTimeZone::createVTimeZone(vtzdata, status);
            if (U_FAILURE(status)) {
                errln((UnicodeString)"FAIL: error returned while reading VTIMEZONE data for " + *tzid);
            } else {
                // Check equivalency after the first transition.
                // The DST information before the first transition might be lost
                // because there is no good way to represent the initial time with
                // VTIMEZONE.
                int32_t raw1, raw2, dst1, dst2;
                tz->getOffset(startTime, FALSE, raw1, dst1, status);
                vtz_new->getOffset(startTime, FALSE, raw2, dst2, status);
                if (U_FAILURE(status)) {
                    errln("FAIL: error status is returned from getOffset");
                } else {
                    if (raw1 + dst1 != raw2 + dst2) {
                        errln("FAIL: VTimeZone for " + *tzid +
                            " is not equivalent to its OlsonTimeZone corresponding at "
                            + dateToString(startTime));
                    }
                    TimeZoneTransition trans;
                    UBool avail = tz->getNextTransition(startTime, FALSE, trans);
                    if (avail) {
                        if (!vtz_new->hasEquivalentTransitions(*tz, trans.getTime(),
                                endTime, TRUE, status)) {
                            errln("FAIL: VTimeZone for " + *tzid +
                                " is not equivalent to its OlsonTimeZone corresponding.");
                        }
                        if (U_FAILURE(status)) {
                            errln("FAIL: error status is returned from hasEquivalentTransition");
                        }
                    }
                }
            }
        }
        delete tz;
        delete vtz_org;
        delete vtz_new;
    }
}

/*
 * Write out time zone rules of OlsonTimeZone after a cutover date into VTIMEZONE format,
 * create a new VTimeZone from the VTIMEZONE data, then compare transitions
 */
void
TimeZoneRuleTest::TestVTimeZoneRoundTripPartial(void) {
    const int32_t CUTOVERYEARS[] = {
        1900,
        1950,
        2020,
        0
    };
    UDate endTime = getUTCMillis(2050, UCAL_JANUARY, 1);

    UErrorCode status = U_ZERO_ERROR;
    TestZIDEnumeration tzenum(!quick);
    while (TRUE) {
        const UnicodeString *tzid = tzenum.snext(status);
        if (tzid == NULL) {
            break;
        }
        if (U_FAILURE(status)) {
            errln("FAIL: error returned while enumerating timezone IDs.");
            break;
        }
        BasicTimeZone *tz = (BasicTimeZone*)TimeZone::createTimeZone(*tzid);
        VTimeZone *vtz_org = VTimeZone::createVTimeZoneByID(*tzid);
        VTimeZone *vtz_new = NULL;
        UnicodeString vtzdata;

        for (int32_t i = 0; CUTOVERYEARS[i] != 0; i++) {
            UDate startTime = getUTCMillis(CUTOVERYEARS[i], UCAL_JANUARY, 1);
            vtz_org->write(startTime, vtzdata, status);
            if (U_FAILURE(status)) {
                errln((UnicodeString)"FAIL: error returned while writing time zone rules for " +
                    *tzid + " into VTIMEZONE format since " + dateToString(startTime));
            } else {
                // Read VTIMEZONE data
                vtz_new = VTimeZone::createVTimeZone(vtzdata, status);
                if (U_FAILURE(status)) {
                    errln((UnicodeString)"FAIL: error returned while reading VTIMEZONE data for " + *tzid
                        + " since " + dateToString(startTime));
                } else {
                    // Check equivalency after the first transition.
                    // The DST information before the first transition might be lost
                    // because there is no good way to represent the initial time with
                    // VTIMEZONE.
                    int32_t raw1, raw2, dst1, dst2;
                    tz->getOffset(startTime, FALSE, raw1, dst1, status);
                    vtz_new->getOffset(startTime, FALSE, raw2, dst2, status);
                    if (U_FAILURE(status)) {
                        errln("FAIL: error status is returned from getOffset");
                    } else {
                        if (raw1 + dst1 != raw2 + dst2) {
                            errln("FAIL: VTimeZone for " + *tzid +
                                " is not equivalent to its OlsonTimeZone corresponding at "
                                + dateToString(startTime));
                        }
                        TimeZoneTransition trans;
                        UBool avail = tz->getNextTransition(startTime, FALSE, trans);
                        if (avail) {
                            if (!vtz_new->hasEquivalentTransitions(*tz, trans.getTime(),
                                    endTime, TRUE, status)) {
                                errln("FAIL: VTimeZone for " + *tzid +
                                    " is not equivalent to its OlsonTimeZone corresponding.");
                            }
                            if (U_FAILURE(status)) {
                                errln("FAIL: error status is returned from hasEquivalentTransition");
                            }
                        }
                    }
                }
            }
            if (vtz_new != NULL) {
                delete vtz_new;
                vtz_new = NULL;
            }
        }
        delete tz;
        delete vtz_org;
    }
}

/*
 * Write out simple time zone rules from an OlsonTimeZone at various time into VTIMEZONE
 * format and create a new VTimeZone from the VTIMEZONE data, then make sure the raw offset
 * and DST savings are same in these two time zones.
 */
void
TimeZoneRuleTest::TestVTimeZoneSimpleWrite(void) {
    const int32_t TESTDATES[][3] = {
        {2006,  UCAL_JANUARY,   1},
        {2006,  UCAL_MARCH,     15},
        {2006,  UCAL_MARCH,     31},
        {2006,  UCAL_OCTOBER,   25},
        {2006,  UCAL_NOVEMBER,  1},
        {2006,  UCAL_NOVEMBER,  5},
        {2007,  UCAL_JANUARY,   1},
        {0,     0,              0}
    };

    UErrorCode status = U_ZERO_ERROR;
    TestZIDEnumeration tzenum(!quick);
    while (TRUE) {
        const UnicodeString *tzid = tzenum.snext(status);
        if (tzid == NULL) {
            break;
        }
        if (U_FAILURE(status)) {
            errln("FAIL: error returned while enumerating timezone IDs.");
            break;
        }
        VTimeZone *vtz_org = VTimeZone::createVTimeZoneByID(*tzid);
        VTimeZone *vtz_new = NULL;
        UnicodeString vtzdata;

        for (int32_t i = 0; TESTDATES[i][0] != 0; i++) {
            UDate time = getUTCMillis(TESTDATES[i][0], TESTDATES[i][1], TESTDATES[i][2]);
            vtz_org->writeSimple(time, vtzdata, status);
            if (U_FAILURE(status)) {
                errln((UnicodeString)"FAIL: error returned while writing simple time zone rules for " +
                    *tzid + " into VTIMEZONE format at " + dateToString(time));
            } else {
                // Read VTIMEZONE data
                vtz_new = VTimeZone::createVTimeZone(vtzdata, status);
                if (U_FAILURE(status)) {
                    errln((UnicodeString)"FAIL: error returned while reading simple VTIMEZONE data for " + *tzid
                        + " at " + dateToString(time));
                } else {
                    int32_t raw0, dst0;
                    int32_t raw1, dst1;
                    vtz_org->getOffset(time, FALSE, raw0, dst0, status);
                    vtz_new->getOffset(time, FALSE, raw1, dst1, status);
                    if (U_SUCCESS(status)) {
                        if (raw0 != raw1 || dst0 != dst1) {
                            errln("FAIL: VTimeZone writeSimple for " + *tzid + " at "
                                + dateToString(time) + " failed to the round trip.");
                        }
                    } else {
                        errln("FAIL: getOffset returns error status");
                    }
                }
            }
            if (vtz_new != NULL) {
                delete vtz_new;
                vtz_new = NULL;
            }
        }
        delete vtz_org;
    }
}

/*
 * Write out time zone rules of OlsonTimeZone into VTIMEZONE format with RFC2445 header TZURL and
 * LAST-MODIFIED, create a new VTimeZone from the VTIMEZONE data to see if the headers are preserved.
 */
void
TimeZoneRuleTest::TestVTimeZoneHeaderProps(void) {
    const UnicodeString TESTURL1("http://source.icu-project.org");
    const UnicodeString TESTURL2("http://www.ibm.com");

    UErrorCode status = U_ZERO_ERROR;
    UnicodeString tzurl;
    UDate lmod;
    UDate lastmod = getUTCMillis(2007, UCAL_JUNE, 1);
    VTimeZone *vtz = VTimeZone::createVTimeZoneByID("America/Chicago");
    vtz->setTZURL(TESTURL1);
    vtz->setLastModified(lastmod);

    // Roundtrip conversion
    UnicodeString vtzdata;
    vtz->write(vtzdata, status);
    VTimeZone *newvtz1;
    if (U_FAILURE(status)) {
        errln("FAIL: error returned while writing VTIMEZONE data 1");
    } else {
        // Create a new one
        newvtz1 = VTimeZone::createVTimeZone(vtzdata, status);
        if (U_FAILURE(status)) {
            errln("FAIL: error returned while loading VTIMEZONE data 1");
        } else {
            // Check if TZURL and LAST-MODIFIED properties are preserved
            newvtz1->getTZURL(tzurl);
            if (tzurl.compare(TESTURL1) != 0) {
                errln("FAIL: TZURL 1 was not preserved");
            }
            vtz->getLastModified(lmod);
            if (lastmod != lmod) {
                errln("FAIL: LAST-MODIFIED was not preserved");
            }
        }
    }

    if (U_SUCCESS(status)) {
        // Set different tzurl
        newvtz1->setTZURL(TESTURL2);

        // Second roundtrip, with a cutover
        newvtz1->write(vtzdata, status);
        if (U_FAILURE(status)) {
            errln("FAIL: error returned while writing VTIMEZONE data 2");
        } else {
            VTimeZone *newvtz2 = VTimeZone::createVTimeZone(vtzdata, status);
            if (U_FAILURE(status)) {
                errln("FAIL: error returned while loading VTIMEZONE data 2");
            } else {
                // Check if TZURL and LAST-MODIFIED properties are preserved
                newvtz2->getTZURL(tzurl);
                if (tzurl.compare(TESTURL2) != 0) {
                    errln("FAIL: TZURL was not preserved in the second roundtrip");
                }
                vtz->getLastModified(lmod);
                if (lastmod != lmod) {
                    errln("FAIL: LAST-MODIFIED was not preserved in the second roundtrip");
                }
            }
        }
    }
}

void
TimeZoneRuleTest::TestGetSimpleRules(void) {
    UDate testTimes[] = {
        getUTCMillis(1970, UCAL_JANUARY, 1),
        getUTCMillis(2000, UCAL_MARCH, 31),
        getUTCMillis(2005, UCAL_JULY, 1),
        getUTCMillis(2010, UCAL_NOVEMBER, 1),        
    };
    int32_t numTimes = sizeof(testTimes)/sizeof(UDate);
    UErrorCode status = U_ZERO_ERROR;
    TestZIDEnumeration tzenum(!quick);
    InitialTimeZoneRule *initial;
    AnnualTimeZoneRule *std, *dst;
    for (int32_t i = 0; i < numTimes ; i++) {
        while (TRUE) {
            const UnicodeString *tzid = tzenum.snext(status);
            if (tzid == NULL) {
                break;
            }
            if (U_FAILURE(status)) {
                errln("FAIL: error returned while enumerating timezone IDs.");
                break;
            }
            BasicTimeZone *tz = (BasicTimeZone*)TimeZone::createTimeZone(*tzid);
            initial = NULL;
            std = dst = NULL;
            tz->getSimpleRulesNear(testTimes[i], initial, std, dst, status);
            if (U_FAILURE(status)) {
                errln("FAIL: getSimpleRules failed.");
                break;
            }
            if (initial == NULL) {
                errln("FAIL: initial rule must not be NULL");
                break;
            } else if (!(std == NULL && dst == NULL || std != NULL && dst != NULL)) {
                errln("FAIL: invalid std/dst pair.");
                break;
            }
            if (std != NULL) {
                const DateTimeRule *dtr = std->getRule();
                if (dtr->getDateRuleType() != DateTimeRule::DOW) {
                    errln("FAIL: simple std rull must use DateTimeRule::DOW as date rule.");
                    break;
                }
                if (dtr->getTimeRuleType() != DateTimeRule::WALL_TIME) {
                    errln("FAIL: simple std rull must use DateTimeRule::WALL_TIME as time rule.");
                    break;
                }
                dtr = dst->getRule();
                if (dtr->getDateRuleType() != DateTimeRule::DOW) {
                    errln("FAIL: simple dst rull must use DateTimeRule::DOW as date rule.");
                    break;
                }
                if (dtr->getTimeRuleType() != DateTimeRule::WALL_TIME) {
                    errln("FAIL: simple dst rull must use DateTimeRule::WALL_TIME as time rule.");
                    break;
                }                
            }
            // Create an RBTZ from the rules and compare the offsets at the date
            RuleBasedTimeZone *rbtz = new RuleBasedTimeZone(*tzid, initial);
            if (std != NULL) {
                rbtz->addTransitionRule(std, status);
                if (U_FAILURE(status)) {
                    errln("FAIL: couldn't add std rule.");
                }
                rbtz->addTransitionRule(dst, status);
                if (U_FAILURE(status)) {
                    errln("FAIL: couldn't add dst rule.");
                }
            }
            rbtz->complete(status);
            if (U_FAILURE(status)) {
                errln("FAIL: couldn't complete rbtz for " + *tzid);
            }

            int32_t raw0, dst0, raw1, dst1;
            tz->getOffset(testTimes[i], FALSE, raw0, dst0, status);
            if (U_FAILURE(status)) {
                errln("FAIL: couldn't get offsets from tz for " + *tzid);
            }
            rbtz->getOffset(testTimes[i], FALSE, raw1, dst1, status);
            if (U_FAILURE(status)) {
                errln("FAIL: couldn't get offsets from rbtz for " + *tzid);
            }
            if (raw0 != raw1 || dst0 != dst1) {
                errln("FAIL: rbtz created by simple rule does not match the original tz for tzid " + *tzid);
            }
            delete rbtz;
            delete tz;
        }
    }
}


UDate
TimeZoneRuleTest::getUTCMillis(int32_t y, int32_t m, int32_t d,
                               int32_t hr, int32_t min, int32_t sec, int32_t msec) {
    UErrorCode status = U_ZERO_ERROR;
    const TimeZone *tz = TimeZone::getGMT();
    Calendar *cal = Calendar::createInstance(*tz, status);
    if (U_FAILURE(status)) {
        delete cal;
        errln("FAIL: Calendar::createInstance failed");
        return 0.0;
    }
    cal->set(y, m, d, hr, min, sec);
    cal->set(UCAL_MILLISECOND, msec);
    UDate utc = cal->getTime(status);
    if (U_FAILURE(status)) {
        delete cal;
        errln("FAIL: Calendar::getTime failed");
        return 0.0;
    }
    delete cal;
    return utc;
}

/*
 * Check if a time shift really happens on each transition returned by getNextTransition or
 * getPreviousTransition in the specified time range
 */
void
TimeZoneRuleTest::verifyTransitions(BasicTimeZone& icutz, UDate start, UDate end) {
    UErrorCode status = U_ZERO_ERROR;
    UDate time;
    int32_t raw, dst, raw0, dst0;
    TimeZoneTransition tzt, tzt0;
    UBool avail;
    UBool first = TRUE;
    UnicodeString tzid;

    // Ascending
    time = start;
    while (TRUE) {
        avail = icutz.getNextTransition(time, FALSE, tzt);
        if (!avail) {
            break;
        }
        time = tzt.getTime();
        if (time >= end) {
            break;
        }
        icutz.getOffset(time, FALSE, raw, dst, status);
        icutz.getOffset(time - 1, FALSE, raw0, dst0, status);
        if (U_FAILURE(status)) {
            errln("FAIL: Error in getOffset");
            break;
        }

        if (raw == raw0 && dst == dst0) {
            errln((UnicodeString)"FAIL: False transition returned by getNextTransition for "
                + icutz.getID(tzid) + " at " + dateToString(time));
        }
        if (!first &&
                (tzt0.getTo()->getRawOffset() != tzt.getFrom()->getRawOffset()
                || tzt0.getTo()->getDSTSavings() != tzt.getFrom()->getDSTSavings())) {
            errln((UnicodeString)"FAIL: TO rule of the previous transition does not match FROM rule of this transtion at "
                    + dateToString(time) + " for " + icutz.getID(tzid));                
        }
        tzt0 = tzt;
        first = FALSE;
    }

    // Descending
    first = TRUE;
    time = end;
    while(true) {
        avail = icutz.getPreviousTransition(time, FALSE, tzt);
        if (!avail) {
            break;
        }
        time = tzt.getTime();
        if (time <= start) {
            break;
        }
        icutz.getOffset(time, FALSE, raw, dst, status);
        icutz.getOffset(time - 1, FALSE, raw0, dst0, status);
        if (U_FAILURE(status)) {
            errln("FAIL: Error in getOffset");
            break;
        }

        if (raw == raw0 && dst == dst0) {
            errln((UnicodeString)"FAIL: False transition returned by getPreviousTransition for "
                + icutz.getID(tzid) + " at " + dateToString(time));
        }

        if (!first &&
                (tzt0.getFrom()->getRawOffset() != tzt.getTo()->getRawOffset()
                || tzt0.getFrom()->getDSTSavings() != tzt.getTo()->getDSTSavings())) {
            errln((UnicodeString)"FAIL: TO rule of the next transition does not match FROM rule in this transtion at "
                    + dateToString(time) + " for " + icutz.getID(tzid));                
        }
        tzt0 = tzt;
        first = FALSE;
    }
}

/*
 * Compare all time transitions in 2 time zones in the specified time range in ascending order
 */
void
TimeZoneRuleTest::compareTransitionsAscending(BasicTimeZone& z1, BasicTimeZone& z2,
                                              UDate start, UDate end, UBool inclusive) {
    UnicodeString zid1, zid2;
    TimeZoneTransition tzt1, tzt2;
    UBool avail1, avail2;
    UBool inRange1, inRange2;

    z1.getID(zid1);
    z2.getID(zid2);

    UDate time = start;
    while (TRUE) {
        avail1 = z1.getNextTransition(time, inclusive, tzt1);
        avail2 = z2.getNextTransition(time, inclusive, tzt2);

        inRange1 = inRange2 = FALSE;
        if (avail1) {
            if (tzt1.getTime() < end || (inclusive && tzt1.getTime() == end)) {
                inRange1 = TRUE;
            }
        }
        if (avail2) {
            if (tzt2.getTime() < end || (inclusive && tzt2.getTime() == end)) {
                inRange2 = TRUE;
            }
        }
        if (!inRange1 && !inRange2) {
            // No more transition in the range
            break;
        }
        if (!inRange1) {
            errln((UnicodeString)"FAIL: " + zid1 + " does not have any transitions after "
                + dateToString(time) + " before " + dateToString(end));
            break;
        }
        if (!inRange2) {
            errln((UnicodeString)"FAIL: " + zid2 + " does not have any transitions after "
                + dateToString(time) + " before " + dateToString(end));
            break;
        }
        if (tzt1.getTime() != tzt2.getTime()) {
            errln((UnicodeString)"FAIL: First transition after " + dateToString(time) + " "
                    + zid1 + "[" + dateToString(tzt1.getTime()) + "] "
                    + zid2 + "[" + dateToString(tzt2.getTime()) + "]");
            break;
        }
        time = tzt1.getTime();
        if (inclusive) {
            time += 1;
        }
    }
}

/*
 * Compare all time transitions in 2 time zones in the specified time range in descending order
 */
void
TimeZoneRuleTest::compareTransitionsDescending(BasicTimeZone& z1, BasicTimeZone& z2,
                                               UDate start, UDate end, UBool inclusive) {
    UnicodeString zid1, zid2;
    TimeZoneTransition tzt1, tzt2;
    UBool avail1, avail2;
    UBool inRange1, inRange2;

    z1.getID(zid1);
    z2.getID(zid2);

    UDate time = end;
    while (TRUE) {
        avail1 = z1.getPreviousTransition(time, inclusive, tzt1);
        avail2 = z2.getPreviousTransition(time, inclusive, tzt2);

        inRange1 = inRange2 = FALSE;
        if (avail1) {
            if (tzt1.getTime() > start || (inclusive && tzt1.getTime() == start)) {
                inRange1 = TRUE;
            }
        }
        if (avail2) {
            if (tzt2.getTime() > start || (inclusive && tzt2.getTime() == start)) {
                inRange2 = TRUE;
            }
        }
        if (!inRange1 && !inRange2) {
            // No more transition in the range
            break;
        }
        if (!inRange1) {
            errln((UnicodeString)"FAIL: " + zid1 + " does not have any transitions before "
                + dateToString(time) + " after " + dateToString(start));
            break;
        }
        if (!inRange2) {
            errln((UnicodeString)"FAIL: " + zid2 + " does not have any transitions before "
                + dateToString(time) + " after " + dateToString(start));
            break;
        }
        if (tzt1.getTime() != tzt2.getTime()) {
            errln((UnicodeString)"FAIL: Last transition before " + dateToString(time) + " "
                    + zid1 + "[" + dateToString(tzt1.getTime()) + "] "
                    + zid2 + "[" + dateToString(tzt2.getTime()) + "]");
            break;
        }
        time = tzt1.getTime();
        if (inclusive) {
            time -= 1;
        }
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof
