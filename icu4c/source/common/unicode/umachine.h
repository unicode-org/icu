/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  umachine.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep13
*   created by: Markus W. Scherer
*
*   This file defines basic types and constants for utf.h to be
*   platform-independent. umachine.h and utf.h are included into
*   utypes.h to provide all the general definitions for ICU.
*   All of these definitions used to be in utypes.h before
*   the UTF-handling macros made this unmaintainable.
*/

#ifndef __UMACHINE_H__
#define __UMACHINE_H__

/*===========================================================================*/
/* Include platform-dependent definitions                                    */
/* which are contained in the platform-specific file platform.h              */
/*===========================================================================*/

#if defined(WIN32) || defined(_WIN32) || defined(WIN64) || defined(_WIN64)
#   include "unicode/pwin32.h"
#elif defined(__OS2__)
#   include "unicode/pos2.h"
#elif defined(__OS400__)
#   include "unicode/pos400.h"
#else
#   include "unicode/platform.h"
#endif

/*===========================================================================*/
/* XP_CPLUSPLUS is a cross-platform symbol which should be defined when      */
/* using C++.  It should not be defined when compiling under C.              */
/*===========================================================================*/

#ifdef __cplusplus
#   ifndef XP_CPLUSPLUS
#       define XP_CPLUSPLUS
#   endif
#else
#   undef XP_CPLUSPLUS
#endif

/*===========================================================================*/
/* For C wrappers, we use the symbol U_CAPI.                                 */
/* This works properly if the includer is C or C++.                          */
/* Functions are declared   U_CAPI return-type U_EXPORT2 function-name() ... */
/*===========================================================================*/

#ifdef XP_CPLUSPLUS
#   define U_CFUNC extern "C"
#   define U_CDECL_BEGIN extern "C" {
#   define U_CDECL_END   }
#else
#   define U_CFUNC
#   define U_CDECL_BEGIN
#   define U_CDECL_END
#endif
#define U_CAPI U_CFUNC U_EXPORT

/*===========================================================================*/
/* limits for int32_t etc., like in POSIX inttypes.h                         */
/*===========================================================================*/

#ifndef INT8_MIN
#   define INT8_MIN        (-128)
#endif
#ifndef INT16_MIN
#   define INT16_MIN       (-32767-1)
#endif
#ifndef INT32_MIN
#   define INT32_MIN       (-2147483647-1)
#endif

#ifndef INT8_MAX
#   define INT8_MAX        (127)
#endif
#ifndef INT16_MAX
#   define INT16_MAX       (32767)
#endif
#ifndef INT32_MAX
#   define INT32_MAX       (2147483647)
#endif

#ifndef UINT8_MAX
#   define UINT8_MAX       (255U)
#endif
#ifndef UINT16_MAX
#   define UINT16_MAX      (65535U)
#endif
#ifndef UINT32_MAX
#   define UINT32_MAX      (4294967295U)...
#endif

#if defined(__64BIT__) || defined(_LONG_LONG) || defined(_LP64) || defined(WIN64) || defined(_WIN64)
#   ifndef INT64_MIN
#       define INT64_MIN       (-9223372036854775807-1)
#   endif
#   ifndef INT64_MAX
#       define INT64_MAX       (9223372036854775807)
#   endif
#   ifndef UINT64_MAX
#       define UINT64_MAX      (18446744073709551615)
#   endif
#   ifndef INTMAX_MIN
#       define INTMAX_MIN      INT64_MIN
#   endif
#   ifndef INTMAX_MAX
#       define INTMAX_MAX      INT64_MAX
#   endif
#   ifndef UINTMAX_MAX
#       define UINTMAX_MAX     UINT64_MAX
#   endif
#else
#   ifndef INTMAX_MIN
#       define INTMAX_MIN      INT32_MIN
#   endif
#   ifndef INTMAX_MAX
#       define INTMAX_MAX      INT32_MAX
#   endif
#   ifndef UINTMAX_MAX
#       define UINTMAX_MAX     UINT32_MAX
#   endif
#endif

/*===========================================================================*/
/* Boolean data type                                                         */
/*===========================================================================*/

#if !HAVE_BOOL_T
    typedef int8_t bool_t;
#endif

#ifndef TRUE
#   define TRUE  1
#endif
#ifndef FALSE
#   define FALSE 0
#endif

#endif
