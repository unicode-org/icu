/*
*******************************************************************************
* Copyright (C) 2003-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File BUDDHCAL.CPP
*
* Modification History:
*  05/13/2003    srl     copied from gregocal.cpp
*
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "buddhcal.h"
#include "unicode/gregocal.h"
#include "mutex.h"
#include <float.h>

U_NAMESPACE_BEGIN

const char BuddhistCalendar::fgClassID = 0; // Value is irrelevant

static const int32_t kMaxEra = 0; // only 1 era

static const int32_t kBuddhistEraStart = -543;  // 544 BC (Gregorian)

static const int32_t kGregorianEpoch = 1970; 

BuddhistCalendar::BuddhistCalendar(const Locale& aLocale, UErrorCode& success)
  :   GregorianCalendar(aLocale, success)
{
    setTimeInMillis(getNow(), success); // Call this again now that the vtable is set up properly.
}

BuddhistCalendar::~BuddhistCalendar()
{
}

BuddhistCalendar::BuddhistCalendar(const BuddhistCalendar& source)
  : GregorianCalendar(source)
{
}

BuddhistCalendar& BuddhistCalendar::operator= ( const BuddhistCalendar& right)
{
  GregorianCalendar::operator=(right);
  return *this;
}

Calendar* BuddhistCalendar::clone(void) const
{
  return new BuddhistCalendar(*this);
}

const char *BuddhistCalendar::getType() const
{
  return "buddhist";
}

int32_t
BuddhistCalendar::getMaximum(UCalendarDateFields field) const
{
  if(field == UCAL_ERA) {
    return kMaxEra;
  } else {
    return GregorianCalendar::getMaximum(field);
  }
}

int32_t
BuddhistCalendar::getLeastMaximum(UCalendarDateFields field) const
{
  if(field == UCAL_ERA) {
    return kMaxEra;
  } else {
    return GregorianCalendar::getLeastMaximum(field);
  }
}

int32_t
BuddhistCalendar::monthLength(int32_t month, int32_t year) const
{
  return GregorianCalendar::monthLength(month,year);
}


int32_t
BuddhistCalendar::monthLength(int32_t month) const
{
    UErrorCode status = U_ZERO_ERROR;
    // ignore era
    return GregorianCalendar::monthLength(month, getGregorianYear(status));
}

int32_t BuddhistCalendar::internalGetEra() const
{
  return internalGet(UCAL_ERA, BE);
}

int32_t
BuddhistCalendar::getGregorianYear(UErrorCode &status)  const
{
  int32_t year = (fStamp[UCAL_YEAR] != kUnset) ? internalGet(UCAL_YEAR) : kGregorianEpoch+kBuddhistEraStart;
  int32_t era = BE;
  if (fStamp[UCAL_ERA] != kUnset) {
    era = internalGet(UCAL_ERA);
    if (era != BE) {
      status = U_ILLEGAL_ARGUMENT_ERROR;
      return kGregorianEpoch + kBuddhistEraStart;
    }
  }
  return year + kBuddhistEraStart;
}

int32_t BuddhistCalendar::handleGetExtendedYear()
{
  int32_t year;
  if (newerField(UCAL_EXTENDED_YEAR, UCAL_YEAR) == UCAL_EXTENDED_YEAR) {
    year = internalGet(UCAL_EXTENDED_YEAR, 1);
  } else {
    // Ignore the era, as there is only one
    year = internalGet(UCAL_YEAR, 1);
  }
  return year;
}

int32_t BuddhistCalendar::handleComputeMonthStart(int32_t eyear, int32_t month,

                                                   UBool useMonth) const
{
  return GregorianCalendar::handleComputeMonthStart(eyear+kBuddhistEraStart, month, useMonth);
}

void BuddhistCalendar::handleComputeFields(int32_t julianDay, UErrorCode& status)
{
  GregorianCalendar::handleComputeFields(julianDay, status);
  int32_t y = internalGet(UCAL_EXTENDED_YEAR) - kBuddhistEraStart;
  internalSet(UCAL_EXTENDED_YEAR, y);
  internalSet(UCAL_ERA, 0);
  internalSet(UCAL_YEAR, y);
}

int32_t BuddhistCalendar::handleGetLimit(UCalendarDateFields field, ELimitType limitType) const
{
  if(field == UCAL_ERA) {
    return BE;
  } else {
    return GregorianCalendar::handleGetLimit(field,limitType);
  }
}

#if 0
void BuddhistCalendar::timeToFields(UDate theTime, UBool quick, UErrorCode& status)
{
  //Calendar::timeToFields(theTime, quick, status);

  int32_t era = internalGet(UCAL_ERA);
  int32_t year = internalGet(UCAL_YEAR);

  if(era == GregorianCalendar::BC) {
    year = 1-year;
    era = BuddhistCalendar::BE;
  } else if(era == GregorianCalendar::AD) {
    era = BuddhistCalendar::BE;
  } else {
    status = U_INTERNAL_PROGRAM_ERROR;
  }

  year = year - kBuddhistEraStart;
    
  internalSet(UCAL_ERA, era);
  internalSet(UCAL_YEAR, year);
}
#endif

void BuddhistCalendar::add(UCalendarDateFields field, int32_t amount, UErrorCode& status)
{
    if (U_FAILURE(status)) 
        return;

    if (amount == 0) 
        return;   // Do nothing!
    
    if(field == UCAL_YEAR /* || field == UCAL_YEAR_WOY */) {
        int32_t year = get(field, status); // not internalGet -- force completion

        year += amount;
        
        set(field,year);
        pinDayOfMonth();
    } else {
      GregorianCalendar::add(field,amount,status);
    }
}



// default century
const UDate     BuddhistCalendar::fgSystemDefaultCentury        = DBL_MIN;
const int32_t   BuddhistCalendar::fgSystemDefaultCenturyYear    = -1;

UDate           BuddhistCalendar::fgSystemDefaultCenturyStart       = DBL_MIN;
int32_t         BuddhistCalendar::fgSystemDefaultCenturyStartYear   = -1;


UBool BuddhistCalendar::haveDefaultCentury() const
{
  return TRUE;
}

UDate BuddhistCalendar::defaultCenturyStart() const
{
  return internalGetDefaultCenturyStart();
}

int32_t BuddhistCalendar::defaultCenturyStartYear() const
{
  return internalGetDefaultCenturyStartYear();
}

UDate
BuddhistCalendar::internalGetDefaultCenturyStart() const
{
  // lazy-evaluate systemDefaultCenturyStart
  UBool needsUpdate;
  { 
    Mutex m;
    needsUpdate = (fgSystemDefaultCenturyStart == fgSystemDefaultCentury);
  }

  if (needsUpdate) {
    initializeSystemDefaultCentury();
  }

  // use defaultCenturyStart unless it's the flag value;
  // then use systemDefaultCenturyStart
  
  return fgSystemDefaultCenturyStart;
}

int32_t
BuddhistCalendar::internalGetDefaultCenturyStartYear() const
{
  // lazy-evaluate systemDefaultCenturyStartYear
  UBool needsUpdate;
  { 
    Mutex m;
    needsUpdate = (fgSystemDefaultCenturyStart == fgSystemDefaultCentury);
  }

  if (needsUpdate) {
    initializeSystemDefaultCentury();
  }

  // use defaultCenturyStart unless it's the flag value;
  // then use systemDefaultCenturyStartYear
  
  return    fgSystemDefaultCenturyStartYear;
}

void
BuddhistCalendar::initializeSystemDefaultCentury()
{
  // initialize systemDefaultCentury and systemDefaultCenturyYear based
  // on the current time.  They'll be set to 80 years before
  // the current time.
  // No point in locking as it should be idempotent.
  if (fgSystemDefaultCenturyStart == fgSystemDefaultCentury)
  {
    UErrorCode status = U_ZERO_ERROR;
    BuddhistCalendar calendar(Locale("th_TH_TRADITIONAL"),status);
    if (U_SUCCESS(status))
    {
      calendar.setTime(Calendar::getNow(), status);
      calendar.add(UCAL_YEAR, -80, status);
      UDate    newStart =  calendar.getTime(status);
      int32_t  newYear  =  calendar.get(UCAL_YEAR, status);
      {
        Mutex m;
        fgSystemDefaultCenturyStart = newStart;
        fgSystemDefaultCenturyStartYear = newYear;
      }
    }
    // We have no recourse upon failure unless we want to propagate the failure
    // out.
  }
}


U_NAMESPACE_END

#endif
