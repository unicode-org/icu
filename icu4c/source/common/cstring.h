/*
*******************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
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

#include "unicode/utypes.h"

#define uprv_strcpy(dst, src) strcpy(dst, src)
#define uprv_strcpyWithSize(dst, src, size) strncpy(dst, src, size)
#define uprv_strncpy(dst, src, size) strncpy(dst, src, size)
#define uprv_strlen(str) strlen(str)
#define uprv_strcmp(s1, s2) strcmp(s1, s2)
#define uprv_strncmp(s1, s2, n) strncmp(s1, s2, n)
#define uprv_strcat(dst, src) strcat(dst, src)
#define uprv_strncat(dst, src, n) strncat(dst, src, n)
#define uprv_strchr(s, c) strchr(s, c)
#define uprv_strstr(s, c) strstr(s, c)
#define uprv_strrchr(s, c) strrchr(s, c)
#define uprv_toupper(c) toupper(c)
#define uprv_tolower(c) tolower(c)
#define uprv_strtoul(str, end, base) strtoul(str, end, base)
#ifdef WIN32
#   define uprv_stricmp(str1, str2) _stricmp(str1, str2)
#elif defined(POSIX)
#   define uprv_stricmp(str1, str2) strcasecmp(str1, str2)
#else
#   define uprv_stricmp(str1, str2) T_CString_stricmp(str1, str2)
#endif

/*===========================================================================*/
/* Wide-character functions                                                  */
/*===========================================================================*/

/* The following are not available on all systemts, defined in wchar.h or string.h . */
#if U_HAVE_WCHAR_H
#   define uprv_wcscat(dst, src) wcscat(dst, src)
#   define uprv_wcscpy(dst, src) wcscpy(dst, src)
#   define uprv_wcslen(src) wcslen(src)
#else
U_CAPI wchar_t *uprv_wcscat(wchar_t *dst, const wchar_t *src);
U_CAPI wchar_t *uprv_wcscpy(wchar_t *dst, const wchar_t *src);
U_CAPI size_t uprv_wcslen(const wchar_t *src);
#endif

/* The following are part of the ANSI C standard, defined in stdlib.h . */
#define uprv_wcstombs(mbstr, wcstr, count) wcstombs(mbstr, wcstr, count)
#define uprv_mbstowcs(wcstr, mbstr, count) mbstowcs(wcstr, mbstr, count)

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
