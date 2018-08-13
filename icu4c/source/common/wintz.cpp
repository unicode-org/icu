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
#include "cwchar.h"
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

/*
  This code attempts to detect the Windows time zone directly, as set in the Windows Date and Time control panel. 
  Call GetDynamicTimeZoneInformation to get dynamic time zone info, which contains the TimeZoneKeyName.

  Author: Alan Liu
  Since: ICU 2.6
  Based on original code by Carl Brown <cbrown@xnetinc.com>

  Modified on 8/13/2018
  ICU ticket: ICU-13842 Windows time zone should be read from a per-user registry instead of system-wide registry?
*/

/**
 * Main Windows time zone detection function.  Returns the Windows
 * time zone, translated to an ICU time zone, or NULL upon failure.
 */
U_CFUNC const char* U_EXPORT2
uprv_detectWindowsTimeZone() 
{
    char* icuid = NULL;
    size_t len;
    DYNAMIC_TIME_ZONE_INFORMATION dynamicTZI;

    /* Obtain DYNAMIC_TIME_ZONE_INFORMATION from the API */
    uprv_memset(&dynamicTZI, 0, sizeof(dynamicTZI));
    GetDynamicTimeZoneInformation(&dynamicTZI);

    /* Copy the timezone ID to icuid to be returned. */
    if (dynamicTZI.TimeZoneKeyName[0] != 0)
    {
        len = uprv_wcslen(dynamicTZI.TimeZoneKeyName);
        icuid = (char*)uprv_calloc(len + 1, sizeof(char));
        if (icuid != NULL) 
        {
            uprv_wcstombs(icuid, dynamicTZI.TimeZoneKeyName, len + 1);
        }
    }
    
    return icuid;
}

#endif /* U_PLATFORM_USES_ONLY_WIN32_API || U_PLATFORM_HAS_WINUWP_API*/
