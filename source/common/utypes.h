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
*   02/27/97    aliu        Added typedefs for UClassID, int8, int16, int32,
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
#   include "pwin32.h"
#elif defined(__OS2__)
#   include "pos2.h"
#elif defined(__OS400__)
#   include "pos400.h"
#else
#   include "platform.h"
#endif

/* XP_CPLUSPLUS is a cross-platform symbol which should be defined when 
   using C++.  It should not be defined when compiling under C. */
#ifdef __cplusplus
#   ifndef XP_CPLUSPLUS
#       define XP_CPLUSPLUS
#   endif
#else
#   undef XP_CPLUSPLUS
#endif

/*===========================================================================*/
/* Boolean data type                                                         */
/*===========================================================================*/

#if ! HAVE_BOOL_T
typedef int8_t bool_t;
#endif

#ifndef TRUE
#   define TRUE  1
#endif
#ifndef FALSE
#   define FALSE 0
#endif

/*===========================================================================*/
/* Unicode string offset                                                     */
/*===========================================================================*/
typedef int32_t UTextOffset;

/*===========================================================================*/
/* Unicode character                                                         */
/*===========================================================================*/
typedef uint16_t UChar;

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
/* For C wrappers, we use the symbol U_CAPI.                                   */
/* This works properly if the includer is C or C++.                          */
/* Functions are declared   U_CAPI return-type U_EXPORT2 function-name() ...   */
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


/* Define NULL pointer value  if it isn't already defined */

#ifndef NULL
#ifdef XP_CPLUSPLUS
#define NULL    0
#else
#define NULL    ((void *)0)
#endif
#endif

/* Maximum value of a (void*) - use to indicate the limit of
   an 'infinite' buffer.  */
#define U_MAX_PTR ((void*)-1)

/*===========================================================================*/
/* Calendar/TimeZone data types                                              */
/*===========================================================================*/

/**
 * Date and Time data type.
 * This is a primitive data type that holds the date and time
 * as the number of milliseconds since 1970-jan-01, 00:00 UTC.
 * UTC leap seconds are ignored.
 */
typedef double UDate;

/* Common time manipulation constants */
#define U_MILLIS_PER_SECOND        (1000)
#define U_MILLIS_PER_MINUTE       (60000)
#define U_MILLIS_PER_HOUR       (3600000)
#define U_MILLIS_PER_DAY       (86400000)


/*===========================================================================*/
/* UClassID-based RTTI */
/*===========================================================================*/

/**
 * UClassID is used to identify classes without using RTTI, since RTTI
 * is not yet supported by all C++ compilers.  Each class hierarchy which needs
 * to implement polymorphic clone() or operator==() defines two methods,
 * described in detail below.  UClassID values can be compared using
 * operator==(). Nothing else should be done with them.
 *
 * getDynamicClassID() is declared in the base class of the hierarchy as
 * a pure virtual.  Each concrete subclass implements it in the same way:
 *
 *      class Base {
 *      public:
 *          virtual UClassID getDynamicClassID() const = 0;
 *      }
 *
 *      class Derived {
 *      public:
 *          virtual UClassID getDynamicClassID() const
 *            { return Derived::getStaticClassID(); }
 *      }
 *
 * Each concrete class implements getStaticClassID() as well, which allows
 * clients to test for a specific type.
 *
 *      class Derived {
 *      public:
 *          static UClassID getStaticClassID();
 *      private:
 *          static char fgClassID;
 *      }
 *
 *      // In Derived.cpp:
 *      UClassID Derived::getStaticClassID()
 *        { return (UClassID)&Derived::fgClassID; }
 *      char Derived::fgClassID = 0; // Value is irrelevant
 */

typedef void* UClassID;

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
    U_ERROR_INFO_START        = -128,     /* Start of information results (semantically successful) */
    U_USING_FALLBACK_ERROR    = -128,
    U_USING_DEFAULT_ERROR     = -127,
    U_ERROR_INFO_LIMIT,

    U_ZERO_ERROR              =  0,       /* success */

    U_ILLEGAL_ARGUMENT_ERROR  =  1,       /* Start of codes indicating failure */
    U_MISSING_RESOURCE_ERROR  =  2,
    U_INVALID_FORMAT_ERROR    =  3,
    U_FILE_ACCESS_ERROR       =  4,
    U_INTERNAL_PROGRAM_ERROR  =  5,       /* Indicates a bug in the library code */
    U_MESSAGE_PARSE_ERROR     =  6,
    U_MEMORY_ALLOCATION_ERROR =  7,       /* Memory allocation error */
    U_INDEX_OUTOFBOUNDS_ERROR =  8,
    U_PARSE_ERROR             =  9,       /* Equivalent to Java ParseException */
    U_INVALID_CHAR_FOUND      = 10,       /* In the Character conversion routines: Invalid character or sequence was encountered*/
    U_TRUNCATED_CHAR_FOUND    = 11,       /* In the Character conversion routines: More bytes are required to complete the conversion successfully*/
    U_ILLEGAL_CHAR_FOUND      = 12,       /* In codeset conversion: a sequence that does NOT belong in the codepage has been encountered*/
    U_INVALID_TABLE_FORMAT    = 13,       /* Conversion table file found, but corrupted*/
    U_INVALID_TABLE_FILE      = 14,       /* Conversion table file not found*/
    U_BUFFER_OVERFLOW_ERROR   = 15,       /* A result would not fit in the supplied buffer */
    U_UNSUPPORTED_ERROR       = 16,       /* Requested operation not supported in current context */
    U_ERROR_LIMIT
};

#ifndef XP_CPLUSPLUS
typedef enum UErrorCode UErrorCode;
#endif

/* Use the following to determine if an UErrorCode represents */
/* operational success or failure. */
#ifdef XP_CPLUSPLUS
inline bool_t U_SUCCESS(UErrorCode code) { return (bool_t)(code<=U_ZERO_ERROR); }
inline bool_t U_FAILURE(UErrorCode code) { return (bool_t)(code>U_ZERO_ERROR); }
#else
#define U_SUCCESS(x) ((x)<=U_ZERO_ERROR)
#define U_FAILURE(x) ((x)>U_ZERO_ERROR)
#endif


/* Casting function for int32_t (backward compatibility version, here until
   T_INT32 is replaced) */
#define T_INT32(i) ((int32_t)i)


/*===========================================================================*/
/* Debugging */
/*===========================================================================*/

/* remove this */

/* This function is useful for debugging; it returns the text name */
/* of an UErrorCode result.  This is not the most efficient way of */
/* doing this but it's just for Debug builds anyway. */

/* Do not use these arrays directly: they will move to a .c file! */
static const char *
_uErrorInfoName[U_ERROR_INFO_LIMIT-U_ERROR_INFO_START]={
    "U_USING_FALLBACK_ERROR",
    "U_USING_DEFAULT_ERROR"
};

static const char *
_uErrorName[U_ERROR_LIMIT]={
    "U_ZERO_ERROR",

    "U_ILLEGAL_ARGUMENT_ERROR",
    "U_MISSING_RESOURCE_ERROR",
    "U_INVALID_FORMAT_ERROR",
    "U_FILE_ACCESS_ERROR",
    "U_INTERNAL_PROGRAM_ERROR",
    "U_MESSAGE_PARSE_ERROR",
    "U_MEMORY_ALLOCATION_ERROR",
    "U_INDEX_OUTOFBOUNDS_ERROR",
    "U_PARSE_ERROR",
    "U_INVALID_CHAR_FOUND",
    "U_TRUNCATED_CHAR_FOUND",
    "U_ILLEGAL_CHAR_FOUND",
    "U_INVALID_TABLE_FORMAT",
    "U_INVALID_TABLE_FILE",
    "U_BUFFER_OVERFLOW_ERROR",
    "U_UNSUPPORTED_ERROR"
};

#ifdef XP_CPLUSPLUS
inline const char *
errorName(UErrorCode code)
{
    if(code>=0 && code<U_ERROR_LIMIT) {
        return _uErrorName[code];
    } else if(code>=U_ERROR_INFO_START && code<U_ERROR_INFO_LIMIT) {
        return _uErrorInfoName[code-U_ERROR_INFO_START];
    } else {
        return "[BOGUS UErrorCode]";
    }
}
#else
#   define errorName(code) \
        ((code)>=0 && (code)<U_ERROR_LIMIT) ? \
            _uErrorName[code] : \
            ((code)>=U_ERROR_INFO_START && (code)<U_ERROR_INFO_LIMIT) ? \
                _uErrorInfoName[code-U_ERROR_INFO_START] : \
                "[BOGUS UErrorCode]"
#endif

/*===========================================================================*/
/* Include header for platform utilies */
/*===========================================================================*/

#include "putil.h"

#endif /* _UTYPES */
