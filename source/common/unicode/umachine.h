/*
******************************************************************************
*
*   Copyright (C) 1999-2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
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


/**
 * \file
 * \brief Basic types and constants for UTF 
 * 
 * <h2> Basic types and constants for UTF </h2>
 *   This file defines basic types and constants for utf.h to be
 *   platform-independent. umachine.h and utf.h are included into
 *   utypes.h to provide all the general definitions for ICU.
 *   All of these definitions used to be in utypes.h before
 *   the UTF-handling macros made this unmaintainable.
 * 
 */
/*==========================================================================*/
/* Include platform-dependent definitions                                   */
/* which are contained in the platform-specific file platform.h             */
/*==========================================================================*/

#if defined(WIN32) || defined(_WIN32) || defined(WIN64) || defined(_WIN64)
#   include "unicode/pwin32.h"
#elif defined(__OS400__)
#   include "unicode/pos400.h"
#elif defined(__MWERKS__)
#   include "unicode/pmacos.h"
#else
#   include "unicode/platform.h"
#endif

/*==========================================================================*/
/* XP_CPLUSPLUS is a cross-platform symbol which should be defined when     */
/* using C++.  It should not be defined when compiling under C.             */
/*==========================================================================*/

#ifdef __cplusplus
#   ifndef XP_CPLUSPLUS
#       define XP_CPLUSPLUS
#   endif
#else
#   undef XP_CPLUSPLUS
#endif

/*==========================================================================*/
/* For C wrappers, we use the symbol U_CAPI.                                */
/* This works properly if the includer is C or C++.                         */
/* Functions are declared   U_CAPI return-type U_EXPORT2 function-name()... */
/*==========================================================================*/

/**
 * \def U_CFUNC
 * This is used in a declaration of a library private ICU C function.
 * @stable ICU 2.4
 */

/**
 * \def U_CDECL_BEGIN
 * This is used to begin a declaration of a library private ICU C API.
 * @stable ICU 2.4
 */

/**
 * \def U_CDECL_END
 * This is used to end a declaration of a library private ICU C API 
 * @stable ICU 2.4
 */

#ifdef XP_CPLUSPLUS
#   define U_CFUNC extern "C"
#   define U_CDECL_BEGIN extern "C" {
#   define U_CDECL_END   }
#else
#   define U_CFUNC extern
#   define U_CDECL_BEGIN
#   define U_CDECL_END
#endif

/**
 * \def U_NAMESPACE_BEGIN
 * This is used to begin a declaration of a public ICU C++ API.
 * If the compiler doesn't support namespaces, this does nothing.
 * @stable ICU 2.4
 */

/**
 * \def U_NAMESPACE_END
 * This is used to end a declaration of a public ICU C++ API 
 * If the compiler doesn't support namespaces, this does nothing.
 * @stable ICU 2.4
 */

/**
 * \def U_NAMESPACE_USE
 * This is used to specify that the rest of the code uses the
 * public ICU C++ API namespace.
 * If the compiler doesn't support namespaces, this does nothing.
 * @stable ICU 2.4
 */

/**
 * \def U_NAMESPACE_QUALIFIER
 * This is used to qualify that a function or class is part of
 * the public ICU C++ API namespace.
 * If the compiler doesn't support namespaces, this does nothing.
 * @stable ICU 2.4
 */

/* Define namespace symbols if the compiler supports it. */
#if U_HAVE_NAMESPACE
#   define U_NAMESPACE_BEGIN namespace U_ICU_NAMESPACE {
#   define U_NAMESPACE_END  }
#   define U_NAMESPACE_USE using namespace U_ICU_NAMESPACE;
#   define U_NAMESPACE_QUALIFIER U_ICU_NAMESPACE::
#else
#   define U_NAMESPACE_BEGIN
#   define U_NAMESPACE_END
#   define U_NAMESPACE_USE
#   define U_NAMESPACE_QUALIFIER
#endif

/** This is used to declare a function as a public ICU C API @stable ICU 2.0*/
#define U_CAPI U_CFUNC U_EXPORT

/*==========================================================================*/
/* limits for int32_t etc., like in POSIX inttypes.h                        */
/*==========================================================================*/

#ifndef INT8_MIN
/** The smallest value an 8 bit signed integer can hold @stable ICU 2.0 */
#   define INT8_MIN        ((int8_t)(-128))
#endif
#ifndef INT16_MIN
/** The smallest value a 16 bit signed integer can hold @stable ICU 2.0 */
#   define INT16_MIN       ((int16_t)(-32767-1))
#endif
#ifndef INT32_MIN
/** The smallest value a 32 bit signed integer can hold @stable ICU 2.0 */
#   define INT32_MIN       ((int32_t)(-2147483647-1))
#endif

#ifndef INT8_MAX
/** The largest value an 8 bit signed integer can hold @stable ICU 2.0 */
#   define INT8_MAX        ((int8_t)(127))
#endif
#ifndef INT16_MAX
/** The largest value a 16 bit signed integer can hold @stable ICU 2.0 */
#   define INT16_MAX       ((int16_t)(32767))
#endif
#ifndef INT32_MAX
/** The largest value a 32 bit signed integer can hold @stable ICU 2.0 */
#   define INT32_MAX       ((int32_t)(2147483647))
#endif

#ifndef UINT8_MAX
/** The largest value an 8 bit unsigned integer can hold @stable ICU 2.0 */
#   define UINT8_MAX       ((uint8_t)(255U))
#endif
#ifndef UINT16_MAX
/** The largest value a 16 bit unsigned integer can hold @stable ICU 2.0 */
#   define UINT16_MAX      ((uint16_t)(65535U))
#endif
#ifndef UINT32_MAX
/** The largest value a 32 bit unsigned integer can hold @stable ICU 2.0 */
#   define UINT32_MAX      ((uint32_t)(4294967295U))
#endif

#if defined(U_INT64_T_UNAVAILABLE)
#   ifndef INTMAX_MIN
#       define INTMAX_MIN      INT32_MIN
#   endif
#   ifndef INTMAX_MAX
#       define INTMAX_MAX      INT32_MAX
#   endif
#   ifndef UINTMAX_MAX
#       define UINTMAX_MAX     UINT32_MAX
#   endif
#else
#   ifndef INT64_MIN
/** The smallest value a 64 bit signed integer can hold @stable ICU 2.0 */
#       define INT64_MIN       ((int64_t)(-9223372036854775807-1))
#   endif
#   ifndef INT64_MAX
/** The largest value a 64 bit signed integer can hold @stable ICU 2.0 */
#       define INT64_MAX       ((int64_t)(9223372036854775807))
#   endif
#   ifndef UINT64_MAX
/** The largest value a 64 bit unsigned integer can hold @stable ICU 2.0 */
#       define UINT64_MAX      ((uint64_t)(18446744073709551615))
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
#endif

/*==========================================================================*/
/* Boolean data type                                                        */
/*==========================================================================*/

/** The ICU boolean type @stable ICU 2.0 */
typedef int8_t UBool;

#ifndef TRUE
/** The TRUE value of a UBool @stable ICU 2.0 */
#   define TRUE  1
#endif
#ifndef FALSE
/** The FALSE value of a UBool @stable ICU 2.0 */
#   define FALSE 0
#endif


/*==========================================================================*/
/* U_INLINE and U_ALIGN_CODE   Set default values if these are not already  */
/*                             defined.  Definitions normally are in        */
/*                             platform.h or the corresponding file for     */
/*                             the OS in use.                               */
/*==========================================================================*/

/**
 * \def U_ALIGN_CODE
 * This is used to align code fragments to a specific byte boundary.
 * This is useful for getting consistent performance test results.
 * @internal
 */
#ifndef U_ALIGN_CODE
#   define U_ALIGN_CODE(n)
#endif

#ifndef U_INLINE
#   define U_INLINE
#endif

#include "unicode/urename.h"

#endif
