/*
******************************************************************************
*
*   Copyright (C) 1997-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*  FILE NAME : platform.h
*
*   Date        Name        Description
*   05/13/98    nos         Creation (content moved here from ptypes.h).
*   03/02/99    stephen     Added AS400 support.
*   03/30/99    stephen     Added Linux support.
*   04/13/99    stephen     Reworked for autoconf.
******************************************************************************
*/

/* Define the platform we're on. */
#ifndef WIN32
#define WIN32
#endif

#if defined(__BORLANDC__)
#define U_HAVE_PLACEMENT_NEW 0
#define U_HAVE_INTTYPES_H 1
#define __STDC_CONSTANT_MACROS
#endif

/* Define whether inttypes.h is available */
#ifndef U_HAVE_INTTYPES_H
#define U_HAVE_INTTYPES_H 0
#endif

/*
 * Define what support for C++ streams is available.
 *     If U_IOSTREAM_SOURCE is set to 199711, then <iostream> is available
 * (1997711 is the date the ISO/IEC C++ FDIS was published), and then
 * one should qualify streams using the std namespace in ICU header
 * files.
 *     If U_IOSTREAM_SOURCE is set to 198506, then <iostream.h> is
 * available instead (198506 is the date when Stroustrup published
 * "An Extensible I/O Facility for C++" at the summer USENIX conference).
 *     If U_IOSTREAM_SOURCE is 0, then C++ streams are not available and
 * support for them will be silently suppressed in ICU.
 *
 */

#ifndef U_IOSTREAM_SOURCE
#define U_IOSTREAM_SOURCE 199711
#endif

/* Determines whether specific types are available */
#ifndef U_HAVE_INT8_T
#define U_HAVE_INT8_T U_HAVE_INTTYPES_H
#endif

#ifndef U_HAVE_UINT8_T
#define U_HAVE_UINT8_T U_HAVE_INTTYPES_H
#endif

#ifndef U_HAVE_INT16_T
#define U_HAVE_INT16_T U_HAVE_INTTYPES_H
#endif

#ifndef U_HAVE_UINT16_T
#define U_HAVE_UINT16_T U_HAVE_INTTYPES_H
#endif

#ifndef U_HAVE_INT32_T
#define U_HAVE_INT32_T U_HAVE_INTTYPES_H
#endif

#ifndef U_HAVE_UINT32_T
#define U_HAVE_UINT32_T U_HAVE_INTTYPES_H
#endif

#ifndef U_HAVE_INT64_T
#define U_HAVE_INT64_T U_HAVE_INTTYPES_H
#endif

#ifndef U_HAVE_UINT64_T
#define U_HAVE_UINT64_T U_HAVE_INTTYPES_H
#endif

/* Define 64 bit limits */
#define INT64_C(x) ((int64_t)x)
#define UINT64_C(x) ((uint64_t)x)

/*===========================================================================*/
/* Generic data types                                                        */
/*===========================================================================*/

/* If your platform does not have the <inttypes.h> header, you may
   need to edit the typedefs below. */
#if U_HAVE_INTTYPES_H
#include <inttypes.h>
#else /* U_HAVE_INTTYPES_H */

#if ! U_HAVE_INT8_T
typedef signed char int8_t;
#endif

#if ! U_HAVE_UINT8_T
typedef unsigned char uint8_t;
#endif

#if ! U_HAVE_INT16_T
typedef signed short int16_t;
#endif

#if ! U_HAVE_UINT16_T
typedef unsigned short uint16_t;
#endif

#if ! U_HAVE_INT32_T
typedef signed int int32_t;
#endif

#if ! U_HAVE_UINT32_T
typedef unsigned int uint32_t;
#endif

#if ! U_HAVE_INT64_T
/* _MSC_VER is used to detect the Microsoft compiler. */
#ifdef _MSC_VER
    typedef signed __int64 int64_t;
#else
    typedef signed long long int64_t;
#endif
#endif

#if ! U_HAVE_UINT64_T
/* _MSC_VER is used to detect the Microsoft compiler. */
#ifdef _MSC_VER
    typedef unsigned __int64 uint64_t;
#else
    typedef unsigned long long uint64_t;
#endif
#endif
#endif

/*===========================================================================*/
/* Compiler and environment features                                         */
/*===========================================================================*/

/* Define whether namespace is supported */
#ifndef U_HAVE_NAMESPACE
#define U_HAVE_NAMESPACE 1
#endif

/* Determines the endianness of the platform */
#define U_IS_BIG_ENDIAN 0

/* 1 or 0 to enable or disable threads.  If undefined, default is: enable threads. */
#define ICU_USE_THREADS 1

/* Windows currently only runs on x86 CPUs which currently all have strong memory models. */
#define UMTX_STRONG_MEMORY_MODEL 1

#ifndef U_DEBUG
#ifdef _DEBUG
#define U_DEBUG 1
#else
#define U_DEBUG 0
#endif
#endif

#ifndef U_RELEASE
#ifdef NDEBUG
#define U_RELEASE 1
#else
#define U_RELEASE 0
#endif
#endif
/* Determine whether to disable renaming or not. This overrides the
   setting in umachine.h which is for all platforms. */
#ifndef U_DISABLE_RENAMING
#define U_DISABLE_RENAMING 0
#endif

/* Determine whether to override new and delete. */
#ifndef U_OVERRIDE_CXX_ALLOCATION
#define U_OVERRIDE_CXX_ALLOCATION 1
#endif
/* Determine whether to override placement new and delete for STL. */
#ifndef U_HAVE_PLACEMENT_NEW
#define U_HAVE_PLACEMENT_NEW 1
#endif

/* Determine whether to enable tracing. */
#ifndef U_ENABLE_TRACING
#define U_ENABLE_TRACING 1
#endif

/* Define the library suffix in a C syntax. */
#define U_HAVE_LIB_SUFFIX 0
#define U_LIB_SUFFIX_C_NAME 
#define U_LIB_SUFFIX_C_NAME_STRING ""

/*===========================================================================*/
/* Information about wchar support                                           */
/*===========================================================================*/

#define U_HAVE_WCHAR_H 1
#define U_SIZEOF_WCHAR_T 2

#define U_HAVE_WCSCPY 1

/*===========================================================================*/
/* Information about POSIX support                                           */
/*===========================================================================*/

#define U_TZSET         _tzset
#define U_HAVE_TIMEZONE 1
#if U_HAVE_TIMEZONE
#   define U_TIMEZONE   _timezone
#endif
#define U_TZNAME        _tzname

#define U_HAVE_MMAP 0
#define U_HAVE_POPEN 0

/*===========================================================================*/
/* Symbol import-export control                                              */
/*===========================================================================*/

#ifdef U_STATIC_IMPLEMENTATION
#define U_EXPORT
#else
#define U_EXPORT __declspec(dllexport)
#endif
#define U_EXPORT2 __cdecl
#define U_IMPORT __declspec(dllimport)

/*===========================================================================*/
/* Code alignment and C function inlining                                    */
/*===========================================================================*/

#ifndef U_INLINE
#define U_INLINE __inline
#endif

#if defined(_MSC_VER) && defined(_M_IX86)
#define U_ALIGN_CODE(val)    __asm      align val
#else
#define U_ALIGN_CODE(val)
#endif
      

/*===========================================================================*/
/* Programs used by ICU code                                                 */
/*===========================================================================*/

#ifndef U_MAKE
#define U_MAKE  "nmake"
#define U_MAKE_IS_NMAKE 1
#endif
