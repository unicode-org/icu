/*
*******************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
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
#ifndef WIN32
#define WIN32
#endif

/* Define whether inttypes.h is available */
#define HAVE_INTTYPES_H 0

/* Determines whether specific types are available */
#define HAVE_INT8_T 0
#define HAVE_UINT8_T 0
#define HAVE_INT16_T 0
#define HAVE_UINT16_T 0
#define HAVE_INT32_T 0
#define HAVE_UINT32_T 0
#define HAVE_BOOL_T 0

/* Determines the endianness of the platform */
#define U_IS_BIG_ENDIAN 0

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

#include <limits.h>
#if defined(_LP64)
#   define T_INT32_MAX (INT_MAX)
#   define T_INT32_MIN (INT_MIN)
#else
#   define T_INT32_MAX (LONG_MAX)
#   define T_INT32_MIN (LONG_MIN)
#endif


/*===========================================================================*/
/* Character data types                                                      */
/*===========================================================================*/

#define U_SIZEOF_WCHAR_T 2

/*===========================================================================*/
/* Symbol import-export control                                              */
/*===========================================================================*/

#define U_EXPORT __declspec(dllexport)
#define U_EXPORT2
#define U_IMPORT __declspec(dllimport)
