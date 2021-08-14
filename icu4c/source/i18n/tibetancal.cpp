// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 * Copyright (C) 2003-2021, International Business Machines Corporation
 * and others. All Rights Reserved.
 ******************************************************************************
 *
 * File TIBETANCAL.CPP
 *****************************************************************************
 */

#include "tibetancal.h"
#include <stdlib.h>
#if !UCONFIG_NO_FORMATTING

#include "umutex.h"
#include <float.h>
#include "gregoimp.h" // Math
#include "uhash.h"

// Debugging
#ifdef U_DEBUG_TIBETANCAL
#include <stdio.h>
#include <stdarg.h>

#endif

U_NAMESPACE_BEGIN

// Implementation of the TibetanCalendar class

//-------------------------------------------------------------------------
// Constructors...
//-------------------------------------------------------------------------


TibetanCalendar* TibetanCalendar::clone() const {
  return new TibetanCalendar(*this);
}

TibetanCalendar::TibetanCalendar(const Locale& aLocale, UErrorCode& success)
  :   Calendar(TimeZone::forLocaleOrDefault(aLocale), aLocale, success)
{
  setTimeInMillis(getNow(), success); // Call this again now that the vtable is set up properly.
}

TibetanCalendar::TibetanCalendar(const TibetanCalendar& other) : Calendar(other) {
}

TibetanCalendar::~TibetanCalendar()
{
}

/**
 * return the calendar type, "tibetan" or "tibetan-tsurphu".
 *
 * @return calendar type
 * @internal
 */
const char *TibetanCalendar::getType() const { 
   const char *sType = NULL;

   switch (cType) {
    case PHUGPA:
      sType = 'tibetan';
      break;
    case TSURPHU:
      sType = 'tibetan-tsurphu';
      break;
    default:
      UPRV_UNREACHABLE; // out of range
   }
   return sType;
}


/**
 * Sets Tibetan calendar calculation type used by this instance.
 *
 * @param type    The calendar calculation type, <code>PHUGPA</code> to use the phugpa
 *                calendar, <code>TSURPHU</code> to use the tsurphu calendar.
 * @internal
 */
void TibetanCalendar::setCalculationType(ECalculationType type, UErrorCode &status)
{
    if (cType != type) {
        // The fields of the calendar will become invalid, because the calendar
        // rules are different
        UDate m = getTimeInMillis(status);
        cType = type;
        clear();
        setTimeInMillis(m, status);
    }
}


/**
* Returns <code>true</code> if this object is using the phugpa
* calendar, or <code>false</code> if using the tsurphu calendar.
*/
UBool TibetanCalendar::isPhugpa() {
    return (cType == PHUGPA);
}



//-------------------------------------------------------------------------
// Assorted calculation utilities
//-------------------------------------------------------------------------

// fixed tables
int32_t SUN_TAB[] = {0, 6, 10, 11};
int32_t MOON_TAB[] = {0, 5, 10, 15, 19, 22, 24, 25};

// mean date
double PHUGPA_M0 = 2015501 + 4783.0/5656;
double TSURPHU_M0 = 2353745 + 1795153 / 7635600;
double M1 = 167025.0/5656;
double M2 = M1/30;

// mean sun
double PHUGPA_S0 = 743.0/804;
double TSURPHU_S0 = -5983/108540;
double S1 = 65.0/804;
double S2 = S1/30;

// anomaly moon
double PHUGPA_A0 = 475.0/3528;
double TSURPHU_A0 = 207 / 392;;
double A1 = 253.0/3528;
double A2 = 1.0/28;

int32_t PHUGPA_YEAR0 = 806;
int32_t PHUGPA_MONTH0 = 3;
int32_t PHUGPA_BETA_STAR = 61;
double PHUGPA_ALPHA = 1 + 827.0/1005;

int32_t TSURPHU_YEAR0 = 1732;
int32_t TSURPHU_MONTH0 = 3;
int32_t TSURPHU_BETA_STAR = 59;
double TSURPHU_ALPHA = 2 + 1052/9045;

/**
 * Return the moonTab by using the table for the given integer value. 
 * @param moonTab An integer number
 */
int32_t TibetanCalendar::moonTab(int32_t moonTab) const {
    moonTab = (moonTab % 28 + 28) % 28;
    if(moonTab <= 7){
        return MOON_TAB[moonTab];
    }else if(moonTab <= 14){
        return MOON_TAB[14 - moonTab];
    }else if(moonTab <= 21){
        return -MOON_TAB[moonTab - 14];
    }else{
        return -MOON_TAB[28 - moonTab];
    }
}


/**
 * Return the sunTab by using the table for the given integer value. 
 * @param sunTab An integer number
 */
int32_t TibetanCalendar::sunTab(int32_t sunTab) const {
    sunTab = (sunTab % 12 +12) % 12;
    if(sunTab <= 3){
        return SUN_TAB[sunTab];
    }else if(sunTab <= 6){
        return SUN_TAB[6 - sunTab];
    }else if(sunTab <= 9){
        return -SUN_TAB[sunTab - 6];
    }else{
        return -SUN_TAB[12 - sunTab];
    }
}


/**
 * Return the julian date at the end of the lunar day (similar to the month count since the beginning of the epoch, but for days)
 * It is calculated by first calculating a simpler mean date corresponding to the linear mean motion of the moon and then adjusting 
 * it by the equations of the sun and moon, which are determined by the anomalies of the sun and moon together with tables.
 * @param day the tibetan day
 * @param monthCount month count since the begining of epoch
 */
int32_t TibetanCalendar::trueDate(int32_t day, int32_t monthCount) const {

    double meanDate =  monthCount*M1 + day*M2 + (isPhugpa()?PHUGPA_M0:TSURPHU_M0);
    double moonAnomaly = 28.0*(monthCount*A1 + day*A2 + (isPhugpa()?PHUGPA_A0:TSURPHU_A0));
    int32_t mu = moonTab((int)ceil(moonAnomaly)); 
    int32_t md = moonTab((int)floor(moonAnomaly));
    double moonEqu = (md + (moonAnomaly - floor(moonAnomaly))*(mu - md))/60.0;
    double sunAnomaly = 12.0*(monthCount*S1 + day*S2 + (isPhugpa()?PHUGPA_S0:TSURPHU_S0) - 1.0/4);
    int32_t su = sunTab((int)ceil(sunAnomaly));
    int32_t sd = sunTab((int)floor(sunAnomaly));
    double sunEqu = (sd +(sunAnomaly - floor(sunAnomaly))*(su - sd))/60.0;

    return (int)floor(meanDate + moonEqu - sunEqu);
}


/**
 * Return the month count(based on the epoch) from the given Tibetan year, month number and leap month indicator.
 * @param month the month number
 * @param is_leap_month flag indicating whether or not the given month is leap month
 */
int32_t TibetanCalendar::toMonthCount(int32_t eyear, int32_t month, int32_t is_leap_month) {

    return floor((12*(eyear-127-(isPhugpa()?PHUGPA_YEAR0:TSURPHU_YEAR0)) + month - (isPhugpa()?PHUGPA_ALPHA:TSURPHU_ALPHA) - (1.0-12.0*S1)*is_leap_month)/(12.0*S1));
}


/**
 * Return the modulo of the number if(num % mod != 0) else is return mod.
 * @param num the number to be divided
 * @param mod the number to be divided with
 */
int32_t TibetanCalendar::amod(int32_t num, int32_t mod) {

    if((num % mod) == 0) return mod;
    return (num % mod + mod) % mod;
}


//-------------------------------------------------------------------------
// Minimum / Maximum access functions
//-------------------------------------------------------------------------


static const int32_t LIMITS[UCAL_FIELD_COUNT][4] = {
    // Minimum  Greatest     Least    Maximum
    //           Minimum   Maximum
    {        1,        1,    83333,    83333}, // ERA 
    {        1,        1,       60,       60}, // YEAR
    {        0,        0,       11,       11}, // MONTH
    {        1,        1,       50,       55}, // WEEK_OF_YEAR
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // WEEK_OF_MONTH
    {        1,        1,       29,       30}, // DAY_OF_MONTH
    {        1,        1,      354,      384}, // DAY_OF_YEAR
    {/*N/A*/-1,/*N/A*/-1,/*N/A*/-1,/*N/A*/-1}, // DAY_OF_WEEK
    {       -1,       -1,        5,        5}, // DAY_OF_WEEK_IN_MONTH
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
    {        0,        0,        1,        1}, // IS_LEAP_MONTH
};


/**
 * Override Calendar method to return the number of days in the given
 * extended year and month.
 *
 * <p>Note: This method also reads the IS_LEAP_MONTH field to determine
 * whether or not the given month is a leap month.
 * @stable ICU 2.8
 */
int32_t TibetanCalendar::handleGetMonthLength(int32_t eyear, int32_t month) const {
    int32_t is_leap_month = (month>=(1<<6) ? 1 : 0);
    int32_t trueMonth = (is_leap_month ? (month-(1<<6)) : month);
    int32_t monthCount = toMonthCount(eyear, trueMonth, is_leap_month);

    int32_t thisStart = trueDate(30,monthCount-1);
    int32_t nextStart = trueDate(30,monthCount);

    return nextStart - thisStart;
}


/**
 * Return JD of start of given month/year.
 * @internal
 */
 int32_t TibetanCalendar::handleComputeMonthStart(int32_t eyear, int32_t month, UBool useMonth) const {
     UErrorCode status = U_ZERO_ERROR;
 
     int32_t is_leap_month = (month>=(1<<6) ? 1 : 0);
     int32_t trueMonth = (is_leap_month ? (month-(1<<6)) : month);
     int32_t monthCount = toMonthCount(eyear, trueDate, is_leap_month);
 
     return (int)(trueDate(30, monthCount-1));
 }


/**
 * Override Calendar to handle leap months and leap days properly.
 */
void TibetanCalendar::add(UCalendarDateFields field, int32_t amount, UErrorCode& status) {
    switch (field) {
    case UCAL_MONTH:
        int32_t month = internalGet(UCAL_MONTH);
        int32_t is_leap_month = (month>=(1<<6) ? 1 : 0);
        int32_t trueMonth = (is_leap_month ? (month-(1<<6)) : month);
        int32_t monthCount = toMonthCount(eyear, trueMonth, is_leap_month);     
        newMonthCount = monthCount + amount;
        int32_t julianDay = trueDate(internalGet(UCAL_DAY_OF_MONTH),newMonthCount);
        handleComputeFields(julianDay, status); 
        break;
    case UCAL_DAY_OF_MONTH:
        int32_t julianDay = internalGet(UCAL_JULIAN_DAY);
        int32_t newJulianDay = julianDay + amount;
        handleComputeFields(newJulianDay, status);
        break;
    default:
        Calendar::add(field, amount, status);
        break;
    }
}


/**
 * Override Calendar to handle leap months and leap days properly.
 */
void TibetanCalendar::add(EDateFields field, int32_t amount, UErrorCode& status) {
    add((UCalendarDateFields)field, amount, status);
}


/**
 * Override Calendar to handle leap months and leap days properly.
 */
void TibetanCalendar::roll(UCalendarDateFields field, int32_t amount, UErrorCode& status) {
    switch (field) {
    case UCAL_MONTH:
        int32_t month = internalGet(UCAL_MONTH);
        int32_t is_leap_month = (month>=(1<<6) ? 1 : 0);
        int32_t trueMonth = (is_leap_month ? (month-(1<<6)) : month);
        int32_t monthCount = toMonthCount(eyear, trueMonth, is_leap_month);     
        newMonthCount = monthCount + amount;
        int32_t julianDay = trueDate(internalGet(UCAL_DAY_OF_MONTH),newMonthCount);
        handleComputeFields(julianDay, status); 
        break;
    case UCAL_DAY_OF_MONTH:
        int32_t julianDay = internalGet(UCAL_JULIAN_DAY);
        int32_t newJulianDay = julianDay + amount;
        handleComputeFields(newJulianDay, status);
        break;
    default:
        Calendar::add(field, amount, status);
        break;
    }
}


/**
 * Override Calendar to handle leap months and leap days properly.
 */
void TibetanCalendar::roll(EDateFields field, int32_t amount, UErrorCode& status) {
    roll((UCalendarDateFields)field, amount, status);
}


/**
  * Subclasses may override this method to compute several fields
  * specific to each calendar system.  These are:
  *
  * <ul><li>ERA
  * <li>YEAR
  * <li>MONTH
  * <li>DAY_OF_MONTH
  * <li>DAY_OF_YEAR
  * <li>EXTENDED_YEAR</ul>
  *
  * <p>The GregorianCalendar implementation implements
  * a calendar with the specified Julian/Gregorian cutover date.
  * @internal
  */
void TibetanCalendar::handleComputeFields(int32_t julianDay, UErrorCode &status) {
    int32_t gyear = getGregorianYear();
    int32_t dn1 = 1 + 30*toMonthCount(gyear + 126, 1, 1);
    int32_t dn2 = 1 + 30*toMonthCount(gyear + 128, 1, 1);

    int32_t jd1 = trueDate(amod(dn1, 30), floor((dn1 - 1)/30));
    int32_t jd2 = trueDate(amod(dn2, 30), floor((dn2 - 1)/30));

    while (dn1 < dn2 - 1  && jd1 < jd2 - 1){
        int32_t ndn = (dn1 + dn2)>>1;
        int32_t njd = trueDate(amod(ndn, 30), floor((ndn - 1)/30));

        if(njd < julianDay){
            dn1 = ndn;
            jd1 = njd;
        } else {
            dn2 = ndn;
            jd2 = njd;
        }
    }

    if(jd1 == julianDay){
        jd2 = jd1;
        dn2 = dn1;
    }
    int32_t monthCount = (dn2-1)/30;
    int32_t x = ceil(12*S1*monthCount + (isPhugpa()?PHUGPA_ALPHA:TSURPHU_ALPHA));

    int32_t tmonth = amod(x, 12);
    int32_t tyear = (x - tmonth)/12 + (isPhugpa()?PHUGPA_YEAR0?TSURPHU_YEAR0) + 127;
    bool is_leap_month = (ceil(12*S1*(monthCount + 1) + (isPhugpa()?PHUGPA_ALPHA:TSURPHU_ALPHA)) == x);
    int32_t tday = amod(dn2, 30);
    bool is_leap_day = (jd2 > julianDay);
    int32_t dayOfYear = julianDay - trueDate(30, toMonthCount(tyear - 1, 12, 0));

    int32_t cycleNumber = ceil((tyear-1153.0)/60.0);
    int32_t yearNumber = amod(tyear-133,60);
    
    if(is_leap_day){
        tday += (1<<6);
    }

    if(is_leap_month){
        tmonth += (1<<6);
    }

    internalSet(UCAL_ERA, cycleNumber);
    internalSet(UCAL_YEAR, yearNumber);
    internalSet(UCAL_EXTENDED_YEAR, tyear);
    internalSet(UCAL_MONTH, tmonth);
    internalSet(UCAL_DAY_OF_MONTH, tday);
    internalSet(UCAL_DAY_OF_YEAR, dayOfYear);
    internalSet(UCAL_JULIAN_DAY,julianDay);

}


/**
 * Implement abstract Calendar method to return the extended year
 * defined by the current fields.  This will use either the ERA and
 * YEAR field as the cycle and year-of-cycle, or the EXTENDED_YEAR
 * field as the continuous year count, depending on which is newer.
 */
 int32_t TibetanCalendar::handleGetExtendedYear() {
    int32_t year;
    if (newestStamp(UCAL_ERA, UCAL_YEAR, kUnset) <= fStamp[UCAL_EXTENDED_YEAR]) {
        year = internalGet(UCAL_EXTENDED_YEAR, 1); // Default to year 1
    } else {
        int32_t cycle = internalGet(UCAL_ERA, 1) - 1; // 0-based cycle
        // adjust to the instance specific epoch
        year = cycle * 60 + internalGet(UCAL_YEAR, 1) - (fEpochYear - (isPhugpa()?PHUGPA_YEAR0?TSURPHU_YEAR0) );
    }
    return year;
 }


/**
 * @internal
 */
 int32_t TibetanCalendar::handleGetLimit(UCalendarDateFields field, ELimitType limitType) const {
     return LIMITS[field][limitType];
 }


UBool
TibetanCalendar::inDaylightTime(UErrorCode& status) const
{
    // copied from GregorianCalendar
    if (U_FAILURE(status) || !getTimeZone().useDaylightTime()) {
        return FALSE;
    }

    // Force an update of the state of the Calendar.
    ((TibetanCalendar*)this)->complete(status); // cast away const

    return (UBool)(U_SUCCESS(status) ? (internalGet(UCAL_DST_OFFSET) != 0) : FALSE);
}


/**
 * The system maintains a static default century start date and Year.  They are
 * initialized the first time they are used.  Once the system default century date
 * and year are set, they do not change.
 */
static UDate           gSystemDefaultCenturyStart       = DBL_MIN;
static int32_t         gSystemDefaultCenturyStartYear   = -1;
static icu::UInitOnce  gSystemDefaultCenturyInit        = U_INITONCE_INITIALIZER;


UBool TibetanCalendar::haveDefaultCentury() const
{
    return TRUE;
}


static void U_CALLCONV initializeSystemDefaultCentury()
{
    // initialize systemDefaultCentury and systemDefaultCenturyYear based
    // on the current time.  They'll be set to 80 years before
    // the current time.
    UErrorCode status = U_ZERO_ERROR;

    if(isPhugpa()){
        TibetanCalendar calendar ( Locale ( "@calendar=tibetan" ), status);
    } else {
        TibetanCalendar calendar ( Locale ( "@calendar=tibetan-tsurphu" ), status);       
    }
    if ( U_SUCCESS ( status ) ) {
        calendar.setTime ( Calendar::getNow(), status );
        calendar.add ( UCAL_YEAR, -80, status );

        UDate    newStart = calendar.getTime ( status );
        int32_t  newYear  = calendar.get ( UCAL_YEAR, status );

        gSystemDefaultCenturyStart = newStart;
        gSystemDefaultCenturyStartYear = newYear;
    }
    // We have no recourse upon failure.
}


UDate TibetanCalendar::defaultCenturyStart() const
{
    // lazy-evaluate systemDefaultCenturyStart
    umtx_initOnce(gSystemDefaultCenturyInit, &initializeSystemDefaultCentury);
    return gSystemDefaultCenturyStart;
}


int32_t TibetanCalendar::defaultCenturyStartYear() const
{
    // lazy-evaluate systemDefaultCenturyStartYear
    umtx_initOnce(gSystemDefaultCenturyInit, &initializeSystemDefaultCentury);
    return    gSystemDefaultCenturyStartYear;
}


UOBJECT_DEFINE_RTTI_IMPLEMENTATION(TibetanCalendar)

U_NAMESPACE_END

#endif