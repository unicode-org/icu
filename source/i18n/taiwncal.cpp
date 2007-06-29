/*
*******************************************************************************
* Copyright (C) 2003-2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File TAIWNCAL.CPP
*
* Modification History:
*  05/13/2003    srl     copied from gregocal.cpp
*  06/29/2007    srl     copied from buddhcal.cpp
*
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "taiwncal.h"
#include "unicode/gregocal.h"
#include "umutex.h"
#include <float.h>

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(TaiwanCalendar)

static const int32_t kMaxEra = 0; // only 1 era

static const int32_t kTaiwanEraStart = 1911;  // 1911 (Gregorian)

static const int32_t kGregorianEpoch = 1970; 

TaiwanCalendar::TaiwanCalendar(const Locale& aLocale, UErrorCode& success)
:   GregorianCalendar(aLocale, success)
{
    setTimeInMillis(getNow(), success); // Call this again now that the vtable is set up properly.
}

TaiwanCalendar::~TaiwanCalendar()
{
}

TaiwanCalendar::TaiwanCalendar(const TaiwanCalendar& source)
: GregorianCalendar(source)
{
}

TaiwanCalendar& TaiwanCalendar::operator= ( const TaiwanCalendar& right)
{
    GregorianCalendar::operator=(right);
    return *this;
}

Calendar* TaiwanCalendar::clone(void) const
{
    return new TaiwanCalendar(*this);
}

const char *TaiwanCalendar::getType() const
{
    return "taiwan";
}

int32_t
TaiwanCalendar::getMaximum(UCalendarDateFields field) const
{
    if(field == UCAL_ERA) {
        return kMaxEra;
    } else {
        return GregorianCalendar::getMaximum(field);
    }
}

int32_t
TaiwanCalendar::getLeastMaximum(UCalendarDateFields field) const
{
    if(field == UCAL_ERA) {
        return kMaxEra;
    } else {
        return GregorianCalendar::getLeastMaximum(field);
    }
}

int32_t
TaiwanCalendar::monthLength(int32_t month, int32_t year) const
{
    return GregorianCalendar::monthLength(month,year);
}


int32_t
TaiwanCalendar::monthLength(int32_t month) const
{
    UErrorCode status = U_ZERO_ERROR;
    // ignore era
    return GregorianCalendar::monthLength(month, getGregorianYear(status));
}

int32_t TaiwanCalendar::internalGetEra() const
{
    return internalGet(UCAL_ERA, MINGUO);
}

int32_t
TaiwanCalendar::getGregorianYear(UErrorCode &status)  const
{
    int32_t year = (fStamp[UCAL_YEAR] != kUnset) ? internalGet(UCAL_YEAR) : kGregorianEpoch+kTaiwanEraStart;
    int32_t era = MINGUO;
    if (fStamp[UCAL_ERA] != kUnset) {
        era = internalGet(UCAL_ERA);
        if (era != MINGUO) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return kGregorianEpoch + kTaiwanEraStart;
        }
    }
    return year + kTaiwanEraStart;
}

int32_t TaiwanCalendar::handleGetExtendedYear()
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

int32_t TaiwanCalendar::handleComputeMonthStart(int32_t eyear, int32_t month,

                                                  UBool useMonth) const
{
    return GregorianCalendar::handleComputeMonthStart(eyear+kTaiwanEraStart, month, useMonth);
}

void TaiwanCalendar::handleComputeFields(int32_t julianDay, UErrorCode& status)
{
    GregorianCalendar::handleComputeFields(julianDay, status);
    int32_t y = internalGet(UCAL_EXTENDED_YEAR) - kTaiwanEraStart;
    internalSet(UCAL_EXTENDED_YEAR, y);
    internalSet(UCAL_ERA, 0);
    internalSet(UCAL_YEAR, y);
}

int32_t TaiwanCalendar::handleGetLimit(UCalendarDateFields field, ELimitType limitType) const
{
    if(field == UCAL_ERA) {
        return MINGUO;
    } else {
        return GregorianCalendar::handleGetLimit(field,limitType);
    }
}

#if 0
void TaiwanCalendar::timeToFields(UDate theTime, UBool quick, UErrorCode& status)
{
    //Calendar::timeToFields(theTime, quick, status);

    int32_t era = internalGet(UCAL_ERA);
    int32_t year = internalGet(UCAL_YEAR);

    if(era == GregorianCalendar::BC) {
        year = 1-year;
        era = TaiwanCalendar::MINGUO;
    } else if(era == GregorianCalendar::AD) {
        era = TaiwanCalendar::MINGUO;
    } else {
        status = U_INTERNAL_PROGRAM_ERROR;
    }

    year = year - kTaiwanEraStart;

    internalSet(UCAL_ERA, era);
    internalSet(UCAL_YEAR, year);
}
#endif

void TaiwanCalendar::add(UCalendarDateFields field, int32_t amount, UErrorCode& status)
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
const UDate     TaiwanCalendar::fgSystemDefaultCentury        = DBL_MIN;
const int32_t   TaiwanCalendar::fgSystemDefaultCenturyYear    = -1;

UDate           TaiwanCalendar::fgSystemDefaultCenturyStart       = DBL_MIN;
int32_t         TaiwanCalendar::fgSystemDefaultCenturyStartYear   = -1;


UBool TaiwanCalendar::haveDefaultCentury() const
{
    return TRUE;
}

UDate TaiwanCalendar::defaultCenturyStart() const
{
    return internalGetDefaultCenturyStart();
}

int32_t TaiwanCalendar::defaultCenturyStartYear() const
{
    return internalGetDefaultCenturyStartYear();
}

UDate
TaiwanCalendar::internalGetDefaultCenturyStart() const
{
    // lazy-evaluate systemDefaultCenturyStart
    UBool needsUpdate;
    UMTX_CHECK(NULL, (fgSystemDefaultCenturyStart == fgSystemDefaultCentury), needsUpdate);

    if (needsUpdate) {
        initializeSystemDefaultCentury();
    }

    // use defaultCenturyStart unless it's the flag value;
    // then use systemDefaultCenturyStart

    return fgSystemDefaultCenturyStart;
}

int32_t
TaiwanCalendar::internalGetDefaultCenturyStartYear() const
{
    // lazy-evaluate systemDefaultCenturyStartYear
    UBool needsUpdate;
    UMTX_CHECK(NULL, (fgSystemDefaultCenturyStart == fgSystemDefaultCentury), needsUpdate);

    if (needsUpdate) {
        initializeSystemDefaultCentury();
    }

    // use defaultCenturyStart unless it's the flag value;
    // then use systemDefaultCenturyStartYear

    return    fgSystemDefaultCenturyStartYear;
}

void
TaiwanCalendar::initializeSystemDefaultCentury()
{
    // initialize systemDefaultCentury and systemDefaultCenturyYear based
    // on the current time.  They'll be set to 80 years before
    // the current time.
    // No point in locking as it should be idempotent.
    if (fgSystemDefaultCenturyStart == fgSystemDefaultCentury)
    {
        UErrorCode status = U_ZERO_ERROR;
        TaiwanCalendar calendar(Locale("@calendar=Taiwan"),status);
        if (U_SUCCESS(status))
        {
            calendar.setTime(Calendar::getNow(), status);
            calendar.add(UCAL_YEAR, -80, status);
            UDate    newStart =  calendar.getTime(status);
            int32_t  newYear  =  calendar.get(UCAL_YEAR, status);
            {
                umtx_lock(NULL);
                fgSystemDefaultCenturyStart = newStart;
                fgSystemDefaultCenturyStartYear = newYear;
                umtx_unlock(NULL);
            }
        }
        // We have no recourse upon failure unless we want to propagate the failure
        // out.
    }
}


U_NAMESPACE_END

#endif
