
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

#include "callimts.h"
#include "calendar.h"
#include "gregocal.h"
#include "datefmt.h"
#include "smpdtfmt.h"
#include <float.h>
#include <math.h>

void CalendarLimitTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite TestCalendarLimit");
    switch (index) {
        // Re-enable this later
        case 0:
            name = "TestCalendarLimit";
            if (exec) {
                logln("TestCalendarLimit---"); logln("");
                TestCalendarLimit();
            }
            break;
        default: name = ""; break;
    }
}


// *****************************************************************************
// class CalendarLimitTest
// *****************************************************************************
 
// this is 2^52 - 1, the largest allowable mantissa with a 0 exponent in a 64-bit double
const UDate CalendarLimitTest::EARLIEST_SUPPORTED_MILLIS = - 4503599627370495.0;
const UDate CalendarLimitTest::LATEST_SUPPORTED_MILLIS    =   4503599627370495.0;
 
// -------------------------------------
 
void
CalendarLimitTest::test(UDate millis, Calendar* cal, DateFormat* fmt)
{
    UErrorCode exception = U_ZERO_ERROR;
    UnicodeString theDate;
    UErrorCode status = U_ZERO_ERROR;
    UDate d = millis;
    cal->setTime(millis, exception);
    if (U_SUCCESS(exception)) {
        fmt->format(millis, theDate);
        UDate dt = fmt->parse(theDate, status);
        // allow a small amount of error (drift)
        if(! withinErr(dt, millis, 1e-10))
            errln(UnicodeString("FAIL:round trip for large milli, got: ") + dt + " wanted: " + millis);
        else {
            logln(UnicodeString("OK: got ") + dt + ", wanted " + millis);
            logln(UnicodeString("    ") + theDate);
        }
    }        
}
 
// -------------------------------------

double
CalendarLimitTest::nextDouble(double a)
{
    return icu_nextDouble(a, TRUE);
}

double
CalendarLimitTest::previousDouble(double a)
{
    return icu_nextDouble(a, FALSE);
}

bool_t
CalendarLimitTest::withinErr(double a, double b, double err)
{
    return ( icu_fabs(a - b) < icu_fabs(a * err) ); 
}

void
CalendarLimitTest::TestCalendarLimit()
{
    logln("Limit tests");
    logln("--------------------");
    UErrorCode status = U_ZERO_ERROR;
    explore2(EARLIEST_SUPPORTED_MILLIS);
    explore3(LATEST_SUPPORTED_MILLIS);
    Calendar *cal = Calendar::createInstance(status);
    if (failure(status, "Calendar::createInstance")) return;
    cal->adoptTimeZone(TimeZone::createTimeZone("Africa/Casablanca"));
    DateFormat *fmt = DateFormat::createDateTimeInstance();
    fmt->adoptCalendar(cal);
    ((SimpleDateFormat*) fmt)->applyPattern("HH:mm:ss.SSS zzz, EEEE, MMMM d, yyyy G");

    logln("");
    logln("Round trip tests");
    logln("--------------------");
    // We happen to know that the upper failure point is between
    // 1e17 and 1e18.
    UDate m;
    for ( m = 1e17; m < 1e18; m *= 1.1)
    {
        test(m, cal, fmt);
    }
    for ( m = -1e14; m > -1e15; m *= 1.1) {
        test(m, cal, fmt);
    }

    test(EARLIEST_SUPPORTED_MILLIS, cal, fmt);
    test(previousDouble(EARLIEST_SUPPORTED_MILLIS), cal, fmt);
    delete fmt;
}
 
// -------------------------------------

void
CalendarLimitTest::explore2(UDate expectedEarlyLimit)
{
    UDate millis = - 1;
    int32_t* fields = new int32_t[3];
    while (timeToFields(millis, fields)) 
        millis *= 2;
    UDate bad = millis;
    UDate good = millis / 2;
    UDate mid;
    while ( ! withinErr(good, bad, 1e-15) ) { 
        mid = (good + bad) / 2;
        if (timeToFields(mid, fields)) 
            good = mid;
        else 
            bad = mid;
    }
    timeToFields(good, fields);
    logln(UnicodeString(UnicodeString("Good: "))  + good + " " + fields[0] + "/" + fields[1] + "/" + fields[2]);
    timeToFields(bad, fields);
    logln(UnicodeString(UnicodeString("Bad:  "))  + bad + " " + fields[0] + "/" + fields[1] + "/" + fields[2]);
    if (good <= expectedEarlyLimit) {
        logln("PASS: Limit <= expected.");
    }
    else 
        errln(UnicodeString("FAIL: Expected limit ") + expectedEarlyLimit + "; got " + good);
    delete[] fields;
}

void
CalendarLimitTest::explore3(UDate expectedLateLimit)
{
    UDate millis = 1;
    int32_t* fields = new int32_t[3];
    int32_t oldYear = -1;
    int32_t newYear = -1;
    while (TRUE) {
        if(! timeToFields(millis, fields))
            break;
        newYear = fields[0];
        if(newYear < oldYear)
            break;
        oldYear = newYear;
        millis *= 2;
    }

    // narrow the range a little
    oldYear = -1;
    newYear = -1;
    millis /= 2;
    while (TRUE) {
        if(! timeToFields(millis, fields))
            break;
        newYear = fields[0];
        if(newYear < oldYear)
            break;
        oldYear = newYear;
        millis *= 1.01;
    }

    // this isn't strictly accurate, but we are only trying to verify that
    // the Calendar breaks AFTER the latest date it is promised to work with
    UDate good = millis / 1.01;
    UDate bad = millis;

    timeToFields(good, fields);
    logln(UnicodeString(UnicodeString("Good:  "))  + good + " " + fields[0] + "/" + fields[1] + "/" + fields[2]);
    timeToFields(bad, fields);
    logln(UnicodeString(UnicodeString("Bad:   "))  + bad + " " + fields[0] + "/" + fields[1] + "/" + fields[2]);
    if (good >= expectedLateLimit) {
        logln("PASS: Limit >= expected.");
    }
    else 
        errln(UnicodeString("FAIL: Expected limit ") + expectedLateLimit + "; got " + good);

    delete[] fields;
}
 
UDate CalendarLimitTest::gregorianCutover = - 12219292800000.0;
 
// -------------------------------------

const int32_t CalendarLimitTest::kEpochStartAsJulianDay    = 2440588; // January 1, 1970 (Gregorian)

double
CalendarLimitTest::millisToJulianDay(UDate millis)
{
    return (double)kEpochStartAsJulianDay + floorDivide(millis, (double)millisPerDay);
}

 
int32_t CalendarLimitTest::julianDayOffset = 2440588;
 
int32_t CalendarLimitTest::millisPerDay = 24 * 60 * 60 * 1000;
 
int32_t CalendarLimitTest::YEAR = 0;
 
int32_t CalendarLimitTest::MONTH = 1;
 
int32_t CalendarLimitTest::DATE = 2;
 
// -------------------------------------
 
double
CalendarLimitTest::floorDivide(double numerator, double denominator) 
{
    // We do this computation in order to handle
    // a numerator of Long.MIN_VALUE correctly
    return icu_floor(numerator / denominator);
    /*
    return (numerator >= 0) ?
        icu_trunc(numerator / denominator) :
        icu_trunc((numerator + 1) / denominator) - 1;
*/
}

// -------------------------------------

int32_t 
CalendarLimitTest::floorDivide(int32_t numerator, int32_t denominator) 
{
    // We do this computation in order to handle
    // a numerator of Long.MIN_VALUE correctly
    return (numerator >= 0) ?
        numerator / denominator :
        ((numerator + 1) / denominator) - 1;
}

// -------------------------------------

int32_t 
CalendarLimitTest::floorDivide(int32_t numerator, int32_t denominator, int32_t remainder[])
{
    if (numerator >= 0) {
        remainder[0] = numerator % denominator;
        return numerator / denominator;
    }
    int32_t quotient = ((numerator + 1) / denominator) - 1;
    remainder[0] = numerator - (quotient * denominator);
    return quotient;
}

// -------------------------------------

int32_t
CalendarLimitTest::floorDivide(double numerator, int32_t denominator, int32_t remainder[]) 
{
    if (numerator >= 0) {
        remainder[0] = (int32_t)icu_fmod(numerator, denominator);
        return (int32_t)icu_trunc(numerator / denominator);
    }
    int32_t quotient = (int32_t)(icu_trunc((numerator + 1) / denominator) - 1);
    remainder[0] = (int32_t)(numerator - (quotient * denominator));
    return quotient;
}

// -------------------------------------

const UDate CalendarLimitTest::kPapalCutover = 
    (2299161.0 - kEpochStartAsJulianDay) * (double)millisPerDay;

const int32_t CalendarLimitTest::kJan1_1JulianDay = 1721426; // January 1, year 1 (Gregorian)

const int32_t CalendarLimitTest::kNumDays[]
    = {0,31,59,90,120,151,181,212,243,273,304,334}; // 0-based, for day-in-year
const int32_t CalendarLimitTest::kLeapNumDays[]
    = {0,31,60,91,121,152,182,213,244,274,305,335}; // 0-based, for day-in-year
const int32_t CalendarLimitTest::kMonthLength[]
    = {31,28,31,30,31,30,31,31,30,31,30,31}; // 0-based
const int32_t CalendarLimitTest::kLeapMonthLength[]
    = {31,29,31,30,31,30,31,31,30,31,30,31}; // 0-based

bool_t
CalendarLimitTest::timeToFields(UDate theTime, int32_t* fields)
{
    if(icu_isInfinite(theTime))
        return FALSE;

    int32_t rawYear;
    int32_t year, month, date, dayOfWeek, dayOfYear, era;
    bool_t isLeap;

    // Compute the year, month, and day of month from the given millis
    // {sfb} for simplicity's sake, assume no one will change the cutover date
    if (theTime >= kPapalCutover/*fNormalizedGregorianCutover*/) {
        // The Gregorian epoch day is zero for Monday January 1, year 1.
        double gregorianEpochDay = millisToJulianDay(theTime) - kJan1_1JulianDay;
        // Here we convert from the day number to the multiple radix
        // representation.  We use 400-year, 100-year, and 4-year cycles.
        // For example, the 4-year cycle has 4 years + 1 leap day; giving
        // 1461 == 365*4 + 1 days.
        int32_t rem[1];
        int32_t n400 = floorDivide(gregorianEpochDay, 146097, rem); // 400-year cycle length
        int32_t n100 = floorDivide(rem[0], 36524, rem); // 100-year cycle length
        int32_t n4 = floorDivide(rem[0], 1461, rem); // 4-year cycle length
        int32_t n1 = floorDivide(rem[0], 365, rem);
        rawYear = 400*n400 + 100*n100 + 4*n4 + n1;
        dayOfYear = rem[0]; // zero-based day of year
        if (n100 == 4 || n1 == 4) 
            dayOfYear = 365; // Dec 31 at end of 4- or 400-yr cycle
        else 
            ++rawYear;
        
        isLeap = ((rawYear&0x3) == 0) && // equiv. to (rawYear%4 == 0)
            (rawYear%100 != 0 || rawYear%400 == 0);
        
        // Gregorian day zero is a Monday
        dayOfWeek = (int32_t)icu_fmod(gregorianEpochDay + 1, 7);
    }
    else {
        // The Julian epoch day (not the same as Julian Day)
        // is zero on Saturday December 30, 0 (Gregorian).
        double julianEpochDay = millisToJulianDay(theTime) - (kJan1_1JulianDay - 2);
        //rawYear = floorDivide(4 * julianEpochDay + 1464, 1461);
            rawYear = (int32_t) floorDivide(4*julianEpochDay + 1464, 1461.0);
        
        // Compute the Julian calendar day number for January 1, rawYear
        //double january1 = 365 * (rawYear - 1) + floorDivide(rawYear - 1, 4);
        double january1 = 365 * (rawYear - 1) + floorDivide(rawYear - 1, 4L);
        dayOfYear = (int32_t)(julianEpochDay - january1); // 0-based
        
        // Julian leap years occurred historically every 4 years starting
        // with 8 AD.  Before 8 AD the spacing is irregular; every 3 years
        // from 45 BC to 9 BC, and then none until 8 AD.  However, we don't
        // implement this historical detail; instead, we implement the
        // computatinally cleaner proleptic calendar, which assumes
        // consistent 4-year cycles throughout time.
        isLeap = ((rawYear & 0x3) == 0); // equiv. to (rawYear%4 == 0)
        
        // Julian calendar day zero is a Saturday
        dayOfWeek = (int32_t)icu_fmod(julianEpochDay-1, 7);
    }
    
    // Common Julian/Gregorian calculation
    int32_t correction = 0;
    int32_t march1 = isLeap ? 60 : 59; // zero-based DOY for March 1
    if (dayOfYear >= march1) 
        correction = isLeap ? 1 : 2;
    month = (12 * (dayOfYear + correction) + 6) / 367; // zero-based month
    date = dayOfYear -
        (isLeap ? kLeapNumDays[month] : kNumDays[month]) + 1; // one-based DOM
    
    // Normalize day of week
    dayOfWeek += (dayOfWeek < 0) ? (Calendar::SUNDAY+7) : Calendar::SUNDAY;

    era = GregorianCalendar::AD;
    year = rawYear;
    if (year < 1) {
        era = GregorianCalendar::BC;
        year = 1 - year;
    }

    //internalSet(ERA, era);
    //internalSet(YEAR, year);
    //internalSet(MONTH, month + JANUARY); // 0-based
    //internalSet(DATE, date);
    //internalSet(DAY_OF_WEEK, dayOfWeek);
    //internalSet(DAY_OF_YEAR, ++dayOfYear); // Convert from 0-based to 1-based
    
    fields[YEAR] = year;
    month += Calendar::JANUARY;
    fields[MONTH] = month;
    fields[DATE] = date;
    // month: 0 <= m <= 11
    bool_t monthLegal = (    (month - Calendar::JANUARY) >= 0 &&
                            (month - Calendar::JANUARY) <= 11 );

    bool_t dateLegal = (    date >= 1 && 
                            date <= (isLeap ? kLeapMonthLength[month - Calendar::JANUARY] 
                                            : kMonthLength[month - Calendar::JANUARY]));
    
    bool_t yearLegal = (year >= 0);
    
    return monthLegal && dateLegal && yearLegal;
}

// eof
