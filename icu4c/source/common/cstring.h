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
#define icu_strlen(str) strlen(str)
#define icu_strcmp(s1, s2) strcmp(s1, s2)
#define icu_strncmp(s1, s2, n) strncmp(s1, s2, n)
#define icu_strcat(dst, src) strcat(dst, src)
#define icu_strncat(dst, src, n) strncat(dst, src, n)
#define icu_strchr(s, c) strchr(s, c)
#define icu_toupper(c) toupper(c)
#define icu_tolower(c) tolower(c)

CAPI char* U_EXPORT2
T_CString_toLowerCase(char* str);

CAPI char* U_EXPORT2
T_CString_toUpperCase(char* str);

CAPI void U_EXPORT2
T_CString_integerToString(char *buffer, int32_t n, int32_t radix);

CAPI int32_t U_EXPORT2
T_CString_stringToInteger(const char *integerString, int32_t radix);

#endif /* ! CSTRING_H */
