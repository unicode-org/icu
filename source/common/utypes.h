/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1996, 1997                                 *
*   (C) Copyright International Business Machines Corporation,  1996-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
*  FILE NAME : UTYPES.H (formerly ptypes.h)
*
*   Date        Name        Description
*   12/11/96    helena      Creation.
*   02/27/97    aliu        Added typedefs for ClassID, int8, int16, int32,
*                           uint8, uint16, and uint32.
*   04/01/97    aliu        Added XP_CPLUSPLUS and modified to work under C as
*                            well as C++.
*                           Modified to use memcpy() for icu_arrayCopy() fns.
*   04/14/97    aliu        Added TPlatformUtilities.
*   05/07/97    aliu        Added import/export specifiers (replacing the old
*                           broken EXT_CLASS).  Added version number for our
*                           code.  Cleaned up header.
*    6/20/97    helena      Java class name change.
*   08/11/98    stephen     UErrorCode changed from typedef to enum
*   08/12/98    erm         Changed T_ANALYTIC_PACKAGE_VERSION to 3
*   08/14/98    stephen     Added icu_arrayCopy() for int8_t, int16_t, int32_t
*   12/09/98    jfitz       Added BUFFER_OVERFLOW_ERROR (bug 1100066)
*   04/20/99    stephen     Cleaned up & reworked for autoconf.
*                           Renamed to utypes.h.
*   05/05/99    stephen     Changed to use <inttypes.h>
*******************************************************************************
*/

#ifndef UTYPES_H
#define UTYPES_H

#include <memory.h>
#include <wchar.h>


#include <stdlib.h>

/*===========================================================================*/
/* Include platform-dependent definitions                                    */
/* which are contained in the platform-specific file platform.h              */
/*===========================================================================*/

#if defined(WIN32) || defined(_WIN32)
#include "pwin32.h"
#elif defined(__OS2__)
#include "pos2.h"
#else
#include "platform.h"
#endif

/*===========================================================================*/
/* ICU version number                                                        */
/*===========================================================================*/

/**
 * ICU package code version number.
 * This version number is incremented if and only if the code has changed
 * in a binary incompatible way.  For example, if the algorithm for generating
 * sort keys has changed, this code version must be incremented.
 *
 * This is for internal use only.  Clients should use
 * ResourceBundle::getVersionNumber().
 *
 * ResourceBundle::getVersionNumber() returns a full version number
 * for a resource, which consists of this code version number concatenated
 * with the ResourceBundle data file version number.
 */
#define ICU_VERSION "3"


/*===========================================================================*/
/* For C wrappers, we use the symbol CAPI.                                   */
/* This works properly if the includer is C or C++.                          */
/* ADDED MVS SPECIFICS - JJD   Including: FUNC_EXPORT                        */
/*                                 Since _Export MUST come after return type */
/*===========================================================================*/

#ifdef XP_CPLUSPLUS
# define C_FUNC extern "C"
# ifdef OS390
#  define CAPI C_FUNC
#  define U_EXPORT2 U_EXPORT
# else
#  define CAPI C_FUNC U_EXPORT
#  define U_EXPORT2
# endif
#else
#define C_FUNC
#if defined(OS390)
# define CAPI
# define U_EXPORT2 U_EXPORT
#else
# define CAPI U_EXPORT
# define U_EXPORT2
#endif
#endif



/*===========================================================================*/
/* Calendar/TimeZone data types                                              */
/*===========================================================================*/

typedef double UDate;

/* Common time manipulation constants */
#define kMillisPerSecond        (1000)
#define kMillisPerMinute       (60000)
#define kMillisPerHour       (3600000)
#define kMillisPerDay       (86400000)


/** A struct representing a range of text containing a specific field */
struct UFieldPosition {
  /** The field */
  int32_t field;
  /** The start of the text range containing field */
  int32_t beginIndex;
  /** The limit of the text range containing field */
  int32_t endIndex;
};
typedef struct UFieldPosition UFieldPosition;

/*===========================================================================*/
/* ClassID-based RTTI */
/*===========================================================================*/

/**
 * ClassID is used to identify classes without using RTTI, since RTTI
 * is not yet supported by all C++ compilers.  Each class hierarchy which needs
 * to implement polymorphic clone() or operator==() defines two methods,
 * described in detail below.  ClassID values can be compared using
 * operator==(). Nothing else should be done with them.
 *
 * getDynamicClassID() is declared in the base class of the hierarchy as
 * a pure virtual.  Each concrete subclass implements it in the same way:
 *
 *      class Base {
 *      public:
 *          virtual ClassID getDynamicClassID() const = 0;
 *      }
 *
 *      class Derived {
 *      public:
 *          virtual ClassID getDynamicClassID() const
 *            { return Derived::getStaticClassID(); }
 *      }
 *
 * Each concrete class implements getStaticClassID() as well, which allows
 * clients to test for a specific type.
 *
 *      class Derived {
 *      public:
 *          static ClassID getStaticClassID();
 *      private:
 *          static char fgClassID;
 *      }
 *
 *      // In Derived.cpp:
 *      ClassID Derived::getStaticClassID()
 *        { return (ClassID)&Derived::fgClassID; }
 *      char Derived::fgClassID = 0; // Value is irrelevant
 */

typedef void* ClassID;

/*===========================================================================*/
/* Shared library/DLL import-export API control                              */
/*===========================================================================*/

/**
 * Control of symbol import/export.
 * The ICU is separated into two libraries.
 */


#ifdef U_COMMON_IMPLEMENTATION
#define U_COMMON_API  U_EXPORT
#define U_I18N_API    U_IMPORT
#elif defined(U_I18N_IMPLEMENTATION)
#define U_COMMON_API  U_IMPORT
#define U_I18N_API    U_EXPORT
#else
#define U_COMMON_API  U_IMPORT
#define U_I18N_API    U_IMPORT
#endif
/*===========================================================================*/
/* UErrorCode */
/*===========================================================================*/

/** Error code to replace exception handling */
enum UErrorCode {
  ZERO_ERROR              =  0,
  ILLEGAL_ARGUMENT_ERROR  =  1,       /* Start of codes indicating failure */
  MISSING_RESOURCE_ERROR  =  2,
  INVALID_FORMAT_ERROR    =  3,
  FILE_ACCESS_ERROR       =  4,
  INTERNAL_PROGRAM_ERROR  =  5,       /* Indicates a bug in the library code */
  MESSAGE_PARSE_ERROR     =  6,
  MEMORY_ALLOCATION_ERROR =  7,       /* Memory allocation error */
  INDEX_OUTOFBOUNDS_ERROR =  8,
  PARSE_ERROR             =  9,       /* Equivalent to Java ParseException */
  INVALID_CHAR_FOUND      = 10,       /* In the Character conversion routines: Invalid character or sequence was encountered*/
  TRUNCATED_CHAR_FOUND    = 11,       /* In the Character conversion routines: More bytes are required to complete the conversion successfully*/
  ILLEGAL_CHAR_FOUND     =  12,       /* In codeset conversion: a sequence that does NOT belong in the codepage has been encountered*/
  INVALID_TABLE_FORMAT   =  13,       /*Conversion table file found, nut corrupted*/
  INVALID_TABLE_FILE     =  14,       /*Conversion table file not found*/
  BUFFER_OVERFLOW_ERROR =   15,        /* A result would not fit in the supplied buffer */
  UNSUPPORTED_ERROR     = 16,         /* Requested operation not supported in current context */
  USING_FALLBACK_ERROR  = -128,       /* Start of information results (semantically successful) */
  USING_DEFAULT_ERROR   = -127
};

#ifndef XP_CPLUSPLUS
typedef enum UErrorCode UErrorCode;
#endif

/* Use the following to determine if an UErrorCode represents */
/* operational success or failure. */
#ifdef XP_CPLUSPLUS
inline bool_t SUCCESS(UErrorCode code) { return (bool_t)(code<=ZERO_ERROR); }
inline bool_t FAILURE(UErrorCode code) { return (bool_t)(code>ZERO_ERROR); }
#else
#define SUCCESS(x) ((x)<=ZERO_ERROR)
#define FAILURE(x) ((x)>ZERO_ERROR)
#endif


/* Casting function for int32_t (backward compatibility version, here until
   T_INT32 is replaced) */
#define T_INT32(i) ((int32_t)i)


/*===========================================================================*/
/* Wide-character functions                                                  */
/*===========================================================================*/
#define icu_wcscat(dst, src) wcscat(dst, src)
#define icu_wcscpy(dst, src) wcscpy(dst, src)
#define icu_wcslen(src) wcslen(src)
#define icu_wcstombs(mbstr, wcstr, count) wcstombs(mbstr, wcstr, count)
#define icu_mbstowcs(wcstr, mbstr, count) mbstowcs(wcstr, mbstr, count)

/*===========================================================================*/
/* Array copy utility functions */
/*===========================================================================*/

#ifdef XP_CPLUSPLUS
inline void icu_arrayCopy(const double* src, double* dst, int32_t count)
{ memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const double* src, int32_t srcStart,
              double* dst, int32_t dstStart, int32_t count)
{ memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int8_t* src, int8_t* dst, int32_t count)
    { memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int8_t* src, int32_t srcStart,
              int8_t* dst, int32_t dstStart, int32_t count)
{ memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int16_t* src, int16_t* dst, int32_t count)
{ memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int16_t* src, int32_t srcStart,
              int16_t* dst, int32_t dstStart, int32_t count)
{ memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int32_t* src, int32_t* dst, int32_t count)
{ memcpy(dst, src, (size_t)(count * sizeof(*src))); }

inline void icu_arrayCopy(const int32_t* src, int32_t srcStart,
              int32_t* dst, int32_t dstStart, int32_t count)
{ memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

inline void
icu_arrayCopy(const UChar *src, int32_t srcStart,
        UChar *dst, int32_t dstStart, int32_t count)
{ memcpy(dst+dstStart, src+srcStart, (size_t)(count * sizeof(*src))); }

#endif

/*===========================================================================*/
/* Debugging */
/*===========================================================================*/

/* remove this */

/* This function is useful for debugging; it returns the text name */
/* of an UErrorCode result.  This is not the most efficient way of */
/* doing this but it's just for Debug builds anyway. */
#if defined(_DEBUG) && defined(XP_CPLUSPLUS)
inline const char* errorName(UErrorCode code)
{
  switch (code) {
  case ZERO_ERROR:                return "ZERO_ERROR";
  case ILLEGAL_ARGUMENT_ERROR:    return "ILLEGAL_ARGUMENT_ERROR";
  case MISSING_RESOURCE_ERROR:    return "MISSING_RESOURCE_ERROR";
  case INVALID_FORMAT_ERROR:      return "INVALID_FORMAT_ERROR";
  case FILE_ACCESS_ERROR:         return "FILE_ACCESS_ERROR";
  case INTERNAL_PROGRAM_ERROR:    return "INTERNAL_PROGRAM_ERROR";
  case MESSAGE_PARSE_ERROR:       return "MESSAGE_PARSE_ERROR";
  case MEMORY_ALLOCATION_ERROR:   return "MEMORY_ALLOCATION_ERROR";
  case PARSE_ERROR:               return "PARSE_ERROR";
  case INVALID_CHAR_FOUND:        return "INVALID_CHAR_FOUND";
  case TRUNCATED_CHAR_FOUND:      return "TRUNCATED_CHAR_FOUND";
  case ILLEGAL_CHAR_FOUND:        return "ILLEGAL_CHAR_FOUND";
  case INVALID_TABLE_FORMAT:      return "INVALID_TABLE_FORMAT";
  case INVALID_TABLE_FILE:        return "INVALID_TABLE_FILE";
  case BUFFER_OVERFLOW_ERROR:     return "BUFFER_OVERFLOW_ERROR";
  case USING_FALLBACK_ERROR:      return "USING_FALLBACK_ERROR";
  case USING_DEFAULT_ERROR:       return "USING_DEFAULT_ERROR";
  default:                        return "[BOGUS UErrorCode]";
  }
}
#endif

/* Define NULL pointer value  if it isn't already defined */

#ifndef NULL
#ifdef XP_CPLUSPLUS
#define NULL    0
#else
#define NULL    ((void *)0)
#endif
#endif


/*===========================================================================*/
/* Include header for platform utilies */
/*===========================================================================*/

#include "putil.h"

#endif /* _UTYPES */




