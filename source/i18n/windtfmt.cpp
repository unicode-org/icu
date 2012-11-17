/*
********************************************************************************
*   Copyright (C) 2005-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File WINDTFMT.CPP
*
********************************************************************************
*/

#include "unicode/utypes.h"

#ifdef U_WINDOWS

#if !UCONFIG_NO_FORMATTING

#include "unicode/ures.h"
#include "unicode/format.h"
#include "unicode/fmtable.h"
#include "unicode/datefmt.h"
#include "unicode/msgfmt.h"
#include "unicode/calendar.h"
#include "unicode/gregocal.h"
#include "unicode/locid.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "unicode/timezone.h"
#include "unicode/basictz.h"
#include "unicode/utmscale.h"

#include "uassert.h"
#include "cmemory.h"
#include "putilimp.h"
#include "uresimp.h"
#include "windtfmt.h"
#include "wintz.h"

#   define WIN32_LEAN_AND_MEAN
#   define VC_EXTRALEAN
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#include <windows.h>

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(Win32DateFormat)

#define ARRAY_SIZE(array) (sizeof array / sizeof array[0])

#define NEW_ARRAY(type,count) (type *) uprv_malloc((count) * sizeof(type))
#define DELETE_ARRAY(array) uprv_free((void *) (array))

#define STACK_BUFFER_SIZE 64

UnicodeString *getTimeDateFormat(const Calendar *cal, const Locale *locale, UErrorCode &status)
{
    UnicodeString *result = NULL;
    const char *type = cal->getType();
    const char *base = locale->getBaseName();
    UResourceBundle *topBundle = ures_open((char *) 0, base, &status);
    UResourceBundle *calBundle = ures_getByKey(topBundle, "calendar", NULL, &status);
    UResourceBundle *typBundle = ures_getByKeyWithFallback(calBundle, type, NULL, &status);
    UResourceBundle *patBundle = ures_getByKeyWithFallback(typBundle, "DateTimePatterns", NULL, &status);

    if (status == U_MISSING_RESOURCE_ERROR) {
        status = U_ZERO_ERROR;
        typBundle = ures_getByKeyWithFallback(calBundle, "gregorian", typBundle, &status);
        patBundle = ures_getByKeyWithFallback(typBundle, "DateTimePatterns", patBundle, &status);
    }

    if (U_FAILURE(status)) {
        static const UChar defaultPattern[] = {0x007B, 0x0031, 0x007D, 0x0020, 0x007B, 0x0030, 0x007D, 0x0000}; // "{1} {0}"
        return new UnicodeString(defaultPattern, ARRAY_SIZE(defaultPattern));
    }

    int32_t resStrLen = 0;
    const UChar *resStr = ures_getStringByIndex(patBundle, (int32_t)DateFormat::kDateTime, &resStrLen, &status);

    result = new UnicodeString(TRUE, resStr, resStrLen);

    ures_close(patBundle);
    ures_close(typBundle);
    ures_close(calBundle);
    ures_close(topBundle);

    return result;
}

// TODO: Range-check timeStyle, dateStyle
Win32DateFormat::Win32DateFormat(DateFormat::EStyle timeStyle, DateFormat::EStyle dateStyle, const Locale &locale, UErrorCode &status)
  : DateFormat(), fDateTimeMsg(NULL), fTimeStyle(timeStyle), fDateStyle(dateStyle), fLocale(&locale), fZoneID()
{
    if (U_SUCCESS(status)) {
        fLCID = locale.getLCID();
        fTZI = NEW_ARRAY(TIME_ZONE_INFORMATION, 1);
        uprv_memset(fTZI, 0, sizeof(TIME_ZONE_INFORMATION));
        adoptCalendar(Calendar::createInstance(locale, status));
    }
}

Win32DateFormat::Win32DateFormat(const Win32DateFormat &other)
  : DateFormat(other)
{
    *this = other;
}

Win32DateFormat::~Win32DateFormat()
{
//    delete fCalendar;
    uprv_free(fTZI);
    delete fDateTimeMsg;
}

Win32DateFormat &Win32DateFormat::operator=(const Win32DateFormat &other)
{
    // The following handles fCalendar
    DateFormat::operator=(other);

//    delete fCalendar;

    this->fDateTimeMsg = other.fDateTimeMsg;
    this->fTimeStyle   = other.fTimeStyle;
    this->fDateStyle   = other.fDateStyle;
    this->fLCID        = other.fLCID;
//    this->fCalendar    = other.fCalendar->clone();
    this->fZoneID      = other.fZoneID;

    this->fTZI = NEW_ARRAY(TIME_ZONE_INFORMATION, 1);
    *this->fTZI = *other.fTZI;

    return *this;
}

Format *Win32DateFormat::clone(void) const
{
    return new Win32DateFormat(*this);
}

// TODO: Is just ignoring pos the right thing?
UnicodeString &Win32DateFormat::format(Calendar &cal, UnicodeString &appendTo, FieldPosition &pos) const
{
    FILETIME ft;
    SYSTEMTIME st_gmt;
    SYSTEMTIME st_local;
    TIME_ZONE_INFORMATION tzi = *fTZI;
    UErrorCode status = U_ZERO_ERROR;
    const TimeZone &tz = cal.getTimeZone();
    int64_t uct, uft;

    setTimeZoneInfo(&tzi, tz);

    uct = utmscale_fromInt64((int64_t) cal.getTime(status), UDTS_ICU4C_TIME, &status);
    uft = utmscale_toInt64(uct, UDTS_WINDOWS_FILE_TIME, &status);

    ft.dwLowDateTime =  (DWORD) (uft & 0xFFFFFFFF);
    ft.dwHighDateTime = (DWORD) ((uft >> 32) & 0xFFFFFFFF);

    FileTimeToSystemTime(&ft, &st_gmt);
    SystemTimeToTzSpecificLocalTime(&tzi, &st_gmt, &st_local);


    if (fDateStyle != DateFormat::kNone && fTimeStyle != DateFormat::kNone) {
        UnicodeString *date = new UnicodeString();
        UnicodeString *time = new UnicodeString();
        UnicodeString *pattern = fDateTimeMsg;
        Formattable timeDateArray[2];

        formatDate(&st_local, *date);
        formatTime(&st_local, *time);

        timeDateArray[0].adoptString(time);
        timeDateArray[1].adoptString(date);

        if (strcmp(fCalendar->getType(), cal.getType()) != 0) {
            pattern = getTimeDateFormat(&cal, fLocale, status);
        }

        MessageFormat::format(*pattern, timeDateArray, 2, appendTo, status);
    } else if (fDateStyle != DateFormat::kNone) {
        formatDate(&st_local, appendTo);
    } else if (fTimeStyle != DateFormat::kNone) {
        formatTime(&st_local, appendTo);
    }

    return appendTo;
}

void Win32DateFormat::parse(const UnicodeString& text, Calendar& cal, ParsePosition& pos) const
{
    pos.setErrorIndex(pos.getIndex());
}

void Win32DateFormat::adoptCalendar(Calendar *newCalendar)
{
    if (fCalendar == NULL || strcmp(fCalendar->getType(), newCalendar->getType()) != 0) {
        UErrorCode status = U_ZERO_ERROR;

        if (fDateStyle != DateFormat::kNone && fTimeStyle != DateFormat::kNone) {
            delete fDateTimeMsg;
            fDateTimeMsg = getTimeDateFormat(newCalendar, fLocale, status);
        }
    }

    delete fCalendar;
    fCalendar = newCalendar;

    fZoneID = setTimeZoneInfo(fTZI, fCalendar->getTimeZone());
}

void Win32DateFormat::setCalendar(const Calendar &newCalendar)
{
    adoptCalendar(newCalendar.clone());
}

void Win32DateFormat::adoptTimeZone(TimeZone *zoneToAdopt)
{
    fZoneID = setTimeZoneInfo(fTZI, *zoneToAdopt);
    fCalendar->adoptTimeZone(zoneToAdopt);
}

void Win32DateFormat::setTimeZone(const TimeZone& zone)
{
    fZoneID = setTimeZoneInfo(fTZI, zone);
    fCalendar->setTimeZone(zone);
}

static const DWORD dfFlags[] = {DATE_LONGDATE, DATE_LONGDATE, DATE_SHORTDATE, DATE_SHORTDATE};

void Win32DateFormat::formatDate(const SYSTEMTIME *st, UnicodeString &appendTo) const
{
    int result;
    UChar stackBuffer[STACK_BUFFER_SIZE];
    UChar *buffer = stackBuffer;

    result = GetDateFormatW(fLCID, dfFlags[fDateStyle - kDateOffset], st, NULL, buffer, STACK_BUFFER_SIZE);

    if (result == 0) {
        if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
            int newLength = GetDateFormatW(fLCID, dfFlags[fDateStyle - kDateOffset], st, NULL, NULL, 0);

            buffer = NEW_ARRAY(UChar, newLength);
            GetDateFormatW(fLCID, dfFlags[fDateStyle - kDateOffset], st, NULL, buffer, newLength);
        }
    }

    appendTo.append(buffer, (int32_t) wcslen(buffer));

    if (buffer != stackBuffer) {
        DELETE_ARRAY(buffer);
    }
}

static const DWORD tfFlags[] = {0, 0, 0, TIME_NOSECONDS};

void Win32DateFormat::formatTime(const SYSTEMTIME *st, UnicodeString &appendTo) const
{
    int result;
    UChar stackBuffer[STACK_BUFFER_SIZE];
    UChar *buffer = stackBuffer;

    result = GetTimeFormatW(fLCID, tfFlags[fTimeStyle], st, NULL, buffer, STACK_BUFFER_SIZE);

    if (result == 0) {
        if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
            int newLength = GetTimeFormatW(fLCID, tfFlags[fTimeStyle], st, NULL, NULL, 0);

            buffer = NEW_ARRAY(UChar, newLength);
            GetDateFormatW(fLCID, tfFlags[fTimeStyle], st, NULL, buffer, newLength);
        }
    }

    appendTo.append(buffer, (int32_t) wcslen(buffer));

    if (buffer != stackBuffer) {
        DELETE_ARRAY(buffer);
    }
}

UnicodeString Win32DateFormat::setTimeZoneInfo(TIME_ZONE_INFORMATION *tzi, const TimeZone &zone) const
{
    UnicodeString zoneID;

    zone.getID(zoneID);

    if (zoneID.compare(fZoneID) != 0) {
        UnicodeString icuid;

        zone.getID(icuid);
        if (! uprv_getWindowsTimeZoneInfo(tzi, icuid.getBuffer(), icuid.length())) {
            UBool found = FALSE;
            int32_t ec = TimeZone::countEquivalentIDs(icuid);

            for (int z = 0; z < ec; z += 1) {
                UnicodeString equiv = TimeZone::getEquivalentID(icuid, z);

                if (found = uprv_getWindowsTimeZoneInfo(tzi, equiv.getBuffer(), equiv.length())) {
                    break;
                }
            }

            if (! found) {
                GetTimeZoneInformation(tzi);
            }
        }
    }

    return zoneID;
}

static UBool getSystemTimeInformation(TimeZone *tz, SYSTEMTIME &daylightDate, SYSTEMTIME &standardDate, int32_t &bias, int32_t &daylightBias, int32_t &standardBias) {
    UErrorCode status = U_ZERO_ERROR;
    UBool result = TRUE;
    BasicTimeZone *btz = (BasicTimeZone*)tz; // we should check type
    InitialTimeZoneRule *initial = NULL;
    AnnualTimeZoneRule *std = NULL, *dst = NULL;

    btz->getSimpleRulesNear(uprv_getUTCtime(), initial, std, dst, status);
    if (U_SUCCESS(status)) {
        if (std == NULL || dst == NULL) {
            bias = -1 * (initial->getRawOffset()/60000);
            standardBias = 0;
            daylightBias = 0;
            // Do not use DST.  Set 0 to all stadardDate/daylightDate fields
            standardDate.wYear = standardDate.wMonth  = standardDate.wDayOfWeek = standardDate.wDay = 
            standardDate.wHour = standardDate.wMinute = standardDate.wSecond    = standardDate.wMilliseconds = 0;
            daylightDate.wYear = daylightDate.wMonth  = daylightDate.wDayOfWeek = daylightDate.wDay =
            daylightDate.wHour = daylightDate.wMinute = daylightDate.wSecond    = daylightDate.wMilliseconds = 0;
        } else {
            U_ASSERT(std->getRule()->getDateRuleType() == DateTimeRule::DOW);
            U_ASSERT(dst->getRule()->getDateRuleType() == DateTimeRule::DOW);

            bias = -1 * (std->getRawOffset()/60000);
            standardBias = 0;
            daylightBias = -1 * (dst->getDSTSavings()/60000);
            // Always use DOW type rule
            int32_t hour, min, sec, mil;
            standardDate.wYear = 0;
            standardDate.wMonth = std->getRule()->getRuleMonth() + 1;
            standardDate.wDay = std->getRule()->getRuleWeekInMonth();
            if (standardDate.wDay < 0) {
                standardDate.wDay = 5;
            }
            standardDate.wDayOfWeek = std->getRule()->getRuleDayOfWeek() - 1;

            mil = std->getRule()->getRuleMillisInDay();
            hour = mil/3600000;
            mil %= 3600000;
            min = mil/60000;
            mil %= 60000;
            sec = mil/1000;
            mil %= 1000;

            standardDate.wHour = hour;
            standardDate.wMinute = min;
            standardDate.wSecond = sec;
            standardDate.wMilliseconds = mil;

            daylightDate.wYear = 0;
            daylightDate.wMonth = dst->getRule()->getRuleMonth() + 1;
            daylightDate.wDay = dst->getRule()->getRuleWeekInMonth();
            if (daylightDate.wDay < 0) {
                daylightDate.wDay = 5;
            }
            daylightDate.wDayOfWeek = dst->getRule()->getRuleDayOfWeek() - 1;

            mil = dst->getRule()->getRuleMillisInDay();
            hour = mil/3600000;
            mil %= 3600000;
            min = mil/60000;
            mil %= 60000;
            sec = mil/1000;
            mil %= 1000;

            daylightDate.wHour = hour;
            daylightDate.wMinute = min;
            daylightDate.wSecond = sec;
            daylightDate.wMilliseconds = mil;
        }
    } else {
        result = FALSE;
    }

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

        if (getSystemTimeInformation(tz, daylightDate, standardDate, bias, daylightBias, standardBias)) {
            uprv_memset(zoneInfo, 0, sizeof(TIME_ZONE_INFORMATION)); // We do not set standard/daylight names, so nullify first.
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

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // #ifdef U_WINDOWS

