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
