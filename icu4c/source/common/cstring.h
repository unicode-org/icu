/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File CSTRING.H
*
* Contains CString interface
*
* @author       Helena Shih
*
* Modification History:
*
*   Date        Name        Description
*   6/17/98     hshih       Created.
*  05/03/99     stephen     Changed from functions to macros.
*  06/14/99     stephen     Added icu_strncat, icu_strncmp, icu_tolower
*
*******************************************************************************
*/

#ifndef CSTRING_H
#define CSTRING_H 1

#include <string.h>
#include <ctype.h>

#include "utypes.h"

#define icu_strcpy(dst, src) strcpy(dst, src)
#define icu_strcpyWithSize(dst, src, size) strncpy(dst, src, size)
#define icu_strncpy(dst, src, size) strncpy(dst, src, size)
#define icu_strlen(str) strlen(str)
#define icu_strcmp(s1, s2) strcmp(s1, s2)
#define icu_strncmp(s1, s2, n) strncmp(s1, s2, n)
#define icu_strcat(dst, src) strcat(dst, src)
#define icu_strncat(dst, src, n) strncat(dst, src, n)
#define icu_strchr(s, c) strchr(s, c)
#define icu_strstr(s, c) strstr(s, c)
#define icu_strrchr(s, c) strrchr(s, c)
#define icu_toupper(c) toupper(c)
#define icu_tolower(c) tolower(c)
#define icu_strtoul(str, end, base) strtoul(str, end, base)
#ifdef WIN32
#   define icu_stricmp(str1, str2) _stricmp(str1, str2)
#elif defined(POSIX)
#   define icu_stricmp(str1, str2) strcasecmp(str1, str2)
#else
#   define icu_stricmp(str1, str2) T_CString_stricmp(str1, str2)
#endif

/*===========================================================================*/
/* Wide-character functions                                                  */
/*===========================================================================*/
#define icu_wcscat(dst, src) wcscat(dst, src)
#define icu_wcscpy(dst, src) wcscpy(dst, src)
#define icu_wcslen(src) wcslen(src)
#define icu_wcstombs(mbstr, wcstr, count) wcstombs(mbstr, wcstr, count)
#define icu_mbstowcs(wcstr, mbstr, count) mbstowcs(wcstr, mbstr, count)

U_CAPI char* U_EXPORT2
T_CString_toLowerCase(char* str);

U_CAPI char* U_EXPORT2
T_CString_toUpperCase(char* str);

U_CAPI void U_EXPORT2
T_CString_integerToString(char *buffer, int32_t n, int32_t radix);

U_CAPI int32_t U_EXPORT2
T_CString_stringToInteger(const char *integerString, int32_t radix);

U_CAPI int U_EXPORT2
T_CString_stricmp(const char *str1, const char *str2);

#endif /* ! CSTRING_H */
