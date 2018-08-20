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

#if U_PLATFORM_USES_ONLY_WIN32_API || U_PLATFORM_HAS_WINUWP_API

#include "wintz.h"
#include "cmemory.h"
#include "cstring.h"

#include "unicode/ures.h"
#include "unicode/ustring.h"

#ifndef WIN32_LEAN_AND_MEAN
#   define WIN32_LEAN_AND_MEAN
#endif
#   define VC_EXTRALEAN
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#include <windows.h>

#define MAX_LENGTH_ID 40

/**
* Main Windows time zone detection function.  Returns the Windows
* time zone, translated to an ICU time zone, or NULL upon failure.
* It is calling GetDynamicTimeZoneInformation to get the current time zone info.
* The API returns non-localized time zone name so it can be used for mapping ICU time zone name.
*/
U_CFUNC const char* U_EXPORT2
uprv_detectWindowsTimeZone()
{
    UErrorCode status = U_ZERO_ERROR;
    UResourceBundle* bundle = NULL;
    char* icuid = NULL;
    char dynamicTZKeyName[MAX_LENGTH_ID];
    char tmpid[MAX_LENGTH_ID];
    int32_t len;
    int id;
    int errorCode;
    wchar_t ISOcodeW[3]; /* 2 letter iso code in UTF-16*/
    char  ISOcodeA[3]; /* 2 letter iso code in ansi */

    DYNAMIC_TIME_ZONE_INFORMATION dynamicTZI;

    /* Obtain TIME_ZONE_INFORMATION from the API and get the non-localized time zone name. */
    uprv_memset(&dynamicTZI, 0, sizeof(dynamicTZI));
    GetDynamicTimeZoneInformation(&dynamicTZI);

    tmpid[0] = 0;

    id = GetUserGeoID(GEOCLASS_NATION);
    errorCode = GetGeoInfoW(id, GEO_ISO2, ISOcodeW, 3, 0);
    u_strToUTF8(ISOcodeA, 3, NULL, (const UChar *)ISOcodeW, 3, &status);

    bundle = ures_openDirect(NULL, "windowsZones", &status);
    ures_getByKey(bundle, "mapTimezones", bundle, &status);

    /* Convert the wchar_t* standard name to char* */
    uprv_memset(dynamicTZKeyName, 0, sizeof(dynamicTZKeyName));
    wcstombs(dynamicTZKeyName, dynamicTZI.TimeZoneKeyName, MAX_LENGTH_ID);
    if (dynamicTZI.TimeZoneKeyName[0] != 0)
    {
        UResourceBundle* winTZ = ures_getByKey(bundle, dynamicTZKeyName, NULL, &status);
        if (U_SUCCESS(status))
        {
            const UChar* icuTZ = NULL;
            if (errorCode != 0)
            {
                icuTZ = ures_getStringByKey(winTZ, ISOcodeA, &len, &status);
            }
            if (errorCode == 0 || icuTZ == NULL)
            {
                /* fallback to default "001" and reset status */
                status = U_ZERO_ERROR;
                icuTZ = ures_getStringByKey(winTZ, "001", &len, &status);
            }

            if (U_SUCCESS(status))
            {
                int index = 0;
                while (!(*icuTZ == '\0' || *icuTZ == ' '))
                {
                    tmpid[index++] = (char)(*icuTZ++);  /* safe to assume 'char' is ASCII compatible on windows */
                }
                tmpid[index] = '\0';
            }
        }
        ures_close(winTZ);
    }

    /*
    * Copy the timezone ID to icuid to be returned.
    */
    if (tmpid[0] != 0)
    {
        len = uprv_strlen(tmpid);
        icuid = (char*)uprv_calloc(len + 1, sizeof(char));
        if (icuid != NULL)
        {
            uprv_strcpy(icuid, tmpid);
        }
    }

    ures_close(bundle);

    return icuid;
}

#endif /* U_PLATFORM_USES_ONLY_WIN32_API || U_PLATFORM_HAS_WINUWP_API */
