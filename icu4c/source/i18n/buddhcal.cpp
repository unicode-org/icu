/*
*******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and    *
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


U_NAMESPACE_BEGIN

const char BuddhistCalendar::fgClassID = 0; // Value is irrelevant

static const int32_t kMaxEra = 0; // only 1 era

static const int32_t kBuddhistEraStart = -543;  // 544 BC (Gregorian)

static const int32_t kGregorianEpoch = 1970; 

BuddhistCalendar::BuddhistCalendar(const Locale& aLocale, UErrorCode& success)
  :   GregorianCalendar(aLocale, success)
{
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
    int32_t year = internalGet(UCAL_YEAR);
    // ignore era
    return monthLength(month, year);
}

#if 0
UBool 
BuddhistCalendar::isLeapYear(int32_t year) const
{
    return (year >= fGregorianCutoverYear ?
        ((year%4 == 0) && ((year%100 != 0) || (year%400 == 0))) : // Gregorian
        (year%4 == 0)); // Julian
}
#endif

int32_t BuddhistCalendar::internalGetEra() const
{
    return isSet(UCAL_ERA) ? internalGet(UCAL_ERA) : BE;  
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

void BuddhistCalendar::timeToFields(UDate theTime, UBool quick, UErrorCode& status)
{
  GregorianCalendar::timeToFields(theTime, quick, status);

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

UBool BuddhistCalendar::haveDefaultCentury() const
{
  return FALSE;
}


U_NAMESPACE_END

#endif
