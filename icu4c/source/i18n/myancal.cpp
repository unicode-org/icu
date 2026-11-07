// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ******************************************************************************
 *
 * File MYANCAL.CPP
 *
 * Modification History:
 *
 *   Date        Name        Description
 *   4/9/2019    mapmeld     adapted from mmcal project
 *****************************************************************************
 */

#include "myancal.h"

#if !UCONFIG_NO_FORMATTING

#include "umutex.h"
#include "gregoimp.h" // Math
#include <math.h>
#include <float.h>

static const int32_t kMyanmarCalendarLimits[UCAL_FIELD_COUNT][4] = {
  // These are ICU field-limit contracts used by generic calendar logic/tests.
  // They are not a direct list of raw historical extrema for every Myanmar year.
    // Minimum  Greatest     Least   Maximum
    //           Minimum   Maximum
    {        0,        0,        2,        2}, // ERA
    { -5000000, -5000000,  5000000,  5000000}, // YEAR
    {        0,        1,       12,       14}, // MONTH
    {        1,        1,       55,       55}, // WEEK_OF_YEAR
    {        0,        0,        5,        6}, // WEEK_OF_MONTH
    {        1,        1,       29,       30}, // DAY_OF_MONTH
    {        1,        1,      385,      385}, // DAY_OF_YEAR
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // DAY_OF_WEEK
    {        1,        1,        5,        5}, // DAY_OF_WEEK_IN_MONTH
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
    {        0,        1,       12,       14}, // ORDINAL_MONTH
};

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
 * Determine whether a Myanmar year is a leap year (either big or little watat)
 */
bool MyanmarCalendar::isLeapYear(int32_t year) {
    long watat_type, waso_type;
    cal_watat(year, watat_type, waso_type);
    return watat_type > 0;
}

/**
 * Return the day # on which the given year starts.  Days are counted
 * from the Myanmar epoch, origin 0.
 */
int32_t MyanmarCalendar::yearStart(int32_t year, UErrorCode& status) {
    return handleComputeMonthStart(year, 1, false, status);
}

/**
 * Return the day # on which the given month starts.  Days are counted
 * from the Myanmar epoch, origin 0.
 *
 * @param year  The Myanmar year
 * @param month The Myanmar month, 0-based
 */
int32_t MyanmarCalendar::monthStart(int32_t year, int32_t month, UErrorCode& status) const {
    return handleComputeMonthStart(year, month, true, status);
}

//----------------------------------------------------------------------
// Calendar framework
//----------------------------------------------------------------------

/**
 * Return the length (in days) of the given month.
 *
 * @param extendedYear  The Myanmar year
 * @param month The Myanmar month, 0-based
 */
int32_t MyanmarCalendar::handleGetMonthLength(int32_t extendedYear, int32_t month,
  UErrorCode& /*status*/) const {
    int32_t days[] = {
      29, // tagu
      30, // kason
      29, // nayon, non-leap year
      30, // waso
      0, // second waso, non-leap years
      29, // wagaung
      30, // tawthalin
      29, // THADINGYUT
      30, // TAZAUNGMON
      29, // NADAW
      30, // PYATHO
      29, // TABODWE
      30, // Tabaung
      29, // Late Tagu
      30, // Late Kason
    };

    int32_t myan_year_type;
    long startOfTagu, full_moon_waso_2;
    cal_my(extendedYear, myan_year_type, startOfTagu, full_moon_waso_2);

    int32_t mm_length = days[month];

    // second waso exists in all leap years
    if (myan_year_type > 0 && month == 4) {
      return 30;
    }

    // long leap year, Nayon is longer by 1 day
    if (myan_year_type >= 2 && month == 2) {
      mm_length = 30;
    }

    return mm_length;
}

/**
 * Return the length of the given Myanmar year in days.
 */
int32_t MyanmarCalendar::handleGetYearLength(int32_t eyear, UErrorCode& status) const {
    if (U_FAILURE(status)) return 0;
  int32_t myt;
  long startOfTagu, full_moon_waso_2;
  cal_my(eyear, myt, startOfTagu, full_moon_waso_2);
    long b = long(floor(myt / 2));
    long c = long(floor(1.0 / (myt + 1))); // 1 only when myt==0
    return 354 + (1 - c) * 30 + b;
}

int32_t MyanmarCalendar::getActualMaximum(UCalendarDateFields field, UErrorCode& status) const {
  if (U_FAILURE(status)) {
    return 0;
  }

  if (field == UCAL_DAY_OF_YEAR) {
    return 385;
  }

  return Calendar::getActualMaximum(field, status);
}

void MyanmarCalendar::cal_my(int32_t myan_year, int32_t& myan_year_type, long& startOfTagu, long& full_moon_waso_2) const {
  long prevYears = 0;
  long prev_year_watat = 0;
  long prev_year_full_moon_waso_2 = 0;
  long subject_year_watat = 0;
  long subject_year_full_moon_waso_2 = 0;
  cal_watat(myan_year, subject_year_watat, subject_year_full_moon_waso_2);
  myan_year_type = static_cast<int32_t>(subject_year_watat);

  do {
    prevYears++;
    cal_watat(myan_year - prevYears, prev_year_watat, prev_year_full_moon_waso_2);
  } while (prev_year_watat == 0 && prevYears < 3);

  if (myan_year_type != 0) {
    long year_length_diff = (subject_year_full_moon_waso_2 - prev_year_full_moon_waso_2) % 354;
    // JS reference: myt = floor((fm2 - fm1) % 354 / 31) + 1
    myan_year_type = static_cast<int32_t>(floor(static_cast<double>(year_length_diff) / 31.0) + 1);
    full_moon_waso_2 = subject_year_full_moon_waso_2;
  } else {
    full_moon_waso_2 = prev_year_full_moon_waso_2 + 354 * prevYears;
  }

  startOfTagu = prev_year_full_moon_waso_2 + 354 * prevYears - 102;
}

//-------------------------------------------------------------------------
// Functions for converting from field values to milliseconds....
//-------------------------------------------------------------------------

// Return JD of start of given month/year
int64_t MyanmarCalendar::handleComputeMonthStart(int32_t eyear, int32_t month, UBool /*useMonth*/, UErrorCode& /*status*/) const {
    int32_t myan_day_offset = -1;
    int32_t myan_year_type;
    long b, c, dayOfYear, year_length, monthType;
    long startOfTagu, full_moon_waso_2;

    cal_my(long(eyear), myan_year_type, startOfTagu, full_moon_waso_2);

    /* month order correction */
    if (month == 3) {
      month = 0;
    } else if (month < 3) {
      month++;
    }

    monthType = long(floor(month / 13));
    month = month % 13 + monthType; // to 0-12, monthType signifying greater
    b = long(floor(myan_year_type / 2));
    c = 1 - long(floor((myan_year_type + 1) / 2)); //if big watat and common year
    month += 4 - long(floor((month + 15) / 16)) * 4
            + long(floor((month + 12) / 16)); //adjust month
	  dayOfYear = myan_day_offset + long(floor(29.544 * month - 29.26))
            - c * long(floor((month + 11) / 16)) * 30
            + b * long(floor((month + 12) / 16));
	  year_length = 354 + (1 - c) * 30 + b;
    dayOfYear += monthType * year_length; // months past 12 get dayOfYear adjustment
	  return dayOfYear + startOfTagu;
}

//-------------------------------------------------------------------------
// Functions for converting from milliseconds to field values
//-------------------------------------------------------------------------

int32_t MyanmarCalendar::handleGetExtendedYear(UErrorCode& /* status */) {
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
    long startOfTagu, full_moon_waso_2, a, b, c, e, f;
    int32_t myan_year_type;
    long myan_year = long(floor((julianDay - 0.5 - MYANMAR_EPOCH) / SOLAR_YEAR)); //Myanmar year

    cal_my(myan_year, myan_year_type, startOfTagu, full_moon_waso_2);
    dayOfYear = julianDay - startOfTagu + 1;//day count
    b = long(floor(myan_year_type / 2));
    c = long(floor(1 / (myan_year_type + 1))); //big wa and common yr

    year_length = 354 + (1 - c) * 30 + b; //year length
    monthType = long(floor((dayOfYear - 1) / year_length)); //month type: late =1 or early = 0
    dayOfYear -= monthType * year_length;
    a = long(floor((dayOfYear + 423) / 512)); //adjust leap years
    int32_t myan_month = long(floor((dayOfYear - b * a + c * a * 30 + 29.26) / 29.544)); //month
    e = long(floor((myan_month + 12) / 16));
    f = long(floor((myan_month + 11) / 16));
    int32_t myan_day = dayOfYear - long(floor(29.544 * myan_month - 29.26))
          - b * e + c * f * 30;
    myan_month += f * 3 - e * 4 + 12 * monthType;

    /* month order correction */
    if (myan_month == 0) {
      myan_month = 3;
    } else if (myan_month <= 3) {
      myan_month--;
    }
    /* 2nd waso correction for non-leap years */
    if (myan_year_type == 0 && myan_month == 4) {
      myan_month = 3;
    }

    internalSet(UCAL_ERA, 2);
    internalSet(UCAL_YEAR, myan_year);
    internalSet(UCAL_EXTENDED_YEAR, myan_year);
    internalSet(UCAL_MONTH, myan_month);
    internalSet(UCAL_ORDINAL_MONTH, myan_month);
    internalSet(UCAL_DAY_OF_MONTH, myan_day);
    internalSet(UCAL_DAY_OF_YEAR, dayOfYear);
}

void MyanmarCalendar::GetMyConst(int32_t myan_year, double& era, double& WO, double& NM, long& EW) const {
	EW = 0;
	// The third era (the era after Independence 1312 ME and after)
	if (myan_year >= 1312) {
		era = 3;
    WO = -0.5;
    NM = 8;
    if (myan_year == 1377) {
      WO += 1;
    }
    if (myan_year == 1344 || myan_year == 1345) {
      EW = 1;
    }
	}
	// The second era (the era under British colony: 1217 ME - 1311 ME)
	else if (myan_year >= 1217) {
		era = 2;
    WO = -1;
    NM = 4;
    if (myan_year == 1234) {
      WO += 1;
    } else if (myan_year == 1261) {
      WO += -1;
    }
    if (myan_year == 1263 || myan_year == 1264) {
      EW = 1;
    }
  }
	// The first era (the era of Myanmar kings: ME1216 and before)
	// Thandeikta (ME 1100 - 1216)
	else if (myan_year >= 1100) {
		era = 1.3;
    WO = -0.85;
    NM = -1;
    if (myan_year == 1120 || myan_year == 1150 || myan_year == 1207) {
      WO += 1;
    } else if (myan_year == 1126 || myan_year == 1172) {
      WO += -1;
    }
    if (myan_year == 1201 || myan_year == 1202) {
      EW = 1;
    }
	}
	// Makaranta system 2 (ME 798 - 1099)
	else if (myan_year >= 798) {
		era = 1.2;
    WO = -1.1;
    NM = -1;
    if (myan_year == 813 || myan_year == 849 || myan_year == 851 || myan_year == 854 ||
        myan_year == 927 || myan_year == 933 || myan_year == 936 || myan_year == 938 ||
        myan_year == 949 || myan_year == 952 || myan_year == 963 || myan_year == 968 ||
        myan_year == 1039) {
      WO += -1;
    }
	}
	// Makaranta system 1 (ME 0 - 797)
	else {
		era = 1.1;
    WO = -1.1;
    NM = -1;
    if (myan_year == 205 || myan_year == 246 || myan_year == 471 || myan_year == 651 ||
        myan_year == 656 || myan_year == 672 || myan_year == 729) {
      WO += 1;
    } else if (myan_year == 653) {
      WO += 2;
    } else if (myan_year == 572 || myan_year == 767) {
      WO += -1;
    }
	}
}

void MyanmarCalendar::cal_watat(int32_t myan_year, long& watat, long& full_moon_waso_2) const {
	double era, WO, NM, excess_days;
  long EW;
	GetMyConst(myan_year, era, WO, NM, EW); // get constants for the corresponding calendar era
	double TA = (SOLAR_YEAR / 12 - LUNAR_MONTH) * (12 - NM);
  excess_days = fmod(SOLAR_YEAR*(myan_year+3739),LUNAR_MONTH);
	if (excess_days < TA) {
    excess_days += LUNAR_MONTH; //adjust excess days
  }
	full_moon_waso_2 = long(round(SOLAR_YEAR * myan_year + MYANMAR_EPOCH - excess_days + 4.5 * LUNAR_MONTH + WO));
	double TW = 0;
  watat = 0;
	if (era >= 2) {
		TW = LUNAR_MONTH - (SOLAR_YEAR / 12 - LUNAR_MONTH) * NM;
		if (excess_days >= TW) {
      watat = 1;
    }
	}
	else {//if 1st era,find watat by 19 years metonic cycle
	//Myanmar year is divided by 19 and there is intercalary month
	//if the remainder is 2,5,7,10,13,15,18
	//https://github.com/kanasimi/CeJS/blob/master/data/date/calendar.js#L2330
		watat = (myan_year * 7 + 2) % 19;
    if (watat < 0) {
      watat += 19;
    }
		watat = long(floor(watat / 12));
	}
	watat^=EW;//correct watat exceptions
}

// default century

static UDate           gSystemDefaultCenturyStart       = DBL_MIN;
static int32_t         gSystemDefaultCenturyStartYear   = -1;
static icu::UInitOnce  gSystemDefaultCenturyInit        = {};

UBool MyanmarCalendar::haveDefaultCentury() const
{
    return true;
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
