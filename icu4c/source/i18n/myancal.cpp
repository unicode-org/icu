// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*
* File MYANCAL.CPP
*
* Modification History:
* 04/18/2025  mapmeld          copied from buddhcal.h
*
*/

#include "unicode/ucal.h"
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "myancal.h"
#include "gregoimp.h"
#include "unicode/gregocal.h"
#include "umutex.h"
#include <float.h>

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(MyanmarCalendar)

//static const int32_t kMaxEra = 0; // only 1 era

static const int32_t kMEEraStart = 639;  // 639 AD (Gregorian)

static const int32_t kGregorianEpoch = 1970;    // used as the default value of EXTENDED_YEAR

MyanmarCalendar::MyanmarCalendar(const Locale& aLocale, UErrorCode& success)
:   GregorianCalendar(aLocale, success)
{
}

MyanmarCalendar::~MyanmarCalendar()
{
}

MyanmarCalendar::MyanmarCalendar(const MyanmarCalendar& source)
: GregorianCalendar(source)
{
}

MyanmarCalendar* MyanmarCalendar::clone() const
{
    return new MyanmarCalendar(*this);
}

const char *MyanmarCalendar::getType() const
{
    return "myanmar";
}

int32_t MyanmarCalendar::yearStart(int32_t year, UErrorCode& status) {
    return GregorianCalendar::handleComputeMonthStart(year + 638, 3, true, status) + 16;
}

int32_t MyanmarCalendar::handleGetExtendedYear(UErrorCode& status)
{
    if (U_FAILURE(status)) {
        return 0;
    }

    // extended year is a gregorian year, where 1 = 1AD,  0 = 1BC, -1 = 2BC, etc
    int32_t y = internalGet(UCAL_YEAR, kGregorianEpoch - kMEEraStart);
    int32_t m = internalGet(UCAL_MONTH);
    int32_t d = internalGet(UCAL_DAY_OF_MONTH);
    if ((m == 3 && d >= 17) || m >= 4) {
        y--;
        internalSet(UCAL_YEAR, y);
    }
    if (uprv_add32_overflow(y, kMEEraStart, &y)) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    return y;
}

void MyanmarCalendar::handleComputeFields(int32_t julianDay, UErrorCode& status)
{
    GregorianCalendar::handleComputeFields(julianDay, status);
    int32_t y = internalGet(UCAL_EXTENDED_YEAR) - kMEEraStart;
    int32_t m = internalGet(UCAL_MONTH);
    int32_t d = internalGet(UCAL_DAY_OF_MONTH);
    if ((m == 3 && d >= 17) || m >= 4) {
        y++;
    }
    internalSet(UCAL_ERA, 0);
    internalSet(UCAL_YEAR, y);
}

int32_t MyanmarCalendar::handleGetLimit(UCalendarDateFields field, ELimitType limitType) const
{
    if(field == UCAL_ERA) {
        return ME;
    }
    return GregorianCalendar::handleGetLimit(field,limitType);
}

IMPL_SYSTEM_DEFAULT_CENTURY(MyanmarCalendar, "@calendar=myanmar")

U_NAMESPACE_END

#endif
