/*
******************************************************************************
*
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*  FILE NAME : pmacos.h
*
*   Date        Name        Description
*   05/13/98    nos         Creation (content moved here from ptypes.h).
*   03/02/99    stephen     Added AS400 support.
*   03/30/99    stephen     Added Linux support.
*   04/13/99    stephen     Reworked for autoconf.
*   08/26/00    srl         Moved to pmacos.h
******************************************************************************
*/

/* Define the platform we're on. */
#ifndef macintosh
#define macintosh
#endif

#ifndef XP_MAC
#define  XP_MAC
#endif

/* We want the console window (argc/argv) */
/*
#ifndef XP_MAC_CONSOLE
#define  XP_MAC_CONSOLE
#endif
*/

/* Define whether inttypes.h is available */
#define U_HAVE_INTTYPES_H 0

/* Define whether namespace is supported */
#define U_HAVE_NAMESPACE 1

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
#define U_IOSTREAM_SOURCE 198506    /* Wish we could use 199711 but that breaks while compiling chariter.cpp */
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
#   if defined(_LP64)
      typedef signed int  int32_t;
#   else
      typedef signed long int32_t;
#   endif
#endif

#if ! HAVE_UINT32_T
#   if defined(_LP64)
      typedef unsigned int  uint32_t;
#   else
      typedef unsigned long uint32_t;
#   endif
#endif

#endif

/*===========================================================================*/
/* Character data types                                                      */
/*===========================================================================*/

#define U_HAVE_WCHAR_H      0
#define U_SIZEOF_WCHAR_T    2

#define U_HAVE_WCSCPY       0

/*===========================================================================*/
/* Symbol import-export control                                              */
/*===========================================================================*/

/* should be pragmas? */

#define U_EXPORT /*__declspec(dllexport)*/
#define U_EXPORT2
#define U_IMPORT /*__declspec(dllimport)*/

/*===========================================================================*/
/* Programs used by ICU code                                                 */
/*===========================================================================*/

#define U_MAKE  "Make"

