/*
******************************************************************************
*
*   Copyright (C) 1996-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*/
/*
*============================================================================
*
* File locmap.h      : Locale Mapping Classes
*
* 
*
* Created by: Helena Shih
*
* Modification History:
*
*  Date        Name        Description
*  3/11/97     aliu        Added setId().
*  4/20/99     Madhu       Added T_convertToPosix()
* 09/18/00     george      Removed the memory leaks.
* 08/23/01     george      Convert to C
*============================================================================
*/

/* include this first so that we are sure to get WIN32 defined */
#include "unicode/utypes.h"

#if defined(WIN32) && !defined(LOCMAP_H)
#define LOCMAP_H

#define LANGUAGE_LCID(hostID) (uint16_t)(0x03FF & hostID)

U_CFUNC const char *T_convertToPosix(uint32_t hostid, UErrorCode* status);

U_CFUNC uint32_t T_convertToLCID(const char* posixID, UErrorCode* status);


#endif
