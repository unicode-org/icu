
/*
********************************************************************
* COPYRIGHT:
* (C) Copyright Taligent, Inc., 1997
* (C) Copyright International Business Machines Corporation, 1997 - 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved.
* US Government Users Restricted Rights - Use, duplication, or disclosure
* restricted by GSA ADP Schedule Contract with IBM Corp.
*
********************************************************************
*/

#include <math.h>
#include "tztest.h"
#include "timezone.h"
#include "simpletz.h"
#include "calendar.h"
#include "gregocal.h"
#include "resbund.h"

#define CHECK_HEAP 0

#if defined(_WIN32) && !defined(__WINDOWS__)
#define _CRTDBG_MAP_ALLOC
#include <crtdbg.h>
#endif

// *****************************************************************************
// class TimeZoneTest
// *****************************************************************************

void TimeZoneTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite TestTimeZone");
    switch (index) {
        case 0:
            name = "TestPRTOffset";
            if (exec) {
                logln("TestPRTOffset---"); logln("");
                TestPRTOffset();
            }
            break;
        case 1:
            name = "TestVariousAPI518";
            if (exec) {
                logln("TestVariousAPI518---"); logln("");
                TestVariousAPI518();
            }
            break;
        case 2:
            name = "TestGetAvailableIDs913";
            if (exec) {
                logln("TestGetAvailableIDs913---"); logln("");
                TestGetAvailableIDs913();
            }
            break;
        case 3:
            name = "TestGenericAPI";
            if (exec) {
                logln("TestGenericAPI---"); logln("");
                TestGenericAPI();
            }
            break;
        case 4:
            name = "TestRuleAPI";
            if (exec) {
                logln("TestRuleAPI---"); logln("");
                TestRuleAPI();
            }
            break;
        case 5:
            name = "TestShortZoneIDs";
            if (exec) {
                logln("TestShortZoneIDs---"); logln("");
                TestShortZoneIDs();
            }
            break;
        case 6:
            name = "TestCustomParse";
            if (exec) {
                logln("TestCustomParse---"); logln("");
                TestCustomParse();
            }
            break;
        case 7:
            name = "TestDisplayName";
            if (exec) {
                logln("TestDisplayName---"); logln("");
                TestDisplayName();
            }
            break;

        case 8:
            name = "TestDSTSavings";
            if (exec) {
                logln("TestDSTSavings---"); logln("");
                TestDSTSavings();
            }
            break;

        case 9:
            name = "TestAlternateRules";
            if (exec) {
                logln("TestAlternateRules---"); logln("");
                TestAlternateRules();
            }
            break;

        default: name = ""; break;
    }
}

const int32_t TimeZoneTest::millisPerHour = 3600000;

// ---------------------------------------------------------------------------------

/**
 * Generic API testing for API coverage.
 */
void
TimeZoneTest::TestGenericAPI()
{
    UnicodeString id("NewGMT");
    int32_t offset = 12345;

    SimpleTimeZone *zone = new SimpleTimeZone(offset, id);
    if (zone->useDaylightTime()) errln("FAIL: useDaylightTime should return FALSE");

    TimeZone* zoneclone = zone->clone();
    if (!(*zoneclone == *zone)) errln("FAIL: clone or operator== failed");
    zoneclone->setID("abc");
    if (!(*zoneclone != *zone)) errln("FAIL: clone or operator!= failed");
    delete zoneclone;

    zoneclone = zone->clone();
    if (!(*zoneclone == *zone)) errln("FAIL: clone or operator== failed");
    zoneclone->setRawOffset(45678);
    if (!(*zoneclone != *zone)) errln("FAIL: clone or operator!= failed");

    SimpleTimeZone copy(*zone);
    if (!(copy == *zone)) errln("FAIL: copy constructor or operator== failed");
    copy = *(SimpleTimeZone*)zoneclone;
    if (!(copy == *zoneclone)) errln("FAIL: assignment operator or operator== failed");

    TimeZone* saveDefault = TimeZone::createDefault();
        TimeZone* pstZone = TimeZone::createTimeZone("PST");

    logln("call u_t_timezone() which uses the host");
        logln("to get the difference in seconds between coordinated universal");
        logln("time and local time. E.g., -28,800 for PST (GMT-8hrs)");

    int32_t tzoffset = icu_timezone();
    logln(UnicodeString("Value returned from t_timezone = ") + tzoffset);
    // Invert sign because UNIX semantics are backwards
    if (tzoffset < 0) tzoffset = -tzoffset;
        UErrorCode status = ZERO_ERROR;
    // --- The following test would fail outside PST now that
    // --- PST is generally set to be default timezone in format tests
    //if ((*saveDefault == *pstZone) && (tzoffset != 28800)) {
    //  errln("FAIL: t_timezone may be incorrect.  It is not 28800");
    //}

    if (tzoffset != 28800) {
        logln("***** WARNING: If testing in the PST timezone, t_timezone should return 28800! *****");
    }
    if ((tzoffset % 1800 != 0)) {
      errln("FAIL: t_timezone may be incorrect. It is not a multiple of 30min.");
    }

    TimeZone::adoptDefault(zone);
    TimeZone* defaultzone = TimeZone::createDefault();
    if (defaultzone == zone ||
        !(*defaultzone == *zone))
        errln("FAIL: createDefault failed");
    TimeZone::adoptDefault(saveDefault);
    delete defaultzone;
    delete zoneclone;
    delete pstZone;
}

// ---------------------------------------------------------------------------------

/**
 * Test the setStartRule/setEndRule API calls.
 */
void
TimeZoneTest::TestRuleAPI()
{
    UErrorCode status = ZERO_ERROR;

    UDate offset = 60*60*1000*1.75; // Pick a weird offset
    SimpleTimeZone *zone = new SimpleTimeZone((int32_t)offset, "TestZone");
    if (zone->useDaylightTime()) errln("FAIL: useDaylightTime should return FALSE");

    // Establish our expected transition times.  Do this with a non-DST
    // calendar with the (above) declared local offset.
    GregorianCalendar *gc = new GregorianCalendar(*zone, status);
    if (failure(status, "new GregorianCalendar")) return;
    gc->clear();
    gc->set(1990, Calendar::MARCH, 1);
    UDate marchOneStd = gc->getTime(status); // Local Std time midnight
    gc->clear();
    gc->set(1990, Calendar::JULY, 1);
    UDate julyOneStd = gc->getTime(status); // Local Std time midnight
    if (failure(status, "GregorianCalendar::getTime")) return;

    // Starting and ending hours, WALL TIME
    int32_t startHour = (int32_t)(2.25 * 3600000);
    int32_t endHour   = (int32_t)(3.5  * 3600000);

    zone->setStartRule(Calendar::MARCH, 1, 0, startHour, status);
    zone->setEndRule  (Calendar::JULY,  1, 0, endHour, status);

    delete gc;
    gc = new GregorianCalendar(*zone, status);
    if (failure(status, "new GregorianCalendar")) return;

    UDate marchOne = marchOneStd + startHour;
    UDate julyOne = julyOneStd + endHour - 3600000; // Adjust from wall to Std time

    UDate expMarchOne = 636251400000.0;
    if (marchOne != expMarchOne)
    {
        errln((UnicodeString)"FAIL: Expected start computed as " + marchOne +
          " = " + dateToString(marchOne));
        logln((UnicodeString)"      Should be                  " + expMarchOne +
          " = " + dateToString(expMarchOne));
    }

    UDate expJulyOne = 646793100000.0;
    if (julyOne != expJulyOne)
    {
        errln((UnicodeString)"FAIL: Expected start computed as " + julyOne +
          " = " + dateToString(julyOne));
        logln((UnicodeString)"      Should be                  " + expJulyOne +
          " = " + dateToString(expJulyOne));
    }

    testUsingBinarySearch(zone, date(90, Calendar::JANUARY, 1), date(90, Calendar::JUNE, 15), marchOne);
    testUsingBinarySearch(zone, date(90, Calendar::JUNE, 1), date(90, Calendar::DECEMBER, 31), julyOne);

    if (zone->inDaylightTime(marchOne - 1000, status) ||
        !zone->inDaylightTime(marchOne, status))
        errln("FAIL: Start rule broken");
    if (!zone->inDaylightTime(julyOne - 1000, status) ||
        zone->inDaylightTime(julyOne, status))
        errln("FAIL: End rule broken");

    zone->setStartYear(1991);
    if (zone->inDaylightTime(marchOne, status) ||
        zone->inDaylightTime(julyOne - 1000, status))
        errln("FAIL: Start year broken");

    failure(status, "TestRuleAPI");
    delete gc;
    delete zone;
}

void
TimeZoneTest::testUsingBinarySearch(SimpleTimeZone* tz, UDate min, UDate max, UDate expectedBoundary)
{
    UErrorCode status = ZERO_ERROR;
    bool_t startsInDST = tz->inDaylightTime(min, status);
    if (failure(status, "SimpleTimeZone::inDaylightTime")) return;
    if (tz->inDaylightTime(max, status) == startsInDST) {
        logln("Error: inDaylightTime(" + dateToString(max) + ") != " + ((!startsInDST)?"TRUE":"FALSE"));
        return;
    }
    if (failure(status, "SimpleTimeZone::inDaylightTime")) return;
    while ((max - min) > INTERVAL) {
        UDate mid = (min + max) / 2;
        if (tz->inDaylightTime(mid, status) == startsInDST) {
            min = mid;
        }
        else {
            max = mid;
        }
        if (failure(status, "SimpleTimeZone::inDaylightTime")) return;
    }
    logln(UnicodeString("Binary Search Before: ") + icu_floor(0.5 + min) + " = " + dateToString(min));
    logln(UnicodeString("Binary Search After:  ") + icu_floor(0.5 + max) + " = " + dateToString(max));
    UDate mindelta = expectedBoundary - min;
    UDate maxdelta = max - expectedBoundary;
    if (mindelta >= 0 &&
        mindelta <= INTERVAL &&
        mindelta >= 0 &&
        mindelta <= INTERVAL)
        logln(UnicodeString("PASS: Expected bdry:  ") + expectedBoundary + " = " + dateToString(expectedBoundary));
    else
        errln(UnicodeString("FAIL: Expected bdry:  ") + expectedBoundary + " = " + dateToString(expectedBoundary));
}

const UDate TimeZoneTest::INTERVAL = 100;

// ---------------------------------------------------------------------------------

// -------------------------------------

/**
 * Test the offset of the PRT timezone.
 */
void
TimeZoneTest::TestPRTOffset()
{
    TimeZone* tz = TimeZone::createTimeZone("PRT");
    if (tz == 0) {
        errln("FAIL: TimeZone(PRT) is null");
    }
    else {
        if (tz->getRawOffset() != (- 4 * millisPerHour)) errln("FAIL: Offset for PRT should be -4");
    }
    delete tz;
}

// -------------------------------------

/**
 * Regress a specific bug with a sequence of API calls.
 */
void
TimeZoneTest::TestVariousAPI518()
{
    UErrorCode status = ZERO_ERROR;
    TimeZone* time_zone = TimeZone::createTimeZone("PST");
    UDate d = date(97, Calendar::APRIL, 30);
    UnicodeString str;
    logln("The timezone is " + time_zone->getID(str));
    if (!time_zone->inDaylightTime(d, status)) errln("FAIL: inDaylightTime returned FALSE");
    if (FAILURE(status)) { errln("FAIL: TimeZone::inDaylightTime failed"); return; }
    if (!time_zone->useDaylightTime()) errln("FAIL: useDaylightTime returned FALSE");
    if (time_zone->getRawOffset() != - 8 * millisPerHour) errln("FAIL: getRawOffset returned wrong value");
    GregorianCalendar *gc = new GregorianCalendar(status);
    if (FAILURE(status)) { errln("FAIL: Couldn't create GregorianCalendar"); return; }
    gc->setTime(d, status);
    if (FAILURE(status)) { errln("FAIL: GregorianCalendar::setTime failed"); return; }
    if (time_zone->getOffset(gc->AD, gc->get(gc->YEAR, status), gc->get(gc->MONTH, status),
        gc->get(gc->DAY_OF_MONTH, status), (uint8_t)gc->get(gc->DAY_OF_WEEK, status), 0) != - 7 * millisPerHour)
        errln("FAIL: getOffset returned wrong value");
    if (FAILURE(status)) { errln("FAIL: GregorianCalendar::set failed"); return; }
    delete gc;
    delete time_zone;
}

// -------------------------------------

/**
 * Test the call which retrieves the available IDs.
 */
void
TimeZoneTest::TestGetAvailableIDs913()
{
#if defined(_WIN32) && !defined(__WINDOWS__)
#if defined(WIN32) && defined(_DEBUG) && CHECK_HEAP
    /*
     * Set the debug-heap flag to keep freed blocks in the
     * heap's linked list - This will allow us to catch any
     * inadvertent use of freed memory
     */
    int32_t tmpDbgFlag = _CrtSetDbgFlag(_CRTDBG_REPORT_FLAG);
    tmpDbgFlag |= _CRTDBG_DELAY_FREE_MEM_DF;
    tmpDbgFlag |= _CRTDBG_LEAK_CHECK_DF;
    tmpDbgFlag |= _CRTDBG_CHECK_ALWAYS_DF;
    _CrtSetDbgFlag(tmpDbgFlag);

    _CrtMemState memstate;
    _CrtMemCheckpoint(&memstate);
    {
#endif
#endif

    UnicodeString str;
    UnicodeString *buf = new UnicodeString("TimeZone.getAvailableIDs() = { ");
    int32_t s_length;
    const UnicodeString** s = TimeZone::createAvailableIDs(s_length);
    int32_t i;
    for (i = 0; i < s_length;++i) {
        if (i > 0) *buf += ", ";
        *buf += *s[i];
    }
    *buf += " };";
    logln(*buf);
    buf->truncate(0);
    *buf += "TimeZone.getAvailableIDs(GMT+02:00) = { ";
    delete [] s;

    s = TimeZone::createAvailableIDs(+ 2 * 60 * 60 * 1000, s_length);
    for (i = 0; i < s_length;++i) {
        if (i > 0) *buf += ", ";
        *buf += *s[i];
    }
    *buf += " };";
    logln(*buf);
    TimeZone *tz = TimeZone::createTimeZone("PST");
    if (tz != 0) logln("getTimeZone(PST) = " + tz->getID(str));
    else errln("FAIL: getTimeZone(PST) = null");
    delete tz;
    tz = TimeZone::createTimeZone("America/Los_Angeles");
    if (tz != 0) logln("getTimeZone(America/Los_Angeles) = " + tz->getID(str));
    else errln("FAIL: getTimeZone(PST) = null");
    delete tz;

    // @bug 4096694
    tz = TimeZone::createTimeZone("NON_EXISTENT");
    UnicodeString temp;
    if (tz == 0)
        errln("FAIL: getTimeZone(NON_EXISTENT) = null");
    else if (tz->getID(temp) != "GMT")
        errln("FAIL: getTimeZone(NON_EXISTENT) = " + temp);
    delete tz;

    delete buf;
    delete [] s;
 /*
#if defined(WIN32) && defined(_DEBUG) && CHECK_HEAP
    }
    _CrtMemDumpAllObjectsSince(&memstate);

    /*
     * Set the debug-heap flag to keep freed blocks in the
     * heap's linked list - This will allow us to catch any
     * inadvertent use of freed memory
     */ /*
    tmpDbgFlag = _CrtSetDbgFlag(_CRTDBG_REPORT_FLAG);
    tmpDbgFlag |= _CRTDBG_DELAY_FREE_MEM_DF;
    tmpDbgFlag &= ~_CRTDBG_LEAK_CHECK_DF;
    tmpDbgFlag &= ~_CRTDBG_CHECK_ALWAYS_DF;
    _CrtSetDbgFlag(tmpDbgFlag);
#endif*/
}


    /**
     * [srl - from java - 7/5/1998]
     * @bug 4130885
     * Certain short zone IDs, used since 1.1.x, are incorrect.
     *
     * The worst of these is:
     *
     * "CAT" (Central African Time) should be GMT+2:00, but instead returns a
     * zone at GMT-1:00. The zone at GMT-1:00 should be called EGT, CVT, EGST,
     * or AZOST, depending on which zone is meant, but in no case is it CAT.
     *
     * Other wrong zone IDs:
     *
     * ECT (European Central Time) GMT+1:00: ECT is Ecuador Time,
     * GMT-5:00. European Central time is abbreviated CEST.
     *
     * SST (Solomon Island Time) GMT+11:00. SST is actually Samoa Standard Time,
     * GMT-11:00. Solomon Island time is SBT.
     *
     * NST (New Zealand Time) GMT+12:00. NST is the abbreviation for
     * Newfoundland Standard Time, GMT-3:30. New Zealanders use NZST.
     *
     * AST (Alaska Standard Time) GMT-9:00. [This has already been noted in
     * another bug.] It should be "AKST". AST is Atlantic Standard Time,
     * GMT-4:00.
     *
     * PNT (Phoenix Time) GMT-7:00. PNT usually means Pitcairn Time,
     * GMT-8:30. There is no standard abbreviation for Phoenix time, as distinct
     * from MST with daylight savings.
     *
     * In addition to these problems, a number of zones are FAKE. That is, they
     * don't match what people use in the real world.
     *
     * FAKE zones:
     *
     * EET (should be EEST)
     * ART (should be EEST)
     * MET (should be IRST)
     * NET (should be AMST)
     * PLT (should be PKT)
     * BST (should be BDT)
     * VST (should be ICT)
     * CTT (should be CST) +
     * ACT (should be CST) +
     * AET (should be EST) +
     * MIT (should be WST) +
     * IET (should be EST) +
     * PRT (should be AST) +
     * CNT (should be NST)
     * AGT (should be ARST)
     * BET (should be EST) +
     *
     * + A zone with the correct name already exists and means something
     * else. E.g., EST usually indicates the US Eastern zone, so it cannot be
     * used for Brazil (BET).
     */
void TimeZoneTest::TestShortZoneIDs()
{
     int32_t i;
     // Create a small struct to hold the array
     struct
     {
          char      id[20];
          int32_t   offset;
          bool_t    daylight;
     }
     kReferenceList [] =
     {
        "MIT", -660, FALSE,
        "HST", -600, FALSE,
        "AST", -540, TRUE,
        "PST", -480, TRUE,
        "PNT", -420, FALSE,
        "MST", -420, TRUE,
        "CST", -360, TRUE,
        "IET", -300, FALSE,
        "EST", -300, TRUE,
        "PRT", -240, FALSE,
        "CNT", -210, TRUE,
        "AGT", -180, FALSE,
        "BET", -180, TRUE,
        // "CAT", -60, FALSE, // Wrong:
        // As of bug 4130885, fix CAT (Central Africa)
        "CAT", 120, FALSE, // Africa/Harare
        "GMT", 0, FALSE,
        "UTC", 0, FALSE, // ** srl: seems broken in C++
        "ECT", 60, TRUE,
        "ART", 120, TRUE,
        "EET", 120, TRUE,
        "EAT", 180, FALSE,
        "MET", 210, TRUE,
        "NET", 240, FALSE,
        "PLT", 300, FALSE,
        "IST", 330, FALSE,
        "BST", 360, FALSE,
        "VST", 420, FALSE,
        "CTT", 480, FALSE,
        "JST", 540, FALSE,
        "ACT", 570, FALSE,
        "AET", 600, TRUE,
        "SST", 660, FALSE,
        // "NST", 720, FALSE,
        // As of bug 4130885, fix NST (New Zealand)
        "NST", 720, TRUE, // Pacific/Auckland
        "",0,FALSE
     };


     for(i=0;kReferenceList[i].id[0];i++)
     {
          UnicodeString itsID(kReferenceList[i].id);


          // Check existence.

          TimeZone *tz = TimeZone::createTimeZone(itsID);

          if(!tz)
          {
              errln("FAIL: Time Zone " + itsID + " does not exist!");
              continue;
          }


          // Check daylight usage.

          bool_t usesDaylight = tz->useDaylightTime();

          if(usesDaylight != kReferenceList[i].daylight)
          {
              errln("FAIL: Time Zone " + itsID + " 's daylight is " + (usesDaylight?"TRUE":"FALSE") +
                    " but it should be " + ((kReferenceList[i].daylight)?"TRUE":"FALSE"));
          }


          // Check offset

          int32_t offsetInMinutes = tz->getRawOffset()/60000;

          if(offsetInMinutes != kReferenceList[i].offset)
          {
              errln("FAIL: Time Zone " + itsID + " 's offset is " + offsetInMinutes +
                    " but it should be " + kReferenceList[i].offset);
          }

          logln(" Time Zone " + itsID + " is OK. ");
          delete tz;
     }


     // OK now test compat
     logln("Testing for compatibility zones");

     const char* compatibilityMap[] =
    {
        // UTC is Universal Time, Coordinated.  Synonym for GMT.
        /*GMT+0*/ "UTC", "GMT",
        // ECT is the ID for European Central Time time zone.
        /*GMT+1*/ "ECT", "Europe/Paris",
        // EET is the ID for Eastern European Time time zone.
        /*GMT+2*/ "EET", "Europe/Istanbul",
        // ART is the ID for (Arabic) Egypt Standard Time timezone.
        /*GMT+2*/ "ART", "Africa/Cairo",
        // CAT is the ID for Central African Time time zone.
        /*GMT+2*/ "CAT", "Africa/Johannesburg",
        // EAT is the ID for Eastern African Time time zone.
        /*GMT+3*/ "EAT", "Asia/Riyadh",
        // MET is the ID for Middle East Time time zone.
        /*GMT+0330*/ "MET", "Asia/Tehran",
        // NET is the ID for Near East Time time zone.
        /*GMT+4*/ "NET", "Asia/Yerevan",
        // PLT is the ID for Pakistan Lahore Time time zone.
        /*GMT+5*/ "PLT", "Asia/Karachi",
        // IST is the ID for India Standard Time time zone.
        /*GMT+0550*/ "IST", "Asia/Calcutta",
        // BST is the ID for Bangladesh Standard Time time zone.
        /*GMT+6*/ "BST", "Asia/Dacca",
        // VST is the ID for Vietnam Standard Time time zone.
        /*GMT+7*/ "VST", "Asia/Bangkok",
        // CTT is the ID for China Taiwan Time time zone.
        /*GMT+8*/ "CTT", "Asia/Shanghai",
        // JST is the ID for Japan Standard Time time zone.
        /*GMT+9*/ "JST", "Asia/Tokyo",
        // ACT is the ID for Australia Central Time time zone.
        /*GMT+0930*/ "ACT", "Australia/Darwin",
        // AET is the ID for Australia Eastern Time time zone.
        /*GMT+10*/ "AET", "Australia/Sydney",
        // SST is the ID for Solomon Standard Time time zone.
        /*GMT+11*/ "SST", "Pacific/Guadalcanal",
        // NST is the ID for New Zealand Standard Time time zone.
        /*GMT+12*/ "NST", "Pacific/Auckland",
        // MIT is the ID for Midway Islands Time time zone.
        /*GMT-11*/ "MIT", "Pacific/Apia",
        // HST is the ID for Hawaii Standard Time time zone.
        /*GMT-10*/ "HST", "Pacific/Honolulu",
        // AST is the ID for Alaska Standard Time time zone.
        /*GMT-9*/ "AST", "America/Anchorage",
        // PST is the ID for Pacific Standard Time time zone.
        /*GMT-8*/ "PST", "America/Los_Angeles",
        // PNT is the ID for Phoenix Standard Time time zone.
        /*GMT-7*/ "PNT", "America/Phoenix",
        // MST is the ID for Mountain Standard Time time zone.
        /*GMT-7*/ "MST", "America/Denver",
        // CST is the ID for Central Standard Time time zone.
        /*GMT-6*/ "CST", "America/Chicago",
        // EST is the ID for Eastern Standard Time time zone.
        /*GMT-5*/ "EST", "America/New_York",
        // IET is the ID for Indiana Eastern Standard Time time zone.
        /*GMT-5*/ "IET", "America/Indianapolis",
        // PRT is the ID for Puerto Rico and US Virgin Islands Time time zone.
        /*GMT-4*/ "PRT", "America/Caracas",
        // CNT is the ID for Canada Newfoundland Time time zone.
        /*GMT-0330*/ "CNT", "America/St_Johns",
        // AGT is the ID for Argentina Standard Time time zone.
        /*GMT-3*/ "AGT", "America/Buenos_Aires",
        // BET is the ID for Brazil Eastern Time time zone.
        /*GMT-3*/ "BET", "America/Sao_Paulo",
         "\0","\0","\0",
    };

    for(i=0;*compatibilityMap[i];i+=2)
    {
        UnicodeString itsID;

        const char *zone1 = compatibilityMap[i];
        const char *zone2 = compatibilityMap[i+1];

        TimeZone *tz1 = TimeZone::createTimeZone(zone1);
        TimeZone *tz2 = TimeZone::createTimeZone(zone2);

        // ok what did we get?

        // test #1
        {
            UnicodeString id(zone1);

            if(!tz1)
            {
                errln("FAIL: Could not find left hand zone " + id);
            }
            else
            {
                tz1->getID(itsID);
                int ioffset = tz1->getRawOffset()/60000;
                UnicodeString offset;
                formatMinutes(ioffset, offset);
                logln(id + " -> " + itsID + " GMT" + offset + " (left hand)");
            }
        }

        // test #2
        {
            UnicodeString id(zone2);

            if(!tz2)
            {
                errln("FAIL: Could not find right hand zone " + id + " (right hand)");
            }
            else
            {
                tz2->getID(itsID);
                int ioffset = tz2->getRawOffset()/60000;
                UnicodeString offset;
                formatMinutes(ioffset, offset);
                logln(id + " -> " + itsID + " GMT" + offset +" (right hand)");
            }
        }

        if(tz1&&tz2)
        {
            tz2->setID(tz1->getID(itsID)); // make the NAME the same so that comparison will only look at the rest of it.

            if(*tz1 != *tz2)
            {
                errln(" FAIL: time zones " + UnicodeString(zone1) + " and " + UnicodeString(zone2) + " do NOT match.");
            }
        }

        delete tz1;
        delete tz2;
    }

}

/**
 * Utility function for TestCustomParse
 */
UnicodeString& TimeZoneTest::formatMinutes(int32_t min, UnicodeString& rv)
{
        rv.remove();

        char sign = '+';
        if (min < 0) { sign = '-'; min = -min; }
        int h = min/60;
        min = min%60;

        rv += UChar(sign);
        if(h > 10)
            rv += UChar('0' + (h/10));
        rv += UChar('0' + (h%10));

        rv += ":";

        if(min > 10)
            rv += UChar('0' + (min/10));
        else
            rv += "0";

        rv += UChar('0' + (min%10));

        return rv;
}


/**
 * As part of the VM fix (see CCC approved RFE 4028006, bug
 * 4044013), TimeZone.getTimeZone() has been modified to recognize
 * generic IDs of the form GMT[+-]hh:mm, GMT[+-]hhmm, and
 * GMT[+-]hh.  Test this behavior here.
 *
 * @bug 4044013
 */
void TimeZoneTest::TestCustomParse()
{
    int32_t i;
    const int32_t kUnparseable = 604800; // the number of seconds in a week. More than any offset should be.
    const UnicodeString kExpectedCustomID = "Custom";

    struct
    {
        char customId[20];
        int32_t expectedOffset;
    }
    kData[] =
    {
        // ID        Expected offset in minutes
        //"GMT",       kUnparseable,   Isn't custom. Can't test it here. [returns normal GMT]
        "GMT-YOUR.AD.HERE",     kUnparseable,
        "GMT0",      kUnparseable,
        "GMT+0",     (0),
        "GMT+1",     (60),
        "GMT-0030",  (-30),
        "GMT+15:99", (15*60+99),
        "GMT+",      kUnparseable,
        "GMT-",      kUnparseable,
        "GMT+0:",    kUnparseable,
        "GMT-:",     kUnparseable,
        "GMT-YOUR.AD.HERE",     kUnparseable,
        "GMT+0010",  (10), // Interpret this as 00:10
        "GMT-10",    (-10*60),
        "GMT+30",    (30),
        "GMT-3:30",  (-(3*60+30)),
        "GMT-230",   (-(2*60+30)),
        0,           0
    };

    for (i=0; kData[i].customId[0] != '\0'; i++)
    {
        UnicodeString id(kData[i].customId);
        int32_t exp = kData[i].expectedOffset;

        TimeZone *zone = TimeZone::createTimeZone(id);
        UnicodeString   itsID, temp;

        logln();
        logln("testing # " + formatMinutes(i, temp) + id);

        /*
        if(zone == NULL)
        {
            errln("FAIL: Could not createTimeZone(" + id + "). Returned NULL.");
            continue;
        }
        */


        if (! zone->getID(itsID).compare("GMT"))
        //if(zone == NULL)
        {
            logln(id + " -> generic GMT");
            // When TimeZone.getTimeZone() can't parse the id, it
            // returns GMT -- a dubious practice, but required for
            // backward compatibility.
            if (exp != kUnparseable) {
                errln("FAIL: Expected offset of " + formatMinutes(exp,temp) +
                                    " for " + id + ", got parse failure");
            }
        }
        else
        {
            zone->getID(itsID);
            int32_t ioffset = zone->getRawOffset()/60000;
            UnicodeString offset;
            formatMinutes(ioffset, offset);
            logln(id + " -> " + itsID + " GMT" + offset);
            if (exp == kUnparseable)
            {
                errln("FAIL: Expected parse failure for " + id +
                                    ", got offset of " + offset +
                                    ", id " + itsID);
            }
            else if (ioffset != exp ||
                     (itsID.compare(kExpectedCustomID) != 0))
            {
                errln("Expected offset of " + formatMinutes(exp,temp) +
                                    ", id Custom, for " + id +
                                    ", got offset of " + offset +
                                    ", id " + itsID);
            }
        }
        delete zone;
    }
}

/**
 * Test the basic functionality of the getDisplayName() API.
 *
 * @bug 4112869
 * @bug 4028006
 *
 * See also API change request A41.
 *
 * 4/21/98 - make smarter, so the test works if the ext resources
 * are present or not.
 */
void
TimeZoneTest::TestDisplayName()
{
    UErrorCode status = ZERO_ERROR;
    int32_t i;
    TimeZone *zone = TimeZone::createTimeZone("PST");
    UnicodeString name;
    zone->getDisplayName(Locale::ENGLISH, name);
    logln("PST->" + name);
    if (name.compare("Pacific Standard Time") != 0)
        errln("Fail: Expected \"Pacific Standard Time\" but got " + name);

    //*****************************************************************
    // THE FOLLOWING LINES MUST BE UPDATED IF THE LOCALE DATA CHANGES
    // THE FOLLOWING LINES MUST BE UPDATED IF THE LOCALE DATA CHANGES
    // THE FOLLOWING LINES MUST BE UPDATED IF THE LOCALE DATA CHANGES
    //*****************************************************************
    struct
    {
        bool_t useDst;
        TimeZone::EDisplayType style;
        char expect[100];
    } kData[] = {
        FALSE, TimeZone::SHORT, "PST",
        TRUE,  TimeZone::SHORT, "PDT",
        FALSE, TimeZone::LONG,  "Pacific Standard Time",
        TRUE,  TimeZone::LONG,  "Pacific Daylight Time",

        FALSE, TimeZone::LONG, ""
    };

    for (i=0; kData[i].expect[0] != '\0'; i++)
    {
        name.remove();
        name = zone->getDisplayName(kData[i].useDst,
                                   kData[i].style,
                                   Locale::ENGLISH, name);
        if (name.compare(kData[i].expect) != 0)
            errln("Fail: Expected " + UnicodeString(kData[i].expect) + "; got " + name);
        logln("PST [with options]->" + name);
    }

    // Make sure that we don't display the DST name by constructing a fake
    // PST zone that has DST all year long.
    SimpleTimeZone *zone2 = new SimpleTimeZone(0, "PST");

    zone2->setStartRule(Calendar::JANUARY, 1, 0, 0, status);
    zone2->setEndRule(Calendar::DECEMBER, 31, 0, 0, status);

    UnicodeString inDaylight = (zone2->inDaylightTime(UDate(), status)? UnicodeString("TRUE"):UnicodeString("FALSE"));
    logln(UnicodeString("Modified PST inDaylightTime->") + inDaylight );
    if(FAILURE(status))
    {
        errln("Some sort of error..."); // REVISIT
    }
    name.remove();
    name = zone2->getDisplayName(Locale::ENGLISH,name);
    logln("Modified PST->" + name);
    if (name.compare("Pacific Standard Time") != 0)
        errln("Fail: Expected \"Pacific Standard Time\"");

    // Make sure we get the default display format for Locales
    // with no display name data.
    Locale zh_CN = Locale::SIMPLIFIED_CHINESE;
    name.remove();
    name = zone->getDisplayName(zh_CN,name);
    //*****************************************************************
    // THE FOLLOWING LINE MUST BE UPDATED IF THE LOCALE DATA CHANGES
    // THE FOLLOWING LINE MUST BE UPDATED IF THE LOCALE DATA CHANGES
    // THE FOLLOWING LINE MUST BE UPDATED IF THE LOCALE DATA CHANGES
    //*****************************************************************
    logln("PST(zh_CN)->" + name);

    // *** REVISIT SRL how in the world do I check this? looks java specific.
    // Now be smart -- check to see if zh resource is even present.
    // If not, we expect the en fallback behavior.
    ResourceBundle enRB(Locale::getDataDirectory(),
                            Locale::ENGLISH, status);
    if(FAILURE(status))
        errln("Couldn't get ResourceBundle for en");

    ResourceBundle zhRB(Locale::getDataDirectory(),
                         zh_CN, status);
    //if(FAILURE(status))
    //    errln("Couldn't get ResourceBundle for zh_CN");

    bool_t noZH = FAILURE(status);

    if (noZH) {
        logln("Warning: Not testing the zh_CN behavior because resource is absent");
        if (name != "Pacific Standard Time")
            errln("Fail: Expected Pacific Standard Time");
    }


    if      (name.compare("GMT-08:00") &&
             name.compare("GMT-8:00") &&
             name.compare("GMT-0800") &&
             name.compare("GMT-800")) {
        errln("Fail: Expected GMT-08:00 or something similar");
        errln("************************************************************");
        errln("THE ABOVE FAILURE MAY JUST MEAN THE LOCALE DATA HAS CHANGED");
        errln("************************************************************");
    }

    // Now try a non-existent zone
    delete zone2;
    zone2 = new SimpleTimeZone(90*60*1000, "xyzzy");
    name.remove();
    name = zone2->getDisplayName(Locale::ENGLISH,name);
    logln("GMT+90min->" + name);
    if (name.compare("GMT+01:30") &&
        name.compare("GMT+1:30") &&
        name.compare("GMT+0130") &&
        name.compare("GMT+130"))
        errln("Fail: Expected GMT+01:30 or something similar");

    // clean up
    delete zone;
    delete zone2;
}

/**
 * @bug 4107276
 */
void
TimeZoneTest::TestDSTSavings()
{
    UErrorCode status = ZERO_ERROR;
    // It might be better to find a way to integrate this test into the main TimeZone
    // tests above, but I don't have time to figure out how to do this (or if it's
    // even really a good idea).  Let's consider that a future.  --rtg 1/27/98
    SimpleTimeZone *tz = new SimpleTimeZone(-5 * kMillisPerHour, "dstSavingsTest",
                                           Calendar::MARCH, 1, 0, 0, Calendar::SEPTEMBER, 1, 0, 0,
                                           (int32_t)(0.5 * kMillisPerHour), status);
    if(FAILURE(status))
        errln("couldn't create TimeZone");

    if (tz->getRawOffset() != -5 * kMillisPerHour)
        errln(UnicodeString("Got back a raw offset of ") + (tz->getRawOffset() / kMillisPerHour) +
              " hours instead of -5 hours.");
    if (!tz->useDaylightTime())
        errln("Test time zone should use DST but claims it doesn't.");
    if (tz->getDSTSavings() != 0.5 * kMillisPerHour)
        errln(UnicodeString("Set DST offset to 0.5 hour, but got back ") + (tz->getDSTSavings() /
                                                             kMillisPerHour) + " hours instead.");

    int32_t offset = tz->getOffset(GregorianCalendar::AD, 1998, Calendar::JANUARY, 1,
                              Calendar::THURSDAY, 10 * kMillisPerHour);
    if (offset != -5 * kMillisPerHour)
        errln(UnicodeString("The offset for 10 AM, 1/1/98 should have been -5 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    offset = tz->getOffset(GregorianCalendar::AD, 1998, Calendar::JUNE, 1, Calendar::MONDAY,
                          10 * kMillisPerHour);
    if (offset != -4.5 * kMillisPerHour)
        errln(UnicodeString("The offset for 10 AM, 6/1/98 should have been -4.5 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    tz->setDSTSavings(kMillisPerHour);
    offset = tz->getOffset(GregorianCalendar::AD, 1998, Calendar::JANUARY, 1,
                          Calendar::THURSDAY, 10 * kMillisPerHour);
    if (offset != -5 * kMillisPerHour)
        errln(UnicodeString("The offset for 10 AM, 1/1/98 should have been -5 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    offset = tz->getOffset(GregorianCalendar::AD, 1998, Calendar::JUNE, 1, Calendar::MONDAY,
                          10 * kMillisPerHour);
    if (offset != -4 * kMillisPerHour)
        errln(UnicodeString("The offset for 10 AM, 6/1/98 (with a 1-hour DST offset) should have been -4 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    delete tz;
}

/**
 * @bug 4107570
 */
void
TimeZoneTest::TestAlternateRules()
{
    // Like TestDSTSavings, this test should probably be integrated somehow with the main
    // test at the top of this class, but I didn't have time to figure out how to do that.
    //                      --rtg 1/28/98

    SimpleTimeZone tz(-5 * kMillisPerHour, "alternateRuleTest");

    // test the day-of-month API
    UErrorCode status = ZERO_ERROR;
    tz.setStartRule(Calendar::MARCH, 10, 12 * kMillisPerHour, status);
    if(FAILURE(status))
        errln("tz.setStartRule failed");
    tz.setEndRule(Calendar::OCTOBER, 20, 12 * kMillisPerHour, status);
    if(FAILURE(status))
        errln("tz.setStartRule failed");

    int32_t offset = tz.getOffset(GregorianCalendar::AD, 1998, Calendar::MARCH, 5,
                              Calendar::THURSDAY, 10 * kMillisPerHour);
    if (offset != -5 * kMillisPerHour)
        errln(UnicodeString("The offset for 10AM, 3/5/98 should have been -5 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    offset = tz.getOffset(GregorianCalendar::AD, 1998, Calendar::MARCH, 15,
                          Calendar::SUNDAY, 10 * millisPerHour);
    if (offset != -4 * kMillisPerHour)
        errln(UnicodeString("The offset for 10AM, 3/15/98 should have been -4 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    offset = tz.getOffset(GregorianCalendar::AD, 1998, Calendar::OCTOBER, 15,
                          Calendar::THURSDAY, 10 * millisPerHour);
    if (offset != -4 * kMillisPerHour)
        errln(UnicodeString("The offset for 10AM, 10/15/98 should have been -4 hours, but we got ")              + (offset / kMillisPerHour) + " hours.");

    offset = tz.getOffset(GregorianCalendar::AD, 1998, Calendar::OCTOBER, 25,
                          Calendar::SUNDAY, 10 * millisPerHour);
    if (offset != -5 * kMillisPerHour)
        errln(UnicodeString("The offset for 10AM, 10/25/98 should have been -5 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    // test the day-of-week-after-day-in-month API
    tz.setStartRule(Calendar::MARCH, 10, Calendar::FRIDAY, 12 * millisPerHour, TRUE, status);
    if(FAILURE(status))
        errln("tz.setStartRule failed");
    tz.setEndRule(Calendar::OCTOBER, 20, Calendar::FRIDAY, 12 * millisPerHour, FALSE, status);
    if(FAILURE(status))
        errln("tz.setStartRule failed");

    offset = tz.getOffset(GregorianCalendar::AD, 1998, Calendar::MARCH, 11,
                          Calendar::WEDNESDAY, 10 * millisPerHour);
    if (offset != -5 * kMillisPerHour)
        errln(UnicodeString("The offset for 10AM, 3/11/98 should have been -5 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    offset = tz.getOffset(GregorianCalendar::AD, 1998, Calendar::MARCH, 14,
                          Calendar::SATURDAY, 10 * millisPerHour);
    if (offset != -4 * kMillisPerHour)
        errln(UnicodeString("The offset for 10AM, 3/14/98 should have been -4 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    offset = tz.getOffset(GregorianCalendar::AD, 1998, Calendar::OCTOBER, 15,
                          Calendar::THURSDAY, 10 * millisPerHour);
    if (offset != -4 * kMillisPerHour)
        errln(UnicodeString("The offset for 10AM, 10/15/98 should have been -4 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");

    offset = tz.getOffset(GregorianCalendar::AD, 1998, Calendar::OCTOBER, 17,
                          Calendar::SATURDAY, 10 * millisPerHour);
    if (offset != -5 * kMillisPerHour)
        errln(UnicodeString("The offset for 10AM, 10/17/98 should have been -5 hours, but we got ")
              + (offset / kMillisPerHour) + " hours.");
}
