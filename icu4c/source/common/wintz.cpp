// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
********************************************************************************
*   Copyright (C) 2005-2015, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File WINTZ.CPP
*
********************************************************************************
*/

#include "unicode/utypes.h"

#if U_PLATFORM_USES_ONLY_WIN32_API

#include "wintz.h"
#include "charstr.h"
#include "cmemory.h"
#include "cstring.h"

#include "unicode/ures.h"
#include "unicode/unistr.h"
#include "uresimp.h"

#ifndef WIN32_LEAN_AND_MEAN
#   define WIN32_LEAN_AND_MEAN
#endif
#   define VC_EXTRALEAN
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#include <windows.h>

U_NAMESPACE_BEGIN

// The max size of TimeZoneKeyName is 128, defined in DYNAMIC_TIME_ZONE_INFORMATION
#define MAX_TIMEZONE_ID_LENGTH 128

/**
* Main Windows time zone detection function.
* Returns the Windows time zone converted to an ICU time zone as a heap-allocated buffer, or nullptr upon failure.
* Note: We use the Win32 API GetDynamicTimeZoneInformation to get the current time zone info.
* This API returns a non-localized time zone name, which we can then map to an ICU time zone name.
*/
U_INTERNAL const char* U_EXPORT2
uprv_detectWindowsTimeZone()
{
    /* Obtain TIME_ZONE_INFORMATION from the API and get the non-localized time zone name. */
    DYNAMIC_TIME_ZONE_INFORMATION dynamicTZI;
    uprv_memset(&dynamicTZI, 0, sizeof(dynamicTZI));

    DWORD tzIDStatus = GetDynamicTimeZoneInformation(&dynamicTZI);
    if (tzIDStatus == TIME_ZONE_ID_INVALID || dynamicTZI.TimeZoneKeyName[0] == 0)
        return nullptr;

    // If DST is turned off in the control panel, return "Etc/GMT<offset>".
    if (dynamicTZI.DynamicDaylightTimeDisabled) {
        LONG utcOffsetMins = dynamicTZI.Bias;
        if (utcOffsetMins == 0)
          return uprv_strdup("UTC");

        // No way to support when DST is turned off and the offset in minutes is not
        // a multiple of 60.
        if (utcOffsetMins % 60 == 0) {
          char gmtOffsetTz[11]; // "Etc/GMT+dd" is 11-char long with a terminal null.
          // Note '-' before 'utcOffsetMin'. The timezone ID's sign convention
          // is that a timezone ahead of UTC is Etc/GMT-<offset> and a timezone
          // behind UTC is Etc/GMT+<offset>.
          snprintf(gmtOffsetTz, 11, "Etc/GMT%+d", -utcOffsetMins / 60);
          return uprv_strdup(gmtOffsetTz);
        }
    }

    CharString winTZ;
    UErrorCode status = U_ZERO_ERROR;
    winTZ.appendInvariantChars(UnicodeString(TRUE, dynamicTZI.TimeZoneKeyName, -1), status);

    // Map Windows Timezone name (non-localized) to ICU timezone ID (~ Olson timezone id).
    LocalUResourceBundlePointer winTZBundle(ures_openDirect(nullptr, "windowsZones", &status));
    ures_getByKey(winTZBundle.getAlias(), "mapTimezones", winTZBundle.getAlias(), &status);
    ures_getByKey(winTZBundle.getAlias(), winTZ.data(), winTZBundle.getAlias(), &status);

    if (U_FAILURE(status)) {
        return nullptr;
    }

    const UChar* icuTZ16 = nullptr;
    char regionCode[3] = {}; // 2 letter ISO 3166 country code made entirely of invariant chars.
    int geoId = GetUserGeoID(GEOCLASS_NATION);
    int regionCodeLen = GetGeoInfoA(geoId, GEO_ISO2, regionCode, 3, 0);
    int32_t tzLen;
    if (regionCodeLen != 0) {
        icuTZ16 = ures_getStringByKey(winTZBundle.getAlias(), regionCode, &tzLen, &status);
    }
    if (regionCodeLen == 0 || U_FAILURE(status)) {
        // fallback to default "001" (world)
        status = U_ZERO_ERROR;
        icuTZ16 = ures_getStringByKey(winTZBundle.getAlias(), "001", &tzLen, &status);
    }

    CharString icuTZStr;
    return icuTZStr.appendInvariantChars(icuTZ16, tzLen, status).cloneData(status);
}

U_NAMESPACE_END
#endif /* U_PLATFORM_USES_ONLY_WIN32_API  */
