/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1998     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
*  FILE NAME : platform.h
*
*   Date        Name        Description
*   05/13/98    nos         Creation (content moved here from ptypes.h).
*   03/02/99    stephen     Added AS400 support.
*   03/30/99    stephen     Added Linux support.
*   04/13/99    stephen     Reworked for autoconf.
*******************************************************************************
*/

/* Define the platform we're on. */
#ifndef OS2
#define OS2  1
#endif

/* Define whether inttypes.h is available */
#define HAVE_INTTYPES_H 0

/* Determines whether specific types are available */
#define HAVE_INT8_T 0
#define HAVE_UINT8_T 0
#define HAVE_INT16_T 0
#define HAVE_UINT16_T 0
#define HAVE_INT32_T  0
#define HAVE_UINT32_T 0
#define HAVE_BOOL_T 0

/*===========================================================================*/
/* Platform/Language determination                                           */
/*===========================================================================*/

#ifdef macintosh
#ifdef XP_MAC
#undef XP_MAC
#endif
#define XP_MAC 1
#include <string.h>
#endif

//#if defined(__OS2__) || defined(OS//2)
#if defined(__OS2__)
  #ifdef OS2
    #undef OS2
  #endif
  #define OS2   1
#endif



/* XP_CPLUSPLUS is a cross-platform symbol which should be defined when 
   using C++.  It should not be defined when compiling under C. */
#undef XP_CPLUSPLUS
#ifdef __cplusplus
#define XP_CPLUSPLUS
#endif

/*===========================================================================*/
/* Generic data types                                                        */
/*===========================================================================*/

/* If your platform does not have the <inttypes.h> header, you may
   need to edit the typedefs below. */
#if HAVE_INTTYPES_H
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

#include <limits.h>
#define T_INT32_MAX (LONG_MAX)

/*===========================================================================*/
/* Boolean data type                                                         */
/*===========================================================================*/

#undef TRUE
#undef FALSE

#if ! HAVE_BOOL_T
typedef int8_t bool_t;
#endif

#define TRUE  1
#define FALSE 0

/*===========================================================================*/
/* Unicode string offset                                                     */
/*===========================================================================*/
typedef int32_t UTextOffset;

/*===========================================================================*/
/* Unicode character                                                         */
/*===========================================================================*/
/* Another common UChar definition is wchar_t.  However, this is valid
 * only if wchar_t is at least 16 bits and in Unicode encoding.  */
typedef uint16_t UChar;

/*===========================================================================*/
/* Symbol import-export control                                              */
/*===========================================================================*/

#ifdef _WIN32
#define U_EXPORT __declspec(dllexport)
#define U_IMPORT __declspec(dllimport)
#elif defined(AS400)
#define U_EXPORT __declspec(dllexport)
#define U_IMPORT
#else
#define U_EXPORT
#define U_IMPORT
#endif
