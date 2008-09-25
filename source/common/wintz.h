/*
********************************************************************************
*   Copyright (C) 2005-2008, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File WINTZ.H
*
********************************************************************************
*/

#ifndef __WINTZ
#define __WINTZ

#include "unicode/utypes.h"

#ifdef U_WINDOWS

/**
 * \file 
 * \brief C API: Utilities for dealing w/ Windows time zones.
 */

U_CDECL_BEGIN
/* Forward declarations for Windows types... */
typedef struct _TIME_ZONE_INFORMATION TIME_ZONE_INFORMATION;
U_CDECL_END

#if 0
/* This function is deprecated and is replaced by the function in i18n\wintzimpl.h */
U_CAPI UBool U_EXPORT2
uprv_getWindowsTimeZoneInfo(TIME_ZONE_INFORMATION *zoneInfo, const UChar *icuid, int32_t length);
#endif

U_CFUNC const char* U_EXPORT2
uprv_detectWindowsTimeZone();

#endif /* #ifdef U_WINDOWS */

#endif /* __WINTZ */
