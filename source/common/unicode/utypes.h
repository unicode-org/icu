/*
**********************************************************************
*   Copyright (C) 1996-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
*  FILE NAME : UTYPES.H (formerly ptypes.h)
*
*   Date        Name        Description
*   12/11/96    helena      Creation.
*   02/27/97    aliu        Added typedefs for UClassID, int8, int16, int32,
*                           uint8, uint16, and uint32.
*   04/01/97    aliu        Added XP_CPLUSPLUS and modified to work under C as
*                            well as C++.
*                           Modified to use memcpy() for uprv_arrayCopy() fns.
*   04/14/97    aliu        Added TPlatformUtilities.
*   05/07/97    aliu        Added import/export specifiers (replacing the old
*                           broken EXT_CLASS).  Added version number for our
*                           code.  Cleaned up header.
*    6/20/97    helena      Java class name change.
*   08/11/98    stephen     UErrorCode changed from typedef to enum
*   08/12/98    erm         Changed T_ANALYTIC_PACKAGE_VERSION to 3
*   08/14/98    stephen     Added uprv_arrayCopy() for int8_t, int16_t, int32_t
*   12/09/98    jfitz       Added BUFFER_OVERFLOW_ERROR (bug 1100066)
*   04/20/99    stephen     Cleaned up & reworked for autoconf.
*                           Renamed to utypes.h.
*   05/05/99    stephen     Changed to use <inttypes.h>
*   12/07/99    helena      Moved copyright notice string from ucnv_bld.h here.
*******************************************************************************
*/

#ifndef UTYPES_H
#define UTYPES_H

#include "unicode/umachine.h"
#include "unicode/utf.h"

/*!
 * \file
 * \brief Basic definitions for ICU, for both C and C++ APIs
 *
 * This file defines basic types, constants, and enumerations directly or
 * indirectly by including other header files, especially utf.h for the
 * basic character and string definitions and umachine.h for consistent
 * integer and other types.
 */

/*===========================================================================*/
/* char Character set family                                                 */
/*===========================================================================*/

/**
 * \def U_CHARSET_FAMILY
 *
 * <p>These definitions allow to specify the encoding of text
 * in the char data type as defined by the platform and the compiler.
 * It is enough to determine the code point values of "invariant characters",
 * which are the ones shared by all encodings that are in use
 * on a given platform.</p>
 *
 * <p>Those "invariant characters" should be all the uppercase and lowercase
 * latin letters, the digits, the space, and "basic punctuation".
 * Also, '\n', '\r', '\t' should be available.</p>
 *
 * <p>The list of "invariant characters" is:<br>
 * \code
 *    A-Z  a-z  0-9  SPACE  "  %  &amp;  '  (  )  *  +  ,  -  .  /  :  ;  <  =  >  ?  _
 *\endcode
 *<br>
 * (52 letters + 10 numbers + 20 punc/sym = 82 total)</p>
 *
 * <p>In other words, all the graphic characters in 7-bit ASCII should
 * be safely accessible except the following:</p>
 * 
 * \code
 *    '\' <backslash>
 *    '[' <left bracket>
 *    ']' <right bracket>
 *    '{' <left brace>
 *    '}' <right brace>
 *    '^' <circumflex>
 *    '~' <tilde>
 *    '!' <exclamation mark>
 *    '#' <number sign>
 *    '|' <vertical line>
 *    '$' <dollar sign>
 *    '@' <commercial at>
 *    '`' <grave accent>
 * \endcode
 */

#define U_ASCII_FAMILY 0
#define U_EBCDIC_FAMILY 1

#ifndef U_CHARSET_FAMILY
#   define U_CHARSET_FAMILY 0
#endif

/*===========================================================================*/
/* Related version information                                               */
/*===========================================================================*/

/** The current ICU major version as an integer. */
#define U_ICU_VERSION_MAJOR_NUM 1

/** The current ICU minor version as an integer. */
#define U_ICU_VERSION_MINOR_NUM 8

/** The current ICU patchlevel version as an integer. */
#define U_ICU_VERSION_PATCHLEVEL_NUM 0

/** The current ICU library version as a dotted-decimal string. */
#define U_ICU_VERSION "1.8"

/** The current ICU library major/minor version as a string without dots, for library name suffixes. */
#define U_ICU_VERSION_SHORT "18"

/** An ICU version consists of up to 4 numbers from 0..255. */
#define U_MAX_VERSION_LENGTH 4

/** In a string, ICU version fields are delimited by dots. */
#define U_VERSION_DELIMITER '.'

/** The maximum length of an ICU version string. */
#define U_MAX_VERSION_STRING_LENGTH 20

/** The binary form of a version on ICU APIs is an array of 4 uint8_t. */
typedef uint8_t UVersionInfo[U_MAX_VERSION_LENGTH];

/*===========================================================================*/
/* ICUDATA naming scheme                                                     */
/*===========================================================================*/

/**
 * \def U_ICUDATA_TYPE_LETTER
 *
 * This is a platform-dependent string containing one letter:
 * - b for big-endian, ASCII-family platforms
 * - l for little-endian, ASCII-family platforms
 * - e for big-endian, EBCDIC-family platforms
 * This letter is part of the common data file name.
 */
#if U_CHARSET_FAMILY
#   if U_IS_BIG_ENDIAN
   /* EBCDIC - should always be BE */
#     define U_ICUDATA_TYPE_LETTER "e"
#   else
#     error "Don't know what to do with little endian EBCDIC!"
#     define U_ICUDATA_TYPE_LETTER "x"
#   endif
#else
#   if U_IS_BIG_ENDIAN
      /* Big-endian ASCII */
#     define U_ICUDATA_TYPE_LETTER "b"
#   else
      /* Little-endian ASCII */
#     define U_ICUDATA_TYPE_LETTER "l"
#   endif
#endif

/** A single string literal containing the icudata stub name, i.e. 'icudt18e' for 
   ICU 1.8.x on EBCDIC, etc.. */
#define U_ICUDATA_NAME    "icudt" U_ICU_VERSION_SHORT U_ICUDATA_TYPE_LETTER

/**
 * \def U_CALLCONV
 * Work around the OS390 compiler issue, to be removed when the compiler
 * updates come out.
 */
#if defined(OS390) && defined(XP_CPLUSPLUS)
#    define U_CALLCONV __cdecl
#else
#    define U_CALLCONV 
#endif

/**
 * \def NULL
 * Define NULL if necessary, to 0 for C++ and to ((void *)0) for C.
 */
#ifndef NULL
#ifdef XP_CPLUSPLUS
#define NULL    0
#else
#define NULL    ((void *)0)
#endif
#endif

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
 * \par
 * getDynamicClassID() is declared in the base class of the hierarchy as
 * a pure virtual.  Each concrete subclass implements it in the same way:
 *
 * \code
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
 * \endcode
 *
 * Each concrete class implements getStaticClassID() as well, which allows
 * clients to test for a specific type.
 *
 * \code
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
 * \endcode
 */

typedef void* UClassID;

/*===========================================================================*/
/* Shared library/DLL import-export API control                              */
/*===========================================================================*/

/*
 * Control of symbol import/export.
 * ICU is separated into three libraries.
 */

/**
 * \def U_COMBINED_IMPLEMENTATION
 * Set to export library symbols from inside the ICU library
 * when all of ICU is in a single library.
 * This can be set as a compiler option while building ICU, and it
 * needs to be the first one tested to override U_COMMON_API, U_I18N_API, etc.
 */

/**
 * \def U_COMMON_API
 * Set to export library symbols from inside the common library,
 * and to import them from outside.
 */

/**
 * \def U_I18N_API
 * Set to export library symbols from inside the i18n library,
 * and to import them from outside.
 */

/**
 * \def U_LAYOUT_API
 * Set to export library symbols from inside the layout engine library,
 * and to import them from outside.
 */


#if defined(U_COMBINED_IMPLEMENTATION)
#define U_COMMON_API  U_EXPORT
#define U_I18N_API    U_EXPORT
#define U_LAYOUT_API  U_EXPORT
#elif defined(U_COMMON_IMPLEMENTATION)
#define U_COMMON_API  U_EXPORT
#define U_I18N_API    U_IMPORT
#define U_LAYOUT_API  U_IMPORT
#elif defined(U_I18N_IMPLEMENTATION)
#define U_COMMON_API  U_IMPORT
#define U_I18N_API    U_EXPORT
#define U_LAYOUT_API  U_IMPORT
#elif defined(U_LAYOUT_IMPLEMENTATION)
#define U_COMMON_API  U_IMPORT
#define U_I18N_API    U_IMPORT
#define U_LAYOUT_API  U_EXPORT
#else
#define U_COMMON_API  U_IMPORT
#define U_I18N_API    U_IMPORT
#define U_LAYOUT_API  U_IMPORT
#endif

/**
 * \def U_STANDARD_CPP_NAMESPACE
 * Control of C++ Namespace
 */
#ifdef __cplusplus
#define U_STANDARD_CPP_NAMESPACE        ::
#else
#define U_STANDARD_CPP_NAMESPACE
#endif

/*===========================================================================*/
/* UErrorCode */
/*===========================================================================*/

/**
 * Error code to replace exception handling, so that the code is compatible with all C++ compilers,
 * and to use the same mechanism for C and C++.
 *
 * \par
 * ICU functions that take a reference (C++) or a pointer (C) to a UErrorCode
 * first test if(U_FAILURE(errorCode)) { return immediately; }
 * so that in a chain of such functions the first one that sets an error code
 * causes the following ones to not perform any operations.
 *
 * \par
 * Error codes should be tested using U_FAILURE() and U_SUCCESS().
 */
enum UErrorCode {
    U_ERROR_INFO_START        = -128,   /**< Start of information results (semantically successful) */
    U_USING_FALLBACK_ERROR    = -128,   /**< A resource bundle lookup returned a fallback result (not an error) */
    U_USING_DEFAULT_ERROR     = -127,   /**< A resource bundle lookup returned a result from the root locale (not an error) */
    U_SAFECLONE_ALLOCATED_ERROR = -126, /**< A SafeClone operation required allocating memory (informational only) */
    U_ERROR_INFO_LIMIT,                 /**< This must always be the last warning value to indicate the limit for UErrorCode warnings (last warning code +1) */

    U_ZERO_ERROR              =  0,     /**< No error, no warning. */

    U_ILLEGAL_ARGUMENT_ERROR  =  1,     /**< Start of codes indicating failure */
    U_MISSING_RESOURCE_ERROR  =  2,
    U_INVALID_FORMAT_ERROR    =  3,
    U_FILE_ACCESS_ERROR       =  4,
    U_INTERNAL_PROGRAM_ERROR  =  5,     /**< Indicates a bug in the library code */
    U_MESSAGE_PARSE_ERROR     =  6,
    U_MEMORY_ALLOCATION_ERROR =  7,     /**< Memory allocation error */
    U_INDEX_OUTOFBOUNDS_ERROR =  8,
    U_PARSE_ERROR             =  9,     /**< Equivalent to Java ParseException */
    U_INVALID_CHAR_FOUND      = 10,     /**< In the Character conversion routines: Invalid character or sequence was encountered */
    U_TRUNCATED_CHAR_FOUND    = 11,     /**< In the Character conversion routines: More bytes are required to complete the conversion successfully */
    U_ILLEGAL_CHAR_FOUND      = 12,     /**< In codeset conversion: a sequence that does NOT belong in the codepage has been encountered */
    U_INVALID_TABLE_FORMAT    = 13,     /**< Conversion table file found, but corrupted */
    U_INVALID_TABLE_FILE      = 14,     /**< Conversion table file not found */
    U_BUFFER_OVERFLOW_ERROR   = 15,     /**< A result would not fit in the supplied buffer */
    U_UNSUPPORTED_ERROR       = 16,     /**< Requested operation not supported in current context */
    U_RESOURCE_TYPE_MISMATCH  = 17,     /**< an operation is requested over a resource that does not support it */
    U_ILLEGAL_ESCAPE_SEQUENCE = 18,     /**< ISO-2022 illlegal escape sequence */
    U_UNSUPPORTED_ESCAPE_SEQUENCE = 19, /**< ISO-2022 unsupported escape sequence */
    U_NO_SPACE_AVAILABLE      = 20,     /**< No space available for in-buffer expansion for Arabic shaping */
    U_ERROR_LIMIT                       /**< This must always be the last value to indicate the limit for UErrorCode (last error code +1) */
};

#ifndef XP_CPLUSPLUS
typedef enum UErrorCode UErrorCode;
#endif

/* Use the following to determine if an UErrorCode represents */
/* operational success or failure. */

#ifdef XP_CPLUSPLUS
    /**
     * Does the error code indicate success?
     * @stable
     */
    inline UBool U_SUCCESS(UErrorCode code) { return (UBool)(code<=U_ZERO_ERROR); }
    /**
     * Does the error code indicate a failure?
     * @stable
     */
    inline UBool U_FAILURE(UErrorCode code) { return (UBool)(code>U_ZERO_ERROR); }
#else
    /**
     * Does the error code indicate success?
     * @stable
     */
#   define U_SUCCESS(x) ((x)<=U_ZERO_ERROR)
    /**
     * Does the error code indicate a failure?
     * @stable
     */
#   define U_FAILURE(x) ((x)>U_ZERO_ERROR)
#endif

/**
 * Return a string for a UErrorCode value.
 * The string will be the same as the name of the error code constant
 * in the UErrorCode enum above.
 */
U_CAPI const char * U_EXPORT2
u_errorName(UErrorCode code);

#define U_COPYRIGHT_STRING \
  " Copyright (C) 2001, International Business Machines Corporation and others. All Rights Reserved. "

#define U_COPYRIGHT_STRING_LENGTH  128

/**
 * Mutex data type.
 * @internal
 */
typedef void *UMTX;

/*===========================================================================*/
/* Include header for platform utilies */
/*===========================================================================*/

#include "unicode/putil.h"

#endif /* _UTYPES */
