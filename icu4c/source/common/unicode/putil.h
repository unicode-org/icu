/*
******************************************************************************
*
*   Copyright (C) 1997-2001, International Business Machines
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
 * functions may have to be re-implemented.  */

/* Floating point utilities 
 * @draft
 */
U_CAPI UBool  U_EXPORT2  uprv_isNaN(double);
U_CAPI UBool  U_EXPORT2 uprv_isInfinite(double);
U_CAPI UBool   U_EXPORT2 uprv_isPositiveInfinity(double);
U_CAPI UBool   U_EXPORT2 uprv_isNegativeInfinity(double);
U_CAPI double   U_EXPORT2 uprv_getNaN(void);
U_CAPI double   U_EXPORT2 uprv_getInfinity(void);

U_CAPI double   U_EXPORT2 uprv_floor(double x);
U_CAPI double   U_EXPORT2 uprv_ceil(double x);
U_CAPI double   U_EXPORT2 uprv_fabs(double x);
U_CAPI double   U_EXPORT2 uprv_modf(double x, double* y);
U_CAPI double   U_EXPORT2 uprv_fmod(double x, double y);
U_CAPI double   U_EXPORT2 uprv_pow10(int32_t x);
U_CAPI double   U_EXPORT2 uprv_IEEEremainder(double x, double y);
U_CAPI double   U_EXPORT2 uprv_fmax(double x, double y);
U_CAPI double   U_EXPORT2 uprv_fmin(double x, double y);
U_CAPI int32_t  U_EXPORT2 uprv_max(int32_t x, int32_t y);
U_CAPI int32_t  U_EXPORT2 uprv_min(int32_t x, int32_t y);
U_CAPI double   U_EXPORT2 uprv_trunc(double d);
U_CAPI void     U_EXPORT2 uprv_longBitsFromDouble(double d, int32_t *hi, uint32_t *lo);
#if U_IS_BIG_ENDIAN
#   define uprv_isNegative(number) (*((signed char *)&(number))<0)
#else
#   define uprv_isNegative(number) (*((signed char *)&(number)+sizeof(number)-1)<0)
#endif

/* Conversion from a digit to the character with radix base from 2-19 */
#define T_CString_itosOffset(a) ((a)<=9?('0'+(a)):('A'+(a)-10))

/*
 * Return the floor of the log base 10 of a given double.
 * This method compensates for inaccuracies which arise naturally when
 * computing logs, and always gives the correct value.  The parameter
 * must be positive and finite.
 * (Thanks to Alan Liu for supplying this function.)
 */
/**
 * Returns the common log of the double value d.
 *
 * @param d the double value to apply the common log function for.
 * @return the log of value d.
 * @draft
 */
U_CAPI int16_t  U_EXPORT2 uprv_log10(double d);

/**
 * Returns the number of digits after the decimal point in a double number x.
 *
 * @param x the double number
 * @draft
 */
U_CAPI int32_t  U_EXPORT2 uprv_digitsAfterDecimal(double x);

/**
 * Time zone utilities
 *
 * Wrappers for C runtime library functions relating to timezones.
 * The t_tzset() function (similar to tzset) uses the current setting 
 * of the environment variable TZ to assign values to three global 
 * variables: daylight, timezone, and tzname. These variables have the 
 * following meanings, and are declared in &lt;time.h>.
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
 * @draft
 */
U_CAPI void     U_EXPORT2 uprv_tzset(void);
U_CAPI int32_t  U_EXPORT2 uprv_timezone(void);
U_CAPI char*    U_EXPORT2 uprv_tzname(int n);

/**
 * Get UTC (GMT) time measured in seconds since 0:00 on 1/1/70. 
 * @draft
 */
U_CAPI int32_t  U_EXPORT2 uprv_getUTCtime(void);

/**
 * Return the data directory for this platform. 
 * @draft
 */
U_CAPI const char* U_EXPORT2 u_getDataDirectory(void);

/** 
 * Set the data directory. 
 * @draft
 */
U_CAPI void U_EXPORT2 u_setDataDirectory(const char *directory);

/**
 * Return the default codepage for this platform and locale 
 * @draft
 */
U_CAPI const char*  U_EXPORT2 uprv_getDefaultCodepage(void);

/**
 * Return the default locale ID string by querying ths system, or
 *     zero if one cannot be found. 
 * @draft
 */
U_CAPI const char*  U_EXPORT2 uprv_getDefaultLocaleID(void);

/*
 * Finds the least double greater than d (if positive == true),
 * or the greatest double less than d (if positive == false).
 *
 * This is a special purpose function defined by the ChoiceFormat API
 * documentation.
 * It is not a general purpose function and not defined for NaN or Infinity
 * @deprecated This will be removed after 2002-Jun-30. Use the ChoiceFormat closures API instead.
 */
U_CAPI double       U_EXPORT2 uprv_nextDouble(double d, UBool positive);

/**
 * Filesystem file and path separator characters.
 * Example: '/' and ':' on Unix, '\\' and ';' on Windows.
 * @draft
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
 * @stable
 */
U_CAPI void U_EXPORT2
u_charsToUChars(const char *cs, UChar *us, UTextOffset length);

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
 * @stable
 */
U_CAPI void U_EXPORT2
u_UCharsToChars(const UChar *us, char *cs, UTextOffset length);

/* Define U_UPPER_ORDINAL */
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
#    define U_MAX_PTR(base) ((void *)(((char *)(base)+0x7fffffff) > (char *)(base) ? ((char *)(base)+0x7fffffff) : (char *)-1))
#  endif
#endif

#endif
