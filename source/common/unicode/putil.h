/*
******************************************************************************
*
*   Copyright (C) 1997-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*  FILE NAME : putil.h
*
*   Date        Name        Description
*   05/14/98    nos         Creation (content moved here from utypes.h).
*   06/17/99    erm         Added IEEE_754
*   07/22/98    stephen     Added IEEEremainder, max, min, trunc
*   08/13/98    stephen     Added isNegativeInfinity, isPositiveInfinity
*   08/24/98    stephen     Added longBitsFromDouble
*   03/02/99    stephen     Removed openFile().  Added AS400 support.
*   04/15/99    stephen     Converted to C
*   11/15/99    helena      Integrated S/390 changes for IEEE support.
*   01/11/00    helena      Added u_getVersion.
******************************************************************************
*/

#ifndef PUTIL_H
#define PUTIL_H

#include "unicode/utypes.h"

/* Define this to 1 if your platform supports IEEE 754 floating point,
   to 0 if it does not. */
#ifndef IEEE_754
#   define IEEE_754 1
#endif

/*==========================================================================*/
/* Platform utilities                                                       */
/*==========================================================================*/

/**
 * Platform utilities isolates the platform dependencies of the
 * libarary.  For each platform which this code is ported to, these
 * functions may have to be re-implemented.
 */

/**
 * Floating point utility to determine if a double is Not a Number (NaN).
 * @internal
 */
U_CAPI UBool   U_EXPORT2 uprv_isNaN(double d);
/**
 * Floating point utility to determine if a double has an infinite value.
 * @internal
 */
U_CAPI UBool   U_EXPORT2 uprv_isInfinite(double d);
/**
 * Floating point utility to determine if a double has a positive infinite value.
 * @internal
 */
U_CAPI UBool   U_EXPORT2 uprv_isPositiveInfinity(double d);
/**
 * Floating point utility to determine if a double has a negative infinite value.
 * @internal
 */
U_CAPI UBool   U_EXPORT2 uprv_isNegativeInfinity(double d);
/**
 * Floating point utility that returns a Not a Number (NaN) value.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_getNaN(void);
/**
 * Floating point utility that returns an infinite value.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_getInfinity(void);

/**
 * Floating point utility to truncate a double.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_trunc(double d);
/**
 * Floating point utility to calculate the floor of a double.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_floor(double d);
/**
 * Floating point utility to calculate the ceiling of a double.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_ceil(double d);
/**
 * Floating point utility to calculate the absolute value of a double.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_fabs(double d);
/**
 * Floating point utility to calculate the fractional and integer parts of a double.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_modf(double d, double* pinteger);
/**
 * Floating point utility to calculate the remainder of a double divided by another double.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_fmod(double d, double y);
/**
 * Floating point utility to calculate d to the power of exponent (d^exponent).
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_pow(double d, double exponent);
/**
 * Floating point utility to calculate 10 to the power of exponent (10^exponent).
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_pow10(int32_t exponent);
/**
 * Floating point utility to calculate the maximum value of two doubles.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_fmax(double d, double y);
/**
 * Floating point utility to calculate the minimum value of two doubles.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_fmin(double d, double y);
/**
 * Private utility to calculate the maximum value of two integers.
 * @internal
 */
U_CAPI int32_t U_EXPORT2 uprv_max(int32_t d, int32_t y);
/**
 * Private utility to calculate the minimum value of two integers.
 * @internal
 */
U_CAPI int32_t U_EXPORT2 uprv_min(int32_t d, int32_t y);

#if U_IS_BIG_ENDIAN
#   define uprv_isNegative(number) (*((signed char *)&(number))<0)
#else
#   define uprv_isNegative(number) (*((signed char *)&(number)+sizeof(number)-1)<0)
#endif

/**
 * Return the largest positive number that can be represented by an integer
 * type of arbitrary bit length.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_maxMantissa(void);

/**
 * Return the floor of the log base 10 of a given double.
 * This method compensates for inaccuracies which arise naturally when
 * computing logs, and always gives the correct value.  The parameter
 * must be positive and finite.
 * (Thanks to Alan Liu for supplying this function.)
 *
 * @param d the double value to apply the common log function for.
 * @return the log of value d.
 * @internal
 */
U_CAPI int16_t  U_EXPORT2 uprv_log10(double d);

/**
 * Floating point utility to calculate the logarithm of a double.
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_log(double d);

/**
 * Does common notion of rounding e.g. uprv_floor(x + 0.5);
 * @param x the double number
 * @return the rounded double
 * @internal
 */
U_CAPI double  U_EXPORT2 uprv_round(double x);

/**
 * Returns the number of digits after the decimal point in a double number x.
 *
 * @param x the double number
 * @return the number of digits after the decimal point in a double number x.
 * @internal
 */
U_CAPI int32_t  U_EXPORT2 uprv_digitsAfterDecimal(double x);

/**
 * Time zone utilities
 *
 * Wrappers for C runtime library functions relating to timezones.
 * The t_tzset() function (similar to tzset) uses the current setting 
 * of the environment variable TZ to assign values to three global 
 * variables: daylight, timezone, and tzname. These variables have the 
 * following meanings, and are declared in &lt;time.h&gt;.
 *
 *   daylight   Nonzero if daylight-saving-time zone (DST) is specified
 *              in TZ; otherwise, 0. Default value is 1.
 *   timezone   Difference in seconds between coordinated universal
 *              time and local time. E.g., -28,800 for PST (GMT-8hrs)
 *   tzname(0)  Three-letter time-zone name derived from TZ environment
 *              variable. E.g., "PST".
 *   tzname(1)  Three-letter DST zone name derived from TZ environment
 *              variable.  E.g., "PDT". If DST zone is omitted from TZ,
 *              tzname(1) is an empty string.
 *
 * Notes: For example, to set the TZ environment variable to correspond
 * to the current time zone in Germany, you can use one of the
 * following statements:
 *
 *   set TZ=GST1GDT
 *   set TZ=GST+1GDT
 *
 * If the TZ value is not set, t_tzset() attempts to use the time zone
 * information specified by the operating system. Under Windows NT
 * and Windows 95, this information is specified in the Control Panel’s
 * Date/Time application.
 * @internal
 */
U_CAPI void     U_EXPORT2 uprv_tzset(void);

/**
 * Difference in seconds between coordinated universal
 * time and local time. E.g., -28,800 for PST (GMT-8hrs)
 * @return the difference in seconds between coordinated universal time and local time.
 * @internal
 */
U_CAPI int32_t  U_EXPORT2 uprv_timezone(void);

/**
 *   tzname(0)  Three-letter time-zone name derived from TZ environment
 *              variable. E.g., "PST".
 *   tzname(1)  Three-letter DST zone name derived from TZ environment
 *              variable.  E.g., "PDT". If DST zone is omitted from TZ,
 *              tzname(1) is an empty string.
 * @internal
 */
U_CAPI char*    U_EXPORT2 uprv_tzname(int n);

/**
 * Get UTC (GMT) time measured in seconds since 0:00 on 1/1/70.
 * @return the UTC time measured in seconds 
 * @stable ICU 2.0
 * @internal
 */
U_CAPI int32_t  U_EXPORT2 uprv_getUTCtime(void);

/**
 * Return the ICU data directory. 
 * The data directory is where common format ICU data files (.dat files)
 *   are loaded from.  Note that normal use of the built-in ICU
 *   facilities does not require loading of an external data file;
 *   unless you are adding custom data to ICU, the data directory
 *   does not need to be set.
 *
 * The data directory is determined as follows:
 *    If u_setDataDirectory() has been called, that is it, otherwise
 *    if the ICU_DATA environment variable is set, use that, otherwise
 *    If a data directory was specifed at ICU build time
 *      (#define ICU_DATA_DIR "path"), use that,
 *    otherwise no data directory is available.
 *
 * @return the data directory, or an empty string ("") if no data directory has
 *         been specified.
 *   
 * @stable ICU 2.0
 */
U_CAPI const char* U_EXPORT2 u_getDataDirectory(void);

/** 
 * Set the ICU data directory. 
 * The data directory is where common format ICU data files (.dat files)
 *   are loaded from.  Note that normal use of the built-in ICU
 *   facilities does not require loading of an external data file;
 *   unless you are adding custom data to ICU, the data directory
 *   does not need to be set.
 *
 * This function should be called at most once in a process, before the
 * first ICU operation (e.g., u_init()) that will require the loading of an
 * ICU data file.
 * This function is not thread-safe. Use it before calling ICU APIs from
 * multiple threads.
 *
 * @param directory The directory to be set.
 *
 * @see u_init
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 u_setDataDirectory(const char *directory);

/**
 * Please use ucnv_getDefaultName() instead.
 * Return the default codepage for this platform and locale.
 * This function can call setlocale() on Unix platforms. Please read the
 * platform documentation on setlocale() before calling this function.
 * @return the default codepage for this platform 
 * @internal
 */
U_CAPI const char*  U_EXPORT2 uprv_getDefaultCodepage(void);

/**
 * Please use uloc_getDefault() instead.
 * Return the default locale ID string by querying ths system, or
 *     zero if one cannot be found. 
 * This function can call setlocale() on Unix platforms. Please read the
 * platform documentation on setlocale() before calling this function.
 * @return the default locale ID string
 * @internal
 */
U_CAPI const char*  U_EXPORT2 uprv_getDefaultLocaleID(void);

/**
 * Filesystem file and path separator characters.
 * Example: '/' and ':' on Unix, '\\' and ';' on Windows.
 * @stable ICU 2.0
 */
#ifdef XP_MAC
#   define U_FILE_SEP_CHAR ':'
#   define U_PATH_SEP_CHAR ';'
#   define U_FILE_SEP_STRING ":"
#   define U_PATH_SEP_STRING ";"
#elif defined(WIN32) || defined(OS2)
#   define U_FILE_SEP_CHAR '\\'
#   define U_PATH_SEP_CHAR ';'
#   define U_FILE_SEP_STRING "\\"
#   define U_PATH_SEP_STRING ";"
#else
#   define U_FILE_SEP_CHAR '/'
#   define U_PATH_SEP_CHAR ':'
#   define U_FILE_SEP_STRING "/"
#   define U_PATH_SEP_STRING ":"
#endif

/**
 * Convert char characters to UChar characters.
 * This utility function is useful only for "invariant characters"
 * that are encoded in the platform default encoding.
 * They are a small, constant subset of the encoding and include
 * just the latin letters, digits, and some punctuation.
 * For details, see utypes.h .
 *
 * @param cs Input string, points to <code>length</code>
 *           character bytes from a subset of the platform encoding.
 * @param us Output string, points to memory for <code>length</code>
 *           Unicode characters.
 * @param length The number of characters to convert; this may
 *               include the terminating <code>NUL</code>.
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2
u_charsToUChars(const char *cs, UChar *us, int32_t length);

/**
 * Convert UChar characters to char characters.
 * This utility function is useful only for "invariant characters"
 * that can be encoded in the platform default encoding.
 * They are a small, constant subset of the encoding and include
 * just the latin letters, digits, and some punctuation.
 * For details, see utypes.h .
 *
 * @param us Input string, points to <code>length</code>
 *           Unicode characters that can be encoded with the
 *           codepage-invariant subset of the platform encoding.
 * @param cs Output string, points to memory for <code>length</code>
 *           character bytes.
 * @param length The number of characters to convert; this may
 *               include the terminating <code>NUL</code>.
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2
u_UCharsToChars(const UChar *us, char *cs, int32_t length);

/**
 * \def U_UPPER_ORDINAL
 * Get the ordinal number of an uppercase invariant character
 * @stable ICU 2.4
 */
#if U_CHARSET_FAMILY==U_ASCII_FAMILY
#   define U_UPPER_ORDINAL(x) ((x)-'A')
#elif U_CHARSET_FAMILY==U_EBCDIC_FAMILY
#   define U_UPPER_ORDINAL(x) (((x) < 'J') ? ((x)-'A') : \
                              (((x) < 'S') ? ((x)-'J'+9) : \
                               ((x)-'S'+18)))
#else
#   error Unknown charset family!
#endif

/**
 * Maximum value of a (void*) - use to indicate the limit of an 'infinite' buffer.
 * In fact, buffer sizes must not exceed 2GB so that the difference between
 * the buffer limit and the buffer start can be expressed in an int32_t.
 *
 * The definition of U_MAX_PTR must fulfill the following conditions:
 * - return the largest possible pointer greater than base
 * - return a valid pointer according to the machine architecture (AS/400, 64-bit, etc.)
 * - avoid wrapping around at high addresses
 * - make sure that the returned pointer is not farther from base than 0x7fffffff
 *
 * @param base The beginning of a buffer to find the maximum offset from
 * @internal
 */
#ifndef U_MAX_PTR
#  ifdef OS390
#    define U_MAX_PTR(base) ((void *)0x7fffffff)
#  elif defined(OS400)
/*
 * With the provided macro we should never be out of range of a given segment
 * (a traditional/typical segment that is).  Our segments have 5 bytes for the id
 * and 3 bytes for the offset.  The key is that the casting takes care of only
 * retrieving the offset portion minus x1000.  Hence, the smallest offset seen in
 * a program is x001000 and when casted to an int would be 0.  That's why we can
 * only add 0xffefff.  Otherwise, we would exceed the segment.
 *
 * Currently, 16MB is the current addressing limitation on as/400.  This macro
 * may eventually be changed to use 2GB addressability for the newer version of
 * as/400 machines.
 */
#    define U_MAX_PTR(base) ((void *)(((char *)base)-((int32_t)(base))+((int32_t)0xffefff)))
#  else
#    define U_MAX_PTR(base) ((void *)(((char *)(base)+0x7fffffffu) > (char *)(base) ? ((char *)(base)+0x7fffffffu) : (char *)-1))
#  endif
#endif

#endif
