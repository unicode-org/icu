/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File GREGOCAL.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/05/97    clhuang     Creation.
*   03/28/97    aliu        Made highly questionable fix to computeFields to
*                           handle DST correctly.
*   04/22/97    aliu        Cleaned up code drastically.  Added monthLength().
*                           Finished unimplemented parts of computeTime() for
*                           week-based date determination.  Removed quetionable
*                           fix and wrote correct fix for computeFields() and
*                           daylight time handling.  Rewrote inDaylightTime()
*                           and computeFields() to handle sensitive Daylight to
*                           Standard time transitions correctly.
*   05/08/97    aliu        Added code review changes.  Fixed isLeapYear() to
*                           not cutover.
*   08/12/97    aliu        Added equivalentTo.  Misc other fixes.  Updated
*                           add() from Java source.
*    07/28/98    stephen        Sync up with JDK 1.2
*    09/14/98    stephen        Changed type of kOneDay, kOneWeek to double.
*                            Fixed bug in roll() 
*   10/15/99    aliu        Fixed j31, incorrect WEEK_OF_YEAR computation.
*   10/15/99    aliu        Fixed j32, cannot set date to Feb 29 2000 AD.
*                           {JDK bug 4210209 4209272}
*   11/15/99    weiv        Added YEAR_WOY and DOW_LOCAL computation
*                           to timeToFields method, updated kMinValues, kMaxValues & kLeastMaxValues
*   12/09/99    aliu        Fixed j81, calculation errors and roll bugs
*                           in year of cutover.
********************************************************************************
*/

#ifndef _GREGOCAL
#include "unicode/gregocal.h"
#endif

// *****************************************************************************
// class GregorianCalendar
// *****************************************************************************


const int32_t GregorianCalendar::kJan1_1JulianDay = 1721426; // January 1, year 1 (Gregorian)

/**
 * Note that the Julian date used here is not a true Julian date, since
 * it is measured from midnight, not noon.  This value is the Julian
 * day number of January 1, 1970 (Gregorian calendar) at noon UTC. [LIU]
 */
const int32_t GregorianCalendar::kEpochStartAsJulianDay    = 2440588; // January 1, 1970 (Gregorian)

const int32_t GregorianCalendar::kEpochYear             = 1970;

const int32_t GregorianCalendar::kNumDays[]
    = {0,31,59,90,120,151,181,212,243,273,304,334}; // 0-based, for day-in-year
const int32_t GregorianCalendar::kLeapNumDays[]
    = {0,31,60,91,121,152,182,213,244,274,305,335}; // 0-based, for day-in-year
const int32_t GregorianCalendar::kMonthLength[]
    = {31,28,31,30,31,30,31,31,30,31,30,31}; // 0-based
const int32_t GregorianCalendar::kLeapMonthLength[]
    = {31,29,31,30,31,30,31,31,30,31,30,31}; // 0-based

// Useful millisecond constants
const int32_t GregorianCalendar::kOneSecond = 1000;
const int32_t GregorianCalendar::kOneMinute = 60   * kOneSecond;     //      60,000
const int32_t GregorianCalendar::kOneHour   = 60   * kOneMinute;    //   3,600,000
const double  GregorianCalendar::kOneDay    = 24.0 * kOneHour;        //  86,400,000
const double  GregorianCalendar::kOneWeek   = 7.0  * kOneDay;        // 604,800,000

// These numbers are 2^52 - 1, the largest allowable mantissa in a 64-bit double
// with a 0 exponent.  These are the absolute largest numbers for millis that
// this calendar will handle reliably.  It will work for larger values, however.
// The problem is that, once the exponent is not 0, the calendar will jump.
// When translated into a year, LATEST_SUPPORTED_MILLIS corresponds to 144,683 AD 
// and EARLIEST_SUPPORTED_MILLIS corresponds to 140,742 BC
const UDate GregorianCalendar::EARLIEST_SUPPORTED_MILLIS = - 4503599627370495.0;
const UDate GregorianCalendar::LATEST_SUPPORTED_MILLIS    =   4503599627370495.0;

/*
 * <pre>
 *                            Greatest       Least 
 * Field name        Minimum   Minimum     Maximum     Maximum
 * ----------        -------   -------     -------     -------
 * ERA                     0         0           1           1
 * YEAR                    1         1      140742      144683
 * MONTH                   0         0          11          11
 * WEEK_OF_YEAR            1         1          52          53
 * WEEK_OF_MONTH           0         0           4           6
 * DAY_OF_MONTH            1         1          28          31
 * DAY_OF_YEAR             1         1         365         366
 * DAY_OF_WEEK             1         1           7           7
 * DAY_OF_WEEK_IN_MONTH   -1        -1           4           6
 * AM_PM                   0         0           1           1
 * HOUR                    0         0          11          11
 * HOUR_OF_DAY             0         0          23          23
 * MINUTE                  0         0          59          59
 * SECOND                  0         0          59          59
 * MILLISECOND             0         0         999         999
 * ZONE_OFFSET           -12*      -12*         12*         12*
 * DST_OFFSET              0         0           1*          1*
 * YEAR_WOY                1         1      140742      144683
 * DOW_LOCAL               1         1           7           7
 * </pre>
 * (*) In units of one-hour
 */
const int32_t GregorianCalendar::kMinValues[] = {
    0,1,0,1,0,1,1,1,-1,0,0,0,0,0,0,-12*U_MILLIS_PER_HOUR,0,1,1
};
const int32_t GregorianCalendar::kLeastMaxValues[] = {
    1,140742,11,52,4,28,365,7,4,1,11,23,59,59,999,12*U_MILLIS_PER_HOUR,1*U_MILLIS_PER_HOUR,140742,7
};
const int32_t GregorianCalendar::kMaxValues[] = {
    1,144683,11,53,6,31,366,7,6,1,11,23,59,59,999,12*U_MILLIS_PER_HOUR,1*U_MILLIS_PER_HOUR, 144683,7
};

char GregorianCalendar::fgClassID = 0; // Value is irrelevant

// 00:00:00 UTC, October 15, 1582, expressed in ms from the epoch.
// Note that only Italy and other Catholic countries actually
// observed this cutover.  Most other countries followed in
// the next few centuries, some as late as 1928. [LIU]
// in Java, -12219292800000L
//const UDate GregorianCalendar::kPapalCutover = -12219292800000L;
const UDate GregorianCalendar::kPapalCutover = (2299161.0 - kEpochStartAsJulianDay) * kOneDay;

// -------------------------------------

GregorianCalendar::GregorianCalendar(UErrorCode& status)
    :   Calendar(TimeZone::createDefault(), Locale::getDefault(), status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    setTimeInMillis(getNow(), status);
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(TimeZone* zone, UErrorCode& status)
    :   Calendar(zone, Locale::getDefault(), status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    setTimeInMillis(getNow(), status);
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(const TimeZone& zone, UErrorCode& status)
    :   Calendar(zone, Locale::getDefault(), status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    setTimeInMillis(getNow(), status);
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(const Locale& aLocale, UErrorCode& status)
    :   Calendar(TimeZone::createDefault(), aLocale, status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    setTimeInMillis(getNow(), status);
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(TimeZone* zone, const Locale& aLocale,
                                     UErrorCode& status)
    :   Calendar(zone, aLocale, status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    setTimeInMillis(getNow(), status);
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(const TimeZone& zone, const Locale& aLocale,
                                     UErrorCode& status)
    :   Calendar(zone, aLocale, status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    setTimeInMillis(getNow(), status);
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(int32_t year, int32_t month, int32_t date,
                                     UErrorCode& status)
    :   Calendar(TimeZone::createDefault(), Locale::getDefault(), status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    set(Calendar::ERA, AD);
    set(Calendar::YEAR, year);
    set(Calendar::MONTH, month);
    set(Calendar::DATE, date);
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(int32_t year, int32_t month, int32_t date,
                                     int32_t hour, int32_t minute, UErrorCode& status)
    :   Calendar(TimeZone::createDefault(), Locale::getDefault(), status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    set(Calendar::ERA, AD);
    set(Calendar::YEAR, year);
    set(Calendar::MONTH, month);
    set(Calendar::DATE, date);
    set(Calendar::HOUR_OF_DAY, hour);
    set(Calendar::MINUTE, minute);
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(int32_t year, int32_t month, int32_t date,
                                     int32_t hour, int32_t minute, int32_t second,
                                     UErrorCode& status)
    :   Calendar(TimeZone::createDefault(), Locale::getDefault(), status),
        fGregorianCutover(kPapalCutover),
        fNormalizedGregorianCutover(fGregorianCutover),
        fGregorianCutoverYear(1582)
{
    set(Calendar::ERA, AD);
    set(Calendar::YEAR, year);
    set(Calendar::MONTH, month);
    set(Calendar::DATE, date);
    set(Calendar::HOUR_OF_DAY, hour);
    set(Calendar::MINUTE, minute);
    set(Calendar::SECOND, second);
}

// -------------------------------------

GregorianCalendar::~GregorianCalendar()
{
}

// -------------------------------------

GregorianCalendar::GregorianCalendar(const GregorianCalendar &source)
    :   Calendar(source),
        fGregorianCutover(source.fGregorianCutover),
        fNormalizedGregorianCutover(source.fNormalizedGregorianCutover),
        fGregorianCutoverYear(source.fGregorianCutoverYear)
{
}

// -------------------------------------

Calendar* GregorianCalendar::clone() const
{
    return new GregorianCalendar(*this);
}

// -------------------------------------

GregorianCalendar &
GregorianCalendar::operator=(const GregorianCalendar &right)
{
    if (this != &right)
    {
        Calendar::operator=(right);
        fGregorianCutover = right.fGregorianCutover;
        fNormalizedGregorianCutover = right.fNormalizedGregorianCutover;
        fGregorianCutoverYear = right.fGregorianCutoverYear;
    }
    return *this;
}

// -------------------------------------

bool_t
GregorianCalendar::operator==(const Calendar& that) const
{
    GregorianCalendar* other = (GregorianCalendar*)&that;

    return (this == &that) ||
        (Calendar::operator==(that) &&
         getDynamicClassID() == that.getDynamicClassID() &&
         fGregorianCutover == other->fGregorianCutover);
}

// {sfb} API change?
bool_t GregorianCalendar::equivalentTo(const Calendar& other) const
{
    // Calendar override.
    // Return true if another Calendar object is equivalent to this one.  An equivalent
    // Calendar will behave exactly as this one does, but may be set to a different time.
    return Calendar::equivalentTo(other) &&
        fGregorianCutover == ((GregorianCalendar*)&other)->fGregorianCutover;
}

// -------------------------------------

void
GregorianCalendar::setGregorianChange(UDate date, UErrorCode& status)
{
    if (U_FAILURE(status)) 
        return;

    fGregorianCutover = date;

    // Precompute two internal variables which we use to do the actual
    // cutover computations.  These are the normalized cutover, which is the
    // midnight at or before the cutover, and the cutover year.  The
    // normalized cutover is in pure date milliseconds; it contains no time
    // of day or timezone component, and it used to compare against other
    // pure date values.
    UDate cutoverDay = floorDivide(fGregorianCutover, kOneDay);
    fNormalizedGregorianCutover = cutoverDay * kOneDay;

    // Handle the rare case of numeric overflow.  If the user specifies a
    // change of UDate(Long.MIN_VALUE), in order to get a pure Gregorian
    // calendar, then the epoch day is -106751991168, which when multiplied
    // by ONE_DAY gives 9223372036794351616 -- the negative value is too
    // large for 64 bits, and overflows into a positive value.  We correct
    // this by using the next day, which for all intents is semantically
    // equivalent.
    if (cutoverDay < 0 && fNormalizedGregorianCutover > 0) {
        fNormalizedGregorianCutover = (cutoverDay + 1) * kOneDay;
    }

    // Normalize the year so BC values are represented as 0 and negative
    // values.
    GregorianCalendar *cal = new GregorianCalendar(getTimeZone(), status);
    if(U_FAILURE(status))
        return;
    cal->setTime(date, status);
    fGregorianCutoverYear = cal->get(YEAR, status);
    if (cal->get(ERA, status) == BC) 
        fGregorianCutoverYear = 1 - fGregorianCutoverYear;
    
    delete cal;
}

// -------------------------------------

UDate
GregorianCalendar::getGregorianChange() const
{
    return fGregorianCutover;
}

// -------------------------------------

bool_t 
GregorianCalendar::isLeapYear(int32_t year) const
{
    return (year >= fGregorianCutoverYear ?
        ((year%4 == 0) && ((year%100 != 0) || (year%400 == 0))) : // Gregorian
        (year%4 == 0)); // Julian
}


// -------------------------------------

/**
 * Compute the date-based fields given the milliseconds since the epoch start.
 * Do not compute the time-based fields (HOUR, MINUTE, etc.).
 *
 * @param theTime the given time as LOCAL milliseconds, not UTC.
 */
void
GregorianCalendar::timeToFields(UDate theTime, bool_t quick, UErrorCode& status)
{
    if (U_FAILURE(status)) 
        return;

    int32_t rawYear;
    int32_t year, yearOfWeekOfYear, month, date, dayOfWeek, locDayOfWeek, dayOfYear, era;
    bool_t isLeap;

    // Compute the year, month, and day of month from the given millis
    if (theTime >= fNormalizedGregorianCutover) {
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
        dayOfWeek = (int32_t)uprv_fmod(gregorianEpochDay + 1, 7);
    }
    else {
        // The Julian epoch day (not the same as Julian Day)
        // is zero on Saturday December 30, 0 (Gregorian).
        double julianEpochDay = millisToJulianDay(theTime) - (kJan1_1JulianDay - 2);
        rawYear = (int32_t) floorDivide(4*julianEpochDay + 1464, 1461.0);
        
        // Compute the Julian calendar day number for January 1, rawYear
        double january1 = 365.0 * (rawYear - 1) + floorDivide((double)(rawYear - 1), 4.0);
        dayOfYear = (int32_t)(julianEpochDay - january1); // 0-based
        
        // Julian leap years occurred historically every 4 years starting
        // with 8 AD.  Before 8 AD the spacing is irregular; every 3 years
        // from 45 BC to 9 BC, and then none until 8 AD.  However, we don't
        // implement this historical detail; instead, we implement the
        // computatinally cleaner proleptic calendar, which assumes
        // consistent 4-year cycles throughout time.
        isLeap = ((rawYear & 0x3) == 0); // equiv. to (rawYear%4 == 0)
        
        // Julian calendar day zero is a Saturday
        dayOfWeek = (int32_t)uprv_fmod(julianEpochDay-1, 7);
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
    dayOfWeek += (dayOfWeek < 0) ? (SUNDAY+7) : SUNDAY;
    

    era = AD;
    year = rawYear;
    if (year < 1) {
        era = BC;
        year = 1 - year;
    }

    // Adjust the doy for the cutover year.  Do this AFTER the above
    // computations using doy!  [j81 - aliu]
    if (rawYear == fGregorianCutoverYear &&
        theTime >= fNormalizedGregorianCutover) {
        dayOfYear -= 10;
    }

    // Calculate year of week of year


    internalSet(ERA, era);
    internalSet(YEAR, year);
    internalSet(MONTH, month + JANUARY); // 0-based
    internalSet(DATE, date);
    internalSet(DAY_OF_WEEK, dayOfWeek);
    internalSet(DAY_OF_YEAR, ++dayOfYear); // Convert from 0-based to 1-based
    if (quick) 
        return;

    yearOfWeekOfYear = year;

    // Compute the week of the year.  Valid week numbers run from 1 to 52
    // or 53, depending on the year, the first day of the week, and the
    // minimal days in the first week.  Days at the start of the year may
    // fall into the last week of the previous year; days at the end of
    // the year may fall into the first week of the next year.
    int32_t relDow = (dayOfWeek + 7 - getFirstDayOfWeek()) % 7; // 0..6
    int32_t relDowJan1 = (dayOfWeek - dayOfYear + 701 - getFirstDayOfWeek()) % 7; // 0..6
    int32_t woy = (dayOfYear - 1 + relDowJan1) / 7; // 0..53
    if ((7 - relDowJan1) >= getMinimalDaysInFirstWeek()) {
        ++woy;
        // Check to see if we are in the last week; if so, we need
        // to handle the case in which we are the first week of the
        // next year.
        int32_t lastDoy = yearLength();
        int32_t lastRelDow = (relDow + lastDoy - dayOfYear) % 7;
        if (lastRelDow < 0) lastRelDow += 7;
        if (dayOfYear > 359 && // Fast check which eliminates most cases
            (6 - lastRelDow) >= getMinimalDaysInFirstWeek() &&
            (dayOfYear + 7 - relDow) > lastDoy) {
                woy = 1;
                yearOfWeekOfYear++;
        }
    }
    else if (woy == 0) {
        // We are the last week of the previous year.
        int32_t prevDoy = dayOfYear + yearLength(rawYear - 1);
        woy = weekNumber(prevDoy, dayOfWeek);
        yearOfWeekOfYear--;
    }


    internalSet(WEEK_OF_YEAR, woy);
    internalSet(YEAR_WOY, yearOfWeekOfYear);

    internalSet(WEEK_OF_MONTH, weekNumber(date, dayOfWeek));
    internalSet(DAY_OF_WEEK_IN_MONTH, (date-1) / 7 + 1);

    // Calculate localized day of week
    locDayOfWeek = dayOfWeek-getFirstDayOfWeek()+1;
    locDayOfWeek += (locDayOfWeek<1?7:0);
    internalSet(DOW_LOCAL, locDayOfWeek);
}

// -------------------------------------

/**
 * Return the week number of a day, within a period. This may be the week number in
 * a year, or the week number in a month. Usually this will be a value >= 1, but if
 * some initial days of the period are excluded from week 1, because
 * minimalDaysInFirstWeek is > 1, then the week number will be zero for those
 * initial days. Requires the day of week for the given date in order to determine
 * the day of week of the first day of the period.
 *
 * @param dayOfPeriod  Day-of-year or day-of-month. Should be 1 for first day of period.
 * @param day   Day-of-week for given dayOfPeriod. 1-based with 1=Sunday.
 * @return      Week number, one-based, or zero if the day falls in part of the
 *              month before the first week, when there are days before the first
 *              week because the minimum days in the first week is more than one.
 */
int32_t
GregorianCalendar::weekNumber(int32_t dayOfPeriod, int32_t dayOfWeek)
{
    // Determine the day of the week of the first day of the period
    // in question (either a year or a month).  Zero represents the
    // first day of the week on this calendar.
    int32_t periodStartDayOfWeek = (dayOfWeek - getFirstDayOfWeek() - dayOfPeriod + 1) % 7;
    if (periodStartDayOfWeek < 0) 
        periodStartDayOfWeek += 7;
    
    // Compute the week number.  Initially, ignore the first week, which
    // may be fractional (or may not be).  We add periodStartDayOfWeek in
    // order to fill out the first week, if it is fractional.
    int32_t weekNo = (dayOfPeriod + periodStartDayOfWeek - 1)/7;
    
    // If the first week is long enough, then count it.  If
    // the minimal days in the first week is one, or if the period start
    // is zero, we always increment weekNo.
    if ((7 - periodStartDayOfWeek) >= getMinimalDaysInFirstWeek()) 
        ++weekNo;
    
    return weekNo;
}

// -------------------------------------

int32_t
GregorianCalendar::monthLength(int32_t month) const
{
    int32_t year = internalGet(YEAR);
    if(internalGetEra() == BC) {
        year = 1 - year;
    }

    return monthLength(month, year);
}

// -------------------------------------

int32_t
GregorianCalendar::monthLength(int32_t month, int32_t year) const
{
    return isLeapYear(year) ? kLeapMonthLength[month] : kMonthLength[month];
}

// -------------------------------------

int32_t
GregorianCalendar::yearLength(int32_t year) const
{
    return isLeapYear(year) ? 366 : 365;
}

// -------------------------------------

int32_t
GregorianCalendar::yearLength() const
{
    return isLeapYear(internalGet(YEAR)) ? 366 : 365;
}

// -------------------------------------

/**
 * Overrides Calendar
 * Converts UTC as milliseconds to time field values.
 * The time is <em>not</em>
 * recomputed first; to recompute the time, then the fields, call the
 * <code>complete</code> method.
 * @see Calendar#complete
 */
void
GregorianCalendar::computeFields(UErrorCode& status)
{
    if (U_FAILURE(status)) 
        return;

    int32_t rawOffset = getTimeZone().getRawOffset();
    double localMillis = internalGetTime() + rawOffset;

    /* Check for very extreme values -- millis near Long.MIN_VALUE or
     * Long.MAX_VALUE.  For these values, adding the zone offset can push
     * the millis past MAX_VALUE to MIN_VALUE, or vice versa.  This produces
     * the undesirable effect that the time can wrap around at the ends,
     * yielding, for example, a UDate(Long.MAX_VALUE) with a big BC year
     * (should be AD).  Handle this by pinning such values to Long.MIN_VALUE
     * or Long.MAX_VALUE. - liu 8/11/98 bug 4149677 */
    
    /* {sfb} 9/04/98 
     * Since in C++ we use doubles instead of longs for dates, there is
     * an inherent loss of range in the calendar (because in Java you have all 64
     * bits to store data, while in C++ you have only 52 bits of mantissa.
     * So, I will pin to these (2^52 - 1) values instead */
    
    if(internalGetTime() > 0 && localMillis < 0 && rawOffset > 0) {
        localMillis = LATEST_SUPPORTED_MILLIS;
    } 
    else if(internalGetTime() < 0 && localMillis > 0 && rawOffset < 0) {
        localMillis = EARLIEST_SUPPORTED_MILLIS;
    }

    // Time to fields takes the wall millis (Standard or DST).
    timeToFields(localMillis, FALSE, status);

    uint8_t era         = (uint8_t) internalGetEra();
    int32_t year         = internalGet(YEAR);
    int32_t month         = internalGet(MONTH);
    int32_t date         = internalGet(DATE);
    uint8_t dayOfWeek     = (uint8_t) internalGet(DAY_OF_WEEK);

    double days = uprv_floor(localMillis / kOneDay);
    int32_t millisInDay = (int32_t) (localMillis - (days * kOneDay));
    if (millisInDay < 0) 
        millisInDay += U_MILLIS_PER_DAY;

    // Call getOffset() to get the TimeZone offset.  The millisInDay value must
    // be standard local millis.
    int32_t dstOffset = getTimeZone().getOffset(era, year, month, date, dayOfWeek, millisInDay,
                                            monthLength(month), status) - rawOffset;
    if(U_FAILURE(status))
        return;

    // Adjust our millisInDay for DST, if necessary.
    millisInDay += dstOffset;

    // If DST has pushed us into the next day, we must call timeToFields() again.
    // This happens in DST between 12:00 am and 1:00 am every day.  The call to
    // timeToFields() will give the wrong day, since the Standard time is in the
    // previous day.
    if (millisInDay >= U_MILLIS_PER_DAY) {
        UDate dstMillis = localMillis + dstOffset;
        millisInDay -= U_MILLIS_PER_DAY;
        // As above, check for and pin extreme values
        if(localMillis > 0 && dstMillis < 0 && dstOffset > 0) {
            dstMillis = LATEST_SUPPORTED_MILLIS;
        } 
        else if(localMillis < 0 && dstMillis > 0 && dstOffset < 0) {
            dstMillis = EARLIEST_SUPPORTED_MILLIS;
        }
        timeToFields(dstMillis, FALSE, status);
    }

    // Fill in all time-related fields based on millisInDay.  Call internalSet()
    // so as not to perturb flags.
    internalSet(MILLISECOND, millisInDay % 1000);
    millisInDay /= 1000;
    internalSet(SECOND, millisInDay % 60);
    millisInDay /= 60;
    internalSet(MINUTE, millisInDay % 60);
    millisInDay /= 60;
    internalSet(HOUR_OF_DAY, millisInDay);
    internalSet(AM_PM, millisInDay / 12); // Assume AM == 0
    internalSet(HOUR, millisInDay % 12);

    internalSet(ZONE_OFFSET, rawOffset);
    internalSet(DST_OFFSET, dstOffset);

    // Careful here: We are manually setting the time stamps[] flags to
    // INTERNALLY_SET, so we must be sure that the above code actually does
    // set all these fields.
    for (int i=0; i<FIELD_COUNT; ++i) {
        fStamp[i] = kInternallySet;
        fIsSet[i] = TRUE; // Remove later
    }
}

// -------------------------------------

/**
 * After adjustments such as add(MONTH), add(YEAR), we don't want the
 * month to jump around.  E.g., we don't want Jan 31 + 1 month to go to Mar
 * 3, we want it to go to Feb 28.  Adjustments which might run into this
 * problem call this method to retain the proper month.
 */
void 
GregorianCalendar::pinDayOfMonth() 
{
    int32_t monthLen = monthLength(internalGet(MONTH));
    int32_t dom = internalGet(DAY_OF_MONTH);
    if(dom > monthLen) 
        set(DAY_OF_MONTH, monthLen);
}

// -------------------------------------

bool_t
GregorianCalendar::validateFields() const
{
    for (int32_t field = 0; field < FIELD_COUNT; field++) {
        // Ignore DATE and DAY_OF_YEAR which are handled below
        if (field != DATE &&
            field != DAY_OF_YEAR &&
            isSet((EDateFields)field) &&
            ! boundsCheck(internalGet((EDateFields)field), (EDateFields)field))

            return FALSE;
    }

    // Values differ in Least-Maximum and Maximum should be handled
    // specially.
    if (isSet(DATE)) {
        int32_t date = internalGet(DATE);
        if (date < getMinimum(DATE) ||
            date > monthLength(internalGet(MONTH))) {
            return FALSE;
        }
    }

    if (isSet(DAY_OF_YEAR)) {
        int32_t days = internalGet(DAY_OF_YEAR);
        if (days < 1 || days > yearLength()) 
            return FALSE;
    }

    // Handle DAY_OF_WEEK_IN_MONTH, which must not have the value zero.
    // We've checked against minimum and maximum above already.
    if (isSet(DAY_OF_WEEK_IN_MONTH) &&
        0 == internalGet(DAY_OF_WEEK_IN_MONTH)) 
            return FALSE;

    return TRUE;
}

// -------------------------------------

bool_t
GregorianCalendar::boundsCheck(int32_t value, EDateFields field) const
{
    return value >= getMinimum(field) && value <= getMaximum(field);
}

// -------------------------------------

UDate 
GregorianCalendar::getEpochDay(UErrorCode& status) 
{
    complete(status);
    // Divide by 1000 (convert to seconds) in order to prevent overflow when
    // dealing with UDate(Long.MIN_VALUE) and UDate(Long.MAX_VALUE).
    double wallSec = internalGetTime()/1000 + (internalGet(ZONE_OFFSET) + internalGet(DST_OFFSET))/1000;
    
    // {sfb} force conversion to double
    return uprv_trunc(wallSec / (kOneDay/1000.0));
    //return floorDivide(wallSec, kOneDay/1000.0);
}

// -------------------------------------
void GregorianCalendar::calculateFromYear_Woy(void) {
    UErrorCode errorCode=U_ZERO_ERROR;

    // We need to get January 1st of year in question
    Calendar *calt = new GregorianCalendar(errorCode);
    calt->set(internalGet(Calendar::YEAR_WOY), Calendar::JANUARY, 1);
    int32_t day1Jan = calt->get(Calendar::DAY_OF_WEEK, errorCode);
    delete (calt);

    // Get data for calculation
    int32_t dow = internalGet(Calendar::DOW_LOCAL);
    int32_t woy = internalGet(Calendar::WEEK_OF_YEAR);
    int32_t year = internalGet(Calendar::YEAR_WOY);

    // This is calculated day of year
    int32_t doy = ((woy-1)*7+dow-day1Jan+getFirstDayOfWeek());

/*
    // Unsuccesful attempt to solve cutover year
    if (year==fGregorianCutoverYear) {
        if (doy>=355) {
            year++;
            doy -= 355+7;
        } else if (doy>=298) {
                doy +=20;
        }
    }
*/

    if (day1Jan-getFirstDayOfWeek()<getMinimalDaysInFirstWeek()) {
        // There is a possibility that we're in the last year
        if(dow<day1Jan-1 && woy == 1 && doy<=0) {
            // we're still in the last year
            doy += yearLength(year-1);
            // substract a year to get real year!
            year--;
            set(DAY_OF_YEAR,doy);
            set(YEAR, year);
            return;
        } else {
        }
    } else {
        doy += 7;
    }

    // This is taking us to the next year!
    if(doy>yearLength(year)) {
        doy -= yearLength(year);
        year ++;
        // add a year to get real year!
    } 

    // set calculated values
    set(DAY_OF_YEAR,doy);
    set(YEAR, year);
    return;
}
// -------------------------------------

void
GregorianCalendar::computeTime(UErrorCode& status)
{
    if (U_FAILURE(status)) 
        return;

    if (! isLenient() && ! validateFields()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    // If the only available stuff is localized day of week, week of year and adjusted year
    // we'll calculate the rest from there
    if (fStamp[YEAR] == kUnset && fStamp[YEAR_WOY] != kUnset) {
        calculateFromYear_Woy();
    }

    // This function takes advantage of the fact that unset fields in
    // the time field list have a value of zero.

    // The year defaults to the epoch start.
    int32_t year = (fStamp[YEAR] != kUnset) ? internalGet(YEAR) : kEpochYear;

    int32_t era = AD;
    if (fStamp[ERA] != kUnset) {
        era = internalGet(ERA);
        if (era == BC)
            year = 1 - year;
        // Even in lenient mode we disallow ERA values other than AD & BC
        else if (era != AD) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
    }

    // First, use the year to determine whether to use the Gregorian or the
    // Julian calendar. If the year is not the year of the cutover, this
    // computation will be correct. But if the year is the cutover year,
    // this may be incorrect. In that case, assume the Gregorian calendar,
    // make the computation, and then recompute if the resultant millis
    // indicate the wrong calendar has been assumed.

    // A date such as Oct. 10, 1582 does not exist in a Gregorian calendar
    // with the default changeover of Oct. 15, 1582, since in such a
    // calendar Oct. 4 (Julian) is followed by Oct. 15 (Gregorian).  This
    // algorithm will interpret such a date using the Julian calendar,
    // yielding Oct. 20, 1582 (Gregorian).
    bool_t isGregorian = year >= fGregorianCutoverYear;
    double julianDay = computeJulianDay(isGregorian, year);
    double millis = julianDayToMillis(julianDay);

    // The following check handles portions of the cutover year BEFORE the
    // cutover itself happens. The check for the julianDate number is for a
    // rare case; it's a hardcoded number, but it's efficient.  The given
    // Julian day number corresponds to Dec 3, 292269055 BC, which
    // corresponds to millis near Long.MIN_VALUE.  The need for the check
    // arises because for extremely negative Julian day numbers, the millis
    // actually overflow to be positive values. Without the check, the
    // initial date is interpreted with the Gregorian calendar, even when
    // the cutover doesn't warrant it.
    if (isGregorian != (millis >= fNormalizedGregorianCutover) &&
        julianDay != -106749550580.0) { // See above
        julianDay = computeJulianDay(!isGregorian, year);
        millis = julianDayToMillis(julianDay);
    }

    // Do the time portion of the conversion.

    int32_t millisInDay = 0;

    // Find the best set of fields specifying the time of day.  There
    // are only two possibilities here; the HOUR_OF_DAY or the
    // AM_PM and the HOUR.
    int32_t hourOfDayStamp = fStamp[HOUR_OF_DAY];
    int32_t hourStamp = fStamp[HOUR];
    int32_t bestStamp = (hourStamp > hourOfDayStamp) ? hourStamp : hourOfDayStamp;

    // Hours
    if (bestStamp != kUnset) {
        if (bestStamp == hourOfDayStamp)
            // Don't normalize here; let overflow bump into the next period.
            // This is consistent with how we handle other fields.
            millisInDay += internalGet(HOUR_OF_DAY);

        else {
            // Don't normalize here; let overflow bump into the next period.
            // This is consistent with how we handle other fields.
            millisInDay += internalGet(HOUR);

            millisInDay += 12 * internalGet(AM_PM); // Default works for unset AM_PM
        }
    }

    // We use the fact that unset == 0; we start with millisInDay
    // == HOUR_OF_DAY.
    millisInDay *= 60;
    millisInDay += internalGet(MINUTE); // now have minutes
    millisInDay *= 60;
    millisInDay += internalGet(SECOND); // now have seconds
    millisInDay *= 1000;
    millisInDay += internalGet(MILLISECOND); // now have millis

    // Compute the time zone offset and DST offset.  There are two potential
    // ambiguities here.  We'll assume a 2:00 am (wall time) switchover time
    // for discussion purposes here.
    // 1. The transition into DST.  Here, a designated time of 2:00 am - 2:59 am
    //    can be in standard or in DST depending.  However, 2:00 am is an invalid
    //    representation (the representation jumps from 1:59:59 am Std to 3:00:00 am DST).
    //    We assume standard time.
    // 2. The transition out of DST.  Here, a designated time of 1:00 am - 1:59 am
    //    can be in standard or DST.  Both are valid representations (the rep
    //    jumps from 1:59:59 DST to 1:00:00 Std).
    //    Again, we assume standard time.
    // We use the TimeZone object, unless the user has explicitly set the ZONE_OFFSET
    // or DST_OFFSET fields; then we use those fields.
    const TimeZone& zone = getTimeZone();
    int32_t zoneOffset = (fStamp[ZONE_OFFSET] >= kMinimumUserStamp)
        /*isSet(ZONE_OFFSET) && userSetZoneOffset*/ ?
        internalGet(ZONE_OFFSET) : zone.getRawOffset();

    // Now add date and millisInDay together, to make millis contain local wall
    // millis, with no zone or DST adjustments
    millis += millisInDay;

    int32_t dstOffset = 0;
    if (fStamp[ZONE_OFFSET] >= kMinimumUserStamp
        /*isSet(DST_OFFSET) && userSetDSTOffset*/)
        dstOffset = internalGet(DST_OFFSET);
    else {
        /* Normalize the millisInDay to 0..ONE_DAY-1.  If the millis is out
         * of range, then we must call timeToFields() to recompute our
         * fields. */
        int32_t normalizedMillisInDay [1];
        floorDivide(millis, (int32_t)kOneDay, normalizedMillisInDay);

        // We need to have the month, the day, and the day of the week.
        // Calling timeToFields will compute the MONTH and DATE fields.
        // If we're lenient then we need to call timeToFields() to
        // normalize the year, month, and date numbers.
        uint8_t dow;
        if (isLenient() || fStamp[MONTH] == kUnset || fStamp[DATE] == kUnset
                || millisInDay != normalizedMillisInDay[0]) {
            timeToFields(millis, TRUE, status); // Use wall time; true == do quick computation
            dow = (uint8_t) internalGet(DAY_OF_WEEK); // DOW is computed by timeToFields
        }
        else {
            // It's tempting to try to use DAY_OF_WEEK here, if it
            // is set, but we CAN'T.  Even if it's set, it might have
            // been set wrong by the user.  We should rely only on
            // the Julian day number, which has been computed correctly
            // using the disambiguation algorithm above. [LIU]
            dow = julianDayToDayOfWeek(julianDay);
        }

        // It's tempting to try to use DAY_OF_WEEK here, if it
        // is set, but we CAN'T.  Even if it's set, it might have
        // been set wrong by the user.  We should rely only on
        // the Julian day number, which has been computed correctly
        // using the disambiguation algorithm above. [LIU]
        dstOffset = zone.getOffset((uint8_t)era,
                                   internalGet(YEAR),
                                   internalGet(MONTH),
                                   internalGet(DATE),
                                   dow,
                                   normalizedMillisInDay[0],
                                   monthLength(internalGet(MONTH)),
                                   status) -
            zoneOffset;
        // Note: Because we pass in wall millisInDay, rather than
        // standard millisInDay, we interpret "1:00 am" on the day
        // of cessation of DST as "1:00 am Std" (assuming the time
        // of cessation is 2:00 am).
    }

    // Store our final computed GMT time, with timezone adjustments.
    internalSetTime(millis - zoneOffset - dstOffset);
}

// -------------------------------------

double 
GregorianCalendar::computeJulianDay(bool_t isGregorian, int32_t year) 
{
    int32_t month = 0;
    int32_t date = 0;
    int32_t y;
    double millis = 0;

    // Find the most recent set of fields specifying the day within
    // the year.  These may be any of the following combinations:
    //   MONTH* + DAY_OF_MONTH*
    //   MONTH* + WEEK_OF_MONTH* + DAY_OF_WEEK
    //   MONTH* + DAY_OF_WEEK_IN_MONTH* + DAY_OF_WEEK
    //   DAY_OF_YEAR*
    //   WEEK_OF_YEAR* + DAY_OF_WEEK
    // We look for the most recent of the fields marked thus*.  If other
    // fields are missing, we use their default values, which are those of
    // the epoch start, or in the case of DAY_OF_WEEK, the first day in
    // the week.
    int32_t monthStamp   = fStamp[MONTH];
    int32_t domStamp     = fStamp[DAY_OF_MONTH];
    int32_t womStamp     = fStamp[WEEK_OF_MONTH];
    int32_t dowimStamp   = fStamp[DAY_OF_WEEK_IN_MONTH];
    int32_t doyStamp     = fStamp[DAY_OF_YEAR];
    int32_t woyStamp     = fStamp[WEEK_OF_YEAR];

    int32_t bestStamp = (monthStamp > domStamp) ? monthStamp : domStamp;
    if (womStamp > bestStamp) bestStamp = womStamp;
    if (dowimStamp > bestStamp) bestStamp = dowimStamp;
    if (doyStamp > bestStamp) bestStamp = doyStamp;
    if (woyStamp > bestStamp) bestStamp = woyStamp;

    bool_t useMonth = FALSE;

    if (bestStamp != kUnset &&
        (bestStamp == monthStamp ||
         bestStamp == domStamp ||
         bestStamp == womStamp ||
         bestStamp == dowimStamp)) {
        useMonth = TRUE;

        // We have the month specified. Make it 0-based for the algorithm.
        month = (monthStamp != kUnset) ? internalGet(MONTH) - JANUARY : 0;

        // If the month is out of range, adjust it into range
        if (month < 0 || month > 11) {
            int32_t rem[1];
            year += floorDivide(month, 12, rem);
            month = rem[0];
        }
    }

    bool_t isLeap = year%4 == 0;
    y = year - 1;
    double julianDay = 365.0*y + floorDivide(y, 4) + (kJan1_1JulianDay - 3);

    if (isGregorian) {
        isLeap = isLeap && ((year%100 != 0) || (year%400 == 0));
        // Add 2 because Gregorian calendar starts 2 days after Julian calendar
        julianDay += floorDivide(y, 400) - floorDivide(y, 100) + 2;
    }

    // At this point julianDay is the 0-based day BEFORE the first day of
    // January 1, year 1 of the given calendar.  If julianDay == 0, it
    // specifies (Jan. 1, 1) - 1, in whatever calendar we are using (Julian
    // or Gregorian).

    if (useMonth) {

        julianDay += isLeap ? kLeapNumDays[month] : kNumDays[month];

        if (bestStamp == domStamp ||
            bestStamp == monthStamp) {

            date = (domStamp != kUnset) ? internalGet(DAY_OF_MONTH) : 1;
        }
        else { // assert(bestStamp == womStamp || bestStamp == dowimStamp)
            // Compute from day of week plus week number or from the day of
            // week plus the day of week in month.  The computations are
            // almost identical.

            // Find the day of the week for the first of this month.  This
            // is zero-based, with 0 being the locale-specific first day of
            // the week.  Add 1 to get the 1st day of month.  Subtract
            // getFirstDayOfWeek() to make 0-based.
            int32_t fdm = julianDayToDayOfWeek(julianDay + 1) - getFirstDayOfWeek();
            if (fdm < 0) 
                fdm += 7;

            // Find the start of the first week.  This will be a date from
            // 1..-6.  It represents the locale-specific first day of the
            // week of the first day of the month, ignoring minimal days in
            // first week.
            date = 1 - fdm + ((fStamp[DAY_OF_WEEK] != kUnset) ?
                              (internalGet(DAY_OF_WEEK) - getFirstDayOfWeek()) : 0);

            if (bestStamp == womStamp) {
                // Adjust for minimal days in first week.
                if ((7 - fdm) < getMinimalDaysInFirstWeek()) 
                    date += 7;

                // Now adjust for the week number.
                date += 7 * (internalGet(WEEK_OF_MONTH) - 1);
            }
            else { // assert(bestStamp == dowimStamp)
                // Adjust into the month, if needed.
                if (date < 1) date += 7;

                // We are basing this on the day-of-week-in-month.  The only
                // trickiness occurs if the day-of-week-in-month is
                // negative.
                int32_t dim = internalGet(DAY_OF_WEEK_IN_MONTH);
                if (dim >= 0) 
                    date += 7*(dim - 1);
                else {
                    // Move date to the last of this day-of-week in this
                    // month, then back up as needed.  If dim==-1, we don't
                    // back up at all.  If dim==-2, we back up once, etc.
                    // Don't back up past the first of the given day-of-week
                    // in this month.  Note that we handle -2, -3,
                    // etc. correctly, even though values < -1 are
                    // technically disallowed.
                    date += ((monthLength(internalGet(MONTH), year) - date) / 7 + dim + 1) * 7;
                }
            }
        }

        julianDay += date;
    }
    else {
        // assert(bestStamp == doyStamp || bestStamp == woyStamp ||
        // bestStamp == UNSET).  In the last case we should use January 1.

        // No month, start with January 0 (day before Jan 1), then adjust.

        if (bestStamp == kUnset) {
            ++julianDay; // Advance to January 1
        }
        else if (bestStamp == doyStamp) {
            julianDay += internalGet(DAY_OF_YEAR);

            // Adjust for cutover year [j81 - aliu]
            if (year == fGregorianCutoverYear && isGregorian) {
                julianDay -= 10;
            }
        }
        else if (bestStamp == woyStamp) {
            // Compute from day of week plus week of year

            // Find the day of the week for the first of this year.  This
            // is zero-based, with 0 being the locale-specific first day of
            // the week.  Add 1 to get the 1st day of month.  Subtract
            // getFirstDayOfWeek() to make 0-based.
            int32_t fdy = julianDayToDayOfWeek(julianDay + 1) - getFirstDayOfWeek();
            if (fdy < 0) 
                fdy += 7;

            // Find the start of the first week.  This may be a valid date
            // from 1..7, or a date before the first, from 0..-6.  It
            // represents the locale-specific first day of the week
            // of the first day of the year.

            // First ignore the minimal days in first week.
            date = 1 - fdy + ((fStamp[DAY_OF_WEEK] != kUnset) ?
                              (internalGet(DAY_OF_WEEK) - getFirstDayOfWeek()) : 0);

            // Adjust for minimal days in first week.
            if ((7 - fdy) < getMinimalDaysInFirstWeek()) 
                date += 7;

            // Now adjust for the week number.
            date += 7 * (internalGet(WEEK_OF_YEAR) - 1);

            julianDay += date;

            // Adjust for cutover year [j81 - aliu]
            if (year == fGregorianCutoverYear && isGregorian) {
                julianDay -= 10;
            }
        }
    }

    return julianDay;
}

// -------------------------------------

double 
GregorianCalendar::millisToJulianDay(UDate millis)
{
    return (double)kEpochStartAsJulianDay + floorDivide(millis, kOneDay);
    //return kEpochStartAsJulianDay + uprv_trunc(millis / kOneDay);
}

// -------------------------------------

UDate
GregorianCalendar::julianDayToMillis(double julian)
{
    return (UDate) ((julian - kEpochStartAsJulianDay) * (double) kOneDay);
}

// -------------------------------------

double
GregorianCalendar::floorDivide(double numerator, double denominator) 
{
    return uprv_floor(numerator / denominator);
}

// -------------------------------------

int32_t 
GregorianCalendar::floorDivide(int32_t numerator, int32_t denominator) 
{
    // We do this computation in order to handle
    // a numerator of Long.MIN_VALUE correctly
    return (numerator >= 0) ?
        numerator / denominator :
        ((numerator + 1) / denominator) - 1;
}

// -------------------------------------

int32_t 
GregorianCalendar::floorDivide(int32_t numerator, int32_t denominator, int32_t remainder[])
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
GregorianCalendar::floorDivide(double numerator, int32_t denominator, int32_t remainder[]) 
{
    if (numerator >= 0) {
        remainder[0] = (int32_t)uprv_fmod(numerator, denominator);
        return (int32_t)uprv_trunc(numerator / denominator);
    }
    int32_t quotient = (int32_t)(uprv_trunc((numerator + 1) / denominator) - 1);
    remainder[0] = (int32_t)(numerator - ((double)quotient * denominator));
    return quotient;
}


// -------------------------------------

// {sfb} why does this work, while Calendar::EStampValues doesn't?
GregorianCalendar::EStampValues
GregorianCalendar::aggregateStamp(EStampValues stamp_a, EStampValues stamp_b) 
{
    return ((EStampValues)((stamp_a != kUnset && stamp_b != kUnset) 
        ? uprv_max((int32_t)stamp_a, (int32_t)stamp_b)
        : kUnset));
}

// -------------------------------------

void
GregorianCalendar::add(EDateFields field, int32_t amount, UErrorCode& status)
{
    if (U_FAILURE(status)) 
        return;

    if (amount == 0) 
        return;   // Do nothing!
    complete(status);

    if (field == YEAR_WOY) {
        computeTime(status);
        add(YEAR, amount, status);
        return;
    }

    if (field == DOW_LOCAL) {
        computeTime(status);
        add(DAY_OF_WEEK, amount, status);
        return;
    }

    if (field == YEAR) {
        int32_t year = internalGet(YEAR);
        if (internalGetEra() == AD) {
            year += amount;
            if (year > 0)
                set(YEAR, year);
            else { // year <= 0
                set(YEAR, 1 - year);
                // if year == 0, you get 1 BC
                set(ERA, BC);
            }
        }
        else { // era == BC
            year -= amount;
            if (year > 0)
                set(YEAR, year);
            else { // year <= 0
                set(YEAR, 1 - year);
                // if year == 0, you get 1 AD
                set(ERA, AD);
            }
        }
        pinDayOfMonth();
    }
    else if (field == MONTH) {
        int32_t month = internalGet(MONTH) + amount;
        if (month >= 0) {
            add(YEAR, (int32_t) (month / 12), status);
            set(MONTH, (int32_t) (month % 12));
        }
        else { // month < 0

            add(YEAR, (int32_t) ((month + 1) / 12) - 1, status);
            month %= 12;
            if (month < 0) 
                month += 12;
            set(MONTH, JANUARY + month);
        }
        pinDayOfMonth();
    }
    else if (field == ERA) {
        int32_t era = internalGet(ERA) + amount;
        if (era < 0) 
            era = 0;
        if (era > 1) 
            era = 1;
        set(ERA, era);
    }
    else {
        // We handle most fields here.  The algorithm is to add a computed amount
        // of millis to the current millis.  The only wrinkle is with DST -- if
        // the result of the add operation is to move from DST to Standard, or vice
        // versa, we need to adjust by an hour forward or back, respectively.
        // Otherwise you get weird effects in which the hour seems to shift when
        // you add to the DAY_OF_MONTH field, for instance.

        // We only adjust the DST for fields larger than an hour.  For fields
        // smaller than an hour, we cannot adjust for DST without causing problems.
        // for instance, if you add one hour to April 5, 1998, 1:00 AM, in PST,
        // the time becomes "2:00 AM PDT" (an illegal value), but then the adjustment
        // sees the change and compensates by subtracting an hour.  As a result the
        // time doesn't advance at all.

        // {sfb} do we want to use a double here, or a int32_t?
        // probably a double, since if we used a int32_t in the
        // WEEK_OF_YEAR clause below, if delta was greater than approx.
        // 7.1 we would reach the limit of a int32_t
        double delta = amount;
        bool_t adjustDST = TRUE;

        switch (field) {
        case WEEK_OF_YEAR:
        case WEEK_OF_MONTH:
        case DAY_OF_WEEK_IN_MONTH:
            delta *= 7 * 24 * 60 * 60 * 1000; // 7 days
            break;

        case AM_PM:
            delta *= 12 * 60 * 60 * 1000; // 12 hrs
            break;

        case DATE: // synonym of DAY_OF_MONTH
        case DAY_OF_YEAR:
        case DAY_OF_WEEK:
            delta *= 24 * 60 * 60 * 1000; // 1 day
            break;

        case HOUR_OF_DAY:
        case HOUR:
            delta *= 60 * 60 * 1000; // 1 hour
            adjustDST = FALSE;
            break;

        case MINUTE:
            delta *= 60 * 1000; // 1 minute
            adjustDST = FALSE;
            break;

        case SECOND:
            delta *= 1000; // 1 second
            adjustDST = FALSE;
            break;

        case MILLISECOND:
            adjustDST = FALSE;
            break;

        case ZONE_OFFSET:
        case DST_OFFSET:
        default:
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }

        // Save the current DST state.
        int32_t dst = 0;
        if (adjustDST) 
            dst = internalGet(DST_OFFSET);

        setTimeInMillis(internalGetTime() + delta, status); // Automatically computes fields if necessary

        if (adjustDST) {
            // Now do the DST adjustment alluded to above.
            // Only call setTimeInMillis if necessary, because it's an expensive call.
            dst -= internalGet(DST_OFFSET);
            if(dst!= 0) 
                setTimeInMillis(internalGetTime() + dst, status);
        }
    }
}

// -------------------------------------

/**
 * Roll a field by a signed amount.
 * Note: This will be made public later. [LIU]
 */
void
GregorianCalendar::roll(EDateFields field, int32_t amount, UErrorCode& status)
{
    if(U_FAILURE(status))
        return;

    if (amount == 0) 
        return; // Nothing to do

    if(field == YEAR_WOY) {
        computeTime(status);
        roll(YEAR, amount, status);
        return;
    }

    if(field == DOW_LOCAL) {
        computeTime(status);
        roll(DAY_OF_WEEK, amount, status);
        return;
    }

    int32_t min = 0, max = 0, gap;
    if (field >= 0 && field < FIELD_COUNT) {
        complete(status);
        min = getMinimum(field);
        max = getMaximum(field);
    }

    /* Some of the fields require special handling to work in the month
     * containing the Gregorian cutover point.  Do shared computations
     * for these fields here.  [j81 - aliu] */
    bool_t inCutoverMonth = FALSE;
    int32_t cMonthLen; // 'c' for cutover; in days
    int32_t cDayOfMonth; // no discontinuity: [0, cMonthLen)
    double cMonthStart; // in ms
    switch (field) {
    case DAY_OF_MONTH:
    case WEEK_OF_MONTH:
        {
            max = monthLength(internalGet(MONTH));
            double t = internalGetTime();
            // We subtract 1 from the DAY_OF_MONTH to make it zero-based, and an
            // additional 10 if we are after the cutover.  Thus the monthStart
            // value will be correct iff we actually are in the cutover month.
            cDayOfMonth = internalGet(DAY_OF_MONTH) - ((t >= fGregorianCutover) ? 10 : 0);
            cMonthStart = t - ((cDayOfMonth - 1) * kOneDay);

            // A month containing the cutover is 10 days shorter.
            if ((cMonthStart < fGregorianCutover) &&
                (cMonthStart + (cMonthLen=(max-10))*kOneDay >= fGregorianCutover)) {
                inCutoverMonth = TRUE;
            }
        }
        break;
    }

    switch (field) {
    case ERA:
    case YEAR:
    case AM_PM:
    case MINUTE:
    case SECOND:
    case MILLISECOND:
        // These fields are handled simply, since they have fixed minima
        // and maxima.  The field DAY_OF_MONTH is almost as simple.  Other
        // fields are complicated, since the range within they must roll
        // varies depending on the date.
        break;

    case HOUR:
    case HOUR_OF_DAY:
        // Rolling the hour is difficult on the ONSET and CEASE days of
        // daylight savings.  For example, if the change occurs at
        // 2 AM, we have the following progression:
        // ONSET: 12 Std -> 1 Std -> 3 Dst -> 4 Dst
        // CEASE: 12 Dst -> 1 Dst -> 1 Std -> 2 Std
        // To get around this problem we don't use fields; we manipulate
        // the time in millis directly.
        {
            // Assume min == 0 in calculations below
            UDate start = getTime(status);
            int32_t oldHour = internalGet(field);
            int32_t newHour = (oldHour + amount) % (max + 1);
            if(newHour < 0)
                newHour += max + 1;
            setTime(start + ((double)kOneHour * (newHour - oldHour)), status);
            return;
        }
    case MONTH:
        // Rolling the month involves both pinning the final value to [0, 11]
        // and adjusting the DAY_OF_MONTH if necessary.  We only adjust the
        // DAY_OF_MONTH if, after updating the MONTH field, it is illegal.
        // E.g., <jan31>.roll(MONTH, 1) -> <feb28> or <feb29>.
        {
            int32_t mon = (internalGet(MONTH) + amount) % 12;
            if (mon < 0) 
                mon += 12;
            set(MONTH, mon);
            
            // Keep the day of month in range.  We don't want to spill over
            // into the next month; e.g., we don't want jan31 + 1 mo -> feb31 ->
            // mar3.
            // NOTE: We could optimize this later by checking for dom <= 28
            // first.  Do this if there appears to be a need. [LIU]
            int32_t monthLen = monthLength(mon);
            int32_t dom = internalGet(DAY_OF_MONTH);
            if (dom > monthLen) 
                set(DAY_OF_MONTH, monthLen);
            return;
        }

    case WEEK_OF_YEAR:
        {
            // Unlike WEEK_OF_MONTH, WEEK_OF_YEAR never shifts the day of the
            // week.  However, rolling the week of the year can have seemingly
            // strange effects simply because the year of the week of year
            // may be different from the calendar year.  For example, the
            // date Dec 28, 1997 is the first day of week 1 of 1998 (if
            // weeks start on Sunday and the minimal days in first week is
            // <= 3).
            int32_t woy = internalGet(WEEK_OF_YEAR);
            // Get the ISO year, which matches the week of year.  This
            // may be one year before or after the calendar year.
            int32_t isoYear = internalGet(YEAR);
            int32_t isoDoy = internalGet(DAY_OF_YEAR);
            if (internalGet(MONTH) == Calendar::JANUARY) {
                if (woy >= 52) {
                    --isoYear;
                    isoDoy += yearLength(isoYear);
                }
            }
            else {
                if (woy == 1) {
                    isoDoy -= yearLength(isoYear);
                    ++isoYear;
                }
            }
            woy += amount;
            // Do fast checks to avoid unnecessary computation:
            if (woy < 1 || woy > 52) {
                // Determine the last week of the ISO year.
                // We do this using the standard formula we use
                // everywhere in this file.  If we can see that the
                // days at the end of the year are going to fall into
                // week 1 of the next year, we drop the last week by
                // subtracting 7 from the last day of the year.
                int32_t lastDoy = yearLength(isoYear);
                int32_t lastRelDow = (lastDoy - isoDoy + internalGet(DAY_OF_WEEK) -
                                  getFirstDayOfWeek()) % 7;
                if (lastRelDow < 0) 
                    lastRelDow += 7;
                if ((6 - lastRelDow) >= getMinimalDaysInFirstWeek()) 
                    lastDoy -= 7;
                int32_t lastWoy = weekNumber(lastDoy, lastRelDow + 1);
                woy = ((woy + lastWoy - 1) % lastWoy) + 1;
            }
            set(WEEK_OF_YEAR, woy);
            set(YEAR, isoYear);
            return;
        }
    case WEEK_OF_MONTH:
        {
            // This is tricky, because during the roll we may have to shift
            // to a different day of the week.  For example:

            //    s  m  t  w  r  f  s
            //          1  2  3  4  5
            //    6  7  8  9 10 11 12

            // When rolling from the 6th or 7th back one week, we go to the
            // 1st (assuming that the first partial week counts).  The same
            // thing happens at the end of the month.

            // The other tricky thing is that we have to figure out whether
            // the first partial week actually counts or not, based on the
            // minimal first days in the week.  And we have to use the
            // correct first day of the week to delineate the week
            // boundaries.

            // Here's our algorithm.  First, we find the real boundaries of
            // the month.  Then we discard the first partial week if it
            // doesn't count in this locale.  Then we fill in the ends with
            // phantom days, so that the first partial week and the last
            // partial week are full weeks.  We then have a nice square
            // block of weeks.  We do the usual rolling within this block,
            // as is done elsewhere in this method.  If we wind up on one of
            // the phantom days that we added, we recognize this and pin to
            // the first or the last day of the month.  Easy, eh?

            // Another wrinkle: To fix jitterbug 81, we have to make all this
            // work in the oddball month containing the Gregorian cutover.
            // This month is 10 days shorter than usual, and also contains
            // a discontinuity in the days; e.g., the default cutover month
            // is Oct 1582, and goes from day of month 4 to day of month 15.

            // Normalize the DAY_OF_WEEK so that 0 is the first day of the week
            // in this locale.  We have dow in 0..6.
            int32_t dow = internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();
            if (dow < 0) 
                dow += 7;

            // Find the day of month, compensating for cutover discontinuity.
            int32_t dom = inCutoverMonth ? cDayOfMonth : internalGet(DAY_OF_MONTH);

            // Find the day of the week (normalized for locale) for the first
            // of the month.
            int32_t fdm = (dow - dom + 1) % 7;
            if (fdm < 0) 
                fdm += 7;

            // Get the first day of the first full week of the month,
            // including phantom days, if any.  Figure out if the first week
            // counts or not; if it counts, then fill in phantom days.  If
            // not, advance to the first real full week (skip the partial week).
            int32_t start;
            if ((7 - fdm) < getMinimalDaysInFirstWeek())
                start = 8 - fdm; // Skip the first partial week
            else
                start = 1 - fdm; // This may be zero or negative

            // Get the day of the week (normalized for locale) for the last
            // day of the month.
            int32_t monthLen = inCutoverMonth ? cMonthLen : monthLength(internalGet(MONTH));
            int32_t ldm = (monthLen - dom + dow) % 7;
            // We know monthLen >= DAY_OF_MONTH so we skip the += 7 step here.

            // Get the limit day for the blocked-off rectangular month; that
            // is, the day which is one past the last day of the month,
            // after the month has already been filled in with phantom days
            // to fill out the last week.  This day has a normalized DOW of 0.
            int32_t limit = monthLen + 7 - ldm;

            // Now roll between start and (limit - 1).
            gap = limit - start;
            int32_t newDom = (dom + amount*7 - start) % gap;
            if (newDom < 0) 
                newDom += gap;
            newDom += start;

            // Finally, pin to the real start and end of the month.
            if (newDom < 1) 
                newDom = 1;
            if (newDom > monthLen) 
                newDom = monthLen;

            // Set the DAY_OF_MONTH.  We rely on the fact that this field
            // takes precedence over everything else (since all other fields
            // are also set at this point).  If this fact changes (if the
            // disambiguation algorithm changes) then we will have to unset
            // the appropriate fields here so that DAY_OF_MONTH is attended
            // to.

            // If we are in the cutover month, manipulate ms directly.  Don't do
            // this in general because it doesn't work across DST boundaries
            // (details, details).  This takes care of the discontinuity.
            if (inCutoverMonth) {
                setTimeInMillis(cMonthStart + (newDom-1)*kOneDay, status);                
            } else {
                set(DAY_OF_MONTH, newDom);
            }
            return;
        }
    case DAY_OF_MONTH:
        if (inCutoverMonth) {            
            // The default computation works except when the current month
            // contains the Gregorian cutover.  We handle this special case
            // here.  [j81 - aliu]
            double monthLen = cMonthLen * kOneDay;
            double msIntoMonth = uprv_fmod(internalGetTime() - cMonthStart +
                                          amount * kOneDay, monthLen);
            if (msIntoMonth < 0) {
                msIntoMonth += monthLen;
            }
            setTimeInMillis(cMonthStart + msIntoMonth, status);
            return;
        } else {
            max = monthLength(internalGet(MONTH));
            // ...else fall through to default computation
        }
        break;
    case DAY_OF_YEAR:
        {
            // Roll the day of year using millis.  Compute the millis for
            // the start of the year, and get the length of the year.
            double delta = amount * kOneDay; // Scale up from days to millis
            double min2 = internalGetTime() - (internalGet(DAY_OF_YEAR) - 1) * kOneDay;
            int32_t yearLen = yearLength();
            internalSetTime( uprv_fmod((internalGetTime() + delta - min2), (yearLen * kOneDay)));
            if (internalGetTime() < 0) 
                internalSetTime( internalGetTime() + yearLen * kOneDay);

            setTimeInMillis(internalGetTime() + min2, status);
            return;
        }
    case DAY_OF_WEEK:
        {
            // Roll the day of week using millis.  Compute the millis for
            // the start of the week, using the first day of week setting.
            // Restrict the millis to [start, start+7days).
            double delta = amount * kOneDay; // Scale up from days to millis
            // Compute the number of days before the current day in this
            // week.  This will be a value 0..6.
            int32_t leadDays = internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();
            if (leadDays < 0) 
                leadDays += 7;
            double min2 = internalGetTime() - leadDays * kOneDay;
            internalSetTime(uprv_fmod((internalGetTime() + delta - min2), kOneWeek));
            if (internalGetTime() < 0) 
                internalSetTime(internalGetTime() + kOneWeek);
            setTimeInMillis(internalGetTime() + min2, status);
            return;
        }
    case DAY_OF_WEEK_IN_MONTH:
        {
            // Roll the day of week in the month using millis.  Determine
            // the first day of the week in the month, and then the last,
            // and then roll within that range.
            double delta = amount * kOneWeek; // Scale up from weeks to millis
            // Find the number of same days of the week before this one
            // in this month.
            int32_t preWeeks = (internalGet(DAY_OF_MONTH) - 1) / 7;
            // Find the number of same days of the week after this one
            // in this month.
            int32_t postWeeks = (monthLength(internalGet(MONTH)) - internalGet(DAY_OF_MONTH)) / 7;
            // From these compute the min and gap millis for rolling.
            double min2 = internalGetTime() - preWeeks * kOneWeek;
            double gap2 = kOneWeek * (preWeeks + postWeeks + 1); // Must add 1!
            // Roll within this range
            internalSetTime(uprv_fmod((internalGetTime() + delta - min2), gap2));
            if (internalGetTime() < 0) 
                internalSetTime(internalGetTime() + gap2);
            setTimeInMillis(internalGetTime() + min2, status);
            return;
        }
    case ZONE_OFFSET:
    case DST_OFFSET:
    default:
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
        // These fields cannot be rolled
    }

    // These are the standard roll instructions.  These work for all
    // simple cases, that is, cases in which the limits are fixed, such
    // as the hour, the month, and the era.
    gap = max - min + 1;
    int32_t value = internalGet(field) + amount;
    value = (value - min) % gap;
    if (value < 0) 
        value += gap;
    value += min;

    set(field, value);

}

// -------------------------------------

int32_t
GregorianCalendar::getMinimum(EDateFields field) const
{
    return kMinValues[field];
}

// -------------------------------------

int32_t
GregorianCalendar::getMaximum(EDateFields field) const
{
    return kMaxValues[field];
}

// -------------------------------------

int32_t
GregorianCalendar::getGreatestMinimum(EDateFields field) const
{
    return kMinValues[field];
}

// -------------------------------------

int32_t
GregorianCalendar::getLeastMaximum(EDateFields field) const
{
    return kLeastMaxValues[field];
}

// -------------------------------------

int32_t 
GregorianCalendar::getActualMinimum(EDateFields field) const
{
    return getMinimum(field);
}

// -------------------------------------

int32_t 
GregorianCalendar::getActualMaximum(EDateFields field) const
{
    /* It is a known limitation that the code here (and in getActualMinimum)
     * won't behave properly at the extreme limits of GregorianCalendar's
     * representable range (except for the code that handles the YEAR
     * field).  That's because the ends of the representable range are at
     * odd spots in the year.  For calendars with the default Gregorian
     * cutover, these limits are Sun Dec 02 16:47:04 GMT 292269055 BC to Sun
     * Aug 17 07:12:55 GMT 292278994 AD, somewhat different for non-GMT
     * zones.  As a result, if the calendar is set to Aug 1 292278994 AD,
     * the actual maximum of DAY_OF_MONTH is 17, not 30.  If the date is Mar
     * 31 in that year, the actual maximum month might be Jul, whereas is
     * the date is Mar 15, the actual maximum might be Aug -- depending on
     * the precise semantics that are desired.  Similar considerations
     * affect all fields.  Nonetheless, this effect is sufficiently arcane
     * that we permit it, rather than complicating the code to handle such
     * intricacies. - liu 8/20/98 */

    UErrorCode status = U_ZERO_ERROR;

    switch (field) {
        // we have functions that enable us to fast-path number of days in month
        // of year
    case DAY_OF_MONTH:
        return monthLength(get(MONTH, status));

    case DAY_OF_YEAR:
        return yearLength();

        // for week of year, week of month, or day of week in month, we
        // just fall back on the default implementation in Calendar (I'm not sure
        // we could do better by having special calculations here)
    case WEEK_OF_YEAR:
    case WEEK_OF_MONTH:
    case DAY_OF_WEEK_IN_MONTH:
        return Calendar::getActualMaximum(field, status);

    case YEAR:
        /* The year computation is no different, in principle, from the
         * others, however, the range of possible maxima is large.  In
         * addition, the way we know we've exceeded the range is different.
         * For these reasons, we use the special case code below to handle
         * this field.
         *
         * The actual maxima for YEAR depend on the type of calendar:
         *
         *     Gregorian = May 17, 292275056 BC - Aug 17, 292278994 AD
         *     Julian    = Dec  2, 292269055 BC - Jan  3, 292272993 AD
         *     Hybrid    = Dec  2, 292269055 BC - Aug 17, 292278994 AD
         *
         * We know we've exceeded the maximum when either the month, date,
         * time, or era changes in response to setting the year.  We don't
         * check for month, date, and time here because the year and era are
         * sufficient to detect an invalid year setting.  NOTE: If code is
         * added to check the month and date in the future for some reason,
         * Feb 29 must be allowed to shift to Mar 1 when setting the year.
         */
        {
            Calendar *cal = (Calendar*)this->clone();
            cal->setLenient(TRUE);
            
            int32_t era = cal->get(ERA, status);
            if(U_FAILURE(status))
                return 0;

            UDate d = cal->getTime(status);
            if(U_FAILURE(status))
                return 0;

            /* Perform a binary search, with the invariant that lowGood is a
             * valid year, and highBad is an out of range year.
             */
            int32_t lowGood = kLeastMaxValues[YEAR];
            int32_t highBad = kMaxValues[YEAR] + 1;
            while((lowGood + 1) < highBad) {
                int32_t y = (lowGood + highBad) / 2;
                cal->set(YEAR, y);
                if(cal->get(YEAR, status) == y && cal->get(ERA, status) == era) {
                    lowGood = y;
                } 
                else {
                    highBad = y;
                    cal->setTime(d, status); // Restore original fields
                }
            }
            
            delete cal;
            return lowGood;
        }

        // and we know none of the other fields have variable maxima in
        // GregorianCalendar, so we can just return the fixed maximum
    default:
        return getMaximum(field);
    }
}

// -------------------------------------

bool_t
GregorianCalendar::inDaylightTime(UErrorCode& status) const
{
    if (U_FAILURE(status) || !getTimeZone().useDaylightTime()) 
        return FALSE;

    // Force an update of the state of the Calendar.
    ((GregorianCalendar*)this)->complete(status); // cast away const

    return U_SUCCESS(status) ? (internalGet(DST_OFFSET) != 0) : FALSE;
}

// -------------------------------------

int32_t
GregorianCalendar::getISOYear(UErrorCode& status) 
{
    ((GregorianCalendar*)this)->complete(status);

    int32_t woy = internalGet(WEEK_OF_YEAR);
    // Get the ISO year, which matches the week of year.  This
    // may be one year before or after the calendar year.
    int32_t isoYear = internalGet(YEAR);
    if (internalGet(MONTH) == Calendar::JANUARY) {
        if (woy >= 52) {
            --isoYear;
        }
    }
    else {
        if (woy == 1) {
            ++isoYear;
        }
    }
    return isoYear;
}

/**
 * Return the ERA.  We need a special method for this because the
 * default ERA is AD, but a zero (unset) ERA is BC.
 */
int32_t
GregorianCalendar::internalGetEra() const {
    return isSet(ERA) ? internalGet(ERA) : AD;
}

//eof
