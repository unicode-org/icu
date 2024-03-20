// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
******************************************************************************
*
*   Copyright (C) 1997-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*  FILE NAME : ptypes.h
*
*   Date        Name        Description
*   05/13/98    nos         Creation (content moved here from ptypes.h).
*   03/02/99    stephen     Added AS400 support.
*   03/30/99    stephen     Added Linux support.
*   04/13/99    stephen     Reworked for autoconf.
*   09/18/08    srl         Moved basic types back to ptypes.h from platform.h
******************************************************************************
*/

/**
 * \file
 * \brief C API: Definitions of integer types of various widths
 */

#ifndef _PTYPES_H
#define _PTYPES_H

/**
 * \def __STDC_LIMIT_MACROS
 * According to the Linux stdint.h, the ISO C99 standard specifies that in C++ implementations
 * macros like INT32_MIN and UINTPTR_MAX should only be defined if explicitly requested.
 * We need to define __STDC_LIMIT_MACROS before including stdint.h in C++ code
 * that uses such limit macros.
 * @internal
 */
#ifndef __STDC_LIMIT_MACROS
#define __STDC_LIMIT_MACROS
#endif

/* NULL, size_t, wchar_t */
#include <stddef.h>

/*
 * If all compilers provided all of the C99 headers and types,
 * we would just unconditionally #include <stdint.h> here
 * and not need any of the stuff after including platform.h.
 */

/* Find out if we have stdint.h etc. */
#include "unicode/platform.h"

/*===========================================================================*/
/* Generic data types                                                        */
/*===========================================================================*/

#include <stdint.h>

// The <uchar.h> file does not exist in Apple SDKs (as of 3/15/24),
// so we have to define char16_t ourselves (in C; it's a built-in type in C++)
#if U_CPLUSPLUS_VERSION == 0
#if !U_PLATFORM_IS_DARWIN_BASED
#include <uchar.h>
#else
typedef uint16_t char16_t;
#endif // !U_PLATFORM_IS_DARWIN_BASED
#endif // U_CPLUSPLUS_VERSION == 0

#endif /* _PTYPES_H */
