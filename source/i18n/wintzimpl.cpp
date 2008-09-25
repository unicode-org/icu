/*
********************************************************************************
*   Copyright (C) 2008, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File WINTZIMPL.CPP
*
********************************************************************************
*/

#include "unicode/utypes.h"

#ifdef U_WINDOWS

#include "wintzimpl.h"

#include "unicode/unistr.h"
#include "unicode/timezone.h"
#include "unicode/basictz.h"
#include "unicode/calendar.h"

#   define WIN32_LEAN_AND_MEAN
#   define VC_EXTRALEAN
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX

#include <windows.h>

static UBool getSystemTimeInformation(TimeZone *tz, SYSTEMTIME &daylightDate, SYSTEMTIME &standardDate, int32_t &bias, int32_t &daylightBias, int32_t &standardBias) {
    UErrorCode status = U_ZERO_ERROR;
    UBool result = TRUE;
    BasicTimeZone *btz = NULL;
    Calendar *cal = NULL;
    InitialTimeZoneRule *initial = NULL;
    AnnualTimeZoneRule *std = NULL;
    AnnualTimeZoneRule *dst = NULL;
    UDate currentTime;
    int32_t currentYear;
    int32_t rawOffset = tz->getRawOffset();
    int32_t dstOffset = tz->getDSTSavings();

    /* Offset set needs to be normalized so to Windows */
    bias = (tz->getRawOffset() * -1)/(60000);
    standardBias = 0;
    
    if (tz->useDaylightTime()) {
        daylightBias = (tz->getDSTSavings() / 60000) * -1;
    } else {
        daylightBias = 0;
    }

    btz = (BasicTimeZone *)tz->clone();
    cal = Calendar::createInstance(tz, status);
    if (btz != NULL && cal != NULL && U_SUCCESS(status)) {
        currentTime = cal->getTime(status);
        currentYear = cal->get(UCAL_YEAR, status);
        
        btz->getSimpleRulesNear(currentTime, initial, std, dst, status);
        if (U_SUCCESS(status)) {
            if (std == NULL || dst == NULL) {
                /* Generally, if std is NULL, dst will also be NULL.
                 * This indicates that daylight saving is not observed
                 * in this timezone so the standarDate and daylightDate
                 * should be zero.
                 */
                standardDate.wYear = standardDate.wMonth  = standardDate.wDayOfWeek = standardDate.wDay = 
                standardDate.wHour = standardDate.wMinute = standardDate.wSecond    = standardDate.wMilliseconds = 0;
                daylightDate.wYear = daylightDate.wMonth  = daylightDate.wDayOfWeek = daylightDate.wDay =
                daylightDate.wHour = daylightDate.wMinute = daylightDate.wSecond    = daylightDate.wMilliseconds = 0;
            } else {
                /* Get the UDate value for the start of the timezone standard time and daylight saving time.
                 * Then set the standardDate and daylightDate to this. The wYear value is 0 to indicate that
                 * this is not an absolute date but a rule. (e.g. the first Sunday in November)
                 */
                UDate dateTime;
                if (std->getStartInYear(currentYear, rawOffset, 0, dateTime)) {
                    cal->setTime(dateTime, status);

                    standardDate.wYear          = 0;
                    standardDate.wMonth         = cal->get(UCAL_MONTH, status) + 1;
                    standardDate.wDayOfWeek     = cal->get(UCAL_DAY_OF_WEEK, status) - 1;
                    standardDate.wDay           = cal->get(UCAL_DAY_OF_WEEK_IN_MONTH, status);
                    standardDate.wHour          = cal->get(UCAL_HOUR_OF_DAY, status);
                    standardDate.wMinute        = cal->get(UCAL_MINUTE, status);
                    standardDate.wSecond        = cal->get(UCAL_SECOND, status);
                    standardDate.wMilliseconds  = cal->get(UCAL_MILLISECOND, status);

                    if (U_SUCCESS(status) && dst->getStartInYear(currentYear, rawOffset, dstOffset, dateTime)) {
                        cal->setTime(dateTime, status);

                        daylightDate.wYear          = 0;
                        daylightDate.wMonth         = cal->get(UCAL_MONTH, status) + 1;
                        daylightDate.wDayOfWeek     = cal->get(UCAL_DAY_OF_WEEK, status) - 1;
                        daylightDate.wDay           = cal->get(UCAL_DAY_OF_WEEK_IN_MONTH, status);
                        daylightDate.wHour          = cal->get(UCAL_HOUR_OF_DAY, status) + dstOffset/3600000;
                        daylightDate.wMinute        = cal->get(UCAL_MINUTE, status);
                        daylightDate.wSecond        = cal->get(UCAL_SECOND, status);
                        daylightDate.wMilliseconds  = cal->get(UCAL_MILLISECOND, status);
                    } else {
                        result = FALSE;
                    }
                } else {
                    result = FALSE;
                }
            } 
        } else {
            result = FALSE;
        }
    } else {
        result = FALSE;
    }

    if (result && U_FAILURE(status)) {
        result = FALSE;
    }
    
    delete btz;
    delete cal;
    delete initial;
    delete std;
    delete dst;

    return result;
}

static UBool getWindowsTimeZoneInfo(TIME_ZONE_INFORMATION *zoneInfo, const UChar *icuid, int32_t length) {
    UBool result = FALSE;
    UnicodeString id = UnicodeString(icuid, length);
    TimeZone *tz = TimeZone::createTimeZone(id);
    
    if (tz != NULL) {
        int32_t bias;
        int32_t daylightBias;
        int32_t standardBias;
        SYSTEMTIME daylightDate;
        SYSTEMTIME standardDate;
        if (!getSystemTimeInformation(tz, daylightDate, standardDate, bias, daylightBias, standardBias)) {
            result = FALSE;
        } else {
            zoneInfo->Bias          = bias;
            zoneInfo->DaylightBias  = daylightBias;
            zoneInfo->StandardBias  = standardBias;
            zoneInfo->DaylightDate  = daylightDate;
            zoneInfo->StandardDate  = standardDate;

            result = TRUE;
        }
    }

    return result;
}

/*
 * Given the timezone icuid, fill in zoneInfo by calling auxillary functions that creates a timezone and extract the 
 * information to put into zoneInfo. This includes bias and standard time date and daylight saving date.
 */
U_CAPI UBool U_EXPORT2
uprv_getWindowsTimeZoneInfo(TIME_ZONE_INFORMATION *zoneInfo, const UChar *icuid, int32_t length)
{
    if (getWindowsTimeZoneInfo(zoneInfo, icuid, length)) {
        return TRUE;
    } else {
        return FALSE;
    }
}

#endif
