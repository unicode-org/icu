// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ******************************************************************************
 *
 * File MYANCAL.CPP
 *
 * Modification History:
 *
 *   Date        Name        Description
 *   12/28/2017  mapmeld     copied from persncal.cpp
 *   4/9/2019    mapmeld     continuing project
 *****************************************************************************
 */

#include "myancal.h"

#if !UCONFIG_NO_FORMATTING

#include "umutex.h"
#include "gregoimp.h" // Math
#include <math.h>
#include <float.h>

static const int32_t kMyanmarCalendarLimits[UCAL_FIELD_COUNT][4] = {
    // Minimum  Greatest     Least   Maximum
    //           Minimum   Maximum
    {        0,        0,        2,        2}, // ERA
    { -5000000, -5000000,  5000000,  5000000}, // YEAR
    {        1,        1,       12,       14}, // MONTH
    {        1,        1,       51,       56}, // WEEK_OF_YEAR
    {        1,        1,        5,        5}, // WEEK_OF_MONTH
    {        1,       1,        29,       30}, // DAY_OF_MONTH
    {        1,       1,       354,      385}, // DAY_OF_YEAR
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // DAY_OF_WEEK
    {        1,       1,         5,        5}, // DAY_OF_WEEK_IN_MONTH
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // AM_PM
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // HOUR
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // HOUR_OF_DAY
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // MINUTE
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // SECOND
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // MILLISECOND
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // ZONE_OFFSET
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // DST_OFFSET
    { -5000000, -5000000,  5000000,  5000000}, // YEAR_WOY
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // DOW_LOCAL
    { -5000000, -5000000,  5000000,  5000000}, // EXTENDED_YEAR
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // JULIAN_DAY
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // MILLISECONDS_IN_DAY
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // IS_LEAP_MONTH
};

static const int8_t kMyanmarMonthLength[]
= {29,30,29,30,0,29,30,29,30,29,30,29,30}; // 0-based
static const int8_t kMyanmarSmallLeapMonthLength[]
= {29,30,29,30,30,29,30,29,30,29,30,29,30}; // 0-based
static const int8_t kMyanmarLargeLeapMonthLength[]
= {29,30,30,30,30,29,30,29,30,29,30,29,30}; // 0-based

static const double SOLAR_YEAR = 1577917828.0 / 4320000.0; //solar year (365.2587565)
static const double LUNAR_MONTH = 1577917828.0 / 53433336.0; //lunar month (29.53058795)
static const double MYANMAR_EPOCH = 1954168.050623; //beginning of 0 ME

U_NAMESPACE_BEGIN

// Implementation of the MyanmarCalendar class

//-------------------------------------------------------------------------
// Constructors...
//-------------------------------------------------------------------------

const char *MyanmarCalendar::getType() const {
    return "myanmar";
}

Calendar* MyanmarCalendar::clone() const {
    return new MyanmarCalendar(*this);
}

MyanmarCalendar::MyanmarCalendar(const Locale& aLocale, UErrorCode& success)
  :   Calendar(TimeZone::createDefault(), aLocale, success)
{
    setTimeInMillis(getNow(), success); // Call this again now that the vtable is set up properly.
}

MyanmarCalendar::MyanmarCalendar(const MyanmarCalendar& other) : Calendar(other) {
}

MyanmarCalendar::~MyanmarCalendar()
{
}

//-------------------------------------------------------------------------
// Minimum / Maximum access functions
//-------------------------------------------------------------------------


int32_t MyanmarCalendar::handleGetLimit(UCalendarDateFields field, ELimitType limitType) const {
    return kMyanmarCalendarLimits[field][limitType];
}

//-------------------------------------------------------------------------
// Assorted calculation utilities
//

/**
 * Determine whether a Myanmar year is a leap year (big or little watat)
 */
UBool MyanmarCalendar::isLeapYear(int32_t year)
{
    return false;
}

/**
 * Return the day # on which the given year starts.  Days are counted
 * from the Myanmar epoch, origin 0.
 */
int32_t MyanmarCalendar::yearStart(int32_t year) {
    return handleComputeMonthStart(year,1,FALSE);
}

/**
 * Return the day # on which the given month starts.  Days are counted
 * from the Myanmar epoch, origin 0.
 *
 * @param year  The Myanmar year
 * @param month The Myanmar month, 0-based
 */
int32_t MyanmarCalendar::monthStart(int32_t year, int32_t month) const {
    return handleComputeMonthStart(year,month,TRUE);
}

//----------------------------------------------------------------------
// Calendar framework
//----------------------------------------------------------------------

/**
 * Return the length (in days) of the given month.
 *
 * @param year  The Myanmar year
 * @param month The Myanmar month, 0-based
 */
int32_t MyanmarCalendar::handleGetMonthLength(int32_t extendedYear, int32_t month) const {
    // If the month is out of range, adjust it into range, and
    // modify the extended year value accordingly.
    // if (month < 0 || month > 12) {
    //     extendedYear += ClockMath::floorDivide(month, 13, month);
    // }

    bool watat_year = isLeapYear(extendedYear);
    if (watat_year) {
      return kMyanmarMonthLength[month];
    } else {
      return kMyanmarSmallLeapMonthLength[month];
    } //else if (watat_year == 2) {
      //return kMyanmarLargeLeapMonthLength[month];
    //}
}

/**
 * Return the number of days in the given Myanmar year
 */
int32_t MyanmarCalendar::handleGetYearLength(int32_t extendedYear) const {
    int32_t leapStatus;

    leapStatus = isLeapYear(extendedYear);
    if (leapStatus) {
      return 354;
    } else { //if (leapStatus == 1) {
      return 384;
    }// else if (leapStatus == 2) {
    //  return 385;
    //}
}

void MyanmarCalendar::cal_my(int32_t myan_year, int32_t& myan_year_type, long& startOfTagu, long& full_moon_waso_2, long& addedOffset) const {
	long prevYears = 0, year_length_diff = 0, prev_year_little_watat, prev_year_full_moon_waso_2, subject_year_little_watat, subject_year_full_moon_waso_2;
  addedOffset = 0;
	cal_watat(myan_year, subject_year_little_watat, subject_year_full_moon_waso_2);
  myan_year_type = subject_year_little_watat;
	do {
    prevYears++;
    cal_watat(myan_year - prevYears, prev_year_little_watat, prev_year_full_moon_waso_2);
  } while (prev_year_little_watat == 0 && prevYears < 3);
	if (myan_year_type) {
		year_length_diff = (subject_year_full_moon_waso_2 - prev_year_full_moon_waso_2) % 354;
    myan_year_type = long(ClockMath::floorDivide(year_length_diff, 31) + 1);
		full_moon_waso_2 = subject_year_full_moon_waso_2;
    if (year_length_diff != 30 && year_length_diff != 31) {
      addedOffset = 1;
    }
	} else {
    full_moon_waso_2 = prev_year_full_moon_waso_2 + 354 * prevYears;
  }
	startOfTagu = prev_year_full_moon_waso_2 + 354 * prevYears - 102;
}

//-------------------------------------------------------------------------
// Functions for converting from field values to milliseconds....
//-------------------------------------------------------------------------

// Return JD of start of given month/year
int32_t MyanmarCalendar::handleComputeMonthStart(int32_t eyear, int32_t monthOrder, UBool /*useMonth*/) const {
    // If the month is out of range, adjust it into range, and
    // modify the extended year value accordingly.
    // if (month < 0 || month > 12) {
    //     eyear += ClockMath::floorDivide(month, 13, month);
    // }

    // convert to mcal month order
    int32_t month = monthOrder;
    if (month == 4) {
      month = 0;
    } else if (month > 4) {
      month--;
    }
    int32_t myan_day = 1; // first of month
    int32_t myan_year_type;
    long b, c, dayOfYear, year_length, monthType;
    long startOfTagu, full_moon_waso_2, addedOffset;
    cal_my(long(eyear), myan_year_type, startOfTagu, full_moon_waso_2, addedOffset);//check year
    monthType = ClockMath::floorDivide(month, 13);
    month = month % 13 + monthType; // to 1-12 with month type
    b = ClockMath::floorDivide(myan_year_type, 2);
    c = 1 - ClockMath::floorDivide(myan_year_type + 1, 2); //if big watat and common year
    month += 4 - long(ClockMath::floorDivide(month + 15, 16)) * 4
            + long(ClockMath::floorDivide(month + 12, 16)); //adjust month
	  dayOfYear = myan_day + ClockMath::floorDivide(int32_t(29.544 * month - 29.26), 1)
            - c * ClockMath::floorDivide(month + 11, 16) * 30
            + b * ClockMath::floorDivide(month + 12, 16);
	  year_length = 354 + (1 - c) * 30 + b;
    dayOfYear += monthType * year_length;//adjust day count with year length
	  return (dayOfYear + startOfTagu);
}

//-------------------------------------------------------------------------
// Functions for converting from milliseconds to field values
//-------------------------------------------------------------------------

int32_t MyanmarCalendar::handleGetExtendedYear() {
    int32_t year;
    if (newerField(UCAL_EXTENDED_YEAR, UCAL_YEAR) == UCAL_EXTENDED_YEAR) {
        year = internalGet(UCAL_EXTENDED_YEAR, 1); // Default to year 1
    } else {
        year = internalGet(UCAL_YEAR, 1); // Default to year 1
    }
    return year;
}

/**
 * Override Calendar to compute several fields specific to the Myanmar
 * calendar system.  These are:
 *
 * <ul><li>ERA
 * <li>YEAR
 * <li>MONTH
 * <li>DAY_OF_MONTH
 * <li>DAY_OF_YEAR
 * <li>EXTENDED_YEAR</ul>
 *
 * The DAY_OF_WEEK and DOW_LOCAL fields are already set when this
 * method is called.
 */
void MyanmarCalendar::handleComputeFields(int32_t julianDay, UErrorCode &/*status*/) {
    int32_t dayOfYear, year_length, monthType;
    long startOfTagu, full_moon_waso_2, addedOffset, a, b, c, e, f;
    int32_t myan_year_type;
    long myan_year = ClockMath::floorDivide(julianDay - 0.5 - MYANMAR_EPOCH, SOLAR_YEAR); //Myanmar year
    cal_my(myan_year, myan_year_type, startOfTagu, full_moon_waso_2, addedOffset); //check year
    dayOfYear = julianDay - startOfTagu + 1;//day count
    b = ClockMath::floorDivide(myan_year_type, 2);
    c = ClockMath::floorDivide(1, (myan_year_type + 1)); //big wa and common yr
    year_length = 354 + (1 - c) * 30 + b;//year length
    monthType = ClockMath::floorDivide(dayOfYear - 1, year_length); //month type: late =1 or early = 0
    dayOfYear -= monthType * year_length;
    a = ClockMath::floorDivide(dayOfYear + 423, 512); //adjust day count and threshold
    int32_t myan_month = ClockMath::floorDivide(dayOfYear - b * a + c * a * 30 + 29.26, 29.544); //month
    e = ClockMath::floorDivide(myan_month + 12, 16);
    f = ClockMath::floorDivide(myan_month + 11, 16);
    int32_t myan_day = dayOfYear - long(ClockMath::floorDivide(int32_t(29.544 * myan_month - 29.26), 1))
          - b * e + c * f * 30; //day
    myan_month += f * 3 - e * 4 + 12 * monthType;

    // adjust myan_month from mcal's order
    if (myan_month == 0) {
      myan_month = 4;
    } else if (myan_month >= 4) {
      myan_month++;
    }
    //myan_month--;
    //myan_day++;

    internalSet(UCAL_ERA, 0);
    internalSet(UCAL_YEAR, myan_year);
    internalSet(UCAL_EXTENDED_YEAR, myan_year);
    internalSet(UCAL_MONTH, myan_month);
    internalSet(UCAL_DAY_OF_MONTH, myan_day);
    internalSet(UCAL_DAY_OF_YEAR, dayOfYear);
}

// Search first dimension in a 2D array
// input: (k=key,A=array,u=size)
// output: (i=index)
long MyanmarCalendar::bSearch2(int32_t k, long (*A)[2], long u) const {
	long i = 0;
  long l = 0;
  u--;
	while(u >= l) {
		i = ClockMath::floorDivide(l + u, 2);
		if (A[i][0] > k) u = i - 1;
		else if (A[i][0] < k) l = i + 1;
		else return i;
	} return -1;
}
//-----------------------------------------------------------------------------
// Search a 1D array
// input: (k=key,A=array,u=size)
// output: (i=index)
long MyanmarCalendar::bSearch1(int32_t k,long* A, long u) const {
	long i=0;
  long l=0;
  u--;
	while (u >= l) {
		i = ClockMath::floorDivide(l + u, 2);
		if (A[i] > k)  u = i - 1;
		else if (A[i] < k) l = i + 1;
		else return i;
	} return -1;
}

void MyanmarCalendar::GetMyConst(int32_t myan_year, double& era, double& WO, double& NM, long& EW) const {
	EW = 0;
  long (*big_watat)[2];
  long* wte;
  long i = -1, uf, uw;
	// The third era (the era after Independence 1312 ME and after)
	if (myan_year >= 1312) {
		era = 3;
    WO = -0.5;
    NM = 8;
		long era_big_watat[][2] = { {1377, 1} };
		long wte3[] = { 1344, 1345 };
		big_watat = era_big_watat;
    wte = wte3;
		uf = long(sizeof(era_big_watat) / sizeof(era_big_watat[0]));
		uw = long(sizeof(wte3) / sizeof(wte3[0]));
	}
	// The second era (the era under British colony: 1217 ME - 1311 ME)
	else if (myan_year >= 1217) {
		era = 2;
    WO = -1;
    NM = 4;
		long era_big_watat[][2] = { {1234, 1},{1261, -1} };
		long wte2[] = { 1263, 1264 };
		big_watat = era_big_watat;
    wte = wte2;
		uf = long(sizeof(era_big_watat) / sizeof(era_big_watat[0]));
		uw = long(sizeof(wte2) / sizeof(wte2[0]));
	}
	// The first era (the era of Myanmar kings: ME1216 and before)
	// Thandeikta (ME 1100 - 1216)
	else if (myan_year >= 1100) {
		era = 1.3;
    WO = -0.85;
    NM = -1;
		long era_big_watat[][2] = {{1120, 1}, {1126, -1}, {1150, 1}, {1172, -1}, {1207, 1}};
		long wte13[] = {1201, 1202};
		big_watat = era_big_watat;
    wte = wte13;
		uf = long(sizeof(era_big_watat) / sizeof(era_big_watat[0]));
		uw = long(sizeof(wte13) / sizeof(wte13[0]));
	}
	// Makaranta system 2 (ME 798 - 1099)
	else if (myan_year >= 798) {
		era = 1.2;
    WO = -1.1;
    NM = -1;
		long era_big_watat[][2] = {{813, -1}, {849, -1}, {851, -1}, {854, -1}, {927, -1},
		{933, -1}, {936, -1}, {938, -1}, {949, -1}, {952, -1}, {963, -1}, {968, -1}, {1039, -1}};
		long wte12[] = {-9999};
		big_watat = era_big_watat;
    wte = wte12;
		uf = long(sizeof(era_big_watat) / sizeof(era_big_watat[0]));
		uw = long(sizeof(wte12) / sizeof(wte12[0]));
	}
	// Makaranta system 1 (ME 0 - 797)
	else {
		era = 1.1;
    WO = -1.1;
    NM = -1;
		long era_big_watat[][2] = {{205, 1}, {246, 1}, {471, 1}, {572, -1}, {651, 1},
		{653, 2}, {656, 1}, {672, 1}, {729, 1}, {767, -1}};
		long wte11[] = {-9999};
		big_watat = era_big_watat;
    wte = wte11;
		uf = long(sizeof(era_big_watat) / sizeof(era_big_watat[0]));
		uw = long(sizeof(wte11) / sizeof(wte11[0]));
	}

	i = bSearch2(myan_year, big_watat, uf);
  if (i >= 0) WO += big_watat[i][1]; // full moon day offset exceptions

  i = bSearch1(myan_year, wte, uw);
  if (i >= 0) EW = 1; //correct watat exceptions
}

void MyanmarCalendar::cal_watat(int32_t myan_year, long& watat, long& full_moon_waso_2) const {//get data for respective era
	double era, WO, NM, excess_days;
  long EW;
	GetMyConst(myan_year, era, WO, NM, EW); // get constants for the corresponding calendar era
	double TA = (SOLAR_YEAR / 12 - LUNAR_MONTH) * (12 - NM); //threshold to adjust
  // printf("calculate remainder of solar_year * (%d + 3739) / lunar_month", myan_year);
	// ClockMath::floorDivide(double(SOLAR_YEAR * (long(myan_year) + 3739)), LUNAR_MONTH, excess_days); // excess days
  // printf("got excess_days %s instead of %s", excess_days, fmod(SOLAR_YEAR*(long(myan_year)+3739),LUNAR_MONTH));
  excess_days = fmod(SOLAR_YEAR*(long(myan_year)+3739),LUNAR_MONTH);
	if (excess_days < TA) {
    excess_days += LUNAR_MONTH; //adjust excess days
  }
	full_moon_waso_2 = long(round(SOLAR_YEAR * long(myan_year) + MYANMAR_EPOCH - excess_days + 4.5 * LUNAR_MONTH + WO));
  //full moon day of 2nd Waso
	double TW = 0;
  watat = 0;//find watat
	if (era >= 2) {//if 2nd era or later find watat based on excess days
		TW = LUNAR_MONTH - (SOLAR_YEAR / 12 - LUNAR_MONTH) * NM;
		if (excess_days >= TW) {
      watat = 1;
    }
	}
	else {//if 1st era,find watat by 19 years metonic cycle
	//Myanmar year is divided by 19 and there is intercalary month
	//if the remainder is 2,5,7,10,13,15,18
	//https://github.com/kanasimi/CeJS/blob/master/data/date/calendar.js#L2330
		watat = (long(myan_year) * 7 + 2) % 19;
    if (watat < 0) {
      watat += 19;
    }
		watat = long(ClockMath::floorDivide(watat, 12));
	}
	watat^=EW;//correct watat exceptions
}

// default century

static UDate           gSystemDefaultCenturyStart       = DBL_MIN;
static int32_t         gSystemDefaultCenturyStartYear   = -1;
static icu::UInitOnce  gSystemDefaultCenturyInit        = U_INITONCE_INITIALIZER;

UBool MyanmarCalendar::haveDefaultCentury() const
{
    return TRUE;
}

static void U_CALLCONV initializeSystemDefaultCentury() {
    // initialize systemDefaultCentury and systemDefaultCenturyYear based
    // on the current time.  They'll be set to 80 years before
    // the current time.
    UErrorCode status = U_ZERO_ERROR;
    MyanmarCalendar calendar(Locale("@calendar=myanmar"),status);
   if (U_SUCCESS(status))
   {
        calendar.setTime(Calendar::getNow(), status);
        calendar.add(UCAL_YEAR, -80, status);

        gSystemDefaultCenturyStart = calendar.getTime(status);
        gSystemDefaultCenturyStartYear = calendar.get(UCAL_YEAR, status);
   } else {
     // printf("fail create Myanmar\n");
   }
}

UDate MyanmarCalendar::defaultCenturyStart() const {
    // lazy-evaluate systemDefaultCenturyStart
    umtx_initOnce(gSystemDefaultCenturyInit, &initializeSystemDefaultCentury);
    return gSystemDefaultCenturyStart;
}

int32_t MyanmarCalendar::defaultCenturyStartYear() const {
    // lazy-evaluate systemDefaultCenturyStartYear
    umtx_initOnce(gSystemDefaultCenturyInit, &initializeSystemDefaultCentury);
    return gSystemDefaultCenturyStartYear;
}

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(MyanmarCalendar)

U_NAMESPACE_END

#endif
