/*
*******************************************************************************
*
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
*  FILE NAME : pos400.h
*
*   Date        Name        Description
*   05/13/98    nos         Creation (content moved here from ptypes.h).
*   03/02/99    stephen     Added AS400 support.
*   03/30/99    stephen     Added Linux support.
*   04/13/99    stephen     Reworked for autoconf.
*   09/21/99    barry       Created new for OS/400 platform.
*******************************************************************************
*/

/* Define the platform we're on. */
#ifndef OS400
#define OS400
#endif

/* Define whether inttypes.h is available */
#define U_HAVE_INTTYPES_H 0

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
#define U_IOSTREAM_SOURCE 198506
#endif

/* Determines whether specific types are available */
#define HAVE_INT8_T 0
#define HAVE_UINT8_T 0
#define HAVE_INT16_T 0
#define HAVE_UINT16_T 0
#define HAVE_INT32_T 0
#define HAVE_UINT32_T 0
#define HAVE_BOOL_T 0

/* Determines the endianness of the platform */
#define U_IS_BIG_ENDIAN 1

#define HAVE_DLOPEN     0

/* 1 or 0 to enable or disable threads.  If undefined, default is: enable threads. */
#define ICU_USE_THREADS 1

/*===========================================================================*/
/* Generic data types                                                        */
/*===========================================================================*/

/* If your platform does not have the <inttypes.h> header, you may
   need to edit the typedefs below. */
#if U_HAVE_INTTYPES_H
#include <inttypes.h>
#else

#if ! HAVE_INT8_T
typedef signed char int8_t;
#endif

#if ! HAVE_UINT8_T
typedef unsigned char uint8_t;
#endif

#if ! HAVE_INT16_T
typedef signed short int16_t;
#endif

#if ! HAVE_UINT16_T
typedef unsigned short uint16_t;
#endif

#if ! HAVE_INT32_T
typedef signed long int32_t;
#endif

#if ! HAVE_UINT32_T
typedef unsigned long uint32_t;
#endif

#endif

/*===========================================================================*/
/* See utypes.h for the normal defintion                                     */
/*===========================================================================*/

/*
With the provided macro we should never be out of range of a given segment
(a traditional/typical segment that is).  Our segments have 5 bytes for the id
and 3 bytes for the offset.  The key is that the casting takes care of only
retrieving the offset portion minus x1000.  Hence, the smallest offset seen in
a program is x001000 and when casted to an int would be 0.  That's why we can
only add 0xffefff.  Otherwise, we would exceed the segment.

Currently, 16MB is the current addressing limitation on as/400.  This macro
may eventually be changed to use 2GB addressability for the newer version of
as/400 machines.
*/
#ifndef U_MAX_PTR
#define U_MAX_PTR(ptr) ((void*)(((char*)ptr)-((int32_t)(ptr))+((int32_t)0xffefff)))
#endif

/*===========================================================================*/
/* Character data types                                                      */
/*===========================================================================*/

#define U_CHARSET_FAMILY 1

/*===========================================================================*/
/* Information about wchar support                                           */
/*===========================================================================*/

#define U_HAVE_WCHAR_H   1
#define U_SIZEOF_WCHAR_T 2

#define U_HAVE_WCSCPY    1

/*===========================================================================*/
/* Information about POSIX support                                           */
/*===========================================================================*/

#define U_HAVE_NL_LANGINFO              0
#define U_HAVE_NL_LANGINFO_CODESET      0
#define U_NL_LANGINFO_CODESET

/* These cannot be defined for this platform
#define U_TZSET
#define U_TIMEZONE
#define U_TZNAME
*/

/*===========================================================================*/
/* Symbol import-export control                                              */
/*===========================================================================*/

#define U_EXPORT
#define U_EXPORT2
#define U_IMPORT

/*===========================================================================*/
/* Programs used by ICU code                                                 */
/*===========================================================================*/

#define U_MAKE  "gmake"
